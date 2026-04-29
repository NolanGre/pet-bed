# Stage 9: Telegram Form Handlers
**Status:** 🔄 Planning Complete - Ready for Implementation
**Goal:** Implement form handlers for CREATE_LOST_REQUEST and CREATE_FOUND_REQUEST forms

---

## Overview

Stage 9 implements two `FormSubmissionHandler` components for the Lost Module that process completed forms and create domain entities. These handlers integrate with the existing form infrastructure and trigger the matching algorithm via events.

---

## Files to Create

### 1. CreateLostRequestHandler
**Path:** `src/main/java/op/edu/ua/petbed/telegram/form/handler/CreateLostRequestHandler.java`

**Purpose:** Handle completion of CREATE_LOST_REQUEST form for pet owners

**Dependencies to Inject:**
- `LostRequestService`
- `PetService` (for updating special features)
- `UserService` (for user lookup)

**Form Steps Mapping:**
| Step | Method | Field | Required |
|------|--------|-------|----------|
| 0 | `data.text(steps.get(0))` | Contact Info | ✅ Yes |
| 1 | `data.location(steps.get(1))` | Last Seen Location | ✅ Yes |
| 2 | `data.textOrNull(steps.get(2))` | Special Features | ❌ No |

**Business Logic:**
1. Extract `petId` from `data.entityId()` (passed via `startUpdateForm`)
2. Get contact info (Step 0)
3. Get location Point (Step 1)
4. If special features provided (Step 2 not null/blank):
   - Call `petService.updateSpecialFeatures(petId, specialFeatures)`
5. Create lost request: `lostRequestService.create(petId, contactInfo, location)`
6. Return success message with back button

**Response Pattern:**
```java
return ResponseBuilder.sendMessage(data.chatId())
    .text("✅ Пошук запущено! Ми повідомимо вас про можливі збіги.")
    .keyboard(InlineKeyboardBuilder.builder()
            .backButtonTo(data.returnCallback())
            .build())
    .build();
```

**Annotations:**
- `@Slf4j`
- `@Component`
- `@NullMarked`
- `@RequiredArgsConstructor`

---

### 2. CreateFoundRequestHandler
**Path:** `src/main/java/op/edu/ua/petbed/telegram/form/handler/CreateFoundRequestHandler.java`

**Purpose:** Handle completion of CREATE_FOUND_REQUEST form for finders

**Dependencies to Inject:**
- `FoundRequestService`
- `UserService` (for user lookup)

**Form Steps Mapping:**
| Step | Method | Field | Required | Notes |
|------|--------|-------|----------|-------|
| 0 | `data.choice(steps.get(0))` | Pet Type | ✅ Yes | Parse: `PetType.valueOf(choice.toUpperCase())` |
| 1 | `data.photo(steps.get(1))` | Photo | ✅ Yes | Telegram file_id |
| 2 | `data.location(steps.get(2))` | Location | ✅ Yes | Point (lat, lon) |
| 3 | `data.textOrNull(steps.get(3))` | Breed | ❌ No | |
| 4 | `data.textOrNull(steps.get(4))` | Color | ❌ No | |
| 5 | `data.textOrNull(steps.get(5))` | Coat Type | ❌ No | |
| 6 | `data.choiceOrNull(steps.get(6))` | Sex | ❌ No | Parse: `PetSex.valueOf(choice.toUpperCase())` if not null |
| 7 | `data.choiceOrNull(steps.get(7))` | Size | ❌ No | Parse: `PetSize.valueOf(choice.toUpperCase())` if not null |
| 8 | `data.textOrNull(steps.get(8))` | Special Features | ❌ No | |

**Business Logic:**
1. Get finder ID: `userService.findById(data.userId()).id()`
2. Get pet type from Step 0 (parse enum)
3. Get photo URL from Step 1
4. Get location Point from Step 2
5. Build description by aggregating Steps 3-8:
   ```java
   StringBuilder sb = new StringBuilder();
   for (int i = 3; i <= 8; i++) {
       String value = data.textOrNull(steps.get(i));
       if (value != null && !value.isBlank()) {
           sb.append(value).append(" ");
       }
   }
   String description = sb.toString().trim();
   ```
