package com.example.socketapp.data

import com.example.socketapp.model.Security

interface SecuritiesRepository {
    suspend fun getSecurities(): List<Security>
}
