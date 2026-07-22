package com.luis.itera.presentation.active_workout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.luis.itera.domain.repository.SaveRoutineResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.Routine
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.components.ConfettiOverlay
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.components.RestTimerOverlay
import com.luis.itera.presentation.components.RoutineAccent
import com.luis.itera.presentation.components.rememberReduceMotion
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import com.luis.itera.presentation.theme.RoutineColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val homeDateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale("es"))
private const val MAX_HOME_ROUTINES = 4
private const val FREQUENT_EXERCISES_COUNT = 5

// Orden de RENDERIZADO de los chips de foco (2 filas, 4+3), distinto del orden del enum.
// No afecta selección/conflictos: esos operan sobre Set<WorkoutFocus>, sin importar el orden.
private val FOCUS_ROW_TOP = listOf(WorkoutFocus.PUSH, WorkoutFocus.PULL, WorkoutFocus.LEGS, WorkoutFocus.CARDIO)
private val FOCUS_ROW_BOTTOM = listOf(WorkoutFocus.UPPER, WorkoutFocus.LOWER, WorkoutFocus.FULL_BODY)

@Composable
fun ActiveWorkoutScreen(
    onSessionFinished: (Long) -> Unit,
    onLastSessionClick: (Long) -> Unit,
    onHydrationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeeAllRoutinesClick: () -> Unit,
    // Si se arrancó una rutina desde la pestaña Rutinas, el atrás vuelve allí (no sale a Home).
    onBackToRoutines: () -> Unit = {},
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finishedSessionId by viewModel.finishedSessionId.collectAsStateWithLifecycle()
    val returnToRoutines by viewModel.returnToRoutines.collectAsStateWithLifecycle()
    val screenScope = rememberCoroutineScope()
    BackHandler(enabled = returnToRoutines) {
        viewModel.disarmReturnToRoutines()
        onBackToRoutines()
    }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(finishedSessionId) {
        finishedSessionId?.let { viewModel.onFinishedSessionConsumed(); onSessionFinished(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.routineFeedback.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.setBlockedMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    val restGoalSeconds by viewModel.restGoalSeconds.collectAsStateWithLifecycle()
    val setCounts by viewModel.setCountsByExercise.collectAsStateWithLifecycle()
    val routineLoaded by viewModel.routineLoaded.collectAsStateWithLifecycle()
    val pendingZeroWeight by viewModel.pendingZeroWeightConfirm.collectAsStateWithLifecycle()
    if (pendingZeroWeight) {
        ZeroWeightDialog(
            exerciseName = state.selectedExercise?.name,
            onDismiss = viewModel::onDismissZeroWeight,
            onConfirm = viewModel::onConfirmZeroWeight
        )
    }
    val reduceMotion = rememberReduceMotion()
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.session != null,
            label = "home_session",
            transitionSpec = {
                if (returnToRoutines) {
                    // Volviendo a Rutinas: el pager lleva TODA la transición visual (desliza
                    // sobre este vacío). Un fundido propio aquí crearía un dip a negro antes
                    // del deslizamiento — un destello peor que ninguna animación.
                    EnterTransition.None togetherWith ExitTransition.None
                } else if (reduceMotion) {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                } else {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                }
            }
        ) { hasSession ->
            if (!hasSession) {
                // Si se está arrancando una rutina (desde Rutinas), la sesión aún no existe por
                // unas milésimas: NO mostrar Home (dejar vacío) para no ver el flash de Home antes.
                if (!returnToRoutines) {
                    HomeContent(
                        state,
                        viewModel::onFocusToggle,
                        viewModel::onStartSession,
                        onLastSessionClick,
                        onHydrationClick,
                        onSettingsClick,
                        onSeeAllRoutinesClick,
                        viewModel::onStartRoutine,
                        viewModel::onWeeklyGoalChange,
                        viewModel::onClearFocuses
                    )
                }
            } else {
                ActiveSessionContent(
                state = state,
                muscleGroups = viewModel.muscleGroups,
                restGoalSeconds = restGoalSeconds,
                setCounts = setCounts,
                routineLoaded = routineLoaded,
                onLoadRoutine = viewModel::onLoadRoutine,
                onClearRoutine = viewModel::onClearRoutine,
                onCreateExercise = viewModel::onCreateExercise,
                onSearchChange = viewModel::onSearchQueryChange,
                onExerciseSelected = viewModel::onExerciseSelected,
                onRepsDelta = viewModel::onRepsDelta,
                onWeightDelta = viewModel::onWeightDelta,
                onRegisterSet = viewModel::onRegisterSet,
                onDeleteSet = viewModel::onDeleteSet,
                onDiscardSession = {
                    // La flecha "←": el DESCARTE es SIEMPRE inmediato (nunca retrasado) — si el
                    // usuario encadena varias rutinas rápido, un descarte tardío borraría la
                    // sesión equivocada (la nueva, no la abandonada), dejando huérfanas. Solo el
                    // DESARME de returnToRoutines espera a que el pager termine de deslizar, para
                    // seguir ocultando el Home mientras tanto (sin fundido propio: instantáneo).
                    viewModel.onDiscardSession()
                    if (returnToRoutines) {
                        onBackToRoutines()
                        screenScope.launch {
                            delay(400)
                            viewModel.disarmReturnToRoutines()
                        }
                    }
                },
                onFinishSession = viewModel::onFinishSession,
                onDurationDelta = viewModel::onDurationDelta,
                onIntensityDelta = viewModel::onIntensityDelta,
                onToggleTimerPause = viewModel::onToggleTimerPause,
                onSaveRoutine = viewModel::onSaveRoutine
            )
            }
        }
        SnackbarHost(snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    state: ActiveWorkoutUiState,
    onFocusToggle: (WorkoutFocus) -> Unit,
    onStart: () -> Unit,
    onLastSessionClick: (Long) -> Unit,
    onHydrationClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSeeAllRoutinesClick: () -> Unit,
    onStartRoutine: (Routine) -> Unit,
    onWeeklyGoalChange: (Int) -> Unit,
    onClearFocuses: () -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showFocusPicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp, bottom = 12.dp)) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(LocalDate.now().format(homeDateFormatter).uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    val goalReached = state.streak.sessionsThisWeek >= state.streak.weeklyGoal
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showGoalDialog = true }
                    ) {
                        Text(
                            "${state.streak.sessionsThisWeek}/${state.streak.weeklyGoal} esta semana",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (goalReached) LocalAccent.current.color else IteraColors.TextPrimary
                        )
                        if (state.streak.weeks > 0) {
                            Text(
                                " · racha ${state.streak.weeks} sem",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (goalReached) LocalAccent.current.color else IteraColors.TextSecondary
                            )
                        }
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Ajustes",
                            tint = IteraColors.TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    MiniHydrationRing(state.hydrationProgress, onHydrationClick)
                }
            }
            Spacer(Modifier.height(16.dp))
            WeekProgressBar(state.trainedDaysThisWeek, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(18.dp))
            state.lastFinishedSession?.let { last ->
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("ÚLTIMA SESIÓN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(IteraColors.SurfaceElevated).clickable { onLastSessionClick(last.id) }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(IteraColors.Surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_barbell), null, tint = LocalAccent.current.color, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(WorkoutFocus.fromStored(last.focus).takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.label } ?: "SESIÓN ${last.id}", style = MaterialTheme.typography.titleMedium, color = IteraColors.TextPrimary)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${relativeDay(last.dateEpochDay)} · ", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                                if (last.durationMinutes < 1) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_flash), null, tint = IteraColors.TextSecondary, modifier = Modifier.size(14.dp))
                                    Text(" Rápida", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                                } else {
                                    Text("${last.durationMinutes} min", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                                }
                                Text(" · ${last.sets.size} sets", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                            }
                        }
                        Icon(ImageVector.vectorResource(R.drawable.ic_chevron_right), contentDescription = null, tint = IteraColors.TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            if (state.routines.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("TUS RUTINAS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    state.routines.take(MAX_HOME_ROUTINES).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            pair.forEach { routine ->
                                RoutineCard(routine, onClick = { onStartRoutine(routine) }, modifier = Modifier.weight(1f))
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (state.routines.size > MAX_HOME_ROUTINES) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onSeeAllRoutinesClick)
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ver todas (${state.routines.size})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = IteraColors.TextSecondary
                            )
                            Icon(ImageVector.vectorResource(R.drawable.ic_chevron_right), contentDescription = null, tint = IteraColors.TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { showFocusPicker = true },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current.color, contentColor = LocalAccent.current.onAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(ImageVector.vectorResource(R.drawable.ic_widget_play), null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("INICIAR ENTRENAMIENTO", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (showGoalDialog) {
        WeeklyGoalDialog(
            current = state.streak.weeklyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { onWeeklyGoalChange(it); showGoalDialog = false }
        )
    }

    if (showFocusPicker) {
        FocusPickerSheet(
            selectedFocuses = state.selectedFocuses,
            blockedFocuses = state.blockedFocuses,
            suggestedFocus = state.suggestedFocus,
            onFocusToggle = onFocusToggle,
            onClearFocuses = onClearFocuses,
            onDismiss = { showFocusPicker = false },
            onConfirmStart = {
                showFocusPicker = false
                onStart()
            }
        )
    }
}

@Composable
private fun WeeklyGoalDialog(current: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var goal by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IteraColors.Surface,
        title = { Text("Meta semanal", color = IteraColors.TextPrimary) },
        text = {
            FastStepper(
                label = "SESIONES",
                value = goal.toFloat(),
                onDelta = { delta -> goal = (goal + delta.toInt()).coerceIn(1, 7) },
                onValueSet = { v -> goal = v.toInt().coerceIn(1, 7) },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Text(
                "GUARDAR",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = LocalAccent.current.color,
                modifier = Modifier.clickable { onConfirm(goal) }.padding(8.dp)
            )
        },
        dismissButton = {
            Text("CANCELAR", style = MaterialTheme.typography.labelMedium, color = IteraColors.TextSecondary, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
        }
    )
}

/**
 * Selector de foco relocado del Home a un modal, disparado al tocar "Iniciar entrenamiento":
 * separa "¿entreno?" (el Home, sin fricción) de "¿qué entreno?" (aquí, solo cuando hace falta).
 * Reusa FocusChip tal cual; el sugerido llega PRE-seleccionado, así que un usuario apurado solo
 * necesita tocar "Iniciar" una vez, sin tocar ningún chip.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FocusPickerSheet(
    selectedFocuses: Set<WorkoutFocus>,
    blockedFocuses: Set<WorkoutFocus>,
    suggestedFocus: WorkoutFocus,
    onFocusToggle: (WorkoutFocus) -> Unit,
    onClearFocuses: () -> Unit,
    onDismiss: () -> Unit,
    onConfirmStart: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (selectedFocuses.isEmpty()) onFocusToggle(suggestedFocus)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IteraColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IteraColors.BorderStrong) }
    ) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Text(
                "¿QUÉ TOCA HOY?",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Sugerencia de hoy: ${suggestedFocus.label}",
                style = MaterialTheme.typography.bodySmall,
                color = LocalAccent.current.color
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FOCUS_ROW_TOP.forEach { focus ->
                        FocusChip(focus, focus in selectedFocuses, focus in blockedFocuses, onFocusToggle)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FOCUS_ROW_BOTTOM.forEach { focus ->
                        FocusChip(focus, focus in selectedFocuses, focus in blockedFocuses, onFocusToggle)
                    }
                }
            }
            Text(
                "Libre · cualquier grupo",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (selectedFocuses.isEmpty()) LocalAccent.current.color else IteraColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClearFocuses)
                    .padding(vertical = 14.dp)
            )
            Button(
                onClick = onConfirmStart,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current.color, contentColor = LocalAccent.current.onAccent),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ic_widget_play), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("INICIAR ENTRENAMIENTO", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun RowScope.FocusChip(
    focus: WorkoutFocus,
    selected: Boolean,
    blocked: Boolean,
    onToggle: (WorkoutFocus) -> Unit
) {
    // Etiqueta solo de PRESENTACIÓN en el chip: "Cuerpo completo" partía en 2 líneas y
    // desalineaba la fila. El valor interno sigue siendo WorkoutFocus.FULL_BODY sin tocar.
    val chipLabel = if (focus == WorkoutFocus.FULL_BODY) "Full body" else focus.label
    Text(
        chipLabel,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = when { selected -> LocalAccent.current.onAccent; blocked -> IteraColors.TextTertiary; else -> IteraColors.TextPrimary },
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) LocalAccent.current.color else IteraColors.SurfaceElevated)
            .clickable(enabled = !blocked) { onToggle(focus) }
            .padding(horizontal = 8.dp, vertical = 10.dp)
    )
}

@Composable
private fun WeekProgressBar(trainedDays: Set<Long>, modifier: Modifier = Modifier) {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val today = LocalDate.now()
    val labels = listOf("L", "M", "M", "J", "V", "S", "D")
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..6).forEach { offset ->
                val day = monday.plusDays(offset.toLong())
                val trained = day.toEpochDay() in trainedDays
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (trained) LocalAccent.current.color else IteraColors.SurfaceElevated)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (0..6).forEach { offset ->
                val day = monday.plusDays(offset.toLong())
                val isToday = day == today
                Text(
                    labels[offset],
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isToday) LocalAccent.current.color else IteraColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RoutineCard(routine: Routine, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Franja de color descriptiva de la rutina, pegada al borde real de la card.
        RoutineAccent(
            RoutineColor.fromOrdinal(routine.color).color,
            Modifier.fillMaxHeight().padding(vertical = 10.dp)
        )
        Column(Modifier.padding(start = 8.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
            Text(routine.name, style = MaterialTheme.typography.titleMedium, color = IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text("${routine.exerciseIds.size} ejercicios", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
        }
    }
}

@Composable
private fun MiniHydrationRing(rawProgress: Float, onClick: () -> Unit) {
    val base = rawProgress.coerceIn(0f, 1f)
    val overflow = (rawProgress - 1f).coerceIn(0f, 1f)
    val percent = (rawProgress * 100).toInt()
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize().padding(4.dp), color = IteraColors.BorderStrong, strokeWidth = 3.dp)
        CircularProgressIndicator(progress = { base }, modifier = Modifier.fillMaxSize().padding(4.dp), color = if (rawProgress > 0f) LocalAccent.current.color else IteraColors.Border, strokeWidth = 3.dp)
        if (rawProgress > 1f) {
            CircularProgressIndicator(progress = { overflow }, modifier = Modifier.fillMaxSize().padding(4.dp), color = LocalAccent.current.color.copy(alpha = 0.45f), strokeWidth = 3.dp)
        }
        Text(
            "$percent%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium).let { if (percent >= 100) it.copy(fontSize = 9.sp) else it },
            color = if (rawProgress > 0f) IteraColors.TextPrimary else IteraColors.TextSecondary
        )
    }
}

private fun relativeDay(epochDay: Long): String {
    val diff = LocalDate.now().toEpochDay() - epochDay
    return when {
        diff <= 0L -> "hoy"
        diff == 1L -> "ayer"
        else -> "hace $diff días"
    }
}

@Composable
private fun ActiveSessionContent(
    state: ActiveWorkoutUiState,
    muscleGroups: List<String>,
    restGoalSeconds: Int,
    setCounts: Map<Long, Int>,
    routineLoaded: Boolean,
    onLoadRoutine: (Routine) -> Unit,
    onClearRoutine: () -> Unit,
    onCreateExercise: (String, String) -> Unit,
    onSearchChange: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onRepsDelta: (Int) -> Unit,
    onWeightDelta: (Float) -> Unit,
    onRegisterSet: () -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onDiscardSession: () -> Unit,
    onFinishSession: () -> Unit,
    onDurationDelta: (Int) -> Unit,
    onIntensityDelta: (Int) -> Unit,
    onToggleTimerPause: () -> Unit,
    onSaveRoutine: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    // Estado LOCAL del descanso: se re-crea al empezar un descanso nuevo (cambia
    // setTimerMillis). No toca el ViewModel: el restSeconds persistido sigue siendo el real.
    var restSkipped by remember(state.setTimerMillis) { mutableStateOf(false) }
    var restExtraSeconds by remember(state.setTimerMillis) { mutableStateOf(0) }
    // Selector de ejercicios contenido: por defecto solo FRECUENTES; la lista completa a un toque.
    var allExpanded by remember { mutableStateOf(false) }
    var showRoutinePicker by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .padding(16.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_back), null,
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDiscardSession).padding(end = 16.dp)
                )
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ENTRENAMIENTO ACTIVO", style = MaterialTheme.typography.titleMedium, color = IteraColors.TextSecondary)
                    if (state.sessionFocuses.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(state.sessionFocuses.joinToString(" · ") { it.label }, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = LocalAccent.current.color)
                    }
                }
            }
            state.selectedExercise?.let { exercise ->
                val isCardio = exercise.mainMuscleGroup.equals("Cardio", ignoreCase = true)
                Spacer(Modifier.height(16.dp))
                Text(exercise.name, style = MaterialTheme.typography.headlineSmall, color = IteraColors.TextPrimary)
                if (!isCardio && state.lastSets.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    val lastSessionId = state.lastSets.first().sessionId
                    val lastSessionSets = state.lastSets.filter { it.sessionId == lastSessionId }
                    val first = lastSessionSets.first()
                    val count = lastSessionSets.size
                    val weight = first.weightAddedKg
                    val repsVal = first.reps
                    Text(
                        text = buildString {
                            append("Última vez: ${count}×${repsVal}")
                            if (weight > 0f) append(" +${if (weight % 1f == 0f) "${weight.toInt()}" else "%.1f".format(weight)}kg")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                    val suggestion = state.suggestion
                    if (suggestion != null) {
                        Spacer(Modifier.height(4.dp))
                        val isWeightUp = suggestion.contains("↑")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sugerido: ${suggestion.replace(" ↑ peso", "")}", style = MaterialTheme.typography.bodyMedium, color = LocalAccent.current.color)
                            if (isWeightUp) {
                                Spacer(Modifier.width(4.dp))
                                Icon(ImageVector.vectorResource(R.drawable.ic_weight_up), null, tint = LocalAccent.current.color, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isCardio) {
                        FastStepper("MINUTOS", state.durationSeconds / 60f, { onDurationDelta((it * 60).toInt()) }, modifier = Modifier.weight(1f))
                        FastStepper("NIVEL", state.intensity.toFloat(), { onIntensityDelta(it.toInt()) }, modifier = Modifier.weight(1f))
                    } else {
                        FastStepper("REPS", state.reps.toFloat(), { onRepsDelta(it.toInt()) }, modifier = Modifier.weight(1f))
                        FastStepper("+KG", state.weightKg, onWeightDelta, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(16.dp))
                RegisterSetButton(onRegisterSet, state.prCelebrationText)
            }
            Spacer(Modifier.height(16.dp))
            LazyColumn(Modifier.weight(1f)) {
                // SETS DE HOY del ejercicio seleccionado (numerados, con logro ámbar vía set.isPr)
                val selected = state.selectedExercise
                val todaySets = if (selected != null) state.session?.sets.orEmpty().filter { it.exerciseId == selected.id } else emptyList()
                if (todaySets.isNotEmpty()) {
                    item(key = "sets_header") {
                        Text("SETS DE HOY · ${todaySets.size}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = IteraColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    itemsIndexed(todaySets, key = { _, s -> "set_${s.id}" }) { index, set ->
                        // Fila COMPACTA: "SET n · 10 reps · +12 kg" en una línea, borrar discreto.
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(IteraColors.SurfaceElevated).padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SET ${index + 1}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextPrimary)
                            Text(
                                buildString {
                                    append(" · ")
                                    if (set.durationSeconds > 0) { append("${set.durationSeconds / 60} min"); if (set.intensity > 0) append(" · nivel ${set.intensity}") }
                                    else { append("${set.reps} reps"); if (set.weightAddedKg > 0f) append(" · +${if (set.weightAddedKg % 1f == 0f) "${set.weightAddedKg.toInt()}" else "%.1f".format(set.weightAddedKg)} kg") }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = IteraColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (set.isPr) {
                                Text(
                                    "PR",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IteraColors.Achievement,
                                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(IteraColors.Achievement.copy(alpha = 0.16f)).padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(ImageVector.vectorResource(R.drawable.ic_trash), contentDescription = "Borrar set", tint = IteraColors.TextTertiary, modifier = Modifier.size(22.dp).clickable { onDeleteSet(set) }.padding(3.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                    item(key = "sets_gap") { Spacer(Modifier.height(10.dp)) }
                }
                // Selector CONTENIDO sobre superficie diferenciada (separa visualmente de los
                // steppers de arriba). Por defecto solo los frecuentes (los de esta sesión
                // primero, luego los más usados históricamente en el grupo); buscar, cargar
                // rutina o "Ver todos" dan acceso al resto sin lista infinita por defecto.
                item(key = "selector_block") {
                    val query = state.searchQuery
                    val showAll = query.isNotBlank() || routineLoaded || allExpanded
                    val sessionUsedIds = state.session?.sets?.map { it.exerciseId }?.distinct().orEmpty()
                    val visibleExercises = if (showAll) state.exercises else {
                        val inSession = state.exercises.filter { it.id in sessionUsedIds }.sortedBy { sessionUsedIds.indexOf(it.id) }
                        val byUsage = state.exercises.filterNot { it.id in sessionUsedIds }.sortedByDescending { setCounts[it.id] ?: 0 }
                        (inSession + byUsage).take(FREQUENT_EXERCISES_COUNT)
                    }
                    val listHeader = when {
                        routineLoaded -> "RUTINA"
                        query.isNotBlank() -> "RESULTADOS"
                        allExpanded -> "EJERCICIOS"
                        else -> "FRECUENTES"
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            .background(IteraColors.SurfaceSubtle)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(IteraColors.SurfaceElevated)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_search), contentDescription = null, tint = IteraColors.TextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(Modifier.weight(1f)) {
                                    if (query.isEmpty()) {
                                        Text("Buscar ejercicio", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                                    }
                                    BasicTextField(
                                        value = query,
                                        onValueChange = onSearchChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = IteraColors.TextPrimary),
                                        cursorBrush = SolidColor(LocalAccent.current.color),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    Icon(
                                        ImageVector.vectorResource(R.drawable.ic_close),
                                        contentDescription = "Limpiar búsqueda",
                                        tint = IteraColors.TextSecondary,
                                        modifier = Modifier.size(16.dp).clickable { onSearchChange("") }
                                    )
                                }
                            }
                            if (state.routines.isNotEmpty()) {
                                // Siempre tocable: con rutina cargada, permite cargar OTRA o
                                // soltarla (opción "Sesión libre" dentro del propio picker).
                                val routineColor = if (routineLoaded) LocalAccent.current.color else IteraColors.TextPrimary
                                Row(
                                    Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IteraColors.SurfaceElevated)
                                        .clickable { showRoutinePicker = true }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(ImageVector.vectorResource(R.drawable.ic_list), contentDescription = null, tint = routineColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Rutina", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = routineColor)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(listHeader, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp), color = IteraColors.TextSecondary)
                        Spacer(Modifier.height(8.dp))
                        visibleExercises.forEach { exercise ->
                            val sel = exercise.id == state.selectedExercise?.id
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).then(if (sel) Modifier.background(IteraColors.SurfaceElevated) else Modifier).clickable { onExerciseSelected(exercise) }.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(exercise.name, style = MaterialTheme.typography.bodyMedium, color = if (sel) LocalAccent.current.color else IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Text(exercise.mainMuscleGroup, style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                        if (!showAll && state.exercises.size > visibleExercises.size) {
                            val focusLabel = state.sessionFocuses.joinToString(" · ") { it.label }.ifEmpty { "la sesión" }
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { allExpanded = true }.padding(vertical = 10.dp, horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ver todos los de $focusLabel (${state.exercises.size})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                                Icon(ImageVector.vectorResource(R.drawable.ic_chevron_right), contentDescription = null, tint = IteraColors.TextSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                        if (allExpanded && query.isBlank() && !routineLoaded) {
                            Text(
                                "Mostrar menos",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = IteraColors.TextSecondary,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { allExpanded = false }.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                        if (query.isNotBlank() && state.exercises.isEmpty()) {
                            CreateExercisePrompt(query, muscleGroups, onCreateExercise)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val hasSets = state.session?.sets?.isNotEmpty() == true
            val alreadyRoutine = state.matchingRoutine != null
            if (hasSets && !alreadyRoutine) {
                var showDialog by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DiscreetAction("Guardar rutina", R.drawable.ic_bookmark, IteraColors.TextPrimary, Modifier.weight(1f)) { showDialog = true }
                    DiscreetAction("Finalizar", R.drawable.ic_check, IteraColors.TextPrimary, Modifier.weight(1f), onClick = onFinishSession)
                }
                if (showDialog) {
                    SaveRoutineDialog(
                        onDismiss = { showDialog = false },
                        onSave = { name -> onSaveRoutine(name); showDialog = false }
                    )
                }
            } else {
                DiscreetAction("Finalizar sesión", R.drawable.ic_check, IteraColors.TextPrimary, Modifier.fillMaxWidth(), onClick = onFinishSession)
            }
        }
        if (showRoutinePicker) {
            RoutinePickerDialog(
                routines = state.routines,
                routineLoaded = routineLoaded,
                onDismiss = { showRoutinePicker = false },
                onPick = { routine ->
                    showRoutinePicker = false
                    onLoadRoutine(routine)
                },
                onClear = {
                    showRoutinePicker = false
                    onClearRoutine()
                }
            )
        }
        if (state.setTimerMillis > 0L && !restSkipped) {
            RestTimerOverlay(
                startMillis = state.setTimerMillis,
                pausedElapsed = state.pausedElapsed,
                state = state.timerState,
                goalSeconds = restGoalSeconds,
                extraSeconds = restExtraSeconds,
                nextHint = state.suggestion?.replace(" ↑ peso", ""),
                onTogglePause = onToggleTimerPause,
                onAddThirty = { restExtraSeconds += 30 },
                onSkip = { restSkipped = true },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
        ConfettiOverlay(trigger = state.prCelebrationText != null)
    }
}

@Composable
private fun DiscreetAction(text: String, iconRes: Int, contentColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IteraColors.SurfaceElevated)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(ImageVector.vectorResource(iconRes), contentDescription = null, tint = contentColor, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = contentColor
        )
    }
}

@Composable
private fun RoutinePickerDialog(routines: List<Routine>, routineLoaded: Boolean, onDismiss: () -> Unit, onPick: (Routine) -> Unit, onClear: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IteraColors.Surface,
        title = { Text("Cargar rutina", color = IteraColors.TextPrimary) },
        text = {
            Column {
                if (routineLoaded) {
                    // Vía de escape: soltar la rutina cargada y volver a sesión libre.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onClear)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sesión libre (sin rutina)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = LocalAccent.current.color)
                    }
                }
                routines.forEach { routine ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onPick(routine) }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(routine.name, style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text("${routine.exerciseIds.size} ej.", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Text("CANCELAR", style = MaterialTheme.typography.labelMedium, color = IteraColors.TextSecondary, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateExercisePrompt(query: String, muscleGroups: List<String>, onCreate: (String, String) -> Unit) {
    Column(Modifier.fillMaxWidth().border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp)).padding(12.dp)) {
        Text("Sin resultados. Crear \"$query\":", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(10.dp))
        Text("GRUPO MUSCULAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            muscleGroups.forEach { group ->
                Text(group, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = LocalAccent.current.color,
                    modifier = Modifier.border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp)).clickable { onCreate(query, group) }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun SaveRoutineDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = IteraColors.Surface,
        title = { Text("Guardar rutina", color = IteraColors.TextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Ej. Push Day", color = IteraColors.TextSecondary) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current.color,
                    unfocusedBorderColor = IteraColors.Border,
                    focusedTextColor = IteraColors.TextPrimary,
                    unfocusedTextColor = IteraColors.TextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            )
        },
        confirmButton = {
            Text(
                "GUARDAR",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (name.isBlank()) IteraColors.TextSecondary else LocalAccent.current.color,
                modifier = Modifier.clickable(enabled = name.isNotBlank()) { onSave(name) }.padding(8.dp)
            )
        },
        dismissButton = {
            Text("CANCELAR", style = MaterialTheme.typography.labelMedium, color = IteraColors.TextSecondary, modifier = Modifier.clickable { onDismiss() }.padding(8.dp))
        }
    )
}

@Composable
private fun ZeroWeightDialog(exerciseName: String?, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(IteraColors.Surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono ámbar de advertencia (aviso de atención, no de logro) sobre fondo tenue.
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(IteraColors.Achievement.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_warning),
                    contentDescription = null,
                    tint = IteraColors.Achievement,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "¿Registrar sin peso?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = IteraColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${exerciseName ?: "Este ejercicio"} suele llevar peso. Registrar a 0 kg contará como sin carga.",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            // Primario (enfatizado): añadir peso = volver al registro. Es la opción segura.
            Text(
                "Añadir peso",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = LocalAccent.current.onAccent,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalAccent.current.color)
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp)
            )
            Spacer(Modifier.height(10.dp))
            // Secundario (de-enfatizado): registrar a 0 kg de todos modos.
            Text(
                "Registrar a 0 kg de todos modos",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IteraColors.SurfaceElevated)
                    .clickable { onConfirm() }
                    .padding(vertical = 14.dp)
            )
        }
    }
}

@Composable
private fun RegisterSetButton(onRegisterSet: () -> Unit, prText: String? = null) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    var registered by remember { mutableStateOf(false) }
    LaunchedEffect(registered) { if (registered) { delay(900L); registered = false } }
    LaunchedEffect(prText) {
        if (prText != null) {
            repeat(3) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); delay(200) }
        }
    }
    val fireTransition = rememberInfiniteTransition(label = "fire")
    val fireScaleY by fireTransition.animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(280), RepeatMode.Reverse),
        label = "fire_scaleY"
    )
    val fireScaleX by fireTransition.animateFloat(
        initialValue = 1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(280), RepeatMode.Reverse),
        label = "fire_scaleX"
    )
    val container = when { prText != null -> LocalAccent.current.color; registered -> IteraColors.Surface; else -> LocalAccent.current.color }
    val content = when { prText != null -> LocalAccent.current.onAccent; registered -> LocalAccent.current.color; else -> LocalAccent.current.onAccent }
    Button(
        onClick = {
            focusManager.clearFocus()
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            registered = true
            onRegisterSet()
        },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (prText != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_fire), null,
                    tint = LocalAccent.current.onAccent,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = fireScaleX
                            scaleY = fireScaleY
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
                )
                Spacer(Modifier.width(6.dp))
                Text("NUEVO $prText", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Text(if (registered) "✓ REGISTRADO" else "REGISTRAR SET", style = MaterialTheme.typography.titleMedium)
        }
    }
}