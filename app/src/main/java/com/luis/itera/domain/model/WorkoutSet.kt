package com.luis.itera.domain.model

data class WorkoutSet(
    val id: Long,
    val sessionId: Long,
    val exerciseId: Long,
    val reps: Int,
    val weightAddedKg: Float,
    val order: Int,
    val durationSeconds: Int = 0,
    val intensity: Int = 0
)