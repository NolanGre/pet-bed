# Adoption Module — Implementation Plan

> Детальний план реалізації модуля адопції (передачі тварин).
> Створено на основі аналізу вимог та архітектурного проєктування.

> ⚠️ **Примітка:** Існуючі тести в проєкті неактуальні (помилки компіляції в `FoundRequestDTO`). Нові тести створено як заглушки з `@Disabled` — їх ігнорувати при збірці.

---

## 1. Огляд модуля

Модуль **Adoption** з'єднує власників тварин з охочими їх отримати через систему анкет та відгуків з двостороннім підтвердженням.

### Основні сутності
- **AdoptionPost** — анкета передачі тварини (створюється власником)
- **AdoptionResponse** — відгук на анкету (створюється охочим)
- **AdoptionSavedPost** — збережені анкети користувача
- **AdoptionViewHistory** — історія переглянутих анкет

### Ключові бізнес-процеси
1. Власник створює анкету → тварина отримує статус FOR_ADOPTION
2. Охочі переглядають анкети та залишають відгуки
3. Власник підтверджує відгук → інші автоматично відхиляються
4. Охочий фінально підтверджує → тварина змінює власника

---

## 2. Статуси та правила

### AdoptionPostStatus
| Статус | Опис |
|--------|------|
| ACTIVE | Активна анкета, очікує відгуків |
| PENDING_CONFIRMATION | Власник підтвердив охочого, очікує фіналу |
| COMPLETED | Передача завершена успішно |
| CANCELLED | Анкета скасована власником |

### AdoptionResponseStatus
| Статус | Смайлик | Опис |
|--------|---------|------|
| NEW | 🆕 | Новий відгук |
| CONFIRMED_BY_OWNER | ⏳ | Підтверджений власником |
| REJECTED_BY_OWNER | ❌ | Відхилений власником |
| FINAL_CONFIRMED | ✅ | Фінально підтверджений охочим |
| CANCELLED | 🚫 | Скасований охочим |

### Бізнес-правила
- **R-1:** Одна тварина = одна активна анкета (unique на pet_id)
- **R-2:** Один користувач = один відгук на анкету
- **R-3:** При підтвердженні одного відгуку — інші автоматично відхиляються
- **R-4:** Охочий інформується про передачу контакту при створенні відгуку
- **R-5:** Власник бачить username охочого тільки після створення відгуку
- **R-6:** Анкети зі статусом COMPLETED/CANCELLED не видаляються з БД

---

## 3. Фази реалізації

### Фаза 1: Database Schema (Liquibase)

#### 1.1 Таблиця adoption_posts
```sql
CREATE TABLE adoption_posts (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    pet_id BIGINT NOT NULL UNIQUE REFERENCES pets(id) ON DELETE CASCADE,
    owner_comment TEXT,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'PENDING_CONFIRMATION', 'COMPLETED', 'CANCELLED')),
    pending_response_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_adoption_pet_id ON adoption_posts(pet_id);
CREATE INDEX idx_adoption_status ON adoption_posts(status);
CREATE INDEX idx_adoption_created_at ON adoption_posts(created_at);
```

#### 1.2 Таблиця adoption_responses
```sql
CREATE TABLE adoption_responses (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    adoption_post_id BIGINT NOT NULL REFERENCES adoption_posts(id) ON DELETE CASCADE,
    responder_id BIGINT NOT NULL REFERENCES users(id),
    comment TEXT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('NEW', 'CONFIRMED_BY_OWNER', 'REJECTED_BY_OWNER', 'FINAL_CONFIRMED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(adoption_post_id, responder_id)
);

CREATE INDEX idx_response_post_id ON adoption_responses(adoption_post_id);
CREATE INDEX idx_response_responder_id ON adoption_responses(responder_id);
CREATE INDEX idx_response_status ON adoption_responses(status);
```

#### 1.3 Таблиця adoption_saved_posts
```sql
CREATE TABLE adoption_saved_posts (
    post_id BIGINT NOT NULL REFERENCES adoption_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    saved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);
```

