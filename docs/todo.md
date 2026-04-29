# Implementation Plan: "Я знайшов тварину" (I Found a Pet)

## Goal
Implement complete flow for finders (people who found a pet) to:
- Create a found request with photo, location, and pet characteristics
- View potential matches (lost pet requests from owners)
- Navigate through recommendations

## Analysis Summary

### What Already Exists
- `FoundRequestService` interface and implementation
- `FoundRequest` entity and repository
- `MatchingService` with `processNewFoundRequest()` method
- `FinderRecommendationCache` (in-memory cache for finders)
- `FoundRequestCreatedEvent` for triggering matching
- `MatchingEventListener` with `@TransactionalEventListener`

### What's Missing
- `LOST_FOUND` callback handler (entry point)
- `CREATE_FOUND_REQUEST` form submission handler
- `LOST_FOUND_MATCHES` callback handler (view recommendations)
- Navigation handlers for recommendation viewing

---

## Implementation Steps

### Step 1: Create LOST_FOUND Callback Handler ✅ COMPLETE
**File:** `telegram/callback/handler/lost/LostFoundCallbackHandler.java`

**Behavior:**
- Entry point when user clicks "Я знайшов тварину"
- Starts `CREATE_FOUND_REQUEST` form
- Form return callback: `MENU` (after completion)

**Form Steps (CREATE_FOUND_REQUEST):**
1. Pet type (choice: DOG, CAT, OTHER)
2. Photo (required)
3. Location (required)
4. Breed (text, skippable)
5. Color (text, skippable)
6. Coat type (text, skippable)
7. Sex (choice: MALE, FEMALE, UNKNOWN - skippable)
8. Size (choice: SMALL, MEDIUM, LARGE - skippable)
9. Special features (text, skippable)

**Dependencies:** `FormService`

**Status:** Implemented and tested with 5 unit tests.

---

### Step 2: Create CREATE_FOUND_REQUEST Form Handler ✅ COMPLETE
**File:** `telegram/form/handler/CreateFoundRequestHandler.java`

**Behavior:**
- Aggregates all text fields (breed, color, coat, sex, size, features) into `description`
- Calls `foundRequestService.create()` with:
  - `finderId`: current user
  - `photoUrl`: from step 2
  - `petType`: from step 1
  - `location`: from step 3
  - `description`: aggregated text
- Returns success message with two buttons:
  - "🔍 Переглянути рекомендації" → `LOST_FOUND_MATCHES`
  - "⬅️ Повернутись" → `MENU` (clears cache, recommendations become unavailable)

**Important:** Message must warn user that clicking "Повернутись" will make recommendations unavailable forever

**Dependencies:** `FoundRequestService`, `FinderRecommendationCache`

**Status:** Implemented and tested with 8 unit tests.

---

### Step 3: Create LOST_FOUND_MATCHES Callback Handler ✅ COMPLETE
**File:** `telegram/callback/handler/lost/LostFoundMatchesCallbackHandler.java`

**Behavior:**
- Gets next recommendation from `FinderRecommendationCache.pollNext(finderId)`
- If no recommendations left or cache expired:
  - Show "Ви переглянули всі доступні анкети" message
  - Clear cache
  - Back button to `MENU`
- If recommendation exists:
  - Get `LostRequestDTO` by ID
  - **Only show if contact_info is present** (skip if no contact)
  - Display:
    - Pet photo
    - Pet info (name, breed, color, etc.)
    - Owner contact info
    - Distance
  - Keyboard:
    - "Наступна" → `LOST_FOUND_MATCHES_NEXT` (same handler, new message)
    - "Повернутись" → `MENU` (removes keyboard, sends new message, clears cache)

**Pattern:** Follow `FeedViewNextCallbackHandler` - remove keyboard from previous message, send new message

**Dependencies:** `FinderRecommendationCache`, `LostRequestService`, `TelegramMessageService`

**Status:** Implemented and tested with 11 unit tests.

---

### Step 4: Add Required Method to FinderRecommendationCache ✅ COMPLETE
**Check if exists:** `hasRecommendations(finderId)` or `getNext(finderId)`

Methods added via `FinderRecommendationService` interface:
```java
public Optional<Long> pollNext(Long finderId) // already exists
public boolean hasRecommendations(Long finderId) // check if queue not empty
public void remove(Long finderId) // clear cache for user
```

