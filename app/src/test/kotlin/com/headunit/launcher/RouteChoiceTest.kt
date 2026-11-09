package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteChoiceTest {
    @Test fun rerouteTakesQuickestWithoutAPickByHand() =
        assertEquals(1, NavigationPolicy.keepChosenIndex(listOf(900.0, 600.0, 700.0), listOf(0.9, 0.1, 0.2), false))

    @Test fun rerouteKeepsTheWayPickedByHand() =
        assertEquals(0, NavigationPolicy.keepChosenIndex(listOf(900.0, 600.0, 700.0), listOf(0.9, 0.1, 0.2), true))

    @Test fun rerouteTakesQuickestWhenNothingKeepsToThePick() =
        assertEquals(1, NavigationPolicy.keepChosenIndex(listOf(900.0, 600.0), listOf(0.3, 0.2), true))

    @Test fun oneRouteIsAlwaysTaken() = assertEquals(0, NavigationPolicy.keepChosenIndex(listOf(900.0), listOf(0.0), true))

    @Test fun anySlowerWayPickedByHandIsKept() = assertTrue(NavigationPolicy.isPickedSlower(605.0, 600.0, true))

    @Test fun theQuickestIsNeverASlowerPick() = assertFalse(NavigationPolicy.isPickedSlower(600.0, 600.0, true))

    @Test fun theAppsOwnPickCountsOnlyWhenOverAMinuteSlower() {
        assertFalse(NavigationPolicy.isPickedSlower(650.0, 600.0, false))
        assertTrue(NavigationPolicy.isPickedSlower(661.0, 600.0, false))
    }
}
