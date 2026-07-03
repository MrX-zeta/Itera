package com.luis.itera.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay

@Composable
fun SessionTimer(startMillis: Long) {
    var elapsed by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startMillis) {
        while (true) {
            elapsed = System.currentTimeMillis() - startMillis
            delay(1000L)
        }
    }

    val totalSeconds = (elapsed / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    Text(
        text = "%02d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
        color = IteraColors.Accent
    )
}