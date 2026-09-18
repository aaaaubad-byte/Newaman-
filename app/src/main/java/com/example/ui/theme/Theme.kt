package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AmanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

private val AmanLightColorScheme = lightColorScheme(
    primary = AmanTealDark,
    onPrimary = Color.White,
    primaryContainer = AmanTealLight.copy(alpha = 0.4f),
    onPrimaryContainer = AmanTealDark,
    secondary = AmanTealPrimary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = AmanTealDark,
    tertiary = AmanTealLight,
    onTertiary = AmanDarkSlate,
    background = AmanBgLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFF5F7FA),
    onSurfaceVariant = TextSecondary,
    outline = BorderField,
    outlineVariant = BorderSubtle
)

private val AmanDarkColorScheme = darkColorScheme(
    primary = AmanTealPrimary,
    onPrimary = Color.White,
    primaryContainer = AmanTealDark,
    onPrimaryContainer = AmanTealLight,
    secondary = AmanTealLight,
    onSecondary = AmanDarkSlate,
    background = Color(0xFF172326),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF1E2E31),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF24363B),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF37474F),
    outlineVariant = Color(0xFF263238)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // Always enforce AMAN's brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) AmanDarkColorScheme else AmanLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AmanShapes,
        typography = Typography,
        content = content
    )
}
