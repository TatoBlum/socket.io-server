# Plan — Review + Refactor del módulo WebSocket (2026-04-13)

Autor: 🦧 Architect (Opus)
Task: review arquitectural del módulo WebSocket + propuesta de refactor + fix URL deprecada.

## Research consultado
- `.claude/research/2026-04-13-market-websocket-endpoints.md` → Binance recomendado para POC (3 cambios mínimos).

## 1. Issues detectados (priorizados)

### CRITICAL
| # | Issue | Archivo:línea |
|---|-------|---------------|
| C1 | **Bypass TLS** `hostnameVerifier { _, _ -> true }` — acepta cualquier certificado | `WebServiceProvider.kt:18` |
| C2 | **URL deprecada** `wss://ws-feed.pro.coinbase.com` — la app no conecta | `Constants.kt:6` |
| C3 | **GlobalScope x4** — coroutines no cancelables, fugas de memoria | `SocketListener.kt:32,39,54,67` |

### HIGH
| # | Issue | Archivo |
|---|-------|---------|
| H1 | `MutableSharedFlow()` sin replay/buffer → se pierde primer mensaje | `SocketListener.kt:20` |
| H2 | `subscribeToSocketEvents()` no es idempotente | `MainViewModel.kt:19` |
| H3 | `CheckNetworkConnection` recreada en cada `onResume` → observers acumulados | `MainActivity.kt:41-52` |
| H4 | `subscribeObserver()` lanza nuevo coroutine en cada `onResume` | `MainActivity.kt:33-38` |
| H5 | Sin retry/reconnect cuando falla | `SocketListener.kt:62` |
| H6 | `connectionPool.evictAll()` post-creación puede matar el socket | `WebServiceProvider.kt:50` |
| H7 | `onClosing` envía unsubscribe Y cierra → doble acción | `SocketListener.kt:44-58` |

### MEDIUM
| # | Issue | Archivo |
|---|-------|---------|
| M1 | 3 clases en `MainViewModel.kt` (VM + Interactor + Repository) | `MainViewModel.kt` |
| M2 | `Moshi.Builder().build()` en cada mensaje — debe ser singleton | `SocketListener.kt:73` |
| M3 | Producto `BTC-EUR` y JSON subscribe hardcodeados en el listener | `SocketListener.kt:25-29` |
| M4 | `DataState` acoplado (mezcla ticker + byteString + exception + webSocket) — usar sealed class | `SocketListener.kt:82-87` |
| M5 | `println` en todos lados (~12 usos) | múltiples |
| M6 | Dependencia coroutines duplicada (1.5.1 y 1.5.2) | `build.gradle:56,69` |
| M7 | Sin manejo de `onClosed` | `SocketListener.kt` |
| M8 | Acoplamiento al proveedor (Coinbase) dentro del listener | `SocketListener.kt:24-30,47-52` |

### LOW
- L1: ProGuard rules vacías (si `minifyEnabled=true`, Moshi rompe)
- L2: kapt → migrar a KSP
- L3: `lifecycleScope.launchWhenStarted` deprecado → `repeatOnLifecycle`
- L4: `stopSocket()` swallow exception
- L5: `BitcoinTicker` solo tiene `price` — hay campos útiles desperdiciados
- L6: Sin tests reales

## 2. Arquitectura propuesta

```
+------------------+        +---------------------+
|   MainActivity   |        | CheckNetworkConn.   |
|  (observa flows) |------->|  (LiveData<Bool>)   |
+--------+---------+        +---------------------+
         | ViewModelProvider + Factory
         v
+------------------+
|  MainViewModel   |  expone: StateFlow<UiState>
|  (viewModelScope)|  sealed UiState { Loading, Data(TickerData), Error(msg), Disconnected }
+--------+---------+
         v
+------------------+
|  MainInteractor  |  (archivo propio)
+--------+---------+
         v
+------------------+
|  MainRepository  |  (archivo propio) — usa MarketDataSource
+--------+---------+
         v
+-----------------------+
| <<interface>>         |
| MarketDataSource      |
|  + connect(config)    |
|  + disconnect()       |
|  + tickerFlow         |
+-----------+-----------+
            |
   +--------+---------+
   v                  v
+------------------+ +------------------+
| CoinbaseSource   | | BinanceSource    |
+--------+---------+ +------------------+
         v
+------------------+       +------------------+
| SocketManager    |<------| MarketConfig     |
| (retry+backoff,  |       | (url, product,   |
|  scope inyectado)|       |  subscribeMsg)   |
+--------+---------+       +------------------+
         v
+------------------+
| TickerParser     |  <<interface>>
+------------------+

sealed TickerResult { Data(TickerData), Error(Throwable), Connected, Disconnected }
```

