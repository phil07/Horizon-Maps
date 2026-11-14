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
    /**
     * A ring: its pieces (one or more OSM ways, joined) and its middle. A mini roundabout: no pieces, [mini] true. [arms]: where
     * the roads off it are, as angles round its middle (the two carriageways of a divided road count as one); null when not known.
     */
    class Ring(val id: Long, val centre: LL, val pieces: List<List<LL>>, val mini: Boolean, val arms: List<Double>? = null) {
        val radius: Double = if (pieces.isEmpty()) 0.0 else pieces.flatten().maxOf { Geo.dist(centre, it) }
    }

    private val rings = java.util.concurrent.ConcurrentHashMap<Long, Ring>()
    @Volatile private var loaded = false
    private const val FILE = "roundabouts2.json"
    private const val MAX_KEPT = 4000
    /** The way in and out are read where the route is this far outside the ring (past the funnels), at most [ARM_MAX_M] along it. */
    private const val ARM_OUT_M = 15.0
    private const val ARM_MAX_M = 40.0

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
                        val am = o.optJSONArray("a")
                        val arms = am?.let { q -> (0 until q.length()).map { q.getDouble(it) } }
                        rings[o.getLong("id")] = Ring(o.getLong("id"), LL(o.getDouble("la"), o.getDouble("lo")), pieces, o.optBoolean("m", false), arms)
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
                r.arms?.let { am -> val q = JSONArray(); for (x in am) q.put(x); o.put("a", q) }
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
        // (kept from before the arms were counted: asked again once, for their arms)
        val missing = at.filter { p -> ringNear(p)?.arms == null }
        if (missing.isEmpty()) { fetchedFor = route; return }
        val ringQ = StringBuilder(); val miniQ = StringBuilder()
        for (p in missing.take(60)) {
            val c = String.format(java.util.Locale.US, "%.6f,%.6f", p.lat, p.lon)
            ringQ.append("way(around:90,$c)[\"junction\"~\"^(roundabout|circular)$\"];")
            miniQ.append("node(around:40,$c)[\"highway\"=\"mini_roundabout\"];")
        }
        // the rings, the mini roundabouts, and the roads that meet them (their arms)
        val q = "[out:json][timeout:25];($ringQ)->.r;($miniQ)->.m;node(w.r)->.rn;(way(bn.rn)[\"highway\"];way(bn.m)[\"highway\"];)->.a;(.r;.m;.a;);out geom;"
        val els = try { Overpass.query(q, 10000, 30000) } catch (e: Exception) { lastFetch = "lookup failed (${e.javaClass.simpleName})"; return }
        class W(val id: Long, val pts: List<LL>, val tags: JSONObject)
        val ringWays = ArrayList<W>(); val armWays = ArrayList<W>()
        val minis = ArrayList<Pair<Long, LL>>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            val tags = e.optJSONObject("tags") ?: JSONObject()
            when (e.optString("type")) {
                "node" -> if (e.has("lat") && tags.optString("highway") == "mini_roundabout") minis += e.getLong("id") to LL(e.getDouble("lat"), e.getDouble("lon"))
                "way" -> {
                    val g = e.optJSONArray("geometry") ?: continue
                    val pts = (0 until g.length()).map { k -> val o = g.getJSONObject(k); LL(o.getDouble("lat"), o.getDouble("lon")) }
                    if (pts.size < 2) continue
                    val j = tags.optString("junction")
                    if (j == "roundabout" || j == "circular") ringWays += W(e.getLong("id"), pts, tags)
                    else if (!NOT_ARM.matches(tags.optString("highway"))) armWays += W(e.getLong("id"), pts, tags)
                }
            }
        }
        fun key(p: LL) = Math.round(p.lat * 1e7) * 31 + Math.round(p.lon * 1e7)
        // ways sharing an end are pieces of one ring
        val parent = IntArray(ringWays.size) { it }
        fun find(x: Int): Int { var a = x; while (parent[a] != a) { parent[a] = parent[parent[a]]; a = parent[a] }; return a }
        val byEnd = HashMap<Long, Int>()
        for ((i, w) in ringWays.withIndex()) for (end in listOf(w.pts.first(), w.pts.last())) {
            val k = key(end)
            val j = byEnd[k]
            if (j == null) byEnd[k] = i else parent[find(i)] = find(j)
        }
        for ((_, idx) in ringWays.indices.groupBy { find(it) }) {
            val ps = idx.map { ringWays[it].pts }
            val all = ps.flatten()
            // the middle: the centre of its outline (an even spread of points isn't guaranteed, so not their average)
            val c = LL((all.minOf { it.lat } + all.maxOf { it.lat }) / 2.0, (all.minOf { it.lon } + all.maxOf { it.lon }) / 2.0)
            val onRing = all.map { key(it) }.toHashSet()
            val raw = ArrayList<Arm>()
            for (w in armWays) for ((k, pt) in w.pts.withIndex()) if (key(pt) in onRing) {
                // the road leaves the ring here (a road that only touches the ring at a point along it counts once each way)
                if (k > 0 && key(w.pts[k - 1]) !in onRing) raw += Arm(angleAt(c, pt), w.tags)
                if (k < w.pts.size - 1 && key(w.pts[k + 1]) !in onRing) raw += Arm(angleAt(c, pt), w.tags)
            }
            val id = idx.minOf { ringWays[it].id }
            rings[id] = Ring(id, c, ps, false, groupArms(raw))
        }
        for ((nid, pt) in minis) {
            val k0 = key(pt)
            val raw = ArrayList<Arm>()
            // a mini roundabout is one point: each road through it is an arm each way, pointing at its next point
            for (w in armWays) for ((k, q2) in w.pts.withIndex()) if (key(q2) == k0) {
                if (k > 0) raw += Arm(angleAt(pt, w.pts[k - 1]), w.tags)
                if (k < w.pts.size - 1) raw += Arm(angleAt(pt, w.pts[k + 1]), w.tags)
            }
            rings[-nid] = Ring(-nid, pt, emptyList(), true, groupArms(raw))
        }
        fetchedFor = route
        lastFetch = "looked up ${missing.size} of ${at.size} on the route: ${ringWays.size} ring ways, ${minis.size} minis, ${armWays.size} roads; ${rings.size} kept"
        save(ctx)
    }

    /** What the last lookup and the last measure did, for the feedback report. */
    @Volatile var lastFetch = ""
    @Volatile var lastNote = ""

    /** Roads that aren't arms: paths, tracks, access roads (a petrol station's way in isn't counted as an exit). */
    private val NOT_ARM = Regex("^(footway|cycleway|path|steps|pedestrian|track|bridleway|corridor|construction|proposed|service|platform|bus_stop|busway|elevator|escape|raceway|services|rest_area)$")

    private class Arm(val angle: Double, val tags: JSONObject)

    /** Angle of [p] round [c] in degrees (0 east, 90 north), from local metres. */
    private fun angleAt(c: LL, p: LL): Double {
        val kx = 111320.0 * cos(Math.toRadians(c.lat))
        return Math.toDegrees(atan2((p.lat - c.lat) * 110540.0, (p.lon - c.lon) * kx))
    }

    /**
     * The arms round a ring, with the two carriageways of a divided road (one-way, the same name or number, close together)
     * taken as one. Returns their angles in order round the ring.
     */
    private fun groupArms(raw: List<Arm>): List<Double> {
        if (raw.isEmpty()) return emptyList()
        val a = raw.sortedBy { (it.angle + 360.0) % 360.0 }
        fun gap(x: Arm, y: Arm) = (((y.angle - x.angle) % 360.0) + 360.0) % 360.0
        fun pair(x: Arm, y: Arm): Boolean {
            val g = gap(x, y)
            if (g > 60.0) return false
            val nx = x.tags.optString("name"); val ny = y.tags.optString("name")
            val rx = x.tags.optString("ref"); val ry = y.tags.optString("ref")
            val same = (nx.isNotBlank() && nx == ny) || (rx.isNotBlank() && rx == ry)
            val oneWay = x.tags.optString("oneway") == "yes" && y.tags.optString("oneway") == "yes"
            return same || (oneWay && g <= 50.0) || g < 12.0
        }
        // (an arm is at most its two carriageways: three roads in a row aren't run together)
        val groups = ArrayList<MutableList<Arm>>()
        for (arm in a) { val g = groups.lastOrNull(); if (g != null && g.size < 2 && pair(g.last(), arm)) g += arm else groups += mutableListOf(arm) }
        if (groups.size > 1 && groups.last().size < 2 && groups.first().size < 2 && pair(groups.last().last(), groups.first().first())) {
            groups.first().addAll(0, groups.last()); groups.removeAt(groups.size - 1)
        }
        // the arm sits in the middle of its carriageways: the way in meets the ring a little past that, the way out a little before
        return groups.map { g -> g.first().angle + gap(g.first(), g.last()) / 2.0 }
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

    /** A roundabout as the turn card draws it: your [turn] (-90 a left, 0 straight on, 90 a right, 180 right round) and the other arms' turns. */
    class Turn(val turn: Double, val others: List<Double>)

    /**
     * The roundabout whose step is at [entry] metres along the route: the arm you come in on and the arm you leave by, as angles
     * round the ring's middle, to the nearest eighth of a turn. Each arm sits in the middle of its carriageways (from the map);
     * how the road leads in or out plays no part. Null when no ring is kept for it or the route doesn't touch it, so the
     * line-based measure is used instead.
     */
    fun turnAt(ctx: Context, route: Route, entry: Double, leftTraffic: Boolean): Turn? {
        load(ctx)
        if (route.pts.size < 2) return null
        val len = route.cum.last()
        val stepPt = Geo.pointOnLine(route.pts, route.cum, entry.coerceIn(0.0, len)).first
        val ring = ringNear(stepPt) ?: run { lastNote = "no ring kept near the turn (${rings.size} kept)"; return null }
        val c = ring.centre
        val kx = 111320.0 * cos(Math.toRadians(c.lat)); val ky = 110540.0
        fun xy(p: LL) = doubleArrayOf((p.lon - c.lon) * kx, (p.lat - c.lat) * ky)
        fun ang(p: LL): Double { val v = xy(p); return Math.toDegrees(atan2(v[1], v[0])) }
        val from = max(0.0, entry - 80.0)
        val to = min(len, entry + 300.0)
        if (to <= from + 5.0) return null
        val pIn: LL; val pOut: LL
        var onRing = 0.0
        if (ring.mini) {
            // a mini roundabout: where the route is 15 m before and after its point
            var bestO = -1.0; var bestD = Double.MAX_VALUE
            var o = from
            while (o <= to) { val d = Geo.dist(Geo.pointOnLine(route.pts, route.cum, o).first, c); if (d < bestD) { bestD = d; bestO = o }; o += 2.0 }
            if (bestO < 0 || bestD > 25.0) { lastNote = "mini roundabout ${Math.round(bestD)} m off the route"; return null }
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
            var join = -1.0; var leave = -1.0; var nearest = Double.MAX_VALUE
            var o = from
            while (o <= to) {
                val d = distToRing(Geo.pointOnLine(route.pts, route.cum, o).first)
                nearest = min(nearest, d)
                if (d < 8.0) { if (join < 0) join = o; leave = o } else if (join >= 0 && o - leave > 12.0) break
                o += 2.0
            }
            if (join < 0 || leave - join < 4.0) { lastNote = "route doesn't run on the ring (nearest ${Math.round(nearest)} m)"; return null }
            onRing = leave - join
            // a little way out along each arm (where the route is 15 m outside the ring, at most 40 m along it), to tell which arm it is
            val outR = ring.radius + ARM_OUT_M
            var oi = join
            while (oi > max(0.0, join - ARM_MAX_M) && Geo.dist(Geo.pointOnLine(route.pts, route.cum, oi).first, c) < outR) oi -= 2.0
            var oo = leave
            while (oo < min(len, leave + ARM_MAX_M) && Geo.dist(Geo.pointOnLine(route.pts, route.cum, oo).first, c) < outR) oo += 2.0
            pIn = Geo.pointOnLine(route.pts, route.cum, max(0.0, oi)).first
            pOut = Geo.pointOnLine(route.pts, route.cum, min(len, oo)).first
        }
        val aIn = ang(pIn); val aOut = ang(pOut)
        fun off(a: Double, b: Double) = Math.abs(((a - b + 540.0) % 360.0) - 180.0)
        val arms = ring.arms.orEmpty()
        val armIn = arms.minByOrNull { off(it, aIn) }?.takeIf { off(it, aIn) < 50.0 }
        val armOut = arms.minByOrNull { off(it, aOut) }?.takeIf { off(it, aOut) < 50.0 }
        val byArms = armIn != null && armOut != null
        val a0 = if (byArms) armIn!! else aIn
        val a1 = if (byArms) armOut!! else aOut
        // how far round from the way in, in the ring's direction
        fun round(a: Double) = if (leftTraffic) ((a0 - a) % 360.0 + 360.0) % 360.0 else ((a - a0) % 360.0 + 360.0) % 360.0
        val circ = 2.0 * Math.PI * max(ring.radius, 5.0)
        val raw = round(a1)
        val whole = (byArms && armIn == armOut) || ((raw < 30.0 || raw > 330.0) && (ring.mini || onRing > circ * 0.5))
        fun eighth(a: Double) = (Math.round(a / 45.0) * 45.0)
        val arc = if (whole) 360.0 else eighth(raw).coerceIn(45.0, 315.0)
        fun turnOf(arcDeg: Double) = if (leftTraffic) arcDeg - 180.0 else 180.0 - arcDeg
        val others = if (!byArms) emptyList() else arms.filter { it != armIn && it != armOut }
            .map { eighth(round(it)) }.filter { it in 45.0..315.0 && it != arc }.distinct().map { turnOf(it) }
        lastNote = (if (ring.mini) "mini" else "ring r=${Math.round(ring.radius)} m") + ", ${arms.size} arms at " + arms.joinToString("/") { Math.round((it + 360.0) % 360.0).toString() } +
            "; route in ${Math.round((aIn + 360.0) % 360.0)} out ${Math.round((aOut + 360.0) % 360.0)}; " + (if (byArms) "by arms" else "by route (no arm matched)") +
            "; round ${Math.round(raw)} shown ${Math.round(arc)}"
        return Turn(turnOf(arc), others)
    }
}
