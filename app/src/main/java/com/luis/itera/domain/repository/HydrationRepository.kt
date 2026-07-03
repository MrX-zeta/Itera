package com.luis.itera.domain.repository

import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.model.HydrationIntake
import kotlinx.coroutines.flow.Flow

interface HydrationRepository {
    fun getIntakesForDay(dateEpochDay: Long): Flow<List<HydrationIntake>>
    fun getTotalMlForDay(dateEpochDay: Long): Flow<Int>
    fun getAllIntakes(): Flow<List<HydrationIntake>>
    suspend fun deleteIntake(intake: HydrationIntake)
    fun getDailyGoal(dateEpochDay: Long): Flow<DailyHydrationGoal?>
    suspend fun addIntake(amountMl: Int)
    suspend fun upsertDailyGoal(goal: DailyHydrationGoal)

    suspend fun getLastIntakeForDay(dayStartMillis: Long): HydrationIntake?
    suspend fun updateIntakeAmount(id: Long, newAmount: Int)
}