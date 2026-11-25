package com.headunit.launcher.ui

import com.headunit.launcher.installedVoices
import com.headunit.launcher.useVoice
import com.headunit.launcher.R
import androidx.compose.ui.res.stringResource

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.TurnRight
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.BuildConfig
import com.headunit.launcher.FuelFinder
import com.headunit.launcher.KeyBackup
import com.headunit.launcher.AutoBackup
import com.headunit.launcher.Geo
import com.headunit.launcher.Lang
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflinePlaces
import com.headunit.launcher.Place
import com.headunit.launcher.Prefs
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.mapSource
import com.headunit.launcher.mapDetail
import com.headunit.launcher.setApiKey
import com.headunit.launcher.Zones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.headunit.launcher.softFail

/** A settings section; [heading] puts a small group heading (Map, Driving, You, Device) above it in the list. */
private class Section(val title: String, val sub: String, val icon: ImageVector, val tint: Color, val heading: String? = null)

// (the order here is the order in the list; SettingsIndex refers to them by number)
private val sections = listOf(
    Section(Lang.t("Kartenansicht", "Map view"), Lang.t("Licht, Wetter, Details, 3D, Satellit", "Light, weather, detail, 3D, satellite"), Icons.Rounded.Map, Color(0xFF4C8DFF), Lang.t("KARTE", "MAP")),
    Section(Lang.t("Auf der Karte", "On the map"), Lang.t("Verkehr, Ampeln, Kameras, Schilder", "Traffic, lights, cameras, signs"), Icons.Rounded.Traffic, Color(0xFF4C8DFF)),
    Section(Lang.t("Pfeil & Routenlinie", "Arrow & route line"), Lang.t("Pfeil oder Auto, Farben, Routendesign", "Arrow or car, colours, route style"), Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
    Section(Lang.t("Routen", "Routes"), Lang.t("Vermeiden, Öko, schnellere Routen, Start", "Avoid, eco, faster routes, start"), Icons.Rounded.Route, Color(0xFF12A4B4), Lang.t("FAHREN", "DRIVING")),
    Section(Lang.t("Abbiegekarte & Spuren", "Turn card & lanes"), Lang.t("Schilder, Spuren, Fahrtenkarte, Ankunft", "Signs, lanes, trip card, arrival"), Icons.Rounded.TurnRight, Color(0xFF12A4B4)),
    Section(Lang.t("Warnungen", "Alerts"), Lang.t("Tempo, Blitzer, Gefahren, Meldungen", "Speed, cameras, hazards, reports"), Icons.Rounded.NotificationsActive, Color(0xFFEA4335)),
    Section(Lang.t("Ton & Stimme", "Sound & voice"), Lang.t("Sprachansagen und Lautstärke", "Spoken directions and volume"), Icons.Rounded.VolumeUp, Color(0xFFEA4335)),
    Section(Lang.t("Orte & Fahrten", "Places & trips"), Lang.t("Zuhause, Arbeit, Favoriten, Teilen, Fahrtenbuch", "Home, work, favourites, sharing, trip log"), Icons.Rounded.Star, Color(0xFFF29900), "DU"),
    Section(Lang.t("Kraftstoff & Laden", "Fuel & charging"), Lang.t("Spritart, Preise, Elektroauto", "Fuel type, prices, electric car"), Icons.Rounded.LocalGasStation, Color(0xFF34A853)),
    Section(Lang.t("Offline-Karten", "Offline maps"), Lang.t("Karten, Routen und Orte ohne Empfang", "Maps, routes and places with no signal"), Icons.Rounded.CloudDownload, Color(0xFF8E5CF0)),
    Section(Lang.t("Bildschirm & Größen", "Screen & sizes"), Lang.t("Helligkeit, Größen, Einheiten", "Brightness, sizes, units"), Icons.Rounded.BrightnessMedium, Color(0xFFFFB020), Lang.t("GERÄT", "DEVICE")),
    Section(Lang.t("Beim Fahren", "While driving"), Lang.t("Straßenname, Tacho, Tunnel, Medien, Sperren", "Road name, speedometer, tunnels, media, locking"), Icons.Rounded.DirectionsCar, Color(0xFF12A4B4)),
    Section("Android Auto", Lang.t("Autodisplay-Größen und Einrichtung", "Car screen sizes and setup"), Icons.Rounded.DirectionsCar, Color(0xFF9CA4BF)),
    Section(Lang.t("Leistung", "Performance"), Lang.t("Flüssigkeit auf langsamen Displays, Demo", "Smoothness on slow screens, demo"), Icons.Rounded.Speed, Color(0xFF9CA4BF)),
    Section(Lang.t("Schlüssel & Konten", "Keys & accounts"), Lang.t("Kartenschlüssel und Suche", "Map keys and search"), Icons.Rounded.VpnKey, Color(0xFF9CA4BF)),
    Section(Lang.t("Info & Sicherung", "About & backup"), Lang.t("Updates, Berechtigungen, Sicherung", "Updates, permissions, backup"), Icons.Rounded.Info, Color(0xFF9CA4BF)),
)

/** Every setting in one screen: sections on the left, the chosen section on the right, in the map screen's soft style. */
@Composable
fun SettingsScreen(prefs: Prefs, engine: NavEngine, startAt: Int? = null, onClose: () -> Unit) {
    val base = LocalVw.current
    val v = remember(base) { navVw(base) }
    CompositionLocalProvider(LocalVw provides v) { SettingsBody(prefs, engine, startAt, onClose) }
}

/** The index of the Offline maps section (see [sections]): the "Außerhalb deiner Karten" tap opens it. */
internal const val SECTION_OFFLINE_MAPS = 9

@Composable
private fun SettingsBody(prefs: Prefs, engine: NavEngine, startAt: Int?, onClose: () -> Unit) {
    val c = LocalVw.current
    var cat by rememberSaveable { mutableIntStateOf(startAt ?: 0) }
    // portrait / narrow: one pane at a time (the section list, or the open section with a back arrow)
    val narrowS = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720
    var showList by rememberSaveable { mutableStateOf(startAt == null) }
    androidx.activity.compose.BackHandler(enabled = narrowS && !showList) { showList = true }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = remember(query) {
        if (words.isEmpty()) emptyList() else settingsIndex.filter { r ->
            val hay = (listOf(r.title, r.sub, r.group).flatMap { listOf(it.de, it.en) } + sections[r.section].title).joinToString(" ").lowercase()
            words.all { hay.contains(it) }
        }.sortedBy { if (it.title.s.lowercase().startsWith(words[0])) 0 else if (it.title.s.lowercase().contains(words[0])) 1 else 2 }.take(40)
    }
    Row(Modifier.fillMaxSize().background(c.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // the list of sections
        if (!narrowS || showList) Column(
            Modifier.then(if (narrowS) Modifier.fillMaxWidth() else Modifier.width(290.dp)).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(28.dp)).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (Neon.on) Text("FILTER", color = c.sub, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(c.tile).pressClick(onClose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.ArrowBack, Lang.t("Zurück zur Karte", "Back to the map"), tint = c.text, modifier = Modifier.size(24.dp))
                }
                Text(Lang.t("Einstellungen", "Settings"), color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
            }
            // search every setting by name
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).height(46.dp).clip(RoundedCornerShape(50)).background(c.tile).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.sub, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(Lang.t("Sucheinstellungen", "Search settings"), color = c.sub, fontSize = 15.sp)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query, onValueChange = { query = it; highlight = null }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = c.text, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(c.accent),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) Icon(Icons.Rounded.Close, "Löschen", tint = c.sub, modifier = Modifier.size(20.dp).clickable { query = ""; focus.clearFocus() })
            }
            if (words.isNotEmpty()) {
                if (matches.isEmpty()) Text(Lang.f("Keine Treffer für „%1\$s“", "Nothing matches “%1\$s”", query.trim()), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                matches.forEach { r ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                            cat = r.section; highlight = r.title.s; query = ""; focus.clearFocus(); showList = false
                        }.padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(r.title.s, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(sections[r.section].title + (if (r.group.s.isNotBlank()) " · " + r.group.s else ""), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else sections.forEachIndexed { i, s ->
                // the small group headings in the list: Map, Driving, You, Device
                if (s.heading != null) Text(s.heading, color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(start = 14.dp, top = if (i == 0) 2.dp else 12.dp, bottom = 2.dp))
                val on = i == cat
                val rowBg by androidx.compose.animation.animateColorAsState(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent, androidx.compose.animation.core.tween(200), label = "secBg")
                Row(
                    Modifier.fillMaxWidth().then(if (Neon.on && on) Modifier.neonBrackets(c.accent, 8.dp) else Modifier).clip(RoundedCornerShape(18.dp)).background(rowBg).clickable { cat = i; highlight = null; showList = false }.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (Neon.on) Box(Modifier.padding(start = 4.dp).size(10.dp).then(if (on) Modifier.background(c.accent) else Modifier.border(1.5.dp, Neon.red)))
                    else Box(Modifier.size(38.dp).clip(CircleShape).background(s.tint.copy(alpha = if (on) 0.26f else 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(s.icon, null, tint = s.tint, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(if (Neon.on) s.title.uppercase() else s.title, color = if (on) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = if (Neon.on) 1.sp else 0.sp)
                        Text(s.sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // the chosen section (each opens at the top), sliding up gently into place
        if (!narrowS || !showList) Column(Modifier.weight(1f).fillMaxHeight()) {
        // the neon look: the sections as a bar of tabs at the top
        if (Neon.on) NeonTabs(sections.map { it.title }, cat, { cat = it; highlight = null })
        // on a phone, the map pages (On the map, Your arrow, and Map style's own design below its look) keep the live map at the top
        // while you scroll the settings, so each change shows as you make it. The map moves here from the map screen (which is off
        // while the settings are open). Map style shows it inside its own design instead (see LiveDesignPreview).
        if (narrowS && !showList && cat in 1..2) Box(
            Modifier.fillMaxWidth().height((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp / 3).dp).padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            // the preview starts on your own position (not wherever the map was moved to on the map screen), a step further out
            // than the map (so the 3D buildings still show, and more of the map round you); the map's own zoom comes back when it closes
            androidx.compose.runtime.DisposableEffect(Unit) {
                val before = engine.zoomOffset
                engine.recenter(byUser = true)
                engine.zoomOffset = (engine.zoomOffset - 1.0).coerceIn(-4.0, 3.0)
                onDispose { engine.zoomOffset = before }
            }
            MapHost(engine.map, Modifier.fillMaxSize(), cornerDp = 18)
        }
        androidx.compose.animation.AnimatedContent(
            targetState = cat,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)) +
                    androidx.compose.animation.slideInVertically(androidx.compose.animation.core.tween(260)) { it / 24 }) togetherWith
                    androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
            },
            label = "section",
        ) { cat ->
          CompositionLocalProvider(LocalSettingHighlight provides highlight) {
            Column(Modifier.fillMaxSize().imePadding()) {
                // on a phone held upright, the back arrow and the page name stay in a bar at the top while the page scrolls
                if (narrowS) Row(Modifier.fillMaxWidth().background(c.bg).padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(c.tile).clickable { showList = true }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, Lang.t("Alle Einstellungen", "All settings"), tint = c.text, modifier = Modifier.size(22.dp))
                    }
                    Text(sections[cat].title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp)) {
                if (!narrowS) Text(sections[cat].title, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(sections[cat].sub, color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, top = if (narrowS) 4.dp else 0.dp, bottom = 16.dp))
                Column(Modifier.widthIn(max = 820.dp)) {
                    when (cat) {
                        0 -> MapLookSection(prefs, engine)
                        1 -> OnTheMapSection(prefs, engine)
                        2 -> ArrowSection(prefs, engine)
                        3 -> RoutesSection(prefs)
                        4 -> GuidanceSection(prefs)
                        5 -> AlertsSection(prefs, engine)
                        6 -> SoundSection(prefs, engine)
                        7 -> { PlacesSection(engine); MyRoutesGroup(engine); PhoneLinkGroup(prefs); SharingGroups(prefs) }
                        8 -> FuelSection(prefs, engine)
                        9 -> { OfflineMapsContent(prefs, engine); OfflinePlacesGroup(engine) }
                        10 -> ScreenSection(prefs, engine)
                        11 -> WhileDrivingSection(prefs, engine)
                        12 -> AndroidAutoSection(prefs)
                        13 -> PerformanceSection(prefs)
                        14 -> KeysSection(prefs, engine)
                        else -> AboutSection(prefs, engine)
                    }
                }
            }
            // on a phone the settings fade softly at the top and bottom edges, under the bar and above the last row
            if (narrowS) {
                Box(Modifier.align(androidx.compose.ui.Alignment.TopCenter).fillMaxWidth().height(14.dp)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(c.bg, androidx.compose.ui.graphics.Color.Transparent))))
                Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth().height(14.dp)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, c.bg))))
            }
                }
            }
          }
        }
        }
    }
}

