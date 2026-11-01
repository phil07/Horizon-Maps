package com.headunit.launcher

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * One electric car from Open EV Data: its usable battery and DC charging behaviour.
 * [curve] is (battery %, kW) points sorted by %, empty when the dataset has none.
 */
data class EvCar(
    val id: String,
    val brand: String,
    val model: String,
    val variant: String?,
    val year: Int?,
    val kwh: Double,
    val maxKw: Double?,
    val curve: List<Pair<Int, Double>>,
) {
    /** Name shown to the user, e.g. "Volkswagen ID.7 Pro (2023)". */
    val label: String
        get() = buildString {
            append(brand).append(' ').append(model)
            if (!variant.isNullOrBlank()) append(' ').append(variant)
            if (year != null) append(" (").append(year).append(')')
        }.trim()
}

/**
 * The electric car you drive, picked from the Open EV Data list (github.com/KilowattApp/open-ev-data, MIT).
 * Its usable battery and DC charging curve make the charging-stop times accurate.
 */
object EvCars {
    /** Attribution text shown in Settings. */
    const val SOURCE = "Open EV Data (MIT)"

    private const val URL_DATA = "https://raw.githubusercontent.com/KilowattApp/open-ev-data/master/data/ev-data.json"
    private const val FILE = "ev-data.json"
    /** Charging power assumed when neither the car nor the station says better. */
    private const val DEFAULT_KW = 50.0

    private val lock = Any()
    @Volatile private var cached: List<EvCar>? = null

    /**
     * Blocking: returns all cars. Downloads the dataset once into ctx.filesDir/ev-data.json, then reads the file; keeps the
     * parsed list in memory. Throws on network or parse failure. Call off the main thread.
     */
    fun load(ctx: Context): List<EvCar> {
        cached?.let { return it }
        synchronized(lock) {
            cached?.let { return it }
            val f = File(ctx.filesDir, FILE)
            val fromCache = f.isFile && f.length() > 0
            if (!fromCache) download(f)
            val cars = try {
                parse(f.readText())
            } catch (e: Exception) {
                // a broken file must not stick: drop it, and if it was an old copy try one fresh download
                f.delete()
                if (!fromCache) throw IOException("EV data: ${e.message}", e)
                download(f)
                try { parse(f.readText()) } catch (e2: Exception) { f.delete(); throw IOException("EV data: ${e2.message}", e2) }
            }
            cached = cars
            return cars
        }
    }

    /** Downloads the dataset to a temporary file and renames it, so a cut download never leaves a broken [dest]. */
    private fun download(dest: File) {
        val tmp = File(dest.parentFile, "$FILE.part")
        val c = URL(URL_DATA).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 30000
        c.setRequestProperty("User-Agent", "HorizonNav/${BuildConfig.VERSION_NAME}")
        c.setRequestProperty("Accept", "application/json")
        try {
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            c.inputStream.use { inp -> tmp.outputStream().use { out -> inp.copyTo(out) } }
        } catch (e: Exception) {
            tmp.delete()
            throw e
        } finally { c.disconnect() }
        if (tmp.length() <= 0L) { tmp.delete(); throw IOException("EV data: empty download") }
        if (!tmp.renameTo(dest)) {
            dest.delete()
            if (!tmp.renameTo(dest)) { tmp.delete(); throw IOException("EV data: could not save $FILE") }
        }
    }

