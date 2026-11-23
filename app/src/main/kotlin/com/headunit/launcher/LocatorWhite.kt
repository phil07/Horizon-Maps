package com.headunit.launcher

import android.graphics.Bitmap

/**
 * A picture saved with a solid white background (rather than a see-through one) gets its white made see-through: only the white
 * that touches the picture's edge (the background), found by a fill from the border, so white inside the picture (a shirt, a
 * highlight) stays. Pure pixel work, no map state; the map uses it when the switch is on (Settings › Your arrow).
 */
internal fun stripWhiteEdge(src: Bitmap): Bitmap {
    val w = src.width; val h = src.height
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val px = IntArray(w * h)
    out.getPixels(px, 0, w, 0, 0, w, h)
    val seen = BooleanArray(w * h)
    val queue = IntArray(w * h)
    var head = 0; var tail = 0
    fun nearWhite(c: Int): Boolean {
        val r = (c shr 16) and 0xFF; val g = (c shr 8) and 0xFF; val b = c and 0xFF
        return r >= 235 && g >= 235 && b >= 235 && maxOf(r, g, b) - minOf(r, g, b) <= 24
    }
    fun push(i: Int) {
        if (!seen[i] && nearWhite(px[i])) { seen[i] = true; queue[tail++] = i }
    }
    for (x in 0 until w) { push(x); push((h - 1) * w + x) }
    for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
    while (head < tail) {
        val i = queue[head++]
        val x = i % w; val y = i / w
        if (x > 0) push(i - 1)
        if (x < w - 1) push(i + 1)
        if (y > 0) push(i - w)
        if (y < h - 1) push(i + w)
    }
    for (i in px.indices) if (seen[i]) px[i] = px[i] and 0x00FFFFFF
    out.setPixels(px, 0, w, 0, 0, w, h)
    return out
}
