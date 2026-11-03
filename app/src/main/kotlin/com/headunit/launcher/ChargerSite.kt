package com.headunit.launcher

/**
 * A charge point (or site) for the map: its position, its power in kW when known, its number of chargers and how many are free.
 * [kwLabel] is the text shown for the power when it isn't just one number (e.g. "2 × 22 kW"), [op] the operator (for the route name).
 */
class ChargerSite(val pos: LL, val kw: Int?, val total: Int?, val free: Int?, val op: String? = null, val kwLabel: String? = null)

/**
 * One marker per place: the register lists each charging unit of a site separately, at the same spot. The site shows its strongest
 * unit (its power and label), and the chargers of all its units are counted together.
 */
fun mergeSites(sites: List<ChargerSite>): List<ChargerSite> {
    val groups = LinkedHashMap<Pair<Long, Long>, MutableList<ChargerSite>>()
    for (s in sites) groups.getOrPut(Math.round(s.pos.lat * 1e5) to Math.round(s.pos.lon * 1e5)) { ArrayList() } += s
    return groups.values.map { g ->
        val best = g.maxWith(compareBy<ChargerSite> { it.kw ?: -1 })
        val totals = g.mapNotNull { it.total }
        val frees = g.mapNotNull { it.free }
        ChargerSite(best.pos, best.kw, totals.takeIf { it.isNotEmpty() }?.sum(), frees.takeIf { it.isNotEmpty() }?.sum(), best.op, best.kwLabel)
    }
}
