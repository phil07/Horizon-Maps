package com.headunit.launcher

/**
 * How much battery a stretch of road takes, for the arrival estimate. The plain "km × consumption" was too hopeful: it counted
 * a motorway at 130 the same as a country road at 80, so you arrived with less than shown. Now:
 * - each stretch costs more the faster it is driven (air drag grows with the square of the speed),
 * - the speed is the stretch's posted limit times how fast you actually drive against the limit on this trip,
 * - cold weather adds consumption (heating, a cold battery),
 * - hills: climbing costs, going down gives some back (how much is learned from the battery level),
 * - wind against you, a wet or snowy road,
 * - what the car's own battery level shows during the drive corrects it (learned per kind of road, kept between trips),
 * - and 10 % is added on top, so the estimate errs on the safe side.
 * Pure functions, so they can be tested without a car.
 */
object EvModel {
    /** Added on top of every estimate, so you arrive with a little more than shown rather than less. */
    const val BUFFER = 1.10

    // consumption at each speed (km/h), relative to 80 km/h (about where a car's rated, mixed figure sits)
    private val SPEEDS = doubleArrayOf(0.0, 30.0, 50.0, 70.0, 80.0, 100.0, 120.0, 130.0, 150.0, 170.0, 200.0, 230.0, 250.0)
    private val FACTORS = doubleArrayOf(1.0, 0.88, 0.85, 0.93, 1.0, 1.15, 1.35, 1.48, 1.80, 2.15, 2.8, 3.5, 4.1)

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

    /** A stretch of road's weather for the estimate: where it applies (metres along the route), the wind and whether it's wet. */
    class Wx(val off: Double, val windKmh: Double, val windFrom: Double, val wet: Boolean, val snow: Boolean)

    /**
     * What the road and the weather add: the heights along the route ([elevOff] metres → [elevH] metres above sea level), the car's
     * weight, how much of the energy downhill comes back ([regen], learned), the weather along the way and the road's direction at
     * a point (for head- and tailwind).
     */
    class Env(
        val elevOff: DoubleArray = DoubleArray(0), val elevH: DoubleArray = DoubleArray(0),
        val massKg: Double = 2000.0, val regen: Double = DEFAULT_REGEN,
        val wx: List<Wx> = emptyList(), val bearing: (Double) -> Double? = { null },
    ) {
        fun heightAt(off: Double): Double? {
            val n = elevOff.size
            if (n == 0 || n != elevH.size) return null
            if (off <= elevOff[0]) return elevH[0]
            if (off >= elevOff[n - 1]) return elevH[n - 1]
            var lo = 0; var hi = n - 1
            while (hi - lo > 1) { val m = (lo + hi) / 2; if (elevOff[m] <= off) lo = m else hi = m }
            val span = elevOff[hi] - elevOff[lo]
            return if (span <= 0.0) elevH[lo] else elevH[lo] + (elevH[hi] - elevH[lo]) * (off - elevOff[lo]) / span
        }

        fun wxAt(off: Double): Wx? = wx.minByOrNull { kotlin.math.abs(it.off - off) }

        /** kWh for climbing [dh] metres (positive) or what comes back going down (negative). */
        fun gradeKwh(dh: Double): Double = gradeKwh(dh, massKg, regen)

        companion object { val NONE = Env() }
    }

    /** Of the energy for going down, the share that comes back through the brakes, before anything is learned. */
    const val DEFAULT_REGEN = 0.6
    /** Of the battery's energy, the share that turns the wheels (motor, inverter). */
    private const val DRIVE_EFF = 0.88

    /** kWh to climb [dh] metres (from the battery, through the motor) or, going down, the kWh that comes back (negative). */
    fun gradeKwh(dh: Double, massKg: Double, regen: Double): Double {
        val j = massKg * 9.81 * dh
        return if (dh >= 0) j / DRIVE_EFF / 3.6e6 else j * regen.coerceIn(0.0, 0.95) / 3.6e6
    }

    /** The weight of a car with [batteryKwh], with two people and some luggage, when the car's own weight isn't known. */
    fun massFor(batteryKwh: Double): Double = (1450.0 + batteryKwh.coerceIn(20.0, 120.0) * 8.0 + 150.0)

    /**
     * Extra (or less) use from the wind and a wet or snowy road at [kmh]. Wind against you costs much more at speed (air drag
     * grows with the square of the speed through the air); wind from behind helps, but only half of it is counted (on the safe
     * side). The wind at 10 m height is stronger than at a car's: 70 % of it.
     */
    fun wxFactor(w: Wx?, kmh: Double, heading: Double?): Double {
        if (w == null) return 1.0
        var f = 1.0
        if (heading != null && w.windKmh > 3.0 && kmh > 20.0) {
            val rel = Math.toRadians(w.windFrom - heading)
            val head = w.windKmh * 0.7 * Math.cos(rel)             // > 0: against you
            val air = kmh + head
            val share = kmh * kmh / (kmh * kmh + 70.0 * 70.0)       // how much of the use is air drag at this speed
            var g = 1.0 + share * ((air * kotlin.math.abs(air)) / (kmh * kmh) - 1.0)
            if (g < 1.0) g = 1.0 - (1.0 - g) * 0.5
            f *= g.coerceIn(0.85, 1.6)
        }
        if (w.snow) f *= 1.18 else if (w.wet) f *= 1.07
        return f
    }

