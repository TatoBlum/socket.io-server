# Learnings

Memoria colectiva del equipo. Todos los agentes leen este archivo como primer paso. El orquestador (🐙) lo actualiza al final de cada ciclo (Step 8 de `/orchestrate`).

## Formato

```
- **[xN | última: YYYY-MM-DD]** Descripción de la lección. _Fuente: ciclo-origen_
```

- `xN` = recurrencia (cuántas veces se detectó).
- `última: fecha` = última vez que apareció.
- `Fuente` = ciclo que la generó.

Cuando una sección activa supera 60 entradas, mover las más antiguas a `## Archivo`.

---

## Errores a evitar

- **[x1 | última: 2026-04-13]** Cuando varios developers trabajan en worktrees paralelos, los agents devuelven archivos modificados sin commit en su worktree. El orquestador debe copiar los archivos al workspace principal (no intentar `git merge` del branch del worktree — queda "Already up to date" porque nunca hubo commit). _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** 🦉 Reviewer puede revisar el worktree de un developer en vez del workspace principal y reportar issues falsos positivos sobre archivos que el developer no tenía en scope. Al lanzar 🦉 indicar explícitamente "revisar el workspace principal, no los worktrees individuales". _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** Worktrees parten del último commit: si hay cambios **uncommitted en main**, el developer en worktree trabaja sobre código stale y puede revertir cambios previos. Reglas: (a) commitear antes de lanzar worktrees, o (b) para fixes chicos (<5 líneas), aplicar directo con Edit sin worktree. _Fuente: ciclo review WS↔VM 2026-04-13_
- **[x1 | última: 2026-04-17]** 🔧 Developer (Sonnet) puede reportar "all fixes applied + build PASS" y solo haber aplicado un subset (típicamente lint autofixes como `toUri`/`toColorInt`, saltando los fixes funcionales complejos). El orquestador debe `grep` los tokens clave del fix **antes** de lanzar 🦉/🦂 de nuevo. _Fuente: ciclo TV heatmap_
- **[x1 | última: 2026-04-17]** 🦉 Reviewer puede alucinar veredicto "APROBADO" con line numbers inventados que no matchean el archivo real. Cuando 🦉 y 🦂 corren en paralelo y divergen, trustear al que cita tokens/líneas verificables en el archivo actual. _Fuente: ciclo TV heatmap_

## Patrones que funcionaron

- **[x1 | última: 2026-04-13]** Paralelizar research (🐬) + plan (🦧) en Step 1 cuando la tarea tiene dos sub-tareas independientes (ej. "review arquitectural" + "research endpoint"). Ahorra una vuelta completa. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** Dividir la Fase 1 en PR 1A (muy pequeño, solo Constants) + PR 1B (todo lo demás) permite paralelismo real con archivos disjuntos entre developers, evitando conflictos de merge. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-14]** Dual-coroutine throttling en ViewModel (Coroutine 1 acumula, Coroutine 2 publica cada Nms) desacopla la frecuencia del WS de la UI. Usar `synchronized` en un solo bloque atómico para read+write del mapa interno. _Fuente: ciclo multi-ticker_
- **[x1 | última: 2026-04-14]** Cuando el ViewModel tiene `while(isActive) { delay(N) }`, los tests con `runTest` cuelgan en `advanceUntilIdle()` al finalizar. Fix: cada test debe llamar `vm.stopSocket()` antes de salir del bloque `runTest` para cancelar el loop. _Fuente: ciclo multi-ticker (🐸)_
- **[x1 | última: 2026-04-17]** Cuando un developer falla en aplicar fixes complejos incrementalmente (iter 2+), relanzar con **Write del archivo completo** y darle el contenido exacto esperado es más robusto que Edit incremental. Incluir checklist de tokens a verificar en el prompt. _Fuente: ciclo TV heatmap_
- **[x1 | última: 2026-04-17]** Para embeber widgets JS de TradingView en WebView Android, usar `loadDataWithBaseURL("https://tradingview-heatmap.local/", html, "text/html", "UTF-8", null)` en vez de `loadUrl("file://...")`. El WidgetSheriff de TV rechaza con 400 cuando el origin header es `null` (file:// scheme). _Fuente: ciclo TV heatmap_

## Reglas del usuario

_(vacío)_

## Anti-patrones detectados

- **[x1 | última: 2026-04-13]** En este proyecto apareció `hostnameVerifier { _, _ -> true }` en OkHttpClient — bypass TLS total. 🦂 debe flaggear esto como CRITICAL siempre. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** `GlobalScope.launch` en `WebSocketListener`/componentes Android: no cancelable, leak garantizado. Reemplazar por scope inyectado (`SupervisorJob + Dispatchers.IO`) desde arriba. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** `MutableSharedFlow()` con defaults (`replay=0, buffer=0`) pierde mensajes si el colector no está listo. Para casos de "últimos valores deben verse" usar `replay=1, extraBufferCapacity=1, DROP_OLDEST`. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** `Moshi.Builder().build()` dentro de un handler que corre por cada mensaje es un anti-patrón: Moshi es thread-safe y debe ser singleton (companion object). _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** Llamar `connectionPool.evictAll()` justo después de `newWebSocket(...)` puede cerrar la conexión que acabás de crear. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** `onClosing` que manda `unsubscribe` y luego `close()` es doble-acción: el `send` puede fallar porque el cierre ya empezó, y `close()` es redundante (OkHttp cierra automáticamente tras el callback). _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** Observers en `onResume` (LiveData o `lifecycleScope.launchWhenStarted`) sin remover los anteriores → acumulación en cada ciclo start/stop. Mover a `onCreate` o usar `repeatOnLifecycle`. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-17]** 🦂 `host.contains("domain.com")` en `WebViewClient.shouldOverrideUrlLoading` es bypass trivial (matchea `evil-domain.com.attacker.io`). Usar siempre exact + suffix: `host == d || host.endsWith(".$d")`. Aplica también a cualquier whitelist de dominios. _Fuente: ciclo TV heatmap_
- **[x1 | última: 2026-04-17]** 🦂 `Intent(ACTION_VIEW, uri).startActivity()` con scheme sin validar desde un WebView expone a deep-link injection (`intent:`, `javascript:`, custom schemes). Validar `uri.scheme in listOf("https", "http")` antes de disparar el intent. _Fuente: ciclo TV heatmap_

## Archivo

_(vacío)_
