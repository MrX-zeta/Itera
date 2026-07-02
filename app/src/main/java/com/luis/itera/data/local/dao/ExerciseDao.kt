package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.luis.itera.data.local.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>)
}