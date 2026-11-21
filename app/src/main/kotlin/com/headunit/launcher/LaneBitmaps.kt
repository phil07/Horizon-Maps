package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** The lane arrows and the lane sign pictures (pure bitmap drawing; no map state). */
/** The sign itself: a panel with a white edge, one cell per lane (the ones to use lit), on two posts ([posts] false: just the panel, for the bubble). */
internal fun laneSignBitmap(lanes: List<Pair<List<Pair<String, Boolean>>, Boolean>>, posts: Boolean = true): Bitmap {
    val cw = 64; val ch = 80; val pad = 10; val gap = 4; val post = if (posts) 44 else 0
    val w = pad * 2 + cw * lanes.size + gap * (lanes.size - 1)
    val ph = ch + pad * 2
    val bmp = Bitmap.createBitmap(w, ph + post, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    if (posts) {
        p.color = 0xFF8A949C.toInt()
        c.drawRect(pad + 6f, ph - 4f, pad + 14f, (ph + post).toFloat(), p)
        c.drawRect(w - pad - 14f, ph - 4f, w - pad - 6f, (ph + post).toFloat(), p)
    }
    // panel: dark teal (the turn card's colour) with a white edge
    val panel = android.graphics.RectF(1.5f, 1.5f, w - 1.5f, ph - 1.5f)
    p.color = 0xFF1F5E52.toInt(); c.drawRoundRect(panel, 14f, 14f, p)
    p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = 0xFFFFFFFF.toInt(); c.drawRoundRect(panel, 14f, 14f, p)
    p.style = Paint.Style.FILL
    for ((i, lane) in lanes.withIndex()) {
        val x = pad + i * (cw + gap)
        val cell = android.graphics.RectF(x.toFloat(), pad.toFloat(), (x + cw).toFloat(), (pad + ch).toFloat())
        p.color = if (lane.second) 0xFF3FB596.toInt() else 0xFF173F37.toInt()
        c.drawRoundRect(cell, 9f, 9f, p)
        for ((d, hit) in lane.first.sortedBy { it.second }) c.drawBitmap(laneBitmap(d, hit && lane.second), x.toFloat(), pad.toFloat(), null)
    }
    return bmp
}

internal fun laneBitmap(dir: String, on: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(64, 80, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ang = when (dir) {
        "SLIGHT_LEFT" -> -45f; "LEFT" -> -90f; "SHARP_LEFT" -> -135f; "LEFT_U_TURN" -> -180f
        "SLIGHT_RIGHT" -> 45f; "RIGHT" -> 90f; "SHARP_RIGHT" -> 135f; "RIGHT_U_TURN" -> 180f
        else -> 0f
    }
    fun draw(p: Paint) {
        c.drawLine(32f, 74f, 32f, 44f, p)
        c.save(); c.rotate(ang, 32f, 44f)
        c.drawLine(32f, 44f, 32f, 18f, p)
        c.drawLine(32f, 18f, 22f, 28f, p)
        c.drawLine(32f, 18f, 42f, 28f, p)
        c.restore()
    }
    val out = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 12f; color = 0x66000000.toInt() }
    draw(out)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 8f
        color = if (on) 0xFFFFFFFF.toInt() else 0x99FFFFFF.toInt()
    }
    draw(fill)
    return bmp
}
