# Adoption Telegram Handlers — Refactoring Plan

> План рефакторингу хендлерів модуля adoption для досягнення консистентності з Pet модулем.

## Проблеми

| Проблема | Приклад | Рішення |
|----------|---------|----------|
| Ручне створення кнопок замість `paginatedList` | `AdoptionGiveCallbackHandler` — `pets.forEach(addButton())` | Використати `paginatedList(toPageDto(pets), context.callbackData())` |
| Ручне додавання кнопок замість `navButtonsFor` | `AdoptionGetCallbackHandler` — `addButton("✉️ Відгукнутись", ...)` | Використати `navButtonsFor(ADOPTION_GET, postId, predicate)` |
| Дублювання коду клавіатури | `AdoptionGetCallbackHandler` + `AdoptionGetNextHandler` мають 90% однакового коду | Винести в окремий метод |
| `ADOPTION_GET_NEXT` — окремий CallbackId | Використовує окремий хендлер з ручною логікою offset | Видалити, використовувати пагінацію як в Pet (offset в callbackData) |
| Різні способи навігації | `backButtonFor` vs `backButtonTo` використовуються непередбачувано | `backButtonFor` — основний, `backButtonTo` — тільки для специфічних випадків |

---

## Референс: Pet Модуль (правильний патерн)

### MyPetsCallbackHandler
```java
// Пагінація через offset в callbackData
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
var result = petService.findAllByOwnerId(userId, PageRequest.of(offset, pageSize));

// Клавіатура через paginatedList + navButtonsFor + backButtonFor
private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> result) {
    return InlineKeyboardBuilder.builder()
            .paginatedList(toPageDto(result), context.callbackData())
            .navButtonsFor(getCallbackId())
            .backButtonFor(getCallbackId())
            .build();
}

// Трансформація в CallbackListItem
private Page<CallbackListItem> toPageDto(Page<PetDTO> result) {
    return result.map(pet -> new CallbackListItem(CallbackId.PET_DETAIL, pet.id(), pet.name()));
}
```

### PetDetailCallbackHandler
```java
// Умовні кнопки через navButtonsFor з predicate
private InlineKeyboardMarkup actionKeyboard(PetDTO pet) {
    return InlineKeyboardBuilder.builder()
            .navButtonsFor(CallbackId.PET_DETAIL, pet.id(), child -> {
                if (child == CallbackId.PET_UPDATE) return pet.status() == PetStatus.DEFAULT;
                if (child == CallbackId.PET_DELETE) return pet.status().canDelete();
                return false;
            })
            .backButtonFor(CallbackId.PET_DETAIL)
            .build();
}
```

### FeedViewNextCallbackHandler (історія повідомлень)
```java
// Видаляє клавіатуру з попереднього повідомлення
tryToDeleteKeyboardInMessage(context);
// Показує нове повідомлення
return ResponseBuilder.sendPhoto(...).keyboard(
    InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.FEED_VIEW)
        .backButtonTo(CallbackId.FEED)
        .build()
).build();
```

---

## План Рефакторингу

### Фаза 1: Видалити ADOPTION_GET_NEXT (найважливіше)

**Проблема:** `ADOPTION_GET_NEXT` — окремий CallbackId з окремим хендлером, що дублює логіку з `AdoptionGetCallbackHandler`.

**Рішення:**
1. Видалити `AdoptionGetNextHandler.java` — він більше не потрібен
2. Змінити `AdoptionGetCallbackHandler` — використовувати offset з callbackData як в Pet
3. При натисканні "Наступна" — збільшуємо offset на 1

**Після:**
```java
// AdoptionGetCallbackHandler.java
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
AdoptionRecommendationDTO post = adoptionPostService.findNextForFeed(userId, offset);

// Клавіатура
return InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.ADOPTION_GET, post.postId(), child -> {
            // Фільтри для відгукнутись / зберегти / видалити
        })
        .backButtonFor(CallbackId.ADOPTION_GET)
        .build();
```

---

### Фаза 2: Рефакторинг списків — використання paginatedList

#### 2.1 AdoptionGiveCallbackHandler
**Було:**
```java
pets.forEach(pet -> builder.addButton(pet.name(), CallbackId.ADOPTION_SELECT_PET, pet.id()));
builder.addButton("➕ Додати нову тварину", CallbackId.ADOPTION_ADD_PET);
builder.backButtonFor(CallbackId.ADOPTION);
```

**Має бути:**
```java
// Потрібна пагінація з offset
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
Page<PetDTO> pets = petService.findPetsAvailableForLostSearch(userId, PageRequest.of(offset, pageSize));

return InlineKeyboardBuilder.builder()
        .paginatedList(toPageDto(pets), context.callbackData())
        .navButtonsFor(getCallbackId())  // "Додати тварину" — дочірній елемент
        .backButtonFor(getCallbackId())
        .build();
```

#### 2.2 AdoptionMyPostsCallbackHandler
**Було:**
```java
posts.forEach(post -> builder.addButton(statusEmoji + " " + post.petName(), ...));
builder.backButtonFor(CallbackId.ADOPTION);
```

**Має бути:**
```java
// Пагінація
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
Page<AdoptionPostDTO> posts = adoptionPostService.findAllByOwnerId(userId, PageRequest.of(offset, pageSize));

return InlineKeyboardBuilder.builder()
        .paginatedList(toPageDto(posts), context.callbackData())
        .backButtonFor(getCallbackId())
        .build();

// Трансформація з emoji
private Page<CallbackListItem> toPageDto(Page<AdoptionPostDTO> posts) {
    return posts.map(post -> new CallbackListItem(
        CallbackId.ADOPTION_POST_DETAIL,
        post.id(),
        getStatusEmoji(post.status()) + " " + post.petName()
    ));
}
```

