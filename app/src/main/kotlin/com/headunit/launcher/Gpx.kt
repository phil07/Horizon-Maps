package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A GPX track you imported (Komoot, Strava, Garmin or your own recording): its name and its points, kept in the app's own folder. */
data class GpxTrack(val id: String, val name: String, val points: Int)

/** Reads GPX files and keeps the imported tracks on the phone (the points as text, the list of them in the preferences). */
object Gpx {
    private const val LIST_KEY = "gpx_tracks"
    /** Points closer than this to the one before are left out: a track of 40 000 points keeps its shape and stays small. */
    private const val MIN_STEP_M = 12.0
    private const val MAX_POINTS = 6000

    /** The name and the points of a GPX file (track points first, then route points); null if there are none. */
    fun parse(text: String): Pair<String, List<LL>>? {
        val parser = android.util.Xml.newPullParser()
        parser.setInput(java.io.StringReader(text))
        var name = ""
        var inName = false
        val pts = ArrayList<LL>()
        val routePts = ArrayList<LL>()
        var inTrk = false; var inRte = false
        var event = parser.eventType
        while (event != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            when (event) {
                org.xmlpull.v1.XmlPullParser.START_TAG -> when (parser.name) {
                    "trk" -> inTrk = true
                    "rte" -> inRte = true
                    "trkpt", "rtept" -> {
                        val lat = parser.getAttributeValue(null, "lat")?.toDoubleOrNull()
                        val lon = parser.getAttributeValue(null, "lon")?.toDoubleOrNull()
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            if (parser.name == "trkpt" || inTrk) pts.add(LL(lat, lon)) else routePts.add(LL(lat, lon))
                        }
                    }
                    "name" -> if (name.isEmpty()) inName = true
                }
                org.xmlpull.v1.XmlPullParser.TEXT -> if (inName) name += parser.text
                org.xmlpull.v1.XmlPullParser.END_TAG -> when (parser.name) {
                    "name" -> inName = false
                    "trk" -> inTrk = false
                    "rte" -> inRte = false
                }
            }
            event = parser.next()
        }
        val all = if (pts.size >= 2) pts else routePts
        if (all.size < 2) return null
        return name.trim().take(60).ifBlank { "GPX-Track" } to all
    }

    /** Thins the points out to about one every [MIN_STEP_M] metres, and to at most [MAX_POINTS]. */
    fun thin(pts: List<LL>): List<LL> {
        if (pts.isEmpty()) return pts
        val out = ArrayList<LL>()
        out.add(pts.first())
        for (p in pts.drop(1)) if (Geo.dist(out.last(), p) >= MIN_STEP_M) out.add(p)
        if (out.last() != pts.last()) out.add(pts.last())
        if (out.size <= MAX_POINTS) return out
        val step = out.size.toDouble() / MAX_POINTS
        return (0 until MAX_POINTS).map { out[(it * step).toInt()] } + out.last()
    }

    fun list(ctx: Context): List<GpxTrack> = try {
        val a = JSONArray(prefs(ctx).getString(LIST_KEY, "[]") ?: "[]")
        (0 until a.length()).map { i -> a.getJSONObject(i).let { GpxTrack(it.getString("id"), it.getString("name"), it.optInt("n")) } }
    } catch (e: Exception) { emptyList() }

    fun load(ctx: Context, id: String): List<LL> = try {
        File(dir(ctx), "$id.txt").readLines().mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size == 2) LL(parts[0].toDouble(), parts[1].toDouble()) else null
        }
    } catch (e: Exception) { emptyList() }

    /** Saves the track and returns its id. */
    fun save(ctx: Context, name: String, pts: List<LL>): String {
        val id = "t" + System.currentTimeMillis()
        val kept = thin(pts)
        File(dir(ctx), "$id.txt").writeText(kept.joinToString("\n") { String.format(java.util.Locale.US, "%.6f,%.6f", it.lat, it.lon) })
        val a = JSONArray()
        list(ctx).forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("n", it.points)) }
        a.put(JSONObject().put("id", id).put("name", name).put("n", kept.size))
        prefs(ctx).edit().putString(LIST_KEY, a.toString()).apply()
        return id
    }

    fun delete(ctx: Context, id: String) {
        File(dir(ctx), "$id.txt").delete()
        val a = JSONArray()
        list(ctx).filter { it.id != id }.forEach { a.put(JSONObject().put("id", it.id).put("name", it.name).put("n", it.points)) }
        prefs(ctx).edit().putString(LIST_KEY, a.toString()).apply()
    }

    /** The index of the track point nearest to [p]. */
    fun nearest(pts: List<LL>, p: LL): Int {
        var best = 0; var bestD = Double.MAX_VALUE
        for (i in pts.indices) { val d = Geo.dist(pts[i], p); if (d < bestD) { bestD = d; best = i } }
        return best
    }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("gpx", Context.MODE_PRIVATE)
    private fun dir(ctx: Context): File = File(ctx.filesDir, "gpx").also { it.mkdirs() }
}
