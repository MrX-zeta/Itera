package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class MaxWeightPoint(
    val sessionId: Long,
    val dateEpochDay: Long,
    val maxWeightKg: Float
)

data class VolumePoint(
    val sessionId: Long,
    val dateEpochDay: Long,
    val volumeKg: Float
)

data class PersonalRecord(
    val exerciseId: Long,
    val maxWeightKg: Float,
    val estimated1RmKg: Float,
    val dateEpochDay: Long
)

data class WeeklyVolumeRow(
    val weekStart: Long,
    val totalVolume: Float
)

data class DailyGroupCountRow(val day: Long, val groupCount: Int)

data class TopExerciseRecord(
    val exerciseId: Long,
    val setCount: Int,
    val maxWeightKg: Float,
    val estimated1RmKg: Float,
    val maxReps: Int,
    val maxDurationSeconds: Int,
    val hasWeight: Boolean,
    val isCardio: Boolean
)


@Dao
interface StatisticsDao {

    @Query("""
    SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
           MAX(s.weightAddedKg) AS maxWeightKg
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
          AND ses.dateEpochDay >= :fromEpochDay AND s.weightAddedKg > 0
    GROUP BY s.sessionId
    ORDER BY ses.dateEpochDay ASC
""")
    fun getMaxWeightSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<MaxWeightPoint>>

    @Query("""
        SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
               SUM(s.reps * s.weightAddedKg) AS volumeKg
        FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
              AND ses.dateEpochDay >= :fromEpochDay
        GROUP BY s.sessionId
        ORDER BY ses.dateEpochDay ASC
    """)
    fun getVolumeSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<VolumePoint>>

    @Query("""
    SELECT s.exerciseId AS exerciseId,
           MAX(s.weightAddedKg) AS maxWeightKg,
           MAX(s.weightAddedKg * (1.0 + s.reps / 30.0)) AS estimated1RmKg,
           ses.dateEpochDay AS dateEpochDay
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId IN (:exerciseIds) AND ses.isFinished = 1 AND s.weightAddedKg > 0
    GROUP BY s.exerciseId
""")
    fun getPersonalRecords(exerciseIds: List<Long>): Flow<List<PersonalRecord>>

    @Query("""
        SELECT COUNT(*) FROM sessions
        WHERE isFinished = 1 AND dateEpochDay >= :fromEpochDay
    """)
    fun getFinishedSessionCount(fromEpochDay: Long): Flow<Int>

    @Query("""
        SELECT focus FROM sessions
        WHERE isFinished = 1 AND dateEpochDay >= :fromEpochDay AND focus IS NOT NULL
    """)
    fun getFocusList(fromEpochDay: Long): Flow<List<String>>

    @Query("""
        SELECT DISTINCT dateEpochDay FROM sessions
        WHERE isFinished = 1
        ORDER BY dateEpochDay DESC
    """)
    fun getAllTrainedDays(): Flow<List<Long>>

    @Query("""
        SELECT s.exerciseId FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE ses.isFinished = 1
        GROUP BY s.exerciseId
        ORDER BY COUNT(s.id) DESC
        LIMIT 1
    """)
    fun getMostTrainedExerciseId(): Flow<Long?>

    @Query("""
    SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
           CASE WHEN MAX(s.durationSeconds) > 0 THEN MAX(s.durationSeconds / 60)
                ELSE MAX(s.reps) END AS maxWeightKg
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
          AND ses.dateEpochDay >= :fromEpochDay
    GROUP BY s.sessionId
    ORDER BY ses.dateEpochDay ASC
""")
    fun getMaxRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<MaxWeightPoint>>

    @Query("""
    SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
           CASE WHEN SUM(s.durationSeconds) > 0 THEN SUM(s.durationSeconds) / 60
                ELSE SUM(s.reps) END AS volumeKg
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
          AND ses.dateEpochDay >= :fromEpochDay
    GROUP BY s.sessionId
    ORDER BY ses.dateEpochDay ASC
""")
    fun getTotalRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<VolumePoint>>

    @Query("""
    SELECT (
        SELECT COUNT(DISTINCT s.sessionId) FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
              AND ses.dateEpochDay >= :fromEpochDay AND s.weightAddedKg > 0
    ) >= 2
""")
    fun hasWeightedSets(exerciseId: Long, fromEpochDay: Long): Flow<Boolean>

    @Query("""
    SELECT s.exerciseId, COUNT(s.id) AS setCount,
           MAX(s.weightAddedKg) AS maxWeightKg,
           MAX(s.weightAddedKg * (1.0 + s.reps / 30.0)) AS estimated1RmKg,
           MAX(s.reps) AS maxReps,
           MAX(s.durationSeconds) AS maxDurationSeconds,
           CASE WHEN MAX(s.weightAddedKg) > 0 THEN 1 ELSE 0 END AS hasWeight,
           CASE WHEN MAX(s.durationSeconds) > 0 THEN 1 ELSE 0 END AS isCardio
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE ses.isFinished = 1
    GROUP BY s.exerciseId
    ORDER BY setCount DESC
    LIMIT :limit
""")
    fun getTopExercises(limit: Int = 3): Flow<List<TopExerciseRecord>>


    @Query("""
    SELECT ((ses.dateEpochDay - 4) / 7) * 7 + 4 AS weekStart,
           SUM(s.reps * s.weightAddedKg) AS totalVolume
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE ses.isFinished = 1 AND s.weightAddedKg > 0
    GROUP BY weekStart
    ORDER BY weekStart DESC
    LIMIT 5
""")
    fun getWeeklyVolume(): Flow<List<WeeklyVolumeRow>>

    @Query("""
    SELECT s.exerciseId FROM sets s
    ORDER BY s.id DESC
    LIMIT 1
""")
    fun getLastExercisedId(): Flow<Long?>

    @Query("""
        SELECT ses.dateEpochDay AS day, COUNT(DISTINCT e.mainMuscleGroup) AS groupCount
        FROM sets s
        JOIN sessions ses ON s.sessionId = ses.id
        JOIN exercises e ON s.exerciseId = e.id
        WHERE ses.isFinished = 1
        GROUP BY ses.dateEpochDay
    """)
    fun getDailyMuscleGroupCount(): Flow<List<DailyGroupCountRow>>
}