package com.luis.itera.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPrefsRepository {
    fun getUserWeightKg(): Flow<Float>
    suspend fun setUserWeightKg(weightKg: Float)
}