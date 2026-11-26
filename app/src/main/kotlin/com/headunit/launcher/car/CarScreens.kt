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

/** A coloured icon for a list row. */
private fun rowIcon(ctx: CarContext, res: Int, color: Int): CarIcon =
    CarIcon.Builder(IconCompat.createWithResource(ctx, res)).setTint(androidx.car.app.model.CarColor.createCustom(color, color)).build()

/** "1.2 mi" from where you are (or nothing when the position isn't known yet). */
private fun distFrom(e: NavEngine?, p: Place): String {
    if (e == null) return ""
    val here = e.fix?.pos ?: return ""
    return com.headunit.launcher.Geo.fmtDist(com.headunit.launcher.Geo.dist(here, p.pos), e.imperial)
}

/** Home, Work, your saved places, favourites and recent destinations: tap one to go. */
class PlacesScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val e = holder.engine
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val list = ItemList.Builder().setNoItemsMessage(if (e == null) Lang.t("Karte wird gestartet…", "Starting the map…") else Lang.t("Noch keine gespeicherten oder letzten Orte", "No saved or recent places yet"))
        // Suche first: on cars that don't pass taps on to the map, this is the way in to it (the Search button is Melden now)
        var shownRows = 0
        if (e != null) {
            list.addItem(Row.Builder().setTitle(Lang.t("Suche", "Search")).addText(Lang.t("Ziel eingeben oder sagen", "Type or say where to"))
                .setImage(rowIcon(carContext, R.drawable.ic_car_search, 0xFF9CA4BF.toInt()))
                .setOnClickListener { screenManager.push(SearchScreen(carContext, holder)) }.build())
            shownRows = 1
        }
        if (e != null) {
            // (label, place, icon, colour) in the app's colours: Home blue, Work amber, saved teal, favourites gold, recent grey
            val all = ArrayList<Triple<String, Place, Pair<Int, Int>>>()
            e.places.slot("home")?.let { all += Triple(Lang.t("Zuhause", "Home"), it, R.drawable.ic_car_home to 0xFF4C8DFF.toInt()) }
            e.places.slot("work")?.let { all += Triple(Lang.t("Arbeit", "Work"), it, R.drawable.ic_car_work to 0xFFF29900.toInt()) }
            for (c in e.places.customs()) all += Triple(c.label, c.place, R.drawable.ic_car_star to 0xFF12A4B4.toInt())
            for (p in e.places.favourites()) all += Triple(p.name, p, R.drawable.ic_car_star to 0xFFF2C230.toInt())
            for (p in e.places.recents()) all += Triple(p.name, p, R.drawable.ic_car_history to 0xFF9CA4BF.toInt())
            val seen = HashSet<String>()
            for ((label, p, ic) in all) {
                if (!seen.add("%.4f,%.4f".format(p.pos.lat, p.pos.lon))) continue
                if (seen.size + shownRows > limit.coerceIn(1, 12)) break
                val r = Row.Builder().setTitle(label).setImage(rowIcon(carContext, ic.first, ic.second))
                val sub = if (label == p.name) p.sub else p.name
                val d = distFrom(e, p)
                val line = listOf(d, sub).filter { it.isNotBlank() }.joinToString(" · ")
                if (line.isNotBlank()) r.addText(line)
                r.setOnClickListener { go(e, p) }
                list.addItem(r.build())
            }
        }
        return ListTemplate.Builder().setTitle(Lang.t("Orte", "Places")).setHeaderAction(Action.BACK).setSingleList(list.build()).build()
    }

    private fun go(e: NavEngine, p: Place) {
        e.startRoute(p)
        screenManager.popToRoot()
    }
}

