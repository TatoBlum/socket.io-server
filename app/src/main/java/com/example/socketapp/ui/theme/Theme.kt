package com.example.socketapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = GaliciaPrimary,
    onPrimary = GaliciaOnPrimary,
    primaryContainer = GaliciaPrimaryContainer,
    onPrimaryContainer = GaliciaOnPrimaryContainer,
    secondary = GaliciaSecondary,
    onSecondary = GaliciaOnSecondary,
    secondaryContainer = GaliciaSecondaryContainer,
    onSecondaryContainer = GaliciaOnSecondaryContainer,
    tertiary = GaliciaTertiary,
    onTertiary = GaliciaOnTertiary,
    tertiaryContainer = GaliciaTertiaryContainer,
    onTertiaryContainer = GaliciaOnTertiaryContainer,
    background = GaliciaBackground,
    onBackground = GaliciaOnBackground,
    surface = GaliciaSurface,
    onSurface = GaliciaOnSurface,
    surfaceVariant = GaliciaSurfaceVariant,
    onSurfaceVariant = GaliciaOnSurfaceVariant,
    outline = GaliciaOutline,
    outlineVariant = GaliciaOutlineVariant,
    error = GaliciaError,
    onError = GaliciaOnError,
    errorContainer = GaliciaErrorContainer,
    onErrorContainer = GaliciaOnErrorContainer,
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
