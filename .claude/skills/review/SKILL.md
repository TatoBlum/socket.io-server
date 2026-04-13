---
name: review
description: Solo review — 🦉 + 🦂 en paralelo sobre el diff actual.
user_invocable: true
---

# /review

Review puro del diff actual. Útil para revisar cambios hechos fuera de `/orchestrate` (manuales, de otra rama, etc.).

## Flujo

1. Chequeá que hay diff: `git diff --name-only`. Si está vacío, reportalo y salí.
2. Lanzá 🦉 y 🦂 **en paralelo** (misma tanda de Agent calls):
   ```
   Agent (x2 en paralelo):
     - subagent_type: reviewer, model: sonnet, description: "🦉 Review"
     - subagent_type: security-auditor, model: sonnet, description: "🦂 Security"
     prompts: leer agents/reviewer.md | security-auditor.md + learnings.md + patterns
   ```
3. Si 🦂 reporta **CRITICAL** o **HIGH**, marcalo prominente al inicio del resumen.
4. Presentá resumen unificado al usuario en español.

## Output al usuario

```
## Review

### 🦂 Security — <PASS/FAIL>
<resumen + CRITICAL/HIGH si aplica>

### 🦉 Reviewer — <APROBADO / CAMBIOS MENORES / RECHAZADO>
<resumen + issues>

### Acción sugerida
<según veredictos>
```