6. Create found request: `foundRequestService.create(finderId, photoUrl, petType, location, description)`
7. Return success message with two buttons

**Response Pattern:**
```java
return ResponseBuilder.sendMessage(data.chatId())
    .text("✅ Анкету збережено!\n\nМи знайшли потенційних власників для цієї тварини.")
    .keyboard(InlineKeyboardBuilder.builder()
            .addButton("🔍 Переглянути рекомендації", CallbackId.LOST_FOUND_MATCHES)
            .addButton("⬅️ Повернутись", CallbackId.MENU)
            .build())
    .build();
```

**Critical Warning to User:**
The success message should include a warning: "Якщо ви натиснете 'Повернутись', переглянути анкети буде неможливо"

---

## Form Type Definitions Required

### FormType Enum Updates
**File:** `src/main/java/op/edu/ua/petbed/telegram/form/scheme/FormType.java`

Need to add two new form types:

```java
CREATE_LOST_REQUEST(List.of(
        FormStep.text("📞 Введіть контактну інформацію для зв'язку з вами"),
        FormStep.location("📍 Надішліть геолокацію останнього місця, де бачили тварину"),
        FormStep.text("📝 Введіть особливі прикмети тварини (допоможе у пошуку)").optional()
)),

CREATE_FOUND_REQUEST(List.of(
        FormStep.choice("🐾 Оберіть тип тварини", List.of(PetType.values())),
        FormStep.photo("📷 Надішліть фото знайденої тварини"),
        FormStep.location("📍 Надішліть геолокацію знахідки"),
        FormStep.text("✏️ Вкажіть породу (якщо відомо)").optional(),
        FormStep.text("🎨 Вкажіть колір").optional(),
        FormStep.text("🧥 Вкажіть тип окрасу").optional(),
        FormStep.choice("⚥ Оберіть стать", List.of(PetSex.values())).optional(),
        FormStep.choice("📏 Оберіть розмір", List.of(PetSize.values())).optional(),
        FormStep.text("📝 Опишіть особливі ознаки").optional()
));
```

---

## Prerequisites Checklist

Before implementing Stage 9, ensure these are completed:

### ✅ Domain Layer (Completed in Stages 2-4)
- [x] `LostRequest`, `FoundRequest` entities
- [x] `LostRequestDTO`, `FoundRequestDTO`
- [x] `PetType`, `PetSex`, `PetSize` enums

### ✅ Service Layer (Completed in Stage 5)
- [x] `LostRequestService` / `LostRequestServiceImpl` with `create()` method
- [x] `FoundRequestService` / `FoundRequestServiceImpl` with `create()` method
- [x] `PetService` with `updateSpecialFeatures(petId, features)` method

### ✅ Form Infrastructure (Existing)
- [x] `FormService` with `startCreateForm()` and `startUpdateForm()`
- [x] `FormSubmissionHandler` interface
- [x] `FormType`, `FormStep`, `FormInput` classes
- [x] `FormData` record with all accessor methods
- [x] `ResponseBuilder` for building responses
- [x] `InlineKeyboardBuilder` for building keyboards

### ✅ Telegram Infrastructure (Existing)
- [x] `CallbackId` enum with LOST_FOUND_MATCHES, MENU values
- [x] `UserService` for user lookups

---

## Implementation Steps

### Step 1: Add Form Types
Update `FormType.java` with CREATE_LOST_REQUEST and CREATE_FOUND_REQUEST definitions

### Step 2: Create CreateLostRequestHandler
Implement handler following the pattern from existing handlers (AddPetHandled, CreateFeedPostHandler)

### Step 3: Create CreateFoundRequestHandler  
Implement handler with description aggregation logic

### Step 4: Write Unit Tests
Write tests following existing patterns (AddPetHandledTest)

### Step 5: Verify Integration
Run all tests and verify forms work end-to-end

---

## Testing Strategy

