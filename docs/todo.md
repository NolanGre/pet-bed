# Тест План: Form Architecture

## Фокус

FormService повертає `BotApiMethod<?>` (Telegram повідомлення), не кидає винятки на бізнес-помилки.
Тестуємо **бізнес логіку**, не JPA/ Lombok (вони працюють з коробки).

---

## FormServiceTest

```java
@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    FormRepository formRepository;

    @Mock
    FormSubmissionHandler handler;

    @InjectMocks
    FormService underTest;
}
```

| Тест | Сценарій | Перевірка |
|------|----------|-----------|
| `startForm_new_returnsPromptWithEntity_saved` | Нова форма | entity saved + message contains first step prompt |
| `startForm_existing_deletesOldAndCreates` | Існує форма | delete called, new entity saved |
| `processInput_validInput_savesAndReturns_nextOrComplete` | Валідний ввід | entity saved + message contains next prompt OR "форма завершена" |
| `processInput_invalidInput_returnsErrorMessage` | Інвалідний ввід | message contains validation error |
| `processInput_noActiveForm_returnsNoActiveFormMessage` | Немає активної форми | message: "немає активних форм" |
| `confirmForm_complete_invokesHandlerAndReturnsResult` | Форма завершена | handler called, entity deleted, result returned |
| `confirmForm_incomplete_returnsFormNotCompleteMessage` | Форма неповна | message: "форма ще не заповнена" |
| `confirmForm_noActiveForm_returnsNoActiveFormMessage` | Немає активної форми | message: "немає активних форм" |
| `confirmForm_noHandler_throws` | Немає handler для типу | PetBedException |
| `cancelForm_active_deletesAndReturnsCancelled` | Є активна форма | entity deleted + message with back button |
| `cancelForm_noActiveForm_returnsNoActiveFormMessage` | Немає активної форми | message: "немає активних форм" |
| `hasActiveForm_exists_true` | Форма існує | true |
| `hasActiveForm_notExists_false` | Форма не існує | false |

---

## FormEntityTest

```java
class FormEntityTest { }
```

| Тест | Сценарій | Очікуваний результат |
|------|----------|---------------------|
| `initiate_setsAllFields` | → Все поля встановлені |
| `nextStep_whenComplete_throws` | Форма завершена | PetBedException |
| `nextStep_returnsNextStep` | Є незаповнені кроки | FormStep |
| `isComplete_allFilled_true` | Всі кроки заповнені | true |
| `isComplete_notAll_false` | Не всі заповнені | false |
| `applyStep_savesInput` | → rawSteps оновлено |

---

## FormInputTest

```java
class FormInputTest { }
```

| Тест | Сценарій | Очікуваний результат |
|------|----------|---------------------|
| `from_withPhoto_returnsPhoto` | Message.hasPhoto()=true | Photo з правильним fileId |
| `from_withLocation_returnsLocation` | Message.hasLocation()=true | Location з lat/lng |
| `from_withText_returnsText` | Message.hasText()=true, text="hello" | Text("hello") |
| `from_withTextBlank_throws` | Message.hasText()=true, text=" " | PetBedException |
| `from_withTextNull_throws` | Message.hasText()=true, text=null | PetBedException |
| `from_unsupportedType_sticker_throws` | Message тільки sticker | PetBedException |

---

## FormStepTest

```java
class FormStepTest { }
```

| Тест | Сценарій | Очікуваний результат |
|------|----------|---------------------|
| `validate_nonBlankText_withValidText_true` | Text("name") | true |
| `validate_nonBlankText_withBlank_false` | Text(" ") | false |
| `validate_nonBlankText_withWrongType_false` | Photo("id") | false |
| `validate_nonBlankPhoto_withValidPhoto_true` | Photo("fileId") | true |
| `validate_nonBlankPhoto_withBlank_false` | Photo("") | false |
| `validate_nonBlankPhoto_withWrongType_false` | Text("name") | false |
| `validate_anyLocation_withLocation_true` | Location(50.4, 30.5) | true |
| `validate_anyLocation_withWrongType_false` | Text("50.4,30.5") | false |

---

## FormTypeTest

```java
class FormTypeTest { }
```

| Тест | Сценарій | Очікуваний результат |
|------|----------|---------------------|
| `addPet_stepsCount_is3` | FormType.ADD_PET | 3 |
| `addPet_step0_isName` | Перший крок | Text input |
| `addPet_step1_isPhoto` | Другий крок | Photo input |
| `addPet_step2_isLocation` | Третій крок | Location input |

---

## CallbackIdTest (new logic)

```java
class CallbackIdTest { }
```

| Тест | Сценарій | Очікуваний результат |
|------|----------|---------------------|
| `isPaginated_withPaginatedParent_true` | CallbackId з дітьми що мають пустий label | true |
| `isPaginated_withoutChildren_false` | CallbackId без дітей | false |

---

## SubmitFormHandlerTest

```java
@ExtendWith(MockitoExtension.class)
class SubmitFormHandlerTest { }
```

| Тест | Сценарій | Перевірка |
|------|----------|-----------|
| `getCommand_returnsSubmitForm` | → Command.SUBMIT_FORM |
| `handle_confirmFormCalled` | → formService.confirmForm called |

---

## CancelFormHandlerTest

```java
@ExtendWith(MockitoExtension.class)
class CancelFormHandlerTest { }
```

| Тест | Сценарій | Перевірка |
|------|----------|-----------|
| `getCommand_returnsCancelForm` | → Command.CANCEL_FORM |
| `handle_cancelFormCalled` | → formService.cancelForm called |

---

## Coverage Targets

| Клас | Ціль |
|------|------|
| FormService | 90%+ (всі методи) |
| FormInput | 100% |
| FormStep | 100% |
| FormType | 100% (тільки one test) |

---

## Run

```bash
./gradlew test > test.log 2>&1
```

---

## Status

- [x] Unit tests FormService
- [x] Unit tests FormEntity
- [x] Unit tests FormInput
- [x] Unit tests FormStep
- [x] Unit tests FormType
- [x] Unit tests CallbackId
- [x] Unit tests SubmitFormHandler
- [x] Unit tests CancelFormHandler
- [x] Run all tests (198 passed)