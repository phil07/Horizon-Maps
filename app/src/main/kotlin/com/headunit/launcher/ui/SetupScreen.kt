package com.headunit.launcher.ui

import com.headunit.launcher.R
import androidx.compose.ui.res.stringResource

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.togetherWith
import com.headunit.launcher.NavEngine
import com.headunit.launcher.OfflineRouter
import com.headunit.launcher.Prefs
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.mapSource
import com.headunit.launcher.setApiKey
import kotlinx.coroutines.delay

private class SetupStep(val title: String, val sub: String, val icon: ImageVector)

private val setupSteps = listOf(
    SetupStep("Willkommen bei Horizon Nav", "In wenigen Schritten bist du startklar zum Fahren", Icons.Rounded.Navigation),
    SetupStep("Kartenschlüssel", "Mapbox für Karte und Routen, TomTom für die Suche", Icons.Rounded.VpnKey),
    SetupStep("Zuhause und Arbeit", "Ein Tipp von der Karte zu deinen Zielen", Icons.Rounded.Home),
    SetupStep("Offline-Paket", "Routen und Suche auch ohne Empfang", Icons.Rounded.CloudDownload),
    SetupStep("Berechtigungen", "Deine Position und Benachrichtigungen", Icons.Rounded.Shield),
)

/** First-run setup: map key, home and work, the offline pack and permissions, one step at a time. Every step can be skipped. */
@Composable
fun SetupScreen(prefs: Prefs, engine: NavEngine, onDone: () -> Unit) {
    val base = LocalVw.current
    val v = remember(base) { navVw(base) }
    CompositionLocalProvider(LocalVw provides v) { SetupBody(prefs, engine, onDone) }
}

@Composable
private fun SetupBody(prefs: Prefs, engine: NavEngine, onDone: () -> Unit) {
    val c = LocalVw.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    val last = setupSteps.size - 1
    fun finish() { engine.results = emptyList(); prefs.put(PreferenceKeys.SETUP_DONE, true); prefs.put(PreferenceKeys.FORCESETUP, false); onDone() }
    Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn(max = 760.dp).fillMaxWidth().padding(24.dp).shadow(16.dp, RoundedCornerShape(32.dp)).clip(RoundedCornerShape(32.dp))
                .background(c.panel).border(1.dp, c.line, RoundedCornerShape(32.dp)).padding(28.dp),
        ) {
            // progress dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                setupSteps.indices.forEach { i ->
                    val w by androidx.compose.animation.core.animateDpAsState(if (i == step) 28.dp else 8.dp, androidx.compose.animation.core.tween(220), label = "dot")
                    Box(Modifier.height(8.dp).width(w).clip(RoundedCornerShape(50)).background(if (i <= step) c.accent else c.tile))
                }
                Box(Modifier.weight(1f))
                if (step < last) Text(stringResource(R.string.ui_049), color = c.sub, fontSize = 14.sp, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { finish() }.padding(horizontal = 12.dp, vertical = 6.dp))
            }
            androidx.compose.animation.AnimatedContent(
                targetState = step,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(220, delayMillis = 60)) +
                        androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(260)) { dir * it / 12 }) togetherWith
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(120))
                },
                label = "setup",
            ) { s ->
                Column(Modifier.fillMaxWidth().padding(top = 22.dp).heightIn(min = 300.dp, max = 470.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(setupSteps[s].icon, null, tint = c.accent, modifier = Modifier.size(28.dp))
                        }
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(setupSteps[s].title, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text(setupSteps[s].sub, color = c.sub, fontSize = 14.sp)
                        }
                    }
                    when (s) {
                        0 -> WelcomeStep()
                        1 -> KeyStep(prefs, engine)
                        2 -> HomeWorkStep(engine)
                        3 -> PackStep(prefs, engine)
                        else -> PermissionStep()
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) SButton("Zurück") { engine.results = emptyList(); step-- }
                Box(Modifier.weight(1f))
                if (step < last) SButton(if (step == 0) "Starten" else "Weiter", primary = true) { engine.results = emptyList(); step++ }
                else SButton("Losfahren", primary = true) { finish() }
            }
        }
    }
}

