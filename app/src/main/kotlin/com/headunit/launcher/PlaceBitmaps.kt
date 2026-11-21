package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.headunit.launcher.MapBinding.ChainPin
import com.headunit.launcher.MapBinding.SavedKind

/** The pictures for the places on the map (home, work and starred places, favourite stars, chain stops, car parks): pure drawing; the sizes and label choices are passed in. */

internal fun savedBitmap(k: SavedKind, scale: Float): Bitmap {
    val d = 44f * scale
    val bmp = Bitmap.createBitmap(d.toInt().coerceAtLeast(1), d.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ring = Paint(Paint.ANTI_ALIAS_FLAG)
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.FILL }
    val m = d / 2f
    when (k) {
        SavedKind.HOME -> {
            ring.color = 0xFF2F6FED.toInt(); c.drawCircle(m, m, m - 1f, ring)
            val roof = Path().apply { moveTo(m, d * 0.2f); lineTo(d * 0.76f, d * 0.48f); lineTo(d * 0.24f, d * 0.48f); close() }
            c.drawPath(roof, white)
            c.drawRect(RectF(d * 0.32f, d * 0.46f, d * 0.68f, d * 0.8f), white)
        }
        SavedKind.WORK -> {
            ring.color = 0xFFF5A623.toInt(); c.drawCircle(m, m, m - 1f, ring)
            val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A2A00.toInt() }
            c.drawRoundRect(RectF(d * 0.22f, d * 0.38f, d * 0.78f, d * 0.74f), 3f, 3f, dark)
            c.drawRect(RectF(d * 0.4f, d * 0.26f, d * 0.6f, d * 0.4f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3A2A00.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f * scale })
        }
        SavedKind.STAR -> {
            val star = Path()
            val big = m - 3f; val small = big * 0.45f
            for (i in 0 until 10) {
                val rad = if (i % 2 == 0) big else small
                val a = -Math.PI / 2 + i * Math.PI / 5
                val x = (m + rad * Math.cos(a)).toFloat(); val y = (m + rad * Math.sin(a)).toFloat()
                if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close()
            c.drawPath(star, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD60A.toInt() })
            c.drawPath(star, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B5300.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f; strokeJoin = Paint.Join.ROUND })
        }
    }
    return bmp
}

/** A yellow star with a dark edge; with the name on a white label under it, when names are shown. */
internal fun favBitmap(name: String, names: Boolean): Bitmap {
    val s = 44f
    val label = if (names) name.take(24) else ""
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111418.toInt(); textSize = 26f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val tw = if (label.isEmpty()) 0f else tp.measureText(label)
    val w = maxOf(s, tw + 16f).toInt().coerceAtLeast(1)
    val h = (s + if (label.isEmpty()) 0f else 40f).toInt().coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = w / 2f; val cy = s / 2f; val big = s / 2f - 3f; val small = big * 0.45f
    val star = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) big else small
        val a = -Math.PI / 2 + i * Math.PI / 5
        val x = (cx + rad * Math.cos(a)).toFloat(); val y = (cy + rad * Math.sin(a)).toFloat()
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    c.drawPath(star, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFD60A.toInt() })
    c.drawPath(star, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B5300.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f; strokeJoin = Paint.Join.ROUND })
    if (label.isNotEmpty()) {
        c.drawRoundRect(RectF(0f, s + 2f, w.toFloat(), h.toFloat()), 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xEEFFFFFF.toInt() })
        tp.textAlign = Paint.Align.CENTER
        c.drawText(label, w / 2f, h - 12f, tp)
    }
    return bmp
}

