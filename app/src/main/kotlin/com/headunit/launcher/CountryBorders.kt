package com.headunit.launcher

import org.json.JSONObject

/**
 * Country outlines (Natural Earth 50 m, public domain, Europe and its edges; in assets/country_borders.json) and what each country
 * is about for the notice at the border. The limits and the rest are the usual ones: the signs on the road count, so they are said
 * as a guide.
 */
internal object CountryBorders {
    private class Ring(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double, val lat: DoubleArray, val lon: DoubleArray)

    @Volatile private var rings: Map<String, List<Ring>>? = null

    /** One country for the notice: its names, the usual limits (km/h, or mph when [mph]), the currency, the languages and the emergency number. */
    class Info(val de: String, val en: String, val motorway: Int, val rural: Int, val urban: Int, val mph: Boolean,
               val currency: String, val language: String, val emergency: String = "112")

    val info: Map<String, Info> = mapOf(
        "AT" to Info("Österreich", "Austria", 130, 100, 50, false, "Euro", "Deutsch"),
        "BE" to Info("Belgien", "Belgium", 120, 90, 50, false, "Euro", "Niederländisch, Französisch, Deutsch"),
        "BG" to Info("Bulgarien", "Bulgaria", 140, 90, 50, false, "Euro", "Bulgarisch"),
        "CH" to Info("Schweiz", "Switzerland", 120, 80, 50, false, "Franken", "Deutsch, Französisch, Italienisch"),
        "CZ" to Info("Tschechien", "Czechia", 130, 90, 50, false, "Krone", "Tschechisch"),
        "DE" to Info("Deutschland", "Germany", 130, 100, 50, false, "Euro", "Deutsch"),
        "DK" to Info("Dänemark", "Denmark", 130, 80, 50, false, "Krone", "Dänisch"),
        "ES" to Info("Spanien", "Spain", 120, 90, 50, false, "Euro", "Spanisch"),
        "FR" to Info("Frankreich", "France", 130, 80, 50, false, "Euro", "Französisch"),
        "GB" to Info("Großbritannien", "Great Britain", 70, 60, 30, true, "Pfund", "Englisch", "999"),
        "GR" to Info("Griechenland", "Greece", 130, 90, 50, false, "Euro", "Griechisch"),
        "HR" to Info("Kroatien", "Croatia", 130, 90, 50, false, "Euro", "Kroatisch"),
        "HU" to Info("Ungarn", "Hungary", 130, 90, 50, false, "Forint", "Ungarisch"),
        "IE" to Info("Irland", "Ireland", 120, 80, 50, false, "Euro", "Englisch, Irisch", "999"),
        "IT" to Info("Italien", "Italy", 130, 90, 50, false, "Euro", "Italienisch"),
        "LU" to Info("Luxemburg", "Luxembourg", 130, 90, 50, false, "Euro", "Luxemburgisch, Französisch, Deutsch"),
        "NL" to Info("Niederlande", "Netherlands", 100, 80, 50, false, "Euro", "Niederländisch"),
        "NO" to Info("Norwegen", "Norway", 110, 80, 50, false, "Krone", "Norwegisch"),
        "PL" to Info("Polen", "Poland", 140, 90, 50, false, "Zloty", "Polnisch"),
        "PT" to Info("Portugal", "Portugal", 120, 90, 50, false, "Euro", "Portugiesisch"),
        "RO" to Info("Rumänien", "Romania", 130, 90, 50, false, "Leu", "Rumänisch"),
        "SE" to Info("Schweden", "Sweden", 110, 70, 50, false, "Krone", "Schwedisch"),
        "SI" to Info("Slowenien", "Slovenia", 130, 90, 50, false, "Euro", "Slowenisch"),
        "SK" to Info("Slowakei", "Slovakia", 130, 90, 50, false, "Euro", "Slowakisch"),
    )

    /** The outlines, read from the app's assets once (none when the file can't be read: then no notice comes). */
    private fun load(ctx: android.content.Context): Map<String, List<Ring>> {
        rings?.let { return it }
        val out = HashMap<String, List<Ring>>()
        try {
            val obj = JSONObject(ctx.assets.open("country_borders.json").bufferedReader().use { it.readText() })
            for (cc in obj.keys()) {
                val arr = obj.getJSONArray(cc)
                val list = ArrayList<Ring>()
                for (i in 0 until arr.length()) {
                    val pts = arr.getJSONArray(i)
                    val n = pts.length()
                    val la = DoubleArray(n); val lo = DoubleArray(n)
                    for (k in 0 until n) { val c = pts.getJSONArray(k); lo[k] = c.getDouble(0); la[k] = c.getDouble(1) }
                    list += Ring(la.min(), la.max(), lo.min(), lo.max(), la, lo)
                }
                out[cc] = list
            }
        } catch (e: Exception) { softFail(e) }
        rings = out
        return out
    }

    private fun inside(r: Ring, lat: Double, lon: Double): Boolean {
        if (lat < r.minLat || lat > r.maxLat || lon < r.minLon || lon > r.maxLon) return false
        var c = false
        var j = r.lat.size - 1
        for (i in r.lat.indices) {
            if ((r.lat[i] > lat) != (r.lat[j] > lat) && lon < (r.lon[j] - r.lon[i]) * (lat - r.lat[i]) / (r.lat[j] - r.lat[i]) + r.lon[i]) c = !c
            j = i
        }
        return c
    }

    /** The countries [r] goes through, in order, sampled about every 5 km (codes from the table only; none repeated). */
    fun along(ctx: android.content.Context, r: Route): List<String> {
        val out = ArrayList<String>()
        var next = 0.0
        for (i in r.pts.indices) {
            if (r.cum[i] < next) continue
            next = r.cum[i] + 5_000.0
            val c = at(ctx, r.pts[i]) ?: continue
            if (c in info && c !in out) out += c
        }
        return out
    }

    /** The country code of [p], or null when it is in none of the outlines. */
    fun at(ctx: android.content.Context, p: LL): String? {
        for ((cc, list) in load(ctx)) for (r in list) if (inside(r, p.lat, p.lon)) return cc
        return null
    }
}
