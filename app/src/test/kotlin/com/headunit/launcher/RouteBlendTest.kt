package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteBlendTest {
    // a straight line north, a point about every 111 m, flowing for the first half and heavy after
    private val pts = (0..10).map { LL(50.0 + it * 0.001, 8.0) }
    private fun lvl(j: Int) = if (j < 5) 0 else 2

    @Test fun withoutBlendingOnePiecePerLevel() {
        val out = RouteBlend.pieces(pts, ::lvl, blend = false)
        assertEquals(listOf(0.0, 2.0), out.map { it.level })
        assertEquals(pts[5], out[0].pts.last())
        assertEquals(pts[5], out[1].pts.first())
    }

    @Test fun blendingAddsInBetweenLevelsAroundTheChange() {
        val out = RouteBlend.pieces(pts, ::lvl, blend = true, span = 80.0, steps = 4)
        assertEquals(0.0, out.first().level, 1e-9)
        assertEquals(2.0, out.last().level, 1e-9)
        val mids = out.map { it.level }.filter { it > 0.0 && it < 2.0 }
        assertEquals(listOf(0.25, 0.75, 1.25, 1.75), mids)
        // the pieces join up: each one starts where the one before ends
        for (i in 1 until out.size) {
            val a = out[i - 1].pts.last(); val b = out[i].pts.first()
            assertTrue(Geo.dist(a, b) < 0.01)
        }
    }

    @Test fun oneLevelIsOnePiece() {
        val out = RouteBlend.pieces(pts, { 1 }, blend = true)
        assertEquals(1, out.size)
        assertEquals(1.0, out[0].level, 1e-9)
    }
}
