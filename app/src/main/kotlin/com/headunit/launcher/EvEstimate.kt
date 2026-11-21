package com.headunit.launcher

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.res.stringResource

/**
 * The battery estimate for electric cars: the range and charge left, learned from the drive (how far a percent of charge goes,
 * the hills, wind, temperature, the driving style), and the charges read from the car. Lives on the engine as [NavEngine.ev]:
 * it reads the engine's state through [n] and keeps its own.
 */
internal class EvEstimate(private val n: NavEngine) {
    // ---- electric cars: battery estimate ----
    val isEv: Boolean get() = n.prefs.s(PreferenceKeys.FUEL_TYPE, "E10") == "EV"
    /** Usable battery (kWh): the chosen car's, else from Settings > Fuel (60 if not set). */
    internal fun evBatteryKwh(): Double = EvCars.batteryKwh(n.prefs) ?: (n.prefs.s(PreferenceKeys.EV_BATTERY_KWH, "").replace(',', '.').toDoubleOrNull() ?: 60.0).coerceIn(10.0, 200.0)
    /** kWh used per km: from the efficiency in Settings > Fuel (miles per kWh), or a typical 0.18. */
    /** kWh per km from Settings > Effizienz, which is given in km per kWh (it was read as miles per kWh before: far too hopeful). */
    internal fun evKwhPerKm(): Double = n.prefs.s(PreferenceKeys.EVMPK, "").replace(',', '.').toDoubleOrNull()?.takeIf { it in 2.0..15.0 }?.let { 1.0 / it } ?: 0.18
    /** Roughly how much battery (%) you'll arrive with: the charge you set before the trip, less what's been driven and what's left. */
    /** The battery level (%) the car reported, if it was read in the last 6 hours, or null. */
    fun evNowPct(): Int? {
        if (!isEv) return null
        val v = n.prefs.s(PreferenceKeys.EV_NOW_PCT, "").toIntOrNull() ?: return null
        val age = evNowAgeMin() ?: return null
        return if (age <= 360 || evParkedSinceRead(age)) v.coerceIn(0, 100) else null
    }

    /**
     * Not driven since the last reading (the car simply stood): the reading stays true for up to a week. After 1 or 6 hours it
     * used to give way to an estimate (the range from the battery and the set efficiency, or the charge set before the trip),
     * and a parked car showed a clearly lower battery or range without moving.
     */
    internal fun evParkedSinceRead(ageMin: Long): Boolean {
        if (ageMin > 7 * 24 * 60L) return false
        evEnsureLoaded()
        return evSinceRead / evBatteryKwh() * 100.0 < 0.5
    }

    /** Minutes since the battery level was last read, or null. */
    fun evNowAgeMin(): Long? = EvWidget.readAt(n.prefs).takeIf { it > 0 }?.let { (System.currentTimeMillis() - it) / 60_000L }?.takeIf { it >= 0 }  // a clock set back: not fresh

    /** The range in km the widget reported, if it was read in the last 60 minutes, or null. */
    fun evNowKm(): Int? {
        if (!isEv) return null
        val v = n.prefs.s(PreferenceKeys.EV_NOW_KM, "").toIntOrNull() ?: return null
        // a range under 5 km is no real reading (the car or widget sent 0 or nothing usable): treat it as unknown
        val age = evNowAgeMin() ?: return null
        return if ((age <= 60 || evParkedSinceRead(age)) && v >= 5) v.coerceAtMost(1500) else null
    }

    /** The range now and the full range, in km: the widget's own figures when they're fresh, else from the battery and efficiency. */
    internal fun evRangeKm(): Pair<Double, Double> {
        val nowKm = evNowKm(); val nowPct = evNowPct()?.takeIf { evNowAgeMin()?.let { a -> a <= 60 || evParkedSinceRead(a) } == true }
        val full = if (nowKm != null && nowPct != null && nowPct > 0 && nowKm > 0) nowKm * 100.0 / nowPct else evBatteryKwh() / evKwhPerKm()
        // where the battery is now (the car's reading less what has been driven since, the charge at the last charging stop, or
        // the charge set before the trip less what has been driven on it): a plan made mid-trip starts from here, not from the start
        val readingWins = (nowKm != null || nowPct != null) && EvWidget.readAt(n.prefs) >= evChargeRefAt
        val start = when {
            readingWins && nowKm != null && nowPct == null -> nowKm - evSinceRead / evBaseKwhPerKm()
            else -> evNowEstimatePct() / 100.0 * full
        }
        return start.coerceAtLeast(0.0) to full
    }

