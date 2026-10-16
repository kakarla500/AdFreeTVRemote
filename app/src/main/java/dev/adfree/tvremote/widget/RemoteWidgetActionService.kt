package dev.adfree.tvremote.widget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.service.RemoteSessionManager

/**
 * Dispatches keypress actions triggered from the Home Screen App Widget
 * directly to the connected Android TV.
 */
class RemoteWidgetActionService : Service() {

    companion object {
        const val ACTION_SEND_KEY = "dev.adfree.tvremote.widget.ACTION_SEND_KEY"
        const val EXTRA_KEY_NAME = "extra_remote_key_name"

        fun dispatchKey(context: Context, key: RemoteKey) {
            val intent = Intent(context, RemoteWidgetActionService::class.java).apply {
                action = ACTION_SEND_KEY
                putExtra(EXTRA_KEY_NAME, key.name)
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SEND_KEY) {
            val keyName = intent.getStringExtra(EXTRA_KEY_NAME)
            if (keyName != null) {
                try {
                    val key = RemoteKey.valueOf(keyName)
                    val sessionManager = RemoteSessionManager.getInstance(this)
                    sessionManager.sendKey(key)
                } catch (_: Exception) {}
            }
        }
        return START_NOT_STICKY
    }
}

