# UI Response Mapping

## Overview

The UI layer maps domain DTOs into Telegram Bot API responses using a Builder Pattern. Handlers use private methods to
build responses and return `BotApiMethod<?>`.

## Architecture

```
Handler (receives DTO from service) → mapToResponse() → BotApiMethod<?>
```

### Flow

1. Handler calls domain service → gets DTO
2. Handler calls private `mapToResponse(DTO)`
3. `mapToResponse()` builds response and returns to router

## Response Builder

All responses use Builder Pattern for flexible composition.

### Example: Pet List with Pagination

```java
public BotApiMethod<?> handle(CommandContext context) {
    Page<PetDTO> pets = petService.findAdoptable(page, userId);
    return mapToResponse(context, pets);
}

private BotApiMethod<?> mapToResponse(CommandContext context, Page<PetDTO> pets) {
    return ResponseBuilder.telegram()
            .chatId(context.chatId())
            .text(message)
            .keyboard(petListKeyboard(pets))
            .build();
}

private InlineKeyboardMarkup petListKeyboard(Page<PetDTO> pets) {
    return InlineKeyboardBuilder.builder()
            .navButtonsFor(CallbackId.PET_LIST)
            .backButtonFor(CallbackId.PET_LIST)
            .paginatedList(pets)
            .build();
}
```

### Example: Text + Photo + Buttons

```java
private BotApiMethod<?> mapToResponse(PetDTO pet) {
    return ResponseBuilder.telegram()
            .text(formatPetInfo(pet))
            .photo(pet.photoUrl())
            .keyboard(petActionKeyboard(pet))
            .build();
}
```

### Example: Edit Message (on callback)

```java
private BotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
    return ResponseBuilder.telegram()
            .editMessage(context.messageId())
            .text(formatPetInfo(pet))
            .keyboard(petActionKeyboard(pet))
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
3. **Return BotApiMethod<?>** — router expects this type
4. **EditMessage for callbacks** — update existing message on button click
5. **Use InlineKeyboardBuilder** — all keyboards use this builder