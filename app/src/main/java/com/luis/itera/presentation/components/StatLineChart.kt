package com.luis.itera.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val lineDateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("es"))

@Composable
fun StatLineChart(
    points: List<ExerciseSeriesPoint>,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 12.sp, color = IteraColors.TextSecondary.copy(alpha = 0.7f))
    val dateStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.6f))
    val accent = LocalAccent.current.color

    Canvas(modifier.fillMaxWidth().height(92.dp)) {
        if (points.size < 2) return@Canvas
        val dataMax = points.maxOf { it.value }
        val dataMin = points.minOf { it.value }
        val padAmount = if (dataMax > 0f && (dataMax - dataMin) / dataMax < 0.15f) dataMax * 0.1f else 0f
        val minV = (dataMin - padAmount).coerceAtLeast(0f)
        val maxV = (dataMax + padAmount).coerceAtLeast(1f)
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val pad = 6.dp.toPx()
        val topPad = 20.dp.toPx()
        val labelZone = 14.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - topPad - labelZone
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))

        val prY = topPad + h * (1f - (dataMax - minV) / range)
        drawLine(accent.copy(alpha = 0.2f), Offset(0f, prY), Offset(size.width, prY), 0.5.dp.toPx(), pathEffect = dash)

        val path = Path()
        val coords = points.mapIndexed { i, pt ->
            val x = pad + w * i / (points.size - 1)
            val y = if (maxV == minV) topPad + h * 0.5f
            else topPad + h * (1f - (pt.value - minV) / range)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            Triple(x, y, pt.value)
        }

        clipRect(right = size.width * progress.value) {
            drawPath(path, accent, style = Stroke(width = 2.dp.toPx()))
        }

        val maxIndex = points.indices.maxBy { points[it].value }
        val minIndex = points.indices.minBy { points[it].value }

        coords.forEachIndexed { i, (x, y, value) ->
            if (x > size.width * progress.value) return@forEachIndexed

            drawCircle(accent, 3.dp.toPx(), Offset(x, y))

            if (i == maxIndex || i == minIndex) {
                val label = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)
                val measured = textMeasurer.measure(label, labelStyle)
                val labelX = (x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width)
                val labelY = y - measured.size.height - 12.dp.toPx()
                drawText(measured, topLeft = Offset(labelX, labelY.coerceAtLeast(0f)))
            }
        }

        val baseY = topPad + h + 2.dp.toPx()
        val dateIdx = if (points.size <= 3) points.indices.toList() else listOf(0, points.size / 2, points.lastIndex)
        dateIdx.forEach { idx ->
            val x = pad + w * idx / (points.size - 1)
            val dl = LocalDate.ofEpochDay(points[idx].dateEpochDay).format(lineDateFormatter)
            val m = textMeasurer.measure(dl, dateStyle)
            val lx = (x - m.size.width / 2f).coerceIn(0f, size.width - m.size.width)
            drawText(m, topLeft = Offset(lx, baseY))
        }
    }
}