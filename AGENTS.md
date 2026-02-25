# MambaSplit Agent Guidance

## Scope
- This file applies to `mambasplit-api` only.
- Also inherit shared guidance from `../Agents.md` when present.
- Repo-specific instructions override broader/global instructions when they conflict.

## Workflow Keywords
- Inherit workflow commands from parent `C:\MambaSplit\AGENTS.md`.

## Useful Defaults To Reuse
- `agent_start_local`:
  - Run `.\scripts\start-local.ps1`.
- `agent_test_fast`:
  - Run `.\mvnw.cmd test`.
- `agent_verify_full`:
  - Run `.\mvnw.cmd verify`.
- `agent_db_up`:
  - Run `docker compose up -d db`.
- `agent_db_psql`:
  - Run `docker compose exec db psql -U mambasplit -d mambasplit`.

## Commit Message Style
- Use imperative present tense.
- Keep subject line under 72 chars.
- Example: `Add Docker preflight to local startup script`

## Safety Rules
- Never run destructive git commands unless explicitly requested (`reset --hard`, force push, delete branches).
- If rebase/merge conflicts occur, stop and ask how to proceed.
