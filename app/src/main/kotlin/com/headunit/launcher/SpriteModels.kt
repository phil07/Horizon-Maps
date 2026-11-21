package com.headunit.launcher

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * The shapes of the 3D-look locator sprites (the arrow and the cars, as solid pieces) and the picture they are drawn into. Pure
 * geometry and drawing: no map state; the car model is passed in.
 */

/** One solid piece of the model: a footprint pushed up from [z0] to [z1], in one colour. */
internal class Prism(val pts: List<Pair<Double, Double>>, val z0: Double, val z1: Double, val col: Int)

internal fun arrowParts(col: String): List<Prism> {
    // the same soft chevron as the flat arrow, now solid: a dark edge underneath and the arrow on top
    // (listed anticlockwise, so the walls face outwards)
    // very soft corners, like the flat arrow: a round nose, rounded wing tips and a smooth curve at the back
    // sized and shaped to match the flat arrow: it fills a little over half the ring's width, with a deep, smooth curve at the back
    val raw = listOf(0.0 to 0.58, -0.64 to -0.53, 0.0 to -0.28, 0.64 to -0.53)
    val soft = soften(soften(raw, 0.3, 8), 0.06, 2)
    // centre the arrow exactly on the ring: move it so the middle of its area sits on the centre point,
    // and mirror the right half onto the left so it is perfectly symmetrical from every angle
    var a2 = 0.0; var cyA = 0.0
    for (i in soft.indices) {
        val p0 = soft[i]; val p1 = soft[(i + 1) % soft.size]
        val cr = p0.first * p1.second - p1.first * p0.second
        a2 += cr; cyA += (p0.second + p1.second) * cr
    }
    val cy = if (Math.abs(a2) > 1e-9) cyA / (3.0 * a2) else 0.0
    val half = soft.filter { it.first <= 1e-9 }.map { 0.0.coerceAtMost(it.first) to it.second - cy }
    val right = half.asReversed().map { -it.first to it.second }.filter { it.first > 1e-9 }
    val shape = if (half.size >= 3) half + right else soft.map { it.first to it.second - cy }
    return listOf(Prism(shape, 0.0, 0.19, shade(col, 1f)))
}

