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

    /** Color used for segments filtered out of the current selection. */
    val DimColor = Color(0xFF3A3D45)

    fun bandIndexFor(speedMps: Float): Int {
        val kmh = speedMps * 3.6f
        val idx = bands.indexOfFirst { kmh < it.maxKmh }
        return if (idx < 0) bands.lastIndex else idx
    }

    fun colorFor(speedMps: Float): Color = bands[bandIndexFor(speedMps)].color

    /** Which bands actually occur in this trip (drives enabling legend segments). */
    fun presentBands(points: List<LocationPointEntity>): Set<Int> {
        if (points.size < 2) return emptySet()
        return segmentBandIndices(points).toHashSet()
    }

    /** Points fewer than this on each side are averaged in to de-jitter the color. */
    private const val SMOOTH_WINDOW = 5

    /**
     * Builds the per-segment color spans for a [com.google.maps.android.compose.Polyline].
     * Consecutive segments in the same displayed color are merged into one
     * [StyleSpan] (run-length encoding), so a 6000-point ride yields tens of
     * spans, not thousands. [points] must already be ordered by time.
     *
     * When [selected] is non-empty, only segments whose band is selected keep
     * their color; the rest are dimmed so the map reads as a filter while still
     * showing the full route for context. Empty [selected] shows every band.
     */
    fun speedSpans(
        points: List<LocationPointEntity>,
        selected: Set<Int> = emptySet(),
    ): List<StyleSpan> {
        if (points.size < 2) return emptyList()
        val indices = segmentBandIndices(points)

        val spans = ArrayList<StyleSpan>()
        var runColor = 0
        var runLength = 0.0
        for (bandIdx in indices) {
            val shown = selected.isEmpty() || bandIdx in selected
            val argb = (if (shown) bands[bandIdx].color else DimColor).toArgb()
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

    /** Band index of every segment (between consecutive points), speed-smoothed. */
    private fun segmentBandIndices(points: List<LocationPointEntity>): IntArray {
        val speeds = smooth(points.map { it.speedMps })
        return IntArray(points.size - 1) { i ->
            bandIndexFor((speeds[i] + speeds[i + 1]) / 2f)
        }
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
