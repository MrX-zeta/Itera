package com.luis.itera.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.R
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent
import kotlinx.coroutines.delay

enum class TimerState { INACTIVE, RUNNING, PAUSED }

private const val DONE_AUTO_DISMISS_MS = 4500L

private fun mmss(totalSeconds: Int): String = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)

/**
 * Overlay de descanso: CUENTA ATRÁS desde una meta configurable. El tiempo real de
 * descanso lo mide el ViewModel (setTimerStartMillis) y lo persiste sin cambios; aquí
 * solo se PRESENTA meta − transcurrido. "Saltar" y "+30 s" son estado local (no tocan
 * el modelo). Al llegar a 0: mensaje "¡Descanso listo!" + haptic, el modal permanece
 * ~4.5 s (o hasta pulsar "Listo") y se cierra solo. Nunca bloquea el siguiente set.
 */
@Composable
fun RestTimerOverlay(
    startMillis: Long,
    pausedElapsed: Long,
    state: TimerState,
    goalSeconds: Int,
    extraSeconds: Int,
    nextHint: String?,
    onTogglePause: () -> Unit,
    onAddThirty: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val goalMs = (goalSeconds + extraSeconds) * 1000L

    var elapsed by remember { mutableLongStateOf(pausedElapsed) }
    LaunchedEffect(state, startMillis, extraSeconds) {
        if (state == TimerState.RUNNING && startMillis > 0) {
            while (true) {
                elapsed = System.currentTimeMillis() - startMillis
                delay(250L)
            }
        }
    }

    val displayElapsed = if (state == TimerState.PAUSED) pausedElapsed else elapsed
    val done = displayElapsed >= goalMs
    val remainingSecs = ((goalMs - displayElapsed).coerceAtLeast(0L) / 1000).toInt()

    // Aviso háptico único al llegar a 0. Se re-arma al empezar otro descanso o al pulsar +30 s.
    var alerted by remember(startMillis, extraSeconds) { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done && !alerted) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            alerted = true
        }
    }
    // Terminado: el modal permanece un rato por si el usuario no está mirando, y se cierra solo.
    LaunchedEffect(done) {
        if (done) {
            delay(DONE_AUTO_DISMISS_MS)
            onSkip()
        }
    }

    val accent = LocalAccent.current.color
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(IteraColors.SurfaceElevated)
            // Consume los toques: sin esto, tocar el modal seleccionaba ejercicios detrás.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (done) {
            Spacer(Modifier.height(6.dp))
            Text(
                "¡Descanso listo!",
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
        } else {
            Text(
                "DESCANSO",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = IteraColors.TextSecondary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = mmss(remainingSecs),
                fontFamily = FontFamily.Monospace,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = IteraColors.TextPrimary
            )
            Text(
                "de ${mmss(goalSeconds + extraSeconds)}",
                style = MaterialTheme.typography.bodyMedium,
                color = IteraColors.TextSecondary
            )
        }
        if (nextHint != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Siguiente: $nextHint",
                style = MaterialTheme.typography.bodyMedium,
                color = if (done) IteraColors.TextPrimary else accent,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(14.dp))
        if (done) {
            RestAction("Listo", R.drawable.ic_check, accent, Modifier.fillMaxWidth(), onSkip)
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // "Saltar" es la acción más frecuente: acento y un poco más de ancho.
                RestAction("Saltar", R.drawable.ic_check, accent, Modifier.weight(1.25f), onSkip)
                RestAction(
                    if (state == TimerState.PAUSED) "Reanudar" else "Pausar",
                    if (state == TimerState.PAUSED) R.drawable.ic_widget_play else R.drawable.ic_pause,
                    IteraColors.TextPrimary,
                    Modifier.weight(1f),
                    onTogglePause
                )
                RestAction("+30 s", R.drawable.ic_rest_timer, IteraColors.TextPrimary, Modifier.weight(1f), onAddThirty)
            }
        }
    }
}

@Composable
private fun RestAction(
    text: String,
    iconRes: Int,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IteraColors.Surface)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            ImageVector.vectorResource(iconRes),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = contentColor
        )
    }
}
