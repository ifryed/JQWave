package com.jqwave.notifications

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.jqwave.R

/**
 * Launches the system share sheet. Used as the notification action target because
 * Android 12+ blocks starting activities from a [android.content.BroadcastReceiver]
 * triggered by a notification (trampoline restriction).
 */
class ShareNotificationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra(EXTRA_TEXT)
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(send, getString(R.string.share_chooser_title))
        startActivity(chooser)
        finish()
    }

    companion object {
        const val EXTRA_TEXT = "share_text"
    }
}
