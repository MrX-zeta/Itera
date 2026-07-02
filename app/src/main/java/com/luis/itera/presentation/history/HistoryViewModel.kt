package com.luis.itera.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val exerciseNames: Map<Long, String> = emptyMap(),
    val pendingDeleteId: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository
) : ViewModel() {

    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val pendingDeleteId = MutableStateFlow<Long?>(null)
    private var deleteJob: Job? = null

    private val sessions = selectedDate.flatMapLatest { date ->
        sessionRepository.getSessionsByDate(date.toEpochDay())
    }

    val uiState: StateFlow<HistoryUiState> = combine(
        selectedDate,
        sessionRepository.getTrainedDays(),
        sessions,
        exerciseRepository.getAll(),
        pendingDeleteId
    ) { date, trainedDays, sessionList, exercises, pendingId ->
        HistoryUiState(
            selectedDate = date,
            trainedDays = trainedDays.map(LocalDate::ofEpochDay).toSet(),
            sessions = sessionList.sortedByDescending { it.id },
            exerciseNames = exercises.associate { it.id to it.name },
            pendingDeleteId = pendingId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
    }

    fun onSwipeDelete(sessionId: Long) {
        deleteJob?.cancel()
        val previousPending = pendingDeleteId.value
        pendingDeleteId.value = sessionId
        deleteJob = viewModelScope.launch {
            if (previousPending != null && previousPending != sessionId) {
                sessionRepository.deleteSession(previousPending)
            }
            delay(UNDO_WINDOW_MS)
            sessionRepository.deleteSession(sessionId)
            pendingDeleteId.value = null
        }
    }

    fun onUndoDelete() {
        deleteJob?.cancel()
        deleteJob = null
        pendingDeleteId.value = null
    }

    override fun onCleared() {
        val pending = pendingDeleteId.value ?: return
        deleteJob?.cancel()
        kotlinx.coroutines.runBlocking { sessionRepository.deleteSession(pending) }
    }

    private companion object {
        const val UNDO_WINDOW_MS = 4_000L
    }
}