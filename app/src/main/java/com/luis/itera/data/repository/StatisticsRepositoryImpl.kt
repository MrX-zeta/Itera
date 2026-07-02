package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.StatisticsDao
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsDao: StatisticsDao
) : StatisticsRepository {

    override fun getMaxWeightSeries(
        exerciseId: Long,
        fromEpochDay: Long
    ): Flow<List<ExerciseSeriesPoint>> =
        statisticsDao.getMaxWeightSeries(exerciseId, fromEpochDay)
            .map { list -> list.map { ExerciseSeriesPoint(it.dateEpochDay, it.maxWeightKg) } }

    override fun getVolumeSeries(
        exerciseId: Long,
        fromEpochDay: Long
    ): Flow<List<ExerciseSeriesPoint>> =
        statisticsDao.getVolumeSeries(exerciseId, fromEpochDay)
            .map { list -> list.map { ExerciseSeriesPoint(it.dateEpochDay, it.volumeKg) } }

    override fun getPersonalRecords(
        exerciseIds: List<Long>
    ): Flow<Map<Long, Pair<Float, Long>>> =
        statisticsDao.getPersonalRecords(exerciseIds)
            .map { list -> list.associate { it.exerciseId to (it.maxWeightKg to it.dateEpochDay) } }

    override fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int> =
        statisticsDao.getFinishedSessionCount(fromEpochDay)

    override fun getFocusList(fromEpochDay: Long): Flow<List<String>> =
        statisticsDao.getFocusList(fromEpochDay)

    override fun getAllTrainedDays(): Flow<List<Long>> =
        statisticsDao.getAllTrainedDays()

    override suspend fun getMostTrainedExerciseId(): Long? =
        statisticsDao.getMostTrainedExerciseId()
}