package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NeuraDarkColorScheme = darkColorScheme(
    primary = NeuraCyan,
    onPrimary = NeuraCosmicBlack,
    primaryContainer = NeuraCardDarkElevated,
    onPrimaryContainer = NeuraCyan,
    secondary = NeuraPurple,
    onSecondary = Color.White,
    secondaryContainer = NeuraBorderDark,
    onSecondaryContainer = NeuraPurple,
    tertiary = NeuraBlue,
    onTertiary = Color.White,
    background = NeuraCosmicBlack,
    onBackground = NeuraTextPrimaryDark,
    surface = NeuraSurfaceDark,
    onSurface = NeuraTextPrimaryDark,
    surfaceVariant = NeuraCardDark,
    onSurfaceVariant = NeuraTextSecondaryDark,
    outline = NeuraBorderDark,
    error = NeuraRose,
    onError = Color.White
)

val NeuraLightColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = NeuraPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFF0284C7),
    onTertiary = Color.White,
    background = NeuraCosmicLight,
    onBackground = NeuraTextPrimaryLight,
    surface = NeuraSurfaceLight,
    onSurface = NeuraTextPrimaryLight,
    surfaceVariant = NeuraCardLight,
    onSurfaceVariant = NeuraTextSecondaryLight,
    outline = NeuraBorderLight,
    error = NeuraRose,
    onError = Color.White
)

@Composable
fun NeuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NeuraDarkColorScheme else NeuraLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
