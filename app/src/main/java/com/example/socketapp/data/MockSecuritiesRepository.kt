package com.example.socketapp.data

import com.example.socketapp.model.Security
import com.example.socketapp.model.SecurityCurrency
import com.example.socketapp.model.SecurityPanel
import com.example.socketapp.model.SecuritySector
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random
import kotlinx.coroutines.delay

class MockSecuritiesRepository : SecuritiesRepository {
    private var securities = buildMockSecurities()

    override suspend fun getSecurities(): List<Security> {
        delay(350)
        securities = securities.map { security ->
            val movement = BigDecimal(Random.nextDouble(-2.4, 2.4)).setScale(2, RoundingMode.HALF_UP)
            val nextPrice = (security.price + movement).coerceAtLeast(BigDecimal("1.00"))
            val percent = movement
                .divide(security.price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)

            security.copy(
                price = nextPrice.setScale(2, RoundingMode.HALF_UP),
                priceChange = movement,
                percentageChange = percent,
            )
        }
        return securities
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

        return List(1_000) { index ->
            val symbol = symbols[index % symbols.size]
            val suffix = index / symbols.size
            val price = BigDecimal(Random.nextDouble(40.0, 10_000.0)).setScale(2, RoundingMode.HALF_UP)
            val change = BigDecimal(Random.nextDouble(-8.0, 8.0)).setScale(2, RoundingMode.HALF_UP)
            val percent = change
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)

            Security(
                id = "$symbol-$suffix",
                symbol = if (suffix == 0) symbol else "$symbol$suffix",
                name = names.getValue(symbol),
                price = price,
                priceChange = change,
                percentageChange = percent,
                currency = if (index % 3 == 0) SecurityCurrency.Dollars else SecurityCurrency.Pesos,
                panel = if (index % 4 == 0) SecurityPanel.General else SecurityPanel.Merval,
                sector = SecuritySector.entries[index % SecuritySector.entries.size],
            )
        }
    }
}
