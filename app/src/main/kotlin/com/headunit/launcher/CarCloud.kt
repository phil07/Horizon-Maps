package com.headunit.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The battery level (and whether the car is charging) from the car maker's own cloud, for when Android Auto isn't giving it: before
 * the drive, while parked, while charging. The driver signs in with their own account and their own (free) developer keys, which stay
 * on the phone: there is no server of ours in between.
 *
 * Providers: "volvo" (Volvo's own Energy API, Volvo ID login), "bmw" (BMW and Mini's CarData, a sign-in with a code) and "smartcar" (one login for many brands: BMW, Mercedes, Tesla, Ford,
 * Hyundai, Kia and more; the free plan covers one car). Both use the browser for the login and come back through [REDIRECT].
 */
object CarCloud {
    const val REDIRECT = "horizonnav://cloud"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var loop: Job? = null

    /** What the settings show: signed in or not, the last reading, or what went wrong. */
    var status by mutableStateOf("")

    class Reading(val pct: Int, val km: Int?, val charging: Boolean, val minLeft: Int?)

    fun provider(p: Prefs): String = p.s(PreferenceKeys.CLOUD, "none").let { if (it == "volvo" || it == "smartcar" || it == "bmw") it else "none" }
    fun signedIn(p: Prefs): Boolean = provider(p) != "none" && p.s(PreferenceKeys.CLOUD_REFRESH, "").isNotBlank()
    // (BMW's sign-in is a public one: only the client ID, no secret)
    private fun keysOk(p: Prefs): Boolean = p.s(PreferenceKeys.CLOUD_ID, "").isNotBlank() && (provider(p) == "bmw" || p.s(PreferenceKeys.CLOUD_SECRET, "").isNotBlank()) &&
        (provider(p) != "volvo" || p.s(PreferenceKeys.CLOUD_VCC, "").isNotBlank())

    // ---- the login (in the browser) ----

