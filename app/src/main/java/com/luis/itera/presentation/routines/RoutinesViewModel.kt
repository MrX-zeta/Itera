package com.luis.itera.presentation.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.PendingRoutineStart
import com.luis.itera.domain.model.Routine
import com.luis.itera.domain.repository.RoutineRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Lista de rutinas para la pestaña de gestión. El editor tiene su propio ViewModel. */
@HiltViewModel
class RoutinesViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val pendingRoutineStart: PendingRoutineStart
) : ViewModel() {

    val routines: StateFlow<List<Routine>> = routineRepository.getRoutines()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { routineRepository.deleteRoutine(id) }
    }

    /** Pide arrancar la rutina; el ViewModel de Entrenamiento (vivo) reacciona al evento. */
    fun requestStart(id: Long) = pendingRoutineStart.requestStart(id)
}
