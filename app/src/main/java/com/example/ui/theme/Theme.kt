package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF26B6AF),
    onPrimary = Color(0xFF002927),
    primaryContainer = Color(0xFF004E4A),
    onPrimaryContainer = Color(0xFFB4F0EC),
    secondary = Color(0xFF62AAA6),
    onSecondary = Color(0xFF002927),
    secondaryContainer = Color(0xFF173836),
    onSecondaryContainer = Color(0xFFD5F2F0),
    tertiary = Color(0xFFFF8A98),
    onTertiary = Color(0xFF4C000E),
    tertiaryContainer = Color(0xFF731826),
    onTertiaryContainer = Color(0xFFFFD9DD),
    error = Color(0xFFFF8A98),
    errorContainer = Color(0xFF5C1622),
    onErrorContainer = Color(0xFFFFD9DD),
    background = BgSurfaceDark,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardSurfaceAltDark,
    onSurfaceVariant = TextMutedDark,
    outline = BorderLineDark
)

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = BrandTealSoft,
    onPrimaryContainer = BrandTealDeep,
    secondary = BrandTealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF2F0),
    onSecondaryContainer = BrandTealDeep,
    tertiary = AlertCrimson,
    onTertiary = Color.White,
    tertiaryContainer = AlertCrimsonSoft,
    onTertiaryContainer = AlertCrimsonDark,
    error = AlertCrimson,
    errorContainer = AlertCrimsonSoft,
    onErrorContainer = AlertCrimsonDark,
    background = BgSurfaceLight,
    onBackground = TextPrimaryLight,
    surface = CardSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardSurfaceAltLight,
    onSurfaceVariant = TextMutedLight,
    outline = BorderLineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
