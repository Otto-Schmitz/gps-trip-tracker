package com.gearhead.redline.ui.detail

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.gearhead.redline.data.local.entity.LocationPointEntity
import com.google.android.gms.maps.model.StyleSpan

/**
 * Maps instantaneous speed to a color for the route overlay. Bands are fixed in
 * km/h (not relative to the trip) so a color means the same speed across every
 * trip, and the palette runs cool→hot for an intuitive "cold = slow, red = flat
 * out" read on the dark map.
 */
object SpeedGradient {

    data class Band(
        /** Exclusive upper bound in km/h; the last band is the open-ended ceiling. */
        val maxKmh: Int,
        val color: Color,
        val label: String,
    )

    val bands: List<Band> = listOf(
        Band(30, Color(0xFF2E7DF6), "0–30"),
        Band(60, Color(0xFF17C3B2), "30–60"),
        Band(90, Color(0xFF7FD13B), "60–90"),
        Band(120, Color(0xFFFFB300), "90–120"),
        Band(150, Color(0xFFFF6D00), "120–150"),
        Band(Int.MAX_VALUE, Color(0xFFE53935), "150+"),
    )

    fun colorFor(speedMps: Float): Color {
        val kmh = speedMps * 3.6f
        return bands.first { kmh < it.maxKmh }.color
    }

    /** Points fewer than this on each side are averaged in to de-jitter the color. */
    private const val SMOOTH_WINDOW = 5

    /**
     * Builds the per-segment color spans for a [com.google.maps.android.compose.Polyline].
     * Consecutive segments in the same band are merged into one [StyleSpan]
     * (run-length encoding), so a 6000-point ride yields tens of spans, not
     * thousands. [points] must already be ordered by time.
     */
    fun speedSpans(points: List<LocationPointEntity>): List<StyleSpan> {
        if (points.size < 2) return emptyList()
        val speeds = smooth(points.map { it.speedMps })

        val spans = ArrayList<StyleSpan>()
        var runColor = 0
        var runLength = 0.0
        for (i in 0 until points.size - 1) {
            val segmentSpeed = (speeds[i] + speeds[i + 1]) / 2f
            val argb = colorFor(segmentSpeed).toArgb()
            when {
                runLength == 0.0 -> {
                    runColor = argb
                    runLength = 1.0
                }
                argb == runColor -> runLength += 1.0
                else -> {
                    spans.add(StyleSpan(runColor, runLength))
                    runColor = argb
                    runLength = 1.0
                }
            }
        }
        if (runLength > 0.0) spans.add(StyleSpan(runColor, runLength))
        return spans
    }

    private fun smooth(values: List<Float>): FloatArray {
        val n = values.size
        val out = FloatArray(n)
        val half = SMOOTH_WINDOW / 2
        for (i in 0 until n) {
            var sum = 0f
            var count = 0
            for (j in (i - half)..(i + half)) {
                if (j in 0 until n) {
                    sum += values[j]
                    count++
                }
            }
            out[i] = if (count > 0) sum / count else values[i]
        }
        return out
    }
}
