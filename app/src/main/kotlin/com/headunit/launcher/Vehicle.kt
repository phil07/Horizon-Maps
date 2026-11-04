package com.headunit.launcher

/**
 * What you're driving (Settings > Fahrzeug): a car, a motorhome or a car with a trailer. A motorhome or trailer has its own top
 * speed (for the arrival time and the speed warning) and its size and weight (so TomTom and Mapbox route round low bridges and
 * weight limits where their road data knows them).
 */
data class VehicleProfile(val kind: String, val maxKmh: Int, val heightM: Double, val widthM: Double, val lengthM: Double, val weightKg: Int, val axleWeightKg: Int = 0) {
    val isCar get() = kind == "car"
    /** The top speed in m/s, or null when there's none (a car). */
    val maxMs: Double? get() = if (isCar || maxKmh <= 0) null else maxKmh / 3.6
}

object Vehicle {
    /** The profile in use, set before each route is asked for. */
    @Volatile var active: VehicleProfile = VehicleProfile("car", 0, 0.0, 0.0, 0.0, 0)

    /** The settings' defaults for each kind: a motorhome (up to 100 km/h) and a car with a caravan (80 km/h in Germany). */
    fun defaults(kind: String): VehicleProfile = when (kind) {
        "camper" -> VehicleProfile("camper", 100, 3.0, 2.3, 7.0, 3500, 3500)
        "trailer" -> VehicleProfile("trailer", 80, 2.6, 2.3, 12.0, 3000, 3000)
        else -> VehicleProfile("car", 0, 0.0, 0.0, 0.0, 0, 0)
    }

    /** The settings key for one of a kind's values ("max", "h", "w", "l", "kg"). */
    fun key(kind: String, what: String) = "veh_${kind}_$what"

    fun from(prefs: Prefs): VehicleProfile {
        val kind = prefs.s(PreferenceKeys.VEHICLE, "car").let { if (it in setOf("car", "camper", "trailer")) it else "car" }
        val d = defaults(kind)
        if (d.isCar) return d
        fun num(w: String, def: Double) = prefs.s(key(kind, w), "").replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 } ?: def
        return VehicleProfile(kind, num("max", d.maxKmh.toDouble()).toInt().coerceIn(30, 200), num("h", d.heightM).coerceIn(1.0, 5.0),
            num("w", d.widthM).coerceIn(1.0, 3.0), num("l", d.lengthM).coerceIn(2.0, 25.0), num("kg", d.weightKg.toDouble()).toInt().coerceIn(500, 40000),
            num("axlekg", d.axleWeightKg.toDouble()).toInt().coerceIn(500, 25000))
    }

    /** TomTom's routing parameters for the profile (nothing for a car). */
    fun tomtomParams(v: VehicleProfile = active): String = if (v.isCar) "" else String.format(java.util.Locale.US,
        "&vehicleMaxSpeed=%d&vehicleHeight=%.2f&vehicleWidth=%.2f&vehicleLength=%.2f&vehicleWeight=%d", v.maxKmh, v.heightM, v.widthM, v.lengthM, v.weightKg)

    /** Mapbox's routing parameters for the profile: height and width in metres, weight in tonnes (nothing for a car). */
    fun mapboxParams(v: VehicleProfile = active): String = if (v.isCar) "" else String.format(java.util.Locale.US,
        "&max_height=%.1f&max_width=%.1f&max_weight=%.1f", v.heightM.coerceAtMost(10.0), v.widthM.coerceAtMost(10.0), (v.weightKg / 1000.0).coerceAtMost(100.0))

    /**
     * Seconds the rest of the route takes longer at the profile's top speed: every stretch ahead whose limit is above it (an
     * unrestricted motorway counts as 130 km/h) is driven at the top speed instead.
     */
    fun extraSecs(limits: List<LimitSec>, progress: Double, maxMs: Double?): Double {
        if (maxMs == null || maxMs <= 0) return 0.0
        var extra = 0.0
        for (l in limits) {
            val from = maxOf(l.off, progress)
            if (l.end <= from) continue
            val ms = if (l.ms <= 0) 130 / 3.6 else l.ms
            if (ms > maxMs) extra += (l.end - from) * (1 / maxMs - 1 / ms)
        }
        return extra
    }
}
