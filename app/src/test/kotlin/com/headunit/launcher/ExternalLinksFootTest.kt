package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalLinksFootTest {
    @Test fun walkingAndCyclingLinksAreRecognised() {
        assertEquals("w", ExternalLinks.footMode("google.navigation:q=52.520000,13.405000&mode=w"))
        assertEquals("b", ExternalLinks.footMode("google.navigation:q=Berlin&mode=b"))
        assertEquals("w", ExternalLinks.footMode("https://www.google.com/maps/dir/?api=1&destination=52.52,13.40&travelmode=walking"))
        assertEquals("b", ExternalLinks.footMode("https://www.google.com/maps/dir/?api=1&destination=52.52,13.40&travelmode=bicycling"))
    }

    @Test fun drivingLinksStayWithTheApp() {
        assertNull(ExternalLinks.footMode("google.navigation:q=52.52,13.40"))
        assertNull(ExternalLinks.footMode("google.navigation:q=52.52,13.40&mode=d"))
        assertNull(ExternalLinks.footMode("geo:52.52,13.40"))
        assertNull(ExternalLinks.footMode("https://www.google.com/maps/dir/?api=1&destination=52.52,13.40&travelmode=driving"))
    }
}
