package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The turn at a roundabout, measured in the route line. The lines follow the roundabout on the B 44 near Biblis as it is
 * mapped (arms to the south-west, north-west and north-east, and a track to the south-east).
 */
class RoundaboutLineTest {
    private fun line(vararg v: Double): Pair<List<LL>, DoubleArray> {
        val pts = (0 until v.size / 2).map { LL(v[it * 2], v[it * 2 + 1]) }
        val cum = DoubleArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + Geo.dist(pts[i - 1], pts[i])
        return pts to cum
    }

    // (each joins the ring about 155 m along)
    private val straight = line(49.7054897, 8.4755066, 49.7058421, 8.4770832, 49.7059462, 8.4775489, 49.7059302, 8.4776101, 49.7059261, 8.4776758, 49.7059341, 8.4777406, 49.7059536, 8.4777993, 49.7059806, 8.4778439, 49.7060141, 8.4778760, 49.7060519, 8.4778935, 49.7060913, 8.4778950, 49.7061619, 8.4780213, 49.7062855, 8.4783043, 49.7068901, 8.4796887)
    private val left = line(49.7054897, 8.4755066, 49.7058421, 8.4770832, 49.7059462, 8.4775489, 49.7059302, 8.4776101, 49.7059261, 8.4776758, 49.7059341, 8.4777406, 49.7059536, 8.4777993, 49.7059806, 8.4778439, 49.7060141, 8.4778760, 49.7060519, 8.4778935, 49.7060913, 8.4778950, 49.7061301, 8.4778803, 49.7061651, 8.4778501, 49.7061936, 8.4778067, 49.7062141, 8.4777514, 49.7062241, 8.4776895, 49.7062227, 8.4776257, 49.7064371, 8.4772583, 49.7071615, 8.4760169)
    private val back = line(49.7054897, 8.4755066, 49.7058421, 8.4770832, 49.7059462, 8.4775489, 49.7059302, 8.4776101, 49.7059261, 8.4776758, 49.7059341, 8.4777406, 49.7059536, 8.4777993, 49.7059806, 8.4778439, 49.7060141, 8.4778760, 49.7060519, 8.4778935, 49.7060913, 8.4778950, 49.7061301, 8.4778803, 49.7061651, 8.4778501, 49.7061936, 8.4778067, 49.7062141, 8.4777514, 49.7062241, 8.4776895, 49.7062227, 8.4776257, 49.7062074, 8.4775567, 49.7061790, 8.4774985, 49.7061402, 8.4774569, 49.7060981, 8.4774367, 49.7060540, 8.4774364, 49.7058421, 8.4770832, 49.7051066, 8.4758572)

    /** The turn shown (0 straight on, 90 right, -90 left), to the nearest eighth, with the step given [off] metres from the ring. */
    private fun shown(l: Pair<List<LL>, DoubleArray>, off: Double): Double {
        val arc = Geo.ringOnLine(l.first, l.second, 155.0 + off, -1.0, Double.MAX_VALUE, false)
        assertNotNull("no ring found with the step $off m from it", arc)
        return Math.round((180.0 - arc!!) / 45.0) * 45.0
    }

    @Test fun straightOnIsNotARightTurn() {
        // the step of a route often lies some metres before the ring: that read as half right ("turn right")
        for (off in listOf(-20.0, -10.0, 0.0, 10.0)) assertEquals(0.0, shown(straight, off), 0.01)
    }

    @Test fun thirdExitIsALeftTurn() {
        for (off in listOf(-20.0, -10.0, 0.0, 10.0)) assertEquals(-90.0, shown(left, off), 0.01)
    }

    @Test fun allTheWayRoundIsATurnBack() {
        for (off in listOf(-20.0, 0.0, 10.0)) assertEquals(-180.0, shown(back, off), 0.01)
    }
}
