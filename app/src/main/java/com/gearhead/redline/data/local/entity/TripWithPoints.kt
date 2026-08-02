package com.gearhead.redline.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/** A trip together with all of its route points, for the detail screen. */
data class TripWithPoints(
    @Embedded val trip: TripEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tripId",
    )
    val points: List<LocationPointEntity>,
)
