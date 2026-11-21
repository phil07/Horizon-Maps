package com.headunit.launcher

import android.content.Context
import com.mapbox.geojson.Feature
import com.mapbox.geojson.Point

/**
 * Where the street lamps and the traffic lights stand, as geometry only (no map state): the mapped lamps (the offline pack's list,
 * and OpenStreetMap when [onlineOn]), each with its arm turned over the nearest street; where the area has hardly any mapped,
 * lamps every [gap] metres along the streets. The map and its state decide when and what to show (MapBinding.refreshLamps).
 */
internal object LampPlacement {
    /**
     * Where the lamps go round [c] (about 900 m each way): the mapped ones, each with its arm turned over the nearest street; and,
     * if the area has hardly any mapped, lamps every [gap] metres along the streets on screen, at the left kerb, arm over the road.
     * The nearest 90 are kept. Returns the posts, the light pools under their lamps, and a line for Settings.
     */
    fun place(ctx: Context, c: LL, roads: List<List<Point>>, gap: Double, onlineOn: Boolean): Triple<List<Feature>, List<Feature>, String> {
        val dLat = 0.008; val dLon = dLat / Math.cos(Math.toRadians(c.lat))
        val offlineLamps = if (OfflineRouter.hasLamps(ctx)) OfflineRouter.lamps(ctx, c.lat - dLat, c.lon - dLon, c.lat + dLat, c.lon + dLon, maxTiles = 64) else emptyList()
        // the mapped lamps of OpenStreetMap online (once per area, kept a month): the real positions, wherever people have mapped them
        val online = if (onlineOn) try { LampsOnline.inBox(ctx, c.lat - dLat, c.lon - dLon, c.lat + dLat, c.lon + dLon, LampsOnline.connected(ctx)) } catch (e: Exception) { emptyList() } else emptyList()
        val mapped = offlineLamps + online
        // spots: lat, lon, bearing of the arm
        val spots = ArrayList<DoubleArray>()
        val near = mapped.sortedBy { Geo.dist(LL(it[0], it[1]), c) }.take(90)
        for (q in near) spots += doubleArrayOf(q[0], q[1], armTowardsRoad(q[0], q[1], roads))
        val guessing = mapped.size < 15
        if (guessing && roads.isNotEmpty()) {
            val taken = HashSet<Long>()
            val cell = Math.max(8.0, gap * 0.6)
            fun key(lat: Double, lon: Double) = Math.floor(lat * 111320.0 / cell).toLong() * 1000003L + Math.floor(lon * 111320.0 * Math.cos(Math.toRadians(lat)) / cell).toLong()
            for (q in spots) taken += key(q[0], q[1])
            val kx = 111320.0 * Math.cos(Math.toRadians(c.lat)); val ky = 111320.0
            for (line in roads) {
                if (line.size < 2) continue
                var carry = gap / 2
                for (i in 0 until line.size - 1) {
                    val a = line[i]; val b = line[i + 1]
                    val ax = a.longitude() * kx; val ay = a.latitude() * ky
                    val bx = b.longitude() * kx; val by = b.latitude() * ky
                    val len = Math.hypot(bx - ax, by - ay)
                    if (len < 0.5) continue
                    var d = carry
                    while (d <= len) {
                        val t = d / len
                        val lat = a.latitude() + (b.latitude() - a.latitude()) * t
                        val lon = a.longitude() + (b.longitude() - a.longitude()) * t
                        val dir = Math.toDegrees(Math.atan2(bx - ax, by - ay))
                        // at the left kerb, about 5.5 m from the middle of the road, arm reaching back over it
                        val at = Furniture.offset(LL(lat, lon), dir - 90.0, 5.5)
                        if (taken.add(key(at.lat, at.lon)) && Geo.dist(at, c) < 900.0) spots += doubleArrayOf(at.lat, at.lon, dir + 90.0)
                        d += gap
                    }
                    carry = d - len
                }
            }
        }
        val kept = spots.sortedBy { Geo.dist(LL(it[0], it[1]), c) }.take(90)
        val posts = kept.map { Furniture.point(it[0], it[1], it[2]) }
        // the lamp hangs about 2.5 m out along the arm
        val pools = kept.map { val h = Furniture.offset(LL(it[0], it[1]), it[2], 2.5); Feature.fromGeometry(Point.fromLngLat(h.lon, h.lat)) }
        val placedOnRoads = kept.size - minOf(kept.size, near.size)
        val note = when {
            kept.isEmpty() && roads.isEmpty() -> Lang.t("hier noch keine Straßen aus der Karte gelesen", "no streets read from the map here yet")
            kept.isEmpty() -> Lang.t("hier keine", "none here")
            mapped.isNotEmpty() && placedOnRoads > 0 -> Lang.t("${kept.size} angezeigt (${near.size} erfasst, $placedOnRoads entlang der Straßen gesetzt)", "${kept.size} shown (${near.size} mapped, $placedOnRoads placed along the streets)")
            mapped.isNotEmpty() -> Lang.t("${kept.size} angezeigt, aus deinem Offline-Paket", "${kept.size} shown, from your offline pack")
            !OfflineRouter.hasLamps(ctx) -> Lang.t("${kept.size} entlang der Straßen gesetzt (dein Offline-Paket hat keine Laternenliste)", "${kept.size} placed along the streets (your offline pack has no lamp list)")
            else -> Lang.t("${kept.size} entlang der Straßen gesetzt (hier keine erfasst)", "${kept.size} placed along the streets (none mapped here)")
        }
        return Triple(posts, pools, note)
    }

