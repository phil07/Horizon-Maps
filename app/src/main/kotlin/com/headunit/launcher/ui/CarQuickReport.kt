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
import androidx.compose.material.icons.rounded.Block
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
import com.headunit.launcher.ReportKind
import kotlinx.coroutines.delay

/** The quick reports on the car screen: the kinds, their icon and colour (the same reports as the phone's "Hier melden"). */
/** The icon of each kind on the quick panel (the phone's tiles use the same icons). */
private fun ReportKind.quickIcon(): ImageVector = when (this) {
    ReportKind.POLICE -> Icons.Rounded.LocalPolice
    ReportKind.ACCIDENT -> Icons.Rounded.CarCrash
    ReportKind.HAZARD -> Icons.Rounded.PriorityHigh
    ReportKind.BREAKDOWN -> Icons.Rounded.Build
    ReportKind.MOBILECAM -> Icons.Rounded.CameraAlt
    ReportKind.JAM -> Icons.Rounded.DirectionsCar
    ReportKind.ROADWORKS -> Icons.Rounded.Build
    ReportKind.CLOSED -> Icons.Rounded.Block
}

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
        ReportKind.entries.filter { it.quick }.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { kind ->
                    Column(Modifier.size(width = 150.dp, height = 110.dp).carTap("quick-${kind.key}") { engine.report(kind.key); engine.quickReportAt = 0L },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Box(Modifier.size(56.dp).clip(CircleShape).background(Color(kind.argb)), contentAlignment = Alignment.Center) {
                            Icon(kind.quickIcon(), null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Text(kind.label, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
        }
    }
}

private const val QUICK_CLOSE_MS = 8_000L
