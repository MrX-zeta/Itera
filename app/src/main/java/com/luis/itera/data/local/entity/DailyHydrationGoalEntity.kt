package com.luis.itera.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_hydration_goals")
data class DailyHydrationGoalEntity(
    @PrimaryKey val dateEpochDay: Long,
    val baseGoalMl: Int,
    val activityBonusMl: Int,
    val totalGoalMl: Int,
    val isActiveDay: Boolean
)