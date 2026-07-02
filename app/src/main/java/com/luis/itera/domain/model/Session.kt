package com.luis.itera.domain.model

data class Session(
    val id: Long,
    val dateEpochDay: Long,
    val durationMinutes: Int,
    val notes: String?,
    val isFinished: Boolean,
    val sets: List<WorkoutSet> = emptyList()
)