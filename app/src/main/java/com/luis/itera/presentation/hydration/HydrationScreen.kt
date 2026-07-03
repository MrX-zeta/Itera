package com.luis.itera.presentation.hydration

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.HydrationIntake
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
            val todayEpoch = LocalDate.now().toEpochDay()
            val yesterdayEpoch = todayEpoch - 1

            state.intakesByDay
                .toSortedMap(compareByDescending { it })
                .forEach { (epochDay, dayIntakes) ->
                    item(key = "card_$epochDay") {
                        DayIntakeCard(
                            epochDay = epochDay,
                            todayEpoch = todayEpoch,
                            yesterdayEpoch = yesterdayEpoch,
                            intakes = dayIntakes.sortedByDescending { it.dateTimeEpochMillis },
                            onDelete = viewModel::onDeleteIntake
                        )
                        Spacer(Modifier.height(10.dp))
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
private fun DayIntakeCard(
    epochDay: Long,
    todayEpoch: Long,
    yesterdayEpoch: Long,
    intakes: List<HydrationIntake>,
    onDelete: (HydrationIntake) -> Unit
) {
    val dayLabel = when (epochDay) {
        todayEpoch -> "Hoy"
        yesterdayEpoch -> "Ayer"
        else -> LocalDate.ofEpochDay(epochDay).format(dayFormatter)
            .replaceFirstChar { it.uppercase() }
    }
    val dayTotal = intakes.sumOf { it.amountMl }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.Surface)
            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextPrimary
            )
            Text(
                text = "$dayTotal ml",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = IteraColors.Border
        )
        intakes.forEachIndexed { index, intake ->
            DismissableIntakeRow(intake = intake, onDelete = onDelete)
            if (index < intakes.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    thickness = 0.5.dp,
                    color = IteraColors.Border
                )
            }
        }
    }
}

@Composable
private fun DismissableIntakeRow(
    intake: HydrationIntake,
    onDelete: (HydrationIntake) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDelete(intake)
                true
            } else false
        },
        positionalThreshold = { it * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(IteraColors.Error)
                    .padding(horizontal = 16.dp),
                contentAlignment = alignment
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_trash),
                    contentDescription = null,
                    tint = IteraColors.Background,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(IteraColors.Surface)
                .padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                style = MaterialTheme.typography.bodyMedium,
                color = if (intake.amountMl >= 0) IteraColors.TextPrimary else IteraColors.Error
            )
        }
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
    var triggerAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggerAnimation = true }

    val targetProgress = when {
        isDragging -> progress
        triggerAnimation -> progress
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = if (isDragging) 0 else 1000,
            easing = FastOutSlowInEasing
        ),
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