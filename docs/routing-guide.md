# Telegram Routing Guide

## Overview

This guide explains how to use the Telegram bot routing system.

---

## Working with Commands

### Adding a New Command

**Step 1:** Add command to enum in `Command.java`

```java
// src/main/java/op/edu/ua/petbed/telegram/command/Command.java
public enum Command {
    START("/start"),
    MENU("/menu"),
    MY_COMMAND("/my_command"),  // new command
    DEFAULT;

    // ... rest of code
}
```

**Step 2:** Create handler class

```java
// src/main/java/op/edu/ua/petbed/telegram/command/handler/MyCommandHandler.java
package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class MyCommandHandler implements CommandHandler {

    @Override
    public Command getCommand() {
        return Command.MY_COMMAND;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return SendMessage.builder()
                .chatId(context.chatId())
                .text("Your response text here!")
                .build();
    }
}
```

**That's it!** Spring will automatically register it.

---

## Working with Callbacks

### How It Works

1. **Bot sends inline keyboard with callback data for each button** (CallbackId + entityId + offset)
2. **User clicks button**
3. **Telegram sends CallbackQuery to bot**
4. **Router parses CallbackData and calls appropriate handler**

### CallbackId Hierarchy

UI hierarchy is defined in `CallbackId` enum (see `docs/callback-tree.md`).

### Creating Callback Buttons via InlineKeyboardBuilder API

| Method                                       | Description                                          |
|----------------------------------------------|------------------------------------------------------|
| `navButtonsFor(CallbackId)`                  | Creates buttons from all of children                 |
| `backButtonFor(CallbackId)`                  | Adds back button to parent                           |
| `backButtonTo(CallbackId)`                   | Adds back button to specific id                      |
| `paginatedList(Page<CallbackListItem> page)` | Creates pagination list with buttons and page number |

### Examples

TODO
---

## Working with Callbacks (Handler)

```java

@Component
public class PetDetailHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_DETAIL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        PetDTO pet = petService.findById(entityId);
        return mapToResponse(context, pet);
    }

    private BotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text(formatPetInfo(pet))
                .keyboard(petKeyboard(pet))
                .editMessage(context.messageId())
                .build();
    }
}
```

---

## Key Files

| File                                             | Purpose                    |
|--------------------------------------------------|----------------------------|
| `telegram/command/Command.java`                  | Command enum               |
| `telegram/command/CommandHandler.java`           | Command handler interface  |
| `telegram/command/CommandContext.java`           | Command context record     |
| `telegram/callback/CallbackId.java`              | Callback hierarchy enum    |
| `telegram/callback/CallbackData.java`            | Callback data format       |
| `telegram/callback/CallbackHandler.java`         | Callback handler interface |
| `telegram/callback/CallbackQueryContext.java`    | Callback context record    |
| `telegram/auth/TelegramAuthService.java`         | Auth service               |
| `telegram/auth/UserAuthContext.java`             | Auth context record        |
| `telegram/service/TelegramUpdateRouterImpl.java` | Main router                |
| `telegram/response/InlineKeyboardBuilder.java`   | Keyboard builder           |

---

## Response Types

### SendMessage (text reply)

```
SendMessage.builder()
        .chatId(chatId)
        .text("Hello!")
        .build();
```

### SendMessage with Keyboard

```
SendMessage.builder()
        .chatId(chatId)
        .text("Choose option:")
        .replyMarkup(keyboard)
        .build();
```

### EditMessageText (edit callback response)

```
EditMessageText.builder()
        .chatId(chatId)
        .messageId(messageId)
        .text("Updated text!")
        .build();
```