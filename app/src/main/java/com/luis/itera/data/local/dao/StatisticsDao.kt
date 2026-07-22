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

data class WeeklyVolumeRow(
    val weekStart: Long,
    val totalVolume: Float
)

data class MuscleGroupLastTrainedRow(
    val mainMuscleGroup: String,
    val lastEpochDay: Long
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
    SELECT MAX(weekly) FROM (
        SELECT SUM(s.reps * s.weightAddedKg) AS weekly
        FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE ses.isFinished = 1 AND s.weightAddedKg > 0
        GROUP BY (ses.dateEpochDay - 4) / 7
    )
""")
    fun getMaxWeeklyVolume(): Flow<Float?>

    @Query("""
    SELECT s.exerciseId FROM sets s
    ORDER BY s.id DESC
    LIMIT 1
""")
    fun getLastExercisedId(): Flow<Long?>

    @Query("""
        SELECT DISTINCT ses.dateEpochDay FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE s.isPr = 1 AND ses.isFinished = 1
    """)
    fun getDaysWithPr(): Flow<List<Long>>

    @Query("""
        SELECT e.mainMuscleGroup AS mainMuscleGroup, MAX(ses.dateEpochDay) AS lastEpochDay
        FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        INNER JOIN exercises e ON e.id = s.exerciseId
        WHERE ses.isFinished = 1
        GROUP BY e.mainMuscleGroup
    """)
    fun getLastTrainedDayByMuscleGroup(): Flow<List<MuscleGroupLastTrainedRow>>
}