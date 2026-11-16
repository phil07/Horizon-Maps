package com.headunit.launcher.ui

import android.os.Build
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The room the camera cutout (punch hole or notch) needs at each edge of the screen: only as far as the hole itself reaches, plus
 * [gap], less the [ownMargin] the cards and buttons keep from the edge anyway. So the search bar and the buttons sit just under the
 * hole (beside it, with the phone held sideways) instead of under a strip as tall as the status bar, which Android's own safe
 * inset keeps free.
 */
@Composable
fun cutoutPadding(gap: Dp = 8.dp, ownMargin: Dp = 16.dp): PaddingValues {
    val density = LocalDensity.current
    val view = LocalView.current
    // read so this is worked out again whenever the insets change (turning the phone)
    val wi = WindowInsets.displayCutout
    val safeL = wi.getLeft(density, LayoutDirection.Ltr); val safeT = wi.getTop(density)
    val safeR = wi.getRight(density, LayoutDirection.Ltr); val safeB = wi.getBottom(density)
    if (safeL == 0 && safeT == 0 && safeR == 0 && safeB == 0) return PaddingValues(0.dp)
    fun dp(px: Int): Dp = (px / density.density).dp
    fun room(edgePx: Int): Dp = if (edgePx <= 0) 0.dp else (dp(edgePx) + gap - ownMargin).coerceAtLeast(0.dp)
    val safe = PaddingValues.Absolute(dp(safeL), dp(safeT), dp(safeR), dp(safeB))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return safe
    val cut = view.rootWindowInsets?.displayCutout ?: return safe
    val w = view.rootView.width; val h = view.rootView.height
    if (w <= 0 || h <= 0 || cut.boundingRects.isEmpty()) return safe
    var l = 0; var t = 0; var r = 0; var b = 0
    for (rc in cut.boundingRects) {
        // each hole belongs to the nearest edge that Android says has a cutout
        val near = listOfNotNull(
            if (safeT > 0) 0 to rc.top else null, if (safeB > 0) 1 to (h - rc.bottom) else null,
            if (safeL > 0) 2 to rc.left else null, if (safeR > 0) 3 to (w - rc.right) else null,
        ).minByOrNull { it.second }?.first ?: continue
        when (near) {
            0 -> t = maxOf(t, rc.bottom)
            1 -> b = maxOf(b, h - rc.top)
            2 -> l = maxOf(l, rc.right)
            else -> r = maxOf(r, w - rc.left)
        }
    }
    return PaddingValues.Absolute(room(l), room(t), room(r), room(b))
}

/**
 * Shrinks the content, drawing and touches alike, just enough to fit the height it is given, so a panel that is taller than a
 * phone held sideways shows whole (its Start button included) instead of running off the bottom. Below [minScale] it stops
 * shrinking and the content scrolls instead (put a verticalScroll after this).
 */
fun Modifier.shrinkToFitHeight(minScale: Float = 0.6f): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val p = measurable.measure(constraints)
        return@layout layout(p.width, p.height) { p.place(0, 0) }
    }
    val maxH = constraints.maxHeight
    val p = measurable.measure(constraints.copy(minHeight = 0, maxHeight = (maxH / minScale).roundToInt()))
    if (p.height <= maxH) return@layout layout(p.width, p.height) { p.place(0, 0) }
    val s = maxH.toFloat() / p.height
    layout(constraints.constrainWidth((p.width * s).roundToInt()), maxH) {
        p.placeWithLayer(0, 0) { scaleX = s; scaleY = s; transformOrigin = TransformOrigin(0f, 0f) }
    }
}
