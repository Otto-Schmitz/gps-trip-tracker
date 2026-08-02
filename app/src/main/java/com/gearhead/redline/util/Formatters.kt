package com.gearhead.redline.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Edge formatting: the app stores SI (m/s, meters, millis) and converts to
 * gearhead-friendly units only for display. Metric (km/h, km) by default.
 */
object Formatters {

    private const val MPS_TO_KMH = 3.6

    fun speedKmh(speedMps: Number): Int =
        (speedMps.toDouble() * MPS_TO_KMH).toInt().coerceAtLeast(0)

    /** "0.0" style km with one decimal, or meters below 1 km. */
    fun distance(meters: Double): String {
        return if (meters >= 1000.0) {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        } else {
            String.format(Locale.US, "%d m", meters.toInt())
        }
    }

    /** Distance value only (no unit), for cockpit tiles where the unit is separate. */
    fun distanceValue(meters: Double): String =
        if (meters >= 1000.0) String.format(Locale.US, "%.1f", meters / 1000.0)
        else meters.toInt().toString()

    fun distanceUnit(meters: Double): String = if (meters >= 1000.0) "km" else "m"

    /** "H:MM:SS" once past an hour, otherwise "MM:SS". */
    fun duration(millis: Long): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(millis.coerceAtLeast(0))
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private val dateFormat = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())

    fun dateTime(epochMillis: Long): String = dateFormat.format(Date(epochMillis))
}
