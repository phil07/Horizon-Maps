package com.headunit.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SatTileCacheTest {
    @Test fun bboxOfWholeWorldTile() {
        assertEquals("-20037508.343,-20037508.343,20037508.343,20037508.343", SatTileCache.bbox(0, 0, 0))
    }

    @Test fun stateTileAddressAsksTheWmsForTheTilesBox() {
        val u = SatTileCache.upstream("de-he", 16, 34343, 22300)!!
        assertTrue(u.startsWith("https://gds-srv.hessen.de/"))
        assertTrue(u.contains("BBOX=" + SatTileCache.bbox(16, 34343, 22300)))
        assertEquals(null, SatTileCache.upstream("de-xx", 1, 0, 0))
        assertTrue(SatTileCache.upstream("eox", 10, 1, 2)!!.endsWith("/10/2/1.jpg"))
    }

    @Test fun corridorCoversRouteWithoutGaps() {
        // about 2 km east-west near Darmstadt: at zoom 16 (about 390 m tiles) a 600 m wide strip is a few tiles high
        val t = SatTileCache.corridor(listOf(LL(49.87, 8.64), LL(49.87, 8.668)), 16, 300.0)
        val xs = t.map { (it shr 32).toInt() }.toSet()
        assertEquals((xs.minOrNull()!!..xs.maxOrNull()!!).toSet(), xs)
        assertTrue(t.size in 10..60)
    }
}
