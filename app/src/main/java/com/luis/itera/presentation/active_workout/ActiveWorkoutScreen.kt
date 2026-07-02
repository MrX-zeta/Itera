package com.luis.itera.presentation.active_workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.components.SessionTimer
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    onSessionFinished: (Long) -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finishedSessionId by viewModel.finishedSessionId.collectAsStateWithLifecycle()

    LaunchedEffect(finishedSessionId) {
        finishedSessionId?.let {
            viewModel.onFinishedSessionConsumed()
            onSessionFinished(it)
        }
    }

    if (state.session == null) {
        EmptySessionContent(
            selectedFocuses = state.selectedFocuses,
            blockedFocuses = state.blockedFocuses,
            onFocusToggle = viewModel::onFocusToggle,
            onStart = viewModel::onStartSession
        )
    } else {
        ActiveSessionContent(
            state = state,
            muscleGroups = viewModel.muscleGroups,
            onCreateExercise = viewModel::onCreateExercise,
            onSearchChange = viewModel::onSearchQueryChange,
            onExerciseSelected = viewModel::onExerciseSelected,
            onRepsDelta = viewModel::onRepsDelta,
            onWeightDelta = viewModel::onWeightDelta,
            onRegisterSet = viewModel::onRegisterSet,
            onDeleteSet = viewModel::onDeleteSet,
            onStartTimer = viewModel::onStartTimer,
            onDiscardSession = viewModel::onDiscardSession,
            onFinishSession = viewModel::onFinishSession
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptySessionContent(
    selectedFocuses: Set<WorkoutFocus>,
    blockedFocuses: Set<WorkoutFocus>,
    onFocusToggle: (WorkoutFocus) -> Unit,
    onStart: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¿QUÉ ENTRENAS HOY?",
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            WorkoutFocus.entries.forEach { focus ->
                val selected = focus in selectedFocuses
                val blocked = focus in blockedFocuses
                Text(
                    text = focus.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        selected -> IteraColors.OnAccent
                        blocked -> IteraColors.Border
                        else -> IteraColors.TextPrimary
                    },
                    modifier = Modifier
                        .background(
                            color = if (selected) IteraColors.Accent else IteraColors.Surface,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) IteraColors.Accent else IteraColors.Border,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable(enabled = !blocked) { onFocusToggle(focus) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = IteraColors.Accent,
                contentColor = IteraColors.OnAccent
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("INICIAR ENTRENAMIENTO", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ActiveSessionContent(
    state: ActiveWorkoutUiState,
    muscleGroups: List<String>,
    onCreateExercise: (String, String) -> Unit,
    onSearchChange: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onRepsDelta: (Int) -> Unit,
    onWeightDelta: (Float) -> Unit,
    onRegisterSet: () -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onStartTimer: () -> Unit,
    onDiscardSession: () -> Unit,
    onFinishSession: () -> Unit
) {
    var searchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_back),
                    contentDescription = "Descartar entrenamiento",
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier
                        .clickable(onClick = onDiscardSession)
                        .padding(end = 16.dp)
                )
                Column {
                    Text(
                        text = "ENTRENAMIENTO ACTIVO",
                        style = MaterialTheme.typography.labelSmall,
                        color = IteraColors.TextSecondary
                    )
                    if (state.sessionFocuses.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.sessionFocuses.joinToString(" · ") { it.label },
                            style = MaterialTheme.typography.bodySmall,
                            color = IteraColors.Accent
                        )
                    }
                }
            }
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                contentDescription = "Buscar ejercicio",
                tint = if (searchExpanded) IteraColors.Accent else IteraColors.TextSecondary,
                modifier = Modifier
                    .clickable {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) onSearchChange("")
                    }
                    .padding(horizontal = 10.dp)
            )
            if (state.sessionStartMillis != null) {
                SessionTimer(state.sessionStartMillis)
            } else {
                OutlinedButton(
                    onClick = onStartTimer,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, IteraColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = IteraColors.Accent
                    )
                ) {
                    Text("CRONÓMETRO", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        AnimatedVisibility(visible = searchExpanded) {
            Column {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Buscar ejercicio", color = IteraColors.TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IteraColors.Accent,
                        unfocusedBorderColor = IteraColors.Border,
                        focusedTextColor = IteraColors.TextPrimary,
                        unfocusedTextColor = IteraColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
            }
        }

        state.selectedExercise?.let { exercise ->
            Spacer(Modifier.height(12.dp))
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            if (state.lastSets.isNotEmpty()) {
                Text(
                    text = "Última vez: " + state.lastSets.reversed().joinToString(" · ") { set ->
                        "${set.reps}" + if (set.weightAddedKg > 0f) "+${set.weightAddedKg}kg" else ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FastStepper(
                    label = "REPS",
                    value = state.reps.toFloat(),
                    onDelta = { onRepsDelta(it.toInt()) },
                    modifier = Modifier.weight(1f)
                )
                FastStepper(
                    label = "+KG",
                    value = state.weightKg,
                    onDelta = onWeightDelta,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            RegisterSetButton(onRegisterSet = onRegisterSet)
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(Modifier.weight(1f)) {
            item {
                Text(
                    text = "EJERCICIOS",
                    style = MaterialTheme.typography.labelSmall,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(state.exercises, key = { "ex_${it.id}" }) { exercise ->
                val isSelected = exercise.id == state.selectedExercise?.id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onExerciseSelected(exercise) }
                        .border(
                            1.dp,
                            if (isSelected) IteraColors.Accent else IteraColors.Border,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) IteraColors.Accent else IteraColors.TextPrimary
                    )
                    Text(
                        exercise.mainMuscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = IteraColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            if (state.searchQuery.isNotBlank() && state.exercises.isEmpty()) {
                item {
                    CreateExercisePrompt(
                        query = state.searchQuery,
                        muscleGroups = muscleGroups,
                        onCreate = onCreateExercise
                    )
                }
            }
            val sets = state.session?.sets.orEmpty()
            if (sets.isNotEmpty()) {
                item {
                    Text(
                        text = "SETS DE HOY",
                        style = MaterialTheme.typography.labelSmall,
                        color = IteraColors.TextSecondary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    )
                }
                items(sets, key = { "set_${it.id}" }) { set ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = state.exerciseNameOf(set.exerciseId),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "SET ${set.order} · ${set.reps} reps" +
                                        if (set.weightAddedKg > 0f) " +${set.weightAddedKg}kg" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = IteraColors.TextSecondary
                            )
                        }
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                            contentDescription = "Eliminar set",
                            tint = IteraColors.TextSecondary,
                            modifier = Modifier
                                .clickable { onDeleteSet(set) }
                                .padding(4.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onFinishSession,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, IteraColors.Border),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = IteraColors.TextSecondary
            )
        ) {
            Text("FINALIZAR SESIÓN", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateExercisePrompt(
    query: String,
    muscleGroups: List<String>,
    onCreate: (String, String) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "Sin resultados. Crear \"$query\":",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "GRUPO MUSCULAR",
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            muscleGroups.forEach { group ->
                Text(
                    text = group,
                    style = MaterialTheme.typography.labelSmall,
                    color = IteraColors.Accent,
                    modifier = Modifier
                        .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                        .clickable { onCreate(query, group) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun RegisterSetButton(onRegisterSet: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var registered by remember { mutableStateOf(false) }

    LaunchedEffect(registered) {
        if (registered) {
            delay(900L)
            registered = false
        }
    }

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            registered = true
            onRegisterSet()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (registered) IteraColors.Surface else IteraColors.Accent,
            contentColor = if (registered) IteraColors.Accent else IteraColors.OnAccent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (registered) "✓ REGISTRADO" else "REGISTRAR SET",
            style = MaterialTheme.typography.titleMedium
        )
    }
}