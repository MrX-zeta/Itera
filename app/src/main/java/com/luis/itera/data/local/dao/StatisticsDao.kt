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

@Dao
interface StatisticsDao {

    @Query("""
        SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
               MAX(s.weightAddedKg) AS maxWeightKg
        FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
              AND ses.dateEpochDay >= :fromEpochDay
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
    suspend fun getMostTrainedExerciseId(): Long?

    @Query("""
    SELECT s.sessionId AS sessionId, ses.dateEpochDay AS dateEpochDay,
           MAX(s.reps) AS maxWeightKg
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
           SUM(s.reps) AS volumeKg
    FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
          AND ses.dateEpochDay >= :fromEpochDay
    GROUP BY s.sessionId
    ORDER BY ses.dateEpochDay ASC
""")
    fun getTotalRepsSeries(exerciseId: Long, fromEpochDay: Long): Flow<List<VolumePoint>>

    @Query("""
    SELECT EXISTS(
        SELECT 1 FROM sets s
        INNER JOIN sessions ses ON ses.id = s.sessionId
        WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
              AND ses.dateEpochDay >= :fromEpochDay AND s.weightAddedKg > 0
    )
""")
    fun hasWeightedSets(exerciseId: Long, fromEpochDay: Long): Flow<Boolean>
}