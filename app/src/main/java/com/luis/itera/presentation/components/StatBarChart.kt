package com.luis.itera.presentation.components

import androidx.compose.animation.core.Animatable
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
    val progress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { progress.snapTo(0f); progress.animateTo(1f, tween(600)) }

    val textMeasurer = rememberTextMeasurer()
    val xLabelStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.6f))
    val yLabelStyle = TextStyle(fontSize = 10.sp, color = IteraColors.TextSecondary.copy(alpha = 0.5f))

    Canvas(modifier.fillMaxWidth().height(110.dp)) {
        val xLabelZone = 16.dp.toPx()
        val yLabelWidth = 32.dp.toPx()
        val yLabelGap = 6.dp.toPx()
        val baseline = size.height - xLabelZone - 2.dp.toPx()
        val top = 6.dp.toPx()
        val chartHeight = baseline - top
        val chartWidth = size.width - yLabelWidth - yLabelGap
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))

        drawLine(IteraColors.Border, Offset(0f, baseline), Offset(chartWidth, baseline), 1.dp.toPx())
        drawLine(IteraColors.BorderStrong, Offset(0f, top), Offset(chartWidth, top), 0.5.dp.toPx(), pathEffect = dash)
        drawLine(IteraColors.BorderStrong, Offset(0f, top + chartHeight * 0.5f), Offset(chartWidth, top + chartHeight * 0.5f), 0.5.dp.toPx(), pathEffect = dash)

        if (points.isEmpty()) return@Canvas

        val maxV = points.maxOf { it.value }.takeIf { it > 0f } ?: 1f
        val yAnchor = chartWidth + yLabelGap

        val maxLabel = textMeasurer.measure(maxV.toInt().toString(), yLabelStyle)
        drawText(maxLabel, topLeft = Offset(yAnchor, top - maxLabel.size.height / 2f))

        val midLabel = textMeasurer.measure((maxV / 2f).toInt().toString(), yLabelStyle)
        drawText(midLabel, topLeft = Offset(yAnchor, top + chartHeight * 0.5f - midLabel.size.height / 2f))

        val slot = chartWidth / points.size
        val barWidth = (slot * 0.6f).coerceAtMost(20.dp.toPx())
        val threshold = maxV * 0.8f

        points.forEachIndexed { i, point ->
            val barH = chartHeight * (point.value / maxV) * progress.value
            val cx = slot * i + slot / 2f
            val left = cx - barWidth / 2f

            if (point.value >= threshold) {
                drawRect(IteraColors.Accent.copy(alpha = 0.25f), Offset(left, baseline - barH), Size(barWidth, barH))
            }
            drawRect(IteraColors.Accent, Offset(left, baseline - barH), Size(barWidth, barH), style = Stroke(1.dp.toPx()))

            val dateLabel = LocalDate.ofEpochDay(point.dateEpochDay).format(barDateFormatter)
            val measured = textMeasurer.measure(dateLabel, xLabelStyle)
            drawText(measured, topLeft = Offset(cx - measured.size.width / 2f, baseline + 4.dp.toPx()))
        }
    }
}