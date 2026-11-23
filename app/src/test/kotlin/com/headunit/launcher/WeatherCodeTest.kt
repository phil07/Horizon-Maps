package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherCodeTest {
    @Test fun clearSkyStaysDryWithATraceOfRain() = assertEquals(0, Weather.effectiveCode(0, 0.1, 0.0))
    @Test fun clearSkyStaysDryEvenWithRainFigure() = assertEquals(1, Weather.effectiveCode(1, 1.0, 0.0))
    @Test fun aTraceUnderCloudIsNotRain() = assertEquals(3, Weather.effectiveCode(3, 0.1, 0.0))
    @Test fun realRainUnderCloudIsRain() = assertEquals(61, Weather.effectiveCode(3, 0.5, 0.0))
    @Test fun realSnowUnderCloudIsSnow() = assertEquals(71, Weather.effectiveCode(2, 0.0, 0.5))
}
