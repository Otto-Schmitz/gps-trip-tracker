package com.gearhead.redline.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One recorded ride. Metrics are computed and persisted when the trip is stopped.
 * All speeds are stored in meters/second and all durations in milliseconds (SI);
 * UI formats to km/h and human-readable time at the edge.
 */
@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Epoch millis when "Start Trip" was pressed. */
    val startedAt: Long,

    /** Epoch millis when "Stop Trip" was pressed; null while recording. */
    val endedAt: Long? = null,

    /** Total ground distance via Haversine over accepted points, in meters. */
    val distanceMeters: Double = 0.0,

    /** distanceMeters / (movingMillis / 1000), in m/s. Excludes time spent stopped. */
    val avgSpeedMps: Double = 0.0,

    /** Highest plausible instantaneous speed observed, in m/s. */
    val maxSpeedMps: Double = 0.0,

    /** Wall-clock duration end - start, in millis. */
    val durationMillis: Long = 0,

    /** Time spent above the "moving" speed threshold, in millis. */
    val movingMillis: Long = 0,

    /** Number of GPS points accepted after noise filtering. */
    val pointCount: Int = 0,
)
