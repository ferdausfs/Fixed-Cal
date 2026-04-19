package com.trading.signalapp.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.trading.signalapp.model.*

object SignalParser {

    // Safe helpers to extract from JsonElement
    private fun JsonElement?.asStrSafe(): String? = try {
        if (this == null || isJsonNull) null
        else if (isJsonPrimitive) asString
        else null
    } catch (e: Exception) { null }

    private fun JsonElement?.asIntSafe(): Int? = try {
        if (this == null || isJsonNull) null
        else if (isJsonPrimitive && asJsonPrimitive.isNumber) asInt
        else if (isJsonPrimitive) asString.toIntOrNull()
        else null
    } catch (e: Exception) { null }

    private fun JsonElement?.asDoubleSafe(): Double? = try {
        if (this == null || isJsonNull) null
        else if (isJsonPrimitive && asJsonPrimitive.isNumber) asDouble
        else if (isJsonPrimitive) asString.toDoubleOrNull()
        else null
    } catch (e: Exception) { null }

    // Parse API response → ParsedSignal (mirrors HTML fetchSig exactly)
    fun parse(response: ApiSignalResponse): ParsedSignal? {
        val s = response.signal ?: return null

        // confidence "78%" → 78
        val confNum = s.confidence?.replace("%", "")?.trim()?.toIntOrNull() ?: 0

        // recommendations & best timeframe
        val rec = s.recommendations ?: emptyMap()
        val tfKeys = rec.keys.toList()
        val bestTFKey = s.bestTimeframe?.timeframe?.takeIf { rec.containsKey(it) }
            ?: tfKeys.firstOrNull() ?: "5min"
        val bestRec = rec[bestTFKey]

        // expiry — totalMinutes can be int or string
        val expMins = bestRec?.expiry?.totalMinutes?.asIntSafe() ?: 5
        val expSug  = bestRec?.expiry?.humanReadable ?: "${expMins}m"

        // entry price — can be string or number
        val ep = bestRec?.entry?.price?.asDoubleSafe()

        // timeframe breakdown
        val tfa = s.timeframeAnalysis ?: emptyMap()
        val tfBreakdown = mutableMapOf<String, TFBreakdown>()
        tfKeys.forEach { tf ->
            val r = rec[tf] ?: return@forEach
            val sc = r.score
            val ind = tfa[tf]?.indicators ?: emptyMap()
            val adxVal = ind["adx"]?.asDoubleSafe()
            tfBreakdown[tf] = TFBreakdown(
                bias      = r.direction ?: "NEUTRAL",
                buyVotes  = sc?.up ?: 0,
                sellVotes = sc?.down ?: 0,
                adxValue  = adxVal
            )
        }

        // session label
        val sessRaw = response.session
        var sesLbl = sessRaw?.overlap
            ?: sessRaw?.sessions?.firstOrNull()
            ?: "N/A"
        sesLbl = sesLbl.replace("OTC_24/7", "OTC 24/7")

        // market condition → ATR level
        val mc = s.marketCondition ?: emptyList()
        val mcStr = mc.joinToString(",")
        val atrLvl = when {
            mcStr.contains("VOLATILE") -> "HIGH"
            mcStr.contains("DEAD")     -> "LOW"
            else                       -> "MED"
        }

        // grade — string or object {grade:"A"}
        val grade = when {
            s.grade == null         -> ""
            s.grade.isJsonPrimitive -> s.grade.asString
            s.grade.isJsonObject    -> (s.grade as? JsonObject)
                ?.get("grade")?.asString ?: ""
            else -> ""
        }

        // direction label
        val lbl = when (s.finalSignal) {
            "BUY"      -> "BUY"
            "SELL"     -> "SELL"
            "NO_TRADE" -> "WAIT"
            else       -> "HOLD"
        }

        // reasons
        val reasons = mutableListOf<String>()
        if (s.entryReason != null && lbl != "WAIT") {
            s.entryReason.split("·").forEach { r ->
                val t = r.trim()
                if (t.isNotEmpty() && t != "No clear setup — entry conditions not met.")
                    reasons.add(t)
            }
        }
        if (s.alignment != null && s.alignment != "NONE")
            reasons.add("Alignment: ${s.alignment}")
        if (s.averageConfluence != null)
            reasons.add("Avg Confluence: ${s.averageConfluence}/11")

        // votes — weightedBuy/Sell can be int or double
        val votes = s.votes
        val buyScore  = votes?.weightedBuy?.asDoubleSafe()?.toInt() ?: 0
        val sellScore = votes?.weightedSell?.asDoubleSafe()?.toInt() ?: 0
        val tfAgree = when {
            lbl == "BUY"  && (votes?.buy ?: 0) > 0  -> "${votes?.buy}TF"
            lbl == "SELL" && (votes?.sell ?: 0) > 0 -> "${votes?.sell}TF"
            else -> "—"
        }

        val pair = response.pair ?: "?"
        return ParsedSignal(
            label           = lbl,
            confidence      = confNum,
            grade           = grade,
            pair            = pair,
            timestamp       = response.timestamp ?: s.generatedAt ?: "",
            entryPrice      = ep?.let { PairUtils.formatPrice(pair, it) },
            expiryMinutes   = expMins,
            expirySuggestion = expSug,
            tfAgreement     = tfAgree,
            buyScore        = buyScore,
            sellScore       = sellScore,
            sessionLabel    = sesLbl,
            h1Structure     = s.higherTFTrend ?: "NEUTRAL",
            atrLevel        = atrLvl,
            marketRegime    = mc.firstOrNull() ?: "",
            reasons         = reasons,
            tfBreakdown     = tfBreakdown,
            aiValidation    = s.aiValidation,
            isOtc           = PairUtils.isOtc(pair)
        )
    }

    fun gradeInfo(grade: String): Triple<String, String, String> = when (grade) {
        "A+" -> Triple("🏆", "Elite Signal",    "85%+ confidence. Perfect TF alignment.")
        "A"  -> Triple("✅", "Strong Signal",   "70–84% confidence. Clear directional bias.")
        "B"  -> Triple("👍", "Good Signal",     "55–69% confidence. Moderate TF alignment.")
        "C"  -> Triple("⚠️", "Moderate Signal", "40–54% confidence. Some conflicts.")
        "D"  -> Triple("⚡", "Weak Signal",     "Below 40% confidence. High risk setup.")
        "F"  -> Triple("🚫", "Blocked",         "Signal blocked by HTF conflict.")
        else -> Triple("📊", grade.ifEmpty { "?" }, "—")
    }
}
