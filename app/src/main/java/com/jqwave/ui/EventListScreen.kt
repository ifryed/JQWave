@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jqwave.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jqwave.R
import com.jqwave.data.EventKind
import com.jqwave.data.NotificationRule
import com.jqwave.data.ShabbatSegment
import com.jqwave.data.TimeAnchor

private val EventCardLightGreen = Color(0xFFF2FAF6)

@Composable
private fun EventKindCardIcon(
    kind: EventKind,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val desc = stringResource(kind.displayNameRes)
    when (kind) {
        EventKind.SHABBAT -> Icon(
            painter = painterResource(R.drawable.ic_event_shabbat_candles),
            contentDescription = desc,
            tint = tint,
            modifier = modifier,
        )
        EventKind.ROSH_HODESH -> Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = desc,
            tint = tint,
            modifier = modifier,
        )
        EventKind.SFIRAT_HAOMER -> Icon(
            imageVector = Icons.Filled.FormatListNumbered,
            contentDescription = desc,
            tint = tint,
            modifier = modifier,
        )
    }
}

@Composable
private fun languageToggleLabel(): String {
    val locales = LocalConfiguration.current.locales
    val lang = if (locales.size() == 0) "" else locales[0].language
    val isHebrew = lang == "iw" || lang == "he"
    return if (isHebrew) "En" else "ע"
}

@Composable
fun EventListScreen(
    rows: List<EventUiState>,
    onEnabledChange: (EventKind, Boolean) -> Unit,
    onRulesChange: (EventKind, List<NotificationRule>) -> Unit,
    onTestEventNotification: (EventKind) -> Unit,
    onLanguageToggle: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val langLabel = languageToggleLabel()
    var menuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = scheme.background,
        contentColor = scheme.onBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_title),
                        color = scheme.onPrimaryContainer,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.menu_more_options),
                                tint = scheme.onPrimaryContainer,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings_menu_settings)) },
                                onClick = {
                                    menuExpanded = false
                                    onOpenSettings()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        stringResource(
                                            R.string.settings_menu_language_format,
                                            langLabel,
                                        ),
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onLanguageToggle()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.primaryContainer,
                    titleContentColor = scheme.onPrimaryContainer,
                    actionIconContentColor = scheme.onPrimaryContainer,
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
                    onTestNotification = { onTestEventNotification(row.kind) },
                )
            }
        }
    }
}

