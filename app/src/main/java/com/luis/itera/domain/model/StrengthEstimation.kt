package com.luis.itera.domain.model

/**
 * Estimación de fuerza a partir de sets reales. Cálculo PURO en memoria (no persiste, no
 * toca Room). Solo tiene sentido con carga externa: en calistenia / peso corporal el 1RM
 * no aplica. Lo consumirá la vista de Fuerza en Estadísticas (1RM absoluto solo en los 3
 * básicos; en el resto, el % de cambio).
 */

/** Por encima de estas reps, Epley pierde fiabilidad y NO damos una cifra concreta. */
const val ONE_REP_MAX_RELIABLE_REPS = 12

/** Resultado de estimar un 1RM: o una cifra fiable, o una razón honesta para no darla. */
sealed interface OneRepMaxEstimate {
    /** 1RM estimado fiable (reps dentro del cap). */
    data class Reliable(val valueKg: Float) : OneRepMaxEstimate

    /**
     * reps por encima del cap: un 1RM extrapolado desde 13+ reps es poco fiable. Preferimos
     * honestidad a una cifra bonita pero falsa, así que NO devolvemos número.
     */
    data object LowConfidence : OneRepMaxEstimate

    /** No aplica: peso ≤ 0 (corporal) o reps ≤ 0. En calistenia el 1RM no significa nada. */
    data object NotApplicable : OneRepMaxEstimate
}

/**
 * 1RM estimado con Epley: `1RM ≈ peso × (1 + reps / 30)`.
 * - peso ≤ 0 (corporal) o reps ≤ 0 → [OneRepMaxEstimate.NotApplicable].
 * - reps == 1 → [OneRepMaxEstimate.Reliable] con el propio peso (ya ES el 1RM, sin fórmula).
 * - 2..[ONE_REP_MAX_RELIABLE_REPS] → [OneRepMaxEstimate.Reliable] con Epley.
 * - reps > [ONE_REP_MAX_RELIABLE_REPS] → [OneRepMaxEstimate.LowConfidence] (sin cifra).
 */
fun estimateOneRepMax(weightKg: Float, reps: Int): OneRepMaxEstimate = when {
    weightKg <= 0f || reps <= 0 -> OneRepMaxEstimate.NotApplicable
    reps == 1 -> OneRepMaxEstimate.Reliable(weightKg)
    reps <= ONE_REP_MAX_RELIABLE_REPS -> OneRepMaxEstimate.Reliable(weightKg * (1f + reps / 30f))
    else -> OneRepMaxEstimate.LowConfidence
}

/**
 * Mejor 1RM estimado FIABLE de una lista de sets (p.ej. los de un ejercicio en una sesión).
 * Descarta los no aplicables (corporal) y los de baja fiabilidad (reps > cap). null si
 * ninguno da una estimación fiable. Es el puente natural hacia [strengthChangePercent].
 */
fun bestReliableOneRepMax(sets: List<WorkoutSet>): Float? =
    sets.mapNotNull { (estimateOneRepMax(it.weightAddedKg, it.reps) as? OneRepMaxEstimate.Reliable)?.valueKg }
        .maxOrNull()

/**
 * Variación porcentual de fuerza estimada entre dos momentos (reciente vs previo), a partir
 * de sus 1RM estimados. Ej: 100 → 108 devuelve +8.0. Es SOLO el cálculo; la traducción
 * humana ("subió 8%") vive en la UI.
 *
 * null cuando falta algún 1RM o el previo es ≤ 0: sin referente no hay porcentaje.
 */
fun strengthChangePercent(recentOneRmKg: Float?, previousOneRmKg: Float?): Float? {
    if (recentOneRmKg == null || previousOneRmKg == null || previousOneRmKg <= 0f) return null
    return (recentOneRmKg - previousOneRmKg) / previousOneRmKg * 100f
}
