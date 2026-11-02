package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkingLiveTest {
    private val d = LL(49.0094, 8.4044)

    @Test fun levels() {
        assertEquals(1, Garage("a", "A", d, 400, 200).level())
        assertEquals(2, Garage("a", "A", d, 400, 30).level())
        assertEquals(2, Garage("a", "A", d, 50, 15).level())
        assertEquals(3, Garage("a", "A", d, 400, 0).level())
        assertEquals(3, Garage("a", "A", d, 400, 200, open = false).level())
        assertEquals(0, Garage("a", "A", d, 400, null).level())
    }

    @Test fun mergeKeepsLiveOne() {
        val a = listOf(Garage("m1", "Parkhaus Schlossplatz", d, 900, null))
        val b = listOf(Garage("p1", "Schlossplatz", LL(d.lat + 0.0003, d.lon), 912, 587), Garage("p2", "Far", LL(d.lat + 0.01, d.lon), 100, 5))
        val m = ParkingLive.merge(a, b)
        assertEquals(2, m.size)
        assertEquals(587, m[0].free)
        assertEquals("Schlossplatz", m[0].name)
    }

    @Test fun nearestWithRoomFirst() {
        val full = Nearby(Place("Full", "", d), 100.0, "", free = 0, total = 300, open = true)
        val far = Nearby(Place("Far", "", d), 500.0, "", free = 50, total = 300, open = true)
        val unknown = Nearby(Place("Unknown", "", d), 300.0, "")
        assertEquals(listOf("Unknown", "Far", "Full"), ParkingLive.sortNearby(listOf(full, far, unknown)).map { it.place.name })
    }

    @Test fun enrichAddsCountsAndExtraGarages() {
        val found = listOf(Nearby(Place("Tiefgarage Markt", "", LL(d.lat, d.lon)), 120.0, "Tiefgarage"))
        val gs = listOf(Garage("m1", "Markt", LL(d.lat + 0.0002, d.lon), 300, 120), Garage("m2", "Bahnhof", LL(d.lat + 0.005, d.lon), 500, 10))
        val out = ParkingLive.enrich(found, gs, d, 800.0)
        assertEquals(2, out.size)
        assertEquals(120, out[0].free)
        assertEquals(10, out[1].free)
    }

    @Test fun isoTimes() {
        assertEquals(0L, ParkingLive.parseIso(""))
        assertEquals(ParkingLive.parseIso("2026-11-02T15:25:00Z"), ParkingLive.parseIso("2026-11-02T15:25:00"))
        assertTrue(ParkingLive.parseIso("2026-11-02T15:25:00Z") > 0L)
    }
}
