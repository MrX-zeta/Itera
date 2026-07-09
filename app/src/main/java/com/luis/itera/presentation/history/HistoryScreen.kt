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
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Session
import com.luis.itera.presentation.components.ActivityHeatmapCard
import com.luis.itera.presentation.components.fmtWeight
import com.luis.itera.presentation.theme.IteraColors
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
    val snackbarHostState = remember { SnackbarHostState() }

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
            Text(
                text = "HISTORIAL",
                style = MaterialTheme.typography.titleMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))

            ActivityHeatmapCard(
                data = state.activityByDay,
                selectedDate = selectedDay,
                onDateSelected = viewModel::onDaySelected
            )

            Spacer(Modifier.height(16.dp))

            val visibleSessions = state.sessions.filter { it.id != state.pendingDeleteId }

            if (visibleSessions.isEmpty()) {
                Text(
                    text = "Sin sesiones registradas",
                    style = MaterialTheme.typography.bodySmall,
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

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled &&
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
        modifier = modifier,
        backgroundContent = {
            val revealedDp = with(density) { kotlin.math.abs(offsetPx).toDp() }

            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                val panelAlignment =
                    if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                        Alignment.CenterStart else Alignment.CenterEnd

                Box(
                    Modifier
                        .align(panelAlignment)
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
            .background(IteraColors.Background, RoundedCornerShape(12.dp))
            .border(1.dp, IteraColors.Border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
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
                Text(
                    text = " · ${LocalDate.ofEpochDay(session.dateEpochDay).format(cardDateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
            }
            if (session.sets.any { it.isPr }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_fire),
                        contentDescription = null,
                        tint = IteraColors.Accent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "PR",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = IteraColors.Accent
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        displayed.forEach { (exerciseId, sets) ->
            val isCardio = sets.any { it.durationSeconds > 0 }
            Column(Modifier.padding(vertical = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = exerciseNames[exerciseId] ?: "—",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${sets.size} ${if (sets.size == 1) "set" else "sets"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = IteraColors.Accent
                    )
                }
                Text(
                    text = sets.joinToString(" · ") { set ->
                        if (isCardio) "${set.durationSeconds / 60} min"
                        else buildString {
                            append("${set.reps}")
                            if (set.weightAddedKg > 0f) append(" × ${fmtWeight(set.weightAddedKg)}kg")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (remaining > 0) {
            Text(
                text = "+ $remaining ejercicios más",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}