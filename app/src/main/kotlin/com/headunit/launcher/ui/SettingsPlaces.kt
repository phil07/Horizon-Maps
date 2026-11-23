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

/** Home or Work set to where you are now: the address is looked up and shown, so it can be saved while parked. */
internal fun useHereFor(engine: NavEngine, slot: String) {
    val at = engine.fix?.pos ?: return
    engine.toast(Lang.t("Adresse wird gesucht …", "Looking up the address …"), 2000)
    kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
        val addr = withContext(Dispatchers.IO) { try { com.headunit.launcher.PoiSearch.addressAt(at) } catch (e: Exception) { null } }
        val name = addr?.substringBefore(", ")?.ifBlank { null } ?: Lang.t("Aktueller Ort", "Current spot")
        val sub = addr?.substringAfter(", ", "") ?: ""
        engine.places.setSlot(slot, Place(name, sub, at))
        engine.toast(Lang.t("Gespeichert: $name", "Saved: $name"), 2500)
    }
}

@Composable
internal fun PlaceItem(label: String, p: Place?, onChange: () -> Unit, onClear: () -> Unit, useHere: (() -> Unit)? = null) {
    SItem(label, p?.let { it.name + (if (it.sub.isNotBlank()) ", " + it.sub else "") } ?: Lang.t("Nicht festgelegt", "Not set")) {
        if (useHere != null) SButton("Hier verwenden", onClick = useHere)
        SButton(if (p != null) Lang.t("Ändern", "Change") else Lang.t("Festlegen", "Set"), primary = true, onClick = onChange)
        if (p != null) SButton("Löschen", danger = true, onClick = onClear)
    }
}

