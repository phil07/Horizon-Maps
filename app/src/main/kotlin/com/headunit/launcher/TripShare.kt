package com.headunit.launcher

import org.json.JSONObject

/**
 * "Ankunftszeit teilen": a link to a web page (on the reports server) that shows where you are, your arrival time and the
 * distance left, kept up to date every 30 seconds. The page shows the trip as over once you arrive, stop sharing, or after
 * 30 minutes without an update. Only the app holds the key that can update the trip.
 */
object TripShare {
    class Link(val id: String, val key: String, val url: String)

    /** The sign-off for an update that the server refused because the trip is gone or over. */
    class Gone : Exception()

    private fun body(pos: LL, heading: Double, etaMs: Long, remM: Double, name: String? = null): String {
        val o = JSONObject().put("lat", pos.lat).put("lon", pos.lon).put("heading", if (heading >= 0) heading else -1.0)
            .put("eta", etaMs).put("rem", Math.round(remM))
        if (name != null) o.put("name", name)
        return o.toString()
    }

    fun create(prefs: Prefs, name: String, pos: LL, heading: Double, etaMs: Long, remM: Double): Link {
        val base = SharedReports.baseUrl(prefs)
        if (base.isBlank()) throw java.io.IOException(Lang.t("Kein Server eingestellt", "No server set"))
        val o = JSONObject(SharedReports.request("POST", "$base/v1/trips", SharedReports.deviceId(prefs), body(pos, heading, etaMs, remM, name.take(40))))
        val id = o.optString("id"); val key = o.optString("key")
        if (id.isBlank() || key.isBlank()) throw java.io.IOException("bad answer")
        return Link(id, key, o.optString("url").ifBlank { "$base/t/$id" })
    }

    fun update(prefs: Prefs, l: Link, pos: LL, heading: Double, etaMs: Long, remM: Double) = send(prefs, l, body(pos, heading, etaMs, remM))

    fun end(prefs: Prefs, l: Link) = send(prefs, l, "{\"end\":true}")

    private fun send(prefs: Prefs, l: Link, json: String) {
        val base = SharedReports.baseUrl(prefs)
        val c = java.net.URL("$base/v1/trips/${l.id}").openConnection() as java.net.HttpURLConnection
        try {
            c.connectTimeout = 8000; c.readTimeout = 8000
            c.requestMethod = "POST"
            c.setRequestProperty("User-Agent", "HorizonNav/1.0")
            c.setRequestProperty("X-Device", SharedReports.deviceId(prefs))
            c.setRequestProperty("X-Trip-Key", l.key)
            c.setRequestProperty("Content-Type", "application/json")
            c.doOutput = true
            c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            val code = c.responseCode
            if (code == 404 || code == 409) throw Gone()
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
        } finally { try { c.disconnect() } catch (e: Exception) { } }
    }

    /** The message handed to the share menu. */
    fun message(arrive: String?, url: String): String =
        (if (arrive != null) Lang.t("Ich bin unterwegs, Ankunft etwa $arrive.", "I'm on my way, arriving about $arrive.") else Lang.t("Ich bin unterwegs.", "I'm on my way.")) +
            Lang.t(" Hier siehst du live, wo ich gerade bin: ", " See where I am, live: ") + url
}
