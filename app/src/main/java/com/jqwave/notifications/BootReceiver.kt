package com.jqwave.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jqwave.JQWaveApplication
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as JQWaveApplication
        val pendingResult = goAsync()
        app.applicationScope.launch {
            try {
                app.eventNotificationScheduler.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
