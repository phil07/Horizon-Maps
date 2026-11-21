package com.headunit.launcher

/**
 * The route cut into small square cells (about 15 m to 22 m each), so "is this point on the route?" and "which pieces of
 * the route are near it?" are a lookup instead of a scan over the whole route. [ensure] builds it for a route, once.
 */
internal class RouteGrid {
    private var built: List<LL>? = null
    private val cells = HashMap<Long, ArrayList<Int>>()

    /** Builds the grid for [r] unless it is already built for this same list. */
    fun ensure(r: List<LL>) {
        if (built !== r) rebuild(r)
    }

    private fun rebuild(r: List<LL>) {
        cells.clear()
        for (i in 0 until r.size - 1) {
            val a = r[i]; val b = r[i + 1]
            val steps = (Geo.dist(a, b) / 15.0).toInt()
            var lastK = Long.MIN_VALUE
            for (k in 0..steps + 1) {
                val t = k / (steps + 1.0)
                val key = key(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)
                if (key == lastK) continue
                lastK = key
                val l = cells.getOrPut(key) { ArrayList(2) }
                if (l.isEmpty() || l[l.size - 1] != i) l.add(i)
            }
        }
        built = r
    }

    /** Whether [p] lies in a cell of the route, or next to one. */
    fun onRoute(p: LL): Boolean {
        val k = key(p.lat, p.lon)
        for (dy in -1..1) for (dx in -1..1) if (cells.containsKey(k + dy * 1000003L + dx)) return true
        return false
    }

    /** Adds to [into] the route segments (by index) in the cells around [p], its own and the eight next to it. */
    fun segmentsNear(p: LL, into: MutableCollection<Int>) {
        val k = key(p.lat, p.lon)
        for (dy in -1..1) for (dx in -1..1) cells[k + dy * 1000003L + dx]?.let { into.addAll(it) }
    }

    companion object {
        /** The cell of a point: a number that is the same for all points in the same cell. */
        fun key(lat: Double, lon: Double): Long = Math.floor(lat / 0.0002).toLong() * 1000003L + Math.floor(lon / 0.0003).toLong()
    }
}
