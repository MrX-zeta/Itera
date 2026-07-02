package com.luis.itera.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luis.itera.presentation.theme.IteraColors

@Composable
fun FastStepper(
    label: String,
    value: Float,
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
    format: (Float) -> String = {
        if (it % 1f == 0f) it.toInt().toString() else "%.1f".format(it)
    }
) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepButton("−", IteraColors.TextSecondary) { onDelta(-it) }
            Text(
                text = format(value),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                color = IteraColors.TextPrimary
            )
            StepButton("+", IteraColors.Accent) { onDelta(it) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StepButton(
    symbol: String,
    tint: Color,
    onStep: (Float) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .combinedClickable(
                onClick = { onStep(1f) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStep(5f)
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, color = tint, style = MaterialTheme.typography.titleMedium)
    }
}