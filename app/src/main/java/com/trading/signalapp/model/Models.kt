package com.trading.signalapp.model

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// ── API Raw Response (matches HTML fetchSig exactly) ─────────────────────────

data class ApiSignalResponse(
    val pair: String?,
    val signal: RawSignal?,
    val session: RawSession?,
    val timestamp: String?,
    val source: String?,
    val errors: Map<String, String>?
)

data class RawSignal(
    val finalSignal: String?,       // BUY / SELL / NO_TRADE / HOLD
    val confidence: String?,        // "78%"
    val grade: JsonElement?,        // string or object {grade:"A"}
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

data class RawEntry(val price: String?)

data class RawExpiry(
    val totalMinutes: Int?,
    val humanReadable: String?,
    val countdown: String?
)

data class RawBestTF(val timeframe: String?)

data class RawTFAnalysis(
    val indicators: Map<String, String>?
)

data class RawVotes(
    @SerializedName("BUY") val buy: Int?,
    @SerializedName("SELL") val sell: Int?,
    val weightedBuy: Double?,
    val weightedSell: Double?
)

data class RawSession(
    val sessions: List<String>?,
    val overlap: String?
)

data class AiValidation(
    val status: String?,    // agree / disagree / uncertain
    val confidence: Int?,
    val reason: String?,
    val concerns: String?
)

// ── Parsed Signal (what UI uses — mirrors HTML's parsed object) ────────────────

data class ParsedSignal(
    val label: String,              // BUY / SELL / WAIT / HOLD
    val confidence: Int,            // 0–100
    val grade: String,              // A+ / A / B / C / D / F / ""
    val pair: String,
    val timestamp: String,
    val entryPrice: String?,        // "1.23456"
    val expiryMinutes: Int,
    val expirySuggestion: String,
    val tfAgreement: String,
    val buyScore: Int,
    val sellScore: Int,
    val sessionLabel: String,
    val h1Structure: String,
    val atrLevel: String,           // HIGH / MED / LOW
    val marketRegime: String,
    val reasons: List<String>,
    val tfBreakdown: Map<String, TFBreakdown>,
    val aiValidation: AiValidation?,
    val isOtc: Boolean,
    val slPips: Double = 15.0,
    val tpPips: Double = 30.0
)

data class TFBreakdown(
    val bias: String,               // BUY / SELL / NEUTRAL
    val buyVotes: Int,
    val sellVotes: Int,
    val adxValue: Double?
)

// ── Journal ────────────────────────────────────────────────────────────────────

data class JournalEntry(
    val id: String,
    val pair: String,
    val dir: String,
    val conf: Int,
    val sess: String,
    val grade: String,
    val ep: String?,                // entry price
    val sl: String?,
    val tp: String?,
    val result: String,             // WIN / LOSS / PENDING
    val pips: Double?,
    val note: String,
    val ts: Long,                   // timestamp millis
    val isAuto: Boolean,
    val exitPrice: String?,
    val closedTs: Long?
)

// ── Watchlist ──────────────────────────────────────────────────────────────────

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

// ── Grades ────────────────────────────────────────────────────────────────────

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
