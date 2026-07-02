package com.luis.itera.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.presentation.theme.IteraColors

@Composable
fun StatBarChart(
    points: List<ExerciseSeriesPoint>,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(600),
        label = "bar_progress"
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val baseline = size.height - 4.dp.toPx()
        drawLine(IteraColors.Border, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())

        if (points.isEmpty()) return@Canvas

        val maxV = points.maxOf { it.value }.takeIf { it > 0f } ?: 1f
        val top = 6.dp.toPx()
        val chartHeight = baseline - top
        val slot = size.width / points.size
        val barWidth = (slot * 0.55f).coerceAtMost(20.dp.toPx())
        val threshold = maxV * 0.8f

        points.forEachIndexed { i, point ->
            val barHeight = chartHeight * (point.value / maxV) * progress
            val left = slot * i + (slot - barWidth) / 2
            val topLeft = Offset(left, baseline - barHeight)
            val barSize = Size(barWidth, barHeight)

            if (point.value >= threshold) {
                drawRect(IteraColors.Accent.copy(alpha = 0.25f), topLeft, barSize)
            }
            drawRect(IteraColors.Accent, topLeft, barSize, style = Stroke(width = 1.dp.toPx()))
        }
    }
}