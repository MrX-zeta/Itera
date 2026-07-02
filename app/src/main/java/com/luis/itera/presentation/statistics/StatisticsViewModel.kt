package com.luis.itera.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.BigThreeRecord
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.domain.model.WeeklyStreak
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.StatisticsRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateWeeklyStreakUseCase
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

data class StatisticsUiState(
    val sessionsThisMonth: Int = 0,
    val topFocus: String? = null,
    val streak: WeeklyStreak = WeeklyStreak(0, 0, 3),
    val bigThree: List<BigThreeRecord> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val selectedExercise: Exercise? = null,
    val selectedGroup: String? = null,
    val range: StatsRange = StatsRange.D30,
    val maxWeightSeries: List<ExerciseSeriesPoint> = emptyList(),
    val volumeSeries: List<ExerciseSeriesPoint> = emptyList()
) {
    val personalRecord: Float?
        get() = maxWeightSeries.maxOfOrNull { it.value }

    val totalVolume: Float
        get() = volumeSeries.map { it.value }.sum()
}

private val BIG_THREE_NAMES = listOf("Press banca", "Peso muerto", "Sentadilla con barra")

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

    private val bigThree = exerciseRepository.getAll().flatMapLatest { exercises ->
        val targets = BIG_THREE_NAMES.mapNotNull { name ->
            exercises.find { it.name == name }
        }
        if (targets.isEmpty()) flowOf(emptyList())
        else statisticsRepository.getPersonalRecords(targets.map { it.id }).map { records ->
            targets.map { exercise ->
                val record = records[exercise.id]
                BigThreeRecord(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    maxWeightKg = record?.first,
                    estimated1RmKg = record?.second,
                    dateEpochDay = record?.third
                )
            }
        }
    }

    private val filteredExercises = combine(
        exerciseRepository.getAll(),
        selectedGroup
    ) { exercises, group ->
        if (group == null) exercises
        else exercises.filter { it.mainMuscleGroup == group }
    }

    private val series = combine(selectedExercise, range) { exercise, r -> exercise to r }
        .flatMapLatest { (exercise, r) ->
            if (exercise == null) {
                flowOf(emptyList<ExerciseSeriesPoint>() to emptyList<ExerciseSeriesPoint>())
            } else {
                val from = LocalDate.now().minusDays(r.days).toEpochDay()
                combine(
                    statisticsRepository.getMaxWeightSeries(exercise.id, from),
                    statisticsRepository.getVolumeSeries(exercise.id, from)
                ) { max, vol -> max to vol }
            }
        }

    val uiState: StateFlow<StatisticsUiState> = combine(
        summary,
        bigThree,
        filteredExercises,
        combine(selectedExercise, selectedGroup, range) { e, g, r -> Triple(e, g, r) },
        series
    ) { summaryData, bigThreeData, exercises, selection, seriesData ->
        StatisticsUiState(
            sessionsThisMonth = summaryData.first,
            topFocus = summaryData.second,
            streak = summaryData.third,
            bigThree = bigThreeData,
            exercises = exercises,
            selectedExercise = selection.first,
            selectedGroup = selection.second,
            range = selection.third,
            maxWeightSeries = seriesData.first,
            volumeSeries = seriesData.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatisticsUiState())

    init {
        viewModelScope.launch {
            val defaultId = statisticsRepository.getMostTrainedExerciseId() ?: return@launch
            val exercise = exerciseRepository.getAll().first().find { it.id == defaultId }
            if (selectedExercise.value == null) selectedExercise.value = exercise
        }
    }

    fun onExerciseSelected(exercise: Exercise) {
        selectedExercise.value = exercise
    }

    fun onGroupSelected(group: String?) {
        selectedGroup.value = if (selectedGroup.value == group) null else group
    }

    fun onRangeSelected(newRange: StatsRange) {
        range.value = newRange
    }

    fun onWeeklyGoalDelta(delta: Int) {
        viewModelScope.launch {
            val current = userPrefsRepository.getWeeklyGoal().first()
            userPrefsRepository.setWeeklyGoal(current + delta)
        }
    }

    private fun topFocusOf(focusList: List<String>): String? =
        focusList
            .flatMap { WorkoutFocus.fromStored(it) }
            .groupingBy { it }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key?.label
}