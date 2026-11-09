package com.headunit.launcher

import org.json.JSONObject
import java.util.Calendar

/**
 * "Geöffnet bis 20:00" / "Geschlossen · öffnet Mo 08:00" for a shop, from the opening times TomTom's search gives for the next
 * seven days. Null when the search gave none.
 */
object OpenHours {
    /** [ranges] are (opens, closes) in milliseconds, in any order. */
    fun note(ranges: List<Pair<Long, Long>>, now: Long): String? {
        if (ranges.isEmpty()) return null
        val rs = ranges.sortedBy { it.first }
        // back-to-back ranges (closing at midnight, opening again at midnight) are one stretch
        val merged = ArrayList<Pair<Long, Long>>()
        for (r in rs) {
            val l = merged.lastOrNull()
            if (l != null && r.first <= l.second + 60_000L) merged[merged.size - 1] = l.first to maxOf(l.second, r.second) else merged += r
        }
        val cur = merged.firstOrNull { now >= it.first && now < it.second }
        if (cur != null) {
            if (cur.second - now > 36 * 3600_000L) return Lang.t("Durchgehend geöffnet", "Open 24 hours")
            return Lang.t("Geöffnet bis ${hm(cur.second)}", "Open until ${hm(cur.second)}")
        }
        val next = merged.firstOrNull { it.first > now } ?: return Lang.t("Geschlossen", "Closed")
        val sameDay = day(next.first) == day(now)
        return Lang.t("Geschlossen · öffnet ", "Closed · opens ") + (if (sameDay) "" else weekday(next.first) + " ") + hm(next.first)
    }

    /** True when [note] says the place is open. */
    fun isOpen(note: String?): Boolean = note != null && !note.startsWith("Geschlossen") && !note.startsWith("Closed")

    private fun cal(ms: Long) = Calendar.getInstance().apply { timeInMillis = ms }
    private fun day(ms: Long) = cal(ms).let { it.get(Calendar.YEAR) * 400 + it.get(Calendar.DAY_OF_YEAR) }
    private fun hm(ms: Long) = cal(ms).let { String.format(java.util.Locale.US, "%02d:%02d", it.get(Calendar.HOUR_OF_DAY), it.get(Calendar.MINUTE)) }
    private fun weekday(ms: Long): String {
        val de = listOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"); val en = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        val i = cal(ms).get(Calendar.DAY_OF_WEEK) - 1
        return if (Lang.de) de[i] else en[i]
    }

    /** The note for a TomTom search result's "poi" object (asked for with openingHours=nextSevenDays). */
    fun fromTomTom(poi: JSONObject, now: Long = System.currentTimeMillis()): String? {
        val arr = poi.optJSONObject("openingHours")?.optJSONArray("timeRanges") ?: return null
        val out = ArrayList<Pair<Long, Long>>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val a = time(r.optJSONObject("startTime")) ?: continue
            val b = time(r.optJSONObject("endTime")) ?: continue
            if (b > a) out += a to b
        }
        return note(out, now)
    }

    private fun time(o: JSONObject?): Long? {
        o ?: return null
        val d = o.optString("date").split("-").mapNotNull { it.toIntOrNull() }
        if (d.size != 3) return null
        return Calendar.getInstance().apply { clear(); set(d[0], d[1] - 1, d[2], o.optInt("hour", 0), o.optInt("minute", 0), 0) }.timeInMillis
    }
}
