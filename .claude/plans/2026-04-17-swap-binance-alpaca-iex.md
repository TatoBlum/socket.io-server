# Plan — Swap Binance → Alpaca IEX

_Fecha: 2026-04-17_

## Research consulted

- `.claude/research/2026-04-17-stocks-websocket-feasibility.md` — TL;DR + schema Alpaca + gap analysis.
- `.claude/research/INDEX.md`.
- `.claude/agents/learnings.md`.
- Memoria: `project_websocket_refactor_2026_04_13`, `project_multi_ticker_2026_04_14`, feedback StateFlows sueltos + `ioDispatcher` inyectable.

## Existing code analysis

- `CryptoTickerDataSource` encapsula policy (URL, backoff/retry `retryWhen` 5 intentos, Moshi singleton, `parse()` privado). `WebSocketClient.connect(url)` es transport puro y cold.
- `MainViewModel` genérico: dual-coroutine (collect + publish 250ms) con `synchronized(internalMap)`, `ioDispatcher` inyectable. No se toca.
- `FakeCryptoTickerDataSource` extiende `CryptoTickerDataSource(...)` → impactado por rename.
- `MainActivity.kt:83` usa `Constants.SYMBOLS.indexOf(ticker.symbol.lowercase())`. Alpaca emite uppercase → cambiar sort a case-sensitive.
- `CryptoTickerItem.kt:74` usa `Constants.iconUrl(ticker.symbol)` con Coil.
- `build.gradle` ya tiene `buildConfig true` (L36-37) y `org.json:json:20240303` en testImplementation (L55).
- `proguard-rules.pro` tiene `-keep` para `BitcoinTicker`, `CombinedStreamMessage`, `TickerData`.
- `BitcoinTickerDataSource:27` aún referencia `Constants.WEB_SOCKET_URL` — dead code contenido.
- `RootScreen.kt:51-55` pasa `searchQuery` a `MainScreen`.

---

## Decisiones arquitectónicas

### A. Secrets — `local.properties` + `buildConfigField`
Leer `ALPACA_KEY_ID`/`ALPACA_SECRET_KEY` desde `local.properties` en `app/build.gradle`, exponer via `buildConfigField`. Fallback a `""` si faltan (no falla build). `StockTickerDataSource` detecta vacío al `start()` y emite `ConnectionState.Failed(IllegalStateException("Alpaca API key missing"))`. `local.properties.example` nuevo. `.gitignore` ya excluye (verificar). README con link a <https://app.alpaca.markets/paper/dashboard/overview>.

### B. Handshake — hook `onOpen(ws)` en `WebSocketClient`, policy en `StockTickerDataSource`
Extender `WebSocketClient.connect(url, onOpen: (WebSocket) -> Unit = {})`. En el listener `onOpen`, invocar el hook post-`_connectionState.value = Connected`. El DataSource envía auth + subscribe secuencialmente dentro del hook — sin state machine "esperar authenticated" (si auth falla, Alpaca cierra y el backoff existente reintenta).

### C. Schema mensaje — `JSONTokener` + discriminador manual
Alpaca manda array (trades) u objeto suelto (saludos). Parsear con `org.json.JSONTokener` en runtime (no requiere dep — `org.json` está en Android runtime). Iterar elementos con `when (T) { "t" -> trade; else -> ignore }`. Cero Moshi para el nuevo DTO → una clase menos en ProGuard. Data class `AlpacaTrade(symbol, price)` sin `@JsonClass`.

### D. Símbolos iniciales — 25 US tickers uppercase
`AAPL, TSLA, MSFT, NVDA, GOOGL, AMZN, META, AMD, NFLX, INTC, DIS, BA, JPM, V, MA, KO, PEP, WMT, XOM, CVX, PYPL, UBER, SHOP, COIN, SQ`. Margen sobre el límite 30 de Alpaca free. Sort en `MainActivity` a case-sensitive (`ticker.symbol` sin `.lowercase()`).

### E. Icons — placeholder letra monocromo
Reemplazar `AsyncImage` por `Box` circular 32dp con `Text(ticker.symbol.take(2))`. Color de fondo desde paleta fija de 8 colores indexada por `symbol.hashCode()`. Cero HTTP, cero 404s, fase futura puede swap por Clearbit si queda feo.

