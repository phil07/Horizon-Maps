package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class SlipForkTest {
    private fun step(off: Double, man: String) = Step(off, man, "", "")

    @Test fun turnAtFarEndOfSlipRoadCountsToWhereItLeavesTheMotorway() {
        // motorway until 1000 m, slip road, turn left at 1400 m: the turn counts down to 1000 m
        val out = slipForks(listOf(step(200.0, "STRAIGHT"), step(1400.0, "TURN_LEFT")), listOf(0.0 to 1000.0))
        assertEquals(1000.0, out[1].fork, 0.01)
        assertEquals(-1.0, out[0].fork, 0.01)
    }

    @Test fun turnWithItsOwnExitStepIsLeftAlone() {
        val out = slipForks(listOf(step(1000.0, "MOTORWAY_EXIT_LEFT"), step(1400.0, "TURN_LEFT")), listOf(0.0 to 1000.0))
        assertEquals(-1.0, out[1].fork, 0.01)
    }

    @Test fun turnStillOnTheMotorwayIsLeftAlone() {
        val out = slipForks(listOf(step(500.0, "TURN_LEFT")), listOf(0.0 to 1000.0))
        assertEquals(-1.0, out[0].fork, 0.01)
    }

    @Test fun exitSideFollowsTheTurn() {
        assertEquals("MOTORWAY_EXIT_LEFT", slipExitManeuver("TURN_SLIGHT_LEFT"))
        assertEquals("MOTORWAY_EXIT_RIGHT", slipExitManeuver("SHARP_RIGHT"))
    }
}
