package com.luis.itera.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors
import kotlinx.coroutines.delay

@Composable
fun FastStepper(
    label: String,
    value: Float,
    onDelta: (Float) -> Unit,
    onValueSet: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    var editing by remember { mutableStateOf(false) }
    var textValue by remember { mutableStateOf("") }

    val displayText = if (value % 1f == 0f) value.toInt().toString() else "%.1f".format(value)

    Column(
        modifier
            .border(1.dp, IteraColors.Border, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IteraColors.TextSecondary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelta(-1f)
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelta(-5f)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge, color = IteraColors.TextSecondary)
            }

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (editing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            textValue = input.filter { it.isDigit() || it == '.' }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (!state.isFocused && editing) {
                                    editing = false
                                    textValue.toFloatOrNull()?.let { onValueSet?.invoke(it) ?: onDelta(it - value) }
                                }
                            },
                        textStyle = TextStyle(
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            color = IteraColors.Accent
                        ),
                        cursorBrush = SolidColor(IteraColors.Accent),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (label == "REPS") KeyboardType.Number else KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                    }
                } else {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        color = IteraColors.TextPrimary,
                        modifier = Modifier.clickable {
                            textValue = displayText
                            editing = true
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDelta(1f)
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelta(5f)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge, color = IteraColors.Accent)
            }
        }
    }
}

@Composable
private fun Column(
    modifier: Modifier,
    horizontalAlignment: Alignment.Horizontal,
    content: @Composable () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment
    ) { content() }
}