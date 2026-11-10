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

class NavEngine(private val ctx: Context, val prefs: Prefs, val map: MapBinding) : LocationListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    /** Place searches run here, apart from everything else, so one stuck on a slow server can just be left behind. */
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ---- observable state ----
    var fix by mutableStateOf<Fix?>(null)
    var routes by mutableStateOf<List<Route>>(emptyList())
    /** The route picked as the simpler one (fewer turns), or -1: shown as "Einfachste" rather than "Kürzeste". */
    var simplerIdx by mutableStateOf(-1)
    var routeFrac by mutableStateOf(0f)
    var active by mutableIntStateOf(0)
    var dest by mutableStateOf<Place?>(null)
    var turn by mutableStateOf<TurnInfo?>(null)
    var eta by mutableStateOf<EtaInfo?>(null)
    var following by mutableStateOf(true)
    // (remembered: it used to start in 3D every time, even if you'd left it in 2D)
    var tilt3d by mutableStateOf(prefs.b(PreferenceKeys.VIEW_3D, true))
    var simMode by mutableIntStateOf(0)
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
    /** Saved & recent under the search bar starts folded on every start (app or car connection); a tap unfolds it until the next one. */
    var quickFolded by mutableStateOf(true)
    var previewing by mutableStateOf(false)
    var northUp by mutableStateOf(false)
    var results by mutableStateOf<List<Place>>(emptyList())
    var alternatives by mutableStateOf(false)
    var weather by mutableStateOf<Weather.Now?>(null)
    var zoomOffset by mutableDoubleStateOf(0.0)
    /** Your own normal zoom when not on a route (Settings > Map look), in levels from the standard one. */
    val defaultZoom: Double get() = (prefs.s(PreferenceKeys.DEFAULT_ZOOM, "0").toDoubleOrNull() ?: 0.0).coerceIn(-2.0, 2.0)
    /** The flat traffic view after being stopped a while is showing. */
    var stoppedOverview by mutableStateOf(false)
    private var stoppedOverviewActive = false
    private var stoppedSince = 0L
    /** A place sent from your phone, waiting to be started (head unit). */
    var phoneDest by mutableStateOf<Place?>(null)
    /** The stops sent along with [phoneDest], and points along the way picked on the phone. */
    var phoneStops: List<Place> = emptyList()
        private set
    private var phoneTrack: List<LL> = emptyList()
    /** Points along a way picked elsewhere: the next route choice takes the way that follows them most closely (see startRoute). */
    private var preferTrack: List<LL> = emptyList()
    /** Additional auto-zoom adjustment for Android Auto's usable viewport. Negative values zoom out
     * on compact or narrow displays, where the same camera zoom shows too little road ahead. */
    @Volatile private var carViewportZoomOffset = 0.0

    /** Recalculate the camera's automatic zoom for the actual Android Auto display, not just its pixel count.
     * Inputs are surface pixels and the display's DPI. The short edge controls the base adjustment and
     * aspect ratio adds a smaller correction for unusually narrow/tall or very wide screens. */
    fun setCarViewport(widthPx: Int, heightPx: Int, dpi: Int) {
        if (!isCar || widthPx <= 0 || heightPx <= 0 || dpi <= 0) return
        // Keep viewport sizing as a pure policy so its boundaries can be unit-tested without a map SDK.
        carViewportZoomOffset = NavigationPolicy.carViewportZoomOffset(widthPx, heightPx, dpi)
    }

    // saved places, departure planner, road info
    val places = PlaceStore(prefs)
    val trips = TripStore(prefs)
    var pendingSave by mutableStateOf<String?>(null)
    var departMin by mutableIntStateOf(0)
    var departAtMs by mutableLongStateOf(0L)
    var speedLimit by mutableStateOf<Double?>(null)     // m/s
    var limitEstimated by mutableStateOf(false)
    /** The road has no general limit (an unrestricted Autobahn): the "end of all restrictions" sign shows instead of a number. Only while [speedLimit] is null. */
    var noLimit by mutableStateOf(false)
    /** The sources don't agree on the limit shown (a small mark on the sign). */
    var limitDisputed by mutableStateOf(false)
    /** What each source said about the limit ("Schild 50 · TomTom 70 · OSM 50"), shown when the sign is tapped. */
    var limitDetails by mutableStateOf("")
    private var limitOkAt = 0L
    private var limitOkPos: LL? = null
    private var limitFails = 0
    var cameraDist by mutableStateOf<Int?>(null)
    var incidentAhead by mutableStateOf<IncidentAhead?>(null)
    private var incAt = 0L
    private var incMatches: List<Triple<Incident, Double, String>> = emptyList()
    /** Autobahn GmbH's roadworks and closures on the motorways of the route (AutobahnRest): fetched at the route's start and every 5 minutes. */
    private var abAt = 0L
    private var abRoads = ""
    private var abItems: List<Incident> = emptyList()
    private var routeCams: List<Pair<LL, Double>> = emptyList()
    private var camsRoute: Route? = null
    private var camsAt = 0L
    private var hazardSig = 0
    private var hzProgress = 0.0
    private var policeSig = 0
    private val policeAlerted = HashSet<Long>()
    private val incAlerted = HashSet<String>()
    var nearby by mutableStateOf<List<Nearby>>(emptyList())
    var nearbyKind by mutableStateOf<String?>(null)
    var nearbyBusy by mutableStateOf(false)

    // trip computer (real GPS only)
    var tripDist by mutableDoubleStateOf(0.0)
    var tripMax by mutableDoubleStateOf(0.0)
    var tripMoving by mutableDoubleStateOf(0.0)
    var tripStart by mutableStateOf(System.currentTimeMillis())
    /** Where the trip being driven started (for Lang.t("Rückweg", "Route back") on the arrival card). */
    var tripFrom: LL? = null
    /** A short, quiet piece of advice for the conditions ("Heavy rain · take it steady"), or null. */
    var advice by mutableStateOf<String?>(null)
    private var adviceAt = 0L

    /** Flying over the chosen route in 3D (Fly over route): the start countdown waits while it plays. */
    var flyingOver by mutableStateOf(false)

    /** A 3D flight along the whole of the chosen route, then back to the overview. Touching the map stops it. */
    fun flyOver() {
        val r = routes.getOrNull(active) ?: return
        if (!previewing) return
        flyingOver = true
        val allPts = routes.take(3).flatMap { x -> x.pts.filterIndexed { i, _ -> i % 8 == 0 } + x.pts.last() }
        map.flyRoute(r.pts, r.cum, r.len, 62.0, full = true) { flyingOver = false; if (previewing) map.fit(allPts) }
    }

    /** The way back to where this trip started (home when it started near home). */
    fun routeBack() {
        val from = tripFrom ?: return
        val home = places.slot("home")
        val name = if (home != null && Geo.dist(home.pos, from) < 250.0) home.name.ifBlank { Lang.t("Zuhause", "Home") } else Lang.t("Startpunkt", "Where you started")
        dismissArrival()
        startRoute(Place(name, "", if (home != null && Geo.dist(home.pos, from) < 250.0) home.pos else from))
    }

    // extras
    val habits = HabitStore(prefs)
    var overLimit by mutableStateOf(false)
    var via by mutableStateOf<List<Place>>(emptyList())
    var stopMode by mutableStateOf(false)
    var destRain by mutableStateOf<String?>(null)
    var routeWx by mutableStateOf<List<RouteWx>>(emptyList())
    var arriveFlash by mutableStateOf(false)
    var arrival by mutableStateOf<ArrivalInfo?>(null)

    /**
     * A spot picked by tapping the map in the car (Android Auto passes no long press): a pin and a card with "Hierhin fahren"
     * (while driving a route: "Als Stopp"). [sub] is the address once it's known. Goes away by itself after 15 s.
     */
    var mapPick by mutableStateOf<Place?>(null)
        private set
    private var pickJob: kotlinx.coroutines.Job? = null

    fun carMapTap(sp: android.graphics.PointF) { map.pickAt(sp) { p, name -> showPick(p, name) } }

    /** Shows the card for the spot [p] (a place called [name], or just a point): on the car screen, and for a place tapped on the phone. */
    fun showPick(p: LL, name: String?) {
        run {
            val first = Place(name ?: Lang.t("Markierter Ort", "Marked spot"), "", p)
            mapPick = first
            map.setPick(p)
            pickJob?.cancel()
            pickJob = scope.launch {
                launch {
                    val addr = withContext(Dispatchers.IO) { try { PoiSearch.addressAt(p) } catch (e: Exception) { null } } ?: return@launch
                    val cur = mapPick
                    if (cur != null && Geo.dist(cur.pos, p) < 1.0) mapPick = (if (name != null) Place(name, addr, p) else Place(addr.substringBefore(", "), addr.substringAfter(", ", ""), p))
                        .also { it.hours = cur.hours; it.web = cur.web; it.brand = cur.brand }
                }
                // a named place: its opening times and website (for the logo) from TomTom's search, when there is a key
                if (name != null && hasKey && refusedKey != apiKey && online()) launch {
                    val hit = withContext(Dispatchers.IO) { try { TomTomRest.poiNear(apiKey, name, null, p, 150).firstOrNull() } catch (e: Exception) { null } } ?: return@launch
                    val cur = mapPick
                    if (cur != null && Geo.dist(cur.pos, p) < 1.0) {
                        mapPick = Place(cur.name, cur.sub, cur.pos).also { it.hours = hit.hours; it.web = hit.web; it.brand = hit.brand }
                        loadLogos(listOfNotNull(mapPick))
                    }
                }
                delay(15_000)
                clearPick()
            }
        }
    }

    fun clearPick() { pickJob?.cancel(); pickJob = null; mapPick = null; map.setPick(null) }

    /** The picked spot becomes a stop on the trip being driven or chosen (the phone's "Als Stopp"). */
    fun stopPick() {
        val p = mapPick ?: return
        clearPick()
        if (dest != null && route != null) addStop(p) else startRoute(p)
    }

    /** The picked spot becomes the destination (the phone's "Los"). */
    fun routePick() { val p = mapPick ?: return; clearPick(); startRoute(p) }

    /** Goes to the picked spot, or adds it as a stop while a route is being driven. */
    fun goPick() {
        val p = mapPick ?: return
        clearPick()
        if (route != null && !previewing && dest != null) addStop(p) else startRoute(p)
    }
    /** A quicker route than the one being followed, found while driving. */
    class FasterRoute(val route: Route, val savedMin: Int)
    var faster by mutableStateOf<FasterRoute?>(null)
    var liveAlts by mutableStateOf<List<LiveAlt>>(emptyList())
    var radar by mutableStateOf<List<RadarItem>>(emptyList())
    private var radarSig = 0
    private var bendsFor: Route? = null
    private var bends: List<Pair<Double, Int>> = emptyList()
    var prediction by mutableStateOf<Prediction?>(null)
    private var predictionAt = 0L
    var arrivalParking by mutableStateOf<Nearby?>(null)
    /** Roads driven on this trip, counted into the familiar-roads list when the trip ends. */
    private val tripRoads = HashSet<String>()
    private val roadFam: HashMap<String, Int> by lazy {
        val m = HashMap<String, Int>()
        try { val o = org.json.JSONObject(prefs.s(PreferenceKeys.ROADFAM, "{}")); for (k in o.keys()) m[k] = o.optInt(k) } catch (e: Exception) { }
        m
    }
    private var lastFasterCheck = 0L
    /** Destination of a trip that was still running when the app last closed. */
    var resume by mutableStateOf<Place?>(null)
    private var navStartMs = 0L
    /** Where the trip under way goes: a restart to the same place (new stops) continues the trip instead of starting a new one. */
    private var navTripDest: LL? = null
    private var navStartDist = 0.0
    var brakeFlash by mutableStateOf(false)
    /** True for a few seconds after the speed limit changes, so the sign can pulse. */
    var limitPulse by mutableStateOf(false)
    /** The alert banner currently on screen (camera, speed limit, incident, rain, break). */
    var alert by mutableStateOf<AlertInfo?>(null)
    private var alertToken = 0

    // ---- route extras: average speed checks, services, bumps, schools, zones, the route bar ----
    /** The average speed check you are in (or about to enter), or null. */
    var avgInfo by mutableStateOf<AvgInfo?>(null)
    /** The next motorway services ahead on the route (up to two). */
    var servicesAhead by mutableStateOf<List<ServiceAhead>>(emptyList())
    /** Markers for the route bar, and how far along the route you are (updated about once a second). */
    var barEvents by mutableStateOf<List<BarEvent>>(emptyList())
    var barProgress by mutableDoubleStateOf(0.0)
    /** Clean air / low emission zones the chosen route goes into (shown on the route options). */
    var routeZones by mutableStateOf<List<String>>(emptyList())
    /** Countries of [routeZones] ("DE", "GB", "FR", or "" when unknown), to say which rules apply. */
    var routeZoneCountries by mutableStateOf<Set<String>>(emptySet())
    /** True while the GPS has dropped out (a tunnel) and the position is being estimated along the route. */
    var deadReckoning by mutableStateOf(false)
    /** Text shared from another app, to be put in the search box. */
    var externalQuery by mutableStateOf<String?>(null)
    /** Goes up each time another app hands over a destination (closes the settings screen). */
    var externalTick by mutableIntStateOf(0)
    /** The stops editor is open. */
    var stopsEditor by mutableStateOf(false)
    /** How the fuel list is sorted: "near" or "cheap". */
    var nearbySort by mutableStateOf("near")
    /** Name of the road you are on (route or map data), for the free-driving card. */
    var roadNow by mutableStateOf("")
    /** Roads you asked to avoid on this trip (as boxes). */
    private val sessionAvoid = ArrayList<Pair<LL, LL>>()

    /** A trip planned with "arrive by": where, when to be there, and when to leave (kept until it starts or is cancelled). */
    class PlannedTrip(val dest: Place, val arriveBy: Long, val leaveBy: Long)
    var planned by mutableStateOf<PlannedTrip?>(null)
    private var plannedJob: Job? = null
    /** A good place to fill up on a long trip (cheapest around the middle of the route, or the quickest detour). */
    var fuelSuggestion by mutableStateOf<Nearby?>(null)
    /** Electric: the charging stops the trip needs, and a warning if it can't be done with the reserve. */
    var evStops by mutableStateOf<List<EvStop>>(emptyList())
    var evWarning by mutableStateOf<String?>(null)
    /** The charging stops that were put on this trip (by the plan or the "Add stops" pill), kept for their charge and minutes. */
    var evPlanned by mutableStateOf<List<EvStop>>(emptyList())
    /** Where the charging stops were last added on their own: once per destination, so stops you took off don't come back. */
    private var evAutoDest: LL? = null
    /** The warning that came with the plan put on the trip (a stretch it couldn't cover), shown again once the stops are on. */
    private var evPlanWarn: String? = null
    /** The route whose charging stops are still being worked out (the countdown waits for them, as they may be added to it). */
    private var evPlanningFor by mutableStateOf<Route?>(null)
    val evPlanning: Boolean get() = evPlanningFor.let { it != null && it === route }
    /** Minutes the planned charging stops take (charging only, without the detours). */
    fun evChargeMinutes(): Int = evPlanned.sumOf { it.minutes }
    private var wrongCount = 0
    private var wrongAt = 0L

    private class Placed(
        val route: Route, val services: List<PoiOnRoute>, val serviceFuel: Map<RoutePoi, String>, val hz: List<PoiOnRoute>,
        val avg: List<AvgOnRoute>, val stops: List<Double>, val zones: List<Triple<String, Double, String>>,   // name, where, country
    )
    private var rawServices: List<RoutePoi> = emptyList()
    private var servicesFor: Place? = null
    private var servicesBusy = false
    private var servicesRetryAt = 0L
    private var rawHz: List<RoutePoi> = emptyList()
    private var rawAvg: List<AvgDef> = emptyList()
    private var hzWinEnd = -1.0
    private var hzWinRoute: Route? = null
    private var hzBusy = false
    private var hzRetryAt = 0L
    private var hzLastAt = 0L
    // motorway junctions (names and numbers) from OpenStreetMap, found with the hazards, and where they are on the route
    private var rawJcts: List<Jct> = emptyList()
    @Volatile private var jctsOn: Pair<Route, List<JctOnRoute>>? = null
    private var jctsSig = 0
    private var jctsVersion = 0
    private var jctsPlacing = false
    private var placed: Placed? = null
    private var placing = false
    private var placedSig = 0
    private var zonesVersion = 0
    private var avgIn: AvgOnRoute? = null
    private var avgEnterAt = 0L
    private var avgEnterProg = 0.0
    private var avgOverWarned = false
    private val avgAlerted = HashSet<Long>()
    private val hzAlerted = HashSet<String>()
    private var lastBumpAt = 0L
    private val zoneAlerted = HashSet<String>()
    private var barSig = 0
    private var servicesSig = 0
    /** Moving at speed on a route: the map UI simplifies itself. */
    val driving: Boolean get() = route != null && (fix?.speed ?: 0.0) > 8.0
    private var lastSpd = 0.0
    private var lastSpdAt = 0L
    private var limitCandidate = -1
    private var candidateHits = 0
    private var announcedLimit = -1
    private var lastBeep = 0L
    private var breakNext = 7200L
    private var tripPts = ArrayList<LL>()

    val route: Route? get() = routes.getOrNull(active)
    val imperial: Boolean get() = prefs.s(PreferenceKeys.DISTANCE_UNIT, "km") == "mi"
    /** 0 = all sound, 1 = alerts only, 2 = off. Falls back to the old on/off voice setting. */
    val soundMode: Int get() = prefs.i(PreferenceKeys.SOUND_MODE, if (prefs.b(PreferenceKeys.VOICE, true)) 0 else 2).coerceIn(0, 2)
    val voiceOn: Boolean get() = soundMode != 2
    fun cycleSound() {
        if (walking) { toggleWalkSound(); return }
        val n = (soundMode + 1) % 3
        prefs.put(PreferenceKeys.SOUND_MODE, n)
        toast(str(listOf(R.string.toast_050, R.string.toast_051, R.string.toast_052)[n]), 1800)
    }

    /** Miles in the UK and US (and the few others that use them), kilometres everywhere else; before the country is known, miles. */

    val apiKey: String
        get() = prefs.s(PreferenceKeys.API_KEY, "").ifBlank { BuildConfig.TOMTOM_API_KEY }.trim()
    val hasKey: Boolean get() = apiKey.isNotEmpty() && !apiKey.startsWith("YOUR_")

    /** Your Mapbox access token (Settings > Keys & accounts), for the Mapbox map, routes, search and traffic. */
    val mbToken: String get() = MapboxRest.clean(prefs.s(PreferenceKeys.MAPBOX_TOKEN, ""))
    val hasMapbox: Boolean get() = MapboxRest.looksValid(mbToken) && refusedMb != mbToken
    /** A Mapbox token that Mapbox turned away: not used again until it's changed. (Screen state, so the map can say so.) */
    private var refusedMb: String? by mutableStateOf<String?>(null)
    /** The Mapbox token in Settings was turned down by Mapbox (401). */
    val mbKeyRefused: Boolean get() = refusedMb != null && refusedMb == mbToken

    /**
     * Who plans routes and does the place search and live traffic: "tomtom", "mapbox", or "" for none (then the offline pack and
     * OpenStreetMap). Settings choose; "auto" uses Mapbox when it's the map source, otherwise TomTom when there's a key, otherwise
     * Mapbox when there's a token.
     */
    val provider: String get() {
        val chosen = when (prefs.s(PreferenceKeys.DATAPROV, "auto")) {
            "tomtom" -> if (hasKey) "tomtom" else if (hasMapbox) "mapbox" else ""
            "mapbox" -> if (hasMapbox) "mapbox" else if (hasKey) "tomtom" else ""
            // (eco routes are TomTom's: Mapbox has none, so with Eco routes on and a TomTom key, TomTom plans them)
            else -> if (prefs.b(PreferenceKeys.ROUTE_ECO, false) && hasKey) "tomtom"
                else if (prefs.mapSource() == "mapbox" && hasMapbox) "mapbox" else if (hasKey) "tomtom" else if (hasMapbox) "mapbox" else ""
        }
        // the chosen one isn't working right now (its limit reached, the key refused, an outage): the other one meanwhile
        val now = System.currentTimeMillis()
        return when {
            chosen == "tomtom" && now < ttDownUntil && hasMapbox -> "mapbox"
            chosen == "mapbox" && now < mbDownUntil && hasKey -> "tomtom"
            else -> chosen
        }
    }

    /** Until when TomTom / Mapbox is skipped after failing (0 = working). */
    @Volatile private var ttDownUntil = 0L
    @Volatile private var mbDownUntil = 0L
    /** TomTom is being skipped for now (so the speed limits and incidents come from Mapbox too). */
    private fun ttDown() = System.currentTimeMillis() < ttDownUntil && hasMapbox

    /**
     * Runs [call] with the service in use, and if that service itself fails (not a lack of signal, and not "no route there"),
     * runs it again with the other one, which is then used for a while: an hour after a refused key or a reached limit, ten
     * minutes after an outage. Anyone with both a TomTom key and a Mapbox token is never left without routes or search.
     */
    private fun <T> withBackup(call: (String) -> T): T {
        val p = provider
        try { return call(p) }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val m = e.message ?: ""
            val serviceFault = m.startsWith("HTTP") || m.contains("TomTom") || m.contains("Mapbox") || m.contains("too many") || m.contains("limit") || m == "Routing failed"
            val backup = if (p == "tomtom" && hasMapbox) "mapbox" else if (p == "mapbox" && hasKey) "tomtom" else ""
            if (!serviceFault || backup.isEmpty()) throw e
            val wait = if (m.contains("401") || m.contains("403") || m.contains("accept") || m.contains("limit") || m.contains("too many")) 3_600_000L else 600_000L
            if (p == "tomtom") ttDownUntil = System.currentTimeMillis() + wait else mbDownUntil = System.currentTimeMillis() + wait
            val name = if (p == "tomtom") "TomTom" else "Mapbox"
            scope.launch { toast(str(R.string.toast_053, name, if (backup == "tomtom") "TomTom" else "Mapbox"), 4500) }
            return call(backup)
        }
    }
    /** Some online service can plan routes and search. */
    val hasOnline: Boolean get() = provider.isNotEmpty()

    private fun noteMbRefused(e: Exception) {
        val m = e.message ?: ""
        if (m.contains("(401)")) scope.launch { onMbUnauthorised(false) }
    }

    /** Why Mapbox turned your token down, in plain words (from Mapbox's own check of it), for the key box and the feedback report. */
    var mbRefusedWhy by mutableStateOf("")
        private set
    /** The last answer from Mapbox's token check, for the feedback report. */
    private var mbCheckNote = ""
    private var mbChecking: String? = null
    private var mbRetryFor: String? = null
    private var mbRetries = 0

    /**
     * Mapbox answered 401. Before giving up on the token (which turns the map off until it's changed), Mapbox is asked what's
     * wrong with it. A 401 used to be taken as "wrong token" straight away, so one refusal (even one left over from before the
     * token was pasted) shut the map off for the rest of the drive, and people made new tokens that "didn't work" either.
     */
    private fun onMbUnauthorised(fromMap: Boolean) {
        val t = mbToken
        if (t.isBlank() || mbChecking == t || refusedMb == t) return
        // the map was already retried with this (valid) token and the "keeps refusing" note shown: don't ask Mapbox again each time
        if (fromMap && mbRetryFor == t && mbRetries > 3) return
        mbChecking = t
        scope.launch {
            val r = try { withContext(Dispatchers.IO) { MapboxRest.checkToken(t) } } catch (e: Exception) { null }
            mbChecking = null
            if (t != mbToken) return@launch
            val code = r?.first
            val user = r?.second.orEmpty()
            val acct = if (user.isNotBlank()) Lang.t(" (Konto $user)", " (account $user)") else ""
            mbCheckNote = "Mapbox token check: ${code ?: "no answer"}" + (if (user.isNotBlank()) " (account $user)" else "")
            if (code == "TokenValid") {
                // the token is fine: don't lock it out. Load the map again (a few times, further apart each time)
                mbRefusedWhy = ""
                if (!fromMap) return@launch
                if (mbRetryFor != t) { mbRetryFor = t; mbRetries = 0 }
                if (mbRetries < 3) {
                    mbRetries++
                    toast(Lang.t("Mapbox hat die Karte kurz verweigert, aber dein Schlüssel ist in Ordnung$acct. Neuer Versuch …", "Mapbox refused the map for a moment, but your token is fine$acct. Trying again…"), 5000)
                    delay(4000L * mbRetries)
                    if (t == mbToken) try { applyStyle(lastDark) } catch (e: Exception) { }
                } else if (mbRetries == 3) { mbRetries++; toast(Lang.t("Mapbox akzeptiert deinen Schlüssel, verweigert aber weiter die Karte. Melde dich auf account.mapbox.com an und suche dort nach einem Hinweis zu deinem Konto.", "Mapbox accepts your token but keeps refusing the map. Sign in at account.mapbox.com and look for a notice about your account."), 10_000) }
                return@launch
            }
            refusedMb = t
            val why = when (code) {
                "TokenMalformed" -> Lang.t("Mapbox hat den Schlüssel abgelehnt: Das ist kein vollständiger Schlüssel, ein Teil fehlt oder ist vertippt. Kopiere ihn noch einmal mit dem Kopieren-Knopf auf account.mapbox.com › Tokens (abtippen klappt selten).", "Mapbox says that isn't a whole token: part of it is missing or mistyped. Copy it again with the copy button at account.mapbox.com › Tokens (typing it in by hand rarely works).")
                "TokenInvalid" -> Lang.t("Mapbox hat den Schlüssel abgelehnt: Der Schlüssel enthält einen Fehler. Kopiere ihn noch einmal mit dem Kopieren-Knopf auf account.mapbox.com › Tokens, statt ihn abzutippen.", "Mapbox says that token has a mistake in it. Copy it again with the copy button at account.mapbox.com › Tokens rather than typing it.")
                "TokenRevoked" -> Lang.t("Mapbox hat den Schlüssel abgelehnt: Der Schlüssel wurde gelöscht oder ersetzt. Kopiere einen, der auf account.mapbox.com › Tokens noch aufgeführt ist$acct.", "Mapbox says that token has been deleted or replaced. Copy one that's still listed at account.mapbox.com › Tokens$acct.")
                "TokenExpired" -> Lang.t("Mapbox hat den Schlüssel abgelehnt: Das ist ein temporärer Schlüssel und er ist abgelaufen. Kopiere deinen Default public token (beginnt mit pk.) auf account.mapbox.com › Tokens.", "That's a temporary token and it has run out. Copy your Default public token (it starts pk.) at account.mapbox.com › Tokens.")
                else -> Lang.t("Mapbox hat deinen Zugangsschlüssel nicht akzeptiert (401). Kopiere ihn noch einmal mit dem Kopieren-Knopf auf account.mapbox.com › Tokens.", "Mapbox didn't accept your access token (401). Copy it again with the copy button at account.mapbox.com › Tokens.")
            }
            mbRefusedWhy = why
            toast(mbRefusedWhy, 9000)
        }
    }

    /** The number of real turns on a route (not the start, the arrival or going straight on). */
    private fun turnCount(r: Route): Int = r.steps.count { st ->
        val m = st.maneuver.uppercase()
        m != "DEPART" && !m.startsWith("ARRIVE") && m != "STRAIGHT" && m != "FOLLOW" && m != "CONTINUE"
    }

    /**
     * The route to start with: the quickest, unless one with clearly fewer turns (at least two fewer) is at most 90 seconds slower. Such
     * a route keeps to the main roads instead of cutting through back streets. The quicker one is still offered to pick.
     */
    private fun simplerPick(rs: List<Route>): Int {
        if (rs.isEmpty()) return 0
        val f = rs.indices.minByOrNull { rs[it].time } ?: 0
        if (!prefs.b(PreferenceKeys.SIMPLER_ROUTE, true)) return f
        val fastT = rs[f].time
        val fastTurns = turnCount(rs[f])
        return rs.indices
            .filter { rs[it].time <= fastT + 90.0 && turnCount(rs[it]) <= fastTurns - 2 }
            .minWithOrNull(compareBy<Int>({ turnCount(rs[it]) }, { rs[it].time }))
            ?: f
    }

    /** Routes from one service ("mapbox" or "tomtom"). */
    private fun routesFrom(p: String, from: LL, to: LL, alternatives: Boolean, departAtMs: Long, via: List<LL>, headingDeg: Double, quick: Boolean): List<Route> =
        if (p == "mapbox") try { MapboxRest.curbArrival = prefs.b(PreferenceKeys.CURB_ARRIVAL, true); MapboxRest.routes(mbToken, from, to, alternatives, departAtMs, via, headingDeg, quick, leftTraffic) } catch (e: Exception) { noteMbRefused(e); throw e }
        else TomTomRest.routes(apiKey, from, to, alternatives, departAtMs, via, headingDeg, quick)

    /**
     * Routes from whichever service is set up (see [provider]). With both a TomTom key and a Mapbox token (and Settings leaving the
     * choice on Auto), a new route is asked of both at once: the two often pick different roads, and one alone sometimes missed a
     * clearly quicker way. The quicker one comes first; a different route from the other service is kept as an alternative.
     * Only when planning a new route ([askBoth]): not when rerouting, which has to be quick, nor in the faster-route checks while
     * driving (the two services time the same road differently, which would offer "faster" routes that aren't), nor with eco
     * routes (TomTom's only), nor when one of them isn't working.
     */
    /** The way you're driving (degrees), when moving and [from] is where you are; -1 otherwise. Sent with every route request, so
     *  the route starts the way you're going (not with a U-turn behind you) and a check for a faster way compares like with like. */
    private fun driveHeading(from: LL? = null): Double {
        val f = fix ?: return -1.0
        if (f.speed < 2.0 || f.heading < 0) return -1.0
        if (from != null && Geo.dist(from, f.pos) > 80.0) return -1.0
        return f.heading
    }

    private fun netRoutes(from: LL, to: LL, alternatives: Boolean, departAtMs: Long = 0L, via: List<LL> = emptyList(), headingDeg: Double = -1.0, quick: Boolean = false,
                          askBoth: Boolean = false): List<Route> =
        netRoutesRaw(from, to, alternatives, departAtMs, via, headingDeg, quick, askBoth)
            .also { rs -> if (lastRouteProvider == "tomtom" && rs.isNotEmpty()) mapboxLanesFor(rs, from, to, via, headingDeg) }

    private fun netRoutesRaw(from: LL, to: LL, alternatives: Boolean, departAtMs: Long, via: List<LL>, headingDeg: Double, quick: Boolean,
                             askBoth: Boolean): List<Route> {
        val now = System.currentTimeMillis()
        val both = askBoth && !quick && hasKey && hasMapbox && refusedKey != apiKey &&
            prefs.s(PreferenceKeys.DATAPROV, "auto") == "auto" &&
            !prefs.b(PreferenceKeys.ROUTE_ECO, false) && now >= ttDownUntil && now >= mbDownUntil
        if (!both) return withBackup { p -> lastRouteProvider = p; routesFrom(p, from, to, alternatives, departAtMs, via, headingDeg, quick) }
        val mainP = provider
        val otherP = if (mainP == "mapbox") "tomtom" else "mapbox"
        // the other service on a thread of its own, so the two are asked at the same time
        val other = java.util.concurrent.FutureTask<List<Route>>(java.util.concurrent.Callable {
            try { routesFrom(otherP, from, to, false, departAtMs, via, headingDeg, false) } catch (e: Exception) { emptyList<Route>() }
        })
        Thread(other, "routes-$otherP").apply { isDaemon = true }.start()
        val mine = try {
            withBackup { p -> lastRouteProvider = p; routesFrom(p, from, to, alternatives, departAtMs, via, headingDeg, quick) }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) { other.cancel(true); throw e }
            val theirs = try { other.get(20, java.util.concurrent.TimeUnit.SECONDS) } catch (x: Exception) { emptyList() }
            if (theirs.isEmpty()) throw e
            lastRouteProvider = otherP
            return theirs
        }
        // the main service has answered: the other gets a few more seconds at most, so nobody waits long for a route
        val theirs = try { other.get(6, java.util.concurrent.TimeUnit.SECONDS) } catch (x: Exception) { other.cancel(true); emptyList() }
        val best = theirs.firstOrNull() ?: return mine
        val top = mine.firstOrNull() ?: run { lastRouteProvider = otherP; return theirs }
        if (mine.any { sameRoad(it, best) }) return mine
        // clearly quicker (2 minutes, or 5% on longer trips): it goes first. Otherwise it's offered as another choice
        return if (best.time < top.time - max(120.0, top.time * 0.05)) {
            lastRouteProvider = otherP
            (listOf(best) + mine).take(3)
        } else mine.take(2) + best
    }

    /** True when [b] runs along [a] almost all the way (two services' versions of the same route). */
    private fun sameRoad(a: Route, b: Route): Boolean {
        if (a.pts.size < 2 || b.pts.size < 2) return false
        if (abs(a.len - b.len) > 0.06 * max(a.len, b.len)) return false
        val n = 24
        var near = 0
        for (i in 1 until n) {
            val q = b.pts[(i.toLong() * (b.pts.size - 1) / n).toInt()]
            // every few points of the other line is plenty: route lines have points a few metres to a few tens of metres apart
            var d = Double.MAX_VALUE
            var j = 0
            val step = max(1, a.pts.size / 4000)
            while (j < a.pts.size) { val dd = Geo.dist(a.pts[j], q); if (dd < d) d = dd; j += step }
            if (d < 60.0) near++
        }
        return near >= (n - 1) * 0.9
    }

    /**
     * A TomTom route's lane list isn't from the map Mapbox draws the lanes on, so it often doesn't match what's painted. Once the
     * route is out, Mapbox's own route for the same trip is asked for in the background (one request) and its lane list is put onto
     * the route, junction by junction, where Mapbox's route passes the same junction in the same direction. Junctions it doesn't
     * match keep TomTom's lanes. Silent if there's no token, no signal or it's switched off.
     */
    private fun mapboxLanesFor(rs: List<Route>, from: LL, to: LL, via: List<LL>, headingDeg: Double) {
        if (!hasMapbox || !prefs.b(PreferenceKeys.LANES_MAPBOX, true) || !online()) return
        searchScope.launch {
            try {
                MapboxRest.curbArrival = prefs.b(PreferenceKeys.CURB_ARRIVAL, true)
                val mb = MapboxRest.routes(mbToken, from, to, false, 0L, via, headingDeg, true, leftTraffic).firstOrNull() ?: return@launch
                if (mb.lanes.isEmpty()) return@launch
                var matched = 0
                for (r in rs.take(3)) {
                    val out = ArrayList<LaneSec>()
                    for (ls in mb.lanes) {
                        val p = pointAt(mb, ls.end).first
                        val pr = MapRouteGeometry.projectOn(r.pts, p) ?: continue
                        if (Geo.dist(pr.second, p) > 20.0) continue
                        val offTT = r.cum[pr.first] + Geo.dist(r.pts[pr.first], pr.second)
                        // (the nearest step: with two turns close together the first one within 40 m can be the wrong one)
                        val st = r.steps.filter { Math.abs(it.off - offTT) < 40.0 }.minByOrNull { Math.abs(it.off - offTT) } ?: continue
                        // the same way through the junction: a lane Mapbox says to use must suit TomTom's turn here
                        if (ls.lanes.none { it.follow != null && laneHit(it.follow, st.maneuver) }) continue
                        out += LaneSec(max(0.0, offTT - (ls.end - ls.off)), offTT, ls.lanes)
                    }
                    if (out.isEmpty()) continue
                    matched += out.size
                    val keep = r.lanes.filter { t -> out.none { Math.abs(it.end - t.end) < 60.0 } }
                    r.lanes = (keep + out).sortedBy { it.off }
                    laneKey = null
                }
                laneSource = "Mapbox lane list on $matched of ${mb.lanes.size} junctions"
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
            }
        }
    }
    /** Where the lane list came from, for the feedback report. */
    @Volatile var laneSource = ""
        private set
    @Volatile private var lastRouteProvider = ""

    /** The departure time was used for the last route (it isn't with every service or route). */
    private val departApplied: Boolean get() = if (lastRouteProvider == "mapbox") MapboxRest.departApplied else TomTomRest.departApplied

    /**
     * Place search. With a TomTom key it's TomTom's search, the same as the original Horizon Nav (fuzzy, finds house numbers near
     * you as you type), topped up with Mapbox's when TomTom finds little; Settings > Keys & accounts > Search with can pick Mapbox
     * instead. Without a key it's Mapbox's (with OpenStreetMap's alongside), or OpenStreetMap's alone with neither.
     */
    private fun netGeocode(country: String, q: String, near: LL?): List<Place> {
        val with = prefs.s(PreferenceKeys.SEARCHWITH, "auto")
        val tomtomOk = hasKey && refusedKey != apiKey && System.currentTimeMillis() >= ttDownUntil
        if (tomtomOk && (with == "tomtom" || (with == "auto") || !hasMapbox)) {
            val tt = try { TomTomRest.geocode(apiKey, country, q, near) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; null }
            if (tt != null && (tt.size >= 4 || !hasMapbox)) return tt
            if (!hasMapbox) return tt ?: OsmSearch.geocode(country, q, near)
            val mb = try { MapboxRest.geocode(mbToken, country, q, near) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emptyList() }
            val out = ArrayList(tt ?: emptyList())
            for (p in mb) if (out.none { (it.name.equals(p.name, true) && Geo.dist(it.pos, p.pos) < 80) || Geo.dist(it.pos, p.pos) < 15 }) out += p
            return out
        }
        if (hasMapbox) {
            val r = MapboxRest.geocode(mbToken, country, q, near)
            if (r.isEmpty()) MapboxRest.lastError?.let { err -> scope.launch { toast(str(R.string.rest_442, err.take(120)), 6000) } }
            return r
        }
        // no key or token: OpenStreetMap's free search
        return OsmSearch.geocode(country, q, near)
    }

    /**
     * Shops and places near you that match the search: a chain's branches ("dm", "Edeka") or, for a kind of place ("Supermarkt",
     * "Apotheke"), the nearest ones of that kind. The search service's own result list ranks by name over the whole country and
     * often left these out. TomTom or Mapbox when set up, and OpenStreetMap (free) alongside or instead.
     */
    private fun nearbyMatches(q: String, near: LL?, limit: Int = 12): List<Place> {
        if (near == null || q.trim().length < 2 || SearchRank.streety(q)) return emptyList()
        val kind = PoiSearch.kindOf(q)
        val radius = if (kind != null) 10_000 else 25_000
        val tomtomOk = hasKey && refusedKey != apiKey && System.currentTimeMillis() >= ttDownUntil
        val out = ArrayList<Place>()
        // OpenStreetMap by name runs alongside: its shops are well mapped and it knows every branch of a chain
        val osm: java.util.concurrent.CompletableFuture<List<Place>>? = if (kind == null) java.util.concurrent.CompletableFuture.supplyAsync<List<Place>> {
            try { PoiSearch.osmByName(q, near, radius) } catch (e: Exception) { emptyList<Place>() }
        } else null
        if (tomtomOk) try { out += TomTomRest.poiNear(apiKey, q, kind?.tomtom, near, radius) } catch (e: Exception) { android.util.Log.w("NavEngine", "nearby search (TomTom) failed", e) }
        if (out.size < 3 && hasMapbox) try { out += MapboxRest.poiNear(mbToken, kind?.label ?: q, near, radius) } catch (e: Exception) { android.util.Log.w("NavEngine", "nearby search (Mapbox) failed", e) }
        if (kind != null && out.size < 3) try { out += PoiSearch.osmByKind(kind, near, radius) } catch (e: Exception) { }
        if (osm != null) out += try { osm.get(9, java.util.concurrent.TimeUnit.SECONDS) ?: emptyList<Place>() } catch (e: Exception) { emptyList<Place>() }
        return PoiSearch.distinct(out.sortedBy { Geo.dist(near, it.pos) }).take(limit)
    }

    /** Places of a kind nearby from whichever service is set up. */
    private fun netPoiNearby(kind: String, c: LL, radius: Int): List<Nearby> =
        withBackup { p -> if (p == "mapbox") MapboxRest.poiNearby(mbToken, kind, c, radius) else TomTomRest.poiNearby(apiKey, kind, c, radius) }

    // ---- internals ----
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val said = HashSet<String>()
    /** The road point for a fix, when it is within reach of the fix (the GPS error plus a little), otherwise the fix itself. */
    private fun snapNear(p: Fix): LL {
        val s = Overpass.snapToRoad(p.pos, p.heading) ?: map.snapToRoad(p.pos, p.heading) ?: return p.pos
        val reach = if (p.acc.isFinite() && p.acc > 0.0) max(15.0, p.acc * 1.5) else 25.0
        return if (Geo.dist(s, p.pos) <= reach) s else p.pos
    }

    private var lastIdx = 0
    private var offCount = 0
    /** When the car first left the route in the current stretch of being off it (0 when it's on the route). */
    private var offSince = 0L
    /** Off-route strikes before the route is recalculated: 2 (was 3) so a wrong turn is caught sooner. */
    private val REROUTE_STRIKES = 2
    /** Since when (elapsed ms, 0 none) you've been going the wrong way along the route, and how far along it you were then. */
    private var backSince = 0L
    private var backProg = 0.0
    /**
     * A sudden jump onto the road next to the route (GPS reflections between buildings, two roads side by side) is not believed
     * at once: while you keep driving in the route's direction it is held for [SIDE_HOLD_MS], with the arrow kept on the route and
     * no reroute. A real wrong turn goes off at an angle and is noticed straight away; a road that really carries on beside the
     * route (a missed exit) is believed after the wait.
     */
    private val SIDE_HOLD_MS = 8000L
    private var sideSince = 0L
    /** How far along the route you last were for certain, and when: a jump far beyond it is a different road, not progress. */
    private var trustProg = -1.0
    private var trustAt = 0L
    private var lastReroute = 0L
    private var lastDraw = 0L
    private var drawnWhole = false
    private var lastDrawProg = 0.0
    private var simJob: Job? = null
    private var simDist = 0.0
    private var lastRaw: LL? = null
    private var lastGpsTime = 0L
    private var msgToken = 0
    private var lm: LocationManager? = null
    private var weatherAt = 0L
    private var limitAt = 0L
    private var limitPos: LL? = null
    private var camAt = 0L
    private var camPos: LL? = null
    private var cameras: List<LL> = emptyList()
    private val camAlerted = HashSet<LL>()
    private var drove = false
    private var stillSince = 0L
    private var posSavedAt = 0L
    private var tripOn = false
    private var tripT0 = 0L
    private var tripDistance = 0.0
    private var tripMoveSec = 0L
    private var tripTop = 0.0
    private var tripLast: LL? = null
    private var tripDest = ""

    init {
        // an older pack on the unit goes into its own area folder (it's the area that was chosen then)
        try { migratePack() } catch (e: Exception) { }
        // the one-way street warning went off too often on OpenStreetMap's data: off for everyone once (it can be switched back on)
        if (!prefs.b(PreferenceKeys.WRONGWAY_RESET, false)) { prefs.put(PreferenceKeys.WRONGWAY, false); prefs.put(PreferenceKeys.WRONGWAY_RESET, true) }
        // speed limits and the wrong-way warning read the roads from a downloaded area when one covers you (no network needed)
        Overpass.localWays = { p -> OfflineLimits.waysNear(ctx, p) }
        // the phone and car screens share the trip (joined a moment after start, once this engine is fully set up)
        scope.launch { delay(1500); RouteSync.register(this@NavEngine) }
        // a place sent from your phone (Settings > Places & trips): looked for shortly after starting, then every minute
        scope.launch {
            delay(6000)
            while (true) {
                try {
                    val code = prefs.s(PreferenceKeys.LINK_CODE, "")
                    if (!isCar && code.isNotBlank() && prefs.s(PreferenceKeys.LINK_ROLE, "") == "car" && appVisible && route == null && phoneDest == null && online()) {
                        val last = prefs.s(PreferenceKeys.LINK_LAST, "")
                        val got = withContext(Dispatchers.IO) { try { CarLink.latest(code, last) } catch (e: Exception) { null } }
                        if (got != null && got.id != last) { prefs.put(PreferenceKeys.LINK_LAST, got.id); if (route == null) { phoneStops = got.stops; phoneTrack = got.track; phoneDest = got.place } }
                    }
                } catch (e: Exception) { }
                delay(60_000)
            }
        }
        // the performance meter (Settings > Smoothness), once a second while it's on
        scope.launch {
            while (true) {
                delay(1000)
                // (nothing to show while the phone app is in the background)
                if (!isCar && !appVisible) continue
                PerfStats.on = prefs.b(PreferenceKeys.PERFORMANCE_METER, false)
                if (PerfStats.on) perfSample() else if (perfLine.isNotEmpty()) perfLine = ""
                // (the traffic view when stopped shouldn't depend on the GPS still sending positions while you're stationary)
                if (stoppedSince > 0L && !stoppedOverview && following && android.os.SystemClock.elapsedRealtime() - stoppedSince > 30_500L &&
                    prefs.b(PreferenceKeys.STOPPED_OVERVIEW, true)) fix?.let { try { onPosition(it, true) } catch (e: Exception) { } }
            }
        }
        // look for a newer app and newer map data straight after starting (the checks are a few small requests)
        scope.launch {
            if (Updater.enabled) try { withContext(Dispatchers.IO) { Updater.cleanUp(ctx) } } catch (e: Exception) { }
            delay(3_000)
            // (the phone's app does the update checks; the car screen's engine doesn't repeat them)
            if (!isCar) onOpened()
            // electric: the battery level from the car app's widget, if one was chosen
            try { EvWidget.start(ctx, prefs) } catch (e: Exception) { }
        }
        try {
            val o = org.json.JSONObject(prefs.s(PreferenceKeys.RESUME, "{}"))
            val age = System.currentTimeMillis() - o.optLong("b", o.optLong("t"))
            if (o.has("la") && age < 4 * 60_000L) {
                // the app was navigating a moment ago (Android restarted it, e.g. when the screen switched between day and night):
                // carry straight on, stops included, without asking
                val d = Place(o.optString("n"), o.optString("s"), LL(o.getDouble("la"), o.getDouble("lo")))
                val vs = o.optJSONArray("v")
                val stops = if (vs == null) emptyList() else (0 until vs.length()).mapNotNull { i -> try { val q = vs.getJSONObject(i); Place(q.optString("n"), q.optString("s"), LL(q.getDouble("la"), q.getDouble("lo"))) } catch (e: Exception) { null } }
                val es = o.optJSONArray("e")
                val ev = if (es == null) emptyList() else (0 until es.length()).mapNotNull { i -> try {
                    val q = es.getJSONObject(i)
                    val pos = LL(q.getDouble("la"), q.getDouble("lo"))
                    EvStop(Nearby(Place(q.optString("n"), q.optString("s"), pos), 0.0, "", kw = q.optInt("kw", -1).takeIf { it > 0 }, op = q.optString("op").takeIf { it.isNotBlank() }),
                        q.optInt("a"), q.optInt("t"), q.optInt("m"), q.optBoolean("p"))
                } catch (e: Exception) { null } }
                scope.launch {
                    var n = 0
                    while (fix == null && n < 40) { delay(500); n++ }
                    if (route != null) return@launch
                    via = stops
                    // the trip's own charging plan: no new one on top (evAutoDest), and its stops drawn as chargers
                    evPlanned = ev; if (ev.isNotEmpty()) evAutoDest = d.pos
                    toast(str(R.string.rest_437, d.name), 3500)
                    startRoute(d, 0, keepVia = true, startNow = true)
                }
            } else if (o.has("la") && age < 15 * 60_000L)
                // stopped navigating 4 to 15 minutes ago: offer to carry on with the "Resume trip" card
                resume = Place(o.optString("n"), o.optString("s"), LL(o.getDouble("la"), o.getDouble("lo")))
        } catch (e: Exception) { }
        tts = TextToSpeech(ctx) { status ->
            if (status != TextToSpeech.SUCCESS) {
                // no working text-to-speech on this unit: the beeps still sound, but nothing can be said (this used to fail silently)
                scope.launch { toast(ctx.getString(R.string.toast_024), 9000) }
            }
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                val r = tts?.setLanguage(Locale.getDefault())
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val r2 = tts?.setLanguage(Locale.UK)
                    if (r2 == TextToSpeech.LANG_MISSING_DATA || r2 == TextToSpeech.LANG_NOT_SUPPORTED)
                        scope.launch { toast(ctx.getString(R.string.toast_045), 9000) }
                }
                pickBestVoice()
                voiceChan = prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav")
                tts?.setAudioAttributes(speechAttrs)
                tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String?) {}
                    override fun onDone(id: String?) { if (id?.startsWith("nav") == true) uttDone(id) else unduck() }
                    override fun onStop(id: String?, interrupted: Boolean) { if (id?.startsWith("nav") == true) uttDone(id) }
                    @Deprecated("Deprecated in Java") override fun onError(id: String?) { if (id?.startsWith("nav") == true) uttDone(id) else unduck() }
                })
            }
        }
        // holding on a place you've just tapped on the map goes there by its name (a shop, a station...), anywhere else to a dropped pin
        // (while you're driving a route, holding adds the place as a stop on the way instead of replacing where you're going)
        map.onLongPress = { p ->
            val t = tappedPoi
            val place = if (t != null && Geo.dist(t.second, p) < 60.0 && android.os.SystemClock.uptimeMillis() - tappedAt < 120_000L) { tappedPoi = null; Place(t.first, "", t.second) }
                else Place(pinName(), "", p)
            if (route != null && !previewing && dest != null) addStop(place) else startRoute(place)
        }
// a place tapped on the map: a card with its name, "Los" and "Als Stopp" (holding on it still goes there straight away)
        map.onPoiTap = { name, p -> tappedPoi = name to p; tappedAt = android.os.SystemClock.uptimeMillis(); showPick(p, name) }
        map.onUserGesture = { if (following) { following = false }; scheduleRecentre(12_000) }
        map.onMapIdle = { box -> refreshChargerLayer(box) }
        map.onAltTap = { i -> switchToAlt(i) }
        map.onChargerTap = { s -> chargerTapped(s) }
        // the parked-car spot is no longer used (the app lives in the car): drop any old one
        if (prefs.s(PreferenceKeys.PLACE_PARKED, "").isNotBlank()) places.clearParked()
        map.onError = { msg -> onMapError(msg) }
        // with no signal Mapbox's map keeps showing what it has stored; only when it starts missing pieces does the pack's map take over
        map.onTileMissing = {
            val now = android.os.SystemClock.uptimeMillis()
            if (!mapboxGaps && !online()) {
                if (now - gapFirstAt > 20_000L) { gapFirstAt = now; gapCount = 0 }
                if (++gapCount >= 3) { mapboxGaps = true; scope.launch { applyStyle(lastDark) } }
            }
        }
        map.onStyleLoaded = { styleRetry = 0 }
        try {
            val o = org.json.JSONObject(prefs.s(PreferenceKeys.PLANNED, "{}"))
            if (o.has("la") && o.optLong("a") > System.currentTimeMillis())
                planned = PlannedTrip(Place(o.optString("n"), o.optString("s"), LL(o.getDouble("la"), o.getDouble("lo"))), o.getLong("a"), o.getLong("l"))
        } catch (e: Exception) { }
        if (planned != null) watchPlanned()
        // clean air zones are downloaded once and refreshed monthly
        scope.launch {
            delay(15_000)
            try { if (Zones.needsRefresh(ctx) && online()) { withContext(Dispatchers.IO) { Zones.fetch(ctx) }; zonesVersion++ } } catch (e: Exception) { }
        }
    }

    /** Which sound channel the voice uses: "nav" (navigation, the normal one) or "media" (Settings > Sound > Voice channel). Some
     *  head units have the navigation channel turned right down or not wired to the speakers, so the beeps (on the media channel)
     *  sounded but the spoken directions never did. */
    private var voiceChan = "nav"
    private val speechAttrs: android.media.AudioAttributes get() = android.media.AudioAttributes.Builder()
        .setUsage(if (voiceChan == "media") android.media.AudioAttributes.USAGE_MEDIA else android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val audioMgr = ctx.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
    private var focusReq: android.media.AudioFocusRequest? = null

    /** Announcements not yet finished. The music is ducked (lowered, not paused) while there are any. */
    private var spoken = 0

    /** Lowers the music while a spoken instruction plays, so it can be heard. Asks for "may duck" only: the music keeps playing. */
    private fun duck() {
        if (focusReq != null) return
        try {
            val req = android.media.AudioFocusRequest.Builder(android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(speechAttrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { }
                .build()
            focusReq = req
            audioMgr.requestAudioFocus(req)
        } catch (e: Exception) { }
    }

    /** Puts the music back up once every announcement has finished (not after each one, so it doesn't flicker between them). */
    private fun unduck() {
        spoken = (spoken - 1).coerceAtLeast(0)
        if (spoken > 0) return
        try { focusReq?.let { audioMgr.abandonAudioFocusRequest(it) } } catch (e: Exception) { }
        focusReq = null
    }

    /** Puts an alert on screen as a prominent banner. */
    fun raiseAlert(kind: String, text: String, ms: Long = 5000) {
        alert = AlertInfo(kind, text, ms)
        val t = ++alertToken
        scope.launch { delay(ms); if (t == alertToken) alert = null }
    }

    /** Plays a sample alert (chime, banner and voice) so the sound can be checked. */
    fun testAlert() {
        chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
        raiseAlert("camera", Lang.t("Testwarnung: Blitzer voraus", "Test alert: speed camera ahead"))
        speak("Blitzer voraus", true)
    }

    fun shutdown() {
        RouteSync.unregister(this)
        stopLocation(); simJob?.cancel(); tts?.shutdown()
        // everything this engine had running (its timers, lookups and the map glide) stops with it: the car screen makes a new
        // engine each time the phone is plugged in, and the old one must not keep going in the background
        try { scope.coroutineContext[Job]?.cancel() } catch (e: Exception) { }
        try { toneGen?.release() } catch (e: Exception) { }; toneGen = null
    }

    fun str(id: Int, vararg a: Any): String = ctx.getString(id, *a)

    fun toast(msg: String, ms: Long = 4000) {
        message = msg
        val token = ++msgToken
        scope.launch { delay(ms); if (token == msgToken) message = null }
    }

    // ---- style ----
    fun styleUrl(dark: Boolean): String {
        val traffic = prefs.b(PreferenceKeys.TRAFFIC, true)
        return if (hasKey && prefs.mapSource() == "tomtom" && refusedKey != apiKey) TomTomRest.styleUrl(apiKey, dark, traffic)
        else OsmStyle.json(dark)
    }


    /**
     * ISO country the place search is limited to; blank means anywhere. Until you choose in Settings ("auto"), it's the country
     * you're in, worked out from your GPS position, so people outside the UK can find their own addresses straight away.
     */
    /**
     * Left-hand drive (driver on the left, as in the US and most of Europe): the screen is mirrored, with the cards on the right
     * and the round buttons on the left. Until it's chosen in Settings, it follows the country you're in.
     */
    val mirrored: Boolean get() = when (prefs.s(PreferenceKeys.DRIVE, "")) {
        "lhd" -> true
        "rhd" -> false
        else -> detectedCountry.isNotBlank() && detectedCountry.uppercase() !in KEEP_LEFT
    }

    /** The speed limit sign to draw: the white "SPEED LIMIT" board in the US, "MAXIMUM" in Canada, the red roundel elsewhere. */
    val limitSignStyle: String get() = when (detectedCountry.uppercase()) { "US", "PR", "GU", "VI", "AS", "MP" -> "us"; "CA" -> "ca"; else -> "round" }

    fun countryCode(): String { val v = prefs.s(PreferenceKeys.COUNTRY, "auto"); return if (v == "auto") detectedCountry else v }

    /** The country you're in (from the GPS), remembered between drives; blank until it's known. */
    var detectedCountry by mutableStateOf(prefs.s(PreferenceKeys.COUNTRYDETECTED, ""))
        private set
    // road numbers on routes are written the way this country signs them ("A 4" in Germany, never the E-number)
    init { RoadRefs.country = detectedCountry.uppercase() }

    /** The car's own speed (Android Auto), when it sends one: shown on the speedometer instead of the GPS speed. */
    var carSpeedMs by mutableStateOf<Double?>(null)
        private set
    init { CarData.onSpeed = { v -> carSpeedMs = v } }

    /** The speed to show: the car's own while it's coming in, else the GPS speed. */
    val shownSpeed: Double get() {
        val c = carSpeedMs
        return if (c != null && CarData.fresh(3000L) != null) c else fix?.speed ?: 0.0
    }

    /**
     * Traffic drives on the left here (UK, Ireland…): roundabouts go clockwise and U-turn lanes are on the right. From the
     * country you're in; only when that isn't known yet from which side the car is driven from.
     */
    val leftTraffic: Boolean get() = if (detectedCountry.isNotBlank()) detectedCountry.uppercase() in KEEP_LEFT else !mirrored
    private var countryChecked = false
    private var countryTryAt = 0L

    private fun detectCountry(lat: Double, lon: Double) {
        val now = android.os.SystemClock.elapsedRealtime()
        // once per app start; if it couldn't tell (no signal), try again a couple of minutes later
        if (countryChecked || (countryTryAt != 0L && now - countryTryAt < 120_000L)) return
        countryChecked = true; countryTryAt = now
        scope.launch {
            val cc = withContext(Dispatchers.IO) {
                val g = try {
                    @Suppress("DEPRECATION")
                    android.location.Geocoder(ctx, Locale.UK).getFromLocation(lat, lon, 1)?.firstOrNull()?.countryCode
                } catch (e: Exception) { null }
                // no geocoder on this unit (or no signal): a rough outline of Great Britain and Northern Ireland
                g ?: if (lat in 49.85..60.9 && lon in -8.2..1.8 && !(lon < -5.45 && lat < 54.05) && !(lat < 51.0 && lon > 1.0)) "GB" else null
            }
            if (cc == null) { countryChecked = false; return@launch }
            val up = cc.uppercase()
            val changed = up != detectedCountry
            if (changed) { detectedCountry = up; prefs.put(PreferenceKeys.COUNTRYDETECTED, up) }
            RoadRefs.country = up
            // the low emission zones are the country's own (Germany's Umweltzonen, Britain's clean air zones): fetch them for a new country
            if (changed) try { if (Zones.needsRefresh(ctx) && online()) { withContext(Dispatchers.IO) { Zones.fetch(ctx) }; zonesVersion++ } } catch (e: Exception) { }
        }
    }

    private var styleJob: kotlinx.coroutines.Job? = null

    private var lastDark = true

    // ---- offline pack download (runs here so it carries on when you leave the settings page) ----
    var packBusy by mutableStateOf(false)
    var packStatus by mutableStateOf("")
    var packRev by mutableStateOf(0)

    /** The area being downloaded (shown on its row in Settings), or null. */
    var packDownloading by mutableStateOf<String?>(null)

    fun downloadPack(url: String, id: String, extrasUrl: String = url, onDone: () -> Unit = {}) {
        if (packBusy || url.isBlank()) return
        packBusy = true; packStatus = "Starting…"; packDownloading = id
        scope.launch {
            // a foreground service keeps the download alive while the app is in the background; a dropped connection is
            // retried (the files already downloaded are kept, so the retry carries on where it stopped)
            OfflineDownloadService.show(ctx, Lang.t("Karte wird geladen…", "Downloading map…"))
            try {
                var attempt = 0
                while (true) {
                    try {
                        withContext(Dispatchers.IO) { OfflineRouter.download(ctx, url, id, extrasUrl) { s -> packStatus = s; OfflineDownloadService.show(ctx, s) } }
                        break
                    } catch (e: Exception) {
                        attempt++
                        if (attempt >= 5) throw e
                        packStatus = Lang.t("Verbindung abgebrochen, neuer Versuch ($attempt von 4) …", "Connection dropped, retrying ($attempt of 4) …")
                        delay(10_000L)
                    }
                }
                onDone()
                // the files are as new as the moment they finished: a long download would otherwise look older than the server's
                // copy (made earlier) and the same pack would be downloaded again straight away
                try { OfflineRouter.packDir(ctx, id).listFiles()?.forEach { it.setLastModified(System.currentTimeMillis()) } } catch (e: Exception) { }
                packStatus = Lang.t("Paket bereit", "Pack ready")
                toast(ctx.getString(R.string.toast_028), 3000)
            } catch (e: Throwable) {
                packStatus = Lang.t("Download fehlgeschlagen: ${e.message ?: e.javaClass.simpleName}. Tippe erneut auf Herunterladen, um fortzufahren.", "Download failed: ${e.message ?: e.javaClass.simpleName}. Tap Download again to carry on.")
            } finally { packBusy = false; packDownloading = null; packRev++; restyle(); OfflineDownloadService.stop(ctx) }
        }
    }

    /** Removes one downloaded area. */
    fun deletePack(id: String) {
        if (packBusy && packDownloading == id) return
        scope.launch {
            withContext(Dispatchers.IO) { OfflineRouter.deleteArea(ctx, id) }
            packStatus = ""; packRev++; restyle()
        }
    }

    // ---- several areas: the one you're in is used automatically ----
    private var areaCheckAt = 0L
    private var areaPending: String? = null

    /**
     * About once a minute: if another downloaded area fits where you are better (the smallest one that covers you), switch to it
     * once it's been the better fit on two checks in a row (so a border, where boxes overlap, doesn't flip it back and forth).
     */
    private fun checkArea(p: LL) {
        val now = System.currentTimeMillis()
        if (now - areaCheckAt < 60_000L || packBusy) return
        areaCheckAt = now
        scope.launch {
            val (cur, best) = withContext(Dispatchers.IO) {
                val ids = OfflineRouter.installedIds(ctx)
                if (ids.size < 2) return@withContext null to null
                OfflineRouter.activeId(ctx) to OfflineRouter.areaFor(ctx, p)
            }
            if (best == null || best == cur) { areaPending = null; return@launch }
            if (areaPending != best) { areaPending = best; areaCheckAt = now - 45_000L; return@launch }   // look again in ~15 s
            areaPending = null
            withContext(Dispatchers.IO) { OfflineRouter.setActive(ctx, best) }
            packRev++
            if (useOfflineMap()) restyle()
        }
    }
    fun restyle() = applyStyle(lastDark)

    /** True when the map should be drawn from the downloaded pack: always, or (in auto) when there is no signal. */
    private var styleRetry = 0

    /** The map style failed to load (usually a signal blip): try again a few times, then fall back to the downloaded map if there is one. */
    /** A key TomTom's map server turned away (it may still work for search and routes): the OpenStreetMap map is used instead. */
    @Volatile private var refusedKey: String? = null

    private fun onMapError(msg: String) {
        val m = msg.lowercase()
        // Mapbox turned the token away: say so (the map can't show without a working token)
        if (m.contains("401") || m.contains("403") || m.contains("unauthor") || m.contains("forbidden") || m.contains("access token")) {
            if (m.contains("403") || m.contains("forbidden"))
                toast(Lang.t("Mapbox lässt diesen Schlüssel die Karte nicht anzeigen (403). Bearbeite ihn auf account.mapbox.com › Tokens: Hake alle öffentlichen Berechtigungen an und entferne URL-Beschränkungen.", "Mapbox won't let this token show the map (403). At account.mapbox.com › Tokens, edit it: tick all the public scopes and remove any URL restrictions."), 9000)
            // 401: Mapbox is asked what's wrong with the token first (it says so itself if it's mistyped, deleted or out of date)
            else onMbUnauthorised(true)
            return
        }
        val network = m.contains("resolve") || m.contains("host") || m.contains("timeout") || m.contains("timed out") || m.contains("connect") || m.contains("network") || m.contains("unreachable")
        if (!network) { toast(str(R.string.rest_440, msg), 5000); return }
        styleRetry++
        if (styleRetry == 1) toast(ctx.getString(R.string.toast_020), 3000)
        // still no signal: the downloaded pack's own map meanwhile
        if (styleRetry == 2 && OfflineMap.installed(ctx) && OfflineMap.hasFonts(ctx) && prefs.s(PreferenceKeys.OFF_MAP, "auto") != "never") {
            try { map.setStyleUrl(OfflineServer.styleJson(ctx, lastDark)) } catch (e: Exception) { }
        }
        if (styleRetry > 8) return
        val wait = listOf(2_000L, 4_000L, 8_000L, 15_000L, 30_000L, 30_000L, 60_000L, 60_000L)[(styleRetry - 1).coerceIn(0, 7)]
        scope.launch { delay(wait); applyStyle(lastDark) }
    }

    /**
     * The Mapbox edition draws Mapbox's map (which keeps recently seen areas for when signal drops); the downloaded packs' own map
     * isn't drawn yet (their offline routing, places, limits, cameras and lights all still work).
     */
    /** The last place tapped on Mapbox's map (name and spot), and when. */
    private var tappedPoi: Pair<String, LL>? = null
    private var tappedAt = 0L

    /** With no signal, Mapbox's map has started missing pieces (an area it hasn't stored): the pack's map is used until signal returns. */
    @Volatile private var mapboxGaps = false
    private var gapFirstAt = 0L
    private var gapCount = 0

    fun useOfflineMap(): Boolean {
        if (!OfflineMap.installed(ctx) || !OfflineMap.hasFonts(ctx)) return false
        return when (prefs.s(PreferenceKeys.OFF_MAP, "auto")) {
            "always" -> true
            "never" -> false
            // no signal: Mapbox's map carries on with what it has stored (areas seen recently, and saved Mapbox areas); the pack's
            // own map only takes over once Mapbox's starts missing pieces, and not where a saved Mapbox area covers you
            else -> !online() && mapboxGaps && !MapboxOffline.covers(ctx, fix?.pos ?: map.center())
        }
    }

    private var netWatching = false
    /** When the connection last came or went (elapsed ms): for a short while after it, a position jump or a small drift doesn't count as leaving the route. */
    @Volatile private var netChangeAt = -100_000L
    /** Switches between the online and downloaded map by itself when the connection comes or goes. */
    fun watchNetwork() {
        if (netWatching) return
        netWatching = true
        try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            cm.registerDefaultNetworkCallback(object : android.net.ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: android.net.Network) { netChangeAt = android.os.SystemClock.elapsedRealtime(); mapboxGaps = false; scope.launch { delay(2000); applyStyle(lastDark); checkForUpdate(); checkDataUpdates() } }
                override fun onLost(network: android.net.Network) { netChangeAt = android.os.SystemClock.elapsedRealtime(); scope.launch { delay(800); applyStyle(lastDark) } }
            })
        } catch (e: Exception) { }
    }

    private var noTokenToldAt = 0L
    private var lightJob: kotlinx.coroutines.Job? = null

    /**
     * Standard's light for now: dawn and dusk around sunrise and sunset, otherwise day or night (the app's own day / night choice).
     * "Day" or "Night" picked in Settings is kept as it is.
     */
    private fun lightPreset(dark: Boolean): String {
        val theme = prefs.s(PreferenceKeys.THEME, "auto")
        if (theme == "day") return "day"
        if (theme == "night") return "night"
        if (theme == "times") return if (dark) "night" else "day"
        val pos = fix?.pos ?: prefs.s(PreferenceKeys.LASTLAT, "").toDoubleOrNull()?.let { la -> prefs.s(PreferenceKeys.LASTLON, "").toDoubleOrNull()?.let { lo -> LL(la, lo) } }
        val ph = pos?.let { Sun.phase(it.lat, it.lon, System.currentTimeMillis()) } ?: ""
        return when {
            ph == "dawn" || ph == "dusk" -> ph
            dark -> "night"
            else -> "day"
        }
    }

    /**
     * The map. It's always drawn by Mapbox's map engine (so it always needs the Mapbox token), showing, as chosen in Settings > Map:
     * Mapbox's own map (Standard, its Lite / Lightest versions, or Standard Satellite), TomTom's map in the Horizon colours,
     * or the OpenStreetMap map. With no signal the downloaded pack's own map is shown (unless a saved Mapbox area covers where
     * you are). Day and night on Mapbox's map only change its light (a smooth fade, no reload).
     */
    fun applyStyle(dark: Boolean) {
        lastDark = dark
        map.nightStyle = dark
        if (map.onRestyle == null) map.onRestyle = { restyle() }
        watchNetwork()
        styleJob?.cancel()
        map.setTraffic(prefs.b(PreferenceKeys.TRAFFIC, true))
        map.setLightPreset(lightPreset(dark))
        // the light follows the sun (dawn, day, dusk, night) without waiting for the day / night switch
        if (lightJob == null) lightJob = scope.launch { while (true) { delay(60_000L); try { map.setLightPreset(lightPreset(lastDark)) } catch (e: Throwable) { } } }
        if (!hasMapbox) {
            map.setToken("")
            val now = System.currentTimeMillis()
            if (refusedMb != mbToken && now - noTokenToldAt > 60_000L) {
                noTokenToldAt = now
                toast(ctx.getString(R.string.toast_001), 8000)
            }
            return
        }
        map.setToken(mbToken)
        map.keepBaseLook = false
        map.recolourBase = false
        // no signal (or "Downloaded" chosen): the pack's own map, served from the phone. Satellite is Mapbox's photos, so it
        // wins while there's a connection (with "Downloaded" chosen, switching satellite on used to do nothing at all)
        if (useOfflineMap() && !(map.isSatellite && online())) {
            if (map.isSatellite) toast(ctx.getString(R.string.toast_039), 5000)
            try { map.setStyleUrl(OfflineServer.styleJson(ctx, dark)); return } catch (e: Exception) { android.util.Log.w("NavEngine", "offline map", e) }
        }
        OfflineServer.stop()
        val src = prefs.mapSource()
        when {
            map.isSatellite || src == "mapbox" -> map.setStyleUrl(map.baseStyle())
            src == "tomtom" -> {
                if (hasKey && refusedKey != apiKey) {
                    // TomTom's map in the Horizon colours, as in Horizon Nav
                    map.recolourBase = true
                    map.setStyleUrl(TomTomRest.styleUrl(apiKey, dark, prefs.b(PreferenceKeys.TRAFFIC, true)))
                } else {
                    toast(str(if (hasKey) R.string.toast_055 else R.string.toast_056), 5000)
                    map.setStyleUrl(map.baseStyle())
                }
            }
            else -> map.setStyleUrl(OsmStyle.json(dark))
        }
    }

    // ---- location ----
    @SuppressLint("MissingPermission")
    fun startLocation() {
        if (lm != null) return
        val m = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm = m
        try {
            if (m.allProviders.contains(LocationManager.GPS_PROVIDER))
                m.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, this, Looper.getMainLooper())
            if (m.allProviders.contains(LocationManager.NETWORK_PROVIDER))
                m.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 0f, this, Looper.getMainLooper())
            (m.getLastKnownLocation(LocationManager.GPS_PROVIDER) ?: m.getLastKnownLocation(LocationManager.NETWORK_PROVIDER))
                ?.let { handleLocation(it, true) }
        } catch (e: SecurityException) {
            lm = null
            toast(ctx.getString(R.string.toast_010))
        } catch (e: Exception) {
            lm = null
            android.util.Log.w("NavEngine", "startLocation failed", e)
        }
    }

    fun stopLocation() {
        // the battery learning state is saved every minute while driving: the last stretch is saved here as well (the phone app going
        // into the background, Android Auto unplugged), so it isn't lost when the app is closed after that
        if (evLoaded) try { evSave() } catch (_: Exception) {}
        try { lm?.removeUpdates(this) } catch (_: Exception) {}
        lm = null
    }

    override fun onLocationChanged(l: Location) = handleLocation(l, false)
    // Android 10 and older have no built-in versions of these: without them the app crashed as soon as GPS was switched off or on
    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    @Deprecated("Needed on Android 10 and older")
    @Suppress("DEPRECATION")
    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}

    private var startSavedAt = 0L

    private fun handleLocation(l: Location, first: Boolean) {
        try { handleLocationInner(l, first) } catch (e: Exception) { android.util.Log.w("NavEngine", "location handling failed", e) }
    }

    private fun handleLocationInner(l: Location, first: Boolean) {
        run {
            val t = System.currentTimeMillis()
            // kept for "where you parked" when the car's Bluetooth goes (CarBluetooth)
            if (!l.hasAccuracy() || l.accuracy < 60f) CarBluetooth.noteFix(LL(l.latitude, l.longitude), if (l.hasSpeed()) l.speed else 0f)
            // where you are exactly (and which way you're facing), for the map to open right there next time
            if (t - startSavedAt > 15_000 && (!l.hasAccuracy() || l.accuracy < 60f)) {
                startSavedAt = t
                prefs.put(PreferenceKeys.STARTPOS, "%.5f,%.5f,%.0f".format(Locale.US, l.latitude, l.longitude, if (l.hasBearing()) l.bearing.toDouble() else -1.0))
            }
            if (t - posSavedAt > 600_000) {
                posSavedAt = t
                prefs.put(PreferenceKeys.LASTLAT, Math.round(l.latitude * 10) / 10.0)
                prefs.put(PreferenceKeys.LASTLON, Math.round(l.longitude * 10) / 10.0)
            }
        }
        if (simMode != 0) return
        detectCountry(l.latitude, l.longitude)
        val now = System.currentTimeMillis()
        if (l.provider == LocationManager.GPS_PROVIDER) lastGpsTime = now
        else if (now - lastGpsTime < 10000) return
        // with the GPS gone (a tunnel) a rough mobile-network position would throw the route off: keep estimating along the route instead
        else if (route != null && !previewing && glideOnRoute && now - lastGpsTime < 120_000 && (!l.hasAccuracy() || l.accuracy > 30f)) return
        val cur = LL(l.latitude, l.longitude)
        var h = fix?.heading ?: 0.0
        if (l.hasBearing() && l.hasSpeed() && l.speed > 1.0f) h = l.bearing.toDouble()
        else lastRaw?.let { if (Geo.dist(it, cur) > 6) h = Geo.bearing(it, cur) }
        val prev = lastRaw
        if (prev == null || Geo.dist(prev, cur) > TrackMath.ANCHOR_M) lastRaw = cur
        val speed = if (l.hasSpeed()) l.speed.toDouble() else 0.0
        // on foot (walking to the car): no trip distance, no battery use or learning
        // parked, the GPS wanders (worst in a garage or between buildings) and sometimes reports a little speed: with a poor fix or
        // a crawl under 1.5 m/s nothing counts as driven, or a car that stood still lost battery and range
        val goodFix = !l.hasAccuracy() || l.accuracy <= 25f
        if (prev != null && speed > 0.8 && !walking && !(goodFix && speed >= 1.5)) { /* drift, not driving */ }
        else if (prev != null && speed > 0.8 && !walking) {
            val d = Geo.dist(prev, cur)
            // counted only when the anchor (lastRaw) moves on, that is past 6 m: a step of 3 to 6 m left the anchor where it was, and
            // the next step was measured from it again, so slow driving was counted up to half as much again
            if (TrackMath.countedStep(d)) { tripDist += d; tripMoving += 1.0; try { evTrack(speed, d) } catch (e: Exception) { } }
            // a gap in the GPS track (a tunnel, a lost signal): the distance along the route between the two fixes
            else if (d > TrackMath.MAX_STEP_M) try { if (!(d < 5000.0 && evGapOnRoute(speed, prev, cur, d))) evGapBad = true } catch (e: Exception) { evGapBad = true }
        } else if (speed <= 0.8) try { evIdle() } catch (e: Exception) { }
        if (speed > tripMax) tripMax = speed
        val firstFix = fix == null
        onPosition(Fix(cur, h, speed, if (l.hasAltitude()) l.altitude else 0.0, if (l.hasAccuracy()) l.accuracy.toDouble() else 0.0), firstFix || first)
        if (firstFix) {
            map.cameraTo(cur, null, if (tilt3d) (chosenTilt() ?: 55.0) else 0.0, 15.5, 0, 0.0)
        }
        // current weather is only used for the rain effect on the map, so it is only fetched when that is switched on
        if (prefs.b(PreferenceKeys.RAIN_EFFECTS, false) && now - weatherAt > 20 * 60 * 1000L) {
            weatherAt = now
            scope.launch { weather = withContext(Dispatchers.IO) { Weather.fetch(cur) } ?: weather }
        }
    }

    // ---- extras: journey times, rain at destination, stops, arriving home ----
    private fun loadRain(to: Place, routeSecs: Double) {
        destRain = null
        scope.launch {
            val f = withContext(Dispatchers.IO) { Weather.forecast(to.pos) } ?: return@launch
            val idx = Math.round(routeSecs / 3600.0).toInt().coerceIn(0, f.hours.size - 1)
            val h = f.hours.getOrNull(idx) ?: return@launch
            val wet = h.code in 51..67 || h.code in 80..82 || h.code in 95..99
            val snow = h.code in 71..77 || h.code in 85..86
            if (h.rain >= 50 || wet || snow) destRain = (if (snow) Lang.t("Schnee", "Snow") else Lang.t("Regen", "Rain")) + Lang.t(" bei Ankunft wahrscheinlich (${h.rain} %)", " likely at arrival (${h.rain}%)")
        }
    }

    /** Forecast at points along the route, timed for when you will be there. */
    private fun loadRouteWeather(r: Route) {
        routeWx = emptyList()
        if (!prefs.b(PreferenceKeys.ROUTEWX, true) || r.cum.isEmpty()) return
        val total = r.cum.last()
        if (total < 10_000) return
        val fracs = if (total < 40_000) listOf(0.5, 1.0) else listOf(0.25, 0.5, 0.75, 1.0)
        scope.launch {
            val out = ArrayList<RouteWx>()
            for (fr in fracs) {
                val secs = r.time * fr
                val pos = pointAt(r, total * fr).first
                val f = withContext(Dispatchers.IO) { Weather.forecast(pos) } ?: continue
                val h = f.hours.getOrNull(Math.round(secs / 3600.0).toInt().coerceIn(0, f.hours.size - 1)) ?: continue
                out += RouteWx(Math.round(secs / 60.0).toInt(), h.code, h.temp, h.rain, total * fr, h.windKmh, h.windFrom)
            }
            routeWx = out
        }
    }

    // one tone generator, kept and reused: making a new one for every beep leaked audio resources and caused hiccups over time
    private var toneGen: android.media.ToneGenerator? = null
    /**
     * The short beep (speeding, camera ahead, turn prompts). It plays on the navigation channel, as the spoken directions do, so it is
     * heard over the radio or the car's own music too (a ToneGenerator on the music stream was drowned out or silent in the car).
     */
    private fun chime(tone: Int = android.media.ToneGenerator.TONE_PROP_ACK) {
        if (soundMode == 2 || quiet()) return
        try {
            val rate = 22050
            val n = rate * 250 / 1000
            // a sine with a short fade in and out, so it doesn't click
            // two pitches: the one for a warning (speeding, a camera) is lower, the prompts higher, so they can be told apart by ear
            val hz = when (tone) {
                android.media.ToneGenerator.TONE_PROP_BEEP2 -> 660.0
                android.media.ToneGenerator.TONE_PROP_PROMPT -> 1100.0
                else -> 880.0
            }
            val buf = ShortArray(n) { i ->
                val fade = minOf(1.0, minOf(i, n - i) / (rate * 0.01))
                (Math.sin(2 * Math.PI * hz * i / rate) * fade * 0.5 * Short.MAX_VALUE).toInt().toShort()
            }
            val track = android.media.AudioTrack.Builder()
                .setAudioAttributes(android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(android.media.AudioFormat.Builder().setSampleRate(rate)
                    .setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setChannelMask(android.media.AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(n * 2).setTransferMode(android.media.AudioTrack.MODE_STATIC).build()
            track.write(buf, 0, n)
            track.play()
            scope.launch { delay(400); try { track.release() } catch (e: Exception) { } }
        } catch (e: Exception) { }
    }

    private fun checkLimitChange(newMs: Double, estimated: Boolean) {
        if (!prefs.b(PreferenceKeys.LIMITCHANGE, false) || estimated) return
        val k = if (imperial) 2.23694 else 3.6
        val n = (Math.round(newMs * k / 5.0) * 5).toInt()
        if (announcedLimit < 0) { announcedLimit = n; return }
        if (n == announcedLimit) { candidateHits = 0; limitCandidate = -1; return }
        if (n == limitCandidate) candidateHits++ else { limitCandidate = n; candidateHits = 1 }
        if (candidateHits >= 2 && (fix?.speed ?: 0.0) > 3.0) {
            announcedLimit = n; candidateHits = 0; limitCandidate = -1
            chime()
            raiseAlert("limit", "Tempolimit $n")
            speak("Tempolimit $n", true)
            limitPulse = true
            scope.launch { delay(3500); limitPulse = false }
        }
    }

    /** Adds the planned charging stops to the trip, in route order, and routes once. */
    fun addStops(list: List<EvStop>, auto: Boolean = false, startNow: Boolean = route != null && !previewing) {
        val d = dest ?: return
        val all = via + list.map { it.n.place }
        // in route order, so a stop before a later one on the route comes first
        val sorted = route?.let { r -> all.sortedBy { RouteExtras.project(r, it.pos, 5000.0)?.first ?: Double.MAX_VALUE } } ?: all
        stopMode = false
        clearNearby()
        evPlanWarn = evWarning
        evStops = emptyList()
        toast(if (auto) Lang.t("${list.size} Ladestopp(s) automatisch eingeplant", "${list.size} charging stop(s) added automatically")
            else Lang.t("${list.size} Ladestopp(s) eingeplant", "${list.size} charging stop(s) added"), 2500)
        // while driving the route is changed on the go, not started afresh
        if (route != null && !previewing && startNow) { changeViaLive(sorted, evPlanned + list); return }
        via = sorted
        evPlanned = evPlanned + list
        startRoute(d, departMin, true, startNow = startNow)
    }

    /**
     * Stops changed while driving (a charging stop planned, one of yours added or taken off): the way through them is worked out
     * from where you are and swapped in, with the guidance carrying on. No route options in between, no new overview, no
     * weather and offline-map announcements again, no restart on the other screen. When no way is found the trip stays as it was.
     * [sync] false: the other screen did it first and this one only follows. [after] runs once the new route is in.
     */
    private fun changeViaLive(newVia: List<Place>, ev: List<EvStop>, sync: Boolean = true, after: (() -> Unit)? = null) {
        val d = dest ?: return
        val f = fix
        if (f == null || route == null || previewing) { via = newVia; evPlanned = ev; startRoute(d, departMin, true, startNow = route != null && !previewing); return }
        if (rerouting) { via = newVia; evPlanned = ev; startRoute(d, departMin, true, startNow = true); return }
        rerouting = true
        scope.launch {
            try {
                syncRoutePrefs(f.pos, d.pos); faster = null
                val vp = newVia.map { it.pos }
                val hd = if (f.speed > 2.0) f.heading else -1.0
                val rs = withContext(Dispatchers.IO) {
                    val haveOffline = OfflineRouter.installed(ctx)
                    if (haveOffline && (!hasOnline || !online())) OfflineRouter.routes(ctx, f.pos, d.pos, vp, hd)
                    else try { netRoutes(f.pos, d.pos, false, 0L, vp, hd, true) }
                    catch (e: Exception) { if (haveOffline && NavigationFallbackPolicy.shouldUseOffline(e)) OfflineRouter.routes(ctx, f.pos, d.pos, vp, hd) else throw e }
                }
                // ended, or another trip chosen, meanwhile
                if (dest !== d || previewing || route == null) return@launch
                if (rs.isEmpty()) { toast(Lang.t("Kein Weg über die Stopps gefunden, die Route bleibt", "No way through the stops found, the route stays"), 4000); return@launch }
                via = newVia; evPlanned = ev
                val was = route
                routes = rs; active = keepChosen(rs, was)
                said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
                clearLiveAlts(); drawRoutes(); updateRouteZones(); pushVia()
                try { loadRouteWeather(rs[active]) } catch (e: Exception) { android.util.Log.w("NavEngine", "route weather failed", e) }
                if (prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) lastFasterCheck = System.currentTimeMillis() - 90_000L
                if (sync) try { RouteSync.stopsChanged(this@NavEngine, d, newVia) } catch (e: Exception) { }
                after?.invoke()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                toast(Lang.t("Route konnte nicht geändert werden, sie bleibt", "Couldn't change the route, it stays"), 4000)
            } finally { rerouting = false; lastReroute = System.currentTimeMillis() }
        }
    }

    /** The other screen changed the stops of this trip: the same here, on the go when the trip is under way. */
    fun followStops(to: Place, stops: List<Place>, ev: List<EvStop>) {
        val d = dest
        if (d != null && Geo.dist(d.pos, to.pos) < 30.0 && route != null && !previewing) changeViaLive(stops, ev, sync = false)
        else followRoute(to, stops, ev, RouteSync.currentRoute)
    }

    fun addStop(p: Place) {
        val d = dest ?: return
        stopMode = false
        clearNearby()
        toast(str(R.string.rest_433, p.name), 2500)
        if (route != null && !previewing) { changeViaLive(via + p, evPlanned); return }
        via = via + p
        startRoute(d, departMin, true, startNow = route != null && !previewing)
    }

    /** Route settings (avoid motorways, tolls...) changed: work the route out again with them, if there is one. */
    fun rerouteForSettings() {
        val d = dest ?: return
        startRoute(d, departMin, true, startNow = route != null && !previewing)
    }

    /** Replaces the stops on the way (in order) and works the route out again. */
    fun setStops(stops: List<Place>) {
        val d = dest ?: return
        // a charging stop taken off in the stops editor is no longer part of the plan
        val keepEv = evPlanned.filter { e -> stops.any { Geo.dist(it.pos, e.n.place.pos) < 30.0 } }
        if (route != null && !previewing) { changeViaLive(stops, keepEv); return }
        evPlanned = keepEv
        via = stops
        startRoute(d, departMin, true, startNow = route != null && !previewing)
    }

    /** Which clean air / low emission zones the chosen route goes into, for the route options. */
    // ---- vignettes ----
    /** The vignette countries on the route (shown under the route choice, and said before the border). */
    var routeVignette by mutableStateOf<List<Vignette.Need>>(emptyList()); private set
    private var vignetteRoute: Route? = null
    private val vignetteSaid = HashSet<String>()

    /**
     * Settings > Routes "Vignettenpflichtige Straßen meiden": when the best route uses a vignette road in a country you only pass
     * through, it's asked again without toll roads; the new one is kept if it needs fewer vignettes.
     */
    private suspend fun avoidVignette(rs: List<Route>, from: LL, to: LL, departMs: Long): List<Route> {
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
    private fun checkVignette(r: Route, progress: Double) {
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

    private fun updateRouteZones() {
        val r = route ?: run { routeZones = emptyList(); routeVignette = emptyList(); vignetteRoute = null; return }
        if (vignetteRoute !== r) {
            scope.launch {
                val vs = withContext(Dispatchers.Default) { try { Vignette.along(r) } catch (e: Exception) { emptyList() } }
                if (route === r) { if (vignetteRoute == null || vignetteRoute?.let { o -> o.pts.lastOrNull() != r.pts.lastOrNull() } == true) vignetteSaid.clear(); routeVignette = vs; vignetteRoute = r }
            }
        }
        scope.launch {
            val zs = withContext(Dispatchers.Default) { try { Zones.entries(r, Zones.all(ctx)).map { it.first } } catch (e: Exception) { emptyList() } }
            if (route === r) { routeZones = zs.map { it.name }.distinct(); routeZoneCountries = zs.map { it.country }.toSet() }
        }
    }

    // ---- feedback ----
    /** Everything useful for a bug report, with your own words first: the app and unit, the main settings, the areas and the last crash. */
    fun feedbackReport(note: String): String {
        val dm = ctx.resources.displayMetrics
        val crash = try { java.io.File(ctx.filesDir, "lastcrash.txt").takeIf { it.exists() }?.readText()?.take(2500) } catch (e: Exception) { null }
        return buildString {
            if (note.isNotBlank()) { appendLine(note.trim()); appendLine() }
            appendLine("---")
            appendLine("Horizon Nav ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR})")
            appendLine("Unit: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT}), screen ${dm.widthPixels}x${dm.heightPixels}")
            appendLine("Country: ${detectedCountry.ifBlank { "unknown" }} · map: ${prefs.mapSource()} · TomTom key: ${if (hasKey) "yes" else "no"} · Mapbox token: ${if (hasMapbox) "yes" else "no"} · routes from: ${provider.ifBlank { "offline" }} · online: ${if (online()) "yes" else "no"}")
            appendLine("Offline area: ${installedPackId() ?: "none"}")
            appendLine("Frame rate: ${prefs.s(PreferenceKeys.MAP_FPS, "auto")} · map detail: ${prefs.mapDetail()} · driving side: ${if (mirrored) "left-hand" else "right-hand"}")
            if (perfLine.isNotBlank()) appendLine("Performance: $perfLine")
            try { append(SessionWatch.report(ctx)) } catch (e: Exception) { }
            if (laneDiag.isNotBlank()) appendLine(laneDiag)
            appendLine("Lane list: ${if (laneSource.isNotBlank()) laneSource else "TomTom's / the route's own"}")
            run {
                val t = mbToken
                val shape = when { t.isBlank() -> "none"; !MapboxRest.looksValid(t) -> "not a token"; !MapboxRest.looksWhole(t) -> "looks incomplete"; else -> "looks whole" }
                appendLine("Mapbox token: ${t.take(3)}… $shape" + (if (mbCheckNote.isNotBlank()) " · $mbCheckNote" else "") + (if (mbKeyRefused) " · refused" else "") + (if (mbRefusedWhy.isNotBlank()) " · $mbRefusedWhy" else ""))
            }
            if (crash != null) { appendLine(); appendLine("Last crash:"); appendLine(crash) }
            val freeze = try { FreezeWatch.file(ctx).takeIf { it.exists() }?.readText()?.take(2500) } catch (e: Exception) { null }
            if (freeze != null) { appendLine(); appendLine("Last freeze:"); appendLine(freeze) }
        }
    }

    // ---- parking near the destination ----
    /** A few car parks close to where you're going, offered about a mile before you arrive. */
    var parkingNear by mutableStateOf<List<Nearby>>(emptyList())
    private var parkingFor: Route? = null

    private fun checkParking(r: Route, rem: Double) {
        if (parkingFor === r || rem > 1700.0 || rem < 150.0 || r.len < 3000.0 || previewing || !prefs.b(PreferenceKeys.PARKPROMPT, true)) return
        val d = dest ?: return
        parkingFor = r
        // going to a car park already: nothing to offer
        val nm = d.name.lowercase()
        if (nm.contains("parking") || nm.contains("car park") || nm.contains("multi-storey") || nm.contains("multistorey")) return
        scope.launch {
            val list = withContext(Dispatchers.IO) {
                val live = async { try { if (online()) ParkingLive.around(d.pos, 1000) else null } catch (e: Exception) { null } }
                val tt = if (hasOnline && online()) try { netPoiNearby("parking", d.pos, 700) } catch (e: Exception) { null } else null
                val found = (tt?.takeIf { it.isNotEmpty() } ?: try { if (online()) Overpass.nearby("parking", d.pos, 700) else emptyList() } catch (e: Exception) { emptyList() })
                    .map { it.copy(dist = Geo.dist(d.pos, it.place.pos)) }.filter { it.dist < 800.0 }
                // car parks with live free spaces: the nearest with room first, full ones at the end
                ParkingLive.sortNearby(ParkingLive.enrich(found, live.await().orEmpty(), d.pos, 800.0)).take(3)
            }
            if (route === r && dest === d) parkingNear = list
        }
    }

    /** Drives to a car park from the list instead (you walk the last bit). */
    fun parkAt(n: Nearby) {
        val d = dest
        parkingNear = emptyList()
        startRoute(Place(n.place.name, Lang.t("Parkplatz", "Car park") + (if (d != null) Lang.t(" bei ${d.name}", " near ${d.name}") else ""), n.place.pos), startNow = true)
        parkingFor = route
    }

    // ---- the list of turns (tap the turn card) ----
    var itineraryOpen by mutableStateOf(false)

    class ItinRow(val index: Int, val maneuver: String, val text: String, val tag: String, val dist: Double, val arrive: String, val off: Double, val road: String, val exit: Int)

    /** Every turn still ahead on the route, nearest first, with how far away it is and roughly when you'll get there. */
    fun itinerary(): List<ItinRow> {
        val r = route ?: return emptyList()
        val prog = curProgress
        val left = max(1.0, r.len - prog)
        val secs = eta?.secs ?: (r.time * left / max(1.0, r.len))
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val now = System.currentTimeMillis()
        val out = ArrayList<ItinRow>()
        r.steps.forEachIndexed { i, s ->
            if (s.off <= prog + 4) return@forEachIndexed
            val arriving = s.maneuver.uppercase().startsWith("ARRIVE")
            val road = s.street.ifBlank { s.sign }
            val text = if (arriving) Lang.t("Ankunft: ", "Arrive at ") + (dest?.name ?: Lang.t("dein Ziel", "your destination"))
                       else Geo.maneuverText(s.maneuver, s.roundExit).replaceFirstChar { it.uppercase() } + (if (road.isNotBlank()) Lang.t(" auf $road", " onto $road") else "")
            val d = s.off - prog
            val tag = (if (arriving) null else junctionAt(r, s))?.let { Junctions.label(it.first, it.second) } ?: Geo.stepTag(s)
            out += ItinRow(i, s.maneuver, text, tag, d, fmt.format(Date(now + (secs * d / left * 1000).toLong())), s.off, if (arriving) "" else road, s.roundExit)
        }
        return out
    }

    /** Shows one junction from the list on the map (the camera goes back to the car when the list closes). */
    fun previewTurn(row: ItinRow) {
        val r = route ?: return
        following = false
        val pts = (-3..3).map { k -> pointAt(r, (row.off + k * 50.0).coerceIn(0.0, r.len)).first }
        map.fit(pts)
    }

    fun closeItinerary() {
        itineraryOpen = false
        if (!following && route != null && !previewing) recenter()
    }

    /** Plans the route again without the road a turn in the list leads onto (the first 3 km of it). */
    fun avoidTurnRoad(row: ItinRow) {
        val r = route ?: return
        val next = r.steps.getOrNull(row.index + 1)?.off ?: r.len
        val p0 = row.off + 40.0
        val p1 = min(next - 40.0, p0 + 3000.0)
        if (p1 <= p0) { toast(ctx.getString(R.string.toast_044), 2500); return }
        itineraryOpen = false
        avoidStretch(r, p0, p1)
    }

    /** Works the route out again without the road you are on now (the stretch up to the next turn). */
    fun avoidRoadAhead() {
        val r = route ?: return
        if (previewing || dest == null) return
        if (fix == null) { toast(ctx.getString(R.string.toast_013)); return }
        val p0 = curProgress + 60.0
        val toTurn = turn?.dist ?: 1500.0
        val p1 = p0 + toTurn.coerceIn(300.0, 3000.0)
        avoidStretch(r, p0, p1)
    }

    /** Keeps the next route out of the part of [r] between [p0] and [p1] metres along it, and plans again. */
    private fun avoidStretch(r: Route, p0: Double, p1: Double) {
        val d = dest ?: return
        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
        // the ends of the stretch as well as the route's own points (a long straight road can have none in between)
        val inside = r.pts.indices.filter { r.cum[it] in p0..p1 }.map { r.pts[it] } + pointAt(r, p0).first + pointAt(r, p1).first
        for (q in inside) {
            minLat = min(minLat, q.lat); maxLat = max(maxLat, q.lat); minLon = min(minLon, q.lon); maxLon = max(maxLon, q.lon)
        }
        if (minLat > maxLat) { toast(ctx.getString(R.string.toast_027), 2500); return }
        val pad = 0.0004
        sessionAvoid += LL(minLat - pad, minLon - pad) to LL(maxLat + pad, maxLon + pad)
        toast(ctx.getString(R.string.toast_011), 2500)
        startRoute(d, 0, true, startNow = true)
    }

    fun resetTrip() { tripDist = 0.0; tripMax = 0.0; tripMoving = 0.0; tripStart = System.currentTimeMillis() }

    // ---- search + routing ----
    /** True when the device has a network that claims internet access (it can still be too weak to use). */
    fun online(): Boolean {
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            val n = cm.activeNetwork
            if (n == null) false else cm.getNetworkCapabilities(n)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) { true }
    }

    /** True on Wi-Fi or another network that isn't metered (safe for the big offline download). */
    fun onWifi(): Boolean = try {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        caps != null && (caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
    } catch (e: Exception) { false }

    /** How many days old the downloaded offline pack is, or null when there isn't one. */
    fun packAgeDays(): Int? {
        if (!OfflineRouter.installed(ctx)) return null
        val t = java.io.File(OfflineRouter.dir(ctx), "tiles.tar").lastModified()
        if (t <= 0L) return null
        return ((System.currentTimeMillis() - t) / 86_400_000L).toInt()
    }

    /** The offline pack is old enough to be worth updating (about 4 months), and the reminder wasn't put off in the last 30 days. */
    fun packNeedsUpdate(): Boolean {
        val age = packAgeDays() ?: return false
        if (age < 120 || packBusy) return false
        val snoozed = prefs.s(PreferenceKeys.PACKNAG, "0").toLongOrNull() ?: 0L
        return System.currentTimeMillis() - snoozed > 30L * 86_400_000L
    }

    fun snoozePackUpdate() { prefs.put(PreferenceKeys.PACKNAG, System.currentTimeMillis().toString()) }

    // ---- app updates (the shareable build only) ----
    /** A newer version of the app that's been published, shown as a card on the map until you update or say later. */
    var appUpdate by mutableStateOf<Updater.Update?>(null)
    var updateBusy by mutableStateOf(false)
    var updateStatus by mutableStateOf("")

    /** Guards the check before its coroutine is launched, preventing overlapping lifecycle/network callbacks. */
    @Volatile private var updateCheckInFlight = false

    /** When the last automatic app / map-data checks ran (opening the app, or the connection coming back). */
    private var lastAutoUpdateCheck = 0L
    private var lastAutoDataCheck = 0L
    /** Automatic checks run at most this often: the head unit opens and resumes the app, and drops and regains its
     *  connection, many times a drive, and each check is network work (GitHub also limits how often it can be asked). */
    private val autoCheckGapMs = 30 * 60_000L

    /** Check when the app is opened or resumed (at most every half hour). Network work runs on IO. */
    fun onOpened() {
        if (!online()) return
        checkForUpdate()
        checkDataUpdates()
    }

    @Synchronized
    fun checkForUpdate(manual: Boolean = false) {
        if (!Updater.enabled || updateBusy || updateCheckInFlight) return
        val now = System.currentTimeMillis()
        if (!manual) {
            // "Look for updates by itself" switched off in Settings: only the Check button looks
            if (!prefs.b(PreferenceKeys.UPDCHECK, true)) return
            if (now - lastAutoUpdateCheck < autoCheckGapMs) return
            lastAutoUpdateCheck = now
        }
        updateCheckInFlight = true
        if (manual) updateStatus = Lang.t("Wird geprüft…", "Checking…")
        scope.launch {
            val u = try { withContext(Dispatchers.IO) { Updater.check() } } catch (e: Exception) {
                if (manual) updateStatus = when ((e as? Updater.CheckFailed)?.code) {
                    null -> Lang.t("Prüfung nicht möglich: keine Verbindung", "Couldn't check: no connection")
                    404 -> Lang.t("Prüfung nicht möglich: keine veröffentlichte Version auf GitHub gefunden", "Couldn't check: no published version found on GitHub (is the repository private?)")
                    403, 429 -> Lang.t("Prüfung nicht möglich: GitHub begrenzt die Anfragen, versuche es später erneut", "Couldn't check: GitHub is limiting requests, try again later")
                    else -> Lang.t("Prüfung nicht möglich: GitHub antwortete ${(e as Updater.CheckFailed).code}", "Couldn't check: GitHub answered ${(e as Updater.CheckFailed).code}")
                }
                return@launch
            } finally { updateCheckInFlight = false }
            prefs.put(PreferenceKeys.UPDATE_LAST_CHECK, now.toString())
            if (u == null) { if (manual) updateStatus = Lang.t("Du hast die neueste Version", "You have the latest version"); return@launch }
            if (manual) updateStatus = Lang.t("Version ${u.version} ist verfügbar", "Version ${u.version} is available")
            // "later" keeps the card away for a few days, unless you check by hand
            val snoozed = (prefs.s(PreferenceKeys.UPDSNOOZE, "0").toLongOrNull() ?: 0L) > now
            if (manual || !snoozed) appUpdate = u
        }
    }

    fun snoozeAppUpdate() { prefs.put(PreferenceKeys.UPDSNOOZE, (System.currentTimeMillis() + 12 * 3600_000L).toString()); appUpdate = null }

    // ---- newer map data for the downloaded areas ----
    /** Downloaded areas with newer data published (a rebuilt pack, or new speed limit / camera files). */
    var dataUpdates by mutableStateOf<List<String>>(emptyList())

    fun checkDataUpdates() {
        if (packBusy) return
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastAutoDataCheck < autoCheckGapMs) return
        lastAutoDataCheck = nowMs
        val snoozed = (prefs.s(PreferenceKeys.DATASNOOZE, "0").toLongOrNull() ?: 0L) > System.currentTimeMillis()
        if (snoozed) return
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                OfflineRouter.installedIds(ctx).filter { PackRegions.newerOnline(PackRegions.byId(it), OfflineRouter.packDir(ctx, it)) == true }
            }
            dataUpdates = found
        }
    }

    /** Updates the first area with newer data (the others follow, one at a time, as each finishes). */
    fun updateData(id: String) {
        dataUpdates = dataUpdates - id
        downloadRegion(PackRegions.byId(id))
    }

    fun snoozeDataUpdates() { prefs.put(PreferenceKeys.DATASNOOZE, (System.currentTimeMillis() + 3 * 24 * 3600_000L).toString()); dataUpdates = emptyList() }

    /** Downloads the new version and opens Android's installer (asking first for permission to install, if it's needed). */
    fun installAppUpdate() {
        val u = appUpdate ?: return
        if (updateBusy) return
        if (!Updater.canInstall(ctx)) {
            toast(ctx.getString(R.string.toast_002), 6000)
            try { Updater.openInstallSetting(ctx) } catch (e: Exception) { }
            return
        }
        updateBusy = true; updateStatus = Lang.t("Download startet…", "Starting the download…")
        // installed with AAEnabler: the update is saved to Downloads for AAEnabler to install, so it stays in Android Auto
        val forAaTool = Updater.installedViaAaTool(ctx) && android.os.Build.VERSION.SDK_INT >= 29
        scope.launch {
            try {
                withContext(Dispatchers.IO) { Updater.downloadAndInstall(ctx, u, toDownloads = forAaTool) { updateStatus = it } }
                updateStatus = if (forAaTool) Lang.t("Als HorizonNav-${u.version}.apk in Downloads gespeichert. Installiere sie mit AAEnabler, damit sie in Android Auto bleibt", "Saved to Downloads as HorizonNav-${u.version}.apk. Install it with AAEnabler to keep it in Android Auto")
                    else Lang.t("Folge dem Installationsprogramm, um abzuschließen", "Follow the installer to finish")
            } catch (e: Exception) {
                // Never bypass checksum verification by opening the APK URL after a failed download.
                // Browser fallback is reserved for devices that genuinely lack DownloadManager.
                val message = e.message.orEmpty()
                if (message.contains("No download service", ignoreCase = true)) {
                    updateStatus = Lang.t("Download-Dienst nicht verfügbar, der Download wird im Browser geöffnet…", "Download service unavailable; opening the download in your browser…")
                    try { Updater.openInBrowser(ctx, u) }
                    catch (_: Exception) { updateStatus = Lang.t("Download konnte nicht geöffnet werden. Bitte erneut versuchen.", "Couldn't open the download. Please try again.") }
                } else {
                    updateStatus = when {
                        message.contains("checksum", ignoreCase = true) -> Lang.t("Prüfung fehlgeschlagen. Die Datei wurde gelöscht, bitte erneut versuchen.", "Verification failed. The downloaded file was removed; please try again.")
                        message.contains("timed out", ignoreCase = true) -> Lang.t("Zeitüberschreitung beim Download. Bitte erneut versuchen.", "Download timed out. Please try again.")
                        message.contains("cancelled", ignoreCase = true) -> Lang.t("Download abgebrochen. Bitte erneut versuchen.", "Download cancelled. Please try again.")
                        message.isNotBlank() -> message.take(140)
                        else -> Lang.t("Update fehlgeschlagen. Bitte erneut versuchen.", "Update failed. Please try again.")
                    }
                }
            } finally { updateBusy = false }
        }
    }

    /** Updates the offline pack that's on the unit (or the one chosen in Settings). */
    fun updatePack() = downloadRegion(PackRegions.byId(installedPackId() ?: prefs.s(PreferenceKeys.PACK_REGION, "ew")))

    /** The area in use (the one you're in, when several are downloaded), or null when there's none. */
    fun installedPackId(): String? = OfflineRouter.activeId(ctx)

    /** Every downloaded area. */
    fun installedPacks(): List<String> = OfflineRouter.installedIds(ctx)

    /** Moves a pack from before several areas could be kept into its own area folder (once). */
    private fun migratePack() {
        if (!OfflineRouter.installedIn(OfflineRouter.legacyDir(ctx))) return
        val id = prefs.s(PreferenceKeys.PACKINSTALLED, "").ifBlank { prefs.s(PreferenceKeys.PACK_REGION, "ew").takeIf { it == "gb" } ?: "ew" }
        OfflineRouter.migrateLegacy(ctx, id)
    }

    /** The area chosen for the next download: what you picked, else the one in use, else a guess from the country you're in. */
    fun chosenPack(): PackRegion {
        val picked = prefs.s(PreferenceKeys.PACK_REGION, "")
        if (picked.isNotBlank()) return PackRegions.byId(picked)
        installedPackId()?.let { return PackRegions.byId(it) }
        return PackRegions.suggestFor(detectedCountry) ?: PackRegions.byId("ew")
    }

    /** Switches to a downloaded area by hand (it switches back by itself if you're somewhere another area covers better). */
    fun useArea(id: String) {
        OfflineRouter.setActive(ctx, id); areaPending = null; areaCheckAt = System.currentTimeMillis()
        packRev++
        if (useOfflineMap()) restyle()
    }

    /**
     * Downloads the pack for [r] alongside any others already here (or updates it). It checks the pack has been built first, and that
     * there's room for it.
     */
    fun downloadRegion(r: PackRegion) {
        if (packBusy) return
        packBusy = true; packStatus = Lang.t("Paket ${r.name} wird geprüft…", "Checking the ${r.name} pack…")
        scope.launch {
            val ok = withContext(Dispatchers.IO) { PackRegions.available(r) }
            when (ok) {
                false -> { packBusy = false; packStatus = Lang.t("Das Paket ${r.name} ist noch nicht verfügbar. Es erscheint hier, sobald es gebaut wurde.", "The ${r.name} pack isn't available yet. It appears here once it has been built."); return@launch }
                null -> { packBusy = false; packStatus = Lang.t("Download nicht erreichbar. Prüfe die Verbindung und versuche es erneut.", "Couldn't reach the download. Check the connection and try again."); return@launch }
                true -> {}
            }
            packBusy = false
            downloadPack(r.url, r.id, r.extrasUrl) {
                // a new area becomes the one in use straight away (it switches to whichever covers you within a minute or so)
                OfflineRouter.setActive(ctx, r.id)
            }
        }
    }

    /** Places that can be found without signal: saved, favourite and previously used ones, then the downloaded UK towns and villages. */
    private suspend fun offlineMatches(q: String, near: LL?): List<Place> = withContext(Dispatchers.IO) {
        // a search replaced by a newer one (you typed another letter) stops reading the pack's lists at once
        val job = coroutineContext[kotlinx.coroutines.Job]
        val alive = { job?.isActive != false }
        try {
            val own = places.searchLocal(q, near)
            val addr = OfflineRouter.searchAddresses(ctx, q, near, alive).filter { t -> own.none { Geo.dist(it.pos, t.pos) < 30 } }
            val pack = (if (q.trim().length >= 3) {
                // the area in use first; the other downloaded areas too when it has little (a place over the border, say)
                val first = OfflineRouter.searchPlaces(ctx, q, near, alive)
                if (first.size >= 8) first else {
                    val act = OfflineRouter.activeId(ctx)
                    first + OfflineRouter.installedIds(ctx).filter { it != act }.flatMap { id ->
                        if (alive()) OfflineRouter.searchPlaces(ctx, q, near, alive, OfflineRouter.packDir(ctx, id)) else emptyList()
                    }
                }
            } else emptyList()).filter { t -> own.none { Geo.dist(it.pos, t.pos) < 150 } }
            val towns = OfflinePlaces.search(ctx, q, near).filter { t -> own.none { Geo.dist(it.pos, t.pos) < 400 } && pack.none { Geo.dist(it.pos, t.pos) < 1500 && it.name.equals(t.name, true) } }
            own + addr + pack + towns
        } catch (e: Throwable) { emptyList() }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    /**
     * The search service's results with the shops near you added: for a kind of place ("Supermarkt") the nearby ones come first,
     * for a name they join the list and are ranked by the name and nearness like the rest. Null when both found nothing because
     * there was no signal.
     */
    private fun withNearby(q: String, net: List<Place>?, shops: List<Place>): List<Place>? {
        if (shops.isEmpty()) return net
        val rest = (net ?: emptyList()).filter { p -> shops.none { Geo.dist(it.pos, p.pos) < 40 || (it.name.equals(p.name, true) && Geo.dist(it.pos, p.pos) < 150) } }
        return if (PoiSearch.kindOf(q) != null) shops + rest else rest + shops
    }

    /**
     * Exact name matches first, then names starting with what you typed (towns and cities before streets, see [SearchRank]),
     * then the rest with online results ahead of offline ones.
     */
    private fun rankResults(q: String, near: LL?, net: List<Place>, extra: List<Place>): List<Place> {
        val all = ArrayList<Pair<Place, Int>>()
        net.forEachIndexed { i, p -> all += p to i }
        extra.forEachIndexed { i, raw ->
            // with signal the downloaded results are mixed in with the online ones: no "offline" note on them
            val p = SearchRank.online(raw)
            // the same place found online and in a downloaded list: keep the online one, but a town stays a town
            val dup = all.firstOrNull { it.first.name.equals(p.name, true) && Geo.dist(it.first.pos, p.pos) < (if (p.town) 3000 else 300) || Geo.dist(it.first.pos, p.pos) < 40 }
            if (dup == null) all += p to (1000 + i) else if (p.town && dup.first.name.equals(p.name, true)) dup.first.town = true
        }
        // a kind of place ("Supermarkt"): the nearby ones of that kind first, not places that happen to be called that
        if (PoiSearch.kindOf(q) != null) return all.sortedBy { it.second }.map { it.first }.take(15)
        return districtThird(q, SearchRank.rank(q, all, { it.name }, { it.town }, { if (near != null) Geo.dist(near, it.pos) else 0.0 })).take(15)
    }

    /**
     * A postcode district on its own ("M25", "B1") is also how roads are named: the district goes third, so a search for the M25
     * still shows the motorway first. A full postcode ("SO14 2AB") keeps its place.
     */
    private fun districtThird(q: String, list: List<Place>): List<Place> {
        if (!Regex("^[A-Za-z]{1,2}\\d[A-Za-z\\d]?$").matches(q.trim())) return list
        val pc = list.indexOfFirst { it.sub.startsWith("Postleitzahl") || it.sub.startsWith("Postcode") || it.sub.startsWith("Postal code") }
        if (pc < 0 || pc >= 2 || list.size < 2) return list
        val m = list.toMutableList(); val d = m.removeAt(pc); m.add(min(2, m.size), d); return m
    }

    fun search(q: String) {
        if (chain != null || chainBusy) closeChain()
        if (q.length < 2) { results = emptyList(); return }
        val near = fix?.pos ?: map.center()
        searchJob?.cancel()
        searchJob = scope.launch {
            // with signal it always searches online (TomTom, Mapbox, or OpenStreetMap with neither); with none, the downloaded names
            if (!online()) {
                val local = offlineMatches(q, near)
                results = local
                if (local.isEmpty()) toast(if (OfflinePlaces.installed(ctx) || OfflineRouter.hasPlaces(ctx) || OfflineRouter.hasAddresses(ctx)) Lang.t("Kein Empfang, und nichts Gespeichertes oder Heruntergeladenes passt", "No signal, and nothing saved or downloaded matches") else Lang.t("Kein Empfang. Lade unter Einstellungen > Offline-Karten ein Paket herunter, um ohne Empfang zu suchen", "No signal. Download the offline pack or place names in Settings > Offline maps to search with no signal"), 4500)
                else toast(ctx.getString(R.string.toast_022), 3000)
                return@launch
            }
            // the online search and the saved / downloaded places run at the same time, and their results are merged
            val localJob = async { offlineMatches(q, near) }
            val nearJob = async(Dispatchers.IO) { try { nearbyMatches(q, near) } catch (e: Exception) { emptyList() } }
            val net0 = try { withContext(Dispatchers.IO) { netGeocode(countryCode(), q, near) } } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { android.util.Log.w("NavEngine", "search failed", e); null }
            val shops = kotlinx.coroutines.withTimeoutOrNull(10_000L) { nearJob.await() } ?: emptyList()
            val net = withNearby(q, net0, shops)
            // the online results are shown straight away if the downloaded lists are still being searched; those are merged in after
            var local = kotlinx.coroutines.withTimeoutOrNull(500L) { localJob.await() }
            if (local == null) {
                val first = rankResults(q, near, net ?: emptyList(), emptyList())
                if (first.isNotEmpty()) results = first
                local = localJob.await()
            }
            val merged = rankResults(q, near, net ?: emptyList(), local)
            results = merged
            loadLogos(merged)
            if (merged.isEmpty()) toast(if (net == null) Lang.t("Suche fehlgeschlagen: schwacher oder kein Empfang", "Search failed: weak or no signal") else Lang.t("Keine Treffer. Füge einen Ort oder eine Postleitzahl hinzu.", "No matches. Try adding a town or postcode."), 3500)
            else if (net == null) toast(ctx.getString(R.string.toast_049), 3500)
        }
    }

    // ---- every location of a shop or chain ("Alle N Standorte") ----

    /** One branch in the all-locations panel: [secs] is the drive time with today's traffic once known, [offRoute] metres from the route. */
    class ChainLoc(val place: Place, val dist: Double, val offRoute: Double?) {
        var secs by mutableStateOf<Double?>(null)
    }
    class ChainView(val name: String, val center: LL, val locs: List<ChainLoc>)

    var chain by mutableStateOf<ChainView?>(null)
    var chainBusy by mutableStateOf(false)
    /** "quick", "near" or "route" */
    var chainSort by mutableStateOf("quick")
    var chainSel by mutableStateOf<ChainLoc?>(null)
    private var chainJob: kotlinx.coroutines.Job? = null

    /** How many results are the same shop or chain as [p] within 25 km of here (the button shows from 2). */
    fun chainCount(p: Place): Int {
        val k = BrandLogos.key(p.brand ?: p.name)
        if (k.length < 2) return 0
        val here = fix?.pos ?: map.center() ?: return 0
        return results.count { r -> BrandLogos.sameChain(r.name, r.brand, k) && Geo.dist(here, r.pos) <= 25_000 }
    }

    /** Opens the panel with every branch of [p]'s chain within 25 km of [around] (here, or where the map is for "search this area"). */
    fun showAllLocations(p: Place, around: LL? = null) = showAllLocations(p.brand ?: p.name, around, p.web)

    fun showAllLocations(name: String, around: LL? = null, web: String? = null) {
        val center = around ?: fix?.pos ?: map.center() ?: return
        val k = BrandLogos.key(name)
        chainJob?.cancel()
        chainBusy = true
        chainSel = null
        if (chainSort == "route" && (route == null || previewing)) chainSort = "quick"
        chainJob = scope.launch {
            val shown = results
            val found = withContext(Dispatchers.IO) {
                val more = if (online()) try { nearbyMatches(name, center, 40) } catch (e: Exception) { emptyList() } else emptyList()
                (shown + more).filter { BrandLogos.sameChain(it.name, it.brand, k) }
                    .filter { Geo.dist(center, it.pos) <= 25_000 }
            }
            val uniq = PoiSearch.distinct(found).onEach { if (it.web == null && web != null) it.web = web }
            val line = route?.takeIf { !previewing }?.pts
            val locs = uniq.map { pl ->
                val off = line?.let { pts -> var b = Double.MAX_VALUE; val step = maxOf(1, pts.size / 400); var i = 0; while (i < pts.size) { b = min(b, Geo.dist(pts[i], pl.pos)); i += step }; b }
                ChainLoc(pl, Geo.dist(fix?.pos ?: center, pl.pos), off)
            }.sortedBy { it.dist }.take(24)
            chain = ChainView(name, center, locs)
            chainBusy = false
            showChainOnMap()
            map.fit(locs.map { it.place.pos } + listOfNotNull(fix?.pos))
            // drive times with today's traffic (Mapbox's matrix), then the logos
            val from = fix?.pos
            if (from != null && online() && locs.isNotEmpty()) {
                val t = if (hasMapbox) withContext(Dispatchers.IO) { try { MapboxRest.driveTimes(mbToken, from, locs.map { it.place.pos }) } catch (e: Exception) { android.util.Log.w("NavEngine", "chain drive times (Mapbox) failed", e); null } } else null
                if (t != null) t.forEachIndexed { i, v -> locs.getOrNull(i)?.secs = v }
                // no Mapbox token (or it failed): TomTom's route time for the eight nearest, one request each
                else if (hasKey && refusedKey != apiKey) withContext(Dispatchers.IO) {
                    for (l in locs.take(8)) try { l.secs = TomTomRest.driveTime(apiKey, from, l.place.pos) } catch (e: Exception) { android.util.Log.w("NavEngine", "chain drive time (TomTom) failed", e); break }
                }
                showChainOnMap()
            }
            val doms = locs.mapNotNull { BrandLogos.domainFor(it.place) }.distinct()
            if (doms.isNotEmpty() && online()) {
                withContext(Dispatchers.IO) { for (d in doms) try { BrandLogos.load(ctx, d) } catch (e: Exception) { } }
                logoRev++
                showChainOnMap()
            }
        }
    }

    /** Loads the logos of the shops in [list] that aren't on the unit yet (once each), then redraws the lists. */
    private fun loadLogos(list: List<Place>) {
        if (!online()) return
        val doms = list.mapNotNull { BrandLogos.domainFor(it) }.distinct().filter { BrandLogos.cached(ctx, it) == null }
        if (doms.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) { for (d in doms.take(10)) try { BrandLogos.load(ctx, d) } catch (e: Exception) { } }
            logoRev++
        }
    }

    /** Bumped when logos have been loaded, so the lists draw them. */
    var logoRev by mutableStateOf(0)

    /** The branches in the order chosen ([chainSort]). */
    fun chainSorted(sort: String = chainSort): List<ChainLoc> {
        val l = chain?.locs ?: return emptyList()
        return when (sort) {
            "near" -> l.sortedBy { it.dist }
            "route" -> l.sortedBy { it.offRoute ?: Double.MAX_VALUE }
            // quickest: by drive time, or by distance where it isn't known yet
            else -> l.sortedBy { it.secs ?: (it.dist / 13.0) }
        }
    }

    fun selectChain(c: ChainLoc?) { chainSel = c; showChainOnMap(); c?.let { map.fit(listOfNotNull(it.place.pos, fix?.pos)) } }

    /** "Search this area": the same chain again around where the map is now. */
    fun chainSearchHere() { val c = chain ?: return; showAllLocations(c.name, map.center() ?: c.center, c.locs.firstOrNull()?.place?.web) }

    fun closeChain() { chainJob?.cancel(); chain = null; chainSel = null; chainBusy = false; map.setChainPins(emptyList()) }

    private fun showChainOnMap() {
        val c = chain ?: return
        val sel = chainSel
        map.setChainPins(c.locs.map { l ->
            val dom = BrandLogos.domainFor(l.place)
            MapBinding.ChainPin(l.place.pos, l.secs?.let { Geo.fmtDur(it) } ?: Geo.fmtDist(l.dist, imperial), dom?.let { BrandLogos.cached(ctx, it) }, l === sel)
        })
    }

    /** Search used by the stops editor: the same sources as the search box, returned instead of shown. */
    suspend fun findPlaces(q: String): List<Place> {
        val near = fix?.pos ?: map.center()
        if (!online()) return offlineMatches(q, near)
        // both at once, as in the main search
        return kotlinx.coroutines.coroutineScope {
            val localJob = async { offlineMatches(q, near) }
            val nearJob = async(Dispatchers.IO) { try { nearbyMatches(q, near) } catch (e: Exception) { emptyList() } }
            val net0 = try { withContext(Dispatchers.IO) { netGeocode(countryCode(), q, near) } } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { null }
            val shops = kotlinx.coroutines.withTimeoutOrNull(10_000L) { nearJob.await() } ?: emptyList()
            rankResults(q, near, withNearby(q, net0, shops) ?: emptyList(), localJob.await())
        }
    }

    // a destination chosen with no signal is kept and routed as soon as the connection comes back
    var waitingFor by mutableStateOf<Place?>(null)
    private var waitJob: kotlinx.coroutines.Job? = null

    private fun waitForSignal(to: Place, departInMin: Int, keepVia: Boolean) {
        waitingFor = to
        waitJob?.cancel()
        waitJob = scope.launch {
            var tries = 0
            while (waitingFor === to && tries < 150) {
                delay(8000); tries++
                if (online() && !busy) { startRoute(to, departInMin, keepVia); delay(6000) }
            }
            if (waitingFor === to) waitingFor = null
        }
    }

    fun cancelWaiting() { waitingFor = null; waitJob?.cancel() }

    fun pickPlace(p: Place) {
        val slot = pendingSave
        if (slot != null) {
            places.setSlot(slot, p)
            pendingSave = null
            results = emptyList()
            toast(str(if (slot == "home") R.string.toast_061 else R.string.toast_062, p.name), 3500)
        } else startRoute(p)
    }

    fun setDeparture(minutes: Int) {
        val d = dest ?: return
        startRoute(d, minutes, true)
    }

    /** Pushes the route preferences from Settings into the routing client. */
    private fun syncRoutePrefs(from: LL? = null, to: LL? = null) {
        // areas to keep out of: roads you asked to avoid on this trip, and clean air zones when that is switched on
        // (a zone you start or finish in cannot be avoided, so it is left out)
        val rects = ArrayList<Pair<LL, LL>>(sessionAvoid)
        val rings = ArrayList<List<LL>>()
        for ((sw, ne) in sessionAvoid) rings += listOf(sw, LL(sw.lat, ne.lon), ne, LL(ne.lat, sw.lon), sw)
        if (prefs.b(PreferenceKeys.AVOIDZONES, false)) {
            for (z in Zones.all(ctx)) {
                if ((from != null && Zones.inside(z, from)) || (to != null && Zones.inside(z, to))) continue
                if (z.maxLat - z.minLat > 1.2 || z.maxLon - z.minLon > 1.8) continue
                rects += LL(z.minLat, z.minLon) to LL(z.maxLat, z.maxLon)
                rings.addAll(z.rings)
            }
        }
        // known road closures (traffic feeds and drivers' reports): small boxes on the closed road, all but one at the start or the
        // end of the trip (the only road there: still used). Behind the areas you set yourself, as TomTom takes 10 at most.
        val cl = closureBoxes(from, to)
        rects.addAll(cl.take(max(0, 10 - rects.size)))
        for ((sw, ne) in cl) rings += listOf(sw, LL(sw.lat, ne.lon), ne, LL(ne.lat, sw.lon), sw)
        MapboxRest.excludePts = closurePts(from, to).take(40)
        TomTomRest.avoidRects = rects.take(10)
        OfflineRouter.exclude = rings
        TomTomRest.avoid = listOfNotNull(
            if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) "tollRoads" else null,
            if (prefs.b(PreferenceKeys.AVOID_MOTORWAY, false)) "motorways" else null,
            if (prefs.b(PreferenceKeys.AVOID_FERRY, false)) "ferries" else null,
            if (prefs.b(PreferenceKeys.AVOID_UNPAVED, true)) "unpavedRoads" else null,
        )
        TomTomRest.routeType = if (prefs.b(PreferenceKeys.ROUTE_ECO, false)) "eco" else "fastest"
        // Settings > Fahrzeug: a motorhome's or trailer's size, weight and top speed
        Vehicle.active = Vehicle.from(prefs)
        // electric: TomTom's eco route for an electric car uses the least battery, from how much the car uses at different speeds
        TomTomRest.evParams = if (isEv && prefs.b(PreferenceKeys.ROUTE_ECO, false)) {
            val k = evKwhPerKm() * 100.0
            val batt = evBatteryKwh()
            val now = batt * prefs.i(PreferenceKeys.EV_CHARGE_PCT, 80).coerceIn(5, 100) / 100.0
            String.format(java.util.Locale.US, "&vehicleEngineType=electric&constantSpeedConsumptionInkWhPerHundredkm=50,%.1f:90,%.1f:130,%.1f&currentChargeInkWh=%.1f&maxChargeInkWh=%.1f",
                k * 0.7, k, k * 1.45, now, batt)
        } else ""
        // tolls avoided only in chosen countries (when "Avoid toll roads" itself is off)
        MapboxRest.tollCountries = if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) emptySet()
            else prefs.s(PreferenceKeys.AVOID_TOLL_COUNTRIES, "").split(',').map { it.trim().uppercase() }.filter { it.length == 2 }.toSet()
    }

    fun startRoute(to: Place, departInMin: Int = 0, keepVia: Boolean = false, startNow: Boolean = false) {
        // a drive ends the walk to the car
        if (walking) endWalk()
        syncRoutePrefs(fix?.pos ?: map.center(), to.pos)
        resume = null
        if (!keepVia) { via = emptyList(); evPlanned = emptyList(); evAutoDest = null; evPlanWarn = null }
        if (!hasOnline && !OfflineRouter.installed(ctx)) { toast(ctx.getString(R.string.toast_047), 7000); return }
        val from = fix?.pos ?: map.center() ?: return
        if (fix == null) toast(ctx.getString(R.string.toast_015), 5000)
        busy = true
        scope.launch {
            try {
                val departMs = if (departInMin > 0) System.currentTimeMillis() + departInMin * 60_000L else 0L
                var usedOffline = false
                val rs0 = withContext(Dispatchers.IO) {
                    val haveOffline = OfflineRouter.installed(ctx)
                    val hd = driveHeading(from)
                    if (haveOffline && (!hasOnline || !online())) { usedOffline = true; OfflineRouter.routes(ctx, from, to.pos, via.map { it.pos }, hd) }
                    else try { netRoutes(from, to.pos, true, departMs, via.map { it.pos }, hd, askBoth = true) }
                    catch (e: Exception) {
                        if (haveOffline && NavigationFallbackPolicy.shouldUseOffline(e)) { usedOffline = true; OfflineRouter.routes(ctx, from, to.pos, via.map { it.pos }, hd) } else throw e
                    }
                }
                if (rs0.isEmpty()) throw IllegalStateException("No route found")
                var rs = if (usedOffline) rs0 else avoidVignette(rs0, from, to.pos, departMs)
                if (usedOffline) toast(ctx.getString(R.string.toast_030), 4000)
                // the fastest route is the one chosen first: everything planned for the trip (rain, weather, charging stops) is for it
                // except when the charging stops went onto the trip as it started: then the way that follows the route you chose
                // (fastest, shortest or an alternative) most closely, not simply the quickest one through the stops
                val prefer = evPreferRoute; evPreferRoute = null
                // none of the new ways follows the one picked on the other screen: that one itself is taken
                if (prefer != null && keepVia && rs.none { routeOverlap(it, prefer) > 0.8 }) rs = listOf(prefer) + rs
                // a trip sent from the phone: the way that follows the one picked there most closely
                val track = preferTrack; preferTrack = emptyList()
                val sent = if (track.isNotEmpty() && rs.size > 1) rs.indices.maxByOrNull { i -> track.count { RouteExtras.project(rs[i], it, 300.0) != null } } else null
                val pick = (if (prefer != null && keepVia && rs.size > 1) rs.indices.maxByOrNull { routeOverlap(rs[it], prefer) } else null)
                    ?: sent ?: simplerPick(rs)
                // (a way taken over from the other screen counts as picked by hand; the app's own pick only when clearly slower)
                pickedSlower = rs.size > 1 && (if (prefer != null && keepVia) rs[pick] !== rs.minByOrNull { it.time } else rs[pick].time > rs.minOf { it.time } + 60.0)
                if (rs[pick].steps.isEmpty()) toast(if (lastRouteProvider == "mapbox") Lang.t("Mapbox hat für diese Route keine Abbiegehinweise geliefert", "Mapbox returned no turn instructions for this route") else Lang.t("Dieser Schlüssel liefert keine Abbiegehinweise. Prüfe, ob er Zugriff auf die TomTom Routing API hat", "This key returned no turn instructions. Check it has TomTom Routing API access"), 6000)
                if (departInMin > 0 && !usedOffline && !departApplied) toast(ctx.getString(R.string.toast_007), 4000)
                departMin = departInMin
                departAtMs = if (!usedOffline && departApplied) departMs else 0L
                if (!isPin(to.name)) places.addRecent(to)
                if (!keepVia) habits.record(to)
                loadRain(to, rs[pick].time)
                loadRouteWeather(rs[pick])
                fuelSuggestion = null
                evStops = emptyList(); evWarning = null
                clearLiveAlts()
                if (prefs.b(PreferenceKeys.SMARTFUEL, true)) suggestFuel(rs[pick])
                clearNearby()
                results = emptyList()
                stopSim()
                if (TomTomRest.avoidDropped && !usedOffline) toast(ctx.getString(R.string.toast_005), 5000)
                // (the way sent from the phone was picked by hand there: it is kept like one picked here)
                // the simpler route (fewer turns, about as quick) is labelled as such; none when the fastest was picked
                simplerIdx = if (prefer == null && sent == null && rs.size > 1 && pick != rs.indices.minByOrNull { rs[it].time }) pick else -1
                dest = to; routes = rs; active = pick; userPicked = sent != null && prefer == null
                if (isEv && via.isEmpty() && prefs.b(PreferenceKeys.SMARTFUEL, true)) planEvAllRoutes()
                prediction = null; predictionAt = 0L
                updateRouteZones()
                said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
                turn = null; eta = null; routeFrac = 0f
                map.setDest(to.pos); pushVia()
                drawRoutes()
                following = false
                previewing = true
                val allPts = rs.take(3).flatMap { r -> r.pts.filterIndexed { i, _ -> i % 8 == 0 } + r.pts.last() }
                delay(350)
                // a short fly-through along the new route first (not when stops change mid-drive)
                // (on long routes the fly-through has to load map tiles all along the way, which made the start of a trip stutter;
                // those go straight to the overview)
                if (!startNow && prefs.b(PreferenceKeys.CINEMATIC, true) && rs[pick].len < 40_000.0) {
                    val r0 = rs[pick]
                    map.flyRoute(r0.pts, r0.cum, r0.len, 50.0) { if (previewing && routes === rs) map.fit(allPts) }
                } else map.fit(allPts)
                waitingFor = null; waitJob?.cancel()
                // changing stops while already driving carries straight on, without the route options in between
                if (startNow) beginNavigation()
            } catch (e: Exception) {
                val m = e.message ?: ""
                if (m.contains("signal")) {
                    toast(ctx.getString(R.string.toast_023), 6000)
                    if (waitingFor !== to) waitForSignal(to, departInMin, keepVia)
                } else toast(str(R.string.rest_439, m), 6000)
            } finally { busy = false }
        }
    }

    /** True once a way was picked by hand from the choice (not the app's own pick): then it is kept, see [pickedSlower]. */
    private var userPicked = false

    fun selectRoute(i: Int, byUser: Boolean = true) {
        if (i !in routes.indices) return
        if (byUser) userPicked = true
        map.stopFly()
        active = i; lastIdx = 0
        updateRouteZones()
        drawRoutes()
        val r = routes[i]
        map.fit(r.pts.filterIndexed { n, _ -> n % 6 == 0 } + r.pts.last())
        // electric, no stops yet: the charging plan was for the first route, plan again for this one
        if (isEv && via.isEmpty() && prefs.b(PreferenceKeys.SMARTFUEL, true)) planEv(r)
    }

    /** A name for the map strip of the current route: "Southampton → Bristol" style, from where you are to the destination. */
    fun offlineRouteName(): String {
        val to = dest?.name?.takeIf { it.isNotBlank() } ?: Lang.t("Ziel", "destination")
        val from = onRoad.takeIf { it.isNotBlank() } ?: Lang.t("Hier", "here")
        return "$from → $to"
    }

    /** The route chosen on the cards, while the trip is worked out again through its charging stops (see startRoute). */
    private var evPreferRoute: Route? = null
    /** The route being driven was picked over a quicker one: faster ways are only offered (a card for 7 s), never switched to. */
    private var pickedSlower = false
    /** Starting was asked for while the charging was still being planned: it starts as soon as the plan is in. */
    private var evStartPending = false

    /** How much of [b] route [a] follows (share of sampled points of [a] within 300 m of [b]). */
    private fun routeOverlap(a: Route, b: Route): Double {
        if (a.pts.isEmpty()) return 0.0
        val step = maxOf(1, a.pts.size / 60)
        var n = 0; var on = 0
        for (i in a.pts.indices step step) { n++; if (RouteExtras.project(b, a.pts[i], 300.0) != null) on++ }
        return if (n == 0) 0.0 else on.toDouble() / n
    }

    /**
     * The way to take from new routes [rs] worked out on the go: after picking a slower way yourself, the one that keeps to it
     * ([old], the route you were on) most closely; otherwise the quickest. A reroute used to always take the quickest, which threw
     * away the way you had chosen.
     */
    private fun keepChosen(rs: List<Route>, old: Route?): Int {
        if (!pickedSlower || old == null) return NavigationPolicy.keepChosenIndex(rs.map { it.time }, emptyList(), false)
        return NavigationPolicy.keepChosenIndex(rs.map { it.time }, rs.map { routeOverlap(it, old) }, true)
    }

    fun beginNavigation() {
        if (!previewing) return
        // electric: the charging plan of the chosen route is still being worked out. Starting now would set off without its stops
        // (they would only come a little later, with the route worked out again on the way): wait for the plan, then start
        if (isEv && via.isEmpty() && evPlanningFor != null && prefs.b(PreferenceKeys.SMARTFUEL, true)) {
            if (!evStartPending) toast(Lang.t("Ladestopps werden noch geplant, die Navigation startet gleich …", "Still planning the charging stops, starting in a moment …"), 3500)
            evStartPending = true
            return
        }
        evStartPending = false
        // electric: the chosen route's charging stops go onto the trip as it starts (once per destination), and the route is
        // worked out through them; the other routes stayed on offer until now
        val d0 = dest
        if (isEv && via.isEmpty() && evStops.isNotEmpty() && evPlanningFor == null && prefs.b(PreferenceKeys.SMARTFUEL, true) && d0 != null &&
            evAutoDest?.let { Geo.dist(it, d0.pos) < 50.0 } != true) {
            evAutoDest = d0.pos
            evPreferRoute = route
            addStops(evStops, auto = true, startNow = true)
            return
        }
        map.stopFly()
        // a slower way picked from the choice: kept, faster ones are only offered
        // (any way that isn't the quickest: also one only seconds slower, else a reroute or a quicker way found later took it away)
        // The app's own pick (a simpler way up to 90 s slower, or the best one counting charging) is not a pick by hand: it still gives
        // way to a quicker route later, unless it is over a minute slower as before.
        if (routes.size > 1) pickedSlower = route?.let { r -> NavigationPolicy.isPickedSlower(r.time, routes.minOf { it.time }, userPicked) } == true
        previewing = false
        // where this trip started, for "Route back" when you arrive
        tripFrom = fix?.pos
        // the other screen (phone or car) follows onto the same trip
        dest?.let { d -> try { RouteSync.started(this, d, via) } catch (e: Exception) { } }
        said.clear(); lastIdx = 0; zoomOffset = 0.0
        // a new trip starts its counters; a trip that only got new stops or a new way (stops added, charging planned again) keeps
        // them, so the battery driven down so far and the trip's time and distance are not forgotten
        val continuing = navStartMs > 0 && navTripDest?.let { a -> dest?.let { Geo.dist(a, it.pos) < 50.0 } } == true
        if (!continuing) {
            navStartMs = System.currentTimeMillis(); navStartDist = tripDist; evSinceNav = 0.0; evChargeRefPct = -1.0; evChargeRefAt = 0L; evSinceCharge = 0.0
            navTripDest = dest?.pos
            evKmAtPlan = null
        }
        lastFasterCheck = System.currentTimeMillis(); faster = null; lastTrafficAt = 0L; undoRoute = null; if (notice?.kind == "reroute") notice = null
        dest?.let { d -> prefs.put(PreferenceKeys.RESUME, org.json.JSONObject().put("n", d.name).put("s", d.sub).put("la", d.pos.lat).put("lo", d.pos.lon).put("t", System.currentTimeMillis()).toString()) }
        following = true
        // Settings > Offline maps > Save every route's map: the Mapbox map along this route, kept for when signal drops
        try { route?.let { r -> MapboxOffline.autoSaveRoute(ctx, prefs, offlineRouteName(), r.pts, onWifi(), map.isSatellite) } } catch (e: Throwable) { }
        // Settings > Satellitenbilder > Luftbilder vorab laden: the satellite photos along this route, kept on the phone
        try {
            val mode = prefs.s(PreferenceKeys.SAT_PREFETCH, "wifi")
            val plan = map.satPrefetchPlan()
            if (plan != null && mode != "off" && (mode == "always" || onWifi())) route?.let { r ->
                SatTileCache.prefetchRoute(ctx, r.pts, plan.first, plan.second) { mode == "always" || onWifi() }
            }
        } catch (e: Throwable) { }
        routeWx.firstOrNull { it.code in 51..67 || it.code in 80..82 || it.code in 95..99 || it.code in 71..77 || it.code in 85..86 || it.rain >= 70 }?.let {
            val snow = it.code in 71..77 || it.code in 85..86
            val inT = if (it.minutes >= 60) "${it.minutes / 60} h ${it.minutes % 60} Min." else "${it.minutes} Min."
            raiseAlert("rain", (if (snow) Lang.t("Schnee", "Snow") else Lang.t("Regen", "Rain")) + Lang.t(" in etwa $inT auf deiner Route erwartet", " expected in about ${inT.replace("Min.", "min")} on your route"), 6000)
            val h = it.minutes / 60; val m = it.minutes % 60
            val said = if (h == 0) Lang.t("$m Minuten", "$m minutes") else Lang.t("$h ${if (h == 1) "Stunde" else "Stunden"}" + (if (m > 0) " $m Minuten" else ""), "$h ${if (h == 1) "hour" else "hours"}" + (if (m > 0) " $m minutes" else ""))
            speak((if (snow) Lang.t("Schnee", "Snow") else Lang.t("Regen", "Rain")) + Lang.t(" in etwa $said auf deiner Route erwartet", " expected in about $said on your route"), true)
        }
        val f = fix
        if (f != null) onPosition(f, true)
        else { updateGuidance(0.0); toast(ctx.getString(R.string.toast_014), 5000) }
        cardsForStartAlts()
    }

    private fun drawRoutes() {
        val r = route ?: return
        map.setRoute(r.pts, routes.filterIndexed { i, _ -> i != active }.map { it.pts }, r.sev)
        map.setTunnels(r.tunnels.map { (a, b) -> slice(r, a, b) })
    }

    fun cycleRoute() {
        if (routes.size < 2) { toast(ctx.getString(R.string.toast_016)); return }
        userPicked = true
        active = (active + 1) % routes.size
        said.clear(); lastIdx = 0
        drawRoutes()
        fix?.let { onPosition(it, true) } ?: updateGuidance(0.0)
    }

    fun toggleAlternatives() {
        alternatives = !alternatives
        toast(if (alternatives) Lang.t("Alternativrouten an", "Alternative routes on") else Lang.t("Alternativrouten aus", "Alternative routes off"), 2500)
        val d = dest
        if (d != null) startRoute(d, 0, true)
    }

    private var rerouting = false

    private fun reroute() {
        val d = dest ?: return
        if (rerouting || fix == null) return
        if (System.currentTimeMillis() - lastReroute < 4000) return
        lastReroute = System.currentTimeMillis()
        rerouting = true
        syncRoutePrefs(fix?.pos, d.pos); faster = null
        toast(ctx.getString(R.string.toast_038), 2500)
        scope.launch {
            var err: String? = null
            try {
                for (attempt in 1..3) {
                    val f = fix ?: break
                    try {
                        val hd = if (f.speed > 2.0) f.heading else -1.0
                        val rs = withContext(Dispatchers.IO) {
                            val haveOffline = OfflineRouter.installed(ctx)
                            if (haveOffline && (!hasOnline || !online())) OfflineRouter.routes(ctx, f.pos, d.pos, via.map { it.pos }, hd)
                            else try { netRoutes(f.pos, d.pos, false, 0L, via.map { it.pos }, hd, true) }
                            catch (e: Exception) {
                                if (haveOffline && NavigationFallbackPolicy.shouldUseOffline(e)) OfflineRouter.routes(ctx, f.pos, d.pos, via.map { it.pos }, hd) else throw e
                            }
                        }
                        // the route was ended (or a new destination chosen) while this was worked out: the old route must not come back
                        if (dest !== d || previewing) { err = null; break }
                        if (rs.isNotEmpty()) { val was = route; routes = rs; active = keepChosen(rs, was); said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0; clearLiveAlts(); drawRoutes(); cardsForStartAlts(); updateRouteZones(); evAfterReroute()
                            // live alternatives looked for again half a minute after a reroute
                            if (prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) lastFasterCheck = System.currentTimeMillis() - 90_000L
                            try { closureAfterReroute(rs[active]) } catch (e: Exception) { }
                            err = null; break }
                        err = "no route found"
                    } catch (e: Exception) { android.util.Log.w("NavEngine", "reroute failed", e); err = e.message ?: "unknown error" }
                    if (attempt < 3) delay(2500)
                    if (dest !== d) { err = null; break }
                }
            } finally {
                rerouting = false; lastReroute = System.currentTimeMillis()
                // a reroute round a closure that found no route at all: tried again at the next traffic check, not dropped for good
                closurePending?.let { closureRerouted.remove(it.id); closurePending = null }
            }
            // it keeps trying on its own while you are still off the route, so this is only a heads-up
            if (err != null) toast(if (err.contains("signal")) Lang.t("Neuberechnung: schwacher Empfang, wird weiter versucht…", "Rerouting: weak signal, still trying…") else Lang.t("Neuberechnung fehlgeschlagen ($err), neuer Versuch…", "Reroute failed ($err), trying again…"), 3500)
        }
    }

    fun endRoute() {
        stopTripShare(quiet = true)
        try { evTripDone() } catch (e: Exception) { }
        // (only a trip under way: cancelling a route choice leaves the other screen's trip alone)
        if (route != null && !previewing) try { RouteSync.ended(this) } catch (e: Exception) { }
        itineraryOpen = false
        cancelWaiting()
        map.stopFly()
        faster = null; resume = null; prefs.put(PreferenceKeys.RESUME, "{}")
        // the map strip that was saved by itself for this trip is only needed during it: gone with the trip
        try { MapboxOffline.removeAutoRoutes(ctx) } catch (e: Throwable) { }
        incMatches = emptyList(); incidentAhead = null; incAt = 0L; incAlerted.clear(); abAt = 0L; abRoads = ""; wwItems = emptyList(); wwSaid.clear(); wwAt = 0L
        routeCams = emptyList(); camsRoute = null; camsAt = 0L; camsCount = -1; hazardSig = 0; policeSig = 0; map.setHazards(emptyList())
        via = emptyList(); evPlanned = emptyList(); evAutoDest = null; evStops = emptyList(); evWarning = null; stopMode = false; destRain = null; routeWx = emptyList()
        routes = emptyList(); active = 0; dest = null; turn = null; eta = null; lastIdx = 0; previewing = false; routeFrac = 0f; onRoad = ""
        departMin = 0; departAtMs = 0L; navStartMs = 0L; navTripDest = null; evStartPending = false; evPreferRoute = null; pickedSlower = false
        fuelSuggestion = null; parkingNear = emptyList(); parkingFor = null
        turnArrowKey = -1; map.setTurnArrow(emptyList())
        clearLiveAlts(); liveSaid.clear(); radar = emptyList(); radarSig = 0; bendsFor = null; bends = emptyList()
        flushRoads()
        sessionAvoid.clear(); routeZones = emptyList(); avgInfo = null; avgIn = null; servicesAhead = emptyList(); barEvents = emptyList()
        rawServices = emptyList(); servicesFor = null; rawHz = emptyList(); rawAvg = emptyList(); rawJcts = emptyList(); jctsOn = null; jctsSig = 0; hzWinRoute = null; hzWinEnd = -1.0
        placed = null; placedSig = 0; avgAlerted.clear(); hzAlerted.clear(); zoneAlerted.clear(); stopsEditor = false
        map.setRoute(emptyList(), emptyList()); map.setDest(null); map.setVia(emptyList()); map.setTurnPulse(false); laneKey = null; map.setLanes(emptyList()); map.setLaneSign(null, emptyList()); laneSignFor = -1.0; bubbleLanes = null; map.setLaneBubble(null, emptyList()); map.setRouteShift(emptyList()); laneLatAt = null; laneShiftEnd = 0.0
        map.highlightDestination(null)
        stopSim()
        speechQ.clear(); playing = null; playingId = ""
        tts?.stop()
        // a stopped announcement never reports done: put the music back up here
        spoken = 0; unduck()
        glideRoute = null; glideOnRoute = false
        // back to the zoom you use when not on a route
        zoomOffset = try { savedFreeZoom() } catch (e: Exception) { 0.0 }
        refreshRoad()
        map.setTilt(if (tilt3d) (chosenTilt() ?: 55.0) else 0.0)
    }

    // ---- camera controls ----
    /**
     * Back to following the car. With no route, the zoom you last set with + and - comes back (it's remembered between drives,
     * separately for the phone / head unit and Android Auto); pressing re-centre again while already following you puts the zoom
     * back to automatic. On a route the zoom is always automatic.
     */
    fun recenter(byUser: Boolean = false) {
        val wasFollowing = following
        following = true
        val resetZoom = route != null || (byUser && wasFollowing)
        zoomOffset = if (resetZoom) 0.0 else savedFreeZoom()
        if (route == null && resetZoom) rememberFreeZoom()
        val f = fix
        if (f != null) onPosition(f, true) else toast(ctx.getString(R.string.toast_013))
    }

    private var recentreToken = 0

    /** While navigating, snaps the map back to the car after you stop moving it for [ms]. */
    private fun scheduleRecentre(ms: Long) {
        if (route == null || previewing) return
        val t = ++recentreToken
        scope.launch { delay(ms); if (t == recentreToken && !following && route != null && !previewing) recenter() }
    }

    /** Shows the rest of the route on screen, then goes back to following the car after a few seconds. */
    /** "Andere Routen" on the trip card (or in the car's settings): the grey lines with +/- minutes on or off. */
    fun toggleOtherRoutes() { prefs.put(PreferenceKeys.LIVE_ALTERNATIVES, !prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)); otherRoutesChanged() }
    fun otherRoutesChanged() { if (!prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) clearLiveAlts() }

    fun overview() {
        val r = route ?: return
        val pts = listOfNotNull(fix?.pos) + r.pts.drop(lastIdx.coerceIn(0, max(0, r.pts.size - 2))).filterIndexed { n, _ -> n % 6 == 0 } + r.pts.last()
        following = false
        // on the car the route fills the screen: there is no panel at the bottom to leave room for, so it is zoomed in further
        map.fit(pts, panel = !isCar)
        scheduleRecentre(10_000)
    }

    fun toggleNorthUp() {
        northUp = !northUp
        if (isCar) prefs.put(PreferenceKeys.NORTHUP_CAR, northUp)
        if (northUp) map.resetNorth() else fix?.let { onPosition(it, true) }
    }

    /** The 3D tilt chosen for this screen (30 to 65 degrees), or null for automatic. Tall = the trip card sits at the bottom. */
    fun chosenTilt(): Double? {
        val key = if (isCar) PreferenceKeys.TILT_CAR else if (bottomCover > 0.0) PreferenceKeys.TILT_TALL else PreferenceKeys.TILT_WIDE
        return prefs.i(key, 0).takeIf { it in 30..65 }?.toDouble()
    }

    fun toggleTilt() {
        tilt3d = !tilt3d
        prefs.put(PreferenceKeys.VIEW_3D, tilt3d)
        refreshTilt()
        if (following) fix?.let { onPosition(it, true) }
    }

    /** Applies the tilt again after it was changed in the settings. */
    fun refreshTilt() {
        map.setTilt(if (tilt3d) (chosenTilt() ?: 55.0) else 0.0)
        if (following) fix?.let { onPosition(it, true) }
    }

    fun zoom(delta: Double) {
        if (following && fix != null) { zoomOffset = (zoomOffset + delta).coerceIn(-4.0, 3.0); rememberFreeZoom(); fix?.let { onPosition(it, true) } }
        else map.zoomBy(delta)
    }

    // ---- the zoom you set with + and - when not on a route, remembered for next time (phone / head unit and Android Auto apart) ----
    private val freeZoomKey: String get() = if (isCar) PreferenceKeys.FREE_ZOOM_CAR else PreferenceKeys.FREE_ZOOM
    private var freeZoomRestored = false
    /** Brings back the zoom saved for this screen as soon as the screen is up (Android Auto may connect long after the first fix). */
    fun restoreFreeZoomNow() { if (!freeZoomRestored) { freeZoomRestored = true; if (route == null) zoomOffset = try { savedFreeZoom() } catch (e: Exception) { 0.0 } } }

    private fun savedFreeZoom(): Double = (prefs.s(freeZoomKey, "0").toDoubleOrNull() ?: 0.0).coerceIn(-4.0, 3.0)

    /** Keeps the current + / - zoom for next time; only off a route (a route's zoom is automatic and starts afresh). */
    private fun rememberFreeZoom() {
        if (route != null) return
        try { prefs.put(freeZoomKey, String.format(Locale.US, "%.2f", zoomOffset)) } catch (e: Exception) { }
    }


    // ---- speed limit, speed cameras (real GPS + OpenStreetMap) ----
    @Volatile private var limitBusy = false
    @Volatile private var ttAt = 0L
    @Volatile private var ttPos: LL? = null
    @Volatile private var ttWay = -1L
    @Volatile private var ttLim: TomTomRest.RoadLimit? = null
    private var routeLimitAt = 0L
    private var pendLimit = -2.0
    private var pendHits = 0

    /** The route's own limit where you are now (m/s), while navigating; it votes with the other sources. */
    @Volatile private var routeLimitNow: Double? = null
    /** The next different posted limit on the route ahead (m/s) and how far off it is, for the small sign beside the speed sign. */
    var nextLimitMs by mutableStateOf<Double?>(null)
    var nextLimitM by mutableStateOf(0.0)
    /** The route's limit that the other sources outvoted, so it isn't put back on the sign with every GPS fix. */
    @Volatile private var routeOverruled: Double? = null

    /** True while the posted limits that came with the route are being shown (updated with every GPS fix). */
    private fun routeLimitLive() = route != null && !previewing && System.currentTimeMillis() - routeLimitAt < 4000L

    /**
     * The limit of the road at [p], asked of every source at once and decided by a weighted vote (see [SpeedVote]): OpenStreetMap
     * (which also gives the road name), HERE and TomTom with a key, Mapbox with a token, the route's own limits while navigating
     * ([routeMs]), and the last speed sign passed as recognised on Mapillary's street photos. Each map source is asked again on a
     * new stretch of road, otherwise only every 15 to 20 seconds.
     */
    private val mbTrail = ArrayList<LL>()
    @Volatile private var mbAt = 0L
    @Volatile private var mbPos: LL? = null
    @Volatile private var mbWay = 0L
    @Volatile private var mbLim: Double? = null
    @Volatile private var hereAt = 0L
    @Volatile private var herePos: LL? = null
    @Volatile private var hereWay = -1L
    @Volatile private var hereLim: Double? = null

    /** The last speed sign passed (Mapillary), with the road it was on: it counts until the road changes, or for 10 minutes. */
    private class SeenSign(val sign: MlySign, val wayId: Long, val road: String, val pos: LL, val at: Long)
    @Volatile private var seenSign: SeenSign? = null

    /** The sign from Mapillary that applies at [p] now, or null (none passed, or the road has changed since). */
    private fun signNow(signs: List<MlySign>?, p: LL, hd: Double, osm: Overpass.Limit?): MlySign? {
        val now = System.currentTimeMillis()
        val way = osm?.wayId ?: 0L
        val road = osm?.road.orEmpty()
        if (signs != null) Mapillary.passed(signs, p, hd)?.let { s ->
            if (seenSign?.sign != s) seenSign = SeenSign(s, way, road, p, now)
        }
        val ss = seenSign ?: return null
        val sameRoad = (way != 0L && way == ss.wayId) || (road.isNotBlank() && road == ss.road && Geo.dist(ss.pos, p) < 2500.0) ||
            (way == 0L && road.isBlank() && Geo.dist(ss.pos, p) < 400.0)
        if (!sameRoad || now - ss.at > 600_000L) { seenSign = null; return null }
        // an "end of limit" sign: the limit it ended no longer counts, and it says nothing itself
        return ss.sign.takeIf { !it.end }
    }

    private suspend fun lookUpLimit(p: LL, hd: Double, routeMs: Double?): Pair<Overpass.Limit?, SpeedVote.Result?> {
        // the country you're in (from where you are), not the head unit's language setting, which is often US English anywhere
        val cc = if (Overpass.countryAt(p) == "GB") "GB" else detectedCountry.ifBlank { Overpass.countryAt(p) }
        val osm = Overpass.speedLimit(p, hd, cc)
        // your last few positions, for HERE and Mapbox to match to their roads
        synchronized(mbTrail) {
            if (mbTrail.isEmpty() || Geo.dist(mbTrail.last(), p) > 15.0) { mbTrail.add(p); while (mbTrail.size > 5) mbTrail.removeAt(0) }
            if (Geo.dist(mbTrail.first(), p) > 600.0) { mbTrail.clear(); mbTrail.add(p) }
        }
        if (!online()) return osm to null
        // a limit for certain times from OpenStreetMap ("30 @ 22:00-06:00"): the other sources only know the plain one
        if (osm != null && osm.timed) return osm to null
        val way = osm?.wayId ?: 0L
        val trail = synchronized(mbTrail) { mbTrail.toList() }
        val hk = prefs.s(PreferenceKeys.HERE_KEY, "").trim()
        val mk = prefs.s(PreferenceKeys.MAPILLARY_KEY, "").trim()
        val mph = cc == "GB" || cc == "US"
        // every source at once
        val got = kotlinx.coroutines.coroutineScope {
            val hereJ = async(Dispatchers.IO) {
                if (hk.isEmpty()) return@async null
                val now = System.currentTimeMillis()
                val stale = now - hereAt > 20_000L || (herePos?.let { Geo.dist(it, p) > 250.0 } ?: true)
                // a new stretch of road or an old answer, at most once every 4 seconds (a long way inside HERE's free allowance)
                if ((way != hereWay || stale) && now - hereAt > 4000L && trail.size >= 2) {
                    hereAt = now; herePos = p; hereWay = way
                    hereLim = try { HereRest.speedLimitNoted(hk, trail) } catch (e: Exception) { null }
                }
                if (osm == null || hereWay == way) hereLim else null
            }
            val ttJ = async(Dispatchers.IO) {
                if (!hasKey || ttDown()) return@async null
                val now = System.currentTimeMillis()
                val stale = now - ttAt > 15_000L || (ttPos?.let { Geo.dist(it, p) > 200.0 } ?: true)
                // a new stretch of road (OpenStreetMap splits roads where the limit changes) or an old answer, at most every 2.5 s
                if ((way != ttWay || stale) && now - ttAt > 2500L) {
                    ttAt = now; ttPos = p; ttWay = way
                    ttLim = try { TomTomRest.speedLimit(apiKey, p, hd) } catch (e: Exception) { null }
                }
                if (osm == null || ttWay == way) ttLim else null
            }
            val mbJ = async(Dispatchers.IO) {
                if (!hasMapbox) return@async null
                val now = System.currentTimeMillis()
                val stale = now - mbAt > 20_000L || (mbPos?.let { Geo.dist(it, p) > 250.0 } ?: true)
                if ((way != mbWay || stale) && now - mbAt > 4000L && trail.size >= 2) {
                    mbAt = now; mbPos = p; mbWay = way
                    mbLim = try { MapboxRest.speedLimit(mbToken, trail) } catch (e: Exception) { null }
                }
                if (osm == null || mbWay == way) mbLim else null
            }
            val mlyJ = async(Dispatchers.IO) { if (mk.isEmpty()) null else try { Mapillary.signsAround(mk, p, mph) } catch (e: Exception) { null } }
            // limits other drivers reported (Horizon's reports server)
            val usJ = async(Dispatchers.IO) { try { SpeedReports.around(prefs, p) } catch (e: Exception) { emptyList() } }
            // roadworks on a German motorway with a posted limit (Autobahn GmbH)
            val abJ = async(Dispatchers.IO) {
                val ab = if (cc == "DE" && osm != null) (listOf(osm.road) + osm.names).firstNotNullOfOrNull { AutobahnRest.roadOf(it) } else null
                if (ab == null) null else try { AutobahnRest.limitAt(AutobahnRest.roadworkLimits(ab), p, hd, System.currentTimeMillis()) } catch (e: Exception) { null }
            }
            // roadworks on a German Autobahn with no posted limit: the usual 100 before them and 80 in them
            val abGuessJ = async(Dispatchers.IO) {
                val ab = if (cc == "DE" && osm != null) (listOf(osm.road) + osm.names).firstNotNullOfOrNull { AutobahnRest.roadOf(it) } else null
                if (ab == null) null else try { AutobahnRest.defaultLimitAt(AutobahnRest.roadworkZones(ab), p, hd, System.currentTimeMillis()) } catch (e: Exception) { null }
            }
            // Dutch roads: Rijkswaterstaat's own posted limits (WEGGEG, CC0)
            val rwsJ = async(Dispatchers.IO) { if (cc == "NL") RwsSpeedLimits.limitAt(p, hd) else null }
            Quad(hereJ.await(), ttJ.await(), mbJ.await(), mlyJ.await(), usJ.await(), abJ.await(), rwsJ.await(), abGuessJ.await())
        }
        val (hv, tt, mb, signs, users, roadworks, rws, rwGuess) = got
        val votes = ArrayList<SpeedVote.Vote>()
        val osmNone = osm != null && osm.noLimit && !osm.estimated
        if (osm != null) {
            // how up to date OpenStreetMap's limit is counts a little: changed or checked in the last six months 10% more, over three years
            // ago 30% less (unknown: unchanged). The other services give no date for their limits.
            val ageK = if (osm.refDays <= 0) 1.0 else {
                val years = (System.currentTimeMillis() / 86_400_000L - 10957L - osm.refDays) / 365.25
                if (years < 0.5) 1.1 else if (years > 3.0) 0.7 else 1.0
            }
            if (osmNone) votes += SpeedVote.Vote("OSM", SpeedVote.NONE, SpeedVote.W_OSM * ageK)
            else if (osm.ms > 0.0) votes += SpeedVote.Vote("OSM", osm.ms, if (osm.estimated) SpeedVote.W_GUESS else SpeedVote.W_OSM * ageK, osm.estimated)
        }
        // OpenStreetMap knows a time rule here that doesn't apply now ("30 @ (22:00-06:00)" in the afternoon): another service
        // giving exactly that value most likely has the rule, not the limit for now, so it counts like a guess
        fun cw(v: Double, full: Double) = if (osm != null && !osm.estimated && osm.ms > 0.0 && abs(v - osm.ms) > 0.6 && osm.idle.any { abs(it - v) < 0.6 }) SpeedVote.W_GUESS else full
        if (hv != null) votes += SpeedVote.Vote("HERE", if (hv <= 0.0) SpeedVote.NONE else hv, if (hv <= 0.0) SpeedVote.W_HERE else cw(hv, SpeedVote.W_HERE))
        // HERE answered for this very road without a number, and OpenStreetMap has no posted limit here: on an Autobahn or
        // Schnellstraße that means no general limit, not the last sign seen (a failed request doesn't count)
        else if (hk.isNotEmpty() && hereWay == way && HereRest.lastError == null && osm != null && osm.major && !(osm.ms > 0.0 && !osm.estimated))
            votes += SpeedVote.Vote("HERE", SpeedVote.NONE, SpeedVote.W_HERE)
        // TomTom has no way to say "no limit": where OpenStreetMap says so outright, an answer of its from the stretch before doesn't count
        if (tt != null && !osmNone) votes += SpeedVote.Vote("TomTom", tt.ms, cw(tt.ms, SpeedVote.W_TOMTOM))
        // Mapbox's limits are largely OpenStreetMap's: where it says the same as OpenStreetMap, that is one source, counted once
        // (both at full weight made an OSM-based limit count twice as much as a sign). Where they differ, both still count.
        val mbSameAsOsm = mb != null && osm != null && !osm.estimated &&
            ((osmNone && mb <= 0.0) || (!osmNone && osm.ms > 0.0 && mb > 0.0 && abs(mb - osm.ms) < 0.6))
        if (mb != null && !mbSameAsOsm) votes += SpeedVote.Vote("Mapbox", if (mb <= 0.0) SpeedVote.NONE else mb, if (mb <= 0.0) SpeedVote.W_MAPBOX else cw(mb, SpeedVote.W_MAPBOX))
        if (rws != null) votes += SpeedVote.Vote("RWS", rws, cw(rws, SpeedVote.W_RWS))
        // the route's limit is its service's own data: where that service already answered for this road it isn't counted twice
        // (TomTom's route and TomTom's road lookup both voting 30 outweighed OpenStreetMap and Mapbox saying 50)
        val rsrc = route?.source.orEmpty()
        val sameAsRoute = (rsrc == "tomtom" && tt != null && !osmNone) || (rsrc == "mapbox" && mb != null)
        if (routeMs != null && routeMs > 0.0 && !sameAsRoute) votes += SpeedVote.Vote(Lang.t("Route", "Route"), routeMs, cw(routeMs, SpeedVote.W_ROUTE))
        signNow(signs, p, hd, osm)?.let { s ->
            val w = SpeedVote.signWeight(if (s.seenAt > 0) System.currentTimeMillis() - s.seenAt else 0L)
            if (w > 0.0) votes += SpeedVote.Vote(Lang.t("Schild", "Sign"), s.ms, w)
        }
        if (roadworks != null) votes += SpeedVote.Vote(Lang.t("Baustelle", "Roadworks"), roadworks, SpeedVote.W_ROADWORKS)
        else if (rwGuess != null) {
            // roadworks of the Autobahn GmbH with no posted limit: usually 100 before them and 80 in them. The maps don't know such temporary
            // limits (they say 120 or none), so what they say above that doesn't count; a sign, the Autobahn GmbH's own limit and other
            // drivers' reports still do
            val mapSources = setOf("OSM", "HERE", "TomTom", "Mapbox", "RWS", Lang.t("Route", "Route"))
            votes.removeAll { it.source in mapSources && (it.ms == SpeedVote.NONE || it.ms > rwGuess + 0.5) }
            votes += SpeedVote.Vote(Lang.t("Baustelle (üblich)", "Roadworks (usual)"), rwGuess, SpeedVote.W_ROADWORKS)
        }
        votes += SpeedReports.votes(users, p, hd, System.currentTimeMillis())
        val res = SpeedVote.decide(votes, imperial) ?: return osm to null
        android.util.Log.w("SpeedVote", "${res.details} -> ${SpeedVote.label(res.ms, imperial)}${if (res.disputed) " (uneinig)" else ""}")
        // only OpenStreetMap for it: its own sureness (how closely the road was matched); any other source is sure
        val sure = if (res.winners.all { it == "OSM" }) osm?.sure ?: false else true
        val lim = if (res.ms == SpeedVote.NONE) Overpass.Limit(-1.0, false, osm?.road ?: "", way, osm?.names ?: emptyList(), sure, noLimit = true, major = osm?.major ?: false)
            else Overpass.Limit(res.ms, res.estimated, osm?.road ?: "", way, osm?.names ?: emptyList(), sure, major = osm?.major ?: false)
        return lim to res
    }

    private data class Quad(val here: Double?, val tt: TomTomRest.RoadLimit?, val mb: Double?, val signs: List<MlySign>?,
                            val users: List<SpeedReport>, val roadworks: Double?, val rws: Double?, val rwGuess: Double?)

    /**
     * Your report of the limit where you are now (tap on the sign, or Melden in the car): [kmh] the limit, 0 for no limit, or
     * null for "the sign is right" (the value shown). Shared with other drivers and counted in the vote at once.
     */
    fun reportLimit(kmh: Int?) {
        val f = fix ?: run { toast(Lang.t("Warte auf GPS", "Waiting for GPS"), 2500); return }
        val v = kmh ?: when {
            speedLimit != null -> Math.round(speedLimit!! * 3.6).toInt()
            noLimit -> 0
            else -> { toast(Lang.t("Kein Tempolimit angezeigt", "No limit shown"), 2500); return }
        }
        val hd = if (f.speed > 2.0) f.heading else -1.0
        val pos = (if (glideOnRoute) gPos else mPos)?.takeIf { Geo.dist(it, f.pos) < 15.0 } ?: f.pos
        scope.launch {
            val ok = withContext(Dispatchers.IO) { SpeedReports.send(prefs, pos, hd, v) }
            val what = if (v == 0) Lang.t("kein Limit", "no limit") else "$v km/h"
            toast(if (ok) Lang.t("Danke! $what gemeldet", "Thanks! $what reported") else Lang.t("$what gespeichert, wird mitgezählt (Server nicht erreicht)", "$what saved and counted (server not reached)"), 3000)
            // look again at once, so your report shows
            limitAt = 0L; limitPos = null
        }
    }

    /**
     * Shows a looked-up limit. A guess from the road type is only shown when "Show estimated limits" is on (a blank sign is better
     * than a confidently wrong one). A change only shows straight away when the road was matched closely; otherwise it has to be
     * seen twice, so passing a side road at a junction doesn't flash its limit.
     */
    private fun showLimit(r: Overpass.Limit, at: LL) {
        val showEst = prefs.b(PreferenceKeys.LIMITEST, false)
        val v = if (r.ms <= 0.0 || (r.estimated && !showEst)) -1.0 else r.ms
        // no general limit (an unrestricted Autobahn) counts as its own value (-3), apart from no sign at all (-1)
        // no limit is tagged on a motorway or trunk road right after a posted one: the posted limit has ended (a Schnellstraße)
        val none = v <= 0.0 && (r.noLimit || (r.major && speedLimit != null && !limitEstimated))
        val key = if (none) -3.0 else v
        val cur = speedLimit ?: if (noLimit) -3.0 else -1.0
        if (abs(key - cur) > 0.3 || (v > 0 && r.estimated != limitEstimated)) {
            if (!r.sure && (speedLimit != null || noLimit)) {
                if (abs(key - pendLimit) < 0.3) pendHits++ else { pendLimit = key; pendHits = 1 }
                if (pendHits < 2) return
            }
            pendHits = 0; pendLimit = -2.0
            if (v <= 0.0) {
                // the end of a posted limit is announced like a change (when limit announcements are on)
                if (none && speedLimit != null && !limitEstimated && prefs.b(PreferenceKeys.LIMITCHANGE, false))
                    speak(Lang.t("Tempolimit aufgehoben", "Speed limit lifted"), true)
                speedLimit = null; limitEstimated = false; noLimit = none
            }
            else { speedLimit = v; limitEstimated = r.estimated; noLimit = false }
        }
        if (v > 0.0) checkLimitChange(v, r.estimated)
        limitOkAt = System.currentTimeMillis(); limitOkPos = at
    }

    // ---- "Ankunftszeit teilen": a live link with your position, arrival time and distance left ----

    /** The link while sharing (the options show "Teilen beenden" then), else null. */
    var tripShareUrl by mutableStateOf<String?>(null); private set
    private var tripLink: TripShare.Link? = null
    private var tripSentAt = 0L
    private var tripBusy = false

    private fun etaMs(): Long = System.currentTimeMillis() + ((eta?.secs ?: 0.0) * 1000).toLong()

    /**
     * Makes the link (or reuses the running one) and hands it to the share menu. [fromCar]: Android Auto has no share menu, so a
     * notification on the phone opens it.
     */
    fun shareTrip(fromCar: Boolean = false) {
        tripShareUrl?.let { openShare(it, fromCar); return }
        if (route == null || previewing) { toast(Lang.t("Erst eine Route starten", "Start a route first"), 2500); return }
        val f = fix ?: run { toast(Lang.t("Warte auf GPS", "Waiting for GPS"), 2500); return }
        if (!SharedReports.configured(prefs)) { toast(Lang.t("Kein Server für Meldungen eingestellt", "No reports server set"), 3000); return }
        if (tripBusy) return
        tripBusy = true
        val name = prefs.s(PreferenceKeys.TRIP_NAME, "").trim()
        val rem = eta?.dist ?: 0.0
        scope.launch {
            try {
                val l = withContext(Dispatchers.IO) { try { TripShare.create(prefs, name, f.pos, f.heading, etaMs(), rem) } catch (e: Exception) { lastTripError = e.message; null } }
                if (l == null) { toast(Lang.t("Link konnte nicht erstellt werden", "Couldn't make the link") + (lastTripError?.let { " ($it)" } ?: ""), 3500); return@launch }
                if (route == null) { withContext(Dispatchers.IO) { try { TripShare.end(prefs, l) } catch (e: Exception) { } }; return@launch }
                tripLink = l; tripShareUrl = l.url; tripSentAt = System.currentTimeMillis()
                openShare(l.url, fromCar)
            } finally { tripBusy = false }
        }
    }
    private var lastTripError: String? = null

    private fun openShare(url: String, fromCar: Boolean) {
        val text = TripShare.message(eta?.arrive, url)
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_TEXT, text)
        val chooser = android.content.Intent.createChooser(send, Lang.t("Ankunftszeit teilen", "Share arrival time")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!fromCar) {
            try { ctx.startActivity(chooser); return } catch (e: Exception) { }
        }
        // in the car (or when the share menu couldn't open): a notification on the phone that opens it
        try {
            val nm = ctx.getSystemService(android.app.NotificationManager::class.java)
            if (nm.getNotificationChannel("tripshare") == null)
                nm.createNotificationChannel(android.app.NotificationChannel("tripshare", Lang.t("Ankunftszeit teilen", "Share arrival time"), android.app.NotificationManager.IMPORTANCE_HIGH))
            val pi = android.app.PendingIntent.getActivity(ctx, 7301, chooser, android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
            val n = android.app.Notification.Builder(ctx, "tripshare").setSmallIcon(R.drawable.ic_stat_nav)
                .setContentTitle(Lang.t("Link für deine Ankunftszeit ist bereit", "Your arrival time link is ready"))
                .setContentText(Lang.t("Tippen, um ihn zu teilen", "Tap to share it"))
                .setContentIntent(pi).setAutoCancel(true).build()
            nm.notify(7301, n)
            toast(Lang.t("Link erstellt: auf dem Handy teilen", "Link made: share it on your phone"), 3500)
        } catch (e: Exception) { toast(Lang.t("Link erstellt, Teilen-Menü nicht verfügbar", "Link made, no share menu"), 3500) }
    }

    /** Ends sharing: the page shows the trip as over straight away. */
    fun stopTripShare(quiet: Boolean = false) {
        val l = tripLink ?: return
        tripLink = null; tripShareUrl = null
        try { ctx.getSystemService(android.app.NotificationManager::class.java).cancel(7301) } catch (e: Exception) { }
        scope.launch(Dispatchers.IO) { try { TripShare.end(prefs, l) } catch (e: Exception) { } }
        if (!quiet) toast(Lang.t("Teilen beendet", "Stopped sharing"), 2500)
    }

    /** Every 30 s while sharing: your exact position, arrival time and the distance left. */
    private fun tripShareTick() {
        val l = tripLink ?: return
        val now = System.currentTimeMillis()
        if (now - tripSentAt < 30_000L || tripBusy || simMode != 0) return
        val f = fix ?: return
        tripSentAt = now
        val rem = eta?.dist ?: 0.0
        val at = etaMs()
        scope.launch(Dispatchers.IO) {
            try { TripShare.update(prefs, l, f.pos, f.heading, at, rem) }
            catch (e: TripShare.Gone) { withContext(Dispatchers.Main) { if (tripLink === l) { tripLink = null; tripShareUrl = null } } }
            catch (e: Exception) { }
        }
    }

    // ---- notices: official weather warnings (DWD) and the rescue lane ----

    /** A notice card under the turn card: [kind] "wx" (weather warning) or "lane" (rescue lane). */
    class Notice(val kind: String, val title: String, val text: String, val level: Int = 2, val action: String? = null)
    var notice by mutableStateOf<Notice?>(null)
    private fun showNotice(n: Notice, ms: Long) { notice = n; scope.launch { delay(ms); if (notice === n) notice = null } }
    fun dismissNotice() { notice = null }

    private var wxAt = 0L
    private var wxRoute: Route? = null
    private var wxBusy = false
    private val wxSaid = HashSet<String>()

    /**
     * The DWD's warnings (markant and worse) for the stretch you'll drive in the next hour, or where you are without a route: every
     * 10 minutes, and at once for a new route. Each warning is shown and announced once.
     */
    private fun checkWeatherWarnings(p: Fix) {
        if (!prefs.b(PreferenceKeys.DWD_WARN, true) || wxBusy || !online() || !DwdWarnings.inGermany(p.pos)) return
        val now = System.currentTimeMillis()
        val r = if (previewing) null else route
        if (now - wxAt < 600_000L && r === wxRoute) return
        wxAt = now; wxRoute = r; wxBusy = true
        // the route ahead you'll reach within the hour (from its own average speed), a point every few kilometres
        val line = ArrayList<LL>()
        if (r != null && r.len > 0 && r.time > 0) {
            val ahead = min(r.len - curProgress, min(r.len / r.time * 3600.0, 150_000.0))
            val step = max(2000.0, ahead / 45)
            var d = curProgress; var i = 0
            while (d <= curProgress + ahead && line.size < 50) {
                while (i < r.cum.size - 1 && r.cum[i + 1] < d) i++
                line += r.pts[i]; d += step
            }
        }
        if (line.isEmpty()) line += p.pos
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { DwdWarnings.along(line) } ?: return@launch
                val t = System.currentTimeMillis()
                // one warning covers many districts: the first one along the way is named
                fun firstAlong(w: DwdWarning): Int { val b = w.box ?: return line.size; return line.indexOfFirst { it.lat in b[0]..b[2] && it.lon in b[1]..b[3] }.let { if (it < 0) line.size else it } }
                val fresh = found.filter { DwdWarnings.current(it, t, 3_600_000L) && it.id !in wxSaid }
                    .groupBy { it.id }.map { (_, ws) -> ws.minByOrNull { firstAlong(it) }!! }
                    .sortedWith(compareByDescending<DwdWarning> { it.level }.thenBy { firstAlong(it) })
                for (w in fresh.take(2)) {
                    wxSaid += w.id
                    val txt = DwdWarnings.text(w, t)
                    showNotice(Notice("wx", txt.substringBefore(": "), txt.substringAfter(": "), w.level), 20_000L)
                    speak(txt, true)
                }
            } finally { wxBusy = false }
        }
    }

    /** On a motorway or Schnellstraße (from the last limit lookup), for the rescue lane notice. */
    @Volatile private var onMotorway = false
    private var slowSince = 0L
    private var laneShownAt = 0L
    private var fastSince = 0L

    /**
     * The rescue lane: on a motorway in a jam (reported on the route, by Autobahn GmbH or by other drivers) and slower than 30 km/h
     * for 15 seconds, a card and a short announcement, once per jam (again only after you've been moving freely for 2 minutes, or
     * after 20 minutes). Only where it's the law (Germany, Austria, Switzerland, Czechia, Slovakia, Slovenia).
     */
    private fun checkRescueLane(p: Fix) {
        if (!prefs.b(PreferenceKeys.RESCUE_LANE, true)) return
        val cc = detectedCountry.uppercase()
        if (cc !in setOf("DE", "AT", "CH", "CZ", "SK", "SI")) return
        val now = System.currentTimeMillis()
        if (p.speed > 16.7) { if (fastSince == 0L) fastSince = now } else fastSince = 0L
        // moving freely for 2 minutes: the jam is over, the next one gets its own notice
        if (laneShownAt > 0 && fastSince > 0 && now - fastSince > 120_000L) laneShownAt = 0L
        if (p.speed >= 8.3 || !onMotorway) { slowSince = 0L; return }
        if (slowSince == 0L) slowSince = now
        if (now - slowSince < 15_000L || (laneShownAt > 0 && now - laneShownAt < 20 * 60_000L)) return
        if (!jamHere(p.pos)) return
        laneShownAt = now
        showNotice(Notice("lane", Lang.t("Rettungsgasse bilden", "Form a rescue lane"),
            Lang.t("Ganz linke Spur nach links, alle anderen nach rechts", "Far left lane to the left, all others to the right")), 25_000L)
        speak(Lang.t("Stau. Rettungsgasse bilden: ganz links nach links, alle anderen nach rechts.", "Traffic jam. Form a rescue lane: far left lane to the left, all others to the right."), true)
    }

    /** A jam where you are: on the route's traffic, a jam incident within 2 km, or another driver's jam report within 1.5 km. */
    private fun jamHere(p: LL): Boolean {
        val r = route
        if (r != null && !previewing) {
            val prog = curProgress
            if (incMatches.any { (inc, off, _) -> (inc.cat.contains("jam", true) || inc.delaySec >= 60) && off in (prog - 1500)..(prog + 2000) }) return true
            if (r.sev.isNotEmpty()) {
                var i = 0
                while (i < r.cum.size - 1 && r.cum[i + 1] < prog) i++
                if ((i until min(r.sev.size, i + 30)).any { r.sev[it] >= 2 && r.cum[it] - prog < 1500 }) return true
            }
        }
        return SharedReports.items.any { it.kind == "jam" && Geo.dist(it.pos, p) < 1500.0 }
    }

    // ---- height warning (motorhome / caravan) ----

    data class DimensionLimit(val offset: Double, val type: String, val limitValue: Double, val vehicleValue: Double, val name: String)

    /** Dimension limits on the route ahead (offset, type, limit, vehicle value, name). */
    private var dimSpots: List<DimensionLimit> = emptyList()
    private var dimRoute: Route? = null
    private var dimUpTo = 0.0
    private var dimBusy = false
    private val dimSaid = HashSet<Long>()

    /**
     * With a motorhome or truck profile: bridges/underpasses/weight limits on the route (OpenStreetMap's maxheight, maxwidth,
     * maxweight, maxaxleweight), looked up 100 km at a time; one about 1.5 km ahead is shown and announced once.
     */
    private fun checkClearances(p: Fix) {
        val r = route ?: return
        if (previewing) return
        val v = Vehicle.active
        if (v.isCar || (v.heightM <= 0.0 && v.widthM <= 0.0 && v.weightKg <= 0 && v.axleWeightKg <= 0)) return
        val prog = curProgress
        if (r !== dimRoute) { dimRoute = r; dimSpots = emptyList(); dimUpTo = 0.0; dimSaid.clear() }
        if (!dimBusy && online() && prog + 30_000.0 > dimUpTo && dimUpTo < r.len) {
            dimBusy = true
            val from = max(prog, dimUpTo - 500.0); val to = min(r.len, from + 100_000.0)
            scope.launch {
                try {
                    val found = withContext(Dispatchers.IO) { Overpass.clearances(RouteExtras.line(r, from, to, 40.0, 2500)) }
                    if (found != null && route === r) {
                        val add = found.mapNotNull { c ->
                            val vWeightTons = v.weightKg / 1000.0
                            val vAxleWeightTons = v.axleWeightKg / 1000.0
                            val limits = listOfNotNull(
                                if (c.heightM > 0.0 && c.heightM < v.heightM + 0.1) "height" to c.heightM else null,
                                if (c.widthM > 0.0 && c.widthM < v.widthM + 0.05) "width" to c.widthM else null,
                                if (c.weightTons > 0.0 && c.weightTons < vWeightTons + 0.1) "weight" to c.weightTons else null,
                                if (c.axleWeightTons > 0.0 && c.axleWeightTons < vAxleWeightTons + 0.05) "axleweight" to c.axleWeightTons else null
                            )
                            limits.mapNotNull { (type, limit) ->
                                val pr = RouteExtras.project(r, c.pos, 25.0, from) ?: return@mapNotNull null
                                when (type) {
                                    "height" -> DimensionLimit(pr.first, "h", limit, v.heightM, c.name)
                                    "width" -> DimensionLimit(pr.first, "w", limit, v.widthM, c.name)
                                    "weight" -> DimensionLimit(pr.first, "wt", limit, vWeightTons, c.name)
                                    "axleweight" -> DimensionLimit(pr.first, "aw", limit, vAxleWeightTons, c.name)
                                    else -> null
                                }
                            }
                        }.flatten()
                        dimSpots = (dimSpots + add).distinctBy { Math.round(it.offset / 30.0) to it.type }.sortedBy { it.offset }
                        dimUpTo = to
                    }
                } finally { dimBusy = false }
            }
        }
        val next = dimSpots.firstOrNull { it.offset > prog - 20.0 } ?: return
        val dist = next.offset - prog
        val key = Math.round(next.offset / 30.0) * 1000 + next.type.hashCode()
        if (dist > 1500.0 || key in dimSaid) return
        dimSaid += key
        val d = Geo.fmtDist(dist, imperial)
        when (next.type) {
            "h" -> {
                val h = String.format(Locale.GERMANY, "%.1f", next.limitValue)
                showNotice(Notice("wx", Lang.t("Durchfahrtshöhe $h m in $d", "Height limit $h m in $d"),
                    Lang.t("Dein Fahrzeug ist ${String.format(Locale.GERMANY, "%.1f", next.vehicleValue)} m hoch", "Your vehicle is ${String.format(Locale.UK, "%.1f", next.vehicleValue)} m high") +
                        (if (next.name.isNotBlank()) " · ${next.name}" else ""), 3), 25_000L)
                speak(Lang.t("Achtung, Durchfahrtshöhe $h Meter in $d. Dein Fahrzeug ist zu hoch.", "Caution, height limit $h metres in $d. Your vehicle is too high."), true)
            }
            "w" -> {
                val w = String.format(Locale.GERMANY, "%.2f", next.limitValue)
                showNotice(Notice("wx", Lang.t("Fahrbahn $w m breit in $d", "Road width $w m in $d"),
                    Lang.t("Dein Fahrzeug ist ${String.format(Locale.GERMANY, "%.2f", next.vehicleValue)} m breit", "Your vehicle is ${String.format(Locale.UK, "%.2f", next.vehicleValue)} m wide") +
                        (if (next.name.isNotBlank()) " · ${next.name}" else ""), 3), 25_000L)
                speak(Lang.t("Achtung, Fahrbahn $w Meter breit in $d. Dein Fahrzeug könnte zu breit sein.", "Caution, road width $w metres in $d. Your vehicle may be too wide."), true)
            }
            "wt" -> {
                val wt = String.format(Locale.GERMANY, "%.1f", next.limitValue)
                showNotice(Notice("wx", Lang.t("Gewichtsbegrenzung $wt t in $d", "Weight limit $wt t in $d"),
                    Lang.t("Dein Fahrzeug wiegt ${String.format(Locale.GERMANY, "%.1f", next.vehicleValue)} t", "Your vehicle weighs ${String.format(Locale.UK, "%.1f", next.vehicleValue)} t") +
                        (if (next.name.isNotBlank()) " · ${next.name}" else ""), 3), 25_000L)
                speak(Lang.t("Achtung, Gewichtsbegrenzung $wt Tonnen in $d. Dein Fahrzeug ist zu schwer.", "Caution, weight limit $wt tonnes in $d. Your vehicle is too heavy."), true)
            }
            "aw" -> {
                val aw = String.format(Locale.GERMANY, "%.1f", next.limitValue)
                showNotice(Notice("wx", Lang.t("Achslastbegrenzung $aw t in $d", "Axle weight limit $aw t in $d"),
                    Lang.t("Deine Achslast ist ${String.format(Locale.GERMANY, "%.1f", next.vehicleValue)} t", "Your axle weight is ${String.format(Locale.UK, "%.1f", next.vehicleValue)} t") +
                        (if (next.name.isNotBlank()) " · ${next.name}" else ""), 3), 25_000L)
                speak(Lang.t("Achtung, Achslastbegrenzung $aw Tonnen in $d. Deine Achslast ist zu schwer.", "Caution, axle weight limit $aw tonnes in $d. Your axle weight is too heavy."), true)
            }
        }
    }

    // ---- camera warning ----

    data class CameraSpot(val offset: Double, val type: String, val ref: String, val pos: LL)

    private var camRoute: Route? = null
    private var camSpots: List<CameraSpot> = emptyList()
    private var camUpTo = 0.0
    private var camBusy = false
    private val camSaid = HashSet<Long>()

    /** Traffic and speed cameras on the route ahead. */
    private fun checkCameras(p: Fix) {
        val r = route ?: return
        if (previewing) return
        val prog = curProgress
        if (r !== camRoute) { camRoute = r; camSpots = emptyList(); camUpTo = 0.0; camSaid.clear(); map.setAlprCameras(emptyList()) }
        if (!camBusy && online() && prog + 50_000.0 > camUpTo && camUpTo < r.len) {
            camBusy = true
            val from = max(prog, camUpTo - 500.0); val to = min(r.len, from + 100_000.0)
            scope.launch {
                try {
                    val found = withContext(Dispatchers.IO) { Overpass.cameras(RouteExtras.line(r, from, to, 30.0, 2500)) }
                    if (found != null && route === r) {
                        val add = found.mapNotNull { cam ->
                            val pr = RouteExtras.project(r, cam.pos, 50.0, from) ?: return@mapNotNull null
                            CameraSpot(pr.first, cam.type, cam.ref, cam.pos)
                        }
                        camSpots = (camSpots + add).distinctBy { Math.round(it.offset / 100.0) }.sortedBy { it.offset }
                        camUpTo = to
                        map.setAlprCameras(camSpots.map { it.pos })
                    }
                } finally { camBusy = false }
            }
        }
        val next = camSpots.firstOrNull { it.offset > prog - 20.0 } ?: return
        val dist = next.offset - prog
        val key = Math.round(next.offset / 100.0)
        if (dist > 2000.0 || key in camSaid) return
        camSaid += key
        val d = Geo.fmtDist(dist, imperial)
        val type = if (next.type == "speed") Lang.t("Geschwindigkeitskamera", "Speed camera") else Lang.t("Verkehrskamera", "Traffic camera")
        showNotice(Notice("camera", Lang.t("$type in $d", "$type in $d"), next.ref.takeIf { it.isNotBlank() } ?: "", 2), 8_000L)
    }

    /** While navigating a TomTom route: the posted limit for exactly where you are on it, updated with every GPS fix. */
    private fun applyRouteLimit(r: Route, prog: Double, p: Fix) {
        val secs = r.limits
        if (secs.isEmpty()) { routeLimitNow = null; nextLimitMs = null; return }
        // a few metres ahead, to make up for the GPS being about a second behind
        val at = prog + min(max(p.speed, 0.0) * 0.8, 20.0)
        var lo = 0; var hi = secs.size - 1; var hit: LimitSec? = null
        var hitIdx = -1
        while (lo <= hi) {
            val m = (lo + hi) ushr 1
            val sc = secs[m]
            if (at < sc.off) hi = m - 1 else if (at >= sc.end) lo = m + 1 else { hit = sc; hitIdx = m; break }
        }
        // the next posted limit that differs from this one, along the route
        nextLimitMs = null
        if (hit != null) for (j in hitIdx + 1 until secs.size) if (secs[j].ms != hit.ms) { nextLimitMs = secs[j].ms; nextLimitM = secs[j].off - at; break }
        if (hit == null) { routeLimitNow = null; return }      // no posted limit known for this stretch: the map data takes over
        routeLimitNow = hit.ms
        routeLimitAt = System.currentTimeMillis()
        // the other sources outvoted this very value (a sign on the road, say): the vote's answer stays until the route's value changes
        val over = routeOverruled
        if (over != null) { if (SpeedVote.key(over) == SpeedVote.key(hit.ms)) return else routeOverruled = null }
        pendHits = 0; pendLimit = -2.0
        if (speedLimit != hit.ms || limitEstimated) { speedLimit = hit.ms; limitEstimated = false }
        if (noLimit) noLimit = false
        checkLimitChange(hit.ms, false)
        limitOkAt = routeLimitAt; limitOkPos = p.pos
    }

    /**
     * The speed cameras around the car (6 km), looked up as soon as there is a fix: at the start too, and also while the phone is
     * quiet with Android Auto connected, where the road check (updateRoadInfo) waits. A failed lookup is tried again after about
     * half a minute.
     */
    private fun lookCamerasAround(p: Fix) {
        if (!camerasOnMap()) return
        val now = System.currentTimeMillis()
        val cp = camPos
        if (now - camAt <= 120000 || (cp != null && Geo.dist(cp, p.pos) <= 2500)) return
        camAt = now; camPos = p.pos
        scope.launch {
            var src = Lang.t("Offline-Karte", "offline map")
            val found = withContext(Dispatchers.IO) { localCameras(p.pos, 6000.0) ?: Overpass.cameras(p.pos, 6000).also { src = "OpenStreetMap" } }
            val at = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
            // a failed lookup (weak signal, busy server) keeps the ones already shown and tries again in about half a minute
            if (found == null) { camAt = System.currentTimeMillis() - 90_000L; camPos = null; if (aroundCams.isEmpty()) showNoCamData(); camNote = Lang.t("$at: Suche fehlgeschlagen (${Overpass.lastNote.take(60)})", "$at: lookup failed (${Overpass.lastNote.take(60)})") }
            else { camsFound(); addAroundCams(found); mergeCams(); camNote = Lang.t("$at: ${found.size} Blitzer im Umkreis von 6 km ($src)", "$at: ${found.size} cameras within 6 km ($src)") }
        }
    }

    private var noCamJob: kotlinx.coroutines.Job? = null
    /** Shows the "no camera data" note for 5 seconds, then hides it again. */
    private fun showNoCamData() {
        // once: not again with each failed lookup, only after cameras were found and then lost
        if (noCamShown) return
        noCamShown = true
        camNoData = true
        noCamJob = scope.launch { delay(5000L); camNoData = false }
    }
    /** Set once the note has been shown, cleared when cameras are found again. */
    private var noCamShown = false
    private fun camsFound() { camNoData = false; noCamShown = false }

    private fun updateRoadInfo(p: Fix) {
        val now = System.currentTimeMillis()
        if (simMode == 0) {
            val dt = (now - lastSpdAt) / 1000.0
            if (lastSpdAt > 0 && dt in 0.3..2.5 && lastSpd > 8.0 && (lastSpd - p.speed) / dt > 3.2 && !brakeFlash) {
                brakeFlash = true
                scope.launch { delay(1400); brakeFlash = false }
            }
            lastSpd = p.speed; lastSpdAt = now
        }
        run {
            val lim = speedLimit
            val tolUnits = prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5").toIntOrNull() ?: 5
            val tol = tolUnits * (if (imperial) 0.44704 else 0.27778)
            // a motorhome's or trailer's own top speed counts where it's below the road's limit (Settings > Fahrzeug)
            val vmax = Vehicle.active.maxMs
            val overVehicle = vmax != null && (lim == null || vmax < lim) && p.speed > vmax + tol
            val over = prefs.b(PreferenceKeys.SPEEDWARN, false) && p.speed > 4.0 && ((lim != null && !limitEstimated && p.speed > lim + tol) || overVehicle)
            overLimit = over
            if (over && soundMode != 2 && now - lastBeep > 9000) {
                lastBeep = now
                chime(android.media.ToneGenerator.TONE_PROP_BEEP2)
            }
        }
        val lp = limitPos
        val moving = p.speed > 0.5
        // a quick local check every few seconds (the roads around you are downloaded a few hundred metres at a time, so most
        // checks need no network): the sign changes within a few seconds of passing the real one
        if (shadow()) return
        if (!limitBusy && now - limitAt > (if (moving) 3000L else 12000L) && (lp == null || Geo.dist(lp, p.pos) > 25) && (moving || lp == null)) {
            limitAt = now; limitPos = p.pos; limitBusy = true
            // where the car really is: on the route line while navigating, or snapped to the road the map shows
            val look = (if (glideOnRoute) gPos else mPos)?.takeIf { Geo.dist(it, p.pos) < 15.0 } ?: p.pos
            val routeMs = if (routeLimitLive()) routeLimitNow else null
            scope.launch {
                try {
                    val hd = if (p.speed > 2.0) p.heading else -1.0
                    val (r, vote) = withContext(Dispatchers.IO) { lookUpLimit(look, hd, routeMs) }
                    limitDetails = vote?.details.orEmpty(); limitDisputed = vote?.disputed == true
                    // while navigating, the vote's answer replaces the route's own only where they differ
                    val overrule = routeMs != null && vote != null && SpeedVote.key(vote.ms) != SpeedVote.key(routeMs)
                    routeOverruled = if (overrule) routeMs else null
                    if (r != null) {
                        onMotorway = r.major || AutobahnRest.roadOf(r.road) != null
                        if (r.wayId != 0L || r.road.isNotBlank()) noteRoad(r.road, hd); limitFails = 0
                        if (!routeLimitLive() || overrule) showLimit(r, p.pos)
                    } else if (Overpass.lastError == null) {
                        // looked fine, but you're not on a mapped road (a car park, a new estate): no road name or limit rather than the
                        // last road's
                        noteRoad("", hd)
                        if (!routeLimitLive()) showLimit(Overpass.Limit(-1.0, true), p.pos)
                    } else {
                        // keep showing the last known limit through a failed lookup; only drop it once it is clearly out of date (10 minutes or 3 km)
                        val okPos = limitOkPos
                        if (!routeLimitLive() && (System.currentTimeMillis() - limitOkAt > 600_000 || (okPos != null && Geo.dist(okPos, p.pos) > 3000))) { speedLimit = null; limitEstimated = false; noLimit = false }
                        // the lookup itself failed: try again soon instead of waiting until the car has moved (and let an old road name
                        // drop once it's out of date)
                        refreshRoad()
                        limitFails++
                        limitPos = null
                        limitAt = System.currentTimeMillis() + minOf(limitFails * 8000L, 60_000L)
                    }
                } finally { limitBusy = false }
            }
        }
        try { checkPolice(p) } catch (e: Exception) { }
        try { maybeRefreshChargers() } catch (e: Exception) { }
        try { checkWrongWay(p) } catch (e: Exception) { }
        noteCameraBan()
        if (!camerasOnMap()) { cameraDist = null; if (cameras.isNotEmpty()) { cameras = emptyList(); map.setCameras(emptyList()) }; camPos = null; return }
        // the cameras on the map follow the route coming and going, and the ones you've passed drop off
        if (now - camsMergedAt > 4000L) { camsMergedAt = now; mergeCams() }
        var best: LL? = null
        var bestD = 1e9
        // warned in good time: at least 500 m, or about 18 seconds ahead at speed
        val warnAt = max(500.0, p.speed * 18.0)
        val rc = routeCams
        if (camerasOn() && navigatingNow() && onRouteNow && rc.isNotEmpty()) {
            // on a route: the cameras along it, by distance along the road (works round bends, where "straight ahead" doesn't)
            for ((c, off) in rc) {
                val ahead = off - curProg
                if (ahead < -15.0 || ahead > warnAt || ahead >= bestD) continue
                if (!camFacesUs(c, p.heading)) continue
                best = c; bestD = max(0.0, ahead)
            }
        }
        // off the route (or driving without one): a camera counts when it lies ahead, within 35 m to the side of the line you drive along
        val onRouteCams = camerasOn() && navigatingNow() && onRouteNow && rc.isNotEmpty()
        if (best == null && !onRouteCams && p.heading >= 0 && p.speed > 2.0) {
            for (c in cameras) {
                val d = Geo.dist(p.pos, c)
                if (d < 1.0 || d > warnAt + 40.0) continue
                val ang = angDiff(Geo.bearing(p.pos, c), p.heading)
                val along = d * Math.cos(Math.toRadians(ang))
                val side = Math.abs(d * Math.sin(Math.toRadians(ang)))
                if (along < 0.0 || along > warnAt || side > 35.0) continue
                if (!camFacesUs(c, p.heading)) continue
                if (along < bestD) { best = c; bestD = along }
            }
        }
        cameraDist = if (best != null) bestD.toInt() else null
        if (best != null) {
            if (camAlerted.size > 100) camAlerted.clear()
            if (camAlerted.add(best)) { chime(android.media.ToneGenerator.TONE_PROP_PROMPT); raiseAlert("camera", Lang.t("Blitzer voraus", "Speed camera ahead")); speak(Lang.t("Blitzer voraus", "Speed camera ahead"), true) }
        }
    }

    private fun trackParking(p: Fix) {
        if (simMode != 0) return
        // the battery estimate's drive is counted in evTrack, from every GPS fix (heat, wind, hills, the learned correction). It used
        // to be counted here again at the base use only, so each stretch was taken twice. Standing still adds nothing.
        // charging: standing at a known charger, and a new reading higher than the last one. It ends when the car moves off.
        if (isEv) {
            evEnsureLoaded()
            val readAt = EvWidget.readAt(prefs)
            if (readAt != evChargeReadAt) {
                evChargeReadAt = readAt
                val pct = evNowPct()
                if (pct != null) {
                    evChargingNow = evChargeLastPct >= 0 && pct > evChargeLastPct && p.speed < 1.0 && chgAll.any { Geo.dist(it.pos, p.pos) <= 50.0 }
                    if (evChargingNow) evChargingAt = System.currentTimeMillis()
                    evChargeLastPct = pct.toDouble()
                }
                evSave()
            }
            if (p.speed > 3.0 && evChargingNow) { evChargingNow = false; evSave() }
        }
        // a reading that is the same as the last one, after the car has used more than 1 % since, is the widget's old figure: not believed
        EvWidget.staleCheck = { evSinceRead / evBatteryKwh() * 100.0 >= 1.0 }
        val now = System.currentTimeMillis()
        // ---- trip recording ----
        if (tripOn) {
            val last = tripLast
            if (last != null) {
                val d = Geo.dist(last, p.pos)
                if (d in 2.0..250.0) tripDistance += d
            }
            tripLast = p.pos
            if (p.speed > 1.0) tripMoveSec += 1
            if (p.speed > tripTop) tripTop = p.speed
            val lp = tripPts.lastOrNull()
            if ((lp == null || Geo.dist(lp, p.pos) > 80) && tripPts.size < 3000) tripPts.add(p.pos)
            if (tripMoveSec >= breakNext && prefs.b(PreferenceKeys.BREAKREMIND, false)) {
                val hrs = breakNext / 3600
                breakNext += 7200
                raiseAlert("break", Lang.t("Du fährst seit $hrs Stunden. Zeit für eine Pause?", "You have been driving for $hrs hours. Time for a break?"), 8000)
                speak(Lang.t("Sie fahren seit $hrs Stunden. Bitte machen Sie eine Pause.", "You have been driving for $hrs hours. Consider taking a break."), true)
                // with the reminder on: on a route the rest areas along it are offered (a tap adds one as a stop); otherwise fuel nearby
                if (route != null && !previewing) loadNearby("services", false) else if (route == null) loadNearby("fuel", false)
            }
        } else if (p.speed > 3.0) {
            tripOn = true; tripT0 = now; tripDistance = 0.0; tripMoveSec = 0; tripTop = p.speed; tripLast = p.pos
            breakNext = 7200L; tripPts = arrayListOf(p.pos)
        }
        // ---- end of trip (stopped for two minutes) ----
        if (p.speed > 4.0) { drove = true; stillSince = 0L }
        else if (p.speed < 0.8 && drove) {
            if (stillSince == 0L) stillSince = now
            else if (now - stillSince > 120_000) {
                if (tripOn && tripDistance > 300) {
                    trips.add(Trip(tripT0, stillSince, tripDistance, tripMoveSec, tripTop, tripDest))
                }
                tripOn = false
                drove = false; stillSince = 0L
            }
        }
        if (dest != null) tripDest = dest?.name.orEmpty()
    }

    /** Finds [q] and starts routing there (used for calendar appointments). */
    fun routeToText(q: String, then: () -> Unit) {
        if (!online()) { toast(ctx.getString(R.string.toast_021), 4000); return }
        scope.launch {
            try {
                val p = withContext(Dispatchers.IO) { netGeocode(countryCode(), q, fix?.pos ?: map.center()) }.firstOrNull()
                if (p == null) toast(str(R.string.toast_066, q), 3500) else { then(); startRoute(p) }
            } catch (e: Exception) { toast(str(R.string.rest_438, e.message.orEmpty()), 4000) }
        }
    }

    /** Finds a place by text and returns it with the driving time in minutes from here. */
    suspend fun travelTo(q: String): Pair<Place, Int>? = withContext(Dispatchers.IO) {
        try {
            val from = fix?.pos ?: return@withContext null
            val pl = netGeocode(countryCode(), q, from).firstOrNull() ?: return@withContext null
            val r = netRoutes(from, pl.pos, false, headingDeg = driveHeading(from)).firstOrNull() ?: return@withContext null
            Pair(pl, Math.round(r.time / 60.0).toInt())
        } catch (e: Exception) { null }
    }

    // ---- nearby fuel / charging / parking ----
    fun clearNearby() { nearby = emptyList(); nearbyKind = null; nearbyBusy = false; stopMode = false }

    private fun distToRoute(r: Route, p: LL): Double {
        var best = 1e9
        var i = lastIdx.coerceIn(0, max(0, r.pts.size - 1))
        while (i < r.pts.size) { best = min(best, Geo.dist(r.pts[i], p)); i += 3 }
        return best
    }

    /**
     * Finds fuel, chargers, parking, food, coffee, toilets or services. With a route, it looks along the road ahead
     * (sorted by how far ahead, with roughly how long the detour adds); otherwise around you.
     * Fuel uses live Fuel Finder prices when you have added your Fuel Finder details in Settings.
     */
    /**
     * One tap from the stops box: finds [kind] along the route ahead and adds the best one as a stop straight away (fuel: the
     * cheapest once a little detour time is counted; everything else: the shortest detour), so nothing needs typing while driving.
     */
    fun addBestStop(kind: String) { loadNearby(kind, false, pickBest = true) }

    /** The places only a motorhome or a car with a caravan looks for (shown with that profile in Settings > Fahrzeug). */
    val CAMPER_KINDS = setOf("camp", "dump", "lpg")
    val camperProfile: Boolean get() = !Vehicle.from(prefs).isCar

    fun loadNearby(kind: String, nearDest: Boolean, pickBest: Boolean = false) {
        val centre = (if (nearDest) dest?.pos else null) ?: fix?.pos ?: map.center() ?: run { toast(ctx.getString(R.string.toast_019)); return }
        val origin = fix?.pos ?: centre
        val rt = route
        val alongRoute = rt != null && !nearDest && kind != "parking"
        val progress = if (rt != null && !previewing) curProgress else 0.0
        val camper = kind in CAMPER_KINDS
        val radius = if (nearDest) 900 else if (camper) 25_000 else if (kind == "parking") 2000 else if (kind == "toilets" || kind == "coffee") 3000 else 6000
        val fuelType = prefs.s(PreferenceKeys.FUEL_TYPE, "E10")
        // (Fuel Finder has no E85 prices, and its station list would show every station rather than ones selling it)
        val fuelLive = kind == "fuel" && FuelFinder.configured(prefs) && fuelType != "E85"
        nearbyKind = kind; nearbyBusy = true; nearby = emptyList()
        scope.launch {
            try {
                // the search runs on its own, and the 30 s limit just stops waiting for it: a blocked network read used to hold the
                // limit up too (the timeout can't end a read that's waiting on a server), so a busy server left it spinning for minutes
                val work = searchScope.async {
                    if (fuelLive) {
                        try { FuelFinder.refresh(ctx, prefs) } catch (e: Exception) { /* use what is stored */ }
                        val st = FuelFinder.stations(ctx)
                        if (st.isNotEmpty()) {
                            val out = ArrayList<Nearby>()
                            // only stations inside the box around the next 120 km of route are checked against the route itself
                            var bMinLat = 90.0; var bMaxLat = -90.0; var bMinLon = 180.0; var bMaxLon = -180.0
                            if (alongRoute && rt != null) for (i in rt.pts.indices) {
                                if (rt.cum[i] < progress - 200 || rt.cum[i] > progress + 120_000) continue
                                val q = rt.pts[i]
                                bMinLat = min(bMinLat, q.lat); bMaxLat = max(bMaxLat, q.lat); bMinLon = min(bMinLon, q.lon); bMaxLon = max(bMaxLon, q.lon)
                            }
                            for (s0 in st) {
                                if (alongRoute && rt != null) {
                                    if (s0.pos.lat < bMinLat - 0.015 || s0.pos.lat > bMaxLat + 0.015 || s0.pos.lon < bMinLon - 0.025 || s0.pos.lon > bMaxLon + 0.025) continue
                                    val pr = RouteExtras.project(rt, s0.pos, 1500.0, progress) ?: continue
                                    if (pr.first < progress - 100 || pr.first - progress > 120_000) continue
                                    val price = FuelFinder.priceOf(ctx, s0.id, fuelType)
                                    out += Nearby(Place(s0.name, listOf(s0.brand, s0.postcode).filter { it.isNotBlank() && it != s0.name }.joinToString(" · "), s0.pos),
                                        pr.first - progress, priceExtra(price, s0.motorway), price?.pence, detour(pr.second), true)
                                } else {
                                    val dd = Geo.dist(centre, s0.pos)
                                    if (dd > radius) continue
                                    val price = FuelFinder.priceOf(ctx, s0.id, fuelType)
                                    out += Nearby(Place(s0.name, listOf(s0.brand, s0.postcode).filter { it.isNotBlank() && it != s0.name }.joinToString(" · "), s0.pos),
                                        Geo.dist(origin, s0.pos), priceExtra(price, s0.motorway), price?.pence)
                                }
                            }
                            if (out.isNotEmpty()) return@async out
                        }
                    }
                    // outside the UK: live prices from the country's own price service (France, Austria, Germany with a key)
                    val cc = detectedCountry.uppercase()
                    if (kind == "fuel" && cc != "GB" && EuFuel.supported(cc, prefs) && EuFuel.hasFuel(cc, fuelType) && online()) {
                        val eu = if (alongRoute && rt != null) EuFuel.along(cc, prefs, RouteExtras.line(rt, progress, progress + 60_000.0, 1500.0, 45), fuelType)
                            else EuFuel.near(cc, prefs, centre, radius / 1000 + 1, fuelType)
                        val out = ArrayList<Nearby>()
                        for (s0 in eu) {
                            if (alongRoute && rt != null) {
                                val pr = RouteExtras.project(rt, s0.pos, 1500.0, progress) ?: continue
                                if (pr.first < progress - 100) continue
                                out += Nearby(Place(s0.name, s0.sub, s0.pos), pr.first - progress, "", s0.cents, detour(pr.second), true, unit = "€")
                            } else if (Geo.dist(centre, s0.pos) <= radius) out += Nearby(Place(s0.name, s0.sub, s0.pos), Geo.dist(origin, s0.pos), "", s0.cents, unit = "€")
                        }
                        if (out.isNotEmpty()) return@async out
                    }
                    // a motorhome's places: only OpenStreetMap knows pitches, dump stations and LPG (a wider strip along the route)
                    if (camper) {
                        return@async if (alongRoute && rt != null) {
                            Overpass.nearbyAlong(kind, RouteExtras.line(rt, progress, progress + 80_000.0, 3000.0, 45), 3000).mapNotNull { n ->
                                val pr = RouteExtras.project(rt, n.place.pos, 4000.0, progress) ?: return@mapNotNull null
                                if (pr.first < progress - 100) null else n.copy(dist = pr.first - progress, detourMin = detour(pr.second), ahead = true)
                            }
                        } else Overpass.nearby(kind, centre, radius).map { it.copy(dist = Geo.dist(origin, it.place.pos)) }
                    }
                    // bioethanol: only stations mapped as selling E85 (TomTom's search can't tell which do)
                    val e85 = kind == "fuel" && fuelType == "E85"
                    if (e85) {
                        return@async if (alongRoute && rt != null) {
                            Overpass.nearbyAlong("fuel_e85", RouteExtras.line(rt, progress, progress + 60_000.0, 1500.0, 45), 1500).mapNotNull { n ->
                                val pr = RouteExtras.project(rt, n.place.pos, 2000.0, progress) ?: return@mapNotNull null
                                if (pr.first < progress - 100) null else n.copy(dist = pr.first - progress, detourMin = detour(pr.second), ahead = true)
                            }
                        } else Overpass.nearby("fuel_e85", centre, 15_000).map { it.copy(dist = Geo.dist(origin, it.place.pos)) }
                    }
                    // TomTom's place search first (quick, and it knows the detour time along a route); OpenStreetMap if that fails
                    if (hasOnline && online()) {
                        val tt = try {
                            if (alongRoute && rt != null && provider == "tomtom") {
                                val line = RouteExtras.line(rt, progress, progress + 80_000.0, 600.0, 140)
                                TomTomRest.poiAlong(apiKey, kind, line).mapNotNull { (n, secs) ->
                                    val pr = RouteExtras.project(rt, n.place.pos, 3000.0, progress) ?: return@mapNotNull null
                                    if (pr.first < progress - 100) null
                                    else n.copy(dist = pr.first - progress, detourMin = if (secs >= 0) Math.round(secs / 60.0).toInt() else detour(pr.second), ahead = true)
                                }
                            } else if (alongRoute && rt != null) null   // (along the route: Mapbox can't search along a line, so OpenStreetMap does it below)
                            else netPoiNearby(kind, centre, radius).map { it.copy(dist = Geo.dist(origin, it.place.pos)) }
                        } catch (e: Exception) { null }
                        if (!tt.isNullOrEmpty()) return@async tt
                    }
                    if (alongRoute && rt != null) {
                        val line = RouteExtras.line(rt, progress, progress + 60_000.0, 1500.0, 45)
                        // Mapbox first (a few points along the next 40 km, asked all at once: a second or two); the free OpenStreetMap
                        // servers only when it finds nothing or there's no Mapbox key (they're often busy and can take a long time)
                        val fromMapbox = if (mbToken.isNotBlank() && online()) (try { mapboxAlong(kind, line) } catch (e: Exception) { emptyList() }) else emptyList()
                        val raw = fromMapbox.ifEmpty { Overpass.nearbyAlong(kind, line, if (kind == "services") 400 else 900) }
                        raw.mapNotNull { n ->
                            val pr = RouteExtras.project(rt, n.place.pos, 1200.0, progress) ?: return@mapNotNull null
                            if (pr.first < progress - 100) null else n.copy(dist = pr.first - progress, detourMin = detour(pr.second), ahead = true)
                        }
                    } else Overpass.nearby(kind, centre, radius).map { it.copy(dist = Geo.dist(origin, it.place.pos)) }
                }
                val list = kotlinx.coroutines.withTimeoutOrNull(30_000L) { work.await() }
                if (list == null) { work.cancel(); toast(ctx.getString(R.string.toast_042), 4000); clearNearby(); return@launch }
                if (pickBest) {
                    val ahead = list.filter { it.ahead || route == null }
                    val best = if (ahead.any { it.price != null }) ahead.filter { it.price != null && it.detourMin in 0..10 }.minByOrNull { (it.price ?: 999.0) + it.detourMin }
                        else ahead.filter { it.detourMin >= 0 }.minByOrNull { it.detourMin * 1000.0 + it.dist / 100.0 } ?: ahead.minByOrNull { it.dist }
                    nearbyBusy = false
                    if (best == null) { toast(ctx.getString(R.string.toast_026), 3000); clearNearby() }
                    else { clearNearby(); addStop(best.place) }
                    return@launch
                }
                if (kind == "parking") {
                    // live free spaces where the car park publishes them, and car parks only those sources know
                    val live = withContext(Dispatchers.IO) { try { if (online()) ParkingLive.around(centre, radius) else null } catch (e: Exception) { null } }
                    nearby = ParkingLive.sortNearby(ParkingLive.enrich(list, live.orEmpty(), origin, radius.toDouble())).take(15)
                } else
                nearby = sortNearby(list).take(15)
                loadLogos(nearby.map { it.place })
                if (nearby.isEmpty()) toast(if (alongRoute) Lang.t("Nichts entlang der Route gefunden", "Nothing found along the route ahead") else Lang.t("Nichts in der Nähe gefunden", "Nothing found nearby"), 3000)
            } catch (e: Exception) { toast(searchFailText(e), 4500) }
            finally { nearbyBusy = false }
        }
    }

    /** Places along the route ahead from Mapbox: its category search round a point every 8 km or so (at most 5, asked together). */
    private suspend fun mapboxAlong(kind: String, line: List<LL>): List<Nearby> {
        val pts = ArrayList<LL>(); var last: LL? = null
        for (q in line) if (last == null || Geo.dist(last, q) > 8000.0) { pts += q; last = q }
        val found = kotlinx.coroutines.coroutineScope {
            pts.take(5).map { q -> async(Dispatchers.IO) { try { MapboxRest.poiNearby(mbToken, kind, q, 2500) } catch (e: Exception) { emptyList() } } }.map { it.await() }
        }
        val seen = HashSet<String>(); val out = ArrayList<Nearby>()
        for (l in found) for (n in l) if (seen.add(String.format(java.util.Locale.US, "%.4f,%.4f", n.place.pos.lat, n.place.pos.lon))) out += n
        return out
    }

    /** The zoom you've set with + and - becomes your normal zoom when not on a route (Settings > Map look to change or reset). */
    fun saveZoomAsDefault() {
        val v = (defaultZoom + zoomOffset).coerceIn(-2.0, 2.0)
        prefs.put(PreferenceKeys.DEFAULT_ZOOM, String.format(java.util.Locale.US, "%.2f", v))
        zoomOffset = 0.0
        rememberFreeZoom()
        fix?.let { onPosition(it, true) }
        toast(ctx.getString(R.string.toast_046), 4500)
    }

    /** Phone: sends [p] to the paired head unit, which offers to start it (next time it's on, within 12 hours). */
    fun sendToCar(p: Place) {
        val code = prefs.s(PreferenceKeys.LINK_CODE, "")
        if (code.isBlank()) { toast(ctx.getString(R.string.toast_031), 4000); return }
        scope.launch {
            try {
                // from the route options: the whole trip goes, with its stops and the way picked there
                val whole = previewing && dest?.let { Geo.dist(it.pos, p.pos) < 30.0 } == true
                val stops = if (whole) via else emptyList()
                val r = if (whole && routes.size > 1) routes.getOrNull(active) else null
                val track = r?.pts?.let { pts -> if (pts.size < 3) emptyList() else (1..24).map { pts[(it * (pts.size - 1L) / 25).toInt()] } } ?: emptyList()
                withContext(Dispatchers.IO) { CarLink.send(code, p, stops, track) }
                toast(str(R.string.rest_434, p.name) + (if (stops.isNotEmpty()) Lang.t(" (mit ${stops.size} ${if (stops.size == 1) "Stopp" else "Stopps"})", " (with ${stops.size} ${if (stops.size == 1) "stop" else "stops"})") else ""), 3500)
                if (previewing) endRoute()
            } catch (e: Exception) { toast(str(R.string.toast_057, e.message ?: str(R.string.toast_058)), 4500) }
        }
    }

    /** Head unit: the place sent from the phone has been started or turned down. */
    fun takePhoneDest(go: Boolean) {
        val p = phoneDest ?: return
        phoneDest = null
        val stops = phoneStops; val track = phoneTrack
        phoneStops = emptyList(); phoneTrack = emptyList()
        if (!go) return
        preferTrack = track
        if (stops.isEmpty()) startRoute(p)
        else { via = stops; evPlanned = emptyList(); evAutoDest = null; evPlanWarn = null; startRoute(p, keepVia = true) }
    }

    /** A search failure in plain words (it used to show the servers' own errors, like "HTTP 429" or "HTTP 502"). */
    private fun searchFailText(e: Exception): String {
        val m = (e.message ?: "").lowercase()
        return when {
            m.contains("no signal") || m.contains("unable to resolve") || m.contains("failed to connect") -> Lang.t("Kein Empfang, die Suche konnte nicht laufen. Versuche es gleich noch einmal.", "No signal, so the search couldn't run. Try again in a moment.")
            m.contains("429") || m.contains("502") || m.contains("503") || m.contains("504") || m.contains("busy") || m.contains("timeout") || m.contains("timed out") ->
                Lang.t("Die kostenlosen Kartenserver sind gerade ausgelastet. Versuche es in einer Minute erneut.", "The free map servers are busy right now. Try again in a minute.") + (if (!hasMapbox) Lang.t(" Mit einem Mapbox-Schlüssel (Einstellungen > Schlüssel & Konten) wird die Suche zuverlässiger.", " Add a Mapbox key in Settings > Keys & accounts for more reliable searches.") else "")
            m.contains("mapbox") -> e.message ?: Lang.t("Suche fehlgeschlagen", "Search failed")
            else -> Lang.t("Suche fehlgeschlagen. Versuche es gleich noch einmal.", "Search failed. Try again in a moment.")
        }
    }

    private fun priceExtra(p: FuelFinder.Price?, motorway: Boolean): String =
        listOfNotNull(p?.let { FuelFinder.ageText(it.updated).let { a -> if (a.isNotBlank()) Lang.t("aktualisiert $a", "updated $a") else null } }, if (motorway) Lang.t("Raststätte", "Motorway services") else null).joinToString(" · ")

    /** Rough extra minutes to call somewhere [off] metres from the route: there and back at town speed, plus a minute. */
    private fun detour(off: Double): Int = Math.round((2.0 * off / 11.0 + 60.0) / 60.0).toInt()

    private fun sortNearby(l: List<Nearby>): List<Nearby> =
        if (nearbySort == "cheap" && l.any { it.price != null }) l.sortedWith(compareBy<Nearby> { it.price ?: 9999.0 }.thenBy { it.dist }) else l.sortedBy { it.dist }

    fun changeNearbySort(mode: String) { nearbySort = mode; nearby = sortNearby(nearby) }

    /** A destination shared from another app: routes straight to coordinates, or puts text in the search box. */
    fun openShared(t: SharedTarget) {
        externalTick++
        val p = t.pos
        if (p != null) {
            val pl = Place(t.name, Lang.t("Aus einer anderen App geteilt", "Shared from another app"), p)
            startRoute(pl)
            try { RouteSync.offerToCar(this, pl) } catch (e: Exception) { }
        }
        else if (!t.query.isNullOrBlank()) externalQuery = t.query
    }

    // ---- guidance ----
    private fun snap(r: Route, p: LL): RouteSnap {
        val result = RouteMatcher.snap(r, p, lastIdx)
        lastIdx = result.segmentIndex
        return result
    }

    private var lastPosAt = 0L
    private var posDt = 1000L
    private var lastMeasured: Fix? = null

    // ---- glide: the arrow and camera are moved every frame by dead reckoning, and only gently corrected by each GPS fix ----
    private var gPos: LL? = null
    private var gHead = 0.0
    private var gSpeed = 0.0
    private var gProg = 0.0
    private var glideRoute: Route? = null
    private var glideOnRoute = false
    private var mAt = 0L
    private var mProg = 0.0
    private var mSpeed = 0.0
    /** In a tunnel with the car's speed coming in: the metres driven since the last fix, by the car's speed. */
    private var drRun = -1.0
    private var mPos: LL? = null
    private var mHead = 0.0
    // camera targets (set once per fix) and the eased values actually shown
    private var tBMode = 2
    private var tTilt = 0.0
    private var tZoom = 16.0
    private var tPad = 0.0
    private var tLeft = 0.0
    private var camTargetAt = 0L
    private var camReady = false
    private var camCatchUp = false
    private var glCamPos: LL = LL(0.0, 0.0)
    private var camB = 0.0
    private var camT = 0.0
    private var camZ = 16.0
    private var camPad = 0.0
    private var camLeft = 0.0

    private fun angDiff(a: Double, b: Double): Double = ((a - b + 540.0) % 360.0 + 360.0) % 360.0 - 180.0
    private fun easeAngle(cur: Double, tgt: Double, dt: Double, tau: Double): Double =
        ((cur + angDiff(tgt, cur) * (1.0 - Math.exp(-dt / tau))) % 360.0 + 360.0) % 360.0

    private var glideRunning = false
    /** The app is on screen (set by the activity): in the background the per-frame work drops right down. */
    @Volatile var appVisible = true
    private var gLastNs = 0L
    private var gGuideAt = 0L
    private var gFullAt = 0L

    /** Runs the glide once per screen refresh (vsync), so the arrow and camera advance in step with what is actually drawn. */
    private fun ensureGlide() {
        if (glideRunning) return
        glideRunning = true
        gLastNs = 0L
        val ch = android.view.Choreographer.getInstance()
        ch.postFrameCallback(object : android.view.Choreographer.FrameCallback {
            private var lastFrame = 0L
            override fun doFrame(frameTimeNanos: Long) {
                val before = PerfStats.ticks
                val t0 = System.nanoTime()
                try { glideTick(frameTimeNanos) } catch (e: Exception) { android.util.Log.w("NavEngine", "glide failed", e) }
                if (PerfStats.on) {
                    if (lastFrame != 0L) PerfStats.gapMsMax = maxOf(PerfStats.gapMsMax, (frameTimeNanos - lastFrame) / 1_000_000L)
                    if (PerfStats.ticks != before) { val d = System.nanoTime() - t0; PerfStats.tickNsSum += d; PerfStats.tickNsMax = maxOf(PerfStats.tickNsMax, d) }
                }
                lastFrame = frameTimeNanos
                ch.postFrameCallback(this)
            }
        })
    }

    // ---- automatic frame rate ----
    /** Camera updates a second in the automatic mode: starts at 30 and steps down when the head unit can't draw that many. */
    private var autoFps = 30
    private var paceAtNs = 0L
    private var paceRenders = 0
    private var paceTicks = 0
    private var paceLow = 0
    private var paceHigh = 0

    /**
     * Every 2 seconds while the camera is following you on the move: if the map drew noticeably fewer frames than the camera
     * moves asked for, the head unit is falling behind and drops frames unevenly (that's the jerky lag). A lower, steady rate
     * looks smoother than a higher one with gaps, so it steps down to 25 and then 20, and back up when there's room again.
     */
    private fun autoPace(frameNs: Long) {
        if (frameNs - paceAtNs < 2_000_000_000L) return
        val renders = PerfStats.renders - paceRenders
        val ticks = PerfStats.ticks - paceTicks
        paceAtNs = frameNs; paceRenders = PerfStats.renders; paceTicks = PerfStats.ticks
        if (prefs.s(PreferenceKeys.MAP_FPS, "auto") != "auto") return
        val moving = following && !previewing && camReady && gSpeed > 2.0
        if (!moving || ticks < autoFps) { paceLow = 0; return }
        val ratio = renders.toDouble() / ticks
        if (ratio < 0.8) {
            paceHigh = 0
            if (++paceLow >= 2 && autoFps > 20) { autoFps = if (autoFps > 25) 25 else 20; paceLow = 0 }
        } else if (ratio > 0.97) {
            paceLow = 0
            if (++paceHigh >= 10 && autoFps < 30) { autoFps = if (autoFps < 25) 25 else 30; paceHigh = 0 }
        }
    }

    /** One line for the performance meter, refreshed every second while it's turned on. */
    var perfLine by mutableStateOf("")
    private var perfRenders = 0; private var perfTicks = 0; private var perfNs = 0L

    private fun perfSample() {
        val r = PerfStats.renders - perfRenders; val t = PerfStats.ticks - perfTicks
        val ns = PerfStats.tickNsSum - perfNs
        perfRenders = PerfStats.renders; perfTicks = PerfStats.ticks; perfNs = PerfStats.tickNsSum
        val p = prefs.s(PreferenceKeys.MAP_FPS, "auto")
        val cam = if (p == "auto") "$autoFps auto" else p
        perfLine = "Map $r fps · camera $t/s ($cam) · app " + String.format(Locale.UK, "%.1f", if (t > 0) ns / t / 1e6 else 0.0) +
            " ms · longest pause ${PerfStats.gapMsMax} ms · lights ${try { map.signalShort() } catch (e: Throwable) { "?" }}"
        PerfStats.gapMsMax = 0
    }

    private var fpsReadNs = 0L
    private var vsyncLastNs = 0L
    private var vsyncNs = 16_666_667.0
    private var frameCount = 0L
    private var glideFps = 30

    private fun glideTick(frameNs: Long) {
        // never do more work than the map can actually draw: on a slow head unit this frees the CPU for rendering
        if (frameNs - fpsReadNs > 1_000_000_000L) { fpsReadNs = frameNs; val p = prefs.s(PreferenceKeys.MAP_FPS, "auto"); glideFps = if (p == "auto") autoFps else (p.toIntOrNull() ?: 30).coerceIn(15, 60) }
        // with another app in front (guidance carrying on in the background) a few updates a second is plenty: nobody sees the map
        val fps = if (appVisible) glideFps else 5
        // update on a steady beat of whole screen frames (every frame at 60, every 2nd at 30, ...), so the map draws at perfectly even
        // intervals; skipping by elapsed time drifted against the screen's own timing and gave an uneven, slightly jumpy motion
        if (frameNs - vsyncLastNs in 1..100_000_000L) vsyncNs = vsyncNs * 0.9 + (frameNs - vsyncLastNs) * 0.1
        vsyncLastNs = frameNs
        val every = Math.round(1e9 / fps / vsyncNs).toInt().coerceIn(1, 4)
        if (++frameCount % every != 0L) return
        PerfStats.ticks++
        // the flowing light only while you're driving the route (in the overview it kept the map redrawing while tiles loaded)
        if (following && !previewing && appVisible) map.flowFrame()
        autoPace(frameNs)
        val now = android.os.SystemClock.elapsedRealtime()
        val dt = if (gLastNs == 0L) 0.016 else ((frameNs - gLastNs) / 1e9).coerceIn(0.002, 0.1)
        gLastNs = frameNs
        val f = fix ?: return
        // in a tunnel (no fix for a while) the arrow keeps going along the route at the last speed for up to two minutes,
        // instead of stopping and then jumping when the signal comes back
        val drOk = glideOnRoute && glideRoute != null && mSpeed > 3.0 && !previewing && simMode == 0
        val rawAge = (now - mAt) / 1000.0
        val age = rawAge.coerceIn(0.0, if (drOk) 120.0 else 2.5)
        val dr = drOk && rawAge > 3.0 && rawAge < 120.0
        if (dr != deadReckoning) deadReckoning = dr
        if (dr) camTargetAt = now
        // in a tunnel the car's own speed (when it sends it) says how far you've really gone: braking in a jam inside is followed too
        val carV = if (dr) CarData.fresh() else null
        if (carV != null) { if (drRun < 0) drRun = gSpeed * age; drRun += carV * dt } else if (!dr) drRun = -1.0
        // braking is followed quickly (a soft 1.2 s lag let the arrow roll on into the junction while you stopped at the lights)
        val vT = carV ?: mSpeed
        gSpeed += (vT - gSpeed) * (1.0 - Math.exp(-dt / (if (vT < gSpeed) 0.35 else 1.2)))
        if (mSpeed < 0.2 && gSpeed < 0.15) gSpeed = 0.0
        val r = glideRoute
        var lookB = gHead
        if (glideOnRoute && r != null) {
            // distance along the route: run on at the current speed, nudge towards where the latest fix says we are, and never go backwards
            // run-on past the last fix uses the slower of glided and measured speed, so a stop doesn't push the arrow ahead
            val target = mProg + (if (dr && drRun >= 0) drRun else min(gSpeed, mSpeed) * age)
            var np = gProg + gSpeed * dt
            val err = target - np
            np = if (abs(err) > 60.0) target else np + (err * (1.0 - Math.exp(-dt / 2.5))).coerceIn(-(0.3 * gSpeed + 0.4) * dt, (0.3 * gSpeed + 0.4) * dt)
            // never backwards while moving; standing still, an arrow that ran ahead slides back to where the fix puts you
            if (np < gProg && abs(err) <= 60.0) np = if (mSpeed < 0.5 && err < -1.0) max(target, gProg - 4.0 * dt) else gProg
            gProg = min(np, r.len)
            val cur = pointAt(r, gProg).first
            val ahead = pointAt(r, gProg + 12.0).first
            if (Geo.dist(cur, ahead) > 2.0) gHead = easeAngle(gHead, Geo.bearing(cur, ahead), dt, 0.18)
            gPos = cur
            // the camera looks further ahead (and further still through a roundabout) so it follows the road, not every wiggle of the wheel
            val round = turn?.let { it.maneuver.uppercase().contains("ROUNDABOUT") && it.dist < 90.0 } == true
            val la = pointAt(r, gProg + (if (round) 60.0 else 30.0)).first
            lookB = if (Geo.dist(cur, la) > 3.0) Geo.bearing(cur, la) else gHead
        } else {
            val gp = gPos ?: f.pos
            val mp = mPos ?: f.pos
            val rad = Math.toRadians(mHead)
            val kLon = 111320.0 * cos(Geo.rad(gp.lat))
            val run = min(gSpeed, mSpeed) * age
            val predLat = (mp.lat) + cos(rad) * run / 110574.0
            val predLon = (mp.lon) + Math.sin(rad) * run / kLon
            var nLat = gp.lat + cos(rad) * gSpeed * dt / 110574.0
            var nLon = gp.lon + Math.sin(rad) * gSpeed * dt / kLon
            val eN = (predLat - nLat) * 110574.0; val eE = (predLon - nLon) * kLon
            if (hypot(eN, eE) > 80.0) { nLat = predLat; nLon = predLon }
            else { val k = 1.0 - Math.exp(-dt / 0.9); nLat += (predLat - nLat) * k; nLon += (predLon - nLon) * k }
            gPos = LL(nLat, nLon)
            if (mSpeed > 1.5) gHead = easeAngle(gHead, mHead, dt, 0.35)
            lookB = gHead
        }
        val gp = gPos ?: return
        map.setMe(gp, gHead, 0)
        if (glideOnRoute && r != null && !previewing) {
            if (now - gDoneAt >= (if (following) 350L else 1500L)) { gDoneAt = now; updateDone(r, gProg) }
        } else if (doneShown) { map.setDone(emptyList()); doneShown = false }

        // guidance (distance to the next turn, lane info, spoken prompts) follows the glided position, a few times a second
        if (glideOnRoute && !previewing && now - gGuideAt >= 250L) { gGuideAt = now; val full = now - gFullAt >= 1000L; if (full) gFullAt = now; updateGuidance(gProg, !full) }

        // camera
        val fresh = now - camTargetAt < 4000
        if (!following || previewing || !fresh) { camReady = false; return }
        if (!camReady) {
            val c = map.cameraNow()
            if (c != null) { glCamPos = LL(c[0], c[1]); camB = c[2]; camT = c[3]; camZ = c[4]; camPad = c[5]; camLeft = c[6] }
            else { glCamPos = gp; camB = lookB; camT = tTilt; camZ = tZoom; camPad = tPad; camLeft = tLeft }
            camCatchUp = true; camReady = true; lastCamSet = null
        }
        if (camCatchUp) {
            // gliding in from the overview to the car: a little gentler, so the move (and the map loading in) feels smooth
            val k = 1.0 - Math.exp(-dt / 0.4)
            glCamPos = LL(glCamPos.lat + (gp.lat - glCamPos.lat) * k, glCamPos.lon + (gp.lon - glCamPos.lon) * k)
            if (Geo.dist(glCamPos, gp) < 1.5) camCatchUp = false
        } else glCamPos = gp
        fun ease(c: Double, t: Double, tau: Double) = c + (t - c) * (1.0 - Math.exp(-dt / tau))
        // slower, softer easing: zoom, tilt and framing glide into place instead of nudging
        camZ = ease(camZ, tZoom, 0.9)
        camT = ease(camT, tTilt, 0.8)
        camPad = ease(camPad, tPad, 0.8)
        camLeft = ease(camLeft, tLeft, 0.9)
        val tb = when (tBMode) { 0 -> 0.0; 1 -> lookB; else -> camB }
        var d = angDiff(tb, camB)
        // a small dead-band and a gentle turn rate: slight wheel movements and small roundabouts no longer swing the map around
        // a soft dead-band (instead of a hard 5 degree one that held, then suddenly caught up): tiny wobbles are ignored, and the turn
        // fades in smoothly as the difference grows
        if (tBMode == 1) { val f = (abs(d) / 9.0).coerceIn(0.0, 1.0); d *= f * f * (3 - 2 * f) }
        val step = d * (1.0 - Math.exp(-dt / 1.05))
        val maxStep = (if (camCatchUp) 140.0 else 32.0) * dt
        camB = ((camB + step.coerceIn(-maxStep, maxStep)) % 360.0 + 360.0) % 360.0
        // nothing has moved (parked, or waiting at lights): don't ask the map to draw the same picture again,
        // which lets it sit idle instead of redrawing every frame (a big saving in heat and battery)
        val lc = lastCamSet
        if (lc != null && abs(lc[0] - glCamPos.lat) < 2e-8 && abs(lc[1] - glCamPos.lon) < 2e-8 && abs(angDiff(lc[2], camB)) < 0.03 &&
            abs(lc[3] - camT) < 0.03 && abs(lc[4] - camZ) < 0.002 && abs(lc[5] - camPad) < 0.0005 && abs(lc[6] - camLeft) < 0.0005) return
        lastCamSet = doubleArrayOf(glCamPos.lat, glCamPos.lon, camB, camT, camZ, camPad, camLeft)
        map.cameraSet(glCamPos, camB, camT, camZ, camPad, camLeft)
    }
    private var lastCamSet: DoubleArray? = null

    /** The visible part of the map changed (Android Auto showing another app beside it): the camera is set again even when parked. */
    fun viewAreaChanged() { lastCamSet = null }

    private var movingNow = false
    /** In portrait: the share of the screen height covered by the trip card at the bottom (the locator is kept above it). */
    var bottomCover = 0.0

    // Standing still (parked, at lights): the first position while stopped, kept until you've clearly moved off. GPS (and the
    // car's own GPS over Android Auto) wanders a few metres and reports small made-up speeds while stopped; following that
    // made the map shake and turn on the spot.
    private var stillAnchor: LL? = null
    private var isStill = false

    /** True while [p] is still within GPS wander of where you stopped. Movement is taken as real once you're clearly away. */
    private fun stillCheck(p: Fix): Boolean {
        if (simMode != 0) { stillAnchor = null; return false }
        val a = stillAnchor
        if (a == null) {
            // (also hardly moved since the last fix: positions that come without a speed read as 0, and must not hold the arrow
            // while you're driving)
            val prev = lastMeasured?.pos
            if (p.speed < 1.0 && (prev == null || Geo.dist(prev, p.pos) < 3.0)) { stillAnchor = p.pos; return true }
            return false
        }
        // how far counts as having moved off: the fix's own uncertainty, kept between 8 and 20 m
        val radius = (if (p.acc > 0.0) p.acc else 10.0).coerceIn(8.0, 20.0)
        if (p.speed > 4.0 || Geo.dist(a, p.pos) > radius) { stillAnchor = null; return false }
        return true
    }

    private fun onPosition(p: Fix, force: Boolean) {
        fix = p
        val newFix = p !== lastMeasured
        if (newFix) isStill = try { stillCheck(p) } catch (e: Exception) { false }
        // the speed used for the camera and the arrow: none while standing still
        val sp = if (isStill) 0.0 else p.speed
        // start/stop moving with a little hysteresis (the full map rests its shop labels while you drive)
        if (!movingNow && sp > 4.0) { movingNow = true; map.setMoving(true) }
        else if (movingNow && sp < 1.5) { movingNow = false; map.setMoving(false) }
        val tNow = android.os.SystemClock.elapsedRealtime()
        if (newFix) {
            lastMeasured = p
            posDt = if (lastPosAt == 0L) 1000L else (tNow - lastPosAt).coerceIn(500L, 2500L)
            lastPosAt = tNow
            // on foot: the walk's directions, and no speed limit or cameras
            if (walking) try { updateWalk(p) } catch (e: Exception) { android.util.Log.w("NavEngine", "walk failed", e) }
            if (!walking) try { lookCamerasAround(p) } catch (e: Exception) { }
            try { if (!walking) updateRoadInfo(p); trackParking(p) } catch (e: Exception) { android.util.Log.w("NavEngine", "road info failed", e) }
            try { checkArea(p.pos) } catch (e: Exception) { }
            try { checkWeatherWarnings(p) } catch (e: Exception) { }
            try { checkRescueLane(p) } catch (e: Exception) { }
            try { checkClearances(p) } catch (e: Exception) { }
            if (!walking) try { checkCameras(p) } catch (e: Exception) { }
            try { garagesAt(p.pos, true) } catch (e: Exception) { }
        }
        // the first position of this session: the zoom you left the map at last time comes back (not on a route)
        if (!freeZoomRestored) { freeZoomRestored = true; if (route == null) zoomOffset = try { savedFreeZoom() } catch (e: Exception) { 0.0 } }
        val r = route
        val s = if (!previewing && r != null && r.pts.size >= 2) snap(r, p.pos) else null
        // how far from the route counts as having left it: tighter than before, but widened when the GPS is less certain
        // Ignore very poor fixes for off-route decisions: they can drift across parallel roads and trigger false reroutes.
        // Still use the fix for display/glide fallback, but require a credible position before declaring a route departure.
        val gpsReliableForReroute = NavigationPolicy.gpsReliableForReroute(p.acc)
        val offLim = NavigationPolicy.offRouteLimit(p.acc)
        // Do not carry a partial off-route count through a bad GPS fix: otherwise one noisy fix can
        // combine with an older departure and cause a reroute when accuracy briefly improves.
        if (newFix && !gpsReliableForReroute) offCount = 0
        // having gone a different way, the closest bit of the route can be much further along it (a road that runs near the end of
        // the route, say). That isn't progress: it must not jump you there (it once said "you have arrived" with 5 minutes to go)
        val nowT = android.os.SystemClock.elapsedRealtime()
        // just after the connection came or went the position can wobble (the phone changes how it finds you): the route stays, and only
        // a clearly wrong position (80 m off or more) counts as leaving it, so the switch to the offline map and back is not a reroute
        val netGrace = nowT - netChangeAt < 20_000L
        val jump = s != null && trustProg >= 0.0 && NavigationPolicy.isProgressJump(
            candidateProgressMeters = s.progress,
            trustedProgressMeters = trustProg,
            speedMps = p.speed,
            elapsedSeconds = (nowT - trustAt) / 1000.0
        )
        // (standing still, e.g. in a car park next to the road, the arrow is only put on the route when the route is really
        // where you are: it used to be pulled up to 30 m onto the route line as soon as a route was worked out)
        val joinLim = if (isStill) max(10.0, min(p.acc, 15.0)) else 30.0
        // the road next to the route: off the route, but still going the route's way (within 35 degrees), at a moderate distance,
        // having been on the route just before. Held for a while instead of believed (see SIDE_HOLD_MS)
        val sideJump = r != null && s != null && !jump && glideOnRoute && gpsReliableForReroute && p.speed > 3.0 &&
            s.dist > offLim && s.dist < 120.0 &&
            abs(angDiff(Geo.bearing(r.pts[s.segmentIndex], r.pts[min(s.segmentIndex + 1, r.pts.size - 1)]), p.heading)) < 35.0
        if (newFix) { if (sideJump) { if (sideSince == 0L) sideSince = nowT } else sideSince = 0L }
        val holdSide = sideJump && sideSince > 0L && nowT - sideSince < SIDE_HOLD_MS
        // driving along the route the other way (on the other carriageway, or turned round): the arrow must not be pulled along the route,
        // which would point it the route's way, backwards, and slide it back along the line. It follows the GPS heading instead (the
        // reroute below still notices it by the direction)
        val againstRoute = r != null && s != null && !isStill && p.speed > 4.0 && gpsReliableForReroute &&
            abs(angDiff(Geo.bearing(r.pts[s.segmentIndex], r.pts[min(s.segmentIndex + 1, r.pts.size - 1)]), p.heading)) > 130.0
        // starting a route: the arrow only joins it when you are really on it (15 m), not from up to 30 m away (it jumped onto the line)
        val onR = r != null && s != null && !jump && !againstRoute && (holdSide || s.dist < (if (glideOnRoute) offLim + 12.0 else joinLim)) &&
            (glideOnRoute || s.dist < 15.0)
        if (onR && s != null && newFix && s.dist <= offLim) { trustProg = s.progress; trustAt = nowT }
        if (newFix) {
            mAt = tNow; mSpeed = sp
            if (onR && r != null && s != null) {
                if (glideRoute !== r || gPos == null) { glideRoute = r; gProg = s.progress; gPos = s.point; gHead = p.heading }
                // standing still: the arrow stays where it stopped instead of creeping along with the GPS wander
                if (!isStill || glideRoute !== r) mProg = s.progress
                glideOnRoute = true
            } else {
                // leaving the route: the arrow goes to where the GPS says at once, not left on the route line (it used to stay
                // there while it was within 150 m, which put it on the route wherever you were)
                val wasOnRoute = glideOnRoute
                glideRoute = null; glideOnRoute = false
                // standing still: the arrow (and the map round it) stays where you stopped, facing the same way
                // (only a position near this fix is held: mPos isn't kept up to date while on a route, so after a route it could be
                // somewhere quite different, and the arrow glided over there until you drove off)
                val held = if (isStill) mPos?.takeIf { Geo.dist(it, p.pos) < 25.0 } else null
                // snapped to the road only when the road is really next to you: the nearest road can be the route, far away
                mPos = held ?: if (p.speed > 2.0 && prefs.b(PreferenceKeys.SNAPROAD, true)) snapNear(p) else p.pos
                if (!isStill) mHead = p.heading
                val gp = gPos
                // off the route the arrow follows the GPS: from the route line it is put straight where you are, and otherwise
                // it is only moved when the GPS has moved on far enough
                if (gp == null || wasOnRoute || Geo.dist(gp, p.pos) > 30.0) { gPos = p.pos; gHead = p.heading }
            }
        }
        ensureGlide()
        // camera target for this fix; the glide loop eases the shown camera towards it every frame
        if (!previewing && following) {
            val hdg = if (onR && r != null && s != null) gHead else p.heading
            followCamera(Fix(gPos ?: p.pos, hdg, sp, p.alt, p.acc), onR)
        } else if (stoppedOverview) {
            // the stopped check only runs while the map follows you: once you've moved the map away (or it isn't following),
            // the traffic view and its banner must go too, or they stay on the screen for good
            stoppedOverview = false; stoppedOverviewActive = false; stoppedSince = 0L
        }
        if (previewing) return
        if (r == null || r.pts.size < 2 || s == null) return
        if (holdSide) {
            // (the road next to the route, not believed yet: no strike towards a reroute)
        } else if (gpsReliableForReroute && (s.dist > (if (netGrace) maxOf(offLim, 80.0) else offLim) || (jump && !netGrace))) {
            if (newFix) {
                // off the route for about 5 seconds in a row: a new route is asked for (the arrow has followed you already)
                if (offSince == 0L) offSince = nowT
                if (nowT - offSince >= 5000L) { offSince = 0L; offCount = 0; reroute(); return }
            }
        } else if (gpsReliableForReroute && newFix && !netGrace) {
            // Still close to the route, but a vehicle moving away from its direction can indicate a wrong turn.
            val seg = Geo.bearing(r.pts[s.segmentIndex], r.pts[min(s.segmentIndex + 1, r.pts.size - 1)])
            // driving along the route the other way (turned round, or on the other carriageway right next to it): the distance never
            // gets big enough to count as leaving the route, so it's noticed by the direction and by going backwards along it
            if (!isStill && p.speed > 3.0 && abs(angDiff(seg, p.heading)) > 130.0) {
                if (backSince == 0L) { backSince = nowT; backProg = s.progress }
                if (nowT - backSince > 5000L && backProg - s.progress > 25.0) { backSince = 0L; offCount = 0; reroute(); return }
            } else backSince = 0L
            if (NavigationPolicy.shouldCountHeadingAwayStrike(s.dist, p.speed, angDiff(seg, p.heading), p.acc)) {
                if (++offCount >= REROUTE_STRIKES) reroute()
            } else offCount = 0
        } else if (newFix) { offCount = 0; offSince = 0L }
        try { if (newFix) checkEvRange() } catch (e: Exception) { }
        try { if (newFix) checkFuelRange() } catch (e: Exception) { }
        val now = System.currentTimeMillis()
        // while the map follows you, only a window of the route is drawn (cheap to redraw); once you move or zoom the map yourself,
        // the whole rest of the route is drawn, so zooming out shows all of it (it used to stop a few miles ahead)
        val wantWhole = !following
        if (force || lastDraw == 0L || wantWhole != drawnWhole || abs(s.progress - lastDrawProg) > 500.0 || now - lastDraw > 30_000L) {
            lastDraw = now; lastDrawProg = s.progress; drawnWhole = wantWhole
            // a little behind the car (hidden under the grey trail) to well ahead: further at motorway speed, where the view reaches further
            val ahead = if (wantWhole) r.len else if (p.speed > 25.0) 25_000.0 else 12_000.0
            val i0 = idxAt(r, s.progress - 900.0)
            val i1 = min(r.pts.size - 1, idxAt(r, s.progress + ahead) + 1)
            if (i1 > i0) map.setMainOnly(r.pts.subList(i0, i1 + 1), r.sev, i0)
        }
        // off the route, the distance along it stays where you left it (until the new route comes) instead of following the nearest bit
        val guideProg = if (onR) gProg else if (!jump && s.dist <= offLim) s.progress else if (trustProg >= 0.0) trustProg else s.progress
        onRouteNow = onR
        if (onR) curProg = guideProg
        if (onR && newFix) try { applyRouteLimit(r, guideProg, p) } catch (e: Exception) { }
        updateGuidance(guideProg, onR && android.os.SystemClock.elapsedRealtime() - gFullAt < 1500L)
        syncPulse()
    }

    /** Makes the route line pulse on the map when a turn is close. */
    private var nearTurn = false
    private var pulseUntil = 0L

    private fun syncPulse() {
        val near = !previewing && route != null && (turn?.dist ?: 1e9) < 150.0
        val now = System.currentTimeMillis()
        if (!near) nearTurn = false
        else if (!nearTurn) { nearTurn = true; pulseUntil = now + 5000 }
        map.setTurnPulse(prefs.b(PreferenceKeys.AMBREACT, true) && prefs.b(PreferenceKeys.AMBMAP, true) && near && now < pulseUntil)
    }

    /** Zoom that adapts: closer when a turn is near or the destination is close, wider on fast roads with a long way to go. */
    private var smoothZ = 0.0
    private var smoothZAt = 0L
    // Start each app session with a slightly wider overview. Once real movement is detected,
    // hand control back to the existing speed-based zoom curve for the rest of the session.
    private var hasDetectedMovement = false

    private fun speedZoom(sp: Double): Double = NavigationPolicy.speedZoom(sp)

    /** How far into the junction close-up the camera is (0 none, 1 fully in). */
    private var closeF = 0.0

    private fun autoZoom(sp: Double, navigating: Boolean): Double {
        if (sp > 1.5) hasDetectedMovement = true
        val now = android.os.SystemClock.uptimeMillis()
        speedLog.addLast(now to sp)
        while (speedLog.isNotEmpty() && now - speedLog.first().first > 8000L) speedLog.removeFirst()
        var z = if (navigating) navZoom(sp) else speedZoom(sp)
        if (navigating) {
            // coming up to traffic lights: in close if you're slowing down (you may be stopping), only a little if you're carrying on
            val dl = nextLightDist()
            if (dl != null && dl < 180.0) {
                val near = ((180.0 - dl) / 180.0).coerceIn(0.0, 1.0)
                val peak = speedLog.maxOfOrNull { it.second } ?: sp
                val slowing = sp < 3.0 || sp < peak * 0.72
                z += (if (slowing) 0.25 else 0.08) * near
            }
            val t = turn
            if (t != null) {
                // Ease closer on approach to a real manoeuvre, then hold a useful junction view.
                val near = ((650.0 - t.dist) / 650.0).coerceIn(0.0, 1.0)
                z += 0.38 * near
                if (sp > 18) z -= 0.3 * ((t.dist - 2500.0) / 2500.0).coerceIn(0.0, 1.0)

                // Painted lane arrows can be visible well before the turn card is in its final
                // close-up. Zoom in progressively for that whole lane-guidance section so the
                // road-level arrows are readable, especially on motorways and dual carriageways.
                if (laneKey != null && prefs.b(PreferenceKeys.LANEMAP, true)) {
                    val laneRange = if (laneFast) 1300.0 else 450.0
                    val laneNear = ((laneRange - t.dist) / laneRange).coerceIn(0.0, 1.0)
                    z += (if (laneFast) 0.95 else 0.65) * laneNear
                }
            }
            val e = eta
            if (e != null && e.dist < 300) z = max(z, 16.4)
        }
        // Free drive: let speed determine the overview. At walking/parking speeds stay close;
        // on faster roads pull back progressively. Do not apply a fixed extra zoom-out here:
        // it made the map much too distant when stopped and flattened the speed curve.
        if (!navigating) z -= 0.15 * (sp / 3.0).coerceIn(0.0, 1.0) +
            0.25 * ((sp - 3.0) / 17.0).coerceIn(0.0, 1.0) +
            0.35 * ((sp - 20.0) / 20.0).coerceIn(0.0, 1.0)
        // lane view: with lane strips showing, close in on the approach so the lanes are big enough to read at a glance
        // Leave headroom for the lane-guidance floor applied by followCamera().
        var top = 19.0
        val lt = turn
        if (navigating && laneKey != null && lt != null && lt.dist < (if (laneFast) 1300.0 else 450.0) && prefs.b(PreferenceKeys.LANE_VIEW, true)) {
            val laneNear = ((if (laneFast) 1300.0 else 450.0) - lt.dist) / (if (laneFast) 1300.0 else 450.0)
            z = max(z, 16.9 + 0.9 * laneNear.coerceIn(0.0, 1.0))
            top = 18.2
        }
        if (navigating && closeF > 0.0) { z = max(z, 17.5 + 0.35 * closeF); top = max(top, 18.2) }
        // (the first-open overview zoom-out is applied once, in followCamera; applying it here too doubled it)
        val target = (z - (if (navigating) 0.45 else 0.15))
            .coerceIn(if (navigating) 13.5 else 14.6, top - 0.3)
        // Dampen automatic zoom changes to prevent the camera pulsing in and out around junctions. The limit is per
        // second rather than per GPS fix: at one fix a second a fixed 0.1 step took 10+ seconds to zoom in for a turn.
        val dt = if (smoothZAt == 0L) 1.0 else ((now - smoothZAt) / 1000.0).coerceIn(0.05, 2.0)
        smoothZAt = now
        smoothZ = NavigationPolicy.smoothZoom(smoothZ, target, maxStep = 0.6 * dt)
        return smoothZ
    }

    private fun followCamera(p: Fix, navigating: Boolean) {
        val sp = p.speed
        val im = prefs.s(PreferenceKeys.MAP_THEME, "immersive") == "immersive"
        // Immersive shows more of the road ahead: the car sits lower on a steeper view
        // The padding pushes the locator down the window (centre sits at (1 + pad) / 2 of the height), in 2D and 3D, on and off a route
        val moving = navigating || sp > 2.0
        // the locator sits low on the screen so you see more of what's ahead (centre at (1 + pad) / 2 of the height)
        val flatPad = if (moving) 0.44 else 0.36
        var pad = if (tilt3d) (if (moving) 0.6 else 0.5) else flatPad
        var tilt = if (tilt3d) (if (im) 64.0 else 60.0) else 0.0
        // a lower view shows far fewer distant tiles and labels, which is a large saving on a slow GPU
        val lite = prefs.mapDetail() != "full"
        // the full map stays a touch lower than it used to (fewer distant tiles to draw), the light map lower still
        val chosen = chosenTilt()
        // a tilt the driver picked is used as it is; automatic keeps the cap that goes with the map's detail
        tilt = if (chosen != null && tilt3d) chosen else minOf(tilt, if (lite) 52.0 else 54.0)
        // tilt with speed: a little flatter when slow (easier to read streets), lower and further-looking at speed
        // (with the lighter map it only ever flattens, so it never draws more than before)
        if (tilt3d && prefs.b(PreferenceKeys.SPEEDTILT, true)) {
            val f = (sp / 31.0).coerceIn(0.0, 1.0)          // 0 when stopped, 1 at about 70 mph
            tilt += -9.0 * (1.0 - f)
        }
        // coming up to a junction or roundabout the view eases down to a flat 2D map so the turn is easy to read, then tilts back up once you are away from it
        if (navigating && tilt3d && prefs.b(PreferenceKeys.TURN2D, true)) {
            val t = turn
            // only for a real turn or roundabout, only in the last stretch before it, and not at speed
            if (t != null && sp < 22.0 && t.maneuver.uppercase().let { it.contains("ROUNDABOUT") || it.contains("TURN") || it.startsWith("SHARP") }) {
                val reach = (sp * 5.0).coerceIn(80.0, 130.0)
                val f = ((t.dist - 25.0) / (reach - 25.0)).coerceIn(0.0, 1.0)     // 0 at the turn, 1 when it is still a way off
                tilt *= f
                pad = pad * f + flatPad * (1.0 - f)
            }
        }
        // junction close-up: at slip roads, motorway exits, forks and lane splits the view moves closer and a little flatter for the last
        // 200 m or so. Never at ordinary turns and roundabouts (those flatten to 2D above), and it never tilts the view lower than
        // usual: it used to swing up to 66 degrees (more than the map's normal limit), which filled the screen with horizon just as
        // the 2D view should have shown the junction.
        closeF = 0.0
        if (navigating && tilt3d && prefs.b(PreferenceKeys.JUNCTION_VIEW, true)) {
            val t = turn
            val m = t?.maneuver?.uppercase() ?: ""
            val ordinaryTurn = m.contains("ROUNDABOUT") || m.contains("ROTARY") || m.contains("TURN") || m.startsWith("SHARP") || m.contains("UTURN")
            val complex = t != null && !ordinaryTurn && (m.startsWith("KEEP") || m.contains("EXIT") || m.contains("RAMP") || m.contains("FORK") || laneKey != null || t.exitNo.isNotBlank())
            if (t != null && complex && t.dist in 5.0..220.0) {
                closeF = ((220.0 - t.dist) / 140.0).coerceIn(0.0, 1.0)
                if (tilt > 44.0) tilt += (44.0 - tilt) * closeF
            }
        }
        // portrait: keep the locator clear of the trip card (its centre sits at (1 + pad) / 2 of the height)
        if (bottomCover > 0.0) pad = minOf(pad, 2.0 * (1.0 - bottomCover - 0.09) - 1.0).coerceAtLeast(0.0)
        // a narrow map (Android Auto split beside music): the trip card along the bottom reaches across most of the width, so
        // moving the arrow sideways can't clear it; the arrow goes up to sit just above the card instead
        else for (pn in panels) {
            if (pn[2] - pn[0] > 0.55f && pn[3] > 0.85f && pn[1] > 0.3f) pad = minOf(pad, 2.0 * (pn[1] - 0.08) - 1.0).coerceAtLeast(0.0)
        }
        tBMode = if (northUp) 0 else if (navigating || sp > 2.0) 1 else 2
        // stopped for 30 s (lights, a queue): a flat top-down view of the traffic round you, back to normal as soon as you move.
        // Not when you've moved the map yourself, nor in the last 400 m of a route
        run {
            val nowMs = android.os.SystemClock.elapsedRealtime()
            if (sp < 0.8) { if (stoppedSince == 0L) stoppedSince = nowMs } else if (sp > 2.5) stoppedSince = 0L
            // (nor waiting at a turn: the close-up of the junction matters more then)
            val nearEnd = navigating && ((eta?.dist ?: Double.MAX_VALUE) < 400.0 || (turn?.dist ?: Double.MAX_VALUE) < 200.0)
            val ov = prefs.b(PreferenceKeys.STOPPED_OVERVIEW, true) && stoppedSince > 0L && nowMs - stoppedSince > 30_000L &&
                !nearEnd && arrival == null && abs(zoomOffset) < 0.01
            if (ov != stoppedOverview) { stoppedOverview = ov; stoppedOverviewActive = ov }
            if (ov) { tilt = 0.0; pad = 0.0 }
        }
        tTilt = tilt; tPad = pad
        val requestedZoom = autoZoom(sp, navigating) + zoomOffset + (if (isCar) carViewportZoomOffset else 0.0) + (if (!navigating) defaultZoom else 0.0)
        // Road-level lane ribbons are only useful when their lane boundaries and the route are
        // distinguishable. Keep a firm camera zoom floor for the entire time they are drawn;
        // user/viewport zoom offsets must not silently pull the map back until the ribbons look
        // detached from the road. Tighten the view as the manoeuvre gets closer.
        val laneTurnDistance = if (navigating) turn?.dist else null
        val laneZoomFloor = if (navigating && laneKey != null && prefs.b(PreferenceKeys.LANE_VIEW, true)) {
            when {
                laneTurnDistance != null && laneTurnDistance < 100.0 -> 18.25
                laneTurnDistance != null && laneTurnDistance < 300.0 -> 18.05
                else -> 17.8
            }
        } else Double.NEGATIVE_INFINITY
        // Apply the first-open overview adjustment after the lane zoom floor so that the
        // floor cannot cancel the requested two-step zoom-out while stationary. Once
        // movement is detected, this offset is zero and the existing zoom policy is unchanged.
        val initialOverviewOffset = if (hasDetectedMovement) 0.0 else 0.8
        tZoom = max(requestedZoom, laneZoomFloor) - initialOverviewOffset
        // (the traffic view pulls back about two zoom levels)
        if (stoppedOverview) tZoom = (tZoom - 1.8).coerceAtLeast(12.5)
        // landscape: your arrow moves sideways just enough to clear whatever card is beside it at its height (the search panel
        // and saved places, the turn card, the trip card), and back to the middle when they fold away, e.g. once you're driving.
        // Portrait: the cards span the width, so it stays centred. Mirrored for left-hand drive.
        // Not navigating (on the phone / head unit screen): the arrow sits in the middle of the clear map between the search panel and
        // the buttons, whatever height they reach to, so it's always in the middle of what's left of the map.
        // (Android Auto too: there the outlines are measured within the part of the screen its own buttons leave clear)
        tLeft = if (!navigating && !previewing) idleShift(bottomCover > 0.0)
                else if (bottomCover <= 0.0) arrowShift((1.0 + pad) / 2.0) else 0.0
        map.cardsMirrored = mirrored && !isCar // (Android Auto always has its cards on the left)
        camTargetAt = android.os.SystemClock.elapsedRealtime()
    }

    // outlines of the cards on screen, as parts of its width and height (left, top, right, bottom), reported by the screen
    // (0: the search panel / turn card, 1: the trip card, 2: the map buttons down the other side)
    private val panels = Array(3) { FloatArray(4) }

    fun setPanel(i: Int, l: Float, t: Float, r: Float, b: Float) {
        if (i !in panels.indices) return
        val p = panels[i]; p[0] = l; p[1] = t; p[2] = r; p[3] = b
    }

    /**
     * How far to move the arrow sideways (as the camera's side padding) for an arrow at [y] (a part of the screen height):
     * when a card reaches across that height, the arrow moves to the middle of the clear part of the map, a little nearer the
     * card than the exact middle so it doesn't end up far over. Nothing beside it: dead centre.
     */
    private fun arrowShift(y: Double): Double {
        // Use a slightly taller collision band so the locator clears cards in compact Android Auto
        // surfaces too (full-screen, music split, and app-launcher split layouts report different bounds).
        val band = 0.14
        // Which side the cards are on is read from where they actually are, not from the driving side: Android Auto keeps
        // its cards on the left even for left-hand drive, and assuming they'd swapped sides pushed the arrow under them.
        var coverL = 0.0; var coverR = 0.0
        for (p in panels) {
            if (p[2] - p[0] < 0.02 || p[3] - p[1] < 0.02) continue
            if (p[1] < y + band && p[3] > y - band) {
                if (p[0] + p[2] < 1.0) coverL = max(coverL, p[2].toDouble()) else coverR = max(coverR, 1.0 - p[0])
            }
        }
        val cover = max(coverL, coverR)
        if (cover < 0.05) return 0.0
        // Move farther away from the info/turn card; panel bounds are measured from the actual
        // Compose surface, so this adapts as Android Auto changes between full and split layouts.
        val shift = (cover * 0.78).coerceAtMost(0.42)
        return if (coverR > coverL) -shift else shift
    }

    /**
     * Not navigating: how far to move the arrow sideways so it's centred between the search panel on one side and the map buttons
     * on the other (as a part of the screen width; positive moves it right). Portrait: the search panel is above the arrow, so only the
     * buttons count. Kept small, so it's a gentle nudge off centre rather than a jump.
     */
    private fun idleShift(portrait: Boolean): Double {
        var left = 0.0; var right = 1.0
        for ((i, p) in panels.withIndex()) {
            if (p[2] - p[0] < 0.02 || p[3] - p[1] < 0.02) continue
            if (i == 1) continue                                   // the trip card isn't up when you're not navigating
            if (i == 0 && (portrait || p[2] - p[0] > 0.6f)) continue // full-width in portrait: it's above the arrow, not beside it
            if (p[0] + p[2] < 1.0) left = max(left, p[2].toDouble()) else right = minOf(right, p[0].toDouble())
        }
        if (right - left < 0.2) return 0.0
        // the arrow's spot moves by half the side padding, so twice the distance from the middle
        return (2.0 * ((left + right) / 2.0 - 0.5)).coerceIn(-0.42, 0.42)
    }

    private fun incidentLabel(i: Incident): String = when (i.cat) {
        "roadWorks" -> Lang.t("Baustelle", "Roadworks"); "roadClosed" -> Lang.t("Straße gesperrt", "Road closed"); "accident" -> Lang.t("Unfall", "Accident")
        "jam" -> Lang.t("Stau", "Traffic jam"); "laneClosed" -> Lang.t("Fahrstreifen gesperrt", "Lane closed"); "danger" -> Lang.t("Gefahr", "Hazard")
        "wrongway" -> Lang.t("Falschfahrer", "Wrong-way driver")
        else -> i.text.ifBlank { Lang.t("Störung", "Incident") }
    }

    /** Speed cameras within [radius] m from a downloaded area that covers [p] (null when none does, so they're looked up online). */
    private fun localCameras(p: LL, radius: Double): List<LL>? {
        val area = OfflineRouter.areaWith(ctx, "cameras.tsv", p) ?: return null
        val dLat = radius / 111320.0; val dLon = radius / (111320.0 * cos(Math.toRadians(p.lat)).coerceAtLeast(0.2))
        return OfflineRouter.lamps(ctx, p.lat - dLat, p.lon - dLon, p.lat + dLat, p.lon + dLon, "cameras.tsv", OfflineRouter.packDir(ctx, area), 600)
            .map { a -> LL(a[0], a[1]).also { c -> if (a.size > 2) Overpass.camDir[Overpass.camKey(c)] = a[2] } }.filter { Geo.dist(it, p) <= radius }
    }

    /** Camera alerts are on: switched on in Settings, and not in a country that bans them (unless you've chosen to keep them there). */
    fun camerasOn(): Boolean = prefs.b(PreferenceKeys.CAMERA_WARNINGS, false) &&!(detectedCountry in CAMERA_BANNED && prefs.b(PreferenceKeys.CAMBANOBEY, true))

    /** Speed cameras are drawn on the map: switched on in Settings, also in countries that ban the warnings (they're never announced there unless [camerasOn]). */
    fun camerasOnMap(): Boolean = prefs.b(PreferenceKeys.CAMERA_MAP, true)

    private var banNoted = ""
    /** Says once, on arriving in a country that bans camera warnings, that they've been switched off. */
    private fun noteCameraBan() {
        val cc = detectedCountry
        val name = CAMERA_BANNED[cc]
        if (name == null || !prefs.b(PreferenceKeys.CAMERA_WARNINGS, false) ||!prefs.b(PreferenceKeys.CAMBANOBEY, true)) { if (name == null) banNoted = ""; return }
        if (banNoted == cc) return
        banNoted = cc
        toast(str(R.string.rest_441, name), 7000)
    }

    /** False when the camera is known to check only the other direction (or a road crossing yours), judged against [heading]. */
    private fun camFacesUs(c: LL, heading: Double): Boolean {
        val d = Overpass.camDir[Overpass.camKey(c)] ?: return true
        return abs(angDiff(d, heading)) <= 75.0
    }

    /** What the last camera search around you found, for Settings (so "none shown" can be told apart from "none found"). */
    var camNote by mutableStateOf("")
    /** No camera list could be had here (no offline pack, OpenStreetMap not answering) and none is kept: the map says so. */
    var camNoData by mutableStateOf(false)
    /** The cameras from the search around you (route or not). */
    private var aroundCams: List<LL> = emptyList()

    /** The cameras shown and warned about: the ones around you plus the ones found along the route, within 10 km. */
    private fun mergeCams() {
        val here = fix?.pos
        val seen = HashSet<Long>()
        val out = ArrayList<LL>()
        // free look: the cameras around the middle of the screen, wherever it is; otherwise around you
        val freeLook = !navigatingNow() && viewCentre != null
        val ref = if (freeLook) viewCentre else here
        for (c in aroundCams + knownCams) {
            if (ref != null && Geo.dist(ref, c) > (if (freeLook) 8_000.0 else 10_000.0)) continue
            if (seen.add(Math.round(c.lat * 1e5) * 1000003L + Math.round(c.lon * 1e5))) out += c
        }
        // the list the warnings use: empty where they're off, so nothing is announced; the map draws its own list below
        cameras = if (camerasOn()) out else emptyList()
        // on a route only the cameras on it are drawn (the ones still ahead); otherwise every camera around you
        val shown = if (!camerasOnMap()) emptyList() else if (navigatingNow()) routeCams.filter { it.second > curProg - 20.0 }.map { it.first } else out
        if (shown != camsShown) { camsShown = shown; map.setCameras(shown) }
    }
    private var camsShown: List<LL>? = null
    private var camsMergedAt = 0L
    /** How far along the route you are (updated with every GPS fix while on it). */
    @Volatile private var curProg = 0.0
    private var onRouteNow = false
    private fun navigatingNow() = route != null && !previewing

    /** Every speed camera found along routes so far (by the map's search along the route, together with the traffic lights). */
    @Volatile private var knownCams: List<LL> = emptyList()
    private var camsCount = -1
    init {
        map.onRouteCameras = { c -> knownCams = c; if (camerasOnMap()) mergeCams() }
        map.onViewMoved = { c -> viewMoved(c); garagesAt(c, false) }
        map.onGarageTap = { g -> showGarage(g) }
        map.onChainTap = { i -> chain?.locs?.getOrNull(i)?.let { selectChain(it) } }
        // the phone, not while being guided: a tap on a free spot shows its address with "Los" (a second tap anywhere puts the card away)
        map.onFreeTap = { p ->
            if (!isCar && (route == null || previewing) && results.isEmpty() && chain == null && !chainBusy) { if (mapPick != null) clearPick() else showPick(p, null) }
        }
    }

    // ---- car parks on the map, with their free spaces ----
    private var garagesFetchedAt = 0L
    private var garagesFetchedPos: LL? = null
    private var garagesBusy = false
    private var garagesShown = false

    /** Looks up the car parks round [c] (your position while following, else the middle of the map) from city zoom in. */
    private fun garagesAt(c: LL, fromFix: Boolean) {
        if (!prefs.b(PreferenceKeys.GARAGES_MAP, true)) { if (garagesShown) { garagesShown = false; map.setGarages(emptyList()) }; return }
        if (fromFix && !following) return
        if (garagesBusy || map.zoomNow() < 12.5) return
        val now = System.currentTimeMillis()
        val last = garagesFetchedPos
        if (last != null && now - garagesFetchedAt < 90_000L && Geo.dist(last, c) < 1200.0) return
        garagesBusy = true; garagesFetchedAt = now; garagesFetchedPos = c
        scope.launch {
            try {
                val l = withContext(Dispatchers.IO) { try { if (online()) ParkingLive.around(c, 4000) else null } catch (e: Exception) { null } }
                if (l != null && prefs.b(PreferenceKeys.GARAGES_MAP, true)) { garagesShown = true; map.setGarages(l) }
            } finally { garagesBusy = false }
        }
    }

    /** A car park tapped on the map: its card with the free spaces, to drive there or add it as a stop. */
    private fun showGarage(g: Garage) {
        nearbyBusy = false
        nearbyKind = "parking"
        nearby = listOf(g.toNearby(fix?.pos ?: g.pos))
    }

    /** Where the map was last dragged to (when not following the car), and the last look there. */
    @Volatile private var viewCentre: LL? = null
    private var viewAt = 0L
    private var viewFetched: LL? = null

    /**
     * Free look (the map dragged away from you, not navigating): the speed cameras round the middle of the screen are looked up,
     * so the ones in the places you look at show on the map. Before, they were only looked up round your own position.
     */
    private fun viewMoved(c: LL) {
        if (!camerasOnMap() || navigatingNow()) return
        viewCentre = c
        mergeCams()
        val now = System.currentTimeMillis()
        val last = viewFetched
        if (now - viewAt < 4000L || (last != null && Geo.dist(last, c) < 1500.0)) return
        viewAt = now; viewFetched = c
        scope.launch {
            // the offline pack's own camera list first (no internet needed), then OpenStreetMap
            val found = withContext(Dispatchers.IO) { localCameras(c, 6000.0) ?: Overpass.cameras(c, 6000) }
            // a failed lookup keeps the cameras already shown; the next drag tries again
            if (found == null) { viewFetched = null; if (aroundCams.isEmpty()) showNoCamData(); camNote = Lang.t("Suche am Kartenausschnitt fehlgeschlagen (${Overpass.lastNote.take(60)})", "Lookup at the map view failed (${Overpass.lastNote.take(60)})"); return@launch }
            camsFound()
            addAroundCams(found)
            camNote = Lang.t("${found.size} Blitzer am Kartenausschnitt (OpenStreetMap)", "${found.size} cameras at the map view (OpenStreetMap)")
            mergeCams()
        }
    }

    /** Adds cameras found by a lookup to the ones already known: kept for the rest of the drive, so those looked at once stay on the map when the server doesn't answer later. */
    private fun addAroundCams(found: List<LL>) {
        val keys = HashSet<Long>(); aroundCams.forEach { keys += camKeyOf(it) }
        val merged = ArrayList(aroundCams)
        for (f in found) if (keys.add(camKeyOf(f))) merged += f
        while (merged.size > 4000) merged.removeAt(0)
        aroundCams = merged
    }

    private fun camKeyOf(c: LL): Long = Math.round(c.lat * 1e5) * 1000003L + Math.round(c.lon * 1e5)

    /** How far along the route each traffic light on it is (metres, in order), for zooming in as you come up to one. */
    @Volatile private var lightOffs = DoubleArray(0)
    private var lightRoute: Route? = null
    private var lightPts: List<LL> = emptyList()
    init {
        map.onRouteSignals = { pts ->
            val r = route
            if (r == null || pts.isEmpty()) { lightOffs = DoubleArray(0); lightRoute = null; lightPts = emptyList() }
            // (the same lights on the same route: nothing to work out again)
            else if (r === lightRoute && pts == lightPts) { }
            else scope.launch(Dispatchers.Default) {
                val offs = try { pts.take(600).map { offsetAlong(r, it) }.sorted().toDoubleArray() } catch (e: Throwable) { DoubleArray(0) }
                withContext(Dispatchers.Main) { if (route === r) { lightOffs = offs; lightRoute = r; lightPts = pts } }
            }
        }
    }

    /** How far along [r] the point nearest to [p] is, in metres (on its own: it doesn't touch the position tracking). */
    private fun offsetAlong(r: Route, p: LL): Double {
        val pts = r.pts
        val k = cos(Geo.rad(p.lat)) * 111320.0; val k2 = 110574.0
        var bd = 1e12; var bo = 0.0
        for (i in 0 until pts.size - 1) {
            val ax = (pts[i].lon - p.lon) * k; val ay = (pts[i].lat - p.lat) * k2
            val bx = (pts[i + 1].lon - p.lon) * k; val by = (pts[i + 1].lat - p.lat) * k2
            if ((ax > bd && bx > bd) || (ax < -bd && bx < -bd) || (ay > bd && by > bd) || (ay < -bd && by < -bd)) continue
            val dx = bx - ax; val dy = by - ay
            val l2 = dx * dx + dy * dy
            val t = (if (l2 > 0) -(ax * dx + ay * dy) / l2 else 0.0).coerceIn(0.0, 1.0)
            val d = hypot(ax + dx * t, ay + dy * t)
            if (d < bd) { bd = d; bo = r.cum[i] + (r.cum[i + 1] - r.cum[i]) * t }
        }
        return bo
    }

    /** Recent speeds (time, m/s), to tell whether you're slowing down for a traffic light. */
    private val speedLog = ArrayDeque<Pair<Long, Double>>()

    /** Distance to the next traffic light on the route ahead (null when none within 400 m). */
    private fun nextLightDist(): Double? {
        val r = route ?: return null
        if (lightRoute !== r) return null
        val offs = lightOffs
        val prog = curProgress
        for (o in offs) { val d = o - prog; if (d > 5.0) return if (d < 400.0) d else null }
        return null
    }

    /**
     * The driving zoom on a route: closer the slower you go, and by the kind of road you're on (closer on town streets, further out
     * on motorways). About z17.6 at 30 mph in town, z16 at 70 on a motorway.
     */
    private fun navZoom(sp: Double): Double {
        val f = fix
        val cls = if (f != null) try { map.roadAt(f.pos, f.heading)?.cls } catch (e: Throwable) { null } else null
        return NavigationPolicy.routeSpeedZoom(sp, cls)
    }

    /**
     * Speed cameras along the whole route, with how far along it each one is. The cameras come from the map's search along the
     * route (one combined request per stretch with the traffic lights), so this needs no requests of its own: it just matches
     * them to the route whenever more arrive or the route changes.
     */
    private fun updateRouteCams(r: Route, @Suppress("UNUSED_PARAMETER") progress: Double) {
        // (the ones found along the route and the ones from the search around you: either can hold a camera on the route)
        val cams = (knownCams + aroundCams).distinctBy { Math.round(it.lat * 1e5) * 1000003L + Math.round(it.lon * 1e5) }
        if (camsRoute === r && camsCount == cams.size) return
        camsRoute = r; camsCount = cams.size
        if (cams.isEmpty()) { routeCams = emptyList(); return }
        scope.launch {
            val found = withContext(Dispatchers.Default) {
                // points every 10 m along the route (filled in along long straight stretches, where the route's own points can be
                // hundreds of metres apart: a camera in between was too far from any point and never shown or warned about),
                // in a coarse grid so each camera only checks the route nearby
                val grid = HashMap<Long, ArrayList<Int>>()
                val pts = ArrayList<LL>(); val offs = ArrayList<Double>()
                fun key(lat: Double, lon: Double) = Math.floor(lat / 0.002).toLong() * 1000003L + Math.floor(lon / 0.003).toLong()
                fun add(p: LL, o: Double) { grid.getOrPut(key(p.lat, p.lon)) { ArrayList() }.add(pts.size); pts += p; offs += o }
                for (n in r.pts.indices) {
                    add(r.pts[n], r.cum[n])
                    if (n + 1 < r.pts.size) {
                        val a = r.pts[n]; val b = r.pts[n + 1]
                        val len = r.cum[n + 1] - r.cum[n]
                        val steps = (len / 10.0).toInt()
                        for (j in 1 until steps) {
                            val f = j / steps.toDouble()
                            add(LL(a.lat + (b.lat - a.lat) * f, a.lon + (b.lon - a.lon) * f), r.cum[n] + len * f)
                        }
                    }
                }
                val out = ArrayList<Pair<LL, Double>>()
                for (c in cams) {
                    // within 25 m of the route line (was 45 m): a camera in the next village, just off the road, is not on the route
                    var bestD = 25.0; var off = -1.0
                    val k = key(c.lat, c.lon)
                    var bi = -1
                    for (dy in -1..1) for (dx in -1..1) grid[k + dy * 1000003L + dx]?.forEach { i ->
                        val d = Geo.dist(pts[i], c)
                        if (d < bestD) { bestD = d; off = offs[i]; bi = i }
                    }
                    // a camera that checks the other carriageway (it's known which way it points): not on your route
                    val cd = Overpass.camDir[Overpass.camKey(c)]
                    if (off >= 0 && cd != null && bi >= 0 && bi + 1 < pts.size && abs(angDiff(cd, Geo.bearing(pts[bi], pts[bi + 1]))) > 75.0) off = -1.0
                    if (off >= 0) out += c to off
                }
                out.sortBy { it.second }
                out
            }
            if (camsRoute === r) { routeCams = found; hazardSig = 0; mergeCams() }
        }
    }

    /**
     * A report on the map: what it is, where, and when. Your own are kept on this device ([sid] is its id on the shared reports
     * server once it has been sent there, 0 before); [shared] ones come from other drivers through [SharedReports].
     */
    class Report(val kind: String, val pos: LL, val t: Long, val sid: Long = 0L, val shared: Boolean = false)

    /** How long each kind of report lasts, its map icon, and what is said when you come up to it. */
    private fun reportLife(k: String) = when (k) { "roadworks" -> 12 * 3_600_000L; "closed" -> 3 * 3_600_000L; "jam" -> 1_800_000L; "hazard" -> 3_600_000L; "accident", "breakdown" -> 2 * 3_600_000L; "mobilecam" -> 4 * 3_600_000L; else -> 3 * 3_600_000L }
    private fun reportIcon(k: String) = when (k) { "roadworks" -> 1; "closed" -> 2; "accident" -> 4; "hazard" -> 5; "breakdown" -> 6; "mobilecam" -> 7; "jam" -> 8; else -> 3 }
    fun reportName(k: String) = when (k) { "roadworks" -> Lang.t("Baustelle", "Roadworks"); "closed" -> Lang.t("Straße gesperrt", "Road closed"); "accident" -> str(R.string.ui_077); "hazard" -> str(R.string.ui_076); "breakdown" -> str(R.string.ui_078); "mobilecam" -> str(R.string.ui_079); "jam" -> str(R.string.ui_103); else -> str(R.string.ui_075) }
    /** The kinds this version knows (a shared report of a kind added later is left out rather than shown as police). */
    private val reportKinds = setOf("police", "accident", "hazard", "breakdown", "mobilecam", "jam", "roadworks", "closed")

    /** Your own reports still in force (older police-only reports are read too). Stored as "kind,lat,lon,time[,server id]". */
    fun reportsActive(): List<Report> {
        val now = System.currentTimeMillis()
        val out = ArrayList<Report>()
        for (r in prefs.s(PreferenceKeys.POLICEREP, "").split(";")) {
            val q = r.split(",")
            val la = q.getOrNull(0)?.toDoubleOrNull(); val lo = q.getOrNull(1)?.toDoubleOrNull(); val t = q.getOrNull(2)?.toLongOrNull()
            if (la != null && lo != null && t != null && now - t < reportLife("police")) out += Report("police", LL(la, lo), t)
        }
        for (r in prefs.s(PreferenceKeys.REPORTS, "").split(";")) {
            val q = r.split(",")
            val k = q.getOrNull(0).orEmpty()
            val la = q.getOrNull(1)?.toDoubleOrNull(); val lo = q.getOrNull(2)?.toDoubleOrNull(); val t = q.getOrNull(3)?.toLongOrNull()
            val sid = q.getOrNull(4)?.toLongOrNull() ?: 0L
            if (k.isNotBlank() && la != null && lo != null && t != null && now - t < reportLife(k)) out += Report(k, LL(la, lo), t, sid)
        }
        return out
    }

    private fun saveReports(list: List<Report>) {
        // (the server id is a fifth field only when there is one, so an older version reading it back still finds the first four)
        prefs.put(PreferenceKeys.REPORTS, list.joinToString(";") { "${it.kind},${it.pos.lat},${it.pos.lon},${it.t}" + (if (it.sid > 0) ",${it.sid}" else "") })
    }

    fun report(kind: String) {
        val p = fix?.pos
        if (p == null) { toast(ctx.getString(R.string.toast_048), 2500); return }
        val keep = reportsActive().takeLast(29)
        val mine = Report(kind, p, System.currentTimeMillis())
        saveReports(keep + mine)
        prefs.put(PreferenceKeys.POLICEREP, "")
        toast(str(R.string.toast_067, reportName(kind)), 3000)
        pushHazards(hzProgress)
        // shared with other drivers (Settings, on by default): the server's id is kept with the report, so the same report coming
        // back from the server isn't shown twice
        if (SharedReports.sharing(prefs)) scope.launch {
            val sid = withContext(Dispatchers.IO) { SharedReports.send(prefs, kind, p) } ?: return@launch
            SharedReports.markOwn(sid)
            val cur = reportsActive()
            if (cur.none { it.t == mine.t && it.kind == kind }) return@launch
            saveReports(cur.map { if (it.t == mine.t && it.kind == kind && it.sid == 0L) Report(it.kind, it.pos, it.t, sid) else it })
            policeSig = 0
        }
    }

    fun reportPolice() = report("police")

    fun clearReports() { prefs.put(PreferenceKeys.REPORTS, ""); prefs.put(PreferenceKeys.POLICEREP, ""); pushHazards(hzProgress); toast(ctx.getString(R.string.toast_037), 2000) }

    // ---- reports shared by other drivers ----
    /** Other drivers' reports near you or along the route, from the last fetch (already narrowed down). */
    @Volatile private var sharedShown: List<SharedReports.Item> = emptyList()
    private var sharedAt = 0L
    private var sharedRoute: Route? = null
    @Volatile private var sharedBusy = false

    /** "<Name> noch da?" for a shared report you are passing: answered with [answerVote], or gone by itself after 12 s. */
    class VotePrompt(val id: Long, val kind: String, val name: String)
    var votePrompt by mutableStateOf<VotePrompt?>(null)
    private val votePrompted = HashSet<Long>()

    /** Your own reports and the shared ones from others (each shown once: one that came back from the server as yours is left out). */
    private fun allReports(): List<Report> {
        val own = reportsActive()
        val shared = sharedShown
        if (shared.isEmpty() || !SharedReports.sharing(prefs)) return own
        val mine = own.mapTo(HashSet()) { it.sid }
        val now = System.currentTimeMillis()
        val out = ArrayList<Report>(own)
        for (s in shared) if (s.exp > now && !s.mine && s.kind in reportKinds && s.id !in mine && !SharedReports.isOwn(s.id) && !SharedReports.isGone(s.id))
            out += Report(s.kind, s.pos, s.t, s.id, shared = true)
        return out
    }

    /** Fetches the shared reports every 2 minutes, and at once when the route changes (a new one, a reroute, or none any more). */
    private fun refreshShared(p: Fix) {
        // sharing off (or no server): no requests at all, and nothing shared shown
        if (!SharedReports.sharing(prefs)) { if (sharedShown.isNotEmpty()) { sharedShown = emptyList(); SharedReports.shownCount = 0 }; return }
        val now = System.currentTimeMillis()
        val r = route
        if (sharedBusy || (r === sharedRoute && now - sharedAt < 120_000L)) return
        sharedBusy = true; sharedAt = now; sharedRoute = r
        val car = p.pos
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { try { fetchShared(car, r) } catch (e: Throwable) { null } }
                if (found != null) { sharedShown = found; SharedReports.shownCount = found.size; policeSig = 0 }
            } finally { sharedBusy = false }
        }
    }

    /**
     * The shared reports within 30 km of the car or within about 1 km of the route. The area asked for is the car's 30 km box and
     * the route's box (1 km wider) together; when that is more than the server's 6 degrees a side, the route is asked for in parts
     * instead. Null when every request failed.
     */
    private fun fetchShared(car: LL, r: Route?): List<SharedReports.Item>? {
        fun padLon(lat: Double, m: Double) = m / (111_320.0 * max(0.15, cos(Math.toRadians(lat))))
        fun grow(b: DoubleArray, m: Double): DoubleArray {
            val dLat = m / 111_320.0
            val dLon = padLon(if (abs(b[0]) > abs(b[2])) b[0] else b[2], m)
            return doubleArrayOf((b[0] - dLat).coerceAtLeast(-90.0), (b[1] - dLon).coerceAtLeast(-180.0), (b[2] + dLat).coerceAtMost(90.0), (b[3] + dLon).coerceAtMost(180.0))
        }
        // boxes as south, west, north, east
        val boxes = ArrayList<DoubleArray>()
        boxes += grow(doubleArrayOf(car.lat, car.lon, car.lat, car.lon), 30_000.0)
        if (r != null && r.pts.size >= 2) {
            var cur: DoubleArray? = null
            var prev: LL? = null
            for (q in r.pts) {
                // across the 180th meridian the box would wrap the whole world: close the part here and start a new one
                if (prev != null && abs(q.lon - prev.lon) > 180.0) { cur?.let { boxes += grow(it, 1_000.0) }; cur = null }
                val b = cur
                if (b == null) { cur = doubleArrayOf(q.lat, q.lon, q.lat, q.lon); prev = q; continue }
                val s = min(b[0], q.lat); val w = min(b[1], q.lon); val n = max(b[2], q.lat); val e = max(b[3], q.lon)
                if (n - s > 4.0 || e - w > 4.0) {
                    // this part is full: the next one starts at the last point, so the stretch in between is covered too
                    boxes += grow(b, 1_000.0)
                    val pv = prev ?: q
                    cur = doubleArrayOf(min(pv.lat, q.lat), min(pv.lon, q.lon), max(pv.lat, q.lat), max(pv.lon, q.lon))
                } else { b[0] = s; b[1] = w; b[2] = n; b[3] = e }
                prev = q
            }
            cur?.let { boxes += grow(it, 1_000.0) }
        }
        val all = doubleArrayOf(boxes.minOf { it[0] }, boxes.minOf { it[1] }, boxes.maxOf { it[2] }, boxes.maxOf { it[3] })
        val ask = if (all[2] - all[0] <= 6.0 && all[3] - all[1] <= 6.0) listOf(all) else boxes.take(16)
        val byId = LinkedHashMap<Long, SharedReports.Item>()
        var anyOk = false
        for (b in ask) {
            val got = SharedReports.fetch(prefs, b[0], b[1], b[2], b[3]) ?: continue
            anyOk = true
            for (x in got) byId[x.id] = x
        }
        if (!anyOk) return null
        SharedReports.remember(byId.values.toList())
        // the route as points about every 300 m, in a coarse grid (cells of roughly 2 km) so each report only checks the route nearby
        val grid = HashMap<Long, ArrayList<LL>>()
        fun key(lat: Double, lon: Double) = Math.floor(lat / 0.02).toLong() * 1000003L + Math.floor(lon / 0.03).toLong()
        if (r != null) {
            var last = -1e9
            for (n in r.pts.indices) if (n < r.cum.size && (r.cum[n] - last >= 300.0 || n == r.pts.size - 1)) {
                val q = r.pts[n]
                grid.getOrPut(key(q.lat, q.lon)) { ArrayList() }.add(q)
                last = r.cum[n]
            }
        }
        val out = ArrayList<SharedReports.Item>()
        for (x in byId.values) {
            if (x.mine) continue
            if (Geo.dist(car, x.pos) <= 30_000.0) { out += x; continue }
            if (grid.isEmpty()) continue
            val k = key(x.pos.lat, x.pos.lon)
            var near = false
            // (points 300 m apart: within 1.15 km of one is within about 1 km of the line between them)
            for (dy in -1..1) for (dx in -1..1) {
                if (!near && grid[k + dy * 1000003L + dx]?.any { q -> Geo.dist(q, x.pos) <= 1_150.0 } == true) near = true
            }
            if (near) out += x
        }
        return out
    }

    /** "Noch da" (true) or "Weg" (false) for the report in [votePrompt]. "Weg" takes it off the map at once. */
    fun answerVote(still: Boolean) {
        val vp = votePrompt ?: return
        votePrompt = null
        if (!still) { SharedReports.markGone(vp.id); policeSig = 0; pushHazards(hzProgress) }
        scope.launch(Dispatchers.IO) { SharedReports.vote(prefs, vp.id, still) }
    }

    /** Closes the "noch da?" question without answering. */
    fun dismissVote() { votePrompt = null }

    /**
     * How many kinds of source can report traffic here at all: the drivers' reports (when sharing is on), Mapbox and TomTom traffic
     * (with their keys), and the Autobahn GmbH (in Germany). With fewer than three, an older report isn't doubted for having no second source.
     */
    private fun sourcesPossible(): Int =
        (if (SharedReports.sharing(prefs)) 1 else 0) + (if (hasMapbox) 1 else 0) + (if (hasKey) 1 else 0) + (if (detectedCountry == "DE") 1 else 0)

    /**
     * How many sources say what [r] says: the report itself, a "still there" vote from another driver, a second report of the same
     * kind within 300 m, and an incident of the traffic feeds (Autobahn GmbH, Mapbox, TomTom) of the same kind within 600 m.
     */
    private fun reportSources(r: Report, list: List<Report>): Int {
        fun related(c: String) = when (r.kind) {
            "jam" -> c.contains("jam", true)
            "roadworks" -> c.contains("work", true) || c.contains("laneclosed", true)
            "accident" -> c.contains("accident", true)
            "hazard" -> c.contains("danger", true)
            "closed" -> isClosure(c)
            else -> false
        }
        var n = 1
        if (r.shared && (sharedShown.firstOrNull { it.id == r.sid }?.up ?: 0) > 0) n++
        if (list.any { it !== r && it.kind == r.kind && Geo.dist(it.pos, r.pos) < 300.0 }) n++
        if (incMatches.any { (inc, _, _) -> related(inc.cat) && inc.pts.any { Geo.dist(it, r.pos) < 600.0 } }) n++
        return n
    }

    /** Refreshes the map icons when reports change or expire, and warns when one is just ahead. */
    private fun checkPolice(p: Fix) {
        try { refreshShared(p) } catch (e: Exception) { }
        val list = allReports()
        var sig = list.size
        for (r in list) sig = sig * 31 + (r.t % 100000).toInt() + (r.sid % 100000).toInt()
        if (sig != policeSig) { policeSig = sig; pushHazards(hzProgress) }
        // passing someone else's report: is it still there? (asked once per report, only while moving)
        if (votePrompt == null && p.speed > 3.0 && SharedReports.sharing(prefs)) {
            val hit = list.firstOrNull { it.shared && it.sid !in votePrompted && Geo.dist(p.pos, it.pos) < 150.0 }
            if (hit != null) {
                votePrompted.add(hit.sid)
                val vp = VotePrompt(hit.sid, hit.kind, reportName(hit.kind))
                votePrompt = vp
                // a jam or a closure driven through at 50 km/h or more is gone: left unanswered, it is voted "weg" by itself
                val fast = p.speed >= 14.0 && hit.kind in setOf("jam", "closed")
                scope.launch {
                    delay(12_000)
                    if (votePrompt === vp) {
                        if (fast && (fix?.speed ?: 0.0) >= 11.0) answerVote(false) else votePrompt = null
                    }
                }
            }
        }
        if (!prefs.b(PreferenceKeys.POLICEALERTS, true) || p.speed < 4.0) return
        for (r in list) {
            val d = Geo.dist(p.pos, r.pos)
            if (d < 40 || d > 600) continue
            // weighed by its sources: a fresh report counts alone. One that has run most of its life (70 %) is only announced when another
            // source says the same, because the others (a driver's vote, a second report, a traffic feed) would have it too, but only where
            // there are other sources at all: the fewer there are here, the more the one that exists is trusted
            if (System.currentTimeMillis() - r.t > reportLife(r.kind) * 7 / 10 && sourcesPossible() >= 3 && reportSources(r, list) < 2) continue
            val diff = abs(((Geo.bearing(p.pos, r.pos) - p.heading + 540.0) % 360.0) - 180.0)
            // (a shared report is remembered by its server id: two drivers' reports can carry the same time)
            if (diff < 50 && policeAlerted.add(if (r.shared) -r.sid else r.t)) {
                chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
                raiseAlert(if (r.kind == "mobilecam") "camera" else "report", str(R.string.toast_068, reportName(r.kind)))
                speak(str(R.string.toast_068, reportName(r.kind)), true)
            }
        }
    }

    /** Pushes the camera, roadworks and closure icons for what is still ahead on the route (only when something changed). */
    private fun pushHazards(progress: Double) {
        hzProgress = progress
        val list = ArrayList<Pair<LL, Int>>()
        if (prefs.b(PreferenceKeys.HAZARD_ICONS, true)) {
            for (r in allReports()) list += r.pos to reportIcon(r.kind)
            for ((inc, off, _) in incMatches) {
                if (off <= progress - 20) continue
                val k = when (inc.cat.lowercase()) { "roadclosed" -> 2; "roadworks", "laneclosed" -> 1; else -> -1 }
                if (k >= 0) list += inc.pts.first() to k
            }
        }
        var sig = list.size
        for ((p, k) in list) sig = sig * 31 + (p.lat * 1e5).toInt() * 7 + (p.lon * 1e5).toInt() + k
        if (sig == hazardSig) return
        hazardSig = sig
        map.setHazards(list)
    }

    private var yieldAt: LL? = null
    @Volatile private var yieldNear: List<Pair<LL, Boolean>> = emptyList()
    private val yieldAlerted = HashSet<LL>()

    private fun checkYieldSigns(p: LL, heading: Double) {
        val last = yieldAt
        if (last == null || Geo.dist(last, p) > 300.0) {
            yieldAt = p
            Thread {
                yieldNear = try { Overpass.yieldSigns(p.lat - 0.005, p.lon - 0.007, p.lat + 0.005, p.lon + 0.007) } catch (e: Exception) { emptyList() }
            }.start()
        }
        // stop signs and "give way" signs have their own switches; both are off by default
        val stopOn = prefs.b(PreferenceKeys.STOP_SIGNS, false)
        val giveWayOn = prefs.b(PreferenceKeys.GIVE_WAY, false)
        val hit = yieldNear.firstOrNull { (s, stop) ->
            (if (stop) stopOn else giveWayOn) && s !in yieldAlerted && Geo.dist(p, s) < 200.0 && abs(angDiff(Geo.bearing(p, s), heading)) <= 60.0
        } ?: return
        yieldAlerted.add(hit.first)
        val text = if (hit.second) "Stoppschild voraus" else "Vorfahrt gewähren"
        raiseAlert("yield", text)
        speak(text, alert = true)
    }

    /** Autobahn GmbH's roadworks, warnings and closures on the motorways of [r]. Asked for when the route starts and every 5 minutes. */
    private fun autobahnIncidents(r: Route): List<Incident> {
        if (!prefs.b(PreferenceKeys.INCIDENTS, true)) return emptyList()
        val roads = r.steps.mapNotNull { AutobahnRest.roadOf(it.street) }.distinct()
        if (roads.isEmpty()) return emptyList()
        val key = roads.joinToString(",")
        val now = System.currentTimeMillis()
        if (key != abRoads || now - abAt > 300_000L) {
            abAt = now; abRoads = key
            scope.launch { abItems = withContext(Dispatchers.IO) { try { AutobahnRest.incidents(roads) } catch (e: Exception) { emptyList() } } }
        }
        return abItems
    }

    // ---- wrong-way drivers reported on the motorway ahead ----
    private var wwAt = 0L
    private var wwBusy = false
    private var wwItems: List<Incident> = emptyList()
    private val wwSaid = HashSet<String>()

    /**
     * A driver going the wrong way reported on the motorway you're on, up to 30 km ahead (either carriageway: which way they're
     * going is often not known). From Autobahn GmbH's warnings (asked every minute) and the traffic incidents already loaded.
     */
    private fun checkWrongWay(r: Route, progress: Double) {
        if (!prefs.b(PreferenceKeys.WRONGWAY_REPORTS, true) || simMode != 0 || previewing) return
        val now = System.currentTimeMillis()
        val roads = r.steps.filter { it.off < progress + 30_000.0 }.let { st ->
            // the road you're on now and the motorways in the next 30 km
            val cur = st.lastOrNull { it.off <= progress }
            (listOfNotNull(cur) + st.filter { it.off > progress })
        }.mapNotNull { AutobahnRest.roadOf(it.street) }.distinct()
        if (roads.isNotEmpty() && !wwBusy && now - wwAt > 60_000L) {
            wwAt = now; wwBusy = true
            scope.launch {
                try { wwItems = withContext(Dispatchers.IO) { AutobahnRest.wrongWay(roads) } } catch (e: Exception) { }
                finally { wwBusy = false }
            }
        }
        val all = wwItems + incMatches.map { it.first }.filter { it.cat == "wrongway" || AutobahnRest.isWrongWay(it.text) }
        for (inc in all) {
            // the nearest point of the route ahead (30 km) within 400 m of the report: the other carriageway counts too
            var bestOff = -1.0; var bestD = 400.0
            for (q in inc.pts.take(30)) for (n in r.pts.indices) {
                val c = r.cum[n]
                if (c < progress - 100 || c > progress + 30_000) continue
                val d = Geo.dist(r.pts[n], q)
                if (d < bestD) { bestD = d; bestOff = c }
            }
            if (bestOff < 0) continue
            val ahead = max(0.0, bestOff - progress)
            // said when found, and again when it's under 5 km away
            val stage = if (ahead < 5000) "near" else "far"
            if (!wwSaid.add(inc.id + stage)) continue
            if (stage == "near") wwSaid.add(inc.id + "far")
            val text = Lang.t("Achtung, Falschfahrer gemeldet, etwa ${Geo.fmtDist(ahead, imperial)} voraus. Rechts fahren, nicht überholen.",
                "Warning: wrong-way driver reported about ${Geo.fmtDist(ahead, imperial)} ahead. Keep right, don't overtake.")
            chime(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
            raiseAlert("incident", text, 12_000)
            showNotice(Notice("wrongway", Lang.t("Falschfahrer gemeldet", "Wrong-way driver reported"),
                Lang.t("Etwa ${Geo.fmtDist(ahead, imperial)} voraus · rechts fahren, nicht überholen", "About ${Geo.fmtDist(ahead, imperial)} ahead · keep right, don't overtake"), 3), 60_000L)
            speak(Lang.t("Achtung, Falschfahrer gemeldet, in ${Geo.spoken(ahead, imperial)}. Bitte rechts fahren und nicht überholen.",
                "Warning, wrong-way driver reported ${Geo.spoken(ahead, imperial)} ahead. Keep right and don't overtake."), true)
            break
        }
    }

    // ---- road closures: routes find a way round them ----
    /** Closures known from the traffic feeds (TomTom, Mapbox, Autobahn GmbH): their points and until when they're kept (by id). */
    private val closures = java.util.concurrent.ConcurrentHashMap<String, Pair<List<LL>, Long>>()
    /** Closures a reroute was already asked for (each once), and one waiting to be said once the new route is in. */
    private val closureRerouted = HashSet<String>()
    private var closurePending: Incident? = null

    private fun isClosure(cat: String) = cat.equals("roadClosed", true) || cat == "8"

    private fun noteClosures(list: List<Incident>) {
        val now = System.currentTimeMillis()
        closures.entries.removeIf { it.value.second < now }
        for (inc in list) if (isClosure(inc.cat) && inc.pts.isNotEmpty()) closures[inc.id.ifBlank { "${inc.pts.first().lat},${inc.pts.first().lon}" }] = inc.pts to now + 30 * 60_000L
    }

    /** Points on closed roads: from the feeds (start, middle and end of each), and drivers' "Straße gesperrt" reports. */
    private fun closurePts(from: LL?, to: LL?): List<LL> {
        val now = System.currentTimeMillis()
        val pts = ArrayList<LL>()
        for ((ps, until) in closures.values) if (until >= now) pts.addAll(listOf(ps.first(), ps[ps.size / 2], ps.last()).distinct())
        try { for (rp in allReports()) if (rp.kind == "closed") pts += rp.pos } catch (e: Exception) { }
        // the start or the end of the trip: the only way there or away, so it is not avoided
        val ok = pts.filter { p -> (from == null || Geo.dist(p, from) > 300.0) && (to == null || Geo.dist(p, to) > 300.0) }
        val here = from ?: fix?.pos
        return if (here != null) ok.sortedBy { Geo.dist(it, here) } else ok
    }

    /** About 40 m round each closure point, for TomTom and the offline router. */
    private fun closureBoxes(from: LL?, to: LL?): List<Pair<LL, LL>> = closurePts(from, to).take(20).map { p ->
        val dLat = 40.0 / 110574.0; val dLon = 40.0 / (111320.0 * cos(Geo.rad(p.lat)))
        LL(p.lat - dLat, p.lon - dLon) to LL(p.lat + dLat, p.lon + dLon)
    }

    /** True when [r] runs within 30 m of a point of [pts] ahead of [from] metres along it. */
    private fun passes(r: Route, pts: List<LL>, from: Double): Boolean {
        for (q in pts) for (n in r.pts.indices) { if (r.cum[n] < from) continue; if (Geo.dist(r.pts[n], q) < 30.0) return true }
        return false
    }

    /** A closure on the route ahead: a new route round it (once per closure). */
    private fun closureAhead(r: Route) {
        if (previewing || simMode != 0 || rerouting || route !== r) return
        val prog = curProgress
        val feed = incMatches.filter { (inc, off, _) -> isClosure(inc.cat) && off > prog + 50 && off < prog + 40_000 }.map { it.first }
        val own = try { allReports().filter { it.kind == "closed" }.map { Incident("rep${it.t}", "roadClosed", "", 0, listOf(it.pos)) } } catch (e: Exception) { emptyList() }
        val d = dest?.pos
        val hit = (feed + own).firstOrNull { inc -> inc.id !in closureRerouted && (d == null || inc.pts.none { Geo.dist(it, d) < 300.0 }) && passes(r, inc.pts.take(30), prog + 50) } ?: return
        closureRerouted += hit.id
        if (hit.id.startsWith("rep")) noteClosures(emptyList()) else closures[hit.id] = hit.pts to System.currentTimeMillis() + 30 * 60_000L
        closurePending = hit
        lastReroute = 0L
        reroute()
    }

    /** After a reroute for a closure: says the new way round, or that there is none (the route still goes through). */
    private fun closureAfterReroute(r: Route) {
        val c = closurePending ?: return
        closurePending = null
        if (passes(r, c.pts.take(30), 0.0)) {
            speak(Lang.t("Sperrung auf der Route, kein Umweg gefunden.", "Road closed on the route, no way round found."), true)
            showNotice(Notice("closure", Lang.t("Straße gesperrt", "Road closed"), Lang.t("Kein Umweg gefunden", "No way round found"), 2), 15_000L)
        } else {
            speak(Lang.t("Straße gesperrt. Neue Route.", "Road closed. New route."), true)
            showNotice(Notice("closure", Lang.t("Straße gesperrt", "Road closed"), Lang.t("Neue Route um die Sperrung", "New route round the closure"), 2), 10_000L)
        }
    }

    private fun checkIncidents(r: Route, progress: Double) {
        try { checkWrongWay(r, progress) } catch (e: Exception) { }
        try { if (camerasOnMap()) updateRouteCams(r, progress); pushHazards(progress) } catch (e: Exception) { }
        // TomTom's incident data when there's a key; otherwise the incidents that came with a Mapbox route
        val fromRoute = (!hasKey || ttDown()) && r.incidents.isNotEmpty()
        val ab = autobahnIncidents(r)
        if ((!hasKey && !fromRoute && ab.isEmpty()) || simMode != 0 || !prefs.b(PreferenceKeys.INCIDENTS, true)) { incidentAhead = null; return }
        val now = System.currentTimeMillis()
        // (the phone in the background while the car screen runs the trip: the car asks, not both)
        if (now - incAt > 180_000 && !shadow()) {
            incAt = now
            scope.launch {
                val found = withContext(Dispatchers.IO) {
                    try {
                        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
                        var any = false
                        for (n in r.pts.indices) {
                            if (r.cum[n] < progress - 200 || r.cum[n] > progress + 40_000) continue
                            val q = r.pts[n]; any = true
                            minLat = min(minLat, q.lat); maxLat = max(maxLat, q.lat); minLon = min(minLon, q.lon); maxLon = max(maxLon, q.lon)
                        }
                        if (!any) emptyList() else {
                            val list = (if (fromRoute) r.incidents else if (hasKey) TomTomRest.incidents(apiKey, minLon - 0.01, minLat - 0.01, maxLon + 0.01, maxLat + 0.01) else emptyList<Incident>()) + ab
                            noteClosures(list)
                            val out = ArrayList<Triple<Incident, Double, String>>()
                            for (inc in list) {
                                var bestOff = -1.0; var bestD = 70.0
                                val q = inc.pts.first()
                                for (n in r.pts.indices) {
                                    if (r.cum[n] < progress - 200 || r.cum[n] > progress + 40_000) continue
                                    val d = Geo.dist(r.pts[n], q)
                                    if (d < bestD) { bestD = d; bestOff = r.cum[n] }
                                }
                                if (bestOff >= 0) out += Triple(inc, bestOff, incidentLabel(inc))
                            }
                            out
                        }
                    } catch (e: Exception) { null }
                }
                if (found != null) { incMatches = found; try { applyIncidentSev(r, found) } catch (e: Exception) { }; try { closureAhead(r) } catch (e: Exception) { } }
            }
        }
        // (wrong-way drivers have their own, louder warning: checkWrongWay)
        val next = incMatches.filter { it.second > progress - 30 && it.first.cat != "wrongway" && !AutobahnRest.isWrongWay(it.first.text) }.minByOrNull { it.second }
        if (next == null) { incidentAhead = null; return }
        val d = max(0.0, next.second - progress)
        if (d > 15_000) { incidentAhead = null; return }
        incidentAhead = IncidentAhead(next.third, d, next.first.delaySec)
        if (d < 8000 && incAlerted.add(next.first.id)) {
            val mins = next.first.delaySec / 60
            val extra = if (mins >= 2) Lang.t(", etwa $mins Minuten Verzögerung", ", about $mins minutes delay") else ""
            raiseAlert("incident", Lang.t("${next.third} in ${Geo.fmtDist(d, imperial)}$extra", "${next.third} ahead, ${Geo.fmtDist(d, imperial)}$extra"), 7000)
            speak(Lang.t("In ${Geo.spoken(d, imperial)}: ${next.third}$extra", "${next.third} ahead in ${Geo.spoken(d, imperial)}$extra"), true)
        }
    }

    private var sevBase: IntArray? = null
    private var sevBaseRoute: Route? = null
    /** The route service's own traffic colours for [svcSevRoute], before any refresh, TomTom or incident was painted on. */
    private var svcSev: IntArray? = null
    private var svcSevRoute: Route? = null

    /** Colours the stretch of road around each live incident amber (slow) or red (heavy), on top of the route's own traffic data. */
    private fun applyIncidentSev(r: Route, found: List<Triple<Incident, Double, String>>) {
        if (sevBaseRoute !== r) { sevBase = r.sev.copyOf(); sevBaseRoute = r }
        val base = sevBase ?: return
        if (base.size != r.sev.size) return
        System.arraycopy(base, 0, r.sev, 0, base.size)
        for ((inc, startOff, _) in found) {
            val level = when {
                inc.cat.contains("close", true) || inc.delaySec >= 300 -> 2
                // every jam ahead colours the route (amber; red above from 5 minutes, see above)
                inc.delaySec >= 90 || inc.cat.contains("jam", true) -> 1
                else -> 0
            }
            if (level == 0) continue
            var endOff = startOff + 250.0
            if (inc.pts.size > 1) {
                val last = inc.pts.last()
                var bd = 70.0
                for (n in r.pts.indices) {
                    if (r.cum[n] < startOff - 50) continue
                    val d = Geo.dist(r.pts[n], last)
                    if (d < bd) { bd = d; endOff = r.cum[n] }
                }
            }
            if (endOff < startOff) endOff = startOff + 250.0
            for (i in r.sev.indices) if (r.cum[i] >= startOff - 20 && r.cum[i] <= endOff + 20) r.sev[i] = max(r.sev[i], level)
        }
    }

    private fun p0Speed(): Double = fix?.speed ?: 0.0

    /** Every few minutes while driving, asks for fresh routes and offers a different one if it saves a few minutes. */
    private fun checkFaster(cur: Route, remainingSecs: Double) {
        val now = System.currentTimeMillis()
        val f = fix ?: return
        val d = dest ?: return
        val live = prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)
        if (previewing || simMode != 0 || faster != null || !prefs.b(PreferenceKeys.FASTER_ROUTE, true) || shadow()) return
        // live: every 2 minutes up to the last 5 minutes, in slow traffic too (a jam is when another way matters most)
        if (remainingSecs < (if (live) 300 else 900) || now - lastFasterCheck < (if (!live) 240_000L else if (f.speed < 0.5) 360_000L else 120_000L) || (!live && f.speed < 3.0)) return
        lastFasterCheck = now
        val ahead = cur.pts.drop(lastIdx.coerceIn(0, max(0, cur.pts.size - 2))).filterIndexed { i, _ -> i % 3 == 0 }
        val viaPts = via.map { it.pos }
        scope.launch {
            try {
                syncRoutePrefs(f.pos, d.pos)
                // live: from both route services when there are keys for both (each finds ways the other misses)
                val rs = withContext(Dispatchers.IO) { netRoutes(f.pos, d.pos, true, 0L, viaPts, driveHeading(f.pos), askBoth = live) }
                if (rs.size < 2 || route !== cur) return@launch
                // how much of each candidate runs along the road we are already following
                fun overlap(r: Route): Double {
                    val s = r.pts.filterIndexed { i, _ -> i % 10 == 0 }
                    if (s.isEmpty()) return 0.0
                    return s.count { p -> ahead.any { Geo.dist(p, it) < 40.0 } }.toDouble() / s.size
                }
                val same = rs.maxByOrNull { overlap(it) } ?: return@launch
                try { refreshTraffic(cur, same) } catch (e: Exception) { android.util.Log.w("NavEngine", "traffic refresh failed", e) }
                if (live) {
                    // live comparison: every other way stays on the map in grey with how it compares, to tap any time
                    val others = rs.filter { it !== same && overlap(it) < 0.8 }.take(2)
                    val list = withContext(Dispatchers.Default) { altsFor(cur, same, others, curProgress) }
                    if (route !== cur) return@launch
                    liveAlts = list
                    map.setLiveAlts(list.map { it.line() }, list.map { it.bubble }, list.map { it.diffMin }, list.map { it.traffic })
                    val best = list.minByOrNull { it.diffMin }
                    // a slower way you picked yourself: a quicker one only as a card to take or leave (7 s), never switched to or said
                    if (pickedSlower) {
                        if (best != null && best.diffMin <= -3 && liveSaid.add(best.diffMin / 2)) offerFaster(best.route, -best.diffMin)
                        return@launch
                    }
                    if (best != null && autoOn() && -best.diffMin >= autoMin()) {
                        switchToAlt(list.indexOf(best), auto = true)
                        announceAuto(cur, -best.diffMin)
                        return@launch
                    }
                    if (best != null && best.diffMin <= -3 && liveSaid.add(best.diffMin / 2)) speak(Lang.t("Eine schnellere Route spart ${-best.diffMin} Minuten. Tippe sie auf der Karte an, um zu wechseln.", "A faster route is available, saving ${-best.diffMin} minutes. Tap it on the map to switch."), true)
                    return@launch
                }
                val best = rs.filter { it !== same && overlap(it) < 0.8 }.minByOrNull { it.time } ?: return@launch
                val saved = ((same.time - best.time) / 60.0).toInt()
                if (pickedSlower) { if (saved >= 3 && best.time < same.time * 0.95) offerFaster(best, saved); return@launch }
                if (autoOn() && saved >= autoMin()) {
                    routes = listOf(best); active = 0
                    said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
                    drawRoutes()
                    updateRouteZones()
                    fix?.let { onPosition(it, true) }
                    announceAuto(cur, saved)
                    return@launch
                }
                if (saved >= 3 && best.time < same.time * 0.95) {
                    faster = FasterRoute(best, saved)
                    speak(Lang.t("Eine schnellere Route spart $saved Minuten", "A faster route is available, saving $saved minutes"), true)
                    val shown = faster
                    delay(25_000)
                    if (faster === shown) { faster = null; lastFasterCheck = System.currentTimeMillis() + 300_000L }
                }
            } catch (e: Exception) { }
        }
    }

    private var curProgress = 0.0
    private var lastTrafficAt = 0L
    private var trafficBusy = false

    /** Every few minutes asks TomTom for the same trip again, so the amber/red road and the time left follow the traffic as it changes. */
    private fun checkTraffic(cur: Route) {
        val now = System.currentTimeMillis()
        val f = fix ?: return
        val d = dest ?: return
        if (previewing || simMode != 0 || trafficBusy || !hasOnline || shadow()) return
        if (lastTrafficAt == 0L) { lastTrafficAt = now - 120_000L }   // first refresh about a minute after setting off
        // in a queue (crawling, under about 20 km/h) the traffic is asked for every 2 minutes, so the arrival time catches up with a jam sooner
        val crawling = (fix?.speed ?: 99.0) < 5.5
        if (now - lastTrafficAt < (if (crawling) 120_000L else 180_000L)) return
        lastTrafficAt = now
        trafficBusy = true
        val viaPts = via.map { it.pos }
        val at = curProgress
        scope.launch {
            try {
                syncRoutePrefs(f.pos, d.pos)
                val rs = withContext(Dispatchers.IO) { netRoutes(f.pos, d.pos, false, 0L, viaPts, driveHeading(f.pos), true) }
                val fresh = rs.firstOrNull() ?: return@launch
                if (route !== cur) return@launch
                // on a Mapbox route with a TomTom key, TomTom times the same road too: each stretch keeps the worse of the two
                val others = ArrayList<Route>()
                if (provider == "mapbox" && apiKey.isNotBlank() && !prefs.b(PreferenceKeys.ROUTE_ECO, false)) {
                    val tt: Route? = try { withContext(Dispatchers.IO) { routesFrom("tomtom", f.pos, d.pos, false, 0L, viaPts, driveHeading(f.pos), true).firstOrNull() } } catch (e: Exception) { null }
                    tt?.let { others += it }
                }
                refreshTraffic(cur, fresh, at, others)
            } catch (e: Exception) { }
            finally { trafficBusy = false }
        }
    }

    /** Copies the latest traffic colouring from a fresh request for the same road onto the route being driven, so amber and red stay current. */
    private fun refreshTraffic(cur: Route, fresh: Route, at: Double = curProgress, others: List<Route> = emptyList()) {
        if (fresh.sev.isEmpty() || cur.sev.isEmpty()) return
        fun key(p: LL) = Math.round(p.lat * 1e4) * 1000003L + Math.round(p.lon * 1e4)
        val m = HashMap<Long, Int>(fresh.sev.size * 2)
        for (i in fresh.sev.indices) m[key(fresh.pts[i])] = fresh.sev[i]
        // the route service's own traffic for this route, kept from before anything was painted on it: a refresh starts from that
        // (plus the fresh answer), never from the painted line, or a jam or incident that had cleared stayed red for good
        if (svcSevRoute !== cur || svcSev?.size != cur.sev.size) {
            svcSev = (if (sevBaseRoute === cur && sevBase?.size == cur.sev.size) sevBase!! else cur.sev).copyOf(); svcSevRoute = cur
        }
        val own = svcSev!!
        var hit = 0
        val base = IntArray(cur.sev.size)
        for (i in cur.sev.indices) { val v = m[key(cur.pts[i])]; if (v != null) { base[i] = v; hit++ } else base[i] = own[i] }
        // the other service's view of the same stretch: the worse of the two counts
        for (o in others) {
            if (o.sev.isEmpty()) continue
            val mo = HashMap<Long, Int>(o.sev.size * 2)
            for (i in o.sev.indices) mo[key(o.pts[i])] = o.sev[i]
            for (i in cur.sev.indices) { val v = mo[key(cur.pts[i])]; if (v != null) base[i] = max(base[i], v) }
        }
        // only trust it when it is the same road: a good share of the points still ahead must line up with the fresh route
        val firstAhead = max(0, idxAt(cur, at))
        var ahead = 0; var match = 0
        for (i in firstAhead until cur.sev.size) { ahead++; if (m.containsKey(key(cur.pts[i]))) match++ }
        if (ahead > 0 && match < ahead * 0.6) return
        // fresh offsets count from where you were when it was asked, so shift them onto the full route
        if (fresh.len > 0) {
            cur.freeFlow = max(0.0, fresh.time - fresh.delay) / fresh.len * cur.len
            cur.delays = fresh.delays.map { DelaySeg(it.off + at, it.end + at, it.secs) }
        }
        sevBase = base.copyOf(); sevBaseRoute = cur
        System.arraycopy(base, 0, cur.sev, 0, base.size)
        try { applyIncidentSev(cur, incMatches) } catch (e: Exception) { }
        lastDraw = 0L
    }

    /** The quicker way as a card at the top (phone and car) to take or leave; it goes again by itself after 7 seconds. */
    private fun offerFaster(r: Route, saved: Int) {
        val fr = FasterRoute(r, saved)
        faster = fr
        scope.launch { delay(7_000); if (faster === fr) { faster = null; lastFasterCheck = System.currentTimeMillis() } }
    }

    fun acceptFaster() {
        val fr = faster ?: return
        routes = listOf(fr.route); active = 0
        said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0
        faster = null
        drawRoutes()
        toast(ctx.getString(R.string.toast_043), 2500)
    }

    // ---- automatic rerouting: a faster way saving at least the set minutes is taken straight away, with a card to go back ----

    /** The route given up by the last automatic switch, for "Alte Route" on the card. */
    private var undoRoute: Route? = null
    /** After going back to the old route, no automatic switch for a while (a faster way is still offered). */
    private var autoPausedUntil = 0L

    private fun autoOn() = prefs.b(PreferenceKeys.AUTO_REROUTE, true) && System.currentTimeMillis() > autoPausedUntil
    private fun autoMin() = prefs.i(PreferenceKeys.AUTO_REROUTE_MIN, 5).coerceAtLeast(1)

    private fun announceAuto(old: Route, saved: Int) {
        undoRoute = old
        speak(Lang.t("Schnellere Route gefunden. Die Route wurde geändert und spart etwa $saved Minuten.",
            "Faster route found. Your route has been changed, saving about $saved minutes."), true)
        val n = Notice("reroute", Lang.t("Route geändert", "Route changed"),
            Lang.t("Spart etwa $saved Min. bis zum Ziel", "Saves about $saved min to your destination"), 1, Lang.t("Alte Route", "Old route"))
        showNotice(n, 20_000L)
        scope.launch { delay(20_500L); if (notice !== n && undoRoute === old && route !== old) undoRoute = null }
    }

    /** The button on a notice card: "Alte Route" goes back to the way you were on before the automatic switch. */
    fun noticeAction() {
        val n = notice ?: return
        notice = null
        if (n.kind != "reroute") return
        val old = undoRoute ?: return
        undoRoute = null
        val now = System.currentTimeMillis()
        autoPausedUntil = now + 15 * 60_000L
        routes = listOf(old); active = 0
        said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0; liveSaid.clear()
        clearLiveAlts()
        lastFasterCheck = now
        drawRoutes()
        updateRouteZones()
        toast(Lang.t("Alte Route wieder aktiv", "Back on your previous route"), 2500)
        fix?.let { onPosition(it, true) }
    }

    fun dismissFaster() { faster = null; lastFasterCheck = System.currentTimeMillis() + 300_000L }

    private val liveSaid = HashSet<Int>()
    private var lastBeat = 0L

    /** While navigating, notes every 20 s that a route is running (with its stops), so a restart can carry straight on. */
    private fun saveResumeBeat() {
        val now = System.currentTimeMillis()
        if (now - lastBeat < 20_000L || previewing || simMode != 0) return
        lastBeat = now
        val d = dest ?: return
        try {
            val o = org.json.JSONObject(prefs.s(PreferenceKeys.RESUME, "{}"))
            o.put("n", d.name).put("s", d.sub).put("la", d.pos.lat).put("lo", d.pos.lon).put("b", now)
            if (!o.has("t")) o.put("t", now)
            val a = org.json.JSONArray()
            for (v in via) a.put(org.json.JSONObject().put("n", v.name).put("s", v.sub).put("la", v.pos.lat).put("lo", v.pos.lon))
            o.put("v", a)
            // the charging plan too: after a restart its stops still show as chargers, with their times
            val e = org.json.JSONArray()
            for (st in evPlanned) e.put(org.json.JSONObject().put("n", st.n.place.name).put("s", st.n.place.sub).put("la", st.n.place.pos.lat).put("lo", st.n.place.pos.lon)
                .put("kw", st.n.kw ?: -1).put("op", st.n.op ?: "").put("a", st.arrPct).put("t", st.toPct).put("m", st.minutes).put("p", st.preferred))
            o.put("e", e)
            prefs.put(PreferenceKeys.RESUME, o.toString())
        } catch (e: Exception) { }
    }

    /** Switches to one of the grey alternatives on the map. */
    fun switchToAlt(i: Int, auto: Boolean = false) {
        val a = liveAlts.getOrNull(i) ?: return
        val old = route; val oldProg = curProgress
        routes = listOf(a.route); active = 0
        said.clear(); offCount = 0; lastIdx = 0; trustProg = -1.0; liveSaid.clear()
        clearLiveAlts()
        lastFasterCheck = System.currentTimeMillis()
        drawRoutes()
        updateRouteZones()
        if (!auto) toast(if (a.diffMin < 0) Lang.t("Gewechselt: etwa ${-a.diffMin} Min. schneller", "Switched: about ${-a.diffMin} min quicker") else if (a.diffMin > 0) Lang.t("Gewechselt: etwa ${a.diffMin} Min. länger", "Switched: about ${a.diffMin} min longer") else Lang.t("Route gewechselt", "Switched route"), 3000)
        fix?.let { onPosition(it, true) }
        // the way you were going stays on the map as an alternative, with its card (the other way round: slower becomes faster)
        if (old != null && old !== a.route && prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) {
            val nr = a.route
            scope.launch {
                val back = withContext(Dispatchers.Default) { try { altsFor(nr, nr, listOf(old), curProgress, oldProg) } catch (e: Exception) { emptyList() } }
                    .map { LiveAlt(it.route, -a.diffMin, it.bubble, it.divergeOff, -a.traffic, it.divIdx) }
                if (route !== nr || back.isEmpty()) return@launch
                liveAlts = back
                map.setLiveAlts(back.map { it.line() }, back.map { it.bubble }, back.map { it.diffMin }, back.map { it.traffic })
            }
        }
    }

    /**
     * The other ways to go, as cards on the map: where each leaves your road, a spot a little way along its own road for its card,
     * the minutes it saves or costs compared with [same] (your way, worked out at the same moment) and how its traffic compares.
     */
    private fun altsFor(cur: Route, same: Route, others: List<Route>, progress: Double, altFrom: Double = 0.0): List<LiveAlt> = others.mapNotNull { alt ->
        // [altFrom]: how far along the other route you are (it may have started further back, like the way you just left)
        var div = -1
        for (i in alt.pts.indices step 2) {
            if (alt.cum[i] < altFrom) continue
            if (alt.cum[i] > altFrom + 60_000.0) break
            if (RouteExtras.project(cur, alt.pts[i], 35.0, progress) == null) { div = i; break }
        }
        if (div < 0 || cur.pts.isEmpty()) return@mapNotNull null
        // the line starts right where it leaves your road (back to its last point that's still on it), so the two lines join up
        var join = div
        while (join > 0 && alt.cum[join - 1] >= altFrom && RouteExtras.project(cur, alt.pts[join - 1], 6.0, progress) == null) join--
        if (join > 0) join--
        val divOff = RouteExtras.project(cur, alt.pts[max(0, div - 2)], 60.0, progress)?.first ?: progress
        val own = alt.cum[div]
        // the card a short way along it, so it's on screen near the turn-off (it used to be miles away, off the screen)
        val bi = alt.cum.indexOfFirst { it >= own + min(320.0, (alt.len - own) * 0.3) }.let { if (it < 0) alt.pts.size / 2 else it }
        // traffic: the delay on it compared with the delay on your way (two minutes either way counts as similar)
        val tr = when { alt.delay < same.delay - 120.0 -> -1; alt.delay > same.delay + 120.0 -> 1; else -> 0 }
        LiveAlt(alt, Math.round((alt.time - same.time) / 60.0).toInt(), alt.pts[bi], divOff, tr, join)
    }

    /**
     * Setting off with alternatives on the map: they get their cards straight away (they used to stay as plain grey lines until
     * the first check for a faster way, a few minutes in).
     */
    private fun cardsForStartAlts() {
        val cur = route ?: return
        if (!prefs.b(PreferenceKeys.FASTER_ROUTE, true) || !prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) return
        val others = routes.filter { it !== cur }
        if (others.isEmpty()) return
        scope.launch {
            val list = withContext(Dispatchers.Default) { try { altsFor(cur, cur, others, 0.0) } catch (e: Exception) { emptyList() } }
            if (route !== cur || previewing || list.isEmpty()) return@launch
            liveAlts = list
            // the plain grey lines make way for the ones with cards
            map.setRoute(cur.pts, emptyList(), cur.sev)
            map.setTunnels(cur.tunnels.map { (a, b) -> slice(r = cur, a = a, b = b) })
            map.setLiveAlts(list.map { it.line() }, list.map { it.bubble }, list.map { it.diffMin }, list.map { it.traffic })
        }
    }

    private fun clearLiveAlts() { if (liveAlts.isNotEmpty()) liveAlts = emptyList(); map.setLiveAlts(emptyList(), emptyList(), emptyList(), emptyList()) }

    /** Drops alternatives whose turn-off is already behind you. */
    private fun pruneLiveAlts(progress: Double) {
        val l = liveAlts
        if (l.isEmpty()) return
        val keep = l.filter { it.divergeOff > progress - 15.0 }
        if (keep.size != l.size) {
            liveAlts = keep
            map.setLiveAlts(keep.map { it.line() }, keep.map { it.bubble }, keep.map { it.diffMin }, keep.map { it.traffic })
        }
    }

    private fun updateGuidance(progress: Double, light: Boolean = false) {
        val r = route ?: return
        val rem = max(0.0, r.len - progress)
        val frac = if (r.len > 0) rem / r.len else 0.0
        if (!light) {
            try { checkIncidents(r, progress) } catch (e: Exception) { }
            fix?.let { f -> try { checkYieldSigns(f.pos, f.heading) } catch (e: Exception) { } }
            fix?.let { f -> if (via.isNotEmpty() && Geo.dist(f.pos, via[0].pos) < viaReachM()) { toast(str(R.string.rest_432, via[0].name), 3000); val reached = via[0].pos
                // a charging stop reached: from here the battery counts from the charge planned there (until the car reports a newer level)
                evPlanned.firstOrNull { Geo.dist(it.n.place.pos, reached) < 30.0 }?.let { st -> evChargeRefPct = st.toPct.toDouble(); evChargeRefAt = System.currentTimeMillis(); evSinceCharge = 0.0; evRefPct = -1; evSave() }
                evPlanned = evPlanned.filter { Geo.dist(it.n.place.pos, reached) >= 30.0 }; via = via.drop(1); pushVia() } }
            if (via.isEmpty()) checkParking(r, rem)
            curProgress = progress
            // time left = what the rest of the road takes with no traffic + the delays that are still ahead of you
            val ahead = if (r.delays.isEmpty()) r.delay * frac else r.delays.sumOf { d ->
                val span = max(1.0, d.end - d.off)
                d.secs * ((d.end - max(progress, d.off)) / span).coerceIn(0.0, 1.0)
            }
            // a motorhome or trailer is slower than the limit on fast roads (Settings > Fahrzeug)
            // the free-flowing part at your own pace (learned on stretches without traffic, at most 15 % either way)
            trackPace(r, progress)
            // electric: the charging stops still ahead, each with its charging time and about 5 minutes to get off, plug in and back on
            val charge = if (isEv) evPlanned.filter { st -> via.any { Geo.dist(it.pos, st.n.place.pos) < 30.0 } }.sumOf { (it.minutes + 5) * 60.0 } else 0.0
            val secs = r.freeFlow * frac * etaPace() + ahead + Vehicle.extraSecs(r.limits, progress, Vehicle.active.maxMs) + charge
            val delayMin = Math.round(ahead / 60.0).toInt()
            val arrive = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + (secs * 1000).toLong()))
            eta = EtaInfo(arrive, rem, secs, delayMin, charge)
            routeFrac = (1.0 - frac).toFloat().coerceIn(0f, 1f)
            pruneLiveAlts(progress)
            saveResumeBeat()
            checkFaster(r, secs)
            try { checkTraffic(r) } catch (e: Exception) { }
            try { tripShareTick() } catch (e: Exception) { }
        }

        if (!light && !shadow()) try { routeExtrasTick(r, progress) } catch (e: Exception) { android.util.Log.w("NavEngine", "route extras failed", e) }
        val ins = r.steps
        var k = ins.indexOfFirst { it.off > progress + 4 }
        if (k < 0) k = ins.size - 1
        if (k >= 0) {
            val cur0 = ins[k]
            // a slip road's turn: until the road leaves the main road it counts down to there, and reads as the exit it starts with
            val slipStart = cur0.fork >= 0 && progress < cur0.fork - 5.0 && !cur0.maneuver.uppercase().startsWith("ARRIVE")
            val cur = if (slipStart) cur0.copy(maneuver = slipExitManeuver(cur0.maneuver)) else cur0
            val d = max(0.0, (if (slipStart) cur0.fork else cur0.off) - progress)
            val arriving = cur.maneuver.uppercase().startsWith("ARRIVE")
            val side = if (arriving) arrivalSide(r, cur.maneuver) else ""
            val nxt = ins.getOrNull(k + 1)
            val street = if (arriving) (dest?.name ?: Lang.t("Ziel", "Destination")) + (if (side.isNotEmpty()) Lang.t(" · $side", " · on the " + (if (side == "links") "left" else "right")) else "") else cur.street.ifBlank { Geo.maneuverText(cur.maneuver, cur.roundExit).replaceFirstChar { it.uppercase() } }
            // several lane sections can touch one junction (the road often widens on the approach): use the one with the most lanes, then the nearest to the junction
            // on motorways and dual carriageways the lanes are looked at (and shown, and spoken) much earlier, as there's further to move over
            val fastRoad = cur.exitNo.isNotBlank() || (fix?.let { f -> try { map.roadAt(f.pos, f.heading)?.cls } catch (e: Throwable) { null } }?.let { it.startsWith("motorway") || it.startsWith("trunk") } == true)
            laneFast = fastRoad
            // only sections that belong to this junction: with a second turn close behind (right, then left 80 m on), that turn's
            // lane section also ends within 200 m and often has more lanes, so it was shown too early (the left lane lit up where the right
            // one is needed). A section belongs to the step its end is nearest to.
            val realSec = if (d < (if (fastRoad) 1900 else 1000) && !arriving) r.lanes.filter {
                val dc = abs(it.end - cur.off)
                // only the lanes of this very junction: the section must end within 30 m of it, and no other junction on the route is nearer to
                // its end (two slip roads close together used to light the first one's lanes while the route took the second)
                progress < it.end + 5 && abs(it.end - cur.off) < 30.0 && ins.minByOrNull { st -> abs(st.off - it.end) }?.off == cur.off
            }.sortedWith(compareByDescending<LaneSec> { it.lanes.size }.thenBy { abs(it.end - cur.off) }).firstOrNull() else null
            val laneSec = realSec
            val laneAdv = laneSec?.let { Geo.laneAdvice(it, cur.maneuver) }
            // the early lane call on fast roads, about a mile out (once per junction): "In about a mile, move into the left two lanes"
            if (fastRoad && laneAdv != null && d in 1250.0..1850.0 && earlyLaneFor != cur.off) {
                earlyLaneFor = cur.off
                val dist = if (imperial) Lang.t("In etwa einer Meile", "In about a mile") else Lang.t("In etwa eineinhalb Kilometern", "In about one and a half kilometres")
                speak(dist + " " + laneAdv.replaceFirst("Links ", "links ").replaceFirst("Rechts ", "rechts ").replaceFirst("Mittlere ", "die mittleren "))
            }
            updateLaneMarks(r, cur, laneSec, d)
            updateTurnArrow(r, k, cur, d, arriving)
            // (after an unnamed slip road the step's "street" is the exit sign's text, not a road: the map's own name is used then)
            onRoad = ins.getOrNull(k - 1)?.let { st -> if (st.exitNo.isNotBlank() || st.maneuver.uppercase().contains("RAMP")) "" else st.street } ?: ""
            refreshRoad()
            // the turn after this one: only when it comes soon after it (about 15 seconds at your speed, and never less than 300 m),
            // unless set to always show or never
            val gap = if (nxt != null) nxt.off - cur.off else Double.MAX_VALUE
            val thenOn = nxt != null && !arriving && when (prefs.s(PreferenceKeys.THEN_MODE, "close")) {
                "always" -> true
                "off" -> false
                else -> gap <= max(300.0, (fix?.speed ?: 0.0) * 15.0)
            }
            // the motorway junction there, by name: "Frankfurter Kreuz", "Ausfahrt 17 Weiterstadt"
            val jc = if (arriving) null else junctionAt(r, cur)
            val newTurn = TurnInfo(
                d, street, cur.maneuver, if (thenOn) nxt?.maneuver else null,
                if (thenOn && nxt != null) Lang.t("Dann ", "Then ") + Geo.maneuverText(nxt.maneuver, nxt.roundExit) + (if (nxt.street.isNotBlank()) Lang.t(" auf ${nxt.street}", " onto ${nxt.street}") else "") else "",
                laneSec, Geo.stepTag(cur), laneAdv, cur.roundExit, cur.sign,
                exitNo = cur.exitNo, rbIn = cur.rbIn, rbOut = cur.rbOut, rbExits = cur.rbExits, thenGap = if (thenOn) gap else 0.0, thenExit = if (thenOn) (nxt?.roundExit ?: 0) else 0,
                junction = jc?.let { Junctions.label(it.first, it.second) }.orEmpty(), junctionName = jc?.first.orEmpty(),
            )
            // only publish a new turn card when something a driver can see changed (about 3 m of distance), so the screen is not redrawn four times a second for nothing
            val ot = turn
            if (ot == null || ot.maneuver != newTurn.maneuver || ot.street != newTurn.street || ot.lanes !== newTurn.lanes || ot.thenText != newTurn.thenText || ot.junction != newTurn.junction || abs(ot.dist - newTurn.dist) >= 3.0 || newTurn.dist < 30.0) turn = newTurn
            val word = Geo.maneuverText(cur.maneuver, cur.roundExit)
            // (Settings > Sound > Say road names off: just the direction, "In 200 metres, turn left")
            // a road number takes its article in German ("auf die A 4"); a street name is said as it is ("auf Hauptstraße")
            val tail = if (arriving || cur.street.isBlank() || !prefs.b(PreferenceKeys.VOICE_ROADS, true)) "" else Lang.t(if (RoadRefs.isRef(cur.street)) " auf die ${cur.street}" else " auf ${cur.street}", " onto ${cur.street}")
            val lane = if (laneAdv != null) ". $laneAdv" else ""
            if (onRoad.isNotBlank()) tripRoads.add(onRoad)
            // smarter voice: junction numbers, sign destinations and the next move when it follows quickly
            val phrase = if (arriving) word else smartPhrase(cur, nxt, word, tail, jc)
            // on roads you drive a lot the early heads-ups are skipped and the main one comes a little later
            val familiar = prefs.b(PreferenceKeys.SMARTVOICE, true) && onRoad.isNotBlank() && (roadFam[onRoad] ?: 0) >= 6
            // the call-outs come a set time before the turn, not at set distances: at 130 km/h much further out than at 50.
            // About 45 s ahead on fast roads (a heads-up), 15 s (the main one; less on roads you know) and 4 s (at the junction),
            // each kept within sensible distances
            val v = max(p0Speed(), 8.0)
            val nAt = (v * 4.0).coerceIn(60.0, 200.0)
            // (the main call about 30 s out: some 400 to 500 m in town; it used to come at 250 to 300 m, too late to get in lane)
            val fAt = (v * (if (familiar) 20.0 else 30.0)).coerceIn(if (familiar) 300.0 else 400.0, 1000.0)
            val eAt = (v * 45.0).coerceIn(900.0, 2200.0)
            if (!familiar && d <= eAt && d > fAt + 400.0 && p0Speed() > 18.0 && !arriving && said.add("${k}e"))
                speak("In ${Geo.spoken(d, imperial)}, $phrase", turn = true)
            if (d <= fAt && d > nAt + 60.0 && said.add("${k}f"))
                speak(if (arriving) "In ${Geo.spoken(d, imperial)} erreichen Sie Ihr Ziel" + (if (side.isNotEmpty()) ". Das Ziel liegt $side" else "") else "In ${Geo.spoken(d, imperial)}, $phrase$lane", turn = true)
            if (d <= nAt && said.add("${k}n"))
                // at the junction itself only the direction ("Links halten"): the names were said before, and a long call ran into the next
                speak(if (arriving) (if (side.isNotEmpty()) "Das Ziel liegt $side" else "Sie haben Ihr Ziel erreicht") else word.replaceFirstChar { it.uppercase() }, turn = true)
        }
        // arrived: at the end of the route and really there (not just level with the end of the route on another road)
        val atEnd = rem < 25 && fix.let { f -> f == null || Geo.dist(f.pos, dest?.pos ?: r.pts.last()) < 120.0 || Geo.dist(f.pos, r.pts.last()) < 120.0 }
        if (atEnd && said.add("end")) {
            toast(str(R.string.toast_060), 5000)
            if (navStartMs > 0) {
                val secsT = (System.currentTimeMillis() - navStartMs) / 1000.0
                val dd = max(0.0, tripDist - navStartDist)
                val sideA = try { arrivalSide(r, r.steps.lastOrNull()?.maneuver ?: "") } catch (e: Exception) { "" }
                val a = ArrivalInfo(dest, dest?.name ?: Lang.t("Ziel", "Destination"), dd, secsT, if (secsT > 0) dd / secsT else 0.0, sideA)
                arrival = a
                navStartMs = 0L
                arrivalMoment(a)
                scope.launch { delay(20_000); if (arrival === a) dismissArrival() }
            }
            arriveFlash = true
            scope.launch { delay(7000); arriveFlash = false }
            val doneRoute = r
            scope.launch { delay(4000); if (route === doneRoute || route == null) endRoute() }
        }
    }

    // ---- live rain radar ----
    private var radarJob: Job? = null
    private var radarOnNow = false

    /** Switches the rain radar layer on or off; while on it picks up the newest radar picture every 10 minutes. */
    /** A layer switched in the car's Ebenen list: shown or hidden on this engine's map straight away. */
    fun applyLayer(key: String) {
        when (key) {
            PreferenceKeys.TRAFFIC -> applyStyle(lastDark)
            PreferenceKeys.SATELLITE, PreferenceKeys.BUILDINGS_3D -> map.setView(prefs.b(PreferenceKeys.SATELLITE, false), prefs.b(PreferenceKeys.BUILDINGS_3D, true))
            PreferenceKeys.ROUTETRAFFICONLY -> map.setHideOtherTraffic(prefs.b(PreferenceKeys.ROUTETRAFFICONLY, false))
            PreferenceKeys.RAIN_RADAR -> setRadarOn(prefs.b(PreferenceKeys.RAIN_RADAR, false))
            PreferenceKeys.CAMERA_MAP -> mergeCams()
            PreferenceKeys.HAZARD_ICONS -> { hazardSig = -1; pushHazards(hzProgress) }
            PreferenceKeys.GARAGES_MAP -> {
                garagesFetchedPos = null
                if (!prefs.b(PreferenceKeys.GARAGES_MAP, true)) { garagesShown = false; map.setGarages(emptyList()) }
                else (if (following) fix?.pos else map.center())?.let { garagesAt(it, false) }
            }
        }
    }

    fun setRadarOn(on: Boolean) {
        if (on == radarOnNow) return
        radarOnNow = on
        radarJob?.cancel(); radarJob = null
        if (!on) { map.setRadar(null); return }
        radarJob = scope.launch {
            var warned = false
            while (true) {
                // the phone app in the background shows no map: the radar is fetched again once it is back in front
                if (!isCar && !appVisible) { delay(30_000L); continue }
                val url = withContext(Dispatchers.IO) {
                    try {
                        val c = java.net.URL("https://api.rainviewer.com/public/weather-maps.json").openConnection() as java.net.HttpURLConnection
                        c.connectTimeout = 10_000; c.readTimeout = 15_000
                        val body = c.inputStream.bufferedReader().use { it.readText() }
                        val o = org.json.JSONObject(body)
                        val host = o.optString("host", "https://tilecache.rainviewer.com")
                        val past = o.getJSONObject("radar").getJSONArray("past")
                        val path = past.getJSONObject(past.length() - 1).getString("path")
                        "$host$path/256/{z}/{x}/{y}/2/1_1.png"
                    } catch (e: Exception) { null }
                }
                if (url != null) map.setRadar(url)
                else if (!warned) { warned = true; toast(ctx.getString(R.string.toast_033), 3500) }
                delay(if (url != null) 600_000L else 120_000L)
            }
        }
    }

    /**
     * Predictive destinations: where you are probably heading now (a place you often drive to at this time, or work in the
     * morning and home later on), with the live journey time. Checked at most every 5 minutes, and only while idle.
     */
    fun refreshPrediction(force: Boolean = false) {
        if (!prefs.b(PreferenceKeys.PREDICT, true)) { prediction = null; return }
        if (route != null || previewing) return
        val now = System.currentTimeMillis()
        if (!force && now - predictionAt < 300_000L) return
        val here = fix?.pos ?: return
        predictionAt = now
        val cal = java.util.Calendar.getInstance()
        val hm = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val weekend = dow == java.util.Calendar.SATURDAY || dow == java.util.Calendar.SUNDAY
        val home = places.slot("home"); val work = places.slot("work")
        var cand: Place? = habits.suggest(now)
        if (cand == null && !weekend && work != null && hm in 360..630 && Geo.dist(here, work.pos) > 1500) cand = work
        if (cand == null && home != null && hm >= (if (weekend) 1020 else 930) && Geo.dist(here, home.pos) > 1500) cand = home
        val pl = cand
        if (pl == null || Geo.dist(here, pl.pos) < 800) { prediction = null; return }
        val title = when {
            home != null && Geo.dist(home.pos, pl.pos) < 150 -> Lang.t("Zuhause", "Home")
            work != null && Geo.dist(work.pos, pl.pos) < 150 -> Lang.t("Arbeit", "Work")
            else -> pl.name
        }
        scope.launch {
            var live = false
            val r = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                withContext(Dispatchers.IO) {
                    try {
                        if (hasOnline && online()) { live = true; netRoutes(here, pl.pos, false, 0L, emptyList(), driveHeading(here), true).firstOrNull() }
                        // (no offline routing for this: it loads large files into memory, too heavy for a suggestion in the background)
                        else null
                    } catch (e: Exception) { null }
                }
            }
            if (route != null) return@launch
            val mins = r?.let { Math.round(it.time / 60.0).toInt() } ?: -1
            val dmin = if (live && r != null) Math.round(r.delay / 60.0).toInt() else 0
            val note = when { r == null || !live -> ""; dmin >= 10 -> Lang.t("starker Verkehr", "heavy traffic"); dmin >= 3 -> Lang.t("etwas Verkehr", "some traffic"); else -> Lang.t("wenig Verkehr", "light traffic") }
            prediction = Prediction(pl, title, mins, dmin, note)
        }
    }

    /**
     * Drive times in minutes to Home and Work, for their tiles on the map page (0 = you're there; missing or -1 = not known, and the
     * tile shows the distance instead). Online only, so it never runs the offline router on the head unit in the background: at most
     * once every 10 minutes, sooner if you've moved 3 km or Home / Work changed. Two small route requests at a time.
     */
    val slotMins = androidx.compose.runtime.mutableStateMapOf<String, Int>()
    private var slotMinsAt = 0L
    private var slotMinsFrom: LL? = null
    private var slotMinsKey = ""
    private var slotMinsJob: Job? = null
    fun refreshSlotTimes() {
        if (route != null || previewing || !hasOnline) return
        val here = fix?.pos ?: return
        val home = places.slot("home"); val work = places.slot("work")
        val key = "${home?.pos?.lat},${home?.pos?.lon}|${work?.pos?.lat},${work?.pos?.lon}"
        val now = System.currentTimeMillis()
        val moved = slotMinsFrom?.let { Geo.dist(it, here) > 3000.0 } ?: true
        if (key == slotMinsKey && !moved && now - slotMinsAt < 600_000L) return
        if (slotMinsJob?.isActive == true) return
        if (key != slotMinsKey) slotMins.clear()
        slotMinsAt = now; slotMinsFrom = here; slotMinsKey = key
        slotMinsJob = scope.launch {
            for ((k, p) in listOf("home" to home, "work" to work)) {
                if (p == null) { slotMins.remove(k); continue }
                if (Geo.dist(here, p.pos) < 800.0) { slotMins[k] = 0; continue }
                val r = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                    withContext(Dispatchers.IO) {
                        try { if (online()) netRoutes(here, p.pos, false, 0L, emptyList(), driveHeading(here), true).firstOrNull() else null } catch (e: Exception) { null }
                    }
                }
                if (route != null) return@launch
                slotMins[k] = r?.let { Math.round(it.time / 60.0).toInt() } ?: -1
            }
        }
    }

    // ---- turn arrow on the route ----
    private var turnArrowKey = -1

    /** Draws a white arrow on the route through the next junction once it's within ~700 m (not for "carry straight on"). */
    private fun updateTurnArrow(r: Route, k: Int, cur: Step, d: Double, arriving: Boolean) {
        val m = cur.maneuver.uppercase()
        val wanted = prefs.b(PreferenceKeys.TURNARROWS, true) && !previewing && !arriving && d < 700.0 &&
            !(m == "STRAIGHT" || m == "FOLLOW" || m.startsWith("DEPART") || m == "ROUNDABOUT_STRAIGHT" || m == "ROUNDABOUT_CROSS")
        // (redrawn when the lane offset changes, so the arrow always carries on from the route line in your lane)
        val key = if (wanted) (System.identityHashCode(r) * 31 + k) * 31 + System.identityHashCode(laneLatAt) else -1
        if (key == turnArrowKey) return
        turnArrowKey = key
        if (!wanted) { map.setTurnArrow(emptyList()); return }
        // a roundabout needs a longer arrow (round to the exit); a plain turn a short one
        val back = 38.0
        // ...but never into the next manoeuvre: "turn right, then left" 20 m on drew the right turn's arrow round the left turn
        // too, so its head pointed left. It now ends a little before the next step (at least 10 m past this turn).
        val nextOff = r.steps.getOrNull(k + 1)?.off
        val fwdMax = if (m.startsWith("ROUNDABOUT")) 70.0 else 30.0
        val fwd = if (nextOff != null) min(fwdMax, max(10.0, nextOff - cur.off - 6.0)) else fwdMax
        val pts = ArrayList<LL>()
        var o = max(0.0, cur.off - back)
        val end = min(r.len, cur.off + fwd)
        val lat = laneLatAt
        fun at(off: Double): LL {
            val (p, brg) = pointAt(r, off)
            val l = lat?.invoke(off) ?: 0.0
            if (Math.abs(l) < 0.01) return p
            val rad = Math.toRadians(brg + 90.0)
            return LL(p.lat + l * Math.cos(rad) / 111320.0, p.lon + l * Math.sin(rad) / (111320.0 * Math.cos(Math.toRadians(p.lat))))
        }
        while (o < end) { pts += at(o); o += 4.0 }
        pts += at(end)
        map.setTurnArrow(pts)
    }

    /** The hazard radar: everything in the next two miles, nearest first. Only republished when what you would see changes. */
    private fun updateRadar(r: Route, progress: Double, pl: Placed) {
        if (bendsFor !== r) {
            bendsFor = r; bends = emptyList()
            scope.launch { val b = withContext(Dispatchers.Default) { try { routeBends(r) } catch (e: Exception) { emptyList() } }; if (bendsFor === r) bends = b }
        }
        val reach = 3219.0
        val out = ArrayList<RadarItem>()
        fun add(kind: String, off: Double, label: String) { val d = off - progress; if (d in -5.0..reach) out += RadarItem(kind, max(0.0, d), label) }
        if (camerasOn()) for ((_, off) in routeCams) add("camera", off, Lang.t("Blitzer", "Speed camera"))
        for ((inc, off, _) in incMatches) add(if (inc.cat.contains("work", true)) "works" else "incident", off, incidentLabel(inc))
        if (prefs.b(PreferenceKeys.AVGZONES, true)) for (a in pl.avg) add("avg", a.start, Lang.t("Abschnittskontrolle", "Average speed check"))
        val school = RouteExtras.schoolTime(System.currentTimeMillis())
        for (h in pl.hz) when (h.poi.kind) {
            "bump" -> if (prefs.b(PreferenceKeys.BUMPS, false)) add("bump", h.off, Lang.t("Bodenwelle", "Speed bump"))
            "school" -> if (prefs.b(PreferenceKeys.SCHOOLS, false)) add("school", h.off, if (school) Lang.t("Schule · Schulweg", "School · school run") else Lang.t("Schule", "School"))
            "crossing" -> add("crossing", h.off, Lang.t("Fußgängerüberweg", "Crossing"))
            "rail" -> if (prefs.b(PreferenceKeys.RAILFORD, false)) add("rail", h.off, Lang.t("Bahnübergang", "Level crossing"))
            "ford" -> if (prefs.b(PreferenceKeys.RAILFORD, false)) add("ford", h.off, Lang.t("Furt", "Ford"))
        }
        for ((o, dir) in bends) add("bend", o, if (dir < 0) Lang.t("Linkskurve", "Bend left") else Lang.t("Rechtskurve", "Bend right"))
        out.sortBy { it.dist }
        val shown = out.take(14)
        var sig = shown.size
        for (e in shown) sig = sig * 31 + e.kind.hashCode() + (e.dist / 40.0).toInt()
        if (sig != radarSig) { radarSig = sig; radar = shown }
    }

    // British "A34", "M3", "A3(M)" and German "A 5", "B 27", "E 40" (with or without the space)
    private val refRx = Regex("\\b((?:[MABEKL]|St)\\s?[0-9]{1,4}(?:\\(M\\))?)(?![0-9A-Za-z])")

    /**
     * Road numbers on a sign ("A34", "M3", "A 5"), written as the country signs them; a European number ("E 40") only when the
     * sign has no national one, as that's the number people follow.
     */
    private fun signRefs(sign: String): List<String> {
        val all = refRx.findAll(sign).map { RoadRefs.format(it.groupValues[1].trim()) }.distinct().toList()
        return all.filter { !RoadRefs.isEuro(it) }.ifEmpty { all }
    }

    /** Place names on a sign, without the road numbers ("Winchester", "Basingstoke", "Frankfurt" from "A 5: Frankfurt"). */
    private fun signDests(sign: String): List<String> =
        sign.split(',', '/', ';', '|').map { refRx.replace(it, "").replace(Regex("[():]"), "").trim() }.filter { it.length >= 3 && it.any { ch -> ch.isLetter() } }.distinct()

    /** "take junction 9 towards Winchester, then keep left for the A34", from the route's step and sign data. */
    /** Junction names are used outside the UK and Ireland (there the junction number is what's signed and said). */
    private fun jctCountry(): Boolean { val c = detectedCountry.uppercase(); return c.isNotBlank() && c !in setOf("GB", "IE", "IM", "JE", "GG") }

    /** The motorway junction at a step ([Junctions.forStep]): its name and the exit number to go with it, or null. */
    private fun junctionAt(r: Route, s: Step): Pair<String, String>? {
        if (!jctCountry()) return null
        val on = jctsOn?.takeIf { it.first === r }?.second ?: emptyList()
        return try { Junctions.forStep(s, on)?.takeIf { it.first.isNotBlank() } } catch (e: Exception) { null }
    }

    private fun smartPhrase(cur: Step, nxt: Step?, word: String, tail: String, jc: Pair<String, String>? = null): String {
        if (!prefs.b(PreferenceKeys.SMARTVOICE, true)) return word + tail
        val sayRoads = prefs.b(PreferenceKeys.VOICE_ROADS, true)
        val m = cur.maneuver.uppercase()
        val exitLike = m.contains("EXIT") || m.contains("KEEP") || m.contains("BEAR") || m.contains("RAMP")
        // "junction 9" is how it's said in the UK and Ireland; everywhere else it's "exit 9"
        val jWord = if (detectedCountry.ifBlank { "GB" }.uppercase() in setOf("GB", "IE", "IM", "JE", "GG")) "junction" else "exit"
        // the junction by name: "am Frankfurter Kreuz rechts halten", "Ausfahrt 17 Weiterstadt nehmen", "an der Ausfahrt 17 Weiterstadt rechts halten"
        val inter = jc != null && Junctions.isInterchange(jc.first)
        var main = when {
            jc != null && inter -> Junctions.at(jc.first, jc.second) + " " + word
            jc != null && ((cur.exitNo.isNotBlank() && exitLike) || m.contains("EXIT") || m.contains("RAMP")) -> Junctions.take(jc.first, jc.second)
            jc != null -> Junctions.at(jc.first, jc.second) + " " + word
            cur.exitNo.isNotBlank() && exitLike -> "Ausfahrt ${cur.exitNo} nehmen"
            else -> word
        }
        // (not "Ausfahrt Weiterstadt in Richtung Weiterstadt")
        val dests = signDests(cur.sign).filter { jc == null || inter || !it.equals(jc.first, true) }
        val refs = signRefs(cur.sign)
        main += when {
            !sayRoads -> tail
            // one place from the sign is enough to find it ("links halten Richtung Frankfurt"); two and the road number made it long
            dests.isNotEmpty() -> " Richtung " + dests[0]
            // (onto the road: "rechts halten auf die A 3", not "auf der", which means staying on it)
            refs.isNotEmpty() && exitLike -> " auf die ${refs[0]}"
            else -> tail
        }
        if (nxt != null && nxt.off - cur.off in 1.0..250.0 && !nxt.maneuver.uppercase().startsWith("ARRIVE")) {
            val target = signRefs(nxt.sign).firstOrNull() ?: nxt.street
            val forWhat = when {
                target.isBlank() || !sayRoads -> ""
                refRx.matches(target) -> " auf die $target"
                else -> " auf $target"
            }
            main += ", dann " + Geo.maneuverText(nxt.maneuver, nxt.roundExit) + forWhat
        }
        return main
    }

    /** Counts the roads driven on this trip into the familiar-roads list (kept to the 300 most driven). */
    private fun flushRoads() {
        if (tripRoads.isEmpty()) return
        if (simMode != 0) { tripRoads.clear(); return }   // the demo drive doesn't count
        for (r in tripRoads) roadFam[r] = (roadFam[r] ?: 0) + 1
        tripRoads.clear()
        val top = roadFam.entries.sortedByDescending { it.value }.take(300)
        val o = org.json.JSONObject()
        for ((k, v) in top) o.put(k, v)
        prefs.put(PreferenceKeys.ROADFAM, o.toString())
    }

    /** The arrival moment: the camera swoops down to the destination, and the nearest car park is looked up. */
    private fun arrivalMoment(a: ArrivalInfo) {
        parkMinutes = 0
        stopTripShare(quiet = true)
        arrivalParking = null
        val d = a.place?.pos ?: return
        if (prefs.b(PreferenceKeys.ARRIVALFX, true)) {
            following = false
            recentreToken++
            map.cameraTo(d, fix?.heading ?: map.cameraNow()?.get(2), 58.0, 17.4, 2600, 0.25)
        }
        if (simMode != 0) return
        scope.launch {
            val list = kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                withContext(Dispatchers.IO) {
                    val live = async { try { ParkingLive.around(d, 1000) } catch (e: Exception) { null } }
                    val found = try { if (hasOnline && online()) netPoiNearby("parking", d, 700) else Overpass.nearby("parking", d, 700) }
                        catch (e: Exception) { try { Overpass.nearby("parking", d, 700) } catch (e2: Exception) { emptyList() } }
                    ParkingLive.enrich(found.map { it.copy(dist = Geo.dist(d, it.place.pos)) }, live.await().orEmpty(), d, 800.0)
                }
            }
            // the nearest one with free spaces (a full one only when there's nothing else)
            if (arrival === a) arrivalParking = list?.let { ParkingLive.sortNearby(it).firstOrNull() }
        }
    }

    /** Parkuhr: the minutes chosen on the arrival card (0 none), so the card shows which one runs. */
    var parkMinutes by mutableStateOf(0); private set

    /** Starts (or, tapped again, stops) the parking time; the reminder comes 10 minutes before it ends. */
    fun parkTimer(minutes: Int) {
        if (parkMinutes == minutes) { ParkTimer.cancel(ctx, prefs); parkMinutes = 0; toast(Lang.t("Parkzeit aufgehoben", "Parking time cancelled"), 2000); return }
        val car = fix?.pos ?: arrival?.place?.pos
        ParkTimer.start(ctx, prefs, minutes, car)
        parkMinutes = minutes
        val end = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + minutes * 60_000L))
        toast(Lang.t("Parkzeit bis $end: Erinnerung ${ParkTimer.BEFORE_MIN} Min. vorher", "Parked until $end: reminder ${ParkTimer.BEFORE_MIN} min before"), 3000)
    }

    /** Closes the arrival card and goes back to following the car. */
    fun dismissArrival() {
        arrival = null; arrivalParking = null
        if (!following && route == null) recenter()
    }

    /** "left" or "right" for the side of the road the destination is on, from the route's own instruction or from where the pin is. */
    private fun arrivalSide(r: Route, maneuver: String): String {
        val m = maneuver.uppercase()
        if (m.endsWith("LEFT")) return "links"
        if (m.endsWith("RIGHT")) return "rechts"
        val d = dest?.pos ?: return ""
        if (r.pts.size < 2) return ""
        val a = r.pts[r.pts.size - 2]; val b = r.pts.last()
        if (Geo.dist(a, b) < 2.0 || Geo.dist(b, d) < 8.0) return ""
        val kx = cos(Geo.rad(b.lat)) * 111320.0
        val cross = ((b.lon - a.lon) * kx) * ((d.lat - a.lat) * 110574.0) - ((b.lat - a.lat) * 110574.0) * ((d.lon - a.lon) * kx)
        return if (cross > 0) "links" else "rechts"
    }

    // ---- arrive by ----
    /** Works out when to leave the chosen destination's route to arrive at [arriveMs], and reminds you when it is time. */
    fun planArriveBy(arriveMs: Long) {
        val d = dest ?: return
        val from = fix?.pos ?: map.center() ?: return
        val r = route
        val viaPts = via.map { it.pos }
        scope.launch {
            val leave = withContext(Dispatchers.IO) {
                try { if (hasKey && online()) TomTomRest.departureFor(apiKey, from, d.pos, viaPts, arriveMs) else null } catch (e: Exception) { null }
            } ?: (arriveMs - ((r?.time ?: 1800.0) * 1000).toLong() - 5 * 60_000L)
            val p = PlannedTrip(d, arriveMs, leave)
            planned = p
            savePlanned()
            // back to the map, where the planned card shows the leave time
            if (previewing) endRoute()
            val f = SimpleDateFormat("HH:mm", Locale.UK)
            toast(if (leave <= System.currentTimeMillis()) "Jetzt losfahren, um ${f.format(Date(arriveMs))} anzukommen" else "Um ${f.format(Date(leave))} losfahren, um ${f.format(Date(arriveMs))} anzukommen. Ich erinnere Sie.", 5000)
            watchPlanned()
        }
    }

    fun cancelPlanned() { planned = null; savePlanned(); plannedJob?.cancel() }

    /** Starts the planned trip now. */
    fun startPlanned() { val p = planned ?: return; cancelPlanned(); startRoute(p.dest) }

    private fun savePlanned() {
        val p = planned
        prefs.put(PreferenceKeys.PLANNED, if (p == null) "{}" else org.json.JSONObject().put("n", p.dest.name).put("s", p.dest.sub).put("la", p.dest.pos.lat).put("lo", p.dest.pos.lon).put("a", p.arriveBy).put("l", p.leaveBy).toString())
    }

    /** Checks the planned trip twice a minute: refreshes the leave time with live traffic half an hour before, and gives the reminders. */
    private fun watchPlanned() {
        plannedJob?.cancel()
        plannedJob = scope.launch {
            var warned10 = false; var warned0 = false; var refreshed = false
            while (true) {
                val p = planned ?: break
                val now = System.currentTimeMillis()
                if (now > p.arriveBy) { cancelPlanned(); break }
                if (!refreshed && now > p.leaveBy - 35 * 60_000L && hasKey && online()) {
                    refreshed = true
                    val from = fix?.pos
                    if (from != null) {
                        val l = withContext(Dispatchers.IO) { try { TomTomRest.departureFor(apiKey, from, p.dest.pos, emptyList(), p.arriveBy) } catch (e: Exception) { null } }
                        if (l != null && planned === p) { planned = PlannedTrip(p.dest, p.arriveBy, l); savePlanned() }
                    }
                }
                val cur = planned ?: break
                val busyDriving = route != null && !previewing
                if (!busyDriving && !warned10 && now >= cur.leaveBy - 10 * 60_000L && now < cur.leaveBy) {
                    warned10 = true
                    raiseAlert("plan", Lang.t("In 10 Minuten losfahren nach ${cur.dest.name}", "Leave in 10 minutes for ${cur.dest.name}"), 7000)
                    speak(Lang.t("Fahren Sie in etwa 10 Minuten los nach ${cur.dest.name}", "Leave in about 10 minutes for ${cur.dest.name}"), true)
                }
                if (!busyDriving && !warned0 && now >= cur.leaveBy) {
                    warned0 = true
                    chime(android.media.ToneGenerator.TONE_PROP_PROMPT)
                    raiseAlert("plan", Lang.t("Zeit zum Losfahren nach ${cur.dest.name}", "Time to leave for ${cur.dest.name}"), 8000)
                    speak(Lang.t("Es ist Zeit, nach ${cur.dest.name} loszufahren", "It's time to leave for ${cur.dest.name}"), true)
                }
                delay(30_000)
            }
        }
    }

    // ---- smart fuel stop ----
    /** On a long trip, finds a good place to fill up around the middle of the route: the cheapest (with Fuel Finder) or the quickest detour. */
    // ---- electric cars: battery estimate ----
    val isEv: Boolean get() = prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV"
    /** Usable battery (kWh): the chosen car's, else from Settings > Fuel (60 if not set). */
    private fun evBatteryKwh(): Double = EvCars.batteryKwh(prefs) ?: (prefs.s(PreferenceKeys.EV_BATTERY_KWH, "").replace(',', '.').toDoubleOrNull() ?: 60.0).coerceIn(10.0, 200.0)
    /** kWh used per km: from the efficiency in Settings > Fuel (miles per kWh), or a typical 0.18. */
    /** kWh per km from Settings > Effizienz, which is given in km per kWh (it was read as miles per kWh before: far too hopeful). */
    private fun evKwhPerKm(): Double = prefs.s(PreferenceKeys.EVMPK, "").replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..15.0 }?.let { 1.0 / it } ?: 0.18
    /** Roughly how much battery (%) you'll arrive with: the charge you set before the trip, less what's been driven and what's left. */
    /** The battery level (%) the car reported, if it was read in the last 6 hours, or null. */
    fun evNowPct(): Int? {
        if (!isEv) return null
        val v = prefs.s(PreferenceKeys.EV_NOW_PCT, "").toIntOrNull() ?: return null
        val age = evNowAgeMin() ?: return null
        return if (age <= 360 || evParkedSinceRead(age)) v.coerceIn(0, 100) else null
    }

    /**
     * Not driven since the last reading (the car simply stood): the reading stays true for up to a week. After 1 or 6 hours it
     * used to give way to an estimate (the range from the battery and the set efficiency, or the charge set before the trip),
     * and a parked car showed a clearly lower battery or range without moving.
     */
    private fun evParkedSinceRead(ageMin: Long): Boolean {
        if (ageMin > 7 * 24 * 60L) return false
        evEnsureLoaded()
        return evSinceRead / evBatteryKwh() * 100.0 < 0.5
    }

    /** Minutes since the battery level was last read, or null. */
    fun evNowAgeMin(): Long? = EvWidget.readAt(prefs).takeIf { it > 0 }?.let { (System.currentTimeMillis() - it) / 60_000L }?.takeIf { it >= 0 }  // a clock set back: not fresh

    /** The range in km the widget reported, if it was read in the last 60 minutes, or null. */
    fun evNowKm(): Int? {
        if (!isEv) return null
        val v = prefs.s(PreferenceKeys.EV_NOW_KM, "").toIntOrNull() ?: return null
        // a range under 5 km is no real reading (the car or widget sent 0 or nothing usable): treat it as unknown
        val age = evNowAgeMin() ?: return null
        return if ((age <= 60 || evParkedSinceRead(age)) && v >= 5) v.coerceAtMost(1500) else null
    }

    /** The range now and the full range, in km: the widget's own figures when they're fresh, else from the battery and efficiency. */
    private fun evRangeKm(): Pair<Double, Double> {
        val nowKm = evNowKm(); val nowPct = evNowPct()?.takeIf { evNowAgeMin()?.let { a -> a <= 60 || evParkedSinceRead(a) } == true }
        val full = if (nowKm != null && nowPct != null && nowPct > 0 && nowKm > 0) nowKm * 100.0 / nowPct else evBatteryKwh() / evKwhPerKm()
        // where the battery is now (the car's reading less what has been driven since, the charge at the last charging stop, or
        // the charge set before the trip less what has been driven on it): a plan made mid-trip starts from here, not from the start
        val readingWins = (nowKm != null || nowPct != null) && EvWidget.readAt(prefs) >= evChargeRefAt
        val start = when {
            readingWins && nowKm != null && nowPct == null -> nowKm - evSinceRead / evBaseKwhPerKm()
            else -> evNowEstimatePct() / 100.0 * full
        }
        return start.coerceAtLeast(0.0) to full
    }

    /** The charge at the last charging stop reached on this trip (%), when it was reached, and the kWh driven since. */
    private var evChargeRefPct = -1.0
    private var evChargeRefAt = 0L
    private var evSinceCharge = 0.0

    /** The height (m) the last climb or descent without a route was counted from, from the phone's GPS. */
    private var evAltRef: Double? = null
    /** The distance driven since the app began (kept over trips and restarts, unlike tripDist), when the state was last saved, and whether it is loaded. */
    private var evDriven = 0.0
    private var evSavedAt = 0L
    private var evLoaded = false
    /** The battery level at the previous reading (to see a rise, that is a charge), -1 when none yet. */
    private var evPrevPct = -1
    /** True when a stretch of the drive since the reference reading could not be measured (a gap off the route): no learning from it. */
    private var evGapBad = false

    /**
     * The learning state, by name (JSON): a field added later is simply missing in an older saved state and keeps its default,
     * instead of the whole state being thrown away as with the plain list of numbers before.
     */
    private fun evState(): org.json.JSONObject = org.json.JSONObject().apply {
        put("refPct", evRefPct); put("refDist", evRefDist); put("refModel", evRefModel)
        put("refModelB", org.json.JSONArray(evRefModelB.toList())); put("refAux", evRefAux); put("refUp", evRefUp); put("refDown", evRefDown)
        put("refKm", org.json.JSONArray(evRefKm.toList())); put("readSeen", evReadSeen); put("sinceRead", evSinceRead); put("sinceNav", evSinceNav)
        put("sinceCharge", evSinceCharge); put("driven", evDriven); put("chargeRefPct", evChargeRefPct); put("chargeRefAt", evChargeRefAt)
        put("gapBad", evGapBad); put("prevPct", evPrevPct)
        put("chargeReadAt", evChargeReadAt); put("chargeLastPct", evChargeLastPct); put("charging", evChargingNow); put("chargingAt", evChargingAt)
    }

    /** Saved to the settings, so the reference reading and the drive since it survive a restart of the app. */
    /**
     * Whether this engine learns and saves. With Android Auto two engines run (the car's and the phone's, which can follow the same
     * trip): both saw the same readings, and each learning step was applied twice. Only the car's learns while it is connected.
     */
    private fun evLearner(): Boolean = isCar || !RouteSync.carConnected()

    private fun evSave() {
        if (!evLearner()) return
        // never before the saved state is read: that would write over it with empty values
        evEnsureLoaded()
        evSavedAt = System.currentTimeMillis()
        try { prefs.put(PreferenceKeys.EV_STATE, evState().toString()) } catch (e: Exception) { }
    }

    /** Loaded once, before first use: the state of the last drive (also the older plain list of 21 numbers). */
    private fun evLoad() {
        val raw = prefs.s(PreferenceKeys.EV_STATE, "").trim()
        if (raw.isEmpty()) return
        try {
            if (raw.startsWith("{")) {
                val o = org.json.JSONObject(raw)
                fun arr(k: String, into: DoubleArray) { o.optJSONArray(k)?.let { a -> for (i in into.indices) into[i] = a.optDouble(i, 0.0) } }
                evRefPct = o.optInt("refPct", -1); evRefDist = o.optDouble("refDist", 0.0); evRefModel = o.optDouble("refModel", 0.0)
                arr("refModelB", evRefModelB); evRefAux = o.optDouble("refAux", 0.0); evRefUp = o.optDouble("refUp", 0.0); evRefDown = o.optDouble("refDown", 0.0)
                arr("refKm", evRefKm); evReadSeen = o.optLong("readSeen", 0L); evSinceRead = o.optDouble("sinceRead", 0.0); evSinceNav = o.optDouble("sinceNav", 0.0)
                evSinceCharge = o.optDouble("sinceCharge", 0.0); evDriven = o.optDouble("driven", 0.0); evChargeRefPct = o.optDouble("chargeRefPct", -1.0)
                evChargeRefAt = o.optLong("chargeRefAt", 0L); evGapBad = o.optBoolean("gapBad", false); evPrevPct = o.optInt("prevPct", -1)
                evChargeReadAt = o.optLong("chargeReadAt", 0L); evChargeLastPct = o.optDouble("chargeLastPct", -1.0); evChargingAt = o.optLong("chargingAt", 0L)
                // still charging after a restart: only when it was seen within the last two hours (it ends when the car moves off)
                evChargingNow = o.optBoolean("charging", false) && System.currentTimeMillis() - evChargingAt in 0..2 * 3_600_000L
                return
            }
            // the older form: 21 numbers in a fixed order
            val v = raw.split(",").mapNotNull { it.toDoubleOrNull() }
            if (v.size != 21) return
            evRefPct = v[0].toInt(); evRefDist = v[1]; evRefModel = v[2]
            for (i in 0..2) evRefModelB[i] = v[3 + i]
            evRefAux = v[6]; evRefUp = v[7]; evRefDown = v[8]
            for (i in 0..2) evRefKm[i] = v[9 + i]
            evReadSeen = v[12].toLong(); evSinceRead = v[13]; evSinceNav = v[14]; evSinceCharge = v[15]
            evDriven = v[16]; evChargeRefPct = v[17]; evChargeRefAt = v[18].toLong(); evGapBad = v[19] > 0.5; evPrevPct = v[20].toInt()
        } catch (e: Exception) { }
    }
    /** Charging at a charger: the last battery reading seen (its time and level), and whether the level rose while standing there. */
    private var evChargeReadAt = 0L
    private var evChargeLastPct = -1.0
    private var evChargingNow = false
    /** When charging was last seen (a rise at a charger), to show it again after a restart of the app. */
    private var evChargingAt = 0L
    /** True while the car stands at a charger and the battery level has risen (until it moves off), also after a restart. */
    fun evCharging(): Boolean { evEnsureLoaded(); return evChargingNow }

    /** The range now in km, from the same estimate (null when there is no reading yet). */
    fun evEstimateKm(): Int? = Math.round(evRangeKm().first).toInt().takeIf { it > 0 }

    /** True when the last real reading is so old that the battery figure on screen is an estimate. */
    fun evEstimateOld(): Boolean = (evNowAgeMin() ?: Long.MAX_VALUE) > 10

    /**
     * The battery now, in %: the car's reading (60 minutes at most) less the modelled kWh driven since it; or, when a charging stop
     * was reached after that reading, the charge planned there less what has been driven since; or the charge set before the
     * trip less what has been driven since the start.
     */
    fun evNowEstimatePct(): Double {
        evEnsureLoaded()
        val batt = evBatteryKwh()
        // the last level read, up to 6 hours old, less what was used since (shown with "ca."): after an hour it used to fall back
        // to the charge set before the trip (80 %), even when the battery was much lower
        val nowPct = evNowPct()?.toDouble()
        val nowKm = evNowKm()
        val readAt = EvWidget.readAt(prefs)
        val fromReading = nowPct ?: nowKm?.let { km -> km * evBaseKwhPerKm() / batt * 100.0 }
        val pct = when {
            fromReading != null && readAt >= evChargeRefAt -> fromReading - evSinceRead / batt * 100.0
            evChargeRefPct >= 0 -> evChargeRefPct - evSinceCharge / batt * 100.0
            else -> prefs.i(PreferenceKeys.EV_CHARGE_PCT, 80).coerceIn(1, 100) - (if (navStartMs > 0) evSinceNav else 0.0) / batt * 100.0
        }
        return pct.coerceIn(0.0, 100.0)
    }

    // ---- electric: what the drive really uses (see EvModel) ----
    /** The modelled kWh driven since the battery level was last read, and since the route started. */
    private var evSinceRead = 0.0
    private var evSinceNav = 0.0
    private var evReadSeen = 0L
    /** For learning: the battery level, and the modelled kWh (and per kind of road, the km) since then. */
    private var evRefPct = -1
    private var evRefDist = 0.0
    private var evRefModel = 0.0
    /** The modelled road use since the reference reading, per kind of road, and the heating / air conditioning in it. */
    private val evRefModelB = DoubleArray(3)
    private var evRefAux = 0.0
    private var evTrackAt = 0L
    private val evRefKm = DoubleArray(3)
    /** Since the battery reading: the modelled kWh for climbing, and the kWh that going downhill could give back. */
    private var evRefUp = 0.0
    private var evRefDown = 0.0
    private var evLastH: Double? = null
    private var evHRoute: Route? = null
    /** How fast you drive against the limit on this trip (distance weighted), and where there's no limit. */
    private var evStyleSum = 0.0; private var evStyleM = 0.0
    private var evFreeSum = 0.0; private var evFreeM = 0.0
    /** How briskly you accelerate on this trip: the sum of acceleration x time while speeding up, and that time (s). */
    private var evAccSum = 0.0; private var evAccT = 0.0
    private var evLastSpeedMs = -1.0
    private var evTempC: Double? = null
    private var evTempAt = 0L

    private fun evBaseKwhPerKm(): Double {
        if (prefs.s(PreferenceKeys.EVMPK, "").isNotBlank()) return evKwhPerKm()
        // no efficiency set: the car's own figure (its range against its battery level). That figure already holds the speeds the car
        // was driven at lately (mixed driving), while the model's base is the use at 80 km/h and adds speed on top: so it is taken
        // as a tenth lower. It is kept (and only eased towards each new reading), so what is learned is corrected against a steady base
        prefs.s(PreferenceKeys.EV_BASE_GOM, "").toDoubleOrNull()?.takeIf { it in 0.08..0.4 }?.let { return it }
        evGomKwhPerKm()?.let { prefs.put(PreferenceKeys.EV_BASE_GOM, String.format(java.util.Locale.US, "%.4f", it)); return it }
        return 0.18
    }

    /** The car's own consumption figure (its range against its battery level, both fresh), normalised to 80 km/h; null when not known. */
    private fun evGomKwhPerKm(): Double? {
        val km = evNowKm(); val pct = evNowPct()?.takeIf { (evNowAgeMin() ?: Long.MAX_VALUE) <= 60 }
        if (km == null || pct == null || pct < 5) return null
        return (evBatteryKwh() * pct / 100.0 / km / 1.1).coerceIn(0.10, 0.35)
    }

    private fun evLearned(): DoubleArray = EvModel.parseLearned(prefs.s(PreferenceKeys.EV_LEARN, ""))

    /**
     * How much the way you accelerate costs against a usual driver: 1.0 for about 1.2 m/s² on average, more for a brisker one (up to
     * 1.35), less for a gentle one (down to 0.85). Not learned yet: 1.0.
     */
    private fun evAccelF(): Double {
        val a = if (evAccT > 30.0) evAccSum / evAccT else prefs.s(PreferenceKeys.EV_ACCEL, "").toDoubleOrNull() ?: return 1.0
        return (1.0 + 0.25 * (a - 1.2)).coerceIn(0.85, 1.35)
    }

    private fun evStyle(): Double = if (evStyleM > 3000) (evStyleSum / evStyleM).coerceIn(0.8, 1.3)
        else (prefs.s(PreferenceKeys.EV_STYLE, "").toDoubleOrNull() ?: 1.0).coerceIn(0.8, 1.3)

    private fun evFreeKmh(): Double = if (evFreeM > 3000) (evFreeSum / evFreeM).coerceIn(100.0, 250.0)
        else (prefs.s(PreferenceKeys.EV_FREE_KMH, "").toDoubleOrNull() ?: 140.0).coerceIn(100.0, 250.0)

    /** The outside temperature: the route's weather, else the weather here (fetched every 30 minutes while driving). */
    /** The temperature where you are: on a route the forecast nearest to here, else the trip's own figure. */
    private fun evTempHere(): Double? {
        val w = routeWx.takeIf { it.isNotEmpty() && route != null && !previewing }?.minByOrNull { abs(it.off - curProgress) }
        return w?.temp ?: evTemp()
    }

    private fun evTemp(): Double? {
        routeWx.takeIf { it.isNotEmpty() }?.let { w -> return w.map { it.temp }.average() }
        weather?.let { return it.tempC }
        return evTempC
    }

    /**
     * A gap in the GPS track (a tunnel, a lost signal) while on a route: the distance is the way along the route between the two
     * fixes, not the straight line. Only when the route agrees with the straight line (no jump to another road).
     */
    private fun evGapOnRoute(speed: Double, prev: LL, cur: LL, straight: Double): Boolean {
        val r = route ?: return false
        if (previewing || r.pts.size < 2) return false
        val a = RouteMatcher.snap(r, prev, 0); val b = RouteMatcher.snap(r, cur, 0)
        // both ends on the route, forwards, and near what the straight line allows (see TrackMath.gapAlong)
        val along = TrackMath.gapAlong(a.progress, a.dist, b.progress, b.dist, straight) ?: return false
        tripDist += along
        evTrack(speed, along)
        return true
    }

    /** The saved state is read before anything uses it: after a restart the estimate can be asked for before the first drive. */
    private fun evEnsureLoaded() { if (!evLoaded) { evLoaded = true; evLoad() } }

    /**
     * Standing still on a route (a jam, lights, a break with the car on): the heating or air conditioning still runs, by the time.
     * Not while charging (the charger powers it) and not without a route (the car may be switched off).
     */
    private fun evIdle() {
        if (!isEv) return
        evEnsureLoaded()
        // a new battery reading while standing: taken now (it used to wait for the next metres driven, and the estimate showed the
        // new reading less the use counted since the old one)
        if (EvWidget.readAt(prefs) != evReadSeen) { evTrack(0.0, 0.0); return }
        if (route == null || previewing || evChargingNow) { evTrackAt = 0L; return }
        evEnsureLoaded()
        val now = System.currentTimeMillis()
        val dtS = if (evTrackAt > 0) ((now - evTrackAt) / 1000.0).coerceIn(0.0, 60.0) else 0.0
        evTrackAt = now
        val aux = EvModel.auxKw(evTempHere()) * dtS / 3600.0
        if (aux <= 0.0) return
        evSinceRead += aux; evSinceNav += aux; evSinceCharge += aux
        if (evRefPct >= 0) evRefAux += aux
    }

    /** Every few metres driven (electric): the modelled use, how you drive against the limit, and learning from the battery level. */
    private fun evTrack(speedMs: Double, d: Double) {
        evEnsureLoaded()
        evDriven += d
        if (System.currentTimeMillis() - evSavedAt > 60_000L) evSave()
        val kmh = speedMs * 3.6
        // how briskly you speed up: the acceleration over the last few metres, while the speed rises (not at a standstill or past 120 km/h)
        val last = evLastSpeedMs
        evLastSpeedMs = speedMs
        if (last >= 0.0 && speedMs > 1.0 && speedMs < 33.0 && d > 0.5) {
            val dt = d / maxOf(1.0, (speedMs + last) / 2.0)
            val acc = (speedMs - last) / dt
            if (dt in 0.2..5.0 && acc in 0.4..4.0) { evAccSum += acc * dt; evAccT += dt }
        }
        // how you drive against the limit: for every car (it also goes into the arrival time and a fuel car's range)
        val lim = speedLimit
        if (lim != null && lim > 5.0 && speedMs > lim * 0.6 && speedMs > 8.0) { evStyleSum += speedMs / lim * d; evStyleM += d }
        else if (lim == null && noLimit && kmh > 90.0) { evFreeSum += kmh * d; evFreeM += d }
        if (!isEv) return
        // the road's own use (speed, cold, wind, rain), then the hill climbed or gone down since the last point
        val wx = routeWx.takeIf { it.isNotEmpty() && route != null }?.minByOrNull { abs(it.off - curProgress) }
        val flatK = d / 1000.0 * evBaseKwhPerKm() * EvModel.speedFactor(kmh) * EvModel.tempFactor(evTempHere()) *
            EvModel.vehicleFactor(kmh, evDragExtra(), evMass(), EvModel.massFor(evBatteryKwh())) *
            (if (wx != null) EvModel.wxFactor(EvModel.Wx(wx.off, wx.windKmh, wx.windFrom, wx.wet, wx.snow), kmh, fix?.heading?.toDouble()) else 1.0)
        // heating / air conditioning: by the time since the last point (also standing in a jam), at most a minute at a time
        val nowT = System.currentTimeMillis()
        val dtS = if (evTrackAt > 0) ((nowT - evTrackAt) / 1000.0).coerceIn(0.0, 60.0) else 0.0
        evTrackAt = nowT
        val aux = EvModel.auxKw(evTempHere()) * dtS / 3600.0
        val bk = EvModel.bucket(kmh)
        var up = 0.0; var down = 0.0
        val r = route; val prof = r?.elev
        if (r != null && prof != null && !previewing) {
            if (evHRoute !== r) { evHRoute = r; evLastH = null }
            val h = EvModel.Env(prof.off, prof.h).heightAt(curProgress)
            val last = evLastH
            if (h != null && last != null && abs(h - last) < 150.0) {
                val g = EvModel.gradeKwh(h - last, evMass(), 1.0)
                if (g >= 0) up = g else down = -g
            }
            evLastH = h
            evAltRef = null
        } else {
            // no route: the height from the phone's GPS, with a good fix only, and only a change of 5 m or more counts (the GPS height
            // wobbles by a few metres). A missing height comes as 0.
            evLastH = null
            val alt = fix?.takeIf { it.alt != 0.0 && it.acc <= 15.0 }?.alt
            val ref = evAltRef
            if (alt != null) {
                if (ref == null) evAltRef = alt
                else if (abs(alt - ref) in 5.0..150.0) { val g = EvModel.gradeKwh(alt - ref, evMass(), 1.0); if (g >= 0) up = g else down = -g; evAltRef = alt }
            }
        }
        // what the battery is counted down by: with the correction learned for this kind of road (the planning uses it too)
        val kwh = flatK * (evLearned().getOrNull(bk) ?: 1.0) + aux + up - down * evRegen()
        evSinceRead += kwh; evSinceNav += kwh; evSinceCharge += kwh
        if (evRefPct >= 0) { evRefModel += flatK; evRefModelB[bk] += flatK; evRefAux += aux; evRefUp += up; evRefDown += down; evRefKm[bk] += d / 1000.0 }
        val now = System.currentTimeMillis()
        // also without a route: the cold and the heating always count
        if (evTemp() == null && now - evTempAt > 30 * 60_000L) {
            evTempAt = now
            val at = fix?.pos
            if (at != null) scope.launch { evTempC = withContext(Dispatchers.IO) { try { Weather.fetch(at)?.tempC } catch (e: Exception) { null } } ?: evTempC }
        }
        // a new battery reading
        val readAt = EvWidget.readAt(prefs)
        if (readAt != evReadSeen) {
            evReadSeen = readAt
            evSinceRead = 0.0
            evSave()
            // the car's own figure eases the steady base a little (a new car or season shows over time, one odd reading doesn't)
            if (evLearner() && prefs.s(PreferenceKeys.EVMPK, "").isBlank()) evGomKwhPerKm()?.let { g ->
                val old = prefs.s(PreferenceKeys.EV_BASE_GOM, "").toDoubleOrNull() ?: g
                prefs.put(PreferenceKeys.EV_BASE_GOM, String.format(java.util.Locale.US, "%.4f", old * 0.8 + g * 0.2))
            }
            val pct = evNowPct() ?: return
            // the level went up since the previous reading: the car was charged in between, so the drive before it is not learned from
            val rose = evPrevPct >= 0 && pct > evPrevPct
            evPrevPct = pct
            if (rose) { evStartRef(pct); return }
            // standing at a charger: nothing is learned while the car is charging, the reference is kept at the charge
            if (evChargingNow) { evStartRef(pct); return }
            if (evRefPct < 0 || pct > evRefPct) { evStartRef(pct); return }   // first reading, or charged since
            val drop = evRefPct - pct
            if (drop >= 5 && evDriven - evRefDist >= 10_000 && evRefModel > 0.5 && !evGapBad) {
                // what the battery really lost against what the model said (without its buffer and corrections)
                val actual = drop / 100.0 * evBatteryKwh()
                // one learning step (see EvModel.learnStep: the recovery downhill is taken off, a mostly downhill stretch isn't learned)
                if (evLearner()) EvModel.learnStep(evLearned(), evRefModelB, actual, evRefAux, evRefUp, evRefDown)?.let { prefs.put(PreferenceKeys.EV_LEARN, EvModel.formatLearned(it)) }
                evStartRef(pct)
            }
        }
    }

    private fun evStartRef(pct: Int) { evRefPct = pct; evRefDist = evDriven; evGapBad = false; evRefModel = 0.0; evRefModelB.fill(0.0); evRefAux = 0.0; evRefUp = 0.0; evRefDown = 0.0; evRefKm.fill(0.0) }

    /**
     * Of the energy going downhill, the share that comes back: none. The estimate counts the full use on the hills, so it stays on
     * the safe side (also on mountain routes). The recovery is therefore not learned or shown any more.
     */
    private fun evRegen(): Double = 0.0

    /** The weight: an ordinary car of this battery, or the motorhome / car with trailer from Settings > Fahrzeug when that is heavier. */
    private fun evMass(): Double {
        val car = EvModel.massFor(evBatteryKwh())
        val v = Vehicle.from(prefs)
        return if (v.isCar) car else maxOf(car, v.weightKg.toDouble())
    }

    /** Extra air drag: a motorhome 70 %, a car with a trailer 60 %, none for a car. */
    private fun evDragExtra(): Double = when (Vehicle.from(prefs).kind) { "camper" -> 0.7; "lorry" -> 0.6; else -> 0.0 }

    /** The hills and the weather along [r], for the estimate. */
    private fun evEnv(r: Route): EvModel.Env {
        val prof = r.elev
        if (prof == null) evLoadElev(r)
        val wx = (if (route === r || routes.getOrNull(0) === r) routeWx else emptyList()).map { EvModel.Wx(it.off, it.windKmh, it.windFrom, it.wet, it.snow, it.temp) }
        return EvModel.Env(prof?.off ?: DoubleArray(0), prof?.h ?: DoubleArray(0), evMass(), evRegen(), wx, { off -> pointAt(r, off).second },
            evDragExtra(), EvModel.massFor(evBatteryKwh()), r.delays)
    }

    private var elevAskedFor: Route? = null

    /** Loads the heights along [r] once (in the background), when online. */
    private fun evLoadElev(r: Route) {
        if (r.elev != null || elevAskedFor === r || !online()) return
        elevAskedFor = r
        val tok = if (hasMapbox) mbToken else null
        scope.launch { r.elev = withContext(Dispatchers.IO) { try { Elevation.along(r, tok) { pointAt(r, it).first } } catch (e: Exception) { null } } }
    }

    /** Waits (up to 15 s) for the heights along [r], for planning the charging stops. */
    private suspend fun evElevNow(r: Route) {
        if (r.elev != null || !online()) return
        elevAskedFor = r
        val tok = if (hasMapbox) mbToken else null
        r.elev = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(15_000L) { try { Elevation.along(r, tok) { pointAt(r, it).first } } catch (e: Exception) { null } } }
    }

    /** Clears what's been learned about your driving and your car's use. */
    fun resetLearned() {
        listOf(PreferenceKeys.EV_LEARN, PreferenceKeys.EV_STYLE, PreferenceKeys.EV_ACCEL, PreferenceKeys.EV_FREE_KMH, PreferenceKeys.EV_REGEN, PreferenceKeys.ETA_PACE, PreferenceKeys.EV_BASE_GOM, PreferenceKeys.EV_STATE).forEach { prefs.put(it, "") }
        paceReal = 0.0; pacePred = 0.0
        evStyleSum = 0.0; evStyleM = 0.0; evFreeSum = 0.0; evFreeM = 0.0; evAccSum = 0.0; evAccT = 0.0
    }

    /** What's been learned, for Settings: (style ×, open-motorway km/h, regen share, use per town/country/motorway). */
    fun learnedSummary(): List<Pair<String, String>> {
        val l = evLearned()
        val out = ArrayList<Pair<String, String>>()
        out += Lang.t("Tempo gegenüber Limit", "Speed against the limit") to String.format(Locale.getDefault(), "%+.0f %%", (evStyle() - 1.0) * 100)
        out += Lang.t("Tempo ohne Limit", "Speed with no limit") to "${Math.round(evFreeKmh())} km/h"
        out += Lang.t("Fahrzeit gegenüber Routenplan", "Driving time against the route's") to String.format(Locale.getDefault(), "%+.0f %%", (etaPace() - 1.0) * 100)
        if (isEv) {
            out += Lang.t("Verbrauch Stadt / Land / Autobahn", "Use town / country / motorway") to l.joinToString(" / ") { String.format(Locale.getDefault(), "%+.0f %%", (it - 1.0) * 100) }
        }
        return out
    }

    // ---- your pace against the route's own time (for the arrival time) ----
    private var paceRoute: Route? = null
    private var paceProg = 0.0
    private var paceAt = 0L
    private var paceReal = 0.0
    private var pacePred = 0.0

    /**
     * Every 500 m or so on the route: the seconds it really took against the seconds the route said for it, counted only where
     * there's no traffic on that stretch and you didn't stop for long.
     */
    private fun trackPace(r: Route, progress: Double) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (paceRoute !== r || previewing || simMode != 0) { paceRoute = r; paceProg = progress; paceAt = now; return }
        val d = progress - paceProg
        if (d < 0.0 || d > 5000.0) { paceProg = progress; paceAt = now; return }
        if (d < 500.0) return
        val real = (now - paceAt) / 1000.0
        val pred = if (r.len > 0) r.freeFlow * d / r.len else 0.0
        val jam = r.delays.any { it.end > paceProg && it.off < progress } || SharedReports.items.any { it.kind == "jam" && fix?.let { f -> Geo.dist(it.pos, f.pos) < 3000.0 } == true }
        if (!jam && pred > 5.0 && real in pred * 0.5..pred * 2.0) { paceReal += real; pacePred += pred }
        paceProg = progress; paceAt = now
    }

    /** Your pace: this trip's once 5 minutes are counted, else what's been learned (1.0 = the route's own time). */
    private fun etaPace(): Double = if (pacePred >= 300.0) (paceReal / pacePred).coerceIn(0.85, 1.15)
        else (prefs.s(PreferenceKeys.ETA_PACE, "").toDoubleOrNull() ?: 1.0).coerceIn(0.85, 1.15)

    /** A route is over: how you drove against the limit is kept for the next trips (blended in). */
    private fun evTripDone() {
        // with Android Auto the car's engine learns from the trip; the phone following it would count it a second time
        if (!evLearner()) { paceReal = 0.0; pacePred = 0.0; paceRoute = null; evStyleSum = 0.0; evStyleM = 0.0; evFreeSum = 0.0; evFreeM = 0.0; evSinceNav = 0.0; evAccSum = 0.0; evAccT = 0.0; return }
        if (evStyleM > 5000) {
            val old = (prefs.s(PreferenceKeys.EV_STYLE, "").toDoubleOrNull() ?: 1.0)
            prefs.put(PreferenceKeys.EV_STYLE, String.format(java.util.Locale.US, "%.3f", (old * 0.6 + evStyleSum / evStyleM * 0.4).coerceIn(0.8, 1.3)))
        }
        if (evAccT > 120.0) {
            val old = prefs.s(PreferenceKeys.EV_ACCEL, "").toDoubleOrNull() ?: 1.2
            prefs.put(PreferenceKeys.EV_ACCEL, String.format(java.util.Locale.US, "%.3f", (old * 0.6 + evAccSum / evAccT * 0.4).coerceIn(0.4, 3.0)))
        }
        if (evFreeM > 5000) {
            val old = (prefs.s(PreferenceKeys.EV_FREE_KMH, "").toDoubleOrNull() ?: 140.0)
            prefs.put(PreferenceKeys.EV_FREE_KMH, String.format(java.util.Locale.US, "%.1f", (old * 0.6 + evFreeSum / evFreeM * 0.4).coerceIn(100.0, 250.0)))
        }
        if (pacePred >= 300.0) {
            val old = (prefs.s(PreferenceKeys.ETA_PACE, "").toDoubleOrNull() ?: 1.0)
            prefs.put(PreferenceKeys.ETA_PACE, String.format(java.util.Locale.US, "%.3f", (old * 0.6 + paceReal / pacePred * 0.4).coerceIn(0.85, 1.15)))
        }
        paceReal = 0.0; pacePred = 0.0; paceRoute = null
        evStyleSum = 0.0; evStyleM = 0.0; evFreeSum = 0.0; evFreeM = 0.0; evSinceNav = 0.0; evAccSum = 0.0; evAccT = 0.0
    }

    /** kWh the rest of route [r] takes from [from] metres, with your speed, the limits, the weather and what's been learned. */
    private fun evRouteKwh(r: Route, from: Double, avgKmh: Double, to: Double = r.len): Double =
        EvModel.kwh(from, to, r.limits, avgKmh.coerceIn(20.0, 140.0), evStyle(), evFreeKmh(), evBaseKwhPerKm(), evTemp(), evLearned(), evEnv(r), evAccelF())

    /** The last arrival estimate and what it was worked out from (see evArrivalPct). */
    private var arrPct: Int? = null
    private var arrAt = 0L
    private var arrProg = 0.0
    private var arrRoute: Route? = null
    private var arrRead = 0L
    private var arrChargeAt = 0L
    private var arrPlanned: Any? = null

    /**
     * Roughly how much battery (%) you'll arrive with. The screens ask for it on every redraw (each second, on the phone and in the
     * car), and it walks the whole rest of the route: it is worked out again only every 10 seconds or 200 m, or at once when the
     * route, the charging stops, a battery reading or a charge stop change.
     */
    fun evArrivalPct(): Int? {
        if (!isEv) return null
        val r = route ?: return null
        val now = System.currentTimeMillis()
        val readAt = EvWidget.readAt(prefs)
        if (arrRoute === r && arrPlanned === evPlanned && arrRead == readAt && arrChargeAt == evChargeRefAt &&
            now - arrAt < 10_000L && abs(curProgress - arrProg) < 200.0) return arrPct
        val v = evArrivalPctNow()
        arrPct = v; arrAt = now; arrProg = curProgress; arrRoute = r; arrRead = readAt; arrChargeAt = evChargeRefAt; arrPlanned = evPlanned
        return v
    }

    private fun evArrivalPctNow(): Int? {
        if (!isEv) return null
        val e = eta ?: return null
        val r = route ?: return null
        val batt = evBatteryKwh()
        // where the battery is now: the car's reading less what's been driven since it, else the charge set before the trip
        val startPct = evNowEstimatePct()
        val avgKmh = if (e.secs - e.chargeSecs > 30) e.dist / (e.secs - e.chargeSecs) * 3.6 else 60.0
        // the charging stops still ahead: the battery goes up to each stop's charge target, and the rest of the way is driven from there
        val here = curProgress
        val stops = evPlanned.mapNotNull { st -> RouteExtras.project(r, st.n.place.pos, 50.0, here)?.first?.takeIf { it > here }?.let { it to st } }
            .sortedBy { it.first }
        // the arrival estimate is kept on the safe side: 10 % more energy than the model says (the charge planning is unchanged)
        val safety = 1.10
        var pct = startPct; var at = here
        for ((off, st) in stops) {
            pct -= evRouteKwh(r, at, avgKmh, off) * safety / batt * 100.0
            pct = st.toPct.toDouble().coerceAtLeast(pct).coerceAtMost(100.0)
            at = off
        }
        pct -= evRouteKwh(r, at, avgKmh) * safety / batt * 100.0
        return Math.round(pct).toInt().coerceIn(0, 100)
    }

    /** The battery level or the charge to arrive with changed on the route options: look again for a charging stop. */
    fun refreshEvPlan() {
        val r = route ?: return
        val d = dest
        // the charging stops already on the trip came from the old battery / reserve: take them off and plan again (planEv only
        // plans while there are no stops, and adds the new ones once more)
        if (evPlanned.isNotEmpty() && d != null && prefs.b(PreferenceKeys.SMARTFUEL, true)) {
            val old = evPlanned
            val rest = via.filterNot { p -> old.any { Geo.dist(it.n.place.pos, p.pos) < 30.0 } }
            evAutoDest = null; evPlanWarn = null
            if (!previewing) { changeViaLive(rest, emptyList()) { route?.let { planEv(it) } }; return }
            via = rest; evPlanned = emptyList()
            startRoute(d, departMin, true, startNow = !previewing)
            return
        }
        fuelSuggestion = null; suggestFuel(r)
    }

    private var evKmAtPlan: Double? = null
    private var evDrivenAtPlan = 0.0
    private var evKmCheckAt = 0L
    private var evCheckBusy = false
    /** The destination a charging stop was added late for (the battery was lower than first thought): once per trip, so it can't fight your own changes. */
    private var evLateAddFor: LL? = null

    /**
     * Electric, while driving, every 3 minutes at most: is the plan still right? The range counts as the car's own reading or, when
     * that is old, the estimate from the last reading. Planned again when it differs clearly from what the drive since the last
     * plan explains (a charge, a wrong first reading), or when no stop is planned although the range falls short of the
     * destination's reserve (Settings). The route is only started afresh when the stops really change: it used to restart each time
     * the range counter had dropped 5 km (it drops as you drive), repeating announcements and losing the position on the route.
     */
    private fun checkEvRange() {
        if (!isEv || route == null || previewing || packBusy || evCheckBusy || !prefs.b(PreferenceKeys.SMARTFUEL, true)) return
        val now = System.currentTimeMillis()
        if (now - evKmCheckAt < 180_000L) return
        val r = route ?: return
        val d = dest ?: return
        evEnsureLoaded()
        val km = evNowKm()?.toDouble() ?: evRangeKm().first
        val drivenKm = (evDriven - evDrivenAtPlan) / 1000.0
        val base = evKmAtPlan
        if (base == null) { evKmAtPlan = km; evDrivenAtPlan = evDriven; return }
        val expected = base - drivenKm
        val changed = abs(km - expected) >= max(8.0, 0.2 * drivenKm)
        val destKm = prefs.i(PreferenceKeys.EV_DEST_KM, 50).coerceIn(0, 500).toDouble()
        val remKm = max(0.0, r.len - curProgress) / 1000.0
        val short = evPlanned.isEmpty() && km - remKm < destKm && evLateAddFor?.let { Geo.dist(it, d.pos) < 50.0 } != true
        if (!changed && !short) return
        evKmCheckAt = now; evKmAtPlan = km; evDrivenAtPlan = evDriven
        evCheckBusy = true
        scope.launch {
            try {
                evPlanJobs.remove(r)
                val (stops, warning) = computeEvPlan(r) ?: return@launch
                if (route !== r || previewing) return@launch
                val have = evPlanned
                if (have.isEmpty()) {
                    evWarning = warning
                    if (stops.isNotEmpty() && short) {
                        evLateAddFor = d.pos; evAutoDest = d.pos
                        speak(Lang.t("Der Akku reicht nicht bis zum Ziel. Ein Ladestopp wird eingeplant.", "The battery won't reach the destination. A charging stop is being added."), true)
                        addStops(stops, auto = true)
                    }
                    return@launch
                }
                // the stops it would pick now against the planned ones: the same, then nothing changes
                val same = stops.size == have.size && stops.all { n -> have.any { Geo.dist(it.n.place.pos, n.n.place.pos) < 300.0 } }
                if (same) return@launch
                // the new set of stops goes onto the route on the go (the guidance carries on)
                val rest = via.filterNot { p -> have.any { Geo.dist(it.n.place.pos, p.pos) < 30.0 } }
                val sorted = (rest + stops.map { it.n.place }).sortedBy { RouteExtras.project(r, it.pos, 5000.0)?.first ?: Double.MAX_VALUE }
                evAutoDest = d.pos; evPlanWarn = warning
                changeViaLive(sorted, stops)
            } finally { evCheckBusy = false }
        }
    }

    /**
     * Electric: the charging stops this trip needs. Each stop is the furthest charger reachable with the reserve left there, or
     * the furthest of a preferred operator (Settings) when that plan has as many stops and costs only a few minutes more.
     */
    /**
     * The charging plan for one route: the stops and a warning when the plan falls short. No stops (and no warning) when the
     * battery already gets you there; null when the route was dropped meanwhile or its data didn't come.
     */
    /** Each route's plan is worked out once, however many places ask for it (the route cards and the chosen route's stops), and all routes at the same time. */
    private val evPlanJobs = HashMap<Route, kotlinx.coroutines.Deferred<Pair<List<EvStop>, String?>?>>()

    private suspend fun computeEvPlan(r: Route): Pair<List<EvStop>, String?>? {
        val job = evPlanJobs.getOrPut(r) {
            if (evPlanJobs.size > 12) evPlanJobs.clear()
            scope.async { try { computeEvPlanRaw(r) } catch (e: Exception) { null } }
        }
        val res = job.await()
        if (res == null) evPlanJobs.remove(r)
        return res
    }

    private suspend fun computeEvPlanRaw(r: Route): Pair<List<EvStop>, String?>? {
        val batt = evBatteryKwh()
        val destKm = prefs.i(PreferenceKeys.EV_DEST_KM, 50).coerceIn(0, 500).toDouble()
        val chargerKm = prefs.i(PreferenceKeys.EV_CHARGER_KM, 30).coerceIn(0, 500).toDouble()
        val (startKm0, full0) = evRangeKm()
        val lenKm = r.len / 1000.0
        // the chargers along the route are looked up while the heights load (both are network calls)
        val candsJob = scope.async { withContext(Dispatchers.IO) { chargersAlong(r) } }
        // the heights along the route first (hills change the range a lot)
        evElevNow(r)
        if (route !== r && routes.none { it === r }) { candsJob.cancel(); return null }
        // the range counts as far as this route's speeds, hills, the weather and the safety margin allow (EvModel), not the rated km
        val rf = try {
            if (lenKm > 1.0 && r.time > 60) (evRouteKwh(r, 0.0, r.len / r.time * 3.6) / (lenKm * evBaseKwhPerKm())).coerceIn(0.5, 2.5) else 1.0
        } catch (e: Exception) { 1.0 }
        val startKm = startKm0 / rf; val full = full0 / rf
        if (startKm - lenKm >= destKm) { candsJob.cancel(); return emptyList<EvStop>() to null }
        // no internet: no charger can be looked up, so no plan; say so plainly (not "no charger in reach")
        if (!online()) { candsJob.cancel(); return emptyList<EvStop>() to EV_OFFLINE_MSG }
        val cands = candsJob.await()
        if (route !== r && routes.none { it === r }) return null
        val ids = ChargeOperators.selected(prefs)
        // the greedy plan: at each stop [pick] chooses among the chargers in reach; the stops and a warning if it falls short
        fun plan(pick: (List<Nearby>) -> Nearby?): Pair<List<EvStop>, String?> {
            val stops = ArrayList<EvStop>()
            var s = 0.0; var range = startKm; var warning: String? = null
            while (range - (lenKm - s) < destKm) {
                if (stops.size >= 6) { warning = Lang.t("Mehr als 6 Ladestopps: die Strecke ist für diese Planung zu lang.", "More than 6 charging stops: the trip is too long for this plan."); break }
                val reach = s + range - chargerKm
                val inReach = cands.filter { it.dist / 1000.0 > s + 0.5 && it.dist / 1000.0 <= reach }
                val c = pick(inReach)
                if (c == null) {
                    val minKw = prefs.i(PreferenceKeys.EV_MIN_KW, 50)
                    warning = if (minKw > 0) Lang.t("Kein Ladepunkt mit mind. $minKw kW auf der Strecke ist mit ${chargerKm.toInt()} km Reserve erreichbar.", "No charger of at least $minKw kW on the route can be reached with ${chargerKm.toInt()} km left.")
                    else Lang.t("Kein Ladepunkt auf der Strecke ist mit ${chargerKm.toInt()} km Reserve erreichbar.", "No charger on the route can be reached with ${chargerKm.toInt()} km left.")
                    break
                }
                val ck = c.dist / 1000.0
                val arr = range - (ck - s)
                // charge only as far as the destination needs (with its reserve), never past full
                val target = min(full, max(arr, (lenKm - ck) + destKm))
                // the car's charging curve, capped by what this charger delivers
                // a cold battery charges slower; one warmed by a long drive (an hour or more since the start or the last stop) less so
                val pf = EvModel.chargeTempFactor(evTemp(), warm = ck - s >= 100.0)
                val mins = EvCars.chargeMinutes(prefs, arr / full * 100.0, target / full * 100.0, batt, c.kw, pf).coerceAtLeast(0)
                stops += EvStop(c, Math.round(arr / full * 100.0).toInt().coerceIn(0, 100), Math.round(target / full * 100.0).toInt().coerceIn(0, 100), mins, ChargeOperators.matches(c, ids))
                s = ck; range = target
            }
            return stops to warning
        }
        // a charger of known power first; one of unknown power only when nothing else is in reach
        fun quickest(inReach: List<Nearby>) = inReach.filter { it.kw != null }.ifEmpty { inReach }.maxByOrNull { it.dist }
        // time the stops cost: charging plus the detour to each charger
        fun total(p: List<EvStop>) = p.sumOf { it.minutes + max(0, it.n.detourMin) }
        var (stops, warning) = plan { quickest(it) }
        if (ids.isNotEmpty() && stops.isNotEmpty() && stops.any { !it.preferred }) {
            // the furthest preferred charger in reach. One of unknown power only when no charger of known power is in reach
            // at all, so a preference never swaps a known fast charger for what may be a slow one; none: the quickest rule
            val pref = plan { inReach ->
                val known = inReach.filter { it.kw != null }
                (if (known.isEmpty()) inReach else known).filter { ChargeOperators.matches(it, ids) }.maxByOrNull { it.dist } ?: quickest(inReach)
            }
            // only when it needs no extra stop and costs at most the extra minutes allowed in Settings
            val allowed = prefs.i(PreferenceKeys.EV_PREF_MIN, 10).coerceIn(0, 120)
            if ((pref.second == null) == (warning == null) && pref.first.size == stops.size && pref.first.count { it.preferred } > stops.count { it.preferred } &&
                total(pref.first) - total(stops) <= allowed) { stops = pref.first; warning = pref.second }
        }
        return stops to warning
    }

    /** After a reroute, what to say once the charging is planned again: 0 nothing, 1 the result, 2 only if stops were added. */
    private var evReplanNotice = 0

    /**
     * Electric, after a reroute while driving. With charging stops on the trip: when the new way to them is clearly longer than the
     * way without them (more than 5 minutes on top of their own detours), they are planned again for the new way, your own stops
     * kept. With no charging stops yet: checks the (maybe longer) new way still reaches; if not, a stop is planned.
     */
    private fun evAfterReroute() {
        if (!isEv || previewing || !prefs.b(PreferenceKeys.SMARTFUEL, true)) return
        val d = dest ?: return
        val r = route ?: return
        val auto = evPlanned
        if (auto.isEmpty()) {
            // (not when you took the planned stops off yourself for this destination)
            if (evAutoDest?.let { Geo.dist(it, d.pos) < 50.0 } != true && evPlanningFor == null) { evReplanNotice = 2; planEv(r) }
            return
        }
        val f = fix ?: return
        if (!online()) return
        scope.launch {
            val own = via.filter { p -> auto.none { Geo.dist(it.n.place.pos, p.pos) < 30.0 } }
            val hd = if (f.speed > 2.0) f.heading else -1.0
            val direct = try {
                withContext(Dispatchers.IO) { netRoutes(f.pos, d.pos, false, 0L, own.map { it.pos }, hd, true) }.minByOrNull { it.time }
            } catch (e: Exception) { null } ?: return@launch
            // the trip may have moved on meanwhile (another reroute, stops changed, ended)
            if (route !== r || evPlanned !== auto || previewing) return@launch
            val extraMin = (r.time - direct.time) / 60.0 - auto.sumOf { max(0, it.n.detourMin) }
            if (extraMin <= 5.0) return@launch
            // take the automatic stops off (your own stay) and plan again for the new way, like a changed range does
            via = own
            evPlanned = emptyList(); evAutoDest = null; evPlanWarn = null
            evReplanNotice = 1
            toast(Lang.t("Neuer Weg: Ladestopps werden neu geplant …", "New way: planning the charging stops again …"), 4000)
            startRoute(d, departMin, true, startNow = true)
        }
    }

    /** How close counts as reached for the next stop: a charging site (a car park) more generously than a plain stop. */
    private fun viaReachM(): Double {
        val p = via.firstOrNull() ?: return 80.0
        return if (evPlanned.any { Geo.dist(it.n.place.pos, p.pos) < 30.0 }) 150.0 else 80.0
    }

    private fun planEv(r: Route) {
        evStops = emptyList(); evWarning = null; pushVia()
        // stops already on the trip are the user's plan: no suggestion on top of them (but a warning that came with the
        // charging stops put on it stays: that plan still falls short somewhere)
        // your own stops are fixed: the charging stops are planned round them (a stop of yours is not counted as a charge: no charge assumed there)
        evPlanningFor = r
        // while driving, the battery and the position have moved on: plan afresh (in the preview the cards' plan is reused)
        if (!previewing) evPlanJobs.remove(r)
        scope.launch { try {
            val (stops, warning) = computeEvPlan(r) ?: return@launch
            if (route === r) {
                evStops = stops; evWarning = warning; pushVia()
                // after a reroute that planned the charging again: say what came of it
                val notice = evReplanNotice
                if (notice != 0 && !previewing) {
                    evReplanNotice = 0
                    if (stops.isNotEmpty()) {
                        toast(Lang.t("Ladestopps neu geplant: ", "Charging stops planned again: ") + stops.joinToString(", ") { it.n.place.name }, 7000)
                        speak(if (stops.size == 1) Lang.t("Ladestopp neu geplant", "Charging stop planned again") else Lang.t("${stops.size} Ladestopps neu geplant", "${stops.size} charging stops planned again"))
                    } else if (notice == 1) toast(Lang.t("Ladestopps neu geplant: kein Ladestopp mehr nötig", "Charging planned again: no stop needed now"), 6000)
                }
                // put the stops on the trip straight away, once per destination: if you take them off again (stops editor) they
                // stay a suggestion (the "Add stops" pill) instead of coming back with every new route
                val d = dest
                // (not while the routes are on offer: then they stay a plan per route, and the chosen route's stops go on
                // the trip as it starts; this is for a plan that only finishes while you are already driving)
                if (stops.isNotEmpty() && d != null && !previewing && evAutoDest?.let { Geo.dist(it, d.pos) < 50.0 } != true) {
                    evAutoDest = d.pos
                    addStops(stops, auto = true)
                }
            }
        } finally {
            if (evPlanningFor === r) evPlanningFor = null
            // Start was tapped while this plan was still being worked out: start now (with the stops it found)
            if (evStartPending && previewing && route === r && evPlanningFor == null) beginNavigation()
        } }
    }

    /** What the charging plan of each route comes to, for the route cards: number of stops, minutes of charging, falls short or not. */
    class EvRouteSummary(val stops: Int, val minutes: Int, val warning: Boolean, val offline: Boolean = false)
    /** The warning of a plan that could not be made for lack of internet. */
    val EV_OFFLINE_MSG get() = Lang.t("Ohne Internet können keine Ladestopps geplant werden. Der Akku reicht nicht bis zum Ziel.", "No internet: charging stops can't be planned, and the battery won't reach the destination.")
    val evRoutePlans = androidx.compose.runtime.mutableStateMapOf<Route, EvRouteSummary>()

    /** Plans the charging for every route on offer, one after the other, so each route card can show its own stops. */
    /** Routes whose plan in [evRoutePlans] is only a quick estimate so far (the real plan replaces it). */
    val evEstimated = androidx.compose.runtime.mutableStateMapOf<Route, Boolean>()

    /**
     * A rough plan that takes no network at all: charger stops as far apart as the range allows, each with the charging time of the car's
     * curve at a 150 kW charger and a few minutes' detour. Shown at once on the route cards until the real plan is in.
     */
    /**
     * The charging plan to show for a route on offer: the real one when it is in, else the quick estimate. Never nothing for an
     * electric car with charge planning on, so a route card is never drawn, even for one frame, without its charging time.
     */
    fun evPlanShown(r: Route): EvRouteSummary? =
        evRoutePlans[r] ?: if (isEv && prefs.b(PreferenceKeys.SMARTFUEL, true) && via.isEmpty()) evQuickPlan(r) else null

    /** True while what [evPlanShown] gives for [r] is only the estimate. */
    fun evPlanIsEstimate(r: Route): Boolean = evEstimated[r] == true || (evRoutePlans[r] == null && evPlanShown(r) != null)

    private fun evQuickPlan(r: Route): EvRouteSummary {
        return try {
            val destKm = prefs.i(PreferenceKeys.EV_DEST_KM, 50).coerceIn(0, 500).toDouble()
            val chargerKm = prefs.i(PreferenceKeys.EV_CHARGER_KM, 30).coerceIn(0, 500).toDouble()
            val (startKm, full) = evRangeKm()
            val lenKm = r.len / 1000.0
            val batt = evBatteryKwh()
            var s = 0.0; var range = startKm; var n = 0; var mins = 0
            while (range - (lenKm - s) < destKm && n < 6 && full > chargerKm + 20) {
                val ck = s + range - chargerKm
                if (ck <= s + 1.0) break
                val arr = chargerKm
                val target = minOf(full, maxOf(arr, (lenKm - ck) + destKm))
                mins += EvCars.chargeMinutes(prefs, arr / full * 100.0, target / full * 100.0, batt, 150, 1.0).coerceAtLeast(0) + 3
                s = ck; range = target; n++
            }
            EvRouteSummary(n, mins, range - (lenKm - s) < destKm)
        } catch (e: Exception) { EvRouteSummary(0, 0, false) }
    }

    /** True while the routes on offer are still being planned: their times are not final until every plan is in. */
    var evPlansPending by mutableStateOf(false)

    private fun planEvAllRoutes() {
        val rs = routes
        evRoutePlans.clear(); evEstimated.clear()
        evPlansPending = true
        for (r in rs) { evRoutePlans[r] = evQuickPlan(r); evEstimated[r] = true }
        scope.launch {
            try {
                // all routes at once: the slowest one decides, not the sum
                rs.map { r -> scope.async {
                    val res = try { computeEvPlan(r) } catch (e: Exception) { android.util.Log.w("NavEngine", "charging plan failed", e); null }
                    if (routes === rs) { evEstimated.remove(r); evRoutePlans[r] = if (res == null) EvRouteSummary(0, 0, true) else EvRouteSummary(res.first.size, res.first.sumOf { it.minutes }, res.second != null, res.second == EV_OFFLINE_MSG) }
                } }.forEach { it.await() }
                // the routes are offered only now: pre-select the quickest one counting its charging time
                if (routes === rs && previewing) {
                    val best = rs.indices.minByOrNull { rs[it].time + (evRoutePlans[rs[it]]?.minutes ?: 0) * 60.0 }
                    if (best != null && best != active) selectRoute(best, byUser = false)
                }
            } finally {
                if (routes === rs) evPlansPending = false
            }
        }
    }

    private var chgLastBox: DoubleArray? = null
    /** The charge points last found for the view (before the filter). */
    private var chgAll: List<ChargerSite> = emptyList()
    /** The map's charger filter in kW (0 = all); stays after a restart. */
    var chargerMinKw by mutableStateOf(prefs.i(PreferenceKeys.CHARGER_FILTER_KW, 0))
    var chargerFilterOpen by mutableStateOf(false)

    /** Only the charge points in the chosen power class; one whose power is unknown is hidden while a filter is on. */
    /** The map's markers: each place's charge points added up, but only those at the chosen power or faster (a 200 kW point isn't counted at 300 kW). */
    private fun shownChargers(units: List<ChargerSite>): List<ChargerSite> =
        mergeSites(if (chargerMinKw <= 0) units else units.filter { (it.kw ?: 0) >= chargerMinKw })

    fun pickChargerMinKw(kw: Int) {
        chargerMinKw = kw
        prefs.put(PreferenceKeys.CHARGER_FILTER_KW, kw)
        map.setChargers(shownChargers(chgAll))
        // on a trip with electric planning: the charging stops are planned again with the chargers the filter leaves
        if (route != null && !previewing && isEv && prefs.b(PreferenceKeys.SMARTFUEL, true)) refreshEvPlan()
    }

    private var chgNavAt = 0L

    /**
     * While driving, the map is never still, so the charge points are asked for as the car goes: the view and the next 30 km of the
     * route (as one box), every 15 s. The layer's own check skips the call while the box hasn't moved far.
     */
    private fun maybeRefreshChargers() {
        val r = route ?: return
        if (previewing || prefs.s(PreferenceKeys.FUEL_TYPE, "E10") != "EV") return
        val now = System.currentTimeMillis()
        if (now - chgNavAt < 15_000L) return
        chgNavAt = now
        val i0 = lastIdx.coerceIn(0, max(0, r.pts.size - 1))
        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
        var acc = 0.0; var prev: LL? = null
        for (i in i0 until r.pts.size) {
            val p = r.pts[i]
            if (prev != null) acc += Geo.dist(prev, p)
            prev = p
            minLat = min(minLat, p.lat); maxLat = max(maxLat, p.lat); minLon = min(minLon, p.lon); maxLon = max(maxLon, p.lon)
            if (acc > 30_000.0) break
        }
        if (minLat > maxLat) return
        val view = try { map.viewBox() } catch (e: Exception) { null }
        val b = if (view == null) doubleArrayOf(minLat, minLon, maxLat, maxLon, 12.0)
            else doubleArrayOf(min(view[0], minLat), min(view[1], minLon), max(view[2], maxLat), max(view[3], maxLon), view[4])
        refreshChargerLayer(b)
    }

    /**
     * The charge points in the map view, with their power and free count. Asked again only when the view has moved on; the German
     * register is asked online, the Dutch and Brussels copies come from the phone. Not shown from zoom 9 out.
     */
    private fun refreshChargerLayer(box: DoubleArray) {
        // charge points are only for electric cars: with a fuel car they stay off the map
        if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") != "EV") { chgLastBox = null; map.setChargers(emptyList()); return }
        val last = chgLastBox
        if (last != null && abs(last[4] - box[4]) < 0.01 && abs(last[0] - box[0]) < 0.003 && abs(last[1] - box[1]) < 0.003 &&
            abs(last[2] - box[2]) < 0.003 && abs(last[3] - box[3]) < 0.003) return
        chgLastBox = box
        if (box[4] < 9.0) { map.setChargers(emptyList()); return }
        scope.launch {
            // the foreign charger files (France, Netherlands, Brussels) are downloaded only when switched on in Settings, with one switch
            val dl = prefs.b(PreferenceKeys.FR_CHARGERS, false)
            val fr = dl && FrChargers.inFrance(box)
            val sites = withContext(Dispatchers.IO) {
                val out = ArrayList<ChargerSite>()
                if (fr) try { FrChargers.refresh(ctx) } catch (e: Exception) { }
                // a region's copy is fetched (once a week) when the view is there
                if (dl && box[0] < 53.6 && box[2] > 50.7 && box[1] < 7.3 && box[3] > 3.3) try { NdwChargers.refresh(ctx) } catch (e: Exception) { }
                if (dl && box[0] < 50.91 && box[2] > 50.76 && box[1] < 4.47 && box[3] > 4.23) try { BruChargers.refresh(ctx) } catch (e: Exception) { }
                try { out += NdwChargers.inBox(ctx, box) } catch (e: Exception) { }
                try { out += BruChargers.inBox(ctx, box) } catch (e: Exception) { }
                if (fr) try { out += FrChargers.inBox(ctx, box) } catch (e: Exception) { }
                if (online()) try { out += ChargerInfo.inBox(box, prefs.s(PreferenceKeys.OCM_KEY, "")) } catch (e: Exception) { }
                // free points in Baden-Württemberg from the open live feed (no key); the rest keep their register counts
                val live0 = if (BwLive.inBw(box)) BwLive.apply(out, BwLive.statuses()) else out
                // free points in France from the national live file, matched by the stored copy's ids
                val live = if (fr) try { FrLive.apply(live0, FrLive.statuses()) } catch (e: Exception) { live0 } else live0
                live.take(300)
            }
            chgAll = sites
            map.setChargers(shownChargers(chgAll))
        }
    }

    /** A charge point on the map was tapped: the route there, or a stop on the way when a route is already running. */
    private fun chargerTapped(s: ChargerSite) {
        val name = s.op?.takeIf { it.isNotBlank() } ?: Lang.t("Ladesäule", "Charging point")
        val p = Place(name, s.kwLabel ?: s.kw?.let { "$it kW" } ?: "", s.pos)
        if (route != null && !previewing && dest != null) addStop(p) else startRoute(p)
    }

    /** Draws the stops on the way on the map; a stop is a charger when it's one of the planned charging stops. */
    private fun pushVia() {
        // in the route preview the planned charging stops are not on the trip yet, but they are drawn so you see where you would charge
        if (previewing && via.isEmpty() && evStops.isNotEmpty()) {
            map.setVia(evStops.map { it.n.place.pos to true }, evStops.map { st -> st.n.kw?.let { "$it kW" } ?: "?" })
            return
        }
        // a charging stop shows its power on the map; "?" when the power isn't known
        val labels = via.map { p -> evPlanned.firstOrNull { Geo.dist(it.n.place.pos, p.pos) < 30.0 }?.let { st -> st.n.kw?.let { "$it kW" } ?: "?" } }
        map.setVia(via.map { p -> p.pos to evPlanned.any { Geo.dist(it.n.place.pos, p.pos) < 30.0 } }, labels)
    }

    /** Chargers along the whole route, with where they are on it (metres) and the detour. */
    private fun chargersAlong(r: Route): List<Nearby> {
        val out = ArrayList<Nearby>()
        val minKw = prefs.i(PreferenceKeys.EV_MIN_KW, 50)
        // the power filter from Settings drops chargers known to be slower. Ones of unknown power stay, as a last resort
        // (the planner takes them only when no charger of known power is in reach)
        fun fast(n: Nearby) = minKw <= 0 || n.kw == null || n.kw >= minKw
        try {
            // TomTom's list and the registers are asked at the same time (each is a network call of a second or more)
            val (tt, reg) = kotlinx.coroutines.runBlocking {
                val ttJob = async(Dispatchers.IO) {
                    val l = ArrayList<Nearby>()
                    if (hasKey && online()) {
                        val line = RouteExtras.line(r, 0.0, r.len, 800.0, 140)
                        try {
                            for ((n, secs) in TomTomRest.poiAlong(apiKey, "charging", line, 600)) {
                                val pr = RouteExtras.project(r, n.place.pos, 3000.0) ?: continue
                                l += n.copy(dist = pr.first, detourMin = if (secs >= 0) Math.round(secs / 60.0).toInt() else detour(pr.second), ahead = true)
                            }
                        } catch (e: Exception) { }
                    }
                    l
                }
                val regJob = async(Dispatchers.IO) {
                    // the German register / Open Charge Map: the power and operator of chargers TomTom lists without them (filled in before
                    // the power filter, so one of unknown power can turn out fast or slow), and the fast chargers TomTom left out
                    val regLine = RouteExtras.line(r, 0.0, r.len, 1000.0, 150)
                    val reg0: List<Nearby> = if (online()) ChargerInfo.along(prefs, regLine) else emptyList()
                    // the Netherlands: NDW's national register too, each site counted once with the others
                    val regNl: List<Nearby> = if (online() && regLine.any { it.lat in 50.7..53.6 && it.lon in 3.3..7.3 })
                        reg0 + NdwChargers.nearby(ctx, regLine).filter { n -> reg0.none { Geo.dist(it.place.pos, n.place.pos) < 150.0 } }
                        else reg0
                    // Brussels: its open charge points (DC only, no power given)
                    if (online() && regLine.any { it.lat in 50.76..50.91 && it.lon in 4.23..4.47 })
                        regNl + BruChargers.nearby(ctx, regLine).filter { n -> regNl.none { Geo.dist(it.place.pos, n.place.pos) < 150.0 } }
                        else regNl
                }
                ttJob.await() to regJob.await()
            }
            val known = ChargerInfo.enrich(tt, reg)
            out += known.filter { fast(it) }
            val added = ArrayList<Nearby>()
            for (n in reg) {
                if (!fast(n) || ChargerInfo.near(n.place.pos, known)) continue
                val pr = RouteExtras.project(r, n.place.pos, 1600.0) ?: continue
                added += n.copy(dist = pr.first, detourMin = detour(pr.second), ahead = true)
            }
            out += added
            // TomTom gives at most 20 along the route: add OpenStreetMap's when few of known power are left
            if (out.count { it.kw != null } < 10 && online()) {
                val line = RouteExtras.line(r, 0.0, r.len, 1500.0, 45)
                for (n in ChargerInfo.enrich(Overpass.nearbyAlong("charging", line, 900), reg)) {
                    // (one the register already added would be listed twice)
                    if (!fast(n) || ChargerInfo.near(n.place.pos, added)) continue
                    val pr = RouteExtras.project(r, n.place.pos, 1200.0) ?: continue
                    out += n.copy(dist = pr.first, detourMin = detour(pr.second), ahead = true)
                }
            }
        } catch (e: Exception) { }
        return out.distinctBy { it.place.pos.lat to it.place.pos.lon }
    }

    private fun suggestFuel(r: Route) {
        val fuel = prefs.s(PreferenceKeys.FUEL_TYPE, "E10")
        // a fuel stop is suggested only for an electric car (its charging stops); a combustion car gets no stop suggestion on the route
        if (fuel != "EV") return
        var a = r.len * 0.35; var b = r.len * 0.75
        // electric: a charger before the battery gets down to the charge you want to keep (Settings > Fuel), with the shortest
        // detour, and how much you'll have when you get there. Only when the trip needs one.
        if (fuel == "EV") { planEv(r); return }
        scope.launch {
            val best = withContext(Dispatchers.IO) {
                try {
                    val out = ArrayList<Nearby>()
                    if (FuelFinder.configured(prefs)) {
                        try { FuelFinder.refresh(ctx, prefs) } catch (e: Exception) { }
                        var minLat = 90.0; var maxLat = -90.0; var minLon = 180.0; var maxLon = -180.0
                        for (i in r.pts.indices) if (r.cum[i] in a..b) { val q = r.pts[i]; minLat = min(minLat, q.lat); maxLat = max(maxLat, q.lat); minLon = min(minLon, q.lon); maxLon = max(maxLon, q.lon) }
                        for (s0 in FuelFinder.stations(ctx)) {
                            if (s0.pos.lat < minLat - 0.015 || s0.pos.lat > maxLat + 0.015 || s0.pos.lon < minLon - 0.025 || s0.pos.lon > maxLon + 0.025) continue
                            val pr = RouteExtras.project(r, s0.pos, 1200.0) ?: continue
                            if (pr.first !in a..b) continue
                            val price = FuelFinder.priceOf(ctx, s0.id, fuel) ?: continue
                            out += Nearby(Place(s0.name, s0.brand, s0.pos), pr.first, if (s0.motorway) Lang.t("Raststätte", "Motorway services") else "", price.pence, detour(pr.second), true)
                        }
                    }
                    // outside the UK: the country's own price service, at a few points along the middle of the trip
                    val cc = detectedCountry.uppercase()
                    if (out.isEmpty() && cc != "GB" && EuFuel.supported(cc, prefs) && EuFuel.hasFuel(cc, fuel) && online()) {
                        for (s0 in EuFuel.along(cc, prefs, RouteExtras.line(r, a, b, 1500.0, 45), fuel, maxAsks = 5)) {
                            val pr = RouteExtras.project(r, s0.pos, 1200.0) ?: continue
                            if (pr.first !in a..b) continue
                            out += Nearby(Place(s0.name, s0.sub, s0.pos), pr.first, "", s0.cents, detour(pr.second), true, unit = "€")
                        }
                    }
                    if (out.isEmpty() && hasKey && online() && fuel != "E85") {
                        val line = RouteExtras.line(r, a, b, 800.0, 140)
                        for ((n, secs) in TomTomRest.poiAlong(apiKey, "fuel", line, 600)) {
                            val pr = RouteExtras.project(r, n.place.pos, 3000.0) ?: continue
                            out += n.copy(dist = pr.first, detourMin = if (secs >= 0) Math.round(secs / 60.0).toInt() else detour(pr.second), ahead = true)
                        }
                    }
                    // price matters most, but a long detour costs time (each minute counts as a penny a litre)
                    if (out.any { it.price != null }) out.filter { it.detourMin in 0..8 }.minByOrNull { (it.price ?: 999.0) + it.detourMin }
                    else out.filter { it.detourMin >= 0 }.minByOrNull { it.detourMin }
                } catch (e: Exception) { null }
            }
            if (route === r) fuelSuggestion = best
        }
    }

    // ---- a petrol or diesel car's range ----
    /** The km the tank still goes (by your speed and the limits ahead), or null when the fill level is unknown. */
    var fuelRangeKm by mutableStateOf<Int?>(null)
    private var fuelWarned = false
    private var fuelCheckAt = 0L

    /**
     * A petrol or diesel car: how far the tank goes, from the car's fill level (Android Auto) and the tank size and use set in
     * Settings, counted along the route with the speeds you'll drive (EvModel.litres). Under 80 km: a warning (once, until you've
     * filled up) with the nearest fuel station ahead.
     */
    private fun checkFuelRange() {
        if (isEv) { fuelRangeKm = null; return }
        val now = System.currentTimeMillis()
        if (now - fuelCheckAt < 20_000L) return
        fuelCheckAt = now
        val at = prefs.s(PreferenceKeys.FUEL_NOW_AT, "").toLongOrNull() ?: 0L
        if (now - at > 15 * 60_000L) { fuelRangeKm = null; return }
        val l100 = prefs.s(PreferenceKeys.FUELMPG, "").replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..30.0 } ?: 7.0
        val pct = prefs.s(PreferenceKeys.FUEL_NOW_PCT, "").toIntOrNull()
        val carKm = prefs.s(PreferenceKeys.FUEL_NOW_KM, "").toIntOrNull()
        val tank = prefs.s(PreferenceKeys.FUEL_TANK_L, "").toDoubleOrNull()?.takeIf { it in 10.0..200.0 }
        // the litres left: the fill level × the tank, else the car's own range at its rated use
        val litres = when {
            pct != null && tank != null -> pct / 100.0 * tank
            carKm != null -> carKm * l100 / 100.0
            pct != null -> pct / 100.0 * 50.0
            else -> { fuelRangeKm = null; return }
        }
        val r = route
        val km: Double = if (r != null && !previewing) {
            // along the route: the point where the litres run out (searched in steps), past the end at the rated use
            val from = curProgress
            val avg = eta?.let { e -> if (e.secs - e.chargeSecs > 30) e.dist / (e.secs - e.chargeSecs) * 3.6 else null } ?: 70.0
            fun use(to: Double) = EvModel.litres(from, to, r.limits, avg.coerceIn(20.0, 140.0), evStyle(), evFreeKmh(), l100)
            val all = use(r.len)
            if (all <= litres) (r.len - from) / 1000.0 + (litres - all) / (l100 * EvModel.FUEL_BUFFER) * 100.0
            else {
                var lo = from; var hi = r.len
                repeat(18) { val m = (lo + hi) / 2; if (use(m) <= litres) lo = m else hi = m }
                (lo - from) / 1000.0
            }
        } else litres / (l100 * 1.1 * EvModel.FUEL_BUFFER) * 100.0
        fuelRangeKm = Math.round(km).toInt()
        if (km >= 95.0) { fuelWarned = false; return }
        if (km >= 80.0 || fuelWarned) return
        fuelWarned = true
        val n = Math.round(km).toInt()
        speak(Lang.t("Tankreichweite nur noch etwa $n Kilometer", "Fuel range only about $n kilometres"), alert = true)
        toast(Lang.t("Tank: noch etwa $n km. Suche Tankstelle…", "Fuel: about $n km left. Looking for a fuel station…"), 6000)
        val p0 = fix?.pos ?: return
        scope.launch {
            val st = withContext(Dispatchers.IO) {
                try {
                    if (r != null) {
                        val prog = curProgress
                        val line = RouteExtras.line(r, prog, min(r.len, prog + km * 1000.0 * 0.9), 1500.0, 45)
                        val along = if (hasKey && online()) TomTomRest.poiAlong(apiKey, "fuel", line, 600).mapNotNull { (n, secs) ->
                            val pr = RouteExtras.project(r, n.place.pos, 3000.0, prog) ?: return@mapNotNull null
                            n.copy(dist = pr.first, detourMin = if (secs >= 0) Math.round(secs / 60.0).toInt() else detour(pr.second), ahead = true)
                        } else emptyList()
                        (along.ifEmpty { Overpass.nearbyAlong("fuel", line, 1500).mapNotNull { n ->
                            val pr = RouteExtras.project(r, n.place.pos, 2000.0, prog) ?: return@mapNotNull null
                            n.copy(dist = pr.first, detourMin = detour(pr.second), ahead = true)
                        } }).filter { it.dist > prog }.minByOrNull { it.dist - prog + it.detourMin * 1000.0 }
                    } else {
                        PoiSearch.kindOf("tankstelle")?.let { k -> PoiSearch.osmByKind(k, p0, 15_000).firstOrNull() }
                            ?.let { pl -> Nearby(pl, Geo.dist(p0, pl.pos), "", null, 0, true) }
                    }
                } catch (e: Exception) { null }
            } ?: return@launch
            fuelSuggestion = st
            toast(Lang.t("Nächste Tankstelle: ${st.place.name}", "Nearest fuel station: ${st.place.name}"), 6000)
        }
    }

    // ---- wrong way ----
    private fun checkWrongWay(p: Fix) {
        if (simMode != 0 || !prefs.b(PreferenceKeys.WRONGWAY, false) || p.speed < 4.0 || (p.acc > 0 && p.acc > 20.0)) { wrongCount = 0; return }
        // the check runs through every nearby road, so it's done in the background (it used to stall the screen once a second)
        if (wrongBusy) return
        wrongBusy = true
        scope.launch {
            val wrong = withContext(Dispatchers.Default) { try { Overpass.wrongWay(p.pos, p.heading) } catch (e: Exception) { false } }
            wrongBusy = false
            if (wrong) wrongCount++ else wrongCount = 0
            wrongResult()
        }
    }
    private var wrongBusy = false

    private fun wrongResult() {
        val now = System.currentTimeMillis()
        if (wrongCount >= 3 && now - wrongAt > 60_000L) {
            wrongAt = now
            chime(android.media.ToneGenerator.TONE_PROP_BEEP2)
            raiseAlert("wrongway", Lang.t("Falsche Richtung! Das ist eine Einbahnstraße", "Wrong way! This is a one-way road"), 7000)
            speak(Lang.t("Achtung. Sie fahren möglicherweise falsch herum in eine Einbahnstraße.", "Warning. You may be going the wrong way on a one-way road."), true)
        }
    }

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
    private fun pickBestVoice() {
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
    fun installedVoices(): List<android.speech.tts.Voice> {
        val t = tts ?: return emptyList()
        return try {
            val lang = t.language?.language ?: return emptyList()
            (t.voices ?: emptySet()).filter { it.locale.language == lang && it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true }
                .sortedByDescending { it.quality }
        } catch (e: Exception) { emptyList() }
    }

    /** Uses the voice [name] from now on (kept across restarts). */
    fun useVoice(name: String) {
        prefs.put(PreferenceKeys.TTS_VOICE, name)
        try { tts?.voices?.firstOrNull { it.name == name }?.let { tts?.voice = it } } catch (e: Exception) { }
    }

    fun handleVoice(raw: String): String? {
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
    private fun slice(r: Route, a: Double, b: Double): List<LL> {
        val out = ArrayList<LL>()
        out += pointAt(r, a).first
        for (i in r.pts.indices) { val c = r.cum[i]; if (c > a && c < b) out += r.pts[i] }
        out += pointAt(r, b).first
        return out
    }

    /** Draws the average speed zones on the route on the map: the road between the cameras, and the cameras at each end. */
    private fun pushAvgZones(r: Route, zones: List<AvgOnRoute>) {
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
    private fun routeExtrasTick(r: Route, progress: Double) {
        try { checkVignette(r, progress) } catch (e: Exception) { }
        if (previewing) return
        // your destination's building lights up for the last 400 m (and goes back to normal when you get there)
        run {
            val dp = dest?.pos
            val left = r.len - progress
            map.highlightDestination(if (dp != null && left in 15.0..400.0) dp else null)
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
        val wantHz = prefs.b(PreferenceKeys.BUMPS, false) || prefs.b(PreferenceKeys.SCHOOLS, false) || prefs.b(PreferenceKeys.AVGZONES, true) || wantJct
        // (a new route after a reroute is looked up again, but not more than once every 45 seconds)
        if (wantHz && !hzBusy && (progress + 6000.0 > hzWinEnd || (hzWinRoute !== r && now - hzLastAt > 45_000L)) && now > hzRetryAt && online()) {
            hzBusy = true; hzLastAt = now
            val to = progress + 20_000.0
            val line = RouteExtras.line(r, progress - 300.0, to, 250.0, 90)
            scope.launch {
                try {
                    val jl = ArrayList<Jct>()
                    val (p, a) = withContext(Dispatchers.IO) { RouteExtras.hazards(line, if (wantJct) jl else null) }
                    rawHz = (rawHz + p).distinctBy { "${it.kind}${Math.round(it.pos.lat * 1e5)},${Math.round(it.pos.lon * 1e5)}" }.takeLast(1500)
                    if (jl.isNotEmpty()) { rawJcts = (rawJcts + jl).distinctBy { "${Math.round(it.pos.lat * 1e5)},${Math.round(it.pos.lon * 1e5)}" }.takeLast(600); jctsVersion++ }
                    rawAvg = (rawAvg + a).distinctBy { it.id }.takeLast(60)
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
                val lim = cur.def.limit ?: speedLimit
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
                avgInfo = next?.let { AvgInfo(0.0, it.def.limit ?: speedLimit, it.end - it.start, false, true, it.start - progress) }
                if (next != null && next.start - progress < 500.0 && avgAlerted.add(next.def.id)) {
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

    fun speakForce(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "read")
    }

    /** Turn-by-turn guidance is spoken only in "all" mode; alerts are also spoken in "alerts only". */
    /** True for the engine running the Android Auto screen. */
    @Volatile var isCar = false

    /** The phone keeps quiet while the car screen is connected: the car says the directions and alerts. */
    private fun quiet() = !isCar && RouteSync.carConnected()

    /** The phone's app is showing its own map (set by the screen; with Android Auto connected it normally shows the trip in brief). */
    @Volatile var showingMap = true

    /**
     * The phone while the car screen runs the trip and the phone isn't showing its map: it keeps following the route (so it can
     * carry on by itself when the phone is unplugged) but leaves the lookups to the car: speed limits, cameras, traffic and faster
     * routes aren't fetched twice, which halves the requests on your TomTom key and saves the phone's battery.
     */
    private fun shadow() = quiet() && !showingMap

    /** Joins a trip started on the other screen (the phone or the car): the same destination and stops, straight into guidance. */
    fun followRoute(to: Place, stops: List<Place>, ev: List<EvStop> = emptyList(), chosen: Route? = null) {
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

    // ---- the announcement queue ----
    // Announcements used to go straight into the speech engine's own queue, each waiting for the one before: after a weather note,
    // a camera and a jam, "In 500 m take the exit" came so late that the exit was already there. Now the app keeps its own queue:
    // a turn call goes first (it cuts short a plain note, never a warning), replaces any older turn call still waiting, and
    // anything that has waited too long to still be right is dropped.

    /** [kind]: 0 a note, 1 a warning, 2 a turn call. */
    private class Utt(val text: String, val kind: Int, val at: Long)
    private val speechQ = ArrayDeque<Utt>()
    private var playing: Utt? = null
    private var playingId = ""
    private var uttSeq = 0
    private val speechMain = android.os.Handler(android.os.Looper.getMainLooper())

    /** [turn]: a turn-by-turn call ("In 500 m, take the exit"), which has to be on time. */
    fun speak(text: String, alert: Boolean = false, turn: Boolean = false) {
        if (quiet()) return
        val m = soundMode
        if (m == 2 || (m == 1 && !alert) || !ttsReady) return
        val u = Utt(text, if (turn) 2 else if (alert) 1 else 0, android.os.SystemClock.elapsedRealtime())
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) enqueue(u) else speechMain.post { enqueue(u) }
    }

    // ---- on foot: back to the parked car ----
    /** What the walk card shows: the next instruction, how far to it, and what is left to the car. */
    class WalkInfo(val text: String, val stepDist: Double, val maneuver: String, val leftM: Double, val leftMin: Int)

    /** Where the walk goes (your car), null when not walking. A walk is kept apart from a drive: no route, no cameras, no battery. */
    var walkTo by mutableStateOf<LL?>(null); private set
    var walkInfo by mutableStateOf<WalkInfo?>(null); private set
    /** The walk's spoken directions: on at the start, the sound button on the map turns them off and on again. */
    var walkMuted by mutableStateOf(false); private set
    val walking: Boolean get() = walkTo != null
    private var walkRoute: Route? = null
    private var walkIdx = 0
    private var walkOffCount = 0
    private var walkBusy = false
    private var walkAskedAt = 0L
    private val walkSaid = HashSet<Int>()
    private var walkStartSaid = false

    /** Where you parked (saved when the car's connection ended, or by "hier geparkt"), for two days; null when none. */
    fun parkedSpot(): LL? {
        val ps = prefs.s(PreferenceKeys.PARKED_SPOT, "").split(',')
        val la = ps.getOrNull(0)?.toDoubleOrNull() ?: return null
        val lo = ps.getOrNull(1)?.toDoubleOrNull() ?: return null
        val at = ps.getOrNull(2)?.toLongOrNull() ?: 0L
        if (at > 0 && System.currentTimeMillis() - at > 48 * 3600_000L) return null
        return LL(la, lo)
    }

    /** Walks you back to your parked car, in the app (a route on foot, not on the roads). False when no spot is saved. */
    fun startWalkToCar(): Boolean {
        val spot = parkedSpot() ?: run { toast(Lang.t("Es ist kein Parkplatz gespeichert", "No parking spot is saved"), 3000); return false }
        startWalk(spot)
        return true
    }

    /** Starts a walk to [to]. A drive on the screen (or a route choice) ends first: you are on foot now. */
    fun startWalk(to: LL) {
        if (route != null) endRoute()
        walkTo = to; walkInfo = null; walkMuted = false; walkRoute = null; walkIdx = 0; walkOffCount = 0; walkSaid.clear(); walkStartSaid = false
        // no speed limit or camera on foot
        speedLimit = null; limitEstimated = false; noLimit = false; nextLimitMs = null
        following = true
        computeWalk()
    }

    fun endWalk() {
        walkTo = null; walkInfo = null; walkRoute = null
        map.setWalk(emptyList())
    }

    /** The sound button while walking: the walk's directions off or on again (the sound for driving stays as it is). */
    private fun toggleWalkSound() {
        walkMuted = !walkMuted
        toast(if (walkMuted) Lang.t("Fußweg-Ansagen aus", "Walking directions muted") else Lang.t("Fußweg-Ansagen an", "Walking directions on"), 1800)
    }

    /** Spoken on the walk, unless muted with the sound button (the drive's sound setting doesn't apply on foot). */
    private fun walkSpeak(text: String) {
        if (walkMuted || !ttsReady) return
        val u = Utt(text, 2, android.os.SystemClock.elapsedRealtime())
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) enqueue(u) else speechMain.post { enqueue(u) }
    }

    /** The way on foot: Mapbox's walking route online, the offline pack's footpaths without signal, else a straight line. */
    private fun computeWalk() {
        val to = walkTo ?: return
        val from = fix?.pos ?: return   // the first position starts it (see updateWalk)
        if (walkBusy) return
        walkBusy = true; walkAskedAt = System.currentTimeMillis()
        scope.launch {
            val r = withContext(Dispatchers.IO) {
                (if (hasMapbox && online()) try { MapboxRest.walkRoutes(mbToken, from, to).firstOrNull() } catch (e: Exception) { null } else null)
                    ?: (if (OfflineRouter.installed(ctx)) try { OfflineRouter.routes(ctx, from, to, emptyList(), -1.0, "pedestrian").firstOrNull() } catch (e: Exception) { null } else null)
                    ?: straightWalk(from, to)
            }
            walkBusy = false
            if (walkTo != to) return@launch
            walkRoute = r; walkIdx = 0; walkOffCount = 0; walkSaid.clear()
            map.setWalk(r.pts)
            fix?.let { updateWalk(it) }
        }
    }

    /** No route on foot to be had: the straight line to the car, with its direction and distance. */
    private fun straightWalk(from: LL, to: LL): Route {
        val d = Geo.dist(from, to)
        return Route(listOf(from, to), doubleArrayOf(0.0, d), listOf(Step(d, "ARRIVE", "", "")), d, d / 1.3, 0.0)
    }

    /** Each new position on the walk: the next instruction, the spoken call before it, a new way when you leave it, and arriving. */
    private fun updateWalk(p: Fix) {
        val to = walkTo ?: return
        if (walkRoute == null) { computeWalk(); return }
        val r = walkRoute ?: return
        if (Geo.dist(p.pos, to) < 20.0) {
            val msg = Lang.t("Du bist bei deinem Auto", "You're at your car")
            toast(msg, 4000)
            walkSpeak(msg)
            endWalk()
            return
        }
        if (r.pts.size < 2) return
        val s = RouteMatcher.snap(r, p.pos, walkIdx)
        walkIdx = s.segmentIndex
        // gone another way (more than 40 m off, with a good fix, several times): the way from here is asked again
        if (s.dist > 40.0 && p.acc in 0.1..30.0) {
            if (++walkOffCount >= 4 && System.currentTimeMillis() - walkAskedAt > 15_000L) { walkOffCount = 0; walkRoute = null; computeWalk(); return }
        } else walkOffCount = 0
        val prog = s.progress
        val total = r.cum.last()
        val left = max(0.0, total - prog)
        var k = r.steps.indexOfFirst { it.off > prog + 3.0 }
        if (k < 0) k = r.steps.size - 1
        val st = r.steps.getOrNull(k)
        val arriving = st == null || st.maneuver.uppercase().startsWith("ARRIVE")
        val stepD = if (st != null && !arriving) max(0.0, st.off - prog) else left
        val word = if (arriving) "" else Geo.maneuverText(st!!.maneuver, st.roundExit)
        val street = if (arriving || st!!.street.isBlank()) "" else Lang.t(" auf ${st.street}", " onto ${st.street}")
        val text = if (arriving) Lang.t("Zu deinem Auto", "To your car") else word.replaceFirstChar { it.uppercase() } + street
        val mins = maxOf(1, Math.ceil(left / 80.0).toInt())
        walkInfo = WalkInfo(text, stepD, st?.maneuver ?: "ARRIVE", left, mins)
        if (!walkStartSaid) {
            walkStartSaid = true
            walkSpeak(Lang.t("Fußweg zu deinem Auto, ${walkMetres(left)}, etwa $mins Minuten", "Walking to your car, ${walkMetres(left)}, about $mins minutes"))
        }
        // one call per turn, about 25 m before it ("In 20 Metern links abbiegen")
        if (!arriving && stepD <= 25.0 && k !in walkSaid) {
            walkSaid += k
            walkSpeak(Lang.t("In ${walkMetres(stepD)} $word$street", "In ${walkMetres(stepD)}, $word$street"))
        }
    }

    /** A walking distance to say: to 10 m, "Metern" in German after "in". */
    private fun walkMetres(m: Double): String {
        if (imperial) return Geo.spoken(m, true)
        val r = maxOf(10, (Math.round(m / 10.0) * 10).toInt())
        return if (r < 1000) Lang.t("$r Metern", "$r metres") else String.format(Locale.GERMANY, "%.1f Kilometern", r / 1000.0).let { Lang.t(it, String.format(Locale.US, "%.1f kilometres", r / 1000.0)) }
    }

    private fun enqueue(u: Utt) {
        if (u.kind == 2) {
            // older turn calls and plain notes still waiting are out of date now
            speechQ.removeAll { it.kind != 1 }
            speechQ.addFirst(u)
            val p = playing
            // a plain note being read out is cut short; a warning or a turn call is finished first (cutting a turn call off mid
            // sentence left half a direction)
            if (p != null && p.kind == 0) { playing = null; playingId = ""; try { tts?.stop() } catch (e: Exception) { } }
        } else speechQ.addLast(u)
        playNext()
    }

    private fun playNext() {
        if (playing != null) return
        val now = android.os.SystemClock.elapsedRealtime()
        while (true) {
            val u = speechQ.removeFirstOrNull() ?: break
            // how long each may wait: a turn call only a few seconds (the distance in it is soon wrong), a warning half a minute
            val maxAge = when (u.kind) { 2 -> 6_000L; 1 -> 30_000L; else -> 15_000L }
            if (now - u.at > maxAge) continue
            val ch = prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav")
            if (ch != voiceChan) { voiceChan = ch; try { tts?.setAudioAttributes(speechAttrs) } catch (e: Exception) { } }
            spoken = 1
            duck()
            val id = "nav${++uttSeq}"
            playing = u; playingId = id
            val r = tts?.speak(u.text, TextToSpeech.QUEUE_ADD, null, id)
            if (r == TextToSpeech.ERROR) { playing = null; playingId = ""; continue }
            return
        }
        // nothing left to say: the music comes back up
        spoken = 1; unduck()
    }

    /** The speech engine finished (or stopped) [id]: the next one, if it's still the current one. */
    private fun uttDone(id: String?) {
        speechMain.post {
            if (id == null || id != playingId) return@post
            playing = null; playingId = ""
            playNext()
        }
    }

    // ---- demo drive ----
    fun cycleSim() {
        val r = route
        if (r == null) { toast(ctx.getString(R.string.toast_032)); return }
        when (simMode) {
            0 -> { simMode = 1; startSim(r) }
            1 -> simMode = 4
            else -> stopSim()
        }
    }

    private fun startSim(r: Route) {
        simJob?.cancel()
        simDist = fix?.let { snap(r, it.pos).progress } ?: 0.0
        following = true
        simJob = scope.launch {
            while (true) {
                val cur = route ?: break
                // the position moves by exactly the speed it reports, so the arrow glide never sees a gap it has to jump to close
                // (the old demo moved 16 m a second while claiming 7 m/s near a turn, which the glide "corrected" with a jump halfway round the roundabout)
                val td = turn?.dist ?: 1000.0
                val demoSpeed = (if (td < 150) 7.0 else if (td < 400) 13.0 else if (td < 1200) 20.0 else 27.0) * simMode
                simDist = min(cur.len, simDist + demoSpeed)
                val (pt, hd) = pointAt(cur, simDist)
                onPosition(Fix(pt, hd, demoSpeed, 0.0), false)
                if (simDist >= cur.len) break
                delay(1000)
            }
            simMode = 0
        }
    }

    fun stopSim() { simJob?.cancel(); simJob = null; simMode = 0 }

    var onRoad by mutableStateOf("")
    private var limitRoad = ""
    private var roadShown = ""

    private var limitRoadAt = 0L
    private var limitRoadHead = -1.0
    private var roadCandidate = ""

    /**
     * A road name from the lookup round you. A different name only shows once a second lookup agrees, or straight away if you've
     * clearly turned (40 degrees or more), so it no longer jumps to a side road for a moment at junctions. An empty name (no road
     * found, like a car park) clears it.
     */
    private fun noteRoad(name: String, heading: Double) {
        val now = System.currentTimeMillis()
        val turned = heading >= 0 && limitRoadHead >= 0 && abs(((heading - limitRoadHead + 540.0) % 360.0) - 180.0) >= 40.0
        if (name == limitRoad || name.isBlank() || limitRoad.isBlank() || turned || name == roadCandidate) {
            limitRoad = name; roadCandidate = ""
            limitRoadAt = now; if (heading >= 0) limitRoadHead = heading
        } else roadCandidate = name
        refreshRoad()
    }

    /** Name of the road you are on: from the route while navigating, otherwise from the map data around you. */
    private fun refreshRoad() {
        // the map-data name is dropped once it's 45 seconds old (lookups that kept failing used to leave the last road up)
        val mapRoad = if (System.currentTimeMillis() - limitRoadAt < 45_000L) limitRoad else ""
        val n = Geo.shortRoad(if (route != null && !previewing && onRoad.isNotBlank()) onRoad else mapRoad)
        if (n != roadShown) { roadShown = n; map.setRoadName(n) }
        if (roadNow != n) roadNow = n
    }
    private var laneKey: Pair<Double, LaneSec>? = null

    private fun laneHit(dir: String, maneuver: String): Boolean {
        val m = maneuver.uppercase()
        return when {
            m.contains("U_TURN") -> dir.endsWith("U_TURN")
            m.contains("LEFT") -> dir.contains("LEFT")
            m.contains("RIGHT") -> dir.contains("RIGHT")
            else -> dir == "STRAIGHT"
        }
    }

    /** Paints the lane arrows on the road ahead of the junction (approximate: placed across the route line). */
    /** Set when you're on a motorway or dual carriageway: lane guidance then starts further out. */
    private var laneFast = false
    private var earlyLaneFor = -1.0

    private fun updateLaneMarks(r: Route, cur: Step, sec: LaneSec?, d: Double) {
        val key = if (sec != null && d < (if (laneFast) 1300.0 else 400.0) && prefs.b(PreferenceKeys.LANEMAP, true)) Pair(cur.off, sec) else null
        // the route line stays in its lane until you're past the end of its ease back after the junction (clearing it as soon as
        // the lane guidance ended made the line just ahead of you jump sideways)
        if (laneShiftEnd > 0.0 && key == null && curProgress >= laneShiftEnd) { map.setRouteShift(emptyList()); laneLatAt = null; laneShiftEnd = 0.0; turnArrowKey = -2 }
        // where Mapbox has HD roads (the lanes it paints on motorways), the real lanes are read off the map just before the junction,
        // so the lanes drawn and the route line sit exactly on the painted ones (asked every 3 s until found, up to 8 times)
        if (key != null && hdLanesFor != cur.off) {
            if (hdTriesFor != cur.off) { hdTriesFor = cur.off; hdTries = 0 }
            val now = android.os.SystemClock.uptimeMillis()
            if (hdTries < 8 && now - hdAskAt > 3000L && d < 700.0) {
                hdAskAt = now; hdTries++
                val askOff = cur.off
                val nLanes = sec?.lanes?.size ?: 0
                // several places across the approach: close to the junction the exit lanes have often already split off behind
                // the hatched area, so the lanes are looked at further back too, and followed from one place to the next
                val backs = listOf(150.0, 120.0, 90.0, 60.0, 35.0, 15.0).filter { cur.off - it > 0.0 }
                map.laneCentresAlong(backs.map { pointAt(r, cur.off - it) }) { res, lines ->
                    if (hdTriesFor != askOff) return@laneCentresAlong
                    val prof = ArrayList<Pair<Double, List<Double>>>()
                    res?.forEachIndexed { k, offs -> pickLaneWindow(offs, nLanes)?.let { prof += (askOff - backs[k]) to it } }
                    laneDiag = when {
                        res == null -> "Lane data: the junction isn't on screen yet"
                        lines == 0 -> "Lane data: no HD lanes from Mapbox here (lanes laid out from the road's line)"
                        prof.isEmpty() -> "Lane data: HD lanes found ($lines lines) but they didn't match the $nLanes lanes listed (found " + res.joinToString(", ") { it.size.toString() } + " across the road)"
                        else -> "Lane data: HD lanes lined up at ${prof.size} of ${backs.size} points along the approach"
                    }
                    if (res != null && (prof.size >= 3 || hdTries >= 8) && laneDiagFor != askOff && prefs.b(PreferenceKeys.DEMO, false)) { laneDiagFor = askOff; toast(laneDiag, 5000) }
                    // a better read (more places matched) replaces the last; it keeps looking until at least 3 places match
                    if (prof.isNotEmpty() && (hdProfileFor != askOff || prof.size > hdProfile.size)) { hdProfile = prof.sortedBy { it.first }; hdProfileFor = askOff; laneKey = null }
                    if (prof.size >= 3) hdLanesFor = askOff
                }
            }
        }
        // the lane sign goes once you're up to it
        if (laneSignFor >= 0.0 && curProgress > laneSignFor - 15.0) { map.setLaneSign(null, emptyList()); laneSignFor = -1.0 }
        // the lane bubble rides just ahead of the arrow until you reach the junction
        bubbleLanes?.let { bl ->
            if (key != null && curProgress < cur.off - 20.0) map.setLaneBubble(pointAt(r, min(r.len, curProgress + 24.0)).first, bl) else map.setLaneBubble(null, emptyList())
        }
        if (key == laneKey) return
        laneKey = key
        if (key == null || sec == null) {
            map.setLaneSign(null, emptyList()); laneSignFor = -1.0
            bubbleLanes = null; map.setLaneBubble(null, emptyList())
            map.setLanes(emptyList())
            if (laneShiftEnd <= 0.0 || curProgress >= laneShiftEnd) { map.setRouteShift(emptyList()); laneLatAt = null; laneShiftEnd = 0.0 }
            return
        }
        val n = sec.lanes.size
        val anyFollow = sec.lanes.any { it.follow != null }
        val activeIdx = sec.lanes.indices.filter { i -> val ln = sec.lanes[i]; if (anyFollow) ln.follow != null else ln.dirs.any { laneHit(it, cur.maneuver) } }
        // Lined up with the real lanes (the ones Mapbox's map paints where it has them): the route line runs down the middle of the
        // road as mapped, so the lanes are laid out from there, at real lane widths. On a one-way road or a dual carriageway the
        // lanes are centred on the line; on a two-way road they're all on your side of it (the left in the UK).
        val (approach, approachB) = pointAt(r, max(0.0, cur.off - 45.0))
        val road = map.roadAt(approach, approachB, maxDistance = 10.0, headingTolerance = 25.0)
        // Require a close, direction-compatible road match here: at exits and junctions a nearby ramp or cross-road can
        // otherwise supply the wrong one-way/road-centre metadata and offset the highlighted lane away from the route.
        // If the match is ambiguous, use the lane-relative fallback below so the route remains in the selected lane.
        // (with no road data from the map the old way is used: the lanes laid out with the lane to be in under the route line)
        // Only lined up with the road on one-way motorway and trunk carriageways (not slip roads), where the mapped line really
        // runs down the middle of the lanes. On ordinary A roads and town junctions the lanes often widen or gain a turning lane
        // that the mapped line doesn't follow, which put the lanes (and the route line moved into them) off the road; there the
        // lanes are laid out with the lane to be in under the route line, and the route line stays where it is.
        val big = road?.cls?.let { (it == "motorway" || it == "trunk") } == true
        val aligned = road != null && road.oneway == true && big
        val laneW = if (!aligned) 3.6 else if (big) 3.65 else 3.5
        // Lined up: before a junction the mapped line runs down the middle of the main carriageway, which is the lanes that carry
        // straight on. The lanes peeling off to an exit are extra lanes beside it (at the junction Mapbox lists them all together),
        // so the straight-on lanes are centred on the line and the exit lanes sit off to their side. Centring all of them on the
        // line put every lane a lane or two out towards the exit, over the verge. A true split (no straight-on lane) stays centred.
        val throughIdx = sec.lanes.indices.filter { i -> sec.lanes[i].dirs.any { it == "STRAIGHT" } }
        val centre = if (!aligned) (if (activeIdx.isEmpty()) (n - 1) / 2.0 else activeIdx.average())
            else if (throughIdx.isNotEmpty() && throughIdx.size < n && isContiguous(throughIdx)) throughIdx.average()
            else (n - 1) / 2.0
        // each lane's middle and edges, sideways from the route line (metres, + to the right): the real ones read off the map when
        // they match the lane list, else laid out at standard widths as described above
        // Lanes are only painted on the road (and the route line moved into them) once the real lanes have been read off the
        // map and matched at 3 or more places along the approach. Everywhere else a lane sign stands over the road at the junction
        // instead, like a motorway gantry, so nothing is drawn that might not sit on the painted lanes.
        val prof = if (hdLanesFor == cur.off && hdProfileFor == cur.off) hdProfile.filter { it.second.size == n } else emptyList()
        val lined = prof.isNotEmpty()
        if (!lined && curProgress < cur.off - 15.0) {
            map.setLaneSign(pointAt(r, max(0.0, cur.off - 10.0)).first, sec.lanes.mapIndexed { i, lane ->
                val active = i in activeIdx
                lane.dirs.map { dir -> dir to (active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))) } to active
            })
            laneSignFor = cur.off
            bubbleLanes = null
        } else {
            map.setLaneSign(null, emptyList()); laneSignFor = -1.0
            bubbleLanes = if (lined) sec.lanes.mapIndexed { i, lane ->
                val active = i in activeIdx
                lane.dirs.map { dir -> dir to (active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))) } to active
            } else null
        }
        // each lane's middle at [o] metres along the route: from the real lanes (in between the places they were read, and held
        // at the first / last beyond them), else the standard layout
        fun mids(o: Double): DoubleArray {
            if (prof.isEmpty()) return DoubleArray(n) { i -> (i - centre) * laneW }
            if (o <= prof.first().first) return prof.first().second.toDoubleArray()
            if (o >= prof.last().first) return prof.last().second.toDoubleArray()
            val k = prof.indexOfLast { it.first <= o }
            val (oa, a) = prof[k]; val (ob, b) = prof[k + 1]
            val t = if (ob > oa) (o - oa) / (ob - oa) else 0.0
            return DoubleArray(n) { i -> a[i] + (b[i] - a[i]) * t }
        }
        fun leftE(m: DoubleArray, i: Int) = if (prof.isEmpty()) m[i] - laneW / 2 else if (i > 0) (m[i - 1] + m[i]) / 2 else m[0] - (m[1] - m[0]) / 2
        fun rightE(m: DoubleArray, i: Int) = if (prof.isEmpty()) m[i] + laneW / 2 else if (i < n - 1) (m[i] + m[i + 1]) / 2 else m[n - 1] + (m[n - 1] - m[n - 2]) / 2
        fun shifted(p: LL, brg: Double, lateral: Double): LL {
            val rad = Math.toRadians(brg + 90.0)
            return LL(p.lat + lateral * Math.cos(rad) / 111320.0, p.lon + lateral * Math.sin(rad) / (111320.0 * Math.cos(Math.toRadians(p.lat))))
        }
        val marks = ArrayList<MapBinding.LaneMark>()
        for (back in if (lined) listOf(28.0, 65.0, 105.0) else emptyList()) {
            val off = cur.off - back
            if (off < 0) continue
            val (p, brg) = pointAt(r, off)
            val m = mids(off)
            sec.lanes.forEachIndexed { i, lane ->
                val at = shifted(p, brg, m[i])
                val active = i in activeIdx
                lane.dirs.forEach { dir ->
                    val hit = active && (if (anyFollow) dir == lane.follow else laneHit(dir, cur.maneuver))
                    marks += MapBinding.LaneMark(at, brg, dir, hit)
                }
            }
        }
        // lane strips from up to 130 m before the junction to just short of it
        // TomTom's lane section can be short, but the lanes carry on back along the approach, so the strips do not stop where the section starts
        // (only the last stretch before the junction, where the lanes really are: about 110 m on motorways, 60 m elsewhere,
        // so lanes aren't painted far back along a road that only widens at the junction)
        val from = max(0.0, cur.off - (if (lined) 150.0 else 60.0))
        val to = cur.off - 6.0
        val bands = ArrayList<MapBinding.LaneBand>()
        val divs = ArrayList<MapBinding.LaneEdge>()
        if (lined && to - from > 20.0) {
            val samples = ArrayList<Pair<LL, Double>>()
            val sampleMids = ArrayList<DoubleArray>()
            var o = from
            while (o < to) { samples += pointAt(r, o); sampleMids += mids(o); o += 10.0 }
            samples += pointAt(r, to); sampleMids += mids(to)
            val last = samples.size - 1
            for (i in 0 until n) {
                // cut into short pieces so the ribbon can fade in softly at its start
                for (k in 0 until last) {
                    val l = ArrayList<LL>(); val rt = ArrayList<LL>()
                    for (m in k..k + 1) { val (p, brg) = samples[m]; l += shifted(p, brg, leftE(sampleMids[m], i)); rt += shifted(p, brg, rightE(sampleMids[m], i)) }
                    val a = 0.5 + 0.5 * min(1.0, k / 3.0)
                    bands += MapBinding.LaneBand(l + rt.asReversed(), i in activeIdx, a)
                }
                val edge = ArrayList<LL>(); for ((j, sp) in samples.withIndex()) edge += shifted(sp.first, sp.second, rightE(sampleMids[j], i))
                divs += MapBinding.LaneEdge(edge, i == n - 1)
            }
            val outer = ArrayList<LL>(); for ((j, sp) in samples.withIndex()) outer += shifted(sp.first, sp.second, leftE(sampleMids[j], 0))
            divs += MapBinding.LaneEdge(outer, true)
        }
        map.setLaneBands(bands, divs)
        // the route line moved into the lane you need (when the lanes are lined up with the road). One sideways offset along the
        // route, used for both the route line and the turn arrow so they always meet with no step: it eases across (over up to
        // 60 m), stays in the middle of the lit-up lanes right up to the junction, then eases back onto the road's line over the first
        // 25 m after it (where the route carries on along the slip road or the main road). Before, the line only came back in the
        // last 8 m while the turn arrow, drawn on the road's own line from 38 m out, left a gap and a step where they met.
        val shift = ArrayList<LL>()
        laneLatAt = null; laneShiftEnd = 0.0
        if (lined && activeIdx.isNotEmpty()) {
            // the middle of the lit-up lanes, wherever they are along the approach
            fun midAt(o: Double): Double { val m = mids(o); return activeIdx.map { m[it] }.average() }
            val mid = midAt(cur.off - 40.0)
            val s0 = max(0.0, max(sec.off, cur.off - 250.0))
            val s1 = min(r.len, cur.off + 25.0)
            // (fully in the lane by the time the lane strips start, 110 m out, when there's room; else over the first half)
            val stripStart = cur.off - 150.0
            val ramp = if (stripStart - s0 >= 20.0) min(60.0, stripStart - s0) else max(10.0, (cur.off - s0) * 0.5)
            if (maxOf(Math.abs(mid), Math.abs(midAt(cur.off - 100.0)), Math.abs(midAt(cur.off - 10.0))) > 0.3 && cur.off - s0 > 30.0) {
                val fn: (Double) -> Double = { o ->
                    val inF = ((o - s0) / ramp).coerceIn(0.0, 1.0)
                    val outF = ((s1 - o) / 35.0).coerceIn(0.0, 1.0)     // from 10 m before the junction to 25 m after it
                    val f = minOf(inF, outF)
                    midAt(min(o, cur.off)) * f * f * (3 - 2 * f)
                }
                laneLatAt = fn; laneShiftEnd = s1
                var o = s0
                while (true) {
                    val (p, brg) = pointAt(r, o)
                    shift += shifted(p, brg, fn(o))
                    if (o >= s1) break
                    o = minOf(s1, o + 5.0)
                }
            }
        }
        map.setRouteShift(shift)
        map.setLanes(marks)
    }

    /** The sideways offset (metres, + to the right) of the route line in the lane you need, by distance along the route; null when none. */
    private var laneLatAt: ((Double) -> Double)? = null
    /** The real lanes' middles (sideways from the route line, left to right) read off the map for the junction at [hdLanesFor]. */
    private var hdProfile: List<Pair<Double, List<Double>>> = emptyList()
    /** What the last look for the real lanes found (for Demo mode and the feedback report). */
    var laneDiag = ""
        private set
    private var laneDiagFor = -1.0
    private var hdProfileFor = -1.0
    /** The junction the lane sign is standing at (−1 for none). */
    private var laneSignFor = -1.0
    /** The lanes shown in the bubble just ahead of the arrow (null when none). */
    private var bubbleLanes: List<Pair<List<Pair<String, Boolean>>, Boolean>>? = null
    private var hdLanesFor = -1.0
    private var hdTriesFor = -1.0
    private var hdTries = 0
    private var hdAskAt = 0L

    /**
     * The [n] real lanes that make up your carriageway, out of [offs] (all the lanes found across the road there, left to right):
     * the block of [n] side by side (no gap wider than a lane and a half between them) nearest the route line. Null when they
     * don't match the lane list, so the standard layout is used instead.
     */
    private fun pickLaneWindow(offs: List<Double>, n: Int): List<Double>? {
        if (n < 2 || offs.size < n) return null
        var best: List<Double>? = null; var bestD = Double.MAX_VALUE
        for (s in 0..offs.size - n) {
            val w = offs.subList(s, s + n)
            if ((1 until n).any { w[it] - w[it - 1] !in 2.4..5.5 }) continue
            val mid = w.average()
            if (Math.abs(mid) < bestD) { bestD = Math.abs(mid); best = w }
        }
        // the route line has to be somewhere on that carriageway, or it's another road
        return best?.takeIf { it.first() - 3.0 < 0.0 && it.last() + 3.0 > 0.0 }
    }

    /** Where along the route that offset has eased back to nothing (0 when there's none). */
    private var laneShiftEnd = 0.0

    /** True when the lane numbers follow on from each other (2, 3, 4), so they form one block of lanes. */
    private fun isContiguous(idx: List<Int>): Boolean = idx.isNotEmpty() && idx.last() - idx.first() == idx.size - 1

    /** Index of the last route point at or before [d] metres along the route. */
    private fun idxAt(r: Route, d: Double): Int {
        val c = r.cum
        var lo = 0; var hi = c.size - 1
        while (lo < hi) { val m = (lo + hi + 1) ushr 1; if (c[m] <= d) lo = m else hi = m - 1 }
        return lo
    }

    private var gDoneAt = 0L
    private var doneShown = false

    private fun updateDone(r: Route, prog: Double) {
        // grey right back to where the blue line starts (it's drawn from a little behind the car), so no blue shows behind you;
        // with the whole route on screen (map moved by hand), the whole driven part is grey
        val start = if (!following) 0.0 else max(0.0, min(prog - 800.0, lastDrawProg - 950.0))
        if (prog - start < 2.0) { if (doneShown) { map.setDone(emptyList()); doneShown = false }; return }
        val pts = ArrayList<LL>()
        pts += pointAt(r, start).first
        var i = idxAt(r, start) + 1
        while (i < r.pts.size && r.cum[i] < prog) { pts += r.pts[i]; i++ }
        pts += pointAt(r, prog).first
        map.setDone(pts); doneShown = true
    }

    private fun pointAt(r: Route, d: Double): Pair<LL, Double> {
        val c = r.cum
        var lo = 0; var hi = c.size - 1
        while (lo < hi - 1) { val m = (lo + hi) ushr 1; if (c[m] <= d) lo = m else hi = m }
        val seg = (c[hi] - c[lo]).takeIf { it > 0 } ?: 1.0
        val t = ((d - c[lo]) / seg).coerceIn(0.0, 1.0)
        val a = r.pts[lo]; val b = r.pts[hi]
        return Pair(LL(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t), Geo.bearing(a, b))
    }
}

/**
 * One charging stop on an electric trip: the charger, the charge on arrival and the level to charge to (both in %), and the minutes
 * to charge. [preferred]: the charger belongs to one of the operators preferred in Settings.
 */
data class EvStop(val n: Nearby, val arrPct: Int, val toPct: Int, val minutes: Int, val preferred: Boolean = false)
