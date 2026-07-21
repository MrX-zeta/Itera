package com.luis.itera.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Neutros de la app (fondo, superficies y texto). Son ESTÁTICOS: nunca cambian
 * con el selector de acento, para que ninguna combinación vuelva algo ilegible.
 * El acento, en cambio, es dinámico y vive en [AccentColor] / [LocalAccent].
 */
object IteraColors {
    val Background = Color(0xFF0F1113)
    val SurfaceSubtle = Color(0xFF131519)
    val Surface = Color(0xFF16191D)
    val SurfaceElevated = Color(0xFF1E2229)
    val Border = Color(0xFF1F1F24)
    val BorderStrong = Color(0xFF3A3A44)
    val TextPrimary = Color(0xFFEDEEF0)
    val TextSecondary = Color(0xFF8A8D94)
    val TextSecondaryStrong = Color(0xFFB0B0B8)
    val TextTertiary = Color(0xFF6E7178)
    val Error = Color(0xFFE24B4A)

    // Ámbar de LOGRO: exclusivo para logros (PR, récord, mejora vs antes). Nunca como
    // marca, relleno de botón ni acento primario (regla estricta de la skill).
    val Achievement = Color(0xFFE8B75D)

    // Azul-agua: EXCLUSIVO del heatmap/histórico de hidratación (nunca el acento de la
    // app). Distingue el hábito de agua del de entrenamiento (que va en el acento/teal).
    val HydrationAccent = Color(0xFF5FC5DB)
    val HydrationAccentMedium = Color(0xFF3F9BB8)
    val HydrationAccentDim = Color(0xFF26525E)
    // Contraste oscuro para texto/contorno sobre relleno azul-agua (análogo a onAccent).
    val HydrationOnAccent = Color(0xFF04242C)
}
