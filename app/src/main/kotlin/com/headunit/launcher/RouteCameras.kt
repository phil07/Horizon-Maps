package com.headunit.launcher

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Traffic and speed cameras on the route ahead. The route is asked about in stretches (OpenStreetMap, as the car comes near
 * the end of the last one), and one warning per camera comes when it is within 2 km.
 * The lists are placed by distance along the route; the map gets the positions through [showOnMap].
 */
internal class RouteCameras(
    private val scope: CoroutineScope,
    private val online: () -> Boolean,
    private val currentRoute: () -> Route?,
    private val showOnMap: (List<LL>) -> Unit,
    private val notice: (NavEngine.Notice, Long) -> Unit,
    private val imperial: () -> Boolean,
) {
    data class Spot(val offset: Double, val type: String, val ref: String, val pos: LL)

    private var route: Route? = null
    private var spots: List<Spot> = emptyList()
    private var upTo = 0.0
    private var busy = false
    private val said = HashSet<Long>()

    /** Called with the route being driven and how far along it the car is. */
    fun check(r: Route, prog: Double) {
        if (r !== route) { route = r; spots = emptyList(); upTo = 0.0; said.clear(); showOnMap(emptyList()) }
        if (!busy && online() && prog + 50_000.0 > upTo && upTo < r.len) {
            val from = max(prog, upTo - 500.0)
            fetch(r, from, min(r.len, from + 100_000.0))
        }
        val next = spots.firstOrNull { it.offset > prog - 20.0 } ?: return
        val dist = next.offset - prog
        val key = Math.round(next.offset / 100.0)
        if (dist > 2000.0 || key in said) return
        said += key
        val d = Geo.fmtDist(dist, imperial())
        val type = if (next.type == "speed") Lang.t("Geschwindigkeitskamera", "Speed camera") else Lang.t("Verkehrskamera", "Traffic camera")
        notice(NavEngine.Notice("camera", Lang.t("$type in $d", "$type in $d"), next.ref.takeIf { it.isNotBlank() } ?: "", 2), 8_000L)
    }

    private fun fetch(r: Route, from: Double, to: Double) {
        busy = true
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { Overpass.cameras(RouteExtras.line(r, from, to, 30.0, 2500)) }
                if (found != null && currentRoute() === r) {
                    val add = found.mapNotNull { cam ->
                        val pr = RouteExtras.project(r, cam.pos, 50.0, from) ?: return@mapNotNull null
                        Spot(pr.first, cam.type, cam.ref, cam.pos)
                    }
                    spots = (spots + add).distinctBy { Math.round(it.offset / 100.0) }.sortedBy { it.offset }
                    upTo = to
                    showOnMap(spots.map { it.pos })
                }
            } finally { busy = false }
        }
    }
}
