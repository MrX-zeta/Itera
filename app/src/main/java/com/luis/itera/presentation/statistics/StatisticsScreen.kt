package com.luis.itera.presentation.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.Exercise
import com.luis.itera.presentation.components.StatBarChart
import com.luis.itera.presentation.components.StatLineChart
import com.luis.itera.presentation.components.WorkoutDensityChart
import com.luis.itera.presentation.components.formatVolume
import com.luis.itera.presentation.components.volumeUnitFor
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(IteraColors.Background)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 20.dp, bottom = 16.dp)
        ) {
            Text(
                "ESTADÍSTICAS",
                style = MaterialTheme.typography.titleMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            RangeChips(state.range, viewModel::onRangeSelected)
        }

        HorizontalDivider(thickness = 1.dp, color = IteraColors.BorderStrong)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HeroVolumeCard(state) }
            item {
                RepartoPorFocoCard(
                    focusCounts = state.focusCounts,
                    sessionsInRange = state.sessionsInRange,
                    rangeLabel = rangeWindowLabel(state.range),
                    hasMultiFocus = state.hasMultiFocusSessions
                )
            }
            item {
                AnimatedVisibility(
                    visible = state.range != StatsRange.ALL,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                    exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                ) {
                    ProgressionCard(state, onOpenPicker = { showSheet = true })
                }
            }
        }
    }

    if (showSheet) {
        ExercisePickerSheet(
            exercises = state.exercises,
            selectedExercise = state.selectedExercise,
            muscleGroups = viewModel.muscleGroups,
            selectedGroup = state.selectedGroup,
            onGroupSelected = viewModel::onGroupSelected,
            onExerciseSelected = viewModel::onExerciseSelected,
            onDismiss = {
                showSheet = false
                state.selectedGroup?.let(viewModel::onGroupSelected)
            }
        )
    }
}

@Composable
private fun IteraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.Surface)
            .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(12.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Aún no hay datos", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = IteraColors.TextSecondary
    )
}

