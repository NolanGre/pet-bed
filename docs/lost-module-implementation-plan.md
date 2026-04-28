# Lost Module — Implementation Plan

> Детальний план реалізації модуля пошуку загублених тварин.
> План розрахований на **ітеративну реалізацію** з чіткими етапами та залежностями.

---

## Структура модуля

```
src/main/java/op/edu/ua/petbed/lost/
├── LostRequestService.java          // інтерфейс (публічний API)
├── FoundRequestService.java         // інтерфейс (публічний API)
├── domain/
│   ├── model/
│   │   ├── LostRequest.java
│   │   ├── FoundRequest.java
│   │   └── MatchQueueEntry.java
│   ├── repository/
│   │   ├── LostRequestRepository.java
│   │   ├── FoundRequestRepository.java
│   │   └── MatchQueueRepository.java
│   └── service/
│       ├── LostRequestServiceImpl.java   // реалізація
│       ├── FoundRequestServiceImpl.java  // реалізація
│       ├── MatchQueueService.java
│       └── FinderRecommendationCache.java
├── application/
│   ├── event/
│   │   ├── LostRequestCreatedEvent.java
│   │   ├── FoundRequestCreatedEvent.java
│   │   └── MatchingEventListener.java
│   ├── matching/
│   │   ├── MatchingService.java
│   │   └── MatchingAlgorithm.java
│   └── dto/
│       ├── LostRequestDTO.java
│       ├── FoundRequestDTO.java
│       └── MatchRecommendationDTO.java
└── infrastructure/
    └── job/
        └── FoundRequestCleanupJob.java
```

---

## Spring Modulith: Публічний API модуля

Для коректної роботи Spring Modulith, **публічні сервіси** модуля (ті, що використовуються іншими модулями) повинні бути розташовані **безпосередньо в пакеті модуля** (`op.edu.ua.petbed.lost`), а не в підпакетах.

### Структура модуля:

```
src/main/java/op/edu/ua/petbed/lost/
├── LostRequestService.java          // інтерфейс (публічний API)
├── FoundRequestService.java         // інтерфейс (публічний API)
├── domain/
│   ├── model/
│   │   ├── LostRequest.java
│   │   ├── FoundRequest.java
│   │   └── MatchQueueEntry.java
│   ├── repository/
│   │   ├── LostRequestRepository.java
│   │   ├── FoundRequestRepository.java
│   │   └── MatchQueueRepository.java
│   └── service/
│       ├── LostRequestServiceImpl.java   // реалізація
│       ├── FoundRequestServiceImpl.java  // реалізація
│       ├── MatchQueueService.java
│       └── FinderRecommendationCache.java
├── application/
│   ├── event/
│   ├── matching/
│   └── dto/
└── infrastructure/
    └── job/
```

**Правила:**
- Інтерфейси (`LostRequestService`, `FoundRequestService`) на рівні `lost/` — публічний API модуля
- Реалізації (`LostRequestServiceImpl`, `FoundRequestServiceImpl`) в підпакетах — не експортуються назовні
- DTO в `application/dto/` — експортуються для використання в інших модулях

**Використання в інших модулях:**
```java
// Telegram модуль може використовувати:
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.lost.LostRequestDTO;
```

---

## Етап 1: Database Schema (Liquibase)

### 1.1 Changelog: 0003-lost-module-tables.xml

**Таблиця lost_requests:**
```sql
CREATE TABLE lost_requests (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    pet_id BIGINT NOT NULL UNIQUE REFERENCES pets(id) ON DELETE CASCADE,
    contact_info VARCHAR(255) NOT NULL,
    last_seen_location GEOGRAPHY(POINT, 4326) NOT NULL,
    pet_type VARCHAR(50) NOT NULL,
    search_text TEXT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lost_pet_id ON lost_requests(pet_id);
CREATE INDEX idx_lost_type ON lost_requests(pet_type);
CREATE INDEX idx_lost_status ON lost_requests(status);
CREATE INDEX idx_lost_location ON lost_requests USING GIST (last_seen_location);
CREATE INDEX idx_lost_search_text ON lost_requests USING GIN (search_text gin_trgm_ops);
```

**Таблиця found_requests:**
```sql
CREATE TABLE found_requests (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    finder_id BIGINT NOT NULL REFERENCES users(id),
    photo_url VARCHAR(255) NOT NULL,
    pet_type VARCHAR(50) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_found_finder_id ON found_requests(finder_id);
CREATE INDEX idx_found_pet_type ON found_requests(pet_type);
CREATE INDEX idx_found_location ON found_requests USING GIST (location);
CREATE INDEX idx_found_description_trgm ON found_requests USING GIN (description gin_trgm_ops);
CREATE INDEX idx_found_created_at ON found_requests(created_at); -- для крони
```

