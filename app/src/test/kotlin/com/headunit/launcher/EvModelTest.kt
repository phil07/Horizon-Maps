package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        // a tenth more at −5 °C and below (the heating is counted separately, by the time)
        assertEquals(1.10, EvModel.tempFactor(-10.0), 1e-9)
        assertEquals(1.05, EvModel.tempFactor(5.0), 1e-9)
        assertEquals(1.0, EvModel.tempFactor(null), 1e-9)
    }

    @Test fun motorwayStretch() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        // 100 km at a 130 limit, driven at the limit, 0.18 kWh/km rated: 18 kWh × 1.48 (no hidden buffer: EvModel.BUFFER)
        val k = EvModel.kwh(0.0, 100_000.0, listOf(LimitSec(0.0, 100_000.0, 130 / 3.6)), 80.0, 1.0, 140.0, 0.18, 20.0, ones)
        assertEquals(18.0 * 1.48 * EvModel.BUFFER, k, 0.05)
        // no limit data: the route's average speed
        val k2 = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones)
        assertEquals(1.8 * EvModel.BUFFER, k2, 0.01)
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
        assertEquals(flat + 2000 * 9.81 * 300 / 0.88 / 3.6e6 * EvModel.BUFFER, kUp, 0.02)
        assertTrue(kDown < flat)
        assertTrue(kDown >= 0.0)
    }

    @Test fun headwindCostsMore() {
        val against = EvModel.Wx(0.0, 40.0, 0.0, false, false)
        // driving north (0°) into a wind from the north
        assertTrue(EvModel.wxFactor(against, 120.0, 0.0) > 1.15)
        // the same wind from behind helps, but only half counts
        val behind = EvModel.wxFactor(against, 120.0, 180.0)
        // (held to at most 15 % less: wxFactor's lower limit is 0.85 itself)
        assertTrue(behind < 1.0 && behind >= 0.85)
        assertEquals(1.07, EvModel.wxFactor(EvModel.Wx(0.0, 0.0, 0.0, true, false), 100.0, null), 1e-9)
    }

    @Test fun coldCharging() {
        assertEquals(1.0, EvModel.chargeTempFactor(15.0, false), 1e-9)
        assertEquals(0.6, EvModel.chargeTempFactor(-10.0, false), 1e-9)
        assertEquals(0.8, EvModel.chargeTempFactor(-10.0, true), 1e-9)
    }

    @Test fun coldStretchCountsWhereItIs() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        // the same 20 km, mild at the start and freezing at the end: the second half costs more than the first
        val env = EvModel.Env(wx = listOf(EvModel.Wx(0.0, 0.0, 0.0, false, false, 20.0), EvModel.Wx(20_000.0, 0.0, 0.0, false, false, -5.0)))
        val first = EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones, env)
        val second = EvModel.kwh(10_000.0, 20_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones, env)
        assertTrue(second > first * 1.2)
        // no temperature on the stretch: the trip's own figure
        val noTemp = EvModel.Env(wx = listOf(EvModel.Wx(0.0, 0.0, 0.0, false, false)))
        assertEquals(EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones),
            EvModel.kwh(0.0, 10_000.0, emptyList(), 80.0, 1.0, 140.0, 0.18, 20.0, ones, noTemp), 1e-9)
    }

    @Test fun learningMovesHalfwayByShare() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        // all on the motorway, the battery lost 20 % more than the model: half of it is learned, on the motorway only
        val l = EvModel.learnStep(ones, doubleArrayOf(0.0, 0.0, 10.0), 12.0, 0.0, 0.0, 0.0)!!
        assertEquals(1.0, l[0], 1e-9); assertEquals(1.0, l[1], 1e-9); assertEquals(1.1, l[2], 1e-9)
        // half town, half country, 10 % less: each gets its share
        val m = EvModel.learnStep(ones, doubleArrayOf(5.0, 5.0, 0.0), 9.0, 0.0, 0.0, 0.0)!!
        assertEquals(0.975, m[0], 1e-9); assertEquals(0.975, m[1], 1e-9); assertEquals(1.0, m[2], 1e-9)
        // heating and climbing are taken off before comparing
        val n = EvModel.learnStep(ones, doubleArrayOf(0.0, 10.0, 0.0), 13.0, 2.0, 1.0, 0.0)!!
        assertEquals(1.0, n[1], 1e-9)
    }

    @Test fun learningTakesOffTheRecoveryDownhill() {
        val ones = doubleArrayOf(1.0, 1.0, 1.0)
        // the model was right on the road, and the usual 60 % came back from 2 kWh downhill: nothing to correct
        val l = EvModel.learnStep(ones, doubleArrayOf(0.0, 10.0, 0.0), 10.0 - 0.6 * 2.0, 0.0, 0.0, 2.0)!!
        assertEquals(1.0, l[1], 1e-9)
        // mostly downhill: not learned from
        assertNull(EvModel.learnStep(ones, doubleArrayOf(0.0, 2.0, 0.0), 0.5, 0.0, 0.0, 5.0))
        // too little modelled use: not learned from
        assertNull(EvModel.learnStep(ones, doubleArrayOf(0.1, 0.1, 0.0), 0.3, 0.0, 0.0, 0.0))
        // a wild reading is held to the limits
        val w = EvModel.learnStep(ones, doubleArrayOf(0.0, 0.0, 10.0), 100.0, 0.0, 0.0, 0.0)!!
        assertEquals(1.4, w[2], 1e-9)
    }

    @Test fun fuelBySpeed() {
        val l = EvModel.litres(0.0, 100_000.0, listOf(LimitSec(0.0, 100_000.0, 130 / 3.6)), 80.0, 1.0, 140.0, 6.0)
        assertEquals(6.0 * 1.32 * 1.10, l, 0.05)
    }
}
