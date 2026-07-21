package com.luis.itera.presentation.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ---------------------------------------------------------------------------
// Parámetros
// ---------------------------------------------------------------------------

/** Nº de columnas (semanas) mostradas. Filas = días de la semana (7). */
const val HEATMAP_COLUMNS = 10
private const val HEATMAP_ROWS = 7

// Animación de entrada (barrido por columnas, izquierda -> derecha), en ms.
private const val SWEEP_COLUMN_DELAY_MS = 34f
private const val SWEEP_CELL_MS = 460f
private const val SWEEP_TOTAL_MS = (HEATMAP_COLUMNS - 1) * SWEEP_COLUMN_DELAY_MS + SWEEP_CELL_MS

private val esLocale = Locale("es")
private val heatmapDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", esLocale)

// Un paso por encima de SurfaceElevated (#1E2229): celda "sin entrenar" perceptible,
// no un hueco casi invisible sobre el fondo.
private val EMPTY_CELL_COLOR = Color(0xFF262A31)

// ---------------------------------------------------------------------------
// Modelo + funciones puras
// ---------------------------------------------------------------------------

/** Constancia BINARIA: entrenó ese día o no. Nada de niveles por volumen/grupos/sesiones. */
data class HeatmapCell(val date: LocalDate, val trained: Boolean)

/** Lunes de inicio de la ventana rodante: 10 semanas terminando en la semana actual. */
fun heatmapStartMonday(today: LocalDate = LocalDate.now()): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks((HEATMAP_COLUMNS - 1).toLong())

/**
 * Genera 70 celdas ORDENADAS POR DÍA-MAYOR (fila = día de la semana, columna = semana).
 * Con LazyVerticalGrid(GridCells.Fixed(COLUMNS)) en row-major, cada fila queda como un
 * día de la semana a lo largo de las 10 columnas; el índice de columna es `index % COLUMNS`.
 */
fun buildHeatmapCells(data: Set<LocalDate>, startMonday: LocalDate): List<HeatmapCell> {
    val cells = ArrayList<HeatmapCell>(HEATMAP_ROWS * HEATMAP_COLUMNS)
    for (weekday in 0 until HEATMAP_ROWS) {
        for (week in 0 until HEATMAP_COLUMNS) {
            val date = startMonday.plusWeeks(week.toLong()).plusDays(weekday.toLong())
            cells.add(HeatmapCell(date, trained = date in data))
        }
    }
    return cells
}

/** Etiquetas de día empezando en LUNES, abreviatura de 2 letras localizada (Lu Ma Mi Ju Vi Sá Do). */
fun buildDayLabels(locale: Locale = esLocale): List<String> =
    (0 until HEATMAP_ROWS).map { i ->
        DayOfWeek.MONDAY.plus(i.toLong())
            .getDisplayName(TextStyle.SHORT, locale)
            .take(2)
            .replaceFirstChar { it.uppercase() }
    }

/** Etiqueta de mes corto SOLO cuando cambia de mes; el resto en blanco. */
fun buildMonthLabels(startMonday: LocalDate, locale: Locale = esLocale): List<String> {
    var lastMonth = -1
    return (0 until HEATMAP_COLUMNS).map { week ->
        val d = startMonday.plusWeeks(week.toLong())
        if (d.monthValue != lastMonth) {
            lastMonth = d.monthValue
            d.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
        } else ""
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

/**
 * Tarjeta con el mapa de actividad de las últimas 10 semanas.
 *
 * @param data eventos completados por día (días ausentes = 0). No accede a BD ni red.
 * @param selectedDate fecha seleccionada (estado elevado); null = sin selección.
 * @param onDateSelected callback al tocar una celda; pasa null al deseleccionar.
 */
@Composable
fun ActivityHeatmapCard(
    data: Set<LocalDate>,
    prDays: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.Surface)
            .padding(16.dp)
    ) {
        ActivityHeatmap(data = data, prDays = prDays, selectedDate = selectedDate, onDateSelected = onDateSelected)
    }
}

