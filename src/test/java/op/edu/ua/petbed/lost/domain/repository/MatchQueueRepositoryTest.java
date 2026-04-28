package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.MatchQueueEntry;
import op.edu.ua.petbed.lost.domain.model.ViewingStatus;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchQueueRepositoryTest extends PostgresTestContainer {

    @Autowired
    MatchQueueRepository underTest;

    @Autowired
    LostRequestRepository lostRequestRepository;

    @Autowired
    FoundRequestRepository foundRequestRepository;

    @Autowired
    TestEntityManager em;

    private Long ownerId;
    private Long finderId;
    private Long petId;
    private LostRequest savedLostRequest;
    private FoundRequest savedFoundRequest;
    private MatchQueueEntry savedEntry;

    @BeforeEach
    void setUp() {
        ownerId = 100L;
        finderId = 200L;
        petId = 1L;

        // Create LostRequest
        savedLostRequest = lostRequestRepository.save(createLostRequest(petId, "Барсик", "+380991234567"));
        em.flush();
        em.clear();

        // Create FoundRequest
        savedFoundRequest = foundRequestRepository.save(createFoundRequest(finderId));
        em.flush();
        em.clear();

        // Create MatchQueueEntry
        savedEntry = underTest.save(createMatchQueueEntry(savedLostRequest, savedFoundRequest, new BigDecimal("0.8500")));
        em.flush();
        em.clear();
    }

    private LostRequest createLostRequest(Long petId, String name, String contactInfo) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(30.0, 50.0));
        PetDTO petDTO = new PetDTO(
                petId,
                ownerId,
                name,
                PetType.DOG,
                "photo123",
                "TestBreed",
                "TestColor",
                "solid",
                3,
                PetSex.MALE,
                PetSize.MEDIUM,
                "test marks",
                PetStatus.DEFAULT
        );
        return LostRequest.create(petDTO, contactInfo, location);
    }

    private FoundRequest createFoundRequest(Long finderId) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(30.0, 50.0));
        return FoundRequest.create(finderId, "photo_url", PetType.DOG, location, "Found description");
    }

    private MatchQueueEntry createMatchQueueEntry(LostRequest lost, FoundRequest found, BigDecimal score) {
        return MatchQueueEntry.create(lost, found, score);
    }

    // .findByLostRequestIdAndViewingStatusIn() -------------------------------------------------

    @Nested
    class FindByLostRequestIdAndViewingStatusIn {

        @Test
        void existing_status_returns_matching_entries() {
            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdAndViewingStatusIn(
                    savedLostRequest.getIdOrThrow(), List.of(ViewingStatus.NEW));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIdOrThrow()).isEqualTo(savedEntry.getIdOrThrow());
        }

        @Test
        void multiple_statuses_returns_all_matching() {
            // given
            Long finder2Id = 201L;
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2Id));
            em.flush();
            em.clear();
            MatchQueueEntry confirmedEntry = underTest.save(createMatchQueueEntry(savedLostRequest, found2, new BigDecimal("0.7500")));
            confirmedEntry.markAsViewed("test_user");
            underTest.save(confirmedEntry);
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdAndViewingStatusIn(
                    savedLostRequest.getIdOrThrow(), List.of(ViewingStatus.NEW, ViewingStatus.VIEWED));

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        void non_matching_status_returns_empty() {
            // when - search for VIEWED when only NEW exists
            List<MatchQueueEntry> result = underTest.findByLostRequestIdAndViewingStatusIn(
                    savedLostRequest.getIdOrThrow(), List.of(ViewingStatus.VIEWED));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void different_lost_request_returns_empty() {
            // given
            Long otherPetId = 999L;
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPetId, "OtherPet", "+380997654321"));
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdAndViewingStatusIn(
                    otherLost.getIdOrThrow(), List.of(ViewingStatus.NEW));

            // then
            assertThat(result).isEmpty();
        }
    }

    // .findByLostRequestIdOrderByScoreDesc() -------------------------------------------------

    @Nested
    class FindByLostRequestIdOrderByScoreDesc {

        @Test
        void returns_entries_ordered_by_score_desc() {
            // given
            Long finder2Id = 201L;
            Long finder3Id = 202L;

            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2Id));
            em.flush();
            em.clear();
            FoundRequest found3 = foundRequestRepository.save(createFoundRequest(finder3Id));
            em.flush();
            em.clear();

            // Create with different scores
            MatchQueueEntry entryHigh = underTest.save(createMatchQueueEntry(savedLostRequest, found2, new BigDecimal("0.9200")));
            em.flush();
            em.clear();
            MatchQueueEntry entryLow = underTest.save(createMatchQueueEntry(savedLostRequest, found3, new BigDecimal("0.6500")));
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdOrderByScoreDesc(
                    savedLostRequest.getIdOrThrow());

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).getScore()).isEqualTo(new BigDecimal("0.9200"));
            assertThat(result.get(1).getScore()).isEqualTo(new BigDecimal("0.8500"));
            assertThat(result.get(2).getScore()).isEqualTo(new BigDecimal("0.6500"));
        }

        @Test
        void empty_when_no_entries() {
            // given
            Long otherPetId = 999L;
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPetId, "OtherPet", "+380997654321"));
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdOrderByScoreDesc(
                    otherLost.getIdOrThrow());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void different_lost_request_returns_empty() {
            // given
            Long otherPetId = 888L;
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPetId, "OtherPet2", "+380997654321"));
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdOrderByScoreDesc(
                    otherLost.getIdOrThrow());

            // then
            assertThat(result).isEmpty();
        }
    }

    // .existsByLostRequestIdAndFoundRequestId() -------------------------------------------------

    @Nested
    class ExistsByLostRequestIdAndFoundRequestId {

        @Test
        void existing_pair_returns_true() {
            // when
            boolean result = underTest.existsByLostRequestIdAndFoundRequestId(
                    savedLostRequest.getIdOrThrow(), savedFoundRequest.getIdOrThrow());

            // then
            assertThat(result).isTrue();
        }

        @Test
        void non_existing_pair_returns_false() {
            // when
            boolean result = underTest.existsByLostRequestIdAndFoundRequestId(9999L, 9999L);

            // then
            assertThat(result).isFalse();
        }

        @Test
        void different_lost_request_returns_false() {
            // given
            Long otherPetId = 777L;
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPetId, "OtherPet3", "+380997654321"));
            em.flush();
            em.clear();

            // when - use existing foundRequestId but different lostRequestId
            boolean result = underTest.existsByLostRequestIdAndFoundRequestId(
                    otherLost.getIdOrThrow(), savedFoundRequest.getIdOrThrow());

            // then
            assertThat(result).isFalse();
        }

        @Test
        void different_found_request_returns_false() {
            // given
            Long finder2Id = 201L;
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2Id));
            em.flush();
            em.clear();

            // when - use existing lostRequestId but different foundRequestId
            boolean result = underTest.existsByLostRequestIdAndFoundRequestId(
                    savedLostRequest.getIdOrThrow(), found2.getIdOrThrow());

            // then
            assertThat(result).isFalse();
        }
    }

    // .deleteByLostRequestId() -------------------------------------------------

    @Nested
    class DeleteByLostRequestId {

        @Test
        void deletes_all_entries_for_lost_request() {
            // given
            Long finder2Id = 201L;
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2Id));
            em.flush();
            em.clear();
            underTest.save(createMatchQueueEntry(savedLostRequest, found2, new BigDecimal("0.7500")));
            em.flush();
            em.clear();

            // verify we have 2 entries
            assertThat(underTest.findByLostRequestIdOrderByScoreDesc(savedLostRequest.getIdOrThrow())).hasSize(2);

            // when
            underTest.deleteByLostRequestId(savedLostRequest.getIdOrThrow());
            em.flush();
            em.clear();

            // then
            assertThat(underTest.findByLostRequestIdOrderByScoreDesc(savedLostRequest.getIdOrThrow())).isEmpty();
        }

        @Test
        void keeps_entries_for_other_lost_requests() {
            // given
            Long otherPetId = 666L;
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPetId, "OtherPet4", "+380997654321"));
            em.flush();
            em.clear();

            Long finder2Id = 201L;
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2Id));
            em.flush();
            em.clear();

            MatchQueueEntry otherEntry = underTest.save(createMatchQueueEntry(otherLost, found2, new BigDecimal("0.7500")));
            em.flush();
            em.clear();

            // when
            underTest.deleteByLostRequestId(savedLostRequest.getIdOrThrow());
            em.flush();
            em.clear();

            // then
            assertThat(underTest.findById(otherEntry.getIdOrThrow())).isPresent();
            assertThat(underTest.findByLostRequestIdOrderByScoreDesc(savedLostRequest.getIdOrThrow())).isEmpty();
        }

        @Test
        void non_existing_lost_request_does_nothing() {
            // when - delete non-existing
            underTest.deleteByLostRequestId(9999L);
            em.flush();
            em.clear();

            // then - existing entry should still be there
            assertThat(underTest.findById(savedEntry.getIdOrThrow())).isPresent();
        }
    }
}
