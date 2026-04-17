# Research Index

Índice de investigaciones previas. Cada entrada apunta a un archivo `.md` en esta carpeta con el TL;DR y los detalles.

## Formato

```
- [Título](archivo.md) (YYYY-MM-DD)
  - **When to use**: <tipos de tareas donde aplica>
  - **Key conclusion**: <una línea>
```

## Política

- Antes de investigar un tema, **chequeá acá**. Si hay research reciente (<7 días), reusá.
- El orquestador inyecta la sección `## TL;DR (max 10 bullets)` de cada research relevante en el prompt del 🦧.

---

- [Market WebSocket Endpoints — Public No Auth](2026-04-13-market-websocket-endpoints.md) (2026-04-13)
  - **When to use**: Reemplazar el endpoint WebSocket de precio de cripto; elegir entre Binance/Bitstamp/Kraken/Coinbase AT.
  - **Key conclusion**: Binance `wss://stream.binance.com:9443/ws/btceur@ticker` es la opción con menos cambios (3 archivos, sin modelos nuevos); Bitstamp es la alternativa EU-nativa.

- [TradingView widgets para Android](tradingview-android-widgets.md) (2026-04-17)
  - **When to use**: Evaluar cómo integrar un gráfico candlestick tipo TradingView en la app; elegir entre TradingView Lightweight Charts, Advanced Charts, Vico o MPAndroidChart.
  - **Key conclusion**: TradingView NO tiene SDK nativo Android (todo WebView). Recomendación: **Vico** (Compose-native, Apache 2.0, v3.1.0 abr 2026) para este stack; **lightweight-charts-android** si se necesita look TV o indicadores.
