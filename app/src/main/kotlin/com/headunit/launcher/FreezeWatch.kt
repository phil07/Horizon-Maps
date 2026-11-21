package com.headunit.launcher

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Catches freezes ("Horizon Nav isn't responding: close or wait"), which never reach the crash handler. A watchdog thread checks
 * the screen thread answers within 5 seconds; when it doesn't, what it was stuck doing is written to lastfreeze.txt. On Android 11
 * and newer, Android's own record of the last freeze is picked up too, the next time the app starts. Feedback reports include it.
 */
object FreezeWatch {
    @Volatile private var started = false
    /** When the screen thread stopped answering (0 when it's fine): SessionWatch notes it every 10 seconds. */
    @Volatile var stuckSince = 0L

    fun file(ctx: Context) = File(ctx.filesDir, "lastfreeze.txt")
    fun exitFile(ctx: Context) = File(ctx.filesDir, "lastexit.txt")

    @Synchronized
    fun start(ctx: Context) {
        if (started) return
        started = true
        val app = ctx.applicationContext
        val main = Handler(Looper.getMainLooper())
        Thread({
            var reported = false
            while (true) {
                val answered = AtomicBoolean(false)
                main.post { answered.set(true) }
                val t0 = System.currentTimeMillis()
                try { Thread.sleep(5000) } catch (e: InterruptedException) { return@Thread }
                // the phone went to sleep (or the app was paused in the background) in between: the screen thread can't answer then,
                // and that is not a freeze, so nothing is recorded
                if (System.currentTimeMillis() - t0 > 15_000L) { reported = false; stuckSince = 0L; continue }
                if (!answered.get()) {
                    if (stuckSince == 0L) stuckSince = System.currentTimeMillis() - 5000L
                    if (!reported) {
                        reported = true
                        try {
                            val st = Looper.getMainLooper().thread.stackTrace.take(40).joinToString("\n") { "  at $it" }
                            file(app).writeText("Freeze: the screen thread was stuck for 5+ s (${java.util.Date()})\n$st")
                        } catch (e: Throwable) { softFail(e) }
                    }
                } else { reported = false; stuckSince = 0L }
            }
        }, "freeze-watch").apply { isDaemon = true; priority = Thread.MIN_PRIORITY }.start()
    }

    /** The way the app last ended, from Android's record (Android 11+), or null. */
    private fun lastExit(ctx: Context): android.app.ApplicationExitInfo? {
        if (Build.VERSION.SDK_INT < 30) return null
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            am.getHistoricalProcessExitReasons(ctx.packageName, 0, 1).firstOrNull()
        } catch (e: Throwable) { softFail(e); null }
    }

    /** Whether the last run ended in a crash, a native crash in the map's code, or a freeze. Swiping the app away, or Android
     *  closing it for low memory, is not that, so it doesn't count towards turning the drawing down. Unknown counts as bad. */
    fun lastEndedBadly(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 30) return true
        val r = lastExit(ctx)?.reason ?: return false
        return r == android.app.ApplicationExitInfo.REASON_CRASH || r == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE || r == android.app.ApplicationExitInfo.REASON_ANR
    }

    /** The words for how the app ended, for the problem report. */
    fun exitWords(reason: Int): String = when (reason) {
        android.app.ApplicationExitInfo.REASON_USER_REQUESTED -> "swiped away or closed by you"
        android.app.ApplicationExitInfo.REASON_USER_STOPPED -> "stopped by you"
        android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "closed by Android for low memory"
        android.app.ApplicationExitInfo.REASON_CRASH -> "crashed"
        android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "crashed in the map's code (native)"
        android.app.ApplicationExitInfo.REASON_ANR -> "not responding"
        android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "closed by Android for using too many resources"
        android.app.ApplicationExitInfo.REASON_OTHER -> "ended (Android gave no reason)"
        else -> "ended (reason $reason)"
    }

    /** Android 11+: keeps Android's record of how the app last ended for the problem report, and the screen thread's part of a freeze. */
    fun collectLastExit(ctx: Context) {
        try {
            val last = lastExit(ctx) ?: return
            val prefs = ctx.getSharedPreferences(CrashGuardPrefs.FILE, Context.MODE_PRIVATE)
            if (prefs.getLong(CrashGuardPrefs.LAST_EXIT_SEEN, 0L) < last.timestamp) {
                prefs.edit().putLong(CrashGuardPrefs.LAST_EXIT_SEEN, last.timestamp).apply()
                exitFile(ctx).writeText("Ended: ${exitWords(last.reason)} (${java.util.Date(last.timestamp)})${last.description?.let { " · $it" } ?: ""}")
            }
            if (last.reason == android.app.ApplicationExitInfo.REASON_ANR && prefs.getLong(CrashGuardPrefs.LAST_ANR_SEEN, 0L) < last.timestamp) {
                prefs.edit().putLong(CrashGuardPrefs.LAST_ANR_SEEN, last.timestamp).apply()
                val text = last.traceInputStream?.bufferedReader()?.use { it.readText() } ?: ""
                // just the screen thread ("main") from the full dump of every thread
                val i = text.indexOf("\"main\"")
                val mainPart = if (i >= 0) text.substring(i).substringBefore("\n\n") else text
                file(ctx).writeText("Not responding (Android's record, ${java.util.Date(last.timestamp)}): ${last.description ?: ""}\n" + mainPart.take(3500))
            }
        } catch (e: Throwable) { softFail(e) }
    }
}
