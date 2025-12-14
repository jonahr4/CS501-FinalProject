package com.example.cs501_mealmapproject.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = DarkNaturalGreen,
    onPrimary = OffWhite,
    primaryContainer = DarkNaturalGreen,
    onPrimaryContainer = OffWhite,
    secondary = AccentYellow,
    onSecondary = DarkCharcoal,
    secondaryContainer = DarkNaturalGreen, 
    onSecondaryContainer = OffWhite,
    background = DarkBackground,
    onBackground = LightGrayText,
    surface = DarkSurface,
    onSurface = LightGrayText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = LightGrayText,
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = NaturalGreen,
    onPrimary = OffWhite,
    primaryContainer = LightMintContainer,
    onPrimaryContainer = DarkNaturalGreen,
    secondary = AccentYellow,
    onSecondary = DarkCharcoal,
    secondaryContainer = NaturalGreen, 
    onSecondaryContainer = OffWhite,   
    background = LightBackground,
    onBackground = DarkCharcoal,
    surface = OffWhite,
    onSurface = DarkCharcoal,
    surfaceVariant = LightMintContainer, 
    onSurfaceVariant = DarkCharcoal,     
    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun CS501MealMapProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use the surface color for status bar to match the app's background
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