@Composable
internal fun PlacesSection(engine: NavEngine) {
    val pl = engine.places
    val raw = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val mine = remember(raw) { pl.customs() }
    var renaming by remember { mutableStateOf<String?>(null) }
    var folderFor by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    SGroup(Lang.t("Unter der Suchleiste", "Under the search bar"), footer = Lang.t("Die Knöpfe unter der Suchleiste: Verknüpfungen, Such-Knöpfe, die Suchleiste und Zuhause und Arbeit. Der Knopf zeigt eine Liste, den nächsten Ort oder Track zuerst.", "The buttons under the search bar: shortcuts, find buttons, the search bar, and Home and Work. A button shows a list, the nearest place or track first.")) {
        SToggle(engine.prefs, Lang.t("Meine Orte", "My places"), PreferenceKeys.SC_PLACES, false, Lang.t("Liste deiner Orte und Favoriten", "A list of your places and favourites"))
        SToggle(engine.prefs, Lang.t("Meine Tracks", "My tracks"), PreferenceKeys.SC_TRACKS, false, Lang.t("Liste deiner GPX-Tracks", "A list of your GPX tracks"))
        for ((kind, label) in listOf("fuel" to "Tanken", "charging" to "Laden", "food" to "Essen", "coffee" to "Kaffee", "parking" to "Parken",
            "camp" to "Stellplätze", "dump" to "Entsorgung", "lpg" to "Autogas")) {
            SToggle(engine.prefs, label, "find_$kind", true, "Knopf „$label in der Nähe“")
        }
        SToggle(engine.prefs, Lang.t("Suche als Knopf", "Search as a button"), PreferenceKeys.SEARCH_FOLD, true, Lang.t("Ohne Route nur ein runder Knopf in der Ecke. Aus: die Suchleiste mit ihren Knöpfen ist immer da", "With no route, just one round button in the corner. Off: the search bar with its buttons is always there"))
        SToggle(engine.prefs, Lang.t("Zuhause", "Home"), PreferenceKeys.HOME_TILE, true, Lang.t("Die Kachel Zuhause mit der Fahrtzeit", "The Home tile with the drive time"))
        SToggle(engine.prefs, Lang.t("Arbeit", "Work"), PreferenceKeys.WORK_TILE, true, Lang.t("Die Kachel Arbeit mit der Fahrtzeit", "The Work tile with the drive time"))
        SToggle(engine.prefs, Lang.t("Markierte Orte unter der Suche", "Starred places under the search"), PreferenceKeys.STARRED_ROW, true, Lang.t("Aus: alle markierten Orte sind ausgeblendet. Sie bleiben in der Suche und in „Meine Orte“.", "Off: all starred places are hidden. They stay in search and in My places."))
        if (mine.isEmpty()) SNote(Lang.t("Noch keine eigenen Orte vorhanden.", "No places of your own yet."))
        mine.forEach { lp ->
            if (renaming == lp.label) {
                var t by remember(lp.label) { mutableStateOf(lp.label) }
                SItem("${lp.label} umbenennen") {
                    SField(t, { t = it.take(24) }, width = 220.dp)
                    SButton(Lang.t("Speichern", "Save"), primary = true) { pl.renameCustom(lp.label, t); renaming = null }
                }
            } else if (folderFor == lp.label) {
                var f by remember(lp.label) { mutableStateOf(lp.folder) }
                SItem("Ordner für ${lp.label}", Lang.t("Einen neuen Namen eintippen oder einen vorhandenen. Leer: kein Ordner.", "Type a new name or an existing one. Empty: no folder.")) {
                    SField(f, { f = it.take(24) }, width = 220.dp)
                    SButton(Lang.t("Speichern", "Save"), primary = true) { pl.setFolder(lp.label, f); folderFor = null }
                }
            } else SItem(lp.label, (if (lp.folder.isNotBlank()) "Ordner: ${lp.folder} · " else "") + lp.place.name + if (lp.place.sub.isNotBlank()) ", " + lp.place.sub else "") {
                SButton(if (lp.starred) Lang.t("★ Markiert", "★ Starred") else Lang.t("☆ Markieren", "☆ Star"), primary = lp.starred) { pl.setStar(lp.label, !lp.starred) }
                SButton(Lang.t("Ordner", "Folder")) { folderFor = lp.label }
                SButton(Lang.t("Umbenennen", "Rename")) { renaming = lp.label }
                SButton(Lang.t("Entfernen", "Remove"), danger = true) { pl.removeCustom(lp.label) }
            }
        }
        SItem(Lang.t("Ort hinzufügen", "Add a place")) { SButton(Lang.t("Hinzufügen", "Add"), primary = true) { adding = true } }
        if (mine.size > 1) {
            SNote(Lang.t("Die Reihenfolge der Knöpfe unter der Suchleiste. Mit den Pfeilen verschieben.", "The order of the shortcuts under the search bar. Move them with the arrows."))
            mine.forEachIndexed { i, lp ->
                SItem(lp.label) {
                    SButton("↑") { if (i > 0) pl.moveCustom(lp.label, -1) }
                    SButton("↓") { if (i < mine.size - 1) pl.moveCustom(lp.label, 1) }
                }
            }
        }
    }
    GpxTracksGroup(engine)
    var settingSlot by remember { mutableStateOf<String?>(null) }
    val slotsRaw = engine.prefs.s(PreferenceKeys.PLACE_HOME, "") + engine.prefs.s("place_work", "")
    SGroup(Lang.t("Favoriten", "Favourites"), footer = "A–Z: alphabetisch. Nächste zuerst: die nächsten Favoriten oben. Gilt auch für Android Auto.") {
        SChoice("Favoriten sortieren", listOf("az" to "A–Z", "near" to "Nächste zuerst"), engine.prefs.s(PreferenceKeys.FAV_SORT, "az")) { engine.prefs.put(PreferenceKeys.FAV_SORT, it) }
    }
    SGroup(Lang.t("Gespeicherte Orte", "Saved places"), footer = Lang.t("Auch über die Buttons unter der Suchleiste festlegbar.", "Can also be set with the buttons under the search bar.")) {
        PlaceItem(Lang.t("Zuhause", "Home"), remember(slotsRaw) { pl.slot("home") }, { settingSlot = "home" }, { pl.clearSlot("home") }, { useHereFor(engine, "home") })
        PlaceItem(Lang.t("Arbeitsplatz", "Work"), remember(slotsRaw) { pl.slot("work") }, { settingSlot = "work" }, { pl.clearSlot("work") }, { useHereFor(engine, "work") })
    }
    settingSlot?.let { s -> AddPlaceDialog(engine, null, slot = s) { settingSlot = null } }
    // Home and Work under the search bar: each can be left off (the app and Android Auto alike)
    // the order of these shortcuts under the search bar: each moves up or down with its arrows
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val favRaw = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val favs = remember(favRaw) { pl.favourites() }
    var renamingFav by remember { mutableStateOf<Place?>(null) }
    var slotFor by remember { mutableStateOf<Place?>(null) }
    var taggingFav by remember { mutableStateOf<Place?>(null) }
    var tagFilter by remember { mutableStateOf("") }
    val tagRaw = engine.prefs.s(PreferenceKeys.FAV_TAGS, "")
    val tags = remember(favRaw, tagRaw) { pl.favTags() }
    SGroup(Lang.t("Favoriten", "Favourites")) {
        if (favs.isEmpty()) SNote(Lang.t("Noch keine Favoriten vorhanden.", "No favourites yet."))
        // your own tags ("Kunden", "Urlaub"): the list shows one tag's favourites at a time
        if (tags.isNotEmpty()) SItem(Lang.t("Schlagwort", "Tag"), if (tagFilter.isBlank() || tagFilter !in tags) Lang.t("Alle", "All") else tagFilter) {
            SButton(Lang.t("Alle", "All"), primary = tagFilter.isBlank() || tagFilter !in tags) { tagFilter = "" }
            for (t in tags) SButton(t, primary = tagFilter == t) { tagFilter = t }
        }
        favs.filter { tagFilter.isBlank() || tagFilter !in tags || pl.favTag(it) == tagFilter }.forEach { f ->
            when {
                taggingFav == f -> {
                    var t by remember(f) { mutableStateOf(pl.favTag(f)) }
                    SItem("Schlagwort für ${f.name}") {
                        SField(t, { t = it.take(20) }, placeholder = Lang.t("z. B. Kunden", "e.g. customers"), width = 200.dp)
                        SButton(Lang.t("Speichern", "Save"), primary = true) { pl.setFavTag(f, t); taggingFav = null }
                    }
                }
                renamingFav == f -> {
                    var t by remember(f) { mutableStateOf(f.name) }
                    SItem("${f.name} umbenennen") {
                        SField(t, { t = it.take(40) }, width = 220.dp)
                        SButton(Lang.t("Speichern", "Save"), primary = true) { pl.renameFavourite(f, t); renamingFav = null }
                    }
                }
                slotFor == f -> SItem("${f.name} festlegen als…") {
                    SButton(Lang.t("Zuhause", "Home"), primary = true) { pl.setSlot("home", f); slotFor = null; engine.toast(Lang.f("%1\$s ist jetzt Zuhause", "%1\$s is now Home", f.name), 2500) }
                    SButton(Lang.t("Arbeitsplatz", "Work"), primary = true) { pl.setSlot("work", f); slotFor = null; engine.toast(Lang.f("%1\$s ist jetzt Arbeit", "%1\$s is now Work", f.name), 2500) }
                    SButton(Lang.t("Abbrechen", "Cancel")) { slotFor = null }
                }
                else -> SItem(f.name, listOf(pl.favTag(f).let { if (it.isBlank()) "" else "#$it" }, f.sub).filter { it.isNotBlank() }.joinToString(" · ")) {
                    SButton(Lang.t("Schlagwort", "Tag")) { taggingFav = f }
                    SButton(Lang.t("Umbenennen", "Rename")) { renamingFav = f }
                    SButton(Lang.t("Zuhause / Arbeit", "Home / Work")) { slotFor = f }
                    SButton("In Deine Orte") { pl.moveFavouriteToPlaces(f) }
                    SButton(Lang.t("Entfernen", "Remove"), danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        if (favs.size > 1) SItem("Alle in Deine Orte", "Alle Favoriten kommen in einen Ordner „Favoriten“ unter Deine Orte. Die Liste neben Zuhause und Arbeit bleibt kurz.") { SButton("Alle verschieben", primary = true) { pl.moveAllFavouritesToPlaces() } }
        if (favs.size > 1) SItem(Lang.t("Alle Favoriten entfernen", "Remove all favourites")) { SButton(Lang.t("Alle löschen", "Delete all"), danger = true) { pl.clearFavourites() } }
    }
    SGroup(Lang.t("Verlauf", "History"), footer = Lang.t("Zum Entfernen zur Seite wischen.", "Swipe aside to remove.")) {
        SToggle(engine.prefs, Lang.t("Gespeichert & Zuletzt unter der Suchleiste", "Saved & Recent under the search bar"), PreferenceKeys.QUICKLIST, true, Lang.t("Die Liste der Favoriten und letzten Orte auf der Karte", "The list of favourites and recent places on the map"))
        SItem(Lang.t("Letzte Ziele", "Recent destinations"), "${pl.recents().size} letzte Ziele") {
            SButton("Löschen", danger = true) { pl.clearHistory(); engine.toast(Lang.t("Verlauf gelöscht", "History cleared"), 2000) }
        }
    }
    TripsGroup(engine)
}

@Composable
internal fun TripsGroup(engine: NavEngine) {
    var shown by remember { mutableStateOf(engine.trips.all()) }
    val fmt = java.text.SimpleDateFormat("EEE, d. MMM, HH:mm", java.util.Locale.GERMANY)
    val k = if (engine.imperial) 2.23694 else 3.6
    val unit = if (engine.imperial) "mph" else "km/h"
    SGroup(Lang.t("Fahrtenbuch", "Trip log")) {
        // closed at start to save room; the header (count and distance) opens and closes it
        var open by remember { mutableStateOf(false) }
        if (shown.isEmpty()) SNote(Lang.t("Noch keine Fahrten aufgezeichnet.", "No trips recorded yet."))
        else SItem("${shown.size} Fahrten", Geo.fmtDist(shown.sumOf { it.dist }, engine.imperial) + Lang.t(" insgesamt", " in total"), onClick = { open = !open }) {
            Text(if (open) "▴" else "▾", color = LocalVw.current.sub, fontSize = 18.sp)
        }
        if (!open) return@SGroup
        if (shown.isNotEmpty()) {
            SItem(Lang.t("Fahrtenbuch leeren", "Clear the trip log"), Lang.t("Alle Fahrten löschen", "Delete all trips")) { SButton("Löschen", danger = true) { engine.trips.clear(); shown = emptyList() } }
            val ctx = androidx.compose.ui.platform.LocalContext.current
            SItem(Lang.t("Exportieren", "Export"), Lang.t("CSV oder PDF. Fahrt antippen: dienstlich oder privat.", "CSV or PDF. Tap a trip: business or private.")) {
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
                "${Geo.fmtDur(mins * 60.0)} · in Fahrt ${Geo.fmtDur(t.moving.toDouble())} · max ${Math.round(t.maxSpeed * k)} $unit · " + (if (isBiz) Lang.t("Dienstlich", "Business") else Lang.t("Privat", "Private")),
                onClick = { com.headunit.launcher.TripExport.toggleBusiness(engine.prefs, t.start) },
            ) { Text(Geo.fmtDist(t.dist, engine.imperial), color = LocalVw.current.text, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
        }
    }
}


/** GPX tracks from Komoot, Strava or Garmin: import one, show it on the map, or drive it to its end. */
@Composable
internal fun GpxTracksGroup(engine: NavEngine) {
    val ctx = LocalContext.current
    var tracks by remember { mutableStateOf(com.headunit.launcher.Gpx.list(ctx)) }
    var shown by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    // the folders that are unfolded, and the track whose folder is being typed
    var openFolders by remember { mutableStateOf(setOf<String>()) }
    var movingId by remember { mutableStateOf<String?>(null) }
    var folderName by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val parsed = try { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { com.headunit.launcher.Gpx.parse(it.readText()) } } catch (e: Exception) { null }
            if (parsed == null) note = Lang.t("Keine Strecke in dieser Datei gefunden.", "No track found in this file.")
            else {
                com.headunit.launcher.Gpx.save(ctx, parsed.first, parsed.second)
                tracks = com.headunit.launcher.Gpx.list(ctx); note = null
            }
        }
    }
    fun refresh() { tracks = com.headunit.launcher.Gpx.list(ctx) }
    val trackRow: @Composable (com.headunit.launcher.GpxTrack) -> Unit = { t ->
        if (movingId == t.id) SItem("Ordner für ${t.name}", Lang.t("Leer lassen für keinen Ordner", "Leave empty for no folder")) {
            SField(folderName, { folderName = it.take(24) }, placeholder = "z. B. Urlaub", width = 200.dp)
            SButton(Lang.t("Speichern", "Save"), primary = true) { com.headunit.launcher.Gpx.setGroup(ctx, t.id, folderName); movingId = null; refresh() }
            SButton(Lang.t("Abbrechen", "Cancel")) { movingId = null }
        } else SItem(t.name, "${t.points} Punkte") {
            SButton(Lang.t("Ordner", "Folder")) { movingId = t.id; folderName = t.group }
            SButton(if (shown == t.id) Lang.t("Ausblenden", "Hide") else Lang.t("Zeigen", "Show")) {
                if (shown == t.id) { shown = null; engine.map.setTrack(emptyList()) }
                else { shown = t.id; engine.map.setTrack(com.headunit.launcher.Gpx.load(ctx, t.id)) }
            }
            SButton(Lang.t("Navigieren", "Navigate"), primary = true) {
                engine.driveTrack(t.name, com.headunit.launcher.Gpx.load(ctx, t.id))
                engine.externalTick++
            }
            SButton(Lang.t("Entfernen", "Remove"), danger = true) {
                if (shown == t.id) { shown = null; engine.map.setTrack(emptyList()) }
                com.headunit.launcher.Gpx.delete(ctx, t.id); refresh()
            }
        }
    }
    SGroup(Lang.t("GPX-Tracks", "GPX tracks"), footer = Lang.t("Eine .gpx-Datei aus Komoot, Strava, Garmin oder einer eigenen Aufnahme. „Zeigen“ legt sie auf die Karte, „Fahren“ führt bis zu ihrem Ende mit den üblichen Ansagen. Ordner fassen Tracks zusammen. In der Suche findest du einen Track über seinen Namen.", "A .gpx file from Komoot, Strava, Garmin or your own recording. Show puts it on the map, Drive takes you to its end with the usual spoken directions. Folders group tracks. In search you find a track by its name.")) {
        SItem(Lang.t("GPX-Datei importieren", "Import a GPX file"), note) { SButton(Lang.t("Datei wählen", "Choose file"), primary = true) { pick.launch(arrayOf("*/*")) } }
        if (tracks.isEmpty()) SNote(Lang.t("Noch keine Tracks importiert.", "No tracks imported yet."))
        val folders = tracks.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
        for (g in folders) {
            val open = g in openFolders
            SItem("Ordner: $g", "${tracks.count { it.group == g }} Tracks") {
                SButton(if (open) Lang.t("Zuklappen", "Fold") else Lang.t("Aufklappen", "Unfold")) { openFolders = if (open) openFolders - g else openFolders + g }
            }
            if (open) tracks.filter { it.group == g }.forEach { trackRow(it) }
        }
        tracks.filter { it.group.isBlank() }.forEach { trackRow(it) }
    }
}

/** Your own normal zoom when not on a route: a slider from further out to closer, and Reset. */
@Composable
internal fun DefaultZoomRow(prefs: Prefs) {
    val c = LocalVw.current
    SToggle(prefs, Lang.t("Automatischer Zoom", "Automatic zoom"), PreferenceKeys.AUTO_ZOOM, true, Lang.t("Aus: die Karte bleibt auf einem gleichen Zoom, egal wie schnell man fährt. + und − wirken trotzdem.", "Off: the map stays at one steady zoom, however fast you drive. + and − still work."))
    val saved = (prefs.s(PreferenceKeys.DEFAULT_ZOOM, "0").toFloatOrNull() ?: 0f).coerceIn(-2f, 2f)
    var v by remember(saved) { mutableStateOf(saved) }
    fun save(x: Float) = prefs.put(PreferenceKeys.DEFAULT_ZOOM, String.format(java.util.Locale.US, "%.2f", x))
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem(Lang.t("Standard-Zoom", "Default zoom"), Lang.t("Zoom ohne aktive Route", "Zoom without an active route")) {
            if (kotlin.math.abs(v) > 0.01f) SButton("Zurücksetzen") { v = 0f; save(0f) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(Lang.t("Weiter entfernt", "Further out"), color = c.sub, fontSize = 13.sp)
            androidx.compose.material3.Slider(value = v, onValueChange = { v = (Math.round(it * 4f) / 4f) }, onValueChangeFinished = { save(v) },
                valueRange = -2f..2f, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
            Text(Lang.t("Näher", "Closer"), color = c.sub, fontSize = 13.sp)
        }
    }
}

/**
 * Sending places from the phone to the head unit (through a free relay, encrypted with the pairing code). The head unit makes the
 * code; you type it on the phone.
 */
@Composable
internal fun PhoneLinkGroup(prefs: Prefs) {
    val code = prefs.s(PreferenceKeys.LINK_CODE, "")
    val role = prefs.s(PreferenceKeys.LINK_ROLE, "")
    var typing by remember { mutableStateOf(false) }
    SGroup(Lang.t("Orte vom Smartphone senden", "Send places from the phone"), footer = Lang.t("Einmalig koppeln: auf dem Autodisplay „Smartphone koppeln“ wählen.", "Pair once: choose \"Pair phone\" on the car screen.")) {
        when {
            code.isNotBlank() && role == "car" -> {
                SItem(Lang.t("Dies ist das Auto", "This is the car"), "Gekoppelt. Code ${com.headunit.launcher.CarLink.pretty(code)}") {
                    SButton(Lang.t("Entkoppeln", "Unpair"), danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, ""); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                val qr = remember(code) { qrCode(com.headunit.launcher.CarLink.pretty(code), 300) }
                if (qr != null) Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.size(110.dp).clip(RoundedCornerShape(12.dp)).background(Color.White).padding(6.dp)) {
                        androidx.compose.foundation.Image(qr, Lang.t("Kopplungscode als QR-Code", "Pairing code as a QR code"), Modifier.size(98.dp), filterQuality = androidx.compose.ui.graphics.FilterQuality.None)
                    }
                    Text(com.headunit.launcher.CarLink.pretty(code), color = LocalVw.current.text, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            code.isNotBlank() && role == "phone" -> SItem(Lang.t("Dies ist das Smartphone", "This is the phone"), "Mit deinem Auto gekoppelt (${com.headunit.launcher.CarLink.pretty(code)})") {
                SButton(Lang.t("Entkoppeln", "Unpair"), danger = true) { prefs.put(PreferenceKeys.LINK_CODE, ""); prefs.put(PreferenceKeys.LINK_ROLE, "") }
            }
            else -> {
                SItem(Lang.t("Auf dem Autodisplay", "On the car screen"), Lang.t("Erstellt einen Code zum Eingeben auf dem Smartphone", "Makes a code to enter on the phone")) {
                    SButton(Lang.t("Smartphone koppeln", "Pair phone"), primary = true) { prefs.put(PreferenceKeys.LINK_CODE, com.headunit.launcher.CarLink.newCode()); prefs.put(PreferenceKeys.LINK_ROLE, "car"); prefs.put(PreferenceKeys.LINK_LAST, "") }
                }
                SItem(Lang.t("Auf dem Smartphone", "On the phone"), Lang.t("Code eingeben, den das Autodisplay anzeigt", "Enter the code the car screen shows")) { SButton(Lang.t("Code eingeben", "Enter code")) { typing = true } }
                if (typing) SFieldItem(Lang.t("Code des Autos", "The car's code"), "", Lang.t("Zum Beispiel K7P-4QX-M2D", "For example K7P-4QX-M2D"), placeholder = "K7P-4QX-M2D") { t ->
                    val ok = com.headunit.launcher.CarLink.clean(t)
                    if (ok != null) { prefs.put(PreferenceKeys.LINK_CODE, ok); prefs.put(PreferenceKeys.LINK_ROLE, "phone"); typing = false }
                }
            }
        }
    }
}

/** Share your ETA and places sent from other apps (part of Places & trips). */
@Composable
internal fun SharingGroups(prefs: Prefs) {
    val ctx = LocalContext.current
    val pkg = android.net.Uri.parse("package:" + ctx.packageName)
    SGroup(Lang.t("Ankunftszeit teilen", "Share arrival time"), footer = Lang.t("Unter Optionen > „Ankunftszeit teilen“: Link zu Position, Ankunftszeit und Restweg. Endet bei Ankunft.", "Under Options > \"Share arrival time\": a link to position, arrival time and distance left. Ends on arrival.")) {
        SFieldItem(Lang.t("Name", "Name"), prefs.s(PreferenceKeys.TRIP_NAME, ""), Lang.t("Auf der Webseite, z. B. „Max ist unterwegs“", "On the web page, e.g. \"Max is on the way\""),
            placeholder = Lang.t("Name (optional)", "Name (optional)")) { prefs.put(PreferenceKeys.TRIP_NAME, it.trim().take(40)) }
    }
    SGroup(Lang.t("Orte aus anderen Apps", "Places from other apps"), footer = Lang.t("Orte aus Google Maps, WhatsApp oder Kontakten teilen.", "Share places from Google Maps, WhatsApp or Contacts.")) {
        SItem(Lang.t("Google Maps Links", "Google Maps links")) {
            SButton(Lang.t("Standardmäßig öffnen", "Open by default")) {
                if (android.os.Build.VERSION.SDK_INT >= 31) openSystemSettings(ctx, Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS, pkg)
                else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
            }
        }
    }
}

// ------------------------------------------------------------------ Fuel & charging

@Composable
internal fun FuelSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    SGroup(Lang.t("Kraftstoffpreise", "Fuel prices")) {
        SChoice(Lang.t("Kraftstoff", "Fuel"), listOf("E10" to "Super E10", "E5" to "Super E5", "B7" to "Diesel", "E85" to "Bioethanol (E85)", "EV" to Lang.t("Elektro", "Electric"), "LPG" to Lang.t("Autogas (LPG)", "LPG (Autogas)")), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
        // only an electric car gets stops planned on the route: for a combustion car the switch would do nothing, so it is not shown
        if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SToggle(prefs, Lang.t("Ladestopps auf langen Fahrten planen", "Plan charging stops on long trips"), PreferenceKeys.SMARTFUEL, true, Lang.t("Wenn der Akku nicht bis zum Ziel reicht", "When the battery won't reach the destination"))
        // the UK government's Fuel Finder (pump prices): only in Great Britain and only for a combustion car
        val ukFuel = engine.countryCode().uppercase() in setOf("GB", "UK") && prefs.s(PreferenceKeys.FUEL_TYPE, "E10") != "EV"
        if (ukFuel) {
            SFieldItem("Client ID", prefs.s(PreferenceKeys.FFID, "")) { prefs.put(PreferenceKeys.FFID, it.trim()) }
            SFieldItem("Client Secret", prefs.s(PreferenceKeys.FFSECRET, ""), password = true) { prefs.put(PreferenceKeys.FFSECRET, it.trim()) }
            var st by remember { mutableStateOf(FuelFinder.status) }
            var busy by remember { mutableStateOf(false) }
            SItem(Lang.t("Preise", "Prices"), st.ifBlank { if (FuelFinder.configured(prefs)) Lang.t("Noch nicht heruntergeladen", "Not downloaded yet") else Lang.t("Zuerst Daten eintragen", "Enter the details first") }) {
                SButton(if (busy) Lang.t("Sucht…", "Searching…") else Lang.t("Jetzt aktualisieren", "Update now"), primary = true) {
                    if (!busy && FuelFinder.configured(prefs)) {
                        busy = true; st = Lang.t("Tankstellen und Preise werden heruntergeladen…", "Downloading fuel stations and prices…")
                        sc.launch {
                            try { withContext(Dispatchers.IO) { FuelFinder.refresh(ctx, prefs, force = true) } } catch (e: Exception) { softFail(e) }
                            st = FuelFinder.status; busy = false
                        }
                    }
                }
            }
            SItem(Lang.t("Falschen Preis entdeckt?", "Spotted a wrong price?")) { SButton(Lang.t("Melden", "Report")) { openSystemSettings(ctx, android.content.Intent.ACTION_VIEW, android.net.Uri.parse(FuelFinder.REPORT_URL)) } }
        }
    }
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") CloudAccountGroup(prefs)
    if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SGroup(Lang.t("Elektroauto", "Electric car")) {
        EvCarItem(prefs)
        SFieldItem(Lang.t("Effizienz (km pro kWh)", "Efficiency (km per kWh)"), prefs.s(PreferenceKeys.EVMPK, ""), placeholder = Lang.t("z. B. 5.5", "e.g. 5.5"), number = true) { s -> prefs.put(PreferenceKeys.EVMPK, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        // a chosen car brings its own battery size; the typed one is then not used
        val carKwh = if (prefs.s(PreferenceKeys.EV_CAR, "").isNotBlank()) com.headunit.launcher.EvCars.batteryKwh(prefs) else null
        if (carKwh != null) SItem(Lang.t("Nutzbare Batterie", "Usable battery"), "${evNum(carKwh)} kWh aus den Fahrzeugdaten")
        else SFieldItem(Lang.t("Nutzbare Batterie (kWh)", "Usable battery (kWh)"), prefs.s(PreferenceKeys.EV_BATTERY_KWH, ""), Lang.t("Für Akkuschätzung und Ladestopps (Standard: 60 kWh)", "For the battery estimate and charging stops (default: 60 kWh)"), placeholder = Lang.t("z. B. 64", "e.g. 64"), number = true) { s -> prefs.put(PreferenceKeys.EV_BATTERY_KWH, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Strompreis (Cent oder Euro pro kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = "z. B. 38 oder 0,38", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
        SFieldItem(Lang.t("Restreichweite am Ziel (km)", "Range left at the destination (km)"), prefs.s(PreferenceKeys.EV_DEST_KM, ""), Lang.t("Mindestreichweite bei Ankunft (Standard: 50 km)", "Minimum range on arrival (default: 50 km)"), placeholder = Lang.t("z. B. 50", "e.g. 50"), number = true) { s -> prefs.put(PreferenceKeys.EV_DEST_KM, s.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(500)?.toString() ?: "") }
        SFieldItem(Lang.t("Restreichweite an der Ladesäule (km)", "Range left at the charger (km)"), prefs.s(PreferenceKeys.EV_CHARGER_KM, ""), Lang.t("Puffer, falls die Säule belegt ist (Standard: 30 km)", "Buffer if the charger is busy (default: 30 km)"), placeholder = Lang.t("z. B. 30", "e.g. 30"), number = true) { s -> prefs.put(PreferenceKeys.EV_CHARGER_KM, s.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceAtMost(500)?.toString() ?: "") }
        SChoice(Lang.t("Ladesäulen für Ladestopps", "Chargers for charging stops"), listOf("0" to Lang.t("Alle", "All"), "50" to "50 kW", "150" to "150 kW", "300" to "300 kW"), prefs.i(PreferenceKeys.EV_MIN_KW, 50).toString(),
            Lang.t("Mindestleistung für Ladestopps. Säulen ohne bekannte Leistung nur als letzte Wahl. Daten: Bundesnetzagentur.de (CC BY 4.0).", "Minimum power for charging stops. Chargers of unknown power only as a last resort. Data: Bundesnetzagentur.de (CC BY 4.0).")) { prefs.put(PreferenceKeys.EV_MIN_KW, it.toInt()) }
        EvOperatorsItem(prefs)
        SChoice(Lang.t("Umweg für bevorzugte Anbieter", "Detour for preferred networks"), listOf("5" to Lang.t("5 Min.", "5 min"), "10" to Lang.t("10 Min.", "10 min"), "15" to Lang.t("15 Min.", "15 min"), "20" to Lang.t("20 Min.", "20 min")), prefs.i(PreferenceKeys.EV_PREF_MIN, 10).toString(),
            Lang.t("Maximaler Umweg ohne zusätzlichen Stopp (Standard: 10 Min.)", "Maximum detour without an extra stop (default: 10 min)")) { prefs.put(PreferenceKeys.EV_PREF_MIN, it.toInt()) }
        SToggle(prefs, Lang.t("Ladesäulen-Daten im Ausland laden", "Load charger data abroad"), PreferenceKeys.FR_CHARGERS, false, Lang.t("Lädt bei Bedarf Daten für Frankreich (ca. 136 MB/Monat), Niederlande und Brüssel. Quellen: transport.data.gouv.fr (Licence Ouverte 2.0), NDW (CC0), Brussels Mobility (CC BY 4.0).", "Downloads data for France (about 136 MB/month), the Netherlands and Brussels as needed. Sources: transport.data.gouv.fr (Licence Ouverte 2.0), NDW (CC0), Brussels Mobility (CC BY 4.0)."))
        EvWidgetItem(prefs)
    } else SGroup(Lang.t("Kraftstoffkosten auf der Route", "Fuel cost on the route")) {
        SFieldItem(Lang.t("Verbrauch (Liter pro 100 km)", "Consumption (litres per 100 km)"), prefs.s(PreferenceKeys.FUELMPG, ""), placeholder = Lang.t("z. B. 6.5", "e.g. 6.5"), number = true) { s -> prefs.put(PreferenceKeys.FUELMPG, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(5)) }
        SFieldItem("Kraftstoffpreis (Cent oder Euro pro Liter)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = "z. B. 175 oder 1,75", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
        SFieldItem(Lang.t("Tankinhalt (Liter)", "Tank size (litres)"), prefs.s(PreferenceKeys.FUEL_TANK_L, ""), Lang.t("Für die Tank-Reichweite", "For the tank range"), placeholder = Lang.t("z. B. 50", "e.g. 50"), number = true) { s -> prefs.put(PreferenceKeys.FUEL_TANK_L, s.filter { it.isDigit() }.take(3)) }
    }
    SGroup(Lang.t("Live-Kosten", "Live cost")) {
        SToggle(prefs, Lang.t("Live-Spritkosten", "Live fuel cost"), PreferenceKeys.LIVE_FUEL_COST, false,
            Lang.t("Was die Fahrt bisher gekostet hat, als Zeile unter der Fahrtkarte, und die Summe auf der Ankunftskarte. Nutzt Verbrauch und Preis von oben", "What the trip has cost so far, as a line under the trip card, and the total on the arrival card. Uses the economy and price above"))
    }
    LearnedGroup(engine)
}

/** What the app has learned about your driving (and an electric car's use), with a button to start again. */
@Composable
internal fun LearnedGroup(engine: NavEngine) {
    var rows by remember { mutableStateOf(engine.ev.learnedSummary()) }
    SGroup(Lang.t("Aus Fahrten gelernt", "Learnt from trips"), footer = Lang.t("Verbessert Ankunftszeit und Reichweite", "Improves arrival time and range")) {
        rows.forEach { (k, v) -> SItem(k, v) }
        SItem(Lang.t("Gelerntes zurücksetzen", "Reset what was learnt")) { SButton("Zurücksetzen", danger = true) { engine.ev.resetLearned(); rows = engine.ev.learnedSummary() } }
    }
}


/** Town and village names for your country, so search works with no signal even without the full pack. */
@Composable
internal fun OfflinePlacesGroup(engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    val cc = engine.chosenPack().country.ifBlank { engine.detectedCountry }.ifBlank { "DE" }
    val where = countryName(cc)
    var state by remember { mutableStateOf(if (OfflinePlaces.installed(ctx)) Lang.t("Installiert (%.1f MB)", "Installed (%.1f MB)").format(OfflinePlaces.sizeMb(ctx)) else Lang.t("Nicht heruntergeladen", "Not downloaded")) }
    var busy by remember { mutableStateOf(false) }
    SGroup(Lang.t("Ortsnamen", "Place names"), footer = Lang.t("Orte in $where für die Suche ohne Signal.", "Places in $where for search without signal.")) {
        SItem(Lang.t("Städte und Dörfer", "Towns and villages"), state) {
            SButton(if (busy) Lang.t("Lädt…", "Loading…") else if (OfflinePlaces.installed(ctx)) Lang.t("Aktualisieren", "Update") else Lang.t("Herunterladen", "Download"), primary = !OfflinePlaces.installed(ctx)) {
                if (!busy) {
                    busy = true; state = Lang.t("Lädt herunter (benötigt Verbindung)…", "Downloading (needs a connection)…")
                    sc.launch {
                        try {
                            val n = withContext(Dispatchers.IO) { OfflinePlaces.download(ctx, cc) }
                            state = "Installiert: $n Orte"; engine.toast(Lang.t("Offline-Orte bereit", "Offline places ready"), 3000)
                        } catch (e: Exception) { state = "Fehlgeschlagen: ${e.message}" } finally { busy = false }
                    }
                }
            }
            if (OfflinePlaces.installed(ctx) && !busy) SButton(Lang.t("Entfernen", "Remove"), danger = true) { OfflinePlaces.delete(ctx); state = Lang.t("Nicht heruntergeladen", "Not downloaded") }
        }
    }
}
