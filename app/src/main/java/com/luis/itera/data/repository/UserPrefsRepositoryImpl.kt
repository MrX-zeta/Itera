package com.luis.itera.data.repository

import com.luis.itera.data.local.UserPrefsDataStore
import com.luis.itera.domain.repository.UserPrefsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPrefsRepositoryImpl @Inject constructor(
    private val dataStore: UserPrefsDataStore
) : UserPrefsRepository {
    override fun getUserWeightKg(): Flow<Float> = dataStore.getUserWeightKg()
    override suspend fun setUserWeightKg(weightKg: Float) = dataStore.setUserWeightKg(weightKg)
    override fun getWeeklyGoal(): Flow<Int> = dataStore.getWeeklyGoal()
    override suspend fun setWeeklyGoal(goal: Int) = dataStore.setWeeklyGoal(goal)
    override fun getOnboardingCompleted(): Flow<Boolean> = dataStore.getOnboardingCompleted()
    override suspend fun setOnboardingCompleted(completed: Boolean) = dataStore.setOnboardingCompleted(completed)
    override fun getWidgetPinRequested(): Flow<Boolean> = dataStore.getWidgetPinRequested()
    override suspend fun setWidgetPinRequested(requested: Boolean) = dataStore.setWidgetPinRequested(requested)
}