/** "Hier melden": Polizei, Unfall, Gefahr, Panne, mobiler Blitzer oder Stau, saved where you are (the same reports as the phone's). */
class ReportScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val kinds = ReportKind.entries.map { it to carIcon(it) }
        val list = ItemList.Builder()
        // a car that shows only 6 tiles: the speed limit takes the place of "Panne" (the least used), so it isn't cut off
        val max = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID) } catch (x: Exception) { 6 }
        val e0 = holder.engine
        val canShare = e0 != null && e0.route != null && !e0.previewing
        val room = max - 1 - (if (canShare) 1 else 0)
        // the least used kinds make room when the car shows few tiles (breakdown first)
        val shownKinds = (if (room >= kinds.size) kinds else kinds.filter { it.first != ReportKind.BREAKDOWN }).take(room.coerceAtLeast(1))
        for ((kind, ic) in shownKinds) list.addItem(
            // (the roadworks sign keeps its own colours: red, white and black, not one flat tint)
            GridItem.Builder().setTitle(kind.label)
                .setImage(if (kind == ReportKind.ROADWORKS) CarIcon.Builder(IconCompat.createWithResource(carContext, ic)).build() else rowIcon(carContext, ic, kind.argb.toInt()), GridItem.IMAGE_TYPE_LARGE)
                .setOnClickListener { send(kind.key) }.build()
        )
        // the speed limit here: the right value, or that the sign shown is right
        list.addItem(GridItem.Builder().setTitle(Lang.t("Tempolimit", "Speed limit")).setImage(rowIcon(carContext, R.drawable.ic_car_speed, 0xFFF28B82.toInt()), GridItem.IMAGE_TYPE_LARGE)
            .setOnClickListener { screenManager.push(LimitReportScreen(carContext, holder)) }.build())
        // on a route: share your arrival time (the link is made here, the share menu opens on the phone), or stop sharing it
        if (canShare && max >= kinds.size + 1) {
            val sharing = e0?.tripShareUrl != null
            list.addItem(GridItem.Builder().setTitle(if (sharing) Lang.t("Teilen beenden", "Stop sharing") else Lang.t("Ankunft teilen", "Share ETA"))
                .setImage(rowIcon(carContext, R.drawable.ic_car_pin, if (sharing) 0xFFF28B82.toInt() else 0xFF81C995.toInt()), GridItem.IMAGE_TYPE_LARGE)
                .setOnClickListener {
                    val e = holder.engine
                    if (e != null) { if (e.tripShareUrl != null) e.stopTripShare() else e.shareTrip(fromCar = true) }
                    screenManager.pop()
                }.build())
        }
        return GridTemplate.Builder().setTitle(Lang.t("Hier melden", "Report here")).setHeaderAction(Action.BACK).setSingleList(list.build()).build()
    }

    /** The car's icon for each kind (drawables, as the car's icons are not the phone's). */
    private fun carIcon(k: ReportKind): Int = when (k) {
        ReportKind.POLICE -> R.drawable.ic_car_police
        ReportKind.ACCIDENT -> R.drawable.ic_car_crash
        ReportKind.HAZARD, ReportKind.CLOSED -> R.drawable.ic_car_report
        ReportKind.BREAKDOWN -> R.drawable.ic_car_breakdown
        ReportKind.MOBILECAM -> R.drawable.ic_car_mobilecam
        ReportKind.JAM -> R.drawable.ic_car_jam
        ReportKind.ROADWORKS -> R.drawable.ic_roadworks_sign
    }

    private fun send(kind: String) {
        val e = holder.engine
        val msg = when {
            e == null -> Lang.t("Karte wird noch gestartet", "The map is still starting")
            e.fix == null -> Lang.t("Warte auf GPS", "Waiting for GPS")
            else -> null
        }
        if (msg != null || e == null) {
            try { androidx.car.app.CarToast.makeText(carContext, msg ?: "", androidx.car.app.CarToast.LENGTH_SHORT).show() } catch (x: Exception) { softFail(x) }
            return
        }
        try { e.report(kind) } catch (x: Exception) { softFail(x) }
        try { androidx.car.app.CarToast.makeText(carContext, Lang.t("${ReportKind.orPolice(kind).label}-Meldung hier gespeichert", "${ReportKind.orPolice(kind).label} report saved here"), androidx.car.app.CarToast.LENGTH_SHORT).show() } catch (x: Exception) { softFail(x) }
        screenManager.pop()
    }
}

/** Ebenen: what the car map shows, as switches (the same settings as on the phone). */
class LayersScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    private val prefs = Prefs(carContext.applicationContext)

    private fun row(title: String, key: String, def: Boolean): Row = Row.Builder().setTitle(title)
        .setToggle(Toggle.Builder { on -> prefs.put(key, on); holder.engine?.applyLayer(key); invalidate() }.setChecked(prefs.b(key, def)).build()).build()

    override fun onGetTemplate(): Template {
        val e = holder.engine
        // most important first: a car that shows fewer rows loses the last ones
        val rows = listOf(
            row(Lang.t("Verkehr", "Traffic"), PreferenceKeys.TRAFFIC, true),
            row(Lang.t("Satellit", "Satellite"), PreferenceKeys.SATELLITE, false),
            row(Lang.t("Blitzer", "Speed cameras"), PreferenceKeys.CAMERA_MAP, true),
            row(Lang.t("Meldungen & Baustellen", "Reports & roadworks"), PreferenceKeys.HAZARD_ICONS, true),
            row(Lang.t("Parkhäuser", "Car parks"), PreferenceKeys.GARAGES_MAP, true),
            row(Lang.t("Regenradar", "Rain radar"), PreferenceKeys.RAIN_RADAR, false),
            row(Lang.t("Verkehr nur auf der Route", "Traffic on route only"), PreferenceKeys.ROUTETRAFFICONLY, false),
        )
        val max = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val list = ItemList.Builder()
        for (r in rows.take(max.coerceAtLeast(1))) list.addItem(r)
        return ListTemplate.Builder().setTitle(Lang.t("Ebenen", "Layers")).setHeaderAction(Action.BACK).setSingleList(list.build()).build()
    }
}

