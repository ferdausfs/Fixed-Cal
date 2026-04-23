package com.ftt.signal.data.model

import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

enum class SignalDirection { BUY, SELL, WAIT }

data class TimeframeCard(
    val timeframe: String,
    val direction: SignalDirection,
    val scoreUp: Double,
    val scoreDown: Double,
    val expiryLabel: String,
    val entryPrice: Double?
)

data class SignalSnapshot(
    val pair: String,
    val direction: SignalDirection,
    val confidence: Int,
    val grade: String,
    val entryPrice: Double?,
    val expiryLabel: String,
    val sessionLabel: String,
    val marketRegime: String,
    val reasons: List<String>,
    val generatedAt: String,
    val timeframes: List<TimeframeCard>
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val pair: String,
    val direction: SignalDirection,
    val confidence: Int,
    val entryPrice: Double?,
    val expiryLabel: String,
    val createdAt: String,
    val result: String = "PENDING",
    val notes: String = ""
)

data class HistoryTrade(
    val id: String,
    val pair: String,
    val direction: String,
    val confidence: String,
    val result: String,
    val timestamp: String,
    val grade: String,
    val bestTf: String
)

data class SignalResponseDto(
    val pair: String? = null,
    val signal: SignalDto? = null,
    val session: SessionDto? = null,
    val timestamp: String? = null
)

data class SignalDto(
    @SerializedName("finalSignal") val finalSignal: String? = null,
    val confidence: String? = null,
    val grade: GradeDto? = null,
    val recommendations: Map<String, RecommendationDto>? = null,
    val bestTimeframe: BestTimeframeDto? = null,
    val session: SessionDto? = null,
    val marketRegime: String? = null,
    val entryReason: String? = null,
    val alignment: String? = null,
    val filtersApplied: List<String>? = null,
    val generatedAt: String? = null
)

data class GradeDto(
    val grade: String? = null,
    val label: String? = null,
    val description: String? = null
)

data class RecommendationDto(
    val direction: String? = null,
    val score: ScoreDto? = null,
    val expiry: ExpiryDto? = null,
    val entry: EntryDto? = null
)

data class ScoreDto(
    val up: Double? = null,
    val down: Double? = null,
    val diff: Double? = null
)

data class ExpiryDto(
    val humanReadable: String? = null,
    val totalMinutes: Int? = null,
    val expiryTime: String? = null
)

data class EntryDto(
    val price: Double? = null,
    val candleTime: String? = null,
    val candleDirection: String? = null
)

data class BestTimeframeDto(
    val timeframe: String? = null
)

data class SessionDto(
    val overlap: String? = null,
    val sessions: List<String>? = null,
    val quality: String? = null
)

data class HistoryResponseDto(
    val signals: List<HistorySignalDto>? = null
)

data class HistorySignalDto(
    val id: String? = null,
    val pair: String? = null,
    val direction: String? = null,
    val confidence: String? = null,
    val result: String? = null,
    val timestamp: String? = null,
    val grade: String? = null,
    @SerializedName("bestTF") val bestTf: String? = null
)

data class StatsResponseDto(
    val pair: String? = null,
    val stats: StatsDto? = null,
    val message: String? = null
)

data class StatsDto(
    val total: Int? = null,
    val wins: Int? = null,
    val losses: Int? = null,
    val winRate: Double? = null
)

fun SignalResponseDto.toSnapshot(displayPair: String): SignalSnapshot {
    val dto = signal
    val tfCards = dto?.recommendations.orEmpty()
        .toList()
        .sortedBy { it.first.filter(Char::isDigit).toIntOrNull() ?: 999 }
        .map { (tf, value) ->
            TimeframeCard(
                timeframe = tf,
                direction = value.direction.toDirection(),
                scoreUp = value.score?.up ?: 0.0,
                scoreDown = value.score?.down ?: 0.0,
                expiryLabel = value.expiry?.humanReadable ?: ((value.expiry?.totalMinutes ?: 0).toString() + " min"),
                entryPrice = value.entry?.price
            )
        }

    val reasonLines = buildList {
        dto?.entryReason
            ?.split("·")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && it != "No clear setup — entry conditions not met." }
            ?.let(::addAll)

        dto?.alignment?.takeIf { !it.isNullOrBlank() && it != "NONE" }?.let { add("Alignment: $it") }
        dto?.filtersApplied?.filter { it.isNotBlank() }?.take(3)?.let(::addAll)
    }.ifEmpty { listOf("No extra reasoning returned by API.") }

    val bestTf = dto?.bestTimeframe?.timeframe
    val bestExpiry = bestTf?.let { dto?.recommendations?.get(it)?.expiry?.humanReadable }
        ?: tfCards.firstOrNull()?.expiryLabel
        ?: "—"

    return SignalSnapshot(
        pair = pair ?: displayPair,
        direction = dto?.finalSignal.toDirection(),
        confidence = dto?.confidence?.replace("%", "")?.toIntOrNull() ?: 0,
        grade = dto?.grade?.grade ?: dto?.grade?.label ?: "—",
        entryPrice = bestTf?.let { dto?.recommendations?.get(it)?.entry?.price } ?: tfCards.firstOrNull()?.entryPrice,
        expiryLabel = bestExpiry,
        sessionLabel = dto?.session?.overlap ?: session?.overlap ?: dto?.session?.sessions?.joinToString(" / ") ?: "N/A",
        marketRegime = dto?.marketRegime ?: "UNKNOWN",
        reasons = reasonLines,
        generatedAt = dto?.generatedAt ?: timestamp ?: nowStamp(),
        timeframes = tfCards
    )
}

fun HistorySignalDto.toHistoryTrade(): HistoryTrade = HistoryTrade(
    id = id.orEmpty(),
    pair = pair.orEmpty(),
    direction = direction.orEmpty(),
    confidence = confidence.orEmpty(),
    result = result ?: "UNKNOWN",
    timestamp = timestamp.orEmpty(),
    grade = grade.orEmpty(),
    bestTf = bestTf.orEmpty()
)

fun String?.toDirection(): SignalDirection = when (this?.uppercase(Locale.ROOT)) {
    "BUY" -> SignalDirection.BUY
    "SELL" -> SignalDirection.SELL
    else -> SignalDirection.WAIT
}

fun SignalDirection.label(): String = when (this) {
    SignalDirection.BUY -> "BUY"
    SignalDirection.SELL -> "SELL"
    SignalDirection.WAIT -> "WAIT"
}

fun nowStamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(System.currentTimeMillis())
