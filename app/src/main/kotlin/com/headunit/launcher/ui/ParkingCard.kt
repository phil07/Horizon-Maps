package com.headunit.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.headunit.launcher.Geo
import com.headunit.launcher.Lang
import com.headunit.launcher.NavEngine

/**
 * Car parks near the destination, as their own card under the turn card (phone, head unit and Android Auto): up to three, a tap
 * drives to one (you walk the last bit), the X hides the card for this trip. [car] makes the taps work on the car screen.
 */
@Composable
internal fun ParkingCard(engine: NavEngine, car: Boolean, modifier: Modifier = Modifier) {
    val list = engine.parkingNear
    if (list.isEmpty()) return
    val c = LocalVw.current
    @Composable
    fun Modifier.tap(id: String, action: () -> Unit): Modifier = if (car) carTap(id, action) else clickable(onClick = action)
    Column(modifier.fillMaxWidth().padding(top = 6.dp).panel(8.dp, RoundedCornerShape(20.dp), c.surface).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalParking, null, tint = Color(0xFFFBBC04), modifier = Modifier.size(20.dp))
            Text(Lang.t("Parken in der Nähe", "Parking nearby"), color = c.sub, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 6.dp))
            Box(Modifier.size(40.dp).tap("car-park-close") { engine.dismissParking() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Close, Lang.t("Schließen", "Close"), tint = c.sub, modifier = Modifier.size(20.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            list.take(3).forEachIndexed { i, n ->
                Row(Modifier.fillMaxWidth().tap("car-park-$i") { engine.parkAt(n) }.padding(end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(n.place.name, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(listOfNotNull(Geo.fmtDist(n.dist, engine.imperial), n.extra.ifBlank { null }).joinToString(" · "), color = c.sub, fontSize = 14.sp, maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
