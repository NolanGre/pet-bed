# Adoption Callback Handlers — Refactoring Plan

> План рефакторингу хендлерів модуля adoption для досягнення консистентності з Pet модулем.

## Аналіз поточного стану

### ✅ Вже виправлено (відповідають Pet патерну)

| Хендлер | Що добре |
|----------|----------|
| `AdoptionGiveCallbackHandler` | `paginatedList` + `navButtonsFor` + `backButtonFor` |
| `AdoptionMyPostsCallbackHandler` | `paginatedList` + `backButtonFor` |
| `AdoptionPostDetailOwnerCallbackHandler` | `navButtonsFor` з predicate |
| `AdoptionResponseDetailOwnerCallbackHandler` | `navButtonsFor` з predicate |

### ❌ Потребують виправлення

| Хендлер | Проблема | Рішення |
|----------|---------|----------|
| `AdoptionGetCallbackHandler` | `addButton("➡️ Наступна", offset)` — ручна кнопка замість пагінації | Використати `CallbackId.ADOPTION_GET` як parent для "Наступна" в callbackData |
| `AdoptionResponsesCallbackHandler` | `responses.forEach(addButton)` — виводить список цілком | `paginatedList` через offset |
| `AdoptionMyResponsesCallbackHandler` | Плейсхолдер, немає реалізації | Реалізувати з пагінацією |

---

## Референс: Pet Модуль (правильний патерн)

### MyPetsCallbackHandler
```java
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
var result = petService.findAllByOwnerId(userId, PageRequest.of(offset, pageSize));

private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<PetDTO> result) {
    return InlineKeyboardBuilder.builder()
            .paginatedList(toPageDto(result), context.callbackData())
            .navButtonsFor(getCallbackId())
            .backButtonFor(getCallbackId())
            .build();
}
```

### PetDetailCallbackHandler
```java
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

---

## План виправлень

### Фаза 1: AdoptionGetCallbackHandler

**Проблема:** Використовує ручну кнопку:
```java
.addButton("➡️ Наступна", CallbackId.ADOPTION_GET, (long) offset + 1)
```

**Рішення:** Використати "Наступна" як дочірній елемент в `navButtonsFor`:
```java
// В CallbackId додати:
// ADOPTION_GET_NEXT(625, "➡️ Наступна", ADOPTION_GET)

// В хендлері:
private InlineKeyboardMarkup buildKeyboard(AdoptionRecommendationDTO post, int offset) {
    // Створюємо новий callbackData з збільшеним offset
    var nextOffset = new CallbackData(CallbackId.ADOPTION_GET, post.postId(), (long) offset + 1);
    
    return InlineKeyboardBuilder.builder()
            .navButtonsFor(CallbackId.ADOPTION_GET, post.postId(), child -> {
                if (child == CallbackId.ADOPTION_SAVE_POST) return !post.isSaved();
                if (child == CallbackId.ADOPTION_UNSAVE_POST) return post.isSaved();
                if (child == CallbackId.ADOPTION_GET_NEXT) return true;  // Завжди показуємо
                return false;
            })
            .backButtonFor(CallbackId.ADOPTION)
            .build();
}
```

### Фаза 2: AdoptionResponsesCallbackHandler

**Проблема:** Виводить всі відгуки списком:
```java
responses.forEach(response -> {
    builder.addButton(label, CallbackId.ADOPTION_RESPONSE_SINGLE, response.id());
});
```

**Рішення:** Пагінація через offset:
```java
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
Page<AdoptionResponseDTO> responses = adoptionResponseService.findByPostId(postId, PageRequest.of(offset, pageSize));

private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionResponseDTO> responses) {
    return InlineKeyboardBuilder.builder()
            .paginatedList(toPageDto(responses), context.callbackData())
            .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
            .build();

private Page<CallbackListItem> toPageDto(Page<AdoptionResponseDTO> responses) {
    return responses.map(response -> new CallbackListItem(
            CallbackId.ADOPTION_RESPONSE_SINGLE,
            response.id(),
            response.getStatusEmoji() + " " + response.responderUsername()
    ));
}
```

### Фаза 3: AdoptionMyResponsesCallbackHandler

**Проблема:** Плейсхолдер без реалізації

**Рішення:** Реалізувати з пагінацією:
```java
int offset = callbackData.offset() != null ? callbackData.offset() : 0;
Page<AdoptionResponseDTO> responses = adoptionResponseService.findByResponderId(userId,
        PageRequest.of(offset, pageSize));

private InlineKeyboardMarkup buildKeyboard(CallbackQueryContext context, Page<AdoptionResponseDTO> responses) {
    return InlineKeyboardBuilder.builder()
            .paginatedList(toPageDto(responses), context.callbackData())
            .backButtonFor(CallbackId.ADOPTION)
            .build();

private Page<CallbackListItem> toPageDto(Page<AdoptionResponseDTO> responses) {
    return responses.map(response -> new CallbackListItem(
            CallbackId.ADOPTION_RESPONSE_DETAIL,
            response.id(),
            response.getStatusEmoji() + " " + response.postPetName()
    ));
}
```

---

## Потрібні зміни в сервісах

| Сервіс | Метод | Зміна |
|--------|-------|-------|
| `AdoptionResponseService` | `findByPostId(postId, Pageable)` | Додати `Pageable` параметр |
| `AdoptionResponseService` | `findByResponderId(userId, Pageable)` | Додати новий метод |

---

## CallbackId зміни

| Дія | CallbackId | Parent |
|-----|-----------|--------|
| Наступна | `ADOPTION_GET_NEXT` | `ADOPTION_GET` |

---

## Виконано

- [x] AdoptionGiveCallbackHandler — paginatedList
- [x] AdoptionMyPostsCallbackHandler — paginatedList  
- [x] AdoptionPostDetailOwnerCallbackHandler — navButtonsFor з predicate
- [x] AdoptionResponseDetailOwnerCallbackHandler — navButtonsFor з predicate
- [x] AdoptionGetCallbackHandler — refactor (Фаза 1)
- [x] AdoptionResponsesCallbackHandler — refactor (Фаза 2)
- [ ] AdoptionMyResponsesCallbackHandler — реалізувати (Фаза 3)

---

*Оновлено: 2026-05-08*