**Таблиця match_queue:**
```sql
CREATE TABLE match_queue (
    id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    lost_request_id BIGINT NOT NULL REFERENCES lost_requests(id) ON DELETE CASCADE,
    found_request_id BIGINT NOT NULL REFERENCES found_requests(id) ON DELETE CASCADE,
    score NUMERIC(7,4) NOT NULL,
    viewing_status VARCHAR(50) NOT NULL CHECK (viewing_status IN ('NEW', 'VIEWED', 'REJECTED')),
    viewed_by VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(lost_request_id, found_request_id)
);

CREATE INDEX idx_match_lost_id ON match_queue(lost_request_id);
CREATE INDEX idx_match_found_id ON match_queue(found_request_id);
CREATE INDEX idx_match_status ON match_queue(viewing_status);
CREATE INDEX idx_match_score ON match_queue(score DESC);
```

**Залежності:** None (можна виконувати паралельно з іншими етапами)

**Критерій завершення:** Міграція успішно застосована, таблиці створені з індексами.

---

## Етап 2: Domain Entities

### 2.1 LostRequest (Entity)

```java
@Entity
@Table(name = "lost_requests")
@NullMarked
public class LostRequest extends AbstractAuditableEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false, unique = true)
    private Pet pet;
    
    @Column(name = "contact_info", nullable = false)
    private String contactInfo;
    
    @Column(name = "last_seen_location", nullable = false, columnDefinition = "GEOGRAPHY(POINT, 4326)")
    private Point lastSeenLocation;
    
    @Column(name = "pet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PetType petType;
    
    @Column(name = "search_text", nullable = false)
    private String searchText;
    
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private LostRequestStatus status;
    
    // Factory method
    public static LostRequest create(Pet pet, String contactInfo, Point location) {
        // validation
        // generate searchText from pet data
    }
    
    // Domain methods
    public void cancel() {
        this.status = LostRequestStatus.CANCELLED;
    }
    
    public boolean isActive() {
        return status == LostRequestStatus.ACTIVE;
    }
}
```

### 2.2 FoundRequest (Entity)

```java
@Entity
@Table(name = "found_requests")
@NullMarked
public class FoundRequest extends AbstractAuditableEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finder_id", nullable = false)
    private User finder;
    
    @Column(name = "photo_url", nullable = false)
    private String photoUrl;
    
    @Column(name = "pet_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private PetType petType;
    
    @Column(name = "location", nullable = false, columnDefinition = "GEOGRAPHY(POINT, 4326)")
    private Point location;
    
    @Column(name = "description", nullable = false)
    private String description;
    
    // Factory method
    public static FoundRequest create(User finder, String photoUrl, PetType petType, 
                                     Point location, String description) {
        // validation
    }
}
```

### 2.3 MatchQueueEntry (Entity)

```java
@Entity
@Table(name = "match_queue")
@NullMarked
public class MatchQueueEntry extends AbstractAuditableEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_request_id", nullable = false)
    private LostRequest lostRequest;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_request_id", nullable = false)
    private FoundRequest foundRequest;
    
    @Column(name = "score", nullable = false, precision = 7, scale = 4)
    private BigDecimal score;
    
    @Column(name = "viewing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ViewingStatus viewingStatus;
    
    @Column(name = "viewed_by")
    private String viewedBy;
    
    // Factory method
    public static MatchQueueEntry create(LostRequest lost, FoundRequest found, BigDecimal score) {
        // validation
    }
    
    // Domain methods
    public void markAsViewed(String viewedBy) {
        this.viewingStatus = ViewingStatus.VIEWED;
        this.viewedBy = viewedBy;
    }
    
    public void markAsRejected() {
        this.viewingStatus = ViewingStatus.REJECTED;
    }
}
```

### 2.4 Enums

```java
public enum LostRequestStatus {
    ACTIVE,
    CANCELLED
}

public enum ViewingStatus {
    NEW,
    VIEWED,
    REJECTED
}
```

**Залежності:** Етап 1 (таблиці повинні існувати)

**Критерій завершення:** Всі entity компілюються, мають коректні анотації JPA та методи.

---

## Етап 3: Repositories

### 3.1 LostRequestRepository

```java
@Repository
public interface LostRequestRepository extends JpaRepository<LostRequest, Long> {
    
    Optional<LostRequest> findByPetId(Long petId);
    
    List<LostRequest> findByPetOwnerIdAndStatus(Long ownerId, LostRequestStatus status);
    
    List<LostRequest> findByStatusAndPetType(LostRequestStatus status, PetType petType);
    
    boolean existsByPetId(Long petId);
    
    @Modifying
    @Query("DELETE FROM LostRequest lr WHERE lr.pet.id = :petId")
    void deleteByPetId(Long petId);
}
```

