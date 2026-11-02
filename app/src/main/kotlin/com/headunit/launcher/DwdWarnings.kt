package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A weather warning from the Deutscher Wetterdienst for one district: what, how bad (2 markant … 4 extrem), when, and where. */
internal data class DwdWarning(
    val id: String, val event: String, val level: Int, val headline: String, val area: String,
    val onset: Long, val expires: Long, val description: String,
    /** The district's rough box: south, west, north, east. */
    val box: DoubleArray?,
)

/**
 * Official weather warnings from the Deutscher Wetterdienst (free, no key: the DWD GeoServer), for the districts a line
 * crosses (the route ahead, or just where you are). Only "markant" (level 2) and worse: ice, storm gusts, heavy rain, thick fog.
 */
internal object DwdWarnings {
    private const val URL_BASE = "https://maps.dwd.de/geoserver/dwd/ows?service=WFS&version=2.0.0&request=GetFeature" +
        "&typeName=dwd:Warnungen_Landkreise&outputFormat=application/json" +
        "&propertyName=IDENTIFIER,EVENT,SEVERITY,HEADLINE,AREADESC,ONSET,EXPIRES,DESCRIPTION"

    fun level(severity: String): Int = when (severity) { "Minor" -> 1; "Moderate" -> 2; "Severe" -> 3; "Extreme" -> 4; else -> 0 }

    /** The warnings (level 2 and up) for the districts [line] passes through (lat/lon points; one point is fine). Null when no answer. */
    fun along(line: List<LL>): List<DwdWarning>? {
        if (line.isEmpty()) return emptyList()
        val geom = if (line.size == 1) String.format(java.util.Locale.US, "POINT(%.4f %.4f)", line[0].lat, line[0].lon)
            else "LINESTRING(" + line.joinToString(",") { String.format(java.util.Locale.US, "%.4f %.4f", it.lat, it.lon) } + ")"
        val cql = "SEVERITY IN ('Moderate','Severe','Extreme') AND INTERSECTS(THE_GEOM,$geom)"
        return try {
            val c = URL(URL_BASE + "&CQL_FILTER=" + URLEncoder.encode(cql, "UTF-8").replace("+", "%20")).openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 12000
            c.setRequestProperty("User-Agent", "HorizonNav/1.0")
            val code = c.responseCode
            if (code !in 200..299) return null
            parse(c.inputStream.bufferedReader().use { it.readText() })
        } catch (e: Exception) { null }
    }

    private fun parse(body: String): List<DwdWarning> {
        val arr = JSONObject(body).optJSONArray("features") ?: return emptyList()
        val out = ArrayList<DwdWarning>()
        for (i in 0 until arr.length()) {
            val f = arr.optJSONObject(i) ?: continue
            val p = f.optJSONObject("properties") ?: continue
            val lv = level(p.optString("SEVERITY"))
            if (lv < 2) continue
            val b = f.optJSONArray("bbox")
            // (the box comes as lat, lon pairs: south, west, north, east)
            val box = if (b != null && b.length() == 4) doubleArrayOf(b.optDouble(0), b.optDouble(1), b.optDouble(2), b.optDouble(3)) else null
            out += DwdWarning(p.optString("IDENTIFIER") + "|" + p.optString("EVENT"), p.optString("EVENT"), lv, p.optString("HEADLINE"),
                p.optString("AREADESC"), iso(p.optString("ONSET")), iso(p.optString("EXPIRES")), p.optString("DESCRIPTION"), box)
        }
        return out
    }

    private fun iso(s: String): Long = try { java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", java.util.Locale.US).parse(s)?.time ?: 0L } catch (e: Exception) { 0L }

    /** "GLÄTTE" → "Glätte", "STURMBÖEN" → "Sturmböen" (the DWD writes events in capitals). */
    fun eventName(e: String): String = e.lowercase().split(' ').joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }

    /** The spoken and shown text: "Unwetterwarnung: Glätte, Kreis Kassel, ab 18 Uhr". */
    fun text(w: DwdWarning, now: Long): String {
        val what = eventName(w.event)
        val kind = if (w.level >= 3) Lang.t("Unwetterwarnung", "Severe weather warning") else Lang.t("Wetterwarnung", "Weather warning")
        val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY)
        val whenTxt = if (w.onset > now + 10 * 60_000L) Lang.t(", ab ${fmt.format(java.util.Date(w.onset))} Uhr", ", from ${fmt.format(java.util.Date(w.onset))}")
            else if (w.expires > 0) Lang.t(", bis ${fmt.format(java.util.Date(w.expires))} Uhr", ", until ${fmt.format(java.util.Date(w.expires))}") else ""
        return "$kind: $what, ${w.area}$whenTxt"
    }

    /** Whether a warning matters within the next [aheadMs]: starts before then and hasn't ended. */
    fun current(w: DwdWarning, now: Long, aheadMs: Long): Boolean = (w.onset == 0L || w.onset <= now + aheadMs) && (w.expires == 0L || w.expires > now)

    /** Roughly in Germany (the DWD only covers Germany). */
    fun inGermany(p: LL): Boolean = p.lat in 47.2..55.1 && p.lon in 5.8..15.1
}
