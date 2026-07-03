package com.luis.itera.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.luis.itera.data.local.entity.DailyHydrationGoalEntity
import com.luis.itera.data.local.entity.HydrationIntakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HydrationDao {
    @Insert
    suspend fun insertIntake(intake: HydrationIntakeEntity)

    @Query("SELECT * FROM hydration_intakes WHERE dateTimeEpochMillis BETWEEN :startMillis AND :endMillis ORDER BY dateTimeEpochMillis DESC")
    fun getIntakesBetween(startMillis: Long, endMillis: Long): Flow<List<HydrationIntakeEntity>>

    @Query("SELECT COALESCE(SUM(amountMl), 0) FROM hydration_intakes WHERE dateTimeEpochMillis BETWEEN :startMillis AND :endMillis")
    fun getTotalMlBetween(startMillis: Long, endMillis: Long): Flow<Int>

    @Upsert
    suspend fun upsertDailyGoal(goal: DailyHydrationGoalEntity)

    @Query("SELECT * FROM daily_hydration_goals WHERE dateEpochDay = :dateEpochDay")
    fun getDailyGoal(dateEpochDay: Long): Flow<DailyHydrationGoalEntity?>

    @Query("SELECT * FROM hydration_intakes ORDER BY dateTimeEpochMillis DESC")
    fun getAllIntakes(): Flow<List<HydrationIntakeEntity>>

    @Delete
    suspend fun deleteIntake(intake: HydrationIntakeEntity)
}