/** A round badge with the logo (or a red pin) and the drive time in a pill beside it, standing on a little stem; bigger when chosen. */
internal fun chainBitmap(p: ChainPin): Bitmap {
    val k = if (p.selected) 1.3f else 1f
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF111418.toInt(); textSize = 34f * k; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val d = 84f * k
    val pillW = tp.measureText(p.label) + 34f * k
    val w = d + pillW - 10f * k
    val h = d + 22f * k
    val bmp = Bitmap.createBitmap(w.toInt() + 6, h.toInt() + 6, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 }
    val accent = if (p.selected) 0xFF1A73E8.toInt() else 0xFFFFFFFF.toInt()
    // the pill with the time
    val pr = RectF(d - 20f * k, d * 0.22f, w, d * 0.78f)
    c.drawRoundRect(RectF(pr.left, pr.top + 3f, pr.right, pr.bottom + 3f), pr.height() / 2, pr.height() / 2, shadow)
    c.drawRoundRect(pr, pr.height() / 2, pr.height() / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent })
    tp.color = if (p.selected) 0xFFFFFFFF.toInt() else 0xFF111418.toInt()
    c.drawText(p.label, pr.left + 22f * k, pr.centerY() - (tp.descent() + tp.ascent()) / 2f, tp)
    // the stem and the round badge
    c.drawRect(d / 2 - 3f * k, d - 6f, d / 2 + 3f * k, h, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF5F6368.toInt() })
    c.drawCircle(d / 2, d / 2 + 3f, d / 2 - 2f, shadow)
    c.drawCircle(d / 2, d / 2, d / 2 - 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (p.selected) 0xFF1A73E8.toInt() else 0xFFDADCE0.toInt() })
    c.drawCircle(d / 2, d / 2, d / 2 - 6f * k, white)
    val logo = p.logo
    if (logo != null) {
        val r = d / 2 - 14f * k
        c.drawBitmap(logo, null, RectF(d / 2 - r, d / 2 - r, d / 2 + r, d / 2 + r), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    } else {
        val pin = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFEA4335.toInt() }
        c.drawCircle(d / 2, d / 2 - 6f * k, 16f * k, pin)
        val path = android.graphics.Path().apply { moveTo(d / 2 - 14f * k, d / 2 - 1f * k); lineTo(d / 2 + 14f * k, d / 2 - 1f * k); lineTo(d / 2, d / 2 + 24f * k); close() }
        c.drawPath(path, pin)
        c.drawCircle(d / 2, d / 2 - 6f * k, 6f * k, white)
    }
    return bmp
}

/** A blue P with the free spaces beside it: green, amber when filling up, red when full, grey without a live count (then the size). */
internal fun garageBitmap(g: Garage): Bitmap {
    val txt = g.badge()
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 34f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    val h = 64f
    val pillW = if (txt.isEmpty()) 0f else maxOf(48f, tp.measureText(txt) + 26f)
    val w = h + (if (pillW > 0) pillW - 8f else 0f)
    val bmp = Bitmap.createBitmap(w.toInt() + 4, h.toInt() + 12, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000 }
    if (pillW > 0) {
        val col = when (g.level()) { 1 -> 0xFF1E9E52; 2 -> 0xFFF59E0B; 3 -> 0xFFDC2626; else -> 0xFF6B7280 }.toInt()
        val r = RectF(h - 14f, 10f, w, h - 6f)
        c.drawRoundRect(RectF(r.left, r.top + 3f, r.right, r.bottom + 3f), 22f, 22f, shadow)
        c.drawRoundRect(r, 22f, 22f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        c.drawRoundRect(RectF(r.left + 3f, r.top + 3f, r.right - 3f, r.bottom - 3f), 19f, 19f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
        tp.textSize = 28f
        c.drawText(txt, r.left + 8f + (r.width() - 8f) / 2f, r.centerY() - (tp.descent() + tp.ascent()) / 2f, tp)
    }
    val sq = RectF(2f, 2f, h - 2f, h - 2f)
    c.drawRoundRect(RectF(sq.left, sq.top + 4f, sq.right, sq.bottom + 4f), 14f, 14f, shadow)
    c.drawRoundRect(sq, 14f, 14f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    c.drawRoundRect(RectF(sq.left + 4f, sq.top + 4f, sq.right - 4f, sq.bottom - 4f), 11f, 11f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2E6FD8.toInt() })
    tp.textSize = 40f
    c.drawText("P", sq.centerX(), sq.centerY() - (tp.descent() + tp.ascent()) / 2f, tp)
    return bmp
}
