package com.luis.itera.presentation.statistics


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.R
import com.luis.itera.data.local.dao.WeeklyVolumeRow
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.presentation.components.DensityPoint
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
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val selectedExercise = MutableStateFlow<Exercise?>(null)
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val range = MutableStateFlow(StatsRange.D30)

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

    private val filteredExercises = combine(
        exerciseRepository.getAll(),
        selectedGroup
    ) { exercises, group ->
        if (group == null) exercises else exercises.filter { it.mainMuscleGroup == group }
    }

    private val defaultExercise = combine(
        statisticsRepository.getLastExercisedId(),
        exerciseRepository.getAll(),
        selectedExercise
    ) { lastId, exercises, manual ->
        manual ?: lastId?.let { id -> exercises.find { it.id == id } }
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

    private companion object {
        val shortDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
    }
}
