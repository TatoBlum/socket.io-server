# Plan — Paleta "Galicia Editorial"

_Fecha: 2026-04-18_
_Research base: `.claude/research/2026-04-18-light-theme-with-character.md`_
_Architect: 🦧 (Opus)_

## Objetivo

Aplicar la paleta **Option D Monocromo + Naranja Galicia `#E67B21`** como signature accent único para despastelizar la app, reemplazando "Soft Sky" actual que el usuario rechazó por "foggy / sin carácter".

---

## Decisiones de diseño resueltas

| Pregunta | Decisión | Rationale |
|---|---|---|
| Naming objeto `LightColors` / archivo `Theme.kt` | **Mantener genérico** (`LightColors`, `Theme.kt`, `AppTheme`). Renombrar solo los **tokens** `SoftSky*` → `Galicia*`. | No acoplar chrome del tema al branding. |
| `StatusWarning` vs naranja Galicia (hue cercano) | **Cambiar `StatusWarning` a `#CA8A04`** (amber-600). | Evita ambigüedad visual con el primary accent. |
| `SoftSkyError #B85A5A` → mapeo | **`error = #B91C1C`**, `errorContainer = #FEE2E2`, `onErrorContainer = #7F1D1D`. | Coherente con PriceDown saturado. |
| `SoftAvatarPalette` rework | **8 shades de gris neutro `#5A5A5A → #2B2B2B`** con inicial en `#E67B21` `FontWeight.Bold`. | Editorial puro, deja al naranja como único color del avatar. |
| `primaryContainer` en uso hoy | No hay consumer. Se declara en `lightColorScheme` para futuro. | Sin trabajo extra. |
| Botón "Abrir" en `ConnectionStatusBar` | **"Abrir" → `primary` naranja** (regla del accent). **"Cerrar" → `error` rojo**. Label "Conectado" → `PriceUp #16A34A` (o `PriceUpText`). "Conectando..." → `StatusWarning #CA8A04`. `Color.White` → `onPrimary`/`onError`. | Accent solo en CTAs; label de estado separado. |
| TabSelector thumb shadow | **Bajar a `1.dp`**. Fallback a `border hairline` si queda fantasma. | Mínimo cambio, preserva animación. |
| F5 tipografía | **OPCIONAL — ciclo posterior**. | Core editorial (F1-F4) entrega despastelización completa. |
| F6 TV rgba | **OPCIONAL — ciclo posterior**. | Baja prioridad visual. |

---

## Paleta final consolidada

```kotlin
// Galicia Editorial — tokens M3
GaliciaPrimary            #E67B21
GaliciaOnPrimary          #FFFFFF
GaliciaPrimaryContainer   #FBE8D4
GaliciaOnPrimaryContainer #6B3A0F
GaliciaSecondary          #0A0A0A
GaliciaOnSecondary        #FFFFFF
GaliciaSecondaryContainer #F4F4F4
GaliciaOnSecondaryContainer #0A0A0A
GaliciaTertiary           #6B6B6B
GaliciaOnTertiary         #FFFFFF
GaliciaTertiaryContainer  #EBEBEB
GaliciaOnTertiaryContainer #0A0A0A
GaliciaBackground         #FFFFFF
GaliciaOnBackground       #0A0A0A
GaliciaSurface            #FFFFFF
GaliciaOnSurface          #0A0A0A
GaliciaSurfaceVariant     #F4F4F4
GaliciaOnSurfaceVariant   #6B6B6B
GaliciaOutline            #EBEBEB
GaliciaOutlineVariant     #F0F0F0
GaliciaError              #B91C1C
GaliciaOnError            #FFFFFF
GaliciaErrorContainer     #FEE2E2
GaliciaOnErrorContainer   #7F1D1D

// Semantic — precios
PriceUp          #16A34A
PriceUpText      #15803D   (darker shade para texto)
PriceDown        #DC2626
PriceUpFlash     #D1FAE5
PriceDownFlash   #FEE2E2

// Semantic — estado
StatusWarning    #CA8A04   (amber-600)

// Chrome — mantener nombres existentes, solo cambiar hex
SegmentedTrack   #F4F4F4
CardSurface      #F4F4F4

// Avatars — 8 shades gris neutro (Editorial)
GaliciaAvatarPalette = [
    #5A5A5A, #4F4F4F, #454545, #3B3B3B,
    #5F5F5F, #4A4A4A, #3F3F3F, #353535,
]
GaliciaAvatarInitial = #E67B21
```