internal fun carParts(col: String, model: String): List<Prism> {
    val body = shade(col, 1f); val lower = shade(col, 0.9f); val roof = shade(col, 1.05f)
    val glass = 0xFF2C4F8C.toInt(); val dark = 0xFF1B1E26.toInt(); val red = 0xFFE3262F.toInt()
    val l = ArrayList<Prism>()
    fun wheels(front: Double, back: Double, x: Double = 0.98, r: Double = 0.36, h: Double = 0.52) {
        for (sx in listOf(-1.0, 1.0)) for (sy in listOf(back, front)) l += Prism(rrect(sx * x - 0.13, sx * x + 0.13, sy - r, sy + r, 0.12), 0.0, h, 0xFF101114.toInt())
    }
    fun tails(y: Double, x: Double, z0: Double, z1: Double) {
        l += Prism(rrect(-x, -x + 0.32, y, y + 0.25, 0.12), z0, z1, red); l += Prism(rrect(x - 0.32, x, y, y + 0.25, 0.12), z0, z1, red)
    }
    fun mirrors(y: Double, x: Double, z: Double) {
        l += Prism(rrect(-x - 0.18, -x, y, y + 0.26, 0.08), z, z + 0.14, dark); l += Prism(rrect(x, x + 0.18, y, y + 0.26, 0.08), z, z + 0.14, dark)
    }
    when (model) {
        // a short, rounded hatchback
        "hatch" -> {
            wheels(1.2, -1.15)
            l += Prism(rrect(-0.9, 0.9, -1.92, -1.62, 0.28), 0.1, 0.36, dark)
            l += Prism(rrect(-1.0, 1.0, -1.86, 1.95, 0.8), 0.24, 0.6, lower)
            l += Prism(rrect(-1.06, 1.06, -1.82, 1.9, 0.9), 0.6, 0.98, body)
            tails(-1.87, 0.96, 0.5, 1.0)
            mirrors(0.3, 1.04, 0.96)
            l += Prism(rrect(-0.88, 0.88, -1.6, 0.75, 0.62), 0.98, 1.44, glass)
            l += Prism(rrect(-0.8, 0.8, -1.5, 0.48, 0.55), 1.44, 1.6, roof)
        }
        // a long estate with a long flat roof
        "estate" -> {
            wheels(1.42, -1.5)
            l += Prism(rrect(-0.92, 0.92, -2.42, -2.1, 0.28), 0.1, 0.36, dark)
            l += Prism(rrect(-1.02, 1.02, -2.36, 2.2, 0.7), 0.24, 0.6, lower)
            l += Prism(rrect(-1.08, 1.08, -2.32, 2.14, 0.8), 0.6, 0.98, body)
            tails(-2.37, 0.98, 0.46, 0.95)
            mirrors(0.42, 1.04, 0.96)
            l += Prism(rrect(-0.9, 0.9, -2.08, 0.95, 0.55), 0.98, 1.42, glass)
            l += Prism(rrect(-0.82, 0.82, -1.98, 0.66, 0.45), 1.42, 1.58, roof)
        }
        // a tall, square SUV with roof rails
        "suv" -> {
            wheels(1.45, -1.4, 1.02, 0.42, 0.66)
            l += Prism(rrect(-0.98, 0.98, -2.3, -2.0, 0.22), 0.14, 0.46, dark)
            l += Prism(rrect(-1.08, 1.08, -2.24, 2.24, 0.55), 0.3, 0.78, lower)
            l += Prism(rrect(-1.14, 1.14, -2.2, 2.18, 0.62), 0.78, 1.22, body)
            tails(-2.25, 1.04, 0.66, 1.18)
            mirrors(0.5, 1.1, 1.2)
            l += Prism(rrect(-0.98, 0.98, -2.0, 1.0, 0.4), 1.22, 1.74, glass)
            l += Prism(rrect(-0.92, 0.92, -1.92, 0.78, 0.34), 1.74, 1.88, roof)
            l += Prism(rrect(-0.84, -0.74, -1.8, 0.6, 0.04), 1.88, 1.96, dark)
            l += Prism(rrect(0.74, 0.84, -1.8, 0.6, 0.04), 1.88, 1.96, dark)
        }
        // a pickup: a cab at the front and an open load bed behind
        "pickup" -> {
            wheels(1.5, -1.55, 1.04, 0.42, 0.66)
            l += Prism(rrect(-1.0, 1.0, -2.5, -2.22, 0.18), 0.14, 0.46, dark)
            l += Prism(rrect(-1.1, 1.1, -2.44, 2.3, 0.5), 0.3, 0.78, lower)
            l += Prism(rrect(-1.14, 1.14, -0.25, 2.26, 0.6), 0.78, 1.2, body)
            l += Prism(rrect(-1.14, 1.14, -2.42, -0.25, 0.14), 0.78, 0.86, dark)                      // the bed floor
            l += Prism(rrect(-1.14, -0.98, -2.42, -0.25, 0.06), 0.86, 1.24, body)                    // bed sides and tailgate
            l += Prism(rrect(0.98, 1.14, -2.42, -0.25, 0.06), 0.86, 1.24, body)
            l += Prism(rrect(-1.14, 1.14, -2.42, -2.28, 0.06), 0.86, 1.24, body)
            tails(-2.45, 1.06, 0.7, 1.16)
            mirrors(0.95, 1.1, 1.2)
            l += Prism(rrect(-0.98, 0.98, -0.2, 1.3, 0.4), 1.2, 1.72, glass)
            l += Prism(rrect(-0.92, 0.92, -0.12, 1.06, 0.34), 1.72, 1.86, roof)
        }
        // a tall box van
        "van" -> {
            wheels(1.75, -1.6, 1.04, 0.4, 0.62)
            l += Prism(rrect(-1.0, 1.0, -2.55, -2.3, 0.14), 0.12, 0.44, dark)
            l += Prism(rrect(-1.1, 1.1, -2.5, 2.35, 0.45), 0.26, 0.74, lower)
            l += Prism(rrect(-1.14, 1.14, -2.48, 2.3, 0.5), 0.74, 1.1, body)
            tails(-2.52, 1.06, 0.7, 1.4)
            mirrors(1.55, 1.1, 1.15)
            l += Prism(rrect(-1.0, 1.0, 1.25, 2.0, 0.4), 1.1, 1.72, glass)                           // the windscreen
            l += Prism(rrect(-1.14, 1.14, -2.48, 1.3, 0.24), 1.1, 2.15, body)                       // the load box
            l += Prism(rrect(-1.06, 1.06, -2.4, 1.25, 0.2), 2.15, 2.22, roof)
        }
        // a low, wide sports car with a little spoiler
        "sports" -> {
            wheels(1.3, -1.38, 1.04, 0.38, 0.46)
            l += Prism(rrect(-0.95, 0.95, -2.28, -2.0, 0.3), 0.08, 0.3, dark)
            l += Prism(rrect(-1.1, 1.1, -2.22, 2.28, 0.95), 0.18, 0.5, lower)
            l += Prism(rrect(-1.16, 1.16, -2.18, 2.24, 1.0), 0.5, 0.78, body)
            tails(-2.22, 1.02, 0.42, 0.72)
            mirrors(0.25, 1.1, 0.78)
            l += Prism(rrect(-0.84, 0.84, -1.0, 0.85, 0.7), 0.78, 1.12, glass)
            l += Prism(rrect(-0.72, 0.72, -0.78, 0.5, 0.6), 1.12, 1.22, roof)
            l += Prism(rrect(-0.98, 0.98, -2.2, -1.96, 0.08), 0.86, 0.94, dark)                       // spoiler
        }
        else -> {
    // the saloon: a chunky, friendly, cartoon car in the style of the Google Maps one: short and wide, very rounded, a big domed
    // glasshouse, a fat white roof, tall red tail lights in the rear corners, a dark under-bumper and black mirrors. Length 4.4.
    for (sx in listOf(-1.0, 1.0)) for (sy in listOf(-1.38, 1.42)) l += Prism(rrect(sx * 0.98 - 0.13, sx * 0.98 + 0.13, sy - 0.36, sy + 0.36, 0.12), 0.0, 0.52, 0xFF101114.toInt())
    l += Prism(rrect(-0.92, 0.92, -2.24, -1.9, 0.3), 0.1, 0.36, dark)                // under-bumper
    l += Prism(rrect(-1.02, 1.02, -2.16, 2.2, 0.85), 0.24, 0.6, lower)             // lower body
    l += Prism(rrect(-1.08, 1.08, -2.12, 2.14, 0.95), 0.6, 0.98, body)             // big rounded shoulders
    l += Prism(rrect(-0.98, -0.66, -2.17, -1.92, 0.12), 0.46, 0.95, red)           // tall tail lights in the corners
    l += Prism(rrect(0.66, 0.98, -2.17, -1.92, 0.12), 0.46, 0.95, red)
    l += Prism(rrect(-1.22, -1.04, 0.42, 0.68, 0.08), 0.96, 1.1, dark)              // mirrors
    l += Prism(rrect(1.04, 1.22, 0.42, 0.68, 0.08), 0.96, 1.1, dark)
    l += Prism(rrect(-0.9, 0.9, -1.35, 0.95, 0.7), 0.98, 1.42, glass)             // domed glasshouse
    l += Prism(rrect(-0.8, 0.8, -1.05, 0.66, 0.62), 1.42, 1.58, roof)              // fat roof
        }
    }
    return l
}

