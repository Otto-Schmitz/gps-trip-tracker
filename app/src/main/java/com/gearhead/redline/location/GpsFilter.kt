package com.gearhead.redline.location

import kotlin.math.abs

/**
 * Stateful noise filter for a single trip. Feed it fixes in arrival order; it
 * decides whether each one is trustworthy and, if so, reports the cleaned speed
 * and the segment (distance + time) since the previous accepted fix.
 *
 * Rejection rules:
 *  - accuracy worse than [TrackingConfig.MAX_ACCURACY_METERS]
 *  - non-monotonic or duplicate timestamps
 *  - implied speed above [TrackingConfig.MAX_PLAUSIBLE_SPEED_MPS] (a position jump)
 *
 * Not thread-safe: call from a single collection coroutine.
 */
class GpsFilter {

    private var last: GpsSample? = null

    sealed interface Result {
        data class Accept(
            val sample: GpsSample,
            /** Best available speed for this fix, m/s. */
            val speedMps: Float,
            /** Haversine distance from the previous accepted fix, meters (0 for the first). */
            val distanceFromPrevMeters: Double,
            /** Elapsed time since the previous accepted fix, ms (0 for the first). */
            val dtMillis: Long,
            val isFirst: Boolean,
        ) : Result

        data class Reject(val reason: String) : Result
    }

    fun process(sample: GpsSample): Result {
        if (sample.accuracyMeters > TrackingConfig.MAX_ACCURACY_METERS) {
            return Result.Reject("low accuracy ${sample.accuracyMeters}m")
        }

        val prev = last
        if (prev == null) {
            last = sample
            return Result.Accept(
                sample = sample,
                speedMps = cleanedSpeed(sample, distanceMeters = 0.0, dtMillis = 0),
                distanceFromPrevMeters = 0.0,
                dtMillis = 0,
                isFirst = true,
            )
        }

        val dt = sample.timestamp - prev.timestamp
        if (dt <= 0) {
            return Result.Reject("non-monotonic timestamp")
        }

        val distance = GeoMath.haversineMeters(
            prev.latitude, prev.longitude, sample.latitude, sample.longitude,
        )

        val impliedSpeed = distance / (dt / 1000.0)
        if (impliedSpeed > TrackingConfig.MAX_PLAUSIBLE_SPEED_MPS) {
            // Position teleported; drop it but keep `last` so we re-anchor on the next good fix.
            return Result.Reject("implausible jump ${impliedSpeed.toInt()} m/s")
        }

        // Suppress stationary jitter so idling at a light doesn't accrue phantom distance.
        val stationaryJitter = distance < TrackingConfig.MIN_DISPLACEMENT_METERS &&
            reportedOrDerivedSpeed(sample, distance, dt) < TrackingConfig.MOVING_SPEED_THRESHOLD_MPS
        val effectiveDistance = if (stationaryJitter) 0.0 else distance

        last = sample
        return Result.Accept(
            sample = sample,
            speedMps = cleanedSpeed(sample, effectiveDistance, dt),
            distanceFromPrevMeters = effectiveDistance,
            dtMillis = dt,
            isFirst = false,
        )
    }

    /**
     * Prefer the GPS Doppler speed when the fix reports one with usable accuracy,
     * since it's more stable than differentiating positions. Fall back to the
     * derived speed otherwise.
     */
    private fun cleanedSpeed(sample: GpsSample, distanceMeters: Double, dtMillis: Long): Float {
        val derived = if (dtMillis > 0) (distanceMeters / (dtMillis / 1000.0)).toFloat() else 0f
        if (!sample.hasSpeed) return derived

        val acc = sample.speedAccuracyMps
        // If the reported speed is wildly off from the derived one and its accuracy
        // is poor, trust geometry instead.
        val trustReported = acc == null || acc <= 3f || abs(sample.speedMps - derived) < 5f
        return if (trustReported) sample.speedMps else derived
    }

    private fun reportedOrDerivedSpeed(sample: GpsSample, distanceMeters: Double, dtMillis: Long): Float {
        if (sample.hasSpeed) return sample.speedMps
        return if (dtMillis > 0) (distanceMeters / (dtMillis / 1000.0)).toFloat() else 0f
    }
}