@Composable
private fun SetupPoint(text: String) {
    val c = LocalVw.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Check, null, tint = Good, modifier = Modifier.size(20.dp))
        Text(text, color = c.text, fontSize = 15.sp, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun WelcomeStep() {
    val c = LocalVw.current
    Text(stringResource(R.string.ui_022), color = c.text, fontSize = 16.sp)
    SetupPoint("Live-Verkehr und schnellere Routen während der Fahrt")
    SetupPoint("Spurführung, Kreuzungsschilder und Geschwindigkeitswarnungen")
    SetupPoint("Offline-Routen und Suche für Funklöcher")
    Text(stringResource(R.string.ui_014), color = c.sub, fontSize = 13.sp)
}

@Composable
private fun KeyStep(prefs: Prefs, engine: NavEngine) {
    val c = LocalVw.current
    Text(stringResource(R.string.ui_026), color = c.text, fontSize = 15.sp)
    Text(stringResource(R.string.ui_001), color = c.sub, fontSize = 14.sp)
    var k by remember { mutableStateOf(prefs.s(PreferenceKeys.MAPBOX_TOKEN, "")) }
    SField(k, { k = it; prefs.put(PreferenceKeys.MAPBOX_TOKEN, com.headunit.launcher.MapboxRest.clean(it)) }, placeholder = "Mapbox Zugangstoken (pk.…)", width = 560.dp, fill = true)
    val t = k.trim()
    when {
        t.isEmpty() -> Text(stringResource(R.string.ui_065), color = c.sub, fontSize = 13.sp)
        t.startsWith("sk.") -> Text(stringResource(R.string.ui_057), color = Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        !com.headunit.launcher.MapboxRest.looksValid(t) -> Text(stringResource(R.string.ui_056), color = Warn, fontSize = 13.sp)
        else -> SetupPoint("Token hinzugefügt: Die Karte wird nach der Einrichtung geladen")
    }
    Spacer(Modifier.height(10.dp))
    Text(stringResource(R.string.ui_059), color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.ui_004), color = c.text, fontSize = 15.sp)
    Text(stringResource(R.string.ui_002), color = c.sub, fontSize = 14.sp)
    var tk by remember { mutableStateOf(prefs.s(PreferenceKeys.API_KEY, "")) }
    SField(tk, { tk = it; prefs.setApiKey(it) }, placeholder = "TomTom API-Schlüssel", width = 560.dp, fill = true)
    if (tk.isBlank()) Text(stringResource(R.string.ui_067), color = c.sub, fontSize = 13.sp)
    else SetupPoint("TomTom-Schlüssel hinzugefügt: Suche nutzt TomTom")
}

@Composable
private fun HomeWorkStep(engine: NavEngine) {
    val c = LocalVw.current
    var which by remember { mutableStateOf<String?>(null) }
    var q by remember { mutableStateOf("") }
    var rev by remember { mutableIntStateOf(0) }
    LaunchedEffect(q) { if (q.length >= 3) { delay(450); engine.search(q) } else engine.results = emptyList() }
    @Suppress("UNUSED_VARIABLE") val r = rev
    // where to look: the country you're in (worked out from the GPS) or anywhere
    // the label sits above the choice, so both options have the card's full width (side by side they were cut off)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.ui_044), color = c.sub, fontSize = 14.sp)
        Segmented(searchCountryOptions(engine), searchCountryValue(engine.prefs)) { engine.prefs.put(PreferenceKeys.COUNTRY, it); if (q.length >= 3) engine.search(q) }
    }
    for ((slot, label, icon) in listOf(Triple("home", "Zuhause", Icons.Rounded.Home), Triple("work", "Arbeit", Icons.Rounded.Work))) {
        val cur = engine.places.slot(slot)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(if (which == slot) c.accent.copy(alpha = 0.14f) else c.tile)
                .clickable { which = slot; q = "" }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (cur != null) c.accent else c.sub, modifier = Modifier.size(26.dp))
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(label, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(cur?.let { it.name + if (it.sub.isNotBlank()) ", " + it.sub else "" } ?: "Nicht festgelegt · Tippen zum Suchen", color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (cur != null) Icon(Icons.Rounded.Check, null, tint = Good, modifier = Modifier.size(22.dp))
        }
        if (which == slot) {
            SField(q, { q = it }, placeholder = "Suche nach deiner Adresse oder Postleitzahl", width = 640.dp, fill = true)
            engine.results.take(5).forEach { p ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                        engine.places.setSlot(slot, p); which = null; q = ""; engine.results = emptyList(); rev++
                    }.padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Column {
                        Text(p.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PackStep(prefs: Prefs, engine: NavEngine) {
    val c = LocalVw.current
    val ctx = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val rev = engine.packRev
    @Suppress("UNUSED_VARIABLE") val pick = prefs.s(PreferenceKeys.PACK_REGION, "")
    val region = engine.chosenPack()
    var picking by remember { mutableStateOf(false) }
    val installed = region.id in engine.installedPacks()
    Text(stringResource(R.string.ui_013), color = c.text, fontSize = 15.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(region.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        SButton("Gebiet ändern") { picking = true }
    }
    if (picking) PackPickerDialog(region, { prefs.put(PreferenceKeys.PACK_REGION, it.id) }) { picking = false }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (installed && !engine.packBusy) SetupPoint("Offline-Paket installiert")
        else SButton(if (engine.packBusy) "Lädt herunter…" else "Jetzt herunterladen", primary = true) {
            engine.downloadRegion(region)
        }
        if (!engine.onWifi() && !installed) Text(stringResource(R.string.ui_071), color = Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (engine.packBusy) LinearProgressIndicator(color = c.accent, trackColor = c.tile, modifier = Modifier.fillMaxWidth())
    if (engine.packStatus.isNotBlank()) Text(engine.packStatus + if (engine.packBusy) " · läuft weiter, wenn du fortfährst" else "", color = c.sub, fontSize = 13.sp)
    Text(stringResource(R.string.ui_069), color = c.sub, fontSize = 13.sp)
}

@Composable
private fun PermissionStep() {
    val c = LocalVw.current
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    @Suppress("UNUSED_VARIABLE") val t = tick
    fun has(p: String) = ctx.checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED
    val loc = has(Manifest.permission.ACCESS_FINE_LOCATION)
    val notif = android.os.Build.VERSION.SDK_INT < 33 || has(Manifest.permission.POST_NOTIFICATIONS)
    @Composable
    fun row(title: String, sub: String, ok: Boolean, perms: Array<String>) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.tile).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, color = c.sub, fontSize = 13.sp)
            }
            if (ok) Icon(Icons.Rounded.Check, "Erlaubt", tint = Good, modifier = Modifier.size(24.dp))
            else SButton("Erlauben", primary = true) { try { ask.launch(perms) } catch (e: Exception) { } }
        }
    }
    row("Standort", "Erforderlich, um deine Position auf der Karte zu zeigen und zu führen", loc, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    if (android.os.Build.VERSION.SDK_INT >= 33) row("Benachrichtigungen", "Damit die Navigation beim App-Wechsel weiterläuft", notif, arrayOf(Manifest.permission.POST_NOTIFICATIONS))
    if (!loc) Text(stringResource(R.string.ui_028), color = c.sub, fontSize = 13.sp)
}