package com.luis.itera.domain.usecase

import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalculateHydrationGoalUseCase @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val sessionRepository: SessionRepository,
    private val hydrationRepository: HydrationRepository
) {
    suspend operator fun invoke(dateEpochDay: Long): DailyHydrationGoal {
        val weightKg = userPrefsRepository.getUserWeightKg().first()
        val baseGoalMl = (weightKg * ML_PER_KG).toInt()
        val isActiveDay = sessionRepository.hasFinishedSession(dateEpochDay)
        val activityBonusMl = if (isActiveDay) ACTIVE_DAY_BONUS_ML else 0

        val goal = DailyHydrationGoal(
            dateEpochDay = dateEpochDay,
            baseGoalMl = baseGoalMl,
            activityBonusMl = activityBonusMl,
            totalGoalMl = baseGoalMl + activityBonusMl,
            isActiveDay = isActiveDay
        )
        hydrationRepository.upsertDailyGoal(goal)
        return goal
    }

    private companion object {
        const val ML_PER_KG = 35
        const val ACTIVE_DAY_BONUS_ML = 1000
    }
}