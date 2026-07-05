package com.luis.itera.presentation.active_workout

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.domain.model.WorkoutFocus
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.components.ConfettiOverlay
import com.luis.itera.presentation.components.FastStepper
import com.luis.itera.presentation.components.SessionTimer
import com.luis.itera.presentation.theme.IteraColors
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
    LaunchedEffect(finishedSessionId) {
        finishedSessionId?.let { viewModel.onFinishedSessionConsumed(); onSessionFinished(it) }
    }
    if (state.session == null) {
        HomeContent(state, viewModel::onFocusToggle, viewModel::onStartSession, onLastSessionClick, onHydrationClick)
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
            onToggleTimerPause = viewModel::onToggleTimerPause
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeContent(
    state: ActiveWorkoutUiState,
    onFocusToggle: (WorkoutFocus) -> Unit,
    onStart: () -> Unit,
    onLastSessionClick: (Long) -> Unit,
    onHydrationClick: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(top = 32.dp, bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(LocalDate.now().format(homeDateFormatter).uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "META ${state.streak.sessionsThisWeek}/${state.streak.weeklyGoal}" + if (state.streak.weeks > 0) " · RACHA ${state.streak.weeks} SEM" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (state.streak.sessionsThisWeek >= state.streak.weeklyGoal) IteraColors.Accent else IteraColors.TextPrimary
                )
                Spacer(Modifier.height(14.dp))
                WeekActivityRow(state.trainedDaysThisWeek)
            }
            MiniHydrationRing(state.hydrationProgress, onHydrationClick)
        }
        Spacer(Modifier.height(32.dp))
        state.lastFinishedSession?.let { last ->
            Text("ÚLTIMA SESIÓN", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(IteraColors.SurfaceElevated).border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(12.dp)).clickable { onLastSessionClick(last.id) }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(WorkoutFocus.fromStored(last.focus).takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.label } ?: "SESIÓN ${last.id}", style = MaterialTheme.typography.titleMedium, color = IteraColors.Accent)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${relativeDay(last.dateEpochDay)} · ", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                        if (last.durationMinutes < 1) {
                            Icon(ImageVector.vectorResource(R.drawable.ic_flash), null, tint = IteraColors.Accent, modifier = Modifier.size(12.dp))
                            Text(" Rápida", style = MaterialTheme.typography.bodySmall, color = IteraColors.Accent)
                        } else {
                            Text("${last.durationMinutes} min", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                        }
                        Text(" · ${last.sets.size} sets", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                    }
                }
                Text("›", style = MaterialTheme.typography.titleLarge, color = IteraColors.TextSecondary)
            }
        }
        Spacer(Modifier.weight(1f))
        Text("¿QUÉ TOCA HOY?", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkoutFocus.entries.forEach { focus ->
                val selected = focus in state.selectedFocuses
                val blocked = focus in state.blockedFocuses
                Text(
                    focus.label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = when { selected -> IteraColors.OnAccent; blocked -> IteraColors.Border; else -> IteraColors.TextPrimary },
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) IteraColors.Accent else IteraColors.Surface).border(1.dp, if (selected) IteraColors.Accent else IteraColors.BorderStrong, RoundedCornerShape(8.dp)).clickable(enabled = !blocked) { onFocusToggle(focus) }.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart, enabled = state.selectedFocuses.isNotEmpty(), modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = IteraColors.Accent, contentColor = IteraColors.OnAccent, disabledContainerColor = IteraColors.Border, disabledContentColor = IteraColors.TextSecondary),
            shape = RoundedCornerShape(8.dp)
        ) { Text("INICIAR ENTRENAMIENTO", style = MaterialTheme.typography.titleMedium) }
        Spacer(Modifier.height(8.dp))
    }
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
                    Modifier.size(20.dp).then(if (isToday) Modifier.border(1.5.dp, IteraColors.Accent, CircleShape) else Modifier).padding(if (isToday) 4.dp else 0.dp).clip(CircleShape)
                        .then(if (trained) Modifier.background(IteraColors.Accent) else Modifier.background(IteraColors.SurfaceElevated).border(1.dp, IteraColors.BorderStrong, CircleShape))
                )
                Spacer(Modifier.height(4.dp))
                Text(labels[offset], style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = if (isToday) IteraColors.Accent else IteraColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun MiniHydrationRing(progress: Float, onClick: () -> Unit) {
    Box(Modifier.size(52.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxSize().padding(4.dp), color = IteraColors.BorderStrong, strokeWidth = 3.dp)
        CircularProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxSize().padding(4.dp), color = IteraColors.Accent, strokeWidth = 3.dp)
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextPrimary)
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
    onToggleTimerPause: () -> Unit
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
                            Text(state.sessionFocuses.joinToString(" · ") { it.label }, style = MaterialTheme.typography.bodySmall, color = IteraColors.Accent)
                        }
                    }
                }
                Icon(ImageVector.vectorResource(R.drawable.ic_search), null, tint = if (searchExpanded) IteraColors.Accent else IteraColors.TextSecondary,
                    modifier = Modifier.clickable { searchExpanded = !searchExpanded; if (!searchExpanded) onSearchChange("") }.padding(horizontal = 10.dp))
                if (state.setTimerMillis > 0L) {
                    SessionTimer(state.setTimerMillis, state.pausedElapsed, state.timerState, onToggleTimerPause)
                } else {
                    OutlinedButton(onClick = onStartTimer, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, IteraColors.Border), colors = ButtonDefaults.outlinedButtonColors(contentColor = IteraColors.Accent)) {
                        Text("DESCANSO", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            AnimatedVisibility(visible = searchExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(state.searchQuery, onSearchChange, Modifier.fillMaxWidth().focusRequester(focusRequester), placeholder = { Text("Buscar ejercicio", color = IteraColors.TextSecondary) }, singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = IteraColors.Accent, unfocusedBorderColor = IteraColors.Border, focusedTextColor = IteraColors.TextPrimary, unfocusedTextColor = IteraColors.TextPrimary), shape = RoundedCornerShape(8.dp))
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
                            Text("Sugerido: ${suggestion.replace(" ↑ peso", "")}", style = MaterialTheme.typography.bodySmall, color = IteraColors.Accent.copy(alpha = 0.6f))
                            if (isWeightUp) {
                                Spacer(Modifier.width(4.dp))
                                Icon(ImageVector.vectorResource(R.drawable.ic_weight_up), null, tint = IteraColors.Accent, modifier = Modifier.size(14.dp))
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
                    Row(Modifier.fillMaxWidth().clickable { onExerciseSelected(exercise) }.border(1.dp, if (sel) IteraColors.Accent else IteraColors.Border, RoundedCornerShape(8.dp)).padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyMedium, color = if (sel) IteraColors.Accent else IteraColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
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
                                        if (isPRSet && state.session?.sets?.count { it.exerciseId == set.exerciseId } ?: 0 > 1) {
                                            Spacer(Modifier.width(4.dp))
                                            Box(Modifier.size(6.dp).background(IteraColors.Accent, CircleShape))
                                        }
                                    }
                                }
                                Text(buildString {
                                    append("SET ${set.order} · ")
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
            OutlinedButton(onFinishSession, Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, IteraColors.Border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IteraColors.TextSecondary)) {
                Text("FINALIZAR SESIÓN", style = MaterialTheme.typography.titleMedium)
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
                Text(group, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.Accent,
                    modifier = Modifier.border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp)).clickable { onCreate(query, group) }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
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
    val fireLift by fireTransition.animateFloat(
        initialValue = 0f, targetValue = -3f,
        animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
        label = "fire_lift"
    )

    val container = when { prText != null -> IteraColors.Accent; registered -> IteraColors.Surface; else -> IteraColors.Accent }
    val content = when { prText != null -> IteraColors.OnAccent; registered -> IteraColors.Accent; else -> IteraColors.OnAccent }

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
                    tint = IteraColors.OnAccent,
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer {
                            scaleX = fireScaleX
                            scaleY = fireScaleY
                            transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
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