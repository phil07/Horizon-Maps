package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMathTest {
    /** Fixes along a straight line, [step] metres apart, counted the way the app does it: from an anchor that moves past 6 m. */
    private fun drive(step: Double, fixes: Int): Pair<Double, Double> {
        var anchor = 0.0; var counted = 0.0
        for (i in 1..fixes) {
            val pos = step * i
            val d = pos - anchor
            if (TrackMath.countedStep(d)) counted += d
            if (d > TrackMath.ANCHOR_M) anchor = pos
        }
        return counted to step * fixes
    }

    @Test fun slowDrivingIsNotCountedTwice() {
        // 2, 4 and 5 m per fix (7 to 18 km/h): it used to come out up to half as long again
        for (step in listOf(2.0, 4.0, 5.0, 8.0, 25.0)) {
            val (counted, real) = drive(step, 300)
            assertTrue("step $step: $counted of $real", counted <= real)
            assertTrue("step $step: $counted of $real", counted >= real - TrackMath.ANCHOR_M - step)
        }
    }

    @Test fun stepLimits() {
        assertFalse(TrackMath.countedStep(5.9))
        assertTrue(TrackMath.countedStep(6.1))
        assertTrue(TrackMath.countedStep(200.0))
        assertFalse(TrackMath.countedStep(201.0))
    }

    @Test fun gapAlongTheRoute() {
        // a tunnel: 1500 m along the route, 1200 m in a straight line
        assertEquals(1500.0, TrackMath.gapAlong(1000.0, 5.0, 2500.0, 8.0, 1200.0)!!, 1e-9)
        // either end off the route (a detour): not trusted
        assertNull(TrackMath.gapAlong(1000.0, 80.0, 2500.0, 8.0, 1200.0))
        assertNull(TrackMath.gapAlong(1000.0, 5.0, 2500.0, 60.0, 1200.0))
        // backwards along the route
        assertNull(TrackMath.gapAlong(2500.0, 5.0, 1000.0, 5.0, 1200.0))
        // far more along the route than the straight line allows (a jump to another part of it)
        assertNull(TrackMath.gapAlong(0.0, 5.0, 9000.0, 5.0, 1200.0))
    }
}
