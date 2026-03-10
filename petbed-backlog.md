# PetBed — Беклог проєкту (GitHub Projects)

> **Стек:** Java 21, Spring Boot, Spring Modulith, Spring Data JPA, Hibernate, PostgreSQL + PostGIS + pg_trgm, Liquibase, Gradle, Docker / Docker Compose, TelegramBots, AWS S3 / MinIO, Ngrok, Prometheus, Grafana, Loki, Promtail, JUnit 5, Mockito, Testcontainers, Lombok

---

## Епіки

| # | Епік | Опис |
|---|------|------|
| E1 | 🏗️ Інфраструктура та налаштування проєкту | Початкова конфігурація, Docker, CI, інструменти |
| E2 | 🗄️ База даних та міграції | Схема БД, Liquibase, PostGIS, індекси |
| E3 | 👤 User Module | Реєстрація, профіль, типи акаунтів |
| E4 | 🐾 Pets Module | CRUD тварин, реєстр |
| E5 | 🔍 Lost & Found Module | Анкети втрати/знахідки, геолокація |
| E6 | 🤖 Matching Module | Автоматичне зіставлення, черга, scored results |
| E7 | 🏠 Adoption Module | Передача тварин, заявки, підтвердження |
| E8 | 🛏️ Fostering Module | Перетримка, заявки, статуси |
| E9 | 📰 Feed Module | Стрічка волонтерів, геофільтрація |
| E10 | 💬 Telegram Module | Webhook, FSM форм, меню, callback |
| E11 | 🔔 Notification Module | Сповіщення користувачів через Telegram |
| E12 | 📦 Зберігання файлів | AWS S3 / MinIO інтеграція |
| E13 | 📊 Моніторинг та логування | Prometheus, Grafana, Loki, Promtail |
| E14 | ✅ Тестування | Unit, Integration, E2E тести |

---

## Беклог із послідовністю виконання

> **Пріоритет виконання:** E1 → E2 → E3 → E4 → E10 (базовий) → E12 → E5 → E6 → E7 → E8 → E9 → E11 → E10 (повний) → E13 → E14

---

## E1 — 🏗️ Інфраструктура та налаштування проєкту

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E1-1 | Ініціалізувати Gradle-проєкт | Spring Initializr, Java 21, налаштувати `build.gradle`, підключити Lombok, Spring Boot | 🔴 Critical |
| E1-2 | Налаштувати багатомодульну структуру (Spring Modulith) | Визначити пакети-модулі: `user`, `pet`, `lost`, `found`, `matching`, `adoption`, `fostering`, `feed`, `telegram`, `notification` | 🔴 Critical |
| E1-3 | Налаштувати Docker та Docker Compose | Контейнери: `app`, `postgres`, `minio`, `prometheus`, `grafana`, `loki`, `promtail` | 🔴 Critical |
| E1-4 | Налаштувати Ngrok для локальної розробки | Отримати статичний домен, прописати webhook URL для Telegram Bot API | 🟠 High |
| E1-5 | Налаштувати Git та `.gitignore` | Ігнорувати `.env`, secrets, build-директорії | 🔴 Critical |
| E1-6 | Налаштувати `.env` та конфігурацію через `application.yml` | Токен бота, credentials БД, S3 ключі, Ngrok URL | 🔴 Critical |
| E1-7 | Підключити Actuator + Micrometer для Prometheus | Ендпоінт `/actuator/prometheus`, базові метрики | 🟡 Medium |

---

