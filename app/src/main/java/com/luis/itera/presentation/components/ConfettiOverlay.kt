package com.luis.itera.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.luis.itera.presentation.theme.LocalAccent
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val drift: Float,
    val size: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val shape: Int
)

@Composable
fun ConfettiOverlay(trigger: Boolean) {
    if (!trigger) return

    val accent = LocalAccent.current.color
    val progress = remember { Animatable(0f) }
    val particles = remember(accent) {
        val colors = listOf(
            accent,
            accent.copy(alpha = 0.7f),
            accent.copy(alpha = 0.4f),
            Color(0xFF0D9B7A),
            Color(0xFF0A7D63),
            Color.White.copy(alpha = 0.6f)
        )
        List(120) {
            Particle(
                x = Random.nextFloat(),
                startY = Random.nextFloat() * -0.3f,
                speed = 0.4f + Random.nextFloat() * 0.6f,
                drift = (Random.nextFloat() - 0.5f) * 0.15f,
                size = 4f + Random.nextFloat() * 8f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 720f,
                color = colors.random(),
                shape = Random.nextInt(3)
            )
        }
    }

    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(2500, easing = LinearEasing))
    }

    Canvas(Modifier.fillMaxSize()) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val fadeOut = if (t > 0.7f) 1f - ((t - 0.7f) / 0.3f) else 1f

        particles.forEach { p ->
            val currentX = (p.x + p.drift * t) * size.width
            val currentY = (p.startY + p.speed * t) * size.height
            val currentRotation = p.rotation + p.rotationSpeed * t
            val alpha = (fadeOut * p.color.alpha).coerceIn(0f, 1f)
            val color = p.color.copy(alpha = alpha)

            if (currentY < 0 || currentY > size.height) return@forEach

            rotate(currentRotation, Offset(currentX, currentY)) {
                when (p.shape) {
                    0 -> drawRect(color, Offset(currentX - p.size / 2, currentY - p.size / 2), Size(p.size, p.size * 0.6f))
                    1 -> drawCircle(color, p.size / 2.5f, Offset(currentX, currentY))
                    2 -> drawRect(color, Offset(currentX - p.size / 2, currentY - 1.5f), Size(p.size, 3f))
                }
            }
        }
    }
}