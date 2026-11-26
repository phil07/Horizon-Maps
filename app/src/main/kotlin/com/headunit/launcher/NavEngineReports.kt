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
import com.headunit.launcher.NavEngine.Report
import com.headunit.launcher.NavEngine.VotePrompt

// Part of NavEngine (moved out of NavEngine.kt to keep that file workable): these are extension functions on the engine,
// so they are called exactly as before.

/** Your own reports and the shared ones from others (each shown once: one that came back from the server as yours is left out). */
internal fun NavEngine.allReports(): List<Report> {
    val own = reportsActive()
    val shared = sharedShown
    if (shared.isEmpty() || !SharedReports.sharing(prefs)) return own
    val mine = own.mapTo(HashSet()) { it.sid }
    val now = System.currentTimeMillis()
    val out = ArrayList<Report>(own)
    for (s in shared) if (s.exp > now && !s.mine && ReportKind.of(s.kind) != null && s.id !in mine && !SharedReports.isOwn(s.id) && !SharedReports.isGone(s.id))
        out += Report(s.kind, s.pos, s.t, s.id, shared = true)
    return out
}

/** Fetches the shared reports every 2 minutes, and at once when the route changes (a new one, a reroute, or none any more). */
private fun NavEngine.refreshShared(p: Fix) {
    // sharing off (or no server): no requests at all, and nothing shared shown
    if (!SharedReports.sharing(prefs)) { if (sharedShown.isNotEmpty()) { sharedShown = emptyList(); SharedReports.shownCount = 0 }; return }
    val now = System.currentTimeMillis()
    val r = route
    if (sharedBusy || (r === sharedRoute && now - sharedAt < 120_000L)) return
    sharedBusy = true; sharedAt = now; sharedRoute = r
    val car = p.pos
    scope.launch {
        try {
            val found = withContext(Dispatchers.IO) { try { fetchShared(car, r) } catch (e: Exception) { null } }
            if (found != null) { sharedShown = found; SharedReports.shownCount = found.size; policeSig = 0 }
        } finally { sharedBusy = false }
    }
}

/**
 * The shared reports within 30 km of the car or within about 1 km of the route. The area asked for is the car's 30 km box and
 * the route's box (1 km wider) together; when that is more than the server's 6 degrees a side, the route is asked for in parts
 * instead. Null when every request failed.
 */