### F. % change 24h — ocultar por ahora
Alpaca trade stream no lo trae. `StockTickerDataSource` emite `percentChange = "0.00"` y `StockTickerItem` esconde el text si vale `"0.00"`. Campo del modelo se mantiene para fase futura que use bars/snapshot REST.

### G. Naming — renombrar a `StockTicker*`
- `CryptoTicker` → `StockTicker`
- `CryptoTickerDataSource` → `StockTickerDataSource`
- `CryptoTickerItem` → `StockTickerItem`
- `FakeCryptoTickerDataSource` → `FakeStockTickerDataSource`

Rename mecánico (~30 sitios). Evita fricción mental permanente. Precedente: `heatmap → tradingview`.

### H. Dead code — borrar ahora
`BitcoinTicker.kt`, `BitcoinTickerDataSource.kt`, `BitcoinTickerParsingTest.kt`, `Constants.WEB_SOCKET_URL`, entrada `-keep BitcoinTicker` en ProGuard. Zero usages productivos confirmado. Limpiar en Fase 2.

### I. Tests
- **Nuevos**: `AlpacaTradesParsingTest` — array vacío, 3 trades, mezcla success/error/trade, trade incompleto ignorado, `p` Number → String 2 decimales, objeto suelto no crashea.
- **Actualizar**: `MainViewModelTest` — símbolos `BTCUSDT` → `AAPL`, rename fake.
- **Borrar**: `BitcoinTickerParsingTest`, `CombinedStreamParsingTest`.
- Test de handshake (auth/subscribe payloads) queda como tech debt si agrega mucha fixture — preferir validación manual (punto J).

### J. Validación manual
1. `cp local.properties.example local.properties` + pegar keys de Alpaca paper dashboard.
2. Run app. Logcat filtrar `WebSocketClient` + `StockTickerDataSource`.
3. Esperar `onOpen: 101` → `auth sent` → `subscribe sent` (sin loguear secret) → `{"T":"success","msg":"authenticated"}` → trades fluyendo (solo 9:30-16:00 ET).
4. UI: barra verde, 25 filas con avatar letra + símbolo + precio USD.
5. Auth error test: secret inválido → barra roja, retry loop.
6. Missing keys test: `local.properties` vacío → "Alpaca API key missing" sin crashear.

---

## Fases

### Fase 1 — Secrets infra

**Archivos**: `app/build.gradle` (modify), `local.properties.example` (new), `.gitignore` raíz (verify), `README.md` (add section).

**Cambios**:
- `app/build.gradle`: leer `local.properties` via Groovy `Properties`, declarar `buildConfigField "String" "ALPACA_KEY_ID" "\"${localProps.getProperty('ALPACA_KEY_ID','')}\""` (y `ALPACA_SECRET_KEY`).
- `local.properties.example` con placeholders comentados.
- README nuevo section "Configurar API key Alpaca" (3 pasos + link).

**Tests**: ninguno.

**Bloquea**: Fase 3.

**Done cuando**: `./gradlew assembleDebug` compila sin `local.properties`; con keys puestas, `BuildConfig.ALPACA_KEY_ID` no vacío; `git status` no muestra `local.properties`.

### Fase 2 — Rename + dead code + Constants stocks

**Archivos**:
- Rename: `CryptoTicker.kt` → `StockTicker.kt`, `CryptoTickerDataSource.kt` → `StockTickerDataSource.kt` (solo rename, lógica cripto se mantiene temporalmente), `ui/CryptoTickerItem.kt` → `ui/StockTickerItem.kt`.
- Modify: `Constants.kt`, `MainActivity.kt` (imports + sort sin `.lowercase()`), `ViewModelFactory.kt`, `MainViewModel.kt`, `ui/RootScreen.kt`, `MainViewModelTest.kt`, `proguard-rules.pro` (quitar BitcoinTicker).
- Delete: `BitcoinTicker.kt`, `BitcoinTickerDataSource.kt`, `BitcoinTickerParsingTest.kt`.

