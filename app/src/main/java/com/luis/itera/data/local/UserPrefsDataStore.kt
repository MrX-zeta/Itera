package com.luis.itera.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPrefsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val weightKey = floatPreferencesKey("user_weight_kg")

    fun getUserWeightKg(): Flow<Float> =
        context.dataStore.data.map { it[weightKey] ?: DEFAULT_WEIGHT_KG }

    suspend fun setUserWeightKg(weightKg: Float) {
        context.dataStore.edit { it[weightKey] = weightKg }
    }

    private companion object {
        const val DEFAULT_WEIGHT_KG = 70f
    }
}