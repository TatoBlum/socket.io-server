---
name: plan
description: Solo planificación con 🦧 (Architect, Opus). Guarda el plan y espera aprobación.
user_invocable: true
---

# /plan

Planificación pura. Útil cuando querés diseñar sin implementar todavía.

## Flujo

1. Leé `.claude/research/INDEX.md` y abrí archivos de research relevantes a la tarea.
2. Lanzá 🦧:
   ```
   Agent:
     subagent_type: architect
     model: opus
     description: "🦧 Architect — <tarea>"
     prompt: |
       Leé .claude/agents/architect.md y tu skill `patterns`.
       Leé .claude/agents/learnings.md como primer paso.

       Tarea: <descripción del usuario>

       ## Available research
       <TL;DR de research disponible, si hay>
   ```
3. Si 🦧 pide más investigación, reportalo al usuario y sugerí correr `/research <tema>`.
4. Guardá el plan en `.claude/plans/YYYY-MM-DD-<tema-kebab>.md`.
5. Presentá el plan al usuario en español. **Esperá aprobación explícita.**

## Output al usuario

```
## 🦧 Plan propuesto

<contenido del plan del architect>

---

¿Aprobás el plan para avanzar con `/implement`?
```
