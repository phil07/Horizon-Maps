package com.headunit.launcher

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

/**
 * Live incidents in Spain from the DGT's National Access Point (nap.dgt.es): accidents, roadworks, closures, hazards and the V16
 * beacons of broken-down cars. Free and without a key. The whole country is one file (about 5 MB, DATEX II). Catalonia and the
 * Basque Country publish their own, which are not included. They feed the same incident pipeline as TomTom's (NavEngine).
 *
 * The server gives no change marker and ignores range requests, so the file is read from its start: its publication time is near
 * the top, and when it is the same as the last one read, the download is cut off there. Only a changed file is read in full.
 */
object DgtRest {
    private const val URL_ = "https://nap.dgt.es/datex2/v3/dgt/SituationPublication/datex2_v37.xml"
    /** How often the file is looked at while the app is open in Spain. */
    const val EVERY_MS = 15 * 60_000L

    @Volatile var items: List<Incident> = emptyList(); private set
    private var stamp = ""
    private var fetchedAt = 0L

    /** Looks at the file if it is due (every 15 minutes), and reads it when it has changed. Never throws. */
    @Synchronized
    fun refreshIfDue(now: Long = System.currentTimeMillis()): Boolean {
        if (now - fetchedAt < EVERY_MS) return false
        fetchedAt = now
        return try { refresh() } catch (e: Exception) { false }
    }

    /** Downloads the file (up to its publication time, when that is unchanged) and reads it. True when the list was replaced. */
    private fun refresh(): Boolean {
        val conn = URL(URL_).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("Accept", "text/xml")
        conn.setRequestProperty("Accept-Encoding", "gzip")
        try {
            if (conn.responseCode != 200) return false
            val raw = conn.inputStream
            val input = if ("gzip".equals(conn.contentEncoding, true)) GZIPInputStream(raw) else raw
            input.use { ins ->
                val res = parse(ins, stamp)
                if (res.items == null) return false  // the same publication as before: nothing to read
                stamp = res.stamp
                items = res.items
                return true
            }
        } finally {
            conn.disconnect()
        }
    }

    /** The incidents within [radius] metres of [p]. */
    fun near(p: LL, radius: Double): List<Incident> = items.filter { Geo.dist(it.pts.first(), p) <= radius }

    /** The DGT incidents that are not already in [other] (TomTom's, which show the same jam or closure): within 80 m, the same thing is skipped. */
    fun notIn(other: List<Incident>): List<Incident> = items.filter { d ->
        other.none { o -> o.pts.isNotEmpty() && Geo.dist(o.pts.first(), d.pts.first()) < 80.0 }
    }

    // ---- reading the file ----

    private class Parsed(val stamp: String, val items: List<Incident>?)

    /** One situation record, collected as its tags are read. */
    private class Rec(val type: String, val id: String) {
        var active = false
        var lat: Double? = null
        var lon: Double? = null
        var road: String? = null
        var municipality: String? = null
        var cause: String? = null
        var sub: String? = null
        var lane: String? = null
        var ref: String? = null
    }

    /** Reads the file. [last] is the publication time read before: when it comes up the same, the rest is not read (items null). */
    private fun parse(input: java.io.InputStream, last: String): Parsed {
        val p = Xml.newPullParser()
        p.setInput(input, "UTF-8")
        var stamp = ""
        val out = ArrayList<Incident>()
        var rec: Rec? = null
        var inLoc = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG) {
                val n = p.name.substringAfter(':')
                when (n) {
                    "publicationTime" -> {
                        stamp = p.nextText().trim()
                        // the same publication as the last one read: stop here, the rest of the file is not downloaded
                        if (stamp == last) return Parsed(stamp, null)
                    }
                    "situationRecord" -> rec = Rec(attr(p, "type"), attr(p, "id"))
                    "locationReference" -> inLoc = true
                    "latitude" -> if (rec != null && inLoc && rec.lat == null) rec.lat = p.nextText().trim().toDoubleOrNull()
                    "longitude" -> if (rec != null && inLoc && rec.lon == null) rec.lon = p.nextText().trim().toDoubleOrNull()
                    "roadName" -> if (rec != null && rec.road == null) rec.road = p.nextText().trim()
                    "municipality" -> if (rec != null && rec.municipality == null) rec.municipality = p.nextText().trim()
                    "validityStatus" -> rec?.let { it.active = p.nextText().trim() == "active" }
                    "causeType" -> rec?.let { if (it.cause == null) it.cause = p.nextText().trim() }
                    "situationRecordCreationReference" -> rec?.let { if (it.ref == null) it.ref = p.nextText().trim() }
                    "roadOrCarriagewayOrLaneManagementType" -> rec?.let { if (it.lane == null) it.lane = p.nextText().trim() }
                    "roadMaintenanceType", "accidentType", "vehicleObstructionType", "obstructionType", "environmentalObstructionType",
                    "infrastructureDamageObstructionType", "poorEnvironmentType", "nonWeatherRelatedRoadConditionType", "abnormalTrafficType" ->
                        rec?.let { if (it.sub == null) it.sub = p.nextText().trim() }
                }
            } else if (ev == XmlPullParser.END_TAG) {
                when (p.name.substringAfter(':')) {
                    "locationReference" -> inLoc = false
                    "situationRecord" -> { rec?.let { r -> toIncident(r)?.let { out += it } }; rec = null }
                }
            }
            ev = p.next()
        }
        return Parsed(stamp, out)
    }

    private fun attr(p: XmlPullParser, local: String): String {
        for (i in 0 until p.attributeCount) {
            val name = p.getAttributeName(i)
            if (name == local || name.endsWith(":$local")) return p.getAttributeValue(i)
        }
        return ""
    }

    private fun toIncident(r: Rec): Incident? {
        val la = r.lat ?: return null
        val lo = r.lon ?: return null
        if (!r.active || la.isNaN() || lo.isNaN()) return null
        val cat = categoryOf(r) ?: return null
        val text = listOfNotNull(r.road?.takeIf { it.isNotBlank() }, r.municipality?.takeIf { it.isNotBlank() }).joinToString(" · ")
        return Incident("dgt-${r.id}", cat, text, 0, listOf(LL(la, lo)))
    }

    /**
     * The app's incident kind for a record: closures, roadworks, accidents, jams, hazards and broken-down cars (the V16 beacons are
     * the "vehicle stuck" records). Speed limits, instructions for drivers and weather are left out: they are not what this is for.
     */
    private fun categoryOf(r: Rec): String? {
        val lane = r.lane
        if (lane != null) return when (lane) {
            "roadClosed", "carriagewayClosures" -> "roadClosed"
            "laneClosures" -> "laneClosed"
            else -> "roadWorks"
        }
        return when (r.cause) {
            "accident" -> "accident"
            "vehicleObstruction" -> if (r.sub == "vehicleStuck" || r.ref?.startsWith("V16") == true) "breakdown" else "danger"
            "abnormalTraffic" -> "jam"
            "obstruction", "environmentalObstruction", "infrastructureDamageObstruction", "poorEnvironment" -> "danger"
            "roadMaintenance" -> "roadWorks"
            else -> null
        }
    }
}
