# Migración de MainActivity a Jetpack Compose

**Fecha**: 2026-04-13
**Autor**: 🦧 Architect
**Estado**: Propuesto, esperando aprobación del usuario.

## Resumen ejecutivo

Migrar la pantalla única de `MainActivity` de XML + ViewBinding a Jetpack Compose (Material 3), manteniendo intactos `MainViewModel`, `CheckNetworkConnection`, `BitcoinTickerDataSource` y demás capas. El único "plus" funcional es que se elimina implícitamente el anti-patrón de `observe(this)` acumulando callbacks en `onResume`.

## Research consultado

- `.claude/agents/learnings.md`
- `.claude/research/INDEX.md` (no hay research previo de Compose)
- Skill `patterns` — confirma Kotlin **1.9.22**, AGP 8.2.2, sin `libs.versions.toml`.

## Estado actual

- `MainActivity.kt` — `AppCompatActivity` + ViewBinding; observa `label: StateFlow<String>` con `repeatOnLifecycle(STARTED)` y `CheckNetworkConnection: LiveData<Boolean>` con `observe(this)`.
- `activity_main.xml` — único layout XML, `ConstraintLayout` con un `TextView#btc_price_tv` 40sp centrado.
- Tema actual: `Theme.SocketApp` (parent `Theme.MaterialComponents.DayNight.DarkActionBar`).
- `app/build.gradle`: `compileSdk 34`, `minSdk 21`, JVM 17, `viewBinding true`, sin Compose.
- Deps viejas: `lifecycle 2.4.0-alpha03`, `activity-ktx 1.3.0`, `core-ktx 1.7.0`, `appcompat 1.4.2`, `constraintlayout 2.1.4`, `material 1.6.1`.

## Decisiones de diseño

1. **Kotlin 1.9.22 → Compose Compiler 1.5.10** (no hay que bumpear Kotlin).
2. **Material 3** (`androidx.compose.material3`) en vez de M2 — app trivial, no vale la pena preservar M2 custom.
3. **Remover `viewBinding true`** — `activity_main.xml` es el único layout y se borra.
4. **Un solo archivo** `MainActivity.kt` con `MainScreen` composable inline — el proyecto es pequeño, no amerita separar.
5. **`CheckNetworkConnection`** se mantiene como LiveData, se observa desde Compose con `observeAsState()` (`runtime-livedata`).
6. **`label: StateFlow<String>`** se colecta con `collectAsStateWithLifecycle()` (`lifecycle-runtime-compose`) — equivalente lifecycle-aware al `repeatOnLifecycle(STARTED)` actual.
7. **`SocketAppTheme`** inline como `MaterialTheme { ... }` default — no se crea carpeta `ui/theme/`.

## Cambios en Gradle (`app/build.gradle`)

**Bumps** (necesarios para Compose):
- `lifecycle_version`: `2.4.0-alpha03` → `2.7.0`
- `activity_version`: `1.3.0` → `1.8.2`
- `core-ktx`: `1.7.0` → `1.12.0`

**Habilitar Compose**:
```groovy
buildFeatures {
    compose true
    // viewBinding true  <- REMOVER
}
composeOptions {
    kotlinCompilerExtensionVersion '1.5.10'
}
```

**Agregar deps** (BOM `2024.02.00`):
- `implementation platform('androidx.compose:compose-bom:2024.02.00')`
- `implementation 'androidx.compose.ui:ui'`
- `implementation 'androidx.compose.material3:material3'`
- `implementation 'androidx.compose.ui:ui-tooling-preview'`
- `debugImplementation 'androidx.compose.ui:ui-tooling'`
- `implementation "androidx.activity:activity-compose:$activity_version"` (reemplaza `activity-ktx`)
- `implementation "androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_version"`
- `implementation 'androidx.compose.runtime:runtime-livedata'`

**Remover**:
- `androidx.constraintlayout:constraintlayout:2.1.4`
- `androidx.activity:activity-ktx` (la trae transitivamente `activity-compose`)

**Mantener**: `appcompat`, `material` (el tema XML todavía los usa vía manifest).

## Cambios en código

**Modificar** `app/src/main/java/com/example/socketapp/MainActivity.kt`:
- Base class: `AppCompatActivity` → `ComponentActivity`.
- Eliminar ViewBinding y `setContentView`.
- `setContent { MaterialTheme { MainScreen(vm, checkNetworkConnection) } }`.

Estructura propuesta:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this, ViewModelFactory())[MainViewModel::class.java]
        val network = CheckNetworkConnection(application)
        setContent { MaterialTheme { MainScreen(vm, network) } }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel, network: CheckNetworkConnection) {
    val label by viewModel.label.collectAsStateWithLifecycle()
    val isConnected by network.observeAsState(initial = false)
    LaunchedEffect(isConnected) {
        if (isConnected) viewModel.subscribeToSocketEvents() else viewModel.stopSocket()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = label, fontSize = 40.sp)
    }
}
```

**Eliminar**:
- `app/src/main/res/layout/activity_main.xml`

**NO tocar**: `MainViewModel.kt`, `CheckNetworkConnection.kt`, `ViewModelFactory.kt`, `BitcoinTickerDataSource.kt`, `WebServiceProvider.kt`, `Constants.kt`, `BitcoinTicker.kt`, `ConnectionState.kt`, `AndroidManifest.xml`, `proguard-rules.pro`, tests.

## Developers necesarios: 1 (🐺)

Task cohesiva (Gradle + Activity + borrado XML son inseparables). Sin paralelismo posible.

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Bump `lifecycle 2.4.0-alpha03 → 2.7.0` podría cambiar APIs (`viewModelScope`, `SharingStarted`). | APIs usadas son estables desde 2.5.0. Fallback: `2.6.2`. |
| Compose Compiler 1.5.10 vs Kotlin 1.9.22 mismatch. | Tabla oficial dice compatible. Fallback: `1.5.9`. |
| Interacciones `kapt` + Compose compiler en builds incrementales. | `./gradlew clean` si hay issues raros. |
| `runtime-livedata` requiere LiveData real (confirmado: `CheckNetworkConnection : LiveData<Boolean>`). | OK. |

**Bonus**: Compose arregla implícitamente el anti-patrón de callbacks acumulados en `observe(this)` porque `observeAsState` se vincula al ciclo de vida de la composición.

## Checklist de validación

1. `./gradlew clean assembleDebug` sin errores.
2. `./gradlew test` — tests unitarios actuales pasan.
3. Run en device/emulador: label centrado con fontSize ≈ 40sp.
4. Llega el precio de BTC por WebSocket → actualiza el label.
5. Rotar pantalla → el estado sobrevive.
6. Desactivar WiFi → "Sin conexión" / "Error de conexión".
7. Reactivar WiFi → reconecta y muestra precio.
8. No quedan imports de `com.example.socketapp.databinding.*`.

## Out of scope (follow-ups)

- Tests de UI con `createComposeRule` / `ComposeTestRule`.
- `@Preview` composables.
- Theming custom (colores, tipografía).
- Migración `kapt` → KSP.
- Migración `CheckNetworkConnection` LiveData → Flow.
- Navigation Component.
