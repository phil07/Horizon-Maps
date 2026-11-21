package com.headunit.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.res.stringResource
import com.headunit.launcher.NavEngine.FasterRoute
import com.headunit.launcher.NavEngine.Notice

// Part of NavEngine (moved out of NavEngine.kt to keep that file workable): these are extension functions on the engine,
// so they are called exactly as before.

internal fun NavEngine.isClosure(cat: String) = cat.equals("roadClosed", true) || cat == "8"

private fun NavEngine.noteClosures(list: List<Incident>) {
    val now = System.currentTimeMillis()
    closures.entries.removeIf { it.value.second < now }
    for (inc in list) if (isClosure(inc.cat) && inc.pts.isNotEmpty()) closures[inc.id.ifBlank { "${inc.pts.first().lat},${inc.pts.first().lon}" }] = inc.pts to now + 30 * 60_000L
}

/** Points on closed roads: from the feeds (start, middle and end of each), and drivers' "Straße gesperrt" reports. */
internal fun NavEngine.closurePts(from: LL?, to: LL?): List<LL> {
    val now = System.currentTimeMillis()
    val pts = ArrayList<LL>()
    for ((ps, until) in closures.values) if (until >= now) pts.addAll(listOf(ps.first(), ps[ps.size / 2], ps.last()).distinct())
    try { for (rp in allReports()) if (rp.kind == "closed") pts += rp.pos } catch (e: Exception) { softFail(e) }
    // the start or the end of the trip: the only way there or away, so it is not avoided
    val ok = pts.filter { p -> (from == null || Geo.dist(p, from) > 300.0) && (to == null || Geo.dist(p, to) > 300.0) }
    val here = from ?: fix?.pos
    return if (here != null) ok.sortedBy { Geo.dist(it, here) } else ok
}

/** About 40 m round each closure point, for TomTom and the offline router. */
internal fun NavEngine.closureBoxes(from: LL?, to: LL?): List<Pair<LL, LL>> = closurePts(from, to).take(20).map { p ->
    val dLat = 40.0 / 110574.0; val dLon = 40.0 / (111320.0 * cos(Geo.rad(p.lat)))
    LL(p.lat - dLat, p.lon - dLon) to LL(p.lat + dLat, p.lon + dLon)
}

/** True when [r] runs within 30 m of a point of [pts] ahead of [from] metres along it. */
internal fun NavEngine.passes(r: Route, pts: List<LL>, from: Double): Boolean {
    for (q in pts) for (n in r.pts.indices) { if (r.cum[n] < from) continue; if (Geo.dist(r.pts[n], q) < 30.0) return true }
    return false
}

/** A closure on the route ahead: a new route round it (once per closure). */
private fun NavEngine.closureAhead(r: Route) {
    if (previewing || simMode != 0 || rerouting || route !== r) return
    val prog = curProgress
    val feed = incMatches.filter { (inc, off, _) -> isClosure(inc.cat) && off > prog + 50 && off < prog + 40_000 }.map { it.first }
    val own = try { allReports().filter { it.kind == "closed" }.map { Incident("rep${it.t}", "roadClosed", "", 0, listOf(it.pos)) } } catch (e: Exception) { emptyList() }
    val d = dest?.pos
    val hit = (feed + own).firstOrNull { inc -> inc.id !in closureRerouted && (d == null || inc.pts.none { Geo.dist(it, d) < 300.0 }) && passes(r, inc.pts.take(30), prog + 50) } ?: return
    closureRerouted += hit.id
    if (hit.id.startsWith("rep")) noteClosures(emptyList()) else closures[hit.id] = hit.pts to System.currentTimeMillis() + 30 * 60_000L
    closurePending = hit
    lastReroute = 0L
    reroute()
}

