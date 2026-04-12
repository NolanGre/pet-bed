# Java & Spring Boot Code Style

## Java Version

- **Java 25** (required)
- Use modern Java features (records, switch expressions, pattern matching)

## Type Safety (JSpecify)

- All classes should be annotated with `@NullMarked` (or inherit from it)
- Use `@Nullable` for nullable parameters and return types
- Avoid `null` where possible — use `Optional` or empty collections instead

## Class Organization

Order for class elements:
1. Constants
2. Fields/Properties
3. Constructor
4. Factory methods (static)
5. Public methods
6. Package-private/private methods

## Lombok Usage

### Models (DDD Entities)

- **Simple getters**: Use `@Getter` if the getter is trivial
- **Business methods**: Write manually — describe **what happens** to the entity, not a setter
- **NEVER use `@Setter`** on entities — DDD enforces behavior through domain methods
- **Protected getters**: Use `@Getter(AccessLevel.PROTECTED)` when external access not needed

```java
// Good - business method describes what happens
public void switchType() {
    this.type = (this.type == UserType.REGULAR) ? UserType.VOLUNTEER : UserType.REGULAR;
}

// Bad - setter-based mutation
public void setType(UserType type) {
    this.type = type;
}
```

### Services

- Use `@RequiredArgsConstructor` with `final` fields for dependency injection
- Avoid Lombok on service methods — keep logic explicit

### Tests

- Lombok allowed only if it doesn't reduce test readability/quality
- Prefer explicit setup for clarity over condensed Lombok magic

## Code Quality Principles

- **Simplicity First**: Prefer simple code over clever code
- **SOLID & GRASP**: Use OOP principles and Spring features instead of complex if/switch
- **No Getters for Behavior**: Don't expose internal state just to process it externally — move behavior into the class

## Database Access (JPA)

- Use repository pattern via Spring Data JPA
- Prefer repository methods over raw queries
- Use entity relationships over manual joins
- Use eager loading to prevent N+1 queries
- Use pagination for large datasets
- Remember about potential problems with Hibernate.

## Code Formatting

- Use **IntelliJ IDEA formatter** (default settings)
- Configure: `Settings → Editor → Code Style → Java`
- Apply formatting before commit (Ctrl+Alt+L / Cmd+Alt+L)

## Naming Conventions

| Element | Convention |
|---------|------------|
| Classes | PascalCase (`UserService`, `TelegramUpdateRouter`) |
| Methods | camelCase (`findByTelegramId`, `switchType`) |
| Constants | UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`) |
| Packages | lowercase, single words (`user`, `telegram`, `common`) |
| Variables | camelCase, descriptive names |
| Tests | `ClassNameTest`, method: `methodName_scenario_expectedResult` |

## Null Handling

- Avoid returning `null` — return empty `Optional`, empty list, or throw exception
- Use `@Nullable` sparingly — prefer non-null design
- Validate inputs at method entry, fail fast

## Best Practices

- Keep classes focused (Single Responsibility)
- Dependency injection via constructor (not field injection)
- Use interfaces for services when multiple implementations possible or you can't decide.
- Immutable value objects (use `final` fields, no setters)
- Prefer composition over inheritance
- Use enums or seald classes for fixed sets of values
