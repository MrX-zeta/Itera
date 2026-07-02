package com.luis.itera.domain.repository

import com.luis.itera.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getAll(): Flow<List<Exercise>>
    fun search(query: String): Flow<List<Exercise>>
    suspend fun create(name: String, mainMuscleGroup: String): Long
}