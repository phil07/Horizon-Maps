package com.headunit.launcher.ui

import com.headunit.launcher.R
import com.headunit.launcher.Lang
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EvStation
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.TurnSlightRight
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Construction
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GpsOff
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.headunit.launcher.Geo
import com.headunit.launcher.NavEngine
import com.headunit.launcher.Place
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import com.headunit.launcher.PreferenceKeys

/** True while a route is being followed (not just previewed). */
private val NavEngine.guiding: Boolean get() = route != null && !previewing

/** The route bar is shown on the right while navigating (it can be switched off in Settings). */
val NavEngine.showRouteBar: Boolean get() = guiding && prefs.b(PreferenceKeys.ROUTEBAR, true)

/** Driving with no route: just the road, your speed and the alerts. */
val NavEngine.cruising: Boolean get() = route == null && !previewing && (fix?.speed ?: 0.0) > 4.0

private fun barIcon(kind: String): ImageVector = when (kind) {
    "camera" -> Icons.Rounded.CameraAlt
    "works" -> Icons.Rounded.Construction
    "incident" -> Icons.Rounded.PriorityHigh
    "services" -> Icons.Rounded.LocalCafe
    "avg" -> Icons.Rounded.Speed
    "stop" -> Icons.Rounded.Place
    "charge" -> Icons.Rounded.EvStation
    "zone" -> Icons.Rounded.Eco
    else -> Icons.Rounded.Flag
}

private fun barColor(kind: String): Color = when (kind) {
    "camera", "avg" -> Bad
    "works", "incident" -> Warn
    "services" -> Color(0xFF4C8DFF)
    "zone" -> Good
    "charge" -> Color(0xFF14B8A6)
    else -> Color(0xFF8E5CF0)
}

/**
 * The whole route as a strip above the trip card: traffic in amber and red, markers for cameras, incidents, services,
 * average speed checks, stops and zones, and an arrow that moves along it as you drive (start on the left, destination on the right).
 */
@Composable
fun RouteBar(engine: NavEngine, modifier: Modifier = Modifier, thin: Boolean = false) {
    val r = engine.route ?: return
    val c = LocalVw.current
    val len = max(1.0, r.len)
    val target = (engine.barProgress / len).coerceIn(0.0, 1.0).toFloat()
    // progress arrives about once a second; the arrow glides between updates (only the drawing is redone, nothing is recomposed)
    val frac by androidx.compose.animation.core.animateFloatAsState(target, androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing), label = "barArrow")
    val bins = 120
    val levels = remember(r, r.sev.size, engine.barEvents) {
        val out = IntArray(bins)
        for (i in r.sev.indices) {
            if (i + 1 >= r.cum.size) break
            val b = ((r.cum[i] / len) * bins).toInt().coerceIn(0, bins - 1)
            if (r.sev[i] > out[b]) out[b] = r.sev[i]
        }
        out
    }
    val accent = c.accent
    val theme = engine.map.theme
    val locator = remember(theme) { engine.map.locatorBitmap().asImageBitmap() }
    // [thin]: the slimmer bar on the car screen (smaller markers, a thinner track and a smaller arrow)
    val dot = if (thin) 16.dp else 20.dp
    BoxWithConstraints(modifier.height(if (thin) 34.dp else 48.dp)) {
        val w = maxWidth
        // the track, with the part already driven greyed out, and the arrow
        Canvas(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(if (thin) 18.dp else 26.dp)) {
            val th = (if (thin) 4.dp else 8.dp).toPx()
            val y0 = (size.height - th) / 2f
            val bw = size.width / bins
            drawRoundRect(Color.Black.copy(alpha = 0.45f), Offset(0f, y0 - 1.5f), Size(size.width, th + 3f), CornerRadius(th))
            for (b in 0 until bins) {
                val done = (b + 1f) / bins <= frac
                val col = when (levels[b]) { 2 -> Bad; 1 -> Warn; else -> accent }
                drawRect(if (done) Color(0xFF8A8F99) else col, Offset(b * bw, y0), Size(bw + 0.6f, th))
            }
            // the arrow: the map's own locator, shrunk and turned to point along the bar
            val sz = (if (thin) 22.dp else 32.dp).toPx()
            val ax = (frac * size.width).coerceIn(sz / 2f, size.width - sz / 2f)
            val cy = size.height / 2f
            rotate(90f, Offset(ax, cy)) {
                drawImage(
                    locator, dstOffset = androidx.compose.ui.unit.IntOffset((ax - sz / 2f).toInt(), (cy - sz / 2f).toInt()),
                    dstSize = androidx.compose.ui.unit.IntSize(sz.toInt(), sz.toInt()),
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.Medium,
                )
            }
        }
        // markers above the track (ones that would sit on top of each other are left out)
        var lastX = -100f
        for (e in engine.barEvents.sortedBy { it.off }) {
            val f = (e.off / len).coerceIn(0.0, 1.0).toFloat()
            val x = w.value * f
            if (x - lastX < 22f) continue
            lastX = x
            Box(
                Modifier.offset(x = (w * f - dot / 2).coerceIn(0.dp, w - dot), y = 0.dp).size(dot).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center,
            ) { Icon(barIcon(e.kind), null, tint = barColor(e.kind), modifier = Modifier.size(dot * 0.65f)) }
        }
        Box(
            Modifier.offset(x = w - dot, y = 0.dp).size(dot).shadow(3.dp, CircleShape).clip(CircleShape).background(Color.White),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Flag, null, tint = Color(0xFFEA4335), modifier = Modifier.size(dot * 0.65f)) }
    }
}

