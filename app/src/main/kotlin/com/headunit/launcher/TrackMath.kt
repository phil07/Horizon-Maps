package com.headunit.launcher

/**
 * The distance driven, from GPS fixes. Pure functions, so they can be tested without a phone.
 */
internal object TrackMath {
    /** The last position counted from (the anchor) only moves on once the car is this far from it (the GPS wobbles by a few metres). */
    const val ANCHOR_M = 6.0
    /** Steps longer than this are a gap in the GPS (a tunnel, a lost signal), not a step. */
    const val MAX_STEP_M = 200.0

    /**
     * Whether a step of [d] metres from the anchor is counted. Only when the anchor moves on with it (past [ANCHOR_M]): a step that
     * leaves the anchor in place is measured again from it with the next fix, so counting it as well counted slow driving up to half
     * as much again.
     */
    fun countedStep(d: Double): Boolean = d > ANCHOR_M && d <= MAX_STEP_M

    /**
     * A gap in the GPS on a route: the distance along the route between the fix before ([prevProg] metres along it, [prevDist] off
     * it) and the one after, or null when it can't be trusted: either end off the route (a detour), backwards, or far from what the
     * straight line between the two ([straight]) allows.
     */
    fun gapAlong(prevProg: Double, prevDist: Double, curProg: Double, curDist: Double, straight: Double): Double? {
        if (prevDist > 50.0 || curDist > 50.0) return null
        val along = curProg - prevProg
        if (along < 0.0 || along < straight * 0.5 - 200.0 || along > straight * 1.5 + 200.0) return null
        return along
    }
}
