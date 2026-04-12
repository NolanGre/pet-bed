# Exception Handling

## Custom Exception

Use `PetBedException` for all application exceptions:

```java
throw new PetBedException("message", ErrorCode.CODE);
```

## ErrorCode Enum

Each `ErrorCode` has a `userMessage` — a friendly message for the end user.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNSUPPORTED_UPDATE("This type of update is not supported"),
    INTERNAL_ERROR("Something went wrong. Please try again later"),
    // ...

    private final String userMessage;
}
```

## Creating New ErrorCodes

If the required `ErrorCode` doesn't exist, create a new one following the pattern:

```java
USER_NOT_FOUND("User not found"),
SOME_NEW_ERROR("Description for user");
```

**Rules:**
- Use UPPER_SNAKE_CASE for code names
- Write clear, user-friendly messages
- Group related codes together in the enum

## Handling Exceptions

- **Telegram Bot**: Use `WebhookExceptionHandler` to convert exceptions to bot responses
- **Services**: Throw `PetBedException` with appropriate `ErrorCode`
- **Controllers**: Not applicable (no REST controllers, only Telegram webhook)

## Best Practices

1. **Fail fast** — validate inputs at method entry or throw
2. **Use specific codes** — don't use generic `INTERNAL_ERROR` unless truly necessary
3. **Provide context** — message should help debug, `userMessage` helps the user
4. **Don't catch and ignore** — let exceptions propagate or handle explicitly
