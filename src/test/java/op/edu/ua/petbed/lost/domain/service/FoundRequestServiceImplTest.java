package op.edu.ua.petbed.lost.domain.service;

import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.application.event.FoundRequestCreatedEvent;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import op.edu.ua.petbed.user.UserService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
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
class FoundRequestServiceImplTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    FoundRequestRepository foundRequestRepository;

    @Mock
    UserService userService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    FoundRequestServiceImpl underTest;

    // Helper methods -----------------------------------------------------------

    private static Point createPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static UserDTO existingUser(Long id) {
        return new UserDTO(id, 123456789L, "testuser", UserType.REGULAR, null);
    }

    private static FoundRequest existingFoundRequest(Long id, Long finderId) {
        FoundRequest request = FoundRequest.create(
                finderId,
                "photo123",
                PetType.DOG,
                createPoint(50.45, 30.52),
                "Brown dog found near park"
        );
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "createdAt", Instant.parse("2024-01-15T10:30:00Z"));
        return request;
    }

    // .create() -----------------------------------------------------------------

    @Nested
    class Create {

        @Test
        void create_withValidData_createsFoundRequestAndReturnsDto() {
            // given
            Long finderId = 1L;
            String photoUrl = "photo123";
            PetType petType = PetType.DOG;
            Point location = createPoint(50.45, 30.52);
            String description = "Brown dog found near park";

            UserDTO finder = existingUser(finderId);
            given(userService.findById(finderId)).willReturn(finder);

            FoundRequest savedRequest = existingFoundRequest(1L, finderId);
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when
            FoundRequestDTO result = underTest.create(finderId, photoUrl, petType, location, description);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.finderId()).isEqualTo(finderId);
            assertThat(result.photoUrl()).isEqualTo(photoUrl);
            assertThat(result.petType()).isEqualTo(petType);
            assertThat(result.location()).isEqualTo(location);
            assertThat(result.description()).isEqualTo(description);
            assertThat(result.createdAt()).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));

            verify(userService).findById(finderId);
            verify(foundRequestRepository).save(any(FoundRequest.class));
        }

        @Test
        void create_callsUserServiceFindById_withCorrectFinderId() {
            // given
            Long finderId = 1L;
            given(userService.findById(finderId)).willReturn(existingUser(finderId));
            given(foundRequestRepository.save(any(FoundRequest.class)))
                    .willReturn(existingFoundRequest(1L, finderId));

            // when
            underTest.create(finderId, "photo", PetType.CAT, createPoint(50.0, 30.0), "test");

            // then
            verify(userService).findById(finderId);
        }

        @Test
        void create_publishesFoundRequestCreatedEvent_withSavedRequestId() {
            // given
            Long finderId = 1L;
            given(userService.findById(finderId)).willReturn(existingUser(finderId));

            FoundRequest savedRequest = existingFoundRequest(100L, finderId);
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when
            underTest.create(finderId, "photo", PetType.CAT, createPoint(50.0, 30.0), "test");

            // then
            ArgumentCaptor<FoundRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(FoundRequestCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            FoundRequestCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.foundRequestId()).isEqualTo(100L);
        }

        @Test
        void create_savesFoundRequestWithCorrectData() {
            // given
            Long finderId = 1L;
            String photoUrl = "photo123";
            PetType petType = PetType.DOG;
            Point location = createPoint(50.45, 30.52);
            String description = "Brown dog found near park";

            given(userService.findById(finderId)).willReturn(existingUser(finderId));
            given(foundRequestRepository.save(any(FoundRequest.class)))
                    .willReturn(existingFoundRequest(1L, finderId));

            // when
            underTest.create(finderId, photoUrl, petType, location, description);

            // then
            ArgumentCaptor<FoundRequest> requestCaptor = ArgumentCaptor.forClass(FoundRequest.class);
            verify(foundRequestRepository).save(requestCaptor.capture());

            FoundRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getFinderId()).isEqualTo(finderId);
            assertThat(capturedRequest.getPhotoUrl()).isEqualTo(photoUrl);
            assertThat(capturedRequest.getPetType()).isEqualTo(petType);
            assertThat(capturedRequest.getLocation()).isEqualTo(location);
            assertThat(capturedRequest.getDescription()).isEqualTo(description);
        }

        @Test
        void create_whenCreatedAtIsNull_throwsPetBedException() {
            // given
            Long finderId = 1L;
            given(userService.findById(finderId)).willReturn(existingUser(finderId));

            FoundRequest savedRequest = FoundRequest.create(
                    finderId, "photo", PetType.DOG, createPoint(50.0, 30.0), "test"
            );
            ReflectionTestUtils.setField(savedRequest, "id", 1L);
            // createdAt is null by default
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when/then
            assertThatThrownBy(() -> underTest.create(finderId, "photo", PetType.DOG, createPoint(50.0, 30.0), "test"))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("createdAt is null");
        }

        @Test
        void create_whenUserNotFound_throwsException() {
            // given
            Long finderId = 999L;
            given(userService.findById(finderId))
                    .willThrow(new PetBedException("User not found", PetBedException.ErrorCode.USER_NOT_FOUND));

            // when/then
            assertThatThrownBy(() -> underTest.create(finderId, "photo", PetType.DOG, createPoint(50.0, 30.0), "test"))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_NOT_FOUND);

            verify(foundRequestRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // .findById() ----------------------------------------------------------------

    @Nested
    class FindById {

        @Test
        void findById_existingRequest_returnsDto() {
            // given
            Long requestId = 1L;
            FoundRequest request = existingFoundRequest(requestId, 1L);
            given(foundRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            FoundRequestDTO result = underTest.findById(requestId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(requestId);
            assertThat(result.finderId()).isEqualTo(1L);
            assertThat(result.photoUrl()).isEqualTo("photo123");
            assertThat(result.description()).isEqualTo("Brown dog found near park");
            verify(foundRequestRepository).findById(requestId);
        }

        @Test
        void findById_notExistingRequest_throwsPetBedException_withCorrectErrorCode() {
            // given
            Long requestId = 999L;
            given(foundRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> underTest.findById(requestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.FOUND_REQUEST_REQUIRED)
                    .hasMessageContaining("not found");

            verify(foundRequestRepository).findById(requestId);
        }

        @Test
        void findById_whenCreatedAtIsNull_throwsPetBedException() {
            // given
            Long requestId = 1L;
            FoundRequest request = FoundRequest.create(
                    1L, "photo", PetType.DOG, createPoint(50.0, 30.0), "test"
            );
            ReflectionTestUtils.setField(request, "id", requestId);
            // createdAt is null by default
            given(foundRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when/then
            assertThatThrownBy(() -> underTest.findById(requestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR)
                    .hasMessageContaining("createdAt is null");
        }
    }

    // .findByFinderId() ----------------------------------------------------------

    @Nested
    class FindByFinderId {

        @Test
        void findByFinderId_withExistingRequests_returnsListOfDtos() {
            // given
            Long finderId = 1L;
            FoundRequest request1 = existingFoundRequest(1L, finderId);
            FoundRequest request2 = FoundRequest.create(
                    finderId,
                    "photo456",
                    PetType.CAT,
                    createPoint(50.46, 30.53),
                    "White cat found"
            );
            ReflectionTestUtils.setField(request2, "id", 2L);
            ReflectionTestUtils.setField(request2, "createdAt", Instant.parse("2024-01-16T12:00:00Z"));

            given(foundRequestRepository.findByFinderId(finderId))
                    .willReturn(List.of(request1, request2));

            // when
            List<FoundRequestDTO> result = underTest.findByFinderId(finderId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(1L);
            assertThat(result.get(0).photoUrl()).isEqualTo("photo123");
            assertThat(result.get(1).id()).isEqualTo(2L);
            assertThat(result.get(1).photoUrl()).isEqualTo("photo456");
            assertThat(result.get(1).petType()).isEqualTo(PetType.CAT);
            verify(foundRequestRepository).findByFinderId(finderId);
        }

        @Test
        void findByFinderId_withNoRequests_returnsEmptyList() {
            // given
            Long finderId = 1L;
            given(foundRequestRepository.findByFinderId(finderId)).willReturn(List.of());

            // when
            List<FoundRequestDTO> result = underTest.findByFinderId(finderId);

            // then
            assertThat(result).isEmpty();
            verify(foundRequestRepository).findByFinderId(finderId);
        }

        @Test
        void findByFinderId_whenOneRequestHasNullCreatedAt_throwsPetBedException() {
            // given
            Long finderId = 1L;
            FoundRequest request1 = existingFoundRequest(1L, finderId);
            FoundRequest request2 = FoundRequest.create(
                    finderId, "photo", PetType.CAT, createPoint(50.0, 30.0), "test"
            );
            ReflectionTestUtils.setField(request2, "id", 2L);
            // createdAt is null by default

            given(foundRequestRepository.findByFinderId(finderId))
                    .willReturn(List.of(request1, request2));

            // when/then
            assertThatThrownBy(() -> underTest.findByFinderId(finderId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR)
                    .hasMessageContaining("createdAt is null");
        }

        @Test
        void findByFinderId_returnsRequestsSortedByRepositoryOrder() {
            // given
            Long finderId = 1L;
            FoundRequest request1 = existingFoundRequest(1L, finderId);
            FoundRequest request2 = FoundRequest.create(
                    finderId,
                    "photo2",
                    PetType.CAT,
                    createPoint(51.0, 31.0),
                    "Second request"
            );
            ReflectionTestUtils.setField(request2, "id", 2L);
            ReflectionTestUtils.setField(request2, "createdAt", Instant.parse("2024-01-20T10:00:00Z"));

            // Repository returns in specific order
            given(foundRequestRepository.findByFinderId(finderId))
                    .willReturn(List.of(request2, request1));

            // when
            List<FoundRequestDTO> result = underTest.findByFinderId(finderId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(2L); // First from repository
            assertThat(result.get(1).id()).isEqualTo(1L); // Second from repository
        }
    }
}
