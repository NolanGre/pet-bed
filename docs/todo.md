## Next Stage: Stage 8 - Event System (Cleanup Job)
**Status:** Pending

### Goal
Implement FoundRequestCleanupJob - scheduled job to remove old found requests (1 year TTL)

### Files to Create
- `FoundRequestCleanupJob.java` in `lost/infrastructure/job/`

---

## Stage 7 - Matching Algorithm
**Status:** ✅ Completed
**Goal:** Implement matching algorithm and event listeners

### Requirements from docs/lost-module-analysis.md Section 3:

**Algorithm:**
- **Географічна складова (40%):** Лінійна шкала, 50км = 0%, 0км = 100%
- **Текстова складова (60%):** pg_trgm.similarity(lost.search_text, found.description)
- **Бонус:** Якщо found.description містить точні співпадіння з ключовими словами (порода, колір) — додаткові бали

**Async Processing (Section 2.4):**
- Matching is asynchronous via events
- User doesn't wait for results, feeds update in background

### Files to Create:

#### 1. MatchingAlgorithm.java
**Path:** `src/main/java/op/edu/ua/petbed/lost/application/matching/MatchingAlgorithm.java`

**Methods:**
- `BigDecimal calculateScore(LostRequest lost, FoundRequest found)`
  - Returns score 0.0000 - 1.0000 (4 decimal places)
  - Formula: (geoScore * 0.4) + (textScore * 0.6)
  
- `double calculateGeoScore(Point lostLoc, Point foundLoc)`
  - Use JdbcTemplate with PostGIS: `SELECT ST_Distance(?::geography, ?::geography)`
  - Linear scale: 50km = 0%, 0km = 100%
  - Return 0.0 if distance > 50km
  
- `double calculateTextScore(String searchText, String description)`
  - Use JdbcTemplate with pg_trgm: `SELECT similarity(?, ?)`
  - Returns 0.0 - 1.0

**Constants:**
- `GEO_WEIGHT = 0.4`
- `TEXT_WEIGHT = 0.6`
- `MAX_DISTANCE_KM = 50.0`

#### 2. MatchingService.java
**Path:** `src/main/java/op/edu/ua/petbed/lost/application/matching/MatchingService.java`

**Methods:**
- `@Async processNewFoundRequest(Long foundRequestId)`
  1. Load FoundRequest by id
  2. Find all active LostRequests with same pet_type
  3. Filter by distance (50km radius) via PostGIS
  4. Calculate score for each pair
  5. Save matches to match_queue via MatchQueueService
  6. Populate FinderRecommendationCache (top 50 matches)
  
- `@Async processNewLostRequest(Long lostRequestId)`
  1. Load LostRequest by id
  2. Find recent FoundRequests (last 30 days) with same pet_type
  3. Filter by distance (50km radius)
  4. Calculate score for each pair
  5. Save matches to match_queue via MatchQueueService

**Dependencies to inject:**
- MatchingAlgorithm
- LostRequestRepository
- FoundRequestRepository
- MatchQueueService
- FinderRecommendationCache

#### 3. MatchingEventListener.java
**Path:** `src/main/java/op/edu/ua/petbed/lost/application/event/MatchingEventListener.java`

**Methods:**
- `@EventListener handleLostRequestCreated(LostRequestCreatedEvent event)`
  - Call matchingService.processNewLostRequest(event.lostRequestId())
  
- `@EventListener handleFoundRequestCreated(FoundRequestCreatedEvent event)`
  - Call matchingService.processNewFoundRequest(event.foundRequestId())

**Dependencies to inject:**
- MatchingService

### Implementation Notes:

1. **Use @Async**: Both process methods should be async
2. **Transaction boundaries**: Each match should be saved in its own transaction or batch
3. **PostGIS queries**: Use native queries via JdbcTemplate for ST_Distance and similarity
4. **Distance calculation**: Returns meters from ST_Distance, convert to km
5. **Score format**: Use BigDecimal with scale 4 (0.0000 - 1.0000)
6. **Duplicate prevention**: MatchQueueService.addMatch() already checks for duplicates

### Testing Strategy:
- Unit tests for MatchingAlgorithm with fixed inputs
- Integration tests for MatchingService with Testcontainers
- Event listener tests

### Checklist:
- [x] MatchingAlgorithm with geo and text scoring
- [x] MatchingService with async processing
- [x] MatchingEventListener wiring
- [x] Code review fixes applied (see below)
- [ ] Unit tests for algorithm
- [ ] Integration tests for service
- [x] Verify all Lost module tests pass

### Code Review Fixes Applied:

#### 🔴 Critical Issues Fixed:

1. **@Async + @Transactional separation** (`MatchingService.java`)
   - Розділено на публічний `@Async` метод + приватний `@Transactional` метод
   - Уникнено проблем з проксі Spring

2. **PostGIS Point format** (`MatchingAlgorithm.java`, `MatchingService.java`)
   - Додано метод `toWkt()` для конвертації Point в WKT формат
   - Формат: `SRID=4326;POINT(x y)` замість ненадійного `toString()`

3. **N+1 Query fix** (`MatchingService.java`)
   - Використано `findAllById(ids)` для batch loading замість окремих `findById()`

4. **Specific exception handling** (`MatchingEventListener.java`)
   - Ловляться конкретні винятки: `DataAccessException`, `PetBedException`
   - Загальний `Exception` ловиться окремо для алертів
   - Додано `log.info` для отримання подій (не тільки `debug`)

5. **Added missing ErrorCodes** (`PetBedException.java`)
   - `LOST_REQUEST_NOT_FOUND`
   - `FOUND_REQUEST_NOT_FOUND`

---

## Completed Stages:
- ✅ Stage 2: Domain Entities (abfcf39)
- ✅ Stage 3: Repositories (5276083, 65e0a82)
- ✅ Stage 4: DTOs
- ✅ Stage 5: Domain Services (2766366)
- ✅ Stage 6: FinderRecommendationCache
- ✅ Stage 7: Matching Algorithm

## Pending Stages:
- ⏳ Stage 8: Event System (Cleanup Job) (current)
- ⏳ Stage 9: Telegram Form Handlers
- ⏳ Stage 10: Telegram Callback Handlers
- ⏳ Stage 11: Integration Testing
