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

- [Stocks via WebSocket — Feasibility](2026-04-17-stocks-websocket-feasibility.md) (2026-04-17)
  - **When to use**: Reemplazar el feed cripto Binance por precios de acciones/títulos (US y/o Merval) en tiempo real.
  - **Key conclusion**: Binance WS NO ofrece equities. US stocks → **Alpaca IEX** (`wss://stream.data.alpaca.markets/v2/iex`, API key gratis). Merval → no hay WS público gratuito; solo Primary (comercial) o Yahoo REST con delay ~15 min.

- [Light Theme Design](2026-04-17-light-theme-design.md) (2026-04-17)
  - **When to use**: Migrar la app a light theme; definir paleta, tipografía y tokens M3; flip de TradingView widgets a light.
  - **Key conclusion**: Paleta **"Soft Sky"** (azul pastel bancario, primary `#7FA8D1`, bg `#F7FAFC`, onSurface `#2B3A4A`) + semantic sage/coral (`#4CAF7F`/`#E88B8B`) + Inter bundleada con `fontFeatureSettings="tnum"`. 13 puntos hardcoded a reemplazar + crear `ui/theme/{Color,Theme,Typography}.kt`. _Superseded por 2026-04-18 — usuario rechazó el look pastel._

- [Light Theme con Carácter](2026-04-18-light-theme-with-character.md) (2026-04-18)
  - **When to use**: Despastelizar la app manteniendo light mode; paleta editorial con personalidad; naranja accent como signature accent.

- [TradingView widgets — tap abre navegador](2026-04-18-tradingview-widgets-click-redirect.md) (2026-04-18)
  - **When to use**: Evitar que los widgets TV (Stock Heatmap, Hotlists) abran tradingview.com al tocar dentro de la app; dejarlos como vitrina.
  - **Key conclusion**: Es diseño de los widgets `embed-widget-*.js`. Fix: en `TradingViewWebView.kt` devolver `true` en `shouldOverrideUrlLoading` cuando `isForMainFrame`; agregar `WebChromeClient.onCreateWindow { false }`; `setSupportMultipleWindows(true)` + `javaScriptCanOpenWindowsAutomatically = false`. Los scripts embed no tienen parámetro de "custom link" (eso es solo en Web Components `<tv-*>`).
