package com.luis.itera.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.R
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay

enum class TimerState { INACTIVE, RUNNING, PAUSED }

@Composable
fun SessionTimer(
    startMillis: Long,
    pausedElapsed: Long,
    state: TimerState,
    onTogglePause: () -> Unit
) {
    var elapsed by remember { mutableLongStateOf(pausedElapsed) }

    LaunchedEffect(state, startMillis) {
        if (state == TimerState.RUNNING && startMillis > 0) {
            while (true) {
                elapsed = System.currentTimeMillis() - startMillis
                delay(1000L)
            }
        }
    }

    val displayElapsed = if (state == TimerState.PAUSED) pausedElapsed else elapsed
    val totalSeconds = (displayElapsed / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val blinkAlpha = if (state == TimerState.PAUSED) {
        val transition = rememberInfiniteTransition(label = "blink")
        val alpha by transition.animateFloat(
            initialValue = 1f, targetValue = 0.3f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "blink_alpha"
        )
        alpha
    } else 1f

    val color = when (state) {
        TimerState.INACTIVE -> IteraColors.TextSecondary
        TimerState.RUNNING -> IteraColors.Accent
        TimerState.PAUSED -> IteraColors.Accent
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onTogglePause)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state == TimerState.RUNNING) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_timer_itera),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = "%02d:%02d".format(minutes, seconds),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.graphicsLayer { alpha = blinkAlpha }
        )
    }
}