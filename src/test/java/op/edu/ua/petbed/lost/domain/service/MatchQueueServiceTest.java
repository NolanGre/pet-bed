package op.edu.ua.petbed.lost.domain.service;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.MatchRecommendationDTO;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.MatchQueueEntry;
import op.edu.ua.petbed.lost.domain.model.ViewingStatus;
import op.edu.ua.petbed.lost.domain.repository.MatchQueueRepository;
import org.junit.jupiter.api.DisplayName;
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
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchQueueServiceTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    MatchQueueRepository matchQueueRepository;

    @Mock
    JdbcTemplate jdbcTemplate;

    @InjectMocks
    MatchQueueService underTest;

    private Point createPoint(double longitude, double latitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private LostRequest createLostRequest(Long id, Long petId, Point location) {
        PetDTO pet = new PetDTO(petId, 1L, "Buddy", PetType.DOG, "photo", "Labrador", "Brown", "Solid", 5, PetSex.MALE, PetSize.MEDIUM, "None", PetStatus.DEFAULT);
        LostRequest lost = LostRequest.create(pet, "test@example.com", location);
        ReflectionTestUtils.setField(lost, "id", id);
        return lost;
    }

    private FoundRequest createFoundRequest(Long id, Long finderId, Point location) {
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(finderId)
                .photoUrl("photo123")
                .petType(PetType.DOG)
                .location(location)
                .breed("Brown dog")
                .build();
        FoundRequest found = FoundRequest.create(dto);
        ReflectionTestUtils.setField(found, "id", id);
        ReflectionTestUtils.setField(found, "createdAt", Instant.now());
        return found;
    }

    private MatchQueueEntry createMatchQueueEntry(Long id, LostRequest lost, FoundRequest found, BigDecimal score, ViewingStatus status) {
        MatchQueueEntry entry = MatchQueueEntry.create(lost, found, score);
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "viewingStatus", status);
        ReflectionTestUtils.setField(entry, "lostRequest", lost);
        ReflectionTestUtils.setField(entry, "foundRequest", found);
        return entry;
    }

    @Nested
    @DisplayName("addMatch()")
    class AddMatchTests {

        @Test
        void addMatch_new_match_creates_and_saves_entry() {
            // given
            Long lostId = 1L;
            Long foundId = 2L;
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            BigDecimal score = BigDecimal.valueOf(0.85);

            LostRequest lost = createLostRequest(lostId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(foundId, 200L, foundLocation);

            given(matchQueueRepository.existsByLostRequestIdAndFoundRequestId(lostId, foundId)).willReturn(false);

            MatchQueueEntry savedEntry = createMatchQueueEntry(10L, lost, found, score, ViewingStatus.NEW);
            given(matchQueueRepository.save(any(MatchQueueEntry.class))).willReturn(savedEntry);

            // when
            underTest.addMatch(lost, found, score);

            // then
            verify(matchQueueRepository).existsByLostRequestIdAndFoundRequestId(lostId, foundId);
            verify(matchQueueRepository).save(any(MatchQueueEntry.class));
        }

        @Test
        void addMatch_existing_match_skips_creation() {
            // given
            Long lostId = 1L;
            Long foundId = 2L;
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            BigDecimal score = BigDecimal.valueOf(0.85);

            LostRequest lost = createLostRequest(lostId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(foundId, 200L, foundLocation);

            given(matchQueueRepository.existsByLostRequestIdAndFoundRequestId(lostId, foundId)).willReturn(true);

            // when
            underTest.addMatch(lost, found, score);

            // then
            verify(matchQueueRepository).existsByLostRequestIdAndFoundRequestId(lostId, foundId);
            verify(matchQueueRepository, never()).save(any(MatchQueueEntry.class));
        }

        @Test
        void addMatch_persists_correct_score() {
            // given
            Long lostId = 1L;
            Long foundId = 2L;
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            BigDecimal score = BigDecimal.valueOf(0.9234).setScale(4);

            LostRequest lost = createLostRequest(lostId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(foundId, 200L, foundLocation);

            given(matchQueueRepository.existsByLostRequestIdAndFoundRequestId(lostId, foundId)).willReturn(false);

            MatchQueueEntry savedEntry = createMatchQueueEntry(10L, lost, found, score, ViewingStatus.NEW);
            given(matchQueueRepository.save(any(MatchQueueEntry.class))).willReturn(savedEntry);

            // when
            underTest.addMatch(lost, found, score);

            // then
            verify(matchQueueRepository).save(any(MatchQueueEntry.class));
        }
    }

    @Nested
    @DisplayName("getRecommendationsForOwner()")
    class GetRecommendationsForOwnerTests {

        @Test
        void getRecommendationsForOwner_returns_new_and_viewed_sorted_by_score_desc() {
            // given
            Long lostRequestId = 1L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(lostRequestId, 100L, location);
            FoundRequest found1 = createFoundRequest(2L, 200L, createPoint(30.51, 50.51));
            FoundRequest found2 = createFoundRequest(3L, 201L, createPoint(30.52, 50.52));
            FoundRequest found3 = createFoundRequest(4L, 202L, createPoint(30.53, 50.53));

            MatchQueueEntry entry1 = createMatchQueueEntry(10L, lost, found1, BigDecimal.valueOf(0.75), ViewingStatus.NEW);
            MatchQueueEntry entry2 = createMatchQueueEntry(11L, lost, found2, BigDecimal.valueOf(0.90), ViewingStatus.VIEWED);
            MatchQueueEntry entry3 = createMatchQueueEntry(12L, lost, found3, BigDecimal.valueOf(0.85), ViewingStatus.NEW);

            List<ViewingStatus> expectedStatuses = List.of(ViewingStatus.NEW, ViewingStatus.VIEWED);
            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(lostRequestId, expectedStatuses))
                    .willReturn(List.of(entry1, entry2, entry3));

            given(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(), any()))
                    .willReturn(1.5);

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).hasSize(3);
            // Should be sorted by score descending: 0.90, 0.85, 0.75
            var result0 = Objects.requireNonNull(result.get(0));
            var result1 = Objects.requireNonNull(result.get(1));
            var result2 = Objects.requireNonNull(result.get(2));

            assertThat(result0.score()).isEqualByComparingTo(BigDecimal.valueOf(0.90));
            assertThat(result1.score()).isEqualByComparingTo(BigDecimal.valueOf(0.85));
            assertThat(result2.score()).isEqualByComparingTo(BigDecimal.valueOf(0.75));
        }

        @Test
        void getRecommendationsForOwner_returns_empty_list_when_no_matches() {
            // given
            Long lostRequestId = 1L;
            List<ViewingStatus> expectedStatuses = List.of(ViewingStatus.NEW, ViewingStatus.VIEWED);

            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(lostRequestId, expectedStatuses))
                    .willReturn(List.of());

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void getRecommendationsForOwner_excludes_confirmed_status() {
            // given
            Long lostRequestId = 1L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(lostRequestId, 100L, location);
            FoundRequest found = createFoundRequest(2L, 200L, createPoint(30.51, 50.51));

            MatchQueueEntry entry = createMatchQueueEntry(10L, lost, found, BigDecimal.valueOf(0.75), ViewingStatus.CONFIRMED);

            List<ViewingStatus> expectedStatuses = List.of(ViewingStatus.NEW, ViewingStatus.VIEWED);
            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(lostRequestId, expectedStatuses))
                    .willReturn(List.of());

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).isEmpty();
            verify(matchQueueRepository).findByLostRequestIdAndViewingStatusIn(lostRequestId, expectedStatuses);
        }

        @Test
        void getRecommendationsForOwner_calculates_distance_using_jdbc_template() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.51, 50.51);
            double expectedDistance = 2.5;

            LostRequest lost = createLostRequest(lostRequestId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = createMatchQueueEntry(10L, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.NEW);

            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(any(), any()))
                    .willReturn(List.of(entry));
            given(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(), any()))
                    .willReturn(expectedDistance);

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).hasSize(1);
            var dto = Objects.requireNonNull(result.get(0));
            assertThat(dto.distanceKm()).isEqualTo(expectedDistance);
        }

        @Test
        void getRecommendationsForOwner_handles_null_distance_from_jdbc() {
            // given
            Long lostRequestId = 1L;
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.51, 50.51);

            LostRequest lost = createLostRequest(lostRequestId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = createMatchQueueEntry(10L, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.NEW);

            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(any(), any()))
                    .willReturn(List.of(entry));
            given(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(), any()))
                    .willReturn(null);

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).hasSize(1);
            var dto = Objects.requireNonNull(result.get(0));
            assertThat(dto.distanceKm()).isEqualTo(0.0);
        }

        @Test
        void getRecommendationsForOwner_maps_all_fields_correctly() {
            // given
            Long lostRequestId = 1L;
            Long foundRequestId = 2L;
            Long matchQueueId = 10L;
            BigDecimal score = BigDecimal.valueOf(0.85);
            String photoUrl = "test-photo-url";
            String description = "Brown dog found in park";
            Instant createdAt = Instant.parse("2025-01-15T10:30:00Z");
            double distance = 5.2;

            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.51, 50.51);

            LostRequest lost = createLostRequest(lostRequestId, 100L, lostLocation);
            FoundRequest found = createFoundRequest(foundRequestId, 200L, foundLocation);
            ReflectionTestUtils.setField(found, "photoUrl", photoUrl);
            ReflectionTestUtils.setField(found, "description", description);
            ReflectionTestUtils.setField(found, "createdAt", createdAt);

            MatchQueueEntry entry = createMatchQueueEntry(matchQueueId, lost, found, score, ViewingStatus.NEW);

            given(matchQueueRepository.findByLostRequestIdAndViewingStatusIn(any(), any()))
                    .willReturn(List.of(entry));
            given(jdbcTemplate.queryForObject(any(String.class), eq(Double.class), any(), any()))
                    .willReturn(distance);

            // when
            List<MatchRecommendationDTO> result = underTest.getRecommendationsForOwner(lostRequestId);

            // then
            assertThat(result).hasSize(1);
            var dto = Objects.requireNonNull(result.get(0));

            assertThat(dto.matchQueueId()).isEqualTo(matchQueueId);
            assertThat(dto.lostRequestId()).isEqualTo(lostRequestId);
            assertThat(dto.foundRequestId()).isEqualTo(foundRequestId);
            assertThat(dto.score()).isEqualByComparingTo(score);
            assertThat(dto.photoUrl()).isEqualTo(photoUrl);
            assertThat(dto.breedText()).isEqualTo("Brown dog");
            assertThat(dto.location()).isEqualTo(foundLocation);
            assertThat(dto.foundRequestCreatedAt()).isEqualTo(createdAt);
            assertThat(dto.distanceKm()).isEqualTo(distance);
        }
    }

    @Nested
    @DisplayName("markAsViewed()")
    class MarkAsViewedTests {

        @Test
        void markAsViewed_existing_entry_updates_status_to_viewed() {
            // given
            Long matchQueueId = 10L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(1L, 100L, location);
            FoundRequest found = createFoundRequest(2L, 200L, location);
            MatchQueueEntry entry = createMatchQueueEntry(matchQueueId, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.NEW);

            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.of(entry));
            given(matchQueueRepository.save(entry)).willReturn(entry);

            // when
            underTest.markAsViewed(matchQueueId);

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.VIEWED);
            verify(matchQueueRepository).findById(matchQueueId);
            verify(matchQueueRepository).save(entry);
        }

        @Test
        void markAsViewed_non_existing_entry_throws_PetBedException() {
            // given
            Long matchQueueId = 99L;
            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> underTest.markAsViewed(matchQueueId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED)
                    .hasMessageContaining("Match queue entry not found with id: " + matchQueueId);
        }

        @Test
        void markAsViewed_does_not_change_if_already_viewed() {
            // given
            Long matchQueueId = 10L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(1L, 100L, location);
            FoundRequest found = createFoundRequest(2L, 200L, location);
            MatchQueueEntry entry = createMatchQueueEntry(matchQueueId, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.VIEWED);

            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.of(entry));
            given(matchQueueRepository.save(entry)).willReturn(entry);

            // when
            underTest.markAsViewed(matchQueueId);

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.VIEWED);
            verify(matchQueueRepository).save(entry);
        }
    }

    @Nested
    @DisplayName("markAsConfirmed()")
    class MarkAsConfirmedTests {

        @Test
        void markAsConfirmed_existing_entry_updates_status_to_confirmed() {
            // given
            Long matchQueueId = 10L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(1L, 100L, location);
            FoundRequest found = createFoundRequest(2L, 200L, location);
            MatchQueueEntry entry = createMatchQueueEntry(matchQueueId, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.VIEWED);

            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.of(entry));
            given(matchQueueRepository.save(entry)).willReturn(entry);

            // when
            underTest.markAsConfirmed(matchQueueId);

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.CONFIRMED);
            verify(matchQueueRepository).findById(matchQueueId);
            verify(matchQueueRepository).save(entry);
        }

        @Test
        void markAsConfirmed_non_existing_entry_throws_PetBedException() {
            // given
            Long matchQueueId = 99L;
            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> underTest.markAsConfirmed(matchQueueId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED)
                    .hasMessageContaining("Match queue entry not found with id: " + matchQueueId);
        }

        @Test
        void markAsConfirmed_works_from_new_status() {
            // given
            Long matchQueueId = 10L;
            Point location = createPoint(30.5, 50.5);

            LostRequest lost = createLostRequest(1L, 100L, location);
            FoundRequest found = createFoundRequest(2L, 200L, location);
            MatchQueueEntry entry = createMatchQueueEntry(matchQueueId, lost, found, BigDecimal.valueOf(0.85), ViewingStatus.NEW);

            given(matchQueueRepository.findById(matchQueueId)).willReturn(Optional.of(entry));
            given(matchQueueRepository.save(entry)).willReturn(entry);

            // when
            underTest.markAsConfirmed(matchQueueId);

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.CONFIRMED);
            verify(matchQueueRepository).save(entry);
        }
    }
}
