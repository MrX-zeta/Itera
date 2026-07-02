package com.luis.itera.domain.usecase

import com.luis.itera.domain.model.WeeklyStreak
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

class CalculateWeeklyStreakUseCase @Inject constructor() {

    operator fun invoke(trainedDaysDesc: List<Long>, weeklyGoal: Int): WeeklyStreak {
        val today = LocalDate.now()
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val sessionsByWeek = trainedDaysDesc
            .map(LocalDate::ofEpochDay)
            .groupBy { it.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
            .mapValues { it.value.size }

        val sessionsThisWeek = sessionsByWeek[currentWeekStart] ?: 0

        var streak = 0
        var week = currentWeekStart.minusWeeks(1)
        while ((sessionsByWeek[week] ?: 0) >= weeklyGoal) {
            streak++
            week = week.minusWeeks(1)
        }
        if (sessionsThisWeek >= weeklyGoal) streak++

        return WeeklyStreak(
            weeks = streak,
            sessionsThisWeek = sessionsThisWeek,
            weeklyGoal = weeklyGoal
        )
    }
}