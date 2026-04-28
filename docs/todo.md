# Lost Module - Implementation Plan

## Phase 1: Foundation

### Stage 2: Domain Entities (COMPLETED ✅)
**Status:** Completed - Committed as `abfcf39`

---

### Stage 3: Repositories (COMPLETED ✅)
**Status:** Completed - Committed as `5276083`, Fixed tests after entity changes
**Goal:** Create Spring Data JPA Repositories and Tests for Lost module

#### Entity Changes Applied:
User modified entities to simplify the model:
1. **LostRequest**: Removed `status` field, changed `pet` relationship to `petId` (Long)
2. **FoundRequest**: Changed `finder` relationship to `finderId` (Long)
3. **MatchQueueEntry**: Removed `viewedBy` field, changed `REJECTED` to `CONFIRMED`
4. **Deleted**: `LostRequestStatus.java` enum

#### Database Migration (0008-update-lost-module-entities.xml):
Created migration to align DB with new entity structure:
- Dropped FK constraints: `fk_lost_requests_pet`, `fk_found_requests_finder`
- Dropped `status` column from `lost_requests`
- Dropped `viewed_by` column from `match_queue`
- Updated CHECK constraint: `('NEW', 'VIEWED', 'CONFIRMED')` (removed REJECTED)

#### Test Fixes Applied:
1. **LostRequestRepositoryTest**: Updated to use `PetDTO` instead of `Pet` entity
2. **FoundRequestRepositoryTest**: Updated to use `finderId` (Long) instead of `User`
3. **MatchQueueRepositoryTest**: 
   - Updated to use `PetDTO` and `finderId`
   - Changed `ViewingStatus.REJECTED` to `ViewingStatus.VIEWED`
   - Removed `markAsRejected()` calls
4. **FoundRequestRepository**: Fixed SQL column name `fr.type` → `fr.pet_type`

#### Completed Tasks:

1. **LostRequestRepository** ✅ (Agent: @developer)
   - [x] Interface extends JpaRepository<LostRequest, Long>
   - [x] `findByPetId(Long petId)` - find lost request by pet
   - [x] `findByPetOwnerIdAndStatus(Long ownerId, LostRequestStatus status)` - find by owner and status
   - [x] `findByStatusAndPetType(LostRequestStatus status, PetType petType)` - for matching
   - [x] `existsByPetId(Long petId)` - check if pet has lost request
   - [x] `deleteByPetId(Long petId)` - cascade delete

2. **FoundRequestRepository** ✅ (Agent: @developer)
   - [x] Interface extends JpaRepository<FoundRequest, Long>
   - [x] `findByFinderId(Long finderId)` - find all by finder
   - [x] `findRecentByPetTypeAndLocation(String petType, Point location, Timestamp since)` - native query with PostGIS ST_DWithin
   - [x] `deleteOlderThan(Instant date)` - for cleanup job

3. **MatchQueueRepository** ✅ (Agent: @developer)
   - [x] Interface extends JpaRepository<MatchQueueEntry, Long>
   - [x] `findByLostRequestIdAndViewingStatusIn(Long lostRequestId, List<ViewingStatus> statuses)` - for owner recommendations
   - [x] `findByLostRequestIdOrderByScoreDesc(Long lostRequestId)` - sorted by score
   - [x] `existsByLostRequestIdAndFoundRequestId(Long lostRequestId, Long foundRequestId)` - prevent duplicates
   - [x] `deleteByLostRequestId(Long lostRequestId)` - cascade delete

4. **Tests** ✅ (Agent: @developer)
   - [x] LostRequestRepositoryTest (11 тестів в 5 @Nested класах)
   - [x] FoundRequestRepositoryTest (10 тестів в 3 @Nested класах)
   - [x] MatchQueueRepositoryTest (14 тестів в 4 @Nested класах)

5. **Review & Verify** ✅ (Agent: @reviewer + Manual)
   - [x] All repositories compile
   - [x] All tests pass
   - [x] Correct method signatures
   - [x] Proper native queries for PostGIS
   - [x] Follow project patterns

