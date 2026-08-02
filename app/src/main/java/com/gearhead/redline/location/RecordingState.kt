package com.gearhead.redline.location

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide bridge between the tracking service (writer) and the cockpit UI
 * (reader). A singleton because the foreground service and the Compose UI live
 * in the same process but have no direct reference to each other.
 *
 * This is deliberately lightweight (no persistence); the durable record is the
 * Room trip row. If the process is killed and restarted, this resets to Idle
 * while the service, if still alive, republishes on its next fix.
 */
object RecordingState {

    private val _state = MutableStateFlow(LiveTripState.Idle)
    val state: StateFlow<LiveTripState> = _state.asStateFlow()

    val isRecording: Boolean get() = _state.value.isRecording

    fun update(newState: LiveTripState) {
        _state.value = newState
    }

    fun reset() {
        _state.value = LiveTripState.Idle
    }
}
