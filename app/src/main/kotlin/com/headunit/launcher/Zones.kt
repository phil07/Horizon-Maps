package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Clean Air Zones, ULEZ, low emission zones and charging zones in Great Britain, or the Umweltzonen in Germany (when you're
 * there), from OpenStreetMap (boundary=low_emission_zone and similar). Downloaded once and refreshed monthly; only as good as
 * the map data.
 */
object Zones {
    /** [country]: the zone's country code ("DE", "FR", "NL", "GB", ...), or "" when it could not be told (then not German). */
    class Zone(val name: String, val rings: List<List<LL>>, val country: String = "") {
        val minLat = rings.minOf { r -> r.minOf { it.lat } }
        val maxLat = rings.maxOf { r -> r.maxOf { it.lat } }
        val minLon = rings.minOf { r -> r.minOf { it.lon } }
        val maxLon = rings.maxOf { r -> r.maxOf { it.lon } }
    }

    /**
     * Germany's outline, roughly (a few km off at worst): lat, lon pairs going round the border. The German download box also
     * covers Strasbourg, Luxembourg, Basel, Zürich, Prague, Arnhem, Szczecin and more, whose zones have their own rules.
     */
    private val deOutline: List<LL> = doubleArrayOf(
        53.75, 6.40, 55.10, 8.10, 54.91, 8.66, 54.82, 9.45, 54.85, 9.95, 54.60, 11.30, 54.75, 13.30, 54.70, 13.80,
        53.93, 14.22, 53.75, 14.27, 53.45, 14.40, 53.25, 14.40, 52.95, 14.13, 52.60, 14.58, 52.34, 14.56, 52.10, 14.72,
        51.75, 14.62, 51.55, 14.72, 51.30, 14.98, 51.15, 14.98, 50.88, 14.83, 50.98, 14.60, 51.02, 14.30, 50.88, 14.00,
        50.75, 13.75, 50.55, 13.20, 50.42, 12.97, 50.35, 12.45, 50.32, 12.20, 50.15, 12.10, 49.97, 12.45, 49.70, 12.50,
        49.45, 12.65, 49.30, 12.95, 49.12, 13.20, 48.95, 13.50, 48.77, 13.84, 48.55, 13.75, 48.45, 13.45, 48.26, 13.03,
        48.15, 12.83, 47.95, 12.93, 47.80, 13.00, 47.53, 13.02, 47.60, 12.80, 47.64, 12.45, 47.61, 12.20, 47.58, 11.70,
        47.40, 11.25, 47.40, 10.98, 47.55, 10.75, 47.45, 10.45, 47.27, 10.18, 47.53, 9.74, 47.53, 9.62, 47.62, 9.40, 47.65, 9.18,
        47.685, 9.05, 47.665, 8.87, 47.80, 8.75, 47.72, 8.55, 47.58, 8.25, 47.55, 7.95, 47.555, 7.70, 47.60, 7.68, 47.59, 7.64, 47.585, 7.59, 47.80, 7.53, 48.10, 7.58,
        48.35, 7.73, 48.58, 7.81, 48.80, 8.10, 48.97, 8.23, 49.05, 7.95, 49.12, 7.70, 49.18, 7.40, 49.11, 7.06,
        49.20, 6.98, 49.22, 6.88, 49.35, 6.60, 49.46, 6.37, 49.71, 6.50, 49.82, 6.40, 50.00, 6.13, 50.13, 6.13, 50.33, 6.38,
        50.50, 6.20, 50.62, 6.20, 50.75, 6.02, 50.85, 6.08, 50.90, 6.09, 50.97, 6.02, 51.05, 5.87, 51.20, 6.08, 51.40, 6.22, 51.60, 6.10,
        51.80, 5.97, 51.85, 6.18, 51.85, 6.40, 52.05, 6.80, 52.25, 6.98, 52.45, 6.95, 52.65, 7.05, 53.00, 7.20,
        53.30, 7.20, 53.55, 6.90,
    ).let { a -> (0 until a.size / 2).map { LL(a[it * 2], a[it * 2 + 1]) } }

    private fun inRing(r: List<LL>, p: LL): Boolean {
        var inside = false
        var j = r.size - 1
        for (i in r.indices) {
            val a = r[i]; val b = r[j]
            if ((a.lat > p.lat) != (b.lat > p.lat) && p.lon < (b.lon - a.lon) * (p.lat - a.lat) / (b.lat - a.lat) + a.lon) inside = !inside
            j = i
        }
        return inside
    }

    /** Whether a point is in Germany (from the rough outline above; no network). */
    fun inGermany(p: LL): Boolean = p.lat in 47.2..55.2 && p.lon in 5.8..15.1 && inRing(deOutline, p)

