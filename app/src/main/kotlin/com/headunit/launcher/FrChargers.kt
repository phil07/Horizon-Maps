package com.headunit.launcher

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Charge points in France from the national base of transport.data.gouv.fr (Licence Ouverte 2.0). Only used when the owner has switched
 * it on in Settings. The full static file (about 127 MB) is downloaded at most once a month and kept on the phone as a compact table,
 * one line per charge point. A station is one marker; its power is that of its strongest charge point, as a car charges at one point
 * at a time. Never throws.
 */
object FrChargers {
    private const val URL_STATIC = "https://transport.data.gouv.fr/resources/84013/download"
    private const val MONTH_MS = 30L * 86_400_000L
    private const val FILE = "fr_chargers_v1.tsv"

    /** One charge point: its id (also in the live file), its station, position, power in kW (0 unknown), operator and address. */
    class Pdc(val id: String, val station: String, val lat: Double, val lon: Double, val kw: Int, val op: String, val addr: String)

    private class Data(val pdcs: List<Pdc>, val byStation: Map<String, List<String>>)

    @Volatile private var data: Data? = null

    /** Whether the map box [box] (minLat, minLon, maxLat, maxLon) reaches into France (rough). */
    fun inFrance(box: DoubleArray): Boolean = box[0] < 51.2 && box[2] > 41.3 && box[1] < 9.6 && box[3] > -5.2

    /** Downloads the static file when the stored copy is older than a month (or missing). Blocking: call off the main thread. */
    @Synchronized
    fun refresh(ctx: Context) {
        val f = File(ctx.filesDir, FILE)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < MONTH_MS) return
        val tmp = File(ctx.filesDir, "$FILE.part")
        val c = URL(URL_STATIC).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 15_000; c.readTimeout = 60_000
            c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
            if (c.responseCode !in 200..299) throw IllegalStateException("FR HTTP ${c.responseCode}")
            c.inputStream.use { input -> tmp.bufferedWriter(Charsets.UTF_8).use { w -> convert(input, w) } }
            if (tmp.length() == 0L) throw IllegalStateException("FR: empty file")
            if (f.exists()) f.delete()
            tmp.renameTo(f)
            data = null
        } finally {
            c.disconnect()
            if (tmp.exists()) tmp.delete()
        }
    }

    /** The stations inside [box] for the map (one marker each), from the stored copy. Empty when none is stored. Never throws. */
    fun inBox(ctx: Context, box: DoubleArray): List<ChargerSite> = try {
        val d = load(ctx)
        if (d == null) emptyList() else {
            val groups = LinkedHashMap<String, MutableList<Pdc>>()
            for (p in d.pdcs) {
                if (p.lat < box[0] || p.lat > box[2] || p.lon < box[1] || p.lon > box[3]) continue
                groups.getOrPut(p.station) { ArrayList() }.add(p)
            }
            groups.map { (st, ps) ->
                val first = ps[0]
                ChargerSite(LL(first.lat, first.lon), ps.maxOf { it.kw }.takeIf { it > 0 }, ps.size, null,
                    first.op.takeIf { it.isNotBlank() }, null, "FR:$st")
            }
        }
    } catch (e: Exception) { emptyList() }

    /** The charge point ids of a station (from the last loaded copy). */
    fun pdcIds(station: String): List<String> = data?.byStation?.get(station).orEmpty()

    @Synchronized
    private fun load(ctx: Context): Data? {
        data?.let { return it }
        val f = File(ctx.filesDir, FILE)
        if (!f.exists()) return null
        val pdcs = ArrayList<Pdc>()
        val by = HashMap<String, MutableList<String>>()
        f.forEachLine(Charsets.UTF_8) { line ->
            val t = line.split('\t')
            if (t.size >= 7) {
                val lat = t[2].toDoubleOrNull(); val lon = t[3].toDoubleOrNull()
                if (lat != null && lon != null) {
                    pdcs += Pdc(t[0], t[1], lat, lon, t[4].toIntOrNull() ?: 0, t[5], t[6])
                    by.getOrPut(t[1]) { ArrayList() }.add(t[0])
                }
            }
        }
        val d = Data(pdcs, by)
        data = d
        return d
    }

    /** Writes one line per charge point: id, station, lat, lon, kW, operator, address. Tabs and line breaks inside values become spaces. */
    private fun convert(input: InputStream, w: BufferedWriter) {
        var idx: Map<String, Int>? = null
        csvRecords(input.bufferedReader(Charsets.UTF_8)) { f ->
            val h = idx
            if (h == null) {
                idx = f.withIndex().associate { it.value.trim() to it.index }
                return@csvRecords
            }
            fun g(name: String): String = h[name]?.let { f.getOrNull(it) }.orEmpty().trim().replace(Regex("[\\t\\r\\n]"), " ")
            val pdc = g("id_pdc_itinerance")
            val lat = g("consolidated_latitude").toDoubleOrNull()
            val lon = g("consolidated_longitude").toDoubleOrNull()
            if (pdc.isBlank() || lat == null || lon == null || lat !in 41.0..52.0 || lon !in -6.0..10.0) return@csvRecords
            val station = g("id_station_itinerance").ifBlank { pdc }
            val kw = g("puissance_nominale").toDoubleOrNull()?.toInt() ?: 0
            val op = g("nom_operateur").ifBlank { g("nom_amenageur") }
            val addr = g("adresse_station")
            w.write("$pdc\t$station\t$lat\t$lon\t$kw\t$op\t$addr")
            w.newLine()
        }
    }
}

