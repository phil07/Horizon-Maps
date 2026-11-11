package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.Locale

/**
 * Street lamps as OpenStreetMap has them (node highway=street_lamp), fetched online once per area (a 0.02° square, about 2 km) and kept
 * for a month in the app's cache. The areas missing from the cache are fetched in one query for the whole block they cover, then split
 * up again per area. Only used with a connection; the lamps of an offline pack still count where there is one.
 */
object LampsOnline {
    private const val STEP = 0.02
    private const val MAX_AGE = 30L * 86_400_000L
    private const val MAX_TILES = 4

    private data class Key(val i: Int, val j: Int)

    fun connected(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true } == true
    } catch (e: Exception) { false }

    /** The lamps (lat, lon) inside the box, from the cache or fetched; [allowFetch] false means only what's already cached. */
    fun inBox(ctx: Context, south: Double, west: Double, north: Double, east: Double, allowFetch: Boolean): List<DoubleArray> {
        val i0 = Math.floor(south / STEP).toInt(); val i1 = Math.floor(north / STEP).toInt()
        val j0 = Math.floor(west / STEP).toInt(); val j1 = Math.floor(east / STEP).toInt()
        val areas = ArrayList<Key>()
        loop@ for (i in i0..i1) for (j in j0..j1) {
            if (areas.size >= MAX_TILES) break@loop
            areas += Key(i, j)
        }
        val got = HashMap<Key, List<DoubleArray>>()
        val missing = ArrayList<Key>()
        val dir = File(ctx.cacheDir, "lamps").apply { mkdirs() }
        for (k in areas) {
            val f = file(dir, k)
            if (f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE) got[k] = parse(f.readText())
            else missing += k
        }
        if (missing.isNotEmpty()) {
            if (allowFetch) got.putAll(fetchBlock(dir, missing))
            else for (k in missing) { val f = file(dir, k); if (f.exists()) got[k] = parse(f.readText()) }
        }
        val out = ArrayList<DoubleArray>()
        for (list in got.values) for (p in list) if (p[0] in south..north && p[1] in west..east) out += p
        return out
    }

    private fun file(dir: File, k: Key) = File(dir, "t_${k.i}_${k.j}.json")

    /** One query for the smallest block that covers the missing areas; each area's lamps are saved to its own file (empty ones too). */
    private fun fetchBlock(dir: File, missing: List<Key>): Map<Key, List<DoubleArray>> {
        val minI = missing.minOf { it.i }; val maxI = missing.maxOf { it.i }
        val minJ = missing.minOf { it.j }; val maxJ = missing.maxOf { it.j }
        val s = minI * STEP; val w = minJ * STEP; val n = (maxI + 1) * STEP; val e = (maxJ + 1) * STEP
        val q = String.format(Locale.US, "[out:json][timeout:25];node[\"highway\"=\"street_lamp\"](%.5f,%.5f,%.5f,%.5f);out;", s, w, n, e)
        val els = try { Overpass.query(q, 8000, 25000, 2) } catch (x: Exception) { return emptyMap() }
        val byArea = HashMap<Key, JSONArray>()
        for (k in missing) byArea[k] = JSONArray()
        for (k in 0 until els.length()) {
            val o = els.optJSONObject(k) ?: continue
            if (!o.has("lat") || !o.has("lon")) continue
            val lat = o.getDouble("lat"); val lon = o.getDouble("lon")
            val key = Key(Math.floor(lat / STEP).toInt(), Math.floor(lon / STEP).toInt())
            byArea[key]?.put(JSONArray().put(lat).put(lon))
        }
        val result = HashMap<Key, List<DoubleArray>>()
        for ((k, arr) in byArea) {
            try { file(dir, k).writeText(arr.toString()) } catch (x: Exception) { }
            result[k] = parse(arr.toString())
        }
        return result
    }

    private fun parse(s: String): List<DoubleArray> = try {
        val a = JSONArray(s)
        (0 until a.length()).map { val p = a.getJSONArray(it); doubleArrayOf(p.getDouble(0), p.getDouble(1), 0.0) }
    } catch (e: Exception) { emptyList() }
}
