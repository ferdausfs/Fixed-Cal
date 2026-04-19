package com.trading.signalapp.ui.watchlist

import android.view.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trading.signalapp.R
import com.trading.signalapp.databinding.ItemWatchlistBinding
import com.trading.signalapp.model.WatchlistItem

class WatchlistAdapter(
    val onRemove: (String) -> Unit,
    val onSignalClick: (String) -> Unit
) : ListAdapter<WatchlistItem, WatchlistAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemWatchlistBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    inner class VH(private val b: ItemWatchlistBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: WatchlistItem) {
            val ctx = b.root.context
            b.tvPair.text = item.pair
            if (item.isScanning) {
                b.tvDirection.text = "⟳"
                b.tvConfidence.text = "..."
                b.progressConf.progress = 0
            } else {
                val sig = item.signal
                b.tvDirection.text = sig?.label ?: "—"
                b.tvConfidence.text = if (sig != null) "${sig.confidence}%" else "—"
                b.progressConf.progress = sig?.confidence ?: 0
                val colorId = when (sig?.label) {
                    "BUY"  -> R.color.signal_buy
                    "SELL" -> R.color.signal_sell
                    else   -> R.color.text_secondary
                }
                b.tvDirection.setTextColor(ContextCompat.getColor(ctx, colorId))
                b.tvGrade.text = sig?.grade ?: ""
                b.tvSession.text = sig?.sessionLabel ?: ""
            }
            b.btnRemove.setOnClickListener { onRemove(item.pair) }
            b.root.setOnClickListener { onSignalClick(item.pair) }
        }
    }

    class Diff : DiffUtil.ItemCallback<WatchlistItem>() {
        override fun areItemsTheSame(a: WatchlistItem, b: WatchlistItem) = a.pair == b.pair
        override fun areContentsTheSame(a: WatchlistItem, b: WatchlistItem) = a == b
    }
}
