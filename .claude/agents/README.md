# Sistema de Agentes

Equipo coordinado por el Orchestrator (🐙) que ejecuta ciclos completos de desarrollo Android: plan -> implement -> review -> learnings -> commit.

## Filosofía

- El orquestador nunca escribe código — solo coordina.
- Single source of truth: cada agente vive en un solo archivo aquí.
- Feedback loop: todos leen `learnings.md` antes de empezar.
- Gates de aprobación: plan y commit requieren OK explícito del usuario.
- Paralelismo: developers en worktrees aislados, researchers en paralelo.

## Roster

| Emoji | Agente | Modelo | Archivo | Rol |
|-------|--------|--------|---------|-----|
| 🐙 | Orchestrator | (principal) | — | Coordina el ciclo completo |
| 🦧 | Architect | Opus | `architect.md` | Diseña la solución y divide el trabajo |
| 🐺🦊🐆... | Developers x N | Sonnet | `developer.md` | Implementan en worktrees aislados |
| 🐸 | Test Writer | Sonnet | `test-writer.md` | Escribe unit tests |
| 🐝 | Validator | Haiku | `validator.md` | Build + tests + lint (solo reporta) |
| 🦉 | Reviewer | Sonnet | `reviewer.md` | Code review |
| 🦂 | Security Auditor | Sonnet | `security-auditor.md` | Auditoría de seguridad |
| 🐬 | Web Researcher | Sonnet | `researcher.md` | Investigación web |
| 🐜 | Codebase Researcher | Sonnet | `codebase-researcher.md` | Investigación local |

## Skills

Workflows en `.claude/skills/` que coordinan estos agentes:

- `/orchestrate` — ciclo completo
- `/plan` — solo planificación con 🦧
- `/implement` — solo implementación
- `/review` — solo review con 🦉 + 🦂
- `/research` — solo investigación con 🐬 + 🐜
- `patterns` (compartida) — patrones del proyecto
- `patterns-templates` (compartida) — boilerplate para developers

## Archivos compartidos

- `learnings.md` — lecciones acumuladas; todos leen primero, el orquestador actualiza al final del ciclo.
- `../research/INDEX.md` — índice de investigaciones previas.
- `../plans/` — planes guardados por el architect.
- `../worktrees/` — worktrees temporales de developers.