private fun NavEngine.fetchShared(car: LL, r: Route?): List<SharedReports.Item>? {
    fun padLon(lat: Double, m: Double) = m / (111_320.0 * max(0.15, cos(Math.toRadians(lat))))
    fun grow(b: DoubleArray, m: Double): DoubleArray {
        val dLat = m / 111_320.0
        val dLon = padLon(if (abs(b[0]) > abs(b[2])) b[0] else b[2], m)
        return doubleArrayOf((b[0] - dLat).coerceAtLeast(-90.0), (b[1] - dLon).coerceAtLeast(-180.0), (b[2] + dLat).coerceAtMost(90.0), (b[3] + dLon).coerceAtMost(180.0))
    }
    // boxes as south, west, north, east
    val boxes = ArrayList<DoubleArray>()
    boxes += grow(doubleArrayOf(car.lat, car.lon, car.lat, car.lon), 30_000.0)
    if (r != null && r.pts.size >= 2) {
        var cur: DoubleArray? = null
        var prev: LL? = null
        for (q in r.pts) {
            // across the 180th meridian the box would wrap the whole world: close the part here and start a new one
            if (prev != null && abs(q.lon - prev.lon) > 180.0) { cur?.let { boxes += grow(it, 1_000.0) }; cur = null }
            val b = cur
            if (b == null) { cur = doubleArrayOf(q.lat, q.lon, q.lat, q.lon); prev = q; continue }
            val s = min(b[0], q.lat); val w = min(b[1], q.lon); val n = max(b[2], q.lat); val e = max(b[3], q.lon)
            if (n - s > 4.0 || e - w > 4.0) {
                // this part is full: the next one starts at the last point, so the stretch in between is covered too
                boxes += grow(b, 1_000.0)
                val pv = prev ?: q
                cur = doubleArrayOf(min(pv.lat, q.lat), min(pv.lon, q.lon), max(pv.lat, q.lat), max(pv.lon, q.lon))
            } else { b[0] = s; b[1] = w; b[2] = n; b[3] = e }
            prev = q
        }
        cur?.let { boxes += grow(it, 1_000.0) }
    }
    val all = doubleArrayOf(boxes.minOf { it[0] }, boxes.minOf { it[1] }, boxes.maxOf { it[2] }, boxes.maxOf { it[3] })
    val ask = if (all[2] - all[0] <= 6.0 && all[3] - all[1] <= 6.0) listOf(all) else boxes.take(16)
    val byId = LinkedHashMap<Long, SharedReports.Item>()
    var anyOk = false
    for (b in ask) {
        val got = SharedReports.fetch(prefs, b[0], b[1], b[2], b[3]) ?: continue
        anyOk = true
        for (x in got) byId[x.id] = x
    }
    if (!anyOk) return null
    SharedReports.remember(byId.values.toList())
    // the route as points about every 300 m, in a coarse grid (cells of roughly 2 km) so each report only checks the route nearby
    val grid = HashMap<Long, ArrayList<LL>>()
    fun key(lat: Double, lon: Double) = Math.floor(lat / 0.02).toLong() * 1000003L + Math.floor(lon / 0.03).toLong()
    if (r != null) {
        var last = -1e9
        for (n in r.pts.indices) if (n < r.cum.size && (r.cum[n] - last >= 300.0 || n == r.pts.size - 1)) {
            val q = r.pts[n]
            grid.getOrPut(key(q.lat, q.lon)) { ArrayList() }.add(q)
            last = r.cum[n]
        }
    }
    val out = ArrayList<SharedReports.Item>()
    for (x in byId.values) {
        if (x.mine) continue
        if (Geo.dist(car, x.pos) <= 30_000.0) { out += x; continue }
        if (grid.isEmpty()) continue
        val k = key(x.pos.lat, x.pos.lon)
        var near = false
        // (points 300 m apart: within 1.15 km of one is within about 1 km of the line between them)
        for (dy in -1..1) for (dx in -1..1) {
            if (!near && grid[k + dy * 1000003L + dx]?.any { q -> Geo.dist(q, x.pos) <= 1_150.0 } == true) near = true
        }
        if (near) out += x
    }
    return out
}

/** "Noch da" (true) or "Weg" (false) for the report in [votePrompt]. "Weg" takes it off the map at once. */
fun NavEngine.answerVote(still: Boolean) {
    val vp = votePrompt ?: return
    votePrompt = null
    if (!still) { SharedReports.markGone(vp.id); policeSig = 0; pushHazards(hzProgress) }
    scope.launch(Dispatchers.IO) { SharedReports.vote(prefs, vp.id, still) }
}

/** Closes the "noch da?" question without answering. */
fun NavEngine.dismissVote() { votePrompt = null }

/**
 * How many kinds of source can report traffic here at all: the drivers' reports (when sharing is on), Mapbox and TomTom traffic
 * (with their keys), and the Autobahn GmbH (in Germany). With fewer than three, an older report isn't doubted for having no second source.
 */
private fun NavEngine.sourcesPossible(): Int =
    (if (SharedReports.sharing(prefs)) 1 else 0) + (if (hasMapbox) 1 else 0) + (if (hasKey) 1 else 0) + (if (detectedCountry == "DE") 1 else 0)

/**
 * How many sources say what [r] says: the report itself, a "still there" vote from another driver, a second report of the same
 * kind within 300 m, and an incident of the traffic feeds (Autobahn GmbH, Mapbox, TomTom) of the same kind within 600 m.
 */
