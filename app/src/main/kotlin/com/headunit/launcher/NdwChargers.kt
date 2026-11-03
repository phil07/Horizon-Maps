package com.headunit.launcher

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Public charge points in the Netherlands from the national register of NDW (Nationaal Dataportaal Wegverkeer, open data, CC0 unless a
 * dataset says otherwise). The file has no area query, so it is downloaded once a week, kept on the phone as a plain table, and searched
 * there. Only in use for routes that pass through the Netherlands. Never throws.
 */
object NdwChargers {
    private const val URL_GZ = "https://opendata.ndw.nu/charging_point_locations.geojson.gz"
    private const val WEEK_MS = 7L * 86_400_000L
    /** How close (m) a charge point must be to the route to count. */
    private const val NEAR_M = 150.0
    private const val FILE = "ndw_chargers.tsv"

    private class Cp(val lat: Double, val lon: Double, val kw: Int, val op: String, val addr: String)

    @Volatile private var loaded: List<Cp>? = null
    @Volatile private var loadedStamp = 0L

    /** Downloads the register when the stored copy is older than a week (or missing). Blocking: call off the main thread. */
    @Synchronized
    fun refresh(ctx: Context) {
        val f = File(ctx.filesDir, FILE)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < WEEK_MS) return
        val tmp = File(ctx.filesDir, "$FILE.part")
        val c = URL(URL_GZ).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 60_000
            c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
            if (c.responseCode !in 200..299) throw IllegalStateException("NDW HTTP ${c.responseCode}")
            GZIPInputStream(c.inputStream).use { input ->
                tmp.bufferedWriter(Charsets.UTF_8).use { w -> parse(input, w) }
            }
            if (tmp.length() == 0L) throw IllegalStateException("NDW: empty file")
            if (f.exists()) f.delete()
            tmp.renameTo(f)
            loaded = null
        } finally {
            c.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    /** The charge points near [line] (within [NEAR_M]); empty when none are stored. Refreshes the copy first when it is old. Never throws. */
    fun nearby(ctx: Context, line: List<LL>): List<Nearby> = try {
        if (line.size < 2) emptyList() else {
            try { refresh(ctx) } catch (e: Exception) { /* use the stored copy */ }
            along(ctx, line)
        }
    } catch (e: Exception) { emptyList() }

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
            out += Nearby(Place(c.op.ifBlank { "Ladepunkt" }, c.addr, pos), 0.0, "", kw = c.kw.takeIf { it > 0 }, op = c.op.takeIf { it.isNotBlank() })
        }
        return out
    }

    private fun stored(ctx: Context): List<Cp> {
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return emptyList()
        loaded?.takeIf { loadedStamp == f.lastModified() }?.let { return it }
        val list = ArrayList<Cp>()
        f.bufferedReader(Charsets.UTF_8).use { br ->
            for (line in br.lineSequence()) {
                val p = line.split('\t')
                if (p.size < 5) continue
                val lat = p[0].toDoubleOrNull() ?: continue
                val lon = p[1].toDoubleOrNull() ?: continue
                list += Cp(lat, lon, p[2].toIntOrNull() ?: 0, p[3], p.getOrElse(4) { "" })
            }
        }
        loaded = list; loadedStamp = f.lastModified()
        return list
    }

    /** Streams the GeoJSON (too big to hold as one text) and writes one line per charge point: lat, lon, kW, operator, address. */
    private fun parse(input: java.io.InputStream, w: BufferedWriter) {
        val r = JsonReader(InputStreamReader(input, Charsets.UTF_8))
        r.beginObject()
        while (r.hasNext()) {
            if (r.nextName() == "features") {
                r.beginArray()
                while (r.hasNext()) feature(r, w)
                r.endArray()
            } else r.skipValue()
        }
        r.endObject()
    }

    private fun feature(r: JsonReader, w: BufferedWriter) {
        var lat = Double.NaN; var lon = Double.NaN
        var op = ""; var addr = ""; var kw = 0
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "geometry" -> {
                    if (r.peek() == JsonToken.BEGIN_OBJECT) {
                        r.beginObject()
                        while (r.hasNext()) {
                            if (r.nextName() == "coordinates" && r.peek() == JsonToken.BEGIN_ARRAY) {
                                r.beginArray(); lon = r.nextDouble(); lat = r.nextDouble()
                                while (r.hasNext()) r.skipValue()
                                r.endArray()
                            } else r.skipValue()
                        }
                        r.endObject()
                    } else r.skipValue()
                }
                "properties" -> {
                    if (r.peek() == JsonToken.BEGIN_OBJECT) {
                        r.beginObject()
                        while (r.hasNext()) {
                            when (r.nextName()) {
                                "operator_name" -> op = text(r) ?: ""
                                "address" -> addr = text(r) ?: ""
                                "availabilities" -> if (r.peek() == JsonToken.BEGIN_ARRAY) {
                                    r.beginArray()
                                    while (r.hasNext()) {
                                        if (r.peek() != JsonToken.BEGIN_OBJECT) { r.skipValue(); continue }
                                        r.beginObject()
                                        while (r.hasNext()) {
                                            if (r.nextName() == "power_max" && r.peek() == JsonToken.NUMBER) {
                                                // watts, to kilowatts
                                                kw = maxOf(kw, Math.round(r.nextDouble() / 1000.0).toInt())
                                            } else r.skipValue()
                                        }
                                        r.endObject()
                                    }
                                    r.endArray()
                                } else r.skipValue()
                                else -> r.skipValue()
                            }
                        }
                        r.endObject()
                    } else r.skipValue()
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        if (lat.isNaN() || lon.isNaN()) return
        w.write("$lat\t$lon\t$kw\t${clean(op)}\t${clean(addr)}")
        w.newLine()
    }

    private fun text(r: JsonReader): String? =
        if (r.peek() == JsonToken.STRING) r.nextString() else { r.skipValue(); null }

    private fun clean(s: String) = s.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim()
}
