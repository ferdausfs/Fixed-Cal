package com.ftt.signal.ui.screens

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.ftt.signal.data.local.AppPrefs
import com.ftt.signal.data.model.HistoryTrade
import com.ftt.signal.data.model.JournalEntry
import com.ftt.signal.data.model.SignalDirection
import com.ftt.signal.data.model.SignalSnapshot
import com.ftt.signal.data.model.label
import com.ftt.signal.data.model.nowStamp
import com.ftt.signal.data.remote.SignalRepository
import com.ftt.signal.worker.SignalScanService
import kotlinx.coroutines.launch

private data class BottomTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val Tabs = listOf(
    BottomTab("Signal", Icons.Default.SignalCellularAlt),
    BottomTab("Watchlist", Icons.Default.ShowChart),
    BottomTab("Journal", Icons.Default.Bookmark),
    BottomTab("Analytics", Icons.Default.Analytics),
    BottomTab("Settings", Icons.Default.Settings)
)

private val MajorPairs = listOf(
    "EUR/USD", "GBP/USD", "USD/JPY", "AUD/USD", "USD/CAD", "USD/CHF", "NZD/USD", "EUR/GBP", "EUR/JPY", "GBP/JPY"
)
private val CryptoPairs = listOf("BTC/USD", "ETH/USD", "XRP/USD", "BNB/USD", "SOL/USD", "ADA/USD", "DOGE/USD")
private val OtcPairs = listOf(
    "EUR/USD-OTC", "GBP/USD-OTC", "USD/JPY-OTC", "AUD/USD-OTC", "USD/CAD-OTC", "USD/CHF-OTC", "NZD/USD-OTC"
)
private val AllPairs = (MajorPairs + CryptoPairs + OtcPairs).distinct()

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.applicationContext
    private val prefs = AppPrefs(app)

    var selectedTab by mutableIntStateOf(0)
    var selectedPair by mutableStateOf(prefs.selectedPair)
    var apiBase by mutableStateOf(prefs.apiBase)
    var currentSignal by mutableStateOf<SignalSnapshot?>(null)
    var recentHistory by mutableStateOf<List<HistoryTrade>>(emptyList())
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var notificationsEnabled by mutableStateOf(prefs.notificationsEnabled)
    var vibrationEnabled by mutableStateOf(prefs.vibrationEnabled)
    var scanIntervalMinutes by mutableIntStateOf(prefs.scanIntervalMinutes)
    var scanEnabled by mutableStateOf(prefs.scanEnabled)

    val journal = mutableStateListOf<JournalEntry>().apply { addAll(prefs.loadJournal()) }
    val watchlist = mutableStateListOf<String>().apply { addAll(prefs.watchlist.sorted()) }

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            runCatching {
                val repo = SignalRepository(apiBase)
                repo.fetchSignal(selectedPair) to repo.fetchHistory(selectedPair)
            }.onSuccess { (signal, history) ->
                currentSignal = signal
                recentHistory = history
                vibrateIfNeeded(signal.direction)
            }.onFailure {
                errorMessage = it.message ?: "Failed to load signal"
            }
            isLoading = false
        }
    }

    fun selectPair(pair: String) {
        selectedPair = pair
        prefs.selectedPair = pair
        refresh()
    }

    fun saveApiBase() {
        prefs.apiBase = apiBase
        refresh()
    }

    fun toggleWatchlist(pair: String) {
        if (watchlist.contains(pair)) watchlist.remove(pair) else watchlist.add(pair)
        prefs.watchlist = watchlist.toSet()
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        prefs.notificationsEnabled = enabled
    }

    fun toggleVibration(enabled: Boolean) {
        vibrationEnabled = enabled
        prefs.vibrationEnabled = enabled
    }

    fun setScanInterval(value: Int) {
        scanIntervalMinutes = value
        prefs.scanIntervalMinutes = value
    }

    fun setScanEnabled(context: Context, enabled: Boolean) {
        scanEnabled = enabled
        prefs.scanEnabled = enabled
        val intent = Intent(context, SignalScanService::class.java)
        if (enabled) context.startForegroundService(intent) else context.stopService(intent)
    }

    fun addCurrentToJournal() {
        val snapshot = currentSignal ?: return
        journal.add(
            0,
            JournalEntry(
                pair = snapshot.pair,
                direction = snapshot.direction,
                confidence = snapshot.confidence,
                entryPrice = snapshot.entryPrice,
                expiryLabel = snapshot.expiryLabel,
                createdAt = nowStamp()
            )
        )
        persistJournal()
    }

    fun setJournalResult(id: String, result: String) {
        val index = journal.indexOfFirst { it.id == id }
        if (index == -1) return
        journal[index] = journal[index].copy(result = result)
        persistJournal()
    }

    private fun persistJournal() {
        prefs.saveJournal(journal)
    }

    val totalTrades: Int get() = journal.size
    val wins: Int get() = journal.count { it.result == "WIN" }
    val losses: Int get() = journal.count { it.result == "LOSS" }
    val winRate: Int get() {
        val decided = wins + losses
        return if (decided == 0) 0 else ((wins * 100f) / decided).toInt()
    }

    private fun vibrateIfNeeded(direction: SignalDirection) {
        if (!vibrationEnabled || direction == SignalDirection.WAIT) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = app.getSystemService(VibratorManager::class.java)
                manager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(
                        if (direction == SignalDirection.BUY) 160 else 250,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = app.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        if (direction == SignalDirection.BUY) 160 else 250,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FttRootApp(vm: AppViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text("FTT Signal Native", fontWeight = FontWeight.Bold)
                        Text(vm.selectedPair, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = vm.selectedTab == index,
                        onClick = { vm.selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (vm.selectedTab) {
            0 -> SignalTab(vm, padding)
            1 -> WatchlistTab(vm, context, padding)
            2 -> JournalTab(vm, padding)
            3 -> AnalyticsTab(vm, padding)
            else -> SettingsTab(vm, context, padding)
        }
    }
}

@Composable
private fun SignalTab(vm: AppViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("Major pairs")
            PairRow(MajorPairs, vm.selectedPair, vm::selectPair)
        }
        item {
            SectionTitle("Crypto")
            PairRow(CryptoPairs, vm.selectedPair, vm::selectPair)
        }
        item {
            SectionTitle("OTC")
            PairRow(OtcPairs, vm.selectedPair, vm::selectPair)
        }
        item {
            if (vm.isLoading) {
                CardBlock {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Loading live signal…")
                    }
                }
            }
        }
        vm.errorMessage?.let { msg ->
            item {
                CardBlock(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)) {
                    Text(msg, color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item {
            vm.currentSignal?.let { signal ->
                CurrentSignalCard(signal = signal, onAddJournal = vm::addCurrentToJournal)
            } ?: CardBlock { Text("No signal loaded yet") }
        }
    }
}

@Composable
private fun WatchlistTab(vm: AppViewModel, context: Context, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardBlock {
                Text("Native background scanner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Select pairs and run a native foreground service that polls the API without WebView.")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = vm.scanEnabled, onCheckedChange = { vm.setScanEnabled(context, it) })
                    Text(if (vm.scanEnabled) "Scanner running" else "Scanner stopped")
                }
                Spacer(Modifier.height(8.dp))
                Text("Interval: ${vm.scanIntervalMinutes} min")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf(1, 3, 5, 10, 15).forEach { interval ->
                        FilterChip(
                            selected = vm.scanIntervalMinutes == interval,
                            onClick = { vm.setScanInterval(interval) },
                            label = { Text("${interval}m") }
                        )
                    }
                }
            }
        }
        item { SectionTitle("Watchlist pairs") }
        items(AllPairs.chunked(2)) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { pair ->
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { vm.toggleWatchlist(pair) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (vm.watchlist.contains(pair)) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pair, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (vm.watchlist.contains(pair)) {
                                AssistChip(onClick = { vm.toggleWatchlist(pair) }, label = { Text("Added") })
                            }
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun JournalTab(vm: AppViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardBlock {
                Text("Trade journal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Save the current signal locally and mark the final outcome later.")
                Spacer(Modifier.height(10.dp))
                Button(onClick = vm::addCurrentToJournal, enabled = vm.currentSignal != null) {
                    Text("Add current signal")
                }
            }
        }
        if (vm.journal.isEmpty()) {
            item { CardBlock { Text("No journal entries yet") } }
        } else {
            items(vm.journal, key = { it.id }) { entry ->
                CardBlock {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.pair, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${entry.direction.label()} · ${entry.confidence}% · ${entry.expiryLabel}")
                            Text("Entry ${entry.entryPrice ?: "—"} · ${entry.createdAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DirectionPill(entry.direction)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = entry.result == "WIN", onClick = { vm.setJournalResult(entry.id, "WIN") }, label = { Text("WIN") })
                        FilterChip(selected = entry.result == "LOSS", onClick = { vm.setJournalResult(entry.id, "LOSS") }, label = { Text("LOSS") })
                        FilterChip(selected = entry.result == "PENDING", onClick = { vm.setJournalResult(entry.id, "PENDING") }, label = { Text("PENDING") })
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsTab(vm: AppViewModel, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Trades", vm.totalTrades.toString(), Modifier.weight(1f))
                StatCard("Wins", vm.wins.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("Losses", vm.losses.toString(), Modifier.weight(1f))
                StatCard("Win rate", "${vm.winRate}%", Modifier.weight(1f))
            }
        }
        item {
            CardBlock {
                Text("Recent remote history", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (vm.recentHistory.isEmpty()) {
                    Text("No remote history available for the selected pair yet.")
                } else {
                    vm.recentHistory.forEach { trade ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${trade.pair} · ${trade.direction}", fontWeight = FontWeight.SemiBold)
                                Text("${trade.confidence} · ${trade.bestTf} · ${trade.timestamp}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(trade.result)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(vm: AppViewModel, context: Context, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CardBlock {
                Text("API configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = vm.apiBase,
                    onValueChange = { vm.apiBase = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL") },
                    singleLine = true
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    vm.saveApiBase()
                    Toast.makeText(context, "API base saved", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Save & refresh")
                }
            }
        }
        item {
            CardBlock {
                SettingSwitchRow("Notifications", vm.notificationsEnabled) { vm.toggleNotifications(it) }
                SettingSwitchRow("Vibration", vm.vibrationEnabled) { vm.toggleVibration(it) }
            }
        }
        item {
            CardBlock {
                Text("Selected watchlist: ${vm.watchlist.size}")
                Spacer(Modifier.height(6.dp))
                Text(vm.watchlist.joinToString())
            }
        }
    }
}

@Composable
private fun CurrentSignalCard(signal: SignalSnapshot, onAddJournal: () -> Unit) {
    val tint = when (signal.direction) {
        SignalDirection.BUY -> Color(0xFF34D97B)
        SignalDirection.SELL -> Color(0xFFFF5370)
        SignalDirection.WAIT -> Color(0xFFF5A623)
    }
    CardBlock {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Text(signal.pair, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${signal.sessionLabel} · ${signal.marketRegime}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DirectionPill(signal.direction)
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMini("Confidence", "${signal.confidence}%")
            StatMini("Grade", signal.grade)
            StatMini("Expiry", signal.expiryLabel)
        }
        Spacer(Modifier.height(12.dp))
        signal.entryPrice?.let {
            Text("Entry price: $it", color = tint, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
        }
        Text("Reasons", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        signal.reasons.forEach { reason ->
            Text("• $reason", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(12.dp))
        Text("Timeframe analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        signal.timeframes.forEach { tf ->
            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tf.timeframe, fontWeight = FontWeight.SemiBold)
                    Text("${tf.direction.label()} · ${tf.expiryLabel}")
                }
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { ((tf.scoreUp) / (tf.scoreUp + tf.scoreDown + 0.1)).toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    color = if (tf.direction == SignalDirection.SELL) Color(0xFFFF5370) else Color(0xFF34D97B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text("UP ${tf.scoreUp} · DOWN ${tf.scoreDown}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onAddJournal) { Text("Save to journal") }
        Spacer(Modifier.height(6.dp))
        Text("Updated: ${signal.generatedAt}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PairRow(items: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { pair ->
            FilterChip(
                selected = selected == pair,
                onClick = { onSelect(pair) },
                label = { Text(pair) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun DirectionPill(direction: SignalDirection) {
    val bg = when (direction) {
        SignalDirection.BUY -> Color(0x2234D97B)
        SignalDirection.SELL -> Color(0x22FF5370)
        SignalDirection.WAIT -> Color(0x22F5A623)
    }
    val fg = when (direction) {
        SignalDirection.BUY -> Color(0xFF34D97B)
        SignalDirection.SELL -> Color(0xFFFF5370)
        SignalDirection.WAIT -> Color(0xFFF5A623)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(direction.label(), color = fg, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            Text(value, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun CardBlock(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}
