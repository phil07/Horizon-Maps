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
