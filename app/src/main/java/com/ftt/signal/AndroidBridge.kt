package com.ftt.signal

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.atomic.AtomicInteger

/**
 * JavascriptInterface that the HTML expects as window.AndroidBridge.
 *
 * Methods called from JS:
 *   AndroidBridge.getApiBase()            → String
 *   AndroidBridge.vibrate(ms)
 *   AndroidBridge.notify(title, body, id)
 *   AndroidBridge.notifPermStatus()       → "granted"|"denied"
 *   AndroidBridge.requestNotifPermission()
 *   AndroidBridge.startScan(pairsJson, intervalMinutes)
 *   AndroidBridge.stopScan()
 */
class AndroidBridge(
    private val context: Context,
    private val webView: WebView,
    private val notifPermLauncher: ActivityResultLauncher<String>? = null
) {

    companion object {
        const val REQ_NOTIF_PERM = 1001
        private const val PREFS_NAME   = "ftt_native_prefs"
        private const val KEY_API_BASE = "api_base"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val notifIdCounter = AtomicInteger(2000)

    // ─────────────────────────────────────────
    // API Base URL
    // ─────────────────────────────────────────

    @JavascriptInterface
    fun getApiBase(): String =
        prefs.getString(KEY_API_BASE, "") ?: ""

    /** Called from native settings if we ever add a native settings screen */
    fun setApiBase(url: String) {
        prefs.edit().putString(KEY_API_BASE, url.trimEnd('/')).apply()
    }

    // ─────────────────────────────────────────
    // Vibration
    // ─────────────────────────────────────────

    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        runCatching {
            val dur = durationMs.toLong().coerceIn(50, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(dur, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(dur)
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // Notifications
    // ─────────────────────────────────────────

    @JavascriptInterface
    fun notifPermStatus(): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return "granted"
        return if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) "granted" else "denied"
    }

    @JavascriptInterface
    fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                ?: (context as? MainActivity)?.requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIF_PERM
                )
        } else {
            // Already granted on older Android
            webView.post {
                webView.evaluateJavascript(
                    "window.dispatchEvent(new CustomEvent('ftt_notif_perm',{detail:'granted'}))", null
                )
            }
        }
    }

    @JavascriptInterface
    fun notify(title: String, body: String, id: Int) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notif = NotificationCompat.Builder(context, FttApp.NOTIF_CHANNEL_SIGNALS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 150, 80, 150))
                .build()

            val notifId = if (id > 0) id else notifIdCounter.getAndIncrement()
            NotificationManagerCompat.from(context).notify(notifId, notif)
        }
    }

    // ─────────────────────────────────────────
    // Watchlist scan lifecycle hints
    // ─────────────────────────────────────────

    @JavascriptInterface
    fun startScan(pairsJson: String, intervalMinutes: Int) {
        // The actual scanning runs in JS (fetch calls).
        // We just persist state so on next app open we know scan was active.
        prefs.edit()
            .putString("wl_pairs_json", pairsJson)
            .putInt("wl_interval", intervalMinutes)
            .putBoolean("wl_active", true)
            .apply()

        // Keep screen dim-lock while scanning (optional — handled via WakeLock in service)
        ScanService.start(context)
    }

    @JavascriptInterface
    fun stopScan() {
        prefs.edit().putBoolean("wl_active", false).apply()
        ScanService.stop(context)
    }

    // ─────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────

    fun destroy() {
        // nothing to clean up currently
    }
}
