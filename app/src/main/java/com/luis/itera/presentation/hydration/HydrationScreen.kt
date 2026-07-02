package com.luis.itera.presentation.hydration

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.theme.IteraColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val quickAmounts = listOf(250 to "VASO", 500 to "BOTELLA", 1000 to "LITRO")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val ML_PER_DP = 2.5f
private const val DRAG_STEP_ML = 50

@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "HIDRATACIÓN · HOY",
            style = MaterialTheme.typography.labelSmall,
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

        Spacer(Modifier.height(20.dp))

        Text(
            text = "REGISTRO",
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            items(state.intakes, key = { it.id }) { intake ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                        color = if (intake.amountMl >= 0) IteraColors.TextPrimary
                        else IteraColors.TextSecondary
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
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
                var accumulatedPx = 0f
                detectVerticalDragGestures(
                    onDragEnd = {
                        accumulatedPx = 0f
                        onDragEnd()
                    },
                    onDragCancel = {
                        accumulatedPx = 0f
                        onDragEnd()
                    }
                ) { change, dragAmount ->
                    change.consume()
                    accumulatedPx -= dragAmount
                    val stepsMl = (accumulatedPx / density * ML_PER_DP / DRAG_STEP_ML)
                        .roundToInt() * DRAG_STEP_ML
                    if (stepsMl != 0) {
                        accumulatedPx = 0f
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDrag(stepsMl)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = IteraColors.Border,
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
                text = if (isDragging) "AJUSTANDO" else "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
    }
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
            .border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp))
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