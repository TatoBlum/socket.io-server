---
name: patterns-templates
description: Boilerplate / templates de código para developers. Refleja el stack real del proyecto (XML + ViewBinding + MVVM simple + OkHttp).
user_invocable: false
---

# Patterns Templates — SocketAndroidPOC

Boilerplate de referencia. Solo lo cargan los **developers**. Adaptá nombres al feature.

## 1. Activity con ViewBinding

```kotlin
package com.example.socketapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.socketapp.databinding.ActivityFeatureBinding
import kotlinx.coroutines.flow.collectLatest

class FeatureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFeatureBinding
    private lateinit var viewModel: FeatureViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeatureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this, ViewModelFactory())[FeatureViewModel::class.java]
    }

    override fun onResume() {
        super.onResume()
        observeState()
    }

    private fun observeState() {
        lifecycleScope.launchWhenStarted {
            viewModel.state.collectLatest { state ->
                binding.someText.text = state.someValue
            }
        }
    }
}
```

## 2. ViewModel con StateFlow

```kotlin
package com.example.socketapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FeatureViewModel(
    private val webServiceProvider: WebServiceProvider = WebServiceProvider(),
) : ViewModel() {

    private val _state = MutableStateFlow(FeatureUiState())
    val state = _state.asStateFlow()

    fun loadSomething() {
        viewModelScope.launch {
            // ... actualizar _state.value
        }
    }
}

data class FeatureUiState(
    val loading: Boolean = false,
    val someValue: String = "",
    val error: String? = null,
)
```

## 3. Factory (agregar rama nueva en ViewModelFactory)

```kotlin
class ViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when {
            modelClass.isAssignableFrom(MainViewModel::class.java) -> MainViewModel() as T
            modelClass.isAssignableFrom(FeatureViewModel::class.java) -> FeatureViewModel() as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
}
```

## 4. DTO con Moshi codegen

```kotlin
package com.example.socketapp

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeatureDto(
    val id: String,
    val name: String,
    val price: Double,
)
```

Si se activa `minifyEnabled true`, agregar a `proguard-rules.pro`:
```
-keep class com.example.socketapp.FeatureDto { *; }
```

## 5. WebSocket listener

```kotlin
package com.example.socketapp

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class FeatureListener(
    private val onMessage: (String) -> Unit,
    private val onError: (Throwable) -> Unit,
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) { /* ... */ }

    override fun onMessage(webSocket: WebSocket, text: String) {
        onMessage(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        onError(t)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(code, reason)
    }
}
```

## 6. Unit test (JUnit 4, sin MockK, sin coroutines-test)

Para código suspendible simple, `runBlocking` (solo en tests) o fakes manuales.

```kotlin
package com.example.socketapp

import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureMapperTest {

    @Test
    fun `maps dto to domain correctly`() {
        // given
        val dto = FeatureDto(id = "1", name = "BTC", price = 42000.0)

        // when
        val result = dto.toDomain()

        // then
        assertEquals("1", result.id)
        assertEquals(42000.0, result.price, 0.0)
    }
}
```

## Reglas al usar templates

- Adaptá nombres al feature. No dejes `Feature` literal.
- Solo usá lo que aplica al scope asignado por el architect.
- Si tu scope necesita infraestructura que **no existe** (ej. Retrofit, Koin, Room, Compose), **reportalo al orquestador en vez de inventarla**.
- Si agregás un `@JsonClass` nuevo, dejá un TODO sobre ProGuard (o resolvelo si tenés el scope).
