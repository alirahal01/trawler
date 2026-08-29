package io.github.alirahal01.trawler.sample

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MARKET_API = "https://api.coingecko.com/api/v3/coins/markets"
private const val FX_API = "https://api.frankfurter.dev/v1/latest"

/** A holding in the demo user's portfolio, priced live against [MARKET_API]. */
data class Holding(val coinId: String, val units: Double)

val demoPortfolio = listOf(
    Holding("bitcoin", units = 0.42),
    Holding("ethereum", units = 3.1),
    Holding("solana", units = 18.0),
    Holding("cardano", units = 2500.0),
    Holding("dogecoin", units = 12_000.0),
)

@Serializable
data class MarketCoinDto(
    val id: String,
    val symbol: String,
    val name: String,
    @SerialName("current_price") val currentPrice: Double,
    @SerialName("price_change_percentage_24h") val changePercent24h: Double? = null,
)

@Serializable
data class FxRatesDto(
    val base: String,
    val date: String,
    val rates: Map<String, Double>,
)

/** Fetches live market and FX data through [client] so the calls surface in the Trawler monitor. */
class WalletRepository(private val client: HttpClient) {

    suspend fun fetchMarket(): List<MarketCoinDto> = client.get(MARKET_API) {
        parameter("vs_currency", "usd")
        parameter("ids", demoPortfolio.joinToString(",") { it.coinId })
        parameter("order", "market_cap_desc")
    }.body()

    suspend fun fetchFxRates(): FxRatesDto = client.get(FX_API) {
        parameter("amount", "1")
        parameter("from", "USD")
        parameter("to", "EUR,GBP,JPY")
    }.body()
}
