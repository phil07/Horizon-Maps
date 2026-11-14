package com.headunit.launcher.ui

import com.headunit.launcher.R
import com.headunit.launcher.Lang
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PropaneTank
import androidx.compose.material.icons.rounded.RvHookup
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Satellite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.Junctions
import com.headunit.launcher.LL
import com.headunit.launcher.NavEngine
import kotlinx.coroutines.delay
import com.headunit.launcher.PreferenceKeys

private val Teal = GmGreen
private val TealDeep = GmGreenDeep

/** The map screen. It looks the same whichever map colours are chosen; it takes the shared soft palette. */
@Composable
fun ImmersiveNav(engine: NavEngine, onSettings: () -> Unit) {
    val base = LocalVw.current
    val glass = engine.prefs.b(PreferenceKeys.GLASS, true)
    val v = remember(base, glass) { navVw(base).let { if (glass) glassVw(it) else it } }
    CompositionLocalProvider(LocalVw provides v) { MapScreen(engine, onSettings) }
}

// ------------------------------------------------------------------ small building blocks

/** True for buttons inside the grouped column on the right: they sit flat in its one panel instead of each floating on its own. */
private val LocalInRail = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
private fun ImmCircle(onClick: () -> Unit, size: Dp = 52.dp, active: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val c = LocalVw.current
    Box(
        if (LocalInRail.current) Modifier.size(size).pressClick(onClick).padding(4.dp).clip(CircleShape).background(if (active) c.accent else Color.Transparent)
        else Modifier.size(size).panel(6.dp, CircleShape, if (active) c.accent else c.surface).pressClick(onClick),
        contentAlignment = Alignment.Center, content = content,
    )
}

@Composable
private fun ImmIcon(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, size: Dp = 52.dp, desc: String? = null) {
    val c = LocalVw.current
    ImmCircle(onClick, size, active) { Icon(icon, desc, tint = if (active) Color.White else c.text, modifier = Modifier.size(size * 0.46f)) }
}

