package com.headunit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Lang
import com.headunit.launcher.Prefs
import com.headunit.launcher.VehicleProfiles
import kotlinx.coroutines.delay

/**
 * "A Bluetooth device you haven't assigned connected: which car is it?" Offered on the phone and on the car's screen; the answer is
 * the same as in Settings > Android Auto > Fahrzeuge. On the car it goes away by itself after a minute (it stays in Settings).
 */
@Composable
internal fun VehicleAskCard(prefs: Prefs, onCar: Boolean, modifier: Modifier = Modifier) {
    val pend = VehicleProfiles.pending(prefs) ?: return
    val c = LocalVw.current
    var shown by remember(pend.first) { mutableStateOf(true) }
    LaunchedEffect(pend.first, onCar) { if (onCar) { delay(60_000); shown = false } }
    if (!shown) return
    val cars = VehicleProfiles.cars(prefs)
    Column(modifier.panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(Lang.t("Neues Bluetooth-Gerät: ${pend.second}", "New Bluetooth device: ${pend.second}"), color = c.text,
            fontSize = if (onCar) 22.sp else 16.sp, fontWeight = FontWeight.Bold, maxLines = 2)
        Text(Lang.t("Welches Fahrzeug ist das?", "Which car is this?"), color = c.sub, fontSize = if (onCar) 18.sp else 13.sp)
        for (car in cars) AskButton(Lang.t("Zu ${car.name}", "This is ${car.name}"), "ask-${car.id}", onCar, primary = false) {
            VehicleProfiles.assign(prefs, car.id, pend.first)
            VehicleProfiles.clearPending(prefs)
            VehicleProfiles.switchTo(prefs, car.id)
        }
        AskButton(Lang.t("Neues Fahrzeug", "New car"), "ask-new", onCar, primary = true) {
            VehicleProfiles.create(prefs, pend.second, pend.first)
            VehicleProfiles.clearPending(prefs)
        }
        AskButton(Lang.t("Nicht jetzt", "Not now"), "ask-later", onCar, primary = false) { shown = false }
    }
}

@Composable
private fun AskButton(text: String, id: String, onCar: Boolean, primary: Boolean, action: () -> Unit) {
    val c = LocalVw.current
    val base = Modifier.fillMaxWidth().clip(CircleShape).background(if (primary) c.accent else c.line).padding(horizontal = 16.dp, vertical = if (onCar) 12.dp else 8.dp)
    Box(if (onCar) base.carTap(id, action) else base.clickable { action() }) {
        Text(text, color = if (primary) Color.White else c.text, fontSize = if (onCar) 20.sp else 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}
