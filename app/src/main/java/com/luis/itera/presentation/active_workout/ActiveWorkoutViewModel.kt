package com.luis.itera.presentation.active_workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WeeklyStreak
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

private data class HomeData(
    val lastSession: Session? = null,
    val streak: WeeklyStreak = WeeklyStreak(0, 0, 3),
    val hydrationProgress: Float = 0f,
    val trainedDaysThisWeek: Set<Long> = emptySet()
)

data class ActiveWorkoutUiState(
    val session: Session? = null,
    val exercises: List<Exercise> = emptyList(),
    val searchQuery: String = "",
    val selectedExercise: Exercise? = null,
    val reps: Int = 10,
    val weightKg: Float = 0f,
    val sessionStartMillis: Long? = null,
    val selectedFocuses: Set<WorkoutFocus> = emptySet(),
    val allExerciseNames: Map<Long, String> = emptyMap(),
    val lastSets: List<WorkoutSet> = emptyList(),
    val lastFinishedSession: Session? = null,
    val streak: WeeklyStreak = WeeklyStreak(0, 0, 3),
    val hydrationProgress: Float = 0f,
    val trainedDaysThisWeek: Set<Long> = emptySet(),
    val durationSeconds: Int = 0,
    val intensity: Int = 1
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

private data class InputBundle(
    val reps: Int,
    val weightKg: Float,
    val startMillis: Long?,
    val durationSeconds: Int,
    val intensity: Int
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val exerciseRepository: ExerciseRepository,
    private val hydrationRepository: HydrationRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val statisticsRepository: StatisticsRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase,
    private val calculateWeeklyStreak: CalculateWeeklyStreakUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedExercise = MutableStateFlow<Exercise?>(null)
    private val reps = MutableStateFlow(10)
    private val weightKg = MutableStateFlow(0f)
    private val sessionStartMillis = MutableStateFlow<Long?>(null)
    private val selectedFocuses = MutableStateFlow<Set<WorkoutFocus>>(emptySet())
    private val lastRegisteredSetAt = MutableStateFlow(0L)
    private val lastSets = MutableStateFlow<List<WorkoutSet>>(emptyList())
    private val durationSeconds = MutableStateFlow(0)
    private val intensity = MutableStateFlow(1)

    private val _finishedSessionId = MutableStateFlow<Long?>(null)
    val finishedSessionId: StateFlow<Long?> = _finishedSessionId

    val muscleGroups = listOf(
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazo", "Trapecios", "Piernas", "Femoral", "Gastrocnemio"
    )

    private val today = LocalDate.now().toEpochDay()

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

    private val homeData = combine(
        sessionRepository.getLastFinishedSession(),
        statisticsRepository.getAllTrainedDays(),
        userPrefsRepository.getWeeklyGoal(),
        hydrationRepository.getTotalMlForDay(today),
        hydrationRepository.getDailyGoal(today)
    ) { last, trainedDays, weeklyGoal, totalMl, goal ->
        val weekStart = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .toEpochDay()
        HomeData(
            lastSession = last,
            streak = calculateWeeklyStreak(trainedDays, weeklyGoal),
            hydrationProgress = goal?.totalGoalMl?.takeIf { it > 0 }
                ?.let { (totalMl.toFloat() / it).coerceIn(0f, 1f) } ?: 0f,
            trainedDaysThisWeek = trainedDays.filter { it >= weekStart }.toSet()
        )
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        sessionRepository.getActiveSession(),
        exercises,
        searchQuery,
        combine(selectedExercise, selectedFocuses, lastSets) { e, f, l -> Triple(e, f, l) },
        combine(reps, weightKg, sessionStartMillis, durationSeconds, intensity) { r, w, s, d, i ->
            InputBundle(r, w, s, d, i)
        },
        allExercises,
        homeData,
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val selection = args[3] as Triple<Exercise?, Set<WorkoutFocus>, List<WorkoutSet>>
        val inputs = args[4] as InputBundle
        val home = args[6] as HomeData
        @Suppress("UNCHECKED_CAST")
        ActiveWorkoutUiState(
            session = args[0] as Session?,
            exercises = args[1] as List<Exercise>,
            searchQuery = args[2] as String,
            selectedExercise = selection.first,
            selectedFocuses = selection.second,
            lastSets = selection.third,
            reps = inputs.reps,
            weightKg = inputs.weightKg,
            sessionStartMillis = inputs.startMillis,
            allExerciseNames = (args[5] as List<Exercise>).associate { it.id to it.name },
            lastFinishedSession = home.lastSession,
            streak = home.streak,
            hydrationProgress = home.hydrationProgress,
            trainedDaysThisWeek = home.trainedDaysThisWeek,
            durationSeconds = inputs.durationSeconds,
            intensity = inputs.intensity,
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
        viewModelScope.launch {
            val previous = sessionRepository.getLastSetsForExercise(exercise.id)
            lastSets.value = previous
            previous.firstOrNull()?.let { last ->
                reps.value = last.reps
                weightKg.value = last.weightAddedKg
            }
        }
    }

    fun onCreateExercise(name: String, muscleGroup: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = exerciseRepository.create(name, muscleGroup)
            searchQuery.value = ""
            onExerciseSelected(
                Exercise(id, name.trim(), "Personalizado", "Personalizado", muscleGroup)
            )
        }
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
        val isCardio = exercise.mainMuscleGroup.equals("Cardio", ignoreCase = true)
        viewModelScope.launch {
            sessionRepository.addSet(
                sessionId = session.id,
                exerciseId = exercise.id,
                reps = if (isCardio) 0 else reps.value,
                weightAddedKg = if (isCardio) 0f else weightKg.value,
                durationSeconds = if (isCardio) durationSeconds.value else 0,
                intensity = if (isCardio) intensity.value else 0
            )
        }
    }

    fun onDeleteSet(set: WorkoutSet) {
        viewModelScope.launch { sessionRepository.deleteSet(set) }
    }

    fun onDiscardSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepository.deleteSession(session.id)
            sessionStartMillis.value = null
            selectedExercise.value = null
        }
    }

    fun onFinishSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            if (session.sets.isEmpty()) {
                sessionRepository.deleteSession(session.id)
            } else {
                sessionRepository.finishSession(session)
                calculateHydrationGoal(session.dateEpochDay)
            }
            sessionStartMillis.value = null
            selectedExercise.value = null
            if (session.sets.isNotEmpty()) {
                _finishedSessionId.value = session.id
            }
        }
    }

    fun onFinishedSessionConsumed() {
        _finishedSessionId.value = null
    }

    fun onDurationDelta(delta: Int) {
        durationSeconds.value = (durationSeconds.value + delta).coerceAtLeast(0)
    }

    fun onIntensityDelta(delta: Int) {
        intensity.value = (intensity.value + delta).coerceIn(1, 10)
    }

    private companion object {
        const val REGISTER_DEBOUNCE_MS = 700L
    }
}