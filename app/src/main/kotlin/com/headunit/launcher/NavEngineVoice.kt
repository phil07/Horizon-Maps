package com.headunit.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.res.stringResource
import com.headunit.launcher.NavEngine.Placed

// Part of NavEngine (moved out of NavEngine.kt to keep that file workable): these are extension functions on the engine,
// so they are called exactly as before.

// ---- voice commands ----
/**
 * Understands simple spoken commands. Returns null when it isn't a command (the words are then searched for),
 * "share" when the ETA should be shared, "search:<place>" to search for a place, or "" when it was handled.
 */
/** Understands spoken commands in English and German ("take me home", "bring mich nach Hause", "nächste Tankstelle"…). */
/**
 * The default voice of many head units sounds robotic. Pick the best installed voice for the spoken language
 * (the highest quality; Google's own voices are the highest). Falls back to the system voice if none is found.
 */
internal fun NavEngine.pickBestVoice() {
    val t = tts ?: return
    try {
        // the voice chosen in Settings, when it is still installed
        val chosen = prefs.s(PreferenceKeys.TTS_VOICE, "").takeIf { it.isNotBlank() }
        // (only while it speaks the app's language: after switching the language the matching voice is picked instead)
        if (chosen != null) { t.voices?.firstOrNull { it.name == chosen && it.locale.language == Locale.getDefault().language }?.let { t.voice = it; return } }
        val lang = t.language?.language ?: return
        val best = t.voices
            ?.filter { it.locale.language == lang && it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true }
            ?.maxByOrNull { it.quality }
        if (best != null) t.voice = best
    } catch (e: Exception) { }
}

/** The installed voices in the app's language, best first, for the Settings list. */
fun NavEngine.installedVoices(): List<android.speech.tts.Voice> {
    val t = tts ?: return emptyList()
    return try {
        val lang = t.language?.language ?: return emptyList()
        (t.voices ?: emptySet()).filter { it.locale.language == lang && it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true }
            .sortedByDescending { it.quality }
    } catch (e: Exception) { emptyList() }
}

/** Uses the voice [name] from now on (kept across restarts). */
fun NavEngine.useVoice(name: String) {
    prefs.put(PreferenceKeys.TTS_VOICE, name)
    try { tts?.voices?.firstOrNull { it.name == name }?.let { tts?.voice = it } } catch (e: Exception) { }
}

