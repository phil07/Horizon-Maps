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

@Composable
internal fun ImmSearch(engine: NavEngine, q: String, onQ: (String) -> Unit, locked: Boolean, focus: FocusRequester, voice: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(30.dp)
    Box(Modifier.fillMaxWidth().height(60.dp).panel(10.dp, shape, c.surface)) {
        TextField(
            value = q, onValueChange = onQ, enabled = !locked,
            placeholder = { Text(if (locked) Lang.t("Tippe auf das Mikrofon, um während der Fahrt zu suchen", "Tap the mic to search while driving") else Lang.t("Wohin?", "Where to?"), fontSize = if (locked) 15.sp else 18.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = c.sub) },
            trailingIcon = {
                if (q.isNotEmpty()) Icon(Icons.Rounded.Close, Lang.t("Löschen", "Clear"), tint = c.sub, modifier = Modifier.padding(end = 8.dp).clickable { onQ(""); engine.results = emptyList() })
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    // folds Saved & recent up under the bar, and back down again
                    val open = engine.prefs.b(PreferenceKeys.QUICKLIST, true)
                    Box(Modifier.padding(end = 4.dp).size(40.dp).clip(CircleShape).clickable { engine.prefs.put(PreferenceKeys.QUICKLIST, !open) }, contentAlignment = Alignment.Center) {
                        Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, if (open) Lang.t("Gespeichert & zuletzt ausblenden", "Hide saved and recent") else Lang.t("Gespeichert & zuletzt einblenden", "Show saved and recent"), tint = c.sub, modifier = Modifier.size(26.dp))
                    }
                    Box(Modifier.padding(end = 8.dp).size(44.dp).clip(CircleShape).background(c.accent).pressClick { voice() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Mic, Lang.t("Sprachsuche", "Voice search"), tint = Color.White, modifier = Modifier.size(23.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { if (engine.stopMode && engine.route != null) engine.searchAlong(q) else engine.search(q) }),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = c.surface, unfocusedContainerColor = c.surface, disabledContainerColor = c.surface,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
                focusedTextColor = c.text, unfocusedTextColor = c.text, disabledTextColor = c.sub,
                focusedPlaceholderColor = c.sub, unfocusedPlaceholderColor = c.sub, disabledPlaceholderColor = c.sub,
                cursorColor = c.accent,
            ),
            modifier = Modifier.fillMaxSize().focusRequester(focus),
        )
    }
}

/** A place picked while driving: "Als Zwischenziel" or "Neues Ziel". Left alone for 15 s, the trip keeps its destination. */
@Composable
internal fun NewDestCard(engine: NavEngine) {
    val p = engine.newDestAsk ?: return
    val c = LocalVw.current
    LaunchedEffect(p) { delay(15_000); if (engine.newDestAsk === p) engine.newDestAsk = null }
    Column(Modifier.widthIn(max = 420.dp).panel(10.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(Lang.t("Zwischenziel oder neues Ziel?", "Stop or new destination?"), color = c.sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(p.name, color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Lang.t("Als Zwischenziel", "As a stop"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { engine.answerNewDest(true) }.padding(vertical = 11.dp))
            Text(Lang.t("Neues Ziel", "New destination"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.accent).clickable { engine.answerNewDest(false) }.padding(vertical = 11.dp))
        }
    }
}

/** The name box after a place is saved as a favourite: pre-filled with its name, to call it something of your own. */
@Composable
internal fun FavNameDialog(engine: NavEngine, p: Place) {
    var name by remember(p) { mutableStateOf(p.name) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { engine.favNaming = null },
        title = { Text(Lang.t("Name für den Favoriten", "Name for the favourite")) },
        text = { androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it.take(40) }, singleLine = true) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { engine.saveFavName(name) }) { Text(Lang.t("Speichern", "Save")) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { engine.favNaming = null }) { Text(Lang.t("Abbrechen", "Cancel")) } },
    )
}