#### Created Files:
**Repositories:**
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/repository/LostRequestRepository.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/repository/FoundRequestRepository.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/repository/MatchQueueRepository.java`

**Tests:**
- `/home/nolan/Code/pet-bed/src/test/java/op/edu/ua/petbed/lost/domain/repository/LostRequestRepositoryTest.java`
- `/home/nolan/Code/pet-bed/src/test/java/op/edu/ua/petbed/lost/domain/repository/FoundRequestRepositoryTest.java`
- `/home/nolan/Code/pet-bed/src/test/java/op/edu/ua/petbed/lost/domain/repository/MatchQueueRepositoryTest.java`

#### Verification:
- ✅ All repositories compile successfully
- ✅ All 35 tests pass (11 + 10 + 14)
- ✅ Build: SUCCESSFUL
- ✅ Correct Spring Data JPA patterns
- ✅ Native PostGIS query with ST_DWithin (50km radius)
- ✅ @Modifying for delete operations
- ✅ Follows project repository patterns
- ✅ Tests use @DataJpaTest with PostgresTestContainer
- ✅ @Nested classes for test organization

#### Dependencies:
- Requires: Stage 2 entities
- Required by: Stage 4 (DTOs and Services)

---

### Stage 4: DTOs (COMPLETED ✅)
**Status:** Completed
**Goal:** Create DTOs for Lost module

#### Completed Tasks:
1. **LostRequestDTO** ✅
   - Fields: id, petId, contactInfo, lastSeenLocation, petType, searchText, createdAt
   - `fromEntity()` with null validation using PetBedException

2. **FoundRequestDTO** ✅
   - Fields: id, finderId, photoUrl, petType, location, description, createdAt
   - `fromEntity()` with null validation using PetBedException

3. **MatchRecommendationDTO** ✅
   - Fields: matchQueueId, lostRequestId, foundRequestId, score, photoUrl, description, location, foundRequestCreatedAt, distanceKm
   - Used for displaying match recommendations

#### Created Files:
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/dto/LostRequestDTO.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/dto/FoundRequestDTO.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/dto/MatchRecommendationDTO.java`

---

### Stage 5: Domain Services (COMPLETED ✅)
**Status:** Completed
**Goal:** Create service interfaces and implementations

#### Completed Tasks:

1. **LostRequestService** - interface + implementation ✅
   - Interface: `LostRequestService.java` at module root (public API)
   - Implementation: `LostRequestServiceImpl.java` in `domain/service/`
   - Event: `LostRequestCreatedEvent.java` in `application/event/`
   - Methods implemented:
     - `create(Long petId, String contactInfo, Point location)` - checks for duplicates, creates request, publishes event
     - `findById(Long id)` - finds and returns DTO
     - `findActiveByOwnerId(Long ownerId)` - finds all lost requests for owner's pets
     - `cancel(Long lostRequestId)` - deletes lost request by petId
     - `existsByPetId(Long petId)` - delegates to repository

#### Created Files:
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/LostRequestService.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/service/LostRequestServiceImpl.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/event/LostRequestCreatedEvent.java`

#### Verification:
- ✅ All files compile successfully
- ✅ All existing tests pass
- ✅ Follows Spring Modulith public API pattern (interface at module root)
- ✅ Uses `@NullMarked`, `@RequiredArgsConstructor`, `@Transactional`
- ✅ Proper error handling with `PetBedException` and appropriate `ErrorCode`
- ✅ Publishes `LostRequestCreatedEvent` for async matching
- ✅ Uses `entity.getIdOrThrow()` pattern
- ✅ SLF4J logging for operations

---

### Stage 5: Domain Services (COMPLETED ✅)
**Status:** Completed
**Goal:** Create service interfaces and implementations

#### Completed Tasks:

1. **LostRequestService** - interface + implementation ✅
   - Interface: `LostRequestService.java` at module root (public API)
   - Implementation: `LostRequestServiceImpl.java` in `domain/service/`
   - Event: `LostRequestCreatedEvent.java` in `application/event/`
   - Methods implemented:
     - `create(Long petId, String contactInfo, Point location)` - checks for duplicates, creates request, publishes event
     - `findById(Long id)` - finds and returns DTO
     - `findActiveByOwnerId(Long ownerId)` - finds all lost requests for owner's pets
     - `cancel(Long lostRequestId)` - deletes lost request by petId
     - `existsByPetId(Long petId)` - delegates to repository

2. **FoundRequestService** - interface + implementation ✅
   - Interface: `FoundRequestService.java` at module root (public API)
   - Implementation: `FoundRequestServiceImpl.java` in `domain/service/`
   - Event: `FoundRequestCreatedEvent.java` in `application/event/`
   - Methods implemented:
     - `create(Long finderId, String photoUrl, PetType petType, Point location, String description)`
     - `findById(Long id)`
     - `findByFinderId(Long finderId)`

3. **MatchQueueService** - implementation ✅
   - Implementation: `MatchQueueService.java` in `domain/service/`
   - Methods:
     - `addMatch(LostRequest lost, FoundRequest found, BigDecimal score)` - prevents duplicates
     - `getRecommendationsForOwner(Long lostRequestId)` - with distance calculation via PostGIS
     - `markAsViewed(Long matchQueueId)` - marks entry as viewed
     - `markAsConfirmed(Long matchQueueId)` - marks entry as confirmed

4. **Entity Updates** ✅
   - Updated `MatchQueueEntry.markAsViewed()` - removed viewedBy parameter
   - Added `MatchQueueEntry.markAsConfirmed()` domain method
   - Fixed test to use new method signature

5. **Pet Status Integration** ✅
   - Added `PetService.updateStatus(Long petId, PetStatus status)` method
   - Added `Pet.changeStatus(PetStatus)` domain method
   - Updated `LostRequestServiceImpl.create()` - changes Pet status to IN_LOST
   - Updated `LostRequestServiceImpl.cancel()` - resets Pet status to DEFAULT

#### Created Files:
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/LostRequestService.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/service/LostRequestServiceImpl.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/event/LostRequestCreatedEvent.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/FoundRequestService.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/service/FoundRequestServiceImpl.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/application/event/FoundRequestCreatedEvent.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/service/MatchQueueService.java`

