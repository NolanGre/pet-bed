# Architecture Patterns

## Overview

This project uses **Spring Modulith** for modular architecture with **DDD** (Domain-Driven Design) principles and **TDD
** development approach. The application is a **Telegram Bot** (webhook-based), not a REST API.

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

**Module boundaries** are defined by Spring Modulith — each module declares its public API via `@ApplicationModule` or
package-level organization.

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
- Default to `FetchType.LAZY` on all relationships
- Use **Entity Graphs** (`@EntityGraph`) or `JOIN FETCH` in queries to load associations explicitly per use case
- Use **pagination** for large datasets
  > ⚠️ Avoid `JOIN FETCH` with pagination — Hibernate applies `LIMIT` in memory, not at DB level (`HHH90003004`). Use
  `@EntityGraph` with `LAZY` loading or a separate count query instead.

## Testing Strategy (TDD)

See `.opencode/rules/testing.md` for details.

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
throw new PetBedException("message",PetBedException.ErrorCode.CODE);
```

Exceptions are handled by `@ControllerAdvice` or `WebhookExceptionHandler`.

## Telegram Routing Rules

See `.opencode/rules/telegram-routing.md` for detailed Telegram bot routing patterns, `docs/routing-guide.md` for usage
examples, `docs/callback-tree.md` for complete CallbackId hierarchy, and `docs/callback-business-rules.md` for business
logic and behavior details.

## Null Safety (JSpecify + NullAway)

NullAway checks all code under `op.edu.ua.petbed` at **compile time as errors**.
`@NullMarked` is applied **per class** (not package-level) — only annotated classes get strict JSpecify mode.

### Class with `@NullMarked`

All fields, params, and return types are implicitly `@NonNull` unless marked `@Nullable`.

> Always add `@NullMarked` to every new class — no exceptions.

```java

@NullMarked
public class UserService {
    // Long id — implicitly @NonNull
    // @Nullable String name — explicitly nullable
}
```

> Prefer adding `@NullMarked` to all new classes — keeps null contracts explicit and fully enforced.

### Key rules for entities and services

```java
// JPA no-args constructor suppresses NullAway — uninitialized fields are expected by Hibernate
@NoArgsConstructor(access = AccessLevel.PROTECTED, onConstructor_ = @SuppressWarnings("NullAway"))
// Private constructor is used by builder — all fields are initialized
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends AbstractAuditableEntity {
}
```

- All fields are implicitly `@NonNull` unless marked `@Nullable`
- Lombok-generated code is annotated `@Generated` — NullAway skips it (`TreatGeneratedAsUnchecked=true`)
- `@Nullable` propagates to generated constructors/setters via `lombok.copyableAnnotations`
- Always use `@Nullable` on optional fields, never `Optional<>` as a field type

### When to use `@Nullable`

```java
// Field that may not be set
@Nullable
private String phoneNumber;

// Method that may return null
@Nullable
public String findNameById(Long id) { ...}

// Parameter that accepts null
public void process(@Nullable String input) { ...}
```

> NullAway catches null violations at compile time — do not add null-check tests for `@NonNull` params, and do not wrap
> returns in `Optional<>` when `@Nullable` is sufficient.