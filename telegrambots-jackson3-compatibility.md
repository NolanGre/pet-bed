# Несумісність telegrambots 9.x із Jackson 3 (Spring Boot 4)

## Контекст

При інтеграції Telegram-бота у Spring Boot 4 застосунок з використанням бібліотеки
`org.telegram:telegrambots-springboot-webhook-starter:9.4.0` було виявлено критичну
несумісність на рівні серіалізації/десеріалізації JSON.

## Суть проблеми

Spring Boot 4 використовує **Jackson 3.x** (`tools.jackson.*`) як основну бібліотеку
для роботи з JSON, тоді як `telegrambots 9.x` написана під **Jackson 2.x**
(`com.fasterxml.jackson.*`).

Бібліотека `telegrambots` використовує Lombok-анотацію `@Jacksonized` у поєднанні з
`@SuperBuilder` для генерації Jackson-сумісних конструкторів:

```java
// org.telegram.telegrambots.meta.api.objects.User
@SuperBuilder
@Jacksonized
public class User implements BotApiObject { ... }
```

Анотація `@Jacksonized` під капотом генерує:
```java
@JsonDeserialize(builder = User.UserBuilder.class)
```

де `@JsonDeserialize` належить простору імен `com.fasterxml.jackson.databind.annotation` — тобто **Jackson 2.x**.

Коли Spring Boot 4 отримує webhook-запит від Telegram, він використовує свій
`tools.jackson.ObjectMapper` для десеріалізації тіла запиту. Цей маппер **не розпізнає**
анотації з простору імен `com.fasterxml.*`, бачить клас `User` без жодних Creator-ів і
кидає виняток:

```
InvalidDefinitionException: Cannot construct instance of
`org.telegram.telegrambots.meta.api.objects.User`
(no Creators, like default constructor, exist)
```

## Технічна причина

| | Jackson 2.x | Jackson 3.x |
|---|---|---|
| Пакет | `com.fasterxml.jackson.*` | `tools.jackson.*` |
| Spring Boot | 2.x / 3.x | 4.x |
| `@Jacksonized` (Lombok 1.18.x) | ✅ сумісний | ❌ несумісний |

Два Jackson-и співіснують у classpath одночасно — Jackson 2.x підтягується як
транзитивна залежність `telegrambots`, Jackson 3.x — як залежність Spring Boot 4.

## Статус у бібліотеці

На момент написання (березень 2026) у репозиторії `rubenlagus/TelegramBots` існує
відкритий PR з міграцією на Jackson 3, який ще **не змержений**. PR передбачає:

- Заміну всіх імпортів `com.fasterxml.jackson.*` на `tools.jackson.*`
- Апгрейд Lombok до `edge-SNAPSHOT` для сумісного `@Jacksonized`
- Заміну `ObjectMapper` на `JsonMapper` (Jackson 3 еквівалент)

Це свідчить про те, що **офіційної підтримки Jackson 3 у telegrambots 9.x немає**.

## Застосований Workaround

Оскільки клас `Update` та всі вкладені об'єкти (`User`, `Message`, `Chat` тощо)
коректно десеріалізуються саме через `com.fasterxml.jackson.ObjectMapper`,
було застосовано підміну `HttpMessageConverter` на рівні Spring MVC:

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        com.fasterxml.jackson.databind.ObjectMapper fasterxmlMapper =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Ставимо com.fasterxml конвертер першим у черзі
        converters.add(0, new MappingJackson2HttpMessageConverter(fasterxmlMapper));
    }
}
```

Spring Boot перебирає конвертери по черзі і використовує перший підходящий.
`MappingJackson2HttpMessageConverter` обгортає саме `com.fasterxml.ObjectMapper`,
тому анотації `@Jacksonized` розпізнаються коректно.

Глобальний `tools.jackson.ObjectMapper` Spring Boot 4 залишається незміненим
і використовується для всіх інших ендпоінтів.

## Висновок

Дана несумісність є прикладом **транзитивного конфлікту залежностей** у екосистемі
JVM, де дві бібліотеки покладаються на різні мажорні версії спільної залежності.
Ситуація ускладнюється тим, що Jackson 3.x змінив базовий пакет (`tools.jackson`
замість `com.fasterxml.jackson`), що унеможливлює прозору сумісність через
стандартні механізми розв'язання версій (Gradle/Maven обирають лише одну версію,
але тут це два **різні артефакти** з різними package names).
