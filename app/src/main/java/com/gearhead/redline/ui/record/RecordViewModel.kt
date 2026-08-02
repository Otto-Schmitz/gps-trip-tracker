package com.gearhead.redline.ui.record

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.gearhead.redline.location.LiveTripState
import com.gearhead.redline.location.LocationTrackingService
import com.gearhead.redline.location.RecordingState
import kotlinx.coroutines.flow.StateFlow

/**
 * Thin cockpit ViewModel. The durable state lives in the tracking service and is
 * mirrored through [RecordingState]; this just relays it and forwards start/stop
 * intents. Kept as AndroidViewModel because start/stop need an app Context.
 */
class RecordViewModel(application: Application) : AndroidViewModel(application) {

    val liveState: StateFlow<LiveTripState> = RecordingState.state

    fun startTrip() = LocationTrackingService.start(getApplication())

    fun stopTrip() = LocationTrackingService.stop(getApplication())
}
