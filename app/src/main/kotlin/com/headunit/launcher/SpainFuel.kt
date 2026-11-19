package com.headunit.launcher

import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Spain's live petrol-station prices from the Ministry for the Ecological Transition (the open Geoportal de Carburantes), no key
 * needed. One JSON list of every station with its prices; the prices are written with a comma ("1,459"). The list is kept on the
 * device and fetched at most every four hours (a failed fetch keeps the last list, and is tried again after half an hour), so
 * prices still show while the service is down. Everything here is network or file work: call it off the main thread.
 */
object SpainFuel {
    private const val LIST_URL = "https://sedeaplicaciones.minetur.gob.es/ServiciosRESTCarburantes/PreciosCarburantes/EstacionesTerrestres/"
    private const val MAX_AGE_MS = 4L * 60 * 60 * 1000
    private const val RETRY_MS = 30L * 60 * 1000

    /** Where the list is kept (set by the app at start). */
    @Volatile var dir: File? = null

    private class Site(val name: String, val sub: String, val pos: LL, val prices: Map<String, Double>)
    private var sites: List<Site> = emptyList()
    private var loadedStamp = 0L
    private var lastTry = 0L

    fun hasFuel(fuel: String): Boolean = fuel in setOf("E10", "E5", "B7", "LPG")

    /** Stations with a price for [fuel] within [km] of [c], as [EuFuel.Station] (cents a litre). */
    @Synchronized
    fun near(c: LL, km: Int, fuel: String): List<EuFuel.Station> {
        ensure()
        if (!hasFuel(fuel)) return emptyList()
        val r = km * 1000.0
        val dLat = r / 111_000.0
        val dLon = r / (111_000.0 * Math.cos(Math.toRadians(c.lat)).coerceAtLeast(0.2))
        val out = ArrayList<EuFuel.Station>()
        for (s in sites) {
            if (Math.abs(s.pos.lat - c.lat) > dLat || Math.abs(s.pos.lon - c.lon) > dLon) continue
            if (Geo.dist(c, s.pos) > r) continue
            val euros = s.prices[fuel] ?: continue
            out += EuFuel.Station(s.name, s.sub, s.pos, euros * 100.0)
        }
        return out.sortedBy { Geo.dist(c, it.pos) }.take(60)
    }

    /** Fetches the list when it is four hours old (or missing), and reads it into memory when it changed. */
    private fun ensure() {
        val d = dir ?: return
        val f = File(d, "spain_fuel.json")
        val now = System.currentTimeMillis()
        if ((!f.exists() || now - f.lastModified() > MAX_AGE_MS) && now - lastTry > RETRY_MS) {
            lastTry = now
            try {
                val t = File(d, "spain_fuel.tmp")
                download(LIST_URL, t)
                // only a complete list replaces the last one
                if (t.length() > 100_000) t.renameTo(f) else t.delete()
            } catch (e: Exception) { android.util.Log.w("SpainFuel", "fetch: ${e.message}") }
        }
        if (!f.exists()) return
        val stamp = f.lastModified()
        if (stamp == loadedStamp && sites.isNotEmpty()) return
        try { read(f); loadedStamp = stamp } catch (e: Exception) { android.util.Log.w("SpainFuel", "read: ${e.message}") }
    }

    private fun download(url: String, to: File) {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10_000; c.readTimeout = 60_000
        c.setRequestProperty("User-Agent", "HorizonNav/${BuildConfig.VERSION_NAME}")
        try {
            if (c.responseCode !in 200..299) throw java.io.IOException("HTTP ${c.responseCode}")
            c.inputStream.use { inp -> to.outputStream().use { out -> inp.copyTo(out) } }
        } finally { c.disconnect() }
    }

    private fun price(o: JSONObject, key: String): Double? =
        o.optString(key).trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.3 && it < 5.0 }

    private fun read(f: File) {
        val list = JSONObject(f.readText(Charsets.UTF_8)).optJSONArray("ListaEESSPrecio") ?: return
        val out = ArrayList<Site>(list.length())
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val lat = o.optString("Latitud").trim().replace(',', '.').toDoubleOrNull() ?: continue
            val lon = o.optString("Longitud (WGS84)").trim().replace(',', '.').toDoubleOrNull() ?: continue
            val prices = HashMap<String, Double>(4)
            price(o, "Precio Gasolina 95 E5")?.let { prices["E5"] = it }
            price(o, "Precio Gasolina 95 E10")?.let { prices["E10"] = it }
            price(o, "Precio Gasoleo A")?.let { prices["B7"] = it }
            price(o, "Precio Gases licuados del petróleo")?.let { prices["LPG"] = it }
            if (prices.isEmpty()) continue
            val name = o.optString("Rótulo").trim().ifBlank { "Gasolinera" }
            val town = o.optString("Localidad").trim().ifBlank { o.optString("Municipio").trim() }
            val sub = listOf(o.optString("Dirección").trim(), town).filter { it.isNotBlank() }.joinToString(", ")
            out += Site(name, sub, LL(lat, lon), prices)
        }
        sites = out
    }
}