## E2 — 🗄️ База даних та міграції (Liquibase)

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E2-1 | Підключити PostgreSQL + PostGIS + pg_trgm розширення | `CREATE EXTENSION postgis; CREATE EXTENSION pg_trgm;` у першій міграції | 🔴 Critical |
| E2-2 | Міграція: таблиця `users` | Поля: `id`, `telegram_id`, `telegram_username`, `type`, `adoption_history_offset`, `fostering_history_offset`, `created_at`, `updated_at` | 🔴 Critical |
| E2-3 | Міграція: таблиця `pets` | Поля: `id`, `owner_id` (FK), `name`, `photo_url`, `type`, `breed`, `color`, `color_pattern`, `age`, `sex`, `size`, `special_marks`, timestamps | 🔴 Critical |
| E2-4 | Міграція: таблиці `lost_request` та `found_request` | `last_seen_location` / `location` типу `GEOGRAPHY(Point, 4326)` | 🔴 Critical |
| E2-5 | Міграція: таблиця `match_queue` | Поля: `id`, `lost_request_id` (FK), `found_request_id` (FK), `score NUMERIC(7,4)`, `viewing_status`, timestamps | 🔴 Critical |
| E2-6 | Міграція: таблиці `adoption_posts`, `adoption_responses`, `adoption_saved_posts`, `adoption_view_history` | Зв'язки, композитні PK для history/saved | 🟠 High |
| E2-7 | Міграція: таблиці `fostering_posts`, `fostering_responses`, `fostering_saved_posts`, `fostering_view_history` | Аналогічно adoption + поле `planned_duration_days` | 🟠 High |
| E2-8 | Міграція: таблиця `feed_posts` | `location GEOGRAPHY(Point, 4326)`, `publisher_id` (FK), `text`, `photo_url` | 🟠 High |
| E2-9 | Міграція: таблиця `users_feed_history` | Композитний PK `(user_id, post_id)` | 🟠 High |
| E2-10 | Міграція: таблиця `user_form_states` | Поля: `user_id` (PK/FK), `form_type`, `current_step`, `collected_data JSONB`, timestamps | 🟠 High |
| E2-11 | Додати просторові індекси (GiST) на location-поля | `CREATE INDEX ... USING GIST (location)` для `lost_request`, `found_request`, `feed_posts` | 🟠 High |
| E2-12 | Додати триграмні індекси (GIN) на текстові поля | `CREATE INDEX ... USING GIN (special_marks gin_trgm_ops)` для `pets`, `found_request`, `lost_request` | 🟡 Medium |

---

## E3 — 👤 User Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E3-1 | Створити доменну сутність `User` + JPA entity | Поля згідно з таблицею 2.1, анотації Lombok (`@Builder`, `@Data`) | 🔴 Critical |
| E3-2 | Створити `UserRepository` (Spring Data JPA) | Методи: `findByTelegramId`, `existsByTelegramId` | 🔴 Critical |
| E3-3 | Реалізувати `UserService.registerOrGet` | Автоматична реєстрація при першій взаємодії з ботом (FR 1.1); повертає існуючого або створює нового | 🔴 Critical |
| E3-4 | Реалізувати зміну типу акаунту (USER ↔ VOLUNTEER) | FR 1.3; перевірка прав | 🟠 High |
| E3-5 | Реалізувати перегляд статистики користувача | Кількість знайдених/загублених/переданих тварин | 🟡 Medium |
| E3-6 | Unit-тести для `UserService` | Mockito, покриття `registerOrGet`, зміна типу | 🟠 High |

---

## E4 — 🐾 Pets Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E4-1 | Створити JPA entity `Pet` | Поля згідно таблиці 2.2, зв'язок `@ManyToOne` з `User` | 🔴 Critical |
| E4-2 | Створити `PetRepository` | `findAllByOwnerId` з пагінацією (FR 2.2) | 🔴 Critical |
| E4-3 | Реалізувати `PetService.addPet` | FR 2.1; прив'язка до власника | 🔴 Critical |
| E4-4 | Реалізувати `PetService.updatePet` | FR 2.3; перевірка статусу тварини перед редагуванням | 🟠 High |
| E4-5 | Реалізувати `PetService.deletePet` | FR 2.4; каскадне видалення пов'язаних анкет | 🟠 High |
| E4-6 | Реалізувати пагінований список тварин | FR 2.2; повертати сторінками по 4 штуки для Telegram inline keyboard | 🟠 High |
| E4-7 | Unit-тести для `PetService` | CRUD, перевірка каскадного видалення | 🟠 High |

---

