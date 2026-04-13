---
name: orchestrate
description: Ciclo completo de desarrollo — plan → implement → review → learnings → commit. Coordina todos los agentes.
user_invocable: true
---

# /orchestrate

Ciclo completo coordinado por el 🐙 Orchestrator. Prefijá cada mensaje con `🐙 Orchestrator:` y logueá cada lanzamiento de agente.

## Step 0 — Preparación

- Leé `.claude/research/INDEX.md` buscando investigación previa relevante a la tarea.
- Confirmá que entendés la tarea. Si es ambigua, preguntá (usá AskUserQuestion).
- Si hay research reciente (<7 días) sobre el tema, mencionalo al usuario.

## Step 1 — Plan (🦧, Opus)

Lanzá la skill `/plan` que corre 🦧:

```
Agent:
  subagent_type: architect
  model: opus
  description: "🦧 Architect — <tarea en 1 línea>"
  prompt: [leé agents/architect.md + tarea del usuario + TL;DR de research relevante bajo "## Available research"]
```

- Si 🦧 responde "**necesito más investigación**", ejecutá `/research` con el scope que pidió y reintentá Step 1 una vez.

## Step 2 — Aprobación del plan

- Guardá el plan en `.claude/plans/YYYY-MM-DD-<tema-kebab>.md`.
- Presentá el plan al usuario **en español** con el equipo de developers.
- **Esperá aprobación explícita.** No avances sin un "dale", "ok", "aprobado", etc.

## Step 3 — Implement + Step 4-5 — Tests + Build

Ejecutá `/implement`:

1. Lanzá N developers en paralelo (una llamada Agent por developer, `isolation: worktree`).
2. Esperá que todos terminen. Si hay conflictos de merge:
   - 1-3 archivos → lanzá un developer Sonnet para resolver
   - \>3 archivos → escalá al usuario
3. Mergeá worktrees a la branch actual.
4. Lanzá 🐸 (test-writer) para tests.
5. Lanzá 🐝 (validator) para build + tests + lint.

## Step 6 — Review (🦉 + 🦂 en paralelo)

Lanzá 🦉 y 🦂 en **la misma tanda de Agent calls** (paralelo real).

- Si 🦂 reporta **CRITICAL** o **HIGH**, **detené el ciclo** sin importar el veredicto de 🦉. Reportá al usuario y proponé fix.

## Step 7 — Decisión

Presentá resumen en español con:

```
## Ciclo completado

- **Tarea**: ...
- **Equipo**: 🦧 + <N developers> + 🐸 + 🐝 + 🦉 + 🦂
- **Build**: <PASS/FAIL>
- **Tests**: <PASS/FAIL>
- **Security**: <PASS/FAIL>
- **Review**: <APROBADO / CAMBIOS MENORES / RECHAZADO>
```

Según el veredicto de 🦉:

- **APROBADO** → Step 8.
- **CAMBIOS MENORES** → Step 7a:
  1. Extraé los issues específicos del review.
  2. Lanzá 1 developer Sonnet con las correcciones.
  3. Re-ejecutá 🐝 + 🦉.
  4. **Max 2 iteraciones.** Después, escalá al usuario.
- **RECHAZADO** → Step 7b:
  1. Limpiá worktrees del ciclo (`git worktree remove`).
  2. Renombrá el plan a `.claude/plans/YYYY-MM-DD-<tema>-rejected.md`.
  3. Reportá al usuario. Ofrecé replanificar con el feedback negativo.

## Step 8 — Save Learnings

Guardá lecciones en `.claude/agents/learnings.md` y en la memoria del proyecto (`~/.claude/projects/.../memory/`).

**Filtro de calidad** (solo guardar si se cumple ambos):
1. ¿Olvidar esto causaría un bug o tiempo perdido en un futuro ciclo?
2. ¿Es generalizable (no hiper-específico a este contexto)?

Incluí:
- **Feedback memories** con tag del agente detector (🦉, 🦂, etc.).
- **Un project memory** resumen: fecha, tarea, veredicto, equipo, 1-3 lecciones clave.

## Step 9 — Commit & Push

Preguntá al usuario en español:

> ¿Querés que haga commit y push? ¿A qué branch?

- **Sí**: stage solo los archivos tocados en el ciclo, commit con mensaje descriptivo, push.
- **No**: dejá los cambios sin commit.
- **Nunca hagas commit sin aprobación explícita.**

## Reglas del orquestador

1. Nunca escribas código directamente.
2. Prefijá todos los mensajes con `🐙 Orchestrator:`.
3. Logueá cada agente antes de lanzarlo: `🐙 Orchestrator: Lanzando 🦧 (Architect, Opus) → Va a: <tarea>`.
4. Esperá aprobación del plan antes de implementar.
5. Corré 🐝 antes de lanzar 🦉 y 🦂.
6. Guardá learnings al final de cada ciclo.
7. Preguntá antes de commit/push.
