# Market WebSocket Endpoints — Public, No Auth (2026-04-13)

## TL;DR (max 10 bullets)

- `wss://ws-feed.pro.coinbase.com` (Coinbase Pro) está **muerto** — deprecado, connection refused.
- Sucesor Coinbase: `wss://advanced-trade-ws.coinbase.com` — ticker funciona sin auth, pero payload anidado (`events[0].tickers[0].price`).
- **Binance** `wss://stream.binance.com:9443/ws/btceur@ticker` — sin auth, sin subscribe msg, JSON plano, precio en campo `"c"` (String). **Mínimo cambio de código para este POC**.
- **Bitstamp** `wss://ws.bitstamp.net` — sin auth, subscribe a `live_trades_btceur`, precio en `data.price` (Number). Patrón subscribe compatible con código existente.
- **Kraken v2** `wss://ws.kraken.com/v2` — sin auth para ticker, precio en `data[0].last` (Float). Requiere mayor refactor de modelos.
- Binance recomendado para este POC: 3 cambios mínimos, cero clases de modelo nuevas.
- Bitstamp es #2 si se requiere compliance EU o granularidad trade-by-trade.
- Kraken v2 es #3 — API limpia pero `last` (Float) + envoltura list necesita modelos nuevos.
- Coinbase Advanced Trade es viable pero el más anidado — mejor si uno quiere seguir en el ecosistema Coinbase.
- Los cuatro son gratis, públicos, sin API key en 2026.

## Endpoint 1 — Binance (RECOMENDADO)

**URL:** `wss://stream.binance.com:9443/ws/btceur@ticker`
Alt port: `wss://stream.binance.com:443/ws/btceur@ticker`

**Subscribe message:** Ninguno — la URL del stream suscribe al conectar.

**Payload (plano, top-level):**
```json
{
  "e": "24hrTicker",
  "E": 1672515782136,
  "s": "BTCEUR",
  "c": "34220.10",
  "o": "34099.60",
  "h": "34450.00",
  "l": "33900.10",
  "v": "1250.43",
  "p": "120.50",
  "P": "0.35",
  "b": "34219.00",
  "a": "34221.00",
  "n": 18151
}
```

- **Campo precio:** `"c"` (String, last price)
- **Auth:** Ninguna
- **Límites:** 5 msg/s in, 300 conexiones/5 min/IP, auto-close a las 24h, ping cada 20s (OkHttp maneja pong)

**Pros:** JSON plano, sin subscribe, par BTC/EUR nativo, muy estable.
**Cons:** Campo `"c"` no `"price"` — necesita `@Json(name = "c")`. Ticker 24h rolling, no per-trade. Algunas jurisdicciones EU pueden restringir el dominio Binance.com.

**Cambios de código — 3 archivos:**

`Constants.kt`:
```kotlin
const val WEB_SOCKET_URL = "wss://stream.binance.com:9443/ws/btceur@ticker"
```

`BitcoinTicker.kt`:
```kotlin
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BitcoinTicker(@Json(name = "c") val price: String?)
```

`SocketListener.kt` — `onOpen` (remover `webSocket.send(...)`):
```kotlin
override fun onOpen(webSocket: WebSocket, response: Response) {
    // Binance: la URL del stream ya suscribe, no hace falta mandar mensaje
    scope.launch {
        _socketBitcoinPriceEventChannel.emit(TickerResult.Connected)
    }
}
```

`SocketListener.kt` — `onClosing` (remover unsubscribe `webSocket.send(...)`):
```kotlin
override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
    Log.d(TAG, "onClosing: $code $reason")
    scope.launch {
        _socketBitcoinPriceEventChannel.emit(TickerResult.Error(SocketAbortedException()))
    }
}
```

---

## Endpoint 2 — Bitstamp

**URL:** `wss://ws.bitstamp.net`

**Subscribe:**
```json
{ "event": "bts:subscribe", "data": { "channel": "live_trades_btceur" } }
```

**Unsubscribe:**
```json
{ "event": "bts:unsubscribe", "data": { "channel": "live_trades_btceur" } }
```

**Payload (trade):**
```json
{
  "event": "trade",
  "channel": "live_trades_btceur",
  "data": {
    "id": 21565524,
    "timestamp": "1505558814",
    "amount_str": "0.01500000",
    "price": 34220.80,
    "price_str": "34220.80",
    "type": 1
  }
}
```

- **Campo precio:** `data.price` (Double) — usar `data.price_str` (String) para mantener `BitcoinTicker.price: String?`
- **Auth:** Ninguna
- **Pros:** Regulado en EU, trade-by-trade, patrón subscribe calza con el código actual.
- **Cons:** Modelo anidado. Mensajes espaciados en momentos de poco volumen.

