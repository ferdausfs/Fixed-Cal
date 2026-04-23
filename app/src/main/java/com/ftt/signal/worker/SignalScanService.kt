package com.ftt.signal.worker

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ftt.signal.data.local.AppPrefs
import com.ftt.signal.data.model.SignalDirection
import com.ftt.signal.data.model.label
import com.ftt.signal.data.remote.SignalRepository
import com.ftt.signal.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class SignalScanService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var prefs: AppPrefs
    private var loopJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        prefs = AppPrefs(this)
        NotificationHelper.ensureChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        prefs.scanEnabled = true
        startForeground(
            NotificationHelper.SCAN_NOTIFICATION_ID,
            NotificationHelper.ongoingScanNotification(this, "Scanning selected pairs in native background service")
        )

        if (loopJob?.isActive != true) {
            loopJob = serviceScope.launch {
                while (isActive && prefs.scanEnabled) {
                    val repo = SignalRepository(prefs.apiBase)
                    val pairs = prefs.watchlist.ifEmpty { AppPrefs.DEFAULT_WATCHLIST }
                    pairs.forEach { pair ->
                        runCatching {
                            val snapshot = repo.fetchSignal(pair)
                            val direction = snapshot.direction.label()
                            val previous = prefs.lastAlertDirection(pair)
                            if (snapshot.direction != SignalDirection.WAIT && previous != direction && prefs.notificationsEnabled) {
                                NotificationHelper.sendSignalNotification(
                                    context = this@SignalScanService,
                                    title = "${snapshot.pair} ${direction}",
                                    body = "Confidence ${snapshot.confidence}% · ${snapshot.expiryLabel} · ${snapshot.marketRegime}",
                                    id = Random.nextInt(2000, 9000)
                                )
                                prefs.setLastAlertDirection(pair, direction)
                            }
                        }
                    }
                    val mins = prefs.scanIntervalMinutes.coerceIn(1, 60)
                    delay(mins * 60 * 1000L)
                }
                stopSelf()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        prefs.scanEnabled = false
        loopJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
