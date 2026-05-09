package op.edu.ua.petbed.lost.domain.service;

import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
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

    private static CreateFoundRequestDTO createDto(Long finderId, String photoUrl, PetType petType, Point location) {
        return CreateFoundRequestDTO.builder()
                .finderId(finderId)
                .photoUrl(photoUrl)
                .petType(petType)
                .location(location)
                .breed("Brown")
                .build();
    }

    private static FoundRequest existingFoundRequest(Long id, Long finderId) {
        CreateFoundRequestDTO dto = createDto(finderId, "photo123", PetType.DOG, createPoint(50.45, 30.52));
        FoundRequest request = FoundRequest.create(dto);
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
            CreateFoundRequestDTO dto = createDto(1L, "photo123", PetType.DOG, createPoint(50.45, 30.52));

            UserDTO finder = existingUser(1L);
            given(userService.findById(1L)).willReturn(finder);

            FoundRequest savedRequest = existingFoundRequest(1L, 1L);
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when
            FoundRequestDTO result = underTest.create(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.finderId()).isEqualTo(1L);
            assertThat(result.photoUrl()).isEqualTo("photo123");
            assertThat(result.petType()).isEqualTo(PetType.DOG);
            assertThat(result.breedText()).isEqualTo("Brown");
            assertThat(result.createdAt()).isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));

            verify(userService).findById(1L);
            verify(foundRequestRepository).save(any(FoundRequest.class));
        }

        @Test
        void create_callsUserServiceFindById_withCorrectFinderId() {
            // given
            CreateFoundRequestDTO dto = createDto(1L, "photo", PetType.CAT, createPoint(50.0, 30.0));
            given(userService.findById(1L)).willReturn(existingUser(1L));
            given(foundRequestRepository.save(any(FoundRequest.class)))
                    .willReturn(existingFoundRequest(1L, 1L));

            // when
            underTest.create(dto);

            // then
            verify(userService).findById(1L);
        }

        @Test
        void create_publishesFoundRequestCreatedEvent_withSavedRequestId() {
            // given
            CreateFoundRequestDTO dto = createDto(1L, "photo", PetType.CAT, createPoint(50.0, 30.0));
            given(userService.findById(1L)).willReturn(existingUser(1L));

            FoundRequest savedRequest = existingFoundRequest(100L, 1L);
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when
            underTest.create(dto);

            // then
            ArgumentCaptor<FoundRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(FoundRequestCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            FoundRequestCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.foundRequestId()).isEqualTo(100L);
        }

        @Test
        void create_savesFoundRequestWithCorrectData() {
            // given
            CreateFoundRequestDTO dto = createDto(1L, "photo123", PetType.DOG, createPoint(50.45, 30.52));

            given(userService.findById(1L)).willReturn(existingUser(1L));
            given(foundRequestRepository.save(any(FoundRequest.class)))
                    .willReturn(existingFoundRequest(1L, 1L));

            // when
            underTest.create(dto);

            // then
            ArgumentCaptor<FoundRequest> requestCaptor = ArgumentCaptor.forClass(FoundRequest.class);
            verify(foundRequestRepository).save(requestCaptor.capture());

            FoundRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getFinderId()).isEqualTo(1L);
            assertThat(capturedRequest.getPhotoUrl()).isEqualTo("photo123");
            assertThat(capturedRequest.getPetType()).isEqualTo(PetType.DOG);
            assertThat(capturedRequest.getLocation()).isEqualTo(dto.location());
            assertThat(capturedRequest.getBreedText()).isEqualTo("Brown");
        }

        @Test
        void create_whenCreatedAtIsNull_throwsPetBedException() {
            // given
            CreateFoundRequestDTO dto = createDto(1L, "photo", PetType.DOG, createPoint(50.0, 30.0));
            given(userService.findById(1L)).willReturn(existingUser(1L));

            FoundRequest savedRequest = FoundRequest.create(dto);
            ReflectionTestUtils.setField(savedRequest, "id", 1L);
            // createdAt is null by default
            given(foundRequestRepository.save(any(FoundRequest.class))).willReturn(savedRequest);

            // when/then
            assertThatThrownBy(() -> underTest.create(dto))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("createdAt is null");
        }

        @Test
        void create_whenUserNotFound_throwsException() {
            // given
            CreateFoundRequestDTO dto = createDto(999L, "photo", PetType.DOG, createPoint(50.0, 30.0));
            given(userService.findById(999L))
                    .willThrow(new PetBedException("User not found", PetBedException.ErrorCode.USER_NOT_FOUND));

            // when/then
            assertThatThrownBy(() -> underTest.create(dto))
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
            assertThat(result.breedText()).isEqualTo("Brown");
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
            CreateFoundRequestDTO dto = createDto(1L, "photo", PetType.DOG, createPoint(50.0, 30.0));
            FoundRequest request = FoundRequest.create(dto);
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
            CreateFoundRequestDTO dto2 = createDto(finderId, "photo456", PetType.CAT, createPoint(50.46, 30.53));
            FoundRequest request2 = FoundRequest.create(dto2);
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
            CreateFoundRequestDTO dto2 = createDto(finderId, "photo", PetType.CAT, createPoint(50.0, 30.0));
            FoundRequest request2 = FoundRequest.create(dto2);
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
            CreateFoundRequestDTO dto2 = createDto(finderId, "photo2", PetType.CAT, createPoint(51.0, 31.0));
            FoundRequest request2 = FoundRequest.create(dto2);
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