    /**
     * A zone's country: from its tags when they say (a "DE:270.1" traffic sign, a country code), from words in its name that
     * only one country uses, otherwise from where its middle is (Germany or not).
     */
    private fun countryOf(name: String, tags: JSONObject?, mid: LL, region: String): String {
        if (region != "DE") return "GB"
        if (tags != null) {
            for (k in listOf("ISO3166-1", "ISO3166-1:alpha2", "is_in:country_code", "addr:country", "country")) {
                val v = tags.optString(k).trim().uppercase()
                if (Regex("^[A-Z]{2}$").matches(v)) return v
            }
            for (k in listOf("traffic_sign", "zone:traffic", "source:zone")) {
                val m = Regex("^([A-Za-z]{2}):").find(tags.optString(k).trim()) ?: continue
                return m.groupValues[1].uppercase()
            }
        }
        val n = name.lowercase()
        when {
            n.contains("zfe") || n.contains("faibles émissions") || n.contains("faibles emissions") || n.contains("crit'air") || n.contains("critair") -> return "FR"
            n.contains("milieuzone") -> return "NL"
            n.contains("nízkoemisní") || n.contains("nizkoemisni") -> return "CZ"
            n.contains("strefa czystego") || n.contains("czystego transportu") -> return "PL"
            n.contains("miljøzone") || n.contains("miljozone") -> return "DK"
        }
        return if (inGermany(mid)) "DE" else ""
    }

    private fun mid(rings: List<List<LL>>): LL {
        val all = rings.flatten()
        return LL((all.minOf { it.lat } + all.maxOf { it.lat }) / 2, (all.minOf { it.lon } + all.maxOf { it.lon }) / 2)
    }

    @Volatile private var cache: List<Zone>? = null
    @Volatile private var cacheRegion = ""

    /** Which country's zones: Germany's when you're in Germany (from the GPS), otherwise Great Britain's. */
    private fun region(): String = if (RoadRefs.country.uppercase() == "DE") "DE" else "GB"

    /** The box the zones are downloaded for: south, west, north, east. */
    private fun box(region: String): String = if (region == "DE") "47.2,5.8,55.1,15.1" else "49.8,-8.7,60.9,1.9"
    @Volatile private var fetching = false
    @Volatile var status = ""

    private fun file(ctx: Context, region: String = region()) = File(ctx.filesDir, if (region == "DE") "zones-DE.json" else "zones.json")

