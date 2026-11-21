package com.headunit.launcher

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * The speed camera that warns next, and how far ahead it is (metres), or null.
 *
 * While a route is followed ([onRoute]), the cameras along it count, by distance along the road (so bends are fine). Otherwise,
 * or when the route has none, a camera counts when it lies ahead within 35 m to the side of the line you drive along.
 * [skip] leaves out the cameras that belong to an average speed check, [facesUs] the ones that check the other way.
 */
internal fun nearestCameraAhead(
    p: Fix,
    curProg: Double,
    onRoute: Boolean,
    routeCams: List<Pair<LL, Double>>,
    cams: List<LL>,
    skip: (LL) -> Boolean,
    facesUs: (LL, Double) -> Boolean,
): Pair<LL, Double>? {
    // warned in good time: at least 500 m, or about 18 seconds ahead at speed
    val warnAt = max(500.0, p.speed * 18.0)
    var best: LL? = null
    var bestD = 1e9
    if (onRoute) for ((c, off) in routeCams) {
        if (skip(c)) continue
        val ahead = off - curProg
        if (ahead < -15.0 || ahead > warnAt || ahead >= bestD) continue
        if (!facesUs(c, p.heading)) continue
        best = c; bestD = max(0.0, ahead)
    }
    val followingRoute = onRoute && routeCams.isNotEmpty()
    if (best == null && !followingRoute && p.heading >= 0 && p.speed > 2.0) {
        for (c in cams) {
            if (skip(c)) continue
            val d = Geo.dist(p.pos, c)
            if (d < 1.0 || d > warnAt + 40.0) continue
            val ang = Geo.angDiff(Geo.bearing(p.pos, c), p.heading)
            val along = d * cos(Math.toRadians(ang))
            val side = abs(d * sin(Math.toRadians(ang)))
            if (along < 0.0 || along > warnAt || side > 35.0) continue
            if (!facesUs(c, p.heading)) continue
            if (along < bestD) { best = c; bestD = along }
        }
    }
    return best?.let { it to bestD }
}

/** What to sound for the camera ahead: [first] when it first comes into range (once per camera), [second] a short beep about 4 seconds before it (once per camera). */
internal class CameraAlerts {
    data class Announce(val first: Boolean, val second: Boolean)

    private val alerted = HashSet<LL>()
    private var beepNear: LL? = null

    /** [best] is the camera ahead ([dist] metres away), null when none; [speed] in m/s. */
    fun check(best: LL?, dist: Double, speed: Double): Announce {
        if (best == null) return Announce(first = false, second = false)
        if (alerted.size > 100) alerted.clear()
        val first = alerted.add(best)
        val fourSec = max(4.0 * speed, 30.0)
        val second = speed > 2.0 && dist <= fourSec && beepNear != best
        if (second) beepNear = best
        return Announce(first, second)
    }
}

/** The cameras that lie on the route, with how far along it each one is (metres), in order. Each one counts when it is within 25 m
 *  of the route line, and not when it checks the other carriageway. */
internal fun camerasAlongRoute(r: Route, cams: List<LL>): List<Pair<LL, Double>> {
    // points every 10 m along the route (filled in along long straight stretches, where the route's own points can be
    // hundreds of metres apart: a camera in between was too far from any point and never shown or warned about),
    // in a coarse grid so each camera only checks the route nearby
    val grid = HashMap<Long, ArrayList<Int>>()
    val pts = ArrayList<LL>(); val offs = ArrayList<Double>()
    fun key(lat: Double, lon: Double) = Math.floor(lat / 0.002).toLong() * 1000003L + Math.floor(lon / 0.003).toLong()
    fun add(p: LL, o: Double) { grid.getOrPut(key(p.lat, p.lon)) { ArrayList() }.add(pts.size); pts += p; offs += o }
    for (n in r.pts.indices) {
        add(r.pts[n], r.cum[n])
        if (n + 1 < r.pts.size) {
            val a = r.pts[n]; val b = r.pts[n + 1]
            val len = r.cum[n + 1] - r.cum[n]
            val steps = (len / 10.0).toInt()
            for (j in 1 until steps) {
                val f = j / steps.toDouble()
                add(LL(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f), r.cum[n] + len * f)
            }
        }
    }
    val out = ArrayList<Pair<LL, Double>>()
    for (c in cams) {
        // within 25 m of the route line (was 45 m): a camera in the next village, just off the road, is not on the route
        var bestD = 25.0; var off = -1.0
        val k = key(c.lat, c.lon)
        var bi = -1
        for (dy in -1..1) for (dx in -1..1) grid[k + dy * 1000003L + dx]?.forEach { i ->
            val d = Geo.dist(pts[i], c)
            if (d < bestD) { bestD = d; off = offs[i]; bi = i }
        }
        // a camera that checks the other carriageway (it's known which way it points): not on your route
        val cd = Overpass.camDir[Overpass.camKey(c)]
        if (off >= 0 && cd != null && bi >= 0 && bi + 1 < pts.size && abs(Geo.angDiff(cd, Geo.bearing(pts[bi], pts[bi + 1]))) > 75.0) off = -1.0
        if (off >= 0) out += c to off
    }
    out.sortBy { it.second }
    return out
}
