# UI Response Mapping

## Overview

The UI layer maps domain DTOs into Telegram Bot API responses using a Builder Pattern. Handlers use private methods to
build responses and return `PartialBotApiMethod<?>`.

## Architecture

```
Handler (receives DTO from service) → mapToResponse() → PartialBotApiMethod<?>
                        ↓
                    Router.deliver(response)
```

### Flow

1. Handler calls domain service → gets DTO
2. Handler calls private `mapToResponse(DTO)`
3. `mapToResponse()` builds response and returns to router
4. Router determines delivery method based on response type

---

## New Approach: deliver() Method

The router uses `deliver()` to determine how to send the response to Telegram:

```java
private void deliver(PartialBotApiMethod<?> response) throws TelegramApiException {
    // Webhook return — router does nothing, Telegram handles it
    if (response instanceof BotApiMethod<?> m) {
        return;
    }

    // Media/files — execute directly via TelegramClient
    if (response instanceof SendPhoto p)        telegramClient.execute(p);
    if (response instanceof SendDocument d)     telegramClient.execute(d);
    if (response instanceof SendVideo v)        telegramClient.execute(v);
    if (response instanceof SendLocation l)    telegramClient.execute(l);
    if (response instanceof EditMessageMedia e) telegramClient.execute(e);
    // ... other media types
}
```

### Response Method Types

Telegram Bot API methods split into two transport categories:

| Transport | Methods | Delivery |
|-----------|---------|----------|
| **Webhook return** | `BotApiMethod<?>` — text, edit, answer | Router returns via webhook |
| **execute()** | Media, files, location, contacts, stickers | Router calls `telegramClient.execute()` |

**Why this matters:**
- Text messages (`SendMessage`) → return via webhook (fast, no additional call)
- Photos/videos/documents → require `execute()` call to Telegram API
- The handler doesn't care — router handles delivery transparently

### Auto AnswerCallbackQuery

The router automatically answers callback queries:

```java
// In router.route() method
return AnswerCallbackQuery.builder()
        .callbackQueryId(context.callbackQuery().getId())
        .build();
```

**Handlers should NOT return `AnswerCallbackQuery`** — it's handled automatically.

## Response Builder

All responses use Builder Pattern for flexible composition.

### Example: Pet List with Pagination

```java
public PartialBotApiMethod<?> handle(CommandContext context) {
    Page<PetDTO> pets = petService.findAdoptable(page, userId);
    return mapToResponse(context, pets);
}

private PartialBotApiMethod<?> mapToResponse(CommandContext context, Page<PetDTO> pets) {
    return ResponseBuilder.sendMessage(context.chatId())
            .text(message)
            .keyboard(petListKeyboard(pets))
            .build();
}

private InlineKeyboardMarkup petListKeyboard(Page<PetDTO> pets) {
    return InlineKeyboardBuilder.builder()
            .navButtonsFor(CallbackId.MY_PETS)
            .backButtonFor(CallbackId.MY_PETS)
            .paginatedList(pets)
            .build();
}
```

### Example: Text + Photo + Buttons

```java
private PartialBotApiMethod<?> mapToResponse(PetDTO pet) {
    return ResponseBuilder.sendPhoto(context.chatId(), pet.photoUrl())
            .caption(formatPetInfo(pet))
            .keyboard(petActionKeyboard(pet))
            .build();
}
```

### Example: Edit Message (on callback)

```java
private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
    return ResponseBuilder.editMessage(context.chatId(), context.messageId())
            .text(formatPetInfo(pet))
            .keyboard(petActionKeyboard(pet))
            .build();
}
```

### Example: Send Photo (new message)

```java
private PartialBotApiMethod<?> mapToResponse(PetDTO pet) {
    // sendPhoto returns SendPhoto — router will execute() it
    return ResponseBuilder.sendPhoto(chatId, pet.photoUrl())
            .caption("🐕 " + pet.name())
            .keyboard(InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PET_DETAIL, pet.id())
                    .backButtonFor(CallbackId.PET_DETAIL)
                    .build())
            .build();
}
```

