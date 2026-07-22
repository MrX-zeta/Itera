package com.luis.itera.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.repository.UserPrefsRepository
import com.luis.itera.domain.usecase.CalculateHydrationGoalUseCase
import com.luis.itera.presentation.theme.AccentColor
import com.luis.itera.presentation.widget.WidgetPinResult
import com.luis.itera.presentation.widget.WidgetPinner
import com.luis.itera.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SettingsUiState(
    val accentColor: AccentColor = AccentColor.Default,
    val weightKg: Float = 70f,
    val weeklyGoal: Int = 3,
    val restGoalSeconds: Int = 90,
    val appVersion: String = ""
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefsRepository: UserPrefsRepository,
    private val calculateHydrationGoal: CalculateHydrationGoalUseCase,
    private val widgetUpdater: WidgetUpdater,
    private val widgetPinner: WidgetPinner,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val appVersion: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: ""

    val uiState: StateFlow<SettingsUiState> = combine(
        userPrefsRepository.getAccentColor(),
        userPrefsRepository.getUserWeightKg(),
        userPrefsRepository.getWeeklyGoal(),
        userPrefsRepository.getRestGoalSeconds()
    ) { accent, weight, goal, restGoal ->
        SettingsUiState(
            accentColor = accent,
            weightKg = weight,
            weeklyGoal = goal,
            restGoalSeconds = restGoal,
            appVersion = appVersion
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        SettingsUiState(appVersion = appVersion)
    )

    private val _widgetMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val widgetMessage: SharedFlow<String> = _widgetMessage.asSharedFlow()

    fun onAccentSelected(accent: AccentColor) {
        viewModelScope.launch { userPrefsRepository.setAccentColor(accent) }
    }

    fun onWeightDelta(delta: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val newWeight = (uiState.value.weightKg + delta).coerceIn(MIN_WEIGHT_KG, MAX_WEIGHT_KG)
            userPrefsRepository.setUserWeightKg(newWeight)
            calculateHydrationGoal(LocalDate.now().toEpochDay())
            widgetUpdater.refresh()
        }
    }

    fun onGoalSelected(goal: Int) {
        viewModelScope.launch {
            userPrefsRepository.setWeeklyGoal(goal)
            widgetUpdater.refresh()
        }
    }

    /** Meta del descanso por defecto, en pasos de 15s. El "+30 s" del modal de descanso
     *  sigue siendo temporal para ese descanso concreto; esto solo cambia el default. */
    fun onRestGoalDelta(deltaSeconds: Int) {
        viewModelScope.launch {
            val newGoal = (uiState.value.restGoalSeconds + deltaSeconds)
                .coerceIn(MIN_REST_GOAL_SECONDS, MAX_REST_GOAL_SECONDS)
            userPrefsRepository.setRestGoalSeconds(newGoal)
        }
    }

    fun onAddWidgetClick() {
        when (widgetPinner.requestPinWithStatus()) {
            WidgetPinResult.ALREADY_PINNED ->
                _widgetMessage.tryEmit("El widget ya está en la pantalla de inicio")
            WidgetPinResult.UNSUPPORTED ->
                _widgetMessage.tryEmit("Mantén pulsada la pantalla de inicio y elige Widgets")
            WidgetPinResult.REQUESTED -> Unit
        }
    }

    private companion object {
        const val MIN_WEIGHT_KG = 30f
        const val MAX_WEIGHT_KG = 250f
        const val MIN_REST_GOAL_SECONDS = 30
        const val MAX_REST_GOAL_SECONDS = 300
    }
}
