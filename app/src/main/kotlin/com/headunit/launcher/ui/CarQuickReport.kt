package com.headunit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Lang
import com.headunit.launcher.NavEngine
import kotlinx.coroutines.delay

/** The quick reports on the car screen: the kinds, their icon and colour (the same reports as the phone's "Hier melden"). */
private val quickKinds: List<Triple<String, ImageVector, Color>> = listOf(
    Triple("police", Icons.Rounded.LocalPolice, Color(0xFF1E66F5)),
    Triple("accident", Icons.Rounded.CarCrash, Bad),
    Triple("hazard", Icons.Rounded.PriorityHigh, Warn),
    Triple("breakdown", Icons.Rounded.Build, Color(0xFFFB8C00)),
    Triple("mobilecam", Icons.Rounded.CameraAlt, Color(0xFF1E66F5)),
    Triple("jam", Icons.Rounded.DirectionsCar, Color(0xFFF4511E)),
)

/**
 * The megaphone's panel on Android Auto, over the map: one tap reports where you are and closes it. Without a tap it closes
 * by itself after [QUICK_CLOSE_MS].
 */
@Composable
internal fun QuickReportPanel(engine: NavEngine, modifier: Modifier) {
    val at = engine.quickReportAt
    LaunchedEffect(at) {
        if (at > 0L) {
            delay(QUICK_CLOSE_MS)
            if (engine.quickReportAt == at) engine.quickReportAt = 0L
        }
    }
    if (at <= 0L) return
    val c = LocalVw.current
    Column(modifier.panel(14.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(Lang.t("Schnellmeldung hier", "Quick report here"), color = c.sub, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        quickKinds.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (kind, icon, tint) ->
                    Column(Modifier.size(width = 150.dp, height = 110.dp).carTap("quick-$kind") { engine.report(kind); engine.quickReportAt = 0L },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Text(engine.reportName(kind), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

private const val QUICK_CLOSE_MS = 8_000L
