package com.headunit.launcher

/**
 * One online speed-limit service, kept from asking too often. It is asked again only when the road (the OpenStreetMap way) has
 * changed, when its answer is older than [staleMs], or when you have moved more than [staleM] metres since it was asked, and then
 * at most once per [gapMs]. The answer is kept, so the same stretch isn't asked again at once. The check and the update happen
 * under one lock, so two lookups can't both decide to ask.
 */
internal class SpeedSourceMemo<T>(private val gapMs: Long, private val staleMs: Long, private val staleM: Double) {
    private var at = 0L
    private var pos: LL? = null
    private var value: T? = null

    /** The road (the way) the last answer was asked for. */
    @Volatile var way = -1L
        private set

    /** The answer for [p] on [way]: asked now when [ask] is given and one is due, else the one kept. */
    @Synchronized
    fun answer(p: LL, way: Long, now: Long, ask: (() -> T?)?): T? {
        val stale = now - at > staleMs || (pos?.let { Geo.dist(it, p) > staleM } ?: true)
        if (ask != null && (this.way != way || stale) && now - at > gapMs) {
            at = now
            pos = p
            this.way = way
            value = try { ask() } catch (e: Exception) { null }
        }
        return value
    }
}
