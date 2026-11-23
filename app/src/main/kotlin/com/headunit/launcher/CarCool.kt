package com.headunit.launcher

import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Settings › Android Auto › Cool running, for phones that get hot on the car screen: 30 frames a second, the flat map with the
 * lighter detail, and no street lamps or falling rain or snow. "auto" turns it on when the phone says it's getting hot (Android 10
 * and later) and off again once it's cool; "always" keeps it on; "off" (the default) never.
 */
object CarCool {
    var on by mutableStateOf(false)
        private set

    /** Reads the setting and, for "auto", the phone's temperature; called every half minute while the car screen shows. */
    fun update(ctx: Context, prefs: Prefs) {
        on = when (prefs.s(PreferenceKeys.CAR_COOL, "off")) {
            "always" -> true
            // on from "moderate" heat, and off only once it's back to none at all, so it doesn't flick on and off at the edge
            "auto" -> thermal(ctx).let { if (on) it >= PowerManager.THERMAL_STATUS_LIGHT else it >= PowerManager.THERMAL_STATUS_MODERATE }
            else -> false
        }
    }

    private fun thermal(ctx: Context): Int = if (android.os.Build.VERSION.SDK_INT >= 29) try {
        (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus
    } catch (e: Exception) { softFail(e); 0 } else 0
}
