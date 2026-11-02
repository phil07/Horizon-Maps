package com.headunit.launcher

/**
 * Hidden log export. Settings > About: tapping the version seven times quickly shows a "Logs" row that copies the app's
 * recent messages. The unlock lives only in memory, so it is gone after an app restart, and the messages come from this
 * process's own logcat buffer (apps may read their own logs without extra permission).
 */
object DebugLog {
    @Volatile var unlocked = false

    /** The last [maxLines] log lines of this app process, or a short note when they cannot be read. */
    fun dump(maxLines: Int = 3000): String = try {
        val p = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-t", maxLines.toString(), "-v", "time", "--pid=${android.os.Process.myPid()}"))
        val out = p.inputStream.bufferedReader().use { it.readText() }
        p.waitFor()
        out.ifBlank { "(keine Meldungen)" }
    } catch (e: Exception) {
        "Logs nicht lesbar: ${e.message}"
    }
}