fun NavEngine.handleVoice(raw: String): String? {
    val t = raw.lowercase(Locale.GERMAN).trim().removePrefix("hey horizon").removePrefix("hallo horizon").trim(' ', ',', '.', '!', '?')
    fun has(vararg w: String) = w.any { t.contains(it) }
    val f = SimpleDateFormat("HH:mm", Locale.UK)
    fun say(s: String) { toast(s, 3500); speak(s, true) }
    // German verbs that start a trip: "fahr / navigiere / bring mich / route / führe mich / ich will …"
    val deGo = "(?:bitte )?(?:fahr(?:e)?(?: mich)?|navigier(?:e)?(?: mich)?|bring(?:e)?(?: mich)?|führ(?:e)?(?: mich)?|leite mich|route|ich will|ich möchte|wir fahren|zeig mir den weg)"
    // places you have saved
    if (Regex("^(go|take me|navigate|drive|directions|head)( back)?( to)? ?home$").matches(t) || t == "home" ||
        Regex("^(?:$deGo )?(?:nach hause|heim|zu hause|nachhause)(?: fahren| bitte)?$").matches(t) || t == "zuhause") {
        val h = places.slot("home"); if (h != null) { say(Lang.t("Fahre nach Hause", "Going home")); startRoute(h) } else say(Lang.t("Zuhause ist noch nicht festgelegt", "Home isn't set yet")); return ""
    }
    if (Regex("^(go|take me|navigate|drive|directions|head)( to)? ?work$").matches(t) || t == "work" ||
        Regex("^(?:$deGo )?(?:zur arbeit|in die arbeit|ins büro|zum büro|zur firma|in die firma)(?: fahren| bitte)?$").matches(t) || t == "arbeit") {
        val w = places.slot("work"); if (w != null) { say(Lang.t("Fahre zur Arbeit", "Going to work")); startRoute(w) } else say(Lang.t("Arbeit ist noch nicht festgelegt", "Work isn't set yet")); return ""
    }
    // reports
    if (t.startsWith("report") || t.startsWith("melde") || t.startsWith("meldung")) {
        val k = when {
            has("police", "cop", "polizei", "kontrolle") -> "police"; has("accident", "crash", "unfall") -> "accident"
            has("breakdown", "broken", "panne", "liegengeblieben") -> "breakdown"; has("camera", "blitzer", "radar") -> "mobilecam"
            has("traffic jam", "jam", "stau") -> "jam"
            else -> "hazard"
        }
        report(k); return ""
    }
    // finding things
    val kind = when {
        has("charg", "ladesäule", "ladestation", "ladepunkt", "aufladen", "e-tankstelle", "strom tanken") -> "charging"
        has("fuel", "petrol", "diesel", "filling station", "gas station", "tankstelle", "tanken", "sprit", "benzin") -> "fuel"
        has("toilet", "loo", "bathroom", "klo", "wc", "toilette") -> "toilets"
        has("coffee", "cafe", "café", "kaffee") -> "coffee"
        has("food", "eat", "restaurant", "hungry", "essen", "hunger", "imbiss") -> "food"
        has("services", "rest stop", "rest area", "raststätte", "rastplatz", "raststelle", "autohof") -> "services"
        has("parking", "car park", "somewhere to park", "parkplatz", "parkhaus", "parken") -> "parking"
        else -> null
    }
    // "fahr nach Ladenburg" is a place, "fahr zur nächsten Tankstelle" is a search nearby
    val deDest = Regex("^$deGo\\s+(?:nach|zu|zum|zur|in die|ins|in den|an den|an die|zu der|zu dem)\\s+(.+?)(?:\\s+fahren|\\s+bitte)?$").find(t)?.groupValues?.get(1)
    val nearbyWords = Regex("(^|\\s)(nächste|günstig|billig|eine[mnr]?\\s|in der nähe)").containsMatchIn(deDest ?: "")
    if (deDest != null && !nearbyWords) return "search:$deDest"
    if (kind != null && has("find", "nearest", "look for", "search for", "where", "any ", "need", "show", "cheap", "stop for", "add a stop", "add stop", "near",
            "such", "finde", "nächste", "wo ist", "wo gibt", "brauche", "zeig", "günstig", "billig", "stopp", "halt", "zwischenstopp", "in der nähe", "muss", "will", "möchte")) {
        nearbySort = if (has("cheap", "günstig", "billig")) "cheap" else "near"
        if (has("stop", "zwischenstopp", "halt") && route != null) stopMode = true
        loadNearby(kind, has("destination", "when i get there", "am ziel", "beim ziel", "dort"))
        say(if (Lang.de) "Suche " + Overpass.kindLabel(kind) + if (route != null) " entlang der Route" else " in der Nähe"
            else "Looking for " + Overpass.kindLabel(kind).lowercase() + if (route != null) " along your route" else " nearby")
        return ""
    }
    // route preferences
    if (has("avoid toll", "maut")) { prefs.put(PreferenceKeys.AVOID_TOLL, true); say(Lang.t("Mautstraßen werden gemieden", "Avoiding toll roads")); dest?.let { startRoute(it, departMin, true, startNow = route != null && !previewing) }; return "" }
    if (has("avoid motorway", "keine autobahn", "ohne autobahn", "autobahn vermeiden", "autobahnen vermeiden", "meide autobahn")) { prefs.put(PreferenceKeys.AVOID_MOTORWAY, true); say(Lang.t("Autobahnen werden gemieden", "Avoiding motorways")); dest?.let { startRoute(it, departMin, true, startNow = route != null && !previewing) }; return "" }
    if (has("avoid this road", "avoid the road", "diese straße vermeiden", "straße meiden", "umfahrung", "umleitung", "andere route", "anderen weg")) { if (route != null) { say(Lang.t("Suche eine Umfahrung", "Looking for a way round")); avoidRoadAhead() } else say(Lang.t("Es gibt keine Route zum Ändern", "There's no route to change")); return "" }
    // trip info
    if (has("eta", "arrive", "how long", "how far", "time left", "when will i", "wann komme", "wann sind wir", "wie lange", "wie weit", "ankunft", "restzeit", "wie viel zeit")) {
        val e = eta
        if (e == null) say(Lang.t("Gerade ist keine Route aktiv", "There's no route at the moment"))
        else say(Lang.t("Ankunft um ${e.arrive}, in ${Geo.fmtDur(e.secs)}. Noch ${Geo.spokenPlain(e.dist, imperial)}.", "You'll arrive at ${e.arrive}, in ${Geo.fmtDur(e.secs)}. ${Geo.spoken(e.dist, imperial)} to go.") + if (e.delayMin >= 1) Lang.t(" ${e.delayMin} Minuten Verzögerung durch Verkehr.", " ${e.delayMin} minutes of traffic.") else "")
        return ""
    }
    if (has("share", "teilen", "teile", "schicke", "sende")) return "share"
    // sound
    if (has("unmute", "sound on", "turn the voice on", "voice on", "ton an", "ton ein", "stimme an", "sprache an", "ansagen an", "lauter")) { prefs.put(PreferenceKeys.SOUND_MODE, 0); say(Lang.t("Sprachansagen an", "Voice guidance on")); return "" }
    if (has("alerts only", "nur warnungen", "nur warnung")) { prefs.put(PreferenceKeys.SOUND_MODE, 1); say(Lang.t("Nur Warnungen", "Alerts only")); return "" }
    if (has("mute", "be quiet", "quiet", "stop talking", "silence", "voice off", "stumm", "ruhe", "leise", "ton aus", "sprache aus", "ansagen aus", "sei still", "halt die klappe")) { say(Lang.t("Sprachansagen aus", "Voice off")); prefs.put(PreferenceKeys.SOUND_MODE, 2); return "" }
    // the map
    if (has("stop navigation", "end route", "end navigation", "cancel route", "cancel navigation", "stop the route", "stop route",
            "navigation beenden", "navigation stoppen", "route beenden", "route abbrechen", "navigation abbrechen", "beende die", "stoppe die", "abbrechen")) { endRoute(); say(Lang.t("Route beendet", "Route ended")); return "" }
    if (has("overview", "show the route", "show route", "whole route", "übersicht", "ganze route", "route zeigen", "zeig die route", "zeige die route")) { overview(); return "" }
    if (has("zoom in", "reinzoomen", "hineinzoomen", "näher", "vergrößern")) { zoom(1.0); return "" }
    if (has("zoom out", "rauszoomen", "herauszoomen", "weiter weg", "verkleinern")) { zoom(-1.0); return "" }
    if (has("north up", "norden oben", "nach norden")) { if (!northUp) toggleNorthUp(); return "" }
    if (t == "3d" || t == "2d" || has("three d", "two d", "drei d", "zwei d")) { if ((t.contains("3") || t.contains("three") || t.contains("drei")) != tilt3d) toggleTilt(); return "" }
    // where you parked: "wo steht mein Auto", "zum Auto", "hier geparkt"
    if (has("where did i park", "where is my car", "where's my car", "back to the car", "wo steht mein auto", "wo ist mein auto", "wo hab ich geparkt", "wo habe ich geparkt", "zum auto", "zurück zum auto")) {
        val spot = parkedSpot()
        if (spot == null) { say(Lang.t("Es ist kein Parkplatz gespeichert", "No parking spot is saved")); return "" }
        val d = fix?.pos?.let { Geo.dist(it, spot) }
        // the way on foot in the app (it used to open the phone's maps app, or this app's own driving route)
        toast(if (d != null) Lang.t("Dein Auto steht ${Geo.fmtDist(d, imperial)} entfernt", "Your car is ${Geo.fmtDist(d, imperial)} away") else Lang.t("Fußweg zu deinem Auto", "Walking to your car"), 3500)
        startWalk(spot)
        return ""
    }
    if (has("i parked here", "remember where i parked", "hier geparkt", "parkplatz merken", "merk dir den parkplatz", "ich parke hier")) {
        val p = fix?.pos
        if (p == null) say(Lang.t("Ich kenne deinen Standort gerade nicht", "I don't know where you are right now"))
        else { prefs.put(PreferenceKeys.PARKED_SPOT, String.format(Locale.US, "%.6f,%.6f,%d", p.lat, p.lon, System.currentTimeMillis())); say(Lang.t("Parkplatz gemerkt", "Parking spot saved")) }
        return ""
    }
    // satellite photos on or off
    if (has("satellite", "satellit", "luftbild")) {
        val on = !has(" off", "aus", "normal", "standard")
        prefs.put(PreferenceKeys.SATELLITE, on); say(if (on) Lang.t("Satellitenkarte an", "Satellite on") else Lang.t("Satellitenkarte aus", "Satellite off")); return ""
    }
    // the speed limit here
    if (has("speed limit", "tempolimit", "wie schnell darf ich", "geschwindigkeitsbegrenzung", "höchstgeschwindigkeit", "wie schnell ist hier erlaubt")) {
        val lim = speedLimit?.let { Math.round(it * 3.6) }
        say(if (lim == null && noLimit) Lang.t("Hier gibt es kein Tempolimit", "There's no speed limit here")
            else if (lim == null) Lang.t("Das Tempolimit hier kenne ich nicht", "I don't know the speed limit here")
            else if (imperial) Lang.t("Hier gilt ${Math.round(lim / 1.609)} Meilen pro Stunde", "The limit here is ${Math.round(lim / 1.609)} miles per hour")
            else Lang.t("Hier gilt $lim Kilometer pro Stunde", "The limit here is $lim kilometres per hour"))
        return ""
    }
    if (has("what time", "wie spät", "uhrzeit", "wieviel uhr", "wie viel uhr")) { say(Lang.t("Es ist ${f.format(Date())}", "It's ${f.format(Date())}")); return "" }
    // "take me to …", "navigate to …", "fahr nach …", "navigiere zu …"
    Regex("^(?:go to|take me to|navigate to|drive to|directions to|head to|find|search for|show me)\\s+(.+)$").find(t)?.let { return "search:" + it.groupValues[1] }
    deDest?.let { return "search:$it" }
    Regex("^(?:suche?|such mal|finde?|zeig(?:e)? mir|wo ist|wo gibt es)\\s+(?:nach\\s+)?(.+)$").find(t)?.let { return "search:" + it.groupValues[1] }
    Regex("^(?:nach|zu|zum|zur)\\s+(.+)$").find(t)?.let { return "search:" + it.groupValues[1] }
    return null
}

