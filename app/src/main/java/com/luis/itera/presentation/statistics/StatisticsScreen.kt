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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luis.itera.R
import com.luis.itera.domain.model.BigThreeRecord
import com.luis.itera.presentation.components.StatBarChart
import com.luis.itera.presentation.components.StatLineChart
import com.luis.itera.presentation.theme.IteraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val prDateFormatter = DateTimeFormatter.ofPattern("dd MMM yy", Locale("es"))

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectorExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "ESTADÍSTICAS",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.TextSecondary
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(
                    value = "${state.sessionsThisMonth}",
                    label = "SESIONES/MES",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    value = state.topFocus ?: "—",
                    label = "FOCUS TOP",
                    accent = true,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    value = "${state.streak.weeks}",
                    label = "RACHA SEM",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))
            WeeklyGoalRow(
                sessionsThisWeek = state.streak.sessionsThisWeek,
                weeklyGoal = state.streak.weeklyGoal,
                onDelta = viewModel::onWeeklyGoalDelta
            )
            Spacer(Modifier.height(22.dp))
        }

        item {
            Text(
                text = "RÉCORDS · BIG 3",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.TextSecondary
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.bigThree.forEach { record ->
                    BigThreeCard(record, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(22.dp))
        }

        item {
            Text(
                text = "PROGRESIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = IteraColors.TextSecondary
            )
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
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.selectedExercise?.name ?: "Selecciona ejercicio",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (state.selectedExercise != null) IteraColors.TextPrimary
                        else IteraColors.TextSecondary
                    )
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_search),
                        contentDescription = "Buscar ejercicio",
                        tint = if (selectorExpanded) IteraColors.Accent else IteraColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                RangeChips(
                    selected = state.range,
                    onSelect = viewModel::onRangeSelected
                )
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
                            text = group,
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
                        .clickable {
                            viewModel.onExerciseSelected(exercise)
                            selectorExpanded = false
                        }
                        .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(exercise.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        exercise.mainMuscleGroup,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        if (!selectorExpanded) {
            item {
                if (state.selectedExercise == null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Selecciona un ejercicio para ver la progresión",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Spacer(Modifier.height(18.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "PESO MÁXIMO",
                            style = MaterialTheme.typography.labelSmall,
                            color = IteraColors.TextSecondary
                        )
                        state.personalRecord?.let {
                            Text(
                                text = "PR ${formatKg(it)} kg",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                color = IteraColors.Accent
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    StatLineChart(points = state.maxWeightSeries)
                    if (state.maxWeightSeries.isEmpty()) {
                        Text(
                            text = "Sin datos en el rango",
                            style = MaterialTheme.typography.bodySmall,
                            color = IteraColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "VOLUMEN POR SESIÓN",
                            style = MaterialTheme.typography.labelSmall,
                            color = IteraColors.TextSecondary
                        )
                        if (state.totalVolume > 0f) {
                            Text(
                                text = "Σ ${formatKg(state.totalVolume)} kg",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
                                color = IteraColors.TextPrimary
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    StatBarChart(points = state.volumeSeries)
                    if (state.volumeSeries.isEmpty()) {
                        Text(
                            text = "Sin datos en el rango",
                            style = MaterialTheme.typography.bodySmall,
                            color = IteraColors.TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Column(
        modifier
            .border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
            color = if (accent) IteraColors.Accent else IteraColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
    }
}

@Composable
private fun WeeklyGoalRow(
    sessionsThisWeek: Int,
    weeklyGoal: Int,
    onDelta: (Int) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "META SEMANAL · $sessionsThisWeek/$weeklyGoal",
            style = MaterialTheme.typography.bodyLarge,
            color = if (sessionsThisWeek >= weeklyGoal) IteraColors.Accent
            else IteraColors.TextPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelta(-1) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
                    color = IteraColors.TextSecondary
                )
            }
            Text(
                text = "$weeklyGoal",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                color = IteraColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 32.dp)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDelta(1) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 30.sp),
                    color = IteraColors.Accent
                )
            }
        }
    }
}

@Composable
private fun BigThreeCard(record: BigThreeRecord, modifier: Modifier = Modifier) {
    Column(
        modifier
            .border(1.dp, IteraColors.Border, RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = record.maxWeightKg?.let { "${formatKg(it)} kg" } ?: "—",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = if (record.maxWeightKg != null) IteraColors.Accent
            else IteraColors.TextSecondary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = shortName(record.exerciseName),
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        record.dateEpochDay?.let {
            Text(
                text = LocalDate.ofEpochDay(it).format(prDateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = IteraColors.TextSecondary
            )
        }
    }
}

@Composable
private fun RangeChips(
    selected: StatsRange,
    onSelect: (StatsRange) -> Unit
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
    ) {
        StatsRange.entries.forEach { range ->
            val active = range == selected
            Text(
                text = range.label,
                style = MaterialTheme.typography.bodySmall,
                color = if (active) IteraColors.OnAccent else IteraColors.TextSecondary,
                modifier = Modifier
                    .background(if (active) IteraColors.Accent else IteraColors.Background)
                    .clickable { onSelect(range) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

private fun formatKg(value: Float): String =
    if (value % 1f == 0f) "%,d".format(value.toInt()) else "%,.1f".format(value)

private fun shortName(name: String): String = when {
    name.startsWith("Press banca") -> "PRESS BANCA"
    name.startsWith("Peso muerto") -> "PESO MUERTO"
    name.startsWith("Sentadilla") -> "SENTADILLA"
    else -> name.uppercase()
}