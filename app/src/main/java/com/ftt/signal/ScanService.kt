package com.ftt.signal

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Foreground service — watchlist pairs গুলো periodically scan করে।
 * BUY/SELL signal পেলে notification পাঠায়।
 *
 * HTML থেকে call হয়:
 *   AndroidBridge.startScan(pairsJson, intervalMin)
 *   AndroidBridge.stopScan()
 */
class ScanService : Service() {

    companion object {
        const val ACTION_START = "START"
        const val ACTION_STOP  = "STOP"
        const val EXTRA_PAIRS    = "pairs"
        const val EXTRA_INTERVAL = "interval"
        const val EXTRA_API_BASE = "api_base"

        private const val FG_ID = 1001
        private const val TAG   = "ScanService"
        private const val DEFAULT_API = "https://asignal.umuhammadiswa.workers.dev"
        private const val OTC_API     = "https://fttotcv6.umuhammadiswa.workers.dev"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var notifId = 2000 // signal notification id counter

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val pairsJson = intent.getStringExtra(EXTRA_PAIRS) ?: "[]"
                val interval  = intent.getIntExtra(EXTRA_INTERVAL, 5).coerceIn(1, 60)
                val apiBase   = intent.getStringExtra(EXTRA_API_BASE) ?: DEFAULT_API

                val pairs = parsePairs(pairsJson)
                if (pairs.isEmpty()) {
                    stopSelf(); return START_NOT_STICKY
                }

                // Start foreground notification
                startForeground(FG_ID, NotifHelper.foreground(this, pairs.size, interval))
                Log.d(TAG, "Scan started: ${pairs.size} pairs, every ${interval}m")

                // Launch scan loop
                scanJob?.cancel()
                scanJob = scope.launch {
                    while (isActive) {
                        pairs.forEach { pair -> scanPair(pair, apiBase) }
                        delay(interval * 60_000L)
                    }
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Scan stopped")
                scanJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun parsePairs(json: String): List<String> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) { emptyList() }
    }

    private suspend fun scanPair(pair: String, apiBase: String) {
        try {
            val isOTC = pair.contains("-OTC", ignoreCase = true)
            val base  = if (isOTC) OTC_API else apiBase
            val apiPair = if (isOTC) pair.replace("-OTC", "otc", ignoreCase = true) else pair

            val url = "$base/api/signal?pair=${encode(apiPair)}"
            val req = Request.Builder().url(url).build()

            val response = withTimeout(25_000) {
                http.newCall(req).execute()
            }

            if (!response.isSuccessful) return

            val responseBody = response.body?.string() ?: return
            val json = JSONObject(responseBody)

            val signal = json.optJSONObject("signal") ?: return
            val final  = signal.optString("finalSignal", "")
            if (final != "BUY" && final != "SELL") return

            val conf    = signal.optString("confidence", "0").replace("%", "").toIntOrNull() ?: 0
            val grade   = signal.optJSONObject("grade")?.optString("grade", "") ?: ""
            val gradeStr = if (grade.isNotEmpty()) " [$grade]" else ""
            val arrow   = if (final == "BUY") "▲" else "▼"
            val title   = "FTT — $pair $arrow $final$gradeStr"
            val notifBody = "$final · $conf% confidence · Scan result"

            NotifHelper.show(this, notifId++, title, notifBody)
            if (notifId > 9000) notifId = 2000

        } catch (e: Exception) {
            Log.w(TAG, "scanPair($pair) error: ${e.message}")
        }
    }

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    override fun onDestroy() {
        scanJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
}
