# Plan — Card "Favoritos" en TradingViewScreen

Fecha: 2026-04-18
Arquitecto: 🦧 (Opus)

## Tarea

Agregar card "Favoritos" con los primeros 5 stocks de `Constants.SYMBOLS` que estén presentes en el socket. Si no hay tickers (mercado cerrado), la card se oculta.

## Decisiones de producto

- Ubicación: primera card (arriba del Mapa de calor).
- Selección: `Constants.SYMBOLS.take(5)` → filtrar por los que estén en `tickerMap`.
- Lifecycle del socket: subir la suscripción de `StocksScreen` a `MainScreen`.
- Filas: reusar `StockTickerItem`.

## Archivos

### Crear

- `app/src/main/java/com/example/socketapp/ui/tradingview/Favorites.kt` — función pura `top5Favorites(tickerMap, symbols)`.
- `app/src/test/java/com/example/socketapp/ui/tradingview/FavoritesTest.kt` — 4 tests JVM puros.

### Modificar

- `app/src/main/java/com/example/socketapp/ui/tradingview/TradingViewScreen.kt` — agrega `favorites: List<StockTicker>` a la firma; renderiza `WidgetCard("Favoritos")` como primer hijo del Column si `favorites.isNotEmpty()`, con `StockTickerItem` + `Spacer(4.dp)` entre filas.
- `app/src/main/java/com/example/socketapp/ui/MainScreen.kt` — colecta `viewModel.tickers`, observa red, mueve acá el `LaunchedEffect(isConnected){ subscribe/stop }`, computa `val favoritesTop5 = remember(tickerMap){ top5Favorites(tickerMap) }` y lo pasa a `TradingViewScreen`.
- `app/src/main/java/com/example/socketapp/ui/stocks/StocksScreen.kt` — borrar el `LaunchedEffect(isConnected){ ... }` y los imports que queden huérfanos. NO tocar `ConnectionStatusBar` ni los botones manuales.

### NO tocar

`MainViewModel`, `MainActivity`, `StockTickerItem`, `Constants`, widgets TV.

## Equipo

**1 developer secuencial** (🦊) — los 3 archivos tienen dependencia directa (firma de `TradingViewScreen` + call-site en `MainScreen` + remoción del `LaunchedEffect` en `StocksScreen`). Paralelizar genera conflictos.

## Riesgos clave

1. **Doble suscripción transitoria** → `subscribeToSocketEvents()` ya es idempotente, pero el cambio (quitar de `StocksScreen` + agregar a `MainScreen`) se hace en el mismo batch.
2. **Recomposition cada 250ms** → `remember(tickerMap){ top5Favorites(tickerMap) }` + `StockTicker @Immutable` → Compose skipea filas sin cambios.
3. **Imports huérfanos en `StocksScreen`** → grep explícito post-edit.

## Tests (JVM puros, sin Compose)

1. map vacío → `[]`.
2. Respeta orden de `Constants.SYMBOLS.take(5)`.
3. Si faltan símbolos del top-5, devuelve solo los presentes.
4. Ignora símbolos fuera del top-5.
