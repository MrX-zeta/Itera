package com.luis.itera.domain.model

/**
 * Foco sugerido para "hoy": rotación pseudoaleatoria estable durante el día (cambia mañana),
 * SIN depender del historial de entrenamiento — funciona igual para un usuario nuevo que para
 * uno con años de datos. Determinística por fecha (mismo día → mismo resultado).
 */
fun dailySuggestedFocus(todayEpochDay: Long): WorkoutFocus {
    val entries = WorkoutFocus.entries
    // Constante de Knuth para dispersar el epochDay: evita que la sugerencia cicle en el mismo
    // orden del enum cada 7 días (ej. siempre PUSH los lunes).
    val hashed = todayEpochDay * 2654435761L
    val index = ((hashed % entries.size) + entries.size) % entries.size
    return entries[index.toInt()]
}
