package com.headunit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.Lang
import com.headunit.launcher.NavEngine
import com.headunit.launcher.parkHere
import com.headunit.launcher.parkedSpot
import com.headunit.launcher.startWalkToCar

/**
 * "Your car" on the phone, under the search bar while you're not on a route: where the car is parked and the walk back to it,
 * or, when no spot is saved, a Park here button that saves where you are now.
 */
@Composable
internal fun ParkedCard(engine: NavEngine) {
    if (engine.walking) return
    val c = LocalVw.current
    val spot = engine.parkedSpot()
    val here = engine.fix?.pos
    // "Park here" only once you've driven and stopped, and not on the phone while Android Auto is connected (it saves the spot itself)
    val canPark = engine.hasDetectedMovement && (engine.fix?.speed ?: 0.0) < 1.0 && !com.headunit.launcher.RouteSync.carConnected()
    if (spot == null && !canPark) return
    val gold = Color(0xFFFBBC04)
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(gold.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocalParking, null, tint = gold, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(if (spot == null) Lang.t("Kein Auto gespeichert", "No car saved") else Lang.t("Dein Auto", "Your car"),
                color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (spot != null && here != null) Text(Geo.fmtDist(Geo.dist(here, spot), engine.imperial) + Lang.t(" entfernt", " away"),
                color = c.sub, fontSize = 13.sp, maxLines = 1)
        }
        if (spot != null) ImmPill(Lang.t("Zu Fuß", "Walk"), { engine.startWalkToCar() }, filled = true)
        if (canPark) ImmPill(Lang.t("Hier parken", "Park here"), { engine.parkHere() }, filled = spot == null)
    }
}
