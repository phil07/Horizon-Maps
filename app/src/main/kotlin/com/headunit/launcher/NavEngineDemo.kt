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

// Part of NavEngine (moved out of NavEngine.kt to keep that file workable): these are extension functions on the engine,
// so they are called exactly as before.

// ---- demo drive ----
// a tap on the demo's speed: 1x, 2x, 4x and back to 1x (it keeps going, no restart)
fun NavEngine.cycleSim() {
    val r = route
    if (r == null) { toast(ctx.Lang.t("Wähle zuerst ein Ziel, dann starte die Demofahrt", "Pick a destination first, then start the demo drive")); return }
    when (simMode) {
        0 -> { simMode = 1; startSim(r) }
        1 -> simMode = 2
        2 -> simMode = 4
        else -> simMode = 1
    }
}

/** Jumps the demo to about 250 m before the next junction (the next turn keeps its place on the route). */
fun NavEngine.skipSimTurn() {
    val r = route ?: return
    if (simMode == 0) return
    val t = turn ?: return
    if (t.dist > 250.0) simDist = min(r.len, simDist + t.dist - 250.0)
}

fun NavEngine.beginDemo() {
    if (!previewing || route == null) return
    demoOnStart = true
    beginNavigation()
}

private fun NavEngine.startSim(r: Route) {
    simJob?.cancel()
    simDist = fix?.let { snap(r, it.pos).progress } ?: 0.0
    following = true
    simJob = scope.launch {
        while (true) {
            val cur = route ?: break
            // the position moves by exactly the speed it reports, so the arrow glide never sees a gap it has to jump to close
            // (the old demo moved 16 m a second while claiming 7 m/s near a turn, which the glide "corrected" with a jump halfway round the roundabout)
            val td = turn?.dist ?: 1000.0
            val demoSpeed = (if (td < 150) 7.0 else if (td < 400) 13.0 else if (td < 1200) 20.0 else 27.0) * simMode
            simDist = min(cur.len, simDist + demoSpeed)
            val (pt, hd) = pointAt(cur, simDist)
            onPosition(Fix(pt, hd, demoSpeed, 0.0), false)
            if (simDist >= cur.len) break
            delay(1000)
        }
        simMode = 0
    }
}

fun NavEngine.stopSim() { simJob?.cancel(); simJob = null; simMode = 0 }

/**
 * A road name from the lookup round you. A different name only shows once a second lookup agrees, or straight away if you've
 * clearly turned (40 degrees or more), so it no longer jumps to a side road for a moment at junctions. An empty name (no road
 * found, like a car park) clears it.
 */
internal fun NavEngine.noteRoad(name: String, heading: Double) {
    val now = System.currentTimeMillis()
    val turned = heading >= 0 && limitRoadHead >= 0 && abs(((heading - limitRoadHead + 540.0) % 360.0) - 180.0) >= 40.0
    if (name == limitRoad || name.isBlank() || limitRoad.isBlank() || turned || name == roadCandidate) {
        limitRoad = name; roadCandidate = ""
        limitRoadAt = now; if (heading >= 0) limitRoadHead = heading
    } else roadCandidate = name
    refreshRoad()
}

/** Name of the road you are on: from the route while navigating, otherwise from the map data around you. */
internal fun NavEngine.refreshRoad() {
    // the map-data name is dropped once it's 45 seconds old (lookups that kept failing used to leave the last road up)
    val mapRoad = if (System.currentTimeMillis() - limitRoadAt < 45_000L) limitRoad else ""
    // stopped: the name stays for a minute, so it doesn't swap every time you slow down
    val held = stillRoadSince > 0L && System.currentTimeMillis() - stillRoadSince < 60_000L && roadShown.isNotBlank()
    val n = if (held) roadShown else Geo.shortRoad(if (route != null && !previewing && onRoad.isNotBlank()) onRoad else mapRoad)
    if (n != roadShown) { roadShown = n; map.setRoadName(n) }
    if (roadNow != n) roadNow = n
}

internal fun NavEngine.laneHit(dir: String, maneuver: String): Boolean {
    val m = maneuver.uppercase()
    return when {
        m.contains("U_TURN") -> dir.endsWith("U_TURN")
        m.contains("LEFT") -> dir.contains("LEFT")
        m.contains("RIGHT") -> dir.contains("RIGHT")
        else -> dir == "STRAIGHT"
    }
}

