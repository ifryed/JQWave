package com.jqwave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.scheduledAlarmsDataStore: DataStore<Preferences> by preferencesDataStore("scheduled_alarms")

class ScheduledAlarmsStore(context: Context) {

    private val ds = context.scheduledAlarmsDataStore
    private val key = stringSetPreferencesKey("tokens")

    suspend fun getTokens(): Set<String> = ds.data.map { it[key] ?: emptySet() }.first()

    suspend fun setTokens(tokens: Set<String>) {
        ds.edit { it[key] = tokens }
    }
}