@Composable
internal fun ImmPill(text: String, onClick: () -> Unit, icon: ImageVector? = null, filled: Boolean = false) {
    val c = LocalVw.current
    val fg = if (filled) Color.White else c.text
    Row(
        Modifier.height(48.dp).panel(6.dp, RoundedCornerShape(24.dp), if (filled) c.accent else c.surface)
            .pressClick(onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

@Composable
internal fun ImmStat(value: String, label: String, color: Color, compact: Boolean = false) {
    val c = LocalVw.current
    Column {
        Text(value, color = color, fontSize = if (compact) 19.sp else 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text(label, color = c.sub, fontSize = if (compact) 11.sp else 12.sp, maxLines = 1, softWrap = false)
    }
}

private class TileSpec(val icon: ImageVector, val label: String, val tint: Color, val onClick: () -> Unit, val picture: Int? = null)

@Composable
private fun SheetHeading(text: String) {
    Text(text.uppercase(), color = LocalVw.current.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
}

// ------------------------------------------------------------------ the screen

@Composable
private fun MapScreen(engine: NavEngine, onSettings: () -> Unit) {
    val c = LocalVw.current
    val shareEta = rememberShareEta(engine)
    var q by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf(false) }
    // voice: "take me to…", "share my ETA", "avoid this road", "mute"… anything else is searched for
    val voice = rememberVoiceInput(Lang.t("Sag ein Ziel oder einen Befehl", "Say a destination or a command")) { said ->
        when (val r = engine.handleVoice(said)) {
            null -> { q = said; engine.search(said) }
            "share" -> shareEta()
            "" -> {}
            else -> if (r.startsWith("search:")) { val w = r.removePrefix("search:"); q = w; engine.search(w) }
        }
    }
    val typingLocked by remember { derivedStateOf { (engine.fix?.speed ?: 0.0) > 3.0 && engine.prefs.b(PreferenceKeys.DRIVELOCK, false) } }
    val focus = remember { FocusRequester() }
    LaunchedEffect(q) {
        if (q.trim().length >= 2) { delay(500); engine.search(q) } else if (q.isEmpty()) engine.results = emptyList()
    }
    // text shared from another app goes into the search box
    LaunchedEffect(engine.externalQuery) { engine.externalQuery?.let { q = it; engine.externalQuery = null } }
    BackHandler(enabled = sheet) { sheet = false }
    val routing = engine.route != null && !engine.previewing
    val previewing = engine.previewing
    val simple by remember { derivedStateOf { engine.driving && engine.prefs.b(PreferenceKeys.DRIVESIMPLE, true) } }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        // portrait (or any narrow window): cards use the full width, the round buttons stack down the right-hand side,
        // and your speed and the re-centre button sit just above the trip card instead of beside it
        val narrow = maxWidth < 720.dp
        val dens = androidx.compose.ui.platform.LocalDensity.current
        var tripPx by remember { mutableStateOf(0) }
        // where the speedometer starts (in pixels), so zoom only moves beside the panel when it would really touch it
        var speedTopPx by remember { mutableStateOf(-1) }
        val tripH = with(dens) { tripPx.toDp() }
        // heights of the turn card and the arrival card, so the traffic pill can sit under the one or above the other
        var turnPx by remember { mutableStateOf(0) }
        var arrPx by remember { mutableStateOf(0) }
        val screenH = maxHeight
        LaunchedEffect(narrow, tripPx, screenH) { engine.bottomCover = if (narrow && tripPx > 0) (tripH / screenH).coerceIn(0f, 0.5f).toDouble() else 0.0 }
        // left-hand drive: everything swaps sides (cards on the right, round buttons on the left)
        val mir = engine.mirrored
        // Settings > sizes > map buttons: scales the button column on the right
        val mainButtonScale = engine.prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).coerceIn(70, 150) / 100f
        fun side(a: Alignment): Alignment = if (!mir) a else when (a) {
            Alignment.TopStart -> Alignment.TopEnd; Alignment.TopEnd -> Alignment.TopStart
            Alignment.CenterStart -> Alignment.CenterEnd; Alignment.CenterEnd -> Alignment.CenterStart
            Alignment.BottomStart -> Alignment.BottomEnd; Alignment.BottomEnd -> Alignment.BottomStart
            else -> a
        }
        // padding on the "outer" (screen edge) and "inner" sides, swapped when mirrored
        fun Modifier.sides(start: androidx.compose.ui.unit.Dp = 0.dp, end: androidx.compose.ui.unit.Dp = 0.dp, top: androidx.compose.ui.unit.Dp = 0.dp, bottom: androidx.compose.ui.unit.Dp = 0.dp) =
            if (mir) padding(start = end, end = start, top = top, bottom = bottom) else padding(start = start, end = end, top = top, bottom = bottom)
        // ---- top left: search while idle; the turn card and its companions while navigating
        // the panels' outlines (as parts of the screen) go to the camera, so your arrow is moved clear of whatever is beside it
        val boxW = constraints.maxWidth.toFloat().coerceAtLeast(1f); val boxH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        fun report(i: Int, r: androidx.compose.ui.geometry.Rect) { engine.setPanel(i, r.left / boxW, r.top / boxH, r.right / boxW, r.bottom / boxH) }
        // a place tapped on the map: its card floats over everything else (also over the route options while choosing a route)
        if (engine.mapPick != null) Box(Modifier.align(Alignment.Center).offset(y = (-70).dp).padding(horizontal = 16.dp).zIndex(30f)) { PhonePickCard(engine) }
        Column(Modifier.align(side(Alignment.TopStart)).padding(16.dp).then(if (narrow) Modifier.fillMaxWidth().sides(end = 52.dp * mainButtonScale + 10.dp) else Modifier.width(380.dp))
            .onGloballyPositioned { report(0, it.boundsInParent()) }
            // the turn list opens over the trip card below it
            .zIndex(if (engine.itineraryOpen) 2f else 0f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // every block slides and fades in and out instead of popping, so the screen changes feel like one piece
            AnimatedVisibility(!routing && !previewing, enter = fadeIn(tween(220)) + androidx.compose.animation.expandVertically(tween(260)), exit = fadeOut(tween(160)) + androidx.compose.animation.shrinkVertically(tween(220))) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // on foot to the parked car: the next instruction and what is left, over the search bar
                if (engine.walking) WalkCard(engine)
                ImmSearch(engine, q, { q = it }, typingLocked, focus, voice)
                // adding a stop: the matches along the rest of the route (the keyboard's search key does the same)
                if (engine.stopMode && q.trim().length >= 2 && !engine.alongOpen) ImmPill(Lang.t("Entlang der Route suchen", "See them along your route"), { engine.searchAlong(q) }, icon = Icons.Rounded.Route, filled = true)
                if (engine.alongOpen) AlongPanel(engine)
                engine.phoneDest?.let { pd -> PhoneDestCard(engine, pd) }
                // an unknown car's Bluetooth device: which car it is (see Settings > Android Auto > Fahrzeuge)
                VehicleAskCard(engine.prefs, onCar = false, Modifier.fillMaxWidth())
                val showResults = engine.results.isNotEmpty() || engine.chain != null || engine.chainBusy
                AnimatedVisibility(showResults, enter = fadeIn(tween(180)) + androidx.compose.animation.expandVertically(tween(240)), exit = fadeOut(tween(120)) + androidx.compose.animation.shrinkVertically(tween(180))) {
                    if (engine.chain != null || engine.chainBusy) ChainPanel(engine) { q = "" } else ImmResults(engine) { q = "" }
                }
                AnimatedVisibility(!showResults && q.isEmpty() && engine.nearbyKind == null, enter = fadeIn(tween(220)) + androidx.compose.animation.expandVertically(tween(260)), exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(200))) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // your places (Home, Work...) always stay under the search bar; while you drive with no route, the suggestion and
                        // resume cards stay too and the rest folds away
                        val cruising = engine.cruising
                        ImmChips(engine) { try { focus.requestFocus() } catch (_: Exception) {} }
                        NoticeCard(engine)
                        AnimatedVisibility(!cruising, enter = fadeIn(tween(220)) + androidx.compose.animation.expandVertically(tween(260)), exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(200))) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                PlannedCard(engine)
                                AppUpdateCard(engine)
                                // newer map data first; the "it's getting old" reminder only when there's nothing newer to offer
                                if (engine.dataUpdates.isNotEmpty()) DataUpdateCard(engine) else PackAgeCard(engine)
                            }
                        }
                        ImmQuick(engine, showSaved = !cruising)
                    }
                }
              }
            }
            if (routing) {
                if (engine.chain != null || engine.chainBusy) ChainPanel(engine) { q = "" }
                else if (engine.results.isNotEmpty()) ImmResults(engine) { q = "" }
                else {
                    val curTurn = engine.turn
                    val lastTurn = remember { mutableStateOf(curTurn) }
                    if (curTurn != null) lastTurn.value = curTurn
                    AnimatedVisibility(curTurn != null, enter = fadeIn(tween(240)) + androidx.compose.animation.slideInVertically(tween(300)) { -it / 4 }, exit = fadeOut(tween(160))) {
                        lastTurn.value?.let { t ->
                            if (engine.itineraryOpen) {
                                val itineraryDensity = LocalDensity.current
                                val itineraryScale = engine.prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).coerceIn(70, 150) / 100f
                                CompositionLocalProvider(LocalDensity provides Density(itineraryDensity.density * itineraryScale, itineraryDensity.fontScale)) { ItineraryCard(engine) }
                            } else {
                                val turnDensity = LocalDensity.current
                                val cardSize = engine.prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).coerceIn(70, 150) / 100f
                                CompositionLocalProvider(LocalDensity provides Density(turnDensity.density * cardSize, turnDensity.fontScale)) {
                                    Box(Modifier.onSizeChanged { turnPx = it.height }) { ImmTurnCard(engine, t) }
                                }
                            }
                        }
                    }
                    engine.advice?.let { AdvicePill(it) }
                    NoticeCard(engine)
                    AvgSpeedCard(engine)
                    ServicesCard(engine)
                    ParkingCard(engine)
                }
            }
            if (engine.nearbyKind != null && (!previewing || engine.stopMode)) NearbyCard(engine)
        }

        // ---- right side: the map buttons grouped in one panel at the top, zoom under it, and Settings beside it
        if (!previewing) {
            val buttonBaseDensity = LocalDensity.current
            // just two buttons on the map: the compass and map options. 3D / 2D and sound are at the top of map options, voice and sound
            // are on the trip card while navigating, Settings is beside these when you're not (and in map options' header when you are)
            val railItems = ArrayList<@Composable () -> Unit>()
            railItems.add(@Composable { ImmCompass(engine) })
            railItems.add(@Composable { ImmIcon(Icons.Rounded.Layers, { sheet = !sheet }, active = sheet, desc = Lang.t("Kartenoptionen", "Map options")) })
            if (!simple && (engine.prefs.b(PreferenceKeys.DEMO, false) || engine.simMode != 0)) railItems.add(@Composable { ImmIcon(Icons.Rounded.PlayArrow, { engine.cycleSim() }, active = engine.simMode != 0, desc = Lang.t("Demofahrt", "Demo drive")) })
            // zoom: hidden while the map is following you on the move (it zooms itself); back as soon as you touch or zoom the map, or
            // whenever you're stopped. (A little hysteresis on the speed, so it doesn't flicker at walking pace.)
            var stopped by remember { mutableStateOf(true) }
            val spNow = engine.fix?.speed ?: 0.0
            androidx.compose.runtime.SideEffect { if (spNow > 3.0) stopped = false else if (spNow < 1.0) stopped = true }
            val showZoom = stopped || !engine.following || kotlin.math.abs(engine.zoomOffset) > 0.01
            // electric on a trip: the zoom column stays, with just the charger filter (the zoom buttons come back when you zoom or stop)
            val evTrip = engine.isEv && engine.route != null && !engine.previewing
            val showSettings = !routing
            val unit = 52.dp * mainButtonScale
            // the top of the speedometer (measured; a fair guess until it has been laid out)
            val speedTop = if (speedTopPx >= 0) with(dens) { speedTopPx.toDp() } else maxHeight - 190.dp
            // landscape: Settings sits to the left of the panel (to the right when mirrored) so the panel starts at the very top, with
            // zoom straight under it. Portrait: Settings above the panel, as there's no room beside it next to the search bar.
            val settingsAbove = narrow && showSettings
            val top = 16.dp * mainButtonScale + (if (settingsAbove) unit + 10.dp * mainButtonScale else 0.dp)
            // (with zoom hidden there's nothing to fit under the panel)
            val zoomH = (2 * 52 + 1).dp * mainButtonScale
            // zoom goes under the panel unless that would touch the speedometer; then it moves beside the panel, and on a really short
            // screen the panel splits into two columns rather than run into the speedometer
            val railH = unit * railItems.size
            val zoomUnder = narrow || top + railH + 10.dp * mainButtonScale + zoomH + 8.dp <= speedTop
            val perCol = ((speedTop - top - 8.dp) / unit).toInt().coerceAtLeast(3)
            val chunks = if (narrow || railItems.size <= perCol) listOf(railItems.toList()) else railItems.chunked(perCol)
            CompositionLocalProvider(LocalDensity provides Density(buttonBaseDensity.density * mainButtonScale, buttonBaseDensity.fontScale)) {
                val rail: @Composable (List<@Composable () -> Unit>) -> Unit = { part ->
                    Column(Modifier.panel(6.dp, RoundedCornerShape(26.dp), c.surface), horizontalAlignment = Alignment.CenterHorizontally) {
                        CompositionLocalProvider(LocalInRail provides true) { for (item in part) item() }
                    }
                }
                val zoom: @Composable () -> Unit = {
                    AnimatedVisibility(showZoom || evTrip, enter = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(220)), exit = fadeOut(tween(250)) + androidx.compose.animation.shrinkVertically(tween(260))) {
                        Column(Modifier.panel(6.dp, RoundedCornerShape(26.dp), c.surface), horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showZoom) { Box(Modifier.size(52.dp).pressClick { engine.zoom(1.0) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Add, Lang.t("Hineinzoomen", "Zoom in"), tint = c.text, modifier = Modifier.size(26.dp)) }
                            Box(Modifier.width(26.dp).height(1.dp).background(c.line))
                            Box(Modifier.size(52.dp).pressClick { engine.zoom(-1.0) }, contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Remove, Lang.t("Herauszoomen", "Zoom out"), tint = c.text, modifier = Modifier.size(26.dp)) } }
                            // charge points: the power filter, under the minus (only with an electric car)
                            if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") {
                                Box(Modifier.width(26.dp).height(1.dp).background(c.line))
                                Box(Modifier.size(52.dp).pressClick { engine.chargerFilterOpen = true }, contentAlignment = Alignment.Center) {
                                    // a funnel (filter) with a small bolt beside it (charging points)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.FilterAlt, Lang.t("Ladesäulen filtern", "Filter chargers"), tint = if (engine.chargerMinKw > 0) c.accent else c.text, modifier = Modifier.size(24.dp))
                                        Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF34A853), modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                val settings: @Composable () -> Unit = { if (showSettings) ImmIcon(Icons.Rounded.Settings, { sheet = false; onSettings() }, desc = Lang.t("Einstellungen", "Settings")) }
                // the panel against the screen edge with zoom under it (or beside it), in as many columns as it needs
                val panelAndZoom: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                        val parts = if (mir) chunks else chunks.asReversed()
                        if (!zoomUnder && !mir) zoom()
                        for ((i, part) in parts.withIndex()) {
                            // zoom goes under the column that's against the screen edge
                            val edge = if (mir) i == 0 else i == parts.size - 1
                            if (edge && zoomUnder) Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { rail(part); zoom() }
                            else rail(part)
                        }
                        if (!zoomUnder && mir) zoom()
                    }
                }
                // (its outline goes to the camera too, so with no route your arrow sits in the middle of the clear map)
                if (settingsAbove) Column(Modifier.align(side(Alignment.TopEnd)).padding(16.dp).onGloballyPositioned { report(2, it.boundsInParent()) }, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = if (mir) Alignment.Start else Alignment.End) {
                    settings(); panelAndZoom()
                }
                else Row(Modifier.align(side(Alignment.TopEnd)).padding(16.dp).onGloballyPositioned { report(2, it.boundsInParent()) }, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                    if (mir) { panelAndZoom(); settings() } else { settings(); panelAndZoom() }
                }
            }
        } else androidx.compose.runtime.SideEffect { engine.setPanel(2, 0f, 0f, 0f, 0f) }

        // ---- speed limit and speedometer: bottom right, or bottom left when mirrored for left-hand drive (the cards are on the
        // right then; keeping the speed on the right too put it under the turn card on shorter screens)
        if (!previewing) Box(
            Modifier.align(side(Alignment.BottomEnd))
                .sides(end = 16.dp, bottom = if (narrow && tripPx > 0) tripH + 4.dp else 16.dp)
                .onGloballyPositioned { val t = it.boundsInParent().top.toInt(); if (it.size.height > 0 && t != speedTopPx) speedTopPx = t }
        ) {
            val baseSpeedDensity = LocalDensity.current
            // (sized by Settings > Screen & sizes > Speed limit and speedometer size, inside ImmSpeed)
            val speedSize = 1f
            CompositionLocalProvider(LocalDensity provides Density(baseSpeedDensity.density * speedSize, baseSpeedDensity.fontScale)) {
                // the car's battery level sits right above the speed, on the same side
                Column(horizontalAlignment = if (engine.mirrored) Alignment.Start else Alignment.End) {
                    if (Neon.on && !narrow) NeonQuestBox(engine)
                    EvNowChip(engine)
                    ImmSpeed(engine)
                    if (Neon.on && !narrow) NeonPlaceBox(engine)
                }
            }
        }
        // neon look: the key bar along the bottom (wide screens)
        if (Neon.on && !narrow && !previewing) NeonKeyBar(engine, Modifier.align(Alignment.BottomCenter))

        // ---- bottom left: hazards and the trip card
        if (!previewing && engine.arrival == null) {
            val tripBaseDensity = LocalDensity.current
            val tripCardScale = engine.prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).coerceIn(70, 150) / 100f
            Box(Modifier.align(side(Alignment.BottomStart)).then(if (narrow) Modifier.fillMaxWidth().sides(end = 16.dp) else Modifier).onSizeChanged { tripPx = it.height }
                .onGloballyPositioned { report(1, it.boundsInParent()) }) {
                CompositionLocalProvider(LocalDensity provides Density(tripBaseDensity.density * tripCardScale, tripBaseDensity.fontScale)) {
                    TripArea(engine, shareEta, narrow, voice)
                }
            }
        }
        else androidx.compose.runtime.SideEffect { if (tripPx != 0) tripPx = 0; engine.setPanel(1, 0f, 0f, 0f, 0f) }

        // ---- demo drive: its speed (tap to switch 1x, 2x, 4x), skip to the next turn, and stop
        if (engine.simMode != 0 && !previewing) Row(Modifier.align(Alignment.BottomCenter).padding(bottom = if (narrow && tripPx > 0) tripH + 10.dp else 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ImmPill("${engine.simMode}×", { engine.cycleSim() }, filled = true)
            ImmPill(Lang.t("Abbiegung", "Skip turn"), { engine.skipSimTurn() })
            ImmPill(Lang.t("Demo beenden", "End demo"), { engine.stopSim() })
        }
        // ---- bottom centre: re-centre once you have panned away, or zoomed in / out with the buttons (it puts the zoom back to automatic)
        AnimatedVisibility(
            !previewing && (!engine.following || kotlin.math.abs(engine.zoomOffset) > 0.01) && engine.arrival == null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (narrow && tripPx > 0) tripH + 10.dp else if (Neon.on && !narrow) 52.dp else 18.dp),
            enter = fadeIn(tween(200)) + androidx.compose.animation.scaleIn(tween(220), initialScale = 0.85f), exit = fadeOut(tween(150)) + androidx.compose.animation.scaleOut(tween(150), targetScale = 0.9f),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ImmPill(stringResource(R.string.ui_082), { engine.recenter(byUser = true) }, Icons.Rounded.MyLocation, filled = true)
                // zoomed with + and - while following you, with no route: keep this as your normal zoom
                if (!routing && engine.following && kotlin.math.abs(engine.zoomOffset) > 0.01) ImmPill(Lang.t("Als Standard verwenden", "Make this my default"), { engine.saveZoomAsDefault() }, Icons.Rounded.ZoomIn)
            }
        }

        // ---- top centre: the traffic view while you're stopped (it goes back by itself when you move off)
        // shown for 5 seconds when the traffic view comes on, then gone (the view stays until you move)
        var stoppedHint by remember { mutableStateOf(false) }
        LaunchedEffect(engine.stoppedOverview) {
            stoppedHint = engine.stoppedOverview
            if (engine.stoppedOverview) { delay(5000); stoppedHint = false }
        }
        // under the turn card when there's room; otherwise just above the trip or arrival card, not over it
        val turnDp = with(dens) { (if (routing && engine.turn != null) turnPx else 0).toDp() }
        val arrDp = with(dens) { (if (engine.arrival != null) arrPx else 0).toDp() }
        val pillTop = 16.dp + turnDp + 8.dp
        val pillUnder = turnDp > 0.dp && pillTop + 48.dp <= maxHeight * 0.45f
        val pillBottom = maxOf(if (engine.arrival != null) arrDp + 16.dp else tripH, 0.dp) + 10.dp
        AnimatedVisibility(stoppedHint && engine.stoppedOverview && !previewing,
            modifier = (if (pillUnder) Modifier.align(Alignment.TopCenter).padding(top = pillTop) else Modifier.align(Alignment.BottomCenter).padding(bottom = pillBottom)),
            enter = fadeIn(tween(300)), exit = fadeOut(tween(250))) {
            Row(Modifier.panel(6.dp, RoundedCornerShape(20.dp), c.surface).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Traffic, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.ui_060), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
        }

        if (previewing) RoutePreview(engine)
        // the arrival moment: the summary slides up from the bottom while the camera swoops down to the destination
        val curArr = engine.arrival
        val lastArr = remember { mutableStateOf(curArr) }
        if (curArr != null) lastArr.value = curArr
        AnimatedVisibility(
            visible = curArr != null,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
            enter = androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(520)) { it } + fadeIn(),
            exit = androidx.compose.animation.slideOutVertically { it } + fadeOut(),
        ) { lastArr.value?.let { a -> Box(Modifier.onSizeChanged { arrPx = it.height }) { ArrivalCard(engine, a) } } }

        engine.faster?.let { fr ->
            Row(
                // at the top, under the turn card, where it's seen at a glance
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 150.dp).widthIn(max = 540.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Good.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Route, null, tint = Good, modifier = Modifier.size(24.dp)) }
                Column {
                    Text(stringResource(R.string.ui_015), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(Lang.t("Spart etwa ${fr.savedMin} Min.", "Saves about ${fr.savedMin} min"), color = Good, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                ImmPill(Lang.t("Wechseln", "Switch"), { engine.acceptFaster() }, filled = true)
                ImmPill(Lang.t("Behalten", "Keep"), { engine.dismissFaster() })
            }
        }

        // passing another driver's shared report: "Stau noch da?" with Noch da / Weg (it goes by itself after 12 s; above the faster
        // route card when that is up too)
        engine.votePrompt?.takeIf { !previewing && engine.arrival == null }?.let { vp ->
            Row(
                Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = (if (narrow) tripH + 10.dp else 84.dp))
                    .widthIn(max = 540.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(Warn.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.NotificationsActive, null, tint = Warn, modifier = Modifier.size(22.dp)) }
                Text(Lang.t("${vp.name} noch da?", "${vp.name} still there?"), color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                ImmPill(Lang.t("Noch da", "Still there"), { engine.answerVote(true) }, filled = true)
                ImmPill(Lang.t("Weg", "Gone"), { engine.answerVote(false) })
                // closes the question without a vote
                Box(Modifier.size(34.dp).clip(CircleShape).pressClick { engine.dismissVote() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(18.dp))
                }
            }
        }

        AnimatedVisibility(engine.busy, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp), enter = fadeIn(tween(160)) + androidx.compose.animation.scaleIn(tween(200), initialScale = 0.7f), exit = fadeOut(tween(160))) {
            Box(Modifier.size(44.dp).panel(6.dp, CircleShape, c.surface), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = c.accent, strokeWidth = 3.dp)
            }
        }

        // ---- the options sheet slides in from the right; tapping the map closes it
        if (sheet && !previewing) Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { sheet = false },
        )
        AnimatedVisibility(
            visible = sheet && !previewing,
            modifier = Modifier.align(side(Alignment.CenterEnd)),
            enter = slideInHorizontally(initialOffsetX = { if (mir) -it else it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { if (mir) -it else it }) + fadeOut(),
        ) { OptionsSheet(engine, shareEta, wide = !narrow && maxWidth >= 860.dp && maxHeight < maxWidth, onSettings = { sheet = false; onSettings() }) { sheet = false } }
        if (engine.stopsEditor) StopsDialog(engine, onClose = { engine.stopsEditor = false })
        if (engine.chargerFilterOpen) ChargerFilterDialog(engine) { engine.chargerFilterOpen = false }
    }
}

