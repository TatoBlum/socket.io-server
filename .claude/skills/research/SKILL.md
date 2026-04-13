---
name: research
description: Investigación con 🐬 (web) + 🐜 (codebase) en paralelo. Guarda resultado en .claude/research/.
user_invocable: true
---

# /research

Investigación paralela: 🐬 web y 🐜 codebase.

## Flujo

1. **Verificación previa**: leé `.claude/research/INDEX.md`. Si hay un research reciente (<7 días) sobre el tema, preguntá al usuario si quiere reutilizarlo antes de re-investigar.
2. **Definí scope** con el usuario si es ambiguo (AskUserQuestion).
3. **Lanzá 🐬 y 🐜 en paralelo** (una sola tanda):
   ```
   Agent (x2 en paralelo):
     - subagent_type: researcher, model: sonnet,
       description: "🐬 Web research — <tema>"
       prompt: leer agents/researcher.md + scope
     - subagent_type: codebase-researcher, model: sonnet,
       description: "🐜 Codebase research — <tema>"
       prompt: leer agents/codebase-researcher.md + scope
   ```
4. **Cross-reference** los hallazgos. Identificá:
   - **Acuerdos** — donde ambos coinciden (más confiable).
   - **Complementos** — hallazgos únicos valiosos de cada uno.
   - **Conflictos** — info contradictoria; resolvé por recencia / confiabilidad de la fuente.
   - **Gap analysis** — distancia entre lo que recomienda 🐬 y lo que tiene el proyecto (🐜).
   - **Aplicabilidad** — filtrá lo relevante al stack del proyecto.
5. **Guardá el resultado** completo en `.claude/research/<tema-kebab>.md`:
   ```markdown
   # <Tema>

   _Fecha: YYYY-MM-DD_

   ## TL;DR (max 10 bullets — para inyección en prompts)
   - ...

   ## 🐬 Web findings
   ...

   ## 🐜 Codebase findings
   ...

   ## Cross-reference
   ### Acuerdos
   ### Complementos
   ### Conflictos
   ### Gap analysis
   ### Aplicabilidad al proyecto

   ## Conclusión
   ```
6. **Actualizá `.claude/research/INDEX.md`** agregando:
   ```
   - [<Tema>](<tema-kebab>.md) (YYYY-MM-DD)
     - **When to use**: <tipos de tareas>
     - **Key conclusion**: <una línea>
   ```
7. Presentá resumen al usuario en español.
