---
name: patterns
description: Patrones del proyecto — arquitectura, naming, DI, navegación, anti-patrones. Fuente de verdad compartida.
user_invocable: false
---

# Patterns

**Fuente de verdad** para cómo se escribe código en esta app. Lo cargan: 🦧 (architect), developers, 🦉 (reviewer), 🦂 (security-auditor), 🐜 (codebase-researcher).

> NOTA: Este archivo es un **template** derivado de una app Android de mayor escala. Adaptá/recortá secciones según el estado real del proyecto. Si una sección no aplica (p. ej. no usás Koin o Compose todavía), dejá la sección vacía o marcada como `N/A` para que los agentes no asuman su existencia.

## Architecture

- **MVVM + MVI híbrido.** Features nuevas usan `MVIViewModel` (base class) con contrato `Intent`/`State`/`Effect`.
- Features legacy con `AndroidViewModel` + LiveData se mantienen hasta que haya necesidad de tocarlas (no migrar proactivamente).
- Capa de datos: Repository → Remote/Local datasource. Retorna `Resource<T>` (sealed: Loading/Success/Error).
- Use Cases son `factory` en Koin, toman Repository y exponen `suspend operator fun invoke(...)`.

## UI

- **Compose** para pantallas nuevas. XML para legacy.
- **No** usar `ComposeView` dentro de XML — migrar la pantalla completa o mantenerla en XML.
- Scaffold raíz via `LocalScaffoldComposition.current`.
- Imágenes remotas: `UrlImage` (wrapper de Coil).

## Naming

| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| ViewModel | `{Feature}ViewModel` | `BitcoinViewModel` |
| Contract MVI | `{Feature}Contract` con `Intent`, `State`, `Effect` | `BitcoinContract` |
| Screen Composable | `{Feature}Screen` (stateful) + `{Feature}Content` (stateless) | `BitcoinScreen` |
| Repository | `{Feature}Repository` | `BitcoinRepository` |
| Use Case | `{Verb}{Feature}UseCase` | `GetBitcoinPriceUseCase` |
| Mapper | `{Source}To{Target}Mapper` | `BitcoinDtoToEntityMapper` |
| Koin module | `{feature}Module` | `bitcoinModule` |

## DI (Koin)

- Un módulo por feature: `bitcoinModule`, `socketModule`, etc.
- `single` para repositorios y datasources.
- `viewModel` para ViewModels.
- `factory` para use cases y mappers.
- Registrar todos los módulos en `App.kt` vía `modules(listOf(...))`.

## Navigation

- **Compose Navigation** con routes `@Serializable`.
- Entre Activities: `startActivity(Intent(...))` clásico.
- Deep links: Branch.io (ver `help.branch.io`).
- No `SafeArgs` en código nuevo.

## Project APIs

- `LocalScaffoldComposition` — acceso al Scaffold raíz desde cualquier Composable descendiente.
- `UrlImage` — carga de imágenes remotas.
- `Resource.asBearer()` — construye header `Authorization: Bearer <token>`.

## DON'T (anti-patrones explícitos)

- **No** usar `runBlocking` en producción.
- **No** crear instancias nuevas de ViewModel manualmente — siempre vía `viewModel()` de Koin.
- **No** loggear tokens, passwords ni PII.
- **No** construir el header Bearer a mano — usar `asBearer()`.
- **No** dejar clases con `@JsonClass` (Moshi) sin reglas `-keep` en ProGuard.
- **No** `setJavaScriptEnabled(true)` en WebView salvo que esté justificado.
- **No** usar `android:exported="true"` sin permisos.
- **No** `AES/ECB`, `MD5`, `SHA1`.

## Tech debt conocido (respetar — no arreglar salvo que la tarea lo pida)

- _(listar aquí deuda técnica conocida que los agentes no deben tocar)_
