package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.ExerciseDao
import com.luis.itera.data.local.entity.ExerciseEntity
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getAll(): Flow<List<Exercise>> =
        exerciseDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun search(query: String): Flow<List<Exercise>> =
        exerciseDao.search(query).map { list -> list.map { it.toDomain() } }
}

private fun ExerciseEntity.toDomain() =
    Exercise(id, name, category, equipment, mainMuscleGroup)