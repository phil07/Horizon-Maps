package com.headunit.launcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where the crossings round you were last looked up (when no route is driven), and whether a lookup is running. */
internal object CrossingNotes {
    var at: LL? = null
    @Volatile var busy = false
}

/**
 * Zebra and other marked crossings round you, painted on the road even with no route: looked up as you drive under about 40 mph,
 * about every 500 m. With a route, its own lookup (hazards on the route) draws them. The crossing's direction is taken from
 * the way you are heading, so it is only a guide. Never throws.
 */
internal fun NavEngine.crossingsAroundTick(p: Fix) {
    if (route != null || previewing || !prefs.b(PreferenceKeys.ZEBRAS, true) || p.speed > 17.9 || !online() || shadow()) return
    val last = CrossingNotes.at
    if (CrossingNotes.busy || (last != null && Geo.dist(last, p.pos) < 500.0)) return
    CrossingNotes.busy = true
    CrossingNotes.at = p.pos
    val heading = p.heading
    scope.launch {
        try {
            val pois = withContext(Dispatchers.IO) { try { RouteExtras.crossingsAround(p.pos) } catch (e: Exception) { null } }
            if (pois != null && route == null) map.setCrossings(pois.map { Triple(it.pos, heading, it.name == "zebra") })
            else if (pois == null) CrossingNotes.at = null   // not answered: asked again at the next fix
        } finally { CrossingNotes.busy = false }
    }
}
