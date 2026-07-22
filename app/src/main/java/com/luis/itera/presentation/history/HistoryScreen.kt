package com.luis.itera.presentation.history

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Session
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.components.ActivityHeatmapCard
import com.luis.itera.presentation.components.HEATMAP_EMPTY_CELL_COLOR
import com.luis.itera.presentation.components.TrainingHeatmapLegend
import com.luis.itera.presentation.components.fmtWeight
import com.luis.itera.presentation.components.heatmapDateFormatter
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

private val cardDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es"))
private const val MIN_REVEAL_MS = 125L

@Composable
fun HistoryScreen(
    onSessionClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDay by viewModel.selectedDay.collectAsStateWithLifecycle()
    val lookupResult by viewModel.exerciseLookup.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLookupDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.pendingDeleteId) {
        val pendingId = state.pendingDeleteId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Sesión eliminada",
            actionLabel = "DESHACER",
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onUndoDelete()
        }
    }

    Scaffold(
        containerColor = IteraColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .padding(16.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Text(
                    text = "HISTORIAL",
                    style = MaterialTheme.typography.titleMedium,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_search),
                    contentDescription = "¿Dónde me quedé?",
                    tint = IteraColors.TextPrimary,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clickable { showLookupDialog = true }
                        .padding(12.dp)
                        .size(24.dp)
                )
            }
            Spacer(Modifier.height(12.dp))

            val accent = LocalAccent.current
            ActivityHeatmapCard(
                levelForDate = { date ->
                    when {
                        date in state.trainedDays && date in state.prDays -> 2
                        date in state.trainedDays -> 1
                        else -> 0
                    }
                },
                colorForLevel = { level ->
                    when (level) {
                        2 -> accent.color
                        1 -> accent.color.copy(alpha = 0.7f)
                        else -> HEATMAP_EMPTY_CELL_COLOR
                    }
                },
                emptyBorderColor = accent.color,
                filledBorderColor = accent.onAccent,
                highlightDays = state.prDays,
                selectedDate = selectedDay,
                onDateSelected = viewModel::onDaySelected,
                selectionLabel = { cell, isToday ->
                    val datePart = if (isToday) "Hoy" else cell.date.format(heatmapDateFormatter)
                    if (cell.level > 0) "$datePart · Entrenaste" else datePart
                },
                legend = { TrainingHeatmapLegend() }
            )

            Spacer(Modifier.height(16.dp))

            val visibleSessions = state.sessions.filter { it.id != state.pendingDeleteId }

            if (visibleSessions.isEmpty()) {
                Text(
                    text = "Sin sesiones registradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextSecondaryStrong
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(visibleSessions, key = { it.id }) { session ->
                        val itemModifier = Modifier.animateItem(
                            placementSpec = tween(450, easing = FastOutSlowInEasing),
                            fadeOutSpec = tween(450)
                        )
                        if (session.isFinished) {
                            DismissableSessionCard(
                                session = session,
                                exerciseNames = state.exerciseNames,
                                onClick = { onSessionClick(session.id) },
                                onDismiss = { viewModel.onSwipeDelete(session.id) },
                                modifier = itemModifier
                            )
                        } else {
                            SessionCard(
                                session = session,
                                exerciseNames = state.exerciseNames,
                                onClick = { onSessionClick(session.id) },
                                modifier = itemModifier
                            )
                        }
                    }
                }
            }
        }
    }

    if (showLookupDialog) {
        WhereWasILeftDialog(
            exercises = state.exercises,
            result = lookupResult,
            onSelect = viewModel::onLookupExercise,
            onSearchAgain = viewModel::onClearLookup,
            onDismiss = {
                showLookupDialog = false
                viewModel.onClearLookup()
            }
        )
    }
}

