package com.gearhead.redline.location

/** Tunables for GPS collection, noise filtering and the moving/stopped split. */
object TrackingConfig {

    /** Target interval between location updates, ms (high-precision tracking). */
    const val UPDATE_INTERVAL_MS = 1_000L

    /** Fastest interval we'll accept updates at, ms. */
    const val FASTEST_INTERVAL_MS = 1_000L

    /** Reject fixes less accurate than this (meters). Loose enough for canyons/tunnels edges. */
    const val MAX_ACCURACY_METERS = 30f

    /**
     * Reject a fix whose implied speed from the previous point exceeds this (m/s).
     * ~110 m/s ≈ 396 km/h — beyond any street vehicle, so it's a GPS jump.
     */
    const val MAX_PLAUSIBLE_SPEED_MPS = 110.0

    /**
     * Ignore sub-meter jitter: if two consecutive fixes are closer than this and
     * the reported speed is near zero, treat displacement as noise (meters).
     */
    const val MIN_DISPLACEMENT_METERS = 1.5

    /** At/above this speed the vehicle counts as "moving" (m/s ≈ 3.6 km/h). */
    const val MOVING_SPEED_THRESHOLD_MPS = 1.0

    /**
     * Guard against a single huge dt (e.g. GPS lost for minutes) inflating moving
     * time or distance: cap the gap credited between two accepted points, ms.
     */
    const val MAX_SEGMENT_GAP_MS = 10_000L
}
