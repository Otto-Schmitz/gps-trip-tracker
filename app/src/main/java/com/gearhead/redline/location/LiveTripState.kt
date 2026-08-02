package com.gearhead.redline.location

/**
 * Live snapshot of the in-progress trip, published by the tracking service and
 * observed by the cockpit UI. Not persisted — the final metrics are written to
 * the trip row on stop.
 */
data class LiveTripState(
    val isRecording: Boolean = false,
    val tripId: Long? = null,
    val startedAt: Long = 0L,
    val elapsedMillis: Long = 0L,
    val movingMillis: Long = 0L,
    val distanceMeters: Double = 0.0,
    val currentSpeedMps: Float = 0f,
    val maxSpeedMps: Float = 0f,
    val pointCount: Int = 0,
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    /** True once at least one fix has been accepted (UI can hide "acquiring GPS"). */
    val hasFix: Boolean = false,
) {
    val avgSpeedMps: Double
        get() = if (movingMillis > 0) distanceMeters / (movingMillis / 1000.0) else 0.0

    companion object {
        val Idle = LiveTripState()
    }
}
