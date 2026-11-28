package com.headunit.launcher.car

import com.headunit.launcher.acceptFaster
import com.headunit.launcher.answerVote
import com.headunit.launcher.beginDemo
import com.headunit.launcher.cycleSim
import com.headunit.launcher.skipSimTurn
import com.headunit.launcher.stopSim
import com.headunit.launcher.R
import com.headunit.launcher.Lang

import com.headunit.launcher.PreferenceKeys
import android.app.Presentation
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.FrameLayout
import androidx.car.app.AppManager
import androidx.car.app.CarAppService
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.DateTimeWithZone
import androidx.car.app.model.Distance
import androidx.car.app.model.GridItem
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.ItemList
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.SearchTemplate
import androidx.car.app.model.Template
import androidx.car.app.model.Toggle
import androidx.car.app.navigation.model.LaneDirection
import androidx.car.app.navigation.NavigationManager
import androidx.car.app.navigation.NavigationManagerCallback
import androidx.car.app.navigation.model.Destination
import androidx.car.app.navigation.model.Maneuver
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.car.app.navigation.model.Step
import androidx.car.app.navigation.model.TravelEstimate
import androidx.car.app.navigation.model.Trip
import androidx.car.app.validation.HostValidator
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.headunit.launcher.KEEP_LEFT
import com.headunit.launcher.MapBinding
import com.headunit.launcher.MapboxRest
import com.headunit.launcher.locatorSpec
import com.headunit.launcher.mapDetail
import com.headunit.launcher.NavEngine
import com.headunit.launcher.ReportKind
import com.headunit.launcher.NavService
import com.headunit.launcher.Place
import com.headunit.launcher.Prefs
import com.headunit.launcher.ui.weatherKindOn
import com.headunit.launcher.TurnInfo
import java.util.TimeZone
import com.headunit.launcher.softFail

/**
 * Horizon Nav on Android Auto. Android Auto connects to this service when the phone is plugged into the car; the car screen shows
 * the map (drawn by the app onto a surface Android Auto hands over) under Google's navigation layout: the turn card, the arrival
 * estimate and the buttons. Search and places use Google's list and search screens (third-party navigation apps can't use their own).
 *
 * The car has its own engine and map, separate from the app on the phone's screen, so it works with the phone locked.
 * (Sideloaded copies only appear in Android Auto with its developer setting "Unknown sources" turned on.)
 */
class CarNavService : CarAppService() {
    // any Android Auto host may connect: the app isn't distributed through Play, where Google would vouch for the host
    override fun createHostValidator(): HostValidator = HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    override fun onCreateSession(): Session = CarNavSession()
    // a car with a map on the driver's display asks for a second session for it
    override fun onCreateSession(sessionInfo: androidx.car.app.SessionInfo): Session =
        if (sessionInfo.displayType == androidx.car.app.SessionInfo.DISPLAY_TYPE_CLUSTER) ClusterSession() else CarNavSession()
}

/** The engine and map for one connection to the car. */
class CarHolder(val carContext: CarContext) {
    var engine: NavEngine? = null
    var map: MapBinding? = null
}

class CarNavSession : Session() {
    private lateinit var holder: CarHolder
    private lateinit var surface: CarSurface
    private var mapScreen: MapScreen? = null
    /** A "navigate to" from Google Assistant (or another app) that came before the map was ready. */
    private var pendingNav: Intent? = null
    @Volatile private var pendingNavWait: Thread? = null
    @Volatile private var pendingNavWaitToken = 0L
    private val main = Handler(Looper.getMainLooper())

    override fun onNewIntent(intent: Intent) { takeNav(intent) }

