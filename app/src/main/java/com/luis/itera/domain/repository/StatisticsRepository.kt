package com.luis.itera.domain.repository

import com.luis.itera.domain.model.ExerciseSeriesPoint
import kotlinx.coroutines.flow.Flow
import com.luis.itera.data.local.dao.WeeklyVolumeRow

interface StatisticsRepository {
    fun getMaxWeightSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getVolumeSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int>
    fun getFocusList(fromEpochDay: Long): Flow<List<String>>
    fun getAllTrainedDays(): Flow<List<Long>>
    fun getMaxRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun getTotalRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<ExerciseSeriesPoint>>
    fun hasWeightedSets(exerciseId: Long, fromEpochDay: Long): Flow<Boolean>

    fun getWeeklyVolume(): Flow<List<WeeklyVolumeRow>>

    fun getMaxWeeklyVolume(): Flow<Float>

    fun getLastExercisedId(): Flow<Long?>

    /** Días (epochDay) con al menos un set marcado como PR histórico. */
    fun getDaysWithPr(): Flow<List<Long>>

    /** Última fecha (epochDay) entrenada por grupo muscular. Grupos nunca entrenados no aparecen. */
    fun getLastTrainedDayByMuscleGroup(): Flow<Map<String, Long>>
}