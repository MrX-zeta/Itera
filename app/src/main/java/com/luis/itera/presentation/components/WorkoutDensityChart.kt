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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    // 1. Dos estados de animación completamente independientes
    val bgProgress = remember { Animatable(0f) }
    val fgProgress = remember { Animatable(0f) }

    LaunchedEffect(points) {
        bgProgress.snapTo(0f)
        fgProgress.snapTo(0f)

        // 2. Fase de Entrada: Salen casi juntas
        launch {
            // La barra opaca (total) se va directo al 100% de su tamaño
            bgProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }

        // La barra cyan fuerte llega simulando el valor "anterior" (75% de su tamaño total)
        fgProgress.animateTo(0.75f, tween(700, easing = FastOutSlowInEasing))

        // 3. Fase de Pausa: Se mantiene un instante para que el ojo lo procese
        delay(200)

        // 4. Fase de Actualización: Da el estirón final hasta su valor real (100%)
        fgProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = IteraColors.TextSecondary)
    val valueStyle = TextStyle(fontSize = 9.sp, color = IteraColors.TextSecondary.copy(alpha = 0.5f))
    val chartHeight = (points.size * 40).coerceIn(80, 400)

    Canvas(modifier.fillMaxWidth().height(chartHeight.dp)) {
        val maxTotal = points.maxOf { it.workSeconds + it.restSeconds }.toFloat().coerceAtLeast(1f)
        val labelW = 48.dp.toPx()
        val valueW = 36.dp.toPx()
        val barArea = size.width - labelW - valueW - 8.dp.toPx()
        val slot = size.height / points.size
        val barH = (slot * 0.5f).coerceAtMost(18.dp.toPx())
        val r = CornerRadius(4.dp.toPx())

        points.forEachIndexed { i, pt ->
            val cy = slot * i + slot / 2f
            val total = (pt.workSeconds + pt.restSeconds).toFloat()

            // 5. Matemáticas separadas para que cada barra respete su propia animación
            val maxBarW = (total / maxTotal) * barArea
            val maxWorkW = if (total > 0) (pt.workSeconds / total) * maxBarW else 0f

            val currentBgW = maxBarW * bgProgress.value
            val currentFgW = maxWorkW * fgProgress.value

            val label = textMeasurer.measure(pt.label, labelStyle)
            drawText(label, topLeft = Offset(0f, cy - label.size.height / 2f))

            if (currentBgW > 0) {
                // Dibujo de la barra opaca
                drawRoundRect(
                    color = Color(0xFF1A3A35),
                    topLeft = Offset(labelW, cy - barH / 2f),
                    size = Size(currentBgW, barH),
                    cornerRadius = r
                )

                // Dibujo de la barra cyan fuerte
                if (currentFgW > 0) {
                    drawRoundRect(
                        color = IteraColors.Accent,
                        topLeft = Offset(labelW, cy - barH / 2f),
                        size = Size(currentFgW, barH),
                        cornerRadius = r
                    )
                }
            }

            // El texto de los minutos se desliza suavemente junto con la barra opaca
            val mins = "${(total / 60).toInt()}m"
            val vm = textMeasurer.measure(mins, valueStyle)
            drawText(vm, topLeft = Offset(labelW + currentBgW + 6.dp.toPx(), cy - vm.size.height / 2f))
        }
    }
}