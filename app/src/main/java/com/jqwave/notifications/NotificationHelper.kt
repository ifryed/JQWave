package com.jqwave.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jqwave.R
import com.jqwave.data.EventKind
import com.jqwave.data.NotificationRule
import com.jqwave.data.ShabbatSegment
import com.jqwave.data.UserLocation
import com.jqwave.domain.JewishEventEvaluator
import com.jqwave.domain.ShabbatPreview
import com.jqwave.domain.sunset
import com.kosherjava.zmanim.ComplexZmanimCalendar
import com.kosherjava.zmanim.util.GeoLocation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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

    fun showEventNotification(context: Context, kind: EventKind, location: UserLocation) {
        val (title, body) = notificationContent(context, kind, null, location)
        showWithShare(
            context = context,
            title = title,
            body = body,
            requestKey = "${kind.storageKey}|test|${System.currentTimeMillis()}",
        )
    }

    fun showForRule(context: Context, kind: EventKind, rule: NotificationRule, location: UserLocation) {
        val (title, body) = notificationContent(context, kind, rule, location)
        showWithShare(
            context = context,
            title = title,
            body = body,
            requestKey = "${kind.storageKey}|${rule.id}|${System.currentTimeMillis()}",
        )
    }

    private fun notificationContent(
        context: Context,
        kind: EventKind,
        rule: NotificationRule?,
        location: UserLocation,
    ): Pair<String, String> {
        val title = when {
            kind == EventKind.SHABBAT && rule?.shabbatSegment == ShabbatSegment.END ->
                context.getString(R.string.notify_title_shabbat_end)
            kind == EventKind.SHABBAT && rule?.shabbatSegment == ShabbatSegment.START ->
                context.getString(R.string.notify_title_shabbat_start)
            else -> context.getString(kind.notificationTitleRes)
        }
        val body = when (kind) {
            EventKind.ROSH_HODESH -> context.getString(R.string.notify_body_rosh_hodesh)
            EventKind.SFIRAT_HAOMER -> omerBody(context, location)
            EventKind.SHABBAT -> shabbatBody(context, rule, location)
        }
        return title to body
    }

    private fun shabbatBody(context: Context, rule: NotificationRule?, location: UserLocation): String {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
        val timeStr = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withZone(zone)
            .format(Instant.now())
        return when (rule?.shabbatSegment) {
            ShabbatSegment.START -> context.getString(R.string.notify_shabbat_start, timeStr)
            ShabbatSegment.END -> context.getString(R.string.notify_shabbat_end, timeStr)
            null -> {
                if (rule == null) {
                    val pair = ShabbatPreview.upcomingStartEndTimeLabels(location)
                    if (pair != null) {
                        context.getString(
                            R.string.notify_shabbat_test_both,
                            pair.first,
                            pair.second,
                        )
                    } else {
                        context.getString(R.string.notify_shabbat_generic)
                    }
                } else {
                    context.getString(R.string.notify_shabbat_at, timeStr)
                }
            }
        }
    }

    private fun omerBody(context: Context, location: UserLocation): String {
        val zone = runCatching { ZoneId.of(location.timeZoneId) }.getOrNull() ?: ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val nowMillis = System.currentTimeMillis()
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
            zcal.sunset?.time,
        )
        return if (d >= 1) {
            context.getString(R.string.notify_body_omer_day, d)
        } else {
            context.getString(R.string.notify_body_omer)
        }
    }

    private fun showWithShare(context: Context, title: String, body: String, requestKey: String) {
        val shareText = "$title\n$body"
        val sharePi = PendingIntent.getBroadcast(
            context,
            shareRequestCode(requestKey),
            Intent(context, ShareNotificationReceiver::class.java).putExtra(
                ShareNotificationReceiver.EXTRA_TEXT,
                shareText,
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_share,
                context.getString(R.string.notification_action_share),
                sharePi,
            )
            .build()
        val id = requestKey.hashCode()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun shareRequestCode(key: String): Int = 500_000 + (key.hashCode() and 0x7FFF_FFFF)
}
