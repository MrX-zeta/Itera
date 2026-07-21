package com.luis.itera.data.local

import android.content.Context
import androidx.datastore.dataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.luis.itera.presentation.theme.AccentColor
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
        context.dataStore.edit { it[weightKey] = weightKg.coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG) }
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

    private val accentColorKey = intPreferencesKey("accent_color")

    fun getAccentColor(): Flow<AccentColor> =
        context.dataStore.data.map { AccentColor.fromOrdinal(it[accentColorKey] ?: AccentColor.Default.ordinal) }

    suspend fun setAccentColor(accent: AccentColor) {
        context.dataStore.edit { it[accentColorKey] = accent.ordinal }
    }

    private val restGoalSecondsKey = intPreferencesKey("rest_goal_seconds")

    fun getRestGoalSeconds(): Flow<Int> =
        context.dataStore.data.map { it[restGoalSecondsKey] ?: DEFAULT_REST_GOAL_SECONDS }

    suspend fun setRestGoalSeconds(seconds: Int) {
        context.dataStore.edit { it[restGoalSecondsKey] = seconds.coerceIn(MIN_REST_GOAL_SECONDS, MAX_REST_GOAL_SECONDS) }
    }

    private companion object {
        const val DEFAULT_WEIGHT_KG = 70f
        const val DEFAULT_WEEKLY_GOAL = 3
        const val MIN_WEIGHT_KG = 30f
        const val MAX_WEIGHT_KG = 250f
        const val DEFAULT_REST_GOAL_SECONDS = 90
        const val MIN_REST_GOAL_SECONDS = 10
        const val MAX_REST_GOAL_SECONDS = 600
    }
}