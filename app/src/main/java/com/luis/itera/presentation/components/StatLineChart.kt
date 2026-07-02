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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.presentation.theme.IteraColors
import androidx.compose.ui.graphics.drawscope.clipRect

@Composable
fun StatLineChart(
    points: List<ExerciseSeriesPoint>,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = if (points.isEmpty()) 0f else 1f,
        animationSpec = tween(600),
        label = "line_progress"
    )

    Canvas(
        modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val baseline = size.height - 14.dp.toPx()
        drawLine(IteraColors.Border, Offset(0f, baseline), Offset(size.width, baseline), 1.dp.toPx())

        if (points.isEmpty()) return@Canvas

        val minV = points.minOf { it.value }
        val maxV = points.maxOf { it.value }
        val rangeV = (maxV - minV).takeIf { it > 0f } ?: 1f
        val top = 8.dp.toPx()
        val chartHeight = baseline - top - 8.dp.toPx()

        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))
        drawLine(IteraColors.Border, Offset(0f, top), Offset(size.width, top), 0.5.dp.toPx(), pathEffect = dash)
        drawLine(
            IteraColors.Border,
            Offset(0f, top + chartHeight / 2),
            Offset(size.width, top + chartHeight / 2),
            0.5.dp.toPx(),
            pathEffect = dash
        )

        val stepX = if (points.size == 1) 0f else (size.width - 24.dp.toPx()) / (points.size - 1)
        val startX = 12.dp.toPx()

        fun pointAt(index: Int): Offset {
            val normalized = (points[index].value - minV) / rangeV
            return Offset(
                x = startX + stepX * index,
                y = top + chartHeight * (1f - normalized)
            )
        }

        val path = Path()
        points.indices.forEach { i ->
            val p = pointAt(i)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }

        clipRect(right = size.width * progress) {
            drawPath(path, IteraColors.Accent, style = Stroke(width = 1.5.dp.toPx()))
        }

        val maxIndex = points.indexOfFirst { it.value == maxV }
        points.indices.forEach { i ->
            val p = pointAt(i)
            if (i == maxIndex) {
                drawCircle(IteraColors.Accent, radius = 3.5.dp.toPx(), center = p)
            } else {
                drawCircle(IteraColors.Background, radius = 3.dp.toPx(), center = p)
                drawCircle(
                    IteraColors.Accent,
                    radius = 3.dp.toPx(),
                    center = p,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}