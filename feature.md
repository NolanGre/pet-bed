### Контекст
- **Проєкт:** Spring Telegram Bot
- **Залежності які вже є в проєкті:**
    - Spring Boot `4.0.3`
    - Java `25`
    - Spring Web, Actuator, Data JPA, Validation
    - Spring Modulith `2.0.3` (core, jpa, actuator, observability)
    - Liquibase (DB міграції)
    - PostgreSQL driver
    - Hibernate Spatial + JTS Core `1.19.0` (геопросторові дані)
    - Lombok
    - Telegram Bot — `telegrambots-springboot-webhook-starter:9.4.0` + `telegrambots-client:9.4.0`
    - AWS S3 SDK BOM `2.42.9`
    - Micrometer + Prometheus
    - Testcontainers BOM `2.0.3` (junit-jupiter, postgresql)
    - Spring Boot Test + Spring Boot Testcontainers
    - Spring Modulith Test
    - JUnit Platform Launcher

### Тест-кейси (TDD — спочатку тести)

#### ✅ Happy path
- [ ] [Що саме перевіряємо при коректних даних]

#### ❌ Edge cases / негативні сценарії
- [ ] [null / порожній input]
- [ ] [невалідні дані]
- [ ] [граничні значення]

#### 🔁 Взаємодія із залежностями (моки)
- Що мокається: [Repository / Service / TelegramBot API / etc.]
- Що має бути викликано: `verify(mock).method(args)`
- Що НЕ має бути викликано: `verifyNoMoreInteractions(mock)`

---

### Структура тесту (шаблон)
```java
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class [ClassName]Test {

    @Mock
    [Dependency] dependency;

    @InjectMocks
    [ClassUnderTest] underTest;

    @Test
    @DisplayName("[Що саме тестується — людською мовою]")
    void [methodName]_[scenario]_[expectedResult]() {
        // given
        
        // when
        
        // then
    }
}
```

На кожен метод пишуться тести та відокремлюються блоками через коментар:

```java
// .methodName() -------------------------------------------------
```

---

### Що реалізувати (після тестів)
1. Клас: `[ClassName]`
2. Метод: `[returnType] methodName([params])`
3. Анотації Spring якщо потрібні: `@Service` / `@Component` / etc.

---

- Для створення домених сутностей використовувати фабричні методи у сутностях.
- Усі гетери щщо не треба, зроблені PROTECTED
- Весь код та коментарі пишуться англійською.
- Використовувати JSpecify, щоб уберегтись від null. (Якщо треба, анотувати клас через `@NullMarked` тощо)

---

### Виключення

Використовуємо власне виключення `public class PetBedException extends RuntimeException` яке використовує `ErrorCode`.
Конструктор для виключень:
```java
public PetBedException(String message, ErrorCode errorCode) {
  super(message);
  this.errorCode = errorCode;
}
```
Якщо потрібно створити свій `PetBedException.ErrorCode` роби його за прикладом уже створених:
```java
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  UNSUPPORTED_UPDATE("This type of update is not supported"),
  INTERNAL_ERROR("Something went wrong. Please try again later");

  private final String userMessage;
}
```

---

### Обмеження
- [ ] Працювати пиключно через DDD та TDD
- [ ] Не додавати фічі яких немає в завданні
- [ ] Не змінювати існуючі тести
- [ ] Не змінювати інші класи якщо не вказано явно
- [ ] Реалізація має проходити **тільки** написані тести

Запам'ятай це все та чекай моїх вказівок