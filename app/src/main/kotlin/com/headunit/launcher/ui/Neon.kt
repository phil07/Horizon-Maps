package com.headunit.launcher.ui

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.Prefs
import com.headunit.launcher.R

/**
 * The neon look (Settings > Map > Design): a dark city at night with glowing lines, cut corners and a sharp, technical typeface. It's
 * only a style: the same screens and settings, with other colours, shapes and map colours. Inspired by the neon-noir look of
 * cyberpunk films and games; no pictures, logos or fonts of any game are used (the typeface is Rajdhani, SIL Open Font License).
 */
object Neon {
    /** The accent colour while the neon look is on, else null (the usual look). */
    var accent by mutableStateOf<Color?>(null)
        private set

    val on: Boolean get() = accent != null

    /** The id of the accent ("yellow", "cyan", "magenta") while the neon look is on, else null. For the map's colours. */
    var accentId by mutableStateOf<String?>(null)
        private set

    fun colorOf(id: String): Color = when (id) {
        "cyan" -> Color(0xFF19F0FF)
        "magenta" -> Color(0xFFFF3CCB)
        else -> Color(0xFFFCEE0A)
    }

    /** Takes the look from the settings (call before the screens that use it). */
    fun sync(prefs: Prefs) {
        val id = if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon") prefs.s(PreferenceKeys.NEON_ACCENT, "yellow").let { if (it in setOf("yellow", "cyan", "magenta")) it else "yellow" } else null
        val c = id?.let { colorOf(it) }
        if (c != accent) accent = c
        if (id != accentId) accentId = id
    }

    /** The neon palette on [c]: near-black blue with the accent for lines, highlights and choices. */
    fun vw(c: Vw, a: Color): Vw = c.copy(
        dark = true,
        bg = Color(0xFF05060B), panel = Color(0xFF0B0E16),
        surface = Color(0xFF0D1018), surface2 = Color(0xFF151A26), tile = Color(0xFF141926),
        text = Color(0xFFEAF4FF), sub = Color(0xFF8C98AE), accent = a, line = a.copy(alpha = 0.42f),
        onBg = Color(0xFFEAF4FF), onBgSub = Color(0xFF8C98AE),
    )

    /** Text on the accent colour: near-black on the bright ones. */
    val onAccent = Color(0xFF05060B)

    // ---- typeface ----
    val font = FontFamily(
        Font(R.font.rajdhani_medium, FontWeight.Normal),
        Font(R.font.rajdhani_medium, FontWeight.Medium),
        Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
        Font(R.font.rajdhani_bold, FontWeight.Bold),
        Font(R.font.rajdhani_bold, FontWeight.ExtraBold),
    )

    /** Material's type scale in the neon typeface (a little larger: the face is narrow and small). */
    fun typography(): Typography {
        val d = Typography()
        fun s(t: androidx.compose.ui.text.TextStyle) = t.copy(fontFamily = font, fontSize = t.fontSize * 1.06f)
        return Typography(
            displayLarge = s(d.displayLarge), displayMedium = s(d.displayMedium), displaySmall = s(d.displaySmall),
            headlineLarge = s(d.headlineLarge), headlineMedium = s(d.headlineMedium), headlineSmall = s(d.headlineSmall),
            titleLarge = s(d.titleLarge), titleMedium = s(d.titleMedium), titleSmall = s(d.titleSmall),
            bodyLarge = s(d.bodyLarge), bodyMedium = s(d.bodyMedium), bodySmall = s(d.bodySmall),
            labelLarge = s(d.labelLarge), labelMedium = s(d.labelMedium), labelSmall = s(d.labelSmall),
        )
    }
}

// The cards, buttons and pills of the app are drawn with [RoundedCornerShape]; in the neon look the same calls give cut corners
// (two long cuts on opposite corners, two short ones), a pill becomes a hexagon.

/** A card's shape: rounded as usual, with cut corners in the neon look. */
fun RoundedCornerShape(size: Dp): Shape {
    if (!Neon.on || size.value < 5f) return androidx.compose.foundation.shape.RoundedCornerShape(size)
    val c = (size.value * 0.75f).coerceIn(5f, 16f).dp
    return CutCornerShape(topStart = c * 0.3f, topEnd = c, bottomEnd = c * 0.3f, bottomStart = c)
}

/** A pill or circle ([percent] of the shorter side): rounded as usual, a hexagon in the neon look. */
fun RoundedCornerShape(percent: Int): Shape {
    if (!Neon.on || percent < 20) return androidx.compose.foundation.shape.RoundedCornerShape(percent)
    return CutCornerShape((percent * 0.6f).toInt().coerceIn(10, 30))
}
