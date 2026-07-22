package com.luis.itera.domain.model

/**
 * Actividad de una modalidad dentro de una ventana temporal: cuándo se entrenó por última
 * vez y cuántas sesiones la incluyeron. Es la señal que decide qué pestañas de modalidad
 * aparecen en Estadísticas (Fuerza / Calistenia / Cardio). Cálculo PURO en memoria.
 */
data class ModalityActivity(
    val modality: TrainingModality,
    val lastSessionEpochDay: Long,
    val sessionCount: Int
)

/**
 * Modalidades presentes en una sesión, derivadas de sus EJERCICIOS REALES, no del `focus`
 * (que es lossy: un "PULL" puede ser fuerza o calistenia). Cada set → su [Exercise] →
 * [toModality]. Una sesión puede tener varias (p.ej. sentadilla con barra [STRENGTH] +
 * dominadas [CALISTHENICS]). Los sets cuyo ejercicio no esté en el mapa se ignoran.
 */
fun sessionModalities(session: Session, exercisesById: Map<Long, Exercise>): Set<TrainingModality> =
    session.sets.mapNotNull { exercisesById[it.exerciseId]?.toModality() }.toSet()

/**
 * Actividad por modalidad contando SOLO las sesiones dentro de la ventana (dateEpochDay ≥
 * [windowStartEpochDay]). Para cada modalidad presente devuelve su última fecha y cuántas
 * sesiones la incluyeron. Deriva la modalidad de los ejercicios reales de cada sesión.
 */
fun modalityActivityInWindow(
    sessions: List<Session>,
    exercisesById: Map<Long, Exercise>,
    windowStartEpochDay: Long
): Map<TrainingModality, ModalityActivity> {
    val datesByModality = mutableMapOf<TrainingModality, MutableList<Long>>()
    for (session in sessions) {
        if (session.dateEpochDay < windowStartEpochDay) continue
        for (modality in sessionModalities(session, exercisesById)) {
            datesByModality.getOrPut(modality) { mutableListOf() }.add(session.dateEpochDay)
        }
    }
    return datesByModality.mapValues { (modality, dates) ->
        ModalityActivity(modality, lastSessionEpochDay = dates.max(), sessionCount = dates.size)
    }
}

/**
 * ¿La modalidad es RECIENTE y SIGNIFICATIVA? Activa en las últimas [recentWeeks] semanas
 * (ventana móvil desde [todayEpochDay]) Y con al menos [minSessions] sesiones. Si no cumple,
 * queda ARCHIVADA del FOCO de Estadísticas — NUNCA se borra: Historial conserva todo.
 * Un día de calistenia puntual no crea pestaña; una modalidad pausada meses se oculta del foco.
 */
fun isRecentAndSignificant(
    activity: ModalityActivity?,
    todayEpochDay: Long,
    recentWeeks: Int,
    minSessions: Int
): Boolean {
    if (activity == null) return false
    val recentEnough = todayEpochDay - activity.lastSessionEpochDay <= recentWeeks * 7L
    return recentEnough && activity.sessionCount >= minSessions
}
