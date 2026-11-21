package com.headunit.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.content.ContextCompat

/** The map badges for speed cameras and reports (the camera, the warnings, the queue, roadworks): pictures only, no map state. */

/**
 * The speed camera badge: a solid red disc with a thick white ring, a thin dark rim round the outside and a white camera, so it
 * stands out on any map colour, day or night. Drawn larger than the other badges (it shows bigger on the map).
 */
internal fun cameraBitmap(): Bitmap {
    val n = 136
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = n / 2f
    fun p(col: Int, st: Paint.Style = Paint.Style.FILL, w: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = st; strokeWidth = w }
    // a soft shadow under it
    c.drawCircle(cx, cx + 4f, 60f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66000000; maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL) })
    c.drawCircle(cx, cx, 60f, p(0xFF1A1A1A.toInt()))           // dark rim
    c.drawCircle(cx, cx, 57f, p(0xFFFFFFFF.toInt()))           // white ring
    // the red face, a touch lighter at the top
    c.drawCircle(cx, cx, 46f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, cx - 46f, 0f, cx + 46f, 0xFFFF3B3B.toInt(), 0xFFD0101E.toInt(), android.graphics.Shader.TileMode.CLAMP)
    })
    // white camera: body, viewfinder bump, lens with a red centre
    val white = p(0xFFFFFFFF.toInt())
    c.drawRoundRect(android.graphics.RectF(cx - 27f, cx - 13f, cx + 27f, cx + 21f), 7f, 7f, white)
    c.drawRoundRect(android.graphics.RectF(cx - 11f, cx - 23f, cx + 11f, cx - 11f), 4f, 4f, white)
    c.drawCircle(cx, cx + 4f, 12f, p(0xFFD0101E.toInt()))
    c.drawCircle(cx, cx + 4f, 6.5f, white)
    return bmp
}

