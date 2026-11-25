package com.headunit.launcher.ui

import com.headunit.launcher.acceptFaster
import com.headunit.launcher.answerVote
import com.headunit.launcher.cycleSim
import com.headunit.launcher.dismissVote
import com.headunit.launcher.endWalk
import com.headunit.launcher.handleVoice
import com.headunit.launcher.skipSimTurn
import com.headunit.launcher.startWalkToCar
import com.headunit.launcher.stopSim
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
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
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
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
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
import com.headunit.launcher.Place
import com.headunit.launcher.NavEngine
import kotlinx.coroutines.delay
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.softFail

internal val Teal = GmGreen
internal val TealDeep = GmGreenDeep

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
internal fun ImmCircle(onClick: () -> Unit, size: Dp = 52.dp, active: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val c = LocalVw.current
    Box(
        if (LocalInRail.current) Modifier.size(size).pressClick(onClick).padding(4.dp).clip(CircleShape).background(if (active) c.accent else Color.Transparent)
        else Modifier.size(size).panel(6.dp, CircleShape, if (active) c.accent else c.surface).pressClick(onClick),
        contentAlignment = Alignment.Center, content = content,
    )
}

@Composable
internal fun ImmIcon(icon: ImageVector, onClick: () -> Unit, active: Boolean = false, size: Dp = 52.dp, desc: String? = null) {
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

internal class TileSpec(val icon: ImageVector, val label: String, val tint: Color, val onClick: () -> Unit, val picture: Int? = null)

@Composable
internal fun SheetHeading(text: String) {
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
                // a stop asked for by voice: the search picks it as a stop (the destination stays)
                else if (r.startsWith("stop:")) { val w = r.removePrefix("stop:"); q = w; engine.stopMode = true; engine.search(w) }
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
    // Search as a button (Settings › Places & trips › Search bar): with no route, the bar and its buttons fold into one round button
    var searchOpen by remember { mutableStateOf(false) }
    // (on by default; off, the bar with its buttons is always there)
    val foldOn = engine.prefs.b(PreferenceKeys.SEARCH_FOLD, true)
    val foldSearch = foldOn && !routing && !engine.previewing && !engine.walking && !searchOpen
    val focusMgr = androidx.compose.ui.platform.LocalFocusManager.current
    // folds the open bar back into the button (its arrow, or Back), and the places and buttons under it with it
    val foldUp = { searchOpen = false; q = ""; engine.results = emptyList(); focusMgr.clearFocus() }
    BackHandler(enabled = foldOn && searchOpen && !routing) { foldUp() }
    LaunchedEffect(searchOpen) { if (searchOpen) { try { focus.requestFocus() } catch (e: Exception) { softFail(e) } } }
    LaunchedEffect(routing) { if (routing) searchOpen = false }
    val previewing = engine.previewing
    val simple by remember { derivedStateOf { engine.driving && engine.prefs.b(PreferenceKeys.DRIVESIMPLE, true) } }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        // portrait (or any narrow window): cards use the full width, the round buttons stack down the right-hand side,
        // and your speed and the re-centre button sit just above the trip card instead of beside it
        val narrow = maxWidth < 720.dp
        val upright = maxHeight > maxWidth
        var portraitTripUp by remember { mutableStateOf(false) }
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
        val mainButtonScale = engine.prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).coerceIn(70, 150) / 100f * (if (sidewaysPhone(engine)) 0.8f else 1f)
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
        // with the saved & recent list folded away and no route, the arrow still moves a third as far as with the list down (its height
        // is estimated: about four rows and a heading, 260 dp)
        val listThird = with(LocalDensity.current) { (260.dp / 3).toPx() }
        fun report(i: Int, r: androidx.compose.ui.geometry.Rect) {
            val folded = i == 0 && !routing && !engine.prefs.b(PreferenceKeys.QUICKLIST, true)
            engine.setPanel(i, r.left / boxW, r.top / boxH, r.right / boxW, (r.bottom + if (folded) listThird else 0f) / boxH)
        }
        // a place tapped on the map: its card floats over everything else (also over the route options while choosing a route)
        // docked like Add a stop: along the bottom, or down the driver's side when the screen is held sideways (a head unit too)
        engine.favNaming?.let { FavNameDialog(engine, it) }
        if (engine.mapPick != null) {
            val landscape = boxW > boxH
            Box(Modifier.align(if (landscape) side(Alignment.CenterStart) else Alignment.BottomCenter).padding(16.dp).zIndex(30f)) { PhonePickCard(engine) }
        }
        // a place picked while driving: stop or new destination? (it waits 15 s, then the trip keeps its destination)
        if (engine.newDestAsk != null) {
            val landscape = boxW > boxH
            Box(Modifier.align(if (landscape) side(Alignment.CenterStart) else Alignment.BottomCenter).padding(16.dp).zIndex(31f)) { NewDestCard(engine) }
        }
        // the media bar: back, play / pause and next while music plays (see MediaHub)
        if (engine.prefs.b(PreferenceKeys.MEDIA_CONTROLS, false)) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).zIndex(1f)) { MediaBar(engine) }
        if (foldSearch) Box(Modifier.align(side(Alignment.TopStart)).padding(16.dp).zIndex(1f)) {
            ImmCircle(onClick = { searchOpen = true }, size = 56.dp) { Icon(Icons.Rounded.Search, Lang.t("Suche", "Search"), tint = LocalVw.current.text, modifier = Modifier.size(26.dp)) }
        }
        // upright on a route the turn card takes the full width; the map buttons then sit under it (see the rail's top)
        val turnFull = narrow && routing && engine.turn != null
        // Settings › Advanced screen sizes › Turn card width (upright the card can only be narrower than the screen)
        val turnW = engine.prefs.i(PreferenceKeys.TURN_CARD_WIDTH, 100).coerceIn(60, 150) / 100f
        Column(Modifier.align(side(Alignment.TopStart)).padding(16.dp).then(if (narrow && !turnFull) Modifier.fillMaxWidth().sides(end = 52.dp * mainButtonScale + 10.dp) else if (turnFull) Modifier.fillMaxWidth(turnW.coerceAtMost(1f)) else Modifier.width(380.dp * (if (routing) turnW else 1f)))
            .onGloballyPositioned { report(0, it.boundsInParent()) }
            // the turn list opens over the trip card below it
            .zIndex(if (engine.itineraryOpen) 2f else 0f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // every block slides and fades in and out instead of popping, so the screen changes feel like one piece
            AnimatedVisibility(!routing && !previewing && !foldSearch, enter = fadeIn(tween(220)) + androidx.compose.animation.expandVertically(tween(260)), exit = fadeOut(tween(160)) + androidx.compose.animation.shrinkVertically(tween(220))) {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // on foot to the parked car: the next instruction and what is left, over the search bar
                if (engine.walking) WalkCard(engine)
                ImmSearch(engine, q, { q = it }, typingLocked, focus, voice, onFold = if (foldOn) foldUp else null)
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
                        // a phone held sideways is short: the home and work tiles come down a little there
                        val chipsDensity = LocalDensity.current
                        CompositionLocalProvider(LocalDensity provides Density(chipsDensity.density * (if (sidewaysPhone(engine)) 0.85f else 1f), chipsDensity.fontScale)) {
                            ImmChips(engine) { try { focus.requestFocus() } catch (e: Exception) { softFail(e) } }
                            ParkedCard(engine)
                            // upright: the weather, the cheapest fuel and the leave-by times under your places
                            if (upright) { PortraitChips(engine); LeaveByChips(engine) }
                            OutsideMapsChip(engine)
                        }
                        PredictionCard(engine)
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
                    TurnLineChips(engine)
                    if (!engine.itineraryOpen) ParkingCard(engine, car = false)
                }
                // an alert on the route: one line under the turn card (the big banner is off on the route, see App)
                val curAlert = engine.alert
                val lastAlert = remember { mutableStateOf(curAlert) }
                if (curAlert != null) lastAlert.value = curAlert
                AnimatedVisibility(curAlert != null, enter = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(220)), exit = fadeOut(tween(160)) + androidx.compose.animation.shrinkVertically(tween(180))) {
                    lastAlert.value?.let { AlertLine(it) }
                }
            }
            // (adding a stop upright: the list sits along the bottom instead, see below)
            if (engine.nearbyKind != null && (!previewing || engine.stopMode) && !(narrow && engine.stopMode) && !(engine.stopsEditor && !narrow)) NearbyCard(engine)
        }
        // adding a stop on a phone held upright: the panel and the places along the route sit along the bottom, over the trip card's place,
        // with the places shown on the map above them
        if (narrow && engine.stopMode && engine.nearbyKind != null) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .heightIn(max = maxHeight * 0.5f).padding(start = 16.dp, end = 16.dp, bottom = if (tripPx > 0) tripH + 10.dp else 16.dp)) { NearbyCard(engine) }

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
            val evTrip = engine.ev.isEv && engine.route != null && !engine.previewing
            val showSettings = !routing
            val unit = 52.dp * mainButtonScale
            // the top of the speedometer (measured; a fair guess until it has been laid out)
            val speedTop = if (speedTopPx >= 0) with(dens) { speedTopPx.toDp() } else maxHeight - 190.dp
            // landscape: Settings sits to the left of the panel (to the right when mirrored) so the panel starts at the very top, with
            // zoom straight under it. Portrait: Settings above the panel, as there's no room beside it next to the search bar.
            val settingsAbove = narrow && showSettings
            val turnAbove = if (narrow && routing && engine.turn != null) with(dens) { turnPx.toDp() } + 16.dp else 0.dp
            val top = 16.dp * mainButtonScale + (if (settingsAbove) unit + 10.dp * mainButtonScale else 0.dp) + turnAbove
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
        // (Settings > Beim Fahren > Tacho-Seite: right by default, left, or automatic as it used to be. On a wide screen the trip
        // card can be on the same side: the speed then sits above it, as it does on a narrow screen.)
        val speedLeft = engine.speedLeft()
        val speedOverTrip = (narrow || speedLeft == (side(Alignment.BottomStart) == Alignment.BottomStart)) && tripPx > 0
        if (!previewing) Box(
            Modifier.align(if (speedLeft) Alignment.BottomStart else Alignment.BottomEnd)
                .padding(start = if (speedLeft) 16.dp else 0.dp, end = if (speedLeft) 0.dp else 16.dp, bottom = if (speedOverTrip) tripH + 4.dp else 16.dp)
                .onGloballyPositioned { val t = it.boundsInParent().top.toInt(); if (it.size.height > 0 && t != speedTopPx) speedTopPx = t }
        ) {
            val baseSpeedDensity = LocalDensity.current
            // (sized by Settings > Screen & sizes > Speed limit and speedometer size, inside ImmSpeed)
            val speedSize = 1f
            CompositionLocalProvider(LocalDensity provides Density(baseSpeedDensity.density * speedSize, baseSpeedDensity.fontScale)) {
                // the car's battery level sits right above the speed, on the same side
                Column(horizontalAlignment = if (speedLeft) Alignment.Start else Alignment.End) {
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
        // the new portrait layout on a route: the trip as a sheet along the bottom (time left, arrival, distance, End); pulled up it
        // shows the rest of the trip card (add a stop, share, the turns, the music)
        val portraitRoute = engine.prefs.b(PreferenceKeys.PORTRAIT_LAYOUT, true) && maxHeight > maxWidth && routing && !previewing
        if (portraitRoute && engine.arrival == null) {
            val pe = engine.eta
            val pc = LocalVw.current
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp).panel(10.dp, RoundedCornerShape(24.dp), pc.surface).padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.padding(top = 8.dp, bottom = 6.dp).clip(RoundedCornerShape(3.dp)).background(pc.sub.copy(alpha = 0.5f)).size(width = 44.dp, height = 5.dp)
                    .clickable { portraitTripUp = !portraitTripUp })
                FasterStrip(engine, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
                if (pe != null) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(Geo.fmtDur(pe.secs), color = pc.accent, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(Lang.t("Verbleibend", "Left"), color = pc.sub, fontSize = 12.sp, maxLines = 1) }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) { Text(pe.arrive, color = pc.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(Lang.t("Ankunft", "Arrival"), color = pc.sub, fontSize = 12.sp, maxLines = 1) }
                    Spacer(Modifier.width(14.dp))
                    Text(Geo.fmtDist(pe.dist, engine.imperial), color = pc.text, fontSize = 18.sp, maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFEA4335)).clickable { engine.endRoute() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Close, Lang.t("Route beenden", "End route"), tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                // the last stretch: which side the destination is on (from where you are and the way you are heading)
                val lastSide = run {
                    val from = engine.fix ?: return@run null
                    val to = engine.dest?.pos ?: return@run null
                    if (pe == null || pe.dist > 400.0) return@run null
                    val d = ((com.headunit.launcher.Geo.bearing(from.pos, to) - from.heading + 540.0) % 360.0) - 180.0
                    when { d > 25.0 -> Lang.t("Ziel rechts", "Destination on the right"); d < -25.0 -> Lang.t("Ziel links", "Destination on the left"); else -> Lang.t("Ziel geradeaus", "Destination straight ahead") }
                }
                lastSide?.let { Text(it, color = pc.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 4.dp)) }
                liveCostText(engine)?.let { Text(it, color = pc.sub, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp)) }
                if (portraitTripUp) TripArea(engine, shareEta, narrow, voice)
            }
        }
        else if (!previewing && engine.arrival == null) {
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
                ImmPill(Lang.t("Zentrieren", "Re-centre"), { engine.recenter(byUser = true) }, Icons.Rounded.MyLocation, filled = true)
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
                Text(Lang.t("Verkehr wird angezeigt, während du stehst", "Showing traffic while you’re stopped"), color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
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

        // a stop on the way reached: the trip waits here until you go on
        engine.stopPause?.let { name ->
            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(start = 16.dp, end = 16.dp, top = 150.dp).widthIn(max = 540.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Route, null, tint = c.accent, modifier = Modifier.size(24.dp)) }
                Column(Modifier.weight(1f, fill = false)) {
                    Text(Lang.t("Zwischenziel erreicht", "Stop reached"), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(name, color = c.sub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                ImmPill(Lang.t("Fahrt weitermachen", "Continue trip"), { engine.resumeAfterStop() }, filled = true)
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
        ) { OptionsSheet(engine, shareEta, voice = voice, wide = !narrow && maxWidth >= 860.dp && maxHeight < maxWidth, onSettings = { sheet = false; onSettings() }) { sheet = false } }
        // Add a stop: a panel down the side on a phone held sideways (the route stays on the map beside it), a box over the map upright
        if (engine.stopsEditor) {
            if (narrow) StopsDialog(engine, onClose = { engine.stopsEditor = false })
            else Box(Modifier.align(side(Alignment.TopStart)).padding(16.dp).width(400.dp).heightIn(max = maxHeight - 32.dp).zIndex(5f)) {
                StopsDialog(engine, asPanel = true, onClose = { engine.clearNearby(); engine.stopsEditor = false })
            }
        }
        if (engine.chargerFilterOpen) ChargerFilterDialog(engine) { engine.chargerFilterOpen = false }
    }
}

/** A faster route on offer, as a strip on the trip card: its saving, then switch or keep. */
@Composable
private fun FasterStrip(engine: NavEngine, modifier: Modifier) {
    val c = LocalVw.current
    val fr = engine.faster ?: return
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Good.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Route, null, tint = Good, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Schnellere Route gefunden", "Faster route found"), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Lang.t("Spart etwa ${fr.savedMin} Min.", "Saves about ${fr.savedMin} min"), color = Good, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        ImmPill(Lang.t("Wechseln", "Switch"), { engine.acceptFaster() }, filled = true)
        ImmPill(Lang.t("Behalten", "Keep"), { engine.dismissFaster() })
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
            val dc = delayColour(e.delayMin, c.dark)
            val shape = RoundedCornerShape(22.dp)
            // landscape: the card is only as wide as its contents (figures, then the buttons close by), and the route bar matches that width.
            // portrait: full width, with the buttons under the figures.
            val dens = androidx.compose.ui.platform.LocalDensity.current
            var rowPx by remember { mutableStateOf(0) }
            val rowW = with(dens) { rowPx.toDp() }
            Column(Modifier.then(if (narrow) Modifier.fillMaxWidth() else Modifier).panel(10.dp, shape, c.surface).animateContentSize()) {
                val barMod = if (narrow) Modifier.fillMaxWidth() else Modifier.width(rowW)
                // a faster route on offer: a strip across the top of the card, switch or keep
                if (engine.faster != null && (narrow || rowPx > 0)) {
                    FasterStrip(engine, barMod.padding(start = if (narrow) 16.dp else 18.dp, end = if (narrow) 16.dp else 18.dp, top = if (narrow) 8.dp else 9.dp, bottom = 8.dp))
                    Box(barMod.padding(horizontal = 18.dp).height(1.dp).background(c.line))
                }
                // the route bar across the top of the card, with room to breathe
                if (engine.showRouteBar && (narrow || rowPx > 0)) {
                    RouteBar(engine, barMod.padding(start = if (narrow) 16.dp else 18.dp, end = if (narrow) 16.dp else 18.dp, top = if (narrow) 7.dp else 9.dp, bottom = if (narrow) 2.dp else 4.dp))
                    Box(barMod.padding(horizontal = 18.dp).height(1.dp).background(c.line))
                }
                // electric: roughly how much battery you'll arrive with (amber under 20%, red under 10%)
                val evPct = engine.evArrivalPct()
                val evCol = if (evPct == null) c.text else if (evPct < 10) Bad else if (evPct < 20) Warn else Good
                // the figures sit together on the left of the card (one group, not spread across it)
                val stats: @Composable () -> Unit = {
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp), verticalAlignment = Alignment.CenterVertically) {
                        ImmStat(Geo.fmtDur(e.secs), if (e.delayMin >= 1) Lang.t("+${e.delayMin} Min. Verkehr", "+${e.delayMin} min traffic") else Lang.t("Restzeit", "time left"), dc)
                        ImmStat(e.arrive, Lang.t("Ankunft", "arrival"), c.text)
                        ImmStat(Geo.fmtDist(e.dist, engine.imperial), Lang.t("verbleibend", "to go"), c.text)
                        if (evPct != null) ImmStat("$evPct%", Lang.t("Akku bei Ankunft", "battery at arrival"), evCol)
                    }
                }
                val buttons: @Composable () -> Unit = {
                    // electric: chargers along the route from here
                    if (evPct != null) ImmCircle({ engine.loadNearby("charging", false) }, 40.dp) { Icon(Icons.Rounded.EvStation, Lang.t("Ladesäulen voraus", "Chargers ahead"), tint = Good, modifier = Modifier.size(20.dp)) }
                    // add a stop: opens your stops, where one can be added on the way
                    ImmCircle({ engine.stopsEditor = true }, 40.dp) { Icon(Icons.Rounded.Add, Lang.t("Stopp hinzufügen", "Add stop"), tint = c.text, modifier = Modifier.size(21.dp)) }
                    // the arrival time to someone: only while stopped, so nobody picks a contact while driving
                    if ((engine.fix?.speed ?: 0.0) < 0.8) ImmCircle({ engine.shareArrival() }, 40.dp) { Icon(Icons.Rounded.Share, Lang.t("Ankunftszeit senden", "Send arrival time"), tint = c.text, modifier = Modifier.size(20.dp)) }
                    // (voice and sound are in Map options › This trip, not on the trip card)
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
                        ImmCircle({ engine.stopsEditor = true }, 36.dp) { Icon(Icons.Rounded.Add, Lang.t("Stopp hinzufügen", "Add stop"), tint = c.text, modifier = Modifier.size(19.dp)) }
                        Box(Modifier.size(36.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Bad).pressClick { engine.endRoute() }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Close, Lang.t("Route beenden", "End route"), tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                // the live fuel cost (Settings › Fuel & charging), a line under the figures
                liveCostText(engine)?.let { Text(it, color = c.sub, fontSize = 13.sp, maxLines = 1, modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 8.dp)) }
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
    val pct = Math.round(engine.ev.evNowEstimatePct()).toInt()
    val km = engine.ev.evEstimateKm()
    val age = engine.ev.evNowAgeMin() ?: 0L
    val col = if (pct < 10) Bad else if (pct < 20) Warn else Good
    Row(
        Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(50)).background(Color(0xCC0B0F16)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(col))
        Text((if (engine.ev.evEstimateOld()) "ca. " else "") + "$pct %", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        km?.let { Text("· $it km", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
        // charging, from the car maker's cloud (only while that reading is recent)
        if (engine.prefs.s(PreferenceKeys.CLOUD_CHARGING, "") == "1" && System.currentTimeMillis() - (engine.prefs.s(PreferenceKeys.CLOUD_AT, "").toLongOrNull() ?: 0L) < 30 * 60_000L) {
            val m = engine.prefs.s(PreferenceKeys.CLOUD_CHARGE_MIN, "").toIntOrNull()
            Text("· " + Lang.t("lädt", "charging") + (m?.let { " · $it min" } ?: ""), color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        } else if (engine.ev.evCharging()) {
            // standing at a charger with a rising reading (see NavEngine.evCharging)
            Text("· " + Lang.t("lädt", "charging"), color = Good, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        if (age >= 10) Text("· $age Min.", color = if (age > 60) Warn else Color(0xFFC3CAD5), fontSize = 12.sp)
    }
}

/** Whether the speed is shown on the left: on the phone / head unit by the setting (right by default, left, or automatic = left when mirrored); on the car screen where the car overlay put it. */
internal fun NavEngine.speedLeft(): Boolean = if (isCar) carSpeedLeft else when (prefs.s(PreferenceKeys.MAIN_SPEED_SIDE, "right")) { "left" -> true; "auto" -> mirrored; else -> false }

/** The colour of the time left: green on time, amber for a few minutes' delay, red for ten or more. On a light card deeper shades,
 *  so they stand out against white. */
internal fun delayColour(delayMin: Int, dark: Boolean): androidx.compose.ui.graphics.Color = when {
    delayMin >= 10 -> if (dark) Bad else androidx.compose.ui.graphics.Color(0xFFC62828)
    delayMin >= 1 -> if (dark) Warn else androidx.compose.ui.graphics.Color(0xFFB45309)
    else -> if (dark) Good else androidx.compose.ui.graphics.Color(0xFF15803D)
}

/**
 * The sheet's small chips: the weather now and the cheapest of your fuel nearby (tap to go there). Each one only when there is
 * something to show. (Your car has its own card, see ParkedCard.)
 */
@Composable
private fun PortraitChips(engine: NavEngine) {
    val w = engine.weather
    val cheap = engine.nearby.filter { it.price != null }.minByOrNull { it.price ?: Double.MAX_VALUE }
    if (w == null && cheap == null) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        w?.let { ImmChip(Icons.Rounded.Cloud, Color(0xFF4A90E2), "${Math.round(it.tempC)} °C", {}) }
        cheap?.let { n ->
            ImmChip(Icons.Rounded.LocalGasStation, Color(0xFF34A853), com.headunit.launcher.fmtFuelPrice(n.price ?: 0.0, n.unit)) { engine.startRoute(n.place) }
        }
    }
}

/**
 * "Abfahren um 08:04 zur Arbeit": from 45 minutes before the time you usually set off for Home or Work (learnt from at least
 * three trips there) until then. Tap to go.
 */
@Composable
private fun LeaveByChips(engine: NavEngine) {
    val cal = java.util.Calendar.getInstance()
    val now = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    for ((slot, de, en) in listOf(Triple("home", "Zuhause", "Home"), Triple("work", "Arbeit", "Work"))) {
        val place = engine.places.slot(slot) ?: continue
        val dep = com.headunit.launcher.LearnedTrips.usualDeparture(engine.prefs, place.name) ?: continue
        if (now < dep - 45 || now >= dep) continue
        val hhmm = String.format(java.util.Locale.US, "%02d:%02d", dep / 60, dep % 60)
        ImmChip(Icons.Rounded.DirectionsCar, Color(0xFF2EC27E), Lang.t("Abfahren $hhmm zur $de", "Leave by $hhmm for $en")) { engine.startRoute(place) }
    }
}

/**
 * A line when you're outside every map you've downloaded (there are some, and none covers where you are): so you know to get this
 * area while you still have a connection. Shown only with a GPS fix.
 */
@Composable
private fun OutsideMapsChip(engine: NavEngine) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val p = engine.fix?.pos ?: return
    if (!com.headunit.launcher.MapboxOffline.hasAreas(ctx) || com.headunit.launcher.MapboxOffline.covers(ctx, p)) return
    ImmChip(Icons.Rounded.Map, Color(0xFFEA4335), Lang.t("Außerhalb deiner Karten", "Outside your maps")) { engine.settingsOpenAt = SECTION_OFFLINE_MAPS }
}

/**
 * The hints under the turn card as small chips in one line (it scrolls sideways when it's too long): the average speed (red when
 * over the limit), the next service, the advice for the conditions and the notice (tap to dismiss it); car parks have their own
 * card under it (see ParkingCard). Each one only when there is something to show.
 */
@Composable
internal fun TurnLineChips(engine: NavEngine) {
    val k = if (engine.imperial) 2.23694 else 3.6
    val unit = if (engine.imperial) "mph" else "km/h"
    val avg = engine.avgInfo
    val svc = engine.servicesAhead.firstOrNull()
    val advice = engine.advice
    val notice = engine.notice
    if (avg == null && svc == null && advice == null && notice == null) return
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        avg?.let { a ->
            ImmChip(Icons.Rounded.Speed, if (a.over) Color(0xFFD32F2F) else Color(0xFF34A853), "Ø ${Math.round(a.avgMs * k)} $unit") { }
        }
        notice?.let { n -> ImmChip(Icons.Rounded.Info, Color(0xFF8E24AA), n.title.take(24)) { engine.dismissNotice() } }
        advice?.let { ImmChip(Icons.Rounded.Lightbulb, Color(0xFFFBBC04), it.take(28)) { } }
        svc?.let { ImmChip(Icons.Rounded.LocalGasStation, Color(0xFF34A853), "${it.name.take(14)} · ${Geo.fmtDist(it.dist, engine.imperial)}") { } }    }
}