#### 2.3 AdoptionResponsesCallbackHandler
**Було:**
```java
responses.forEach(response -> builder.addButton(label, ...));
```

**Має бути:**
```java
// Пагінація відгуків
return InlineKeyboardBuilder.builder()
        .paginatedList(toPageDto(responses), context.callbackData())
        .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
        .build();
```

---

### Фаза 3: Рефакторинг кнопок дій — використання navButtonsFor з predicate

#### 3.1 AdoptionResponseDetailOwnerCallbackHandler
**Було:**
```java
switch (response.status()) {
    case NEW -> {
        builder.addButton("✅ Підтвердити", ...);
        builder.addButton("❌ Відхилити", ...);
    }
    case REJECTED_BY_OWNER -> builder.addButton("🔄 Відновити", ...);
    // ...
}
```

**Має бути:**
```java
return InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.ADOPTION_RESPONSE_SINGLE, responseId, child -> {
            return switch (response.status()) {
                case NEW -> child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_CONFIRM
                         || child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_REJECT;
                case REJECTED_BY_OWNER -> child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_RESTORE;
                default -> false;
            };
        })
        .backButtonTo(CallbackId.ADOPTION_RESPONSES_LIST)
        .build();
```

#### 3.2 AdoptionPostDetailOwnerCallbackHandler
**Було:**
```java
if (post.status() == ACTIVE && !responses.isEmpty()) {
    builder.addButton("📨 Переглянути відгуки", ...);
}
if (post.status() != COMPLETED) {
    builder.addButton("🗑️ Скасувати оголошення", ...);
}
```

**Має бути:**
```java
return InlineKeyboardBuilder.builder()
        .navButtonsFor(CallbackId.ADOPTION_POST_DETAIL, postId, child -> {
            if (child == CallbackId.ADOPTION_RESPONSES_LIST) {
                return post.status() == ACTIVE && !responses.isEmpty();
            }
            if (child == CallbackId.ADOPTION_POST_TRY_DELETE) {
                return post.status() != COMPLETED;
            }
            return false;
        })
        .backButtonFor(CallbackId.ADOPTION_POST_DETAIL)
        .build();
```

---

### Фаза 4: Винести дублювання клавіатур в окремі методи

#### 4.1 Створити допоміжний клас/метод для adoption клавіатур

```java
@Component
public class AdoptionKeyboardHelper {

    public InlineKeyboardMarkup feedKeyboard(AdoptionRecommendationDTO post, Long postId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION_GET, postId, child -> {
                    if (child == CallbackId.ADOPTION_RESPONSE_CREATE) return true;
                    if (child == CallbackId.ADOPTION_SAVE_POST) return !post.isSaved();
                    if (child == CallbackId.ADOPTION_UNSAVE_POST) return post.isSaved();
                    return child == CallbackId.ADOPTION_GET_NEXT;  // Потрібно додати дочірній
                })
                .backButtonFor(CallbackId.ADOPTION_GET)
                .build();
    }

    public InlineKeyboardMarkup postDetailKeyboard(AdoptionPostDTO post, List<AdoptionResponseDTO> responses, Long postId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION_POST_DETAIL, postId, child -> {
                    // Умовна логіка
                })
                .backButtonFor(CallbackId.ADOPTION_POST_DETAIL)
                .build();
    }
}
```

---

### Фаза 5: CallbackId — додати відсутні лейбли

**Проблема:** Деякі кнопки додаються через `addButton` бо не мають лейблів в CallbackId.

**Рішення:** Додати лейбли для кнопок стрічки:
```java
ADOPTION_RESPONSE_CREATE(622, "✉️ Відгукнутись", ADOPTION_GET),
ADOPTION_SAVE_POST(623, "❤️ Зберегти", ADOPTION_GET),
ADOPTION_UNSAVE_POST(624, "💔 Видалити зі збережених", ADOPTION_GET),
ADOPTION_GET_NEXT(625, "➡️ Наступна", ADOPTION_GET),  // Додати як дочірній!
```

---

## Файли для оновлення

| Хендлер | Дії |
|---------|-----|
| `AdoptionGetNextHandler.java` | **ВИДАЛИТИ** — більше не потрібен |
| `AdoptionGetCallbackHandler.java` | Використовувати offset + navButtonsFor |
| `AdoptionGiveCallbackHandler.java` | paginatedList + navButtonsFor |
| `AdoptionMyPostsCallbackHandler.java` | paginatedList + backButtonFor |
| `AdoptionResponsesCallbackHandler.java` | paginatedList + backButtonTo |
| `AdoptionResponseDetailOwnerCallbackHandler.java` | navButtonsFor з predicate |
| `AdoptionPostDetailOwnerCallbackHandler.java` | navButtonsFor з predicate |

---

## Очікувані результати

| Метрика | До | Після |
|---------|-----|-------|
| `addButton` виклики | ~25 | ~3 |
| Дублювання коду | Високе | Мінімальне |
| Консистентність API | Низька | Висока |
| Кількість хендлерів | 21 | 20 (-1) |

---

## Порядок виконання

1. **Фаза 1** — Видалити ADOPTION_GET_NEXT + оновити AdoptionGetCallbackHandler
2. **Фаза 2** — Рефакторинг списків (Give, MyPosts, Responses)
3. **Фаза 3** — Рефакторинг кнопок дій (Response Detail, Post Detail)
4. **Фаза 4** — Винести дублювання (опціонально)
5. **Фаза 5** — CallbackId лейбли (якщо потрібно)

---

*Дата: 2026-05-08*