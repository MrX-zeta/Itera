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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.presentation.theme.IteraColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val barDateFormatter = DateTimeFormatter.ofPattern("dd/MM", Locale("es"))

@Composable
fun StatBarChart(
    points: List<ExerciseSeriesPoint>,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
    }

    val textMeasurer = rememberTextMeasurer()
    val xStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.6f))
    val yStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.45f))

    Canvas(modifier.fillMaxWidth().height(100.dp)) {
        val xZone = 16.dp.toPx()
        val yW = 30.dp.toPx()
        val baseline = size.height - xZone
        val top = 4.dp.toPx()
        val h = baseline - top
        val w = size.width - yW
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))

        drawLine(IteraColors.Border, Offset(0f, baseline), Offset(w, baseline), 0.5.dp.toPx())

        if (points.isEmpty()) return@Canvas
        val maxV = points.maxOf { it.value }.takeIf { it > 0f } ?: 1f

        listOf(0.25f, 0.5f, 0.75f, 1f).forEach { pct ->
            val y = baseline - h * pct
            drawLine(IteraColors.BorderStrong, Offset(0f, y), Offset(w, y), 0.5.dp.toPx(), pathEffect = dash)
            val label = textMeasurer.measure((maxV * pct).toInt().toString(), yStyle)
            drawText(label, topLeft = Offset(w + 4.dp.toPx(), y - label.size.height / 2f))
        }

        val slot = w / points.size
        val barW = (slot * 0.6f).coerceAtMost(18.dp.toPx())

        points.forEachIndexed { i, pt ->
            val bH = h * (pt.value / maxV) * progress.value
            val cx = slot * i + slot / 2f
            val left = cx - barW / 2f

            if (pt.value >= maxV * 0.8f) {
                drawRect(IteraColors.Accent.copy(alpha = 0.25f), Offset(left, baseline - bH), Size(barW, bH))
            }
            drawRect(IteraColors.Accent, Offset(left, baseline - bH), Size(barW, bH), style = Stroke(1.dp.toPx()))

            val dl = LocalDate.ofEpochDay(pt.dateEpochDay).format(barDateFormatter)
            val m = textMeasurer.measure(dl, xStyle)
            drawText(m, topLeft = Offset(cx - m.size.width / 2f, baseline + 3.dp.toPx()))
        }
    }
}