/**
 * Map options: the map's look first (Standard or Satellite, Auto / Day / Night), then what's shown on it as tiles that light up when
 * on, then the trip tools, reports and things to find. In landscape on a wide screen it's twice as wide, in two columns, so nothing
 * needs scrolling; it still scrolls if a small screen needs it.
 */
@Composable
private fun OptionsSheet(engine: NavEngine, shareEta: () -> Unit, wide: Boolean = false, onSettings: () -> Unit = {}, close: () -> Unit) {
    val c = LocalVw.current
    val p = engine.prefs
    val routing = engine.route != null && !engine.previewing
    val shape = RoundedCornerShape(28.dp)
    Column(
        Modifier.fillMaxHeight().widthIn(max = if (wide) 760.dp else 410.dp).fillMaxWidth().padding(10.dp).shadow(16.dp, shape).clip(shape).background(c.panel)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_033), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ImmIcon(Icons.Rounded.Settings, onSettings, size = 42.dp, desc = Lang.t("Einstellungen", "Settings"))
            Spacer(Modifier.width(8.dp))
            ImmIcon(Icons.Rounded.Close, close, size = 42.dp, desc = Lang.t("Schließen", "Close"))
        }
        val mapPart: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // quick rows: the view and the sound (they were round buttons on the map)
                SheetHeading(Lang.t("Ansicht", "View"))
                Segments(listOf("2D", "3D"), if (engine.tilt3d) 1 else 0) { i -> if ((i == 1) != engine.tilt3d) engine.toggleTilt() }
                SheetHeading(Lang.t("Ton", "Sound"))
                Segments(listOf(Lang.t("Sprache", "Voice"), Lang.t("Nur Warnungen", "Alerts only"), Lang.t("Stumm", "Mute")), engine.soundMode) { i -> p.put(PreferenceKeys.SOUND_MODE, i) }
                SheetHeading(Lang.t("Karte", "Map"))
                // the map's look: two picture cards, then day and night
                val sat = p.b(PreferenceKeys.SATELLITE, false)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LookCard(Modifier.weight(1f), "Standard", false, !sat) { p.put(PreferenceKeys.SATELLITE, false) }
                    LookCard(Modifier.weight(1f), Lang.t("Satellit", "Satellite"), true, sat) { p.put(PreferenceKeys.SATELLITE, true) }
                }
                // Auto covers every automatic choice in Settings (headlights, sun times, your own times): tapping it while one of those
                // is on leaves it as it is
                val theme = p.s(PreferenceKeys.THEME, "auto")
                val seg = when (theme) { "day" -> 1; "night" -> 2; else -> 0 }
                Segments(listOf("Auto", Lang.t("Tag", "Day"), Lang.t("Nacht", "Night")), seg) { i -> if (i != seg) p.put(PreferenceKeys.THEME, listOf("auto", "day", "night")[i]) }
                SheetHeading(Lang.t("Auf der Karte", "On the map"))
                fun tog(key: String, def: Boolean): Pair<Boolean, () -> Unit> { val on = p.b(key, def); return on to { p.put(key, !on) } }
                val layers = listOf(
                    Triple(Icons.Rounded.Traffic, Lang.t("Verkehr", "Traffic"), tog(PreferenceKeys.TRAFFIC, true)),
                    Triple(Icons.Rounded.Apartment, Lang.t("3D-Gebäude", "3D buildings"), tog(PreferenceKeys.BUILDINGS_3D, true)),
                    Triple(Icons.Rounded.Traffic, Lang.t("Ampeln", "Traffic lights"), tog(PreferenceKeys.SIGNALS, true)),
                    Triple(Icons.Rounded.CameraAlt, Lang.t("Blitzer", "Cameras"), tog(PreferenceKeys.HAZARD_ICONS, true)),
                    Triple(Icons.Rounded.Lightbulb, Lang.t("Straßenlaternen", "Street lamps"), tog(PreferenceKeys.LAMPS, false)),
                    Triple(Icons.Rounded.Park, Lang.t("Bäume", "Trees"), tog(PreferenceKeys.TREES, true)),
                    Triple(Icons.Rounded.DirectionsWalk, Lang.t("Fußwege", "Footpaths"), tog(PreferenceKeys.PATHS, true)),
                    Triple(Icons.Rounded.WaterDrop, Lang.t("Regenradar", "Rain radar"), tog(PreferenceKeys.RAIN_RADAR, false)),
                    Triple(Icons.Rounded.AcUnit, Lang.t("Wetter", "Weather"), tog(PreferenceKeys.RAIN_EFFECTS, false)),
                )
                for (row in layers.chunked(3)) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((icon, label, st) in row) ToggleTile(Modifier.weight(1f), icon, label, st.first, st.second)
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        val tripPart: @Composable () -> Unit = {
            if (routing) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetHeading(Lang.t("Diese Fahrt", "This trip"))
                WideTiles(4, listOf(
                    TileSpec(Icons.Rounded.Add, if (engine.via.isEmpty()) Lang.t("Stopp hinzufügen", "Add stop") else Lang.t("Stopps", "Stops"), c.accent, { engine.stopsEditor = true; close() }),
                    TileSpec(Icons.Rounded.DoNotDisturbOn, Lang.t("Straße meiden", "Avoid road"), Warn, { engine.avoidRoadAhead(); close() }),
                    TileSpec(Icons.Rounded.Route, Lang.t("Übersicht", "Overview"), c.accent, { engine.overview(); close() }),
                    TileSpec(Icons.Rounded.Share, if (engine.tripShareUrl != null) Lang.t("Teilen beenden", "Stop sharing") else Lang.t("Ankunftszeit teilen", "Share ETA"), if (engine.tripShareUrl != null) Warn else Good, { shareEta(); close() }),
                ))
            }
        }
        val restPart: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetHeading(Lang.t("Hier melden", "Report here"))
                WideTiles(3, listOf(
                    TileSpec(Icons.Rounded.LocalPolice, Lang.t("Polizei", "Police"), Color(0xFF1E66F5), { engine.report("police"); close() }),
                    TileSpec(Icons.Rounded.CarCrash, Lang.t("Unfall", "Crash"), Bad, { engine.report("accident"); close() }),
                    TileSpec(Icons.Rounded.PriorityHigh, Lang.t("Gefahr", "Hazard"), Warn, { engine.report("hazard"); close() }),
                    TileSpec(Icons.Rounded.Build, Lang.t("Panne", "Broken down"), Color(0xFFFB8C00), { engine.report("breakdown"); close() }),
                    TileSpec(Icons.Rounded.CameraAlt, Lang.t("Mobiler Blitzer", "Mobile cam"), Color(0xFF1E66F5), { engine.report("mobilecam"); close() }),
                    TileSpec(Icons.Rounded.DirectionsCar, Lang.t("Stau", "Traffic jam"), Color(0xFFF4511E), { engine.report("jam"); close() }),
                    TileSpec(Icons.Rounded.Build, Lang.t("Baustelle", "Roadworks"), Color(0xFFFBC02D), { engine.report("roadworks"); close() }, picture = R.drawable.ic_roadworks_sign),
                    TileSpec(Icons.Rounded.Block, Lang.t("Gesperrt", "Road closed"), Bad, { engine.report("closed"); close() }),
                ), compact = true)
                // with no route the map page already has fuel, charging, food, coffee and parking, so only the rest are here
                fun find(kind: String): () -> Unit = { engine.loadNearby(kind, false); close() }
                val finds = listOfNotNull(
                    if (engine.route != null && !(engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV")) TileSpec(Icons.Rounded.LocalGasStation, Lang.t("Tanken", "Fuel"), Color(0xFFEA4335), find("fuel")) else null,
                    if (engine.route != null && engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") TileSpec(Icons.Rounded.EvStation, Lang.t("Laden", "Charging"), Color(0xFF34A853), find("charging")) else null,
                    if (engine.route != null) TileSpec(Icons.Rounded.Restaurant, Lang.t("Essen", "Food"), Color(0xFFF29900), find("food")) else null,
                    if (engine.route != null) TileSpec(Icons.Rounded.LocalCafe, Lang.t("Kaffee", "Coffee"), Color(0xFFA56BFF), find("coffee")) else null,
                    TileSpec(Icons.Rounded.Wc, Lang.t("Toiletten", "Toilets"), Color(0xFF4C8DFF), find("toilets")),
                    if (engine.route != null) TileSpec(Icons.Rounded.LocalParking, Lang.t("Parken", "Parking"), Color(0xFF4C8DFF), find("parking")) else null,
                    if (engine.route != null) TileSpec(Icons.Rounded.Apartment, Lang.t("Raststätten", "Services"), Color(0xFF12A4B4), find("services")) else null,
                    if (engine.dest != null) TileSpec(Icons.Rounded.Place, Lang.t("Parken am Ziel", "Park at end"), Color(0xFFEA4335), { engine.loadNearby("parking", true); close() }) else null,
                    // a motorhome or a car with a caravan (Settings > Fahrzeug)
                    if (engine.camperProfile) TileSpec(Icons.Rounded.RvHookup, Lang.t("Stellplätze", "Pitches"), Color(0xFF2E7D32), find("camp")) else null,
                    if (engine.camperProfile) TileSpec(Icons.Rounded.WaterDrop, Lang.t("Entsorgung", "Water & waste"), Color(0xFF0288D1), find("dump")) else null,
                    if (engine.camperProfile) TileSpec(Icons.Rounded.PropaneTank, Lang.t("Gas", "LPG"), Color(0xFFEF6C00), find("lpg")) else null,
                )
                SheetHeading(if (engine.route != null) Lang.t("Entlang der Route finden", "Find along the route") else Lang.t("In der Nähe finden", "Find nearby"))
                WideTiles(4, finds)
            }
        }
        if (wide) Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.weight(1f)) { mapPart() }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { tripPart(); restPart() }
        } else {
            // while navigating the trip tools come first; otherwise the map
            tripPart(); mapPart(); restPart()
        }
    }
}

