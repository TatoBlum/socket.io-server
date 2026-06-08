package com.example.socketapp.data

import com.example.socketapp.Security
import java.math.BigDecimal
import kotlinx.serialization.KSerializer
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
data class SecurityResponse(
    val id: Int = 0,
    val ticker: String = "",
    val description: String = "",
    val type: String = "",
    val currency: String = "ARS",
    val codeType: String = "",
    val codeValue: String = "",
    val industry: String = "",
    val panel: String = "",
    val liderMerval: Boolean = false,
    val indexationType: String? = null,
    val isFavorite: Boolean = false,
    val minInstrumentNominals: Int = 0,
    val maxInstrumentNominals: Int = Int.MAX_VALUE,
    val lotInstrumentSize: Int = 0,
    val holdingQuantity: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal = BigDecimal.ZERO,
    @Serializable(with = BigDecimalSerializer::class)
    val priceChange: BigDecimal = BigDecimal.ZERO,
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
        industry = industry,
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
