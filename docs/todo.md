# Lost Module - Implementation Plan

## Phase 1: Foundation

### Stage 2: Domain Entities (COMPLETED ✅)
**Status:** Completed - Committed as `abfcf39`

---

### Stage 3: Repositories (COMPLETED ✅)
**Status:** Completed
**Goal:** Create Spring Data JPA Repositories and Tests for Lost module

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

## Next Stage: Stage 4 - DTOs
**Status:** Pending
**Goal:** Create DTOs for Lost module

#### Planned Tasks:
1. LostRequestDTO
2. FoundRequestDTO
3. MatchRecommendationDTO

---

## Completed Stages:
- ✅ Stage 2: Domain Entities
- ✅ Stage 3: Repositories

## Pending Stages:
- ⏳ Stage 4: DTOs (current)
- ⏳ Stage 5: Domain Services
- ⏳ Stage 6: FinderRecommendationCache
- ⏳ Stage 7: Matching Algorithm
- ⏳ Stage 8: Event System
- ⏳ Stage 9: Cleanup Job
- ⏳ Stage 10: Telegram Form Handlers
- ⏳ Stage 11: Telegram Callback Handlers
- ⏳ Stage 12: Testing
