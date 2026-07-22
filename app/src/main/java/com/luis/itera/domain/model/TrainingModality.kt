package com.luis.itera.domain.model

/**
 * Modalidad de entrenamiento de un ejercicio. NO es una columna de Room: se deriva en
 * memoria de los campos existentes del [Exercise] con [toModality]. Alimenta la vista
 * adaptativa de Estadísticas (pestañas Fuerza / Calistenia / Cardio) sin tocar el esquema.
 */
enum class TrainingModality { STRENGTH, CALISTHENICS, CARDIO }

// Equipment sin carga externa → Calistenia cuando la category no decide ("Personalizado").
private val CALISTHENICS_EQUIPMENT = listOf("Peso Corporal", "Ninguno")

/**
 * Clasifica un ejercicio en su [TrainingModality] de forma determinística. Jerarquía de
 * confianza: la `category` es el dato más intencional (lo fija quien crea el ejercicio),
 * así que MANDA sobre los demás; `equipment` y `mainMuscleGroup` solo desempatan cuando
 * la category no es una de las tres conocidas.
 *
 * 1. `category` conocida gana siempre: Cardio→CARDIO, Calistenia→CALISTHENICS,
 *    Gimnasio→STRENGTH. Por eso Dominadas (category=Calistenia aunque equipment=Barra)
 *    da CALISTHENICS, y un Gimnasio con `mainMuscleGroup` raro sigue siendo STRENGTH.
 * 2. Cualquier otra category ("Personalizado" o libre) se desempata:
 *    - `mainMuscleGroup == "Cardio"` → CARDIO (override de cardio dentro del desempate).
 *    - Peso Corporal / Ninguno → CALISTHENICS.
 *    - Cuerda → CARDIO.
 *    - resto (Barra/Mancuerna/Máquina/Polea o equipment desconocido) → STRENGTH.
 */
fun Exercise.toModality(): TrainingModality = when {
    category.equals("Cardio", ignoreCase = true) -> TrainingModality.CARDIO
    category.equals("Calistenia", ignoreCase = true) -> TrainingModality.CALISTHENICS
    category.equals("Gimnasio", ignoreCase = true) -> TrainingModality.STRENGTH

    // category "Personalizado" o libre: desempate por mainMuscleGroup / equipment.
    mainMuscleGroup.equals("Cardio", ignoreCase = true) -> TrainingModality.CARDIO
    CALISTHENICS_EQUIPMENT.any { it.equals(equipment, ignoreCase = true) } -> TrainingModality.CALISTHENICS
    equipment.equals("Cuerda", ignoreCase = true) -> TrainingModality.CARDIO
    else -> TrainingModality.STRENGTH
}
