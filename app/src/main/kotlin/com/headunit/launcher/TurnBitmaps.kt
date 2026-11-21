package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** The pictures for the turn arrow's head, the chevrons along the route and the road name pill: pure drawing, no map state. */

/** A white chevron pointing along the line (to the right in the picture, which is "along the line" for line placement). */
internal fun chevronBitmap(): Bitmap {
    val b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 10f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    c.drawPath(Path().apply { moveTo(20f, 12f); lineTo(42f, 32f); lineTo(20f, 52f) }, p)
    return b
}

/**
 * The head of the turn arrow, pointing up, with its flat base along the bottom edge so it joins the shaft as one piece:
 * white with the same dark edge as the shaft, slightly rounded. Drawn at 1:1 map pixels (200 wide).
 */
internal fun turnHeadBitmap(day: Boolean): Bitmap {
    // the triangle is 196 wide inside a 220 wide picture, leaving room round it for the soft halo; its base is the bottom edge
    val b = Bitmap.createBitmap(220, 162, Bitmap.Config.ARGB_8888)
    b.density = android.util.DisplayMetrics.DENSITY_DEFAULT
    val c = Canvas(b)
    val ox = 12f; val top = 12f; val bot = 162f
    // the dark edge matches the shaft's: the shaft's edge is 0.1 of a route width each side, and the head is 3.07 route widths across
    val edgeW = 196f / 3.072f * 0.1f * 1.4f
    val outer = Path().apply { moveTo(ox + 98f, top); lineTo(ox + 196f, bot); lineTo(ox, bot); close() }
    if (day) c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x5916467A; style = Paint.Style.STROKE; strokeWidth = 12f; strokeJoin = Paint.Join.ROUND
        pathEffect = android.graphics.CornerPathEffect(30f); maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    })
    c.drawPath(outer, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF16467A.toInt(); pathEffect = android.graphics.CornerPathEffect(30f) })
    val inset = edgeW
    val inner = Path().apply { moveTo(ox + 98f, top + inset * 1.8f); lineTo(ox + 196f - inset * 1.7f, bot); lineTo(ox + inset * 1.7f, bot); close() }
    c.drawPath(inner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); pathEffect = android.graphics.CornerPathEffect(24f) })
    return b
}

/** The road you're on, in a Google-style pill: deep blue, fully rounded, bold white text, a soft shadow. Drawn at 2x (shown at half). */
internal fun roadBitmap(name: String): Bitmap {
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 40f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); color = 0xFFFFFFFF.toInt(); letterSpacing = 0.02f }
    val txt = if (name.isBlank()) " " else name
    val maxText = 640f
    val shown = if (tp.measureText(txt) > maxText) android.text.TextUtils.ellipsize(txt, android.text.TextPaint(tp), maxText, android.text.TextUtils.TruncateAt.END).toString() else txt
    val w = tp.measureText(shown) + 64f
    val hgt = 80f
    val bmp = Bitmap.createBitmap((w + 16f).toInt(), (hgt + 18f).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val rect = android.graphics.RectF(8f, 6f, 8f + w, 6f + hgt)
    c.drawRoundRect(android.graphics.RectF(rect).apply { offset(0f, 4f) }, hgt / 2, hgt / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000; maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL) })
    c.drawRoundRect(rect, hgt / 2, hgt / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, rect.top, 0f, rect.bottom, 0xFF3B8BD0.toInt(), 0xFF2A72B5.toInt(), android.graphics.Shader.TileMode.CLAMP)
    })
    c.drawRoundRect(android.graphics.RectF(rect).apply { inset(1.5f, 1.5f) }, hgt / 2, hgt / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.5f; color = 0x40FFFFFF })
    c.drawText(shown, rect.left + 32f, rect.centerY() - (tp.descent() + tp.ascent()) / 2, tp)
    return bmp
}