### Unit Tests for CreateLostRequestHandler
**Test class:** `CreateLostRequestHandlerTest`

1. `handle_withValidData_createsLostRequest()`
2. `handle_withSpecialFeatures_updatesPet()` - verify petService.updateSpecialFeatures called
3. `handle_withoutSpecialFeatures_skipsPetUpdate()` - verify petService not called
4. `handle_returnsSuccessMessageWithBackButton()`
5. `handle_entityIdNull_throwsPetBedException()`

### Unit Tests for CreateFoundRequestHandler
**Test class:** `CreateFoundRequestHandlerTest`

1. `handle_withValidData_createsFoundRequest()`
2. `handle_buildsDescriptionFromTextFields()` - verify description aggregation
3. `handle_skipsEmptyFieldsInDescription()` - verify null/blank fields excluded
4. `handle_withAllOptionalFields_buildsCompleteDescription()`
5. `handle_returnsSuccessMessageWithRecommendationsButton()`
6. `handle_parsesEnumValuesCorrectly()` - verify PetType, PetSex, PetSize parsing

---

## Key Implementation Patterns

### Pattern 1: Handler Class Structure
```java
@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
public class CreateLostRequestHandler implements FormSubmissionHandler {

    private final LostRequestService lostRequestService;
    private final PetService petService;
    private final UserService userService;

    @Override
    public FormType getFormType() {
        return FormType.CREATE_LOST_REQUEST;
    }

    @Override
    public BotApiMethod<?> handle(FormData data) {
        log.debug("Processing CREATE_LOST_REQUEST form: {}", data);
        // ... implementation
    }
}
```

### Pattern 2: Form Data Extraction
```java
List<FormStep> steps = FormType.CREATE_LOST_REQUEST.steps();

// Required fields
String contactInfo = data.text(steps.get(0));
var location = data.location(steps.get(1));

// Optional fields
String specialFeatures = data.textOrNull(steps.get(2));
```

### Pattern 3: Conditional Business Logic
```java
if (specialFeatures != null && !specialFeatures.isBlank()) {
    petService.updateSpecialFeatures(petId, specialFeatures);
}
```

### Pattern 4: Description Aggregation
```java
StringBuilder sb = new StringBuilder();
for (int i = 3; i <= 8; i++) {
    String value = data.textOrNull(steps.get(i));
    if (value != null && !value.isBlank()) {
        sb.append(value).append(" ");
    }
}
String description = sb.toString().trim();
```

### Pattern 5: Response Building
```java
return ResponseBuilder.sendMessage(data.chatId())
        .text("✅ Success message")
        .keyboard(InlineKeyboardBuilder.builder()
                .backButtonTo(data.returnCallback())
                .build())
        .build();
```

---

## Dependencies Summary

| Handler | Services Required | Form Steps | Response Type |
|---------|------------------|------------|---------------|
| CreateLostRequestHandler | LostRequestService, PetService, UserService | 3 (2 required, 1 optional) | Back button only |
| CreateFoundRequestHandler | FoundRequestService, UserService | 9 (3 required, 6 optional) | Recommendations + Back buttons |

---

## Notes

1. **Entity ID Passing:** CREATE_LOST_REQUEST uses `startUpdateForm()` to pass petId via `data.entityId()`
2. **No Entity for CREATE_FOUND_REQUEST:** CREATE_FOUND_REQUEST uses `startCreateForm()` (no entity ID needed)
3. **Event Triggering:** Both handlers trigger async matching via service layer events
4. **Warning Message:** CREATE_FOUND_REQUEST success message must warn about permanent unavailability of recommendations if user clicks "Back"
5. **Null Safety:** All handlers must be annotated with `@NullMarked`

---

## Next Stage After Completion

**Stage 10: Telegram Callback Handlers**
- LOST_START, LOST_SELECT_PET, LOST_ADD_PET handlers
- LOST_ACTIVE, LOST_ACTIVE_DETAIL handlers
- LOST_RECOMMENDATIONS handler (for owners)
- LOST_FOUND_MATCHES handler (for finders)
