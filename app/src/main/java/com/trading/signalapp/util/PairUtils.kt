package com.trading.signalapp.util

object PairUtils {

    val FX = listOf(
        "EUR/USD","GBP/USD","USD/JPY","USD/CHF","AUD/USD","NZD/USD","USD/CAD",
        "EUR/GBP","GBP/JPY","EUR/JPY","EUR/CHF","AUD/JPY","GBP/CHF","CAD/JPY",
        "EUR/AUD","GBP/AUD","AUD/NZD","USD/MXN","USD/ZAR"
    )

    val CRYPTO = listOf(
        "BTC/USD","ETH/USD","BNB/USD","XRP/USD","SOL/USD","ADA/USD",
        "DOGE/USD","AVAX/USD","DOT/USD","LINK/USD","BTC/EUR","ETH/EUR","BTC/GBP","ETH/BTC"
    )

    val OTC = listOf(
        "EUR/USD-OTC","GBP/USD-OTC","USD/JPY-OTC","AUD/USD-OTC","USD/CAD-OTC",
        "USD/CHF-OTC","NZD/USD-OTC","EUR/GBP-OTC","EUR/JPY-OTC","GBP/JPY-OTC",
        "EUR/AUD-OTC","GBP/AUD-OTC","AUD/JPY-OTC","EUR/CHF-OTC","GBP/CHF-OTC",
        "CAD/JPY-OTC","AUD/NZD-OTC","AUD/CHF-OTC"
    )

    val ALL = FX + CRYPTO + OTC

    fun isOtc(pair: String) = pair.endsWith("-OTC")
    fun isCrypto(pair: String) = CRYPTO.contains(pair)
    fun isForex(pair: String) = FX.contains(pair)

    fun category(pair: String) = when {
        isOtc(pair)    -> "OTC"
        isCrypto(pair) -> "CRYPTO"
        isForex(pair)  -> "FX"
        else           -> "FX"
    }

    // Mirrors HTML isOpen()
    fun isOpen(pair: String): Boolean {
        if (isOtc(pair) || isCrypto(pair)) return true
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val day = now.get(java.util.Calendar.DAY_OF_WEEK) // 1=Sun
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        if (day == 7) return false                   // Saturday
        if (day == 1 && hour < 21) return false      // Sunday before 21:00
        if (day == 6 && hour >= 21) return false     // Friday after 21:00
        return true
    }

    fun whyClosed(pair: String): String {
        val now = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        val day = now.get(java.util.Calendar.DAY_OF_WEEK)
        val hour = now.get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            day == 7 -> "Weekend — reopens Sun 21:00 UTC"
            day == 1 && hour < 21 -> "Opens today at 21:00 UTC"
            day == 6 && hour >= 21 -> "Closed for weekend"
            else -> "Market closed"
        }
    }

    fun getPipSize(pair: String): Double = when {
        pair.contains("JPY") -> 0.01
        pair.contains("XAU") -> 0.1
        pair.contains("BTC") -> 1.0
        pair.contains("ETH") -> 0.1
        isCrypto(pair)       -> 0.01
        else                 -> 0.0001
    }

    fun calcSL(pair: String, dir: String, entryPrice: Double, slPips: Double): Double {
        val ps = getPipSize(pair)
        return if (dir == "BUY") entryPrice - slPips * ps else entryPrice + slPips * ps
    }

    fun calcTP(pair: String, dir: String, entryPrice: Double, tpPips: Double): Double {
        val ps = getPipSize(pair)
        return if (dir == "BUY") entryPrice + tpPips * ps else entryPrice - tpPips * ps
    }

    fun formatPrice(pair: String, price: Double): String {
        val decimals = when {
            pair.contains("JPY") -> 3
            pair.contains("BTC") -> 1
            pair.contains("XAU") -> 2
            price > 100          -> 2
            else                 -> 5
        }
        return "%.${decimals}f".format(price)
    }

    fun sessionLabel(): String {
        val h = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            .get(java.util.Calendar.HOUR_OF_DAY)
        return when {
            h >= 22 || h < 8  -> "Asia"
            h in 8..12        -> "London"
            h in 13..16       -> "New York"
            else              -> "London/NY"
        }
    }

    // OTC API pair format: EUR/USD-OTC → EURUSDotc
    fun toApiPair(pair: String): String =
        if (isOtc(pair)) pair.replace("/", "").replace("-OTC", "otc")
        else pair
}
