package com.luis.itera.domain.usecase

import com.luis.itera.domain.model.ModalityActivity
import com.luis.itera.domain.model.isRecentAndSignificant
import com.luis.itera.domain.model.modalityActivityInWindow
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import java.time.LocalDate
import javax.inject.Inject

/**
 * Modalidades ACTIVAS (RECIENTES y SIGNIFICATIVAS) que 6B pintará como pestañas dinámicas
 * de Estadísticas. Orquesta la señal completa —sesiones terminadas + ejercicios reales +
 * umbrales de prefs— para que el ViewModel solo consuma el resultado ("pinto lo que me da"),
 * sin reconstruir el combine. La modalidad se deriva de los ejercicios de cada sesión.
 *
 * Devuelve la actividad de cada modalidad activa, ordenada por [TrainingModality] (orden de
 * pestaña estable). Vacío = usuario nuevo o sin modalidad significativa → solo General en 6B.
 */
class GetActiveModalitiesUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val userPrefsRepository: UserPrefsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<ModalityActivity>> =
        combine(
            userPrefsRepository.getStatsRecentWeeks(),
            userPrefsRepository.getStatsMinSessions()
        ) { recentWeeks, minSessions -> recentWeeks to minSessions }
            .flatMapLatest { (recentWeeks, minSessions) ->
                val today = LocalDate.now()
                val windowStart = today.minusWeeks(recentWeeks.toLong()).toEpochDay()
                combine(
                    sessionRepository.getFinishedSessionsSince(windowStart),
                    exerciseRepository.getAll()
                ) { sessions, exercises ->
                    val exercisesById = exercises.associateBy { it.id }
                    modalityActivityInWindow(sessions, exercisesById, windowStart).values
                        .filter { isRecentAndSignificant(it, today.toEpochDay(), recentWeeks, minSessions) }
                        .sortedBy { it.modality.ordinal }
                }
            }
}
