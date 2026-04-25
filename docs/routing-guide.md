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
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        PetDTO pet = petService.findById(entityId);
        return mapToResponse(context, pet);
    }

    private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(formatPetInfo(pet))
                .keyboard(petKeyboard(pet))
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

Telegram Bot API methods split into two transport categories:

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

Router knows how to deliver the response. All transport logic lives here.

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
    // ...
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

> If the method takes a file or media → use `execute()`.  
> If it's pure JSON without attachments → can use webhook.

---

## TelegramMessageService

Use `TelegramMessageService` when you need to edit a text message that was previously a media message.

### Rule

- **Webhook return** — preferred for text edits
- **editOrReplace** — only in handlers that are back/nav actions from a media screen

Currently this only applies to `MyPetsCallbackHandler` (back from `PET_DETAIL` which is a photo).

### Usage

```java
@Component
@RequiredArgsConstructor
public class MyPetsCallbackHandler implements CallbackHandler {

    private final TelegramMessageService telegramMessageService;

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Build message
        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("🐾 Мої улюбленці")
                .keyboard(keyboard)
                .build();

        // Try edit, if previous was media → delete & send new
        telegramMessageService.editOrReplace(context.messageId(), message);

        return AnswerCallbackQuery.builder()
                .callbackQueryId(context.callbackQuery().getId())
                .build();
    }
}
```