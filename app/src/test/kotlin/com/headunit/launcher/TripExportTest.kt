package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripExportTest {
    @Test fun csvHasHeaderGermanNumbersAndKind() {
        val t1 = Trip(1_700_000_000_000L, 1_700_000_000_000L + 45 * 60_000L, 52_340.0, 2400, 33.4, "Büro; Halle 2")
        val t2 = Trip(1_600_000_000_000L, 1_600_000_000_000L + 10 * 60_000L, 4_000.0, 500, 14.0, "Bäcker")
        val lines = TripExport.csv(listOf(t1, t2), setOf(t1.start)).trim().split("\r\n")
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("Datum;"))
        // oldest first
        assertTrue(lines[1].contains("Bäcker") && lines[1].endsWith(";Privat"))
        assertTrue(lines[2].contains("\"Büro; Halle 2\";52,3;0:45;0:40;120;Dienstlich"))
    }
}
