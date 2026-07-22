package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.SessionDao
import com.luis.itera.data.local.dao.SessionWithSets
import com.luis.itera.data.local.dao.SetDao
import com.luis.itera.data.local.entity.SessionEntity
import com.luis.itera.data.local.entity.SetEntity
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val setDao: SetDao
) : SessionRepository {

    override fun getActiveSession(): Flow<Session?> =
        sessionDao.getActiveSessionWithSets().map { it?.toDomain() }

    override fun getSessionsByDate(dateEpochDay: Long): Flow<List<Session>> =
        sessionDao.getSessionsWithSetsByDate(dateEpochDay).map { list -> list.map { it.toDomain() } }

    override fun getSessionById(sessionId: Long): Flow<Session?> =
        sessionDao.getSessionWithSetsById(sessionId).map { it?.toDomain() }

    override fun getTrainedDays(): Flow<List<Long>> = sessionDao.getTrainedDays()

    override suspend fun startSession(dateEpochDay: Long, focus: String?): Long =
        sessionDao.insert(
            SessionEntity(
                dateEpochDay = dateEpochDay,
                focus = focus,
                startEpochMillis = System.currentTimeMillis()
            )
        )

    override suspend fun finishSession(session: Session) {
        val durationMinutes = if (session.startEpochMillis > 0L)
            ((System.currentTimeMillis() - session.startEpochMillis) / 60_000L).toInt()
        else session.durationMinutes
        sessionDao.update(
            SessionEntity(
                id = session.id,
                dateEpochDay = session.dateEpochDay,
                durationMinutes = durationMinutes,
                notes = session.notes,
                isFinished = true,
                focus = session.focus,
                startEpochMillis = session.startEpochMillis
            )
        )
    }

    override suspend fun deleteSession(sessionId: Long) = sessionDao.deleteById(sessionId)

    override suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightAddedKg: Float,
        durationSeconds: Int,
        intensity: Int,
        workSeconds: Int,
        restSeconds: Int,
        isPr: Boolean
    ): Long {
        val nextOrder = setDao.getMaxOrder(sessionId) + 1
        return setDao.insert(
            SetEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                reps = reps,
                weightAddedKg = weightAddedKg,
                order = nextOrder,
                durationSeconds = durationSeconds,
                intensity = intensity,
                workSeconds = workSeconds,
                restSeconds = restSeconds,
                isPr = isPr
            )
        )
    }

    override suspend fun deleteSet(set: WorkoutSet) {
        setDao.delete(
            SetEntity(
                id = set.id, sessionId = set.sessionId, exerciseId = set.exerciseId,
                reps = set.reps, weightAddedKg = set.weightAddedKg, order = set.order,
                durationSeconds = set.durationSeconds, intensity = set.intensity,
                workSeconds = set.workSeconds, restSeconds = set.restSeconds, isPr = set.isPr
            )
        )
    }

    override suspend fun hasFinishedSession(dateEpochDay: Long): Boolean =
        sessionDao.hasFinishedSession(dateEpochDay)

    override suspend fun getLastSetsForExercise(exerciseId: Long, limit: Int): List<WorkoutSet> =
        setDao.getLastSetsForExercise(exerciseId, limit).map { it.toDomain() }

    override fun getLastFinishedSession(): Flow<Session?> =
        sessionDao.getLastFinishedSession().map { it?.toDomain() }

    override fun getFinishedSessionsSince(fromEpochDay: Long): Flow<List<Session>> =
        sessionDao.getFinishedSessionsWithSetsSince(fromEpochDay).map { list -> list.map { it.toDomain() } }

    override suspend fun getMaxWeightForExercise(exerciseId: Long): Float? =
        setDao.getMaxWeightFinished(exerciseId)

    override suspend fun getMaxRepsBodyweight(exerciseId: Long): Int? =
        setDao.getMaxRepsBodyweightFinished(exerciseId)

    override fun getSetCountsByExercise(): Flow<Map<Long, Int>> =
        setDao.getSetCountsByExercise().map { rows -> rows.associate { it.exerciseId to it.setCount } }
}

private fun SessionEntity.toDomain(sets: List<WorkoutSet> = emptyList()) =
    Session(id, dateEpochDay, durationMinutes, notes, isFinished, focus, startEpochMillis, sets)

private fun SetEntity.toDomain() =
    WorkoutSet(id, sessionId, exerciseId, reps, weightAddedKg, order, durationSeconds, intensity, workSeconds, restSeconds, isPr)

private fun SessionWithSets.toDomain() =
    session.toDomain(sets.map { it.toDomain() })