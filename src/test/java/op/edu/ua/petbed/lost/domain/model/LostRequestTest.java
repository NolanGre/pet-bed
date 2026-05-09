package op.edu.ua.petbed.lost.domain.model;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostRequestTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    // Helper method to create a Point
    private static Point createPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    // Helper method to create a PetDTO for testing
    private static PetDTO createPetDTO(
            Long id,
            String name,
            PetType type,
            String breed,
            String color,
            String colorPattern,
            String specialMarks) {
        return new PetDTO(
                id,
                1L, // ownerId
                name,
                type,
                "photo123", // photoId
                breed,
                color,
                colorPattern,
                3, // age
                PetSex.MALE,
                PetSize.MEDIUM,
                specialMarks,
                PetStatus.DEFAULT
        );
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class Create {

        @Test
        void create_withValidData_createsLostRequest() throws Exception {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);

            // when
            LostRequest result = LostRequest.create(pet, contactInfo, location);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getPetId()).isEqualTo(1L);
            assertThat(result.getContactInfo()).isEqualTo(contactInfo);
            assertThat(result.getLastSeenLocation()).isEqualTo(location);
            assertThat(result.getPetType()).isEqualTo(PetType.CAT);

            // Use reflection to verify id is null (not persisted yet)
            Field idField = LostRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            assertThat(idField.get(result)).isNull();
        }

        @Test
        void create_generatesCorrectSearchText() {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);

            // when
            LostRequest result = LostRequest.create(pet, contactInfo, location);

            // then - check individual fields instead of searchText
            assertThat(result.getBreedText()).isEqualTo("Дворовий");
            assertThat(result.getColorText()).isEqualTo("Сірий");
            assertThat(result.getCoatText()).isEqualTo("Смугастий");
            assertThat(result.getFeaturesText()).isEqualTo("Білий хвіст");
        }

        @Test
        void create_withEmptySpecialMarks_generatesSearchTextWithTrimming() {
            // given
            PetDTO pet = createPetDTO(
                    2L,
                    "Рекс",
                    PetType.DOG,
                    "Вівчарка",
                    "Чорний",
                    "Однотонний",
                    "" // empty special marks
            );
            String contactInfo = "test@example.com";
            Point location = createPoint(49.84, 24.03);

            // when
            LostRequest result = LostRequest.create(pet, contactInfo, location);

            // then - check individual fields
            assertThat(result.getBreedText()).isEqualTo("Вівчарка");
            assertThat(result.getColorText()).isEqualTo("Чорний");
            assertThat(result.getCoatText()).isEqualTo("Однотонний");
            assertThat(result.getFeaturesText()).isNull();
        }

        @Test
        void create_withBlankContactInfo_throwsPetBedException() {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            String blankContactInfo = "   ";
            Point location = createPoint(50.45, 30.52);

            // when & then
            assertThatThrownBy(() -> LostRequest.create(pet, blankContactInfo, location))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        @Test
        void create_withEmptyContactInfo_throwsPetBedException() {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            String emptyContactInfo = "";
            Point location = createPoint(50.45, 30.52);

            // when & then
            assertThatThrownBy(() -> LostRequest.create(pet, emptyContactInfo, location))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class GetIdOrThrow {

        @ParameterizedTest
        @ValueSource(longs = {1L, 100L, Long.MAX_VALUE})
        void getIdOrThrow_withId_returnsId(long id) throws Exception {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            LostRequest lostRequest = LostRequest.create(pet, "+380991234567", createPoint(50.45, 30.52));

            // Set id using reflection (simulating database assignment)
            Field idField = LostRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(lostRequest, id);

            // when
            long result = lostRequest.getIdOrThrow();

            // then
            assertThat(result).isEqualTo(id);
        }

        @Test
        void getIdOrThrow_withoutId_throwsPetBedException() {
            // given
            PetDTO pet = createPetDTO(
                    1L,
                    "Барсик",
                    PetType.CAT,
                    "Дворовий",
                    "Сірий",
                    "Смугастий",
                    "Білий хвіст"
            );
            LostRequest lostRequest = LostRequest.create(pet, "+380991234567", createPoint(50.45, 30.52));

            // when & then
            assertThatThrownBy(lostRequest::getIdOrThrow)
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_NOT_PERSISTED);
        }
    }
}