internal fun NavEngine.updateLaneMarks(r: Route, cur: Step, sec: LaneSec?, d: Double) {
    val key = if (sec != null && d < (if (laneFast) 1300.0 else 400.0) && prefs.b(PreferenceKeys.LANEMAP, true)) Pair(cur.off, sec) else null
    // the route line stays in its lane until you're past the end of its ease back after the junction (clearing it as soon as
    // the lane guidance ended made the line just ahead of you jump sideways)
    if (laneShiftEnd > 0.0 && key == null && curProgress >= laneShiftEnd) { map.setRouteShift(emptyList()); laneLatAt = null; laneShiftEnd = 0.0; turnArrowKey = -2 }
    // where Mapbox has HD roads (the lanes it paints on motorways), the real lanes are read off the map just before the junction,
    // so the lanes drawn and the route line sit exactly on the painted ones (asked every 3 s until found, up to 8 times)
    if (key != null && hdLanesFor != cur.off) {
        if (hdTriesFor != cur.off) { hdTriesFor = cur.off; hdTries = 0 }
        val now = android.os.SystemClock.uptimeMillis()
        if (hdTries < 8 && now - hdAskAt > 3000L && d < 700.0) {
            hdAskAt = now; hdTries++
            val askOff = cur.off
            val nLanes = sec?.lanes?.size ?: 0
            // several places across the approach: close to the junction the exit lanes have often already split off behind
            // the hatched area, so the lanes are looked at further back too, and followed from one place to the next
            val backs = listOf(150.0, 120.0, 90.0, 60.0, 35.0, 15.0).filter { cur.off - it > 0.0 }
            map.laneCentresAlong(backs.map { pointAt(r, cur.off - it) }) { res, lines ->
                if (hdTriesFor != askOff) return@laneCentresAlong
                val prof = ArrayList<Pair<Double, List<Double>>>()
                res?.forEachIndexed { k, offs -> pickLaneWindow(offs, nLanes)?.let { prof += (askOff - backs[k]) to it } }
                laneDiag = when {
                    res == null -> "Lane data: the junction isn't on screen yet"
                    lines == 0 -> "Lane data: no HD lanes from Mapbox here (lanes laid out from the road's line)"
                    prof.isEmpty() -> "Lane data: HD lanes found ($lines lines) but they didn't match the $nLanes lanes listed (found " + res.joinToString(", ") { it.size.toString() } + " across the road)"
                    else -> "Lane data: HD lanes lined up at ${prof.size} of ${backs.size} points along the approach"
                }
                if (res != null && (prof.size >= 3 || hdTries >= 8) && laneDiagFor != askOff && prefs.b(PreferenceKeys.DEMO, false) && prefs.b(PreferenceKeys.HD_DIAG, false)) { laneDiagFor = askOff; toast(laneDiag, 5000) }
                // a better read (more places matched) replaces the last; it keeps looking until at least 3 places match
                if (prof.isNotEmpty() && (hdProfileFor != askOff || prof.size > hdProfile.size)) { hdProfile = prof.sortedBy { it.first }; hdProfileFor = askOff; laneKey = null }
                if (prof.size >= 3) hdLanesFor = askOff
            }
        }
    }
    // the lane sign goes once you're up to it
    if (laneSignFor >= 0.0 && curProgress > laneSignFor - 15.0) { map.setLaneSign(null, emptyList()); laneSignFor = -1.0 }
    // the lane bubble rides just ahead of the arrow until you reach the junction
    bubbleLanes?.let { bl ->
        if (key != null && curProgress < cur.off - 20.0) map.setLaneBubble(pointAt(r, min(r.len, curProgress + 24.0)).first, bl) else map.setLaneBubble(null, emptyList())
    }
    if (key == laneKey) return
    laneKey = key
    if (key == null || sec == null) {
        map.setLaneSign(null, emptyList()); laneSignFor = -1.0
        bubbleLanes = null; map.setLaneBubble(null, emptyList())
        map.setLanes(emptyList())
        if (laneShiftEnd <= 0.0 || curProgress >= laneShiftEnd) { map.setRouteShift(emptyList()); laneLatAt = null; laneShiftEnd = 0.0 }
        return
    }
    val n = sec.lanes.size
    val anyFollow = sec.lanes.any { it.follow != null }
    val activeIdx = sec.lanes.indices.filter { i -> val ln = sec.lanes[i]; if (anyFollow) ln.follow != null else ln.dirs.any { laneHit(it, cur.maneuver) } }
    // Lined up with the real lanes (the ones Mapbox's map paints where it has them): the route line runs down the middle of the
    // road as mapped, so the lanes are laid out from there, at real lane widths. On a one-way road or a dual carriageway the
    // lanes are centred on the line; on a two-way road they're all on your side of it (the left in the UK).
    val (approach, approachB) = pointAt(r, max(0.0, cur.off - 45.0))
    val road = map.roadAt(approach, approachB, maxDistance = 10.0, headingTolerance = 25.0)
    // Require a close, direction-compatible road match here: at exits and junctions a nearby ramp or cross-road can
    // otherwise supply the wrong one-way/road-centre metadata and offset the highlighted lane away from the route.
    // If the match is ambiguous, use the lane-relative fallback below so the route remains in the selected lane.
    // (with no road data from the map the old way is used: the lanes laid out with the lane to be in under the route line)
    // Only lined up with the road on one-way motorway and trunk carriageways (not slip roads), where the mapped line really
    // runs down the middle of the lanes. On ordinary A roads and town junctions the lanes often widen or gain a turning lane
    // that the mapped line doesn't follow, which put the lanes (and the route line moved into them) off the road; there the
    // lanes are laid out with the lane to be in under the route line, and the route line stays where it is.
    val big = road?.cls?.let { (it == "motorway" || it == "trunk") } == true
    val aligned = road != null && road.oneway == true && big
    val laneW = if (!aligned) 3.6 else if (big) 3.65 else 3.5
    // Lined up: before a junction the mapped line runs down the middle of the main carriageway, which is the lanes that carry
    // straight on. The lanes peeling off to an exit are extra lanes beside it (at the junction Mapbox lists them all together),
    // so the straight-on lanes are centred on the line and the exit lanes sit off to their side. Centring all of them on the
    // line put every lane a lane or two out towards the exit, over the verge. A true split (no straight-on lane) stays centred.
    val throughIdx = sec.lanes.indices.filter { i -> sec.lanes[i].dirs.any { it == "STRAIGHT" } }
    val centre = if (!aligned) (if (activeIdx.isEmpty()) (n - 1) / 2.0 else activeIdx.average())
        else if (throughIdx.isNotEmpty() && throughIdx.size < n && isContiguous(throughIdx)) throughIdx.average()
        else (n - 1) / 2.0
    // each lane's middle and edges, sideways from the route line (metres, + to the right): the real ones read off the map when
    // they match the lane list, else laid out at standard widths as described above
    // Lanes are only painted on the road (and the route line moved into them) once the real lanes have been read off the
    // map and matched at 3 or more places along the approach. Everywhere else a lane sign stands over the road at the junction
    // instead, like a motorway gantry, so nothing is drawn that might not sit on the painted lanes.
    val prof = if (hdLanesFor == cur.off && hdProfileFor == cur.off) hdProfile.filter { it.second.size == n } else emptyList()
    val lined = prof.isNotEmpty()
    if (!lined && curProgress < cur.off - 15.0) {
        map.setLaneSign(pointAt(r, max(0.0, cur.off - 10.0)).first, sec.lanes.mapIndexed { i, lane ->
            val active = i in activeIdx
            lane.dirs.map { dir -> dir to (active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))) } to active
        })
        laneSignFor = cur.off
        bubbleLanes = null
    } else {
        map.setLaneSign(null, emptyList()); laneSignFor = -1.0
        bubbleLanes = if (lined) sec.lanes.mapIndexed { i, lane ->
            val active = i in activeIdx
            lane.dirs.map { dir -> dir to (active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))) } to active
        } else null
    }
    // each lane's middle at [o] metres along the route: from the real lanes (in between the places they were read, and held
    // at the first / last beyond them), else the standard layout
    fun mids(o: Double): DoubleArray {
        if (prof.isEmpty()) return DoubleArray(n) { i -> (i - centre) * laneW }
        if (o <= prof.first().first) return prof.first().second.toDoubleArray()
        if (o >= prof.last().first) return prof.last().second.toDoubleArray()
        val k = prof.indexOfLast { it.first <= o }
        val (oa, a) = prof[k]; val (ob, b) = prof[k + 1]
        val t = if (ob > oa) (o - oa) / (ob - oa) else 0.0
        return DoubleArray(n) { i -> a[i] + (b[i] - a[i]) * t }
    }
    fun leftE(m: DoubleArray, i: Int) = if (prof.isEmpty()) m[i] - laneW / 2 else if (i > 0) (m[i - 1] + m[i]) / 2 else m[0] - (m[1] - m[0]) / 2
    fun rightE(m: DoubleArray, i: Int) = if (prof.isEmpty()) m[i] + laneW / 2 else if (i < n - 1) (m[i] + m[i + 1]) / 2 else m[n - 1] + (m[n - 1] - m[n - 2]) / 2
    fun shifted(p: LL, brg: Double, lateral: Double): LL {
        val rad = Math.toRadians(brg + 90.0)
        return LL(p.lat + lateral * Math.cos(rad) / 111320.0, p.lon + lateral * Math.sin(rad) / (111320.0 * Math.cos(Math.toRadians(p.lat))))
    }
    val marks = ArrayList<MapBinding.LaneMark>()
    for (back in if (lined) listOf(28.0, 65.0, 105.0) else emptyList()) {
        val off = cur.off - back
        if (off < 0) continue
        val (p, brg) = pointAt(r, off)
        val m = mids(off)
        sec.lanes.forEachIndexed { i, lane ->
            val at = shifted(p, brg, m[i])
            val active = i in activeIdx
            lane.dirs.forEach { dir ->
                val hit = active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))
                marks += MapBinding.LaneMark(at, brg, dir, hit)
            }
        }
    }
    // lane strips from up to 130 m before the junction to just short of it
    // TomTom's lane section can be short, but the lanes carry on back along the approach, so the strips do not stop where the section starts
    // (only the last stretch before the junction, where the lanes really are: about 110 m on motorways, 60 m elsewhere,
    // so lanes aren't painted far back along a road that only widens at the junction)
    val from = max(0.0, cur.off - (if (lined) 150.0 else 60.0))
    val to = cur.off - 6.0
    val bands = ArrayList<MapBinding.LaneBand>()
    val divs = ArrayList<MapBinding.LaneEdge>()
    if (lined && to - from > 20.0) {
        val samples = ArrayList<Pair<LL, Double>>()
        val sampleMids = ArrayList<DoubleArray>()
        var o = from
        while (o < to) { samples += pointAt(r, o); sampleMids += mids(o); o += 10.0 }
        samples += pointAt(r, to); sampleMids += mids(to)
        val last = samples.size - 1
        for (i in 0 until n) {
            // cut into short pieces so the ribbon can fade in softly at its start
            for (k in 0 until last) {
                val l = ArrayList<LL>(); val rt = ArrayList<LL>()
                for (m in k..k + 1) { val (p, brg) = samples[m]; l += shifted(p, brg, leftE(sampleMids[m], i)); rt += shifted(p, brg, rightE(sampleMids[m], i)) }
                val a = 0.5 + 0.5 * min(1.0, k / 3.0)
                bands += MapBinding.LaneBand(l + rt.asReversed(), i in activeIdx, a)
            }
            val edge = ArrayList<LL>(); for ((j, sp) in samples.withIndex()) edge += shifted(sp.first, sp.second, rightE(sampleMids[j], i))
            divs += MapBinding.LaneEdge(edge, i == n - 1)
        }
        val outer = ArrayList<LL>(); for ((j, sp) in samples.withIndex()) outer += shifted(sp.first, sp.second, leftE(sampleMids[j], 0))
        divs += MapBinding.LaneEdge(outer, true)
    }
    map.setLaneBands(bands, divs)
    // the route line moved into the lane you need (when the lanes are lined up with the road). One sideways offset along the
    // route, used for both the route line and the turn arrow so they always meet with no step: it eases across (over up to
    // 60 m), stays in the middle of the lit-up lanes right up to the junction, then eases back onto the road's line over the first
    // 25 m after it (where the route carries on along the slip road or the main road). Before, the line only came back in the
    // last 8 m while the turn arrow, drawn on the road's own line from 38 m out, left a gap and a step where they met.
    val shift = ArrayList<LL>()
    laneLatAt = null; laneShiftEnd = 0.0
    if (lined && activeIdx.isNotEmpty()) {
        // the middle of the lit-up lanes, wherever they are along the approach
        fun midAt(o: Double): Double { val m = mids(o); return activeIdx.map { m[it] }.average() }
        val mid = midAt(cur.off - 40.0)
        val s0 = max(0.0, max(sec.off, cur.off - 250.0))
        val s1 = min(r.len, cur.off + 25.0)
        // (fully in the lane by the time the lane strips start, 110 m out, when there's room; else over the first half)
        val stripStart = cur.off - 150.0
        val ramp = if (stripStart - s0 >= 20.0) min(60.0, stripStart - s0) else max(10.0, (cur.off - s0) * 0.5)
        if (maxOf(Math.abs(mid), Math.abs(midAt(cur.off - 100.0)), Math.abs(midAt(cur.off - 10.0))) > 0.3 && cur.off - s0 > 30.0) {
            val fn: (Double) -> Double = { o ->
                val inF = ((o - s0) / ramp).coerceIn(0.0, 1.0)
                val outF = ((s1 - o) / 35.0).coerceIn(0.0, 1.0)     // from 10 m before the junction to 25 m after it
                val f = minOf(inF, outF)
                midAt(min(o, cur.off)) * f * f * (3 - 2 * f)
            }
            laneLatAt = fn; laneShiftEnd = s1
            var o = s0
            while (true) {
                val (p, brg) = pointAt(r, o)
                shift += shifted(p, brg, fn(o))
                if (o >= s1) break
                o = minOf(s1, o + 5.0)
            }
        }
    }
    map.setRouteShift(shift)
    map.setLanes(marks)
}

