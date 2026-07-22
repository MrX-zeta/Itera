package com.luis.itera.presentation.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import com.luis.itera.presentation.theme.RoutineColor

@Composable
fun RoutineEditorScreen(
    onBack: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(ImageVector.vectorResource(R.drawable.ic_back), contentDescription = "Volver", tint = IteraColors.TextPrimary)
            }
            Text(
                if (state.isEditing) "Editar rutina" else "Nueva rutina",
                style = MaterialTheme.typography.headlineSmall,
                color = IteraColors.TextPrimary
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column {
                    SectionLabel("NOMBRE")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        singleLine = true,
                        placeholder = { Text("Ej. Pierna completa") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAccent.current.color,
                            unfocusedBorderColor = IteraColors.Border,
                            focusedTextColor = IteraColors.TextPrimary,
                            unfocusedTextColor = IteraColors.TextPrimary,
                            cursorColor = LocalAccent.current.color,
                            focusedContainerColor = IteraColors.SurfaceElevated,
                            unfocusedContainerColor = IteraColors.SurfaceElevated,
                            focusedPlaceholderColor = IteraColors.TextSecondary,
                            unfocusedPlaceholderColor = IteraColors.TextSecondary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Column {
                    SectionLabel("COLOR")
                    Spacer(Modifier.height(12.dp))
                    ColorPicker(state.colorOrdinal, viewModel::onColorSelected)
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("EJERCICIOS")
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(LocalAccent.current.color.copy(alpha = 0.15f))
                            .clickable { showPicker = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_plus),
                            contentDescription = null,
                            tint = LocalAccent.current.color,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Añadir",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = LocalAccent.current.color
                        )
                    }
                }
            }

            if (state.selectedExercises.isEmpty()) {
                item {
                    Text(
                        "Añade al menos un ejercicio a la rutina.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                }
            } else {
                items(state.selectedExercises, key = { it.id }) { exercise ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IteraColors.SurfaceElevated)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IteraColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                exercise.mainMuscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = IteraColors.TextSecondary
                            )
                        }
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_trash),
                            contentDescription = "Quitar",
                            tint = IteraColors.TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.onRemoveExercise(exercise.id) }
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            if (state.isEditing) {
                item {
                    Text(
                        "Eliminar rutina",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = IteraColors.Error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.delete(onBack) }
                            .padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Guardar (ancho completo, deshabilitado si falta nombre o ejercicios).
        val enabled = state.canSave
        Text(
            if (state.isEditing) "Guardar cambios" else "Crear rutina",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (enabled) LocalAccent.current.onAccent else IteraColors.TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) LocalAccent.current.color else IteraColors.SurfaceElevated)
                .clickable(enabled = enabled) { viewModel.save(onBack) }
                .padding(vertical = 16.dp)
        )
    }

    if (showPicker) {
        ExerciseAddSheet(
            exercises = state.allExercises,
            selectedIds = state.selectedExerciseIds.toSet(),
            onAdd = viewModel::onAddExercise,
            onRemove = viewModel::onRemoveExercise,
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = IteraColors.TextSecondary
    )
}

/** Selector de los 10 colores de rutina. La selección se marca con un anillo (agnóstico al tono). */
@Composable
private fun ColorPicker(selected: Int, onSelect: (Int) -> Unit) {
    val colors = RoutineColor.entries
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (rowStart in colors.indices step 5) {
            // Dispersos a todo el ancho (SpaceBetween): sin hueco a la derecha.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (i in rowStart until minOf(rowStart + 5, colors.size)) {
                    val isSelected = i == selected
                    Box(
                        Modifier
                            .then(
                                if (isSelected) Modifier.border(2.dp, IteraColors.TextPrimary, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .padding(3.dp)
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(colors[i].color)
                                .clickable { onSelect(i) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseAddSheet(
    exercises: List<Exercise>,
    selectedIds: Set<Long>,
    onAdd: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(exercises, query) {
        val q = query.trim()
        if (q.isEmpty()) exercises else exercises.filter { it.name.contains(q, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IteraColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IteraColors.BorderStrong) }
    ) {
        Column(Modifier.imePadding()) {
            Text(
                "AÑADIR EJERCICIOS",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Buscar ejercicio") },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current.color,
                    unfocusedBorderColor = IteraColors.Border,
                    focusedTextColor = IteraColors.TextPrimary,
                    unfocusedTextColor = IteraColors.TextPrimary,
                    cursorColor = LocalAccent.current.color,
                    focusedContainerColor = IteraColors.SurfaceElevated,
                    unfocusedContainerColor = IteraColors.SurfaceElevated,
                    focusedPlaceholderColor = IteraColors.TextSecondary,
                    unfocusedPlaceholderColor = IteraColors.TextSecondary
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f, fill = false),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    val isSelected = exercise.id in selectedIds
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) LocalAccent.current.color.copy(alpha = 0.15f)
                                else IteraColors.SurfaceElevated
                            )
                            .clickable { if (isSelected) onRemove(exercise.id) else onAdd(exercise.id) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) LocalAccent.current.color else IteraColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                exercise.mainMuscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = IteraColors.TextSecondary
                            )
                        }
                        if (isSelected) {
                            Icon(
                                ImageVector.vectorResource(R.drawable.ic_check),
                                contentDescription = null,
                                tint = LocalAccent.current.color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
