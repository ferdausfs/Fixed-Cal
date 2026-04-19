package com.trading.signalapp.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.trading.signalapp.model.JournalEntry
import com.trading.signalapp.model.ParsedSignal
import java.util.UUID

object JournalStore {
    private const val KEY = "ftt_journal"
    private const val MAX = 300
    private val gson = Gson()

    fun getAll(ctx: Context): MutableList<JournalEntry> {
        val prefs = ctx.getSharedPreferences("ftt", Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val type = object : TypeToken<MutableList<JournalEntry>>() {}.type
            gson.fromJson(json, type) ?: mutableListOf()
        } catch (e: Exception) { mutableListOf() }
    }

    fun save(ctx: Context, list: List<JournalEntry>) {
        val trimmed = if (list.size > MAX) list.takeLast(MAX) else list
        ctx.getSharedPreferences("ftt", Context.MODE_PRIVATE).edit()
            .putString(KEY, gson.toJson(trimmed)).apply()
    }

    fun addFromSignal(ctx: Context, sig: ParsedSignal) {
        if (sig.label != "BUY" && sig.label != "SELL") return
        val list = getAll(ctx)
        // avoid duplicate same timestamp
        if (list.any { it.ts == parseTs(sig.timestamp) }) return
        val entry = JournalEntry(
            id       = UUID.randomUUID().toString(),
            pair     = sig.pair,
            dir      = sig.label,
            conf     = sig.confidence,
            sess     = sig.sessionLabel,
            grade    = sig.grade,
            ep       = sig.entryPrice,
            sl       = null,
            tp       = null,
            result   = "PENDING",
            pips     = null,
            note     = "",
            ts       = parseTs(sig.timestamp),
            isAuto   = true,
            exitPrice = null,
            closedTs  = null
        )
        list.add(0, entry)
        save(ctx, list)
    }

    fun markResult(ctx: Context, id: String, result: String, exitPrice: String? = null, pips: Double? = null) {
        val list = getAll(ctx)
        val idx = list.indexOfFirst { it.id == id }
        if (idx == -1) return
        list[idx] = list[idx].copy(
            result    = result,
            exitPrice = exitPrice,
            pips      = pips,
            closedTs  = System.currentTimeMillis()
        )
        save(ctx, list)
    }

    fun updateNote(ctx: Context, id: String, note: String) {
        val list = getAll(ctx)
        val idx = list.indexOfFirst { it.id == id }
        if (idx == -1) return
        list[idx] = list[idx].copy(note = note)
        save(ctx, list)
    }

    fun delete(ctx: Context, id: String) {
        val list = getAll(ctx)
        save(ctx, list.filter { it.id != id })
    }

    fun exportCsv(ctx: Context): String {
        val list = getAll(ctx)
        val sb = StringBuilder("Date,Pair,Direction,Confidence,Grade,Session,Entry,SL,TP,Result,Pips,Note\n")
        list.forEach { e ->
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
                .format(java.util.Date(e.ts))
            sb.append("$date,${e.pair},${e.dir},${e.conf}%,${e.grade},${e.sess},")
            sb.append("${e.ep ?: ""},${e.sl ?: ""},${e.tp ?: ""},${e.result},")
            sb.append("${e.pips ?: ""},\"${e.note.replace("\"", "'")}\"\n")
        }
        return sb.toString()
    }

    private fun parseTs(ts: String): Long {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                .parse(ts)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
                    .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                    .parse(ts)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) { System.currentTimeMillis() }
        }
    }
}