/** "Tempolimit melden": the limit where you are (or that the one shown is right), shared with other drivers. */
class LimitReportScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val e = holder.engine
        val shown = e?.speedLimit?.let { Math.round(it * 3.6).toInt() } ?: if (e?.noLimit == true) 0 else null
        val items = ArrayList<Pair<String, Int?>>()
        if (shown != null) items += Lang.t("Stimmt (${if (shown == 0) "frei" else shown})", "Right (${if (shown == 0) "none" else shown})") to null
        // ascending, so the speeds read in order on the car screen
        for (v in listOf(30, 50, 60, 70, 80, 100, 120)) if (v != shown) items += "$v" to v
        if (shown != 0) items += Lang.t("Kein Limit", "No limit") to 0
        val max = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_GRID) } catch (x: Exception) { 6 }
        val list = ItemList.Builder()
        for ((label, v) in items.take(max.coerceAtLeast(1))) list.addItem(
            GridItem.Builder().setTitle(label).setImage(rowIcon(carContext, R.drawable.ic_car_speed, if (v == null) 0xFF81C995.toInt() else 0xFFF28B82.toInt()), GridItem.IMAGE_TYPE_LARGE)
                .setOnClickListener {
                    val en = holder.engine
                    if (en == null || en.fix == null) {
                        try { androidx.car.app.CarToast.makeText(carContext, Lang.t("Warte auf GPS", "Waiting for GPS"), androidx.car.app.CarToast.LENGTH_SHORT).show() } catch (x: Exception) { softFail(x) }
                    } else {
                        try { en.reportLimit(v) } catch (x: Exception) { softFail(x) }
                        try { androidx.car.app.CarToast.makeText(carContext, Lang.t("Tempolimit gemeldet", "Speed limit reported"), androidx.car.app.CarToast.LENGTH_SHORT).show() } catch (x: Exception) { softFail(x) }
                        screenManager.popToRoot()
                    }
                }.build()
        )
        return GridTemplate.Builder().setTitle(Lang.t("Welches Tempolimit gilt hier?", "Which limit applies here?")).setHeaderAction(Action.BACK).setSingleList(list.build()).build()
    }
}

/** Search with the car's keyboard or voice: results from the same search as the app (TomTom, or offline when there's no signal). */
class SearchScreen(carContext: CarContext, private val holder: CarHolder, initial: String = "", private val voice: Boolean = false) : Screen(carContext) {
    private var query = initial
    private var shown: List<Place> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    /** The results from before a spoken search, so its own answer is known when it comes. */
    private val before: List<Place>? = holder.engine?.results
    private var decided = !voice
    private val poll = object : Runnable {
        override fun run() {
            val r = holder.engine?.results ?: emptyList()
            if (r !== shown) {
                shown = r
                // a spoken destination: one result, or one whose name is exactly what was said, starts the route straight away
                if (!decided && r !== before && r.isNotEmpty()) {
                    decided = true
                    val exact = r.filter { it.name.trim().equals(query.trim(), true) }
                    val pick = if (r.size == 1) r[0] else if (exact.size == 1 && r[0] === exact[0]) r[0] else null
                    if (pick != null) {
                        // driving: the place is asked about (as a stop, or as the new destination), not taken over at once
                        // (Set home / Set work: it is saved instead)
                        holder.engine?.let { en -> if (en.pendingSave != null) en.pickPlace(pick) else en.askNewDest(pick) }
                        screenManager.popToRoot(); return
                    }
                }
                invalidate()
            }
            handler.postDelayed(this, 500)
        }
    }

    private val typed = Runnable { if (query.trim().length >= 2) searchText(query) }

    /** A search text: "Zwischenziel …" (typed or spoken on the car's keyboard) searches for a stop, the destination stays. */
    private fun searchText(text: String) {
        val e = holder.engine ?: return
        val stop = com.headunit.launcher.stopPlace(text.trim().lowercase(java.util.Locale.GERMAN))
        if (stop != null) { e.stopMode = true; e.search(stop) } else e.search(text)
    }

