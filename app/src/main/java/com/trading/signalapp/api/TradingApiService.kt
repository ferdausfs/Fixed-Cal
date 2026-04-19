package com.trading.signalapp.api

import com.trading.signalapp.model.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TradingApiService {
    @GET("/")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("/api/signal")
    suspend fun getSignal(@Query("pair") pair: String): Response<ApiSignalResponse>

    @GET("/api/history")
    suspend fun getHistory(
        @Query("pair") pair: String,
        @Query("limit") limit: Int = 20
    ): Response<HistoryResponse>

    @GET("/api/stats")
    suspend fun getStats(@Query("pair") pair: String): Response<StatsResponse>
}
