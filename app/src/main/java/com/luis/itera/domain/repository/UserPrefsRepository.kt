package com.luis.itera.domain.repository

import com.luis.itera.presentation.theme.AccentColor
import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    fun getUserWeightKg(): Flow<Float>
    suspend fun setUserWeightKg(weightKg: Float)
    fun getWeeklyGoal(): Flow<Int>
    suspend fun setWeeklyGoal(goal: Int)

    fun getOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted(completed: Boolean)

    fun getWidgetPinRequested(): Flow<Boolean>
    suspend fun setWidgetPinRequested(requested: Boolean)

    fun getAccentColor(): Flow<AccentColor>
    suspend fun setAccentColor(accent: AccentColor)

    fun getRestGoalSeconds(): Flow<Int>
    suspend fun setRestGoalSeconds(seconds: Int)
}