#### 1.4 Таблиця adoption_view_history
```sql
CREATE TABLE adoption_view_history (
    post_id BIGINT NOT NULL REFERENCES adoption_posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);
```

#### 1.5 Оновлення таблиці users
```sql
ALTER TABLE users ADD COLUMN adoption_history_offset INTEGER DEFAULT 0;
```

**Залежності:** Немає

**Критерій завершення:** Міграція успішно застосована, всі таблиці та індекси створені.

---

### Фаза 2: Domain Entities

#### 2.1 AdoptionPost.java
- Поля: pet, ownerComment, status, pendingResponseId
- Factory method: `create(Pet pet, @Nullable String comment)`
- Domain methods: `confirmOwner(Long responseId)`, `complete()`, `cancel()`, `isActive()`

#### 2.2 AdoptionResponse.java
- Поля: adoptionPost, responder, comment, status
- Factory method: `create(AdoptionPost post, User responder, String comment)`
- Domain methods: `confirmByOwner()`, `rejectByOwner()`, `finalConfirm()`, `cancel()`

#### 2.3 AdoptionSavedPost.java (Embeddable/Entity з PK)
- Складений ключ: postId + userId
- Поле: savedAt

#### 2.4 AdoptionViewHistory.java (Embeddable/Entity з PK)
- Складений ключ: postId + userId
- Поле: viewedAt

#### 2.5 Enums
- AdoptionPostStatus.java
- AdoptionResponseStatus.java

**Залежності:** Фаза 1

**Критерій завершення:** Всі entity компілюються, мають коректні JPA-анотації.

---

### Фаза 3: Repositories

#### 3.1 AdoptionPostRepository
- `Optional<AdoptionPost> findByPetId(Long petId)`
- `List<AdoptionPost> findByStatus(AdoptionPostStatus status)`
- `List<AdoptionPost> findByPetOwnerId(Long ownerId)`
- `boolean existsByPetId(Long petId)`
- Query: пошук невідібраних активних анкет для користувача

#### 3.2 AdoptionResponseRepository
- `List<AdoptionResponse> findByAdoptionPostId(Long postId)`
- `List<AdoptionResponse> findByAdoptionPostIdOrderByStatusDescCreatedAtDesc(Long postId)`
- `boolean existsByAdoptionPostIdAndResponderId(Long postId, Long responderId)`
- `@Modifying void rejectOthers(Long postId, Long confirmedId)` — відхилити інші відгуки

#### 3.3 AdoptionSavedPostRepository
- `List<AdoptionSavedPost> findByIdUserId(Long userId)`
- `boolean existsByIdPostIdAndIdUserId(Long postId, Long userId)`

#### 3.4 AdoptionViewHistoryRepository
- `boolean existsByIdPostIdAndIdUserId(Long postId, Long userId)`
- `@Modifying void deleteByIdUserId(Long userId)` — скидання історії

**Залежності:** Фаза 2

**Критерій завершення:** Репозиторії компілюються.

**Тести:** Slice tests (@DataJpaTest) для кожного репозиторію.

---

### Фаза 4: DTOs

#### 4.1 AdoptionPostDTO
```java
public record AdoptionPostDTO(
    Long id,
    Long petId,
    String petName,
    String petPhotoUrl,
    String ownerComment,
    AdoptionPostStatus status,
    Instant createdAt
) {
    public static AdoptionPostDTO fromEntity(AdoptionPost entity) { ... }
}
```

#### 4.2 AdoptionPostDetailDTO
```java
public record AdoptionPostDetailDTO(
    AdoptionPostDTO post,
    List<AdoptionResponseDTO> responses
) {}
```

#### 4.3 AdoptionResponseDTO
```java
public record AdoptionResponseDTO(
    Long id,
    Long postId,
    Long responderId,
    String responderUsername,
    String responderTelegramUsername,
    String comment,
    AdoptionResponseStatus status,
    Instant createdAt
) {
    public String getStatusEmoji() { ... }
}
```

#### 4.4 AdoptionRecommendationDTO
Для відображення в стрічці охочого.

**Залежності:** Фаза 2

**Критерій завершення:** DTO створені з factory методами fromEntity.