## E5 — 🔍 Lost & Found Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E5-1 | Створити JPA entity `LostRequest` | `last_seen_location` як `Point` (Hibernate Spatial / JTS), зв'язок `@OneToOne` з `Pet` | 🔴 Critical |
| E5-2 | Створити JPA entity `FoundRequest` | `location` як `Point`, зв'язок `@ManyToOne` з `User` (finder) | 🔴 Critical |
| E5-3 | Створити `LostRequestRepository` | Методи для пошуку за власником, статусом | 🔴 Critical |
| E5-4 | Створити `FoundRequestRepository` | Метод просторового пошуку в радіусі (PostGIS `ST_DWithin`) | 🔴 Critical |
| E5-5 | Реалізувати `LostService.createLostRequest` | FR 3.1, FR 3.2; можливість обрати тварину з реєстру | 🔴 Critical |
| E5-6 | Реалізувати `FoundService.createFoundRequest` | FR 3.3; мінімальні дані: фото, геолокація, базовий опис | 🔴 Critical |
| E5-7 | Реалізувати закриття/деактивацію анкети | FR 3.4; зміна статусу на `CLOSED` | 🟠 High |
| E5-8 | Реалізувати перегляд та фільтрацію анкет | FR 4.1, FR 4.2; фільтри за геолокацією, типом, породою | 🟠 High |
| E5-9 | Реалізувати оновлення геолокації `LostRequest` | FR 4.4; власник підтверджує нове місце | 🟡 Medium |
| E5-10 | Unit + Integration тести для Lost/Found сервісів | Testcontainers + реальна PostgreSQL, тест просторових запитів | 🟠 High |

---

## E6 — 🤖 Matching Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E6-1 | Створити JPA entity `MatchQueue` | Поля: `lostRequest`, `foundRequest`, `score`, `viewingStatus` (enum: `NEW`, `VIEWED`, `CONFIRMED`, `REJECTED`) | 🔴 Critical |
| E6-2 | Створити `MatchingRepository` | Методи: `findByLostRequestIdAndViewingStatus`, `saveAll` | 🔴 Critical |
| E6-3 | Реалізувати географічний скоринг | FR 5.6; `ST_Distance` між координатами → нормалізований score (0–1), радіус попередньої фільтрації FR 5.10 | 🔴 Critical |
| E6-4 | Реалізувати скоринг фізичних характеристик | FR 5.7; порівняння `type`, `breed`, `color`, `color_pattern`, `sex`, `size` → частковий score | 🔴 Critical |
| E6-5 | Реалізувати триграмний текстовий скоринг | FR 5.8; PostgreSQL `similarity(a, b)` через нативний запит для `special_marks` | 🟠 High |
| E6-6 | Реалізувати фінальний зважений score | FR 5.5; наприклад: `geo * 0.4 + physical * 0.4 + text * 0.2` | 🔴 Critical |
| E6-7 | Реалізувати `MatchingService.matchAsync` | FR 5.12; `@Async` метод, що запускається після збереження `FoundRequest`/`LostRequest`; зберігає результати (FR 5.13) | 🔴 Critical |
| E6-8 | Реалізувати попередню фільтрацію кандидатів | FR 5.10, FR 5.11; спочатку малий радіус, потім розширення | 🟠 High |
| E6-9 | Реалізувати персоналізовану стрічку збігів | FR 5.14, FR 5.15; тільки `NEW` записи, відсортовані за `score DESC` | 🔴 Critical |
| E6-10 | Реалізувати підтвердження/відхилення збігу власником | FR 5.4, FR 5.16; зміна `viewingStatus`; відхилені можна повернути | 🟠 High |
| E6-11 | Unit-тести скорингових функцій | Перевірити граничні випадки (нульова відстань, відсутній опис) | 🟠 High |
| E6-12 | Integration-тест `matchAsync` | Testcontainers, перевірити що результати зберігаються в `match_queue` | 🟠 High |

---

## E7 — 🏠 Adoption Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E7-1 | Створити JPA entities: `AdoptionPost`, `AdoptionResponse`, `AdoptionSavedPost`, `AdoptionViewHistory` | Зв'язки згідно таблиць 2.8–2.11 | 🟠 High |
| E7-2 | Створити репозиторії для всіх Adoption-сутностей | `findByCriteria` з фільтрами, `existsByUserIdAndPostId` | 🟠 High |
| E7-3 | Реалізувати `AdoptionService.createPost` | FR 6.1; перевірка що тварина не має активної анкети | 🟠 High |
| E7-4 | Реалізувати `AdoptionService.applyForAdoption` | FR 6.2, FR 6.3; збереження відгуку + асинхронне сповіщення власника | 🟠 High |
| E7-5 | Реалізувати підтвердження/скасування передачі | FR 6.4, FR 6.5; бронювання тварини після підтвердження (зміна статусу) | 🟠 High |
| E7-6 | Реалізувати збереження та перегляд анкет | FR 6.6, FR 6.7; `adoption_saved_posts`, `adoption_view_history` з `offset` | 🟡 Medium |
| E7-7 | Unit + Integration тести для `AdoptionService` | Перевірити флоу: пост → відгук → підтвердження → зміна статусу тварини | 🟠 High |

