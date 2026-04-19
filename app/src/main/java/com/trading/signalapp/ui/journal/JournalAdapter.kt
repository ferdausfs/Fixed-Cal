package com.trading.signalapp.ui.journal

import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.trading.signalapp.R
import com.trading.signalapp.databinding.ItemJournalBinding
import com.trading.signalapp.model.JournalEntry
import java.text.SimpleDateFormat
import java.util.*

class JournalAdapter(
    val onResult: (JournalEntry, String) -> Unit,
    val onDelete: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.VH>(Diff()) {

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        VH(ItemJournalBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    inner class VH(private val b: ItemJournalBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(e: JournalEntry) {
            val isBuy = e.dir == "BUY"
            val ctx = b.root.context
            b.tvPair.text = e.pair
            b.tvDir.text = e.dir
            b.tvDir.setTextColor(ContextCompat.getColor(ctx,
                if (isBuy) R.color.signal_buy else R.color.signal_sell))
            b.tvConf.text = "${e.conf}%"
            b.tvGrade.text = e.grade.ifEmpty { "—" }
            b.tvSession.text = e.sess
            b.tvResult.text = e.result
            b.tvResult.setTextColor(ContextCompat.getColor(ctx, when (e.result) {
                "WIN"  -> R.color.signal_buy
                "LOSS" -> R.color.signal_sell
                else   -> R.color.quality_medium
            }))
            val fmt = SimpleDateFormat("MMM dd HH:mm", Locale.US)
            b.tvTime.text = fmt.format(Date(e.ts))
            if (!e.note.isNullOrEmpty()) { b.tvNote.text = e.note; b.tvNote.visibility = View.VISIBLE }
            else b.tvNote.visibility = View.GONE

            b.btnWin.setOnClickListener  { onResult(e, "WIN") }
            b.btnLoss.setOnClickListener { onResult(e, "LOSS") }
            b.btnDelete.setOnClickListener { onDelete(e) }
        }
    }

    class Diff : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(a: JournalEntry, b: JournalEntry) = a.id == b.id
        override fun areContentsTheSame(a: JournalEntry, b: JournalEntry) = a == b
    }
}
