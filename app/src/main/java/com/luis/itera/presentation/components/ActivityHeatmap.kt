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
val heatmapDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", esLocale)

// Un paso por encima de SurfaceElevated (#1E2229): celda "vacía" perceptible, no un
// hueco casi invisible sobre el fondo. Compartido por AMBOS heatmaps (entrenamiento e
// hidratación): el nivel 0 siempre se lee igual, solo cambia el color de los niveles activos.
val HEATMAP_EMPTY_CELL_COLOR = Color(0xFF262A31)

// ---------------------------------------------------------------------------
// Modelo + funciones puras
// ---------------------------------------------------------------------------

/**
 * Celda genérica de heatmap: un HECHO por día, nunca una medida de esfuerzo/volumen.
 * `level` es un ordinal 0..N que cada pantalla define (p.ej. entrenamiento: 0=no entrenó,
 * 1=entrenó; hidratación: 0=sin registros, 1=parcial, 2=meta cumplida). El realce
 * (PR / 110%+) vive aparte, en `highlightDays`, y solo añade el punto ámbar.
 */
data class HeatmapCell(val date: LocalDate, val level: Int)

/** Lunes de inicio de la ventana rodante: 10 semanas terminando en la semana actual. */
fun heatmapStartMonday(today: LocalDate = LocalDate.now()): LocalDate =
    today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .minusWeeks((HEATMAP_COLUMNS - 1).toLong())

/**
 * Genera 70 celdas ORDENADAS POR DÍA-MAYOR (fila = día de la semana, columna = semana).
 * Con LazyVerticalGrid(GridCells.Fixed(COLUMNS)) en row-major, cada fila queda como un
 * día de la semana a lo largo de las 10 columnas; el índice de columna es `index % COLUMNS`.
 */
