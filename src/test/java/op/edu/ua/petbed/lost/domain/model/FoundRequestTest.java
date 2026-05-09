package op.edu.ua.petbed.lost.domain.model;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FoundRequestTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    // .create() with valid data ------------------------------------

    @Test
    void create_withValidData_createsFoundRequest() {
        // given
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(123L)
                .photoUrl("photo123.jpg")
                .petType(PetType.DOG)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52)))
                .breed("Labrador")
                .color("Golden")
                .coat("Smooth")
                .sex("male")
                .size("medium")
                .features("Friendly")
                .build();

        // when
        FoundRequest foundRequest = FoundRequest.create(dto);

        // then
        assertThat(foundRequest.getFinderId()).isEqualTo(123L);
        assertThat(foundRequest.getPhotoUrl()).isEqualTo("photo123.jpg");
        assertThat(foundRequest.getPetType()).isEqualTo(PetType.DOG);
        assertThat(foundRequest.getLocation()).isEqualTo(dto.location());
        assertThat(foundRequest.getBreedText()).isEqualTo("Labrador");
        assertThat(foundRequest.getColorText()).isEqualTo("Golden");
    }

    // .create() validation -----------------------------------------

    @Test
    void create_withBlankPhotoUrl_throwsPetBedException() {
        // given
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(123L)
                .photoUrl("   ")
                .petType(PetType.DOG)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52)))
                .build();

        // when & then
        assertThatThrownBy(() -> FoundRequest.create(dto))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_PHOTO_URL_REQUIRED);
    }

    // .getIdOrThrow() ----------------------------------------------

    @Test
    void getIdOrThrow_withId_returnsId() {
        // given
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(123L)
                .photoUrl("photo123.jpg")
                .petType(PetType.DOG)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52)))
                .build();
        FoundRequest foundRequest = FoundRequest.create(dto);

        // Simulate persisted entity by setting id via reflection
        Long expectedId = 456L;
        setIdViaReflection(foundRequest, expectedId);

        // when
        Long actualId = foundRequest.getIdOrThrow();

        // then
        assertThat(actualId).isEqualTo(expectedId);
    }

    @Test
    void getIdOrThrow_withoutId_throwsPetBedException() {
        // given
        CreateFoundRequestDTO dto = CreateFoundRequestDTO.builder()
                .finderId(123L)
                .photoUrl("photo123.jpg")
                .petType(PetType.DOG)
                .location(GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52)))
                .build();
        FoundRequest foundRequest = FoundRequest.create(dto);

        // when & then
        assertThatThrownBy(foundRequest::getIdOrThrow)
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_NOT_PERSISTED);
    }

    // Helper method to set id via reflection for testing
    private void setIdViaReflection(FoundRequest foundRequest, Long id) {
        try {
            java.lang.reflect.Field idField = FoundRequest.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(foundRequest, id);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to set id via reflection", e);
        }
    }
}
