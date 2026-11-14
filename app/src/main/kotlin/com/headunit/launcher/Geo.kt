package com.headunit.launcher

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

data class LL(val lat: Double, val lon: Double)
data class Step(val off: Double, val maneuver: String, val street: String, val msg: String, val roundExit: Int = 0, val exitNo: String = "", val sign: String = "",
    /** At a roundabout: the way you're heading as you come onto it, the way you leave it, and the other roads off it (degrees; -1 unknown). */
    val rbIn: Double = -1.0, val rbOut: Double = -1.0, val rbExits: List<Double> = emptyList(),
    /** The motorway junction's name when the route data has it ("Frankfurter Kreuz", "Weiterstadt"); else it comes from OpenStreetMap (see Junctions). */
    val jct: String = "",
    /** Where a slip road leaves the main road, when this turn is at its far end (metres along the route; -1 when none). See [slipForks]. */
    val fork: Double = -1.0,
    /** At a roundabout: where the route leaves the ring (metres along the route; -1 when the route data doesn't say). */
    val rbEnd: Double = -1.0)

/**
 * Slip roads: a turn at the far end of a slip road often has no step of its own where the road leaves the motorway (no exit number).
 * The turn then counted down all the way to its far end, past the point where you had to move over. Here each such turn gets the end
 * of the motorway stretch just before it ([motorway] = stretches of motorway or trunk road along the route, in metres) as its [Step.fork].
 */
fun slipForks(steps: List<Step>, motorway: List<Pair<Double, Double>>): List<Step> {
    if (motorway.isEmpty()) return steps
    return steps.map { s ->
        val m = s.maneuver.uppercase()
        val turn = m.startsWith("TURN") || m.startsWith("SHARP") || m.startsWith("BEAR")
        if (!turn || m.contains("EXIT") || m.contains("RAMP")) return@map s
        // still on the motorway at the turn itself: not a slip road
        if (motorway.any { it.first <= s.off && s.off <= it.second }) return@map s
        // the motorway stretch that ends closest before the turn (slip roads are short)
        val end = motorway.filter { it.second <= s.off - 20.0 && it.second >= s.off - 1500.0 }.maxOfOrNull { it.second } ?: return@map s
        // an exit step in between already counts down from its own point
        if (steps.any { o -> o.off >= end - 1.0 && o.off < s.off - 1.0 && o.maneuver.uppercase().let { it.contains("EXIT") || it.contains("RAMP") } }) return@map s
        s.copy(fork = end)
    }
}

/** The exit a slip road starts with, on the side the turn at its end goes ("MOTORWAY_EXIT_LEFT"); other manoeuvres unchanged. */
fun slipExitManeuver(m: String): String {
    val u = m.uppercase()
    return when { u.contains("LEFT") -> "MOTORWAY_EXIT_LEFT"; u.contains("RIGHT") -> "MOTORWAY_EXIT_RIGHT"; else -> m }
}

data class Place(val name: String, val sub: String, val pos: LL) {
    /** Whether a search found this as a town, city or region, so those can come before streets (not saved, not compared). */
    var town: Boolean = false
    /** The shop's or brand's website when the search said ("lidl.de"), for its logo (see BrandLogos). Not saved, not compared. */
    var web: String? = null
    /** The brand or chain the search named ("Lidl"), if any. Not saved, not compared. */
    var brand: String? = null
    /** "Geöffnet bis 20:00" or "Geschlossen · öffnet 08:00" when the search gave opening times (see OpenHours). Not saved, not compared. */
    var hours: String? = null
}

/** The name of a spot held on the map with no place there: in the app's language (older ones were always "Dropped pin"). */
fun pinName(): String = Lang.t("Markierter Ort", "Dropped pin")
fun isPin(name: String): Boolean = name == "Dropped pin" || name == "Markierter Ort" || name == "Marked spot"

/** Marks a search result as a town, city or region (see [Place.town]). */
fun Place.town(yes: Boolean): Place = apply { town = yes }

class Incident(val id: String, val cat: String, val text: String, val delaySec: Int, val pts: List<LL>)
class Lane(val dirs: List<String>, val follow: String?)
class LaneSec(val off: Double, val end: Double, val lanes: List<Lane>)

class Route(
    val pts: List<LL>,
    val cum: DoubleArray,
    val steps: List<Step>,
    val len: Double,
    val time: Double,
    val delay: Double,
    lanes: List<LaneSec> = emptyList(),
    /** Traffic level per route segment (point i to i+1): 0 clear, 1 slow, 2 heavy. */
    val sev: IntArray = IntArray(0),
) {
    /** The lanes at each junction. Can be replaced after the route starts, when Mapbox's lane list arrives for a TomTom route. */
    @Volatile var lanes: List<LaneSec> = lanes
    /** Live delays along the route (from TomTom's traffic data), used to work out the time left. Replaced whenever fresh traffic arrives. */
    @Volatile var delays: List<DelaySeg> = emptyList()
    /** Seconds the whole route takes with no traffic at all. */
    @Volatile var freeFlow: Double = max(0.0, time - delay)
    /** Stretches of the route in tunnels, as (from, to) metres along it (from Mapbox's junction data; empty when not known). */
    @Volatile var tunnels: List<Pair<Double, Double>> = emptyList()
    /** Posted speed limits along the route from TomTom's road data (empty when the route came without them, e.g. offline). */
    @Volatile var limits: List<LimitSec> = emptyList()
    /** Incidents on the route that came with it (Mapbox routes carry them; TomTom's are looked up separately). */
    @Volatile var incidents: List<Incident> = emptyList()

    /** The box round the route (lat min, lon min, lat max, lon max): a quick way to rule a point out before the exact check. */
    val bounds: DoubleArray by lazy {
        var s = 90.0; var n = -90.0; var w = 180.0; var e = -180.0
        for (p in pts) { s = minOf(s, p.lat); n = maxOf(n, p.lat); w = minOf(w, p.lon); e = maxOf(e, p.lon) }
        doubleArrayOf(s, w, n, e)
    }

    /** The manoeuvres that are turns (not the start, the arrival, or going straight on). Counted once per route. */
    val turns: Int by lazy {
        steps.count { st ->
            val m = st.maneuver.uppercase()
            m != "DEPART" && !m.startsWith("ARRIVE") && m != "STRAIGHT" && m != "FOLLOW" && m != "CONTINUE"
        }
    }
    /** Whose route this is ("tomtom", "mapbox", "offline"): its speed limits are that service's own data. */
    @Volatile var source: String = ""
    /** The heights along the route (loaded after it, for the battery estimate), or null. */
    @Volatile var elev: Elevation.Profile? = null
    /** Countries (two-letter codes) where the route uses a toll road, as far as the routing service tells (Mapbox does). */
    @Volatile var tollCc: Set<String> = emptySet()
}

/** The posted limit [ms] (m/s) from [off] to [end] metres along a route. */
class LimitSec(val off: Double, val end: Double, val ms: Double)

/** A stretch of route [off]..[end] metres from the start that is slowed by [secs] seconds. */
class DelaySeg(val off: Double, val end: Double, val secs: Double)

/** A queue ahead on a route, at its start: its minutes, heavy (red) or slow (amber), and dimmed when it's on a route not being driven. */
data class QueueMark(val pos: LL, val min: Int, val heavy: Boolean, val dim: Boolean)

object Geo {
    /** The point [d] metres along a line ([cum]: the distance to each point) and the heading of the piece it is on. */
    fun pointOnLine(pts: List<LL>, cum: DoubleArray, d: Double): Pair<LL, Double> {
        var lo = 0; var hi = cum.size - 1
        while (lo < hi - 1) { val m = (lo + hi) ushr 1; if (cum[m] <= d) lo = m else hi = m }
        val seg = (cum[hi] - cum[lo]).takeIf { it > 0 } ?: 1.0
        val t = ((d - cum[lo]) / seg).coerceIn(0.0, 1.0)
        val a = pts[lo]; val b = pts[hi]
        return Pair(LL(a.lat + (b.lat - a.lat) * t, a.lon + (b.lon - a.lon) * t), bearing(a, b))
    }