---

## E8 — 🛏️ Fostering Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E8-1 | Створити JPA entities: `FosteringPost`, `FosteringResponse`, `FosteringSavedPost`, `FosteringViewHistory` | Поля згідно таблиць 2.12–2.15; `planned_duration_days` | 🟠 High |
| E8-2 | Створити репозиторії для всіх Fostering-сутностей | Аналогічно Adoption | 🟠 High |
| E8-3 | Реалізувати `FosteringService.createPost` | FR 7.1 | 🟠 High |
| E8-4 | Реалізувати `FosteringService.applyForFostering` | FR 7.2; збереження відгуку + сповіщення | 🟠 High |
| E8-5 | Реалізувати підтвердження, скасування, завершення перетримки | FR 7.3, FR 7.4; перехід статусів: `OPEN → ACTIVE → COMPLETED / CANCELLED` | 🟠 High |
| E8-6 | Реалізувати збереження та перегляд анкет | FR 7.5, FR 7.6; `fostering_history_offset` | 🟡 Medium |
| E8-7 | Unit + Integration тести для `FosteringService` | Перевірити всі переходи статусів | 🟠 High |

---

## E9 — 📰 Feed Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E9-1 | Створити JPA entities: `FeedPost`, `UserFeedHistory` | `location GEOGRAPHY`, `publisher_id` (тільки волонтери) | 🟡 Medium |
| E9-2 | Створити `FeedRepository` | Запит `ST_DWithin` для геофільтрації + виключення переглянутих (`NOT IN users_feed_history`) | 🟡 Medium |
| E9-3 | Реалізувати `FeedService.createPost` | Перевірка що користувач — волонтер (тип акаунту) | 🟡 Medium |
| E9-4 | Реалізувати стрічку оголошень з геофільтрацією | Вибірка непереглянутих постів у радіусі від локації користувача | 🟡 Medium |
| E9-5 | Реалізувати вибір радіусу користувачем | Зберігати вибраний радіус у сесії або `user_form_states` | 🟡 Medium |
| E9-6 | Unit-тести для `FeedService` | Тест перевірки ролі, тест геофільтрації | 🟡 Medium |

---