/** Card for an average speed check: your average so far against the limit, and how much is left. */
@Composable
fun AvgSpeedCard(engine: NavEngine, modifier: Modifier = Modifier) {
    val a = engine.avgInfo ?: return
    val c = LocalVw.current
    val k = if (engine.imperial) 2.23694 else 3.6
    val unit = if (engine.imperial) "mph" else "km/h"
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier.fillMaxWidth().panel(8.dp, shape, c.surface).border(2.dp, if (a.over) Bad else Color.Transparent, shape).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val lim = a.limitMs?.let { (Math.round(it * k / 5.0) * 5).toInt() }
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White).border(5.dp, Bad, CircleShape), contentAlignment = Alignment.Center) {
            Text(lim?.toString() ?: "?", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(if (a.ahead) Lang.t("ABSCHNITTSKONTROLLE VORAUS", "AVERAGE SPEED CHECK AHEAD") else Lang.t("ABSCHNITTSKONTROLLE", "AVERAGE SPEED CHECK"), color = Bad, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            if (a.ahead) Text(Lang.t("Beginnt in ${Geo.fmtDist(a.toStartM, engine.imperial)} · ${Geo.fmtDist(a.leftM, engine.imperial)} lang", "Starts in ${Geo.fmtDist(a.toStartM, engine.imperial)} · ${Geo.fmtDist(a.leftM, engine.imperial)} long"), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            else {
                Text(Lang.t("noch ${Geo.fmtDist(a.leftM, engine.imperial)}", "${Geo.fmtDist(a.leftM, engine.imperial)} left"), color = c.sub, fontSize = 13.sp)
                // what you can do from here and still come out under the limit (only worth saying once it differs from the limit)
                val safe = a.safeMs
                val lm = a.limitMs
                if (safe != null && lm != null) {
                    val sv = Math.floor(safe * k).toInt()
                    val lv = Math.round(lm * k).toInt()
                    when {
                        safe <= 0.5 || sv < lv - 25 -> Text(stringResource(R.string.ui_050), color = Bad, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        sv < lv -> Text(Lang.t("Bleib für den Rest unter $sv $unit", "Keep under $sv $unit for the rest"), color = Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        sv > lv -> Text(Lang.t("Bis ${minOf(sv, lv + 15)} $unit bleibst du darunter", "Up to ${minOf(sv, lv + 15)} $unit keeps you under"), color = c.sub, fontSize = 13.sp)
                        else -> {}
                    }
                }
            }
        }
        if (!a.ahead) Column(horizontalAlignment = Alignment.End) {
            Text("${Math.round(a.avgMs * k)}", color = if (a.over) Bad else Good, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(Lang.t("Ø $unit", "avg $unit"), color = c.sub, fontSize = 11.sp)
        }
    }
}

/** The next motorway services on the route, with the fuel price there when Fuel Finder is set up. */
@Composable
fun ServicesCard(engine: NavEngine, modifier: Modifier = Modifier) {
    val list = engine.servicesAhead
    if (list.isEmpty() || engine.avgInfo != null || !engine.prefs.b(PreferenceKeys.SERVICESCARD, true)) return
    val c = LocalVw.current
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().panel(6.dp, shape, c.surface).padding(horizontal = 14.dp, vertical = 8.dp)) {
        // only the next services (the one after it is shown once you've passed this one)
        list.take(1).forEachIndexed { i, s ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF4C8DFF)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalCafe, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(s.name, color = c.text, fontSize = if (i == 0) 15.sp else 13.sp, fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (s.fuel.isNotBlank()) Text(s.fuel, color = c.sub, fontSize = 11.sp)
                }
                Text(Geo.fmtDist(s.dist, engine.imperial), color = c.text, fontSize = if (i == 0) 15.sp else 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** About a mile from the end: car parks close to your destination, with how far each is to walk. Tap one to drive there instead. */
@Composable
fun ParkingCard(engine: NavEngine, modifier: Modifier = Modifier) {
    val list = engine.parkingNear
    if (list.isEmpty()) return
    val c = LocalVw.current
    val shape = RoundedCornerShape(20.dp)
    Column(modifier.fillMaxWidth().panel(6.dp, shape, c.surface).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (Lang.de) "Parken bei ${engine.dest?.name ?: "deinem Ziel"}" else "Parking near ${engine.dest?.name ?: "your destination"}", color = c.sub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Box(Modifier.size(32.dp).clip(CircleShape).clickable { engine.parkingNear = emptyList() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(16.dp))
            }
        }
        list.forEach { n ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { engine.parkAt(n) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF2E6FD8)), contentAlignment = Alignment.Center) {
                    Text("P", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
                Text(n.place.name, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 10.dp))
                // a walk at about 80 m a minute
                Text(maxOf(1, Math.round(n.dist / 80.0).toInt()).let { Lang.t("$it Min. zu Fuß", "$it min walk") }, color = c.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, end = 8.dp))
            }
        }
    }
}

/** Shown while the GPS has dropped out (a tunnel): the arrow is being kept moving along the route. */
@Composable
fun DeadReckoningChip(modifier: Modifier = Modifier) {
    // the same day/night colours as the map's other cards
    val c = navVw(LocalVw.current)
    Row(
        modifier.panel(6.dp, RoundedCornerShape(50), c.surface).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.GpsOff, null, tint = Warn, modifier = Modifier.size(18.dp))
        Text(stringResource(R.string.ui_035), color = c.text, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Driving with no route: the road you are on and which way you are heading. */
@Composable
fun CruisePill(engine: NavEngine, modifier: Modifier = Modifier) {
    val road = engine.roadNow
    val dir = com.headunit.launcher.RouteExtras.compass(engine.fix?.heading ?: 0.0)
    // light in the day, dark at night, like the map's other cards
    val c = navVw(LocalVw.current)
    Row(
        modifier.panel(6.dp, RoundedCornerShape(50), c.surface).border(1.dp, c.line, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Navigation, null, tint = c.accent, modifier = Modifier.size(18.dp))
        Text((if (road.isNotBlank()) "$road · " else "") + Lang.t("Richtung $dir", "Heading $dir"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Edit the stops on the way: add by searching, move them up or down, or remove them. */
@Composable
fun StopsDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    var stops by remember { mutableStateOf(engine.via) }
    var q by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Place>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(q) {
        if (q.trim().length < 3) { results = emptyList(); return@LaunchedEffect }
        delay(500)
        busy = true
        results = try { engine.findPlaces(q) } catch (e: Exception) { emptyList() }
        busy = false
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 620.dp).fillMaxWidth().heightIn(max = 560.dp).clip(RoundedCornerShape(26.dp)).background(c.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.ui_053), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(28.dp).clickable { onClose() })
            }
            // one tap, no typing: the best one along the route ahead is added straight away
            if (engine.dest != null) {
                Text(stringResource(R.string.ui_037), color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val ev = engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV"
                    for ((kind, name) in listOfNotNull(if (ev) null else "fuel" to stringResource(R.string.ui_084), "charging" to stringResource(R.string.ui_085), "coffee" to stringResource(R.string.ui_086), "food" to stringResource(R.string.ui_087), "services" to stringResource(R.string.ui_088), "toilets" to stringResource(R.string.ui_089)))
                        Pill(name, false, { engine.addBestStop(kind); onClose() })
                }
            }
            // your own places, added to the list below (then Update route)
            val mine = remember { listOfNotNull(engine.places.slot("home")?.let { Lang.t("Zuhause", "Home") to it }, engine.places.slot("work")?.let { Lang.t("Arbeit", "Work") to it }) +
                engine.places.favourites().take(4).map { "★ " + it.name to it } }
            if (mine.isNotEmpty()) {
                Text(stringResource(R.string.ui_068), color = c.sub, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((name, p) in mine) Pill(name, false, { stops = (stops + p).take(8) })
                }
            }
            TextField(
                value = q, onValueChange = { q = it }, singleLine = true,
                placeholder = { Text(stringResource(R.string.ui_043)) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.sub) },
                trailingIcon = { if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                shape = RoundedCornerShape(50),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = c.tile, unfocusedContainerColor = c.tile, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = c.text, unfocusedTextColor = c.text, cursorColor = c.accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (results.isNotEmpty()) LazyColumn(Modifier.heightIn(max = 200.dp).clip(RoundedCornerShape(16.dp)).background(c.tile)) {
                itemsIndexed(results) { _, p ->
                    Row(Modifier.fillMaxWidth().clickable { stops = (stops + p).take(8); q = ""; results = emptyList() }.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Add, null, tint = c.accent, modifier = Modifier.size(22.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(p.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 260.dp)) {
                itemsIndexed(stops) { i, p ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(30.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                            Text("${i + 1}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(p.name, color = c.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        Icon(Icons.Rounded.KeyboardArrowUp, Lang.t("Früher", "Earlier"), tint = if (i > 0) c.text else c.sub.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp).clickable(enabled = i > 0) { stops = stops.toMutableList().also { l -> val t = l[i]; l[i] = l[i - 1]; l[i - 1] = t } })
                        Icon(Icons.Rounded.KeyboardArrowDown, Lang.t("Später", "Later"), tint = if (i < stops.size - 1) c.text else c.sub.copy(alpha = 0.3f),
                            modifier = Modifier.size(36.dp).clickable(enabled = i < stops.size - 1) { stops = stops.toMutableList().also { l -> val t = l[i]; l[i] = l[i + 1]; l[i + 1] = t } })
                        Icon(Icons.Rounded.Close, Lang.t("Entfernen", "Remove"), tint = Bad, modifier = Modifier.padding(start = 6.dp).size(30.dp).clickable { stops = stops.filterIndexed { n, _ -> n != i } })
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(0xFFEA4335)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Flag, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(engine.dest?.name ?: Lang.t("Ziel", "Destination"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 12.dp))
            }
            if (stops.isEmpty()) Text(stringResource(R.string.ui_036), color = c.sub, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
                Pill(stringResource(R.string.ui_097), true, { engine.setStops(stops); onClose() })
                Pill(Lang.t("Abbrechen", "Cancel"), false, onClose)
            }
        }
    }
}

/** Route options row: the clean air / low emission zones this route goes into. */
@Composable
fun ZonesRow(engine: NavEngine) {
    val z = engine.routeZones
    if (z.isEmpty()) return
    val c = LocalVw.current
    Row(
        Modifier.floating(50.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Eco, null, tint = Warn, modifier = Modifier.size(20.dp))
        val cs = engine.routeZoneCountries
        val note = when {
            cs == setOf("DE") -> Lang.t(". Nur mit grüner Plakette.", ". Green sticker required.")
            cs == setOf("GB") -> Lang.t(". Es kann eine Gebühr anfallen.", ". A charge may apply.")
            cs == setOf("FR") -> Lang.t(". Crit'Air-Vignette nötig.", ". Crit'Air sticker required.")
            else -> Lang.t(". Regeln vor Ort beachten.", ". Check the local rules.")
        }
        Text(Lang.t("Führt durch ", "Goes into ") + z.joinToString(", ") + note, color = c.text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (!engine.prefs.b(PreferenceKeys.AVOIDZONES, false)) Pill(Lang.t("Zonen meiden", "Avoid zones"), false, { engine.prefs.put(PreferenceKeys.AVOIDZONES, true); engine.dest?.let { engine.startRoute(it, engine.departMin, true) } })
    }
}

// ------------------------------------------------------------------ night sky

/**
 * At night in 3D, a soft sky along the top of the map: a deep blue fade with stars that twinkle slowly. It only redraws twice a second.
 */
@Composable
fun NightSky(modifier: Modifier = Modifier) {
    val stars = remember { List(46) { floatArrayOf(Math.random().toFloat(), (Math.random() * Math.random()).toFloat(), 0.4f + Math.random().toFloat() * 0.6f, Math.random().toFloat() * 6.28f) } }
    var tick by remember { mutableStateOf(0) }
    // Two updates a second are enough for a subtle twinkle and keep extra map redraws low on slower head units.
    LaunchedEffect(Unit) { while (true) { delay(500); tick += 2 } }
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawRect(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xE6070B1F), Color(0x990B1433), Color.Transparent)), Offset.Zero, size)
        val t = tick * 0.125f
        for (s in stars) {
            val a = (0.35f + 0.45f * s[2] * (0.6f + 0.4f * kotlin.math.sin(t * 0.9f + s[3]))).coerceIn(0f, 1f) * (1f - s[1])
            drawCircle(Color.White.copy(alpha = a), (0.8f + 1.4f * s[2]) * density, Offset(s[0] * w, s[1] * h * 0.8f))
        }
    }
}

// ------------------------------------------------------------------ arrive by

/** Pick the time to arrive by; the app works out when to leave and reminds you. */
@Composable
fun ArriveByDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val cal = remember {
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.MINUTE, 60 + (engine.route?.time ?: 0.0).toInt() / 60)
            set(java.util.Calendar.MINUTE, (get(java.util.Calendar.MINUTE) / 15) * 15)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
    }
    var ms by remember { mutableStateOf(cal.timeInMillis) }
    val f = java.text.SimpleDateFormat("EEE HH:mm", java.util.Locale.UK)
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.ui_005), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(engine.dest?.name ?: "", color = c.sub, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Pill("−15", false, { ms -= 15 * 60_000L })
                Text(f.format(java.util.Date(ms)), color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                Pill("+15", false, { ms += 15 * 60_000L })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("+1 h", false, { ms += 3_600_000L })
                Pill("−1 h", false, { ms -= 3_600_000L })
                Pill(Lang.t("Morgen 9:00", "Tomorrow 9:00"), false, {
                    ms = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1); set(java.util.Calendar.HOUR_OF_DAY, 9); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }.timeInMillis
                })
            }
            Text(stringResource(R.string.ui_027), color = c.sub, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(Lang.t("Planen", "Plan it"), true, { if (ms > System.currentTimeMillis()) { engine.planArriveBy(ms); onClose() } })
                Pill(Lang.t("Abbrechen", "Cancel"), false, onClose)
            }
        }
    }
}

/** On the map while idle: the planned trip with its leave time, to start it or cancel it. */
@Composable
fun PlannedCard(engine: NavEngine) {
    val p = engine.planned ?: return
    val c = LocalVw.current
    val f = java.text.SimpleDateFormat("HH:mm", java.util.Locale.UK)
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(p) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val mins = ((p.leaveBy - now) / 60_000L).toInt()
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().panel(8.dp, shape, if (mins <= 10) c.accent else c.surface).clickable { engine.startPlanned() }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (mins <= 10) Color.White else c.text
        Column(Modifier.weight(1f)) {
            Text(if (mins <= 0) Lang.t("Zeit zum Losfahren nach ${p.dest.name}", "Time to leave for ${p.dest.name}") else Lang.t("Losfahren bis ${f.format(java.util.Date(p.leaveBy))} nach ${p.dest.name}", "Leave by ${f.format(java.util.Date(p.leaveBy))} for ${p.dest.name}"), color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(Lang.t("Ankunft bis ${f.format(java.util.Date(p.arriveBy))}", "Arrive by ${f.format(java.util.Date(p.arriveBy))}") + (if (mins in 1..120) Lang.t(" · in $mins Min.", " · in $mins min") else ""), color = if (mins <= 10) Color.White.copy(alpha = 0.85f) else c.sub, fontSize = 12.sp)
        }
        Text(stringResource(R.string.ui_019), color = fg, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp))
        Icon(Icons.Rounded.Close, Lang.t("Abbrechen", "Cancel"), tint = fg, modifier = Modifier.size(24.dp).clickable { engine.cancelPlanned() })
    }
}

