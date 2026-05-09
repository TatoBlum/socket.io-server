package com.example.socketapp.data

import com.example.socketapp.model.Security

interface SecuritiesRemoteDataSource {
    suspend fun fetchSecurities(currentSecurities: List<Security>?): List<Security>
}
