package com.headunit.launcher.car

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import com.headunit.launcher.Geo
import com.headunit.launcher.LL
import com.headunit.launcher.NavEngine

/** What the driver's display shows comes from the main car screen's engine (one engine per connection to the car). */
object ClusterLink {
    @Volatile var engine: NavEngine? = null
    @Volatile var dark: Boolean = true
}

/**
 * The driver's display (the screen behind the steering wheel, on cars that give Android Auto a map there): a plain drawn view, not a
 * second map. The route ahead with you at the bottom, heading up, zoomed out further the faster you go; the next turn at the top;
 * and with a route a small bar at the bottom centre with time left · distance · arrival.
 */
class ClusterSession : Session() {
    private val ui = Handler(Looper.getMainLooper())
    private var surface: SurfaceContainer? = null
    private val redraw = object : Runnable {
        override fun run() {
            try { draw() } catch (e: Throwable) { }
            if (surface != null) ui.postDelayed(this, 500L)
        }
    }

    override fun onCreateScreen(intent: Intent): Screen {
        try {
            carContext.getCarService(AppManager::class.java).setSurfaceCallback(object : SurfaceCallback {
                override fun onSurfaceAvailable(c: SurfaceContainer) { surface = c; ui.removeCallbacks(redraw); ui.post(redraw) }
                override fun onSurfaceDestroyed(c: SurfaceContainer) { surface = null; ui.removeCallbacks(redraw) }
            })
        } catch (e: Throwable) { android.util.Log.w("CarCluster", "no surface on the driver's display", e) }
        return ClusterScreen(carContext)
    }

    private fun draw() {
        val sc = surface ?: return
        val sf = sc.surface ?: return
        if (!sf.isValid) return
        val cv = try { sf.lockCanvas(null) } catch (e: Throwable) { null } ?: return
        try { ClusterPainter.paint(cv, sc.width.toFloat(), sc.height.toFloat(), ClusterLink.engine, ClusterLink.dark) } finally {
            try { sf.unlockCanvasAndPost(cv) } catch (e: Throwable) { }
        }
    }
}

private class ClusterScreen(ctx: CarContext) : Screen(ctx) {
    override fun onGetTemplate(): Template = NavigationTemplate.Builder()
        .setActionStrip(ActionStrip.Builder().addAction(Action.Builder().setTitle("Horizon").setOnClickListener { }.build()).build())
        .build()
}

/** The sums behind the driver's display (kept apart from the drawing, so they can be tested without a screen). */
object ClusterMath {
    /** How far ahead the view reaches, in metres: 350 m standing, further the faster you go (25 seconds of driving more), 1.5 km at most. */
    fun reach(speedMps: Double): Double = (350.0 + speedMps.coerceAtLeast(0.0) * 25.0).coerceAtMost(1500.0)

    /** "17 min · 17 km · 20:13" */
    fun tripLine(secs: Double, metres: Double, arrive: String, imperial: Boolean): String =
        Geo.fmtDur(secs) + " · " + Geo.fmtDist(metres, imperial) + " · " + arrive
}

object ClusterPainter {
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; isFakeBoldText = true }

    fun paint(cv: Canvas, w: Float, h: Float, e: NavEngine?, dark: Boolean) {
        val bg = if (dark) 0xFF10141C.toInt() else 0xFFE9EDF2.toInt()
        val fg = if (dark) 0xFFFFFFFF.toInt() else 0xFF10141C.toInt()
        val blue = 0xFF29B6F6.toInt()
        cv.drawColor(bg)
        val u = minOf(w, h) / 100f
        val f = e?.fix
        val r = e?.route
        val ax = w / 2f; val ay = h * 0.66f
        if (e != null && f != null && r != null && r.pts.size >= 2 && !e.previewing) {
            val range = ClusterMath.reach(f.speed)
            val scale = (h * 0.56f) / range.toFloat()
            val prog = e.progressNow
            val mLat = 111320.0; val mLon = 111320.0 * Math.cos(Math.toRadians(f.pos.lat))
            val hr = Math.toRadians(f.heading); val ch = Math.cos(hr); val sh = Math.sin(hr)
            fun pt(p: LL): FloatArray {
                val x = (p.lon - f.pos.lon) * mLon; val y = (p.lat - f.pos.lat) * mLat
                val rx = x * ch - y * sh; val ry = x * sh + y * ch
                return floatArrayOf(ax + (rx * scale).toFloat(), ay - (ry * scale).toFloat())
            }
            val c = r.cum
            var lo = 0; var hi = c.size - 1
            while (lo < hi - 1) { val m = (lo + hi) ushr 1; if (c[m] <= prog) lo = m else hi = m }
            val path = Path()
            path.moveTo(ax, ay)
            var i = hi
            val end = prog + range * 1.8
            while (i < r.pts.size && c[i] <= end) { val q = pt(r.pts[i]); path.lineTo(q[0], q[1]); i++ }
            line.color = blue; line.strokeWidth = 4.2f * u
            cv.drawPath(path, line)
        }
        // you: an arrow pointing up
        val a = 5.2f * u
        val arrow = Path().apply { moveTo(ax, ay - a * 1.3f); lineTo(ax + a, ay + a); lineTo(ax, ay + a * 0.45f); lineTo(ax - a, ay + a); close() }
        fill.color = bg; line.color = bg; line.strokeWidth = 2.2f * u
        cv.drawPath(arrow, line)
        fill.color = fg
        cv.drawPath(arrow, fill)
        if (e == null || r == null || e.previewing) return
        // the next turn, at the top
        e.turn?.let { t ->
            text.color = fg; text.textSize = 8.5f * u
            val s = Geo.fmtDist(t.dist, e.imperial) + (if (t.street.isNotBlank()) " · " + t.street else "")
            cv.drawText(fit(s, w * 0.9f), w / 2f, 11f * u, text)
        }
        // the trip: time left · distance · arrival, in a small bar at the bottom centre
        e.eta?.let { eta ->
            text.textSize = 7.5f * u
            val s = fit(ClusterMath.tripLine(eta.secs, eta.dist, eta.arrive, e.imperial), w * 0.86f)
            val tw = text.measureText(s); val bh = 12f * u
            val box = RectF(w / 2f - tw / 2f - 4f * u, h - bh - 3f * u, w / 2f + tw / 2f + 4f * u, h - 3f * u)
            fill.color = if (dark) 0xE6232A36.toInt() else 0xE6FFFFFF.toInt()
            cv.drawRoundRect(box, bh / 2f, bh / 2f, fill)
            text.color = fg
            cv.drawText(s, w / 2f, box.centerY() - (text.descent() + text.ascent()) / 2f, text)
        }
    }

    private fun fit(s: String, max: Float): String {
        if (text.measureText(s) <= max) return s
        var t = s
        while (t.length > 4 && text.measureText("$t…") > max) t = t.dropLast(1)
        return "$t…"
    }
}