    /**
     * A roundabout's way in and way out, read from the route line itself (the line the map draws), as headings in degrees:
     * the heading over the last 12 m before the ring ([entry]) and over the 30 m after leaving it. Where the route leaves the ring
     * is [exitAt] when the route data says so, otherwise where the line stops turning round the ring (anticlockwise where traffic
     * keeps right, clockwise where it keeps left). Null when the line is too short to tell.
     */
    /** For a mini roundabout with no ring in the line only: metres before the entry, and the exit's heading is taken between
     *  these distances after the exit point. (A real ring is measured by the arc the route covers on it.) */
    const val ROUNDABOUT_IN_M = 10.0
    const val ROUNDABOUT_OUT_FROM_M = 20.0
    const val ROUNDABOUT_OUT_TO_M = 40.0

    fun roundaboutHeadings(pts: List<LL>, cum: DoubleArray, entry: Double, exitAt: Double, limitAt: Double, leftTraffic: Boolean,
                           inM: Double = ROUNDABOUT_IN_M, outFromM: Double = ROUNDABOUT_OUT_FROM_M, outToM: Double = ROUNDABOUT_OUT_TO_M): Pair<Double, Double>? {
        if (pts.size < 2 || cum.size != pts.size) return null
        val len = cum.last()
        val e = entry.coerceIn(0.0, len)
        val limit = minOf(len, e + 250.0, limitAt)
        if (limit <= e + 5.0) return null
        val inH = bearing(pointOnLine(pts, cum, maxOf(0.0, e - inM.coerceIn(0.0, 50.0))).first, pointOnLine(pts, cum, e).first)
        // the line's heading every 4 m from the entry on, and how far it turns round the ring (in the ring's direction)
        val step = 4.0
        val hs = ArrayList<Double>()
        var o = e
        while (o <= limit) { hs += pointOnLine(pts, cum, o).second; o += step }
        val sign = if (leftTraffic) 1.0 else -1.0
        fun dlt(a: Double, b: Double) = ((b - a + 540.0) % 360.0) - 180.0
        val given = exitAt > e && exitAt <= limit
        var turned = 0.0
        var found = -1
        val win = 5
        for (i in 0 until hs.size - 1) {
            val d = sign * dlt(hs[i], hs[i + 1])
            if (given) {
                if (e + (i + 1) * step > exitAt) { found = i; break }
            } else if (turned >= 15.0) {
                // turning out of the ring (against its direction), or not turning round it any more
                if (d < -2.0) { found = i; break }
                if (i + win <= hs.size - 1 && (i until i + win).all { j -> sign * dlt(hs[j], hs[j + 1]) <= 1.0 }) { found = i; break }
            }
            if (d > 0) turned += d
        }
        // How far round the ring the route goes is what the map shows. Its heading as it comes in isn't a good guide to that:
        // many entries meet the ring at a slant rather than head-on, which made the exit look 45 degrees or more further round.
        // On the ring the line turns by just the arc it covers, so that sets the exit (180 minus the arc: 270 round is a left turn)
        if ((given || found >= 0) && turned >= 30.0) {
            // (how much the line turns on the ring falls short of the arms' angle: the way in joins the ring past its arm and the
            // way out leaves before its arm, so a third exit to the left read as about 200 rather than 270. The arms' places
            // round the ring's middle are used where the ring can be made out in the line.)
            val exitO = if (given) exitAt else e + found * step
            val byCircle = try { ringArc(pts, cum, e, exitO, leftTraffic) } catch (ex: Exception) { null }
            rbNote = "line: turned ${Math.round(turned)}, by circle ${byCircle?.let { Math.round(it).toString() } ?: "none"}"
            val arc = byCircle ?: turned.coerceAtMost(360.0)
            val turn = if (leftTraffic) arc - 180.0 else 180.0 - arc
            return 0.0 to ((turn % 360.0) + 360.0) % 360.0
        }
        // no ring in the line (a mini roundabout drawn as a single point): the way in and the way out a little way on
        val exit = if (given) exitAt else if (found >= 0) e + found * step else e + 25.0
        val from = minOf(len, exit + outFromM.coerceIn(0.0, 50.0))
        val outEnd = minOf(len, exit + maxOf(outToM.coerceIn(5.0, 60.0), outFromM.coerceIn(0.0, 50.0) + 5.0))
        if (outEnd - from < 5.0) return null
        val outH = bearing(pointOnLine(pts, cum, from).first, pointOnLine(pts, cum, outEnd).first)
        return inH to outH
    }

    /** What the last line-based roundabout measure did, for the feedback report. */
    @Volatile var rbNote = ""

    /**
     * How far round a roundabout the route goes, from the route line alone: a circle is laid through the stretch of the line on
     * the ring ([e] to [x] metres along it), and the way in and the way out are read 15 m outside that circle (at most 40 m
     * along the line), as angles round its middle. Null when the stretch doesn't make a circle.
     */
    fun ringArc(pts: List<LL>, cum: DoubleArray, e: Double, x: Double, leftTraffic: Boolean): Double? {
        val len = cum.last()
        if (x - e < 6.0) return null
        val p0 = pointOnLine(pts, cum, e).first
        val kx = 111320.0 * cos(rad(p0.lat)); val ky = 110540.0
        fun xy(o: Double): DoubleArray { val p = pointOnLine(pts, cum, o.coerceIn(0.0, len)).first; return doubleArrayOf((p.lon - p0.lon) * kx, (p.lat - p0.lat) * ky) }
        val ps = ArrayList<DoubleArray>()
        var o = e
        while (o <= x) { ps += xy(o); o += 2.0 }
        val n = ps.size
        if (n < 4) return null
        val mx = ps.sumOf { it[0] } / n; val my = ps.sumOf { it[1] } / n
        var suu = 0.0; var svv = 0.0; var suv = 0.0; var suuu = 0.0; var svvv = 0.0; var suvv = 0.0; var svuu = 0.0
        for (q in ps) {
            val u = q[0] - mx; val v = q[1] - my
            suu += u * u; svv += v * v; suv += u * v; suuu += u * u * u; svvv += v * v * v; suvv += u * v * v; svuu += v * u * u
        }
        val det = suu * svv - suv * suv
        if (abs(det) < 1e-6) return null
        val b1 = (suuu + suvv) / 2.0; val b2 = (svvv + svuu) / 2.0
        val uc = (b1 * svv - b2 * suv) / det; val vc = (b2 * suu - b1 * suv) / det
        val r = sqrt(uc * uc + vc * vc + (suu + svv) / n)
        if (r < 4.0 || r > 90.0) return null
        val cx = mx + uc; val cy = my + vc
        fun dc(q: DoubleArray) = sqrt((q[0] - cx).pow(2) + (q[1] - cy).pow(2))
        // (the stretch has to lie on the circle: a line that isn't a ring gives a circle it doesn't follow)
        if (sqrt(ps.sumOf { (dc(it) - r).pow(2) } / n) > 3.0) return null
        var oi = e
        while (oi > max(0.0, e - 40.0) && dc(xy(oi)) < r + 15.0) oi -= 2.0
        var oo = x
        while (oo < min(len, x + 40.0) && dc(xy(oo)) < r + 15.0) oo += 2.0
        val qi = xy(oi); val qo = xy(oo)
        val a0 = deg(atan2(qi[1] - cy, qi[0] - cx)); val a1 = deg(atan2(qo[1] - cy, qo[0] - cx))
        val raw = if (leftTraffic) ((a0 - a1) % 360.0 + 360.0) % 360.0 else ((a1 - a0) % 360.0 + 360.0) % 360.0
        if (raw < 30.0 || raw > 330.0) return if (x - e > PI * r) 360.0 else null
        return raw
    }

    fun rad(d: Double) = d * PI / 180.0

    private val abbr = mapOf("Close" to "Cl", "Road" to "Rd", "Street" to "St", "Avenue" to "Ave", "Drive" to "Dr", "Lane" to "Ln", "Crescent" to "Cres",
        "Gardens" to "Gdns", "Court" to "Ct", "Place" to "Pl", "Terrace" to "Ter", "Grove" to "Gr", "Way" to "Way", "Walk" to "Wk", "Square" to "Sq", "Park" to "Pk")

