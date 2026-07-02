package com.luis.itera.presentation.session_detail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.theme.IteraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("es"))

@Composable
fun SessionDetailScreen(
    onBack: () -> Unit,
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()

    LaunchedEffect(deleted) { if (deleted) onBack() }

    val session = state.session ?: return

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
                text = "‹ VOLVER",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.Accent,
                modifier = Modifier.clickable(onClick = onBack)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${session.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.Accent
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                    contentDescription = "Eliminar sesión",
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .clickable(onClick = viewModel::onDeleteSession)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Text(
            text = LocalDate.ofEpochDay(session.dateEpochDay).format(dateFormatter)
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineSmall
        )
        WorkoutFocus.fromStored(session.focus).takeIf { it.isNotEmpty() }?.let { focuses ->
            Text(
                text = focuses.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
        Text(
            text = "${session.sets.size} sets totales",
            style = MaterialTheme.typography.bodySmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            session.sets
                .groupBy { it.exerciseId }
                .forEach { (exerciseId, sets) ->
                    item(key = exerciseId) {
                        ExerciseBlock(
                            name = state.exerciseNames[exerciseId] ?: "—",
                            sets = sets,
                            onDeleteSet = viewModel::onDeleteSet
                        )
                    }
                }
        }
    }
}

@Composable
private fun ExerciseBlock(
    name: String,
    sets: List<WorkoutSet>,
    onDeleteSet: (WorkoutSet) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, IteraColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(text = name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        sets.sortedBy { it.order }.forEach { set ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET ${set.order}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
                Text(
                    text = "${set.reps} reps",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = if (set.weightAddedKg > 0f) "+${set.weightAddedKg} kg" else "corporal",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (set.weightAddedKg > 0f) IteraColors.Accent
                    else IteraColors.TextSecondary
                )
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                    contentDescription = "Eliminar set",
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier
                        .clickable { onDeleteSet(set) }
                        .padding(2.dp)
                )
            }
        }
    }
}