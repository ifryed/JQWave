package com.jqwave

import android.app.Application
import com.jqwave.data.AppDatabase
import com.jqwave.data.DefaultEventRules
import com.jqwave.data.EventConfigEntity
import com.jqwave.data.EventKind
import com.jqwave.data.LocationPreferences
import com.jqwave.data.NotificationRule
import com.jqwave.data.ScheduledAlarmsStore
import com.jqwave.data.toJson
import com.jqwave.location.LocationRefreshScheduler
import com.jqwave.notifications.EventNotificationScheduler
import com.jqwave.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JQWaveApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var locationPreferences: LocationPreferences
        private set
    lateinit var eventNotificationScheduler: EventNotificationScheduler
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        locationPreferences = LocationPreferences(this)
        val scheduledStore = ScheduledAlarmsStore(this)
        eventNotificationScheduler = EventNotificationScheduler(
            this,
            database.eventConfigDao(),
            locationPreferences,
            scheduledStore,
        )
        NotificationHelper.ensureChannel(this)
        LocationRefreshScheduler.schedulePeriodic(this)
        applicationScope.launch {
            seedIfEmpty()
            eventNotificationScheduler.rescheduleAll()
        }
    }

    private suspend fun seedIfEmpty() {
        val dao = database.eventConfigDao()
        if (dao.getAll().isNotEmpty()) return
        for (kind in EventKind.entries) {
            val rules = if (kind == EventKind.SHABBAT) {
                DefaultEventRules.shabbatRules
            } else {
                listOf(NotificationRule())
            }
            dao.upsert(
                EventConfigEntity(
                    kind = kind.storageKey,
                    enabled = false,
                    rulesJson = rules.toJson(),
                ),
            )
        }
    }
}
