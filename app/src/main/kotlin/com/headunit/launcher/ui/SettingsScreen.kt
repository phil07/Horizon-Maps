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
    Section("Kartenansicht", "Licht, Wetter, Details, 3D, Satellit", Icons.Rounded.Map, Color(0xFF4C8DFF), "KARTE"),
    Section("Auf der Karte", "Verkehr, Ampeln, Kameras, Schilder", Icons.Rounded.Traffic, Color(0xFF4C8DFF)),
    Section("Pfeil & Routenlinie", "Pfeil oder Auto, Farben, Routendesign", Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
    Section("Routen", "Vermeiden, Öko, schnellere Routen, Start", Icons.Rounded.Route, Color(0xFF12A4B4), "FAHREN"),
    Section("Abbiegekarte & Spuren", "Schilder, Spuren, Fahrtenkarte, Ankunft", Icons.Rounded.TurnRight, Color(0xFF12A4B4)),
    Section("Warnungen", "Tempo, Blitzer, Gefahren, Meldungen", Icons.Rounded.NotificationsActive, Color(0xFFEA4335)),
    Section("Ton & Stimme", "Sprachansagen und Lautstärke", Icons.Rounded.VolumeUp, Color(0xFFEA4335)),
    Section("Orte & Fahrten", "Zuhause, Arbeit, Favoriten, Teilen, Fahrtenbuch", Icons.Rounded.Star, Color(0xFFF29900), "DU"),
    Section("Kraftstoff & Laden", "Spritart, Preise, Elektroauto", Icons.Rounded.LocalGasStation, Color(0xFF34A853)),
    Section("Offline-Karten", "Karten, Routen und Orte ohne Empfang", Icons.Rounded.CloudDownload, Color(0xFF8E5CF0)),
    Section("Bildschirm & Größen", "Helligkeit, Größen, Einheiten", Icons.Rounded.BrightnessMedium, Color(0xFFFFB020), "GERÄT"),
    Section("Beim Fahren", "Straßenname, Tacho, Tunnel, Medien, Sperren", Icons.Rounded.DirectionsCar, Color(0xFF12A4B4)),
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
        // on a phone, the map pages (Map style, On the map, Your arrow) keep the live map at the top while you scroll the settings,
        // so each change shows as you make it. The map moves here from the map screen (which is off while the settings are open).
        if (narrowS && !showList && cat <= 2) Box(
            Modifier.fillMaxWidth().height((androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp / 3).dp).padding(horizontal = 4.dp, vertical = 4.dp),
        ) { MapHost(engine.map, Modifier.fillMaxSize(), cornerDp = 18) }
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
                        Icon(Icons.Rounded.ArrowBack, "Alle Einstellungen", tint = c.text, modifier = Modifier.size(22.dp))
                    }
                    Text(tr(sections[cat].title), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp).weight(1f))
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 4.dp, vertical = 4.dp)) {
                if (!narrowS) Text(tr(sections[cat].title), color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(tr(sections[cat].sub), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, top = if (narrowS) 4.dp else 0.dp, bottom = 16.dp))
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
        SChoice("Tag und Nacht", listOf("auto" to "Auto", "lights" to "Scheinwerfer", "sun" to "Sonnenstand", "times" to "Feste Zeiten", "day" to "Tag", "night" to "Nacht", "phone" to "Dunkelmodus des Telefons"), prefs.s(PreferenceKeys.THEME, "auto"),
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
        SChoice("Kartenquelle", listOfNotNull(
            "osm" to "OpenStreetMap",
            if (engine.hasMapbox) "mapbox" to "Mapbox" else null,
            if (engine.hasKey) "tomtom" to "TomTom" else null,
        ), prefs.mapSource(),
            "Mapbox: 3D-Karte. Benötigt das Mapbox-Token.") { prefs.put(PreferenceKeys.MAP_STYLE, it) }
        if (prefs.mapSource() == "mapbox") {
            SChoice("Kartendetails", listOf("full" to "Vollständiges 3D", "lite" to "Lite", "lightest" to "Einfachst"), prefs.mapDetail(),
                "Lite: flache Gebäude. Einfachst: 2D-Karte.") { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
            MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { prefs.put(PreferenceKeys.MAP_COLOURS, it) }
        }
        SToggle(prefs, "Hinweis zu HD-Spuren", PreferenceKeys.HD_DIAG, false, "Nur im Demo-Modus: zeigt, ob die Spurdaten für die Abbiegung gefunden wurden. Die Spurführung selbst bleibt unverändert")
        if (prefs.mapSource() == "mapbox" && prefs.mapDetail() == "full") SChoice("HD-Straßen", listOf("auto" to "Auto", "on" to "An", "off" to "Aus"), prefs.s(PreferenceKeys.HD_ROADS2, "auto"),
            "Fahrspuren, Abbiegepfeile und 3D-Brücken. Auto: nur in UK an, sonst zeigt die Karte in manchen Ländern nur die Hauptstraßen.") { prefs.put(PreferenceKeys.HD_ROADS2, it) }
        DefaultZoomRow(prefs)
        SToggle(prefs, "Fußwege und Pfade", PreferenceKeys.PATHS, true, "Ausschalten für eine übersichtlichere Fahrkarte")
        SToggle(prefs, "Favoriten auf der Karte", PreferenceKeys.FAV_ON_MAP, true, "Gelbe Sterne mit Namen. Beim Fahren ohne Namen")
        SSlider("Symbole gespeicherter Orte", prefs.i(PreferenceKeys.SAVED_ICON_SIZE, 100).toFloat(), 50f..200f, " %", "Zuhause, Arbeit und markierte Orte", reset = 100f) { prefs.put(PreferenceKeys.SAVED_ICON_SIZE, it.toString()) }
        SToggle(prefs, "Glaskarten", PreferenceKeys.GLASS, true, "Halbtransparente Karten")
        SChoice("Mapbox-Logo-Position", listOf("tl" to "Oben links", "tr" to "Oben rechts", "bl" to "Unten links", "br" to "Unten rechts"), prefs.s(PreferenceKeys.MAPBOX_LOGO, "tr"),
            "Ecke für das geforderte Mapbox-Logo wählen") { prefs.put(PreferenceKeys.MAPBOX_LOGO, it) }
    }
    SGroup("Wetter") {
        SToggle(prefs, "Wettereffekte auf der Karte", PreferenceKeys.RAIN_EFFECTS, false, "Regen, Schnee und Nebel passend zum Wetter")
        SToggle(prefs, "Regen auf der Karte", PreferenceKeys.FX_RAIN, true, "Nur wenn die Wettereffekte an sind")
        SToggle(prefs, "Schnee auf der Karte", PreferenceKeys.FX_SNOW, true, "Nur wenn die Wettereffekte an sind")
        SToggle(prefs, "Nebel auf der Karte", PreferenceKeys.FX_FOG, true, "Nur wenn die Wettereffekte an sind")
        SToggle(prefs, "Regenradar-Ebene", PreferenceKeys.RAIN_RADAR, false, "Live-Radar von RainViewer unter der Route")
    }
    SGroup("3D und Satellit") {
        SToggle(prefs, "Satellit", PreferenceKeys.SATELLITE, false)
        SToggle(prefs, "3D-Gebäude", PreferenceKeys.BUILDINGS_3D, true)
        SToggle(prefs, "Detaillierte Fassaden", PreferenceKeys.FACADES, true, "Fenster, Türen und Schaufenster an Gebäuden")
        SToggle(prefs, "3D-Sehenswürdigkeiten", PreferenceKeys.LANDMARKS3D, true, "Detaillierte Modelle bekannter Gebäude und Stadien")
        SToggle(prefs, "3D-Berge und Täler", PreferenceKeys.TERRAIN, false, "Echtes Gelände")
        SToggle(prefs, "Nebel und Himmel", PreferenceKeys.SKYWEATHER, true, "Dunst am Horizont passend zum Wetter")
        SToggle(prefs, "Nachtsternenhimmel in 3D", PreferenceKeys.NIGHTSKY, true, "Sterne über dem Horizont bei Nacht")
        SToggle(prefs, "Bäume in Parks", PreferenceKeys.TREES, true, "3D-Bäume in Parks und an Straßen")
        SToggle(prefs, "Freie Sicht auf die Straße voraus", PreferenceKeys.CLEARVIEW, true, "Bäume und Gebäude verdecken die Straße nicht")
        SToggle(prefs, "Tippen zum Hervorheben", PreferenceKeys.TAPSELECT, true, "Tippe auf ein Gebäude oder einen Ort zum Auswählen")
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Verkehr") {
        SToggle(prefs, "Live-Verkehr", PreferenceKeys.TRAFFIC, true)
        SToggle(prefs, "Verkehrsübersicht beim Halt", PreferenceKeys.STOPPED_OVERVIEW, true, "Zoomt nach 30 s Stillstand heraus")
        SToggle(prefs, "Verkehr nur auf der Route anzeigen", PreferenceKeys.ROUTETRAFFICONLY, false, "Aus zeigt auch Verkehr auf umliegenden Straßen")
        SToggle(prefs, "Grüner Verkehr bei freier Fahrt", PreferenceKeys.TRAFFIC_GREEN, false, "Frei fließende Hauptstraßen grün anzeigen")
    }
    SGroup("Auf der Route") {
        SToggle(prefs, "Ampeln", PreferenceKeys.SIGNALS, true, if (prefs.b(PreferenceKeys.SIGNALS, true)) "Nur Ampeln auf deiner Route. Aktuell: " + engine.map.signalInfo() else null)
        SToggle(prefs, "Blitzer und Baustellen", PreferenceKeys.HAZARD_ICONS, true)
        SToggle(prefs, "Zebrastreifen", PreferenceKeys.ZEBRAS, true, "Auf der Straße gezeichnet")
        SToggle(prefs, "Stoppschilder", PreferenceKeys.STOP_SIGNS, false, "Mit kurzem Ton")
        SToggle(prefs, "Hinweis „Vorfahrt gewähren“", PreferenceKeys.GIVE_WAY, false, "Standardmäßig aus")
    }
    SGroup("3D-Straßenausstattung") {
        SToggle(prefs, "3D-Straßenlaternen, Ampeln und Kameras", PreferenceKeys.FURNITURE3D, true, "3D-Modelle in echter Größe")
        SToggle(prefs, "Straßenlaternen", PreferenceKeys.LAMPS, false, if (prefs.b(PreferenceKeys.LAMPS, false)) "Aktuell: " + engine.map.lampInfo() else null)
        if (prefs.b(PreferenceKeys.LAMPS, false)) SToggle(prefs, "Laternen aus OpenStreetMap", PreferenceKeys.LAMPS_ONLINE, true, "Echte Laternenstandorte, wo Leute sie eingetragen haben. Einmal pro Gebiet geladen, einen Monat gespeichert")
        if (prefs.b(PreferenceKeys.LAMPS, false)) SSlider("Laternenabstand", prefs.s(PreferenceKeys.LAMPGAP, "40").toFloatOrNull() ?: 40f, 15f..120f, " m", reset = 40f) { prefs.put(PreferenceKeys.LAMPGAP, it.toString()) }
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
            SChoice("Modellform", listOf("arrow" to "Pfeil", "car" to "Auto", "own" to "Dein Modell"), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) { prefs.put(PreferenceKeys.MODEL_SHAPE, it) }
            if (prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "own") {
                // your own model (.glb, up to 8 MB): sized to match the built-in car; the built-in car stands in while there's none
                val hasOwn = com.headunit.launcher.OwnModel.factor(ctx) != null
                val ownName = prefs.s(PreferenceKeys.OWN_MODEL_NAME, "")
                val pickOwn = rememberOwnModelPicker { ok, name ->
                    if (ok) {
                        prefs.put(PreferenceKeys.OWN_MODEL_NAME, name.ifBlank { "3D" })
                        prefs.put(PreferenceKeys.OWN_MODEL_STAMP, System.currentTimeMillis().toString())
                    } else android.widget.Toast.makeText(ctx, tr("Modell nicht lesbar (eine .glb-Datei, höchstens 8 MB)"), android.widget.Toast.LENGTH_LONG).show()
                }
                SItem("Dein Modell", if (hasOwn) "Aktuell: $ownName" else "Noch keins gewählt · eine .glb-Datei, höchstens 8 MB. Bis dahin steht das eingebaute Auto da.") {
                    SButton(if (hasOwn) "Ersetzen" else "Datei wählen", primary = true) {
                        if (!pickOwn()) android.widget.Toast.makeText(ctx, tr("Keine App auf diesem Gerät kann eine Datei auswählen"), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
                if (hasOwn) {
                    SSlider("Größe des Modells", prefs.i(PreferenceKeys.OWN_MODEL_SIZE, 100).toFloat(), 30f..300f, "%", reset = 100f) { prefs.put(PreferenceKeys.OWN_MODEL_SIZE, it.toInt()) }
                    SChoice("Drehen", listOf("0" to "0°", "90" to "90°", "180" to "180°", "270" to "270°"), prefs.i(PreferenceKeys.OWN_MODEL_TURN, 0).toString(),
                        "Dreht das Modell, bis es in Fahrtrichtung zeigt") { prefs.put(PreferenceKeys.OWN_MODEL_TURN, it.toInt()) }
                }
            }
            SToggle(prefs, "Leuchten", PreferenceKeys.MODEL_GLOW, true, "Weiches blaues Leuchten um das Modell")
            if (prefs.mapDetail() != "full") SNote("Nur mit Kartendetails „Vollständiges 3D“")
        }
        SToggle(prefs, "Scheinwerferkegel bei Nacht", PreferenceKeys.HEADLIGHT_BEAM, false, "Nach Einbruch der Dunkelheit liegt ein weicher Lichtkegel vor dem Pfeil, bei höherem Tempo länger")
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
        SToggle(prefs, "Abbiegepfeil auf der Route", PreferenceKeys.TURNARROWS, true, "Ein weißer Pfeil durch die nächste Kreuzung")
        SToggle(prefs, "Pfeile entlang der Route", PreferenceKeys.ROUTECHEVRONS, true, "Kleine Pfeile entlang der Routenlinie beim Zoomen")
        SToggle(prefs, "Routenschattenband", PreferenceKeys.RIBBON, true, "Ein weicher Schatten unter der Route")
        SToggle(prefs, "3D-Fahnen", PreferenceKeys.FLAGS_3D, true, "Fahnen an Ziel und Stopps")
        if (prefs.b(PreferenceKeys.FLAGS_3D, true)) SToggle(prefs, "Fahnen wehen", PreferenceKeys.FLAGS_WAVE, true, "Fahnen bewegen sich im Wind")
        SToggle(prefs, "Fließendes Licht auf der Route", PreferenceKeys.ROUTEFLOW, true)
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
        SToggle(prefs, "Mautstraßen vermeiden", PreferenceKeys.AVOID_TOLL, false, if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) "Überall" else null)
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
        SToggle(prefs, "Autobahnen vermeiden", PreferenceKeys.AVOID_MOTORWAY, false)
        SToggle(prefs, "Fähren vermeiden", PreferenceKeys.AVOID_FERRY, false)
        SToggle(prefs, "Autozüge vermeiden", PreferenceKeys.AVOID_CARTRAIN, false, "Z. B. Eurotunnel und die Alpen-Tunnelzüge. Ist ein Autozug der einzige Weg, wird er trotzdem genommen")
        SToggle(prefs, "Unbefestigte Straßen vermeiden", PreferenceKeys.AVOID_UNPAVED, true)
        SToggle(prefs, "Umweltzonen vermeiden", PreferenceKeys.AVOIDZONES, false, "Eine Start- oder Zielzone kann nicht vermieden werden")
        ZonesItem()
    }
    SGroup("Routenauswahl") {
        SToggle(prefs, "Öko-Routen", PreferenceKeys.ROUTE_ECO, false, "Kraftstoffsparend statt am schnellsten")
        SToggle(prefs, "Schnellere Routen anbieten", PreferenceKeys.FASTER_ROUTE, true)
        SToggle(prefs, "Einfachere Routen bevorzugen", PreferenceKeys.SIMPLER_ROUTE, true, "Eine Route, die nur etwa eine Minute langsamer ist, aber viel weniger Abbiegungen hat, wird zuerst gewählt")
        SToggle(prefs, "Auf der Bordsteinseite ankommen", PreferenceKeys.CURB_ARRIVAL, true, "Ankunft auf der Straßenseite des Ziels (Mapbox)")
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) {
            SToggle(prefs, "Automatisch umleiten", PreferenceKeys.AUTO_REROUTE, true, "Wechselt mit Ansage auf eine schnellere Route")
            if (prefs.b(PreferenceKeys.AUTO_REROUTE, true)) {
                val m = prefs.i(PreferenceKeys.AUTO_REROUTE_MIN, 5)
                SItem("Umleiten ab Ersparnis von", "Mindestersparnis der neuen Route")
                androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in listOf(3, 5, 10, 15)) Pill("$v Min", m == v, { prefs.put(PreferenceKeys.AUTO_REROUTE_MIN, v) })
                }
            }
        }
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, "Andere Routen auf der Karte anzeigen", PreferenceKeys.LIVE_ALTERNATIVES, true, "Graue Linien mit +/- Minuten zum Antippen")
        SToggle(prefs, "Ziele vorschlagen", PreferenceKeys.PREDICT, true, "Mit Live-Fahrzeit für Zuhause und Arbeitsplatz")
        SToggle(prefs, "Wetter auf der Route", PreferenceKeys.ROUTEWX, true)
    }
    SGroup("Routenstart") {
        SToggle(prefs, "Auto-Start-Countdown", PreferenceKeys.AUTO_START_ROUTE, true, "Startet die Navigation nach dem Countdown")
        if (prefs.b(PreferenceKeys.AUTO_START_ROUTE, true)) SSlider("Auto-Start-Countdown", prefs.i(PreferenceKeys.AUTO_START_SECONDS, 10).coerceIn(3, 30).toFloat(), 3f..30f, " s", sub = "Dauer bis zum Start der Navigation", steps = 26, reset = 10f) { prefs.put(PreferenceKeys.AUTO_START_SECONDS, it.coerceIn(3, 30)) }
        SToggle(prefs, "3D-Flug entlang neuer Route", PreferenceKeys.CINEMATIC, true, "Ein 3-Sekunden-3D-Flug vor der Übersicht")
        SToggle(prefs, "Routenflug-Button", PreferenceKeys.FLYOVER, true, "3D-Flug über die gesamte Strecke vor der Abfahrt")
    }
}

