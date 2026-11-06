package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ElevationRefineTest {
    @Test fun flatRouteGetsNoExtraPoints() {
        val o = doubleArrayOf(0.0, 1000.0, 2000.0)
        assertEquals(0, Elevation.refineOffsets(o, doubleArrayOf(100.0, 110.0, 105.0), 100).size)
    }

    @Test fun steepGapIsFilledEvery250m() {
        val o = doubleArrayOf(0.0, 1000.0, 2000.0)
        val e = Elevation.refineOffsets(o, doubleArrayOf(100.0, 300.0, 305.0), 100)
        assertEquals(listOf(250.0, 500.0, 750.0), e.toList())
    }

    @Test fun capKeepsSteepestFirst() {
        val o = doubleArrayOf(0.0, 1000.0, 2000.0)
        val e = Elevation.refineOffsets(o, doubleArrayOf(100.0, 150.0, 400.0), 3)
        assertTrue(e.all { it > 1000.0 })
    }

    @Test fun mergeSortsAlongRoute() {
        val (o, h) = Elevation.merge(doubleArrayOf(0.0, 1000.0), doubleArrayOf(1.0, 3.0), doubleArrayOf(500.0), doubleArrayOf(2.0))
        assertEquals(listOf(0.0, 500.0, 1000.0), o.toList())
        assertEquals(listOf(1.0, 2.0, 3.0), h.toList())
    }
}
