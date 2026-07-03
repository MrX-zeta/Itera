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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors

data class DensityPoint(
    val label: String,
    val workSeconds: Int,
    val restSeconds: Int
)

@Composable
fun WorkoutDensityChart(
    points: List<DensityPoint>,
    modifier: Modifier = Modifier
) {
    val progress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) { progress.snapTo(0f); progress.animateTo(1f, tween(600)) }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = IteraColors.TextSecondary)

    Canvas(
        modifier
            .fillMaxWidth()
            .height((points.size * 36).coerceAtLeast(36).dp)
    ) {
        if (points.isEmpty()) return@Canvas

        val maxTotal = points.maxOf { it.workSeconds + it.restSeconds }.toFloat().coerceAtLeast(1f)
        val labelWidth = 48.dp.toPx()
        val barArea = size.width - labelWidth - 8.dp.toPx()
        val slot = size.height / points.size
        val barH = (slot * 0.55f).coerceAtMost(16.dp.toPx())
        val r = CornerRadius(4.dp.toPx())

        points.forEachIndexed { i, pt ->
            val cy = slot * i + slot / 2f
            val total = (pt.workSeconds + pt.restSeconds).toFloat()
            val totalW = (total / maxTotal) * barArea * progress.value
            val workW = if (total > 0) (pt.workSeconds / total) * totalW else 0f

            val label = textMeasurer.measure(pt.label, labelStyle)
            drawText(label, topLeft = Offset(0f, cy - label.size.height / 2f))

            if (totalW > 0) {
                drawRoundRect(Color(0xFF1A3A35), Offset(labelWidth, cy - barH / 2f), Size(totalW, barH), r)
                if (workW > 0) drawRoundRect(IteraColors.Accent, Offset(labelWidth, cy - barH / 2f), Size(workW, barH), r)
            }
        }
    }
}