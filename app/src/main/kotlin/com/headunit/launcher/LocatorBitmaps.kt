package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/** The locator pictures (the arrow for the flat look, the Google-style one laid flat, and the neon one): pure drawing, no map state. */

/**
 * The neon look's position arrow: a thick chevron with cut ends and a small triangle inside, a white-hot core fading into the accent
 * colour, a dark edge so it reads on any road, and a soft glow. Same canvas as the Modern arrow (384 px, drawn 4x larger than shown).
 */
internal fun neonArrowBitmap(hex: String): Bitmap {
    val col = try { android.graphics.Color.parseColor(hex) } catch (e: Throwable) { 0xFFFCEE0A.toInt() }
    val n = 384
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val k = 3.2f
    // design space 100x100 (tip up), its middle (50, 49) in the middle of the picture
    fun pt(x: Float, y: Float) = floatArrayOf(192f + (x - 50f) * k, 192f + (y - 49f) * k)
    fun poly(vararg v: Float) = Path().apply {
        for (i in v.indices step 2) { val q = pt(v[i], v[i + 1]); if (i == 0) moveTo(q[0], q[1]) else lineTo(q[0], q[1]) }
        close()
    }
    val chev = poly(50f, 8f, 84f, 84f, 66f, 90f, 50f, 44f, 34f, 90f, 16f, 84f)
    val tri = poly(50f, 54f, 60f, 76f, 40f, 76f)
    val glowP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; maskFilter = android.graphics.BlurMaskFilter(26f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
    c.drawPath(chev, glowP); c.drawPath(tri, glowP)
    c.drawPath(chev, glowP)
    val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF06060F.toInt(); style = Paint.Style.FILL_AND_STROKE; strokeWidth = 22f; strokeJoin = Paint.Join.ROUND }
    c.drawPath(chev, dark); c.drawPath(tri, dark)
    val top = pt(50f, 8f)[1]; val bot = pt(50f, 90f)[1]
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, top, 0f, bot,
            intArrayOf(0xFFFFFFFF.toInt(), col, (col and 0x00FFFFFF) or (0xC7 shl 24)), floatArrayOf(0f, 0.35f, 1f), android.graphics.Shader.TileMode.CLAMP)
    }
    c.drawPath(chev, fill)
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; style = Paint.Style.STROKE; strokeWidth = 5f; strokeJoin = Paint.Join.ROUND }
    c.drawPath(chev, edge)
    c.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col })
    c.drawPath(tri, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f; strokeJoin = Paint.Join.ROUND })
    // the bright inner line along the core
    val core = poly(50f, 20f, 70f, 76f, 63f, 78f, 50f, 42f, 37f, 78f, 30f, 76f)
    c.drawPath(core, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3FFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f; strokeJoin = Paint.Join.ROUND })
    return bmp
}

/** Google Maps locator: a chunky white chevron with a visible edge, lying flat on the map. Points up; the badge is rotated by heading. */
internal fun arrowBitmapImmersive(): Bitmap {
    // the whole locator in one picture (translucent ring plus a soft 3D slab arrow), laid flat on the map, so the ring and the arrow always keep the
    // same proportions and squash together as the view tilts. Drawn 4x larger than it is shown (the layer's icon size scales it back down).
    val n = 384
    val cx = 192f; val cy = 192f
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ringR = 160f
    c.drawCircle(cx, cy, ringR - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB02F78AE.toInt() })
    c.drawCircle(cx, cy, ringR - 3f, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7f; color = 0xB39ED0E2.toInt() })
    val top = Path().apply {
        moveTo(cx, cy - 125f)
        lineTo(cx + 91f, cy + 33f)
        lineTo(cx, cy + 11f)
        lineTo(cx - 91f, cy + 33f)
        close()
    }
    val round = android.graphics.CornerPathEffect(26f)
    val h = 34
    c.drawPath(Path(top).apply { offset(0f, h + 22f) }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66102040; pathEffect = round; maskFilter = android.graphics.BlurMaskFilter(18f, android.graphics.BlurMaskFilter.Blur.NORMAL) })
    // the slab's wall: lighter at the top edge, darker towards the ground, so it reads as a solid block
    for (d in h downTo 1) {
        val t = d / h.toFloat()
        val col = android.graphics.Color.rgb((0xA4 - 0x3A * t).toInt(), (0xA5 - 0x3A * t).toInt(), (0xC8 - 0x38 * t).toInt())
        c.drawPath(Path(top).apply { offset(0f, d.toFloat()) }, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = col; pathEffect = round })
    }
    c.drawPath(top, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        pathEffect = round
        shader = android.graphics.LinearGradient(0f, cy - 125f, 0f, cy + 33f, 0xFFF2F2F6.toInt(), 0xFFC4C5D0.toInt(), android.graphics.Shader.TileMode.CLAMP)
    })
    // a thin bright edge round the top face
    c.drawPath(top, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = 0x99FFFFFF.toInt(); pathEffect = round })
    return bmp
}