@Composable
private fun WhereWasILeftDialog(
    exercises: List<Exercise>,
    result: ExerciseLookupResult?,
    onSelect: (Exercise) -> Unit,
    onSearchAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IteraColors.Surface,
        title = { Text("¿Dónde me quedé?", color = IteraColors.TextPrimary) },
        text = {
            if (result != null) {
                Column {
                    Text(result.exercise.name, style = MaterialTheme.typography.titleMedium, color = IteraColors.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    if (result.sets.isEmpty()) {
                        Text(
                            "Aún no tiene sets registrados",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextSecondary
                        )
                    } else {
                        val isCardio = result.sets.any { it.durationSeconds > 0 }
                        Text(
                            text = result.dateEpochDay?.let { LocalDate.ofEpochDay(it).format(cardDateFormatter) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        result.sets.forEachIndexed { index, set ->
                            Text(
                                text = "SET ${index + 1} · " + if (isCardio) {
                                    "${set.durationSeconds / 60} min" + if (set.intensity > 0) " · nivel ${set.intensity}" else ""
                                } else {
                                    buildString {
                                        append("${set.reps} reps")
                                        if (set.weightAddedKg > 0f) append(" · +${fmtWeight(set.weightAddedKg)}kg")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = IteraColors.TextPrimary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Buscar otro ejercicio",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = LocalAccent.current.color,
                        modifier = Modifier.clickable(onClick = onSearchAgain)
                    )
                }
            } else {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Buscar ejercicio", color = IteraColors.TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalAccent.current.color,
                            unfocusedBorderColor = IteraColors.Border,
                            focusedTextColor = IteraColors.TextPrimary,
                            unfocusedTextColor = IteraColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    val matches = if (query.isBlank()) exercises else exercises.filter { it.name.contains(query, ignoreCase = true) }
                    Column {
                        matches.take(20).forEach { exercise ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelect(exercise) }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(exercise.mainMuscleGroup, style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text("CERRAR", style = MaterialTheme.typography.labelMedium, color = IteraColors.TextSecondary, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
        }
    )
}

@Composable
private fun DismissableSessionCard(
    session: Session,
    exerciseNames: Map<Long, String>,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trashSeenAt by remember { mutableLongStateOf(0L) }

    // Solo se borra deslizando hacia la izquierda (EndToStart), como en Hidratación: unifica
    // el gesto en toda la app y evita el conflicto con el swipe de pestañas del pager.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart &&
                System.currentTimeMillis() - trashSeenAt >= MIN_REVEAL_MS
            ) {
                onDismiss()
                true
            } else false
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.5f }
    )
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val offsetPx = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
    val crossed = dismissState.targetValue != SwipeToDismissBoxValue.Settled
    val isDragged = offsetPx != 0f

    LaunchedEffect(isDragged) {
        if (isDragged && trashSeenAt == 0L) trashSeenAt = System.currentTimeMillis()
        if (!isDragged) trashSeenAt = 0L
    }

    val cardScale by animateFloatAsState(
        targetValue = if (isDragged) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    val lidRotation by animateFloatAsState(
        targetValue = if (crossed) -35f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lid_rotation"
    )

    LaunchedEffect(crossed) {
        if (crossed) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            val revealedDp = with(density) { kotlin.math.abs(offsetPx).toDp() }

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                // Solo EndToStart es alcanzable: el panel siempre revela desde el final (derecha).
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(revealedDp)
                        .fillMaxHeight()
                        .background(IteraColors.Error),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = revealedDp > 40.dp,
                        enter = scaleIn(
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            initialScale = 0.3f
                        ) + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        Box {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_trash_lid),
                                contentDescription = null,
                                tint = IteraColors.Background,
                                modifier = Modifier
                                    .size(30.dp)
                                    .graphicsLayer {
                                        rotationZ = lidRotation
                                        transformOrigin = TransformOrigin(0.15f, 0.3f)
                                    }
                            )
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_trash_body),
                                contentDescription = null,
                                tint = IteraColors.Background,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        SessionCard(
            session = session,
            exerciseNames = exerciseNames,
            onClick = onClick,
            modifier = Modifier.scale(cardScale)
        )
    }
}

/**
 * Resumen COMPACTO de sets para la tarjeta de la lista de Historial (no el detalle de
 * sesión, que sigue mostrando cada set individual con su papelera). Colapsa reps/peso
 * repetidos en un solo rango en vez de listar set por set.
 */
private fun summarizeSets(sets: List<WorkoutSet>, isCardio: Boolean): String {
    val setsLabel = if (sets.size == 1) "1 set" else "${sets.size} sets"
    return if (isCardio) {
        val minutesLabel = formatIntRange(sets.map { it.durationSeconds / 60 }) + " min"
        val intensities = sets.map { it.intensity }.filter { it > 0 }
        val intensityLabel = if (intensities.isNotEmpty()) " · nivel ${formatIntRange(intensities)}" else ""
        "$setsLabel · $minutesLabel$intensityLabel"
    } else {
        val repsLabel = formatIntRange(sets.map { it.reps }) + " reps"
        val weights = sets.map { it.weightAddedKg }
        val weightLabel = when {
            weights.all { it <= 0f } -> "corporal"
            weights.distinct().size == 1 -> "${fmtWeight(weights.first())}kg"
            else -> "${fmtWeight(weights.min())}-${fmtWeight(weights.max())}kg"
        }
        "$setsLabel · $repsLabel · $weightLabel"
    }
}

private fun formatIntRange(values: List<Int>): String {
    val min = values.min()
    val max = values.max()
    return if (min == max) "$min" else "$min-$max"
}

@Composable
private fun SessionCard(
    session: Session,
    exerciseNames: Map<Long, String>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = session.sets.groupBy { it.exerciseId }
    val displayed = grouped.entries.take(3)
    val remaining = grouped.size - displayed.size

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (session.isFinished && session.durationMinutes < 1) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_flash),
                        contentDescription = null,
                        tint = LocalAccent.current.color,
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = LocalAccent.current.color
                )
                Text(
                    text = " · ${LocalDate.ofEpochDay(session.dateEpochDay).format(cardDateFormatter)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextSecondary
                )
            }
            // Logro: ámbar, no acento (verde=actividad, ámbar=logro; no se mezclan).
            if (session.sets.any { it.isPr }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_fire),
                        contentDescription = null,
                        tint = IteraColors.Achievement,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "PR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = IteraColors.Achievement
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        displayed.forEach { (exerciseId, sets) ->
            val isCardio = sets.any { it.durationSeconds > 0 }
            Column(Modifier.padding(vertical = 6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = exerciseNames[exerciseId] ?: "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextPrimary
                    )
                    Text(
                        text = "${sets.size} ${if (sets.size == 1) "set" else "sets"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                }
                Text(
                    text = summarizeSets(sets, isCardio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (remaining > 0) {
            Text(
                text = "+ $remaining ejercicios más",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}