Modelos nuevos:
```kotlin
@JsonClass(generateAdapter = true)
data class BitstampMessage(val event: String?, val data: BitstampData?)

@JsonClass(generateAdapter = true)
data class BitstampData(@Json(name = "price_str") val price: String?)
```

---

## Endpoint 3 — Kraken WebSocket v2

**URL:** `wss://ws.kraken.com/v2`

**Subscribe:**
```json
{ "method": "subscribe", "params": { "channel": "ticker", "symbol": ["BTC/EUR"] } }
```

**Payload:**
```json
{
  "channel": "ticker",
  "type": "snapshot",
  "data": [
    {
      "symbol": "BTC/EUR",
      "bid": 34219.00, "ask": 34221.00,
      "last": 34220.10,
      "volume": 1250.43, "low": 33900.10, "high": 34450.00,
      "change": 120.50, "change_pct": 0.35,
      "timestamp": "2025-04-13T14:00:00.000000Z"
    }
  ]
}
```

- **Campo precio:** `data[0].last` (Float/Double)
- **Auth:** Ninguna para ticker
- **Pros:** API v2 limpia, EU-compliant, updates por trade.
- **Cons:** `last` es Float, `data` es lista — mayor refactor. Primer mensaje es `"snapshot"`.

---

## Endpoint 4 — Coinbase Advanced Trade (migración desde Coinbase Pro)

**URL:** `wss://advanced-trade-ws.coinbase.com`

**Subscribe (sin JWT para ticker):**
```json
{ "type": "subscribe", "product_ids": ["BTC-EUR"], "channel": "ticker" }
```

**Payload:**
```json
{
  "channel": "ticker",
  "timestamp": "2023-02-09T20:30:37.167359596Z",
  "sequence_num": 0,
  "events": [
    {
      "type": "snapshot",
      "tickers": [
        {
          "type": "ticker",
          "product_id": "BTC-EUR",
          "price": "34220.98",
          "volume_24_h": "1250.28",
          "best_bid": "34219.98",
          "best_ask": "34221.98"
        }
      ]
    }
  ]
}
```

- **Campo precio:** `events[0].tickers[0].price` (String) — más anidado
- **Auth:** No requerido para ticker. Rate limit: ~8 msg/s sin auth por IP.
- **Pros:** Migración directa desde Coinbase Pro, campo `"price"` igual al modelo actual.
- **Cons:** Payload más anidado — 3 modelos wrapper. JWT refresh cada 2 min si se agrega auth.

---

## Recomendación final para el POC

**Usar Binance** (`wss://stream.binance.com:9443/ws/btceur@ticker`).

Requiere 3 cambios mínimos y cero clases de modelo nuevas:

1. `Constants.kt` — cambiar `WEB_SOCKET_URL`.
2. `BitcoinTicker.kt` — agregar `@Json(name = "c")`.
3. `SocketListener.kt` — remover `webSocket.send(...)` de `onOpen` y `onClosing`.

---

## Sources

- [Advanced Trade WebSocket Channels — Coinbase Developer Documentation](https://docs.cdp.coinbase.com/coinbase-app/advanced-trade-apis/websocket/websocket-channels)
- [Advanced Trade WebSocket Overview — Coinbase Developer Platform](https://docs.cdp.coinbase.com/coinbase-app/advanced-trade-apis/websocket/websocket-overview)
- [WebSocket Streams — Binance Open Platform](https://developers.binance.com/docs/binance-spot-api-docs/web-socket-streams)
- [Binance spot-api-docs web-socket-streams.md — GitHub](https://github.com/binance/binance-spot-api-docs/blob/master/web-socket-streams.md)
- [Ticker (Level 1) — Kraken API Center](https://docs.kraken.com/api/docs/websocket-v2/ticker/)
- [Bitstamp Websocket API](https://www.bitstamp.net/websocket/v2/)

---

## Conflicts / Doubts

- Binance.com bloqueado en algunos países EU (BE, NL) — comportamiento de `stream.binance.com` en esas regiones sin verificar; probar desde dispositivo objetivo.
- Rate limit Coinbase AT sin auth (8 msg/s) aproximado; página de rate-limits no renderizó.
- Kraken `last`: "solo garantizado si hubo trade en últimas 24h" — irrelevante para BTC/EUR en la práctica.
- Bitstamp `price` es Number (Double) en payload — usar `price_str` (String) para evitar mismatch con el `BitcoinTicker(val price: String?)` actual.
