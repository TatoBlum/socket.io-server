package com.example.socketapp.data

import com.example.socketapp.Security
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.delay

open class MockSecuritiesRepository @Inject constructor() {
    private var cachedSecurities: List<Security>? = null

    open fun getCachedSecurities(): List<Security>? = cachedSecurities

    open suspend fun refreshSecurities(): List<Security> {
        delay(350)

        val securities = cachedSecurities ?: buildMockSecurities()
        val refreshedSecurities = securities.map { security ->
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
        cachedSecurities = refreshedSecurities
        return refreshedSecurities
    }

    open suspend fun getBuyableInstruments(): List<Security> =
        cachedSecurities.orEmpty()

    open suspend fun getBuyableInstrument(
        codeType: String,
        codeValue: String,
    ): Security? {
        val securities = cachedSecurities ?: refreshSecurities()
        val instrument = securities.findMockInstrument(
            codeType = codeType,
            codeValue = codeValue,
        )

        if (instrument != null) return instrument

        val fallbackInstrument = buildFallbackInstrument(
            codeType = codeType,
            codeValue = codeValue,
        )
        cachedSecurities = (securities + fallbackInstrument)
            .distinctBy { security -> "${security.codeType}:${security.codeValue}" }
        return fallbackInstrument
    }

    private fun List<Security>.findMockInstrument(
        codeType: String,
        codeValue: String,
    ): Security? {
        val normalizedCodeType = codeType.trim()
        val normalizedCodeValue = codeValue.trim()
        val normalizedTicker = normalizedCodeValue.substringBefore("-")

        return firstOrNull { security ->
            security.codeType.equals(normalizedCodeType, ignoreCase = true) &&
                security.codeValue.equals(normalizedCodeValue, ignoreCase = true)
        } ?: firstOrNull { security ->
            security.codeValue.equals(normalizedCodeValue, ignoreCase = true) ||
                security.ticker.equals(normalizedTicker, ignoreCase = true)
        }
    }

    private fun buildFallbackInstrument(
        codeType: String,
        codeValue: String,
    ): Security {
        val normalizedCodeType = codeType.trim().ifBlank { DEFAULT_CODE_TYPE }
        val normalizedCodeValue = codeValue.trim().ifBlank { DEFAULT_CODE_VALUE }
        val ticker = normalizedCodeValue.substringBefore("-").ifBlank { DEFAULT_TICKER }
        val price = BigDecimal("100.00")
        val ask = BigDecimal("100.15")
        val bid = BigDecimal("99.85")

        return Security(
            id = "$normalizedCodeType-$normalizedCodeValue".hashCode() and Int.MAX_VALUE,
            ticker = ticker,
            description = "$ticker Mock",
            type = "Acciones",
            currency = "ARS",
            codeType = normalizedCodeType,
            codeValue = normalizedCodeValue,
            industry = "Mock",
            panel = "S&P Merval",
            liderMerval = true,
            minInstrumentNominals = 1,
            maxInstrumentNominals = 999999999,
            lotInstrumentSize = 1,
            holdingQuantity = DEFAULT_HOLDING_QUANTITY,
            price = price,
            askPrice00 = ask,
            askPrice24 = ask,
            bidPrice00 = bid,
            bidPrice24 = bid,
            percentageMovement = BigDecimal("0.015"),
            minBuyArsAmount = BigDecimal("100.00"),
        )
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
                codeType = DEFAULT_CODE_TYPE,
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
        const val DEFAULT_CODE_TYPE = "MOCK_SECURITY_ID"
        const val DEFAULT_CODE_VALUE = "MOCK-0"
        const val DEFAULT_TICKER = "MOCK"
        const val DEFAULT_HOLDING_QUANTITY = 100
        const val MOCK_MIN_BUY_ARS_AMOUNT = "100.00"
    }
}
