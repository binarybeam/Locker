package com.msu.mfalocker

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val notificationListenerComponent = ComponentName(context.packageName, ListenerService::class.java.name)
            val isNotificationListenerEnabled = NotificationManagerCompat.getEnabledListenerPackages(context).contains(notificationListenerComponent.packageName)

            if (!isNotificationListenerEnabled) return
            val serviceIntent = Intent(context, ListenerService::class.java)
            context.startService(serviceIntent)
        }
    }
}