/** Reads CSV records (RFC 4180: quoted fields, "" inside quotes, line breaks inside quotes). Each record's fields go to [onRecord]. */
internal fun csvRecords(r: Reader, onRecord: (List<String>) -> Unit) {
    val fields = ArrayList<String>()
    val sb = StringBuilder()
    var inQ = false
    var pendingQ = false
    var any = false
    while (true) {
        val c = r.read()
        if (c == -1) break
        val ch = c.toChar()
        if (pendingQ) {
            pendingQ = false
            if (ch == '"') { sb.append('"'); continue }
            inQ = false
        }
        if (inQ) {
            if (ch == '"') pendingQ = true else sb.append(ch)
            continue
        }
        when (ch) {
            '"' -> { inQ = true; any = true }
            ',' -> { fields += sb.toString(); sb.setLength(0); any = true }
            '\n' -> {
                fields += sb.toString(); sb.setLength(0)
                if (any || fields.any { it.isNotEmpty() }) onRecord(ArrayList(fields))
                fields.clear(); any = false
            }
            '\r' -> {}
            else -> { sb.append(ch); any = true }
        }
    }
    if (any || sb.isNotEmpty() || fields.isNotEmpty()) {
        fields += sb.toString()
        onRecord(ArrayList(fields))
    }
}

/**
 * Free charge points in France from the live file of the same base (no key). It has no positions: each point's state is matched by its
 * id to the stored copy. Asked at most every three minutes and only while France is on the map. Never throws.
 */
object FrLive {
    private const val URL_DYN = "https://transport.data.gouv.fr/resources/84098/download"
    private const val TTL_MS = 3 * 60_000L

    @Volatile private var cache: Map<String, Int> = emptyMap()
    @Volatile private var at = 0L

    /** Charge point id -> 1 when free, 0 when occupied or out of service. Points the file doesn't know are absent. */
    @Synchronized
    fun statuses(): Map<String, Int> {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && now - at < TTL_MS) return cache
        try {
            val c = URL(URL_DYN).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 10_000; c.readTimeout = 30_000
                c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
                if (c.responseCode in 200..299) {
                    val out = HashMap<String, Int>()
                    c.inputStream.use { input -> parse(input, out) }
                    if (out.isNotEmpty()) { cache = out; at = now }
                }
            } finally { c.disconnect() }
        } catch (e: Exception) { }
        return cache
    }

    private fun parse(input: InputStream, out: MutableMap<String, Int>) {
        var idx: Map<String, Int>? = null
        csvRecords(input.bufferedReader(Charsets.UTF_8)) { f ->
            val h = idx
            if (h == null) {
                idx = f.withIndex().associate { it.value.trim() to it.index }
                return@csvRecords
            }
            fun g(name: String): String = h[name]?.let { f.getOrNull(it) }.orEmpty().trim()
            val id = g("id_pdc_itinerance")
            if (id.isEmpty()) return@csvRecords
            val etat = g("etat_pdc")
            val occ = g("occupation_pdc")
            if (etat == "en_service" && occ == "libre") {
                out[id] = 1
            } else if (etat == "hors_service" || occ == "occupe") {
                out[id] = 0
            }
        }
    }

    /** Sets the free and total counts of the France stations from the live file. Other sites pass unchanged. */
    fun apply(sites: List<ChargerSite>, st: Map<String, Int>): List<ChargerSite> {
        if (st.isEmpty()) return sites
        return sites.map { s ->
            val id = s.id ?: return@map s
            if (!id.startsWith("FR:")) return@map s
            val ids = FrChargers.pdcIds(id.removePrefix("FR:"))
            if (ids.isEmpty()) return@map s
            var free = 0; var known = 0
            for (p in ids) { val v = st[p] ?: continue; known++; free += v }
            ChargerSite(s.pos, s.kw, s.total, if (known > 0) free else null, s.op, s.kwLabel, s.id)
        }
    }
}
