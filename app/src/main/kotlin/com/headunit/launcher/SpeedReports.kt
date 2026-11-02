package com.headunit.launcher

import org.json.JSONObject
import java.util.Locale

/** A speed limit a driver reported: [kmh] 0 = no general limit; [heading] -1 when unknown; [t] when (this device's clock). */
internal data class SpeedReport(val pos: LL, val heading: Double, val kmh: Int, val t: Long, val mine: Boolean = false)

/**
 * Speed limits reported by drivers (tap on the sign, or Melden in the car), shared through Horizon's own reports server (the
 * same Cloudflare Worker as the police / jam reports, /v1/limits). A device counts once per spot; the more devices report the
 * same value, the more it counts in the vote (see [votes]). Reports are kept for a year and count less as they get older.
 */
internal object SpeedReports {
    private val lock = Any()
    private var items: List<SpeedReport> = emptyList()
    private var box: DoubleArray? = null      // s, w, n, e
    private var boxAt = 0L
    private var failedAt = 0L
    @Volatile var lastError = ""

    private const val HALF = 0.03          // about 3 km each way
    private const val MARGIN = 0.008
    private const val KEEP_MS = 10L * 60 * 1000
    private const val YEAR_MS = 365L * 24 * 3600 * 1000

    /** The reports around [p], from what's held or fetched now (with sharing off or no server: none). */
    fun around(prefs: Prefs, p: LL): List<SpeedReport> {
        if (!SharedReports.sharing(prefs)) return emptyList()
        val now = System.currentTimeMillis()
        synchronized(lock) {
            val b = box
            val inside = b != null && p.lat > b[0] + MARGIN && p.lon > b[1] + MARGIN && p.lat < b[2] - MARGIN && p.lon < b[3] - MARGIN
            if (inside && now - boxAt < KEEP_MS) return items
            if (now - failedAt < 60_000L) return items
        }
        val nb = doubleArrayOf(p.lat - HALF, p.lon - HALF, p.lat + HALF, p.lon + HALF)
        val got = fetch(prefs, nb)
        synchronized(lock) {
            if (got == null) { failedAt = now; return items }
            items = got; box = nb; boxAt = now
            return items
        }
    }

    private fun fetch(prefs: Prefs, b: DoubleArray): List<SpeedReport>? {
        val base = SharedReports.baseUrl(prefs)
        if (base.isBlank()) return null
        return try {
            fun f(v: Double) = String.format(Locale.US, "%.5f", v)
            val o = JSONObject(SharedReports.request("GET", "$base/v1/limits?s=${f(b[0])}&w=${f(b[1])}&n=${f(b[2])}&e=${f(b[3])}", SharedReports.deviceId(prefs), null))
            val local = System.currentTimeMillis()
            val skew = if (o.has("now")) o.optLong("now", local) - local else 0L
            val arr = o.optJSONArray("limits")
            val out = ArrayList<SpeedReport>()
            if (arr != null) for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                val lat = r.optDouble("lat", Double.NaN); val lon = r.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) continue
                out += SpeedReport(LL(lat, lon), r.optDouble("h", -1.0), r.optInt("kmh", -1), r.optLong("t", local) - skew, r.optBoolean("mine", false))
            }
            lastError = ""
            out
        } catch (e: Throwable) { lastError = (e.message ?: e.javaClass.simpleName).take(80); null }
    }

    /** Sends your report ([kmh] 0 = no limit) and keeps it at once, so it counts straight away. True when the server took it. */
    fun send(prefs: Prefs, p: LL, heading: Double, kmh: Int): Boolean {
        val base = SharedReports.baseUrl(prefs)
        val mine = SpeedReport(LL(Math.round(p.lat * 1e5) / 1e5, Math.round(p.lon * 1e5) / 1e5), if (heading >= 0) heading else -1.0, kmh, System.currentTimeMillis(), true)
        synchronized(lock) {
            // (your own earlier report at this spot is replaced, as on the server)
            items = items.filterNot { it.mine && Geo.dist(it.pos, p) < 80.0 && (it.heading < 0 || heading < 0 || angle(it.heading, heading) < 60.0) } + mine
        }
        if (base.isBlank() || !SharedReports.sharing(prefs)) return false
        return try {
            val body = JSONObject().put("lat", mine.pos.lat).put("lon", mine.pos.lon).put("heading", if (heading >= 0) Math.round(heading).toInt() else -1).put("kmh", kmh).toString()
            SharedReports.request("POST", "$base/v1/limits", SharedReports.deviceId(prefs), body)
            lastError = ""
            true
        } catch (e: Throwable) { lastError = (e.message ?: e.javaClass.simpleName).take(80); false }
    }

    /**
     * The reports that apply at [p] driving at [heading]: reported within 150 m along your way (behind or a little ahead), close to
     * your line of travel and in your direction. One vote per value: 1 device counts 1.0, 2 count 2.0, 3 or more 3.0 (as much as a
     * sign), at most 4.0; older reports count less (down to 40 % at a year), and a report older than a newer one with another value
     * at the same spot counts half (the limit may have changed since).
     */
    fun votes(reports: List<SpeedReport>, p: LL, heading: Double, now: Long): List<SpeedVote.Vote> {
        if (reports.isEmpty()) return emptyList()
        val h = if (heading >= 0) Math.toRadians(heading) else Double.NaN
        val kx = 111_320.0 * Math.cos(Math.toRadians(p.lat))
        val near = reports.filter { r ->
            val age = now - r.t
            if (age > YEAR_MS || r.kmh < 0) return@filter false
            val dx = (r.pos.lon - p.lon) * kx; val dy = (r.pos.lat - p.lat) * 111_320.0
            if (h.isNaN()) return@filter Math.hypot(dx, dy) < 60.0
            val along = dx * Math.sin(h) + dy * Math.cos(h)
            val lateral = Math.abs(dx * Math.cos(h) - dy * Math.sin(h))
            along in -150.0..60.0 && lateral < 20.0 && (r.heading < 0 || angle(r.heading, heading) < 50.0)
        }
        if (near.isEmpty()) return emptyList()
        val out = ArrayList<SpeedVote.Vote>()
        for ((kmh, rs) in near.groupBy { it.kmh }) {
            val newestOther = near.filter { it.kmh != kmh }.maxOfOrNull { it.t } ?: 0L
            val w = rs.sumOf { r ->
                val age = (now - r.t).coerceAtLeast(0L).toDouble() / YEAR_MS
                (1.0 - 0.6 * age) * (if (r.t < newestOther) 0.5 else 1.0)
            }
            // three agreeing devices count as much as a sign, never more than 4
            val weight = minOf(4.0, if (w >= 3.0) w.coerceAtLeast(SpeedVote.W_SIGN) else w)
            val ms = if (kmh == 0) SpeedVote.NONE else kmh / 3.6
            out += SpeedVote.Vote(Lang.t("Nutzer", "Drivers") + " (${rs.size})", ms, weight)
        }
        return out
    }

    private fun angle(a: Double, b: Double): Double { val d = ((a - b) % 360 + 540) % 360 - 180; return Math.abs(d) }
}
