package io.github.alirahal01.trawler.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Result of an in-flight or completed network fetch, kept minimal on purpose for this demo. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

@Composable
fun WalletScreen(
    repository: WalletRepository,
    onOpenMonitor: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var market by remember { mutableStateOf<LoadState<List<MarketCoinDto>>>(LoadState.Loading) }
    var fx by remember { mutableStateOf<LoadState<FxRatesDto>>(LoadState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        isRefreshing = true
        market = runCatching { repository.fetchMarket() }.fold(
            onSuccess = { LoadState.Success(it) },
            onFailure = { LoadState.Error(it.message ?: "Couldn't load market prices") },
        )
        fx = runCatching { repository.fetchFxRates() }.fold(
            onSuccess = { LoadState.Success(it) },
            onFailure = { LoadState.Error(it.message ?: "Couldn't load exchange rates") },
        )
        isRefreshing = false
    }

    LaunchedEffect(Unit) { refresh() }

    WalletScreenContent(
        market = market,
        fx = fx,
        isRefreshing = isRefreshing,
        onRefresh = { scope.launch { refresh() } },
        onOpenMonitor = onOpenMonitor,
        onOpenTools = onOpenTools,
        modifier = modifier,
    )
}

/**
 * Pure rendering of the wallet dashboard given already-resolved state — split out from
 * [WalletScreen] so it can be exercised in `@Preview`s without a real [WalletRepository].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletScreenContent(
    market: LoadState<List<MarketCoinDto>>,
    fx: LoadState<FxRatesDto>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenMonitor: () -> Unit,
    onOpenTools: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { WalletTopBar(onOpenMonitor = onOpenMonitor, onOpenTools = onOpenTools) }
            item { PortfolioCard(market) }
            item { SectionHeader("Markets") }
            when (market) {
                is LoadState.Loading -> item { LoadingRow() }
                is LoadState.Error -> item { ErrorRow(market.message, onRetry = onRefresh) }
                is LoadState.Success -> items(market.data, key = { it.id }) { coin -> MarketRow(coin) }
            }
            item { SectionHeader("Currency exchange") }
            item { FxConverterCard(fx) }
        }
    }
}

@Composable
private fun WalletTopBar(onOpenMonitor: () -> Unit, onOpenTools: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Trawler Wallet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Live crypto & FX demo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row {
            TextButton(onClick = onOpenTools) { Text("Tools ›") }
            TextButton(onClick = onOpenMonitor) { Text("Monitor ›") }
        }
    }
}

@Composable
private fun PortfolioCard(market: LoadState<List<MarketCoinDto>>) {
    val coins = (market as? LoadState.Success)?.data
    val totalValue = coins?.sumOf { coin -> holdingUnits(coin.id) * coin.currentPrice }
    val totalChange = if (coins != null && totalValue != null && totalValue > 0) {
        coins.sumOf { coin ->
            val value = holdingUnits(coin.id) * coin.currentPrice
            (coin.changePercent24h ?: 0.0) * (value / totalValue)
        }
    } else {
        null
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00695C)),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Total balance", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(4.dp))
            Text(
                totalValue?.let { formatUsd(it) } ?: "—",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            totalChange?.let {
                val positive = it >= 0
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${if (positive) "▲" else "▼"} ${formatPercent(it)} today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (positive) Color(0xFFB9F6CA) else Color(0xFFFFCDD2),
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketRow(coin: MarketCoinDto) {
    val units = holdingUnits(coin.id)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CoinAvatar(coin.symbol)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(coin.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${formatNumber(units)} ${coin.symbol.uppercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(formatUsd(coin.currentPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                coin.changePercent24h?.let {
                    val positive = it >= 0
                    Text(
                        "${if (positive) "▲" else "▼"} ${formatPercent(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (positive) Color(0xFF2E7D32) else Color(0xFFC62828),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinAvatar(symbol: String) {
    Box(
        modifier = Modifier.size(36.dp).background(avatarColor(symbol), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol.take(1).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun avatarColor(symbol: String): Color = when (symbol.lowercase()) {
    "btc" -> Color(0xFFF7931A)
    "eth" -> Color(0xFF627EEA)
    "sol" -> Color(0xFF9945FF)
    "ada" -> Color(0xFF0033AD)
    "doge" -> Color(0xFFC2A633)
    else -> Color(0xFF546E7A)
}

private fun holdingUnits(coinId: String): Double = demoPortfolio.firstOrNull { it.coinId == coinId }?.units ?: 0.0

@Composable
private fun FxConverterCard(fx: LoadState<FxRatesDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            when (fx) {
                is LoadState.Loading -> LoadingRow()
                is LoadState.Error -> ErrorRow(fx.message, onRetry = null)
                is LoadState.Success -> {
                    Text(
                        "1 USD equals",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        fx.data.rates.forEach { (currency, rate) ->
                            Column {
                                Text(
                                    currency,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    formatNumber(rate, decimals = 4),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun LoadingRow() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
    }
}

@Composable
private fun ErrorRow(message: String, onRetry: (() -> Unit)?) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        onRetry?.let {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = it) { Text("Retry") }
        }
    }
}

private val previewMarket = listOf(
    MarketCoinDto(id = "bitcoin", symbol = "btc", name = "Bitcoin", currentPrice = 77_195.0, changePercent24h = 0.83),
    MarketCoinDto(id = "ethereum", symbol = "eth", name = "Ethereum", currentPrice = 2_459.04, changePercent24h = 2.04),
    MarketCoinDto(id = "solana", symbol = "sol", name = "Solana", currentPrice = 94.47, changePercent24h = 1.34),
    MarketCoinDto(id = "cardano", symbol = "ada", name = "Cardano", currentPrice = 0.41, changePercent24h = -0.9),
    MarketCoinDto(id = "dogecoin", symbol = "doge", name = "Dogecoin", currentPrice = 0.09, changePercent24h = 0.53),
)

private val previewFx = FxRatesDto(
    base = "USD",
    date = "2026-08-24",
    rates = linkedMapOf("EUR" to 0.85477, "GBP" to 0.73228, "JPY" to 158.7),
)

@Preview
@Composable
private fun WalletScreenLoadedPreview() {
    MaterialTheme(colorScheme = TrawlerColorScheme) {
        WalletScreenContent(
            market = LoadState.Success(previewMarket),
            fx = LoadState.Success(previewFx),
            isRefreshing = false,
            onRefresh = {},
            onOpenMonitor = {},
            onOpenTools = {},
        )
    }
}

@Preview
@Composable
private fun WalletScreenLoadingPreview() {
    MaterialTheme(colorScheme = TrawlerColorScheme) {
        WalletScreenContent(
            market = LoadState.Loading,
            fx = LoadState.Loading,
            isRefreshing = true,
            onRefresh = {},
            onOpenMonitor = {},
            onOpenTools = {},
        )
    }
}

@Preview
@Composable
private fun WalletScreenErrorPreview() {
    MaterialTheme(colorScheme = TrawlerColorScheme) {
        WalletScreenContent(
            market = LoadState.Error("Couldn't reach api.coingecko.com"),
            fx = LoadState.Error("Couldn't reach api.frankfurter.dev"),
            isRefreshing = false,
            onRefresh = {},
            onOpenMonitor = {},
            onOpenTools = {},
        )
    }
}
