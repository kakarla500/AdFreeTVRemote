package dev.adfree.tvremote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import dev.adfree.tvremote.MainActivity
import dev.adfree.tvremote.R
import dev.adfree.tvremote.model.ConnectionState
import dev.adfree.tvremote.model.RemoteKey
import dev.adfree.tvremote.service.RemoteSessionManager

class RemoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val sessionManager = RemoteSessionManager.getInstance(context)
        val device = sessionManager.activeDevice.value
        val state = sessionManager.connectionState.value

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_remote)

            // Device Name / Status
            val statusText = when (state) {
                is ConnectionState.Connected -> "● ${device?.name ?: "Connected"}"
                is ConnectionState.Connecting -> "○ Connecting…"
                else -> device?.name ?: "Tap to Connect TV"
            }
            views.setTextViewText(R.id.widget_tv_title, statusText)

            // Open App on Title Click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_tv_title, openAppPendingIntent)

            // Bind Key Actions
            views.setOnClickPendingIntent(R.id.widget_btn_power, createKeyPendingIntent(context, RemoteKey.POWER, 1))
            views.setOnClickPendingIntent(R.id.widget_btn_up, createKeyPendingIntent(context, RemoteKey.DPAD_UP, 2))
            views.setOnClickPendingIntent(R.id.widget_btn_down, createKeyPendingIntent(context, RemoteKey.DPAD_DOWN, 3))
            views.setOnClickPendingIntent(R.id.widget_btn_left, createKeyPendingIntent(context, RemoteKey.DPAD_LEFT, 4))
            views.setOnClickPendingIntent(R.id.widget_btn_right, createKeyPendingIntent(context, RemoteKey.DPAD_RIGHT, 5))
            views.setOnClickPendingIntent(R.id.widget_btn_select, createKeyPendingIntent(context, RemoteKey.DPAD_CENTER, 6))
            views.setOnClickPendingIntent(R.id.widget_btn_back, createKeyPendingIntent(context, RemoteKey.BACK, 7))
            views.setOnClickPendingIntent(R.id.widget_btn_home, createKeyPendingIntent(context, RemoteKey.HOME, 8))
            views.setOnClickPendingIntent(R.id.widget_btn_vol_down, createKeyPendingIntent(context, RemoteKey.VOLUME_DOWN, 9))
            views.setOnClickPendingIntent(R.id.widget_btn_vol_up, createKeyPendingIntent(context, RemoteKey.VOLUME_UP, 10))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createKeyPendingIntent(context: Context, key: RemoteKey, requestCode: Int): PendingIntent {
        val intent = Intent(context, RemoteWidgetActionService::class.java).apply {
            action = RemoteWidgetActionService.ACTION_SEND_KEY
            putExtra(RemoteWidgetActionService.EXTRA_KEY_NAME, key.name)
        }
        return PendingIntent.getService(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

