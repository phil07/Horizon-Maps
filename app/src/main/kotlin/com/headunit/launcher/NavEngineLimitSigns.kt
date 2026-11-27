package com.headunit.launcher

import kotlin.math.roundToInt

/**
 * The speed limit signs drawn on the route where the limit changes (from the route's own limit sections, which TomTom routes carry):
 * one sign at the start of each stretch with a new limit, shown from city zoom in. Off with Settings > Show speed limits.
 */
internal fun NavEngine.pushLimitSigns(r: Route?) {
    if (r == null || !prefs.b(PreferenceKeys.SHOW_LIMITS, true)) { map.setLimitSigns(emptyList()); return }
    val k = if (imperial) 2.23694 else 3.6
    val out = ArrayList<Pair<LL, Int>>()
    var last = -1
    for (sc in r.limits) {
        val v = (sc.ms * k / 5.0).roundToInt() * 5
        if (v <= 0 || v == last) continue
        out += pointAt(r, sc.off).first to v
        last = v
        if (out.size >= 80) break
    }
    map.setLimitSigns(out)
}
