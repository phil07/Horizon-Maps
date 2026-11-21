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

/**
 * Settings > Fuel > Your electric car: takes the battery level from another app's widget (e.g. the car maker's app). The
 * widget is hosted out of sight; Android asks once whether Horizon Nav may create widgets.
 */
@Composable
internal fun EvWidgetItem(prefs: Prefs) {
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
internal fun evNum(v: Double): String =
    if (v % 1.0 == 0.0) v.toLong().toString() else String.format(java.util.Locale.GERMANY, "%.1f", v)

/** "77 kWh · 175 kW" for a car in the list (the power is left out when the dataset doesn't know it). */
internal fun evCarDetails(car: com.headunit.launcher.EvCar): String =
    "${evNum(car.kwh)} kWh" + (car.maxKw?.let { " · ${evNum(Math.round(it).toDouble())} kW" } ?: "")

/**
 * Settings > Fuel > Your electric car > Preferred operators: a menu of the common networks with a tick box each. Ticking one
 * keeps the menu open, so several can be picked in one go; the charging-stop plan prefers their chargers (see NavEngine.planEv).
 */
@Composable
internal fun EvOperatorsItem(prefs: Prefs) {
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
internal fun EvCarItem(prefs: Prefs) {
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
internal fun EvCarDialog(prefs: Prefs, onClose: () -> Unit) {
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
internal fun EvCarRow(car: com.headunit.launcher.EvCar, onClick: () -> Unit) {
    val c = LocalVw.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 10.dp)) {
        Text(car.label, color = c.text, fontSize = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(evCarDetails(car), color = c.sub, fontSize = 13.sp)
    }
}