fun buildHeatmapCells(startMonday: LocalDate, levelForDate: (LocalDate) -> Int): List<HeatmapCell> {
    val cells = ArrayList<HeatmapCell>(HEATMAP_ROWS * HEATMAP_COLUMNS)
    for (weekday in 0 until HEATMAP_ROWS) {
        for (week in 0 until HEATMAP_COLUMNS) {
            val date = startMonday.plusWeeks(week.toLong()).plusDays(weekday.toLong())
            cells.add(HeatmapCell(date, levelForDate(date)))
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

/**
 * Etiqueta de mes corto SOLO cuando cambia de mes; el resto en blanco. Si la ventana visible
 * cruza un cambio de año (p.ej. dic -> ene), añade el año corto ("dic '25") para desambiguar;
 * el caso normal (mismo año) queda idéntico a antes, sin año.
 */
fun buildMonthLabels(startMonday: LocalDate, locale: Locale = esLocale): List<String> {
    val endDate = startMonday.plusWeeks((HEATMAP_COLUMNS - 1).toLong())
    val crossesYear = startMonday.year != endDate.year

    var lastMonth = -1
    return (0 until HEATMAP_COLUMNS).map { week ->
        val d = startMonday.plusWeeks(week.toLong())
        if (d.monthValue != lastMonth) {
            lastMonth = d.monthValue
            val monthName = d.month.getDisplayName(TextStyle.SHORT, locale).removeSuffix(".")
            if (crossesYear) "$monthName '${d.year % 100}" else monthName
        } else ""
    }
}

// ---------------------------------------------------------------------------
// UI
// ---------------------------------------------------------------------------

/**
 * Tarjeta con un mapa de actividad de las últimas 10 semanas. Genérica: cada pantalla
 * define su propio significado de nivel/color (entrenamiento = teal, hidratación =
 * azul-agua) pero comparten grid, animación de barrido y contorno de "hoy".
 *
 * @param levelForDate nivel 0..N de un día (0 = vacío, siempre [HEATMAP_EMPTY_CELL_COLOR]).
 * @param colorForLevel color de relleno para cada nivel devuelto por [levelForDate].
 * @param emptyBorderColor contorno de "hoy" cuando esa celda está en nivel 0.
 * @param filledBorderColor contorno de "hoy" cuando esa celda está en nivel > 0 (debe
 *   contrastar con `colorForLevel`, análogo a onAccent).
 * @param highlightDays días con el realce ámbar (punto), aparte del nivel/color.
 * @param selectionLabel texto de la línea secundaria para la celda seleccionada.
 * @param title encabezado pequeño de la tarjeta (p.ej. "MAPA DE ACTIVIDAD").
 * @param legend contenido de la leyenda, propio de cada pantalla.
 */
@Composable
fun ActivityHeatmapCard(
    levelForDate: (LocalDate) -> Int,
    colorForLevel: (Int) -> Color,
    emptyBorderColor: Color,
    filledBorderColor: Color,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    selectionLabel: (HeatmapCell, isToday: Boolean) -> String,
    legend: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    highlightDays: Set<LocalDate> = emptySet(),
    title: String = "MAPA DE ACTIVIDAD",
    emptyStateLabel: String = "Aún no hay actividad"
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IteraColors.Surface)
            .padding(16.dp)
    ) {
        ActivityHeatmap(
            levelForDate = levelForDate,
            colorForLevel = colorForLevel,
            emptyBorderColor = emptyBorderColor,
            filledBorderColor = filledBorderColor,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            selectionLabel = selectionLabel,
            legend = legend,
            highlightDays = highlightDays,
            title = title,
            emptyStateLabel = emptyStateLabel
        )
    }
}

@Composable
fun ActivityHeatmap(
    levelForDate: (LocalDate) -> Int,
    colorForLevel: (Int) -> Color,
    emptyBorderColor: Color,
    filledBorderColor: Color,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    selectionLabel: (HeatmapCell, isToday: Boolean) -> String,
    legend: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    highlightDays: Set<LocalDate> = emptySet(),
    title: String = "MAPA DE ACTIVIDAD",
    emptyStateLabel: String = "Aún no hay actividad"
) {
    val today = remember { LocalDate.now() }
    val startMonday = remember(today) { heatmapStartMonday(today) }
    val cells = remember(startMonday, levelForDate) { buildHeatmapCells(startMonday, levelForDate) }
    val dayLabels = remember { buildDayLabels() }
    val monthLabels = remember(startMonday) { buildMonthLabels(startMonday) }
    val allEmpty = remember(cells) { cells.all { it.level == 0 } }

    // La FECHA seleccionada es estado elevado (ViewModel); la celda se deriva de los datos
    // actuales para que el conteo nunca quede obsoleto tras borrar/añadir sesiones.
    val selectedCell = selectedDate?.let { d -> cells.firstOrNull { it.date == d } }

    Column(modifier.fillMaxWidth()) {
        // Cabecera: solo el título (la fecha vive en la línea secundaria).
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = IteraColors.TextPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        // Estado vacío: solo título + mensaje, sin cuadrícula ni línea de selección.
        if (allEmpty) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = emptyStateLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = IteraColors.TextSecondaryStrong
            )
            return@Column
        }

        // Línea secundaria: en blanco por defecto; texto de la celda seleccionada si hay una.
        Text(
            text = selectedCell?.let { selectionLabel(it, it.date == today) } ?: " ",
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
                                hasHighlight = cellData.date in highlightDays,
                                color = colorForLevel(cellData.level),
                                emptyBorderColor = emptyBorderColor,
                                filledBorderColor = filledBorderColor,
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
        legend()
    }
}

/** Leyenda del heatmap de ENTRENAMIENTO: "entrenaste" (verde base) · "récord (PR)" (verde pleno + punto ámbar). */
@Composable
fun TrainingHeatmapLegend() {
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
    hasHighlight: Boolean,
    color: Color,
    emptyBorderColor: Color,
    filledBorderColor: Color,
    sweep: Animatable<Float, AnimationVector1D>,
    onClick: () -> Unit
) {
    val isToday = cell.date == today
    // Si la celda ya está rellena (nivel > 0), el borde con ese mismo color se perdería:
    // usa el color de contraste (onAccent-like). Si está vacía, usa el color activo pleno.
    val todayBorderColor = if (cell.level > 0) filledBorderColor else emptyBorderColor

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
            .background(color)
            .then(
                when {
                    isSelected -> Modifier.border(1.5.dp, IteraColors.TextPrimary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    isToday -> Modifier.border(2.dp, todayBorderColor, RoundedCornerShape(4.dp))
                    else -> Modifier
                }
            )
            .clickable(onClick = onClick)
    ) {
        // Realce: OTRO color (ámbar), no otra intensidad. Nunca sustituye el color de constancia.
        if (hasHighlight) {
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
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
}
