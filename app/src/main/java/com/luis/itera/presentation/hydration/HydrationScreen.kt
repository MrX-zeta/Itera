package com.luis.itera.presentation.hydration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.luis.itera.presentation.components.ActivityHeatmapCard
import com.luis.itera.presentation.components.HEATMAP_EMPTY_CELL_COLOR
import com.luis.itera.presentation.components.heatmapDateFormatter
import com.luis.itera.presentation.components.rememberReduceMotion
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
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

private enum class HydrationTab { HOY, HISTORIAL }

@Composable
fun HydrationScreen(
    onSettingsClick: () -> Unit = {},
    viewModel: HydrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val historyDays by viewModel.historyDays.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(HydrationTab.HOY) }

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
                "HIDRATACIÓN",
                style = MaterialTheme.typography.titleMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            if (state.showWeightPrompt) {
                Spacer(Modifier.height(12.dp))
                WeightPromptBanner(
                    weightKg = state.userWeightKg,
                    onAdjustClick = onSettingsClick,
                    onDismiss = viewModel::onDismissWeightPrompt
                )
            }
            Spacer(Modifier.height(16.dp))
            HydrationTabToggle(tab, onTabChange = { tab = it })
            Spacer(Modifier.height(16.dp))

            val reduceMotion = rememberReduceMotion()
            AnimatedContent(
                targetState = tab,
                label = "hydration_tab",
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    if (reduceMotion) {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        val towards = if (forward) {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        }
                        (slideIntoContainer(towards, tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)))
                            .togetherWith(slideOutOfContainer(towards, tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)))
                    }
                }
            ) { targetTab ->
                when (targetTab) {
                    HydrationTab.HOY -> TodayTab(
                        state = state,
                        onAddIntake = viewModel::onAddIntake,
                        onDrag = viewModel::onDrag,
                        onDragEnd = viewModel::onDragEnd,
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
                    HydrationTab.HISTORIAL -> HistoryTab(days = historyDays)
                }
            }
        }
    }
}

/**
 * Aviso discreto y NO bloqueante: la meta de agua se calcula desde el peso, y el
 * onboarding ya no lo pide. "Ajustar" navega a Ajustes (sin descartar el aviso: solo
 * distraerse allí sin tocar el peso no debe hacerlo desaparecer). Únicamente la X es
 * la señal explícita de "no me interesa"; cambiar el peso de verdad también lo cierra
 * (ver [com.luis.itera.data.local.UserPrefsDataStore.setUserWeightKg]).
 */
@Composable
private fun WeightPromptBanner(weightKg: Float, onAdjustClick: () -> Unit, onDismiss: () -> Unit) {
    val weightLabel = if (weightKg % 1f == 0f) weightKg.toInt().toString() else "%.1f".format(weightKg)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IteraColors.Surface)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Tu meta está calculada para $weightLabel kg",
            style = MaterialTheme.typography.bodySmall,
            color = IteraColors.TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            "AJUSTAR",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = LocalAccent.current.color,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onAdjustClick)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        )
        Icon(
            ImageVector.vectorResource(R.drawable.ic_close),
            contentDescription = "Descartar aviso",
            tint = IteraColors.TextTertiary,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDismiss)
                .padding(8.dp)
                .size(16.dp)
        )
    }
}

