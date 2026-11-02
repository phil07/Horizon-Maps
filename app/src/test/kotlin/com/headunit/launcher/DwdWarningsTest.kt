package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DwdWarningsTest {
    @Test fun levels() {
        assertEquals(2, DwdWarnings.level("Moderate"))
        assertEquals(4, DwdWarnings.level("Extreme"))
        assertEquals(0, DwdWarnings.level("x"))
    }

    @Test fun names() {
        assertEquals("Glätte", DwdWarnings.eventName("GLÄTTE"))
        assertEquals("Dichter Nebel", DwdWarnings.eventName("DICHTER NEBEL"))
    }

    @Test fun currentWithinTheHour() {
        val now = 1_800_000_000_000L
        val w = DwdWarning("a", "GLÄTTE", 2, "", "Kreis Kassel", now + 30 * 60_000L, now + 5 * 3600_000L, "", null)
        assertTrue(DwdWarnings.current(w, now, 3600_000L))
        assertFalse(DwdWarnings.current(w.copy(onset = now + 3 * 3600_000L), now, 3600_000L))
        assertFalse(DwdWarnings.current(w.copy(expires = now - 1), now, 3600_000L))
        assertTrue(DwdWarnings.text(w, now).contains("Kreis Kassel"))
    }

    @Test fun germany() {
        assertTrue(DwdWarnings.inGermany(LL(50.1, 8.7)))
        assertFalse(DwdWarnings.inGermany(LL(48.85, 2.35)))
    }
}
