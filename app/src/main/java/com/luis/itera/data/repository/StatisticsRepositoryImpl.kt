package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.StatisticsDao
import com.luis.itera.data.local.dao.WeeklyVolumeRow
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

    override fun getMaxRepsSeries(
        exerciseId: Long,
        fromEpochDay: Long
    ): Flow<List<ExerciseSeriesPoint>> =
        statisticsDao.getMaxRepsSeries(exerciseId, fromEpochDay)
            .map { list -> list.map { ExerciseSeriesPoint(it.dateEpochDay, it.maxWeightKg) } }

    override fun getTotalRepsSeries(
        exerciseId: Long,
        fromEpochDay: Long
    ): Flow<List<ExerciseSeriesPoint>> =
        statisticsDao.getTotalRepsSeries(exerciseId, fromEpochDay)
            .map { list -> list.map { ExerciseSeriesPoint(it.dateEpochDay, it.volumeKg) } }

    override fun hasWeightedSets(exerciseId: Long, fromEpochDay: Long): Flow<Boolean> =
        statisticsDao.hasWeightedSets(exerciseId, fromEpochDay)

    override fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int> =
        statisticsDao.getFinishedSessionCount(fromEpochDay)

    override fun getFocusList(fromEpochDay: Long): Flow<List<String>> =
        statisticsDao.getFocusList(fromEpochDay)

    override fun getAllTrainedDays(): Flow<List<Long>> =
        statisticsDao.getAllTrainedDays()

    override fun getWeeklyVolume(): Flow<List<WeeklyVolumeRow>> =
        statisticsDao.getWeeklyVolume()

    override fun getMaxWeeklyVolume(): Flow<Float> =
        statisticsDao.getMaxWeeklyVolume().map { it ?: 0f }

    override fun getLastExercisedId(): Flow<Long?> =
        statisticsDao.getLastExercisedId()

    override fun getDaysWithPr(): Flow<List<Long>> =
        statisticsDao.getDaysWithPr()

    override fun getLastTrainedDayByMuscleGroup(): Flow<Map<String, Long>> =
        statisticsDao.getLastTrainedDayByMuscleGroup()
            .map { rows -> rows.associate { it.mainMuscleGroup to it.lastEpochDay } }
}