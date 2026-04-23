package com.ftt.signal.data.local

import android.content.Context
import android.content.SharedPreferences
import com.ftt.signal.data.model.JournalEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppPrefs(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ftt_native_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    var apiBase: String
        get() = prefs.getString(KEY_API_BASE, DEFAULT_API_BASE) ?: DEFAULT_API_BASE
        set(value) = prefs.edit().putString(KEY_API_BASE, value.trim().trimEnd('/')).apply()

    var selectedPair: String
        get() = prefs.getString(KEY_SELECTED_PAIR, DEFAULT_PAIR) ?: DEFAULT_PAIR
        set(value) = prefs.edit().putString(KEY_SELECTED_PAIR, value).apply()

    var watchlist: Set<String>
        get() = prefs.getStringSet(KEY_WATCHLIST, DEFAULT_WATCHLIST)?.toSet() ?: DEFAULT_WATCHLIST
        set(value) = prefs.edit().putStringSet(KEY_WATCHLIST, value).apply()

    var scanIntervalMinutes: Int
        get() = prefs.getInt(KEY_SCAN_INTERVAL, 5)
        set(value) = prefs.edit().putInt(KEY_SCAN_INTERVAL, value.coerceIn(1, 60)).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION, value).apply()

    var scanEnabled: Boolean
        get() = prefs.getBoolean(KEY_SCAN_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SCAN_ENABLED, value).apply()

    fun loadJournal(): MutableList<JournalEntry> {
        val json = prefs.getString(KEY_JOURNAL, null) ?: return mutableListOf()
        return runCatching {
            val type = object : TypeToken<MutableList<JournalEntry>>() {}.type
            gson.fromJson<MutableList<JournalEntry>>(json, type) ?: mutableListOf()
        }.getOrDefault(mutableListOf())
    }

    fun saveJournal(entries: List<JournalEntry>) {
        prefs.edit().putString(KEY_JOURNAL, gson.toJson(entries.takeLast(300))).apply()
    }

    fun lastAlertDirection(pair: String): String? = prefs.getString("alert_$pair", null)

    fun setLastAlertDirection(pair: String, direction: String) {
        prefs.edit().putString("alert_$pair", direction).apply()
    }

    companion object {
        const val DEFAULT_API_BASE = "https://fttotcv6.umuhammadiswa.workers.dev"
        const val DEFAULT_PAIR = "EUR/USD"
        val DEFAULT_WATCHLIST = setOf("EUR/USD", "GBP/USD", "USD/JPY", "BTC/USD")

        private const val KEY_API_BASE = "api_base"
        private const val KEY_SELECTED_PAIR = "selected_pair"
        private const val KEY_WATCHLIST = "watchlist"
        private const val KEY_SCAN_INTERVAL = "scan_interval"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SCAN_ENABLED = "scan_enabled"
        private const val KEY_JOURNAL = "journal"
    }
}
