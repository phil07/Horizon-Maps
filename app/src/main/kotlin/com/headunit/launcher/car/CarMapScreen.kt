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

/** The main car screen: the map with the turn card and arrival estimate while navigating, and buttons for search and places. */
class MapScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var lastSig = ""
    private var lastTrip = ""
    /** The shared report last asked about in the car ("Stau noch da?"), so each is asked only once. */
    private var lastVoteId = 0L
    /** The faster route offer last shown as a pop-up (by identity; 0 when none), and the pop-up's id. */
    private var lastFasterKey = 0
    private val FASTER_ALERT_ID = 4711
    /** The stop reached last shown as a pop-up (its name; null when none), and the pop-up's id. */
    private var lastStopShown: String? = null
    private val STOP_ALERT_ID = 4712
    private var navigating = false
    private var autoDrive = false
    /** Opens the car's quick settings (set by the session). */
    var openSettings: () -> Unit = {}
    private val nav: NavigationManager? = try { carContext.getCarService(NavigationManager::class.java) } catch (e: Exception) { null }

    init {
        try {
            nav?.setNavigationManagerCallback(object : NavigationManagerCallback {
                // the car (or another navigation app) asked to stop
                override fun onStopNavigation() { holder.engine?.endRoute() }
                // Google's test tool: simulate driving to the chosen destination
                override fun onAutoDriveEnabled() { autoDrive = true }
            })
        } catch (e: Exception) { softFail(e) }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    fun start() { if (!running) { running = true; handler.post(tick) } }
    fun stop() {
        running = false; handler.removeCallbacks(tick)
        if (navigating) try { nav?.navigationEnded() } catch (e: Exception) { softFail(e) }
        navigating = false
    }

    private fun refresh() {
        val e = holder.engine ?: return
        val guiding = e.route != null && !e.previewing
        // tell Android Auto when navigation starts and ends (it shows turns in the car's dashboard and notifications)
        if (guiding != navigating) {
            navigating = guiding
            lastTrip = ""
            try {
                if (guiding) {
                    nav?.navigationStarted()
                    try { NavService.start(holder.carContext.applicationContext, "Navigation nach ${e.dest?.name ?: "deinem Ziel"}") } catch (x: Exception) { softFail(x) }
                } else {
                    nav?.navigationEnded()
                    // (the phone may still be guiding a trip of its own: then its background GPS service stays on)
                    try { if (!com.headunit.launcher.RouteSync.anyGuiding()) NavService.stop(holder.carContext.applicationContext) } catch (x: Exception) { softFail(x) }
                }
            } catch (x: Exception) { softFail(x) }
        }
        if (autoDrive && guiding && e.simMode == 0) e.cycleSim()
        val t = e.turn; val eta = e.eta
        // Android Auto's buttons only change with the state (rebuilding them every second while the distance counts down made the
        // host redo its layout for nothing); the turn and distance go to the car's displays through updateTrip
        val sig = "${guiding}|${e.previewing}|${e.active}|${e.routes.size}|${e.busy}|${e.simMode}|${e.faster?.savedMin}|${e.soundMode}|${e.following}"
        if (sig != lastSig) { lastSig = sig; invalidate() }
        val tsig = "${t?.maneuver}|${t?.street}|${t?.exit}|${t?.dist?.let { Math.round(it / 10) }}|${eta?.secs?.let { Math.round(it / 30) }}|${eta?.dist?.let { Math.round(it / 100) }}|${System.identityHashCode(t?.lanes)}"
        if (guiding && t != null && eta != null && tsig != lastTrip) {
            lastTrip = tsig
            try { nav?.updateTrip(trip(e, t, eta)) } catch (x: Exception) { softFail(x) }
        }
        // passing another driver's shared report: ask in the car too (Android Auto's alert with two buttons; car API 5 and newer)
        val vp = e.votePrompt
        if (vp != null && vp.id != lastVoteId) {
            lastVoteId = vp.id
            if (carContext.carAppApiLevel >= 5) try { showVoteAlert(e, vp) } catch (x: Exception) { softFail(x) }
        }
        // a faster route found: the same offer as a pop-up with the minutes and the distance saved (Android Auto's alert with two
        // buttons, car API 5 and newer); it goes when the offer is gone
        val fr = e.faster
        val frKey = fr?.let { System.identityHashCode(it) } ?: 0
        if (frKey != lastFasterKey) {
            val had = lastFasterKey != 0
            lastFasterKey = frKey
            if (carContext.carAppApiLevel >= 5) try {
                if (fr != null) showFasterAlert(e, fr)
                else if (had) carContext.getCarService(AppManager::class.java).dismissAlert(FASTER_ALERT_ID)
            } catch (x: Exception) { softFail(x) }
        }
        // a stop on the way reached: the pop-up once, and the trip waits until "Fahrt weitermachen" (on the car and on the phone)
        val sp0 = e.stopPause
        if (sp0 != lastStopShown) {
            val had = lastStopShown
            lastStopShown = sp0
            if (carContext.carAppApiLevel >= 5) try {
                if (sp0 != null) showStopAlert(e, sp0)
                else if (had != null) carContext.getCarService(AppManager::class.java).dismissAlert(STOP_ALERT_ID)
            } catch (x: Exception) { softFail(x) }
        }
        CarTicks.tick?.invoke()
    }

    private fun showStopAlert(e: NavEngine, name: String) {
        val am = carContext.getCarService(AppManager::class.java)
        val go = Action.Builder().setTitle(Lang.t("Fahrt weitermachen", "Continue trip")).setOnClickListener {
            e.resumeAfterStop(); try { am.dismissAlert(STOP_ALERT_ID) } catch (x: Exception) { softFail(x) }
        }.build()
        val alert = androidx.car.app.model.Alert.Builder(STOP_ALERT_ID, androidx.car.app.model.CarText.create(Lang.t("Zwischenziel erreicht: $name", "Stop reached: $name")), 60_000L)
            .addAction(go)
            .setCallback(object : androidx.car.app.model.AlertCallback {
                override fun onCancel(reason: Int) { }
                override fun onDismiss() { }
            })
            .build()
        am.showAlert(alert)
    }

    private fun showFasterAlert(e: NavEngine, fr: NavEngine.FasterRoute) {
        val am = carContext.getCarService(AppManager::class.java)
        // the distance saved: the current route against the faster one (negative when the faster one is longer)
        val diff = (e.route?.len ?: fr.route.len) - fr.route.len
        val dist = com.headunit.launcher.Geo.fmtDist(kotlin.math.abs(diff), e.imperial)
        val text = if (diff >= 0) Lang.t("Schnellere Route: spart ${fr.savedMin} Min. · $dist kürzer", "Faster route: saves ${fr.savedMin} min · $dist shorter")
            else Lang.t("Schnellere Route: spart ${fr.savedMin} Min. · $dist länger", "Faster route: saves ${fr.savedMin} min · $dist longer")
        val yes = Action.Builder().setTitle(Lang.t("Wechseln", "Switch")).setOnClickListener {
            e.acceptFaster(); try { am.dismissAlert(FASTER_ALERT_ID) } catch (x: Exception) { softFail(x) }
        }.build()
        val no = Action.Builder().setTitle(Lang.t("Beibehalten", "Keep")).setOnClickListener {
            e.dismissFaster(); try { am.dismissAlert(FASTER_ALERT_ID) } catch (x: Exception) { softFail(x) }
        }.build()
        val alert = androidx.car.app.model.Alert.Builder(FASTER_ALERT_ID, androidx.car.app.model.CarText.create(text), 20_000L)
            .addAction(yes).addAction(no)
            .setCallback(object : androidx.car.app.model.AlertCallback {
                override fun onCancel(reason: Int) { }
                override fun onDismiss() { }
            })
            .build()
        am.showAlert(alert)
    }

    private fun showVoteAlert(e: NavEngine, vp: NavEngine.VotePrompt) {
        val am = carContext.getCarService(AppManager::class.java)
        val alertId = (vp.id % Int.MAX_VALUE).toInt()
        val still = Action.Builder().setTitle(Lang.t("Noch da", "Still there")).setOnClickListener {
            e.answerVote(true); try { am.dismissAlert(alertId) } catch (x: Exception) { softFail(x) }
        }.build()
        val gone = Action.Builder().setTitle(Lang.t("Weg", "Gone")).setOnClickListener {
            e.answerVote(false); try { am.dismissAlert(alertId) } catch (x: Exception) { softFail(x) }
        }.build()
        val alert = androidx.car.app.model.Alert.Builder(alertId, androidx.car.app.model.CarText.create(Lang.t("${vp.name} noch da?", "${vp.name} still there?")), 12_000L)
            .addAction(still).addAction(gone)
            .setCallback(object : androidx.car.app.model.AlertCallback {
                override fun onCancel(reason: Int) { }
                override fun onDismiss() { }
            })
            .build()
        am.showAlert(alert)
    }

    private fun icon(res: Int) = CarIcon.Builder(IconCompat.createWithResource(carContext, res)).build()

    /** Melden (Polizei, Unfall, Gefahr …): opens the "Hier melden" screen. It has the Search button's place. */
    private fun reportAction(): Action = if (holder.engine?.carShort == true)
        // a short screen: the icon alone (its label is the same word everywhere, so it's read from the icon)
        Action.Builder().setIcon(icon(R.drawable.ic_car_report)).setOnClickListener { screenManager.push(ReportScreen(carContext, holder)) }.build()
    else Action.Builder().setTitle(Lang.t("Melden", "Report"))
        .setOnClickListener { screenManager.push(ReportScreen(carContext, holder)) }.build()

    override fun onGetTemplate(): Template = try {
        buildTemplate()
    } catch (ex: RuntimeException) {
        // a template the car refuses (too many buttons: IllegalArgumentException; an empty strip: IllegalStateException) must not
        // close the app: show the map with Melden only
        android.util.Log.w("HorizonCar", "Map template refused: ${ex.message}")
        NavigationTemplate.Builder()
            .setActionStrip(ActionStrip.Builder().addAction(reportAction()).build())
            .setMapActionStrip(ActionStrip.Builder().addAction(Action.PAN).build())
            .build()
    }

    private fun buildTemplate(): Template {
        val e = holder.engine
        val guiding = e != null && e.route != null && !e.previewing
        val strip = ActionStrip.Builder()
        val previewing = e != null && e.route != null && e.previewing
        if (previewing) {
            // the route choice (drawn by the app on the map): start, try the next route, or cancel
            strip.addAction(Action.Builder().setTitle("Starten").setOnClickListener { e?.beginNavigation(); invalidate() }.build())
            if (e != null && e.prefs.b(PreferenceKeys.DEMO, false)) strip.addAction(Action.Builder().setTitle("Demo").setOnClickListener { e.beginDemo(); invalidate() }.build())
            if ((e?.routes?.size ?: 0) > 1) strip.addAction(Action.Builder().setTitle("Nächste Route").setOnClickListener {
                e?.let { en -> en.selectRoute((en.active + 1) % en.routes.size.coerceAtMost(3)) }; invalidate()
            }.build())
            strip.addAction(Action.Builder().setTitle("Abbrechen").setOnClickListener { e?.endRoute(); invalidate() }.build())
            return NavigationTemplate.Builder().setActionStrip(strip.build()).build()
        }
        val taps = carContext.carAppApiLevel >= 5
        com.headunit.launcher.ui.CarTaps.available = taps
        // voice guidance: all / alerts only / off (the same as the app's sound button)
        val soundIcon = when (e?.soundMode ?: 0) { 0 -> R.drawable.ic_car_sound_on; 1 -> R.drawable.ic_car_sound_alerts; else -> R.drawable.ic_car_sound_off }
        val sound = Action.Builder().setIcon(icon(soundIcon)).setOnClickListener { e?.cycleSound(); invalidate() }.build()
        val demoOn = e != null && (e.prefs.b(PreferenceKeys.DEMO, false) || e.simMode != 0)
        val fr = e?.faster
        if (guiding && fr != null) {
            // a faster route turned up: switch or keep (also on the map card, for cars that pass taps on)
            strip.addAction(Action.Builder().setTitle("Wechseln −${fr.savedMin} Min.").setOnClickListener { e?.acceptFaster(); invalidate() }.build())
            strip.addAction(Action.Builder().setTitle("Beibehalten").setOnClickListener { e?.dismissFaster(); invalidate() }.build())
            strip.addAction(sound)
        } else if (guiding && e != null && e.simMode != 0) {
            // a demo drive: end it, the speed (tap to switch 1x, 2x, 4x), skip to the next turn, and Settings
            strip.addAction(Action.Builder().setTitle(Lang.t("Demo beenden", "End demo")).setOnClickListener { e.stopSim(); invalidate() }.build())
            strip.addAction(Action.Builder().setTitle("${e.simMode}×").setOnClickListener { e.cycleSim(); invalidate() }.build())
            strip.addAction(Action.Builder().setTitle(Lang.t("Abbiegung", "Skip turn")).setOnClickListener { e.skipSimTurn(); invalidate() }.build())
            strip.addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_settings)).setOnClickListener { openSettings() }.build())
        } else if (guiding) {
            // (the app's own column on the left has Beenden, Melden, the filter, zoom and re-centre: nothing else up here.
            // A bar can't be empty, so 2D / 3D stays as the one button)
            // the demo drive (for trying it out parked; only with Demo switched on in Settings, as in the app): starts it, speeds it
            // up, stops it. Otherwise that space is the 2D / 3D button
            // the trip's buttons (end, charger filter, sound) are the app's own, on the left of the map and always there. Android Auto's
            // bar can't be empty, though: its library refuses a strip without a button (IllegalStateException), which closed the app as
            // soon as a trip started. Melden stays there, as before. Cars that don't pass taps on can't use the buttons on the map, so
            // there Beenden and the sound are in the bar as well
            if (taps) {
                // the bar can't be empty: the sound is its one button. Beenden is the first of the app's own column (Android Auto puts
                // its bar button in the middle of the strip, off the column's line). Settings is the gear at the bottom left
                strip.addAction(sound)
            } else {
                strip.addAction(Action.Builder().setTitle("Beenden").setOnClickListener { e?.endRoute(); invalidate() }.build())
                strip.addAction(reportAction())
                strip.addAction(sound)
            }

        } else if (taps) {
            // a trip that ended by itself on the way: Carry on, also in Android Auto's own row (for rotary knobs)
            if (e?.resume != null) strip.addAction(Action.Builder().setTitle(Lang.t("Weiter", "Carry on")).setOnClickListener { e.resumeTrip(startNow = true); invalidate() }.build())
            // cars that pass taps on use the app's own search bar, chips and button column on the map (Melden, zoom and re-centre are
            // there). The bar can't be empty: Settings, which the column doesn't have, stays as its one button
            // (with the search bar left off the map, Places (★) comes up here too: Search, Home, Work and the saved places)
            if (e?.prefs?.b(PreferenceKeys.CAR_HIDE_SEARCH, false) == true)
                strip.addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_star)).setOnClickListener { screenManager.push(PlacesScreen(carContext, holder)) }.build())
            strip.addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_settings)).setOnClickListener { openSettings() }.build())
        } else {
            if (e?.resume != null) strip.addAction(Action.Builder().setTitle(Lang.t("Weiter", "Carry on")).setOnClickListener { e.resumeTrip(startNow = true); invalidate() }.build())
            // cars that don't pass taps on: Melden has the Search button's place, and Suche is the first row in Orte
            strip.addAction(reportAction())
            strip.addAction(if (e?.carShort == true) Action.Builder().setIcon(icon(R.drawable.ic_car_star)).setOnClickListener { screenManager.push(PlacesScreen(carContext, holder)) }.build()
                else Action.Builder().setTitle(Lang.t("Orte", "Places")).setOnClickListener { screenManager.push(PlacesScreen(carContext, holder)) }.build())
            strip.addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_settings)).setOnClickListener { openSettings() }.build())
        }
        val mapStrip = ActionStrip.Builder()
            .addAction(Action.PAN)
            .addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_plus)).setOnClickListener { e?.zoom(0.7) }.build())
            .addAction(Action.Builder().setIcon(icon(R.drawable.ic_car_minus)).setOnClickListener { e?.zoom(-0.7) }.build())
            // (Android Auto allows four buttons here: while the map follows the car, Zentrieren isn't needed and the layers take its place)
            .addAction(if (e == null || e.following) Action.Builder().setIcon(icon(R.drawable.ic_car_layers)).setOnClickListener { screenManager.push(LayersScreen(carContext, holder)) }.build()
                else Action.Builder().setIcon(icon(R.drawable.ic_car_recenter)).setOnClickListener { e.recenter(byUser = true) }.build())
            .build()
        val b = NavigationTemplate.Builder().setActionStrip(strip.build())
        // cars that pass taps on: the app's own column on the left has zoom, settings and re-centre, so Android Auto's map buttons on
        // the right (zoom and layers) are left off. Cars without taps keep them: there they are the only zoom
        if (!taps) b.setMapActionStrip(mapStrip)
        // the turn card, trip figures and the "working out the route" spinner are the app's own, drawn on the map (see CarOverlay):
        // Android Auto's own loading card took the top-left corner and pushed the app's cards aside. The turns still go to Android
        // Auto through updateTrip, for the car's dashboard display and notifications.
        return b.build()
    }

    private fun trip(e: NavEngine, t: TurnInfo, eta: com.headunit.launcher.EtaInfo): Trip {
        val est = estimate(eta.dist, eta.secs, e.imperial)
        val tb = Trip.Builder().addStep(step(e, t, carContext), estimate(t.dist, 0.0, e.imperial))
        val d = e.dest
        tb.addDestination(Destination.Builder().setName(d?.name ?: Lang.t("Ziel", "Destination")).setAddress(d?.sub ?: "").build(), est)
        if (e.roadNow.isNotBlank()) tb.setCurrentRoad(e.roadNow)
        return tb.build()
    }

    companion object {
        fun distance(m: Double, imperial: Boolean): Distance = if (imperial) {
            val mi = m / 1609.344
            if (mi >= 0.25) Distance.create(Math.round(mi * 10) / 10.0, Distance.UNIT_MILES)
            else Distance.create((Math.round(m * 1.09361 / 10.0) * 10).toDouble(), Distance.UNIT_YARDS)
        } else {
            if (m >= 1000) Distance.create(Math.round(m / 100.0) / 10.0, Distance.UNIT_KILOMETERS)
            else Distance.create((Math.round(m / 10.0) * 10).toDouble(), Distance.UNIT_METERS)
        }

        fun estimate(m: Double, secs: Double, imperial: Boolean): TravelEstimate {
            val arrive = System.currentTimeMillis() + (secs * 1000).toLong()
            return TravelEstimate.Builder(distance(m, imperial), DateTimeWithZone.create(arrive, TimeZone.getDefault()))
                .setRemainingTimeSeconds(secs.toLong().coerceAtLeast(0)).build()
        }

        /** The engine's turn as Android Auto's step: the kind of turn (for its arrow), what to do, and the road. */
        /** The arrow picture for each kind of turn (Android Auto only draws the turn card when the step has one). */
        private fun maneuverIcon(type: Int): Int = when (type) {
            Maneuver.TYPE_TURN_NORMAL_LEFT -> R.drawable.ic_man_turn_left
            Maneuver.TYPE_TURN_NORMAL_RIGHT -> R.drawable.ic_man_turn_right
            Maneuver.TYPE_TURN_SHARP_LEFT -> R.drawable.ic_man_sharp_left
            Maneuver.TYPE_TURN_SHARP_RIGHT -> R.drawable.ic_man_sharp_right
            Maneuver.TYPE_TURN_SLIGHT_LEFT, Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT -> R.drawable.ic_man_slight_left
            Maneuver.TYPE_TURN_SLIGHT_RIGHT, Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT -> R.drawable.ic_man_slight_right
            Maneuver.TYPE_KEEP_LEFT -> R.drawable.ic_man_keep_left
            Maneuver.TYPE_KEEP_RIGHT -> R.drawable.ic_man_keep_right
            Maneuver.TYPE_U_TURN_LEFT -> R.drawable.ic_man_uturn_left
            Maneuver.TYPE_U_TURN_RIGHT -> R.drawable.ic_man_uturn_right
            Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW, Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE, Maneuver.TYPE_ROUNDABOUT_ENTER_CW -> R.drawable.ic_man_roundabout_cw
            Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW, Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE, Maneuver.TYPE_ROUNDABOUT_ENTER_CCW -> R.drawable.ic_man_roundabout_ccw
            Maneuver.TYPE_DESTINATION, Maneuver.TYPE_DESTINATION_LEFT, Maneuver.TYPE_DESTINATION_RIGHT -> R.drawable.ic_man_destination
            else -> R.drawable.ic_man_straight
        }

        /** The last lanes picture (the same junction is sent many times on the approach). */
        private var lanesCache: Pair<String, android.graphics.Bitmap>? = null

        private fun laneHit(dir: String, maneuver: String): Boolean {
            val m = maneuver.uppercase()
            return when {
                m.contains("U_TURN") || m.contains("UTURN") -> dir.contains("U_TURN")
                m.contains("LEFT") -> dir.contains("LEFT") && !dir.contains("U_TURN")
                m.contains("RIGHT") -> dir.contains("RIGHT") && !dir.contains("U_TURN")
                else -> dir == "STRAIGHT"
            }
        }

        private fun laneShape(d: String): Int {
            val left = d.contains("LEFT")
            return when {
                d.contains("U_TURN") -> if (left) LaneDirection.SHAPE_U_TURN_LEFT else LaneDirection.SHAPE_U_TURN_RIGHT
                d.contains("SHARP") -> if (left) LaneDirection.SHAPE_SHARP_LEFT else LaneDirection.SHAPE_SHARP_RIGHT
                d.contains("SLIGHT") || d.contains("BEAR") -> if (left) LaneDirection.SHAPE_SLIGHT_LEFT else LaneDirection.SHAPE_SLIGHT_RIGHT
                left -> LaneDirection.SHAPE_NORMAL_LEFT
                d.contains("RIGHT") -> LaneDirection.SHAPE_NORMAL_RIGHT
                d == "STRAIGHT" -> LaneDirection.SHAPE_STRAIGHT
                else -> LaneDirection.SHAPE_UNKNOWN
            }
        }

        /** The lanes drawn side by side: white arrows for the lanes to use, grey for the rest. */
        private fun lanesPicture(lanes: List<List<Pair<String, Boolean>>>, scale: Float = 1f): android.graphics.Bitmap {
            val full = lanesPictureFull(lanes)
            if (scale >= 0.999f) return full
            return android.graphics.Bitmap.createScaledBitmap(full, (full.width * scale).toInt().coerceAtLeast(1), (full.height * scale).toInt().coerceAtLeast(1), true)
        }

        private fun lanesPictureFull(lanes: List<List<Pair<String, Boolean>>>): android.graphics.Bitmap {
            val lw = 72; val h = 96
            val bmp = android.graphics.Bitmap.createBitmap(lw * lanes.size, h, android.graphics.Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 7f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
            val head = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            val sep = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66FFFFFF; strokeWidth = 3f; pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 8f), 0f) }
            for ((i, dirs) in lanes.withIndex()) {
                val cx = lw * i + lw / 2f
                if (i > 0) c.drawLine(lw * i.toFloat(), 8f, lw * i.toFloat(), h - 8f, sep)
                // the lane's other arrows first, then the ones to use on top
                for ((d, ok) in dirs.sortedBy { it.second }) {
                    val col = if (ok) Color.WHITE else 0x80FFFFFF.toInt()
                    line.color = col; head.color = col
                    val ang = when {
                        d.contains("U_TURN") -> 180.0
                        d.contains("SHARP") -> 135.0
                        d.contains("SLIGHT") || d.contains("BEAR") -> 45.0
                        d.contains("LEFT") || d.contains("RIGHT") -> 90.0
                        else -> 0.0
                    } * (if (d.contains("LEFT")) -1 else 1)
                    val baseY = h - 10f; val midY = h * 0.52f
                    val p = android.graphics.Path(); p.moveTo(cx, baseY)
                    val tipX: Float; val tipY: Float; val dirA: Double
                    if (Math.abs(ang) >= 179.0) {
                        // a U-turn: up, over and back down
                        val side = if (ang < 0) -1f else 1f
                        p.lineTo(cx, h * 0.34f)
                        p.quadTo(cx, 12f, cx + side * 14f, 12f)
                        p.quadTo(cx + side * 26f, 12f, cx + side * 26f, h * 0.34f)
                        p.lineTo(cx + side * 26f, h * 0.48f)
                        tipX = cx + side * 26f; tipY = h * 0.48f + 4f; dirA = 180.0
                    } else {
                        p.lineTo(cx, midY)
                        val len = if (ang == 0.0) midY - 16f else 26f
                        val r = Math.toRadians(ang)
                        tipX = cx + (Math.sin(r) * len).toFloat(); tipY = midY - (Math.cos(r) * len).toFloat()
                        p.lineTo(tipX, tipY); dirA = ang
                    }
                    c.drawPath(p, line)
                    // the arrowhead, pointing the way the arrow goes
                    val r = Math.toRadians(dirA)
                    val fx = Math.sin(r).toFloat(); val fy = -Math.cos(r).toFloat()
                    val px = -fy; val py = fx
                    val hp = android.graphics.Path()
                    hp.moveTo(tipX + fx * 12f, tipY + fy * 12f)
                    hp.lineTo(tipX - fx * 4f + px * 12f, tipY - fy * 4f + py * 12f)
                    hp.lineTo(tipX - fx * 4f - px * 12f, tipY - fy * 4f - py * 12f)
                    hp.close()
                    c.drawPath(hp, head)
                }
            }
            return bmp
        }

        fun step(e: NavEngine, t: TurnInfo, ctx: CarContext): Step {
            val left = e.leftTraffic
            val m = t.maneuver.uppercase()
            val type = when {
                // always the enter-and-exit type: the car shows the roundabout only with an exit number, and a plain "enter" type shows as a turn
                // (the "with angle" kind: head-up displays and dashboards draw the exit from its angle, so straight on shows as straight on
                // and not as a turn)
                m.startsWith("ROUNDABOUT") -> if (left) Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE else Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE
                m == "TURN_LEFT" -> Maneuver.TYPE_TURN_NORMAL_LEFT
                m == "TURN_RIGHT" -> Maneuver.TYPE_TURN_NORMAL_RIGHT
                m == "SHARP_LEFT" -> Maneuver.TYPE_TURN_SHARP_LEFT
                m == "SHARP_RIGHT" -> Maneuver.TYPE_TURN_SHARP_RIGHT
                m == "BEAR_LEFT" -> Maneuver.TYPE_TURN_SLIGHT_LEFT
                m == "BEAR_RIGHT" -> Maneuver.TYPE_TURN_SLIGHT_RIGHT
                m == "KEEP_LEFT" -> Maneuver.TYPE_KEEP_LEFT
                m == "KEEP_RIGHT" -> Maneuver.TYPE_KEEP_RIGHT
                m.contains("UTURN") || m.contains("U_TURN") -> if (left) Maneuver.TYPE_U_TURN_RIGHT else Maneuver.TYPE_U_TURN_LEFT
                m.startsWith("MOTORWAY_EXIT_LEFT") -> Maneuver.TYPE_OFF_RAMP_NORMAL_LEFT
                m.startsWith("MOTORWAY_EXIT_RIGHT") -> Maneuver.TYPE_OFF_RAMP_NORMAL_RIGHT
                m == "ARRIVE_LEFT" -> Maneuver.TYPE_DESTINATION_LEFT
                m == "ARRIVE_RIGHT" -> Maneuver.TYPE_DESTINATION_RIGHT
                m.startsWith("ARRIVE") -> Maneuver.TYPE_DESTINATION
                m == "DEPART" -> Maneuver.TYPE_DEPART
                m.contains("FERRY") -> Maneuver.TYPE_FERRY_BOAT
                m == "STRAIGHT" || m == "FOLLOW" -> Maneuver.TYPE_STRAIGHT
                else -> Maneuver.TYPE_UNKNOWN
            }
            // (back to the plain icon for each kind of turn: the picture made the turn disappear on some dashboards, e.g. VW's)
            val pic: android.graphics.Bitmap? = null
            val mb = Maneuver.Builder(type).setIcon(CarIcon.Builder(if (pic != null) IconCompat.createWithBitmap(pic) else IconCompat.createWithResource(ctx, maneuverIcon(type))).build())
            val isRb = type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CW_WITH_ANGLE || type == Maneuver.TYPE_ROUNDABOUT_ENTER_AND_EXIT_CCW_WITH_ANGLE
            var rbExit = t.exit
            if (isRb) {
                // the exit angle, from the way you come in to the way you leave, counted in the direction round the roundabout:
                // 90 = first exit on the near side, 180 = straight on, 270 = far side, 360 = back the way you came
                var ang = -1
                if (t.rbIn >= 0 && t.rbOut >= 0) {
                    // (the way in and out come from the route line, as on the map: see NavEngine)
                    val rel = ((t.rbOut - t.rbIn + 540.0) % 360.0) - 180.0   // -180..180, right turn positive
                    val a = Math.round(if (left) 180.0 + rel else 180.0 - rel).toInt()
                    ang = ((a % 360) + 360) % 360
                } else {
                    ang = when (m) {
                        "ROUNDABOUT_RIGHT" -> if (left) 270 else 90
                        "ROUNDABOUT_LEFT" -> if (left) 90 else 270
                        "ROUNDABOUT_BACK" -> 360
                        else -> 180
                    }
                }
                if (ang == 0) ang = 360
                mb.setRoundaboutExitAngle(ang.coerceIn(1, 360))
                // no exit number from the route data: counted from the angle (about a quarter turn per exit) rather than left out,
                // which Android Auto refuses
                if (rbExit <= 0) rbExit = (Math.round(ang / 90.0).toInt()).coerceIn(1, 4)
                mb.setRoundaboutExitNumber(rbExit)
            }
            val what = when {
                isRb ->
                    if (t.exit > 0) Lang.t("Ausfahrt ${t.exit} nehmen", "Take exit ${t.exit}") else when (m) {
                        "ROUNDABOUT_LEFT" -> Lang.t("Im Kreisverkehr links", "Left at the roundabout")
                        "ROUNDABOUT_RIGHT" -> Lang.t("Im Kreisverkehr rechts", "Right at the roundabout")
                        "ROUNDABOUT_CROSS", "ROUNDABOUT_STRAIGHT" -> Lang.t("Im Kreisverkehr geradeaus", "Straight on at the roundabout")
                        else -> Lang.t("In den Kreisverkehr", "Enter the roundabout")
                    }
                m.startsWith("ARRIVE") -> Lang.t("Ankunft", "Arrive")
                // a motorway junction by name: "Frankfurter Kreuz", "Ausfahrt 17 Weiterstadt"
                t.junction.isNotBlank() && !m.startsWith("ROUNDABOUT") -> t.junction
                t.tag.isNotBlank() -> t.tag
                else -> m.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
            }
            val cue = if (t.street.isNotBlank()) "$what · ${t.street}" else what
            val sb = Step.Builder(cue).setManeuver(mb.build())
            if (t.street.isNotBlank()) sb.setRoad(t.street)
            // which lanes to be in, for the car's own displays (the dashboard behind the wheel or a head-up display, where the car has
            // one): each lane's arrows with the ones to use marked, plus the picture of them that Android Auto needs alongside
            try {
                val sec = t.lanes
                if (sec != null && sec.lanes.size in 1..10) {
                    val anyFollow = sec.lanes.any { it.follow != null }
                    val recs = ArrayList<List<Pair<String, Boolean>>>()
                    for (ln in sec.lanes) {
                        val dirs = ln.dirs.ifEmpty { listOf("STRAIGHT") }
                        val r = dirs.map { d -> d to (if (anyFollow) d == ln.follow else laneHit(d, t.maneuver)) }
                        recs += r
                        val lb = androidx.car.app.navigation.model.Lane.Builder()
                        for ((d, ok) in r) lb.addDirection(LaneDirection.create(laneShape(d), ok))
                        sb.addLane(lb.build())
                    }
                    // Android Auto puts this picture under the turn card, and the app's trip card may cover the space below it: the
                    // picture shrinks to the room left by the trip card, and is left out when even a small one wouldn't fit (the
                    // lanes still show in the turn card itself)
                    val room = ((1.0 - (e?.bottomCover ?: 0.0) - 0.45) / 0.35).coerceIn(0.0, 1.0)
                    if (room >= 0.4) {
                        val key = recs.toString() + "@" + (room * 10).toInt()
                        val pic = lanesCache?.takeIf { it.first == key }?.second ?: lanesPicture(recs, room.toFloat()).also { lanesCache = key to it }
                        sb.setLanesImage(CarIcon.Builder(IconCompat.createWithBitmap(pic)).build())
                    }
                }
            } catch (x: Exception) { softFail(x) }
            return sb.build()
        }
    }
}
