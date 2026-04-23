# Form Architecture

## Overview

Form — механізм покрокового заповнення даних користувачем у Telegram bot. Telegram не зберігає стан, сервер не знає що
вводить користувач — форма вирішує ці проблеми.

---

## Концепція

Два шари:
- **Схема** — незмінний шаблон форми. Визначає кроки, підказки, валідатори.
- **Модель** — носій даних. Зберігає відповіді користувача, знає поточний крок, надає інформацію для handler-а.

```
FormType / FormStep  →  шаблон (що запитати, як валідувати)
FormEntity           →  стан   (що вже заповнено, що далі)
```

---

## Схема

### FormInput
Sealed interface — можливі типи вводу користувача.

```java
FormInput.Text(String value)
FormInput.Photo(String fileId)
FormInput.Location(double latitude, double longitude)
```

Конвертація з Telegram Message:
```java
FormInput.from(Message message)  // автоматично визначає тип
```

### FormStep
Один крок форми. Містить промпт, type-tag і валідатор.

```java
FormStep(String prompt, FormInput input, Predicate<FormInput> validator)
```

Вбудовані валідатори:
```java
FormStep.NON_BLANK_TEXT   // i -> i instanceof FormInput.Text(String v) && !v.isBlank()
FormStep.NON_BLANK_PHOTO  // i -> i instanceof FormInput.Photo(String id) && !id.isBlank()
FormStep.ANY_LOCATION    // i -> i instanceof FormInput.Location
```

Приклад:
```java
new FormStep("✏️ Введіть ім'я тварини", FormInput.TEXT, FormStep.NON_BLANK_TEXT)
```

### FormType
Enum форм. Кожна форма — впорядкований список кроків.

```java
FormType.ADD_PET(List.of(
        new FormStep("✏️ Введіть ім'я тварини", FormInput.TEXT, NON_BLANK_TEXT),
        new FormStep("📷 Надішліть фото тварини", FormInput.PHOTO, NON_BLANK_PHOTO),
        new FormStep("📍 Надішліть локацію", FormInput.LOCATION, ANY_LOCATION)
));

// Метод для отримання списку кроків
FormType.steps()  // List<FormStep>
```

---

## Модель

### FormEntity
JPA entity. Зберігає відповіді користувача у `rawSteps` (jsonb).

**Таблиця:** `user_form_states`

**Поля:**
- `telegram_id` (PK) — ID користувача Telegram
- `chat_id` — ID чату
- `form_type` — типFormType
- `return_callback` — CallbackId куди повернутись після скасування
- `raw_steps` (JSONB) — Map<Integer, String> де ключ = номер кроку

FormEntity надає API для роботи зі станом форми. 

**Методи:**
```java
// Повертає схему (FormType)
FormType scheme() 

// Повертає наступний незаповнений крок
FormStep nextStep()  

// Чи всі кроки заповнені
boolean isComplete()  

// Зберігає відповідь для поточного кроку
void applyStep(FormInput input)  

// Отримати ChatId
Long getChatId()

// Отримати ReturnCallback
CallbackId getReturnCallback()
```

---

## Флоу

```
[ініціація]
formService.startForm(FormType.ADD_PET, CallbackId.MY_PETS, userId, chatId)
→ FormService.create entity → returns first step prompt

[крок 1]
formService.processInput(FormInput.Text("Барсик"), userId)
→ validate → save to rawSteps → returns next prompt

[крок 2]
formService.processInput(FormInput.Photo("AgAC..."), userId)
→ validate → save to rawSteps → returns next prompt

[крок 3]
formService.processInput(FormInput.Location(50.45, 30.52), userId)
→ validate → save → isComplete() = true → returns confirmation message

[підтвердження]
User sends /submit
→ formService.confirmForm(userId)
→ handler.handle(entity) → returns success message → delete form

[скасування]
User sends /cancel
→ formService.cancelForm(userId)
→ delete form → returns message with back button
```

---

## Handler

### FormSubmissionHandler
Інтерфейс для обробки даних після завершення форми.

```java
public interface FormSubmissionHandler {
    FormType getFormType();                        // Який тип форми обробляє
    BotApiMethod<?> handle(FormEntity entity);    // Обробка даних
}
```

