package com.headunit.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// My routes (see MyRoutes): the way driven on a trip is recorded, can be saved on the arrival card, and is offered first in the
// route choice next time ("Your route"). Driving it, a reroute brings you back onto it and no faster route is offered.

/** Notes where you are while a trip is guided (a point every 25 m or so), for "Save as my route". */
internal fun NavEngine.recordTrack(p: LL) {
    val last = drivenTrack.lastOrNull()
    if (last == null || Geo.dist(last, p) >= 25.0) drivenTrack += p
}

/** Whether there is a driven way to save on the arrival card. */
fun NavEngine.canSaveMyRoute(): Boolean = drivenTrack.size >= 10 && !myRouteSaved

/** A name such as "Zuhause → Arbeit": your own places by their name, otherwise the destination's. */
fun NavEngine.myRouteDefaultName(dest: Place?): String {
    fun label(p: LL?, fallback: String): String {
        if (p == null) return fallback
        places.slot("home")?.let { if (Geo.dist(it.pos, p) < 300.0) return Lang.t("Zuhause", "Home") }
        places.slot("work")?.let { if (Geo.dist(it.pos, p) < 300.0) return Lang.t("Arbeit", "Work") }
        return fallback
    }
    val a = label(drivenTrack.firstOrNull(), Lang.t("Start", "Start"))
    val b = label(dest?.pos ?: drivenTrack.lastOrNull(), dest?.name?.takeIf { it.isNotBlank() } ?: Lang.t("Ziel", "Destination"))
    return "$a → $b"
}

/** "Save as my route": keeps the roads you really took on this trip, under [name] (blank: a name like "Zuhause → Arbeit"). */
fun NavEngine.saveMyRoute(name: String?, dest: Place?): Boolean {
    val track = drivenTrack.toList()
    if (track.size < 2) return false
    val cal = java.util.Calendar.getInstance().apply { if (navStartMs > 0) timeInMillis = navStartMs }
    val dm = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    val nm = name?.trim()?.takeIf { it.isNotBlank() } ?: myRouteDefaultName(dest)
    MyRoutes.add(prefs, MyRoutes.MyRoute(System.currentTimeMillis().toString(36), nm, track.first(), dest?.pos ?: track.last(), dest?.name ?: "", MyRoutes.thin(track), dm))
    myRouteSaved = true
    toast(Lang.f("Als meine Route gespeichert: %1\$s", "Saved as my route: %1\$s", nm), 3000)
    return true
}

/**
 * A saved way for this trip, worked out from where you are now through points along it, or null when there is none (or it can't
 * be routed). [hd]: the way you're heading, as for the other routes.
 */
internal suspend fun NavEngine.myRouteOption(from: LL, to: LL, departMs: Long, hd: Double): Pair<Route, MyRoutes.MyRoute>? {
    val cal = java.util.Calendar.getInstance()
    val m = MyRoutes.match(MyRoutes.load(prefs), from, to, cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)) ?: return null
    val vias = MyRoutes.viaPoints(m.pts)
    if (vias.isEmpty()) return null
    val r = withContext(Dispatchers.IO) { try { netRoutes(from, to, false, departMs, vias, hd) } catch (e: Exception) { emptyList() } }.firstOrNull() ?: return null
    return r to m
}

/** Driving your saved way now (no faster route offered, a reroute brings you back onto it). */
fun NavEngine.onMyRoute(): Boolean = myRouteVias.isNotEmpty() && route != null && !previewing

/** The points to route through after leaving the way: your stops, then the rest of your saved way still ahead. */
internal fun NavEngine.rerouteVias(): List<LL> {
    val stops = via.map { it.pos }
    if (!onMyRoute()) return stops
    val r = route ?: return stops
    val here = curProgress
    val ahead = myRouteVias.filter { p -> RouteExtras.project(r, p, 300.0)?.first?.let { it > here + 200.0 } ?: false }
    return stops + ahead
}
