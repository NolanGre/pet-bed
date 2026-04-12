# Agent Workflow

## When to Use Pipeline

Use the full pipeline only for **non-trivial** tasks:
- Multiple files (3+)
- Database changes
- New domain logic
- Architectural decisions

**Skip pipeline** for simple tasks:
- Fixing typos
- Config changes
- Small bug fixes
- Single file changes

## Self-Improvement (CRITICAL)

After **ANY** user correction:
1. Update `./docs/lessons.md` with what went wrong
2. Write rules to prevent the same mistake
3. Review relevant lessons at session start

## Task Management

1. **Plan First**: Write plan to `./docs/todo.md` with checkable items
2. **Verify Plan**: Check in before starting
3. **Track Progress**: Mark items as you go
4. **High-level summary**: Explain changes at each step

## Core Principles

- **Simplicity First**: Minimal changes, minimal impact
- **No Laziness**: Find root causes, no temp fixes
- **Minimal Impact**: Only touch what's necessary

## Full Pipeline (For Complex Tasks)

Only run when task meets ANY of:
- 3+ files changed
- Database changes
- New domain logic
- Architectural decisions

### Step 1: Analysis (BA)
- Break down requirements
- Define scope
- Create implementation roadmap

### Step 2: Implementation (Developer)
- Write code following project conventions
- Run tests: `./gradlew test > test.log 2>&1`
- Output: code changes

### Step 3: Testing (Tester)
- Write unit/integration tests
- Verify tests pass

### Step 4: Review (Reviewer)
- Check code quality, standards, logic errors
- Classify: Critical, Important, Minor
- If Critical/Important → back to Developer → repeat

### Step 5: Security (Security Scanner)
- Scan for vulnerabilities
- Check auth, no credential leaks

### Step 6: Documentation (DocsWriter)
- Summarize changes
- Prepare PR description

## Bug Fix Pipeline

1. **Debug** — investigate root cause
2. **Fix** — implement solution
3. **Test** — write regression test
4. **Verify** — all tests pass

## Before Marking Task Complete

- Run tests
- Verify fix works
- Ask: "Would a senior developer approve this?"

## Elegance Check

For non-trivial changes: pause and ask "is there a more elegant way?"
Skip this for simple fixes — don't over-engineer.