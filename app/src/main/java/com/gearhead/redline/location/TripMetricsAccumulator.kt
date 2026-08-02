package com.gearhead.redline.location

import kotlin.math.min

/**
 * Folds accepted GPS fixes into running trip metrics. Holds only primitives, so
 * it's trivially unit-testable. The tracking service owns one instance per trip.
 */
class TripMetricsAccumulator(private val startedAt: Long) {

    var distanceMeters: Double = 0.0
        private set
    var movingMillis: Long = 0
        private set
    var maxSpeedMps: Float = 0f
        private set
    var pointCount: Int = 0
        private set
    var currentSpeedMps: Float = 0f
        private set
    var lastLatitude: Double? = null
        private set
    var lastLongitude: Double? = null
        private set

    /** Apply one accepted fix. Returns nothing; read the properties after. */
    fun onAccepted(accept: GpsFilter.Result.Accept) {
        pointCount++
        currentSpeedMps = accept.speedMps
        lastLatitude = accept.sample.latitude
        lastLongitude = accept.sample.longitude

        if (accept.speedMps > maxSpeedMps) {
            maxSpeedMps = accept.speedMps
        }
        if (accept.isFirst) return

        distanceMeters += accept.distanceFromPrevMeters

        // Only credit time toward "moving" when actually in motion, and never
        // credit a huge stale gap (GPS was lost) to keep avg speed honest.
        if (accept.speedMps >= TrackingConfig.MOVING_SPEED_THRESHOLD_MPS) {
            movingMillis += min(accept.dtMillis, TrackingConfig.MAX_SEGMENT_GAP_MS)
        }
    }

    fun toLiveState(tripId: Long, now: Long): LiveTripState = LiveTripState(
        isRecording = true,
        tripId = tripId,
        startedAt = startedAt,
        elapsedMillis = now - startedAt,
        movingMillis = movingMillis,
        distanceMeters = distanceMeters,
        currentSpeedMps = currentSpeedMps,
        maxSpeedMps = maxSpeedMps,
        pointCount = pointCount,
        lastLatitude = lastLatitude,
        lastLongitude = lastLongitude,
        hasFix = pointCount > 0,
    )

    val avgSpeedMps: Double
        get() = if (movingMillis > 0) distanceMeters / (movingMillis / 1000.0) else 0.0
}
