package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live availability of charge points in Baden-Württemberg from MobiData BW's open DATEX II feed (no key, no login). Each station is
 * keyed by its Bundesnetzagentur number, the same as in the register, so its free points can be shown on the map. Only inside the
 * state. Never throws.
 */
object BwLive {
    private const val URL_RT = "https://api.mobidata-bw.de/ocpdb/api/public/datex/v3.5/json/realtime"
    private const val TTL_MS = 3 * 60_000L

    /** [available] is -1 when the feed doesn't know the status of any point of the station. */
    class Status(val available: Int, val total: Int)

    @Volatile private var cache: Map<String, Status> = emptyMap()
    @Volatile private var at = 0L

    /** Whether the map box [box] (minLat, minLon, maxLat, maxLon) reaches into Baden-Württemberg. */
    fun inBw(box: DoubleArray): Boolean = box[0] < 49.8 && box[2] > 47.5 && box[1] < 10.5 && box[3] > 7.4

    /** The stations' status, fetched at most every three minutes. Blocking: call off the main thread. */
    fun statuses(): Map<String, Status> {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - at < TTL_MS) return cache
        try {
            val c = URL(URL_RT).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000; c.readTimeout = 20_000
                c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
                if (c.responseCode in 200..299) {
                    val text = c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    cache = parse(text); at = now
                }
            } finally { c.disconnect() }
        } catch (e: Exception) { }
        return cache
    }

    /** The sites with their free and total points from the feed (where the feed knows the station). */
    fun apply(sites: List<ChargerSite>, st: Map<String, Status>): List<ChargerSite> {
        if (st.isEmpty()) return sites
        return sites.map { s ->
            val x = s.id?.let { st[it] } ?: return@map s
            ChargerSite(s.pos, s.kw, x.total.takeIf { it > 0 } ?: s.total, x.available.takeIf { it >= 0 }, s.op, s.kwLabel, s.id)
        }
    }

    internal fun parse(text: String): Map<String, Status> {
        val out = HashMap<String, Status>()
        val sites = JSONObject(text).getJSONObject("payload")
            .getJSONObject("aegiEnergyInfrastructureStatusPublication").optJSONArray("energyInfrastructureSiteStatus") ?: return out
        for (i in 0 until sites.length()) {
            val stations = sites.optJSONObject(i)?.optJSONArray("energyInfrastructureStationStatus") ?: continue
            for (k in 0 until stations.length()) {
                val station = stations.optJSONObject(k) ?: continue
                val id = station.optJSONObject("reference")?.optString("idG").orEmpty()
                if (id.isBlank()) continue
                val points = station.optJSONArray("refillPointStatus") ?: continue
                var free = 0; var known = 0
                for (p in 0 until points.length()) {
                    val v = points.optJSONObject(p)?.optJSONObject("aegiRefillPointStatus")?.optJSONObject("status")?.optString("value").orEmpty()
                    if (v.isEmpty() || v == "unknown") continue
                    known++
                    if (v == "available") free++
                }
                out[id] = Status(if (known > 0) free else -1, points.length())
            }
        }
        return out
    }
}
