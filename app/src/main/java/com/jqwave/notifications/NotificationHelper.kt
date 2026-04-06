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
import com.jqwave.domain.JewishEventEvaluator
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone

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
        val nowMillis = System.currentTimeMillis()
        val text = when (kind) {
            EventKind.ROSH_HODESH -> context.getString(R.string.notify_body_rosh_hodesh)
            EventKind.SFIRAT_HAOMER -> {
                val geo = GeoLocation(
                    "user",
                    location.latitude,
                    location.longitude,
                    0.0,
                    TimeZone.getTimeZone(location.timeZoneId),
                )
                val zcal = ComplexZmanimCalendar(geo)
                val cal = zcal.getCalendar()
                cal.set(Calendar.YEAR, today.year)
                cal.set(Calendar.MONTH, today.monthValue - 1)
                cal.set(Calendar.DAY_OF_MONTH, today.dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 12)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val d = JewishEventEvaluator.dayOfOmerAtTrigger(
                    location.inIsrael,
                    today,
                    nowMillis,
                    zcal.getSunset()?.time,
                )
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
