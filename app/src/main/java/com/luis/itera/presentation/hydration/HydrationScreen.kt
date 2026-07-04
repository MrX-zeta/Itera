package com.luis.itera.presentation.hydration

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
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
fun HydrationScreen(viewModel: HydrationViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = IteraColors.Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(padding)
                .imePadding()
                .padding(16.dp)
        ) {
            Text(
                "HIDRATACIÓN · HOY",
                style = MaterialTheme.typography.titleMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))

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
                    QuickAmountButton(amount, label, { viewModel.onAddIntake(amount) }, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(
                "REGISTRO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary
            )
            Spacer(Modifier.height(8.dp))

            val todayEpoch = LocalDate.now().toEpochDay()
            val yesterdayEpoch = todayEpoch - 1
            val sortedDays = state.intakesByDay
                .toSortedMap(compareByDescending { it })
                .filter { it.value.isNotEmpty() }

            LazyColumn(Modifier.weight(1f)) {
                sortedDays.forEach { (epochDay, dayIntakes) ->
                    item(key = "card_$epochDay") {
                        DayIntakeCard(
                            epochDay = epochDay,
                            todayEpoch = todayEpoch,
                            yesterdayEpoch = yesterdayEpoch,
                            intakes = dayIntakes.sortedByDescending { it.dateTimeEpochMillis },
                            onSwipeDelete = { intake ->
                                viewModel.stageForDeletion(intake)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Registro eliminado",
                                        actionLabel = "DESHACER",
                                        duration = SnackbarDuration.Short
                                    )
                                    when (result) {
                                        SnackbarResult.ActionPerformed -> viewModel.undoDeletion(intake.id)
                                        SnackbarResult.Dismissed -> viewModel.commitDeletion(intake.id)
                                    }
                                }
                            }
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
}

@Composable
private fun DayIntakeCard(
    epochDay: Long,
    todayEpoch: Long,
    yesterdayEpoch: Long,
    intakes: List<HydrationIntake>,
    onSwipeDelete: (HydrationIntake) -> Unit
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
            .wrapContentHeight()
            .animateContentSize(tween(300))
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
                dayLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextPrimary
            )
            Text(
                "$dayTotal ml",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.Accent
            )
        }
        HorizontalDivider(
            Modifier.padding(vertical = 8.dp),
            thickness = 0.5.dp,
            color = IteraColors.Border
        )
        intakes.forEachIndexed { index, intake ->
            key(intake.id) {
                DismissableIntakeRow(intake, onSwipeDelete)
            }
            if (index < intakes.lastIndex) {
                HorizontalDivider(
                    Modifier.padding(vertical = 2.dp),
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
    onSwipeDelete: (HydrationIntake) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onSwipeDelete(intake)
                true
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(IteraColors.Error)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_trash),
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
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                Instant.ofEpochMilli(intake.dateTimeEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(timeFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
            Text(
                "${if (intake.amountMl >= 0) "+" else ""}${intake.amountMl} ml",
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
        animationSpec = tween(if (isDragging) 0 else 1000, easing = FastOutSlowInEasing),
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
                    onDragStart = { lastAngle = angleOf(it, center); accumulatedMl = 0f },
                    onDragEnd = { lastAngle = null; onDragEnd() },
                    onDragCancel = { lastAngle = null; onDragEnd() }
                ) { change, _ ->
                    change.consume()
                    val prev = lastAngle ?: return@detectDragGestures
                    val cur = angleOf(change.position, center)
                    var d = cur - prev
                    if (d > 180f) d -= 360f
                    if (d < -180f) d += 360f
                    lastAngle = cur
                    accumulatedMl += d / 360f * ML_PER_TURN
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
            Text("$totalMl", style = MaterialTheme.typography.titleLarge, color = if (isDragging) IteraColors.Accent else IteraColors.TextPrimary)
            Text("/ $goalMl ml", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
            Text(
                if (isDragging) "${(progress * 100).toInt()}% · AJUSTANDO" else "${(animatedProgress * 100).toInt()}%",
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
private fun QuickAmountButton(amountMl: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("+$amountMl", style = MaterialTheme.typography.titleLarge, color = IteraColors.Accent, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
    }
}