    init {
        if (initial.length >= 2) searchText(initial)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { handler.post(poll) }
            override fun onStop(owner: LifecycleOwner) { handler.removeCallbacks(poll); handler.removeCallbacks(typed) }
            // left without picking a place: Set home / Set work is off again, so a later search doesn't save over it
            override fun onDestroy(owner: LifecycleOwner) { holder.engine?.pendingSave = null }
        })
    }

    override fun onGetTemplate(): Template {
        val e = holder.engine
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val list = ItemList.Builder().setNoItemsMessage(if (query.trim().length < 2) Lang.t("Ort, Adresse oder Postleitzahl eingeben, oder sag „Hey Google, navigiere mit Horizon Nav zu …“", "Type a place, address or postcode, or say \"Hey Google, navigate to … with Horizon Nav\"") else Lang.t("Suche läuft…", "Searching…"))
        if (query.trim().length >= 2 && e != null && e.route != null) {
            // adding a stop: the matches along the rest of the route, sorted by detour
            list.addItem(Row.Builder().setTitle(Lang.t("Entlang der Route suchen", "See them along your route"))
                .setOnClickListener { screenManager.push(AlongScreen(carContext, holder, query.trim())) }.build())
        }
        if (query.trim().length >= 2) {
            // a shop or chain with several branches nearby: "Alle N Standorte" as the first row
            val chainOf = shown.firstOrNull { e != null && e.chainCount(it) >= 2 }
            var n = limit.coerceIn(1, 12)
            if (chainOf != null && e != null) {
                val cnt = e.chainCount(chainOf)
                list.addItem(Row.Builder().setTitle(Lang.t("Alle $cnt Standorte: ${chainOf.name}", "See all $cnt locations: ${chainOf.name}"))
                    .setImage(rowIcon(carContext, R.drawable.ic_car_pin, 0xFF1A73E8.toInt()))
                    .addText(Lang.t("Nach Fahrzeit mit heutigem Verkehr", "By drive time with today's traffic"))
                    .setBrowsable(true)
                    .setOnClickListener { e.showAllLocations(chainOf); screenManager.push(ChainScreen(carContext, holder)) }.build())
                n--
            }
            for (p in shown.take(n)) {
                // a red pin (or the shop's logo), the name, and how far it is with the address under it (as in the app's results)
                val r = Row.Builder().setTitle(p.name).setImage(placeIcon(carContext, p))
                val line = listOf(distFrom(e, p), p.sub).filter { it.isNotBlank() }.joinToString(" · ")
                if (line.isNotBlank()) r.addText(line)
                p.hours?.let { r.addText(it) }
                r.setOnClickListener {
                    // Set home / Set work (from the tile): the place is saved there, no route
                    if (e?.pendingSave != null) e.pickPlace(p) else e?.askNewDest(p)
                    screenManager.popToRoot()
                }
                list.addItem(r.build())
            }
        }
        return SearchTemplate.Builder(object : SearchTemplate.SearchCallback {
            // searched once typing pauses (as in the app), not on every letter: each search is a request on your TomTom key
            override fun onSearchTextChanged(searchText: String) {
                query = searchText
                handler.removeCallbacks(typed)
                if (searchText.trim().length >= 2) handler.postDelayed(typed, 500)
            }
            override fun onSearchSubmitted(searchText: String) { query = searchText; handler.removeCallbacks(typed); e?.search(searchText) }
        }).setHeaderAction(Action.BACK).setSearchHint(Lang.t("Ort suchen", "Search for a place")).setInitialSearchText(query)
            // the keyboard opens at once: Android Auto puts the microphone in it, one tap starts voice input
            // (the mic next to "Wohin?" only opens this page, Android Auto gives an app no way to start speech itself)
            .setShowKeyboardByDefault(true).setItemList(list.build()).build()
    }
}

/** The shop's logo when it is on the unit, else the red pin. */
private fun placeIcon(ctx: CarContext, p: Place): CarIcon {
    val logo = com.headunit.launcher.BrandLogos.domainFor(p)?.let { com.headunit.launcher.BrandLogos.cached(ctx, it) }
    return if (logo != null) CarIcon.Builder(IconCompat.createWithBitmap(logo)).build() else rowIcon(ctx, R.drawable.ic_car_pin, 0xFFEA4335.toInt())
}