// ------------------------------------------------------------------ Map look

/** Own design: route, arrow and accent colour and a lighter or darker map, with a preview that changes as you pick. */
@Composable
private fun OwnDesignGroup(prefs: Prefs, engine: NavEngine) {
    val route = prefs.s(PreferenceKeys.ROUTE_COLOUR, "")
    val arrow = prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")
    val accentHex = prefs.s(PreferenceKeys.ACCENT_COLOUR, "")
    val light = (prefs.s(PreferenceKeys.MAP_LIGHT, "0").toFloatOrNull() ?: 0f).toInt()
    SGroup(Lang.t("Eigenes Design", "Own design"), footer = Lang.t("Wirkt sofort. Standard stellt die übliche Farbe wieder her.", "Applies right away. Standard restores the usual colour.")) {
        LiveDesignPreview(engine)
        val std = "" to Lang.t("Standard", "Standard")
        SColours(Lang.t("Routenfarbe", "Route colour"), listOf(std, "#4CC9FF" to Lang.t("Hellblau", "Light blue"), "#2A7BE4" to Lang.t("Blau", "Blue"), "#7B4FC9" to Lang.t("Violett", "Purple"), "#A56BFF" to Lang.t("Lila", "Purple"), "#1FC7A6" to Lang.t("Türkis", "Turquoise"),
            "#3DDC84" to Lang.t("Grün", "Green"), "#FFD54A" to Lang.t("Gelb", "Yellow"), "#FF9A3C" to Lang.t("Orange", "Orange"), "#FF6FB5" to Lang.t("Rosa", "Pink"), "#FFFFFF" to Lang.t("Weiß", "White")), route) { prefs.put(PreferenceKeys.ROUTE_COLOUR, it) }
        SColours(Lang.t("Pfeilfarbe", "Arrow colour"), listOf("#F2F2F6" to Lang.t("Weiß", "White"), "#C2C6D0" to Lang.t("Silber", "Silver"), "#4C8DFF" to Lang.t("Blau", "Blue"), "#6FD0FF" to Lang.t("Hellblau", "Light blue"), "#1FC7A6" to Lang.t("Türkis", "Turquoise"),
            "#3DDC84" to Lang.t("Grün", "Green"), "#FFD54A" to Lang.t("Gelb", "Yellow"), "#FF9A3C" to Lang.t("Orange", "Orange"), "#FF4D5E" to Lang.t("Rot", "Red"), "#FF6FB5" to Lang.t("Rosa", "Pink"), "#A56BFF" to Lang.t("Violett", "Purple")), arrow) { prefs.put(PreferenceKeys.ARROW_COLOUR, it) }
        SColours(Lang.t("Akzentfarbe", "Accent colour"), listOf(std, "#1A73E8" to Lang.t("Blau", "Blue"), "#8AB4F8" to Lang.t("Hellblau", "Light blue"), "#1FA39A" to Lang.t("Türkis", "Turquoise"), "#2E8B57" to Lang.t("Grün", "Green"), "#B97A00" to Lang.t("Gold", "Gold"),
            "#D9661A" to Lang.t("Orange", "Orange"), "#D7263D" to Lang.t("Rot", "Red"), "#D2508F" to Lang.t("Rosa", "Pink"), "#7B4FC9" to Lang.t("Violett", "Purple")), accentHex) { prefs.put(PreferenceKeys.ACCENT_COLOUR, it) }
        SSlider(Lang.t("Karte heller/dunkler", "Map lighter/darker"), light.toFloat(), -50f..50f, "%", reset = 0f) { prefs.put(PreferenceKeys.MAP_LIGHT, it.toInt().toString()) }
    }
}

/**
 * The own-design preview is the real map (as at the top of this page on a phone), with a short sample route when no trip is on, so
 * the route colour shows. The map view moves here and back, and the map screen takes it again when the settings close.
 */
@Composable
private fun LiveDesignPreview(engine: NavEngine) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        val before = engine.zoomOffset
        engine.recenter(byUser = true)
        engine.zoomOffset = (engine.zoomOffset - 1.0).coerceIn(-4.0, 3.0)
        val here = engine.fix?.pos
        val sample = engine.route == null && here != null
        if (sample && here != null) engine.map.setRoute(sampleRoute(here), emptyList())
        onDispose {
            engine.zoomOffset = before
            if (sample) { if (engine.route != null) engine.drawRoutes() else engine.map.setRoute(emptyList(), emptyList()) }
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(200.dp).clip(RoundedCornerShape(18.dp))) {
        MapHost(engine.map, Modifier.fillMaxSize(), cornerDp = 18)
    }
}

/** A short bent line ahead of [p] (about 200 m), only to show the route colour in the preview. */
private fun sampleRoute(p: com.headunit.launcher.LL): List<com.headunit.launcher.LL> = listOf(
    com.headunit.launcher.LL(p.lat - 0.0010, p.lon - 0.0012), com.headunit.launcher.LL(p.lat - 0.0002, p.lon - 0.0004),
    com.headunit.launcher.LL(p.lat + 0.0006, p.lon), com.headunit.launcher.LL(p.lat + 0.0010, p.lon + 0.0008),
    com.headunit.launcher.LL(p.lat + 0.0022, p.lon + 0.0012),
)

