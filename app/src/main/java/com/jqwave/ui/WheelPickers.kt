package com.jqwave.ui

import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private val PickerHeight = 160.dp
private val PickerWidth = 72.dp

@Composable
fun HourMinuteWheelRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IntWheelPicker(
            value = hour,
            range = 0..23,
            onValueChange = onHourChange,
            modifier = Modifier
                .width(PickerWidth)
                .height(PickerHeight),
        )
        Text(":", style = MaterialTheme.typography.headlineSmall)
        IntWheelPicker(
            value = minute,
            range = 0..59,
            onValueChange = onMinuteChange,
            modifier = Modifier
                .width(PickerWidth)
                .height(PickerHeight),
        )
    }
}

@Composable
fun IntWheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = {
            NumberPicker(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                minValue = range.first
                maxValue = range.last
                wrapSelectorWheel = true
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        update = { picker ->
            if (picker.minValue != range.first || picker.maxValue != range.last) {
                picker.minValue = range.first
                picker.maxValue = range.last
            }
            if (picker.value != value) {
                picker.value = value
            }
        },
    )
}

private const val OFFSET_MAX_ABS = 600
private val OFFSET_RANGE_SIZE = OFFSET_MAX_ABS * 2 + 1

@Composable
fun OffsetMinuteWheelPicker(
    offsetMinutes: Int,
    onOffsetChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val labels = remember {
        Array(OFFSET_RANGE_SIZE) { idx -> "${idx - OFFSET_MAX_ABS}" }
    }
    val internalValue = (offsetMinutes + OFFSET_MAX_ABS).coerceIn(0, OFFSET_RANGE_SIZE - 1)

    AndroidView(
        modifier = modifier
            .width(88.dp)
            .height(PickerHeight),
        factory = {
            NumberPicker(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                minValue = 0
                maxValue = OFFSET_RANGE_SIZE - 1
                displayedValues = labels
                wrapSelectorWheel = false
                descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
                setOnValueChangedListener { _, _, newVal ->
                    onOffsetChange(newVal - OFFSET_MAX_ABS)
                }
            }
        },
        update = { picker ->
            if (picker.value != internalValue) {
                picker.value = internalValue
            }
        },
    )
}
