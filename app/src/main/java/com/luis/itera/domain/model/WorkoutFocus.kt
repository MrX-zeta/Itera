package com.luis.itera.domain.model

enum class WorkoutFocus(val label: String, val muscleGroups: Set<String>) {
    PUSH("PUSH", setOf("Pecho", "Hombros", "Tríceps")),
    PULL("PULL", setOf("Espalda", "Bíceps", "Antebrazo", "Trapecios")),
    LEGS("LEGS", setOf("Piernas", "Femoral", "Gastrocnemio")),
    TORSO("TORSO", setOf("Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps", "Trapecios", "Antebrazo")),
    PIERNA("PIERNA", setOf("Piernas", "Femoral", "Gastrocnemio")),
    PECHO("PECHO", setOf("Pecho")),
    ESPALDA("ESPALDA", setOf("Espalda", "Trapecios")),
    BRAZO("BRAZO", setOf("Bíceps", "Tríceps", "Antebrazo")),
    FULL_BODY("FULL BODY", emptySet());

    companion object {
        fun fromStored(stored: String?): Set<WorkoutFocus> =
            stored?.split(",")?.mapNotNull { name ->
                entries.find { it.name == name }
            }?.toSet() ?: emptySet()

        fun toStored(focuses: Set<WorkoutFocus>): String? =
            focuses.takeIf { it.isNotEmpty() }?.joinToString(",") { it.name }
    }

    fun conflictsWith(other: WorkoutFocus): Boolean {
        if (this == other) return false
        if (this == FULL_BODY || other == FULL_BODY) return true
        return muscleGroups.intersect(other.muscleGroups).isNotEmpty()
    }

}