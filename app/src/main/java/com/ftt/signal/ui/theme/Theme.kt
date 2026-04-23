package com.ftt.signal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF6C8EFF),
    secondary = Color(0xFF9B6CFF),
    tertiary = Color(0xFF34D97B),
    background = Color(0xFF08090F),
    surface = Color(0xFF13141F),
    surfaceVariant = Color(0xFF1A1B27),
    onBackground = Color(0xFFE8E9F6),
    onSurface = Color(0xFFE8E9F6),
    onSurfaceVariant = Color(0xFF9B9CB8),
    error = Color(0xFFFF5370)
)

@Composable
fun FttSignalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        content = content
    )
}
