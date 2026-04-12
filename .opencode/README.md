# OpenCode Configuration for Pet Bed

This directory contains the complete OpenCode configuration for the Pet Bed Spring Boot Telegram Bot project.

## Quick Start

When you start working with OpenCode on this project:
1. It will read `AGENT.md` for project context
2. It will use rules from `.opencode/rules/` for coding standards
3. It will use skills from `.opencode/.agents/skills/` for specialized knowledge

## Structure

```
.opencode/
├── AGENT.md                    # Main project config
├── agents/                      # Agent definitions
│   ├── ba.md                   # Business Analyst
│   ├── developer.md            # Developer
│   ├── tester.md               # Tester
│   ├── reviewer.md             # Code Reviewer
│   ├── security-scanner.md    # Security
│   ├── ddd-architect.md       # DDD Architecture
│   ├── debugger.md            # Debugger
│   └── docs-writer.md         # Documentation
├── rules/                       # Coding rules
│   ├── architecture.md         # DDD, Spring Modulith
│   ├── code-style.md          # Java 25, Lombok, JSpecify
│   ├── testing.md             # TDD, Mockito, Testcontainers
│   ├── exceptions.md           # PetBedException
│   ├── git-operations.md      # Commit/PR rules
│   └── workflow.md            # Agent pipeline
└── .agents/skills/             # Installed skills
    ├── java-spring-boot/       # Spring Boot patterns
    ├── springboot-patterns/    # REST, DTOs, caching
    ├── spring-boot-test-patterns/ # Testing patterns
    └── java-architect/         # DDD, Clean Architecture
```

## Installed Skills

| Skill | Purpose |
|-------|---------|
| `java-spring-boot` | Spring Boot 4.x REST APIs, JPA, Security |
| `springboot-patterns` | REST patterns, DTOs, exception handling |
| `spring-boot-test-patterns` | Unit/Slice/Integration tests with Testcontainers |
| `java-architect` | DDD, Clean Architecture, Spring Modulith |

## Technology Stack

- **Spring Boot 4.0.3** with Java 25
- **Spring Modulith** for modular architecture
- **Telegram Bot API** (webhook-based)
- **PostgreSQL 17** + PostGIS
- **Liquibase** for migrations
- **JUnit 5** + Mockito + Testcontainers

## Running Tests

```bash
./gradlew test > test.log 2>&1
```

## Key Patterns

- **DDD**: Entities with factory methods, business methods instead of setters
- **TDD**: Write tests first, then implementation
- **JSpecify**: Use `@NullMarked`, `@Nullable` for null safety
- **Lombok**: Use carefully - simple getters OK, business logic = manual methods