package com.gearhead.redline.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single accepted GPS fix belonging to a trip (1:N). Rows are deleted with
 * their parent trip via [ForeignKey.CASCADE]. Indexed on tripId for fast route
 * lookups on the detail screen.
 */
@Entity(
    tableName = "location_points",
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("tripId")],
)
data class LocationPointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val tripId: Long,

    val latitude: Double,
    val longitude: Double,

    /** Epoch millis of the fix. */
    val timestamp: Long,

    /** Instantaneous speed in m/s (GPS Doppler when available, else derived). */
    val speedMps: Float,

    /** Altitude in meters if the fix provided it, otherwise null. */
    val altitude: Double? = null,
)
