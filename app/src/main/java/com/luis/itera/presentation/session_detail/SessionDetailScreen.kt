package com.luis.itera.presentation.session_detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.components.fmtWeight
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

    var lastSession by remember { mutableStateOf(state.session) }
    LaunchedEffect(state.session) { state.session?.let { lastSession = it } }
    val session = lastSession ?: return

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ElongatedBackButton(onClick = onBack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (session.isFinished && session.durationMinutes < 1) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_flash),
                        contentDescription = null,
                        tint = IteraColors.Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    text = when {
                        !session.isFinished -> "EN CURSO"
                        session.durationMinutes < 1 -> "Rápida"
                        else -> "${session.durationMinutes} min"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.Accent
                )
                IconButton(onClick = viewModel::onDeleteSession) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                        contentDescription = "Eliminar sesión",
                        tint = IteraColors.TextSecondary
                    )
                }
            }
        }

        Text(
            text = LocalDate.ofEpochDay(session.dateEpochDay).format(dateFormatter)
                .replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.headlineSmall
        )
        WorkoutFocus.fromStored(session.focus).takeIf { it.isNotEmpty() }?.let { focuses ->
            Spacer(Modifier.height(10.dp))
            Text(
                text = focuses.joinToString(" · ") { it.label },
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.Accent
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "${session.sets.size} sets totales",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            color = IteraColors.TextPrimary
        )
        Spacer(Modifier.height(20.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            session.sets
                .groupBy { it.exerciseId }
                .forEach { (exerciseId, sets) ->
                    item(key = exerciseId) {
                        ExerciseDetailCard(
                            name = state.exerciseNames[exerciseId] ?: "—",
                            sets = sets
                        ) { viewModel.onDeleteSet(it) }
                    }
                }
        }
    }
}

@Composable
private fun ElongatedBackButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(Modifier.size(width = 28.dp, height = 14.dp)) {
            val stroke = 1.8.dp.toPx()
            val tipY = size.height / 2f
            val tipX = 0f
            val armLen = 7.dp.toPx()

            drawLine(
                color = IteraColors.Accent,
                start = Offset(tipX, tipY),
                end = Offset(size.width, tipY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = IteraColors.Accent,
                start = Offset(tipX, tipY),
                end = Offset(tipX + armLen, tipY - armLen),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = IteraColors.Accent,
                start = Offset(tipX, tipY),
                end = Offset(tipX + armLen, tipY + armLen),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun ExerciseDetailCard(
    name: String,
    sets: List<WorkoutSet>,
    onDeleteSet: (WorkoutSet) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.Surface)
            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            color = IteraColors.TextPrimary
        )
        sets.firstOrNull { it.isPr }?.let { prSet ->
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_fire),
                    contentDescription = null,
                    tint = IteraColors.Accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (prSet.weightAddedKg > 0f) "Nuevo máximo · ${fmtWeight(prSet.weightAddedKg)} kg"
                    else "Nuevo máximo · ${prSet.reps} reps",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.Accent
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            thickness = 0.5.dp,
            color = IteraColors.Border
        )
        sets.sortedBy { it.order }.forEachIndexed { index, set ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET ${index + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.weight(0.2f)
                )
                Text(
                    text = when {
                        set.durationSeconds > 0 -> "${set.durationSeconds / 60} min"
                        else -> "${set.reps} reps"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextPrimary,
                    modifier = Modifier.weight(0.3f)
                )
                Text(
                    text = when {
                        set.durationSeconds > 0 && set.intensity > 0 -> "nivel ${set.intensity}"
                        set.weightAddedKg > 0f -> "+${fmtWeight(set.weightAddedKg)} kg"
                        else -> "corporal"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = when {
                        set.weightAddedKg > 0f || (set.durationSeconds > 0 && set.intensity > 0) -> IteraColors.TextPrimary
                        else -> IteraColors.TextSecondary
                    },
                    modifier = Modifier.weight(0.3f)
                )
                IconButton(
                    onClick = { onDeleteSet(set) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                        contentDescription = "Eliminar set",
                        tint = IteraColors.TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}