/** Electric trip: the charging stops it needs, with the charge on arrival and the time to charge, or a warning if it can't be done. */
@Composable
fun EvStopsRow(engine: NavEngine, planned: List<com.headunit.launcher.EvStop>? = null) {
    // with [planned]: just list those stops (already on the route), without the "add stops" button or the warning
    val stops = planned ?: engine.evStops; val warn = if (planned != null) null else engine.evWarning
    if (stops.isEmpty() && warn == null) return
    val c = LocalVw.current
    val many = if (stops.size > 1) "s" else ""
    Row(
        Modifier.floating(50.dp).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f, fill = false).widthIn(max = 460.dp)) {
            if (stops.isEmpty()) Text(warn ?: "", color = Bad, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            else {
                Text(if (planned != null) Lang.t("${stops.size} Ladestopp$many geplant", "${stops.size} charging stop$many planned") else Lang.t("${stops.size} Ladestopp$many nötig", "${stops.size} charging stop$many needed"), color = Good, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                // up to two stops are simply listed; from three on the list is as tall as the first two (plus a sliver of
                // the third, so it shows there is more) and scrolls by itself
                val scrollList = stops.size >= 3
                val density = androidx.compose.ui.platform.LocalDensity.current
                // (not keyed on the stops: a new list with the same rows would empty it, and onSizeChanged does not fire again for
                // rows whose size didn't change, which left the list stuck at the 80 dp fallback; the heights only steer the cap)
                val itemH = remember { androidx.compose.runtime.mutableStateMapOf<Int, Int>() }
                val twoPx = (itemH[0] ?: 0) + (itemH[1] ?: 0)
                val listCap = if (twoPx > 0) with(density) { twoPx.toDp() } + 10.dp else 80.dp
                Column(if (scrollList) Modifier.heightIn(max = listCap).verticalScroll(rememberScrollState()) else Modifier) {
                    stops.forEachIndexed { i, s ->
                        // the network first (unless the name already says it), and a star for one of the operators preferred in Settings
                        val op = com.headunit.launcher.ChargeOperators.label(s.n)?.takeUnless { s.n.place.name.contains(it, ignoreCase = true) }
                        Text(
                            "${i + 1}. " + (if (s.preferred) "★ " else "") + (op?.let { "$it · " } ?: "") + "${s.n.place.name} · ${Geo.fmtDist(s.n.dist, engine.imperial)} · " + (s.n.kw?.let { "$it kW · " } ?: "") +
                                Lang.t("Ankunft ${s.arrPct} %, laden auf ${s.toPct} % (${s.minutes} Min.)", "arrive ${s.arrPct}%, charge to ${s.toPct}% (${s.minutes} min)"),
                            color = c.text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            modifier = if (scrollList && i < 2) Modifier.onSizeChanged { if (itemH[i] != it.height) itemH[i] = it.height } else Modifier,
                        )
                    }
                }
                if (warn != null) Text(warn, color = Bad, fontSize = 12.sp)
            }
        }
        if (planned == null && stops.isNotEmpty()) Pill(Lang.t("Stopps hinzufügen", "Add stops"), false, { engine.addStops(stops) })
    }
}