### Example: Edit Photo (on callback)

```java
private PartialBotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
    // editPhoto returns EditMessageMedia — router will execute() it
    return ResponseBuilder.editPhoto(context.chatId(), context.messageId(), pet.photoUrl())
            .caption("🐕 " + pet.name())
            .keyboard(InlineKeyboardBuilder.builder()
                    .navButtonsFor(CallbackId.PET_DETAIL, pet.id())
                    .backButtonFor(CallbackId.PET_DETAIL)
                    .build())
            .build();
}
```

### Example: Send Location

```java
private PartialBotApiMethod<?> mapToResponse(LocationDTO location) {
    // sendLocation returns SendLocation — router will execute() it
    return ResponseBuilder.sendLocation(chatId, location.latitude(), location.longitude())
            .build();
}
```

---

## InlineKeyboardBuilder API

All keyboards are built using `InlineKeyboardBuilder`.

### Navigation Methods

| Method                                       | Description                                                            |
|----------------------------------------------|------------------------------------------------------------------------|
| `navButtonsFor(CallbackId)`                  | Creates buttons for all children                                       |
| `navButtonsFor(CallbackId, Predicate)`      | Creates buttons for children that match predicate                         |
| `navButtonsFor(CallbackId, Long, Predicate)` | Creates buttons with entityId for children that match predicate        |
| `backButtonFor(CallbackId)`                  | Adds back button to parent                                             |
| `backButtonTo(CallbackId)`                   | Adds back button to specific id                                        |
| `paginatedList(Page<CallbackListItem> page)` | Creates pagination buttons: "<", "1/n", ">", and show list of elements |

### Usage Examples

```
// Menu with back button
InlineKeyboardBuilder.builder()
        .navButtonsFor(getCallbackId())
        .backButtonFor(getCallbackId())
        .build();

// Pet list
InlineKeyboardBuilder.builder()
        .navButtonsFor(getCallbackId())
        .backButtonFor(getCallbackId())
        .paginatedList(pets)
        .build();

// Pet detail with back to list
InlineKeyboardBuilder.builder()
        .navButtonsFor(getCallbackId())
        .backButtonFor(getCallbackId())
        .build();

// Conditional buttons - filter children by predicate
InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.PET_DETAIL, child -> child != CallbackId.PET_DELETE)
        .backButtonFor(CallbackId.PET_DETAIL)
        .build();

// Conditional with entityId
InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.PET_DETAIL, petId, pet -> pet != CallbackId.PET_DELETE)
        .backButtonFor(CallbackId.PET_DETAIL)
        .build();
```

---

## CallbackData Format

Format: `callbackId,entityId,offset` (comma-separated, empty = null)

```
123,111,222  → callbackId=123, entityId=111, offset=222
123,,2       → callbackId=123, entityId=null, offset=2
123           → callbackId=123
```

## Class Structure

```
src/main/java/op/edu/ua/petbed/telegram/
├── response/
│   ├── ResponseBuilder.java      # Main builder
│   └── InlineKeyboardBuilder.java  # Keyboard builder
├── callback/
│   ├── CallbackId.java          # Hierarchy enum
│   ├── CallbackData.java        # Data format
│   ├── CallbackQueryContext.java
│   └── CallbackHandler.java
└── command/
    └── handler/
        └── PetHandler.java     # Example handler
```

---

## Key Principles

1. **One private mapping method per DTO** — keeps handler focused
2. **Builder Pattern** — flexible composition of text/photo/keyboard
3. **Return PartialBotApiMethod<?>** — router handles delivery transparently
4. **EditMessage for callbacks** — update existing message on button click
5. **Use InlineKeyboardBuilder** — all keyboards use this builder
6. **Always provide text for new messages** — Telegram will NOT deliver callbacks if message has no text (only keyboard). This rule does NOT apply for editMessage.
7. **Handler never calls execute()** — router's deliver() method handles all transport
8. **Handler never returns AnswerCallbackQuery** — router adds it automatically