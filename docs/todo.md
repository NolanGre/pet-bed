# PET_DETAIL Implementation Plan

## Overview
Callback handler for viewing pet detail (one pet card with photo).

## Callback Data Structure
- `callbackId`: PET_DETAIL (32)
- `entityId`: Long (pet ID, required)
- `offset`: null (single entity, not paginated)

## Implementation

### 1. PetDetailCallbackHandler

**Location:** `src/main/java/op/edu/ua/petbed/telegram/callback/handler/pet/PetDetailCallbackHandler.java`

**handle():**
1. Get entityId from callbackData
2. Fetch PetDTO from service
3. Map to response with `mapToResponse()`

**mapToResponse():**
1. Format pet info text via `formatPetInfo(pet)`
2. Build keyboard with action buttons
3. Return SendMessage with photo + text + keyboard

### 2. PetDTO - Self-Describing Behavior (DDD)

PetDTO should format itself — no external formatting logic in handler.

**New method in PetDTO:**
```java
public String formatInfo() {
    // Returns formatted card string:
    // 🐾 Ім'я
    // 🐩 Порода: breed
    // 🎨 Колір: color + colorPattern
    // 🎂 Вік: X years (or 1 year)
    // 📏 Розмір: size (small/medium/large)
    // 🏷️ Статус: status
    // 📝 Особливі прикмети: specialMarks
}
```

**Age formatting:**
- 1 рік → "1 рік"
- X років → "X років"

### 3. Response Structure

```
SendMessage with:
- photo: pet.photoId()
- text: pet.formatInfo()
- keyboard: action buttons
```

### 4. Conditional Action Buttons

| Button | Condition |
|--------|-----------|
| PET_UPDATE | `pet.status() == PetStatus.DEFAULT` |
| PET_DELETE | `pet.status().canDelete()` |

### 5. PetStatus - Business Logic (DDD)

PetStatus already has `canUpdate()` and `canDelete()` methods.

**Status emoji mapping:**
- DEFAULT → "available"
- IN_LOST → "searching"
- IN_ADOPTION → "adoption"
- IN_FOSTERING → "fostering"
- FOSTERED → "fostered"

---

## Tasks

- [ ] Add `formatInfo()` method to PetDTO (DDD)
- [ ] Create PetDetailCallbackHandler
- [ ] Implement handle() - fetch pet, handle not found
- [ ] Build response with photo + text + keyboard
- [ ] Add conditional action buttons
- [ ] Run tests
- [ ] Write unit tests

---

## Edge Cases

1. **Pet not found** → `PetBedException(ErrorCode.PET_NOT_FOUND)`
2. **Status DEFAULT** → show PET_UPDATE button
3. **Status != DEFAULT** → hide PET_UPDATE button
4. **canDelete = false (FOSTERED)** → hide PET_DELETE button