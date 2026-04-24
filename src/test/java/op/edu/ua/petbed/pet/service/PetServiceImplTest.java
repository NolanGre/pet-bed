package op.edu.ua.petbed.pet.service;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.pet.model.Pet;
import op.edu.ua.petbed.pet.repository.PetRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PetServiceImplTest {

    @Mock
    PetRepository petRepository;

    @InjectMocks
    PetServiceImpl underTest;

    // .create() ------------------------------------------------------------------

    @Nested
    class Create {

        @Test
        void create_validDto_returnsPet() {
            // given
            CreatePetDTO dto = CreatePetDTO.builder()
                    .ownerId(1L)
                    .name("Barsik")
                    .type(PetType.CAT)
                    .photoId("abc123")
                    .breed("Persian")
                    .color("White")
                    .colorPattern("Solid")
                    .age(3)
                    .sex(PetSex.MALE)
                    .size(PetSize.SMALL)
                    .specialMarks("None")
                    .build();

            Pet savedPet = Pet.create(dto);
            ReflectionTestUtils.setField(savedPet, "id", 1L);
            given(petRepository.save(any(Pet.class))).willReturn(savedPet);

            // when
            PetDTO result = underTest.create(dto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Barsik");
            assertThat(result.type()).isEqualTo(PetType.CAT);
            verify(petRepository).save(any(Pet.class));
        }

        @Test
        void create_repositoryThrows_throwsException() {
            // given
            CreatePetDTO dto = CreatePetDTO.builder()
                    .ownerId(1L)
                    .name("Barsik")
                    .type(PetType.CAT)
                    .photoId("abc123")
                    .breed("Persian")
                    .color("White")
                    .colorPattern("Solid")
                    .age(3)
                    .sex(PetSex.MALE)
                    .size(PetSize.SMALL)
                    .specialMarks("None")
                    .build();

            given(petRepository.save(any(Pet.class)))
                    .willThrow(new RuntimeException("DB Error"));

            // when/then
            assertThatThrownBy(() -> underTest.create(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB Error");
        }
    }

    // .findById() -----------------------------------------------------------------

    @Nested
    class FindById {

        @Test
        void findById_exists_returnsPet() {
            // given
            Pet pet = existingPet(1L, "Barsik");
            given(petRepository.findById(1L)).willReturn(Optional.of(pet));

            // when
            PetDTO result = underTest.findById(1L);

            // then
            assertThat(result).isNotNull();
            assertThat(Objects.requireNonNull(result).id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Barsik");
            verify(petRepository).findById(1L);
        }

        @Test
        void findById_notExists_returnsNull() {
            // given
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when
            PetDTO result = underTest.findById(999L);

            // then
            assertThat(result).isNull();
            verify(petRepository).findById(999L);
        }
    }

    // .findAllByOwnerId() ------------------------------------------------------------

    @Nested
    class FindAllByOwnerId {

        @Test
        void findAllByOwnerId_returnsPage() {
            // given
            Pet pet1 = existingPet(1L, "Barsik");
            Pet pet2 = existingPet(2L, "Murzik");
            Page<Pet> page = new PageImpl<>(List.of(pet1, pet2), PageRequest.of(0, 10), 2);
            given(petRepository.findByOwnerId(1L, PageRequest.of(0, 10))).willReturn(page);

            // when
            Page<PetDTO> result = underTest.findAllByOwnerId(1L, PageRequest.of(0, 10));

            // then
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            verify(petRepository).findByOwnerId(1L, PageRequest.of(0, 10));
        }

        @Test
        void findAllByOwnerId_emptyPage_returnsEmptyPage() {
            // given
            Page<Pet> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(petRepository.findByOwnerId(1L, PageRequest.of(0, 10))).willReturn(page);

            // when
            Page<PetDTO> result = underTest.findAllByOwnerId(1L, PageRequest.of(0, 10));

            // then
            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
            verify(petRepository).findByOwnerId(1L, PageRequest.of(0, 10));
        }
    }

    // .update() ------------------------------------------------------------------

    @Nested
    class Update {

        @Test
        void update_validDto_returnsPet() {
            // given
            Pet pet = existingPet(1L, "Barsik");
            UpdatePetDTO dto = new UpdatePetDTO(1L, "Murzik", null, null, null, null, null, null, null, null);
            given(petRepository.findById(1L)).willReturn(Optional.of(pet));
            given(petRepository.save(any(Pet.class))).willReturn(pet);

            // when
            PetDTO result = underTest.update(dto);

            // then
            assertThat(result).isNotNull();
            verify(petRepository).findById(1L);
            verify(petRepository).save(any(Pet.class));
        }

        @Test
        void update_notExists_throwsPetBedException() {
            // given
            UpdatePetDTO dto = new UpdatePetDTO(999L, "Murzik", null, null, null, null, null, null, null, null);
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> underTest.update(dto))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.PET_NOT_FOUND);
        }
    }

    // .delete() ------------------------------------------------------------------

    @Nested
    class Delete {

        @Test
        void delete_petExists_deletesPet() {
            // given
            Pet pet = existingPet(1L, "Barsik");
            given(petRepository.findById(1L)).willReturn(Optional.of(pet));

            // when
            underTest.delete(1L);

            // then
            verify(petRepository).findById(1L);
            verify(petRepository).delete(pet);
        }

        @Test
        void delete_notExists_throwsPetBedException() {
            // given
            given(petRepository.findById(999L)).willReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> underTest.delete(999L))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.PET_NOT_FOUND);
        }

        @Test
        void delete_cannotDelete_throwsPetBedException() {
            // given
            Pet pet = existingFosteredPet(1L, "Barsik");
            // Mock setup - called in the method but exception thrown before delete
            given(petRepository.findById(1L)).willReturn(Optional.of(pet));

            // when/then
            assertThatThrownBy(() -> underTest.delete(1L))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.PET_CANNOT_DELETE);
            // findById was called, but delete should not be called
            verify(petRepository, org.mockito.Mockito.never()).delete(org.mockito.ArgumentMatchers.any());
        }
    }

    // Helper methods

    private Pet existingPet(Long id, String name) {
        Pet pet = Pet.create(CreatePetDTO.builder()
                .ownerId(1L)
                .name(name)
                .type(PetType.CAT)
                .photoId("abc123")
                .breed("Persian")
                .color("White")
                .colorPattern("Solid")
                .age(3)
                .sex(PetSex.MALE)
                .size(PetSize.SMALL)
                .specialMarks("None")
                .build());
        ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }

    private Pet existingFosteredPet(Long id, String name) {
        Pet pet = existingPet(id, name);
        ReflectionTestUtils.setField(pet, "status", PetStatus.FOSTERED);
        return pet;
    }
}