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
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Component
public class MyCommandHandler implements CommandHandler {

    @Override
    public Command getCommand() {
        return Command.MY_COMMAND;
    }

    @Override
    public PartialBotApiMethod<?> handle(CommandContext context) {
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

```java
// Menu with back button
InlineKeyboardBuilder.builder()
        .navButtonsFor(getCallbackId())
        .backButtonFor(getCallbackId())
        .build();
```
```java
// Pet list
InlineKeyboardBuilder.builder()
        .navButtonsFor(getCallbackId())
        .backButtonFor(getCallbackId())
        .paginatedList(pets)
        .build();
```

---

## Working with Callbacks (Handler)

```java
@Component
public class PetDetailCallbackHandler implements CallbackHandler {

    private final PetService petService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.PET_DETAIL;
    }

    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        long entityId = context.callbackData().entityId();
        if (entityId == null) {
            throw new PetBedException("Entity ID is required for PET_DETAIL", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
        PetDTO pet = petService.findById(entityId);
        return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), pet.photoId())
                .caption(pet.formatInfo())
                .keyboard(actionKeyboard(pet))
                .build();
    }

    private InlineKeyboardMarkup actionKeyboard(PetDTO pet) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.PET_DETAIL, pet.id(), child -> {
                    if (child == CallbackId.PET_UPDATE) {
                        return pet.status() == PetStatus.DEFAULT;
                    }
                    if (child == CallbackId.PET_DELETE) {
                        return pet.status().canDelete();
                    }
                    return false;
                })
                .backButtonFor(CallbackId.PET_DETAIL)
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

```java
ResponseBuilder.sendMessage(chatId)
        .text("Hello!")
        .build();
```

### SendMessage with Keyboard

```java
ResponseBuilder.sendMessage(chatId)
        .text("Choose option:")
        .keyboard(keyboard)
        .build();
```

### EditMessageText (edit callback response)

```java
ResponseBuilder.editMessage(chatId, messageId)
        .text("Updated text!")
        .build();
```

---

## Response Method Types

### PartialBotApiMethod<?> Hierarchy

Telegram Bot API methods are split into two transport categories based on how they can be delivered:

```
PartialBotApiMethod<?>
├── BotApiMethod<?>        → can return via webhook
│   ├── SendMessage
│   ├── EditMessageText
│   ├── EditMessageReplyMarkup
│   ├── AnswerCallbackQuery
│   ├── DeleteMessage
│   └── ...
└── execute() only       → CANNOT return via webhook
    ├── SendPhoto
    ├── SendDocument
    ├── SendVideo
    ├── SendAudio
    ├── SendLocation
    ├── SendVenue
    ├── SendContact
    ├── SendSticker
    ├── EditMessageMedia
    └── ...
```

**Why this matters:**
- `BotApiMethod<?>` methods send a JSON response back to Telegram via the webhook return mechanism
- Methods requiring `execute()` need the bot to make an outgoing API call — this happens after the webhook response is sent

### Rule

| Transport | Methods |
|-----------|---------|
| **Webhook return** | `BotApiMethod<?>` — text, edit, answer |
| **execute()** | Media, files, location, venues, contacts, stickers |

### Handler Interface

Handlers return `PartialBotApiMethod<?>` — agnostic to transport.

```java
public interface CallbackHandler {
    PartialBotApiMethod<?> handle(CallbackQueryContext context);
}
```

### Router Delivery

```java
private void deliver(PartialBotApiMethod<?> response) throws TelegramApiException {
    if (response instanceof BotApiMethod<?> m) {
        // return via webhook — do nothing here
        return;
    }
    // media — execute directly
    if (response instanceof SendPhoto p)        telegramClient.execute(p);
    if (response instanceof SendDocument d)     telegramClient.execute(d);
    if (response instanceof SendVideo v)        telegramClient.execute(v);
    if (response instanceof SendLocation l)     telegramClient.execute(l);
    if (response instanceof EditMessageMedia e) telegramClient.execute(e);
    // ... additional media types
}
```

### When to Use What

| Scenario | Solution |
|---|---|
| Send text | `SendMessage` → webhook return |
| Edit text | `EditMessageText` → webhook return |
| Send photo | `SendPhoto` → `execute()` |
| Edit media | `EditMessageMedia` → `execute()` |
| Send location | `SendLocation` → `execute()` |
| Answer callback | `AnswerCallbackQuery` → webhook return |
| Delete message | `DeleteMessage` → webhook return |

### Quick Test

If the method takes a file or media → use `execute()`. Else it can be returned via webhook.

---

## TelegramMessageService

Use `TelegramMessageService` when you need to edit a text message that was previously a media message.

### Rule
- Webhook return is preferred for text edits
- `editOrReplace` is only required when the previous message was media (back/nav actions)

```java
@Component
@RequiredArgsConstructor
public class TelegramMessageService {
    private final TelegramClient telegramClient;

    public void editOrReplace(Integer messageId, SendMessage message) {
        // Implementation unchanged — it retries with delete+send if edit fails
    }
}
```