    private fun b64url(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** The address of the sign-in page for the chosen maker, or null while the keys are missing. Keeps the one-time values for the way back. */
    fun loginUrl(p: Prefs): String? {
        if (provider(p) == "none" || !keysOk(p)) return null
        val state = b64url(ByteArray(16).also { SecureRandom().nextBytes(it) })
        p.put(PreferenceKeys.CLOUD_STATE, state)
        val id = p.s(PreferenceKeys.CLOUD_ID, "").trim()
        return if (provider(p) == "volvo") {
            val verifier = b64url(ByteArray(48).also { SecureRandom().nextBytes(it) })
            p.put(PreferenceKeys.CLOUD_VERIFIER, verifier)
            val challenge = b64url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
            "https://auth.volvocars.com/realms/volvocars/protocol/openid-connect/auth?response_type=code&client_id=${enc(id)}&redirect_uri=${enc(REDIRECT)}" +
                "&scope=${enc("openid offline_access conve:vehicle_relation energy:state:read")}&state=$state&code_challenge=$challenge&code_challenge_method=S256"
        } else {
            "https://connect.smartcar.com/oauth/authorize?response_type=code&client_id=${enc(id)}&redirect_uri=${enc(REDIRECT)}" +
                "&scope=${enc("read_vehicle_info read_battery read_charge")}&mode=live&state=$state"
        }
    }

    /** Opens the sign-in page in the browser. Returns false (with a note in [status]) when something is missing. */
    fun signIn(ctx: Context, p: Prefs): Boolean {
        if (provider(p) == "bmw") {
            if (!keysOk(p)) { status = Lang.t("Erst die Client-ID eintragen", "Enter the client ID first"); return false }
            bmwSignIn(ctx, p); return true
        }
        val url = loginUrl(p)
        if (url == null) { status = Lang.t("Erst Anbieter, Client-ID und Secret eintragen", "Enter the provider, client ID and secret first"); return false }
        return try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); status = Lang.t("Anmeldung im Browser…", "Signing in in the browser…"); true
        } catch (e: Exception) { status = Lang.t("Kein Browser gefunden", "No browser found"); false }
    }

    /** The browser came back with [REDIRECT] and a code: swaps it for tokens. True when the intent was ours (also when it failed). */
    fun handle(ctx: Context, p: Prefs, i: Intent?): Boolean {
        val u = i?.data ?: return false
        if (u.scheme != "horizonnav" || u.host != "cloud") return false
        val code = u.getQueryParameter("code")
        val err = u.getQueryParameter("error")
        if (code == null || err != null) { status = Lang.t("Anmeldung abgebrochen", "Sign-in cancelled") + (err?.let { " ($it)" } ?: ""); return true }
        if (u.getQueryParameter("state") != p.s(PreferenceKeys.CLOUD_STATE, "")) { status = Lang.t("Anmeldung abgelehnt (falscher Status)", "Sign-in refused (wrong state)"); return true }
        status = Lang.t("Anmeldung läuft…", "Signing in…")
        scope.launch {
            try {
                tokens(p, mapOf("grant_type" to "authorization_code", "code" to code, "redirect_uri" to REDIRECT) +
                    (if (provider(p) == "volvo") mapOf("code_verifier" to p.s(PreferenceKeys.CLOUD_VERIFIER, "")) else emptyMap()))
                p.put(PreferenceKeys.CLOUD_VERIFIER, ""); p.put(PreferenceKeys.CLOUD_STATE, "")
                refresh(ctx, p, force = true)
                start(ctx, p)
            } catch (e: Exception) { status = Lang.t("Anmeldung fehlgeschlagen: ", "Sign-in failed: ") + (e.message ?: "") }
        }
        return true
    }

    fun signOut(p: Prefs) {
        for (k in listOf(PreferenceKeys.CLOUD_ACCESS, PreferenceKeys.CLOUD_REFRESH, PreferenceKeys.CLOUD_EXPIRES, PreferenceKeys.CLOUD_VEHICLE, PreferenceKeys.CLOUD_CONTAINER, PreferenceKeys.CLOUD_CHARGING,
            PreferenceKeys.CLOUD_CHARGE_MIN, PreferenceKeys.CLOUD_AT)) p.put(k, "")
        status = Lang.t("Abgemeldet", "Signed out")
    }

    // ---- web ----

    private fun http(method: String, url: String, headers: Map<String, String>, form: Map<String, String>? = null, json: String? = null): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("User-Agent", "HorizonNav/1.0 (Android navigation app)")
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        if (form != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(form.entries.joinToString("&") { (k, v) -> enc(k) + "=" + enc(v) }.toByteArray()) }
        }
        if (json != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(json.toByteArray()) }
        }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        return code to body
    }

    private fun basic(p: Prefs) = "Basic " + Base64.encodeToString("${p.s(PreferenceKeys.CLOUD_ID, "").trim()}:${p.s(PreferenceKeys.CLOUD_SECRET, "").trim()}".toByteArray(), Base64.NO_WRAP)

    private fun tokenUrl(p: Prefs) = when (provider(p)) {
        "volvo" -> "https://auth.volvocars.com/realms/volvocars/protocol/openid-connect/token"
        "bmw" -> "https://customer.bmwgroup.com/gcdm/oauth/token"
        else -> "https://auth.smartcar.com/oauth/token"
    }

    private fun reason(code: Int, body: String): String =
        "HTTP $code" + (try { JSONObject(body).let { it.optString("error_description").ifBlank { it.optString("error") } } } catch (e: Exception) { "" }).let { if (it.isBlank()) "" else ": $it" }

    /** Keeps the tokens of a token service's answer. */
    private fun storeTokens(p: Prefs, body: String) {
        val o = JSONObject(body)
        p.put(PreferenceKeys.CLOUD_ACCESS, o.getString("access_token"))
        o.optString("refresh_token").takeIf { it.isNotBlank() }?.let { p.put(PreferenceKeys.CLOUD_REFRESH, it) }
        p.put(PreferenceKeys.CLOUD_EXPIRES, (System.currentTimeMillis() + o.optLong("expires_in", 3600L) * 1000L).toString())
    }

    /** Asks the token service (login code or refresh token) and keeps the answer. Throws with a short reason when refused. */
    private fun tokens(p: Prefs, form: Map<String, String>) {
        val bmw = provider(p) == "bmw"
        val (code, body) = http("POST", tokenUrl(p),
            if (bmw) mapOf("Accept" to "application/json") else mapOf("Authorization" to basic(p), "Accept" to "application/json"),
            if (bmw) form + ("client_id" to p.s(PreferenceKeys.CLOUD_ID, "").trim()) else form)
        if (code !in 200..299) throw java.io.IOException(reason(code, body))
        storeTokens(p, body)
    }

    /**
     * BMW's sign-in: the app asks for a short code, opens BMW's page, where the driver enters it and agrees, and meanwhile waits for the
     * tokens (the "device code" flow, with a code challenge). Needs only the client ID made in the CarData portal.
     */
    private fun bmwSignIn(ctx: Context, p: Prefs) {
        status = Lang.t("Verbinde mit BMW…", "Connecting to BMW…")
        scope.launch {
            try {
                val id = p.s(PreferenceKeys.CLOUD_ID, "").trim()
                val verifier = b64url(ByteArray(48).also { SecureRandom().nextBytes(it) })
                val challenge = b64url(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
                val (c, b) = http("POST", "https://customer.bmwgroup.com/gcdm/oauth/device/code", mapOf("Accept" to "application/json"),
                    mapOf("client_id" to id, "response_type" to "device_code", "scope" to "authenticate_user openid cardata:api:read", "code_challenge" to challenge, "code_challenge_method" to "S256"))
                if (c !in 200..299) throw java.io.IOException(reason(c, b))
                val o = JSONObject(b)
                val user = o.getString("user_code"); val dev = o.getString("device_code")
                val uri = o.optString("verification_uri_complete").ifBlank { o.optString("verification_uri") }
                var interval = o.optLong("interval", 5L).coerceAtLeast(3L)
                val until = System.currentTimeMillis() + o.optLong("expires_in", 300L) * 1000L
                status = Lang.t("Gib im Browser diesen Code ein: ", "Enter this code in the browser: ") + user
                try { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) { }
                while (System.currentTimeMillis() < until) {
                    delay(interval * 1000L)
                    val (c2, b2) = http("POST", tokenUrl(p), mapOf("Accept" to "application/json"),
                        mapOf("grant_type" to "urn:ietf:params:oauth:grant-type:device_code", "device_code" to dev, "client_id" to id, "code_verifier" to verifier))
                    if (c2 in 200..299) {
                        storeTokens(p, b2)
                        p.put(PreferenceKeys.CLOUD_VEHICLE, ""); p.put(PreferenceKeys.CLOUD_CONTAINER, "")
                        refresh(ctx, p, force = true); start(ctx, p)
                        return@launch
                    }
                    val err = try { JSONObject(b2).optString("error") } catch (e: Exception) { "" }
                    if (err == "slow_down") { interval += 5; continue }
                    if (err == "authorization_pending" || err.isBlank() && c2 == 400) continue
                    throw java.io.IOException(reason(c2, b2))
                }
                status = Lang.t("Der Code ist abgelaufen, bitte noch einmal anmelden", "The code expired, please sign in again")
            } catch (e: Exception) { status = Lang.t("Anmeldung fehlgeschlagen: ", "Sign-in failed: ") + (e.message ?: "") }
        }
    }

    /** A valid access token, refreshed when it is about to run out; null when signing in again is needed. */
    private fun token(p: Prefs): String? {
        val exp = p.s(PreferenceKeys.CLOUD_EXPIRES, "").toLongOrNull() ?: 0L
        val have = p.s(PreferenceKeys.CLOUD_ACCESS, "")
        if (have.isNotBlank() && exp - System.currentTimeMillis() > 60_000L) return have
        val rt = p.s(PreferenceKeys.CLOUD_REFRESH, "")
        if (rt.isBlank()) return null
        return try { tokens(p, mapOf("grant_type" to "refresh_token", "refresh_token" to rt)); p.s(PreferenceKeys.CLOUD_ACCESS, "") }
        catch (e: Exception) {
            // refused (the sign-in is no longer valid): the next one has to be a new login
            if (e.message?.contains("HTTP 400") == true || e.message?.contains("HTTP 401") == true) { p.put(PreferenceKeys.CLOUD_REFRESH, ""); p.put(PreferenceKeys.CLOUD_ACCESS, "") }
            null
        }
    }

    // ---- the readings ----

    private fun volvo(p: Prefs, tok: String): Reading? {
        val h = mapOf("Authorization" to "Bearer $tok", "vcc-api-key" to p.s(PreferenceKeys.CLOUD_VCC, "").trim(), "Accept" to "application/json")
        var vin = p.s(PreferenceKeys.CLOUD_VEHICLE, "")
        if (vin.isBlank()) {
            val (c, b) = http("GET", "https://api.volvocars.com/connected-vehicle/v2/vehicles", h)
            if (c !in 200..299) throw java.io.IOException("Fahrzeugliste HTTP $c")
            vin = JSONObject(b).optJSONArray("data")?.optJSONObject(0)?.optString("vin").orEmpty()
            if (vin.isBlank()) throw java.io.IOException(Lang.t("Kein Fahrzeug im Konto", "No vehicle in the account"))
            p.put(PreferenceKeys.CLOUD_VEHICLE, vin)
        }
        val (c, b) = http("GET", "https://api.volvocars.com/energy/v2/vehicles/$vin/state", h)
        if (c !in 200..299) throw java.io.IOException("Energiedaten HTTP $c")
        val root = JSONObject(b).let { it.optJSONObject("data") ?: it }
        fun num(k: String): Double? = root.optJSONObject(k)?.takeIf { it.optString("status") == "OK" && it.has("value") }?.optDouble("value")?.takeIf { !it.isNaN() }
        fun str(k: String): String? = root.optJSONObject(k)?.takeIf { it.optString("status") == "OK" }?.optString("value")
        val pct = num("batteryChargeLevel")?.let { Math.round(it).toInt().coerceIn(0, 100) } ?: return null
        val rangeObj = root.optJSONObject("electricRange")
        val km = num("electricRange")?.let { v -> if (rangeObj?.optString("unit") == "mi") v * 1.609 else v }?.let { Math.round(it).toInt() }?.takeIf { it >= 1 }
        val charging = str("chargingStatus") == "CHARGING"
        val mins = if (charging) num("estimatedChargingTimeToTargetBatteryChargeLevel")?.toInt()?.takeIf { it > 0 } else null
        return Reading(pct, km, charging, mins)
    }

    // BMW's CarData: a "container" names the data points that are kept for the driver's account; the app makes one (or finds its own) and reads it.
    // The portal allows 50 calls a day, so BMW is read rarely and counted.
    private val BMW_KEYS = listOf("vehicle.drivetrain.batteryManagement.header", "vehicle.powertrain.electric.battery.stateOfCharge.displayed",
        "vehicle.drivetrain.electricEngine.kombiRemainingElectricRange", "vehicle.drivetrain.lastRemainingRange",
        "vehicle.drivetrain.electricEngine.charging.status", "vehicle.drivetrain.electricEngine.charging.timeRemaining")
    private const val BMW_HOST = "https://api-cardata.bmwgroup.com"
    private const val BMW_DAILY_MAX = 44

    /** Counts a BMW call against today's allowance (UTC day); throws once it is used up. */
    private fun bmwCall(p: Prefs) {
        val day = (System.currentTimeMillis() / 86_400_000L).toString()
        val used = callsToday(p)
        if (used >= BMW_DAILY_MAX) throw java.io.IOException(Lang.t("BMW erlaubt nur 50 Abrufe am Tag, morgen wieder", "BMW allows only 50 calls a day, again tomorrow"))
        p.put(PreferenceKeys.CLOUD_CALLS, "$day:${used + 1}")
    }

    /** The first string under [key] anywhere in [j] (objects and lists), or null. */
    private fun findString(j: Any?, key: String): String? = when (j) {
        is JSONObject -> j.optString(key).takeIf { j.has(key) && it.isNotBlank() } ?: j.keys().asSequence().firstNotNullOfOrNull { findString(j.opt(it), key) }
        is org.json.JSONArray -> (0 until j.length()).firstNotNullOfOrNull { findString(j.opt(it), key) }
        else -> null
    }

    /** The first object anywhere in [j] with the string [key] equal to [value]. */
    private fun findObj(j: Any?, key: String, value: String): JSONObject? = when (j) {
        is JSONObject -> if (j.optString(key) == value) j else j.keys().asSequence().firstNotNullOfOrNull { findObj(j.opt(it), key, value) }
        is org.json.JSONArray -> (0 until j.length()).firstNotNullOfOrNull { findObj(j.opt(it), key, value) }
        else -> null
    }

    private fun bmw(p: Prefs, tok: String): Reading? {
        val h = mapOf("Authorization" to "Bearer $tok", "x-version" to "v1", "Accept" to "application/json")
        var vin = p.s(PreferenceKeys.CLOUD_VEHICLE, "")
        if (vin.isBlank()) {
            bmwCall(p)
            val (c, b) = http("GET", "$BMW_HOST/customers/vehicles/mappings", h)
            if (c !in 200..299) throw java.io.IOException(Lang.t("Fahrzeugliste ", "Vehicle list ") + reason(c, b))
            vin = findString(try { JSONObject(b) } catch (e: Exception) { org.json.JSONArray(b) }, "vin").orEmpty()
            if (vin.isBlank()) throw java.io.IOException(Lang.t("Kein Fahrzeug im Konto (CarData im BMW-Portal aktivieren)", "No vehicle in the account (activate CarData in the BMW portal)"))
            p.put(PreferenceKeys.CLOUD_VEHICLE, vin)
        }
        var cid = p.s(PreferenceKeys.CLOUD_CONTAINER, "")
        if (cid.isBlank()) {
            bmwCall(p)
            val (c, b) = http("GET", "$BMW_HOST/customers/containers", h)
            if (c in 200..299) {
                val mine = findObj(try { JSONObject(b) } catch (e: Exception) { org.json.JSONArray(b) }, "name", "Horizon Nav")
                cid = mine?.let { it.optString("containerId").ifBlank { it.optString("id") } }.orEmpty()
            }
            if (cid.isBlank()) {
                bmwCall(p)
                val body = JSONObject().put("name", "Horizon Nav").put("purpose", "Akkustand und Reichweite fuer die Navigation")
                    .put("technicalDescriptors", org.json.JSONArray(BMW_KEYS)).toString()
                val (c2, b2) = http("POST", "$BMW_HOST/customers/containers", h, json = body)
                if (c2 !in 200..299) throw java.io.IOException(Lang.t("Container ", "Container ") + reason(c2, b2))
                val o = JSONObject(b2)
                cid = findString(o, "containerId") ?: findString(o, "id").orEmpty()
            }
            if (cid.isBlank()) throw java.io.IOException(Lang.t("Kein Container", "No container"))
            p.put(PreferenceKeys.CLOUD_CONTAINER, cid)
        }
        bmwCall(p)
        val (c, b) = http("GET", "$BMW_HOST/customers/vehicles/$vin/telematicData?containerId=${enc(cid)}", h)
        if (c == 404 || c == 403) { p.put(PreferenceKeys.CLOUD_CONTAINER, ""); throw java.io.IOException(reason(c, b)) }
        if (c !in 200..299) throw java.io.IOException(reason(c, b))
        val data = JSONObject(b).optJSONObject("telematicData") ?: return null
        fun v(k: String): String? = data.optJSONObject(k)?.optString("value")?.takeIf { it.isNotBlank() && !it.equals("null", true) && !it.equals("INVALID", true) }
        val pct = (v("vehicle.drivetrain.batteryManagement.header") ?: v("vehicle.powertrain.electric.battery.stateOfCharge.displayed"))?.replace(',', '.')?.toDoubleOrNull()
            ?.let { Math.round(it).toInt().coerceIn(0, 100) } ?: return null
        val rangeKey = if (v("vehicle.drivetrain.electricEngine.kombiRemainingElectricRange") != null) "vehicle.drivetrain.electricEngine.kombiRemainingElectricRange" else "vehicle.drivetrain.lastRemainingRange"
        val km = v(rangeKey)?.replace(',', '.')?.toDoubleOrNull()?.let { r -> if (data.optJSONObject(rangeKey)?.optString("unit")?.lowercase()?.startsWith("mi") == true) r * 1.609 else r }
            ?.let { Math.round(it).toInt() }?.takeIf { it >= 1 }
        val st = v("vehicle.drivetrain.electricEngine.charging.status")?.uppercase().orEmpty()
        val charging = st.contains("CHARGINGACTIVE") || st == "CHARGING"
        val mins = if (charging) v("vehicle.drivetrain.electricEngine.charging.timeRemaining")?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 } else null
        return Reading(pct, km, charging, mins)
    }

    private fun smartcar(p: Prefs, tok: String): Reading? {
        val h = mapOf("Authorization" to "Bearer $tok", "sc-unit-system" to "metric", "Accept" to "application/json")
        var id = p.s(PreferenceKeys.CLOUD_VEHICLE, "")
        if (id.isBlank()) {
            val (c, b) = http("GET", "https://api.smartcar.com/v2.0/vehicles", h)
            if (c !in 200..299) throw java.io.IOException("Fahrzeugliste HTTP $c")
            id = JSONObject(b).optJSONArray("vehicles")?.optString(0).orEmpty()
            if (id.isBlank()) throw java.io.IOException(Lang.t("Kein Fahrzeug im Konto", "No vehicle in the account"))
            p.put(PreferenceKeys.CLOUD_VEHICLE, id)
        }
        val (c, b) = http("GET", "https://api.smartcar.com/v2.0/vehicles/$id/battery", h)
        if (c !in 200..299) throw java.io.IOException("Akkustand HTTP $c")
        val o = JSONObject(b)
        if (!o.has("percentRemaining")) return null
        val pct = Math.round(o.optDouble("percentRemaining", 0.0) * 100).toInt().coerceIn(0, 100)
        val km = o.optDouble("range", Double.NaN).takeIf { !it.isNaN() }?.let { Math.round(it).toInt() }?.takeIf { it >= 1 }
        // whether it charges (a second call that may fail without spoiling the reading)
        var charging = false
        try {
            val (c2, b2) = http("GET", "https://api.smartcar.com/v2.0/vehicles/$id/charge", h)
            if (c2 in 200..299) charging = JSONObject(b2).optString("state") == "CHARGING"
        } catch (e: Exception) { }
        return Reading(pct, km, charging, null)
    }

    /**
     * Reads the battery now and passes it on. Off the main thread. Skips the call while Android Auto's own live reading is fresh (the
     * cloud is slower and may count calls), unless [force].
     */
    fun refresh(ctx: Context, p: Prefs, force: Boolean = false) {
        lastTry = System.currentTimeMillis()
        if (provider(p) == "none") return
        if (!keysOk(p)) return
        if (!force && EvWidget.source(p) == "car" && System.currentTimeMillis() - EvWidget.readAt(p) < 10 * 60_000L) return
        try {
            val tok = token(p)
            if (tok == null) { status = Lang.t("Bitte neu anmelden", "Please sign in again"); return }
            val r = when (provider(p)) { "volvo" -> volvo(p, tok); "bmw" -> bmw(p, tok); else -> smartcar(p, tok) }
            if (r == null) { status = Lang.t("Das Auto meldet keinen Akkustand", "The car sends no battery level"); return }
            EvWidget.cloudReading(p, r.pct, r.km)
            p.put(PreferenceKeys.CLOUD_CHARGING, if (r.charging) "1" else "")
            p.put(PreferenceKeys.CLOUD_CHARGE_MIN, r.minLeft?.toString() ?: "")
            p.put(PreferenceKeys.CLOUD_AT, System.currentTimeMillis().toString())
            status = Lang.t("Angemeldet · ", "Signed in · ") + "${r.pct} %" + (r.km?.let { " · $it km" } ?: "") + (if (r.charging) Lang.t(" · lädt", " · charging") else "")
        } catch (e: Exception) { status = Lang.t("Abruf fehlgeschlagen: ", "Request failed: ") + (e.message ?: "") }
    }

    // ---- when to read: only while the app is really in use ----

    /** The app is open on the phone. Set by the activity. */
    @Volatile var appVisible = false
    /** Android Auto is showing the app. Set by [com.headunit.launcher.car.CarEnergy]. */
    @Volatile var carActive = false
    /** Whether a route is running (the app may be in the background then). Set by the activity. */
    @Volatile var navigating: () -> Boolean = { false }
    private var lastTry = 0L

    private fun inUse() = appVisible || carActive || try { navigating() } catch (e: Exception) { false }

    /** How often to read, in minutes (Settings): BMW starts slower because of its 50 calls a day, the others at 10. */
    fun rateMin(p: Prefs): Int = p.s(PreferenceKeys.CLOUD_RATE + provider(p), "").toIntOrNull()?.coerceIn(2, 240) ?: if (provider(p) == "bmw") 30 else 10

    /** BMW calls used today (of 50). */
    fun callsToday(p: Prefs): Int {
        val day = (System.currentTimeMillis() / 86_400_000L).toString()
        val parts = p.s(PreferenceKeys.CLOUD_CALLS, "").split(":")
        return if (parts.getOrNull(0) == day) parts.getOrNull(1)?.toIntOrNull() ?: 0 else 0
    }

    /** The app came to the front: one reading straight away (unless one was just made), and the regular ones start. */
    fun wake(ctx: Context, p: Prefs) {
        appVisible = true
        start(ctx, p)
        if (!signedIn(p) || System.currentTimeMillis() - lastTry < 60_000L) return
        lastTry = System.currentTimeMillis()
        val app = ctx.applicationContext
        scope.launch { refresh(app, p) }
    }

    /** Starts the checks (every half minute, free of cost); a reading is only made while the app is in use and the chosen rate has passed. */
    fun start(ctx: Context, p: Prefs) {
        if (loop?.isActive == true) return
        val app = ctx.applicationContext
        loop = scope.launch {
            while (true) {
                delay(30_000L)
                if (!signedIn(p) || !inUse()) continue
                if (System.currentTimeMillis() - lastTry < rateMin(p) * 60_000L) continue
                lastTry = System.currentTimeMillis()
                refresh(app, p)
            }
        }
    }
}
