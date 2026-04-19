package com.trading.signalapp.ui.dashboard

import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.trading.signalapp.R
import com.trading.signalapp.api.Result
import com.trading.signalapp.databinding.FragmentDashboardBinding
import com.trading.signalapp.model.HealthResponse
import com.trading.signalapp.viewmodel.MainViewModel

class DashboardFragment : Fragment() {
    private var _b: FragmentDashboardBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentDashboardBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        vm.health.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> { b.progressBar.visibility = View.VISIBLE; b.scrollContent.visibility = View.GONE }
                is Result.Success -> { b.progressBar.visibility = View.GONE; b.scrollContent.visibility = View.VISIBLE; updateUI(result.data) }
                is Result.Error   -> { b.progressBar.visibility = View.GONE; b.tvError.text = result.message; b.tvError.visibility = View.VISIBLE }
                else -> {}
            }
        }
        b.swipeRefresh.setOnRefreshListener { vm.loadHealth(); b.swipeRefresh.isRefreshing = false }
        vm.loadHealth()
    }

    private fun updateUI(data: HealthResponse) {
        b.tvError.visibility = View.GONE
        val isHealthy = data.status == "healthy"
        b.tvServerStatus.text = if (isHealthy) "● ONLINE" else "● OFFLINE"
        b.tvServerStatus.setTextColor(ContextCompat.getColor(requireContext(),
            if (isHealthy) R.color.signal_buy else R.color.signal_sell))
        b.tvServerVersion.text = "v${data.version ?: "?"}"

        data.currentSession?.let { s ->
            b.tvSession.text = s.sessions?.joinToString(" + ")?.ifEmpty { "OFF MARKET" } ?: "—"
            b.tvSessionQuality.text = s.quality ?: "—"
            b.tvSessionQuality.setTextColor(ContextCompat.getColor(requireContext(), when (s.quality) {
                "HIGHEST" -> R.color.signal_buy
                "HIGH"    -> R.color.quality_high
                else      -> R.color.quality_medium
            }))
        }

        data.newsBlackout?.let { n ->
            if (n.blocked == true) {
                b.tvNewsBlackout.visibility = View.VISIBLE
                b.tvNewsBlackout.text = "⚠ NEWS BLACKOUT: ${n.label}"
            } else b.tvNewsBlackout.visibility = View.GONE
        }

        data.markets?.forex?.let { f ->
            val open = f.status?.contains("OPEN", ignoreCase = true) == true
            b.tvForexStatus.text = f.status ?: "—"
            b.tvForexStatus.setTextColor(ContextCompat.getColor(requireContext(),
                if (open) R.color.signal_buy else R.color.text_secondary))
        }
        data.markets?.crypto?.let {
            b.tvCryptoStatus.text = "ALWAYS OPEN 24/7"
            b.tvCryptoStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.signal_buy))
        }

        data.filters?.let { f ->
            b.tvMinConfidence.text = f.minConfidenceFloor ?: "—"
            b.tvVolumeSpike.text   = f.volumeSpikeMultiplier ?: "—"
            b.tvNewsMargin.text    = f.newsBlackoutMargin ?: "—"
        }

        data.apiKeys?.let { b.tvApiKeys.text = "${it.configured ?: 0} API keys configured" }
        data.indicators?.let { b.tvIndicatorCount.text = "${it.size} active indicators" }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