---

## Orden de ejecución

1 developer secuencial, 1 branch `feat/theme/galicia-editorial`, 4 commits atómicos:
1. **F1+F2** juntos (Color.kt + Theme.kt) — atómico porque consumers importan semantic tokens del mismo archivo.
2. **F3** (TradingViewScreen.kt shadow).
3. **F4** (Color.kt avatar palette + StockTickerItem.kt).
4. **ConnectionStatusBar.kt** cambios de CTAs (puede ir junto con F3 o F4).

**NO usar worktrees paralelos acá** — los 5 archivos tocados se entrelazan por imports. Un solo dev es más rápido y seguro.

---

## Developer 🐺 — Paleta Galicia Editorial completa (F1 + F2 + F3 + F4)

### Archivos a modificar
- `app/src/main/java/com/example/socketapp/ui/theme/Color.kt`
- `app/src/main/java/com/example/socketapp/ui/theme/Theme.kt`
- `app/src/main/java/com/example/socketapp/ui/tradingview/TradingViewScreen.kt`
- `app/src/main/java/com/example/socketapp/ui/stocks/ConnectionStatusBar.kt`
- `app/src/main/java/com/example/socketapp/ui/stocks/StockTickerItem.kt`

### NO tocar
- `ui/theme/Typography.kt` (reservado F5)
- `ui/tradingview/HotlistsWidget.kt` (F6)
- `ui/tradingview/HeatmapWidget.kt`, `TradingViewWebView.kt`
- `ui/SearchableTopBar.kt`, `ui/MainScreen.kt`, `ui/stocks/StocksScreen.kt`

### Fase 1 — Despastelizar core (Color.kt + Theme.kt)
- Renombrar 29 tokens `SoftSky*` → `Galicia*` en `Color.kt` con nuevos hex.
- Eliminar `SoftSkyTertiary*` pastel hues, reemplazar por grises neutros.
- **Mantener** nombres `CardSurface` y `SegmentedTrack` — solo cambiar sus hex.
- En `Theme.kt`, actualizar 20+ mapeos `SoftSky*` → `Galicia*` en el `lightColorScheme(...)`.
- **No** renombrar `private val LightColors` ni `AppTheme`.
- Mantener `isAppearanceLightStatusBars = true`.

**Aceptación F1**: la app compila, abre; background `#F7FAFC` → `#FFFFFF`; texto `#2B3A4A` → `#0A0A0A`; spinner y botón Reintentar → naranja.

### Fase 2 — Semántica saturada (Color.kt)
Mismo commit que F1:
- `PriceUp` `#4CAF7F` → `#16A34A`
- `PriceUpText` `#2E9C68` → `#15803D`
- `PriceDown` `#E88B8B` → `#DC2626`
- `PriceUpFlash` alpha → `Color(0xFFD1FAE5)` hex explícito
- `PriceDownFlash` alpha → `Color(0xFFFEE2E2)`
- `StatusWarning` `#E0A84A` → `#CA8A04`
- `SegmentedTrack` `#DDE5EF` → `#F4F4F4`
- `CardSurface` `#ECF1F6` → `#F4F4F4`

**Aceptación F2**: ticker muestra verde `#16A34A` / rojo `#DC2626` saturados; flashes sutiles; "Conectando..." en amber.

### Fase 3 — Eliminar shadow (TradingViewScreen.kt)
- Línea 150: `elevation = 4.dp` → `elevation = 0.dp`.
- Líneas 146-151: agregar `Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))` al Card. Import `androidx.compose.foundation.border`.
- Línea 267 (TabSelector thumb): `shadow(2.dp)` → `shadow(1.dp)`. Fallback: reemplazar por `.border(1.dp, outline, RoundedCornerShape(10.dp))` si visualmente queda fantasma.

**Aceptación F3**: WidgetCard sin shadow, con hairline `#EBEBEB`.

### Fase 4 — Avatares (Color.kt + StockTickerItem.kt)
- En `Color.kt`:
  ```kotlin
  val GaliciaAvatarPalette = listOf(
      Color(0xFF5A5A5A), Color(0xFF4F4F4F), Color(0xFF454545), Color(0xFF3B3B3B),
      Color(0xFF5F5F5F), Color(0xFF4A4A4A), Color(0xFF3F3F3F), Color(0xFF353535),
  )
  val GaliciaAvatarInitial = Color(0xFFE67B21)
  ```
