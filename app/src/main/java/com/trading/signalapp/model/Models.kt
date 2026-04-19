package com.trading.signalapp.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ── API Raw Response ──────────────────────────────────────────────────────────

data class ApiSignalResponse(
    val pair: String?,
    val signal: RawSignal?,
    val session: RawSession?,
    val timestamp: String?,
    val source: String?,
    val errors: Map<String, String>?
)

data class RawSignal(
    val finalSignal: String?,
    val confidence: String?,
    val grade: JsonElement?,
    val recommendations: Map<String, RawRecommendation>?,
    val bestTimeframe: RawBestTF?,
    val timeframeAnalysis: Map<String, RawTFAnalysis>?,
    val votes: RawVotes?,
    val entryReason: String?,
    val alignment: String?,
    val averageConfluence: Int?,
    val higherTFTrend: String?,
    val marketCondition: List<String>?,
    val aiValidation: AiValidation?,
    val session: JsonElement?,
    val generatedAt: String?,
    val method: String?
)

data class RawRecommendation(
    val direction: String?,
    val score: RawScore?,
    val entry: RawEntry?,
    val expiry: RawExpiry?
)

data class RawScore(val up: Int?, val down: Int?)

data class RawEntry(val price: JsonElement?)  // can be string or number

data class RawExpiry(
    val totalMinutes: JsonElement?,      // can be int or string
    val humanReadable: String?,
    val countdown: JsonElement?          // can be string OR object — use JsonElement
)

data class RawBestTF(val timeframe: String?)

data class RawTFAnalysis(
    val indicators: Map<String, JsonElement>?  // values can be string or number
)

data class RawVotes(
    @SerializedName("BUY")  val buy: Int?,
    @SerializedName("SELL") val sell: Int?,
    val weightedBuy: JsonElement?,   // can be int or double
    val weightedSell: JsonElement?
)

data class RawSession(
    val sessions: List<String>?,
    val overlap: String?
)

data class AiValidation(
    val status: String?,
    val confidence: JsonElement?,  // can be int or string
    val reason: String?,
    val concerns: String?
)

// ── Parsed Signal (UI model) ──────────────────────────────────────────────────

data class ParsedSignal(
    val label: String,
    val confidence: Int,
    val grade: String,
    val pair: String,
    val timestamp: String,
    val entryPrice: String?,
    val expiryMinutes: Int,
    val expirySuggestion: String,
    val tfAgreement: String,
    val buyScore: Int,
    val sellScore: Int,
    val sessionLabel: String,
    val h1Structure: String,
    val atrLevel: String,
    val marketRegime: String,
    val reasons: List<String>,
    val tfBreakdown: Map<String, TFBreakdown>,
    val aiValidation: AiValidation?,
    val isOtc: Boolean,
    val slPips: Double = 15.0,
    val tpPips: Double = 30.0
)

data class TFBreakdown(
    val bias: String,
    val buyVotes: Int,
    val sellVotes: Int,
    val adxValue: Double?
)

// ── Journal ───────────────────────────────────────────────────────────────────

data class JournalEntry(
    val id: String,
    val pair: String,
    val dir: String,
    val conf: Int,
    val sess: String,
    val grade: String,
    val ep: String?,
    val sl: String?,
    val tp: String?,
    val result: String,
    val pips: Double?,
    val note: String,
    val ts: Long,
    val isAuto: Boolean,
    val exitPrice: String?,
    val closedTs: Long?
)

// ── Watchlist ─────────────────────────────────────────────────────────────────

data class WatchlistItem(
    val pair: String,
    val signal: ParsedSignal?,
    val isScanning: Boolean = false,
    val isNew: Boolean = false,
    val lastUpdated: Long = 0
)

// ── Health ────────────────────────────────────────────────────────────────────

data class HealthResponse(
    val status: String?,
    val version: String?,
    val timestamp: String?,
    val currentSession: CurrentSession?,
    val newsBlackout: NewsBlackout?,
    val markets: Markets?,
    val filters: Filters?,
    val indicators: List<String>?,
    val apiKeys: ApiKeys?
)

data class ApiKeys(val configured: Int?)
data class CurrentSession(val sessions: List<String>?, val overlap: String?, val quality: String?, val hour: Int?)
data class NewsBlackout(val blocked: Boolean?, val label: String?)
data class Markets(val forex: ForexMarket?, val crypto: CryptoMarket?)
data class ForexMarket(val status: String?, val holiday: String?)
data class CryptoMarket(val status: String?, val topPairs: List<String>?)
data class Filters(val minConfidenceFloor: String?, val volumeSpikeMultiplier: String?, val newsBlackoutMargin: String?)
data class GradeInfo(val emoji: String, val label: String, val desc: String)

// ── History & Stats ───────────────────────────────────────────────────────────

data class HistoryResponse(val history: List<HistoryItem>?, val pair: String?, val total: Int?)

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
