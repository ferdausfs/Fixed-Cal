package com.trading.signalapp.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.trading.signalapp.R
import com.trading.signalapp.api.ApiResult
import com.trading.signalapp.databinding.FragmentDashboardBinding
import com.trading.signalapp.model.HealthResponse
import com.trading.signalapp.viewmodel.MainViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.health.observe(viewLifecycleOwner) { result ->
            when (result) {
                is ApiResult.Loading -> { binding.progressBar.visibility = View.VISIBLE; binding.scrollContent.visibility = View.GONE }
                is ApiResult.Success -> { binding.progressBar.visibility = View.GONE; binding.tvError.visibility = View.GONE; updateUI(result.data) }
                is ApiResult.Error   -> { binding.progressBar.visibility = View.GONE; binding.tvError.text = result.message; binding.tvError.visibility = View.VISIBLE }
            }
        }
        binding.swipeRefresh.setOnRefreshListener {
            binding.tvError.visibility = View.GONE
            viewModel.loadHealth()
            binding.swipeRefresh.isRefreshing = false
        }
        viewModel.loadHealth()
    }

    private fun updateUI(data: HealthResponse) {
        binding.scrollContent.visibility = View.VISIBLE
        // Server status
        val isHealthy = data.status == "healthy"
        binding.tvServerStatus.text = if (isHealthy) "● ONLINE" else "● OFFLINE"
        binding.tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(), if (isHealthy) R.color.signal_buy else R.color.signal_sell))
        binding.tvServerVersion.text = "v${data.version}"

        // Session
        data.currentSession?.let { s ->
            binding.tvSession.text = s.sessions.joinToString(" + ").ifEmpty { "OFF MARKET" }
            binding.tvSessionQuality.text = s.quality
            binding.tvSessionQuality.setTextColor(ContextCompat.getColor(requireContext(),
                when (s.quality) { "HIGHEST" -> R.color.signal_buy; "HIGH" -> R.color.quality_high; else -> R.color.quality_medium }))
        }

        // News blackout
        data.newsBlackout?.let { n ->
            binding.tvNewsBlackout.visibility = if (n.blocked) View.VISIBLE else View.GONE
            if (n.blocked) binding.tvNewsBlackout.text = "⚠ NEWS BLACKOUT: ${n.label}"
        }

        // Markets
        data.markets?.forex?.let { f ->
            val open = f.status.contains("OPEN", ignoreCase = true)
            binding.tvForexStatus.text = f.status
            binding.tvForexStatus.setTextColor(ContextCompat.getColor(requireContext(), if (open) R.color.signal_buy else R.color.text_secondary))
        }
        data.markets?.crypto?.let {
            binding.tvCryptoStatus.text = "ALWAYS OPEN 24/7"
            binding.tvCryptoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.signal_buy))
        }

        // Filters
        data.filters?.let { f ->
            binding.tvMinConfidence.text = f.minConfidenceFloor
            binding.tvVolumeSpike.text = f.volumeSpikeMultiplier
            binding.tvNewsMargin.text = f.newsBlackoutMargin
        }

        // Indicators
        data.indicators?.let { binding.tvIndicatorCount.text = "${it.size} active indicators" }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
