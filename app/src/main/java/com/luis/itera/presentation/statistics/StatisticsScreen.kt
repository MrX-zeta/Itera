package com.luis.itera.presentation.statistics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import com.luis.itera.domain.model.TrainingModality
import com.luis.itera.presentation.components.StatLineChart
import com.luis.itera.presentation.components.rememberReduceMotion
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import java.util.Locale

/** Pestaña de Estadísticas: General (fija) o una modalidad activa (dinámica). */
private sealed interface StatsTab {
    data object General : StatsTab
    data class Modality(val modality: TrainingModality) : StatsTab
}

/** Orden de pestaña estable para decidir la dirección del deslizamiento. */
private fun StatsTab.order(): Int = when (this) {
    StatsTab.General -> 0
    is StatsTab.Modality -> 1 + modality.ordinal
}

private fun TrainingModality.tabLabel(): String = when (this) {
    TrainingModality.STRENGTH -> "Fuerza"
    TrainingModality.CALISTHENICS -> "Calistenia"
    TrainingModality.CARDIO -> "Cardio"
}

@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val general by viewModel.generalState.collectAsStateWithLifecycle()
    val strength by viewModel.strengthState.collectAsStateWithLifecycle()
    val modalityStats by viewModel.modalityStatsState.collectAsStateWithLifecycle()
    val activeModalities by viewModel.activeModalities.collectAsStateWithLifecycle()
    var showSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf<StatsTab>(StatsTab.General) }

    val tabs = remember(activeModalities) {
        listOf(StatsTab.General) + activeModalities.map { StatsTab.Modality(it.modality) }
    }
    // Si la modalidad seleccionada deja de estar activa (archivada del foco), vuelve a General.
    LaunchedEffect(tabs) {
        if (selectedTab !in tabs) selectedTab = StatsTab.General
    }
    // Informa al ViewModel qué modalidad está activa (para el ejercicio destacado y sus métricas).
    LaunchedEffect(selectedTab) {
        viewModel.onModalitySelected((selectedTab as? StatsTab.Modality)?.modality)
    }

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
            StatsTabBar(tabs, selectedTab, onSelect = { selectedTab = it })
        }

        HorizontalDivider(thickness = 1.dp, color = IteraColors.BorderStrong)

        val reduceMotion = rememberReduceMotion()
        AnimatedContent(
            targetState = selectedTab,
            label = "stats_tab",
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (reduceMotion) {
                    fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                } else {
                    val forward = targetState.order() > initialState.order()
                    val towards = if (forward) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    (slideIntoContainer(towards, tween(250, easing = FastOutSlowInEasing)) + fadeIn(tween(200)))
                        .togetherWith(slideOutOfContainer(towards, tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(150)))
                }
            }
        ) { tab ->
            when (tab) {
                StatsTab.General -> GeneralTab(state, general, viewModel, onOpenPicker = { showSheet = true })
                is StatsTab.Modality -> when (tab.modality) {
                    TrainingModality.STRENGTH -> FuerzaTab(state, strength, onOpenPicker = { showSheet = true })
                    TrainingModality.CALISTHENICS -> CalisteniaTab(state, modalityStats, onOpenPicker = { showSheet = true })
                    TrainingModality.CARDIO -> CardioTab(state, modalityStats, onOpenPicker = { showSheet = true })
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

/** Barra de filtro: "General" fija + una pestaña por modalidad activa. Estilo segmentado. */
@Composable
private fun StatsTabBar(tabs: List<StatsTab>, selected: StatsTab, onSelect: (StatsTab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(IteraColors.SurfaceElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == selected
            val label = when (tab) {
                StatsTab.General -> "General"
                is StatsTab.Modality -> tab.modality.tabLabel()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isSelected) LocalAccent.current.onAccent else IteraColors.TextSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) LocalAccent.current.color else IteraColors.SurfaceElevated)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 8.dp)
            )
        }
    }
}

/** Vista GENERAL (fija): "¿progresé?", constancia y equilibrio. Reparto por foco se conserva
 *  para decidir en la Parada 2 si convive con Equilibrio o se retira. */
