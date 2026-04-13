---
name: patterns
description: Patrones del proyecto SocketAndroidPOC — arquitectura, naming, dependencias, anti-patrones.
user_invocable: false
---

# Patterns — SocketAndroidPOC

**Fuente de verdad** para cómo se escribe código en esta app. Lo cargan: 🦧 (architect), developers, 🦉 (reviewer), 🦂 (security-auditor), 🐜 (codebase-researcher).

## Stack actual

| Capa | Tecnología | Versión |
|------|-----------|---------|
| Gradle | wrapper | 8.5 |
| AGP | `com.android.application` | 8.2.2 |
| Kotlin | JVM | 1.9.22 |
| JDK build | Corretto/Oracle | 17+ (probado con 21) |
| compileSdk / targetSdk | Android | 34 |
| minSdk | Android | 21 |
| Java target | bytecode | 17 |
| UI | Views XML + ViewBinding | — |
| DI | **ninguno** | — |
| Async | Coroutines + Flow | 1.5.x |
| Observabilidad | LiveData + StateFlow | lifecycle 2.4.0-alpha03 |
| Networking | OkHttp 3 (WebSocket + HTTP) | 4.9.0 |
| JSON | Moshi + codegen (kapt) | 1.15.1 |
| Tests | JUnit 4 | 4.13.2 |

## Architecture

- **MVVM simple.** `MainActivity` observa un `MainViewModel`.
- ViewModel expone **`Flow`/`StateFlow`** que el Activity consume con `lifecycleScope.launchWhenStarted { ... collectLatest { ... } }`.
- `ViewModelFactory` manual (sin DI framework).
- WebSocket handling: `SocketListener` (OkHttp `WebSocketListener`) + `WebServiceProvider`.
- Conectividad: `CheckNetworkConnection` como `LiveData<Boolean>`.
- JSON parsing: `BitcoinTicker` con `@JsonClass(generateAdapter = true)` de Moshi.

## UI

- **Solo XML layouts + ViewBinding.** No Compose.
- ViewBinding habilitado (`buildFeatures.viewBinding true`).
- Patrón en `MainActivity.kt:18-20`:
  ```kotlin
  binding = ActivityMainBinding.inflate(layoutInflater)
  setContentView(binding.root)
  ```
- Acceder a vistas vía `binding.<id_camelCase>`.

## Naming

| Tipo | Convención | Ejemplo real |
|------|-----------|--------------|
| Activity | `{Feature}Activity` | `MainActivity` |
| ViewModel | `{Feature}ViewModel` | `MainViewModel` |
| Factory | `ViewModelFactory` | `ViewModelFactory` |
| DTO Moshi | `{Entity}` con `@JsonClass` | `BitcoinTicker` |
| WebSocket listener | `{Feature}Listener` | `SocketListener` |
| Provider | `{X}Provider` | `WebServiceProvider` |
| Constantes | `object Constants` | `Constants.kt` |

## DI / wiring

- **Sin framework.** Instanciación manual vía `ViewModelFactory`:
  ```kotlin
  mainViewModel = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
  ```
- Si crece la complejidad, antes de introducir un framework, proponer la migración como plan separado.

## Navigation

- Actualmente **una sola Activity**. No hay Navigation Component, ni Fragments, ni SafeArgs.
- Si se agregan pantallas, preferir Fragments + Navigation Component (XML) para mantener coherencia con Views, o proponer migración a Compose Navigation como ciclo separado.

## Networking

- OkHttp directo. No Retrofit.
- WebSocket: `OkHttpClient.newWebSocket(request, listener)`.
- Endpoints / URLs: `Constants.kt`.
- Moshi con `kotlin-codegen` (kapt) para adaptadores.

## Coroutines / Flow

- ViewModel usa `viewModelScope.launch { ... }`.
- Estado expuesto como `StateFlow` o `Flow`.
- UI consume con `lifecycleScope.launchWhenStarted { flow.collectLatest { ... } }`.

## Build / JDK

- El build requiere **JDK 17+** (21 funciona). No usar JDK 11 (incompatible con AGP 8.x).
- `gradle.properties` tiene `--add-opens` para kapt en JDK 17+.
- Si el build falla por `class file major version`, chequear `JAVA_HOME`.

## DON'T (anti-patrones explícitos)

- **No** introducir Compose sin un ciclo de migración planificado — actualmente el proyecto es 100% XML.
- **No** introducir un framework de DI (Koin/Hilt) sin planificar — la app es pequeña, el factory manual alcanza.
- **No** usar `kotlinx.android.synthetic` — fue migrado a ViewBinding deliberadamente.
- **No** agregar `package="..."` al `AndroidManifest.xml` — AGP 8.x usa `namespace` en `build.gradle`.
- **No** hardcodear URLs en Activities/ViewModels — van en `Constants.kt`.
- **No** hacer `Log.d`/`println` de tokens, IDs sensibles, o payloads completos (el socket recibe precios en vivo — ok, pero si se agrega auth, cuidado).
- **No** construir headers `Authorization` a mano — si se agrega auth, crear un helper.
- **No** dejar clases con `@JsonClass(generateAdapter = true)` sin reglas `-keep` en `proguard-rules.pro` si se activa minify.
- **No** usar `runBlocking` en producción.
- **No** `setJavaScriptEnabled(true)` en WebView salvo justificación clara.
- **No** `android:exported="true"` sin permisos en components nuevos.
- **No** AES/ECB, MD5, SHA1 para criptografía.

## Tech debt conocido

- `kotlin-android-extensions` fue removido en la migración a Gradle 8.5 (antes de esa migración, `MainActivity` usaba synthetics).
- `minifyEnabled false` en release — si se activa, hay que agregar reglas ProGuard para `BitcoinTicker` y cualquier `@JsonClass` futuro.
- `kapt` para Moshi codegen está deprecado a favor de KSP. Migración recomendada pero no urgente.
- Tests existentes son mínimos (`ExampleUnitTest`, `ExampleInstrumentedTest`). Ampliar cobertura cuando se toque lógica.
