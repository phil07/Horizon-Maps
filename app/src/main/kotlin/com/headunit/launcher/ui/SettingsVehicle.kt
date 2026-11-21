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

/** Settings > Fahrzeug: car, motorhome or car with trailer, with the motorhome's or trailer's top speed, size and weight. */
@Composable
internal fun VehicleGroup(prefs: Prefs) {
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
internal fun VehicleField(prefs: Prefs, kind: String, what: String, title: String, def: String, unit: String) {
    SFieldItem(title, prefs.s(com.headunit.launcher.Vehicle.key(kind, what), ""), Lang.t("Leer = $def $unit", "Empty = $def $unit"), placeholder = def, number = true) {
        prefs.put(com.headunit.launcher.Vehicle.key(kind, what), it.trim().replace(',', '.'))
    }
}


/** The battery level from the car maker's cloud: the user's own account and own (free) developer keys, nothing in between. */
@Composable
internal fun CloudAccountGroup(prefs: Prefs) {
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
internal fun VehiclesGroup(prefs: Prefs) {
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