private fun NavEngine.reportSources(r: Report, list: List<Report>): Int {
    fun related(c: String) = when (r.kind) {
        "jam" -> c.contains("jam", true)
        "roadworks" -> c.contains("work", true) || c.contains("laneclosed", true)
        "accident" -> c.contains("accident", true)
        "hazard" -> c.contains("danger", true)
        "closed" -> isClosure(c)
        else -> false
    }
    var n = 1
    if (r.shared && (sharedShown.firstOrNull { it.id == r.sid }?.up ?: 0) > 0) n++
    if (list.any { it !== r && it.kind == r.kind && Geo.dist(it.pos, r.pos) < 300.0 }) n++
    if (incMatches.any { (inc, _, _) -> related(inc.cat) && inc.pts.any { Geo.dist(it, r.pos) < 600.0 } }) n++
    return n
}

/** Refreshes the map icons when reports change or expire, and warns when one is just ahead. */
internal fun NavEngine.checkPolice(p: Fix) {
    try { refreshShared(p) } catch (e: Exception) { softFail(e) }
    val list = allReports()
    var sig = list.size
    for (r in list) sig = sig * 31 + (r.t % 100000).toInt() + (r.sid % 100000).toInt()
    if (sig != policeSig) { policeSig = sig; pushHazards(hzProgress) }
    // passing someone else's report: is it still there? (asked once per report, only while moving)
    if (votePrompt == null && p.speed > 3.0 && SharedReports.sharing(prefs)) {
        val hit = list.firstOrNull { it.shared && it.sid !in votePrompted && Geo.dist(p.pos, it.pos) < 150.0 }
        if (hit != null) {
            votePrompted.add(hit.sid)
            val vp = VotePrompt(hit.sid, hit.kind, ReportKind.orPolice(hit.kind).label)
            votePrompt = vp
            // a jam or a closure driven through at 50 km/h or more is gone: left unanswered, it is voted "weg" by itself
            val fast = p.speed >= 14.0 && hit.kind in setOf("jam", "closed")
            scope.launch {
                delay(12_000)
                if (votePrompt === vp) {
                    if (fast && (fix?.speed ?: 0.0) >= 11.0) answerVote(false) else votePrompt = null
                }
            }
        }
    }
    if (!prefs.b(PreferenceKeys.POLICEALERTS, true) || p.speed < 4.0) return
    for (r in list) {
        val d = Geo.dist(p.pos, r.pos)
        if (d < 40 || d > 600) continue
        // weighed by its sources: a fresh report counts alone. One that has run most of its life (70 %) is only announced when another
        // source says the same, because the others (a driver's vote, a second report, a traffic feed) would have it too, but only where
        // there are other sources at all: the fewer there are here, the more the one that exists is trusted
        if (System.currentTimeMillis() - r.t > ReportKind.orPolice(r.kind).lifeMs * 7 / 10 && sourcesPossible() >= 3 && reportSources(r, list) < 2) continue
        val diff = abs(((Geo.bearing(p.pos, r.pos) - p.heading + 540.0) % 360.0) - 180.0)
        // (a shared report is remembered by its server id: two drivers' reports can carry the same time)
        if (diff < 50 && policeAlerted.add(if (r.shared) -r.sid else r.t)) {
            chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
            raiseAlert(if (r.kind == "mobilecam") "camera" else "report", Lang.f("%1\$s voraus gemeldet", "%1\$s reported ahead", ReportKind.orPolice(r.kind).label))
            speak(Lang.f("%1\$s voraus gemeldet", "%1\$s reported ahead", ReportKind.orPolice(r.kind).label), true)
        }
    }
}

