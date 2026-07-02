package com.luis.itera.presentation.active_workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ActiveWorkoutUiState(
    val session: Session? = null,
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedExercise: Exercise? = null,
    val reps: Int = 10,
    val weightKg: Float = 0f,
    val sessionStartMillis: Long? = null,
    val selectedFocuses: Set<WorkoutFocus> = emptySet(),
    val allExerciseNames: Map<Long, String> = emptyMap()
) {
    val sessionFocuses: Set<WorkoutFocus>
        get() = WorkoutFocus.fromStored(session?.focus)

    val blockedFocuses: Set<WorkoutFocus>
        get() = WorkoutFocus.entries.filter { candidate ->
            candidate !in selectedFocuses &&
                    selectedFocuses.any { it.conflictsWith(candidate) }
        }.toSet()

    fun exerciseNameOf(exerciseId: Long): String =
        allExerciseNames[exerciseId] ?: "—"
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedExercise = MutableStateFlow<Exercise?>(null)
    private val reps = MutableStateFlow(10)
    private val weightKg = MutableStateFlow(0f)
    private val sessionStartMillis = MutableStateFlow<Long?>(null)
    private val selectedFocuses = MutableStateFlow<Set<WorkoutFocus>>(emptySet())
    private val lastRegisteredSetAt = MutableStateFlow(0L)

    private val _finishedSessionId = MutableStateFlow<Long?>(null)
    val finishedSessionId: StateFlow<Long?> = _finishedSessionId

    private val allExercises = exerciseRepository.getAll()

    private val exercises = combine(
        searchQuery,
        sessionRepository.getActiveSession()
    ) { query, session -> query to WorkoutFocus.fromStored(session?.focus) }
        .flatMapLatest { (query, focuses) ->
            val source = if (query.isBlank()) exerciseRepository.getAll()
            else exerciseRepository.search(query)
            source.map { list -> filterByFocus(list, focuses) }
        }

    private fun filterByFocus(
        list: List<Exercise>,
        focuses: Set<WorkoutFocus>
    ): List<Exercise> {
        val groups = focuses.flatMap { it.muscleGroups }.toSet()
        if (groups.isEmpty()) return list
        return list.filter { it.mainMuscleGroup in groups }
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        sessionRepository.getActiveSession(),
        exercises,
        searchQuery,
        combine(selectedExercise, selectedFocuses) { e, f -> e to f },
        combine(reps, weightKg, sessionStartMillis) { r, w, s -> Triple(r, w, s) },
        allExercises
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val selection = args[3] as Pair<Exercise?, Set<WorkoutFocus>>
        val inputs = args[4] as Triple<Int, Float, Long?>
        @Suppress("UNCHECKED_CAST")
        ActiveWorkoutUiState(
            session = args[0] as Session?,
            exercises = args[1] as List<Exercise>,
            searchQuery = args[2] as String,
            selectedExercise = selection.first,
            selectedFocuses = selection.second,
            reps = inputs.first,
            weightKg = inputs.second,
            sessionStartMillis = inputs.third,
            allExerciseNames = (args[5] as List<Exercise>).associate { it.id to it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    fun onFocusToggle(focus: WorkoutFocus) {
        val current = selectedFocuses.value
        selectedFocuses.value = when {
            focus in current -> current - focus
            current.any { it.conflictsWith(focus) } -> current
            else -> current + focus
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onExerciseSelected(exercise: Exercise) {
        selectedExercise.value = exercise
        searchQuery.value = ""
    }

    fun onRepsDelta(delta: Int) {
        reps.value = (reps.value + delta).coerceAtLeast(1)
    }

    fun onWeightDelta(delta: Float) {
        weightKg.value = (weightKg.value + delta).coerceAtLeast(0f)
    }

    fun onStartSession() {
        viewModelScope.launch {
            sessionRepository.startSession(
                dateEpochDay = LocalDate.now().toEpochDay(),
                focus = WorkoutFocus.toStored(selectedFocuses.value)
            )
            selectedFocuses.value = emptySet()
        }
    }

    fun onStartTimer() {
        sessionStartMillis.value = System.currentTimeMillis()
    }

    fun onRegisterSet() {
        val now = System.currentTimeMillis()
        if (now - lastRegisteredSetAt.value < REGISTER_DEBOUNCE_MS) return
        val session = uiState.value.session ?: return
        val exercise = selectedExercise.value ?: return
        lastRegisteredSetAt.value = now
        viewModelScope.launch {
            sessionRepository.addSet(session.id, exercise.id, reps.value, weightKg.value)
        }
    }

    fun onDeleteSet(set: WorkoutSet) {
        viewModelScope.launch { sessionRepository.deleteSet(set) }
    }

    fun onDiscardSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepository.discardSession(session.id)
            sessionStartMillis.value = null
            selectedExercise.value = null
        }
    }

    fun onFinishSession() {
        val session = uiState.value.session ?: return
        val start = sessionStartMillis.value ?: System.currentTimeMillis()
        val durationMinutes = ((System.currentTimeMillis() - start) / 60_000L).toInt()
        viewModelScope.launch {
            sessionRepository.finishSession(session.copy(durationMinutes = durationMinutes))
            calculateHydrationGoal(session.dateEpochDay)
            sessionStartMillis.value = null
            selectedExercise.value = null
            _finishedSessionId.value = session.id
        }
    }

    fun onFinishedSessionConsumed() {
        _finishedSessionId.value = null
    }

    private companion object {
        const val REGISTER_DEBOUNCE_MS = 700L
    }
}