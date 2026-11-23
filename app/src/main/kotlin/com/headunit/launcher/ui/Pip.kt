package com.headunit.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.NavEngine
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.RouteSync

/**
 * Picture in picture on phones: on a route, going to the home screen or another app keeps the map going in a small window with
 * the next turn and the time left, like Google Maps; a tap brings the app back. Settings › While driving › Picture in picture (on by
 * default). Never on a head unit (no phone in it), nor while Android Auto has the map.
 */
object Pip {
    /** The app is showing as the small window now (set by MainActivity). */
    var on by mutableStateOf(false)

    /** Whether leaving the app now should open the small window. */
    fun allowed(ctx: android.content.Context, engine: NavEngine): Boolean =
        android.os.Build.VERSION.SDK_INT >= 26 && engine.prefs.b(PreferenceKeys.PIP, true) && isPhoneDevice(ctx) &&
            engine.route != null && !engine.previewing && !RouteSync.carConnected()
}

/**
 * What the small window shows over the map: the next turn at the top, the time left and the arrival along the bottom. While it's
 * open the map zooms out a step and a half, your arrow is drawn smaller and sits low (see NavEngine.pipMode), so the road ahead
 * shows; all of it goes back when the app is full size again.
 */
@Composable
fun PipOverlay(engine: NavEngine) {
    DisposableEffect(Unit) {
        val zoom = engine.zoomOffset
        val pct = engine.prefs.s(PreferenceKeys.ARROW_PERCENT, "240").toIntOrNull() ?: 240
        engine.pipMode = true
        engine.zoomOffset = (zoom - 1.5).coerceIn(-4.0, 3.0)
        engine.map.setArrowScale(pct / 100f * 0.55f)
        onDispose {
            engine.pipMode = false
            engine.zoomOffset = zoom
            engine.map.setArrowScale(pct / 100f)
        }
    }
    Box(Modifier.fillMaxSize()) {
        engine.turn?.let { t ->
            Row(Modifier.align(Alignment.TopStart).padding(6.dp).background(Teal, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically) {
                ManeuverArrow(t.maneuver, Geo.maneuverAngle(t.maneuver), Modifier.size(24.dp), roundaboutExit = t.exit, roundaboutClockwise = engine.leftTraffic)
                Text(Geo.fmtDist(t.dist, engine.imperial), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
            }
        }
        engine.eta?.let { e ->
            Text("${Geo.fmtDur(e.secs)} · ${e.arrive}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                modifier = Modifier.align(Alignment.BottomCenter).padding(6.dp).background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
        }
    }
}
