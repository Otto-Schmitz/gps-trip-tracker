package com.gearhead.redline.location

import android.location.Location
import android.os.Build

/**
 * Provider-agnostic snapshot of a location fix. Decouples the filter/metrics
 * logic from android.location.Location so it can be unit-tested off-device.
 */
data class GpsSample(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val hasSpeed: Boolean,
    val speedMps: Float,
    val speedAccuracyMps: Float?,
    val hasAltitude: Boolean,
    val altitude: Double,
    val accuracyMeters: Float,
) {
    companion object {
        fun from(location: Location): GpsSample {
            val hasSpeedAcc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                location.hasSpeedAccuracy()
            return GpsSample(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                hasSpeed = location.hasSpeed(),
                speedMps = location.speed,
                speedAccuracyMps = if (hasSpeedAcc) location.speedAccuracyMetersPerSecond else null,
                hasAltitude = location.hasAltitude(),
                altitude = location.altitude,
                accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
            )
        }
    }
}
