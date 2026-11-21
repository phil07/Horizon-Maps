package com.headunit.launcher

import android.graphics.Path

// Drawing helpers for the map (colours, rounded and soft shapes, a hexagon): pure functions of their arguments.

internal fun shade(hex: String, f: Float): Int {
    val c = try { android.graphics.Color.parseColor(hex) } catch (e: Exception) { -1 }
    fun ch(v: Int) = (v * f).toInt().coerceIn(0, 255)
    return android.graphics.Color.rgb(ch(android.graphics.Color.red(c)), ch(android.graphics.Color.green(c)), ch(android.graphics.Color.blue(c)))
}

internal fun soften(pts: List<Pair<Double, Double>>, r: Double, steps: Int = 5): List<Pair<Double, Double>> {
    val n = pts.size
    val out = ArrayList<Pair<Double, Double>>()
    for (i in 0 until n) {
        val p0 = pts[(i - 1 + n) % n]; val p1 = pts[i]; val p2 = pts[(i + 1) % n]
        val d1 = Math.hypot(p1.first - p0.first, p1.second - p0.second); val d2 = Math.hypot(p2.first - p1.first, p2.second - p1.second)
        val rr = minOf(r, d1 * 0.45, d2 * 0.45)
        val a = (p1.first + (p0.first - p1.first) * rr / d1) to (p1.second + (p0.second - p1.second) * rr / d1)
        val b = (p1.first + (p2.first - p1.first) * rr / d2) to (p1.second + (p2.second - p1.second) * rr / d2)
        for (k in 0..steps) {
            val t = k / steps.toDouble(); val u = 1 - t
            out += (u * u * a.first + 2 * u * t * p1.first + t * t * b.first) to (u * u * a.second + 2 * u * t * p1.second + t * t * b.second)
        }
    }
    return out
}

internal fun rrect(x0: Double, x1: Double, y0: Double, y1: Double, r: Double) = soften(listOf(x1 to y1, x0 to y1, x0 to y0, x1 to y0), r, 4)

internal fun scaleCol(c: Int, f: Double): Int {
    fun ch(v: Int) = (v * f).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(android.graphics.Color.alpha(c), ch(android.graphics.Color.red(c)), ch(android.graphics.Color.green(c)), ch(android.graphics.Color.blue(c)))
}

internal fun hexPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
    for (k in 0 until 6) {
        val a = Math.toRadians(-90.0 + 60.0 * k)
        val x = cx + r * Math.cos(a).toFloat(); val y = cy + r * Math.sin(a).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
