package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.max

/**
 * Average speed checks from OpenStreetMap (relations tagged enforcement=average_speed), looked up in a small request of their
 * own and kept on the device, so a road driven before shows its check straight away, also without signal.
 *
 * They used to come only with the big search for bumps, schools and crossings along the route: the slowest request and the most
 * likely to fail, so the cameras at both ends of a check showed (they come another way) while the check itself never started.
 */
object AvgZones {
    /** A box as south, west, north, east. */
    fun boxOf(line: List<LL>, padLat: Double = 0.01, padLon: Double = 0.015): DoubleArray? {
        if (line.isEmpty()) return null
        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
        for (p in line) { if (p.lat < minLat) minLat = p.lat; if (p.lat > maxLat) maxLat = p.lat; if (p.lon < minLon) minLon = p.lon; if (p.lon > maxLon) maxLon = p.lon }
        return doubleArrayOf(minLat - padLat, minLon - padLon, maxLat + padLat, maxLon + padLon)
    }

    fun inBox(p: LL, b: DoubleArray): Boolean = p.lat >= b[0] && p.lat <= b[2] && p.lon >= b[1] && p.lon <= b[3]

    /** The checks in [box], asked on their own (blocking; throws when no server answers). The servers can take half a minute for it. */
    fun fetch(box: DoubleArray): List<AvgDef> {
        val bb = String.format(java.util.Locale.US, "%.5f,%.5f,%.5f,%.5f", box[0], box[1], box[2], box[3])
        val els = Overpass.query("[out:json][timeout:45];rel($bb)[\"enforcement\"=\"average_speed\"]->.r;.r out body;node(r.r);out skel qt;", 12000, 50000, 3)
        val nodes = HashMap<Long, LL>()
        val rels = ArrayList<JSONObject>()
        for (i in 0 until els.length()) {
            val e = els.getJSONObject(i)
            when (e.optString("type")) {
                "relation" -> rels += e
                "node" -> if (e.has("lat")) nodes[e.optLong("id")] = LL(e.getDouble("lat"), e.getDouble("lon"))
            }
        }
        return parse(rels, nodes)
    }

    private fun parse(rels: List<JSONObject>, nodes: Map<Long, LL>): List<AvgDef> {
        val out = ArrayList<AvgDef>()
        for (rel in rels) {
            val ms = rel.optJSONArray("members") ?: continue
            val from = ArrayList<LL>(); val to = ArrayList<LL>(); val dev = ArrayList<LL>()
            for (k in 0 until ms.length()) {
                val m = ms.getJSONObject(k)
                if (m.optString("type") != "node") continue
                val p = nodes[m.optLong("ref")] ?: continue
                when (m.optString("role")) { "from" -> from += p; "to" -> to += p; else -> dev += p }
            }
            val t = rel.optJSONObject("tags") ?: JSONObject()
            val starts = from.ifEmpty { dev }; val ends = to.ifEmpty { dev }
            if (starts.isEmpty() || ends.isEmpty()) continue
            // the check's limit is in the units of the country it stands in (mph in Britain and the US, km/h elsewhere), worked out
            // from where it is, never from the device's language setting
            val lim = t.optString("maxspeed").takeIf { it.isNotBlank() }?.let { Overpass.parseSpeed(it, RoadRefs.unitsCountry(starts.first())) }
            out += AvgDef(rel.optLong("id"), t.optString("name"), starts, ends, lim, dev)
        }
        return out
    }

    // ---- kept on the device ----

    private const val FILE = "avgzones.json"
    private const val MAX_STORED = 3000
    private val lock = Any()
    @Volatile private var cache: List<AvgDef>? = null

    private fun pts(a: List<LL>) = JSONArray().apply { a.forEach { put(JSONArray().put(it.lat).put(it.lon)) } }
    private fun pts(a: JSONArray?): List<LL> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { i -> a.optJSONArray(i)?.let { LL(it.getDouble(0), it.getDouble(1)) } }

    private fun all(ctx: android.content.Context): List<AvgDef> = synchronized(lock) {
        cache ?: run {
            val l = try {
                val a = JSONArray(File(ctx.filesDir, FILE).readText())
                (0 until a.length()).mapNotNull { i ->
                    val o = a.optJSONObject(i) ?: return@mapNotNull null
                    val s = pts(o.optJSONArray("s")); val e = pts(o.optJSONArray("e"))
                    if (s.isEmpty() || e.isEmpty()) null
                    else AvgDef(o.optLong("id"), o.optString("n"), s, e, if (o.has("l")) o.getDouble("l") else null, pts(o.optJSONArray("d")))
                }
            } catch (e: Exception) { emptyList() }
            cache = l
            l
        }
    }

    /** The checks kept on the device that start inside [box]. */
    fun stored(ctx: android.content.Context, box: DoubleArray): List<AvgDef> = all(ctx).filter { inBox(it.starts.first(), box) }

