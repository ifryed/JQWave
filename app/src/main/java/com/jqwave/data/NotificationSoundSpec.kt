package com.jqwave.data

import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import com.jqwave.R
import java.util.UUID

/** Persisted string for silent notification (distinct from empty = system default). */
const val NOTIFICATION_SOUND_SILENT = "__silent__"

private val notificationAudioAttributes: AudioAttributes =
    AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

/**
 * Resolves a stored preference or override string to the URI used on the notification channel.
 * @return null for silent; non-null for playable sound (including system default notification).
 */
fun resolveStoredNotificationSound(context: Context, stored: String?): Uri? {
    if (stored == null || stored.isEmpty()) {
        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
    }
    if (stored == NOTIFICATION_SOUND_SILENT) return null
    return runCatching { Uri.parse(stored) }.getOrNull()
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
}

/**
 * Effective channel sound URI for a rule given app-wide [defaultStored] and optional [rule].
 */
fun effectiveNotificationChannelUri(
    context: Context,
    defaultStored: String?,
    rule: NotificationRule?,
): Uri? {
    if (rule == null || rule.useAppNotificationSound) {
        return resolveStoredNotificationSound(context, defaultStored)
    }
    val override = rule.notificationSoundUri
    if (override == null || override.isEmpty()) return null
    if (override == NOTIFICATION_SOUND_SILENT) return null
    return runCatching { Uri.parse(override) }.getOrNull()
        ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
}

/** Stable channel id for this sound fingerprint (Android O+ channels cannot change sound reliably). */
fun notificationChannelIdForFingerprint(fingerprint: String): String =
    "jewish_events_" + UUID.nameUUIDFromBytes(fingerprint.toByteArray(Charsets.UTF_8)).toString()

fun notificationChannelFingerprint(defaultStored: String?, rule: NotificationRule?): String =
    buildString {
        if (rule == null || rule.useAppNotificationSound) {
            append("app:")
            append(defaultStored ?: "")
        } else {
            append("rule:")
            append(rule.notificationSoundUri ?: "")
        }
    }

fun notificationChannelAudioAttributes(): AudioAttributes = notificationAudioAttributes

fun notificationSoundTitle(context: Context, stored: String?): String = when {
    stored == NOTIFICATION_SOUND_SILENT ->
        context.getString(R.string.notification_sound_silent)
    stored.isNullOrEmpty() ->
        context.getString(R.string.notification_sound_system_default)
    else -> {
        val uri = runCatching { Uri.parse(stored) }.getOrNull()
        if (uri != null) {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
                ?: context.getString(R.string.notification_sound_custom)
        } else {
            context.getString(R.string.notification_sound_custom)
        }
    }
}
