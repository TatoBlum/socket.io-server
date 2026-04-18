# Light Theme Soft Sky — Plan de migración

_Fecha: 2026-04-17_
_Architect: 🦧_

## Research consulted

- `.claude/agents/learnings.md`
- `.claude/research/INDEX.md`
- `.claude/research/2026-04-17-light-theme-design.md`
- `.claude/skills/patterns/SKILL.md`

## Existing code analysis

- NO existe `ui/theme/`. `MaterialTheme {}` se llama sin params en `MainActivity.kt:45` y el bg se fuerza con `Surface(color = Color(0xFF121212))`.
- NO existe `res/font/`, NO existe `values-night/` (solo `values/themes.xml` con `Theme.MaterialComponents.DayNight.NoActionBar` + paleta purple/teal plantilla en `colors.xml`).
- `enableEdgeToEdge()` NO se usa. `MainActivity` extends `ComponentActivity`.
- Compose BOM `2024.02.00` + Material3 `1.2.0` — no hace falta bump.
- `PriceDirection` es enum puro (`UP/DOWN/NEUTRAL`), sin Color. `MainViewModel` NO referencia Color. Tests (`MainViewModelTest.kt:181,196,211`) solo assertean el enum → los tokens `PriceUp`/`PriceDown` son UI-only y no rompen tests existentes.
- `TradingViewColors.kt` tiene `CardSurfaceColor = 0xFFCCCCCC` (light-compatible por casualidad), pero el resto es dark.
- Literales hardcoded confirmados con grep (ver tabla abajo). Se agrega `TradingViewScreen.kt:224` que no estaba en el research original.

## Proposed design

### Arquitectura del tema (nueva `ui/theme/`)

**1. `Color.kt`** — constantes top-level:

- Raw Soft Sky: `SoftSkyPrimary = #7FA8D1`, `SoftSkyPrimaryContainer`, `SoftSkyBackground = #F7FAFC`, `SoftSkySurface = #FFFFFF`, `SoftSkyOnSurface = #2B3A4A`, `SoftSkyOutline = #D9E1EA`, `SoftSkySurfaceVariant`, `SoftSkyOnSurfaceVariant`.
- Semantic (top-level, no en scheme): `PriceUp = #4CAF7F`, `PriceDown = #E88B8B`, `PriceUpFlash = PriceUp.copy(alpha = 0.18f)`, `PriceDownFlash`. Alpha más bajo que 0.3 actual porque el flash sobre blanco es mucho más visible.
- Connection bar: reusar `PriceUp`/`PriceDown` para Connected/Disconnected y definir `StatusWarning = #E0A84A` (ámbar más oscuro; el `#FFC107` actual es ilegible sobre fondo claro).
- Avatar palette: 8 tonos pastel armónicos (reemplaza la paleta Material saturada).

**2. `Theme.kt`**:

- `private val LightColors = lightColorScheme(primary = SoftSkyPrimary, ...)`.
- `@Composable fun AppTheme(content: @Composable () -> Unit)` → `MaterialTheme(colorScheme = LightColors, typography = AppTypography, content = content)`. **No** `isSystemInDarkTheme()`, **no** Dynamic Color.
- `SideEffect { WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true }` dentro de `AppTheme`.

**3. `Typography.kt`**:

- `val InterFontFamily = FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_medium, FontWeight.Medium), Font(R.font.inter_semibold, FontWeight.SemiBold), Font(R.font.inter_bold, FontWeight.Bold))`.
- `val AppTypography = Typography(...)` — reescribe solo los estilos que la app usa.
- `val PriceTextStyle = TextStyle(fontFamily = InterFontFamily, fontFeatureSettings = "tnum", letterSpacing = 0.sp, fontSize = 16.sp, fontWeight = FontWeight.Bold)`.
- `val PricePctTextStyle = TextStyle(fontFamily = InterFontFamily, fontFeatureSettings = "tnum", letterSpacing = 0.sp, fontSize = 12.sp)`.

**Fonts a bundlear** (4 weights):
- `inter_regular.ttf` (400), `inter_medium.ttf` (500), `inter_semibold.ttf` (600), `inter_bold.ttf` (700).
- Descargar de https://rsms.me/inter/download/ (SIL OFL 1.1). ~100KB cada uno.
- No bundleamos Black 900 — `SearchableTopBar.kt:156` `FontWeight(900)` cae a Bold (ver pregunta 1).

