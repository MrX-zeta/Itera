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

    fun getAccentColor(): Flow<AccentColor>
    suspend fun setAccentColor(accent: AccentColor)

    fun getRestGoalSeconds(): Flow<Int>
    suspend fun setRestGoalSeconds(seconds: Int)

    /** Umbrales de pestañas dinámicas de Estadísticas. La UI en Ajustes los conecta luego. */
    fun getStatsRecentWeeks(): Flow<Int>
    suspend fun setStatsRecentWeeks(weeks: Int)
    fun getStatsMinSessions(): Flow<Int>
    suspend fun setStatsMinSessions(sessions: Int)
}