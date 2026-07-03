package com.luis.itera.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            Text(
                "Registra tus tiempos de descanso para analizar densidad",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        }
        return
    }

    val maxTotal = points.maxOf { it.workSeconds + it.restSeconds }.toFloat().coerceAtLeast(1f)
    val animatedMax by animateFloatAsState(
        targetValue = maxTotal,
        animationSpec = tween(800),
        label = "density_max"
    )

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = IteraColors.TextSecondary)
    val valueStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.5f))
    val chartHeight = (points.size * 40).coerceIn(80, 400)

    Canvas(modifier.fillMaxWidth().height(chartHeight.dp)) {
        val labelW = 48.dp.toPx()
        val valueW = 36.dp.toPx()
        val barArea = size.width - labelW - valueW - 8.dp.toPx()
        val slot = size.height / points.size
        val barH = (slot * 0.5f).coerceAtMost(18.dp.toPx())
        val r = CornerRadius(4.dp.toPx())

        points.forEachIndexed { i, pt ->
            val cy = slot * i + slot / 2f
            val total = (pt.workSeconds + pt.restSeconds).toFloat()
            val totalW = (total / animatedMax) * barArea
            val workW = if (total > 0) (pt.workSeconds / total) * totalW else 0f

            val label = textMeasurer.measure(pt.label, labelStyle)
            drawText(label, topLeft = Offset(0f, cy - label.size.height / 2f))

            if (totalW > 0) {
                drawRoundRect(Color(0xFF1A3A35), Offset(labelW, cy - barH / 2f), Size(totalW, barH), r)
                if (workW > 0) {
                    drawRoundRect(IteraColors.Accent, Offset(labelW, cy - barH / 2f), Size(workW, barH), r)
                }
            }

            val mins = "${(total / 60).toInt()}m"
            val valueMeasured = textMeasurer.measure(mins, valueStyle)
            drawText(valueMeasured, topLeft = Offset(labelW + totalW + 6.dp.toPx(), cy - valueMeasured.size.height / 2f))
        }
    }
}