package com.example.mykultv2

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the light color scheme
val LightCultureColorScheme = lightColorScheme(
    primary = Color(0x80000000),        // Semi-transparent black (50% opacity)
    secondary = Color(0x4D000000),      // Lighter transparent black (30% opacity)
    background = Color.Transparent,     // Fully transparent background
    surface = Color(0xFFFFFFFF),        // Solid white for content areas
    onPrimary = Color(0xFFFFFFFF),      // White text on primary
    onSecondary = Color(0xFFFFFFFF),    // White text on secondary
    onBackground = Color(0xFF000000),   // Black text on transparent
    onSurface = Color(0xFF000000)       // Black text on white
)

// Define the dark color scheme
val DarkCultureColorScheme = darkColorScheme(
    primary = Color(0x80FFFFFF),        // Semi-transparent white (50% opacity)
    secondary = Color(0x4DFFFFFF),      // Lighter transparent white (30% opacity)
    background = Color(0xFF121212),     // Dark background
    surface = Color(0xFF1E1E1E),        // Dark surface for content areas
    onPrimary = Color(0xFF000000),      // Black text on primary
    onSecondary = Color(0xFF000000),    // Black text on secondary
    onBackground = Color(0xFFFFFFFF),   // White text on dark background
    onSurface = Color(0xFFFFFFFF)       // White text on dark surface
)

@Composable
fun CultureAppTheme(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) DarkCultureColorScheme else LightCultureColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}