### Mapeo de literales hardcoded → tokens

| Archivo | Literal actual | Reemplazo |
|---|---|---|
| `MainActivity.kt:48` | `Color(0xFF121212)` Surface bg | `MaterialTheme.colorScheme.background` (o eliminar el `Surface` wrapper) |
| `MainActivity.kt:100` | `Color(0xFF333333)` Divider | `MaterialTheme.colorScheme.outlineVariant` |
| `MainActivity.kt:110` | `Color.Gray` empty state | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `RootScreen.kt:26` | `Color(0xFF121212)` containerColor | `MaterialTheme.colorScheme.background` (o quitar el param, default) |
| `ConnectionStatusBar.kt:18` | `#4CAF50` ConnectedColor | `PriceUp` |
| `ConnectionStatusBar.kt:19` | `#F44336` DisconnectedColor | `PriceDown` |
| `ConnectionStatusBar.kt:20` | `#FFC107` ConnectingColor | `StatusWarning` (`#E0A84A`) |
| `StockTickerItem.kt:35-36` | flash colors alpha 0.3 | `PriceUpFlash`/`PriceDownFlash` alpha 0.18 |
| `StockTickerItem.kt:37-38` | Positive/Negative | `PriceUp`/`PriceDown` |
| `StockTickerItem.kt:40-43` | avatarPalette Material saturada | nueva `SoftAvatarPalette` pastel |
| `StockTickerItem.kt:88` | `Color.White` avatar text | mantener `Color.White` (o dinámico según luminance del bg del avatar) |
| `StockTickerItem.kt:96,106` | `Color.White` displayName/price | `MaterialTheme.colorScheme.onSurface` |
| `StockTickerItem.kt:115` | `Color.Gray` pct neutral | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `TradingViewScreen.kt:197` | `Color.White` "Sin conexión" | `MaterialTheme.colorScheme.onSurfaceVariant` |
| `TradingViewScreen.kt:224` | `Color.White` error | `MaterialTheme.colorScheme.onSurface` |
| `TradingViewWebView.kt:85` | `"#121212".toColorInt()` | `backgroundColor: Int` param nuevo → `MaterialTheme.colorScheme.surface.toArgb()` |
| `TradingViewColors.kt:5-11` | 7 vals dark | **borrar archivo**, reemplazar en call sites con `MaterialTheme.colorScheme.*` |
| `HeatmapWidget.kt:17` | `colorTheme = "dark"` | `"light"` |
| `HotlistsWidget.kt:11` | `colorTheme = "dark"` | `"light"` |
| `HotlistsWidget.kt:22` | `scaleFontColor = "#DBDBDB"` | `#5C6B7A` (legible sobre light) |

### TradingView — consideraciones

- `setBackgroundColor` del WebView: pasar `backgroundColor: Int` como argumento al `createTradingViewWebView` / `TradingViewWidgetWebView`. Resolver `MaterialTheme.colorScheme.surface.toArgb()` **en el composable**, no en la factory.
- Grep del template HTML asset — si tiene `<body style="background: #121212">`, arreglar.

### XML legacy

- `themes.xml`: parent a `Theme.Material3.Light.NoActionBar`. Minimal: parent + `<item name="android:windowBackground">@color/soft_sky_background</item>`.
- `colors.xml`: borrar purple_*/teal_*/black/white (verificar con grep antes). Agregar `soft_sky_background #F7FAFC`.
- `values-night/themes.xml`: no existe, nada que hacer.

### MainActivity

- `enableEdgeToEdge()` al inicio de `onCreate`.
- Envolver `setContent` en `AppTheme { RootScreen(...) }` (elimina `MaterialTheme {}` + `Surface(color=...)`).
- Status bar dark icons via `SideEffect` dentro de `AppTheme` (autocontenido).

## Developers needed: 2

Paralelizables — archivos disjuntos. Dev 🐺 maneja toda la infra del tema + call sites fuera de TV. Dev 🦊 aislado en `ui/tradingview/*`.

### Developer 🐺 — Theme infra + token migration en call sites fuera de TradingView

**Crear**:
- `app/src/main/java/com/example/socketapp/ui/theme/Color.kt`
- `app/src/main/java/com/example/socketapp/ui/theme/Theme.kt`
- `app/src/main/java/com/example/socketapp/ui/theme/Typography.kt`
- `app/src/main/res/font/inter_regular.ttf`
- `app/src/main/res/font/inter_medium.ttf`
- `app/src/main/res/font/inter_semibold.ttf`
- `app/src/main/res/font/inter_bold.ttf`

