package com.headunit.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A speed limit sign Mapillary recognised on street photos. [ms] the limit (m/s), or -1 for an "end of limit" sign. */
internal data class MlySign(val pos: LL, val ms: Double, val facing: Double, val seenAt: Long) {
    val end: Boolean get() = ms <= 0.0
}

/**
 * Speed limit signs from Mapillary (free, needs a client token from mapillary.com/dashboard/developers, Settings > Kartenkonten):
 * signs recognised on photos people took of the road, with where they stand and which way they face. They're fetched for a
 * few square kilometres at a time and kept, so most checks need no request at all.
 */
internal object Mapillary {
    @Volatile var lastError: String? = null

    /** What the last request brought, with the time, shown under the token in Settings. */
    var status by androidx.compose.runtime.mutableStateOf(""); private set

    private val lock = Any()
    private var signs: List<MlySign> = emptyList()
    private var box: DoubleArray? = null      // minLon, minLat, maxLon, maxLat of what's held
    private var boxAt = 0L
    private var failedAt = 0L

    /** Half the size of the area fetched at once, in degrees (Mapillary allows up to 0.01 square degrees per request). */
    private const val HALF = 0.012
    /** A new area is fetched once you're this close to the edge of the one held. */
    private const val MARGIN = 0.004
    private const val KEEP_MS = 6L * 3600 * 1000

    /** The signs around [p], from what's held or fetched now; null when they couldn't be fetched. [mph]: signs in mph (UK, US). */
    fun signsAround(token: String, p: LL, mph: Boolean): List<MlySign>? {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val b = box
            if (b != null && now - boxAt < KEEP_MS && p.lon > b[0] + MARGIN && p.lon < b[2] - MARGIN && p.lat > b[1] + MARGIN && p.lat < b[3] - MARGIN) return signs
            // a failed request is tried again after half a minute, not on every check
            if (now - failedAt < 30_000L) return if (b != null && p.lon in b[0]..b[2] && p.lat in b[1]..b[3]) signs else null
        }
        val nb = doubleArrayOf(p.lon - HALF, p.lat - HALF, p.lon + HALF, p.lat + HALF)
        val found = try { fetch(token, nb, mph) } catch (e: Exception) {
            lastError = e.message ?: e.javaClass.simpleName
            note(Lang.t("Fehler: ", "Error: ") + lastError)
            synchronized(lock) { failedAt = now }
            return null
        }
        lastError = null
        note(Lang.t("${found.size} Tempo-Schilder in der Umgebung", "${found.size} speed signs around"))
        synchronized(lock) { signs = found; box = nb; boxAt = now }
        return found
    }

    private fun note(s: String) {
        val at = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        try { android.os.Handler(android.os.Looper.getMainLooper()).post { status = "$at: $s" } } catch (e: Throwable) { }
    }

    private fun fetch(token: String, b: DoubleArray, mph: Boolean): List<MlySign> {
        val bbox = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f", b[0], b[1], b[2], b[3])
        val url = "https://graph.mapillary.com/map_features?access_token=${URLEncoder.encode(token.trim(), "UTF-8")}" +
            "&fields=id,object_value,geometry,aligned_direction,last_seen_at&limit=2000&bbox=$bbox" +
            "&object_values=${URLEncoder.encode("regulatory--*speed-limit*", "UTF-8")}"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 9000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw java.io.IOException("HTTP $code" + when (code) { 401, 403 -> Lang.t(" (Token ungültig)", " (token invalid)"); 429 -> Lang.t(" (zu viele Abfragen)", " (too many requests)"); else -> "" })
        val arr = JSONObject(body).optJSONArray("data") ?: return emptyList()
        val out = ArrayList<MlySign>()
        for (i in 0 until arr.length()) {
            val x = arr.optJSONObject(i) ?: continue
            val v = parseValue(x.optString("object_value"), mph) ?: continue
            val co = x.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            val facing = if (x.has("aligned_direction") && !x.isNull("aligned_direction")) x.optDouble("aligned_direction", -1.0) else -1.0
            out += MlySign(LL(co.optDouble(1), co.optDouble(0)), v, if (facing.isNaN()) -1.0 else facing, parseTime(x.opt("last_seen_at")))
        }
        return out
    }

    private val LIMIT = Regex("^regulatory--maximum-speed-limit(?:-zone)?-(\\d+)(?:--|$)")
    private val END = Regex("^regulatory--end-of-(?:maximum-)?speed-limit")

    /** "regulatory--maximum-speed-limit-50--g1" → 50 km/h in m/s; an "end of" sign → -1; anything else (minimum speed…) → null. */
    fun parseValue(v: String, mph: Boolean): Double? {
        if (END.containsMatchIn(v)) return -1.0
        val n = LIMIT.find(v)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (n < 5 || n > 160) return null
        return if (mph) n * 0.44704 else n / 3.6
    }

    /** last_seen_at: an ISO time ("2021-12-23T03:24:54+0000") or milliseconds; 0 when unknown. */
    fun parseTime(t: Any?): Long {
        if (t is Number) return t.toLong()
        val s = t as? String ?: return 0L
        s.toLongOrNull()?.let { return it }
        for (f in listOf("yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ssXXX")) {
            try { return java.text.SimpleDateFormat(f, java.util.Locale.US).parse(s)?.time ?: continue } catch (e: Exception) { }
        }
        return 0L
    }

    /**
     * The sign you passed most recently at [p] driving at [heading] (degrees): behind you (up to [maxBack] metres), close to
     * your line of travel, and facing the traffic you're in (so not one for the other carriageway or a side road). Null when none.
     */
    fun passed(signs: List<MlySign>, p: LL, heading: Double, maxBack: Double = 350.0): MlySign? {
        if (heading < 0) return null
        val h = Math.toRadians(heading)
        val ux = Math.sin(h); val uy = Math.cos(h)
        val kx = 111_320.0 * Math.cos(Math.toRadians(p.lat))
        var best: MlySign? = null; var bestAlong = Double.MAX_VALUE
        for (s in signs) {
            val dx = (p.lon - s.pos.lon) * kx
            val dy = (p.lat - s.pos.lat) * 111_320.0
            val along = dx * ux + dy * uy
            if (along < 0.0 || along > maxBack) continue
            val lateral = Math.abs(dx * uy - dy * ux)
            // signs stand at the roadside (a few metres out); a little more allowed further back, for bends
            if (lateral > 18.0 + along * 0.06) continue
            if (s.facing >= 0 && angle(s.facing, heading + 180.0) > 60.0) continue
            if (along < bestAlong) { best = s; bestAlong = along }
        }
        return best
    }

    private fun angle(a: Double, b: Double): Double { val d = ((a - b) % 360 + 540) % 360 - 180; return Math.abs(d) }
}