/** Round badge with a simple glyph: camera on white with a red ring, amber warning triangle, red no-entry. */
internal fun hazardBitmap(ctx: Context, kind: Int): Bitmap {
    if (kind == 0) return cameraBitmap()
    val n = 96
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = n / 2f
    fun p(col: Int, st: Paint.Style = Paint.Style.FILL, w: Float = 0f) = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = st; strokeWidth = w }
    // a round badge with a bold mark in the middle, for reports
    fun badge(ring: Int, fill: Int, mark: String) {
        c.drawCircle(cx, cx + 2f, 38f, p(0x55000000))
        c.drawCircle(cx, cx, 38f, p(0xFFFFFFFF.toInt()))
        c.drawCircle(cx, cx, 31f, p(fill))
        c.drawCircle(cx, cx, 38f, p(ring, Paint.Style.STROKE, 6f))
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 46f; isFakeBoldText = true; textAlign = Paint.Align.CENTER }
        c.drawText(mark, cx, cx + 16f, tp)
    }
    when (kind) {
        4 -> badge(0xFFB71C1C.toInt(), 0xFFE53935.toInt(), "!")
        5 -> badge(0xFFE65100.toInt(), 0xFFF9A825.toInt(), "!")
        6 -> {
            badge(0xFFE65100.toInt(), 0xFFFB8C00.toInt(), "")
            // a small car
            c.drawRoundRect(android.graphics.RectF(cx - 18f, cx - 4f, cx + 18f, cx + 9f), 5f, 5f, p(0xFFFFFFFF.toInt()))
            c.drawRoundRect(android.graphics.RectF(cx - 11f, cx - 13f, cx + 11f, cx - 2f), 5f, 5f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx - 10f, cx + 11f, 4.5f, p(0xFF37474F.toInt()))
            c.drawCircle(cx + 10f, cx + 11f, 4.5f, p(0xFF37474F.toInt()))
        }
        8 -> {
            badge(0xFFBF360C.toInt(), 0xFFF4511E.toInt(), "")
            // a queue: two small cars one behind the other, the back one a little faded
            val back = p(0xCCFFFFFF.toInt())
            c.drawRoundRect(android.graphics.RectF(cx - 20f, cx - 17f, cx + 2f, cx - 7f), 4f, 4f, back)
            c.drawRoundRect(android.graphics.RectF(cx - 15f, cx - 23f, cx - 3f, cx - 15f), 4f, 4f, back)
            val front = p(0xFFFFFFFF.toInt())
            c.drawRoundRect(android.graphics.RectF(cx - 6f, cx + 1f, cx + 20f, cx + 12f), 4f, 4f, front)
            c.drawRoundRect(android.graphics.RectF(cx, cx - 6f, cx + 14f, cx + 3f), 4f, 4f, front)
            c.drawCircle(cx - 1f, cx + 14f, 3.8f, p(0xFF37474F.toInt()))
            c.drawCircle(cx + 15f, cx + 14f, 3.8f, p(0xFF37474F.toInt()))
        }
        7 -> {
            c.drawCircle(cx, cx + 2f, 38f, p(0x55000000))
            c.drawCircle(cx, cx, 38f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx, 38f, p(0xFF1E66F5.toInt(), Paint.Style.STROKE, 7f))
            c.drawRoundRect(android.graphics.RectF(cx - 19f, cx - 9f, cx + 19f, cx + 15f), 5f, 5f, p(0xFF37474F.toInt()))
            c.drawRoundRect(android.graphics.RectF(cx - 8f, cx - 16f, cx + 8f, cx - 8f), 3f, 3f, p(0xFF37474F.toInt()))
            c.drawCircle(cx, cx + 3f, 8f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx + 3f, 4.5f, p(0xFF37474F.toInt()))
        }
        1 -> {
            // the roadworks sign (Verkehrszeichen 123), in its own colours
            val d = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.ic_roadworks_sign)
            if (d != null) {
                d.setBounds(2, 6, n - 2, n - 6)
                d.draw(c)
                return bmp
            }
            val tri = Path().apply { moveTo(cx, 10f); lineTo(n - 8f, n - 14f); lineTo(8f, n - 14f); close() }
            val rounded = android.graphics.CornerPathEffect(10f)
            c.save(); c.translate(0f, 3f)
            c.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000; pathEffect = rounded })
            c.restore()
            c.drawPath(tri, p(0xFFFFFFFF.toInt()).apply { pathEffect = rounded })
            val inner = Path().apply { moveTo(cx, 22f); lineTo(n - 22f, n - 23f); lineTo(22f, n - 23f); close() }
            c.drawPath(inner, p(0xFFF9A825.toInt()).apply { pathEffect = android.graphics.CornerPathEffect(6f) })
            c.drawRoundRect(android.graphics.RectF(cx - 3.5f, 42f, cx + 3.5f, 62f), 3f, 3f, p(0xFF2B2B2B.toInt()))
            c.drawCircle(cx, 70f, 4.2f, p(0xFF2B2B2B.toInt()))
        }
        2 -> {
            c.drawCircle(cx, cx + 2f, 38f, p(0x55000000))
            c.drawCircle(cx, cx, 38f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx, 30f, p(0xFFE53935.toInt()))
            c.drawRoundRect(android.graphics.RectF(cx - 18f, cx - 5f, cx + 18f, cx + 5f), 4f, 4f, p(0xFFFFFFFF.toInt()))
        }
        3 -> {
            c.drawCircle(cx, cx + 2f, 38f, p(0x55000000))
            c.drawCircle(cx, cx, 38f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx, 38f, p(0xFF1E66F5.toInt(), Paint.Style.STROKE, 7f))
            // shield
            val sh = Path().apply {
                moveTo(cx - 14f, cx - 15f); lineTo(cx + 14f, cx - 15f); lineTo(cx + 14f, cx + 2f)
                quadTo(cx + 14f, cx + 14f, cx, cx + 20f); quadTo(cx - 14f, cx + 14f, cx - 14f, cx + 2f); close()
            }
            c.drawPath(sh, p(0xFF1E66F5.toInt()))
            c.drawCircle(cx, cx - 1f, 5f, p(0xFFFFFFFF.toInt()))
        }
        else -> {
            c.drawCircle(cx, cx + 2f, 38f, p(0x55000000))
            c.drawCircle(cx, cx, 38f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx, 38f, p(0xFFE53935.toInt(), Paint.Style.STROKE, 7f))
            // camera glyph: body, viewfinder bump and lens
            c.drawRoundRect(android.graphics.RectF(cx - 19f, cx - 9f, cx + 19f, cx + 15f), 5f, 5f, p(0xFF37474F.toInt()))
            c.drawRoundRect(android.graphics.RectF(cx - 8f, cx - 16f, cx + 8f, cx - 8f), 3f, 3f, p(0xFF37474F.toInt()))
            c.drawCircle(cx, cx + 3f, 8f, p(0xFFFFFFFF.toInt()))
            c.drawCircle(cx, cx + 3f, 4.5f, p(0xFF37474F.toInt()))
        }
    }
    return bmp
}
