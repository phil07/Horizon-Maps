package com.headunit.launcher.ui

import com.headunit.launcher.acceptFaster
import com.headunit.launcher.answerVote
import com.headunit.launcher.cycleSim
import com.headunit.launcher.dismissVote
import com.headunit.launcher.endWalk
import com.headunit.launcher.handleVoice
import com.headunit.launcher.skipSimTurn
import com.headunit.launcher.startWalkToCar
import com.headunit.launcher.stopSim
import com.headunit.launcher.R
import com.headunit.launcher.Lang
import androidx.compose.ui.res.stringResource

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PropaneTank
import androidx.compose.material.icons.rounded.RvHookup
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.NorthWest
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Satellite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Traffic
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wc
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.Junctions
import com.headunit.launcher.LL
import com.headunit.launcher.Place
import com.headunit.launcher.NavEngine
import kotlinx.coroutines.delay
import com.headunit.launcher.PreferenceKeys
import com.headunit.launcher.softFail

/** Speed limit sign beside a large readout of your current speed. */
@Composable
internal fun ImmSpeed(engine: NavEngine) {
    // the car screen can show the compact pill instead: a small dark pill with the limit sign and speed side by side
    // (the big limit sign works with the compact pill too: with a limit to show, the big layout takes over)
    val bigSign = engine.prefs.b(PreferenceKeys.SPEED_SIGN_BIG, false) && (engine.speedLimit != null || engine.noLimit)
    if (engine.isCar && engine.prefs.s(PreferenceKeys.CAR_SPEED_STYLE, "normal") == "compact" && !bigSign) { CompactSpeed(engine); return }
    // the size from Settings (100% = the standard size): it's laid out at that size, so it keeps clear of the edges and other cards
    val pct = engine.prefs.i(PreferenceKeys.SPEED_SIZE, 100).coerceIn(50, 200)
    if (pct == 100) { SpeedBlock(engine); return }
    val base = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(base.density * pct / 100f, base.fontScale),
    ) { SpeedBlock(engine) }
}

@Composable
private fun CompactSpeed(engine: NavEngine) {
    val show = engine.prefs.b(PreferenceKeys.MAPSPEED, true)
    val lim = engine.speedLimit
    val sign = lim != null || engine.noLimit
    if (!sign && !show) return
    val k = if (engine.imperial) 2.23694 else 3.6
    // the day and night colours of the car screen (the panel was always black)
    val c = LocalVw.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.surface).padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (sign) SpeedLimitSign(engine, 44.dp)
        if (show) Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${Math.round(engine.shownSpeed * k)}", color = speedTint(engine) ?: c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 12.sp, maxLines = 1, modifier = Modifier.padding(bottom = 4.dp))
        }
    }
}

@Composable
private fun SpeedBlock(engine: NavEngine) {
    val c = LocalVw.current
    val show = engine.prefs.b(PreferenceKeys.MAPSPEED, true)
    val lim = engine.speedLimit
    // a sign to show: a limit, or the "no limit" sign of an unrestricted Autobahn
    val sign = lim != null || engine.noLimit
    if (!sign && !show) return
    val k = if (engine.imperial) 2.23694 else 3.6
    // the big limit sign: the sign where the speedometer was, your speed small beside it (it turns amber and red as before)
    if (sign && engine.prefs.b(PreferenceKeys.SPEED_SIGN_BIG, false)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // the size comes from Settings › Android Auto › Sizes (100 % is the usual 120 dp)
            SpeedLimitSign(engine, 120.dp * (engine.prefs.i(PreferenceKeys.CAR_LIMIT_SIGN_SIZE, 100).coerceIn(70, 200) / 100f))
            if (show) Column(Modifier.panel(8.dp, RoundedCornerShape(18.dp), c.surface).padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${Math.round(engine.shownSpeed * k)}", color = speedTint(engine) ?: c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 10.sp)
            }
        }
        return
    }
    if (engine.prefs.b(PreferenceKeys.SPEEDRING, true)) { SpeedRing(engine, show, k); return }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Keep the speed cluster in the same screen position, but mirror the sign's
        // relationship to the speed readout for left-hand-drive layouts.
        // the sign goes on the side facing the middle of the screen, so it can't sit off the edge: right of the speed when the
        // speed is on the left, left of it when the speed is on the right (the car screen keeps its own order)
        val signFirst = !engine.speedLeft()
        if (show && signFirst && sign) SpeedLimitSign(engine, 70.dp)
        if (show) Column(
            Modifier.width(78.dp).panel(8.dp, RoundedCornerShape(24.dp), c.surface).padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${Math.round(engine.shownSpeed * k)}", color = speedTint(engine) ?: c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 12.sp)
            // the clock, for head units whose own status bar is covered (Settings › While driving › Clock on the map)
            if (engine.prefs.b(PreferenceKeys.CLOCK_ON_MAP, false)) Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
        if (sign && !(show && signFirst)) SpeedLimitSign(engine, 70.dp)
    }
}