## E10 — 💬 Telegram Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E10-1 | Підключити бібліотеку TelegramBots, зареєструвати бота | `application.yml`: token, webhook URL (Ngrok) | 🔴 Critical |
| E10-2 | Реалізувати `TelegramWebhookController` | REST ендпоінт `POST /webhook`, приймає `Update` від Telegram | 🔴 Critical |
| E10-3 | Реалізувати `TelegramWebhookHandler` | Маршрутизація за типом події: `message`, `callback_query`; делегування до модулів | 🔴 Critical |
| E10-4 | Реалізувати `FormService` + `UserFormState` | FR зі збереженням кроку форми в `user_form_states` (JSONB); загальний механізм для всіх форм (рис. 2.18, 2.19) | 🔴 Critical |
| E10-5 | Реалізувати головне меню (Reply Keyboard) | Кнопки: Профіль, Мої тварини, Пошук тварин, Стрічка оголошень, Адопція, Перетримка (рис. 2.2) | 🔴 Critical |
| E10-6 | Реалізувати меню профілю | Зміна типу акаунту, перегляд статистики, закрити (рис. 2.3) | 🟠 High |
| E10-7 | Реалізувати меню тварин з пагінацією | Список тварин (4 на сторінку), "Додати тварину", "Закрити" (рис. 2.4) | 🟠 High |
| E10-8 | Реалізувати форму додавання тварини (FSM) | Кроки: тип → порода → колір → стать → вік → розмір → особливі прикмети → фото | 🟠 High |
| E10-9 | Реалізувати меню пошуку тварин | "Я знайшов тварину", "Почати пошук", "Активні пошуки" (рис. 2.6) | 🟠 High |
| E10-10 | Реалізувати форму створення `FoundRequest` (FSM) | Кроки: фото → геолокація → тип → порода → колір → особливі прикмети | 🟠 High |
| E10-11 | Реалізувати форму створення `LostRequest` (FSM) | Кроки: вибір з реєстру або нова тварина → фото → геолокація → контактний телефон | 🟠 High |
| E10-12 | Реалізувати перегляд стрічки збігів (Match Queue) | Відображення анкети знахідки з відсотком збігу, кнопки: "Підтвердити", "Відхилити", "Далі" | 🟠 High |
| E10-13 | Реалізувати меню адопції | "Віддати тварину", "Отримати тварину", "Активні заяви" (рис. 2.7) | 🟠 High |
| E10-14 | Реалізувати форму публікації анкети адопції (FSM) | Вибір тварини → коментар → підтвердження | 🟠 High |
| E10-15 | Реалізувати перегляд анкет адопції та подачу заявки | Список анкет, деталі, кнопка "Відгукнутися" + коментар | 🟠 High |
| E10-16 | Реалізувати меню перетримки | "Віддати тварину", "Взяти на перетримку", "Активні заяви" (рис. 2.8) | 🟡 Medium |
| E10-17 | Реалізувати форму публікації анкети перетримки (FSM) | Вибір тварини → термін (днів) → коментар → підтвердження | 🟡 Medium |
| E10-18 | Реалізувати перегляд та відгук на анкети перетримки | Аналогічно адопції | 🟡 Medium |
| E10-19 | Реалізувати меню стрічки волонтерів | "Переглянути оголошення", "Обрати геолокацію", "Обрати радіус", "Створити оголошення" (рис. 2.5) | 🟡 Medium |
| E10-20 | Реалізувати форму публікації поста волонтера (FSM) | Текст → фото (опц.) → геолокація → підтвердження | 🟡 Medium |
| E10-21 | Обробка помилок та невідомих команд | Загальний error handler, повідомлення про помилку, логування | 🟠 High |

---

## E11 — 🔔 Notification Module

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E11-1 | Створити `NotificationService` | Обгортка навколо Telegram Bot API для надсилання повідомлень за `telegram_id` | 🟠 High |
| E11-2 | Сповіщення про нову заявку на адопцію/перетримку | FR 8.2; власник отримує повідомлення після `applyFor*` | 🟠 High |
| E11-3 | Сповіщення про новий збіг у Match Queue | FR 8.3; власник `LostRequest` отримує повідомлення після `matchAsync` | 🟠 High |
| E11-4 | Сповіщення про зміну статусу | FR 8.4; підтвердження/скасування адопції, перетримки, закриття анкети | 🟠 High |
| E11-5 | Unit-тести для `NotificationService` | Mockito mock Telegram API, перевірити що повідомлення надсилаються в правильних сценаріях | 🟡 Medium |

---

## E12 — 📦 Зберігання файлів (S3 / MinIO)

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E12-1 | Підключити AWS SDK v2 для роботи з S3 | `S3Client` bean, налаштування через `application.yml` (bucket, region, endpoint) | 🟠 High |
| E12-2 | Реалізувати `FileStorageService.upload` | Завантаження фото (byte[] / InputStream) → повертає `photo_url` | 🟠 High |
| E12-3 | Налаштувати MinIO як fallback у Docker Compose | Однаковий S3-сумісний інтерфейс; перемикання через `application.yml` | 🟡 Medium |
| E12-4 | Реалізувати завантаження фото з Telegram | Отримати `file_id` → завантажити з Telegram → передати до `FileStorageService` | 🟠 High |

---

## E13 — 📊 Моніторинг та логування

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E13-1 | Налаштувати Prometheus scrape config | `prometheus.yml` з таргетом `app:8080/actuator/prometheus` | 🟡 Medium |
| E13-2 | Налаштувати Grafana дашборди | Імпортувати стандартний Spring Boot дашборд (ID 12900) | 🟡 Medium |
| E13-3 | Налаштувати Promtail + Loki | `promtail-config.yml` читає логи з Docker socket; Loki зберігає | 🟡 Medium |
| E13-4 | Підключити Loki datasource до Grafana | Перевірити що логи доступні у Grafana Explore | 🟡 Medium |
| E13-5 | Додати структуроване логування (SLF4J + Logback) | JSON-формат для Promtail; логувати ключові події без чутливих даних (NFR 4.2) | 🟡 Medium |

