package com.luis.itera.presentation.session_detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.SessionRepository
import com.luis.itera.presentation.navigation.IteraDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionDetailUiState(
    val session: Session? = null,
    val exerciseNames: Map<Long, String> = emptyMap()
)

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long =
        checkNotNull(savedStateHandle[IteraDestination.SessionDetail.ARG_SESSION_ID])

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    val uiState: StateFlow<SessionDetailUiState> = combine(
        sessionRepository.getSessionById(sessionId),
        exerciseRepository.getAll()
    ) { session, exercises ->
        SessionDetailUiState(
            session = session,
            exerciseNames = exercises.associate { it.id to it.name }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionDetailUiState())

    fun onDeleteSet(set: WorkoutSet) {
        viewModelScope.launch { sessionRepository.deleteSet(set) }
    }

    fun onDeleteSession() {
        viewModelScope.launch {
            sessionRepository.deleteSession(sessionId)
            _deleted.value = true
        }
    }
}