    /** The charge at the last charging stop reached on this trip (%), when it was reached, and the kWh driven since. */
    internal var evChargeRefPct = -1.0
    internal var evChargeRefAt = 0L
    internal var evSinceCharge = 0.0

    /** The height (m) the last climb or descent without a route was counted from, from the phone's GPS. */
    internal var evAltRef: Double? = null
    /** The distance driven since the app began (kept over trips and restarts, unlike tripDist), when the state was last saved, and whether it is loaded. */
    internal var evDriven = 0.0
    internal var evSavedAt = 0L
    internal var evLoaded = false
    /** The battery level at the previous reading (to see a rise, that is a charge), -1 when none yet. */
    internal var evPrevPct = -1
    /** True when a stretch of the drive since the reference reading could not be measured (a gap off the route): no learning from it. */
    internal var evGapBad = false

    /**
     * The learning state, by name (JSON): a field added later is simply missing in an older saved state and keeps its default,
     * instead of the whole state being thrown away as with the plain list of numbers before.
     */
    internal fun evState(): org.json.JSONObject = org.json.JSONObject().apply {
        put("refPct", evRefPct); put("refDist", evRefDist); put("refModel", evRefModel)
        put("refModelB", org.json.JSONArray(evRefModelB.toList())); put("refAux", evRefAux); put("refUp", evRefUp); put("refDown", evRefDown)
        put("refKm", org.json.JSONArray(evRefKm.toList())); put("readSeen", evReadSeen); put("sinceRead", evSinceRead); put("sinceNav", evSinceNav)
        put("sinceCharge", evSinceCharge); put("driven", evDriven); put("chargeRefPct", evChargeRefPct); put("chargeRefAt", evChargeRefAt)
        put("gapBad", evGapBad); put("prevPct", evPrevPct)
        put("chargeReadAt", evChargeReadAt); put("chargeLastPct", evChargeLastPct); put("charging", evChargingNow); put("chargingAt", evChargingAt)
    }

    /** Saved to the settings, so the reference reading and the drive since it survive a restart of the app. */
    /**
     * Whether this engine learns and saves. With Android Auto two engines run (the car's and the phone's, which can follow the same
     * trip): both saw the same readings, and each learning step was applied twice. Only the car's learns while it is connected.
     */
    internal fun evLearner(): Boolean = n.isCar || !RouteSync.carConnected()

    internal fun evSave() {
        if (!evLearner()) return
        // never before the saved state is read: that would write over it with empty values
        evEnsureLoaded()
        evSavedAt = System.currentTimeMillis()
        try { n.prefs.put(PreferenceKeys.EV_STATE, evState().toString()) } catch (e: Exception) { softFail(e) }
    }

