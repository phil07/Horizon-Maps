package com.headunit.launcher

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/**
 * Public holidays of a country (Nager.Date, free, no key). A holiday weekend means more traffic on the roads, so the route's start
 * gets a short note. School holidays need the federal state, which the app doesn't know, so they are not part of this.
 */
internal object PublicHolidays {
    private val cache = HashMap<String, Map<String, String>>()

    /** The holiday's name on [day] in [cc], or null when it isn't one. Null too when the list can't be read. */
    fun on(cc: String, day: LocalDate): String? {
        val code = cc.uppercase()
        if (code.length != 2) return null
        return year(code, day.year)?.get(day.toString())
    }

    /** The year's holidays in [cc] (date → local name), read once and kept. Null when the list can't be read. */
    private fun year(cc: String, y: Int): Map<String, String>? {
        val key = "$cc-$y"
        synchronized(cache) { cache[key]?.let { return it } }
        val map = read("https://date.nager.at/api/v3/PublicHolidays/$y/$cc") ?: return null
        synchronized(cache) { cache[key] = map }
        return map
    }

    private fun read(u: String): Map<String, String>? = try {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000; c.readTimeout = 12_000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        if (c.responseCode !in 200..299) null else {
            val arr = JSONArray(c.inputStream.bufferedReader().use { it.readText() })
            val out = HashMap<String, String>()
            for (i in 0 until arr.length()) {
                val h = arr.optJSONObject(i) ?: continue
                out[h.optString("date")] = h.optString("localName").ifBlank { h.optString("name") }
            }
            out
        }
    } catch (e: Exception) { null }
}
