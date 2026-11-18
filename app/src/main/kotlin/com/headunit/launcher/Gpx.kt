package com.headunit.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A GPX track you imported (Komoot, Strava, Garmin or your own recording): its name and its points, kept in the app's own folder. */
/** A GPX track: its name, its number of points, its folder (blank for none) and its first point (where a route to it starts). */
data class GpxTrack(val id: String, val name: String, val points: Int, val group: String = "", val start: LL? = null)

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
        (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            GpxTrack(o.getString("id"), o.getString("name"), o.optInt("n"), o.optString("g"), parseStart(o.optString("s")))
        }
    } catch (e: Exception) { emptyList() }

    fun load(ctx: Context, id: String): List<LL> = try {
        File(dir(ctx), "$id.txt").readLines().mapNotNull { line ->
            val parts = line.split(',')
            if (parts.size == 2) LL(parts[0].toDouble(), parts[1].toDouble()) else null
        }
    } catch (e: Exception) { emptyList() }

    /** Saves the track (in [group], or none) and returns its id. */
    fun save(ctx: Context, name: String, pts: List<LL>, group: String = ""): String {
        val id = "t" + System.currentTimeMillis()
        val kept = thin(pts)
        File(dir(ctx), "$id.txt").writeText(kept.joinToString("\n") { String.format(java.util.Locale.US, "%.6f,%.6f", it.lat, it.lon) })
        write(ctx, list(ctx) + GpxTrack(id, name, kept.size, group.trim().take(24), kept.firstOrNull()))
        return id
    }

    /** The tracks for a backup: each with its name, its folder and its stored points. */
    fun backupArray(ctx: Context): JSONArray {
        val a = JSONArray()
        for (t in list(ctx)) {
            val f = File(dir(ctx), "${t.id}.txt")
            if (f.exists()) a.put(JSONObject().put("name", t.name).put("g", t.group).put("text", f.readText()))
        }
        return a
    }

    /** Puts backed-up tracks back, each as a new track in its folder (one already there with the same name and folder is skipped). */
    fun restoreArray(ctx: Context, a: JSONArray): Int {
        val cur = list(ctx).toMutableList()
        var n = 0
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val name = o.optString("name").take(60).ifBlank { "Track" }
            val group = o.optString("g").trim().take(24)
            if (cur.any { it.name == name && it.group == group }) continue
            val text = o.optString("text")
            val pts = text.lines().mapNotNull { line ->
                val p = line.split(',')
                if (p.size == 2) LL(p[0].toDoubleOrNull() ?: return@mapNotNull null, p[1].toDoubleOrNull() ?: return@mapNotNull null) else null
            }
            if (pts.size < 2) continue
            val id = "t" + System.currentTimeMillis() + "_$i"
            File(dir(ctx), "$id.txt").writeText(pts.joinToString("\n") { String.format(java.util.Locale.US, "%.6f,%.6f", it.lat, it.lon) })
            cur += GpxTrack(id, name, pts.size, group, pts.firstOrNull())
            n++
        }
        write(ctx, cur)
        return n
    }

    /** Puts the track in the folder [group] (blank takes it out of its folder). */
    fun setGroup(ctx: Context, id: String, group: String) =
        write(ctx, list(ctx).map { if (it.id == id) it.copy(group = group.trim().take(24)) else it })

    fun delete(ctx: Context, id: String) {
        File(dir(ctx), "$id.txt").delete()
        write(ctx, list(ctx).filter { it.id != id })
    }

    private fun write(ctx: Context, tracks: List<GpxTrack>) {
        val a = JSONArray()
        tracks.forEach { t ->
            a.put(JSONObject().put("id", t.id).put("name", t.name).put("n", t.points).put("g", t.group)
                .put("s", t.start?.let { String.format(java.util.Locale.US, "%.6f,%.6f", it.lat, it.lon) } ?: ""))
        }
        prefs(ctx).edit().putString(LIST_KEY, a.toString()).apply()
    }

    private fun parseStart(s: String): LL? {
        val p = s.split(',')
        if (p.size != 2) return null
        val la = p[0].toDoubleOrNull() ?: return null
        val lo = p[1].toDoubleOrNull() ?: return null
        return LL(la, lo)
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
