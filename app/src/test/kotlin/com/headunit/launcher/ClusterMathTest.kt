package com.headunit.launcher

import com.headunit.launcher.car.ClusterMath
import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterMathTest {
    @Test fun standingReaches350m() = assertEquals(350.0, ClusterMath.reach(0.0), 0.01)

    @Test fun fasterReachesFurther() = assertEquals(350.0 + 30.0 * 25.0, ClusterMath.reach(30.0), 0.01)

    @Test fun neverFurtherThan1500m() = assertEquals(1500.0, ClusterMath.reach(70.0), 0.01)
}
