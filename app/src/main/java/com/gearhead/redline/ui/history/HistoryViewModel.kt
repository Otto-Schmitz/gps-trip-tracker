package com.gearhead.redline.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gearhead.redline.data.local.entity.TripEntity
import com.gearhead.redline.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ServiceLocator.from(application).tripRepository

    val trips: StateFlow<List<TripEntity>> = repository.observeTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
