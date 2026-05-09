package op.edu.ua.petbed.lost.domain.model;

import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class MatchQueueEntryTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

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
        return found;
    }

    @Nested
    @DisplayName(".create()")
    class CreateTests {

        @Test
        void create_withValidData_createsEntryWithNewStatus() {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            BigDecimal score = BigDecimal.valueOf(0.85);

            // when
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, score);

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.NEW);
            assertThat(entry.getLostRequest()).isEqualTo(lost);
            assertThat(entry.getFoundRequest()).isEqualTo(found);
        }

        @Test
        void create_storesCorrectScore() {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            BigDecimal score = BigDecimal.valueOf(0.9234).setScale(4);

            // when
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, score);

            // then
            assertThat(entry.getScore()).isEqualByComparingTo(score);
            assertThat(entry.getScore().scale()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName(".markAsViewed()")
    class MarkAsViewedTests {

        @Test
        void markAsViewed_changesStatusToViewed() {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, BigDecimal.valueOf(0.85));

            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.NEW);

            // when
            entry.markAsViewed();

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.VIEWED);
        }
    }

    @Nested
    @DisplayName(".markAsConfirmed()")
    class MarkAsConfirmedTests {

        @Test
        void markAsConfirmed_changesStatusToConfirmed() {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, BigDecimal.valueOf(0.85));

            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.NEW);

            // when
            entry.markAsConfirmed();

            // then
            assertThat(entry.getViewingStatus()).isEqualTo(ViewingStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName(".getIdOrThrow()")
    class GetIdOrThrowTests {

        @ParameterizedTest
        @ValueSource(longs = {1L, 100L, Long.MAX_VALUE})
        void getIdOrThrow_withId_returnsId(long id) {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, BigDecimal.valueOf(0.85));
            ReflectionTestUtils.setField(entry, "id", id);

            // when
            long result = entry.getIdOrThrow();

            // then
            assertThat(result).isEqualTo(id);
        }

        @Test
        void getIdOrThrow_withoutId_throwsPetBedException() {
            // given
            Point lostLocation = createPoint(30.5, 50.5);
            Point foundLocation = createPoint(30.6, 50.6);
            LostRequest lost = createLostRequest(1L, 100L, lostLocation);
            FoundRequest found = createFoundRequest(2L, 200L, foundLocation);
            MatchQueueEntry entry = MatchQueueEntry.create(lost, found, BigDecimal.valueOf(0.85));

            // when & then
            assertThatThrownBy(entry::getIdOrThrow)
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED);
        }
    }
}
