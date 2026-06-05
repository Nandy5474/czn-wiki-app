package com.cznwiki.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9C7CF4),
    secondary = Color(0xFF5B8DEF),
    tertiary = Color(0xFF4ECDC4),
    background = Color(0xFF0D0D1A),
    surface = Color(0xFF1A1A2E),
    surfaceVariant = Color(0xFF252540),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFF0D0D1A),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFFB0B0C0),
    outline = Color(0xFF3A3A55),
    primaryContainer = Color(0xFF2D1F5E),
    secondaryContainer = Color(0xFF1A3A6E),
    tertiaryContainer = Color(0xFF0D3B42),
    error = Color(0xFFCF6679),
    onError = Color(0xFF1C1C1C),
)

@Composable
fun CznWikiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
