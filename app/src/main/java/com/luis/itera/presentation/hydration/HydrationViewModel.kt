package com.luis.itera.presentation.hydration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.DailyHydrationGoal
import com.luis.itera.domain.model.HydrationIntake
import com.luis.itera.domain.repository.HydrationRepository
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    val intakesByDay: Map<Long, List<HydrationIntake>> = emptyMap()
) {
    val displayTotalMl: Int
        get() = (totalMl + dragDeltaMl).coerceAtLeast(0)

    val progress: Float
        get() = goal?.totalGoalMl?.takeIf { it > 0 }
            ?.let { (displayTotalMl.toFloat() / it).coerceIn(0f, 1f) } ?: 0f
}

@HiltViewModel
class HydrationViewModel @Inject constructor(
    private val hydrationRepository: HydrationRepository,
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase
) : ViewModel() {

    private val today = LocalDate.now().toEpochDay()
    private val dragDeltaMl = MutableStateFlow(0)

    val uiState: StateFlow<HydrationUiState> = combine(
        hydrationRepository.getTotalMlForDay(today),
        hydrationRepository.getDailyGoal(today),
        hydrationRepository.getAllIntakes(),
        userPrefsRepository.getUserWeightKg(),
        dragDeltaMl
    ) { total, goal, allIntakes, weight, drag ->
        HydrationUiState(
            totalMl = total,
            goal = goal,
            userWeightKg = weight,
            dragDeltaMl = drag,
            intakesByDay = allIntakes.groupBy {
                Instant.ofEpochMilli(it.dateTimeEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .toEpochDay()
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationUiState())

    init {
        viewModelScope.launch { calculateHydrationGoal(today) }
    }

    fun onAddIntake(amountMl: Int) {
        viewModelScope.launch { hydrationRepository.addIntake(amountMl) }
    }

    fun onDeleteIntake(intake: HydrationIntake) {
        viewModelScope.launch { hydrationRepository.deleteIntake(intake) }
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
        viewModelScope.launch { hydrationRepository.addIntake(delta) }
    }

    fun onWeightDelta(delta: Float) {
        viewModelScope.launch {
            val newWeight = (uiState.value.userWeightKg + delta).coerceIn(30f, 250f)
            userPrefsRepository.setUserWeightKg(newWeight)
            calculateHydrationGoal(today)
        }
    }
}