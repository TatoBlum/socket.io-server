package com.example.socketapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = SoftSkyPrimary,
    onPrimary = SoftSkyOnPrimary,
    primaryContainer = SoftSkyPrimaryContainer,
    onPrimaryContainer = SoftSkyOnPrimaryContainer,
    secondary = SoftSkySecondary,
    onSecondary = SoftSkyOnSecondary,
    secondaryContainer = SoftSkySecondaryContainer,
    onSecondaryContainer = SoftSkyOnSecondaryContainer,
    tertiary = SoftSkyTertiary,
    onTertiary = SoftSkyOnTertiary,
    tertiaryContainer = SoftSkyTertiaryContainer,
    onTertiaryContainer = SoftSkyOnTertiaryContainer,
    background = SoftSkyBackground,
    onBackground = SoftSkyOnBackground,
    surface = SoftSkySurface,
    onSurface = SoftSkyOnSurface,
    surfaceVariant = SoftSkySurfaceVariant,
    onSurfaceVariant = SoftSkyOnSurfaceVariant,
    outline = SoftSkyOutline,
    outlineVariant = SoftSkyOutlineVariant,
    error = SoftSkyError,
    onError = SoftSkyOnError,
    errorContainer = SoftSkyErrorContainer,
    onErrorContainer = SoftSkyOnErrorContainer,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view)
                .isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