@Composable
private fun MapLookSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Design", footer = if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon") Lang.t("Neon ist immer ein Nachtdesign und ändert nur das Aussehen. Schrift: Rajdhani (SIL Open Font License).", "Neon is always a night look and only changes the appearance. Font: Rajdhani (SIL Open Font License).") else null) {
        SChoice(Lang.t("Aussehen", "Look"), listOf("standard" to Lang.t("Standard", "Standard"), "neon" to "Neon"), prefs.s(PreferenceKeys.LOOK, "standard"),
            Lang.t("Neon: dunkle Karte mit leuchtenden Linien, überall in der App", "Neon: dark map with glowing lines, throughout the app")) { prefs.put(PreferenceKeys.LOOK, it) }
        if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon")
            SChoice(Lang.t("Neonfarbe", "Neon colour"), listOf("yellow" to Lang.t("Gelb", "Yellow"), "cyan" to "Cyan", "magenta" to "Magenta"), prefs.s(PreferenceKeys.NEON_ACCENT, "yellow"),
                Lang.t("Farbe von Menüs und Hervorhebungen", "Colour of menus and highlights")) { prefs.put(PreferenceKeys.NEON_ACCENT, it) }
    }
    if (prefs.s(PreferenceKeys.LOOK, "standard") != "neon") OwnDesignGroup(prefs, engine)
    SGroup(Lang.t("Abbiegekarte", "Turn card")) {
        SChoice(Lang.t("Farbe", "Colour"), listOf("green" to Lang.t("Grün", "Green"), "black" to Lang.t("Schwarz", "Black"), "grey" to Lang.t("Dunkelgrau", "Dark grey"),
            "navy" to "Navy", "purple" to Lang.t("Lila", "Purple")), prefs.s(PreferenceKeys.TURN_CARD_COLOR, "green"),
            Lang.t("Die Farbe hinter der nächsten Abbiegung, mit weißer Schrift", "The colour behind the next turn, with white writing")) { prefs.put(PreferenceKeys.TURN_CARD_COLOR, it) }
    }
    SGroup(Lang.t("Tag und Nacht", "Day and night")) {
        SChoice(Lang.t("Tag und Nacht", "Day and night"), listOf("auto" to "Auto", "lights" to Lang.t("Scheinwerfer", "Headlights"), "sun" to Lang.t("Sonnenstand", "Sunrise/sunset"), "times" to Lang.t("Feste Zeiten", "Set times"), "day" to Lang.t("Tag", "Day"), "night" to Lang.t("Nacht", "Night"), "phone" to Lang.t("Dunkelmodus des Telefons", "Phone's dark mode")), prefs.s(PreferenceKeys.THEME, "auto"),
            Lang.t("Auto folgt dem Nachtmodus des Geräts", "Auto follows the device's dark mode")) { prefs.put(PreferenceKeys.THEME, it) }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "lights") {
            val learned = VendorLight.learned(prefs)
            SToggle(prefs, Lang.t("Lichtsignal umkehren", "Lights signal is reversed"), PreferenceKeys.LIGHTSREVERSED, false,
                if (learned.isEmpty()) Lang.t("Noch kein Lichtsignal des Geräts erkannt: Abblendlicht einschalten und kurz warten.", "No lights signal from the unit found yet: switch the headlights on and wait a moment.") else "Erkannt: $learned")
        }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "times") {
            SFieldItem(Lang.t("Nacht beginnt um", "Night starts at"), prefs.s(PreferenceKeys.NIGHT_FROM, "19:00"), Lang.t("24-Stunden-Format, z. B. 19:30", "24-hour format, e.g. 19:30"), placeholder = "19:00") { prefs.put(PreferenceKeys.NIGHT_FROM, it.trim()) }
            SFieldItem(Lang.t("Tag beginnt um", "Day starts at"), prefs.s(PreferenceKeys.DAY_FROM, "07:00"), Lang.t("24-Stunden-Format, z. B. 06:45", "24-hour format, e.g. 06:45"), placeholder = "07:00") { prefs.put(PreferenceKeys.DAY_FROM, it.trim()) }
        }
    }
    SGroup(Lang.t("Karte", "Map")) {
        SChoice(Lang.t("Kartenquelle", "Map source"), listOfNotNull(
            "osm" to "OpenStreetMap",
            if (engine.hasMapbox) "mapbox" to "Mapbox" else null,
            if (engine.hasKey) "tomtom" to "TomTom" else null,
        ), prefs.mapSource(),
            Lang.t("Mapbox: 3D-Karte. Benötigt das Mapbox-Token.", "Mapbox: 3D map. Needs the Mapbox token.")) { prefs.put(PreferenceKeys.MAP_STYLE, it) }
        if (prefs.mapSource() == "mapbox") {
            SChoice(Lang.t("Kartendetails", "Map detail"), listOf("full" to Lang.t("Vollständiges 3D", "Full 3D"), "lite" to "Lite", "lightest" to Lang.t("Einfachst", "Simplest")), prefs.mapDetail(),
                Lang.t("Lite: flache Gebäude. Einfachst: 2D-Karte.", "Lite: flat buildings. Simplest: 2D map.")) { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
            MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { prefs.put(PreferenceKeys.MAP_COLOURS, it) }
        }
        SToggle(prefs, Lang.t("Hinweis zu HD-Spuren", "HD lane note"), PreferenceKeys.HD_DIAG, false, Lang.t("Nur im Demo-Modus: zeigt, ob die Spurdaten für die Abbiegung gefunden wurden. Die Spurführung selbst bleibt unverändert", "Demo mode only: shows whether the lane data for the turn was found. The lane guidance itself stays unchanged"))
        if (prefs.mapSource() == "mapbox" && prefs.mapDetail() == "full") SChoice(Lang.t("HD-Straßen", "HD roads"), listOf("auto" to "Auto", "on" to "An", "off" to Lang.t("Aus", "Off")), prefs.s(PreferenceKeys.HD_ROADS2, "auto"),
            Lang.t("Fahrspuren, Abbiegepfeile und 3D-Brücken. Auto: nur in UK an, sonst zeigt die Karte in manchen Ländern nur die Hauptstraßen.", "Lane markings, turn arrows and 3D overpasses. Auto: on in the UK only; elsewhere some maps show only the main roads.")) { prefs.put(PreferenceKeys.HD_ROADS2, it) }
        DefaultZoomRow(prefs)
        SToggle(prefs, Lang.t("Fußwege und Pfade", "Footpaths and paths"), PreferenceKeys.PATHS, true, Lang.t("Ausschalten für eine übersichtlichere Fahrkarte", "Turn off for a clearer driving map"))
        SToggle(prefs, Lang.t("Favoriten auf der Karte", "Favourites on the map"), PreferenceKeys.FAV_ON_MAP, true, Lang.t("Gelbe Sterne mit Namen. Beim Fahren ohne Namen", "Yellow stars with names. Without names while driving"))
        SSlider(Lang.t("Symbole gespeicherter Orte", "Saved places icons"), prefs.i(PreferenceKeys.SAVED_ICON_SIZE, 100).toFloat(), 50f..200f, " %", Lang.t("Zuhause, Arbeit und markierte Orte", "Home, Work and starred places"), reset = 100f) { prefs.put(PreferenceKeys.SAVED_ICON_SIZE, it.toString()) }
        SToggle(prefs, Lang.t("Glaskarten", "Glass cards"), PreferenceKeys.GLASS, true, Lang.t("Halbtransparente Karten", "Semi-transparent cards"))
        SChoice(Lang.t("Mapbox-Logo-Position", "Mapbox logo position"), listOf("tl" to Lang.t("Oben links", "Top left"), "tr" to Lang.t("Oben rechts", "Top right"), "bl" to Lang.t("Unten links", "Bottom left"), "br" to Lang.t("Unten rechts", "Bottom right")), prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"),
            Lang.t("Ecke für das geforderte Mapbox-Logo wählen", "Corner for the required Mapbox logo")) { prefs.put(PreferenceKeys.MAPBOX_LOGO, it) }
    }
    SGroup(Lang.t("Wetter", "Weather")) {
        SToggle(prefs, Lang.t("Wettereffekte auf der Karte", "Weather effects on the map"), PreferenceKeys.RAIN_EFFECTS, false, Lang.t("Regen, Schnee und Nebel passend zum Wetter", "Rain, snow and fog to match the weather"))
        SToggle(prefs, Lang.t("Regen auf der Karte", "Rain on the map"), PreferenceKeys.FX_RAIN, true, Lang.t("Nur wenn die Wettereffekte an sind", "Only when weather effects are on"))
        SToggle(prefs, Lang.t("Schnee auf der Karte", "Snow on the map"), PreferenceKeys.FX_SNOW, true, Lang.t("Nur wenn die Wettereffekte an sind", "Only when weather effects are on"))
        SToggle(prefs, Lang.t("Nebel auf der Karte", "Fog on the map"), PreferenceKeys.FX_FOG, true, Lang.t("Nur wenn die Wettereffekte an sind", "Only when weather effects are on"))
        SToggle(prefs, Lang.t("Regenradar-Ebene", "Rain radar layer"), PreferenceKeys.RAIN_RADAR, false, Lang.t("Live-Radar von RainViewer unter der Route", "Live radar from RainViewer under the route"))
    }
    SGroup(Lang.t("3D und Satellit", "3D and satellite")) {
        SToggle(prefs, Lang.t("Satellit", "Satellite"), PreferenceKeys.SATELLITE, false)
        SToggle(prefs, Lang.t("3D-Gebäude", "3D buildings"), PreferenceKeys.BUILDINGS_3D, true)
        SToggle(prefs, Lang.t("Detaillierte Fassaden", "Detailed facades"), PreferenceKeys.FACADES, true, Lang.t("Fenster, Türen und Schaufenster an Gebäuden", "Windows, doors and shop fronts on buildings"))
        SToggle(prefs, Lang.t("3D-Sehenswürdigkeiten", "3D landmarks"), PreferenceKeys.LANDMARKS3D, true, Lang.t("Detaillierte Modelle bekannter Gebäude und Stadien", "Detailed models of well-known buildings and stadiums"))
        SToggle(prefs, Lang.t("3D-Berge und Täler", "3D hills and valleys"), PreferenceKeys.TERRAIN, false, Lang.t("Echtes Gelände", "Real terrain"))
        SToggle(prefs, Lang.t("Nebel und Himmel", "Fog and sky"), PreferenceKeys.SKYWEATHER, true, Lang.t("Dunst am Horizont passend zum Wetter", "Haze on the horizon to match the weather"))
        SToggle(prefs, Lang.t("Nachtsternenhimmel in 3D", "3D night sky with stars"), PreferenceKeys.NIGHTSKY, true, Lang.t("Sterne über dem Horizont bei Nacht", "Stars above the horizon at night"))
        SToggle(prefs, Lang.t("Bäume in Parks", "Trees in parks"), PreferenceKeys.TREES, true, Lang.t("3D-Bäume in Parks und an Straßen", "3D trees in parks and along roads"))
        SToggle(prefs, Lang.t("Freie Sicht auf die Straße voraus", "Clear view of the road ahead"), PreferenceKeys.CLEARVIEW, true, Lang.t("Bäume und Gebäude verdecken die Straße nicht", "Trees and buildings don't cover the road"))
        SToggle(prefs, Lang.t("Tippen zum Hervorheben", "Tap to highlight"), PreferenceKeys.TAPSELECT, true, Lang.t("Tippe auf ein Gebäude oder einen Ort zum Auswählen", "Tap a building or place to select it"))
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup(Lang.t("Verkehr", "Traffic")) {
        SToggle(prefs, Lang.t("Live-Verkehr", "Live traffic"), PreferenceKeys.TRAFFIC, true)
        SToggle(prefs, Lang.t("Verkehrsübersicht beim Halt", "Traffic overview when stopped"), PreferenceKeys.STOPPED_OVERVIEW, true, Lang.t("Zoomt nach 30 s Stillstand heraus", "Zooms out after 30 s standing still"))
        SToggle(prefs, Lang.t("Verkehr nur auf der Route anzeigen", "Show traffic on the route only"), PreferenceKeys.ROUTETRAFFICONLY, false, Lang.t("Aus zeigt auch Verkehr auf umliegenden Straßen", "Off also shows traffic on nearby roads"))
        SToggle(prefs, Lang.t("Grüner Verkehr bei freier Fahrt", "Green traffic when clear"), PreferenceKeys.TRAFFIC_GREEN, false, Lang.t("Frei fließende Hauptstraßen grün anzeigen", "Show free-flowing main roads in green"))
        SToggle(prefs, Lang.t("Verkehrsfarben fließend", "Blend traffic colours"), PreferenceKeys.TRAFFIC_BLEND, true, Lang.t("Die Verkehrsfarben auf der Route und der Routenleiste gehen ineinander über, statt hart zu wechseln", "The traffic colours on the route and the route bar run into each other instead of changing in one step"))
    }
    SGroup(Lang.t("Auf der Route", "On the route")) {
        SToggle(prefs, Lang.t("Ampeln", "Traffic lights"), PreferenceKeys.SIGNALS, true, if (prefs.b(PreferenceKeys.SIGNALS, true)) Lang.t("Nur Ampeln auf deiner Route. Aktuell: ", "Only traffic lights on your route. Now: ") + engine.map.signalInfo() else null)
        SToggle(prefs, Lang.t("Blitzer und Baustellen", "Cameras and roadworks"), PreferenceKeys.HAZARD_ICONS, true)
        SToggle(prefs, Lang.t("Zebrastreifen", "Zebra crossings"), PreferenceKeys.ZEBRAS, true, Lang.t("Auf der Straße gezeichnet", "Drawn on the road"))
        SToggle(prefs, Lang.t("Stoppschilder", "Stop signs"), PreferenceKeys.STOP_SIGNS, false, Lang.t("Mit kurzem Ton", "With a short tone"))
        SToggle(prefs, Lang.t("Hinweis „Vorfahrt gewähren“", "\"Give way\" alerts"), PreferenceKeys.GIVE_WAY, false, Lang.t("Standardmäßig aus", "Off by default"))
    }
    SGroup(Lang.t("3D-Straßenausstattung", "3D street furniture")) {
        SToggle(prefs, Lang.t("3D-Straßenlaternen, Ampeln und Kameras", "3D street lights, traffic lights and cameras"), PreferenceKeys.FURNITURE3D, true, Lang.t("3D-Modelle in echter Größe", "Life-size 3D models"))
        SToggle(prefs, Lang.t("Straßenlaternen", "Street lights"), PreferenceKeys.LAMPS, false, if (prefs.b(PreferenceKeys.LAMPS, false)) Lang.t("Aktuell: ", "Now: ") + engine.map.lampInfo() else null)
        if (prefs.b(PreferenceKeys.LAMPS, false)) SToggle(prefs, "Laternen aus OpenStreetMap", PreferenceKeys.LAMPS_ONLINE, true, "Echte Laternenstandorte, wo Leute sie eingetragen haben. Einmal pro Gebiet geladen, einen Monat gespeichert")
        if (prefs.b(PreferenceKeys.LAMPS, false)) SSlider(Lang.t("Laternenabstand", "Street light spacing"), prefs.s(PreferenceKeys.LAMPGAP, "40").toFloatOrNull() ?: 40f, 15f..120f, " m", reset = 40f) { prefs.put(PreferenceKeys.LAMPGAP, it.toString()) }
    }
}

// ------------------------------------------------------------------ Your arrow & route line

@Composable
private fun ArrowSection(prefs: Prefs, engine: NavEngine) {
    SGroup(Lang.t("Pfeil", "Arrow"), footer = Lang.t("Flacher Pfeil: am schonendsten für langsame Geräte", "Flat arrow: lightest for slow devices")) {
        val loc = prefs.s(PreferenceKeys.LOCATOR, "arrow")
        val ctx = LocalContext.current
        val hasPic = prefs.s(PreferenceKeys.LOCATOR_IMAGE, "").isNotBlank() && java.io.File(ctx.filesDir, "locator.png").exists()
        val pickPic = rememberLocatorImagePicker { ok ->
            if (ok) { prefs.put(PreferenceKeys.LOCATOR_IMAGE, System.currentTimeMillis().toString()); prefs.put(PreferenceKeys.LOCATOR, "custom") }
            else android.widget.Toast.makeText(ctx, Lang.t("Bild nicht lesbar (PNG oder JPG verwenden)", "Picture unreadable (use PNG or JPG)"), android.widget.Toast.LENGTH_SHORT).show()
        }
        fun choosePic() { if (!pickPic()) android.widget.Toast.makeText(ctx, Lang.t("Keine App auf diesem Gerät kann ein Bild auswählen", "No app on this device can pick a picture"), android.widget.Toast.LENGTH_LONG).show() }
        SChoice(Lang.t("Positionsanzeige", "Position marker"), listOf("arrow" to Lang.t("3D-Pfeil", "3D arrow"), "car" to "Auto", "model" to Lang.t("Echtes 3D-Modell", "Real 3D model"), "flat" to Lang.t("Flacher Pfeil", "Flat arrow"), "custom" to Lang.t("Eigenes Bild", "Your own picture")), loc) {
            if (it == "custom" && !hasPic) choosePic() else prefs.put(PreferenceKeys.LOCATOR, it)
        }
        if (loc == "model") {
            SChoice(Lang.t("Modellform", "Model shape"), listOf("arrow" to Lang.t("Pfeil", "Arrow"), "car" to "Auto", "own" to Lang.t("Dein Modell", "Your model")), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) { prefs.put(PreferenceKeys.MODEL_SHAPE, it) }
            if (prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "own") {
                // your own model (.glb, up to 48 MB; over 8 MB its pictures are made smaller): sized to match the built-in car; the built-in car stands in while there's none
                val hasOwn = com.headunit.launcher.OwnModel.factor(ctx) != null
                val ownName = prefs.s(PreferenceKeys.OWN_MODEL_NAME, "")
                val pickOwn = rememberOwnModelPicker { ok, name ->
                    if (ok) {
                        prefs.put(PreferenceKeys.OWN_MODEL_NAME, name.ifBlank { "3D" })
                        prefs.put(PreferenceKeys.OWN_MODEL_STAMP, System.currentTimeMillis().toString())
                    } else android.widget.Toast.makeText(ctx, Lang.t("Modell nicht lesbar (eine .glb-Datei, höchstens 48 MB)", "Model can't be read (a .glb file, up to 48 MB)"), android.widget.Toast.LENGTH_LONG).show()
                }
                SItem(Lang.t("Dein Modell", "Your model"), if (hasOwn) "Aktuell: $ownName" else Lang.t("Noch keins gewählt · eine .glb-Datei, höchstens 48 MB (größer als 8 MB: die Bilder darin werden verkleinert). Bis dahin steht das eingebaute Auto da.", "None chosen yet · a .glb file, up to 48 MB (over 8 MB its pictures are made smaller). Until then the built-in car stands in.")) {
                    SButton(if (hasOwn) Lang.t("Ersetzen", "Replace") else Lang.t("Datei wählen", "Choose file"), primary = true) {
                        if (!pickOwn()) android.widget.Toast.makeText(ctx, Lang.t("Keine App auf diesem Gerät kann eine Datei auswählen", "No app on this device can choose a file"), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                if (hasOwn) {
                    SSlider(Lang.t("Größe des Modells", "Model size"), prefs.i(PreferenceKeys.OWN_MODEL_SIZE, 100).toFloat(), 30f..300f, "%", reset = 100f) { prefs.put(PreferenceKeys.OWN_MODEL_SIZE, it.toInt()) }
                    SChoice(Lang.t("Drehen", "Turn"), listOf("0" to "0°", "90" to "90°", "180" to "180°", "270" to "270°"), prefs.i(PreferenceKeys.OWN_MODEL_TURN, 0).toString(),
                        Lang.t("Dreht das Modell, bis es in Fahrtrichtung zeigt", "Turns the model until it points the way you drive")) { prefs.put(PreferenceKeys.OWN_MODEL_TURN, it.toInt()) }
                }
            }
            SToggle(prefs, Lang.t("Leuchten", "Glow"), PreferenceKeys.MODEL_GLOW, true, Lang.t("Weiches blaues Leuchten um das Modell", "Soft blue glow round the model"))
            if (prefs.mapDetail() != "full") SNote(Lang.t("Nur mit Kartendetails „Vollständiges 3D“", "Only with map detail \"Full 3D\""))
        }
        SToggle(prefs, Lang.t("Scheinwerferkegel bei Nacht", "Headlight beam at night"), PreferenceKeys.HEADLIGHT_BEAM, false, Lang.t("Nach Einbruch der Dunkelheit liegt ein weicher Lichtkegel vor dem Pfeil, bei höherem Tempo länger", "After dark a soft cone of light lies on the road ahead of your arrow, longer at speed"))
        val shapeCar = loc == "car" || (loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "car")
        val shapeArrow = loc == "arrow" || (loc == "model" && !shapeCar)
        if (loc == "custom") {
            SItem(Lang.t("Bild", "Picture"), Lang.t("Am besten PNG mit transparentem Hintergrund", "Best as a PNG with a transparent background")) { SButton(if (hasPic) Lang.t("Ändern", "Change") else Lang.t("Wählen", "Choose")) { choosePic() } }
            SToggle(prefs, Lang.t("Dreht sich mit", "Turns with you"), PreferenceKeys.LOCATOR_IMAGE_TURNS, true, Lang.t("Dreht sich beim Fahren wie der Pfeil", "Turns as you drive, like the arrow"))
            SToggle(prefs, Lang.t("Rund zugeschnitten", "Round crop"), PreferenceKeys.LOCATOR_IMAGE_CIRCLE, false, Lang.t("Schneidet das Bild kreisförmig zu", "Crops the picture to a circle"))
            SToggle(prefs, Lang.t("Weißen Hintergrund entfernen", "Remove white background"), PreferenceKeys.LOCATOR_REMOVE_WHITE, true, Lang.t("Ein Bild mit weißem Hintergrund wird am Rand durchsichtig", "A picture with a white background becomes see-through at its edge"))
        }
        if (shapeCar) {
            val cur = prefs.s(PreferenceKeys.CAR_MODEL, "saloon")
            val colour = prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")
            val names = listOf("saloon" to Lang.t("Limousine", "Saloon"), "hatch" to Lang.t("Schrägheck", "Hatchback"), "estate" to Lang.t("Kombi", "Estate"), "suv" to "SUV", "pickup" to "Pickup", "van" to "Van", "sports" to Lang.t("Sportwagen", "Sports car"))
            SItem(Lang.t("Fahrzeugtyp", "Vehicle type"), names.firstOrNull { it.first == cur }?.second)
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((id, name) in names) {
                    val pic = remember(id, colour) { try { engine.map.carPreview(id, colour).asImageBitmap() } catch (e: Throwable) { null } }
                    val sel = id == cur
                    androidx.compose.foundation.layout.Column(
                        Modifier.width(92.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) LocalVw.current.accent.copy(alpha = 0.22f) else LocalVw.current.tile)
                            .border(if (sel) 2.dp else 0.dp, if (sel) LocalVw.current.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { prefs.put(PreferenceKeys.CAR_MODEL, id) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (pic != null) androidx.compose.foundation.Image(pic, name, Modifier.size(80.dp))
                        Text(name, color = LocalVw.current.text, fontSize = 12.sp)
                    }
                }
            }
        }
        if (shapeCar) SColours(Lang.t("Fahrzeugfarbe", "Vehicle colour"), listOf(
            "#F4F5F7" to Lang.t("Weiß", "White"), "#B9BEC8" to Lang.t("Silber", "Silver"), "#7A808C" to Lang.t("Grau", "Grey"), "#3E434D" to Lang.t("Graphit", "Graphite"), "#2A2D34" to Lang.t("Schwarz", "Black"), "#1E2A4A" to Lang.t("Dunkelblau", "Dark blue"),
            "#2F6BDF" to Lang.t("Blau", "Blue"), "#5FB3E8" to Lang.t("Hellblau", "Light blue"), "#1FA39A" to Lang.t("Türkis", "Turquoise"), "#2E8B57" to Lang.t("Grün", "Green"), "#6E8B3D" to Lang.t("Oliv", "Olive"), "#F2C230" to Lang.t("Gelb", "Yellow"),
            "#F07C28" to Lang.t("Orange", "Orange"), "#D7263D" to Lang.t("Rot", "Red"), "#8E1B2E" to Lang.t("Weinrot", "Burgundy"), "#7B4FC9" to Lang.t("Violett", "Purple"), "#E86FA8" to Lang.t("Rosa", "Pink"), "#C9A27A" to Lang.t("Champagner", "Champagne"), "#6B4A35" to Lang.t("Braun", "Brown"),
        ), prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")) { prefs.put(PreferenceKeys.CAR_COLOUR, it) }
        else if (shapeArrow) SColours(Lang.t("Pfeilfarbe", "Arrow colour"), listOf(
            "#F2F2F6" to Lang.t("Weiß", "White"), "#C2C6D0" to Lang.t("Silber", "Silver"), "#8A909C" to Lang.t("Grau", "Grey"), "#3E434D" to Lang.t("Graphit", "Graphite"), "#4C8DFF" to Lang.t("Blau", "Blue"), "#6FD0FF" to Lang.t("Hellblau", "Light blue"),
            "#1FC7A6" to Lang.t("Türkis", "Turquoise"), "#3DDC84" to Lang.t("Grün", "Green"), "#B5E34A" to Lang.t("Limette", "Lime"), "#FFD54A" to Lang.t("Gelb", "Yellow"), "#FF9A3C" to Lang.t("Orange", "Orange"), "#FF4D5E" to Lang.t("Rot", "Red"),
            "#FF6FB5" to Lang.t("Rosa", "Pink"), "#A56BFF" to Lang.t("Violett", "Purple"),
        ), prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")) { prefs.put(PreferenceKeys.ARROW_COLOUR, it) }
        SSlider(if (shapeCar) Lang.t("Fahrzeuggröße", "Vehicle size") else if (loc == "custom") Lang.t("Bildgröße", "Picture size") else Lang.t("Pfeilgröße", "Arrow size"), prefs.s(PreferenceKeys.ARROW_PERCENT, "150").toFloatOrNull() ?: 150f, 100f..500f, "%", reset = 150f) { prefs.put(PreferenceKeys.ARROW_PERCENT, it.toString()) }
    }
    SGroup(Lang.t("Routenlinie", "Route line")) {
        SToggle(prefs, Lang.t("Abbiegepfeil auf der Route", "Turn arrow on the route"), PreferenceKeys.TURNARROWS, true, Lang.t("Ein weißer Pfeil durch die nächste Kreuzung", "A white arrow through the next junction"))
        SToggle(prefs, Lang.t("Pfeile entlang der Route", "Arrows along the route"), PreferenceKeys.ROUTECHEVRONS, true, Lang.t("Kleine Pfeile entlang der Routenlinie beim Zoomen", "Small arrows along the route line when zoomed in"))
        SToggle(prefs, Lang.t("Routenschattenband", "Route shadow band"), PreferenceKeys.RIBBON, true, Lang.t("Ein weicher Schatten unter der Route", "A soft shadow under the route"))
        SToggle(prefs, Lang.t("3D-Fahnen", "3D flags"), PreferenceKeys.FLAGS_3D, true, Lang.t("Fahnen an Ziel und Stopps", "Flags at destination and stops"))
        if (prefs.b(PreferenceKeys.FLAGS_3D, true)) SToggle(prefs, Lang.t("Fahnen wehen", "Waving flags"), PreferenceKeys.FLAGS_WAVE, true, Lang.t("Fahnen bewegen sich im Wind", "Flags move in the wind"))
        SToggle(prefs, Lang.t("Fließendes Licht auf der Route", "Flowing light on the route"), PreferenceKeys.ROUTEFLOW, true)
        SColours(Lang.t("Farbe alternativer Routen", "Colour of other routes"), listOf(
            "#C4D9E6" to Lang.t("Hellblau-Grau (Standard)", "Light blue-grey (default)"), "#A9D4F0" to Lang.t("Hellblau", "Light blue"), "#5AB7F5" to Lang.t("Blau", "Blue"), "#315CBA" to Lang.t("Dunkelblau", "Dark blue"),
            "#A9B4C2" to Lang.t("Silbergrau", "Silver grey"), "#FFFFFF" to Lang.t("Weiß", "White"), "#42CFA5" to Lang.t("Minze", "Mint"), "#F4C542" to Lang.t("Gelb", "Yellow"),
            "#FF9A4A" to Lang.t("Orange", "Orange"), "#F05D6C" to Lang.t("Rot", "Red"), "#C58AF9" to Lang.t("Violett", "Purple"), "#F08AC5" to Lang.t("Rosa", "Pink"),
        ), prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")) { prefs.put(PreferenceKeys.ALT_ROUTE_COLOUR, it) }
    }
}

// ------------------------------------------------------------------ Routes

@Composable
private fun RoutesSection(prefs: Prefs) {
    VehicleGroup(prefs)
    SGroup(Lang.t("Vermeiden", "Avoid"), footer = Lang.t("Änderungen gelten für die nächste geplante Route.", "Changes apply to the next route you plan.")) {
        SToggle(prefs, Lang.t("Mautstraßen vermeiden", "Avoid toll roads"), PreferenceKeys.AVOID_TOLL, false, if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) Lang.t("Überall", "Everywhere") else null)
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) {
            val chosen = prefs.s(PreferenceKeys.AVOID_TOLL_COUNTRIES, "").split(',').filter { it.isNotBlank() }.toSet()
            SItem(Lang.t("Maut vermeiden nur in", "Avoid tolls only in"), if (chosen.isEmpty()) Lang.t("Maut nur in diesen Ländern vermeiden", "Avoid tolls only in these countries") else Lang.t("In diesen Ländern ohne Maut planen", "Plan without tolls in these countries"))
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((cc, name) in listOf("FR" to Lang.t("Frankreich", "France"), "IT" to Lang.t("Italien", "Italy"), "ES" to Lang.t("Spanien", "Spain"), "PT" to "Portugal", "AT" to Lang.t("Österreich", "Austria"), "CH" to Lang.t("Schweiz", "Switzerland"),
                    "SI" to Lang.t("Slowenien", "Slovenia"), "HR" to Lang.t("Kroatien", "Croatia"), "PL" to Lang.t("Polen", "Poland"), "CZ" to Lang.t("Tschechien", "Czechia"), "HU" to Lang.t("Ungarn", "Hungary"), "GR" to Lang.t("Griechenland", "Greece"), "IE" to Lang.t("Irland", "Ireland"), "NO" to Lang.t("Norwegen", "Norway"), "US" to "USA")) {
                    Pill(name, cc in chosen, { prefs.put(PreferenceKeys.AVOID_TOLL_COUNTRIES, (if (cc in chosen) chosen - cc else chosen + cc).joinToString(",")) })
                }
            }
        }
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) SToggle(prefs, Lang.t("Vignettenpflichtige Straßen meiden", "Avoid vignette roads"), PreferenceKeys.AVOID_VIGNETTE, false, Lang.t("Nur bei Durchfahrt (AT, CH, SI, CZ, SK, HU, BG, RO)", "Only when passing through (AT, CH, SI, CZ, SK, HU, BG, RO)"))
        SToggle(prefs, Lang.t("Autobahnen vermeiden", "Avoid motorways"), PreferenceKeys.AVOID_MOTORWAY, false)
        SToggle(prefs, Lang.t("Fähren vermeiden", "Avoid ferries"), PreferenceKeys.AVOID_FERRY, false)
        SToggle(prefs, Lang.t("Autozüge vermeiden", "Avoid car trains"), PreferenceKeys.AVOID_CARTRAIN, false, Lang.t("Z. B. Eurotunnel und die Alpen-Tunnelzüge. Ist ein Autozug der einzige Weg, wird er trotzdem genommen", "Such as the Channel Tunnel shuttle and the Alpine tunnel trains. If a car train is the only way there, it is still used"))
        SToggle(prefs, Lang.t("Unbefestigte Straßen vermeiden", "Avoid unpaved roads"), PreferenceKeys.AVOID_UNPAVED, true)
        SToggle(prefs, Lang.t("Umweltzonen vermeiden", "Avoid low-emission zones"), PreferenceKeys.AVOIDZONES, false, Lang.t("Eine Start- oder Zielzone kann nicht vermieden werden", "A zone at the start or destination can't be avoided"))
        ZonesItem()
    }
    SGroup(Lang.t("Routenauswahl", "Route choice")) {
        SToggle(prefs, Lang.t("Öko-Routen", "Eco routes"), PreferenceKeys.ROUTE_ECO, false, Lang.t("Kraftstoffsparend statt am schnellsten", "Fuel-saving instead of fastest"))
        SToggle(prefs, Lang.t("Schnellere Routen anbieten", "Offer faster routes"), PreferenceKeys.FASTER_ROUTE, true)
        SToggle(prefs, "Einfachere Routen bevorzugen", PreferenceKeys.SIMPLER_ROUTE, true, "Eine Route, die nur etwa eine Minute langsamer ist, aber viel weniger Abbiegungen hat, wird zuerst gewählt")
        SToggle(prefs, Lang.t("Auf der Bordsteinseite ankommen", "Arrive on the kerb side"), PreferenceKeys.CURB_ARRIVAL, true, Lang.t("Ankunft auf der Straßenseite des Ziels (Mapbox)", "Arrive on the destination's side of the road (Mapbox)"))
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) {
            SToggle(prefs, Lang.t("Automatisch umleiten", "Reroute automatically"), PreferenceKeys.AUTO_REROUTE, true, Lang.t("Wechselt mit Ansage auf eine schnellere Route", "Switches to a faster route with an announcement"))
            if (prefs.b(PreferenceKeys.AUTO_REROUTE, true)) {
                val m = prefs.i(PreferenceKeys.AUTO_REROUTE_MIN, 5)
                SItem(Lang.t("Umleiten ab Ersparnis von", "Reroute when it saves at least"), Lang.t("Mindestersparnis der neuen Route", "Minimum time saved by the new route"))
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in listOf(3, 5, 10, 15)) Pill("$v Min", m == v, { prefs.put(PreferenceKeys.AUTO_REROUTE_MIN, v) })
                }
            }
        }
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, Lang.t("Andere Routen auf der Karte anzeigen", "Show other routes on the map"), PreferenceKeys.LIVE_ALTERNATIVES, true, Lang.t("Graue Linien mit +/- Minuten zum Antippen", "Grey lines with +/- minutes to tap"))
        SToggle(prefs, Lang.t("Ziele vorschlagen", "Suggest destinations"), PreferenceKeys.PREDICT, true, Lang.t("Mit Live-Fahrzeit für Zuhause und Arbeitsplatz", "With live travel time for Home and Work"))
        SToggle(prefs, Lang.t("Wetter auf der Route", "Weather on the route"), PreferenceKeys.ROUTEWX, true)
    }
    SGroup(Lang.t("Routenstart", "Route start")) {
        SToggle(prefs, Lang.t("Auto-Start-Countdown", "Auto-start countdown"), PreferenceKeys.AUTO_START_ROUTE, true, Lang.t("Startet die Navigation nach dem Countdown", "Starts navigation after the countdown"))
        if (prefs.b(PreferenceKeys.AUTO_START_ROUTE, true)) SSlider(Lang.t("Auto-Start-Countdown", "Auto-start countdown"), prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30).toFloat(), 3f..30f, " s", sub = Lang.t("Dauer bis zum Start der Navigation", "Time until navigation starts"), steps = 26, reset = 10f) { prefs.put(PreferenceKeys.AUTO_START_SECONDS, it.coerceIn(3, 30)) }
        SToggle(prefs, Lang.t("3D-Flug entlang neuer Route", "3D flight along a new route"), PreferenceKeys.CINEMATIC, true, Lang.t("Ein 3-Sekunden-3D-Flug vor der Übersicht", "A 3-second 3D flight before the overview"))
        SToggle(prefs, Lang.t("Routenflug-Button", "Route flight button"), PreferenceKeys.FLYOVER, true, Lang.t("3D-Flug über die gesamte Strecke vor der Abfahrt", "3D flight over the whole route before you leave"))
    }
}

