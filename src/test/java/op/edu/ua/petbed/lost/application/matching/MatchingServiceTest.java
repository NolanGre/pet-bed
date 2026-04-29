package op.edu.ua.petbed.lost.application.matching;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import op.edu.ua.petbed.lost.domain.repository.LostRequestRepository;
import op.edu.ua.petbed.lost.domain.service.FinderRecommendationCache;
import op.edu.ua.petbed.lost.domain.service.MatchQueueService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchingServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    MatchingAlgorithm matchingAlgorithm;

    @Mock
    LostRequestRepository lostRequestRepository;

    @Mock
    FoundRequestRepository foundRequestRepository;

    @Mock
    MatchQueueService matchQueueService;

    @Mock
    FinderRecommendationCache cache;

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    MatchingService underTest;

    // Helper methods -----------------------------------------------------------

    private static Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static PetDTO createPetDTO(Long id, PetType petType) {
        return new PetDTO(
                id,
                1L,
                "Buddy",
                petType,
                "photo123",
                "Labrador",
                "Brown",
                "Solid",
                5,
                PetSex.MALE,
                PetSize.MEDIUM,
                "White spot on chest",
                PetStatus.DEFAULT
        );
    }

    private static LostRequest createLostRequest(Long id, Long petId, PetType petType, Point location) {
        PetDTO pet = createPetDTO(petId, petType);
        LostRequest request = LostRequest.create(pet, "test@example.com", location);
        ReflectionTestUtils.setField(request, "id", id);
        return request;
    }

    private static FoundRequest createFoundRequest(Long id, Long finderId, PetType petType, Point location) {
        FoundRequest request = FoundRequest.create(finderId, "photo123", petType, location, "Brown dog found");
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "createdAt", Instant.now());
        return request;
    }

    // processNewFoundRequestInternal tests -------------------------------------

    @Nested
    class ProcessNewFoundRequestInternal {

        @Test
        void processNewFoundRequest_withValidData_findsMatchesAndPopulatesCache() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            // Mock JDBC query to return lost request IDs within radius
            Long lostId1 = 10L;
            Long lostId2 = 20L;
            List<Long> candidateIds = List.of(lostId1, lostId2);
            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(candidateIds);

            // Mock batch load of lost requests
            Point lostLocation1 = createPoint(30.50, 50.43);
            Point lostLocation2 = createPoint(30.53, 50.46);
            LostRequest lost1 = createLostRequest(lostId1, 101L, petType, lostLocation1);
            LostRequest lost2 = createLostRequest(lostId2, 102L, petType, lostLocation2);
            given(lostRequestRepository.findAllById(candidateIds))
                    .willReturn(List.of(lost1, lost2));

            // Mock matching algorithm scores
            BigDecimal score1 = BigDecimal.valueOf(0.85);
            BigDecimal score2 = BigDecimal.valueOf(0.72);
            given(matchingAlgorithm.calculateScore(lost1, found)).willReturn(score1);
            given(matchingAlgorithm.calculateScore(lost2, found)).willReturn(score2);

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then
            verify(foundRequestRepository).findById(foundRequestId);
            verify(jdbcTemplate).queryForList(any(), eq(Long.class), any(), any(), any());
            verify(lostRequestRepository).findAllById(candidateIds);
            verify(matchingAlgorithm).calculateScore(lost1, found);
            verify(matchingAlgorithm).calculateScore(lost2, found);
            verify(matchQueueService).addMatch(lost1, found, score1);
            verify(matchQueueService).addMatch(lost2, found, score2);
            verify(cache).put(finderId, List.of(lostId1, lostId2));
        }

        @Test
        void processNewFoundRequest_withNoCandidates_skipsProcessing() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            // Mock JDBC query to return empty list (no candidates in radius)
            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(Collections.emptyList());
            given(lostRequestRepository.findAllById(Collections.emptyList()))
                    .willReturn(Collections.emptyList());

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then
            verify(foundRequestRepository).findById(foundRequestId);
            verify(jdbcTemplate).queryForList(any(), eq(Long.class), any(), any(), any());
            verify(lostRequestRepository).findAllById(Collections.emptyList());
            verifyNoInteractions(matchingAlgorithm);
            verifyNoInteractions(matchQueueService);
            verify(cache).put(finderId, Collections.emptyList());
        }

        @Test
        void processNewFoundRequest_filtersZeroScores() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            Long lostId1 = 10L;
            Long lostId2 = 20L;
            Long lostId3 = 30L;
            List<Long> candidateIds = List.of(lostId1, lostId2, lostId3);
            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(candidateIds);

            Point location = createPoint(30.50, 50.43);
            LostRequest lost1 = createLostRequest(lostId1, 101L, petType, location);
            LostRequest lost2 = createLostRequest(lostId2, 102L, petType, location);
            LostRequest lost3 = createLostRequest(lostId3, 103L, petType, location);
            given(lostRequestRepository.findAllById(candidateIds))
                    .willReturn(List.of(lost1, lost2, lost3));

            // Scores: 0.85, 0.00, 0.72 - middle one should be filtered out
            BigDecimal score1 = BigDecimal.valueOf(0.85);
            BigDecimal score2 = BigDecimal.ZERO;
            BigDecimal score3 = BigDecimal.valueOf(0.72);
            given(matchingAlgorithm.calculateScore(lost1, found)).willReturn(score1);
            given(matchingAlgorithm.calculateScore(lost2, found)).willReturn(score2);
            given(matchingAlgorithm.calculateScore(lost3, found)).willReturn(score3);

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then
            verify(matchQueueService).addMatch(lost1, found, score1);
            verify(matchQueueService, never()).addMatch(lost2, found, score2);
            verify(matchQueueService).addMatch(lost3, found, score3);
            verify(cache).put(finderId, List.of(lostId1, lostId3));
        }

        @Test
        void processNewFoundRequest_limitsCacheTo50() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            // Create 60 candidate lost requests
            List<Long> candidateIds = new java.util.ArrayList<>();
            for (long i = 1; i <= 60; i++) {
                candidateIds.add(i);
            }
            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(candidateIds);

            List<LostRequest> candidates = candidateIds.stream()
                    .map(id -> createLostRequest(id, id + 100, petType, createPoint(30.50, 50.43)))
                    .toList();
            given(lostRequestRepository.findAllById(candidateIds)).willReturn(candidates);

            // All have positive scores, sorted in descending order
            for (int i = 0; i < candidates.size(); i++) {
                BigDecimal score = BigDecimal.valueOf(0.99 - (i * 0.01));
                given(matchingAlgorithm.calculateScore(candidates.get(i), found)).willReturn(score);
            }

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then - verify cache only gets top 50
            verify(matchQueueService, org.mockito.Mockito.times(60)).addMatch(any(), eq(found), any());

            org.mockito.ArgumentCaptor<List<Long>> cacheCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(cache).put(eq(finderId), cacheCaptor.capture());

            List<Long> cachedIds = cacheCaptor.getValue();
            assertThat(cachedIds).hasSize(50);
            // Should be the first 50 IDs (highest scores)
            assertThat(cachedIds.get(0)).isEqualTo(1L);
            assertThat(cachedIds.get(49)).isEqualTo(50L);
        }

        @Test
        void processNewFoundRequest_sortsResultsByScoreDescending() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            Long lostId1 = 10L;
            Long lostId2 = 20L;
            Long lostId3 = 30L;
            List<Long> candidateIds = List.of(lostId1, lostId2, lostId3);
            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(candidateIds);

            Point location = createPoint(30.50, 50.43);
            LostRequest lost1 = createLostRequest(lostId1, 101L, petType, location);
            LostRequest lost2 = createLostRequest(lostId2, 102L, petType, location);
            LostRequest lost3 = createLostRequest(lostId3, 103L, petType, location);
            given(lostRequestRepository.findAllById(candidateIds))
                    .willReturn(List.of(lost1, lost2, lost3));

            // Scores out of order: 0.5, 0.9, 0.7 - should be sorted to 0.9, 0.7, 0.5
            BigDecimal score1 = BigDecimal.valueOf(0.50);
            BigDecimal score2 = BigDecimal.valueOf(0.90);
            BigDecimal score3 = BigDecimal.valueOf(0.70);
            given(matchingAlgorithm.calculateScore(lost1, found)).willReturn(score1);
            given(matchingAlgorithm.calculateScore(lost2, found)).willReturn(score2);
            given(matchingAlgorithm.calculateScore(lost3, found)).willReturn(score3);

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then - verify cache has IDs in correct order (sorted by score desc)
            org.mockito.ArgumentCaptor<List<Long>> cacheCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
            verify(cache).put(eq(finderId), cacheCaptor.capture());

            List<Long> cachedIds = cacheCaptor.getValue();
            assertThat(cachedIds).containsExactly(lostId2, lostId3, lostId1);
        }

        @Test
        void processNewFoundRequest_whenRequestNotFound_throwsException() {
            // given
            Long foundRequestId = 999L;
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> underTest.processNewFoundRequestInternal(foundRequestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FOUND_REQUEST_NOT_FOUND)
                    .hasMessageContaining("Found request not found: " + foundRequestId);

            verify(foundRequestRepository).findById(foundRequestId);
            verifyNoInteractions(jdbcTemplate);
            verifyNoInteractions(lostRequestRepository);
            verifyNoInteractions(matchingAlgorithm);
            verifyNoInteractions(matchQueueService);
            verifyNoInteractions(cache);
        }

        @Test
        void processNewFoundRequest_usesCorrectJdbcQueryParameters() {
            // given
            Long foundRequestId = 1L;
            Long finderId = 100L;
            Point foundLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            FoundRequest found = createFoundRequest(foundRequestId, finderId, petType, foundLocation);
            given(foundRequestRepository.findById(foundRequestId)).willReturn(Optional.of(found));

            given(jdbcTemplate.queryForList(any(), eq(Long.class), any(), any(), any()))
                    .willReturn(Collections.emptyList());
            given(lostRequestRepository.findAllById(anyList())).willReturn(Collections.emptyList());

            // when
            underTest.processNewFoundRequestInternal(foundRequestId);

            // then
            org.mockito.ArgumentCaptor<String> sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
            org.mockito.ArgumentCaptor<Object> paramCaptor = org.mockito.ArgumentCaptor.forClass(Object.class);

            verify(jdbcTemplate).queryForList(sqlCaptor.capture(), eq(Long.class), paramCaptor.capture(), paramCaptor.capture(), paramCaptor.capture());

            String capturedSql = sqlCaptor.getValue();
            assertThat(capturedSql).contains("pet_type = ?");
            assertThat(capturedSql).contains("status = 'ACTIVE'");
            assertThat(capturedSql).contains("ST_DWithin");
        }
    }

    // processNewLostRequestInternal tests --------------------------------------

    @Nested
    class ProcessNewLostRequestInternal {

        @Test
        void processNewLostRequest_withValidData_findsMatches() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            LostRequest lost = createLostRequest(lostRequestId, 100L, petType, lostLocation);
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.of(lost));

            // Mock recent found requests
            Long foundId1 = 10L;
            Long foundId2 = 20L;
            Point foundLocation1 = createPoint(30.51, 50.44);
            Point foundLocation2 = createPoint(30.53, 50.46);
            FoundRequest found1 = createFoundRequest(foundId1, 200L, petType, foundLocation1);
            FoundRequest found2 = createFoundRequest(foundId2, 201L, petType, foundLocation2);

            given(foundRequestRepository.findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class)))
                    .willReturn(List.of(found1, found2));

            // Mock matching algorithm scores
            BigDecimal score1 = BigDecimal.valueOf(0.88);
            BigDecimal score2 = BigDecimal.valueOf(0.65);
            given(matchingAlgorithm.calculateScore(lost, found1)).willReturn(score1);
            given(matchingAlgorithm.calculateScore(lost, found2)).willReturn(score2);

            // when
            underTest.processNewLostRequestInternal(lostRequestId);

            // then
            verify(lostRequestRepository).findById(lostRequestId);
            verify(foundRequestRepository).findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class));
            verify(matchingAlgorithm).calculateScore(lost, found1);
            verify(matchingAlgorithm).calculateScore(lost, found2);
            verify(matchQueueService).addMatch(lost, found1, score1);
            verify(matchQueueService).addMatch(lost, found2, score2);
        }

        @Test
        void processNewLostRequest_withNoCandidates_skipsProcessing() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            LostRequest lost = createLostRequest(lostRequestId, 100L, petType, lostLocation);
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.of(lost));

            given(foundRequestRepository.findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class)))
                    .willReturn(Collections.emptyList());

            // when
            underTest.processNewLostRequestInternal(lostRequestId);

            // then
            verify(lostRequestRepository).findById(lostRequestId);
            verify(foundRequestRepository).findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class));
            verifyNoInteractions(matchingAlgorithm);
            verifyNoInteractions(matchQueueService);
        }

        @Test
        void processNewLostRequest_filtersZeroScores() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            LostRequest lost = createLostRequest(lostRequestId, 100L, petType, lostLocation);
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.of(lost));

            Long foundId1 = 10L;
            Long foundId2 = 20L;
            Point foundLocation = createPoint(30.51, 50.44);
            FoundRequest found1 = createFoundRequest(foundId1, 200L, petType, foundLocation);
            FoundRequest found2 = createFoundRequest(foundId2, 201L, petType, foundLocation);

            given(foundRequestRepository.findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class)))
                    .willReturn(List.of(found1, found2));

            // Scores: 0.75, 0.00 - second one should be filtered out
            BigDecimal score1 = BigDecimal.valueOf(0.75);
            BigDecimal score2 = BigDecimal.ZERO;
            given(matchingAlgorithm.calculateScore(lost, found1)).willReturn(score1);
            given(matchingAlgorithm.calculateScore(lost, found2)).willReturn(score2);

            // when
            underTest.processNewLostRequestInternal(lostRequestId);

            // then
            verify(matchQueueService).addMatch(lost, found1, score1);
            verify(matchQueueService, never()).addMatch(lost, found2, score2);
        }

        @Test
        void processNewLostRequest_whenRequestNotFound_throwsException() {
            // given
            Long lostRequestId = 999L;
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> underTest.processNewLostRequestInternal(lostRequestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_NOT_FOUND)
                    .hasMessageContaining("Lost request not found: " + lostRequestId);

            verify(lostRequestRepository).findById(lostRequestId);
            verifyNoInteractions(foundRequestRepository);
            verifyNoInteractions(matchingAlgorithm);
            verifyNoInteractions(matchQueueService);
        }

        @Test
        void processNewLostRequest_uses30DayLookback() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            LostRequest lost = createLostRequest(lostRequestId, 100L, petType, lostLocation);
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.of(lost));

            org.mockito.ArgumentCaptor<Timestamp> timestampCaptor = org.mockito.ArgumentCaptor.forClass(Timestamp.class);
            given(foundRequestRepository.findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), timestampCaptor.capture()))
                    .willReturn(Collections.emptyList());

            // when
            underTest.processNewLostRequestInternal(lostRequestId);

            // then
            Timestamp capturedTimestamp = timestampCaptor.getValue();
            Instant now = Instant.now();
            Instant capturedInstant = capturedTimestamp.toInstant();

            // Should be approximately 30 days ago (allowing for test execution time)
            long daysDiff = java.time.Duration.between(capturedInstant, now).toDays();
            assertThat(daysDiff).isEqualTo(30);
        }

        @Test
        void processNewLostRequest_doesNotUseCache() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.52, 50.45);
            PetType petType = PetType.DOG;

            LostRequest lost = createLostRequest(lostRequestId, 100L, petType, lostLocation);
            given(lostRequestRepository.findById(lostRequestId)).willReturn(Optional.of(lost));

            Point foundLocation = createPoint(30.51, 50.44);
            FoundRequest found = createFoundRequest(10L, 200L, petType, foundLocation);

            given(foundRequestRepository.findRecentByPetTypeAndLocation(
                    eq(petType.name()), eq(lostLocation), any(Timestamp.class)))
                    .willReturn(List.of(found));

            given(matchingAlgorithm.calculateScore(lost, found)).willReturn(BigDecimal.valueOf(0.75));

            // when
            underTest.processNewLostRequestInternal(lostRequestId);

            // then
            verify(matchQueueService).addMatch(lost, found, BigDecimal.valueOf(0.75));
            verifyNoInteractions(cache); // Cache should not be used for lost requests
        }
    }
}
