package com.luis.itera.domain.model

data class ExerciseSeriesPoint(
    val dateEpochDay: Long,
    val value: Float
)

data class TopMovementRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val displayValue: String,
    val displayLabel: String
)

data class BigThreeRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val maxWeightKg: Float?,
    val estimated1RmKg: Float?,
    val dateEpochDay: Long?
)

data class WeeklyStreak(
    val weeks: Int,
    val sessionsThisWeek: Int,
    val weeklyGoal: Int
)