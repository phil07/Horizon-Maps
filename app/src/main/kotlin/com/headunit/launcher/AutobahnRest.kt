package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Roadworks, warnings and closures on Germany's motorways, from Autobahn GmbH's public traffic API (no key needed). They feed the
 * same incident pipeline as TomTom's and Mapbox's: the map, the warning on the route and the voice announcement (NavEngine).
 * Source credit: "Daten: Autobahn GmbH" (Settings).
 */
object AutobahnRest {
    private const val BASE = "https://verkehr.autobahn.de/o/autobahn/"
    /** Service name, then the incident category the engine knows it by. */
    private val SERVICES = listOf("roadworks" to "roadWorks", "closure" to "roadClosed", "warning" to "danger")

    /** "A3", "A 3" (or "A3;E 35") -> "A3"; null when the street isn't a motorway. */
    fun roadOf(street: String): String? =
        Regex("^\\s*A\\s?(\\d{1,3})\\b").find(street)?.let { "A" + it.groupValues[1] }

    /** The current items on [roads] (e.g. "A3"). Empty when none, or when the server didn't answer for a road. Never throws. */
    fun incidents(roads: List<String>): List<Incident> {
        val out = ArrayList<Incident>()
        for (road in roads) for ((service, cat) in SERVICES) {
            try {
                val arr = JSONObject(get(BASE + road + "/services/" + service)).optJSONArray(service) ?: continue
                for (i in 0 until arr.length()) parse(arr.optJSONObject(i) ?: continue, road, service, cat)?.let { out += it }
            } catch (e: Exception) { }
        }
        return out
    }

    private fun parse(o: JSONObject, road: String, service: String, cat0: String): Incident? {
        val c = o.optJSONObject("coordinate") ?: return null
        val at = LL(c.optDouble("lat", Double.NaN), c.optDouble("long", Double.NaN))
        if (at.lat.isNaN() || at.lon.isNaN()) return null
        // the stretch the item covers (GeoJSON order: lon, lat); its start is where the marker sits
        val pts = arrayListOf(at)
        o.optJSONObject("geometry")?.optJSONArray("coordinates")?.let { g ->
            for (i in 0 until g.length()) { val p = g.optJSONArray(i) ?: continue; pts += LL(p.optDouble(1), p.optDouble(0)) }
        }
        val desc = o.optJSONArray("description")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        // a warning is an accident or a jam when its text says so; otherwise it stays a general hazard
        val cat = when {
            cat0 != "danger" -> cat0
            desc.any { it.contains("Unfall", true) } -> "accident"
            desc.any { it.contains("Stau", true) || it.contains("stockend", true) } -> "jam"
            else -> cat0
        }
        val text = listOf(o.optString("title"), o.optString("subtitle")).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
        val id = o.optString("identifier").ifBlank { "$road-$service-${at.lat}-${at.lon}" }
        return Incident("ab-$id", cat, text.ifBlank { road }, 0, pts)
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/json")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IllegalStateException("Autobahn HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
