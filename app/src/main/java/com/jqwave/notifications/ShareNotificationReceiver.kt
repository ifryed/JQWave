package com.jqwave.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jqwave.R

class ShareNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra(EXTRA_TEXT) ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(
            send,
            context.getString(R.string.share_chooser_title),
        )
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    companion object {
        const val EXTRA_TEXT = "share_text"
    }
}