**Cambios**:
- `Constants.kt`: borrar `WEB_SOCKET_URL`, reemplazar 100 USDT por 25 US uppercase, `combinedStreamUrl()` → `const val ALPACA_WS_URL = "wss://stream.data.alpaca.markets/v2/iex"`, borrar `iconUrl()`, `displayName(s)` retorna `s` directo.
- Rename mecánico clases.
- `MainActivity:83` sort case-sensitive.
- `MainViewModelTest`: `BTCUSDT` → `AAPL`, `ETHUSDT` → `TSLA`, `BNBUSDT` → `MSFT`; rename fake.

**Tests**: `MainViewModelTest` actualizado pasa.

**Bloquea**: Fase 3.

**Done cuando**: compila; `./gradlew testDebugUnitTest` pasa; `grep -r "Bitcoin\|USDT\|CryptoTicker" app/src/` vacío; app muestra "Cargando…" (DataSource aún apunta a Binance en su URL default).

### Fase 3 — Alpaca protocol

**Archivos**:
- New: `AlpacaTrade.kt`, `AlpacaTradesParsingTest.kt`.
- Modify: `StockTickerDataSource.kt` (rewrite `parse()`, auth+subscribe, constructor con keyId/secret, URL Alpaca), `WebSocketClient.kt` (param `onOpen`), `ViewModelFactory.kt` (read `BuildConfig.ALPACA_*`), `proguard-rules.pro` (quitar `CombinedStreamMessage` y `TickerData`).
- Delete: `TickerData.kt`, `CombinedStreamMessage.kt`, `CombinedStreamParsingTest.kt`.

**Cambios**:
- `WebSocketClient.connect(url, onOpen: (WebSocket) -> Unit = {}): Flow<String>` — invoca hook en listener `onOpen` post-`Connected`.
- `StockTickerDataSource(client, keyId, secret, symbols = Constants.SYMBOLS, url = Constants.ALPACA_WS_URL)`.
  - `start()`: si keys vacías, `flow { throw IllegalStateException("Alpaca API key missing — see local.properties.example") }`.
  - `client.connect(url, onOpen = { ws -> ws.send(authJson()); ws.send(subscribeJson()) })`.
  - Construir JSON con `JSONObject`/`JSONArray` (no concat strings).
  - `parse(text)`: `JSONTokener(text).nextValue()` → si array, iterar y mapear `T=="t"` a `StockTicker`; si objeto, ignorar. `mapNotNull` → `flatMapConcat { parse(text).asFlow() }`. `retryWhen` **después** del `flatMapConcat`. Try/catch alrededor de parse → emit empty list.
  - Log nunca el secret. Si loguear keyId: `keyId.take(4) + "***"`. `onMessage` log trimmed a `text.take(200)`.
  - Startup log: `"Alpaca connected — trades flow only during market hours (9:30-16:00 ET)"`.
- `AlpacaTrade` data class (sin `@JsonClass`).
- `ViewModelFactory`: pasar `BuildConfig.ALPACA_KEY_ID/SECRET_KEY`.

**Tests**: `AlpacaTradesParsingTest` (decisión I). `MainViewModelTest` debería pasar sin cambios adicionales (verificar).

**Bloquea**: Fase 4.

**Done cuando**: unit tests pasan; con keys válidas en horario de mercado, precios fluyen; keys inválidas → `Failed`; sin keys → `Failed: Alpaca API key missing` sin crash.

### Fase 4 — UI polish

**Archivos**: `ui/StockTickerItem.kt` (modify).

**Cambios**:
- `AsyncImage` → `Box` circular 32dp con `Text(ticker.symbol.take(2))`. Color de fondo: paleta de 8, index por `symbol.hashCode() and 0x7FFFFFFF % 8`.
- Dejar `"$ ${ticker.price}"`.
- Esconder `Text(percentChange)` si `percentChange == "0.00"`.

**Tests**: ninguno (UI visual).

**Done cuando**: 25 filas con letras + precio, sin 404s, segunda línea oculta, layout sin warnings.

---

## Developers — 3 agentes

### Developer 🐺 — Fase 1 (infra)
- **Crear**: `local.properties.example`, sección README.
- **Modificar**: `app/build.gradle`, `.gitignore` raíz si falta `local.properties`.
- **NO tocar**: `src/main/java`, `src/test/java`, `Constants.kt`.
- **Verificar**: `grep -n "local.properties" .gitignore` antes de editar; `./gradlew assembleDebug` sin `local.properties` debe compilar y `BuildConfig.ALPACA_KEY_ID = ""`.

