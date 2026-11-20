package com.headunit.launcher

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Live traffic events in the Netherlands from NDW (Nationaal Dataportaal Wegverkeer, open data, free and without a key): accidents,
 * broken-down vehicles, obstructions, closures and roadworks. One DATEX II file (about 400 KB, gzipped). It feeds the same incident
 * pipeline as TomTom's and DGT's (NavEngine), like DgtRest does for Spain.
 *
 * The file is read every 15 minutes while the app is open in the Netherlands, and only when it has changed. Speed limits, rerouting
 * notices and network notices are left out: they are not what this is for.
 */
object NdwRest {
    private const val URL_ = "https://opendata.ndw.nu/actueel_beeld.xml.gz"
    /** How often the file is looked at while the app is open in the Netherlands. */
    const val EVERY_MS = 15 * 60_000L

    @Volatile var items: List<Incident> = emptyList(); private set
    private var fetchedAt = 0L

    /** Downloads the file when it is due (every 15 minutes) and reads it. True when the list was replaced. Never throws. */
    @Synchronized
    fun refreshIfDue(now: Long = System.currentTimeMillis()): Boolean {
        if (now - fetchedAt < EVERY_MS) return false
        fetchedAt = now
        return try { refresh(now) } catch (e: Exception) { false }
    }

    private fun refresh(now: Long): Boolean {
        val conn = URL(URL_).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "application/xml")
        try {
            if (conn.responseCode != 200) return false
            GZIPInputStream(conn.inputStream).use { ins ->
                items = parse(ins, now)
            }
            return true
        } finally {
            conn.disconnect()
        }
    }

    /** The incidents within [radius] metres of [p]. */
    fun near(p: LL, radius: Double): List<Incident> = items.filter { Geo.dist(it.pts.first(), p) <= radius }

    /** The NDW incidents not already in [other] (TomTom's, which show the same jam or closure): within 80 m, the same thing is skipped. */
    fun notIn(other: List<Incident>): List<Incident> = items.filter { d ->
        other.none { o -> o.pts.isNotEmpty() && Geo.dist(o.pts.first(), d.pts.first()) < 80.0 }
    }

    /** One situation record, collected as its tags are read. */
    private class Rec(val type: String, val id: String) {
        var lat: Double? = null
        var lon: Double? = null
        var end: Long = 0L
        var cause: String? = null
        var lane: String? = null
        var vehicle: String? = null
        var comment: String? = null
    }

    private fun parse(input: java.io.InputStream, now: Long): List<Incident> {
        val p = Xml.newPullParser()
        p.setInput(input, "UTF-8")
        val out = ArrayList<Incident>()
        var rec: Rec? = null
        var inLoc = false
        var inComment = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                val n = p.name.substringAfter(':')
                when (n) {
                    "situationRecord" -> rec = Rec(attr(p, "type").substringAfter(':'), attr(p, "id"))
                    "locationReference" -> inLoc = true
                    "generalPublicComment" -> inComment = true
                    "latitude" -> rec?.let { if (inLoc && it.lat == null) it.lat = p.nextText().trim().toDoubleOrNull() }
                    "longitude" -> rec?.let { if (inLoc && it.lon == null) it.lon = p.nextText().trim().toDoubleOrNull() }
                    "posList" -> rec?.let {
                        // "lat lon lat lon …": the first pair is where the event starts
                        if (inLoc && it.lat == null) {
                            val t = p.nextText().trim().split(Regex("\\s+"))
                            if (t.size >= 2) { it.lat = t[0].toDoubleOrNull(); it.lon = t[1].toDoubleOrNull() }
                        }
                    }
                    "overallEndTime" -> rec?.let { it.end = iso(p.nextText().trim()) }
                    "causeType" -> rec?.let { if (it.cause == null) it.cause = p.nextText().trim() }
                    "roadOrCarriagewayOrLaneManagementType" -> rec?.let { if (it.lane == null) it.lane = p.nextText().trim() }
                    "vehicleObstructionType" -> rec?.let { if (it.vehicle == null) it.vehicle = p.nextText().trim() }
                    "value" -> rec?.let { if (inComment && it.comment == null) it.comment = p.nextText().trim() }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name.substringAfter(':')) {
                    "locationReference" -> inLoc = false
                    "generalPublicComment" -> inComment = false
                    "situationRecord" -> { rec?.let { r -> toIncident(r, now)?.let { out += it } }; rec = null }
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun attr(p: XmlPullParser, local: String): String {
        for (i in 0 until p.attributeCount) {
            val name = p.getAttributeName(i)
            if (name == local || name.endsWith(":$local")) return p.getAttributeValue(i)
        }
        return ""
    }

    /** The end time (ISO 8601, in UTC) in milliseconds; 0 when it is not readable. */
    private fun iso(s: String): Long = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .parse(s.take(19))?.time ?: 0L
    } catch (e: Exception) { 0L }

    private fun toIncident(r: Rec, now: Long): Incident? {
        val la = r.lat ?: return null
        val lo = r.lon ?: return null
        if (la.isNaN() || lo.isNaN()) return null
        // a record that has already ended is not shown
        if (r.end > 0L && r.end <= now) return null
        val cat = categoryOf(r) ?: return null
        val text = r.comment?.takeIf { it.isNotBlank() } ?: ""
        return Incident("ndw-${r.id}", cat, text, 0, listOf(LL(la, lo)))
    }

    /** The app's incident kind for a record, or null for the ones it leaves out (speed limits, rerouting, network notices). */
    private fun categoryOf(r: Rec): String? {
        val lane = r.lane
        if (lane != null) return when (lane) {
            "roadClosed", "carriagewayClosures" -> "roadClosed"
            "laneClosures" -> "laneClosed"
            else -> if (r.cause == "roadMaintenance") "roadWorks" else null
        }
        return when (r.type) {
            "Accident" -> "accident"
            "VehicleObstruction" -> if (r.vehicle == "vehicleStuck") "breakdown" else "danger"
            "GeneralObstruction" -> "danger"
            else -> null
        }
    }
}
