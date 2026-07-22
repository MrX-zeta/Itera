package com.luis.itera.presentation.widget

import com.luis.itera.presentation.theme.AccentColor

data class WidgetData(
    val sessionsThisWeek: Int = 0,
    val weeklyGoal: Int = 3,
    val streakWeeks: Int = 0,
    val hydrationPercent: Int = 0,
    val trainedDaysThisWeek: Set<Int> = emptySet(),
    /** Índice del día actual (0=lunes..6=domingo), para destacarlo en la fila de iniciales. */
    val todayIndex: Int = 0,
    /** Acento elegido en Ajustes; el widget lo recibe por el mismo canal reactivo. */
    val accent: AccentColor = AccentColor.Default
)