#### Verification:
- ✅ All files compile successfully
- ✅ All 35 Lost module tests pass
- ✅ Follows Spring Modulith public API pattern (interface at module root)
- ✅ Uses `@NullMarked`, `@RequiredArgsConstructor`, `@Transactional`
- ✅ Proper error handling with `PetBedException`
- ✅ Event-driven architecture with `ApplicationEventPublisher`
- ✅ DTOs in `common.dto.lost` package (public API)

---

### Stage 6: FinderRecommendationCache (COMPLETED ✅)
**Status:** Completed
**Goal:** Create in-memory cache for finder recommendations

#### Completed Tasks:

1. **FinderRecommendationCache** ✅
   - File: `src/main/java/op/edu/ua/petbed/lost/domain/service/FinderRecommendationCache.java`
   - Thread-safe implementation using `ConcurrentHashMap` and `ConcurrentLinkedQueue`
   - TTL: 1 day with automatic eviction
   - FIFO behavior for recommendations
   - Methods:
     - `put(Long finderId, List<Long> lostRequestIds)` - store recommendations
     - `pollNext(Long finderId)` - get and remove next recommendation (FIFO)
     - `hasRecommendations(Long finderId)` - check if cache has items
     - `remove(Long finderId)` - clear cache for user
     - `@Scheduled(fixedRate = 3600000)` evictExpired() - hourly cleanup

2. **Unit Tests** ✅
   - File: `src/test/java/op/edu/ua/petbed/lost/domain/service/FinderRecommendationCacheTest.java`
   - 25 tests covering all methods and edge cases
   - Tests for TTL expiration, FIFO order, thread isolation

#### Verification:
- ✅ All Lost module tests pass (60+ tests)
- ✅ Thread-safe implementation
- ✅ TTL and FIFO behavior as specified

---

## Next Stage: Stage 7 - Matching Algorithm
**Status:** Pending
**Goal:** Implement matching algorithm and event listeners

#### Planned Tasks:
- **MatchingAlgorithm** - calculate match score between lost and found
  - Geographic component (40%): linear scale, 50km = 0%, 0km = 100%
  - Text component (60%): pg_trgm.similarity()
- **MatchingService** - async matching via events
  - `processNewFoundRequest()` - find matches for new found request
  - `processNewLostRequest()` - find matches for new lost request
- **MatchingEventListener** - event listeners
  - `handleLostRequestCreated()` - listen for LostRequestCreatedEvent
  - `handleFoundRequestCreated()` - listen for FoundRequestCreatedEvent

---

## Completed Stages:
- ✅ Stage 2: Domain Entities (abfcf39)
- ✅ Stage 3: Repositories (5276083, 65e0a82)
- ✅ Stage 4: DTOs
- ✅ Stage 5: Domain Services (2766366)
- ✅ Stage 6: FinderRecommendationCache

## Pending Stages:
- ⏳ Stage 7: Matching Algorithm (current)
- ⏳ Stage 8: Event System
- ⏳ Stage 9: Cleanup Job
- ⏳ Stage 10: Telegram Form Handlers
- ⏳ Stage 11: Telegram Callback Handlers
- ⏳ Stage 12: Integration Testing