    /** Short form of a road name for the little pill under the locator: "Duddon Close" becomes "Duddon Cl". */
    fun shortRoad(n: String): String {
        val w = n.trim().split(" ")
        if (w.size < 2) return n.trim()
        val last = abbr[w.last()] ?: return n.trim()
        return (w.dropLast(1) + last).joinToString(" ")
    }
    fun deg(r: Double) = r * 180.0 / PI

    fun dist(a: LL, b: LL): Double {
        val r = 6371000.0
        val dLat = rad(b.lat - a.lat)
        val dLon = rad(b.lon - a.lon)
        val x = sin(dLat / 2).pow(2) + cos(rad(a.lat)) * cos(rad(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * r * asin(sqrt(x))
    }

    fun bearing(a: LL, b: LL): Double {
        val y = sin(rad(b.lon - a.lon)) * cos(rad(b.lat))
        val x = cos(rad(a.lat)) * sin(rad(b.lat)) - sin(rad(a.lat)) * cos(rad(b.lat)) * cos(rad(b.lon - a.lon))
        return (deg(atan2(y, x)) + 360.0) % 360.0
    }

    fun fmtDist(m: Double, imperial: Boolean): String {
        if (imperial) {
            val ft = m * 3.28084
            if (ft < 100) return "${max(10, ((ft / 10).roundToInt()) * 10)} ft"
            if (ft < 528) return "${max(100, ((ft / 50).roundToInt()) * 50)} ft"
            val mi = m / 1609.344
            return if (mi < 10) String.format("%.1f mi", mi) else "${mi.roundToInt()} mi"
        }
        if (m < 1000) {
            val v = if (m < 100) max(10, ((m / 10).roundToInt()) * 10) else ((m / 50).roundToInt()) * 50
            return "$v m"
        }
        val k = m / 1000.0
        return if (k < 10) String.format("%.1f km", k) else "${k.roundToInt()} km"
    }

    fun fmtDur(sec: Double): String {
        val m = max(0, (sec / 60).roundToInt())
        return if (m < 60) "$m min" else "${m / 60} h ${(m % 60).toString().padStart(2, '0')} min"
    }

    fun spoken(m: Double, imperial: Boolean): String {
        if (imperial) {
            val ft = m * 3.28084
            return if (ft < 528) "${((ft / 50).roundToInt()) * 50} Fuß" else String.format("%.1f Meilen", m / 1609.344).replace('.', ',')
        }
        val r50 = ((m / 50).roundToInt()) * 50
        if (r50 < 1000) return "$r50 Metern"
        // "In einem Kilometer", "In 2 Kilometern", "In 1,5 Kilometern" (never "1,0 Kilometern")
        val tenths = (m / 100.0).roundToInt()
        if (tenths == 10) return "einem Kilometer"
        if (tenths >= 100) return "${(m / 1000.0).roundToInt()} Kilometern"
        if (tenths % 10 == 0) return "${tenths / 10} Kilometern"
        return "${tenths / 10},${tenths % 10} Kilometern"
    }

    /** [spoken] for use without "in" ("Noch 3 Kilometer", not "Noch 3 Kilometern"). */
    fun spokenPlain(m: Double, imperial: Boolean): String =
        spoken(m, imperial).replace("einem Kilometer", "ein Kilometer").replace("Kilometern", "Kilometer").replace("Metern", "Meter")

    fun ordinal(n: Int): String = "$n."

    /** "erste", "zweite"… for the spoken roundabout exit (the screen keeps the short "2. Ausfahrt"). */
    private fun ordinalWord(n: Int): String = when (n) {
        1 -> "erste"; 2 -> "zweite"; 3 -> "dritte"; 4 -> "vierte"; 5 -> "fünfte"; 6 -> "sechste"; 7 -> "siebte"; 8 -> "achte"
        else -> ordinal(n)
    }

    /** Spoken/short wording for a manoeuvre, e.g. "turn left" or "take the 2nd exit at the roundabout". */
    fun maneuverText(m: String, exit: Int = 0): String {
        val u = m.uppercase()
        if (u.startsWith("ROUNDABOUT")) {
            if (u == "ROUNDABOUT_BACK") return if (exit > 0) "im Kreisverkehr die ${ordinalWord(exit)} Ausfahrt nehmen" else "einmal ganz um den Kreisverkehr fahren"
            return if (exit > 0) "im Kreisverkehr die ${ordinalWord(exit)} Ausfahrt nehmen" else "in den Kreisverkehr einfahren"
        }
        val t = mapOf(
            "ARRIVE" to "Ziel erreicht", "ARRIVE_LEFT" to "Ziel erreicht, links", "ARRIVE_RIGHT" to "Ziel erreicht, rechts",
            "DEPART" to "losfahren", "MAKE_UTURN" to "wenden", "TRY_MAKE_UTURN" to "wenden, wenn möglich",
            "STRAIGHT" to "geradeaus weiterfahren", "FOLLOW" to "weiterfahren", "TURN_LEFT" to "links abbiegen", "TURN_RIGHT" to "rechts abbiegen",
            "SHARP_LEFT" to "scharf links abbiegen", "SHARP_RIGHT" to "scharf rechts abbiegen", "BEAR_LEFT" to "halb links halten", "BEAR_RIGHT" to "halb rechts halten",
            "KEEP_LEFT" to "links halten", "KEEP_RIGHT" to "rechts halten", "ENTER_MOTORWAY" to "auf die Autobahn auffahren", "ENTER_FREEWAY" to "auf die Schnellstraße auffahren",
            "ENTER_HIGHWAY" to "auf die Straße auffahren", "TAKE_EXIT" to "die Ausfahrt nehmen", "MOTORWAY_EXIT_LEFT" to "die Ausfahrt links nehmen",
            "MOTORWAY_EXIT_RIGHT" to "die Ausfahrt rechts nehmen", "ENTRANCE_RAMP" to "die Auffahrt nehmen", "SWITCH_PARALLEL_ROAD" to "auf die Parallelstraße wechseln",
            "SWITCH_MAIN_ROAD" to "auf die Hauptstraße wechseln", "TAKE_FERRY" to "die Fähre nehmen",
        )
        return t[u] ?: u.lowercase().replace('_', ' ')
    }

    private fun laneFits(dir: String, maneuver: String): Boolean {
        val m = maneuver.uppercase()
        return when {
            m.contains("U_TURN") -> dir.endsWith("U_TURN")
            m.contains("LEFT") -> dir.contains("LEFT")
            m.contains("RIGHT") -> dir.contains("RIGHT")
            else -> dir == "STRAIGHT"
        }
    }

    /** "Stay in the left two lanes" and similar, from the lanes that lead where the route goes. Null when every lane works. */
    fun laneAdvice(sec: LaneSec, maneuver: String): String? {
        val n = sec.lanes.size
        if (n < 2) return null
        val anyFollow = sec.lanes.any { it.follow != null }
        // a roundabout without Mapbox's own lane marks is not guessed at: the lanes' directions don't say which exit they lead to
        if (!anyFollow && maneuver.uppercase().startsWith("ROUNDABOUT")) return null
        val act = sec.lanes.indices.filter { i -> if (anyFollow) sec.lanes[i].follow != null else sec.lanes[i].dirs.any { laneFits(it, maneuver) } }
        if (act.isEmpty() || act.size == n) return null
        val cnt = act.size
        val lanes = when (cnt) { 1 -> "eine Spur"; 2 -> "zwei Spuren"; 3 -> "drei Spuren"; else -> "$cnt Spuren" }
        return when {
            act.first() == 0 -> "Links $lanes nutzen"
            act.last() == n - 1 -> "Rechts $lanes nutzen"
            cnt == 1 -> "${act[0] + 1}te Spur von links nutzen"
            else -> "Mittlere $lanes nutzen"
        }
    }

    /** Short caption shown on the turn card for a roundabout or motorway exit, or blank. */
    fun stepTag(s: Step): String = when {
        s.maneuver.uppercase().startsWith("ROUNDABOUT") && s.roundExit > 0 -> "${ordinal(s.roundExit)} Ausfahrt"
        s.exitNo.isNotBlank() -> "Ausfahrt ${s.exitNo}"
        else -> ""
    }

    /** Angle in degrees (negative = left) used to draw the manoeuvre arrow. */
    fun maneuverAngle(m: String): Float {
        val u = m.uppercase()
        val table = mapOf(
            "TURN_LEFT" to -90f, "TURN_RIGHT" to 90f, "SHARP_LEFT" to -140f, "SHARP_RIGHT" to 140f,
            "BEAR_LEFT" to -35f, "BEAR_RIGHT" to 35f, "KEEP_LEFT" to -22f, "KEEP_RIGHT" to 22f,
            "STRAIGHT" to 0f, "DEPART" to 0f, "FOLLOW" to 0f, "ROUNDABOUT_LEFT" to -90f,
            "ROUNDABOUT_RIGHT" to 90f, "ROUNDABOUT_STRAIGHT" to 0f, "ROUNDABOUT_CROSS" to 0f,
            "ROUNDABOUT_BACK" to 180f, "MAKE_UTURN" to 180f,
        )
        table[u]?.let { return it }
        if (u.contains("LEFT")) return -40f
        if (u.contains("RIGHT")) return 40f
        return 0f
    }
}


object TomTomRest {
    private const val API = "https://api.tomtom.com/maps/orbis"
    private const val CLASSIC = "https://api.tomtom.com/routing/1/calculateRoute"
    private const val SEARCH = "https://api.tomtom.com/search/2/search"

    fun styleUrl(key: String, dark: Boolean, traffic: Boolean): String {
        val d = if (dark) "dark" else "light"
        val k = URLEncoder.encode(key, "UTF-8")
        return "$API/assets/styles/0.*/style.json?key=$k&apiVersion=1&map=basic_street-$d-driving" +
            (if (traffic) "&trafficFlow=flow_relative-$d" else "")
    }

    /** A failed request, explained in plain words for the common cases (a missing or wrong key, the daily limit). */
    private fun httpError(code: Int, body: String): IOException = IOException(when (code) {
        401, 403 -> Lang.t("TomTom hat den API-Schlüssel abgelehnt. Prüfe ihn unter Einstellungen > Schlüssel & Konten (ein kostenloser Schlüssel von developer.tomtom.com braucht die Search- und Routing-APIs)", "TomTom didn't accept the API key. Check it in Settings > Keys & accounts (a free key from developer.tomtom.com needs the Search and Routing APIs)")
        429 -> Lang.t("Dieser TomTom-Schlüssel hat sein Tageslimit erreicht. Es wird morgen zurückgesetzt", "This TomTom key has reached its daily limit. It resets tomorrow")
        else -> "HTTP $code ${body.take(140)}"
    })

    private fun get(url: String, connectMs: Int = 15000, readMs: Int = 25000): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs
        c.readTimeout = readMs
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw httpError(code, body)
        return body
    }

    private fun parsePlaces(arr: JSONArray, q: String, out: MutableList<Place>) {
        for (i in 0 until arr.length()) {
            val x = arr.getJSONObject(i)
            val pos = x.optJSONObject("position") ?: continue
            val addr = x.optJSONObject("address")
            val poi = x.optJSONObject("poi")
            val free = addr?.optString("freeformAddress").orEmpty()
            val name = poi?.optString("name")?.takeIf { it.isNotBlank() } ?: free.ifBlank { q }
            val sub = if (poi != null) free else addr?.optString("country").orEmpty()
            val p = Place(name, sub, LL(pos.getDouble("lat"), pos.getDouble("lon"))).town(poi == null && x.optString("type") == "Geography")
            if (poi != null) {
                p.web = poi.optString("url").takeIf { it.isNotBlank() }
                p.brand = poi.optJSONArray("brands")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() }
                p.hours = try { OpenHours.fromTomTom(poi) } catch (e: Exception) { null }
            }
            if (out.none { it.name == p.name && Geo.dist(it.pos, p.pos) < 60 }) out += p
        }
    }