/**
 * Draws the 3D model as seen from the camera: [yaw] is the model's heading relative to the top of the screen, [pitch] the map tilt.
 * Faces are flat-shaded (lit from the top left of the screen) and painted back to front.
 */
internal fun renderSprite(parts: List<Prism>, unitPx: Double, yaw: Double, pitch: Double, car: Boolean): Bitmap {
    val w = 300; val h = 300
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val ox = w / 2.0; val oy = h / 2.0
    val cy = Math.cos(Math.toRadians(yaw)); val sy = Math.sin(Math.toRadians(yaw))
    val cp = Math.cos(Math.toRadians(pitch)); val sp = Math.sin(Math.toRadians(pitch))
    fun rot(x: Double, y: Double) = (x * cy + y * sy) to (-x * sy + y * cy)
    fun proj(x: Double, y: Double, z: Double): Pair<Float, Float> {
        val (X, Y) = rot(x, y)
        return (ox + X * unitPx).toFloat() to (oy - (Y * cp + z * sp) * unitPx).toFloat()
    }
    fun depth(x: Double, y: Double, z: Double): Double { val (_, Y) = rot(x, y); return -Y * sp + z * cp }
    // soft glow / shadow on the road
    val glowR = if (car) 1.45 else 0.95
    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (car) 0x667FC8FF else 0x9E2F78AE.toInt()
        if (car) maskFilter = android.graphics.BlurMaskFilter((unitPx * 0.45).toFloat(), android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    val ring = Path()
    for (k in 0..40) { val a = 2 * Math.PI * k / 40; val (px, py) = proj(Math.cos(a) * glowR, Math.sin(a) * glowR * (if (car) 2.0 else 1.0), 0.0); if (k == 0) ring.moveTo(px, py) else ring.lineTo(px, py) }
    ring.close()
    c.drawPath(ring, glowPaint)
    if (!car) c.drawPath(ring, Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 4f; color = 0xB39ED0E2.toInt() })
    // light comes from the viewer's upper left
    val lx = -0.35; val ly = 0.45
    // each face: its outline on screen, depth, base colour and how brightly it's lit; walls get a top-to-bottom shade, tops a soft sheen
    class Face(val path: Path, val d: Double, val col: Int, val top: Boolean, val y0: Float, val y1: Float)
    val faces = ArrayList<Face>()
    for (pr in parts) {
        val n = pr.pts.size
        for (i in 0 until n) {
            val a = pr.pts[i]; val b = pr.pts[(i + 1) % n]
            // outward normal of this wall (footprints are anticlockwise), turned to the screen
            var nx = b.second - a.second; var ny = -(b.first - a.first)
            val nl = Math.hypot(nx, ny); if (nl < 1e-9) continue
            nx /= nl; ny /= nl
            val (NX, NY) = rot(nx, ny)
            // walls facing away from the camera can't be seen
            if (-NY * sp < -0.02 && pitch > 2) continue
            val bright = (0.62 + 0.38 * (NX * lx + NY * ly)).coerceIn(0.42, 1.04)
            val p1 = proj(a.first, a.second, pr.z0); val p2 = proj(b.first, b.second, pr.z0)
            val p3 = proj(b.first, b.second, pr.z1); val p4 = proj(a.first, a.second, pr.z1)
            val path = Path().apply { moveTo(p1.first, p1.second); lineTo(p2.first, p2.second); lineTo(p3.first, p3.second); lineTo(p4.first, p4.second); close() }
            val mx = (a.first + b.first) / 2; val my = (a.second + b.second) / 2
            faces += Face(path, depth(mx, my, (pr.z0 + pr.z1) / 2) - 0.001, scaleCol(pr.col, bright), false, minOf(p3.second, p4.second), maxOf(p1.second, p2.second))
        }
        val top = Path()
        var ty0 = Float.MAX_VALUE; var ty1 = -Float.MAX_VALUE
        pr.pts.forEachIndexed { i, q -> val (px, py) = proj(q.first, q.second, pr.z1); ty0 = minOf(ty0, py); ty1 = maxOf(ty1, py); if (i == 0) top.moveTo(px, py) else top.lineTo(px, py) }
        top.close()
        val cxm = pr.pts.sumOf { it.first } / n; val cym = pr.pts.sumOf { it.second } / n
        faces += Face(top, depth(cxm, cym, pr.z1) + 0.002, pr.col, true, ty0, ty1)
    }
    faces.sortBy { it.d }
    // a soft cartoon outline round the whole shape: every face stroked thickly underneath, then filled on top, so only the
    // outside edge of the stroke shows (much cheaper than joining all the faces into one outline)
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = if (car) 6.4f else 7.2f; strokeJoin = Paint.Join.ROUND; color = 0x8A0E1A33.toInt() }
    for (f in faces) c.drawPath(f.path, outline)
    val fp = Paint(Paint.ANTI_ALIAS_FLAG)
    val seam = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.4f; strokeJoin = Paint.Join.ROUND }
    for (f in faces) {
        val lightC = if (f.top) scaleCol(f.col, 1.04) else scaleCol(f.col, 1.06)
        val darkC = if (f.top) scaleCol(f.col, 0.86) else scaleCol(f.col, 0.78)
        fp.shader = if (f.y1 - f.y0 > 1f) android.graphics.LinearGradient(0f, f.y0, 0f, f.y1, lightC, darkC, android.graphics.Shader.TileMode.CLAMP) else null
        fp.color = f.col
        c.drawPath(f.path, fp)
        // a hairline in the face colour hides tiny gaps between neighbouring faces
        seam.color = scaleCol(f.col, 0.95); c.drawPath(f.path, seam)
    }
    return bmp
}
