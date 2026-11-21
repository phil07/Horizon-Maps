package com.headunit.launcher

import com.headunit.launcher.R
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.mapbox.bindgen.Value
import com.mapbox.common.Cancelable
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapLoadingErrorType
import com.mapbox.maps.MapOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.ScreenBox
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.Style
import com.mapbox.maps.applyDefaultParams
import com.mapbox.maps.toCameraOptions
import com.mapbox.maps.toMapboxImage
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.Layer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.generated.CircleLayer
import com.mapbox.maps.extension.style.layers.generated.FillExtrusionLayer
import com.mapbox.maps.extension.style.layers.generated.FillLayer
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.interactions.standard.generated.standardBuildings
import com.mapbox.maps.interactions.standard.generated.standardPoi
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.layers.properties.generated.ModelScaleMode
import com.mapbox.maps.extension.style.layers.generated.RasterLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.CirclePitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.CirclePitchScale
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.layers.properties.generated.SymbolPlacement
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.RasterSource
import com.mapbox.maps.extension.style.sources.generated.VectorSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnRotateListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.OnShoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.RotateGestureDetector
import com.mapbox.android.gestures.ShoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector

// The map's small pictures (symbols for parking, stops, zebra crossings, beacons, the beam, signals, the neon arrow), drawn in code.

