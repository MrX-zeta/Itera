package com.luis.itera.presentation.active_workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.PendingRoutineStart
import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Routine
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.SetValidation
import com.luis.itera.domain.model.WeeklyStreak
import com.luis.itera.domain.model.validateSet
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.RoutineRepository
import com.luis.itera.presentation.components.fmtWeight
import com.luis.itera.presentation.widget.WidgetUpdater
import com.luis.itera.domain.repository.SaveRoutineResult
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
import com.luis.itera.presentation.components.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val trainedDaysThisWeek: Set<Long> = emptySet(),
    val routines: List<Routine> = emptyList()
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
    val intensity: Int = 1,
    val setTimerMillis: Long = 0L,
    val timerPaused: Boolean = false,
    val pausedElapsed: Long = 0L,
    val prCelebrationText: String? = null,
    val routines: List<Routine> = emptyList()
) {
    val sessionFocuses: Set<WorkoutFocus>
        get() = WorkoutFocus.fromStored(session?.focus)

    val blockedFocuses: Set<WorkoutFocus>
        get() = WorkoutFocus.entries.filter { candidate ->
            candidate !in selectedFocuses &&
                    selectedFocuses.any { it.conflictsWith(candidate) }
        }.toSet()

    val matchingRoutine: Routine?
        get() {
            val current = session?.sets?.map { it.exerciseId }?.distinct()?.toSet() ?: return null
            if (current.isEmpty()) return null
            return routines.firstOrNull { it.exerciseIds.toSet() == current }
        }

    val timerState: TimerState
        get() = when {
            setTimerMillis == 0L -> TimerState.INACTIVE
            timerPaused -> TimerState.PAUSED
            else -> TimerState.RUNNING
        }

    val suggestion: String?
        get() {
            val last = lastSets.firstOrNull() ?: return null
            if (last.weightAddedKg <= 0f) return "${last.reps + 1} reps"
            val sameWeight = lastSets.filter { it.weightAddedKg == last.weightAddedKg }
            val perSession = sameWeight.groupBy { it.sessionId }
                .entries.sortedByDescending { it.key }
                .map { it.value.maxOf { s -> s.reps } }
            val ceilingReached = perSession.size >= 3 && perSession.first() - perSession.last() >= 3
            return if (ceilingReached) {
                val w = last.weightAddedKg + 2.5f
                "${perSession.last()} reps +${fmtWeight(w)}kg ↑ peso"
            } else {
                "${perSession.first() + 1} reps +${fmtWeight(last.weightAddedKg)}kg"
            }
        }

    fun exerciseNameOf(exerciseId: Long): String =
        allExerciseNames[exerciseId] ?: "—"
}