/** Every branch of a shop or chain, quickest first (drive time with today's traffic). On a route a tap adds it as a stop. */
class ChainScreen(carContext: CarContext, private val holder: CarHolder) : Screen(carContext) {
    private val handler = Handler(Looper.getMainLooper())
    private var sig = ""
    private val poll = object : Runnable {
        override fun run() {
            val e = holder.engine
            val s = "${e?.chainBusy}|${e?.chain?.locs?.size}|${e?.chain?.locs?.count { it.secs != null }}|${e?.logoRev}"
            if (s != sig) { sig = s; invalidate() }
            handler.postDelayed(this, 500)
        }
    }

    init {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { handler.post(poll) }
            override fun onStop(owner: LifecycleOwner) { handler.removeCallbacks(poll) }
            override fun onDestroy(owner: LifecycleOwner) { holder.engine?.closeChain() }
        })
    }

    override fun onGetTemplate(): Template {
        val e = holder.engine
        val b = ListTemplate.Builder().setTitle(e?.chain?.name ?: Lang.t("Standorte", "Locations")).setHeaderAction(Action.BACK)
        if (e == null || (e.chainBusy && e.chain == null)) return b.setLoading(true).build()
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val list = ItemList.Builder().setNoItemsMessage(Lang.t("Keine Standorte gefunden", "No locations found"))
        val locs = e.chainSorted("quick")
        for (l in locs.take(limit.coerceIn(1, 12))) {
            val r = Row.Builder().setTitle(l.place.name).setImage(placeIcon(carContext, l.place))
            r.addText(listOfNotNull(l.secs?.let { com.headunit.launcher.Geo.fmtDur(it) }, com.headunit.launcher.Geo.fmtDist(l.dist, e.imperial)).joinToString(" · "))
            if (l.place.sub.isNotBlank()) r.addText(l.place.sub)
            r.setOnClickListener {
                val p = l.place
                e.closeChain()
                if (e.route != null && !e.previewing) e.addStop(p) else e.startRoute(p)
                screenManager.popToRoot()
            }
            list.addItem(r.build())
        }
        return b.setSingleList(list.build()).build()
    }
}

/** Fuel, charging, food, coffee or parking near you (or along the route): the same search as the app's chips. Tap one to go. */
class NearbyScreen(carContext: CarContext, private val holder: CarHolder, private val kind: String) : Screen(carContext) {
    private val handler = Handler(Looper.getMainLooper())
    private var shown: List<com.headunit.launcher.Nearby>? = null
    private var busyShown = true
    private val poll = object : Runnable {
        override fun run() {
            val e = holder.engine
            val r = e?.nearby ?: emptyList()
            val b = e?.nearbyBusy ?: false
            if (r !== shown || b != busyShown) { shown = r; busyShown = b; invalidate() }
            handler.postDelayed(this, 500)
        }
    }

