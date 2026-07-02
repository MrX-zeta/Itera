package com.luis.itera.presentation.active_workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Session
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
    val sessionStartMillis: Long? = null
)

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

    private val exercises = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) exerciseRepository.getAll()
        else exerciseRepository.search(query)
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        sessionRepository.getActiveSession(),
        exercises,
        searchQuery,
        selectedExercise,
        combine(reps, weightKg, sessionStartMillis) { r, w, s -> Triple(r, w, s) }
    ) { session, exerciseList, query, selected, inputs ->
        ActiveWorkoutUiState(
            session = session,
            exercises = exerciseList,
            searchQuery = query,
            selectedExercise = selected,
            reps = inputs.first,
            weightKg = inputs.second,
            sessionStartMillis = inputs.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

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
            sessionRepository.startSession(LocalDate.now().toEpochDay())
        }
    }

    fun onStartTimer() {
        sessionStartMillis.value = System.currentTimeMillis()
    }

    fun onRegisterSet() {
        val session = uiState.value.session ?: return
        val exercise = selectedExercise.value ?: return
        viewModelScope.launch {
            sessionRepository.addSet(session.id, exercise.id, reps.value, weightKg.value)
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
        }
    }
}