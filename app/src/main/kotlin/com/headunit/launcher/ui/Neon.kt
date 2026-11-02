package com.headunit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.headunit.launcher.NavEngine
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

    /** The game-like red for what isn't selected (the tabs), and the cyan of its roads and selections. */
    val red = Color(0xFFFF3B4E)
    val cyan = Color(0xFF19F0FF)

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

// ---- the game-like details: tabs, selection brackets, the grid over the map, the place box ----

/** Four corner brackets round a box (a selection frame), drawn in [color]. */
fun Modifier.neonBrackets(color: Color, len: Dp = 10.dp, width: Dp = 1.5.dp): Modifier = drawBehind {
    val l = len.toPx(); val w = width.toPx(); val x = size.width; val y = size.height
    fun ln(a: Offset, b: Offset) = drawLine(color, a, b, w)
    ln(Offset(0f, l), Offset(0f, 0f)); ln(Offset(0f, 0f), Offset(l, 0f))
    ln(Offset(x - l, 0f), Offset(x, 0f)); ln(Offset(x, 0f), Offset(x, l))
    ln(Offset(0f, y - l), Offset(0f, y)); ln(Offset(0f, y), Offset(l, y))
    ln(Offset(x - l, y), Offset(x, y)); ln(Offset(x, y), Offset(x, y - l))
}

/**
 * The top bar of a page in the neon look: the titles in capitals in a row (scrolling when there are many), the open one in the accent
 * colour with a line under it, the others in red, and short brackets at both ends of the bar.
 */
@Composable
fun NeonTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val a = Neon.accent ?: return
    val scroll = rememberScrollState()
    Box(modifier.fillMaxWidth().padding(bottom = 8.dp).neonBrackets(a.copy(alpha = 0.8f), 8.dp)) {
        Row(Modifier.horizontalScroll(scroll).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            titles.forEachIndexed { i, t ->
                val on = i == selected
                Text(t.uppercase(), color = if (on) a else Neon.red, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.2.sp, maxLines = 1, overflow = TextOverflow.Clip,
                    modifier = Modifier.clickable { onSelect(i) }.drawBehind {
                        val y = size.height - 1.dp.toPx()
                        drawLine(if (on) a else Neon.red.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), if (on) 2.dp.toPx() else 1.dp.toPx())
                    }.padding(vertical = 10.dp))
            }
        }
    }
}

/** A faint dashed grid over the map, as on the game's map screen. */
fun Modifier.neonGrid(color: Color): Modifier = drawBehind {
    val step = 56.dp.toPx(); val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))
    var x = step / 2
    while (x < size.width) { drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f, pathEffect = dash); x += step }
    var y = step / 2
    while (y < size.height) { drawLine(color, Offset(0f, y), Offset(size.width, y), 1f, pathEffect = dash); y += step }
}

/** The small box with the time and the road you are on (bottom right of the map), like the game's place box. */
@Composable
fun NeonPlaceBox(engine: NavEngine, modifier: Modifier = Modifier) {
    val a = Neon.accent ?: return
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); kotlinx.coroutines.delay(15_000) } }
    val time = remember(now) { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(now)) }
    val road = engine.roadNow
    Column(
        modifier.padding(top = 6.dp).background(Color(0xCC05060B), CutCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomEnd = 4.dp, bottomStart = 12.dp))
            .border(1.dp, a.copy(alpha = 0.5f), CutCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomEnd = 4.dp, bottomStart = 12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text(time, color = a, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        if (road.isNotBlank()) Text(road.uppercase(), color = Neon.red, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = androidx.compose.ui.Modifier.padding(top = 1.dp))
    }
}