### Developer 🦊 — Fase 2 (rename + dead code + Constants)
- **Crear (via rename)**: `StockTicker.kt`, `StockTickerDataSource.kt` (lógica cripto intacta), `ui/StockTickerItem.kt`.
- **Modificar**: `Constants.kt` (URL, SYMBOLS, remover iconUrl, displayName noop), `MainActivity.kt` (imports + sort uppercase), `ViewModelFactory.kt`, `MainViewModel.kt`, `ui/RootScreen.kt`, `MainViewModelTest.kt`, `proguard-rules.pro`.
- **Eliminar**: `CryptoTicker.kt`, `CryptoTickerDataSource.kt`, `ui/CryptoTickerItem.kt`, `BitcoinTicker.kt`, `BitcoinTickerDataSource.kt`, `BitcoinTickerParsingTest.kt`.
- **NO tocar**: `WebSocketClient.kt`, `TickerData.kt`, `CombinedStreamMessage.kt`, `CombinedStreamParsingTest.kt`, `app/build.gradle`, `ui/tradingview/*`.
- **Verificar**: `grep -ri "crypto\|bitcoin\|USDT" app/src/` vacío; tests pasan.

### Developer 🐆 — Fase 3 + Fase 4 (protocol + UI)
- **Crear**: `AlpacaTrade.kt`, `AlpacaTradesParsingTest.kt`.
- **Modificar**: `StockTickerDataSource.kt` (rewrite completo), `WebSocketClient.kt` (param `onOpen` con default), `ViewModelFactory.kt` (BuildConfig), `ui/StockTickerItem.kt` (placeholder letra + hide % change), `proguard-rules.pro`.
- **Eliminar**: `TickerData.kt`, `CombinedStreamMessage.kt`, `CombinedStreamParsingTest.kt`.
- **NO tocar**: `Constants.kt`, `MainViewModel.kt`, `MainActivity.kt`, `app/build.gradle`.
- **Reglas**:
  - `org.json` para todo el parseo (no Moshi para AlpacaTrade).
  - `JSONObject.put().toString()` para auth/subscribe (no concat).
  - Nunca loguear secret; keyId trimmed.
  - `start()` valida keys vacías → `IllegalStateException`.
  - `flatMapConcat` para emitir N tickers por mensaje; `retryWhen` después.
  - `p` como Number: `.optDouble("p")` + format 2 decimales.

**Secuencial**: Fase 2 → Fase 3 (🐆 borra archivos que 🦊 aún referencia). Fase 1 puede paralelizarse con Fase 2.

---

## Riesgos y mitigaciones

- **Firma `WebSocketClient.connect` cambia** → default param `onOpen = {}` preserva compat.
- **Race auth vs "connected" saludo** → parser ignora silenciosamente todo `T!="t"`; Alpaca procesa auth en cualquier orden.
- **Logcat spam de mensajes grandes** → 🐆 limita `onMessage` log a `text.take(200)`.
- **`JSONException` mata el cold flow** → try/catch en `parse()`, emit empty list; `retryWhen` sigue protegiendo de errores de I/O.
- **`local.properties` committeado con secret** → 🐺 verifica `.gitignore`; orquestador corre `grep -l "ALPACA_KEY_ID=" .` pre-commit.
- **Mercado cerrado interpreta como bug** → README + startup log aclaran horario 9:30-16:00 ET.
- **Rate limit 1 conexión free tier** → README advierte; no es code problem.
- **`p` Number vs String** → `.optDouble("p")` + format; test explícito.
- **Sort `MainActivity` case mismatch** → checklist 🦊: lista uppercase + lookup uppercase.
- **🐆 vs 🦊 colisión archivos borrados** → fases secuenciales, no paralelas entre 2 y 3.

## Fuera de scope (para futuro)

- Quotes/bars streams para % change 24h real.
- SIP feed Alpaca (pago).
- Merval (Primary comercial o Yahoo REST con delay).
- Multi-feed (US + crypto simultáneo).
- Migración kapt → KSP.
- Full company names.
- Logos reales (Clearbit).
- Test de handshake (auth/subscribe payloads).
