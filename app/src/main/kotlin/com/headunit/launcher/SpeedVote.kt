package com.headunit.launcher

/**
 * Picks the most likely speed limit from what every source says (weighted majority). Each source votes for a value: a posted
 * limit, or "no limit" (an unrestricted Autobahn). The value with the most weight wins; a tie goes to the lower limit (the safe
 * side). The sources are told apart by name, so the sign can show what each one said.
 */
internal object SpeedVote {

    /** One source's answer: [ms] the limit in m/s, or [NONE] for no general limit. */
    data class Vote(val source: String, val ms: Double, val weight: Double, val estimated: Boolean = false)

    /** The winner, with whether a real source said something else ([disputed]) and what each one said ([details]). */
    data class Result(val ms: Double, val estimated: Boolean, val disputed: Boolean, val details: String, val winners: Set<String>)

    const val NONE = -3.0

    // how much each source counts: a sign seen on the road most, the big map makers next, OpenStreetMap a little less, a guess least
    const val W_SIGN = 3.0
    const val W_ROUTE = 2.0
    const val W_ROADWORKS = 2.5
    const val W_HERE = 2.0
    const val W_TOMTOM = 2.0
    const val W_MAPBOX = 1.5
    /** Rijkswaterstaat's posted limits (Dutch road authority, CC0): as sure as HERE and TomTom. */
    const val W_RWS = 2.0
    const val W_OSM = 1.5
    const val W_GUESS = 0.5

    /** A real source (not a guess from the road type) disagreeing with the winner makes it "disputed". */
    private const val REAL = 1.0

    /** Votes for the same limit: 50 and 50.4 km/h are one value; "no limit" is its own. */
    fun key(ms: Double): Int = if (ms <= 0.0) -1 else Math.round(ms * 3.6).toInt()

    fun decide(votes: List<Vote>, imperial: Boolean = false): Result? {
        val vs = votes.filter { it.weight > 0.0 && (it.ms > 0.0 || it.ms == NONE) }
        if (vs.isEmpty()) return null
        val groups = vs.groupBy { key(it.ms) }
        val best = groups.entries.sortedWith(compareByDescending<Map.Entry<Int, List<Vote>>> { e -> e.value.sumOf { it.weight } }
            // a tie: the lower limit (no limit counts as the highest)
            .thenBy { e -> if (e.key < 0) Int.MAX_VALUE else e.key }).first()
        val win = best.value
        val ms = if (best.key < 0) NONE else win.maxByOrNull { it.weight }!!.ms
        val disputed = groups.any { (k, v) -> k != best.key && v.any { it.weight >= REAL } }
        val details = vs.joinToString(" · ") { "${it.source} ${label(it.ms, imperial)}${if (it.estimated) "?" else ""}" }
        return Result(ms, win.all { it.estimated }, disputed, details, win.map { it.source }.toSet())
    }

    fun label(ms: Double, imperial: Boolean): String =
        if (ms <= 0.0) Lang.t("frei", "none") else Math.round(ms * (if (imperial) 2.23694 else 3.6)).toString()

    /** How much a sign seen on Mapillary counts by its age: full within a year, less up to 3 years, not at all after. */
    fun signWeight(ageMs: Long): Double {
        val years = ageMs / (365.25 * 24 * 3600 * 1000)
        return when {
            years < 0 -> W_SIGN
            years <= 1.0 -> W_SIGN
            years <= 2.0 -> W_SIGN * 0.85
            years <= 3.0 -> W_SIGN * 0.7
            else -> 0.0
        }
    }
}
