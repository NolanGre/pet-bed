# Тест План: Виправлення тестів після рефакторингу

## Проблема

Після рефакторингу (зміна API з `telegramId` на `internal userId`, додавання `UserAuthContext` контексту) — 48 помилок компіляції.

---

## Аналіз зламаних тестів

### 1. FormServiceTest.java (14 помилок)

**api змінився:**
| Старе | Нове |
|-------|------|
| `processInput(input, userId)` | `processInput(input, userId, chatId)` |
| `confirmForm(userId)` | `confirmForm(userId, fallbackChatId)` |
| `cancelForm(userId)` | `cancelForm(userId, fallbackChatId)` |

**Що робити:** Оновити виклики методів — додати `fallbackChatId` (456L) параметр

**Тесткейси:** Залишаються, бо бізнес-логіка не змінилась

---

### 2. CommandContextTest.java (2 помилки)

**api змінився:**
| Старе | Нове |
|-------|------|
| `CommandContext.from(update, command)` | `CommandContext.from(update, command, authContext)` |
| `context.userId()` | `context.userAuthContext().userTelegramId()` |

**Що робити:** 
1. Додати `UserAuthContext authContext = new UserAuthContext(123L, 123L, UserType.REGULAR, "testuser")` перед викликом
2. Змінити `assertThat(result.userId())` → `assertThat(result.userAuthContext().userTelegramId())`

**Тесткейси:** Актуальні, змінились лише API

---

### 3. CallbackQueryContextTest.java (3 помилки)

**api змінився:**
| Старе | Нове |
|-------|------|
| `CallbackQueryContext.from(update)` | `CallbackQueryContext.from(update, authContext)` |

**Що робити:** Додати `UserAuthContext` до всіх викликів `.from(update)`

**Тесткейси:** Актуальні

---

### 4. TelegramUpdateRouterImplTest.java (1 помилка)

**api змінився:**
```java
// Було
new UserAuthContext(1L, UserType.REGULAR)
// Потрібно
new UserAuthContext(telegramId, internalId, userType, username)
```

**Що робити:** Оновити конструктор на 4 параметри

**Тесткейс:** Залишається

---

### 5. ProfileHandlerTest.java (1 помилка)

**api змінився:**
| Старе | Нове |
|-------|------|
| `context.userId()` | `context.userAuthContext().userTelegramId()` |

**Що робити:** Оновити виклик

**Тесткейс:** Актуальний

---

## Файли що потребують оновлення

| Файл | Проблема | Дія |
|------|----------|-----|
| FormServiceTest.java | processInput/confirmForm/cancelForm | Оновити виклики |
| CommandContextTest.java | authContext not defined, userId() | Додати authContext |
| CallbackQueryContextTest.java | CallbackQueryContext.from() | Додати authContext |
| TelegramUpdateRouterImplTest.java | UserAuthContext constructor | Оновити конструктор |
| ProfileHandlerTest.java | context.userId() | Оновити на userAuthContext |

---

## Тесткейси що залишаються актуальними

- Всі бізнес-сценарії FormServiceTest (процес форми, валідація)
- FormEntityTest — логіка не змінилась
- FormInputTest — логіка не змінилась
- FormStepTest — логіка не змінилась
- FormTypeTest — оновити кількість кроків (3 → 10)

---

## Run

```bash
./gradlew test > test.log 2>&1
```

---

## Status

- [ ] FormServiceTest — оновити виклики методів
- [ ] CommandContextTest — додати authContext
- [ ] CallbackQueryContextTest — додати authContext
- [ ] TelegramUpdateRouterImplTest — оновити конструктор
- [ ] ProfileHandlerTest — оновити виклик userId
- [ ] FormTypeTest — оновити кількість кроків

## Нові класи без тестів (потрібно додати)

| Клас | Опис | Пріоритет |
|------|------|-----------|
| PetServiceImpl | CRUD for Pet | high |
| PetRepository | Data access | high |
| MyPetsCallbackHandler | Список улюбленців | high |
| AddPetCallbackHandler | Додавання тварини | medium |
| FormData | Доступ до відповідей | medium |