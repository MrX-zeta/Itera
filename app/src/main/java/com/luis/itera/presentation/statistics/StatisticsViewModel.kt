package com.luis.itera.presentation.statistics


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.domain.model.ModalityActivity
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.TrainingModality
import com.luis.itera.domain.model.WeeklyStreak
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.model.bestReliableOneRepMax
import com.luis.itera.domain.model.strengthChangePercent
import com.luis.itera.domain.model.toModality
import com.luis.itera.R
import com.luis.itera.data.local.dao.WeeklyVolumeRow
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
import com.luis.itera.domain.usecase.GetActiveModalitiesUseCase
import com.luis.itera.presentation.components.DensityPoint
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class StatsRange(val label: String, val days: Long) {
    D30("30D", 30),
    D90("90D", 90),
    ALL("TODO", 36500)
}

data class FocusCount(val focus: WorkoutFocus, val count: Int)

private data class SeriesBundle(
    val maxSeries: List<ExerciseSeriesPoint> = emptyList(),
    val volumeSeries: List<ExerciseSeriesPoint> = emptyList(),
    val isBodyweight: Boolean = false
)

enum class VolumeTrend(val label: String?, val iconRes: Int?) {
    NONE(null, null),
    RISING("progresando", R.drawable.ic_trend_up),
    STABLE("estable", R.drawable.ic_trend_flat),
    FALLING("bajando", R.drawable.ic_trend_down),
    DELOAD("deload detectado", null)
}

/** Un grupo muscular que hace tiempo que no se entrena, para el bloque Equilibrio. */
data class MuscleGroupNeglect(val group: String, val daysSince: Int)

/** Fragmento de la línea de evidencia; [positive] = cifra de MEJORA, se pinta en acento. */
data class EvidenceSegment(val text: String, val positive: Boolean = false)

/** Estado de la vista GENERAL (fija): "¿progresé?", constancia y equilibrio. */
data class GeneralUiState(
    val hasAnyData: Boolean = false,
    val streak: WeeklyStreak = WeeklyStreak(0, 0, 3),
    val avgDaysPerWeek: Float = 0f,
    val totalTrainedDays: Int = 0,
    val headline: String = "",
    val evidence: List<EvidenceSegment>? = null,
    val neglected: List<MuscleGroupNeglect> = emptyList()
)

/** Tendencia de un movimiento en el top de Fuerza (↑ mejorando / → estable / ↓ bajando). */
enum class MovementTrend { RISING, STABLE, FALLING }

data class TopMovement(val name: String, val trend: MovementTrend, val changePercent: Float)

/**
 * Estimación de fuerza del ejercicio seleccionado. [changePercent] = variación de 1RM
 * estimado (reciente vs previo). [absoluteOneRmKg] SOLO se rellena en los 3 básicos.
 */
data class StrengthEstimate(
    val changePercent: Float?,
    val absoluteOneRmKg: Float?,
    val isBasic: Boolean
)

/** Estado de la vista FUERZA: estimación del ejercicio elegido + top de movimientos. */
data class StrengthUiState(
    val estimate: StrengthEstimate? = null,
    val topMovements: List<TopMovement> = emptyList()
)

/**
 * Métricas mensuales de la modalidad seleccionada (Calistenia / Cardio). Solo se rellenan los
 * campos de la modalidad activa; ventana rodante de 30 días ("este mes") vs los 30 previos.
 */
data class ModalityStatsUiState(
    val modality: TrainingModality? = null,
    // Calistenia
    val topRepsMovements: List<TopMovement> = emptyList(),
    // Cardio
    val minutesThisMonth: Int = 0,
    val intensityAvg: Float = 0f,
    val intensityAvgLastMonth: Float = 0f,
    val cardioSummary: String? = null
)

