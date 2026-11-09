package com.headunit.launcher.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.headunit.launcher.Prefs
import com.headunit.launcher.PreferenceKeys

/**
 * Own design (Settings > Map > Own design): an accent colour for the app and a lighter or darker map. Empty or 0 = the usual
 * look. The route and arrow colours are set on the map itself. The neon look keeps its own colours.
 */
object CustomLook {
    var accent by mutableStateOf<Color?>(null)
        private set
    /** -50 (darker) .. +50 (lighter), in percent of a black or white wash over the map. */
    var mapLight by mutableStateOf(0)
        private set

    fun parse(hex: String): Color? = if (hex.isBlank()) null else try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { null }

    fun sync(prefs: Prefs) {
        val a = parse(prefs.s(PreferenceKeys.ACCENT_COLOUR, ""))
        if (a != accent) accent = a
        val l = (prefs.s(PreferenceKeys.MAP_LIGHT, "0").toFloatOrNull() ?: 0f).toInt().coerceIn(-50, 50)
        if (l != mapLight) mapLight = l
    }

    fun wash(l: Int): Color = when {
        l > 0 -> Color.White.copy(alpha = l / 100f)
        l < 0 -> Color.Black.copy(alpha = -l / 100f)
        else -> Color.Transparent
    }

    /** [v] with the own accent and map wash (nothing changes in the neon look). */
    fun apply(v: Vw): Vw {
        if (Neon.on) return v
        val a = accent
        val w = wash(mapLight)
        return if (a == null && w == Color.Transparent) v else v.copy(accent = a ?: v.accent, mapTint = if (w == Color.Transparent) v.mapTint else w)
    }
}