private data class InputBundle(
    val reps: Int,
    val weightKg: Float,
    val startMillis: Long?,
    val durationSeconds: Int,
    val intensity: Int,
    val setTimerMillis: Long,
    val timerPaused: Boolean,
    val pausedElapsed: Long,
    val prCelebrationText: String?
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
    private val calculateWeeklyStreak: CalculateWeeklyStreakUseCase,
    private val routineRepository: RoutineRepository,
    private val pendingRoutineStart: PendingRoutineStart,
    private val widgetUpdater: WidgetUpdater
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
    private val setTimerStartMillis = MutableStateFlow(0L)
    private val timerPaused = MutableStateFlow(false)
    private val pausedElapsed = MutableStateFlow(0L)
    private val _prCelebration = MutableStateFlow<String?>(null)
    private val celebratedExercises = mutableSetOf<Long>()
    private val activeRoutineExerciseIds = MutableStateFlow<List<Long>>(emptyList())

    private val _finishedSessionId = MutableStateFlow<Long?>(null)
    val finishedSessionId: StateFlow<Long?> = _finishedSessionId

    private val _routineFeedback = MutableSharedFlow<String>()
    val routineFeedback: SharedFlow<String> = _routineFeedback.asSharedFlow()

    // Integridad de sets: mensaje transitorio de bloqueo (reps 0) y diálogo de
    // confirmación de peso 0 (aviso suave, no bloquea).
    private val _setBlockedMessage = MutableSharedFlow<String>()
    val setBlockedMessage: SharedFlow<String> = _setBlockedMessage.asSharedFlow()


    private val _pendingZeroWeightConfirm = MutableStateFlow(false)
    val pendingZeroWeightConfirm: StateFlow<Boolean> = _pendingZeroWeightConfirm

    // Meta de descanso (segundos) leída de prefs; default 90. Se editará en Ajustes.
    val restGoalSeconds: StateFlow<Int> =
        userPrefsRepository.getRestGoalSeconds()
            .stateIn(viewModelScope, SharingStarted.Eagerly, 90)

    // Nº de sets históricos por ejercicio, para la sección "Frecuentes" del selector.
    val setCountsByExercise: StateFlow<Map<Long, Int>> =
        sessionRepository.getSetCountsByExercise()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    // Hay una rutina cargada en la sesión (el selector muestra solo sus ejercicios).
    val routineLoaded: StateFlow<Boolean> =
        activeRoutineExerciseIds.map { it.isNotEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val muscleGroups = listOf(
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazo", "Trapecios", "Piernas", "Femoral", "Gastrocnemio"
    )

    private val today = LocalDate.now().toEpochDay()
    private val allExercises = exerciseRepository.getAll()

    private val exercises = combine(
        searchQuery, sessionRepository.getActiveSession(), activeRoutineExerciseIds
    ) { query, session, routineIds ->
        Triple(query, WorkoutFocus.fromStored(session?.focus), routineIds)
    }.flatMapLatest { (query, focuses, routineIds) ->
        val source = if (query.isBlank()) exerciseRepository.getAll() else exerciseRepository.search(query)
        source.map { list ->
            when {
                query.isNotBlank() -> list
                routineIds.isNotEmpty() -> list.filter { it.id in routineIds }.sortedBy { routineIds.indexOf(it.id) }
                else -> filterByFocus(list, focuses)
            }
        }
    }

    private fun filterByFocus(list: List<Exercise>, focuses: Set<WorkoutFocus>): List<Exercise> {
        val groups = focuses.flatMap { it.muscleGroups }.toSet()
        return if (groups.isEmpty()) list else list.filter { it.mainMuscleGroup in groups }
    }

    private val homeData = combine(
        sessionRepository.getLastFinishedSession(),
        statisticsRepository.getAllTrainedDays(),
        userPrefsRepository.getWeeklyGoal(),
        hydrationRepository.getTotalMlForDay(today),
        hydrationRepository.getDailyGoal(today),
        routineRepository.getRoutines()
    ) { args ->
        val last = args[0] as Session?
        @Suppress("UNCHECKED_CAST")
        val trainedDays = args[1] as List<Long>
        val weeklyGoal = args[2] as Int
        val totalMl = args[3] as Int
        val goal = args[4] as DailyHydrationGoal?
        @Suppress("UNCHECKED_CAST")
        val routines = args[5] as List<Routine>
        val weekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        HomeData(
            lastSession = last,
            streak = calculateWeeklyStreak(trainedDays, weeklyGoal),
            hydrationProgress = goal?.totalGoalMl?.takeIf { it > 0 }?.let { (totalMl.toFloat() / it).coerceAtLeast(0f) } ?: 0f,
            trainedDaysThisWeek = trainedDays.filter { it >= weekStart }.toSet(),
            routines = routines
        )
    }

    val uiState: StateFlow<ActiveWorkoutUiState> = combine(
        sessionRepository.getActiveSession(),
        exercises,
        searchQuery,
        combine(selectedExercise, selectedFocuses, lastSets) { e, f, l -> Triple(e, f, l) },
        combine(reps, weightKg, sessionStartMillis, durationSeconds, intensity, setTimerStartMillis, timerPaused, pausedElapsed, _prCelebration) { args ->
            InputBundle(
                reps = args[0] as Int,
                weightKg = args[1] as Float,
                startMillis = args[2] as Long?,
                durationSeconds = args[3] as Int,
                intensity = args[4] as Int,
                setTimerMillis = args[5] as Long,
                timerPaused = args[6] as Boolean,
                pausedElapsed = args[7] as Long,
                prCelebrationText = args[8] as String?
            )
        },
        allExercises,
        homeData
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
            setTimerMillis = inputs.setTimerMillis,
            timerPaused = inputs.timerPaused,
            pausedElapsed = inputs.pausedElapsed,
            prCelebrationText = inputs.prCelebrationText,
            routines = home.routines
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ActiveWorkoutUiState())

    /** ¿El atrás en Entrenamiento debe volver a Rutinas? (arrancó una rutina desde ahí). */
    val returnToRoutines: StateFlow<Boolean> = pendingRoutineStart.returnToRoutines
    fun disarmReturnToRoutines() = pendingRoutineStart.disarmReturn()

    init {
        // Arranca la rutina cuando la pestaña Rutinas lo pide (evento reactivo, no one-shot).
        viewModelScope.launch {
            pendingRoutineStart.startEvents.collect { startRoutineById(it) }
        }
        viewModelScope.launch {
            var hadSession = false
            sessionRepository.getActiveSession().collect { session ->
                if (session != null) {
                    hadSession = true
                    if (activeRoutineExerciseIds.value.isNotEmpty() && selectedExercise.value == null) {
                        val firstId = activeRoutineExerciseIds.value.first()
                        exerciseRepository.getAll().first().find { it.id == firstId }?.let { onExerciseSelected(it) }
                    }
                } else if (hadSession) {
                    hadSession = false
                    activeRoutineExerciseIds.value = emptyList()
                }
            }
        }
    }

    fun onFocusToggle(focus: WorkoutFocus) {
        val current = selectedFocuses.value
        selectedFocuses.value = when {
            focus in current -> current - focus
            current.any { it.conflictsWith(focus) } -> current
            else -> current + focus
        }
    }

    fun onSearchQueryChange(query: String) { searchQuery.value = query }

    fun onExerciseSelected(exercise: Exercise) {
        selectedExercise.value = exercise
        searchQuery.value = ""
        viewModelScope.launch {
            val previous = sessionRepository.getLastSetsForExercise(exercise.id, 50)
            lastSets.value = previous
            val last = previous.firstOrNull() ?: return@launch
            if (last.weightAddedKg > 0f) {
                val sameWeight = previous.filter { it.weightAddedKg == last.weightAddedKg }
                val perSession = sameWeight.groupBy { it.sessionId }
                    .entries.sortedByDescending { it.key }
                    .map { it.value.maxOf { s -> s.reps } }
                val ceilingReached = perSession.size >= 3 && perSession.first() - perSession.last() >= 3
                val lastSessionId = previous.first().sessionId
                val lastSessionMax = previous.filter { it.sessionId == lastSessionId }.maxOf { it.reps }
                if (ceilingReached) {
                    weightKg.value = last.weightAddedKg + 2.5f
                    reps.value = perSession.last()
                } else {
                    weightKg.value = last.weightAddedKg
                    reps.value = lastSessionMax + 1
                }
            } else {
                reps.value = last.reps + 1
                weightKg.value = 0f
            }
        }
    }

    fun onToggleTimerPause() {
        if (setTimerStartMillis.value == 0L) return
        if (timerPaused.value) {
            setTimerStartMillis.value = System.currentTimeMillis() - pausedElapsed.value
            timerPaused.value = false
        } else {
            pausedElapsed.value = System.currentTimeMillis() - setTimerStartMillis.value
            timerPaused.value = true
        }
    }

    fun onCreateExercise(name: String, muscleGroup: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = exerciseRepository.create(name, muscleGroup)
            searchQuery.value = ""
            onExerciseSelected(Exercise(id, name.trim(), "Personalizado", "Personalizado", muscleGroup))
        }
    }

    fun onRepsDelta(delta: Int) { reps.value = (reps.value + delta).coerceAtLeast(1) }
    fun onWeightDelta(delta: Float) { weightKg.value = (weightKg.value + delta).coerceAtLeast(0f) }

    fun onStartSession() {
        viewModelScope.launch {
            sessionRepository.startSession(LocalDate.now().toEpochDay(), WorkoutFocus.toStored(selectedFocuses.value))
            selectedFocuses.value = emptySet()
        }
    }

    fun onStartRoutine(routine: Routine) {
        activeRoutineExerciseIds.value = routine.exerciseIds
        viewModelScope.launch {
            sessionRepository.startSession(LocalDate.now().toEpochDay(), routine.focus)
        }
    }

    /** Carga la rutina por id (desde el holder de arranque) y la inicia en esta instancia. */
    private fun startRoutineById(routineId: Long) {
        viewModelScope.launch {
            routineRepository.getRoutines().first().firstOrNull { it.id == routineId }?.let(::onStartRoutine)
        }
    }

    /** Carga una rutina en la sesión YA activa: el selector pasa a mostrar sus ejercicios. */
    fun onLoadRoutine(routine: Routine) {
        activeRoutineExerciseIds.value = routine.exerciseIds
    }

    /** Suelta la rutina cargada: el selector vuelve a sesión libre (frecuentes por foco). */
    fun onClearRoutine() {
        activeRoutineExerciseIds.value = emptyList()
    }

    fun onSaveRoutine(name: String) {
        val session = uiState.value.session ?: return
        val exerciseIds = session.sets.map { it.exerciseId }.distinct()
        if (exerciseIds.isEmpty() || name.isBlank()) return
        viewModelScope.launch {
            when (val result = routineRepository.saveRoutine(name.trim(), session.focus, exerciseIds)) {
                is SaveRoutineResult.Created -> _routineFeedback.emit("Rutina \"${name.trim()}\" guardada")
                is SaveRoutineResult.Duplicate -> _routineFeedback.emit("Ya existe como \"${result.existingName}\"")
            }
        }
    }

    fun onDeleteRoutine(routineId: Long) {
        viewModelScope.launch { routineRepository.deleteRoutine(routineId) }
    }

    // Arranca manualmente el descanso. Antes escribía en sessionStartMillis (duración de
    // sesión), no en setTimerStartMillis, así que el botón no movía el temporizador visible.
    fun onStartTimer() {
        setTimerStartMillis.value = System.currentTimeMillis()
        timerPaused.value = false
        pausedElapsed.value = 0L
    }

    fun onRegisterSet() {
        val exercise = selectedExercise.value ?: return
        val isCardio = exercise.mainMuscleGroup.equals("Cardio", ignoreCase = true)
        // Integridad de sets: cardio usa minutos/nivel, no valida reps/peso.
        if (!isCardio) {
            when (validateSet(exercise, reps.value, weightKg.value)) {
                SetValidation.BlockedZeroReps -> {
                    _setBlockedMessage.tryEmit("Añade al menos 1 repetición para registrar")
                    return
                }
                SetValidation.ConfirmZeroWeight -> {
                    _pendingZeroWeightConfirm.value = true
                    return
                }
                SetValidation.Valid -> Unit
            }
        }
        registerSet()
    }

    /** Confirma registrar el set a 0 kg tras el aviso suave de peso. */
    fun onConfirmZeroWeight() {
        _pendingZeroWeightConfirm.value = false
        registerSet()
    }

    fun onDismissZeroWeight() {
        _pendingZeroWeightConfirm.value = false
    }

    private fun registerSet() {
        val now = System.currentTimeMillis()
        if (now - lastRegisteredSetAt.value < REGISTER_DEBOUNCE_MS) return
        val session = uiState.value.session ?: return
        val exercise = selectedExercise.value ?: return
        val isCardio = exercise.mainMuscleGroup.equals("Cardio", ignoreCase = true)
        val restSeconds = when {
            setTimerStartMillis.value == 0L -> 0
            timerPaused.value -> (pausedElapsed.value / 1000).toInt()
            else -> ((now - setTimerStartMillis.value) / 1000).toInt()
        }
        lastRegisteredSetAt.value = now
        setTimerStartMillis.value = now
        timerPaused.value = false
        pausedElapsed.value = 0L
        val currentReps = reps.value
        val currentWeight = weightKg.value
        viewModelScope.launch {
            var prText: String? = null
            if (!isCardio && exercise.id !in celebratedExercises) {
                val historicalSets = sessionRepository.getLastSetsForExercise(exercise.id, 100)
                if (historicalSets.size >= 3) {
                    if (currentWeight > 0f) {
                        val maxW = sessionRepository.getMaxWeightForExercise(exercise.id) ?: 0f
                        if (currentWeight > maxW && maxW > 0f) {
                            val delta = currentWeight - maxW
                            prText = "PR +${if (delta % 1f == 0f) "${delta.toInt()}" else "%.1f".format(delta)}kg"
                        }
                    } else {
                        val maxR = sessionRepository.getMaxRepsBodyweight(exercise.id) ?: 0
                        if (currentReps > maxR && maxR > 0) {
                            prText = "PR +${currentReps - maxR} reps"
                        }
                    }
                }
            }
            sessionRepository.addSet(
                sessionId = session.id,
                exerciseId = exercise.id,
                reps = if (isCardio) 0 else currentReps,
                weightAddedKg = if (isCardio) 0f else currentWeight,
                durationSeconds = if (isCardio) durationSeconds.value else 0,
                intensity = if (isCardio) intensity.value else 0,
                restSeconds = restSeconds,
                isPr = prText != null
            )
            if (prText != null) {
                celebratedExercises.add(exercise.id)
                _prCelebration.value = prText
                delay(2500)
                _prCelebration.value = null
            }
        }
    }

    fun onDeleteSet(set: WorkoutSet) { viewModelScope.launch { sessionRepository.deleteSet(set) } }

    fun onDiscardSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            sessionRepository.deleteSession(session.id)
            sessionStartMillis.value = null
            selectedExercise.value = null
            celebratedExercises.clear()
            pendingRoutineStart.disarmReturn()
        }
    }

    fun onFinishSession() {
        val session = uiState.value.session ?: return
        viewModelScope.launch {
            if (session.sets.isEmpty()) {
                sessionRepository.deleteSession(session.id)
            } else {
                sessionRepository.finishSession(session)
            }
            widgetUpdater.refresh()
            sessionStartMillis.value = null
            setTimerStartMillis.value = 0L
            timerPaused.value = false
            pausedElapsed.value = 0L
            selectedExercise.value = null
            celebratedExercises.clear()
            if (session.sets.isNotEmpty()) _finishedSessionId.value = session.id
            pendingRoutineStart.disarmReturn()
        }
    }

    fun onFinishedSessionConsumed() { _finishedSessionId.value = null }
    fun onDurationDelta(delta: Int) { durationSeconds.value = (durationSeconds.value + delta).coerceAtLeast(0) }
    fun onIntensityDelta(delta: Int) { intensity.value = (intensity.value + delta).coerceIn(1, 10) }

    fun onWeeklyGoalChange(goal: Int) {
        viewModelScope.launch {
            userPrefsRepository.setWeeklyGoal(goal.coerceIn(1, 7))
            widgetUpdater.refresh()
        }
    }

    private companion object { const val REGISTER_DEBOUNCE_MS = 700L }
}