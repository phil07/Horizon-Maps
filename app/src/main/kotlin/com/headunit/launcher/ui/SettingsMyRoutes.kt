package com.headunit.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Lang
import com.headunit.launcher.MyRoutes
import com.headunit.launcher.NavEngine

/**
 * Settings › Places & trips › My routes: the ways you saved on the arrival card. Each can be shown (a small drawing of the way),
 * renamed or deleted, and set to be offered in the route choice, started straight away, or not offered.
 */
@Composable
internal fun MyRoutesGroup(engine: NavEngine) {
    val prefs = engine.prefs
    var list by remember { mutableStateOf(MyRoutes.load(prefs)) }
    var shown by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<MyRoutes.MyRoute?>(null) }
    fun reload() { list = MyRoutes.load(prefs) }
    SGroup(Lang.t("Meine Routen", "My routes"), footer = Lang.t("Speichere eine Fahrt auf der Ankunftskarte mit „Als meine Route speichern“. Fährst du wieder von dort nach dort, steht sie in der Routenwahl zuerst", "Save a drive on the arrival card with \"Save as my route\". Next time you go from there to there, it's first in the route choice")) {
        if (list.isEmpty()) SItem(Lang.t("Noch keine Routen gespeichert", "No routes saved yet"))
        for (r in list) {
            val c = LocalVw.current
            SItem(r.name, r.toName.takeIf { it.isNotBlank() }) {
                SButton(if (shown == r.id) Lang.t("Ausblenden", "Hide") else Lang.t("Anzeigen", "Show")) { shown = if (shown == r.id) null else r.id }
            }
            if (shown == r.id) {
                // the way as a small drawing, north up
                Canvas(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).height(160.dp).clip(RoundedCornerShape(14.dp))) {
                    drawRect(c.tile)
                    val minLat = r.pts.minOf { it.lat }; val maxLat = r.pts.maxOf { it.lat }
                    val minLon = r.pts.minOf { it.lon }; val maxLon = r.pts.maxOf { it.lon }
                    val k = Math.cos(Math.toRadians((minLat + maxLat) / 2))
                    val w = ((maxLon - minLon) * k).coerceAtLeast(1e-6); val h = (maxLat - minLat).coerceAtLeast(1e-6)
                    val pad = 12.dp.toPx()
                    val sc = minOf((size.width - 2 * pad) / w, (size.height - 2 * pad) / h)
                    val ox = (size.width - w * sc) / 2; val oy = (size.height - h * sc) / 2
                    fun at(p: com.headunit.launcher.LL) = Offset((ox + (p.lon - minLon) * k * sc).toFloat(), (oy + (maxLat - p.lat) * sc).toFloat())
                    val path = Path().apply { moveTo(at(r.pts.first()).x, at(r.pts.first()).y); for (p in r.pts.drop(1)) at(p).let { lineTo(it.x, it.y) } }
                    drawPath(path, c.accent, style = Stroke(5.dp.toPx()))
                    drawCircle(Good, 7.dp.toPx(), at(r.pts.first()))
                    drawCircle(Bad, 7.dp.toPx(), at(r.pts.last()))
                }
            }
            SChoice(Lang.t("Anbieten", "Offer it"), listOf("offer" to Lang.t("In der Routenwahl", "In the route choice"), "auto" to Lang.t("Sofort starten", "Start straight away"), "off" to Lang.t("Nicht anbieten", "Don't offer")), r.mode, null) { m ->
                MyRoutes.update(prefs, r.id) { it.copy(mode = m) }; reload()
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SButton(Lang.t("Umbenennen", "Rename")) { renaming = r }
                SButton(Lang.t("Löschen", "Delete"), danger = true) { MyRoutes.remove(prefs, r.id); if (shown == r.id) shown = null; reload() }
            }
        }
    }
    renaming?.let { r ->
        MyRouteNameDialog(r.name) { nm -> renaming = null; if (nm != null) { MyRoutes.update(prefs, r.id) { it.copy(name = nm) }; reload() } }
    }
}

/** Asks for a route's name ([initial] filled in); null when cancelled. */
@Composable
internal fun MyRouteNameDialog(initial: String, onDone: (String?) -> Unit) {
    val c = LocalVw.current
    var name by remember { mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text(Lang.t("Name der Route", "Route name"), color = c.text) },
        text = { Column { SField(name, { name = it.take(60) }, placeholder = initial, width = 300.dp, fill = true) } },
        confirmButton = { SButton(Lang.t("Speichern", "Save"), primary = true) { onDone(name.trim().ifBlank { initial }) } },
        dismissButton = { SButton(Lang.t("Abbrechen", "Cancel")) { onDone(null) } },
    )
}
