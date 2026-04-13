# Architecture Patterns

## Overview

This project uses **Spring Modulith** for modular architecture with **DDD** (Domain-Driven Design) principles and **TDD** development approach. The application is a **Telegram Bot** (webhook-based), not a REST API.

## Module Structure (Spring Modulith)

```
src/main/java/op/edu/ua/petbed/
├── petbed/                    # Main application module (root)
├── user/                      # User domain module
│   ├── model/                 # Domain entities (DDD)
│   ├── repository/            # Data access
│   ├── service/               # Application services
│   └── dto/                   # Data transfer objects
├── telegram/                   # Telegram bot module
│   ├── service/               # Bot logic
│   └── ...                    # Command handlers, etc.
└── common/                    # Shared utilities
    ├── model/                 # Base classes
    └── exceptions/            # Exception hierarchy
```

**Module boundaries** are defined by Spring Modulith — each module declares its public API via `@ApplicationModule` or package-level organization.

## Domain-Driven Design (DDD)

### Entities

- **Domain entities** live in `model/` packages
- Use **factory methods** for creation (see `User.create()`)
- Getters that shouldn't be exposed externally are `protected`
- Extend `AbstractAuditableEntity` for `createdAt`/`updatedAt`
- Use **JSpecify** annotations (`@NullMarked`, `@Nullable`) for null safety

### Value Objects

- Immutable objects in `model/` packages
- Created via factory methods or constructors

### Domain Services

- Business logic in `@Service` classes
- One service per bounded context/aggregate

### Repository Pattern

- Use Spring Data JPA repositories
- Define custom queries in repository interfaces
- Prefer repository methods over raw queries

## Telegram Bot Architecture

### Webhook-Based Bot

The bot receives updates via webhook (not long polling):

```
Telegram → ngrok → Spring Boot (Webhook) → TelegramUpdateRouter → Handlers
```

### Update Routing

```
TelegramUpdateRouter.route(Update)
├── hasMessage()
│   ├── isCommand() → handleCommand()
│   └── else → handleTextMessage()
└── hasCallbackQuery() → handleCallback()
```

### Command Pattern

Commands are handled via `CommandHandler` interface:

```java
public interface CommandHandler {
    Command getCommand();
    BotMessage handle(String userId, String message);
}
```

### Message Flow

1. `TelegramUpdateRouter` receives `Update`
2. Extracts `MessageContext` (userId, message)
3. Dispatches to appropriate `CommandHandler`
4. Returns `BotMessage` (response to user)

## Business Logic

- **Service Layer**: `@Service` classes contain business logic
- **Use Cases**: Each service method represents one use case
- **DTOs**: For input/output transformation (not persistence)

## Database

- **Liquibase** for migrations (every schema change → new changelog)
- **PostgreSQL 17** with PostGIS for spatial data
- **JPA/Hibernate** for ORM
- Use **entity relationships** over manual joins
- Use **eager loading** to prevent N+1 queries
- Use **pagination** for large datasets

## Testing Strategy (TDD)

### Test Pyramid

```
E2E / Manual
Integration Tests     ← @SpringBootTest
Slice Tests           ← @DataJpaTest
Unit Tests            ← @ExtendWith(MockitoExtension)
```

### Test Structure

```java
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ClassNameTest {

    @Mock
    Dependency dependency;

    @InjectMocks
    ClassUnderTest underTest;

    @Test
    @DisplayName("[Human-readable test description]")
    void methodName_scenario_expectedResult() {
        // given
        // when
        // then
    }
}
```

### Naming Convention

```java
// Good
findByTelegramId_existing_user_returns_user();
create_null_telegramId_throws_PetBedException();

// Bad
testFind();
shouldReturnUser();
```

## Performance

- **Spring Boot Actuator** for monitoring
- **Micrometer + Prometheus** for metrics
- **Modulith** ensures modular, low-coupling architecture

## Development Tools

- **Lombok** for reducing boilerplate
- **JSpecify** for null-safety at compile time

## Exception Handling

Use `PetBedException` with `ErrorCode`:

```java
throw new PetBedException("message", PetBedException.ErrorCode.CODE);
```

Exceptions are handled by `@ControllerAdvice` or `WebhookExceptionHandler`.

## Telegram Routing Rules

See `.opencode/rules/telegram-routing.md` for detailed Telegram bot routing patterns and `docs/routing-guide.md` for usage examples.
