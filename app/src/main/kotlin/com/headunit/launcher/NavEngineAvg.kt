package com.headunit.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

// Part of NavEngine: looking up average speed checks (on a route and without one) and measuring one without a route.

/**
 * On a route: the checks for the next 40 km, in a small request of their own. The ones kept on the device for this route come
 * first, so a road driven before has its check straight away (also without signal); a failed request is tried again after 20 s.
 */
internal fun NavEngine.avgLookupTick(r: Route, progress: Double, now: Long) {
    if (!prefs.b(PreferenceKeys.AVGZONES, true)) return
    if (freeAvg.cur != null) { freeAvg.reset(); avgInfo = null }
    if (avgStoredFor !== r) {
        avgStoredFor = r
        val box = AvgZones.boxOf(RouteExtras.line(r, 0.0, r.len, 1500.0, 400))
        if (box != null) scope.launch {
            val kept = try { withContext(Dispatchers.IO) { AvgZones.stored(ctx, box) } } catch (e: Exception) { emptyList() }
            if (kept.isNotEmpty() && route === r) rawAvg = (kept + rawAvg).distinctBy { it.id }.takeLast(200)
        }
    }
    // a camera of an average speed check turned up where no check is known: look again now rather than at the next turn
    val tagged = Overpass.avgCams.size
    if (tagged != avgTaggedSeen) {
        avgTaggedSeen = tagged
        val known = rawAvg
        if (Overpass.avgCams.values.any { c -> known.none { it.near(c, 150.0) } }) { avgRetryAt = 0L; avgWinEnd = -1.0; avgLastAt = 0L }
    }
    if (avgBusy || now < avgRetryAt || !online()) return
    if (!(progress + 8000.0 > avgWinEnd || (avgWinRoute !== r && now - avgLastAt > 45_000L))) return
    val to = progress + 40_000.0
    val box = AvgZones.boxOf(RouteExtras.line(r, progress - 500.0, to, 400.0, 120)) ?: return
    avgBusy = true; avgLastAt = now
    scope.launch {
        try {
            val fresh = withContext(Dispatchers.IO) { AvgZones.fetch(box).also { AvgZones.save(ctx, box, it) } }
            if (route === r) { rawAvg = AvgZones.merge(rawAvg, box, fresh).takeLast(200); avgWinRoute = r; avgWinEnd = to }
        } catch (e: Exception) { avgRetryAt = System.currentTimeMillis() + 20_000L }
        finally { avgBusy = false }
    }
}

/**
 * Without a destination: the checks around you (kept on the device, and asked for again every few kilometres), and the one you
 * are driving through, measured by the distance driven from its start.
 */
internal fun NavEngine.avgFreeTick(p: Fix) {
    if (!prefs.b(PreferenceKeys.AVGZONES, true)) { if (freeAvg.cur != null) { freeAvg.reset(); avgInfo = null }; return }
    val now = System.currentTimeMillis()
    val at = freeAvgAt
    if ((at == null || Geo.dist(at, p.pos) > 4000.0) && !freeAvgBusy && now > freeAvgRetryAt) {
        freeAvgBusy = true
        val c = p.pos
        // about 10 km each way
        val box = doubleArrayOf(c.lat - 0.09, c.lon - 0.14, c.lat + 0.09, c.lon + 0.14)
        scope.launch {
            try {
                val kept = withContext(Dispatchers.IO) { AvgZones.stored(ctx, box) }
                freeAvgDefs = AvgZones.merge(freeAvgDefs, box, kept).takeLast(200)
                if (online()) {
                    val fresh = withContext(Dispatchers.IO) { AvgZones.fetch(box).also { AvgZones.save(ctx, box, it) } }
                    freeAvgDefs = AvgZones.merge(freeAvgDefs, box, fresh).takeLast(200)
                }
                freeAvgAt = c
            } catch (e: Exception) { freeAvgRetryAt = System.currentTimeMillis() + 20_000L }
            finally { freeAvgBusy = false }
        }
    }
    val t = freeAvg.update(p.pos, p.heading, p.speed, tripDist, now, freeAvgDefs)
    val imp = imperial
    t.entered?.let { d ->
        avgOverWarned = false
        val len = freeAvg.cur?.lengthM ?: 0.0
        chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
        raiseAlert("avg", Lang.t("Abschnittskontrolle, etwa ${Geo.fmtDist(len, imp)}", "Average speed check, about ${Geo.fmtDist(len, imp)}"), 6000)
        speak(Lang.t("Abschnittskontrolle", "Average speed check"), true)
    }
    t.ahead?.let { d ->
        chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
        raiseAlert("avg", Lang.t("Abschnittskontrolle voraus", "Average speed check ahead"), 6000)
        speak(Lang.t("Abschnittskontrolle voraus", "Average speed check ahead"), true)
    }
    if (t.left) { avgInfo = null; toast(ctx.Lang.t("Ende der Durchschnittsgeschwindigkeitskontrolle", "End of average speed check"), 2500) }
    val cur = freeAvg.cur
    if (cur == null) { if (avgInfo != null) avgInfo = null; return }
    val secs = (now - cur.enterAt) / 1000.0
    val gone = tripDist - cur.enterDriven
    val avg = if (secs > 4.0) gone / secs else p.speed
    // never more than the road's own signed limit (a mapped check limit can be a different unit's number)
    val lim = cur.def.limit?.let { l -> speedLimit?.let { min(it, l) } ?: l } ?: speedLimit
    val over = lim != null && secs > 15.0 && avg > lim + 0.45
    if (over && !avgOverWarned) {
        avgOverWarned = true
        chime(android.media.ToneGenerator.TONE_PROP_BEEP2)
        speak(Lang.t("Ihre Durchschnittsgeschwindigkeit liegt über dem Limit", "Your average speed is over the limit"), true)
    } else if (!over && lim != null && avg < lim - 0.5) avgOverWarned = false
    // what is left: to the end as the crow flies, or the length less what was driven, whichever is more
    val left = max(cur.def.ends.minOf { Geo.dist(it, p.pos) }, cur.lengthM - gone).coerceAtLeast(0.0)
    val safe = if (lim != null && secs > 4.0) {
        val allowed = max(cur.lengthM, gone + left) / lim - secs
        if (allowed > 1.0) left / allowed else 0.0
    } else null
    avgInfo = AvgInfo(avg, lim, left, over, false, 0.0, safe)
}
