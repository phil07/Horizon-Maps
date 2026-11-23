package com.headunit.launcher

/**
 * The route line in pieces of one traffic level each (0 flowing, 1 slow, 2 heavy). With blending (Settings › On the map › Blend
 * traffic colours) each change of level is drawn as a short run of pieces with the levels in between (0.25, 0.5 …), so the
 * colours run into each other instead of changing in one step. The map's colour for a level is interpolated, so a whole level
 * keeps exactly the colour it always had.
 */
internal object RouteBlend {
    class Piece(val pts: List<LL>, val level: Double)

    /** [level]: the traffic level of the stretch from point i to point i + 1. [span]: how long a change takes, in metres. */
    fun pieces(pts: List<LL>, level: (Int) -> Int, blend: Boolean, span: Double = 80.0, steps: Int = 6): List<Piece> {
        if (pts.size < 2) return emptyList()
        // the runs of one level, as point index ranges (each run shares its end point with the next one's start)
        val runs = ArrayList<Triple<Int, Int, Int>>()
        var start = 0; var cur = level(0)
        for (j in 1 until pts.size - 1) {
            val l = level(j)
            if (l != cur) { runs += Triple(start, j, cur); start = j; cur = l }
        }
        runs += Triple(start, pts.size - 1, cur)
        if (!blend || runs.size == 1) return runs.map { (a, b, l) -> Piece(pts.subList(a, b + 1), l.toDouble()) }
        // distance along the line at every point
        val at = DoubleArray(pts.size)
        for (i in 1 until pts.size) at[i] = at[i - 1] + Geo.dist(pts[i - 1], pts[i])
        // the stretches to draw, by distance along the line: [from, to] and the level, in order
        val parts = ArrayList<Triple<Double, Double, Double>>()
        var from = 0.0
        for (k in runs.indices) {
            val (a, b, l) = runs[k]
            val next = runs.getOrNull(k + 1)
            if (next == null) { parts += Triple(from, at[b], l.toDouble()); break }
            // the change takes up to [span], but never more than half of either run, so a short one still shows its own colour
            val h = minOf(span / 2, (at[b] - at[a]) / 2, (at[next.second] - at[next.first]) / 2)
            val edge = at[b]
            if (edge - h > from) parts += Triple(from, edge - h, l.toDouble())
            val step = 2 * h / steps
            for (i in 0 until steps) {
                val lv = l + (next.third - l) * (i + 0.5) / steps
                parts += Triple(edge - h + i * step, edge - h + (i + 1) * step, lv)
            }
            from = edge + h
        }
        return parts.filter { it.second - it.first > 0.01 }.map { (x0, x1, l) -> Piece(slice(pts, at, x0, x1), l) }
    }

    /** The part of the line from [x0] to [x1] metres along it, with the ends placed exactly. */
    private fun slice(pts: List<LL>, at: DoubleArray, x0: Double, x1: Double): List<LL> {
        val out = ArrayList<LL>()
        out += pointAt(pts, at, x0)
        for (i in pts.indices) if (at[i] > x0 && at[i] < x1) out += pts[i]
        out += pointAt(pts, at, x1)
        return out
    }

    private fun pointAt(pts: List<LL>, at: DoubleArray, x: Double): LL {
        if (x <= 0.0) return pts.first()
        for (i in 1 until pts.size) if (at[i] >= x) {
            val seg = at[i] - at[i - 1]
            val f = if (seg > 0) (x - at[i - 1]) / seg else 0.0
            val a = pts[i - 1]; val b = pts[i]
            return LL(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f)
        }
        return pts.last()
    }
}
