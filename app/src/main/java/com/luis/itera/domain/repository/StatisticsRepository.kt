package com.luis.itera.domain.repository

import com.luis.itera.domain.model.ExerciseSeriesPoint
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun getMaxWeightSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getVolumeSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getPersonalRecords(exerciseIds: List<Long>): Flow<Map<Long, Pair<Float, Long>>>
    fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int>
    fun getFocusList(fromEpochDay: Long): Flow<List<String>>
    fun getAllTrainedDays(): Flow<List<Long>>
    suspend fun getMostTrainedExerciseId(): Long?
}