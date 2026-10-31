package com.headunit.launcher.ui

import com.headunit.launcher.Lang

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflineRouter
import com.headunit.launcher.PackRegions
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.headunit.launcher.Prefs
import java.io.File
import com.headunit.launcher.PreferenceKeys

/**
 * The Mapbox map with no signal: a circle round Home, the current route's strip, and (optionally) every route's strip as it starts.
 * Mapbox keeps these on the device itself; the map uses them automatically when there's no signal.
 */
@Composable
private fun MapboxOfflineGroup(prefs: Prefs, engine: NavEngine) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) { com.headunit.launcher.MapboxOffline.refresh(ctx) }
    val off = com.headunit.launcher.MapboxOffline
    var confirm by remember { mutableStateOf<String?>(null) }
    SGroup(Lang.t("Mapbox-Karte ohne Empfang", "Mapbox map with no signal"), footer = Lang.t("Mapbox erlaubt pro Gerät nur begrenzte Offline-Gebiete: Ein Umkreis um Zuhause und mehrere Routen passen, ein ganzes Land nicht. Zuletzt befahrene Gebiete werden außerdem automatisch behalten.", "Mapbox lets each device keep only limited offline areas: a radius round home and several routes fit, a whole country doesn't. Areas you've driven recently are also kept automatically.")) {
        if (!engine.hasMapbox) SNote(Lang.t("Trage zuerst deinen Mapbox-Schlüssel unter Einstellungen > Schlüssel & Konten ein.", "Add your Mapbox access token in Settings > Keys & accounts first."))
        val km = prefs.s(PreferenceKeys.MBHOMEKM, "30").toIntOrNull() ?: 30
        val home = engine.places.slot("home")
        SItem(Lang.t("Rund um Zuhause", "Around home"), if (home == null) Lang.t("Lege zuerst Zuhause fest (suchen, dann einen Ort als Zuhause speichern)", "Set Home first (Search, then save a place as Home)") else Lang.t("Die 3D-Karte im Umkreis von $km km um Zuhause · etwa ${off.homeMb(km)} MB", "The 3D map within $km km of Home · about ${off.homeMb(km)} MB"))
        // (the distance choice and Save go on a line of their own: beside the title they squeezed it into a narrow column)
        androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Segmented(listOf("15" to "15 km", "30" to "30 km", "50" to "50 km"), km.toString()) { prefs.put(PreferenceKeys.MBHOMEKM, it) }
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            if (home != null && engine.hasMapbox) SButton(if (off.busy?.startsWith("Around home") == true) Lang.t("Wird gespeichert…", "Saving…") else Lang.t("Speichern", "Save"), primary = true) { off.saveHome(ctx, home.pos, km) }
        }
        val r = engine.route
        SItem(Lang.t("Karte für diese Route speichern", "Save the map for this route"), if (r == null) Lang.t("Plane zuerst eine Route", "Plan a route first") else engine.offlineRouteName() + Lang.t(" · ein Streifen entlang der Route · etwa ${off.routeMb(r.len)} MB", " · a strip along the route · about ${off.routeMb(r.len)} MB")) {
            if (r != null && engine.hasMapbox) SButton(Lang.t("Speichern", "Save"), primary = true) { off.saveRoute(ctx, engine.offlineRouteName(), r.pts) }
        }
        SToggle(prefs, Lang.t("Karte jeder Route speichern", "Save every route's map"), "mbautoroute", false, Lang.t("Beim Routenstart wird der Streifen im Hintergrund gespeichert", "When a route starts, its strip is saved in the background"))
        if (prefs.b(PreferenceKeys.MBAUTOROUTE, false)) SChoice(Lang.t("Routen speichern", "Save routes"), listOf("wifi" to Lang.t("Nur im WLAN", "On Wi-Fi only"), "always" to Lang.t("Immer (nutzt mobile Daten)", "Always (uses mobile data)")), prefs.s(PreferenceKeys.MAPBOX_AUTO_NETWORK, "wifi")) { prefs.put(PreferenceKeys.MAPBOX_AUTO_NETWORK, it) }
        if (off.saved.isNotEmpty() || off.busy != null) {
            SItem(Lang.t("Gespeichert", "Saved"), null)
            for (a in off.saved) {
                SItem(a.name, "%.0f MB".format(a.mb) + if (a.complete) Lang.t(" · gespeichert", " · saved") else Lang.t(" · teilweise gespeichert", " · part saved")) {
                    if (confirm == a.id) {
                        SButton(Lang.t("Ja, löschen", "Yes, delete"), danger = true) { confirm = null; off.delete(ctx, a.id) }
                        SButton(Lang.t("Behalten", "Keep")) { confirm = null }
                    } else SButton(Lang.t("Löschen", "Delete"), danger = true) { confirm = a.id }
                }
            }
        }
        if (off.busy != null) {
            SItem(off.busy ?: "", off.status) { SButton(Lang.t("Stopp", "Stop")) { off.stop() } }
            LinearProgressIndicator(progress = { off.progress }, color = LocalVw.current.accent, trackColor = LocalVw.current.tile, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        } else if (off.status.isNotBlank()) SNote(off.status)
    }
}

