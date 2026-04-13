# WebSocket — integración desde el ViewModel

Guía de implementación genérica del stack WebSocket de esta app. No describe
ningún caso de uso concreto (precio, chat, notificaciones): el foco está en
**cómo se integra el socket desde un ViewModel**, qué contratos expone cada
capa y cómo se propaga el ciclo de vida.

## Índice

1. [Capas y responsabilidades](#capas-y-responsabilidades)
2. [Contrato del ViewModel ↔ DataSource](#contrato-del-viewmodel--datasource)
3. [Abrir una suscripción](#abrir-una-suscripción)
4. [Cerrar la suscripción](#cerrar-la-suscripción)
5. [Ciclo de vida Android](#ciclo-de-vida-android)
6. [Reconexión y errores](#reconexión-y-errores)
7. [Observables expuestos al UI](#observables-expuestos-al-ui)
8. [Testabilidad](#testabilidad)

---

## Capas y responsabilidades

```
┌─────────────────────┐
│  Composable / View  │  observa StateFlow, llama subscribe()/stop()
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│     ViewModel       │  dueño del ciclo de vida de la suscripción
│                     │  - lanza/cancela el Job que colecta el Flow
│                     │  - traduce excepciones a estado visible
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│     DataSource      │  capa de dominio
│                     │  - parseo del payload (texto → modelo)
│                     │  - política de retry/backoff
│                     │  - filtrado de mensajes inválidos
└──────────┬──────────┘
           │
┌──────────▼──────────┐
│   WebSocketClient   │  capa de transporte
│                     │  - OkHttp + callbackFlow
│                     │  - expone connectionState
│                     │  - cierra el socket en awaitClose
└─────────────────────┘
```

**Regla de oro**: cada capa expone un `Flow` *frío* (cold). Quien colecta
paga el costo: apertura del socket, registro de listeners, timers. Quien
cancela libera: cierre del socket, desregistro, cleanup. El ViewModel es
casi siempre el único colector.

---

## Contrato del ViewModel ↔ DataSource

El `DataSource` le da al VM dos cosas y nada más:

```kotlin
open class XxxDataSource(
    private val client: WebSocketClient,
    private val url: String = /* default */,
) {
    /** Estado de la conexión subyacente, observable sin colectar el stream. */
    open val connectionState: StateFlow<ConnectionState> = client.connectionState

    /**
     * Flow frío. Cada collect abre una conexión; cancelar la cierra.
     * Aplica la política de retry/backoff internamente.
     */
    open fun start(): Flow<T> = client.connect(url)
        .mapNotNull { text -> parse(text) }
        .retryWhen { cause, attempt -> /* backoff */ }
}
```

El VM **no conoce** OkHttp, callbacks, `WebSocketListener`, ni la URL. Sólo
sabe "llamo `start()`, colecto, obtengo modelos; cancelo, todo se cierra".

---

## Abrir una suscripción

Patrón canónico en el VM:

```kotlin
class MyViewModel(
    private val dataSource: XxxDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): ViewModel() {

    private val _data = MutableStateFlow<T?>(null)
    val data = _data.asStateFlow()
    val connectionState: StateFlow<ConnectionState> = dataSource.connectionState

    private var socketJob: Job? = null

    fun subscribe() {
        if (socketJob?.isActive == true) return   // idempotencia
        socketJob = viewModelScope.launch(ioDispatcher) {
            dataSource.start()
                .catch { ex ->
                    Log.e(TAG, "stream ended with error", ex)
                    _data.value = null
                }
                .collect { item -> _data.value = item }
        }
    }
}
```

**Claves**:

- **Idempotencia**: el guard `if (socketJob?.isActive == true) return` evita
  dos collectors si `subscribe()` se llama dos veces (rotaciones, doble
  click, callbacks duplicados).
- **`viewModelScope`**: atado al ciclo de vida del VM. Cuando Android llama
  `onCleared()`, el scope se cancela → el collect termina → el socket se
  cierra.
- **`launch(ioDispatcher)`**: el default es `Dispatchers.IO`. El parámetro
  inyectable existe únicamente para tests (ver §Testabilidad).
- **`.catch { }`**: captura cualquier excepción del upstream (después de
  que retry se rindió) y pone el UI en un estado consistente. **No** re-tira.
- **`_data.value = null` en catch**: evita dejar el último valor "colgado"
  como si siguiera vivo.

---

## Cerrar la suscripción

```kotlin
fun stop() {
    socketJob?.cancel()
    socketJob = null
    _data.value = null
}
```

Al cancelar el `Job`, la cancelación se propaga **hacia arriba** por el Flow
chain:

```
socketJob.cancel()
   ↓ CancellationException
collect { } se cancela
   ↓
.catch { } NO la invoca (Flow preserva la transparencia de cancelación:
            cada operador chequea ensureActive() antes de correr su handler)
   ↓
.retryWhen { } tampoco reintenta (el guard `cause is CancellationException`
            es defensivo; aun sin él, respeta la cancelación del scope padre)
   ↓
.mapNotNull { } unwind
   ↓
WebSocketClient.connect() → callbackFlow termina → awaitClose { } corre
   ↓
webSocket.cancel() → socket TCP cerrado (cancel inmediato, sin close frame 1000)
```

El `awaitClose` del `callbackFlow` en `WebSocketClient` es lo que **realmente**
cierra el socket. Corre como cleanup unificado del `callbackFlow`: tanto al
cancelar el collector desde el VM como cuando el propio cliente cierra el
flow con `close(cause)` desde `onFailure` / `onClosing`. Si alguien olvidara
esa línea, el socket seguiría abierto después del cancel.

> **Nota sobre `.catch` y cancelación**: la regla no es "catch ignora
> CancellationException" como excepción especial, sino que Flow re-chequea
> `ensureActive()` en la jerarquía de coroutines antes de invocar cualquier
> handler. Si el scope padre está cancelado, `ensureActive()` re-lanza y el
> handler no corre. Una `CancellationException` "sintética" (no vinculada a
> cancelación del scope) **sí** podría ser atrapada por `catch` — foot-gun
> conocido a evitar tirando `CancellationException` a mano desde operadores
> upstream.

---

## Ciclo de vida Android

### Configuración recomendada

- Observar el estado de red (`CheckNetworkConnection` o similar) y:
  - Llamar `subscribe()` cuando hay conectividad.
  - Llamar `stop()` cuando se pierde.
- En Compose: hacerlo en un `LaunchedEffect(isConnected) { ... }` con
  `collectAsStateWithLifecycle()` para el estado observable.
- En XML: evitar `observe(this)` en `onResume` — acumula callbacks. Moverlo
  a `onCreate` con `repeatOnLifecycle(STARTED)` o migrar a Compose.

### `onCleared` — red de seguridad

El VM debería sobrevivir sin un `onCleared` explícito, porque cancelar el
`viewModelScope` cancela todos los jobs hijos. Aun así, override
`onCleared()` y limpiar referencias es buena práctica:

```kotlin
override fun onCleared() {
    socketJob?.cancel()
    socketJob = null
    super.onCleared()
}
```

Android llama `onCleared()` cuando el `ViewModelStore` asociado se limpia
(activity destruida sin recrearse, fragment descartado, `store.clear()`).

---

## Reconexión y errores

La **política de retry vive en el DataSource**, no en el VM. Ejemplo típico:

```kotlin
client.connect(url)
    .mapNotNull { parse(it) }
    .retryWhen { cause, attempt ->
        if (cause is CancellationException) return@retryWhen false
        if (attempt >= MAX_RETRIES) return@retryWhen false
        delay(exponentialBackoff(attempt) + jitter())
        true
    }
```

Reglas:

- **Cancelación ≠ error**. `CancellationException` nunca reintenta —
  el usuario dijo stop, respetalo.
- **Límite de reintentos**. Sin cap, un endpoint caído consume batería.
- **Backoff exponencial con jitter**. Evita "retry storms" sincronizados.
- **El cliente reporta transiciones** vía `connectionState` (`Connecting`,
  `Connected`, `Failed(cause)`, `Disconnected`). El VM expone eso tal cual,
  sin envolverlo.

---

## Observables expuestos al UI

El VM expone típicamente **dos StateFlows sueltos**:

```kotlin
val data: StateFlow<T?>                           // último modelo recibido
val connectionState: StateFlow<ConnectionState>   // estado del transporte
```

La vista (Composable) los colecta con `collectAsStateWithLifecycle()` y
formatea. Evitá combinar ambos con `combine` + `stateIn` en el VM salvo que
haya una razón real — la vista puede componer con una función pura.

```kotlin
@Composable
fun Screen(viewModel: MyViewModel) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val state by viewModel.connectionState.collectAsStateWithLifecycle()
    Text(renderLabel(data, state))
}
```

---

## Testabilidad

### Inyección de dispatcher

`MainViewModel(dataSource, ioDispatcher = Dispatchers.IO)` permite pasar un
`UnconfinedTestDispatcher` desde los tests, eliminando races con el pool
real de IO threads:

```kotlin
@Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
@After  fun tearDown() { Dispatchers.resetMain() }

@Test fun `stop cancela el collector`() = runTest {
    val fake = FakeXxxDataSource()
    val vm = MyViewModel(fake, testDispatcher)
    vm.subscribe()
    fake.send(item)
    vm.stop()
    assertNull(vm.data.value)
    assertEquals(0, fake.activeCollectors.value)
}
```

### Fake del DataSource

Para hacer `DataSource` subclasable, la clase de producción se marca `open`
(sólo la superficie estrictamente necesaria). El fake expone:

- `send(item)` — empuja un item al stream.
- `fail(cause)` — hace que el stream tire la excepción indicada.
- `activeCollectors: StateFlow<Int>` — cuántos colectores hay; útil para
  verificar idempotencia y cierre.

Patrón del fake:

```kotlin
private class FakeXxxDataSource : XxxDataSource(/* ... */) {
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    private val _active = MutableStateFlow(0)
    val activeCollectors = _active.asStateFlow()

    override fun start(): Flow<T> = events
        .onStart { _active.update { it + 1 } }
        .onCompletion { _active.update { it - 1 } }
        .transform { event -> when (event) { /* ... */ } }
}
```

### `onCleared`

`onCleared()` es `protected` — para ejercitarlo sin reflexión, usar
`ViewModelStore.clear()` que lo invoca canónicamente:

```kotlin
@Test fun `onCleared libera el collector`() = runTest {
    val fake = FakeXxxDataSource()
    val store = ViewModelStore()
    val factory = object : ViewModelProvider.Factory { /* construye VM con fake */ }
    val vm = ViewModelProvider(store, factory)[MyViewModel::class.java]

    vm.subscribe()
    assertEquals(1, fake.activeCollectors.value)
    store.clear()
    assertEquals(0, fake.activeCollectors.value)
}
```

### Qué NO testear unitariamente

- Handshake TCP, timeouts reales, retry backoff medido en wall time.
- Todo eso pertenece a un test de integración separado (opcional) o al
  smoke manual en device.

---

## Checklist al integrar un socket nuevo

- [ ] `WebSocketClient` genérico, no toca el dominio.
- [ ] `XxxDataSource` con `start(): Flow<T>` frío, retry interno, parseo interno.
- [ ] VM con `subscribe()/stop()` idempotentes y `ioDispatcher` inyectable.
- [ ] `viewModelScope.launch` — nunca `GlobalScope`.
- [ ] `.catch { }` en el VM que resetea estado (no re-tira).
- [ ] `awaitClose { webSocket.cancel() }` en el `callbackFlow` del cliente.
- [ ] Observar conectividad desde la vista (Compose `LaunchedEffect` o
      `repeatOnLifecycle`) para llamar `subscribe()/stop()` sin leaks.
- [ ] Tests de caja negra del VM (subscribe / stop / idempotencia /
      re-subscribe / error / onCleared).
