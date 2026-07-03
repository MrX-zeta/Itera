package com.luis.itera.presentation.hydration

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.theme.IteraColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.atan2

private val quickAmounts = listOf(250 to "VASO", 500 to "BOTELLA", 1000 to "LITRO")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormatter = DateTimeFormatter.ofPattern("EEEE dd MMM", Locale("es"))
private const val ML_PER_TURN = 3500f
private const val DRAG_STEP_ML = 50

@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
            .padding(16.dp)
    ) {
        Text(
            text = "HIDRATACIÓN · HOY",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextSecondary
        )
        state.goal?.takeIf { it.isActiveDay }?.let {
            Text(
                text = "+${it.activityBonusMl} ml día activo",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
        Spacer(Modifier.height(20.dp))

        DraggableProgressRing(
            progress = state.progress,
            totalMl = state.displayTotalMl,
            goalMl = state.goal?.totalGoalMl ?: 0,
            isDragging = state.dragDeltaMl != 0,
            onDrag = viewModel::onDrag,
            onDragEnd = viewModel::onDragEnd,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickAmounts.forEach { (amount, label) ->
                QuickAmountButton(
                    amountMl = amount,
                    label = label,
                    onClick = { viewModel.onAddIntake(amount) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = "REGISTRO",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(Modifier.weight(1f)) {
            state.intakesByDay
                .toSortedMap(compareByDescending { it })
                .forEach { (epochDay, dayIntakes) ->
                    item(key = "day_$epochDay") {
                        Text(
                            text = LocalDate.ofEpochDay(epochDay).format(dayFormatter)
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = IteraColors.TextSecondary,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    items(
                        items = dayIntakes.sortedByDescending { it.dateTimeEpochMillis },
                        key = { "intake_${it.id}" }
                    ) { intake ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Instant.ofEpochMilli(intake.dateTimeEpochMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .format(timeFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                color = IteraColors.TextSecondary
                            )
                            Text(
                                text = "${if (intake.amountMl >= 0) "+" else ""}${intake.amountMl} ml",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (intake.amountMl >= 0) IteraColors.TextPrimary else IteraColors.Error
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
        }

        FastStepper(
            label = "PESO CORPORAL (KG)",
            value = state.userWeightKg,
            onDelta = viewModel::onWeightDelta
        )
    }
}

@Composable
private fun DraggableProgressRing(
    progress: Float,
    totalMl: Int,
    goalMl: Int,
    isDragging: Boolean,
    onDrag: (Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 600),
        label = "hydration_progress"
    )

    Box(
        modifier = modifier
            .size(200.dp)
            .pointerInput(Unit) {
                val center = Offset(size.width / 2f, size.height / 2f)
                var lastAngle: Float? = null
                var accumulatedMl = 0f

                detectDragGestures(
                    onDragStart = { offset ->
                        lastAngle = angleOf(offset, center)
                        accumulatedMl = 0f
                    },
                    onDragEnd = {
                        lastAngle = null
                        onDragEnd()
                    },
                    onDragCancel = {
                        lastAngle = null
                        onDragEnd()
                    }
                ) { change, _ ->
                    change.consume()
                    val previous = lastAngle ?: return@detectDragGestures
                    val current = angleOf(change.position, center)
                    var delta = current - previous
                    if (delta > 180f) delta -= 360f
                    if (delta < -180f) delta += 360f
                    lastAngle = current

                    accumulatedMl += delta / 360f * ML_PER_TURN
                    val steps = (accumulatedMl / DRAG_STEP_ML).toInt()
                    if (steps != 0) {
                        accumulatedMl -= steps * DRAG_STEP_ML
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDrag(steps * DRAG_STEP_ML)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = IteraColors.BorderStrong,
            strokeWidth = 4.dp
        )
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxSize(),
            color = IteraColors.Accent,
            strokeWidth = 4.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalMl",
                style = MaterialTheme.typography.titleLarge,
                color = if (isDragging) IteraColors.Accent else IteraColors.TextPrimary
            )
            Text(
                text = "/ $goalMl ml",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
            Text(
                text = if (isDragging) "${(progress * 100).toInt()}% · AJUSTANDO"
                else "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
    }
}

private fun angleOf(position: Offset, center: Offset): Float {
    val dx = position.x - center.x
    val dy = position.y - center.y
    return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
}

@Composable
private fun QuickAmountButton(
    amountMl: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "+$amountMl",
            style = MaterialTheme.typography.titleLarge,
            color = IteraColors.Accent,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
    }
}