/**
 * The [n] real lanes that make up your carriageway, out of [offs] (all the lanes found across the road there, left to right):
 * the block of [n] side by side (no gap wider than a lane and a half between them) nearest the route line. Null when they
 * don't match the lane list, so the standard layout is used instead.
 */
private fun NavEngine.pickLaneWindow(offs: List<Double>, n: Int): List<Double>? {
    if (n < 2 || offs.size < n) return null
    var best: List<Double>? = null; var bestD = Double.MAX_VALUE
    for (s in 0..offs.size - n) {
        val w = offs.subList(s, s + n)
        if ((1 until n).any { w[it] - w[it - 1] !in 2.4..5.5 }) continue
        val mid = w.average()
        if (Math.abs(mid) < bestD) { bestD = Math.abs(mid); best = w }
    }
    // the route line has to be somewhere on that carriageway, or it's another road
    return best?.takeIf { it.first() - 3.0 < 0.0 && it.last() + 3.0 > 0.0 }
}

/** True when the lane numbers follow on from each other (2, 3, 4), so they form one block of lanes. */
private fun NavEngine.isContiguous(idx: List<Int>): Boolean = idx.isNotEmpty() && idx.last() - idx.first() == idx.size - 1

/** Index of the last route point at or before [d] metres along the route. */
internal fun NavEngine.idxAt(r: Route, d: Double): Int {
    val c = r.cum
    var lo = 0; var hi = c.size - 1
    while (lo < hi) { val m = (lo + hi + 1) ushr 1; if (c[m] <= d) lo = m else hi = m - 1 }
    return lo
}

