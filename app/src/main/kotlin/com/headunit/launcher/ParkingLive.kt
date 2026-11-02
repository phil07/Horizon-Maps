package com.headunit.launcher

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * A car park or garage with how many spaces it has and, where the operator publishes it, how many are free right now.
 * [free] null: no live count (only [total] is known). [open] false: closed now.
 */
data class Garage(val id: String, val name: String, val pos: LL, val total: Int, val free: Int?, val open: Boolean = true, val kind: String = "") {
    val live: Boolean get() = free != null
    val full: Boolean get() = !open || (free != null && free <= 0)

    /** "281 frei", "Voll", "Geschlossen", or "400 Plätze" without a live count. */
    fun label(): String = when {
        !open -> Lang.t("Geschlossen", "Closed")
        free != null && free <= 0 -> Lang.t("Voll", "Full")
        free != null -> Lang.t("$free frei", "$free free")
        total > 0 -> Lang.t("$total Plätze", "$total spaces")
        else -> ""
    }

    /** The short text on the map badge: the free count, "voll", or the size in grey. */
    fun badge(): String = when {
        !open || (free != null && free <= 0) -> Lang.t("voll", "full")
        free != null -> free.toString()
        total > 0 -> total.toString()
        else -> ""
    }

    /** 0 grey (no live count), 1 green, 2 amber (under a fifth or 20 left), 3 red (full or closed). */
    fun level(): Int = when {
        full -> 3
        free == null -> 0
        free < 20 || (total > 0 && free < total * 0.2) -> 2
        else -> 1
    }

    fun toNearby(from: LL): Nearby = Nearby(Place(name, Lang.t("Parkhaus", "Car park"), pos), Geo.dist(from, pos), label(), free = free, total = total, open = open)
}

/**
 * Live car park occupancy from two free sources, no key needed:
 *  - ParkAPI by MobiData BW (api.mobidata-bw.de): car parks all over Germany, live counts mostly in Baden-Württemberg;
 *  - ParkenDD (api.parkendd.de): city guidance systems, e.g. Dresden, Nürnberg, Freiburg, Zürich, Basel.
 * Car parks both list are merged (the one with a live count wins). Asked for a few kilometres at a time and kept 3 minutes.
 */
object ParkingLive {
    @Volatile var lastError: String? = null

    private class Area(val centre: LL, val radius: Int, val at: Long, val list: List<Garage>)
    private val lock = Any()
    private var held: Area? = null
    private var failedAt = 0L
    private const val KEEP_MS = 3 * 60_000L

    /** ParkenDD's cities, with their centre (asked once a day). */
    private var cities: List<Pair<String, LL>> = emptyList()
    private var citiesAt = 0L

