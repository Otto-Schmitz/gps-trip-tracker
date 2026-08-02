package com.gearhead.redline.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.local.entity.TripWithPoints
import com.gearhead.redline.ui.components.MetricRow
import com.gearhead.redline.ui.components.MetricTile
import com.gearhead.redline.ui.theme.Amber
import com.gearhead.redline.ui.theme.Ink
import com.gearhead.redline.ui.theme.Panel
import com.gearhead.redline.ui.theme.Redline
import com.gearhead.redline.ui.theme.TextPrimary
import com.gearhead.redline.ui.theme.TextSecondary
import com.gearhead.redline.util.Formatters
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.gearhead.redline.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = viewModel(),
) {
    val tripWithPoints by viewModel.trip.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = { Text("Trip detail", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
            )
        },
    ) { padding ->
        val data = tripWithPoints
        if (data == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = TextSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RouteMap(data)
                Stats(data)
            }
        }
    }
}

@Composable
private fun RouteMap(data: TripWithPoints) {
    val context = LocalContext.current
    val points = data.points.map { LatLng(it.latitude, it.longitude) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Panel),
    ) {
        if (points.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No route recorded", color = TextSecondary)
            }
            return@Box
        }

        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(points.first(), 14f)
        }

        // Once the map is laid out, frame the whole route.
        LaunchedEffect(points) {
            if (points.size >= 2) {
                val bounds = LatLngBounds.builder().apply { points.forEach(::include) }.build()
                runCatching {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 96))
                }
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = MapType.NORMAL,
                mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark),
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false),
        ) {
            Polyline(points = points, color = Amber, width = 12f)

            Marker(state = rememberMarkerState(key = "start", position = points.first()), title = "Start")
            Marker(state = rememberMarkerState(key = "end", position = points.last()), title = "End")

            // Highlight the fastest point of the ride.
            maxSpeedPoint(data.points)?.let { fastest ->
                Marker(
                    state = rememberMarkerState(
                        key = "top",
                        position = LatLng(fastest.latitude, fastest.longitude),
                    ),
                    title = "Top speed",
                    snippet = "${Formatters.speedKmh(fastest.speedMps)} km/h",
                )
            }
        }
    }
}

@Composable
private fun Stats(data: TripWithPoints) {
    val trip = data.trip
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = Formatters.dateTime(trip.startedAt),
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        MetricRow(
            left = { m ->
                MetricTile("Top speed", Formatters.speedKmh(trip.maxSpeedMps).toString(), "km/h", m, valueColor = Redline)
            },
            right = { m ->
                MetricTile("Avg (moving)", Formatters.speedKmh(trip.avgSpeedMps).toString(), "km/h", m, valueColor = Amber)
            },
        )
        MetricRow(
            left = { m ->
                MetricTile("Distance", Formatters.distanceValue(trip.distanceMeters), Formatters.distanceUnit(trip.distanceMeters), m)
            },
            right = { m ->
                MetricTile("Duration", Formatters.duration(trip.durationMillis), "", m)
            },
        )
        MetricRow(
            left = { m ->
                MetricTile("Moving time", Formatters.duration(trip.movingMillis), "", m)
            },
            right = { m ->
                MetricTile("GPS points", trip.pointCount.toString(), "", m)
            },
        )
    }
}

private fun maxSpeedPoint(points: List<LocationPointEntity>): LocationPointEntity? =
    points.maxByOrNull { it.speedMps }