    /**
     * Place search. Tries the fuzzy search (businesses, streets, towns, postcodes) restricted to [country] and biased towards [near],
     * then the Orbis geocoder, then OpenStreetMap's Nominatim, so a name that one source does not know is still found.
     * [country] is an ISO code such as GB; blank means anywhere.
     */
    fun geocode(key: String, country: String, q: String, near: LL?): List<Place> {
        val k = URLEncoder.encode(key, "UTF-8")
        val enc = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        val cc = country.trim().uppercase()
        val out = ArrayList<Place>()
        // 1. classic fuzzy search
        try {
            var url = "$SEARCH/$enc.json?key=$k&limit=15&language=${if (Lang.de) "de-DE" else "en-GB"}&typeahead=true&minFuzzyLevel=1&maxFuzzyLevel=2&idxSet=POI,Addr,Str,Xstr,Geo,PAD&openingHours=nextSevenDays"
            if (cc.isNotEmpty()) url += "&countrySet=$cc"
            if (near != null) url += "&lat=${near.lat}&lon=${near.lon}"
            parsePlaces(JSONObject(get(url)).optJSONArray("results") ?: JSONArray(), q, out)
        } catch (e: Exception) { /* fall through to the next source */ }
        // 2. Orbis geocoder
        if (out.size < 4) try {
            var url = "$API/places/geocode/$enc.json?key=$k&apiVersion=1&limit=8"
            if (cc.isNotEmpty()) url += "&countrySet=$cc"
            if (near != null) url += "&lat=${near.lat}&lon=${near.lon}"
            parsePlaces(JSONObject(get(url)).optJSONArray("results") ?: JSONArray(), q, out)
        } catch (e: Exception) { }
        // 3. Nominatim
        if (out.size < 8) try {
            var url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=10&q=$enc"
            if (near != null) url += "&viewbox=${near.lon - 0.5},${near.lat + 0.35},${near.lon + 0.5},${near.lat - 0.35}"
            if (cc.isNotEmpty()) url += "&countrycodes=${cc.lowercase()}"
            val arr = JSONArray(get(url))
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                val disp = x.optString("display_name")
                val p = Place(x.optString("name").ifBlank { disp.substringBefore(',') }, disp.substringAfter(", ", ""), LL(x.getDouble("lat"), x.getDouble("lon"))).town(SearchRank.nominatimTown(x))
                if (out.none { Geo.dist(it.pos, p.pos) < 60 }) out += p
            }
        } catch (e: Exception) { }
        return out
    }

    /** True when the last request was accepted with its requested departure time. */
    @Volatile var departApplied = false

    /** Route preferences applied to every classic routing request: avoid=tollRoads|motorways|ferries|unpavedRoads. */
    @Volatile var avoid: List<String> = emptyList()
    @Volatile var routeType: String = "fastest"
    /** Extra settings for an electric car's eco route (empty otherwise): tried first, and dropped if TomTom won't take them. */
    @Volatile var evParams: String = ""
    /** Areas the route must keep out of (clean air zones, a road you asked to avoid): south-west and north-east corners. */
    @Volatile var avoidRects: List<Pair<LL, LL>> = emptyList()
    /** True when the last request had to drop the areas to avoid to find a route at all. */
    @Volatile var avoidDropped = false

    private fun post(url: String, body: String, connectMs: Int, readMs: Int): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = connectMs
        c.readTimeout = readMs
        c.requestMethod = "POST"
        c.doOutput = true
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw httpError(code, text)
        return text
    }

    private fun avoidBody(rects: List<Pair<LL, LL>>): String {
        val arr = JSONArray()
        for ((sw, ne) in rects.take(10)) arr.put(JSONObject()
            .put("southWestCorner", JSONObject().put("latitude", sw.lat).put("longitude", sw.lon))
            .put("northEastCorner", JSONObject().put("latitude", ne.lat).put("longitude", ne.lon)))
        return JSONObject().put("avoidAreas", JSONObject().put("rectangles", arr)).toString()
    }

    /** False once TomTom has refused a request for speed limit sections (then routes are asked for without them). */
    @Volatile private var speedSecOk = true

    fun routes(key: String, from: LL, to: LL, alternatives: Boolean, departAtMs: Long = 0L, via: List<LL> = emptyList(), headingDeg: Double = -1.0, quick: Boolean = false): List<Route> {
        val rects = avoidRects
        avoidDropped = false
        if (rects.isEmpty()) return routesWith(key, from, to, alternatives, departAtMs, via, headingDeg, quick, null)
        return try { routesWith(key, from, to, alternatives, departAtMs, via, headingDeg, quick, avoidBody(rects)) }
        catch (e: IOException) {
            val m = e.message ?: ""
            // no way round the areas (or the request was refused): route normally and say so
            if (m == "no signal" || m.startsWith("too many")) throw e
            val r = routesWith(key, from, to, alternatives, departAtMs, via, headingDeg, quick, null)
            avoidDropped = true
            r
        }
    }

    private fun routesWith(key: String, from: LL, to: LL, alternatives: Boolean, departAtMs: Long, via: List<LL>, headingDeg: Double, quick: Boolean, avoidJson: String?): List<Route> {
        val k = URLEncoder.encode(key, "UTF-8")
        val loc = (listOf(from) + via + listOf(to)).joinToString(":") { "${it.lat},${it.lon}" }
        val n = if (alternatives) 2 else 0
        val base = "$API/routing/calculateRoute/$loc/json?key=$k&apiVersion=2&maxAlternatives=$n"
        val iso = if (departAtMs > 0) java.net.URLEncoder.encode(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date(departAtMs)), "UTF-8") else ""
        val dep = if (iso.isNotEmpty()) "&departAt=$iso" else ""
        val lang = "&language=" + URLEncoder.encode(java.util.Locale.getDefault().toLanguageTag(), "UTF-8")
        // The classic routing API is tried first: it returns turn-by-turn instructions, roundabout exits and lane data.
        // The newer Orbis routing API does not return any guidance, so it is only a fallback for the line itself.
        val prefs = avoid.joinToString("") { "&avoid=$it" } + (if (routeType != "fastest") "&routeType=$routeType" else "")
        // the direction you are travelling stops the route sending you the wrong way down the road you are on (used when rerouting)
        val hd = if (headingDeg >= 0) "&vehicleHeading=${headingDeg.toInt() % 360}" else ""
        val classic = "$CLASSIC/$loc/json?key=$k&maxAlternatives=$n&traffic=true&routeRepresentation=polyline&instructionsType=text$prefs$hd${Vehicle.tomtomParams()}"
        val attempts = ArrayList<String>()
        if (quick) {
            // rerouting: only two quick tries, so a weak signal does not leave you waiting through ten slow requests
            attempts += "$classic$lang&sectionType=traffic&sectionType=motorway&sectionType=lanes"
            attempts += classic
        } else {
        if (dep.isNotEmpty()) {
            attempts += "$classic$dep$lang&sectionType=traffic&sectionType=motorway&sectionType=lanes"
            attempts += "$classic$dep&sectionType=traffic&sectionType=motorway&sectionType=lanes"
            attempts += "$classic$dep"
        }
        if (evParams.isNotEmpty()) attempts += "$classic$lang&sectionType=traffic&sectionType=motorway&sectionType=lanes$evParams"
        attempts += "$classic$lang&sectionType=traffic&sectionType=motorway&sectionType=lanes"
        attempts += "$classic&sectionType=traffic&sectionType=motorway&sectionType=lanes"
        attempts += "$classic&sectionType=traffic&sectionType=motorway"
        attempts += classic
        // the Orbis fallback cannot take areas to avoid, so it is only used for ordinary requests
        if (avoidJson == null) {
            if (dep.isNotEmpty()) attempts += "$base$dep&sectionType=traffic&sectionType=motorway"
            attempts += "$base&traffic=live&sectionType=traffic&sectionType=motorway"
            attempts += base
        }
        }
        // ask for the posted speed limits along the route too (exact for the road you are actually on); if this server ever
        // refuses that, it is remembered and not asked again
        if (speedSecOk) {
            val withLimits = ArrayList<String>()
            for (u in attempts) { if (u.contains("sectionType=lanes")) withLimits += "$u&sectionType=speedLimit"; withLimits += u }
            attempts.clear(); attempts.addAll(withLimits)
        }
        var body: String? = null
        var last: IOException? = null
        departApplied = false
        for ((idx, u) in attempts.withIndex()) {
            try {
                body = if (avoidJson != null && u.startsWith(CLASSIC)) post(u, avoidJson, if (quick) 8000 else 15000, if (quick) 12000 else 25000)
                else get(u, if (quick) 8000 else 15000, if (quick) 12000 else 25000)
                departApplied = departAtMs > 0 && u.contains("departAt="); break
            }
            catch (e: IOException) {
                last = e
                val m = e.message ?: ""
                if (m.startsWith("HTTP 400") && u.contains("sectionType=speedLimit")) speedSecOk = false
                // no signal, a bad key or too many requests: trying the other request variants cannot help (and only uses up the quota)
                if (!m.startsWith("HTTP") || m.startsWith("HTTP 401") || m.startsWith("HTTP 403") || m.startsWith("HTTP 429")) break
            }
        }
        if (body == null) {
            val m = last?.message ?: ""
            throw IOException(if (!m.startsWith("HTTP")) "no signal" else if (m.startsWith("HTTP 429")) "too many requests, try again in a moment" else m.ifBlank { "Routing failed" })
        }
        val arr = JSONObject(body).optJSONArray("routes") ?: JSONArray()
        return (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }

    /** What to ask TomTom's search for each kind of place. */
    private fun poiQuery(kind: String) = when (kind) {
        "fuel" -> "petrol station"; "charging" -> "electric vehicle station"; "parking" -> "parking"
        "food" -> "restaurant"; "coffee" -> "cafe"; "toilets" -> "toilet"; "services" -> "rest area"; else -> kind
    }

    private fun poiResult(x: JSONObject, kind: String, from: LL): Nearby? {
        val pos = x.optJSONObject("position") ?: return null
        val p = LL(pos.getDouble("lat"), pos.getDouble("lon"))
        val poi = x.optJSONObject("poi")
        val addr = x.optJSONObject("address")
        val name = poi?.optString("name")?.takeIf { it.isNotBlank() } ?: Overpass.kindLabel(kind)
        val extra = ArrayList<String>()
        poi?.optJSONArray("brands")?.optJSONObject(0)?.optString("name")?.takeIf { it.isNotBlank() && !name.contains(it, true) }?.let { extra += it }
        poi?.optJSONArray("categories")?.optString(0)?.takeIf { it.isNotBlank() && kind in setOf("food", "coffee") }?.let { extra += it.replaceFirstChar { c -> c.uppercase() } }
        val street = addr?.optString("streetName").orEmpty().ifBlank { addr?.optString("municipality").orEmpty() }
        val d = if (x.has("dist")) x.optDouble("dist", 0.0) else Geo.dist(from, p)
        // charging: TomTom lists the connectors with their rated power; the fastest one sets the charge time
        var kw: Int? = null
        if (kind == "charging") {
            val cons = x.optJSONObject("chargingPark")?.optJSONArray("connectors")
            if (cons != null) for (i in 0 until cons.length()) {
                val r = cons.optJSONObject(i)?.optDouble("ratedPowerKW", 0.0) ?: 0.0
                if (!r.isNaN() && r > 0 && r <= 1500 && Math.round(r).toInt() > (kw ?: 0)) kw = Math.round(r).toInt()
            }
        }
        // TomTom puts a charger's operator (or the network it belongs to) in its brands
        val op = if (kind == "charging") poi?.optJSONArray("brands")?.optJSONObject(0)?.optString("name")?.trim()?.takeIf { it.isNotEmpty() } else null
        return Nearby(Place(name, street, p), d, extra.joinToString(" · "), kw = kw, op = op)
    }

    /** Places of a kind around [c] from TomTom's search (fast, and works wherever there is signal). */
    fun poiNearby(key: String, kind: String, c: LL, radius: Int): List<Nearby> {
        val k = URLEncoder.encode(key, "UTF-8")
        val q = URLEncoder.encode(poiQuery(kind), "UTF-8").replace("+", "%20")
        val body = get("https://api.tomtom.com/search/2/categorySearch/$q.json?key=$k&lat=${c.lat}&lon=${c.lon}&radius=$radius&limit=25&language=${if (Lang.de) "de-DE" else "en-GB"}", 8000, 12000)
        val arr = JSONObject(body).optJSONArray("results") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { poiResult(arr.getJSONObject(it), kind, c) }
    }

    /**
     * Shops and places near [near] for the search box: [kind] (TomTom's category word) finds the places of that kind, otherwise
     * [q] is matched against names and brands. Within [radius] metres, nearest first.
     */
    fun poiNear(key: String, q: String, kind: String?, near: LL, radius: Int): List<Place> {
        val k = URLEncoder.encode(key, "UTF-8")
        val enc = URLEncoder.encode(kind ?: q.trim(), "UTF-8").replace("+", "%20")
        val api = if (kind != null) "categorySearch" else "poiSearch"
        val url = "https://api.tomtom.com/search/2/$api/$enc.json?key=$k&lat=${near.lat}&lon=${near.lon}&radius=$radius&limit=25" +
            "&language=${if (Lang.de) "de-DE" else "en-GB"}&openingHours=nextSevenDays" + (if (kind == null) "&typeahead=true" else "")
        val out = ArrayList<Place>()
        parsePlaces(JSONObject(get(url, 8000, 9000)).optJSONArray("results") ?: JSONArray(), q, out)
        return out.filter { Geo.dist(near, it.pos) <= radius * 1.2 }.sortedBy { Geo.dist(near, it.pos) }
    }

    /** Places of a kind along a route line, with how many seconds each detour adds (TomTom's along-route search). */
    fun poiAlong(key: String, kind: String, line: List<LL>, maxDetourSec: Int = 900): List<Pair<Nearby, Int>> {
        if (line.size < 2) return emptyList()
        val k = URLEncoder.encode(key, "UTF-8")
        val q = URLEncoder.encode(poiQuery(kind), "UTF-8").replace("+", "%20")
        val pts = JSONArray()
        for (p in line) pts.put(JSONObject().put("lat", p.lat).put("lon", p.lon))
        val req = JSONObject().put("route", JSONObject().put("points", pts)).toString()
        val body = post("https://api.tomtom.com/search/2/searchAlongRoute/$q.json?key=$k&maxDetourTime=$maxDetourSec&limit=20&spreadingMode=auto", req, 8000, 15000)
        val arr = JSONObject(body).optJSONArray("results") ?: JSONArray()
        return (0 until arr.length()).mapNotNull { i ->
            val x = arr.getJSONObject(i)
            poiResult(x, kind, line.first())?.let { it to x.optInt("detourTime", -1) }
        }
    }

    /** Seconds to drive from [from] to [to] with today's traffic (no route line asked for), or null. */
    fun driveTime(key: String, from: LL, to: LL): Double? {
        val k = URLEncoder.encode(key, "UTF-8")
        val body = get("$CLASSIC/${from.lat},${from.lon}:${to.lat},${to.lon}/json?key=$k&traffic=true&routeRepresentation=none", 8000, 12000)
        return JSONObject(body).optJSONArray("routes")?.optJSONObject(0)?.optJSONObject("summary")?.optDouble("travelTimeInSeconds", -1.0)?.takeIf { it > 0 }
    }

    /** When to leave to arrive at [arriveAtMs] (TomTom's predicted traffic for that time), or null if it can't say. */
    fun departureFor(key: String, from: LL, to: LL, via: List<LL>, arriveAtMs: Long): Long? {
        val k = URLEncoder.encode(key, "UTF-8")
        val loc = (listOf(from) + via + listOf(to)).joinToString(":") { "${it.lat},${it.lon}" }
        val iso = URLEncoder.encode(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).format(java.util.Date(arriveAtMs)), "UTF-8")
        val prefs = avoid.joinToString("") { "&avoid=$it" } + (if (routeType != "fastest") "&routeType=$routeType" else "")
        val body = get("$CLASSIC/$loc/json?key=$k&arriveAt=$iso&traffic=true&routeRepresentation=none$prefs", 10000, 20000)
        val sum = JSONObject(body).optJSONArray("routes")?.optJSONObject(0)?.optJSONObject("summary") ?: return null
        val dep = sum.optString("departureTime")
        if (dep.isNotBlank()) try { return java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", java.util.Locale.US).parse(dep)?.time } catch (e: Exception) { }
        val secs = sum.optDouble("travelTimeInSeconds", -1.0)
        return if (secs > 0) arriveAtMs - (secs * 1000).toLong() else null
    }

    fun incidents(key: String, minLon: Double, minLat: Double, maxLon: Double, maxLat: Double): List<Incident> {
        val url = "$API/traffic/incidents/details?apiVersion=2&timeValidity=present&bbox=$minLon,$minLat,$maxLon,$maxLat"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000; c.readTimeout = 25000
        c.setRequestProperty("TomTom-Api-Key", key)
        c.setRequestProperty("Attributes", "incidents(geometry(type,coordinates),properties(id,iconCategory,magnitudeOfDelay,delay,from,to,roadNumbers,events(description)))")
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code ${body.take(120)}")
        val arr = JSONObject(body).optJSONArray("incidents") ?: JSONArray()
        val out = ArrayList<Incident>()
        for (i in 0 until arr.length()) {
            try {
                val o = arr.optJSONObject(i) ?: continue
                val pr = o.getJSONObject("properties")
                val g = o.getJSONObject("geometry")
                val co = g.getJSONArray("coordinates")
                val pts = ArrayList<LL>()
                if (g.optString("type") == "Point") pts += LL(co.getDouble(1), co.getDouble(0))
                else for (n in 0 until co.length()) { val q = co.getJSONArray(n); pts += LL(q.getDouble(1), q.getDouble(0)) }
                if (pts.isEmpty()) continue
                val ev = pr.optJSONArray("events")?.optJSONObject(0)?.optString("description").orEmpty()
                if (Regex("\\bfire\\b", RegexOption.IGNORE_CASE).containsMatchIn(ev)) continue
                // TomTom gives the kind as a number and the hold-up as "delay" (seconds): read as a word and as "delayInSeconds" before,
                // so jams, closures and roadworks weren't known as such and every delay was 0
                val cat = when (pr.optInt("iconCategory", -1)) {
                    1 -> "accident"; 6 -> "jam"; 7 -> "laneClosed"; 8 -> "roadClosed"; 9 -> "roadWorks"
                    2, 3, 4, 5, 10, 11, 14 -> "danger"; else -> pr.optString("iconCategory")
                }
                val mag = pr.optInt("magnitudeOfDelay", 0)
                // a jam with no delay given: a rough one from how bad TomTom says it is (minor, moderate, major)
                val delay = pr.optInt("delay", 0).takeIf { it > 0 } ?: if (cat == "jam") when (mag) { 1 -> 60; 2 -> 180; 3 -> 420; else -> 0 } else 0
                out += Incident(pr.optString("id"), cat, ev, delay, pts)
            } catch (e: Exception) { /* skip bad entry */ }
        }
        return out
    }

    /** Posted speed limit at [p] (m/s) from TomTom's own road data, or null when it has none there. [heading] picks the right carriageway. */
    /** A limit from TomTom's reverse geocoder, with the road it belongs to (to check it's the road you're on). */
    class RoadLimit(val ms: Double, val street: String, val refs: List<String>)

    fun speedLimit(key: String, p: LL, heading: Double): RoadLimit? {
        val k = URLEncoder.encode(key, "UTF-8")
        val hd = if (heading >= 0) "&heading=${heading.toInt() % 360}" else ""
        // a tight radius: a wide one happily returns the limit of a side road or a parallel slip road
        val body = get("https://api.tomtom.com/search/2/reverseGeocode/${p.lat},${p.lon}.json?key=$k&returnSpeedLimit=true&radius=25$hd", 6000, 8000)
        val addr = JSONObject(body).optJSONArray("addresses")?.optJSONObject(0)?.optJSONObject("address") ?: return null
        val v = addr.optString("speedLimit").trim().uppercase()
        val m = Regex("^([0-9]+(?:\\.[0-9]+)?)\\s*(KPH|MPH)$").find(v) ?: return null
        val n = m.groupValues[1].toDouble()
        if (n <= 0.0) return null
        val ra = addr.optJSONArray("routeNumbers")
        val refs = if (ra == null) emptyList() else (0 until ra.length()).map { ra.optString(it) }.filter { it.isNotBlank() }
        return RoadLimit(if (m.groupValues[2] == "MPH") n * 0.44704 else n / 3.6, addr.optString("streetName").ifBlank { addr.optString("street") }, refs)
    }

    private fun snake(m: String) = m.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").uppercase()

    private fun ll(o: JSONObject) = LL(
        if (o.has("latitude")) o.getDouble("latitude") else o.getDouble("lat"),
        if (o.has("longitude")) o.getDouble("longitude") else if (o.has("lon")) o.getDouble("lon") else o.getDouble("lng"),
    )

    private fun parse(r: JSONObject): Route {
        val pts = ArrayList<LL>()
        val legs = r.optJSONArray("legs") ?: JSONArray()
        for (i in 0 until legs.length()) {
            val p = legs.getJSONObject(i).optJSONArray("points") ?: continue
            for (n in 0 until p.length()) pts += ll(p.getJSONObject(n))
        }
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + Geo.dist(pts[i - 1], pts[i])
        val insArr = r.optJSONObject("guidance")?.optJSONArray("instructions") ?: r.optJSONArray("instructions") ?: JSONArray()
        val cc = RoadRefs.countryFor(pts.firstOrNull())
        val steps = (0 until insArr.length()).map {
            val i = insArr.getJSONObject(it)
            // the road's numbers often come as ["A4", "E40"] (or the E-number alone in "street"): the national one is what's signed
            val nums = i.optJSONArray("roadNumbers")?.let { a -> (0 until a.length()).map { n -> a.optString(n) }.filter { n -> n.isNotBlank() } }.orEmpty()
            val street = RoadRefs.street(i.optString("street").ifBlank {
                i.optJSONObject("nextRoadInformation")?.optJSONObject("streetName")?.optString("text").orEmpty()
            }, nums, cc)
            var man = snake(i.optString("maneuver", "STRAIGHT"))
            val rx = i.optInt("roundaboutExitNumber", 0)
            if (i.optString("junctionType").equals("ROUNDABOUT", true) && !man.startsWith("ROUNDABOUT") && !man.startsWith("ARRIVE")) {
                // These are all drawn as a roundabout (never a plain turn arrow): the angle just says which way the exit points
                val ang = i.optDouble("turnAngleInDecimalDegrees", 0.0)
                man = if (ang > 135 || ang < -135) "ROUNDABOUT_BACK" else if (ang > 25) "ROUNDABOUT_RIGHT" else if (ang < -25) "ROUNDABOUT_LEFT" else "ROUNDABOUT_CROSS"
            }
            Step(
                if (i.has("routeOffsetInMeters")) i.getDouble("routeOffsetInMeters") else i.optDouble("offsetInMeters", 0.0),
                man, street,
                i.optString("message").ifBlank { i.optString("instructionMessage") },
                rx, i.optString("exitNumber"), i.optString("signpostText"),
                // TomTom gives no bearings, only the angle from the way in to the exit (right positive): kept as a way in of 0 and an
                // exit at that angle, so the turn card and the car's head-up display show a half-left exit as half-left
                rbIn = if (man.startsWith("ROUNDABOUT") && i.has("turnAngleInDecimalDegrees")) 0.0 else -1.0,
                rbOut = if (man.startsWith("ROUNDABOUT") && i.has("turnAngleInDecimalDegrees")) ((i.optDouble("turnAngleInDecimalDegrees", 0.0) % 360.0) + 360.0) % 360.0 else -1.0,
            )
        }
        val laneSecs = ArrayList<LaneSec>()
        val sev = IntArray(max(0, pts.size - 1))
        val delays = ArrayList<DelaySeg>()
        val motorway = ArrayList<Pair<Double, Double>>()
        try {
            val secs = r.optJSONArray("sections") ?: JSONArray()
            for (n in 0 until secs.length()) {
                val sec = secs.getJSONObject(n)
                if (sec.optString("sectionType").equals("MOTORWAY", true)) {
                    val a = sec.optInt("startPointIndex", 0).coerceIn(0, pts.size - 1); val b = sec.optInt("endPointIndex", a).coerceIn(0, pts.size - 1)
                    if (b > a) motorway += cum[a] to cum[b]
                    continue
                }
                if (!sec.optString("sectionType").equals("TRAFFIC", true)) continue
                val a = sec.optInt("startPointIndex", 0).coerceIn(0, sev.size)
                val b = sec.optInt("endPointIndex", a).coerceIn(0, sev.size)
                val mag = sec.optInt("magnitudeOfDelay", 0)
                val cat = sec.optString("simpleCategory")
                // judge each stretch by how much slower than free-flowing it really is, so roadworks and tiny slowdowns are not painted as jams
                val segLen = cum[b] - cum[a]
                val delay = sec.optDouble("delayInSeconds", 0.0)
                val crawl = sec.optDouble("effectiveSpeedInKmh", -1.0)
                val perKm = if (segLen > 50) delay / (segLen / 1000.0) else 0.0
                // how many times longer the stretch takes than it would with no traffic
                val slow = if (crawl > 1.0 && segLen > 50) { val t = segLen / (crawl / 3.6); if (t - delay > 1.0) t / (t - delay) else 3.0 } else 1.0
                val closed = cat.equals("ROAD_CLOSURE", true)
                val level = when {
                    closed || mag == 3 || (delay >= 45.0 && (slow >= 2.2 || perKm >= 100.0)) || (crawl in 0.0..12.0 && segLen > 150 && delay >= 40.0) -> 2
                    (delay >= 20.0 && (slow >= 1.35 || perKm >= 40.0)) || (mag == 2 && delay >= 20.0) -> 1
                    else -> 0
                }
                if (delay >= 5.0 && b > a) delays += DelaySeg(cum[a], cum[b], delay)
                if (level > 0) for (i in a until b) sev[i] = max(sev[i], level)
            }
        } catch (e: Exception) { /* traffic colouring is optional */ }
        try {
            val secs = r.optJSONArray("sections") ?: JSONArray()
            for (n in 0 until secs.length()) {
                val sec = secs.getJSONObject(n)
                if (!sec.optString("sectionType").equals("LANES", true)) continue
                val a = sec.optInt("startPointIndex", 0).coerceIn(0, max(0, pts.size - 1))
                val b = sec.optInt("endPointIndex", a).coerceIn(0, max(0, pts.size - 1))
                val la = sec.optJSONArray("lanes") ?: continue
                val lanes = (0 until la.length()).map { li ->
                    val lo = la.getJSONObject(li)
                    val da = lo.optJSONArray("directions") ?: JSONArray()
                    Lane((0 until da.length()).map { snake(da.optString(it)) }, lo.optString("follow").takeIf { it.isNotBlank() }?.let { snake(it) })
                }
                if (lanes.isNotEmpty() && pts.isNotEmpty()) laneSecs += LaneSec(cum[a], cum[b], lanes)
            }
        } catch (e: Exception) { /* lane data is optional */ }
        val limits = ArrayList<LimitSec>()
        try {
            val secs = r.optJSONArray("sections") ?: JSONArray()
            for (n in 0 until secs.length()) {
                val sec = secs.getJSONObject(n)
                val t = sec.optString("sectionType").replace("_", "")
                if (!t.equals("SPEEDLIMIT", true)) continue
                val kmh = sec.optDouble("maxSpeedLimitInKmh", sec.optDouble("speedLimitInKmh", -1.0))
                if (kmh.isNaN() || kmh <= 0.0 || kmh > 200.0 || pts.isEmpty()) continue
                val a = sec.optInt("startPointIndex", 0).coerceIn(0, pts.size - 1)
                val b = sec.optInt("endPointIndex", a).coerceIn(0, pts.size - 1)
                if (b > a) limits += LimitSec(cum[a], cum[b], kmh / 3.6)
            }
            limits.sortBy { it.off }
        } catch (e: Exception) { /* speed limits are optional */ }
        val s = r.optJSONObject("summary")
        val len = s?.optDouble("lengthInMeters", 0.0)?.takeIf { it > 0 } ?: cum.lastOrNull() ?: 0.0
        return Route(pts, cum, slipForks(steps, motorway), len, s?.optDouble("travelTimeInSeconds", 0.0) ?: 0.0, s?.optDouble("trafficDelayInSeconds", 0.0) ?: 0.0, laneSecs, sev).also { it.delays = delays; it.limits = limits; it.source = "tomtom" }
    }
}

