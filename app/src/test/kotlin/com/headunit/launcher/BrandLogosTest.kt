package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrandLogosTest {
    @Test fun keyIgnoresCaseAndBranchSuffixes() {
        assertEquals("lidl", BrandLogos.key("LIDL"))
        assertEquals("lidl", BrandLogos.key("Lidl - Hauptstraße"))
        assertEquals("rewe", BrandLogos.key("REWE Markt"))
    }

    @Test fun branchesOfTheChainMatch() {
        assertTrue(BrandLogos.sameChain("Lidl", null, "lidl"))
        assertTrue(BrandLogos.sameChain("Aldi Süd", null, "aldi"))
        assertTrue(BrandLogos.sameChain("Filiale 12", "Lidl", "lidl"))
    }

    @Test fun placesThatOnlyStartAlikeDoNot() {
        assertFalse(BrandLogos.sameChain("Realschule Musterstadt", null, "real"))
        assertFalse(BrandLogos.sameChain("Starbucks", null, "star"))
    }

    @Test fun knownChainsHaveAWebsite() = assertEquals("lidl.de", BrandLogos.domainFor(Place("Lidl", "", LL(0.0, 0.0))))
}
