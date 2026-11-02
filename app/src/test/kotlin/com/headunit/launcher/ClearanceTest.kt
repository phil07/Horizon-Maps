package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClearanceTest {
    @Test fun heights() {
        assertEquals(3.5, Overpass.parseHeight("3.5")!!, 0.001)
        assertEquals(3.8, Overpass.parseHeight("3,8 m")!!, 0.001)
        assertEquals(3.81, Overpass.parseHeight("12'6\"")!!, 0.01)
        assertNull(Overpass.parseHeight("default"))
        assertNull(Overpass.parseHeight("below_default"))
        assertNull(Overpass.parseHeight("45"))
    }
}
