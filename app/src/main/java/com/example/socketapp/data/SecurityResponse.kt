package com.example.socketapp.data

import com.example.socketapp.Security
import java.math.BigDecimal
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

@Serializable
data class ServerResponse<T>(
    @SerialName("d")
    val data: T,
)

@Serializable
data class ServerListData<T>(
    @SerialName("List")
    val list: List<T> = emptyList(),
    @SerialName("ErrorDetail")
    val errorDetail: ErrorDetailResponse? = null,
)

typealias SecuritiesListResponse = ServerResponse<ServerListData<SecurityResponse>>
typealias SecurityCategoriesResponse = ServerResponse<ServerListData<String>>

@Serializable
data class ErrorDetailResponse(
    @SerialName("Title")
    val title: String? = null,
    @SerialName("Description")
    val description: String? = null,
    @SerialName("TypeIllustration")
    val typeIllustration: String? = null,
    @SerialName("Code")
    val code: String? = null,
)

@Serializable
data class SecurityResponse(
    @SerialName("Id")
    val id: Int = 0,
    @SerialName("Ticker")
    val ticker: String = "",
    @SerialName("Description")
    val description: String = "",
    @SerialName("SubType")
    val type: String = "",
    @SerialName("Currency")
    val currency: String = "ARS",
    @SerialName("CodeType")
    val codeType: String = "",
    @SerialName("CodeValue")
    val codeValue: String = "",
    @SerialName("Category")
    val category: String = "",
    val industry: String = "",
    val panel: String = "",
    @SerialName("LiderMerval")
    val liderMerval: Boolean = false,
    val indexationType: String? = null,
    @SerialName("IsFavorite")
    val isFavorite: Boolean = false,
    @SerialName("Logo")
    val logo: String = "",
    val minInstrumentNominals: Int = 0,
    val maxInstrumentNominals: Int = Int.MAX_VALUE,
    val lotInstrumentSize: Int = 0,
    val holdingQuantity: Int = 0,
    @SerialName("LastPrice")
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val priceChange: BigDecimal = BigDecimal.ZERO,
    @SerialName("DailyVariationPercent")
    @Serializable(with = BigDecimalSerializer::class)
    val dailyVariationPercent: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val askPrice00: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val askPrice24: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val bidPrice00: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val bidPrice24: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val percentageMovement: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val minBuyArsAmount: BigDecimal = BigDecimal.ZERO,
)

fun SecurityResponse.toDomain(): Security =
    Security(
        id = id,
        ticker = ticker,
        description = description,
        type = type,
        currency = currency,
        codeType = codeType,
        codeValue = codeValue,
        industry = industry.ifBlank { category },
        panel = panel,
        liderMerval = liderMerval,
        indexationType = indexationType,
        isFavorite = isFavorite,
        minInstrumentNominals = minInstrumentNominals,
        maxInstrumentNominals = maxInstrumentNominals,
        lotInstrumentSize = lotInstrumentSize,
        holdingQuantity = holdingQuantity,
        price = price,
        priceChange = priceChange,
        dailyVariationPercent = dailyVariationPercent,
        askPrice00 = askPrice00,
        askPrice24 = askPrice24,
        bidPrice00 = bidPrice00,
        bidPrice24 = bidPrice24,
        percentageMovement = percentageMovement,
        minBuyArsAmount = minBuyArsAmount,
    )

object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString().toBigDecimalOrNull() ?: BigDecimal.ZERO

        val primitive = jsonDecoder.decodeJsonElement() as? JsonPrimitive
            ?: return BigDecimal.ZERO

        return primitive.content.toBigDecimalOrNull()
            ?: primitive.doubleOrNull?.let { BigDecimal.valueOf(it) }
            ?: BigDecimal.ZERO
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        encoder.encodeString(value.toPlainString())
    }
}
