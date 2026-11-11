package com.headunit.launcher.ui

import com.headunit.launcher.R
import com.headunit.launcher.Lang
import androidx.compose.material.icons.rounded.RvHookup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.PropaneTank
import androidx.compose.ui.res.stringResource

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.animateScrollBy
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Place
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.NavEngine
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.locatorSpec

/**
 * The app's own screens on the Android Auto display, drawn over the car's map with the app's designs: alerts across the top, the
 * turn card (top left), hazard badges, speed and the trip card (bottom left), the route choice before setting off, and the arrival
 * summary. [scale] makes them the same size on the car screen as in the app (car screens have few pixels per inch for their size).
 * Nothing here can be tapped (Android Auto only lets its own buttons take touches), so the matching buttons are Android Auto's.
 */
@Composable
fun CarOverlay(engine: NavEngine, area: State<android.graphics.Rect?>, dark: State<Boolean>, scale: State<Float>, actions: CarActions, onCover: (Int) -> Unit = {}) {
    val base = LocalDensity.current
    Neon.sync(engine.prefs)
    CustomLook.sync(engine.prefs)
    // the map's colours follow the look live (they used to be set once, when the car connected): switching Neon on or off in the
    // phone's settings while the car is connected changes the car map at once
    val carMapColours = (if (Neon.on) "neon" else null) ?: engine.prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")
    LaunchedEffect(carMapColours) { engine.map.setColours(carMapColours) }
    // the locator follows the look and its accent colour live too (the neon arrow in the chosen colour)
    val (carLoc, carLocCol) = engine.prefs.locatorSpec().let { if (it.first == "custom") "arrow" to engine.prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6") else it }
    LaunchedEffect(carLoc, carLocCol, Neon.accentId) { engine.map.setLocator(carLoc, carLocCol) }
    // [scale] is worked out from the car screen's size (see CarSurface); the Android Auto size setting adjusts it either way
    val user = engine.prefs.i(PreferenceKeys.CARSCALE, 100).coerceIn(40, 300) / 100f
    val textScale = engine.prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).coerceIn(75, 150) / 100f
    CompositionLocalProvider(LocalDensity provides Density(base.density * scale.value * user, base.fontScale * textScale)) {
        VwTheme(dark.value) { CarScreen(engine, area.value, actions, onCover) }
    }
}

/** What taps on the app's cards do that needs Android Auto's own screens (search, the places list). */
interface CarActions {
    fun search()
    fun places()
    fun nearby(kind: String)
    fun settings()
    fun report() {}
}

/**
 * Taps on the car screen: Android Auto passes a tap's position (on newer cars), and it's matched against where the app's cards and
 * buttons are drawn, so the search bar, chips, route cards and Start work by touch as in the app.
 */
object CarTaps {
    private val regions = java.util.LinkedHashMap<String, Pair<androidx.compose.ui.geometry.Rect, () -> Unit>>()
    fun set(id: String, r: androidx.compose.ui.geometry.Rect, a: () -> Unit) { synchronized(regions) { regions.remove(id); regions[id] = r to a } }
    fun remove(id: String) { synchronized(regions) { regions.remove(id) } }
    /** Runs what's under (x, y); true when something was there. */
    fun hit(x: Float, y: Float): Boolean {
        val a = synchronized(regions) { regions.values.toList().asReversed().firstOrNull { it.first.contains(androidx.compose.ui.geometry.Offset(x, y)) }?.second } ?: return false
        a(); return true
    }
}

/** Makes this card or button react to taps on the car screen. */
@Composable
internal fun Modifier.carTap(id: String, action: () -> Unit): Modifier {
    val act = androidx.compose.runtime.rememberUpdatedState(action)
    androidx.compose.runtime.DisposableEffect(id) { onDispose { CarTaps.remove(id) } }
    return this.then(Modifier.onGloballyPositioned { CarTaps.set(id, it.boundsInRoot()) { act.value() } })
}

