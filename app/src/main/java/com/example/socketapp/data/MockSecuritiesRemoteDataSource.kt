package com.example.socketapp.data

import com.example.socketapp.Security
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay

class MockSecuritiesRemoteDataSource @Inject constructor() : SecuritiesRemoteDataSource {
    override suspend fun fetchSecurities(currentSecurities: List<Security>?): List<Security> {
        delay(350)

        val securities = currentSecurities ?: buildMockSecurities()
        return securities.map { security ->
            val movement = BigDecimal(Random.nextDouble(-2.4, 2.4)).setScale(2, RoundingMode.HALF_UP)
            val nextPrice = (security.price + movement).coerceAtLeast(BigDecimal("1.00"))
            val percent = movement
                .divide(security.price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
            val (ask, bid) = nextPrice.toMockAskBid()

            security.copy(
                price = nextPrice.setScale(2, RoundingMode.HALF_UP),
                priceChange = movement,
                dailyVariationPercent = percent,
                askPrice00 = ask,
                askPrice24 = ask,
                bidPrice00 = bid,
                bidPrice24 = bid,
                minBuyArsAmount = BigDecimal(MOCK_MIN_BUY_ARS_AMOUNT),
            )
        }
    }

    private fun buildMockSecurities(): List<Security> {
        val symbols = listOf(
            "ALUA", "BBAR", "BMA", "BYMA", "CEPU", "COME", "CRES", "EDN", "MOLI", "LOMA",
            "MIRG", "PAMP", "SUPV", "TECO2", "TGNO4", "TGSU2", "TRAN", "TXAR", "VALO", "YPFD",
            "AAPL", "AMZN", "GOOGL", "META", "MSFT", "NVDA", "TSLA", "AMD", "JPM", "V",
        )
        val usdSymbols = setOf("AAPL", "AMZN", "GOOGL", "META", "MSFT", "NVDA", "TSLA", "AMD", "JPM", "V")

        val names = mapOf(
            "ALUA" to "Aluar",
            "BBAR" to "BBVA Argentina",
            "BMA" to "Banco Macro",
            "BYMA" to "Bolsas y Mercados Argentinos",
            "CEPU" to "Central Puerto",
            "COME" to "Sociedad Comercial del Plata",
            "CRES" to "Cresud",
            "EDN" to "Edenor",
            "MOLI" to "Molinos Rio de la Plata",
            "LOMA" to "Loma Negra",
            "MIRG" to "Mirgor",
            "PAMP" to "Pampa Energia",
            "SUPV" to "Grupo Supervielle",
            "TECO2" to "Telecom Argentina",
            "TGNO4" to "Transportadora Gas del Norte",
            "TGSU2" to "Transportadora Gas del Sur",
            "TRAN" to "Transener",
            "TXAR" to "Ternium Argentina",
            "VALO" to "Grupo Financiero Valores",
            "YPFD" to "YPF",
            "AAPL" to "Apple",
            "AMZN" to "Amazon",
            "GOOGL" to "Alphabet",
            "META" to "Meta Platforms",
            "MSFT" to "Microsoft",
            "NVDA" to "Nvidia",
            "TSLA" to "Tesla",
            "AMD" to "Advanced Micro Devices",
            "JPM" to "JPMorgan Chase",
            "V" to "Visa",
        )
        val sectors = listOf("Energia", "Tecnologia", "IA", "Industriales", "Transporte", "Finanzas")

        return List(1_000) { index ->
            val symbol = symbols[index % symbols.size]
            val suffix = index / symbols.size
            val price = randomDecimalCents(4_000, 1_000_001)
            val change = randomDecimalCents(-800, 801)
            val percent = change
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
            val (ask, bid) = price.toMockAskBid()

            Security(
                id = "$symbol-$suffix".hashCode() and Int.MAX_VALUE,
                ticker = if (suffix == 0) symbol else "$symbol$suffix",
                description = names.getValue(symbol),
                type = "Acciones",
                currency = if (symbol in usdSymbols) "USD" else "ARS",
                codeType = "MOCK_SECURITY_ID",
                codeValue = "$symbol-$suffix",
                industry = sectors[index % sectors.size],
                panel = if (index % 4 == 0) "General" else "S&P Merval",
                liderMerval = index % 4 != 0,
                minInstrumentNominals = 1,
                maxInstrumentNominals = 999999999,
                lotInstrumentSize = 1,
                price = price.setScale(2, RoundingMode.HALF_UP),
                priceChange = change,
                dailyVariationPercent = percent,
                askPrice00 = ask,
                askPrice24 = ask,
                bidPrice00 = bid,
                bidPrice24 = bid,
                percentageMovement = BigDecimal("0.015"),
                holdingQuantity = mockHoldingQuantity(symbol, suffix),
                minBuyArsAmount = BigDecimal(MOCK_MIN_BUY_ARS_AMOUNT),
            )
        }
    }

    private fun mockHoldingQuantity(
        symbol: String,
        suffix: Int,
    ): Int =
        if (suffix == 0) {
            when (symbol) {
                "PAMP" -> 125
                "YPFD" -> 80
                "ALUA" -> 250
                else -> 0
            }
        } else {
            0
        }

    private fun randomDecimalCents(fromCents: Long, untilCents: Long): BigDecimal =
        BigDecimal.valueOf(Random.nextLong(fromCents, untilCents), 2)

    private fun BigDecimal.toMockAskBid(): Pair<BigDecimal, BigDecimal> {
        val spread = multiply(BigDecimal("0.0015")).setScale(2, RoundingMode.HALF_UP)
        val ask = add(spread).setScale(2, RoundingMode.HALF_UP)
        val bid = subtract(spread).coerceAtLeast(BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP)
        return ask to bid
    }

    private companion object {
        const val MOCK_MIN_BUY_ARS_AMOUNT = "100.00"
    }
}
