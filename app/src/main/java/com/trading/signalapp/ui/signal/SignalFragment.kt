package com.trading.signalapp.ui.signal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.trading.signalapp.R
import com.trading.signalapp.api.ApiResult
import com.trading.signalapp.databinding.FragmentSignalBinding
import com.trading.signalapp.model.Signal
import com.trading.signalapp.viewmodel.MainViewModel
import kotlin.math.roundToInt

class SignalFragment : Fragment() {
    private var _binding: FragmentSignalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private val allPairs = listOf(
        "BTC/USD","ETH/USD","BNB/USD","XRP/USD","SOL/USD",
        "ADA/USD","DOGE/USD","AVAX/USD","DOT/USD","LINK/USD",
        "EUR/USD","GBP/USD","USD/JPY","AUD/USD",
        "USD/CAD","EUR/GBP","USD/CHF","NZD/USD"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.spinnerPair.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, allPairs)
        val idx = allPairs.indexOf(viewModel.selectedPair.value ?: "BTC/USD")
        if (idx >= 0) binding.spinnerPair.setSelection(idx)

        viewModel.signal.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Loading -> { binding.progressBar.visibility = View.VISIBLE; binding.cardSignal.visibility = View.GONE; binding.tvNoSignal.visibility = View.GONE }
                is ApiResult.Success -> { binding.progressBar.visibility = View.GONE; result.data?.let { updateSignalUI(it) } ?: showNoSignal() }
                is ApiResult.Error   -> { binding.progressBar.visibility = View.GONE; binding.tvError.text = result.message; binding.tvError.visibility = View.VISIBLE }
            }
        }

        binding.btnGetSignal.setOnClickListener {
            val pair = allPairs[binding.spinnerPair.selectedItemPosition]
            viewModel.setSelectedPair(pair)
            binding.tvError.visibility = View.GONE
            binding.tvNoSignal.visibility = View.GONE
            viewModel.loadSignal(pair)
        }

        binding.swipeRefresh.setOnRefreshListener {
            val pair = allPairs[binding.spinnerPair.selectedItemPosition]
            viewModel.loadSignal(pair)
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun updateSignalUI(signal: Signal) {
        binding.cardSignal.visibility = View.VISIBLE
        binding.tvNoSignal.visibility = View.GONE
        binding.tvSignalPair.text = signal.pair
        binding.tvDirection.text = signal.direction
        val isBuy = signal.direction.equals("BUY", ignoreCase = true)
        val color = ContextCompat.getColor(requireContext(), if (isBuy) R.color.signal_buy else R.color.signal_sell)
        binding.tvDirection.setTextColor(color)
        binding.cardSignal.strokeColor = color
        val pct = (signal.confidence * 100).roundToInt().coerceIn(0, 100)
        binding.tvConfidence.text = "$pct%"
        binding.progressConfidence.progress = pct
        binding.tvEntry.text      = signal.entry?.let { formatPrice(it) } ?: "—"
        binding.tvTakeProfit.text = signal.takeProfit?.let { formatPrice(it) } ?: "—"
        binding.tvStopLoss.text   = signal.stopLoss?.let { formatPrice(it) } ?: "—"
        binding.tvSession.text    = signal.session ?: "—"
        binding.tvTimeframe.text  = signal.timeframe ?: "—"
        binding.tvIndicatorsUsed.text = signal.indicators?.joinToString(", ") ?: "—"
        binding.tvTimestamp.text = (signal.timestamp ?: signal.expiry ?: "—").take(19).replace("T", " ")
    }

    private fun showNoSignal() {
        binding.cardSignal.visibility = View.GONE
        binding.tvNoSignal.visibility = View.VISIBLE
        binding.tvNoSignal.text = "No signal available. Try another pair or try again later."
    }

    private fun formatPrice(p: Double) = if (p > 100) "%.2f".format(p) else "%.5f".format(p)

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
