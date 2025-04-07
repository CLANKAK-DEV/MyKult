package com.example.mykultv2

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val CultureColorScheme = lightColorScheme(
    primary = Color(0xFF6200EE),        // A soft purple - modern and creative
    secondary = Color(0xFF03DAC6),      // Teal - fresh and friendly
    background = Color(0xFFF2F2F2),     // Light gray - neutral and soft
    surface = Color(0xFFFFFFFF),        // White - clean content area

    onPrimary = Color(0xFFFFFFFF),      // White on purple
    onSecondary = Color(0xFF000000),    // Black on teal
    onBackground = Color(0xFF121212),   // Dark gray text for readability
    onSurface = Color(0xFF121212)       // Dark gray text on white
)

@Composable
fun CultureAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CultureColorScheme,
        content = content
    )
}