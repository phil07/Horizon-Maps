package com.headunit.launcher

import com.headunit.launcher.NavEngine.Notice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Part of NavEngine (moved out of NavEngine.kt): the vignette countries of a route, avoiding them, and the warning before the border.
// The state (the route's vignettes, what was said) stays in NavEngine.

/**
 * Settings > Routes Lang.t("Vignettenpflichtige Straßen meiden", "Avoid vignette roads"): when the best route uses a vignette road in a country you only pass
 * through, it's asked again without toll roads; the new one is kept if it needs fewer vignettes.
 */
internal suspend fun NavEngine.avoidVignette(rs: List<Route>, from: LL, to: LL, departMs: Long): List<Route> {
    if (!prefs.b(PreferenceKeys.AVOID_VIGNETTE, false) || prefs.b(PreferenceKeys.AVOID_TOLL, false)) return rs
    val ends = setOfNotNull(Vignette.countryOf(to), Vignette.countryOf(from))
    val need = withContext(Dispatchers.Default) { try { Vignette.along(rs[0]) } catch (e: Exception) { emptyList() } }.filter { it.cc !in ends }
    if (need.isEmpty()) return rs
    val alt = withContext(Dispatchers.IO) {
        val keep = TomTomRest.avoid
        try { TomTomRest.avoid = (keep + "tollRoads").distinct(); netRoutes(from, to, true, departMs, via.map { it.pos }, driveHeading(from), askBoth = true) }
        catch (e: Exception) { emptyList() }
        finally { TomTomRest.avoid = keep }
    }
    if (alt.isEmpty()) return rs
    val need2 = withContext(Dispatchers.Default) { try { Vignette.along(alt[0]) } catch (e: Exception) { need } }.filter { it.cc !in ends }
    if (need2.size >= need.size) return rs
    val extra = Math.round((alt[0].time - rs[0].time) / 60.0).toInt()
    toast(Lang.t("Route ohne Vignette" + (if (extra > 0) " (+$extra Min.)" else ""), "Route without a vignette" + (if (extra > 0) " (+$extra min)" else "")), 4000)
    return alt
}

/** Before the border: once per country and route, about 30 km ahead (or straight away when it's nearer). */
internal fun NavEngine.checkVignette(r: Route, progress: Double) {
    if (vignetteRoute !== r || previewing || simMode != 0 || !prefs.b(PreferenceKeys.VIGNETTE_WARN, true)) return
    for (n in routeVignette) {
        val ahead = n.off - progress
        if (ahead < 0 || ahead > 30_000.0 || !vignetteSaid.add(n.cc)) continue
        speak(Vignette.spoken(n), true)
        showNotice(Notice("toll", Lang.t("Vignettenpflicht in ${Vignette.name(n.cc)}", "Vignette needed in ${Vignette.name(n.cc)}"),
            Lang.t("Ab etwa ${Geo.fmtDist(ahead, imperial)}", "From about ${Geo.fmtDist(ahead, imperial)}") + (if (n.special.isNotEmpty()) Lang.t(" · Streckenmaut ", " · section toll ") + n.special.joinToString(", ") else ""), 2), 20_000L)
        break
    }
}