object Weather {
    data class Now(val tempC: Double, val code: Int)

    fun fetch(p: LL): Now? = try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}&current=temperature_2m,weather_code,rain,snowfall"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 10000
        val cur = JSONObject(c.inputStream.bufferedReader().readText()).getJSONObject("current")
        val code = cur.getInt("weather_code")
        // the hour's summary often says "cloudy" during showers: when rain or snow is actually falling this quarter hour, it's shown as such
        val rainNow = cur.optDouble("rain", 0.0) > 0.0
        val snowNow = cur.optDouble("snowfall", 0.0) > 0.0
        val effective = when {
            snowNow && code !in 71..77 && code !in 85..86 -> 71
            rainNow && code !in 51..67 && code !in 80..82 && code !in 95..99 && code !in 71..77 && code !in 85..86 -> 61
            else -> code
        }
        Now(cur.getDouble("temperature_2m"), effective)
    } catch (e: Exception) { null }

    class Hour(val label: String, val temp: Double, val code: Int, val rain: Int, val windKmh: Double = 0.0, val windFrom: Double = 0.0)
    class Day(val label: String, val code: Int, val max: Double, val min: Double)
    class Forecast(
        val temp: Double, val feels: Double, val code: Int, val windKmh: Double,
        val sunrise: String, val sunset: String, val hours: List<Hour>, val days: List<Day>,
    )

    fun forecast(p: LL): Forecast? = try {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${p.lat}&longitude=${p.lon}" +
            "&current=temperature_2m,apparent_temperature,weather_code,wind_speed_10m" +
            "&hourly=temperature_2m,weather_code,precipitation_probability,wind_speed_10m,wind_direction_10m" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset&forecast_days=6&timezone=auto"
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 15000
        val j = JSONObject(c.inputStream.bufferedReader().readText())
        val cur = j.getJSONObject("current")
        val nowHour = cur.getString("time").substring(0, 13)
        val h = j.getJSONObject("hourly")
        val times = h.getJSONArray("time"); val temps = h.getJSONArray("temperature_2m")
        val codes = h.getJSONArray("weather_code"); val pops = h.getJSONArray("precipitation_probability")
        val winds = h.optJSONArray("wind_speed_10m"); val dirs = h.optJSONArray("wind_direction_10m")
        val hours = ArrayList<Hour>()
        for (i in 0 until times.length()) {
            val t = times.getString(i)
            if (t.substring(0, 13) < nowHour) continue
            hours += Hour(t.substring(11, 16), temps.getDouble(i), codes.getInt(i), pops.optInt(i, 0), winds?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0, dirs?.optDouble(i, 0.0)?.takeIf { !it.isNaN() } ?: 0.0)
            if (hours.size >= 12) break
        }
        val d = j.getJSONObject("daily")
        val dt = d.getJSONArray("time"); val dc = d.getJSONArray("weather_code")
        val mx = d.getJSONArray("temperature_2m_max"); val mn = d.getJSONArray("temperature_2m_min")
        val parse = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val show = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val days = (0 until dt.length()).map { i ->
            val label = if (i == 0) Lang.t("Heute", "Today") else show.format(parse.parse(dt.getString(i))!!)
            Day(label, dc.getInt(i), mx.getDouble(i), mn.getDouble(i))
        }
        Forecast(
            cur.getDouble("temperature_2m"), cur.getDouble("apparent_temperature"), cur.getInt("weather_code"), cur.getDouble("wind_speed_10m"),
            d.getJSONArray("sunrise").getString(0).substring(11, 16), d.getJSONArray("sunset").getString(0).substring(11, 16), hours, days,
        )
    } catch (e: Exception) { null }

    fun text(code: Int) = when (code) {
        0 -> Lang.t("Klar", "Clear"); 1, 2 -> Lang.t("Teils bewölkt", "Partly cloudy"); 3 -> Lang.t("Bedeckt", "Overcast"); 45, 48 -> Lang.t("Nebel", "Fog")
        in 51..57 -> Lang.t("Nieselregen", "Drizzle"); in 61..67 -> Lang.t("Regen", "Rain"); in 71..77 -> Lang.t("Schnee", "Snow"); in 80..82 -> Lang.t("Schauer", "Showers")
        in 85..86 -> Lang.t("Schneeschauer", "Snow showers"); in 95..99 -> Lang.t("Gewitter", "Thunderstorm"); else -> "—"
    }
}

