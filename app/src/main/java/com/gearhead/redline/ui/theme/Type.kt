package com.gearhead.redline.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Monospace for numeric readouts (odometer/digital-gauge feel). Swap
 * [FontFamily.Monospace] for a bundled font (e.g. a DIN/7-segment face placed in
 * res/font) here if you want a more authentic dashboard look.
 */
val NumberFontFamily = FontFamily.Monospace

/** Big centered speed value in the cockpit. */
val CockpitSpeedStyle = TextStyle(
    fontFamily = NumberFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 120.sp,
    letterSpacing = (-2).sp,
)

/** Secondary numeric readouts (distance, time, metric tiles). */
val MetricValueStyle = TextStyle(
    fontFamily = NumberFontFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 28.sp,
)

val RedlineTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.2.sp,
    ),
)
