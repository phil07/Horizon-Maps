package com.headunit.launcher

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * The heights along a route, for the battery estimate (climbing costs, going down gives some back). From Open-Meteo's elevation
 * service (free, no key, about 90 m grid), and Mapbox's terrain data when that fails and a Mapbox token is set.
 */
object Elevation {
    class Profile(val off: DoubleArray, val h: DoubleArray)

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000; c.readTimeout = 12000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code")
        return body
    }

    /** Where along [len] metres to look: about every 500 m, at most [max] points (spread further apart on a long route). */
    fun sampleOffsets(len: Double, max: Int): DoubleArray {
        if (len <= 0.0) return DoubleArray(0)
        val step = maxOf(500.0, len / (max - 1))
        val n = (len / step).toInt() + 1
        return DoubleArray(n + 1) { i -> if (i == n) len else i * step }.distinct().toDoubleArray()
    }

    /**
     * The height profile of [r] ([at]: a point and its direction at a distance along it), or null when no service answered. The
     * heights are smoothed over about 1.5 km, so the grid's bumps (a bridge, a cutting) are not taken for hills.
     */
    fun along(r: Route, mapboxToken: String?, at: (Double) -> LL): Profile? {
        var offs = sampleOffsets(r.len, 400)
        if (offs.size < 2) return null
        val pts = offs.map(at)
        var h = try { openMeteo(pts) } catch (e: Exception) { null }
            ?: mapboxToken?.takeIf { it.isNotBlank() }?.let { tok ->
                // Mapbox answers one point per request: a coarser profile (at most 60 points)
                val o2 = sampleOffsets(r.len, 60)
                val hs = try { mapbox(o2.map(at), tok) } catch (e: Exception) { null }
                if (hs != null) return Profile(o2, smooth(hs, o2)) else null
            }
            ?: return null

        // Dynamic refinement: if a steep change is found, sample more points in that area
        for (i in 0 until h.size - 1) {
            if (Math.abs(h[i+1] - h[i]) > 50.0) {
                val mid = (offs[i] + offs[i+1]) / 2.0
                val newOffs = offs.toMutableList()
                newOffs.add(i + 1, mid)
                offs = newOffs.toDoubleArray()
                h = try { openMeteo(offs.map(at)) } catch (e: Exception) { h }
                break
            }
        }
        return Profile(offs, smooth(h, offs))
    }

    /** Heights for [pts], 100 per request (the service's limit). */
    private fun openMeteo(pts: List<LL>): DoubleArray {
        val out = DoubleArray(pts.size)
        var i = 0
        while (i < pts.size) {
            val chunk = pts.subList(i, minOf(pts.size, i + 100))
            val lat = chunk.joinToString(",") { String.format(java.util.Locale.US, "%.5f", it.lat) }
            val lon = chunk.joinToString(",") { String.format(java.util.Locale.US, "%.5f", it.lon) }
            val arr = JSONObject(get("https://api.open-meteo.com/v1/elevation?latitude=$lat&longitude=$lon")).getJSONArray("elevation")
            if (arr.length() != chunk.size) throw IOException("short answer")
            for (k in chunk.indices) {
                val v = arr.optDouble(k, Double.NaN)
                if (v.isNaN() || v < -500 || v > 9000) throw IOException("bad height")
                out[i + k] = v
            }
            i += chunk.size
        }
        return out
    }

    /** Heights from Mapbox's contour lines at each point (the highest contour there). */
    private fun mapbox(pts: List<LL>, token: String): DoubleArray {
        val out = DoubleArray(pts.size)
        for ((k, p) in pts.withIndex()) {
            val url = String.format(java.util.Locale.US,
                "https://api.mapbox.com/v4/mapbox.mapbox-terrain-v2/tilequery/%.5f,%.5f.json?layers=contour&limit=50&access_token=%s", p.lon, p.lat, token)
            val f = JSONObject(get(url)).optJSONArray("features") ?: throw IOException("no data")
            var best = Double.NaN
            for (j in 0 until f.length()) {
                val e = f.optJSONObject(j)?.optJSONObject("properties")?.optDouble("ele", Double.NaN) ?: continue
                if (!e.isNaN() && (best.isNaN() || e > best)) best = e
            }
            if (best.isNaN()) throw IOException("no height")
            out[k] = best
        }
        return out
    }

    /** Each height as the average of the heights within 750 m either side. */
    fun smooth(h: DoubleArray, off: DoubleArray): DoubleArray = DoubleArray(h.size) { i ->
        var s = 0.0; var n = 0
        for (j in h.indices) if (kotlin.math.abs(off[j] - off[i]) <= 750.0) { s += h[j]; n++ }
        if (n == 0) h[i] else s / n
    }
}
