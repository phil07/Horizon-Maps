package com.headunit.launcher

import android.content.Context
import android.os.Handler
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point

/**
 * The traffic lights along the route (OpenStreetMap, or the offline pack), found in the background. One search per stretch of road
 * returns the lights and the speed cameras together; a reroute only searches the roads not covered yet.
 *
 * Only the lights that stand on the road you follow are shown: [show] gets them on the main thread, with the 3D models (empty when
 * [on] is off) and the lights kept on the route. [onCameras] gets every speed camera found so far.
 */
internal class TrafficLights(
    private val ctx: Context,
    private val ui: Handler,
    private val drawnRoute: () -> List<LL>,
    private val destroyed: () -> Boolean,
    private val show: (FeatureCollection, FeatureCollection, List<LL>) -> Unit,
    private val onCameras: (List<LL>) -> Unit,
) {
    @Volatile var on = true
    /** How many lights the last update put on the map. */
    @Volatile var shown = 0
        private set
    /** How many lights were found along the route. */
    val found: Int get() = pts.size
    /** The last search's error (empty when it worked). */
    @Volatile var error = ""
        private set
    /** What the search is doing, for the settings page and the performance meter. */
    @Volatile var stage = Lang.t("nicht gestartet", "not started")
        private set
    /** The lights shown on the route (the first one at each junction). */
    @Volatile var kept: List<LL> = emptyList()
        private set

    @Volatile private var pts: List<LL> = emptyList()
    @Volatile private var token = 0
    @Volatile private var busy = false
    @Volatile private var failAt = 0L
    private var sigRoute: List<LL>? = null
    /** Every light found so far (kept across reroutes, so a new route only fetches the roads not already covered). */
    private val sigAll = LinkedHashMap<Long, LL>()
    /** Small grid cells (about 20 m) of road that have already been searched for lights. */
    private val sigCovered = HashSet<Long>()
    private val sigLock = Any()
    private val routeGrid = RouteGrid()
    /** Speed cameras found along the route by the same search. */
    private val camAll = LinkedHashMap<Long, LL>()
    /** Which way the route runs at each light (for turning its 3D model to face you). */
    private val heading = HashMap<LL, Double>()
    @Volatile private var models: FeatureCollection = empty()

    // working out which lights sit on the route is done on its own thread (a long route can have thousands to check)
    private val exec = java.util.concurrent.Executors.newSingleThreadExecutor { r -> Thread(r, "lights").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
    @Volatile private var job = false
    @Volatile private var dirty = false

    /** Stops the background thread (the map is being destroyed). */
    fun close() = exec.shutdownNow()

    /** Puts the lights for the current [on] state and route on the map; changes that come meanwhile are folded into one more pass. */
    fun push() {
        if (destroyed()) return
        // one at a time; changes that arrive meanwhile are folded into one more pass when it finishes
        if (job) { dirty = true; return }
        job = true; dirty = false
        val drawing = on
        try {
            exec.execute {
                val fc = try { if (drawing) lightsOnRoute() else empty() } catch (e: Throwable) { empty() }
                ui.post {
                    if (destroyed()) { job = false; return@post }
                    try {
                        shown = fc.features()?.size ?: 0
                        show(fc, if (drawing) models else empty(), kept)
                    } catch (e: Throwable) { softFail(e) }
                    job = false
                    if (dirty) push()
                }
            }
        } catch (e: Throwable) { job = false }
    }

    /** Called as the map moves: only restarts the search along the route if an earlier one gave up (no signal). */
    fun refresh() {
        if (busy) return
        val r = sigRoute ?: return
        if (error.isNotEmpty() && android.os.SystemClock.uptimeMillis() - failAt > 30_000L) { sigRoute = null; load(r) }
    }

    /** A new route: its lights are searched for. */
    fun start(route: List<LL>) = load(route)

    /** No route: the search stops and nothing is waited for. */
    fun clearRoute() {
        sigRoute = null; token++; busy = false; stage = Lang.t("keine Route", "no route")
    }

    // only the lights on the road you are following are drawn: the route is turned into a grid of small cells, each knowing which
    // bits of the route pass through it, so finding the route near a light is a quick look at its own and neighbouring cells
    private fun lightsOnRoute(): FeatureCollection {
        val r = drawnRoute()
        models = empty(); kept = emptyList()
        if (r.size < 2) return empty()
        routeGrid.ensure(r)
        // only lights that stand on the route's own road (within ~10 m of the line), and at each junction only the first one you reach:
        // a junction usually has a light on every arm, but only the one on your approach matters
        val near = pts.filter { routeGrid.onRoute(it) }
        if (near.isEmpty()) return empty()
        val cum = DoubleArray(r.size)
        for (i in 1 until r.size) cum[i] = cum[i - 1] + Geo.dist(r[i - 1], r[i])
        val placed = ArrayList<Pair<Double, LL>>()
        val segs = HashSet<Int>()
        heading.clear()
        for (p in near) {
            segs.clear()
            routeGrid.segmentsNear(p, segs)
            val kx = Math.cos(Math.toRadians(p.lat)) * 111320.0; val ky = 110574.0
            var bd = 10.0; var bo = -1.0; var bi = -1
            for (i in segs) {
                val ax = (r[i].lon - p.lon) * kx; val ay = (r[i].lat - p.lat) * ky
                val bx = (r[i + 1].lon - p.lon) * kx; val by = (r[i + 1].lat - p.lat) * ky
                val dx = bx - ax; val dy = by - ay
                val l2 = dx * dx + dy * dy
                val t = if (l2 > 0) (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0) else 0.0
                val d = Math.hypot(ax + dx * t, ay + dy * t)
                if (d < bd) { bd = d; bo = cum[i] + (cum[i + 1] - cum[i]) * t; bi = i }
            }
            if (bo >= 0) { placed += bo to p; heading[p] = Geo.bearing(r[bi], r[bi + 1]) }
        }
        placed.sortBy { it.first }
        val keep = ArrayList<Feature>()
        val keepPts = ArrayList<LL>()
        val mdl = ArrayList<Feature>()
        var lastOff = -1e9
        for ((off, p) in placed) {
            if (off - lastOff < 70.0) { lastOff = off; continue }   // same junction: you have already passed its first light
            lastOff = off
            keep += Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat))
            keepPts += p
            // the 3D light: at the left kerb, facing you as you come up to it
            val hd = heading[p]
            if (hd != null && mdl.size < 400) { val at = Furniture.offset(p, hd - 90.0, 4.5); mdl += Furniture.point(at.lat, at.lon, hd) }
            if (keep.size >= 3000) break
        }
        models = FeatureCollection.fromFeatures(mdl)
        kept = keepPts
        return FeatureCollection.fromFeatures(keep)
    }

    /**
     * Finds every traffic light along [route] in the background: the route is cut into stretches of about 20 km (the first one
     * starting where you are), and each is asked for in one go as a corridor 30 m either side of the road. Roads already searched
     * (from an earlier route) are skipped, so a reroute usually needs little or nothing new.
     */
    private fun load(route: List<LL>) {
        if (route.size < 2 || route === sigRoute) return
        sigRoute = route
        val my = ++token
        busy = true; error = ""; stage = Lang.t("suche Ampeln entlang der Route…", "finding lights along the route…")
        Thread {
            try {
                // points every ~50 m along the route
                val marks = ArrayList<LL>()
                marks += route[0]
                var acc = 0.0
                for (i in 1 until route.size) {
                    acc += Geo.dist(route[i - 1], route[i])
                    if (acc >= 50.0 || i == route.size - 1) { marks += route[i]; acc = 0.0 }
                }
                fun cellsAlong(a: LL, b: LL, into: MutableCollection<Long>) {
                    val steps = (Geo.dist(a, b) / 15.0).toInt()
                    for (k in 0..steps + 1) { val t = k / (steps + 1.0); into += RouteGrid.key(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t) }
                }
                // stretches of route not searched yet, at most 400 points (~20 km) each
                val chunks = ArrayList<List<LL>>()
                var cur = ArrayList<LL>()
                for (i in marks.indices) {
                    val covered = synchronized(sigLock) { sigCovered.contains(RouteGrid.key(marks[i].lat, marks[i].lon)) }
                    if (covered) { if (cur.size >= 2) chunks += cur; cur = ArrayList(); continue }
                    if (cur.isEmpty() && i > 0) cur += marks[i - 1]
                    cur += marks[i]
                    // about 7.5 km per request: long enough to be few requests, short enough for a busy server to manage
                    if (cur.size >= 150) { chunks += cur; cur = arrayListOf(marks[i]) }
                }
                if (cur.size >= 2) chunks += cur
                var usedPack = false
                var n = 0
                // stretches that couldn't be searched (a busy server): the search is tried again later (see refresh)
                var gaps = 0
                for (ch in chunks) {
                    if (my != token) return@Thread
                    n++
                    stage = Lang.t("suche Ampeln entlang der Route ($n von ${chunks.size})…", "finding lights along the route ($n of ${chunks.size})…")
                    val cells = HashSet<Long>()
                    for (i in 0 until ch.size - 1) cellsAlong(ch[i], ch[i + 1], cells)
                    var found: List<LL>? = null
                    var foundCams: List<LL> = emptyList()
                    var camsMissed = false
                    // a downloaded area covering this stretch: its traffic lights and cameras, straight from the files
                    val area = OfflineRouter.areaFor(ctx, ch.first(), ch.last()) ?: OfflineRouter.areaFor(ctx, ch[ch.size / 2])
                    val pd = area?.let { OfflineRouter.packDir(ctx, it) }
                    var s0 = 90.0; var n0 = -90.0; var w0 = 180.0; var e0 = -180.0
                    for (p in ch) { s0 = minOf(s0, p.lat); n0 = maxOf(n0, p.lat); w0 = minOf(w0, p.lon); e0 = maxOf(e0, p.lon) }
                    fun fromPack(name: String): List<LL>? {
                        if (pd == null || !java.io.File(pd, name).let { it.exists() && it.length() > 8 }) return null
                        return OfflineRouter.lamps(ctx, s0 - 0.0005, w0 - 0.0005, n0 + 0.0005, e0 + 0.0005, name, pd, 5000).map { a ->
                            LL(a[0], a[1]).also { c -> if (a.size > 2) Overpass.camDir[Overpass.camKey(c)] = a[2] }
                        }
                    }
                    val packLights = fromPack("signals.tsv")
                    val packCams = fromPack("cameras.tsv")
                    if (packLights != null || packCams != null) usedPack = true
                    if (packLights != null && packCams != null) { found = packLights; foundCams = packCams }
                    else for (attempt in 0 until 5) {
                        if (my != token) return@Thread
                        try {
                            val online = Overpass.routeFeatures(ch, 30)
                            found = packLights ?: online.first
                            foundCams = packCams ?: online.second
                            break
                        } catch (e: Throwable) {
                            error = (e.message ?: e.javaClass.simpleName).take(80)
                            // with the lights from the pack, only the cameras were missing: carry on without them (searched again later)
                            if (packLights != null) { found = packLights; camsMissed = true; break }
                            // a busy server: back off for longer each time (5, 10, 20, 30 s)
                            Thread.sleep(longArrayOf(5000L, 10000L, 20000L, 30000L, 30000L)[attempt])
                        }
                    }
                    if (found == null) { gaps++; failAt = android.os.SystemClock.uptimeMillis(); continue }
                    // keep only lights near this stretch (the offline search is a box, and the corridor search is a little generous)
                    val keep = found.filter { p ->
                        val k = RouteGrid.key(p.lat, p.lon)
                        var hit = false
                        for (dy in -1..1) for (dx in -1..1) if (cells.contains(k + dy * 1000003L + dx)) hit = true
                        hit
                    }
                    val snap: List<LL>
                    val camSnap: List<LL>
                    synchronized(sigLock) {
                        for (p in keep) sigAll[Math.round(p.lat * 1e6) * 1000003L + Math.round(p.lon * 1e6)] = p
                        for (p in foundCams) camAll[Math.round(p.lat * 1e6) * 1000003L + Math.round(p.lon * 1e6)] = p
                        while (camAll.size > 5000) camAll.remove(camAll.keys.first())
                        camSnap = ArrayList(camAll.values)
                        while (sigAll.size > 20000) sigAll.remove(sigAll.keys.first())
                        // (when the cameras couldn't be fetched, the stretch is searched again next time)
                        if (!camsMissed) sigCovered.addAll(cells)
                        if (sigCovered.size > 400_000) sigCovered.clear()
                        snap = ArrayList(sigAll.values)
                    }
                    if (my != token) return@Thread
                    if (camsMissed) { gaps++; failAt = android.os.SystemClock.uptimeMillis() }
                    if (gaps == 0) error = ""
                    ui.post {
                        if (my == token) { pts = snap; push() }
                        try { onCameras(camSnap) } catch (e: Throwable) { softFail(e) }
                    }
                }
                if (gaps > 0) { if (error.isEmpty()) error = "some stretches not searched"; failAt = android.os.SystemClock.uptimeMillis() }
                val snap = synchronized(sigLock) { ArrayList(sigAll.values) }
                ui.post {
                    if (my == token) {
                        pts = snap; push(); busy = false
                        stage = if (error.isEmpty()) "route searched (${if (usedPack) "downloaded area" else Overpass.lastNote})" else Lang.t("ein Teil der Route konnte nicht durchsucht werden, neuer Versuch folgt", "some of the route couldn't be searched, will retry")
                    }
                }
            } catch (e: Throwable) {
                error = "internal: " + (e.message ?: e.javaClass.simpleName).take(60); failAt = android.os.SystemClock.uptimeMillis()
                ui.post { if (my == token) busy = false }
            }
        }.apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
    }

    private fun empty() = FeatureCollection.fromFeatures(ArrayList<Feature>())
}
