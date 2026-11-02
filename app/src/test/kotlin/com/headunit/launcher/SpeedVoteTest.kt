package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedVoteTest {
    private fun kmh(v: Int) = v / 3.6

    @Test fun signAndOsmOutvoteTomTom() {
        val r = SpeedVote.decide(listOf(
            SpeedVote.Vote("TomTom", kmh(70), SpeedVote.W_TOMTOM),
            SpeedVote.Vote("OSM", kmh(50), SpeedVote.W_OSM),
            SpeedVote.Vote("Schild", kmh(50), SpeedVote.W_SIGN)))!!
        assertEquals(50, SpeedVote.key(r.ms))
        assertTrue(r.disputed)
    }

    @Test fun agreementIsNotDisputed() {
        val r = SpeedVote.decide(listOf(SpeedVote.Vote("TomTom", kmh(100), 2.0), SpeedVote.Vote("OSM", kmh(100), 1.5)))!!
        assertFalse(r.disputed)
        assertEquals(100, SpeedVote.key(r.ms))
    }

    @Test fun tieGoesToLowerAndNoneIsHighest() {
        val r = SpeedVote.decide(listOf(SpeedVote.Vote("HERE", SpeedVote.NONE, 2.0), SpeedVote.Vote("TomTom", kmh(120), 2.0)))!!
        assertEquals(120, SpeedVote.key(r.ms))
        val n = SpeedVote.decide(listOf(SpeedVote.Vote("HERE", SpeedVote.NONE, 2.0), SpeedVote.Vote("OSM", kmh(130), 0.5, true)))!!
        assertEquals(SpeedVote.NONE, n.ms, 0.0)
        assertFalse(n.disputed)   // a guess disagreeing doesn't count
    }

    @Test fun nothingToDecide() { assertNull(SpeedVote.decide(emptyList())) }

    @Test fun signAge() {
        val y = 365L * 24 * 3600 * 1000
        assertEquals(SpeedVote.W_SIGN, SpeedVote.signWeight(y / 2), 0.0)
        assertTrue(SpeedVote.signWeight(y * 5 / 2) < SpeedVote.W_SIGN)
        assertEquals(0.0, SpeedVote.signWeight(4 * y), 0.0)
    }

    @Test fun mapillaryValues() {
        assertEquals(50, SpeedVote.key(Mapillary.parseValue("regulatory--maximum-speed-limit-50--g1", false)!!))
        assertEquals(30, SpeedVote.key(Mapillary.parseValue("regulatory--maximum-speed-limit-zone-30--g1", false)!!))
        assertEquals(-1.0, Mapillary.parseValue("regulatory--end-of-maximum-speed-limit-70--g1", false)!!, 0.0)
        assertNull(Mapillary.parseValue("regulatory--minimum-speed-limit-30--g1", false))
        assertTrue(Mapillary.parseTime("2021-12-23T03:24:54+0000") > 0L)
    }

    @Test fun passedSignBehindOnYourSide() {
        // driving north; a sign 100 m behind, 6 m to the right, facing south (towards you): counts
        val p = LL(50.0, 8.0)
        val behind = MlySign(LL(50.0 - 100 / 111_320.0, 8.0 + 6 / (111_320.0 * Math.cos(Math.toRadians(50.0)))), kmh(50), 180.0, 0L)
        // one ahead, and one behind facing the other carriageway
        val ahead = MlySign(LL(50.0 + 50 / 111_320.0, 8.0), kmh(70), 180.0, 0L)
        val other = MlySign(LL(50.0 - 40 / 111_320.0, 8.0), kmh(100), 0.0, 0L)
        assertEquals(behind, Mapillary.passed(listOf(behind, ahead, other), p, 0.0))
        assertNull(Mapillary.passed(listOf(ahead, other), p, 0.0))
    }
}
