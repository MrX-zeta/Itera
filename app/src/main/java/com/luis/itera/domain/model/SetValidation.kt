package com.luis.itera.domain.model

/**
 * Resultado de validar un set ANTES de registrarlo. Es lógica pura (sin estado ni
 * efectos): la orquestación (bloquear, pedir confirmación o registrar) vive en el
 * ViewModel. El embudo de datos `addSet` no se toca.
 */
sealed interface SetValidation {
    /** El set puede registrarse sin fricción. */
    data object Valid : SetValidation

    /** Reps en 0 en un ejercicio no-cardio: no tiene lectura legítima, se BLOQUEA. */
    data object BlockedZeroReps : SetValidation

    /**
     * El ejercicio requiere carga externa y el peso es 0: aviso suave CONFIRMABLE
     * (no bloquea; el usuario puede añadir peso o registrar a 0 kg de todos modos).
     */
    data object ConfirmZeroWeight : SetValidation
}

// Equipment que implica carga externa (peso 0 es sospechoso). Peso Corporal / Ninguno /
// Cuerda no cargan peso añadido, así que 0 kg ahí es válido y silencioso.
private val LOAD_EQUIPMENT = listOf("Barra", "Mancuerna", "Máquina", "Polea")

/**
 * Valida reps/peso de un set NO cardio. Cardio usa minutos/nivel y no pasa por aquí.
 *
 * - reps == 0 → [SetValidation.BlockedZeroReps].
 * - requiere carga externa (equipment ∈ LOAD_EQUIPMENT Y category ≠ "Calistenia") y
 *   peso == 0 → [SetValidation.ConfirmZeroWeight]. Así Dominadas/Muscle up
 *   (equipment=Barra pero category=Calistenia) NO piden peso.
 * - resto → [SetValidation.Valid].
 */
fun validateSet(exercise: Exercise, reps: Int, weightKg: Float): SetValidation {
    if (reps == 0) return SetValidation.BlockedZeroReps

    val requiresLoad = LOAD_EQUIPMENT.any { it.equals(exercise.equipment, ignoreCase = true) } &&
        !exercise.category.equals("Calistenia", ignoreCase = true)
    if (requiresLoad && weightKg == 0f) return SetValidation.ConfirmZeroWeight

    return SetValidation.Valid
}
