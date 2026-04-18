# Plan — Remove bottom nav + SearchableTopBar + Crossfade body

**Fecha**: 2026-04-17
**Solicitante**: Martin
**Architect**: 🦧 (Opus)

## Tarea

Sacar el `bottomBar` (NavigationBar) del `Scaffold` en `RootScreen`. Montar una pantalla única: topbar `SearchableTopBar("Trading View", placeholder "Buscar")` + body con `Crossfade(isSearchMode, tween(280))` que cambia entre `TradingViewScreen` (default) y `PlaceholderScreen("Pantalla Precios")` (modo search).

## Decisiones confirmadas con el usuario

1. `MainScreen` (lista WS 24 tickers) → **reemplazada** por `PlaceholderScreen("Pantalla Precios")`. `MainScreen`, `MainViewModel` y todas las clases WS quedan **sin uso pero NO se borran** (limpieza posterior). `CheckNetworkConnection` SÍ se sigue usando (`TradingViewScreen` lo consume).
2. Back button de la topbar en modo **no-search** → **ocultar**.
3. Título `"Trading View"` · Placeholder `"Buscar"`.

## Cambios concretos

### `app/src/main/java/com/example/socketapp/ui/RootScreen.kt` (reescritura)

- Firma nueva: `fun RootScreen(networkConnection: CheckNetworkConnection)` (sin `MainViewModel`).
- Estado local con `rememberSaveable`:
  - `isSearchMode: Boolean = false`
  - `searchQuery: String = ""`
- Usa `Scaffold` **sin** `bottomBar`, solo `topBar` + content. Mantener `containerColor = Color(0xFF121212)`.
- `topBar = SearchableTopBar(title = "Trading View", searchPlaceholder = "Buscar", isSearchMode, searchQuery, onBack = {}, onCloseSearch = { isSearchMode = false; searchQuery = "" }, onOpenSearch = { isSearchMode = true }, onQueryChange = { searchQuery = it }, showNavigationIcon = isSearchMode)`.
- Body:
  ```kotlin
  Crossfade(
      targetState = isSearchMode,
      animationSpec = tween(durationMillis = 280),
      label = "titulos-body",
      modifier = Modifier.padding(innerPadding).fillMaxSize(),
  ) { searchMode ->
      if (searchMode) PlaceholderScreen(label = "Pantalla Precios")
      else TradingViewScreen(networkConnection)
  }
  ```

### `app/src/main/java/com/example/socketapp/ui/SearchableTopBar.kt`

- Agregar parámetro opcional `showNavigationIcon: Boolean = true` (default preserva compat con el sandbox).
- Envolver el `IconButton` del slot `navigationIcon` con `if (showNavigationIcon) { ... }` (o usar lambda vacía cuando false).

### `app/src/main/java/com/example/socketapp/MainActivity.kt`

- Remover `val mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]`.
- Ajustar llamada a `RootScreen(checkNetworkConnection)`.
- **NO** borrar la composable `MainScreen(...)` de abajo (dead code intencional — comentar arriba `// TODO: unused after nav removal, cleanup en ticket siguiente`).

### `app/src/main/java/com/example/socketapp/ui/RootTab.kt`

- **Borrar** (enum trivial, único consumer era `RootScreen`).

### `TitulosSearchScreen.kt`

- **NO tocar**. `PlaceholderScreen` queda exportado desde ahí (mismo package, sin import extra).

## Equipo propuesto

- **1 developer Sonnet** (🐺). La tarea toca 4 archivos acoplados (RootScreen ↔ SearchableTopBar), <80 líneas netas. Más developers = overhead de conflictos.

## Riesgos principales

- `SearchableTopBar` hoy muestra back SIEMPRE → hay que modificar el composable, no alcanza con `navigationIcon = {}`.
- `AnimatedTopBarSandboxPreview` debe seguir compilando → default `showNavigationIcon = true` lo garantiza.
- `searchQuery` debe resetearse en `onCloseSearch` para no re-abrir con texto viejo.
- No envolver el `Crossfade` en otro `verticalScroll` (TradingViewScreen ya scrollea).
- Borrado de `RootTab.kt` debe ir después de editar `RootScreen.kt` para no romper build intermedio.

## Criterios de done

- Build OK (`./gradlew :app:assembleDebug`).
- App abre en `TradingViewScreen` con topbar "Trading View" + lupa, sin back, sin bottom nav.
- Tap lupa → input "Buscar" + back + crossfade 280ms a "Pantalla Precios".
- Tap back → vuelta a "Trading View", `searchQuery = ""`.
- Rotación preserva estado (`rememberSaveable`).
