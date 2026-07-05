package com.luis.itera.domain.repository

import com.luis.itera.domain.model.Routine
import kotlinx.coroutines.flow.Flow

sealed interface SaveRoutineResult {
    data class Created(val id: Long) : SaveRoutineResult
    data class Duplicate(val existingName: String) : SaveRoutineResult
}

interface RoutineRepository {
    fun getRoutines(): Flow<List<Routine>>
    suspend fun saveRoutine(name: String, focus: String?, exerciseIds: List<Long>): SaveRoutineResult
    suspend fun deleteRoutine(routineId: Long)
}