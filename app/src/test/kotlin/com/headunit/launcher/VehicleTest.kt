package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleTest {
    @Test fun carAddsNothing() {
        val car = Vehicle.defaults("car")
        assertEquals("", Vehicle.tomtomParams(car))
        assertEquals("", Vehicle.mapboxParams(car))
        assertEquals(0.0, Vehicle.extraSecs(listOf(LimitSec(0.0, 1000.0, 130 / 3.6)), 0.0, car.maxMs), 0.0)
    }

    @Test fun camperParamsAndSlowerOnFastRoads() {
        val v = Vehicle.defaults("camper")
        assertTrue(Vehicle.tomtomParams(v).contains("vehicleMaxSpeed=100"))
        assertTrue(Vehicle.mapboxParams(v).contains("max_height=3.0"))
        // 10 km at 130 instead of 100: 360 s instead of 277 s
        val extra = Vehicle.extraSecs(listOf(LimitSec(0.0, 10_000.0, 130 / 3.6), LimitSec(10_000.0, 12_000.0, 50 / 3.6)), 0.0, v.maxMs)
        assertEquals(360.0 - 276.9, extra, 1.0)
        // only what's still ahead counts
        assertEquals(extra / 2, Vehicle.extraSecs(listOf(LimitSec(0.0, 10_000.0, 130 / 3.6)), 5_000.0, v.maxMs), 1.0)
    }
}
