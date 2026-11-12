package com.headunit.launcher.ui

import com.headunit.launcher.R
import androidx.compose.ui.res.stringResource

import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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

/** A settings section; [heading] puts a small group heading (Map, Driving, You, Device) above it in the list. */
private class Section(val title: String, val sub: String, val icon: ImageVector, val tint: Color, val heading: String? = null)

// (the order here is the order in the list; SettingsIndex refers to them by number)
private val sections = listOf(
    Section("Kartenansicht", "Tag und Nacht, Details, 3D, Satellit", Icons.Rounded.Map, Color(0xFF4C8DFF), "KARTE"),
    Section("Auf der Karte", "Verkehr, Ampeln, Kameras, Schilder, Wetter", Icons.Rounded.Traffic, Color(0xFF4C8DFF)),
    Section("Pfeil & Routenlinie", "Pfeil oder Auto, Farben, Routendesign", Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
    Section("Routen", "Vermeiden, Öko, schnellere Routen, Start", Icons.Rounded.Route, Color(0xFF12A4B4), "FAHREN"),
    Section("Abbiegekarte & Spuren", "Schilder, Spuren, Fahrtenkarte, Ankunft", Icons.Rounded.TurnRight, Color(0xFF12A4B4)),
    Section("Warnungen", "Tempo, Blitzer, Gefahren, Meldungen", Icons.Rounded.NotificationsActive, Color(0xFFEA4335)),
    Section("Ton & Stimme", "Sprachansagen und Lautstärke", Icons.Rounded.VolumeUp, Color(0xFFEA4335)),
    Section("Orte & Fahrten", "Zuhause, Arbeit, Favoriten, Teilen, Fahrtenbuch", Icons.Rounded.Star, Color(0xFFF29900), "DU"),
    Section("Kraftstoff & Laden", "Spritart, Preise, Elektroauto", Icons.Rounded.LocalGasStation, Color(0xFF34A853)),
    Section("Offline-Karten", "Karten, Routen und Orte ohne Empfang", Icons.Rounded.CloudDownload, Color(0xFF8E5CF0)),
    Section("Bildschirm & Größen", "Helligkeit, Größen, Einheiten, beim Fahren", Icons.Rounded.BrightnessMedium, Color(0xFFFFB020), "GERÄT"),
    Section("Android Auto", "Autodisplay-Größen und Einrichtung", Icons.Rounded.DirectionsCar, Color(0xFF9CA4BF)),
    Section("Leistung", "Flüssigkeit auf langsamen Displays, Demo", Icons.Rounded.Speed, Color(0xFF9CA4BF)),
    Section("Schlüssel & Konten", "Kartenschlüssel und Suche", Icons.Rounded.VpnKey, Color(0xFF9CA4BF)),
    Section("Info & Sicherung", "Updates, Berechtigungen, Sicherung", Icons.Rounded.Info, Color(0xFF9CA4BF)),
)

/** Every setting in one screen: sections on the left, the chosen section on the right, in the map screen's soft style. */
@Composable
fun SettingsScreen(prefs: Prefs, engine: NavEngine, onClose: () -> Unit) {
    val base = LocalVw.current
    val v = remember(base) { navVw(base) }
    CompositionLocalProvider(LocalVw provides v) { SettingsBody(prefs, engine, onClose) }
}

@Composable
private fun SettingsBody(prefs: Prefs, engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    var cat by rememberSaveable { mutableIntStateOf(0) }
    // portrait / narrow: one pane at a time (the section list, or the open section with a back arrow)
    val narrowS = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720
    var showList by rememberSaveable { mutableStateOf(true) }
    androidx.activity.compose.BackHandler(enabled = narrowS && !showList) { showList = true }
    var query by remember { mutableStateOf("") }
    var highlight by remember { mutableStateOf<String?>(null) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val words = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    val matches = remember(query) {
        if (words.isEmpty()) emptyList() else settingsIndex.filter { r ->
            val hay = (r.title + " " + r.sub + " " + r.group + " " + sections[r.section].title + " " + tr(r.title) + " " + tr(r.sub) + " " + tr(r.group) + " " + tr(sections[r.section].title)).lowercase()
            words.all { hay.contains(it) }
        }.sortedBy { if (it.title.lowercase().startsWith(words[0])) 0 else if (it.title.lowercase().contains(words[0])) 1 else 2 }.take(40)
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
                    Icon(Icons.Rounded.ArrowBack, "Zurück zur Karte", tint = c.text, modifier = Modifier.size(24.dp))
                }
                Text(stringResource(R.string.ui_048), color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
            }
            // search every setting by name
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).height(46.dp).clip(RoundedCornerShape(50)).background(c.tile).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.sub, modifier = Modifier.size(20.dp))
                Box(Modifier.weight(1f).padding(start = 10.dp), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text(stringResource(R.string.ui_045), color = c.sub, fontSize = 15.sp)
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
                if (matches.isEmpty()) Text(stringResource(R.string.ui_080, query.trim()), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(10.dp))
                matches.forEach { r ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                            cat = r.section; highlight = r.title; query = ""; focus.clearFocus(); showList = false
                        }.padding(horizontal = 12.dp, vertical = 9.dp),
                    ) {
                        Text(tr(r.title), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(tr(sections[r.section].title) + (if (r.group.isNotBlank()) " · " + tr(r.group) else ""), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else sections.forEachIndexed { i, s ->
                // the small group headings in the list: Map, Driving, You, Device
                if (s.heading != null) Text(tr(s.heading), color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
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
                        Text(if (Neon.on) tr(s.title).uppercase() else tr(s.title), color = if (on) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = if (Neon.on) 1.sp else 0.sp)
                        Text(tr(s.sub), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // the chosen section (each opens at the top), sliding up gently into place
        if (!narrowS || !showList) Column(Modifier.weight(1f).fillMaxHeight()) {
        // the neon look: the sections as a bar of tabs at the top
        if (Neon.on) NeonTabs(sections.map { tr(it.title) }, cat, { cat = it; highlight = null })
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
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp)) {
                if (narrowS) Row(Modifier.padding(bottom = 6.dp).clip(RoundedCornerShape(50)).clickable { showList = true }.padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.ArrowBack, "Alle Einstellungen", tint = c.text, modifier = Modifier.size(22.dp))
                    }
                    Text(stringResource(R.string.ui_048), color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
                }
                Text(tr(sections[cat].title), color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(tr(sections[cat].sub), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, bottom = 16.dp))
                Column(Modifier.widthIn(max = 820.dp)) {
                    when (cat) {
                        0 -> MapLookSection(prefs, engine)
                        1 -> OnTheMapSection(prefs, engine)
                        2 -> ArrowSection(prefs, engine)
                        3 -> RoutesSection(prefs)
                        4 -> GuidanceSection(prefs)
                        5 -> AlertsSection(prefs, engine)
                        6 -> SoundSection(prefs, engine)
                        7 -> { PlacesSection(engine); PhoneLinkGroup(prefs); SharingGroups(prefs) }
                        8 -> FuelSection(prefs, engine)
                        9 -> { OfflineMapsContent(prefs, engine); OfflinePlacesGroup(engine) }
                        10 -> ScreenSection(prefs, engine)
                        11 -> AndroidAutoSection(prefs)
                        12 -> PerformanceSection(prefs)
                        13 -> KeysSection(prefs, engine)
                        else -> AboutSection(prefs, engine)
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
private fun OwnDesignGroup(prefs: Prefs) {
    val c = LocalVw.current
    val route = prefs.s(PreferenceKeys.ROUTE_COLOUR, "")
    val arrow = prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")
    val accentHex = prefs.s(PreferenceKeys.ACCENT_COLOUR, "")
    val light = (prefs.s(PreferenceKeys.MAP_LIGHT, "0").toFloatOrNull() ?: 0f).toInt()
    SGroup("Eigenes Design", footer = "Wirkt sofort. Standard stellt die übliche Farbe wieder her.") {
        val routeCol = CustomLook.parse(route) ?: if (c.dark) Color(0xFF4CC9FF) else Color(0xFF2A7BE4)
        val arrowCol = CustomLook.parse(arrow) ?: Color(0xFFF2F2F6)
        val accentCol = CustomLook.parse(accentHex) ?: c.accent
        val mapBg = if (c.dark) Color(0xFF1B1F2B) else Color(0xFFE9ECF1)
        val road = if (c.dark) Color(0xFF353B4D) else Color(0xFFFFFFFF)
        val wash = CustomLook.wash(light)
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(150.dp).clip(RoundedCornerShape(18.dp))) {
            val w = size.width; val h = size.height
            drawRect(mapBg)
            drawLine(road, androidx.compose.ui.geometry.Offset(0f, h * 0.3f), androidx.compose.ui.geometry.Offset(w, h * 0.42f), 14f)
            drawLine(road, androidx.compose.ui.geometry.Offset(w * 0.75f, 0f), androidx.compose.ui.geometry.Offset(w * 0.62f, h), 12f)
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.12f, h * 1.0f); cubicTo(w * 0.2f, h * 0.55f, w * 0.45f, h * 0.7f, w * 0.68f, h * 0.38f); lineTo(w * 0.73f, 0f) }
            drawPath(p, routeCol, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 22f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            val ax = w * 0.17f; val ay = h * 0.78f
            val tri = androidx.compose.ui.graphics.Path().apply { moveTo(ax, ay - 26f); lineTo(ax + 18f, ay + 18f); lineTo(ax, ay + 8f); lineTo(ax - 18f, ay + 18f); close() }
            drawPath(tri, arrowCol); drawPath(tri, Color.Black.copy(alpha = 0.5f), style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
            if (wash.alpha > 0f) drawRect(wash)
            drawRoundRect(accentCol, androidx.compose.ui.geometry.Offset(w - 150f, h - 70f), androidx.compose.ui.geometry.Size(130f, 50f), androidx.compose.ui.geometry.CornerRadius(25f))
        }
        val std = "" to "Standard"
        SColours("Routenfarbe", listOf(std, "#4CC9FF" to "Hellblau", "#2A7BE4" to "Blau", "#7B4FC9" to "Violett", "#A56BFF" to "Lila", "#1FC7A6" to "Türkis",
            "#3DDC84" to "Grün", "#FFD54A" to "Gelb", "#FF9A3C" to "Orange", "#FF6FB5" to "Rosa", "#FFFFFF" to "Weiß"), route) { prefs.put(PreferenceKeys.ROUTE_COLOUR, it) }
        SColours("Pfeilfarbe", listOf("#F2F2F6" to "Weiß", "#C2C6D0" to "Silber", "#4C8DFF" to "Blau", "#6FD0FF" to "Hellblau", "#1FC7A6" to "Türkis",
            "#3DDC84" to "Grün", "#FFD54A" to "Gelb", "#FF9A3C" to "Orange", "#FF4D5E" to "Rot", "#FF6FB5" to "Rosa", "#A56BFF" to "Violett"), arrow) { prefs.put(PreferenceKeys.ARROW_COLOUR, it) }
        SColours("Akzentfarbe", listOf(std, "#1A73E8" to "Blau", "#8AB4F8" to "Hellblau", "#1FA39A" to "Türkis", "#2E8B57" to "Grün", "#B97A00" to "Gold",
            "#D9661A" to "Orange", "#D7263D" to "Rot", "#D2508F" to "Rosa", "#7B4FC9" to "Violett"), accentHex) { prefs.put(PreferenceKeys.ACCENT_COLOUR, it) }
        SSlider("Karte heller/dunkler", light.toFloat(), -50f..50f, "%", reset = 0f) { prefs.put(PreferenceKeys.MAP_LIGHT, it.toInt().toString()) }
    }
}

@Composable
private fun MapLookSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Design", footer = if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon") "Neon ist immer ein Nachtdesign und ändert nur das Aussehen. Schrift: Rajdhani (SIL Open Font License)." else null) {
        SChoice("Aussehen", listOf("standard" to "Standard", "neon" to "Neon"), prefs.s(PreferenceKeys.LOOK, "standard"),
            "Neon: dunkle Karte mit leuchtenden Linien, überall in der App") { prefs.put(PreferenceKeys.LOOK, it) }
        if (prefs.s(PreferenceKeys.LOOK, "standard") == "neon")
            SChoice("Neonfarbe", listOf("yellow" to "Gelb", "cyan" to "Cyan", "magenta" to "Magenta"), prefs.s(PreferenceKeys.NEON_ACCENT, "yellow"),
                "Farbe von Menüs und Hervorhebungen") { prefs.put(PreferenceKeys.NEON_ACCENT, it) }
    }
    if (prefs.s(PreferenceKeys.LOOK, "standard") != "neon") OwnDesignGroup(prefs)
    SGroup("Tag und Nacht") {
        SChoice("Tag und Nacht", listOf("auto" to "Auto", "lights" to "Scheinwerfer", "sun" to "Sonnenstand", "times" to "Feste Zeiten", "day" to "Tag", "night" to "Nacht"), prefs.s(PreferenceKeys.THEME, "auto"),
            "Auto folgt dem Nachtmodus des Geräts") { prefs.put(PreferenceKeys.THEME, it) }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "lights") {
            val learned = VendorLight.learned(prefs)
            SToggle(prefs, "Lichtsignal umkehren", PreferenceKeys.LIGHTSREVERSED, false,
                if (learned.isEmpty()) "Noch kein Lichtsignal des Geräts erkannt: Abblendlicht einschalten und kurz warten." else "Erkannt: $learned")
        }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "times") {
            SFieldItem("Nacht beginnt um", prefs.s(PreferenceKeys.NIGHT_FROM, "19:00"), "24-Stunden-Format, z. B. 19:30", placeholder = "19:00") { prefs.put(PreferenceKeys.NIGHT_FROM, it.trim()) }
            SFieldItem("Tag beginnt um", prefs.s(PreferenceKeys.DAY_FROM, "07:00"), "24-Stunden-Format, z. B. 06:45", placeholder = "07:00") { prefs.put(PreferenceKeys.DAY_FROM, it.trim()) }
        }
    }
    SGroup("Karte") {
        SChoice("Kartenquelle", listOf("mapbox" to "Mapbox", "tomtom" to "TomTom", "osm" to "OpenStreetMap"), prefs.mapSource(),
            "Mapbox: 3D-Karte. Benötigt das Mapbox-Token.") { prefs.put(PreferenceKeys.MAP_STYLE, it) }
        if (prefs.mapSource() == "mapbox") {
            SChoice("Kartendetails", listOf("full" to "Vollständiges 3D", "lite" to "Lite", "lightest" to "Einfachst"), prefs.mapDetail(),
                "Lite: flache Gebäude. Einfachst: 2D-Karte.") { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
            MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { prefs.put(PreferenceKeys.MAP_COLOURS, it) }
        }
        DefaultZoomRow(prefs)
        SToggle(prefs, "Fußwege und Pfade", "paths", true, "Ausschalten für eine übersichtlichere Fahrkarte")
        SToggle(prefs, "Glaskarten", "glass", true, "Halbtransparente Karten")
        SChoice("Mapbox-Logo-Position", listOf("tl" to "Oben links", "tr" to "Oben rechts", "bl" to "Unten links", "br" to "Unten rechts"), prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"),
            "Ecke für das geforderte Mapbox-Logo wählen") { prefs.put(PreferenceKeys.MAPBOX_LOGO, it) }
    }
    SGroup("3D und Satellit") {
        SToggle(prefs, "Satellit", "satellite", false)
        SToggle(prefs, "3D-Gebäude", "buildings3d", true)
        SToggle(prefs, "Detaillierte Fassaden", "facades", true, "Fenster, Türen und Schaufenster an Gebäuden")
        SToggle(prefs, "3D-Sehenswürdigkeiten", "landmarks3d", true, "Detaillierte Modelle bekannter Gebäude und Stadien")
        SToggle(prefs, "3D-Berge und Täler", "terrain", false, "Echtes Gelände")
        SToggle(prefs, "Nebel und Himmel", "skyweather", true, "Dunst am Horizont passend zum Wetter")
        SToggle(prefs, "Nachtsternenhimmel in 3D", "nightsky", true, "Sterne über dem Horizont bei Nacht")
        SToggle(prefs, "Bäume in Parks", "trees", true, "3D-Bäume in Parks und an Straßen")
        SToggle(prefs, "Freie Sicht auf die Straße voraus", "clearview", true, "Bäume und Gebäude verdecken die Straße nicht")
        SToggle(prefs, "Tippen zum Hervorheben", "tapselect", true, "Tippe auf ein Gebäude oder einen Ort zum Auswählen")
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Verkehr") {
        SToggle(prefs, "Live-Verkehr", "traffic", true)
        SToggle(prefs, "Verkehrsübersicht beim Halt", PreferenceKeys.STOPPED_OVERVIEW, true, "Zoomt nach 30 s Stillstand heraus")
        SToggle(prefs, "Verkehr nur auf der Route anzeigen", "routetrafficonly", false, "Aus zeigt auch Verkehr auf umliegenden Straßen")
        SToggle(prefs, "Grüner Verkehr bei freier Fahrt", PreferenceKeys.TRAFFIC_GREEN, false, "Frei fließende Hauptstraßen grün anzeigen")
    }
    SGroup("Auf der Route") {
        SToggle(prefs, "Ampeln", "signals", true, if (prefs.b(PreferenceKeys.SIGNALS, true)) "Nur Ampeln auf deiner Route. Aktuell: " + engine.map.signalInfo() else null)
        SToggle(prefs, "Blitzer und Baustellen", "hazardicons", true)
        SToggle(prefs, "Zebrastreifen", "zebras", true, "Auf der Straße gezeichnet")
        SToggle(prefs, "Stoppschilder", PreferenceKeys.STOP_SIGNS, false, "Mit kurzem Ton")
        SToggle(prefs, "Hinweis „Vorfahrt gewähren“", PreferenceKeys.GIVE_WAY, false, "Standardmäßig aus")
    }
    SGroup("3D-Straßenausstattung") {
        SToggle(prefs, "3D-Straßenlaternen, Ampeln und Kameras", "furniture3d", true, "3D-Modelle in echter Größe")
        SToggle(prefs, "Straßenlaternen", "lamps", false, if (prefs.b(PreferenceKeys.LAMPS, false)) "Aktuell: " + engine.map.lampInfo() else null)
        if (prefs.b(PreferenceKeys.LAMPS, false)) SToggle(prefs, "Laternen aus OpenStreetMap", PreferenceKeys.LAMPS_ONLINE, true, "Echte Laternenstandorte, wo Leute sie eingetragen haben. Einmal pro Gebiet geladen, einen Monat gespeichert")
        if (prefs.b(PreferenceKeys.LAMPS, false)) SSlider("Laternenabstand", prefs.s(PreferenceKeys.LAMPGAP, "40").toFloatOrNull() ?: 40f, 15f..120f, " m", reset = 40f) { prefs.put(PreferenceKeys.LAMPGAP, it.toString()) }
    }
    SGroup("Wetter") {
        SToggle(prefs, "Wettereffekte auf der Karte", "rainfx", false, "Regen, Schnee und Nebel passend zum Wetter")
        SToggle(prefs, "Regenradar-Ebene", "radar", false, "Live-Radar von RainViewer unter der Route")
    }
}

// ------------------------------------------------------------------ Your arrow & route line

@Composable
private fun ArrowSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Pfeil", footer = "Flacher Pfeil: am schonendsten für langsame Geräte") {
        val loc = prefs.s(PreferenceKeys.LOCATOR, "arrow")
        val ctx = LocalContext.current
        val hasPic = prefs.s(PreferenceKeys.LOCATOR_IMAGE, "").isNotBlank() && java.io.File(ctx.filesDir, "locator.png").exists()
        val pickPic = rememberLocatorImagePicker { ok ->
            if (ok) { prefs.put(PreferenceKeys.LOCATOR_IMAGE, System.currentTimeMillis().toString()); prefs.put(PreferenceKeys.LOCATOR, "custom") }
            else android.widget.Toast.makeText(ctx, tr("Bild nicht lesbar (PNG oder JPG verwenden)"), android.widget.Toast.LENGTH_SHORT).show()
        }
        fun choosePic() { if (!pickPic()) android.widget.Toast.makeText(ctx, tr("Keine App auf diesem Gerät kann ein Bild auswählen"), android.widget.Toast.LENGTH_LONG).show() }
        SChoice("Positionsanzeige", listOf("arrow" to "3D-Pfeil", "car" to "Auto", "model" to "Echtes 3D-Modell", "flat" to "Flacher Pfeil", "custom" to "Eigenes Bild"), loc) {
            if (it == "custom" && !hasPic) choosePic() else prefs.put(PreferenceKeys.LOCATOR, it)
        }
        if (loc == "model") {
            SChoice("Modellform", listOf("arrow" to "Pfeil", "car" to "Auto"), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) { prefs.put(PreferenceKeys.MODEL_SHAPE, it) }
            SToggle(prefs, "Leuchten", PreferenceKeys.MODEL_GLOW, true, "Weiches blaues Leuchten um das Modell")
            if (prefs.mapDetail() != "full") SNote("Nur mit Kartendetails „Vollständiges 3D“")
        }
        val shapeCar = loc == "car" || (loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "car")
        val shapeArrow = loc == "arrow" || (loc == "model" && !shapeCar)
        if (loc == "custom") {
            SItem("Bild", "Am besten PNG mit transparentem Hintergrund") { SButton(if (hasPic) "Ändern" else "Wählen") { choosePic() } }
            SToggle(prefs, "Dreht sich mit", PreferenceKeys.LOCATOR_IMAGE_TURNS, true, "Dreht sich beim Fahren wie der Pfeil")
            SToggle(prefs, "Rund zugeschnitten", PreferenceKeys.LOCATOR_IMAGE_CIRCLE, false, "Schneidet das Bild kreisförmig zu")
        }
        if (shapeCar) {
            val cur = prefs.s(PreferenceKeys.CAR_MODEL, "saloon")
            val colour = prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")
            val names = listOf("saloon" to "Limousine", "hatch" to "Schrägheck", "estate" to "Kombi", "suv" to "SUV", "pickup" to "Pickup", "van" to "Van", "sports" to "Sportwagen")
            SItem("Fahrzeugtyp", names.firstOrNull { it.first == cur }?.second)
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
        if (shapeCar) SColours("Fahrzeugfarbe", listOf(
            "#F4F5F7" to "Weiß", "#B9BEC8" to "Silber", "#7A808C" to "Grau", "#3E434D" to "Graphit", "#2A2D34" to "Schwarz", "#1E2A4A" to "Dunkelblau",
            "#2F6BDF" to "Blau", "#5FB3E8" to "Hellblau", "#1FA39A" to "Türkis", "#2E8B57" to "Grün", "#6E8B3D" to "Oliv", "#F2C230" to "Gelb",
            "#F07C28" to "Orange", "#D7263D" to "Rot", "#8E1B2E" to "Weinrot", "#7B4FC9" to "Violett", "#E86FA8" to "Rosa", "#C9A27A" to "Champagner", "#6B4A35" to "Braun",
        ), prefs.s(PreferenceKeys.CAR_COLOUR, "#F4F5F7")) { prefs.put(PreferenceKeys.CAR_COLOUR, it) }
        else if (shapeArrow) SColours("Pfeilfarbe", listOf(
            "#F2F2F6" to "Weiß", "#C2C6D0" to "Silber", "#8A909C" to "Grau", "#3E434D" to "Graphit", "#4C8DFF" to "Blau", "#6FD0FF" to "Hellblau",
            "#1FC7A6" to "Türkis", "#3DDC84" to "Grün", "#B5E34A" to "Limette", "#FFD54A" to "Gelb", "#FF9A3C" to "Orange", "#FF4D5E" to "Rot",
            "#FF6FB5" to "Rosa", "#A56BFF" to "Violett",
        ), prefs.s(PreferenceKeys.ARROW_COLOUR, "#F2F2F6")) { prefs.put(PreferenceKeys.ARROW_COLOUR, it) }
        SSlider(if (shapeCar) "Fahrzeuggröße" else if (loc == "custom") "Bildgröße" else "Pfeilgröße", prefs.s(PreferenceKeys.ARROW_PERCENT, "150").toFloatOrNull() ?: 150f, 100f..500f, "%", reset = 150f) { prefs.put(PreferenceKeys.ARROW_PERCENT, it.toString()) }
    }
    SGroup("Routenlinie") {
        SToggle(prefs, "Abbiegepfeil auf der Route", "turnarrows", true, "Ein weißer Pfeil durch die nächste Kreuzung")
        SToggle(prefs, "Pfeile entlang der Route", "routechevrons", true, "Kleine Pfeile entlang der Routenlinie beim Zoomen")
        SToggle(prefs, "Routenschattenband", "ribbon", true, "Ein weicher Schatten unter der Route")
        SToggle(prefs, "3D-Fahnen", "flags3d", true, "Fahnen an Ziel und Stopps")
        if (prefs.b(PreferenceKeys.FLAGS_3D, true)) SToggle(prefs, "Fahnen wehen", PreferenceKeys.FLAGS_WAVE, true, "Fahnen bewegen sich im Wind")
        SToggle(prefs, "Fließendes Licht auf der Route", "routeflow", true)
        SColours("Farbe alternativer Routen", listOf(
            "#C4D9E6" to "Hellblau-Grau (Standard)", "#A9D4F0" to "Hellblau", "#5AB7F5" to "Blau", "#315CBA" to "Dunkelblau",
            "#A9B4C2" to "Silbergrau", "#FFFFFF" to "Weiß", "#42CFA5" to "Minze", "#F4C542" to "Gelb",
            "#FF9A4A" to "Orange", "#F05D6C" to "Rot", "#C58AF9" to "Violett", "#F08AC5" to "Rosa",
        ), prefs.s(PreferenceKeys.ALT_ROUTE_COLOUR, "#C4D9E6")) { prefs.put(PreferenceKeys.ALT_ROUTE_COLOUR, it) }
    }
}

// ------------------------------------------------------------------ Routes

@Composable
private fun RoutesSection(prefs: Prefs) {
    VehicleGroup(prefs)
    SGroup("Vermeiden", footer = "Änderungen gelten für die nächste geplante Route.") {
        SToggle(prefs, "Mautstraßen vermeiden", "avoid_toll", false, if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) "Überall" else null)
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) {
            val chosen = prefs.s(PreferenceKeys.AVOID_TOLL_COUNTRIES, "").split(',').filter { it.isNotBlank() }.toSet()
            SItem("Maut vermeiden nur in", if (chosen.isEmpty()) "Maut nur in diesen Ländern vermeiden" else "In diesen Ländern ohne Maut planen")
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((cc, name) in listOf("FR" to "Frankreich", "IT" to "Italien", "ES" to "Spanien", "PT" to "Portugal", "AT" to "Österreich", "CH" to "Schweiz",
                    "SI" to "Slowenien", "HR" to "Kroatien", "PL" to "Polen", "CZ" to "Tschechien", "HU" to "Ungarn", "GR" to "Griechenland", "IE" to "Irland", "NO" to "Norwegen", "US" to "USA")) {
                    Pill(name, cc in chosen, { prefs.put(PreferenceKeys.AVOID_TOLL_COUNTRIES, (if (cc in chosen) chosen - cc else chosen + cc).joinToString(",")) })
                }
            }
        }
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) SToggle(prefs, "Vignettenpflichtige Straßen meiden", PreferenceKeys.AVOID_VIGNETTE, false, "Nur bei Durchfahrt (AT, CH, SI, CZ, SK, HU, BG, RO)")
        SToggle(prefs, "Autobahnen vermeiden", "avoid_motorway", false)
        SToggle(prefs, "Fähren vermeiden", "avoid_ferry", false)
        SToggle(prefs, "Unbefestigte Straßen vermeiden", "avoid_unpaved", true)
        SToggle(prefs, "Umweltzonen vermeiden", "avoidzones", false, "Eine Start- oder Zielzone kann nicht vermieden werden")
        ZonesItem()
    }
    SGroup("Routenauswahl") {
        SToggle(prefs, "Öko-Routen", "route_eco", false, "Kraftstoffsparend statt am schnellsten")
        SToggle(prefs, "Schnellere Routen anbieten", "fasterroute", true)
        SToggle(prefs, "Einfachere Routen bevorzugen", PreferenceKeys.SIMPLER_ROUTE, true, "Eine Route, die nur etwa eine Minute langsamer ist, aber viel weniger Abbiegungen hat, wird zuerst gewählt")
        SToggle(prefs, "Auf der Bordsteinseite ankommen", "curbarrival", true, "Ankunft auf der Straßenseite des Ziels (Mapbox)")
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) {
            SToggle(prefs, "Automatisch umleiten", "autoreroute", true, "Wechselt mit Ansage auf eine schnellere Route")
            if (prefs.b(PreferenceKeys.AUTO_REROUTE, true)) {
                val m = prefs.i(PreferenceKeys.AUTO_REROUTE_MIN, 5)
                SItem("Umleiten ab Ersparnis von", "Mindestersparnis der neuen Route")
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in listOf(3, 5, 10, 15)) Pill("$v Min", m == v, { prefs.put(PreferenceKeys.AUTO_REROUTE_MIN, v) })
                }
            }
        }
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, "Andere Routen auf der Karte anzeigen", "livealts", true, "Graue Linien mit +/- Minuten zum Antippen")
        SToggle(prefs, "Ziele vorschlagen", "predict", true, "Mit Live-Fahrzeit für Zuhause und Arbeitsplatz")
        SToggle(prefs, "Wetter auf der Route", "routewx", true)
    }
    SGroup("Routenstart") {
        SToggle(prefs, "Auto-Start-Countdown", PreferenceKeys.AUTO_START_ROUTE, true, "Startet die Navigation nach dem Countdown")
        SSlider("Auto-Start-Countdown", prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30).toFloat(), 3f..30f, " s", sub = "Dauer bis zum Start der Navigation", steps = 26, reset = 10f) { prefs.put(PreferenceKeys.AUTO_START_SECONDS, it.coerceIn(3, 30)) }
        SToggle(prefs, "3D-Flug entlang neuer Route", "cinematic", true, "Ein 3-Sekunden-3D-Flug vor der Übersicht")
        SToggle(prefs, "Routenflug-Button", "flyover", true, "3D-Flug über die gesamte Strecke vor der Abfahrt")
    }
}