/** After a reroute for a closure: says the new way round, or that there is none (the route still goes through). */
internal fun NavEngine.closureAfterReroute(r: Route) {
    val c = closurePending ?: return
    closurePending = null
    if (passes(r, c.pts.take(30), 0.0)) {
        speak(Lang.t("Sperrung auf der Route, kein Umweg gefunden.", "Road closed on the route, no way round found."), true)
        showNotice(Notice("closure", Lang.t("Straße gesperrt", "Road closed"), Lang.t("Kein Umweg gefunden", "No way round found"), 2), 15_000L)
    } else {
        speak(Lang.t("Straße gesperrt. Neue Route.", "Road closed. New route."), true)
        showNotice(Notice("closure", Lang.t("Straße gesperrt", "Road closed"), Lang.t("Neue Route um die Sperrung", "New route round the closure"), 2), 10_000L)
    }
}

internal fun NavEngine.checkIncidents(r: Route, progress: Double) {
    try { checkWrongWay(r, progress) } catch (e: Exception) { softFail(e) }
    try { if (camerasOnMap()) updateRouteCams(r, progress); pushHazards(progress) } catch (e: Exception) { softFail(e) }
    // TomTom's incident data when there's a key; otherwise the incidents that came with a Mapbox route
    val fromRoute = (!hasKey || ttDown()) && r.incidents.isNotEmpty()
    val ab = autobahnIncidents(r)
    // Spain: the DGT's incidents (DgtRest); the Netherlands: NDW's (NdwRest). With or without a TomTom key
    val feedOn = (countryCode() == "ES" && DgtRest.items.isNotEmpty()) || (countryCode() == "NL" && NdwRest.items.isNotEmpty())
    if ((!hasKey && !fromRoute && ab.isEmpty() && !feedOn) || simMode != 0 || !prefs.b(PreferenceKeys.INCIDENTS, true)) { incidentAhead = null; return }
    val now = System.currentTimeMillis()
    // (the phone in the background while the car screen runs the trip: the car asks, not both)
    if (now - incAt > 180_000 && !shadow()) {
        incAt = now
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                try {
                    var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
                    var any = false
                    for (n in r.pts.indices) {
                        if (r.cum[n] < progress - 200 || r.cum[n] > progress + 40_000) continue
                        val q = r.pts[n]; any = true
                        minLat = min(minLat, q.lat); maxLat = max(maxLat, q.lat); minLon = min(minLon, q.lon); maxLon = max(maxLon, q.lon)
                    }
                    if (!any) emptyList() else {
                        val tt = if (fromRoute) r.incidents else if (hasKey) TomTomRest.incidents(apiKey, minLon - 0.01, minLat - 0.01, maxLon + 0.01, maxLat + 0.01) else emptyList<Incident>()
                        // (what TomTom already has is not repeated from the DGT)
                        val list = tt + ab + when (countryCode()) { "ES" -> DgtRest.notIn(tt); "NL" -> NdwRest.notIn(tt); else -> emptyList() }
                        noteClosures(list)
                        val out = ArrayList<Triple<Incident, Double, String>>()
                        for (inc in list) {
                            var bestOff = -1.0; var bestD = 40.0
                            val q = inc.pts.first()
                            // a line (a stretch of road) only counts where the route drives the same way along it: a road the other way
                            // (the carriageway across the median) is left out
                            val lineDir = if (inc.pts.size >= 2) Geo.bearing(inc.pts[0], inc.pts[1]) else null
                            for (n in r.pts.indices) {
                                if (r.cum[n] < progress - 200 || r.cum[n] > progress + 40_000) continue
                                val d = Geo.dist(r.pts[n], q)
                                if (d >= bestD) continue
                                if (lineDir != null && n + 1 < r.pts.size && abs(angDiff(Geo.bearing(r.pts[n], r.pts[n + 1]), lineDir)) > 45.0) continue
                                bestD = d; bestOff = r.cum[n]
                            }
                            if (bestOff >= 0) out += Triple(inc, bestOff, incidentLabel(inc))
                        }
                        out
                    }
                } catch (e: Exception) { null }
            }
            if (found != null) { incMatches = found; try { applyIncidentSev(r, found) } catch (e: Exception) { softFail(e) }; try { closureAhead(r) } catch (e: Exception) { softFail(e) } }
        }
    }
    // (wrong-way drivers have their own, louder warning: checkWrongWay)
    val next = incMatches.filter { it.second > progress - 30 && it.first.cat != "wrongway" && !AutobahnRest.isWrongWay(it.first.text) }.minByOrNull { it.second }
    if (next == null) { incidentAhead = null; return }
    val d = max(0.0, next.second - progress)
    if (d > 15_000) { incidentAhead = null; return }
    incidentAhead = IncidentAhead(next.third, d, next.first.delaySec)
    if (d < 8000 && incAlerted.add(next.first.id)) {
        val mins = next.first.delaySec / 60
        val extra = if (mins >= 2) Lang.t(", etwa $mins Minuten Verzögerung", ", about $mins minutes delay") else ""
        raiseAlert("incident", Lang.t("${next.third} in ${Geo.fmtDist(d, imperial)}$extra", "${next.third} ahead, ${Geo.fmtDist(d, imperial)}$extra"), 7000)
        speak(Lang.t("In ${Geo.spoken(d, imperial)}: ${next.third}$extra", "${next.third} ahead in ${Geo.spoken(d, imperial)}$extra"), true)
    }
}

