# План: Реалізація функціоналу зміни профілю

## Мета
Створити 3 коллбек-хендлери в модулі `callback/handler` для зміни типу профілю користувача.

## Завдання

### 1. Розширити InlineKeyboardBuilder ✅ ВИКОНАНО
**Файл**: `src/main/java/op/edu/ua/petbed/telegram/response/InlineKeyboardBuilder.java`

Додати перевантажений метод `navButtonsFor` з параметром `entityId`:
```java
public InlineKeyboardBuilder navButtonsFor(CallbackId currentCallbackId, Long entityId)
```
Це дозволить передавати `entityId` в автоматично згенеровані кнопки навігації.

---

### 2. (Пропущено — UserService вже існує)

---

### 3. Створити ProfileCallbackHandler (PROFILE: 110) ✅ ВИКОНАНО + ТЕСТИ
**Новий файл**: `src/main/java/op/edu/ua/petbed/telegram/callback/handler/profile/ProfileCallbackHandler.java`
**Тести**: `src/test/java/.../callback/handler/profile/ProfileCallbackHandlerTest.java`

- **Trigger**: Натискання кнопки "👤 Профіль"
- **Поведінка**:
    - Виводить повідомлення з інформацією про користувача:
        - Тип акаунту (REGULAR/VOLUNTEER)
        - Юзернейм
    - Кнопка "Змінити тип профілю" → PROFILE_CHANGE_TYPE (111)
    - Кнопка "⬅️ Повернутись" → MENU (батьківський)

---

### 4. Створити ProfileChangeTypeHandler (PROFILE_CHANGE_TYPE: 111) ✅ ВИКОНАНО + ТЕСТИ
**Новий файл**: `src/main/java/op/edu/ua/petbed/telegram/callback/handler/profile/ProfileChangeTypeHandler.java`
**Тести**: `src/test/java/.../callback/handler/profile/ProfileChangeTypeHandlerTest.java`

- **Trigger**: Натискання кнопки "Змінити тип профілю"
- **Поведінка**:
    - Виводить інформацію про типи профілів:
        - REGULAR — звичайний користувач
        - VOLUNTEER — волонтер (для волонтерів доступні додаткові функції)
        - Пояснення: "Ви можете змінити тип акаунту. Волонтери можуть створювати оголошення та керувати тваринами."
    - Кнопка "✓ Підтвердження зміни типу" → PROFILE_CHANGE_TYPE_CONFIRM (112)
        - містить `entityId` для ідентифікації користувача
    - Кнопка "⬅️ Повернутись" → PROFILE

---

### 5. Створити ProfileChangeTypeConfirmHandler (PROFILE_CHANGE_TYPE_CONFIRM: 112) ✅ ВИКОНАНО + ТЕСТИ
**Новий файл**: `src/main/java/op/edu/ua/petbed/telegram/callback/handler/profile/ProfileChangeTypeConfirmHandler.java`
**Тести**: `src/test/java/.../callback/handler/profile/ProfileChangeTypeConfirmHandlerTest.java`

- **Trigger**: Натискання кнопки "✓ Підтвердження зміни типу"
- **Поведінка**:
    - Отримує поточний тип користувача з БД
    - Викликає метод toggle() для зміни типу (REGULAR ↔ VOLUNTEER)
    - Виводить повідомлення з підтвердженням:
        - "✅ Тип акаунту змінено на {НОВИЙ_ТИП}"
    - Кнопка "⬅️ Профіль" → PROFILE (110)

---

## Залежності

- `UserService` — для отримання та оновлення даних користувача
- `TelegramAuthService` — для аутентифікації
- `InlineKeyboardBuilder` — для створення клавіатур
- `CallbackQueryContext` — для доступу до даних коллбеку

## Примітка

CallbackId вже визначені в `CallbackId.java`:
- PROFILE(110, "👤 Профіль", MENU)
- PROFILE_CHANGE_TYPE(111, "Змінити тип профілю", PROFILE)
- PROFILE_CHANGE_TYPE_CONFIRM(112, "✓ Підтвердження зміни типу", PROFILE_CHANGE_TYPE)
