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

### Requiring Volunteer Status

If a command should only be available to volunteers:

```java
// In Command enum:
MY_VOLUNTEER_COMMAND("/volunteer_command") {
    @Override
    public boolean requiresVolunteer() {
        return true;
    }
},
```

The router automatically checks volunteer status before executing the handler.

---

## Working with Callbacks

### How It Works

1. **Bot sends inline keyboard with callback data**
2. **User clicks button**
3. **Telegram sends CallbackQuery to bot**
4. **Router parses action and calls appropriate handler**

### Creating Callback Buttons

**Method 1: Simple action with payload**

```java
import op.edu.ua.petbed.telegram.callback.CallbackAction;
import org.telegram.telegrambots.meta.api.objects.replygrid.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replygrid.InlineKeyboardButton;

InlineKeyboardButton button = InlineKeyboardButton.builder()
        .text("Click me!")
        .callbackData("CONFIRM")  // becomes CallbackAction.CONFIRM
        .build();

InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
        .keyboardRow(List.of(button))
        .build();
```

**Method 2: Action with pagination offset**

```java
// Build callback data manually for offset pagination
InlineKeyboardButton nextButton = InlineKeyboardButton.builder()
        .text("Next ▶️")
        .callbackData("{\"a\":\"PAGINATION\",\"o\":10}")
        .build();
```

### Available Callback Actions (Enum)

```java
// src/main/java/op/edu/ua/petbed/telegram/callback/CallbackAction.java
public enum CallbackAction {
    PAGINATION,   // pagination navigation
    CONFIRM,     // confirm action
    CANCEL,     // cancel action
    DEFAULT;    // unknown/fallback
}
```

### Adding a New Callback Handler

**Step 1:** Add action to enum (if needed)

```java
// src/main/java/op/edu/ua/petbed/telegram/callback/CallbackAction.java
public enum CallbackAction {
    PAGINATION,
    CONFIRM,
    CANCEL,
    NEW_ACTION,  // new action
    DEFAULT;
}
```

**Step 2:** Create handler

```java
// src/main/java/op/edu/ua/petbed/telegram/callback/handler/ConfirmCallbackHandler.java
package op.edu.ua.petbed.telegram.callback.handler;

import op.edu.ua.petbed.telegram.callback.CallbackAction;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.answereditcallback.EditMessageText;

@Component
public class ConfirmCallbackHandler implements CallbackHandler {

    @Override
    public CallbackAction getCallbackAction() {
        return CallbackAction.CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Access callback data with payload:
        String payload = context.callbackData().payload();
        Integer offset = context.callbackData().offset();
        
        // Get useful context info:
        Long userId = context.userId();
        Long chatId = context.chatId();
        Integer messageId = context.messageId();
        
        // Build response
        return EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text("You confirmed: " + payload)
                .build();
    }
}
```

### Callbacks with Offset (Pagination)

```java
// When building buttons for paginated list:
int currentOffset = 0;
int nextOffset = currentOffset + 10;

String callbackData = String.format("{\"a\":\"PAGINATION\",\"o\":%d}", nextOffset);

InlineKeyboardButton nextButton = InlineKeyboardButton.builder()
        .text("Next ▶️")
        .callbackData(callbackData)
        .build();
```

```java
// Handler receives offset:
public BotApiMethod<?> handle(CallbackQueryContext context) {
    Integer offset = context.callbackData().offset();  // 10
    
    // Fetch next page of items with offset
    List<Item> items = fetchItems(offset);
    
    // Build response with new pagination buttons
    return buildPaginatedMessage(items, offset);
}
```

---

## Auth System

### How It Works

1. User sends any message
2. Router calls `authService.authenticate(telegramId, username)`
3. System finds or creates user in database
4. Returns `UserAuthContext` with userId and type

### Checking Volunteer Status

**Option 1:** Via Command.requiresVolunteer()

```java
// Command enum
MY_VOLUNTEER_CMD("/volunteer") {
    @Override
    public boolean requiresVolunteer() {
        return true;
    }
},
```

**Option 2:** Manual check in handler

```java
public BotApiMethod<?> handle(CommandContext context) {
    // Already have auth context from router
    UserAuthContext auth = ...;  // router passes context
    
    if (!auth.isVolunteer()) {
        return SendMessage.builder()
                .chatId(context.chatId())
                .text("This command requires volunteer status")
                .build();
    }
    
    // Continue with logic
}
```

**Option 3:** Manual require

```java
// In handler:
authService.requireVolunteer(auth);  // throws if not VOLUNTEER
```

---

## Key Files

| File | Purpose |
|------|--------|
| `telegram/command/Command.java` | Command enum |
| `telegram/command/CommandHandler.java` | Command handler interface |
| `telegram/command/CommandContext.java` | Command context record |
| `telegram/callback/CallbackAction.java` | Callback action enum |
| `telegram/callback/CallbackHandler.java` | Callback handler interface |
| `telegram/callback/CallbackQueryContext.java` | Callback context record |
| `telegram/callback/CallbackData.java` | JSON parsing for callback data |
| `telegram/auth/TelegramAuthService.java` | Auth service |
| `telegram/auth/UserAuthContext.java` | Auth context record |
| `telegram/service/TelegramUpdateRouterImpl.java` | Main router |

---

## Response Types

### SendMessage (text reply)

```java
SendMessage.builder()
        .chatId(chatId)
        .text("Hello!")
        .build();
```

### SendMessage with Keyboard

```java
SendMessage.builder()
        .chatId(chatId)
        .text("Choose option:")
        .replyMarkup(keyboard)
        .build();
```

### EditMessageText (edit callback response)

```java
EditMessageText.builder()
        .chatId(chatId)
        .messageId(messageId)
        .text("Updated text!")
        .build();
```

### AnswerCallbackQuery (just acknowledge)

```java
AnswerCallbackQuery.builder()
        .callbackQueryId(callbackQueryId)
        .text("Processed!")
        .build();
```