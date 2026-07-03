package com.luis.itera.domain.model

enum class WorkoutFocus(val label: String, val muscleGroups: Set<String>) {
    PUSH("PUSH", setOf("Pecho", "Hombros", "Tríceps")),
    PULL("PULL", setOf("Espalda", "Bíceps", "Antebrazo", "Trapecios")),
    LEGS("LEGS", setOf("Piernas", "Femoral", "Gastrocnemio")),
    UPPER("UPPER", setOf("Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazo", "Trapecios")),
    LOWER("LOWER", setOf("Piernas", "Femoral", "Gastrocnemio")),
    FULL_BODY("FULL BODY", setOf("Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Antebrazo", "Trapecios", "Piernas", "Femoral", "Gastrocnemio")),
    CARDIO("CARDIO", emptySet());

    fun conflictsWith(other: WorkoutFocus): Boolean = when {
        this == FULL_BODY || other == FULL_BODY -> true
        this == CARDIO || other == CARDIO -> false
        else -> muscleGroups.intersect(other.muscleGroups).isNotEmpty()
    }

    companion object {
        fun fromStored(csv: String?): Set<WorkoutFocus> =
            csv?.split(",")?.mapNotNull { name -> entries.find { it.name == name } }?.toSet() ?: emptySet()

        fun toStored(focuses: Set<WorkoutFocus>): String? =
            focuses.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name }
    }
}