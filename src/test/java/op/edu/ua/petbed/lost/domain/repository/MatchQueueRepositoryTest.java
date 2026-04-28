package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.MatchQueueEntry;
import op.edu.ua.petbed.lost.domain.model.ViewingStatus;
import op.edu.ua.petbed.pet.model.Pet;
import op.edu.ua.petbed.pet.repository.PetRepository;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchQueueRepositoryTest extends PostgresTestContainer {

    @Autowired
    MatchQueueRepository underTest;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PetRepository petRepository;

    @Autowired
    LostRequestRepository lostRequestRepository;

    @Autowired
    FoundRequestRepository foundRequestRepository;

    @Autowired
    TestEntityManager em;

    private Long ownerId;
    private Long finderId;
    private Pet savedPet;
    private LostRequest savedLostRequest;
    private FoundRequest savedFoundRequest;
    private MatchQueueEntry savedEntry;

    @BeforeEach
    void setUp() {
        // Create owner User
        User owner = userRepository.save(createUser(100L, "pet_owner"));
        em.flush();
        em.clear();
        ownerId = owner.getIdOrThrow();

        // Create finder User
        User finder = userRepository.save(createUser(200L, "finder_user"));
        em.flush();
        em.clear();
        finderId = finder.getIdOrThrow();

        // Create Pet for owner
        savedPet = petRepository.save(createPet(ownerId, "Барсик"));
        em.flush();
        em.clear();

        // Create LostRequest
        savedLostRequest = lostRequestRepository.save(createLostRequest(savedPet, "+380991234567"));
        em.flush();
        em.clear();

        // Create FoundRequest
        savedFoundRequest = foundRequestRepository.save(createFoundRequest(finder));
        em.flush();
        em.clear();

        // Create MatchQueueEntry
        savedEntry = underTest.save(createMatchQueueEntry(savedLostRequest, savedFoundRequest, new BigDecimal("0.8500")));
        em.flush();
        em.clear();
    }

    private User createUser(Long telegramId, String username) {
        return User.create(telegramId, username);
    }

    private Pet createPet(Long ownerId, String name) {
        return Pet.create(CreatePetDTO.builder()
                .ownerId(ownerId)
                .name(name)
                .type(PetType.DOG)
                .photoId("photo123")
                .breed("TestBreed")
                .color("TestColor")
                .colorPattern("solid")
                .age(3)
                .sex(PetSex.MALE)
                .size(PetSize.MEDIUM)
                .specialMarks("test marks")
                .build());
    }

    private LostRequest createLostRequest(Pet pet, String contactInfo) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(30.0, 50.0));
        return LostRequest.create(pet, contactInfo, location);
    }

    private FoundRequest createFoundRequest(User finder) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(30.0, 50.0));
        return FoundRequest.create(finder, "photo_url", PetType.DOG, location, "Found description");
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
            User finder2 = userRepository.save(createUser(201L, "finder2"));
            em.flush();
            em.clear();
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2));
            em.flush();
            em.clear();
            MatchQueueEntry rejectedEntry = underTest.save(createMatchQueueEntry(savedLostRequest, found2, new BigDecimal("0.7500")));
            rejectedEntry.markAsRejected();
            underTest.save(rejectedEntry);
            em.flush();
            em.clear();

            // when
            List<MatchQueueEntry> result = underTest.findByLostRequestIdAndViewingStatusIn(
                    savedLostRequest.getIdOrThrow(), List.of(ViewingStatus.NEW, ViewingStatus.REJECTED));

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
            User otherOwner = userRepository.save(createUser(300L, "other_owner"));
            em.flush();
            em.clear();
            Pet otherPet = petRepository.save(createPet(otherOwner.getIdOrThrow(), "OtherPet"));
            em.flush();
            em.clear();
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPet, "+380997654321"));
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
            User finder2 = userRepository.save(createUser(201L, "finder2"));
            em.flush();
            em.clear();
            User finder3 = userRepository.save(createUser(202L, "finder3"));
            em.flush();
            em.clear();

            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2));
            em.flush();
            em.clear();
            FoundRequest found3 = foundRequestRepository.save(createFoundRequest(finder3));
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
            User otherOwner = userRepository.save(createUser(300L, "other_owner"));
            em.flush();
            em.clear();
            Pet otherPet = petRepository.save(createPet(otherOwner.getIdOrThrow(), "OtherPet"));
            em.flush();
            em.clear();
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPet, "+380997654321"));
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
            User otherOwner = userRepository.save(createUser(300L, "other_owner"));
            em.flush();
            em.clear();
            Pet otherPet = petRepository.save(createPet(otherOwner.getIdOrThrow(), "OtherPet"));
            em.flush();
            em.clear();
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPet, "+380997654321"));
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
            User otherOwner = userRepository.save(createUser(300L, "other_owner"));
            em.flush();
            em.clear();
            Pet otherPet = petRepository.save(createPet(otherOwner.getIdOrThrow(), "OtherPet"));
            em.flush();
            em.clear();
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPet, "+380997654321"));
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
            User finder2 = userRepository.save(createUser(201L, "finder2"));
            em.flush();
            em.clear();
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2));
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
            User finder2 = userRepository.save(createUser(201L, "finder2"));
            em.flush();
            em.clear();
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2));
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
            User otherOwner = userRepository.save(createUser(300L, "other_owner"));
            em.flush();
            em.clear();
            Pet otherPet = petRepository.save(createPet(otherOwner.getIdOrThrow(), "OtherPet"));
            em.flush();
            em.clear();
            LostRequest otherLost = lostRequestRepository.save(createLostRequest(otherPet, "+380997654321"));
            em.flush();
            em.clear();

            User finder2 = userRepository.save(createUser(201L, "finder2"));
            em.flush();
            em.clear();
            FoundRequest found2 = foundRequestRepository.save(createFoundRequest(finder2));
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