// ------------------------------------------------------------------ Turn card & lanes

@Composable
private fun GuidanceSection(prefs: Prefs) {
    SGroup(Lang.t("Abbiegekarte", "Turn card")) {
        SToggle(prefs, Lang.t("Straßenschilder auf der Abbiegekarte", "Road signs on the turn card"), PreferenceKeys.UKSIGNS, true)
        SToggle(prefs, Lang.t("Lebendige Abbiegekarte", "Living turn card"), PreferenceKeys.LIVECARD, true, Lang.t("Wächst und leuchtet bei Annäherung an das Abbiegen", "Grows and glows as you near the turn"))
        SToggle(prefs, Lang.t("Spurführung auf der Abbiegekarte", "Lane guidance on the turn card"), PreferenceKeys.LANE_STRIP, true)
        if (prefs.b(PreferenceKeys.LANE_STRIP, true)) SChoice(Lang.t("Spurführungsstil", "Lane guidance style"), listOf("hero" to Lang.t("Große Spurenanzeige", "Large lane display"), "classic" to Lang.t("Klassischer Streifen", "Classic strip")), prefs.s(PreferenceKeys.LANE_STYLE, "hero"),
            if (prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero") Lang.t("Großes Spurfoto vor Kreuzungen", "Large lane picture before junctions") else Lang.t("Kleine Spurenleiste bei jeder Kreuzung", "Small lane strip at every junction")) { prefs.put(PreferenceKeys.LANE_STYLE, it) }
        SChoice(Lang.t("Nächstes Abbiegen auf der Karte", "Next turn on the card"), listOf("close" to Lang.t("Wenn nah", "When close"), "always" to Lang.t("Immer", "Always"), "off" to Lang.t("Aus", "Off")), prefs.s(PreferenceKeys.THEN_MODE, "close"),
            when (prefs.s(PreferenceKeys.THEN_MODE, "close")) { "close" -> Lang.t("Nur wenn der übernächste Schritt direkt folgt", "Only when the step after next follows straight away"); "always" -> Lang.t("Immer der übernächste Schritt", "Always the step after next"); else -> null }) { prefs.put(PreferenceKeys.THEN_MODE, it) }
        if (prefs.s(PreferenceKeys.THEN_MODE, "close") != "off") SChoice(Lang.t("Anzeigen als", "Show as"), listOf("row" to Lang.t("Zeile unter der Karte", "Line under the card"), "icon" to Lang.t("Kleiner Pfeil", "Small arrow")), prefs.s(PreferenceKeys.THEN_STYLE, "row")) { prefs.put(PreferenceKeys.THEN_STYLE, it) }
    }
    SGroup(Lang.t("Spuren und Kreuzungen", "Lanes and junctions")) {
        SToggle(prefs, Lang.t("Spuren auf der Karte gezeichnet", "Lanes drawn on the map"), PreferenceKeys.LANEMAP, true)
        SToggle(prefs, Lang.t("Mapbox-Spurdaten auf TomTom-Routen nutzen", "Use Mapbox lane data on TomTom routes"), PreferenceKeys.LANES_MAPBOX, true)
        SToggle(prefs, Lang.t("Heranzoomen an Spuren vor einer Kreuzung", "Zoom in on the lanes before a junction"), PreferenceKeys.LANE_VIEW, true)
        SToggle(prefs, Lang.t("Kreuzungs-Nahansicht", "Junction close-up"), PreferenceKeys.JUNCTION_VIEW, true, Lang.t("Nähere, flachere Ansicht bei Ausfahrten und Gabelungen", "A closer, flatter view at exits and forks"))
        SToggle(prefs, Lang.t("Karte beim Abbiegen auf 2D flachlegen", "Lay the map flat (2D) when turning"), PreferenceKeys.TURN2D, true)
        SToggle(prefs, Lang.t("Neigung nach Geschwindigkeit", "Tilt by speed"), PreferenceKeys.SPEEDTILT, true, Lang.t("Flacher bei langsamer Fahrt, weitblickender bei Tempo", "Flatter when slow, looking further ahead at speed"))
        TiltSetting(prefs, Lang.t("3D-Neigung, breiter Bildschirm", "3D tilt, wide screen"), PreferenceKeys.TILT_WIDE)
        TiltSetting(prefs, Lang.t("3D-Neigung, hoher Bildschirm", "3D tilt, tall screen"), PreferenceKeys.TILT_TALL)
    }
    SGroup(Lang.t("Fahrtenkarte", "Trip card")) {
        SToggle(prefs, Lang.t("Routenleiste auf der Fahrtenkarte", "Route bar on the trip card"), PreferenceKeys.ROUTEBAR, true, Lang.t("Verkehr, Kameras, Raststätten und Fortschritt", "Traffic, cameras, services and progress"))
        SToggle(prefs, Lang.t("Nächste Raststätten-Karte", "Next services card"), PreferenceKeys.SERVICESCARD, true)
        SToggle(prefs, Lang.t("Gefahrenradar", "Hazard radar"), PreferenceKeys.HAZARDRADAR, false, Lang.t("Kurven, Schulen, Schwellen, Blitzer (2 km)", "Bends, schools, bumps, cameras (2 km)"))
    }
    SGroup(Lang.t("Ankunft", "Arrival")) {
        SToggle(prefs, Lang.t("Ankunftsmoment", "Arrival moment"), PreferenceKeys.ARRIVALFX, true, Lang.t("Kamera schwenkt zum Ziel und zeigt Parkplätze", "The camera turns to the destination and shows parking"))
        SToggle(prefs, Lang.t("Parkplätze nahe dem Ziel", "Parking near the destination"), PreferenceKeys.PARKPROMPT, false, Lang.t("Parkplatzvorschläge vor der Ankunft", "Parking suggestions before you arrive"))
        SToggle(prefs, Lang.t("Ziel beleuchten", "Light up your destination"), PreferenceKeys.LIGHT_DEST, true, Lang.t("Das Gebäude bleibt bei der Ankunft beleuchtet, der Eingang bekommt eine Markierung, eine gepunktete Linie zeigt den Fußweg vom Parkplatz", "The building stays lit on arrival, its entrance gets a marker and a dotted line shows the walk from the parking"))
        SToggle(prefs, Lang.t("Rückroute", "Way back"), PreferenceKeys.ROUTEBACK, true, Lang.t("Ein Tipp für den Weg zurück zum Startort", "A tip for the way back to where you started"))
        SToggle(prefs, Lang.t("Parkplatz über Bluetooth merken", "Remember where you parked via Bluetooth"), PreferenceKeys.PARK_BLUETOOTH, true, Lang.t("Speichert den Standort beim Trennen vom Auto-Bluetooth", "Saves the location when car Bluetooth disconnects"))
    }
}


/** Checks the Open Charge Map key, and shows what the last charger lookup found. */
@Composable
internal fun OcmTestItem(prefs: Prefs) {
    val sc = rememberCoroutineScope()
    var st by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val last = com.headunit.launcher.ChargerInfo.lastNote
    val sub = st ?: if (last.isNotBlank()) "Letzte Abfrage: $last" else Lang.t("Prüft den Schlüssel mit einer kleinen Abfrage", "Checks the key with a small request")
    SItem(Lang.t("Ladesäulen-Daten prüfen", "Check charger data"), sub) {
        SButton(if (busy) Lang.t("Prüft…", "Checking…") else Lang.t("Prüfen", "Check")) {
            if (!busy) { busy = true; sc.launch {
                val key = prefs.s(PreferenceKeys.OCM_KEY, "")
                st = withContext(Dispatchers.IO) { com.headunit.launcher.ChargerInfo.testOcm(key) }
                busy = false
            } }
        }
    }
}

/** What the app knows about clean air zones, with a button to download them again. */
@Composable
private fun ZonesItem() {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    var st by remember { mutableStateOf(Zones.all(ctx).let { z -> if (z.isEmpty()) Lang.t("Noch nicht heruntergeladen", "Not downloaded yet") else "${z.size} Zonen: " + z.joinToString(", ") { it.name }.take(140) }) }
    var busy by remember { mutableStateOf(false) }
    SItem(Lang.t("Umweltzonen auf diesem Gerät", "Low-emission zones on this device"), st) {
        SButton(if (busy) Lang.t("Sucht…", "Searching…") else Lang.t("Aktualisieren", "Update")) {
            if (!busy) { busy = true; sc.launch { withContext(Dispatchers.IO) { Zones.fetch(ctx) }; st = Zones.status; busy = false } }
        }
    }
}

// ------------------------------------------------------------------ Alerts

@Composable
private fun AlertsSection(prefs: Prefs, engine: NavEngine) {
    SGroup(Lang.t("Geschwindigkeit", "Speed")) {
        SToggle(prefs, Lang.t("Warnen bei Überschreitung", "Warn when speeding"), PreferenceKeys.SPEEDWARN, false)
        if (prefs.b(PreferenceKeys.SPEEDWARN, false)) SChoice(Lang.t("Warnen ab Überschreitung um", "Warn when over by"), listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        if (prefs.b(PreferenceKeys.SPEEDWARN, false)) SToggle(prefs, Lang.t("„Langsamer fahren“ sagen", "Say \"Slow down\""), PreferenceKeys.SAY_SLOW, false, Lang.t("Nach dem Ton beim Überschreiten, einmal pro Überschreitung", "After the beep when you go over, once each time"))
        SChoice(Lang.t("Eigene Höchstgeschwindigkeit", "Top speed alert"), listOf("0" to Lang.t("Aus", "Off")) + (if (engine.imperial) listOf(50, 60, 70, 80, 90) else listOf(80, 100, 120, 130, 150, 180)).map { "$it" to "$it " + if (engine.imperial) "mph" else "km/h" },
            prefs.i(PreferenceKeys.TOP_SPEED, 0).toString(), Lang.t("Piept und sagt einmal „Langsamer fahren“, wenn du sie erreichst, egal welches Limit gilt", "Beeps and says \"Slow down\" once as you reach it, whatever the limit is")) { prefs.put(PreferenceKeys.TOP_SPEED, it.toInt()) }
        SToggle(prefs, Lang.t("Tempolimit-Töne", "Speed limit sounds"), PreferenceKeys.LIMIT_SOUNDS, true, Lang.t("Aus: keine Töne und keine Stimme zum Tempolimit (zu schnell, Wechsel). Blitzer, Gefahren und andere Warnungen bleiben", "Off: no beeps or voice for the speed limit (over it, or a change). Cameras, hazards and other alerts still sound"))
        SToggle(prefs, Lang.t("Limitänderung ansagen", "Announce limit changes"), PreferenceKeys.LIMITCHANGE, false)
        if (prefs.b(PreferenceKeys.LIMITCHANGE, false)) SToggle(prefs, Lang.t("Pop-up beim Tempowechsel", "Pop-up when the limit changes"), PreferenceKeys.LIMIT_POPUP, true, Lang.t("Aus: kein Hinweis auf der Karte, das Schild am Tacho pulsiert trotzdem", "Off: no banner on the map; the sign by the speedometer still pulses"))
        if (prefs.b(PreferenceKeys.LIMITCHANGE, false)) SChoice("Wie", listOf("both" to "Gong und Stimme", "voice" to "Nur Stimme", "chime" to "Nur Gong"), prefs.s(PreferenceKeys.LIMIT_HOW, "both"),
            "Mit Gong kommt die Stimme erst, wenn der Gong vorbei ist") { prefs.put(PreferenceKeys.LIMIT_HOW, it) }
        SToggle(prefs, Lang.t("Geschätzte Limits anzeigen", "Show estimated limits"), PreferenceKeys.LIMITEST, false, Lang.t("Übliches Limit anzeigen, wo keins kartiert ist", "Show the usual limit where none is mapped"))
        // where you are bans camera warnings and "Off where banned" is on: say so here, or it looks as if the switch doesn't work
        val banned = com.headunit.launcher.CAMERA_BANNED[engine.detectedCountry]
        val camOff = banned != null && prefs.b(PreferenceKeys.CAMERA_WARNINGS, false) && prefs.b(PreferenceKeys.CAMBANOBEY, true)
        SToggle(prefs, Lang.t("Blitzer", "Speed cameras"), PreferenceKeys.CAMERA_WARNINGS, false, if (camOff) Lang.t("Aus: in $banned nicht erlaubt", "Off: not allowed in $banned")
            else if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) engine.camNote.ifBlank { Lang.t("Suche startet mit dem nächsten GPS-Signal", "Search starts with the next GPS fix") } else null)
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) SToggle(prefs, Lang.t("Aus, wo verboten", "Off where banned"), PreferenceKeys.CAMBANOBEY, true, Lang.t("Automatisch aus z. B. in DE, FR und CH", "Automatically off e.g. in DE, FR and CH"))
        SToggle(prefs, Lang.t("Blitzer auf der Karte zeigen", "Show cameras on the map"), PreferenceKeys.CAMERA_MAP, true, Lang.t("Nur Anzeige; Warnungen dort nur mit „Blitzer“ an", "Display only; warnings there only with \"Speed cameras\" on"))
        SToggle(prefs, Lang.t("Parkhäuser auf der Karte", "Car parks on the map"), PreferenceKeys.GARAGES_MAP, true, Lang.t("Freie Plätze live, wo verfügbar. Daten: MobiData BW, ParkenDD", "Live free spaces where available. Data: MobiData BW, ParkenDD"))
        SToggle(prefs, Lang.t("Sektionskontrollen (Abschnittskontrolle)", "Average speed cameras"), PreferenceKeys.AVGZONES, true, Lang.t("Durchschnittstempo im Vergleich zum Limit", "Average speed against the limit"))
    }
    SGroup(Lang.t("Unterwegs", "On the way"), footer = Lang.t("Informationen stammen aus OpenStreetMap.", "Information comes from OpenStreetMap.")) {
        SToggle(prefs, Lang.t("Baustellen und Ereignisse", "Roadworks and events"), PreferenceKeys.INCIDENTS, true, Lang.t("Baustellen und Sperrungen auf Autobahnen. Daten: Autobahn GmbH. In Spanien: Unfälle, Baustellen, Sperrungen, Gefahren und V16-Pannenbaken der DGT (nap.dgt.es)", "Roadworks and closures on motorways. Data: Autobahn GmbH. In Spain: accidents, roadworks, closures, hazards and V16 breakdown beacons from the DGT (nap.dgt.es)"))
        SToggle(prefs, Lang.t("Baustellen-Warnungen", "Roadworks warnings"), PreferenceKeys.ROADWORKS_WARN, true, Lang.t("Baustellen und Spuren-Sperrungen auf der Route: Zeile auf der Abbiegekarte und Ansage. Unfälle und Sperrungen warnen weiter", "Roadworks and lane closures on the route: the line on the turn card and the voice. Accidents and closures still warn"))
        SToggle(prefs, Lang.t("Einbahnstraßen-Warnung", "One-way warning"), PreferenceKeys.WRONGWAY, false, Lang.t("Bei falscher Fahrtrichtung; Fehlalarme möglich", "On wrong direction; false alarms possible"))
        SToggle(prefs, Lang.t("Bremsschwellen", "Speed bumps"), PreferenceKeys.BUMPS, false)
        SToggle(prefs, Lang.t("Schulen zu Schulzeiten", "Schools at school times"), PreferenceKeys.SCHOOLS, false)
        SToggle(prefs, Lang.t("Umweltzonen", "Low-emission zones"), PreferenceKeys.ZONEWARN, true)
        SToggle(prefs, Lang.t("Pausenerinnerung nach 2 Stunden", "Break reminder after 2 hours"), PreferenceKeys.BREAKREMIND, false)
        SToggle(prefs, Lang.t("Bahnübergänge und Furten", "Level crossings and fords"), PreferenceKeys.RAILFORD, false, Lang.t("Warnschild und Sprachwarnung", "Warning sign and spoken warning"))
        SToggle(prefs, Lang.t("Scharfe Kurven", "Sharp bends"), PreferenceKeys.SHARPBENDS, false, Lang.t("Gelb gestreift auf der Route, Warnschild und Gong kurz davor (nur wenn du zu schnell bist)", "Striped yellow on the route, a sign and a chime just before (only when you're going too fast)"))
        SToggle(prefs, Lang.t("Wetterhinweise", "Weather notes"), PreferenceKeys.WXADVICE, true, Lang.t("Hinweis bei Regen, Schnee, Nebel oder Glätte", "A note for rain, snow, fog or ice"))
        SToggle(prefs, Lang.t("Unwetterwarnungen (DWD)", "Severe weather warnings (DWD)"), PreferenceKeys.DWD_WARN, true, Lang.t("Amtliche Warnungen auf der Strecke: DWD in Deutschland, Meteoalarm in Frankreich, Österreich, Italien, Spanien, den Niederlanden und weiteren Ländern mit Warngebieten", "Official warnings on the route: DWD in Germany, Meteoalarm in France, Austria, Italy, Spain, the Netherlands and other countries with warning areas"))
        SToggle(prefs, Lang.t("Feiertagshinweis", "Public holiday note"), PreferenceKeys.HOLIDAY_HINT, true, Lang.t("Beim Start einer Route ein Hinweis, wenn heute oder morgen ein Feiertag ist (mehr Verkehr). Daten: date.nager.at", "When a route starts, a note if today or tomorrow is a public holiday (more traffic). Data: date.nager.at"))
        SToggle(prefs, Lang.t("Falschfahrer-Meldungen", "Wrong-way driver reports"), PreferenceKeys.WRONGWAY_REPORTS, true, Lang.t("Alarm bei Meldung bis 30 km voraus", "Alarm for a report up to 30 km ahead"))
        SToggle(prefs, Lang.t("Vignetten-Hinweis", "Vignette note"), PreferenceKeys.VIGNETTE_WARN, true, Lang.t("Ansage ca. 30 km vor Vignetten- oder Mautland", "Announced about 30 km before a vignette or toll country"))
        SToggle(prefs, Lang.t("Rettungsgasse", "Emergency corridor"), PreferenceKeys.RESCUE_LANE, true, Lang.t("Hinweis im Autobahnstau", "Note in motorway jams"))
    }
    SGroup(Lang.t("Meldungen", "Your reports"), footer = Lang.t("Meldungen über „Optionen“ auf der Karte erstellen.", "Create reports with the options button on the map.")) {
        SToggle(prefs, Lang.t("Vor Meldungen voraus warnen", "Warn before reports ahead"), "policealerts", true)
        SItem(Lang.t("Aktive Meldungen", "Active reports"), "${engine.reportsActive().size}") { SButton(Lang.t("Löschen", "Delete"), danger = true) { engine.clearReports() } }
        SToggle(prefs, Lang.t("Meldungen teilen", "Share reports"), PreferenceKeys.REPORTS_SHARE, true, Lang.t("Anonym teilen (Art, Ort, Zeit). Aus: keine Serververbindung.", "Shared anonymously (kind, place, time). Off: no server connection."))
        // the server's state, looked at again every few seconds while this page is open
        var tick by remember { mutableStateOf(0) }
        androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(5000); tick++ } }
        val status = tick.let {
            val sr = com.headunit.launcher.SharedReports
            when {
                !sr.configured(prefs) -> Lang.t("Kein Server eingerichtet", "No server set up")
                !prefs.b(PreferenceKeys.REPORTS_SHARE, true) -> Lang.t("Aus: keine Verbindung zum Server", "Off: no connection to the server")
                sr.lastError.isNotBlank() -> Lang.t("Fehler: ", "Error: ") + sr.lastError
                sr.lastOkAt > 0 -> Lang.t("Server verbunden · ${sr.shownCount} Meldungen in der Nähe", "Server connected · ${sr.shownCount} reports nearby")
                else -> Lang.t("Noch nicht verbunden: startet mit dem nächsten GPS-Signal", "Not connected yet: starts with the next GPS fix")
            }
        }
        SItem(Lang.t("Server für geteilte Meldungen", "Server for shared reports"), status)
        SFieldItem(Lang.t("Meldungs-Server", "Reports server"), prefs.s(PreferenceKeys.REPORTS_URL, ""), Lang.t("Leer: eingebauter Server", "Empty: built-in server"), placeholder = "https://…") { prefs.put(PreferenceKeys.REPORTS_URL, it.trim()) }
    }
}

