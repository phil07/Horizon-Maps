package com.headunit.launcher

/**
 * The road numbers (B 27, A 7, M25) drawn on the route where the route turns onto them, so you see which main road you're on.
 * A number is shown once per stretch: a repeat of the same number right after it is skipped.
 */
internal fun NavEngine.pushRoadNumbers(r: Route?) {
    if (r == null) { map.setRoadNumbers(emptyList()); return }
    val out = ArrayList<Pair<LL, String>>()
    var last = ""
    for (s in r.steps) {
        val no = s.street.trim()
        if (no == last || !RoadRefs.isRef(no) || RoadRefs.isEuro(no)) continue
        out += pointAt(r, s.off).first to no
        last = no
        if (out.size >= 40) break
    }
    map.setRoadNumbers(out)
}