    /**
     * "Hey Google, navigate to …" (and other apps' navigate links) arrive as a geo: address: exact coordinates go straight to the
     * route choice; a name or address opens the search with it filled in; "home" and "work" go to yours.
     */
    private fun takeNav(i: Intent?) {
        if (i == null || i.action != CarContext.ACTION_NAVIGATE) return
        val e = holder.engine
        if (e == null) { pendingNav = i; return }
        val raw = i.dataString ?: return
        pendingNavWaitToken++
        val token = pendingNavWaitToken
        pendingNavWait?.interrupt()
        val worker = Thread {
            val t = try { com.headunit.launcher.ExternalLinks.parse(raw) } catch (x: Exception) { null }
            // a moment for a first position, so the route starts from where you are
            var n = 0
            while (t?.pos != null && e.fix == null && n < 30 && !Thread.currentThread().isInterrupted) {
                try { Thread.sleep(500) } catch (x: InterruptedException) { Thread.currentThread().interrupt(); return@Thread }
                n++
            }
            if (Thread.currentThread().isInterrupted) return@Thread
            main.post {
                if (pendingNavWaitToken == token) pendingNavWait = null
                val ms = mapScreen ?: return@post
                if (holder.engine !== e) return@post
                try { ms.screenManager.popToRoot() } catch (x: Exception) { softFail(x) }
                val p = t?.pos
                val q = spoken(t?.query.orEmpty())
                val slot = when (q.lowercase()) {
                    "home", "zuhause", "zu hause", "hause", "daheim", "heim", "nach hause" -> "home"
                    "work", "arbeit", "die arbeit", "büro", "das büro", "firma", "die firma" -> "work"
                    else -> null
                }
                val fav = if (q.isNotEmpty()) e.places.favourites().firstOrNull { it.name.trim().equals(q, true) } else null
                when {
                    p != null -> e.askNewDest(Place(t?.name ?: Lang.t("Ziel", "Destination"), Lang.t("Ziel aus Google Assistant", "From Google Assistant"), p))
                    slot != null && e.places.slot(slot) != null -> e.askNewDest(e.places.slot(slot)!!)
                    slot != null -> try { androidx.car.app.CarToast.makeText(carContext, if (slot == "home") Lang.t("Noch kein Zuhause gespeichert", "No home saved yet") else Lang.t("Noch keine Arbeit gespeichert", "No work place saved yet"), androidx.car.app.CarToast.LENGTH_LONG).show() } catch (x: Exception) { softFail(x) }
                    fav != null -> e.askNewDest(fav)
                    // a name or address: searched, and a single clear match is started straight away
                    q.isNotEmpty() -> ms.screenManager.push(SearchScreen(carContext, holder, q, voice = true))
                    else -> try { androidx.car.app.CarToast.makeText(carContext, Lang.t("Konnte keinen Ort erkennen", "Couldn't read a place from that"), androidx.car.app.CarToast.LENGTH_LONG).show() } catch (x: Exception) { softFail(x) }
                }
            }
        }.apply { isDaemon = true; name = "car-nav-intent" }
        pendingNavWait = worker
        worker.start()
    }

    /** What Google Assistant passed on, without a leading "nach" / "zu" ("navigiere nach Berlin" can arrive as "nach Berlin"). */
    private fun spoken(raw: String): String {
        var q = raw.trim().trim('"', '\'', '„', '“', '.', '?', '!')
        for (w in listOf("navigiere ", "navigate ", "fahre ", "to ", "nach ", "zum ", "zur ", "zu ", "in die ", "in den "))
            if (q.length > w.length + 1 && q.lowercase().startsWith(w) && !(w == "nach " && q.lowercase() == "nach hause")) q = q.substring(w.length).trim()
        return q
    }

    /** Night colours on the car screen: follow the car, or always day / always night (quick settings on the car). */
    private fun carDark(): Boolean = if (Prefs(carContext.applicationContext).s(PreferenceKeys.LOOK, "standard") == "neon") true else when (Prefs(carContext.applicationContext).s(PreferenceKeys.CARTHEME, "car")) {
        "day" -> false
        "night" -> true
        // the same as the app on the phone (its own Day and night setting)
        "phone" -> com.headunit.launcher.ui.phoneDark(carContext.applicationContext, Prefs(carContext.applicationContext))
        else -> {
            // "Sonnenzeiten auf Android Auto": sunrise and sunset decide, even for a car that goes dark whenever its headlights are on
            val p = Prefs(carContext.applicationContext)
            val la = p.s(PreferenceKeys.LASTLAT, "").toDoubleOrNull(); val lo = p.s(PreferenceKeys.LASTLON, "").toDoubleOrNull()
            if (p.b(PreferenceKeys.CARSUN, false) && la != null && lo != null) !com.headunit.launcher.Sun.isDay(la, lo, System.currentTimeMillis())
            else carContext.isDarkMode
        }
    }

