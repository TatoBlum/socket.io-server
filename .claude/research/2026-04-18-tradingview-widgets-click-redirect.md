# TradingView widgets — tap abre navegador externo

_Fecha: 2026-04-18_

## TL;DR (max 10 bullets — para inyección en prompts)

- El redirect al tap es **comportamiento esperado** de los widgets `embed-widget-*.js` (Stock Heatmap, Hotlists): son "vitrina" que abre TradingView al tocar un símbolo. No es bug nuestro, es diseño de TV.
- El widget corre dentro de un `<iframe>` cross-origin. El tap se maneja con JS del iframe que usa `window.open` / `target=_blank`. Nuestro `setOnTouchListener { true }` **no** impide eso porque el JS corre antes de que el evento nativo llegue al listener.
- Los atributos de "custom links" (`symbol-url`, evento `tv-link-open`) solo existen para los **Web Components** (`<tv-*>`), **no** para los scripts `embed-widget-*.js` que usamos. No hay config del widget que lo desactive.
- Causa concreta en el codebase: `TradingViewWebView.kt:164-178` deja pasar (retorna `false`) cualquier URL cuyo host sea `tradingview.com` → permite navegación de usuario. Y **falta** `WebChromeClient.onCreateWindow`, que es el que captura `window.open`.
- Fix recomendado (Opción A): (1) `shouldOverrideUrlLoading` devuelve `true` para todo request con `isForMainFrame == true` (bloquea navegación de usuario pero deja pasar sub-recursos del iframe); (2) agregar `onCreateWindow` retornando `false` en `WebChromeClient`; (3) `settings.setSupportMultipleWindows(true)` para que `onCreateWindow` sea invocado.
- Opción B (fallback si A no alcanza): overlay transparente en Compose sobre el `AndroidView` que consuma todos los taps. Rompe scroll interno si lo hubiera.
- Opción C (CSS `pointer-events: none` inyectado): **no funciona** — el contenido real está en un iframe cross-origin; no podés tocarlo desde el HTML padre.
- Opción D (migrar a Lightweight Charts): overkill, ya investigado; requiere data feed propio y reescribir UI del chart. No aplica para "vitrina".
- El template HTML actual (`tradingview_widget_template.html`) ya está limpio: no tiene el div `tradingview-widget-copyright` ni enlaces `<a>`. La superficie de redirect viene 100% del JS del widget.

## 🐬 Web findings

### Comportamiento de los widgets embed

