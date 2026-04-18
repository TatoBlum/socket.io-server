# SocketAndroidPOC

Prueba de concepto Android que muestra **24 criptomonedas en tiempo real**
vía WebSocket (Binance combined streams), con arquitectura MVVM y UI en
Jetpack Compose (Material 3).

## Features

- **24 pares USDT** actualizándose en tiempo real (BTC, ETH, BNB, SOL, etc.)
- **1 sola conexión WebSocket** a Binance combined streams
- **Throttling 250ms** en el ViewModel (dual-coroutine: acumula + publica snapshots)
- **Visual feedback**: flash verde/rojo con `animateColorAsState` cuando el precio sube o baja
- **Diffing eficiente**: `LazyColumn` con `key` + `@Immutable` — solo recompone items que cambiaron
- **Iconos** de cada moneda cargados desde CoinCap CDN (Coil)
- **Reconexión automática** con backoff exponencial + jitter (hasta 5 reintentos)
- **Detección de red**: se conecta/desconecta automáticamente según conectividad

## Stack

- **Lenguaje**: Kotlin 1.9.22, JVM 17
- **UI**: Jetpack Compose (Material 3) + `activity-compose` + `lifecycle-runtime-compose`
- **Arquitectura**: MVVM (ViewModel + StateFlow), sin DI framework
- **Red**: OkHttp 4.9.0 (WebSocket)
- **JSON**: Moshi 1.15.1 (kapt codegen)
- **Imágenes**: Coil 2.5.0
- **Tests**: JUnit 4 + `kotlinx-coroutines-test`

## Estructura

```
app/src/main/java/com/example/socketapp/
├── MainActivity.kt              UI Compose (LazyColumn + Surface dark)
├── MainViewModel.kt             tickers Map + dual-coroutine throttle 250ms
├── CryptoTickerDataSource.kt    parseo combined stream + retry/backoff
├── WebSocketClient.kt           transporte (OkHttp + callbackFlow)
├── Constants.kt                 24 símbolos + URL builder + iconUrl()
├── CryptoTicker.kt              modelo de dominio (@Immutable)
├── CombinedStreamMessage.kt     modelo Moshi (wrapper combined stream)
├── TickerData.kt                modelo Moshi (ticker individual)
├── PriceDirection.kt            enum UP/DOWN/NEUTRAL
├── ConnectionState.kt           sealed class de estados
├── CheckNetworkConnection.kt    conectividad (LiveData)
├── ViewModelFactory.kt          factory manual del VM
└── ui/
    ├── CryptoTickerItem.kt      item con icono + precio + flash animado
    └── ConnectionStatusBar.kt   barra de estado + botón abrir/cerrar
```

## Arquitectura

```
Binance WSS (combined streams, 24 pares)
        │
   WebSocketClient          callbackFlow + awaitClose
        │ Flow<String>
   CryptoTickerDataSource   parse JSON + mapNotNull + retryWhen
        │ Flow<CryptoTicker>
   MainViewModel             Coroutine 1: acumula en Map (BigDecimal compare)
        │                    Coroutine 2: publica snapshot cada 250ms
        │ StateFlow<Map<String, CryptoTicker>>
   MainScreen (Compose)      LazyColumn + key + animateColorAsState
```

## Documentación

- [`documents/websocket.md`](documents/websocket.md) — Guía de integración del
  WebSocket desde el ViewModel. Capas, contratos, ciclo de vida, cancelación,
  reconexión, tests.

## Build & Run

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:testDebugUnitTest  # tests unitarios
./gradlew :app:installDebug       # instalar en device/emulador
```

## Tests

- `MainViewModelTest` — 13 tests del VM (subscribe, stop, idempotencia,
  re-subscribe, error, connectionState, onCleared, PriceDirection UP/DOWN/NEUTRAL,
  múltiples símbolos, previousPrice).
- `CombinedStreamParsingTest` — 6 tests del parseo Moshi del combined stream.
- `BitcoinTickerParsingTest` — 4 tests del parseo Moshi legacy.

Total: 24 tests.

## Configurar API key Alpaca

1. Registrate en [Alpaca paper trading](https://app.alpaca.markets/paper/dashboard/overview) y copiá tu `API Key ID` y `Secret Key`.
2. Copiá el archivo de ejemplo: `cp local.properties.example local.properties`
3. Abrí `local.properties` y reemplazá los placeholders con tus keys:
   ```
   ALPACA_KEY_ID=tu_key_id
   ALPACA_SECRET_KEY=tu_secret_key
   ```

> `local.properties` está en `.gitignore` y nunca se commitea. Los streams de trades solo fluyen durante el horario de mercado (9:30–16:00 ET, días hábiles).
