package com.luis.itera.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Colores DESCRIPTIVOS de rutina: la franja que identifica visualmente cada rutina. Son un set
 * propio, distinto del acento dinámico de la app ([AccentColor]) y de los colores semánticos
 * (ámbar=logro, azul=hidratación): aquí solo sirven para distinguir rutinas de un vistazo.
 *
 * El orden es estable: su `ordinal` se persiste en Room (columna `routines.color`), así que
 * NUNCA reordenar ni quitar entradas del medio; solo añadir al final.
 */
enum class RoutineColor(val color: Color) {
    TEAL(Color(0xFF2BBFA8)),
    CYAN(Color(0xFF3FB6D6)),
    INDIGO(Color(0xFF7C6FF0)),
    VIOLET(Color(0xFFB25FE0)),
    PINK(Color(0xFFE85D9A)),
    CORAL(Color(0xFFEA7A4E)),
    AMBER(Color(0xFFE8B75D)),
    LIME(Color(0xFF9BE23F)),
    GREEN(Color(0xFF43C06B)),
    SLATE(Color(0xFF8A8D94));

    companion object {
        val Default = TEAL
        fun fromOrdinal(ordinal: Int): RoutineColor = entries.getOrElse(ordinal) { Default }
    }
}