/** The route's line from [a] to [b] metres along it. */
internal fun NavEngine.slice(r: Route, a: Double, b: Double): List<LL> {
    val out = ArrayList<LL>()
    out += pointAt(r, a).first
    for (i in r.pts.indices) { val c = r.cum[i]; if (c > a && c < b) out += r.pts[i] }
    out += pointAt(r, b).first
    return out
}

/** Draws the average speed zones on the route on the map: the road between the cameras, and the cameras at each end. */
private fun NavEngine.pushAvgZones(r: Route, zones: List<AvgOnRoute>) {
    val lines = ArrayList<List<LL>>()
    val cams = ArrayList<Pair<LL, Double>>()
    for (z in zones) {
        val pts = ArrayList<LL>()
        val step = if (z.end - z.start > 20_000.0) 60.0 else 25.0
        var o = z.start
        while (o < z.end) { pts += pointAt(r, o).first; o += step }
        pts += pointAt(r, z.end).first
        lines += pts
        cams += pointAt(r, z.start); cams += pointAt(r, z.end)
    }
    map.setAvgZones(lines, cams)
}

/** Once a second while navigating: fetches and places services, bumps, schools and average speed checks, and raises their alerts. */
internal fun NavEngine.routeExtrasTick(r: Route, progress: Double) {
    try { checkVignette(r, progress) } catch (e: Exception) { }
    if (previewing) return
    // your destination's building lights up for the last 400 m (and goes back to normal when you get there)
    run {
        val dp = dest?.pos
        val left = r.len - progress
        map.highlightDestination(if (dp != null && left in 15.0..400.0 && prefs.b(PreferenceKeys.LIGHT_DEST, true)) dp else null)
    }
    val now = System.currentTimeMillis()
    barProgress = progress
    val d = dest
    // motorway services: once per destination, for longer trips
    if (d != null && servicesFor !== d && !servicesBusy && r.len > 15_000 && now > servicesRetryAt && online()) {
        servicesBusy = true
        val line = RouteExtras.line(r, progress, progress + 400_000.0, 2000.0, 220)
        scope.launch {
            try { rawServices = withContext(Dispatchers.IO) { RouteExtras.services(line) }; servicesFor = d }
            catch (e: Exception) { servicesRetryAt = System.currentTimeMillis() + 120_000L }
            finally { servicesBusy = false }
        }
    }
    // speed bumps, schools and average speed checks: a window of the next 20 km, fetched again before you reach its end
    // (and motorway junction names, outside the UK and Ireland: they come in the same request)
    val wantJct = jctCountry()
    // (average speed checks have a small request of their own: this one is the slowest and the most likely to fail)
    try { avgLookupTick(r, progress, now) } catch (e: Exception) { }
    // (the average speed setting still switches this search on, as it always did: crossings, stop signs and level crossings come with it)
    val wantHz = prefs.b(PreferenceKeys.BUMPS, false) || prefs.b(PreferenceKeys.SCHOOLS, false) || prefs.b(PreferenceKeys.AVGZONES, true) || wantJct
    // (a new route after a reroute is looked up again, but not more than once every 45 seconds)
    if (wantHz && !hzBusy && (progress + 6000.0 > hzWinEnd || (hzWinRoute !== r && now - hzLastAt > 45_000L)) && now > hzRetryAt && online()) {
        hzBusy = true; hzLastAt = now
        val to = progress + 20_000.0
        val line = RouteExtras.line(r, progress - 300.0, to, 250.0, 90)
        scope.launch {
            try {
                val jl = ArrayList<Jct>()
                val p = withContext(Dispatchers.IO) { RouteExtras.hazards(line, if (wantJct) jl else null) }
                rawHz = (rawHz + p).distinctBy { "${it.kind}${Math.round(it.pos.lat * 1e5)},${Math.round(it.pos.lon * 1e5)}" }.takeLast(1500)
                if (jl.isNotEmpty()) { rawJcts = (rawJcts + jl).distinctBy { "${Math.round(it.pos.lat * 1e5)},${Math.round(it.pos.lon * 1e5)}" }.takeLast(600); jctsVersion++ }
                hzWinRoute = r; hzWinEnd = to
            } catch (e: Exception) { hzRetryAt = System.currentTimeMillis() + 60_000L }
            finally { hzBusy = false }
        }
    }
    // the junctions on this route (kept apart from the hazards: nothing to warn about, only names for the turn card and the voice)
    val js = System.identityHashCode(r) * 31 + jctsVersion
    if (js != jctsSig && !jctsPlacing) {
        jctsPlacing = true
        val jl = rawJcts
        scope.launch {
            try {
                val on = withContext(Dispatchers.Default) { jl.mapNotNull { j -> RouteExtras.project(r, j.pos, 30.0)?.let { JctOnRoute(j, it.first) } }.sortedBy { it.off } }
                jctsOn = r to on
            } catch (e: Exception) { }
            finally { jctsSig = js; jctsPlacing = false }
        }
    }
    // place everything on this route (in the background) whenever the route or what was found changes
    val sig = ((System.identityHashCode(r) * 31 + rawServices.size) * 31 + rawHz.size) * 31 + rawAvg.size * 7 + via.size * 13 + zonesVersion * 17
    if (sig != placedSig && !placing) {
        placing = true
        val sv = rawServices; val hz = rawHz; val av = rawAvg; val vs = via
        val zoneWarn = prefs.b(PreferenceKeys.ZONEWARN, true)
        val fuel = prefs.s(PreferenceKeys.FUEL_TYPE, "E10")
        val fuelOn = FuelFinder.configured(prefs) && fuel != "EV"
        scope.launch {
            try {
                val pl = withContext(Dispatchers.Default) {
                    val svc = sv.mapNotNull { poi -> RouteExtras.project(r, poi.pos, 400.0)?.let { (o, side) -> PoiOnRoute(poi, o, side) } }.sortedBy { it.off }
                    val fuelText = HashMap<RoutePoi, String>()
                    if (fuelOn) {
                        val st = FuelFinder.stations(ctx)
                        for (s in svc) {
                            val near = st.filter { Geo.dist(it.pos, s.poi.pos) < 700.0 }.minByOrNull { Geo.dist(it.pos, s.poi.pos) } ?: continue
                            FuelFinder.priceOf(ctx, near.id, fuel)?.let { fuelText[s.poi] = String.format(java.util.Locale.UK, "%s %.1fp", if (fuel == "B7") "Diesel" else fuel, it.pence) }
                        }
                    }
                    val hzOn = hz.mapNotNull { poi -> RouteExtras.project(r, poi.pos, if (poi.kind == "school") 90.0 else if (poi.kind == "crossing") 12.0 else 15.0)?.let { (o, side) -> PoiOnRoute(poi, o, side) } }
                    val avg = av.mapNotNull { RouteExtras.placeAvg(r, it) }.sortedBy { it.start }
                    val stops = vs.mapNotNull { RouteExtras.project(r, it.pos, 300.0)?.first }
                    val zs = if (zoneWarn) Zones.entries(r, Zones.all(ctx)).map { Triple(it.first.name, it.second, it.first.country) } else emptyList()
                    Placed(r, svc, fuelText, hzOn, avg, stops, zs)
                }
                placed = pl; placedSig = sig
                pushAvgZones(r, if (prefs.b(PreferenceKeys.AVGZONES, true)) pl.avg else emptyList())
                // zebra and other marked crossings on the route, painted on the road
                map.setCrossings(if (prefs.b(PreferenceKeys.ZEBRAS, true)) pl.hz.filter { it.poi.kind == "crossing" && it.poi.name in setOf("zebra", "marked", "uncontrolled") }
                    .take(300).map { Triple(it.poi.pos, pointAt(r, it.off).second, it.poi.name == "zebra") } else emptyList())
                // stop signs on the route
                map.setStopSigns(if (prefs.b(PreferenceKeys.STOP_SIGNS, false)) pl.hz.filter { it.poi.kind == "stop" }.take(300).map { it.poi.pos } else emptyList())
            } catch (e: Exception) { placedSig = sig } finally { placing = false }
        }
    }
    val pl = placed?.takeIf { it.route === r }
    if (pl == null) { if (avgInfo != null) avgInfo = null; return }
    val sp = fix?.speed ?: 0.0

    // sharp bends: striped on the map, and a sign with a chime a few seconds before (only when you're faster than it can be taken)
    run {
        val on = prefs.b(PreferenceKeys.SHARPBENDS, false)
        if (sharpFor !== r) {
            sharpFor = r; sharp = emptyList(); sharpShown = false; map.setSharpBends(emptyList())
            scope.launch { val b = withContext(Dispatchers.Default) { try { sharpBends(r) } catch (e: Exception) { emptyList() } }; if (sharpFor === r) { sharp = b; sharpShown = false } }
        }
        if (on != sharpShown && (sharp.isNotEmpty() || !on)) { sharpShown = on; map.setSharpBends(if (on) sharp.map { slice(r, it.start, it.end) } else emptyList()) }
        if (on) for (b in sharp) {
            val ahead = b.start - progress
            if (ahead < 0.0 || ahead > max(60.0, sp * 9.0) || sp < b.safeMs * 1.1) continue
            val key = "sharp${Math.round(b.start)}"
            if (!hzAlerted.add(key)) continue
            val k = if (imperial) 2.23694 else 3.6
            val v = max(5L, Math.round(b.safeMs * k / 5.0) * 5L)
            chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
            raiseAlert("bend", Lang.t("Scharfe Kurve · mit $v ${if (imperial) "mph" else "km/h"} nehmen", "Sharp bend · take it at $v ${if (imperial) "mph" else "km/h"}"), 5000)
        }
    }

    // average speed check
    if (prefs.b(PreferenceKeys.AVGZONES, true)) {
        val cur = pl.avg.firstOrNull { progress >= it.start && progress < it.end }
        if (cur != null) {
            if (avgIn !== cur) {
                avgIn = cur; avgEnterAt = now; avgEnterProg = progress; avgOverWarned = false
                if (avgAlerted.add(cur.def.id)) {
                    chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
                    raiseAlert("avg", Lang.t("Abschnittskontrolle noch ${Geo.fmtDist(cur.end - progress, imperial)}", "Average speed check for ${Geo.fmtDist(cur.end - progress, imperial)}"), 6000)
                    speak(Lang.t("Abschnittskontrolle noch ${Geo.spokenPlain(cur.end - progress, imperial)}", "Average speed check for ${Geo.spoken(cur.end - progress, imperial)}"), true)
                }
            }
            val secs = (now - avgEnterAt) / 1000.0
            val avg = if (secs > 4.0) (progress - avgEnterProg) / secs else sp
            // never more than the road's own signed limit (a mapped check limit can be a different unit's number)
            val lim = cur.def.limit?.let { l -> speedLimit?.let { min(it, l) } ?: l } ?: speedLimit
            val over = lim != null && secs > 15.0 && avg > lim + 0.45
            if (over && !avgOverWarned) {
                avgOverWarned = true
                chime(android.media.ToneGenerator.TONE_PROP_BEEP2)
                speak(Lang.t("Ihre Durchschnittsgeschwindigkeit liegt über dem Limit", "Your average speed is over the limit"), true)
            } else if (!over && lim != null && avg < lim - 0.5) avgOverWarned = false
            // the speed you can do for the rest of the zone and still average under the limit: the time the whole zone may take
            // at the limit, less the time used so far, spread over what's left
            val left = cur.end - progress
            val safe = if (lim != null && secs > 4.0) {
                val allowed = (cur.end - avgEnterProg) / lim - secs
                if (allowed > 1.0) left / allowed else 0.0
            } else null
            avgInfo = AvgInfo(avg, lim, left, over, false, 0.0, safe)
        } else {
            if (avgIn != null) { avgIn = null; toast(ctx.getString(R.string.toast_008), 2500) }
            val next = pl.avg.filter { it.start > progress && it.start - progress < 1200.0 }.minByOrNull { it.start }
            avgInfo = next?.let { n -> AvgInfo(0.0, n.def.limit?.let { l -> speedLimit?.let { min(it, l) } ?: l } ?: speedLimit, n.end - n.start, false, true, n.start - progress) }
            // as early as a camera is warned about (the same distance, which grows with speed)
            if (next != null && next.start - progress < max(500.0, sp * 18.0) && avgAlerted.add(next.def.id)) {
                chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
                raiseAlert("avg", Lang.t("Abschnittskontrolle voraus, ${Geo.fmtDist(next.end - next.start, imperial)} lang", "Average speed check ahead, ${Geo.fmtDist(next.end - next.start, imperial)} long"), 6000)
                speak(Lang.t("Abschnittskontrolle voraus", "Average speed check ahead"), true)
            }
        }
    } else if (avgInfo != null) avgInfo = null

    if (prefs.b(PreferenceKeys.HAZARDRADAR, false)) updateRadar(r, progress, pl)

    // weather and night advice: a quiet note (no sound) in heavy rain, snow, fog or ice when you're near the limit, once per 10 minutes
    run {
        val code = weather?.code
        val lim = speedLimit
        val what = when (code) {
            45, 48 -> Lang.t("Nebel", "Fog")
            63, 65, 81, 82 -> Lang.t("Starkregen", "Heavy rain")
            in 95..99 -> Lang.t("Gewitter", "Thunderstorms")
            56, 57, 66, 67 -> Lang.t("Eisregen", "Freezing rain")
            in 71..77, 85, 86 -> Lang.t("Schnee", "Snow")
            else -> null
        }
        val nowMs = System.currentTimeMillis()
        if (prefs.b(PreferenceKeys.WXADVICE, true) && what != null && lim != null && sp > lim * 0.85 && nowMs - adviceAt > 600_000L) {
            adviceAt = nowMs
            val k = if (imperial) 2.23694 else 3.6
            advice = Lang.t("$what · vorsichtig fahren, Tempo ${Math.round(lim * k / 5.0) * 5}", "$what · take it steady, ${Math.round(lim * k / 5.0) * 5} limit")
            scope.launch { delay(9000); advice = null }
        }
    }

    // stop signs on the route just ahead: a short chime and a pill (Settings > Junctions and lanes > Stop signs)
    if (prefs.b(PreferenceKeys.STOP_SIGNS, false)) for (h in pl.hz) {
        if (h.poi.kind != "stop") continue
        val ahead = h.off - progress
        if (ahead < 0.0 || ahead > max(80.0, sp * 6.0)) continue
        val key = "stop${Math.round(h.poi.pos.lat * 1e5)},${Math.round(h.poi.pos.lon * 1e5)}"
        if (!hzAlerted.add(key)) continue
        chime(android.media.ToneGenerator.TONE_PROP_ACK)
        raiseAlert("stop", Lang.t("Stoppschild voraus", "Stop sign ahead"), 4000)
    }

    // level crossings and fords just ahead: a sign and a spoken warning
    if (prefs.b(PreferenceKeys.RAILFORD, false)) for (h in pl.hz) {
        if (h.poi.kind != "rail" && h.poi.kind != "ford") continue
        val ahead = h.off - progress
        if (ahead < 0.0 || ahead > max(220.0, sp * 9.0)) continue
        val key = "${h.poi.kind}${Math.round(h.poi.pos.lat * 1e5)},${Math.round(h.poi.pos.lon * 1e5)}"
        if (!hzAlerted.add(key)) continue
        if (h.poi.kind == "rail") { raiseAlert("rail", Lang.t("Bahnübergang voraus", "Level crossing ahead"), 6000); speak(Lang.t("Bahnübergang voraus", "Level crossing ahead"), true) }
        else { raiseAlert("ford", Lang.t("Furt voraus: Die Straße kann überflutet sein", "Ford ahead: the road may be flooded"), 7000); speak(Lang.t("Furt voraus. Die Straße kann überflutet sein", "Ford ahead. The road may be flooded"), true) }
    }

    // speed bumps and schools just ahead
    for (h in pl.hz) {
        val ahead = h.off - progress
        if (ahead < 0.0 || ahead > 320.0 || h.poi.kind == "crossing" || h.poi.kind == "rail" || h.poi.kind == "ford" || h.poi.kind == "stop") continue
        val key = "${h.poi.kind}${Math.round(h.poi.pos.lat * 1e5)},${Math.round(h.poi.pos.lon * 1e5)}"
        if (h.poi.kind == "bump") {
            if (prefs.b(PreferenceKeys.BUMPS, false) && sp > 6.7 && ahead < max(70.0, sp * 6.0) && now - lastBumpAt > 25_000L && hzAlerted.add(key)) {
                lastBumpAt = now
                chime(android.media.ToneGenerator.TONE_PROP_ACK)
                raiseAlert("bump", Lang.t("Bodenwelle voraus", "Speed bump ahead"), 3500)
            }
        } else if (prefs.b(PreferenceKeys.SCHOOLS, false) && ahead < 300.0 && RouteExtras.schoolTime(now) && hzAlerted.add(key)) {
            raiseAlert("school", Lang.t("Schule voraus: ${h.poi.name}", "School ahead: ${h.poi.name}"), 5000)
            speak(Lang.t("Schule voraus", "School ahead"), true)
        }
    }

    // clean air / low emission zones
    if (prefs.b(PreferenceKeys.ZONEWARN, true)) for ((name, off, zc) in pl.zones) {
        val ahead = off - progress
        if (ahead in -50.0..1500.0 && zoneAlerted.add(name)) {
            // the zone's own country decides the rules (the German download also holds Strasbourg, Basel, Prague, Arnhem...)
            if (zc == "DE") {
                // a German Umweltzone: no charge, but only cars with the green sticker may drive in
                val zn = if (name.contains("umweltzone", true)) name else "Umweltzone $name"
                raiseAlert("zone", if (ahead <= 0) Lang.t("In der $zn", "In the $zn") else Lang.t("$zn in ${Geo.fmtDist(ahead, imperial)}", "$zn in ${Geo.fmtDist(ahead, imperial)}"), 7000)
                speak(if (ahead <= 0) Lang.t("Sie sind in der Umweltzone. Nur mit grüner Plakette.", "You are in a low emission zone. Green sticker required.")
                      else Lang.t("Sie fahren gleich in die Umweltzone ein. Nur mit grüner Plakette.", "You are about to enter a low emission zone. Green sticker required."), true)
            } else if (zc != "GB") {
                // a zone abroad: its own rules (France's Crit'Air sticker, the Dutch milieuzones by emission class...), not the green sticker
                val note = when (zc) {
                    "FR" -> Lang.t("Crit'Air-Vignette nötig", "Crit'Air sticker required")
                    else -> Lang.t("Regeln vor Ort beachten", "Check the local rules")
                }
                val zn = when {
                    zc == "NL" && !name.contains("milieuzone", true) -> "Milieuzone $name"
                    zc != "NL" && !Regex("zone|zóna|zfe|strefa", RegexOption.IGNORE_CASE).containsMatchIn(name) -> Lang.t("Umweltzone $name", "Low emission zone $name")
                    else -> name
                }
                raiseAlert("zone", if (ahead <= 0) Lang.t("In $zn – $note", "In $zn – $note") else Lang.t("$zn in ${Geo.fmtDist(ahead, imperial)} – $note", "$zn in ${Geo.fmtDist(ahead, imperial)} – $note"), 7000)
                speak(if (ahead <= 0) Lang.t("Sie sind in einer Umweltzone. $note.", "You are in a low emission zone. $note.")
                      else Lang.t("Sie fahren gleich in eine Umweltzone ein. $note.", "You are about to enter a low emission zone. $note."), true)
            } else {
                raiseAlert("zone", if (ahead <= 0) Lang.t("Sie sind in $name", "You are in $name") else Lang.t("Einfahrt in $name in ${Geo.fmtDist(ahead, imperial)}", "Entering $name in ${Geo.fmtDist(ahead, imperial)}"), 7000)
                speak(if (ahead <= 0) Lang.t("Sie sind in $name. Es kann eine Gebühr anfallen.", "You are in $name. A charge may apply.") else Lang.t("Sie fahren gleich in $name ein. Es kann eine Gebühr anfallen.", "You are about to enter $name. A charge may apply."), true)
            }
        }
    }

    // next services (only republished when what a driver would see changes)
    // (one entry per services: the map data often has the same services twice, as the services and as its fuel station or car park,
    // or once per side of the motorway, which showed the same name twice)
    val nextSvc = ArrayList<PoiOnRoute>()
    for (sv in pl.services.filter { it.off > progress + 150.0 && it.off - progress < 130_000.0 && it.side < 400.0 }.sortedBy { it.off }) {
        val nm = sv.poi.name.lowercase().replace(Regex("\\b(services|service area|motorway services|moto|welcome break|roadchef|extra)\\b"), "").replace(Regex("[^a-z0-9]"), "")
        if (nextSvc.any { o -> Math.abs(o.off - sv.off) < 2500.0 || (nm.isNotEmpty() && o.poi.name.lowercase().replace(Regex("\\b(services|service area|motorway services|moto|welcome break|roadchef|extra)\\b"), "").replace(Regex("[^a-z0-9]"), "") == nm) }) continue
        nextSvc += sv
        if (nextSvc.size >= 1) break
    }
    var ss = nextSvc.size
    for (s in nextSvc) ss = ss * 31 + s.poi.name.hashCode() + ((s.off - progress) / 160.0).toInt()
    if (ss != servicesSig) {
        servicesSig = ss
        servicesAhead = nextSvc.map { ServiceAhead(it.poi.name, it.off - progress, pl.serviceFuel[it.poi].orEmpty()) }
    }

    // route bar markers
    if (prefs.b(PreferenceKeys.ROUTEBAR, true)) {
        val span = r.len
        val ev = ArrayList<BarEvent>()
        if (camerasOn()) for ((_, off) in routeCams) ev += BarEvent(off, "camera")
        for ((inc, off, _) in incMatches) ev += BarEvent(off, if (inc.cat.contains("work", true)) "works" else "incident")
        for (s in pl.services) if (s.side < 400.0) ev += BarEvent(s.off, "services")
        for (a in pl.avg) ev += BarEvent(a.start, "avg")
        // charging stops get the lightning marker, as on the map
        val chargeOffs = evPlanned.mapNotNull { RouteExtras.project(r, it.n.place.pos, 300.0)?.first }
        for (o in pl.stops) ev += BarEvent(o, if (chargeOffs.any { Math.abs(it - o) < 150.0 }) "charge" else "stop")
        for ((_, o) in pl.zones) ev += BarEvent(o, "zone")
        val shown = ev.filter { it.off >= progress - 30.0 && it.off <= span }
        var bs = shown.size
        for (e in shown) bs = bs * 31 + e.off.toInt() / 50 + e.kind.hashCode()
        if (bs != barSig) { barSig = bs; barEvents = shown }
    }
}

