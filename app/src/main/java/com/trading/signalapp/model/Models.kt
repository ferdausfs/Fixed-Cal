package com.trading.signalapp.model

data class HealthResponse(
    val status: String,
    val version: String,
    val timestamp: String,
    val currentSession: CurrentSession?,
    val newsBlackout: NewsBlackout?,
    val markets: Markets?,
    val filters: Filters?,
    val indicators: List<String>?
)

data class CurrentSession(
    val sessions: List<String>,
    val overlap: String?,
    val quality: String,
    val hour: Int
)

data class NewsBlackout(
    val blocked: Boolean,
    val label: String
)

data class Markets(
    val forex: ForexMarket?,
    val crypto: CryptoMarket?
)

data class ForexMarket(
    val status: String,
    val holiday: String?,
    val currencies: Int,
    val possiblePairs: Int
)

data class CryptoMarket(
    val status: String,
    val bases: List<String>,
    val quotes: List<String>,
    val topPairs: List<String>
)

data class Filters(
    val minConfidenceFloor: String,
    val volumeSpikeMultiplier: String,
    val newsBlackoutMargin: String,
    val batchMaxPairs: Int
)

data class SignalResponse(
    val signal: Signal?,
    val error: String?,
    val message: String?
)

data class Signal(
    val id: String?,
    val pair: String,
    val direction: String,
    val confidence: Double,
    val entry: Double?,
    val takeProfit: Double?,
    val stopLoss: Double?,
    val expiry: String?,
    val timeframe: String?,
    val indicators: List<String>?,
    val session: String?,
    val timestamp: String?
)

data class HistoryResponse(
    val history: List<HistoryItem>?,
    val pair: String?,
    val total: Int?
)

data class HistoryItem(
    val id: String,
    val pair: String,
    val direction: String,
    val confidence: Double,
    val result: String?,
    val entry: Double?,
    val takeProfit: Double?,
    val stopLoss: Double?,
    val timestamp: String?,
    val expiry: String?
)

data class StatsResponse(
    val pair: String?,
    val totalSignals: Int?,
    val wins: Int?,
    val losses: Int?,
    val winRate: Double?,
    val pending: Int?
)
