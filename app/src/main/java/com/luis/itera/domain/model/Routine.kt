package com.luis.itera.domain.model

data class Routine(
    val id: Long,
    val name: String,
    val focus: String?,
    val exerciseIds: List<Long>,
    /** Ordinal de [com.luis.itera.presentation.theme.RoutineColor]: la franja identificadora. */
    val color: Int = 0
)