    private var themeWas: Boolean? = null
    private var themeAt = 0L

    /** "Same as your phone": the phone's day and night can change with no word from the car, so it is looked at every 15 seconds. */
    // the rain, snow and haze of the weather on the car's map too (the phone's map got them from the app screen only)
    private var wxShown: Any? = null
    private fun weatherTick() {
        val e = holder.engine ?: return
        val m = holder.map ?: return
        // (none while Cool running is on, see CarCool)
        val on = !com.headunit.launcher.CarCool.on && try { Prefs(carContext.applicationContext).b(PreferenceKeys.RAIN_EFFECTS, false) } catch (x: Exception) { false }
        val sky = try { Prefs(carContext.applicationContext).b(PreferenceKeys.SKYWEATHER, true) } catch (x: Exception) { true }
        val code = if (on) try { weatherKindOn(Prefs(carContext.applicationContext), e.weather?.code) } catch (x: Exception) { e.weather?.code } else null
        val skyCode = if (sky) e.weather?.code else null
        val key = Pair(code, skyCode)
        if (key == wxShown) return
        wxShown = key
        try { m.setWeather(code); m.setSky(skyCode) } catch (x: Exception) { softFail(x) }
    }

    private fun themeTick() {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - themeAt < 15_000L) return
        themeAt = now
        try {
            if (Prefs(carContext.applicationContext).s(PreferenceKeys.CARTHEME, "car") == "phone" && carDark() != themeWas) applyTheme()
        } catch (e: Exception) { softFail(e) }
    }

    private fun applyTheme() {
        val d = carDark()
        themeWas = d
        ClusterLink.dark = d
        holder.engine?.applyStyle(d)
        if (::surface.isInitialized) surface.dark.value = d
    }

    override fun onCreateScreen(intent: Intent): Screen {
        holder = CarHolder(carContext)
        val screen = MapScreen(carContext, holder)
        mapScreen = screen
        surface = CarSurface(carContext) { map -> startEngine(map) }
        surface.actions = object : com.headunit.launcher.ui.CarActions {
            override fun search() { screen.screenManager.push(SearchScreen(carContext, holder)) }
            override fun places() { screen.screenManager.push(PlacesScreen(carContext, holder)) }
            override fun nearby(kind: String) { screen.screenManager.push(NearbyScreen(carContext, holder, kind)) }
            override fun settings() { screen.screenManager.push(CarSettingsScreen(carContext, holder) { applyTheme() }) }
            override fun report() { screen.screenManager.push(ReportScreen(carContext, holder)) }
        }
        screen.openSettings = { screen.screenManager.push(CarSettingsScreen(carContext, holder) { applyTheme() }) }
        // the battery level from the car itself, when Android Auto shares it (the widget is the fallback)
        try { CarEnergy.start(carContext, Prefs(carContext.applicationContext)) } catch (e: Exception) { softFail(e) }
        // and the battery widget's refresh button tapped now and then, so its reading stays fresh with the phone locked
        try { com.headunit.launcher.EvWidget.autoRefreshStart(carContext, Prefs(carContext.applicationContext)) } catch (e: Exception) { softFail(e) }
        try { carContext.getCarService(AppManager::class.java).setSurfaceCallback(surface) } catch (e: Exception) { softFail(e) }
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                screen.stop()
                try { CarEnergy.stop() } catch (e: Exception) { softFail(e) }
                try { com.headunit.launcher.EvWidget.autoRefreshStop() } catch (e: Exception) { softFail(e) }
                pendingNavWaitToken++
                pendingNavWait?.interrupt()
                pendingNavWait = null
                // unplugged: the car's engine stops, but a trip the phone is also following carries on there (with its voice back)
                try { holder.engine?.shutdown() } catch (e: Exception) { softFail(e) }
                try { if (!com.headunit.launcher.RouteSync.anyGuiding()) NavService.stop(carContext.applicationContext) } catch (e: Exception) { softFail(e) }
                CarTicks.tick = null
                surface.release()
                holder.engine = null; holder.map = null
                ClusterLink.engine = null
            }
        })
        pendingNav = intent
        return screen
    }

    /** The map is on the car screen: start an engine for it, with the same settings as the app on the phone. */
    private fun startEngine(map: MapBinding) {
        val app = carContext.applicationContext
        val prefs = Prefs(app)
        map.setTheme("immersive")
        map.setArrowScale((prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toIntOrNull() ?: 240) / 100f)
        // the same map look and locator as on the phone (your own picture isn't used on the car screen: the 3D arrow instead)
        val (loc, locCol) = prefs.locatorSpec()
        if (loc == "custom") map.setLocator("arrow", prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")) else map.setLocator(loc, locCol)
        // Keep the locator proportionate to the more compact Android Auto overlay sizing.
        // Android Auto uses a smaller map surface than the main head-unit app. Keep the
        // user preference, but normalise it so the 240% main-app default doesn't create an enormous car-screen locator.
        map.setArrowScale(((prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toIntOrNull() ?: 240) / 100f).coerceIn(1f, 5f) * 0.9f * (prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 70).coerceIn(30, 180) / 100f))
        map.setMapDetail(if (com.headunit.launcher.CarCool.on) "lite" else prefs.mapDetail())
        map.setTrafficGreen(prefs.b(PreferenceKeys.TRAFFIC_GREEN, false))
        map.setCarModel(prefs.s(PreferenceKeys.CAR_MODEL, "saloon"))
        map.setRoadNameScale(prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).coerceIn(70, 200) / 100f)
        map.setColours(if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon") "neon" else prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox"))
        map.flushBranding = true
        map.setLogoCorner(prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"))
        map.setExtras(prefs.b(PreferenceKeys.FACADES, true), prefs.b(PreferenceKeys.LANDMARKS3D, true), prefs.b(PreferenceKeys.PATHS, true), prefs.b(PreferenceKeys.TERRAIN, false),
            false, prefs.b(PreferenceKeys.FURNITURE3D, true), prefs.b(PreferenceKeys.CLEARVIEW, true))
        val engine = NavEngine(app, prefs, map)
        // the car screen does the talking while it's connected, and shares the trip with the phone
        engine.isCar = true
        // the car keeps its own map direction (north up or turning with you)
        engine.northUp = prefs.b(PreferenceKeys.NORTHUP_CAR, false)
        // back in the car: the "Your car" spot on the phone isn't needed any more
        try { prefs.put(PreferenceKeys.PARKED_SPOT, "") } catch (e: Exception) { softFail(e) }
        // the Bluetooth device that just connected is the car (for "where you parked" without Android Auto)
        try { com.headunit.launcher.CarBluetooth.carStarted(app) } catch (e: Exception) { softFail(e) }
        holder.map = map; holder.engine = engine
        ClusterLink.engine = engine
        surface.engine = engine
        CarTicks.tick = { surface.tick(); themeTick(); weatherTick() }
        applyTheme()
        try { engine.startLocation() } catch (e: Exception) { softFail(e) }
        engine.restoreFreeZoomNow()
        // the phone's layer choices (traffic on the route only, rain radar) on the car map too
        try { engine.applyLayer(PreferenceKeys.ROUTETRAFFICONLY); engine.applyLayer(PreferenceKeys.RAIN_RADAR) } catch (e: Exception) { softFail(e) }
        mapScreen?.start()
        // asked for a destination before the map was up (Android Auto opened the app for it)
        pendingNav?.let { pendingNav = null; takeNav(it) }
    }

    override fun onCarConfigurationChanged(newConfiguration: Configuration) {
        // day / night on the car screen follows the car (its headlights or its own setting), unless set otherwise on the car
        applyTheme()
    }
}

