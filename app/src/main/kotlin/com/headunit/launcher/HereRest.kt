package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Speed limits from HERE (your own key, Settings > Map accounts). HERE's road data comes from its camera cars and is the most
 * complete and up to date in Europe, so where it has a limit it's used ahead of the others (see NavEngine.lookUpLimit).
 * The Route Matching API matches your last few positions to HERE's roads and gives the limit of each road it passes.
 */
object HereRest {
    private const val API = "https://routematching.hereapi.com/v8/match/routelinks"

    /** Set when the last request failed, for the feedback report; null when it worked. */
    @Volatile var lastError: String? = null

    /**
     * The posted limit (m/s) of the road at the end of [trail] (oldest first): -1 where there is no limit (an unrestricted
     * Autobahn), null when HERE couldn't tell.
     */
    fun speedLimit(key: String, trail: List<LL>): Double? {
        if (trail.size < 2 || key.isBlank()) return null
        val sb = StringBuilder(API).append("?apikey=").append(enc(key))
            .append("&routeMatch=1&mode=").append(enc("fastest;car;traffic:disabled"))
            .append("&attributes=").append(enc("SPEED_LIMITS_FCn(*)"))
        trail.forEachIndexed { i, p -> sb.append("&waypoint").append(i).append('=').append(String.format(java.util.Locale.US, "%.6f,%.6f", p.lat, p.lon)) }
        val body = try { get(sb.toString()) } catch (e: Exception) { lastError = e.message ?: e.javaClass.simpleName; return null }
        lastError = null
        val root = JSONObject(body)
        val links = root.optJSONArray("RouteLinks") ?: return null
        if (links.length() == 0) return null
        // the road the last position was matched to (its id is signed: positive when you drive away from its start)
        val tps = root.optJSONArray("TracePoints")
        val lastId = tps?.optJSONObject(tps.length() - 1)?.optLong("linkIdMatched", 0L) ?: 0L
        var link: JSONObject? = null
        for (i in 0 until links.length()) { val l = links.optJSONObject(i) ?: continue; if (lastId != 0L && l.optLong("linkId", 0L) == lastId) link = l }
        if (link == null) link = links.optJSONObject(links.length() - 1) ?: return null
        val linkId = link.optLong("linkId", lastId)
        val attrs = link.optJSONObject("attributes") ?: return null
        val key0 = attrs.keys().asSequence().firstOrNull { it.uppercase().startsWith("SPEED_LIMITS_FC") } ?: return null
        val sl = attrs.optJSONArray(key0)?.optJSONObject(0) ?: return null
        val raw = sl.optString(if (linkId >= 0) "FROM_REF_SPEED_LIMIT" else "TO_REF_SPEED_LIMIT").trim().toIntOrNull() ?: return null
        // 0: no limit known for this direction; 998: variable (overhead signs); 999: no limit at all
        return when {
            raw >= 999 -> -1.0
            raw <= 0 || raw == 998 -> null
            sl.optString("SPEED_LIMIT_UNIT").equals("M", true) -> raw * 0.44704
            else -> raw / 3.6
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 8000
        try {
            val code = c.responseCode
            if (code !in 200..299) throw IllegalStateException("HERE HTTP $code")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally { c.disconnect() }
    }
}
