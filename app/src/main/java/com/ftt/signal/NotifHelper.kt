package com.ftt.signal

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat

object NotifHelper {

    private const val CH_SIGNAL = "ftt_signal"
    private const val CH_SCAN   = "ftt_scan_fg"

    fun init(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Signal alerts channel
        NotificationChannel(CH_SIGNAL, "FTT Signals", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "BUY/SELL signal alerts"
            enableLights(true)
            lightColor = Color.GREEN
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 200, 100, 200)
            nm.createNotificationChannel(this)
        }

        // Foreground scan service channel (low importance, no sound)
        NotificationChannel(CH_SCAN, "Watchlist Scan", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Background watchlist scanner"
            nm.createNotificationChannel(this)
        }
    }

    /** BUY/SELL signal notification */
    fun show(context: Context, id: Int, title: String, body: String) {
        init(context)
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(context, CH_SIGNAL)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id, n)
    }

    /** Persistent notification for foreground scan service */
    fun foreground(context: Context, pairs: Int, interval: Int): Notification {
        init(context)
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CH_SCAN)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("FTT Watchlist Scan")
            .setContentText("$pairs pairs · every ${interval}m")
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
