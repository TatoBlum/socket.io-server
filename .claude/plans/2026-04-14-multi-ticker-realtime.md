# Multi-Ticker Real-Time — 2026-04-14

## Tarea
Transformar la app de un solo precio cripto a 20+ precios en tiempo real con LazyColumn, throttling 250ms, y visual feedback (verde/rojo) con animateColorAsState.

## Diseño

### Binance Combined Streams (1 conexión)
URL: `wss://stream.binance.com:9443/stream?streams=btcusdt@ticker/ethusdt@ticker/...` (24 pares)

Payload wrapper:
```json
{ "stream": "btcusdt@ticker", "data": { "s": "BTCUSDT", "c": "34220.10", "P": "0.35" } }
```

### Modelos nuevos
- `CombinedStreamMessage` — @JsonClass: `stream: String?`, `data: TickerData?`
- `TickerData` — @JsonClass: `@Json("s") symbol`, `@Json("c") price`, `@Json("P") percentChange`
- `CryptoTicker` — dominio @Immutable: `symbol`, `displayName`, `price`, `previousPrice?`, `priceDirection`, `percentChange`
- `PriceDirection` — enum: UP, DOWN, NEUTRAL

### Throttling (ViewModel)
- Coroutine 1: collecta DataSource flow → acumula en mutableMap interno (calcula previousPrice + PriceDirection)
- Coroutine 2: cada 250ms publica `_tickers.value = map.toMap()` (snapshot inmutable)
- Resultado: ~4 recomposiciones/s máximo, independiente de frecuencia WS

### UI
- LazyColumn con `key = { it.symbol }` + `@Immutable` en CryptoTicker → solo recompone items que cambiaron
- `animateColorAsState` + `LaunchedEffect(ticker.price)` para flash verde/rojo 500ms
- ConnectionStatusBar arriba con estado + botón

### 24 pares USDT
btcusdt, ethusdt, bnbusdt, xrpusdt, adausdt, dogeusdt, solusdt, dotusdt, maticusdt, ltcusdt, shibusdt, trxusdt, avaxusdt, linkusdt, uniusdt, atomusdt, xlmusdt, etcusdt, filusdt, vetusdt, icpusdt, aptusdt, nearusdt, arbusdt

---

## Equipo: 3 Developers

### 🐺 Developer A — Data Layer
**Crea:**
- `CombinedStreamMessage.kt` — @JsonClass wrapper
- `TickerData.kt` — @JsonClass ticker data
- `CryptoTicker.kt` — dominio @Immutable
- `PriceDirection.kt` — enum
- `CryptoTickerDataSource.kt` — parsea combined stream, retorna Flow<CryptoTicker>

**Modifica:**
- `Constants.kt` — SYMBOLS list + combinedStreamUrl()
- `proguard-rules.pro` — keep rules para nuevos modelos Moshi

### 🦊 Developer B — ViewModel + Throttling + Wiring
**Modifica:**
- `MainViewModel.kt` — _tickers: MutableStateFlow<Map<String, CryptoTicker>>, dual-coroutine throttle
- `ViewModelFactory.kt` — wiring con CryptoTickerDataSource
- `MainViewModelTest.kt` — migrar a nuevo modelo con FakeCryptoTickerDataSource

### 🐆 Developer C — UI Compose
**Crea:**
- `ui/CryptoTickerItem.kt` — item composable con animateColorAsState
- `ui/ConnectionStatusBar.kt` — barra de estado extraída

**Modifica:**
- `MainActivity.kt` — MainScreen con LazyColumn

---

## Riesgos
1. Tests existentes rompen por cambio de datasource → 🦊 los migra
2. Mensajes non-ticker de Binance → mapNotNull filtra nulls
3. Recomposición innecesaria → @Immutable + key en LazyColumn
4. coroutines 1.5.1 viejo → APIs usadas disponibles desde 1.3.x