/** Route options row: a good place to fill up on a long trip. */
@Composable
fun FuelSuggestionRow(engine: NavEngine) {
    if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") { EvStopsRow(engine); return }
    val n = engine.fuelSuggestion ?: return
    val c = LocalVw.current
    Row(
        Modifier.floating(50.dp).padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") stringResource(R.string.ui_090) else stringResource(R.string.ui_091), color = Good, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            n.place.name + " · " + Geo.fmtDist(n.dist, engine.imperial) + Lang.t(" entfernt", " in") + (if (n.price != null) " · " + com.headunit.launcher.fmtFuelPrice(n.price, n.unit) else "") + (if (n.detourMin >= 0) Lang.t(" · +${n.detourMin} Min.", " · +${n.detourMin} min") else "") + (if (n.extra.startsWith("arrive")) " · " + n.extra else ""),
            color = c.text, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 420.dp),
        )
        Pill(Lang.t("Stopp hinzufügen", "Add stop"), false, { engine.addStop(n.place); engine.fuelSuggestion = null })
    }
}

// ------------------------------------------------------------------ UK road signs

private val ukRefRx = Regex("\\b([MAB][0-9]{1,4})(\\s?\\(M\\))?\\b")
// German numbers, with or without the space: "A 5", "A5", "B 27"
private val deRefRx = Regex("\\b([AB])\\s?([0-9]{1,3})\\b")

