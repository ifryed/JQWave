package com.jqwave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.liturgyDataStore: DataStore<Preferences> by preferencesDataStore("liturgy")

class LiturgyPreferences(private val context: Context) {

    private object Keys {
        val omerNusach = stringPreferencesKey("omer_nusach")
    }

    val omerNusachFlow: Flow<OmerNusach> = context.liturgyDataStore.data.map { prefs ->
        OmerNusach.fromStorageKey(prefs[Keys.omerNusach])
    }

    suspend fun setOmerNusach(value: OmerNusach) {
        context.liturgyDataStore.edit { it[Keys.omerNusach] = value.storageKey }
    }
}
