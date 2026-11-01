package com.headunit.launcher.car

import android.content.Context
import androidx.car.app.CarContext
import androidx.car.app.hardware.CarHardwareManager
import androidx.car.app.hardware.common.CarValue
import androidx.car.app.hardware.common.OnCarDataAvailableListener
import androidx.car.app.hardware.info.EnergyLevel
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
    private var listener: OnCarDataAvailableListener<EnergyLevel>? = null
    private var carInfo: androidx.car.app.hardware.info.CarInfo? = null

    fun start(carContext: CarContext, prefs: Prefs) {
        if (listener != null) return
        val granted = try { carContext.checkSelfPermission(PERMISSION) == android.content.pm.PackageManager.PERMISSION_GRANTED } catch (e: Exception) { false }
        if (granted) subscribe(carContext, prefs)
        else try {
            carContext.requestPermissions(listOf(PERMISSION), carContext.mainExecutor) { granted2, _ ->
                if (granted2.contains(PERMISSION)) subscribe(carContext, prefs)
            }
        } catch (e: Exception) { }
    }

    private fun subscribe(carContext: CarContext, prefs: Prefs) {
        try {
            val info = carContext.getCarService(CarHardwareManager::class.java).carInfo
            val l = OnCarDataAvailableListener<EnergyLevel> { data ->
                val v: CarValue<Float> = data.batteryPercent
                if (v.status == CarValue.STATUS_SUCCESS) v.value?.let { pct ->
                    val p = Math.round(pct).coerceIn(0, 100)
                    EvWidget.liveReading(prefs, p)
                }
            }
            info.addEnergyLevelListener(carContext.mainExecutor, l)
            carInfo = info; listener = l
        } catch (e: Exception) { }
    }

    fun stop() {
        try { listener?.let { carInfo?.removeEnergyLevelListener(it) } } catch (e: Exception) { }
        listener = null; carInfo = null
    }
}
