package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The roundabouts on a route, from OpenStreetMap: each ring as drawn there (or the single point of a mini roundabout), fetched
 * once when a trip starts and kept on the device, so roads you know work without signal too.
 *
 * The turn card places a roundabout's exit by the arms the route comes in on and leaves by, as angles round its middle (like a
 * clock face: in from 9, out at 12 is three quarters round). The way the road comes in or goes out plays no part, so a slanted entry or
 * a bend just before the ring doesn't move the exit.
 */
object Roundabouts {
    /** A ring: its pieces (one or more OSM ways, joined) and its middle. A mini roundabout: no pieces, [mini] true. */
    class Ring(val id: Long, val centre: LL, val pieces: List<List<LL>>, val mini: Boolean) {
        val radius: Double = if (pieces.isEmpty()) 0.0 else pieces.flatten().maxOf { Geo.dist(centre, it) }
    }

    private val rings = java.util.concurrent.ConcurrentHashMap<Long, Ring>()
    @Volatile private var loaded = false
    private const val FILE = "roundabouts.json"
    private const val MAX_KEPT = 4000
    /** How far out along the arms the way in and out are read (past the funnels where they meet the ring). */
    private const val ARM_M = 40.0

    /** Rings already looked up for this trip, by route: the same route isn't asked again. */
    @Volatile private var fetchedFor: Any? = null

    private fun load(ctx: Context) {
        if (loaded) return
        synchronized(this) {
            if (loaded) return
            try {
                val f = java.io.File(ctx.filesDir, FILE)
                if (f.exists()) {
                    val a = JSONArray(f.readText())
                    for (i in 0 until a.length()) {
                        val o = a.getJSONObject(i)
                        val ps = o.optJSONArray("p") ?: JSONArray()
                        val pieces = (0 until ps.length()).map { k ->
                            val q = ps.getJSONArray(k)
                            (0 until q.length() / 2).map { j -> LL(q.getDouble(2 * j), q.getDouble(2 * j + 1)) }
                        }
                        rings[o.getLong("id")] = Ring(o.getLong("id"), LL(o.getDouble("la"), o.getDouble("lo")), pieces, o.optBoolean("m", false))
                    }
                }
            } catch (e: Exception) { }
            loaded = true
        }
    }

    private fun save(ctx: Context) {
        try {
            val a = JSONArray()
            for (r in rings.values.take(MAX_KEPT)) {
                val o = JSONObject().put("id", r.id).put("la", r.centre.lat).put("lo", r.centre.lon).put("m", r.mini)
                val ps = JSONArray()
                for (pc in r.pieces) { val q = JSONArray(); for (p in pc) { q.put(p.lat); q.put(p.lon) }; ps.put(q) }
                o.put("p", ps)
                a.put(o)
            }
            val f = java.io.File(ctx.filesDir, FILE)
            val tmp = java.io.File(ctx.filesDir, "$FILE.tmp")
            tmp.writeText(a.toString())
            tmp.renameTo(f)
        } catch (e: Exception) { }
    }

