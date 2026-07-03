package com.luis.itera.presentation.statistics


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.domain.model.TopMovementRecord
import com.luis.itera.domain.model.WeeklyStreak
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
import com.luis.itera.presentation.components.DensityPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class StatsRange(val label: String, val days: Long) {
    D30("30D", 30),
    D90("90D", 90)
}

private data class SeriesBundle(
    val maxSeries: List<ExerciseSeriesPoint> = emptyList(),
    val volumeSeries: List<ExerciseSeriesPoint> = emptyList(),
    val isBodyweight: Boolean = false
)

data class StatisticsUiState(
    val sessionsThisMonth: Int = 0,
    val topFocus: String? = null,
    val streak: WeeklyStreak = WeeklyStreak(0, 0, 3),
    val topMovements: List<TopMovementRecord> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val selectedGroup: String? = null,
    val range: StatsRange = StatsRange.D30,
    val maxWeightSeries: List<ExerciseSeriesPoint> = emptyList(),
    val volumeSeries: List<ExerciseSeriesPoint> = emptyList(),
    val isBodyweightMode: Boolean = false,
    val densityPoints: List<DensityPoint> = emptyList()
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
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateWeeklyStreak: CalculateWeeklyStreakUseCase
) : ViewModel() {

    private val selectedExercise = MutableStateFlow<Exercise?>(null)
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val range = MutableStateFlow(StatsRange.D30)

    val muscleGroups = listOf(
        "Pecho", "Espalda", "Hombros", "Bíceps", "Tríceps",
        "Antebrazo", "Trapecios", "Piernas", "Femoral", "Gastrocnemio"
    )

    private val monthStart = LocalDate.now().withDayOfMonth(1).toEpochDay()

    private val summary = combine(
        statisticsRepository.getFinishedSessionCount(monthStart),
        statisticsRepository.getFocusList(monthStart),
        statisticsRepository.getAllTrainedDays(),
        userPrefsRepository.getWeeklyGoal()
    ) { count, focusList, trainedDays, weeklyGoal ->
        Triple(count, topFocusOf(focusList), calculateWeeklyStreak(trainedDays, weeklyGoal))
    }

    private val topMovements = combine(
        statisticsRepository.getTopExercises(3),
        exerciseRepository.getAll()
    ) { records, exercises ->
        val nameMap = exercises.associate { it.id to it.name }
        records.map { record ->
            TopMovementRecord(
                exerciseId = record.exerciseId,
                exerciseName = nameMap[record.exerciseId] ?: "—",
                displayValue = when {
                    record.isCardio -> "${record.maxDurationSeconds / 60} min"
                    record.hasWeight -> "${formatKg(record.estimated1RmKg)} kg"
                    else -> "${record.maxReps} reps"
                },
                displayLabel = when {
                    record.isCardio -> "MÁX DURACIÓN"
                    record.hasWeight -> "1RM EST"
                    else -> "MÁX REPS"
                }
            )
        }
    }

    private val filteredExercises = combine(
        exerciseRepository.getAll(),
        selectedGroup
    ) { exercises, group ->
        if (group == null) exercises else exercises.filter { it.mainMuscleGroup == group }
    }

    private val defaultExercise = combine(
        statisticsRepository.getMostTrainedExerciseId(),
        exerciseRepository.getAll(),
        selectedExercise
    ) { defaultId, exercises, manual ->
        manual ?: defaultId?.let { id -> exercises.find { it.id == id } }
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

    val uiState: StateFlow<StatisticsUiState> = combine(
        summary,
        topMovements,
        filteredExercises,
        combine(defaultExercise, selectedGroup, range) { e, g, r -> Triple(e, g, r) },
        series
    ) { summaryData, topMov, exercises, selection, seriesData ->
        StatisticsUiState(
            sessionsThisMonth = summaryData.first,
            topFocus = summaryData.second,
            streak = summaryData.third,
            topMovements = topMov,
            exercises = exercises,
            selectedExercise = selection.first,
            selectedGroup = selection.second,
            range = selection.third,
            maxWeightSeries = seriesData.maxSeries,
            volumeSeries = seriesData.volumeSeries,
            isBodyweightMode = seriesData.isBodyweight
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    fun onExerciseSelected(exercise: Exercise) { selectedExercise.value = exercise }
    fun onGroupSelected(group: String?) { selectedGroup.value = if (selectedGroup.value == group) null else group }
    fun onRangeSelected(newRange: StatsRange) { range.value = newRange }

    fun onWeeklyGoalDelta(delta: Int) {
        viewModelScope.launch {
            val current = userPrefsRepository.getWeeklyGoal().first()
            userPrefsRepository.setWeeklyGoal(current + delta)
        }
    }

    private fun topFocusOf(focusList: List<String>): String? =
        focusList.flatMap { WorkoutFocus.fromStored(it) }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key?.label

    private fun formatKg(value: Float): String =
        if (value % 1f == 0f) "%d".format(value.toInt()) else "%.1f".format(value)
}