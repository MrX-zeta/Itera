package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.luis.itera.data.local.entity.RoutineEntity
import com.luis.itera.data.local.entity.RoutineExerciseEntity
import kotlinx.coroutines.flow.Flow

data class RoutineWithExercises(
    @androidx.room.Embedded val routine: RoutineEntity,
    @androidx.room.Relation(parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseEntity>
)

@Dao
interface RoutineDao {
    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Insert
    suspend fun insertRoutineExercises(items: List<RoutineExerciseEntity>)

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    suspend fun clearRoutineExercises(routineId: Long)

    @Transaction
    @Query("SELECT * FROM routines ORDER BY id DESC")
    fun getRoutinesWithExercises(): Flow<List<RoutineWithExercises>>

    @Query("DELETE FROM routines WHERE id = :routineId")
    suspend fun deleteRoutine(routineId: Long)
}