@Composable
private fun HeroVolumeCard(state: StatisticsUiState) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }

    IteraCard(
        Modifier.graphicsLayer {
            alpha = enter.value
            translationY = (1f - enter.value) * 16.dp.toPx()
        }
    ) {
        SectionTitle("VOLUMEN SEMANAL")
        Spacer(Modifier.height(2.dp))
        Text("ÚLTIMAS 5 SEMANAS", style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)

        val points = state.densityPoints
        if (points.isEmpty()) {
            EmptyState()
            return@IteraCard
        }

        val seriesMax = if (state.maxWeeklyVolume > 0f) state.maxWeeklyVolume else points.maxOf { it.volumeKg }
        val unit = volumeUnitFor(seriesMax)
        val heroText = formatVolume(points.first().volumeKg, unit)

        Spacer(Modifier.height(4.dp))
        Text(
            heroText,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFeatureSettings = "tnum"
            ),
            color = LocalAccent.current.color,
            maxLines = 1
        )

        if (state.volumeTrend != VolumeTrend.NONE) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                state.volumeTrend.iconRes?.let { icon ->
                    Icon(
                        ImageVector.vectorResource(icon),
                        contentDescription = null,
                        tint = LocalAccent.current.color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                state.volumeTrend.label?.let { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = IteraColors.TextPrimary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        WorkoutDensityChart(
            points = points,
            maxReference = state.maxWeeklyVolume,
            modifier = Modifier.fillMaxWidth()
        )

        val bestIsVisible = points.any { kotlin.math.abs(it.volumeKg - state.maxWeeklyVolume) < 0.5f }
        if (points.size >= 2 && state.maxWeeklyVolume > 0f && !bestIsVisible) {
            Text(
                "vs. tu mejor semana: ${formatVolume(state.maxWeeklyVolume, unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun RepartoPorFocoCard(
    focusCounts: List<FocusCount>,
    sessionsInRange: Int,
    rangeLabel: String,
    hasMultiFocus: Boolean
) {
    IteraCard {
        SectionTitle("REPARTO POR FOCO")
        if (sessionsInRange > 0) {
            Spacer(Modifier.height(4.dp))
            Text(
                "${sessionCountLabel(sessionsInRange)} · $rangeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
        }
        Spacer(Modifier.height(12.dp))
        val active = focusCounts.filter { it.count > 0 }
        val inactive = focusCounts.filter { it.count == 0 }
        if (active.isEmpty()) {
            Text(
                "Finaliza sesiones para ver tu reparto",
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                active.forEach { fc ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            fc.focus.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            sessionCountLabel(fc.count),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = IteraColors.TextPrimary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
            if (inactive.isNotEmpty()) {
                Text(
                    "Sin actividad: ${inactive.joinToString(", ") { it.focus.label }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            if (hasMultiFocus) {
                Text(
                    "Una sesión puede tener varios focos",
                    style = MaterialTheme.typography.labelSmall,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

private fun sessionCountLabel(count: Int): String =
    if (count == 1) "1 sesión" else "$count sesiones"

private fun rangeWindowLabel(range: StatsRange): String = when (range) {
    StatsRange.D30 -> "últimos 30 días"
    StatsRange.D90 -> "últimos 90 días"
    StatsRange.ALL -> "histórico completo"
}

@Composable
private fun ProgressionCard(state: StatisticsUiState, onOpenPicker: () -> Unit) {
    IteraCard {
        SectionTitle("PROGRESIÓN")
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(IteraColors.SurfaceElevated)
                .border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(8.dp))
                .clickable(onClick = onOpenPicker)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                state.selectedExercise?.name ?: "Elegir ejercicio",
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.selectedExercise != null) IteraColors.TextPrimary else IteraColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                ImageVector.vectorResource(R.drawable.ic_search),
                contentDescription = null,
                tint = IteraColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        val isCardio = state.selectedExercise?.mainMuscleGroup.equals("Cardio", ignoreCase = true)

        Spacer(Modifier.height(16.dp))
        ChartHeader(
            title = when {
                isCardio -> "DURACIÓN MÁXIMA"
                state.isBodyweightMode -> "REPS MÁXIMAS"
                else -> "PESO MÁXIMO"
            },
            value = state.personalRecord?.let {
                when {
                    isCardio -> "MÁX ${it.toInt()} min"
                    state.isBodyweightMode -> "MÁX ${it.toInt()} reps"
                    else -> "MÁX ${formatKg(it)} kg"
                }
            },
            valueColor = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(8.dp))
        if (state.maxWeightSeries.size >= 2) {
            StatLineChart(points = state.maxWeightSeries)
        } else {
            EmptyState()
        }

        Spacer(Modifier.height(16.dp))
        ChartHeader(
            title = when {
                isCardio -> "MINUTOS TOTALES"
                state.isBodyweightMode -> "REPS TOTALES"
                else -> "VOLUMEN (kg)"
            },
            value = state.totalVolume.takeIf { it > 0f }?.let {
                when {
                    isCardio -> "${it.toInt()} min"
                    state.isBodyweightMode -> "${it.toInt()} reps"
                    else -> formatVolume(it, volumeUnitFor(state.volumeSeries.maxOfOrNull { p -> p.value } ?: 0f))
                }
            }
        )
        Spacer(Modifier.height(8.dp))
        if (state.volumeSeries.isNotEmpty()) {
            StatBarChart(points = state.volumeSeries, modifier = Modifier.fillMaxWidth())
        } else {
            EmptyState()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    exercises: List<Exercise>,
    selectedExercise: Exercise?,
    muscleGroups: List<String>,
    selectedGroup: String?,
    onGroupSelected: (String) -> Unit,
    onExerciseSelected: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val filtered = remember(exercises, query) {
        val q = query.trim()
        if (q.isEmpty()) exercises else exercises.filter { it.name.contains(q, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = IteraColors.Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = IteraColors.BorderStrong) }
    ) {
        Column(Modifier.imePadding()) {
            Text(
                "ELEGIR EJERCICIO",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Buscar ejercicio") },
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalAccent.current.color,
                    unfocusedBorderColor = IteraColors.Border,
                    focusedTextColor = IteraColors.TextPrimary,
                    unfocusedTextColor = IteraColors.TextPrimary,
                    cursorColor = LocalAccent.current.color,
                    focusedContainerColor = IteraColors.Surface,
                    unfocusedContainerColor = IteraColors.Surface,
                    focusedPlaceholderColor = IteraColors.TextSecondary,
                    unfocusedPlaceholderColor = IteraColors.TextSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .focusRequester(focusRequester)
            )

            Spacer(Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(muscleGroups, key = { it }) { group ->
                    val active = selectedGroup == group
                    Text(
                        group,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (active) LocalAccent.current.onAccent else IteraColors.TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) LocalAccent.current.color else IteraColors.Surface)
                            .border(
                                1.dp,
                                if (active) LocalAccent.current.color else IteraColors.BorderStrong,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onGroupSelected(group) }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            if (filtered.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sin resultados", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
                }
            } else {
                LazyColumn(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { exercise ->
                        val isSelected = selectedExercise?.id == exercise.id
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    if (isSelected) LocalAccent.current.color else IteraColors.Border,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onExerciseSelected(exercise)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) LocalAccent.current.color else IteraColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                exercise.mainMuscleGroup,
                                style = MaterialTheme.typography.bodySmall,
                                color = IteraColors.TextSecondary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartHeader(title: String, value: String?, valueColor: Color = IteraColors.TextPrimary) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
        value?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                color = valueColor
            )
        }
    }
}

@Composable
private fun RangeChips(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
    ) {
        StatsRange.entries.forEach { range ->
            val active = range == selected
            Text(
                range.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (active) LocalAccent.current.onAccent else IteraColors.TextSecondary,
                modifier = Modifier
                    .background(if (active) LocalAccent.current.color else IteraColors.Background)
                    .clickable { onSelect(range) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

private fun formatKg(value: Float): String =
    if (value % 1f == 0f) "%,d".format(value.toInt()) else "%,.1f".format(value)
