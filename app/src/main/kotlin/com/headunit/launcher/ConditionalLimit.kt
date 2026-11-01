package com.headunit.launcher

import java.util.Calendar

/**
 * OpenStreetMap's limits that only apply at certain times ("maxspeed:conditional"): "30 @ (22:00-06:00)",
 * "30 @ (Mo-Fr 07:00-17:00; SH off)", "80 @ wet", "60 @ (weight>7.5)". Several rules are separated by ";" outside brackets.
 *
 * [at] picks the rule that applies right now to a car: m/s, -1.0 for "none" (no limit), or null when none applies (then the
 * plain limit stands). Rules for lorries, trailers and the like never apply to a car, and a rule written in a way that isn't
 * understood is skipped rather than guessed.
 */
object ConditionalLimit {
    private const val MPH = 0.44704
    private val DAYS = listOf("su", "mo", "tu", "we", "th", "fr", "sa")   // Calendar.DAY_OF_WEEK - 1

    fun at(cond: String, country: String, now: Calendar, wet: Boolean = false): Double? {
        for (rule in splitTop(cond)) {
            val at = rule.indexOf('@'); if (at < 0) continue
            val value = rule.substring(0, at).trim().lowercase()
            var c = rule.substring(at + 1).trim()
            if (c.startsWith("(") && c.endsWith(")")) c = c.substring(1, c.length - 1).trim()
            if (c.isEmpty() || !applies(c.lowercase(), now, wet)) continue
            if (value == "none") return -1.0
            val m = Regex("^(\\d+)\\s*(mph|km/h|kmh|kph)?$").find(value) ?: continue
            val n = m.groupValues[1].toDouble()
            return if (m.groupValues[2] == "mph" || country == "GB" || country == "US") n * MPH else n / 3.6
        }
        return null
    }

    /** Splits at ";" that are not inside brackets. */
    internal fun splitTop(s: String): List<String> {
        val out = ArrayList<String>(); var depth = 0; val cur = StringBuilder()
        for (ch in s) {
            when (ch) {
                '(' -> depth++
                ')' -> depth = maxOf(0, depth - 1)
            }
            if (ch == ';' && depth == 0) { out += cur.toString(); cur.clear() } else cur.append(ch)
        }
        if (cur.isNotBlank()) out += cur.toString()
        return out.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /** Whether the condition (lower case, without the brackets) holds now for a car. */
    internal fun applies(c: String, now: Calendar, wet: Boolean): Boolean {
        // conditions joined with AND must all hold
        if (c.contains(" and ")) return c.split(" and ").all { applies(it.trim(), now, wet) }
        if (c == "wet") return wet
        // vehicles other than a car, or by weight, length, axle load, trailer…: not for a car
        val vehicle = listOf("weight", "hgv", "goods", "trailer", "length", "axleload", "width", "height", "bus", "caravan", "snowmobile", "maxweight", "agricultural")
        if (vehicle.any { c.contains(it) }) return false
        if (c in setOf("snow", "fog", "winter", "summer", "flashing", "signal")) return false
        return opening(c, now)
    }

    /**
     * A simple opening-hours expression: "22:00-06:00", "Mo-Fr 07:00-17:00", "Mo,We 7:00-9:00,15:00-17:00", "Sa,Su",
     * "Mo-Fr 07:00-17:00; SH off" (the school-holiday part is ignored: no holiday calendar here, so it applies on school days
     * and holidays alike, the safer way round). Several parts separated by ";" or "," between day groups: any one can match.
     */
    internal fun opening(c: String, now: Calendar): Boolean {
        val parts = c.split(';').map { it.trim() }.filter { it.isNotEmpty() && !it.endsWith(" off") && it != "off" }
        if (parts.isEmpty()) return false
        var understood = false
        for (p in parts) {
            val r = part(p, now) ?: continue
            understood = true
            if (r) return true
        }
        if (!understood) return false
        return false
    }

    /** One part ("Mo-Fr 07:00-17:00"): true/false, or null when it isn't understood. */
    private fun part(p0: String, now: Calendar): Boolean? {
        var p = p0.trim()
        // "SH" / "PH" alone: no holiday calendar, so not applied
        if (p == "sh" || p == "ph") return false
        p = p.replace(Regex("\\b(ph|sh)\\b,?"), "").trim().trimStart(',').trim()
        val tokens = p.split(Regex("\\s+")).filter { it.isNotEmpty() }
        var dayOk = true
        var timeOk = true
        var sawDay = false; var sawTime = false
        for (t in tokens) {
            when {
                Regex("^\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2}(,\\d{1,2}:\\d{2}-\\d{1,2}:\\d{2})*$").matches(t) -> {
                    sawTime = true
                    timeOk = t.split(',').any { inRange(it, now) }
                }
                Regex("^[a-z]{2}(-[a-z]{2})?(,[a-z]{2}(-[a-z]{2})?)*$").matches(t) && t.split(Regex("[-,]")).all { it in DAYS } -> {
                    sawDay = true
                    dayOk = t.split(',').any { inDays(it, now) }
                }
                else -> return null
            }
        }
        if (!sawDay && !sawTime) return null
        return dayOk && timeOk
    }

    private fun inDays(g: String, now: Calendar): Boolean {
        val today = now.get(Calendar.DAY_OF_WEEK) - 1
        val ends = g.split('-')
        val a = DAYS.indexOf(ends[0]); val b = DAYS.indexOf(ends.getOrElse(1) { ends[0] })
        if (a < 0 || b < 0) return false
        // Mo=1 … Sa=6, Su=0: walk from a to b round the week (so "Fr-Mo" works too)
        var d = a
        while (true) { if (d == today) return true; if (d == b) return false; d = (d + 1) % 7 }
    }

    private fun inRange(r: String, now: Calendar): Boolean {
        val (s, e) = r.split('-').map { hm -> hm.split(':').let { it[0].toInt() * 60 + it[1].toInt() } }
        val m = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return if (s <= e) m in s until e else m >= s || m < e   // over midnight: 22:00-06:00
    }
}
