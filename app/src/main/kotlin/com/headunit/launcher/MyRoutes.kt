package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject

/**
 * My routes: the ways you drove and saved ("Save as my route" on the arrival card). Next time you go from near the same start to
 * the same place, the saved way is offered first in the route choice as "Your route". Kept in the settings (so they're in
 * backups too) as one JSON list under [PreferenceKeys.MY_ROUTES].
 */
object MyRoutes {
    /** [mode]: "offer" (in the choice, picked), "auto" (started straight away) or "off" (not offered). [depMin]: minute of the
     *  day it was driven, so a morning and an evening way each come up at their own time. */
    data class MyRoute(val id: String, val name: String, val from: LL, val to: LL, val toName: String, val pts: List<LL>, val depMin: Int, val mode: String = "offer")

    /** How near the start and the destination have to be to count as the same trip (metres). */
    private const val START_M = 1000.0
    private const val DEST_M = 300.0

    fun load(prefs: Prefs): List<MyRoute> = parse(prefs.s(PreferenceKeys.MY_ROUTES, "[]"))

    fun parse(json: String): List<MyRoute> = try {
        val a = JSONArray(json)
        (0 until a.length()).mapNotNull { i ->
            try {
                val o = a.getJSONObject(i)
                val p = o.getJSONArray("p")
                val pts = (0 until p.length() / 2).map { k -> LL(p.getDouble(2 * k), p.getDouble(2 * k + 1)) }
                if (pts.size < 2) null else MyRoute(o.getString("id"), o.optString("name"), LL(o.getDouble("fa"), o.getDouble("fo")),
                    LL(o.getDouble("ta"), o.getDouble("to")), o.optString("tn"), pts, o.optInt("dm", -1), o.optString("mode", "offer"))
            } catch (e: Exception) { null }
        }
    } catch (e: Exception) { emptyList() }

    fun toJson(list: List<MyRoute>): String {
        val a = JSONArray()
        for (r in list) {
            val p = JSONArray()
            for (q in r.pts) { p.put(Math.round(q.lat * 1e5) / 1e5); p.put(Math.round(q.lon * 1e5) / 1e5) }
            a.put(JSONObject().put("id", r.id).put("name", r.name).put("fa", r.from.lat).put("fo", r.from.lon).put("ta", r.to.lat).put("to", r.to.lon)
                .put("tn", r.toName).put("p", p).put("dm", r.depMin).put("mode", r.mode))
        }
        return a.toString()
    }

    fun save(prefs: Prefs, list: List<MyRoute>) = prefs.put(PreferenceKeys.MY_ROUTES, toJson(list))

    fun add(prefs: Prefs, r: MyRoute) = save(prefs, load(prefs) + r)
    fun remove(prefs: Prefs, id: String) = save(prefs, load(prefs).filter { it.id != id })
    fun update(prefs: Prefs, id: String, f: (MyRoute) -> MyRoute) = save(prefs, load(prefs).map { if (it.id == id) f(it) else it })

    /** The track thinned out to a point about every [stepM] metres (at most [max] points), for storing. */
    fun thin(track: List<LL>, stepM: Double = 60.0, max: Int = 600): List<LL> {
        if (track.size < 2) return track
        val out = ArrayList<LL>()
        out += track.first()
        for (p in track) if (Geo.dist(out.last(), p) >= stepM) out += p
        if (out.last() != track.last()) out += track.last()
        if (out.size <= max) return out
        val k = out.size.toDouble() / max
        return (0 until max).map { out[(it * k).toInt().coerceAtMost(out.size - 1)] } + out.last()
    }

    /**
     * The saved way for a trip from [from] to [to] at [minuteOfDay], or null: one that starts near [from] and ends near [to], not
     * switched off. With more than one, the one driven nearest this time of day comes first.
     */
    fun match(list: List<MyRoute>, from: LL, to: LL, minuteOfDay: Int): MyRoute? =
        list.filter { it.mode != "off" && Geo.dist(it.from, from) <= START_M && Geo.dist(it.to, to) <= DEST_M }
            .minByOrNull { if (it.depMin < 0) 24 * 60 else minuteGap(it.depMin, minuteOfDay) }

    /** Minutes between two times of day, round the clock (23:50 and 00:10 are 20 minutes apart). */
    fun minuteGap(a: Int, b: Int): Int { val d = Math.abs(a - b) % (24 * 60); return minOf(d, 24 * 60 - d) }

    /**
     * Points along the saved way for the router to pass through, so it follows the roads you took: [n] points spread evenly by
     * distance, leaving out the first and last bit (near the start and the destination the router finds its own way in).
     */
    fun viaPoints(pts: List<LL>, n: Int = 6): List<LL> {
        if (pts.size < 3) return emptyList()
        val at = DoubleArray(pts.size)
        for (i in 1 until pts.size) at[i] = at[i - 1] + Geo.dist(pts[i - 1], pts[i])
        val len = at.last()
        if (len < 1500.0) return emptyList()
        val out = ArrayList<LL>()
        for (k in 1..n) {
            val x = len * k / (n + 1)
            val i = at.indexOfFirst { it >= x }.coerceAtLeast(1)
            out += pts[i]
        }
        return out.distinct()
    }
}
