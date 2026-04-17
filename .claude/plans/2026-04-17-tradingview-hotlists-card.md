# Plan — Segunda card TradingView Hotlists en HeatmapScreen

_Fecha: 2026-04-17_
_Tarea: agregar una segunda card a `HeatmapScreen` con el widget oficial de TradingView Hotlists y segmented control BCBA/NASDAQ/NYSE, con el mismo criterio visual/técnico que la card de Heatmap existente._

## Resumen ejecutivo

Refactor mínimo de la infra WebView de TradingView para soportar múltiples widgets con un único template HTML parametrizable por `{{SCRIPT_SRC}}`, y una segunda card en `HeatmapScreen` que renderiza `embed-widget-hotlists.js` con selector de exchange. Cero dependencias nuevas, cero API keys, cero backend.

## Decisiones del usuario

1. **Ubicación:** segunda card en `HeatmapScreen.kt`, debajo del heatmap actual.
2. **Selector:** segmented control BCBA / NASDAQ / NYSE.

## Archivos

### Nuevos
- `app/src/main/java/com/example/socketapp/ui/heatmap/TradingViewWebView.kt` — infra reusable: `BASE_URL`, `ALLOWED_HOSTS`, `isAllowedHost`, `TimeoutHolder`, `CONFIG_PLACEHOLDER`, `SCRIPT_PLACEHOLDER`, `TEMPLATE_ASSET`, `createTradingViewWebView(...)`, `loadTradingViewWidget(...)`.
- `app/src/main/java/com/example/socketapp/ui/heatmap/HeatmapWidget.kt` — `HeatmapConfig`, `enum Market`, `@Composable TradingViewHeatmapWebView(...)`.
- `app/src/main/java/com/example/socketapp/ui/heatmap/HotlistsWidget.kt` — `HotlistsConfig`, `enum Exchange { BCBA, NASDAQ, NYSE }`, `@Composable TradingViewHotlistsWebView(...)`.
- `app/src/main/java/com/example/socketapp/ui/heatmap/HeatmapColors.kt` — 6 colores como `internal val` top-level.
- `app/src/main/assets/tradingview/tradingview_widget_template.html` — template parametrizable con `{{SCRIPT_SRC}}` + `{{CONFIG}}`.

### Modificados
- `app/src/main/java/com/example/socketapp/ui/heatmap/HeatmapScreen.kt` — segunda card, `SegmentedControl` reusable, `WidgetCard` reusable, estados independientes por card.

### Eliminados
- `app/src/main/java/com/example/socketapp/ui/heatmap/TradingViewHeatmapWebView.kt` (contenido partido en `TradingViewWebView.kt` + `HeatmapWidget.kt`).
- `app/src/main/assets/heatmap/heatmap_template.html` (reemplazado).

### NO se tocan
- `MainActivity.kt`, `MainScreen`, `RootScreen`, `RootTab`, `build.gradle`, `AndroidManifest.xml`.

## Diseño del modelo

**`HotlistsConfig`** — campos del HTML provisto (exchange, colorTheme, dateRange, showChart, locale, largeChartUrl, isTransparent, showSymbolLogo, showFloatingTooltip, plotLineColorGrowing/Falling, gridLineColor, scaleFontColor, belowLineFillColor*, symbolActiveColor). `toJson()` sobrescribe `width:"100%"` y `height:"100%"` para que el `syncSize()` tome control.

**`Exchange { BCBA, NASDAQ, NYSE }`** con `displayName = name` y `config: HotlistsConfig(exchange = name)`.

**`Market`** sigue igual (MERVAL, SPY).

## Configuración del segmented control + card

- **Sin título** en la 2da card (decisión del usuario). La card arranca directo con el segmented control.
- Altura del Box del widget: **570.dp** (HTML pide 550; margen para evitar recortes).
- Colores, tipografía, estilo iOS-like: **idénticos al heatmap** (mismos `internal val` en `HeatmapColors.kt`).
- Estado inicial: `Exchange.BCBA`.
- Interactividad: **visual-only** (swallow de touches, igual que heatmap).

## Template HTML parametrizable

Un único asset en `assets/tradingview/tradingview_widget_template.html`:
- CSP (ya existente para heatmap) — cubre hotlists sin cambios.
- `syncSize()` JS (fix del bug `height:100%`) — reusar idéntico.
- `<script src="{{SCRIPT_SRC}}" async>` + body `{{CONFIG}}`.
- `loadTradingViewWidget(webView, templateHtml, scriptSrc, configJson)` hace `replace(CONFIG).replace(SCRIPT_SRC)` + `loadDataWithBaseURL`.

## Seguridad

- CSP idéntica al heatmap actual (whitelist `*.tradingview.com` + `*.tradingview-widget.com`). Verificar en debug si el widget hotlists pide otros dominios; ampliar solo si es necesario.
- Sin `addJavascriptInterface`, `setAllowFileAccess(false)`, `MIXED_CONTENT_NEVER_ALLOW`, whitelist en `shouldOverrideUrlLoading`.
- `setOnTouchListener { _, _ -> true }` — swallow de touches para mantener visual-only (propuesta; ver preguntas abiertas).

## División en developers

**Secuencial, no paralelo** (scope chico + dependencia directa de símbolos).

### 🐺 Developer A — Infra + Hotlists
- Crea los 4 archivos Kotlin nuevos + template HTML.
- Elimina `TradingViewHeatmapWebView.kt` y `heatmap_template.html`.
- NO toca `HeatmapScreen.kt`.
- Checklist grep post-build: `embed-widget-hotlists.js`, `enum class Exchange`, `{{SCRIPT_SRC}}`, `createTradingViewWebView`, `HeatmapColors.kt`, ausencia de archivos viejos.

### 🦊 Developer B — HeatmapScreen refactor + segunda card
- Se lanza **después** de que 🐺 termine y el orquestador copie al workspace.
- Modifica `HeatmapScreen.kt`: extrae `SegmentedControl<T>` y `WidgetCard`, elimina `private val` de colores, agrega segunda card Hotlists.
- Importa `Exchange`, `Market`, `TradingViewHeatmapWebView`, `TradingViewHotlistsWebView`, colores desde los archivos nuevos.

## Estados edge

- **Offline:** cada card muestra su cartel "Sin conexión" dentro de su Box (igual que hoy).
- **Loading:** `CircularProgressIndicator` independiente por card con su propio `isLoading`.
- **Error:** overlay con botón "Reintentar" independiente por card con su propio `reloadTrigger`.
- **Tab swap BCBA↔NASDAQ↔NYSE:** `loadDataWithBaseURL` con nuevo JSON (no recrear WebView; el `update` del `AndroidView` compara `lastExchange` + `lastReloadKey`).

## Riesgos

1. **CSP** puede bloquear dominios nuevos del widget hotlists. Mitigación: grep consola en debug; ampliar whitelist si hace falta.
2. **Bug altura `height:100%`** ya resuelto con `syncSize()` — verificar que aplique al hotlists también.
3. **Developer aplica subset de fixes** (learning 2026-04-17): orquestador corre grep checklist literal al final; si falta algo, Write del archivo completo.
4. **Uncommitted en `HeatmapScreen.kt`**: commitear antes de trabajar o hacer Edit directo sin worktree.

## Decisiones finales del usuario

1. **Interactividad**: visual-only (swallow de touches, como el heatmap).
2. **Título**: sin título en la segunda card.
3. **Pre-flight**: commit de los cambios uncommitted antes de lanzar developers.

## Pre-flight del orquestador

- Commit de los 13 cambios en `HeatmapScreen.kt` aprobado por el usuario → hacerlo antes de lanzar 🐺.