    /** A fresh answer for [box] replaces what was kept there: new checks are added, and ones no longer in OpenStreetMap inside the box go. */
    fun save(ctx: android.content.Context, box: DoubleArray, fresh: List<AvgDef>) = synchronized(lock) {
        val old = all(ctx)
        val now = merge(old, box, fresh).takeLast(MAX_STORED)
        if (same(old, now)) return@synchronized
        cache = now
        try {
            val a = JSONArray()
            for (d in now) a.put(JSONObject().put("id", d.id).put("n", d.name).put("s", pts(d.starts)).put("e", pts(d.ends)).put("d", pts(d.devices)).apply { d.limit?.let { put("l", it) } })
            val tmp = File(ctx.filesDir, "$FILE.tmp")
            tmp.writeText(a.toString())
            tmp.renameTo(File(ctx.filesDir, FILE))
        } catch (e: Exception) { softFail(e) }
    }

    private fun same(a: List<AvgDef>, b: List<AvgDef>): Boolean =
        a.size == b.size && a.indices.all { i -> a[i].id == b[i].id && a[i].limit == b[i].limit && a[i].starts == b[i].starts && a[i].ends == b[i].ends }

    /** [old] with the part inside [box] replaced by [fresh] (checks are told apart by their OpenStreetMap id). */
    fun merge(old: List<AvgDef>, box: DoubleArray, fresh: List<AvgDef>): List<AvgDef> {
        val ids = fresh.mapTo(HashSet()) { it.id }
        return old.filter { it.id !in ids && !inBox(it.starts.first(), box) } + fresh
    }
}

/** Whether [p] is one of this check's points: its start, its end or one of its cameras. */
fun AvgDef.near(p: LL, m: Double): Boolean = starts.any { Geo.dist(it, p) < m } || ends.any { Geo.dist(it, p) < m } || devices.any { Geo.dist(it, p) < m }

/**
 * An average speed check while driving without a destination: there is no route to measure along, so the check starts when you
 * pass its start heading towards its end, is measured by the distance driven, and ends at its end point.
 */
class FreeAvg {
    /** The check you are in: when and at what driven distance it began, and its length as the crow flies (the road is a little longer). */
    class In(val def: AvgDef, val enterAt: Long, val enterDriven: Double, val lengthM: Double)
    /** What one position did: a check [entered], the one you were in [left] at its end, or one [ahead] to warn about ([aheadM] metres on). */
    class Tick(val entered: AvgDef? = null, val left: Boolean = false, val ahead: AvgDef? = null, val aheadM: Double = 0.0)

    var cur: In? = null
        private set
    private val warned = HashMap<Long, Long>()

    fun reset() { cur = null }

    private fun ang(a: Double, b: Double): Double = abs(((a - b + 540.0) % 360.0 + 360.0) % 360.0 - 180.0)

    /** The check's start nearest to [pos], and the end furthest from that start (the far end of the stretch). */
    private fun ends(d: AvgDef, pos: LL): Pair<LL, LL> {
        val s = d.starts.minByOrNull { Geo.dist(it, pos) }!!
        return s to d.ends.maxByOrNull { Geo.dist(it, s) }!!
    }

    fun update(pos: LL, heading: Double, speed: Double, driven: Double, now: Long, defs: List<AvgDef>): Tick {
        val c = cur
        if (c != null) {
            val gone = driven - c.enterDriven
            if (gone > 300.0 && c.def.ends.any { Geo.dist(it, pos) < REACH }) { cur = null; return Tick(left = true) }
            // turned off the road, or the end was never passed: given up quietly
            if (gone > c.lengthM * 1.6 + 1000.0 || now - c.enterAt > 3 * 3600_000L) cur = null
            return Tick()
        }
        if (speed < 3.0) return Tick()
        var ahead: AvgDef? = null; var aheadM = Double.MAX_VALUE
        for (d in defs) {
            val (s, e) = ends(d, pos)
            val len = Geo.dist(s, e)
            if (len < 400.0 || len > 60_000.0) continue
            val ds = Geo.dist(pos, s)
            // the other direction has its own check, with its start where this one ends: only the one you are heading along counts
            if (ds < REACH) {
                if (ang(heading, Geo.bearing(pos, e)) > 100.0) continue
                cur = In(d, now, driven, len)
                warned[d.id] = now
                return Tick(entered = d)
            }
            if (ds < max(500.0, speed * 18.0) && ds < aheadM && ang(heading, Geo.bearing(pos, s)) < 40.0 && ang(heading, Geo.bearing(s, e)) < 100.0 &&
                (warned[d.id]?.let { now - it > 10 * 60_000L } ?: true)) { ahead = d; aheadM = ds }
        }
        if (ahead != null) { warned[ahead.id] = now; return Tick(ahead = ahead, aheadM = aheadM) }
        return Tick()
    }

    companion object {
        /** How near a start or end point counts as passing it (a position a second at motorway speed is 35 m apart). */
        const val REACH = 70.0
    }
}
