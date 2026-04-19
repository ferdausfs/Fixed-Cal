package com.trading.signalapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trading.signalapp.api.ApiResult
import com.trading.signalapp.api.TradingRepository
import com.trading.signalapp.model.HealthResponse
import com.trading.signalapp.model.HistoryItem
import com.trading.signalapp.model.Signal
import com.trading.signalapp.model.StatsResponse
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val repository = TradingRepository()

    private val _selectedPair = MutableLiveData("BTC/USD")
    val selectedPair: LiveData<String> = _selectedPair
    fun setSelectedPair(pair: String) { _selectedPair.value = pair }

    private val _health = MutableLiveData<ApiResult<HealthResponse>>()
    val health: LiveData<ApiResult<HealthResponse>> = _health
    fun loadHealth() {
        _health.value = ApiResult.Loading
        viewModelScope.launch { _health.value = repository.getHealth() }
    }

    private val _signal = MutableLiveData<ApiResult<Signal?>>()
    val signal: LiveData<ApiResult<Signal?>> = _signal
    fun loadSignal(pair: String = _selectedPair.value ?: "BTC/USD") {
        _signal.value = ApiResult.Loading
        viewModelScope.launch {
            when (val result = repository.getSignal(pair)) {
                is ApiResult.Success -> _signal.value = ApiResult.Success(result.data.signal)
                is ApiResult.Error   -> _signal.value = ApiResult.Error(result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    private val _history = MutableLiveData<ApiResult<List<HistoryItem>>>()
    val history: LiveData<ApiResult<List<HistoryItem>>> = _history
    fun loadHistory(pair: String = _selectedPair.value ?: "BTC/USD") {
        _history.value = ApiResult.Loading
        viewModelScope.launch {
            when (val result = repository.getHistory(pair)) {
                is ApiResult.Success -> _history.value = ApiResult.Success(result.data.history ?: emptyList())
                is ApiResult.Error   -> _history.value = ApiResult.Error(result.message)
                is ApiResult.Loading -> {}
            }
        }
    }

    private val _stats = MutableLiveData<ApiResult<StatsResponse>>()
    val stats: LiveData<ApiResult<StatsResponse>> = _stats
    fun loadStats(pair: String = _selectedPair.value ?: "BTC/USD") {
        _stats.value = ApiResult.Loading
        viewModelScope.launch { _stats.value = repository.getStats(pair) }
    }
}
