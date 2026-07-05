package com.luis.itera.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors

data class DensityPoint(
    val label: String,
    val volumeKg: Float
)

@Composable
fun WorkoutDensityChart(
    points: List<DensityPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            Text(
                "Registra sets con peso para ver tu volumen semanal",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        }
        return
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(points) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 12.sp, color = IteraColors.TextSecondary)
    val valueStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = IteraColors.Accent)
    val chartHeight = (points.size * 42).coerceIn(84, 220)

    Canvas(modifier.fillMaxWidth().height(chartHeight.dp)) {
        val maxVolume = points.maxOf { it.volumeKg }.coerceAtLeast(1f)
        val labelW = 96.dp.toPx()
        val valueW = 76.dp.toPx()
        val barArea = size.width - labelW - valueW - 8.dp.toPx()
        val slot = size.height / points.size
        val barH = (slot * 0.45f).coerceAtMost(16.dp.toPx())
        val r = CornerRadius(4.dp.toPx())

        points.forEachIndexed { i, pt ->
            val cy = slot * i + slot / 2f
            val barW = (pt.volumeKg / maxVolume) * barArea * progress.value

            val label = textMeasurer.measure(pt.label, labelStyle)
            drawText(label, topLeft = Offset(0f, cy - label.size.height / 2f))

            drawRoundRect(
                color = IteraColors.BorderStrong,
                topLeft = Offset(labelW, cy - barH / 2f),
                size = Size(barArea, barH),
                cornerRadius = r
            )
            if (barW > 0) {
                drawRoundRect(
                    color = IteraColors.Accent,
                    topLeft = Offset(labelW, cy - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = r
                )
            }

            val volText = if (pt.volumeKg >= 1000f) "%.1f ton".format(pt.volumeKg / 1000f)
            else "${pt.volumeKg.toInt()} kg"
            val vm = textMeasurer.measure(volText, valueStyle)
            drawText(vm, topLeft = Offset(labelW + barArea + 6.dp.toPx(), cy - vm.size.height / 2f))
        }
    }
}