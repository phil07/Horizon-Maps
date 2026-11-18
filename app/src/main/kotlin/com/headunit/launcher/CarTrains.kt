package com.headunit.launcher

/**
 * Trains that carry cars (the Channel Tunnel shuttle, the Alpine tunnel trains, the Sylt shuttle): a route is on one when it passes
 * both of a line's loading terminals. The lines are OpenStreetMap's car-train lines (route=shuttle_train), kept here with their
 * terminals so they are known without a connection. Mapbox gives some of them (such as Brig–Iselle) as ordinary driving, so the
 * route line is checked rather than the route's own steps.
 */
object CarTrains {
    private class Line(val name: String, val a: LL, val b: LL)

    private val lines = listOf(
        Line("Le Shuttle (Folkestone–Calais)", LL(51.0935, 1.1340), LL(50.9290, 1.8170)),
        Line("Lötschberg (Kandersteg–Goppenstein)", LL(46.4930, 7.6745), LL(46.3690, 7.7560)),
        Line("Simplon (Brig–Iselle)", LL(46.3190, 7.9880), LL(46.2130, 8.2240)),
        Line("Furka (Realp–Oberwald)", LL(46.5980, 8.5020), LL(46.5330, 8.3500)),
        Line("Vereina (Selfranga–Sagliains)", LL(46.8650, 9.8990), LL(46.7820, 10.1130)),
        Line("Albula (Thusis–Samedan)", LL(46.6970, 9.4400), LL(46.5340, 9.8730)),
        Line("Tauernschleuse (Böckstein–Mallnitz)", LL(47.0870, 13.1150), LL(46.9920, 13.1720)),
        Line("Sylt Shuttle (Niebüll–Westerland)", LL(54.7890, 8.8290), LL(54.9070, 8.3100)),
        Line("Bohinj (Bohinjska Bistrica–Podbrdo)", LL(46.2730, 13.9550), LL(46.2120, 13.9660)),
    )

    /** How close the route has to come to both terminals to count as using the line (the terminals are large yards). */
    private const val NEAR_M = 2000.0

    /** The car-train line [r] takes, or null when it takes none. */
    fun on(r: Route): String? {
        if (r.pts.size < 2) return null
        for (l in lines) if (passes(r, l.a) && passes(r, l.b)) return l.name
        return null
    }

    private fun passes(r: Route, p: LL): Boolean {
        // a quick box test first: most routes are nowhere near any terminal
        val dLat = NEAR_M / 111_000.0
        val dLon = NEAR_M / (111_000.0 * Math.cos(Math.toRadians(p.lat)).coerceAtLeast(0.2))
        for (q in r.pts) {
            if (Math.abs(q.lat - p.lat) > dLat || Math.abs(q.lon - p.lon) > dLon) continue
            if (Geo.dist(q, p) <= NEAR_M) return true
        }
        return false
    }

    /**
     * The routes without a car train first (in their own order), then the others. When every route takes one, the order stays: a car
     * train that is the only way there is still used.
     */
    fun preferWithout(rs: List<Route>): List<Route> {
        if (rs.size < 2) return rs
        val (free, train) = rs.partition { on(it) == null }
        return if (free.isEmpty() || train.isEmpty()) rs else free + train
    }
}
