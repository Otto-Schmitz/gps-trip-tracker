package com.gearhead.redline.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val RedlineColorScheme = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Redline,
    onSecondary = TextPrimary,
    tertiary = Ignition,
    background = Ink,
    onBackground = TextPrimary,
    surface = Panel,
    onSurface = TextPrimary,
    surfaceVariant = PanelElevated,
    onSurfaceVariant = TextSecondary,
    outline = Divider,
    error = Redline,
)

/**
 * The app is dark-only by design (cockpit aesthetic); [darkTheme] is accepted for
 * previews but the scheme stays dark regardless.
 */
@Composable
fun RedlineTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Ink.toArgb()
            window.navigationBarColor = Ink.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = RedlineColorScheme,
        typography = RedlineTypography,
        content = content,
    )
}