**Status:** Methods implemented and tested with 26 unit tests.

---

### Step 5: Verify MatchingService.populateFinderCache ✅ COMPLETE
**Check:** In `processNewFoundRequest()`, after computing matches, does it call:
```java
finderRecommendationCache.put(finderId, lostRequestIds);
```

**Status:** Verified - cache is populated correctly in `MatchingService.processNewFoundRequest()`.

---

## Test Cases

### TC-1: Create Found Request
1. Click "Я знайшов тварину"
2. Fill all form steps
3. Submit
4. Verify success message with two buttons
5. Verify `FoundRequest` created in DB
6. Verify `FoundRequestCreatedEvent` published

### TC-2: View Recommendations
1. After creating found request, click "Переглянути рекомендації"
2. Verify first recommendation shown (if matches exist)
3. Verify keyboard has "Наступна" and "Повернутись"
4. Verify cache entry exists

### TC-3: Navigate Recommendations
1. View first recommendation
2. Click "Наступна"
3. Verify previous message keyboard removed
4. Verify new message with next recommendation
5. Verify queue size decreased

### TC-4: No Contact = Skip
1. Create lost request without contact info
2. Create found request that matches
3. Verify this lost request is NOT shown to finder

### TC-5: Return Clears Cache
1. View recommendations
2. Click "Повернутись"
3. Verify cache cleared
4. Verify cannot view recommendations again

---

## Files to Create/Modify

### New Files
- `telegram/callback/handler/lost/LostFoundCallbackHandler.java`
- `telegram/form/handler/CreateFoundRequestHandler.java`
- `telegram/callback/handler/lost/LostFoundMatchesCallbackHandler.java`

### Modify If Needed
- `lost/domain/service/FinderRecommendationCache.java` - add `hasRecommendations()`, `remove()`
- `lost/application/matching/MatchingService.java` - ensure cache is populated

---

## Notes

- **In-memory cache:** Uses `ConcurrentHashMap` with TTL 1 day
- **FIFO queue:** Each view removes one entry from queue
- **No persistence:** If app restarts, cache is lost (acceptable for this use case)
- **Contact filtering:** Only show lost requests with contact_info to finders
- **One-time viewing:** Once finder clicks "Повернутись", they cannot view recommendations again

---

## Completion Summary

### ✅ All Implementation Steps Complete

| Step | Task | Status | Tests |
|------|------|--------|-------|
| 1 | LOST_FOUND Callback Handler | ✅ Complete | 5 |
| 2 | CREATE_FOUND_REQUEST Form Handler | ✅ Complete | 8 |
| 3 | LOST_FOUND_MATCHES Callback Handler | ✅ Complete | 11 |
| 4 | FinderRecommendationCache Methods | ✅ Complete | 26 |
| 5 | MatchingService Cache Population | ✅ Complete | Verified |
| **Total** | | | **50** |

### Files Created

**Handler Files:**
- `src/main/java/op/edu/ua/petbed/telegram/callback/handler/lost/LostFoundCallbackHandler.java`
- `src/main/java/op/edu/ua/petbed/telegram/form/handler/CreateFoundRequestHandler.java`
- `src/main/java/op/edu/ua/petbed/telegram/callback/handler/lost/LostFoundMatchesCallbackHandler.java`
- `src/main/java/op/edu/ua/petbed/lost/FinderRecommendationService.java`

**Test Files:**
- `src/test/java/op/edu/ua/petbed/telegram/callback/handler/lost/LostFoundCallbackHandlerTest.java`
- `src/test/java/op/edu/ua/petbed/telegram/form/handler/CreateFoundRequestHandlerTest.java`
- `src/test/java/op/edu/ua/petbed/telegram/callback/handler/lost/LostFoundMatchesCallbackHandlerTest.java`
- `src/test/java/op/edu/ua/petbed/lost/domain/service/FinderRecommendationCacheTest.java`

### Key Features Implemented
- Complete form flow with 9 steps for creating found requests
- Automated matching algorithm triggering on form submission
- FIFO queue-based recommendation viewing for finders
- Contact filtering (only show lost requests with contact info)
- Cache clearing on "Return" button press
- Comprehensive test coverage (50 tests total)

---

*Created: 2025-04-29*
*Completed: 2025-04-29*
*Feature: "Я знайшов тварину" (I Found a Pet)*
