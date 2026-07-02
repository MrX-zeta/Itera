package com.luis.itera.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HistoryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val trainedDays: Set<LocalDate> = emptySet(),
    val sessions: List<Session> = emptyList(),
    val exerciseNames: Map<Long, String> = emptyMap()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val sessions = selectedDate.flatMapLatest { date ->
        sessionRepository.getSessionsByDate(date.toEpochDay())
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        selectedDate,
        sessionRepository.getTrainedDays(),
        sessions,
        exerciseRepository.getAll()
    ) { date, trainedDays, sessionList, exercises ->
        HistoryUiState(
            selectedDate = date,
            trainedDays = trainedDays.map(LocalDate::ofEpochDay).toSet(),
            sessions = sessionList.sortedByDescending { it.id },
            exerciseNames = exercises.associate { it.id to it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
    }

    fun onDeleteSession(sessionId: Long) {
        viewModelScope.launch { sessionRepository.deleteSession(sessionId) }
    }
}