package com.gearhead.redline.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gearhead.redline.data.local.entity.TripWithPoints
import com.gearhead.redline.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Loads one trip with its route points. [tripId] is read from the navigation
 * argument via [SavedStateHandle], so the default ViewModel factory can build
 * this without a custom factory.
 */
class TripDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val repository = ServiceLocator.from(application).tripRepository

    private val tripId: Long = savedStateHandle.get<Long>(ARG_TRIP_ID) ?: -1L

    val trip: StateFlow<TripWithPoints?> = repository.observeTripWithPoints(tripId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    companion object {
        const val ARG_TRIP_ID = "tripId"
    }
}