    private fun parse(text: String): List<EvCar> {
        val root = JSONObject(text)
        val brands = HashMap<String, String>()
        root.optJSONArray("brands")?.let { arr ->
            for (i in 0 until arr.length()) {
                val b = arr.optJSONObject(i) ?: continue
                val id = b.optString("id"); val name = b.optString("name")
                if (id.isNotBlank() && name.isNotBlank()) brands[id] = name
            }
        }
        val data = root.getJSONArray("data")
        val out = ArrayList<EvCar>(data.length())
        for (i in 0 until data.length()) {
            val x = data.optJSONObject(i) ?: continue
            val kwh = x.optDouble("usable_battery_size", Double.NaN)
            if (kwh.isNaN() || kwh <= 0.0) continue
            val brandRaw = x.optStringOrNull("brand").orEmpty()
            // the brand is normally a name; if it is an id (or missing), look it up in the brands list
            val brand = brands[brandRaw] ?: brandRaw.ifBlank { brands[x.optString("brand_id")].orEmpty() }
            val model = x.optStringOrNull("model").orEmpty()
            if (model.isBlank()) continue
            val year = if (x.isNull("release_year")) null else x.optInt("release_year", 0).takeIf { it > 0 }
            val dc = x.optJSONObject("dc_charger")
            val maxKw = dc?.optDouble("max_power", Double.NaN)?.takeIf { !it.isNaN() && it > 0.0 }
            val curve = ArrayList<Pair<Int, Double>>()
            dc?.optJSONArray("charging_curve")?.let { arr ->
                for (j in 0 until arr.length()) {
                    val pt = arr.optJSONObject(j) ?: continue
                    val pct = pt.optDouble("percentage", Double.NaN)
                    val kw = pt.optDouble("power", Double.NaN)
                    if (pct.isNaN() || kw.isNaN() || pct < 0 || pct > 100 || kw <= 0) continue
                    curve += Math.round(pct).toInt() to kw
                }
            }
            curve.sortBy { it.first }
            out += EvCar(x.optString("id", "$i"), brand.trim(), model.trim(), x.optStringOrNull("variant")?.trim()?.ifBlank { null },
                year, kwh, maxKw, curve)
        }
        if (out.isEmpty()) throw IOException("EV data: no cars")
        out.sortBy { it.label.lowercase() }
        return out
    }

    /** org.json's optString turns JSON null into "null"; this gives a real null instead. */
    private fun JSONObject.optStringOrNull(k: String): String? = if (isNull(k)) null else optString(k)

    /** Lower case with single spaces, for comparing names with widget texts. */
    private fun norm(s: String): String = s.lowercase().replace(Regex("\\s+"), " ").trim()

    /** True if [needle] appears in [hay] as whole words (not inside a longer word or number). */
    private fun hasWords(hay: String, needle: String): Boolean {
        if (needle.isEmpty()) return false
        var from = 0
        while (true) {
            val at = hay.indexOf(needle, from)
            if (at < 0) return false
            val end = at + needle.length
            val okStart = at == 0 || !hay[at - 1].isLetterOrDigit()
            val okEnd = end == hay.length || !hay[end].isLetterOrDigit()
            if (okStart && okEnd) return true
            from = at + 1
        }
    }

    /**
     * Cars whose brand-less "model variant" (and model alone as a weaker match) appears in the widget's texts, e.g. texts
     * ["ID.7 Pro","212","km","57%"] -> the ID.7 Pro entries. Best matches first, at most 8.
     */
    fun suggest(all: List<EvCar>, texts: List<String>): List<EvCar> {
        val hay = norm(texts.joinToString(" "))
        if (hay.isEmpty()) return emptyList()
        val scored = ArrayList<Pair<EvCar, Int>>()
        for (car in all) {
            val model = norm(car.model)
            val full = norm(car.model + " " + car.variant.orEmpty())
            val score = when {
                // the longer the matched name, the more specific (so "ID.7 Pro S" beats "ID.7 Pro" when both appear)
                !car.variant.isNullOrBlank() && hasWords(hay, full) -> 1000 + full.length
                model.length >= 2 && hasWords(hay, model) -> model.length
                else -> 0
            }
            if (score > 0) scored += car to score
        }
        return scored.sortedWith(compareByDescending<Pair<EvCar, Int>> { it.second }.thenByDescending { it.first.year ?: 0 })
            .take(8).map { it.first }
    }

