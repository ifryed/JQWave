package com.jqwave.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jqwave.JQWaveApplication
import com.jqwave.data.EventConfigEntity
import com.jqwave.data.EventKind
import com.jqwave.data.LiturgyPreferences
import com.jqwave.data.LocationPreferences
import com.jqwave.data.NOTIFICATION_SOUND_SILENT
import com.jqwave.data.NotificationPreferences
import com.jqwave.data.effectiveNotificationChannelUri
import com.jqwave.data.OmerNusach
import com.jqwave.data.NotificationRule
import com.jqwave.data.UserLocation
import com.jqwave.data.toJson
import com.jqwave.data.toNotificationRules
import com.jqwave.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventUiState(
    val kind: EventKind,
    val enabled: Boolean,
    val rules: List<NotificationRule>,
)

class EventsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JQWaveApplication
    private val dao = app.database.eventConfigDao()
    private val scheduler = app.eventNotificationScheduler
    private val locationPreferences = app.locationPreferences
    private val liturgyPreferences: LiturgyPreferences = app.liturgyPreferences
    private val notificationPreferences: NotificationPreferences = app.notificationPreferences

    val eventRows = dao.observeAll()
        .map { entities -> entities.toUiStates() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val location = locationPreferences.locationFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            UserLocation(
                LocationPreferences.DEFAULT_LAT,
                LocationPreferences.DEFAULT_LON,
                LocationPreferences.DEFAULT_TZ,
                false,
                "",
            ),
        )

    val omerNusach = liturgyPreferences.omerNusachFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OmerNusach.ASHKENAZI)

    val defaultNotificationSoundStored = notificationPreferences.defaultSoundStoredFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null as String?)

    private fun List<EventConfigEntity>.toUiStates(): List<EventUiState> {
        val byKind = associateBy { it.kind }
        return EventKind.entries.map { kind ->
            val e = byKind[kind.storageKey]
            if (e == null) {
                EventUiState(kind, false, listOf(NotificationRule()))
            } else {
                val rules = runCatching { e.rulesJson.toNotificationRules() }.getOrElse { listOf(NotificationRule()) }
                EventUiState(kind, e.enabled, rules)
            }
        }
    }

    fun setEnabled(kind: EventKind, enabled: Boolean) {
        viewModelScope.launch {
            val e = dao.getByKind(kind.storageKey) ?: return@launch
            dao.upsert(e.copy(enabled = enabled))
            scheduler.rescheduleAll()
        }
    }

    fun saveRules(kind: EventKind, rules: List<NotificationRule>) {
        viewModelScope.launch {
            val e = dao.getByKind(kind.storageKey) ?: return@launch
            dao.upsert(e.copy(rulesJson = rules.toJson()))
            scheduler.rescheduleAll()
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, timeZoneId: String, displayLabel: String) {
        viewModelScope.launch {
            locationPreferences.update(latitude, longitude, timeZoneId, displayLabel)
            scheduler.rescheduleAll()
        }
    }

    fun setInIsrael(value: Boolean) {
        viewModelScope.launch {
            locationPreferences.setInIsrael(value)
            scheduler.rescheduleAll()
        }
    }

    fun setOmerNusach(value: OmerNusach) {
        viewModelScope.launch {
            liturgyPreferences.setOmerNusach(value)
        }
    }

    fun setDefaultNotificationSoundStored(value: String?) {
        viewModelScope.launch {
            notificationPreferences.setDefaultSoundStored(value)
        }
    }

    fun setRuleUseAppNotificationSound(kind: EventKind, rule: NotificationRule, useApp: Boolean) {
        viewModelScope.launch {
            val e = dao.getByKind(kind.storageKey) ?: return@launch
            val rules = runCatching { e.rulesJson.toNotificationRules() }.getOrElse { return@launch }
            val updated = rules.map { r ->
                if (r.id != rule.id) {
                    r
                } else if (useApp) {
                    r.copy(useAppNotificationSound = true, notificationSoundUri = null)
                } else {
                    val ctx = getApplication<Application>()
                    val default = notificationPreferences.defaultSoundStoredFlow.first()
                    val uri = effectiveNotificationChannelUri(ctx, default, null)
                    val snapshot = if (uri == null) {
                        NOTIFICATION_SOUND_SILENT
                    } else {
                        uri.toString()
                    }
                    r.copy(useAppNotificationSound = false, notificationSoundUri = snapshot)
                }
            }
            dao.upsert(e.copy(rulesJson = updated.toJson()))
            scheduler.rescheduleAll()
        }
    }

    fun patchRuleNotificationSoundOverride(kind: EventKind, ruleId: String, overrideUriString: String?) {
        viewModelScope.launch {
            val e = dao.getByKind(kind.storageKey) ?: return@launch
            val rules = runCatching { e.rulesJson.toNotificationRules() }.getOrElse { return@launch }
            val updated = rules.map { r ->
                if (r.id != ruleId) {
                    r
                } else {
                    r.copy(
                        useAppNotificationSound = false,
                        notificationSoundUri = overrideUriString,
                    )
                }
            }
            dao.upsert(e.copy(rulesJson = updated.toJson()))
            scheduler.rescheduleAll()
        }
    }

    fun testEventNotification(kind: EventKind, soundRule: NotificationRule?) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val nusach = liturgyPreferences.omerNusachFlow.first()
            val defaultSound = notificationPreferences.defaultSoundStoredFlow.first()
            NotificationHelper.showEventNotification(
                ctx,
                kind,
                location.value,
                nusach,
                defaultSound,
                soundRule = soundRule,
            )
        }
    }
}

class EventsViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventsViewModel::class.java)) {
            return EventsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