internal fun NavEngine.updateDone(r: Route, prog: Double) {
    // grey right back to where the blue line starts (it's drawn from a little behind the car), so no blue shows behind you;
    // with the whole route on screen (map moved by hand), the whole driven part is grey
    val start = if (!following) 0.0 else max(0.0, min(prog - 800.0, lastDrawProg - 950.0))
    if (prog - start < 2.0) { if (doneShown) { map.setDone(emptyList()); doneShown = false }; return }
    val pts = ArrayList<LL>()
    pts += pointAt(r, start).first
    var i = idxAt(r, start) + 1
    while (i < r.pts.size && r.cum[i] < prog) { pts += r.pts[i]; i++ }
    pts += pointAt(r, prog).first
    map.setDone(pts); doneShown = true
}

internal fun NavEngine.pointAt(r: Route, d: Double): Pair<LL, Double> {
    val c = r.cum
    var lo = 0; var hi = c.size - 1
    while (lo < hi - 1) { val m = (lo + hi) ushr 1; if (c[m] <= d) lo = m else hi = m }
    val seg = (c[hi] - c[lo]).takeIf { it > 0 } ?: 1.0
    val t = ((d - c[lo]) / seg).coerceIn(0.0, 1.0)
    val a = r.pts[lo]; val b = r.pts[hi]
    return Pair(LL(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t), Geo.bearing(a, b))
}
