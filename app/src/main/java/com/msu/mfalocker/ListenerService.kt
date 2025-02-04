@file:Suppress("DEPRECATION")

package com.msu.mfalocker
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.io.File

class ListenerService : Service() {
    private lateinit var lockedAppList : ArrayList<String>
    private lateinit var lastApp: File

    private fun createNotificationChannel() {
        val channel = NotificationChannel("foreground", "Foreground Services", NotificationManager.IMPORTANCE_LOW).apply {
            description = "To inform the user that the app is running in the background."
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "foreground")
            .setContentTitle("Notification Listener")
            .setContentText("Monitoring notifications")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ForegroundServiceType", "HardwareIds")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)

        lockedAppList = ArrayList(File(filesDir, "locked.txt").readText().split(","))
        lastApp = File(filesDir, "lastApp.txt")
        if (!lastApp.exists()) lastApp.writeText("none")
        checkForegroundApp()
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()

        Handler().postDelayed({ checkForegroundApp() }, 500)
        val usageStatsList: List<UsageStats> = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000, currentTime)

        if (usageStatsList.isNotEmpty()) {
            val recentUsageStats = usageStatsList.maxByOrNull { it.lastTimeUsed }
            val pkg = recentUsageStats?.packageName

            if (recentUsageStats != null) {
                if (lockedAppList.contains(pkg)) {
                    if (lastApp.readText() != pkg) {
                        val intent = Intent(this, PinActivity::class.java).putExtra("packageName", pkg).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                        startActivity(intent)
                    }
                }
                else lastApp.writeText("none")
            }
            else lastApp.writeText("none")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSelf()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}