package com.gearhead.redline.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.local.entity.TripEntity
import com.gearhead.redline.data.local.entity.TripWithPoints
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insertTrip(trip: TripEntity): Long

    @Update
    suspend fun updateTrip(trip: TripEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoint(point: LocationPointEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPoints(points: List<LocationPointEntity>)

    /** History list, newest first. Reactive so new trips appear automatically. */
    @Query("SELECT * FROM trips ORDER BY startedAt DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTrip(tripId: Long): TripEntity?

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTripWithPoints(tripId: Long): Flow<TripWithPoints?>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripWithPoints(tripId: Long): TripWithPoints?

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getPointsForTrip(tripId: Long): List<LocationPointEntity>

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    /** Removes an empty trip (e.g. stopped before any fix was accepted). */
    @Query("DELETE FROM trips WHERE id = :tripId AND pointCount = 0")
    suspend fun deleteTripIfEmpty(tripId: Long)
}
