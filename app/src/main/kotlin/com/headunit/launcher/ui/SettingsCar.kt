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

/** Settings › Android Auto: what shows on the car's map, its sizes and the car screen's setup. */
@Composable
internal fun AndroidAutoSection(prefs: Prefs) {
    val ctx = LocalContext.current
    SGroup(Lang.t("Auf der Karte", "On the map")) {
        SToggle(prefs, Lang.t("Handy-Akku anzeigen", "Phone battery"), PreferenceKeys.CAR_PHONE_BATTERY, false, Lang.t("Ein kleines Zeichen mit dem Akkustand des Handys neben dem Tacho", "A small badge with your phone's battery level beside the speedometer"))
        SToggle(prefs, Lang.t("Sound-Knopf auf der Karte, wenn nicht navigiert", "Sound button on the map when not navigating"), PreferenceKeys.CAR_SOUND_IDLE, true, Lang.t("Aus: der Sound-Knopf erscheint nur während der Navigation, für mehr Platz", "Off: the sound button shows only while navigating, for more room"))
        SToggle(prefs, Lang.t("Suchleiste und Orte auf der Karte ausblenden", "Leave the search bar and places off the map"), PreferenceKeys.CAR_HIDE_SEARCH, false, Lang.t("Für Autos mit Drehknopf ohne Touchscreen: Suchleiste, Zuhause, Arbeit, Schnellwahl und Vorschlag verschwinden, wenn du nicht navigierst. Suche, Zuhause, Arbeit und gespeicherte Orte bleiben unter Orte (★)", "For cars with a rotary knob and no touchscreen: the search bar, Home, Work, shortcuts and the suggestion go when you're not navigating. Search, Home, Work and your saved places stay under Places (★)"))
        if (!prefs.b(PreferenceKeys.CAR_HIDE_SEARCH, false)) SChoice(Lang.t("Ausblenden nach", "Hide them after"),
            listOf("0" to Lang.t("Nie", "Never"), "30" to Lang.t("30 s", "30 s"), "60" to Lang.t("1 Min.", "1 min"), "120" to Lang.t("2 Min.", "2 min"), "300" to Lang.t("5 Min.", "5 min")),
            prefs.i(PreferenceKeys.CAR_HIDE_AFTER, 0).toString(),
            Lang.t("Suchleiste und Orte verschwinden nach dieser Zeit ohne Berührung, für eine freie Karte. Eine Berührung irgendwo holt sie zurück", "The search bar and places go after this long without a touch, for a clean map. A touch anywhere brings them back")) { prefs.put(PreferenceKeys.CAR_HIDE_AFTER, it.toInt()) }
        SChoice(Lang.t("Cool Running", "Cool running"), listOf("off" to Lang.t("Aus", "Off"), "auto" to Lang.t("Automatisch", "Automatic"), "always" to Lang.t("Immer", "Always")), prefs.s(PreferenceKeys.CAR_COOL, "off"),
            Lang.t("Für Handys, die heiß werden: 30 Bilder pro Sekunde, flache Karte mit weniger Details, keine Laternen, kein Regen oder Schnee. Automatisch schaltet es ein, wenn das Handy warm wird, und aus, sobald es abgekühlt ist", "For phones that get hot: 30 frames a second, a flat map with the lighter detail, no street lamps and no falling rain or snow. Automatic turns it on when the phone gets hot and off once it's cool")) { prefs.put(PreferenceKeys.CAR_COOL, it) }
        // the same setting as on the phone (Settings › Lanes and junctions), here for the car screen too
        SChoice(Lang.t("Zoom an Kreuzungen", "Junction zoom"), listOf("off" to Lang.t("Aus", "Off"), "further" to Lang.t("Weiter", "Further"), "closer" to Lang.t("Näher", "Closer"), "closest" to Lang.t("Am nächsten", "Closest")),
            prefs.s(PreferenceKeys.JUNCTION_ZOOM, "off"),
            Lang.t("Wie nah die Karte vor einer Abbiegung kommt, auch auf dem Autodisplay", "How close the map comes before a turn, on the car screen too")) { prefs.put(PreferenceKeys.JUNCTION_ZOOM, it) }
    }
    SGroup(Lang.t("Autodisplay", "Car screen")) {
        val gh = "com.google.android.projection.gearhead"
        val ghUri = android.net.Uri.parse("package:$gh")
        val hasAa = remember { try { ctx.packageManager.getPackageInfo(gh, 0); true } catch (e: Exception) { false } }
        val source = remember {
            try {
                val inst = if (android.os.Build.VERSION.SDK_INT >= 30) ctx.packageManager.getInstallSourceInfo(ctx.packageName).installingPackageName
                    else @Suppress("DEPRECATION") ctx.packageManager.getInstallerPackageName(ctx.packageName)
                when (inst) {
                    "com.android.vending" -> "Google Play"
                    null -> Lang.t("Unbekannt (aus Datei installiert)", "Unknown (installed from a file)")
                    ctx.packageName -> Lang.t("Eigener Updater", "Own updater")
                    else -> inst
                }
            } catch (e: Exception) { Lang.t("Unbekannt", "Unknown") }
        }
        SSlider(Lang.t("Gesamtgröße auf dem Autodisplay", "Overall size on the car screen"), prefs.i(PreferenceKeys.CARSCALE, 100).toFloat(), 40f..300f, "%", reset = 100f) { prefs.put(PreferenceKeys.CARSCALE, it) }
        SSlider(Lang.t("Tacho- und Tempolimitgröße", "Speedometer and limit size"), prefs.i(PreferenceKeys.CAR_SPEED_SIZE, 140).toFloat(), 40f..220f, "%", reset = 140f) { prefs.put(PreferenceKeys.CAR_SPEED_SIZE, it) }
        SChoice(Lang.t("Schrift des Fahrerdisplays", "Driver's display text size"), listOf("smaller" to Lang.t("Klein", "Smaller"), "normal" to Lang.t("Normal", "Normal"), "larger" to Lang.t("Groß", "Larger"), "largest" to Lang.t("Sehr groß", "Largest")),
            prefs.s(PreferenceKeys.DRIVER_TEXT, "larger"),
            Lang.t("Die Schrift der Abbiegekarte auf dem Autodisplay", "The writing on the turn card on the car screen")) { prefs.put(PreferenceKeys.DRIVER_TEXT, it) }
        SChoice(Lang.t("Tachostil auf dem Autodisplay", "Speedometer style on the car screen"), listOf("normal" to Lang.t("Normal", "Normal"), "compact" to Lang.t("Kompakt", "Compact")), prefs.s(PreferenceKeys.CAR_SPEED_STYLE, "normal"),
            Lang.t("Kompakt: eine kleine dunkle Pille mit Limit-Schild und Tempo nebeneinander, passt auch oben rechts auf einen kurzen Bildschirm", "Compact: a small dark pill with the limit sign and speed side by side; it also fits top right on a short screen")) { prefs.put(PreferenceKeys.CAR_SPEED_STYLE, it) }
        SChoiceWith(Lang.t("Position des Tachos auf dem Autodisplay", "Speedometer position on the car screen"), listOf("bottom" to Lang.t("Untere Ecke", "Bottom corner"), "turn" to Lang.t("Unter der Abbiege-Karte", "Under the turn card"), "top" to Lang.t("Oben rechts", "Top right")), prefs.s(PreferenceKeys.CAR_SPEED_POS, "bottom"),
            Lang.t("Untere Ecke: die Seite aus „Tacho-Seite“ (Beim Fahren)", "Bottom corner: the side set in \"Speed side\" (While driving)"), true) { prefs.put(PreferenceKeys.CAR_SPEED_POS, it) }
        var carAdvanced by rememberSaveable { mutableStateOf(false) }
        SItem(Lang.t("Erweiterte Autodisplay-Größen", "Advanced car screen sizes"), Lang.t("Einzelne Elemente unabhängig anpassen", "Set single elements on their own")) {
            SButton(if (carAdvanced) Lang.t("Ausblenden", "Hide") else Lang.t("Anzeigen", "Show")) { carAdvanced = !carAdvanced }
        }
        if (carAdvanced) {
            SSlider(Lang.t("Karten-Größe", "Card size"), prefs.i(PreferenceKeys.CAR_CARD_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_CARD_SIZE, it) }
            SSlider(Lang.t("Pfeilgröße der Abbiegekarte", "Turn arrow size"), prefs.i(PreferenceKeys.CAR_TURN_ARROW_SIZE, 100).toFloat(), 70f..200f, "%", sub = Lang.t("Nur der Pfeil, bis doppelt so groß", "The arrow on its own, up to twice as big"), reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_ARROW_SIZE, it) }
            SSlider(Lang.t("Breite der Abbiegekarte", "Turn card width"), prefs.i(PreferenceKeys.CAR_TURN_CARD_WIDTH, 100).toFloat(), 60f..150f, "%", sub = Lang.t("Wie weit die Karte über die Karte reicht", "How far the card reaches across the map"), reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_CARD_WIDTH, it) }
            SSlider(Lang.t("Höhe der Abbiegekarte", "Turn card height"), prefs.i(PreferenceKeys.CAR_TURN_CARD_HEIGHT, 100).toFloat(), 50f..200f, "%", sub = Lang.t("Der Platz über und unter der Schrift", "The room above and below the writing"), reset = 100f) { prefs.put(PreferenceKeys.CAR_TURN_CARD_HEIGHT, it) }
            SSlider(Lang.t("Button-Größe", "Button size"), prefs.i(PreferenceKeys.CAR_BUTTON_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_BUTTON_SIZE, it) }
            SSlider(Lang.t("Limit-Schild-Größe", "Speed-limit sign size"), prefs.i(PreferenceKeys.CAR_LIMIT_SIGN_SIZE, 100).toFloat(), 70f..200f, "%", steps = 26, reset = 100f) { prefs.put(PreferenceKeys.CAR_LIMIT_SIGN_SIZE, it) }
            SSlider(Lang.t("Textgröße", "Text size"), prefs.i(PreferenceKeys.CAR_TEXT_SIZE, 100).toFloat(), 70f..150f, "%", steps = 16, reset = 100f) { prefs.put(PreferenceKeys.CAR_TEXT_SIZE, it) }
            SSlider(Lang.t("Navigationspfeil-Größe", "Navigation arrow size"), prefs.i(PreferenceKeys.CAR_ARROW_SIZE, 70).toFloat(), 30f..180f, "%", reset = 70f) { prefs.put(PreferenceKeys.CAR_ARROW_SIZE, it) }
            SSlider(Lang.t("Leistenhöhe", "Bar height"), prefs.i(PreferenceKeys.CAR_BAR_HEIGHT, 100).toFloat(), 60f..100f, "%", sub = Lang.t("Die Suchleiste schlanker, damit mehr von der Karte frei bleibt", "The search bar slimmer, to leave more of the map"), steps = 8, reset = 100f) { prefs.put(PreferenceKeys.CAR_BAR_HEIGHT, it) }
            SSlider(Lang.t("Straßennamen-Größe", "Road name size"), prefs.i(PreferenceKeys.CAR_ROAD_NAME_SIZE, 100).toFloat(), 70f..200f, "%", steps = 12, reset = 100f) { prefs.put(PreferenceKeys.CAR_ROAD_NAME_SIZE, it) }
        }
        SGroup("Knöpfe auf der Fahrtkarte") {
            SToggle(prefs, Lang.t("Suche", "Search"), PreferenceKeys.TRIP_BTN_SEARCH, true, "Such-Taste auf der Fahrtkarte: ein gewählter Ort wird als Stopp oder als Ziel gefragt")
            SToggle(prefs, "Übersicht", PreferenceKeys.TRIP_BTN_OVERVIEW, true, "Die ganze Route auf einen Blick")
            SToggle(prefs, "Ansagen", PreferenceKeys.TRIP_BTN_OTHERS, true, "Alle Ansagen, nur Warnungen oder aus")
            SToggle(prefs, "Schnellmeldung", PreferenceKeys.TRIP_BTN_QUICK, true, "Megafon: Panel mit Polizei, Unfall, Gefahr, Panne, mobilem Blitzer und Stau. Ein Tipp meldet, wo du bist; es schließt sich nach 8 Sekunden")
            SToggle(prefs, "Beenden", PreferenceKeys.TRIP_BTN_END, true, "Die Fahrt beenden")
            SChoice("Knopfgröße", listOf("normal" to Lang.t("Normal", "Normal"), "large" to "Groß", "xl" to "Extra groß"), prefs.s(PreferenceKeys.TRIP_BTN_SIZE, "normal"),
                "Weniger Knöpfe machen die Karte schmaler, so bleibt mehr von der Karte frei") { prefs.put(PreferenceKeys.TRIP_BTN_SIZE, it) }
        }
        SToggle(prefs, Lang.t("Pfeil seitlich lassen", "Keep the arrow to the side"), PreferenceKeys.KEEP_ARROW_SIDE, false, Lang.t("Auf breitem Bildschirm bleibt der Pfeil seitlich, wenn die Karten einklappen, statt in die Mitte zu gleiten", "On a wide screen the arrow stays to the side when the cards fold away, instead of gliding back to the middle"))
        VehiclesGroup(prefs)
        SItem(Lang.t("Installiert durch", "Installed by"), source)
        SItem(Lang.t("Sprachsuche im Auto", "Voice search in the car"), Lang.t("„Hey Google, navigiere mit Horizon Nav zu …“. Ohne „mit Horizon Nav“ nur als Standard-Navigations-App.", "\"Hey Google, navigate to … with Horizon Nav\". Without \"with Horizon Nav\" only as the default navigation app."))
        SItem(Lang.t("Android Auto Einstellungen", "Android Auto settings"), if (hasAa) null else Lang.t("Android Auto ist auf diesem Telefon nicht installiert", "Android Auto isn't installed on this phone")) {
            if (hasAa) SButton(Lang.t("Öffnen", "Open")) {
                val i = ctx.packageManager.getLaunchIntentForPackage(gh)
                try {
                    if (i != null) ctx.startActivity(i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) else openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri)
                } catch (e: Exception) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
            }
        }
        if (hasAa) SItem(Lang.t("Android Auto App-Info", "Android Auto app info")) {
            SButton(Lang.t("App-Info", "App info")) { openSystemSettings(ctx, Settings.ACTION_APPLICATION_DETAILS_SETTINGS, ghUri) }
        }
    }
}
