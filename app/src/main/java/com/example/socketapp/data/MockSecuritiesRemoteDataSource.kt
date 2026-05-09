package com.example.socketapp.data

import com.example.socketapp.model.Security
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

            security.copy(
                rawPrice = nextPrice.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                rawPriceChange = movement.toPlainString(),
                rawPercentageChange = percent.toPlainString(),
            )
        }
    }

    private fun buildMockSecurities(): List<Security> {
        val symbols = listOf(
            "ALUA", "BBAR", "BMA", "BYMA", "CEPU", "COME", "CRES", "EDN", "GGAL", "LOMA",
            "MIRG", "PAMP", "SUPV", "TECO2", "TGNO4", "TGSU2", "TRAN", "TXAR", "VALO", "YPFD",
            "AAPL", "AMZN", "GOOGL", "META", "MSFT", "NVDA", "TSLA", "AMD", "JPM", "V",
        )

        val names = mapOf(
            "ALUA" to "Aluar",
            "BBAR" to "BBVA Argentina",
            "BMA" to "Banco Macro",
            "BYMA" to "Bolsas y Mercados Argentinos",
            "CEPU" to "Central Puerto",
            "COME" to "Sociedad Comercial del Plata",
            "CRES" to "Cresud",
            "EDN" to "Edenor",
            "GGAL" to "Grupo Financiero Galicia",
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

            Security(
                id = "$symbol-$suffix",
                symbol = if (suffix == 0) symbol else "$symbol$suffix",
                name = names.getValue(symbol),
                rawPrice = price.toPlainString(),
                rawPriceChange = change.toPlainString(),
                rawPercentageChange = percent.toPlainString(),
                currency = if (index % 3 == 0) "Dolares" else "Pesos",
                panel = if (index % 4 == 0) "General" else "S&P Merval",
                sector = sectors[index % sectors.size],
            )
        }
    }

    private fun randomDecimalCents(fromCents: Long, untilCents: Long): BigDecimal =
        BigDecimal.valueOf(Random.nextLong(fromCents, untilCents), 2)
}
