package com.example.socketapp.di

import com.example.socketapp.BuildConfig
import com.example.socketapp.socket.StockTickerDataSource
import com.example.socketapp.socket.WebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object SocketModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideWebSocketClient(): WebSocketClient = WebSocketClient()

    @Provides
    @Singleton
    fun provideStockTickerDataSource(client: WebSocketClient): StockTickerDataSource =
        StockTickerDataSource(
            client = client,
            keyId = BuildConfig.ALPACA_KEY_ID,
            secret = BuildConfig.ALPACA_SECRET_KEY,
        )
}
