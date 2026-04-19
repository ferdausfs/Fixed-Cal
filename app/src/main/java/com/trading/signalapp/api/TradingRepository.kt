package com.trading.signalapp.api

import com.trading.signalapp.model.HealthResponse
import com.trading.signalapp.model.HistoryResponse
import com.trading.signalapp.model.SignalResponse
import com.trading.signalapp.model.StatsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

class TradingRepository {
    private val api = RetrofitClient.service

    suspend fun getHealth(): ApiResult<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val r = api.getHealth()
            if (r.isSuccessful) r.body()?.let { ApiResult.Success(it) } ?: ApiResult.Error("Empty response")
            else ApiResult.Error("Server error: ${r.code()}")
        } catch (e: Exception) { ApiResult.Error(e.message ?: "Network error") }
    }

    suspend fun getSignal(pair: String): ApiResult<SignalResponse> = withContext(Dispatchers.IO) {
        try {
            val r = api.getSignal(pair)
            if (r.isSuccessful) r.body()?.let { ApiResult.Success(it) } ?: ApiResult.Error("Empty response")
            else ApiResult.Error("Server error: ${r.code()}")
        } catch (e: Exception) { ApiResult.Error(e.message ?: "Network error") }
    }

    suspend fun getHistory(pair: String, limit: Int = 20): ApiResult<HistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val r = api.getHistory(pair, limit)
            if (r.isSuccessful) r.body()?.let { ApiResult.Success(it) } ?: ApiResult.Error("Empty response")
            else ApiResult.Error("Server error: ${r.code()}")
        } catch (e: Exception) { ApiResult.Error(e.message ?: "Network error") }
    }

    suspend fun getStats(pair: String): ApiResult<StatsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = api.getStats(pair)
            if (r.isSuccessful) r.body()?.let { ApiResult.Success(it) } ?: ApiResult.Error("Empty response")
            else ApiResult.Error("Server error: ${r.code()}")
        } catch (e: Exception) { ApiResult.Error(e.message ?: "Network error") }
    }
}
