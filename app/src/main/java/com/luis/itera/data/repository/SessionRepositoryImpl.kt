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

    override fun getTrainedDays(): Flow<List<Long>> = sessionDao.getTrainedDays()

    override suspend fun startSession(dateEpochDay: Long, focus: String?): Long =
        sessionDao.insert(SessionEntity(dateEpochDay = dateEpochDay, focus = focus))

    override fun getSessionById(sessionId: Long): Flow<Session?> =
        sessionDao.getSessionWithSetsById(sessionId).map { it?.toDomain() }

    override suspend fun finishSession(session: Session) {
        sessionDao.update(
            SessionEntity(
                id = session.id,
                dateEpochDay = session.dateEpochDay,
                durationMinutes = session.durationMinutes,
                notes = session.notes,
                isFinished = true,
                focus = session.focus
            )
        )
    }

    override suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightAddedKg: Float
    ): Long {
        val nextOrder = setDao.getMaxOrder(sessionId) + 1
        return setDao.insert(
            SetEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                reps = reps,
                weightAddedKg = weightAddedKg,
                order = nextOrder
            )
        )
    }

    override suspend fun deleteSet(set: WorkoutSet) {
        setDao.delete(
            SetEntity(set.id, set.sessionId, set.exerciseId, set.reps, set.weightAddedKg, set.order)
        )
    }

    override suspend fun hasFinishedSession(dateEpochDay: Long): Boolean =
        sessionDao.hasFinishedSession(dateEpochDay)
}

private fun SessionEntity.toDomain(sets: List<WorkoutSet> = emptyList()) =
    Session(id, dateEpochDay, durationMinutes, notes, isFinished, focus, sets)

private fun SetEntity.toDomain() =
    WorkoutSet(id, sessionId, exerciseId, reps, weightAddedKg, order)

private fun SessionWithSets.toDomain() =
    session.toDomain(sets.map { it.toDomain() })