/** The card for a place tapped on the phone's map: its name and address, "Los", and "Als Stopp" when there is a trip to add it to. */
@Composable
internal fun PhonePickCard(engine: NavEngine) {
    val p = engine.mapPick ?: return
    val c = LocalVw.current
    val hasTrip = engine.dest != null && engine.route != null
    val here = engine.fix?.pos
    val dist = here?.let { Geo.fmtDist(Geo.dist(it, p.pos), engine.imperial) }
    // a rough drive time (about 45 km/h over the straight line): an estimate, not a route
    val mins = here?.let { Math.round(Geo.dist(it, p.pos) / 12.5 / 60.0).toInt() }
    val arriveAt = mins?.let { m ->
        val cal = java.util.Calendar.getInstance(); cal.add(java.util.Calendar.MINUTE, m)
        String.format(java.util.Locale.US, "%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
    }
    var fav by remember(p) { mutableStateOf(engine.places.isFavourite(p)) }
    // calling and the website wait until you stop
    val moving = engine.driving
    Column(Modifier.widthIn(max = 420.dp).panel(12.dp, RoundedCornerShape(24.dp), c.surface).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlaceLogo(engine, p)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(p.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = listOfNotNull(dist, mins?.let { Lang.t("ca. $it Min.", "about $it min") }, arriveAt?.let { Lang.t("Ankunft $it", "arrive $it") }).joinToString(" · ")
                if (sub.isNotBlank()) Text(sub, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                HoursLine(p)
                engine.pickOffer?.let { Text(it, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { engine.clearPick() }.padding(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(Lang.t("Los", "Go"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.accent).clickable { engine.routePick() }.padding(vertical = 11.dp))
            if (hasTrip) Text(Lang.t("Als Stopp", "Add stop"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { engine.stopPick() }.padding(vertical = 10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pill(if (fav) Lang.t("Gespeichert", "Saved") else Lang.t("Speichern", "Save"), fav, { fav = engine.places.toggleFavourite(p); if (fav) engine.favNaming = p })
            if (!moving && p.phone != null) Pill(Lang.t("Anrufen", "Call"), false, { engine.callPlace(p.phone!!) })
            if (!moving && p.web != null) Pill(Lang.t("Website", "Website"), false, { engine.openWebsite(p.web!!) })
            Pill(Lang.t("Teilen", "Share"), false, { engine.sharePlace(p) })
        }
    }
}

/** "Geöffnet bis 20:00" in green or "Geschlossen · öffnet 08:00" in red, when the search gave the place's opening times. */
@Composable
internal fun HoursLine(p: com.headunit.launcher.Place) {
    val h = p.hours ?: return
    Text(h, color = if (com.headunit.launcher.OpenHours.isOpen(h)) Good else Bad, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/** A shop's logo in a round tile when it is on the unit, else the usual red pin. */
@Composable
internal fun PlaceLogo(engine: NavEngine, p: com.headunit.launcher.Place, size: androidx.compose.ui.unit.Dp = 42.dp) {
    val c = LocalVw.current
    @Suppress("UNUSED_VARIABLE") val rev = engine.logoRev
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val logo = remember(p, engine.logoRev) { com.headunit.launcher.BrandLogos.domainFor(p)?.let { com.headunit.launcher.BrandLogos.cached(ctx, it) } }
    Box(Modifier.size(size).clip(CircleShape).background(if (logo != null) Color.White else c.tile), contentAlignment = Alignment.Center) {
        if (logo != null) androidx.compose.foundation.Image(logo.asImageBitmap(), null, Modifier.size(size * 0.62f))
        else Icon(Icons.Rounded.Place, null, tint = Color(0xFFEA4335), modifier = Modifier.size(size * 0.52f))
    }
}

@Composable
internal fun ImmResults(engine: NavEngine, onPicked: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(26.dp)
    val here = engine.fix?.pos
    // a shop or chain with several branches nearby: "Alle N Standorte" under its first result
    val chainRows = remember(engine.results) {
        val seen = HashSet<String>()
        engine.results.mapNotNull { p ->
            val k = com.headunit.launcher.BrandLogos.key(p.brand ?: p.name)
            if (k in seen) null else { seen += k; val n = engine.chainCount(p); if (n >= 2) p to n else null }
        }.toMap()
    }
    // the list as it was drawn: a tap empties engine.results while the list is still on screen (it used to be read again then)
    val shownResults = engine.results
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).panel(10.dp, shape, c.surface).padding(vertical = 6.dp)) {
        items(shownResults) { p ->
            Row(Modifier.fillMaxWidth().clickable {
                // (picked once: a second tap in the same moment, on the row or its "See all" line, is ignored)
                if (engine.results !== shownResults) return@clickable
                onPicked()
                try { engine.pickResult(p) } catch (e: Exception) { android.util.Log.w("ImmResults", "pick failed", e) }
            }.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                PlaceLogo(engine, p)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(p.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(p.sub, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    HoursLine(p)
                }
                if (here != null) Text(Geo.fmtDist(Geo.dist(here, p.pos), engine.imperial), color = c.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            }
            chainRows[p]?.let { n ->
                Row(Modifier.fillMaxWidth().clickable { if (engine.results === shownResults) engine.showAllLocations(p) }.padding(start = 72.dp, end = 16.dp, top = 0.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(androidx.compose.material.icons.Icons.Rounded.LocationOn, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Text(Lang.t("Alle $n Standorte anzeigen", "See all $n locations"), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/**
 * Every branch of a shop or chain: sorted by Quickest (drive time with today's traffic), Nearest or On my route, each with its logo.
 * The map shows them all. Tap a row (or a pin) to choose it, then Go or Add as a stop. "Search this area" looks again where the map is.
 */
@Composable
internal fun ChainPanel(engine: NavEngine, onDone: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(26.dp)
    val ch = engine.chain
    val onRoute = engine.route != null && !engine.previewing
    Column(Modifier.fillMaxWidth().heightIn(max = 460.dp).panel(10.dp, shape, c.surface).padding(vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(ch?.name ?: Lang.t("Suche …", "Searching …"), color = c.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (engine.chainBusy) Lang.t("Standorte werden gesucht …", "Finding locations …") else Lang.t("${ch?.locs?.size ?: 0} Standorte", "${ch?.locs?.size ?: 0} locations"), color = c.sub, fontSize = 13.sp)
            }
            Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { engine.closeChain() }.padding(8.dp))
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val sorts = listOf("quick" to Lang.t("Schnellste", "Quickest"), "near" to Lang.t("Nächste", "Nearest")) + (if (onRoute) listOf("route" to Lang.t("Auf meiner Route", "On my route")) else emptyList())
            for ((id, label) in sorts) {
                val sel = engine.chainSort == id
                Text(label, color = if (sel) Color.White else c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(if (sel) c.accent else c.tile).clickable { engine.chainSort = id }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            Text(Lang.t("In diesem Bereich suchen", "Search this area"), color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { engine.chainSearchHere() }.padding(horizontal = 14.dp, vertical = 7.dp))
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(engine.chainSorted()) { l ->
                val sel = engine.chainSel === l
                Column(Modifier.fillMaxWidth().background(if (sel) c.accent.copy(alpha = 0.12f) else Color.Transparent).clickable { engine.selectChain(if (sel) null else l) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PlaceLogo(engine, l.place, 38.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(l.place.name, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val extra = if (engine.chainSort == "route") l.offRoute?.let { Lang.t(" · ${Geo.fmtDist(it, engine.imperial)} von der Route", " · ${Geo.fmtDist(it, engine.imperial)} off route") }.orEmpty() else ""
                            Text(l.place.sub + extra, color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            HoursLine(l.place)
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                            val secs = l.secs
                            if (secs != null) Text(Geo.fmtDur(secs), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(Geo.fmtDist(l.dist, engine.imperial), color = c.sub, fontSize = 13.sp)
                        }
                    }
                    if (sel) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(Lang.t("Los", "Go"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(c.accent).clickable { val p = l.place; engine.closeChain(); engine.results = emptyList(); onDone(); engine.startRoute(p) }.padding(vertical = 11.dp))
                        if (onRoute) Text(Lang.t("Als Stopp", "Add as stop"), color = c.accent, fontSize = 15.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).border(1.dp, c.accent, RoundedCornerShape(50)).clickable { val p = l.place; engine.closeChain(); engine.results = emptyList(); onDone(); engine.addStop(p) }.padding(vertical = 10.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun ImmChip(icon: ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    val c = LocalVw.current
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.height(46.dp).panel(6.dp, shape, c.surface).pressClick(onClick).padding(start = 8.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
        Text(label, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
    }
}

/** A phone held sideways: little height, so the map keeps to the essentials there (smaller buttons, no row of find buttons). */
@Composable
internal fun sidewaysPhone(engine: NavEngine): Boolean {
    val cfg = androidx.compose.ui.platform.LocalConfiguration.current
    return !engine.isCar && cfg.screenWidthDp >= 720 && cfg.screenHeightDp < 480
}

/** Under the search bar: Home and Work as two tiles with the drive time, then five round buttons to find things nearby. */
@Composable
internal fun ImmChips(engine: NavEngine, focusSearch: () -> Unit) {
    val home = engine.places.slot("home")
    val work = engine.places.slot("work")
    // the drive times: the engine throttles this itself (online only, every 10 minutes or after moving 3 km), so a minute's tick is cheap
    LaunchedEffect(Unit) { while (true) { engine.refreshSlotTimes(); delay(60_000) } }
    // your own labelled places and favourites: one tap away in a row that scrolls sideways, also while driving
    // (the same places are in the Saved & recent list; a favourite beside one of your own places counts once)
    val rawCustom = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val rawFav = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    // (own = true for one of your labelled places, false for a favourite)
    // only the starred ones of Your places are in the row (Settings › Places & trips › Your places); the switch there hides them all
    val showStarred = engine.prefs.b(PreferenceKeys.STARRED_ROW, true)
    val mine: List<Triple<com.headunit.launcher.Place, String, Boolean>> = remember(rawCustom, rawFav, showStarred) {
        val custom = engine.places.customs()
        val favs = engine.places.favouritesNotIn(custom)
        (if (showStarred) custom.filter { it.starred }.map { Triple(it.place, it.label, true) } else emptyList()) + favs.map { Triple(it, it.name, false) }
    }
    // the GPX tracks (saved routes) and the "Add a place" tile close the same row: all of it scrolls sideways
    val ctxT = LocalContext.current
    val tracks = com.headunit.launcher.Gpx.list(ctxT)
    var addingPlace by remember { mutableStateOf(false) }
    if (addingPlace) AddPlaceDialog(engine, null) { addingPlace = false }
    // the shortcuts "My places" and "My tracks" (Settings › Places & trips › Shortcuts): their list opens here, nearest first
    var listKind by remember { mutableStateOf<String?>(null) }
    // the folder open in that list (null = the top level); every time the list opens it starts at the top
    var listFolder by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(listKind) { listFolder = null }
    val cList = LocalVw.current
    val here = engine.fix?.pos
    listKind?.let { kind ->
        val rows: List<ShortcutRow> = if (kind == "places") {
            (engine.places.customs().map { Triple(it.label, it.place, it.folder) } + engine.places.favourites().map { Triple(it.name, it, "") }).map { (name, p, folder) ->
                ShortcutRow(name, here?.let { Geo.dist(it, p.pos) } ?: 0.0, folder) { engine.startRoute(p); listKind = null }
            }
        } else com.headunit.launcher.Gpx.list(ctxT).mapNotNull { t ->
            t.start?.let { s -> ShortcutRow(t.name, here?.let { Geo.dist(it, s) } ?: 0.0, t.group) { engine.driveTrack(t.name, com.headunit.launcher.Gpx.load(ctxT, t.id)); listKind = null } }
        }
        // folders first, then what's in no folder; a folder opens to its own places or tracks, and ‹ goes back
        val inFolder = listFolder
        val folders = if (inFolder == null) rows.map { it.folder }.filter { it.isNotBlank() }.distinct().sorted() else emptyList()
        val shown = (if (inFolder == null) rows.filter { it.folder.isBlank() } else rows.filter { it.folder == inFolder }).inListOrder(engine.fix != null)
        androidx.compose.material3.AlertDialog(onDismissRequest = { listKind = null }, confirmButton = { },
            title = { Text(inFolder ?: if (kind == "places") Lang.t("Meine Orte", "My places") else Lang.t("Meine Tracks", "My tracks"), color = cList.text) },
            text = {
                if (rows.isEmpty()) Text(if (kind == "places") Lang.t("Noch keine Orte gespeichert.", "No places saved yet.") else Lang.t("Noch keine Tracks importiert.", "No tracks imported yet."), color = cList.sub)
                else LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    if (inFolder != null) item {
                        Text("‹ " + Lang.t("Zurück", "Back"), color = cList.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { listFolder = null }.padding(vertical = 8.dp))
                    }
                    items(folders) { f ->
                        Row(Modifier.fillMaxWidth().clickable { listFolder = f }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(f, color = cList.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${rows.count { it.folder == f }} ›", color = cList.sub, fontSize = 13.sp)
                        }
                    }
                    items(shown) { r ->
                        Row(Modifier.fillMaxWidth().clickable { r.go() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(r.name, color = cList.text, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (here != null) Text(Geo.fmtDist(r.dist, engine.imperial), color = cList.sub, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            })
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val showHome = engine.prefs.b(PreferenceKeys.HOME_TILE, true); val showWork = engine.prefs.b(PreferenceKeys.WORK_TILE, true)
        if (showHome || showWork) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showHome) PlaceTile(engine, Modifier.weight(1f), Icons.Rounded.Home, Color(0xFF4C8DFF), Lang.t("Zuhause", "Home"), home, engine.slotMins["home"]) {
                if (home != null) engine.startRoute(home) else { engine.pendingSave = "home"; engine.toast(engine.Lang.t("Suche deine Heimadresse", "Search for your home address"), 3500); focusSearch() }
            }
            if (showWork) PlaceTile(engine, Modifier.weight(1f), Icons.Rounded.Work, Color(0xFFF29900), Lang.t("Arbeit", "Work"), work, engine.slotMins["work"]) {
                if (work != null) engine.startRoute(work) else { engine.pendingSave = "work"; engine.toast(engine.Lang.t("Suche deine Arbeitsadresse", "Search for your work address"), 3500); focusSearch() }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((p, label, own) in mine) {
                ImmChip(if (own) Icons.Rounded.Place else Icons.Rounded.Star, if (own) Color(0xFF4C8DFF) else Color(0xFFF29900), label) { engine.startRoute(p) }
            }
            for (t in tracks) ImmChip(Icons.Rounded.Route, Color(0xFF2E7D32), t.name) { engine.driveTrack(t.name, com.headunit.launcher.Gpx.load(ctxT, t.id)) }
            if (engine.prefs.b(PreferenceKeys.SC_PLACES, false)) ImmChip(Icons.Rounded.Place, Color(0xFF4C8DFF), Lang.t("Meine Orte", "My places")) { listKind = "places" }
            if (engine.prefs.b(PreferenceKeys.SC_TRACKS, false)) ImmChip(Icons.Rounded.Route, Color(0xFF2E7D32), Lang.t("Meine Tracks", "My tracks")) { listKind = "tracks" }
            ImmChip(Icons.Rounded.Add, LocalVw.current.accent, Lang.t("Ort hinzufügen", "Add a place")) { addingPlace = true }
        }
        // things to find nearby: always all five on show, no scrolling sideways to find them
        // (with a motorhome or caravan profile: pitches, water & waste and LPG instead of charging, food and coffee)
        // (a phone held sideways leaves these out: the map keeps its room for the route)
        if (!sidewaysPhone(engine)) if (engine.camperProfile) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (findOn(engine.prefs, "fuel")) FindButton(Icons.Rounded.LocalGasStation, Color(0xFFEA4335), Lang.t("Tankstellen in der Nähe", "Fuel nearby")) { engine.loadNearby("fuel", false) }
            if (findOn(engine.prefs, "camp")) FindButton(Icons.Rounded.RvHookup, Color(0xFF2E7D32), Lang.t("Stellplätze in der Nähe", "Pitches nearby")) { engine.loadNearby("camp", false) }
            if (findOn(engine.prefs, "dump")) FindButton(Icons.Rounded.WaterDrop, Color(0xFF0288D1), Lang.t("Ver- und Entsorgung in der Nähe", "Water and waste nearby")) { engine.loadNearby("dump", false) }
            if (findOn(engine.prefs, "lpg")) FindButton(Icons.Rounded.PropaneTank, Color(0xFFEF6C00), Lang.t("Autogas in der Nähe", "LPG nearby")) { engine.loadNearby("lpg", false) }
            if (findOn(engine.prefs, "parking")) FindButton(Icons.Rounded.LocalParking, Color(0xFF4C8DFF), Lang.t("Parkplätze in der Nähe", "Parking nearby")) { engine.loadNearby("parking", false) }
        } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (engine.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV") { if (findOn(engine.prefs, "charging")) FindButton(Icons.Rounded.EvStation, Color(0xFF34A853), Lang.t("Ladesäulen in der Nähe", "Charging nearby")) { engine.loadNearby("charging", false) } }
            else if (findOn(engine.prefs, "fuel")) FindButton(Icons.Rounded.LocalGasStation, Color(0xFFEA4335), Lang.t("Tankstellen in der Nähe", "Fuel nearby")) { engine.loadNearby("fuel", false) }
            if (findOn(engine.prefs, "food")) FindButton(Icons.Rounded.Restaurant, Color(0xFFF29900), Lang.t("Essen in der Nähe", "Food nearby")) { engine.loadNearby("food", false) }
            if (findOn(engine.prefs, "coffee")) FindButton(Icons.Rounded.LocalCafe, Color(0xFFA56BFF), Lang.t("Kaffee in der Nähe", "Coffee nearby")) { engine.loadNearby("coffee", false) }
            if (findOn(engine.prefs, "parking")) FindButton(Icons.Rounded.LocalParking, Color(0xFF4C8DFF), Lang.t("Parkplätze in der Nähe", "Parking nearby")) { engine.loadNearby("parking", false) }
        }
    }
}

@Composable
private fun PlaceTile(engine: NavEngine, modifier: Modifier, icon: ImageVector, tint: Color, label: String, place: com.headunit.launcher.Place?, mins: Int?, onClick: () -> Unit) {
    val c = LocalVw.current
    val here = engine.fix?.pos
    val sub = when {
        place == null -> Lang.t("Tippen zum Festlegen", "Tap to set")
        mins == 0 -> Lang.t("Du bist hier", "You\'re here")
        mins != null && mins > 0 -> Geo.fmtDur(mins * 60.0)
        here != null -> Geo.fmtDist(Geo.dist(here, place.pos), engine.imperial)
        else -> place.sub
    }
    // (kept compact on the phone so the map stays in view; the short label even when not set yet, "Setze zuhause" was cut off)
    Row(modifier.height(46.dp).panel(6.dp, RoundedCornerShape(18.dp), c.surface).pressClick(onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp)) }
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (place == null) Lang.t("Festlegen", "Set") else sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun FindButton(icon: ImageVector, tint: Color, desc: String, onClick: () -> Unit) {
    val c = LocalVw.current
    Box(Modifier.size(42.dp).panel(6.dp, CircleShape, c.surface).pressClick(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** A place sent from your phone: starts by itself after 8 seconds unless you tap Lang.t("Nicht jetzt", "Not now"). */
@Composable
internal fun PhoneDestCard(engine: NavEngine, p: com.headunit.launcher.Place) {
    val c = LocalVw.current
    var left by remember(p) { mutableStateOf(8) }
    LaunchedEffect(p) { while (left > 0) { delay(1000); left-- }; engine.takePhoneDest(true) }
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhoneAndroid, null, tint = c.accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Vom Handy: ${p.name}", "From your phone: ${p.name}"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val n = engine.phoneStops.size
            Text((if (n > 0) Lang.t(if (n == 1) "1 Stopp · " else "$n Stopps · ", if (n == 1) "1 stop · " else "$n stops · ") else "") + Lang.t("Start in $left s", "Starting in $left s"), color = c.sub, fontSize = 13.sp)
        }
        ImmPill(Lang.t("Los", "Go"), { engine.takePhoneDest(true) }, filled = true)
        ImmPill(Lang.t("Nicht jetzt", "Not now"), { engine.takePhoneDest(false) })
    }
}

/** On foot to the parked car: the next turn, the distance to it, and what is left; sound on or off for the walk, and end it. */
@Composable
internal fun WalkCard(engine: NavEngine) {
    val c = LocalVw.current
    val w = engine.walkInfo
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsWalk, null, tint = c.accent, modifier = Modifier.size(24.dp))
        }
        Column(Modifier.weight(1f)) {
            if (w == null) Text(Lang.t("Fußweg wird berechnet …", "Working out the way on foot …"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            else {
                val stepTxt = if (w.maneuver.uppercase().startsWith("ARRIVE")) "" else Geo.fmtDist(w.stepDist, engine.imperial) + " · "
                Text(stepTxt + w.text, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(Lang.t("Noch ${Geo.fmtDist(w.leftM, engine.imperial)} · etwa ${w.leftMin} Min. zum Auto", "${Geo.fmtDist(w.leftM, engine.imperial)} · about ${w.leftMin} min to your car"),
                    color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // the walk's spoken directions: on at the start, off and on again here
        ImmCircle({ engine.cycleSound() }, 40.dp) {
            Icon(if (engine.walkMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp, Lang.t("Ton", "Sound"), tint = if (engine.walkMuted) Bad else c.text, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { engine.endWalk() }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, Lang.t("Fußweg beenden", "End the walk"), tint = c.sub, modifier = Modifier.size(22.dp))
        }
    }
}

/** Where you parked (saved on the phone when Android Auto disconnects): walk there, or clear it. Shown for 2 days. */
@Composable
private fun ParkedCard(engine: NavEngine) {
    val c = LocalVw.current
    val raw = engine.prefs.s(PreferenceKeys.PARKED_SPOT, "")
    val parts = raw.split(',')
    val spot = parts.getOrNull(0)?.toDoubleOrNull()?.let { la -> parts.getOrNull(1)?.toDoubleOrNull()?.let { lo -> LL(la, lo) } } ?: return
    val at = parts.getOrNull(2)?.toLongOrNull() ?: return
    val ageMin = ((System.currentTimeMillis() - at) / 60_000L).coerceAtLeast(0L)
    if (ageMin > 48 * 60) return
    // (while walking there, the walk card shows the way instead)
    if (engine.walking) return
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val here = engine.fix?.pos
    val ago = when { ageMin < 1 -> Lang.t("gerade eben", "just now"); ageMin < 60 -> Lang.t("vor $ageMin Min.", "$ageMin min ago"); ageMin < 24 * 60 -> Lang.t("vor ${ageMin / 60} Std.", "${ageMin / 60} h ago"); else -> Lang.t("gestern", "yesterday") }
    Row(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.DirectionsCar, null, tint = c.accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(Lang.t("Dein Auto", "Your car"), color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(Lang.t("Geparkt $ago", "Parked $ago") + (if (here != null) " · " + Geo.fmtDist(Geo.dist(here, spot), engine.imperial) else ""), color = c.sub, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        // the way on foot, in the app (it used to hand a walking link to the phone, which this app took itself and planned on the roads)
        ImmPill(Lang.t("Zu Fuß", "Walk"), { engine.startWalkToCar() }, filled = true)
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { engine.prefs.put(PreferenceKeys.PARKED_SPOT, "") }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Close, Lang.t("Parkplatz vergessen", "Forget where you parked"), tint = c.sub, modifier = Modifier.size(22.dp))
        }
    }
}

/** Where you're probably heading (a place you often drive to at this time, with its drive time): a green card with Go. */
@Composable
internal fun PredictionCard(engine: NavEngine) {
    val pr = engine.prediction ?: return
    if (engine.route != null || engine.previewing) return
    val mins = if (pr.mins >= 0) Geo.fmtDur(pr.mins * 60.0) else null
    Row(Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(Good)
        .clickable { engine.startRoute(pr.place) }.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(listOfNotNull(pr.title, mins).joinToString(" · "), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (pr.note.isNotBlank()) Text(pr.note, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(Lang.t("Los", "Go"), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Resume a trip, a suggested trip, and saved & recent places. */
@Composable
internal fun ImmQuick(engine: NavEngine, showSaved: Boolean = true) {
    val c = LocalVw.current
    val rawCustom = engine.prefs.s(PreferenceKeys.PLACES_CUSTOM, "")
    val rawFav = engine.prefs.s(PreferenceKeys.PLACES_FAVOURITES, "")
    val rawRecent = engine.prefs.s(PreferenceKeys.PLACES_RECENT, "")
    // only the recent places are listed here: your own places, favourites and tracks are in the row above the list
    val saved: List<Triple<com.headunit.launcher.Place, Int, String>> = remember(rawCustom, rawFav, rawRecent) {
        val mine = engine.places.customs()
        val favs = engine.places.favouritesNotIn(mine)
        val recents = engine.places.recents().filter { r -> favs.none { Geo.within(it.pos, r.pos, 60.0) } && mine.none { Geo.within(it.place.pos, r.pos, 60.0) } }
        recents.map { Triple(it, 2, it.name) }
    }
    var adding by remember { mutableStateOf(false) }
    if (adding) AddPlaceDialog(engine, null) { adding = false }
    val here = engine.fix?.pos
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ParkedCard(engine)
        engine.resume?.let { rp ->
            Row(
                Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(22.dp)).clip(RoundedCornerShape(22.dp)).background(TealDeep).clickable { engine.startRoute(rp) }.padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Route, null, tint = Color.White, modifier = Modifier.size(26.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(Lang.t("Fahrt nach ${rp.name} fortsetzen", "Resume trip to ${rp.name}"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(Lang.t("Du warst noch nicht angekommen, als die App geschlossen wurde", "You hadn\'t arrived when the app closed"), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Dismiss"), tint = Color.White, modifier = Modifier.padding(start = 8.dp).size(24.dp).clickable { engine.resume = null; engine.prefs.put(PreferenceKeys.RESUME, "{}") })
            }
        }
        // every recent place is in the list; the list scrolls (its height is capped below)
        AnimatedVisibility(showSaved && engine.prefs.b(PreferenceKeys.QUICKLIST, true),
            enter = fadeIn(tween(200)) + androidx.compose.animation.expandVertically(tween(240)), exit = fadeOut(tween(140)) + androidx.compose.animation.shrinkVertically(tween(200))) {
        // folded down to this one line on every start; tapping the header unfolds it (and folds it again)
        val folded = engine.quickFolded
        Column(Modifier.fillMaxWidth().panel(10.dp, RoundedCornerShape(22.dp), c.surface).padding(vertical = 2.dp).animateContentSize(tween(200))) {
            Row(Modifier.fillMaxWidth().padding(start = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { engine.quickFolded = !folded }.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(Lang.t("Zuletzt", "Recent"), color = c.sub, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    Icon(if (folded) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                        if (folded) Lang.t("Aufklappen", "Unfold") else Lang.t("Zuklappen", "Fold"), tint = c.sub, modifier = Modifier.padding(start = 4.dp).size(20.dp))
                }
                if (!folded) Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { adding = true }.padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, null, tint = c.accent, modifier = Modifier.size(16.dp))
                    Text(Lang.t("Hinzufügen", "Add"), color = c.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 2.dp))
                }
            }
            if (!folded && saved.isEmpty()) Text(Lang.t("Gespeicherte und angefahrene Orte erscheinen hier", "Places you save or drive to show up here"), color = c.sub, fontSize = 12.sp, modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp))
            if (!folded) LazyColumn(Modifier.heightIn(max = 200.dp)) {
                items(saved) { (p, kind, label) ->
                    val fav = kind != 2
                    // swipe a place sideways to take it off the list (a favourite is unstarred, a recent one forgotten, your own place removed)
                    var dx by remember { mutableStateOf(0f) }
                    var w by remember { mutableStateOf(1) }
                    val shown by androidx.compose.animation.core.animateFloatAsState(dx, label = "swipe")
                    Row(Modifier.fillMaxWidth().onSizeChanged { w = it.width }
                        .pointerInput(p, kind) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (kotlin.math.abs(dx) > w * 0.35f) when (kind) {
                                        0 -> { engine.places.removeCustom(label); engine.toast(engine.Lang.f("%1\$s entfernt", "Removed %1\$s", label), 2000) }
                                        1 -> { engine.places.toggleFavourite(p); engine.toast(engine.Lang.t("Aus den Favoriten entfernt", "Removed from favourites"), 2000) }
                                        else -> { engine.places.removeRecent(p); engine.toast(engine.Lang.t("Aus den letzten Orten entfernt", "Removed from recent places"), 2000) }
                                    }
                                    dx = 0f
                                },
                                onDragCancel = { dx = 0f },
                            ) { change, amount -> change.consume(); dx += amount }
                        }
                        .graphicsLayer { translationX = shown; alpha = 1f - (kotlin.math.abs(shown) / w.toFloat()).coerceIn(0f, 0.7f) }
                        .clickable { engine.startRoute(p) }.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(c.tile), contentAlignment = Alignment.Center) {
                            Icon(if (fav) Icons.Rounded.Star else Icons.Rounded.History, null, tint = if (fav) Warn else c.sub, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.padding(start = 11.dp).weight(1f)) {
                            Text(label, color = c.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (p.sub.isNotBlank()) Text(p.sub, color = c.sub, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (here != null) Text(Geo.fmtDist(Geo.dist(here, p.pos), engine.imperial), color = c.sub, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        }
    }
}

/**
 * Every turn still ahead, opened by tapping the turn card: the arrow, what to do and onto which road, how far away it is and
 * roughly when you'll get there. Tapping a turn shows it on the map and offers to avoid that road. It opens over the trip card,
 * closes by itself 5 seconds after the last touch, and the map goes back to following you.
 */
@Composable
internal fun ItineraryCard(engine: NavEngine) {
    @Suppress("UNUSED_VARIABLE") val tick = engine.turn   // read so the list moves along as you drive
    val rows = engine.itinerary()
    val shape = RoundedCornerShape(28.dp)
    var picked by remember { mutableStateOf(-1) }
    var touched by remember { mutableStateOf(0) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // folds away 5 seconds after the last touch (scrolling the list counts)
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) touched++ }
    LaunchedEffect(touched, listState.isScrollInProgress) { if (!listState.isScrollInProgress) { delay(5_000); engine.closeItinerary() } }
    LaunchedEffect(rows.isEmpty()) { if (rows.isEmpty()) engine.closeItinerary() }
    Column(Modifier.fillMaxWidth().shadow(12.dp, shape).clip(shape).background(Teal).border(1.5.dp, Color.White.copy(alpha = 0.18f), shape)) {
        Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Lang.t("Deine Route", "Your route"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                val e = engine.eta
                Text("${rows.size} ${if (rows.size == 1) Lang.t("Schritt", "step") else Lang.t("Schritte", "steps")}" + (if (e != null) " · " + Geo.fmtDist(e.dist, engine.imperial) + Lang.t(" · Ankunft ", " · arrive ") + e.arrive else ""),
                    color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
            Box(Modifier.size(44.dp).clip(CircleShape).background(TealDeep).clickable { engine.closeItinerary() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).background(TealDeep), state = listState) {
            items(rows, key = { it.index }) { row ->
                val sel = picked == row.index
                Column(Modifier.fillMaxWidth().background(if (sel) Color.White.copy(alpha = 0.10f) else Color.Transparent)
                    .clickable { touched++; picked = row.index; engine.previewTurn(row) }.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ManeuverArrow(row.maneuver, Geo.maneuverAngle(row.maneuver), Modifier.size(30.dp), roundaboutExit = row.exit, roundaboutClockwise = engine.leftTraffic)
                        Column(Modifier.padding(start = 14.dp).weight(1f)) {
                            Text(row.text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(listOfNotNull(row.tag.ifBlank { null }, Lang.t("ca. ", "about ") + row.arrive).joinToString(" · "), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1)
                        }
                        Text(Geo.fmtDist(row.dist, engine.imperial), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp))
                    }
                    if (sel && row.road.isNotBlank()) Row(Modifier.padding(start = 44.dp, top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(Lang.t("${row.road} meiden", "Avoid ${row.road}"), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)).clickable { engine.avoidTurnRoad(row) }.padding(horizontal = 14.dp, vertical = 7.dp))
                    }
                }
            }
        }
    }
}

/** The charger filter: one power class; the map then shows only the charge points that fall in it. */
@Composable
internal fun ChargerFilterDialog(engine: NavEngine, onClose: () -> Unit) {
    val c = LocalVw.current
    val options = listOf(0 to Lang.t("Alle Ladesäulen", "All chargers"), 50 to Lang.t("50 kW und höher", "50 kW and up"),
        150 to Lang.t("150 kW und höher", "150 kW and up"), 300 to Lang.t("300 kW und höher", "300 kW and up"))
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(Modifier.padding(horizontal = 16.dp).widthIn(max = 480.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.surface).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Lang.t("Ladesäulen filtern", "Filter chargers"), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(28.dp).clickable { onClose() })
            }
            for ((kw, label) in options) {
                val on = engine.chargerMinKw == kw
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) c.accent.copy(alpha = 0.18f) else c.surface)
                    .clickable { engine.pickChargerMinKw(kw); onClose() }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = c.text, fontSize = 17.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    if (on) Icon(Icons.Rounded.Check, null, tint = c.accent, modifier = Modifier.size(22.dp))
                }
            }
            Text(Lang.t("Standorte mit unbekannter Leistung werden bei einem Filter ausgeblendet.", "Sites with unknown power are hidden while a filter is on."), color = c.sub, fontSize = 12.sp)
        }
    }
}

/** The matches along the route, sorted (least detour, soonest, nearest), with the minutes each adds; tap one, then Add as a stop. */
@Composable
internal fun AlongPanel(engine: NavEngine) {
    val c = LocalVw.current
    var picked by remember(engine.alongQuery) { mutableStateOf<NavEngine.AlongHit?>(null) }
    Column(Modifier.floating(24.dp).padding(14.dp).heightIn(max = 420.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Lang.t("Entlang der Route: ${engine.alongQuery}", "Along your route: ${engine.alongQuery}"), color = c.text, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (engine.alongBusy) CircularProgressIndicator(Modifier.size(18.dp), color = c.accent, strokeWidth = 2.dp)
            Box(Modifier.padding(start = 10.dp).size(44.dp).clip(CircleShape).clickable { engine.closeAlong() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(22.dp))
            }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(Lang.t("Geringster Umweg", "Least detour"), engine.alongSort == "detour", { engine.changeAlongSort("detour") })
            Pill(Lang.t("Am frühesten", "Soonest"), engine.alongSort == "soon", { engine.changeAlongSort("soon") })
            Pill(Lang.t("Am nächsten", "Nearest"), engine.alongSort == "near", { engine.changeAlongSort("near") })
        }
        if (!engine.alongBusy && engine.alongHits.isEmpty()) Text(Lang.t("Nichts entlang der Route gefunden.", "Nothing found along the route."), color = c.sub, fontSize = 15.sp, modifier = Modifier.padding(top = 10.dp))
        LazyColumn(Modifier.padding(top = 6.dp)) {
            items(engine.alongHits) { h ->
                val sel = picked === h
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { picked = h }.padding(vertical = 8.dp, horizontal = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(h.place.name, color = if (sel) c.accent else c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(if (h.extraMin <= 0) Lang.t("ohne Umweg", "no detour") else Lang.t("+${h.extraMin} Min.", "+${h.extraMin} min"), color = c.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text(Lang.t("in ${com.headunit.launcher.Geo.fmtDist(h.ahead, engine.imperial)} voraus · ${Math.round(h.off)} m abseits", "${com.headunit.launcher.Geo.fmtDist(h.ahead, engine.imperial)} ahead · ${Math.round(h.off)} m off the route"),
                        color = c.sub, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
        picked?.let { h ->
            ImmPill(Lang.t("Als Stopp hinzufügen", "Add as a stop"), { engine.addStop(h.place); engine.closeAlong() }, filled = true, icon = Icons.Rounded.Add)
        }
    }
}


/** Back, play / pause and next for whatever plays (only while something does); the song and artwork when Horizon Nav may see them. */
@Composable
internal fun MediaBar(engine: NavEngine) {
    val ctx = LocalContext.current
    val c = LocalVw.current
    LaunchedEffect(Unit) { while (true) { com.headunit.launcher.MediaHub.refresh(ctx); delay(1000) } }
    val now = com.headunit.launcher.MediaHub.now
    if (!now.playing) return
    Row(Modifier.panel(10.dp, RoundedCornerShape(28.dp), c.surface).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        now.art?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))) }
        if (now.title.isNotBlank()) Column(Modifier.widthIn(max = 200.dp).padding(horizontal = 10.dp)) {
            Text(now.title, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (now.artist.isNotBlank()) Text(now.artist, color = c.sub, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        MediaKey(Icons.Rounded.SkipPrevious, Lang.t("Zurück", "Back")) { com.headunit.launcher.MediaHub.control(ctx, "prev") }
        MediaKey(Icons.Rounded.Pause, Lang.t("Pause", "Pause")) { com.headunit.launcher.MediaHub.control(ctx, "toggle") }
        MediaKey(Icons.Rounded.SkipNext, Lang.t("Weiter", "Next")) { com.headunit.launcher.MediaHub.control(ctx, "next") }
    }
}

@Composable
private fun MediaKey(icon: ImageVector, desc: String, onClick: () -> Unit) {
    val c = LocalVw.current
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = c.text, modifier = Modifier.size(28.dp))
    }
}

/** Whether a find button (fuel, parking, camp…) is on: each one can be taken off in Settings › Places & trips › Verknüpfungen. */
internal fun findOn(prefs: com.headunit.launcher.Prefs, kind: String): Boolean = prefs.b("find_$kind", true)

/** One row of a shortcut list ("My places", "My tracks"): its name, how far it is, the folder it's in (blank = none), and what a tap does. */
internal class ShortcutRow(val name: String, val dist: Double, val folder: String, val go: () -> Unit)

/** Your places: A–Z until the app knows where you are, then nearest first. */
internal fun List<ShortcutRow>.inListOrder(positionKnown: Boolean): List<ShortcutRow> =
    if (positionKnown) sortedBy { it.dist } else sortedBy { it.name.lowercase() }
