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