data class StatisticsUiState(
    val focusCounts: List<FocusCount> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val selectedGroup: String? = null,
    val range: StatsRange = StatsRange.D30,
    val maxWeightSeries: List<ExerciseSeriesPoint> = emptyList(),
    val volumeSeries: List<ExerciseSeriesPoint> = emptyList(),
    val isBodyweightMode: Boolean = false,
    val densityPoints: List<DensityPoint> = emptyList(),
    val maxWeeklyVolume: Float = 0f,
    val volumeTrend: VolumeTrend = VolumeTrend.NONE,
    val sessionsInRange: Int = 0,
    val hasMultiFocusSessions: Boolean = false
) {
    val personalRecord: Float?
        get() = maxWeightSeries.maxOfOrNull { it.value }
    val totalVolume: Float
        get() = volumeSeries.map { it.value }.sum()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository,
    private val exerciseRepository: ExerciseRepository,
    private val sessionRepository: SessionRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateWeeklyStreak: CalculateWeeklyStreakUseCase,
    getActiveModalities: GetActiveModalitiesUseCase
) : ViewModel() {

    /** Modalidades activas (reciente+significativo) que definen qué pestañas se muestran. */
    val activeModalities: StateFlow<List<ModalityActivity>> = getActiveModalities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Ventana de 2 semanas (lunes de la semana pasada) para comparar esta semana vs la anterior.
    private val lastWeekMonday = LocalDate.now()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks(1)
        .toEpochDay()

    /** Estado de la vista GENERAL: "¿progresé?", constancia (racha + días) y equilibrio. */
    val generalState: StateFlow<GeneralUiState> = combine(
        statisticsRepository.getAllTrainedDays(),
        userPrefsRepository.getWeeklyGoal(),
        statisticsRepository.getWeeklyVolume(),
        sessionRepository.getFinishedSessionsSince(lastWeekMonday),
        statisticsRepository.getLastTrainedDayByMuscleGroup()
    ) { trainedDaysList, weeklyGoal, weeklyVolume, recentSessions, lastByGroup ->
        buildGeneralState(trainedDaysList, weeklyGoal, weeklyVolume, recentSessions, lastByGroup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GeneralUiState())

    private val selectedExercise = MutableStateFlow<Exercise?>(null)
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val selectedModality = MutableStateFlow<TrainingModality?>(null)
    private val range = MutableStateFlow(StatsRange.D30)

    // Ventanas fijas (calculadas una vez): ejercicio destacado por modalidad y métricas mensuales.
    private val featuredWindowStart = LocalDate.now().minusDays(90).toEpochDay()
    private val twoMonthsStart = LocalDate.now().minusDays(60).toEpochDay()

    val muscleGroups = listOf(
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazo", "Trapecios", "Piernas", "Femoral", "Gastrocnemio",
        "Cardio"
    )

    init {
        viewModelScope.launch {
            statisticsRepository.getLastExercisedId()
                .distinctUntilChanged()
                .collect { selectedExercise.value = null }
        }
    }

    private val focusCounts: Flow<List<FocusCount>> = range
        .flatMapLatest { r -> statisticsRepository.getFocusList(fromEpochDay(r)) }
        .map(::countFocuses)

    private val sessionsInRange: Flow<Int> = range
        .flatMapLatest { r -> statisticsRepository.getFinishedSessionCount(fromEpochDay(r)) }

    private val focusData: Flow<Triple<List<FocusCount>, Int, Boolean>> =
        combine(focusCounts, sessionsInRange) { counts, total ->
            Triple(counts, total, counts.sumOf { it.count } > total)
        }

    // El picker filtra por modalidad de la pestaña activa (y por grupo si se eligió).
    private val filteredExercises = combine(
        exerciseRepository.getAll(),
        selectedGroup,
        selectedModality
    ) { exercises, group, modality ->
        exercises
            .filter { modality == null || it.toModality() == modality }
            .filter { group == null || it.mainMuscleGroup == group }
    }

    // Ejercicio destacado más reciente por modalidad (para el default de cada pestaña).
    private val lastExercisedByModality: Flow<Map<TrainingModality, Exercise>> = combine(
        sessionRepository.getFinishedSessionsSince(featuredWindowStart),
        exerciseRepository.getAll()
    ) { sessions, exercises ->
        val byId = exercises.associateBy { it.id }
        val result = LinkedHashMap<TrainingModality, Exercise>()
        for (session in sessions) {          // ordenadas por fecha DESC: la primera vista es la más reciente
            for (set in session.sets) {
                val ex = byId[set.exerciseId] ?: continue
                result.putIfAbsent(ex.toModality(), ex)
            }
        }
        result
    }

    private val defaultExercise = combine(
        selectedExercise,
        selectedModality,
        lastExercisedByModality,
        statisticsRepository.getLastExercisedId(),
        exerciseRepository.getAll()
    ) { manual, modality, lastByModality, lastId, exercises ->
        when {
            manual != null -> manual
            modality != null -> lastByModality[modality]
            else -> lastId?.let { id -> exercises.find { it.id == id } }
        }
    }

    private val series = combine(defaultExercise, range) { exercise, r -> exercise to r }
        .flatMapLatest { (exercise, r) ->
            if (exercise == null) flowOf(SeriesBundle())
            else {
                val from = LocalDate.now().minusDays(r.days).toEpochDay()
                statisticsRepository.hasWeightedSets(exercise.id, from)
                    .flatMapLatest { weighted ->
                        if (weighted) {
                            combine(
                                statisticsRepository.getMaxWeightSeries(exercise.id, from),
                                statisticsRepository.getVolumeSeries(exercise.id, from)
                            ) { max, vol -> SeriesBundle(max, vol, isBodyweight = false) }
                        } else {
                            combine(
                                statisticsRepository.getMaxRepsSeries(exercise.id, from),
                                statisticsRepository.getTotalRepsSeries(exercise.id, from)
                            ) { max, vol -> SeriesBundle(max, vol, isBodyweight = true) }
                        }
                    }
            }
        }

    private val weeklyVolume = statisticsRepository.getWeeklyVolume()
    private val maxWeeklyVolume = statisticsRepository.getMaxWeeklyVolume()

    /**
     * Estado de la vista FUERZA. Reutiliza [SessionRepository.getFinishedSessionsSince] (6A)
     * para los sets crudos (reps+peso) por ventana, sin queries nuevas: parte la ventana en dos
     * mitades (previa/reciente) y aplica las funciones puras de 6A (1RM Epley, variación %).
     */
    val strengthState: StateFlow<StrengthUiState> = combine(defaultExercise, range) { e, r -> e to r }
        .flatMapLatest { (exercise, r) ->
            val from = LocalDate.now().minusDays(r.days).toEpochDay()
            combine(
                sessionRepository.getFinishedSessionsSince(from),
                exerciseRepository.getAll()
            ) { sessions, exercises ->
                buildStrengthState(exercise, sessions, exercises, from)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StrengthUiState())

    /**
     * Métricas mensuales de Calistenia / Cardio para la modalidad seleccionada. Se computa desde
     * [SessionRepository.getFinishedSessionsSince] (sin queries nuevas), derivando la modalidad de
     * los ejercicios reales. Ventana "este mes" (30 días) vs los 30 previos.
     */
    val modalityStatsState: StateFlow<ModalityStatsUiState> = combine(
        selectedModality,
        sessionRepository.getFinishedSessionsSince(twoMonthsStart),
        exerciseRepository.getAll()
    ) { modality, sessions, exercises ->
        buildModalityStats(modality, sessions, exercises)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModalityStatsUiState())

    val uiState: StateFlow<StatisticsUiState> = combine(
        focusData,
        filteredExercises,
        combine(defaultExercise, selectedGroup, range) { e, g, r -> Triple(e, g, r) },
        series,
        weeklyVolume,
        maxWeeklyVolume
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val focus = args[0] as Triple<List<FocusCount>, Int, Boolean>
        val exercises = args[1] as List<Exercise>
        val selection = args[2] as Triple<Exercise?, String?, StatsRange>
        val seriesData = args[3] as SeriesBundle
        val weekly = args[4] as List<WeeklyVolumeRow>
        val maxWeekly = args[5] as Float

        val density = weekly.mapIndexed { i, row ->
            DensityPoint(labelFor(i, row.weekStart, selection.third), row.totalVolume)
        }

        StatisticsUiState(
            focusCounts = focus.first,
            exercises = exercises,
            selectedExercise = selection.first,
            selectedGroup = selection.second,
            range = selection.third,
            maxWeightSeries = seriesData.maxSeries,
            volumeSeries = seriesData.volumeSeries,
            isBodyweightMode = seriesData.isBodyweight,
            densityPoints = density,
            maxWeeklyVolume = maxWeekly,
            volumeTrend = computeVolumeTrend(density),
            sessionsInRange = focus.second,
            hasMultiFocusSessions = focus.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun onExerciseSelected(exercise: Exercise) { selectedExercise.value = exercise }
    fun onGroupSelected(group: String?) { selectedGroup.value = if (selectedGroup.value == group) null else group }
    fun onRangeSelected(newRange: StatsRange) { range.value = newRange }

    /** La pantalla informa qué pestaña de modalidad está activa (null = General). Al cambiar,
     *  reinicia la selección para que cada pestaña muestre su ejercicio destacado. */
    fun onModalitySelected(modality: TrainingModality?) {
        if (selectedModality.value != modality) {
            selectedModality.value = modality
            selectedExercise.value = null
            selectedGroup.value = null
        }
    }

    private fun fromEpochDay(r: StatsRange): Long =
        LocalDate.now().minusDays(r.days).toEpochDay()

    private fun countFocuses(stored: List<String>): List<FocusCount> {
        val counts = stored.flatMap { WorkoutFocus.fromStored(it) }
            .groupingBy { it }
            .eachCount()
        return WorkoutFocus.entries
            .map { FocusCount(it, counts[it] ?: 0) }
            .sortedByDescending { it.count }
    }

    private fun labelFor(index: Int, weekStart: Long, range: StatsRange): String = when {
        range == StatsRange.ALL && index > 8 -> LocalDate.ofEpochDay(weekStart).format(shortDateFmt)
        index == 0 -> "Esta semana"
        else -> "Hace $index sem"
    }

    private fun computeVolumeTrend(points: List<DensityPoint>): VolumeTrend {
        if (points.size < 3) return VolumeTrend.NONE
        val daysElapsed = LocalDate.now().dayOfWeek.value
        if (daysElapsed <= 2) return VolumeTrend.NONE
        val current = points.first().volumeKg
        val projectedCurrent = if (daysElapsed >= 7) current else current / daysElapsed * 7f
        val previous = points.drop(1).map { it.volumeKg }.average().toFloat()
        if (previous <= 0f) return VolumeTrend.NONE
        val ratio = projectedCurrent / previous
        return when {
            ratio >= 1.1f -> VolumeTrend.RISING
            ratio <= 0.6f -> VolumeTrend.DELOAD
            ratio <= 0.8f -> VolumeTrend.FALLING
            else -> VolumeTrend.STABLE
        }
    }

    private fun buildGeneralState(
        trainedDaysList: List<Long>,
        weeklyGoal: Int,
        weeklyVolume: List<WeeklyVolumeRow>,
        recentSessions: List<Session>,
        lastByGroup: Map<String, Long>
    ): GeneralUiState {
        val hasAnyData = trainedDaysList.isNotEmpty()
        val streak = calculateWeeklyStreak(trainedDaysList, weeklyGoal)
        val trend = computeVolumeTrend(weeklyVolume.map { DensityPoint("", it.totalVolume) })

        val today = LocalDate.now()
        val thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
        val lastMonday = LocalDate.ofEpochDay(thisMonday).minusWeeks(1).toEpochDay()
        val thisWeek = recentSessions.filter { it.dateEpochDay >= thisMonday }
        val lastWeek = recentSessions.filter { it.dateEpochDay in lastMonday until thisMonday }
        val daysThis = thisWeek.map { it.dateEpochDay }.distinct().size
        val daysLast = lastWeek.map { it.dateEpochDay }.distinct().size
        val minutesThis = thisWeek.sumOf { it.durationMinutes }
        val minutesLast = lastWeek.sumOf { it.durationMinutes }

        // Constancia de LARGO PLAZO (Home ya muestra la semana): promedio y total histórico.
        val totalTrainedDays = trainedDaysList.size
        val avgDaysPerWeek = if (hasAnyData) {
            val spanDays = (today.toEpochDay() - trainedDaysList.min() + 1).coerceAtLeast(1)
            totalTrainedDays / (spanDays / 7f).coerceAtLeast(1f)
        } else 0f

        val todayEpoch = today.toEpochDay()
        val neglected = lastByGroup
            .filterKeys { !it.equals("Cardio", ignoreCase = true) }
            .map { (group, last) -> MuscleGroupNeglect(group, (todayEpoch - last).toInt()) }
            .filter { it.daysSince >= NEGLECT_MIN_DAYS }
            .sortedByDescending { it.daysSince }
            .take(3)

        return GeneralUiState(
            hasAnyData = hasAnyData,
            streak = streak,
            avgDaysPerWeek = avgDaysPerWeek,
            totalTrainedDays = totalTrainedDays,
            headline = headlineFor(trend, streak, hasAnyData),
            evidence = evidenceFor(trend, daysThis, daysLast, minutesThis, minutesLast, lastWeek.isNotEmpty(), today.dayOfWeek.value),
            neglected = neglected
        )
    }

    private fun headlineFor(trend: VolumeTrend, streak: WeeklyStreak, hasAnyData: Boolean): String = when {
        !hasAnyData -> ""
        trend == VolumeTrend.RISING -> "Vas progresando"
        trend == VolumeTrend.DELOAD -> "Semana de descarga"
        trend == VolumeTrend.FALLING -> "Bajó tu ritmo"
        trend == VolumeTrend.STABLE -> "Te mantienes constante"
        streak.sessionsThisWeek > 0 -> "En marcha"
        else -> "Comienza tu semana"
    }

    /**
     * Evidencia concreta bajo el veredicto. Al principio de la semana (pocos días transcurridos)
     * NO comparamos contra una semana completa —siempre saldría "menos"—: mostramos el estado
     * actual sin comparación. Solo comparamos week-over-week cuando la semana ya avanzó.
     *
     * El titular sale del volumen en kg ([trend]); esta evidencia, de días/minutos — señales
     * independientes que pueden apuntar en direcciones opuestas (más días pero menos carga
     * levantada, o al revés). Si diverge del titular, se dice con honestidad en vez de sonar
     * contradictoria (p.ej. "Bajó tu ritmo" + "2 días más" desmentiría al propio titular).
     */
    private fun evidenceFor(
        trend: VolumeTrend,
        daysThis: Int,
        daysLast: Int,
        minutesThis: Int,
        minutesLast: Int,
        hadLastWeek: Boolean,
        daysElapsedThisWeek: Int
    ): List<EvidenceSegment>? {
        if (daysElapsedThisWeek < 4) {
            return if (daysThis == 0) listOf(EvidenceSegment("aún sin entrenar esta semana"))
            else listOf(EvidenceSegment("llevas ${daysWord(daysThis)} esta semana"))
        }
        if (!hadLastWeek) {
            return if (daysThis == 0) null
            else listOf(EvidenceSegment("esta semana: ${daysWord(daysThis)} · $minutesThis min"))
        }
        val deltaDays = daysThis - daysLast
        val deltaMinutes = minutesThis - minutesLast

        val daysSignalUp = deltaDays > 0 || (deltaDays == 0 && deltaMinutes > 0)
        val daysSignalDown = deltaDays < 0 || (deltaDays == 0 && deltaMinutes < 0)
        when {
            (trend == VolumeTrend.FALLING || trend == VolumeTrend.DELOAD) && daysSignalUp ->
                return listOf(
                    EvidenceSegment("más días", positive = true),
                    EvidenceSegment(", pero con menos carga")
                )
            trend == VolumeTrend.RISING && daysSignalDown ->
                return listOf(
                    EvidenceSegment("menos días, pero "),
                    EvidenceSegment("más intenso", positive = true)
                )
        }

        // Cada delta se evalúa por separado: solo el POSITIVO se marca (acento en la UI);
        // el negativo queda en secundario, sin ningún otro color.
        val parts = buildList {
            if (deltaDays > 0) add(EvidenceSegment("${daysWord(deltaDays)} más", positive = true))
            else if (deltaDays < 0) add(EvidenceSegment("${daysWord(-deltaDays)} menos"))
            if (deltaMinutes > 0) add(EvidenceSegment("+$deltaMinutes min", positive = true))
            else if (deltaMinutes < 0) add(EvidenceSegment("$deltaMinutes min"))
        }
        if (parts.isEmpty()) return listOf(EvidenceSegment("mismo ritmo que la semana pasada"))
        return buildList {
            add(EvidenceSegment("esta semana: "))
            parts.forEachIndexed { i, part ->
                if (i > 0) add(EvidenceSegment(" y "))
                add(part)
            }
            add(EvidenceSegment(" vs la anterior"))
        }
    }

    private fun daysWord(n: Int): String = if (n == 1) "1 día" else "$n días"

    private fun buildStrengthState(
        selected: Exercise?,
        sessions: List<Session>,
        exercises: List<Exercise>,
        fromEpochDay: Long
    ): StrengthUiState {
        val exercisesById = exercises.associateBy { it.id }
        val today = LocalDate.now().toEpochDay()
        val midpoint = fromEpochDay + (today - fromEpochDay) / 2

        // Sets por ejercicio, separados en mitad previa vs reciente de la ventana.
        val prevByExercise = HashMap<Long, MutableList<WorkoutSet>>()
        val recentByExercise = HashMap<Long, MutableList<WorkoutSet>>()
        for (session in sessions) {
            val target = if (session.dateEpochDay >= midpoint) recentByExercise else prevByExercise
            for (set in session.sets) {
                target.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
            }
        }

        val estimate = selected?.let { ex ->
            val recentBest = bestReliableOneRepMax(recentByExercise[ex.id].orEmpty())
            val prevBest = bestReliableOneRepMax(prevByExercise[ex.id].orEmpty())
            val change = strengthChangePercent(recentBest, prevBest)
            val isBasic = isBasicLift(ex.name)
            // 1RM absoluto SOLO en los básicos, y solo si hay una estimación fiable en la ventana.
            val absolute = if (isBasic) {
                bestReliableOneRepMax(recentByExercise[ex.id].orEmpty() + prevByExercise[ex.id].orEmpty())
            } else null
            if (change == null && absolute == null) null
            else StrengthEstimate(changePercent = change, absoluteOneRmKg = absolute, isBasic = isBasic)
        }

        val topMovements = (recentByExercise.keys + prevByExercise.keys)
            .mapNotNull { id ->
                val ex = exercisesById[id] ?: return@mapNotNull null
                if (ex.toModality() != TrainingModality.STRENGTH) return@mapNotNull null
                val recentBest = bestReliableOneRepMax(recentByExercise[id].orEmpty())
                val prevBest = bestReliableOneRepMax(prevByExercise[id].orEmpty())
                val change = strengthChangePercent(recentBest, prevBest) ?: return@mapNotNull null
                val trend = when {
                    change >= TREND_THRESHOLD_PERCENT -> MovementTrend.RISING
                    change <= -TREND_THRESHOLD_PERCENT -> MovementTrend.FALLING
                    else -> MovementTrend.STABLE
                }
                TopMovement(ex.name, trend, change)
            }
            .sortedByDescending { it.changePercent }
            .take(5)

        return StrengthUiState(estimate, topMovements)
    }

    private fun isBasicLift(name: String): Boolean = name.trim().lowercase() in BASIC_LIFT_NAMES

    /**
     * Métricas de la modalidad seleccionada. Separa los sets (de esa modalidad) en "este mes"
     * (30 días) y los 30 previos. Calistenia → reps; Cardio → minutos e intensidad.
     */
    private fun buildModalityStats(
        modality: TrainingModality?,
        sessions: List<Session>,
        exercises: List<Exercise>
    ): ModalityStatsUiState {
        if (modality == null) return ModalityStatsUiState()
        val byId = exercises.associateBy { it.id }
        val today = LocalDate.now().toEpochDay()
        val monthStart = today - 30
        val prevStart = today - 60

        val thisMonth = ArrayList<WorkoutSet>()
        val lastMonth = ArrayList<WorkoutSet>()
        val recentByExercise = HashMap<Long, MutableList<WorkoutSet>>()
        val prevByExercise = HashMap<Long, MutableList<WorkoutSet>>()
        for (session in sessions) {
            for (set in session.sets) {
                val ex = byId[set.exerciseId] ?: continue
                if (ex.toModality() != modality) continue
                when {
                    session.dateEpochDay >= monthStart -> {
                        thisMonth.add(set)
                        recentByExercise.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
                    }
                    session.dateEpochDay >= prevStart -> {
                        lastMonth.add(set)
                        prevByExercise.getOrPut(set.exerciseId) { mutableListOf() }.add(set)
                    }
                }
            }
        }

        return when (modality) {
            TrainingModality.CALISTHENICS -> {
                // Progreso por EJERCICIO (más reps recientes vs previas), no reps mezcladas.
                val top = (recentByExercise.keys + prevByExercise.keys).mapNotNull { id ->
                    val ex = byId[id] ?: return@mapNotNull null
                    val recentMax = recentByExercise[id]?.maxOfOrNull { it.reps } ?: return@mapNotNull null
                    val prevMax = prevByExercise[id]?.maxOfOrNull { it.reps } ?: return@mapNotNull null
                    if (prevMax <= 0) return@mapNotNull null
                    val change = (recentMax - prevMax).toFloat() / prevMax * 100
                    val trend = when {
                        change >= TREND_THRESHOLD_PERCENT -> MovementTrend.RISING
                        change <= -TREND_THRESHOLD_PERCENT -> MovementTrend.FALLING
                        else -> MovementTrend.STABLE
                    }
                    TopMovement(ex.name, trend, change)
                }.sortedByDescending { it.changePercent }.take(5)
                ModalityStatsUiState(modality = modality, topRepsMovements = top)
            }
            TrainingModality.CARDIO -> {
                val intensThis = thisMonth.map { it.intensity }.filter { it > 0 }
                val intensLast = lastMonth.map { it.intensity }.filter { it > 0 }
                val minutesThis = thisMonth.sumOf { it.durationSeconds } / 60
                val minutesLast = lastMonth.sumOf { it.durationSeconds } / 60
                val avgThis = if (intensThis.isEmpty()) 0f else intensThis.average().toFloat()
                val avgLast = if (intensLast.isEmpty()) 0f else intensLast.average().toFloat()
                ModalityStatsUiState(
                    modality = modality,
                    minutesThisMonth = minutesThis,
                    intensityAvg = avgThis,
                    intensityAvgLastMonth = avgLast,
                    cardioSummary = cardioSummary(minutesThis, minutesLast, avgThis, avgLast)
                )
            }
            else -> ModalityStatsUiState(modality = modality)
        }
    }

    private fun cardioSummary(minutesThis: Int, minutesLast: Int, intensThis: Float, intensLast: Float): String? {
        if (minutesThis == 0) return null
        val moreTime = minutesThis > minutesLast
        val moreIntense = intensThis > intensLast + 0.1f
        return when {
            moreTime && moreIntense -> "Más tiempo y más intensidad que el mes pasado. Buena señal de resistencia."
            moreTime -> "Estás sumando más minutos que el mes pasado."
            moreIntense -> "Mismo tiempo, pero subiste la intensidad."
            minutesLast == 0 -> "Buen arranque de cardio este mes."
            else -> "Ritmo parecido al del mes pasado."
        }
    }

    private companion object {
        val shortDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
        const val NEGLECT_MIN_DAYS = 10
        const val TREND_THRESHOLD_PERCENT = 2.5f
        // Solo los 3 básicos de barra donde el 1RM absoluto significa algo. Nombres EXACTOS
        // del catálogo: excluye variantes (inclinado, rumano, sumo, Hack, búlgaras, pistol...).
        val BASIC_LIFT_NAMES = setOf("sentadilla con barra", "press banca", "peso muerto")
    }
}