/**
 * Speedometer ring: a glowing arc that fills towards the limit and warms to amber, then red, as you near or pass it,
 * with your speed in the middle and the limit sign tucked against it.
 */
@Composable
private fun SpeedRing(engine: NavEngine, show: Boolean, k: Double) {
    val c = LocalVw.current
    val lim = engine.speedLimit
    val sp = engine.shownSpeed
    // the ring is full at 20% over the limit; with no limit (an Autobahn) at 200 km/h, or 70 mph where miles are used
    val full = if (lim != null && lim > 0) lim * 1.2 else if (engine.imperial) 31.3 else 55.6
    val target = (sp / full).toFloat().coerceIn(0f, 1f)
    val frac by androidx.compose.animation.core.animateFloatAsState(target, androidx.compose.animation.core.tween(350), label = "ring")
    val ratio = if (lim != null && lim > 0 && !engine.limitEstimated) (sp / lim).toFloat() else 0f
    val col = when {
        // blue up to the limit (it used to turn amber and red from 90%, so 28 in a 30 zone looked like speeding);
        // amber just over it, red from 5% over
        engine.overLimit || ratio > 1.04f -> Bad
        ratio > 1.0f -> Warn
        else -> c.accent
    }
    val arcCol by androidx.compose.animation.animateColorAsState(col, androidx.compose.animation.core.tween(400), label = "ringCol")
    Box(contentAlignment = Alignment.Center) {
        if (show) Box(Modifier.size(104.dp).panel(10.dp, CircleShape, c.surface), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(8.dp)) {
                val sw = 7.dp.toPx()
                val inset = sw / 2 + 4.dp.toPx()
                val tl = Offset(inset, inset)
                val sz = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
                val cap = androidx.compose.ui.graphics.drawscope.Stroke(width = sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawArc(c.tile, 135f, 270f, false, tl, sz, style = cap)
                if (frac > 0.005f) {
                    // a soft glow under the arc, then the arc itself
                    drawArc(arcCol.copy(alpha = 0.28f), 135f, 270f * frac, false, tl, sz, style = androidx.compose.ui.graphics.drawscope.Stroke(width = sw * 2.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(arcCol, 135f, 270f * frac, false, tl, sz, style = cap)
                }
                // a tick where the limit sits on the ring
                if (lim != null && lim > 0) {
                    val ang = Math.toRadians(135.0 + 270.0 * (lim / full).coerceIn(0.0, 1.0))
                    val r0 = sz.width / 2 - sw; val r1 = sz.width / 2 + sw
                    val cx = size.width / 2; val cy = size.height / 2
                    drawLine(c.text.copy(alpha = 0.7f), Offset(cx + (kotlin.math.cos(ang) * r0).toFloat(), cy + (kotlin.math.sin(ang) * r0).toFloat()),
                        Offset(cx + (kotlin.math.cos(ang) * r1).toFloat(), cy + (kotlin.math.sin(ang) * r1).toFloat()), 2.5.dp.toPx())
                }
            }
            // the number sits exactly in the middle of the ring; the unit goes in the gap at the bottom of the arc
            val num = "${Math.round(sp * k)}"
            Text(num, color = speedTint(engine) ?: c.text, fontSize = if (num.length >= 3) 27.sp else 33.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                style = androidx.compose.ui.text.TextStyle(platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false), lineHeight = 33.sp,
                    lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both)))
            Text(if (engine.imperial) "mph" else "km/h", color = c.sub, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp))
        }
        // The limit sign goes on the speed's inner side, so it never runs off the screen: the left normally, the right when the
        // app is mirrored for left-hand drive (the speed is then on the left edge). Android Auto never mirrors: its speed is
        // always on the right.
        val signRight = engine.speedLeft()
        if (lim != null || engine.noLimit) Box(
            Modifier.align(if (!show) Alignment.Center else if (signRight) Alignment.TopEnd else Alignment.TopStart)
                // beside the ring's upper corner, just clear of it (raised over the top it covered the battery chip above and the ring)
                .offset(x = if (!show) 0.dp else if (signRight) 48.dp else (-48).dp, y = 0.dp)
        ) {
            // (on the car, a little bigger beside the speedometer: it's read at a glance)
            SpeedLimitSign(engine, if (show) (if (engine.isCar) (if (engine.carShort) 42.dp else 58.dp) else 50.dp) else 70.dp)
        }
    }
}