    init {
        holder.engine?.loadNearby(kind, false)
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { handler.post(poll) }
            override fun onStop(owner: LifecycleOwner) { handler.removeCallbacks(poll) }
            // the places drawn on the map go with the list (picking one already cleared them)
            override fun onDestroy(owner: LifecycleOwner) { holder.engine?.clearNearby() }
        })
    }

    private val title = when (kind) { "fuel" -> Lang.t("Tanken", "Fuel"); "charging" -> Lang.t("Laden", "Charging"); "food" -> Lang.t("Essen", "Food"); "coffee" -> Lang.t("Kaffee", "Coffee"); "parking" -> Lang.t("Parken", "Parking"); "camp" -> Lang.t("Stellplätze", "Pitches"); "dump" -> Lang.t("Ver- & Entsorgung", "Water & waste"); "lpg" -> Lang.t("Autogas", "LPG"); else -> Lang.t("In der Nähe", "Nearby") }
    private val iconRes = when (kind) { "fuel" -> R.drawable.ic_car_fuel; "charging" -> R.drawable.ic_car_charging; "food" -> R.drawable.ic_car_food; "coffee" -> R.drawable.ic_car_coffee; "parking" -> R.drawable.ic_car_parking; "lpg" -> R.drawable.ic_car_fuel; else -> R.drawable.ic_car_pin }
    private val tint = when (kind) { "fuel" -> 0xFFEA4335.toInt(); "charging" -> 0xFF34A853.toInt(); "food" -> 0xFFF29900.toInt(); "coffee" -> 0xFFA56BFF.toInt(); else -> 0xFF4C8DFF.toInt() }

    override fun onGetTemplate(): Template {
        val e = holder.engine
        val items = holder.engine?.nearby ?: emptyList()
        val busy = holder.engine?.nearbyBusy ?: false
        val b = ListTemplate.Builder().setTitle(title).setHeaderAction(Action.BACK)
        if (busy && items.isEmpty()) return b.setLoading(true).build()
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val list = ItemList.Builder().setNoItemsMessage(Lang.t("Nichts in der Nähe gefunden", "Nothing found nearby"))
        for (n in items.take(limit.coerceIn(1, 12))) {
            // the shop's or brand's logo when it is on the unit (fuel brands, restaurant chains), else the kind's icon
            val logo = com.headunit.launcher.BrandLogos.domainFor(n.place)?.let { com.headunit.launcher.BrandLogos.cached(carContext, it) }
            // a long name goes onto a second line (cut at a space near 24 characters), so names that start alike can be told apart
            val nm = n.place.name
            val cut = if (nm.length > 24) nm.lastIndexOf(' ', 24).takeIf { it > 8 } ?: 24 else nm.length
            val r = Row.Builder().setTitle(nm.take(cut).trim()).also { if (cut < nm.length) it.addText(nm.substring(cut).trim()) }
                .setImage(if (logo != null) CarIcon.Builder(IconCompat.createWithBitmap(logo)).build() else rowIcon(carContext, iconRes, tint))
            val bits = ArrayList<String>()
            bits += com.headunit.launcher.Geo.fmtDist(n.dist, e?.imperial ?: false)
            // the price the local way ("1,789 €" in Germany), as on the phone: it was always shown in pence ("178.9p")
            n.price?.let { bits += com.headunit.launcher.fmtFuelPrice(it, n.unit) }
            if (n.detourMin >= 0) bits += Lang.t("+${n.detourMin} Min.", "+${n.detourMin} min")
            if (n.extra.isNotBlank()) bits += n.extra
            r.addText(bits.joinToString(" · "))
            if (n.place.sub.isNotBlank()) r.addText(n.place.sub)
            r.setOnClickListener {
                val en = holder.engine ?: return@setOnClickListener
                // on a route it becomes a stop (as in the app); otherwise it's where you're going
                if (en.route != null && !en.previewing) en.addStop(n.place) else en.startRoute(n.place)
                screenManager.popToRoot()
            }
            list.addItem(r.build())
        }
        // "Günstigste" at the top: the list by price, and back by distance (only when the stations have prices)
        if (items.any { it.price != null }) {
            val cheap = e?.nearbySort == "cheap"
            b.setActionStrip(ActionStrip.Builder().addAction(Action.Builder()
                .setTitle(if (cheap) Lang.t("Nach Entfernung", "By distance") else Lang.t("Günstigste", "Cheapest"))
                .setOnClickListener { holder.engine?.changeNearbySort(if (cheap) "near" else "cheap"); invalidate() }
                .build()).build())
        }
        return b.setSingleList(list.build()).build()
    }
}

/**
 * Quick settings on the car: voice guidance, day / night, what shows on the map and the route options, without picking up the phone.
 * They're the app's own settings (the phone shows the same).
 */
class CarSettingsScreen(carContext: CarContext, private val holder: CarHolder, private val onTheme: () -> Unit) : Screen(carContext) {
    private val prefs = Prefs(carContext.applicationContext)

    private fun toggleRow(title: String, key: String, def: Boolean, sub: String?, after: (Boolean) -> Unit = {}): Row {
        val r = Row.Builder().setTitle(title)
        if (sub != null) r.addText(sub)
        r.setToggle(Toggle.Builder { on -> prefs.put(key, on); after(on); invalidate() }.setChecked(prefs.b(key, def)).build())
        return r.build()
    }

