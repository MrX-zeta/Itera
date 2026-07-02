package com.luis.itera.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay

@Composable
fun SessionTimer(startMillis: Long, modifier: Modifier = Modifier) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(startMillis) {
        while (true) {
            elapsedSeconds = (System.currentTimeMillis() - startMillis) / 1000L
            delay(1_000L)
        }
    }

    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60

    Text(
        text = "%02d:%02d".format(minutes, seconds),
        style = MaterialTheme.typography.titleLarge,
        color = IteraColors.Accent,
        modifier = modifier
            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}