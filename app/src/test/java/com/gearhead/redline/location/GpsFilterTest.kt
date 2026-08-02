package com.gearhead.redline.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsFilterTest {

    private fun sample(
        lat: Double,
        lon: Double,
        t: Long,
        speed: Float = 0f,
        hasSpeed: Boolean = true,
        accuracy: Float = 5f,
    ) = GpsSample(
        latitude = lat,
        longitude = lon,
        timestamp = t,
        hasSpeed = hasSpeed,
        speedMps = speed,
        speedAccuracyMps = 1f,
        hasAltitude = false,
        altitude = 0.0,
        accuracyMeters = accuracy,
    )

    @Test
    fun firstFix_isAcceptedAsFirst() {
        val filter = GpsFilter()
        val r = filter.process(sample(0.0, 0.0, 1000))
        assertTrue(r is GpsFilter.Result.Accept && r.isFirst)
    }

    @Test
    fun lowAccuracyFix_isRejected() {
        val filter = GpsFilter()
        val r = filter.process(sample(0.0, 0.0, 1000, accuracy = 100f))
        assertTrue(r is GpsFilter.Result.Reject)
    }

    @Test
    fun teleportJump_isRejected() {
        val filter = GpsFilter()
        filter.process(sample(0.0, 0.0, 1000))
        // ~11 km in 1 second -> ~11000 m/s, far past the plausibility cap.
        val r = filter.process(sample(0.1, 0.0, 2000, speed = 5f))
        assertTrue(r is GpsFilter.Result.Reject)
    }

    @Test
    fun normalMovement_accumulatesDistance() {
        val filter = GpsFilter()
        filter.process(sample(0.0, 0.0, 1000, speed = 10f))
        val r = filter.process(sample(0.0001, 0.0, 2000, speed = 11f))
        assertTrue(r is GpsFilter.Result.Accept)
        r as GpsFilter.Result.Accept
        assertTrue("expected ~11m segment", r.distanceFromPrevMeters in 10.0..12.0)
    }

    @Test
    fun stationaryJitter_producesZeroDistance() {
        val filter = GpsFilter()
        filter.process(sample(0.0, 0.0, 1000, speed = 0f))
        // ~0.5 m wobble while reported speed is ~0 -> treated as noise.
        val r = filter.process(sample(0.0000045, 0.0, 2000, speed = 0.2f))
        r as GpsFilter.Result.Accept
        assertEquals(0.0, r.distanceFromPrevMeters, 0.0001)
    }
}