    /** Loaded once, before first use: the state of the last drive (also the older plain list of 21 numbers). */
    internal fun evLoad() {
        val raw = n.prefs.s(PreferenceKeys.EV_STATE, "").trim()
        if (raw.isEmpty()) return
        try {
            if (raw.startsWith("{")) {
                val o = org.json.JSONObject(raw)
                fun arr(k: String, into: DoubleArray) { o.optJSONArray(k)?.let { a -> for (i in into.indices) into[i] = a.optDouble(i, 0.0) } }
                evRefPct = o.optInt("refPct", -1); evRefDist = o.optDouble("refDist", 0.0); evRefModel = o.optDouble("refModel", 0.0)
                arr("refModelB", evRefModelB); evRefAux = o.optDouble("refAux", 0.0); evRefUp = o.optDouble("refUp", 0.0); evRefDown = o.optDouble("refDown", 0.0)
                arr("refKm", evRefKm); evReadSeen = o.optLong("readSeen", 0L); evSinceRead = o.optDouble("sinceRead", 0.0); evSinceNav = o.optDouble("sinceNav", 0.0)
                evSinceCharge = o.optDouble("sinceCharge", 0.0); evDriven = o.optDouble("driven", 0.0); evChargeRefPct = o.optDouble("chargeRefPct", -1.0)
                evChargeRefAt = o.optLong("chargeRefAt", 0L); evGapBad = o.optBoolean("gapBad", false); evPrevPct = o.optInt("prevPct", -1)
                evChargeReadAt = o.optLong("chargeReadAt", 0L); evChargeLastPct = o.optDouble("chargeLastPct", -1.0); evChargingAt = o.optLong("chargingAt", 0L)
                // still charging after a restart: only when it was seen within the last two hours (it ends when the car moves off)
                evChargingNow = o.optBoolean("charging", false) && System.currentTimeMillis() - evChargingAt in 0..2 * 3_600_000L
                return
            }
            // the older form: 21 numbers in a fixed order
            val v = raw.split(",").mapNotNull { it.toDoubleOrNull() }
            if (v.size != 21) return
            evRefPct = v[0].toInt(); evRefDist = v[1]; evRefModel = v[2]
            for (i in 0..2) evRefModelB[i] = v[3 + i]
            evRefAux = v[6]; evRefUp = v[7]; evRefDown = v[8]
            for (i in 0..2) evRefKm[i] = v[9 + i]
            evReadSeen = v[12].toLong(); evSinceRead = v[13]; evSinceNav = v[14]; evSinceCharge = v[15]
            evDriven = v[16]; evChargeRefPct = v[17]; evChargeRefAt = v[18].toLong(); evGapBad = v[19] > 0.5; evPrevPct = v[20].toInt()
        } catch (e: Exception) { softFail(e) }
    }
    /** Charging at a charger: the last battery reading seen (its time and level), and whether the level rose while standing there. */
    internal var evChargeReadAt = 0L
    internal var evChargeLastPct = -1.0
    internal var evChargingNow = false
    /** When charging was last seen (a rise at a charger), to show it again after a restart of the app. */
    internal var evChargingAt = 0L
    /** True while the car stands at a charger and the battery level has risen (until it moves off), also after a restart. */
    fun evCharging(): Boolean { evEnsureLoaded(); return evChargingNow }

    /** The range now in km, from the same estimate (null when there is no reading yet). */
    fun evEstimateKm(): Int? = Math.round(evRangeKm().first).toInt().takeIf { it > 0 }

    /** True when the last real reading is so old that the battery figure on screen is an estimate. */
    fun evEstimateOld(): Boolean = (evNowAgeMin() ?: Long.MAX_VALUE) > 10

    /**
     * The battery now, in %: the car's reading (60 minutes at most) less the modelled kWh driven since it; or, when a charging stop
     * was reached after that reading, the charge planned there less what has been driven since; or the charge set before the
     * trip less what has been driven since the start.
     */
    fun evNowEstimatePct(): Double {
        evEnsureLoaded()
        val batt = evBatteryKwh()
        // the last level read, up to 6 hours old, less what was used since (shown with "ca."): after an hour it used to fall back
        // to the charge set before the trip (80 %), even when the battery was much lower
        val nowPct = evNowPct()?.toDouble()
        val nowKm = evNowKm()
        val readAt = EvWidget.readAt(n.prefs)
        val fromReading = nowPct ?: nowKm?.let { km -> km * evBaseKwhPerKm() / batt * 100.0 }
        val pct = when {
            fromReading != null && readAt >= evChargeRefAt -> fromReading - evSinceRead / batt * 100.0
            evChargeRefPct >= 0 -> evChargeRefPct - evSinceCharge / batt * 100.0
            else -> n.prefs.i(PreferenceKeys.EV_CHARGE_PCT, 80).coerceIn(1, 100) - (if (n.navStartMs > 0) evSinceNav else 0.0) / batt * 100.0
        }
        return pct.coerceIn(0.0, 100.0)
    }

    // ---- electric: what the drive really uses (see EvModel) ----
    /** The modelled kWh driven since the battery level was last read, and since the route started. */
    internal var evSinceRead = 0.0
    internal var evSinceNav = 0.0
    internal var evReadSeen = 0L
    /** For learning: the battery level, and the modelled kWh (and per kind of road, the km) since then. */
    internal var evRefPct = -1
    internal var evRefDist = 0.0
    internal var evRefModel = 0.0
    /** The modelled road use since the reference reading, per kind of road, and the heating / air conditioning in it. */
    internal val evRefModelB = DoubleArray(3)
    internal var evRefAux = 0.0
    internal var evTrackAt = 0L
    internal val evRefKm = DoubleArray(3)
    /** Since the battery reading: the modelled kWh for climbing, and the kWh that going downhill could give back. */
    internal var evRefUp = 0.0
    internal var evRefDown = 0.0
    internal var evLastH: Double? = null
    internal var evHRoute: Route? = null
    /** How fast you drive against the limit on this trip (distance weighted), and where there's no limit. */
    internal var evStyleSum = 0.0; internal var evStyleM = 0.0
    internal var evFreeSum = 0.0; internal var evFreeM = 0.0
    /** How briskly you accelerate on this trip: the sum of acceleration x time while speeding up, and that time (s). */
    internal var evAccSum = 0.0; internal var evAccT = 0.0
    internal var evLastSpeedMs = -1.0
    internal var evTempC: Double? = null
    internal var evTempAt = 0L