/**
 * Puts the app's map on the car screen: Android Auto gives a surface; a private virtual display shows on it, and the map (a normal
 * map view with the locator and road name on top) runs on that display. Drags and pinches on the car screen move it.
 */
class CarSurface(private val carContext: CarContext, private val onReady: (MapBinding) -> Unit) : SurfaceCallback {
    private var display: VirtualDisplay? = null
    private var presentation: Presentation? = null
    private var map: MapBinding? = null
    private var overlay: androidx.compose.ui.platform.ComposeView? = null
    private val owner = CarComposeOwner()
    /** The part of the car screen Android Auto leaves free (for the cards), and whether the car wants night colours. */
    val area = androidx.compose.runtime.mutableStateOf<Rect?>(null)
    val dark = androidx.compose.runtime.mutableStateOf(true)
    var engine: NavEngine? = null
        set(v) {
            field = v
            if (v != null) {
                v.setCarViewport(width, height, currentDpi)
                overlay?.setContent { CarMapSettings(dark.value); com.headunit.launcher.ui.CarOverlay(v, area, dark, uiScale, actions) }
            }
        }
    /**
     * The phone's satellite, 3D-building and satellite-source settings, applied to the car map as they change (switching to
     * satellite on the phone while connected used to reach the car only after reconnecting).
     */
    @androidx.compose.runtime.Composable
    private fun CarMapSettings(dark: Boolean) {
        val ctx = overlay?.context ?: return
        val prefs = androidx.compose.runtime.remember { Prefs(ctx.applicationContext) }
        val sat = prefs.b(PreferenceKeys.SATELLITE, false)
        val b3d = prefs.b(PreferenceKeys.BUILDINGS_3D, true)
        val sig = com.headunit.launcher.SatelliteSources.signature(prefs)
        androidx.compose.runtime.LaunchedEffect(sat, b3d) { map?.setView(sat, b3d) }
        // satellite on or off changes the map's style: the car then takes the same style choice the phone makes
        androidx.compose.runtime.LaunchedEffect(sat) { engine?.applyStyle(dark) }
        androidx.compose.runtime.LaunchedEffect(sig) { map?.setSatSource(sig) }
    }

