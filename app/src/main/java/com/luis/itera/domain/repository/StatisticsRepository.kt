package com.luis.itera.domain.repository

import com.luis.itera.data.local.dao.DensityRow
import com.luis.itera.domain.model.ExerciseSeriesPoint
import kotlinx.coroutines.flow.Flow
import com.luis.itera.data.local.dao.TopExerciseRecord

interface StatisticsRepository {
    fun getMaxWeightSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getVolumeSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getPersonalRecords(exerciseIds: List<Long>): Flow<Map<Long, Triple<Float, Float, Long>>>
    fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int>
    fun getFocusList(fromEpochDay: Long): Flow<List<String>>
    fun getAllTrainedDays(): Flow<List<Long>>
    fun getMostTrainedExerciseId(): Flow<Long?>
    fun getMaxRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getTotalRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun hasWeightedSets(exerciseId: Long, fromEpochDay: Long): Flow<Boolean>

    fun getTopExercises(limit: Int = 3): Flow<List<TopExerciseRecord>>

    fun getWorkoutDensity(fromEpochDay: Long): Flow<List<DensityRow>>

    fun getLastExercisedId(): Flow<Long?>
}