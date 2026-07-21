package com.luis.itera.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Neutros de la app (fondo, superficies y texto). Son ESTÁTICOS: nunca cambian
 * con el selector de acento, para que ninguna combinación vuelva algo ilegible.
 * El acento, en cambio, es dinámico y vive en [AccentColor] / [LocalAccent].
 */
object IteraColors {
    val Background = Color(0xFF0F1113)
    val Surface = Color(0xFF16191D)
    val SurfaceElevated = Color(0xFF1E2229)
    val Border = Color(0xFF1F1F24)
    val BorderStrong = Color(0xFF3A3A44)
    val TextPrimary = Color(0xFFEDEEF0)
    val TextSecondary = Color(0xFF8A8D94)
    val TextSecondaryStrong = Color(0xFFB0B0B8)
    val TextTertiary = Color(0xFF6E7178)
    val Error = Color(0xFFE24B4A)
}
