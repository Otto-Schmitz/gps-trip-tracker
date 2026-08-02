package com.gearhead.redline.ui.permissions

import android.Manifest
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Bundles the runtime location permission flow. Android's rules force a two-step
 * request: foreground (fine/coarse) first, then background ("Allow all the time")
 * — on Android 11+ the background grant can only be made from system settings,
 * which [backgroundLocation]'s launcher opens.
 */
@OptIn(ExperimentalPermissionsApi::class)
class LocationPermissionsState(
    val foreground: MultiplePermissionsState,
    val backgroundLocation: PermissionState?,
    val postNotifications: PermissionState?,
) {
    val fineGranted: Boolean
        get() = foreground.permissions
            .firstOrNull { it.permission == Manifest.permission.ACCESS_FINE_LOCATION }
            ?.status?.let { it.isGrantedCompat } ?: false

    val backgroundGranted: Boolean
        get() = backgroundLocation?.status?.isGrantedCompat ?: true // pre-Q: implied by fine

    /** True when we can record but risk interruption because background is denied. */
    val backgroundMissing: Boolean
        get() = fineGranted && !backgroundGranted

    fun requestForeground() = foreground.launchMultiplePermissionRequest()

    /** Launches the background-location request (opens settings on Android 11+). */
    fun requestBackground() {
        backgroundLocation?.launchPermissionRequest()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private val com.google.accompanist.permissions.PermissionStatus.isGrantedCompat: Boolean
    get() = this is com.google.accompanist.permissions.PermissionStatus.Granted

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun rememberLocationPermissionsState(): LocationPermissionsState {
    val foreground = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    )

    val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null

    val notifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    return remember(foreground, background, notifications) {
        LocationPermissionsState(foreground, background, notifications)
    }
}
