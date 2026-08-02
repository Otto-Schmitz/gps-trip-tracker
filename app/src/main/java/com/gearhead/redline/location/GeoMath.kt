package com.gearhead.redline.location

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Geospatial helpers used by the metrics accumulator and GPS filter. */
object GeoMath {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /**
     * Great-circle distance between two coordinates in meters (Haversine).
     * Accurate enough for consecutive GPS fixes a few meters to a few hundred
     * meters apart, and far cheaper than Vincenty.
     */
    fun haversineMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double,
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val rLat1 = Math.toRadians(lat1)
        val rLat2 = Math.toRadians(lat2)

        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(rLat1) * cos(rLat2) * sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }
}
