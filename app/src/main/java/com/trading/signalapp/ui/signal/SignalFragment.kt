package com.trading.signalapp.ui.signal

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.trading.signalapp.R
import com.trading.signalapp.api.Result
import com.trading.signalapp.databinding.FragmentSignalBinding
import com.trading.signalapp.model.ParsedSignal
import com.trading.signalapp.util.PairUtils
import com.trading.signalapp.viewmodel.MainViewModel
import kotlin.math.roundToInt

class SignalFragment : Fragment() {

    private var _b: FragmentSignalBinding? = null
    private val b get() = _b!!
    private val vm: MainViewModel by activityViewModels()
    private var countdownTimer: CountDownTimer? = null
    private var expiryEndMs = 0L
    private var totalMs = 0L

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSignalBinding.inflate(i, c, false); return b.root
    }

    override fun onViewCreated(view: View, s: Bundle?) {
        super.onViewCreated(view, s)
        setupPairButton()
        setupObservers()
        setupButtons()

        val pair = vm.selectedPair.value ?: "EUR/USD"
        if (!PairUtils.isOpen(pair)) showClosed(pair)
        else vm.loadSignal(pair)
    }

    private fun setupPairButton() {
        b.btnPair.text = vm.selectedPair.value ?: "EUR/USD"
        b.btnPair.setOnClickListener { showPairPicker() }
        vm.selectedPair.observe(viewLifecycleOwner) { b.btnPair.text = it }
    }

    private fun setupObservers() {
        vm.signal.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> showLoading()
                is Result.Success -> {
                    hideLoading()
                    val sig = result.data
                    if (sig.label == "WAIT") showWait(sig)
                    else showSignal(sig)
                }
                is Result.Error -> {
                    hideLoading()
                    if (result.isApiLimit) showApiLimit(result.message)
                    else showError(result.message)
                }
                else -> {}
            }
        }
    }

    private fun setupButtons() {
        b.btnRefresh.setOnClickListener {
            val pair = vm.selectedPair.value ?: "EUR/USD"
            if (!PairUtils.isOpen(pair)) { showClosed(pair); return@setOnClickListener }
            b.btnRefresh.animate().rotationBy(360f).setDuration(500).start()
            vm.loadSignal(pair)
        }
        b.btnSound.setOnClickListener {
            vm.soundOn = !vm.soundOn
            b.btnSound.text = if (vm.soundOn) "🔊" else "🔇"
        }
        b.btnSettings.setOnClickListener { showSettingsSheet() }
        b.btnSound.text = if (vm.soundOn) "🔊" else "🔇"
    }

    private fun showLoading() {
        b.progressBar.visibility = View.VISIBLE
        b.cardSignal.visibility = View.GONE
        b.cardWait.visibility = View.GONE
        b.cardClosed.visibility = View.GONE
        b.cardError.visibility = View.GONE
    }

    private fun hideLoading() { b.progressBar.visibility = View.GONE }

    private fun showSignal(sig: ParsedSignal) {
        b.cardSignal.visibility = View.VISIBLE
        b.cardWait.visibility = View.GONE
        b.cardClosed.visibility = View.GONE
        b.cardError.visibility = View.GONE

        val isBuy = sig.label == "BUY"
        val colorId = if (isBuy) R.color.signal_buy else R.color.signal_sell
        val color = ContextCompat.getColor(requireContext(), colorId)

        // Direction
        b.tvDirection.text = sig.label
        b.tvDirection.setTextColor(color)
        b.tvPair.text = sig.pair
        if (sig.isOtc) b.tvOtcBadge.visibility = View.VISIBLE
        else b.tvOtcBadge.visibility = View.GONE

        // Grade
        if (sig.grade.isNotEmpty()) {
            b.tvGrade.text = sig.grade
            b.tvGrade.visibility = View.VISIBLE
            b.tvGrade.setOnClickListener { showGradeInfo(sig.grade) }
        } else b.tvGrade.visibility = View.GONE

        // Confidence
        b.tvConfidence.text = "${sig.confidence}%"
        b.progressConfidence.progress = sig.confidence
        b.tvConfidence.setTextColor(color)

        // Entry / SL / TP
        b.tvEntry.text = sig.entryPrice ?: "—"
        if (sig.entryPrice != null) {
            val ep = sig.entryPrice.toDoubleOrNull()
            if (ep != null) {
                val sl = PairUtils.calcSL(sig.pair, sig.label, ep, vm.slPips)
                val tp = PairUtils.calcTP(sig.pair, sig.label, ep, vm.tpPips)
                b.tvSl.text = PairUtils.formatPrice(sig.pair, sl)
                b.tvTp.text = PairUtils.formatPrice(sig.pair, tp)
                b.tvSlPips.text = "${vm.slPips.toInt()}p"
                b.tvTpPips.text = "${vm.tpPips.toInt()}p"
                b.tvRr.text = "1:${"%.1f".format(vm.tpPips / vm.slPips)}"
            }
        }
        b.btnSltpSet.setOnClickListener { showSltpSheet(sig) }

        // Scores
        val total = (sig.buyScore + sig.sellScore).coerceAtLeast(1)
        b.buyBar.progress = (sig.buyScore * 100 / total).coerceIn(0, 100)
        b.sellBar.progress = (sig.sellScore * 100 / total).coerceIn(0, 100)
        b.tvBuyScore.text = sig.buyScore.toString()
        b.tvSellScore.text = sig.sellScore.toString()

        // Meta
        b.tvSession.text = sig.sessionLabel
        b.tvTfAgreement.text = sig.tfAgreement
        b.tvH1.text = sig.h1Structure
        b.tvAtr.text = sig.atrLevel
        if (sig.marketRegime.isNotEmpty()) {
            b.tvRegime.text = sig.marketRegime; b.tvRegime.visibility = View.VISIBLE
        } else b.tvRegime.visibility = View.GONE

        // Reasons
        if (sig.reasons.isNotEmpty()) {
            b.tvReasons.text = sig.reasons.joinToString("\n• ", "• ")
            b.cardReasons.visibility = View.VISIBLE
        } else b.cardReasons.visibility = View.GONE

        // AI validation
        sig.aiValidation?.let { ai ->
            b.cardAi.visibility = View.VISIBLE
            b.tvAiStatus.text = when (ai.status) {
                "agree"     -> "✅ AI Agrees"
                "disagree"  -> "❌ AI Disagrees"
                else        -> "⚠️ AI Uncertain"
            }
            val aiColorId = when (ai.status) {
                "agree"    -> R.color.signal_buy
                "disagree" -> R.color.signal_sell
                else       -> R.color.quality_medium
            }
            b.tvAiStatus.setTextColor(ContextCompat.getColor(requireContext(), aiColorId))
            b.tvAiReason.text = ai.reason ?: ""
            if (!ai.concerns.isNullOrEmpty()) {
                b.tvAiConcerns.text = "⚠ ${ai.concerns}"; b.tvAiConcerns.visibility = View.VISIBLE
            } else b.tvAiConcerns.visibility = View.GONE
        } ?: run { b.cardAi.visibility = View.GONE }

        // Countdown
        startCountdown(sig.expiryMinutes)

        // Expiry action button
        b.btnOlymp.text = if (isBuy) "▲  PLACE BUY — ${sig.expirySuggestion}" else "▼  PLACE SELL — ${sig.expirySuggestion}"
        b.btnOlymp.setBackgroundColor(color)

        // Sound
        if (vm.soundOn) playBeep(isBuy)

        // Toast
        showToast("${if (isBuy) "▲" else "▼"} ${sig.label} — ${sig.pair}" +
                if (sig.grade.isNotEmpty()) " [${sig.grade}]" else "")
    }

    private fun showWait(sig: ParsedSignal) {
        b.cardSignal.visibility = View.GONE
        b.cardWait.visibility = View.VISIBLE
        b.cardClosed.visibility = View.GONE
        b.cardError.visibility = View.GONE
        b.tvWaitPair.text = sig.pair
        b.tvWaitConf.text = "${sig.confidence}% confidence"
        b.tvWaitSession.text = sig.sessionLabel
        if (sig.reasons.isNotEmpty())
            b.tvWaitReasons.text = sig.reasons.joinToString("\n• ", "• ")
    }

    private fun showClosed(pair: String) {
        b.cardSignal.visibility = View.GONE
        b.cardWait.visibility = View.GONE
        b.cardClosed.visibility = View.VISIBLE
        b.cardError.visibility = View.GONE
        b.tvClosedPair.text = pair
        b.tvClosedReason.text = PairUtils.whyClosed(pair)
        b.btnClosedBtc.setOnClickListener { vm.selectPair("BTC/USD"); vm.loadSignal("BTC/USD") }
    }

    private fun showError(msg: String) {
        b.cardSignal.visibility = View.GONE
        b.cardWait.visibility = View.GONE
        b.cardClosed.visibility = View.GONE
        b.cardError.visibility = View.VISIBLE
        b.tvError.text = msg
        b.btnRetry.setOnClickListener { vm.loadSignal(vm.selectedPair.value ?: "EUR/USD") }
    }

    private fun showApiLimit(msg: String) {
        showError("⚠️ API Limit: $msg")
    }

    private fun startCountdown(minutes: Int) {
        countdownTimer?.cancel()
        totalMs = minutes * 60 * 1000L
        expiryEndMs = System.currentTimeMillis() + totalMs
        b.tvCountdown.visibility = View.VISIBLE
        countdownTimer = object : CountDownTimer(totalMs, 1000) {
            override fun onTick(ms: Long) {
                val rem = ms / 1000
                val mm = rem / 60; val ss = rem % 60
                b.tvCountdown.text = "$mm:${if (ss < 10) "0" else ""}$ss"
                val pct = (ms.toFloat() / totalMs * 100).roundToInt()
                b.progressCountdown.progress = pct
                val colorId = when {
                    pct > 50 -> R.color.signal_buy
                    pct > 25 -> R.color.quality_medium
                    else     -> R.color.signal_sell
                }
                b.tvCountdown.setTextColor(ContextCompat.getColor(requireContext(), colorId))
            }
            override fun onFinish() {
                b.tvCountdown.text = "0:00"
                vm.loadSignal(vm.selectedPair.value ?: "EUR/USD")
            }
        }.start()
    }

    private fun showGradeInfo(grade: String) {
        val (emoji, label, desc) = com.trading.signalapp.util.SignalParser.gradeInfo(grade)
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.sheet_grade, null)
        v.findViewById<TextView>(R.id.tvGradeEmoji).text = emoji
        v.findViewById<TextView>(R.id.tvGradeLabel).text = "Grade $grade — $label"
        v.findViewById<TextView>(R.id.tvGradeDesc).text = desc
        v.findViewById<View>(R.id.btnGradeClose).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(v)
        dialog.show()
    }

    private fun showSltpSheet(sig: ParsedSignal) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.sheet_sltp, null)
        val slInp = v.findViewById<EditText>(R.id.etSl)
        val tpInp = v.findViewById<EditText>(R.id.etTp)
        slInp.setText(vm.slPips.toInt().toString())
        tpInp.setText(vm.tpPips.toInt().toString())
        v.findViewById<View>(R.id.btnSltpSave).setOnClickListener {
            val sl = slInp.text.toString().toDoubleOrNull() ?: 15.0
            val tp = tpInp.text.toString().toDoubleOrNull() ?: 30.0
            vm.slPips = sl; vm.tpPips = tp
            showSignal(sig)
            dialog.dismiss()
            showToast("SL: ${sl.toInt()}p / TP: ${tp.toInt()}p saved")
        }
        v.findViewById<View>(R.id.btnSltpCancel).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(v)
        dialog.show()
    }

    private fun showSettingsSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.sheet_settings, null)
        val urlInp = v.findViewById<EditText>(R.id.etApiUrl)
        urlInp.setText(vm.savedBaseUrl ?: "https://asignal.umuhammadiswa.workers.dev")
        v.findViewById<View>(R.id.btnSettingsSave).setOnClickListener {
            val url = urlInp.text.toString().trim().trimEnd('/')
            if (url.startsWith("http")) {
                vm.savedBaseUrl = url
                showToast("✅ API URL saved")
                dialog.dismiss()
            } else showToast("❌ URL must start with http")
        }
        v.findViewById<View>(R.id.btnSettingsReset).setOnClickListener {
            vm.savedBaseUrl = null
            urlInp.setText("https://asignal.umuhammadiswa.workers.dev")
            showToast("🔄 Reset to default")
        }
        v.findViewById<View>(R.id.btnSettingsCancel).setOnClickListener { dialog.dismiss() }
        dialog.setContentView(v)
        dialog.show()
    }

    private fun showPairPicker() {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.sheet_pair_picker, null)
        val search = v.findViewById<EditText>(R.id.etSearch)
        val listView = v.findViewById<ListView>(R.id.lvPairs)
        val tabs = listOf(
            v.findViewById<TextView>(R.id.tabAll),
            v.findViewById<TextView>(R.id.tabFx),
            v.findViewById<TextView>(R.id.tabCrypto),
            v.findViewById<TextView>(R.id.tabOtc)
        )
        var currentList = PairUtils.ALL.toMutableList()
        var cat = "all"

        fun refresh(q: String = search.text.toString()) {
            val base = when (cat) {
                "forex"  -> PairUtils.FX
                "crypto" -> PairUtils.CRYPTO
                "otc"    -> PairUtils.OTC
                else     -> PairUtils.ALL
            }
            currentList = if (q.isEmpty()) base.toMutableList()
            else base.filter { it.contains(q.uppercase()) }.toMutableList()
            val adapter = ArrayAdapter(requireContext(),
                android.R.layout.simple_list_item_1, currentList)
            listView.adapter = adapter
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = refresh(s.toString())
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
        })

        tabs.forEachIndexed { i, tab ->
            tab.setOnClickListener {
                cat = listOf("all", "forex", "crypto", "otc")[i]
                tabs.forEach { it.setBackgroundResource(0) }
                tab.setBackgroundResource(R.drawable.tab_selected_bg)
                refresh()
            }
        }

        listView.setOnItemClickListener { _, _, pos, _ ->
            val pair = currentList[pos]
            vm.selectPair(pair)
            if (!PairUtils.isOpen(pair)) showClosed(pair)
            else vm.loadSignal(pair)
            dialog.dismiss()
        }

        refresh()
        dialog.setContentView(v)
        dialog.show()
    }

    private fun playBeep(isBuy: Boolean) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            if (isBuy) {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } else {
                tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
            }
        } catch (_: Exception) {}
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
        _b = null
    }
}
