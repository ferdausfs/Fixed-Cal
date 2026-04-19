package com.trading.signalapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.trading.signalapp.api.Result
import com.trading.signalapp.api.TradingRepository
import com.trading.signalapp.model.*
import com.trading.signalapp.util.JournalStore
import com.trading.signalapp.util.PairUtils
import kotlinx.coroutines.*

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs get() = getApplication<Application>()
        .getSharedPreferences("ftt", Context.MODE_PRIVATE)

    // ── Settings ──────────────────────────────────────────────────────────────
    var savedBaseUrl: String?
        get() = prefs.getString("api_base", null)
        set(v) = prefs.edit().putString("api_base", v).apply()

    var savedOtcUrl: String?
        get() = prefs.getString("otc_base", null)
        set(v) = prefs.edit().putString("otc_base", v).apply()

    var slPips: Double
        get() = prefs.getFloat("sl_pips", 15f).toDouble()
        set(v) = prefs.edit().putFloat("sl_pips", v.toFloat()).apply()

    var tpPips: Double
        get() = prefs.getFloat("tp_pips", 30f).toDouble()
        set(v) = prefs.edit().putFloat("tp_pips", v.toFloat()).apply()

    var soundOn: Boolean
        get() = prefs.getBoolean("sound", true)
        set(v) = prefs.edit().putBoolean("sound", v).apply()

    private val repo get() = TradingRepository(savedBaseUrl)

    // ── Selected pair ─────────────────────────────────────────────────────────
    private val _selectedPair = MutableLiveData(
        prefs.getString("cur_pair", "EUR/USD") ?: "EUR/USD"
    )
    val selectedPair: LiveData<String> = _selectedPair

    fun selectPair(pair: String) {
        prefs.edit().putString("cur_pair", pair).apply()
        _selectedPair.value = pair
    }

    // ── Signal ────────────────────────────────────────────────────────────────
    private val _signal = MutableLiveData<Result<ParsedSignal>>()
    val signal: LiveData<Result<ParsedSignal>> = _signal

    private var signalJob: Job? = null

    fun loadSignal(pair: String = _selectedPair.value ?: "EUR/USD") {
        signalJob?.cancel()
        _signal.value = Result.Loading
        signalJob = viewModelScope.launch {
            val result = repo.fetchSignal(pair)
            _signal.value = result
            // Auto-journal on new signal
            if (result is Result.Success) {
                JournalStore.addFromSignal(getApplication(), result.data)
            }
        }
    }

    // ── Health ────────────────────────────────────────────────────────────────
    private val _health = MutableLiveData<Result<HealthResponse>>()
    val health: LiveData<Result<HealthResponse>> = _health

    fun loadHealth() {
        _health.value = Result.Loading
        viewModelScope.launch { _health.value = repo.fetchHealth() }
    }

    // ── History ───────────────────────────────────────────────────────────────
    private val _history = MutableLiveData<Result<List<HistoryItem>>>()
    val history: LiveData<Result<List<HistoryItem>>> = _history

    fun loadHistory(pair: String = _selectedPair.value ?: "EUR/USD") {
        _history.value = Result.Loading
        viewModelScope.launch {
            val r = repo.fetchHistory(pair)
            _history.value = if (r is Result.Success)
                Result.Success(r.data.history ?: emptyList())
            else Result.Error((r as Result.Error).message)
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────
    private val _stats = MutableLiveData<Result<StatsResponse>>()
    val stats: LiveData<Result<StatsResponse>> = _stats

    fun loadStats(pair: String = _selectedPair.value ?: "EUR/USD") {
        viewModelScope.launch { _stats.value = repo.fetchStats(pair) }
    }

    // ── Journal ───────────────────────────────────────────────────────────────
    private val _journal = MutableLiveData<List<JournalEntry>>()
    val journal: LiveData<List<JournalEntry>> = _journal

    fun loadJournal() {
        _journal.value = JournalStore.getAll(getApplication())
    }

    fun markJournalResult(id: String, result: String) {
        JournalStore.markResult(getApplication(), id, result)
        loadJournal()
    }

    fun deleteJournalEntry(id: String) {
        JournalStore.delete(getApplication(), id)
        loadJournal()
    }

    fun updateJournalNote(id: String, note: String) {
        JournalStore.updateNote(getApplication(), id, note)
    }

    // ── Watchlist ─────────────────────────────────────────────────────────────
    private val _watchlist = MutableLiveData<List<WatchlistItem>>()
    val watchlist: LiveData<List<WatchlistItem>> = _watchlist

    private var wlPairs: MutableList<String>
        get() = prefs.getStringSet("wl_pairs", emptySet())
            ?.toMutableList() ?: mutableListOf()
        set(v) = prefs.edit().putStringSet("wl_pairs", v.toSet()).apply()

    fun getWatchlistPairs() = wlPairs

    fun addToWatchlist(pair: String) {
        val list = wlPairs
        if (!list.contains(pair)) { list.add(pair); wlPairs = list }
        scanWatchlist()
    }

    fun removeFromWatchlist(pair: String) {
        val list = wlPairs
        list.remove(pair); wlPairs = list
        scanWatchlist()
    }

    private var scanJob: Job? = null

    fun scanWatchlist() {
        scanJob?.cancel()
        val pairs = wlPairs
        if (pairs.isEmpty()) { _watchlist.value = emptyList(); return }
        _watchlist.value = pairs.map { WatchlistItem(it, null, isScanning = true) }
        scanJob = viewModelScope.launch {
            val results = mutableListOf<WatchlistItem>()
            pairs.forEach { pair ->
                val r = repo.fetchSignal(pair)
                results.add(
                    WatchlistItem(
                        pair = pair,
                        signal = if (r is Result.Success) r.data else null,
                        isScanning = false,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                _watchlist.postValue(results.toList())
            }
        }
    }

    // ── Market open check ─────────────────────────────────────────────────────
    fun isMarketOpen(pair: String) = PairUtils.isOpen(pair)
    fun marketClosedReason(pair: String) = PairUtils.whyClosed(pair)
}
