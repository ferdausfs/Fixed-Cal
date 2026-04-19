package com.trading.signalapp.ui.analytics

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.trading.signalapp.databinding.FragmentAnalyticsBinding
import com.trading.signalapp.model.JournalEntry
import com.trading.signalapp.viewmodel.MainViewModel

class AnalyticsFragment : Fragment() {
    private var _b: FragmentAnalyticsBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAnalyticsBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        vm.journal.observe(viewLifecycleOwner) { list -> updateAnalytics(list) }
        vm.loadJournal()
    }

    private fun updateAnalytics(list: List<JournalEntry>) {
        val closed = list.filter { it.result == "WIN" || it.result == "LOSS" }
        val wins   = closed.count { it.result == "WIN" }
        val losses = closed.count { it.result == "LOSS" }
        val total  = wins + losses
        val wr     = if (total > 0) wins * 100 / total else 0
        val netPips = list.mapNotNull { it.pips }.sum()

        b.tvTotalTrades.text = total.toString()
        b.tvWins.text = wins.toString()
        b.tvLosses.text = losses.toString()
        b.tvWinRate.text = "$wr%"
        b.tvNetPips.text = (if (netPips >= 0) "+" else "") + "%.1f".format(netPips)
        b.tvPending.text = list.count { it.result == "PENDING" }.toString()

        b.progressWinRate.progress = wr

        // Session breakdown
        val sessions = listOf("London", "New York", "Asia", "OTC 24/7")
        val sessStats = sessions.map { sess ->
            val sessT = closed.filter { it.sess.contains(sess, ignoreCase = true) }
            val sessW = sessT.count { it.result == "WIN" }
            Triple(sess, sessW, sessT.size)
        }
        b.tvSessBreakdown.text = sessStats
            .filter { it.third > 0 }
            .joinToString("\n") { (s, w, t) ->
                val wr2 = if (t > 0) w * 100 / t else 0
                "$s: $wr2% ($w/$t)"
            }.ifEmpty { "No data yet" }

        // Grade breakdown
        val grades = list.groupBy { it.grade.ifEmpty { "—" } }
            .map { (g, entries) ->
                val c = entries.filter { it.result == "WIN" || it.result == "LOSS" }
                val w = c.count { it.result == "WIN" }
                val wr2 = if (c.isNotEmpty()) w * 100 / c.size else 0
                "$g: $wr2% (${entries.size} trades)"
            }
        b.tvGradeBreakdown.text = grades.joinToString("\n").ifEmpty { "No data yet" }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
