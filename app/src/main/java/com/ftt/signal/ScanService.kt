package com.ftt.signal

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * Lightweight foreground service shown while the watchlist scanner is running.
 * The actual HTTP requests are made in the WebView JS — this service just
 * keeps the process alive and shows the persistent notification.
 */
class ScanService : Service() {

    companion object {
        private const val NOTIF_ID = 1
        private const val ACTION_STOP = "com.ftt.signal.STOP_SCAN"

        fun start(context: Context) {
            val intent = Intent(context, ScanService::class.java)
            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                android.util.Log.w("FTT/Scan", "Could not start scan service: ${e.message}")
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScanService::class.java))
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        // Partial wake lock — CPU runs but screen can turn off
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FTT:ScanWakeLock")
        wakeLock?.acquire(4 * 60 * 60 * 1000L) // max 4 hours
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification() = run {
        val stopIntent = Intent(this, ScanService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        NotificationCompat.Builder(this, FttApp.NOTIF_CHANNEL_SCAN)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FTT Watchlist Active")
            .setContentText("Scanning pairs for signals…")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPi)
            .addAction(0, "Stop", stopPi)
            .build()
    }

    override fun onDestroy() {
        wakeLock?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
