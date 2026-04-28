# Lost Module - Implementation Plan

## Phase 1: Foundation

### Stage 2: Domain Entities (COMPLETED ✅)
**Status:** Completed
**Goal:** Create JPA entities for LostRequest, FoundRequest, MatchQueueEntry

#### Completed Tasks:

1. **Create module structure** ✅
   - [x] Create directories: `src/main/java/op/edu/ua/petbed/lost/domain/model/`
   - [x] Create directories: `src/main/java/op/edu/ua/petbed/lost/domain/repository/`
   - [x] Create directories: `src/main/java/op/edu/ua/petbed/lost/domain/service/`

2. **Create Enums** ✅ (Agent: @developer)
   - [x] `LostRequestStatus` - ACTIVE, CANCELLED
   - [x] `ViewingStatus` - NEW, VIEWED, REJECTED

3. **Create LostRequest Entity** ✅ (Agent: @developer)
   - [x] Class with @Entity, @Table(name = "lost_requests")
   - [x] Fields: id, pet (OneToOne), contactInfo, lastSeenLocation (Point), petType, searchText, status
   - [x] Factory method `create(Pet pet, String contactInfo, Point location)`
   - [x] Domain methods: `cancel()`, `isActive()`
   - [x] Proper annotations: @NullMarked, Lombok, JPA
   - [x] equals() and hashCode() following project pattern

4. **Create FoundRequest Entity** ✅ (Agent: @developer)
   - [x] Class with @Entity, @Table(name = "found_requests")
   - [x] Fields: id, finder (ManyToOne), photoUrl, petType, location (Point), description
   - [x] Factory method `create(User finder, String photoUrl, PetType petType, Point location, String description)`
   - [x] Proper annotations: @NullMarked, Lombok, JPA
   - [x] equals() and hashCode() following project pattern

5. **Create MatchQueueEntry Entity** ✅ (Agent: @developer)
   - [x] Class with @Entity, @Table(name = "match_queue")
   - [x] Fields: id, lostRequest (ManyToOne), foundRequest (ManyToOne), score (BigDecimal), viewingStatus, viewedBy
   - [x] Factory method `create(LostRequest lost, FoundRequest found, BigDecimal score)`
   - [x] Domain methods: `markAsViewed(String viewedBy)`, `markAsRejected()`
   - [x] Proper annotations: @NullMarked, Lombok, JPA
   - [x] equals() and hashCode() following project pattern

6. **Review & Verify** ✅ (Agent: @reviewer)
   - [x] All entities compile
   - [x] JPA annotations are correct
   - [x] Follows project code style
   - [x] Proper null-safety with JSpecify

#### Created Files:
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/model/LostRequestStatus.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/model/ViewingStatus.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/model/LostRequest.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/model/FoundRequest.java`
- `/home/nolan/Code/pet-bed/src/main/java/op/edu/ua/petbed/lost/domain/model/MatchQueueEntry.java`

#### Verification:
- ✅ All entities compile successfully
- ✅ Build: SUCCESSFUL
- ✅ Code follows project patterns (Pet.java, User.java)
- ✅ Proper JPA annotations
- ✅ Factory methods with validation
- ✅ Domain methods for business logic
- ✅ @NullMarked for null safety
- ✅ equals() and hashCode() with Hibernate proxy support

#### Dependencies:
- Requires: Existing Pet and User entities (available)
- Required by: Stage 3 (Repositories)

#### Notes:
- All entities extend AbstractAuditableEntity
- Use PostGIS Point type for locations (org.locationtech.jts.geom.Point)
- Follow existing patterns from Pet.java and User.java
- Use @NullMarked on all classes
- Use factory methods for creation, not setters (DDD pattern)

---

## Next Stage: Stage 3 - Repositories
**Status:** Pending
**Goal:** Create Spring Data JPA Repositories

#### Planned Tasks:
1. LostRequestRepository
2. FoundRequestRepository
3. MatchQueueRepository
