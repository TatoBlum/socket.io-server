---
name: patterns-templates
description: Boilerplate / templates de código para developers. Solo lo cargan los developers.
user_invocable: false
---

# Patterns Templates

Boilerplate de referencia para crear componentes nuevos. Solo lo cargan los **developers**. Otros agentes (reviewer, security, etc.) no lo necesitan.

## 1. Contract (MVI)

```kotlin
interface FeatureContract {
    sealed interface Intent {
        data object Load : Intent
        data class Select(val id: String) : Intent
    }

    data class State(
        val loading: Boolean = false,
        val items: List<Item> = emptyList(),
        val error: String? = null,
    )

    sealed interface Effect {
        data class ShowToast(val msg: String) : Effect
        data class NavigateTo(val route: String) : Effect
    }
}
```

## 2. ViewModel (MVI)

```kotlin
class FeatureViewModel(
    private val useCase: GetFeatureUseCase,
) : MVIViewModel<FeatureContract.Intent, FeatureContract.State, FeatureContract.Effect>(
    initialState = FeatureContract.State()
) {
    override fun handleIntent(intent: FeatureContract.Intent) {
        when (intent) {
            is FeatureContract.Intent.Load -> load()
            is FeatureContract.Intent.Select -> select(intent.id)
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            when (val res = useCase()) {
                is Resource.Success -> setState { copy(loading = false, items = res.data) }
                is Resource.Error -> {
                    setState { copy(loading = false, error = res.message) }
                    sendEffect(FeatureContract.Effect.ShowToast(res.message))
                }
                Resource.Loading -> Unit
            }
        }
    }
}
```

## 3. Screen (Compose, stateful + stateless split)

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = koinViewModel(),
    navController: NavController,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FeatureContract.Effect.ShowToast -> { /* ... */ }
                is FeatureContract.Effect.NavigateTo -> navController.navigate(effect.route)
            }
        }
    }

    FeatureContent(
        state = state,
        onIntent = viewModel::sendIntent,
    )
}

@Composable
private fun FeatureContent(
    state: FeatureContract.State,
    onIntent: (FeatureContract.Intent) -> Unit,
) {
    // UI stateless
}
```

## 4. Repository

```kotlin
class FeatureRepositoryImpl(
    private val api: FeatureApi,
    private val mapper: FeatureDtoToEntityMapper,
) : FeatureRepository {

    override suspend fun getItems(): Resource<List<Item>> = try {
        val dtos = api.getItems()
        Resource.Success(dtos.map(mapper::map))
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Unknown error")
    }
}
```

## 5. Koin module (feature)

```kotlin
val featureModule = module {
    single<FeatureRepository> { FeatureRepositoryImpl(get(), get()) }
    factory { FeatureDtoToEntityMapper() }
    factory { GetFeatureUseCase(get()) }
    viewModel { FeatureViewModel(get()) }
}
```

Registrar en `App.kt`:
```kotlin
startKoin {
    androidContext(this@App)
    modules(listOf(networkModule, featureModule, /* ... */))
}
```

## Reglas al usar templates

- Adaptá nombres al feature. No dejes `Feature` literal.
- Si el proyecto aún **no tiene** alguna de estas infraestructuras (p.ej. MVIViewModel base class, Koin), **no las inventes** — reportalo al orquestador en tu output.
- No copies secciones que no necesitás.
