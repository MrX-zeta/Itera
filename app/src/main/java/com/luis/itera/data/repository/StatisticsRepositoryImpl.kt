package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.StatisticsDao
import com.luis.itera.data.local.dao.TopExerciseRecord
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

    override fun getPersonalRecords(
        exerciseIds: List<Long>
    ): Flow<Map<Long, Triple<Float, Float, Long>>> =
        statisticsDao.getPersonalRecords(exerciseIds)
            .map { list ->
                list.associate {
                    it.exerciseId to Triple(it.maxWeightKg, it.estimated1RmKg, it.dateEpochDay)
                }
            }

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

    override fun getMostTrainedExerciseId(): Flow<Long?> =
        statisticsDao.getMostTrainedExerciseId()

    override fun getTopExercises(limit: Int): Flow<List<TopExerciseRecord>> =
        statisticsDao.getTopExercises(limit)

    override fun getWeeklyVolume(): Flow<List<WeeklyVolumeRow>> =
        statisticsDao.getWeeklyVolume()

    override fun getLastExercisedId(): Flow<Long?> =
        statisticsDao.getLastExercisedId()

    override fun getDailyMuscleGroupCount(): Flow<Map<Long, Int>> =
        statisticsDao.getDailyMuscleGroupCount()
            .map { list -> list.associate { it.day to it.groupCount } }
}