// ------------------------------------------------------------------ Turn card & lanes

@Composable
private fun GuidanceSection(prefs: Prefs) {
    SGroup("Abbiegekarte") {
        SToggle(prefs, "Straßenschilder auf der Abbiegekarte", "uksigns", true)
        SToggle(prefs, "Lebendige Abbiegekarte", "livecard", true, "Wächst und leuchtet bei Annäherung an das Abbiegen")
        SToggle(prefs, "Spurführung auf der Abbiegekarte", "lanestrip", true)
        if (prefs.b(PreferenceKeys.LANE_STRIP, true)) SChoice("Spurführungsstil", listOf("hero" to "Große Spurenanzeige", "classic" to "Klassischer Streifen"), prefs.s(PreferenceKeys.LANE_STYLE, "hero"),
            if (prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero") "Großes Spurfoto vor Kreuzungen" else "Kleine Spurenleiste bei jeder Kreuzung") { prefs.put(PreferenceKeys.LANE_STYLE, it) }
        SChoice("Nächstes Abbiegen auf der Karte", listOf("close" to "Wenn nah", "always" to "Immer", "off" to "Aus"), prefs.s(PreferenceKeys.THEN_MODE, "close"),
            when (prefs.s(PreferenceKeys.THEN_MODE, "close")) { "close" -> "Nur wenn der übernächste Schritt direkt folgt"; "always" -> "Immer der übernächste Schritt"; else -> null }) { prefs.put(PreferenceKeys.THEN_MODE, it) }
        if (prefs.s(PreferenceKeys.THEN_MODE, "close") != "off") SChoice("Anzeigen als", listOf("row" to "Zeile unter der Karte", "icon" to "Kleiner Pfeil"), prefs.s(PreferenceKeys.THEN_STYLE, "row")) { prefs.put(PreferenceKeys.THEN_STYLE, it) }
    }
    SGroup("Spuren und Kreuzungen") {
        SToggle(prefs, "Spuren auf der Karte gezeichnet", "lanemap", true)
        SToggle(prefs, "Mapbox-Spurdaten auf TomTom-Routen nutzen", PreferenceKeys.LANES_MAPBOX, true)
        SToggle(prefs, "Heranzoomen an Spuren vor einer Kreuzung", "laneview", true)
        SToggle(prefs, "Kreuzungs-Nahansicht", "junctionview", true, "Nähere, flachere Ansicht bei Ausfahrten und Gabelungen")
        SToggle(prefs, "Karte beim Abbiegen auf 2D flachlegen", "turn2d", true)
        SToggle(prefs, "Neigung nach Geschwindigkeit", "speedtilt", true, "Flacher bei langsamer Fahrt, weitblickender bei Tempo")
        TiltSetting(prefs, "3D-Neigung, breiter Bildschirm", PreferenceKeys.TILT_WIDE)
        TiltSetting(prefs, "3D-Neigung, hoher Bildschirm", PreferenceKeys.TILT_TALL)
    }
    SGroup("Fahrtenkarte") {
        SToggle(prefs, "Routenleiste auf der Fahrtenkarte", "routebar", true, "Verkehr, Kameras, Raststätten und Fortschritt")
        SToggle(prefs, "Nächste Raststätten-Karte", "servicescard", true)
        SToggle(prefs, "Gefahrenradar", "hazardradar", false, "Kurven, Schulen, Schwellen, Blitzer (2 km)")
    }
    SGroup("Ankunft") {
        SToggle(prefs, "Ankunftsmoment", "arrivalfx", true, "Kamera schwenkt zum Ziel und zeigt Parkplätze")
        SToggle(prefs, "Parkplätze nahe dem Ziel", "parkprompt", true, "Parkplatzvorschläge vor der Ankunft")
        SToggle(prefs, "Rückroute", "routeback", true, "Ein Tipp für den Weg zurück zum Startort")
        SToggle(prefs, "Parkplatz über Bluetooth merken", "parkbt", true, "Speichert den Standort beim Trennen vom Auto-Bluetooth")
    }
}


/** Checks the Open Charge Map key, and shows what the last charger lookup found. */
@Composable
private fun OcmTestItem(prefs: Prefs) {
    val sc = rememberCoroutineScope()
    var st by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val last = com.headunit.launcher.ChargerInfo.lastNote
    val sub = st ?: if (last.isNotBlank()) "Letzte Abfrage: $last" else "Prüft den Schlüssel mit einer kleinen Abfrage"
    SItem("Ladesäulen-Daten prüfen", sub) {
        SButton(if (busy) "Prüft…" else "Prüfen") {
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
    var st by remember { mutableStateOf(Zones.all(ctx).let { z -> if (z.isEmpty()) "Noch nicht heruntergeladen" else "${z.size} Zonen: " + z.joinToString(", ") { it.name }.take(140) }) }
    var busy by remember { mutableStateOf(false) }
    SItem("Umweltzonen auf diesem Gerät", st) {
        SButton(if (busy) "Sucht…" else "Aktualisieren") {
            if (!busy) { busy = true; sc.launch { withContext(Dispatchers.IO) { Zones.fetch(ctx) }; st = Zones.status; busy = false } }
        }
    }
}

// ------------------------------------------------------------------ Alerts

@Composable
private fun AlertsSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Geschwindigkeit") {
        SToggle(prefs, "Warnen bei Überschreitung", "speedwarn", false)
        if (prefs.b(PreferenceKeys.SPEEDWARN, false)) SChoice("Warnen ab Überschreitung um", listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        SToggle(prefs, "Limitänderung ansagen", "limitchange", false)
        if (prefs.b(PreferenceKeys.LIMITCHANGE, false)) SChoice("Wie", listOf("both" to "Gong und Stimme", "voice" to "Nur Stimme", "chime" to "Nur Gong"), prefs.s(PreferenceKeys.LIMIT_HOW, "both"),
            "Mit Gong kommt die Stimme erst, wenn der Gong vorbei ist") { prefs.put(PreferenceKeys.LIMIT_HOW, it) }
        SToggle(prefs, "Geschätzte Limits anzeigen", "limitest", false, "Übliches Limit anzeigen, wo keins kartiert ist")
        // where you are bans camera warnings and "Off where banned" is on: say so here, or it looks as if the switch doesn't work
        val banned = com.headunit.launcher.CAMERA_BANNED[engine.detectedCountry]
        val camOff = banned != null && prefs.b(PreferenceKeys.CAMERA_WARNINGS, false) && prefs.b(PreferenceKeys.CAMBANOBEY, true)
        SToggle(prefs, "Blitzer", "cameras", false, if (camOff) Lang.t("Aus: in $banned nicht erlaubt", "Off: not allowed in $banned")
            else if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) engine.camNote.ifBlank { "Suche startet mit dem nächsten GPS-Signal" } else null)
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) SToggle(prefs, "Aus, wo verboten", "cambanobey", true, "Automatisch aus z. B. in DE, FR und CH")
        SToggle(prefs, "Blitzer auf der Karte zeigen", "cameramap", true, "Nur Anzeige; Warnungen dort nur mit „Blitzer“ an")
        SToggle(prefs, "Parkhäuser auf der Karte", "garagesmap", true, "Freie Plätze live, wo verfügbar. Daten: MobiData BW, ParkenDD")
        SToggle(prefs, "Sektionskontrollen (Abschnittskontrolle)", "avgzones", true, "Durchschnittstempo im Vergleich zum Limit")
    }
    SGroup("Unterwegs", footer = "Informationen stammen aus OpenStreetMap.") {
        SToggle(prefs, "Baustellen und Ereignisse", "incidents", true, "Baustellen und Sperrungen auf Autobahnen. Daten: Autobahn GmbH")
        SToggle(prefs, "Einbahnstraßen-Warnung", "wrongway", false, "Bei falscher Fahrtrichtung; Fehlalarme möglich")
        SToggle(prefs, "Bremsschwellen", "bumps", false)
        SToggle(prefs, "Schulen zu Schulzeiten", "schools", false)
        SToggle(prefs, "Umweltzonen", "zonewarn", true)
        SToggle(prefs, "Pausenerinnerung nach 2 Stunden", "breakremind", false)
        SToggle(prefs, "Bahnübergänge und Furten", "railford", false, "Warnschild und Sprachwarnung")
        SToggle(prefs, "Wetterhinweise", "wxadvice", true, "Hinweis bei Regen, Schnee, Nebel oder Glätte")
        SToggle(prefs, "Unwetterwarnungen (DWD)", "dwdwarn", true, "Amtliche Warnungen auf der Strecke (Deutschland)")
        SToggle(prefs, "Falschfahrer-Meldungen", PreferenceKeys.WRONGWAY_REPORTS, true, "Alarm bei Meldung bis 30 km voraus")
        SToggle(prefs, "Vignetten-Hinweis", PreferenceKeys.VIGNETTE_WARN, true, "Ansage ca. 30 km vor Vignetten- oder Mautland")
        SToggle(prefs, "Rettungsgasse", "rescuelane", true, "Hinweis im Autobahnstau")
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
    SGroup("Ton & Stimme") {
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
        SChoice("Sprachführung", listOf("0" to "Alle", "1" to "Nur Warnungen", "2" to "Aus"), engine.soundMode.toString(),
            "„Nur Warnungen“: ohne Abbiegehinweise") { prefs.put(PreferenceKeys.SOUND_MODE, it.toInt()) }
        SToggle(prefs, "Straßennamen ansagen", PreferenceKeys.VOICE_ROADS, true, "Aus gibt nur die Richtung an")
        SToggle(prefs, "Intelligente Anweisungen", "smartvoice", true, "Anweisungen mit Ausfahrtsnummern und Schildern")
        SChoice("Sprachkanal", listOf("nav" to "Navigation", "media" to "Medien"), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            "Medien wählen, falls keine Sprache zu hören ist") { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        SItem("Stimme und Sprache", "Nutzt die System-Sprachausgabe") { SButton("Öffnen") { openSystemSettings(ctx, "com.android.settings.TTS_SETTINGS") } }
        // the installed German voices: the more natural ones (Google's network voices) need internet, the others work offline
        val voices = remember { engine.installedVoices().take(8) }
        if (voices.isEmpty()) SNote("Keine passende Stimme installiert. Download unter „Stimme und Sprache“.")
        else {
            val saved = prefs.s(PreferenceKeys.TTS_VOICE, "")
            SChoice("Ansagestimme", voices.mapIndexed { i, v -> v.name to "Stimme ${i + 1} · " + (if (v.isNetworkConnectionRequired) "online" else "offline") },
                if (voices.any { it.name == saved }) saved else voices.first().name,
                "Online-Stimmen klingen natürlicher, benötigen aber Internet.") { engine.useVoice(it) }
            SButton("Probe anhören") { engine.speak(Lang.t("Nach 500 Metern links abbiegen.", "In 500 metres, turn left."), true) }
        }
        SItem("Lautstärke") { SButton("Öffnen") { openSystemSettings(ctx, Settings.ACTION_SOUND_SETTINGS) } }
    }
}


// ------------------------------------------------------------------ Places & trips

@Composable
private fun PlaceItem(label: String, p: Place?, onChange: () -> Unit, onClear: () -> Unit) {
    SItem(label, p?.let { it.name + (if (it.sub.isNotBlank()) ", " + it.sub else "") } ?: "Nicht festgelegt") {
        SButton(if (p != null) "Ändern" else "Festlegen", primary = true, onClick = onChange)
        if (p != null) SButton("Löschen", danger = true, onClick = onClear)
    }
}

@Composable
private fun PlacesSection(engine: NavEngine) {
    val pl = engine.places
    var settingSlot by remember { mutableStateOf<String?>(null) }
    val slotsRaw = engine.prefs.s(PreferenceKeys.PLACE_HOME, "") + engine.prefs.s("place_work", "")
    SGroup("Gespeicherte Orte", footer = "Auch über die Buttons unter der Suchleiste festlegbar.") {
        PlaceItem("Zuhause", remember(slotsRaw) { pl.slot("home") }, { settingSlot = "home" }) { pl.clearSlot("home") }
        PlaceItem("Arbeitsplatz", remember(slotsRaw) { pl.slot("work") }, { settingSlot = "work" }) { pl.clearSlot("work") }
    }
    settingSlot?.let { s -> AddPlaceDialog(engine, null, slot = s) { settingSlot = null } }
    val raw = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val mine = remember(raw) { pl.customs() }
    var renaming by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    SGroup("Eigene Orte", footer = "Eigene benannte Orte unter der Suchleiste.") {
        if (mine.isEmpty()) SNote("Noch keine eigenen Orte vorhanden.")
        mine.forEachIndexed { i, lp ->
            if (renaming == lp.label) {
                var t by remember(lp.label) { mutableStateOf(lp.label) }
                SItem("${lp.label} umbenennen") {
                    SField(t, { t = it.take(24) }, width = 220.dp)
                    SButton("Speichern", primary = true) { pl.renameCustom(lp.label, t); renaming = null }
                }
            } else SItem(lp.label, lp.place.name + if (lp.place.sub.isNotBlank()) ", " + lp.place.sub else "") {
                if (i > 0) SButton("↑") { pl.moveCustom(lp.label, -1) }
                SButton("Umbenennen") { renaming = lp.label }
                SButton("Entfernen", danger = true) { pl.removeCustom(lp.label) }
            }
        }
        SItem("Ort hinzufügen") { SButton("Hinzufügen", primary = true) { adding = true } }
    }
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val favRaw = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val favs = remember(favRaw) { pl.favourites() }
    var renamingFav by remember { mutableStateOf<Place?>(null) }
    var slotFor by remember { mutableStateOf<Place?>(null) }
    var taggingFav by remember { mutableStateOf<Place?>(null) }
    var tagFilter by remember { mutableStateOf("") }
    val tagRaw = engine.prefs.s(PreferenceKeys.FAV_TAGS, "")
    val tags = remember(favRaw, tagRaw) { pl.favTags() }
    SGroup("Favoriten") {
        if (favs.isEmpty()) SNote("Noch keine Favoriten vorhanden.")
        // your own tags ("Kunden", "Urlaub"): the list shows one tag's favourites at a time
        if (tags.isNotEmpty()) SItem("Schlagwort", if (tagFilter.isBlank() || tagFilter !in tags) "Alle" else tagFilter) {
            SButton("Alle", primary = tagFilter.isBlank() || tagFilter !in tags) { tagFilter = "" }
            for (t in tags) SButton(t, primary = tagFilter == t) { tagFilter = t }
        }
        favs.filter { tagFilter.isBlank() || tagFilter !in tags || pl.favTag(it) == tagFilter }.forEach { f ->
            when {
                taggingFav == f -> {
                    var t by remember(f) { mutableStateOf(pl.favTag(f)) }
                    SItem("Schlagwort für ${f.name}") {
                        SField(t, { t = it.take(20) }, placeholder = "z. B. Kunden", width = 200.dp)
                        SButton("Speichern", primary = true) { pl.setFavTag(f, t); taggingFav = null }
                    }
                }
                renamingFav == f -> {
                    var t by remember(f) { mutableStateOf(f.name) }
                    SItem("${f.name} umbenennen") {
                        SField(t, { t = it.take(40) }, width = 220.dp)
                        SButton("Speichern", primary = true) { pl.renameFavourite(f, t); renamingFav = null }
                    }
                }
                slotFor == f -> SItem("${f.name} festlegen als…") {
                    SButton("Zuhause", primary = true) { pl.setSlot("home", f); slotFor = null; engine.toast(engine.str(R.string.toast_063, f.name), 2500) }
                    SButton("Arbeitsplatz", primary = true) { pl.setSlot("work", f); slotFor = null; engine.toast(engine.str(R.string.toast_064, f.name), 2500) }
                    SButton("Abbrechen") { slotFor = null }
                }
                else -> SItem(f.name, listOf(pl.favTag(f).let { if (it.isBlank()) "" else "#$it" }, f.sub).filter { it.isNotBlank() }.joinToString(" · ")) {
                    SButton("Schlagwort") { taggingFav = f }
                    SButton("Umbenennen") { renamingFav = f }
                    SButton("Zuhause / Arbeit") { slotFor = f }
                    SButton("Entfernen", danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        if (favs.size > 1) SItem("Alle Favoriten entfernen") { SButton("Alle löschen", danger = true) { pl.clearFavourites() } }
    }
    SGroup("Verlauf", footer = "Zum Entfernen zur Seite wischen.") {
        SToggle(engine.prefs, "Gespeichert & Zuletzt unter der Suchleiste", "quicklist", true, "Die Liste der Favoriten und letzten Orte auf der Karte")
        SItem("Letzte Ziele", "${pl.recents().size} letzte Ziele") {
            SButton("Löschen", danger = true) { pl.clearHistory(); engine.toast(engine.str(R.string.rest_1437), 2000) }
        }
    }
    TripsGroup(engine)
}

@Composable
private fun TripsGroup(engine: NavEngine) {
    var shown by remember { mutableStateOf(engine.trips.all()) }
    val fmt = java.text.SimpleDateFormat("EEE, d. MMM, HH:mm", java.util.Locale.GERMANY)
    val k = if (engine.imperial) 2.23694 else 3.6
    val unit = if (engine.imperial) "mph" else "km/h"
    SGroup("Fahrtenbuch") {
        // closed at start to save room; the header (count and distance) opens and closes it
        var open by remember { mutableStateOf(false) }
        if (shown.isEmpty()) SNote("Noch keine Fahrten aufgezeichnet.")
        else SItem("${shown.size} Fahrten", Geo.fmtDist(shown.sumOf { it.dist }, engine.imperial) + " insgesamt", onClick = { open = !open }) {
            Text(if (open) "▴" else "▾", color = LocalVw.current.sub, fontSize = 18.sp)
        }
        if (!open) return@SGroup
        if (shown.isNotEmpty()) {
            SItem("Fahrtenbuch leeren", "Alle Fahrten löschen") { SButton("Löschen", danger = true) { engine.trips.clear(); shown = emptyList() } }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            SItem("Exportieren", "CSV oder PDF. Fahrt antippen: dienstlich oder privat.") {
                SButton("CSV") { com.headunit.launcher.TripExport.share(ctx, engine.prefs, shown, false)?.let { engine.toast("Export fehlgeschlagen: $it", 4000) } }
                SButton("PDF", primary = true) { com.headunit.launcher.TripExport.share(ctx, engine.prefs, shown, true)?.let { engine.toast("Export fehlgeschlagen: $it", 4000) } }
            }
        }
        val biz = com.headunit.launcher.TripExport.business(engine.prefs)
        shown.take(50).forEach { t ->
            val mins = ((t.end - t.start) / 60000L).toInt()
            val isBiz = t.start in biz
            SItem(
                fmt.format(java.util.Date(t.start)) + (if (t.to.isNotBlank()) " → ${t.to}" else ""),
                "${Geo.fmtDur(mins * 60.0)} · in Fahrt ${Geo.fmtDur(t.moving.toDouble())} · max ${Math.round(t.maxSpeed * k)} $unit · " + (if (isBiz) "Dienstlich" else "Privat"),
                onClick = { com.headunit.launcher.TripExport.toggleBusiness(engine.prefs, t.start) },
            ) { Text(Geo.fmtDist(t.dist, engine.imperial), color = LocalVw.current.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}


/** Share your ETA and places sent from other apps (part of Places & trips). */
/** Your own normal zoom when not on a route: a slider from further out to closer, and Reset. */
@Composable
private fun DefaultZoomRow(prefs: Prefs) {
    val c = LocalVw.current
    val saved = (prefs.s(PreferenceKeys.DEFAULT_ZOOM, "0").toFloatOrNull() ?: 0f).coerceIn(-2f, 2f)
    var v by remember(saved) { mutableStateOf(saved) }
    fun save(x: Float) = prefs.put(PreferenceKeys.DEFAULT_ZOOM, String.format(java.util.Locale.US, "%.2f", x))
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem("Standard-Zoom", "Zoom ohne aktive Route") {
            if (kotlin.math.abs(v) > 0.01f) SButton("Zurücksetzen") { v = 0f; save(0f) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_017), color = c.sub, fontSize = 13.sp)
            androidx.compose.material3.Slider(value = v, onValueChange = { v = (Math.round(it * 4f) / 4f) }, onValueChangeFinished = { save(v) },
                valueRange = -2f..2f, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            Text(stringResource(R.string.ui_010), color = c.sub, fontSize = 13.sp)
        }
    }
}

/**
 * Sending places from the phone to the head unit (through a free relay, encrypted with the pairing code). The head unit makes the
 * code; you type it on the phone.
 */
@Composable
private fun PhoneLinkGroup(prefs: Prefs) {
    val code = prefs.s(PreferenceKeys.LINK_CODE, "")
    val role = prefs.s(PreferenceKeys.LINK_ROLE, "")
    var typing by remember { mutableStateOf(false) }
    SGroup("Orte vom Smartphone senden", footer = "Einmalig koppeln: auf dem Autodisplay „Smartphone koppeln“ wählen.") {
        when {
            code.isNotBlank() && role == "car" -> {
                SItem("Dies ist das Auto", "Gekoppelt. Code ${com.headunit.launcher.CarLink.pretty(code)}") {
                    SButton("Entkoppeln", danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, ""); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                val qr = remember(code) { qrCode(com.headunit.launcher.CarLink.pretty(code), 300) }
                if (qr != null) Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp)) {
                        androidx.compose.foundation.Image(qr, "Kopplungscode als QR-Code", Modifier.size(98.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
                    }
                    Text(com.headunit.launcher.CarLink.pretty(code), color = LocalVw.current.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            code.isNotBlank() && role == "phone" -> SItem("Dies ist das Smartphone", "Mit deinem Auto gekoppelt (${com.headunit.launcher.CarLink.pretty(code)})") {
                SButton("Entkoppeln", danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, "") }
            }
            else -> {
                SItem("Auf dem Autodisplay", "Erstellt einen Code zum Eingeben auf dem Smartphone") {
                    SButton("Smartphone koppeln", primary = true) { prefs.put(PreferenceKeys.LINK_CODE, com.headunit.launcher.CarLink.newCode()); prefs.put(PreferenceKeys.LINK_ROLE, "car"); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                SItem("Auf dem Smartphone", "Code eingeben, den das Autodisplay anzeigt") { SButton("Code eingeben") { typing = true } }
                if (typing) SFieldItem("Code des Autos", "", "Zum Beispiel K7P-4QX-M2D", placeholder = "K7P-4QX-M2D") { t ->
                    val ok = com.headunit.launcher.CarLink.clean(t)
                    if (ok != null) { prefs.put(PreferenceKeys.LINK_CODE, ok); prefs.put(PreferenceKeys.LINK_ROLE, "phone"); typing = false }
                }
            }
        }
    }
}

@Composable
private fun SharingGroups(prefs: Prefs) {
    val ctx = LocalContext.current
    val pkg = android.net.Uri.parse("package:" + ctx.packageName)
    SGroup("Ankunftszeit teilen", footer = "Unter Optionen > „Ankunftszeit teilen“: Link zu Position, Ankunftszeit und Restweg. Endet bei Ankunft.") {
        SFieldItem("Name", prefs.s(PreferenceKeys.TRIP_NAME, ""), "Auf der Webseite, z. B. „Max ist unterwegs“",
            placeholder = "Name (optional)") { prefs.put(PreferenceKeys.TRIP_NAME, it.trim().take(40)) }
    }
    SGroup("Orte aus anderen Apps", footer = "Orte aus Google Maps, WhatsApp oder Kontakten teilen.") {
        SItem("Google Maps Links") {
            SButton("Standardmäßig öffnen") {
                if (android.os.Build.VERSION.SDK_INT >= 31) openSystemSettings(ctx, Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkg)
                else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
            }
        }
    }
}

// ------------------------------------------------------------------ Fuel & charging

@Composable
private fun FuelSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    SGroup("Kraftstoffpreise") {
        SChoice("Kraftstoff", listOf("E10" to "Super E10", "E5" to "Super E5", "B7" to "Diesel", "E85" to "Bioethanol (E85)", "EV" to "Elektro"), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
        // only an electric car gets stops planned on the route: for a combustion car the switch would do nothing, so it is not shown
        if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SToggle(prefs, "Ladestopps auf langen Fahrten planen", "smartfuel", true, "Wenn der Akku nicht bis zum Ziel reicht")
        // the UK government's Fuel Finder (pump prices): only in Great Britain and only for a combustion car
        val ukFuel = engine.countryCode().uppercase() in setOf("GB", "UK") && prefs.s(PreferenceKeys.FUEL_TYPE, "E10") != "EV"
        if (ukFuel) {
            SFieldItem("Client ID", prefs.s(PreferenceKeys.FFID, "")) { prefs.put(PreferenceKeys.FFID, it.trim()) }
            SFieldItem("Client Secret", prefs.s(PreferenceKeys.FFSECRET, ""), password = true) { prefs.put(PreferenceKeys.FFSECRET, it.trim()) }
            var st by remember { mutableStateOf(FuelFinder.status) }
            var busy by remember { mutableStateOf(false) }
            SItem("Preise", st.ifBlank { if (FuelFinder.configured(prefs)) "Noch nicht heruntergeladen" else "Zuerst Daten eintragen" }) {
                SButton(if (busy) "Sucht…" else "Jetzt aktualisieren", primary = true) {
                    if (!busy && FuelFinder.configured(prefs)) {
                        busy = true; st = "Tankstellen und Preise werden heruntergeladen…"
                        sc.launch {
                            try { withContext(Dispatchers.IO) { FuelFinder.refresh(ctx, prefs, force = true) } } catch (e: Exception) { }
                            st = FuelFinder.status; busy = false
                        }
                    }
                }
            }
            SItem("Falschen Preis entdeckt?") { SButton("Melden") { openSystemSettings(ctx, android.content.Intent.ACTION_VIEW, android.net.Uri.parse(FuelFinder.REPORT_URL)) } }
        }
    }
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") CloudAccountGroup(prefs)
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SGroup("Elektroauto") {
        EvCarItem(prefs)
        SFieldItem("Effizienz (km pro kWh)", prefs.s(PreferenceKeys.EVMPK, ""), placeholder = "z. B. 5.5", number = true) { s -> prefs.put(PreferenceKeys.EVMPK, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        // a chosen car brings its own battery size; the typed one is then not used
        val carKwh = if (prefs.s(PreferenceKeys.EV_CAR, "").isNotBlank()) com.headunit.launcher.EvCars.batteryKwh(prefs) else null
        if (carKwh != null) SItem("Nutzbare Batterie", "${evNum(carKwh)} kWh aus den Fahrzeugdaten")
        else SFieldItem("Nutzbare Batterie (kWh)", prefs.s(PreferenceKeys.EV_BATTERY_KWH, ""), "Für Akkuschätzung und Ladestopps (Standard: 60 kWh)", placeholder = "z. B. 64", number = true) { s -> prefs.put(PreferenceKeys.EV_BATTERY_KWH, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Strompreis (Cent pro kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = "z. B. 38", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
        SFieldItem("Restreichweite am Ziel (km)", prefs.s(PreferenceKeys.EV_DEST_KM, ""), "Mindestreichweite bei Ankunft (Standard: 50 km)", placeholder = "z. B. 50", number = true) { s -> prefs.put(PreferenceKeys.EV_DEST_KM, s.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(500)?.toString() ?: "") }
        SFieldItem("Restreichweite an der Ladesäule (km)", prefs.s(PreferenceKeys.EV_CHARGER_KM, ""), "Puffer, falls die Säule belegt ist (Standard: 30 km)", placeholder = "z. B. 30", number = true) { s -> prefs.put(PreferenceKeys.EV_CHARGER_KM, s.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(500)?.toString() ?: "") }
        SChoice("Ladesäulen für Ladestopps", listOf("0" to "Alle", "50" to "50 kW", "150" to "150 kW", "300" to "300 kW"), prefs.i(PreferenceKeys.EV_MIN_KW, 50).toString(),
            "Mindestleistung für Ladestopps. Säulen ohne bekannte Leistung nur als letzte Wahl. Daten: Bundesnetzagentur.de (CC BY 4.0).") { prefs.put(PreferenceKeys.EV_MIN_KW, it.toInt()) }
        EvOperatorsItem(prefs)
        SChoice("Umweg für bevorzugte Anbieter", listOf("5" to "5 Min.", "10" to "10 Min.", "15" to "15 Min.", "20" to "20 Min."), prefs.i(PreferenceKeys.EV_PREF_MIN, 10).toString(),
            "Maximaler Umweg ohne zusätzlichen Stopp (Standard: 10 Min.)") { prefs.put(PreferenceKeys.EV_PREF_MIN, it.toInt()) }
        SToggle(prefs, "Ladesäulen-Daten im Ausland laden", PreferenceKeys.FR_CHARGERS, false, "Lädt bei Bedarf Daten für Frankreich (ca. 136 MB/Monat), Niederlande und Brüssel. Quellen: transport.data.gouv.fr (Licence Ouverte 2.0), NDW (CC0), Brussels Mobility (CC BY 4.0).")
        EvWidgetItem(prefs)
    } else SGroup("Kraftstoffkosten auf der Route") {
        SFieldItem("Verbrauch (Liter pro 100 km)", prefs.s(PreferenceKeys.FUELMPG, ""), placeholder = "z. B. 6.5", number = true) { s -> prefs.put(PreferenceKeys.FUELMPG, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Kraftstoffpreis (Cent pro Liter)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = "z. B. 175.9", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
        SFieldItem("Tankinhalt (Liter)", prefs.s(PreferenceKeys.FUEL_TANK_L, ""), "Für die Tank-Reichweite", placeholder = "z. B. 50", number = true) { s -> prefs.put(PreferenceKeys.FUEL_TANK_L, s.filter { it.isDigit() }.take(3)) }
    }
    LearnedGroup(engine)
}

/** What the app has learned about your driving (and an electric car's use), with a button to start again. */
@Composable
private fun LearnedGroup(engine: NavEngine) {
    var rows by remember { mutableStateOf(engine.learnedSummary()) }
    SGroup("Aus Fahrten gelernt", footer = "Verbessert Ankunftszeit und Reichweite") {
        rows.forEach { (k, v) -> SItem(k, v) }
        SItem("Gelerntes zurücksetzen") { SButton("Zurücksetzen", danger = true) { engine.resetLearned(); rows = engine.learnedSummary() } }
    }
}


/** Town and village names for your country, so search works with no signal even without the full pack. */
@Composable
private fun OfflinePlacesGroup(engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    val cc = engine.chosenPack().country.ifBlank { engine.detectedCountry }.ifBlank { "DE" }
    val where = countryName(cc)
    var state by remember { mutableStateOf(if (OfflinePlaces.installed(ctx)) "Installiert (%.1f MB)".format(OfflinePlaces.sizeMb(ctx)) else "Nicht heruntergeladen") }
    var busy by remember { mutableStateOf(false) }
    SGroup("Ortsnamen", footer = Lang.t("Orte in $where für die Suche ohne Signal.", "Places in $where for search without signal.")) {
        SItem("Städte und Dörfer", state) {
            SButton(if (busy) "Lädt…" else if (OfflinePlaces.installed(ctx)) "Aktualisieren" else "Herunterladen", primary = !OfflinePlaces.installed(ctx)) {
                if (!busy) {
                    busy = true; state = "Lädt herunter (benötigt Verbindung)…"
                    sc.launch {
                        try {
                            val n = withContext(Dispatchers.IO) { OfflinePlaces.download(ctx, cc) }
                            state = "Installiert: $n Orte"; engine.toast(engine.str(R.string.rest_1436), 3000)
                        } catch (e: Exception) { state = "Fehlgeschlagen: ${e.message}" } finally { busy = false }
                    }
                }
            }
            if (OfflinePlaces.installed(ctx) && !busy) SButton("Entfernen", danger = true) { OfflinePlaces.delete(ctx); state = "Nicht heruntergeladen" }
        }
    }
}

// ------------------------------------------------------------------ Screen & sizes

@Composable
private fun ScreenSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Bildschirm") {
        SSlider("Helligkeit", prefs.i(PreferenceKeys.BRIGHT, 100).toFloat(), 10f..100f, "%", sub = "100% überlässt die Steuerung dem System", live = true) { prefs.put(PreferenceKeys.BRIGHT, it) }
        SToggle(prefs, "Bildschirm nachts dimmen", "dimnight", false)
        SToggle(prefs, "Bildschirm eingeschaltet lassen", "keepon", true)
        val screenConfig = androidx.compose.ui.platform.LocalConfiguration.current
        val autoPercent = recommendedMainUiScale(screenConfig.screenWidthDp, screenConfig.screenHeightDp)
        val autoScale = prefs.b(PreferenceKeys.UI_AUTO_SCALE, prefs.s(PreferenceKeys.UI_SCALE, "").isBlank())
        SItem(
            "Automatische UI-Skalierung",
            if (autoScale) Lang.t("Nach Bildschirmgröße (aktuell $autoPercent%)", "By screen size (now $autoPercent%)") else Lang.t("Manuelle Größe aktiv", "Manual size active")
        ) {
            // (the app's own switch, in the accent colour like all the others)
            com.headunit.launcher.ui.Switch(autoScale) { enabled ->
                if (!enabled) prefs.put(PreferenceKeys.UI_SCALE, autoPercent)
                prefs.put(PreferenceKeys.UI_AUTO_SCALE, enabled)
            }
        }
        SSlider(
            "Text- und Buttongröße",
            (if (autoScale) autoPercent else prefs.i(PreferenceKeys.UI_SCALE, autoPercent)).toFloat(),
            60f..140f, "%", sub = if (autoScale) "Schieberegler bewegen zum manuellen Anpassen" else "Manuell oder automatisch", steps = 15, reset = 100f
        ) {
            prefs.put(PreferenceKeys.UI_AUTO_SCALE, false)
            prefs.put(PreferenceKeys.UI_SCALE, it)
        }
        var mainAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Erweiterte Displaygrößen", "Einzelne Elemente unabhängig anpassen") {
            SButton(if (mainAdvanced) "Ausblenden" else "Anzeigen") { mainAdvanced = !mainAdvanced }
        }
        if (mainAdvanced) {
            SSlider("Karten-Größe", prefs.i(PreferenceKeys.MAIN_CARD_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Abbiege-, Fahrten- und Routenkarten", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_CARD_SIZE, it) }
            SSlider("Button-Größe", prefs.i(PreferenceKeys.MAIN_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Bedienelemente auf dem Bildschirm", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_BUTTON_SIZE, it) }
            SSlider("Textgröße", prefs.i(PreferenceKeys.MAIN_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", sub = "Unabhängige Textskalierung", steps = 15, reset = 100f) { prefs.put(PreferenceKeys.MAIN_TEXT_SIZE, it) }
            SSlider("Navigationspfeil-Größe", prefs.i(PreferenceKeys.MAIN_ARROW_SIZE, 100).toFloat(), 60f..180f, "%", sub = "Positionspfeil auf der Karte", steps = 11, reset = 100f) { prefs.put(PreferenceKeys.MAIN_ARROW_SIZE, it) }
            SSlider("Straßennamen-Größe", prefs.i(PreferenceKeys.ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Name der aktuellen Straße unter dem Pfeil", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.ROAD_NAME_SIZE, it) }
        }

    }
    SGroup("Beim Fahren") {
        SToggle(prefs, "Geschwindigkeit neben dem Limit", "mapspeed", true)
        SToggle(prefs, "Tachoring", "speedring", true, "Füllt sich bis zum Tempolimit, färbt sich gelb dann rot")
        SSlider("Tempolimit- und Tachogröße", prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = "Auf Smartphone und Autodisplay", steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SToggle(prefs, "Straße und Richtung beim Fahren ohne Route", "cruise", true)
        SToggle(prefs, "Pfeil ohne Route auf der Straße halten", "snaproad", true)
        SToggle(prefs, "Karte in Tunnels dimmen", "tunneldim", true)
        SToggle(prefs, "Weniger Buttons beim Fahren", "drivesimple", true)
        SToggle(prefs, "Tastatur beim Fahren sperren", "drivelock", false, "Sprachsuche funktioniert weiterhin")
    }
    SGroup("Einheiten") {
        SChoice("Entfernung und Tempo", listOf("km" to "Kilometer", "mi" to "Meilen"), if (engine.imperial) "mi" else "km") { prefs.put(PreferenceKeys.DISTANCE_UNIT, it) }
        SChoice("Temperatur", listOf("C" to "°C", "F" to "°F"), if (isImperial(prefs)) "F" else "C") { prefs.put(PreferenceKeys.TEMPERATURE_UNIT, it) }
        SChoice("Fahrseite", listOf("" to "Automatisch", "rhd" to "Rechtslenker (Linksverkehr)", "lhd" to "Linkslenker (Rechtsverkehr)"), prefs.s(PreferenceKeys.DRIVE, ""),
            when (prefs.s(PreferenceKeys.DRIVE, "")) {
                "rhd" -> "Fahrer rechts: Karten links, Buttons rechts"
                "lhd" -> "Fahrer links: Bildschirm gespiegelt, Karten rechts"
                else -> "Nach aktuellem Land"
            }) { prefs.put(PreferenceKeys.DRIVE, it) }
    }
}

// ------------------------------------------------------------------ Android Auto

@Composable
private fun AndroidAutoSection(prefs: Prefs) {
    val ctx = LocalContext.current
    SGroup("Autodisplay") {
        val gh = "com.google.android.projection.gearhead"
        val ghUri = android.net.Uri.parse("package:$gh")
        val hasAa = remember { try { ctx.packageManager.getPackageInfo(gh, 0); true } catch (e: Exception) { false } }
        val source = remember {
            try {
                val inst = if (android.os.Build.VERSION.SDK_INT >= 30) ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
                    else @Suppress("DEPRECATION") ctx.packageManager.getInstallerPackageName(ctx.packageName)
                when (inst) {
                    "com.android.vending" -> "Google Play"
                    null -> "Unbekannt (aus Datei installiert)"
                    ctx.packageName -> "Eigener Updater"
                    else -> inst
                }
            } catch (e: Exception) { "Unbekannt" }
        }
        SSlider("Gesamtgröße auf dem Autodisplay", prefs.i(PreferenceKeys.CARSCALE, 100).toFloat(), 40f..300f, "%", reset = 100f) { prefs.put(PreferenceKeys.CARSCALE, it) }
        SSlider("Tacho- und Tempolimitgröße", prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 140).toFloat(), 40f..220f, "%", reset = 140f) { prefs.put(PreferenceKeys.CAR_SPEED_SIZE, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Erweiterte Autodisplay-Größen", "Einzelne Elemente unabhängig anpassen") {
            SButton(if (carAdvanced) "Ausblenden" else "Anzeigen") { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider("Karten-Größe", prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider("Button-Größe", prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider("Textgröße", prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider("Navigationspfeil-Größe", prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 100).toFloat(), 30f..180f, "%", reset = 100f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider("Straßennamen-Größe", prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
        SGroup("Knöpfe auf der Fahrtkarte") {
            SToggle(prefs, "Übersicht", PreferenceKeys.TRIP_BTN_OVERVIEW, true, "Die ganze Route auf einen Blick")
            SToggle(prefs, "Andere Routen", PreferenceKeys.TRIP_BTN_OTHERS, true, "Die anderen Routen an oder aus")
            SToggle(prefs, "Ton", PreferenceKeys.TRIP_BTN_SOUND, true, "Alle Ansagen, nur Warnungen oder aus")
            SToggle(prefs, "Beenden", PreferenceKeys.TRIP_BTN_END, true, "Die Fahrt beenden")
            SChoice("Knopfgröße", listOf("normal" to "Normal", "large" to "Groß", "xl" to "Extra groß"), prefs.s(PreferenceKeys.TRIP_BTN_SIZE, "normal"),
                "Weniger Knöpfe machen die Karte schmaler, so bleibt mehr von der Karte frei") { prefs.put(PreferenceKeys.TRIP_BTN_SIZE, it) }
        }
        VehiclesGroup(prefs)
        SItem("Installiert durch", source)
        SItem("Sprachsuche im Auto", "„Hey Google, navigiere mit Horizon Nav zu …“. Ohne „mit Horizon Nav“ nur als Standard-Navigations-App.")
        SItem("Android Auto Einstellungen", if (hasAa) null else "Android Auto ist auf diesem Telefon nicht installiert") {
            if (hasAa) SButton("Öffnen") {
                val i = ctx.packageManager.getLaunchIntentForPackage(gh)
                try {
                    if (i != null) ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri)
                } catch (e: Exception) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
            }
        }
        if (hasAa) SItem("Android Auto App-Info") {
            SButton("App-Info") { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
        }
    }
}

// ------------------------------------------------------------------ Performance

@Composable
private fun PerformanceSection(prefs: Prefs) {
    SGroup("Flüssigkeit", footer = "Niedriger stellen, falls die Karte langsam reagiert.") {
        SChoice("Karten-Schärfe", listOf("100" to "Voll", "80" to "Ausgewogen", "65" to "Leicht", "50" to "Niedrigst"), prefs.s(PreferenceKeys.MAP_RESOLUTION, "80")) { prefs.put(PreferenceKeys.MAP_RESOLUTION, it) }
        SChoice("Bildrate", listOf("auto" to "Auto", "20" to "20", "30" to "30", "45" to "45", "60" to "60"), prefs.s(PreferenceKeys.MAP_FPS, "auto"),
            if (prefs.s(PreferenceKeys.MAP_FPS, "auto") == "auto") "Startet bei 30 FPS und passt sich an" else "Niedriger entlastet das Gerät") { prefs.put(PreferenceKeys.MAP_FPS, it) }
        SToggle(prefs, "Schneller Kartenaufbau", "mapfast", true)
        SToggle(prefs, "Leistungsanzeige", "perfmeter", false, "Anzeige von FPS und Renderschritten")
    }
    SGroup("Demo") {
        SToggle(prefs, "Demofahrt-Button", "demo", false, "Spielt eine Route ab zum Testen beim Parken")
    }
}

// ------------------------------------------------------------------ Keys & accounts

/** One button that tests every key entered above with a small request each, and lists what worked. */
@Composable
private fun KeyTestGroup(prefs: Prefs) {
    val sc = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<com.headunit.launcher.KeyTest.Result>>(emptyList()) }
    SGroup("Schlüssel testen", footer = "Eine Testabfrage je eingetragenem Schlüssel.") {
        SItem("Alle Schlüssel testen", if (results.isEmpty()) "Prüft, ob die eingetragenen Schlüssel funktionieren" else null) {
            SButton(if (busy) "Prüft…" else "Testen", primary = true) {
                if (!busy) { busy = true; sc.launch { results = withContext(Dispatchers.IO) { com.headunit.launcher.KeyTest.all(prefs) }; busy = false } }
            }
        }
        for (r in results) SItem((when (r.state) { com.headunit.launcher.KeyTest.State.OK -> "✓ "; com.headunit.launcher.KeyTest.State.BAD -> "✗ "; com.headunit.launcher.KeyTest.State.NONE -> "– "; else -> "? " }) + r.name, r.note)
    }
}

@Composable
private fun KeysSection(prefs: Prefs, engine: NavEngine) {
    KeyTestGroup(prefs)
    SGroup("Kraftstoffpreise außerhalb Großbritanniens", footer = "Deutschland: kostenloser Schlüssel von creativecommons.tankerkoenig.de") {
        SFieldItem("Tankerkönig-Schlüssel (Deutschland)", prefs.s(PreferenceKeys.TANKERKOENIG_KEY, ""), placeholder = "Schlüssel einfügen", password = true) { prefs.put(PreferenceKeys.TANKERKOENIG_KEY, it.trim()) }
    }
    SGroup("Ladesäulen-Daten außerhalb Deutschlands") {
        SFieldItem("Open Charge Map API-Schlüssel", prefs.s(PreferenceKeys.OCM_KEY, ""), "Optional, kostenlos auf openchargemap.org. Daten: Bundesnetzagentur.de (CC BY 4.0), Open Charge Map (CC BY-SA 4.0), NDW (CC0), Brussels Mobility (CC BY 4.0).", placeholder = "Schlüssel einfügen", password = true) { prefs.put(PreferenceKeys.OCM_KEY, it.trim()) }
        OcmTestItem(prefs)
    }
    SGroup("Suche") {
        SChoice("Suchergebnisse", searchCountryOptions(engine), searchCountryValue(prefs)) { prefs.put(PreferenceKeys.COUNTRY, it) }
        if (engine.hasKey && engine.hasMapbox) SChoice("Suchen mit", listOf("auto" to "TomTom", "mapbox" to "Mapbox"), if (prefs.s(PreferenceKeys.SEARCHWITH, "auto") == "mapbox") "mapbox" else "auto",
            "TomTom ist am genauesten für Adressen") { prefs.put(PreferenceKeys.SEARCHWITH, it) }
        else if (!engine.hasKey) SNote("Für genaueste Adresssuche: TomTom-Schlüssel eintragen.")
    }
    SGroup("Kartenkonten", footer = "Ohne Empfang: Routen und Suche aus dem Offline-Paket.") {
        SFieldItem("TomTom API-Schlüssel", prefs.s(PreferenceKeys.API_KEY, ""), "Kostenlos erhältlich auf developer.tomtom.com",
            placeholder = "Schlüssel einfügen") { prefs.setApiKey(it) }
        SFieldItem("HERE API-Schlüssel", prefs.s(PreferenceKeys.HERE_KEY, ""), "Für genauere Tempolimits. Kostenlos auf platform.here.com",
            placeholder = "Schlüssel einfügen") { prefs.put(PreferenceKeys.HERE_KEY, it.trim()) }
        if (prefs.s(PreferenceKeys.HERE_KEY, "").isNotBlank()) {
            val st = com.headunit.launcher.HereRest.status
            SNote("HERE: " + st.ifBlank { "noch keine Abfrage" })
        }
        SFieldItem("Mapillary Client-Token", prefs.s(PreferenceKeys.MAPILLARY_KEY, ""), "Tempolimits aus Straßenfotos. Kostenlos auf mapillary.com",
            placeholder = "Token einfügen (MLY|…)") { prefs.put(PreferenceKeys.MAPILLARY_KEY, it.trim()) }
        if (prefs.s(PreferenceKeys.MAPILLARY_KEY, "").isNotBlank()) {
            val st = com.headunit.launcher.Mapillary.status
            SNote("Mapillary: " + st.ifBlank { "noch keine Abfrage" })
        }
        SFieldItem("Mapbox Zugangstoken", prefs.s(PreferenceKeys.MAPBOX_TOKEN, ""), "Öffentliches Token (beginnt mit pk.) einfügen",
            placeholder = "Token einfügen") { prefs.put(PreferenceKeys.MAPBOX_TOKEN, com.headunit.launcher.MapboxRest.clean(it)) }
    }
    SGroup("Satellitenbilder", footer = "Automatisch: Mapbox, Landesamt (DE), Esri, MapTiler, EOX. Anbieter ohne Schlüssel werden übersprungen.") {
        SChoice("Quelle", listOf("auto" to "Automatisch", "mapbox" to "Mapbox", "landesamt" to "Landesamt", "esri" to "Esri", "maptiler" to "MapTiler", "eox" to "EOX"),
            prefs.s(PreferenceKeys.SAT_SOURCE, "auto"), "Landesamt: amtliche Luftbilder (nur Deutschland)") { prefs.put(PreferenceKeys.SAT_SOURCE, it) }
        SChoice("Straßen über dem Luftbild", listOf("subtle" to "Dezent", "normal" to "Normal", "strong" to "Kräftig"),
            prefs.s(PreferenceKeys.SAT_HYBRID, "normal")) { prefs.put(PreferenceKeys.SAT_HYBRID, it) }
        SChoice("Luftbilder vorab laden", listOf("wifi" to "Nur WLAN", "always" to "Immer", "off" to "Aus"),
            prefs.s(PreferenceKeys.SAT_PREFETCH, "wifi"), "Speichert Luftbilder entlang der Route") { prefs.put(PreferenceKeys.SAT_PREFETCH, it) }
        SatStoreItem()
        SFieldItem("Esri API-Schlüssel", prefs.s(PreferenceKeys.ESRI_KEY, ""), "Kostenlos auf location.arcgis.com, mit Zugriff auf Basemaps",
            placeholder = "Schlüssel einfügen") { prefs.put(PreferenceKeys.ESRI_KEY, it.trim()) }
        SFieldItem("MapTiler API-Schlüssel", prefs.s(PreferenceKeys.MAPTILER_KEY, ""), "Kostenlos auf cloud.maptiler.com",
            placeholder = "Schlüssel einfügen") { prefs.put(PreferenceKeys.MAPTILER_KEY, it.trim()) }
    }
    if (engine.hasKey && engine.hasMapbox) SGroup("Routen, Suche und Verkehr") {
        SChoice("Nutzung", listOf("auto" to "Auto", "tomtom" to "TomTom", "mapbox" to "Mapbox"), prefs.s(PreferenceKeys.DATAPROV, "auto")) { prefs.put(PreferenceKeys.DATAPROV, it) }
    }
}

// ------------------------------------------------------------------ About & backup

@Composable
private fun AboutSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    val pkg = android.net.Uri.parse("package:" + ctx.packageName)
    SGroup("Horizon Nav") {
        // seven quick taps on the version reveal the log export (see DebugLog)
        var taps by remember { mutableStateOf(0) }
        var lastTap by remember { mutableStateOf(0L) }
        var logsOn by remember { mutableStateOf(com.headunit.launcher.DebugLog.unlocked) }
        SItem("Version", BuildConfig.VERSION_NAME, onClick = {
            val now = System.currentTimeMillis()
            taps = if (now - lastTap > 3000) 1 else taps + 1
            lastTap = now
            if (taps >= 7) {
                taps = 0
                if (!logsOn) { logsOn = true; com.headunit.launcher.DebugLog.unlocked = true; engine.toast("Log-Optionen eingeblendet", 2500) }
            }
        })
        if (logsOn) SItem("Logs", "In die Zwischenablage. Kann Standortdaten enthalten.") {
            SButton("Kopieren") {
                engine.toast("Logs werden gesammelt …", 2000)
                Thread {
                    val text = com.headunit.launcher.DebugLog.dump()
                    try {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Horizon Nav Logs", text))
                    } catch (e: Exception) { }
                    android.os.Handler(android.os.Looper.getMainLooper()).post { engine.toast("Logs kopiert", 3000) }
                }.start()
            }
        }
        if (com.headunit.launcher.Updater.enabled) {
            SItem("Nach Updates suchen", engine.updateStatus.ifBlank { null }) {
                val u = engine.appUpdate
                if (u != null && !engine.updateBusy) SButton("Aktualisieren auf ${u.version}", primary = true) { engine.installAppUpdate() }
                else SButton(if (engine.updateBusy) "Sucht…" else "Jetzt prüfen") { engine.checkForUpdate(manual = true) }
            }
            SToggle(prefs, "Automatisch nach Updates suchen", "updcheck", true, "Beim Öffnen der App mit Verbindung")
        }
        SItem("Einrichtung", "Ersteinrichtung erneut durchführen") { SButton("Einrichtung starten") { prefs.put(PreferenceKeys.SETUP_DONE, false); prefs.put(PreferenceKeys.FORCESETUP, true) } }
        var feedback by remember { mutableStateOf(false) }
        SItem("Feedback oder Problem", "Sende einen Bericht mit App-Version und Gerät") { SButton("Melden", primary = true) { feedback = true } }
        if (feedback) FeedbackDialog(engine) { feedback = false }
    }
    SGroup("Berechtigungen") {
        val hasLoc = ctx.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        SItem("Standort", if (hasLoc) "Erlaubt" else "Nicht erlaubt: Navigation benötigt Standort") {
            SButton("App-Berechtigungen", primary = !hasLoc) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg) }
        }
        SItem("Standortdienste") { SButton("Öffnen") { openSystemSettings(ctx, Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
    }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(prefs.exportJson().toByteArray()) }
                true
            } catch (e: Exception) { false }
            android.widget.Toast.makeText(ctx, if (ok) tr("Sicherung gespeichert") else tr("Sicherung konnte nicht gespeichert werden"), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val msg = try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                "${prefs.importJson(text)} Einstellungen wiederhergestellt"
            } catch (e: Exception) { "Datei ist keine gültige Sicherung" }
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    // the API keys: their own file, protected by a password the driver chooses (the normal backup never holds them)
    var keyDlg by remember { mutableStateOf("") }   // "", "export" or "import": the password dialog is open
    var keyPw by remember { mutableStateOf("") }
    fun keyToast(m: String) = android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_LONG).show()
    val keySaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val pw = keyPw
            keyToast(try {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(KeyBackup.export(prefs, pw).toByteArray()) }
                "Schlüssel gesichert"
            } catch (e: Exception) { e.message ?: "Schlüssel konnten nicht gesichert werden" })
        }
        keyPw = ""
    }
    val keyOpener = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val pw = keyPw
            keyToast(try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                KeyBackup.import(prefs, text, pw).let { n -> Lang.t("$n Schlüssel wiederhergestellt", "$n keys restored") }
            } catch (e: Exception) { e.message ?: "Schlüssel konnten nicht wiederhergestellt werden" })
        }
        keyPw = ""
    }
    if (keyDlg.isNotEmpty()) androidx.compose.material3.AlertDialog(
        onDismissRequest = { keyDlg = ""; keyPw = "" },
        title = { Text(if (keyDlg == "export") tr("Passwort für die Schlüssel-Sicherung") else tr("Passwort der Schlüssel-Sicherung")) },
        text = {
            androidx.compose.material3.OutlinedTextField(value = keyPw, onValueChange = { keyPw = it }, singleLine = true,
                label = { Text(tr("Passwort (mind. 6 Zeichen)")) },
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = keyPw.length >= 6, onClick = {
                val mode = keyDlg
                keyDlg = ""
                if (mode == "export") keySaver.launch("horizon-nav-keys.json") else keyOpener.launch(arrayOf("application/json", "text/plain", "*/*"))
            }) { Text(tr("Weiter")) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { keyDlg = ""; keyPw = "" }) { Text(tr("Abbrechen")) } },
    )
    SGroup("Sicherung") {
        // the buttons sit under the title: beside it they squeezed the title into a narrow column
        SItem("Einstellungen und gespeicherte Orte")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SButton("Sichern") { exporter.launch("horizon-nav-backup.json") }
            SButton("Wiederherstellen") { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
        }
        SItem("API-Schlüssel", "Eigene Datei, mit Passwort geschützt")
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SButton("Schlüssel sichern") { keyDlg = "export" }
            SButton("Schlüssel wiederherstellen") { keyDlg = "import" }
        }
    }
    val cf = java.io.File(ctx.filesDir, "lastcrash.txt")
    var rep by remember { mutableStateOf(if (cf.exists()) cf.readText() else "") }
    if (rep.isNotBlank()) SGroup("Fehlerbericht") {
        Text(rep.take(900), color = LocalVw.current.text, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
        SItem("Bericht löschen") { SButton("Löschen") { cf.delete(); rep = "" } }
    }
}

/** Readable name of a country code ("Deutschland", "Österreich"...). */
internal fun countryName(cc: String): String = when (cc.uppercase()) {
    "DE" -> "Deutschland"; "GB" -> "Großbritannien"; "AT" -> "Österreich"; "CH" -> "Schweiz"; "US" -> "USA"; "" -> ""
    else -> java.util.Locale("", cc).getDisplayCountry(java.util.Locale.GERMAN).ifBlank { cc }
}

/** Search limited to the country you're in (followed as you travel) or anywhere. */
internal fun searchCountryOptions(engine: NavEngine): List<Pair<String, String>> {
    val here = engine.detectedCountry
    return listOf("auto" to (if (here.isNotBlank()) "Nur in ${countryName(here)}" else "Nur in meinem Land"), "" to "Überall")
}

internal fun searchCountryValue(prefs: Prefs): String = if (prefs.s(PreferenceKeys.COUNTRY, "auto").isBlank()) "" else "auto"

/**
 * Feedback: say what happened in a line or two, then send it as a GitHub issue (the browser opens with it filled in) or copy it to
 * paste anywhere (Reddit, a message). Either way the app version, unit, main settings and the last crash go with it.
 */
@Composable
private fun FeedbackDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var note by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.ui_016), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.ui_063), color = c.sub, fontSize = 14.sp)
            SField(note, { note = it.take(600) }, placeholder = "z. B. Falsch umgeleitet bei Frankfurt", width = 516.dp, fill = true)
            Text(stringResource(R.string.ui_047), color = c.sub, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                SButton("Schließen") { onClose() }
                SButton("Kopieren") {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText(ctx.getString(R.string.ui_024), engine.feedbackReport(note)))
                    engine.toast(engine.str(R.string.rest_1441), 3500)
                }
                SButton("Auf GitHub senden", primary = true) {
                    val title = note.lineSequence().firstOrNull()?.take(80)?.ifBlank { null } ?: "Bericht von Horizon Nav ${com.headunit.launcher.BuildConfig.VERSION_NAME}"
                    val body = engine.feedbackReport(note).take(5000)
                    val url = "https://github.com/${BuildConfig.UPDATE_REPO}/issues/new?title=" + android.net.Uri.encode(title) + "&body=" + android.net.Uri.encode(body)
                    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (e: Exception) { engine.toast(engine.str(R.string.rest_1443), 3500) }
                    onClose()
                }
            }
        }
    }
}

/**
 * Settings > Fuel > Your electric car: takes the battery level from another app's widget (e.g. the car maker's app). The
 * widget is hosted out of sight; Android asks once whether Horizon Nav may create widgets.
 */
@Composable
private fun EvWidgetItem(prefs: Prefs) {
    val ctx = LocalContext.current
    val c = LocalVw.current
    var picking by remember { mutableStateOf(false) }
    var pendingId by remember { mutableIntStateOf(-1) }
    val id = com.headunit.launcher.EvWidget.widgetId(prefs)
    val configure = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val pid = pendingId; pendingId = -1
        if (pid < 0) return@rememberLauncherForActivityResult
        if (r.resultCode == android.app.Activity.RESULT_OK) com.headunit.launcher.EvWidget.finishBind(ctx, prefs, pid)
        else com.headunit.launcher.EvWidget.cancelBind(ctx, pid)
    }
    fun afterBind(pid: Int) {
        val cfg = com.headunit.launcher.EvWidget.configureIntent(ctx, pid)
        if (cfg != null) { pendingId = pid; try { configure.launch(cfg) } catch (e: Exception) { com.headunit.launcher.EvWidget.finishBind(ctx, prefs, pid); pendingId = -1 } }
        else com.headunit.launcher.EvWidget.finishBind(ctx, prefs, pid)
    }
    val bind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val pid = pendingId
        if (pid < 0) return@rememberLauncherForActivityResult
        if (r.resultCode == android.app.Activity.RESULT_OK) afterBind(pid)
        else { pendingId = -1; com.headunit.launcher.EvWidget.cancelBind(ctx, pid) }
    }
    val at = com.headunit.launcher.EvWidget.readAt(prefs)
    val name = remember(id) { com.headunit.launcher.EvWidget.providerLabel(ctx, prefs) }
    val sub = when {
        id < 0 -> "Ladestand aus dem Widget der Hersteller-App"
        at > 0 -> (when (com.headunit.launcher.EvWidget.source(prefs)) { "car" -> "Direkt vom Auto"; "cloud" -> "Aus der Cloud des Herstellers"; else -> name }) + " · ${prefs.s(PreferenceKeys.EV_NOW_PCT, "–")} % · ${prefs.s(PreferenceKeys.EV_NOW_KM, "–")} km, gelesen um " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(at))
        else -> "$name · Noch kein Prozentwert gefunden. Das Widget meldet sich, sobald es sich aktualisiert."
    }
    // the buttons sit on their own line: beside the title they squeeze the text into a narrow column
    SItem("Ladestand aus Widget", sub)
    // (scrolls sideways rather than squeezing a button down to one letter on a narrow screen)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // short labels, so all three fit on one line on a phone
        if (id >= 0) SButton("App öffnen") {
            if (!com.headunit.launcher.EvWidget.openProvider(ctx, prefs))
                android.widget.Toast.makeText(ctx, tr("Die Auto-App lässt sich nicht öffnen."), android.widget.Toast.LENGTH_LONG).show()
        }
        SButton(if (id >= 0) "Ändern" else "Widget wählen", primary = id < 0) { picking = true }
        if (id >= 0) SButton("Entfernen", danger = true) { com.headunit.launcher.EvWidget.remove(ctx, prefs) }
    }
    if (id >= 0) {
        SToggle(prefs, "Widget automatisch aktualisieren", PreferenceKeys.EV_AUTOREFRESH, true, "Aktualisiert das Widget regelmäßig über Android Auto")
        if (prefs.b(PreferenceKeys.EV_AUTOREFRESH, true)) {
            SNote("Achtung: fragt jede Minute den Herstellerserver ab (bei VW wird das Auto geweckt). Kann gegen die AGB verstoßen und zur Sperrung führen. Nutzung auf eigene Verantwortung.")
        }
        val note = com.headunit.launcher.EvWidget.refreshNote
        if (note.isNotBlank()) Text("Zuletzt: $note", color = LocalVw.current.sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
    }
    // while no value has been read: the texts the widget shows, so it's visible whether a percentage is among them
    if (id >= 0 && at == 0L) {
        val texts = com.headunit.launcher.EvWidget.lastTexts
        Text(
            if (texts.isEmpty()) "Noch kein Text vom Widget. Erst nach „App öffnen“ und Aktualisierung."
            else "Text im Widget: " + texts.joinToString("  |  "),
            color = LocalVw.current.sub, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp),
        )
    }
    if (picking) {
        val list = remember { com.headunit.launcher.EvWidget.providers(ctx) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { picking = false }) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(tr("Widget mit dem Ladestand"), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(tr("Widget der Hersteller-App wählen. Der Prozentwert wird daraus gelesen."), color = c.sub, fontSize = 14.sp)
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (list.isEmpty()) Text(tr("Keine Widgets gefunden"), color = c.sub, fontSize = 15.sp)
                    for (info in list) {
                        val label = remember(info) { com.headunit.launcher.EvWidget.label(ctx, info) }
                        Text(label, color = c.text, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                            picking = false
                            val (pid, intent) = com.headunit.launcher.EvWidget.beginBind(ctx, info)
                            if (intent == null) afterBind(pid)
                            else { pendingId = pid; try { bind.launch(intent) } catch (e: Exception) { pendingId = -1; com.headunit.launcher.EvWidget.cancelBind(ctx, pid) } }
                        }.padding(horizontal = 10.dp, vertical = 12.dp))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SButton("Abbrechen") { picking = false } }
            }
        }
    }
}

/** A kWh or kW figure the German way: 77 or 77,4. */
private fun evNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.GERMANY, "%.1f", v)

/** "77 kWh · 175 kW" for a car in the list (the power is left out when the dataset doesn't know it). */
private fun evCarDetails(car: com.headunit.launcher.EvCar): String =
    "${evNum(car.kwh)} kWh" + (car.maxKw?.let { " · ${evNum(Math.round(it).toDouble())} kW" } ?: "")

/**
 * Settings > Fuel > Your electric car > Preferred operators: a menu of the common networks with a tick box each. Ticking one
 * keeps the menu open, so several can be picked in one go; the charging-stop plan prefers their chargers (see NavEngine.planEv).
 */
@Composable
private fun EvOperatorsItem(prefs: Prefs) {
    val c = LocalVw.current
    var open by remember { mutableStateOf(false) }
    val ops = com.headunit.launcher.ChargeOperators.all
    val sel = com.headunit.launcher.ChargeOperators.selected(prefs)
    val names = ops.filter { it.id in sel }.map { it.label }
    val summary = when {
        names.isEmpty() -> "Noch keine ausgewählt"
        names.size <= 4 -> "Ausgewählt: " + names.joinToString(", ")
        else -> "${names.size} ausgewählt: " + names.take(3).joinToString(", ") + " …"
    }
    Box(Modifier.fillMaxWidth()) {
        // a real button, so it's clear the list opens from here
        SItem("Bevorzugte Ladeanbieter", Lang.t("$summary. Bevorzugt bei kleinem Umweg.", "$summary. Preferred if the detour is small."), onClick = { open = true }) {
            SButton(if (names.isEmpty()) "Auswählen ▾" else "Ändern ▾") { open = true }
        }
        // limited height and scrolling: the car's screen is short
        androidx.compose.material3.DropdownMenu(
            expanded = open, onDismissRequest = { open = false },
            modifier = Modifier.heightIn(max = 360.dp).widthIn(min = 260.dp),
            containerColor = c.panel,
        ) {
            if (sel.isNotEmpty()) androidx.compose.material3.DropdownMenuItem(
                text = { Text(tr("Alle abwählen"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) },
                onClick = { com.headunit.launcher.ChargeOperators.setSelected(prefs, emptySet()) },
            )
            for (o in ops) {
                val on = o.id in sel
                fun toggle() = com.headunit.launcher.ChargeOperators.setSelected(prefs, if (on) sel - o.id else sel + o.id)
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(o.label, color = c.text, fontSize = 15.sp) },
                    onClick = { toggle() },
                    leadingIcon = {
                        androidx.compose.material3.Checkbox(
                            checked = on, onCheckedChange = { toggle() },
                            colors = androidx.compose.material3.CheckboxDefaults.colors(checkedColor = c.accent, uncheckedColor = c.sub, checkmarkColor = if (c.accent.luminance() > 0.6f) Color(0xFF10131C) else Color.White),
                        )
                    },
                )
            }
        }
    }
}

/**
 * Settings > Fuel > Your electric car > Your car: the car is picked from the open EV dataset (downloaded once, when the picker
 * opens), with the battery widget's text as a suggestion. Its battery and charging curve give the charging-stop times.
 */
@Composable
private fun EvCarItem(prefs: Prefs) {
    var picking by remember { mutableStateOf(false) }
    val label = prefs.s(PreferenceKeys.EV_CAR, "")
    val sub = if (label.isBlank()) "Für genaue Ladezeiten an Ladestopps"
    else {
        val kwh = com.headunit.launcher.EvCars.batteryKwh(prefs)
        val kw = prefs.s(PreferenceKeys.EV_CAR_KW, "").toDoubleOrNull()
        label + (kwh?.let { " · ${evNum(it)} kWh" } ?: "") + (kw?.let { " · max. ${evNum(Math.round(it).toDouble())} kW" } ?: "")
    }
    // (the dataset's licence asks for the source to be shown)
    SItem("Fahrzeug", sub + "\nFahrzeugdaten: " + com.headunit.launcher.EvCars.SOURCE)
    // the buttons sit on their own line, as for the widget below
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SButton(if (label.isBlank()) "Wählen" else "Ändern", primary = label.isBlank()) { picking = true }
        if (label.isNotBlank()) SButton("Entfernen", danger = true) { com.headunit.launcher.EvCars.clear(prefs) }
    }
    if (picking) EvCarDialog(prefs) { picking = false }
}

/** Picks the car: suggestions from the battery widget's text first, then the whole list, narrowed by a search. */
@Composable
private fun EvCarDialog(prefs: Prefs, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var all by remember { mutableStateOf<List<com.headunit.launcher.EvCar>?>(null) }
    var failed by remember { mutableStateOf(false) }
    // bumped by "Erneut versuchen" to load again
    var attempt by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(attempt) {
        failed = false
        try { all = withContext(Dispatchers.IO) { com.headunit.launcher.EvCars.load(ctx) } }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { failed = true }
    }
    val cars = all
    val texts = com.headunit.launcher.EvWidget.lastTexts
    val suggested = remember(cars, texts) { if (cars == null) emptyList() else com.headunit.launcher.EvCars.suggest(cars, texts) }
    val shown = remember(cars, query) {
        val words = query.lowercase().split(' ').filter { it.isNotBlank() }
        cars.orEmpty().filter { car -> val l = car.label.lowercase(); words.all { l.contains(it) } }
    }
    fun pick(car: com.headunit.launcher.EvCar) { com.headunit.launcher.EvCars.select(prefs, car); onClose() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("Fahrzeug"), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            when {
                failed -> {
                    Text(tr("Fahrzeugdaten nicht verfügbar. Internetverbindung prüfen."), color = c.sub, fontSize = 15.sp)
                    SButton("Erneut versuchen", primary = true) { attempt++ }
                }
                cars == null -> Text(tr("Fahrzeugdaten werden geladen …"), color = c.sub, fontSize = 15.sp)
                else -> {
                    if (suggested.isNotEmpty()) {
                        Text(tr("Vorschlag aus dem Widget"), color = c.sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Column(Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) {
                            for (car in suggested) EvCarRow(car) { pick(car) }
                        }
                    }
                    SField(query, { query = it.take(60) }, placeholder = "Suchen, z. B. ID.7 Pro", width = 516.dp, fill = true)
                    androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f, fill = false).fillMaxWidth().heightIn(max = 360.dp)) {
                        if (shown.isEmpty()) item { Text(tr("Kein Fahrzeug gefunden"), color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(vertical = 12.dp)) }
                        items(shown.size) { i -> val car = shown[i]; EvCarRow(car) { pick(car) } }
                    }
                }
            }
            Text(tr("Fahrzeugdaten: ") + com.headunit.launcher.EvCars.SOURCE, color = c.sub, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SButton("Abbrechen") { onClose() } }
        }
    }
}

/** One car in the picker: its name, then battery and top charging power. */
@Composable
private fun EvCarRow(car: com.headunit.launcher.EvCar, onClick: () -> Unit) {
    val c = LocalVw.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Text(car.label, color = c.text, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(evCarDetails(car), color = c.sub, fontSize = 13.sp)
    }
}

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
        Triple("faded", "Blass", listOf(0xFFF1F1F1, 0xFFD9E2EA, 0xFFE2E8DE, 0xFFFFFFFF, 0xFFE6E6E6)),
        Triple("grey", "Grau", listOf(0xFFE8E8E8, 0xFFCFCFCF, 0xFFDADADA, 0xFFFFFFFF, 0xFFD4D4D4)),
    )
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SItem("Farben", "Farbschema für die Karte wählen.")
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
private fun SatStoreItem() {
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
    SItem("Luftbild-Speicher", (if (mb < 0) "…" else "$mb MB von 500 MB") + progress) {
        SButton("Leeren", danger = true) { com.headunit.launcher.SatTileCache.clear(ctx); mb = 0 }
    }
}

/** Settings > Fahrzeug: car, motorhome or car with trailer, with the motorhome's or trailer's top speed, size and weight. */
@Composable
private fun VehicleGroup(prefs: Prefs) {
    val kind = prefs.s(PreferenceKeys.VEHICLE, "car")
    SGroup("Fahrzeug", footer = if (kind == "car") "Höchsttempo, Maße und Gewicht für Wohnmobil, Lkw oder Anhänger." else
        "Routen meiden zu niedrige, schmale oder zu schwach belastbare Strecken, soweit bekannt. Gilt ab der nächsten Route.") {
        SChoice("Profil", listOf("car" to "Pkw", "camper" to "Wohnmobil", "lorry" to "Lastkraftwagen"), kind) { prefs.put(PreferenceKeys.VEHICLE, it) }
        if (kind != "car") {
            val d = com.headunit.launcher.Vehicle.defaults(kind)
            VehicleField(prefs, kind, "max", "Höchsttempo (km/h)", d.maxKmh.toString(), "km/h")
            VehicleField(prefs, kind, "h", "Höhe (m)", d.heightM.toString(), "m")
            VehicleField(prefs, kind, "w", "Breite (m)", d.widthM.toString(), "m")
            VehicleField(prefs, kind, "l", "Länge (m)", d.lengthM.toString(), "m")
            VehicleField(prefs, kind, "kg", "Gesamtgewicht (kg)", d.weightKg.toString(), "kg")
            VehicleField(prefs, kind, "axlekg", "Achsgewicht (kg)", d.axleWeightKg.toString(), "kg")
        }
    }
}

@Composable
private fun VehicleField(prefs: Prefs, kind: String, what: String, title: String, def: String, unit: String) {
    SFieldItem(title, prefs.s(com.headunit.launcher.Vehicle.key(kind, what), ""), Lang.t("Leer = $def $unit", "Empty = $def $unit"), placeholder = def, number = true) {
        prefs.put(com.headunit.launcher.Vehicle.key(kind, what), it.trim().replace(',', '.'))
    }
}


/** The battery level from the car maker's cloud: the user's own account and own (free) developer keys, nothing in between. */
@Composable
private fun CloudAccountGroup(prefs: Prefs) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    val prov = com.headunit.launcher.CarCloud.provider(prefs)
    val footer = when (prov) {
        "volvo" -> Lang.t("Entwicklerkonto auf developer.volvocars.com: Anwendung mit Redirect-URI ${com.headunit.launcher.CarCloud.REDIRECT}, Energy API und Connected Vehicle API. Client-ID, Secret und VCC-API-Key einfügen, dann anmelden.", "Developer account at developer.volvocars.com: app with redirect URI ${com.headunit.launcher.CarCloud.REDIRECT}, Energy API and Connected Vehicle API. Paste client ID, secret and VCC API key, then sign in.")
        "bmw" -> "Auf bmw-cardata.bmwgroup.com CarData aktivieren, einen Client erstellen und die Client-ID hier einfügen. Max. 50 Abrufe pro Tag."
        "smartcar" -> Lang.t("Konto auf dashboard.smartcar.com (ein Fahrzeug gratis): Anwendung mit Redirect-URI ${com.headunit.launcher.CarCloud.REDIRECT}. Client-ID und Secret einfügen, anmelden, Fahrzeug freigeben.", "Account at dashboard.smartcar.com (one vehicle free): app with redirect URI ${com.headunit.launcher.CarCloud.REDIRECT}. Paste client ID and secret, sign in, allow the vehicle.")
        else -> "Akkustand vom Herstellerserver, wenn Android Auto ihn nicht liefert. Zugangsdaten bleiben auf dem Gerät."
    }
    SGroup("Akkustand aus der Cloud", footer = footer) {
        SChoice("Anbieter", listOf("none" to "Aus", "bmw" to "BMW / Mini", "volvo" to "Volvo", "smartcar" to "Smartcar (viele Marken)"), prov,
            "Smartcar: viele Marken, für ein Fahrzeug kostenlos") { prefs.put(PreferenceKeys.CLOUD, it) }
        if (prov != "none") {
            SFieldItem("Client-ID", prefs.s(PreferenceKeys.CLOUD_ID, ""), placeholder = "aus dem Entwicklerportal") { prefs.put(PreferenceKeys.CLOUD_ID, it.trim()) }
            if (prov != "bmw") SFieldItem("Client-Secret", prefs.s(PreferenceKeys.CLOUD_SECRET, ""), password = true) { prefs.put(PreferenceKeys.CLOUD_SECRET, it.trim()) }
            if (prov == "volvo") SFieldItem("VCC-API-Key", prefs.s(PreferenceKeys.CLOUD_VCC, ""), password = true) { prefs.put(PreferenceKeys.CLOUD_VCC, it.trim()) }
            SChoice("Abrufrate", listOf("5" to "5 Min.", "10" to "10 Min.", "20" to "20 Min.", "30" to "30 Min.", "60" to "60 Min."), com.headunit.launcher.CarCloud.rateMin(prefs).toString(),
                "Nur bei offener App, aktiver Route oder Android Auto." +
                    (if (prov == "bmw") Lang.t(" BMW: max. 50 Abrufe pro Tag (heute ${com.headunit.launcher.CarCloud.callsToday(prefs)}), danach Pause bis morgen.", " BMW: max 50 calls a day (today ${com.headunit.launcher.CarCloud.callsToday(prefs)}), then paused until tomorrow.") else "")) {
                prefs.put(PreferenceKeys.CLOUD_RATE + prov, it)
            }
            val st = com.headunit.launcher.CarCloud.status
            val signed = com.headunit.launcher.CarCloud.signedIn(prefs)
            SItem("Konto", st.ifBlank { if (signed) "Angemeldet" else "Nicht angemeldet" }) {
                SButton(if (signed) "Neu anmelden" else "Anmelden", primary = !signed) { com.headunit.launcher.CarCloud.signIn(ctx, prefs) }
            }
            if (signed) {
                SItem("Akkustand jetzt abrufen") {
                    SButton("Abrufen", primary = true) { sc.launch(Dispatchers.IO) { com.headunit.launcher.CarCloud.refresh(ctx, prefs, force = true) } }
                }
                SButton("Abmelden", danger = true) { com.headunit.launcher.CarCloud.signOut(prefs) }
            }
        }
    }
}

/** The 3D tilt for one kind of screen: automatic, or your own angle from 30 to 65 degrees (0 in the settings = automatic). */
@Composable
internal fun TiltSetting(prefs: Prefs, title: String, key: String) {
    val v = prefs.i(key, 0)
    val own = v in 30..65
    SChoice(title, listOf("auto" to Lang.t("Auto", "Auto"), "own" to Lang.t("Eigene Neigung", "Own angle")), if (own) "own" else "auto",
        sub = if (own) null else Lang.t("Wie bisher, je nach Tempo und Ansicht", "As before, depending on speed and view")) { pick ->
        prefs.put(key, if (pick == "own") 55 else 0)
    }
    if (own) SSlider("Neigung", v.toFloat(), 30f..65f, "°", steps = 34, reset = 55f) { prefs.put(key, it.coerceIn(30, 65)) }
}

/** Several cars (Android Auto > Fahrzeuge): each car is its Bluetooth device, and keeps its own fuel and electric car settings. */
@Composable
private fun VehiclesGroup(prefs: Prefs) {
    SGroup("Fahrzeuge") {
        val cars = com.headunit.launcher.VehicleProfiles.cars(prefs)
        val active = com.headunit.launcher.VehicleProfiles.activeId(prefs)
        val pend = com.headunit.launcher.VehicleProfiles.pending(prefs)
        if (pend != null) {
            SItem("Unbekanntes Bluetooth-Gerät: ${pend.second}", "Welches Fahrzeug ist das? Danach wird es beim Verbinden automatisch gewählt.")
            for (c in cars) SButton("Zu ${c.name}") {
                com.headunit.launcher.VehicleProfiles.assign(prefs, c.id, pend.first)
                com.headunit.launcher.VehicleProfiles.clearPending(prefs)
                com.headunit.launcher.VehicleProfiles.switchTo(prefs, c.id)
            }
            SButton("Als neues Fahrzeug anlegen", primary = true) {
                com.headunit.launcher.VehicleProfiles.create(prefs, pend.second, pend.first)
                com.headunit.launcher.VehicleProfiles.clearPending(prefs)
            }
            SButton("Nicht jetzt") { com.headunit.launcher.VehicleProfiles.clearPending(prefs) }
        }
        if (cars.isEmpty()) SItem("Noch kein Fahrzeug angelegt", "Die Einstellungen gelten für alle Autos, bis du ein Fahrzeug anlegst.")
        for (c in cars) {
            val isActive = c.id == active
            SItem(c.name, if (isActive) "Aktiv · ${if (c.bt.isEmpty()) "kein Bluetooth-Gerät zugeordnet" else c.bt.size.toString() + " Bluetooth-Gerät(e)"}"
                else "Antippen, um dieses Fahrzeug zu wählen") { com.headunit.launcher.VehicleProfiles.switchTo(prefs, c.id) }
            SFieldItem("Name", c.name, "Wie das Auto heißt, z. B. „ID.4“") { com.headunit.launcher.VehicleProfiles.rename(prefs, c.id, it) }
            if (!isActive) SButton("Löschen", danger = true) { com.headunit.launcher.VehicleProfiles.delete(prefs, c.id) }
        }
        SButton("Aktuelle Einstellungen als neues Fahrzeug speichern") {
            com.headunit.launcher.VehicleProfiles.create(prefs, "Fahrzeug ${cars.size + 1}", null)
        }
    }
}

