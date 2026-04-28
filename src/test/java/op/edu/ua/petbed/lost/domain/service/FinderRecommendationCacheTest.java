package op.edu.ua.petbed.lost.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FinderRecommendationCacheTest {

    private FinderRecommendationCache underTest;
    private static final Long FINDER_ID = 123L;
    private static final Long FINDER_ID_2 = 456L;

    // Reflection fields for accessing private state
    private ConcurrentHashMap<Long, Queue<Long>> cache;
    private ConcurrentHashMap<Long, Instant> timestamps;

    @BeforeEach
    void setUp() throws Exception {
        underTest = new FinderRecommendationCache();

        // Access private fields via reflection for test verification
        Field cacheField = FinderRecommendationCache.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cache = (ConcurrentHashMap<Long, Queue<Long>>) cacheField.get(underTest);

        Field timestampsField = FinderRecommendationCache.class.getDeclaredField("timestamps");
        timestampsField.setAccessible(true);
        timestamps = (ConcurrentHashMap<Long, Instant>) timestampsField.get(underTest);
    }

    @Nested
    @DisplayName(".put()")
    class PutTests {

        @Test
        void put_storesRecommendationsInFIFOOrder() {
            // given
            List<Long> lostRequestIds = List.of(1L, 2L, 3L, 4L, 5L);

            // when
            underTest.put(FINDER_ID, lostRequestIds);

            // then
            Queue<Long> storedQueue = cache.get(FINDER_ID);
            assertThat(storedQueue).isNotNull();
            assertThat(storedQueue).containsExactlyElementsOf(lostRequestIds);
            assertThat(timestamps).containsKey(FINDER_ID);
        }

        @Test
        void put_overwritesExistingCache() {
            // given
            List<Long> originalIds = List.of(1L, 2L, 3L);
            List<Long> newIds = List.of(10L, 20L, 30L, 40L);
            underTest.put(FINDER_ID, originalIds);
            Instant originalTimestamp = timestamps.get(FINDER_ID);

            // Wait a tiny bit to ensure timestamp changes
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            underTest.put(FINDER_ID, newIds);

            // then
            Queue<Long> storedQueue = cache.get(FINDER_ID);
            assertThat(storedQueue).containsExactlyElementsOf(newIds);
            assertThat(storedQueue).doesNotContainAnyElementsOf(originalIds);
            assertThat(timestamps.get(FINDER_ID)).isAfter(originalTimestamp);
        }

        @Test
        void put_storesEmptyList() {
            // given
            List<Long> emptyList = Collections.emptyList();

            // when
            underTest.put(FINDER_ID, emptyList);

            // then
            Queue<Long> storedQueue = cache.get(FINDER_ID);
            assertThat(storedQueue).isNotNull();
            assertThat(storedQueue).isEmpty();
        }
    }

    @Nested
    @DisplayName(".pollNext()")
    class PollNextTests {

        @Test
        void pollNext_returnsItemsInFIFOOrder() {
            // given
            List<Long> lostRequestIds = List.of(1L, 2L, 3L, 4L, 5L);
            underTest.put(FINDER_ID, lostRequestIds);

            // when & then - poll all items and verify FIFO order
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(1L);
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(2L);
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(3L);
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(4L);
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(5L);
        }

        @Test
        void pollNext_removesItemFromQueue() {
            // given
            List<Long> lostRequestIds = List.of(1L, 2L, 3L);
            underTest.put(FINDER_ID, lostRequestIds);

            // when
            Optional<Long> first = underTest.pollNext(FINDER_ID);

            // then
            assertThat(first).hasValue(1L);
            Queue<Long> remainingQueue = cache.get(FINDER_ID);
            assertThat(remainingQueue).containsExactly(2L, 3L);
            assertThat(remainingQueue).hasSize(2);
        }

        @Test
        void pollNext_afterTTLExpired_returnsEmpty() {
            // given
            List<Long> lostRequestIds = List.of(1L, 2L, 3L);
            underTest.put(FINDER_ID, lostRequestIds);

            // Simulate TTL expiration by setting timestamp to 2 days ago
            timestamps.put(FINDER_ID, Instant.now().minus(Duration.ofDays(2)));

            // when
            Optional<Long> result = underTest.pollNext(FINDER_ID);

            // then
            assertThat(result).isEmpty();
            assertThat(cache).doesNotContainKey(FINDER_ID);
            assertThat(timestamps).doesNotContainKey(FINDER_ID);
        }

        @Test
        void pollNext_emptyCache_returnsEmpty() {
            // when - polling without any prior put
            Optional<Long> result = underTest.pollNext(FINDER_ID);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void pollNext_afterAllItemsPolled_returnsEmpty() {
            // given
            List<Long> lostRequestIds = List.of(1L, 2L);
            underTest.put(FINDER_ID, lostRequestIds);
            underTest.pollNext(FINDER_ID); // poll first
            underTest.pollNext(FINDER_ID); // poll second

            // when - queue is now empty
            Optional<Long> result = underTest.pollNext(FINDER_ID);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void pollNext_differentFinders_isolated() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L));
            underTest.put(FINDER_ID_2, List.of(100L, 200L));

            // when & then
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(1L);
            assertThat(underTest.pollNext(FINDER_ID_2)).hasValue(100L);
            assertThat(underTest.pollNext(FINDER_ID)).hasValue(2L);
            assertThat(underTest.pollNext(FINDER_ID_2)).hasValue(200L);
        }
    }

    @Nested
    @DisplayName(".hasRecommendations()")
    class HasRecommendationsTests {

        @Test
        void hasRecommendations_withItems_returnsTrue() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L, 3L));

            // when
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void hasRecommendations_empty_returnsFalse() {
            // when - no recommendations stored
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void hasRecommendations_afterAllPolled_returnsFalse() {
            // given
            underTest.put(FINDER_ID, List.of(1L));
            underTest.pollNext(FINDER_ID); // poll the only item

            // when
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void hasRecommendations_afterTTLExpired_returnsFalse() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L, 3L));

            // Simulate TTL expiration by setting timestamp to 2 days ago
            timestamps.put(FINDER_ID, Instant.now().minus(Duration.ofDays(2)));

            // when
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isFalse();
            assertThat(cache).doesNotContainKey(FINDER_ID);
        }

        @Test
        void hasRecommendations_afterPartialPolling_returnsTrue() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L, 3L));
            underTest.pollNext(FINDER_ID); // poll one item, 2 remain

            // when
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void hasRecommendations_withEmptyList_returnsFalse() {
            // given
            underTest.put(FINDER_ID, Collections.emptyList());

            // when
            boolean result = underTest.hasRecommendations(FINDER_ID);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName(".remove()")
    class RemoveTests {

        @Test
        void remove_clearsCache() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L, 3L));
            assertThat(cache).containsKey(FINDER_ID);
            assertThat(timestamps).containsKey(FINDER_ID);

            // when
            underTest.remove(FINDER_ID);

            // then
            assertThat(cache).doesNotContainKey(FINDER_ID);
            assertThat(timestamps).doesNotContainKey(FINDER_ID);
        }

        @Test
        void remove_nonExistentFinder_doesNothing() {
            // when - removing finder that was never added
            underTest.remove(FINDER_ID);

            // then - no exception thrown, cache remains empty
            assertThat(cache).isEmpty();
            assertThat(timestamps).isEmpty();
        }

        @Test
        void remove_afterPartialPolling_clearsRemaining() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L, 3L));
            underTest.pollNext(FINDER_ID); // poll 1, 2 and 3 remain

            // when
            underTest.remove(FINDER_ID);

            // then
            assertThat(cache).doesNotContainKey(FINDER_ID);
            Optional<Long> result = underTest.pollNext(FINDER_ID);
            assertThat(result).isEmpty();
        }

        @Test
        void remove_onlyAffectsSpecifiedFinder() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L));
            underTest.put(FINDER_ID_2, List.of(100L, 200L));

            // when
            underTest.remove(FINDER_ID);

            // then
            assertThat(cache).doesNotContainKey(FINDER_ID);
            assertThat(cache).containsKey(FINDER_ID_2);
            assertThat(underTest.hasRecommendations(FINDER_ID_2)).isTrue();
        }
    }

    @Nested
    @DisplayName(".evictExpired()")
    class EvictExpiredTests {

        @Test
        void evictExpired_removesOldEntries() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L));
            underTest.put(FINDER_ID_2, List.of(100L, 200L));

            // Set FINDER_ID's timestamp to 2 days ago (expired)
            timestamps.put(FINDER_ID, Instant.now().minus(Duration.ofDays(2)));

            // when
            underTest.evictExpired();

            // then
            assertThat(cache).doesNotContainKey(FINDER_ID);
            assertThat(timestamps).doesNotContainKey(FINDER_ID);
            // FINDER_ID_2 should still exist
            assertThat(cache).containsKey(FINDER_ID_2);
            assertThat(timestamps).containsKey(FINDER_ID_2);
        }

        @Test
        void evictExpired_keepsRecentEntries() {
            // given
            underTest.put(FINDER_ID, List.of(1L, 2L));
            underTest.put(FINDER_ID_2, List.of(100L, 200L));

            // Both have current timestamps (fresh entries)

            // when
            underTest.evictExpired();

            // then - both entries should remain
            assertThat(cache).containsKey(FINDER_ID);
            assertThat(cache).containsKey(FINDER_ID_2);
            assertThat(timestamps).containsKey(FINDER_ID);
            assertThat(timestamps).containsKey(FINDER_ID_2);
        }

        @Test
        void evictExpired_handlesEmptyCache() {
            // when - evicting from empty cache
            underTest.evictExpired();

            // then - no exception, cache remains empty
            assertThat(cache).isEmpty();
            assertThat(timestamps).isEmpty();
        }

        @Test
        void evictExpired_removesMultipleExpiredEntries() {
            // given
            Long finderId3 = 789L;
            underTest.put(FINDER_ID, List.of(1L));
            underTest.put(FINDER_ID_2, List.of(100L));
            underTest.put(finderId3, List.of(1000L));

            // Set two entries as expired
            timestamps.put(FINDER_ID, Instant.now().minus(Duration.ofDays(2)));
            timestamps.put(FINDER_ID_2, Instant.now().minus(Duration.ofHours(25)));
            // finderId3 remains fresh

            // when
            underTest.evictExpired();

            // then
            assertThat(cache).doesNotContainKey(FINDER_ID);
            assertThat(cache).doesNotContainKey(FINDER_ID_2);
            assertThat(cache).containsKey(finderId3);
        }

        @Test
        void evictExpired_removesSlightlyExpired() {
            // given
            underTest.put(FINDER_ID, List.of(1L));

            // Set timestamp to just over TTL (1 day + 1 second)
            timestamps.put(FINDER_ID, Instant.now().minus(Duration.ofDays(1)).minus(Duration.ofSeconds(1)));

            // when
            underTest.evictExpired();

            // then - should be evicted
            assertThat(cache).doesNotContainKey(FINDER_ID);
        }
    }
}