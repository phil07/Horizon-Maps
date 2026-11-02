package com.headunit.launcher

/**
 * Live values from the car itself (Android Auto's car hardware API), shared with the engine. The speed is the car's own wheel speed:
 * steadier than GPS, and it keeps coming in a tunnel.
 */
object CarData {
    @Volatile var speedMs: Double? = null
        private set
    @Volatile var speedAt = 0L
        private set
    /** Told about each new speed (on the main thread). */
    @Volatile var onSpeed: ((Double) -> Unit)? = null

    fun speed(ms: Double) {
        if (ms.isNaN() || ms < 0.0 || ms > 120.0) return
        speedMs = ms; speedAt = android.os.SystemClock.elapsedRealtime()
        onSpeed?.invoke(ms)
    }

    /** The car's speed when it came in the last [maxAgeMs], else null. */
    fun fresh(maxAgeMs: Long = 2500L): Double? =
        speedMs?.takeIf { android.os.SystemClock.elapsedRealtime() - speedAt <= maxAgeMs }
}