### 3.2 FoundRequestRepository

```java
@Repository
public interface FoundRequestRepository extends JpaRepository<FoundRequest, Long> {
    
    List<FoundRequest> findByFinderId(Long finderId);
    
    @Query(value = """
        SELECT fr.* FROM found_requests fr
        WHERE fr.pet_type = :petType
        AND ST_DWithin(fr.location, :location, 50000)
        AND fr.created_at > :since
        ORDER BY fr.created_at DESC
        """, nativeQuery = true)
    List<FoundRequest> findRecentByPetTypeAndLocation(
        @Param("petType") String petType,
        @Param("location") Point location,
        @Param("since") Timestamp since
    );
    
    @Query("DELETE FROM FoundRequest fr WHERE fr.createdAt < :date")
    void deleteOlderThan(@Param("date") Instant date);
}
```

### 3.3 MatchQueueRepository

```java
@Repository
public interface MatchQueueRepository extends JpaRepository<MatchQueueEntry, Long> {
    
    List<MatchQueueEntry> findByLostRequestIdAndViewingStatusIn(
        Long lostRequestId, List<ViewingStatus> statuses);
    
    List<MatchQueueEntry> findByLostRequestIdOrderByScoreDesc(Long lostRequestId);
    
    boolean existsByLostRequestIdAndFoundRequestId(Long lostRequestId, Long foundRequestId);
    
    @Modifying
    void deleteByLostRequestId(Long lostRequestId);
}
```

**Залежності:** Етап 2 (entity повинні існувати)

**Критерій завершення:** Репозиторії компілюються, запити працюють коректно.

**Тести:** Repository slice tests (@DataJpaTest) для кожного репозиторію.

---

## Етап 4: DTOs та Mappers

### 4.1 LostRequestDTO

```java
public record LostRequestDTO(
    Long id,
    Long petId,
    String petName,
    String contactInfo,
    Point location,
    PetType petType,
    LostRequestStatus status,
    Instant createdAt
) {
    public static LostRequestDTO fromEntity(LostRequest entity) {
        return new LostRequestDTO(
            entity.getId(),
            entity.getPet().getId(),
            entity.getPet().getName(),
            entity.getContactInfo(),
            entity.getLastSeenLocation(),
            entity.getPetType(),
            entity.getStatus(),
            entity.getCreatedAt()
        );
    }
}
```

### 4.2 FoundRequestDTO

```java
public record FoundRequestDTO(
    Long id,
    Long finderId,
    String photoUrl,
    PetType petType,
    Point location,
    String description,
    Instant createdAt
) {
    public static FoundRequestDTO fromEntity(FoundRequest entity) {
        return new FoundRequestDTO(
            entity.getId(),
            entity.getFinder().getId(),
            entity.getPhotoUrl(),
            entity.getPetType(),
            entity.getLocation(),
            entity.getDescription(),
            entity.getCreatedAt()
        );
    }
}
```

### 4.3 MatchRecommendationDTO

```java
public record MatchRecommendationDTO(
    Long matchQueueId,
    Long lostRequestId,
    Long foundRequestId,
    BigDecimal score,
    String photoUrl,
    String description,
    Point location,
    Instant foundRequestCreatedAt,  // дата створення для власника
    Double distanceKm
) {}
```

**Залежності:** Етап 3

**Критерій завершення:** DTO створені з static factory методами fromEntity.

---

## Етап 5: Domain Services

### 5.1 LostRequestService

**Інтерфейс:**
```java
public interface LostRequestService {
    LostRequestDTO create(Long petId, String contactInfo, Point location);
    LostRequestDTO findById(Long id);
    List<LostRequestDTO> findActiveByOwnerId(Long ownerId);
    void cancel(Long lostRequestId);
    boolean existsByPetId(Long petId);
}
```

**Реалізація:**
```java
@Service
@RequiredArgsConstructor
@NullMarked
public class LostRequestServiceImpl implements LostRequestService {
    
    private final LostRequestRepository lostRequestRepository;
    private final PetRepository petRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public LostRequestDTO create(Long petId, String contactInfo, Point location) {
        // 1. Знайти тварину
        // 2. Перевірити що немає активного lost_request
        // 3. Змінити статус тварини на IN_LOST
        // 4. Створити LostRequest
        // 5. Зберегти
        // 6. Опублікувати подію LostRequestCreatedEvent
    }
    
    @Override
    @Transactional
    public void cancel(Long lostRequestId) {
        // 1. Знайти запит
        // 2. Скасувати (статус CANCELLED)
        // 3. Змінити статус тварини на DEFAULT
        // 4. Видалення match_queue відбувається каскадно
    }
}
```

