package com.luis.itera.data.local

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")
private val weeklyGoalKey = intPreferencesKey("weekly_goal")

@Singleton
class UserPrefsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val weightKey = floatPreferencesKey("user_weight_kg")

    fun getUserWeightKg(): Flow<Float> =
        context.dataStore.data.map { it[weightKey] ?: DEFAULT_WEIGHT_KG }

    fun getWeeklyGoal(): Flow<Int> =
        context.dataStore.data.map { it[weeklyGoalKey] ?: DEFAULT_WEEKLY_GOAL }


    suspend fun setUserWeightKg(weightKg: Float) {
        context.dataStore.edit { it[weightKey] = weightKg }
    }

    suspend fun setWeeklyGoal(goal: Int) {
        context.dataStore.edit { it[weeklyGoalKey] = goal.coerceIn(1, 7) }
    }

    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    fun getOnboardingCompleted(): Flow<Boolean> =
        context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[onboardingKey] = completed }
    }

    private companion object {
        const val DEFAULT_WEIGHT_KG = 70f
        const val DEFAULT_WEEKLY_GOAL = 3
    }
}