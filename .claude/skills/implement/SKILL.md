---
name: implement
description: Solo implementación — lanza developers en worktrees + 🐸 + 🐝. Requiere plan aprobado.
user_invocable: true
---

# /implement

Implementa un plan ya aprobado. Requiere que exista un plan en `.claude/plans/`.

## Flujo

1. **Buscá el plan aprobado** más reciente en `.claude/plans/` (o el que pase el usuario por parámetro).
2. Parseá el plan: cuántos developers, qué emoji tiene cada uno, scope de cada uno.
3. **Lanzá los N developers en paralelo** (una sola tanda de Agent calls):
   ```
   Agent:
     subagent_type: developer
     model: sonnet
     description: "🐺 Developer — Data layer"
     isolation: worktree
     prompt: |
       Leé .claude/agents/developer.md y tus skills `patterns` + `patterns-templates`.
       Leé .claude/agents/learnings.md como primer paso.
       Tu emoji: 🐺
       Scope del plan:
       <sección correspondiente del plan>
   ```
4. Esperá que todos terminen. Mergeá los worktrees a la branch actual:
   - Merge limpio → auto-merge + `git worktree remove`
   - 1-3 archivos en conflicto → lanzá un developer Sonnet para resolver
   - \>3 archivos en conflicto → escalá al usuario
5. Lanzá 🐸 (test-writer, Sonnet) para escribir tests sobre el diff.
6. Lanzá 🐝 (validator, Haiku) para build + tests + lint.
7. Presentá resumen al usuario.

## Output al usuario

```
## Implement completado

- Developers: 🐺 🦊 🐆 (N)
- Archivos creados: N
- Archivos modificados: M
- 🐸 Tests: <PASS/FAIL>
- 🐝 Build: <PASS/FAIL>
- 🐝 Lint: <Clean / N issues>
```
