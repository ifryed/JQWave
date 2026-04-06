package com.jqwave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.locationDataStore: DataStore<Preferences> by preferencesDataStore("location")

class LocationPreferences(private val context: Context) {

    private object Keys {
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val timeZoneId = stringPreferencesKey("time_zone_id")
        val inIsrael = booleanPreferencesKey("in_israel")
    }

    suspend fun currentLocation(): UserLocation = locationFlow.first()

    val locationFlow: Flow<UserLocation> = context.locationDataStore.data.map { prefs ->
        val lat = prefs[Keys.latitude] ?: DEFAULT_LAT
        val lon = prefs[Keys.longitude] ?: DEFAULT_LON
        val tz = prefs[Keys.timeZoneId] ?: DEFAULT_TZ
        val israel = prefs[Keys.inIsrael] ?: false
        UserLocation(lat, lon, tz, israel)
    }

    suspend fun update(latitude: Double, longitude: Double, timeZoneId: String) {
        context.locationDataStore.edit { prefs ->
            prefs[Keys.latitude] = latitude
            prefs[Keys.longitude] = longitude
            prefs[Keys.timeZoneId] = timeZoneId
        }
    }

    suspend fun setInIsrael(value: Boolean) {
        context.locationDataStore.edit { it[Keys.inIsrael] = value }
    }

    companion object {
        const val DEFAULT_LAT = 31.776713
        const val DEFAULT_LON = 35.234092
        const val DEFAULT_TZ = "Asia/Jerusalem"
    }
}

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val inIsrael: Boolean,
)

