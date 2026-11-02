package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvModelTest {
    @Test fun fasterCostsMore() {
        assertEquals(1.0, EvModel.speedFactor(80.0), 1e-9)
        assertTrue(EvModel.speedFactor(130.0) > 1.4)
        assertTrue(EvModel.speedFactor(50.0) < 1.0)
        assertEquals(4.1, EvModel.speedFactor(250.0), 1e-9)
        assertEquals(4.1, EvModel.speedFactor(300.0), 1e-9)
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

    @Test fun hillsCostAndGiveBack() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        val flat = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones)
        val up = EvModel.Env(doubleArrayOf(0.0, 10_000.0), doubleArrayOf(100.0, 400.0), 2000.0, 0.6)
        val down = EvModel.Env(doubleArrayOf(0.0, 10_000.0), doubleArrayOf(400.0, 100.0), 2000.0, 0.6)
        val kUp = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones, up)
        val kDown = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones, down)
        // 300 m up with 2 t: 2000 × 9.81 × 300 J ≈ 1.63 kWh at the wheels, more from the battery
        assertEquals(flat + 2000 * 9.81 * 300 / 0.88 / 3.6e6 * 1.10, kUp, 0.02)
        assertTrue(kDown < flat)
        assertTrue(kDown >= 0.0)
    }

    @Test fun headwindCostsMore() {
        val against = EvModel.Wx(0.0, 40.0, 0.0, false, false)
        // driving north (0°) into a wind from the north
        assertTrue(EvModel.wxFactor(against, 120.0, 0.0) > 1.15)
        // the same wind from behind helps, but only half counts
        val behind = EvModel.wxFactor(against, 120.0, 180.0)
        assertTrue(behind < 1.0 && behind > 0.85)
        assertEquals(1.07, EvModel.wxFactor(EvModel.Wx(0.0, 0.0, 0.0, true, false), 100.0, null), 1e-9)
    }

    @Test fun coldCharging() {
        assertEquals(1.0, EvModel.chargeTempFactor(15.0, false), 1e-9)
        assertEquals(0.6, EvModel.chargeTempFactor(-10.0, false), 1e-9)
        assertEquals(0.8, EvModel.chargeTempFactor(-10.0, true), 1e-9)
    }

    @Test fun fuelBySpeed() {
        val l = EvModel.litres(0.0, 100_000.0, listOf(LimitSec(0.0, 100_000.0, 130 / 3.6)), 80.0, 1.0, 140.0, 6.0)
        assertEquals(6.0 * 1.32 * 1.10, l, 0.05)
    }
}
