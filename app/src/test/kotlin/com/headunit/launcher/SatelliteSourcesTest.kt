package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SatelliteSourcesTest {
    @Test fun chainSkipsProvidersWithoutKey() {
        assertEquals(listOf(SatProvider.MAPBOX, SatProvider.EOX), SatelliteSources.chain("", " "))
        assertEquals(listOf(SatProvider.MAPBOX, SatProvider.ESRI, SatProvider.MAPTILER, SatProvider.EOX), SatelliteSources.chain("e", "m"))
        assertEquals(listOf(SatProvider.MAPBOX, SatProvider.MAPTILER, SatProvider.EOX), SatelliteSources.chain("", "m"))
    }

    @Test fun statePhotosOnlyAfterMapbox() {
        assertFalse(SatelliteSources.withStates(SatProvider.MAPBOX))
        assertTrue(SatelliteSources.withStates(SatProvider.EOX))
    }

    @Test fun refusedKeySwitchesFasterThanTooManyRequests() {
        assertEquals(0, SatelliteSources.refusals("timeout"))
        assertEquals(1, SatelliteSources.refusals("HTTP status code 429"))
        assertTrue(SatelliteSources.refusals("HTTP status code 401") * 3 >= SatelliteSources.REFUSALS_TO_SWITCH)
    }

    @Test fun allSixteenStatesWithValidAreas() {
        assertEquals(16, SatelliteSources.states.map { it.code }.toSet().size)
        for (s in SatelliteSources.states) {
            assertTrue(s.code, s.west < s.east && s.south < s.north && s.west > 5.5 && s.east < 15.2 && s.south > 47.0 && s.north < 55.2)
            assertTrue(s.code, SatelliteSources.wmsTiles(s).contains("{bbox-epsg-3857}"))
        }
    }
}
