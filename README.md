# SocketAndroidPOC

Prueba de concepto Android de integración de un WebSocket (OkHttp) con
arquitectura MVVM simple y UI en Jetpack Compose (Material 3).

## Stack

- **Lenguaje**: Kotlin 1.9.22, JVM 17
- **UI**: Jetpack Compose (Material 3) + `activity-compose` + `lifecycle-runtime-compose`
- **Arquitectura**: MVVM (ViewModel + StateFlow), sin DI framework
- **Red**: OkHttp 4.9.0 (WebSocket)
- **JSON**: Moshi 1.15.1 (kapt codegen)
- **Tests**: JUnit 4 + `kotlinx-coroutines-test`

## Estructura

```
app/src/main/java/com/example/socketapp/
├── MainActivity.kt            UI Compose
├── MainViewModel.kt           subscribe/stop + StateFlows
├── BitcoinTickerDataSource.kt parseo + retry/backoff
├── WebSocketClient.kt         transporte (OkHttp + callbackFlow)
├── ConnectionState.kt         sealed class de estados
├── CheckNetworkConnection.kt  conectividad (LiveData)
├── ViewModelFactory.kt        factory manual del VM
└── ...
```

## Documentación

- [`documents/websocket.md`](documents/websocket.md) — **Guía de integración del
  WebSocket desde el ViewModel**. Capas, contratos, ciclo de vida, cancelación,
  reconexión, tests. Lectura obligada antes de tocar el stack de sockets.

## Build & Run

```bash
./gradlew :app:assembleDebug      # build
./gradlew :app:testDebugUnitTest  # tests unitarios
./gradlew :app:installDebug       # instalar en device/emulador
```

## Tests

- `MainViewModelTest` — 7 tests de caja negra del VM (subscribe / stop /
  idempotencia / re-subscribe / error / connectionState / onCleared).
- `BitcoinTickerParsingTest` — 4 tests del parseo Moshi.

Total: 11 tests, ~200ms.
