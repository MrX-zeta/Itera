package com.luis.itera.presentation.widget

import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun statisticsRepository(): StatisticsRepository
    fun hydrationRepository(): HydrationRepository
    fun userPrefsRepository(): UserPrefsRepository
    fun calculateWeeklyStreak(): CalculateWeeklyStreakUseCase
    fun calculateHydrationGoal(): CalculateHydrationGoalUseCase
}