    /** The zones known on this device (empty until the first download has worked). */
    fun all(ctx: Context): List<Zone> {
        val reg = region()
        cache?.let { if (cacheRegion == reg) return it }
        val f = file(ctx, reg)
        if (!f.exists()) return emptyList()
        val out = ArrayList<Zone>()
        try {
            val a = JSONArray(f.readText())
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val rs = o.getJSONArray("r")
                val rings = (0 until rs.length()).map { k ->
                    val pa = rs.getJSONArray(k)
                    (0 until pa.length() / 2).map { j -> LL(pa.getDouble(j * 2), pa.getDouble(j * 2 + 1)) }
                }.filter { it.size >= 4 }
                if (rings.isNotEmpty()) {
                    // files from before the country was stored: worked out again from the name and where the zone is
                    val cc = if (o.has("c")) o.optString("c") else countryOf(o.optString("n"), null, mid(rings), reg)
                    out += Zone(o.optString("n"), rings, cc)
                }
            }
        } catch (e: Exception) { }
        cache = out; cacheRegion = reg
        status = if (out.isEmpty()) Lang.t("Keine Zonen in den Kartendaten gefunden", "No zones found in the map data") else Lang.t("${out.size} Zonen: ", "${out.size} zones: ") + out.joinToString(", ") { it.name }.take(200)
        return out
    }

    fun needsRefresh(ctx: Context): Boolean {
        val f = file(ctx)
        return !f.exists() || System.currentTimeMillis() - f.lastModified() > 30L * 86_400_000L
    }

    /** Joins the outer ways of a boundary into closed rings. */
    private fun stitch(ways: List<List<LL>>): List<List<LL>> {
        val left = ways.filter { it.size >= 2 }.toMutableList()
        val rings = ArrayList<List<LL>>()
        fun same(a: LL, b: LL) = Math.abs(a.lat - b.lat) < 1e-7 && Math.abs(a.lon - b.lon) < 1e-7
        while (left.isNotEmpty()) {
            val ring = ArrayList(left.removeAt(0))
            var grew = true
            while (!same(ring.first(), ring.last()) && grew) {
                grew = false
                val it = left.iterator()
                while (it.hasNext()) {
                    val w = it.next()
                    when {
                        same(ring.last(), w.first()) -> { ring.addAll(w.drop(1)); it.remove(); grew = true }
                        same(ring.last(), w.last()) -> { ring.addAll(w.asReversed().drop(1)); it.remove(); grew = true }
                        else -> continue
                    }
                    if (same(ring.first(), ring.last())) break
                }
            }
            if (ring.size >= 4) rings += ring
        }
        return rings
    }

    /** Keeps at most [max] points of a ring, so checks and route avoidance stay quick. */
    private fun thin(r: List<LL>, max: Int): List<LL> {
        if (r.size <= max) return r
        val step = Math.ceil(r.size / max.toDouble()).toInt()
        return r.filterIndexed { i, _ -> i % step == 0 } + r.last()
    }

    /** Downloads the zones for Great Britain, or Germany when you're there (blocking: call it off the main thread). */
    fun fetch(ctx: Context) {
        if (fetching) return
        fetching = true
        val reg = region()
        val bb = box(reg)
        try {
            val q = "[out:json][timeout:90];(relation[\"boundary\"~\"^(low_emission_zone|congestion_charge|clean_air_zone)$\"]($bb);" +
                "way[\"boundary\"~\"^(low_emission_zone|congestion_charge|clean_air_zone)$\"]($bb););out geom;"
            val els = Overpass.query(q, 15000, 120000, 3)
            val out = JSONArray()
            var n = 0
            for (i in 0 until els.length()) {
                val e = els.getJSONObject(i)
                val tags = e.optJSONObject("tags") ?: JSONObject()
                val name = tags.optString("name").ifBlank { tags.optString("official_name") }.ifBlank { Lang.t("Umweltzone", "Low emission zone") }
                val ways = ArrayList<List<LL>>()
                if (e.optString("type") == "way") {
                    val g = e.optJSONArray("geometry") ?: continue
                    ways += (0 until g.length()).map { k -> g.getJSONObject(k).let { LL(it.getDouble("lat"), it.getDouble("lon")) } }
                } else {
                    val ms = e.optJSONArray("members") ?: continue
                    for (k in 0 until ms.length()) {
                        val m = ms.getJSONObject(k)
                        if (m.optString("type") != "way" || m.optString("role") == "inner") continue
                        val g = m.optJSONArray("geometry") ?: continue
                        ways += (0 until g.length()).mapNotNull { j -> g.optJSONObject(j)?.let { LL(it.getDouble("lat"), it.getDouble("lon")) } }
                    }
                }
                val rings = stitch(ways).map { thin(it, 400) }
                if (rings.isEmpty()) continue
                val ra = JSONArray()
                for (r in rings) { val pa = JSONArray(); for (p in r) { pa.put(Math.round(p.lat * 1e5) / 1e5); pa.put(Math.round(p.lon * 1e5) / 1e5) }; ra.put(pa) }
                out.put(JSONObject().put("n", name).put("c", countryOf(name, tags, mid(rings), reg)).put("r", ra))
                n++
            }
            file(ctx, reg).writeText(out.toString())
            cache = null
            all(ctx)
            if (n == 0) status = Lang.t("Keine Zonen in den Kartendaten gefunden", "No zones found in the map data")
        } catch (e: Exception) {
            status = Lang.t("Zonen-Download fehlgeschlagen: ", "Zone download failed: ") + (e.message ?: e.javaClass.simpleName).take(120)
        } finally { fetching = false }
    }

    fun inside(z: Zone, p: LL): Boolean {
        if (p.lat < z.minLat || p.lat > z.maxLat || p.lon < z.minLon || p.lon > z.maxLon) return false
        var inside = false
        for (r in z.rings) {
            var j = r.size - 1
            for (i in r.indices) {
                val a = r[i]; val b = r[j]
                if ((a.lat > p.lat) != (b.lat > p.lat) && p.lon < (b.lon - a.lon) * (p.lat - a.lat) / (b.lat - a.lat) + a.lon) inside = !inside
                j = i
            }
        }
        return inside
    }

    /** Where a route goes into each zone: (zone, metres from the route start). A route that starts inside a zone counts as entering it at 0. */
    fun entries(r: Route, zones: List<Zone>): List<Pair<Zone, Double>> {
        if (zones.isEmpty() || r.pts.size < 2) return emptyList()
        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
        for (p in r.pts) { if (p.lat < minLat) minLat = p.lat; if (p.lat > maxLat) maxLat = p.lat; if (p.lon < minLon) minLon = p.lon; if (p.lon > maxLon) maxLon = p.lon }
        val out = ArrayList<Pair<Zone, Double>>()
        for (z in zones) {
            if (z.maxLat < minLat || z.minLat > maxLat || z.maxLon < minLon || z.minLon > maxLon) continue
            var last = -1e9
            var wasIn = false
            for (i in r.pts.indices) {
                if (r.cum[i] - last < 150.0 && i != r.pts.size - 1) continue
                last = r.cum[i]
                val inZ = inside(z, r.pts[i])
                if (inZ && !wasIn) { out += z to r.cum[i]; break }
                wasIn = inZ
            }
        }
        return out.sortedBy { it.second }
    }
}
