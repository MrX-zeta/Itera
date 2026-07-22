package com.luis.itera.presentation.widget

import android.content.Context
import com.luis.itera.domain.model.HydrationGoalFormula
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Datos del widget como [Flow] REACTIVO. Es imprescindible que sea un flujo: con
 * `provideContent`, Glance mantiene viva la composición y solo la recompone, así
 * que si los datos se leyeran una sola vez quedarían congelados. Al observar los
 * flujos de Room, cada cambio (agua, sesiones, peso) emite y recompone el widget
 * en tiempo real mientras el proceso está vivo.
 */
fun loadWidgetDataFlow(context: Context): Flow<WidgetData> {
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java
    )
    val stats = entryPoint.statisticsRepository()
    val hydration = entryPoint.hydrationRepository()
    val prefs = entryPoint.userPrefsRepository()
    val streakUseCase = entryPoint.calculateWeeklyStreak()

    val today = LocalDate.now()
    val todayEpoch = today.toEpochDay()
    val weekStartEpoch = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()

    val base = combine(
        stats.getAllTrainedDays(),
        prefs.getWeeklyGoal(),
        hydration.getTotalMlForDay(todayEpoch),
        hydration.getDailyGoal(todayEpoch),
        prefs.getUserWeightKg()
    ) { trainedDays, weeklyGoal, totalMl, storedGoal, weight ->
        val streak = streakUseCase(trainedDays, weeklyGoal)

        // Meta del día: la guardada, o una calculada al vuelo (sin escribir en BD)
        // para que el anillo refleje el agua aunque no exista fila de meta hoy.
        val goalMl = storedGoal?.totalGoalMl
            ?: HydrationGoalFormula.totalGoalMl(weight, isActiveDay = todayEpoch in trainedDays)
        val hydrationPercent = goalMl.takeIf { it > 0 }
            ?.let { ((totalMl.toFloat() / it) * 100).toInt().coerceAtLeast(0) } ?: 0

        val trainedIndices = trainedDays
            .filter { it in weekStartEpoch..todayEpoch }
            .map { (it - weekStartEpoch).toInt() }
            .filter { it in 0..6 }
            .toSet()

        WidgetData(
            sessionsThisWeek = streak.sessionsThisWeek,
            weeklyGoal = streak.weeklyGoal,
            streakWeeks = streak.weeks,
            hydrationPercent = hydrationPercent,
            trainedDaysThisWeek = trainedIndices,
            todayIndex = (todayEpoch - weekStartEpoch).toInt().coerceIn(0, 6)
        )
    }

    // El acento entra por el MISMO canal reactivo: cambiarlo en Ajustes emite y
    // el widget recompone recoloreado, sin reiniciar nada. combine() con lambda
    // llega hasta 5 flujos, por eso se anida en vez de ampliar la lista.
    return combine(base, prefs.getAccentColor()) { data, accent ->
        data.copy(accent = accent)
    }.catch { emit(WidgetData()) }
}
