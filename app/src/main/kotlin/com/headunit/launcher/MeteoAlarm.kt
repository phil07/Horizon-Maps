package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Severe weather warnings from Meteoalarm (free, no key) for the countries where the warning areas come with their outline: the
 * route ahead is checked against each area's polygon. Germany is left to the DWD (its warnings have no outlines). Only orange and
 * red (moderate and severe) are used, like the DWD's "markant" and worse.
 */
internal object MeteoAlarm {
    private const val BASE = "https://feeds.meteoalarm.org/api/v1/warnings/feeds-"
    /** How often a country's warnings are read again. */
    private const val EVERY_MS = 30 * 60_000L

    /** Meteoalarm's feed name for each country we check. */
    private val FEEDS = mapOf(
        "FR" to "france", "AT" to "austria", "IT" to "italy", "ES" to "spain", "NL" to "netherlands", "BE" to "belgium",
        "CH" to "switzerland", "PL" to "poland", "CZ" to "czechia", "DK" to "denmark", "LU" to "luxembourg",
        "SI" to "slovenia", "SK" to "slovakia", "HR" to "croatia", "HU" to "hungary",
    )

    /** The warnings of one country, with their outlines, and when they were read. */
    private class Cached(val at: Long, val areas: List<Pair<DwdWarning, List<List<LL>>>>)
    private val cache = HashMap<String, Cached>()

    fun supports(cc: String): Boolean = cc.uppercase() in FEEDS

    /**
     * The warnings (orange and red) whose areas the [line] passes through. Null when the feed could not be read. Only a country
     * whose feed is in [FEEDS] is checked; the result is read again every 30 minutes.
     */
    fun along(line: List<LL>, cc: String): List<DwdWarning>? {
        val key = FEEDS[cc.uppercase()] ?: return emptyList()
        val areas = areasOf(key) ?: return null
        val hit = ArrayList<DwdWarning>()
        for ((w, polys) in areas) {
            if (polys.any { poly -> line.any { inside(it, poly) } }) hit += w
        }
        return hit
    }

    private fun areasOf(feed: String): List<Pair<DwdWarning, List<List<LL>>>>? {
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[feed]?.let { if (now - it.at < EVERY_MS) return it.areas } }
        val body = read(BASE + feed) ?: return null
        val list = parse(body)
        synchronized(cache) { cache[feed] = Cached(now, list) }
        return list
    }

    private fun read(u: String): String? = try {
        val c = URL(u).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 25_000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        if (c.responseCode !in 200..299) null else c.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) { null }

    /** One entry per area of each warning (orange and red only). Areas without an outline are left out: they can't be checked. */
    internal fun parse(body: String): List<Pair<DwdWarning, List<List<LL>>>> {
        val out = ArrayList<Pair<DwdWarning, List<List<LL>>>>()
        val warnings: JSONArray = (try { JSONObject(body).optJSONArray("warnings") } catch (e: Exception) { null }) ?: return out
        for (i in 0 until warnings.length()) {
            val alert = warnings.optJSONObject(i)?.optJSONObject("alert") ?: continue
            val infos = alert.optJSONArray("info") ?: continue
            for (j in 0 until infos.length()) {
                val info = infos.optJSONObject(j) ?: continue
                val lv = when (info.optString("severity")) { "Moderate" -> 2; "Severe" -> 3; "Extreme" -> 4; else -> 0 }
                if (lv < 2) continue
                val event = info.optString("event")
                val onset = iso(info.optString("onset"))
                val expires = iso(info.optString("expires"))
                val headline = info.optString("headline")
                val description = info.optString("description")
                val areas = info.optJSONArray("area") ?: continue
                for (k in 0 until areas.length()) {
                    val area = areas.optJSONObject(k) ?: continue
                    val polys = polygons(area.opt("polygon"))
                    if (polys.isEmpty()) continue
                    val name = area.optString("areaDesc")
                    val id = alert.optString("identifier") + "|" + event + "|" + name
                    out += DwdWarning(id, event, lv, headline, name, onset, expires, description, null) to polys
                }
            }
        }
        return out
    }

    /** The outlines: each is a list of "lat,lon" pairs written in one string, given either as a list or as one string. */
    private fun polygons(raw: Any?): List<List<LL>> {
        val strings = when (raw) {
            is JSONArray -> (0 until raw.length()).map { raw.optString(it) }
            is String -> listOf(raw)
            else -> return emptyList()
        }
        return strings.mapNotNull { s ->
            val pts = s.trim().split(Regex("\\s+")).mapNotNull { pair ->
                val a = pair.split(",")
                if (a.size != 2) null else {
                    val la = a[0].toDoubleOrNull(); val lo = a[1].toDoubleOrNull()
                    if (la == null || lo == null) null else LL(la, lo)
                }
            }
            if (pts.size >= 3) pts else null
        }
    }

    private fun iso(s: String): Long = try {
        java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli()
    } catch (e: Exception) { 0L }

    /** Point in polygon (ray casting, in lat/lon: fine for the size of a warning area). */
    internal fun inside(p: LL, poly: List<LL>): Boolean {
        var c = false
        var j = poly.size - 1
        for (i in poly.indices) {
            val a = poly[i]; val b = poly[j]
            if ((a.lat > p.lat) != (b.lat > p.lat) && p.lon < (b.lon - a.lon) * (p.lat - a.lat) / (b.lat - a.lat) + a.lon) c = !c
            j = i
        }
        return c
    }
}
