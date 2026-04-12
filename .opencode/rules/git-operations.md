# Git & Pull Request Rules

## Commit Rules

- **NEVER create commits automatically** — only commit when explicitly requested by the user
- **NEVER push to remote** without explicit user request
- **NEVER force push** or run destructive git commands without explicit approval
- When changes are ready, inform the user and wait for their instruction
- Always show `git diff` or `git status` to let the user review before committing
- Use conventional commits format (optional but recommended):
  - `feat: add new feature`
  - `fix: resolve bug`
  - `refactor: improve code`
  - `test: add tests`
  - `docs: update documentation`

## Pull Request Descriptions

- **NEVER mention AI tools** (Claude, Copilot, Gemini, etc.) in PR title or body
- **NEVER include change statistics** (file count, lines added/removed)
- **NEVER add test plan checklists** — there is no QA team to execute them
- Keep PR descriptions focused on **what** changed and **why**

## Branch Naming (Optional)

- `feature/description` — new features
- `fix/description` — bug fixes
- `refactor/description` — code improvements

## Before Commit

1. Review changes with `git diff`
2. Ensure tests pass
3. Check code follows project conventions
4. Wait for user approval before committing
