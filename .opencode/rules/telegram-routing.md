# Telegram Routing

This project uses a custom Telegram bot routing system.

## Key Files

- `src/main/java/op/edu/ua/petbed/telegram/command/Command.java` - Command enum
- `src/main/java/op/edu/ua/petbed/telegram/command/CommandHandler.java` - Command handler interface
- `src/main/java/op/edu/ua/petbed/telegram/command/CommandContext.java` - Command context record
- `src/main/java/op/edu/ua/petbed/telegram/callback/CallbackAction.java` - Callback action enum
- `src/main/java/op/edu/ua/petbed/telegram/callback/CallbackHandler.java` - Callback handler interface
- `src/main/java/op/edu/ua/petbed/telegram/callback/CallbackQueryContext.java` - Callback context record
- `src/main/java/op/edu/ua/petbed/telegram/callback/CallbackData.java` - JSON parsing for callback data
- `src/main/java/op/edu/ua/petbed/telegram/auth/TelegramAuthService.java` - Auth service
- `src/main/java/op/edu/ua/petbed/telegram/auth/UserAuthContext.java` - Auth context record
- `src/main/java/op/edu/ua/petbed/telegram/service/TelegramUpdateRouterImpl.java` - Main router
- `src/main/java/op/edu/ua/petbed/telegram/response/ResponseBuilder.java` - Builder for Telegram responses
- `src/main/java/op/edu/ua/petbed/telegram/response/InlineKeyboardBuilder.java` - Builder for inline keyboards

## Command Flow

1. Add constant to `Command` enum
2. Create handler implementing `CommandHandler`
3. Spring auto-registers it

## Callback Flow

1. Add constant to `CallbackAction` enum
2. Create handler implementing `CallbackHandler`
3. Spring auto-registers it

## Auth Flow

- `Command.requiresAuth()` - commands that need authentication and creating user (only /start)
- `Command.requiresVolunteer()` - commands that need volunteer status (only /publish)
- Router automatically checks before executing handler

## Response Mapping (UI Layer)

Handlers MUST use ResponseBuilder with Builder Pattern to build Telegram responses.

### Handler Pattern

```java
@Override
public BotApiMethod<?> handle(CommandContext context) {
    // 1. Call domain service to get DTO
    UserDTO user = userService.findByTelegramId(context.userId());
    // 2. Map DTO to response via private method
    return mapToResponse(user);
}

private BotApiMethod<?> mapToResponse(UserDTO dto) {
    return ResponseBuilder.telegram()
            .chatId(context.chatId())
            .text("Profile: " + dto.name())
            .keyboard(profileKeyboard(dto))
            .build();
}
```

### Key Principles

1. **Private mapping method** — one per DTO type, keeps handler focused
2. **Builder Pattern** — flexible composition (text + photo + keyboard)
3. **EditMessage for callbacks** — use `.editMessage(id)` to update existing message
4. **Return BotApiMethod<?>** — router expects this type

### Builder Methods

| Method | Description |
|--------|--------------|
| `.text(String)` | Set message text |
| `.photo(String url)` | Add photo |
| `.keyboard(InlineKeyboardMarkup)` | Add inline keyboard |
| `.editMessage(Integer messageId)` | Edit existing message |
| `.chatId(Long)` | Target chat (required) |
| `.build()` | Build BotApiMethod |

## Guide

See `docs/routing-guide.md` for detailed usage.
See `docs/ui-response-mapping.md` for ResponseBuilder usage examples.