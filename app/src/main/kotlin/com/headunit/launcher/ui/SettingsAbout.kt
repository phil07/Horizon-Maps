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

/** One button that tests every key entered above with a small request each, and lists what worked. */
@Composable
internal fun KeyTestGroup(prefs: Prefs) {
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
internal fun KeysSection(prefs: Prefs, engine: NavEngine) {
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
        if (!engine.hasKey) SNote("Für genaueste Adresssuche: TomTom-Schlüssel eintragen.")
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
    SGroup("Satellitenbilder", footer = "Automatisch: Mapbox, TomTom, Landesamt (DE), Esri, MapTiler, EOX. Anbieter ohne Schlüssel werden übersprungen.") {
        SChoice("Quelle", listOf("auto" to "Automatisch", "mapbox" to "Mapbox", "landesamt" to "Landesamt", "esri" to "Esri", "maptiler" to "MapTiler", "tomtom" to "TomTom", "eox" to "EOX"),
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
internal fun AboutSection(prefs: Prefs, engine: NavEngine) {
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
        SItem(Lang.t("Was ist neu", "What's new"), Lang.t("Die Neuerungen dieser Version noch einmal ansehen", "See this version's changes again")) {
            SButton(Lang.t("Anzeigen", "Show")) { WhatsNewRequest.count++ }
        }
        if (logsOn) SItem("Logs", "In die Zwischenablage. Kann Standortdaten enthalten.") {
            SButton("Kopieren") {
                engine.toast("Logs werden gesammelt …", 2000)
                Thread {
                    val text = com.headunit.launcher.DebugLog.dump()
                    try {
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        cm?.setPrimaryClip(android.content.ClipData.newPlainText("Horizon Nav Logs", text))
                    } catch (e: Exception) { softFail(e) }
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
            SToggle(prefs, "Automatisch nach Updates suchen", PreferenceKeys.UPDCHECK, true, "Beim Öffnen der App mit Verbindung")
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
                // the settings and saved places, and the GPX tracks with their folders
                val backup = org.json.JSONObject(prefs.exportJson()).put("gpx_tracks", com.headunit.launcher.Gpx.backupArray(ctx))
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(backup.toString(2).toByteArray()) }
                true
            } catch (e: Exception) { false }
            android.widget.Toast.makeText(ctx, if (ok) tr("Sicherung gespeichert") else tr("Sicherung konnte nicht gespeichert werden"), android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    // the folder for the daily backup (see AutoBackup): one file, written over the one before
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) { AutoBackup.choose(ctx, uri, prefs); android.widget.Toast.makeText(ctx, tr("Ordner für die tägliche Sicherung gewählt"), android.widget.Toast.LENGTH_SHORT).show() }
    }
    // the picker for a restore: the newer one, or the older one on a unit where it is missing (a missing picker used to close the app)
    val onImported: (android.net.Uri?) -> Unit = { uri ->
        if (uri != null) {
            val msg = try {
                val text = ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val o = org.json.JSONObject(text)
                val tracks = o.optJSONArray("gpx_tracks")
                o.remove("gpx_tracks")
                val n = prefs.importJson(o.toString())
                "$n Einstellungen wiederhergestellt" + (tracks?.let { " · ${com.headunit.launcher.Gpx.restoreArray(ctx, it)} Tracks" } ?: "")
            } catch (e: Exception) { "Datei ist keine gültige Sicherung" }
            android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { onImported(it) }
    val importerOld = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onImported(it) }
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
            SButton("Sichern") {
                try { exporter.launch("horizon-nav-backup.json") } catch (e: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(ctx, tr("Auf diesem Gerät fehlt die Dateiauswahl zum Speichern"), android.widget.Toast.LENGTH_LONG).show()
                }
            }
            SButton("Wiederherstellen") {
                try { importer.launch(arrayOf("application/json", "text/plain", "*/*")) } catch (e: android.content.ActivityNotFoundException) {
                    try { importerOld.launch("*/*") } catch (e2: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(ctx, tr("Auf diesem Gerät fehlt die Dateiauswahl zum Wiederherstellen"), android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        // once a day into a folder you choose; the last file is replaced (see AutoBackup)
        val autoFolder = AutoBackup.folder(prefs)
        val autoLast = AutoBackup.lastMillis(prefs)
        SItem("Tägliche Sicherung", if (autoFolder.isBlank()) tr("Aus. Einmal am Tag in einen Ordner, die letzte Datei wird ersetzt.")
            else tr("An. ") + (if (autoLast > 0) tr("Letzte Sicherung: ") + java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(autoLast)) else tr("noch keine")))
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SButton(if (autoFolder.isBlank()) "Ordner wählen" else "Ordner ändern") {
                try { folderPicker.launch(null) } catch (e: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(ctx, tr("Auf diesem Gerät fehlt die Ordnerauswahl"), android.widget.Toast.LENGTH_LONG).show()
                }
            }
            if (autoFolder.isNotBlank()) SButton("Aus") { AutoBackup.clear(prefs) }
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
internal fun FeedbackDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var note by remember { mutableStateOf("") }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(Lang.t("Feedback oder Problem", "Feedback or a problem"), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(Lang.t("Was ist passiert oder was wünschst du dir? Hilfreich sind Angaben, wo du warst und worauf du getippt hast.", "What happened, or what would you like? Where you were and what you tapped helps."), color = c.sub, fontSize = 14.sp)
            SField(note, { note = it.take(600) }, placeholder = "z. B. Falsch umgeleitet bei Frankfurt", width = 516.dp, fill = true)
            Text(Lang.t("Mitgesendet werden: die App-Version, dein Autodisplay, die wichtigsten Einstellungen, deine heruntergeladenen Gebiete und der letzte Absturz (keine Orte oder API-Schlüssel).", "Sent with it: the app version, your head unit, the main settings, your downloaded areas and the last crash (no places or API keys)."), color = c.sub, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                SButton("Schließen") { onClose() }
                SButton("Kopieren") {
                    val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText(ctx.Lang.t("Horizon Nav Bericht", "Horizon Nav report"), engine.feedbackReport(note)))
                    engine.toast(engine.Lang.t("Bericht kopiert: In eine Nachricht oder einen Reddit-Kommentar einfügen", "Report copied: paste it into a message or a Reddit comment"), 3500)
                }
                SButton("Auf GitHub senden", primary = true) {
                    val title = note.lineSequence().firstOrNull()?.take(80)?.ifBlank { null } ?: "Bericht von Horizon Nav ${com.headunit.launcher.BuildConfig.VERSION_NAME}"
                    val body = engine.feedbackReport(note).take(5000)
                    val url = "https://github.com/${BuildConfig.UPDATE_REPO}/issues/new?title=" + android.net.Uri.encode(title) + "&body=" + android.net.Uri.encode(body)
                    try { ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    catch (e: Exception) { engine.toast(engine.Lang.t("Kein Browser auf diesem Gerät: stattdessen Kopieren nutzen", "No browser on this unit: use Copy instead"), 3500) }
                    onClose()
                }
            }
        }
    }
}
