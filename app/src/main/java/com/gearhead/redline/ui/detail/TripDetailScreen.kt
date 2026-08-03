package com.gearhead.redline.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.gearhead.redline.data.local.entity.TripWithPoints
import com.gearhead.redline.ui.components.MetricRow
import com.gearhead.redline.ui.components.MetricTile
import com.gearhead.redline.ui.components.SectionLabel
import com.gearhead.redline.ui.theme.Amber
import com.gearhead.redline.ui.theme.Ink
import com.gearhead.redline.ui.theme.Panel
import com.gearhead.redline.ui.theme.Redline
import com.gearhead.redline.ui.theme.TextMuted
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
            // Sort once and share with the legend so "present" bands and the route
            // coloring stay in sync. Selected speed bands filter the map overlay.
            val orderedPoints = remember(data) { data.points.sortedBy { it.timestamp } }
            val presentBands = remember(orderedPoints) { SpeedGradient.presentBands(orderedPoints) }
            var selectedBands by remember(data.trip.id) { mutableStateOf(emptySet<Int>()) }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                RouteMap(orderedPoints = orderedPoints, selectedBands = selectedBands)
                if (orderedPoints.isNotEmpty()) {
                    SpeedLegend(
                        present = presentBands,
                        selected = selectedBands,
                        onToggle = { i ->
                            selectedBands = if (i in selectedBands) selectedBands - i else selectedBands + i
                        },
                        onClear = { selectedBands = emptySet() },
                    )
                }
                Stats(data)
            }
        }
    }
}

@Composable
private fun RouteMap(orderedPoints: List<LocationPointEntity>, selectedBands: Set<Int>) {
    val context = LocalContext.current
    val points = remember(orderedPoints) { orderedPoints.map { LatLng(it.latitude, it.longitude) } }
    // Recompute the colored spans only when the trip or the active filter changes.
    val spans = remember(orderedPoints, selectedBands) {
        SpeedGradient.speedSpans(orderedPoints, selectedBands)
    }

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
            // Route colored by speed band (cool = slow, red = fast); non-selected
            // bands dim to a gray when a filter is active.
            Polyline(points = points, spans = spans, width = 14f)

            Marker(state = rememberMarkerState(key = "start", position = points.first()), title = "Start")
            Marker(state = rememberMarkerState(key = "end", position = points.last()), title = "End")

            // Highlight the fastest point of the ride.
            maxSpeedPoint(orderedPoints)?.let { fastest ->
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

/**
 * Full-width, interactive speed scale: the six band colors as one continuous
 * segmented bar with the km/h range under each segment. Uses equal weights so it
 * always fits edge-to-edge (no scroll/clipping). Tapping a segment toggles it as
 * a map filter; bands absent from the trip are disabled.
 */
@Composable
private fun SpeedLegend(
    present: Set<Int>,
    selected: Set<Int>,
    onToggle: (Int) -> Unit,
    onClear: () -> Unit,
) {
    val filterActive = selected.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Speed · km/h")
            if (filterActive) {
                Text(
                    text = "Clear filter",
                    modifier = Modifier.clickable(onClick = onClear),
                    color = Amber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            } else {
                Text(text = "tap a band to filter", color = TextMuted, fontSize = 10.sp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp)),
        ) {
            SpeedGradient.bands.forEachIndexed { i, band ->
                val isPresent = i in present
                val isSelected = i in selected
                val dimmed = filterActive && !isSelected
                val alpha = when {
                    !isPresent -> 0.20f
                    dimmed -> 0.28f
                    else -> 1f
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(band.color.copy(alpha = alpha))
                        .then(if (isPresent) Modifier.clickable { onToggle(i) } else Modifier),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            SpeedGradient.bands.forEachIndexed { i, band ->
                val isPresent = i in present
                val isSelected = i in selected
                val dimmed = filterActive && !isSelected
                Text(
                    text = band.label,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (dimmed) 0.5f else 1f),
                    color = when {
                        !isPresent -> TextMuted
                        isSelected -> band.color
                        else -> TextSecondary
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
