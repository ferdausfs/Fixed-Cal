package com.ftt.signal.data.remote

import com.ftt.signal.data.model.HistoryResponseDto
import com.ftt.signal.data.model.SignalResponseDto
import com.ftt.signal.data.model.SignalSnapshot
import com.ftt.signal.data.model.StatsResponseDto
import com.ftt.signal.data.model.toHistoryTrade
import com.ftt.signal.data.model.toSnapshot
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface SignalApiService {
    @GET("/health")
    suspend fun health(): Map<String, Any>

    @GET("/api/signal")
    suspend fun signal(@Query("pair") pair: String): SignalResponseDto

    @GET("/api/history")
    suspend fun history(
        @Query("pair") pair: String,
        @Query("limit") limit: Int = 10
    ): HistoryResponseDto

    @GET("/api/stats")
    suspend fun stats(@Query("pair") pair: String): StatsResponseDto
}

class SignalRepository(private val baseUrl: String) {
    private val api: SignalApiService by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(baseUrl.ensureTrailingSlash())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SignalApiService::class.java)
    }

    suspend fun fetchSignal(displayPair: String): SignalSnapshot {
        val dto = api.signal(displayPair.toApiPair())
        return dto.toSnapshot(displayPair)
    }

    suspend fun fetchHistory(displayPair: String, limit: Int = 8) =
        api.history(displayPair, limit).signals.orEmpty().map { it.toHistoryTrade() }

    suspend fun health() = api.health()
}

fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"

fun String.toApiPair(): String = if (contains("-OTC")) {
    replace("/", "").replace("-OTC", "otc")
} else {
    replace("/", "")
}