    /**
     * Looks up the rings at the route's roundabout turns in one query (call off the main thread). Those already kept on the device
     * aren't asked again. Quietly does nothing without signal: the turn card then measures the ring from the route line.
     */
    fun fetchFor(ctx: Context, route: Route) {
        load(ctx)
        if (fetchedFor === route) return
        val at = route.steps.filter { it.maneuver.uppercase().startsWith("ROUNDABOUT") }.map { Geo.pointOnLine(route.pts, route.cum, it.off).first }
        if (at.isEmpty()) { fetchedFor = route; return }
        val missing = at.filter { p -> ringNear(p) == null }
        if (missing.isEmpty()) { fetchedFor = route; return }
        val parts = StringBuilder()
        for (p in missing.take(60)) {
            val c = String.format(java.util.Locale.US, "%.6f,%.6f", p.lat, p.lon)
            parts.append("way(around:90,$c)[\"junction\"~\"^(roundabout|circular)$\"];node(around:40,$c)[\"highway\"=\"mini_roundabout\"];")
        }
        val els = try { Overpass.query("[out:json][timeout:25];($parts);out geom;", 10000, 30000) } catch (e: Exception) { return }
        // ways sharing an end are pieces of one ring
        val pieces = ArrayList<Pair<Long, List<LL>>>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            when (e.optString("type")) {
                "node" -> if (e.has("lat")) {
                    val id = -e.getLong("id")
                    rings[id] = Ring(id, LL(e.getDouble("lat"), e.getDouble("lon")), emptyList(), true)
                }
                "way" -> {
                    val g = e.optJSONArray("geometry") ?: continue
                    val pts = (0 until g.length()).map { k -> val q = g.getJSONObject(k); LL(q.getDouble("lat"), q.getDouble("lon")) }
                    if (pts.size >= 2) pieces += e.getLong("id") to pts
                }
            }
        }
        val parent = IntArray(pieces.size) { it }
        fun find(x: Int): Int { var a = x; while (parent[a] != a) { parent[a] = parent[parent[a]]; a = parent[a] }; return a }
        fun key(p: LL) = Math.round(p.lat * 1e7) * 31 + Math.round(p.lon * 1e7)
        val byEnd = HashMap<Long, Int>()
        for ((i, pc) in pieces.withIndex()) for (end in listOf(pc.second.first(), pc.second.last())) {
            val k = key(end)
            val j = byEnd[k]
            if (j == null) byEnd[k] = i else parent[find(i)] = find(j)
        }
        val groups = pieces.indices.groupBy { find(it) }
        for ((_, idx) in groups) {
            val ps = idx.map { pieces[it].second }
            val all = ps.flatten()
            // the middle: the centre of its outline (an even spread of points isn't guaranteed, so not their average)
            val c = LL((all.minOf { it.lat } + all.maxOf { it.lat }) / 2.0, (all.minOf { it.lon } + all.maxOf { it.lon }) / 2.0)
            val id = idx.minOf { pieces[it].first }
            rings[id] = Ring(id, c, ps, false)
        }
        fetchedFor = route
        save(ctx)
    }

    /** The kept ring (or mini roundabout) at a roundabout turn at [p], if any. */
    private fun ringNear(p: LL): Ring? {
        var best: Ring? = null; var bestD = Double.MAX_VALUE
        for (r in rings.values) {
            val d = Geo.dist(p, r.centre)
            val reach = if (r.mini) 40.0 else r.radius + 60.0
            if (d < reach && d < bestD) { best = r; bestD = d }
        }
        return best
    }

    /**
     * The turn at the roundabout whose step is at [entry] metres along the route, from where the route meets and leaves the ring:
     * -90 a left, 0 straight on, 90 a right, -180 all the way round. Null when no ring is kept for it (or the route doesn't
     * touch it), so the line-based measure is used instead.
     */
    fun turnAt(ctx: Context, route: Route, entry: Double, limitAt: Double, leftTraffic: Boolean): Double? {
        load(ctx)
        if (route.pts.size < 2) return null
        val len = route.cum.last()
        val stepPt = Geo.pointOnLine(route.pts, route.cum, entry.coerceIn(0.0, len)).first
        val ring = ringNear(stepPt) ?: return null
        val c = ring.centre
        val kx = 111320.0 * cos(Math.toRadians(c.lat)); val ky = 110540.0
        fun xy(p: LL) = doubleArrayOf((p.lon - c.lon) * kx, (p.lat - c.lat) * ky)
        fun ang(p: LL): Double { val v = xy(p); return Math.toDegrees(atan2(v[1], v[0])) }
        val from = max(0.0, entry - 60.0)
        val to = min(len, min(limitAt, entry + 300.0))
        if (to <= from + 5.0) return null
        val pIn: LL; val pOut: LL
        if (ring.mini) {
            // a mini roundabout: where the route is 15 m before and after its point
            var bestO = -1.0; var bestD = Double.MAX_VALUE
            var o = from
            while (o <= to) { val d = Geo.dist(Geo.pointOnLine(route.pts, route.cum, o).first, c); if (d < bestD) { bestD = d; bestO = o }; o += 2.0 }
            if (bestO < 0 || bestD > 25.0) return null
            pIn = Geo.pointOnLine(route.pts, route.cum, max(0.0, bestO - 15.0)).first
            pOut = Geo.pointOnLine(route.pts, route.cum, min(len, bestO + 15.0)).first
        } else {
            // the stretch of the route that runs on the ring: where it first comes within a few metres of it, and where it leaves
            val segs = ring.pieces.flatMap { pc -> pc.zipWithNext().map { (a, b) -> xy(a) to xy(b) } }
            fun distToRing(p: LL): Double {
                val v = xy(p); var m = Double.MAX_VALUE
                for ((a, b) in segs) {
                    val dx = b[0] - a[0]; val dy = b[1] - a[1]
                    val l2 = dx * dx + dy * dy
                    val t = if (l2 <= 0.0) 0.0 else (((v[0] - a[0]) * dx + (v[1] - a[1]) * dy) / l2).coerceIn(0.0, 1.0)
                    val ex = a[0] + t * dx - v[0]; val ey = a[1] + t * dy - v[1]
                    m = min(m, sqrt(ex * ex + ey * ey))
                }
                return m
            }
            var join = -1.0; var leave = -1.0
            var o = from
            while (o <= to) {
                val on = distToRing(Geo.pointOnLine(route.pts, route.cum, o).first) < 6.0
                if (on) { if (join < 0) join = o; leave = o } else if (join >= 0 && o - leave > 12.0) break
                o += 2.0
            }
            if (join < 0 || leave - join < 4.0) return null
            // the arms, not where they touch the ring: the funnels in and out move those round by 30 degrees or more (in from the
            // left and out at the top met the ring at 8 and half past 1, which looked like the second exit instead of the third).
            // So the route's place 40 m out along each arm sets the angles
            pIn = Geo.pointOnLine(route.pts, route.cum, max(0.0, join - ARM_M)).first
            pOut = Geo.pointOnLine(route.pts, route.cum, min(len, leave + ARM_M)).first
            // the whole way round: in and out at nearly the same place, after more than half the ring
            val circ = 2.0 * Math.PI * max(ring.radius, 5.0)
            val a0 = ang(pIn); val a1 = ang(pOut)
            val raw = if (leftTraffic) ((a0 - a1) % 360.0 + 360.0) % 360.0 else ((a1 - a0) % 360.0 + 360.0) % 360.0
            val arc = if (raw < 40.0 && leave - join > circ * 0.5) 360.0 else raw
            return if (leftTraffic) arc - 180.0 else 180.0 - arc
        }
        val a0 = ang(pIn); val a1 = ang(pOut)
        val arc = if (leftTraffic) ((a0 - a1) % 360.0 + 360.0) % 360.0 else ((a1 - a0) % 360.0 + 360.0) % 360.0
        return if (leftTraffic) arc - 180.0 else 180.0 - arc
    }
}
