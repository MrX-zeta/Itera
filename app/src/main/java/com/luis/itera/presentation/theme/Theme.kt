package com.luis.itera.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IteraColorScheme = darkColorScheme(
    primary = IteraColors.Accent,
    onPrimary = IteraColors.OnAccent,
    background = IteraColors.Background,
    onBackground = IteraColors.TextPrimary,
    surface = IteraColors.Surface,
    onSurface = IteraColors.TextPrimary,
    surfaceVariant = IteraColors.SurfaceElevated,
    onSurfaceVariant = IteraColors.TextSecondary,
    outline = IteraColors.Border,
    error = IteraColors.Error
)

@Composable
fun IteraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IteraColorScheme,
        typography = IteraTypography,
        content = content
    )
}