    /**
     * kWh for the route from [from] to [to] metres. [limits]: the posted limits along it; [fallbackKmh] is used where none is
     * known (the route's own average speed); [style]: how fast you drive against the limit (1.0 = at the limit);
     * [unlimitedKmh]: your speed where there's no limit (an open Autobahn). [learned]: the correction per kind of road.
     * [env]: hills and weather (none: a flat road in calm weather).
     */
    fun kwh(from: Double, to: Double, limits: List<LimitSec>, fallbackKmh: Double, style: Double, unlimitedKmh: Double,
            baseKwhPerKm: Double, tempC: Double?, learned: DoubleArray, env: Env = Env.NONE): Double {
        if (to <= from) return 0.0
        val secs = limits.sortedBy { it.off }
        // the stretch is walked in pieces of at most 500 m, cut where a limit starts or ends
        val cuts = (secs.flatMap { listOf(it.off, it.end) }.filter { it > from && it < to }).sorted()
        var ci = 0; var si = 0
        var flat = 0.0; var grade = 0.0
        var at = from
        var prevH = env.heightAt(from)
        while (at < to - 1e-6) {
            while (ci < cuts.size && cuts[ci] <= at + 1e-6) ci++
            val b = minOf(at + STEP, to, cuts.getOrNull(ci) ?: to)
            val mid = (at + b) / 2
            while (si < secs.size && secs[si].end <= mid) si++
            val s = secs.getOrNull(si)?.takeIf { it.off <= mid }
            val kmh = when {
                s == null -> fallbackKmh
                s.ms <= 0.0 || s.ms * 3.6 > 250.0 -> unlimitedKmh
                else -> s.ms * 3.6 * style
            }
            flat += part(b - at, kmh, baseKwhPerKm, learned) * (if (env.wx.isEmpty()) 1.0 else wxFactor(env.wxAt(mid), kmh, env.bearing(mid)))
            val h = env.heightAt(b)
            if (h != null && prevH != null) grade += env.gradeKwh(h - prevH)
            prevH = h
            at = b
        }
        return maxOf(0.0, flat * tempFactor(tempC) + grade) * BUFFER
    }

    private const val STEP = 500.0

    private fun part(m: Double, kmh: Double, base: Double, learned: DoubleArray): Double =
        m / 1000.0 * base * speedFactor(kmh) * (learned.getOrNull(bucket(kmh)) ?: 1.0)

    // ---- charging in the cold ----
    /**
     * How much of its usual power the car charges with at [tempC]: all of it from 10 °C up, down to 60 % at −10 °C and below. A battery
     * warmed by a long fast drive just before ([warm]) loses only half as much.
     */
    fun chargeTempFactor(tempC: Double?, warm: Boolean): Double {
        if (tempC == null || tempC.isNaN() || tempC >= 10.0) return 1.0
        val loss = 0.4 * ((10.0 - tempC) / 20.0).coerceIn(0.0, 1.0)
        return 1.0 - (if (warm) loss / 2 else loss)
    }

    // ---- a petrol or diesel car ----
    // fuel use at each speed (km/h), relative to 80 km/h: towns cost more (stop and go), fast motorways much more
    private val F_SPEEDS = doubleArrayOf(0.0, 30.0, 50.0, 70.0, 80.0, 100.0, 120.0, 130.0, 150.0, 180.0, 200.0, 250.0)
    private val F_FACTORS = doubleArrayOf(1.35, 1.3, 1.12, 1.0, 1.0, 1.08, 1.22, 1.32, 1.55, 1.95, 2.25, 3.0)

    /** Fuel use at [kmh] relative to the car's mixed figure. */
    fun fuelSpeedFactor(kmh: Double): Double {
        val v = kmh.coerceIn(0.0, F_SPEEDS.last())
        for (i in 1 until F_SPEEDS.size) if (v <= F_SPEEDS[i]) {
            val t = (v - F_SPEEDS[i - 1]) / (F_SPEEDS[i] - F_SPEEDS[i - 1])
            return F_FACTORS[i - 1] + (F_FACTORS[i] - F_FACTORS[i - 1]) * t
        }
        return F_FACTORS.last()
    }

    /**
     * Litres for the route from [from] to [to] metres at [lPer100] (the car's mixed figure), by the limits and your style, with
     * the same 10 % margin.
     */
    fun litres(from: Double, to: Double, limits: List<LimitSec>, fallbackKmh: Double, style: Double, unlimitedKmh: Double, lPer100: Double): Double {
        if (to <= from) return 0.0
        val secs = limits.sortedBy { it.off }
        var sum = 0.0; var at = from
        for (s in secs) {
            if (s.end <= at) continue
            if (s.off >= to) break
            if (s.off > at) { val e = minOf(s.off, to); sum += (e - at) / 100_000.0 * lPer100 * fuelSpeedFactor(fallbackKmh); at = e }
            val e = minOf(s.end, to)
            if (e > at) {
                val kmh = if (s.ms <= 0.0 || s.ms * 3.6 > 250.0) unlimitedKmh else s.ms * 3.6 * style
                sum += (e - at) / 100_000.0 * lPer100 * fuelSpeedFactor(kmh); at = e
            }
            if (at >= to) break
        }
        if (at < to) sum += (to - at) / 100_000.0 * lPer100 * fuelSpeedFactor(fallbackKmh)
        return sum * BUFFER
    }

    /** The learned corrections from the settings ("1.05,1.12,1.2"), three numbers; 1.0 each when nothing is learned yet. */
    fun parseLearned(s: String): DoubleArray {
        val a = s.split(',').mapNotNull { it.trim().toDoubleOrNull() }
        return DoubleArray(3) { i -> a.getOrNull(i)?.takeIf { it in 0.6..1.8 } ?: 1.0 }
    }

    fun formatLearned(a: DoubleArray): String = a.joinToString(",") { String.format(java.util.Locale.US, "%.3f", it) }
}