/** Colours the stretch of road around each live incident amber (slow) or red (heavy), on top of the route's own traffic data. */
private fun NavEngine.applyIncidentSev(r: Route, found: List<Triple<Incident, Double, String>>) {
    if (sevBaseRoute !== r) { sevBase = r.sev.copyOf(); sevBaseRoute = r }
    val base = sevBase ?: return
    if (base.size != r.sev.size) return
    System.arraycopy(base, 0, r.sev, 0, base.size)
    for ((inc, startOff, _) in found) {
        val level = when {
            inc.cat.contains("close", true) || inc.delaySec >= 300 -> 2
            // every jam ahead colours the route (amber; red above from 5 minutes, see above)
            inc.delaySec >= 90 || inc.cat.contains("jam", true) -> 1
            else -> 0
        }
        if (level == 0) continue
        var endOff = startOff + 250.0
        if (inc.pts.size > 1) {
            val last = inc.pts.last()
            var bd = 70.0
            for (n in r.pts.indices) {
                if (r.cum[n] < startOff - 50) continue
                val d = Geo.dist(r.pts[n], last)
                if (d < bd) { bd = d; endOff = r.cum[n] }
            }
        }
        if (endOff < startOff) endOff = startOff + 250.0
        for (i in r.sev.indices) if (r.cum[i] >= startOff - 20 && r.cum[i] <= endOff + 20) r.sev[i] = max(r.sev[i], level)
    }
}

internal fun NavEngine.p0Speed(): Double = fix?.speed ?: 0.0