// ------------------------------------------------------------------ Sound & voice

@Composable
private fun SoundSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    SGroup(Lang.t("Ton & Stimme", "Sound & voice")) {
        // the app's language (screens, Android Auto, spoken directions, route instructions and search results). Android 13 and newer
        // keep a language per app; before that the app follows the phone's language
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val lm = remember { ctx.getSystemService(android.app.LocaleManager::class.java) }
            var lang by remember { mutableStateOf(lm?.applicationLocales?.takeIf { !it.isEmpty }?.get(0)?.language ?: "system") }
            SChoice(Lang.t("Sprache", "Language"), listOf("system" to Lang.t("Telefonsprache", "Phone's language"), "de" to "Deutsch", "en" to "English"), lang,
                Lang.t("Bildschirme, Android Auto und Ansagen", "Screens, Android Auto and voice")) {
                lang = it
                lm?.applicationLocales = if (it == "system") android.os.LocaleList.getEmptyLocaleList() else android.os.LocaleList.forLanguageTags(it)
            }
        }
        SChoice(Lang.t("Sprachführung", "Voice guidance"), listOf("0" to Lang.t("Alle", "All"), "1" to Lang.t("Nur Warnungen", "Alerts only"), "2" to Lang.t("Aus", "Off")), engine.soundMode.toString(),
            Lang.t("„Nur Warnungen“: ohne Abbiegehinweise", "\"Alerts only\": no turn directions")) { prefs.put(PreferenceKeys.SOUND_MODE, it.toInt()) }
        SToggle(prefs, Lang.t("Straßennamen ansagen", "Say road names"), PreferenceKeys.VOICE_ROADS, true, Lang.t("Aus gibt nur die Richtung an", "Off only says the direction"))
        SToggle(prefs, Lang.t("Landmarken in Anweisungen", "Landmarks in directions"), PreferenceKeys.LANDMARKS, true, Lang.t("Abbiegen an Ampeln: „An der Ampel rechts abbiegen“", "Turns at traffic lights: \"At the traffic lights, turn right\""))
        SToggle(prefs, Lang.t("Intelligente Anweisungen", "Smart directions"), PreferenceKeys.SMARTVOICE, true, Lang.t("Anweisungen mit Ausfahrtsnummern und Schildern", "Directions with exit numbers and signs"))
        SChoice(Lang.t("Sprachkanal", "Voice channel"), listOf("nav" to "Navigation", "media" to Lang.t("Medien", "Media"), "notification" to Lang.t("Benachrichtigung", "Notification"), "alarm" to Lang.t("Wecker", "Alarm"), "ringtone" to Lang.t("Klingelton", "Ringtone")), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            Lang.t("Medien wählen, falls keine Sprache zu hören ist. Benachrichtigung, Wecker oder Klingelton für Head-Units, die den Navi-Kanal mit der Musiklautstärke abspielen", "Choose Media if no speech is heard. Notification, Alarm or Ringtone for head units that play the navigation channel at the music volume")) { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        if (android.os.Build.VERSION.SDK_INT >= 28) SChoice(Lang.t("Stimme kommt aus", "Voice comes out of"), listOf("auto" to Lang.t("Automatisch", "Automatic"), "speaker" to Lang.t("Handylautsprecher", "Phone speaker")), prefs.s(PreferenceKeys.VOICE_OUT, "auto"),
            Lang.t("Handylautsprecher: Ansagen und Töne bleiben auf dem Handy, während Musik über Bluetooth läuft. Nicht mit Android Auto, das nutzt die Lautsprecher im Auto", "Phone speaker: directions and chimes stay on the phone while music plays over Bluetooth. Not with Android Auto, which uses the car's speakers")) { prefs.put(PreferenceKeys.VOICE_OUT, it) }
        SToggle(prefs, Lang.t("Stimme wie die Töne abspielen", "Play the voice like the chimes"), PreferenceKeys.VOICE_AS_CHIME, false,
            Lang.t("Für Head-Units, die die Musik bei den Tönen leiser machen, aber nicht bei den Ansagen. Die App spielt die Stimme dann selbst ab", "For head units that lower the music for the beeps but not for the spoken directions. The app then plays the voice itself"))
        SSlider(Lang.t("Sprachverstärkung", "Voice boost"), prefs.i(PreferenceKeys.VOICE_BOOST, 0).toFloat(), 0f..18f, "dB", sub = Lang.t("Macht die Ansagen bis 18 dB lauter, wenn sie neben der Musik zu leise sind. Schaltet „Stimme wie die Töne abspielen“ ein", "Makes the directions up to 18 dB louder when they're too quiet next to the music. Turns on \"Play the voice like the chimes\""), steps = 17, reset = 0f) { v ->
            prefs.put(PreferenceKeys.VOICE_BOOST, v); if (v > 0) prefs.put(PreferenceKeys.VOICE_AS_CHIME, true)
        }
        SSlider(Lang.t("Gong-Lautstärke", "Chime volume"), prefs.i(PreferenceKeys.CHIME_VOL, 100).toFloat(), 0f..100f, "%", sub = Lang.t("Gong bei Tempo, Tempolimit und Blitzern, zur Stimme eingestellt", "Chime for speed, speed limit and cameras, set against the voice"), steps = 19, reset = 100f) { prefs.put(PreferenceKeys.CHIME_VOL, it) }
        SButton(Lang.t("Gong anhören", "Play chime")) { engine.chimeSample() }
        SItem(Lang.t("Stimme und Sprache", "Voice and language"), Lang.t("Nutzt die System-Sprachausgabe", "Uses the system text-to-speech")) { SButton(Lang.t("Öffnen", "Open")) { openSystemSettings(ctx, "com.android.settings.TTS_SETTINGS") } }
        // the installed German voices: the more natural ones (Google's network voices) need internet, the others work offline
        val voices = remember { engine.installedVoices().take(8) }
        if (voices.isEmpty()) SNote(Lang.t("Keine passende Stimme installiert. Download unter „Stimme und Sprache“.", "No matching voice installed. Download under \"Voice and language\"."))
        else {
            val saved = prefs.s(PreferenceKeys.TTS_VOICE, "")
            SChoice(Lang.t("Ansagestimme", "Voice"), voices.mapIndexed { i, v -> v.name to "Stimme ${i + 1} · " + (if (v.isNetworkConnectionRequired) "online" else "offline") },
                if (voices.any { it.name == saved }) saved else voices.first().name,
                Lang.t("Online-Stimmen klingen natürlicher, benötigen aber Internet.", "Online voices sound more natural but need internet.")) { engine.useVoice(it) }
            SButton(Lang.t("Probe anhören", "Play a sample")) { engine.speak(Lang.t("Nach 500 Metern links abbiegen.", "In 500 metres, turn left."), true) }
        }
        SItem(Lang.t("Lautstärke", "Volume")) { SButton(Lang.t("Öffnen", "Open")) { openSystemSettings(ctx, Settings.ACTION_SOUND_SETTINGS) } }
    }
}