---

### Фаза 5: Domain Services (публічний API)

#### 5.1 AdoptionPostService (інтерфейс)
```java
public interface AdoptionPostService {
    AdoptionPostDTO create(Long petId, @Nullable String ownerComment);
    AdoptionPostDetailDTO findById(Long id);
    List<AdoptionPostDTO> findActiveByOwnerId(Long ownerId);
    List<AdoptionPostDTO> findAllByOwnerId(Long ownerId);
    void cancel(Long postId, Long ownerId);
    boolean existsByPetId(Long petId);
    
    // Для стрічки
    AdoptionRecommendationDTO findNextForFeed(Long userId, int offset);
    void recordView(Long postId, Long userId);
    int getOffset(Long userId);
    void resetOffset(Long userId);
}
```

#### 5.2 AdoptionResponseService (інтерфейс)
```java
public interface AdoptionResponseService {
    AdoptionResponseDTO create(Long postId, Long responderId, String comment);
    List<AdoptionResponseDTO> findByPostId(Long postId);
    AdoptionResponseDTO findById(Long id);
    
    void confirmByOwner(Long responseId, Long ownerId);
    void rejectByOwner(Long responseId, Long ownerId);
    void restore(Long responseId, Long ownerId);
    
    void finalConfirm(Long responseId, Long responderId);
    void declineFinalization(Long responseId, Long responderId);
    
    boolean hasResponded(Long postId, Long userId);
}
```

#### 5.3 AdoptionSavedPostService
```java
public interface AdoptionSavedPostService {
    void save(Long postId, Long userId);
    void unsave(Long postId, Long userId);
    List<AdoptionPostDTO> findSavedByUserId(Long userId);
    boolean isSaved(Long postId, Long userId);
}
```

#### 5.4 AdoptionCompletionService (внутрішній)
Виконує передачу тварини при фінальному підтвердженні.

**Залежності:** Фаза 3, Фаза 4

**Критерій завершення:** Сервіси компілюються, логіка відповідає бізнес-правилам.

**Тести:** Unit tests з моками для кожного сервісу.

---

### Фаза 6: Events (опціонально, для сповіщень)

#### 6.1 Events
- `AdoptionResponseCreatedEvent` — новий відгук
- `AdoptionOwnerConfirmedEvent` — власник підтвердив
- `AdoptionFinalizedEvent` — передача завершена

#### 6.2 EventListener
Сповіщення користувачів через Telegram.

**Залежності:** Фаза 5

---

### Фаза 7: Telegram Form Handlers

#### 7.1 CREATE_ADOPTION_POST
**Кроки:**
1. Коментар (текст, опціонально)
2. Підтвердження

**FormSubmissionHandler:**
- Отримує petId з entityId форми
- Викликає `adoptionPostService.create()`
- Повертає повідомлення про успіх

#### 7.2 CREATE_ADOPTION_RESPONSE
**Кроки:**
1. Попередження: "Власник побачить ваш контакт (@username)"
2. Коментар (текст, опціонально)
3. Підтвердження

**FormSubmissionHandler:**
- Отримує postId з entityId форми
- Викликає `adoptionResponseService.create()`
- Повідомляє власника про новий відгук

**Залежності:** Фаза 5

---

### Фаза 8: Telegram Callback Handlers

#### 8.1 Для власника (Give)
| Handler | CallbackId | Опис |
|---------|------------|------|
| AdoptionGiveCallbackHandler | ADOPTION_GIVE | Список тварин для передачі |
| AdoptionGiveSelectPetHandler | ADOPTION_GIVE_SELECT_PET | Запуск форми CREATE_ADOPTION_POST |
| AdoptionMyPostsCallbackHandler | ADOPTION_MY_POSTS | Список анкет власника |
| AdoptionPostDetailOwnerHandler | ADOPTION_POST_DETAIL_OWNER | Деталі анкети + відгуки |
| AdoptionResponsesCallbackHandler | ADOPTION_RESPONSES | Список відгуків |
| AdoptionResponseDetailOwnerHandler | ADOPTION_RESPONSE_DETAIL_OWNER | Деталі відгуку з кнопками |
| AdoptionResponseConfirmHandler | ADOPTION_RESPONSE_CONFIRM | Підтвердження відгуку |
| AdoptionResponseRejectHandler | ADOPTION_RESPONSE_REJECT | Відхилення відгуку |
| AdoptionResponseRestoreHandler | ADOPTION_RESPONSE_RESTORE | Відновлення відхиленого |
| AdoptionPostCancelHandler | ADOPTION_POST_CANCEL | Скасування анкети |