## 3. Fases (cada fase = 1 PR)

### Fase 1 — "Stop the bleeding" (CRITICAL)
- Remover `hostnameVerifier` (C1)
- Actualizar URL a Binance (C2) — decisión del research
- Reemplazar `GlobalScope` x4 por scope inyectado (C3)
- `MutableSharedFlow(replay=1, extraBufferCapacity=1)` (H1)
- Remover `connectionPool.evictAll()` (H6)
- `println` → `Log.d` (M5)
- Fix `onClosing` (H7)
- Moshi singleton (M2)

### Fase 2 — "Clean architecture split"
- Extraer `MainInteractor.kt` y `MainRepository.kt` (M1)
- Crear `TickerResult.kt` sealed class (M4)
- `subscribeToSocketEvents()` idempotente (H2)
- Fix `MainActivity` (mover a `onCreate`, remover re-suscripción) (H3, H4)
- `UiState` sealed class

### Fase 3 — "Provider abstraction + retry"
- Crear `MarketDataSource` interface + `SocketManager` + `MarketConfig` + `TickerParser`
- `CoinbaseDataSource` + `CoinbaseTickerParser` (o `BinanceDataSource` según endpoint)
- Retry con exponential backoff (H5)
- Handle `onClosed` (M7)
- Desacoplar proveedor (M3, M8)

### Fase 4 — "Quality + polish"
- ProGuard rules (L1)
- Fix deps duplicadas (M6)
- `repeatOnLifecycle` (L3)
- Tests unitarios

### Fase 5 — "Segundo proveedor" (futura)
- Agregar `BinanceDataSource` o `KrakenDataSource` si Fase 1 usó Coinbase AT.

## 4. Developers (Fase 1 inicial)

### 🐺 — PR 1A (se mergea primero, rápido)
- Modificar `Constants.kt` → URL Binance

### 🦊 — PR 1B (depende de 1A)
- Modificar `WebServiceProvider.kt`: remover hostnameVerifier, remover evictAll, crear scope interno, pasar scope al listener, `println→Log.d`, normalizar timeouts, log excepciones en stopSocket
- Modificar `SocketListener.kt`: recibir scope, reemplazar GlobalScope x4, SharedFlow con replay, Moshi singleton, fix onClosing (sin unsubscribe porque Binance no lo necesita), println→Log, adaptar para payload Binance (campo `c`)
- Modificar `BitcoinTicker.kt`: agregar `@Json(name = "c")`
- Modificar `MainViewModel.kt`: flag `isSubscribed` idempotencia
- Modificar `MainActivity.kt`: mover observers a `onCreate`
- Modificar `ViewModelFactory.kt`: si cambian firmas

## 5. Tests (Fase 4)
- `SocketListenerTest`, `WebServicesProviderTest`, `MainViewModelTest` (idempotencia)

## 6. Riesgos
- **Binance bloqueado en BE/NL**: verificar desde device objetivo. Fallback: Bitstamp.
- `replay=1` emite stale al resubscribirse — intencional para evitar pérdida.
- Scope cancelado antes de emitir `onFailure`: usar `SupervisorJob` + try-catch `CancellationException`.

## Decisión del orquestador para este ciclo

**Fase 1 solamente**, como lo pide el usuario ("propone ajustes"). La Fase 1 entrega:
- Fix URL (Binance, endpoint funcional 2026)
- Fix TLS bypass (critical security)
- Fix GlobalScope leaks
- Fix SharedFlow buffer
- Limpieza de `println`, Moshi singleton, `onClosing`

Quedan como propuestas documentadas las Fases 2-5 — se pueden abrir como ciclos separados después de que el usuario las revise.
