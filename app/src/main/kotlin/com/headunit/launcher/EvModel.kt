package com.headunit.launcher

/**
 * How much battery a stretch of road takes, for the arrival estimate. The plain "km × consumption" was too hopeful: it counted
 * a motorway at 130 the same as a country road at 80, so you arrived with less than shown. Now:
 * - each stretch costs more the faster it is driven (air drag grows with the square of the speed),
 * - the speed is the stretch's posted limit times how fast you actually drive against the limit on this trip,
 * - cold weather adds consumption (heating, a cold battery),
 * - what the car's own battery level shows during the drive corrects it (learned per kind of road, kept between trips),
 * - and 10 % is added on top, so the estimate errs on the safe side.
 * Pure functions, so they can be tested without a car.
 */
object EvModel {
    /** Added on top of every estimate, so you arrive with a little more than shown rather than less. */
    const val BUFFER = 1.10

    // consumption at each speed (km/h), relative to 80 km/h (about where a car's rated, mixed figure sits)
    private val SPEEDS = doubleArrayOf(0.0, 30.0, 50.0, 70.0, 80.0, 100.0, 120.0, 130.0, 150.0, 170.0, 200.0)
    private val FACTORS = doubleArrayOf(1.0, 0.88, 0.85, 0.93, 1.0, 1.15, 1.35, 1.48, 1.80, 2.15, 2.8)

    /** Consumption at [kmh] relative to the rated one. */
    fun speedFactor(kmh: Double): Double {
        val v = kmh.coerceIn(0.0, SPEEDS.last())
        for (i in 1 until SPEEDS.size) if (v <= SPEEDS[i]) {
            val t = (v - SPEEDS[i - 1]) / (SPEEDS[i] - SPEEDS[i - 1])
            return FACTORS[i - 1] + (FACTORS[i] - FACTORS[i - 1]) * t
        }
        return FACTORS.last()
    }

    /** Extra consumption in the cold: none from 15 °C up, rising evenly to a quarter more at −5 °C and below. Null: unknown. */
    fun tempFactor(c: Double?): Double {
        if (c == null || c.isNaN()) return 1.0
        if (c >= 15.0) return 1.0
        return 1.0 + 0.25 * ((15.0 - c) / 20.0).coerceIn(0.0, 1.0)
    }

    /** The kind of road by speed, for what's learned: 0 town (under 60 km/h), 1 country (to 100), 2 motorway. */
    fun bucket(kmh: Double): Int = if (kmh < 60.0) 0 else if (kmh < 100.0) 1 else 2

    /**
     * kWh for the route from [from] to [to] metres. [limits]: the posted limits along it; [fallbackKmh] is used where none is
     * known (the route's own average speed); [style]: how fast you drive against the limit (1.0 = at the limit);
     * [unlimitedKmh]: your speed where there's no limit (an open Autobahn). [learned]: the correction per kind of road.
     */
    fun kwh(from: Double, to: Double, limits: List<LimitSec>, fallbackKmh: Double, style: Double, unlimitedKmh: Double,
            baseKwhPerKm: Double, tempC: Double?, learned: DoubleArray): Double {
        if (to <= from) return 0.0
        var sum = 0.0
        var at = from
        // the stretches with a known limit, in order; the gaps between them at the route's average speed
        for (s in limits.sortedBy { it.off }) {
            if (s.end <= at) continue
            if (s.off >= to) break
            if (s.off > at) { sum += part(min(s.off, to) - at, fallbackKmh, baseKwhPerKm, learned); at = min(s.off, to) }
            val e = min(s.end, to)
            if (e > at) {
                val kmh = if (s.ms <= 0.0 || s.ms * 3.6 > 250.0) unlimitedKmh else s.ms * 3.6 * style
                sum += part(e - at, kmh, baseKwhPerKm, learned); at = e
            }
            if (at >= to) break
        }
        if (at < to) sum += part(to - at, fallbackKmh, baseKwhPerKm, learned)
        return sum * tempFactor(tempC) * BUFFER
    }

    private fun min(a: Double, b: Double) = if (a < b) a else b

    private fun part(m: Double, kmh: Double, base: Double, learned: DoubleArray): Double =
        m / 1000.0 * base * speedFactor(kmh) * (learned.getOrNull(bucket(kmh)) ?: 1.0)

    /** The learned corrections from the settings ("1.05,1.12,1.2"), three numbers; 1.0 each when nothing is learned yet. */
    fun parseLearned(s: String): DoubleArray {
        val a = s.split(',').mapNotNull { it.trim().toDoubleOrNull() }
        return DoubleArray(3) { i -> a.getOrNull(i)?.takeIf { it in 0.6..1.8 } ?: 1.0 }
    }

    fun formatLearned(a: DoubleArray): String = a.joinToString(",") { String.format(java.util.Locale.US, "%.3f", it) }
}
