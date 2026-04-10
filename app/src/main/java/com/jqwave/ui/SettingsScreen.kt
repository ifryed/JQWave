@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.jqwave.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.location.Geocoder
import com.jqwave.R
import com.jqwave.data.OmerNusach
import com.jqwave.data.UserLocation
import com.jqwave.location.GeocodedCity
import com.jqwave.location.GeocodingHelper
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    location: UserLocation,
    omerNusach: OmerNusach,
    onOmerNusachChange: (OmerNusach) -> Unit,
    onBack: () -> Unit,
    onUpdateLocationFromDevice: () -> Unit,
    onCityChosen: (label: String, latitude: Double, longitude: Double, timeZoneId: String) -> Unit,
    onInIsraelChange: (Boolean) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GeocodedCity>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val currentLabel = if (location.displayLabel.isNotBlank()) {
        location.displayLabel
    } else {
        stringResource(R.string.settings_location_default_label)
    }

    Scaffold(
        containerColor = scheme.background,
        contentColor = scheme.onBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        color = scheme.onPrimaryContainer,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = scheme.onPrimaryContainer,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = scheme.primaryContainer,
                    titleContentColor = scheme.onPrimaryContainer,
                    navigationIconContentColor = scheme.onPrimaryContainer,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.location_section),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.settings_current_location_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        currentLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                    )
                    Button(
                        onClick = onUpdateLocationFromDevice,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_update_location))
                    }
                    Text(
                        stringResource(R.string.settings_location_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.settings_search_city_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(stringResource(R.string.settings_search_city_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                isSearching = true
                                searchResults = emptyList()
                                try {
                                    if (!Geocoder.isPresent()) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.settings_geocoder_unavailable),
                                        )
                                        return@launch
                                    }
                                    val results = GeocodingHelper.searchCities(
                                        context,
                                        searchQuery.trim(),
                                    )
                                    searchResults = results
                                    if (results.isEmpty()) {
                                        snackbarHostState.showSnackbar(
                                            context.getString(R.string.settings_no_results),
                                        )
                                    }
                                } finally {
                                    isSearching = false
                                }
                            }
                        },
                        enabled = searchQuery.isNotBlank() && !isSearching,
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.settings_search))
                    }
                    searchResults.forEach { city ->
                        Text(
                            city.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = scheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCityChosen(city.label, city.latitude, city.longitude, city.timeZoneId)
                                    searchResults = emptyList()
                                    searchQuery = ""
                                }
                                .padding(vertical = 8.dp),
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.in_israel_calendar),
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = location.inIsrael,
                        onCheckedChange = onInIsraelChange,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = scheme.surfaceContainerLow,
                    contentColor = scheme.onSurface,
                ),
            ) {
                Column(
                    Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        stringResource(R.string.settings_omer_nusach_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                    )
                    Text(
                        stringResource(R.string.settings_omer_nusach_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = scheme.surface,
                        labelColor = scheme.onSurface,
                        selectedContainerColor = scheme.primaryContainer,
                        selectedLabelColor = scheme.onPrimaryContainer,
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = omerNusach == OmerNusach.ASHKENAZI,
                            onClick = { onOmerNusachChange(OmerNusach.ASHKENAZI) },
                            label = {
                                Text(
                                    stringResource(R.string.settings_omer_nusach_ashkenazi),
                                    maxLines = 1,
                                )
                            },
                            colors = chipColors,
                        )
                        FilterChip(
                            selected = omerNusach == OmerNusach.SEPHARADI,
                            onClick = { onOmerNusachChange(OmerNusach.SEPHARADI) },
                            label = {
                                Text(
                                    stringResource(R.string.settings_omer_nusach_sepharadi),
                                    maxLines = 1,
                                )
                            },
                            colors = chipColors,
                        )
                        FilterChip(
                            selected = omerNusach == OmerNusach.YEMENITE,
                            onClick = { onOmerNusachChange(OmerNusach.YEMENITE) },
                            label = {
                                Text(
                                    stringResource(R.string.settings_omer_nusach_yemenite),
                                    maxLines = 1,
                                )
                            },
                            colors = chipColors,
                        )
                    }
                }
            }
        }
    }
}
