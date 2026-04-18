# Light Theme Design — SocketAndroidPOC

_Fecha: 2026-04-17_

## TL;DR (max 10 bullets — para inyección en prompts)

- La app NO tiene `Theme.kt` ni `Color.kt`: usa `MaterialTheme {}` sin params + colores dark hardcoded en cada Composable. Migrar requiere crear tokens centralizados primero.
- XML legacy (`values/themes.xml`) todavía apunta a `Theme.MaterialComponents.DayNight.NoActionBar` (Material 2). Compose lo sobreescribe, pero el `android:statusBarColor` = purple queda latente — limpiar cuando se toque.
- Tendencia 2025 fintech light: background near-white `#FAFAFA`/`#F8F9FA` (nunca blanco puro), verde SOLO para up/down (no brand).
- **Decisión del usuario (2026-04-17): paleta "Soft Sky"** — azul pastel bancario, todo en tonos claros. Primary `#7FA8D1`, background `#F7FAFC`, surface `#FFFFFF`, onSurface `#2B3A4A`. Semantic `PriceUp=#4CAF7F` (sage) / `PriceDown=#E88B8B` (dusty coral).
- Rationale: azul = confianza financiera (Chase, BBVA, Santander, Revolut). Pastel mantiene airy sin perder seriedad bancaria. Descartadas "Warm Sand" (wellness/editorial) y "Mint Breeze" (lifestyle).
- Font: **Inter bundleada en `res/font/`** (no downloadable) + `fontFeatureSettings = "tnum"` en estilo de precios → evita layout shift al tickear.
- 13 puntos con colores hardcoded que rompen light: `MainActivity.kt:48,100,112`, `StockTickerItem.kt:35-43,89,95,103,116`, `TradingViewColors.kt:5-12`, `TradingViewWebView.kt:85`, `RootScreen.kt:26`, `ConnectionStatusBar.kt:18-20`.
- TradingView widgets: flip `HeatmapConfig.colorTheme` y `HotlistsConfig.colorTheme` de `"dark"` → `"light"` + `setBackgroundColor` del WebView dinámico.
- Deshabilitar Dynamic Color (Android 12+): para fintech conviene paleta fija de marca, no `dynamicLightColorScheme`.
- Compose BOM `2024.02.00` → Material3 `1.2.0` soporta `lightColorScheme()` con todos los tokens necesarios sin upgrade.
- Plan mínimo de migración: `ui/theme/{Color,Theme,Typography}.kt` + reemplazar literales por tokens + flip TV widgets + `enableEdgeToEdge()` con contraste de status bar.

---

## 🐬 Web findings

### Referencias de apps (para mood)
- **Robinhood**: background blanco puro, primary near-black, verde `#00C805` solo para ganancias (no brand).
- **Revolut**: navy/azul profundo como primary, superficies off-white.
- **Wealthsimple**: warm-white (`#F9F7F4`) editorial, accents teal sofisticados.
- **Apple Stocks / Bloomberg**: minimalismo monocromático, tipografía densa con tabular figures.

### 3 Paletas listas para Material 3 (copiar a `Theme.kt`)

**Paleta 1 — "Neutral Black"** (Robinhood / Apple Stocks)
```kotlin
val LightColorScheme = lightColorScheme(
    primary            = Color(0xFF1A1A1A),
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFE8E8E8),
    onPrimaryContainer = Color(0xFF1A1A1A),
    secondary          = Color(0xFF5C5C5C),
    onSecondary        = Color(0xFFFFFFFF),
    background         = Color(0xFFFAFAFA),
    onBackground       = Color(0xFF1A1A1A),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF1A1A1A),
    surfaceVariant     = Color(0xFFF2F2F2),
    onSurfaceVariant   = Color(0xFF5C5C5C),
    outline            = Color(0xFFD4D4D4),
)
// Semantic (constantes aparte):
val PriceUp   = Color(0xFF16A34A) // green-700 — WCAG AA
val PriceDown = Color(0xFFDC2626) // red-600
```