@Composable
private fun GeneralTab(
    state: StatisticsUiState,
    general: GeneralUiState,
    viewModel: StatisticsViewModel,
    onOpenPicker: () -> Unit
) {
    if (!general.hasAnyData) {
        GeneralEmptyState()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProgresoCard(general) }
        item { ConstanciaCard(general) }
        item { EquilibrioCard(general.neglected) }
        item { RangeChips(state.range, viewModel::onRangeSelected) }
        item {
            RepartoPorFocoCard(
                focusCounts = state.focusCounts,
                sessionsInRange = state.sessionsInRange,
                rangeLabel = rangeWindowLabel(state.range),
                hasMultiFocus = state.hasMultiFocusSessions
            )
        }
    }
}

/** "¿Progresé?": veredicto humano + evidencia concreta derivada de tendencia/constancia. */
@Composable
private fun ProgresoCard(general: GeneralUiState) {
    IteraCard {
        SectionTitle("¿PROGRESÉ?")
        Spacer(Modifier.height(8.dp))
        Text(
            general.headline,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = IteraColors.TextPrimary
        )
        general.evidence?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium, color = IteraColors.TextSecondary)
        }
    }
}

/**
 * Constancia de LARGO PLAZO: racha + promedio de días/semana + total histórico. El vistazo
 * de "esta semana" (barra de 7 segmentos) vive en Home; aquí respondemos "¿soy constante en
 * el tiempo?", no "¿cómo voy esta semana?".
 */
@Composable
private fun ConstanciaCard(general: GeneralUiState) {
    IteraCard {
        SectionTitle("CONSTANCIA")
        Spacer(Modifier.height(8.dp))
        val weeks = general.streak.weeks
        Text(
            if (weeks > 0) "Racha de $weeks ${if (weeks == 1) "semana" else "semanas"}" else "Aún sin racha",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = if (weeks > 0) IteraColors.TextPrimary else IteraColors.TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        val avg = String.format(Locale("es"), "%.1f", general.avgDaysPerWeek)
        Text(
            "Promedio $avg días/semana · ${general.totalTrainedDays} días entrenados",
            style = MaterialTheme.typography.bodyMedium,
            color = IteraColors.TextSecondary
        )
    }
}

/** "Esto descuidas": grupos musculares sin entrenar hace tiempo, en ÁMBAR (señal de atención). */
@Composable
private fun EquilibrioCard(neglected: List<MuscleGroupNeglect>) {
    IteraCard {
        SectionTitle("ESTO DESCUIDAS")
        Spacer(Modifier.height(12.dp))
        if (neglected.isEmpty()) {
            Text(
                "Buen equilibrio: todos los grupos al día",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                neglected.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.group,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextPrimary
                        )
                        Text(
                            "${item.daysSince} días sin entrenar",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = IteraColors.Achievement
                        )
                    }
                }
            }
        }
    }
}

/** Estado vacío de usuario nuevo: invita a registrar, nunca una pantalla muerta. */
@Composable
private fun GeneralEmptyState() {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Aún no hay nada que medir",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = IteraColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Registra tu primer entrenamiento y aquí verás si vas progresando.",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Vista FUERZA: progresión por ejercicio + fuerza estimada (1RM) + top de movimientos. */
@Composable
private fun FuerzaTab(state: StatisticsUiState, strength: StrengthUiState, onOpenPicker: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProgressionCard(state, onOpenPicker = onOpenPicker) }
        item { FuerzaEstimadaCard(state.selectedExercise?.name, strength.estimate) }
        if (strength.topMovements.isNotEmpty()) {
            item { TopMovementsCard(strength.topMovements) }
        }
    }
}

/** "Fuerza estimada +X%" (1RM traducido a %). 1RM ABSOLUTO solo en los 3 básicos. Compacta:
 *  la cifra y la frase comparten línea, para que sea un dato, no una card medio vacía. */
