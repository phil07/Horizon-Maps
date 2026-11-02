package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvModelTest {
    @Test fun fasterCostsMore() {
        assertEquals(1.0, EvModel.speedFactor(80.0), 1e-9)
        assertTrue(EvModel.speedFactor(130.0) > 1.4)
        assertTrue(EvModel.speedFactor(50.0) < 1.0)
    }

    @Test fun coldCostsMore() {
        assertEquals(1.0, EvModel.tempFactor(20.0), 1e-9)
        assertEquals(1.25, EvModel.tempFactor(-10.0), 1e-9)
        assertEquals(1.0, EvModel.tempFactor(null), 1e-9)
    }

    @Test fun motorwayStretch() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        // 100 km at a 130 limit, driven at the limit, 0.18 kWh/km rated: 18 kWh × 1.48 × 1.10 buffer
        val k = EvModel.kwh(0.0, 100_000.0, listOf(LimitSec(0.0, 100_000.0, 130 / 3.6)), 80.0, 1.0, 140.0, 0.18, 20.0, ones)
        assertEquals(18.0 * 1.48 * 1.10, k, 0.05)
        // no limit data: the route's average speed
        val k2 = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones)
        assertEquals(1.8 * 1.10, k2, 0.01)
    }

    @Test fun learnedParsing() {
        val l = EvModel.parseLearned("1.1,bad,5")
        assertEquals(1.1, l[0], 1e-9); assertEquals(1.0, l[1], 1e-9); assertEquals(1.0, l[2], 1e-9)
    }
}
