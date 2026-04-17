# TradingView widgets para Android — Evaluación de integración

_Fecha: 2026-04-17_

## TL;DR (max 10 bullets — para inyección en prompts)

- **TradingView no tiene SDK nativo Android.** Todas sus librerías renderizan en WebView (JS bajo el capó).
- **Lightweight Charts Android** (wrapper oficial, Apache 2.0) es lo más cercano a "widget TradingView": candlestick + `series.update()` para tiempo real. Última release v4.0.0 (sept 2023), sólo `AndroidView` (no Compose nativo).
- **Advanced Charts** (Charting Library completa con indicadores) está *gated*: requiere aplicar en tradingview.com/advanced-charts/; apunta a empresas con app pública. Repo privada.
- **Vico** es la alternativa Compose-native más fuerte: Apache 2.0, v3.1.0 (abr 2026), candlestick desde v2.0.0. Mejor DX en este stack; sin indicadores tipo RSI/MACD.
- **MPAndroidChart**: soporta candlestick pero abandonado (último release 2021, sin Compose). No recomendado para proyecto nuevo.
- Binance klines: REST `GET /api/v3/klines?symbol=BTCUSDT&interval=1m&limit=500` para histórico, WS `<symbol>@kline_1m` (campo `x: true` = vela cerrada) para real-time.
- El proyecto YA es **100% Compose Material3** (MainActivity migrado 2026-04-13) — preferir API Compose-native si alcanza. `patterns.md` dice XML pero está desactualizado.
- Gaps concretos: no hay navegación (un solo `setContent`), no hay HTTP REST client, no hay modelo `Candle`, `CryptoTickerItem` sin `onClick`, no hay `selectedSymbol` en el VM.
- INTERNET permission ya está; WebView técnicamente viable sin tocar Manifest.
- OkHttp 4.9.0 ya en `app/build.gradle` → sirve directo para REST klines, no hace falta sumar Retrofit.

## 🐬 Web findings

### TradingView — qué ofrece para Android
1. **Lightweight Charts Android** (`tradingview/lightweight-charts-android`)
   - Wrapper Kotlin sobre la lib JS renderizada en WebView interno.
   - Licencia Apache 2.0 (pide atribución + link a tradingview.com).
   - v4.0.0 (sept 2023). La lib JS está en v5.x pero el wrapper no actualizó.
   - `CandlestickSeries` con `time/open/high/low/close`. `seriesApi.update(bar)` para updates, `setData()` para carga inicial.
   - Sin `@Composable` oficial; se integra con `AndroidView { ChartsView(...) }`.
   - minSdk 21, requiere WebView con ES6.

2. **Advanced Charts (Charting Library completa)**
   - Acceso gated: formulario en tradingview.com/advanced-charts/ → repo privada.
   - Gratis con branding TradingView; enterprise para quitarlo.
   - Para Android se integra vía WebView + `evaluateJavascript()` bridge (ejemplo en `tradingview/charting-library-examples/android`).
   - Requisito: app pública de empresa (no uso personal/hobby).

### Alternativas open-source

| Lib | Candlestick | Compose | Versión | ⭐ | Licencia | Estado |
|---|---|---|---|---|---|---|
| **Vico** | Sí (v2.0+) | Nativo | v3.1.0 (abr 2026) | 3000 | Apache 2.0 | Activo |
| **lightweight-charts-android** | Sí | vía `AndroidView` | v4.0.0 (sept 2023) | 130 | Apache 2.0 | Mantenido |
| **MPAndroidChart** | Sí | vía `AndroidView` | v3.1.0 (2021) | 38000 | Apache 2.0 | Abandonado |

### Ejemplos de integración

**Vico (Compose-native):**
```kotlin
implementation("com.patrykandpatrick.vico:compose-m3:3.1.0")

val modelProducer = remember { CartesianChartModelProducer() }
modelProducer.runTransaction {
    candlestickSeries(opening = opens, closing = closes, low = lows, high = highs)
}
CartesianChartHost(
    chart = rememberCartesianChart(rememberCandlestickCartesianLayer()),
    modelProducer = modelProducer
)
```

**Lightweight Charts (WebView wrapper):**
```kotlin
implementation("com.tradingview:lightweightcharts:4.0.0")

AndroidView(factory = { ctx ->
    ChartsView(ctx).apply {
        api.addCandlestickSeries { series -> series.setData(historicalBars) }
    }
}, update = { chartsView ->
    chartsView.api.candlestickSeries?.update(latestBar)
})
```

### Datos Binance para alimentar el chart
- **Histórico:** `GET https://api.binance.com/api/v3/klines?symbol=BTCUSDT&interval=1m&limit=500` → array `[openTime, o, h, l, c, v, ...]`.
- **Real-time:** `wss://stream.binance.com:9443/ws/btcusdt@kline_1m`, payload `{ "k": {"t","o","h","l","c","v","x"} }`. `x:false` = update vela en curso, `x:true` = vela cerrada, empieza una nueva.

## 🐜 Codebase findings

### Stack actual (contra lo que dice `patterns.md`)
- AGP 8.2.2, Kotlin 1.9.22, compileSdk/targetSdk 34, minSdk 21.
- UI **100% Compose Material3** (no hay `res/layout/`). `patterns.md` dice XML+ViewBinding → **desactualizado** desde la migración del 2026-04-13.
- MVVM simple, `ViewModelFactory` manual, sin DI.
- OkHttp 4.9.0 ya disponible; sólo se usa para WebSocket (`WebSocketClient`). No hay REST client.

