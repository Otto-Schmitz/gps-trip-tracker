package com.gearhead.redline.di

import android.content.Context
import com.gearhead.redline.RedlineApp
import com.gearhead.redline.data.local.RedlineDatabase
import com.gearhead.redline.data.repository.TripRepository

/**
 * Minimal manual dependency container. For an MVP with one repository this is
 * lighter than Hilt and keeps wiring explicit; swap for Hilt if the graph grows.
 */
class ServiceLocator(context: Context) {

    private val database by lazy { RedlineDatabase.get(context) }

    val tripRepository: TripRepository by lazy { TripRepository(database.tripDao()) }

    companion object {
        /** Convenience accessor from any Context. */
        fun from(context: Context): ServiceLocator =
            (context.applicationContext as RedlineApp).serviceLocator
    }
}
