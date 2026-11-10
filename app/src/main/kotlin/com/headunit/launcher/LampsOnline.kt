package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import java.io.File
import java.util.Locale

/**
 * Street lamps as OpenStreetMap has them (node highway=street_lamp), fetched online once per area (a 0.02° square, about 2 km) and kept
 * for a month in the app's cache. Only used with a connection; the lamps of an offline pack still count where there is one.
 */
object LampsOnline {
    private const val STEP = 0.02
    private const val MAX_AGE = 30L * 86_400_000L
    private const val MAX_TILES = 4

    fun connected(ctx: Context): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true } == true
    } catch (e: Exception) { false }

    /** The lamps (lat, lon) inside the box, from the cache or fetched; [allowFetch] false means only what's already cached. */
    fun inBox(ctx: Context, south: Double, west: Double, north: Double, east: Double, allowFetch: Boolean): List<DoubleArray> {
        val out = ArrayList<DoubleArray>()
        val i0 = Math.floor(south / STEP).toInt(); val i1 = Math.floor(north / STEP).toInt()
        val j0 = Math.floor(west / STEP).toInt(); val j1 = Math.floor(east / STEP).toInt()
        var tiles = 0
        for (i in i0..i1) for (j in j0..j1) {
            if (tiles++ >= MAX_TILES) break
            for (p in tile(ctx, i, j, allowFetch) ?: continue) if (p[0] in south..north && p[1] in west..east) out += p
        }
        return out
    }

    private fun tile(ctx: Context, i: Int, j: Int, allowFetch: Boolean): List<DoubleArray>? {
        val dir = File(ctx.cacheDir, "lamps").apply { mkdirs() }
        val f = File(dir, "t_${i}_$j.json")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < MAX_AGE) return parse(f.readText())
        if (!allowFetch) return if (f.exists()) parse(f.readText()) else null
        val s = i * STEP; val w = j * STEP; val n = s + STEP; val e = w + STEP
        val q = String.format(Locale.US, "[out:json][timeout:25];node[\"highway\"=\"street_lamp\"](%.5f,%.5f,%.5f,%.5f);out;", s, w, n, e)
        val els = try { Overpass.query(q, 8000, 25000, 2) } catch (x: Exception) { return null }
        val arr = JSONArray()
        for (k in 0 until els.length()) {
            val o = els.optJSONObject(k) ?: continue
            if (o.has("lat") && o.has("lon")) arr.put(JSONArray().put(o.getDouble("lat")).put(o.getDouble("lon")))
        }
        try { f.writeText(arr.toString()) } catch (x: Exception) { }
        return parse(arr.toString())
    }

    private fun parse(s: String): List<DoubleArray> = try {
        val a = JSONArray(s)
        (0 until a.length()).map { val p = a.getJSONArray(it); doubleArrayOf(p.getDouble(0), p.getDouble(1), 0.0) }
    } catch (e: Exception) { emptyList() }
}