- [TradingView Widget Formats](https://www.tradingview.com/widget-docs/widget-formats/): hay dos formatos — **Web Components** (`<tv-*>`, nuevos) y **iframe scripts** (`embed-widget-*.js`, legacy). Los widgets que usamos son el formato legacy.
- [Stock Heatmap Widget Docs](https://www.tradingview.com/widget-docs/widgets/heatmaps/stock-heatmap/) — no documenta parámetro que desactive la navegación al tap.
- [General FAQs](https://www.tradingview.com/widget-docs/faq/general/) — los widgets gratuitos son display-only; clicks llevan a tradingview.com por diseño.

### Custom links — solo en Web Components

- [Custom Links tutorial](https://www.tradingview.com/widget-docs/tutorials/web-components/custom-links/): permite sobreescribir URLs con `symbol-url`, `window.TradingViewCustomWidgetSettings`, o evento `tv-link-open` + `event.preventDefault()`. **Solo Web Components.**
- [Navigation customization](https://www.tradingview.com/widget-docs/tutorials/build-page/navigation/): `largeChartUrl` existe en algunos widgets (ticker tape) pero no está documentado para Heatmap ni Hotlists.

### Fix en Android WebView

- [WebView guide](https://developer.android.com/develop/ui/views/layout/webapps/webview) + [WebChromeClient](https://developer.android.com/reference/android/webkit/WebChromeClient): para `target=_blank` y `window.open` hace falta implementar `onCreateWindow`; `shouldOverrideUrlLoading` solo no alcanza.
- [ChartView.java de tradingview-mobile](https://github.com/skela/tradingview-mobile/blob/master/droid/app/src/main/java/com/spreadco/tradetradingview/ChartView.java): combina `shouldOverrideUrlLoading` + deshabilitar click/focus en el WebView para vitrina.

### Opciones de solución (web)

- **A** Bloquear navegación de usuario con `shouldOverrideUrlLoading` + `onCreateWindow` → recomendada.
- **B** Deshabilitar touch/click del WebView (overlay o flags de View) → agresiva pero efectiva.
- **C** CSS `pointer-events: none` inyectado → **no funciona cross-origin iframe**.
- **D** Migrar a Lightweight Charts (wrapper oficial Android, Apache 2.0) → requiere data feed propio.

## 🐜 Codebase findings

### Archivos relevantes

- `app/src/main/java/com/example/socketapp/ui/tradingview/TradingViewWebView.kt` — wrapper central (settings, `WebViewClient`, `WebChromeClient`, carga de HTML).
- `app/src/main/assets/tradingview/tradingview_widget_template.html` — template único con placeholders `{{CONFIG}}` y `{{SCRIPT_SRC}}`.
- `app/src/main/java/com/example/socketapp/ui/tradingview/HeatmapWidget.kt` / `HotlistsWidget.kt` — composables por widget.
- `app/src/main/AndroidManifest.xml` — sin intent-filter VIEW para http/https.

### Setup WebView actual

- Settings (líneas ~76-88): `javaScriptEnabled = true`, `domStorageEnabled = true`, file access bloqueado, `MIXED_CONTENT_NEVER_ALLOW`.
- `setOnTouchListener { _, _ -> true }` en línea ~90 — intenta consumir taps pero no frena los manejados por JS del iframe.
- `shouldOverrideUrlLoading` (líneas ~164-178): **devuelve `false` si host es `tradingview.com` o `tradingview-widget.com`** → permite la navegación. Este es el punto clave del bug.
- `WebChromeClient`: solo logging + cancelación de JS alerts/confirms/prompts. **No implementa `onCreateWindow`**.
- `settings.setSupportMultipleWindows(...)` no seteado (default `false`).
- Carga: `loadDataWithBaseURL("https://tradingview-widget.local/", html, "text/html", "UTF-8", null)` ✓.

### HTML template

Limpio, **sin** div `tradingview-widget-copyright`, sin `<a>` hardcoded:
```html
<div class="tradingview-widget-container">
  <div class="tradingview-widget-container__widget"></div>
  <script type="text/javascript" src="{{SCRIPT_SRC}}" async>
  {{CONFIG}}
  </script>
</div>
```

### Hipótesis ordenadas por probabilidad

1. **Hosts TV pasan `shouldOverrideUrlLoading` → WebView navega** (alta). El widget internamente hace `location.href = 'https://www.tradingview.com/symbols/AAPL/'` al tap y nuestro filtro lo deja pasar.
2. **`window.open` sin `onCreateWindow`** (alta). El widget puede abrir nueva ventana; sin el handler, Android delega al browser.
3. **`setOnTouchListener` no bloquea JS** (alta). El listener opera a nivel View, el JS ya procesó el tap.
4. `largeChartUrl = ""` en HotlistsConfig (media). Vacío puede hacer que TV use su default de `tradingview.com/chart/`.

## Cross-reference

### Acuerdos

- Ambos convergen: **el fix mínimo es `shouldOverrideUrlLoading` (bloquear mainframe) + `onCreateWindow` (bloquear popups)**.
- Ambos confirman que no hay parámetro del widget (`embed-widget-*.js`) para desactivar la navegación.
- Ambos descartan CSS `pointer-events: none` como fix.

### Complementos

- **Web** aporta la explicación del iframe cross-origin y por qué `setOnTouchListener` no alcanza (JS corre primero, además el tap llega al iframe cross-origin donde el padre no tiene control).
- **Codebase** aporta la línea exacta del bug: `TradingViewWebView.kt:164-178` permite hosts TV, y la ausencia de `onCreateWindow`.
- **Web** señala que `setSupportMultipleWindows(true)` es requisito para que `onCreateWindow` sea llamado.

### Conflictos

Ninguno.

### Gap analysis

El codebase tiene 1 de las 3 piezas del fix:

| Pieza | Estado |
|---|---|
| `shouldOverrideUrlLoading` que bloquee mainframe a TV | ❌ hoy deja pasar (devuelve `false` para hosts TV) |
| `WebChromeClient.onCreateWindow` retornando `false` | ❌ no existe |
| `settings.setSupportMultipleWindows(true)` | ❌ no seteado |
| `setOnTouchListener` consumiendo taps | ⚠️ presente pero insuficiente — se puede mantener o quitar |

### Aplicabilidad al proyecto

- Stack Compose + `AndroidView` + WebView ya es el correcto para aplicar Opción A sin refactor.
- El template ya está "limpio" (sin copyright hardcoded), así que no hay que tocar HTML.
- Hostname allowlist puede mantenerse para **sub-resources** (scripts, imágenes del widget): diferenciar por `request.isForMainFrame` permite dejar pasar assets y bloquear solo navegación de usuario.

## Conclusión

**Causa raíz**: los widgets `embed-widget-*.js` de TradingView son display-only y redirigen a tradingview.com al tap por diseño. El código actual permite esa navegación porque `shouldOverrideUrlLoading` devuelve `false` para cualquier URL con host TV, y no se intercepta `window.open` porque falta `WebChromeClient.onCreateWindow`.

**Fix recomendado** — Opción A (mínima, sin cambios de arquitectura, mantiene el widget renderizándose):

1. En `TradingViewWebView.kt`, `shouldOverrideUrlLoading`: devolver `true` cuando `request.isForMainFrame == true` (bloquea navegación de usuario, sin importar host). Dejar pasar sub-resources (`isForMainFrame == false`) para que el widget cargue sus assets.
2. Agregar en `WebChromeClient`:
   ```kotlin
   override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean = false
   ```
3. En el bloque de `settings`:
   ```kotlin
   settings.setSupportMultipleWindows(true)
   settings.javaScriptCanOpenWindowsAutomatically = false
   ```
4. Podemos eliminar el `setOnTouchListener { _, _ -> true }` actual (no aporta una vez hecho A, y podría interferir con eventual scroll interno del widget si lo quisiéramos más adelante).

**Opcional**: overlay transparente en Compose sobre los `AndroidView` como defense-in-depth si algún widget escapa de lo anterior (no esperado).

**Fuera de scope**: migrar a Lightweight Charts (Opción D) — mucho esfuerzo, resuelve otro problema (interactividad real), no es lo que pide este ticket.
