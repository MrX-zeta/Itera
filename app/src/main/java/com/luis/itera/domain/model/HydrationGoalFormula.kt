package com.luis.itera.domain.model

/**
 * Fórmula de la meta de hidratación, compartida entre [com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase]
 * (que persiste la meta del día) y el widget/histórico (que la calculan "al vuelo" para
 * días sin fila guardada, sin escribir en BD). Antes vivía duplicada en ambos sitios.
 */
object HydrationGoalFormula {
    const val ML_PER_KG = 35
    const val ACTIVE_DAY_BONUS_ML = 1000

    fun totalGoalMl(weightKg: Float, isActiveDay: Boolean): Int =
        (weightKg * ML_PER_KG).toInt() + if (isActiveDay) ACTIVE_DAY_BONUS_ML else 0
}
