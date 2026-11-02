package com.headunit.launcher

import org.json.JSONObject

/**
 * The order of the search results. A name exactly as typed comes before one that only starts with it, and towns and cities come
 * before streets: "berlin" finds Berlin first, then the Berliner Straße nearby. When what you typed looks like a street
 * ("Berliner Str", "Hauptstraße 5"), the nearest matching street comes first instead.
 */
internal object SearchRank {

    /** Mapbox's kinds of result that are a town, city or region. */
    val MAPBOX_TOWNS = setOf("place", "locality", "district", "region", "country")

    /** OpenStreetMap place kinds (Photon's osm_value) that are a town or city. */
    val OSM_TOWNS = setOf("city", "town", "village", "municipality", "borough")

    /** The offline pack's kinds (places.tsv) that are a town or city. */
    val PACK_TOWNS = setOf("City", "Town", "Village", "Municipality", "Borough")

    /** Whether a Nominatim (OpenStreetMap) result is a town, city or region. */
    fun nominatimTown(x: JSONObject): Boolean {
        val cat = x.optString("category"); val type = x.optString("type")
        return (cat == "place" && type in OSM_TOWNS) || (cat == "boundary" && type == "administrative")
    }

    private val STREET_ENDINGS = listOf("straße", "strasse", "str", "str.", "weg", "allee", "platz", "gasse", "damm", "ufer",
        "chaussee", "pfad", "steig", "street", "road", "lane", "avenue", "rd", "st")

    /** Whether what was typed looks like a street or an address: a street word ("Str", "…weg", "Allee") or a house number. */
    fun streety(q: String): Boolean {
        val words = q.trim().lowercase().split(' ', ',').filter { it.isNotBlank() }
        if (words.any { w -> w.any { it.isDigit() } }) return true
        // "Berliner Str": a street word after the name
        if (words.drop(1).any { it in STREET_ENDINGS }) return true
        // "Hauptstraße", "Lindenweg": a street word at the end of one word (not the short ones, "st" ends too many names)
        return words.any { w -> STREET_ENDINGS.any { e -> e.length >= 3 && w.length > e.length + 2 && w.endsWith(e) } }
    }

    /**
     * [all]: each result with its place in the list it came from (online results 0, 1, 2 …, offline ones from 1000), so ties
     * keep the search service's order. [dist]: how far a result is from you.
     */
    fun <T> rank(q: String, all: List<Pair<T, Int>>, name: (T) -> String, town: (T) -> Boolean, dist: (T) -> Double): List<T> {
        val full = q.trim().lowercase()
        fun exact(t: T) = name(t).lowercase().let { it == full || it.substringBefore(',').trim() == full }
        fun prefix(t: T) = name(t).lowercase().startsWith(full)
        val byDist = compareBy<Pair<T, Int>> { dist(it.first) }
        val byOrder = compareBy<Pair<T, Int>> { it.second }
        val ex = all.filter { exact(it.first) }
        val pre = all.filter { !exact(it.first) && prefix(it.first) }
        val rest = all.filter { !exact(it.first) && !prefix(it.first) }.sortedWith(byOrder)
        val ordered = if (streety(q)) {
            ex.sortedWith(byDist) + pre.sortedWith(byDist)
        } else {
            // towns of exactly that name nearest first (the nearest Neustadt); towns that only start with it in the service's order
            // (Frankfurt before Frankenberg); other places nearest first
            ex.filter { town(it.first) }.sortedWith(byDist) + ex.filter { !town(it.first) }.sortedWith(byDist) +
                pre.filter { town(it.first) }.sortedWith(byOrder) + pre.filter { !town(it.first) }.sortedWith(byDist)
        }
        return (ordered + rest).map { it.first }
    }
}