#### 8.2 Для охочого (Get)
| Handler | CallbackId | Опис |
|---------|------------|------|
| AdoptionGetCallbackHandler | ADOPTION_GET | Перше оголошення в стрічці |
| AdoptionGetNextHandler | ADOPTION_GET_NEXT | Наступне оголошення |
| AdoptionResponseCreateHandler | ADOPTION_RESPONSE_CREATE | Запуск форми відгуку |
| AdoptionSavedPostsHandler | ADOPTION_SAVED_POSTS | Список збережених |
| AdoptionSavePostHandler | ADOPTION_SAVE_POST | Зберегти поточну анкету |
| AdoptionUnsavePostHandler | ADOPTION_UNSAVE_POST | Видалити зі збережених |
| AdoptionFinalConfirmHandler | ADOPTION_FINAL_CONFIRM | Фінальне підтвердження охочим |
| AdoptionFinalDeclineHandler | ADOPTION_FINAL_DECLINE | Відмова від підтвердженого відгуку |

#### 8.3 Спільні
| Handler | CallbackId | Опис |
|---------|------------|------|
| AdoptionMenuCallbackHandler | ADOPTION_MENU | Головне меню адопції |
| AdoptionPostDetailHandler | ADOPTION_POST_DETAIL | Деталі чужої анкети (стрічка) |

**Залежності:** Фаза 7

**Критерій завершення:** Всі handlers обробляють відповідні callback.

---

### Фаза 9: Testing

#### 9.1 Unit Tests
- AdoptionPostServiceTest — створення, скасування, перевірка обмежень
- AdoptionResponseServiceTest — створення відгуку, підтвердження, відхилення, фіналізація
- AdoptionSavedPostServiceTest — зберегти, видалити, отримати список

#### 9.2 Repository Tests
- AdoptionPostRepositoryTest — пошук за статусом, власником
- AdoptionResponseRepositoryTest — сортування, rejectOthers

#### 9.3 Handler Tests
- AdoptionGiveCallbackHandlerTest — список тварин
- AdoptionGetCallbackHandlerTest — стрічка з offset
- AdoptionResponseDetailOwnerHandlerTest — кнопки за статусом

**Залежності:** Фаза 8

---

### Фаза 10: CallbackId Enum

Додати в `CallbackId.java`:
```java
ADOPTION_MENU("Меню адопції"),
ADOPTION_GIVE("Віддати тварину"),
ADOPTION_GIVE_SELECT_PET("Обрати тварину"),
ADOPTION_GET("Отримати тварину"),
ADOPTION_GET_NEXT("Наступна анкета"),
ADOPTION_RESPONSE_CREATE("Відгукнутися"),
ADOPTION_MY_POSTS("Мої анкети"),
ADOPTION_POST_DETAIL("Деталі анкети"),
ADOPTION_POST_DETAIL_OWNER("Деталі (власник)"),
ADOPTION_RESPONSES("Відгуки"),
ADOPTION_RESPONSE_DETAIL("Деталі відгуку"),
ADOPTION_RESPONSE_DETAIL_OWNER("Деталі відгуку (власник)"),
ADOPTION_RESPONSE_CONFIRM("Підтвердити відгук"),
ADOPTION_RESPONSE_REJECT("Відхилити відгук"),
ADOPTION_RESPONSE_RESTORE("Відновити відгук"),
ADOPTION_FINAL_CONFIRM("Остаточно підтвердити"),
ADOPTION_FINAL_DECLINE("Відмовитися"),
ADOPTION_POST_CANCEL("Скасувати анкету"),
ADOPTION_SAVED_POSTS("Збережені анкети"),
ADOPTION_SAVE_POST("Зберегти"),
ADOPTION_UNSAVE_POST("Видалити зі збережених");
```