    internal fun evBaseKwhPerKm(): Double {
        if (n.prefs.s(PreferenceKeys.EVMPK, "").isNotBlank()) return evKwhPerKm()
        // no efficiency set: the car's own figure (its range against its battery level). That figure already holds the speeds the car
        // was driven at lately (mixed driving), while the model's base is the use at 80 km/h and adds speed on top: so it is taken
        // as a tenth lower. It is kept (and only eased towards each new reading), so what is learned is corrected against a steady base
        n.prefs.s(PreferenceKeys.EV_BASE_GOM, "").toDoubleOrNull()?.takeIf { it in 0.08..0.4 }?.let { return it }
        evGomKwhPerKm()?.let { n.prefs.put(PreferenceKeys.EV_BASE_GOM, String.format(java.util.Locale.US, "%.4f", it)); return it }
        return 0.18
    }

    /** The car's own consumption figure (its range against its battery level, both fresh), normalised to 80 km/h; null when not known. */
    internal fun evGomKwhPerKm(): Double? {
        val km = evNowKm(); val pct = evNowPct()?.takeIf { (evNowAgeMin() ?: Long.MAX_VALUE) <= 60 }
        if (km == null || pct == null || pct < 5) return null
        return (evBatteryKwh() * pct / 100.0 / km / 1.1).coerceIn(0.10, 0.35)
    }

    internal fun evLearned(): DoubleArray = EvModel.parseLearned(n.prefs.s(PreferenceKeys.EV_LEARN, ""))

    /**
     * How much the way you accelerate costs against a usual driver: 1.0 for about 1.2 m/s² on average, more for a brisker one (up to
     * 1.35), less for a gentle one (down to 0.85). Not learned yet: 1.0.
     */
    internal fun evAccelF(): Double {
        val a = if (evAccT > 30.0) evAccSum / evAccT else n.prefs.s(PreferenceKeys.EV_ACCEL, "").toDoubleOrNull() ?: return 1.0
        return (1.0 + 0.25 * (a - 1.2)).coerceIn(0.85, 1.35)
    }

    internal fun evStyle(): Double = if (evStyleM > 3000) (evStyleSum / evStyleM).coerceIn(0.8, 1.3)
        else (n.prefs.s(PreferenceKeys.EV_STYLE, "").toDoubleOrNull() ?: 1.0).coerceIn(0.8, 1.3)

    internal fun evFreeKmh(): Double = if (evFreeM > 3000) (evFreeSum / evFreeM).coerceIn(100.0, 250.0)
        else (n.prefs.s(PreferenceKeys.EV_FREE_KMH, "").toDoubleOrNull() ?: 140.0).coerceIn(100.0, 250.0)

    /** The outside temperature: the route's weather, else the weather here (fetched every 30 minutes while driving). */
    /** The temperature where you are: on a route the forecast nearest to here, else the trip's own figure. */
    internal fun evTempHere(): Double? {
        val w = n.routeWx.takeIf { it.isNotEmpty() && n.route != null && !n.previewing }?.minByOrNull { abs(it.off - n.curProgress) }
        return w?.temp ?: evTemp()
    }

    internal fun evTemp(): Double? {
        n.routeWx.takeIf { it.isNotEmpty() }?.let { w -> return w.map { it.temp }.average() }
        n.weather?.let { return it.tempC }
        return evTempC
    }