    override fun onGetTemplate(): Template {
        val e = holder.engine
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        val rows = ArrayList<Row>()
        val sm = e?.soundMode ?: prefs.i(PreferenceKeys.SOUND_MODE, 0)
        rows += Row.Builder().setTitle(Lang.t("Sprachansagen", "Voice guidance"))
            .addText(listOf(Lang.t("Alle Ansagen und Warnungen", "All directions and alerts"), Lang.t("Nur Warnungen (Blitzer, Limits, Gefahren)", "Alerts only (cameras, limits, hazards)"), Lang.t("Aus", "Off"))[sm.coerceIn(0, 2)])
            .setImage(CarIcon.Builder(IconCompat.createWithResource(carContext, when (sm) { 0 -> R.drawable.ic_car_sound_on; 1 -> R.drawable.ic_car_sound_alerts; else -> R.drawable.ic_car_sound_off })).build())
            .setOnClickListener { if (e != null) e.cycleSound() else prefs.put(PreferenceKeys.SOUND_MODE, (sm + 1) % 3); invalidate() }.build()
        // electric: the map's charger filter (all, 50, 150, 300 kW and faster), as in the app; one tap steps on
        if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") {
            val kw = e?.chargerMinKw ?: prefs.i(PreferenceKeys.CHARGER_FILTER_KW, 0)
            rows += Row.Builder().setTitle(Lang.t("Ladesäulen filtern", "Filter charging points"))
                .addText(if (kw <= 0) Lang.t("Alle · tippen zum Wechseln", "All · tap to change") else Lang.t("Ab $kw kW · tippen zum Wechseln", "$kw kW and faster · tap to change"))
                .setOnClickListener {
                    val next = when { kw < 22 -> 22; kw < 50 -> 50; kw < 150 -> 150; else -> 0 }
                    if (e != null) e.pickChargerMinKw(next) else prefs.put(PreferenceKeys.CHARGER_FILTER_KW, next)
                    invalidate()
                }.build()
        }
        // the map's direction in the car: north up, or turning with you (remembered for the car)
        val nu = e?.northUp ?: prefs.b(PreferenceKeys.NORTHUP_CAR, false)
        rows += Row.Builder().setTitle(Lang.t("Kartenrichtung", "Map direction"))
            .addText(if (nu) Lang.t("Nord oben · tippen zum Wechseln", "North up · tap to change") else Lang.t("Mit der Fahrtrichtung · tippen zum Wechseln", "Turning with you · tap to change"))
            .setOnClickListener {
                val want = !nu
                if (e != null) { if (e.northUp != want) e.toggleNorthUp() } else prefs.put(PreferenceKeys.NORTHUP_CAR, want)
                invalidate()
            }.build()
        // the 3D tilt in the car: automatic, or 30 to 65 degrees, one step per tap
        val tiltCar = prefs.i(PreferenceKeys.TILT_CAR, 0).let { if (it in 30..65) it else 0 }
        rows += Row.Builder().setTitle(Lang.t("3D-Neigung im Auto", "3D tilt in the car"))
            .addText(if (tiltCar == 0) Lang.t("Auto · tippen zum Wechseln", "Automatic · tap to change") else Lang.t("$tiltCar° · tippen zum Wechseln", "$tiltCar° · tap to change"))
            .setOnClickListener {
                val levels = listOf(0) + (30..65 step 5).toList()
                val next = levels[(levels.indexOf(tiltCar).takeIf { it >= 0 } ?: 0).let { (it + 1) % levels.size }]
                prefs.put(PreferenceKeys.TILT_CAR, next)
                e?.refreshTilt()
                invalidate()
            }.build()
        val th = prefs.s(PreferenceKeys.CARTHEME, "car")
        rows += Row.Builder().setTitle(Lang.t("Tag und Nacht", "Day and night"))
            .addText(when (th) { "day" -> Lang.t("Immer Tag", "Always day"); "night" -> Lang.t("Immer Nacht", "Always night"); "phone" -> Lang.t("Wie das Handy", "Same as your phone"); else -> Lang.t("Folgt dem Auto", "Follows the car") })
            .setOnClickListener {
                prefs.put(PreferenceKeys.CARTHEME, when (th) { "car" -> "phone"; "phone" -> "day"; "day" -> "night"; else -> "car" })
                onTheme(); invalidate()
            }.build()
        // Android Auto's templated UI does not expose a native continuous slider here,
        // so these map-accessible controls step through sensible values on each tap.
        val uiScale = prefs.i(PreferenceKeys.CARSCALE, 100).coerceIn(40, 300)
        rows += Row.Builder().setTitle(Lang.t("Größe der Kartenoberfläche", "Map UI scale"))
            .addText(Lang.t("${uiScale} % · tippen zum Wechseln von 40 % bis 300 %", "${uiScale}% · tap to cycle from 40% to 300%"))
            .setOnClickListener {
                val levels = listOf(40, 60, 80, 100, 120, 140, 160, 180, 200, 225, 250, 275, 300)
                val next = levels[(levels.indexOf(uiScale).takeIf { it >= 0 } ?: 3).let { (it + 1) % levels.size }]
                prefs.put(PreferenceKeys.CARSCALE, next)
                invalidate()
            }.build()
        val arrowScale = (prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toIntOrNull() ?: 240).coerceIn(100, 500)
        rows += Row.Builder().setTitle(Lang.t("Größe des Positionspfeils", "Locator arrow scale"))
            .addText(Lang.t("${arrowScale} % · tippen zum Vergrößern, nach 300 % zurück", "${arrowScale}% · tap to increase; resets after 300%"))
            .setOnClickListener {
                val next = when (arrowScale) { in 0..149 -> 150; in 150..199 -> 200; in 200..249 -> 250; in 250..299 -> 300; else -> 100 }
                prefs.put(PreferenceKeys.ARROW_PERCENT, next)
                try { holder.map?.setArrowScale((next / 100f).coerceIn(1f, 5f) * 0.9f * (prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 70).coerceIn(30, 180) / 100f)) } catch (e: Exception) { softFail(e) }
                invalidate()
            }.build()
        val reroute: (Boolean) -> Unit = { holder.engine?.rerouteForSettings() }
        rows += toggleRow(Lang.t("Autobahnen meiden", "Avoid motorways"), "avoid_motorway", false, null, reroute)
        rows += toggleRow(Lang.t("Mautstraßen meiden", "Avoid toll roads"), "avoid_toll", false, null, reroute)
        rows += toggleRow(Lang.t("Andere Routen", "Other routes"), PreferenceKeys.LIVE_ALTERNATIVES, true, Lang.t("Auf der Karte anzeigen", "Show on the map")) { _ -> try { holder.engine?.otherRoutesChanged() } catch (x: Exception) { softFail(x) } }
        rows += toggleRow(Lang.t("Blitzer", "Speed cameras"), "cameras", false, Lang.t("Anzeigen und warnen", "Show and warn")) { on ->
            // the sub-line promises both: the cameras on the map go with the warnings (and come back with them)
            prefs.put(PreferenceKeys.CAMERA_MAP, on)
            try { holder.engine?.applyLayer(PreferenceKeys.CAMERA_MAP) } catch (x: Exception) { softFail(x) }
        }
        rows += toggleRow(Lang.t("Ampeln", "Traffic lights"), "signals", true, Lang.t("Auf deiner Route", "On your route")) { on -> try { holder.map?.signalsEnabled(on) } catch (x: Exception) { softFail(x) } }
        rows += toggleRow(Lang.t("Fähren meiden", "Avoid ferries"), "avoid_ferry", false, null, reroute)
        // the same switch as on the phone: the limit sign big where the speedometer was, your speed small beside it
        rows += toggleRow(Lang.t("Großes Limit-Schild", "Big limit sign"), PreferenceKeys.SPEED_SIGN_BIG, false, Lang.t("Am Tacho, Tempo klein daneben", "Where the speedometer was, speed small beside it"))
        // the same switch as on the phone (first in the list: a car with a knob and few rows still shows it)
        rows.add(0, toggleRow(Lang.t("Suchleiste ausblenden", "Hide the search bar"), PreferenceKeys.CAR_HIDE_SEARCH, false, Lang.t("Karte ohne Suchleiste und Orte, Suche über Orte (★)", "Map without search bar and places, search under Places (★)")))
        val list = ItemList.Builder()
        for (r in rows.take(limit.coerceIn(1, 12))) list.addItem(r)
        return ListTemplate.Builder().setTitle(Lang.t("Einstellungen", "Settings")).setHeaderAction(Action.BACK).setSingleList(list.build()).build()
    }
}

