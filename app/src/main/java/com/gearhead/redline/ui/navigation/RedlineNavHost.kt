package com.gearhead.redline.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gearhead.redline.ui.detail.TripDetailScreen
import com.gearhead.redline.ui.detail.TripDetailViewModel
import com.gearhead.redline.ui.history.HistoryScreen
import com.gearhead.redline.ui.record.RecordScreen

/** Routes for the three MVP screens. */
private object Routes {
    const val RECORD = "record"
    const val HISTORY = "history"
    const val TRIP_DETAIL = "trip/{${TripDetailViewModel.ARG_TRIP_ID}}"
    fun tripDetail(tripId: Long) = "trip/$tripId"
}

@Composable
fun RedlineNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.RECORD,
        modifier = modifier,
    ) {
        composable(Routes.RECORD) {
            RecordScreen(
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onBack = { navController.popBackStack() },
                onOpenTrip = { tripId -> navController.navigate(Routes.tripDetail(tripId)) },
            )
        }

        composable(
            route = Routes.TRIP_DETAIL,
            arguments = listOf(
                navArgument(TripDetailViewModel.ARG_TRIP_ID) { type = NavType.LongType },
            ),
        ) {
            TripDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