### 5.2 FoundRequestService

**Інтерфейс:**
```java
public interface FoundRequestService {
    FoundRequestDTO create(Long finderId, String photoUrl, PetType petType,
                          Point location, String description);
    FoundRequestDTO findById(Long id);
    List<FoundRequestDTO> findByFinderId(Long finderId);
}
```

**Реалізація:**
```java
@Service
@RequiredArgsConstructor
@NullMarked
public class FoundRequestServiceImpl implements FoundRequestService {
    
    private final FoundRequestRepository foundRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    @Override
    @Transactional
    public FoundRequestDTO create(Long finderId, String photoUrl, PetType petType,
                                  Point location, String description) {
        // 1. Знайти користувача
        // 2. Створити FoundRequest
        // 3. Зберегти
        // 4. Опублікувати подію FoundRequestCreatedEvent
    }
}
```

### 5.3 MatchQueueService

```java
@Service
@RequiredArgsConstructor
public class MatchQueueService {
    
    private final MatchQueueRepository matchQueueRepository;
    
    @Transactional
    public void addMatch(LostRequest lost, FoundRequest found, BigDecimal score) {
        // Перевірити що такої пари ще немає
        // Створити MatchQueueEntry
    }
    
    @Transactional(readOnly = true)
    public List<MatchRecommendationDTO> getRecommendationsForOwner(Long lostRequestId) {
        // Отримати всі NEW або REJECTED для цього lost_request
        // Сортувати за score DESC
    }
    
    @Transactional
    public void markAsViewed(Long matchQueueId, String viewedBy) {
        // Оновити статус
    }
    
    @Transactional
    public void markAsRejected(Long matchQueueId) {
        // Оновити статус
    }
}
```

**Залежності:** Етап 4

**Критерій завершення:** Сервіси компілюються, логіка відповідає бізнес-правилам.

**Тести:** Unit tests з моками для кожного сервісу.

---

## Етап 6: FinderRecommendationCache

### 6.1 Реалізація кешу

```java
@Component
public class FinderRecommendationCache {
    
    private final ConcurrentHashMap<Long, Queue<Long>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Instant> timestamps = new ConcurrentHashMap<>();
    private static final Duration TTL = Duration.ofDays(1);
    
    public void put(Long finderId, List<Long> lostRequestIds) {
        cache.put(finderId, new ConcurrentLinkedQueue<>(lostRequestIds));
        timestamps.put(finderId, Instant.now());
    }
    
    public Optional<Long> pollNext(Long finderId) {
        cleanIfExpired(finderId);
        return Optional.ofNullable(cache.get(finderId))
            .map(Queue::poll);
    }
    
    public boolean hasRecommendations(Long finderId) {
        cleanIfExpired(finderId);
        Queue<Long> queue = cache.get(finderId);
        return queue != null && !queue.isEmpty();
    }
    
    public void remove(Long finderId) {
        cache.remove(finderId);
        timestamps.remove(finderId);
    }
    
    private void cleanIfExpired(Long finderId) {
        Instant created = timestamps.get(finderId);
        if (created != null && Instant.now().isAfter(created.plus(TTL))) {
            remove(finderId);
        }
    }
    
    @Scheduled(fixedRate = 3600000) // кожну годину
    public void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        timestamps.forEach((finderId, timestamp) -> {
            if (timestamp.isBefore(cutoff)) {
                remove(finderId);
            }
        });
    }
}
```

**Залежності:** None (незалежний компонент)

**Критерій завершення:** Кеш працює коректно: TTL, FIFO, thread-safe.

**Тести:** Unit tests для кешу.

---

## Етап 7: Matching Algorithm

### 7.1 MatchingAlgorithm

