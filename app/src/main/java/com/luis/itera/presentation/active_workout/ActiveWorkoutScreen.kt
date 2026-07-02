package com.luis.itera.presentation.active_workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.domain.model.Exercise
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.components.SessionTimer
import com.luis.itera.presentation.theme.IteraColors

@Composable
fun ActiveWorkoutScreen(
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.session == null) {
        EmptySessionContent(onStart = viewModel::onStartSession)
    } else {
        ActiveSessionContent(
            state = state,
            onSearchChange = viewModel::onSearchQueryChange,
            onExerciseSelected = viewModel::onExerciseSelected,
            onRepsDelta = viewModel::onRepsDelta,
            onWeightDelta = viewModel::onWeightDelta,
            onRegisterSet = viewModel::onRegisterSet,
            onFinishSession = viewModel::onFinishSession
        )
    }
}

@Composable
private fun EmptySessionContent(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onStart,
            colors = ButtonDefaults.buttonColors(
                containerColor = IteraColors.Accent,
                contentColor = IteraColors.OnAccent
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("INICIAR SESIÓN", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ActiveSessionContent(
    state: ActiveWorkoutUiState,
    onSearchChange: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onRepsDelta: (Int) -> Unit,
    onWeightDelta: (Float) -> Unit,
    onRegisterSet: () -> Unit,
    onFinishSession: () -> Unit
) {
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
            Text(
                text = "SESIÓN ACTIVA",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.TextSecondary
            )
            state.sessionStartMillis?.let { SessionTimer(it) }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
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

        if (state.searchQuery.isNotBlank()) {
            LazyColumn(
                Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onExerciseSelected(exercise) }
                            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            exercise.mainMuscleGroup,
                            style = MaterialTheme.typography.bodySmall,
                            color = IteraColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
            state.selectedExercise?.let { exercise ->
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
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
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRegisterSet,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IteraColors.Accent,
                        contentColor = IteraColors.OnAccent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("REGISTRAR SET", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            SessionSetsList(state, Modifier.weight(1f))
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
}

@Composable
private fun SessionSetsList(state: ActiveWorkoutUiState, modifier: Modifier = Modifier) {
    val sets = state.session?.sets ?: return
    LazyColumn(modifier) {
        items(sets, key = { it.id }) { set ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "SET ${set.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
                Text(
                    "${set.reps} reps" + if (set.weightAddedKg > 0f) " +${set.weightAddedKg}kg" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}