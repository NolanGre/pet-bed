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

## Guide

See `docs/routing-guide.md` for detailed usage.