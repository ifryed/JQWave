package com.jqwave.ui

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import com.jqwave.data.NOTIFICATION_SOUND_SILENT

object RingtonePickerHelper {

    /** MIME types for [androidx.activity.result.contract.ActivityResultContracts.OpenDocument]. */
    val notificationAudioOpenDocumentMimeTypes: Array<String> = arrayOf(
        "audio/*",
        "application/ogg",
    )

    fun buildNotificationPickIntent(existing: Uri?): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existing)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        }

    /**
     * @return null if the user cancelled; otherwise a URI string or [NOTIFICATION_SOUND_SILENT].
     */
    fun parsePickResult(result: ActivityResult): String? {
        if (result.resultCode != Activity.RESULT_OK) return null
        val uri = result.data?.let { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri?
            }
        }
        return if (uri == null) {
            NOTIFICATION_SOUND_SILENT
        } else {
            uri.toString()
        }
    }

    fun takePersistableReadIfNeeded(activity: ComponentActivity, result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val uri = result.data?.let { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                    Uri::class.java,
                )
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri?
            }
        } ?: return
        val flags = result.data?.flags ?: return
        if (flags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) return
        if (flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) return
        runCatching {
            activity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    /** After [androidx.activity.result.contract.ActivityResultContracts.OpenDocument] returns a URI. */
    fun takePersistableReadForPickedDocument(activity: ComponentActivity, uri: Uri) {
        runCatching {
            activity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }
}