private fun fmtSize(b: Long): String = if (b >= 1_000_000_000L) "%.1f GB".format(b / 1e9) else "%.0f MB".format(b / 1e6)

/** The downloaded areas: what's on the unit, which is in use, add another, update and delete. */
@Composable
internal fun OfflineMapsContent(prefs: Prefs, engine: NavEngine) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val rev = engine.packRev // re-read the files when a download, switch or delete finishes
    @Suppress("UNUSED_VARIABLE") val pick = prefs.s(PreferenceKeys.PACK_REGION, "")   // redraw when the area to add changes
    val region = engine.chosenPack()
    val have = engine.installedPacks()
    val activeId = engine.installedPackId()
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }
    // has the area to add been built yet? (checked when it changes)
    var avail by remember(region.id) { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(region.id, have.size) { if (region.id !in have) avail = withContext(Dispatchers.IO) { PackRegions.available(region) } }
    // sizes and free space are worked out in the background (a pack's font folder holds thousands of small files)
    val sizes by androidx.compose.runtime.produceState(emptyMap<String, Long>(), have, rev) {
        value = withContext(Dispatchers.IO) { have.associateWith { OfflineRouter.sizeOf(OfflineRouter.packDir(ctx, it)) } }
    }
    // areas downloaded before the speed limit and camera files existed: has the file been published for them since?
    val extrasReady by androidx.compose.runtime.produceState(emptySet<String>(), have, rev) {
        value = withContext(Dispatchers.IO) {
            have.filter { !File(OfflineRouter.packDir(ctx, it), "limits.bin").exists() && PackRegions.hasExtras(PackRegions.byId(it)) == true }.toSet()
        }
    }
    val free by androidx.compose.runtime.produceState(-1L, rev) {
        value = withContext(Dispatchers.IO) { try { android.os.StatFs(OfflineRouter.packsRoot(ctx).path).availableBytes } catch (e: Exception) { -1L } }
    }

    MapboxOfflineGroup(prefs, engine)
    SGroup(Lang.t("Heruntergeladene Gebiete", "Downloaded areas"), footer = Lang.t("Behalte so viele Gebiete, wie du willst: Das Gebiet, in dem du bist, wird automatisch genutzt (Routen funktionieren offline innerhalb eines Gebiets). Jedes zeigt die Straßen vom Tag seiner Erstellung, also ab und zu aktualisieren.", "Keep as many areas as you like: the one you're in is used automatically (routes work offline within one area). Each is a snapshot of the roads on the day it was built, so update now and then.") + if (free >= 0) Lang.t(" ${fmtSize(free)} frei.", " ${fmtSize(free)} free.") else "") {
        if (have.isEmpty()) SNote(Lang.t("Noch nichts heruntergeladen. Wähle unten ein Gebiet und lade es im WLAN herunter.", "Nothing downloaded yet. Pick an area below and download it on Wi-Fi."))
        for (id in have) {
            val r = PackRegions.byId(id)
            val d = OfflineRouter.packDir(ctx, id)
            val tiles = File(d, "tiles.tar")
            val built = if (tiles.exists()) java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.UK).format(java.util.Date(tiles.lastModified())) else ""
            val inUse = id == activeId
            val hasLimits = File(d, "limits.bin").exists()
            SItem(r.name + if (inUse) Lang.t(" · in Gebrauch", " · in use") else "", (sizes[id]?.let { fmtSize(it) } ?: "…") + (if (built.isNotEmpty()) Lang.t(" · erstellt $built", " · built $built") else "") +
                (if (hasLimits) Lang.t(" · Tempolimits und Blitzer offline", " · speed limits and cameras offline") else "")) {
                if (confirmDelete == id) {
                    SButton(Lang.t("Ja, löschen", "Yes, delete"), danger = true) { confirmDelete = null; engine.deletePack(id) }
                    SButton(Lang.t("Behalten", "Keep")) { confirmDelete = null }
                } else {
                    if (!inUse && have.size > 1) SButton(Lang.t("Verwenden", "Use")) { engine.useArea(id) }
                    SButton(if (engine.packDownloading == id) Lang.t("Wird aktualisiert…", "Updating…") else Lang.t("Aktualisieren", "Update")) { engine.downloadRegion(r) }
                    SButton(Lang.t("Löschen", "Delete"), danger = true) { confirmDelete = id }
                }
            }
        }
        for (id in extrasReady) if (engine.packDownloading != id)
            SNote(Lang.t("Offline-Tempolimits und -Blitzer sind jetzt für ${PackRegions.byId(id).name} bereit: tippe auf Aktualisieren (nur die neuen Dateien werden geladen, meist unter 100 MB).", "Offline speed limits and cameras are now ready for ${PackRegions.byId(id).name}: tap its Update (only the new files download, usually under 100 MB)."))
        val age = engine.packAgeDays()
        if (age != null && age >= 120 && !engine.packBusy) SNote(Lang.t("Das genutzte Gebiet ist etwa ${age / 30} Monate alt. Aktualisiere es im WLAN für neue Straßen und Kreuzungen.", "The area in use is about ${age / 30} months old. Update it on Wi-Fi to pick up new roads and junction changes."))
    }
    SGroup(Lang.t("Gebiet hinzufügen", "Add an area")) {
        SItem(Lang.t("Gebiet", "Area"), region.name + " · " + region.group) { SButton(Lang.t("Ändern", "Change")) { picking = true } }
        if (region.id !in have && avail == false) SNote(Lang.t("Das Paket ${region.name} wurde noch nicht gebaut und kann gerade nicht heruntergeladen werden.", "The ${region.name} pack hasn't been built yet, so it can't be downloaded at the moment."))
        SItem(if (region.id in have) Lang.t("${region.name} ist bereits heruntergeladen", "${region.name} is already downloaded") else Lang.t("${region.name} herunterladen", "Download ${region.name}"), Lang.t("Am besten im WLAN: Ein Land hat meist 1 bis 4 GB", "Best done on Wi-Fi: a country is usually 1 to 4 GB")) {
            SButton(if (engine.packBusy) Lang.t("Wird heruntergeladen…", "Downloading…") else if (region.id in have) Lang.t("Aktualisieren", "Update") else Lang.t("Herunterladen", "Download"), primary = true) { engine.downloadRegion(region) }
        }
        if (engine.packBusy) LinearProgressIndicator(color = c.accent, trackColor = c.tile, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
        if (engine.packStatus.isNotBlank()) SNote((engine.packDownloading?.let { PackRegions.byId(it).name + ": " } ?: "") + engine.packStatus + if (engine.packBusy) Lang.t(" · du kannst diese Seite verlassen", " · you can leave this page") else "")
    }
    SGroup(Lang.t("Ohne Empfang", "Without signal")) {
        SChoice(Lang.t("Welche Karte gezeichnet wird", "Which map to draw"), listOf("auto" to "Auto", "always" to Lang.t("Heruntergeladen", "Downloaded"), "never" to "Online"), prefs.s(PreferenceKeys.OFF_MAP, "auto"),
            Lang.t("Auto: Ohne Empfang nutzt die Mapbox-Karte ihre gespeicherten Gebiete (wo du zuletzt warst und gespeicherte Mapbox-Gebiete); die Karte des heruntergeladenen Pakets springt nur ein, wo Mapbox nichts hat, und mit Empfang kommt die Online-Karte zurück", "Auto: with no signal Mapbox's map carries on with the areas it has stored (where you've been recently, and saved Mapbox areas); the downloaded pack's map takes over only where Mapbox has nothing, and the online map comes back with signal")) { prefs.put(PreferenceKeys.OFF_MAP, it); engine.restyle() }
    }
    if (picking) PackPickerDialog(region, { prefs.put(PreferenceKeys.PACK_REGION, it.id) }) { picking = false }
}
