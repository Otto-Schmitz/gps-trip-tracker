package com.gearhead.redline.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TripMetricsAccumulatorTest {

    private fun accept(
        speed: Float,
        distance: Double,
        dt: Long,
        isFirst: Boolean = false,
    ) = GpsFilter.Result.Accept(
        sample = GpsSample(0.0, 0.0, 0L, true, speed, 1f, false, 0.0, 5f),
        speedMps = speed,
        distanceFromPrevMeters = distance,
        dtMillis = dt,
        isFirst = isFirst,
    )

    @Test
    fun tracksMaxSpeedAndDistance() {
        val acc = TripMetricsAccumulator(startedAt = 0L)
        acc.onAccepted(accept(10f, 0.0, 0, isFirst = true))
        acc.onAccepted(accept(25f, 25.0, 1000))
        acc.onAccepted(accept(15f, 15.0, 1000))

        assertEquals(40.0, acc.distanceMeters, 0.001)
        assertEquals(25f, acc.maxSpeedMps)
        assertEquals(3, acc.pointCount)
    }

    @Test
    fun stoppedTimeIsExcludedFromMovingTime() {
        val acc = TripMetricsAccumulator(startedAt = 0L)
        acc.onAccepted(accept(10f, 0.0, 0, isFirst = true))
        // Moving segment: 2s above threshold.
        acc.onAccepted(accept(10f, 20.0, 2000))
        // Stopped segment: below MOVING_SPEED_THRESHOLD_MPS, should not add moving time.
        acc.onAccepted(accept(0.2f, 0.0, 5000))

        assertEquals(2000L, acc.movingMillis)
        // Avg speed uses moving time only: 20m / 2s = 10 m/s.
        assertEquals(10.0, acc.avgSpeedMps, 0.001)
    }

    @Test
    fun hugeGapIsCappedInMovingTime() {
        val acc = TripMetricsAccumulator(startedAt = 0L)
        acc.onAccepted(accept(10f, 0.0, 0, isFirst = true))
        // 60s gap while "moving" — should be capped at MAX_SEGMENT_GAP_MS.
        acc.onAccepted(accept(10f, 100.0, 60_000))
        assertTrue(acc.movingMillis <= TrackingConfig.MAX_SEGMENT_GAP_MS)
    }
}
