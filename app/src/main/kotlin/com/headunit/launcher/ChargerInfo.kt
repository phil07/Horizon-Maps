package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Charger power and operator from two open registers, for chargers TomTom and OpenStreetMap know but without a power or operator
 * (and as an extra source of chargers along the route):
 *  - Germany's Ladesäulenregister (Bundesnetzagentur, CC BY 4.0, credit "Bundesnetzagentur.de", no key): every public fast charger with its operator and power.
 *  - Open Charge Map (worldwide, mostly CC BY-SA 4.0, each record credits its data provider) with the user's own free key.
 * Both need crediting where their chargers are shown.
 * Blocking: call off the main thread.
 */
object ChargerInfo {
    private const val BNETZA = "https://services-eu1.arcgis.com/TJm8oSvOdJUQvQT5/arcgis/rest/services/Ladesaeulen/FeatureServer/0/query"
    private const val OCM = "https://api.openchargemap.io/v3"
    private const val UA = "HorizonNav/1.0"
    /** How close a register entry must be to count as the same charger (the register's positions are often a car park's, not the pillar's). */
    private const val SAME_M = 150.0

    /** What the last lookup did, for the settings page. */
    @Volatile var lastNote = ""

    // re-planning the same route (a reroute check, a settings change) must not ask the registers again: the last few results are kept
    private val cache = object : LinkedHashMap<Int, List<Nearby>>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<Nearby>>?) = size > 4
    }

    /** Open Charge Map's operator names by id (its compact answers give only the id); fetched once per session. */
    @Volatile private var ocmOperators: Map<Int, String>? = null

    /** Chargers near [line] from the German register (when the line passes through Germany) and Open Charge Map (when a key is set). Never throws. */
    fun along(prefs: Prefs, line: List<LL>): List<Nearby> {
        if (line.size < 2) return emptyList()
        val key = prefs.s(PreferenceKeys.OCM_KEY, "").trim()
        // "all chargers" in Settings: the register's normal (AC) chargers too, so theirs get a power as well
        val fastOnly = prefs.i(PreferenceKeys.EV_MIN_KW, 50) > 0
        val h = (line.hashCode() * 31 + key.hashCode()) * 31 + fastOnly.hashCode()
        synchronized(cache) { cache[h] }?.let { return it }
        val out = ArrayList<Nearby>()
        val notes = ArrayList<String>()
        var failed = false
        if (line.any { it.lat in 47.2..55.1 && it.lon in 5.8..15.1 }) {
            try { out += bnetza(line, fastOnly); notes += "BNetzA ${out.size}" }
            catch (e: Exception) { failed = true; notes += "BNetzA: " + (e.message ?: e.javaClass.simpleName) }
        }
        if (key.isNotEmpty()) {
            try {
                // in Germany Open Charge Map is a second opinion: only its chargers the register doesn't already have
                val o = ocm(key, line).filter { n -> out.none { Geo.dist(it.place.pos, n.place.pos) < SAME_M } }
                out += o; notes += "OCM ${o.size}"
            } catch (e: Exception) { failed = true; notes += "OCM: " + (e.message ?: e.javaClass.simpleName) }
        }
        lastNote = notes.joinToString("; ")
        // a failed source is asked again next time (it may only have been a dead spot)
        if (!failed) synchronized(cache) { cache[h] = out }
        return out
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

    /**
     * Germany's register inside the map view [box] (minLat, minLon, maxLat, maxLon): one request per view. The register gives each
     * site's number of chargers but not how many are free. Empty outside Germany. Throws when the register can't be reached.
     */
    fun inBox(box: DoubleArray, ocmKey: String = ""): List<ChargerSite> {
        val reg = if (box[0] > 55.1 || box[2] < 47.2 || box[1] > 15.1 || box[3] < 5.8) emptyList() else registerInBox(box)
        val key = ocmKey.trim()
        if (key.isEmpty()) return reg
        // Open Charge Map adds only the places the register doesn't have (its own copies are the same spots)
        val out = ArrayList(reg)
        try { out += ocmInBox(key, box).filter { s -> reg.none { Geo.dist(it.pos, s.pos) < SAME_M } } } catch (e: Exception) { }
        return out
    }

    /** Open Charge Map's stations in [box], with their charge points (NumberOfPoints, else the connectors) and strongest power. */
    private fun ocmInBox(key: String, box: DoubleArray): List<ChargerSite> {
        val lat = (box[0] + box[2]) / 2; val lon = (box[1] + box[3]) / 2
        val radiusKm = Math.min(100.0, Geo.dist(LL(lat, lon), LL(box[0], box[1])) / 1000.0 + 0.5)
        val url = "$OCM/poi?output=json&key=${URLEncoder.encode(key, "UTF-8")}&latitude=$lat&longitude=$lon&distance=${"%.2f".format(java.util.Locale.US, radiusKm)}" +
            "&distanceunit=KM&maxresults=500&compact=true&verbose=false"
        val arr = JSONArray(get(url))
        val out = ArrayList<ChargerSite>()
        for (i in 0 until arr.length()) {
            val x = arr.optJSONObject(i) ?: continue
            if (x.optInt("StatusTypeID", 0) >= 100) continue
            val ad = x.optJSONObject("AddressInfo") ?: continue
            val pl = ad.optDouble("Latitude"); val po = ad.optDouble("Longitude")
            if (pl.isNaN() || po.isNaN() || pl < box[0] || pl > box[2] || po < box[1] || po > box[3]) continue
            val cons = x.optJSONArray("Connections")
            var kw = 0.0
            if (cons != null) for (c in 0 until cons.length()) {
                val p = cons.optJSONObject(c)?.optDouble("PowerKW", 0.0) ?: 0.0
                if (!p.isNaN() && p <= 1500 && p > kw) kw = p
            }
            val kwI = if (kw > 0) Math.round(kw).toInt() else null
            val count = x.optInt("NumberOfPoints", 0).takeIf { it > 0 } ?: cons?.length()?.takeIf { it > 0 }
            out += ChargerSite(LL(pl, po), kwI, count, null, null, kwI?.let { "$it kW" }, "ocm-" + x.optInt("ID", 0))
        }
        return out
    }

    private fun registerInBox(box: DoubleArray): List<ChargerSite> {
        val form = linkedMapOf(
            "f" to "json", "geometry" to "${box[1]},${box[0]},${box[3]},${box[2]}", "geometryType" to "esriGeometryEnvelope",
            "inSR" to "4326", "outSR" to "4326", "spatialRel" to "esriSpatialRelIntersects", "where" to "Status='In Betrieb'",
            "outFields" to "ID,Betreiber,operator_companyName,max_electric_power_station,Anzahl_Ladepunkte,F_response_body",
            "returnGeometry" to "true", "resultRecordCount" to "300",
        )
        val j = JSONObject(postForm(BNETZA, form))
        j.optJSONObject("error")?.let { throw IOException(it.optString("message").ifBlank { "error" }) }
        val feats = j.optJSONArray("features") ?: JSONArray()
        val out = ArrayList<ChargerSite>()
        for (i in 0 until feats.length()) {
            val f = feats.optJSONObject(i) ?: continue
            val g = f.optJSONObject("geometry") ?: continue
            val a = f.optJSONObject("attributes") ?: JSONObject()
            val lon = g.optDouble("x"); val lat = g.optDouble("y")
            if (lat.isNaN() || lon.isNaN()) continue
            val kw = str(a, "max_electric_power_station")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 && it <= 1500 }?.let { Math.round(it).toInt() }
            val count = str(a, "Anzahl_Ladepunkte")?.toIntOrNull()?.takeIf { it > 0 }
            val body = str(a, "F_response_body")
            // a car charges at one connector at a time: its power is the strongest connector, at most the station's total
            out += ChargerSite(LL(lat, lon), carKw(kw, body), count, null, str(a, "Betreiber") ?: str(a, "operator_companyName"), powerLabel(kw, body), str(a, "ID"))
        }
        return out
    }

    /** The power a car gets here: the strongest single connector, never more than the station's total. Null when unknown. */
    internal fun carKw(stationKw: Int?, body: String?): Int? {
        val best = connectorKws(body).maxOrNull() ?: return stationKw
        return if (stationKw != null) minOf(best, stationKw) else best
    }

    /** The power of each connector in the register's detail record (empty when there is none). */
    private fun connectorKws(body: String?): List<Int> {
        val out = ArrayList<Int>()
        try {
            val evses = JSONObject(body ?: return out).optJSONArray("evses") ?: return out
            for (i in 0 until evses.length()) {
                val cs = evses.optJSONObject(i)?.optJSONArray("connectors") ?: continue
                for (k in 0 until cs.length()) {
                    val kw = cs.optJSONObject(k)?.optDouble("max_electric_power_connector", 0.0) ?: 0.0
                    if (kw > 0) out += Math.round(kw).toInt()
                }
            }
        } catch (e: Exception) { }
        return out
    }

    /**
     * The power as the map shows it, per connector: "2 × 22 kW" when two equal connectors add up to the station's power, otherwise the
     * station's power (cables that share one power unit, e.g. "150 kW" for two cables). Null when neither is known.
     */
    internal fun powerLabel(stationKw: Int?, body: String?): String? {
        val conns = ArrayList(connectorKws(body))
        if (conns.isEmpty()) return stationKw?.let { "$it kW" }
        if (stationKw != null && conns.sum() != stationKw) return "$stationKw kW"
        val distinct = conns.distinct()
        return if (distinct.size == 1) (if (conns.size == 1) "${conns[0]} kW" else "${conns.size} × ${distinct[0]} kW")
            else conns.joinToString(" / ") + " kW"
    }

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
                "outFields" to "Betreiber,operator_companyName,max_electric_power_station,Straße,Ort,Anzahl_Ladepunkte,F_response_body",
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
                val stationKw = str(a, "max_electric_power_station")?.replace(',', '.')?.toDoubleOrNull()?.takeIf { it > 0 && it <= 1500 }?.let { Math.round(it).toInt() }
                // the car's power: the strongest connector, at most the station's total (see carKw)
                val kw = carKw(stationKw, str(a, "F_response_body"))
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

    // ---- Open Charge Map ----

    private fun ocm(key: String, line: List<LL>): List<Nearby> {
        val k = URLEncoder.encode(key, "UTF-8")
        val poly = URLEncoder.encode(encodePolyline(line), "UTF-8")
        // compact: operators etc. come as ids only (much smaller); the names are looked up once from the reference data
        val url = "$OCM/poi?output=json&key=$k&polyline=$poly&distance=2&distanceunit=KM&maxresults=500&compact=true&verbose=false"
        val arr = JSONArray(get(url))
        val raw = ArrayList<Triple<LL, Int?, Pair<Int, String>>>()
        for (i in 0 until arr.length()) {
            val x = arr.optJSONObject(i) ?: continue
            // 100+ = not operational, planned or removed
            if (x.optInt("StatusTypeID", 0) >= 100) continue
            val ad = x.optJSONObject("AddressInfo") ?: continue
            val lat = ad.optDouble("Latitude"); val lon = ad.optDouble("Longitude")
            if (lat.isNaN() || lon.isNaN()) continue
            var kw = 0.0
            val cons = x.optJSONArray("Connections")
            if (cons != null) for (c in 0 until cons.length()) {
                val p = cons.optJSONObject(c)?.optDouble("PowerKW", 0.0) ?: 0.0
                if (!p.isNaN() && p <= 1500 && p > kw) kw = p
            }
            val street = listOfNotNull(str(ad, "AddressLine1"), str(ad, "Town")).joinToString(", ").ifBlank { str(ad, "Title").orEmpty() }
            raw += Triple(LL(lat, lon), if (kw > 0) Math.round(kw).toInt() else null, x.optInt("OperatorID", 0) to street)
        }
        val ops = if (raw.any { it.third.first > 0 }) operators(key) else emptyMap()
        return raw.map { (pos, kw, idStreet) ->
            // ids 1 and 44/45 are "(Unknown Operator)", "(Business Owner at Location)", "(Private Residence/Individual)": names in brackets are no operator
            val op = ops[idStreet.first]?.takeIf { !it.startsWith("(") }
            Nearby(Place(siteName(op, idStreet.second.substringAfterLast(", ").takeIf { it.isNotBlank() }, pos), idStreet.second, pos), 0.0, "", kw = kw, op = op)
        }
    }

    /** Checks an Open Charge Map key with one small request (for Settings). Returns a short German/English result. Blocking. */
    fun testOcm(key: String): String {
        val k = key.trim(); if (k.isEmpty()) return Lang.t("Kein Schlüssel eingetragen", "No key entered")
        return try {
            val arr = JSONArray(get("$OCM/poi?output=json&key=${URLEncoder.encode(k, "UTF-8")}&latitude=52.52&longitude=13.405&distance=5&distanceunit=KM&maxresults=3&compact=true&verbose=false"))
            Lang.t("Schlüssel funktioniert (${arr.length()} Ladesäulen in Berlin-Mitte gefunden)", "Key works (${arr.length()} chargers found in central Berlin)")
        } catch (e: Exception) {
            val m = e.message ?: e.javaClass.simpleName
            if (m.startsWith("HTTP 401") || m.startsWith("HTTP 403")) Lang.t("Schlüssel abgelehnt – bitte prüfen", "Key refused – please check it")
            else Lang.t("Keine Verbindung zu Open Charge Map: ", "Couldn't reach Open Charge Map: ") + m.take(60)
        }
    }

    private fun operators(key: String): Map<Int, String> {
        ocmOperators?.let { return it }
        return try {
            val j = JSONObject(get("$OCM/referencedata?output=json&key=${URLEncoder.encode(key, "UTF-8")}", readMs = 30000))
            val arr = j.optJSONArray("Operators") ?: JSONArray()
            val m = HashMap<Int, String>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val t = str(o, "Title") ?: continue
                m[o.optInt("ID", 0)] = t
            }
            if (m.isNotEmpty()) ocmOperators = m
            m
        } catch (e: Exception) { emptyMap() }
    }

    /** Google's encoded polyline (5 decimals), as Open Charge Map's polyline filter takes it. */
    fun encodePolyline(pts: List<LL>): String {
        val sb = StringBuilder()
        var pLat = 0L; var pLon = 0L
        fun enc(v: Long) {
            var x = if (v < 0) (v shl 1).inv() else v shl 1
            while (x >= 0x20) { sb.append(((0x20 or (x and 0x1f).toInt()) + 63).toChar()); x = x shr 5 }
            sb.append((x + 63).toInt().toChar())
        }
        for (p in pts) {
            val lat = Math.round(p.lat * 1e5); val lon = Math.round(p.lon * 1e5)
            enc(lat - pLat); enc(lon - pLon)
            pLat = lat; pLon = lon
        }
        return sb.toString()
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

    private fun get(url: String, connectMs: Int = 10000, readMs: Int = 20000): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs
        c.readTimeout = readMs
        c.setRequestProperty("User-Agent", UA)
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code ${body.take(100)}")
        return body
    }

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
