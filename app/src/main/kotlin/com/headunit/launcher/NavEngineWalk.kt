package com.headunit.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.res.stringResource
import com.headunit.launcher.NavEngine.Utt
import com.headunit.launcher.NavEngine.WalkInfo

// Part of NavEngine (moved out of NavEngine.kt to keep that file workable): these are extension functions on the engine,
// so they are called exactly as before.

/** Where you parked (saved when the car's connection ended, or by "hier geparkt"), for two days; null when none. */
fun NavEngine.parkedSpot(): LL? {
    val ps = prefs.s(PreferenceKeys.PARKED_SPOT, "").split(',')
    val la = ps.getOrNull(0)?.toDoubleOrNull() ?: return null
    val lo = ps.getOrNull(1)?.toDoubleOrNull() ?: return null
    val at = ps.getOrNull(2)?.toLongOrNull() ?: 0L
    if (at > 0 && System.currentTimeMillis() - at > 48 * 3600_000L) return null
    return LL(la, lo)
}

/** Walks you back to your parked car, in the app (a route on foot, not on the roads). False when no spot is saved. */
fun NavEngine.startWalkToCar(): Boolean {
    val spot = parkedSpot() ?: run { toast(Lang.t("Es ist kein Parkplatz gespeichert", "No parking spot is saved"), 3000); return false }
    startWalk(spot)
    return true
}

/** Starts a walk to [to]. A drive on the screen (or a route choice) ends first: you are on foot now. */
fun NavEngine.startWalk(to: LL) {
    if (route != null) endRoute()
    walkTo = to; walkInfo = null; walkMuted = false; walkRoute = null; walkIdx = 0; walkOffCount = 0; walkSaid.clear(); walkStartSaid = false
    // no speed limit or camera on foot
    speedLimit = null; limitEstimated = false; noLimit = false; nextLimitMs = null
    following = true
    computeWalk()
}

fun NavEngine.endWalk() {
    walkTo = null; walkInfo = null; walkRoute = null
    map.setWalk(emptyList())
}

/** The sound button while walking: the walk's directions off or on again (the sound for driving stays as it is). */
internal fun NavEngine.toggleWalkSound() {
    walkMuted = !walkMuted
    toast(if (walkMuted) Lang.t("Fußweg-Ansagen aus", "Walking directions muted") else Lang.t("Fußweg-Ansagen an", "Walking directions on"), 1800)
}

/** Spoken on the walk, unless muted with the sound button (the drive's sound setting doesn't apply on foot). */
private fun NavEngine.walkSpeak(text: String) {
    if (walkMuted || !ttsReady) return
    val u = Utt(text, 2, android.os.SystemClock.elapsedRealtime())
    if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) enqueue(u) else speechMain.post { enqueue(u) }
}

/** The way on foot: Mapbox's walking route online, the offline pack's footpaths without signal, else a straight line. */
private fun NavEngine.computeWalk() {
    val to = walkTo ?: return
    val from = fix?.pos ?: return   // the first position starts it (see updateWalk)
    if (walkBusy) return
    walkBusy = true; walkAskedAt = System.currentTimeMillis()
    scope.launch {
        val r = withContext(Dispatchers.IO) {
            (if (hasMapbox && online()) try { MapboxRest.walkRoutes(mbToken, from, to).firstOrNull() } catch (e: Exception) { null } else null)
                ?: (if (OfflineRouter.installed(ctx)) try { OfflineRouter.routes(ctx, from, to, emptyList(), -1.0, "pedestrian").firstOrNull() } catch (e: Exception) { null } else null)
                ?: straightWalk(from, to)
        }
        walkBusy = false
        if (walkTo != to) return@launch
        walkRoute = r; walkIdx = 0; walkOffCount = 0; walkSaid.clear()
        map.setWalk(r.pts)
        fix?.let { updateWalk(it) }
    }
}

/** No route on foot to be had: the straight line to the car, with its direction and distance. */
private fun NavEngine.straightWalk(from: LL, to: LL): Route {
    val d = Geo.dist(from, to)
    return Route(listOf(from, to), doubleArrayOf(0.0, d), listOf(Step(d, "ARRIVE", "", "")), d, d / 1.3, 0.0)
}