@Composable
internal fun ImmTurnCard(
    engine: NavEngine, t: com.headunit.launcher.TurnInfo, compact: Boolean = false,
    /** Android Auto: no written lane advice under the lane arrows (the arrows already say it). */
    car: Boolean = false,
    /** Android Auto with the turn still far off: just one line (arrow, distance, road), so more of the map shows. */
    slim: Boolean = false,
    /** Extra row along the bottom of the card (Android Auto puts the hazards ahead there). */
    footer: (@Composable () -> Unit)? = null,
) {
    // road-sign corners: the progress bar along the bottom reaches the very end of the card
    val shape = RoundedCornerShape(7.dp)
    // Settings › Advanced sizes: the arrow on its own, and the room above and below the writing (car or phone settings)
    val arrowK = engine.prefs.i(if (car) PreferenceKeys.CAR_TURN_ARROW_SIZE else PreferenceKeys.TURN_ARROW_SIZE, 100).coerceIn(70, 200) / 100f
    val padK = engine.prefs.i(if (car) PreferenceKeys.CAR_TURN_CARD_HEIGHT else PreferenceKeys.TURN_CARD_HEIGHT, 100).coerceIn(50, 200) / 100f
    val frac = ((450.0 - t.dist) / 450.0).coerceIn(0.0, 1.0).toFloat()
    // living card: it grows a touch and starts to glow as the turn comes up, and pulses in the last 80 m
    val alive = engine.prefs.b(PreferenceKeys.LIVECARD, true)
    val near = alive && t.dist < 300.0
    val grow by androidx.compose.animation.core.animateFloatAsState(if (near) 1f + 0.035f * frac else 1f, androidx.compose.animation.core.tween(400), label = "grow")
    val glow by androidx.compose.animation.core.animateFloatAsState(if (near) (0.25f + 0.55f * frac) else 0f, androidx.compose.animation.core.tween(500), label = "glow")
    // only runs in the last 80 m, so the screen isn't redrawn constantly the rest of the time
    // read only while drawing, so the pulse repaints the card without recomposing it 60 times a second
    val pulse = rememberPulse(alive && t.dist < 80.0, 650)
    val glowCol = Color(0xFF7FFFE6)
    // on a phone upright it grows evenly from the middle (from the left edge it ran off the right of the screen)
    val growOrigin = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 720) androidx.compose.ui.graphics.TransformOrigin(0.5f, 0f) else androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
    Column(
        Modifier.fillMaxWidth()
            .graphicsLayer { scaleX = grow; scaleY = grow; transformOrigin = growOrigin }
            .shadow((if (compact) 5f else 12f + 14f * glow).dp, shape, ambientColor = glowCol, spotColor = glowCol)
            .clip(shape)
            .background(androidx.compose.ui.graphics.lerp(Teal, Color(0xFF14C9A6), 0.35f * glow))
            .border(1.5.dp, Color.White.copy(alpha = 0.18f + 0.25f * glow), shape)
            .drawWithContent {
                drawContent()
                val pv = pulse.value
                if (pv > 0.01f) {
                    drawRect(Color.White.copy(alpha = 0.10f * pv))
                    val sw = (1.5f + 1.5f * pv) * density
                    drawRoundRect(Color.White.copy(alpha = 0.45f * glow * pv), topLeft = Offset(sw / 2, sw / 2),
                        size = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(sw))
                }
            }
            .animateContentSize()
            // tap for the list of every turn on the route
            .clickable { engine.itineraryOpen = true },
    ) {
        // (in the car as tall as Android Auto's own buttons beside it, "Beenden", with text to match)
        if (slim) Row(Modifier.padding(horizontal = if (car) 20.dp else 14.dp, vertical = (if (car) 14.dp else 9.dp) * padK), verticalAlignment = Alignment.CenterVertically) {
            ManeuverArrow(t.maneuver, Geo.maneuverAngle(t.maneuver), Modifier.size((if (car) 50.dp else 34.dp) * arrowK), roundaboutExit = t.exit, roundaboutClockwise = engine.leftTraffic, exitAngle = roundaboutTurn(t.rbIn, t.rbOut), otherExits = t.rbExits.map { roundaboutTurn(t.rbIn, it) })
            RollText(Geo.fmtDist(t.dist, engine.imperial), Color.White, if (car) 36.sp else 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = if (car) 14.dp else 10.dp))
            Text(t.street, color = Color.White, fontSize = if (car) 26.sp else 16.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
        } else {
        val thenIcon = t.thenManeuver != null && engine.prefs.s(PreferenceKeys.THEN_STYLE, "row") == "icon"
        // Android Auto: the full card as big as the slim one (text as large as the car's own "Beenden" button beside it)
        val distSp = if (car) 44.sp else if (compact) 28.sp else 32.sp
        val streetSp = if (car) 24.sp else if (compact) 16.sp else 17.sp
        val chipSp = if (car) 17.sp else 13.sp
        Row(Modifier.padding(start = if (car) 20.dp else if (compact) 12.dp else 18.dp, end = if (compact && !car) 12.dp else 16.dp, top = (if (car) 14.dp else if (compact) 9.dp else 12.dp) * padK, bottom = (if (car) 12.dp else if (compact) 8.dp else 10.dp) * padK), verticalAlignment = Alignment.CenterVertically) {
            ManeuverArrow(t.maneuver, Geo.maneuverAngle(t.maneuver), Modifier.size((if (car) 64.dp else if (compact) 42.dp else 50.dp) * arrowK), roundaboutExit = t.exit, roundaboutClockwise = engine.leftTraffic, exitAngle = roundaboutTurn(t.rbIn, t.rbOut), otherExits = t.rbExits.map { roundaboutTurn(t.rbIn, it) })
            Column(Modifier.padding(start = if (car) 16.dp else if (compact) 9.dp else 16.dp).weight(1f)) {
                RollText(Geo.fmtDist(t.dist, engine.imperial), Color.White, distSp, fontWeight = FontWeight.Bold)
                // (the tag of a motorway exit is "Ausfahrt 17", from Geo.stepTag; older builds wrote "Exit 17")
                val exitTag = t.tag.startsWith("Exit") || t.tag.startsWith("Ausfahrt ")
                val ukSign = engine.prefs.b(PreferenceKeys.UKSIGNS, true) && (t.sign.isNotBlank() && t.sign != t.street || exitTag)
                // a motorway junction's name takes the tag's place: "Frankfurter Kreuz", "Ausfahrt 17 Weiterstadt" (just the name when the sign
                // below already carries the exit number)
                val jctChip = if (t.junction.isBlank() || t.maneuver.uppercase().startsWith("ROUNDABOUT")) "" else if (ukSign && exitTag) t.junctionName else t.junction
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (jctChip.isNotBlank()) Text(jctChip, color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 190.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    else if (t.tag.isNotBlank() && !(ukSign && exitTag)) Text(t.tag, color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    // an exit number with no tag: the badge shows it (the blue exit strip below the card is gone)
                    else if (t.tag.isBlank() && t.exitNo.isNotBlank() && !ukSign) Text(Lang.t("Ausfahrt ${t.exitNo}", "Exit ${t.exitNo}"), color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                    if (!ukSign && t.sign.isNotBlank() && t.sign != t.street) {
                        // the places on the sign, in the colour of the road they're on: blue Autobahn, yellow Bundes-/Landes-/Kreisstraße, white otherwise
                        val sc = signColors(t.sign + " " + t.street, t.exitNo.isNotBlank() || exitTag)
                        Text(t.sign, color = sc.second, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp).clip(RoundedCornerShape(6.dp)).background(sc.first).padding(horizontal = 7.dp, vertical = 2.dp))
                    }
                    // a road number with a letter (B 44, E39) beside the name as a small badge; outside the UK (its road signs do that)
                    val refM = if (com.headunit.launcher.RoadRefs.country.uppercase() != "GB") roadBadgeRx.find(t.street) else null
                    val nameOnly = refM?.let { t.street.replaceRange(it.range, "").trim().trim(':', '-', ' ') }?.takeIf { it.isNotBlank() }
                    if (refM != null && nameOnly != null) {
                        Text(refM.value, color = Color.White, fontSize = chipSp, fontWeight = FontWeight.Bold, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 2.dp))
                        Text(nameOnly, color = Color.White, fontSize = streetSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else Text(t.street, color = Color.White, fontSize = streetSp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            // the turn straight after, as a small arrow (the "Then" line below is used instead when that style is chosen)
            if (thenIcon && t.thenManeuver != null) Column(Modifier.padding(start = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TealDeep), contentAlignment = Alignment.Center) {
                    ManeuverArrow(t.thenManeuver, Geo.maneuverAngle(t.thenManeuver), Modifier.size(28.dp), roundaboutExit = t.thenExit, roundaboutClockwise = engine.leftTraffic)
                }
                Text(Lang.t("dann", "then"), color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
        }
        // (the blue "AUSFAHRT 17" strip that was here is gone: the exit badge beside the road name already shows the number, and
        // the strip's arrows always pointed right, even for an exit on the left)
        val exitTag = t.tag.startsWith("Exit", ignoreCase = true) || t.tag.startsWith("Ausfahrt ")
        val signShown = engine.prefs.b(PreferenceKeys.UKSIGNS, true) && (t.sign.isNotBlank() && t.sign != t.street || exitTag)
        // A proper UK direction sign: blue for motorways, green for primary roads, white for local roads.
        // a UK road number in the street name shows as its road sign too, even when the route gives no separate sign
        if (signShown || (engine.prefs.b(PreferenceKeys.UKSIGNS, true) && hasUkRoadRef(t.street)))
            UkSign(if (signShown) t.sign else t.street, (t.exitNo.ifBlank { t.tag.removePrefix("Exit ").removePrefix("Ausfahrt ") }).takeIf { exitTag }.orEmpty(), t.street, Modifier.padding(start = if (compact) 12.dp else 16.dp, end = if (compact) 12.dp else 16.dp, bottom = if (compact) 5.dp else 12.dp))
        if (!compact && engine.prefs.b(PreferenceKeys.LANE_STRIP, true)) t.lanes?.let { ls ->
            // lane hero view: before a complex junction the lanes grow into a big picture with yours lit; simple ones keep the strip
            // (not in Android Auto: the big lane picture made the card grow to fill much of a car screen as soon as a junction came up)
            val shortScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 500
            val hero = !car && !shortScreen && engine.prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero" && ls.lanes.size >= 2 && t.dist < 900.0
            if (hero) LaneHero(ls, t.maneuver, if (car) null else t.laneAdvice)
            else Box(Modifier.fillMaxWidth().background(TealDeep).padding(vertical = 4.dp), contentAlignment = Alignment.Center) { LaneRow(ls, t.maneuver, t.dist < 400.0) }
        }
        val heroShown = !car && androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp >= 500 && engine.prefs.b(PreferenceKeys.LANE_STRIP, true) && engine.prefs.s(PreferenceKeys.LANE_STYLE, "hero") == "hero" && (t.lanes?.lanes?.size ?: 0) >= 2 && t.dist < 900.0
        if (!compact && !heroShown && !(car && t.lanes != null && engine.prefs.b(PreferenceKeys.LANE_STRIP, true))) t.laneAdvice?.let { Text(it, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().background(TealDeep).padding(horizontal = 18.dp, vertical = 6.dp)) }
        if (!compact && t.thenManeuver != null && !thenIcon) {
            Row(Modifier.fillMaxWidth().background(TealDeep).padding(horizontal = 18.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                ManeuverArrow(t.thenManeuver, Geo.maneuverAngle(t.thenManeuver), Modifier.size(22.dp), roundaboutExit = t.thenExit, roundaboutClockwise = engine.leftTraffic)
                // UK road numbers in the sentence show as their road signs (Settings › UK road signs)
                if (engine.prefs.b(PreferenceKeys.UKSIGNS, true) && !com.headunit.launcher.RoadRefs.country.equals("DE", true))
                    RoadTextLine(t.thenText, Color.White.copy(alpha = 0.92f), 14.sp, Modifier.padding(start = 10.dp))
                // (a long street name goes onto a second line before it is cut short; the car's line too)
                else Text(t.thenText, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp))
            }
        }
        } // (end of the full card)
        footer?.invoke()
        // the bar appears about 800 m before the turn and fills up to it (it used to sit empty for kilometres)
        // the bar sits a little way in from the edges, so the card's rounded corners don't hide its end
        if (t.dist < 800.0) {
            val barFrac = ((800.0 - t.dist) / 800.0).coerceIn(0.0, 1.0).toFloat()
            Box(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(Color.Black.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(barFrac).fillMaxHeight().background(Color.White.copy(alpha = 0.9f)))
            }
        }
    }
}

/** A road number with a letter in a street name: "B 44", "E39", "A5". Plain numbers (169) and house numbers are not matched. */
private val roadBadgeRx = Regex("\\b([A-Z]{1,2})\\s?([0-9]{1,4})\\b")
