# Plan — TradingView Stock Heatmap en Compose

_Fecha: 2026-04-17_
_Tarea: agregar pantalla "Mapa de calor" con tabs Merval/SPY usando el widget HTML oficial de TradingView embebido en WebView._

## Resumen ejecutivo

Agregar un tab nuevo "Heatmap" a la app (bottom NavigationBar) que muestra el widget oficial de TradingView Stock Heatmap dentro de un WebView, con sub-tabs Merval y SPY. Cero dependencias nuevas, cero API keys, cero backend. La lista de crypto actual queda intacta en su propio tab "Precios".

## Decisiones del usuario

1. Stocks Merval + SPY (no crypto).
2. Tile sizing por market cap / volumen (lo hace el widget internamente).
3. Nuevo tab/pantalla, mantener lista de crypto.
4. Solo highlight/tooltip al tocar (lo maneja el widget).
5. Render: WebView + widget oficial TradingView.
6. Tabs Merval / SPY dentro del heatmap.

## Archivos

### Nuevos
- `app/src/main/java/com/example/socketapp/ui/heatmap/HeatmapScreen.kt` — Composable con `TabRow` Merval/SPY + `Box` WebView + overlays de loading/error/offline.
- `app/src/main/java/com/example/socketapp/ui/heatmap/TradingViewHeatmapWebView.kt` — `AndroidView` wrapper con WebSettings + `WebViewClient` + `enum class Market`.
- `app/src/main/assets/heatmap/merval_heatmap.html` — HTML con widget TV, `dataSource: "SPmerval"`.
- `app/src/main/assets/heatmap/spy_heatmap.html` — HTML con widget TV, `dataSource: "SPX500"`.
- `app/src/main/java/com/example/socketapp/ui/RootScreen.kt` — `Scaffold` + `NavigationBar` (Precios / Heatmap).
- `app/src/main/java/com/example/socketapp/ui/RootTab.kt` — `enum class RootTab { Prices, Heatmap }`.

### Modificados
- `app/src/main/java/com/example/socketapp/MainActivity.kt` — reemplazar `MainScreen(...)` por `RootScreen(mainViewModel, checkNetworkConnection)` en `setContent`.

### NO se tocan
- `build.gradle` (sin dependencias nuevas).
- `AndroidManifest.xml` (INTERNET ya está).
- `MainScreen`, `MainViewModel`, `CryptoTickerItem`, `Constants`.

## Configuración del WebView

- `javaScriptEnabled = true` (requisito TV; minimizado con whitelist URLs + sin `addJavascriptInterface`).
- `domStorageEnabled = true`.
- `setAllowFileAccess(false)`, `setAllowContentAccess(false)`.
- `allowFileAccessFromFileURLs = false`, `allowUniversalAccessFromFileURLs = false`.
- `setMixedContentMode(MIXED_CONTENT_NEVER_ALLOW)`.
- `cacheMode = LOAD_DEFAULT`.
- **Sin** `addJavascriptInterface(...)`.
- `WebViewClient.shouldOverrideUrlLoading` whitelist `tradingview.com` + subdominios; el resto → `Intent.ACTION_VIEW` externo.
- `AndroidView(..., onRelease = { it.destroy() })` para evitar leak.

## Estados edge

- Sin conexión al entrar → Compose vacío con texto "Sin conexión" (usar `CheckNetworkConnection`).
- Error de carga → overlay Compose con botón "Reintentar" (`webView.reload()`).
- Primer load → `CircularProgressIndicator` Compose hasta `onPageFinished` (timeout defensivo 15s).
- Cambio tab Merval↔SPY → `loadUrl(nuevoHtml)` en `update` de `AndroidView`, misma WebView (no recrear).

## División en developers (paralelo)

### 🐺 Developer A — Heatmap feature
- Crea `HeatmapScreen.kt`, `TradingViewHeatmapWebView.kt`, `merval_heatmap.html`, `spy_heatmap.html`.
- Enum `Market { MERVAL, SPY }` con path al asset.
- Merval dataSource: probar en orden `"SPmerval"` → `"BCBA:MERVAL"` → `"INDEX:BCBA:MERVAL"`; documentar el que funciona.
- NO toca `MainActivity`, `MainScreen`, `build.gradle`.

### 🦊 Developer B — Navegación root
- Crea `RootScreen.kt`, `RootTab.kt`.
- Modifica `MainActivity.kt` para usar `RootScreen`.
- Usa `Icons.Filled.*` core (sin `material-icons-extended`). Si no alcanza, labels de texto.
- `NavigationBar` con container color `Color(0xFF1E1E1E)`.
- NO toca archivos del heatmap.

Los dos developers trabajan en worktrees separados. Cero conflictos esperados (sets de archivos disjuntos + `MainActivity` solo lo toca 🦊).

## Seguridad

- Sin `addJavascriptInterface` — superficie cero.
- `setAllowFileAccess(false)` — HTML local no puede leer storage.
- `MIXED_CONTENT_NEVER_ALLOW` — bloquea http plano.
- URL whitelist en `shouldOverrideUrlLoading`.
- 🦂 audita el wrapper al final.

## Tests (alta nivel — 🐸 los detalla después)

- `RootTab` enum — trivial, opcional.
- `Market` enum + asset path resolution.
- Validación manual (no unit test sin Robolectric): WebView carga el widget, tab swap Merval↔SPY, retry tras error, offline state.

## Riesgos / decisiones pendientes del usuario

1. **dataSource Merval**: si los 3 slugs (`SPmerval` / `BCBA:MERVAL` / `INDEX:BCBA:MERVAL`) fallan, el usuario debe confirmar el ticker TradingView correcto.
2. **SPY vs SPX500**: el widget heatmap de TV usa índices como `SPX500` (S&P 500) como dataSource. Si el usuario quería específicamente el ETF `AMEX:SPY` (no el índice), hay que aclarar. El plan asume S&P 500 como equivalente funcional.
3. **Regression WS**: al cambiar Precios→Heatmap→Precios, `MainScreen` se recompone; `LaunchedEffect(isConnected)` podría no re-suscribir al WS si la red ya estaba conectada. Primera iteración: swap simple; si aparece el bug → follow-up con `Box` + ambos composables montados.

## Pre-flight (orquestador)

- `git status` tiene uncommitted en main: `.claude/research/INDEX.md` (M), `.claude/research/tradingview-android-widgets.md` (??), `.idea/inspectionProfiles/` (??). Los worktrees parten del último commit → hay que commitear estos archivos (research) o fixear sin worktree. Pregunta al usuario antes de lanzar implementación.