    /**
     * The traffic lights round [c] from the offline pack's list (the ones not already shown on your route, [onRoute]): each at the
     * left kerb of the road it stands on, facing the traffic coming up to it. The nearest 60.
     */
    fun nearLights(ctx: Context, c: LL, onRoute: List<LL>, roadInfo: List<MapBinding.RoadPiece>): List<Feature> {
        if (!OfflineRouter.hasSignals(ctx)) return emptyList()
        val dLat = 0.008; val dLon = dLat / Math.cos(Math.toRadians(c.lat))
        val pts = OfflineRouter.lamps(ctx, c.lat - dLat, c.lon - dLon, c.lat + dLat, c.lon + dLon, file = "signals.tsv", maxTiles = 64)
        val out = ArrayList<Feature>()
        for (q in pts.sortedBy { Geo.dist(LL(it[0], it[1]), c) }) {
            val p = LL(q[0], q[1])
            if (onRoute.any { Geo.dist(it, p) < 30.0 }) continue
            // (the road isn't known when the map hasn't shown it close up yet: the light then stands where it's mapped, facing north)
            val dir = roadDirAt(p, roadInfo)
            val at = if (dir != null) Furniture.offset(p, dir - 90.0, 4.5) else p
            out += Furniture.point(at.lat, at.lon, dir ?: 0.0)
            if (out.size >= 60) break
        }
        return out
    }

    /** The direction of the road at [p] (within 15 m), from the roads last read off the map, or null. */
    private fun roadDirAt(p: LL, roadInfo: List<MapBinding.RoadPiece>): Double? {
        val k = Math.cos(Math.toRadians(p.lat)) * 111320.0; val k2 = 110574.0
        var bd = 15.0; var dir: Double? = null
        for (rp in roadInfo) {
            val pts = rp.pts
            for (i in 0 until pts.size - 1) {
                val ax = (pts[i].longitude() - p.lon) * k; val ay = (pts[i].latitude() - p.lat) * k2
                val bx = (pts[i + 1].longitude() - p.lon) * k; val by = (pts[i + 1].latitude() - p.lat) * k2
                if ((ax > bd && bx > bd) || (ax < -bd && bx < -bd) || (ay > bd && by > bd) || (ay < -bd && by < -bd)) continue
                val dx = bx - ax; val dy = by - ay
                val l2 = dx * dx + dy * dy
                if (l2 < 0.01) continue
                val t = (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
                val d = Math.hypot(ax + dx * t, ay + dy * t)
                if (d < bd) { bd = d; dir = Math.toDegrees(Math.atan2(dx, dy)) }
            }
        }
        return dir
    }

    /** The way a mapped lamp's arm points: towards the nearest street within 25 m (else north). */
    private fun armTowardsRoad(lat: Double, lon: Double, roads: List<List<Point>>): Double {
        val k = Math.cos(Math.toRadians(lat)) * 111320.0; val k2 = 111320.0
        var bd = 25.0; var bx = 0.0; var by = 0.0; var found = false
        for (pts in roads) for (i in 0 until pts.size - 1) {
            val ax = (pts[i].longitude() - lon) * k; val ay = (pts[i].latitude() - lat) * k2
            val cx = (pts[i + 1].longitude() - lon) * k; val cy = (pts[i + 1].latitude() - lat) * k2
            if ((ax > bd && cx > bd) || (ax < -bd && cx < -bd) || (ay > bd && cy > bd) || (ay < -bd && cy < -bd)) continue
            val dx = cx - ax; val dy = cy - ay
            val l2 = dx * dx + dy * dy
            if (l2 < 0.01) continue
            val t = (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
            val px = ax + dx * t; val py = ay + dy * t
            val d = Math.hypot(px, py)
            if (d < bd && d > 0.3) { bd = d; bx = px; by = py; found = true }
        }
        return if (found) Math.toDegrees(Math.atan2(bx, by)) else 0.0
    }
}
