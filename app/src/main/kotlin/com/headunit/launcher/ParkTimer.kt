package com.headunit.launcher

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Parkuhr: chosen on the arrival card (30 min, 1, 2 or 3 h). A quiet notification counts down while the time runs, with
 * "Aufheben" to stop it; 10 minutes before the end a reminder says how long the walk back to the car is.
 */
object ParkTimer {
    private const val CH_RUN = "parktimer_run"
    private const val CH_ALERT = "parktimer"
    private const val ID_RUN = 7401
    private const val ID_ALERT = 7402
    const val ACTION_REMIND = "com.headunit.launcher.PARK_REMIND"
    const val ACTION_CANCEL = "com.headunit.launcher.PARK_CANCEL"
    /** Minutes before the end that the reminder comes. */
    const val BEFORE_MIN = 10

    /** When the parking time ends (ms), or 0 when none is running. */
    fun until(p: Prefs): Long {
        val t = p.s(PreferenceKeys.PARK_UNTIL, "").split(',').firstOrNull()?.toLongOrNull() ?: 0L
        return if (t > System.currentTimeMillis()) t else 0L
    }

    private fun carPos(p: Prefs): LL? {
        val a = p.s(PreferenceKeys.PARK_UNTIL, "").split(',')
        val la = a.getOrNull(1)?.toDoubleOrNull(); val lo = a.getOrNull(2)?.toDoubleOrNull()
        return if (la != null && lo != null) LL(la, lo) else null
    }

    fun start(ctx: Context, p: Prefs, minutes: Int, car: LL?) {
        val end = System.currentTimeMillis() + minutes * 60_000L
        p.put(PreferenceKeys.PARK_UNTIL, if (car != null) String.format(java.util.Locale.US, "%d,%.6f,%.6f", end, car.lat, car.lon) else end.toString())
        channels(ctx)
        showRunning(ctx, end)
        // the reminder 10 minutes before the end (a short time: half way)
        val at = end - minOf(BEFORE_MIN * 60_000L, minutes * 30_000L)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val pi = pending(ctx, ACTION_REMIND)
        try {
            if (android.os.Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            // without the exact alarm permission Android may hold it back a few minutes: asked a little earlier
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at - 3 * 60_000L, pi)
        } catch (e: Exception) { try { am.set(AlarmManager.RTC_WAKEUP, at, pi) } catch (e2: Exception) { } }
    }

    fun cancel(ctx: Context, p: Prefs) {
        p.put(PreferenceKeys.PARK_UNTIL, "")
        try { ctx.getSystemService(AlarmManager::class.java).cancel(pending(ctx, ACTION_REMIND)) } catch (e: Exception) { }
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.cancel(ID_RUN); nm.cancel(ID_ALERT)
    }

    private fun pending(ctx: Context, action: String): PendingIntent =
        PendingIntent.getBroadcast(ctx, action.hashCode(), Intent(ctx, ParkTimerReceiver::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** Set on the app's start intent: open the walk to the parked car. */
    const val EXTRA_WALK_TO_CAR = "walkToCar"

    private fun walkToCar(ctx: Context): PendingIntent? = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
        it.putExtra(EXTRA_WALK_TO_CAR, true).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        PendingIntent.getActivity(ctx, 7404, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun openApp(ctx: Context): PendingIntent? = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
        PendingIntent.getActivity(ctx, 7403, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH_RUN) == null) nm.createNotificationChannel(NotificationChannel(CH_RUN, Lang.t("Parkzeit läuft", "Parking time running"), NotificationManager.IMPORTANCE_LOW))
        if (nm.getNotificationChannel(CH_ALERT) == null) nm.createNotificationChannel(NotificationChannel(CH_ALERT, Lang.t("Parkzeit-Erinnerung", "Parking reminder"), NotificationManager.IMPORTANCE_HIGH))
    }

    private fun clock(ms: Long): String = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ms))

    private fun showRunning(ctx: Context, end: Long) {
        val b = Notification.Builder(ctx, CH_RUN).setSmallIcon(R.drawable.ic_stat_nav)
            .setContentTitle(Lang.t("Parkzeit bis ${clock(end)}", "Parked until ${clock(end)}"))
            .setContentText(Lang.t("Erinnerung $BEFORE_MIN Minuten vorher", "Reminder $BEFORE_MIN minutes before"))
            .setOngoing(true).setShowWhen(true).setWhen(end).setUsesChronometer(true)
            .addAction(Notification.Action.Builder(null, Lang.t("Aufheben", "Cancel"), pending(ctx, ACTION_CANCEL)).build())
        if (android.os.Build.VERSION.SDK_INT >= 24) b.setChronometerCountDown(true)
        openApp(ctx)?.let { b.setContentIntent(it) }
        try { ctx.getSystemService(NotificationManager::class.java).notify(ID_RUN, b.build()) } catch (e: Exception) { }
    }

    /** The reminder: how long is left and how long the walk back to the car takes (from where the phone last knew it was). */
    fun remind(ctx: Context, p: Prefs) {
        val end = p.s(PreferenceKeys.PARK_UNTIL, "").split(',').firstOrNull()?.toLongOrNull() ?: return
        channels(ctx)
        val left = maxOf(0L, (end - System.currentTimeMillis()) / 60_000L)
        val car = carPos(p)
        val here = lastKnown(ctx)
        // walking: about 80 m a minute, with a quarter more for the streets not being a straight line
        val walk = if (car != null && here != null) Math.max(1L, Math.round(Geo.dist(here, car) * 1.25 / 80.0)) else -1L
        val text = Lang.t("Noch $left Min. bis ${clock(end)}", "$left min left, until ${clock(end)}") +
            (if (walk >= 0) Lang.t(" · etwa $walk Min. zu Fuß zum Auto", " · about $walk min walk to the car") else "")
        val b = Notification.Builder(ctx, CH_ALERT).setSmallIcon(R.drawable.ic_stat_nav)
            .setContentTitle(Lang.t("Parkzeit läuft bald ab", "Parking time ends soon"))
            .setContentText(text).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
        openApp(ctx)?.let { b.setContentIntent(it) }
        // "Zum Auto": opens the app with the way on foot to the car
        if (car != null) walkToCar(ctx)?.let { b.addAction(Notification.Action.Builder(null, Lang.t("Zum Auto", "To the car"), it).build()) }
        try { ctx.getSystemService(NotificationManager::class.java).notify(ID_ALERT, b.build()) } catch (e: Exception) { }
    }

    private fun lastKnown(ctx: Context): LL? = try {
        val lm = ctx.getSystemService(android.location.LocationManager::class.java)
        listOf(android.location.LocationManager.GPS_PROVIDER, android.location.LocationManager.NETWORK_PROVIDER, "fused")
            .mapNotNull { pr -> try { lm.getLastKnownLocation(pr) } catch (e: SecurityException) { null } catch (e: Exception) { null } }
            .maxByOrNull { it.time }?.let { LL(it.latitude, it.longitude) }
    } catch (e: Exception) { null }
}

class ParkTimerReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val p = Prefs(ctx.applicationContext)
        when (intent.action) {
            ParkTimer.ACTION_REMIND -> ParkTimer.remind(ctx, p)
            ParkTimer.ACTION_CANCEL -> ParkTimer.cancel(ctx, p)
        }
    }
}
