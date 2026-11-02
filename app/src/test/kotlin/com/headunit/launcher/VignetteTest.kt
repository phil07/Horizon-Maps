package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VignetteTest {
    @Test fun countries() {
        assertEquals("AT", Vignette.countryOf(LL(48.2082, 16.3738)))   // Wien
        assertEquals("AT", Vignette.countryOf(LL(47.2692, 11.4041)))   // Innsbruck
        assertEquals("CH", Vignette.countryOf(LL(47.3769, 8.5417)))    // Zürich
        assertEquals("CZ", Vignette.countryOf(LL(50.0755, 14.4378)))   // Prag
        assertEquals("HU", Vignette.countryOf(LL(47.4979, 19.0402)))   // Budapest
        assertNull(Vignette.countryOf(LL(48.1374, 11.5755)))          // München
        assertNull(Vignette.countryOf(LL(47.6597, 9.1755), 1500.0))   // Konstanz, right at the Swiss border
        assertNull(Vignette.countryOf(LL(47.6130, 12.1890), 1500.0))  // Kiefersfelden
    }

    @Test fun roads() {
        assertTrue(Vignette.vignetteRoad("AT", "A 12", -1))
        assertTrue(Vignette.vignetteRoad("AT", "Westautobahn", -1))
        assertTrue(Vignette.vignetteRoad("AT", "", 130))
        assertFalse(Vignette.vignetteRoad("AT", "B 179", 100))
        assertTrue(Vignette.vignetteRoad("CZ", "D1", -1))
        assertTrue(Vignette.vignetteRoad("HU", "M1", -1))
        assertTrue(Vignette.vignetteRoad("RO", "DN1", 90))
    }
}
