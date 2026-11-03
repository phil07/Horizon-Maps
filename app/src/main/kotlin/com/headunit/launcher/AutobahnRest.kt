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
            isWrongWay((desc + o.optString("title") + o.optString("subtitle")).joinToString(" ")) -> "wrongway"
            desc.any { it.contains("Unfall", true) } -> "accident"
            desc.any { it.contains("Stau", true) || it.contains("stockend", true) } -> "jam"
            else -> cat0
        }
        val text = listOf(o.optString("title"), o.optString("subtitle")).map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
        val id = o.optString("identifier").ifBlank { "$road-$service-${at.lat}-${at.lon}" }
        return Incident("ab-$id", cat, text.ifBlank { road }, 0, pts)
    }

    // ---- wrong-way drivers ----

    private val WRONG_WAY = Regex("falschfahrer|geisterfahrer|falsche[rn]? (fahrt)?richtung|entgegenkommend|wrong[- ]?way|ghost driver|vehicle on wrong carriageway", RegexOption.IGNORE_CASE)

    /** Whether an incident's text reports a driver going the wrong way. */
    fun isWrongWay(text: String): Boolean = WRONG_WAY.containsMatchIn(text)

    /** The wrong-way driver warnings on [roads] right now (only the warnings, so it can be asked every minute). Never throws. */
    fun wrongWay(roads: List<String>): List<Incident> {
        val out = ArrayList<Incident>()
        for (road in roads) try {
            val arr = JSONObject(get(BASE + road + "/services/warning")).optJSONArray("warning") ?: continue
            for (i in 0 until arr.length()) parse(arr.optJSONObject(i) ?: continue, road, "warning", "danger")?.takeIf { it.cat == "wrongway" }?.let { out += it }
        } catch (e: Exception) { }
        return out
    }

    // ---- speed limits at roadworks ----

    /** A stretch of roadworks with a posted limit ("Max. 80 km/h"): its line in driving direction, the limit, and when it applies. */
    internal class RoadworkLimit(val pts: List<LL>, val ms: Double, val windows: List<LongRange>?, val from: Long)

    private val rwCache = HashMap<String, Pair<Long, List<RoadworkLimit>>>()

    /** The roadworks with a limit on [road] ("A5"), fetched at most every 15 minutes; empty when there are none or no answer. */
    internal fun roadworkLimits(road: String): List<RoadworkLimit> {
        val now = System.currentTimeMillis()
        synchronized(rwCache) { rwCache[road]?.let { if (now - it.first < 15 * 60_000L) return it.second } }
        val list = try {
            val arr = JSONObject(get(BASE + road + "/services/roadworks")).optJSONArray("roadworks")
            val out = ArrayList<RoadworkLimit>()
            if (arr != null) for (i in 0 until arr.length()) parseLimit(arr.optJSONObject(i) ?: continue)?.let { out += it }
            out
        } catch (e: Exception) {
            // no answer: tried again in 2 minutes
            synchronized(rwCache) { rwCache[road] = (now - 13 * 60_000L) to (rwCache[road]?.second ?: emptyList()) }
            return rwCache[road]?.second ?: emptyList()
        }
        synchronized(rwCache) { rwCache[road] = now to list }
        return list
    }

    // ---- the usual limit at roadworks without a posted one: 100 before them, 80 in them (German Autobahn) ----

    /** A stretch of roadworks (with or without a posted limit): its line in driving direction and when it applies. */
    internal class RoadworkZone(val pts: List<LL>, val windows: List<LongRange>?, val from: Long)

    private val zoneCache = HashMap<String, Pair<Long, List<RoadworkZone>>>()

    /** All roadworks on [road] ("A5"), fetched at most every 15 minutes; empty when there are none or no answer. */
    internal fun roadworkZones(road: String): List<RoadworkZone> {
        val now = System.currentTimeMillis()
        synchronized(zoneCache) { zoneCache[road]?.let { if (now - it.first < 15 * 60_000L) return it.second } }
        val list = try {
            val arr = JSONObject(get(BASE + road + "/services/roadworks")).optJSONArray("roadworks")
            val out = ArrayList<RoadworkZone>()
            if (arr != null) for (i in 0 until arr.length()) parseZone(arr.optJSONObject(i) ?: continue)?.let { out += it }
            out
        } catch (e: Exception) {
            synchronized(zoneCache) { zoneCache[road] = (now - 13 * 60_000L) to (zoneCache[road]?.second ?: emptyList()) }
            return zoneCache[road]?.second ?: emptyList()
        }
        synchronized(zoneCache) { zoneCache[road] = now to list }
        return list
    }

    internal fun parseZone(o: JSONObject): RoadworkZone? {
        val desc = o.optJSONArray("description")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        val g = o.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
        val pts = (0 until g.length()).mapNotNull { i -> g.optJSONArray(i)?.let { LL(it.optDouble(1), it.optDouble(0)) } }
        if (pts.size < 2) return null
        return RoadworkZone(pts, windows(desc), parseIso(o.optString("startTimestamp")))
    }

    /**
     * The usual limit (m/s) at roadworks: 80 km/h when you're in them, 100 km/h in the 1500 m before them in your direction
     * of travel; null elsewhere. Only a guess for roadworks with no posted limit (the caller leaves it out when one is posted).
     */
    internal fun defaultLimitAt(list: List<RoadworkZone>, p: LL, heading: Double, now: Long): Double? {
        var ahead = false
        for (z in list) {
            val w = z.windows
            if (w != null) { if (w.none { now in it }) continue } else if (z.from > now) continue
            for (i in 0 until z.pts.size - 1) {
                val a = z.pts[i]; val b = z.pts[i + 1]
                if (segDist(p, a, b) > 25.0) continue
                if (heading >= 0) {
                    val d = ((Geo.bearing(a, b) - heading) % 360 + 540) % 360 - 180
                    if (Math.abs(d) > 45.0) continue
                }
                return 80.0 / 3.6
            }
            val s = z.pts.first()
            val dist = Geo.dist(p, s)
            if (heading >= 0 && dist in 30.0..1500.0) {
                val toStart = ((Geo.bearing(p, s) - heading) % 360 + 540) % 360 - 180
                val dir = ((Geo.bearing(z.pts[0], z.pts[1]) - heading) % 360 + 540) % 360 - 180
                if (Math.abs(toStart) < 30.0 && Math.abs(dir) < 45.0) ahead = true
            }
        }
        return if (ahead) 100.0 / 3.6 else null
    }

    private val MAX_RE = Regex("Max\\.\\s*(\\d{2,3})\\s*km/h", RegexOption.IGNORE_CASE)

    internal fun parseLimit(o: JSONObject): RoadworkLimit? {
        val desc = o.optJSONArray("description")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        val kmh = desc.firstNotNullOfOrNull { MAX_RE.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: return null
        if (kmh < 10 || kmh > 130) return null
        val g = o.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return null
        val pts = (0 until g.length()).mapNotNull { i -> g.optJSONArray(i)?.let { LL(it.optDouble(1), it.optDouble(0)) } }
        if (pts.size < 2) return null
        val from = parseIso(o.optString("startTimestamp"))
        return RoadworkLimit(pts, kmh / 3.6, windows(desc), from)
    }

    private val BERLIN = java.util.TimeZone.getTimeZone("Europe/Berlin")
    private fun at(d: String, t: String): Long? = try {
        java.text.SimpleDateFormat("dd.MM.yy HH:mm", java.util.Locale.GERMANY).apply { timeZone = BERLIN; isLenient = false }.parse("$d $t")?.time
    } catch (e: Exception) { null }

    private val DAY_RE = Regex("(\\d{2}\\.\\d{2}\\.\\d{2})\\s+von\\s+(\\d{1,2}:\\d{2})\\s+bis\\s+(\\d{1,2}:\\d{2})")
    private val BEGIN_RE = Regex("Beginn:\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+um\\s+(\\d{1,2}:\\d{2})")
    private val END_RE = Regex("Ende:\\s*(\\d{2}\\.\\d{2}\\.\\d{2})\\s+um\\s+(\\d{1,2}:\\d{2})")

    /**
     * When the roadworks apply, from their text: "10.11.26 von 09:30 bis 16:00 Uhr" lines (short ones, a window each day) or
     * "Beginn: 09.11.26 um 00:00 Uhr" / "Ende: 05.12.26 um 00:00 Uhr". Null when the text says neither (then they always apply).
     */
    internal fun windows(desc: List<String>): List<LongRange>? {
        val out = ArrayList<LongRange>()
        for (line in desc) for (m in DAY_RE.findAll(line)) {
            val a = at(m.groupValues[1], m.groupValues[2]) ?: continue
            var b = at(m.groupValues[1], m.groupValues[3]) ?: continue
            if (b <= a) b += 24 * 3600_000L      // over midnight
            out += a..b
        }
        if (out.isNotEmpty()) return out
        val begin = desc.firstNotNullOfOrNull { BEGIN_RE.find(it) }?.let { at(it.groupValues[1], it.groupValues[2]) }
        val end = desc.firstNotNullOfOrNull { END_RE.find(it) }?.let { at(it.groupValues[1], it.groupValues[2]) }
        if (begin == null && end == null) return null
        return listOf((begin ?: 0L)..(end ?: Long.MAX_VALUE))
    }

    private fun parseIso(s: String): Long = if (s.isBlank()) 0L else try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(s)?.time ?: 0L
    } catch (e: Exception) { 0L }

    /** The limit (m/s) of roadworks you're in now at [p] driving at [heading]: on its line (25 m) and going its way; else null. */
    internal fun limitAt(list: List<RoadworkLimit>, p: LL, heading: Double, now: Long): Double? {
        for (rw in list) {
            val w = rw.windows
            if (w != null) { if (w.none { now in it }) continue } else if (rw.from > now) continue
            for (i in 0 until rw.pts.size - 1) {
                val a = rw.pts[i]; val b = rw.pts[i + 1]
                if (segDist(p, a, b) > 25.0) continue
                if (heading >= 0) {
                    val d = ((Geo.bearing(a, b) - heading) % 360 + 540) % 360 - 180
                    if (Math.abs(d) > 45.0) continue
                }
                return rw.ms
            }
        }
        return null
    }

    private fun segDist(p: LL, a: LL, b: LL): Double {
        val kx = 111_320.0 * Math.cos(Math.toRadians(p.lat))
        val ax = (a.lon - p.lon) * kx; val ay = (a.lat - p.lat) * 111_320.0
        val bx = (b.lon - p.lon) * kx; val by = (b.lat - p.lat) * 111_320.0
        val dx = bx - ax; val dy = by - ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
        return Math.hypot(ax + t * dx, ay + t * dy)
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
