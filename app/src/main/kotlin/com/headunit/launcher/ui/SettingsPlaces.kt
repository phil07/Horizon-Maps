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
    SItem(label, p?.let { it.name + (if (it.sub.isNotBlank()) ", " + it.sub else "") } ?: "Nicht festgelegt") {
        if (useHere != null) SButton("Hier verwenden", onClick = useHere)
        SButton(if (p != null) "Ändern" else "Festlegen", primary = true, onClick = onChange)
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
    SGroup("Unter der Suchleiste", footer = "Die Knöpfe unter der Suchleiste: Verknüpfungen, Such-Knöpfe, die Suchleiste und Zuhause und Arbeit. Der Knopf zeigt eine Liste, den nächsten Ort oder Track zuerst.") {
        SToggle(engine.prefs, "Meine Orte", PreferenceKeys.SC_PLACES, false, "Liste deiner Orte und Favoriten")
        SToggle(engine.prefs, "Meine Tracks", PreferenceKeys.SC_TRACKS, false, "Liste deiner GPX-Tracks")
        for ((kind, label) in listOf("fuel" to "Tanken", "charging" to "Laden", "food" to "Essen", "coffee" to "Kaffee", "parking" to "Parken",
            "camp" to "Stellplätze", "dump" to "Entsorgung", "lpg" to "Autogas")) {
            SToggle(engine.prefs, label, "find_$kind", true, "Knopf „$label in der Nähe“")
        }
        SToggle(engine.prefs, "Suche als Knopf", PreferenceKeys.SEARCH_FOLD, false, "Ohne Route nur ein runder Knopf in der Ecke")
        SToggle(engine.prefs, "Zuhause", PreferenceKeys.HOME_TILE, true, "Die Kachel Zuhause mit der Fahrtzeit")
        SToggle(engine.prefs, "Arbeit", PreferenceKeys.WORK_TILE, true, "Die Kachel Arbeit mit der Fahrtzeit")
        SToggle(engine.prefs, "Markierte Orte unter der Suche", PreferenceKeys.STARRED_ROW, true, "Aus: alle markierten Orte sind ausgeblendet. Sie bleiben in der Suche und in „Meine Orte“.")
        if (mine.isEmpty()) SNote("Noch keine eigenen Orte vorhanden.")
        mine.forEach { lp ->
            if (renaming == lp.label) {
                var t by remember(lp.label) { mutableStateOf(lp.label) }
                SItem("${lp.label} umbenennen") {
                    SField(t, { t = it.take(24) }, width = 220.dp)
                    SButton("Speichern", primary = true) { pl.renameCustom(lp.label, t); renaming = null }
                }
            } else if (folderFor == lp.label) {
                var f by remember(lp.label) { mutableStateOf(lp.folder) }
                SItem("Ordner für ${lp.label}", "Einen neuen Namen eintippen oder einen vorhandenen. Leer: kein Ordner.") {
                    SField(f, { f = it.take(24) }, width = 220.dp)
                    SButton("Speichern", primary = true) { pl.setFolder(lp.label, f); folderFor = null }
                }
            } else SItem(lp.label, (if (lp.folder.isNotBlank()) "Ordner: ${lp.folder} · " else "") + lp.place.name + if (lp.place.sub.isNotBlank()) ", " + lp.place.sub else "") {
                SButton(if (lp.starred) "★ Markiert" else "☆ Markieren", primary = lp.starred) { pl.setStar(lp.label, !lp.starred) }
                SButton("Ordner") { folderFor = lp.label }
                SButton("Umbenennen") { renaming = lp.label }
                SButton("Entfernen", danger = true) { pl.removeCustom(lp.label) }
            }
        }
        SItem("Ort hinzufügen") { SButton("Hinzufügen", primary = true) { adding = true } }
        if (mine.size > 1) {
            SNote("Die Reihenfolge der Knöpfe unter der Suchleiste. Mit den Pfeilen verschieben.")
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
    SGroup("Favoriten", footer = "A–Z: alphabetisch. Nächste zuerst: die nächsten Favoriten oben. Gilt auch für Android Auto.") {
        SChoice("Favoriten sortieren", listOf("az" to "A–Z", "near" to "Nächste zuerst"), engine.prefs.s(PreferenceKeys.FAV_SORT, "az")) { engine.prefs.put(PreferenceKeys.FAV_SORT, it) }
    }
    SGroup("Gespeicherte Orte", footer = "Auch über die Buttons unter der Suchleiste festlegbar.") {
        PlaceItem("Zuhause", remember(slotsRaw) { pl.slot("home") }, { settingSlot = "home" }, { pl.clearSlot("home") }, { useHereFor(engine, "home") })
        PlaceItem("Arbeitsplatz", remember(slotsRaw) { pl.slot("work") }, { settingSlot = "work" }, { pl.clearSlot("work") }, { useHereFor(engine, "work") })
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
                    SButton("Zuhause", primary = true) { pl.setSlot("home", f); slotFor = null; engine.toast(engine.Lang.f("%1\$s ist jetzt Zuhause", "%1\$s is now Home", f.name), 2500) }
                    SButton("Arbeitsplatz", primary = true) { pl.setSlot("work", f); slotFor = null; engine.toast(engine.Lang.f("%1\$s ist jetzt Arbeit", "%1\$s is now Work", f.name), 2500) }
                    SButton("Abbrechen") { slotFor = null }
                }
                else -> SItem(f.name, listOf(pl.favTag(f).let { if (it.isBlank()) "" else "#$it" }, f.sub).filter { it.isNotBlank() }.joinToString(" · ")) {
                    SButton("Schlagwort") { taggingFav = f }
                    SButton("Umbenennen") { renamingFav = f }
                    SButton("Zuhause / Arbeit") { slotFor = f }
                    SButton("In Deine Orte") { pl.moveFavouriteToPlaces(f) }
                    SButton("Entfernen", danger = true) { pl.toggleFavourite(f) }
                }
            }
        }
        if (favs.size > 1) SItem("Alle in Deine Orte", "Alle Favoriten kommen in einen Ordner „Favoriten“ unter Deine Orte. Die Liste neben Zuhause und Arbeit bleibt kurz.") { SButton("Alle verschieben", primary = true) { pl.moveAllFavouritesToPlaces() } }
        if (favs.size > 1) SItem("Alle Favoriten entfernen") { SButton("Alle löschen", danger = true) { pl.clearFavourites() } }
    }
    SGroup("Verlauf", footer = "Zum Entfernen zur Seite wischen.") {
        SToggle(engine.prefs, "Gespeichert & Zuletzt unter der Suchleiste", PreferenceKeys.QUICKLIST, true, "Die Liste der Favoriten und letzten Orte auf der Karte")
        SItem("Letzte Ziele", "${pl.recents().size} letzte Ziele") {
            SButton("Löschen", danger = true) { pl.clearHistory(); engine.toast(engine.Lang.t("Verlauf gelöscht", "History cleared"), 2000) }
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
            if (parsed == null) note = "Keine Strecke in dieser Datei gefunden."
            else {
                com.headunit.launcher.Gpx.save(ctx, parsed.first, parsed.second)
                tracks = com.headunit.launcher.Gpx.list(ctx); note = null
            }
        }
    }
    fun refresh() { tracks = com.headunit.launcher.Gpx.list(ctx) }
    val trackRow: @Composable (com.headunit.launcher.GpxTrack) -> Unit = { t ->
        if (movingId == t.id) SItem("Ordner für ${t.name}", "Leer lassen für keinen Ordner") {
            SField(folderName, { folderName = it.take(24) }, placeholder = "z. B. Urlaub", width = 200.dp)
            SButton("Speichern", primary = true) { com.headunit.launcher.Gpx.setGroup(ctx, t.id, folderName); movingId = null; refresh() }
            SButton("Abbrechen") { movingId = null }
        } else SItem(t.name, "${t.points} Punkte") {
            SButton("Ordner") { movingId = t.id; folderName = t.group }
            SButton(if (shown == t.id) "Ausblenden" else "Zeigen") {
                if (shown == t.id) { shown = null; engine.map.setTrack(emptyList()) }
                else { shown = t.id; engine.map.setTrack(com.headunit.launcher.Gpx.load(ctx, t.id)) }
            }
            SButton("Navigieren", primary = true) {
                engine.driveTrack(t.name, com.headunit.launcher.Gpx.load(ctx, t.id))
                engine.externalTick++
            }
            SButton("Entfernen", danger = true) {
                if (shown == t.id) { shown = null; engine.map.setTrack(emptyList()) }
                com.headunit.launcher.Gpx.delete(ctx, t.id); refresh()
            }
        }
    }
    SGroup("GPX-Tracks", footer = "Eine .gpx-Datei aus Komoot, Strava, Garmin oder einer eigenen Aufnahme. „Zeigen“ legt sie auf die Karte, „Fahren“ führt bis zu ihrem Ende mit den üblichen Ansagen. Ordner fassen Tracks zusammen. In der Suche findest du einen Track über seinen Namen.") {
        SItem("GPX-Datei importieren", note) { SButton("Datei wählen", primary = true) { pick.launch(arrayOf("*/*")) } }
        if (tracks.isEmpty()) SNote("Noch keine Tracks importiert.")
        val folders = tracks.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
        for (g in folders) {
            val open = g in openFolders
            SItem("Ordner: $g", "${tracks.count { it.group == g }} Tracks") {
                SButton(if (open) "Zuklappen" else "Aufklappen") { openFolders = if (open) openFolders - g else openFolders + g }
            }
            if (open) tracks.filter { it.group == g }.forEach { trackRow(it) }
        }
        tracks.filter { it.group.isBlank() }.forEach { trackRow(it) }
    }
}