@Composable
private fun CarScreen(engine: NavEngine, a: android.graphics.Rect?, actions: CarActions, onCover: (Int) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val dens = LocalDensity.current
        val cardScale = engine.prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).coerceIn(70, 150) / 100f
        val buttonScale = engine.prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).coerceIn(70, 150) / 100f
        val requestedSpeedScale = engine.prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 140).coerceIn(40, 220) / 100f
        // Conservative auto-fit for the compact layout; the edge-positioned layout applies a second fit to its exact bounds.
        val speedScale = requestedSpeedScale * minOf(1f, (maxWidth.value / 220f).coerceAtLeast(0.25f), (maxHeight.value / 150f).coerceAtLeast(0.25f))
        val hPx = with(dens) { maxHeight.toPx() }
        val wPx = with(dens) { maxWidth.toPx() }
        // tall screens (portrait car displays, or Android Auto's split layouts) get their own layout: cards across the full width,
        // the turn card under Android Auto's buttons, and everything else stacked at the bottom
        // (also any screen too narrow for cards beside each other, so square and small screens get the stacked layout too)
        // Switch to a stacked layout when either the aspect ratio or the actual usable width makes a side-by-side
        // arrangement cramped. Keep this threshold lower than before so medium-width landscape screens retain the map.
        // Infer constrained/split-screen surfaces from the actual available Compose area rather than device model.
        // Android Auto reports its usable surface here, so a split-screen map naturally trips these thresholds.
        val aspect = maxWidth.value / maxHeight.value.coerceAtLeast(1f)
        // Include shorter usable areas common in split-screen Android Auto and ultra-wide dashboards.
        // Use the available content bounds, not physical display dimensions, so insets are respected.
        val lowVertical = maxHeight < 460.dp
        val narrow = maxWidth < 900.dp
        val wideLandscape = aspect > 2.0f
        val compact = lowVertical || narrow || wideLandscape
        // a short car screen (or a split-screen tile): the trip is one line and the speed sign is smaller
        val shortCar = maxHeight < 520.dp
        if (engine.isCar && engine.carShort != shortCar) engine.carShort = shortCar
        val portrait = maxHeight > maxWidth * 1.1f || narrow
        val insL = with(dens) { (a?.left ?: 0).toDp() }
        val insT = with(dens) { (a?.top ?: 0).toDp() }
        val insR = with(dens) { (wPx - (a?.right ?: wPx.toInt())).coerceAtLeast(0f).toDp() }
        val insB = with(dens) { (hPx - (a?.bottom ?: hPx.toInt())).coerceAtLeast(0f).toDp() }
        val edge = when {
            maxWidth < 600.dp -> 8.dp
            portrait -> 10.dp
            maxWidth < 1050.dp -> 16.dp
            else -> 24.dp
        }
        val left = insL + edge
        // Wide screens: level with Android Auto's buttons (top right); narrow/tall screens: below them.
        // (landscape: a smaller gap at the top, so the search bar sits level with Android Auto's Melden button beside it)
        val topTarget = if (portrait) insT + edge else insT.coerceAtMost(12.dp) + (edge / 2)
        // On tall screens Android Auto's buttons hide after a few seconds and the cards move up smoothly.
        val top by androidx.compose.animation.core.animateDpAsState(topTarget, tween(300), label = "carTop")
        val controlsGap = when {
            portrait -> 64.dp
            maxWidth < 900.dp -> 68.dp
            maxWidth < 1250.dp -> 80.dp
            else -> 92.dp
        }
        val rightTop = insR + if (portrait) edge else controlsGap
        // Android Auto's zoom and re-centre buttons, bottom right; reserve less space on smaller displays.
        val rightBottom = insR + if (portrait) 64.dp else controlsGap
        val bottom = insB + edge
        val previewing = engine.previewing
        val guiding = engine.route != null && !previewing
        val arriving = engine.arrival != null
        val c = LocalVw.current
        val topW = if (portrait) {
            (maxWidth - left - rightTop).coerceAtLeast(0.dp).coerceAtMost(600.dp)
        } else {
            minOf(if (maxWidth < 1050.dp) 380.dp else 440.dp, maxWidth * 0.38f)
        }
        var turnH by remember { mutableStateOf(0) }
        var topBlockH by remember { mutableStateOf(0) }

        // where the cards are, for placing your arrow the way the app does: on wide screens it moves sideways clear of the cards on
        // the left; on tall ones it stays centred and sits just above the cards along the bottom. Measured within the part of the
        // screen Android Auto leaves visible (the map's camera works within that part too).
        val vL = (a?.left ?: 0).toFloat(); val vT = (a?.top ?: 0).toFloat()
        val vW = (a?.width()?.toFloat() ?: wPx).coerceAtLeast(1f); val vH = (a?.height()?.toFloat() ?: hPx).coerceAtLeast(1f)
        fun report(i: Int, r: androidx.compose.ui.geometry.Rect) =
            engine.setPanel(i, (r.left - vL) / vW, (r.top - vT) / vH, (r.right - vL) / vW, (r.bottom - vT) / vH)
        val topShown = !portrait && !arriving && (engine.route == null || (guiding && engine.turn != null))
        val colShown = !portrait && !previewing && !arriving
        val stackShown = portrait && !previewing && !arriving
        androidx.compose.runtime.SideEffect {
            if (!topShown) engine.setPanel(0, 0f, 0f, 0f, 0f)
            if (!colShown) engine.setPanel(1, 0f, 0f, 0f, 0f)
            if (!stackShown) engine.bottomCover = 0.0
        }

        // the turn card, top left (as in the app)
        val t = engine.turn
        val showTurn = guiding && t != null && !arriving
        // hazards ahead (an incident, a speed camera) go along the bottom of the turn card when it's showing, instead of a
        // separate pill: the left side is then just the turn card and the trip card
        val alert = engine.alert
        val inc = engine.incidentAhead; val cam = engine.cameraDist
        // with the hazard radar on (as on the phone) it shows the incidents and cameras, so the single lines step aside
        val radarOn = engine.prefs.b(PreferenceKeys.HAZARDRADAR, false)
        val incShown = inc != null && alert?.kind != "incident" && !radarOn
        val camShown = cam != null && alert?.kind != "camera" && !radarOn
        // one slim line while the turn is far off (about 2 km), the full card as it comes up (a little either side, so it can't flicker)
        val slimHold = remember { booleanArrayOf(false) }
        if (t != null) slimHold[0] = if (t.dist > 2100.0) true else if (t.dist < 1900.0) false else slimHold[0]
        if (showTurn && t != null) Box(Modifier.padding(start = left, top = top).width(if (portrait) topW else if (slimHold[0]) minOf(560.dp, maxWidth * 0.44f) else minOf(520.dp, maxWidth * 0.42f))
            .onSizeChanged { turnH = it.height }.onGloballyPositioned { if (topShown) report(0, it.boundsInRoot()) }) {
            // the turn card, with the junction sign or roundabout diagram under it and any advice for the conditions, as on the head unit
            Column(verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)) {
                // (the car's "Navigation arrow size" is the arrow on the map, as on the phone: CarNav applies it there)
                CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) {
                    ImmTurnCard(engine, t, compact = compact, car = true, slim = slimHold[0],
                        footer = {
                            if (incShown || camShown) Column(Modifier.fillMaxWidth().background(GmGreenDeep).padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (incShown && inc != null) CarHazardLine("incident", inc.label, Geo.fmtDist(inc.dist, engine.imperial))
                                if (camShown && cam != null) CarHazardLine("camera", "Blitzer", Geo.fmtDist(cam.toDouble(), engine.imperial))
                            }
                        })
                }
                if (!compact) engine.advice?.let { AdvicePill(it) }
                NoticeCard(engine, big = true)
                CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) {
                    AvgSpeedCard(engine); ServicesCard(engine); ParkingCard(engine)
                }
            }
        }

        // not navigating: the app's search bar and quick chips, top left (tap them as in the app)
        // as many saved places as fit above the sound pill / speed (0 to 3), so the list never runs into what's below it
        // (worked out here: the screen's height can't be read from inside the column below)
        val savedRows = ((maxHeight - top - bottom - 330.dp - 150.dp) / 86.dp).toInt().coerceIn(0, 3)
        if (engine.route == null && !arriving) Column(Modifier.padding(start = left, top = top).width(topW).onSizeChanged { topBlockH = it.height }.onGloballyPositioned { if (topShown) report(0, it.boundsInRoot()) },
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // sized like the car's own Melden button: big enough to hit and read while driving
            Row(Modifier.fillMaxWidth().height(80.dp).panel(10.dp, RoundedCornerShape(40.dp), c.surface).padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                // the bar opens the search (Saved & recent folds itself, by its own header)
                Row(Modifier.weight(1f).fillMaxHeight().carTap("search") { actions.search() }.padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Search, null, tint = c.sub, modifier = Modifier.size(32.dp))
                    Text(stringResource(R.string.ui_064), color = c.sub, fontSize = 28.sp, modifier = Modifier.padding(start = 14.dp))
                }
                Box(Modifier.size(60.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.accent).carTap("search-mic") { actions.search() }, contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            NoticeCard(engine, big = true)
            if (!engine.cruising) CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) { PlannedCard(engine) }
            CarChips(engine, actions, if (engine.notice != null) 0 else savedRows)
            // (portrait screens: battery and range stay here; on a landscape screen they sit above the speed, bottom right)
            if (portrait) EvPill(engine)
        }

        // the charger power filter: slides out from the left (opened by the button under Einstellungen), pick one and it closes
        AnimatedVisibility(engine.chargerFilterOpen && !arriving, modifier = Modifier.align(Alignment.CenterStart).padding(start = left),
            enter = androidx.compose.animation.slideInHorizontally(tween(220)) { -it } + fadeIn(tween(200)),
            exit = androidx.compose.animation.slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(180))) { CarChargerFilter(engine) }

        // the charger filter while driving: under Android Auto's own Melden button, in the same column and the same size (Beenden
        // and Melden are Android Auto's buttons, top left; ending the trip and the sound are there or on the trip bar already).
        // Android Auto's column is the strip left of the stable area; where it's too narrow, the filter sits at the left edge.
        if (guiding && !previewing && !arriving && engine.isEv) {
            val colW = 156.dp
            val x = if (!portrait && insL > colW) (insL - colW) / 2 else left
            // Android Auto's buttons: about 80 dp tall, 21 dp apart, the first about 27 dp from the top: this is the third place
            // (no column of Android Auto's on the left, as on a narrow split screen: halfway down the left edge, where it was before)
            val inCol = !portrait && insL > colW
            val y = 27.dp + (80.dp + 21.dp) * 2
            Box((if (inCol) Modifier.padding(start = x, top = y) else Modifier.align(Alignment.CenterStart).padding(start = left)).width(colW).height(80.dp)
                .panel(10.dp, RoundedCornerShape(40.dp), if (engine.chargerMinKw > 0) c.accent else c.surface)
                .carTap("trip-filter") { engine.chargerFilterOpen = !engine.chargerFilterOpen }, contentAlignment = Alignment.Center) {
                Text(Lang.t("Filter", "Filter"), color = if (engine.chargerMinKw > 0) Color.White else c.text, fontSize = 28.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }

        // alerts (cameras, limits, hazards...): beside the turn card on wide screens, under it on tall ones
        val below = if (showTurn && turnH > 0) with(dens) { turnH.toDp() } + 10.dp else if (engine.route == null && topBlockH > 0) with(dens) { topBlockH.toDp() } + 10.dp else 0.dp
        val alertMod = if (portrait) Modifier.align(Alignment.TopStart).padding(start = left, end = rightTop + 64.dp, top = top + below)
            else Modifier.align(Alignment.TopCenter).padding(top = top, start = if (guiding) maxWidth * 0.42f else left, end = maxOf(rightTop, maxWidth * 0.30f))
        AnimatedVisibility(alert != null, modifier = alertMod, enter = fadeIn(), exit = fadeOut()) { alert?.let { AlertBanner(it) } }

        // a quicker way: a card at the top to take or leave (it goes again by itself)
        if (guiding && engine.faster != null) Box(Modifier.align(Alignment.TopCenter).padding(top = top + below, start = left, end = rightTop)) {
            CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) { FasterCard(engine, Modifier.widthIn(max = 560.dp)) }
        }

        // the compass, top right (below the sound button when the screen is tall), as on the phone: a tap turns north up or back
        if (!previewing) CompositionLocalProvider(LocalDensity provides Density(dens.density * buttonScale, dens.fontScale)) {
            ImmCompass(engine, Modifier.align(Alignment.TopEnd).padding(end = rightTop, top = top + below + if (portrait) 84.dp else 0.dp))
        }

        // an unknown car's Bluetooth device connected: which car it is
        VehicleAskCard(engine.prefs, onCar = true, Modifier.align(Alignment.TopCenter).padding(top = 12.dp))
        if (!previewing && !arriving) {
            // the trip card steps aside while the add-a-stop or all-locations panel is open, and comes back when it closes
            val panelOpen = engine.stopMode || engine.chain != null || engine.chainBusy || engine.alongOpen
            val hazards: @Composable () -> Unit = {
                // (only when there's no turn card to carry them)
                if (radarOn) HazardRadar(engine)
                else if (!showTurn && (incShown || camShown)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (incShown && inc != null) HazardBadge("incident", inc.label, Geo.fmtDist(inc.dist, engine.imperial))
                    if (camShown && cam != null) HazardBadge("camera", "Blitzer", Geo.fmtDist(cam.toDouble(), engine.imperial))
                }
            }
            if (portrait) {
                // tall: laid out like the app on a phone. The sound button sits on the right edge under the top cards; along the
                // bottom, the trip card spans the width (clear of Android Auto's buttons) with the hazards on the left above it and
                // your speed on the right above it
                var tripPx by remember { mutableStateOf(0) }
                // (while guiding, the sound button is on the trip card)
                if (!guiding) CompositionLocalProvider(LocalDensity provides Density(dens.density * buttonScale, dens.fontScale)) {
                    ImmRound(engine.soundIcon(), "sound", { engine.cycleSound() }, Modifier.align(Alignment.TopEnd).padding(end = rightTop, top = top + below))
                }
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = left, bottom = bottom)
                        .width((maxWidth - left - rightBottom).coerceAtLeast(0.dp).coerceAtMost(620.dp))
                        .onGloballyPositioned { engine.bottomCover = ((vT + vH - it.boundsInRoot().top) / vH).coerceIn(0f, 0.6f).toDouble() },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // (the row keeps the speed's height, so the hazards line up with it)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        Box(Modifier.weight(1f)) { hazards() }
                        Box(Modifier.padding(start = 30.dp, top = 14.dp)) { CompositionLocalProvider(LocalDensity provides Density(dens.density * speedScale, dens.fontScale)) { ImmSpeed(engine) } }
                    }
                    if (guiding && !panelOpen) CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) { Box(Modifier.onSizeChanged { tripPx = it.height }) { TripCard(engine, true, oneLine = shortCar) } }
                }
            } else {
                var leftColW by remember { mutableStateOf(0) }
                var leftColH by remember { mutableStateOf(0) }
                var speedW by remember { mutableStateOf(0) }
                Column(
                    Modifier.align(Alignment.BottomStart).padding(start = left, bottom = bottom).onSizeChanged { leftColW = it.width; leftColH = it.height }
                        .onGloballyPositioned { report(1, it.boundsInRoot()) },
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // (while guiding, the sound button is on the trip card instead of a pill of its own)
                    if (!guiding) CompositionLocalProvider(LocalDensity provides Density(dens.density * buttonScale, dens.fontScale)) { SoundPill(engine) }
                    hazards()
                    if (guiding && !panelOpen) CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale, dens.fontScale)) { TripCard(engine, false, oneLine = shortCar) }
                }
                // your speed and the limit, bottom right as in the app (clear of Android Auto's zoom buttons); on a small screen where
                // it would run into the cards on the left, it sits above them instead
                val crowded = with(dens) { (left + 16.dp + 34.dp + rightBottom).toPx() } + leftColW + speedW > wPx
                // Down in the bottom corner, level with the trip card and just left of Android Auto's zoom and re-centre buttons
                // (halfway up the side it looked lost in the middle of the screen)
                val speedBottom = if (crowded && leftColH > 0) bottom + with(dens) { leftColH.toDp() } + 10.dp else bottom
                // right up against the edge of the clear map: Android Auto reports the part of the screen its own buttons leave free,
                // which shrinks while its zoom and re-centre buttons are showing, so the sign sits just beside them then and slides
                // into the corner when they hide (it used to keep a fixed 92dp gap as well, so it sat well in from either)
                val speedEnd by androidx.compose.animation.core.animateDpAsState(insR + 6.dp, tween(280), label = "speedEnd")
                var speedH by remember { mutableStateOf(0) }
                BoxWithConstraints(Modifier.align(Alignment.BottomEnd).padding(end = speedEnd, bottom = speedBottom, start = 34.dp, top = 20.dp)
                    .onSizeChanged { speedW = it.width; speedH = it.height }) {
                    val availableFit = minOf(1f, (maxWidth.value / 220f).coerceAtLeast(0.25f), (maxHeight.value / 150f).coerceAtLeast(0.25f))
                    val fittedScale = requestedSpeedScale * availableFit
                    CompositionLocalProvider(LocalDensity provides Density(dens.density * fittedScale, dens.fontScale)) { ImmSpeed(engine) }
                }
                // battery and range from the car (or the widget), always shown, right above the speed (the speed keeps its own
                // place in the corner; the pill is placed by the speed's measured height, so it never pushes the speed away)
                Box(Modifier.align(Alignment.BottomEnd).padding(end = speedEnd, bottom = speedBottom + with(dens) { speedH.toDp() } + 10.dp)) { EvPill(engine) }
            }
        }

        // before setting off: the destination, the route choices and the Start bar that starts by itself after 10 s
        if (previewing) Box(Modifier.align(Alignment.BottomCenter).padding(start = left, end = if (portrait) rightBottom else rightTop, bottom = bottom)) {
            // drawn half as large again as the app's (its text was too small to read in the car), and with the car card size setting
            CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale * 1.5f, dens.fontScale)) { CarRoutePreview(engine) }
        }

        // working out a route: a small spinner at the top, as in the app
        AnimatedVisibility(engine.busy, modifier = Modifier.align(Alignment.TopCenter).padding(top = top + (if (portrait) below else 0.dp)),
            enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.size(48.dp).panel(6.dp, androidx.compose.foundation.shape.CircleShape, c.surface), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.size(26.dp), color = c.accent, strokeWidth = 3.dp)
            }
        }

        // a spot picked by tapping the map: go there (or add it as a stop)
        if (engine.mapPick != null && engine.arrival == null) Box(Modifier.align(Alignment.BottomCenter).padding(start = left, end = rightBottom, bottom = bottom)) {
            CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale * 1.2f, dens.fontScale)) { PickCard(engine) }
        }

        // the arrival summary
        val arr = engine.arrival
        // (drawn larger than in the app: its text and its buttons were too small to read and hit on the car screen)
        if (arr != null) Box(Modifier.align(Alignment.BottomCenter).padding(start = left, end = rightBottom, bottom = bottom)) {
            CompositionLocalProvider(LocalDensity provides Density(dens.density * cardScale * 1.35f, dens.fontScale)) { ArrivalCard(engine, arr, car = true) }
        }
    }
}

