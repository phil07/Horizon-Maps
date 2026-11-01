package com.headunit.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Reads your electric car's battery level from another app's home-screen widget (the car maker's app, for example).
 *
 * Horizon Nav hosts that widget out of sight, like a home screen does, and looks for a percentage in its text whenever the
 * widget updates. The value goes into the "battery now" setting used for the arrival estimate and charging stops.
 */
object EvWidget {
    private const val HOST_ID = 4711
    private const val TAG_ID = "evwidget"
    private const val TAG_AT = "evwidgetat"
    private const val TAG_PROVIDER = "evwidgetprov"
    private const val TAG_SRC = "evsrc"

    private var host: AppWidgetHost? = null
    private var view: AppWidgetHostView? = null
    private val main = Handler(Looper.getMainLooper())

    /** The last battery level read from the widget, and when (ms), or null. */
    var lastPct: Int? = null; private set
    var lastAt: Long = 0L; private set
    /** The text the widget last showed (for checking what Horizon Nav can read from it), shown in Settings when nothing was found. */
    var lastTexts: List<String> by mutableStateOf(emptyList()); internal set

    fun widgetId(p: Prefs): Int = p.i(TAG_ID, -1)
    /** Where the last reading came from: "car" (Android Auto) or "widget". */
    fun source(p: Prefs): String = p.s(TAG_SRC, "widget")
    fun readAt(p: Prefs): Long = p.s(TAG_AT, "").toLongOrNull() ?: 0L
    fun providerLabel(ctx: Context, p: Prefs): String? {
        val id = widgetId(p); if (id < 0) return null
        val info = try { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id) } catch (e: Exception) { null } ?: return null
        return label(ctx, info)
    }

    fun label(ctx: Context, info: AppWidgetProviderInfo): String {
        val pm = ctx.packageManager
        val app = try { pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString() } catch (e: Exception) { info.provider.packageName }
        val w = try { info.loadLabel(pm) } catch (e: Exception) { null }
        return if (w.isNullOrBlank() || w == app) app else "$app – $w"
    }

    /** Every widget on the phone, sorted by app. */
    fun providers(ctx: Context): List<AppWidgetProviderInfo> = try {
        AppWidgetManager.getInstance(ctx).installedProviders.filter { it.provider.packageName != ctx.packageName }
            .sortedBy { label(ctx, it).lowercase() }
    } catch (e: Exception) { emptyList() }

    private fun host(ctx: Context): AppWidgetHost = host ?: object : AppWidgetHost(ctx.applicationContext, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView =
            ReadingView(context)
    }.also { host = it }

    /**
     * Binds [info]'s widget. Returns the new widget id when it's allowed straight away, or the id plus the system's
     * "allow Horizon Nav to create widgets" intent to show first (then call [finishBind] with the id).
     */
    fun beginBind(ctx: Context, info: AppWidgetProviderInfo): Pair<Int, android.content.Intent?> {
        val id = host(ctx).allocateAppWidgetId()
        val ok = try { AppWidgetManager.getInstance(ctx).bindAppWidgetIdIfAllowed(id, info.provider) } catch (e: Exception) { false }
        if (ok) return id to null
        return id to android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
    }

    /** The widget's own setup screen, if it has one (shown after binding, as a home screen would). */
    fun configureIntent(ctx: Context, id: Int): android.content.Intent? {
        val info = try { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id) } catch (e: Exception) { null } ?: return null
        val c: ComponentName = info.configure ?: return null
        return android.content.Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).setComponent(c)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }

    /** Saves the bound widget [id] (dropping any earlier one) and starts reading it. */
    fun finishBind(ctx: Context, p: Prefs, id: Int) {
        val old = widgetId(p)
        if (old >= 0 && old != id) try { host(ctx).deleteAppWidgetId(old) } catch (e: Exception) { }
        val info = try { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id) } catch (e: Exception) { null }
        // a different widget: the reading of the old one doesn't apply to it
        if (old != id) { p.put(PreferenceKeys.EV_NOW_PCT, ""); p.put(PreferenceKeys.EV_NOW_KM, ""); p.put(TAG_AT, "") }
        p.put(TAG_ID, id); p.put(TAG_PROVIDER, info?.provider?.flattenToString() ?: "")
        stop(); start(ctx, p)
    }

    fun cancelBind(ctx: Context, id: Int) { try { host(ctx).deleteAppWidgetId(id) } catch (e: Exception) { } }

    /** Stops reading and forgets the widget. */
    fun remove(ctx: Context, p: Prefs) {
        val id = widgetId(p)
        stop()
        if (id >= 0) try { host(ctx).deleteAppWidgetId(id) } catch (e: Exception) { }
        p.put(TAG_ID, -1); p.put(TAG_AT, ""); p.put(PreferenceKeys.EV_NOW_KM, ""); lastPct = null; lastAt = 0L
    }

    /** Starts listening to the chosen widget (if there is one). Safe to call more than once. */
    fun start(ctx: Context, p: Prefs) {
        main.post {
            try {
                val id = widgetId(p); if (id < 0 || view != null) return@post
                val app = ctx.applicationContext
                val info = AppWidgetManager.getInstance(app).getAppWidgetInfo(id) ?: return@post
                val h = host(app)
                val v = h.createView(app, id, info)
                (v as? ReadingView)?.onRead = { pct, km -> onPct(p, pct, km) }
                view = v
                h.startListening()
            } catch (e: Exception) { }
        }
    }

    /**
     * Opens the widget's app, so it refreshes its data (as it does when you open it yourself). Android doesn't let one app
     * ask another app's widget to refresh, so this is the only way to get a fresh reading on demand. Returns false if the app
     * can't be opened.
     */
    fun openProvider(ctx: Context, p: Prefs): Boolean {
        val id = widgetId(p); if (id < 0) return false
        val pkg = try { AppWidgetManager.getInstance(ctx).getAppWidgetInfo(id)?.provider?.packageName } catch (e: Exception) { null } ?: return false
        val intent = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return try { ctx.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); true } catch (e: Exception) { false }
    }

    fun stop() {
        main.post {
            try { host?.stopListening() } catch (e: Exception) { }
            view = null
        }
    }

    // ---- tapping the widget's own refresh button while Android Auto is connected ----
    // Android has no way for one app to make another app's widget refresh. But Horizon Nav is this widget's host (as a home
    // screen is), so the widget's buttons are real views here: tapping its refresh button sends the maker app the same request a
    // tap on the home screen does, even with the phone locked (as long as the button doesn't open a screen, which Android blocks
    // from the background). How often is set in Settings, every minute to every 30 (the maker's server wakes the car for each
    // request, and frequent requests can go against its terms of use: Settings says so).
    private fun tapEveryMs(p: Prefs): Long = p.i(PreferenceKeys.EV_REFRESH_MIN, 1).coerceIn(1, 30) * 60_000L
    private var tapPrefs: Prefs? = null
    /** What the last tap did, with the time, for Settings ("" before the first one). */
    var refreshNote by mutableStateOf(""); private set
    private val tapJob = object : Runnable {
        override fun run() {
            val p = tapPrefs ?: return
            // the car's own reading is live: no need to wake the car through the maker's server
            // (switched off in Settings meanwhile: no tap, but it keeps checking, so switching it back on works at once)
            if (p.b(PreferenceKeys.EV_AUTOREFRESH, true) && System.currentTimeMillis() - liveAt >= 10 * 60_000L) tapRefresh()
            main.postDelayed(this, tapEveryMs(p))
            if (view == null) try { appCtx?.let { start(it, p) } } catch (e: Exception) { }
        }
    }
    private var appCtx: Context? = null

    /** Android Auto connected: taps the widget's refresh button now and then (Settings > battery widget). Safe to call more than once. */
    fun autoRefreshStart(ctx: Context, p: Prefs) {
        main.post {
            if (!p.b(PreferenceKeys.EV_AUTOREFRESH, true) || widgetId(p) < 0) return@post
            appCtx = ctx.applicationContext; tapPrefs = p
            start(ctx, p)
            main.removeCallbacks(tapJob)
            main.postDelayed(tapJob, 30_000L)
        }
    }

    /** Android Auto disconnected. */
    fun autoRefreshStop() {
        main.post { main.removeCallbacks(tapJob); tapPrefs = null }
    }

    private val refreshWords = listOf("aktualis", "refresh", "reload", "neu laden", "update", "sync", "abrufen")

    /** Finds the widget's refresh button and taps it. True if one was found and tapped. Main thread only. */
    private fun tapRefresh(): Boolean {
        val at = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        val v = view ?: run { refreshNote = Lang.t("$at: Widget nicht geladen", "$at: widget not loaded"); return false }
        try {
            if (v.width == 0) {
                val w = (v.resources.displayMetrics.density * 320).toInt(); val h = (v.resources.displayMetrics.density * 180).toInt()
                v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                v.layout(0, 0, w, h)
            }
            val found = ArrayList<Pair<View, String>>()
            fun walk(x: View) {
                // (never the widget as a whole: tapping that usually opens the app)
                if (x !== v && x.hasOnClickListeners()) {
                    val words = buildList {
                        x.contentDescription?.toString()?.let { add(it) }
                        (x as? TextView)?.text?.toString()?.let { add(it) }
                        if (x.id != View.NO_ID) try { add(x.resources.getResourceEntryName(x.id)) } catch (e: Exception) { }
                    }.joinToString(" ")
                    found += x to words
                }
                if (x is ViewGroup) for (i in 0 until x.childCount) walk(x.getChildAt(i))
            }
            walk(v)
            val hit = found.firstOrNull { (_, w) -> val l = w.lowercase(); refreshWords.any { l.contains(it) } }?.first
            if (hit == null) {
                refreshNote = Lang.t("$at: kein Aktualisieren-Knopf gefunden", "$at: no refresh button found") +
                    (if (found.isNotEmpty()) " (" + found.joinToString(", ") { it.second.ifBlank { "?" }.take(30) } + ")" else "")
                return false
            }
            hit.performClick()
            refreshNote = Lang.t("$at: Aktualisieren angetippt", "$at: refresh tapped")
            return true
        } catch (e: Exception) {
            refreshNote = Lang.t("$at: Antippen fehlgeschlagen", "$at: tap failed")
            return false
        }
    }

    /** Live battery level from the car (Android Auto). Takes precedence over the widget for a while. */
    fun liveReading(p: Prefs, pct: Int, km: Int?) {
        liveAt = System.currentTimeMillis()
        p.put(PreferenceKeys.EV_NOW_PCT, pct.toString())
        p.put(PreferenceKeys.EV_NOW_KM, km?.toString() ?: "")
        p.put(TAG_AT, liveAt.toString()); p.put(TAG_SRC, "car")
        lastPct = pct; lastAt = liveAt
    }

    private var liveAt = 0L

    private fun onPct(p: Prefs, pct: Int?, km: Int?) {
        // the car's own live value wins for 10 minutes after it last arrived
        if (System.currentTimeMillis() - liveAt < 10 * 60_000L) return
        if (pct == null) return
        val now = System.currentTimeMillis()
        lastPct = pct; lastAt = now
        p.put(PreferenceKeys.EV_NOW_PCT, pct.toString())
        p.put(PreferenceKeys.EV_NOW_KM, km?.toString() ?: "")  // no range in this reading: don't keep an older one
        p.put(TAG_AT, now.toString()); p.put(TAG_SRC, "widget")
    }

    /** The range in km the widget shows, e.g. "212 km" (its figures can be split into separate pieces of text, so they're joined first). */
    internal fun findKm(texts: List<String>): Int? {
        val re = Regex("(?<![\\d.,])(\\d{1,4})\\s*km\\b", RegexOption.IGNORE_CASE)
        for (m in re.findAll(texts.joinToString(" "))) { val v = m.groupValues[1].toIntOrNull() ?: continue; if (v in 0..1500) return v }
        return null
    }

    /** The first battery-looking percentage in the widget's text: "78 %", "78%", "Akku: 78 %", "SoC 78%". */
    internal fun findPct(texts: List<String>): Int? {
        val re = Regex("(?<![\\d.,])(\\d{1,3})(?:[.,]\\d)?\\s?%")
        // text that mentions the battery first, then any percentage
        // "Ladeziel 80 %" is a target, not the level: skip texts that say target or limit
        val keyed = texts.filter { t -> val l = t.lowercase(); listOf("akku", "batter", "soc", "ladestand").any { l.contains(it) } && listOf("ziel", "limit", "target").none { l.contains(it) } }
        for (t in keyed + texts) {
            for (m in re.findAll(t)) { val v = m.groupValues[1].toIntOrNull() ?: continue; if (v in 0..100) return v }
        }
        return null
    }

    /** The out-of-sight widget: lays itself out at a home-screen size after every update and reads its text. */
    private class ReadingView(ctx: Context) : AppWidgetHostView(ctx) {
        var onRead: ((Int?, Int?) -> Unit)? = null
        override fun updateAppWidget(remoteViews: RemoteViews?) {
            super.updateAppWidget(remoteViews)
            main.post {
                try {
                    val w = (resources.displayMetrics.density * 320).toInt(); val h = (resources.displayMetrics.density * 180).toInt()
                    measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY))
                    layout(0, 0, w, h)
                    val texts = ArrayList<String>()
                    collect(this, texts)
                    lastTexts = texts.map { it.take(80) }.take(30)
                    val pct = findPct(texts); val km = findKm(texts)
                    if (pct != null) onRead?.invoke(pct, km)
                } catch (e: Exception) { }
            }
        }

        private fun collect(v: View, out: MutableList<String>) {
            if (v is TextView) v.text?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
            v.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out += it }
            if (v is ViewGroup) for (i in 0 until v.childCount) collect(v.getChildAt(i), out)
        }
    }
}
