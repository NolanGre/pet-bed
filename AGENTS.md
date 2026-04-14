## Specific Behavior

- Use available Skills for `Spring` code style, testing, architecture, DevOps
- If a Skill applies, prefer it over repeating rules here
- Project uses **Spring Modulith** for modular architecture

## IMPORTANT

1. Before writing any code, describe your approach and wait for approval.
2. When requirements are ambiguous, ask clarifying questions before writing code.
3. After finishing code, list edge cases and suggest test cases.
4. If a task requires changes to more than 3 files, stop and break it into smaller tasks.
5. When there's a bug, start by writing a test that reproduces it, then fix it.
6. Every time I correct you, reflect on what went wrong and plan to prevent it.
7. **Control Log Output**: When running tests or bootRun, pipe output to files to avoid overwhelming the conversation context with logs. Use `./gradlew test > test.log 2>&1` or similar patterns.
8. **TDD**: Write tests first, then implement
9. **DDD**: Use domain entities with factory methods, apply domain patterns
10. **Null Safety**: Use JSpecify annotations (`@NullMarked`, `@Nullable`) to prevent null-related bugs

## Core Principles

- **Simplicity First**: Make every change as simple as possible. Impact minimal code.
- **No Laziness**: Find root causes. No temporary fixes. Senior developer standards.
- **Minimal Impact**: Changes should only touch what's necessary. Avoid introducing bugs.

## Task Management

1. **Plan First**: Write plan to `docs/todo.md` with checkable items
2. **Verify Plan**: Check in before starting implementation
3. **Track Progress**: Mark items complete as you go
4. **Explain Changes**: High-level summary at each step
5. **Document Results**: Add review section to `docs/todo.md`
6. **Capture Lessons**: Update `docs/lessons.md` after corrections

## Agent Dispatch (MANDATORY)

- **ALWAYS** follow the agent pipeline defined in `.opencode/rules/workflow.md`
- **ALWAYS** run independent pipeline steps in parallel (e.g., Security Scanner + Tester can run simultaneously after Developer completes)
- **ALWAYS** autonomously determine which agents from `.opencode/agents/` should execute each part of the user's task — do NOT ask the user which agent to use
- Available agents: `@ba`, `@developer`, `@tester`, `@reviewer`, `@debugger`, `@security-scanner`, `@ddd-architect`, `@docs-writer`
- For every non-trivial task: analyze → select agents → dispatch in parallel where possible → collect results → verify

## Rules (auto-loaded from `.opencode/rules/`)

- `code-style.md` — Java 25, Spring Boot conventions, JSpecify, code quality tools
- `architecture.md` — Spring Modulith, DDD patterns, domain organization
- `testing.md` — JUnit 5, Mockito, Testcontainers, test structure, naming conventions
- `exceptions.md` — PetBedException with ErrorCode pattern
- `git-operations.md` — Commit/push rules, PR description format
- `workflow.md` — Agent pipeline: BA → Developer → Tester → Security → QA → DocsWriter

## Build/Configuration Instructions

### System Requirements

- **Java 25** (Critical: The project requires Java 25 or higher)
- **Gradle** (via gradlew wrapper)
- **PostgreSQL 17** with PostGIS (via Docker)
- **Docker & Docker Compose** (Required)

### Environment Setup

**Prerequisite**: Start required services before development:

```bash
# Start PostgreSQL, MinIO, ngrok (Telegram webhooks)
docker compose -f local/docker-compose.yml up -d
```

**Local Development** (Recommended for faster development):

```bash
# Run tests for TDD
./gradlew test

# Start the application if necessary (requires services above)
./gradlew bootRun
```

### Development Scripts

All commands use Gradle wrapper:

- `./gradlew bootRun` - Start the application
- `./gradlew test` - Run all tests
- `./gradlew build` - Build the project
- `./gradlew bootJar` - Create executable JAR

### Code Quality Tools

The project uses:

- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Testcontainers** - Integration testing with PostgreSQL

### CI/CD

Not yet configured.

### Environment-Specific Notes

- **Local Development**: Requires PostgreSQL, MinIO, and ngrok from `local/docker-compose.yml`
- **Testing**: Testcontainers with PostgreSQL for integration tests
- **Debugging**: Remote debugging available in development

### Common Commands

```bash
# Run tests (recommended: pipe to file to avoid log overflow)
./gradlew test > test.log 2>&1

# Build
./gradlew build

# Code formatting
./gradlew spotlessApply

# Code quality checks
./gradlew spotlessCheck
./gradlew spotbugsMain

# Clean
./gradlew clean
```