**Paleta 2 — "Deep Blue"** (Revolut / Bloomberg)
```kotlin
primary            = Color(0xFF1E3A5F), // navy
background         = Color(0xFFF8F9FA),
surface            = Color(0xFFFFFFFF),
surfaceVariant     = Color(0xFFEBF0F7),
outline            = Color(0xFFCDD8E5),
// PriceUp = #15803D, PriceDown = #B91C1C
```

**Paleta 3 — "Warm Slate"** (Wealthsimple / Cash App)
```kotlin
primary            = Color(0xFF2D6A4F), // dark teal-green
background         = Color(0xFFF9F7F4), // warm white
surface            = Color(0xFFFFFFFF),
surfaceVariant     = Color(0xFFF0EDE8),
// PriceUp = #166534 (más oscuro para contraste sobre warm-white)
```

### Typography

- **Inter** (rsms.me/inter) — tabular figures nativo, 100-900 weights, diseñada para UI data-dense.
- **Manrope** — alternativa geométrica, mejor para headings, tnum menos robusto.
- **Bundlear en `res/font/`** (no Google Fonts downloadable) para habilitar `fontFeatureSettings = "tnum"`.

```kotlin
val PriceTextStyle = TextStyle(
    fontFamily = InterFontFamily,
    fontFeatureSettings = "tnum",
    letterSpacing = 0.sp
)
```

### TradingView widgets light
- Parámetro `colorTheme: "light"` en embed script (JSON config).
- Producto: fondo `#F8F8F8`/`#FFFFFF`, chart lines azul/verde — armoniza con Paletas 1 y 2; Paleta 3 (warm) puede tener leve contraste de temperatura en el borde.

