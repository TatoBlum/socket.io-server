package com.example.socketapp.data

import java.math.BigDecimal
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class SecurityResponseTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun `decodes securities list response from server json`() {
        val response = json.decodeFromString<SecuritiesListResponse>(
            """
            {
              "d": {
                "List": [
                  {
                    "Id": 66494,
                    "Ticker": "AABA",
                    "Description": "CEDEAR ALTABA INC",
                    "SubType": "CEDEAR",
                    "Currency": "ARS",
                    "CodeType": "CAJA_VALOR",
                    "CodeValue": "8110",
                    "Category": "Closed End Funds",
                    "LiderMerval": false,
                    "IsFavorite": false,
                    "Logo": "agregar",
                    "LastPrice": 175.50,
                    "DailyVariationPercent": 1.25
                  }
                ],
                "ErrorDetail": {
                  "Title": "Titulo del error",
                  "Description": "Descripcion del error",
                  "TypeIllustration": "FeedbackWaiting",
                  "Code": "INPS-911"
                }
              }
            }
            """.trimIndent(),
        )

        val security = response.data.list.single()
        assertEquals(66494, security.id)
        assertEquals("AABA", security.ticker)
        assertEquals("CEDEAR", security.type)
        assertEquals("CAJA_VALOR", security.codeType)
        assertEquals("8110", security.codeValue)
        assertEquals(BigDecimal("175.50"), security.price)
        assertEquals(BigDecimal("1.25"), security.dailyVariationPercent)
        assertEquals("INPS-911", response.data.errorDetail?.code)
    }

    @Test
    fun `decodes security categories response from server json`() {
        val response = json.decodeFromString<SecurityCategoriesResponse>(
            """
            {
              "d": {
                "List": [
                  "CER",
                  "Dolar Linked"
                ],
                "ErrorDetail": {
                  "Title": "Titulo del error",
                  "Description": "Descripcion del error",
                  "TypeIllustration": "FeedbackWaiting",
                  "Code": "INPS-911"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(listOf("CER", "Dolar Linked"), response.data.list)
        assertEquals("INPS-911", response.data.errorDetail?.code)
    }

    @Test
    fun `decodes server technical error with empty list and nullable error fields`() {
        val response = json.decodeFromString<SecurityCategoriesResponse>(
            """
            {
              "d": {
                "List": [],
                "ErrorDetail": {
                  "Title": "TECHNICAL_ERROR",
                  "Description": "TECHNICAL_ERROR",
                  "TypeIllustration": null,
                  "Code": null
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals(emptyList<String>(), response.data.list)
        assertEquals("TECHNICAL_ERROR", response.data.errorDetail?.title)
        assertEquals("TECHNICAL_ERROR", response.data.errorDetail?.description)
        assertEquals(null, response.data.errorDetail?.typeIllustration)
        assertEquals(null, response.data.errorDetail?.code)
    }

    @Test
    fun `decodes server response with arbitrary object data`() {
        val response = json.decodeFromString<ServerResponse<SampleObjectResponse>>(
            """
            {
              "d": {
                "Name": "Portfolio",
                "Enabled": true
              }
            }
            """.trimIndent(),
        )

        assertEquals("Portfolio", response.data.name)
        assertEquals(true, response.data.enabled)
    }

    @Test
    fun `encodes favorites request and decodes favorites response`() {
        val request = json.encodeToString(FavoritesRequest(favs = listOf("AAPL", "PAMP")))
        val response = json.decodeFromString<FavoritesResponse>(
            """
            {
              "d": {
                "ErrorDetail": {
                  "Title": null,
                  "Description": null,
                  "TypeIllustration": null,
                  "Code": "PS-009"
                }
              }
            }
            """.trimIndent(),
        )

        assertEquals("""{"Favs":["AAPL","PAMP"]}""", request)
        assertEquals("PS-009", response.data.errorDetail?.code)
    }

    @Test
    fun `decodes BigDecimal fields from json strings`() {
        val response = json.decodeFromString<SecurityResponse>(
            """
            {
              "Ticker": "PAMP",
              "LastPrice": "150.50"
            }
            """.trimIndent(),
        )

        assertEquals(BigDecimal("150.50"), response.price)
    }

    @Test
    fun `maps response to domain security`() {
        val response = SecurityResponse(
            id = 1,
            ticker = "PAMP",
            codeType = "MOCK_SECURITY_ID",
            codeValue = "PAMP-0",
            price = BigDecimal("150.50"),
        )

        val security = response.toDomain()

        assertEquals("PAMP", security.ticker)
        assertEquals("MOCK_SECURITY_ID", security.codeType)
        assertEquals("PAMP-0", security.codeValue)
        assertEquals(BigDecimal("150.50"), security.price)
    }
}

@kotlinx.serialization.Serializable
private data class SampleObjectResponse(
    @kotlinx.serialization.SerialName("Name")
    val name: String = "",
    @kotlinx.serialization.SerialName("Enabled")
    val enabled: Boolean = false,
)
