package com.headunit.launcher

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/**
 * The satellite photos' own store on the phone, for the state surveying offices' photos and EOX's Sentinel-2 (both free to keep;
 * Mapbox, Esri and MapTiler aren't stored, their terms don't allow it). The map asks a tiny web server on the phone itself
 * (127.0.0.1, unreachable from outside) for each tile: a stored one comes from the phone, any other from the internet and is kept.
 * Photos along a route can be fetched ahead (Settings > Satellitenbilder). The store holds up to 500 MB; the photos used
 * longest ago go first.
 */
object SatTileCache {
    const val MAX_BYTES = 500L * 1024 * 1024
    /** How far either side of the route photos are fetched ahead, and down to which zoom. */
    const val ROUTE_SIDE_M = 300.0
    const val ROUTE_STATE_ZOOMS = 12
    const val ROUTE_MAX_ZOOM = 16
    const val EOX_MIN_ZOOM = 8
    const val EOX_MAX_ZOOM = 13

    @Volatile private var server: ServerSocket? = null
    @Volatile private var port = 0
    @Volatile private var dir: File? = null
    private val size = AtomicLong(-1)
    private val serving = Executors.newFixedThreadPool(6) { r -> Thread(r, "sat-tiles").apply { isDaemon = true } }
    private val upkeep = Executors.newSingleThreadExecutor { r -> Thread(r, "sat-upkeep").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
    private val generation = AtomicInteger(0)

    /** Progress of fetching a route's photos ahead, for Settings: done / total (0 / 0 when nothing is being fetched). */
    @Volatile var prefetchDone = 0; private set
    @Volatile var prefetchTotal = 0; private set

    private fun folder(ctx: Context): File = dir ?: File(ctx.applicationContext.filesDir, "sattiles").also { it.mkdirs(); dir = it }

    /** The address the map asks for a source's tiles ("eox" or "de-<state>"), or null when the server can't run (then direct). */
    fun tileUrl(ctx: Context, src: String): String? = try { "http://127.0.0.1:${start(ctx)}/t/$src/{z}/{x}/{y}" } catch (e: Exception) { null }

    @Synchronized
    private fun start(ctx: Context): Int {
        server?.let { if (!it.isClosed) return port }
        folder(ctx)
        val s = ServerSocket(0, 32, InetAddress.getByName("127.0.0.1"))
        server = s; port = s.localPort
        Thread({
            while (!s.isClosed) {
                val c = try { s.accept() } catch (e: Exception) { break }
                try { serving.execute { serve(c) } } catch (e: Exception) { try { c.close() } catch (x: Exception) { } }
            }
        }, "sat-server").apply { isDaemon = true }.start()
        // the store's size, counted once in the background (then kept up to date as tiles are added)
        upkeep.execute { size.set(folderSize()); trim() }
        return port
    }

    // ---- where a tile comes from ----

    /** A tile's web address at its source, or null for an unknown source. */
    internal fun upstream(src: String, z: Int, x: Int, y: Int): String? = when {
        src == "eox" -> SatelliteSources.EOX_TILES.replace("{z}", "$z").replace("{x}", "$x").replace("{y}", "$y")
        src.startsWith("de-") -> SatelliteSources.states.firstOrNull { it.code == src.removePrefix("de-") }?.let {
            SatelliteSources.wmsTiles(it).replace("{bbox-epsg-3857}", bbox(z, x, y))
        }
        else -> null
    }

    /** A tile's box in web-Mercator metres (west,south,east,north), as a WMS asks for it. */
    internal fun bbox(z: Int, x: Int, y: Int): String {
        val half = PI * 6378137.0
        val res = 2 * half / (1 shl z)
        val west = -half + x * res
        val north = half - y * res
        return String.format(java.util.Locale.US, "%.3f,%.3f,%.3f,%.3f", west, north - res, west + res, north)
    }

    private fun file(src: String, z: Int, x: Int, y: Int): File? {
        val d = dir ?: return null
        if (!src.matches(Regex("[a-z-]{2,8}"))) return null
        return File(d, "$src/$z/$x/$y")
    }

    private class Answer(val code: Int, val body: ByteArray?)

    /** The tile from the phone, or fetched (and kept). */
    private fun tile(src: String, z: Int, x: Int, y: Int): Answer {
        if (z !in 0..22 || x < 0 || y < 0 || x >= (1 shl z) || y >= (1 shl z)) return Answer(404, null)
        val f = file(src, z, x, y) ?: return Answer(404, null)
        if (f.isFile) try {
            val b = f.readBytes()
            // used now: kept longer than photos not looked at for a while
            f.setLastModified(System.currentTimeMillis())
            return Answer(200, b)
        } catch (e: Exception) { }
        return fetch(src, z, x, y, f)
    }

    private fun fetch(src: String, z: Int, x: Int, y: Int, f: File): Answer {
        val url = upstream(src, z, x, y) ?: return Answer(404, null)
        var c: HttpURLConnection? = null
        return try {
            c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10_000; c.readTimeout = 20_000
            c.setRequestProperty("User-Agent", "HorizonNav (Android)")
            val code = c.responseCode
            if (code != 200) return Answer(code, null)
            val b = c.inputStream.use { it.readBytes() }
            if (b.isEmpty() || !isImage(b)) return Answer(502, null)
            keep(f, b)
            Answer(200, b)
        } catch (e: Exception) { Answer(504, null) } finally { try { c?.disconnect() } catch (e: Exception) { } }
    }

    private fun isImage(b: ByteArray) = b.size > 4 && ((b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte()) || (b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte()))

    private fun keep(f: File, b: ByteArray) {
        try {
            f.parentFile?.mkdirs()
            val tmp = File(f.parentFile, f.name + ".part")
            tmp.writeBytes(b)
            if (!tmp.renameTo(f)) { tmp.delete(); return }
            if (size.get() >= 0 && size.addAndGet(b.size.toLong()) > MAX_BYTES) upkeep.execute { trim() }
        } catch (e: Exception) { }
    }

    // ---- the web server the map asks ----

    private fun serve(c: Socket) {
        c.use { sock ->
            try {
                sock.soTimeout = 10_000
                val inp = sock.getInputStream().bufferedReader(Charsets.ISO_8859_1)
                val first = inp.readLine()?.take(2048) ?: return
                var n = 0
                while (n++ < 64) { val l = inp.readLine() ?: break; if (l.isEmpty() || l.length > 8192) break }
                val path = first.split(' ').getOrNull(1)?.substringBefore('?') ?: ""
                val parts = path.removePrefix("/t/").split('/')
                val a = if (path.startsWith("/t/") && parts.size == 4) {
                    val z = parts[1].toIntOrNull(); val x = parts[2].toIntOrNull(); val y = parts[3].toIntOrNull()
                    if (z != null && x != null && y != null) tile(parts[0], z, x, y) else Answer(404, null)
                } else Answer(404, null)
                val out = sock.getOutputStream()
                val body = a.body
                if (body == null) {
                    // the source's own answer (429, 403...) is passed on, so the map can move on to another source
                    out.write("HTTP/1.1 ${a.code} Error\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray())
                } else {
                    val type = if (body[0] == 0x89.toByte()) "image/png" else "image/jpeg"
                    out.write("HTTP/1.1 200 OK\r\nContent-Type: $type\r\nContent-Length: ${body.size}\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n".toByteArray())
                    out.write(body)
                }
                out.flush()
            } catch (e: Exception) { }
        }
    }

    // ---- size ----

    private fun allFiles(): List<File> = dir?.walkTopDown()?.filter { it.isFile && !it.name.endsWith(".part") }?.toList() ?: emptyList()
    private fun folderSize(): Long = allFiles().sumOf { it.length() }

    /** Over 500 MB: the photos used longest ago are deleted until it's down to 90%. */
    private fun trim() {
        if (size.get() <= MAX_BYTES) return
        val files = allFiles().sortedBy { it.lastModified() }
        var total = files.sumOf { it.length() }
        for (f in files) {
            if (total <= MAX_BYTES * 9 / 10) break
            val l = f.length()
            if (f.delete()) total -= l
        }
        size.set(total)
    }

    /** The store's size in bytes (counted on first use, so 0 until the map has asked for a photo since the app started). */
    fun sizeBytes(ctx: Context): Long {
        folder(ctx)
        val s = size.get()
        return if (s >= 0) s else folderSize().also { size.compareAndSet(-1, it) }
    }

    /** Empties the store (Settings > Satellitenbilder). */
    fun clear(ctx: Context) {
        generation.incrementAndGet()
        upkeep.execute {
            try { folder(ctx).listFiles()?.forEach { it.deleteRecursively() } } catch (e: Exception) { }
            size.set(0)
        }
    }

    // ---- fetching a route's photos ahead ----

    /** The tiles within [sideM] of the route at zoom [z], as (x, y) pairs. */
    internal fun corridor(pts: List<LL>, z: Int, sideM: Double): Set<Long> {
        val out = HashSet<Long>()
        if (pts.isEmpty()) return out
        val n = 1 shl z
        fun add(p: LL) {
            val tileM = 40075016.686 * cos(Math.toRadians(p.lat)) / n
            val r = sideM / tileM
            val fx = (p.lon + 180.0) / 360.0 * n
            val latR = Math.toRadians(p.lat)
            val fy = (1.0 - ln(tan(latR) + 1.0 / cos(latR)) / PI) / 2.0 * n
            for (x in floor(fx - r).toInt()..floor(fx + r).toInt()) for (y in floor(fy - r).toInt()..floor(fy + r).toInt()) {
                if (x in 0 until n && y in 0 until n) out.add(x.toLong() shl 32 or y.toLong())
            }
        }
        // points along the route no further apart than half a tile (or 100 m), so no tile in between is missed
        val stepM = minOf(100.0, 40075016.686 * cos(Math.toRadians(pts[0].lat)) / n / 2)
        add(pts[0])
        for (i in 1 until pts.size) {
            val a = pts[i - 1]; val b = pts[i]
            val d = Geo.dist(a, b)
            val k = (d / stepM).toInt()
            for (j in 1..k) { val t = j.toDouble() / (k + 1); add(LL(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t)) }
            add(b)
        }
        return out
    }

    /** Which sources hold a tile: the states whose area covers the tile's middle. */
    private fun statesAt(z: Int, x: Int, y: Int): List<String> {
        val n = (1 shl z).toDouble()
        val lon = (x + 0.5) / n * 360.0 - 180.0
        val lat = Math.toDegrees(kotlin.math.atan(kotlin.math.sinh(PI * (1 - 2 * (y + 0.5) / n))))
        return SatelliteSources.states.filter { lon in it.west..it.east && lat in it.south..it.north }.map { "de-${it.code}" }
    }

    /**
     * Fetches the photos along a route ahead, in the background (a new route stops the previous one): the state offices' photos
     * from zoom 12 to 16 when they're shown, EOX's from zoom 8 to 13 when EOX is shown.
     * [stillAllowed] is asked now and then (e.g. still on Wi-Fi); when it says no, fetching stops.
     */
    fun prefetchRoute(ctx: Context, pts: List<LL>, states: Boolean, eox: Boolean, stillAllowed: () -> Boolean) {
        if (pts.size < 2 || (!states && !eox)) return
        val gen = generation.incrementAndGet()
        try { start(ctx) } catch (e: Exception) { return }
        Thread({
            try {
                val jobs = ArrayList<Triple<String, Int, Long>>()
                if (eox) for (z in EOX_MIN_ZOOM..EOX_MAX_ZOOM) for (t in corridor(pts, z, ROUTE_SIDE_M)) jobs += Triple("eox", z, t)
                if (states) for (z in ROUTE_STATE_ZOOMS..ROUTE_MAX_ZOOM) for (t in corridor(pts, z, ROUTE_SIDE_M)) {
                    for (s in statesAt(z, (t shr 32).toInt(), (t and 0xFFFFFFFFL).toInt())) jobs += Triple(s, z, t)
                }
                prefetchTotal = jobs.size; prefetchDone = 0
                android.util.Log.w("SatTileCache", "route photos: ${jobs.size} tiles (states=$states, eox=$eox)")
                val pool = Executors.newFixedThreadPool(3) { r -> Thread(r, "sat-prefetch").apply { isDaemon = true; priority = Thread.MIN_PRIORITY } }
                val done = AtomicInteger(0)
                for (j in jobs) pool.execute {
                    if (generation.get() != gen) return@execute
                    val x = (j.third shr 32).toInt(); val y = (j.third and 0xFFFFFFFFL).toInt()
                    if (done.get() % 50 == 0 && !stillAllowed()) { generation.compareAndSet(gen, gen + 1); return@execute }
                    val f = file(j.first, j.second, x, y)
                    if (f != null && !f.isFile) fetch(j.first, j.second, x, y, f)
                    prefetchDone = done.incrementAndGet()
                }
                pool.shutdown()
                pool.awaitTermination(6, java.util.concurrent.TimeUnit.HOURS)
                android.util.Log.w("SatTileCache", "route photos: done ${done.get()} of ${jobs.size}")
            } catch (e: Throwable) { android.util.Log.w("SatTileCache", "route photos: ${e.message}") }
            finally { if (generation.get() == gen || generation.get() == gen + 1) { prefetchTotal = 0; prefetchDone = 0 } }
        }, "sat-prefetch-plan").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
    }
}
