package com.headunit.launcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Public charge points of the Brussels-Capital Region from Brussels Mobility's open data (CC BY 4.0, credit "Brussels Mobility"). Only
 * the DC (fast) points are kept: the file gives no power in kW, only voltages, so the AC points would all look the same. A DC point
 * without a known power counts as fast, like the other sources' unknowns. Stored once a week on the phone, searched there. Never throws.
 */
object BruChargers {
    private const val URL_JSON = "https://data.mobility.brussels/geoserver/bm_equipment/wfs?service=wfs&version=1.1.0&request=GetFeature" +
        "&typeName=bm_equipment:evse&outputFormat=json&srsName=EPSG:4326"
    private const val WEEK_MS = 7L * 86_400_000L
    private const val NEAR_M = 150.0
    private const val FILE = "bxl_chargers.tsv"

    private class Cp(val lat: Double, val lon: Double, val op: String)

    @Volatile private var loaded: List<Cp>? = null
    @Volatile private var loadedStamp = 0L

    /** Downloads the register when the stored copy is older than a week. Blocking: call off the main thread. */
    @Synchronized
    fun refresh(ctx: Context) {
        val f = File(ctx.filesDir, FILE)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < WEEK_MS) return
        val c = URL(URL_JSON).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 30_000
            c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
            if (c.responseCode !in 200..299) throw IllegalStateException("Brüssel HTTP ${c.responseCode}")
            val text = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val sb = StringBuilder()
            val feats = JSONObject(text).optJSONArray("features") ?: throw IllegalStateException("Brüssel: no features")
            for (i in 0 until feats.length()) {
                val p = feats.optJSONObject(i)?.optJSONObject("properties") ?: continue
                // DC only: the connector list names it ("DC" alone or with AC)
                if (!p.optString("connector_type").contains("DC")) continue
                val xy = feats.optJSONObject(i)?.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                val lon = xy.optDouble(0); val lat = xy.optDouble(1)
                if (lat.isNaN() || lon.isNaN()) continue
                sb.append(lat).append('\t').append(lon).append('\t').append(p.optString("operator_name").replace('\t', ' ').trim()).append('\n')
            }
            if (sb.isEmpty()) throw IllegalStateException("Brüssel: no DC points")
            val tmp = File(ctx.filesDir, "$FILE.part")
            tmp.writeText(sb.toString(), Charsets.UTF_8)
            if (f.exists()) f.delete()
            tmp.renameTo(f)
            loaded = null
        } finally { c.disconnect() }
    }

    /** The DC points near [line] (within [NEAR_M]). Refreshes the copy first when it is old. Never throws. */
    fun nearby(ctx: Context, line: List<LL>): List<Nearby> = try {
        if (line.size < 2) emptyList() else {
            try { refresh(ctx) } catch (e: Exception) { /* use the stored copy */ }
            along(ctx, line)
        }
    } catch (e: Exception) { emptyList() }

    /** The DC points inside [box] (minLat, minLon, maxLat, maxLon) for the map. */
    fun inBox(ctx: Context, box: DoubleArray): List<ChargerSite> =
        stored(ctx).filter { it.lat >= box[0] && it.lat <= box[2] && it.lon >= box[1] && it.lon <= box[3] }
            .map { ChargerSite(LL(it.lat, it.lon), null, null, null) }

    private fun along(ctx: Context, line: List<LL>): List<Nearby> {
        val cps = stored(ctx)
        if (cps.isEmpty()) return emptyList()
        val lat0 = line.minOf { it.lat } - 0.002; val lat1 = line.maxOf { it.lat } + 0.002
        val lon0 = line.minOf { it.lon } - 0.002; val lon1 = line.maxOf { it.lon } + 0.002
        val out = ArrayList<Nearby>()
        for (c in cps) {
            if (c.lat < lat0 || c.lat > lat1 || c.lon < lon0 || c.lon > lon1) continue
            val pos = LL(c.lat, c.lon)
            if (line.none { Geo.dist(it, pos) < NEAR_M }) continue
            out += Nearby(Place(c.op.ifBlank { "Schnellladepunkt" }, "Brüssel", pos), 0.0, "", kw = null, op = c.op.takeIf { it.isNotBlank() })
        }
        return out
    }

    private fun stored(ctx: Context): List<Cp> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        loaded?.takeIf { loadedStamp == f.lastModified() }?.let { return it }
        val list = ArrayList<Cp>()
        for (line in f.readLines(Charsets.UTF_8)) {
            val p = line.split('\t')
            if (p.size < 2) continue
            val lat = p[0].toDoubleOrNull() ?: continue
            val lon = p[1].toDoubleOrNull() ?: continue
            list += Cp(lat, lon, p.getOrElse(2) { "" })
        }
        loaded = list; loadedStamp = f.lastModified()
        return list
    }
}
