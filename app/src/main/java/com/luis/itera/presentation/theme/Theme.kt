package com.luis.itera.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

private val IteraBaseColorScheme = darkColorScheme(
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
fun IteraTheme(
    accent: AccentColor = AccentColor.Default,
    content: @Composable () -> Unit
) {
    // Enfoque híbrido: el acento se propaga por [LocalAccent] (lo que consumen las
    // pantallas) y de paso alimenta colorScheme.primary/onPrimary para que ripples
    // y cualquier Material futuro sigan el mismo acento. Los neutros no cambian.
    val colorScheme = remember(accent) {
        IteraBaseColorScheme.copy(primary = accent.color, onPrimary = accent.onAccent)
    }
    CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = IteraTypography,
            content = content
        )
    }
}
