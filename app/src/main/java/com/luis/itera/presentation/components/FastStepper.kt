package com.luis.itera.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luis.itera.presentation.theme.IteraColors
import com.luis.itera.presentation.theme.LocalAccent

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
    var hasGainedFocus by remember { mutableStateOf(false) }
    var optimisticValue by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(value) { optimisticValue = null }

    val currentDisplayValue = optimisticValue ?: value
    val displayText = if (currentDisplayValue % 1f == 0f)
        currentDisplayValue.toInt().toString()
    else "%.1f".format(currentDisplayValue)

    var textValue by remember(editing) { mutableStateOf(displayText) }

    val commitEditing = {
        val parsed = textValue.toFloatOrNull() ?: 0f
        optimisticValue = parsed
        if (onValueSet != null) onValueSet(parsed)
        else {
            val delta = parsed - value
            if (delta != 0f) onDelta(delta)
        }
        editing = false
        hasGainedFocus = false
    }

    val exitEditingForButton = {
        if (editing) {
            editing = false
            hasGainedFocus = false
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(IteraColors.SurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
            color = IteraColors.TextSecondary
        )
        Spacer(Modifier.height(10.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IteraColors.Surface)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                exitEditingForButton()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDelta(-1f)
                            },
                            onLongPress = {
                                exitEditingForButton()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelta(-5f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = IteraColors.TextPrimary)
            }

            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (editing) {
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))) {
                                val cleaned = input.trimStart('0')
                                textValue = when {
                                    input.isEmpty() -> ""
                                    input == "0" -> "0"
                                    cleaned.isEmpty() -> "0"
                                    cleaned.startsWith('.') -> "0$cleaned"
                                    else -> cleaned
                                }
                            }
                        },
                        modifier = Modifier
                            .focusRequester(focusRequester)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    hasGainedFocus = true
                                } else if (editing && hasGainedFocus) {
                                    commitEditing()
                                }
                            },
                        textStyle = TextStyle(
                            fontSize = 26.sp,
                            textAlign = TextAlign.Center,
                            color = LocalAccent.current.color
                        ),
                        cursorBrush = SolidColor(LocalAccent.current.color),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (label == "REPS") KeyboardType.Number else KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                commitEditing()
                                focusManager.clearFocus()
                            }
                        )
                    )
                    LaunchedEffect(Unit) { focusRequester.requestFocus() }
                } else {
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 26.sp),
                        color = IteraColors.TextPrimary,
                        modifier = Modifier.clickable { editing = true }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IteraColors.Surface)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                exitEditingForButton()
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDelta(1f)
                            },
                            onLongPress = {
                                exitEditingForButton()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelta(5f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp), color = LocalAccent.current.color)
            }
        }
    }
}