package op.edu.ua.petbed.lost.domain.service;

import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.application.event.LostRequestCreatedEvent;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.repository.LostRequestRepository;
import op.edu.ua.petbed.pet.PetService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostRequestServiceImplTest {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Mock
    LostRequestRepository lostRequestRepository;

    @Mock
    PetService petService;

    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    LostRequestServiceImpl underTest;

    // Helper methods -----------------------------------------------------------

    private static Point createPoint(double latitude, double longitude) {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private static PetDTO createPetDTO(Long id, String name, PetType type) {
        return new PetDTO(
                id,
                1L,
                name,
                type,
                "photo123",
                "Labrador",
                "Black",
                "Solid",
                3,
                PetSex.MALE,
                PetSize.MEDIUM,
                "White spot on chest",
                PetStatus.DEFAULT
        );
    }

    private static LostRequest createLostRequest(Long id, Long petId, String contactInfo, Point location) {
        PetDTO pet = createPetDTO(petId, "TestPet", PetType.DOG);
        LostRequest request = LostRequest.create(pet, contactInfo, location);
        ReflectionTestUtils.setField(request, "id", id);
        ReflectionTestUtils.setField(request, "createdAt", Instant.now());
        return request;
    }

    // .create() ----------------------------------------------------------------

    @Nested
    class Create {

        @Test
        void create_withValidData_createsLostRequest() {
            // given
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);
            PetDTO pet = createPetDTO(petId, "Barsik", PetType.DOG);

            given(lostRequestRepository.existsByPetId(petId)).willReturn(false);
            given(petService.findById(petId)).willReturn(pet);

            LostRequest savedRequest = createLostRequest(100L, petId, contactInfo, location);
            given(lostRequestRepository.save(any(LostRequest.class))).willReturn(savedRequest);

            // when
            LostRequestDTO result = underTest.create(petId, contactInfo, location);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(100L);
            assertThat(result.petId()).isEqualTo(petId);
            assertThat(result.contactInfo()).isEqualTo(contactInfo);
            assertThat(result.location()).isEqualTo(location);
            assertThat(result.petType()).isEqualTo(PetType.DOG);
            assertThat(result.createdAt()).isNotNull();

            verify(lostRequestRepository).existsByPetId(petId);
            verify(petService).findById(petId);
            verify(lostRequestRepository).save(any(LostRequest.class));
            verify(petService).updateStatus(petId, PetStatus.IN_LOST);
            verify(eventPublisher).publishEvent(any(LostRequestCreatedEvent.class));
        }

        @Test
        void create_whenLostRequestAlreadyExists_throwsPetBedException() {
            // given
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);

            given(lostRequestRepository.existsByPetId(petId)).willReturn(true);

            // when + then
            assertThatThrownBy(() -> underTest.create(petId, contactInfo, location))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);

            verify(lostRequestRepository).existsByPetId(petId);
            verifyNoInteractions(petService);
            verifyNoMoreInteractions(lostRequestRepository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void create_publishesLostRequestCreatedEventWithCorrectId() {
            // given
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);
            PetDTO pet = createPetDTO(petId, "Barsik", PetType.CAT);
            Long expectedRequestId = 200L;

            given(lostRequestRepository.existsByPetId(petId)).willReturn(false);
            given(petService.findById(petId)).willReturn(pet);

            LostRequest savedRequest = createLostRequest(expectedRequestId, petId, contactInfo, location);
            given(lostRequestRepository.save(any(LostRequest.class))).willReturn(savedRequest);

            // when
            underTest.create(petId, contactInfo, location);

            // then
            ArgumentCaptor<LostRequestCreatedEvent> eventCaptor = ArgumentCaptor.forClass(LostRequestCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());

            LostRequestCreatedEvent capturedEvent = eventCaptor.getValue();
            assertThat(capturedEvent.lostRequestId()).isEqualTo(expectedRequestId);
        }

        @Test
        void create_updatesPetStatusToInLost() {
            // given
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);
            PetDTO pet = createPetDTO(petId, "Murzik", PetType.CAT);

            given(lostRequestRepository.existsByPetId(petId)).willReturn(false);
            given(petService.findById(petId)).willReturn(pet);

            LostRequest savedRequest = createLostRequest(300L, petId, contactInfo, location);
            given(lostRequestRepository.save(any(LostRequest.class))).willReturn(savedRequest);

            // when
            underTest.create(petId, contactInfo, location);

            // then
            verify(petService).updateStatus(petId, PetStatus.IN_LOST);
        }

        @Test
        void create_savesLostRequestWithCorrectData() {
            // given
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);
            PetDTO pet = createPetDTO(petId, "Sharik", PetType.DOG);

            given(lostRequestRepository.existsByPetId(petId)).willReturn(false);
            given(petService.findById(petId)).willReturn(pet);

            LostRequest savedRequest = createLostRequest(400L, petId, contactInfo, location);
            given(lostRequestRepository.save(any(LostRequest.class))).willReturn(savedRequest);

            // when
            underTest.create(petId, contactInfo, location);

            // then
            ArgumentCaptor<LostRequest> requestCaptor = ArgumentCaptor.forClass(LostRequest.class);
            verify(lostRequestRepository).save(requestCaptor.capture());

            LostRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getPetId()).isEqualTo(petId);
            assertThat(capturedRequest.getContactInfo()).isEqualTo(contactInfo);
            assertThat(capturedRequest.getLastSeenLocation()).isEqualTo(location);
            assertThat(capturedRequest.getPetType()).isEqualTo(PetType.DOG);
        }
    }

    // .findById() ---------------------------------------------------------------

    @Nested
    class FindById {

        @Test
        void findById_exists_returnsLostRequestDTO() {
            // given
            Long requestId = 100L;
            Long petId = 1L;
            String contactInfo = "+380991234567";
            Point location = createPoint(50.45, 30.52);
            LostRequest request = createLostRequest(requestId, petId, contactInfo, location);

            given(lostRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            LostRequestDTO result = underTest.findById(requestId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(requestId);
            assertThat(result.petId()).isEqualTo(petId);
            assertThat(result.contactInfo()).isEqualTo(contactInfo);
            assertThat(result.location()).isEqualTo(location);
            assertThat(result.petType()).isEqualTo(PetType.DOG);
            assertThat(result.createdAt()).isNotNull();

            verify(lostRequestRepository).findById(requestId);
        }

        @Test
        void findById_notExists_throwsPetBedException() {
            // given
            Long requestId = 999L;
            given(lostRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> underTest.findById(requestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_REQUIRED);

            verify(lostRequestRepository).findById(requestId);
        }
    }

    // .findActiveByOwnerId() ----------------------------------------------------

    @Nested
    class FindActiveByOwnerId {

        @Test
        void findActiveByOwnerId_withPetsHavingLostRequests_returnsListOfDTOs() {
            // given
            Long ownerId = 1L;
            Long petId1 = 10L;
            Long petId2 = 20L;

            PetDTO pet1 = createPetDTO(petId1, "Barsik", PetType.CAT);
            PetDTO pet2 = createPetDTO(petId2, "Murzik", PetType.DOG);
            Page<PetDTO> petPage = new PageImpl<>(List.of(pet1, pet2));

            given(petService.findAllByOwnerId(eq(ownerId), any())).willReturn(petPage);

            Point location1 = createPoint(50.45, 30.52);
            Point location2 = createPoint(50.46, 30.53);
            LostRequest request1 = createLostRequest(100L, petId1, "+380991111111", location1);
            LostRequest request2 = createLostRequest(200L, petId2, "+380992222222", location2);

            given(lostRequestRepository.findByPetId(petId1)).willReturn(request1);
            given(lostRequestRepository.findByPetId(petId2)).willReturn(request2);

            // when
            List<LostRequestDTO> result = underTest.findActiveByOwnerId(ownerId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).id()).isEqualTo(100L);
            assertThat(result.get(0).petId()).isEqualTo(petId1);
            assertThat(result.get(1).id()).isEqualTo(200L);
            assertThat(result.get(1).petId()).isEqualTo(petId2);

            verify(petService).findAllByOwnerId(eq(ownerId), any());
            verify(lostRequestRepository).findByPetId(petId1);
            verify(lostRequestRepository).findByPetId(petId2);
        }

        @Test
        void findActiveByOwnerId_withNoPets_returnsEmptyList() {
            // given
            Long ownerId = 1L;
            Page<PetDTO> emptyPage = new PageImpl<>(List.of());

            given(petService.findAllByOwnerId(eq(ownerId), any())).willReturn(emptyPage);

            // when
            List<LostRequestDTO> result = underTest.findActiveByOwnerId(ownerId);

            // then
            assertThat(result).isEmpty();

            verify(petService).findAllByOwnerId(eq(ownerId), any());
            verifyNoInteractions(lostRequestRepository);
        }

        @Test
        void findActiveByOwnerId_withPetsWithoutLostRequests_returnsEmptyList() {
            // given
            Long ownerId = 1L;
            Long petId1 = 10L;
            Long petId2 = 20L;

            PetDTO pet1 = createPetDTO(petId1, "Barsik", PetType.CAT);
            PetDTO pet2 = createPetDTO(petId2, "Murzik", PetType.DOG);
            Page<PetDTO> petPage = new PageImpl<>(List.of(pet1, pet2));

            given(petService.findAllByOwnerId(eq(ownerId), any())).willReturn(petPage);
            given(lostRequestRepository.findByPetId(petId1)).willReturn(null);
            given(lostRequestRepository.findByPetId(petId2)).willReturn(null);

            // when
            List<LostRequestDTO> result = underTest.findActiveByOwnerId(ownerId);

            // then
            assertThat(result).isEmpty();

            verify(petService).findAllByOwnerId(eq(ownerId), any());
            verify(lostRequestRepository).findByPetId(petId1);
            verify(lostRequestRepository).findByPetId(petId2);
        }

        @Test
        void findActiveByOwnerId_withMixedPets_returnsOnlyPetsWithLostRequests() {
            // given
            Long ownerId = 1L;
            Long petId1 = 10L;
            Long petId2 = 20L;

            PetDTO pet1 = createPetDTO(petId1, "Barsik", PetType.CAT);
            PetDTO pet2 = createPetDTO(petId2, "Murzik", PetType.DOG);
            Page<PetDTO> petPage = new PageImpl<>(List.of(pet1, pet2));

            given(petService.findAllByOwnerId(eq(ownerId), any())).willReturn(petPage);

            Point location1 = createPoint(50.45, 30.52);
            LostRequest request1 = createLostRequest(100L, petId1, "+380991111111", location1);

            given(lostRequestRepository.findByPetId(petId1)).willReturn(request1);
            given(lostRequestRepository.findByPetId(petId2)).willReturn(null);

            // when
            List<LostRequestDTO> result = underTest.findActiveByOwnerId(ownerId);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(100L);
            assertThat(result.get(0).petId()).isEqualTo(petId1);
        }
    }

    // .cancel() ----------------------------------------------------------------

    @Nested
    class Cancel {

        @Test
        void cancel_withValidId_updatesPetStatusAndDeletesRequest() {
            // given
            Long requestId = 100L;
            Long petId = 1L;
            Point location = createPoint(50.45, 30.52);
            LostRequest request = createLostRequest(requestId, petId, "+380991234567", location);

            given(lostRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            underTest.cancel(requestId);

            // then
            verify(lostRequestRepository).findById(requestId);
            verify(petService).updateStatus(petId, PetStatus.DEFAULT);
            verify(lostRequestRepository).deleteByPetId(petId);
        }

        @Test
        void cancel_notExists_throwsPetBedException() {
            // given
            Long requestId = 999L;
            given(lostRequestRepository.findById(requestId)).willReturn(Optional.empty());

            // when + then
            assertThatThrownBy(() -> underTest.cancel(requestId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.LOST_REQUEST_REQUIRED);

            verify(lostRequestRepository).findById(requestId);
            verifyNoInteractions(petService);
            verify(lostRequestRepository, never()).deleteByPetId(any());
        }

        @Test
        void cancel_restoresPetStatusToDefault() {
            // given
            Long requestId = 100L;
            Long petId = 1L;
            Point location = createPoint(50.45, 30.52);
            LostRequest request = createLostRequest(requestId, petId, "+380991234567", location);

            given(lostRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            underTest.cancel(requestId);

            // then
            verify(petService).updateStatus(petId, PetStatus.DEFAULT);
        }

        @Test
        void cancel_deletesByPetId() {
            // given
            Long requestId = 100L;
            Long petId = 1L;
            Point location = createPoint(50.45, 30.52);
            LostRequest request = createLostRequest(requestId, petId, "+380991234567", location);

            given(lostRequestRepository.findById(requestId)).willReturn(Optional.of(request));

            // when
            underTest.cancel(requestId);

            // then
            verify(lostRequestRepository).deleteByPetId(petId);
        }
    }

    // .existsByPetId() ----------------------------------------------------------

    @Nested
    class ExistsByPetId {

        @Test
        void existsByPetId_whenExists_returnsTrue() {
            // given
            Long petId = 1L;
            given(lostRequestRepository.existsByPetId(petId)).willReturn(true);

            // when
            boolean result = underTest.existsByPetId(petId);

            // then
            assertThat(result).isTrue();
            verify(lostRequestRepository).existsByPetId(petId);
        }

        @Test
        void existsByPetId_whenNotExists_returnsFalse() {
            // given
            Long petId = 1L;
            given(lostRequestRepository.existsByPetId(petId)).willReturn(false);

            // when
            boolean result = underTest.existsByPetId(petId);

            // then
            assertThat(result).isFalse();
            verify(lostRequestRepository).existsByPetId(petId);
        }
    }
}
