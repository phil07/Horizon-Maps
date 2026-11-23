package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MyRoutesTest {
    private val home = LL(49.68, 8.62)
    private val work = LL(49.75, 8.70)
    private val way = (0..40).map { LL(49.68 + it * 0.00175, 8.62 + it * 0.002) }
    private fun r(id: String, dm: Int, mode: String = "offer", from: LL = home, to: LL = work) = MyRoutes.MyRoute(id, id, from, to, "Work", way, dm, mode)

    @Test fun theOneDrivenNearestThisTimeComesFirst() {
        val list = listOf(r("morning", 7 * 60 + 30), r("evening", 17 * 60 + 15))
        assertEquals("morning", MyRoutes.match(list, home, work, 8 * 60)?.id)
        assertEquals("evening", MyRoutes.match(list, home, work, 18 * 60)?.id)
    }

    @Test fun anotherTripOrOneSwitchedOffIsNotOffered() {
        assertNull(MyRoutes.match(listOf(r("a", 480)), LL(50.5, 9.5), work, 480))
        assertNull(MyRoutes.match(listOf(r("a", 480, mode = "off")), home, work, 480))
    }

    @Test fun minutesRoundTheClock() {
        assertEquals(20, MyRoutes.minuteGap(23 * 60 + 50, 10))
        assertEquals(60, MyRoutes.minuteGap(8 * 60, 9 * 60))
    }

    @Test fun viaPointsSpreadAlongTheWay() {
        val v = MyRoutes.viaPoints(way, 4)
        assertEquals(4, v.size)
        assertTrue(v.none { it == way.first() || it == way.last() })
    }

    @Test fun thinKeepsTheEnds() {
        val t = MyRoutes.thin(way, 500.0)
        assertEquals(way.first(), t.first())
        assertEquals(way.last(), t.last())
        assertTrue(t.size < way.size)
    }
}