    /**
     * A gap in the GPS track (a tunnel, a lost signal) while on a route: the distance is the way along the route between the two
     * fixes, not the straight line. Only when the route agrees with the straight line (no jump to another road).
     */
    internal fun evGapOnRoute(speed: Double, prev: LL, cur: LL, straight: Double): Boolean {
        val r = n.route ?: return false
        if (n.previewing || r.pts.size < 2) return false
        val a = RouteMatcher.snap(r, prev, 0); val b = RouteMatcher.snap(r, cur, 0)
        // both ends on the route, forwards, and near what the straight line allows (see TrackMath.gapAlong)
        val along = TrackMath.gapAlong(a.progress, a.dist, b.progress, b.dist, straight) ?: return false
        n.tripDist += along
        evTrack(speed, along)
        return true
    }

    /** The saved state is read before anything uses it: after a restart the estimate can be asked for before the first drive. */
    internal fun evEnsureLoaded() { if (!evLoaded) { evLoaded = true; evLoad() } }

    /**
     * Standing still on a route (a jam, lights, a break with the car on): the heating or air conditioning still runs, by the time.
     * Not while charging (the charger powers it) and not without a route (the car may be switched off).
     */
    internal fun evIdle() {
        if (!isEv) return
        evEnsureLoaded()
        // a new battery reading while standing: taken now (it used to wait for the next metres driven, and the estimate showed the
        // new reading less the use counted since the old one)
        if (EvWidget.readAt(n.prefs) != evReadSeen) { evTrack(0.0, 0.0); return }
        if (n.route == null || n.previewing || evChargingNow) { evTrackAt = 0L; return }
        evEnsureLoaded()
        val now = System.currentTimeMillis()
        val dtS = if (evTrackAt > 0) ((now - evTrackAt) / 1000.0).coerceIn(0.0, 60.0) else 0.0
        evTrackAt = now
        val aux = EvModel.auxKw(evTempHere()) * dtS / 3600.0
        if (aux <= 0.0) return
        evSinceRead += aux; evSinceNav += aux; evSinceCharge += aux
        if (evRefPct >= 0) evRefAux += aux
    }

