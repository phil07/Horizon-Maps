package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Charger power and operator from Germany's Ladesäulenregister (Bundesnetzagentur, CC BY 4.0, credit "Bundesnetzagentur.de",
 * no key), for chargers TomTom and OpenStreetMap know but without a power or operator, and as an extra source of chargers along
 * the route. The data needs crediting where its chargers are shown.
 * Blocking: call off the main thread.
 */
object ChargerInfo {
    private const val BNETZA = "https://services-eu1.arcgis.com/TJm8oSvOdJUQvQT5/arcgis/rest/services/Ladesaeulen/FeatureServer/0/query"
    private const val UA = "HorizonNav/1.0"
    /** How close a register entry must be to count as the same charger (the register's positions are often a car park's, not the pillar's). */
    private const val SAME_M = 150.0

    /** What the last lookup did, for the settings page. */
    @Volatile var lastNote = ""

    // re-planning the same route (a reroute check, a settings change) must not ask the registers again: the last few results are kept
    private val cache = object : LinkedHashMap<Int, List<Nearby>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<Nearby>>?) = size > 4
    }

    /** Chargers near [line] from the German register (when the line passes through Germany). Never throws. */
    fun along(prefs: Prefs, line: List<LL>): List<Nearby> {
        if (line.size < 2 || line.none { it.lat in 47.2..55.1 && it.lon in 5.8..15.1 }) return emptyList()
        // "all chargers" in Settings: the register's normal (AC) chargers too, so theirs get a power as well
        val fastOnly = prefs.i(PreferenceKeys.EV_MIN_KW, 50) > 0
        val h = line.hashCode() * 31 + fastOnly.hashCode()
        synchronized(cache) { cache[h] }?.let { return it }
        return try {
            val out = bnetza(line, fastOnly)
            lastNote = "BNetzA ${out.size}"
            synchronized(cache) { cache[h] = out }
            out
        } catch (e: Exception) {
            // not cached: asked again next time (it may only have been a dead spot)
            lastNote = "BNetzA: " + (e.message ?: e.javaClass.simpleName)
            emptyList()
        }
    }

    /**
     * [list] with missing power and operators filled in from the nearest of [extra] within 150 m.
     * Known values are kept: TomTom's and OpenStreetMap's are usually per pillar, the register's per site.
     */
    fun enrich(list: List<Nearby>, extra: List<Nearby>): List<Nearby> {
        if (extra.isEmpty()) return list
        return list.map { n ->
            if (n.kw != null && n.op != null) return@map n
            var best: Nearby? = null; var bd = SAME_M
            for (e in extra) {
                if (e.kw == null && e.op == null) continue
                // quick box test first: most of a long route's register is far away
                if (Math.abs(e.place.pos.lat - n.place.pos.lat) > 0.002 || Math.abs(e.place.pos.lon - n.place.pos.lon) > 0.004) continue
                val d = Geo.dist(e.place.pos, n.place.pos)
                if (d < bd) { bd = d; best = e }
            }
            val b = best ?: return@map n
            n.copy(kw = n.kw ?: b.kw, op = n.op ?: b.op)
        }
    }

    /** True when [p] is within 150 m of one of [list]. */
    fun near(p: LL, list: List<Nearby>): Boolean = list.any {
        Math.abs(it.place.pos.lat - p.lat) < 0.002 && Math.abs(it.place.pos.lon - p.lon) < 0.004 && Geo.dist(it.place.pos, p) < SAME_M
    }

    // ---- Bundesnetzagentur Ladesäulenregister (ArcGIS feature service) ----

    private fun bnetza(line: List<LL>, fastOnly: Boolean): List<Nearby> {
        val path = JSONArray()
        for (p in line) path.put(JSONArray().put(Math.round(p.lon * 1e5) / 1e5).put(Math.round(p.lat * 1e5) / 1e5))
        val geom = JSONObject().put("paths", JSONArray().put(path)).put("spatialReference", JSONObject().put("wkid", 4326)).toString()
        // the register lists every station of a site separately (same position): one entry per site, with its fastest station
        val sites = LinkedHashMap<Pair<Long, Long>, Nearby>()
        var offset = 0
        for (page in 0 until 4) {
            val form = linkedMapOf(
                "f" to "json", "geometry" to geom, "geometryType" to "esriGeometryPolyline", "inSR" to "4326", "outSR" to "4326",
                "spatialRel" to "esriSpatialRelIntersects", "distance" to "2500", "units" to "esriSRUnit_Meter",
                "where" to if (fastOnly) "Typ='Schnellladeeinrichtung' AND Status='In Betrieb'" else "Status='In Betrieb'",
                "outFields" to "Betreiber,operator_companyName,max_electric_power_station,Straße,Ort,Anzahl_Ladepunkte",
                "returnGeometry" to "true", "resultOffset" to offset.toString(), "resultRecordCount" to "2000",
            )
            val j = JSONObject(postForm(BNETZA, form))
            // ArcGIS answers errors with HTTP 200 and an "error" object
            j.optJSONObject("error")?.let { throw IOException(it.optString("message").ifBlank { "error" }) }
            val feats = j.optJSONArray("features") ?: JSONArray()
            for (i in 0 until feats.length()) {
                val f = feats.optJSONObject(i) ?: continue
                val g = f.optJSONObject("geometry") ?: continue
                val a = f.optJSONObject("attributes") ?: JSONObject()
                val lon = g.optDouble("x"); val lat = g.optDouble("y")
                if (lat.isNaN() || lon.isNaN()) continue
                val pos = LL(lat, lon)
                val kw = str(a, "max_electric_power_station")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 && it <= 1500 }?.let { Math.round(it).toInt() }
                val op = str(a, "Betreiber") ?: str(a, "operator_companyName")
                val street = listOfNotNull(str(a, "Straße"), str(a, "Ort")).joinToString(", ")
                val k = Math.round(lat * 1e5) to Math.round(lon * 1e5)
                val prev = sites[k]
                if (prev == null || (kw ?: 0) > (prev.kw ?: 0)) sites[k] = Nearby(Place(siteName(op ?: prev?.op, str(a, "Ort"), pos), street, pos), 0.0, "", kw = kw ?: prev?.kw, op = op ?: prev?.op)
                // a slower station of the site may be the only one that names the operator
                else if (prev.op == null && op != null) sites[k] = prev.copy(place = prev.place.copy(name = siteName(op, str(a, "Ort"), pos)), op = op)
            }
            if (!j.optBoolean("exceededTransferLimit", false) || feats.length() == 0) break
            offset += feats.length()
        }
        return sites.values.toList()
    }

    /**
     * A name like TomTom's ("EnBW Neusäß"): the network's short name (no "mobility+ AG und Co.KG") and the town, as the stop list
     * shows only the name. Its full operator stays in [Nearby.op] for matching preferred operators.
     */
    private fun siteName(op: String?, town: String?, pos: LL): String {
        val brand = if (op == null) null else ChargeOperators.label(Nearby(Place("", "", pos), 0.0, "", op = op))
        val b = brand ?: Lang.t("Ladesäule", "Charger")
        // ("Stadtwerke Ulm" in Ulm: the town once)
        return if (town == null || b.contains(town, ignoreCase = true)) b else "$b $town"
    }

    // ---- HTTP ----

    /** A non-blank string value, or null (org.json on Android turns a JSON null into the text "null"). */
    private fun str(o: JSONObject, k: String): String? = if (o.isNull(k)) null else o.optString(k).trim().takeIf { it.isNotEmpty() && it != "null" }

    /** Form-encoded POST: a whole route's line is too long for a URL. */
    private fun postForm(url: String, form: Map<String, String>, connectMs: Int = 10000, readMs: Int = 25000): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs
        c.readTimeout = readMs
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("User-Agent", UA)
        c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val body = form.entries.joinToString("&") { URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8") }
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code ${text.take(100)}")
        return text
    }
}
