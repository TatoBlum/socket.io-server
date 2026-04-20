# Plan: MVI Refactor MainViewModel

## Objetivo

Portar literalmente el patrón base MVI (`ViewIntent`/`ViewState`/`ViewSideEffect` + `MVIViewModel`) al package `com.example.socketapp.base`, con el `dispatcher` inyectable por constructor para preservar testabilidad. Refactor completo de `MainViewModel` + `MainScreen` + `StocksScreen` para consumir `viewState` y enviar `MainIntent`s, manteniendo semántica actual (subscribe/stop idempotente, throttle 250ms, priceDirection, passthrough de connectionState).

## Diseño de Intent / State / Effect

### `MainIntent : ViewIntent` (sealed interface)

```kotlin
sealed interface MainIntent : ViewIntent {
    data class OnSearchQueryChange(val query: String) : MainIntent
    data object SubscribeSocket : MainIntent
    data object StopSocket : MainIntent
}
```

### `MainUiState : ViewState` (data class)

```kotlin
data class MainUiState(
    val tickers: Map<String, StockTicker> = emptyMap(),
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val searchQuery: String = "",
) : ViewState
```

### `MainEffect : ViewSideEffect`

```kotlin
sealed interface MainEffect : ViewSideEffect
```

Vacío por ahora — UI no tiene Toasts/Snackbars ni navegación imperativa.

## Tareas paralelizables

### 🐺 Dev A — Base MVI infra
- **Crear**: `app/src/main/java/com/example/socketapp/base/MVIViewModel.kt`.
- Package `com.example.socketapp.base`. Interfaces `ViewIntent`/`ViewState`/`ViewSideEffect`, const `SIDE_EFFECTS_KEY`, abstract class `MVIViewModel<Intent, UiState, Effect>(protected val dispatcher: CoroutineDispatcher = Dispatchers.IO) : ViewModel()`.
- `_viewState` como `MutableState<UiState>` (Compose) — NO StateFlow.
- `fun setIntent(intent: Intent)` público — sin esto el `_event` es dead code.
- `launchInBackground` usa el `dispatcher` inyectado, NO `Dispatchers.IO` hardcodeado.
- `_event` como `MutableSharedFlow(extraBufferCapacity = 16)` para evitar perder el primer intent.

### 🦊 Dev B — Refactor `MainViewModel`
- **Modificar**: `MainViewModel.kt`. Declarar `MainIntent`/`MainUiState`/`MainEffect` en el mismo archivo.
- Firma: `class MainViewModel(private val tickerDataSource, ioDispatcher = Dispatchers.IO) : MVIViewModel<MainIntent, MainUiState, MainEffect>(ioDispatcher)`.
- `setInitialState() = MainUiState()`.
- `handleIntent`:
  - `OnSearchQueryChange` → `setState { copy(searchQuery = intent.query) }`.
  - `SubscribeSocket` → arrancar collector + publisher (idempotente via `socketJob?.isActive`). Updates via `setState { copy(tickers = snapshot) }`.
  - `StopSocket` → cancel job + clear map + `setState { copy(tickers = emptyMap()) }`.
- `init { ... }` — después del init de la super, lanzar collector de `tickerDataSource.connectionState` que hace `setState { copy(connectionState = cs) }`.
- Mantener `PUBLISH_INTERVAL_MS = 250L`, `synchronized(internalMap)`, priceDirection logic.
- Eliminar API vieja: `tickers`/`connectionState`/`searchQuery`/`onSearchQueryChange`/`subscribeToSocketEvents`/`stopSocket`.
- `override fun onCleared()`: cancelar `socketJob` + `super.onCleared()`.
- **Validar**: `ViewModelFactory.kt` sigue compilando sin cambios.

### 🐆 Dev C — Refactor Screens
- **Modificar**: `MainScreen.kt`, `StocksScreen.kt`.
- Leer state: `val state = viewModel.viewState.value`. Recompose automático (Compose State).
- Reemplazar callers:
  - `viewModel.tickers` → `state.tickers`
  - `viewModel.connectionState` → `state.connectionState`
  - `viewModel.searchQuery` → `state.searchQuery`
  - `onSearchQueryChange(q)` → `setIntent(MainIntent.OnSearchQueryChange(q))`
  - `subscribeToSocketEvents()` → `setIntent(MainIntent.SubscribeSocket)`
  - `stopSocket()` → `setIntent(MainIntent.StopSocket)`
- Import: `com.example.socketapp.MainIntent`.

## Orden de ejecución

1. **Paso 1** (solo): 🐺 crea la base MVI (~50 líneas).
2. **Paso 2** (paralelo): 🦊 + 🐆. Archivos disjuntos; 🐆 depende del contrato público de 🦊, ya definido en este plan.

## Tests (🐸, después de 🦊+🐆)

- `vm.tickers.value[k]` → `vm.viewState.value.tickers[k]`
- `vm.subscribeToSocketEvents()` → `vm.setIntent(MainIntent.SubscribeSocket)`
- `vm.stopSocket()` → `vm.setIntent(MainIntent.StopSocket)`
- Test `assertSame(fake.connectionState, vm.connectionState)` → reescribir como "connectionState llega al viewState".
- Mantener `advanceTimeBy(300)` para el publisher 250ms.

## Riesgos

1. **SharedFlow sin buffer pierde primer intent** → mitigado en 🐺 con `extraBufferCapacity = 16`.
2. **Test `assertSame(connectionState)` rompe** → mitigado en 🐸 (reescritura).
3. **Callers huérfanos** de API vieja → 🦊 debe grep-verify antes de entregar.
4. **Contrato 🦊↔🐆** → nombres exactos inyectados en prompts.
5. **`init` base corre `subscribeToEvents` antes que subclass tenga campos** → safe porque `setInitialState() = MainUiState()` es puro.

## Criterio de aceptación

- Build: `./gradlew :app:compileDebugKotlin` pasa.
- Tests: 12 tests pasan (1 reescrito).
- UI: app arranca, tickers visibles, search funciona, subscribe/stop en toggle de red funciona.
- Dead code: grep limpio de `viewModel.tickers`/`viewModel.connectionState`/`viewModel.searchQuery`/`subscribeToSocketEvents`/`stopSocket`.
