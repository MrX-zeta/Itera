package com.luis.itera.domain.model

data class ExerciseSeriesPoint(
    val dateEpochDay: Long,
    val value: Float
)

data class BigThreeRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeightKg: Float?,
    val dateEpochDay: Long?
)

data class WeeklyStreak(
    val weeks: Int,
    val sessionsThisWeek: Int,
    val weeklyGoal: Int
)