@Composable
private fun EventCard(
    row: EventUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRulesChange: (List<NotificationRule>) -> Unit,
    onTestNotification: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var expanded by remember(row.kind) { mutableStateOf(false) }
    val expandCollapseLabel = stringResource(
        if (expanded) R.string.event_card_minimize else R.string.event_card_expand,
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = EventCardLightGreen,
            contentColor = scheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(
                            onClickLabel = expandCollapseLabel,
                            role = Role.Button,
                            onClick = { expanded = !expanded },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        EventKindCardIcon(
                            kind = row.kind,
                            tint = scheme.primary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(
                            stringResource(row.kind.displayNameRes),
                            style = MaterialTheme.typography.titleMedium,
                            color = scheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = scheme.onSurface,
                    )
                }
                Switch(checked = row.enabled, onCheckedChange = onEnabledChange)
            }
            if (expanded) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onTestNotification) {
                        Text(stringResource(R.string.button_test))
                    }
                }
            }
            if (expanded && row.enabled) {
                Spacer(Modifier.height(12.dp))
                row.rules.forEach { rule ->
                    RuleRow(
                        eventKind = row.kind,
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
                            val newRule = if (row.kind == EventKind.SHABBAT) {
                                when {
                                    row.rules.none { it.shabbatSegment == ShabbatSegment.START } ->
                                        NotificationRule(
                                            shabbatSegment = ShabbatSegment.START,
                                            anchor = TimeAnchor.SUNSET,
                                            offsetMinutes = -18,
                                        )
                                    row.rules.none { it.shabbatSegment == ShabbatSegment.END } ->
                                        NotificationRule(
                                            shabbatSegment = ShabbatSegment.END,
                                            anchor = TimeAnchor.TZAIT,
                                            offsetMinutes = 0,
                                        )
                                    else ->
                                        NotificationRule(
                                            shabbatSegment = ShabbatSegment.START,
                                            anchor = TimeAnchor.SUNSET,
                                            offsetMinutes = -18,
                                        )
                                }
                            } else {
                                NotificationRule()
                            }
                            onRulesChange(row.rules + newRule)
                        },
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = scheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleRow(
    eventKind: EventKind,
    rule: NotificationRule,
    showRemove: Boolean,
    onRuleChange: (NotificationRule) -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val latestRule by rememberUpdatedState(rule)
    val anchorChipColors = FilterChipDefaults.filterChipColors(
        containerColor = scheme.surface,
        labelColor = scheme.onSurface,
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
    )
    val segment = rule.shabbatSegment ?: ShabbatSegment.START
    fun applySegment(newSeg: ShabbatSegment) {
        var r = rule.copy(shabbatSegment = newSeg)
        when (newSeg) {
            ShabbatSegment.END -> {
                if (r.anchor != TimeAnchor.TZAIT && r.anchor != TimeAnchor.CLOCK) {
                    r = r.copy(anchor = TimeAnchor.TZAIT, offsetMinutes = 0)
                }
            }
            ShabbatSegment.START -> {
                if (r.anchor == TimeAnchor.TZAIT) {
                    r = r.copy(anchor = TimeAnchor.SUNSET, offsetMinutes = -18)
                }
            }
        }
        onRuleChange(r)
    }
    val anchors = buildList {
        if (eventKind == EventKind.SHABBAT && segment == ShabbatSegment.END) {
            add(TimeAnchor.TZAIT to stringResource(R.string.anchor_tzait_chip))
            add(TimeAnchor.CLOCK to stringResource(R.string.anchor_clock_chip))
        } else {
            add(TimeAnchor.CLOCK to stringResource(R.string.anchor_clock_chip))
            add(TimeAnchor.SUNRISE to stringResource(R.string.anchor_sunrise_chip))
            add(TimeAnchor.SUNSET to stringResource(R.string.anchor_sunset_chip))
        }
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = scheme.surfaceContainerHigh,
            contentColor = scheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            if (eventKind == EventKind.SHABBAT) {
                Text(
                    stringResource(R.string.shabbat_segment_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurface,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterChip(
                        selected = segment == ShabbatSegment.START,
                        onClick = { applySegment(ShabbatSegment.START) },
                        label = {
                            Text(
                                stringResource(R.string.shabbat_segment_start),
                                maxLines = 1,
                                color = if (segment == ShabbatSegment.START) {
                                    scheme.onPrimaryContainer
                                } else {
                                    scheme.onSurface
                                },
                            )
                        },
                        colors = anchorChipColors,
                    )
                    FilterChip(
                        selected = segment == ShabbatSegment.END,
                        onClick = { applySegment(ShabbatSegment.END) },
                        label = {
                            Text(
                                stringResource(R.string.shabbat_segment_end),
                                maxLines = 1,
                                color = if (segment == ShabbatSegment.END) {
                                    scheme.onPrimaryContainer
                                } else {
                                    scheme.onSurface
                                },
                            )
                        },
                        colors = anchorChipColors,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.anchor_when_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        anchors.forEach { (anchor, label) ->
                            val selected = rule.anchor == anchor
                            FilterChip(
                                selected = selected,
                                onClick = { onRuleChange(rule.copy(anchor = anchor)) },
                                label = {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        color = if (selected) {
                                            scheme.onPrimaryContainer
                                        } else {
                                            scheme.onSurface
                                        },
                                    )
                                },
                                colors = anchorChipColors,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                if (showRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = scheme.onSurface,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            key(rule.id, rule.anchor, rule.shabbatSegment) {
                when (rule.anchor) {
                    TimeAnchor.CLOCK -> {
                        Column(
                            Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                stringResource(R.string.local_time_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onSurface,
                            )
                            Spacer(Modifier.height(4.dp))
                            HourMinuteWheelRow(
                                hour = rule.hour,
                                minute = rule.minute,
                                onHourChange = { onRuleChange(latestRule.copy(hour = it)) },
                                onMinuteChange = { onRuleChange(latestRule.copy(minute = it)) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    TimeAnchor.SUNRISE, TimeAnchor.SUNSET, TimeAnchor.TZAIT -> {
                        RelativeOffsetPickers(
                            offsetMinutes = rule.offsetMinutes,
                            onOffsetChange = { onRuleChange(latestRule.copy(offsetMinutes = it)) },
                            beforeText = stringResource(R.string.offset_before),
                            afterText = stringResource(R.string.offset_after),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

