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
 * Rayita de color de una rutina: recta en el tramo central, con giros SUAVES y amplios en los
 * dos extremos que acompañan el radio de la card (nada de codos bruscos que lean rectangular
 * junto a tanta esquina redondeada). Fina y pegada al borde izquierdo real de la card.
 * Compartida entre el Home y la pestaña Rutinas para un tratamiento visual idéntico.
 */
@Composable
fun RoutineAccent(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.width(9.dp)) {
        val strokePx = 3.dp.toPx()
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.95f, h * 0.03f)
            quadraticBezierTo(w * 0.25f, h * 0.03f, w * 0.25f, h * 0.18f)
            lineTo(w * 0.25f, h * 0.82f)
            quadraticBezierTo(w * 0.25f, h * 0.97f, w * 0.95f, h * 0.97f)
        }
        drawPath(path, color = color, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}
