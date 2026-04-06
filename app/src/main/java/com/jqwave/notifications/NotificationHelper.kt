package com.jqwave.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jqwave.R
import com.jqwave.data.EventKind
import com.jqwave.data.UserLocation
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.time.LocalDate
import java.time.ZoneId

object NotificationHelper {

    const val CHANNEL_ID = "jewish_events"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        mgr.createNotificationChannel(channel)
    }

    fun showEventNotification(
        context: Context,
        kind: EventKind,
        location: UserLocation,
    ) {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val jc = JewishCalendar().apply {
            setInIsrael(location.inIsrael)
            setGregorianDate(today.year, today.monthValue, today.dayOfMonth)
        }
        val text = when (kind) {
            EventKind.ROSH_HODESH -> context.getString(R.string.notify_body_rosh_hodesh)
            EventKind.SFIRAT_HAOMER -> {
                val d = jc.getDayOfOmer()
                if (d >= 1) {
                    context.getString(R.string.notify_body_omer_day, d)
                } else {
                    context.getString(R.string.notify_body_omer)
                }
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(kind.notificationTitle)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        val id = ("${kind.storageKey}:${System.currentTimeMillis()}").hashCode()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
