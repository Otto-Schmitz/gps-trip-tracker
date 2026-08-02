package com.gearhead.redline.ui.record

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gearhead.redline.location.LiveTripState
import com.gearhead.redline.ui.components.MetricRow
import com.gearhead.redline.ui.components.MetricTile
import com.gearhead.redline.ui.components.SectionLabel
import com.gearhead.redline.ui.permissions.rememberLocationPermissionsState
import com.gearhead.redline.ui.theme.Amber
import com.gearhead.redline.ui.theme.CockpitSpeedStyle
import com.gearhead.redline.ui.theme.Ignition
import com.gearhead.redline.ui.theme.Ink
import com.gearhead.redline.ui.theme.Redline
import com.gearhead.redline.ui.theme.TextMuted
import com.gearhead.redline.ui.theme.TextPrimary
import com.gearhead.redline.ui.theme.TextSecondary
import com.gearhead.redline.util.Formatters

@Composable
fun RecordScreen(
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordViewModel = viewModel(),
) {
    val context = LocalContext.current
    val permissions = rememberLocationPermissionsState()
    val live by viewModel.liveState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Ink)
            .safeDrawingPadding()
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(onOpenHistory = onOpenHistory)

            if (!permissions.fineGranted) {
                Spacer(Modifier.height(24.dp))
                PermissionGate(onRequest = { permissions.requestForeground() })
                return@Column
            }

            if (permissions.backgroundMissing) {
                Spacer(Modifier.height(12.dp))
                BackgroundWarning(onEnable = { permissions.requestBackground() })
            }

            SpeedCluster(live = live, modifier = Modifier.weight(1f))

            SecondaryMetrics(live = live)

            Spacer(Modifier.height(20.dp))

            StartStopButton(
                isRecording = live.isRecording,
                onStart = {
                    viewModel.startTrip()
                    if (permissions.backgroundMissing) {
                        Toast.makeText(context, "Recording — keep the app open for best results", Toast.LENGTH_LONG).show()
                    }
                },
                onStop = { viewModel.stopTrip() },
            )
        }
    }
}

@Composable
private fun TopBar(onOpenHistory: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "REDLINE",
            color = Amber,
            fontWeight = FontWeight.Black,
            style = com.gearhead.redline.ui.theme.RedlineTypography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onOpenHistory) {
            Icon(Icons.Filled.History, contentDescription = "History", tint = TextSecondary)
        }
    }
}

@Composable
private fun SpeedCluster(live: LiveTripState, modifier: Modifier = Modifier) {
    val kmh = Formatters.speedKmh(live.currentSpeedMps)
    // Warm toward red as speed climbs — a soft "redline" cue.
    val speedColor = when {
        !live.isRecording -> TextMuted
        kmh >= 120 -> Redline
        kmh >= 80 -> Amber
        else -> TextPrimary
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionLabel(
            text = when {
                live.isRecording && !live.hasFix -> "Acquiring GPS"
                live.isRecording -> "Recording"
                else -> "Ready"
            },
            color = if (live.isRecording && !live.hasFix) Amber else TextMuted,
        )
        Text(
            text = kmh.toString(),
            style = CockpitSpeedStyle,
            color = speedColor,
            textAlign = TextAlign.Center,
        )
        Text(text = "km/h", color = TextSecondary, style = com.gearhead.redline.ui.theme.RedlineTypography.titleLarge)
    }
}

@Composable
private fun SecondaryMetrics(live: LiveTripState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricRow(
            left = { m ->
                MetricTile(
                    label = "Distance",
                    value = Formatters.distanceValue(live.distanceMeters),
                    unit = Formatters.distanceUnit(live.distanceMeters),
                    modifier = m,
                )
            },
            right = { m ->
                MetricTile(
                    label = "Time",
                    value = Formatters.duration(live.elapsedMillis),
                    unit = "",
                    modifier = m,
                )
            },
        )
        MetricRow(
            left = { m ->
                MetricTile(
                    label = "Top speed",
                    value = Formatters.speedKmh(live.maxSpeedMps).toString(),
                    unit = "km/h",
                    valueColor = Redline,
                    modifier = m,
                )
            },
            right = { m ->
                MetricTile(
                    label = "Avg (moving)",
                    value = Formatters.speedKmh(live.avgSpeedMps).toString(),
                    unit = "km/h",
                    modifier = m,
                )
            },
        )
    }
}

@Composable
private fun StartStopButton(
    isRecording: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Button(
        onClick = { if (isRecording) onStop() else onStart() },
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) Redline else Ignition,
            contentColor = Ink,
        ),
    ) {
        Icon(
            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.PlayArrow,
            contentDescription = null,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (isRecording) "STOP TRIP" else "START TRIP",
            fontWeight = FontWeight.Black,
            color = Ink,
        )
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(40.dp))
        Text(
            text = "Location needed",
            color = TextPrimary,
            style = com.gearhead.redline.ui.theme.RedlineTypography.headlineMedium,
        )
        Text(
            text = "Redline records your rides with precise GPS. Grant location access to start a trip.",
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Amber, contentColor = Ink),
        ) {
            Text("Grant location", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BackgroundWarning(onEnable: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33FFB300))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Background location off — tracking may pause when the screen locks. Tap to allow “Always”.",
            color = TextPrimary,
            style = com.gearhead.redline.ui.theme.RedlineTypography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp)),
        )
        androidx.compose.material3.TextButton(onClick = onEnable) {
            Text("Allow", color = Amber, fontWeight = FontWeight.Bold)
        }
    }
}