/** Each new position on the walk: the next instruction, the spoken call before it, a new way when you leave it, and arriving. */
internal fun NavEngine.updateWalk(p: Fix) {
    val to = walkTo ?: return
    if (walkRoute == null) { computeWalk(); return }
    val r = walkRoute ?: return
    if (Geo.dist(p.pos, to) < 20.0) {
        val msg = Lang.t("Du bist bei deinem Auto", "You're at your car")
        toast(msg, 4000)
        walkSpeak(msg)
        endWalk()
        return
    }
    if (r.pts.size < 2) return
    val s = RouteMatcher.snap(r, p.pos, walkIdx)
    walkIdx = s.segmentIndex
    // gone another way (more than 40 m off, with a good fix, several times): the way from here is asked again
    if (s.dist > 40.0 && p.acc in 0.1..30.0) {
        if (++walkOffCount >= 4 && System.currentTimeMillis() - walkAskedAt > 15_000L) { walkOffCount = 0; walkRoute = null; computeWalk(); return }
    } else walkOffCount = 0
    val prog = s.progress
    val total = r.cum.last()
    val left = max(0.0, total - prog)
    var k = r.steps.indexOfFirst { it.off > prog + 3.0 }
    if (k < 0) k = r.steps.size - 1
    val st = r.steps.getOrNull(k)
    val arriving = st == null || st.maneuver.uppercase().startsWith("ARRIVE")
    val stepD = if (st != null && !arriving) max(0.0, st.off - prog) else left
    val word = if (arriving) "" else Geo.maneuverText(st!!.maneuver, st.roundExit)
    val street = if (arriving || st!!.street.isBlank()) "" else Lang.t(" auf ${st.street}", " onto ${st.street}")
    val text = if (arriving) Lang.t("Zu deinem Auto", "To your car") else word.replaceFirstChar { it.uppercase() } + street
    val mins = maxOf(1, Math.ceil(left / 80.0).toInt())
    walkInfo = WalkInfo(text, stepD, st?.maneuver ?: "ARRIVE", left, mins)
    if (!walkStartSaid) {
        walkStartSaid = true
        walkSpeak(Lang.t("Fußweg zu deinem Auto, ${walkMetres(left)}, etwa $mins Minuten", "Walking to your car, ${walkMetres(left)}, about $mins minutes"))
    }
    // one call per turn, about 25 m before it ("In 20 Metern links abbiegen")
    if (!arriving && stepD <= 25.0 && k !in walkSaid) {
        walkSaid += k
        walkSpeak(Lang.t("In ${walkMetres(stepD)} $word$street", "In ${walkMetres(stepD)}, $word$street"))
    }
}

/** A walking distance to say: to 10 m, "Metern" in German after "in". */
private fun NavEngine.walkMetres(m: Double): String {
    if (imperial) return Geo.spoken(m, true)
    val r = maxOf(10, (Math.round(m / 10.0) * 10).toInt())
    return if (r < 1000) Lang.t("$r Metern", "$r metres") else String.format(Locale.GERMANY, "%.1f Kilometern", r / 1000.0).let { Lang.t(it, String.format(Locale.US, "%.1f kilometres", r / 1000.0)) }
}

internal fun NavEngine.enqueue(u: Utt) {
    if (u.kind == 2) {
        // older turn calls and plain notes still waiting are out of date now
        speechQ.removeAll { it.kind != 1 }
        speechQ.addFirst(u)
        val p = playing
        // a plain note being read out is cut short; a warning or a turn call is finished first (cutting a turn call off mid
        // sentence left half a direction)
        if (p != null && p.kind == 0) { playing = null; playingId = ""; try { tts?.stop() } catch (e: Exception) { } }
    } else speechQ.addLast(u)
    playNext()
}

private fun NavEngine.playNext() {
    if (playing != null) return
    val now = android.os.SystemClock.elapsedRealtime()
    while (true) {
        val u = speechQ.removeFirstOrNull() ?: break
        // how long each may wait: a turn call only a few seconds (the distance in it is soon wrong), a warning half a minute
        val maxAge = when (u.kind) { 2 -> 6_000L; 1 -> 30_000L; else -> 15_000L }
        if (now - u.at > maxAge) continue
        val ch = prefs.s(PreferenceKeys.VOICE_CHANNEL, "nav")
        if (ch != voiceChan) { voiceChan = ch; try { tts?.setAudioAttributes(speechAttrs) } catch (e: Exception) { } }
        spoken = 1
        duck()
        val id = "nav${++uttSeq}"
        playing = u; playingId = id
        val r = tts?.speak(u.text, TextToSpeech.QUEUE_ADD, null, id)
        if (r == TextToSpeech.ERROR) { playing = null; playingId = ""; continue }
        return
    }
    // nothing left to say: the music comes back up
    spoken = 1; unduck()
}

/** The speech engine finished (or stopped) [id]: the next one, if it's still the current one. */
internal fun NavEngine.uttDone(id: String?) {
    speechMain.post {
        if (id == null || id != playingId) return@post
        playing = null; playingId = ""
        playNext()
    }
}
