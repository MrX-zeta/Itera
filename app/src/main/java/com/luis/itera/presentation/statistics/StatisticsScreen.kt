package com.luis.itera.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.TopMovementRecord
import com.luis.itera.presentation.components.StatBarChart
import com.luis.itera.presentation.components.StatLineChart
import com.luis.itera.presentation.components.WorkoutDensityChart
import com.luis.itera.presentation.theme.IteraColors

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectorExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        item {
            Text(
                "ESTADÍSTICAS",
                style = MaterialTheme.typography.titleMedium,
                color = IteraColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("${state.sessionsThisMonth}", "SESIONES/MES", Modifier.weight(1f))
                MetricCard(state.topFocus ?: "—", "FOCUS TOP", Modifier.weight(1f), accent = true)
                MetricCard("${state.streak.weeks}", "RACHA SEM", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            WeeklyGoalRow(state.streak.sessionsThisWeek, state.streak.weeklyGoal, viewModel::onWeeklyGoalDelta)
            Spacer(Modifier.height(14.dp))
        }
        item {
            Text("TOP MOVIMIENTOS", style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.topMovements.forEach { record ->
                    TopMovementCard(record, Modifier.weight(1f))
                }
                if (state.topMovements.isEmpty()) {
                    Text("Finaliza sesiones para ver tus top", style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        item {
            Text("PROGRESIÓN", style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                        .clickable { selectorExpanded = !selectorExpanded }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.selectedExercise?.name ?: "Selecciona ejercicio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (state.selectedExercise != null) IteraColors.TextPrimary else IteraColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        ImageVector.vectorResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = if (selectorExpanded) IteraColors.Accent else IteraColors.TextSecondary,
                        modifier = Modifier.size(18.dp).padding(start = 4.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                RangeChips(state.range, viewModel::onRangeSelected)
            }
            if (selectorExpanded) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    viewModel.muscleGroups.forEach { group ->
                        val active = state.selectedGroup == group
                        Text(
                            group,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (active) IteraColors.OnAccent else IteraColors.TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) IteraColors.Accent else IteraColors.Surface)
                                .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                                .clickable { viewModel.onGroupSelected(group) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        if (selectorExpanded) {
            items(state.exercises, key = { it.id }) { exercise ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.onExerciseSelected(exercise); selectorExpanded = false }
                        .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(exercise.mainMuscleGroup, style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary, modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        if (!selectorExpanded) {
            item {
                if (state.selectedExercise == null) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("Selecciona un ejercicio para ver la progresión", style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary, textAlign = TextAlign.Center)
                    }
                } else {
                    val isCardio = state.selectedExercise?.mainMuscleGroup.equals("Cardio", ignoreCase = true)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(
                            when { isCardio -> "DURACIÓN MÁXIMA"; state.isBodyweightMode -> "REPS MÁXIMAS"; else -> "PESO MÁXIMO" },
                            style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary
                        )
                        state.personalRecord?.let {
                            Text(
                                when { isCardio -> "MÁX ${it.toInt()} min"; state.isBodyweightMode -> "MÁX ${it.toInt()} reps"; else -> "MÁX ${formatKg(it)} kg" },
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp), color = IteraColors.Accent
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    if (state.maxWeightSeries.size >= 2) {
                        StatLineChart(points = state.maxWeightSeries)
                    } else {
                        Text(
                            if (state.maxWeightSeries.isEmpty()) "Sin datos en el rango" else "Necesita 2+ sesiones para graficar",
                            style = MaterialTheme.typography.bodySmall, color = IteraColors.TextSecondary
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text(
                            when { isCardio -> "MINUTOS TOTALES"; state.isBodyweightMode -> "REPS TOTALES"; else -> "VOLUMEN TOTAL" },
                            style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary
                        )
                        if (state.totalVolume > 0f) {
                            Text(
                                when {
                                    isCardio -> "Total: ${state.totalVolume.toInt()} min"
                                    state.isBodyweightMode -> "Total: ${state.totalVolume.toInt()} reps"
                                    state.totalVolume >= 1000f -> "Total: ${"%.1f".format(state.totalVolume / 1000f)} ton"
                                    else -> "Total: ${formatKg(state.totalVolume)} kg"
                                },
                                style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary.copy(alpha = 0.9f)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    StatBarChart(points = state.volumeSeries, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Spacer(Modifier.height(14.dp))
                Text("DENSIDAD DE ENTRENAMIENTO", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
                Spacer(Modifier.height(10.dp))
                WorkoutDensityChart(points = state.densityPoints, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MetricCard(value: String, label: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(
        modifier.border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp), color = if (accent) IteraColors.Accent else IteraColors.TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = IteraColors.TextSecondary)
    }
}

@Composable
private fun WeeklyGoalRow(sessionsThisWeek: Int, weeklyGoal: Int, onDelta: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Text("META SEMANAL · $sessionsThisWeek/$weeklyGoal", style = MaterialTheme.typography.bodyLarge, color = if (sessionsThisWeek >= weeklyGoal) IteraColors.Accent else IteraColors.TextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).clickable { onDelta(-1) }, contentAlignment = Alignment.Center) {
                Text("−", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = IteraColors.TextSecondary)
            }
            Text("$weeklyGoal", style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp), color = IteraColors.TextPrimary, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 32.dp))
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).clickable { onDelta(1) }, contentAlignment = Alignment.Center) {
                Text("+", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = IteraColors.Accent)
            }
        }
    }
}

@Composable
private fun RangeChips(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))) {
        StatsRange.entries.forEach { range ->
            val active = range == selected
            Text(range.label, style = MaterialTheme.typography.bodySmall, color = if (active) IteraColors.OnAccent else IteraColors.TextSecondary,
                modifier = Modifier.background(if (active) IteraColors.Accent else IteraColors.Background).clickable { onSelect(range) }.padding(horizontal = 14.dp, vertical = 12.dp))
        }
    }
}

@Composable
private fun TopMovementCard(record: TopMovementRecord, modifier: Modifier = Modifier) {
    Column(
        modifier.height(80.dp).border(1.dp, IteraColors.BorderStrong, RoundedCornerShape(10.dp)).padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Text(record.displayValue, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = IteraColors.Accent)
        Text(record.displayLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = IteraColors.TextSecondary)
        Spacer(Modifier.height(2.dp))
        Text(record.exerciseName.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = IteraColors.TextSecondary, textAlign = TextAlign.Center, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatKg(value: Float): String = if (value % 1f == 0f) "%,d".format(value.toInt()) else "%,.1f".format(value)