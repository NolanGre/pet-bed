package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
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

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostRequestRepositoryTest extends PostgresTestContainer {

    @Autowired
    LostRequestRepository underTest;

    @Autowired
    TestEntityManager em;

    private Long petId;

    @BeforeEach
    void setUp() {
        petId = 1L;
        underTest.save(createLostRequest(petId, "Барсик", PetType.CAT, "+380991234567"));
        em.flush();
        em.clear();
    }

    private LostRequest createLostRequest(Long petId, String name, PetType type, String contactInfo) {
        GeometryFactory gf = new GeometryFactory();
        Point location = gf.createPoint(new Coordinate(30.0, 50.0));
        PetDTO petDTO = new PetDTO(
                petId,
                100L,
                name,
                type,
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

    // .findByPetId() -------------------------------------------------

    @Nested
    class FindByPetId {

        @Test
        void existing_pet_returns_lost_request() {
            // when
            LostRequest result = underTest.findByPetId(petId);

            // then
            assertThat(result).isNotNull();
            assertThat(Objects.requireNonNull(result).getContactInfo()).isEqualTo("+380991234567");
        }

        @Test
        void non_existing_pet_returns_null() {
            // when
            LostRequest result = underTest.findByPetId(999L);

            // then
            assertThat(result).isNull();
        }
    }

    // .existsByPetId() -------------------------------------------------

    @Nested
    class ExistsByPetId {

        @Test
        void existing_pet_returns_true() {
            // when
            boolean result = underTest.existsByPetId(petId);

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
            assertThat(underTest.existsByPetId(petId)).isTrue();
        }
    }
}
