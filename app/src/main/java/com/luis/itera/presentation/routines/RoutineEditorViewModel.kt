package com.luis.itera.presentation.routines

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.repository.ExerciseRepository
import com.luis.itera.domain.repository.RoutineRepository
import com.luis.itera.presentation.navigation.IteraDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineEditorUiState(
    val isEditing: Boolean = false,
    val name: String = "",
    val colorOrdinal: Int = 0,
    val selectedExerciseIds: List<Long> = emptyList(),
    val allExercises: List<Exercise> = emptyList()
) {
    val canSave: Boolean get() = name.isNotBlank() && selectedExerciseIds.isNotEmpty()

    /** Ejercicios seleccionados, en el orden en que se añadieron. */
    val selectedExercises: List<Exercise>
        get() = selectedExerciseIds.mapNotNull { id -> allExercises.find { it.id == id } }
}

@HiltViewModel
class RoutineEditorViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    exerciseRepository: ExerciseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val routineId: Long? =
        savedStateHandle.get<Long>(IteraDestination.RoutineEditor.ARG_ROUTINE_ID)?.takeIf { it > 0L }

    private val name = MutableStateFlow("")
    private val colorOrdinal = MutableStateFlow(0)
    private val selectedIds = MutableStateFlow<List<Long>>(emptyList())

    val uiState: StateFlow<RoutineEditorUiState> = combine(
        name, colorOrdinal, selectedIds, exerciseRepository.getAll()
    ) { name, color, ids, exercises ->
        RoutineEditorUiState(
            isEditing = routineId != null,
            name = name,
            colorOrdinal = color,
            selectedExerciseIds = ids,
            allExercises = exercises
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineEditorUiState(isEditing = routineId != null))

    init {
        if (routineId != null) {
            viewModelScope.launch {
                routineRepository.getRoutines().first().firstOrNull { it.id == routineId }?.let { routine ->
                    name.value = routine.name
                    colorOrdinal.value = routine.color
                    selectedIds.value = routine.exerciseIds
                }
            }
        }
    }

    fun onNameChange(value: String) { name.value = value }
    fun onColorSelected(ordinal: Int) { colorOrdinal.value = ordinal }

    /** Añade el ejercicio al final si no está; si ya está, no lo duplica. */
    fun onAddExercise(id: Long) {
        if (id !in selectedIds.value) selectedIds.value = selectedIds.value + id
    }

    fun onRemoveExercise(id: Long) {
        selectedIds.value = selectedIds.value - id
    }

    fun save(onDone: () -> Unit) {
        val n = name.value.trim()
        val ids = selectedIds.value
        if (n.isBlank() || ids.isEmpty()) return
        viewModelScope.launch {
            if (routineId == null) routineRepository.createRoutine(n, colorOrdinal.value, ids)
            else routineRepository.updateRoutine(routineId, n, colorOrdinal.value, ids)
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        val id = routineId ?: return
        viewModelScope.launch {
            routineRepository.deleteRoutine(id)
            onDone()
        }
    }
}
