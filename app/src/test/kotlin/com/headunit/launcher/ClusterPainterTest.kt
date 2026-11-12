package com.headunit.launcher

import com.headunit.launcher.car.ClusterPainter
import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterPainterTest {
    @Test fun standingReaches350m() = assertEquals(350.0, ClusterPainter.reach(0.0), 0.01)

    @Test fun fasterReachesFurther() = assertEquals(350.0 + 30.0 * 25.0, ClusterPainter.reach(30.0), 0.01)

    @Test fun neverFurtherThan1500m() = assertEquals(1500.0, ClusterPainter.reach(70.0), 0.01)
}