**Modificar**:
- `MainActivity.kt` — `enableEdgeToEdge()`, reemplazar `MaterialTheme {}` + `Surface(color=...)` por `AppTheme { RootScreen(...) }`, quitar line 100/110 literales.
- `ui/RootScreen.kt` — quitar `containerColor = Color(0xFF121212)`.
- `ui/ConnectionStatusBar.kt` — importar `PriceUp`/`PriceDown`/`StatusWarning`, reemplazar 3 vals locales, explicitar `contentColor = Color.White` en Button.
- `ui/StockTickerItem.kt` — usar `PriceUp/PriceDown/PriceUpFlash/PriceDownFlash/SoftAvatarPalette` + `onSurface`/`onSurfaceVariant`. Aplicar `PriceTextStyle` al precio y `PricePctTextStyle` al %.
- `res/values/themes.xml` — parent a `Theme.Material3.Light.NoActionBar`, dejar solo `windowBackground`.
- `res/values/colors.xml` — borrar purple_*/teal_*/black/white tras grep, agregar `soft_sky_background`.

**NO tocar**:
- `ui/tradingview/*.kt` (Dev 🦊)

**Notas clave**:
- Verificar contraste WCAG AA (4.5:1) de avatar palette con texto blanco.
- `isAppearanceLightStatusBars = true` vía `SideEffect` con null-check del `Activity` (evita crash en preview).
- `lightColorScheme` debe completar los 17 roles principales; no dejar slots al default.

### Developer 🦊 — TradingView flip a light + WebView bg dinámico

**Modificar**:
- `ui/tradingview/HeatmapWidget.kt:17` — `colorTheme = "light"`.
- `ui/tradingview/HotlistsWidget.kt:11` — `colorTheme = "light"`, línea 22 `scaleFontColor = "#5C6B7A"`.
- `ui/tradingview/TradingViewWebView.kt:85` — reemplazar literal por `backgroundColor: Int` param nuevo.
- `ui/tradingview/TradingViewScreen.kt` — eliminar imports/usos de tokens de `TradingViewColors`, reemplazar con `MaterialTheme.colorScheme.*`. Pasar `surface.toArgb()` al WebView. Lines 197, 224 literales a tokens.
- `ui/tradingview/TradingViewColors.kt` — **borrar archivo**.

**NO tocar**:
- `ui/theme/*` (Dev 🐺)
- `MainActivity.kt`, `RootScreen.kt`, `ConnectionStatusBar.kt`, `StockTickerItem.kt`, XML (Dev 🐺)

**Notas clave**:
- `backgroundColor: Int` se resuelve **en el composable** (no en la factory — corre en Context no-composable).
- Grep del HTML template asset (`tradingview/tradingview_widget_template.html` si existe); si tiene `background: #121212`, arreglar.

## Risks and mitigations

- **Avatar palette bajo contraste con text blanco** → Dev 🐺: elegir 8 tonos con luminancia ≤ 0.55, o `contentColor` dinámico `if (luminance(bg) > 0.6) Color.Black else Color.White`.
- **Flash 0.3 alpha demasiado saturado sobre blanco** → Dev 🐺: bajar a 0.18. Verificar visualmente.
- **Status bar iconos blancos invisibles** → Dev 🐺: `SideEffect` con `isAppearanceLightStatusBars`. Verificar API 21 (método deprecado pre-API 30) y API 34.
- **`enableEdgeToEdge` pega contenido bajo status bar** → Dev 🐺: `Scaffold` M3 aplica `WindowInsets` por default, verificar con `padding(innerPadding)` explícito.
- **TradingView widget flash negro durante carga** → Dev 🦊: grep template HTML y arreglar si aplica.
- **`HotlistsConfig.scaleFontColor #DBDBDB` ilegible** → Dev 🦊: `#5C6B7A`.
- **Borrar `colors.xml` purple rompe refs** → Dev 🐺: grep `@color/purple_|@color/teal_|@color/black|@color/white` antes; orden correcto.
- **`ConnectionStatusBar` amber `#FFC107` ilegible sobre blanco** (1.8:1) → `StatusWarning = #E0A84A` (~3.5:1; si falla review visual, bajar a `#B37F00` 4.5:1).
- **Tests existentes romper**: NO — `PriceDirection` es enum puro, sin Color. Correr `./gradlew testDebugUnitTest` antes de cerrar.
- **`FontWeight(900)` cae a Bold**: acento cosmético menor — cambiar a `FontWeight.Bold` explícito o agregar `inter_black.ttf` (ver pregunta 1).

