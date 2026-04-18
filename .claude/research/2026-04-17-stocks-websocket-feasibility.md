# Stocks via WebSocket — Feasibility (reemplazar cripto Binance)

_Fecha: 2026-04-17_

## TL;DR (max 10 bullets)

- **Binance WS NO ofrece acciones.** Es exchange cripto puro. Stock Tokens discontinuados 2021-22, BLVT terminados abril 2024. No hay ni hubo equities reales en `stream.binance.com`.
- **US stocks (NASDAQ/NYSE) — hay alternativas free:** Alpaca IEX (`wss://stream.data.alpaca.markets/v2/iex`) y Finnhub (`wss://ws.finnhub.io`). Ambos piden API key gratis (registro web, sin tarjeta). Recomendado: **Alpaca**.
- **Merval (AR) — no hay WS público gratuito.** Primary (`apihub.primary.com.ar`) tiene WS pero requiere credenciales comerciales. BYMA requiere contacto. Finnhub free tier parece bloquear `.BA` (issue GitHub #366).
- **Alternativa Merval sin WS:** Yahoo Finance REST polling (`query1.finance.yahoo.com/v8/finance/chart/GGAL.BA`) — delay ~15 min, sin auth. NO es tiempo real.
- **Costo de integración en este proyecto:** `WebSocketClient`, `MainViewModel`, `CryptoTicker` son genéricos (magnitud trivial). Rompen: `Constants.kt` (URL + 100 USDT + helpers), `TickerData`/`CombinedStreamMessage` (envelope Binance), `CryptoTickerDataSource.parse()`, `CombinedStreamParsingTest` (magnitud alta).
- **Alpaca ≠ drop-in de Binance:** requiere handshake (auth + subscribe), campos distintos (`T`, `S`, `p`), y schema por array de trades no por envelope `stream`/`data`.
- **Gap clave:** el sort del `MainScreen` usa `Constants.SYMBOLS.indexOf(...)` — depende del hardcode cripto. Al cambiar feed hay que redefinir la lista de símbolos.
- **UI tiene `$` hardcodeado** (`CryptoTickerItem.kt:93`) — ok para USD, incorrecto si mezclás ARS.
- **Recomendación para el POC:** si querés mantener tiempo real, ir a Alpaca IEX con AAPL/TSLA/MSFT/… (US). Para Merval, o bien aceptar polling con delay Yahoo, o tramitar Primary sandbox. Mezclar en la misma vista requiere dos datasources.

---

## 🐬 Web findings

### Binance → stocks: NO

- Binance Stock Tokens (tokens 1:1 con acciones reales) discontinuados 2021-22.
- BLVT (tokens apalancados sobre cripto — nunca equities) terminados abril 2024.
- WebSocket `stream.binance.com` solo expone spot cripto, futures, options.
- Docs: <https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams>

### Tabla comparativa de alternativas

| Proveedor | WS URL | Auth | US | AR | Free tier | Payload |
|---|---|---|---|---|---|---|
| **Alpaca IEX** | `wss://stream.data.alpaca.markets/v2/iex` | API key gratis | ✅ (IEX) | ❌ | 1 conn / 30 símbolos | JSON plano (array de trades) |
| **Finnhub** | `wss://ws.finnhub.io` | API key gratis | ✅ | ⚠️ bloqueado free | 1 conn | JSON plano |
| **Twelve Data** | `wss://ws.twelvedata.com/v1/quotes/price` | API key, WS desde Pro | ✅ | ✅ (70+ mercados) | Solo trial símbolos | JSON plano |
| **Primary (AR)** | endpoint privado `apihub.primary.com.ar` | Credenciales comerciales | ❌ | ✅ Merval completo | Sin free | JSON/FIX |
| **Binance** | `wss://stream.binance.com:9443/ws/` | None | ❌ | ❌ | — | JSON plano |

### Alpaca IEX — flujo mínimo

```
Conectar → {"T":"success","msg":"connected"}
→ enviar {"action":"auth","key":"KID","secret":"SECRET"}
← {"T":"success","msg":"authenticated"}
→ enviar {"action":"subscribe","trades":["AAPL","TSLA","MSFT"]}
← [{"T":"t","S":"AAPL","p":189.55,"s":100,"t":"...","x":"V"}]
```

Campo `p` = precio (ya parseás algo similar en Binance ticker).

### Merval — opciones reales

- **Primary** (`apihub.primary.com.ar`): tiene WS "modelo suscripción" para Merval real-time, pero requiere cuenta comercial. Contacto: `remarkets@primary.com.ar`.
- **BYMA**: contactar `marketdata@byma.com.ar`. No self-service.
- **Yahoo Finance REST** (no WS): `query1.finance.yahoo.com/v8/finance/chart/{GGAL.BA,YPFD.BA,PAMP.BA,BMA.BA}` — delay ~15 min, sin auth. Viable para polling cada N segundos pero NO es tiempo real.

### Conflictos / dudas

- Finnhub advertise "global markets" pero issue #366 en GitHub (sin resolver) dice que `.BA` está bloqueado en free tier. Validar con key propia antes de apostar.
- Alpaca IEX ≈ 2-3% del volumen US; para SIP (consolidado) hay que pagar. Para un POC sobra.

---

## 🐜 Codebase findings

### Genérico (no rompe al cambiar feed)

- `WebSocketClient.kt` — transport puro, recibe URL string. OkHttp 4.9.0.
- `MainViewModel.kt` — Map<String, CryptoTicker>, `priceDirection` por BigDecimal, throttling 250ms, `ioDispatcher` inyectable.
- `CryptoTicker.kt` — `symbol/displayName/price/previousPrice/priceDirection/percentChange`. Solo el nombre es "Crypto".
- `PriceDirection`, `ConnectionState` — genéricos.

### Acoplado a cripto / Binance (hay que tocar)

| Archivo | Magnitud | Motivo |
|---|---|---|
| `Constants.kt` | Alto | URL Binance, 100 símbolos USDT (L7-29), `combinedStreamUrl()` (L31-34), `iconUrl()` removeSuffix("USDT") (L37), `displayName()` (L40-47) |
| `TickerData.kt` | Alto | Campos `s/c/P` mapeados al JSON Binance |
| `CombinedStreamMessage.kt` | Alto | Envelope `stream`+`data` específico de Binance combined streams |
| `CryptoTickerDataSource.kt` | Alto | URL default (L23), Moshi adapter (L72), `parse()` usa `Constants.displayName/iconUrl` USDT-aware |
| `CombinedStreamParsingTest.kt` | Alto | Todos los fixtures son JSON Binance (`BTCUSDT`, etc.) |
| `CryptoTickerItem.kt` | Trivial | `"$ ${ticker.price}"` hardcoded (L93), nombre de clase |
| `MainActivity.kt` | Trivial | Sort en L84 por `Constants.SYMBOLS.indexOf(...)` |
| `ViewModelFactory.kt` | Trivial | Instancia `CryptoTickerDataSource` con `WebSocketClient()` |

### Legacy / candidato a borrar

- `BitcoinTicker.kt`, `BitcoinTickerDataSource.kt`, `BitcoinTickerParsingTest.kt`: dead code, el VM activo no los usa.
- `Constants.WEB_SOCKET_URL` (L5): deprecated.

### Strings hardcoded Binance/USDT

`Constants.kt` L5, L7-29, L31-34, L37, L43-44; `CryptoTickerDataSource.kt` L23; `CombinedStreamParsingTest.kt` múltiples fixtures; `MainViewModelTest.kt` símbolos BTC/ETH/BNB; `CryptoTickerItem.kt` L93 `$`; `MainActivity.kt` L84.

---

## Cross-reference

### Acuerdos

- Binance WS = solo cripto. No hay camino a equities por esa vía.
- El schema actual (envelope `stream`/`data`, campos `s/c/P`) está ligado 1:1 a Binance combined streams.

### Complementos

- 🐬 identifica que Alpaca/Finnhub tienen handshake (auth + subscribe) → el código actual NO lo tiene: `CryptoTickerDataSource` solo abre socket y parsea mensajes entrantes.
- 🐜 confirma que `WebSocketClient` ya tiene backoff/retry, así que el handshake se enchufa en el listener sin tocar transport.

### Conflictos

- Ninguno relevante. Ambos coinciden en que el diseño de transport/VM/UI es genérico y que lo específico está concentrado en `Constants` + modelos DTO + parse().

### Gap analysis

| Necesidad Alpaca | Estado en el repo | Delta |
|---|---|---|
| Enviar `auth` con API key | No existe flujo de "mensaje al conectar" | Agregar paso en `DataSource` o en `WebSocketClient.onOpen` |
| Enviar `subscribe` con lista de símbolos | No existe | Agregar `sendMessage(JSON)` post-auth |
| Parsear array de trades (no envelope único) | Hoy parsea un objeto `CombinedStreamMessage` por mensaje | Nuevo adapter Moshi `List<Trade>` |
| Secrets (API key + secret) | No hay storage ni build config para secrets | Agregar a `local.properties` + `buildConfigField` o `gradle.properties` (NO hardcodear) |
| Rate limit 30 símbolos | Hoy lista 100 pares USDT | Reducir a ≤30 símbolos US |
| Sort por lista propia | `Constants.SYMBOLS` hardcoded cripto | Nueva lista de tickers (AAPL, TSLA, …) |
| Símbolo de moneda UI | `$` hardcodeado — ok USD | Sin cambio si solo US stocks; mezclar AR requiere formato por ticker |

### Aplicabilidad al proyecto

- **POC solo US stocks (Alpaca):** viable. ~4 archivos con cambios altos + secrets. Sin dependencias nuevas — Alpaca WS usa JSON + OkHttp (Moshi ya está).
- **POC Merval tiempo real:** bloqueado sin cuenta comercial. Si se quiere avanzar, la opción es Yahoo REST polling (no WS, ~15 min delay) — requiere repensar la arquitectura "socket de precios".
- **Mixto US + Merval:** dos datasources independientes. Feasible pero duplica el esfuerzo del punto 1.

---

## Conclusión

**Pregunta del usuario (¿Binance WS sirve para títulos de mercado?) → NO.** Binance solo cripto en 2026.

**Rutas viables:**

1. **US stocks via Alpaca IEX** — cambio de feed realista, tiempo real, API key gratis. Esfuerzo concentrado en `Constants`, `TickerData`/`CombinedStreamMessage`, `CryptoTickerDataSource`, tests de parseo. ~4 archivos alto + 4 trivial + manejar secrets.
2. **Merval tiempo real** — no hay opción pública free. Requiere tramitar Primary/BYMA.
3. **Merval delay ~15 min via Yahoo REST** — sí es posible pero NO es WebSocket; cambia la arquitectura del DataSource. Evaluar si el delay es aceptable para el POC.

**Decisión pendiente del usuario:** qué mercado priorizar (US vs. AR), y si está dispuesto a (a) registrar API key Alpaca, (b) aceptar delay si se va por AR, o (c) tramitar Primary.