### Archivos relevantes (path:line)
- `app/src/main/java/com/example/socketapp/MainActivity.kt:40-55` — `ComponentActivity.setContent`, un único `MainScreen`. Sin `NavHost`.
- `app/src/main/java/com/example/socketapp/MainViewModel.kt:25-26` — `tickers: StateFlow<Map<String, CryptoTicker>>`. **No expone `selectedSymbol`.**
- `app/src/main/java/com/example/socketapp/ui/CryptoTickerItem.kt:43` — `Row` sin `Modifier.clickable` ni callback `onClick`.
- `app/src/main/java/com/example/socketapp/Constants.kt:31-34` — `combinedStreamUrl()`, `iconUrl()`. **Falta `klinesUrl(symbol, interval, limit)`.**
- `app/src/main/java/com/example/socketapp/WebServiceProvider.kt` — `WebSocketClient`. OkHttp sólo para WS.
- `app/src/main/java/com/example/socketapp/CryptoTicker.kt` — modelo con `symbol`, `price`, `percentChange`, `priceDirection`. **No hay `Candle`/`Kline`.**
- `app/src/main/java/com/example/socketapp/TickerData.kt` — DTO Moshi sin OHLCV.
- `app/src/main/AndroidManifest.xml:5` — `uses-permission INTERNET` presente → WebView viable sin cambios.
- `app/src/main/java/com/example/socketapp/BitcoinTickerDataSource.kt` — legacy muerto (BTC individual, URL deprecada).

### Gaps concretos

| Gap | Qué falta |
|---|---|
| Navegación | Una sola Activity, sin `NavHost`. Hay que agregar Navigation Compose o un state `selectedSymbol: StateFlow<String?>` para swapear pantalla. |
| Click en item | `CryptoTickerItem` sin `onClick`. Sumar callback + `Modifier.clickable`. |
| Cliente REST | `WebSocketClient` no hace GET. OkHttp ya está; sólo hace falta helper `suspend fun getKlines(...)`. |
| Modelo `Candle` | No existe. Crear data class con OHLCV + timestamps. |
| ViewModel detalle | No hay `DetailViewModel`. Debe recibir símbolo, cargar histórico REST y opcionalmente suscribirse a `<symbol>@kline_1m`. |
| Lib de charts | Ninguna declarada en `build.gradle`. |

## Cross-reference

### Acuerdos
- WebView es técnicamente viable: 🐬 dice que TradingView sólo tiene WebView; 🐜 confirma INTERNET permission y que Compose puede envolver WebView vía `AndroidView`.
- Binance klines (REST + WS) es el backend natural: 🐬 describe el endpoint, 🐜 confirma que ya existe Binance WS en el proyecto pero no hay REST.

### Complementos
- 🐬 aporta opciones externas y trade-offs de licencia/mantenimiento.
- 🐜 aporta el estado real del proyecto: 100% Compose (no XML como dice `patterns.md`), OkHttp disponible, qué archivos tocar.

### Conflictos
- `patterns.md` describe la app como XML+ViewBinding → **contradicción con el código real** (ya 100% Compose desde 2026-04-13). Acción sugerida: actualizar `patterns.md` en una tarea aparte.

### Gap analysis
Integrar un chart requiere (en orden):
1. Sumar dependencia (Vico o lightweight-charts-android).
2. Modelo `Candle` + `KlinesResponse` Moshi.
3. Helper REST en `WebSocketClient` (o nuevo `BinanceRestClient`) con OkHttp Call → `GET /api/v3/klines`.
4. `onClick` en `CryptoTickerItem` + `selectedSymbol` en `MainViewModel`.
5. Pantalla `CoinDetailScreen` Composable + `DetailViewModel` con historial + stream `@kline_1m`.
6. Navegación: Navigation Compose (recomendado) o swap manual por `selectedSymbol != null`.
7. `klinesUrl()` en `Constants.kt`.

### Aplicabilidad al proyecto
- **Vico (Opción A):** mejor fit con el stack (Compose-native, sin WebView, cero bridge JS). Basta si sólo queremos velas + tooltip básico. Menos features que TradingView (sin RSI/MACD/dibujo de líneas).
- **Lightweight Charts Android (Opción B):** si se necesita el look TradingView y features del chart JS. Cuesta overhead de WebView y `AndroidView`. El wrapper está en v4.0.0 (2023), algo atrasado.
- **Advanced Charts:** descartable para un POC — gated a empresas con app pública, burocracia.
- **MPAndroidChart:** descartable — abandonado.

## Conclusión

Para este proyecto recomiendo **Vico** (`com.patrykandpatrick.vico:compose-m3:3.1.0`) como primera opción: es Compose-native, activo, y cubre el caso candlestick + updates de Binance sin tocar WebView. Si después hace falta el look exacto de TradingView o indicadores técnicos, migrar a **lightweight-charts-android** vía `AndroidView`.

El plan de implementación mínimo: modelo `Candle` + REST klines (OkHttp ya está) + onClick en item + pantalla detalle Compose con `CartesianChartHost` + suscripción al stream `@kline_1m`. Antes de cualquier integración, actualizar `patterns.md` porque sigue describiendo una app XML/ViewBinding que ya no existe.
