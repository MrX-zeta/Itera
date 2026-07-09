package com.luis.itera.domain.model

data class ExerciseSeriesPoint(
    val dateEpochDay: Long,
    val value: Float
)

data class WeeklyStreak(
    val weeks: Int,
    val sessionsThisWeek: Int,
    val weeklyGoal: Int
)