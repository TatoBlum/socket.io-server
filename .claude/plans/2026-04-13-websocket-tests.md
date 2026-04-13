# Plan — Tests unitarios del módulo WebSocket (2026-04-13)

Autor: 🦧 Architect (Opus)
Tarea: integrar pruebas de conexión, reconexión simple y cierre del socket.

## Research consultado
- `.claude/agents/learnings.md` — patrones previos, anti-patrones.
- `.claude/research/2026-04-13-market-websocket-endpoints.md` — payload Binance (`"c"` field).
- `.claude/skills/patterns/SKILL.md` — convenciones del proyecto.

## Blocker crítico identificado
`android.util.Log` se usa en `WebServiceProvider.kt`, `BitcoinTickerDataSource.kt`, `MainViewModel.kt`. Sin `testOptions { unitTests.returnDefaultValues = true }` en `build.gradle`, los tests JVM crashean con `RuntimeException: Method d in android.util.Log not mocked`. **Fix obligatorio antes de cualquier test que instancie esas clases.**

## Deps nuevas (exactas)

```gradle
testImplementation 'org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1'
testImplementation 'com.squareup.okhttp3:mockwebserver:4.9.0'
```

Y en `android { ... }`:
```gradle
testOptions { unitTests.returnDefaultValues = true }
```

**NO** agregar Turbine — con coroutines-test 1.5.x, `take(n).toList()` + `first()` + `withTimeout` son suficientes.

## Matriz de tests

| # | Clase | Escenario | Aserción |
|---|-------|-----------|----------|
| 1 | BitcoinTicker (Moshi) | JSON válido `{"c":"34220.10"}` | `price == "34220.10"` |
| 2 | BitcoinTicker (Moshi) | Sin campo `c` `{}` | `price == null` |
| 3 | BitcoinTicker (Moshi) | JSON malformado | `fromJson` lanza/null |
| 4 | WebSocketClient | Happy: abre + recibe 1 msg | `flow.first() == text` + `Connected` |
| 5 | WebSocketClient | Recibe N mensajes | `take(3).toList()` tiene 3 items |
| 6 | WebSocketClient | Server cierra (`close(1001)`) | Flow falla con `IOException`, estado `Disconnected` |
| 7 | WebSocketClient | Handshake falla (HTTP 500) | Flow falla, estado `Failed(...)` |
| 8 | WebSocketClient | Cliente cancela collect | `awaitClose` dispara, estado `Disconnected` |
| 9 | BitcoinTickerDataSource | Happy: JSON válido → BitcoinTicker | `first().price == "34220.10"` |
| 10 | BitcoinTickerDataSource | JSON inválido descartado, válido emitido | `mapNotNull` filtra basura |
| 11 | BitcoinTickerDataSource | Retry tras falla | Segunda conexión emite ticker |
| 12 | BitcoinTickerDataSource | Max retries alcanzado | Flow termina tras 5 intentos |
| 13 | BitcoinTickerDataSource | `CancellationException` no reintenta | Flow termina inmediato |

## Arquitectura de fixtures

- **MockWebServer** (mismo grupo que OkHttp productivo) simula el server WS en memoria.
- `server.enqueue(MockResponse().withWebSocketUpgrade(serverListener))` habilita el upgrade WS.
- URL para tests: `server.url("/").toString().replace("http://", "ws://")`.
- Desde `serverListener.onOpen(ws, response)` se puede `ws.send(text)` o `ws.close(code, reason)` para simular comportamientos.
- Para fallar el handshake: `server.enqueue(MockResponse().setResponseCode(500))`.

## Decisiones de diseño clave

1. **Virtual time para retry**: `runBlockingTest` de coroutines-test 1.5.x intercepta `delay()` en el `retryWhen`, evita esperar 30s reales. Solo para tests del DataSource.
2. **WebSocketClientTest usa `runBlocking` (NO `runBlockingTest`)**: MockWebServer opera con threads reales; el handshake TCP necesita tiempo real de ms.
3. **`withTimeout(5_000)`** alrededor de cada collect para evitar tests colgados.
4. **Inyección de URL**: agregar `url: String = Constants.WEB_SOCKET_URL` como parámetro default del constructor de `BitcoinTickerDataSource`. 1 línea, backward-compatible.

## Fases (cheapest first)

### Fase 1 — Moshi parsing (puro sync, sin coroutines ni server)
- `BitcoinTickerParsingTest.kt` — tests #1–3.

### Fase 2 — WebSocketClient con MockWebServer real
- `WebSocketClientTest.kt` — tests #4–8.
- Requiere el fix de `testOptions` + deps nuevas.

### Fase 3 — DataSource con retry virtual time
- `BitcoinTickerDataSourceTest.kt` — tests #9–13.
- Requiere inyección de URL en el constructor.

## Developers

### 🐺 — Fase 1 + Fase 2 + config build
**Crear**:
- `app/src/test/java/com/example/socketapp/BitcoinTickerParsingTest.kt`
- `app/src/test/java/com/example/socketapp/WebSocketClientTest.kt`

**Modificar**:
- `app/build.gradle` → deps nuevas + `testOptions { unitTests.returnDefaultValues = true }`

**No tocar**: producción.

### 🦊 — Fase 3 + refactor mínimo de URL inyectable
**Crear**:
- `app/src/test/java/com/example/socketapp/BitcoinTickerDataSourceTest.kt`

**Modificar**:
- `app/src/main/java/com/example/socketapp/BitcoinTickerDataSource.kt` → agregar `url: String = Constants.WEB_SOCKET_URL` al constructor.

**No tocar**: `build.gradle` (🐺 lo maneja), `WebServiceProvider.kt`.

## Riesgos y mitigaciones

- **`android.util.Log` crashea**: `testOptions.returnDefaultValues = true` (🐺, primero).
- **`runBlockingTest` vs MockWebServer real**: separar — 🐺 usa `runBlocking`, 🦊 usa `runBlockingTest` solo para saltar backoff.
- **Flakiness shutdown race**: `server.shutdown()` en `@After`, `withTimeout` en collect, cancelar flow antes de shutdown.
- **Test #12 lento sin virtual time**: 🦊 usa `runBlockingTest` → backoff auto-advanced.

## Out of scope (explícito)

- **MainViewModel tests**: requiere refactor para inyectar dispatcher (hoy `Dispatchers.IO` hardcoded + `viewModelScope`). Ticket aparte.
- **Instrumentation / Espresso**: no pedido.
- **E2E contra Binance real**: viola "no red real".
- **CheckNetworkConnection**: LiveData + ConnectivityManager, requiere Robolectric.
- **MainActivity**: UI, fuera de scope.