```java
@Component
@RequiredArgsConstructor
public class MatchingAlgorithm {
    
    private static final double GEO_WEIGHT = 0.4;
    private static final double TEXT_WEIGHT = 0.6;
    private static final double MAX_DISTANCE_KM = 50.0;
    
    private final JdbcTemplate jdbcTemplate;
    
    public BigDecimal calculateScore(LostRequest lost, FoundRequest found) {
        // Географічна складова (40%)
        double geoScore = calculateGeoScore(lost.getLastSeenLocation(), found.getLocation());
        
        // Текстова складова (60%)
        double textScore = calculateTextScore(lost.getSearchText(), found.getDescription());
        
        double totalScore = (geoScore * GEO_WEIGHT) + (textScore * TEXT_WEIGHT);
        return BigDecimal.valueOf(totalScore).setScale(4, RoundingMode.HALF_UP);
    }
    
    private double calculateGeoScore(Point lostLoc, Point foundLoc) {
        // ST_Distance в метрах
        String sql = "SELECT ST_Distance(?::geography, ?::geography)";
        Double distanceMeters = jdbcTemplate.queryForObject(sql, Double.class, 
            lostLoc.toString(), foundLoc.toString());
        
        if (distanceMeters == null || distanceMeters > MAX_DISTANCE_KM * 1000) {
            return 0.0;
        }
        
        // Лінійна шкала: 50км = 0%, 0км = 100%
        return 1.0 - (distanceMeters / (MAX_DISTANCE_KM * 1000));
    }
    
    private double calculateTextScore(String searchText, String description) {
        // pg_trgm.similarity
        String sql = "SELECT similarity(?, ?)";
        Double similarity = jdbcTemplate.queryForObject(sql, Double.class, 
            searchText, description);
        return similarity != null ? similarity : 0.0;
    }
}
```

### 7.2 MatchingService

```java
@Service
@RequiredArgsConstructor
public class MatchingService {
    
    private final MatchingAlgorithm matchingAlgorithm;
    private final LostRequestRepository lostRequestRepository;
    private final FoundRequestRepository foundRequestRepository;
    private final MatchQueueService matchQueueService;
    private final FinderRecommendationCache cache;
    
    @Async
    public void processNewFoundRequest(Long foundRequestId) {
        FoundRequest found = foundRequestRepository.findById(foundRequestId)
            .orElseThrow();
        
        // Знайти всі активні lost_requests того ж типу в радіусі 50км
        List<LostRequest> candidates = findCandidates(found);
        
        // Обчислити score та зберегти в match_queue
        List<MatchResult> results = candidates.stream()
            .map(lost -> {
                BigDecimal score = matchingAlgorithm.calculateScore(lost, found);
                return new MatchResult(lost, score);
            })
            .filter(r -> r.score().doubleValue() > 0)
            .sorted(Comparator.comparing(MatchResult::score).reversed())
            .toList();
        
        // Зберегти в match_queue для власників
        results.forEach(r -> matchQueueService.addMatch(r.lost(), found, r.score()));
        
        // Зберегти в кеш для перехожого (топ 50)
        List<Long> lostIds = results.stream()
            .limit(50)
            .map(r -> r.lost().getId())
            .toList();
        cache.put(found.getFinder().getId(), lostIds);
    }
    
    @Async
    public void processNewLostRequest(Long lostRequestId) {
        LostRequest lost = lostRequestRepository.findById(lostRequestId)
            .orElseThrow();
        
        // Знайти всі found_requests останніх N днів того ж типу
        List<FoundRequest> candidates = foundRequestRepository
            .findRecentByPetTypeAndLocation(
                lost.getPetType().name(),
                lost.getLastSeenLocation(),
                Timestamp.from(Instant.now().minus(Duration.ofDays(30)))
            );
        
        // Обчислити score та зберегти в match_queue
        candidates.stream()
            .map(found -> {
                BigDecimal score = matchingAlgorithm.calculateScore(lost, found);
                return new MatchResult(found, score);
            })
            .filter(r -> r.score().doubleValue() > 0)
            .forEach(r -> matchQueueService.addMatch(lost, r.found(), r.score()));
    }
    
    private record MatchResult(Lost lost(), Found found(), BigDecimal score()) {}
}
```

**Залежності:** Етап 5, Етап 6

**Критерій завершення:** Алгоритм коректно обчислює score, зберігає результати.

**Тести:** Unit tests для алгоритму з фіксованими даними.

---

## Етап 8: Event System

### 8.1 Events

```java
public record LostRequestCreatedEvent(Long lostRequestId) {}
public record FoundRequestCreatedEvent(Long foundRequestId) {}
```

### 8.2 Event Listener

```java
@Component
@RequiredArgsConstructor
public class MatchingEventListener {
    
    private final MatchingService matchingService;
    
    @EventListener
    public void handleLostRequestCreated(LostRequestCreatedEvent event) {
        matchingService.processNewLostRequest(event.lostRequestId());
    }
    
    @EventListener
    public void handleFoundRequestCreated(FoundRequestCreatedEvent event) {
        matchingService.processNewFoundRequest(event.foundRequestId());
    }
}
```

**Залежності:** Етап 7

**Критерій завершення:** Події публікуються та обробляються асинхронно.

---

## Етап 9: Cleanup Job

### 9.1 FoundRequestCleanupJob

