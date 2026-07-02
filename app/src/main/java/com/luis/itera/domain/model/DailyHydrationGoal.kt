package com.luis.itera.domain.model

data class DailyHydrationGoal(
    val dateEpochDay: Long,
    val baseGoalMl: Int,
    val activityBonusMl: Int,
    val totalGoalMl: Int,
    val isActiveDay: Boolean
)