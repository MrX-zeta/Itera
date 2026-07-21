package com.luis.itera.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val trainedDays: Set<LocalDate> = emptySet(),
    val prDays: Set<LocalDate> = emptySet(),
    val sessions: List<Session> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val exerciseNames: Map<Long, String> = emptyMap(),
    val pendingDeleteId: Long? = null
)

/** "¿Dónde me quedé?": última sesión completa registrada de un ejercicio concreto. */
data class ExerciseLookupResult(
    val exercise: Exercise,
    val dateEpochDay: Long?,
    val sets: List<WorkoutSet>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository,
    statisticsRepository: StatisticsRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow<LocalDate?>(null)
    val selectedDay: StateFlow<LocalDate?> = _selectedDay.asStateFlow()
    private val pendingDeleteId = MutableStateFlow<Long?>(null)
    private var deleteJob: Job? = null

    private val sessions = _selectedDay.flatMapLatest { day ->
        sessionRepository.getSessionsByDate((day ?: LocalDate.now()).toEpochDay())
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        statisticsRepository.getAllTrainedDays(),
        statisticsRepository.getDaysWithPr(),
        sessions,
        exerciseRepository.getAll(),
        pendingDeleteId
    ) { trainedDayEpochs, prDayEpochs, sessionList, exercises, pendingId ->
        HistoryUiState(
            trainedDays = trainedDayEpochs.map { LocalDate.ofEpochDay(it) }.toSet(),
            prDays = prDayEpochs.map { LocalDate.ofEpochDay(it) }.toSet(),
            sessions = sessionList.sortedByDescending { it.id },
            exercises = exercises,
            exerciseNames = exercises.associate { it.id to it.name },
            pendingDeleteId = pendingId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _exerciseLookup = MutableStateFlow<ExerciseLookupResult?>(null)
    val exerciseLookup: StateFlow<ExerciseLookupResult?> = _exerciseLookup.asStateFlow()

    /**
     * Busca la ÚLTIMA SESIÓN completa de un ejercicio. Reusa getLastSetsForExercise
     * (ya alimenta la sugerencia de sesión activa) agrupando por el sessionId del set
     * más reciente, y getSessionById para su fecha — sin recalcular progresión/PR.
     */
    fun onLookupExercise(exercise: Exercise) {
        viewModelScope.launch {
            val recentSets = sessionRepository.getLastSetsForExercise(exercise.id, 50)
            val mostRecent = recentSets.firstOrNull()
            _exerciseLookup.value = if (mostRecent == null) {
                ExerciseLookupResult(exercise, dateEpochDay = null, sets = emptyList())
            } else {
                val sameSession = recentSets.filter { it.sessionId == mostRecent.sessionId }
                val date = sessionRepository.getSessionById(mostRecent.sessionId).first()?.dateEpochDay
                ExerciseLookupResult(exercise, dateEpochDay = date, sets = sameSession)
            }
        }
    }

    fun onClearLookup() {
        _exerciseLookup.value = null
    }

    fun onDaySelected(date: LocalDate?) {
        _selectedDay.value = date
    }

    fun onSwipeDelete(sessionId: Long) {
        deleteJob?.cancel()
        val previousPending = pendingDeleteId.value
        pendingDeleteId.value = sessionId
        deleteJob = viewModelScope.launch {
            if (previousPending != null && previousPending != sessionId) {
                sessionRepository.deleteSession(previousPending)
            }
            delay(UNDO_WINDOW_MS)
            sessionRepository.deleteSession(sessionId)
            pendingDeleteId.value = null
        }
    }

    fun onUndoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        pendingDeleteId.value = null
    }

    override fun onCleared() {
        val pending = pendingDeleteId.value ?: return
        deleteJob?.cancel()
        kotlinx.coroutines.runBlocking { sessionRepository.deleteSession(pending) }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 4_000L
    }
}