// ------------------------------------------------------------------ Turn card & lanes

@Composable
private fun GuidanceSection(prefs: Prefs) {
    SGroup("Abbiegekarte") {
        SToggle(prefs, "Straßenschilder auf der Abbiegekarte", PreferenceKeys.UKSIGNS, true)
        SToggle(prefs, "Lebendige Abbiegekarte", PreferenceKeys.LIVECARD, true, "Wächst und leuchtet bei Annäherung an das Abbiegen")
        SToggle(prefs, "Spurführung auf der Abbiegekarte", PreferenceKeys.LANE_STRIP, true)
        if (prefs.b(PreferenceKeys.LANE_STRIP, true)) SChoice("Spurführungsstil", listOf("hero" to "Große Spurenanzeige", "classic" to "Klassischer Streifen"), prefs.s(PreferenceKeys.LANE_STYLE, "hero"),
            if (prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero") "Großes Spurfoto vor Kreuzungen" else "Kleine Spurenleiste bei jeder Kreuzung") { prefs.put(PreferenceKeys.LANE_STYLE, it) }
        SChoice("Nächstes Abbiegen auf der Karte", listOf("close" to "Wenn nah", "always" to "Immer", "off" to "Aus"), prefs.s(PreferenceKeys.THEN_MODE, "close"),
            when (prefs.s(PreferenceKeys.THEN_MODE, "close")) { "close" -> "Nur wenn der übernächste Schritt direkt folgt"; "always" -> "Immer der übernächste Schritt"; else -> null }) { prefs.put(PreferenceKeys.THEN_MODE, it) }
        if (prefs.s(PreferenceKeys.THEN_MODE, "close") != "off") SChoice("Anzeigen als", listOf("row" to "Zeile unter der Karte", "icon" to "Kleiner Pfeil"), prefs.s(PreferenceKeys.THEN_STYLE, "row")) { prefs.put(PreferenceKeys.THEN_STYLE, it) }
    }
    SGroup("Spuren und Kreuzungen") {
        SToggle(prefs, "Spuren auf der Karte gezeichnet", PreferenceKeys.LANEMAP, true)
        SToggle(prefs, "Mapbox-Spurdaten auf TomTom-Routen nutzen", PreferenceKeys.LANES_MAPBOX, true)
        SToggle(prefs, "Heranzoomen an Spuren vor einer Kreuzung", PreferenceKeys.LANE_VIEW, true)
        SToggle(prefs, "Kreuzungs-Nahansicht", PreferenceKeys.JUNCTION_VIEW, true, "Nähere, flachere Ansicht bei Ausfahrten und Gabelungen")
        SToggle(prefs, "Karte beim Abbiegen auf 2D flachlegen", PreferenceKeys.TURN2D, true)
        SToggle(prefs, "Neigung nach Geschwindigkeit", PreferenceKeys.SPEEDTILT, true, "Flacher bei langsamer Fahrt, weitblickender bei Tempo")
        TiltSetting(prefs, "3D-Neigung, breiter Bildschirm", PreferenceKeys.TILT_WIDE)
        TiltSetting(prefs, "3D-Neigung, hoher Bildschirm", PreferenceKeys.TILT_TALL)
    }
    SGroup("Fahrtenkarte") {
        SToggle(prefs, "Routenleiste auf der Fahrtenkarte", PreferenceKeys.ROUTEBAR, true, "Verkehr, Kameras, Raststätten und Fortschritt")
        SToggle(prefs, "Nächste Raststätten-Karte", PreferenceKeys.SERVICESCARD, true)
        SToggle(prefs, "Gefahrenradar", PreferenceKeys.HAZARDRADAR, false, "Kurven, Schulen, Schwellen, Blitzer (2 km)")
    }
    SGroup("Ankunft") {
        SToggle(prefs, "Ankunftsmoment", PreferenceKeys.ARRIVALFX, true, "Kamera schwenkt zum Ziel und zeigt Parkplätze")
        SToggle(prefs, "Parkplätze nahe dem Ziel", PreferenceKeys.PARKPROMPT, false, "Parkplatzvorschläge vor der Ankunft")
        SToggle(prefs, "Ziel beleuchten", PreferenceKeys.LIGHT_DEST, true, "Das Gebäude bleibt bei der Ankunft beleuchtet, der Eingang bekommt eine Markierung, eine gepunktete Linie zeigt den Fußweg vom Parkplatz")
        SToggle(prefs, "Rückroute", PreferenceKeys.ROUTEBACK, true, "Ein Tipp für den Weg zurück zum Startort")
        SToggle(prefs, "Parkplatz über Bluetooth merken", PreferenceKeys.PARK_BLUETOOTH, true, "Speichert den Standort beim Trennen vom Auto-Bluetooth")
    }
}


/** Checks the Open Charge Map key, and shows what the last charger lookup found. */
@Composable
internal fun OcmTestItem(prefs: Prefs) {
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
        SToggle(prefs, "Warnen bei Überschreitung", PreferenceKeys.SPEEDWARN, false)
        if (prefs.b(PreferenceKeys.SPEEDWARN, false)) SChoice("Warnen ab Überschreitung um", listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        SToggle(prefs, "Limitänderung ansagen", PreferenceKeys.LIMITCHANGE, false)
        if (prefs.b(PreferenceKeys.LIMITCHANGE, false)) SChoice("Wie", listOf("both" to "Gong und Stimme", "voice" to "Nur Stimme", "chime" to "Nur Gong"), prefs.s(PreferenceKeys.LIMIT_HOW, "both"),
            "Mit Gong kommt die Stimme erst, wenn der Gong vorbei ist") { prefs.put(PreferenceKeys.LIMIT_HOW, it) }
        SToggle(prefs, "Geschätzte Limits anzeigen", PreferenceKeys.LIMITEST, false, "Übliches Limit anzeigen, wo keins kartiert ist")
        // where you are bans camera warnings and "Off where banned" is on: say so here, or it looks as if the switch doesn't work
        val banned = com.headunit.launcher.CAMERA_BANNED[engine.detectedCountry]
        val camOff = banned != null && prefs.b(PreferenceKeys.CAMERA_WARNINGS, false) && prefs.b(PreferenceKeys.CAMBANOBEY, true)
        SToggle(prefs, "Blitzer", PreferenceKeys.CAMERA_WARNINGS, false, if (camOff) Lang.t("Aus: in $banned nicht erlaubt", "Off: not allowed in $banned")
            else if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) engine.camNote.ifBlank { "Suche startet mit dem nächsten GPS-Signal" } else null)
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) SToggle(prefs, "Aus, wo verboten", PreferenceKeys.CAMBANOBEY, true, "Automatisch aus z. B. in DE, FR und CH")
        SToggle(prefs, "Blitzer auf der Karte zeigen", PreferenceKeys.CAMERA_MAP, true, "Nur Anzeige; Warnungen dort nur mit „Blitzer“ an")
        SToggle(prefs, "Parkhäuser auf der Karte", PreferenceKeys.GARAGES_MAP, true, "Freie Plätze live, wo verfügbar. Daten: MobiData BW, ParkenDD")
        SToggle(prefs, "Sektionskontrollen (Abschnittskontrolle)", PreferenceKeys.AVGZONES, true, "Durchschnittstempo im Vergleich zum Limit")
    }
    SGroup("Unterwegs", footer = "Informationen stammen aus OpenStreetMap.") {
        SToggle(prefs, "Baustellen und Ereignisse", PreferenceKeys.INCIDENTS, true, "Baustellen und Sperrungen auf Autobahnen. Daten: Autobahn GmbH. In Spanien: Unfälle, Baustellen, Sperrungen, Gefahren und V16-Pannenbaken der DGT (nap.dgt.es)")
        SToggle(prefs, "Einbahnstraßen-Warnung", PreferenceKeys.WRONGWAY, false, "Bei falscher Fahrtrichtung; Fehlalarme möglich")
        SToggle(prefs, "Bremsschwellen", PreferenceKeys.BUMPS, false)
        SToggle(prefs, "Schulen zu Schulzeiten", PreferenceKeys.SCHOOLS, false)
        SToggle(prefs, "Umweltzonen", PreferenceKeys.ZONEWARN, true)
        SToggle(prefs, "Pausenerinnerung nach 2 Stunden", PreferenceKeys.BREAKREMIND, false)
        SToggle(prefs, "Bahnübergänge und Furten", PreferenceKeys.RAILFORD, false, "Warnschild und Sprachwarnung")
        SToggle(prefs, "Scharfe Kurven", PreferenceKeys.SHARPBENDS, false, "Gelb gestreift auf der Route, Warnschild und Gong kurz davor (nur wenn du zu schnell bist)")
        SToggle(prefs, "Wetterhinweise", PreferenceKeys.WXADVICE, true, "Hinweis bei Regen, Schnee, Nebel oder Glätte")
        SToggle(prefs, "Unwetterwarnungen (DWD)", PreferenceKeys.DWD_WARN, true, "Amtliche Warnungen auf der Strecke: DWD in Deutschland, Meteoalarm in Frankreich, Österreich, Italien, Spanien, den Niederlanden und weiteren Ländern mit Warngebieten")
        SToggle(prefs, "Feiertagshinweis", PreferenceKeys.HOLIDAY_HINT, true, "Beim Start einer Route ein Hinweis, wenn heute oder morgen ein Feiertag ist (mehr Verkehr). Daten: date.nager.at")
        SToggle(prefs, "Falschfahrer-Meldungen", PreferenceKeys.WRONGWAY_REPORTS, true, "Alarm bei Meldung bis 30 km voraus")
        SToggle(prefs, "Vignetten-Hinweis", PreferenceKeys.VIGNETTE_WARN, true, "Ansage ca. 30 km vor Vignetten- oder Mautland")
        SToggle(prefs, "Rettungsgasse", PreferenceKeys.RESCUE_LANE, true, "Hinweis im Autobahnstau")
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
        SToggle(prefs, "Landmarken in Anweisungen", PreferenceKeys.LANDMARKS, true, "Abbiegen an Ampeln: „An der Ampel rechts abbiegen“")
        SToggle(prefs, "Intelligente Anweisungen", PreferenceKeys.SMARTVOICE, true, "Anweisungen mit Ausfahrtsnummern und Schildern")
        SChoice("Sprachkanal", listOf("nav" to "Navigation", "media" to "Medien"), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            "Medien wählen, falls keine Sprache zu hören ist") { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        SSlider("Gong-Lautstärke", prefs.i(PreferenceKeys.CHIME_VOL, 100).toFloat(), 0f..100f, "%", sub = "Gong bei Tempo, Tempolimit und Blitzern, zur Stimme eingestellt", steps = 19, reset = 100f) { prefs.put(PreferenceKeys.CHIME_VOL, it) }
        SButton("Gong anhören") { engine.chimeSample() }
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


// ------------------------------------------------------------------ Screen & sizes

@Composable
private fun ScreenSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Bildschirm") {
        SSlider("Helligkeit", prefs.i(PreferenceKeys.BRIGHT, 100).toFloat(), 10f..100f, "%", sub = "100% überlässt die Steuerung dem System", live = true) { prefs.put(PreferenceKeys.BRIGHT, it) }
        SToggle(prefs, "Bildschirm nachts dimmen", PreferenceKeys.DIMNIGHT, false)
        SToggle(prefs, "Bildschirm eingeschaltet lassen", PreferenceKeys.KEEPON, true)
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
            SSlider("Straßennamen-Größe", prefs.i(PreferenceKeys.ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", sub = "Name der aktuellen Straße unter dem Pfeil", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.ROAD_NAME_SIZE, it) }
        }

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

/** Beim Fahren: its own page (road name, arrow, tunnels, media controls, locking). */
@Composable
private fun WhileDrivingSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Song und Cover", footer = "Zurück, Pause und Weiter unten auf dem Fahrbildschirm, solange auf dem Autodisplay Musik oder Radio läuft, aus jeder App. Mit Benachrichtigungszugriff stehen auch Titel, Interpret und Cover dabei.") {
        val ctx = LocalContext.current
        val access = com.headunit.launcher.MediaHub.hasAccess(ctx)
        SToggle(prefs, "Mediensteuerung", PreferenceKeys.MEDIA_CONTROLS, false, "Die Leiste erscheint nur, wenn etwas läuft")
        if (com.headunit.launcher.BuildConfig.MEDIA_ACCESS) SItem("Titel und Cover", if (access) "Zugriff erteilt" else "Noch kein Zugriff: Titel und Cover fehlen") {
            SButton(if (access) "Ändern" else "Zugriff erlauben", primary = !access) {
                ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
    SGroup("Beim Fahren") {
        SToggle(prefs, "Himmelsrichtung hinter dem Straßennamen", PreferenceKeys.HEADING_WORD, true, "„Richtung E“ hinter der Straße, wenn du ohne Route fährst. Aus: nur der Straßenname")
        SToggle(prefs, "Geschwindigkeit neben dem Limit", PreferenceKeys.MAPSPEED, true)
        SToggle(prefs, "Tachoring", PreferenceKeys.SPEEDRING, true, "Füllt sich bis zum Tempolimit, färbt sich gelb dann rot")
        SSlider("Tempolimit- und Tachogröße", prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = "Auf Smartphone und Autodisplay", steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SToggle(prefs, "Sonnenzeiten auf Android Auto", PreferenceKeys.CARSUN, false, "Tag und Nacht auf dem Autodisplay folgen Sonnenauf- und -untergang, auch wenn das Auto bei Licht dunkel wird")
        SToggle(prefs, "Großes Limit-Schild", PreferenceKeys.SPEED_SIGN_BIG, false, "Das Tempolimit-Schild groß an der Stelle des Tachos, dein Tempo klein daneben")
        SChoice("Tacho-Seite", listOf("right" to "Rechts", "left" to "Links", "auto" to "Automatisch"), prefs.s(PreferenceKeys.MAIN_SPEED_SIDE, "right"),
            "Auf Smartphone, Head-Unit und Android Auto (untere Ecke). Automatisch: wie früher. Das Limit-Schild steht immer zur Bildschirmmitte hin") { prefs.put(PreferenceKeys.MAIN_SPEED_SIDE, it) }
        SToggle(prefs, "Straße und Richtung beim Fahren ohne Route", PreferenceKeys.CRUISE, true)
        SToggle(prefs, "Pfeil ohne Route auf der Straße halten", PreferenceKeys.SNAPROAD, true)
        SToggle(prefs, "Pfeil seitlich lassen", PreferenceKeys.KEEP_ARROW_SIDE, false, "Auf breitem Bildschirm bleibt der Pfeil seitlich, wenn die Karten einklappen, statt in die Mitte zu gleiten")
        SToggle(prefs, "Karte in Tunnels dimmen", PreferenceKeys.TUNNELDIM, true)
        SToggle(prefs, "Uhr auf der Karte", PreferenceKeys.CLOCK_ON_MAP, false, "Eine kleine Uhr unter dem Tacho, wenn die Statusleiste des Geräts verdeckt ist")
        SToggle(prefs, "Weniger Buttons beim Fahren", PreferenceKeys.DRIVESIMPLE, true)
        SToggle(prefs, "Tastatur beim Fahren sperren", PreferenceKeys.DRIVELOCK, false, "Sprachsuche funktioniert weiterhin")
    }
}

@Composable
private fun AndroidAutoSection(prefs: Prefs) {
    val ctx = LocalContext.current
    SGroup("Auf der Karte") {
        SToggle(prefs, "Sound-Knopf auf der Karte, wenn nicht navigiert", PreferenceKeys.CAR_SOUND_IDLE, true, "Aus: der Sound-Knopf erscheint nur während der Navigation, für mehr Platz")
        SToggle(prefs, "Suchleiste und Orte auf der Karte ausblenden", PreferenceKeys.CAR_HIDE_SEARCH, false, "Für Autos mit Drehknopf ohne Touchscreen: Suchleiste, Zuhause, Arbeit, Schnellwahl und Vorschlag verschwinden, wenn du nicht navigierst. Suche, Zuhause, Arbeit und gespeicherte Orte bleiben unter Orte (★)")
    }
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
        SChoice("Tachostil auf dem Autodisplay", listOf("normal" to "Normal", "compact" to "Kompakt"), prefs.s(PreferenceKeys.CAR_SPEED_STYLE, "normal"),
            "Kompakt: eine kleine dunkle Pille mit Limit-Schild und Tempo nebeneinander, passt auch oben rechts auf einen kurzen Bildschirm") { prefs.put(PreferenceKeys.CAR_SPEED_STYLE, it) }
        SChoiceWith("Position des Tachos auf dem Autodisplay", listOf("bottom" to "Untere Ecke", "turn" to "Unter der Abbiege-Karte", "top" to "Oben rechts"), prefs.s(PreferenceKeys.CAR_SPEED_POS, "bottom"),
            "Untere Ecke: die Seite aus „Tacho-Seite“ (Beim Fahren)", true) { prefs.put(PreferenceKeys.CAR_SPEED_POS, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Erweiterte Autodisplay-Größen", "Einzelne Elemente unabhängig anpassen") {
            SButton(if (carAdvanced) "Ausblenden" else "Anzeigen") { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider("Karten-Größe", prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider("Button-Größe", prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider("Textgröße", prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider("Navigationspfeil-Größe", prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 70).toFloat(), 30f..180f, "%", reset = 70f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider("Straßennamen-Größe", prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
        SGroup("Knöpfe auf der Fahrtkarte") {
            SToggle(prefs, "Suche", PreferenceKeys.TRIP_BTN_SEARCH, true, "Such-Taste auf der Fahrtkarte: ein gewählter Ort wird als Stopp oder als Ziel gefragt")
            SToggle(prefs, "Übersicht", PreferenceKeys.TRIP_BTN_OVERVIEW, true, "Die ganze Route auf einen Blick")
            SToggle(prefs, "Ansagen", PreferenceKeys.TRIP_BTN_OTHERS, true, "Alle Ansagen, nur Warnungen oder aus")
            SToggle(prefs, "Beenden", PreferenceKeys.TRIP_BTN_END, true, "Die Fahrt beenden")
            SChoice("Knopfgröße", listOf("normal" to "Normal", "large" to "Groß", "xl" to "Extra groß"), prefs.s(PreferenceKeys.TRIP_BTN_SIZE, "normal"),
                "Weniger Knöpfe machen die Karte schmaler, so bleibt mehr von der Karte frei") { prefs.put(PreferenceKeys.TRIP_BTN_SIZE, it) }
        }
        SToggle(prefs, "Pfeil seitlich lassen", PreferenceKeys.KEEP_ARROW_SIDE, false, "Auf breitem Bildschirm bleibt der Pfeil seitlich, wenn die Karten einklappen, statt in die Mitte zu gleiten")
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
        SToggle(prefs, "Schneller Kartenaufbau", PreferenceKeys.MAPFAST, true)
        SToggle(prefs, "Leistungsanzeige", PreferenceKeys.PERFORMANCE_METER, false, "Anzeige von FPS und Renderschritten")
    }
    SGroup("Demo") {
        SToggle(prefs, "Demofahrt-Button", PreferenceKeys.DEMO, false, "Spielt eine Route ab zum Testen beim Parken")
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
    SItem("Luftbild-Speicher", (if (mb < 0) "…" else "$mb MB von 500 MB") + progress) {
        SButton("Leeren", danger = true) { com.headunit.launcher.SatTileCache.clear(ctx); mb = 0 }
    }
}
