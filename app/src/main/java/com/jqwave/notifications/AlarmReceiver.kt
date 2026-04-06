package com.jqwave.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jqwave.JQWaveApplication
import com.jqwave.data.EventKind
import com.jqwave.data.toNotificationRules
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kindKey = intent.getStringExtra(EXTRA_KIND) ?: return
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID) ?: return
        val kind = EventKind.entries.find { it.storageKey == kindKey } ?: return
        val app = context.applicationContext as JQWaveApplication
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                val loc = app.locationPreferences.currentLocation()
                val entity = app.database.eventConfigDao().getByKind(kind.storageKey)
                val rule = entity?.let { e ->
                    runCatching { e.rulesJson.toNotificationRules() }.getOrNull()
                        ?.find { it.id == ruleId }
                }
                if (rule != null) {
                    NotificationHelper.showForRule(context, kind, rule, loc)
                } else {
                    NotificationHelper.showEventNotification(context, kind, loc)
                }
                app.eventNotificationScheduler.rescheduleAfterAlarm(kind, ruleId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_RULE_ID = "rule_id"
    }
}
