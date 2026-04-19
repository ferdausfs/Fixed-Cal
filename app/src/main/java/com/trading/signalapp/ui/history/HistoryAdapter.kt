package com.trading.signalapp.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trading.signalapp.R
import com.trading.signalapp.databinding.ItemHistoryBinding
import com.trading.signalapp.model.HistoryItem
import kotlin.math.roundToInt

class HistoryAdapter : ListAdapter<HistoryItem, HistoryAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    class VH(private val b: ItemHistoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: HistoryItem) {
            b.tvPair.text = item.pair
            b.tvDirection.text = item.direction
            b.tvDirection.setTextColor(ContextCompat.getColor(b.root.context,
                if (item.direction.equals("BUY", ignoreCase = true)) R.color.signal_buy else R.color.signal_sell))
            b.tvConfidence.text = "${(item.confidence * 100).roundToInt().coerceIn(0, 100)}%"
            val result = item.result ?: "PENDING"
            b.tvResult.text = result
            b.tvResult.setTextColor(ContextCompat.getColor(b.root.context,
                when (result.uppercase()) { "WIN" -> R.color.signal_buy; "LOSS" -> R.color.signal_sell; else -> R.color.quality_medium }))
            b.tvTimestamp.text = item.timestamp?.take(16)?.replace("T", " ") ?: "—"
        }
    }

    class Diff : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(a: HistoryItem, b: HistoryItem) = a.id == b.id
        override fun areContentsTheSame(a: HistoryItem, b: HistoryItem) = a == b
    }
}
