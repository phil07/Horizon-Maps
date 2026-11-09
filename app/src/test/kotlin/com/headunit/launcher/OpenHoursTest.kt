package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class OpenHoursTest {
    private fun at(day: Int, h: Int, m: Int = 0) = Calendar.getInstance().apply { clear(); set(2026, Calendar.NOVEMBER, day, h, m, 0) }.timeInMillis

    @Test fun noTimesGiveNoNote() = assertNull(OpenHours.note(emptyList(), at(9, 12)))

    @Test fun openNowSaysUntilWhen() {
        val n = OpenHours.note(listOf(at(9, 8) to at(9, 20)), at(9, 12))
        assertTrue(n!!.endsWith("20:00")); assertTrue(OpenHours.isOpen(n))
    }

    @Test fun closedSaysWhenItOpensToday() {
        val n = OpenHours.note(listOf(at(9, 8) to at(9, 20)), at(9, 6))
        assertTrue(n!!.endsWith("08:00")); assertFalse(OpenHours.isOpen(n))
    }

    @Test fun closedForTheDayNamesTheNextDay() {
        val n = OpenHours.note(listOf(at(9, 8) to at(9, 20), at(10, 7, 30) to at(10, 20)), at(9, 21))!!
        assertTrue(n.endsWith("07:30")); assertFalse(OpenHours.isOpen(n))
        assertTrue(n.contains("Tue") || n.contains("Di"))
    }

    @Test fun roundTheClockIsOneStretch() {
        val days = (9..15).map { at(it, 0) to at(it + 1, 0) }
        assertEquals(true, OpenHours.isOpen(OpenHours.note(days, at(9, 12))))
        assertFalse(OpenHours.note(days, at(9, 12))!!.contains(":"))
    }
}
