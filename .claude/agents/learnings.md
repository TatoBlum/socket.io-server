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

## Patrones que funcionaron

- **[x1 | última: 2026-04-13]** Paralelizar research (🐬) + plan (🦧) en Step 1 cuando la tarea tiene dos sub-tareas independientes (ej. "review arquitectural" + "research endpoint"). Ahorra una vuelta completa. _Fuente: ciclo websocket fase 1_
- **[x1 | última: 2026-04-13]** Dividir la Fase 1 en PR 1A (muy pequeño, solo Constants) + PR 1B (todo lo demás) permite paralelismo real con archivos disjuntos entre developers, evitando conflictos de merge. _Fuente: ciclo websocket fase 1_

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

## Archivo

_(vacío)_
