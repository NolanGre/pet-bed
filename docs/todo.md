# Adoption Feed — Правильна логіка перегляду анкет

> Формалізовані вимоги та план реалізації.
> Дата: 2026-05-08

---

## Вимоги до перегляду анкет

### 1. Основні принципи

| Принцип | Опис |
|---------|------|
| **Редагування** | Анкети редагують попереднє повідомлення (`EditMessageText`/`EditMessageMedia`), НЕ відправляють новим повідомленням |
| **Історія** | Історія перегляду зберігається в таблиці `adoption_view_history` (вже існує) |
| **User offset** | У сутності User є поле offset для навігації "назад" |
| **No callback offset** | Offset в callbackData НЕ використовується |

### 2. Поведінка кнопок

| Кнопка | Дія |
|--------|-----|
| **Повернутись** | Повернення до меню адопції |
| **Наступна** | Показати наступну анкету, `user.offset = 0` |
| **Минула** | Показати попередню анкету з історії за `user.offset`, потім `user.offset++` |
| **Зберегти** | Зберегти анкету в обране |
| **Відгукнутись** | Відкрити форму відгуку |

### 3. Логіка "Наступна"

```
1. Користувач натискає "Наступна"
2. user.offset = 0
3. Знайти наступну непереглянуту анкету (з урахуванням offset=0)
4. Якщо є → редагувати повідомлення з новою анкетою
5. Якщо немає → показати повідомлення "Наразі немає анкет, спробуйте пізніше"
```

### 4. Логіка "Минула"

```
1. Користувач натискає "Минула"
2. Взяти запис з adoption_view_history за index = user.offset (0 = останній перегляд)
3. Отримати postId з цього запису
4. Знайти анкету за postId
5. user.offset++ (зміститись на ще один запис назад)
6. Редагувати повідомлення з попередньою анкетою
```

### 5. Коли анкет немає

```
Якщо при "Наступна" немає анкет:
→ Показати повідомлення: "Наразі немає доступних анкет. Спробуйте пізніше!"
→ Клавіатура: тільки "Повернутись"
```

### 6. Структура клавіатури кожної анкети

```
[ Зберегти ] [ Відгукнутись ]
[ ◀️ Минула ] [ ➡️ Наступна ]
[ ⬅️ Повернутись ]
```

---

## План реалізації

### Етап 1: Перевірити/додати поле offset в User

- [ ] Перевірити чи є поле `adoptionHistoryOffset` в сутності User
- [ ] Якщо немає — додати міграцію

### Етап 2: Оновити AdoptionGetCallbackHandler

**Поточний стан (неправильно):**
- Відправляє новим повідомленням (SendPhoto)
- Використовує offset в callbackData
- Видаляє клавіатуру з попереднього повідомлення

**Потрібний стан (правильно):**
- Використовує `ResponseBuilder.editPhoto()` або `ResponseBuilder.editMessage()`
- Кнопка "Наступна" → `ADOPTION_GET_NEXT` callback (який обробляється тим самим хендлером)
- Кнопка "Минула" → `ADOPTION_GET_PREV` callback
- НЕ використовує offset з callbackData

### Етап 3: Додати сервісні методи

- [ ] `AdoptionPostService.findById(postId)` — знайти анкету за ID
- [ ] `AdoptionPostService.findNextUnviewed(userId)` — знайти наступну непереглянуту
- [ ] `UserService.updateAdoptionOffset(userId, offset)` — оновити offset

### Етап 4: Реалізувати логіку "Наступна"

```java
// ADOPTION_GET_NEXT handler (або в тому самому хендлері через callbackId)
public PartialBotApiMethod<?> handleNext(CallbackQueryContext context) {
    Long userId = context.auth().userInternalId();
    userService.updateAdoptionOffset(userId, 0);  // скинути offset

    AdoptionRecommendationDTO post = adoptionPostService.findNextUnviewed(userId);
    if (post == null) {
        return noMorePostsMessage(context);
    }

    adoptionPostService.recordView(post.postId(), userId);
    return editCurrentMessage(context, post);
}
```

### Етап 5: Реалізувати логіку "Минула"

```java
// ADOPTION_GET_PREV handler
public PartialBotApiMethod<?> handlePrev(CallbackQueryContext context) {
    Long userId = context.auth().userInternalId();
    int currentOffset = userService.getAdoptionOffset(userId);

    // Взяти запис з історії за offset (0 = останній перегляд)
    AdoptionViewHistory history = adoptionViewHistoryRepository
            .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(currentOffset, 1));

    if (history.isEmpty()) {
        return noHistoryMessage(context);
    }

    // Отримати post і показати
    Long postId = history.getContent().get(0).getPostId();
    AdoptionRecommendationDTO post = adoptionPostService.findById(postId);

    userService.updateAdoptionOffset(userId, currentOffset + 1);
    return editCurrentMessage(context, post);
}
```

### Етап 6: Оновити клавіатуру

```java
private InlineKeyboardMarkup buildKeyboard(AdoptionRecommendationDTO post) {
    return InlineKeyboardBuilder.builder()
            .addButton("❤️ Зберегти", CallbackId.ADOPTION_SAVE_POST, post.postId())
            .addButton("✉️ Відгукнутись", CallbackId.ADOPTION_RESPONSE_CREATE, post.postId())
            .row()
            .addButton("◀️ Минула", CallbackId.ADOPTION_GET_PREV, post.postId())
            .addButton("➡️ Наступна", CallbackId.ADOPTION_GET_NEXT, post.postId())
            .row()
            .backButtonTo(CallbackId.ADOPTION)
            .build();
}
```

---

## CallbackId зміни

| Кнопка | CallbackId | Parent |
|--------|------------|--------|
| Наступна | `ADOPTION_GET_NEXT` | `ADOPTION_GET` |
| Минула | `ADOPTION_GET_PREV` | `ADOPTION_GET` |

---

## Поточний статус

- [ ] Етап 1: Перевірити/додати поле offset в User
- [ ] Етап 2: Оновити AdoptionGetCallbackHandler
- [ ] Етап 3: Додати сервісні методи
- [ ] Етап 4: Реалізувати логіку "Наступна"
- [ ] Етап 5: Реалізувати логіку "Минула"
- [ ] Етап 6: Оновити клавіатуру

---

*Оновлено: 2026-05-08*