package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.RoutineDao
import com.luis.itera.data.local.entity.RoutineEntity
import com.luis.itera.data.local.entity.RoutineExerciseEntity
import com.luis.itera.domain.model.Routine
import com.luis.itera.domain.repository.SaveRoutineResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.luis.itera.domain.repository.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val routineDao: RoutineDao
) : RoutineRepository {
    override fun getRoutines(): Flow<List<Routine>> =
        routineDao.getRoutinesWithExercises().map { list ->
            list.map { rwe ->
                Routine(
                    id = rwe.routine.id,
                    name = rwe.routine.name,
                    focus = rwe.routine.focus,
                    exerciseIds = rwe.exercises.sortedBy { it.displayOrder }.map { it.exerciseId }
                )
            }
        }

    override suspend fun saveRoutine(name: String, focus: String?, exerciseIds: List<Long>): SaveRoutineResult {
        val existing = routineDao.getRoutinesWithExercises().first()
        val target = exerciseIds.toSet()
        existing.firstOrNull { rwe ->
            rwe.exercises.map { it.exerciseId }.toSet() == target
        }?.let { return SaveRoutineResult.Duplicate(it.routine.name) }

        val routineId = routineDao.insertRoutine(RoutineEntity(name = name, focus = focus))
        routineDao.insertRoutineExercises(
            exerciseIds.mapIndexed { i, exId ->
                RoutineExerciseEntity(routineId = routineId, exerciseId = exId, displayOrder = i)
            }
        )
        return SaveRoutineResult.Created(routineId)
    }
    private val _routineFeedback = MutableSharedFlow<String>()
    val routineFeedback: SharedFlow<String> = _routineFeedback.asSharedFlow()
    override suspend fun deleteRoutine(routineId: Long) = routineDao.deleteRoutine(routineId)
}