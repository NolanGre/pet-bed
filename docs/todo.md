# Telegram Bot UI Navigation Architecture - Рефакторинг

## Status: 📋 In Progress

---

## Проблема

Поточна архітектура використовує `CallbackAction` (рядки) замість ієрархічної системи `CallbackId`. Потрібно переробити на нову архітектуру з:
- Централізованим зберіганням ієрархії UI у вигляді дерева (enum)
- CallbackData що містить лише callback_id + entity_id + offset
- Автоматичною побудовою кнопок навігації з enum

---

## Архітектура

### CallbackId Enum
Централізоване зберігання ієрархії UI у вигляді дерева:
- `id` — числовий ідентифікатор скріна
- `parent` — посилання на батька (для кнопки "Повернутись")
- `label` — назва кнопки для користувача

### CallbackData
Містить лише:
- `callbackId` — ідентифікатор скріна (число)
- `entityId` — опційно, ID сутності (тварина, пост тощо)
- `offset` — опційно, для пагінації

**Формат:** `callbackId,entityId,offset` з пропусками (порожні значення = null)
```
123,111,222  → callbackId=123, entityId=111, offset=222
123,,2       → callbackId=123, entityId=null, offset=2
123,111,     → callbackId=123, entityId=111, offset=null
123          → callbackId=123
```

### InlineKeyboardBuilder
Додати методи для побудови навігації:
- `.navButtonsFor(CallbackId)` — кнопки нащадків
- `.backButtonFor(CallbackId)` — кнопка "Повернутись"
- `.putEntityToChild(CallbackId, entityId)` — вшити entityId в callback нащадка
- `.putEntityToCurrent(CallbackId, entityId)` — вшити entityId в поточний callback
- `.pagination(CallbackId, offset, totalPages)` — кнопки пагінації

---

## План

### Етап 1: Основа

- [ ] 1. Створити `CallbackId` enum з ієрархією
  - Поля: `id`, `parent`, `label`
  - Метод: `children()` — повертає нащадків
  - Валідація: перевірка на цикли
- [ ] 2. Модифікувати `CallbackData`
  - Поля: `callbackId` (int), `@Nullable Long entityId`, `@Nullable Integer offset`
  - Формат: `callbackId,entityId,offset` (komm separated, порожні = null)
  - Factory: `of(CallbackId, entityId, offset)`
  - Парсинг: `parse(String)` з `split(",")`
- [ ] 3. Оновити `CallbackQueryContext`
  - Методи: `callbackId()`, `entityId()`, `offset()`

### Етап 2: InlineKeyboardBuilder

- [ ] 4. Додати методи навігації до `InlineKeyboardBuilder`
  - `.navButtonsFor(CallbackId)`
  - `.backButtonFor(CallbackId)`
  - `.putEntityToChild(CallbackId, Long)`
  - `.putEntityToCurrent(CallbackId, Long)`
  - `.pagination(CallbackId, int offset, int totalPages)`

### Етап 3: Хендлери

- [ ] 5. Оновити `CallbackHandler` інтерфейс
  - `CallbackId getCallbackId()` замість `CallbackAction`
  - `BotApiMethod<?> handle(CallbackQueryContext context)`
- [ ] 6. Оновити `TelegramUpdateRouterImpl`
  - Маршрутизація за `CallbackId`
- [ ] 7. Видалити `CallbackAction` enum (або залишити для CONFIRM/CANCEL)

### Етап 4: Тестування

- [ ] 8. Написати unit тести для нових компонентів

### Етап 5: Візуалізація графа

- [ ] 9. Створити ендпоінт `/api/callback-graph`
  - Повертає Mermaid діаграму з текстовими label (не ID)
  - Формат: `graph TD\n MENU["Меню"] --> SEARCH["Пошук"]\n SEARCH --> FOUND_PET["Я знайшов тварину"]`
  - Посилання для перегляду: `https://mermaid.live/edit#pako{BASE64}`

---

## Примітка

> Повний список CallbackId дивись у `docs/callback-tree.md`

---

## Виконано

- [x] Створено план рефакторингу