// ------------------------------------------------------------------ Screen & sizes

@Composable
private fun ScreenSection(prefs: Prefs, engine: NavEngine) {
    SGroup(Lang.t("Bildschirm", "Screen")) {
        SSlider(Lang.t("Helligkeit", "Brightness"), prefs.i(PreferenceKeys.BRIGHT, 100).toFloat(), 10f..100f, "%", sub = Lang.t("100% überlässt die Steuerung dem System", "100% leaves it to the system"), live = true) { prefs.put(PreferenceKeys.BRIGHT, it) }
        SToggle(prefs, Lang.t("Neues Hochformat-Layout", "New portrait layout"), PreferenceKeys.PORTRAIT_LAYOUT, true, Lang.t("Auf dem hochkant stehenden Bildschirm ohne Route: oben nur die Suche mit Einstellungen daneben, die Kartenknöpfe etwa auf halber Höhe", "On an upright screen with no route: just the search at the top with Settings beside it, the map buttons about half-way down"))
        SToggle(prefs, Lang.t("Bildschirm nachts dimmen", "Dim the screen at night"), PreferenceKeys.DIMNIGHT, false)
        SToggle(prefs, Lang.t("Bildschirm eingeschaltet lassen", "Keep the screen on"), PreferenceKeys.KEEPON, true)
        SToggle(prefs, Lang.t("Bildschirm auch ohne Route anlassen", "Keep the screen on without a route"), PreferenceKeys.KEEPON_NO_ROUTE, false,
            Lang.t("Der Bildschirm bleibt an, wenn du nur die Karte und den Verkehr ansiehst. Aus, weil es mehr Akku braucht", "The screen stays on while you just watch the map and traffic. Off, as it uses more battery"))
        val screenConfig = androidx.compose.ui.platform.LocalConfiguration.current
        val autoPercent = recommendedMainUiScale(screenConfig.screenWidthDp, screenConfig.screenHeightDp)
        val autoScale = prefs.b(PreferenceKeys.UI_AUTO_SCALE, prefs.s(PreferenceKeys.UI_SCALE, "").isBlank())
        SItem(
            Lang.t("Automatische UI-Skalierung", "Automatic UI scaling"),
            if (autoScale) Lang.t("Nach Bildschirmgröße (aktuell $autoPercent%)", "By screen size (now $autoPercent%)") else Lang.t("Manuelle Größe aktiv", "Manual size active")
        ) {
            // (the app's own switch, in the accent colour like all the others)
            com.headunit.launcher.ui.Switch(autoScale) { enabled ->
                if (!enabled) prefs.put(PreferenceKeys.UI_SCALE, autoPercent)
                prefs.put(PreferenceKeys.UI_AUTO_SCALE, enabled)
            }
        }
        SSlider(
            Lang.t("Text- und Buttongröße", "Text and button size"),
            (if (autoScale) autoPercent else prefs.i(PreferenceKeys.UI_SCALE, autoPercent)).toFloat(),
            60f..140f, "%", sub = if (autoScale) Lang.t("Schieberegler bewegen zum manuellen Anpassen", "Move the slider to set it by hand") else Lang.t("Manuell oder automatisch", "Manual or automatic"), steps = 15, reset = 100f
        ) {
            prefs.put(PreferenceKeys.UI_AUTO_SCALE, false)
            prefs.put(PreferenceKeys.UI_SCALE, it)
        }
        var mainAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem(Lang.t("Erweiterte Displaygrößen", "Advanced screen sizes"), Lang.t("Einzelne Elemente unabhängig anpassen", "Set single elements on their own")) {
            SButton(if (mainAdvanced) Lang.t("Ausblenden", "Hide") else Lang.t("Anzeigen", "Show")) { mainAdvanced = !mainAdvanced }
        }
        if (mainAdvanced) {
            SSlider(Lang.t("Karten-Größe", "Card size"), prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).toFloat(), 70f..150f, "%", sub = Lang.t("Abbiege-, Fahrten- und Routenkarten", "Turn, trip and route cards"), steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_CARD_SIZE, it) }
            SSlider(Lang.t("Pfeilgröße der Abbiegekarte", "Turn arrow size"), prefs.i(PreferenceKeys.TURN_ARROW_SIZE, 100).toFloat(), 70f..200f, "%", sub = Lang.t("Nur der Pfeil, bis doppelt so groß", "The arrow on its own, up to twice as big"), reset = 100f) { prefs.put(PreferenceKeys.TURN_ARROW_SIZE, it) }
            SSlider(Lang.t("Breite der Abbiegekarte", "Turn card width"), prefs.i(PreferenceKeys.TURN_CARD_WIDTH, 100).toFloat(), 60f..150f, "%", sub = Lang.t("Wie weit die Karte über die Karte reicht", "How far the card reaches across the map"), reset = 100f) { prefs.put(PreferenceKeys.TURN_CARD_WIDTH, it) }
            SSlider(Lang.t("Höhe der Abbiegekarte", "Turn card height"), prefs.i(PreferenceKeys.TURN_CARD_HEIGHT, 100).toFloat(), 50f..200f, "%", sub = Lang.t("Der Platz über und unter der Schrift", "The room above and below the writing"), reset = 100f) { prefs.put(PreferenceKeys.TURN_CARD_HEIGHT, it) }
            SSlider(Lang.t("Button-Größe", "Button size"), prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = Lang.t("Bedienelemente auf dem Bildschirm", "Controls on the screen"), steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_BUTTON_SIZE, it) }
            SSlider(Lang.t("Textgröße", "Text size"), prefs.i(PreferenceKeys.MAIN_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = Lang.t("Unabhängige Textskalierung", "Text scaling on its own"), steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_TEXT_SIZE, it) }
            SSlider(Lang.t("Straßennamen-Größe", "Road name size"), prefs.i(PreferenceKeys.ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = Lang.t("Name der aktuellen Straße unter dem Pfeil", "Name of the current road under the arrow"), steps = 12, reset = 100f) { prefs.put(PreferenceKeys.ROAD_NAME_SIZE, it) }
        }

    }
    SGroup(Lang.t("Einheiten", "Units")) {
        SChoice(Lang.t("Entfernung und Tempo", "Distance and speed"), listOf("km" to Lang.t("Kilometer", "Kilometres"), "mi" to Lang.t("Meilen", "Miles")), if (engine.imperial) "mi" else "km") { prefs.put(PreferenceKeys.DISTANCE_UNIT, it) }
        SChoice(Lang.t("Temperatur", "Temperature"), listOf("C" to "°C", "F" to "°F"), if (isImperial(prefs)) "F" else "C") { prefs.put(PreferenceKeys.TEMPERATURE_UNIT, it) }
        SChoice(Lang.t("Fahrseite", "Driving side"), listOf("" to Lang.t("Automatisch", "Automatic"), "rhd" to Lang.t("Rechtslenker (Linksverkehr)", "Right-hand drive (drive on the left)"), "lhd" to Lang.t("Linkslenker (Rechtsverkehr)", "Left-hand drive (drive on the right)")), prefs.s(PreferenceKeys.DRIVE, ""),
            when (prefs.s(PreferenceKeys.DRIVE, "")) {
                "rhd" -> Lang.t("Fahrer rechts: Karten links, Buttons rechts", "Driver on the right: cards left, buttons right")
                "lhd" -> Lang.t("Fahrer links: Bildschirm gespiegelt, Karten rechts", "Driver on the left: screen mirrored, cards right")
                else -> Lang.t("Nach aktuellem Land", "By current country")
            }) { prefs.put(PreferenceKeys.DRIVE, it) }
    }
}

