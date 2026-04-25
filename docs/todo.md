# Telegram Bot API — Міграція на новий підхід з deliver()

## Поточний стан

Поточна архітектура не відповідає гайдлайну:
- **Роутер** повертає `BotApiMethod<?>` (тільки webhook)
- **Хендлери** використовують `telegramClient.execute()` напряму для медіа
- **Handler інтерфейс** повертає `BotApiMethod<?>`

## Цільовий стан

- **Роутер** повертає `PartialBotApiMethod<?>`
- Хендлери **не використовують** `telegramClient.execute()` напряму
- Роутер має метод `deliver()` що вирішує як відправити відповідь
- Handler інтерфейс повертає `PartialBotApiMethod<?>`

---

## План міграції

### 1. Інтерфейси хендлерів

#### 1.1 CallbackHandler
- Змінити `BotApiMethod<?> handle()` → `PartialBotApiMethod<?> handle()`
- Додати імпорт `PartialBotApiMethod`

#### 1.2 CommandHandler  
- Змінити `BotApiMethod<?> handle()` → `PartialBotApiMethod<?> handle()`
- Додати імпорт `PartialBotApiMethod`

### 2. Роутер

#### 2.1 TelegramUpdateRouterImpl
- Додати `TelegramClient` як залежність
- Додати метод `deliver(PartialBotApiMethod<?> response)`:
  ```java
  private void deliver(PartialBotApiMethod<?> response) throws TelegramApiException {
      if (response instanceof BotApiMethod<?> m) {
          // webhook return — нічого не робимо
          return;
      }
      // медіа — execute напряму
      if (response instanceof SendPhoto p)        telegramClient.execute(p);
      if (response instanceof SendDocument d)     telegramClient.execute(d);
      if (response instanceof SendVideo v)        telegramClient.execute(v);
      if (response instanceof SendLocation l)    telegramClient.execute(l);
      if (response instanceof EditMessageMedia e)  telegramClient.execute(e);
      // ...
  }
  ```
- У методі `route()` викликати `deliver()` після отримання відповіді від хендлера

#### 2.2 ReturnCallback (AnswerCallbackQuery)
- Роутер повинен автоматично відповідати на callback query
- Хендлер **не повинен** повертати `AnswerCallbackQuery`
- Додати автоматичну відповідь: `AnswerCallbackQuery.builder().callbackQueryId(context.callbackQuery().getId()).build()`

### 3. ResponseBuilder

#### 3.1 Методи для медіа
Додати методи для створення медіа-повідомлень:
- `sendPhoto(chatId, photoUrl)` → returns `SendPhoto`
- `editPhoto(chatId, messageId, photoId)` → returns `EditMessageMedia`
- `sendLocation(chatId, latitude, longitude)` → returns `SendLocation`
- `sendVenue(chatId, latitude, longitude, title, address)` → returns `SendVenue`
- `sendContact(chatId, phoneNumber, firstName)` → returns `SendContact`

#### 3.2 Існуючі методи
- Залишити `sendMessage()`, `editMessage()`, `answerCallback()` як є

### 4. Хендлери для оновлення

#### 4.1 PetDetailCallbackHandler
**Було:**
```java
telegramClient.execute(photo);  //.execute() напряму
return AnswerCallbackQuery.builder()...build();
```
**Має бути:**
```java
return ResponseBuilder.editPhoto(chatId, messageId, photoId())
    .caption(..)
    .keyboard(..)
    .build();
```

#### 4.2 FormEnumSelectCallbackHandler
**Було:**
```java
telegramClient.execute(EditMessageText...);  //execute() напряму
```
**Ма�� бути:**
```java
return ResponseBuilder.editMessage(chatId, messageId)
    .text(..)
    .keyboard(..)
    .build();
```

#### 4.3 MyPetsCallbackHandler
**Було:**
```java
telegramMessageService.editOrReplace(context.messageId(), message);
return AnswerCallbackQuery.builder()...build();
```
**Має бути:**
```java
// Видалити виклик editOrReplace
// Використовувати respondVia() або
return ResponseBuilder.sendMessage(chatId)
    .text(..)
    .keyboard(..)
    .build();
```

### 5. TelegramMessageService

#### 5.1 Зміни
- Залишити `editOrReplace()` для сумісності з випадками коли HTTP edit неможливий
- Можливо видалити прямі виклики `execute()` в інших методах

### 6. Документація

#### 6.1 routing-guide.md
Оновити секцію "Response Method Types":
- Пояснити ієрархію `PartialBotApiMethod<?>`
- Додати приклад `deliver()` методу
- Пояснити правило: коли webhook, коли execute()

#### 6.2 ui-response-mapping.md
- Змінити `BotApiMethod<?>` → `PartialBotApiMethod<?>`
- Додати приклади для sendPhoto, editPhoto

---

## Файли для зміни

| Файл | Що змінити |
|------|-----------|
| `CallbackHandler.java` | Інтерфейс: BotApiMethod → PartialBotApiMethod |
| `CommandHandler.java` | Інтерфейс: BotApiMethod → PartialBotApiMethod |
| `TelegramUpdateRouterImpl.java` | Додати deliver() + TelegramClient |
| `ResponseBuilder.java` | Додати методи для медіа |
| `PetDetailCallbackHandler.java` | Return замість execute() |
| `FormEnumSelectCallbackHandler.java` | Return замість execute() |
| `MyPetsCallbackHandler.java` | Видалити editOrReplace |
| `TelegramMessageService.java` | Опціонально: видалити зайві execute() |
| `docs/routing-guide.md` | Оновити документацію |
| `docs/ui-response-mapping.md` | Оновити документацію |

---

## Порядок виконання

1. **Інтерфейси** (1.1, 1.2) — база
2. **ResponseBuilder** (3.1, 3.2) — інструменти
3. **Роутер** (2.1, 2.2) — ядро
4. **Хендлери** (4.1, 4.2, 4.3) — споживачі
5. **Документація** (6.1, 6.2) — після коду

---

## Критерії успіху

- **Пріоритет — webhook return**: завжди повертаємо відповідь через webhook (метод `route()` повертає `BotApiMethod<?>`)
- **Execute() — тільки як fallback**: якщо метод НЕ підтримує webhook (медіа, файли, геолокація) — роутер викликає `telegramClient.execute()`
- Жоден хендлер **не використовує** `telegramClient.execute()` напряму
- Жоден хендлер **не повертає** `AnswerCallbackQuery` (роутер додає автоматично)
- Роутер має метод `deliver()` що обробляє всі типи відповідей