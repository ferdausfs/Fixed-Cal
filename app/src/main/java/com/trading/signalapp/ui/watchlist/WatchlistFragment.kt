package com.trading.signalapp.ui.watchlist

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trading.signalapp.R
import com.trading.signalapp.api.Result
import com.trading.signalapp.databinding.FragmentWatchlistBinding
import com.trading.signalapp.util.PairUtils
import com.trading.signalapp.viewmodel.MainViewModel

class WatchlistFragment : Fragment() {
    private var _b: FragmentWatchlistBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()
    private lateinit var adapter: WatchlistAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentWatchlistBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        adapter = WatchlistAdapter(
            onRemove = { pair ->
                vm.removeFromWatchlist(pair)
                Toast.makeText(requireContext(), "Removed $pair", Toast.LENGTH_SHORT).show()
            },
            onSignalClick = { pair ->
                vm.selectPair(pair)
                requireActivity().findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                    ?.selectedItemId = R.id.signalFragment
            }
        )
        b.recyclerWatchlist.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerWatchlist.adapter = adapter

        b.btnAddPair.setOnClickListener { showAddPairSheet() }
        b.btnScanAll.setOnClickListener {
            vm.scanWatchlist()
            Toast.makeText(requireContext(), "Scanning...", Toast.LENGTH_SHORT).show()
        }

        vm.watchlist.observe(viewLifecycleOwner) { list ->
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            b.recyclerWatchlist.visibility = if (list.isEmpty()) View.GONE else View.VISIBLE
            adapter.submitList(list)
            // Summary
            val buys  = list.count { it.signal?.label == "BUY" }
            val sells = list.count { it.signal?.label == "SELL" }
            b.tvSummary.text = "▲ BUY: $buys   ▼ SELL: $sells   Total: ${list.size}"
        }

        if (vm.getWatchlistPairs().isNotEmpty()) vm.scanWatchlist()
    }

    private fun showAddPairSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.sheet_pair_picker, null)
        val search = v.findViewById<android.widget.EditText>(R.id.etSearch)
        val listView = v.findViewById<android.widget.ListView>(R.id.lvPairs)
        val current = vm.getWatchlistPairs()
        var currentList = PairUtils.ALL.filter { !current.contains(it) }.toMutableList()

        fun refresh(q: String) {
            currentList = PairUtils.ALL
                .filter { !vm.getWatchlistPairs().contains(it) }
                .filter { q.isEmpty() || it.contains(q.uppercase()) }
                .toMutableList()
            listView.adapter = android.widget.ArrayAdapter(requireContext(),
                android.R.layout.simple_list_item_1, currentList)
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = refresh(s.toString())
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
        listView.setOnItemClickListener { _, _, pos, _ ->
            vm.addToWatchlist(currentList[pos])
            Toast.makeText(requireContext(), "${currentList[pos]} added", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }
        refresh("")
        dialog.setContentView(v)
        dialog.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