    /** Every few metres driven (electric): the modelled use, how you drive against the limit, and learning from the battery level. */
    internal fun evTrack(speedMs: Double, d: Double) {
        evEnsureLoaded()
        evDriven += d
        if (System.currentTimeMillis() - evSavedAt > 60_000L) evSave()
        val kmh = speedMs * 3.6
        // how briskly you speed up: the acceleration over the last few metres, while the speed rises (not at a standstill or past 120 km/h)
        val last = evLastSpeedMs
        evLastSpeedMs = speedMs
        if (last >= 0.0 && speedMs > 1.0 && speedMs < 33.0 && d > 0.5) {
            val dt = d / maxOf(1.0, (speedMs + last) / 2.0)
            val acc = (speedMs - last) / dt
            if (dt in 0.2..5.0 && acc in 0.4..4.0) { evAccSum += acc * dt; evAccT += dt }
        }
        // how you drive against the limit: for every car (it also goes into the arrival time and a fuel car's range)
        val lim = n.speedLimit
        if (lim != null && lim > 5.0 && speedMs > lim * 0.6 && speedMs > 8.0) { evStyleSum += speedMs / lim * d; evStyleM += d }
        else if (lim == null && n.noLimit && kmh > 90.0) { evFreeSum += kmh * d; evFreeM += d }
        if (!isEv) return
        // the road's own use (speed, cold, wind, rain), then the hill climbed or gone down since the last point
        val wx = n.routeWx.takeIf { it.isNotEmpty() && n.route != null }?.minByOrNull { abs(it.off - n.curProgress) }
        val flatK = d / 1000.0 * evBaseKwhPerKm() * EvModel.speedFactor(kmh) * EvModel.tempFactor(evTempHere()) *
            EvModel.vehicleFactor(kmh, evDragExtra(), evMass(), EvModel.massFor(evBatteryKwh())) *
            (if (wx != null) EvModel.wxFactor(EvModel.Wx(wx.off, wx.windKmh, wx.windFrom, wx.wet, wx.snow), kmh, n.fix?.heading?.toDouble()) else 1.0)
        // heating / air conditioning: by the time since the last point (also standing in a jam), at most a minute at a time
        val nowT = System.currentTimeMillis()
        val dtS = if (evTrackAt > 0) ((nowT - evTrackAt) / 1000.0).coerceIn(0.0, 60.0) else 0.0
        evTrackAt = nowT
        val aux = EvModel.auxKw(evTempHere()) * dtS / 3600.0
        val bk = EvModel.bucket(kmh)
        var up = 0.0; var down = 0.0
        val r = n.route; val prof = r?.elev
        if (r != null && prof != null && !n.previewing) {
            if (evHRoute !== r) { evHRoute = r; evLastH = null }
            val h = EvModel.Env(prof.off, prof.h).heightAt(n.curProgress)
            val last = evLastH
            if (h != null && last != null && abs(h - last) < 150.0) {
                val g = EvModel.gradeKwh(h - last, evMass(), 1.0)
                if (g >= 0) up = g else down = -g
            }
            evLastH = h
            evAltRef = null
        } else {
            // no route: the height from the phone's GPS, with a good fix only, and only a change of 5 m or more counts (the GPS height
            // wobbles by a few metres). A missing height comes as 0.
            evLastH = null
            val alt = n.fix?.takeIf { it.alt != 0.0 && it.acc <= 15.0 }?.alt
            val ref = evAltRef
            if (alt != null) {
                if (ref == null) evAltRef = alt
                else if (abs(alt - ref) in 5.0..150.0) { val g = EvModel.gradeKwh(alt - ref, evMass(), 1.0); if (g >= 0) up = g else down = -g; evAltRef = alt }
            }
        }
        // what the battery is counted down by: with the correction learned for this kind of road (the planning uses it too)
        val kwh = flatK * (evLearned().getOrNull(bk) ?: 1.0) + aux + up - down * evRegen()
        evSinceRead += kwh; evSinceNav += kwh; evSinceCharge += kwh
        if (evRefPct >= 0) { evRefModel += flatK; evRefModelB[bk] += flatK; evRefAux += aux; evRefUp += up; evRefDown += down; evRefKm[bk] += d / 1000.0 }
        val now = System.currentTimeMillis()
        // also without a route: the cold and the heating always count
        if (evTemp() == null && now - evTempAt > 30 * 60_000L) {
            evTempAt = now
            val at = n.fix?.pos
            if (at != null) n.scope.launch { evTempC = withContext(Dispatchers.IO) { try { Weather.fetch(at)?.tempC } catch (e: Exception) { null } } ?: evTempC }
        }
        // a new battery reading
        val readAt = EvWidget.readAt(n.prefs)
        if (readAt != evReadSeen) {
            evReadSeen = readAt
            evSinceRead = 0.0
            evSave()
            // the car's own figure eases the steady base a little (a new car or season shows over time, one odd reading doesn't)
            if (evLearner() && n.prefs.s(PreferenceKeys.EVMPK, "").isBlank()) evGomKwhPerKm()?.let { g ->
                val old = n.prefs.s(PreferenceKeys.EV_BASE_GOM, "").toDoubleOrNull() ?: g
                n.prefs.put(PreferenceKeys.EV_BASE_GOM, String.format(java.util.Locale.US, "%.4f", old * 0.8 + g * 0.2))
            }
            val pct = evNowPct() ?: return
            // the level went up since the previous reading: the car was charged in between, so the drive before it is not learned from
            val rose = evPrevPct >= 0 && pct > evPrevPct
            evPrevPct = pct
            if (rose) { evStartRef(pct); return }
            // standing at a charger: nothing is learned while the car is charging, the reference is kept at the charge
            if (evChargingNow) { evStartRef(pct); return }
            if (evRefPct < 0 || pct > evRefPct) { evStartRef(pct); return }   // first reading, or charged since
            val drop = evRefPct - pct
            if (drop >= 5 && evDriven - evRefDist >= 10_000 && evRefModel > 0.5 && !evGapBad) {
                // what the battery really lost against what the model said (without its buffer and corrections)
                val actual = drop / 100.0 * evBatteryKwh()
                // one learning step (see EvModel.learnStep: the recovery downhill is taken off, a mostly downhill stretch isn't learned)
                if (evLearner()) EvModel.learnStep(evLearned(), evRefModelB, actual, evRefAux, evRefUp, evRefDown)?.let { n.prefs.put(PreferenceKeys.EV_LEARN, EvModel.formatLearned(it)) }
                evStartRef(pct)
            }
        }
    }

    internal fun evStartRef(pct: Int) { evRefPct = pct; evRefDist = evDriven; evGapBad = false; evRefModel = 0.0; evRefModelB.fill(0.0); evRefAux = 0.0; evRefUp = 0.0; evRefDown = 0.0; evRefKm.fill(0.0) }

