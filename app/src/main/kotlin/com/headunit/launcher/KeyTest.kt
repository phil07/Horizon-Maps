package com.headunit.launcher

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Settings > Keys & accounts > Test all keys: one small request for every key you entered (map, search, speed-limit, satellite and charger
 * accounts), so a wrong or expired key shows up in one place instead of as missing data on the road. The keys are only sent to their own
 * service, the same way the app uses them.
 */
object KeyTest {
    enum class State { OK, BAD, NONE, NET }
    data class Result(val name: String, val state: State, val note: String)

    private fun enc(s: String) = URLEncoder.encode(s.trim(), "UTF-8")

    /** The HTTP code and the start of the answer (up to 4 KB) of a GET. */
    private fun get(url: String): Triple<Int, String, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 7000; c.readTimeout = 10000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0")
        val code = c.responseCode
        val ctype = c.contentType ?: ""
        val st = if (code in 200..299) c.inputStream else c.errorStream
        val body = st?.use { s -> String(s.readNBytes(4096), Charsets.UTF_8) } ?: ""
        return Triple(code, ctype, body)
    }

    private fun check(name: String, key: String, run: (String) -> Result): Result {
        if (key.isBlank()) return Result(name, State.NONE, Lang.t("nicht eingetragen", "not entered"))
        return try { run(key.trim()) } catch (e: java.io.IOException) {
            Result(name, State.NET, Lang.t("keine Verbindung", "no connection") + ": " + (e.message ?: "").take(40))
        } catch (e: Exception) { Result(name, State.NET, (e.message ?: e.javaClass.simpleName).take(60)) }
    }

    private fun byCode(name: String, code: Int, okNote: String): Result = when {
        code in 200..299 -> Result(name, State.OK, okNote)
        code == 401 || code == 403 -> Result(name, State.BAD, Lang.t("abgelehnt (HTTP $code): Schlüssel falsch, abgelaufen oder nicht freigeschaltet", "refused (HTTP $code): key wrong, expired or not enabled"))
        code == 429 -> Result(name, State.BAD, Lang.t("zu viele Abfragen (HTTP 429): Limit des Schlüssels erreicht", "too many requests (HTTP 429): key limit reached"))
        else -> Result(name, State.NET, "HTTP $code")
    }

    /** Tests every key (blocking: call from a background thread). */
    fun all(prefs: Prefs): List<Result> {
        val out = ArrayList<Result>()
        out += check("TomTom", prefs.s(PreferenceKeys.API_KEY, "")) { k ->
            val (c, _, _) = get("https://api.tomtom.com/search/2/geocode/Berlin.json?limit=1&key=${enc(k)}")
            byCode("TomTom", c, Lang.t("Suche funktioniert", "search works"))
        }
        out += check("HERE", prefs.s(PreferenceKeys.HERE_KEY, "")) { k ->
            val (c, _, _) = get("https://routematching.hereapi.com/v8/match/routelinks?apikey=${enc(k)}&routeMatch=1&mode=${enc("fastest;car;traffic:disabled")}" +
                "&attributes=${enc("SPEED_LIMITS_FCn(*)")}&waypoint0=52.52000,13.40500&waypoint1=52.52050,13.40650")
            byCode("HERE", c, Lang.t("Tempolimits funktionieren", "speed limits work"))
        }
        out += check("Mapbox", prefs.s(PreferenceKeys.MAPBOX_TOKEN, "")) { k ->
            val (c, _, _) = get("https://api.mapbox.com/geocoding/v5/mapbox.places/Berlin.json?limit=1&access_token=${enc(MapboxRest.clean(k))}")
            byCode("Mapbox", c, Lang.t("Token funktioniert", "token works"))
        }
        out += check("Mapillary", prefs.s(PreferenceKeys.MAPILLARY_KEY, "")) { k ->
            val (c, _, _) = get("https://graph.mapillary.com/map_features?access_token=${enc(k)}&fields=id&limit=1&bbox=13.4050,52.5200,13.4060,52.5210" +
                "&object_values=${enc("regulatory--*speed-limit*")}")
            byCode("Mapillary", c, Lang.t("Token funktioniert", "token works"))
        }
        out += check("Esri", prefs.s(PreferenceKeys.ESRI_KEY, "")) { k ->
            val (c, t, _) = get("https://ibasemaps-api.arcgis.com/arcgis/rest/services/World_Imagery/MapServer/tile/2/1/1?token=${enc(k)}")
            // (a refused token can come back as HTTP 200 with a text answer instead of a picture)
            if (c in 200..299 && !t.startsWith("image")) Result("Esri", State.BAD, Lang.t("abgelehnt: kein Bild als Antwort (Schlüssel ohne Basemaps-Zugriff?)", "refused: no picture came back (key without basemaps access?)"))
            else byCode("Esri", c, Lang.t("Luftbilder funktionieren", "imagery works"))
        }
        out += check("MapTiler", prefs.s(PreferenceKeys.MAPTILER_KEY, "")) { k ->
            val (c, _, _) = get("https://api.maptiler.com/tiles/satellite-v2/2/1/1.jpg?key=${enc(k)}")
            byCode("MapTiler", c, Lang.t("Luftbilder funktionieren", "imagery works"))
        }
        out += check("Open Charge Map", prefs.s(PreferenceKeys.OCM_KEY, "")) { k ->
            val r = ChargerInfo.testOcm(k)
            Result("Open Charge Map", if (r.contains("funktioniert") || r.contains("works")) State.OK else if (r.contains("abgelehnt") || r.contains("refused")) State.BAD else State.NET, r)
        }
        out += check("Tankerkönig", prefs.s(PreferenceKeys.TANKERKOENIG_KEY, "")) { k ->
            val (c, _, b) = get("https://creativecommons.tankerkoenig.de/json/list.php?lat=52.52&lng=13.405&rad=1&sort=dist&type=all&apikey=${enc(k)}")
            val ok = Regex("\"ok\"\\s*:\\s*true").containsMatchIn(b)
            if (ok) Result("Tankerkönig", State.OK, Lang.t("Spritpreise funktionieren", "fuel prices work"))
            else if (c in 200..299 || c == 400) Result("Tankerkönig", State.BAD, Lang.t("abgelehnt", "refused") + ": " + (Regex("\"message\"\\s*:\\s*\"([^\"]*)\"").find(b)?.groupValues?.get(1) ?: "Schlüssel prüfen").take(60))
            else byCode("Tankerkönig", c, "")
        }
        return out
    }
}
