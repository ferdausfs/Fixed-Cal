package com.trading.signalapp.ui.journal

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.trading.signalapp.databinding.FragmentJournalBinding
import com.trading.signalapp.model.JournalEntry
import com.trading.signalapp.util.JournalStore
import com.trading.signalapp.viewmodel.MainViewModel
import java.io.File

class JournalFragment : Fragment() {
    private var _b: FragmentJournalBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()
    private lateinit var adapter: JournalAdapter
    private var currentFilter = "all"

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentJournalBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        adapter = JournalAdapter(
            onResult = { entry, result -> vm.markJournalResult(entry.id, result) },
            onDelete = { entry ->
                vm.deleteJournalEntry(entry.id)
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
            }
        )
        b.recyclerJournal.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerJournal.adapter = adapter

        b.swipeRefresh.setOnRefreshListener {
            vm.loadJournal()
            b.swipeRefresh.isRefreshing = false
        }

        // Filter buttons
        setupFilter(b.btnFilterAll,  "all")
        setupFilter(b.btnFilterWin,  "WIN")
        setupFilter(b.btnFilterLoss, "LOSS")
        setupFilter(b.btnFilterBuy,  "BUY")
        setupFilter(b.btnFilterSell, "SELL")

        b.btnExportCsv.setOnClickListener { exportCsv() }

        vm.journal.observe(viewLifecycleOwner) { list -> updateList(list) }
        vm.loadJournal()
    }

    private fun setupFilter(btn: android.widget.TextView, filter: String) {
        btn.setOnClickListener {
            currentFilter = filter
            listOf(b.btnFilterAll, b.btnFilterWin, b.btnFilterLoss, b.btnFilterBuy, b.btnFilterSell)
                .forEach { it.isSelected = false }
            btn.isSelected = true
            vm.loadJournal()
        }
    }

    private fun updateList(list: List<JournalEntry>) {
        val filtered = when (currentFilter) {
            "all"  -> list
            "WIN","LOSS","PENDING" -> list.filter { it.result == currentFilter }
            "BUY","SELL"          -> list.filter { it.dir == currentFilter }
            else   -> list
        }
        b.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        b.recyclerJournal.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        adapter.submitList(filtered)

        // Stats
        val wins   = list.count { it.result == "WIN" }
        val losses = list.count { it.result == "LOSS" }
        val total  = wins + losses
        val wr = if (total > 0) (wins * 100 / total) else 0
        b.tvStats.text = "Total: ${list.size}  ✅ $wins  ❌ $losses  WR: $wr%"
    }

    private fun exportCsv() {
        val csv = JournalStore.exportCsv(requireContext())
        val file = File(requireContext().cacheDir, "ftt_journal.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(requireContext(),
            "${requireContext().packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Export Journal CSV"))
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