/** A row of choices with the chosen one filled in blue (2D / 3D, sound, day and night). */
@Composable
private fun Segments(labels: List<String>, selected: Int, onPick: (Int) -> Unit) {
    val c = LocalVw.current
    Row(Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).background(c.tile).padding(3.dp)) {
        for ((i, label) in labels.withIndex()) {
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp)).background(if (i == selected) c.accent else Color.Transparent)
                .clickable { onPick(i) }, contentAlignment = Alignment.Center) {
                Text(label, color = if (i == selected) Color.White else c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
    }
}

private fun soundIcon(mode: Int): ImageVector = when (mode) { 0 -> Icons.Rounded.VolumeUp; 1 -> Icons.Rounded.NotificationsActive; else -> Icons.Rounded.VolumeOff }

/** Standard or Satellite, as a little picture with its name under it; outlined when it's the one in use. */
@Composable
private fun LookCard(modifier: Modifier, label: String, satellite: Boolean, selected: Boolean, onClick: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(if (selected) c.accent.copy(alpha = 0.18f) else c.tile)
        .border(if (selected) 2.dp else 0.dp, if (selected) c.accent else Color.Transparent, shape).pressClick(onClick)) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(54.dp)) {
            val w = size.width; val h = size.height
            if (satellite) {
                drawRect(Color(0xFF3E5A35))
                drawRect(Color(0xFF6B7F4A), topLeft = Offset(w * 0.08f, h * 0.12f), size = androidx.compose.ui.geometry.Size(w * 0.3f, h * 0.38f))
                drawRect(Color(0xFF7C6A4E), topLeft = Offset(w * 0.55f, h * 0.55f), size = androidx.compose.ui.geometry.Size(w * 0.35f, h * 0.3f))
                drawLine(Color(0xFFBDB6A6), Offset(0f, h * 0.72f), Offset(w, h * 0.38f), strokeWidth = h * 0.08f)
            } else {
                drawRect(if (c.dark) Color(0xFF2A3348) else Color(0xFFE9EDF3))
                val road = if (c.dark) Color(0xFF55607A) else Color.White
                drawLine(road, Offset(0f, h * 0.65f), Offset(w, h * 0.35f), strokeWidth = h * 0.1f)
                drawLine(road, Offset(w * 0.35f, 0f), Offset(w * 0.5f, h), strokeWidth = h * 0.07f)
                drawRect(if (c.dark) Color(0xFF2E4A3A) else Color(0xFFCDE8D2), topLeft = Offset(w * 0.62f, h * 0.6f), size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.3f))
            }
        }
        Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** A switch as a tile: lit up in blue when it's on. */
@Composable
private fun ToggleTile(modifier: Modifier, icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.clip(shape).background(if (on) c.accent.copy(alpha = 0.2f) else c.tile)
        .border(if (on) 1.5.dp else 0.dp, if (on) c.accent else Color.Transparent, shape).pressClick(onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, if (on) Lang.t("$label an", "$label on") else Lang.t("$label aus", "$label off"), tint = if (on) c.accent else c.sub, modifier = Modifier.size(24.dp))
        Text(label, color = if (on) c.text else c.sub, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
    }
}

