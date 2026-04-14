# UI Response Mapping

## Overview

The UI layer maps domain DTOs into Telegram Bot API responses using a Builder Pattern. Handlers use private methods to build responses and return `BotApiMethod<?>`.

## Architecture

```
Handler (receives DTO) → mapToResponse() → BotApiMethod<?>
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
    List<PetDTO> pets = petService.findAdoptable(page);
    return mapToResponse(pets, page);
}

private BotApiMethod<?> mapToResponse(List<PetDTO> pets, int page) {
    return ResponseBuilder.telegram()
            .text(formatPetList(pets))
            .keyboard(paginationKeyboard(page, pets.size()))
            .build();
}
```

### Example: Text + Photo + Buttons

```java
private BotApiMethod<?> mapToResponse(PetDTO pet) {
    return ResponseBuilder.telegram()
            .text(formatPetInfo(pet))
            .photo(pet.photoUrl())
            .keyboard(petActionKeyboard(pet.id()))
            .build();
}
```

### Example: Edit Message (on callback)

```java
private BotApiMethod<?> mapToResponse(CallbackQueryContext context, PetDTO pet) {
    return ResponseBuilder.telegram()
            .editMessage(context.messageId())
            .text(formatPetInfo(pet))
            .keyboard(petActionKeyboard(pet.id()))
            .build();
}
```

## Response Types

### Builder Methods

| Method | Description |
|--------|--------------|
| `.text(String)` | Set message text |
| `.photo(String url)` | Add photo |
| `.keyboard(InlineKeyboardMarkup)` | Add inline keyboard |
| `.editMessage(Integer messageId)` | Edit existing message |
| `.chatId(Long)` | Target chat (required) |
| `.build()` | Build BotApiMethod |

### Keyboard Examples

```java
// Simple buttons
InlineKeyboardMarkup keyboard = InlineKeyboardBuilder.builder()
        .row(row -> row.button("Take to Foster", "PET_TAKE_" + petId))
        .row(row -> row.button("Save to Favorites", "PET_FAV_" + petId))
        .build();

// Pagination
InlineKeyboardMarkup paginationKeyboard(int page, int total) {
    return InlineKeyboardBuilder.builder()
            .row(row -> {
                if (page > 0) {
                    row.button("◀️ Previous", "LIST_" + (page - 1));
                }
                if (total >= PAGE_SIZE) {
                    row.button("Next ▶️", "LIST_" + (page + 1));
                }
            })
            .build();
}
```

## Handler Template

```java
@Component
@RequiredArgsConstructor
public class PetsHandler implements CommandHandler {

    private final PetService petService;

    @Override
    public Command getCommand() {
        return Command.PETS;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        int page = parsePage(context.args());
        List<PetDTO> pets = petService.findAdoptable(page);
        return mapToResponse(pets, page);
    }

    private BotApiMethod<?> mapToResponse(List<PetDTO> pets, int page) {
        return ResponseBuilder.telegram()
                .text(formatPetList(pets))
                .keyboard(paginationKeyboard(page, pets.size()))
                .build();
    }

    private String formatPetList(List<PetDTO> pets) {
        StringBuilder sb = new StringBuilder("🐾 Available Pets:\n\n");
        for (PetDTO pet : pets) {
            sb.append("%d. %s (%s) - %s, %d years%n"
                    .formatted(pet.name(), pet.species(), pet.breed(), pet.age()));
        }
        return sb.toString();
    }

    private InlineKeyboardMarkup paginationKeyboard(int page, int total) {
        return InlineKeyboardBuilder.builder()
                .row(row -> {
                    if (page > 0) {
                        row.button("◀️ Previous", "PETS_" + (page - 1));
                    }
                    if (total >= PAGE_SIZE) {
                        row.button("Next ▶️", "PETS_" + (page + 1));
                    }
                })
                .build();
    }

    private int parsePage(String[] args) {
        if (args == null || args.length == 0) return 0;
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
```

## Class Structure

```
src/main/java/op/edu/ua/petbed/telegram/
├── response/
│   ├── ResponseBuilder.java      # Main builder
│   └── InlineKeyboardBuilder.java # Keyboard builder
└── command/
    └── handler/
        └── PetHandler.java       # Example handler
```

## Key Principles

1. **One private mapping method per DTO** — keeps handler focused
2. **Builder Pattern** — flexible composition of text/photo/keyboard
3. **Return BotApiMethod<?>** — router expects this type
4. **EditMessage for callbacks** — update existing message on button click