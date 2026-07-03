package com.luis.itera.domain.repository

import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun getActiveSession(): Flow<Session?>
    fun getSessionsByDate(dateEpochDay: Long): Flow<List<Session>>
    fun getSessionById(sessionId: Long): Flow<Session?>
    fun getTrainedDays(): Flow<List<Long>>
    suspend fun startSession(dateEpochDay: Long, focus: String?): Long
    suspend fun finishSession(session: Session)
    suspend fun deleteSession(sessionId: Long)
    suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightAddedKg: Float,
        durationSeconds: Int = 0,
        intensity: Int = 0,
        workSeconds: Int = 0,
        restSeconds: Int = 0
    ): Long
    suspend fun deleteSet(set: WorkoutSet)
    suspend fun hasFinishedSession(dateEpochDay: Long): Boolean
    suspend fun getLastSetsForExercise(exerciseId: Long, limit: Int = 3): List<WorkoutSet>

    fun getLastFinishedSession(): Flow<Session?>
}