package com.headunit.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The speed cameras looked up so far, kept for the rest of the drive: [around] the ones found by the searches around the car
 * and around the middle of the map, [known] the ones the map's search along routes has found. A failed search keeps what is
 * there already; the "no camera data" note shows once, until cameras are found again.
 *
 * [enabled]: whether cameras are looked up and drawn at all. [localLookup]: the offline pack's list within a radius, if any.
 * [changed]: called when the list has grown, so the map and the warnings are updated.
 */
internal class CameraCache(
    private val scope: CoroutineScope,
    private val enabled: () -> Boolean,
    private val localLookup: (LL, Double) -> List<LL>?,
    private val changed: () -> Unit,
) {
    /** What the last camera search found, for Settings (so "none shown" can be told apart from "none found"). */
    var note by mutableStateOf("")
    /** No camera list could be had here (no offline pack, OpenStreetMap not answering) and none is kept: the map says so. */
    var noData by mutableStateOf(false)
    /** The cameras from the searches around you and around the map view. */
    var around: List<LL> = emptyList()
        private set
    /** Every speed camera found along routes so far (by the map's search along the route, together with the traffic lights). */
    @Volatile var known: List<LL> = emptyList()

    private var lastAt = 0L
    private var lastPos: LL? = null
    private var noDataShown = false
    private var noDataJob: Job? = null

    /**
     * The speed cameras around the car (6 km), looked up as soon as there is a fix: at the start too, and also while the phone is
     * quiet with Android Auto connected, where the road check waits. A failed lookup is tried again after about half a minute.
     */
    fun lookAround(p: Fix) {
        if (!enabled()) return
        val now = System.currentTimeMillis()
        val cp = lastPos
        if (now - lastAt <= 120000 || (cp != null && Geo.dist(cp, p.pos) <= 2500)) return
        lastAt = now; lastPos = p.pos
        scope.launch {
            var src = Lang.t("Offline-Karte", "offline map")
            val found = withContext(Dispatchers.IO) { localLookup(p.pos, 6000.0) ?: Overpass.cameras(p.pos, 6000).also { src = "OpenStreetMap" } }
            val at = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            // a failed lookup (weak signal, busy server) keeps the ones already shown and tries again in about half a minute
            if (found == null) {
                lastAt = System.currentTimeMillis() - 90_000L; lastPos = null
                addFailed(Lang.t("$at: Suche fehlgeschlagen (${Overpass.lastNote.take(60)})", "$at: lookup failed (${Overpass.lastNote.take(60)})"))
            } else {
                addFound(found, Lang.t("$at: ${found.size} Blitzer im Umkreis von 6 km ($src)", "$at: ${found.size} cameras within 6 km ($src)"))
                changed()
            }
        }
    }

    /** A search for the map view (free look) that came back with cameras: they are kept, and the list is updated. */
    fun addFound(found: List<LL>, text: String) {
        noData = false; noDataShown = false
        val keys = HashSet<Long>(); around.forEach { keys += Overpass.camKey(it) }
        val merged = ArrayList(around)
        for (f in found) if (keys.add(Overpass.camKey(f))) merged += f
        while (merged.size > 4000) merged.removeAt(0)
        around = merged
        note = text
    }

    /** A search that failed: the note says so; with nothing kept at all, the map says there is no camera data (once). */
    fun addFailed(text: String) {
        if (around.isEmpty()) showNoData()
        note = text
    }

    /** Camera lookups are off (or the cameras are): the next one may look again straight away. */
    fun forgetPosition() { lastPos = null }

    /** Shows the "no camera data" note for 5 seconds, then hides it again. */
    private fun showNoData() {
        // once: not again with each failed lookup, only after cameras were found and then lost
        if (noDataShown) return
        noDataShown = true
        noData = true
        noDataJob = scope.launch { delay(5000L); noData = false }
    }
}
