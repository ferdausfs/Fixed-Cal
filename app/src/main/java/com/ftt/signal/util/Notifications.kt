package com.ftt.signal.util

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ftt.signal.MainActivity
import com.ftt.signal.R

object NotificationHelper {
    const val SIGNAL_CHANNEL = "ftt_signals"
    const val SCAN_CHANNEL = "ftt_scan"
    const val SCAN_NOTIFICATION_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                SIGNAL_CHANNEL,
                context.getString(R.string.signal_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.signal_channel_desc)
                enableVibration(true)
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SCAN_CHANNEL,
                context.getString(R.string.scan_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.scan_channel_desc)
            }
        )
    }

    fun ongoingScanNotification(context: Context, body: String): Notification =
        NotificationCompat.Builder(context, SCAN_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("FTT watchlist scanner")
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

    fun sendSignalNotification(context: Context, title: String, body: String, id: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val notif = NotificationCompat.Builder(context, SIGNAL_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(id, notif)
    }
}
