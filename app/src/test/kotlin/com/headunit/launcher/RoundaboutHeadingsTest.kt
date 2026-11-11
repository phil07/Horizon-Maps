package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/** The roundabout's way in and out read from the route line (what the map shows), for a ring of 20 m radius. */
class RoundaboutHeadingsTest {
    private val c = LL(50.0, 8.0)

    /** x east, y north, in metres from the ring's centre. */
    private fun ll(x: Double, y: Double) = LL(c.lat + y / 111320.0, c.lon + x / (111320.0 * Math.cos(Math.toRadians(c.lat))))

    /**
     * Coming up from the south onto the ring, round it to [exitDeg] (0 east, 90 north, 180 west; anticlockwise where traffic keeps
     * right, clockwise where it keeps left), then straight out from the ring. Returns the line and where the ring starts.
     */
    private fun line(exitDeg: Double, keepLeft: Boolean = false): Triple<List<LL>, DoubleArray, Double> {
        val r = 20.0
        val pts = ArrayList<LL>()
        var y = -80.0
        while (y < -r) { pts += ll(0.0, y); y += 5.0 }
        val startIdx = pts.size
        pts += ll(0.0, -r)
        // round the ring in 10° steps
        val dir = if (keepLeft) -1.0 else 1.0
        var a = -90.0
        val end = if (keepLeft) (if (exitDeg > -90.0) exitDeg - 360.0 else exitDeg) else (if (exitDeg < -90.0) exitDeg + 360.0 else exitDeg)
        while ((end - a) * dir > 0.1) {
            a += 10.0 * dir
            if ((end - a) * dir < 0) a = end
            pts += ll(r * Math.cos(Math.toRadians(a)), r * Math.sin(Math.toRadians(a)))
        }
        // straight out
        for (k in 1..16) {
            val rr = r + k * 5.0
            pts += ll(rr * Math.cos(Math.toRadians(end)), rr * Math.sin(Math.toRadians(end)))
        }
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + Geo.dist(pts[i - 1], pts[i])
        return Triple(pts, cum, cum[startIdx])
    }

    private fun turn(exitDeg: Double, keepLeft: Boolean = false): Double {
        val (pts, cum, entry) = line(exitDeg, keepLeft)
        val h = Geo.roundaboutHeadings(pts, cum, entry, -1.0, Double.MAX_VALUE, keepLeft)
        assertNotNull(h)
        return ((h!!.second - h.first + 540.0) % 360.0) - 180.0
    }

    @Test fun thirdExitToTheLeftPointsLeft() = assertEquals(-90.0, turn(180.0), 15.0)

    @Test fun straightOnPointsAhead() = assertEquals(0.0, turn(90.0), 15.0)

    @Test fun firstExitToTheRightPointsRight() = assertEquals(90.0, turn(0.0), 15.0)

    @Test fun keepLeftFirstExitPointsLeft() = assertEquals(-90.0, turn(180.0, keepLeft = true), 15.0)

    @Test fun exitFromTheRouteDataIsUsedWhenGiven() {
        val (pts, cum, entry) = line(180.0)
        // three quarters of the ring (about 94 m) before the exit to the west
        val exitAt = entry + 1.5 * Math.PI * 20.0
        val h = Geo.roundaboutHeadings(pts, cum, entry, exitAt, Double.MAX_VALUE, false)!!
        assertEquals(-90.0, ((h.second - h.first + 540.0) % 360.0) - 180.0, 10.0)
    }
}