    /**
     * Of the energy going downhill, the share that comes back: none. The estimate counts the full use on the hills, so it stays on
     * the safe side (also on mountain routes). The recovery is therefore not learned or shown any more.
     */
    internal fun evRegen(): Double = 0.0

    /** The weight: an ordinary car of this battery, or the motorhome / car with trailer from Settings > Fahrzeug when that is heavier. */
    internal fun evMass(): Double {
        val car = EvModel.massFor(evBatteryKwh())
        val v = Vehicle.from(n.prefs)
        return if (v.isCar) car else maxOf(car, v.weightKg.toDouble())
    }

    /** Extra air drag: a motorhome 70 %, a car with a trailer 60 %, none for a car. */
    internal fun evDragExtra(): Double = when (Vehicle.from(n.prefs).kind) { "camper" -> 0.7; "lorry" -> 0.6; else -> 0.0 }

    /** The hills and the weather along [r], for the estimate. */
    internal fun evEnv(r: Route): EvModel.Env {
        val prof = r.elev
        if (prof == null) evLoadElev(r)
        val wx = (if (n.route === r || n.routes.getOrNull(0) === r) n.routeWx else emptyList()).map { EvModel.Wx(it.off, it.windKmh, it.windFrom, it.wet, it.snow, it.temp) }
        return EvModel.Env(prof?.off ?: DoubleArray(0), prof?.h ?: DoubleArray(0), evMass(), evRegen(), wx, { off -> n.pointAt(r, off).second },
            evDragExtra(), EvModel.massFor(evBatteryKwh()), r.delays)
    }

    internal var elevAskedFor: Route? = null

    /** Loads the heights along [r] once (in the background), when online. */
    internal fun evLoadElev(r: Route) {
        if (r.elev != null || elevAskedFor === r || !n.online()) return
        elevAskedFor = r
        val tok = if (n.hasMapbox) n.mbToken else null
        n.scope.launch { r.elev = withContext(Dispatchers.IO) { try { Elevation.along(r, tok) { n.pointAt(r, it).first } } catch (e: Exception) { null } } }
    }

    /** Waits (up to 15 s) for the heights along [r], for planning the charging stops. */
    internal suspend fun evElevNow(r: Route) {
        if (r.elev != null || !n.online()) return
        elevAskedFor = r
        val tok = if (n.hasMapbox) n.mbToken else null
        r.elev = withContext(Dispatchers.IO) { kotlinx.coroutines.withTimeoutOrNull(15_000L) { try { Elevation.along(r, tok) { n.pointAt(r, it).first } } catch (e: Exception) { null } } }
    }

    /** Clears what's been learned about your driving and your car's use. */
    fun resetLearned() {
        listOf(PreferenceKeys.EV_LEARN, PreferenceKeys.EV_STYLE, PreferenceKeys.EV_ACCEL, PreferenceKeys.EV_FREE_KMH, PreferenceKeys.EV_REGEN, PreferenceKeys.ETA_PACE, PreferenceKeys.EV_BASE_GOM, PreferenceKeys.EV_STATE).forEach { n.prefs.put(it, "") }
        n.paceReal = 0.0; n.pacePred = 0.0
        evStyleSum = 0.0; evStyleM = 0.0; evFreeSum = 0.0; evFreeM = 0.0; evAccSum = 0.0; evAccT = 0.0
    }

    /** What's been learned, for Settings: (style ×, open-motorway km/h, regen share, use per town/country/motorway). */
    fun learnedSummary(): List<Pair<String, String>> {
        val l = evLearned()
        val out = ArrayList<Pair<String, String>>()
        out += Lang.t("Tempo gegenüber Limit", "Speed against the limit") to String.format(Locale.getDefault(), "%+.0f %%", (evStyle() - 1.0) * 100)
        out += Lang.t("Tempo ohne Limit", "Speed with no limit") to "${Math.round(evFreeKmh())} km/h"
        out += Lang.t("Fahrzeit gegenüber Routenplan", "Driving time against the route's") to String.format(Locale.getDefault(), "%+.0f %%", (n.etaPace() - 1.0) * 100)
        if (isEv) {
            out += Lang.t("Verbrauch Stadt / Land / Autobahn", "Use town / country / motorway") to l.joinToString(" / ") { String.format(Locale.getDefault(), "%+.0f %%", (it - 1.0) * 100) }
        }
        return out
    }
}