internal fun parkedBitmap(): Bitmap {
    val n = 112
    val bmp = Bitmap.createBitmap(n, n, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    p.color = android.graphics.Color.argb(70, 0, 0, 0); c.drawCircle(n / 2f, n / 2f + 3f, n / 2f - 4f, p)
    p.color = android.graphics.Color.WHITE; c.drawCircle(n / 2f, n / 2f, n / 2f - 6f, p)
    p.color = android.graphics.Color.parseColor("#2F6BDF"); c.drawCircle(n / 2f, n / 2f, n / 2f - 13f, p)
    // a simple side-on car: body, cabin and two wheels
    p.color = android.graphics.Color.WHITE
    c.drawRoundRect(android.graphics.RectF(28f, 52f, 84f, 70f), 7f, 7f, p)
    c.drawRoundRect(android.graphics.RectF(38f, 39f, 72f, 58f), 8f, 8f, p)
    p.color = android.graphics.Color.parseColor("#2F6BDF")
    c.drawRoundRect(android.graphics.RectF(43f, 44f, 54f, 53f), 3f, 3f, p); c.drawRoundRect(android.graphics.RectF(57f, 44f, 67f, 53f), 3f, 3f, p)
    p.color = android.graphics.Color.WHITE
    c.drawCircle(40f, 72f, 7f, p); c.drawCircle(72f, 72f, 7f, p)
    p.color = android.graphics.Color.parseColor("#2F6BDF")
    c.drawCircle(40f, 72f, 3f, p); c.drawCircle(72f, 72f, 3f, p)
    return bmp
}

internal fun neonHex(w: Int, h: Int, cx: Float, cy: Float, r: Float, fill: Int, bolt: Boolean, tip: Boolean): Bitmap {
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    if (tip) c.drawPath(Path().apply { moveTo(cx - r * 0.45f, cy + r * 0.9f); lineTo(cx, h - 6f); lineTo(cx + r * 0.45f, cy + r * 0.9f); close() },
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    val hex = hexPath(cx, cy, r)
    c.drawPath(hex, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF05060B.toInt() })
    c.drawPath(hexPath(cx, cy, r - 5f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF05060B.toInt() }
    if (bolt) {
        val k = r / 35f
        c.drawPath(Path().apply {
            moveTo(cx + 7 * k, cy - 32 * k); lineTo(cx - 19 * k, cy + 5 * k); lineTo(cx - 3 * k, cy + 5 * k); lineTo(cx - 9 * k, cy + 32 * k)
            lineTo(cx + 19 * k, cy - 7 * k); lineTo(cx + 3 * k, cy - 7 * k); lineTo(cx + 10 * k, cy - 32 * k); close()
        }, dark)
    } else c.drawRect(cx - r * 0.3f, cy - r * 0.3f, cx + r * 0.3f, cy + r * 0.3f, dark)
    return bmp
}

internal fun stopBitmap(): Bitmap {
    val s = 72
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    fun octagon(r: Float): Path = Path().apply {
        for (i in 0 until 8) {
            val a = Math.toRadians(22.5 + 45.0 * i)
            val x = s / 2f + r * Math.cos(a).toFloat(); val y = s / 2f + r * Math.sin(a).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    c.drawPath(octagon(35f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    c.drawPath(octagon(30f), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC8102E.toInt() })
    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 18f; typeface = android.graphics.Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    c.drawText(Lang.t("STOPP", "STOP"), s / 2f, s / 2f - (tp.descent() + tp.ascent()) / 2f, tp)
    return bmp
}

internal fun zebraBitmap(): Bitmap {
    val w = 200; val h = 80
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xF2FFFFFF.toInt() }
    var x = 6f
    while (x < w - 10) { c.drawRoundRect(android.graphics.RectF(x, 4f, x + 14f, h - 4f), 2f, 2f, p); x += 26f }
    return bmp
}

internal fun beaconBitmap(): Bitmap {
    val w = 40; val h = 120
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val cx = w / 2f
    for (k in 0 until 6) {
        val y0 = 34f + k * 14f
        c.drawRect(cx - 3.5f, y0, cx + 3.5f, y0 + 14f, Paint().apply { color = if (k % 2 == 0) 0xFF111111.toInt() else 0xFFFFFFFF.toInt() })
    }
    c.drawCircle(cx, 20f, 18f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.RadialGradient(cx - 5f, 14f, 20f, intArrayOf(0xFFFFE08A.toInt(), 0xFFFFA000.toInt(), 0xFFE07B00.toInt()), floatArrayOf(0f, 0.6f, 1f), android.graphics.Shader.TileMode.CLAMP)
    })
    return bmp
}

internal fun beamBitmap(): Bitmap {
    val w = 200; val h = 256
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    // about half as wide as it was: it lights the road ahead, not the buildings either side
    val half = (h * Math.tan(Math.toRadians(8.5))).toFloat()
    val path = Path().apply { moveTo(w / 2f, h - 2f); lineTo(w / 2f - half, 4f); lineTo(w / 2f + half, 4f); close() }
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = android.graphics.LinearGradient(0f, h.toFloat(), 0f, 0f, intArrayOf(0x44FFF1C2.toInt(), 0x22FFF1C2.toInt(), 0x00FFF1C2), floatArrayOf(0f, 0.55f, 1f), android.graphics.Shader.TileMode.CLAMP)
        maskFilter = android.graphics.BlurMaskFilter(6f, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
    c.drawPath(path, p)
    return b
}

internal fun signalBitmap(): Bitmap {
    val s = 3f
    val bmp = Bitmap.createBitmap((30 * s).toInt(), (52 * s).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(s, s)
    val outer = RectF(4f, 3f, 26f, 47f)
    // shadow just below, then the white outline, then the dark body
    c.drawRoundRect(RectF(outer.left, outer.top + 2f, outer.right, outer.bottom + 2f), 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 })
    c.drawRoundRect(outer, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
    val inner = RectF(outer.left + 2.2f, outer.top + 2.2f, outer.right - 2.2f, outer.bottom - 2.2f)
    c.drawRoundRect(inner, 6f, 6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3C4043.toInt() })
    val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    val cx = (outer.left + outer.right) / 2f
    val r = 4.2f
    dot.color = 0xFFEA4335.toInt(); c.drawCircle(cx, 13.5f, r, dot)
    dot.color = 0xFFFBBC04.toInt(); c.drawCircle(cx, 25f, r, dot)
    dot.color = 0xFF34A853.toInt(); c.drawCircle(cx, 36.5f, r, dot)
    return bmp
}
