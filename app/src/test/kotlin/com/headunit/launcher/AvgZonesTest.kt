package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvgZonesTest {
    // a road running north: the check going north starts at the south end, the one going south at the north end
    private val south = LL(-36.9500, 174.9500)
    private val north = LL(-36.9140, 174.9500)   // about 4 km on
    private val goingNorth = AvgDef(1, "", listOf(south), listOf(north), 60 / 3.6)
    private val goingSouth = AvgDef(2, "", listOf(north), listOf(south), 60 / 3.6)
    private val defs = listOf(goingNorth, goingSouth)

    private fun at(frac: Double) = LL(south.lat + (north.lat - south.lat) * frac, south.lon)

    @Test fun startsAtItsStartHeadingAlongIt() {
        val f = FreeAvg()
        val t = f.update(at(0.0), 0.0, 16.0, 1000.0, 0L, defs)
        assertEquals(1L, t.entered?.id)
        assertNotNull(f.cur)
    }

    @Test fun theOtherDirectionsCheckDoesNotStartAtTheEnd() {
        val f = FreeAvg()
        f.update(at(0.0), 0.0, 16.0, 1000.0, 0L, defs)
        // at the north end, still heading north: check 1 ends, and check 2 (which starts here but runs south) must not begin
        val t = f.update(at(1.0), 0.0, 16.0, 5000.0, 250_000L, defs)
        assertTrue(t.left)
        assertNull(f.cur)
        val after = f.update(at(1.002), 0.0, 16.0, 5010.0, 251_000L, defs)
        assertNull(after.entered)
        assertNull(f.cur)
    }

    @Test fun warnsAheadOnceWhenHeadingForTheStart() {
        val f = FreeAvg()
        val before = at(-0.08)   // about 320 m before the start
        val t = f.update(before, 0.0, 16.0, 0.0, 0L, defs)
        assertEquals(1L, t.ahead?.id)
        assertNull(f.update(before, 0.0, 16.0, 10.0, 1000L, defs).ahead)
    }

    @Test fun drivingAwayFromItDoesNothing() {
        val f = FreeAvg()
        val t = f.update(at(-0.08), 180.0, 16.0, 0.0, 0L, defs)
        assertNull(t.ahead); assertNull(t.entered)
    }

    @Test fun givenUpWhenTheEndIsNeverPassed() {
        val f = FreeAvg()
        f.update(at(0.0), 0.0, 16.0, 0.0, 0L, defs)
        // turned off: far more driven than the check is long, nowhere near its end
        f.update(LL(-36.9300, 175.0500), 90.0, 16.0, 9000.0, 600_000L, defs)
        assertNull(f.cur)
    }

    @Test fun aFreshAnswerReplacesWhatWasKeptInItsBox() {
        val far = AvgDef(9, "", listOf(LL(50.0, 8.0)), listOf(LL(50.05, 8.0)), null)
        val box = doubleArrayOf(-37.0, 174.9, -36.9, 175.0)
        // check 2 starts inside the box and is no longer in the answer: it goes. Check 1 comes back with a new limit. The far one stays.
        val now = AvgZones.merge(listOf(goingNorth, goingSouth, far), box, listOf(AvgDef(1, "", listOf(south), listOf(north), 50 / 3.6)))
        assertEquals(listOf(9L, 1L), now.map { it.id })
        assertEquals(50 / 3.6, now.last().limit!!, 1e-9)
    }
}
