package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRankTest {
    private data class R(val name: String, val town: Boolean, val km: Double)

    private fun ranked(q: String, vararg r: R) = SearchRank.rank(q, r.mapIndexed { i, x -> x to i }, { it.name }, { it.town }, { it.km })
    private fun rank(q: String, vararg r: R) = ranked(q, *r).map { it.name }

    @Test fun cityBeforeNearStreets() {
        val out = rank("berlin",
            R("Berliner Straße", false, 1.3), R("Berliner Weg", false, 2.1), R("Berlin", true, 480.0), R("Berlingen", true, 300.0))
        assertEquals(listOf("Berlin", "Berlingen", "Berliner Straße", "Berliner Weg"), out)
    }

    @Test fun streetTypedNearestFirst() {
        val out = ranked("Berliner Str",
            R("Berliner Straße", false, 5.6), R("Berlin", true, 480.0), R("Berliner Str.", false, 3.7), R("Berliner Straße", false, 1.3))
        assertEquals(listOf(1.3, 3.7, 5.6, 480.0), out.map { it.km })
    }

    // (these two pass metres: the far limit is 100 km)
    @Test fun farTownAfterNearPlaceThatMatchesAsWell() {
        val out = ranked("lindenhof", R("Lindenhof", true, 700_000.0), R("Lindenhof", false, 1_500.0))
        assertEquals(listOf(1_500.0, 700_000.0), out.map { it.km })
    }

    @Test fun withTheTownTypedTownsStillComeFirst() {
        val out = ranked("lindenhof, mannheim", R("Lindenhof, Mannheim", true, 700_000.0), R("Lindenhof, Mannheim", false, 1_500.0))
        assertEquals(listOf(700_000.0, 1_500.0), out.map { it.km })
    }

    @Test fun exactTownsNearestFirst() {
        val out = ranked("neustadt", R("Neustadt", true, 200.0), R("Neustadt", true, 20.0), R("Neustadtweg", false, 1.0))
        assertEquals(listOf(20.0, 200.0, 1.0), out.map { it.km })
    }

    @Test fun onlineDropsOfflineNoteKeepsTown() {
        val p = SearchRank.online(Place("Berlin", "Stadt" + SearchRank.OFFLINE, LL(52.5, 13.4)).town(true))
        assertEquals("Stadt", p.sub)
        assertTrue(p.town)
    }

    @Test fun recognisesStreets() {
        assertTrue(SearchRank.streety("Berliner Str"))
        assertTrue(SearchRank.streety("Hauptstraße"))
        assertTrue(SearchRank.streety("Lindenweg"))
        assertTrue(SearchRank.streety("Goethestr 5"))
        assertFalse(SearchRank.streety("berlin"))
        assertFalse(SearchRank.streety("Frankfurt am Main"))
        assertFalse(SearchRank.streety("Darmstadt"))
    }
}