/** Tiles in rows of [cols] that share the width evenly. [compact]: smaller, for a row of five. */
@Composable
private fun WideTiles(cols: Int, tiles: List<TileSpec>, compact: Boolean = false) {
    val c = LocalVw.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in tiles.chunked(cols)) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)) {
            for (t in row) Column(
                Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.tile).pressClick(t.onClick).padding(vertical = if (compact) 9.dp else 11.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(if (compact) 32.dp else 38.dp).clip(CircleShape).background(t.tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    if (t.picture != null) androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(t.picture), null, modifier = Modifier.size(if (compact) 24.dp else 28.dp))
                    else Icon(t.icon, null, tint = t.tint, modifier = Modifier.size(if (compact) 19.dp else 22.dp))
                }
                Text(t.label, color = c.text, fontSize = if (compact) 11.sp else 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Bottom left: hazard badges, then the trip card with the route bar, arrival, time and distance left, and the trip buttons. */
@Composable
private fun TripArea(engine: NavEngine, shareEta: () -> Unit, narrow: Boolean = false, voice: () -> Unit = {}) {
    val c = LocalVw.current
    val inc = engine.incidentAhead
    val cam = engine.cameraDist
    val eta = engine.eta
    val radarOn = engine.prefs.b(PreferenceKeys.HAZARDRADAR, false)
    val noCams = engine.camNoData
    if (inc == null && cam == null && eta == null && !noCams && (!radarOn || engine.radar.isEmpty())) return
    val mir = engine.mirrored
    Column(Modifier.padding(start = if (mir) 0.dp else 16.dp, end = if (mir) 16.dp else 0.dp, bottom = 16.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = if (mir) Alignment.End else Alignment.Start) {
        // no speed camera list here: the offline map of this area has them, Overpass not answering is the usual reason
        if (noCams) Text(Lang.t("Keine Blitzerdaten: Offline-Karte dieser Gegend laden", "No camera data: download this area's offline map"),
            color = c.sub, fontSize = 13.sp, modifier = Modifier.background(c.tile, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
        // hazard radar: the next two miles at a glance (replaces the separate camera and incident badges)
        if (radarOn) HazardRadar(engine)
        else if ((inc != null && engine.alert?.kind != "incident") || (cam != null && engine.alert?.kind != "camera")) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (inc != null && engine.alert?.kind != "incident") HazardBadge("incident", inc.label, Geo.fmtDist(inc.dist, engine.imperial))
            if (cam != null && engine.alert?.kind != "camera") HazardBadge("camera", Lang.t("Blitzer", "Speed camera"), Geo.fmtDist(cam.toDouble(), engine.imperial))
        }
        eta?.let { e ->
            val dc = if (e.delayMin >= 10) Bad else if (e.delayMin >= 1) Warn else Good
            val shape = RoundedCornerShape(22.dp)
            // landscape: the card is only as wide as its contents (figures, then the buttons close by), and the route bar matches that width.
            // portrait: full width, with the buttons under the figures.
            val dens = androidx.compose.ui.platform.LocalDensity.current
            var rowPx by remember { mutableStateOf(0) }
            val rowW = with(dens) { rowPx.toDp() }
            Column(Modifier.then(if (narrow) Modifier.fillMaxWidth() else Modifier).panel(10.dp, shape, c.surface).animateContentSize()) {
                val barMod = if (narrow) Modifier.fillMaxWidth() else Modifier.width(rowW)
                // the route bar across the top of the card, with room to breathe
                if (engine.showRouteBar && (narrow || rowPx > 0)) {
                    RouteBar(engine, barMod.padding(start = if (narrow) 16.dp else 18.dp, end = if (narrow) 16.dp else 18.dp, top = if (narrow) 7.dp else 9.dp, bottom = if (narrow) 2.dp else 4.dp))
                    Box(barMod.padding(horizontal = 18.dp).height(1.dp).background(c.line))
                }
                // electric: roughly how much battery you'll arrive with (amber under 20%, red under 10%)
                val evPct = engine.evArrivalPct()
                val evCol = if (evPct == null) c.text else if (evPct < 10) Bad else if (evPct < 20) Warn else Good
                val stats: @Composable () -> Unit = {
                    ImmStat(Geo.fmtDur(e.secs), if (e.delayMin >= 1) Lang.t("+${e.delayMin} Min. Verkehr", "+${e.delayMin} min traffic") else Lang.t("Restzeit", "time left"), dc)
                    ImmStat(e.arrive, Lang.t("Ankunft", "arrival"), c.text)
                    ImmStat(Geo.fmtDist(e.dist, engine.imperial), Lang.t("verbleibend", "to go"), c.text)
                    if (evPct != null) ImmStat("$evPct%", Lang.t("Akku bei Ankunft", "battery at arrival"), evCol)
                }
                val buttons: @Composable () -> Unit = {
                    // electric: chargers along the route from here
                    if (evPct != null) ImmCircle({ engine.loadNearby("charging", false) }, 40.dp) { Icon(Icons.Rounded.EvStation, Lang.t("Ladesäulen voraus", "Chargers ahead"), tint = Good, modifier = Modifier.size(20.dp)) }
                    ImmCircle({ voice() }, 40.dp) { Icon(Icons.Rounded.Mic, Lang.t("Sprache", "Voice"), tint = c.text, modifier = Modifier.size(20.dp)) }
                    ImmCircle({ engine.cycleSound() }, 40.dp) { Icon(soundIcon(engine.soundMode), Lang.t("Ton", "Sound"), tint = if (engine.soundMode == 2) Bad else c.text, modifier = Modifier.size(20.dp)) }
                    ImmCircle({ engine.stopsEditor = true }, 40.dp) { Icon(Icons.Rounded.Add, Lang.t("Stopps", "Stops"), tint = c.text, modifier = Modifier.size(21.dp)) }
                    Box(Modifier.size(40.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Bad).pressClick { engine.endRoute() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, Lang.t("Route beenden", "End route"), tint = Color.White, modifier = Modifier.size(21.dp))
                    }
                }
                if (!narrow) Row(
                    Modifier.widthIn(min = 360.dp).onSizeChanged { rowPx = it.width }.padding(start = 18.dp, end = 12.dp, top = 9.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    stats()
                    Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { buttons() }
                } else Column(
                    // portrait: the figures get the full width (time and arrival never clipped), the buttons sit in a row under them
                    Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 7.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        ImmStat(Geo.fmtDur(e.secs), if (e.delayMin >= 1) Lang.t("+${e.delayMin} Min.", "+${e.delayMin} min") else Lang.t("Restzeit", "time left"), dc, compact = true)
                        ImmStat(e.arrive, Lang.t("Ankunft", "arrival"), c.text, compact = true)
                        ImmStat(Geo.fmtDist(e.dist, engine.imperial), Lang.t("verbleibend", "to go"), c.text, compact = true)
                        if (evPct != null) ImmStat("$evPct%", Lang.t("Akku", "battery"), evCol, compact = true)
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (evPct != null) ImmCircle({ engine.loadNearby("charging", false) }, 36.dp) { Icon(Icons.Rounded.EvStation, Lang.t("Ladesäulen voraus", "Chargers ahead"), tint = Good, modifier = Modifier.size(19.dp)) }
                        ImmCircle({ voice() }, 36.dp) { Icon(Icons.Rounded.Mic, Lang.t("Sprache", "Voice"), tint = c.text, modifier = Modifier.size(19.dp)) }
                        ImmCircle({ engine.cycleSound() }, 36.dp) { Icon(soundIcon(engine.soundMode), Lang.t("Ton", "Sound"), tint = if (engine.soundMode == 2) Bad else c.text, modifier = Modifier.size(19.dp)) }
                        ImmCircle({ engine.stopsEditor = true }, 36.dp) { Icon(Icons.Rounded.Add, Lang.t("Stopps", "Stops"), tint = c.text, modifier = Modifier.size(20.dp)) }
                        Box(Modifier.size(36.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Bad).pressClick { engine.endRoute() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, Lang.t("Route beenden", "End route"), tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImmCompass(engine: NavEngine, modifier: Modifier = Modifier) {
    val c = LocalVw.current
    Box(
        (if (LocalInRail.current) Modifier.size(52.dp) else Modifier.size(52.dp).panel(6.dp, CircleShape, c.surface)).then(modifier).pressClick { engine.toggleNorthUp() },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(width = 4.dp, height = 8.dp).clip(RoundedCornerShape(2.dp)).background(Bad))
            Text("N", color = if (engine.northUp) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The electric car's battery level, above the speed: green, amber under 20 %, red under 10 %. It shows its age once it's
 * more than 10 minutes old, and turns amber then too after an hour. Hidden without a reading.
 */
@Composable
internal fun EvNowChip(engine: NavEngine) {
    // the estimate, as on the car screen: the last reading moved on by what has been driven or charged since
    if (com.headunit.launcher.EvWidget.readAt(engine.prefs) <= 0) return
    val pct = Math.round(engine.evNowEstimatePct()).toInt()
    val km = engine.evEstimateKm()
    val age = engine.evNowAgeMin() ?: 0L
    val col = if (pct < 10) Bad else if (pct < 20) Warn else Good
    Row(
        Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(50)).background(Color(0xCC0B0F16)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(col))
        Text((if (engine.evEstimateOld()) "ca. " else "") + "$pct %", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        km?.let { Text("· $it km", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        // charging, from the car maker's cloud (only while that reading is recent)
        if (engine.prefs.s(PreferenceKeys.CLOUD_CHARGING, "") == "1" && System.currentTimeMillis() - (engine.prefs.s(PreferenceKeys.CLOUD_AT, "").toLongOrNull() ?: 0L) < 30 * 60_000L) {
            val m = engine.prefs.s(PreferenceKeys.CLOUD_CHARGE_MIN, "").toIntOrNull()
            Text("· " + Lang.t("lädt", "charging") + (m?.let { " · $it min" } ?: ""), color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        } else if (engine.evCharging()) {
            // standing at a charger with a rising reading (see NavEngine.evCharging)
            Text("· " + Lang.t("lädt", "charging"), color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        if (age >= 10) Text("· $age Min.", color = if (age > 60) Warn else Color(0xFFC3CAD5), fontSize = 12.sp)
    }
}

/** Speed limit sign beside a large readout of your current speed. */
@Composable
internal fun ImmSpeed(engine: NavEngine) {
    // the size from Settings (100% = the standard size): it's laid out at that size, so it keeps clear of the edges and other cards
    val pct = engine.prefs.i(PreferenceKeys.SPEED_SIZE, 100).coerceIn(50, 200)
    if (pct == 100) { SpeedBlock(engine); return }
    val base = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.density * pct / 100f, base.fontScale),
    ) { SpeedBlock(engine) }
}

@Composable
private fun SpeedBlock(engine: NavEngine) {
    val c = LocalVw.current
    val show = engine.prefs.b(PreferenceKeys.MAPSPEED, true)
    val lim = engine.speedLimit
    // a sign to show: a limit, or the "no limit" sign of an unrestricted Autobahn
    val sign = lim != null || engine.noLimit
    if (!sign && !show) return
    val k = if (engine.imperial) 2.23694 else 3.6
    if (engine.prefs.b(PreferenceKeys.SPEEDRING, true)) { SpeedRing(engine, show, k); return }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Keep the speed cluster in the same screen position, but mirror the sign's
        // relationship to the speed readout for left-hand-drive layouts.
        if (show && engine.mirrored && !engine.isCar && sign) SpeedLimitSign(engine, 70.dp)
        if (show) Column(
            Modifier.width(78.dp).panel(8.dp, RoundedCornerShape(24.dp), c.surface).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${Math.round(engine.shownSpeed * k)}", color = speedTint(engine) ?: c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 12.sp)
        }
        if (sign && !(show && engine.mirrored && !engine.isCar)) SpeedLimitSign(engine, 70.dp)
    }
}

/**
 * Speedometer ring: a glowing arc that fills towards the limit and warms to amber, then red, as you near or pass it,
 * with your speed in the middle and the limit sign tucked against it.
 */
@Composable
private fun SpeedRing(engine: NavEngine, show: Boolean, k: Double) {
    val c = LocalVw.current
    val lim = engine.speedLimit
    val sp = engine.shownSpeed
    // the ring is full at 20% over the limit; with no limit (an Autobahn) at 200 km/h, or 70 mph where miles are used
    val full = if (lim != null && lim > 0) lim * 1.2 else if (engine.imperial) 31.3 else 55.6
    val target = (sp / full).toFloat().coerceIn(0f, 1f)
    val frac by androidx.compose.animation.core.animateFloatAsState(target, androidx.compose.animation.core.tween(350), label = "ring")
    val ratio = if (lim != null && lim > 0 && !engine.limitEstimated) (sp / lim).toFloat() else 0f
    val col = when {
        // blue up to the limit (it used to turn amber and red from 90%, so 28 in a 30 zone looked like speeding);
        // amber just over it, red from 5% over
        engine.overLimit || ratio > 1.04f -> Bad
        ratio > 1.0f -> Warn
        else -> c.accent
    }
    val arcCol by androidx.compose.animation.animateColorAsState(col, androidx.compose.animation.core.tween(400), label = "ringCol")
    Box(contentAlignment = Alignment.Center) {
        if (show) Box(Modifier.size(104.dp).panel(10.dp, CircleShape, c.surface), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val sw = 7.dp.toPx()
                val inset = sw / 2 + 4.dp.toPx()
                val tl = Offset(inset, inset)
                val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                val cap = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawArc(c.tile, 135f, 270f, false, tl, sz, style = cap)
                if (frac > 0.005f) {
                    // a soft glow under the arc, then the arc itself
                    drawArc(arcCol.copy(alpha = 0.28f), 135f, 270f * frac, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw * 2.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(arcCol, 135f, 270f * frac, false, tl, sz, style = cap)
                }
                // a tick where the limit sits on the ring
                if (lim != null && lim > 0) {
                    val ang = Math.toRadians(135.0 + 270.0 * (lim / full).coerceIn(0.0, 1.0))
                    val r0 = sz.width / 2 - sw; val r1 = sz.width / 2 + sw
                    val cx = size.width / 2; val cy = size.height / 2
                    drawLine(c.text.copy(alpha = 0.7f), Offset(cx + (kotlin.math.cos(ang) * r0).toFloat(), cy + (kotlin.math.sin(ang) * r0).toFloat()),
                        Offset(cx + (kotlin.math.cos(ang) * r1).toFloat(), cy + (kotlin.math.sin(ang) * r1).toFloat()), 2.5.dp.toPx())
                }
            }
            // the number sits exactly in the middle of the ring; the unit goes in the gap at the bottom of the arc
            val num = "${Math.round(sp * k)}"
            Text(num, color = speedTint(engine) ?: c.text, fontSize = if (num.length >= 3) 27.sp else 33.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false), lineHeight = 33.sp,
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
            Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp))
        }
        // The limit sign goes on the speed's inner side, so it never runs off the screen: the left normally, the right when the
        // app is mirrored for left-hand drive (the speed is then on the left edge). Android Auto never mirrors: its speed is
        // always on the right.
        val signRight = engine.mirrored && !engine.isCar
        if (lim != null || engine.noLimit) Box(
            Modifier.align(if (!show) Alignment.Center else if (signRight) Alignment.TopEnd else Alignment.TopStart)
                // beside the ring's upper corner, just clear of it (raised over the top it covered the battery chip above and the ring)
                .offset(x = if (!show) 0.dp else if (signRight) 48.dp else (-48).dp, y = 0.dp)
        ) {
            // (on the car, a little bigger beside the speedometer: it's read at a glance)
            SpeedLimitSign(engine, if (show) (if (engine.isCar) (if (engine.carShort) 42.dp else 58.dp) else 50.dp) else 70.dp)
        }
    }
}

@Composable
internal fun ImmTurnCard(
    engine: NavEngine, t: com.headunit.launcher.TurnInfo, compact: Boolean = false,
    /** Android Auto: no written lane advice under the lane arrows (the arrows already say it). */
    car: Boolean = false,
    /** Android Auto with the turn still far off: just one line (arrow, distance, road), so more of the map shows. */
    slim: Boolean = false,
    /** Extra row along the bottom of the card (Android Auto puts the hazards ahead there). */
    footer: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(28.dp)
    val frac = ((450.0 - t.dist) / 450.0).coerceIn(0.0, 1.0).toFloat()
    // living card: it grows a touch and starts to glow as the turn comes up, and pulses in the last 80 m
    val alive = engine.prefs.b(PreferenceKeys.LIVECARD, true)
    val near = alive && t.dist < 300.0
    val grow by androidx.compose.animation.core.animateFloatAsState(if (near) 1f + 0.035f * frac else 1f, androidx.compose.animation.core.tween(400), label = "grow")
    val glow by androidx.compose.animation.core.animateFloatAsState(if (near) (0.25f + 0.55f * frac) else 0f, androidx.compose.animation.core.tween(500), label = "glow")
    // only runs in the last 80 m, so the screen isn't redrawn constantly the rest of the time
    // read only while drawing, so the pulse repaints the card without recomposing it 60 times a second
    val pulse = rememberPulse(alive && t.dist < 80.0, 650)
    val glowCol = Color(0xFF7FFFE6)
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = grow; scaleY = grow; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f) }
            .shadow((if (compact) 5f else 12f + 14f * glow).dp, shape, ambientColor = glowCol, spotColor = glowCol)
            .clip(shape)
            .background(androidx.compose.ui.graphics.lerp(Teal, Color(0xFF14C9A6), 0.35f * glow))
            .border(1.5.dp, Color.White.copy(alpha = 0.18f + 0.25f * glow), shape)
            .drawWithContent {
                drawContent()
                val pv = pulse.value
                if (pv > 0.01f) {
                    drawRect(Color.White.copy(alpha = 0.10f * pv))
                    val sw = (1.5f + 1.5f * pv) * density
                    drawRoundRect(Color.White.copy(alpha = 0.45f * glow * pv), topLeft = Offset(sw / 2, sw / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                }
            }
            .animateContentSize()
            // tap for the list of every turn on the route
            .clickable { engine.itineraryOpen = true },
    ) {
        // (in the car as tall as Android Auto's own buttons beside it, "Beenden", with text to match)
        if (slim) Row(Modifier.padding(horizontal = if (car) 20.dp else 14.dp, vertical = if (car) 14.dp else 9.dp), verticalAlignment = Alignment.CenterVertically) {
            ManeuverArrow(t.maneuver, Geo.maneuverAngle(t.maneuver), Modifier.size(if (car) 50.dp else 34.dp), roundaboutExit = t.exit, roundaboutClockwise = engine.leftTraffic, exitAngle = roundaboutTurn(t.rbIn, t.rbOut), otherExits = t.rbExits.map { roundaboutTurn(t.rbIn, it) })
            RollText(Geo.fmtDist(t.dist, engine.imperial), Color.White, if (car) 36.sp else 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (car) 14.dp else 10.dp))
            Text(t.street, color = Color.White, fontSize = if (car) 26.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
        } else {
        val thenIcon = t.thenManeuver != null && engine.prefs.s(PreferenceKeys.THEN_STYLE, "row") == "icon"
        // Android Auto: the full card as big as the slim one (text as large as the car's own "Beenden" button beside it)
        val distSp = if (car) 44.sp else if (compact) 28.sp else 38.sp
        val streetSp = if (car) 24.sp else if (compact) 16.sp else 19.sp
        val chipSp = if (car) 17.sp else 13.sp
        Row(Modifier.padding(start = if (car) 20.dp else if (compact) 12.dp else 18.dp, end = if (compact && !car) 12.dp else 16.dp, top = if (car) 14.dp else if (compact) 9.dp else 16.dp, bottom = if (car) 12.dp else if (compact) 8.dp else 12.dp), verticalAlignment = Alignment.CenterVertically) {
            ManeuverArrow(t.maneuver, Geo.maneuverAngle(t.maneuver), Modifier.size(if (car) 64.dp else if (compact) 42.dp else 60.dp), roundaboutExit = t.exit, roundaboutClockwise = engine.leftTraffic, exitAngle = roundaboutTurn(t.rbIn, t.rbOut), otherExits = t.rbExits.map { roundaboutTurn(t.rbIn, it) })
            Column(Modifier.padding(start = if (car) 16.dp else if (compact) 9.dp else 16.dp).weight(1f)) {
                RollText(Geo.fmtDist(t.dist, engine.imperial), Color.White, distSp, fontWeight = FontWeight.Bold)
                // (the tag of a motorway exit is "Ausfahrt 17", from Geo.stepTag; older builds wrote "Exit 17")
                val exitTag = t.tag.startsWith("Exit") || t.tag.startsWith("Ausfahrt ")
                val ukSign = engine.prefs.b(PreferenceKeys.UKSIGNS, true) && (t.sign.isNotBlank() && t.sign != t.street || exitTag)
                // a motorway junction's name takes the tag's place: "Frankfurter Kreuz", "Ausfahrt 17 Weiterstadt" (just the name when the sign
                // below already carries the exit number)
                val jctChip = if (t.junction.isBlank() || t.maneuver.uppercase().startsWith("ROUNDABOUT")) "" else if (ukSign && exitTag) t.junctionName else t.junction
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (jctChip.isNotBlank()) Text(jctChip, color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 190.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    else if (t.tag.isNotBlank() && !(ukSign && exitTag)) Text(t.tag, color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    if (!ukSign && t.sign.isNotBlank() && t.sign != t.street) {
                        // the places on the sign, in the colour of the road they're on: blue Autobahn, yellow Bundes-/Landes-/Kreisstraße, white otherwise
                        val sc = signColors(t.sign + " " + t.street, t.exitNo.isNotBlank() || exitTag)
                        Text(t.sign, color = sc.second, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp).clip(RoundedCornerShape(6.dp)).background(sc.first).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                    Text(t.street, color = Color.White, fontSize = streetSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // the turn straight after, as a small arrow (the "Then" line below is used instead when that style is chosen)
            if (thenIcon && t.thenManeuver != null) Column(Modifier.padding(start = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TealDeep), contentAlignment = Alignment.Center) {
                    ManeuverArrow(t.thenManeuver, Geo.maneuverAngle(t.thenManeuver), Modifier.size(28.dp), roundaboutExit = t.thenExit, roundaboutClockwise = engine.leftTraffic)
                }
                Text(Lang.t("dann", "then"), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
        }
        // Explicit slip-road / motorway-exit cue. Lane guidance alone can be ambiguous on
        // fast roads, so make the exit manoeuvre visible in the turn card as well.
        val maneuverUpper = t.maneuver.uppercase()
        val exitTag = t.tag.startsWith("Exit", ignoreCase = true) || t.tag.startsWith("Ausfahrt ")
        val exitLike = t.exitNo.isNotBlank() ||
            exitTag ||
            maneuverUpper.contains("EXIT") ||
            maneuverUpper.contains("OFF_RAMP") ||
            maneuverUpper == "TAKE_EXIT" ||
            maneuverUpper.startsWith("MOTORWAY_EXIT")
        // Only when the UK road sign below isn't shown: it already carries the exit, and the same thing twice on the card is clutter.
        val signShown = engine.prefs.b(PreferenceKeys.UKSIGNS, true) && (t.sign.isNotBlank() && t.sign != t.street || exitTag)
        if (exitLike && !signShown && !maneuverUpper.startsWith("ROUNDABOUT")) {
            val exitNum = t.exitNo.ifBlank {
                t.tag.replaceFirst(Regex("^(Exit|Ausfahrt)\\s*", RegexOption.IGNORE_CASE), "").trim().takeIf { it.isNotBlank() }.orEmpty()
            }
            // a plain exit's name goes with its number ("AUSFAHRT  17 Weiterstadt"); an interchange's name is on the chip above
            val exitLabel = if (t.junctionName.isNotBlank() && !Junctions.isInterchange(t.junctionName))
                t.junction.replaceFirst(Regex("^(Exit|Ausfahrt)\\s*", RegexOption.IGNORE_CASE), "").trim() else exitNum
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = if (compact) 12.dp else 16.dp, end = if (compact) 12.dp else 16.dp, bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1554A8))
                    .padding(horizontal = 10.dp, vertical = if (compact) 5.dp else 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // a drawn arrow, as tall as the text (the "↗" character came out tiny in the car), pointing to the side the exit is on
                val exitIcon = if (maneuverUpper.contains("LEFT")) androidx.compose.material.icons.Icons.Rounded.NorthWest else androidx.compose.material.icons.Icons.Rounded.NorthEast
                val exitIconSize = if (car) 34.dp else if (compact) 24.dp else 28.dp
                androidx.compose.material3.Icon(exitIcon, null, tint = Color.White, modifier = Modifier.size(exitIconSize))
                Text(
                    if (exitLabel.isNotBlank()) Lang.t("AUSFAHRT  $exitLabel", "EXIT  $exitLabel") else Lang.t("AUSFAHRT", "EXIT / SLIP ROAD"),
                    color = Color.White,
                    fontSize = if (car) 21.sp else if (compact) 13.sp else 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 7.dp)
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.Icon(exitIcon, null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(exitIconSize))
            }
        }
        // A proper UK direction sign: blue for motorways, green for primary roads, white for local roads.
        if (signShown)
            UkSign(t.sign, (t.exitNo.ifBlank { t.tag.removePrefix("Exit ").removePrefix("Ausfahrt ") }).takeIf { exitTag }.orEmpty(), t.street, Modifier.padding(start = if (compact) 12.dp else 16.dp, end = if (compact) 12.dp else 16.dp, bottom = if (compact) 5.dp else 12.dp))
        if (!compact && engine.prefs.b(PreferenceKeys.LANE_STRIP, true)) t.lanes?.let { ls ->
            // lane hero view: before a complex junction the lanes grow into a big picture with yours lit; simple ones keep the strip
            // (not in Android Auto: the big lane picture made the card grow to fill much of a car screen as soon as a junction came up)
            val hero = !car && engine.prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero" && ls.lanes.size >= 2 && t.dist < 900.0
            if (hero) LaneHero(ls, t.maneuver, if (car) null else t.laneAdvice)
            else Box(Modifier.fillMaxWidth().background(TealDeep).padding(vertical = 4.dp), contentAlignment = Alignment.Center) { LaneRow(ls, t.maneuver, t.dist < 400.0) }
        }
        val heroShown = !car && engine.prefs.b(PreferenceKeys.LANE_STRIP, true) && engine.prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero" && (t.lanes?.lanes?.size ?: 0) >= 2 && t.dist < 900.0
        if (!compact && !heroShown && !(car && t.lanes != null && engine.prefs.b(PreferenceKeys.LANE_STRIP, true))) t.laneAdvice?.let { Text(it, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().background(TealDeep).padding(horizontal = 18.dp, vertical = 6.dp)) }
        if (!compact && t.thenManeuver != null && !thenIcon) {
            Row(Modifier.fillMaxWidth().background(TealDeep).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ManeuverArrow(t.thenManeuver, Geo.maneuverAngle(t.thenManeuver), Modifier.size(22.dp), roundaboutExit = t.thenExit, roundaboutClockwise = engine.leftTraffic)
                Text(t.thenText, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
            }
        }
        } // (end of the full card)
        footer?.invoke()
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.Black.copy(alpha = 0.18f))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Color.White.copy(alpha = 0.9f)))
        }
    }
}

@Composable
private fun ImmSearch(engine: NavEngine, q: String, onQ: (String) -> Unit, locked: Boolean, focus: FocusRequester, voice: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(30.dp)
    Box(Modifier.fillMaxWidth().height(60.dp).panel(10.dp, shape, c.surface)) {
        TextField(
            value = q, onValueChange = onQ, enabled = !locked,
            placeholder = { Text(if (locked) stringResource(R.string.ui_081) else stringResource(R.string.ui_064), fontSize = if (locked) 15.sp else 18.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.sub) },
            trailingIcon = {
                if (q.isNotEmpty()) Icon(Icons.Rounded.Close, Lang.t("Löschen", "Clear"), tint = c.sub, modifier = Modifier.padding(end = 8.dp).clickable { onQ(""); engine.results = emptyList() })
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    // folds Saved & recent up under the bar, and back down again
                    val open = engine.prefs.b(PreferenceKeys.QUICKLIST, true)
                    Box(Modifier.padding(end = 4.dp).size(40.dp).clip(CircleShape).clickable { engine.prefs.put(PreferenceKeys.QUICKLIST, !open) }, contentAlignment = Alignment.Center) {
                        Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (open) Lang.t("Gespeichert & zuletzt ausblenden", "Hide saved and recent") else Lang.t("Gespeichert & zuletzt einblenden", "Show saved and recent"), tint = c.sub, modifier = Modifier.size(26.dp))
                    }
                    Box(Modifier.padding(end = 8.dp).size(44.dp).clip(CircleShape).background(c.accent).pressClick { voice() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Mic, Lang.t("Sprachsuche", "Voice search"), tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (engine.stopMode && engine.route != null) engine.searchAlong(q) else engine.search(q) }),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, disabledContainerColor = c.surface,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                focusedTextColor = c.text, unfocusedTextColor = c.text, disabledTextColor = c.sub,
                focusedPlaceholderColor = c.sub, unfocusedPlaceholderColor = c.sub, disabledPlaceholderColor = c.sub,
                cursorColor = c.accent,
            ),
            modifier = Modifier.fillMaxSize().focusRequester(focus),
        )
    }
}

/** The card for a place tapped on the phone's map: its name and address, "Los", and "Als Stopp" when there is a trip to add it to. */
@Composable
private fun PhonePickCard(engine: NavEngine) {
    val p = engine.mapPick ?: return
    val c = LocalVw.current
    val hasTrip = engine.dest != null && engine.route != null
    val dist = engine.fix?.pos?.let { Geo.fmtDist(Geo.dist(it, p.pos), engine.imperial) }
    Column(Modifier.widthIn(max = 420.dp).panel(12.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaceLogo(engine, p)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(p.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(dist, p.sub.ifBlank { null }).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HoursLine(p)
            }
            Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { engine.clearPick() }.padding(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Lang.t("Los", "Go"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.accent).clickable { engine.routePick() }.padding(vertical = 11.dp))
            if (hasTrip) Text(Lang.t("Als Stopp", "Add as stop"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { engine.stopPick() }.padding(vertical = 10.dp))
        }
    }
}

/** "Geöffnet bis 20:00" in green or "Geschlossen · öffnet 08:00" in red, when the search gave the place's opening times. */
@Composable
internal fun HoursLine(p: com.headunit.launcher.Place) {
    val h = p.hours ?: return
    Text(h, color = if (com.headunit.launcher.OpenHours.isOpen(h)) Good else Bad, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A shop's logo in a round tile when it is on the unit, else the usual red pin. */
@Composable
internal fun PlaceLogo(engine: NavEngine, p: com.headunit.launcher.Place, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val c = LocalVw.current
    @Suppress("UNUSED_VARIABLE") val rev = engine.logoRev
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val logo = remember(p, engine.logoRev) { com.headunit.launcher.BrandLogos.domainFor(p)?.let { com.headunit.launcher.BrandLogos.cached(ctx, it) } }
    Box(Modifier.size(size).clip(CircleShape).background(if (logo != null) Color.White else c.tile), contentAlignment = Alignment.Center) {
        if (logo != null) androidx.compose.foundation.Image(logo.asImageBitmap(), null, Modifier.size(size * 0.62f))
        else Icon(Icons.Rounded.Place, null, tint = Color(0xFFEA4335), modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
private fun ImmResults(engine: NavEngine, onPicked: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(26.dp)
    val here = engine.fix?.pos
    // a shop or chain with several branches nearby: "Alle N Standorte" under its first result
    val chainRows = remember(engine.results) {
        val seen = HashSet<String>()
        engine.results.mapNotNull { p ->
            val k = com.headunit.launcher.BrandLogos.key(p.brand ?: p.name)
            if (k in seen) null else { seen += k; val n = engine.chainCount(p); if (n >= 2) p to n else null }
        }.toMap()
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).panel(10.dp, shape, c.surface).padding(vertical = 6.dp)) {
        items(engine.results) { p ->
            Row(Modifier.fillMaxWidth().clickable { onPicked(); engine.pickPlace(p) }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaceLogo(engine, p)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(p.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.sub, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HoursLine(p)
                }
                if (here != null) Text(Geo.fmtDist(Geo.dist(here, p.pos), engine.imperial), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
            chainRows[p]?.let { n ->
                Row(Modifier.fillMaxWidth().clickable { engine.showAllLocations(p) }.padding(start = 72.dp, end = 16.dp, top = 0.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.LocationOn, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(Lang.t("Alle $n Standorte anzeigen", "See all $n locations"), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/**
 * Every branch of a shop or chain: sorted by Quickest (drive time with today's traffic), Nearest or On my route, each with its logo.
 * The map shows them all. Tap a row (or a pin) to choose it, then Go or Add as a stop. "Search this area" looks again where the map is.
 */
@Composable
private fun ChainPanel(engine: NavEngine, onDone: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(26.dp)
    val ch = engine.chain
    val onRoute = engine.route != null && !engine.previewing
    Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).panel(10.dp, shape, c.surface).padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ch?.name ?: Lang.t("Suche …", "Searching …"), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (engine.chainBusy) Lang.t("Standorte werden gesucht …", "Finding locations …") else Lang.t("${ch?.locs?.size ?: 0} Standorte", "${ch?.locs?.size ?: 0} locations"), color = c.sub, fontSize = 13.sp)
            }
            Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { engine.closeChain() }.padding(8.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sorts = listOf("quick" to Lang.t("Schnellste", "Quickest"), "near" to Lang.t("Nächste", "Nearest")) + (if (onRoute) listOf("route" to Lang.t("Auf meiner Route", "On my route")) else emptyList())
            for ((id, label) in sorts) {
                val sel = engine.chainSort == id
                Text(label, color = if (sel) Color.White else c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) c.accent else c.tile).clickable { engine.chainSort = id }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Text(Lang.t("In diesem Bereich suchen", "Search this area"), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { engine.chainSearchHere() }.padding(horizontal = 14.dp, vertical = 7.dp))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(engine.chainSorted()) { l ->
                val sel = engine.chainSel === l
                Column(Modifier.fillMaxWidth().background(if (sel) c.accent.copy(alpha = 0.12f) else Color.Transparent).clickable { engine.selectChain(if (sel) null else l) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaceLogo(engine, l.place, 38.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(l.place.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val extra = if (engine.chainSort == "route") l.offRoute?.let { Lang.t(" · ${Geo.fmtDist(it, engine.imperial)} von der Route", " · ${Geo.fmtDist(it, engine.imperial)} off route") }.orEmpty() else ""
                            Text(l.place.sub + extra, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            HoursLine(l.place)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                            val secs = l.secs
                            if (secs != null) Text(Geo.fmtDur(secs), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(Geo.fmtDist(l.dist, engine.imperial), color = c.sub, fontSize = 13.sp)
                        }
                    }
                    if (sel) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Lang.t("Los", "Go"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.accent).clickable { val p = l.place; engine.closeChain(); engine.results = emptyList(); onDone(); engine.startRoute(p) }.padding(vertical = 11.dp))
                        if (onRoute) Text(Lang.t("Als Stopp", "Add as stop"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { val p = l.place; engine.closeChain(); engine.results = emptyList(); onDone(); engine.addStop(p) }.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImmChip(icon: ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.height(46.dp).panel(6.dp, shape, c.surface).pressClick(onClick).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Under the search bar: Home and Work as two tiles with the drive time, then five round buttons to find things nearby. */
@Composable
private fun ImmChips(engine: NavEngine, focusSearch: () -> Unit) {
    val home = engine.places.slot("home")
    val work = engine.places.slot("work")
    // the drive times: the engine throttles this itself (online only, every 10 minutes or after moving 3 km), so a minute's tick is cheap
    LaunchedEffect(Unit) { while (true) { engine.refreshSlotTimes(); delay(60_000) } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaceTile(engine, Modifier.weight(1f), Icons.Rounded.Home, Color(0xFF4C8DFF), Lang.t("Zuhause", "Home"), home, engine.slotMins["home"]) {
                if (home != null) engine.startRoute(home) else { engine.pendingSave = "home"; engine.toast(engine.str(R.string.rest_1198), 3500); focusSearch() }
            }
            PlaceTile(engine, Modifier.weight(1f), Icons.Rounded.Work, Color(0xFFF29900), Lang.t("Arbeit", "Work"), work, engine.slotMins["work"]) {
                if (work != null) engine.startRoute(work) else { engine.pendingSave = "work"; engine.toast(engine.str(R.string.rest_1197), 3500); focusSearch() }
            }
        }
        // things to find nearby: always all five on show, no scrolling sideways to find them
        // (with a motorhome or caravan profile: pitches, water & waste and LPG instead of charging, food and coffee)
        if (engine.camperProfile) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            FindButton(Icons.Rounded.LocalGasStation, Color(0xFFEA4335), Lang.t("Tankstellen in der Nähe", "Fuel nearby")) { engine.loadNearby("fuel", false) }
            FindButton(Icons.Rounded.RvHookup, Color(0xFF2E7D32), Lang.t("Stellplätze in der Nähe", "Pitches nearby")) { engine.loadNearby("camp", false) }
            FindButton(Icons.Rounded.WaterDrop, Color(0xFF0288D1), Lang.t("Ver- und Entsorgung in der Nähe", "Water and waste nearby")) { engine.loadNearby("dump", false) }
            FindButton(Icons.Rounded.PropaneTank, Color(0xFFEF6C00), Lang.t("Autogas in der Nähe", "LPG nearby")) { engine.loadNearby("lpg", false) }
            FindButton(Icons.Rounded.LocalParking, Color(0xFF4C8DFF), Lang.t("Parkplätze in der Nähe", "Parking nearby")) { engine.loadNearby("parking", false) }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") FindButton(Icons.Rounded.EvStation, Color(0xFF34A853), Lang.t("Ladesäulen in der Nähe", "Charging nearby")) { engine.loadNearby("charging", false) }
            else FindButton(Icons.Rounded.LocalGasStation, Color(0xFFEA4335), Lang.t("Tankstellen in der Nähe", "Fuel nearby")) { engine.loadNearby("fuel", false) }
            FindButton(Icons.Rounded.Restaurant, Color(0xFFF29900), Lang.t("Essen in der Nähe", "Food nearby")) { engine.loadNearby("food", false) }
            FindButton(Icons.Rounded.LocalCafe, Color(0xFFA56BFF), Lang.t("Kaffee in der Nähe", "Coffee nearby")) { engine.loadNearby("coffee", false) }
            FindButton(Icons.Rounded.LocalParking, Color(0xFF4C8DFF), Lang.t("Parkplätze in der Nähe", "Parking nearby")) { engine.loadNearby("parking", false) }
        }
    }
}

@Composable
private fun PlaceTile(engine: NavEngine, modifier: Modifier, icon: ImageVector, tint: Color, label: String, place: com.headunit.launcher.Place?, mins: Int?, onClick: () -> Unit) {
    val c = LocalVw.current
    val here = engine.fix?.pos
    val sub = when {
        place == null -> stringResource(R.string.ui_098)
        mins == 0 -> stringResource(R.string.ui_102)
        mins != null && mins > 0 -> Geo.fmtDur(mins * 60.0)
        here != null -> Geo.fmtDist(Geo.dist(here, place.pos), engine.imperial)
        else -> place.sub
    }
    // (kept compact on the phone so the map stays in view; the short label even when not set yet, "Setze zuhause" was cut off)
    Row(modifier.height(46.dp).panel(6.dp, RoundedCornerShape(18.dp), c.surface).pressClick(onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp)) }
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (place == null) Lang.t("Festlegen", "Set") else sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FindButton(icon: ImageVector, tint: Color, desc: String, onClick: () -> Unit) {
    val c = LocalVw.current
    Box(Modifier.size(42.dp).panel(6.dp, CircleShape, c.surface).pressClick(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** A place sent from your phone: starts by itself after 8 seconds unless you tap Lang.t("Nicht jetzt", "Not now"). */
@Composable
private fun PhoneDestCard(engine: NavEngine, p: com.headunit.launcher.Place) {
    val c = LocalVw.current
    var left by remember(p) { mutableStateOf(8) }
    LaunchedEffect(p) { while (left > 0) { delay(1000); left-- }; engine.takePhoneDest(true) }
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhoneAndroid, null, tint = c.accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Vom Handy: ${p.name}", "From your phone: ${p.name}"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val n = engine.phoneStops.size
            Text((if (n > 0) Lang.t(if (n == 1) "1 Stopp · " else "$n Stopps · ", if (n == 1) "1 stop · " else "$n stops · ") else "") + Lang.t("Start in $left s", "Starting in $left s"), color = c.sub, fontSize = 13.sp)
        }
        ImmPill(Lang.t("Los", "Go"), { engine.takePhoneDest(true) }, filled = true)
        ImmPill(Lang.t("Nicht jetzt", "Not now"), { engine.takePhoneDest(false) })
    }
}

/** On foot to the parked car: the next turn, the distance to it, and what is left; sound on or off for the walk, and end it. */
@Composable
private fun WalkCard(engine: NavEngine) {
    val c = LocalVw.current
    val w = engine.walkInfo
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsWalk, null, tint = c.accent, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            if (w == null) Text(Lang.t("Fußweg wird berechnet …", "Working out the way on foot …"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            else {
                val stepTxt = if (w.maneuver.uppercase().startsWith("ARRIVE")) "" else Geo.fmtDist(w.stepDist, engine.imperial) + " · "
                Text(stepTxt + w.text, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(Lang.t("Noch ${Geo.fmtDist(w.leftM, engine.imperial)} · etwa ${w.leftMin} Min. zum Auto", "${Geo.fmtDist(w.leftM, engine.imperial)} · about ${w.leftMin} min to your car"),
                    color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // the walk's spoken directions: on at the start, off and on again here
        ImmCircle({ engine.cycleSound() }, 40.dp) {
            Icon(if (engine.walkMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp, Lang.t("Ton", "Sound"), tint = if (engine.walkMuted) Bad else c.text, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { engine.endWalk() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, Lang.t("Fußweg beenden", "End the walk"), tint = c.sub, modifier = Modifier.size(22.dp))
        }
    }
}

/** Where you parked (saved on the phone when Android Auto disconnects): walk there, or clear it. Shown for 2 days. */
@Composable
private fun ParkedCard(engine: NavEngine) {
    val c = LocalVw.current
    val raw = engine.prefs.s(PreferenceKeys.PARKED_SPOT, "")
    val parts = raw.split(',')
    val spot = parts.getOrNull(0)?.toDoubleOrNull()?.let { la -> parts.getOrNull(1)?.toDoubleOrNull()?.let { lo -> LL(la, lo) } } ?: return
    val at = parts.getOrNull(2)?.toLongOrNull() ?: return
    val ageMin = ((System.currentTimeMillis() - at) / 60_000L).coerceAtLeast(0L)
    if (ageMin > 48 * 60) return
    // (while walking there, the walk card shows the way instead)
    if (engine.walking) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val here = engine.fix?.pos
    val ago = when { ageMin < 1 -> Lang.t("gerade eben", "just now"); ageMin < 60 -> Lang.t("vor $ageMin Min.", "$ageMin min ago"); ageMin < 24 * 60 -> Lang.t("vor ${ageMin / 60} Std.", "${ageMin / 60} h ago"); else -> Lang.t("gestern", "yesterday") }
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsCar, null, tint = c.accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.ui_073), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(Lang.t("Geparkt $ago", "Parked $ago") + (if (here != null) " · " + Geo.fmtDist(Geo.dist(here, spot), engine.imperial) else ""), color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // the way on foot, in the app (it used to hand a walking link to the phone, which this app took itself and planned on the roads)
        ImmPill(Lang.t("Zu Fuß", "Walk"), { engine.startWalkToCar() }, filled = true)
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { engine.prefs.put(PreferenceKeys.PARKED_SPOT, "") }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, Lang.t("Parkplatz vergessen", "Forget where you parked"), tint = c.sub, modifier = Modifier.size(22.dp))
        }
    }
}

/** Resume a trip, a suggested trip, and saved & recent places. */
@Composable
private fun ImmQuick(engine: NavEngine, showSaved: Boolean = true) {
    val c = LocalVw.current
    val rawCustom = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val rawFav = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val rawRecent = engine.prefs.s(PreferenceKeys.PLACES_RECENT, "")
    // (kind 0 = one of your own labelled places, 1 = a favourite, 2 = recent; the label is the name shown)
    val saved: List<Triple<com.headunit.launcher.Place, Int, String>> = remember(rawCustom, rawFav, rawRecent) {
        val mine = engine.places.customs()
        val favs = engine.places.favourites().filter { f -> mine.none { Geo.dist(it.place.pos, f.pos) < 60 } }
        val recents = engine.places.recents().filter { r -> favs.none { Geo.dist(it.pos, r.pos) < 60 } && mine.none { Geo.dist(it.place.pos, r.pos) < 60 } }
        mine.map { Triple(it.place, 0, it.label) } + favs.map { Triple(it, 1, it.name) } + recents.take(4).map { Triple(it, 2, it.name) }
    }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val here = engine.fix?.pos
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParkedCard(engine)
        engine.resume?.let { rp ->
            Row(
                Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(TealDeep).clickable { engine.startRoute(rp) }.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Route, null, tint = Color.White, modifier = Modifier.size(26.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(Lang.t("Fahrt nach ${rp.name} fortsetzen", "Resume trip to ${rp.name}"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.ui_070), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Dismiss"), tint = Color.White, modifier = Modifier.padding(start = 8.dp).size(24.dp).clickable { engine.resume = null; engine.prefs.put(PreferenceKeys.RESUME, "{}") })
            }
        }
        // three places on show to keep the map clear; "All" opens the rest (it used to be a list up to five tall)
        AnimatedVisibility(showSaved && engine.prefs.b(PreferenceKeys.QUICKLIST, true),
            enter = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(240)), exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(200))) {
        // folded down to this one line on every start; tapping the header unfolds it (and folds it again)
        val folded = engine.quickFolded
        Column(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(vertical = 2.dp).animateContentSize(tween(200))) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { engine.quickFolded = !folded }.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_040), color = c.sub, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(if (folded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        if (folded) Lang.t("Aufklappen", "Unfold") else Lang.t("Zuklappen", "Fold"), tint = c.sub, modifier = Modifier.padding(start = 4.dp).size(20.dp))
                }
                if (!folded && saved.size > 3) Text(if (showAll) Lang.t("Weniger", "Less") else Lang.t("Alle ${saved.size}", "All ${saved.size}"), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { showAll = !showAll }.padding(horizontal = 8.dp, vertical = 6.dp))
                if (!folded) Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { adding = true }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(stringResource(R.string.ui_003), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
                }
            }
            if (!folded && saved.isEmpty()) Text(stringResource(R.string.ui_039), color = c.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp))
            if (!folded) LazyColumn(Modifier.heightIn(max = 200.dp)) {
                items(if (showAll) saved else saved.take(3)) { (p, kind, label) ->
                    val fav = kind != 2
                    // swipe a place sideways to take it off the list (a favourite is unstarred, a recent one forgotten, your own place removed)
                    var dx by remember { mutableStateOf(0f) }
                    var w by remember { mutableStateOf(1) }
                    val shown by androidx.compose.animation.core.animateFloatAsState(dx, label = "swipe")
                    Row(Modifier.fillMaxWidth().onSizeChanged { w = it.width }
                        .pointerInput(p, kind) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(dx) > w * 0.35f) when (kind) {
                                        0 -> { engine.places.removeCustom(label); engine.toast(engine.str(R.string.rest_1216, label), 2000) }
                                        1 -> { engine.places.toggleFavourite(p); engine.toast(engine.str(R.string.rest_1201), 2000) }
                                        else -> { engine.places.removeRecent(p); engine.toast(engine.str(R.string.rest_1200), 2000) }
                                    }
                                    dx = 0f
                                },
                                onDragCancel = { dx = 0f },
                            ) { change, amount -> change.consume(); dx += amount }
                        }
                        .graphicsLayer { translationX = shown; alpha = 1f - (kotlin.math.abs(shown) / w.toFloat()).coerceIn(0f, 0.7f) }
                        .clickable { engine.startRoute(p) }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                            Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.History, null, tint = if (fav) Warn else c.sub, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.padding(start = 11.dp).weight(1f)) {
                            Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (here != null) Text(Geo.fmtDist(Geo.dist(here, p.pos), engine.imperial), color = c.sub, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        }
    }
}

/**
 * Every turn still ahead, opened by tapping the turn card: the arrow, what to do and onto which road, how far away it is and
 * roughly when you'll get there. Tapping a turn shows it on the map and offers to avoid that road. It opens over the trip card,
 * closes by itself 5 seconds after the last touch, and the map goes back to following you.
 */
@Composable
private fun ItineraryCard(engine: NavEngine) {
    @Suppress("UNUSED_VARIABLE") val tick = engine.turn   // read so the list moves along as you drive
    val rows = engine.itinerary()
    val shape = RoundedCornerShape(28.dp)
    var picked by remember { mutableStateOf(-1) }
    var touched by remember { mutableStateOf(0) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // folds away 5 seconds after the last touch (scrolling the list counts)
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) touched++ }
    LaunchedEffect(touched, listState.isScrollInProgress) { if (!listState.isScrollInProgress) { delay(5_000); engine.closeItinerary() } }
    LaunchedEffect(rows.isEmpty()) { if (rows.isEmpty()) engine.closeItinerary() }
    Column(Modifier.fillMaxWidth().shadow(12.dp, shape).clip(shape).background(Teal).border(1.5.dp, Color.White.copy(alpha = 0.18f), shape)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_074), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val e = engine.eta
                Text("${rows.size} ${if (rows.size == 1) Lang.t("Schritt", "step") else Lang.t("Schritte", "steps")}" + (if (e != null) " · " + Geo.fmtDist(e.dist, engine.imperial) + Lang.t(" · Ankunft ", " · arrive ") + e.arrive else ""),
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(TealDeep).clickable { engine.closeItinerary() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).background(TealDeep), state = listState) {
            items(rows, key = { it.index }) { row ->
                val sel = picked == row.index
                Column(Modifier.fillMaxWidth().background(if (sel) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { touched++; picked = row.index; engine.previewTurn(row) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ManeuverArrow(row.maneuver, Geo.maneuverAngle(row.maneuver), Modifier.size(30.dp), roundaboutExit = row.exit, roundaboutClockwise = engine.leftTraffic)
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(row.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(row.tag.ifBlank { null }, Lang.t("ca. ", "about ") + row.arrive).joinToString(" · "), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1)
                        }
                        Text(Geo.fmtDist(row.dist, engine.imperial), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                    }
                    if (sel && row.road.isNotBlank()) Row(Modifier.padding(start = 44.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Lang.t("${row.road} meiden", "Avoid ${row.road}"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).clickable { engine.avoidTurnRoad(row) }.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

/** The charger filter: one power class; the map then shows only the charge points that fall in it. */
@Composable
private fun ChargerFilterDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val options = listOf(0 to Lang.t("Alle Ladesäulen", "All chargers"), 50 to Lang.t("50 kW und höher", "50 kW and up"),
        150 to Lang.t("150 kW und höher", "150 kW and up"), 300 to Lang.t("300 kW und höher", "300 kW and up"))
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 480.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Lang.t("Ladesäulen filtern", "Filter chargers"), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(28.dp).clickable { onClose() })
            }
            for ((kw, label) in options) {
                val on = engine.chargerMinKw == kw
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) c.accent.copy(alpha = 0.18f) else c.surface)
                    .clickable { engine.pickChargerMinKw(kw); onClose() }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = c.text, fontSize = 17.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Rounded.Check, null, tint = c.accent, modifier = Modifier.size(22.dp))
                }
            }
            Text(Lang.t("Standorte mit unbekannter Leistung werden bei einem Filter ausgeblendet.", "Sites with unknown power are hidden while a filter is on."), color = c.sub, fontSize = 12.sp)
        }
    }
}

/** The matches along the route, sorted (least detour, soonest, nearest), with the minutes each adds; tap one, then Add as a stop. */
@Composable
private fun AlongPanel(engine: NavEngine) {
    val c = LocalVw.current
    var picked by remember(engine.alongQuery) { mutableStateOf<NavEngine.AlongHit?>(null) }
    Column(Modifier.floating(24.dp).padding(14.dp).heightIn(max = 420.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Lang.t("Entlang der Route: ${engine.alongQuery}", "Along your route: ${engine.alongQuery}"), color = c.text, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (engine.alongBusy) CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
            Box(Modifier.padding(start = 10.dp).size(44.dp).clip(CircleShape).clickable { engine.closeAlong() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(22.dp))
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(Lang.t("Geringster Umweg", "Least detour"), engine.alongSort == "detour", { engine.changeAlongSort("detour") })
            Pill(Lang.t("Am frühesten", "Soonest"), engine.alongSort == "soon", { engine.changeAlongSort("soon") })
            Pill(Lang.t("Am nächsten", "Nearest"), engine.alongSort == "near", { engine.changeAlongSort("near") })
        }
        if (!engine.alongBusy && engine.alongHits.isEmpty()) Text(Lang.t("Nichts entlang der Route gefunden.", "Nothing found along the route."), color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
        LazyColumn(Modifier.padding(top = 6.dp)) {
            items(engine.alongHits) { h ->
                val sel = picked === h
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { picked = h }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.place.name, color = if (sel) c.accent else c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(if (h.extraMin <= 0) Lang.t("ohne Umweg", "no detour") else Lang.t("+${h.extraMin} Min.", "+${h.extraMin} min"), color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(Lang.t("in ${com.headunit.launcher.Geo.fmtDist(h.ahead, engine.imperial)} voraus · ${Math.round(h.off)} m abseits", "${com.headunit.launcher.Geo.fmtDist(h.ahead, engine.imperial)} ahead · ${Math.round(h.off)} m off the route"),
                        color = c.sub, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        picked?.let { h ->
            ImmPill(Lang.t("Als Stopp hinzufügen", "Add as a stop"), { engine.addStop(h.place); engine.closeAlong() }, filled = true, icon = Icons.Rounded.Add)
        }
    }
}

