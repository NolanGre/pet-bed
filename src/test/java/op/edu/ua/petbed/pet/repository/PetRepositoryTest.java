package op.edu.ua.petbed.pet.repository;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.pet.model.Pet;
import op.edu.ua.petbed.testcontainers.PostgresTestContainer;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PetRepositoryTest extends PostgresTestContainer {

    @Autowired
    PetRepository underTest;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager em;

    private Long ownerUserId;

    @BeforeEach
    void setUp() {
        User owner = userRepository.save(User.create(100L, "pet_owner"));
        em.flush();
        em.clear();

        ownerUserId = owner.getIdOrThrow();
        savedPet = underTest.save(createPet("Барсик", ownerUserId));
        em.flush();
        em.clear();
    }

    private Pet savedPet;

    private static Pet createPet(String name, Long ownerId) {
        return Pet.create(CreatePetDTO.builder()
                .ownerId(ownerId)
                .name(name)
                .type(PetType.CAT)
                .photoId("photo123")
                .breed("Persian")
                .color("white")
                .colorPattern("solid")
                .age(3)
                .sex(PetSex.MALE)
                .size(PetSize.SMALL)
                .specialMarks("friendly")
                .build());
    }

    // .findById() -------------------------------------------------

    @Nested
    class FindById {

        @Test
        void existing_pet_returns_pet() {
            // when
            Optional<Pet> result = underTest.findById(savedPet.getIdOrThrow());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Барсик");
        }

        @Test
        void non_existing_pet_returns_empty() {
            // when
            Optional<Pet> result = underTest.findById(999L);

            // then
            assertThat(result).isEmpty();
        }
    }

    // .findByOwnerId() -------------------------------------------------

    @Nested
    class FindByOwnerId {

        @Test
        void owner_with_pets_returns_page() {
            // given
            underTest.save(createPet("Бебрик", ownerUserId));
            underTest.save(createPet("Мурчик", ownerUserId));
            em.flush();
            em.clear();

            // when
            Page<Pet> result = underTest.findByOwnerIdOrderByUpdatedAtDesc(ownerUserId, PageRequest.of(0, 10));

            // then
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @Test
        void owner_without_pets_returns_empty_page() {
            // when
            Page<Pet> result = underTest.findByOwnerIdOrderByUpdatedAtDesc(999L, PageRequest.of(0, 10));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        void pagination_returns_correct_page() {
            // given
            for (int i = 0; i < 5; i++) {
                underTest.save(createPet("Pet" + i, ownerUserId));
            }
            em.flush();
            em.clear();

            // when
            Page<Pet> page1 = underTest.findByOwnerIdOrderByUpdatedAtDesc(ownerUserId, PageRequest.of(0, 2));
            Page<Pet> page2 = underTest.findByOwnerIdOrderByUpdatedAtDesc(ownerUserId, PageRequest.of(1, 2));

            // then
            assertThat(page1.getTotalElements()).isEqualTo(6);
            assertThat(page1.getContent()).hasSize(2);
            assertThat(page2.getContent()).hasSize(2);
        }

        @Test
        void sorted_by_updated_at_desc() {
            // given
            Pet first = underTest.save(createPet("First", ownerUserId));
            em.flush();
            Pet second = underTest.save(createPet("Second", ownerUserId));
            em.flush();
            em.clear();

            // when
            Page<Pet> result = underTest.findByOwnerIdOrderByUpdatedAtDesc(ownerUserId, PageRequest.of(0, 10));

            // then
            assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Second");
            assertThat(result.getContent().get(1).getName()).isEqualTo("First");
        }
    }

    // .save() -------------------------------------------------

    @Nested
    class Save {

        @Test
        void valid_pet_persists_with_generated_id() {
            // given
            Pet pet = createPet("Новий", ownerUserId);

            // when
            Pet saved = underTest.save(pet);
            em.flush();
            em.clear();

            // then
            assertThat(saved.getIdOrThrow()).isNotNull();
            assertThat(underTest.findById(saved.getIdOrThrow())).isPresent();
        }

        @Test
        void all_fields_persisted_correctly() {
            // given
            Pet pet = createPet("Повний", ownerUserId);

            // when
            Pet saved = underTest.save(pet);
            em.flush();
            em.clear();

            // then
            Pet fetched = underTest.findById(saved.getIdOrThrow()).orElseThrow();
            assertThat(fetched.getName()).isEqualTo("Повний");
            assertThat(fetched.getOwnerId()).isEqualTo(ownerUserId);
            assertThat(fetched.getType()).isEqualTo(PetType.CAT);
            assertThat(fetched.getBreed()).isEqualTo("Persian");
            assertThat(fetched.getColor()).isEqualTo("white");
            assertThat(fetched.getAge()).isEqualTo(3);
            assertThat(fetched.getSex()).isEqualTo(PetSex.MALE);
            assertThat(fetched.getSize()).isEqualTo(PetSize.SMALL);
        }
    }

    // .equals() / .hashCode() -------------------------------------------------

    @Nested
    class Equals {

        @Test
        void same_id_returns_true() {
            // when
            Pet fetched = underTest.findById(savedPet.getIdOrThrow()).orElseThrow();

            // then
            assertThat(fetched).isEqualTo(savedPet);
        }

        @Test
        void different_id_returns_false() {
            // given
            Pet another = underTest.save(createPet("Інший", ownerUserId));
            em.flush();
            em.clear();

            // then
            assertThat(another).isNotEqualTo(savedPet);
        }

        @Test
        void hashCode_same_class_returns_same_value() {
            // given
            Pet another = underTest.save(createPet("Ще один", ownerUserId));
            em.flush();
            em.clear();

            // then
            assertThat(savedPet.hashCode()).isEqualTo(another.hashCode());
        }
    }
}