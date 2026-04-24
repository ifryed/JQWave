package com.jqwave.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.notificationSoundDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_sound",
)

class NotificationPreferences(private val context: Context) {

    private object Keys {
        val defaultSound = stringPreferencesKey("default_notification_sound")
    }

    /**
     * Stored value: empty/absent = system default notification; [NOTIFICATION_SOUND_SILENT] = silent;
     * else a ringtone URI string.
     */
    val defaultSoundStoredFlow: Flow<String?> = context.notificationSoundDataStore.data.map { prefs ->
        prefs[Keys.defaultSound]
    }

    suspend fun setDefaultSoundStored(value: String?) {
        context.notificationSoundDataStore.edit { prefs ->
            if (value == null) {
                prefs.remove(Keys.defaultSound)
            } else {
                prefs[Keys.defaultSound] = value
            }
        }
    }
}