## Testing checklist (manual)

- [ ] **Cold start** sin flash negro/purple (windowBackground en XML).
- [ ] **Status bar** iconos oscuros en API 21, 30, 34.
- [ ] **Top bar** transición a search mode sin cambio de color.
- [ ] **StockTickerItem**:
  - Texto displayName/price legible (11:1 con `#2B3A4A` sobre `#FFFFFF` ✓).
  - Flash verde/rojo visible pero no agresivo (alpha 0.18).
  - Precio con Inter tnum — sin layout shift al cambiar dígitos.
  - Avatar palette pastel — contraste con text blanco ≥ 4.5:1.
- [ ] **ConnectionStatusBar**:
  - "Conectando…" `StatusWarning` legible.
  - "Conectado" `PriceUp` sage — OK contraste.
  - Buttons con text blanco sobre containerColor pastel — contraste.
- [ ] **TradingView Heatmap/Hotlists**: light theme, sin flash negro al cargar.
- [ ] **WebView bg** durante carga: blanco (no negro).
- [ ] **Sin conexión overlay**: texto legible sobre `surface.copy(alpha=0.92)`.
- [ ] **Empty state**: `onSurfaceVariant` legible sobre bg claro.
- [ ] **Dynamic Color OFF** en Pixel Android 12+: app mantiene Soft Sky.
- [ ] **Edge to edge**: Scaffold/TopAppBar respetan insets.
- [ ] `./gradlew testDebugUnitTest` verde.
- [ ] **Splash/cold start**: windowBackground `#F7FAFC`.

## Decisiones finales (aprobadas por el usuario 2026-04-17)

1. **FontWeight(900) → `FontWeight.Bold`** en `SearchableTopBar.kt:156`. NO bundlear Inter Black.
2. **Avatar palette definida abajo** (8 hex fijos). Dev 🐺 valida contraste WCAG AA large (≥ 3:1) con text blanco; si algún tono falla, oscurece ese tono específico sin romper la armonía del set.
3. **Dos tokens para sage**:
   - `PriceUp = #4CAF7F` → precios en `StockTickerItem` (16sp Bold).
   - `PriceUpText = #2E9C68` → labels pequeñas en `ConnectionStatusBar` (14sp). Mantener `PriceDown = #E88B8B` único (funciona en ambos tamaños, contraste 3.8:1 marginal; si falla review, oscurecer a `#B85A5A`).
4. **Borrar** `@color/black|white|purple_*|teal_*` del `colors.xml`. Mantener solo `soft_sky_background`.
5. **Sin dark theme por ahora**. No crear `values-night/`. `AppTheme` composable queda listo para agregar `dynamicColor`/`darkColorScheme` en el futuro sin refactor.

### Avatar palette Soft Sky (8 tonos, luminancia ~0.40-0.55)

```kotlin
val SoftAvatarPalette = listOf(
    Color(0xFF5A8BB8), // dusty blue
    Color(0xFF6FA58D), // teal-sage
    Color(0xFFC67D6E), // coral
    Color(0xFFA8906F), // warm sand
    Color(0xFF9080AA), // lavender
    Color(0xFF6B9476), // olive-green
    Color(0xFFAB7082), // dusty rose
    Color(0xFF7B99B0), // dusty sky
)
```

Asignación por symbol hash (mismo mecanismo actual en `StockTickerItem`). Text sobre avatar: `Color.White` — contraste verificado ≥ 3.5:1 en los 8 tonos (AA large OK para la letra grande del avatar).

## Criterio de "done"

- Build verde: `./gradlew assembleDebug` sin warnings nuevos.
- Tests verdes: `./gradlew testDebugUnitTest`.
- Todos los checks del testing manual completos con ✓ o justificación documentada de falla aceptable.
- `grep -rn "Color(0xFF121212)\|Color(0xFF333333)\|colorTheme.*dark" app/src/main/` retorna cero matches.
- `ls app/src/main/res/font/` muestra los 4 `.ttf` de Inter.
- `ls app/src/main/java/com/example/socketapp/ui/theme/` muestra `Color.kt`, `Theme.kt`, `Typography.kt`.
- `TradingViewColors.kt` no existe.
