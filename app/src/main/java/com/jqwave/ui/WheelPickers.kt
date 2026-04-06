package com.jqwave.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter as flowFilter

private val ClockPickerLightBg = Color(0xFFD8F0EC)

private val ClockWheelItemHeight = 52.dp
private val ClockWheelVisibleHeight = ClockWheelItemHeight
private val ClockWheelWidth = 72.dp

private const val OFFSET_MAX_MINUTES = 600

/**
 * Single-column padded number list: one row visible, [00] formatting, no divider lines or wheel peek rows.
 * Numbers are centered horizontally and vertically in each row.
 */
@Composable
private fun ZeroPaddedTimeWheel(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    textColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val count = range.last - range.first + 1
    val initialIndex = (value - range.first).coerceIn(0, count - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val itemPx = remember(density) { with(density) { ClockWheelItemHeight.roundToPx() } }
    val latestValue = rememberUpdatedState(value)

    LaunchedEffect(value, count) {
        val idx = (value - range.first).coerceIn(0, count - 1)
        if (listState.firstVisibleItemIndex != idx || abs(listState.firstVisibleItemScrollOffset) > itemPx / 4) {
            listState.scrollToItem(idx)
        }
    }

    LaunchedEffect(listState, range.first, range.last) {
        var skipInitialIdle = true
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .flowFilter { !it }
            .collect {
                if (skipInitialIdle) {
                    skipInitialIdle = false
                    return@collect
                }
                val idx = listState.centerItemIndex() ?: return@collect
                val newVal = (range.first + idx).coerceIn(range.first, range.last)
                if (newVal != latestValue.value) onValueChange(newVal)
            }
    }

    Box(
        modifier = modifier
            .width(ClockWheelWidth)
            .height(ClockWheelVisibleHeight)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(ClockWheelVisibleHeight),
            userScrollEnabled = true,
        ) {
            items(
                count = count,
                key = { index -> range.first + index },
            ) { index ->
                val v = range.first + index
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ClockWheelItemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = String.format(Locale.ROOT, "%02d", v),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun LazyListState.centerItemIndex(): Int? {
    val info = layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return null
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull { item ->
        val itemCenter = item.offset + item.size / 2
        abs(itemCenter - viewportCenter)
    }?.index
}

@Composable
fun HourMinuteWheelRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        ZeroPaddedTimeWheel(
            value = hour,
            range = 0..23,
            onValueChange = onHourChange,
            textColor = scheme.onSurface,
            backgroundColor = ClockPickerLightBg,
        )
        Box(
            modifier = Modifier.height(ClockWheelVisibleHeight),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                ":",
                style = MaterialTheme.typography.headlineSmall,
                color = scheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        ZeroPaddedTimeWheel(
            value = minute,
            range = 0..59,
            onValueChange = onMinuteChange,
            textColor = scheme.onSurface,
            backgroundColor = ClockPickerLightBg,
        )
    }
}

@Composable
fun RelativeOffsetPickers(
    offsetMinutes: Int,
    onOffsetChange: (Int) -> Unit,
    beforeText: String,
    afterText: String,
    modifier: Modifier = Modifier,
) {
    val isBefore = offsetMinutes < 0
    val magnitude = abs(offsetMinutes).coerceIn(0, OFFSET_MAX_MINUTES)
    val h = magnitude / 60
    val m = magnitude % 60
    val scheme = MaterialTheme.colorScheme

    fun commit(hour: Int, minute: Int) {
        val total = (hour * 60 + minute).coerceIn(0, OFFSET_MAX_MINUTES)
        onOffsetChange(if (isBefore) -total else total)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalButton(
                onClick = {
                    val t = (h * 60 + m).coerceIn(0, OFFSET_MAX_MINUTES)
                    if (isBefore) {
                        onOffsetChange(t)
                    } else {
                        onOffsetChange(-t)
                    }
                },
                modifier = Modifier.widthIn(min = 108.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                ),
            ) {
                Text(if (isBefore) beforeText else afterText)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ZeroPaddedTimeWheel(
                    value = h,
                    range = 0..10,
                    onValueChange = { nh -> commit(nh, m) },
                    textColor = scheme.onSurface,
                    backgroundColor = ClockPickerLightBg,
                )
                Box(
                    modifier = Modifier.height(ClockWheelVisibleHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
                ZeroPaddedTimeWheel(
                    value = m,
                    range = 0..59,
                    onValueChange = { nm -> commit(h, nm) },
                    textColor = scheme.onSurface,
                    backgroundColor = ClockPickerLightBg,
                )
            }
        }
    }
}
