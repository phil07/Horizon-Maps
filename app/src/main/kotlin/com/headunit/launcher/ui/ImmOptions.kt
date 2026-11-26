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
import com.headunit.launcher.ReportKind
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
import com.headunit.launcher.Place
import com.headunit.launcher.NavEngine
import kotlinx.coroutines.delay
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.softFail

/**
 * Map options: the map's look first (Standard or Satellite, Auto / Day / Night), then what's shown on it as tiles that light up when
 * on, then the trip tools, reports and things to find. In landscape on a wide screen it's twice as wide, in two columns, so nothing
 * needs scrolling; it still scrolls if a small screen needs it.
 */
@Composable
internal fun OptionsSheet(engine: NavEngine, shareEta: () -> Unit, voice: () -> Unit, wide: Boolean = false, onSettings: () -> Unit = {}, close: () -> Unit) {
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
            Text(Lang.t("Kartenoptionen", "Map options"), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                    LookCard(Modifier.weight(1f), Lang.t("Standard", "Standard"), false, !sat) { p.put(PreferenceKeys.SATELLITE, false) }
                    LookCard(Modifier.weight(1f), Lang.t("Satellit", "Satellite"), true, sat) { p.put(PreferenceKeys.SATELLITE, true) }
                }
                // Auto covers every automatic choice in Settings (headlights, sun times, your own times): tapping it while one of those
                // is on leaves it as it is
                val theme = p.s(PreferenceKeys.THEME, "auto")
                val seg = when (theme) { "day" -> 1; "night" -> 2; else -> 0 }
                Segments(listOf("Auto", Lang.t("Tag", "Day"), Lang.t("Nacht", "Night")), seg) { i -> if (i != seg) p.put(PreferenceKeys.THEME, listOf("auto", "day", "night")[i]) }
                // the brightness of the map as it is showing now: day or night (the night one is the same as in Settings)
                run {
                    val key = if (c.dark) PreferenceKeys.MAP_LIGHT else PreferenceKeys.MAP_LIGHT_DAY
                    val v = (p.s(key, "0").toFloatOrNull() ?: 0f).coerceIn(-50f, 50f)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (c.dark) Lang.t("Helligkeit Nacht", "Brightness night") else Lang.t("Helligkeit Tag", "Brightness day"), color = c.text, fontSize = 13.sp, modifier = Modifier.width(128.dp))
                        androidx.compose.material3.Slider(value = v, onValueChange = { p.put(key, it.toInt().toString()) }, valueRange = -50f..50f, modifier = Modifier.weight(1f))
                    }
                }
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
                    TileSpec(Icons.Rounded.Mic, Lang.t("Sprache", "Voice"), c.accent, { voice(); close() }),
                    TileSpec(soundIcon(engine.soundMode), Lang.t("Ton", "Sound"), if (engine.soundMode == 2) Bad else c.accent, { engine.cycleSound() }),
                    TileSpec(Icons.Rounded.Add, if (engine.via.isEmpty()) Lang.t("Stopp hinzufügen", "Add stop") else Lang.t("Stopps", "Stops"), c.accent, { engine.stopsEditor = true; close() }),
                    TileSpec(Icons.Rounded.DoNotDisturbOn, Lang.t("Straße meiden", "Avoid road"), Warn, { engine.avoidRoadAhead(); close() }),
                    TileSpec(Icons.Rounded.Route, Lang.t("Übersicht", "Overview"), c.accent, { engine.overview(); close() }),
                    TileSpec(Icons.Rounded.Share, if (engine.tripShareUrl != null) Lang.t("Teilen beenden", "Stop sharing") else Lang.t("Ankunftszeit teilen", "Share ETA"), if (engine.tripShareUrl != null) Warn else Good, { shareEta(); close() }),
                ))
            }
        }
        // the reports come right after the trip tools (before the map and the finds): one tap away, not at the bottom of the sheet
        val reportPart: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SheetHeading(Lang.t("Hier melden", "Report here"))
                WideTiles(3, ReportKind.entries.map { k ->
                    TileSpec(k.phoneIcon(), k.label, Color(k.argb), { engine.report(k.key); close() }, picture = if (k == ReportKind.ROADWORKS) R.drawable.ic_roadworks_sign else null)
                }, compact = true)
            }
        }
        val restPart: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { tripPart(); reportPart(); restPart() }
        } else {
            // while navigating the trip tools come first, then the reports; otherwise the map
            tripPart(); reportPart(); mapPart(); restPart()
        }
        // closes the app: any route ends, the GPS stops and the app is gone, so nothing keeps running in the background
        Box(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(Bad.copy(alpha = 0.16f))
            .clickable { close(); engine.closeApp() }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(Lang.t("Horizon Nav schließen", "Close Horizon Nav"), color = Bad, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

/** The icon of each report kind on the phone's "Hier melden" tiles. */
private fun ReportKind.phoneIcon(): ImageVector = when (this) {
    ReportKind.POLICE -> Icons.Rounded.LocalPolice
    ReportKind.ACCIDENT -> Icons.Rounded.CarCrash
    ReportKind.HAZARD -> Icons.Rounded.PriorityHigh
    ReportKind.BREAKDOWN -> Icons.Rounded.Build
    ReportKind.MOBILECAM -> Icons.Rounded.CameraAlt
    ReportKind.JAM -> Icons.Rounded.DirectionsCar
    ReportKind.ROADWORKS -> Icons.Rounded.Build
    ReportKind.CLOSED -> Icons.Rounded.Block
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