### Fuentes
- [Material 3 Color roles](https://m3.material.io/styles/color/roles)
- [Material Theme Builder](https://m3.material.io/theme-builder)
- [Compose fonts](https://developer.android.com/develop/ui/compose/text/fonts)
- [TradingView widget theming](https://www.tradingview.com/widget-docs/tutorials/web-components/styling-and-themes/)
- [Inter](https://rsms.me/inter/)

---

## 🐜 Codebase findings

### 1. Theme actual
- **No existe** `ui/theme/Theme.kt`, `Color.kt`, `Typography.kt`.
- `res/values/themes.xml:3` → `Theme.MaterialComponents.DayNight.NoActionBar` (Material 2 legacy).
- `res/values-night/themes.xml:3` → mismo pattern.
- `res/values/colors.xml` → solo 7 colores plantilla AS (`purple_*`, `teal_*`, `black`, `white`).
- `MainActivity.kt:45` → `MaterialTheme { Surface(color = Color(0xFF121212)) { ... } }` — ignora tema del sistema, hardcoded dark.
- `AndroidManifest.xml:13` → `android:theme="@style/Theme.SocketApp"` (sin efecto real en Compose).
- `enableEdgeToEdge` NO se usa.
- Compose BOM `2024.02.00` / Material3 `1.2.0` / compileSdk 34 / Kotlin 1.9.22.

### 2. Colores hardcoded (13 puntos a tocar)
| Archivo | Línea | Valor |
|---|---|---|
| `MainActivity.kt` | 48 | `Color(0xFF121212)` Surface bg |
| `MainActivity.kt` | 100 | `Color(0xFF333333)` Divider |
| `MainActivity.kt` | 112 | `Color.Gray` texto empty |
| `ConnectionStatusBar.kt` | 18-20 | `#4CAF50` `#F44336` `#FFC107` |
| `StockTickerItem.kt` | 35-43 | flash + positive/negative + avatar palette |
| `StockTickerItem.kt` | 89,95,103 | `Color.White` × 3 |
| `StockTickerItem.kt` | 116 | `Color.Gray` |
| `TradingViewColors.kt` | 5-12 | todos dark |
| `TradingViewScreen.kt` | 198 | `Color.White` |
| `TradingViewWebView.kt` | 85 | `setBackgroundColor("#121212")` |
| `RootScreen.kt` | 26 | `containerColor = Color(0xFF121212)` |
| `SearchableTopBar.kt` | 81 | `MaterialTheme.colorScheme.background` ✅ (único correcto) |

### 3. Fonts
- `res/font/` NO existe.
- No hay `FontFamily` custom ni Google Fonts provider.
- Typography inline en cada Composable (`TextStyle(fontSize, fontWeight)`).

### 4. Componentes críticos al flip
- **StockTickerItem**: texto `Color.White` → ilegible en light. Reemplazar por `MaterialTheme.colorScheme.onSurface`.
- **TradingViewScreen/TradingViewColors**: paleta dark fija; el `CardSurfaceColor = 0xFFCCCCCC` es casualmente light-compatible.
- **TradingViewWebView.kt:85**: `setBackgroundColor("#121212")` hardcoded — flash negro durante carga.
- **HeatmapConfig / HotlistsConfig**: `colorTheme = "dark"` hardcoded en `HeatmapWidget.kt:17` y `HotlistsWidget.kt:11`.
- **RootScreen.kt:26**: `Scaffold(containerColor = Color(0xFF121212))` — bg global siempre dark.
- **System bars**: sin `enableEdgeToEdge`; status bar queda en el default del XML (purple).

---

## Cross-reference

### Acuerdos
- Stack ya está listo para `lightColorScheme` (Material3 1.2.0). No hay blocker técnico.
- Semantic colors up/down van como constantes fuera del `ColorScheme` (ambas fuentes).
- TradingView soporta nativo `colorTheme: "light"` — 🐜 identificó los 2 archivos exactos a tocar.

### Complementos
- 🐬: 3 paletas hex concretas + rationale + fonts candidates + snippets.
- 🐜: lista exhaustiva de 13 puntos a tocar + gap real (no hay Theme.kt).

### Conflictos
- Ninguno significativo.
- Duda menor: `fontFeatureSettings = "tnum"` en `TextStyle` requiere font bundleada en `res/font/`. Si se usa `GoogleFont` downloadable, el feature no se aplica — confirmar al implementar.

### Gap analysis
- **Hoy**: dark hardcoded × 13 lugares + Material 2 XML legacy + sin Typography centralizada + sin `enableEdgeToEdge`.
- **Objetivo**: `ui/theme/{Color,Theme,Typography}.kt` + tokens semánticos + Inter bundleada + TV widgets light + status bar compatible.
- **Esfuerzo**: medio. ~5-7 archivos modificados + 3 nuevos + font asset.

### Aplicabilidad al proyecto
1. **Paleta recomendada: 1 "Neutral Black"** — mejor fit para app data-dense; combina con Merval/crypto tickers sin dominar.
2. **Font: Inter bundleada** (no downloadable) para tnum en `StockTickerItem` precio.
3. **TradingView**: flip a `light` es 2-líneas (defaults de los data classes) + WebView bg dinámico.
4. **Deshabilitar Dynamic Color** (`dynamicColor = false`) para mantener consistencia de marca.
5. **XML legacy**: cuando se toque, cambiar `Theme.MaterialComponents` → `Theme.Material3.Light.NoActionBar` o un `@style` neutro que no pinte status bar purple.

---

## Conclusión

Adoptar **Paleta 1 (Neutral Black)** + **Inter bundleada** con `"tnum"` para precios. Crear `ui/theme/{Color,Theme,Typography}.kt` y reemplazar los 13 literales hardcoded por tokens M3. Flip `colorTheme` de los dos widgets TradingView a `"light"` y hacer dinámico el `setBackgroundColor` del WebView. Agregar `enableEdgeToEdge()` en `MainActivity.onCreate`. Deshabilitar Dynamic Color. El refactor es medio-chico (~7 archivos) y deja la base para agregar dark opcional más tarde con `isSystemInDarkTheme()`.
