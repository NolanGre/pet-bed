# Lost Module — Implementation Tasks

## Phase 1: Database Schema (Liquibase) ✅ COMPLETED

### Task 1.1: Create Liquibase changelog 0003-lost-module-tables.xml ✅

Create database schema for Lost module with three tables:

#### lost_requests table
- id (BIGINT, PK)
- pet_id (BIGINT, FK → pets.id, UNIQUE, ON DELETE CASCADE)
- contact_info (VARCHAR(255), NOT NULL)
- last_seen_location (GEOGRAPHY(POINT, 4326), NOT NULL)
- pet_type (VARCHAR(50), NOT NULL)
- search_text (TEXT, NOT NULL) — for pg_trgm search
- status (VARCHAR(50), NOT NULL) — ACTIVE, CANCELLED
- created_at, updated_at (TIMESTAMP)

**Indexes:**
- idx_lost_pet_id
- idx_lost_type
- idx_lost_status
- idx_lost_location (GIST)
- idx_lost_search_text (GIN with gin_trgm_ops)

#### found_requests table
- id (BIGINT, PK)
- finder_id (BIGINT, FK → users.id)
- photo_url (VARCHAR(255), NOT NULL)
- pet_type (VARCHAR(50), NOT NULL)
- location (GEOGRAPHY(POINT, 4326), NOT NULL)
- description (TEXT, NOT NULL) — aggregated text for search
- created_at, updated_at (TIMESTAMP)

**Indexes:**
- idx_found_finder_id
- idx_found_pet_type
- idx_found_location (GIST)
- idx_found_description_trgm (GIN with gin_trgm_ops)
- idx_found_created_at

#### match_queue table
- id (BIGINT, PK)
- lost_request_id (BIGINT, FK → lost_requests.id, ON DELETE CASCADE)
- found_request_id (BIGINT, FK → found_requests.id, ON DELETE CASCADE)
- score (NUMERIC(7,4), NOT NULL) — 0.0000 to 1.0000
- viewing_status (VARCHAR(50), NOT NULL) — NEW, VIEWED, REJECTED
- viewed_by (VARCHAR(20)) — OWNER or FINDER
- created_at, updated_at (TIMESTAMP)
- UNIQUE(lost_request_id, found_request_id)

**Indexes:**
- idx_match_lost_id
- idx_match_found_id
- idx_match_status
- idx_match_score

### Acceptance Criteria:
- [x] Changelog file created at `src/main/resources/db/changelog/0007-lost-module-tables.xml`
- [x] All three tables defined with correct columns and constraints
- [x] All indexes created (including PostGIS GIST and pg_trgm GIN indexes)
- [x] Foreign keys with proper ON DELETE CASCADE
- [x] Master changelog includes the new file
- [x] Migration runs successfully on local PostgreSQL with PostGIS

### Files Created/Updated:
1. `src/main/resources/db/changelog/0007-lost-module-tables.xml` - Liquibase changelog
2. `src/main/resources/db/changelog/master-changelog.xml` - Updated to include new changelog

### Tests Created:
1. `src/test/java/op/edu/ua/petbed/lost/repository/LostModuleSchemaTest.java`
   - 17 comprehensive integration tests
   - Tests for table schema, indexes, constraints
   - Tests for CRUD operations
   - Tests for spatial queries (PostGIS)
   - Tests for text similarity (pg_trgm)
   - Tests for CHECK constraints validation

**Test Coverage:**
- ✅ Table schema verification (lost_requests, found_requests, match_queue)
- ✅ Index verification (all 15 indexes)
- ✅ Foreign key constraints verification
- ✅ CHECK constraints verification
- ✅ UNIQUE constraints verification
- ✅ Extension availability (pg_trgm, postgis)
- ✅ Geography columns type verification
- ✅ CRUD operations on all tables
- ✅ Spatial distance queries
- ✅ Text similarity queries
- ✅ Constraint violation handling

### Notes:
- File named `0007-lost-module-tables.xml` (not 0003) because changelogs 0003-0006 already existed
- Uses `autoIncrement="true"` instead of `GENERATED ALWAYS AS IDENTITY` for Liquibase compatibility
- PostGIS geography columns added via raw SQL (`<sql>` tags)
- pg_trgm GIN indexes created via raw SQL
- CHECK constraints added via raw SQL
- Includes preConditions to drop existing tables from 0002 if they exist

---

## Next Phase: Domain Entities

### Task 2.1: Create LostRequest entity
### Task 2.2: Create FoundRequest entity
### Task 2.3: Create MatchQueueEntry entity
### Task 2.4: Create enums (LostRequestStatus, ViewingStatus)
