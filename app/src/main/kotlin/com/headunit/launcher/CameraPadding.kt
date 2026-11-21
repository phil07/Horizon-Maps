package com.headunit.launcher

import android.graphics.Rect
import com.mapbox.maps.EdgeInsets
import kotlin.math.max
import kotlin.math.min

/**
 * Where the camera's middle goes on the map: the padding that puts the locator at a given place within the part of the view that
 * can be seen (on Android Auto, the app's own cards cover some of it). Only geometry: the map keeps its state (the inset, the top
 * limit, and the eased cover, which is passed in as [cover]).
 */
internal object CameraPadding {
    /**
     * Padding (left, top, right, bottom) in pixels for a view of [w] x [h]: the locator at [topF] from the top and [leftF] across
     * (a negative value pads from the right instead, the mirrored layout). [inset] is the part of the view that can be seen, or null
     * for the whole view; [cover] is asked only when there is an inset.
     */
    fun pads(w: Double, h: Double, inset: Rect?, maxTopFrac: Double, topF: Double, leftF: Double, cover: () -> Double): DoubleArray {
        val tf = min(topF, maxTopFrac)
        val i = inset ?: return doubleArrayOf(w * max(0.0, leftF), h * tf, w * max(0.0, -leftF), 0.0)
        val bot = i.bottom + min(cover(), (h - i.top - i.bottom) * 0.55)
        val vw = max(1.0, w - i.left - i.right); val vh = max(1.0, h - i.top - bot)
        return doubleArrayOf(i.left + vw * max(0.0, leftF), i.top + vh * tf, i.right + vw * max(0.0, -leftF), bot)
    }

    /** Mapbox's padding is (top, left, bottom, right); the app's is (left, top, right, bottom). Kept inside the view so the map never refuses it. */
    fun edge(pd: DoubleArray, w: Double, h: Double): EdgeInsets {
        val ww = max(1.0, w); val hh = max(1.0, h)
        val l = pd[0].coerceIn(0.0, ww * 0.95); val r = pd[2].coerceIn(0.0, max(0.0, ww * 0.95 - l))
        val t = pd[1].coerceIn(0.0, hh * 0.95); val b = pd[3].coerceIn(0.0, max(0.0, hh * 0.95 - t))
        return EdgeInsets(t, l, b, r)
    }
}
