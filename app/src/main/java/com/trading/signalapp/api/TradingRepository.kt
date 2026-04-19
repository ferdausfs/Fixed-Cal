package com.trading.signalapp.api

import com.trading.signalapp.model.*
import com.trading.signalapp.util.PairUtils
import com.trading.signalapp.util.SignalParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val isApiLimit: Boolean = false) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class TradingRepository(private val savedBaseUrl: String? = null) {

    private fun apiFor(pair: String): TradingApiService {
        val isOtc = PairUtils.isOtc(pair)
        return when {
            isOtc -> RetrofitClient.otc
            savedBaseUrl != null -> RetrofitClient.build(savedBaseUrl)
            else -> RetrofitClient.default
        }
    }

    suspend fun fetchSignal(pair: String): Result<ParsedSignal> = withContext(Dispatchers.IO) {
        try {
            val apiPair = PairUtils.toApiPair(pair)
            val response = apiFor(pair).getSignal(apiPair)
            if (!response.isSuccessful)
                return@withContext Result.Error("HTTP ${response.code()}")

            val body = response.body()
                ?: return@withContext Result.Error("Empty response")

            // Check for DUMMY_FALLBACK (API limit exceeded)
            val sig = body.signal
            if (sig?.method == "DUMMY_FALLBACK" || body.source == "DUMMY_FALLBACK") {
                val errMsg = body.errors?.values?.firstOrNull() ?: "API limit exceeded"
                return@withContext Result.Error(errMsg, isApiLimit = true)
            }

            val parsed = SignalParser.parse(body)
                ?: return@withContext Result.Error("Could not parse signal")

            Result.Success(parsed)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }

    suspend fun fetchHealth(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        try {
            val r = RetrofitClient.default.getHealth()
            if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty")
            else Result.Error("HTTP ${r.code()}")
        } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
    }

    suspend fun fetchHistory(pair: String, limit: Int = 20): Result<HistoryResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiFor(pair).getHistory(pair, limit)
            if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty")
            else Result.Error("HTTP ${r.code()}")
        } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
    }

    suspend fun fetchStats(pair: String): Result<StatsResponse> = withContext(Dispatchers.IO) {
        try {
            val r = apiFor(pair).getStats(pair)
            if (r.isSuccessful) r.body()?.let { Result.Success(it) } ?: Result.Error("Empty")
            else Result.Error("HTTP ${r.code()}")
        } catch (e: Exception) { Result.Error(e.message ?: "Network error") }
    }
}
