package com.luis.itera.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class OnboardingUiState(
    val step: Int = 0,
    val weightKg: Float = 75f,
    val weeklyGoal: Int = 3
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state

    fun onWeightDelta(delta: Float) {
        _state.value = _state.value.copy(weightKg = (_state.value.weightKg + delta).coerceIn(30f, 250f))
    }

    fun onGoalSelected(goal: Int) {
        _state.value = _state.value.copy(weeklyGoal = goal)
    }

    fun onNext() { _state.value = _state.value.copy(step = 1) }
    fun onBack() { _state.value = _state.value.copy(step = 0) }

    fun onFinish(onDone: () -> Unit) {
        viewModelScope.launch {
            userPrefsRepository.setUserWeightKg(_state.value.weightKg)
            userPrefsRepository.setWeeklyGoal(_state.value.weeklyGoal)
            calculateHydrationGoal(LocalDate.now().toEpochDay())
            userPrefsRepository.setOnboardingCompleted(true)
            onDone()
        }
    }
}