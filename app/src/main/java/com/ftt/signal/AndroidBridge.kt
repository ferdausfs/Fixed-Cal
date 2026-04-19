package com.ftt.signal

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat

/**
 * JavaScript ↔ Native bridge.
 * HTML এর init code এই class এর methods call করে।
 *
 * window.AndroidBridge.getApiBase()
 * window.AndroidBridge.notify(title, desc, id)
 * window.AndroidBridge.notifPermStatus()
 * window.AndroidBridge.requestNotifPermission()
 * window.AndroidBridge.vibrate(ms)
 * window.AndroidBridge.startScan(pairsJson, intervalMin)
 * window.AndroidBridge.stopScan()
 */
class AndroidBridge(private val context: Context) {

    companion object {
        private const val PREFS = "ftt_native_prefs"
        private const val KEY_API_BASE = "api_base"
        private const val DEFAULT_API = "https://asignal.umuhammadiswa.workers.dev"
    }

    private val prefs by lazy { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    // ── API Base URL ──────────────────────────────────────────

    @JavascriptInterface
    fun getApiBase(): String {
        return prefs.getString(KEY_API_BASE, DEFAULT_API) ?: DEFAULT_API
    }

    /** HTML app saves new API base → native side store করে রাখো */
    @JavascriptInterface
    fun setApiBase(url: String) {
        if (url.startsWith("http")) {
            prefs.edit().putString(KEY_API_BASE, url.trimEnd('/')).apply()
        }
    }

    // ── Notifications ─────────────────────────────────────────

    @JavascriptInterface
    fun notify(title: String, desc: String, id: Int) {
        NotifHelper.show(context, id, title, desc)
    }

    @JavascriptInterface
    fun notifPermStatus(): String {
        return if (NotificationManagerCompat.from(context).areNotificationsEnabled())
            "granted" else "denied"
    }

    @JavascriptInterface
    fun requestNotifPermission() {
        // MainActivity-এ post করো (UI thread দরকার)
        MainActivity.instance?.requestNotifPermFromBridge()
    }

    // ── Vibration ─────────────────────────────────────────────

    @JavascriptInterface
    fun vibrate(ms: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(ms.toLong())
                }
            }
        } catch (e: Exception) {
            // ignore – vibrate is optional
        }
    }

    // ── Background Watchlist Scan ─────────────────────────────

    /**
     * @param pairsJson  JSON array string, e.g. ["EUR/USD","GBP/USD"]
     * @param intervalMin  scan interval in minutes (int)
     */
    @JavascriptInterface
    fun startScan(pairsJson: String, intervalMin: Int) {
        val intent = Intent(context, ScanService::class.java).apply {
            action = ScanService.ACTION_START
            putExtra(ScanService.EXTRA_PAIRS, pairsJson)
            putExtra(ScanService.EXTRA_INTERVAL, intervalMin)
            putExtra(ScanService.EXTRA_API_BASE, getApiBase())
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    @JavascriptInterface
    fun stopScan() {
        val intent = Intent(context, ScanService::class.java).apply {
            action = ScanService.ACTION_STOP
        }
        context.startService(intent)
    }
}