**Залежності:** Немає (можна робити паралельно з Фазою 1)

---

## 4. Порядок виконання

### Ітерація 1: Foundation
- [x] Фаза 1: Database Schema — ✅ Міграція `0011-update-adoption-tables.xml` створена та додана до master-changelog
- [x] Фаза 2: Domain Entities — ✅ Entities створено (AdoptionPost, AdoptionResponse, AdoptionSavedPost, AdoptionViewHistory, Enums)
- [x] Фаза 3: Repositories — ✅ Repository interfaces створено
- [x] Фаза 4: DTOs — ✅ DTOs створено (AdoptionPostDTO, AdoptionResponseDTO, etc.)
- [x] Фаза 9: Testing (placeholders) — ✅ Тести-заглушки з @Disabled створено
- [ ] Фаза 10: CallbackId Enum — ⏳ Існуючі CallbackId потребують оновлення

### Ітерація 2: Data Access
- [x] Фаза 3: Repositories — ✅ Repository interfaces створено
- [x] Фаза 5: Domain Services — ✅ Interfaces + Implementations створено
  - AdoptionPostService + AdoptionPostServiceImpl
  - AdoptionResponseService + AdoptionResponseServiceImpl
  - AdoptionSavedPostService + AdoptionSavedPostServiceImpl
  - AdoptionCompletionService (внутрішній)

### Ітерація 3: Telegram Integration (Give — для власника)
- [x] Фаза 7: CREATE_ADOPTION_POST Form — ✅ FormType + CreateAdoptionPostHandler
- [x] Фаза 8: Give flow handlers — ✅ 10 callback handlers created
  - AdoptionGiveCallbackHandler, AdoptionSelectPetCallbackHandler, AdoptionAddPetCallbackHandler
  - AdoptionMyPostsCallbackHandler, AdoptionPostDetailOwnerCallbackHandler
  - AdoptionResponsesCallbackHandler, AdoptionResponseDetailOwnerCallbackHandler
  - AdoptionResponseTryConfirmHandler, AdoptionResponseConfirmActionHandler
  - AdoptionResponseTryRejectHandler, AdoptionResponseRejectActionHandler
  - AdoptionPostTryCancelHandler, AdoptionPostCancelActionHandler

### Ітерація 4: Telegram Integration (Get — для охочого)
- [x] Фаза 7: CREATE_ADOPTION_RESPONSE Form — ✅ FormType + CreateAdoptionResponseHandler
- [x] Фаза 8: Get flow handlers — ✅ 8 callback handlers created
  - AdoptionGetCallbackHandler, AdoptionGetNextHandler, AdoptionResponseCreateHandler
  - AdoptionSavePostHandler, AdoptionUnsavePostHandler
  - AdoptionMyResponsesCallbackHandler, AdoptionFinalConfirmHandler
  - AdoptionPostDetailFeedHandler

### Ітерація 5: Completion
- [ ] Фаза 6: Events (опціонально)
- [x] Фаза 8: Shared handlers — ✅ AdoptionMenuCallbackHandler
- [ ] Фаза 9: Testing

---

## 5. Примітки

### Стрічка оголошень (History Mode)
- Кожне оголошення = нове повідомлення
- При натисканні будь-якої кнопки — клавіатура прибирається з минулого повідомлення
- Використовується `adoption_history_offset` для відстеження позиції

### Двостороннє підтвердження
1. Власник натискає "Підтвердити" → статус CONFIRMED_BY_OWNER
2. Охочий отримує повідомлення з кнопкою
3. Охочий натискає "Остаточно підтвердити" → статус FINAL_CONFIRMED
4. Виконується передача тварини

### Сортування відгуків
- Спочатку: CONFIRMED_BY_OWNER (підтверджені)
- Потім: NEW (нові)
- Потім: REJECTED_BY_OWNER (відхилені)
- Всередині групи: за датою (новіші перші)

---

*Дата створення плану: 2026-05-07*
