package com.headunit.launcher

import com.mapbox.maps.extension.style.expressions.generated.Expression

/** Small builders for the map's style expressions (literal numbers and colours, values that change with the zoom): pure, no map state. */

internal fun lit(v: Number): Expression = Expression.literal(v.toDouble())
internal fun col(hex: String): Expression = Expression.color(android.graphics.Color.parseColor(hex))
/** A value that changes with the zoom: [base] 1.0 = linear, otherwise exponential; pairs of (zoom, value). */
internal fun zoomed(base: Double, vararg zv: Pair<Number, Number>): Expression {
    val args = ArrayList<Expression>()
    args += if (base == 1.0) Expression.linear() else Expression.exponential(base)
    args += Expression.zoom()
    for ((z, v) in zv) { args += lit(z); args += lit(v) }
    return Expression.interpolate(*args.toTypedArray())
}
internal fun zoomedExpr(base: Double, vararg zv: Pair<Number, Expression>): Expression {
    val args = ArrayList<Expression>()
    args += if (base == 1.0) Expression.linear() else Expression.exponential(base)
    args += Expression.zoom()
    for ((z, v) in zv) { args += lit(z); args += v }
    return Expression.interpolate(*args.toTypedArray())
}