/**
 * Place search with no TomTom key or Mapbox token: OpenStreetMap's own search services (Photon, which finds places as you type,
 * then Nominatim). Free, no key; used together with the downloaded place names and your saved places.
 */
object OsmSearch {
    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 10000; c.readTimeout = 15000
        // both services ask apps to say who they are
        c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        if (code !in 200..299) throw IOException("HTTP $code")
        return body
    }

    fun geocode(country: String, q: String, near: LL?): List<Place> {
        val enc = URLEncoder.encode(q, "UTF-8").replace("+", "%20")
        val cc = country.trim().uppercase()
        val out = ArrayList<Place>()
        var anyOk = false
        try {
            val lang = java.util.Locale.getDefault().language.takeIf { it in setOf("en", "de", "fr", "it") } ?: "en"
            var url = "https://photon.komoot.io/api/?q=$enc&limit=15&lang=$lang"
            if (near != null) url += String.format(java.util.Locale.US, "&lat=%.5f&lon=%.5f", near.lat, near.lon)
            val arr = JSONObject(get(url)).optJSONArray("features") ?: JSONArray()
            anyOk = true
            for (i in 0 until arr.length()) {
                val f = arr.optJSONObject(i) ?: continue
                val pr = f.optJSONObject("properties") ?: continue
                if (cc.isNotEmpty() && !pr.optString("countrycode").equals(cc, true)) continue
                val co = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
                val street = listOf(pr.optString("housenumber"), pr.optString("street")).filter { it.isNotBlank() }.joinToString(" ")
                val name = pr.optString("name").ifBlank { street }.ifBlank { pr.optString("city") }
                if (name.isBlank()) continue
                val sub = listOf(if (name != street) street else "", pr.optString("city").takeIf { it != name } ?: "", pr.optString("postcode"),
                    pr.optString("county").takeIf { pr.optString("city").isBlank() } ?: "").filter { it.isNotBlank() }.joinToString(", ")
                val p = Place(name, sub, LL(co.getDouble(1), co.getDouble(0)))
                    .town(pr.optString("osm_key") == "place" && pr.optString("osm_value") in SearchRank.OSM_TOWNS || pr.optString("type") == "city")
                if (out.none { it.name == p.name && Geo.dist(it.pos, p.pos) < 60 }) out += p
            }
        } catch (e: Exception) { }
        if (out.size < 5) try {
            var url = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=10&q=$enc"
            if (near != null) url += "&viewbox=${near.lon - 0.5},${near.lat + 0.35},${near.lon + 0.5},${near.lat - 0.35}"
            if (cc.isNotEmpty()) url += "&countrycodes=${cc.lowercase()}"
            val arr = JSONArray(get(url))
            anyOk = true
            for (i in 0 until arr.length()) {
                val x = arr.getJSONObject(i)
                val disp = x.optString("display_name")
                val p = Place(x.optString("name").ifBlank { disp.substringBefore(',') }, disp.substringAfter(", ", ""), LL(x.getDouble("lat"), x.getDouble("lon"))).town(SearchRank.nominatimTown(x))
                if (out.none { Geo.dist(it.pos, p.pos) < 60 }) out += p
            }
        } catch (e: Exception) { }
        // neither could be reached: say so (the caller shows the saved and downloaded places instead)
        if (!anyOk) throw IOException("no signal")
        return out
    }
}
