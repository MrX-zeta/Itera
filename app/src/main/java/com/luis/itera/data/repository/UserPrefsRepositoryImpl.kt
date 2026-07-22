package com.luis.itera.data.repository

import com.luis.itera.data.local.UserPrefsDataStore
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.presentation.theme.AccentColor
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: UserPrefsDataStore
) : UserPrefsRepository {
    override fun getUserWeightKg(): Flow<Float> = dataStore.getUserWeightKg()
    override suspend fun setUserWeightKg(weightKg: Float) = dataStore.setUserWeightKg(weightKg)
    override fun getWeightPromptDismissed(): Flow<Boolean> = dataStore.getWeightPromptDismissed()
    override suspend fun setWeightPromptDismissed(dismissed: Boolean) = dataStore.setWeightPromptDismissed(dismissed)
    override fun getWeeklyGoal(): Flow<Int> = dataStore.getWeeklyGoal()
    override suspend fun setWeeklyGoal(goal: Int) = dataStore.setWeeklyGoal(goal)
    override fun getOnboardingCompleted(): Flow<Boolean> = dataStore.getOnboardingCompleted()
    override suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.setOnboardingCompleted(completed)
    override fun getAccentColor(): Flow<AccentColor> = dataStore.getAccentColor()
    override suspend fun setAccentColor(accent: AccentColor) = dataStore.setAccentColor(accent)
    override fun getRestGoalSeconds(): Flow<Int> = dataStore.getRestGoalSeconds()
    override suspend fun setRestGoalSeconds(seconds: Int) = dataStore.setRestGoalSeconds(seconds)
    override fun getStatsRecentWeeks(): Flow<Int> = dataStore.getStatsRecentWeeks()
    override suspend fun setStatsRecentWeeks(weeks: Int) = dataStore.setStatsRecentWeeks(weeks)
    override fun getStatsMinSessions(): Flow<Int> = dataStore.getStatsMinSessions()
    override suspend fun setStatsMinSessions(sessions: Int) = dataStore.setStatsMinSessions(sessions)
}