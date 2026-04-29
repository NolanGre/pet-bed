package op.edu.ua.petbed.lost.domain.model;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
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
        Long finderId = 123L;
        String photoUrl = "photo123.jpg";
        PetType petType = PetType.DOG;
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52));
        String description = "White dog with black spots";

        // when
        FoundRequest foundRequest = FoundRequest.create(finderId, photoUrl, petType, location, description);

        // then
        assertThat(foundRequest.getFinderId()).isEqualTo(finderId);
        assertThat(foundRequest.getPhotoUrl()).isEqualTo(photoUrl);
        assertThat(foundRequest.getPetType()).isEqualTo(petType);
        assertThat(foundRequest.getLocation()).isEqualTo(location);
        assertThat(foundRequest.getDescription()).isEqualTo(description);
    }

    // .create() validation -----------------------------------------

    @Test
    void create_withBlankPhotoUrl_throwsPetBedException() {
        // given
        Long finderId = 123L;
        String blankPhotoUrl = "   ";
        PetType petType = PetType.DOG;
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52));
        String description = "White dog with black spots";

        // when & then
        assertThatThrownBy(() -> FoundRequest.create(finderId, blankPhotoUrl, petType, location, description))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_PHOTO_URL_REQUIRED);
    }

    @Test
    void create_withBlankDescription_throwsPetBedException() {
        // given
        Long finderId = 123L;
        String photoUrl = "photo123.jpg";
        PetType petType = PetType.DOG;
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52));
        String blankDescription = "   ";

        // when & then
        assertThatThrownBy(() -> FoundRequest.create(finderId, photoUrl, petType, location, blankDescription))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_DESCRIPTION_REQUIRED);
    }

    // .getIdOrThrow() ----------------------------------------------

    @Test
    void getIdOrThrow_withId_returnsId() {
        // given
        Long finderId = 123L;
        String photoUrl = "photo123.jpg";
        PetType petType = PetType.DOG;
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52));
        String description = "White dog with black spots";
        FoundRequest foundRequest = FoundRequest.create(finderId, photoUrl, petType, location, description);

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
        Long finderId = 123L;
        String photoUrl = "photo123.jpg";
        PetType petType = PetType.DOG;
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(50.45, 30.52));
        String description = "White dog with black spots";
        FoundRequest foundRequest = FoundRequest.create(finderId, photoUrl, petType, location, description);

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