    /** The car parks within [radius] metres of [p] (from what's held when it covers that), or null when nothing could be fetched. */
    fun around(p: LL, radius: Int = 2500): List<Garage>? {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val h = held
            if (h != null && now - h.at < KEEP_MS && Geo.dist(h.centre, p) + radius <= h.radius + 50) return h.list.filter { Geo.dist(p, it.pos) <= radius }
            if (now - failedAt < 30_000L) return h?.list?.filter { Geo.dist(p, it.pos) <= radius }
        }
        // a little more than asked, so moving the map a bit needs no new request
        val r = maxOf(radius, 3000) + 1500
        val a = try { mobidata(p, r) } catch (e: Exception) { lastError = e.message; null }
        val b = try { parkendd(p, r) } catch (e: Exception) { null }
        if (a == null && b == null) { synchronized(lock) { failedAt = now }; return null }
        val merged = merge(a.orEmpty(), b.orEmpty())
        synchronized(lock) { held = Area(p, r, now, merged) }
        lastError = null
        return merged.filter { Geo.dist(p, it.pos) <= radius }
    }

    /** What's held right now, without asking (for drawing on the map). */
    fun held(): List<Garage> = synchronized(lock) { held?.list.orEmpty() }

    /** Sorted for parking near [d]: the nearest with free spaces first, full or closed ones at the end. */
    fun nearestFree(list: List<Garage>, d: LL): List<Garage> = list.sortedWith(compareBy<Garage> { if (it.full) 1 else 0 }.thenBy { Geo.dist(d, it.pos) })

    /** Same for places found by the other searches, with their live count where one is known. */
    fun sortNearby(list: List<Nearby>): List<Nearby> = list.sortedWith(compareBy<Nearby> { if (it.open == false || (it.free != null && it.free <= 0)) 1 else 0 }.thenBy { it.dist })

    /** Adds live counts to car parks found elsewhere (TomTom, OpenStreetMap) and the car parks only these sources know. */
    fun enrich(found: List<Nearby>, garages: List<Garage>, from: LL, maxDist: Double): List<Nearby> {
        val used = HashSet<String>()
        val out = ArrayList<Nearby>()
        for (n in found) {
            val g = garages.filter { it.id !in used && Geo.dist(it.pos, n.place.pos) < 90.0 }.minByOrNull { Geo.dist(it.pos, n.place.pos) }
            if (g != null) { used += g.id; out += n.copy(extra = listOf(g.label(), n.extra).filter { it.isNotBlank() }.joinToString(" · "), free = g.free, total = g.total, open = g.open) }
            else out += n
        }
        for (g in garages) if (g.id !in used && Geo.dist(from, g.pos) <= maxDist) out += g.toNearby(from)
        return out
    }

    internal fun merge(a: List<Garage>, b: List<Garage>): List<Garage> {
        val out = ArrayList(a)
        for (g in b) {
            val i = out.indexOfFirst { Geo.dist(it.pos, g.pos) < 80.0 }
            if (i < 0) out += g
            else if (!out[i].live && g.live) out[i] = g.copy(name = if (out[i].name.length > g.name.length) g.name else out[i].name)
        }
        return out
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        val code = c.responseCode
        if (code !in 200..299) throw java.io.IOException("HTTP $code")
        return c.inputStream.bufferedReader().readText()
    }

    private val PARK_TYPES = setOf("CAR_PARK", "UNDERGROUND", "OFF_STREET_PARKING_GROUND")

    private fun mobidata(p: LL, radius: Int): List<Garage> {
        val url = String.format(java.util.Locale.US, "https://api.mobidata-bw.de/park-api/api/public/v3/parking-sites?lat=%.5f&lon=%.5f&radius=%d&purpose=CAR&limit=300", p.lat, p.lon, radius)
        return parseMobidata(get(url), System.currentTimeMillis())
    }

    /** ParkAPI v3: live counts only when they're under 2 hours old (some feeds stopped long ago). */
    internal fun parseMobidata(body: String, now: Long): List<Garage> {
        val arr = JSONObject(body).optJSONArray("items") ?: return emptyList()
        val out = ArrayList<Garage>()
        for (i in 0 until arr.length()) {
            val x = arr.optJSONObject(i) ?: continue
            val type = x.optString("type")
            if (type !in PARK_TYPES) continue
            val total = x.optInt("capacity", 0)
            // the small car parks only clutter the map (a dozen spaces behind a shop)
            if (total in 1..19) continue
            val lat = x.optDouble("lat", Double.NaN); val lon = x.optDouble("lon", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            val fresh = x.optBoolean("has_realtime_data", false) && x.has("realtime_free_capacity") && !x.isNull("realtime_free_capacity") &&
                now - parseIso(x.optString("realtime_data_updated_at")) < 2 * 3600_000L
            val free = if (fresh) x.optInt("realtime_free_capacity") else null
            val open = !(fresh && x.optString("realtime_opening_status") == "CLOSED")
            val name = x.optString("name").removePrefix("Parkhaus ").trim().ifBlank { Lang.t("Parkhaus", "Car park") }
            out += Garage("m" + x.optString("id"), name, LL(lat, lon), total, free, open, type)
        }
        return out
    }

    private fun parkendd(p: LL, radius: Int): List<Garage> {
        val now = System.currentTimeMillis()
        if (now - citiesAt > 24 * 3600_000L || cities.isEmpty()) {
            val o = JSONObject(get("https://api.parkendd.de/")).optJSONObject("cities") ?: return emptyList()
            val l = ArrayList<Pair<String, LL>>()
            for (k in o.keys()) {
                val c = o.optJSONObject(k) ?: continue
                if (!c.optBoolean("active_support", false)) continue
                val co = c.optJSONObject("coords") ?: continue
                l += k to LL(co.optDouble("lat"), co.optDouble("lng"))
            }
            cities = l; citiesAt = now
        }
        // only a city whose centre is within 25 km (a city's car parks are all within that)
        val near = cities.filter { Geo.dist(it.second, p) < 25_000.0 }
        val out = ArrayList<Garage>()
        for ((id, _) in near) try { out += parseParkendd(id, get("https://api.parkendd.de/$id"), now).filter { Geo.dist(p, it.pos) <= radius } } catch (e: Exception) { }
        return out
    }

    internal fun parseParkendd(city: String, body: String, now: Long): List<Garage> {
        val o = JSONObject(body)
        val fresh = now - parseIso(o.optString("last_updated")) < 2 * 3600_000L
        val arr = o.optJSONArray("lots") ?: return emptyList()
        val out = ArrayList<Garage>()
        for (i in 0 until arr.length()) {
            val x = arr.optJSONObject(i) ?: continue
            val co = x.optJSONObject("coords") ?: continue
            val total = x.optInt("total", 0)
            if (total in 1..19) continue
            val state = x.optString("state")
            val live = fresh && state != "nodata" && x.has("free")
            out += Garage("p$city/" + x.optString("id"), x.optString("name"), LL(co.optDouble("lat"), co.optDouble("lng")), total,
                if (live) x.optInt("free") else null, !(live && state == "closed"), x.optString("lot_type"))
        }
        return out
    }

    /** "2026-11-02T15:25:00Z" or ParkenDD's "2026-11-02T15:29:38" (UTC); 0 when unknown. */
    internal fun parseIso(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        val t = s.trim().removeSuffix("Z").substringBefore('+').substringBefore('.')
        return try {
            val f = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            f.timeZone = java.util.TimeZone.getTimeZone("UTC")
            f.parse(t)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }
}