/**
 * The direction sign for the junction, drawn the way UK gantries look: blue with white text for motorways,
 * green with yellow road numbers for primary roads, white with black text for local roads. The junction number
 * sits in its own black box, and road numbers of a different class get their own coloured panel.
 * In Germany (the country you're in) the German colours: blue with white text for the Autobahn, yellow with black text for
 * a Bundesstraße, and the numbers written "A 5", "B 27".
 */
@Composable
fun UkSign(sign: String, junction: String, street: String, modifier: Modifier = Modifier) {
    val text = sign.ifBlank { street }
    val de = com.headunit.launcher.RoadRefs.country.uppercase() == "DE"
    val rx = if (de) deRefRx else ukRefRx
    val refs = if (de) deRefRx.findAll(text).map { it.groupValues[1] + " " + it.groupValues[2] }.distinct().toList()
        else ukRefRx.findAll(text).map { it.groupValues[1] + (if (it.groupValues[2].isNotBlank()) " (M)" else "") }.distinct().toList()
    // (Mapbox writes "M27: Salisbury, Ringwood": the colon after the road number is split off too, so it never shows as ": Salisbury")
    val dests = text.split(',', '/', ';', '|', ':').map { rx.replace(it, "").replace(Regex("[()]"), "").trim().trim(':', '-', '–', ' ') }.filter { it.length >= 3 && it.any { ch -> ch.isLetter() } }.distinct().take(3)
    // the motorway class (M roads / Autobahn) and the primary class (A roads / Bundesstraßen)
    fun isMot(r: String) = if (de) r.startsWith("A") else r.startsWith("M") || r.endsWith("(M)")
    fun isPri(r: String) = !isMot(r) && r.startsWith(if (de) "B" else "A")
    val motorway = refs.any { isMot(it) } || (junction.isNotBlank() && refs.none { isPri(it) || it.startsWith("B") })
    val primary = !motorway && refs.any { isPri(it) }
    val motBg = Color(0xFF0065B3)
    val priBg = if (de) Color(0xFFFFD200) else Color(0xFF00703C)
    val priNum = if (de) Color(0xFF111111) else Color(0xFFFFD200)   // the numbers on a primary sign: black (DE), yellow (UK)
    val bg = when { motorway -> motBg; primary -> priBg; else -> Color.White }
    val fg = if (motorway || (primary && !de)) Color.White else Color(0xFF111111)
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier.clip(shape).background(bg).border(2.dp, if (motorway || (primary && !de)) Color.White else Color(0xFF111111), shape).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (junction.isNotBlank()) Text(junction, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1,
            modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color.Black).padding(horizontal = 8.dp, vertical = 2.dp))
        Column(Modifier.weight(1f, fill = false)) {
            if (dests.isNotEmpty()) dests.forEach { Text(it, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            else if (refs.isEmpty()) Text(text, color = fg, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        refs.take(2).forEach { r ->
            val isM = isMot(r)
            val isA = isPri(r)
            when {
                // a road of the sign's own class: plain (yellow numbers on green UK signs, black on yellow German ones)
                isM && motorway -> Text(r, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                isA && primary -> Text(r, color = priNum, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                // a different class gets its own panel
                isM -> Text(r, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(motBg).padding(horizontal = 6.dp, vertical = 1.dp))
                isA -> Text(r, color = priNum, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(priBg).padding(horizontal = 6.dp, vertical = 1.dp))
                else -> Text("($r)", color = fg, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

// ------------------------------------------------------------------ hazard radar

private fun radarColor(kind: String): Color = when (kind) {
    "camera", "avg" -> Bad
    "incident" -> Color(0xFFFF7A45)
    "works" -> Warn
    "school", "crossing" -> Color(0xFF4DA3FF)
    "bend" -> Color(0xFFB4BCD0)
    else -> Warn
}

private fun radarIcon(kind: String): ImageVector = when (kind) {
    "camera" -> Icons.Rounded.CameraAlt
    "avg" -> Icons.Rounded.Speed
    "works" -> Icons.Rounded.Construction
    "incident" -> Icons.Rounded.PriorityHigh
    "school" -> Icons.Rounded.School
    "crossing" -> Icons.Rounded.DirectionsWalk
    "bend" -> Icons.Rounded.TurnSlightRight
    else -> Icons.Rounded.Warning
}

/**
 * Hazard radar: the next two miles as a strip, you on the left, with bends, crossings, schools, bumps, cameras and checks
 * placed where they are, and the nearest one spelled out.
 */
@Composable
fun HazardRadar(engine: NavEngine, modifier: Modifier = Modifier) {
    val items = engine.radar
    if (items.isEmpty()) return
    val c = LocalVw.current
    val reach = 3219.0
    val near = items.first()
    val shape = RoundedCornerShape(22.dp)
    Column(modifier.widthIn(max = 540.dp).fillMaxWidth().panel(8.dp, shape, c.surface).padding(start = 16.dp, end = 16.dp, top = 9.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(radarIcon(near.kind), null, tint = radarColor(near.kind), modifier = Modifier.size(20.dp).graphicsFlip(near.kind == "bend" && (near.label.endsWith("left") || near.label.startsWith("Links"))))
            Text(near.label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(" · " + (if (near.dist < 20) Lang.t("jetzt", "now") else Lang.t("in ", "in ") + Geo.fmtDist(near.dist, engine.imperial)), color = c.sub, fontSize = 14.sp, maxLines = 1)
            Box(Modifier.weight(1f))
            Text(if (engine.imperial) Lang.t("nächste 2 mi", "next 2 mi") else Lang.t("nächste 3 km", "next 3 km"), color = c.sub, fontSize = 11.sp)
        }
        BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 8.dp).height(30.dp)) {
            val w = maxWidth
            Box(Modifier.align(Alignment.CenterStart).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.tile))
            // you
            Box(Modifier.align(Alignment.CenterStart).size(12.dp).clip(CircleShape).background(c.accent))
            // furthest first so the nearest sit on top
            for (it in items.asReversed()) {
                val f = (it.dist / reach).toFloat().coerceIn(0f, 1f)
                val x = (w - 26.dp) * f + 4.dp
                Box(Modifier.offset(x = x).align(Alignment.CenterStart).size(26.dp).clip(CircleShape).background(c.surface2).border(2.dp, radarColor(it.kind), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(radarIcon(it.kind), null, tint = radarColor(it.kind), modifier = Modifier.size(15.dp).graphicsFlip(it.kind == "bend" && (it.label.endsWith("left") || it.label.startsWith("Links"))))
                }
            }
        }
    }
}

/** Mirrors an icon left-to-right (used for left-hand bends). */
private fun Modifier.graphicsFlip(on: Boolean): Modifier = if (on) this.then(Modifier.scale(-1f, 1f)) else this


// ------------------------------------------------------------------ offline pack age

/** Newer map data is out for a downloaded area (a rebuilt pack, or new speed limit / camera files): Update or Later. */
@Composable
fun DataUpdateCard(engine: NavEngine) {
    val id = engine.dataUpdates.firstOrNull() ?: return
    if (engine.packBusy) return
    val c = LocalVw.current
    val name = com.headunit.launcher.PackRegions.byId(id).name
    val wifi = remember(id) { engine.onWifi() }
    val more = engine.dataUpdates.size - 1
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().panel(8.dp, shape, c.surface).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Neue Kartendaten für $name", "New map data for $name") + if (more > 0) Lang.t(" (+$more weitere)", " (+$more more)") else "", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (wifi) Lang.t("Nur Änderungen werden geladen", "Only what's changed downloads") else Lang.t("Nur Änderungen werden geladen · am besten im WLAN", "Only what's changed downloads · best on Wi-Fi"), color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Pill(Lang.t("Aktualisieren", "Update"), true, { engine.updateData(id) })
        Pill(Lang.t("Später", "Later"), false, { engine.snoozeDataUpdates() })
    }
}

/** A gentle note when the offline pack is about four months old or more, with Update (best on Wi-Fi) and Later. */
@Composable
fun PackAgeCard(engine: NavEngine) {
    val rev = engine.packRev
    var hidden by remember { mutableStateOf(false) }
    // file checks happen once (and again after a download), not on every redraw
    val age = remember(rev) { if (engine.packNeedsUpdate()) engine.packAgeDays() else null }
    if (age == null || hidden || engine.packBusy) return
    val c = LocalVw.current
    val wifi = remember(rev) { engine.onWifi() }
    val months = (age / 30).coerceAtLeast(4)
    val shape = RoundedCornerShape(22.dp)
    Row(
        Modifier.fillMaxWidth().panel(8.dp, shape, c.surface).padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Die Offline-Karte ist $months Monate alt", "Offline map is $months months old"), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(if (wifi) Lang.t("Jetzt aktualisieren für neue Straßen und Kreuzungen", "Update it now for new roads and junctions") else Lang.t("Im WLAN aktualisieren für neue Straßen und Kreuzungen", "Update on Wi-Fi for new roads and junctions"), color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (wifi) Pill(Lang.t("Aktualisieren", "Update"), true, { engine.updatePack(); hidden = true })
        Pill(Lang.t("Später", "Later"), false, { engine.snoozePackUpdate(); hidden = true })
    }
}

/** A newer version of the app is out (shareable build only): Update downloads it and opens the installer; Later hides it for a few days. */
@Composable
fun AppUpdateCard(engine: NavEngine) {
    val u = engine.appUpdate ?: return
    val c = LocalVw.current
    val shape = RoundedCornerShape(22.dp)
    // text on top and the buttons in their own row underneath: side by side, a narrow phone squeezed the title to a few letters a line
    Column(
        Modifier.fillMaxWidth().panel(8.dp, shape, c.surface).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.ui_025), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(Lang.t("Installiert ${com.headunit.launcher.BuildConfig.VERSION_NAME}  →  Verfügbar ${u.version}", "Installed ${com.headunit.launcher.BuildConfig.VERSION_NAME}  →  Available ${u.version}"), color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (engine.updateBusy || engine.updateStatus.isNotBlank()) engine.updateStatus else Lang.t("Jetzt aktualisieren: Einstellungen und Orte bleiben erhalten", "Update now: your settings and places are kept"),
                color = c.sub, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (u.releaseNotes.isNotBlank() && !engine.updateBusy) {
                Text(plainNotes(u.releaseNotes), color = c.sub, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            // Horizon Nav isn't on Google Play, so "installed by Google Play" means a tool like AAEnabler installed it to get it into
            // Android Auto. Android Auto drops apps that update themselves, so say so before they find it missing in the car.
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val viaAaTool = remember { com.headunit.launcher.Updater.installedViaAaTool(ctx) }
            if (viaAaTool && !engine.updateBusy && engine.updateStatus.isBlank()) Text(
                if (android.os.Build.VERSION.SDK_INT >= 29) Lang.t("Mit AAEnabler installiert: Aktualisieren speichert die neue Version in Downloads, damit du sie mit AAEnabler installieren kannst und sie in Android Auto bleibt.", "Installed with AAEnabler: Update saves the new version to your Downloads, so you can install it with AAEnabler and keep it in Android Auto.")
                else Lang.t("Mit AAEnabler (o. Ä.) installiert? Ein Update hier nimmt die App aus Android Auto: Installiere die neue Version danach wieder mit AAEnabler.", "Installed with AAEnabler (or similar)? Updating here takes it out of Android Auto: install the new version with AAEnabler again afterwards."),
                color = Warn, fontSize = 11.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (!engine.updateBusy) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
            Pill(Lang.t("Später", "Later"), false, { engine.snoozeAppUpdate() })
            Pill(Lang.t("Aktualisieren", "Update"), true, { engine.installAppUpdate() })
        }
    }
}

/** Release notes are Markdown ("## Horizon Nav 1.0.1055", "- **Neu:** …"): shown as plain text, the version heading left out. */
private fun plainNotes(md: String): String = md.lines()
    .map { it.trim() }
    .filter { it.isNotEmpty() && !it.startsWith("#") }
    .joinToString(" ") { l -> l.removePrefix("- ").removePrefix("* ").replace("**", "").replace("`", "") }

// ------------------------------------------------------------------ your own saved places

/**
 * Save a place under your own name. With [preset] (e.g. the destination on the route options) only the name is asked for;
 * otherwise you search for the address too. Saved places show as pills under the search bar, after Home and Work.
 */
@Composable
fun AddPlaceDialog(engine: NavEngine, preset: Place?, slot: String? = null, onClose: () -> Unit) {
    val c = LocalVw.current
    var label by remember { mutableStateOf("") }
    var q by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(preset) }
    // its own results: the main search's results opened the main search panel behind it, which closed this dialog
    var found by remember { mutableStateOf<List<Place>>(emptyList()) }
    LaunchedEffect(q) {
        if (preset == null && q.length >= 3) { delay(450); found = try { engine.findPlaces(q) } catch (e: kotlinx.coroutines.CancellationException) { throw e } catch (e: Exception) { emptyList() } }
        else if (q.isEmpty()) found = emptyList()
    }
    fun close() { onClose() }
    Dialog(onDismissRequest = { close() }) {
        Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).border(1.dp, c.line, RoundedCornerShape(26.dp)).padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // [slot] "home" or "work": setting Home or Work from Settings (no name to type, it's saved straight into that slot)
            Text(if (slot == "home") Lang.t("Zuhause festlegen", "Set Home") else if (slot == "work") Lang.t("Arbeit festlegen", "Set Work") else if (preset != null) Lang.t("Diesen Ort speichern", "Save this place") else Lang.t("Ort hinzufügen", "Add a place"), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (slot == null) SField(label, { label = it.take(24) }, placeholder = Lang.t("Name, z. B. Mama, Fitnessstudio, Büro 2", "Name, e.g. Mum's, Gym, Office 2"), width = 476.dp, fill = true)
            val pk = picked
            if (pk != null) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.tile).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Place, null, tint = c.accent, modifier = Modifier.size(22.dp))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(pk.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (pk.sub.isNotBlank()) Text(pk.sub, color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (preset == null) Text(stringResource(R.string.ui_008), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { picked = null; q = "" })
            } else {
                SField(q, { q = it }, placeholder = Lang.t("Adresse oder Postleitzahl suchen", "Search for the address or postcode"), width = 476.dp, fill = true)
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    itemsIndexed(found.take(6)) { _, p ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                            picked = p; found = emptyList(); if (label.isBlank()) label = p.name.take(24)
                        }.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Text(p.name, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(Lang.t("Speichern", "Save"), pk != null, {
                    val p = picked
                    if (p != null && slot != null) { engine.places.setSlot(slot, p); engine.toast(if (slot == "home") Lang.t("Zuhause gespeichert", "Home saved") else Lang.t("Arbeit gespeichert", "Work saved"), 2500); close() }
                    else if (p != null) { engine.places.addCustom(label, p); engine.toast(engine.str(R.string.rest_1371, label.trim().ifBlank { p.name }), 2500); close() }
                })
                Pill(Lang.t("Abbrechen", "Cancel"), false, { close() })
            }
        }
    }
}

/**
 * Shown once after updating from a Horizon Nav from before the Mapbox map: what the Mapbox key adds, and a box to paste it in
 * (filled in already if one was saved before). Skipping it is fine: it can be added later in Settings › Keys & accounts.
 */
@Composable
fun MapboxKeyDialog(prefs: com.headunit.launcher.Prefs, refused: Boolean = false, why: String = "", onClose: () -> Unit) {
    val c = LocalVw.current
    var k by remember { mutableStateOf(prefs.s(PreferenceKeys.MAPBOX_TOKEN, "")) }
    Dialog(onDismissRequest = {}) {
        Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.panel).padding(22.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (refused) Lang.t("Mapbox hat deinen Schlüssel abgelehnt", "Mapbox turned down your key") else Lang.t("Mapbox-Schlüssel eintragen", "Add your Mapbox key"), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (refused) Text(why.ifBlank { Lang.t("Mapbox hat deinen Schlüssel mit „401“ abgelehnt, darum lädt die Karte nicht. Erstelle einen neuen auf account.mapbox.com › Tokens › ", "Mapbox answered \"401\" for the key you saved, so the map can't load. Make a fresh one at account.mapbox.com › Tokens › ") +
                Lang.t("Create a token: Nenne ihn Horizon Nav, lass die öffentlichen Berechtigungen angehakt und die geheimen nicht, und füge ihn unten ein.", "Create a token: name it Horizon Nav, leave the public scopes ticked and the secret ones unticked, then paste it below.") }, color = Warn, fontSize = 14.sp)
            Text(Lang.t("Horizon Nav zeichnet die Karte mit Mapbox. Dein kostenloser Mapbox-Schlüssel bringt die 3D-Karte mit Gebäuden, Bäumen und Fahrbahnmarkierungen, ", "Horizon Nav now draws its map with Mapbox. Your free Mapbox key adds the 3D map with buildings, trees and lane markings, ") +
                Lang.t("Live-Verkehr auf der Route sowie bessere Suche und Tempolimits.", "live traffic on your route, and better search and speed limits."), color = c.text, fontSize = 15.sp)
            Text(stringResource(R.string.ui_018), color = c.sub, fontSize = 13.sp)
            SField(k, { k = it }, placeholder = Lang.t("Mapbox-Zugangsschlüssel (pk.…)", "Mapbox access token (pk.…)"), width = 500.dp, fill = true)
            val t = com.headunit.launcher.MapboxRest.clean(k)
            when {
                t.startsWith("sk.") -> Text(stringResource(R.string.ui_057), color = Warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                t.isNotEmpty() && !com.headunit.launcher.MapboxRest.looksValid(t) -> Text(stringResource(R.string.ui_056), color = Warn, fontSize = 13.sp)
                t.isNotEmpty() && !com.headunit.launcher.MapboxRest.looksWhole(t) -> Text(Lang.t("Ein Teil dieses Schlüssels scheint zu fehlen oder vertippt zu sein. Kopiere ihn noch einmal mit dem Kopieren-Knopf auf account.mapbox.com › Tokens.", "Part of that token looks missing or mistyped. Copy it again with the copy button at account.mapbox.com › Tokens."), color = Warn, fontSize = 13.sp)
                t.isEmpty() -> Text(stringResource(R.string.ui_066), color = c.sub, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(Lang.t("Speichern", "Save"), true, { if (t.isNotEmpty() && !t.startsWith("sk.")) { prefs.put(PreferenceKeys.MAPBOX_TOKEN, t); onClose() } })
                Pill(Lang.t("Später", "Later"), false, onClose)
            }
        }
    }
}

/**
 * A UK-style road sign for the junction coming up, from the route's sign data: blue for motorways (with the black junction-number
 * box), green for main roads (yellow road numbers), the roads it leads to and the places on the sign. Only when the route has them.
 */
@Composable
fun JunctionSign(t: com.headunit.launcher.TurnInfo, modifier: Modifier = Modifier) {
    if (t.sign.isBlank() || t.dist > 3200.0) return
    val refRx = Regex("\\b([MAB][0-9]{1,4}(?:\\(M\\))?)\\b")
    val refs = refRx.findAll(t.sign).map { it.groupValues[1] }.distinct().take(2).toList()
    val places = refRx.replace(t.sign, " ").split(',', ';', ':', '/').map { it.trim().trim('-', '–').trim() }
        .filter { it.length > 1 && !it.equals("J", true) && !Regex("^J?[0-9]+[A-Z]?$").matches(it) }.distinct().take(3)
    if (refs.isEmpty() && places.isEmpty()) return
    val motorway = t.exitNo.isNotBlank() || refs.any { it.startsWith("M") || it.endsWith("(M)") }
    val bg = if (motorway) Color(0xFF1554A8) else Color(0xFF0D6B3A)
    val shape = RoundedCornerShape(14.dp)
    val left = t.maneuver.uppercase().contains("LEFT")
    Column(modifier.widthIn(max = 380.dp).shadow(8.dp, shape).clip(shape).background(Color.White).padding(3.dp).clip(RoundedCornerShape(11.dp)).background(bg)
        .padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (t.exitNo.isNotBlank()) {
                Box(Modifier.clip(RoundedCornerShape(4.dp)).background(Color.White).padding(1.5.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black)
                    .padding(horizontal = 7.dp, vertical = 1.dp)) { Text(t.exitNo, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold) }
                Spacer(Modifier.width(6.dp))
            }
            refs.forEach { r ->
                if (motorway) Box(Modifier.padding(end = 6.dp).border(1.5.dp, Color.White, RoundedCornerShape(3.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                    Text(r, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                } else Text(r, color = Color(0xFFFFD200), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(end = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(if (left) "↖" else "↗", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        places.forEach { p -> Text(p, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

/**
 * A small diagram of the roundabout ahead: the ring, the roads off it (from the route's own junction data) and your way round it
 * lit in blue, from the road you come in on (at the bottom) to your exit. Clockwise where traffic keeps left, anticlockwise elsewhere.
 */
@Composable
fun RoundaboutView(engine: NavEngine, t: com.headunit.launcher.TurnInfo, modifier: Modifier = Modifier) {
    if (!t.maneuver.uppercase().startsWith("ROUNDABOUT") || t.rbIn < 0 || t.rbOut < 0 || t.dist > 800.0) return
    val c = navVw(LocalVw.current)
    val clockwise = engine.leftTraffic
    // angles on screen: the way you come in points up, so the road you enter from is at the bottom
    fun rel(b: Double) = ((b - t.rbIn) % 360.0 + 360.0) % 360.0
    val exitA = rel(t.rbOut)
    val others = t.rbExits.map { rel(it) }.filter { a -> Math.abs(((a - exitA + 540.0) % 360.0) - 180.0) > 20.0 && Math.abs(((a - 180.0 + 540.0) % 360.0) - 180.0) > 20.0 }
    val shape = RoundedCornerShape(20.dp)
    Box(modifier.size(132.dp).panel(6.dp, shape, c.surface).border(1.dp, c.line, shape).padding(8.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val cx = w / 2f; val cy = h / 2f
            val r = w * 0.24f
            val road = c.sub.copy(alpha = 0.35f)
            val sw = w * 0.085f
            fun leg(angleDeg: Double, col: Color, width: Float) {
                val a = Math.toRadians(angleDeg)
                val sx = cx + (r * Math.sin(a)).toFloat(); val sy = cy - (r * Math.cos(a)).toFloat()
                val ex = cx + (w * 0.5f * Math.sin(a)).toFloat(); val ey = cy - (h * 0.5f * Math.cos(a)).toFloat()
                drawLine(col, Offset(sx, sy), Offset(ex, ey), width, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }
            drawCircle(road, r, Offset(cx, cy), style = Stroke(sw))
            for (a in others) leg(a, road, sw)
            leg(180.0, road, sw); leg(exitA, road, sw)
            // your way round: in from the bottom, round the ring, out at your exit
            val blue = Color(0xFF2F7CF6)
            leg(180.0, blue, sw * 0.75f)
            leg(exitA, blue, sw * 0.75f)
            // the ring from the entry (bottom) to the exit, the way traffic goes
            val startCanvas = 90f   // drawArc angles: 0 = right, 90 = bottom (clockwise on screen)
            val exitCanvas = ((exitA - 90.0) % 360.0 + 360.0).toFloat() % 360f
            // (on screen a positive sweep goes clockwise: bottom, then left, then top, then right)
            val sweep = if (clockwise) (exitCanvas - startCanvas + 360f) % 360f else -((startCanvas - exitCanvas + 360f) % 360f)
            drawArc(blue, startCanvas, sweep, false, topLeft = Offset(cx - r, cy - r), size = androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(sw * 0.75f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}

/** A short, quiet note about the conditions (Lang.t("Starkregen · vorsichtig fahren, Tempo 50", "Heavy rain · take it steady, 50 limit")). */
@Composable
fun AdvicePill(text: String, modifier: Modifier = Modifier) {
    val c = navVw(LocalVw.current)
    val shape = RoundedCornerShape(50)
    Row(modifier.panel(6.dp, shape, c.surface).border(1.dp, c.line, shape).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.WaterDrop, null, tint = Color(0xFF4C8DFF), modifier = Modifier.size(18.dp))
        Text(text, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
