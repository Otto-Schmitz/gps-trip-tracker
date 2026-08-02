package com.gearhead.redline.location

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoMathTest {

    @Test
    fun haversine_knownDistance_isWithinOnePercent() {
        // ~1.11 km apart: 0.01° of latitude at the equator.
        val d = GeoMath.haversineMeters(0.0, 0.0, 0.01, 0.0)
        assertEquals(1113.0, d, 15.0)
    }

    @Test
    fun haversine_samePoint_isZero() {
        val d = GeoMath.haversineMeters(37.4, -122.0, 37.4, -122.0)
        assertEquals(0.0, d, 0.0001)
    }
}