/** Pushes the camera, roadworks and closure icons for what is still ahead on the route (only when something changed). */
internal fun NavEngine.pushHazards(progress: Double) {
    hzProgress = progress
    val list = ArrayList<Pair<LL, Int>>()
    if (prefs.b(PreferenceKeys.HAZARD_ICONS, true)) {
        for (r in allReports()) list += r.pos to ReportKind.orPolice(r.kind).mapIcon
        for ((inc, off, _) in incMatches) {
            if (off <= progress - 20) continue
            val k = when (inc.cat.lowercase()) { "roadclosed" -> 2; "roadworks", "laneclosed" -> 1; else -> -1 }
            if (k >= 0) list += inc.pts.first() to k
        }
        // Spain: the DGT's incidents within 10 km round you, with or without a route (the V16 beacons as a broken-down car)
        val here = fix?.pos
        if (here != null && countryCode() == "ES") for (inc in DgtRest.near(here, 10_000.0)) {
            val k = when (inc.cat) { "roadClosed" -> 2; "roadWorks", "laneClosed" -> 1; "accident" -> 4; "danger" -> 5; "breakdown" -> 6; "jam" -> 8; else -> -1 }
            if (k >= 0) list += inc.pts.first() to k
        }
        // the Netherlands: NDW's events within 10 km round you, the same icons
        if (here != null && countryCode() == "NL") for (inc in NdwRest.near(here, 10_000.0)) {
            val k = when (inc.cat) { "roadClosed" -> 2; "roadWorks", "laneClosed" -> 1; "accident" -> 4; "danger" -> 5; "breakdown" -> 6; "jam" -> 8; else -> -1 }
            if (k >= 0) list += inc.pts.first() to k
        }
    }
    var sig = list.size
    for ((p, k) in list) sig = sig * 31 + (p.lat * 1e5).toInt() * 7 + (p.lon * 1e5).toInt() + k
    if (sig == hazardSig) return
    hazardSig = sig
    map.setHazards(list)
}

internal fun NavEngine.checkYieldSigns(p: LL, heading: Double) {
    val last = yieldAt
    if (last == null || Geo.dist(last, p) > 300.0) {
        yieldAt = p
        Thread {
            yieldNear = try { Overpass.yieldSigns(p.lat - 0.005, p.lon - 0.007, p.lat + 0.005, p.lon + 0.007) } catch (e: Exception) { emptyList() }
        }.start()
    }
    // stop signs and "give way" signs have their own switches; both are off by default
    val stopOn = prefs.b(PreferenceKeys.STOP_SIGNS, false)
    val giveWayOn = prefs.b(PreferenceKeys.GIVE_WAY, false)
    val hit = yieldNear.firstOrNull { (s, stop) ->
        (if (stop) stopOn else giveWayOn) && s !in yieldAlerted && Geo.dist(p, s) < 200.0 && abs(angDiff(Geo.bearing(p, s), heading)) <= 25.0
    } ?: return
    yieldAlerted.add(hit.first)
    val text = if (hit.second) "Stoppschild voraus" else "Vorfahrt gewähren"
    raiseAlert("yield", text)
    speak(text, alert = true)
}

/** Autobahn GmbH's roadworks, warnings and closures on the motorways of [r]. Asked for when the route starts and every 5 minutes. */
internal fun NavEngine.autobahnIncidents(r: Route): List<Incident> {
    if (!prefs.b(PreferenceKeys.INCIDENTS, true)) return emptyList()
    val roads = r.steps.mapNotNull { AutobahnRest.roadOf(it.street) }.distinct()
    if (roads.isEmpty()) {
        // no motorway on the route now: nothing from it, and its closures may go (see noteClosures)
        abItems = emptyList(); abOk = true; abRoads = ""
        return emptyList()
    }
    val key = roads.joinToString(",")
    val now = System.currentTimeMillis()
    if (key != abRoads || now - abAt > 300_000L) {
        abAt = now; abRoads = key
        scope.launch {
            val res = withContext(Dispatchers.IO) { try { AutobahnRest.incidents(roads) } catch (e: Exception) { null } }
            abOk = res != null; abItems = res ?: emptyList()
        }
    }
    return abItems
}

/**
 * Spain: the DGT's incidents (DgtRest). Looked at every 15 minutes while you are in Spain and incidents are switched on; read again
 * only when the DGT has changed the file. The map's icons round you are brought up to date every minute.
 */
internal fun NavEngine.dgtTick() {
    val cc = countryCode()
    if (!prefs.b(PreferenceKeys.INCIDENTS, true) || (cc != "ES" && cc != "NL")) return
    scope.launch {
        val changed = withContext(Dispatchers.IO) { if (cc == "ES") DgtRest.refreshIfDue() else NdwRest.refreshIfDue() }
        if (changed) hazardSig = -1
        // with a route, the route's check draws them (and gives the progress); without one, they are drawn here
        if (route == null) pushHazards(Double.MAX_VALUE) else if (changed) pushHazards(hzProgress)
    }
}
