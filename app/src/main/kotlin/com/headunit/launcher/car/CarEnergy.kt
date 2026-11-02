package com.headunit.launcher.car

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
import androidx.car.app.hardware.info.Speed
import com.headunit.launcher.CarData
import com.headunit.launcher.EvWidget
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.Prefs

/**
 * The battery level straight from the car, through Android Auto's car hardware API. Live, and without the car maker's
 * server. Android Auto asks the driver once for permission to share the car's energy data. Used in preference to the
 * widget: while a live value is fresh, [EvWidget] doesn't overwrite it.
 */
object CarEnergy {
    private const val PERMISSION = "com.google.android.gms.permission.CAR_FUEL"
    private const val SPEED_PERMISSION = "com.google.android.gms.permission.CAR_SPEED"
    private var listener: OnCarDataAvailableListener<EnergyLevel>? = null
    private var speedListener: OnCarDataAvailableListener<Speed>? = null
    private var carInfo: androidx.car.app.hardware.info.CarInfo? = null

    fun start(carContext: CarContext, prefs: Prefs) {
        if (listener != null || speedListener != null) return
        fun has(p: String) = try { carContext.checkSelfPermission(p) == android.content.pm.PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
        val fuel = has(PERMISSION); val speed = has(SPEED_PERMISSION)
        if (fuel) subscribe(carContext, prefs)
        if (speed) subscribeSpeed(carContext)
        // the ones not yet allowed are asked for together (one question from Android Auto)
        val ask = listOfNotNull(PERMISSION.takeIf { !fuel }, SPEED_PERMISSION.takeIf { !speed })
        if (ask.isNotEmpty()) try {
            carContext.requestPermissions(ask, carContext.mainExecutor) { granted2, _ ->
                if (granted2.contains(PERMISSION)) subscribe(carContext, prefs)
                if (granted2.contains(SPEED_PERMISSION)) subscribeSpeed(carContext)
            }
        } catch (e: Exception) { }
    }

    /** The car's own speed (wheel speed), for the speedometer and for the position in a tunnel. */
    private fun subscribeSpeed(carContext: CarContext) {
        if (speedListener != null) return
        try {
            val info = carContext.getCarService(CarHardwareManager::class.java).carInfo
            val l = OnCarDataAvailableListener<Speed> { data ->
                val raw: CarValue<Float> = data.rawSpeedMetersPerSecond
                val v = if (raw.status == CarValue.STATUS_SUCCESS) raw.value else {
                    val d: CarValue<Float> = data.displaySpeedMetersPerSecond
                    if (d.status == CarValue.STATUS_SUCCESS) d.value else null
                }
                if (v != null) CarData.speed(Math.abs(v.toDouble()))
            }
            info.addSpeedListener(carContext.mainExecutor, l)
            carInfo = info; speedListener = l
        } catch (e: Exception) { }
    }

    private fun subscribe(carContext: CarContext, prefs: Prefs) {
        try {
            val info = carContext.getCarService(CarHardwareManager::class.java).carInfo
            val l = OnCarDataAvailableListener<EnergyLevel> { data ->
                val v: CarValue<Float> = data.batteryPercent
                // a petrol or diesel car (Settings): the tank's fill level and the car's range, for the tank range
                if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") != "EV") {
                    val f: CarValue<Float> = data.fuelPercent
                    val r: CarValue<Float> = data.rangeRemainingMeters
                    val pct = if (f.status == CarValue.STATUS_SUCCESS) f.value?.let { Math.round(it).coerceIn(0, 100) } else null
                    val km = if (r.status == CarValue.STATUS_SUCCESS) r.value?.let { m -> Math.round(m / 1000f) }?.takeIf { it >= 1 } else null
                    if (pct != null || km != null) {
                        prefs.put(PreferenceKeys.FUEL_NOW_PCT, pct?.toString() ?: "")
                        prefs.put(PreferenceKeys.FUEL_NOW_KM, km?.toString() ?: "")
                        prefs.put(PreferenceKeys.FUEL_NOW_AT, System.currentTimeMillis().toString())
                    }
                    return@OnCarDataAvailableListener
                }
                if (v.status == CarValue.STATUS_SUCCESS) v.value?.let { pct ->
                    val p = Math.round(pct).coerceIn(0, 100)
                    // the range the car reports, in km (null when the car doesn't send one)
                    val r: CarValue<Float> = data.rangeRemainingMeters
                    val km = if (r.status == CarValue.STATUS_SUCCESS) r.value?.let { m -> Math.round(m / 1000f) }?.takeIf { it >= 5 } else null
                    EvWidget.liveReading(prefs, p, km)
                }
            }
            info.addEnergyLevelListener(carContext.mainExecutor, l)
            carInfo = info; listener = l
        } catch (e: Exception) { }
    }

    fun stop() {
        try { listener?.let { carInfo?.removeEnergyLevelListener(it) } } catch (e: Exception) { }
        try { speedListener?.let { carInfo?.removeSpeedListener(it) } } catch (e: Exception) { }
        listener = null; speedListener = null; carInfo = null
    }
}
