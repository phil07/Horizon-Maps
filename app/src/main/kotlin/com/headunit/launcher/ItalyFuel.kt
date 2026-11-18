package com.headunit.launcher

import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Italy's fuel prices from the government's own daily price list (MIMIT, "Osservaprezzi carburanti"), no key needed. Two files,
 * fields split by "|", the first two lines a note and the column names:
 *  - anagrafica_impianti_attivi.csv: idImpianto|Gestore|Bandiera|Tipo Impianto|Nome Impianto|Indirizzo|Comune|Provincia|Latitudine|Longitudine
 *  - prezzo_alle_8.csv: idImpianto|descCarburante|prezzo|isSelf|dtComu
 * They are fetched at most once a day and kept on the device (a failed fetch keeps the last ones, and is tried again after an hour).
 * Everything here is network or file work: call it off the main thread.
 */
object ItalyFuel {
    private const val STATIONS_URL = "https://www.mimit.gov.it/images/exportCSV/anagrafica_impianti_attivi.csv"
    private const val PRICES_URL = "https://www.mimit.gov.it/images/exportCSV/prezzo_alle_8.csv"
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val RETRY_MS = 60L * 60 * 1000

    /** Where the two files are kept (set by the app at start). */
    @Volatile var dir: File? = null

    private class Site(val name: String, val sub: String, val pos: LL)
    private var sites: Map<String, Site> = emptyMap()
    /** Per station, per fuel ("E5", "B7", "LPG"): the lowest price in euros (self-service where there is one). */
    private var prices: Map<String, Map<String, Double>> = emptyMap()
    private var loadedFrom = 0L
    private var lastTry = 0L

    fun hasFuel(fuel: String): Boolean = fuel in setOf("E10", "E5", "B7", "LPG")

    /** Stations with a price for [fuel] within [km] of [c], as [EuFuel.Station] (cents a litre). */
    @Synchronized
    fun near(c: LL, km: Int, fuel: String): List<EuFuel.Station> {
        ensure()
        val code = when (fuel) { "B7" -> "B7"; "LPG" -> "LPG"; else -> "E5" }   // Italy's "benzina" is sold as E5 (no separate E10)
        val r = km * 1000.0
        val dLat = r / 111_000.0
        val dLon = r / (111_000.0 * Math.cos(Math.toRadians(c.lat)).coerceAtLeast(0.2))
        val out = ArrayList<EuFuel.Station>()
        for ((id, s) in sites) {
            if (Math.abs(s.pos.lat - c.lat) > dLat || Math.abs(s.pos.lon - c.lon) > dLon) continue
            if (Geo.dist(c, s.pos) > r) continue
            val euros = prices[id]?.get(code) ?: continue
            out += EuFuel.Station(s.name, s.sub, s.pos, euros * 100.0)
        }
        return out.sortedBy { Geo.dist(c, it.pos) }.take(60)
    }

    /** Fetches the files when they are a day old (or missing), and reads them into memory when they changed. */
    private fun ensure() {
        val d = dir ?: return
        val fs = File(d, "mimit_stations.csv"); val fp = File(d, "mimit_prices.csv")
        val now = System.currentTimeMillis()
        val stale = !fs.exists() || !fp.exists() || now - minOf(fs.lastModified(), fp.lastModified()) > DAY_MS
        if (stale && now - lastTry > RETRY_MS) {
            lastTry = now
            try {
                val ts = File(d, "mimit_stations.tmp"); val tp = File(d, "mimit_prices.tmp")
                download(STATIONS_URL, ts); download(PRICES_URL, tp)
                // only a complete pair replaces the last one
                if (ts.length() > 1000 && tp.length() > 1000) { ts.renameTo(fs); tp.renameTo(fp) } else { ts.delete(); tp.delete() }
            } catch (e: Exception) { android.util.Log.w("ItalyFuel", "fetch: ${e.message}") }
        }
        if (!fs.exists() || !fp.exists()) return
        val stamp = maxOf(fs.lastModified(), fp.lastModified())
        if (stamp == loadedFrom && sites.isNotEmpty()) return
        try { read(fs, fp); loadedFrom = stamp } catch (e: Exception) { android.util.Log.w("ItalyFuel", "read: ${e.message}") }
    }

    private fun download(url: String, to: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 30_000
        c.setRequestProperty("User-Agent", "HorizonNav/${BuildConfig.VERSION_NAME}")
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            c.inputStream.use { inp -> to.outputStream().use { out -> inp.copyTo(out) } }
        } finally { c.disconnect() }
    }

    private fun read(fs: File, fp: File) {
        val st = HashMap<String, Site>(30_000)
        fs.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(2).forEach { line ->
                val p = line.split('|')
                if (p.size < 10) return@forEach
                val lat = p[8].trim().replace(',', '.').toDoubleOrNull() ?: return@forEach
                val lon = p[9].trim().replace(',', '.').toDoubleOrNull() ?: return@forEach
                if (lat < 35.0 || lat > 48.0 || lon < 6.0 || lon > 19.0) return@forEach
                val brand = p[2].trim(); val name = p[4].trim(); val town = p[6].trim()
                val title = listOf(brand, name).filter { it.isNotBlank() }.distinct().joinToString(" · ").ifBlank { "Distributore" }
                st[p[0].trim()] = Site(title, listOf(p[5].trim(), town).filter { it.isNotBlank() }.joinToString(", "), LL(lat, lon))
            }
        }
        // the lowest price per fuel; self-service prices win over attended ones at the same station
        val best = HashMap<String, HashMap<String, Pair<Double, Boolean>>>(st.size)
        fp.bufferedReader(Charsets.UTF_8).useLines { lines ->
            lines.drop(2).forEach { line ->
                val p = line.split('|')
                if (p.size < 4) return@forEach
                val id = p[0].trim()
                if (id !in st) return@forEach
                val code = fuelCode(p[1]) ?: return@forEach
                val euros = p[2].trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.3 && it < 5.0 } ?: return@forEach
                val self = p[3].trim() == "1"
                val m = best.getOrPut(id) { HashMap() }
                val cur = m[code]
                if (cur == null || (self && !cur.second) || (self == cur.second && euros < cur.first)) m[code] = euros to self
            }
        }
        sites = st
        prices = best.mapValues { (_, m) -> m.mapValues { it.value.first } }
    }

    /** MIMIT's fuel names to the app's: petrol "E5", diesel "B7", LPG; the premium ones and the rest are left out. */
    private fun fuelCode(raw: String): String? {
        val t = raw.trim().lowercase()
        return when {
            t == "benzina" -> "E5"
            t == "gasolio" || t == "gasolio alpino" -> "B7"
            t == "gpl" -> "LPG"
            else -> null
        }
    }
}