@Composable
private fun FuerzaEstimadaCard(exerciseName: String?, estimate: StrengthEstimate?) {
    IteraCard {
        SectionTitle("FUERZA ESTIMADA")
        Spacer(Modifier.height(6.dp))

        val change = estimate?.changePercent
        if (change != null) {
            val pct = change.toInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (pct >= 0) "+$pct%" else "$pct%",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LocalAccent.current.color
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (pct >= 0) "más fuerte que antes" else "por debajo de tu mejor momento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IteraColors.TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                "Aún no hay suficiente para estimar tu fuerza aquí",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        }

        // 1RM absoluto SOLO en los 3 básicos, cuando hay estimación fiable. En una sola línea.
        if (estimate?.isBasic == true && estimate.absoluteOneRmKg != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "1RM estimado ~${estimate.absoluteOneRmKg.toInt()} kg" + (exerciseName?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = IteraColors.TextPrimary
            )
        }
    }
}

/** Top de movimientos con tendencia (↑ mejorando / → estable), reusando el 1RM% de 6A. */
@Composable
private fun TopMovementsCard(movements: List<TopMovement>) {
    IteraCard {
        SectionTitle("TOP MOVIMIENTOS")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            movements.forEach { m ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        m.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (iconRes, label) = when (m.trend) {
                            MovementTrend.RISING -> R.drawable.ic_trend_up to "mejorando"
                            MovementTrend.STABLE -> R.drawable.ic_trend_flat to "estable"
                            MovementTrend.FALLING -> R.drawable.ic_trend_down to "bajando"
                        }
                        Icon(
                            ImageVector.vectorResource(iconRes),
                            contentDescription = null,
                            tint = if (m.trend == MovementTrend.RISING) LocalAccent.current.color else IteraColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = IteraColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/** Resumen de progresión: el texto "+X% en N sem" y si el cambio fue positivo (para el color). */
private data class ProgressSummary(val text: String, val positive: Boolean)

/**
 * Titular humano de progresión, según la unidad de la modalidad:
 * peso → "+X% en N sem" · reps → "8 → 12 reps" · min → "+10 min".
 */
private fun progressionSummary(
    series: List<com.luis.itera.domain.model.ExerciseSeriesPoint>,
    unit: String
): ProgressSummary? {
    if (series.size < 2) return null
    val first = series.first().value
    val last = series.last().value
    return when (unit) {
        "reps" -> ProgressSummary("${first.toInt()} → ${last.toInt()} reps", positive = last >= first)
        "min" -> {
            val delta = (last - first).toInt()
            ProgressSummary(if (delta >= 0) "+$delta min" else "$delta min", positive = delta >= 0)
        }
        else -> {
            if (first <= 0f) return null
            val pct = ((last - first) / first * 100).toInt()
            val weeks = ((series.last().dateEpochDay - series.first().dateEpochDay) / 7).toInt().coerceAtLeast(1)
            val sign = if (pct >= 0) "+$pct%" else "$pct%"
            ProgressSummary("$sign en $weeks sem", positive = pct >= 0)
        }
    }
}

/** Vista CALISTENIA: progresión en reps (ejercicio destacado) + top de movimientos (reps). */
@Composable
private fun CalisteniaTab(state: StatisticsUiState, stats: ModalityStatsUiState, onOpenPicker: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProgressionCard(state, onOpenPicker = onOpenPicker) }
        if (stats.topRepsMovements.isNotEmpty()) {
            item { TopMovementsCard(stats.topRepsMovements) }
        }
    }
}

/** Vista CARDIO: progresión en duración + minutos del mes + intensidad media + resumen humano. */
@Composable
private fun CardioTab(state: StatisticsUiState, stats: ModalityStatsUiState, onOpenPicker: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ProgressionCard(state, onOpenPicker = onOpenPicker) }
        item { MinutosMesCard(stats.minutesThisMonth) }
        item { IntensidadCard(stats.intensityAvg, stats.intensityAvgLastMonth) }
        stats.cardioSummary?.let { summary ->
            item { ResumenCard(summary) }
        }
    }
}

/** Minutos totales de cardio del mes. */
@Composable
private fun MinutosMesCard(minutes: Int) {
    IteraCard {
        SectionTitle("MINUTOS ESTE MES")
        Spacer(Modifier.height(6.dp))
        Text(
            "$minutes",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
            color = IteraColors.TextPrimary
        )
    }
}

/** Intensidad media del mes, con flecha vs el mes pasado ("7.2 ↑ de 6.5"). */
@Composable
private fun IntensidadCard(avg: Float, avgLast: Float) {
    IteraCard {
        SectionTitle("INTENSIDAD MEDIA")
        Spacer(Modifier.height(6.dp))
        if (avg <= 0f) {
            Text(
                "Sin datos de intensidad este mes",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format(Locale("es"), "%.1f", avg),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = IteraColors.TextPrimary
                )
                if (avgLast > 0f) {
                    val up = avg >= avgLast
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        ImageVector.vectorResource(if (up) R.drawable.ic_trend_up else R.drawable.ic_trend_down),
                        contentDescription = null,
                        tint = if (up) LocalAccent.current.color else IteraColors.TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "de ${String.format(Locale("es"), "%.1f", avgLast)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IteraColors.TextSecondary
                    )
                }
            }
        }
    }
}

