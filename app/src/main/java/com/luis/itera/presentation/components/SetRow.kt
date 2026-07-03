package com.luis.itera.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.domain.model.WorkoutSet
import com.luis.itera.presentation.theme.IteraColors

@Composable
fun SetRow(
    set: WorkoutSet,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append("SET ${set.order} · ")
                    if (set.durationSeconds > 0) {
                        append("${set.durationSeconds / 60} min")
                        if (set.intensity > 0) append(" · nivel ${set.intensity}")
                    } else {
                        append("${set.reps} reps")
                        if (set.weightAddedKg > 0f) append(" +${set.weightAddedKg}kg")
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (set.workSeconds > 0) {
                Text(
                    text = "⏱ ${formatTime(set.workSeconds)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = IteraColors.TextSecondary.copy(alpha = 0.6f)
                )
            }
        }
        trailing?.invoke()
    }
}

private fun formatTime(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else -> "${seconds / 60}m${seconds % 60}s"
}