---

## E14 — ✅ Тестування

| ID | Задача | Деталі | Пріоритет |
|----|--------|--------|-----------|
| E14-1 | Налаштувати Testcontainers для integration-тестів | PostgreSQL контейнер з PostGIS; базовий `AbstractIntegrationTest` | 🟠 High |
| E14-2 | Integration-тест: реєстрація користувача | Перший webhook → користувач з'являється в БД | 🟠 High |
| E14-3 | Integration-тест: створення `FoundRequest` + запуск matching | Перевірити що `match_queue` заповнюється асинхронно | 🟠 High |
| E14-4 | Integration-тест: повний флоу адопції | Пост → відгук → підтвердження → зміна статусу тварини | 🟠 High |
| E14-5 | Integration-тест: повний флоу перетримки | Пост → відгук → підтвердження → активна → завершена | 🟠 High |
| E14-6 | Integration-тест: просторова фільтрація | Перевірити `ST_DWithin` запити повертають правильні результати | 🟠 High |
| E14-7 | Integration-тест: триграмний пошук | `similarity()` повертає коректні скори | 🟡 Medium |
| E14-8 | Unit-тести: скоринг Matching Module | Граничні значення, нульова відстань, порожній опис | 🟠 High |
| E14-9 | Unit-тести: FSM форм (FormService) | Перевірити всі кроки та агрегацію даних | 🟠 High |
| E14-10 | Unit-тести: NotificationService | Mock Telegram API | 🟡 Medium |

---

## 📋 Рекомендована послідовність виконання (Sprint-розбивка)

### Sprint 1 — Фундамент
`E1-1` → `E1-2` → `E1-3` → `E1-5` → `E1-6` → `E2-1` → `E2-2` → `E2-3` → `E3-1` → `E3-2` → `E3-3`

### Sprint 2 — Тварини + базовий Telegram
`E4-1` → `E4-2` → `E4-3` → `E10-1` → `E10-2` → `E10-3` → `E10-4` → `E10-5` → `E1-4` → `E3-6`

### Sprint 3 — Файли + Lost/Found
`E12-1` → `E12-2` → `E12-4` → `E2-4` → `E5-1` → `E5-2` → `E5-3` → `E5-4` → `E5-5` → `E5-6` → `E10-9` → `E10-10` → `E10-11`

### Sprint 4 — Matching (ключовий функціонал)
`E2-5` → `E2-11` → `E2-12` → `E6-1` → `E6-2` → `E6-3` → `E6-4` → `E6-5` → `E6-6` → `E6-7` → `E6-8` → `E6-9` → `E6-10` → `E10-12`

### Sprint 5 — Adoption + Fostering
`E2-6` → `E2-7` → `E7-1` → `E7-2` → `E7-3` → `E7-4` → `E7-5` → `E8-1` → `E8-2` → `E8-3` → `E8-4` → `E8-5` → `E10-13..E10-18`

### Sprint 6 — Feed + Notifications + Повний Telegram
`E2-8` → `E2-9` → `E9-1..E9-5` → `E11-1..E11-4` → `E10-6` → `E10-7` → `E10-8` → `E10-19` → `E10-20` → `E10-21`

### Sprint 7 — Моніторинг + Тести
`E13-1..E13-5` → `E14-1..E14-10` → `E1-7` → `E12-3`

---

## Примітки

- **TDD-підхід:** для кожного сервісу спочатку пишуться тести (E14), потім реалізація. Задачі на тести можна паралелити з відповідними сервісами.
- **FSM форм** (E10-4) — наскрізний механізм, реалізується першим у Telegram Module, всі інші форми (E10-8, E10-10, E10-11, E10-14, E10-17, E10-20) залежать від нього.
- **Matching Module** (E6) — найскладніший алгоритмічний блок, потребує готових `LostRequest` та `FoundRequest`.
- **Ngrok** (E1-4) використовується тільки для локальної розробки; у продакшені замінюється на реальний сервер.
