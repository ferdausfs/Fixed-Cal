package com.trading.signalapp.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.trading.signalapp.api.Result
import com.trading.signalapp.databinding.FragmentHistoryBinding
import com.trading.signalapp.viewmodel.MainViewModel

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = HistoryAdapter()
        binding.recyclerHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHistory.adapter = adapter

        viewModel.history.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvEmpty.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    if (result.data.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = "No history found."
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                        adapter.submitList(result.data)
                    }
                    viewModel.loadStats(viewModel.selectedPair.value ?: "BTC/USD")
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Error: ${result.message}"
                }
            }
        }

        viewModel.stats.observe(viewLifecycleOwner) { result ->
            if (result is Result.Success) {
                val s = result.data
                val wr = s.winRate?.let { "%.1f%%".format(it * 100) } ?: "—"
                binding.tvWinRate.text = "Win Rate: $wr  |  Total: ${s.totalSignals ?: 0}"
            }
        }

        viewModel.selectedPair.observe(viewLifecycleOwner) { binding.tvCurrentPair.text = it }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadHistory(viewModel.selectedPair.value ?: "BTC/USD")
        }
        viewModel.loadHistory(viewModel.selectedPair.value ?: "BTC/USD")
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
