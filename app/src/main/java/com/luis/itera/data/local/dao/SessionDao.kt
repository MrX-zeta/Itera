package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.luis.itera.data.local.entity.SessionEntity
import com.luis.itera.data.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

data class SessionWithSets(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val sets: List<SetEntity>
)

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Transaction
    @Query("SELECT * FROM sessions WHERE dateEpochDay = :dateEpochDay ORDER BY id DESC")
    fun getSessionsWithSetsByDate(dateEpochDay: Long): Flow<List<SessionWithSets>>

    @Query("SELECT * FROM sessions WHERE isFinished = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveSession(): Flow<SessionEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM sessions WHERE dateEpochDay = :dateEpochDay AND isFinished = 1)")
    suspend fun hasFinishedSession(dateEpochDay: Long): Boolean

    @Query("SELECT DISTINCT dateEpochDay FROM sessions WHERE isFinished = 1")
    fun getTrainedDays(): Flow<List<Long>>

    @Transaction
    @Query("SELECT * FROM sessions WHERE isFinished = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveSessionWithSets(): Flow<SessionWithSets?>

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun getSessionWithSetsById(sessionId: Long): Flow<SessionWithSets?>

    @Query("DELETE FROM sessions WHERE id = :sessionId")
    suspend fun deleteById(sessionId: Long)

    @Transaction
    @Query("SELECT * FROM sessions WHERE isFinished = 1 ORDER BY dateEpochDay DESC, id DESC LIMIT 1")
    fun getLastFinishedSession(): Flow<SessionWithSets?>
}