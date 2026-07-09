package com.luis.itera.presentation.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.model.HydrationIntake
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import com.luis.itera.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HydrationUiState(
    val totalMl: Int = 0,
    val goal: DailyHydrationGoal? = null,
    val userWeightKg: Float = 0f,
    val dragDeltaMl: Int = 0,
    val intakesByDay: Map<Long, List<HydrationIntake>> = emptyMap(),
    val pendingDeletionIds: Set<Long> = emptySet()
) {
    val displayTotalMl: Int
        get() = (totalMl + dragDeltaMl).coerceAtLeast(0)

    val rawProgress: Float
        get() = goal?.totalGoalMl?.takeIf { it > 0 }
            ?.let { (displayTotalMl.toFloat() / it).coerceAtLeast(0f) } ?: 0f

    val progress: Float
        get() = rawProgress.coerceIn(0f, 1f)
}

@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase,
    private val widgetUpdater: WidgetUpdater
) : ViewModel() {

    private val today = LocalDate.now().toEpochDay()
    private val dragDeltaMl = MutableStateFlow(0)
    private val pendingDeletionIds = MutableStateFlow<Set<Long>>(emptySet())
    private val stagedIntakes = MutableStateFlow<Map<Long, HydrationIntake>>(emptyMap())
    private val intakeMutex = Mutex()

    val uiState: StateFlow<HydrationUiState> = combine(
        hydrationRepository.getTotalMlForDay(today),
        hydrationRepository.getDailyGoal(today),
        hydrationRepository.getAllIntakes(),
        userPrefsRepository.getUserWeightKg(),
        dragDeltaMl,
        pendingDeletionIds
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val total = args[0] as Int
        val goal = args[1] as DailyHydrationGoal?
        val allIntakes = args[2] as List<HydrationIntake>
        val weight = args[3] as Float
        val drag = args[4] as Int
        val pending = args[5] as Set<Long>

        val filtered = allIntakes.filter { it.id !in pending }

        HydrationUiState(
            totalMl = total,
            goal = goal,
            userWeightKg = weight,
            dragDeltaMl = drag,
            intakesByDay = filtered.groupBy {
                Instant.ofEpochMilli(it.dateTimeEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay()
            },
            pendingDeletionIds = pending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) { calculateHydrationGoal(today) }
    }

    fun onAddIntake(amountMl: Int) {
        if (amountMl <= 0) return
        viewModelScope.launch(Dispatchers.IO) {
            intakeMutex.withLock {
                val now = System.currentTimeMillis()
                val dayStartMillis = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                val last = hydrationRepository.getLastIntakeForDay(dayStartMillis)
                if (last != null) {
                    val lastZoned = Instant.ofEpochMilli(last.dateTimeEpochMillis)
                        .atZone(ZoneId.systemDefault())
                    val nowZoned = Instant.ofEpochMilli(now)
                        .atZone(ZoneId.systemDefault())
                    if (lastZoned.hour == nowZoned.hour && lastZoned.minute == nowZoned.minute) {
                        hydrationRepository.updateIntakeAmount(last.id, last.amountMl + amountMl)
                        widgetUpdater.refresh()
                        return@withLock
                    }
                }
                hydrationRepository.addIntake(amountMl)
                widgetUpdater.refresh()
            }
        }
    }

    fun stageForDeletion(intake: HydrationIntake) {
        stagedIntakes.value = stagedIntakes.value + (intake.id to intake)
        pendingDeletionIds.value = pendingDeletionIds.value + intake.id
        viewModelScope.launch(Dispatchers.IO) {
            hydrationRepository.deleteIntake(intake)
            widgetUpdater.refresh()
        }
    }

    fun undoDeletion(intakeId: Long) {
        val intake = stagedIntakes.value[intakeId] ?: return
        pendingDeletionIds.value = pendingDeletionIds.value - intakeId
        stagedIntakes.value = stagedIntakes.value - intakeId
        viewModelScope.launch(Dispatchers.IO) {
            hydrationRepository.reInsertIntake(intake.dateTimeEpochMillis, intake.amountMl)
            widgetUpdater.refresh()
        }
    }

    fun commitDeletion(intakeId: Long) {
        pendingDeletionIds.value = pendingDeletionIds.value - intakeId
        stagedIntakes.value = stagedIntakes.value - intakeId
    }

    fun onDrag(deltaMl: Int) {
        val current = uiState.value
        val goalMl = current.goal?.totalGoalMl ?: return
        val proposed = dragDeltaMl.value + deltaMl
        dragDeltaMl.value = proposed.coerceIn(-current.totalMl, goalMl - current.totalMl)
    }

    fun onDragEnd() {
        val delta = dragDeltaMl.value
        dragDeltaMl.value = 0
        if (delta == 0) return
        viewModelScope.launch(Dispatchers.IO) {
            hydrationRepository.addIntake(delta)
            widgetUpdater.refresh()
        }
    }

    fun onWeightDelta(delta: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val newWeight = (uiState.value.userWeightKg + delta).coerceIn(30f, 250f)
            userPrefsRepository.setUserWeightKg(newWeight)
            calculateHydrationGoal(today)
            widgetUpdater.refresh()
        }
    }
}