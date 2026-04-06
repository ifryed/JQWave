@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jqwave.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jqwave.R
import com.jqwave.data.EventKind
import com.jqwave.data.NotificationRule
import com.jqwave.data.TimeAnchor
import com.jqwave.data.UserLocation

@Composable
fun EventListScreen(
    rows: List<EventUiState>,
    location: UserLocation,
    onEnabledChange: (EventKind, Boolean) -> Unit,
    onRulesChange: (EventKind, List<NotificationRule>) -> Unit,
    onLocationChange: (Double, Double, String) -> Unit,
    onInIsraelChange: (Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            rows.forEach { row ->
                EventCard(
                    row = row,
                    onEnabledChange = { onEnabledChange(row.kind, it) },
                    onRulesChange = { onRulesChange(row.kind, it) },
                )
            }
            LocationCard(
                location = location,
                onLocationChange = onLocationChange,
                onInIsraelChange = onInIsraelChange,
            )
        }
    }
}

@Composable
private fun EventCard(
    row: EventUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<NotificationRule>) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(row.kind.displayName, style = MaterialTheme.typography.titleMedium)
                Switch(checked = row.enabled, onCheckedChange = onEnabledChange)
            }
            if (row.enabled) {
                Spacer(Modifier.height(12.dp))
                row.rules.forEach { rule ->
                    RuleRow(
                        rule = rule,
                        showRemove = row.rules.size > 1,
                        onRuleChange = { updated ->
                            onRulesChange(row.rules.map { if (it.id == updated.id) updated else it })
                        },
                        onRemove = {
                            onRulesChange(row.rules.filter { it.id != rule.id })
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(
                        onClick = {
                            onRulesChange(row.rules + NotificationRule())
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    rule: NotificationRule,
    showRemove: Boolean,
    onRuleChange: (NotificationRule) -> Unit,
    onRemove: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val anchors = listOf(
                    TimeAnchor.CLOCK to stringResource(R.string.anchor_clock_chip),
                    TimeAnchor.SUNRISE to stringResource(R.string.anchor_sunrise_chip),
                    TimeAnchor.SUNSET to stringResource(R.string.anchor_sunset_chip),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.anchor_when_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        anchors.forEach { (anchor, label) ->
                            FilterChip(
                                selected = rule.anchor == anchor,
                                onClick = { onRuleChange(rule.copy(anchor = anchor)) },
                                label = { Text(label, maxLines = 1) },
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                if (showRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            when (rule.anchor) {
                TimeAnchor.CLOCK -> {
                    Text(
                        stringResource(R.string.local_time_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    HourMinuteWheelRow(
                        hour = rule.hour,
                        minute = rule.minute,
                        onHourChange = { onRuleChange(rule.copy(hour = it)) },
                        onMinuteChange = { onRuleChange(rule.copy(minute = it)) },
                    )
                }
                TimeAnchor.SUNRISE, TimeAnchor.SUNSET -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.offset_minutes),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text(
                                stringResource(R.string.offset_minutes_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OffsetMinuteWheelPicker(
                            offsetMinutes = rule.offsetMinutes,
                            onOffsetChange = { onRuleChange(rule.copy(offsetMinutes = it)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    location: UserLocation,
    onLocationChange: (Double, Double, String) -> Unit,
    onInIsraelChange: (Boolean) -> Unit,
) {
    var latText by remember { mutableStateOf(location.latitude.toString()) }
    var lonText by remember { mutableStateOf(location.longitude.toString()) }
    var tzText by remember { mutableStateOf(location.timeZoneId) }
    LaunchedEffect(location.latitude, location.longitude, location.timeZoneId) {
        latText = location.latitude.toString()
        lonText = location.longitude.toString()
        tzText = location.timeZoneId
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.location_section), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.location_auto_refresh),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text(stringResource(R.string.latitude)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = lonText,
                onValueChange = { lonText = it },
                label = { Text(stringResource(R.string.longitude)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = tzText,
                onValueChange = { tzText = it },
                label = { Text(stringResource(R.string.time_zone_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.in_israel_calendar))
                Switch(checked = location.inIsrael, onCheckedChange = onInIsraelChange)
            }
            Button(
                onClick = {
                    val lat = latText.toDoubleOrNull() ?: return@Button
                    val lon = lonText.toDoubleOrNull() ?: return@Button
                    if (tzText.isNotBlank()) {
                        onLocationChange(lat, lon, tzText.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.apply_location))
            }
        }
    }
}
