package com.headunit.launcher

/**
 * A failure the app carries on without (a missing optional value, a feed that didn't answer, a bad old setting). Each kind is
 * counted, with its latest words, and the list goes into the problem report, so a repeated failure shows up there instead of
 * vanishing. Only the kinds are kept (a fixed number), so a failure that happens on every fix can't fill the memory.
 */
internal object SoftFails {
    private const val MAX_KINDS = 40
    private class Seen(var count: Int, var last: String)
    private val seen = LinkedHashMap<String, Seen>()

    @Synchronized
    fun add(e: Throwable) {
        val where = e.stackTrace.firstOrNull()?.let { "${it.fileName}:${it.lineNumber}" } ?: "?"
        val kind = "${e.javaClass.simpleName} at $where"
        val words = (e.message ?: "").take(160)
        val hit = seen[kind]
        if (hit != null) { hit.count++; hit.last = words; return }
        if (seen.size >= MAX_KINDS) seen.remove(seen.keys.first())
        seen[kind] = Seen(1, words)
    }

    /** The kinds of failure seen so far, most frequent first, for the problem report. */
    @Synchronized
    fun report(): String = seen.entries.sortedByDescending { it.value.count }.joinToString("\n") { (k, v) ->
        "  ${v.count}× $k: ${v.last}"
    }
}

/** Records a failure the app carries on without (see [SoftFails]); logged too, for Logcat. */
internal fun softFail(e: Throwable) {
    try { SoftFails.add(e) } catch (x: Throwable) { }
    try { android.util.Log.d("HorizonNav", "ignored: ${e.javaClass.simpleName}: ${e.message}") } catch (x: Throwable) { }
}
