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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.luis.itera.domain.model.ExerciseSeriesPoint
import com.luis.itera.presentation.theme.IteraColors

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

    Canvas(modifier.fillMaxWidth().height(80.dp)) {
        if (points.size < 2) return@Canvas
        val maxV = points.maxOf { it.value }.takeIf { it > 0f } ?: 1f
        val minV = points.minOf { it.value }
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val pad = 6.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 8f))

        val prY = pad + h * (1f - (maxV - minV) / range)
        drawLine(IteraColors.Accent.copy(alpha = 0.2f), Offset(0f, prY), Offset(size.width, prY), 0.5.dp.toPx(), pathEffect = dash)

        val path = Path()
        points.forEachIndexed { i, pt ->
            val x = pad + w * i / (points.size - 1)
            val y = pad + h * (1f - (pt.value - minV) / range)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        clipRect(right = size.width * progress.value) {
            drawPath(path, IteraColors.Accent, style = Stroke(width = 2.dp.toPx()))
        }

        points.forEachIndexed { i, pt ->
            val x = pad + w * i / (points.size - 1)
            val y = pad + h * (1f - (pt.value - minV) / range)
            if (x <= size.width * progress.value) {
                drawCircle(IteraColors.Accent, 3.dp.toPx(), Offset(x, y))
            }
        }
    }
}