/** Adding a stop on Android Auto: the matches along the rest of the route, a list sorted by detour; a tap adds it as a stop. */
class AlongScreen(carContext: CarContext, private val holder: CarHolder, private val q: String) : Screen(carContext) {
    private var started = false

    override fun onGetTemplate(): Template {
        val e = holder.engine
        if (e != null && !started) { started = true; e.searchAlong(q) { invalidate() } }
        val list = ItemList.Builder().setNoItemsMessage(if (e?.alongBusy == true) Lang.t("Suche entlang der Route …", "Searching along the route …") else Lang.t("Nichts entlang der Route gefunden", "Nothing found along the route"))
        val limit = try { carContext.getCarService(ConstraintManager::class.java).getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_LIST) } catch (x: Exception) { 6 }
        for (h in (e?.alongHits ?: emptyList()).take(limit.coerceIn(1, 12))) {
            val r = Row.Builder().setTitle(h.place.name).setImage(placeIcon(carContext, h.place))
            val imp = e?.imperial ?: false
            r.addText(listOf(if (h.extraMin <= 0) Lang.t("ohne Umweg", "no detour") else Lang.t("+${h.extraMin} Min. Umweg", "+${h.extraMin} min detour"),
                Lang.t("in ${com.headunit.launcher.Geo.fmtDist(h.ahead, imp)}", "in ${com.headunit.launcher.Geo.fmtDist(h.ahead, imp)}")).joinToString(" · "))
            r.setOnClickListener { e?.closeAlong(); e?.addStop(h.place); screenManager.popToRoot() }
            list.addItem(r.build())
        }
        return ListTemplate.Builder().setTitle(Lang.t("Entlang der Route: $q", "Along your route: $q")).setHeaderAction(Action.BACK)
            .setSingleList(list.build()).build()
    }
}
