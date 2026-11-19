package com.headunit.launcher

import java.util.Calendar

/**
 * An offline route (from the downloaded map) has no live traffic, so its time is allowed for the hold-ups that are normal at that
 * time of day: more in the weekday rush hours and on the town part of a route than on motorways. Fixed rough shares, worked out on
 * the phone; nothing is sent anywhere. The added time shows as traffic delay ("+N Min. Verkehr"), as a live delay does.
 */
object TypicalTraffic {
    /** The share of the free-flow time added at [nowMs]: weekday rush hours 15 %, weekday daytime 6 %, weekday evenings 5 %, else 2 %. */
    fun share(nowMs: Long): Double {
        val c = Calendar.getInstance().apply { timeInMillis = nowMs }
        val dow = c.get(Calendar.DAY_OF_WEEK)
        val h = c.get(Calendar.HOUR_OF_DAY)
        val weekday = dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
        return when {
            weekday && (h in 7..9 || h in 16..18) -> 0.15
            weekday && h in 10..15 -> 0.06
            weekday && h in 19..21 -> 0.05
            else -> 0.02
        }
    }

    /** The route with its normal hold-ups added: longer by that share, more where the route is slow (towns) than on a motorway. */
    fun add(r: Route, nowMs: Long = System.currentTimeMillis()): Route {
        // the average speed tells the town part from the motorway part: a route at under 50 km/h on average gets half as much again
        val avgKmh = if (r.time > 0) (r.len / 1000.0) / (r.time / 3600.0) else 0.0
        val town = if (avgKmh in 0.1..50.0) 1.5 else 1.0
        val extra = r.time * share(nowMs) * town
        val nr = Route(r.pts, r.cum, r.steps, r.len, r.time + extra, r.delay + extra, r.lanes, r.sev)
        nr.signals = r.signals; nr.tunnels = r.tunnels; nr.limits = r.limits; nr.incidents = r.incidents
        return nr
    }
}