/** Waze-style arrow: fat rounded chevron, blue fill, thick white outline and a soft shadow. Points up; rotated by heading. */
internal fun arrowBitmap(): Bitmap {
    val n = 128
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val path = Path().apply {
        moveTo(64f, 14f)
        quadTo(70f, 14f, 74f, 22f)
        lineTo(100f, 92f)
        quadTo(104f, 104f, 92f, 100f)
        lineTo(64f, 84f)
        lineTo(36f, 100f)
        quadTo(24f, 104f, 28f, 92f)
        lineTo(54f, 22f)
        quadTo(58f, 14f, 64f, 14f)
        close()
    }
    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x00000000; style = Paint.Style.FILL; setShadowLayer(10f, 0f, 5f, 0x80000000.toInt())
    }
    c.drawPath(path, shadow)
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFFFFF.toInt(); style = Paint.Style.STROKE; strokeWidth = 12f; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND
    }
    c.drawPath(path, edge)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        shader = android.graphics.LinearGradient(0f, 14f, 0f, 104f, 0xFF3AA0FF.toInt(), 0xFF1263E0.toInt(), android.graphics.Shader.TileMode.CLAMP)
    }
    c.drawPath(path, fill)
    return bmp
}

/**
 * Your picture as the locator: the same size as the 3D arrow; when it turns with you it is also laid flat on the road
 * (squashed by the tilt, like the arrow), otherwise it stands upright facing you.
 */
internal fun renderCustom(base: Bitmap, yaw: Double, pitch: Double, lie: Boolean, circle: Boolean): Bitmap {
    val w = 300
    val bmp = Bitmap.createBitmap(w, w, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val side = 190f
    val src = if (circle) {
        // cut to a circle with a thin white edge, for photos and square logos
        val n = minOf(base.width, base.height)
        val out = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
        val oc = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        oc.drawCircle(n / 2f, n / 2f, n / 2f, p)
        p.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        oc.drawBitmap(base, -(base.width - n) / 2f, -(base.height - n) / 2f, p)
        p.xfermode = null; p.style = Paint.Style.STROKE; p.strokeWidth = n * 0.045f; p.color = 0xFFFFFFFF.toInt()
        oc.drawCircle(n / 2f, n / 2f, n / 2f - p.strokeWidth / 2f, p)
        out
    } else base
    val sc = side / maxOf(src.width, src.height)
    val m = android.graphics.Matrix()
    m.postTranslate(-src.width / 2f, -src.height / 2f)
    m.postScale(sc, sc)
    if (lie) {
        m.postRotate(yaw.toFloat())
        m.postScale(1f, Math.cos(Math.toRadians(pitch)).toFloat().coerceAtLeast(0.35f))
    }
    m.postTranslate(w / 2f, w / 2f)
    // a soft shadow under it so it stands out on any map colour
    val sh = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55000000; maskFilter = android.graphics.BlurMaskFilter(14f, android.graphics.BlurMaskFilter.Blur.NORMAL) }
    val shadow = src.extractAlpha()
    c.save(); c.translate(0f, 6f); c.drawBitmap(shadow, m, sh); c.restore()
    c.drawBitmap(src, m, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    shadow.recycle()
    if (src !== base) src.recycle()
    return bmp
}

private val drawnBottom = java.util.WeakHashMap<Bitmap, Float>()

/** How far below the picture's centre its drawing reaches (in the picture's pixels), measured once per picture. */
internal fun drawnBelowCentre(b: Bitmap): Float = drawnBottom.getOrPut(b) {
    val w = b.width; val h = b.height
    val row = IntArray(w)
    var bottom = h / 2
    for (y in h - 1 downTo h / 2) {
        b.getPixels(row, 0, w, 0, y, w, 1)
        if (row.any { (it ushr 24) > 40 }) { bottom = y; break }
    }
    (bottom - h / 2f).coerceAtLeast(0f)
}
