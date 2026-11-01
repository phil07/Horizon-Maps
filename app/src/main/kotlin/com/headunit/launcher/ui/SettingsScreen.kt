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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.BuildConfig
import com.headunit.launcher.FuelFinder
import com.headunit.launcher.Geo
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
    Section("Dein Pfeil & Routenlinie", "Pfeil oder Auto, Farben, Routendesign", Icons.Rounded.Navigation, Color(0xFF4C8DFF)),
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
            val hay = (r.title + " " + r.sub + " " + r.group + " " + sections[r.section].title).lowercase()
            words.all { hay.contains(it) }
        }.sortedBy { if (it.title.lowercase().startsWith(words[0])) 0 else if (it.title.lowercase().contains(words[0])) 1 else 2 }.take(40)
    }
    Row(Modifier.fillMaxSize().background(c.bg).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        // the list of sections
        if (!narrowS || showList) Column(
            Modifier.then(if (narrowS) Modifier.fillMaxWidth() else Modifier.width(290.dp)).fillMaxHeight().clip(RoundedCornerShape(28.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(28.dp)).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
                        Text(r.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(sections[r.section].title + (if (r.group.isNotBlank()) " · " + r.group else ""), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else sections.forEachIndexed { i, s ->
                // the small group headings in the list: Map, Driving, You, Device
                if (s.heading != null) Text(s.heading, color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
                    modifier = Modifier.padding(start = 14.dp, top = if (i == 0) 2.dp else 12.dp, bottom = 2.dp))
                val on = i == cat
                val rowBg by androidx.compose.animation.animateColorAsState(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent, androidx.compose.animation.core.tween(200), label = "secBg")
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(rowBg).clickable { cat = i; highlight = null; showList = false }.padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(s.tint.copy(alpha = if (on) 0.26f else 0.16f)), contentAlignment = Alignment.Center) {
                        Icon(s.icon, null, tint = s.tint, modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(s.title, color = if (on) c.accent else c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(s.sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        // the chosen section (each opens at the top), sliding up gently into place
        if (!narrowS || !showList) androidx.compose.animation.AnimatedContent(
            targetState = cat,
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
                Text(sections[cat].title, color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
                Text(sections[cat].sub, color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 6.dp, bottom = 16.dp))
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
                        8 -> FuelSection(prefs)
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

// ------------------------------------------------------------------ Map look

@Composable
private fun MapLookSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Tag und Nacht") {
        SChoice("Tag und Nacht", listOf("auto" to "Auto", "lights" to "Scheinwerfer", "sun" to "Sonnenstand", "times" to "Feste Zeiten", "day" to "Tag", "night" to "Nacht"), prefs.s(PreferenceKeys.THEME, "auto"),
            "Auto folgt dem Nachtmodus des Geräts. Scheinwerfer schaltet mit dem Licht. Sonnenstand schaltet bei Sonnenauf- und -untergang. Feste Zeiten schaltet zu den festgelegten Uhrzeiten.") { prefs.put(PreferenceKeys.THEME, it) }
        if (prefs.s(PreferenceKeys.THEME, "auto") == "times") {
            SFieldItem("Nacht beginnt um", prefs.s(PreferenceKeys.NIGHT_FROM, "19:00"), "24-Stunden-Format, z. B. 19:30", placeholder = "19:00") { prefs.put(PreferenceKeys.NIGHT_FROM, it.trim()) }
            SFieldItem("Tag beginnt um", prefs.s(PreferenceKeys.DAY_FROM, "07:00"), "24-Stunden-Format, z. B. 06:45", placeholder = "07:00") { prefs.put(PreferenceKeys.DAY_FROM, it.trim()) }
        }
    }
    SGroup("Karte") {
        SChoice("Kartenquelle", listOf("mapbox" to "Mapbox", "tomtom" to "TomTom", "osm" to "OpenStreetMap"), prefs.mapSource(),
            "Mapbox: 3D-Karte. TomTom: TomTom-Karte im Horizon-Design. Benötigt das Mapbox-Token.") { prefs.put(PreferenceKeys.MAP_STYLE, it) }
        if (prefs.mapSource() == "mapbox") {
            SChoice("Kartendetails", listOf("full" to "Vollständiges 3D", "lite" to "Lite", "lightest" to "Einfachst"), prefs.mapDetail(),
                "Vollständiges 3D: Gebäude, Wahrzeichen, Bäume und Schatten. Lite: Flache Gebäude. Einfachst: 2D-Karte.") { prefs.put(PreferenceKeys.MAP_DETAIL, it) }
            MapColoursPicker(prefs.s(PreferenceKeys.MAP_COLOURS, "mapbox")) { prefs.put(PreferenceKeys.MAP_COLOURS, it) }
        }
        DefaultZoomRow(prefs)
        SToggle(prefs, "Fußwege und Pfade", "paths", true, "Ausschalten für eine übersichtlichere Fahrkarte")
        SToggle(prefs, "Glaskarten", "glass", true, "Transparente, leicht getönte Karten. Aus zeigt feste Karten")
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
        SToggle(prefs, "Nachtsternenhimmel in 3D", "nightsky", true, "Funkelnde Sterne über dem Horizont nach Einbruch der Dunkelheit")
        SToggle(prefs, "Bäume in Parks", "trees", true, "3D-Bäume in Parks und an Straßen")
        SToggle(prefs, "Freie Sicht auf die Straße voraus", "clearview", true, "Bäume und Wahrzeichen verbergen nicht die Straße direkt vor dir")
        SToggle(prefs, "Tippen zum Hervorheben", "tapselect", true, "Tippe auf ein Gebäude oder einen Ort zum Auswählen")
    }
}

// ------------------------------------------------------------------ On the map

@Composable
private fun OnTheMapSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Verkehr") {
        SToggle(prefs, "Live-Verkehr", "traffic", true)
        SToggle(prefs, "Verkehrsübersicht beim Halt", PreferenceKeys.STOPPED_OVERVIEW, true, "Nach 30 Sekunden Stillstand herauszoomen zur Verkehrsübersicht")
        SToggle(prefs, "Verkehr nur auf der Route anzeigen", "routetrafficonly", false, "Aus zeigt auch Verkehr auf umliegenden Straßen")
        SToggle(prefs, "Grüner Verkehr bei freier Fahrt", PreferenceKeys.TRAFFIC_GREEN, false, "Frei fließende Hauptstraßen grün anzeigen")
    }
    SGroup("Auf deiner Route") {
        SToggle(prefs, "Ampeln", "signals", true, if (prefs.b(PreferenceKeys.SIGNALS, true)) "Nur Ampeln auf deiner Route. Aktuell: " + engine.map.signalInfo() else null)
        SToggle(prefs, "Blitzer und Baustellen", "hazardicons", true)
        SToggle(prefs, "Zebrastreifen", "zebras", true, "Zebrastreifen auf deiner Route auf der Straße gezeichnet")
        SToggle(prefs, "Stoppschilder", PreferenceKeys.STOP_SIGNS, true, "Stoppschilder auf deiner Route mit kurzem Ton")
    }
    SGroup("3D-Straßenausstattung") {
        SToggle(prefs, "3D-Straßenlaternen, Ampeln und Kameras", "furniture3d", true, "3D-Modelle in echter Größe")
        SToggle(prefs, "Straßenlaternen", "lamps", false, if (prefs.b(PreferenceKeys.LAMPS, false)) "Aktuell: " + engine.map.lampInfo() else null)
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
    SGroup("Dein Pfeil", footer = "Echtes 3D-Modell: Wird in die Karte gezeichnet. Der flache Pfeil ist am leichtesten für langsamere Geräte.") {
        val loc = prefs.s(PreferenceKeys.LOCATOR, "arrow")
        val ctx = LocalContext.current
        val hasPic = prefs.s(PreferenceKeys.LOCATOR_IMAGE, "").isNotBlank() && java.io.File(ctx.filesDir, "locator.png").exists()
        val pickPic = rememberLocatorImagePicker { ok ->
            if (ok) { prefs.put(PreferenceKeys.LOCATOR_IMAGE, System.currentTimeMillis().toString()); prefs.put(PreferenceKeys.LOCATOR, "custom") }
            else android.widget.Toast.makeText(ctx, "Bild konnte nicht gelesen werden (versuche PNG oder JPG)", android.widget.Toast.LENGTH_SHORT).show()
        }
        fun choosePic() { if (!pickPic()) android.widget.Toast.makeText(ctx, "Keine App auf diesem Gerät kann ein Bild auswählen", android.widget.Toast.LENGTH_LONG).show() }
        SChoice("Positionsanzeige", listOf("arrow" to "3D-Pfeil", "car" to "Auto", "model" to "Echtes 3D-Modell", "flat" to "Flacher Pfeil", "custom" to "Eigenes Bild"), loc) {
            if (it == "custom" && !hasPic) choosePic() else prefs.put(PreferenceKeys.LOCATOR, it)
        }
        if (loc == "model") {
            SChoice("Modellform", listOf("arrow" to "Pfeil", "car" to "Auto"), prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow")) { prefs.put(PreferenceKeys.MODEL_SHAPE, it) }
            SToggle(prefs, "Leuchten", PreferenceKeys.MODEL_GLOW, true, "Weiches blaues Leuchten um das Modell")
            if (prefs.mapDetail() != "full") SNote("Das echte 3D-Modell wird bei Kartendetails: Vollständiges 3D angezeigt.")
        }
        val shapeCar = loc == "car" || (loc == "model" && prefs.s(PreferenceKeys.MODEL_SHAPE, "arrow") == "car")
        val shapeArrow = loc == "arrow" || (loc == "model" && !shapeCar)
        if (loc == "custom") {
            SItem("Bild", "Ein PNG mit transparentem Hintergrund sieht am besten aus.") { SButton(if (hasPic) "Ändern" else "Wählen") { choosePic() } }
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
    SGroup("Vermeiden", footer = "Änderungen gelten für die nächste geplante Route.") {
        SToggle(prefs, "Mautstraßen vermeiden", "avoid_toll", false, if (prefs.b(PreferenceKeys.AVOID_TOLL, false)) "Überall" else null)
        if (!prefs.b(PreferenceKeys.AVOID_TOLL, false)) {
            val chosen = prefs.s(PreferenceKeys.AVOID_TOLL_COUNTRIES, "").split(',').filter { it.isNotBlank() }.toSet()
            SItem("Maut vermeiden nur in", if (chosen.isEmpty()) "Länder wählen: Routen mit Mautstraßen in diesen Ländern werden ohne Maut geplant" else "Routen mit Maut in diesen Ländern werden ohne Maut geplant")
            androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((cc, name) in listOf("FR" to "Frankreich", "IT" to "Italien", "ES" to "Spanien", "PT" to "Portugal", "AT" to "Österreich", "CH" to "Schweiz",
                    "SI" to "Slowenien", "HR" to "Kroatien", "PL" to "Polen", "CZ" to "Tschechien", "HU" to "Ungarn", "GR" to "Griechenland", "IE" to "Irland", "NO" to "Norwegen", "US" to "USA")) {
                    Pill(name, cc in chosen, { prefs.put(PreferenceKeys.AVOID_TOLL_COUNTRIES, (if (cc in chosen) chosen - cc else chosen + cc).joinToString(",")) })
                }
            }
        }
        SToggle(prefs, "Autobahnen vermeiden", "avoid_motorway", false)
        SToggle(prefs, "Fähren vermeiden", "avoid_ferry", false)
        SToggle(prefs, "Unbefestigte Straßen vermeiden", "avoid_unpaved", true)
        SToggle(prefs, "Umweltzonen vermeiden", "avoidzones", false, "Eine Start- oder Zielzone kann nicht vermieden werden")
        ZonesItem()
    }
    SGroup("Routenauswahl") {
        SToggle(prefs, "Öko-Routen", "route_eco", false, "Kraftstoffsparend statt am schnellsten")
        SToggle(prefs, "Schnellere Routen anbieten", "fasterroute", true)
        if (prefs.b(PreferenceKeys.FASTER_ROUTE, true)) SToggle(prefs, "Andere Routen auf der Karte anzeigen", "livealts", true, "Graue Linien mit +/- Minuten zum Antippen")
        SToggle(prefs, "Ziele vorschlagen", "predict", true, "Mit Live-Fahrzeit für Zuhause und Arbeitsplatz")
        SToggle(prefs, "Wetter auf der Route", "routewx", true)
    }
    SGroup("Routenstart") {
        SToggle(prefs, "Auto-Start-Countdown", PreferenceKeys.AUTO_START_ROUTE, true, "Startet die Navigation automatisch nach Ablauf des Countdowns")
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
    }
    SGroup("Fahrtenkarte") {
        SToggle(prefs, "Routenleiste auf der Fahrtenkarte", "routebar", true, "Verkehr, Kameras, Raststätten und Fortschritt")
        SToggle(prefs, "Nächste Raststätten-Karte", "servicescard", true)
        SToggle(prefs, "Gefahrenradar", "hazardradar", false, "Nächste 2 Kilometer: Kurven, Schulen, Schwellen, Blitzer")
    }
    SGroup("Ankunft") {
        SToggle(prefs, "Ankunftsmoment", "arrivalfx", true, "Kamera schwenkt zum Ziel und zeigt Parkplätze")
        SToggle(prefs, "Parkplätze nahe dem Ziel", "parkprompt", true, "Parkplatzvorschläge vor der Ankunft")
        SToggle(prefs, "Rückroute", "routeback", true, "Ein Tipp für den Weg zurück zum Startort")
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
        SToggle(prefs, "Warnen bei Überschreitung", "speedwarn", true)
        if (prefs.b(PreferenceKeys.SPEEDWARN, true)) SChoice("Warnen ab Überschreitung um", listOf("0", "5", "10").map { it to it + if (engine.imperial) " mph" else " km/h" }, prefs.s(PreferenceKeys.TRAFFIC_WARNING, "5")) { prefs.put(PreferenceKeys.TRAFFIC_WARNING, it) }
        SToggle(prefs, "Limitänderung ansagen", "limitchange", true)
        SToggle(prefs, "Geschätzte Limits anzeigen", "limitest", false, "Übliches Limit anzeigen, wo keins kartiert ist")
        SToggle(prefs, "Blitzer", "cameras", false)
        if (prefs.b(PreferenceKeys.CAMERA_WARNINGS, false)) SToggle(prefs, "Aus, wo verboten", "cambanobey", true, "Schaltet sich in Ländern wie Deutschland, Frankreich und der Schweiz automatisch aus")
        SToggle(prefs, "Sektionskontrollen (Abschnittskontrolle)", "avgzones", true, "Zeigt dein Durchschnittstempo im Vergleich zum Limit")
    }
    SGroup("Unterwegs", footer = "Informationen stammen aus OpenStreetMap.") {
        SToggle(prefs, "Baustellen und Ereignisse", "incidents", true)
        SToggle(prefs, "Geisterfahrer-Warnung", "wrongway", true, "Warnung beim falschen Einbiegen in Einbahnstraßen")
        SToggle(prefs, "Bremsschwellen", "bumps", true)
        SToggle(prefs, "Schulen zu Schulzeiten", "schools", true)
        SToggle(prefs, "Umweltzonen", "zonewarn", true)
        SToggle(prefs, "Pausenerinnerung nach 2 Stunden", "breakremind", true)
        SToggle(prefs, "Bahnübergänge und Furten", "railford", true, "Warnschild und Sprachwarnung")
        SToggle(prefs, "Wetterhinweise", "wxadvice", true, "Hinweis bei Regen, Schnee, Nebel oder Glätte")
    }
    SGroup("Deine Meldungen", footer = "Meldungen über die Optionen-Taste auf der Karte erstellen.") {
        SToggle(prefs, "Vor Meldungen voraus warnen", "policealerts", true)
        SItem("Aktive Meldungen", "${engine.reportsActive().size}") { SButton("Löschen", danger = true) { engine.clearReports() } }
    }
}

// ------------------------------------------------------------------ Sound & voice

@Composable
private fun SoundSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    SGroup("Ton & Stimme") {
        SChoice("Sprachführung", listOf("0" to "Alle", "1" to "Nur Warnungen", "2" to "Aus"), engine.soundMode.toString(),
            "Nur Warnungen überspringt Abbiegehinweise, sagt aber Kameras, Limits und Gefahren an") { prefs.put(PreferenceKeys.SOUND_MODE, it.toInt()) }
        SToggle(prefs, "Straßennamen ansagen", PreferenceKeys.VOICE_ROADS, true, "Aus gibt nur die Richtung an")
        SToggle(prefs, "Intelligente Anweisungen", "smartvoice", true, "Anweisungen mit Ausfahrtsnummern und Schildern")
        SChoice("Sprachkanal", listOf("nav" to "Navigation", "media" to "Medien"), prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav"),
            "Falls du Töne hörst, aber keine Sprache, wähle Medien") { prefs.put(PreferenceKeys.VOICE_CHANNEL, it) }
        SItem("Stimme und Sprache", "Nutzt die System-Sprachausgabe") { SButton("Öffnen") { openSystemSettings(ctx, "com.android.settings.TTS_SETTINGS") } }
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
    SGroup("Gespeicherte Orte", footer = "Zuhause und Arbeitsplatz können auch über die Buttons unter der Suchleiste festgelegt werden.") {
        PlaceItem("Zuhause", remember(slotsRaw) { pl.slot("home") }, { settingSlot = "home" }) { pl.clearSlot("home") }
        PlaceItem("Arbeitsplatz", remember(slotsRaw) { pl.slot("work") }, { settingSlot = "work" }) { pl.clearSlot("work") }
    }
    settingSlot?.let { s -> AddPlaceDialog(engine, null, slot = s) { settingSlot = null } }
    val raw = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val mine = remember(raw) { pl.customs() }
    var renaming by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    SGroup("Deine Orte", footer = "Eigene benannte Orte unter der Suchleiste.") {
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
    SGroup("Favoriten") {
        if (favs.isEmpty()) SNote("Noch keine Favoriten vorhanden.")
        favs.forEach { f ->
            when {
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
                else -> SItem(f.name, f.sub) {
                    SButton("Umbenennen") { renamingFav = f }
                    SButton("Zuhause / Arbeit") { slotFor = f }
                    SButton("Entfernen", danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        if (favs.size > 1) SItem("Alle Favoriten entfernen") { SButton("Alle löschen", danger = true) { pl.clearFavourites() } }
    }
    SGroup("Verlauf", footer = "Wische einen Ort zur Seite, um ihn aus der Liste zu entfernen.") {
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
        if (shown.isEmpty()) SNote("Noch keine Fahrten aufgezeichnet.")
        else SItem("${shown.size} Fahrten", Geo.fmtDist(shown.sumOf { it.dist }, engine.imperial) + " insgesamt") { SButton("Löschen", danger = true) { engine.trips.clear(); shown = emptyList() } }
        shown.take(50).forEach { t ->
            val mins = ((t.end - t.start) / 60000L).toInt()
            SItem(
                fmt.format(java.util.Date(t.start)) + (if (t.to.isNotBlank()) " → ${t.to}" else ""),
                "${Geo.fmtDur(mins * 60.0)} · in Fahrt ${Geo.fmtDur(t.moving.toDouble())} · max ${Math.round(t.maxSpeed * k)} $unit",
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
        SItem("Standard-Zoom", "Wenn du nicht auf einer Route bist. Zurücksetzen oder auf der Karte mit + und - zoomen.") {
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
    SGroup("Orte vom Smartphone senden", footer = "Einmalig koppeln: Auf dem Autodisplay 'Smartphone koppeln' tippen und Code eingeben.") {
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
            code.isNotBlank() && role == "phone" -> SItem("Dies ist dein Smartphone", "Mit deinem Auto gekoppelt (${com.headunit.launcher.CarLink.pretty(code)})") {
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
    SGroup("Ankunftszeit teilen", footer = "Teilt deine Ankunftszeit und einen Kartenlink.") {
        val pickEta = rememberEtaContactPicker(prefs)
        SItem("Senden an", prefs.s(PreferenceKeys.ETA_NAME, "").ifBlank { "Noch niemand ausgewählt" }) {
            SButton(if (prefs.s(PreferenceKeys.ETA_NUMBER, "").isBlank()) "Wählen" else "Ändern") { pickEta() }
            if (prefs.s(PreferenceKeys.ETA_NUMBER, "").isNotBlank()) SButton("Löschen", danger = true) { prefs.put(PreferenceKeys.ETA_NUMBER, ""); prefs.put(PreferenceKeys.ETA_NAME, "") }
        }
    }
    SGroup("Orte aus anderen Apps", footer = "Teile einen Ort aus Google Maps, WhatsApp oder Kontakte an Horizon Nav.") {
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
private fun FuelSection(prefs: Prefs) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    SGroup("Kraftstoffpreise") {
        SChoice("Dein Kraftstoff", listOf("E10" to "Super E10", "E5" to "Super E5", "B7" to "Diesel", "E85" to "Bioethanol (E85)", "EV" to "Elektro"), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
        SToggle(prefs, "Tankstopp bei langen Fahrten vorschlagen", "smartfuel", true, if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") "Über ca. 150 km eine Ladestation entlang des Weges" else "Über ca. 150 km die günstigste Tankstelle entlang des Weges")
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
    SGroup("Kraftstoffpreise außerhalb Großbritanniens", footer = "Deutschland benötigt einen kostenlosen Schlüssel von Tankerkönig (creativecommons.tankerkoenig.de).") {
        SFieldItem("Tankerkönig-Schlüssel (Deutschland)", prefs.s(PreferenceKeys.TANKERKOENIG_KEY, ""), placeholder = "Schlüssel einfügen", password = true) { prefs.put(PreferenceKeys.TANKERKOENIG_KEY, it.trim()) }
    }
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SGroup("Dein Elektroauto") {
        SFieldItem("Effizienz (km pro kWh)", prefs.s(PreferenceKeys.EVMPK, ""), placeholder = "z. B. 5.5", number = true) { s -> prefs.put(PreferenceKeys.EVMPK, s.filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Nutzbare Batterie (kWh)", prefs.s(PreferenceKeys.EV_BATTERY_KWH, ""), "Für Akkuschätzung und Ladestopps (Standard: 60 kWh)", placeholder = "z. B. 64", number = true) { s -> prefs.put(PreferenceKeys.EV_BATTERY_KWH, s.filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Strompreis (Cent pro kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = "z. B. 38", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.filter { it.isDigit() || it == '.' }.take(6)) }
        EvWidgetItem(prefs)
    } else SGroup("Kraftstoffkosten auf der Route") {
        SFieldItem("Verbrauch (Liter pro 100 km)", prefs.s(PreferenceKeys.FUELMPG, ""), placeholder = "z. B. 6.5", number = true) { s -> prefs.put(PreferenceKeys.FUELMPG, s.filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Kraftstoffpreis (Cent pro Liter)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = "z. B. 175.9", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.filter { it.isDigit() || it == '.' }.take(6)) }
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
    SGroup("Ortsnamen", footer = "Städte und Dörfer in $where für die Suche ohne Signal.") {
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
            if (autoScale) "Nutzt Bildschirmgröße und -form (aktuell $autoPercent%)" else "Manuelle Text- und Buttongröße ist aktiviert"
        ) {
            Switch(checked = autoScale, onCheckedChange = { enabled ->
                if (!enabled) prefs.put(PreferenceKeys.UI_SCALE, autoPercent)
                prefs.put(PreferenceKeys.UI_AUTO_SCALE, enabled)
            })
        }
        SSlider(
            "Text- und Buttongröße",
            (if (autoScale) autoPercent else prefs.i(PreferenceKeys.UI_SCALE, autoPercent)).toFloat(),
            60f..140f, "%", sub = if (autoScale) "Schieberegler bewegen zum manuellen Anpassen" else "Manuell anpassen oder automatische Skalierung aktivieren", steps = 15, reset = 100f
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
        SToggle(prefs, "Deine Geschwindigkeit neben dem Limit", "mapspeed", true)
        SToggle(prefs, "Tachoring", "speedring", true, "Füllt sich bis zum Tempolimit, färbt sich gelb dann rot")
        SSlider("Tempolimit- und Tachogröße", prefs.i(PreferenceKeys.SPEED_SIZE, 100).toFloat(), 50f..200f, "%", sub = "Auf Smartphone und Autodisplay", steps = 29, reset = 100f) { prefs.put(PreferenceKeys.SPEED_SIZE, it) }
        SToggle(prefs, "Straße und Richtung beim Fahren ohne Route", "cruise", true)
        SToggle(prefs, "Pfeil ohne Route auf der Straße halten", "snaproad", true)
        SToggle(prefs, "Karte in Tunnels dimmen", "tunneldim", true)
        SToggle(prefs, "Weniger Buttons beim Fahren", "drivesimple", true)
        SToggle(prefs, "Tastatur beim Fahren sperren", "drivelock", true, "Sprachsuche funktioniert weiterhin")
    }
    SGroup("Einheiten") {
        SChoice("Entfernung und Tempo", listOf("km" to "Kilometer", "mi" to "Meilen"), if (engine.imperial) "mi" else "km") { prefs.put(PreferenceKeys.DISTANCE_UNIT, it) }
        SChoice("Temperatur", listOf("C" to "°C", "F" to "°F"), if (isImperial(prefs)) "F" else "C") { prefs.put(PreferenceKeys.TEMPERATURE_UNIT, it) }
        SChoice("Fahrseite", listOf("" to "Automatisch", "rhd" to "Rechtslenker (Linksverkehr)", "lhd" to "Linkslenker (Rechtsverkehr)"), prefs.s(PreferenceKeys.DRIVE, ""),
            when (prefs.s(PreferenceKeys.DRIVE, "")) {
                "rhd" -> "Fahrer rechts: Karten links, Buttons rechts"
                "lhd" -> "Fahrer links: Bildschirm gespiegelt, Karten rechts"
                else -> "Folgt dem Land, in dem du fährst"
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
        SSlider("Tacho- und Tempolimitgröße", prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 100).toFloat(), 40f..180f, "%", reset = 100f) { prefs.put(PreferenceKeys.CAR_SPEED_SIZE, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem("Erweiterte Autodisplay-Größen", "Einzelne Elemente unabhängig anpassen") {
            SButton(if (carAdvanced) "Ausblenden" else "Anzeigen") { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider("Karten-Größe", prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider("Button-Größe", prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider("Textgröße", prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider("Navigationspfeil-Größe", prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 100).toFloat(), 60f..180f, "%", reset = 100f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider("Straßennamen-Größe", prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
        SItem("Installiert durch", source)
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

@Composable
private fun KeysSection(prefs: Prefs, engine: NavEngine) {
    SGroup("Suche") {
        SChoice("Suchergebnisse", searchCountryOptions(engine), searchCountryValue(prefs)) { prefs.put(PreferenceKeys.COUNTRY, it) }
        if (engine.hasKey && engine.hasMapbox) SChoice("Suchen mit", listOf("auto" to "TomTom", "mapbox" to "Mapbox"), if (prefs.s(PreferenceKeys.SEARCHWITH, "auto") == "mapbox") "mapbox" else "auto",
            "TomTom-Suche ist am genauesten für Adressen und Hausnummern") { prefs.put(PreferenceKeys.SEARCHWITH, it) }
        else if (!engine.hasKey) SNote("Für die genauste Adresssuche unten einen kostenlosen TomTom-Schlüssel eintragen.")
    }
    SGroup("Kartenkonten", footer = "Die Suche findet auch deine gespeicherten Orte. Ohne Empfang kommen Routen und Suche aus dem Offline-Paket.") {
        SFieldItem("TomTom API-Schlüssel", prefs.s(PreferenceKeys.API_KEY, ""), "Kostenlos erhältlich auf developer.tomtom.com",
            placeholder = "Schlüssel einfügen") { prefs.setApiKey(it) }
        SFieldItem("Mapbox Zugangstoken", prefs.s(PreferenceKeys.MAPBOX_TOKEN, ""), "Öffentliches Token (beginnt mit pk.) einfügen",
            placeholder = "Token einfügen") { prefs.put(PreferenceKeys.MAPBOX_TOKEN, com.headunit.launcher.MapboxRest.clean(it)) }
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
        SItem("Version", BuildConfig.VERSION_NAME)
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
            android.widget.Toast.makeText(ctx, if (ok) "Sicherung gespeichert" else "Sicherung konnte nicht gespeichert werden", android.widget.Toast.LENGTH_SHORT).show()
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
    SGroup("Sicherung") {
        SItem("Einstellungen und gespeicherte Orte") {
            SButton("Sichern") { exporter.launch("horizon-nav-backup.json") }
            SButton("Wiederherstellen") { importer.launch(arrayOf("application/json", "text/plain", "*/*")) }
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
        id < 0 -> "Übernimmt den Ladestand aus dem Widget deiner Auto-App, statt ihn von Hand einzustellen. Im Auto kommt der Wert direkt vom Fahrzeug, wenn Android Auto ihn teilt."
        at > 0 -> (if (com.headunit.launcher.EvWidget.source(prefs) == "car") "Direkt vom Auto" else name) + " · ${prefs.s(PreferenceKeys.EV_NOW_PCT, "–")} %, gelesen um " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.GERMANY).format(java.util.Date(at))
        else -> "$name · Noch kein Prozentwert gefunden. Das Widget meldet sich, sobald es sich aktualisiert."
    }
    SItem("Ladestand aus Widget", sub) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (id >= 0) SButton("Entfernen", danger = true) { com.headunit.launcher.EvWidget.remove(ctx, prefs) }
            SButton(if (id >= 0) "Ändern" else "Widget wählen", primary = id < 0) { picking = true }
        }
    }
    if (picking) {
        val list = remember { com.headunit.launcher.EvWidget.providers(ctx) }
        androidx.compose.ui.window.Dialog(onDismissRequest = { picking = false }) {
            Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Widget mit dem Ladestand", color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Wähle das Widget deiner Auto-App. Horizon Nav liest daraus den Prozentwert, wenn es sich aktualisiert.", color = c.sub, fontSize = 14.sp)
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    if (list.isEmpty()) Text("Keine Widgets gefunden", color = c.sub, fontSize = 15.sp)
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
