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
import com.jqwave.data.OmerNusach
import com.jqwave.data.ShabbatSegment
import com.jqwave.data.UserLocation
import com.jqwave.domain.OmerLiturgy
import com.jqwave.domain.ShabbatPreview
import com.jqwave.domain.jewishCalendarAtTrigger
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
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
        omerNusach: OmerNusach,
    ) {
        val (title, body) = notificationContent(context, kind, null, location, omerNusach)
        showWithShare(
            context = context,
            title = title,
            body = body,
            requestKey = "${kind.storageKey}|test|${System.currentTimeMillis()}",
        )
    }

    fun showForRule(
        context: Context,
        kind: EventKind,
        rule: NotificationRule,
        location: UserLocation,
        omerNusach: OmerNusach,
    ) {
        val (title, body) = notificationContent(context, kind, rule, location, omerNusach)
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
        omerNusach: OmerNusach,
    ): Pair<String, String> {
        val title = when {
            kind == EventKind.SHABBAT && rule?.shabbatSegment == ShabbatSegment.END ->
                context.getString(R.string.notify_title_shabbat_end)
            kind == EventKind.SHABBAT && rule?.shabbatSegment == ShabbatSegment.START ->
                context.getString(R.string.notify_title_shabbat_start)
            else -> context.getString(kind.notificationTitleRes)
        }
        val body = when (kind) {
            EventKind.ROSH_HODESH -> roshChodeshBody(context, location)
            EventKind.SFIRAT_HAOMER -> omerBody(context, location, omerNusach)
            EventKind.SHABBAT -> shabbatBody(context, location)
        }
        return title to body
    }

    private fun isHebrewJewishDateFormatting(context: Context): Boolean {
        val lang = context.resources.configuration.locales.get(0)?.language ?: return false
        return lang == "iw" || lang == "he"
    }

    private fun roshChodeshBody(context: Context, location: UserLocation): String {
        val jc = jewishCalendarAtTrigger(location, System.currentTimeMillis())
        val formatter = HebrewDateFormatter().apply {
            setHebrewFormat(isHebrewJewishDateFormatting(context))
        }
        val line = formatter.formatRoshChodesh(jc)
        return line.ifBlank { context.getString(R.string.notify_body_rosh_hodesh) }
    }

    private fun shabbatBody(context: Context, location: UserLocation): String {
        val pair = ShabbatPreview.upcomingStartEndTimeLabels(location)
        return if (pair != null) {
            context.getString(R.string.notify_shabbat_start_and_end, pair.first, pair.second)
        } else {
            context.getString(R.string.notify_shabbat_generic)
        }
    }

    private fun omerBody(context: Context, location: UserLocation, omerNusach: OmerNusach): String {
        val nowMillis = System.currentTimeMillis()
        val jc = jewishCalendarAtTrigger(location, nowMillis)
        val d = jc.getDayOfOmer()
        return OmerLiturgy.notificationBody(context, d, omerNusach)
    }

    private fun showWithShare(context: Context, title: String, body: String, requestKey: String) {
        val shareText = "$title\n$body${context.getString(R.string.share_text_attribution)}"
        val sharePi = PendingIntent.getActivity(
            context,
            shareRequestCode(requestKey),
            Intent(context, ShareNotificationActivity::class.java).putExtra(
                ShareNotificationActivity.EXTRA_TEXT,
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
