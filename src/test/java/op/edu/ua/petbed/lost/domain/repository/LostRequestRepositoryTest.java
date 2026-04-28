package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequestStatus;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostRequestRepositoryTest extends PostgresTestContainer {

    @Autowired
    LostRequestRepository underTest;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PetRepository petRepository;

    @Autowired
    TestEntityManager em;

    private Long ownerId;
    private Pet savedPet;
    private LostRequest savedLostRequest;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(createUser(100L, "test_owner"));
        em.flush();
        em.clear();

        ownerId = owner.getIdOrThrow();
        savedPet = petRepository.save(createPet(ownerId, "Барсик", PetType.CAT));
        em.flush();
        em.clear();

        savedLostRequest = underTest.save(createLostRequest(savedPet, "+380991234567"));
        em.flush();
        em.clear();
    }

    private User createUser(Long telegramId, String username) {
        return User.create(telegramId, username);
    }

    private Pet createPet(Long ownerId, String name, PetType type) {
        return Pet.create(CreatePetDTO.builder()
                .ownerId(ownerId)
                .name(name)
                .type(type)
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

    // .findByPetId() -------------------------------------------------

    @Nested
    class FindByPetId {

        @Test
        void existing_pet_returns_lost_request() {
            // when
            Optional<LostRequest> result = underTest.findByPetId(savedPet.getIdOrThrow());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getContactInfo()).isEqualTo("+380991234567");
        }

        @Test
        void non_existing_pet_returns_empty() {
            // when
            Optional<LostRequest> result = underTest.findByPetId(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // .findByPetOwnerIdAndStatus() -------------------------------------------------

    @Nested
    class FindByPetOwnerIdAndStatus {

        @Test
        void owner_with_active_requests_returns_list() {
            // when
            List<LostRequest> result = underTest.findByPetOwnerIdAndStatus(ownerId, LostRequestStatus.ACTIVE);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContactInfo()).isEqualTo("+380991234567");
        }

        @Test
        void owner_with_no_requests_returns_empty_list() {
            // when
            List<LostRequest> result = underTest.findByPetOwnerIdAndStatus(999L, LostRequestStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void different_status_returns_empty() {
            // given - cancel the saved lost request
            LostRequest request = underTest.findById(savedLostRequest.getIdOrThrow()).orElseThrow();
            request.cancel();
            underTest.save(request);
            em.flush();
            em.clear();

            // when
            List<LostRequest> result = underTest.findByPetOwnerIdAndStatus(ownerId, LostRequestStatus.ACTIVE);

            // then
            assertThat(result).isEmpty();
        }
    }

    // .findByStatusAndPetType() -------------------------------------------------

    @Nested
    class FindByStatusAndPetType {

        @Test
        void active_cats_returns_matching_requests() {
            // when
            List<LostRequest> result = underTest.findByStatusAndPetType(LostRequestStatus.ACTIVE, PetType.CAT);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPetType()).isEqualTo(PetType.CAT);
        }

        @Test
        void cancelled_status_returns_empty() {
            // given - cancel the saved lost request
            LostRequest request = underTest.findById(savedLostRequest.getIdOrThrow()).orElseThrow();
            request.cancel();
            underTest.save(request);
            em.flush();
            em.clear();

            // when
            List<LostRequest> result = underTest.findByStatusAndPetType(LostRequestStatus.ACTIVE, PetType.CAT);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void different_pet_type_returns_empty() {
            // when
            List<LostRequest> result = underTest.findByStatusAndPetType(LostRequestStatus.ACTIVE, PetType.DOG);

            // then
            assertThat(result).isEmpty();
        }
    }

    // .existsByPetId() -------------------------------------------------

    @Nested
    class ExistsByPetId {

        @Test
        void existing_pet_returns_true() {
            // when
            boolean result = underTest.existsByPetId(savedPet.getIdOrThrow());

            // then
            assertThat(result).isTrue();
        }

        @Test
        void non_existing_pet_returns_false() {
            // when
            boolean result = underTest.existsByPetId(999L);

            // then
            assertThat(result).isFalse();
        }
    }

    // .deleteByPetId() -------------------------------------------------

    @Nested
    class DeleteByPetId {

        @Test
        void deletes_existing_lost_request() {
            // given
            Long petId = savedPet.getIdOrThrow();
            assertThat(underTest.existsByPetId(petId)).isTrue();

            // when
            underTest.deleteByPetId(petId);
            em.flush();
            em.clear();

            // then
            assertThat(underTest.existsByPetId(petId)).isFalse();
        }

        @Test
        void non_existing_pet_does_nothing() {
            // when
            underTest.deleteByPetId(999L);
            em.flush();
            em.clear();

            // then - no exception thrown, and existing data remains
            assertThat(underTest.existsByPetId(savedPet.getIdOrThrow())).isTrue();
        }
    }
}