@Composable
fun ActivityHeatmap(
    data: Set<LocalDate>,
    prDays: Set<LocalDate>,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val startMonday = remember(today) { heatmapStartMonday(today) }
    val cells = remember(data, startMonday) { buildHeatmapCells(data, startMonday) }
    val dayLabels = remember { buildDayLabels() }
    val monthLabels = remember(startMonday) { buildMonthLabels(startMonday) }
    val allEmpty = remember(cells) { cells.none { it.trained } }

    // La FECHA seleccionada es estado elevado (ViewModel); la celda se deriva de los datos
    // actuales para que el conteo nunca quede obsoleto tras borrar/añadir sesiones.
    val selectedCell = selectedDate?.let { d -> cells.firstOrNull { it.date == d } }

    Column(modifier.fillMaxWidth()) {
        // Cabecera: solo el título (la fecha vive en la línea secundaria).
        Text(
            text = "MAPA DE ACTIVIDAD",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        // Estado vacío: solo título + mensaje, sin cuadrícula ni línea de selección.
        if (allEmpty) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Aún no hay actividad",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = IteraColors.TextSecondaryStrong
            )
            return@Column
        }

        // Línea secundaria: en blanco por defecto; fecha (+ "Entrenaste" si aplica) si hay selección.
        Text(
            text = selectedCell?.let {
                // "Hoy" evita repetir la fecha; la de otros días sí se muestra completa.
                val datePart = if (it.date == today) "Hoy" else it.date.format(heatmapDateFormatter)
                if (it.trained) "$datePart · Entrenaste" else datePart
            } ?: " ",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = IteraColors.TextSecondaryStrong,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Barrido de entrada; reduce-motion => aparece ya en su estado final.
        val reduceMotion = rememberReduceMotion()
        val sweep = remember { Animatable(0f) }
        LaunchedEffect(cells, reduceMotion) {
            if (reduceMotion) {
                sweep.snapTo(SWEEP_TOTAL_MS)
            } else {
                sweep.snapTo(0f)
                sweep.animateTo(
                    targetValue = SWEEP_TOTAL_MS,
                    animationSpec = tween(SWEEP_TOTAL_MS.toInt(), easing = LinearEasing)
                )
            }
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 4.dp
            val labelWidth = 22.dp
            val gridWidth = maxWidth - labelWidth - 8.dp
            val cell = (gridWidth - gap * (HEATMAP_COLUMNS - 1)) / HEATMAP_COLUMNS

            Column {
                Row {
                    // Etiquetas de día (una por fila), cajas de alto = cell.
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        dayLabels.forEach { label ->
                            Box(
                                modifier = Modifier
                                    .width(labelWidth)
                                    .height(cell),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IteraColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(HEATMAP_COLUMNS),
                        userScrollEnabled = false,
                        horizontalArrangement = Arrangement.spacedBy(gap),
                        verticalArrangement = Arrangement.spacedBy(gap),
                        modifier = Modifier
                            .width(gridWidth)
                            .height(cell * HEATMAP_ROWS + gap * (HEATMAP_ROWS - 1))
                    ) {
                        itemsIndexed(cells) { index, cellData ->
                            HeatmapCellBox(
                                cell = cellData,
                                index = index,
                                cellSize = cell,
                                today = today,
                                isSelected = selectedDate == cellData.date,
                                hasPr = cellData.date in prDays,
                                sweep = sweep,
                                onClick = {
                                    onDateSelected(if (selectedDate == cellData.date) null else cellData.date)
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Etiquetas de mes, alineadas bajo cada columna del grid.
                Row(Modifier.padding(start = labelWidth + 8.dp)) {
                    monthLabels.forEachIndexed { i, label ->
                        Box(
                            modifier = Modifier
                                .width(cell)
                                .height(14.dp)
                        ) {
                            if (label.isNotEmpty()) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IteraColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        if (i < monthLabels.lastIndex) Spacer(Modifier.width(gap))
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        HeatmapLegend()
    }
}

@Composable
private fun HeatmapLegend() {
    val accent = LocalAccent.current.color
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(accent.copy(alpha = 0.7f)))
        Spacer(Modifier.width(8.dp))
        Text("entrenaste", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.size(13.dp).clip(RoundedCornerShape(3.dp)).background(accent)) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(IteraColors.Achievement)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("récord (PR)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = IteraColors.TextSecondary)
    }
}

@Composable
private fun HeatmapCellBox(
    cell: HeatmapCell,
    index: Int,
    cellSize: Dp,
    today: LocalDate,
    isSelected: Boolean,
    hasPr: Boolean,
    sweep: Animatable<Float, AnimationVector1D>,
    onClick: () -> Unit
) {
    val accent = LocalAccent.current
    // 3 estados, siguen siendo HECHOS binarios (no intensidad): sin entrenar (gris
    // perceptible) / entrenado (verde base, algo más suave) / con PR (verde pleno +
    // punto ámbar). El salto entrenado→PR es moderado: realce de récord, no mapa de calor.
    val background = when {
        cell.trained && hasPr -> accent.color
        cell.trained -> accent.color.copy(alpha = 0.7f)
        else -> EMPTY_CELL_COLOR
    }
    val isToday = cell.date == today
    // Si ya está en acento (entrenado), el borde con el acento se perdería, usa onAccent.
    val todayBorderColor = if (cell.trained) accent.onAccent else accent.color

    Box(
        modifier = Modifier
            .size(cellSize)
            .graphicsLayer {
                // Leído en fase de dibujo: no recompone las 70 celdas.
                val column = index % HEATMAP_COLUMNS
                val local = ((sweep.value - column * SWEEP_COLUMN_DELAY_MS) / SWEEP_CELL_MS)
                    .coerceIn(0f, 1f)
                val eased = FastOutSlowInEasing.transform(local)
                alpha = eased
                scaleX = 0.8f + 0.2f * eased
                scaleY = 0.8f + 0.2f * eased
            }
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .then(
                when {
                    isSelected -> Modifier.border(1.5.dp, IteraColors.TextPrimary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    isToday -> Modifier.border(2.dp, todayBorderColor, RoundedCornerShape(4.dp))
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        // Logro: OTRO color (ámbar), no otra intensidad. Nunca sustituye el verde de constancia.
        if (hasPr) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size((cellSize.value * 0.32f).dp)
                    .clip(CircleShape)
                    .background(IteraColors.Achievement)
            )
        }
    }
}

/** true si el sistema tiene las animaciones desactivadas (accesibilidad / reduce-motion). */
@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