fun NavEngine.speakForce(text: String) {
    if (!ttsReady) return
    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "read")
}

/** The phone keeps quiet while the car screen is connected: the car says the directions and alerts. */
internal fun NavEngine.quiet() = !isCar && RouteSync.carConnected()

/**
 * The phone while the car screen runs the trip and the phone isn't showing its map: it keeps following the route (so it can
 * carry on by itself when the phone is unplugged) but leaves the lookups to the car: speed limits, cameras, traffic and faster
 * routes aren't fetched twice, which halves the requests on your TomTom key and saves the phone's battery.
 */
internal fun NavEngine.shadow() = quiet() && !showingMap

/** Joins a trip started on the other screen (the phone or the car): the same destination and stops, straight into guidance. */
fun NavEngine.followRoute(to: Place, stops: List<Place>, ev: List<EvStop> = emptyList(), chosen: Route? = null) {
    val d = dest
    if (d != null && Geo.dist(d.pos, to.pos) < 30.0 && route != null) {
        val sameStops = via.size == stops.size && via.zip(stops).all { (a, b) -> Geo.dist(a.pos, b.pos) < 30.0 }
        if (sameStops) {
            // already showing this route's choice (a place shared to both screens): set off with the way picked on the other
            // screen, not this screen's own pick (it was always the quickest)
            if (previewing) {
                if (chosen != null && routes.size > 1) {
                    val i = routes.indices.maxByOrNull { routeOverlap(routes[it], chosen) } ?: active
                    if (i != active && routeOverlap(routes[i], chosen) > 0.8) selectRoute(i)
                }
                beginNavigation()
            }
            return
        }
        // same trip, but the other screen changed its stops (e.g. charging stops added once the chargers were found mid-drive)
        via = stops
        evPlanned = ev; evAutoDest = to.pos
        evPreferRoute = chosen
        startRoute(to, departMin, keepVia = true, startNow = true)
        return
    }
    scope.launch {
        // a screen that has only just started may not know where it is yet: give the GPS a moment first
        var n = 0
        while (fix == null && n < 40) { delay(500); n++ }
        via = stops
        // the screen that started the trip planned its charging stops: this one takes them over and doesn't add its own
        evPlanned = ev; evAutoDest = to.pos
        evPreferRoute = chosen
        startRoute(to, keepVia = true, startNow = true)
    }
}