/** Every few minutes while driving, asks for fresh routes and offers a different one if it saves a few minutes. */
internal fun NavEngine.checkFaster(cur: Route, remainingSecs: Double) {
    val now = System.currentTimeMillis()
    val f = fix ?: return
    val d = dest ?: return
    val live = prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)
    if (previewing || simMode != 0 || faster != null || !prefs.b(PreferenceKeys.FASTER_ROUTE, true) || shadow()) return
    // live: every 2 minutes up to the last 5 minutes, in slow traffic too (a jam is when another way matters most)
    if (remainingSecs < (if (live) 300 else 900) || now - lastFasterCheck < (if (!live) 240_000L else if (f.speed < 0.5) 360_000L else 120_000L) || (!live && f.speed < 3.0)) return
    lastFasterCheck = now
    val ahead = cur.pts.drop(lastIdx.coerceIn(0, max(0, cur.pts.size - 2))).filterIndexed { i, _ -> i % 3 == 0 }
    val viaPts = via.map { it.pos }
    scope.launch {
        try {
            syncRoutePrefs(f.pos, d.pos)
            // live: from both route services when there are keys for both (each finds ways the other misses)
            val rs = withContext(Dispatchers.IO) { netRoutes(f.pos, d.pos, true, 0L, viaPts, driveHeading(f.pos), askBoth = live) }
            if (rs.size < 2 || route !== cur) return@launch
            // how much of each candidate runs along the road we are already following
            fun overlap(r: Route): Double {
                val s = r.pts.filterIndexed { i, _ -> i % 10 == 0 }
                if (s.isEmpty()) return 0.0
                return s.count { p -> ahead.any { Geo.dist(p, it) < 40.0 } }.toDouble() / s.size
            }
            val same = rs.maxByOrNull { overlap(it) } ?: return@launch
            try { refreshTraffic(cur, same) } catch (e: Exception) { android.util.Log.w("NavEngine", "traffic refresh failed", e) }
            if (live) {
                // live comparison: every other way stays on the map in grey with how it compares, to tap any time
                val others = rs.filter { it !== same && overlap(it) < 0.8 }.take(2)
                val list = withContext(Dispatchers.Default) { altsFor(cur, same, others, curProgress) }
                if (route !== cur) return@launch
                liveAlts = list
                map.setLiveAlts(list.map { it.line() }, list.map { it.bubble }, list.map { it.diffMin }, list.map { it.traffic })
                val best = list.minByOrNull { it.diffMin }
                // a slower way you picked yourself: a quicker one only as a card to take or leave (7 s), never switched to or said
                if (pickedSlower) {
                    if (best != null && best.diffMin <= -3 && liveSaid.add(best.diffMin / 2)) offerFaster(best.route, -best.diffMin)
                    return@launch
                }
                if (best != null && autoOn() && -best.diffMin >= autoMin()) {
                    switchToAlt(list.indexOf(best), auto = true)
                    announceAuto(cur, -best.diffMin)
                    return@launch
                }
                if (best != null && best.diffMin <= -3 && liveSaid.add(best.diffMin / 2)) speak(Lang.t("Eine schnellere Route spart ${-best.diffMin} Minuten. Tippe sie auf der Karte an, um zu wechseln.", "A faster route is available, saving ${-best.diffMin} minutes. Tap it on the map to switch."), true)
                return@launch
            }
            val best = rs.filter { it !== same && overlap(it) < 0.8 }.minByOrNull { it.time } ?: return@launch
            val saved = ((same.time - best.time) / 60.0).toInt()
            if (pickedSlower) { if (saved >= 3 && best.time < same.time * 0.95) offerFaster(best, saved); return@launch }
            if (autoOn() && saved >= autoMin()) {
                routes = listOf(best); active = 0
                said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
                drawRoutes()
                updateRouteZones()
                fix?.let { onPosition(it, true) }
                announceAuto(cur, saved)
                return@launch
            }
            if (saved >= 3 && best.time < same.time * 0.95) {
                faster = FasterRoute(best, saved)
                speak(Lang.t("Eine schnellere Route spart $saved Minuten", "A faster route is available, saving $saved minutes"), true)
                val shown = faster
                delay(25_000)
                if (faster === shown) { faster = null; lastFasterCheck = System.currentTimeMillis() + 300_000L }
            }
        } catch (e: Exception) { softFail(e) }
    }
}

/** Every few minutes asks TomTom for the same trip again, so the amber/red road and the time left follow the traffic as it changes. */
internal fun NavEngine.checkTraffic(cur: Route) {
    val now = System.currentTimeMillis()
    val f = fix ?: return
    val d = dest ?: return
    if (previewing || simMode != 0 || trafficBusy || !hasOnline || shadow()) return
    if (lastTrafficAt == 0L) { lastTrafficAt = now - 120_000L }   // first refresh about a minute after setting off
    // in a queue (crawling, under about 20 km/h) the traffic is asked for every 2 minutes, so the arrival time catches up with a jam sooner
    val crawling = (fix?.speed ?: 99.0) < 5.5
    if (now - lastTrafficAt < (if (crawling) 120_000L else 180_000L)) return
    lastTrafficAt = now
    trafficBusy = true
    val viaPts = via.map { it.pos }
    val at = curProgress
    scope.launch {
        try {
            syncRoutePrefs(f.pos, d.pos)
            val rs = withContext(Dispatchers.IO) { netRoutes(f.pos, d.pos, false, 0L, viaPts, driveHeading(f.pos), true) }
            val fresh = rs.firstOrNull() ?: return@launch
            if (route !== cur) return@launch
            // on a Mapbox route with a TomTom key, TomTom times the same road too: each stretch keeps the worse of the two
            val others = ArrayList<Route>()
            if (provider == "mapbox" && apiKey.isNotBlank() && !prefs.b(PreferenceKeys.ROUTE_ECO, false)) {
                val tt: Route? = try { withContext(Dispatchers.IO) { routesFrom("tomtom", f.pos, d.pos, false, 0L, viaPts, driveHeading(f.pos), true).firstOrNull() } } catch (e: Exception) { null }
                tt?.let { others += it }
            }
            refreshTraffic(cur, fresh, at, others)
        } catch (e: Exception) { softFail(e) }
        finally { trafficBusy = false }
    }
}