```java
@Component
@RequiredArgsConstructor
public class FoundRequestCleanupJob {
    
    private final FoundRequestRepository foundRequestRepository;
    private static final Duration RETENTION_PERIOD = Duration.ofDays(365);
    
    @Scheduled(cron = "0 0 0 * * ?") // Щодня о 00:00
    @Transactional
    public void cleanupOldFoundRequests() {
        Instant cutoff = Instant.now().minus(RETENTION_PERIOD);
        foundRequestRepository.deleteOlderThan(cutoff);
    }
}
```

**Залежності:** Етап 3

**Критерій завершення:** Крона запускається за розкладом та видаляє старі записи.

---

## Етап 10: Telegram Form Handlers

### 10.1 CREATE_LOST_REQUEST Form

**Кроки форми:**
1. Контактна інформація (текст, обов'язкове)
2. Геолокація останнього бачення (location, обов'язкове)
3. Особливі ознаки (текст, опціональне)

**FormSubmissionHandler:**
```java
@Component
@RequiredArgsConstructor
public class CreateLostRequestHandler implements FormSubmissionHandler {
    
    private final LostRequestService lostRequestService;
    private final PetService petService;
    
    @Override
    public FormType getFormType() {
        return FormType.CREATE_LOST_REQUEST;
    }
    
    @Override
    public BotApiMethod<?> handle(FormData formData) {
        Long petId = formData.getEntityId(); // передається при старті форми
        String contactInfo = formData.getStepInput(0);
        Point location = formData.getLocationInput(1);
        String specialFeatures = formData.getStepInput(2); // може бути null
        
        // Якщо є особливі ознаки — оновити тварину
        if (specialFeatures != null && !specialFeatures.isBlank()) {
            petService.updateSpecialFeatures(petId, specialFeatures);
        }
        
        // Створити lost request
        LostRequestDTO dto = lostRequestService.create(petId, contactInfo, location);
        
        return ResponseBuilder.sendMessage(formData.getChatId())
            .text("✅ Пошук запущено! Ми повідомимо вас про можливі збіги.")
            .keyboard(InlineKeyboardBuilder.builder()
                .backButtonTo(formData.getReturnCallback())
                .build())
            .build();
    }
}
```

### 10.2 CREATE_FOUND_REQUEST Form

**Кроки форми:**
1. Тип тварини (choice з PetType)
2. Фото (photo, обов'язкове)
3. Геолокація (location, обов'язкове)
4. Порода (текст, опціональне)
5. Колір (текст, опціональне)
6. Тип окрасу (текст, опціональне)
7. Стать (choice з PetSex, опціональне)
8. Розмір (choice з PetSize, опціональне)
9. Особливі ознаки (текст, опціональне)

**FormSubmissionHandler:**
```java
@Component
@RequiredArgsConstructor
public class CreateFoundRequestHandler implements FormSubmissionHandler {
    
    private final FoundRequestService foundRequestService;
    
    @Override
    public FormType getFormType() {
        return FormType.CREATE_FOUND_REQUEST;
    }
    
    @Override
    public BotApiMethod<?> handle(FormData formData) {
        Long finderId = formData.getUserId();
        PetType petType = PetType.valueOf(formData.getStepInput(0));
        String photoUrl = formData.getPhotoInput(1);
        Point location = formData.getLocationInput(2);
        
        // Агрегація description з усіх текстових кроків
        String description = buildDescription(formData);
        
        FoundRequestDTO dto = foundRequestService.create(
            finderId, photoUrl, petType, location, description);
        
        return ResponseBuilder.sendMessage(formData.getChatId())
            .text("""
                ✅ Анкету збережено!
                
                Ми знайшли потенційних власників для цієї тварини.
                """.trim())
            .keyboard(InlineKeyboardBuilder.builder()
                .addButton("🔍 Переглянути рекомендації", CallbackId.LOST_FOUND_MATCHES)
                .addButton("⬅️ Повернутись", CallbackId.MENU)
                .build())
            .build();
    }
    
    private String buildDescription(FormData formData) {
        StringBuilder sb = new StringBuilder();
        // Додати всі непорожні текстові поля
        for (int i = 3; i <= 8; i++) {
            String value = formData.getStepInput(i);
            if (value != null && !value.isBlank()) {
                sb.append(value).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
```

**Залежності:** Етап 5 (сервіси), існуюча інфраструктура форм

**Критерій завершення:** Форми ініціюються, збирають дані, створюють анкети.

---

## Етап 11: Telegram Callback Handlers

### 11.1 LOST_START Handler

```java
@Component
@RequiredArgsConstructor
public class LostStartCallbackHandler implements CallbackHandler {
    
    private final PetService petService;
    private final FormService formService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_START;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long userId = context.userId();
        Page<PetDTO> pets = petService.findByOwnerId(userId, PageRequest.of(0, 10));
        
        if (pets.isEmpty()) {
            // Якщо немає тварин — запустити форму додавання
            return formService.startForm(
                FormType.ADD_PET, 
                CallbackId.LOST_START, 
                userId, 
                context.chatId()
            );
        }
        
        // Показати список тварин з кнопкою "Додати"
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
            .text("🐾 Оберіть тварину для пошуку або додайте нову:")
            .keyboard(buildPetSelectionKeyboard(pets))
            .build();
    }
    
    private InlineKeyboardMarkup buildPetSelectionKeyboard(Page<PetDTO> pets) {
        InlineKeyboardBuilder builder = InlineKeyboardBuilder.builder();
        
        pets.forEach(pet -> builder.addButton(
            pet.getName(), 
            CallbackId.LOST_SELECT_PET.withEntityId(pet.getId())
        ));
        
        builder.addButton("➕ Додати тварину", CallbackId.LOST_ADD_PET);
        builder.backButtonTo(CallbackId.MENU);
        
        return builder.build();
    }
}
```

### 11.2 LOST_SELECT_PET Handler

```java
@Component
@RequiredArgsConstructor
public class LostSelectPetCallbackHandler implements CallbackHandler {
    
    private final FormService formService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_SELECT_PET;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long petId = context.callbackData().entityId();
        
        return formService.startFormWithEntity(
            FormType.CREATE_LOST_REQUEST,
            CallbackId.LOST_START,
            context.userId(),
            context.chatId(),
            petId // передається в форму
        );
    }
}
```

### 11.3 LOST_ADD_PET Handler

```java
@Component
@RequiredArgsConstructor
public class LostAddPetCallbackHandler implements CallbackHandler {
    
    private final FormService formService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_ADD_PET;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        return formService.startForm(
            FormType.ADD_PET,
            CallbackId.LOST_START, // повернення на початок пошуку
            context.userId(),
            context.chatId()
        );
    }
}
```

### 11.4 LOST_ACTIVE Handler

```java
@Component
@RequiredArgsConstructor
public class LostActiveCallbackHandler implements CallbackHandler {
    
    private final LostRequestService lostRequestService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_ACTIVE;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        List<LostRequestDTO> activeRequests = lostRequestService
            .findActiveByOwnerId(context.userId());
        
        // Показати пагінований список
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
            .text("🔍 Ваші активні пошуки:")
            .keyboard(buildActiveSearchesKeyboard(activeRequests))
            .build();
    }
}
```

### 11.5 LOST_RECOMMENDATIONS Handler (для власника)

```java
@Component
@RequiredArgsConstructor
public class LostRecommendationsCallbackHandler implements CallbackHandler {
    
    private final MatchQueueService matchQueueService;
    private final LostRequestService lostRequestService;
    private final TelegramMessageService messageService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_RECOMMENDATIONS;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long lostRequestId = context.callbackData().entityId();
        
        // Отримати першу рекомендацію
        List<MatchRecommendationDTO> recommendations = matchQueueService
            .getRecommendationsForOwner(lostRequestId);
        
        if (recommendations.isEmpty()) {
            return ResponseBuilder.answerCallbackQuery(context.callbackQuery().getId())
                .text("Поки немає рекомендацій. Перевірте пізніше!")
                .showAlert(true)
                .build();
        }
        
        MatchRecommendationDTO first = recommendations.get(0);
        
        // Прибрати клавіатуру з минулого повідомлення
        messageService.removeKeyboard(context.chatId(), context.messageId());
        
        // Надіслати нове повідомлення з рекомендацією
        return ResponseBuilder.sendPhoto(context.chatId(), first.photoUrl())
            .caption(buildCaption(first))
            .keyboard(buildRecommendationKeyboard(first, lostRequestId))
            .build();
    }
    
    private String buildCaption(MatchRecommendationDTO dto) {
        return String.format("""
            📅 Анкета від: %s
            📍 Відстань: %.1f км
            🎯 Схожість: %.1f%%
            
            %s
            """,
            dto.foundRequestCreatedAt(),
            dto.distanceKm(),
            dto.score().doubleValue() * 100,
            dto.description()
        );
    }
}
```

### 11.6 LOST_FOUND_MATCHES Handler (для перехожого)

```java
@Component
@RequiredArgsConstructor
public class LostFoundMatchesCallbackHandler implements CallbackHandler {
    
    private final FinderRecommendationCache cache;
    private final LostRequestService lostRequestService;
    private final TelegramMessageService messageService;
    
    @Override
    public CallbackId getCallbackId() {
        return CallbackId.LOST_FOUND_MATCHES;
    }
    
    @Override
    public PartialBotApiMethod<?> handle(CallbackQueryContext context) {
        Long finderId = context.userId();
        
        // Отримати наступну анкету з кешу
        Optional<Long> nextLostId = cache.pollNext(finderId);
        
        if (nextLostId.isEmpty()) {
            cache.remove(finderId);
            return ResponseBuilder.sendMessage(context.chatId())
                .text("🔍 Ви переглянули всі доступні анкети.")
                .keyboard(InlineKeyboardBuilder.builder()
                    .backButtonTo(CallbackId.MENU)
                    .build())
                .build();
        }
        
        LostRequestDTO lost = lostRequestService.findById(nextLostId.get());
        
        // Прибрати клавіатуру з минулого повідомлення
        messageService.removeKeyboard(context.chatId(), context.messageId());
        
        // Надіслати нове повідомлення (тільки якщо є контактна інформація!)
        return ResponseBuilder.sendPhoto(context.chatId(), lost.petPhotoUrl())
            .caption(buildCaption(lost))
            .keyboard(buildMatchKeyboard(finderId))
            .build();
    }
}
```

**Залежності:** Етап 10, існуюча інфраструктура Telegram

**Критерій завершення:** Всі callback handlers обробляють відповідні дії.

---

## Етап 12: Testing

### 12.1 Unit Tests

**LostRequestServiceTest:**
- `create_withValidData_createsLostRequest`
- `create_withExistingActiveRequest_throwsException`
- `cancel_withValidId_changesStatusToCancelled`
- `cancel_updatesPetStatusToDefault`

**FoundRequestServiceTest:**
- `create_withValidData_createsFoundRequest`
- `create_publishesFoundRequestCreatedEvent`

**MatchingAlgorithmTest:**
- `calculateScore_withSameLocation_returnsMaxGeoScore`
- `calculateScore_withMaxDistance_returnsZeroGeoScore`
- `calculateScore_withSimilarText_returnsHighTextScore`

**FinderRecommendationCacheTest:**
- `putAndPollNext_returnsInFIFOOrder`
- `pollNext_afterTTLExpired_returnsEmpty`
- `hasRecommendations_afterTTLExpired_returnsFalse`

### 12.2 Integration Tests

**LostRequestRepositoryTest:**
- CRUD операції
- Пошук за статусом та власником
- Каскадне видалення

**MatchingServiceIntegrationTest:**
- `processNewFoundRequest_createsMatchQueueEntries`
- `processNewFoundRequest_populatesCacheForFinder`
- `processNewLostRequest_findsRecentFoundRequests`

### 12.3 Telegram Handler Tests

**LostStartCallbackHandlerTest:**
- `handle_withNoPets_startsAddPetForm`
- `handle_withPets_showsPetSelectionKeyboard`

**LostRecommendationsCallbackHandlerTest:**
- `handle_removesPreviousKeyboard`
- `handle_sendsNewMessageWithPhoto`

---

## Порядок виконання

### Фаза 1: Foundation (паралельно)
1. Етап 1: Database Schema
2. Етап 2: Domain Entities
3. Етап 4: DTOs

### Фаза 2: Data Access (послідовно)
4. Етап 3: Repositories
5. Етап 5: Domain Services

### Фаза 3: Matching Engine (послідовно)
6. Етап 6: FinderRecommendationCache
7. Етап 7: Matching Algorithm
8. Етап 8: Event System

### Фаза 4: Infrastructure
9. Етап 9: Cleanup Job

### Фаза 5: Telegram Integration (послідовно)
10. Етап 10: Form Handlers
11. Етап 11: Callback Handlers

### Фаза 6: Testing
12. Етап 12: Tests

---

## Ризики та мітігація

| Ризик | Ймовірність | Вплив | Мітігація |
|-------|------------|-------|-----------|
| Проблеми з PostGIS | Середня | Високий | Тестування на реальній БД |
| Низька точність matching | Середня | Середній | Налаштування вагів алгоритму |
| Переповнення кешу | Низька | Середній | Обмеження TTL + періодична очистка |
| Конкурентний доступ до кешу | Середня | Середній | ConcurrentHashQueue |

---

## Підсумок

**Загальна оцінка складності:** Середня-Висока
**Орієнтовний час реалізації:** 2-3 тижні (1 розробник)
**Критичні шляхи:**
1. Database → Entities → Repositories → Services
2. Matching Algorithm → Event System
3. Form Handlers → Callback Handlers

**Що потрібно від команди:**
- Доступ до dev PostgreSQL з PostGIS
- Тестові дані (тварини, користувачі)
- Review архітектури перед початком

---

*Дата створення: 2025-04-28*