/** Resumen humano de cardio ("Corres más tiempo y más fuerte que el mes pasado"). */
@Composable
private fun ResumenCard(summary: String) {
    IteraCard {
        SectionTitle("RESUMEN")
        Spacer(Modifier.height(6.dp))
        Text(
            summary,
            style = MaterialTheme.typography.bodyLarge,
            color = IteraColors.TextPrimary
        )
    }
}

@Composable
private fun IteraCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.SurfaceElevated)
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

/**
 * Card de progresión por ejercicio, fiel a la maqueta de Fuerza: nombre (tocar para cambiar)
 * + pill "+X% en N sem" + UNA gráfica lineal con etiquetas de extremos. Sin gráfica de volumen
 * (el tonelaje es cifra cruda que satura y no responde "¿progresé?").
 */
@Composable
private fun ProgressionCard(state: StatisticsUiState, onOpenPicker: () -> Unit) {
    val isCardio = state.selectedExercise?.mainMuscleGroup.equals("Cardio", ignoreCase = true)
    val unit = when {
        isCardio -> "min"
        state.isBodyweightMode -> "reps"
        else -> "kg"
    }

    IteraCard {
        // Fila superior: nombre del ejercicio (tocar para elegir otro) + pill de progreso.
        // El área táctil (y su ripple) envuelve SOLO el nombre + la lupa, no llega al pill.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenPicker)
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    state.selectedExercise?.name ?: "Elegir ejercicio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (state.selectedExercise != null) IteraColors.TextPrimary else IteraColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    ImageVector.vectorResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = IteraColors.TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
            progressionSummary(state.maxWeightSeries, unit)?.let { ProgressPill(it) }
        }

        Spacer(Modifier.height(16.dp))
        if (state.maxWeightSeries.size >= 2) {
            StatLineChart(points = state.maxWeightSeries)
            Spacer(Modifier.height(8.dp))
            // Etiquetas de extremos: valor inicial (izq) → valor actual (der), como en la maqueta.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${state.maxWeightSeries.first().value.toInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
                Text(
                    "${state.maxWeightSeries.last().value.toInt()} $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = IteraColors.TextSecondary
                )
            }
        } else {
            EmptyState()
        }
    }
}

/** Pill de progreso ("+25% en 6 sem"): acento si sube, neutro si baja (nunca verde para una caída). */
@Composable
private fun ProgressPill(summary: ProgressSummary) {
    val color = if (summary.positive) LocalAccent.current.color else IteraColors.TextSecondary
    Text(
        summary.text,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
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
                        color = if (active) LocalAccent.current.onAccent else IteraColors.TextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) LocalAccent.current.color else IteraColors.SurfaceElevated)
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
                                .background(
                                    if (isSelected) LocalAccent.current.color.copy(alpha = 0.15f)
                                    else IteraColors.SurfaceElevated
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
private fun RangeChips(selected: StatsRange, onSelect: (StatsRange) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(IteraColors.SurfaceElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        StatsRange.entries.forEach { range ->
            val active = range == selected
            Text(
                range.label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (active) LocalAccent.current.onAccent else IteraColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (active) LocalAccent.current.color else IteraColors.SurfaceElevated)
                    .clickable { onSelect(range) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
