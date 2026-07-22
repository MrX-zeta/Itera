package com.luis.itera.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Acentos curados que el usuario puede elegir en Ajustes. El selector cambia
 * SOLO el acento; los neutros ([IteraColors]) se mantienen siempre.
 *
 * - [color]:    relleno del acento (botones, chips, iconos activos, gráficas, anillos).
 * - [onAccent]: texto/icono legible SOBRE un relleno de acento.
 *
 * El orden del enum es estable: su `ordinal` se persiste en DataStore, así que
 * no reordenar ni intercalar valores.
 */
enum class AccentColor(val color: Color, val onAccent: Color) {
    TEAL(color = Color(0xFF2BBFA8), onAccent = Color(0xFF04302A)),
    INDIGO(color = Color(0xFF7C6FF0), onAccent = Color(0xFF0E0A2E)),
    LIME(color = Color(0xFF9BE23F), onAccent = Color(0xFF16250A));

    companion object {
        val Default = TEAL

        /** Resuelve un ordinal persistido a su acento, cayendo al default si es inválido. */
        fun fromOrdinal(ordinal: Int): AccentColor =
            entries.getOrElse(ordinal) { Default }
    }
}

/**
 * Acento vigente propagado por el árbol de Compose desde [IteraTheme]. Es
 * `static` a propósito: al cambiar, invalida todo el subárbol y la app repinta
 * entera (que es justo lo que queremos con el selector de acento).
 */
val LocalAccent = staticCompositionLocalOf { AccentColor.Default }
