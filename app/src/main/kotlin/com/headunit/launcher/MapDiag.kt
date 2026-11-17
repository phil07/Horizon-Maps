package com.headunit.launcher

import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import java.util.Date
import java.util.Locale

/**
 * Asks Mapbox directly why the map doesn't show: when the style hasn't loaded after a while with no error. It checks, in turn, that
 * the server can be reached (a private DNS or an ad blocker can stop it), that the head unit's clock is right, and that the token is
 * accepted. Each answer names the cause and what to do about it.
 */
object MapDiag {
    private fun de(): Boolean = Lang.de

    /** The message to show; it always says one thing that is in the way (or that Mapbox itself answers, so the app is to blame). */
    fun diagnose(token: String): String {
        if (token.isBlank()) return if (de()) "Die Karte lädt nicht: Es ist kein Mapbox-Token eingetragen (Einstellungen › Schlüssel & Konten)."
            else "The map isn't loading: no Mapbox token is set (Settings › Keys & accounts)."
        // 1. can the server be reached at all
        val head: Pair<Int, String?> = try {
            val c = URL("https://api.mapbox.com/").openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000; c.requestMethod = "HEAD"
            try { Pair(c.responseCode, c.getHeaderField("Date")) } finally { c.disconnect() }
        } catch (e: UnknownHostException) {
            return if (de()) "Die Karte lädt nicht: api.mapbox.com wird nicht aufgelöst. Privates DNS oder ein Werbeblocker blockiert den Server. Privates DNS abschalten oder api.mapbox.com erlauben."
                else "The map isn't loading: api.mapbox.com can't be looked up. Private DNS or an ad blocker is blocking the server. Turn off private DNS or allow api.mapbox.com."
        } catch (e: javax.net.ssl.SSLException) {
            val clockWrong = e.message?.contains("not yet valid", true) == true || e.message?.contains("expired", true) == true
            return if (clockWrong) {
                if (de()) "Die Karte lädt nicht: Die Uhr des Autodisplays geht falsch. Datum und Uhrzeit einstellen (am besten automatisch)."
                else "The map isn't loading: the head unit's clock is wrong. Set the date and time (best automatic)."
            } else if (de()) "Die Karte lädt nicht: Die sichere Verbindung zu Mapbox kommt nicht zustande. Ein Werbeblocker oder ein Filter im Netz blockiert sie."
            else "The map isn't loading: the secure connection to Mapbox fails. An ad blocker or a network filter is blocking it."
        } catch (e: Exception) {
            return if (de()) "Die Karte lädt nicht: Keine Verbindung zu Mapbox. Netz, WLAN oder Mobilfunk prüfen."
                else "The map isn't loading: no connection to Mapbox. Check the network, Wi-Fi or mobile data."
        }
        // 2. the clock: far off from the server's, the secure connection and the token's checks fail in strange ways
        val serverMs = head.second?.let { try { Date(it).time } catch (e: Exception) { null } }
        if (serverMs != null && kotlin.math.abs(System.currentTimeMillis() - serverMs) > 10 * 60_000L) {
            return if (de()) "Die Karte lädt nicht: Die Uhr des Autodisplays geht um mehr als 10 Minuten falsch. Datum und Uhrzeit einstellen."
                else "The map isn't loading: the head unit's clock is more than 10 minutes off. Set the date and time."
        }
        // 3. the token
        val tokenCode: Int = try {
            val c = URL("https://api.mapbox.com/styles/v1/mapbox/streets-v12?access_token=" + java.net.URLEncoder.encode(token, "UTF-8")).openConnection() as HttpURLConnection
            c.connectTimeout = 8000; c.readTimeout = 8000
            try { c.responseCode } finally { c.disconnect() }
        } catch (e: Exception) { -1 }
        if (tokenCode == 401 || tokenCode == 403) return if (de()) "Die Karte lädt nicht: Mapbox lehnt das Token ab (Code $tokenCode). Das Token in Einstellungen › Schlüssel & Konten prüfen; es muss vollständig und ein öffentliches Token (pk.) sein."
            else "The map isn't loading: Mapbox rejects the token (code $tokenCode). Check it in Settings › Keys & accounts; it must be complete and a public token (pk.)."
        if (head.first in 200..499 && tokenCode in 200..299) return if (de()) "Mapbox ist erreichbar und das Token wird angenommen, die Karte lädt trotzdem nicht. App bitte neu starten; hält es an, Bericht senden."
            else "Mapbox is reachable and accepts the token, but the map still doesn't load. Restart the app; if it persists, send a report."
        return if (de()) "Die Karte lädt nicht: Mapbox antwortet nicht richtig (Code ${head.first}). Verbindung prüfen und die App neu starten."
            else "The map isn't loading: Mapbox doesn't answer properly (code ${head.first}). Check the connection and restart the app."
    }
}