Приклад реалізації:
```java
@Component
public class AddPetHandled implements FormSubmissionHandler {

    @Override
    public FormType getFormType() {
        return FormType.ADD_PET;
    }

    @Override
    public BotApiMethod<?> handle(FormEntity formEntity) {
        // Отримання даних з formEntity
        // Map<Integer, String> rawSteps = ... (або через метод доступу)
        
        // Бізнес-логіка
        
        return ResponseBuilder.telegram()
                .chatId(formEntity.getChatId())
                .text("Тварину успішно додано")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(formEntity.getReturnCallback())
                        .build())
                .build();
    }
}
```

---

## FormService

Основний сервіс керування формами.

```java
@Service
public class FormService {

    // Почати нову форму
    BotApiMethod<?> startForm(FormType type, CallbackId returnCallback, Long telegramUserId, Long chatId)

    // Обробити ввід користувача
    BotApiMethod<?> processInput(FormInput input, Long telegramUserId)

    // Підтвердити форму
    BotApiMethod<?> confirmForm(Long telegramUserId)

    // Скасувати форму
    BotApiMethod<?> cancelForm(Long telegramUserId)

    // Перевірити чи є активна форма
    boolean hasActiveForm(Long telegramUserId)
}
```

---

## Router (TelegramUpdateRouter)

Роутер автоматично перевіряє наявність активної форми:

```java
private BotApiMethod<?> handleMessage(Update update) {
    Long userId = message.getFrom().getId();
    
    if (formService.hasActiveForm(userId)) {
        // Конвертація повідомлення у FormInput
        FormInput input = FormInput.from(message);
        return formService.processInput(input, userId);
    }
    
    return defaultMessageResponse(chatId);
}
```

Пріоритет обробки:
1. Команди (`/start`, `/menu`, ...) — завжди обробляються як команди
2. Форми — якщо є активна форма, весь ввід йде в неї
3. Звичайні повідомлення — якщо немає форми і не команда

---

## Ключові принципи

| | |
|---|---|
| `startForm()` | Створює нову форму, повертає перший prompt |
| `processInput()` | Валідує ввід, зберігає, повертає наступний prompt |
| `confirmForm()` | Викликає handler, видаляє форму |
| `cancelForm()` | Видаляє форму, повертає назад до returnCallback |
| `FormInput.from()` | Автоматично визначає тип з Telegram Message |
| Таймскіп | Весь стан в БД, FormEntity відновлюється з rawSteps |

---

## Як додати нову форму

### Крок 1: Додати FormType
```java
FormType.MY_NEW_FORM(List.of(
        new FormStep("Питання 1", FormInput.TEXT, NON_BLANK_TEXT),
        new FormStep("Питання 2", FormInput.PHOTO, NON_BLANK_PHOTO)
));
```

### Крок 2: Створити FormSubmissionHandler
```java
@Component
public class MyNewFormHandler implements FormSubmissionHandler {

    @Override
    public FormType getFormType() {
        return FormType.MY_NEW_FORM;
    }

    @Override
    public BotApiMethod<?> handle(FormEntity formEntity) {
        // Обробка даних
    }
}
```

### Крок 3: Зареєструвати в callback handler
```java
// Відповідний CallbackHandler
public BotApiMethod<?> handle(CallbackQueryContext context) {
    return formService.startForm(
            FormType.MY_NEW_FORM,
            CallbackId.PARENT,
            context.userId(),
            context.chatId()
    );
}
```

---

## Повідомлення

### Під час заповнення
```
✏️ Введіть ім'я тварини

ℹ️ Для скасування форми /cancel
```

### Після завершення
```
✅ Ви завершили заповнення форми!

✏️ Напишіть /submit щоб надіслати
🗑️ Або /cancel щоб скасувати
```

### При валідаційній помилці
```
ℹ️ Очікується текст. Надішліть повідомленням.
ℹ️ Очікується фото. Надішліть фото.
ℹ️ Очікується геолокація. Надішліть геолокацію.
```

### При скасуванні
```
🗑️ Форму скасовано
[Кнопка повернення]
```