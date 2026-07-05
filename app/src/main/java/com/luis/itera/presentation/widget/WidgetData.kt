package com.luis.itera.presentation.widget

data class WidgetData(
    val sessionsThisWeek: Int = 0,
    val weeklyGoal: Int = 3,
    val streakWeeks: Int = 0,
    val hydrationPercent: Int = 0,
    val trainedDaysThisWeek: Set<Int> = emptySet()
)