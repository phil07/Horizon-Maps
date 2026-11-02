package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedReportsTest {
    private val p = LL(50.0, 8.0)
    private val now = 1_800_000_000_000L
    private fun north(m: Double) = LL(50.0 + m / 111_320.0, 8.0)

    @Test fun moreDevicesCountMore() {
        val one = SpeedReports.votes(listOf(SpeedReport(north(-50.0), 0.0, 50, now)), p, 0.0, now)
        assertEquals(1, one.size); assertEquals(1.0, one[0].weight, 0.01)
        val three = SpeedReports.votes(List(3) { SpeedReport(north(-20.0 * it), 0.0, 50, now) }, p, 0.0, now)
        assertEquals(SpeedVote.W_SIGN, three[0].weight, 0.01)
        val many = SpeedReports.votes(List(9) { SpeedReport(north(-10.0 * it), 0.0, 50, now) }, p, 0.0, now)
        assertEquals(4.0, many[0].weight, 0.01)
    }

    @Test fun otherDirectionAndFarAwayDontCount() {
        val v = SpeedReports.votes(listOf(SpeedReport(north(-50.0), 180.0, 70, now), SpeedReport(north(-500.0), 0.0, 70, now)), p, 0.0, now)
        assertTrue(v.isEmpty())
    }

    @Test fun olderAndSupersededCountLess() {
        val year = 365L * 24 * 3600 * 1000
        val v = SpeedReports.votes(listOf(SpeedReport(north(-30.0), 0.0, 70, now - year / 2), SpeedReport(north(-40.0), 0.0, 50, now)), p, 0.0, now)
        val w70 = v.first { SpeedVote.key(it.ms) == 70 }.weight
        val w50 = v.first { SpeedVote.key(it.ms) == 50 }.weight
        assertTrue(w70 < 0.5 && w50 > 0.99)
    }

    @Test fun roadworksWindowsAndLine() {
        val w = AutobahnRest.windows(listOf("10.11.26 von 09:30 bis 16:00 Uhr", "11.11.26 von 22:00 bis 05:00 Uhr"))!!
        assertEquals(2, w.size)
        assertEquals(6.5 * 3600_000, (w[0].last - w[0].first).toDouble(), 1.0)
        assertEquals(7.0 * 3600_000, (w[1].last - w[1].first).toDouble(), 1.0)
        assertNotNull(AutobahnRest.windows(listOf("Beginn: 09.11.26 um 00:00 Uhr", "Ende: 05.12.26 um 00:00 Uhr")))
        assertNull(AutobahnRest.windows(listOf("Länge: 5.5 km | Max. 80 km/h")))
        // a stretch heading north past you, always on
        val rw = AutobahnRest.RoadworkLimit(listOf(north(-300.0), north(300.0)), 80 / 3.6, null, 0L)
        assertEquals(80, SpeedVote.key(AutobahnRest.limitAt(listOf(rw), p, 0.0, now)!!))
        assertNull(AutobahnRest.limitAt(listOf(rw), p, 180.0, now))
    }
}