@Composable
private fun HydrationTabToggle(selected: HydrationTab, onTabChange: (HydrationTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IteraColors.SurfaceElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        HydrationTab.entries.forEach { entry ->
            val isSelected = entry == selected
            Text(
                text = if (entry == HydrationTab.HOY) "HOY" else "HISTORIAL",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) LocalAccent.current.onAccent else IteraColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LocalAccent.current.color else IteraColors.SurfaceElevated)
                    .clickable { onTabChange(entry) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TodayTab(
    state: HydrationUiState,
    onAddIntake: (Int) -> Unit,
    onDrag: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onSwipeDelete: (HydrationIntake) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        DraggableProgressRing(
            rawProgress = state.rawProgress,
            totalMl = state.displayTotalMl,
            goalMl = state.goal?.totalGoalMl ?: 0,
            isDragging = state.dragDeltaMl != 0,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickAmounts.forEach { (amount, label) ->
                QuickAmountButton(amount, label, { onAddIntake(amount) }, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            "REGISTRO DE HOY",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))

        val todayEpoch = LocalDate.now().toEpochDay()
        val todayIntakes = state.intakesByDay[todayEpoch].orEmpty().sortedByDescending { it.dateTimeEpochMillis }

        LazyColumn(Modifier.weight(1f)) {
            if (todayIntakes.isEmpty()) {
                item {
                    Text(
                        "Aún no has registrado agua hoy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                item(key = "card_today") {
                    DayIntakeCard(
                        epochDay = todayEpoch,
                        todayEpoch = todayEpoch,
                        yesterdayEpoch = todayEpoch - 1,
                        intakes = todayIntakes,
                        onSwipeDelete = onSwipeDelete
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryTab(days: List<HydrationDayStat>) {
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    val byDay = remember(days) { days.associateBy { LocalDate.ofEpochDay(it.dateEpochDay) } }
    val recentFirst = remember(days) { days.sortedByDescending { it.dateEpochDay } }
    val todayEpoch = remember { LocalDate.now().toEpochDay() }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ActivityHeatmapCard(
                levelForDate = { date ->
                    val stat = byDay[date]
                    when {
                        stat == null || stat.totalMl <= 0 -> 0
                        stat.percent >= 100 -> 2
                        else -> 1
                    }
                },
                colorForLevel = { level ->
                    when (level) {
                        2 -> IteraColors.HydrationAccent
                        1 -> IteraColors.HydrationAccentMedium
                        else -> HEATMAP_EMPTY_CELL_COLOR
                    }
                },
                emptyBorderColor = IteraColors.HydrationAccent,
                filledBorderColor = IteraColors.HydrationOnAccent,
                selectedDate = selectedDay,
                onDateSelected = { selectedDay = it },
                selectionLabel = { cell, isToday ->
                    val datePart = if (isToday) "Hoy" else cell.date.format(heatmapDateFormatter)
                    val stat = byDay[cell.date]
                    if (stat != null && stat.totalMl > 0) "$datePart · ${stat.totalMl} ml · ${stat.percent}%" else datePart
                },
                legend = { HydrationHeatmapLegend() },
                title = "MAPA DE HIDRATACIÓN",
                emptyStateLabel = "Aún no hay registros de agua"
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text(
                "REGISTRO POR DÍA",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary
            )
        }
        item { Spacer(Modifier.height(4.dp)) }
        items(recentFirst, key = { it.dateEpochDay }) { stat ->
            HydrationDayRow(stat, todayEpoch)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HydrationDayRow(stat: HydrationDayStat, todayEpoch: Long) {
    val date = LocalDate.ofEpochDay(stat.dateEpochDay)
    val dayLabel = when (stat.dateEpochDay) {
        todayEpoch -> "Hoy"
        todayEpoch - 1 -> "Ayer"
        else -> date.format(dayFormatter).replaceFirstChar { it.uppercase() }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.SurfaceElevated)
            .padding(vertical = 12.dp, horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                dayLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (stat.intakeCount == 1) "1 registro" else "${stat.intakeCount} registros",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${stat.totalMl} ml",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${stat.percent}% de meta",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.HydrationAccentMedium
            )
        }
    }
}

/**
 * Leyenda del heatmap de HIDRATACIÓN: parcial / meta cumplida. A diferencia del PR en
 * entrenamiento, beber de más NO es un logro que premiar — cumplir la meta ya es el
 * objetivo, así que aquí no hay un tercer realce ámbar.
 */
@Composable
private fun HydrationHeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(IteraColors.HydrationAccentMedium))
        Spacer(Modifier.width(8.dp))
        Text("parcial", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(IteraColors.HydrationAccent))
        Spacer(Modifier.width(8.dp))
        Text("meta cumplida", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
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
            .background(IteraColors.SurfaceElevated)
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
                color = LocalAccent.current.color
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
            // Solo revela el panel rojo cuando el offset es realmente hacia la izquierda: así
            // el pequeño "epsilon" de tolerancia táctil hacia la derecha no muestra nada (igual
            // que en Historial), en vez de asomar una porción de rojo sin razón.
            val offsetPx = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
            if (offsetPx < 0f) {
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
        }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(IteraColors.SurfaceElevated)
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
    rawProgress: Float,
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
        isDragging -> rawProgress
        triggerAnimation -> rawProgress
        else -> 0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(if (isDragging) 0 else 1000, easing = FastOutSlowInEasing),
        label = "hydration_progress"
    )
    val base = animatedProgress.coerceIn(0f, 1f)
    val overflow = (animatedProgress - 1f).coerceIn(0f, 1f)
    val percent = ((if (isDragging) rawProgress else animatedProgress) * 100).toInt()

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
            progress = { base },
            modifier = Modifier.fillMaxSize(),
            color = LocalAccent.current.color,
            strokeWidth = 4.dp
        )
        if (animatedProgress > 1f) {
            CircularProgressIndicator(
                progress = { overflow },
                modifier = Modifier.fillMaxSize(),
                color = LocalAccent.current.color.copy(alpha = 0.45f),
                strokeWidth = 4.dp
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$totalMl", style = MaterialTheme.typography.titleLarge, color = if (isDragging) LocalAccent.current.color else IteraColors.TextPrimary)
            Text("/ $goalMl ml", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
            Text(
                if (isDragging) "$percent% · AJUSTANDO" else "$percent%",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextPrimary
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
            .clip(RoundedCornerShape(10.dp))
            .background(IteraColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("+$amountMl", style = MaterialTheme.typography.titleLarge, color = LocalAccent.current.color, textAlign = TextAlign.Center)
        Text(label, style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
    }
}