/** The spot tapped on the map: its name (or address), how far it is, and buttons to go there or drop it. */
@Composable
private fun PickCard(engine: NavEngine) {
    val p = engine.mapPick ?: return
    val c = LocalVw.current
    val guiding = engine.route != null && !engine.previewing && engine.dest != null
    val dist = engine.fix?.pos?.let { Geo.fmtDist(Geo.dist(it, p.pos), engine.imperial) }
    Row(
        Modifier.widthIn(max = 620.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Color(0xFFEA4335).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Place, null, tint = Color(0xFFEA4335), modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(p.name, color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = listOfNotNull(dist, p.sub.ifBlank { null }).joinToString(" · ")
            if (sub.isNotBlank()) Text(sub, color = c.sub, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.carTap("pick-go") { engine.goPick() }) { ImmPill(if (guiding) Lang.t("Als Stopp", "Add stop") else Lang.t("Hierhin fahren", "Go here"), {}, filled = true) }
        Box(Modifier.carTap("pick-no") { engine.clearPick() }) { ImmPill(Lang.t("Abbrechen", "Cancel"), {}) }
    }
}

/** A faster route turned up while driving: switch or keep, as in the app. */
@Composable
private fun FasterCard(engine: NavEngine, modifier: Modifier) {
    val fr = engine.faster ?: return
    val c = LocalVw.current
    Row(
        modifier.panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Good.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Route, null, tint = Good, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text(stringResource(R.string.ui_015), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Lang.t("Spart etwa ${fr.savedMin} Min.", "Saves about ${fr.savedMin} min"), color = Good, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Box(Modifier.carTap("faster-yes") { engine.acceptFaster() }) { ImmPill("Wechseln", {}, filled = true) }
        Box(Modifier.carTap("faster-no") { engine.dismissFaster() }) { ImmPill("Beibehalten", {}) }
    }
}

/** Battery and range from the car (or the widget). Dashes until a reading arrives, so the pill is never just missing. */
@Composable
private fun EvPill(engine: NavEngine) {
    val c = LocalVw.current
    // the estimate from the last reading, worked on every second: the drive since then comes off the battery (standing still adds nothing)
    val est by androidx.compose.runtime.produceState<Pair<Int, Int?>?>(null, engine) {
        while (true) {
            value = if (com.headunit.launcher.EvWidget.readAt(engine.prefs) > 0) Math.round(engine.evNowEstimatePct()).toInt() to engine.evEstimateKm() else null
            kotlinx.coroutines.delay(1000L)
        }
    }
    val evPct = est?.first ?: engine.prefs.s(PreferenceKeys.EV_NOW_PCT, "").toIntOrNull()
    val evKm = est?.second ?: engine.prefs.s(PreferenceKeys.EV_NOW_KM, "").toIntOrNull()
    // "ca." once the last real reading is more than 10 minutes old
    val approx = if (est != null && engine.evEstimateOld()) "ca. " else ""
    Row(Modifier.height(64.dp).panel(10.dp, RoundedCornerShape(32.dp), c.surface).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(if (evPct != null) "$approx$evPct %" else "– %", color = if (evPct == null) c.sub else if (evPct < 15) Bad else if (evPct < 30) Warn else c.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(if (evKm != null) "· $approx$evKm km" else "· – km", color = if (evKm == null) c.sub else c.text, fontSize = 28.sp, maxLines = 1)
        if (est != null && engine.evCharging()) Text(Lang.t("· lädt", "· charging"), color = Good, fontSize = 22.sp, maxLines = 1)
    }
}

/** Voice guidance: tap for all / alerts only / off (as the app's sound button). */
@Composable
private fun SoundPill(engine: NavEngine) {
    val c = LocalVw.current
    val sm = engine.prefs.i(PreferenceKeys.SOUND_MODE, if (engine.prefs.b(PreferenceKeys.VOICE, true)) 0 else 2).coerceIn(0, 2)
    Row(
        Modifier.height(78.dp).panel(6.dp, RoundedCornerShape(39.dp), c.surface).carTap("sound") { engine.cycleSound() }.padding(start = 14.dp, end = 26.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = when (sm) { 0 -> c.accent; 1 -> Warn; else -> Bad }
        Box(Modifier.size(54.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(
                when (sm) { 0 -> androidx.compose.material.icons.Icons.Rounded.VolumeUp; 1 -> androidx.compose.material.icons.Icons.Rounded.NotificationsActive; else -> androidx.compose.material.icons.Icons.Rounded.VolumeOff },
                null, tint = tint, modifier = Modifier.size(34.dp),
            )
        }
        Text(when (sm) { 0 -> "Ton an"; 1 -> "Nur Warnungen"; else -> "Stumm" }, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 14.dp))
    }
}

/** Time left, arrival, distance and the route bar, with the sound button and the red button that ends the route.
 *  [fill]: across the width, compact (tall screens). */
@Composable
private fun TripCard(engine: NavEngine, fill: Boolean, oneLine: Boolean = false) {
    val e = engine.eta ?: return
    val c = LocalVw.current
    val dc = if (e.delayMin >= 10) Bad else if (e.delayMin >= 1) Warn else Good
    // a short screen: time, arrival and distance on one line, with the end button (the figures stay, the rest goes)
    if (oneLine) {
        Row(Modifier.panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Geo.fmtDur(e.secs), color = dc, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(e.arrive, color = c.text, fontSize = 18.sp, maxLines = 1)
            Text(Geo.fmtDist(e.dist, engine.imperial), color = c.sub, fontSize = 18.sp, maxLines = 1, modifier = Modifier.weight(1f))
            Box(Modifier.size(46.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Bad).carTap("end") { engine.endRoute() }, contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        return
    }
    // the card is as wide as its figures and buttons (at least 400dp), never the whole screen: the figures used to share a row that
    // was told to fill the space, so on a very wide car screen the card stretched right across and its sound and end buttons ended
    // up under Android Auto's own buttons on the right. The route bar is set to the row's measured width (it can't be asked for
    // its natural width).
    var rowW by remember { mutableStateOf(0) }
    val dens = LocalDensity.current
    val w = if (fill) Modifier.fillMaxWidth() else Modifier.width(if (rowW > 0) with(dens) { rowW.toDp() } else 460.dp)
    Column((if (fill) Modifier.fillMaxWidth() else Modifier).panel(10.dp, RoundedCornerShape(22.dp), c.surface)) {
        // a slim bar: the full-height one took up nearly half the card
        if (engine.showRouteBar) RouteBar(engine, w.padding(start = 16.dp, end = 16.dp, top = 6.dp), thin = true)
        val end: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { sz ->
            // end the route, as in the app
            Box(Modifier.size(sz).clip(androidx.compose.foundation.shape.CircleShape).background(Bad).carTap("end") { engine.endRoute() }, contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Close, null, tint = Color.White, modifier = Modifier.size(sz * 0.52f))
            }
        }
        // sound: tap for all / alerts only / off, as the app's sound button; lit up in its colour unless it's fully on
        val sound: @Composable (androidx.compose.ui.unit.Dp) -> Unit = { sz ->
            val sm = engine.prefs.i(PreferenceKeys.SOUND_MODE, if (engine.prefs.b(PreferenceKeys.VOICE, true)) 0 else 2).coerceIn(0, 2)
            val tint = when (sm) { 0 -> c.sub; 1 -> Warn; else -> Bad }
            Box(Modifier.size(sz).clip(androidx.compose.foundation.shape.CircleShape).background(if (sm == 0) c.line else tint.copy(alpha = 0.22f))
                .carTap("sound") { engine.cycleSound() }, contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(engine.soundIcon(), null, tint = if (sm == 0) c.text else tint, modifier = Modifier.size(sz * 0.52f))
            }
        }
        val stats: @Composable () -> Unit = {
            CarStat(Geo.fmtDur(e.secs), if (e.delayMin >= 1) "+${e.delayMin} Min." else "Verbleibend", dc)
            CarStat(e.arrive, "Ankunft", c.text)
            CarStat(Geo.fmtDist(e.dist, engine.imperial), "Entfernung", c.text)
            // the battery and range the car reports now (as the app's chip), then the battery expected at arrival
            // (only on a tall screen: on a wide one the battery and range pill sits above the speed, so they aren't repeated here)
            if (fill) engine.evNowPct()?.let { p -> CarStat("$p%", "Akku jetzt", if (p < 10) Bad else if (p < 20) Warn else c.text) }
            if (fill) engine.evNowKm()?.let { k -> CarStat("$k km", "Reichweite", c.text) }
            engine.evArrivalPct()?.let { p -> CarStat("$p%", "Akku Ankunft", if (p < 10) Bad else if (p < 20) Warn else Good) }
        }
        // the two text buttons: the whole route at a glance, and the other routes (grey lines) on or off, as in the app
        val pill: @Composable (String, Dp, () -> Unit) -> Unit = { label, h, action ->
            Box(Modifier.height(h).clip(androidx.compose.foundation.shape.CircleShape).background(c.line).carTap("trip-$label") { action() }
                .padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                Text(label, color = c.text, fontSize = (20f * h.value / 46f).sp, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        val overviewBtn: @Composable (Dp) -> Unit = { h -> pill(Lang.t("Übersicht", "Overview"), h) { engine.overview() } }
        val otherBtn: @Composable (Dp) -> Unit = { h ->
            pill(if (engine.prefs.b(PreferenceKeys.LIVE_ALTERNATIVES, true)) Lang.t("Andere aus", "Hide others") else Lang.t("Andere an", "Show others"), h) { engine.toggleOtherRoutes() }
        }
        // the buttons chosen in Settings > Android Auto > Knöpfe auf der Fahrtkarte, and their size
        val showOv = engine.prefs.b(PreferenceKeys.TRIP_BTN_OVERVIEW, true)
        val showOther = engine.prefs.b(PreferenceKeys.TRIP_BTN_OTHERS, true)
        val showSound = engine.prefs.b(PreferenceKeys.TRIP_BTN_SOUND, true)
        val showEnd = engine.prefs.b(PreferenceKeys.TRIP_BTN_END, true)
        val sizeK = when (engine.prefs.s(PreferenceKeys.TRIP_BTN_SIZE, "normal")) { "large" -> 1.2f; "xl" -> 1.4f; else -> 1f }
        if (fill) Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 10.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { stats() }
            if (showOv) Box(Modifier.padding(start = 8.dp)) { overviewBtn(46.dp * sizeK) }
            if (showOther) Box(Modifier.padding(start = 8.dp)) { otherBtn(46.dp * sizeK) }
            if (showSound) Box(Modifier.padding(start = 12.dp)) { sound(46.dp * sizeK) }
            if (showEnd) Box(Modifier.padding(start = 8.dp)) { end(46.dp * sizeK) }
        } else Row(Modifier.widthIn(min = 460.dp).onSizeChanged { if (it.width != rowW) rowW = it.width }.padding(start = 16.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp), verticalAlignment = Alignment.CenterVertically) { stats() }
            Row(Modifier.padding(start = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (showOv) overviewBtn(54.dp * sizeK)
                if (showOther) otherBtn(54.dp * sizeK)
                if (showSound) sound(54.dp * sizeK)
                if (showEnd) end(54.dp * sizeK)
            }
        }
    }
}

/** A figure on the car trip card, with a label big enough to read at a glance (the app's own labels were tiny on a car screen). */
@Composable
private fun CarStat(value: String, label: String, color: Color) {
    val c = LocalVw.current
    Column {
        Text(value, color = color, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(label, color = c.sub, fontSize = 18.sp, maxLines = 1, softWrap = false)
    }
}

/** One hazard along the bottom of the car turn card: its road sign, what it is and how far. */
@Composable
private fun CarHazardLine(kind: String, label: String, dist: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AlertSign(com.headunit.launcher.AlertInfo(kind, label), 22.dp)
        Text("$label in $dist", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

/** A round button like the app's (sound and so on), tappable on the car screen. */
@Composable
private fun ImmRound(icon: androidx.compose.ui.graphics.vector.ImageVector, id: String, action: () -> Unit, modifier: Modifier) {
    val c = LocalVw.current
    Box(modifier.size(68.dp).panel(6.dp, androidx.compose.foundation.shape.CircleShape, c.surface).carTap(id, action), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Icon(icon, null, tint = c.text, modifier = Modifier.size(34.dp))
    }
}

private fun NavEngine.soundIcon(): androidx.compose.ui.graphics.vector.ImageVector = when (prefs.i(PreferenceKeys.SOUND_MODE, if (prefs.b(PreferenceKeys.VOICE, true)) 0 else 2).coerceIn(0, 2)) {
    0 -> androidx.compose.material.icons.Icons.Rounded.VolumeUp
    1 -> androidx.compose.material.icons.Icons.Rounded.NotificationsActive
    else -> androidx.compose.material.icons.Icons.Rounded.VolumeOff
}

/** The app's route choice, as on its own screen: pick a route, start with Start (or wait 10 s). Stacks up on narrow screens. */
@Composable
private fun CarRoutePreview(engine: NavEngine) {
    val c = LocalVw.current
    val rs = engine.routes.take(3)
    if (rs.isEmpty()) return
    val fastest = rs.indices.minByOrNull { rs[it].time } ?: 0
    val shortest = rs.indices.minByOrNull { rs[it].len } ?: 0
    val progress = remember { Animatable(0f) }
    val autoStart = engine.prefs.b(PreferenceKeys.AUTO_START_ROUTE, true)
    val countdownSeconds = engine.prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30)
    LaunchedEffect(engine.active, engine.routes, autoStart, countdownSeconds) {
        progress.snapTo(0f)
        if (!autoStart) return@LaunchedEffect
        progress.animateTo(1f, tween(countdownSeconds * 1000, easing = LinearEasing))
        if (engine.previewing) engine.beginNavigation()
    }
    val fmt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val baseMs = if (engine.departAtMs > 0) engine.departAtMs else System.currentTimeMillis()
    val dest: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            Text(stringResource(R.string.ui_012), color = c.sub, fontSize = 12.sp)
            Text(engine.dest?.name ?: "", color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
    val cards: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        rs.forEachIndexed { i, r ->
            val sel = i == engine.active
            val label = if (i == fastest) "Schnellste" else if (i == engine.simplerIdx) Lang.t("Einfachste", "Simplest") else if (i == shortest) "Kürzeste" else "Alternative"
            Column(Modifier.weight(1f).inset(18.dp, if (sel) c.accent else null).carTap("route-$i") { engine.selectRoute(i) }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text(label.uppercase(), color = if (sel) c.accent else c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1)
                Text(Geo.fmtDur(r.time), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text("${Geo.fmtDist(r.len, engine.imperial)} · ${Lang.t("Ankunft", "arrive")} ${fmt.format(java.util.Date(baseMs + (r.time * 1000).toLong()))}", color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val via = viaOf(r)
                val delayMin = Math.round(r.delay / 60.0).toInt()
                Text(if (delayMin >= 1) "+$delayMin Min. Verkehr" else if (via.isNotEmpty()) "über $via" else "Keine Verzögerung",
                    color = if (delayMin >= 10) Bad else if (delayMin >= 1) Warn else c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    val start: @Composable (Modifier) -> Unit = { m ->
        Box(m.height(72.dp).clip(RoundedCornerShape(36.dp)).background(c.accent.copy(alpha = 0.30f)).carTap("start") { engine.beginNavigation() }, contentAlignment = Alignment.Center) {
            Box(Modifier.align(Alignment.CenterStart).fillMaxHeight().fillMaxWidth(progress.value).background(c.accent))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.ui_052), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                if (autoStart) Text(Lang.t("Start in ${kotlin.math.ceil((1f - progress.value) * countdownSeconds).toInt()} s", "Starting in ${kotlin.math.ceil((1f - progress.value) * countdownSeconds).toInt()} s"), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) else Text(stringResource(R.string.ui_055), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            }
        }
    }
    val cancel: @Composable () -> Unit = {
        Box(Modifier.size(46.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.tile).carTap("cancel") { engine.endRoute() }, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Close, null, tint = c.text, modifier = Modifier.size(22.dp))
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The horizontal arrangement needs room for the destination, three route cards, Start and Close.
        // A 760dp breakpoint left the route cards squeezed on medium displays, so stack until there is real room.
        // (one row when the destination, every route card, Start and Close fit side by side; else stacked)
        if (maxWidth >= (170 + 220 + 46 + 40 + rs.size * 200).dp) Row(
            Modifier.fillMaxWidth().floating(28.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            dest(Modifier.width(170.dp))
            cards()
            start(Modifier.width(220.dp))
            cancel()
        } else Column(
            // narrow: the destination and close button, the routes side by side, then Start across the width
            Modifier.fillMaxWidth().floating(28.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) { dest(Modifier.weight(1f).padding(start = 6.dp)); cancel() }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { cards() }
            start(Modifier.fillMaxWidth())
        }
    }
}

/**
 * Not navigating, under the search bar, as in the app: Home and Work as two tiles with the drive time, five round buttons to find
 * things nearby (they open a list to pick from), then Saved & recent with the first few places ([rows], as many as fit) and "All" for
 * the rest. Every part works by touch on cars that pass taps on.
 */
@Composable
private fun CarChips(engine: NavEngine, actions: CarActions, rows: Int) {
    val c = LocalVw.current
    val home = engine.places.slot("home"); val work = engine.places.slot("work")
    // the drive times (the engine throttles this itself: online only, every 10 minutes or after moving 3 km)
    LaunchedEffect(Unit) { while (true) { engine.refreshSlotTimes(); kotlinx.coroutines.delay(60_000) } }
    val here = engine.fix?.pos
    val slim = engine.carShort
    @Composable fun tile(id: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, place: com.headunit.launcher.Place?, mins: Int?, m: Modifier) {
        val sub = when {
            place == null -> Lang.t("Festlegen", "Set")
            mins == 0 -> stringResource(R.string.ui_102)
            mins != null && mins > 0 -> Geo.fmtDur(mins * 60.0)
            here != null -> Geo.fmtDist(Geo.dist(here, place.pos), engine.imperial)
            else -> place.sub
        }
        // a short screen: slim pills (the same two lines, smaller)
        Row(m.height(if (slim) 52.dp else 86.dp).panel(6.dp, RoundedCornerShape(if (slim) 26.dp else 30.dp), c.surface).carTap(id) { if (place != null) engine.startRoute(place) else actions.search() }
            .padding(horizontal = if (slim) 8.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(if (slim) 36.dp else 60.dp).clip(androidx.compose.foundation.shape.CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(if (slim) 22.dp else 36.dp))
            }
            Column(Modifier.padding(start = if (slim) 8.dp else 14.dp).weight(1f)) {
                // (the short label even when not set yet: "Setze zuhause" was cut off at this size; the line below says "Festlegen")
                Text(label, color = c.text, fontSize = if (slim) 18.sp else 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(sub, color = c.sub, fontSize = if (slim) 15.sp else 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    @Composable fun find(kind: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
        Box(Modifier.size(if (slim) 50.dp else 78.dp).panel(6.dp, androidx.compose.foundation.shape.CircleShape, c.surface).carTap("n-$kind") { actions.nearby(kind) }, contentAlignment = Alignment.Center) {
            androidx.compose.material3.Icon(icon, null, tint = tint, modifier = Modifier.size(if (slim) 26.dp else 42.dp))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tile("home", androidx.compose.material.icons.Icons.Rounded.Home, Color(0xFF4C8DFF), stringResource(R.string.ui_100), home, engine.slotMins["home"], Modifier.weight(1.2f))
            tile("work", androidx.compose.material.icons.Icons.Rounded.Work, Color(0xFFF29900), stringResource(R.string.ui_101), work, engine.slotMins["work"], Modifier.weight(1.2f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (!(engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV")) find("fuel", androidx.compose.material.icons.Icons.Rounded.LocalGasStation, Color(0xFFEA4335))
            if (engine.camperProfile) {
                // a motorhome or caravan: pitches, water & waste and LPG instead of charging, food and coffee
                find("camp", androidx.compose.material.icons.Icons.Rounded.RvHookup, Color(0xFF2E7D32))
                find("dump", androidx.compose.material.icons.Icons.Rounded.WaterDrop, Color(0xFF0288D1))
                find("lpg", androidx.compose.material.icons.Icons.Rounded.PropaneTank, Color(0xFFEF6C00))
            } else {
            if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") find("charging", androidx.compose.material.icons.Icons.Rounded.EvStation, Color(0xFF34A853))
            find("food", androidx.compose.material.icons.Icons.Rounded.Restaurant, Color(0xFFF29900))
            find("coffee", androidx.compose.material.icons.Icons.Rounded.LocalCafe, Color(0xFFA56BFF))
            }
            find("parking", androidx.compose.material.icons.Icons.Rounded.LocalParking, Color(0xFF4C8DFF))
        }
        // your own places first, then favourites, then recent ones (as in the app)
        val rawC = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, ""); val rawF = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, ""); val rawR = engine.prefs.s(PreferenceKeys.PLACES_RECENT, "")
        val saved = remember(rawC, rawF, rawR) {
            val mine = engine.places.customs()
            val favs = engine.places.favourites().filter { f -> mine.none { Geo.dist(it.place.pos, f.pos) < 60 } }
            val recents = engine.places.recents().filter { r -> favs.none { Geo.dist(it.pos, r.pos) < 60 } && mine.none { Geo.dist(it.place.pos, r.pos) < 60 } }
            mine.map { Triple(it.place, true, it.label) } + favs.map { Triple(it, true, it.name) } + recents.take(4).map { Triple(it, false, it.name) }
        }
        // Saved & recent: folded when the car screen comes up; its own header folds it open and shut (the Settings switch hides it altogether)
        if (engine.prefs.b(PreferenceKeys.QUICKLIST, true)) Column(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(vertical = 2.dp)) {
            val open = !engine.quickFolded
            Row(Modifier.fillMaxWidth().carTap("saved") { engine.quickFolded = !engine.quickFolded }.padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_040), color = c.sub, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
                androidx.compose.material3.Icon(if (open) androidx.compose.material.icons.Icons.Rounded.KeyboardArrowUp else androidx.compose.material.icons.Icons.Rounded.KeyboardArrowDown, null, tint = c.sub, modifier = Modifier.size(34.dp))
            }
            if (open) {
            // all of them: opens the full list
            Row(Modifier.fillMaxWidth().carTap("saved-all") { actions.places() }.padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
                Text(if (saved.size > rows) Lang.t("Alle ${saved.size}", "All ${saved.size}") else Lang.t("Öffnen", "Open"), color = c.accent, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            for ((i, tr) in saved.take(rows).withIndex()) {
                val (p, fav, label) = tr
                Row(Modifier.fillMaxWidth().carTap("saved-$i") { engine.startRoute(p) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(if (fav) androidx.compose.material.icons.Icons.Rounded.Star else androidx.compose.material.icons.Icons.Rounded.History, null,
                            tint = if (fav) Warn else c.sub, modifier = Modifier.size(30.dp))
                    }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(label, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 21.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (here != null) Text(Geo.fmtDist(Geo.dist(here, p.pos), engine.imperial), color = c.sub, fontSize = 21.sp, modifier = Modifier.padding(start = 10.dp))
                }
            }
            }
        }
    }
}

/** The charger power filter on the car map: one tap on a class (all, 50, 150, 300 kW and faster) sets it and closes the list. */
@Composable
private fun CarChargerFilter(engine: NavEngine) {
    val c = LocalVw.current
    val options = listOf(0 to Lang.t("Alle Ladesäulen", "All chargers"), 50 to Lang.t("Ab 50 kW", "50 kW and up"),
        150 to Lang.t("Ab 150 kW", "150 kW and up"), 300 to Lang.t("Ab 300 kW", "300 kW and up"))
    Column(Modifier.width(340.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(vertical = 2.dp)) {
        Row(Modifier.fillMaxWidth().carTap("cf-close") { engine.chargerFilterOpen = false }.padding(start = 16.dp, end = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(Lang.t("Ladesäulen filtern", "Filter chargers").uppercase(), color = c.sub, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.weight(1f))
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Close, null, tint = c.sub, modifier = Modifier.size(32.dp))
        }
        for ((kw, label) in options) {
            val on = engine.chargerMinKw == kw
            Row(Modifier.fillMaxWidth().carTap("cf-$kw") { engine.pickChargerMinKw(kw); engine.chargerFilterOpen = false }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = if (on) c.accent else c.text, fontSize = 26.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (on) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Check, null, tint = c.accent, modifier = Modifier.size(30.dp))
            }
        }
    }
}
