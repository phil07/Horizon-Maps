package com.headunit.launcher

/**
 * A failure the app can carry on without (a missing optional value, a feed that didn't answer, a bad old setting). It is logged
 * for the problem report instead of being dropped without a trace.
 */
internal fun softFail(e: Throwable) {
    try { android.util.Log.d("HorizonNav", "ignored: ${e.javaClass.simpleName}: ${e.message}") } catch (x: Throwable) { }
}