/** Share your ETA and places sent from other apps (part of Places & trips). */
/** Your own normal zoom when not on a route: a slider from further out to closer, and Reset. */
@Composable
internal fun DefaultZoomRow(prefs: Prefs) {
    val c = LocalVw.current
    SToggle(prefs, "Automatischer Zoom", PreferenceKeys.AUTO_ZOOM, true, "Aus: die Karte bleibt auf einem gleichen Zoom, egal wie schnell man fährt. + und − wirken trotzdem.")
    val saved = (prefs.s(PreferenceKeys.DEFAULT_ZOOM, "0").toFloatOrNull() ?: 0f).coerceIn(-2f, 2f)
    var v by remember(saved) { mutableStateOf(saved) }
    fun save(x: Float) = prefs.put(PreferenceKeys.DEFAULT_ZOOM, String.format(java.util.Locale.US, "%.2f", x))
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        SItem("Standard-Zoom", "Zoom ohne aktive Route") {
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
internal fun SharingGroups(prefs: Prefs) {
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
internal fun FuelSection(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    val sc = rememberCoroutineScope()
    SGroup("Kraftstoffpreise") {
        SChoice("Kraftstoff", listOf("E10" to "Super E10", "E5" to "Super E5", "B7" to "Diesel", "E85" to "Bioethanol (E85)", "EV" to "Elektro", "LPG" to "Autogas (LPG)"), prefs.s(PreferenceKeys.FUEL_TYPE, "E10")) { prefs.put(PreferenceKeys.FUEL_TYPE, it) }
        // only an electric car gets stops planned on the route: for a combustion car the switch would do nothing, so it is not shown
        if (prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") SToggle(prefs, "Ladestopps auf langen Fahrten planen", PreferenceKeys.SMARTFUEL, true, "Wenn der Akku nicht bis zum Ziel reicht")
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
                            try { withContext(Dispatchers.IO) { FuelFinder.refresh(ctx, prefs, force = true) } } catch (e: Exception) { softFail(e) }
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
        SFieldItem("Strompreis (Cent oder Euro pro kWh)", prefs.s(PreferenceKeys.EVPPK, ""), placeholder = "z. B. 38 oder 0,38", number = true) { s -> prefs.put(PreferenceKeys.EVPPK, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
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
        SFieldItem("Kraftstoffpreis (Cent oder Euro pro Liter)", prefs.s(PreferenceKeys.FUELPPL, ""), placeholder = "z. B. 175 oder 1,75", number = true) { s -> prefs.put(PreferenceKeys.FUELPPL, s.replace(',', '.').filter { it.isDigit() || it == '.' }.take(6)) }
        SFieldItem("Tankinhalt (Liter)", prefs.s(PreferenceKeys.FUEL_TANK_L, ""), "Für die Tank-Reichweite", placeholder = "z. B. 50", number = true) { s -> prefs.put(PreferenceKeys.FUEL_TANK_L, s.filter { it.isDigit() }.take(3)) }
    }
    LearnedGroup(engine)
}

/** What the app has learned about your driving (and an electric car's use), with a button to start again. */
@Composable
internal fun LearnedGroup(engine: NavEngine) {
    var rows by remember { mutableStateOf(engine.learnedSummary()) }
    SGroup("Aus Fahrten gelernt", footer = "Verbessert Ankunftszeit und Reichweite") {
        rows.forEach { (k, v) -> SItem(k, v) }
        SItem("Gelerntes zurücksetzen") { SButton("Zurücksetzen", danger = true) { engine.resetLearned(); rows = engine.learnedSummary() } }
    }
}


/** Town and village names for your country, so search works with no signal even without the full pack. */
@Composable
internal fun OfflinePlacesGroup(engine: NavEngine) {
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
                            state = "Installiert: $n Orte"; engine.toast(engine.Lang.t("Offline-Orte bereit", "Offline places ready"), 3000)
                        } catch (e: Exception) { state = "Fehlgeschlagen: ${e.message}" } finally { busy = false }
                    }
                }
            }
            if (OfflinePlaces.installed(ctx) && !busy) SButton("Entfernen", danger = true) { OfflinePlaces.delete(ctx); state = "Nicht heruntergeladen" }
        }
    }
}
