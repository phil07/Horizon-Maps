package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

/**
 * Posted speed limits on Dutch roads from Rijkswaterstaat's WEGGEG service (PDOK, OGC API Features, no key needed, CC0).
 * Only used for positions in the Netherlands. Each answer is a road segment with its limit in km/h; the closest segment
 * near the position, driven in about the direction of travel, is the limit. Source credit: "Daten: Rijkswaterstaat (CC0)".
 */
object RwsSpeedLimits {
    private const val ITEMS = "https://api.pdok.nl/rws/weggegevens/ogc/v1/collections/wegvak_max_snelheden/items"
    /** How close (m) a segment must be to the position to count. Both carriageways of a motorway lie about 55 m apart, so this must reach the other one. */
    private const val NEAR_M = 70.0
    /** How far (degrees) the query box reaches around the position: about 90 m north-south, 70 m east-west. */
    private const val BOX = 0.0011
    /** Answers stay valid for this long (ms) and this far (m) from where they were asked. */
    private const val TTL_MS = 10 * 60_000L
    private const val REUSE_M = 120.0

    internal class Seg(val pts: List<LL>, val kmh: Int)
    private class Cached(val at: LL, val time: Long, val segs: List<Seg>)

    @Volatile private var cache: Cached? = null

    /** The posted limit (m/s) of the Dutch road at [p] driven at [heading] (degrees, -1 when unknown), or null when there is none. Never throws. */
    fun limitAt(p: LL, heading: Double): Double? = try {
        val now = System.currentTimeMillis()
        val c = cache
        val segs = if (c != null && now - c.time < TTL_MS && Geo.dist(c.at, p) < REUSE_M) c.segs
            else fetch(p).also { cache = Cached(p, now, it) }
        best(segs, p, heading)?.let { it / 3.6 }
    } catch (e: Exception) { null }

    /** The segment closest to [p] within NEAR_M, preferring one whose direction fits [heading]. */
    internal fun best(segs: List<Seg>, p: LL, heading: Double): Double? {
        var bestKmh: Int? = null
        var bestScore = Double.MAX_VALUE
        for (s in segs) {
            if (s.pts.size < 2) continue
            var dmin = Double.MAX_VALUE
            var brg = 0.0
            for (i in 1 until s.pts.size) {
                val d = Geo.dist(p, s.pts[i - 1]).coerceAtMost(Geo.dist(p, s.pts[i]))
                if (d < dmin) { dmin = d; brg = Geo.bearing(s.pts[i - 1], s.pts[i]) }
            }
            if (dmin > NEAR_M) continue
            // a segment that runs against our direction is the other carriageway: it only counts when nothing that fits is near
            val off = if (heading < 0) 0.0 else angle(brg, heading)
            val score = dmin + if (off > 60.0) 200.0 else 0.0
            if (score < bestScore) { bestScore = score; bestKmh = s.kmh }
        }
        return bestKmh?.takeIf { it > 0 }?.toDouble()
    }

    private fun angle(a: Double, b: Double): Double { val d = abs(a - b) % 360.0; return if (d > 180.0) 360.0 - d else d }

    private fun fetch(p: LL): List<Seg> {
        val box = "${p.lon - BOX},${p.lat - BOX},${p.lon + BOX},${p.lat + BOX}"
        val arr = JSONObject(get("$ITEMS?f=json&limit=50&bbox=$box")).optJSONArray("features") ?: return emptyList()
        val out = ArrayList<Seg>()
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val kmh = f.optJSONObject("properties")?.optInt("omschr", 0) ?: 0
            val g = f.optJSONObject("geometry") ?: continue
            val lines = when (g.optString("type")) {
                "MultiLineString" -> g.optJSONArray("coordinates")
                "LineString" -> org.json.JSONArray().put(g.optJSONArray("coordinates"))
                else -> null
            } ?: continue
            for (l in 0 until lines.length()) {
                val c = lines.optJSONArray(l) ?: continue
                val pts = ArrayList<LL>()
                for (n in 0 until c.length()) c.optJSONArray(n)?.let { q -> pts += LL(q.optDouble(1), q.optDouble(0)) }
                if (pts.size >= 2) out += Seg(pts, kmh)
            }
        }
        return out
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        c.setRequestProperty("Accept", "application/geo+json")
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IllegalStateException("RWS HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