// ------------------------------------------------------------------ Android Auto

/** Beim Fahren: its own page (road name, arrow, tunnels, media controls, locking). */
@Composable
private fun WhileDrivingSection(prefs: Prefs, engine: NavEngine) {
    if (isPhoneDevice(LocalContext.current)) SGroup(Lang.t("Bild im Bild", "Picture in picture")) {
        SToggle(prefs, Lang.t("Bild im Bild", "Picture in picture"), PreferenceKeys.PIP, true, Lang.t("Auf einer Route läuft die Karte beim Wechsel zum Startbildschirm oder einer anderen App in einem kleinen Fenster weiter, mit der nächsten Abbiegung und der Restzeit. Antippen holt die App zurück. Nicht mit Android Auto", "On a route, going to the home screen or another app keeps the map going in a small window, with the next turn and the time left. Tap it to come back. Not with Android Auto"))
    }
    SGroup(Lang.t("Song und Cover", "Song and cover"), footer = "Zurück, Pause und Weiter unten auf dem Fahrbildschirm, solange auf dem Autodisplay Musik oder Radio läuft, aus jeder App. Mit Benachrichtigungszugriff stehen auch Titel, Interpret und Cover dabei.") {
        val ctx = LocalContext.current
        val access = com.headunit.launcher.MediaHub.hasAccess(ctx)
        SToggle(prefs, "Mediensteuerung", PreferenceKeys.MEDIA_CONTROLS, false, "Die Leiste erscheint nur, wenn etwas läuft")
        if (com.headunit.launcher.BuildConfig.MEDIA_ACCESS) SItem("Titel und Cover", if (access) "Zugriff erteilt" else "Noch kein Zugriff: Titel und Cover fehlen") {
            SButton(if (access) Lang.t("Ändern", "Change") else "Zugriff erlauben", primary = !access) {
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
    SGroup(Lang.t("Beim Fahren", "While driving")) {
        SToggle(prefs, Lang.t("Himmelsrichtung hinter dem Straßennamen", "Direction after the road name"), PreferenceKeys.HEADING_WORD, true, Lang.t("„Richtung E“ hinter der Straße, wenn du ohne Route fährst. Aus: nur der Straßenname", "“Heading E” after the road when you drive with no route. Off: only the road name"))
        SToggle(prefs, Lang.t("Geschwindigkeit neben dem Limit", "Speed next to the limit"), PreferenceKeys.MAPSPEED, true)
        SToggle(prefs, Lang.t("Tachoring", "Speed ring"), PreferenceKeys.SPEEDRING, true, Lang.t("Füllt sich bis zum Tempolimit, färbt sich gelb dann rot", "Fills up to the speed limit, turns yellow then red"))
        SSlider(Lang.t("Tempolimit- und Tachogröße", "Speed limit and speedometer size"), prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = Lang.t("Auf Smartphone und Autodisplay", "On the phone and the car screen"), steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SToggle(prefs, Lang.t("Sonnenzeiten auf Android Auto", "Sun times on Android Auto"), PreferenceKeys.CARSUN, false, Lang.t("Tag und Nacht auf dem Autodisplay folgen Sonnenauf- und -untergang, auch wenn das Auto bei Licht dunkel wird", "Day and night on the car screen follow sunrise and sunset, even when the car goes dark with its headlights"))
        SToggle(prefs, "Großes Limit-Schild", PreferenceKeys.SPEED_SIGN_BIG, false, "Das Tempolimit-Schild groß an der Stelle des Tachos, dein Tempo klein daneben")
        SChoice(Lang.t("Tacho-Seite", "Speed side"), listOf("right" to Lang.t("Rechts", "Right"), "left" to Lang.t("Links", "Left"), "auto" to Lang.t("Automatisch", "Automatic")), prefs.s(PreferenceKeys.MAIN_SPEED_SIDE, "right"),
            Lang.t("Auf Smartphone, Head-Unit und Android Auto (untere Ecke). Automatisch: wie früher. Das Limit-Schild steht immer zur Bildschirmmitte hin", "On the phone, head unit and Android Auto (bottom corner). Automatic: as it used to be. The limit sign always sits towards the middle of the screen")) { prefs.put(PreferenceKeys.MAIN_SPEED_SIDE, it) }
        SToggle(prefs, Lang.t("Straße und Richtung beim Fahren ohne Route", "Road and direction when driving without a route"), PreferenceKeys.CRUISE, true)
        SToggle(prefs, Lang.t("Pfeil ohne Route auf der Straße halten", "Keep the arrow on the road without a route"), PreferenceKeys.SNAPROAD, true)
        SToggle(prefs, Lang.t("Pfeil seitlich lassen", "Keep the arrow to the side"), PreferenceKeys.KEEP_ARROW_SIDE, false, Lang.t("Auf breitem Bildschirm bleibt der Pfeil seitlich, wenn die Karten einklappen, statt in die Mitte zu gleiten", "On a wide screen the arrow stays to the side when the cards fold away, instead of gliding back to the middle"))
        SToggle(prefs, Lang.t("Karte in Tunnels dimmen", "Dim the map in tunnels"), PreferenceKeys.TUNNELDIM, true)
        SToggle(prefs, Lang.t("Uhr auf der Karte", "Clock on the map"), PreferenceKeys.CLOCK_ON_MAP, false, Lang.t("Eine kleine Uhr unter dem Tacho, wenn die Statusleiste des Geräts verdeckt ist", "A small clock under the speedometer, when the device's status bar is covered"))
        SToggle(prefs, Lang.t("Weniger Buttons beim Fahren", "Fewer buttons while driving"), PreferenceKeys.DRIVESIMPLE, true)
        SToggle(prefs, Lang.t("Tastatur beim Fahren sperren", "Lock the keyboard while driving"), PreferenceKeys.DRIVELOCK, false, Lang.t("Sprachsuche funktioniert weiterhin", "Voice search still works"))
    }
}

// ------------------------------------------------------------------ Performance

@Composable
private fun PerformanceSection(prefs: Prefs) {
    SGroup(Lang.t("Flüssigkeit", "Smoothness"), footer = Lang.t("Niedriger stellen, falls die Karte langsam reagiert.", "Set it lower if the map reacts slowly.")) {
        SChoice(Lang.t("Karten-Schärfe", "Map sharpness"), listOf("100" to Lang.t("Voll", "Full"), "80" to Lang.t("Ausgewogen", "Balanced"), "65" to Lang.t("Leicht", "Light"), "50" to Lang.t("Niedrigst", "Lowest")), prefs.s(PreferenceKeys.MAP_RESOLUTION, "80")) { prefs.put(PreferenceKeys.MAP_RESOLUTION, it) }
        SChoice(Lang.t("Bildrate", "Frame rate"), listOf("auto" to "Auto", "20" to "20", "30" to "30", "45" to "45", "60" to "60"), prefs.s(PreferenceKeys.MAP_FPS, "auto"),
            if (prefs.s(PreferenceKeys.MAP_FPS, "auto") == "auto") Lang.t("Startet bei 30 FPS und passt sich an", "Starts at 30 FPS and adapts") else Lang.t("Niedriger entlastet das Gerät", "Lower eases the load on the device")) { prefs.put(PreferenceKeys.MAP_FPS, it) }
        SToggle(prefs, Lang.t("Schneller Kartenaufbau", "Fast map loading"), PreferenceKeys.MAPFAST, true)
        SToggle(prefs, Lang.t("Leistungsanzeige", "Performance display"), PreferenceKeys.PERFORMANCE_METER, false, Lang.t("Anzeige von FPS und Renderschritten", "Shows FPS and render steps"))
    }
    SGroup("Demo") {
        SToggle(prefs, Lang.t("Demofahrt-Button", "Demo drive button"), PreferenceKeys.DEMO, false, Lang.t("Spielt eine Route ab zum Testen beim Parken", "Plays a route back for testing while parked"))
    }
}

// ------------------------------------------------------------------ Keys & accounts

/**
 * Settings > Map > Colours: a small picture of each colour set (land, water, park, roads, a building and the route), tap to pick.
 * Day colours are shown; each one has its night version.
 */
@Composable
private fun MapColoursPicker(value: String, onPick: (String) -> Unit) {
    val c = LocalVw.current
    // land, water, park, road, building
    val sets = listOf(
        Triple("mapbox", "Mapbox", listOf(0xFFEDEFE9, 0xFF9CCBF0, 0xFFC6E4B4, 0xFFFFFFFF, 0xFFDCDCE0)),
        Triple("horizon", "Horizon", listOf(0xFFF4F2EE, 0xFFAADAF5, 0xFFCDEBC4, 0xFFFFFFFF, 0xFFE7E5E1)),
        Triple("faded", Lang.t("Blass", "Pale"), listOf(0xFFF1F1F1, 0xFFD9E2EA, 0xFFE2E8DE, 0xFFFFFFFF, 0xFFE6E6E6)),
        Triple("grey", Lang.t("Grau", "Grey"), listOf(0xFFE8E8E8, 0xFFCFCFCF, 0xFFDADADA, 0xFFFFFFFF, 0xFFD4D4D4)),
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SItem(Lang.t("Farben", "Colours"), Lang.t("Farbschema für die Karte wählen.", "Choose the map's colour scheme."))
        Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()).padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for ((id, name, cols) in sets) {
                val sel = id == value
                Column(
                    Modifier.width(124.dp).clip(RoundedCornerShape(14.dp)).background(c.tile)
                        .border(2.dp, if (sel) c.accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { onPick(id) },
                ) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(76.dp)) {
                        val w = size.width; val h = size.height
                        fun col(i: Int) = androidx.compose.ui.graphics.Color(cols[i])
                        drawRect(col(0))
                        drawRect(col(1), topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.3f))
                        drawRect(col(2), topLeft = androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.62f), size = androidx.compose.ui.geometry.Size(w * 0.24f, h * 0.26f))
                        drawRect(col(4), topLeft = androidx.compose.ui.geometry.Offset(w * 0.47f, h * 0.1f), size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.2f))
                        drawRect(col(3), topLeft = androidx.compose.ui.geometry.Offset(0f, h * 0.44f), size = androidx.compose.ui.geometry.Size(w, h * 0.12f))
                        drawRect(col(3), topLeft = androidx.compose.ui.geometry.Offset(w * 0.36f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.06f, h))
                        drawRect(androidx.compose.ui.graphics.Color(0xFF2A7BE4), topLeft = androidx.compose.ui.geometry.Offset(w * 0.68f, 0f), size = androidx.compose.ui.geometry.Size(w * 0.05f, h))
                    }
                    Text(name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                }
            }
        }
    }
}

/** The satellite photo store's size, a route being fetched ahead, and a button to empty it. */
@Composable
internal fun SatStoreItem() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var mb by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(-1L) }
    var progress by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (true) {
            mb = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { com.headunit.launcher.SatTileCache.sizeBytes(ctx) } / (1024 * 1024)
            val t = com.headunit.launcher.SatTileCache.prefetchTotal
            progress = if (t > 0) " · Route: ${com.headunit.launcher.SatTileCache.prefetchDone} von $t" else ""
            kotlinx.coroutines.delay(2000)
        }
    }
    SItem(Lang.t("Luftbild-Speicher", "Satellite image storage"), (if (mb < 0) "…" else "$mb MB von 500 MB") + progress) {
        SButton(Lang.t("Leeren", "Clear"), danger = true) { com.headunit.launcher.SatTileCache.clear(ctx); mb = 0 }
    }
}
