package com.headunit.launcher

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

/**
 * Reports shared with other drivers (police, accident, hazard, breakdown, mobile camera, traffic jam) through Horizon's own small
 * reports server (a small Cloudflare Worker). Anonymous: only the kind, the position, the time and a random id made once on
 * this device are sent. The server's address is built into the app (BuildConfig.REPORTS_URL) and can be replaced in Settings; with
 * no address at all the whole feature is quietly off.
 *
 * Blocking: call off the main thread. Nothing here throws, and a failed request only notes [lastError] (for the settings page),
 * so a weak signal never pops up messages while driving.
 */
object SharedReports {
    private const val UA = "HorizonNav/1.0"
    private const val TIMEOUT_MS = 8000

    /** A report from the server. [exp] is already moved to this device's clock (the server's clock can differ a little). */
    class Item(val id: Long, val kind: String, val pos: LL, val t: Long, val exp: Long, val up: Int, val down: Int, val mine: Boolean = false)

    /** The last list fetched (all of the server's answer, before it's narrowed to the route and the car's surroundings). */
    @Volatile var items: List<Item> = emptyList()
        private set
    /** How many shared reports are shown now (set by the engine after narrowing), for the settings page. */
    @Volatile var shownCount = 0
    /** When the last request went through, and the last failure ("" when the last request worked). */
    @Volatile var lastOkAt = 0L
        private set
    @Volatile var lastError = ""
        private set

    /** Reports voted away ("Weg") on this device: hidden at once, on the phone and on Android Auto, without waiting for the server. */
    private val gone = java.util.Collections.synchronizedSet(HashSet<Long>())
    fun markGone(id: Long) { gone.add(id) }
    fun isGone(id: Long) = id in gone
    /** Ids of reports sent from this device in this session (the server can merge yours into one nearby: that one is then yours too). */
    private val own = java.util.Collections.synchronizedSet(HashSet<Long>())
    fun markOwn(id: Long) { own.add(id) }
    fun isOwn(id: Long) = id in own

    /** The server's address: the one from Settings, else the built-in one; "" when there is none (the feature is then off). */
    fun baseUrl(prefs: Prefs): String {
        var u = prefs.s(PreferenceKeys.REPORTS_URL, "").trim().ifBlank { BuildConfig.REPORTS_URL.trim() }
        if (u.isBlank()) return ""
        if (!u.contains("://")) u = "https://$u"
        return u.trimEnd('/')
    }

    fun configured(prefs: Prefs) = baseUrl(prefs).isNotBlank()

    /**
     * Sharing is on (Settings, on by default): your own reports go to the server, other drivers' are fetched and shown, and you can
     * answer "Noch da / Weg". Off means no network at all for reports.
     */
    fun sharing(prefs: Prefs) = configured(prefs) && prefs.b(PreferenceKeys.REPORTS_SHARE, true)

    /** This device's random id: made once and kept (it identifies nothing but this install). */
    fun deviceId(prefs: Prefs): String {
        val have = prefs.s(PreferenceKeys.REPORTS_DEVICE, "")
        if (have.length >= 16) return have
        val id = UUID.randomUUID().toString()
        prefs.put(PreferenceKeys.REPORTS_DEVICE, id)
        return id
    }

    /** Sends a new report; returns its id on the server (an existing one when the server merged it into a report nearby), or null. */
    fun send(prefs: Prefs, kind: String, pos: LL): Long? {
        val base = baseUrl(prefs)
        if (base.isBlank()) return null
        return try {
            val body = JSONObject().put("kind", kind).put("lat", round5(pos.lat)).put("lon", round5(pos.lon)).toString()
            val o = JSONObject(request("POST", "$base/v1/reports", deviceId(prefs), body))
            val id = o.optLong("id", 0L)
            ok()
            if (id > 0) id else null
        } catch (e: Throwable) { fail(e); null }
    }

    /**
     * The reports in force inside the box (south, west, north, east in degrees; at most 6 degrees per side). Returns null when the
     * request failed (the caller keeps what it had).
     */
    fun fetch(prefs: Prefs, s: Double, w: Double, n: Double, e: Double): List<Item>? {
        val base = baseUrl(prefs)
        if (base.isBlank()) return null
        return try {
            fun f(v: Double) = String.format(Locale.US, "%.5f", v)
            val text = request("GET", "$base/v1/reports?s=${f(s)}&w=${f(w)}&n=${f(n)}&e=${f(e)}", deviceId(prefs), null)
            val o = JSONObject(text)
            val local = System.currentTimeMillis()
            // the server's clock against ours, so a report's end time means the same here
            val skew = if (o.has("now")) o.optLong("now", local) - local else 0L
            val arr = o.optJSONArray("reports")
            val out = ArrayList<Item>()
            if (arr != null) for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                val id = r.optLong("id", 0L)
                val kind = r.optString("kind", "")
                val lat = r.optDouble("lat", Double.NaN); val lon = r.optDouble("lon", Double.NaN)
                if (id <= 0 || kind.isBlank() || lat.isNaN() || lon.isNaN()) continue
                val exp = r.optLong("exp", 0L) - skew
                if (exp <= local) continue
                // ("mine": sent from this device; never shown as someone else's, and gone for good once you delete your own reports)
                out += Item(id, kind, LL(lat, lon), r.optLong("t", local) - skew, exp, r.optInt("up", 0), r.optInt("down", 0), r.optBoolean("mine", false))
            }
            ok()
            out
        } catch (e: Throwable) { fail(e); null }
    }

    /** Keeps the last fetched list (all boxes of one refresh together). */
    fun remember(list: List<Item>) { items = list }

    /** "Noch da" (still = true) or "Weg" for a report from someone else. True when the server took it. */
    fun vote(prefs: Prefs, id: Long, still: Boolean): Boolean {
        val base = baseUrl(prefs)
        if (base.isBlank()) return false
        return try {
            request("POST", "$base/v1/reports/$id/vote", deviceId(prefs), JSONObject().put("still", still).toString())
            ok()
            true
        } catch (e: Throwable) {
            // (an expired or already removed report answers 404: nothing went wrong with the server)
            if (e.message == "HTTP 404") ok() else fail(e)
            false
        }
    }

    private fun round5(v: Double) = Math.round(v * 1e5) / 1e5

    private fun ok() { lastOkAt = System.currentTimeMillis(); lastError = "" }

    private fun fail(e: Throwable) {
        lastError = (e.message ?: e.javaClass.simpleName).take(80)
    }

    private fun request(method: String, url: String, device: String, body: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = TIMEOUT_MS
            c.readTimeout = TIMEOUT_MS
            c.requestMethod = method
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("X-Device", device)
            c.setRequestProperty("Accept", "application/json")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code == 429) throw IOException(Lang.t("Zu viele Meldungen, bitte später", "Too many reports, try later"))
            if (code !in 200..299) throw IOException("HTTP $code")
            return text
        } finally { try { c.disconnect() } catch (e: Exception) { } }
    }
}
