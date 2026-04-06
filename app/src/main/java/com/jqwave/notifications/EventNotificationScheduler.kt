package com.jqwave.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jqwave.data.EventConfigDao
import com.jqwave.data.EventKind
import com.jqwave.data.LocationPreferences
import com.jqwave.data.ScheduledAlarmsStore
import com.jqwave.data.toNotificationRules
import com.jqwave.MainActivity
import com.jqwave.domain.NextTriggerCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class EventNotificationScheduler(
    private val context: Context,
    private val dao: EventConfigDao,
    private val locationPreferences: LocationPreferences,
    private val scheduledAlarmsStore: ScheduledAlarmsStore,
) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() = withContext(Dispatchers.Default) {
        val location = locationPreferences.locationFlow.first()
        val oldTokens = scheduledAlarmsStore.getTokens()
        val configs = dao.getAll().associateBy { it.kind }
        val newTokens = mutableSetOf<String>()

        for (kind in EventKind.entries) {
            val entity = configs[kind.storageKey] ?: continue
            if (!entity.enabled) continue
            val rules = runCatching { entity.rulesJson.toNotificationRules() }.getOrElse { emptyList() }
            for (rule in rules) {
                val token = alarmToken(kind, rule.id)
                newTokens.add(token)
                val next = NextTriggerCalculator.nextTriggerMillis(kind, rule, location) ?: continue
                scheduleNextAlarm(kind, rule.id, next)
            }
        }

        for (token in oldTokens - newTokens) {
            cancelByToken(token)
        }
        scheduledAlarmsStore.setTokens(newTokens)
    }

    suspend fun rescheduleAfterAlarm(kind: EventKind, ruleId: String) = withContext(Dispatchers.Default) {
        val location = locationPreferences.locationFlow.first()
        val entity = dao.getAll().find { it.kind == kind.storageKey } ?: return@withContext
        if (!entity.enabled) return@withContext
        val rules = runCatching { entity.rulesJson.toNotificationRules() }.getOrElse { emptyList() }
        val rule = rules.find { it.id == ruleId } ?: return@withContext
        val next = NextTriggerCalculator.nextTriggerMillis(kind, rule, location) ?: return@withContext
        scheduleNextAlarm(kind, ruleId, next)
        val tokens = scheduledAlarmsStore.getTokens().toMutableSet()
        tokens.add(alarmToken(kind, ruleId))
        scheduledAlarmsStore.setTokens(tokens)
    }

    private fun scheduleNextAlarm(kind: EventKind, ruleId: String, triggerMillis: Long) {
        val operation = alarmPendingIntent(kind, ruleId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                setAlarmClock(kind, ruleId, triggerMillis, operation)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
            }
        } else {
            setAlarmClock(kind, ruleId, triggerMillis, operation)
        }
    }

    private fun setAlarmClock(kind: EventKind, ruleId: String, triggerMillis: Long, operation: PendingIntent) {
        val show = PendingIntent.getActivity(
            context,
            SHOW_PI_REQUEST_BASE + requestCode(kind, ruleId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerMillis, show)
        try {
            alarmManager.setAlarmClock(info, operation)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, operation)
        }
    }

    private fun cancelByToken(token: String) {
        val parts = token.split("|", limit = 2)
        if (parts.size != 2) return
        val kind = EventKind.entries.find { it.storageKey == parts[0] } ?: return
        val ruleId = parts[1]
        val pi = alarmPendingIntent(kind, ruleId)
        alarmManager.cancel(pi)
    }

    private fun alarmPendingIntent(kind: EventKind, ruleId: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_KIND, kind.storageKey)
            putExtra(AlarmReceiver.EXTRA_RULE_ID, ruleId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(kind, ruleId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        fun alarmToken(kind: EventKind, ruleId: String) = "${kind.storageKey}|$ruleId"

        fun requestCode(kind: EventKind, ruleId: String): Int =
            31 * (31 + kind.ordinal) + ruleId.hashCode()

        private const val SHOW_PI_REQUEST_BASE = 1_000_000
    }
}
