package com.headunit.launcher

/**
 * Navigation-domain models and country-specific driving conventions. Kept separate from NavEngine's
 * location, routing and lifecycle implementation so UI and engine changes do not have to touch
 * the same large source file.
 */
class Fix(val pos: LL, val heading: Double, val speed: Double, val alt: Double, val acc: Double = 0.0)
class RouteWx(val minutes: Int, val code: Int, val temp: Double, val rain: Int, val off: Double = 0.0, val windKmh: Double = 0.0, val windFrom: Double = 0.0) {
    val snow: Boolean get() = code in 71..77 || code in 85..86
    val wet: Boolean get() = code in 51..67 || code in 80..82 || code in 95..99
}

class TurnInfo(
    val dist: Double, val street: String, val maneuver: String, val thenManeuver: String?, val thenText: String, val lanes: LaneSec? = null,
    /** "2nd exit" for a roundabout, "Exit 12" for a motorway exit. */
    val tag: String = "", val laneAdvice: String? = null, val exit: Int = 0,
    /** Text from the road sign at the junction, when the route data has it. */
    val sign: String = "",
    /** The junction number at a motorway exit ("9"), when the route data has it. */
    val exitNo: String = "",
    /** At a roundabout: heading in, the exit you take and the other exits (degrees), for the roundabout diagram. */
    val rbIn: Double = -1.0, val rbOut: Double = -1.0, val rbExits: List<Double> = emptyList(),
    /** How far the turn after this one comes after it (for "then" on the card). */
    val thenGap: Double = 0.0,
    /** Roundabout exit for the following manoeuvre shown in the "then" cue. */
    val thenExit: Int = 0,
    /** The motorway junction on the card: "Frankfurter Kreuz", or "Ausfahrt 17 Weiterstadt" for a plain exit (blank when not known). */
    val junction: String = "",
    /** The junction's bare name ("Frankfurter Kreuz", "Weiterstadt"), for when the exit number is shown elsewhere on the card. */
    val junctionName: String = "",
)
/** Countries that use miles for road distances and speeds. */
/** Countries that use Fahrenheit. */
val FAHRENHEIT_COUNTRIES = setOf("US", "PR", "VI", "GU", "AS", "MP", "LR", "BS", "KY", "BZ", "PW", "FM", "MH")

/** Countries that drive on the left (right-hand drive cars), where the screen keeps its normal layout. */
/** Countries where warning drivers about speed cameras isn't allowed (camera alerts switch off there unless you choose otherwise). */
val CAMERA_BANNED = mapOf("FR" to "Frankreich", "DE" to "Deutschland", "CH" to "Schweiz")
val KEEP_LEFT = setOf("GB", "IE", "IM", "GG", "JE", "MT", "CY", "AU", "NZ", "JP", "IN", "ZA", "HK", "MO", "SG", "MY", "TH", "ID", "KE", "TZ", "UG",
    "ZM", "ZW", "BW", "NA", "MZ", "MW", "LS", "SZ", "PK", "BD", "LK", "NP", "BT", "JM", "TT", "BB", "BS", "GY", "SR", "FJ", "PG", "MU", "SC", "BN", "TL", "KY", "BM", "VG", "AG", "DM", "GD", "KN", "LC", "VC", "FK", "SH", "MV", "KI", "SB", "TO", "TV", "WS", "NR")
class AlertInfo(val kind: String, val text: String, val ms: Long = 5000)
/** Shown after you arrive: how the trip went. */
class ArrivalInfo(val place: Place?, val name: String, val dist: Double, val secs: Double, val avgMs: Double, val side: String = "")
/** Another way to the destination, drawn grey on the map while you drive, with how it compares (minutes, negative = quicker). */
/** Another way to go while you drive: how many minutes it saves or costs, and [traffic] -1 less, 0 similar, 1 more than yours. */
class LiveAlt(val route: Route, val diffMin: Int, val bubble: LL, val divergeOff: Double, val traffic: Int = 0, val divIdx: Int = 0) {
    /** The line to draw: only from where it leaves your road, at full detail (every other point cut corners across houses). */
    fun line(): List<LL> = route.pts.subList(maxOf(0, divIdx), route.pts.size)
}

/** Something in the next couple of miles for the hazard radar: what it is and how far away (m). */
class RadarItem(val kind: String, val dist: Double, val label: String)
/** "Work · 23 min · light traffic": where you are probably going now, with the live time. */
class Prediction(val place: Place, val title: String, val mins: Int, val delayMin: Int, val note: String)
class IncidentAhead(val label: String, val dist: Double, val delaySec: Int)
/** [chargeSecs]: of [secs], the time at the charging stops still ahead (charging, and getting off and back on the road). */
class EtaInfo(val arrive: String, val dist: Double, val secs: Double, val delayMin: Int, val chargeSecs: Double = 0.0)

/** Fahrzeugtypen mit Höhen-, Breiten- und Gewichtsbeschränkungen: Pkw, Wohnmobil, Anhänger, Lastkraftwagen. */
enum class VehicleType(val label: String) {
    CAR("Pkw"),
    RV("Wohnmobil"),
    TRAILER("Anhänger"),
    LORRY("Lastkraftwagen")
}

/** Fahrzeugabmessungen in Metern und Tonnen (für Brücken-, Durchfahrts- und Gewichtswarnungen). */
class VehicleDimensions(
    val heightM: Double = 0.0,      // Höhe in m
    val widthM: Double = 0.0,       // Breite in m
    val lengthM: Double = 0.0,      // Länge in m
    val weightTons: Double = 0.0,   // Gesamtgewicht in Tonnen
    val axleWeightTons: Double = 0.0 // Achslast in Tonnen
) {
    val hasLimits: Boolean get() = heightM > 0 || widthM > 0 || lengthM > 0 || weightTons > 0 || axleWeightTons > 0
}

/** A crowd-sourced traffic report: type, position, time, id for deduplication. */
data class CrowdReport(val id: String, val kind: String, val pos: LL, val ts: Long, val ttl: Long = 30 * 60_000L)
