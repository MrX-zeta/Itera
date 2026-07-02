package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.luis.itera.data.local.entity.SetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SetDao {
    @Insert
    suspend fun insert(set: SetEntity): Long

    @Delete
    suspend fun delete(set: SetEntity)

    @Query("SELECT * FROM sets WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun getBySession(sessionId: Long): Flow<List<SetEntity>>

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM sets WHERE sessionId = :sessionId")
    suspend fun getMaxOrder(sessionId: Long): Int

    @Query("""
    SELECT s.* FROM sets s
    INNER JOIN sessions ses ON ses.id = s.sessionId
    WHERE s.exerciseId = :exerciseId AND ses.isFinished = 1
    ORDER BY s.sessionId DESC, s.`order` DESC
    LIMIT :limit
""")
    suspend fun getLastSetsForExercise(exerciseId: Long, limit: Int): List<SetEntity>
}