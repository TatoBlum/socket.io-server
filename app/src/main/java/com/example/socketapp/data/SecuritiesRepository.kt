package com.example.socketapp.data

import com.example.socketapp.BuyableInstrument
import com.example.socketapp.model.Security

interface SecuritiesRepository {
    fun getCachedSecurities(): List<Security>?
    suspend fun refreshSecurities(): List<Security>
    suspend fun getBuyableInstruments(): List<BuyableInstrument>
    suspend fun getBuyableInstrument(id: String): BuyableInstrument?
}
