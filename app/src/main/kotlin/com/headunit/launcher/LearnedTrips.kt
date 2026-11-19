package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/**
 * Your usual trips: how long a trip to a place really took, by the time of day it started (four-hour blocks). The last five are
 * kept per place and block, on the phone only. Once a trip to the place has been made twice at that time of day, its usual time
 * is used for the arrival time (less what has already gone).
 */
object LearnedTrips {
    private fun block(ms: Long): Int = Calendar.getInstance().apply { timeInMillis = ms }.get(Calendar.HOUR_OF_DAY) / 4

    private fun key(dest: String, startMs: Long): String = "$dest|${block(startMs)}"

    private fun load(prefs: Prefs): JSONObject =
        try { JSONObject(prefs.s(PreferenceKeys.LEARNED_TRIPS, "").ifBlank { "{}" }) } catch (e: Exception) { JSONObject() }

    /** A finished trip of [secs] seconds to [dest], started at [startMs]. Short hops are not learned from. */
    fun record(prefs: Prefs, dest: String, startMs: Long, secs: Double) {
        if (dest.isBlank() || secs < 120.0) return
        val o = load(prefs)
        val k = key(dest, startMs)
        val old = o.optJSONArray(k) ?: JSONArray()
        val kept = JSONArray()
        val from = maxOf(0, old.length() - 4)
        for (i in from until old.length()) kept.put(old.optInt(i))
        kept.put(secs.toInt())
        o.put(k, kept)
        prefs.put(PreferenceKeys.LEARNED_TRIPS, o.toString())
    }

    /** The usual total time of a trip to [dest] started at [startMs] (the median), once it has been made twice; else null. */
    fun usual(prefs: Prefs, dest: String, startMs: Long): Double? {
        val a = load(prefs).optJSONArray(key(dest, startMs)) ?: return null
        if (a.length() < 2) return null
        val v = (0 until a.length()).map { a.optInt(it).toDouble() }.sorted()
        return v[v.size / 2]
    }
}
