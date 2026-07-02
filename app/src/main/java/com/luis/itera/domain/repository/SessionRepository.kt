package com.luis.itera.domain.repository

import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getActiveSession(): Flow<Session?>
    fun getSessionsByDate(dateEpochDay: Long): Flow<List<Session>>
    fun getTrainedDays(): Flow<List<Long>>
    suspend fun startSession(dateEpochDay: Long): Long
    suspend fun finishSession(session: Session)
    suspend fun addSet(sessionId: Long, exerciseId: Long, reps: Int, weightAddedKg: Float): Long
    suspend fun deleteSet(set: WorkoutSet)
    suspend fun hasFinishedSession(dateEpochDay: Long): Boolean
}