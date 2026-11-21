package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** The map pictures for a queue (the minutes) and a charge point (the power and the free count): pure drawing, no map state. */

internal fun queueBitmap(q: QueueMark): Bitmap {
    val bmp = Bitmap.createBitmap(132, 48, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val a = if (q.dim) 115 else 255
    val col = if (q.heavy) 0xFFD93025.toInt() else 0xFFF9AB00.toInt()
    c.drawRoundRect(android.graphics.RectF(0f, 0f, 132f, 48f), 24f, 24f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; alpha = a })
    c.drawText("+${q.min} min", 66f, 33f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); alpha = a; textSize = 26f; textAlign = Paint.Align.CENTER; typeface = android.graphics.Typeface.DEFAULT_BOLD })
    return bmp
}

/** A green pin with the free and total count inside, and the power in a dark box above it. */
internal fun chargerBitmap(kwLabel: String, countLabel: String): Bitmap {
    val w = 200; val h = 168
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = w / 2f; val r = 46f; val cy = 58f + r
    val tail = Path().apply {
        moveTo(cx - r * 0.55f, cy + r * 0.6f); lineTo(cx, cy + r + 12f); lineTo(cx + r * 0.55f, cy + r * 0.6f); close()
    }
    val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF34A853.toInt() }
    c.drawPath(tail, green)
    c.drawCircle(cx, cy, r, green)
    c.drawCircle(cx, cy, r, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f })
    if (countLabel.isNotEmpty()) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = if (countLabel.length > 4) 24f else 32f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        c.drawText(countLabel, cx, cy + 11f, tp)
    }
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 28f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    val half = tp.measureText(kwLabel) / 2f + 12f
    c.drawRoundRect(android.graphics.RectF(cx - half, 4f, cx + half, 52f), 12f, 12f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE6202124.toInt() })
    c.drawText(kwLabel, cx, 40f, tp)
    return bmp
}