    /** What the app's own search bar and Lang.t("Gespeichert & zuletzt", "Saved & recent") chip open (set by the map screen). */
    var actions: com.headunit.launcher.ui.CarActions = object : com.headunit.launcher.ui.CarActions { override fun search() {}; override fun places() {}; override fun nearby(kind: String) {}; override fun settings() {} }
    /** How much larger the app's cards are drawn on the car screen than its own pixel density would make them (same as the map). */
    private val uiScale = androidx.compose.runtime.mutableStateOf(1.7f)
    private var displayDensity = 1f
    private var currentDpi = 160
    private var width = 1; private var height = 1

    /**
     * Establish a sensible baseline for the app-drawn overlay on different Android Auto displays.
     * The old 720px target and 2.4x upper bound made overlays disproportionately large on high-resolution
     * or tall displays. Use a more conservative short-edge target, with tighter bounds; CarOverlay then
     * uses the available width/height to choose an appropriate arrangement. The user's carscale setting
     * remains a separate multiplier in CarOverlay.
     */
    private fun fitScale() {
        val dens = displayDensity.coerceAtLeast(0.5f)
        val short = minOf(width, height).toFloat()
        // raised from 0.68–0.98: on the car screens tested, the search bar, chips and sound button were too small to read while driving
        val perUnit = (short / 1050f).coerceIn(dens * 0.85f, dens * 1.2f)
        uiScale.value = perUnit / dens
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        val s = surfaceContainer.surface ?: return
        width = surfaceContainer.width.coerceAtLeast(1); height = surfaceContainer.height.coerceAtLeast(1)
        currentDpi = surfaceContainer.dpi.coerceAtLeast(80)
        engine?.setCarViewport(width, height, currentDpi)
        engine?.let { it.carSurfaceSeen = true; it.carHorizonShown = true }
        val existing = display
        if (existing != null) {
            // The car screen came back or changed size: keep the same map, but refresh its dimensions and scale.
            existing.surface = s
            existing.resize(width, height, surfaceContainer.dpi)
            displayDensity = presentation?.context?.resources?.displayMetrics?.density
                ?: (surfaceContainer.dpi / 160f).coerceAtLeast(0.5f)
            fitScale()
            map?.carResume()
            return
        }
        try {
            val dm = carContext.getSystemService(DisplayManager::class.java)
            val vd = dm.createVirtualDisplay("HorizonNavCar", width, height, surfaceContainer.dpi, s, DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY)
            display = vd
            val p = Presentation(carContext, vd.display)
            // car screens have few pixels per inch for their size: drawn at the phone's own scale the map looked far too zoomed out
            // (and had to draw a huge area, which made it laggy), so it's drawn larger, like the app on a head unit
            val dens = p.context.resources.displayMetrics.density
            // (1.3x, down from 1.7x: with the zoom out that went with it, the car map sat so far out that Mapbox's 3D trees and other
            // close-up detail never appeared. Now it's drawn at close to the phone's zoom, with text a little smaller than before)
            val ratio = (dens * 1.3f).coerceIn(1.25f, 2.5f)
            displayDensity = dens
            fitScale()
            // the Mapbox token has to be in place before the map is made. The phone app sets it when it opens, but when Android Auto
            // starts the app by itself (after an update, or a restart, before the app has been opened on the phone) it wasn't set yet,
            // so the map couldn't be made and the car screen stayed black
            try {
                val tok = MapboxRest.clean(Prefs(p.context.applicationContext).s(PreferenceKeys.MAPBOX_TOKEN, ""))
                if (tok.isNotEmpty() && com.mapbox.common.MapboxOptions.accessToken != tok) com.mapbox.common.MapboxOptions.accessToken = tok
            } catch (e: Throwable) { softFail(e) }
            // drawn straight to its own surface (no extra copy each frame, which a texture view would cost)
            val m = MapBinding(p.context, true, 1f, ratio)
            // Android Auto follows the Street lamps setting, like the trees and the other 3D details
            m.lampsEnabled(Prefs(p.context.applicationContext).b(PreferenceKeys.LAMPS, false))
            // a touch thicker route line on the car screen, where it looked thin against the buildings
            m.routeBoost = 1.2f
            m.carScale = Prefs(p.context.applicationContext).i(PreferenceKeys.CARSCALE, 100).coerceIn(40, 300) / 100.0
            // the satellite and 3D-building choices from the phone's settings apply to the car map as well (it used to ignore them)
            try {
                val carPrefs = Prefs(p.context.applicationContext)
                m.setView(carPrefs.b(PreferenceKeys.SATELLITE, false), carPrefs.b(PreferenceKeys.BUILDINGS_3D, true))
            } catch (e: Throwable) { softFail(e) }
            val root = FrameLayout(p.context)
            root.addView(m.mapView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            root.addView(m.roadView, FrameLayout.LayoutParams(1, 1))
            root.addView(m.locatorView, FrameLayout.LayoutParams(1, 1))
            // the app's own cards (turn card, speed, trip figures) drawn on top with the app's own designs
            owner.start()
            root.setViewTreeLifecycleOwner(owner)
            root.setViewTreeSavedStateRegistryOwner(owner)
            root.setViewTreeViewModelStoreOwner(owner)
            overlay = androidx.compose.ui.platform.ComposeView(p.context)
            root.addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
            p.setContentView(root)
            p.show()
            try { m.mapView.onStart() } catch (e: Throwable) { softFail(e) }
            presentation = p; map = m
            area.value?.let { onVisibleAreaChanged(it) }
            onReady(m)
        } catch (e: Exception) { android.util.Log.w("CarNav", "couldn't put the map on the car screen", e) }
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        // nothing to draw on until it comes back: the map rests instead of drawing to nowhere
        map?.carPause()
        display?.surface = null
        engine?.carHorizonShown = false
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
        // some head units (e.g. Uconnect 8.4) report a visible area that can't be right (a sliver of the screen, or one off the
        // edge): going by it put the arrow under the cards on the left. Then the whole screen is used, as when nothing is reported
        // (a narrow strip is real, though: with another app open beside the map, e.g. music, Android Auto leaves the map only a
        // column of about a third of the screen. Treating that as wrong put your arrow behind the other app, so from a fifth on it counts)
        val plausible = width <= 1 || height <= 1 || (visibleArea.width() >= width * 0.2 && visibleArea.height() >= height * 0.3 &&
            visibleArea.left >= 0 && visibleArea.top >= 0 && visibleArea.right <= width + 2 && visibleArea.bottom <= height + 2)
        if (!plausible) {
            area.value = null
            map?.let { it.viewInset = null; it.maxTopFrac = 1.0; try { it.placeBranding(null) } catch (e: Exception) { softFail(e) } }
            engine?.viewAreaChanged()
            return
        }
        area.value = Rect(visibleArea)
        // the map's locator goes in the part Android Auto leaves visible (on tall screens its cards cover the bottom of the map),
        // and sits a little higher on tall screens so the speed and trip cards at the bottom don't cover it
        val m = map ?: return
        if (visibleArea.width() > 0 && visibleArea.height() > 0) {
            m.viewInset = Rect(visibleArea.left.coerceAtLeast(0), visibleArea.top.coerceAtLeast(0),
                (width - visibleArea.right).coerceAtLeast(0), (height - visibleArea.bottom).coerceAtLeast(0))
            // Mapbox's logo and credits sit in the real corner of the screen (top right by default), not in the visible part
            m.placeBranding(null)
            // (the cards along the bottom of a tall screen are kept clear by the engine, which the overlay tells where they are)
            m.maxTopFrac = 1.0
        }
        engine?.viewAreaChanged()
    }

    override fun onStableAreaChanged(stableArea: Rect) {}

    override fun onScroll(distanceX: Float, distanceY: Float) {
        com.headunit.launcher.ui.CarTaps.touched()
        val m = map ?: return
        m.onUserGesture?.invoke()
        engine?.onCarGesture()
        try { m.panBy(distanceX, distanceY) } catch (e: Exception) { softFail(e) }
    }

    override fun onFling(velocityX: Float, velocityY: Float) {}

    // a tap on the car screen (newer cars): the app's own cards and buttons react, as in the app
    override fun onClick(x: Float, y: Float) {
        com.headunit.launcher.ui.CarTaps.touched()
        try {
            // the app's cards and buttons first, then the map itself (alternative route cards and lines, skipping the fly-through)
            // (a tap on an empty spot of the map picks it, with a card to go there: Android Auto passes no long press)
            if (!com.headunit.launcher.ui.CarTaps.hit(x, y)) {
                // the app's own cards and buttons first, then the map itself (alternative route cards and lines, skipping the fly-through)
                // (a tap on an empty spot of the map picks it, with a card to go there: Android Auto passes no long press)
                val pt = android.graphics.PointF(x, y)
                if (map?.tapAt(pt) != true) engine?.carMapTap(pt)
            }
        } catch (e: Exception) { softFail(e) }
    }

    override fun onScale(focusX: Float, focusY: Float, scaleFactor: Float) {
        com.headunit.launcher.ui.CarTaps.touched()
        val m = map ?: return
        m.onUserGesture?.invoke()
        engine?.onCarGesture()
        // a double tap comes as a negative factor: zoom in one step
        val dz = if (scaleFactor <= 0f) 1.0 else Math.log(scaleFactor.toDouble()) / Math.log(2.0)
        try { m.zoomAround(dz, focusX, focusY) } catch (e: Exception) { softFail(e) }
    }

    /** Called about once a second (the cards update themselves). */
    fun tick() {}

    fun release() {
        try { owner.stop() } catch (e: Exception) { softFail(e) }
        try { map?.destroy() } catch (e: Exception) { softFail(e) }
        try { presentation?.dismiss() } catch (e: Exception) { softFail(e) }
        try { display?.release() } catch (e: Exception) { softFail(e) }
        map = null; presentation = null; display = null
    }
}

/** Lifecycle and saved state for the cards drawn on the car screen (Compose needs them; there's no activity on the car). */
class CarComposeOwner : androidx.lifecycle.LifecycleOwner, androidx.savedstate.SavedStateRegistryOwner, androidx.lifecycle.ViewModelStoreOwner {
    private val reg = androidx.lifecycle.LifecycleRegistry(this)
    private val saved = androidx.savedstate.SavedStateRegistryController.create(this)
    private val store = androidx.lifecycle.ViewModelStore()
    override val lifecycle: androidx.lifecycle.Lifecycle get() = reg
    override val savedStateRegistry: androidx.savedstate.SavedStateRegistry get() = saved.savedStateRegistry
    override val viewModelStore: androidx.lifecycle.ViewModelStore get() = store
    private var on = false
    fun start() {
        if (on) return
        on = true
        saved.performRestore(null)
        reg.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
    }
    fun stop() {
        if (!on) return
        reg.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
        store.clear()
    }
}


/** Lets the map screen's once-a-second tick also update the speed sign drawn on the map. */
object CarTicks { var tick: (() -> Unit)? = null }
