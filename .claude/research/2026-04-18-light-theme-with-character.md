# Light Theme con Carácter (pivot de pastel → editorial)

_Fecha: 2026-04-18_
_Context: follow-up de `2026-04-17-light-theme-design.md`. La paleta "Soft Sky" pastel fue aplicada y el usuario la rechaza por "foggy / sin carácter". Misma restricción (light backgrounds) pero busca más personalidad con sutileza._

## TL;DR (max 10 bullets — para inyección en prompts)

- Patrón 2025-2026 en light mode con carácter: **fondo blanco puro (#FFFFFF)** + **near-black genuino (#0F0F0F, no azulado)** + **1 solo accent saturado** + hairlines sutiles, sin shadows blandas.
- Referencias: Robinhood (Robin Neon #CCFF00), Wise (forest green #163300 + lima #9FE870), Linear (monocromo + indigo puntual), Monzo (hot coral + navy), Mercury/Stripe (blue-600 #2563EB).
- Lo que rompe el "foggy": fondos teñidos (#F0F4F8), texto azulado-desaturado (#7A8B9C), borders muy lavados, shadows con spread ≥4dp, múltiples accents compitiendo.
- **Recomendación ganadora** para este proyecto: **Option A editorial + accent `#2563EB` blue-600** (blanco puro, near-black, blue-600 como firma solo en focus/estado/CTA, verde #16A34A y rojo #DC2626 solo en precios).
- Tipografía: **Inter ya bundleada sirve** (alineado con web research); agregar **JetBrains Mono** (Apache 2.0) para números de precio da carácter técnico-editorial instantáneo sin tocar colores.
- Técnicas de carácter sin color: contraste extremo de pesos (Bold 800 display / Regular body), `letterSpacing -0.02em` en ≥20sp, hairlines 1dp en `#E5E5E5`, elevation por `surfaceContainer*` (sin shadow ni tint).
- **Codebase gap**: el `primary` actual tiene presencia BAJA (solo CircularProgressIndicator + botón Reintentar). Swap de primary es **low-risk**. Chrome dominante usa `background`/`surface`/`CardSurface` — esos sí hay que despastelizar.
- Cambios por impacto: **ALTO** = 1 archivo (`ui/theme/Color.kt`) reemplaza tokens M3 y propaga al 95% de UI. **MEDIO** = 4 tokens extra-tema (`CardSurface`, `SegmentedTrack`, `PriceUp/Down*`, `SoftAvatarPalette` x8). **BAJO** = 3 `Color.White` hardcodeados + rgba del HotlistsWidget.
- `TradingView widgets`: `colorTheme = "light"` hardcodeado en configs; el bg se propaga vía `MaterialTheme.colorScheme.surface.toArgb()` — sin cambios extra. Colores de línea TV (`rgba(41,98,255,1)`) son independientes del theme; se pueden armonizar con el nuevo accent blue si se quiere.
- M3 setup ya correcto: Inter custom bundleada, `fontFeatureSettings="tnum"` activo en `PriceTextStyle`, `dynamicColor` no usado.

---

## 🐬 Web findings

### Apps de referencia — cómo logran carácter en light

- **Robinhood (2024 rebrand)**: blanco puro + neutrales maduros + 1 accent neon (`#CCFF00` Robin Neon). Firma = restricción total en chrome + 1 color vivo.
- **Wise**: `#FFFFFF` puro, verde bosque `#163300` como primary interactivo, `#9FE870` como accent puntual. "White is the most prominent color in our UI."
- **Monzo**: hot coral + navy + blanco. Carácter por tipografía (Oldschool Grotesk) + color de marca en slot único.
- **Linear (2025 refresh)**: monocromo black/white puro + 1 indigo muy puntual. "Si la mayoría no nota qué cambió, buena señal."
- **Raycast**: rojo saturado `#FF6363` único contra blanco puro.
- **Nubank**: custom typeface (Nu Sans) + purple único. Carácter 100% tipográfico + 1 color.

### Paletas concretas

**Option A — High-contrast Editorial** (Robinhood/Linear)
```
background:        #FFFFFF
surface:           #FFFFFF
surfaceContainer:  #F5F5F5    (gris neutro, cero tinte)
onSurface:         #0F0F0F    (near-black, no azulado)
onSurfaceVariant:  #525252
outline:           #E5E5E5    (hairline)
primary:           #1A1A1A    (negro CTA)
[accent opcional]: #CCFF00, #2563EB o #F97316 en max 2-3 puntos
PriceUp:           #16A34A
PriceDown:         #DC2626
```

**Option B — Warm Off-White** (Wise)
```
background:        #FAFAF8    (ivory sutil cálido)
surface:           #FFFFFF
surfaceContainer:  #F0EFE9
onSurface:         #111110
onSurfaceVariant:  #4A4A46
outline:           #E2E1DA
primary:           #163300    (forest green)
accent:            #9FE870    (lima puntual)
PriceUp:           #15803D
PriceDown:         #B91C1C
```

**Option C — Cold Neutral + Bold Accent** (Stripe/Mercury)
```
background:        #F9FAFB    (gray-50)
surface:           #FFFFFF
surfaceContainer:  #F3F4F6
onSurface:         #111827    (gray-900)
onSurfaceVariant:  #6B7280
outline:           #E5E7EB
primary:           #2563EB    (blue-600 eléctrico)
PriceUp:           #059669
PriceDown:         #DC2626
```

**Option D — Monocromo + Accent Único** (hyper-restrained)
```
background:        #FFFFFF
surface:           #FFFFFF
surfaceContainer:  #F4F4F4
onSurface:         #0A0A0A
primary:           #0A0A0A
[1 signature]:     #00C805 / #F97316 / #7C3AED
```

### Tipografía

| Rol | Font | Razón |
|---|---|---|
| Body / Labels | **Inter** (ya bundleada) | tnum nativa, x-height alta, demostrada en datos |
| Números precio | **JetBrains Mono** (Apache 2.0) | Tabular nativo + carácter técnico |
| Display | **Inter ExtraBold** + `letterSpacing -0.5.sp` | Peso extremo = carácter sin cambiar family |
| Alternativa warm | DM Sans | Terminaciones redondeadas |

Técnicas:
- Weight 800 headers + 400 body → contraste es el carácter.
- `letterSpacing -0.02em` en ≥20sp (editorial).
- `letterSpacing 0.08em` en caps/labels (legible).
- JetBrains Mono para TODO valor numérico: prices, %, market cap.

### Sutileza con carácter

1. Hairlines 1dp `#E5E5E5` para separar cards/rows. NO shadow.
2. Color solo en data, nunca en chrome.
3. Near-black genuino (`#0F0F0F`), no `#1A2B3C` azulado.
4. Elevation por `surfaceContainerLow/Container/ContainerHigh` tonal puro.
5. Qué rompe el foggy: bg `#F0F4F8` teñido, texto `#7A8B9C` desaturado, shadows spread ≥4dp.

### M3 en light sin sentirse genérico

- `dynamicColor = false`.
- `surfaceTintColor` deprecado desde M3 1.2 (ya no hace falta).
- `surfaceContainer*` tokens para jerarquía sin shadow.
- Referencia Android reciente con carácter M3 light: **Google Wallet**.

### Recomendación (🐬)

**Option A + accent `#2563EB` blue-600** para app stocks Alpaca:
- `#FFFFFF` fondo + `#111111` texto — máximo contraste, cero fog.
- `#2563EB` firma SOLO en status bar, search focus ring, 1 CTA.
- `PriceUp #16A34A` / `PriceDown #DC2626` — únicos colores saturados visibles la mayor parte del tiempo.
- `surfaceContainer #F5F5F5` para WidgetCards sin shadow.
- Inter body + **JetBrains Mono para precios** = carácter instantáneo.

---

## 🐜 Codebase findings

### Paleta actual (Color.kt)

| Token | Hex | Nota |
|---|---|---|
| SoftSkyPrimary | `#7FA8D1` | **Accent pastel — epicentro del problema** |
| SoftSkyPrimaryContainer | `#DCE8F5` | azul muy lavado |
| SoftSkyBackground | `#F7FAFC` | blanco frío teñido — contribuye al foggy |
| SoftSkySurface | `#FFFFFF` | ok |
| CardSurface | `#ECF1F6` | teñido frío |
| SoftSkyOnSurface | `#2B3A4A` | near-navy (no near-black) |
| SoftSkyOnSurfaceVariant | `#6B7B8C` | gris-azul desaturado |
| SoftSkyOutline | `#D9E1EA` | outline muy lavado |
| SoftSkyError | `#B85A5A` | rojo apagado |
| PriceUp / PriceUpText | `#4CAF7F` / `#2E9C68` | verdes correctos semánticamente |
| PriceDown | `#E88B8B` | rojo demasiado pastel |
| SegmentedTrack | `#DDE5EF` | track pastel |
| SoftAvatarPalette x8 | varios pasteles | necesita rework |

### Tipografía actual (Typography.kt)

- **Inter custom bundleada** (4 pesos: Regular/Medium/SemiBold/Bold) en `res/font/`.
- Escala M3: `titleLarge` (22sp Bold) → `titleMedium` (20sp SemiBold) → `titleSmall`/`bodyLarge/Medium/Small`/`labelLarge/Medium/Small`.
- `displayLarge`, `headlineLarge/Medium/Small` **no definidos** → quedan en default Roboto.
- `PriceTextStyle` (16sp Bold, `tnum`) y `PricePctTextStyle` (12sp Normal, `tnum`) → **números tabulares ya activos**.

### Superficies UI

| Composable | Archivo | Uso de color |
|---|---|---|
| AppTheme | `ui/theme/Theme.kt` | raíz tema, statusBar light |
| SearchableTopBar | `SearchableTopBar.kt` | containerColor = background |
| StocksScreen | `MainActivity.kt:52` | Column + LazyColumn |
| StockTickerItem | `StockTickerItem.kt` | avatar circular + flash bg + precio |
| ConnectionStatusBar | `ConnectionStatusBar.kt` | Row estado + Button |
| TradingViewScreen | `TradingViewScreen.kt` | 2 WidgetCards |
| WidgetCard | `TradingViewScreen.kt:142` | `containerColor = CardSurface`, `elevation = 4.dp` ← shadow blando |
| TabSelector | `TradingViewScreen.kt:239` | Segmented custom animado |
| TradingViewWebView | `TradingViewWebView.kt:232` | `backgroundColor = surface.toArgb()` |

### Colores hardcoded fuera de Color.kt

- `StockTickerItem.kt:84` — `Color.White` (texto inicial avatar)
- `ConnectionStatusBar.kt:48,59` — `Color.White` (botones)
- `HotlistsWidget.kt:19-27` — rgba strings hardcoded (`rgba(41,98,255,1)` plotLine growing, gridLine, `#5C6B7A` scaleFont, symbolActive) — acento azul TV independiente del theme
- `TradingViewWebView.kt:232` — bien, hereda de surface

### Accent signature — presencia real de `primary`

`SoftSkyPrimary` aparece en muy pocos lugares:
- `TradingViewScreen.kt:212` — `CircularProgressIndicator`
- Botón "Reintentar" en `WidgetBox` (implícito M3)
- Chrome M3 implícito

**Conclusión**: swap de primary es **low-risk**. El problema real del "foggy" NO es el primary (apenas se ve) sino:
1. `SoftSkyBackground #F7FAFC` (teñido frío)
2. `SoftSkyOnSurface #2B3A4A` (near-navy)
3. `CardSurface #ECF1F6` (teñido frío)
4. `SoftSkyOutline #D9E1EA` (outline lavado)
5. `PriceDown #E88B8B` (rojo pastel)
6. SoftAvatarPalette (pasteles)

### Puntos de cambio por impacto

**ALTO** (1 edit → 95% UI)
1. `ui/theme/Color.kt` — reemplazar todos los `SoftSky*` tokens M3.
2. `ui/theme/Theme.kt` — renombrar `LightColors` y refs si se cambia naming.

**MEDIO** (tokens extra-tema, ajustar a mano)
3. `Color.kt:32-45` — `PriceUp*`, `PriceDown*`, `PriceUpFlash`, `PriceDownFlash`, `StatusWarning`, `SegmentedTrack`, `CardSurface`.
4. `Color.kt:48-57` — `SoftAvatarPalette` x8.
5. `TradingViewScreen.kt:149` — `CardSurface` hardcoded en WidgetCard.
6. `TradingViewScreen.kt:258-268` — `SegmentedTrack` hardcoded en TabSelector.

**BAJO** (opcional)
7. `HotlistsWidget.kt:19-27` — rgba TV widget.
8. `StockTickerItem.kt:84`, `ConnectionStatusBar.kt:48,59` — `Color.White` hardcoded (robusto pero no respeta `onPrimary`).
9. `Typography.kt` — agregar `displayLarge/headline*` custom si se va a usar; agregar JetBrains Mono si se suma.

### TradingView — control de paleta

- `colorTheme = "light"` hardcoded en `HeatmapConfig` y `HotlistsConfig`.
- Bg propaga automático desde theme.
- Colores de línea (rgba) independientes — se pueden armonizar al nuevo accent.

---

## Cross-reference

### Acuerdos
- Fondo blanco puro + near-black genuino es la fórmula para despastelizar manteniendo light.
- `tnum` para números está ya resuelto en el proyecto (alineado con recomendación web).
- Inter como base funciona; el proyecto ya la tiene bundleada.
- M3 setup actual es correcto (sin surface tint, sin dynamic color).

### Complementos
- 🐬 provee 4 paletas accionables con hex + rationale + tipografía.
- 🐜 confirma que `primary` actual tiene presencia baja (swap low-risk), identifica los tokens "foggy" reales (background teñido, onSurface near-navy, CardSurface teñido, outline lavado, SoftAvatarPalette pastel) y localiza los 6 puntos concretos de edición.

### Conflictos
Ninguno. El web recomienda eliminar surface tint; el codebase muestra que ya no se usa (M3 1.2+).

### Gap analysis

| Aspecto | Actual | Target (Option A + #2563EB) | Delta |
|---|---|---|---|
| background | `#F7FAFC` teñido frío | `#FFFFFF` puro | quitar tinte |
| onSurface | `#2B3A4A` near-navy | `#111111` near-black | menos azul, más contraste |
| primary | `#7FA8D1` pastel | `#2563EB` blue-600 saturado | de pastel a signature |
| CardSurface | `#ECF1F6` teñido | `#F5F5F5` neutro o `#FFFFFF` + hairline | despastelizar |
| outline | `#D9E1EA` lavado | `#E5E5E5` neutro | neutralizar |
| PriceDown | `#E88B8B` pastel | `#DC2626` saturado | saturar |
| PriceUp | `#4CAF7F`/`#2E9C68` | `#16A34A` | saturar un poco |
| SoftAvatarPalette | 8 pasteles | rework: 8 shades de 1 hue o escala de gris + 1 accent | redesign |
| Elevation de Card | `elevation = 4.dp` (shadow blanda) | `elevation = 0.dp` + outline hairline o `surfaceContainer` | eliminar shadow |
| Tipografía | Inter sola | Inter + JetBrains Mono para números | aditivo, bundlear .ttf |

### Aplicabilidad al proyecto
- El cambio es de **alcance acotado**: 1 archivo (Color.kt) hace la mayor parte. 4-6 archivos más para ajustes extra-tema.
- Bajo riesgo: `primary` actual apenas se ve → no hay regresiones visuales mayores.
- TradingView: `surface.toArgb()` → propaga automático. Widget line colors son independientes (se puede armonizar o dejar).
- JetBrains Mono es aditivo (nuevo `.ttf` en `res/font/`) — no rompe nada.

---

## Conclusión

**Decisión del usuario (2026-04-18)**: **Option D Monocromo + Naranja Galicia `#E67B21`** como signature accent único.

### Paleta final "Galicia Editorial"

```
background:        #FFFFFF    (blanco puro)
surface:           #FFFFFF
surfaceContainer:  #F4F4F4    (gris neutro sin tinte)
surfaceContainerHigh: #EBEBEB
onSurface:         #0A0A0A    (near-black, no azulado)
onSurfaceVariant:  #6B6B6B
outline:           #EBEBEB    (hairline neutra)
outlineVariant:    #F0F0F0
primary:           #E67B21    (Naranja Galicia — signature único)
onPrimary:         #FFFFFF
primaryContainer:  #FBE8D4    (para chips/badges sutiles con accent)
onPrimaryContainer: #6B3A0F
error:             #B91C1C    (rojo saturado, no el rojo Galicia por claridad semántica)

PriceUp:           #16A34A    (green-600 saturado — único verde permitido)
PriceDown:         #DC2626    (red-600 saturado — diferenciado del primary naranja)
PriceUpFlash:      #D1FAE5    (tinte verde muy sutil para flash bg)
PriceDownFlash:    #FEE2E2    (tinte rojo muy sutil)
SegmentedTrack:    #F4F4F4    (neutro sin tinte)
```

Colores Galicia secundarios disponibles si se quieren badges/estados con variantes de marca (uso opcional y puntual):
- `#EF945A` — naranja claro (hover states, chips secundarios)
- `#F7CEB5` — naranja palest (backgrounds de badge muy sutiles)
- `#AD2931` — rojo profundo Galicia (NO usar como `PriceDown` — reservar para error crítico si se necesita)

### Regla de uso del accent `#E67B21`
- **Sí**: focus rings del search field, underline de tab activo, CircularProgressIndicator, botón "Conectar"/"Reintentar", badge de status conectado, `primaryContainer` para chips de filtro activos.
- **No**: fondos grandes, cards, nav chrome, textos largos.

### SoftAvatarPalette — rework
Reemplazar los 8 pasteles por **8 shades de gris neutro** (editorial) con el texto inicial en `#E67B21` y `weight = ExtraBold`. Alternativa más viva: los 8 avatares en `#F4F4F4` con el inicial en naranja saturado.

### Fases de implementación

1. **Fase 1 — Despastelizar core** (`Color.kt`): renombrar `SoftSky*` → `Galicia*`, aplicar paleta de arriba, mapear a `lightColorScheme(...)`.
2. **Fase 2 — Semántica saturada**: `PriceUp #16A34A` / `PriceDown #DC2626` + flashes sutiles.
3. **Fase 3 — Eliminar shadow pastel**: `WidgetCard` pasar de `elevation = 4.dp` → `elevation = 0.dp` + `Modifier.border(1.dp, outline)`. Mismo tratamiento en cualquier Card con shadow blanda.
4. **Fase 4 — Rework avatares**: `SoftAvatarPalette` → 8 shades grises con texto en naranja ExtraBold.
5. **Fase 5 (opcional) — Tipografía con carácter**: bundlear JetBrains Mono, aplicar a `PriceTextStyle` y `PricePctTextStyle`. Agregar `displayLarge` con Inter ExtraBold + `letterSpacing -0.02em` para titulares.
6. **Fase 6 (opcional) — TradingView widget line colors**: armonizar el `rgba(41,98,255,1)` hardcodeado en `HotlistsWidget.kt:19-27` con el accent naranja (`rgba(230,123,33,1)`) para que el widget se integre visualmente.

### Por qué funciona

- **Carácter**: el naranja `#E67B21` es saturado y reconocible — no es un azul genérico ni un verde financiero clásico. Evoca marca argentina sin ser literal.
- **Sutileza**: al ser Option D (monocromo + 1 accent), el naranja aparece en puntos quirúrgicos. El 95% del chrome sigue siendo blanco + gris + near-black.
- **Identidad sin copiar**: no decimos "esto es Galicia", pero el usuario argentino lo siente cálido/familiar. No cae en el patrón "otra app fintech azul corporate".
- **Compatibilidad semántica**: el naranja NO colisiona con verde/rojo de precios (hue distinta). Blue-600 habría competido con el azul del TradingView widget; naranja no.

---

_Sources de la paleta Galicia_: logo oficial SVG en [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Logo_Banco_Galicia.svg) — colores extraídos directamente del CSS embebido del SVG (`.fil4 {fill:#E67B21}` como naranja principal).

Implementación por fases sugerida (a confirmar con el usuario):
1. **Fase 1 — Despastelizar core (alto impacto, bajo riesgo)**: reemplazar tokens M3 en `Color.kt` (background → `#FFFFFF`, onSurface → `#111111`, primary → `#2563EB`, outline → `#E5E5E5`, CardSurface → `#F5F5F5` o blanco + hairline).
2. **Fase 2 — Saturar semántica**: `PriceUp #16A34A`, `PriceDown #DC2626`, `SegmentedTrack #F3F4F6`.
3. **Fase 3 — Eliminar shadow pastel**: `WidgetCard` de `elevation = 4.dp` → `elevation = 0.dp` + `Modifier.border(1.dp, outline)`.
4. **Fase 4 — Rework avatars**: `SoftAvatarPalette` → 8 shades de gris neutro (editorial) o 8 de blue-600 con alpha.
5. **Fase 5 (opcional) — Tipografía con carácter**: bundlear JetBrains Mono, aplicar a `PriceTextStyle` y `PricePctTextStyle`. Definir `displayLarge` con Inter ExtraBold + `letterSpacing -0.02em`.

Alternativas:
- Si el usuario prefiere un look **más cálido/diferenciado** (menos "banco digital"), ir a **Option B Warm Off-White** con forest green `#163300` + accent lima `#9FE870`.
- Si prefiere **máxima restricción** (pure editorial), ir a **Option D Monocromo** con 1 accent único (violeta `#7C3AED` da el look menos "fintech-corporate").
