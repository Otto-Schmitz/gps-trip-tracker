package com.gearhead.redline.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gearhead.redline.data.local.entity.TripEntity
import com.gearhead.redline.ui.components.SectionLabel
import com.gearhead.redline.ui.theme.Amber
import com.gearhead.redline.ui.theme.Ink
import com.gearhead.redline.ui.theme.NumberFontFamily
import com.gearhead.redline.ui.theme.Panel
import com.gearhead.redline.ui.theme.Redline
import com.gearhead.redline.ui.theme.TextPrimary
import com.gearhead.redline.ui.theme.TextSecondary
import com.gearhead.redline.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenTrip: (Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = { Text("Trips", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
            )
        },
    ) { padding ->
        if (trips.isEmpty()) {
            EmptyState(Modifier.fillMaxSize().padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripCard(trip = trip, onClick = { onOpenTrip(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: TripEntity, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = Formatters.dateTime(trip.startedAt),
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CardStat("Distance", Formatters.distance(trip.distanceMeters), TextPrimary)
            CardStat("Top", "${Formatters.speedKmh(trip.maxSpeedMps)} km/h", Redline)
            CardStat("Avg", "${Formatters.speedKmh(trip.avgSpeedMps)} km/h", Amber)
            CardStat("Time", Formatters.duration(trip.durationMillis), TextPrimary)
        }
    }
}

@Composable
private fun CardStat(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.Start) {
        SectionLabel(label)
        Text(
            text = value,
            color = valueColor,
            fontFamily = NumberFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No trips yet", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Hit START TRIP to record your first ride.", color = TextSecondary)
        }
    }
}
