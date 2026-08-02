package com.gearhead.redline.data.repository

import com.gearhead.redline.data.local.TripDao
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.local.entity.TripEntity
import com.gearhead.redline.data.local.entity.TripWithPoints
import kotlinx.coroutines.flow.Flow

/**
 * Single entry point to trip persistence. ViewModels and the tracking service
 * talk to this, never to the DAO directly.
 */
class TripRepository(private val dao: TripDao) {

    fun observeTrips(): Flow<List<TripEntity>> = dao.observeTrips()

    fun observeTripWithPoints(tripId: Long): Flow<TripWithPoints?> =
        dao.observeTripWithPoints(tripId)

    suspend fun getTripWithPoints(tripId: Long): TripWithPoints? =
        dao.getTripWithPoints(tripId)

    suspend fun getPointsForTrip(tripId: Long): List<LocationPointEntity> =
        dao.getPointsForTrip(tripId)

    /** Creates the trip row at "Start Trip" and returns its generated id. */
    suspend fun startTrip(startedAt: Long): Long =
        dao.insertTrip(TripEntity(startedAt = startedAt))

    suspend fun appendPoint(point: LocationPointEntity) {
        dao.insertPoint(point)
    }

    /** Writes the final computed metrics when the trip is stopped. */
    suspend fun finishTrip(trip: TripEntity) {
        dao.updateTrip(trip)
    }

    suspend fun getTrip(tripId: Long): TripEntity? = dao.getTrip(tripId)

    suspend fun deleteTrip(tripId: Long) = dao.deleteTrip(tripId)

    suspend fun deleteTripIfEmpty(tripId: Long) = dao.deleteTripIfEmpty(tripId)
}