    /** Stores the car in prefs (EV_CAR, EV_CAR_KWH, EV_CAR_KW, EV_CAR_CURVE). */
    fun select(p: Prefs, car: EvCar) {
        p.put(PreferenceKeys.EV_CAR_KWH, num(car.kwh))
        p.put(PreferenceKeys.EV_CAR_KW, car.maxKw?.let { num(it) } ?: "")
        p.put(PreferenceKeys.EV_CAR_CURVE, car.curve.joinToString(",") { "${it.first}:${num(it.second)}" })
        p.put(PreferenceKeys.EV_CAR, car.label)
    }

    /** Forgets the car (clears those four keys to ""). */
    fun clear(p: Prefs) {
        p.put(PreferenceKeys.EV_CAR, "")
        p.put(PreferenceKeys.EV_CAR_KWH, "")
        p.put(PreferenceKeys.EV_CAR_KW, "")
        p.put(PreferenceKeys.EV_CAR_CURVE, "")
    }

    /** Usable battery of the chosen car in kWh, or null if none. */
    fun batteryKwh(p: Prefs): Double? {
        if (p.s(PreferenceKeys.EV_CAR, "").isBlank()) return null
        return p.s(PreferenceKeys.EV_CAR_KWH, "").toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    /** A number without a needless ".0", locale-independent ("77", "42.2"). */
    private fun num(v: Double): String = if (v == Math.floor(v) && v < 1e9) v.toLong().toString() else v.toString()

    /** The chosen car's curve from prefs, sorted by %; empty if none. */
    private fun storedCurve(p: Prefs): List<Pair<Int, Double>> =
        p.s(PreferenceKeys.EV_CAR_CURVE, "").split(',').mapNotNull { part ->
            val bits = part.split(':')
            if (bits.size != 2) return@mapNotNull null
            val pct = bits[0].trim().toIntOrNull() ?: return@mapNotNull null
            val kw = bits[1].trim().toDoubleOrNull()?.takeIf { it > 0.0 } ?: return@mapNotNull null
            pct to kw
        }.sortedBy { it.first }

    /** The curve's power at [pct], linearly interpolated; flat beyond its first and last points. */
    private fun curveAt(curve: List<Pair<Int, Double>>, pct: Double): Double {
        if (pct <= curve.first().first) return curve.first().second
        for (k in 1 until curve.size) {
            val (p1, k1) = curve[k]
            if (pct <= p1) {
                val (p0, k0) = curve[k - 1]
                if (p1 == p0) return k1
                return k0 + (k1 - k0) * (pct - p0) / (p1 - p0)
            }
        }
        return curve.last().second
    }

    /**
     * Minutes to charge from fromPct to toPct (0..100) with a battery of batteryKwh. Integrates 1-% steps; power at each step =
     * min(the car's curve at that % (linear interpolation; if no curve, the car's max kW; if no car, 50 kW), stationKw (if null,
     * assume 50 kW)). Returns 0 when toPct <= fromPct.
     */
    fun chargeMinutes(p: Prefs, fromPct: Double, toPct: Double, batteryKwh: Double, stationKw: Int?): Int {
        val from = fromPct.coerceIn(0.0, 100.0)
        val to = toPct.coerceIn(0.0, 100.0)
        if (to <= from || batteryKwh <= 0.0) return 0
        val curve = storedCurve(p)
        val hasCar = p.s(PreferenceKeys.EV_CAR, "").isNotBlank()
        val flatKw = if (hasCar) p.s(PreferenceKeys.EV_CAR_KW, "").toDoubleOrNull()?.takeIf { it > 0.0 } ?: DEFAULT_KW else DEFAULT_KW
        val station = stationKw?.takeIf { it > 0 }?.toDouble() ?: DEFAULT_KW
        var hours = 0.0
        var pct = from
        while (pct < to) {
            val next = minOf(Math.floor(pct) + 1.0, to)
            val mid = (pct + next) / 2.0
            val carKw = if (hasCar && curve.isNotEmpty()) curveAt(curve, mid) else flatKw
            val kw = minOf(carKw, station).coerceAtLeast(1.0)
            hours += batteryKwh * (next - pct) / 100.0 / kw
            pct = next
        }
        return Math.round(hours * 60.0).toInt()
    }
}