/** Copies the latest traffic colouring from a fresh request for the same road onto the route being driven, so amber and red stay current. */
private fun NavEngine.refreshTraffic(cur: Route, fresh: Route, at: Double = curProgress, others: List<Route> = emptyList()) {
    if (fresh.sev.isEmpty() || cur.sev.isEmpty()) return
    fun key(p: LL) = Math.round(p.lat * 1e4) * 1000003L + Math.round(p.lon * 1e4)
    val m = HashMap<Long, Int>(fresh.sev.size * 2)
    for (i in fresh.sev.indices) m[key(fresh.pts[i])] = fresh.sev[i]
    // the route service's own traffic for this route, kept from before anything was painted on it: a refresh starts from that
    // (plus the fresh answer), never from the painted line, or a jam or incident that had cleared stayed red for good
    if (svcSevRoute !== cur || svcSev?.size != cur.sev.size) {
        svcSev = (if (sevBaseRoute === cur && sevBase?.size == cur.sev.size) sevBase!! else cur.sev).copyOf(); svcSevRoute = cur
    }
    val own = svcSev!!
    var hit = 0
    val base = IntArray(cur.sev.size)
    for (i in cur.sev.indices) { val v = m[key(cur.pts[i])]; if (v != null) { base[i] = v; hit++ } else base[i] = own[i] }
    // the other service's view of the same stretch: the worse of the two counts
    for (o in others) {
        if (o.sev.isEmpty()) continue
        val mo = HashMap<Long, Int>(o.sev.size * 2)
        for (i in o.sev.indices) mo[key(o.pts[i])] = o.sev[i]
        for (i in cur.sev.indices) { val v = mo[key(cur.pts[i])]; if (v != null) base[i] = max(base[i], v) }
    }
    // only trust it when it is the same road: a good share of the points still ahead must line up with the fresh route
    val firstAhead = max(0, idxAt(cur, at))
    var ahead = 0; var match = 0
    for (i in firstAhead until cur.sev.size) { ahead++; if (m.containsKey(key(cur.pts[i]))) match++ }
    if (ahead > 0 && match < ahead * 0.6) return
    // fresh offsets count from where you were when it was asked, so shift them onto the full route
    if (fresh.len > 0) {
        cur.freeFlow = max(0.0, fresh.time - fresh.delay) / fresh.len * cur.len
        cur.delays = fresh.delays.map { DelaySeg(it.off + at, it.end + at, it.secs) }
    }
    sevBase = base.copyOf(); sevBaseRoute = cur
    System.arraycopy(base, 0, cur.sev, 0, base.size)
    try { applyIncidentSev(cur, incMatches) } catch (e: Exception) { softFail(e) }
    lastDraw = 0L
}

/** The quicker way as a card at the top (phone and car) to take or leave; it goes again by itself after 7 seconds. */
private fun NavEngine.offerFaster(r: Route, saved: Int) {
    val fr = FasterRoute(r, saved)
    faster = fr
    scope.launch { delay(7_000); if (faster === fr) { faster = null; lastFasterCheck = System.currentTimeMillis() } }
}

fun NavEngine.acceptFaster() {
    val fr = faster ?: return
    routes = listOf(fr.route); active = 0
    said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
    faster = null
    drawRoutes()
    toast(Lang.t("Auf die schnellere Route gewechselt", "Switched to the faster route"), 2500)
}
