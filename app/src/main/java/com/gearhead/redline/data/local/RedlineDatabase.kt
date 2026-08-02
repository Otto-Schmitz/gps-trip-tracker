package com.gearhead.redline.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RedlineDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var instance: RedlineDatabase? = null

        fun get(context: Context): RedlineDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RedlineDatabase::class.java,
                    "redline.db",
                ).build().also { instance = it }
            }
    }
}