- `StockTickerItem.kt:39`: import rename `SoftAvatarPalette` → `GaliciaAvatarPalette, GaliciaAvatarInitial`.
- `StockTickerItem.kt:42-43`: usar `GaliciaAvatarPalette`.
- `StockTickerItem.kt:82-87`: `color = Color.White` → `color = GaliciaAvatarInitial`; `FontWeight.Bold` (ya está, confirmar).
- Mantener import de `Color` si `Color.Transparent` se sigue usando en línea 59.

**Aceptación F4**: avatares grises oscuros con inicial naranja bold.

### ConnectionStatusBar.kt (mismo ciclo)
- Línea 37: `"Conectado" to PriceUpText` — mantener (o simplificar a `PriceUp`; ver Q1).
- Líneas 44-52 (botón "Cerrar"): `contentColor = Color.White` → `contentColor = MaterialTheme.colorScheme.onError`. Mantener `containerColor = errorColor`.
- Líneas 55-63 (botón "Abrir"): `containerColor = PriceUpText` → `containerColor = MaterialTheme.colorScheme.primary`; `contentColor = Color.White` → `contentColor = MaterialTheme.colorScheme.onPrimary`.
- Quitar import `Color` si ya no se usa.

**Aceptación**: botón "Abrir" naranja (CTA); "Conectado" verde oscuro; "Conectando..." amber.

### Checks post-edit
- `grep -rn "SoftSky\|SoftAvatarPalette" app/src/main` → 0 matches.
- `./gradlew assembleDebug` desde raíz del repo → build OK.
- Abrir emulador o device: verificar despastelización y CTAs en naranja.

---

## Fases opcionales (ciclo posterior)

### F5 — JetBrains Mono + displayLarge
- Bundlear `.ttf` en `res/font/`.
- `Typography.kt`: agregar `JetBrainsMonoFontFamily`; aplicar a `PriceTextStyle`/`PricePctTextStyle`; agregar `displayLarge` Inter ExtraBold `letterSpacing -0.68sp`.
- Considerar bundlear `inter_extrabold.ttf` si se usa ExtraBold.

### F6 — TradingView widget armonización
- `HotlistsWidget.kt:19-27`: `rgba(41,98,255,X)` → `rgba(230,123,33,X)`; `scaleFontColor "#5C6B7A"` → `"#6B6B6B"`.

---

## Risks and mitigations

- **Rename rompe import**: grep post-edit `SoftAvatarPalette` debe retornar 0.
- **Botón "Abrir" naranja pierde "verde=conectar"**: mantener label "Conectado" en verde para que la semántica viva en el estado, no en el CTA.
- **Thumb TabSelector invisible sobre track gris similar**: fallback a hairline border sin shadow.
- **`PriceUpText` legacy ambiguo**: comentar diferencia con `PriceUp`, o eliminarlo (ver Q1).
- **`PriceUpFlash` alpha vs hex**: el cambio puede verse distinto. Fallback: `PriceUp.copy(alpha = 0.12f)`.
- **`CardSurface`/`SegmentedTrack` rename rompe imports**: NO renombrar, solo cambiar hex.
- **Import `border` faltante**: agregar `androidx.compose.foundation.border` al tope de TradingViewScreen.kt.
- **`FontWeight.ExtraBold` sin .ttf bundleado**: usar `Bold` en el inicial (fallback sintético de ExtraBold se ve feo).
- **Worktrees paralelos acá no ayudan** (lección 2026-04-13): los 5 archivos entrelazan imports. 1 dev secuencial.

---

## Questions / flags al usuario

1. ¿Eliminar `PriceUpText` y usar `PriceUp #16A34A` directo? Default: mantener con `#15803D` + comentario.
2. ¿Label "Conectando..." en amber `#CA8A04` (decisión del plan) o en naranja Galicia `#E67B21`? Default: amber.
3. ¿Avatares en 8 grises (recomendado) o variante "todos `#F4F4F4` con inicial naranja"? Default: 8 grises.
4. ¿F5 tipografía (JetBrains Mono) entra en este ciclo o se posterga? Default: posterga (+150KB APK).
5. ¿F6 TV rgba entra o se mantiene azul TV nativo? Default: posterga.
