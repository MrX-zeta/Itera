package com.luis.itera.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Rayita de color de una rutina: recta en casi todo su largo, con un giro breve y sutil solo
 * en los dos extremos (no una llave "{" pronunciada). Fina y pegada al borde izquierdo real de
 * la card. Compartida entre el Home y la pestaña Rutinas para un tratamiento visual idéntico.
 */
@Composable
fun RoutineAccent(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.width(7.dp)) {
        val strokePx = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.75f, h * 0.05f)
            quadraticBezierTo(w * 0.3f, h * 0.05f, w * 0.3f, h * 0.14f)
            lineTo(w * 0.3f, h * 0.86f)
            quadraticBezierTo(w * 0.3f, h * 0.95f, w * 0.75f, h * 0.95f)
        }
        drawPath(path, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}
