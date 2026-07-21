package com.luis.itera.data.repository

import com.luis.itera.data.local.dao.HydrationDao
import com.luis.itera.data.local.entity.DailyHydrationGoalEntity
import com.luis.itera.data.local.entity.HydrationIntakeEntity
import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.model.HydrationIntake
import com.luis.itera.domain.repository.HydrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class HydrationRepositoryImpl @Inject constructor(
    private val hydrationDao: HydrationDao
) : HydrationRepository {

    override fun getIntakesForDay(dateEpochDay: Long): Flow<List<HydrationIntake>> {
        val (start, end) = dayBounds(dateEpochDay)
        return hydrationDao.getIntakesBetween(start, end)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getLastIntakeForDay(dayStartMillis: Long): HydrationIntake? =
        hydrationDao.getLastIntakeForDay(dayStartMillis)?.toDomain()

    override suspend fun updateIntakeAmount(id: Long, newAmount: Int) =
        hydrationDao.updateIntakeAmount(id, newAmount)

    override fun getAllIntakes(): Flow<List<HydrationIntake>> =
        hydrationDao.getAllIntakes().map { list -> list.map { it.toDomain() } }

    override fun getTotalMlForDay(dateEpochDay: Long): Flow<Int> {
        val (start, end) = dayBounds(dateEpochDay)
        return hydrationDao.getTotalMlBetween(start, end)
    }

    override fun getDailyGoal(dateEpochDay: Long): Flow<DailyHydrationGoal?> =
        hydrationDao.getDailyGoal(dateEpochDay).map { it?.toDomain() }

    override fun getDailyGoalsBetween(fromEpochDay: Long, toEpochDay: Long): Flow<Map<Long, DailyHydrationGoal>> =
        hydrationDao.getDailyGoalsBetween(fromEpochDay, toEpochDay)
            .map { list -> list.associate { it.dateEpochDay to it.toDomain() } }

    override suspend fun addIntake(amountMl: Int) {
        hydrationDao.insertIntake(
            HydrationIntakeEntity(
                dateTimeEpochMillis = System.currentTimeMillis(),
                amountMl = amountMl
            )
        )
    }

    override suspend fun deleteIntake(intake: HydrationIntake) {
        hydrationDao.deleteIntake(
            HydrationIntakeEntity(
                id = intake.id,
                dateTimeEpochMillis = intake.dateTimeEpochMillis,
                amountMl = intake.amountMl
            )
        )
    }

    override suspend fun upsertDailyGoal(goal: DailyHydrationGoal) {
        hydrationDao.upsertDailyGoal(
            DailyHydrationGoalEntity(
                dateEpochDay = goal.dateEpochDay,
                baseGoalMl = goal.baseGoalMl,
                activityBonusMl = goal.activityBonusMl,
                totalGoalMl = goal.totalGoalMl,
                isActiveDay = goal.isActiveDay
            )
        )
    }

    override suspend fun reInsertIntake(dateTimeEpochMillis: Long, amountMl: Int) {
        hydrationDao.insertIntake(
            HydrationIntakeEntity(dateTimeEpochMillis = dateTimeEpochMillis, amountMl = amountMl)
        )
    }

    private fun dayBounds(dateEpochDay: Long): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.ofEpochDay(dateEpochDay)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}

private fun HydrationIntakeEntity.toDomain() =
    HydrationIntake(id, dateTimeEpochMillis, amountMl)

private fun DailyHydrationGoalEntity.toDomain() =
    DailyHydrationGoal(dateEpochDay, baseGoalMl, activityBonusMl, totalGoalMl, isActiveDay)