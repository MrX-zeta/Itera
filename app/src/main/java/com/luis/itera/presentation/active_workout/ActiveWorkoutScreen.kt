package com.luis.itera.presentation.active_workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import com.luis.itera.domain.repository.SaveRoutineResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
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
import com.luis.itera.presentation.components.SessionTimer
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val homeDateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale("es"))

@Composable
fun ActiveWorkoutScreen(
    onSessionFinished: (Long) -> Unit,
    onLastSessionClick: (Long) -> Unit,
    onHydrationClick: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val finishedSessionId by viewModel.finishedSessionId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(finishedSessionId) {
        finishedSessionId?.let { viewModel.onFinishedSessionConsumed(); onSessionFinished(it) }
    }
    LaunchedEffect(Unit) {
        viewModel.routineFeedback.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (state.session == null) {
            HomeContent(
                state,
                viewModel::onFocusToggle,
                viewModel::onStartSession,
                onLastSessionClick,
                onHydrationClick,
                viewModel::onStartRoutine,
                viewModel::onWeeklyGoalChange
            )
        } else {
            ActiveSessionContent(
                state = state,
                muscleGroups = viewModel.muscleGroups,
                onCreateExercise = viewModel::onCreateExercise,
                onSearchChange = viewModel::onSearchQueryChange,
                onExerciseSelected = viewModel::onExerciseSelected,
                onRepsDelta = viewModel::onRepsDelta,
                onWeightDelta = viewModel::onWeightDelta,
                onRegisterSet = viewModel::onRegisterSet,
                onDeleteSet = viewModel::onDeleteSet,
                onStartTimer = viewModel::onStartTimer,
                onDiscardSession = viewModel::onDiscardSession,
                onFinishSession = viewModel::onFinishSession,
                onDurationDelta = viewModel::onDurationDelta,
                onIntensityDelta = viewModel::onIntensityDelta,
                onToggleTimerPause = viewModel::onToggleTimerPause,
                onSaveRoutine = viewModel::onSaveRoutine
            )
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
    onStartRoutine: (Routine) -> Unit,
    onWeeklyGoalChange: (Int) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(top = 32.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(LocalDate.now().format(homeDateFormatter).uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "META ${state.streak.sessionsThisWeek}/${state.streak.weeklyGoal}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = if (state.streak.sessionsThisWeek >= state.streak.weeklyGoal) LocalAccent.current.color else IteraColors.TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(6.dp))
                            .clickable { showGoalDialog = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                    if (state.streak.weeks > 0) {
                        Text(
                            "· RACHA ${state.streak.weeks} SEM",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (state.streak.sessionsThisWeek >= state.streak.weeklyGoal) LocalAccent.current.color else IteraColors.TextPrimary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                WeekActivityRow(state.trainedDaysThisWeek)
            }
            MiniHydrationRing(state.hydrationProgress, onHydrationClick)
        }
        Spacer(Modifier.height(24.dp))
        state.lastFinishedSession?.let { last ->
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text("ÚLTIMA SESIÓN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(IteraColors.SurfaceElevated).border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(12.dp)).clickable { onLastSessionClick(last.id) }.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(WorkoutFocus.fromStored(last.focus).takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.label } ?: "SESIÓN ${last.id}", style = MaterialTheme.typography.titleMedium, color = IteraColors.TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${relativeDay(last.dateEpochDay)} · ", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                            if (last.durationMinutes < 1) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_flash), null, tint = IteraColors.TextSecondary, modifier = Modifier.size(12.dp))
                                Text(" Rápida", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                            } else {
                                Text("${last.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                            }
                            Text(" · ${last.sets.size} sets", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                        }
                    }
                    Icon(ImageVector.vectorResource(R.drawable.ic_chevron_right), contentDescription = null, tint = IteraColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (state.routines.isNotEmpty()) {
            Text("MIS RUTINAS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.routines, key = { it.id }) { routine ->
                    Text(
                        routine.name,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = LocalAccent.current.color,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(IteraColors.Surface)
                            .border(1.dp, LocalAccent.current.color, RoundedCornerShape(8.dp))
                            .clickable { onStartRoutine(routine) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text("¿QUÉ TOCA HOY?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WorkoutFocus.entries.forEach { focus ->
                    val selected = focus in state.selectedFocuses
                    val blocked = focus in state.blockedFocuses
                    Text(
                        focus.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = when { selected -> LocalAccent.current.onAccent; blocked -> IteraColors.Border; else -> IteraColors.TextPrimary },
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) LocalAccent.current.color else IteraColors.Surface).border(1.dp, if (selected) LocalAccent.current.color else IteraColors.BorderStrong, RoundedCornerShape(8.dp)).clickable(enabled = !blocked) { onFocusToggle(focus) }.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart, enabled = state.selectedFocuses.isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LocalAccent.current.color, contentColor = LocalAccent.current.onAccent, disabledContainerColor = IteraColors.Border, disabledContentColor = IteraColors.TextSecondary),
            shape = RoundedCornerShape(8.dp)
        ) { Text("INICIAR ENTRENAMIENTO", style = MaterialTheme.typography.titleMedium) }
    }

    if (showGoalDialog) {
        WeeklyGoalDialog(
            current = state.streak.weeklyGoal,
            onDismiss = { showGoalDialog = false },
            onConfirm = { onWeeklyGoalChange(it); showGoalDialog = false }
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

@Composable
private fun WeekActivityRow(trainedDays: Set<Long>) {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val today = LocalDate.now()
    val labels = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (0..6).forEach { offset ->
            val day = monday.plusDays(offset.toLong())
            val trained = day.toEpochDay() in trainedDays
            val isToday = day == today
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(20.dp).then(if (isToday) Modifier.border(1.5.dp, LocalAccent.current.color, CircleShape) else Modifier).padding(if (isToday) 4.dp else 0.dp).clip(CircleShape)
                        .then(if (trained) Modifier.background(LocalAccent.current.color) else Modifier.background(IteraColors.SurfaceElevated).border(1.dp, IteraColors.BorderStrong, CircleShape))
                )
                Spacer(Modifier.height(4.dp))
                Text(labels[offset], style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = if (isToday) LocalAccent.current.color else IteraColors.TextSecondary)
            }
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
    onCreateExercise: (String, String) -> Unit,
    onSearchChange: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onRepsDelta: (Int) -> Unit,
    onWeightDelta: (Float) -> Unit,
    onRegisterSet: () -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit,
    onStartTimer: () -> Unit,
    onDiscardSession: () -> Unit,
    onFinishSession: () -> Unit,
    onDurationDelta: (Int) -> Unit,
    onIntensityDelta: (Int) -> Unit,
    onToggleTimerPause: () -> Unit,
    onSaveRoutine: (String) -> Unit
) {
    var searchExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                .padding(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(ImageVector.vectorResource(R.drawable.ic_back), null, tint = IteraColors.TextSecondary, modifier = Modifier.clickable(onClick = onDiscardSession).padding(end = 16.dp))
                    Column {
                        Text("ENTRENAMIENTO ACTIVO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                        if (state.sessionFocuses.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.sessionFocuses.joinToString(" · ") { it.label }, style = MaterialTheme.typography.bodySmall, color = LocalAccent.current.color)
                        }
                    }
                }
                Icon(ImageVector.vectorResource(R.drawable.ic_search), null, tint = if (searchExpanded) LocalAccent.current.color else IteraColors.TextSecondary,
                    modifier = Modifier.clickable { searchExpanded = !searchExpanded; if (!searchExpanded) onSearchChange("") }.padding(horizontal = 10.dp))
                if (state.setTimerMillis > 0L) {
                    SessionTimer(state.setTimerMillis, state.pausedElapsed, state.timerState, onToggleTimerPause)
                } else {
                    OutlinedButton(onClick = onStartTimer, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, IteraColors.Border), colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current.color)) {
                        Text("DESCANSO", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            AnimatedVisibility(visible = searchExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(state.searchQuery, onSearchChange, Modifier.fillMaxWidth().focusRequester(focusRequester), placeholder = { Text("Buscar ejercicio", color = IteraColors.TextSecondary) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LocalAccent.current.color, unfocusedBorderColor = IteraColors.Border, focusedTextColor = IteraColors.TextPrimary, unfocusedTextColor = IteraColors.TextPrimary), shape = RoundedCornerShape(8.dp))
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                }
            }
            state.selectedExercise?.let { exercise ->
                val isCardio = exercise.mainMuscleGroup.equals("Cardio", ignoreCase = true)
                Spacer(Modifier.height(12.dp))
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                if (!isCardio && state.lastSets.isNotEmpty()) {
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
                        style = MaterialTheme.typography.bodySmall,
                        color = IteraColors.TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    val suggestion = state.suggestion
                    if (suggestion != null) {
                        val isWeightUp = suggestion.contains("↑")
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sugerido: ${suggestion.replace(" ↑ peso", "")}", style = MaterialTheme.typography.bodySmall, color = LocalAccent.current.color.copy(alpha = 0.85f))
                            if (isWeightUp) {
                                Spacer(Modifier.width(4.dp))
                                Icon(ImageVector.vectorResource(R.drawable.ic_weight_up), null, tint = LocalAccent.current.color, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isCardio) {
                        FastStepper("MINUTOS", state.durationSeconds / 60f, { onDurationDelta((it * 60).toInt()) }, modifier = Modifier.weight(1f))
                        FastStepper("NIVEL", state.intensity.toFloat(), { onIntensityDelta(it.toInt()) }, modifier = Modifier.weight(1f))
                    } else {
                        FastStepper("REPS", state.reps.toFloat(), { onRepsDelta(it.toInt()) }, modifier = Modifier.weight(1f))
                        FastStepper("+KG", state.weightKg, onWeightDelta, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(12.dp))
                RegisterSetButton(onRegisterSet, state.prCelebrationText)
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(Modifier.weight(1f)) {
                item { Text("EJERCICIOS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary, modifier = Modifier.padding(bottom = 8.dp)) }
                items(state.exercises, key = { "ex_${it.id}" }) { exercise ->
                    val sel = exercise.id == state.selectedExercise?.id
                    Row(Modifier.fillMaxWidth().clickable { onExerciseSelected(exercise) }.border(1.dp, if (sel) LocalAccent.current.color else IteraColors.Border, RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyMedium, color = if (sel) LocalAccent.current.color else IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Text(exercise.mainMuscleGroup, style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (state.searchQuery.isNotBlank() && state.exercises.isEmpty()) {
                    item { CreateExercisePrompt(state.searchQuery, muscleGroups, onCreateExercise) }
                }
                val sets = state.session?.sets.orEmpty()
                if (sets.isNotEmpty()) {
                    item { Text("SETS DE HOY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)) }
                    items(sets, key = { "set_${it.id}" }) { set ->
                        Row(Modifier.fillMaxWidth().border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(state.exerciseNameOf(set.exerciseId), style = MaterialTheme.typography.bodyMedium)
                                    if (set.weightAddedKg > 0f || set.reps > 0) {
                                        val isPRSet = state.session?.sets?.filter { it.exerciseId == set.exerciseId }?.let { exSets ->
                                            if (set.weightAddedKg > 0f) set.weightAddedKg >= (exSets.maxOfOrNull { it.weightAddedKg } ?: 0f)
                                            else set.reps >= (exSets.maxOfOrNull { it.reps } ?: 0)
                                        } ?: false
                                        if (isPRSet && (state.session?.sets?.count { it.exerciseId == set.exerciseId } ?: 0) > 1) {
                                            Spacer(Modifier.width(4.dp))
                                            Box(Modifier.size(6.dp).background(LocalAccent.current.color, CircleShape))
                                        }
                                    }
                                }
                                Text(buildString {
                                    append("SET ${sets.filter { it.exerciseId == set.exerciseId }.indexOf(set) + 1} · ")
                                    if (set.durationSeconds > 0) { append("${set.durationSeconds / 60} min"); if (set.intensity > 0) append(" · nivel ${set.intensity}") }
                                    else { append("${set.reps} reps"); if (set.weightAddedKg > 0f) append(" +${set.weightAddedKg}kg") }
                                }, style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                            }
                            Icon(ImageVector.vectorResource(R.drawable.ic_trash), null, tint = IteraColors.TextSecondary, modifier = Modifier.clickable { onDeleteSet(set) }.padding(4.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val hasSets = state.session?.sets?.isNotEmpty() == true
            val alreadyRoutine = state.matchingRoutine != null
            if (hasSets && !alreadyRoutine) {
                var showDialog by remember { mutableStateOf(false) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IteraColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LocalAccent.current.color)
                    ) { Text("GUARDAR RUTINA", style = MaterialTheme.typography.labelSmall) }
                    OutlinedButton(
                        onClick = onFinishSession,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, IteraColors.Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = IteraColors.TextSecondary)
                    ) { Text("FINALIZAR", style = MaterialTheme.typography.labelSmall) }
                }
                if (showDialog) {
                    SaveRoutineDialog(
                        onDismiss = { showDialog = false },
                        onSave = { name -> onSaveRoutine(name); showDialog = false }
                    )
                }
            } else {
                OutlinedButton(onFinishSession, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, IteraColors.Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = IteraColors.TextSecondary)) {
                    Text("FINALIZAR SESIÓN", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        ConfettiOverlay(trigger = state.prCelebrationText != null)
    }
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