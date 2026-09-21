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

// Official Aman Master UI Shapes
private val AmanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// Official Aman Light Color Scheme (Enforced for both Customer & In-App Ops)
private val AmanLightColorScheme = lightColorScheme(
    primary = AmanTealDark,
    onPrimary = Color.White,
    primaryContainer = AmanTealLight,
    onPrimaryContainer = AmanTealDark,
    secondary = AmanTealPrimary,
    onSecondary = Color.White,
    secondaryContainer = AmanTealLight,
    onSecondaryContainer = AmanTealDark,
    tertiary = AmanTealAccent,
    onTertiary = AmanDarkSlate,
    background = AmanBgLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = AmanTealLight,
    onSurfaceVariant = TextSecondary,
    outline = BorderField,
    outlineVariant = BorderSubtle
)

private val AmanDarkColorScheme = darkColorScheme(
    primary = AmanTealPrimary,
    onPrimary = Color.White,
    primaryContainer = AmanTealDark,
    onPrimaryContainer = AmanTealLight,
    secondary = AmanTealAccent,
    onSecondary = AmanDarkSlate,
    background = Color(0xFF12201E),
    onBackground = Color(0xFFECEFF1),
    surface = Color(0xFF182A27),
    onSurface = Color(0xFFECEFF1),
    surfaceVariant = Color(0xFF1E3531),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline = Color(0xFF2C4843),
    outlineVariant = Color(0xFF223A36)
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
