package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@NullMarked
class CreateLostRequestHandlerTest {

    @Mock
    LostRequestService lostRequestService;

    @Mock
    PetService petService;

    @InjectMocks
    CreateLostRequestHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_withValidData_createsLostRequest() {
            // given
            Long petId = 100L;
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;
            String specialFeatures = "Black spot on left ear";

            FormData data = createFormData(petId, contactInfo, latitude, longitude, specialFeatures);

            Point expectedPoint = createPoint(latitude, longitude);
            LostRequestDTO expectedDto = new LostRequestDTO(1L, petId, contactInfo, expectedPoint, PetType.DOG, Instant.now());
            when(lostRequestService.create(eq(petId), eq(contactInfo), any(Point.class))).thenReturn(expectedDto);

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<Point> pointCaptor = ArgumentCaptor.forClass(Point.class);
            verify(lostRequestService).create(eq(petId), eq(contactInfo), pointCaptor.capture());

            Point capturedPoint = pointCaptor.getValue();
            assertThat(capturedPoint.getX()).isEqualTo(longitude);
            assertThat(capturedPoint.getY()).isEqualTo(latitude);
        }

        @Test
        void handle_withSpecialFeatures_updatesPet() {
            // given
            Long petId = 100L;
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;
            String specialFeatures = "Black spot on left ear";

            FormData data = createFormData(petId, contactInfo, latitude, longitude, specialFeatures);

            when(lostRequestService.create(eq(petId), eq(contactInfo), any(Point.class)))
                    .thenReturn(new LostRequestDTO(1L, petId, contactInfo, createPoint(latitude, longitude), PetType.DOG, Instant.now()));

            // when
            underTest.handle(data);

            // then
            verify(petService).updateSpecialFeatures(petId, specialFeatures);
        }

        @Test
        void handle_withoutSpecialFeatures_skipsPetUpdate() {
            // given
            Long petId = 100L;
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;
            @Nullable String specialFeatures = null;

            FormData data = createFormData(petId, contactInfo, latitude, longitude, specialFeatures);

            when(lostRequestService.create(eq(petId), eq(contactInfo), any(Point.class)))
                    .thenReturn(new LostRequestDTO(1L, petId, contactInfo, createPoint(latitude, longitude), PetType.DOG, Instant.now()));

            // when
            underTest.handle(data);

            // then
            verify(petService, never()).updateSpecialFeatures(any(), any());
        }

        @Test
        void handle_withBlankSpecialFeatures_skipsPetUpdate() {
            // given
            Long petId = 100L;
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;
            String specialFeatures = "   ";

            FormData data = createFormData(petId, contactInfo, latitude, longitude, specialFeatures);

            when(lostRequestService.create(eq(petId), eq(contactInfo), any(Point.class)))
                    .thenReturn(new LostRequestDTO(1L, petId, contactInfo, createPoint(latitude, longitude), PetType.DOG, Instant.now()));

            // when
            underTest.handle(data);

            // then
            verify(petService, never()).updateSpecialFeatures(any(), any());
        }

        @Test
        void handle_returnsSuccessMessageWithBackButton() {
            // given
            Long petId = 100L;
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;
            CallbackId returnCallback = CallbackId.MY_PETS;

            FormData data = createFormDataWithCallback(petId, contactInfo, latitude, longitude, null, returnCallback);

            when(lostRequestService.create(eq(petId), eq(contactInfo), any(Point.class)))
                    .thenReturn(new LostRequestDTO(1L, petId, contactInfo, createPoint(latitude, longitude), PetType.DOG, Instant.now()));

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Пошук запущено");
            assertThat(message.getChatId()).isEqualTo("123");
            assertThat(message.getReplyMarkup()).isInstanceOf(InlineKeyboardMarkup.class);
        }

        @Test
        void handle_entityIdNull_throwsPetBedException() {
            // given
            String contactInfo = "+380991234567";
            double latitude = 50.45;
            double longitude = 30.52;

            FormData data = createFormDataWithNullPetId(contactInfo, latitude, longitude);

            // then
            assertThatThrownBy(() -> underTest.handle(data))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("Entity ID is required");
        }

        private FormData createFormData(Long petId, String contactInfo, double latitude, double longitude, @Nullable String specialFeatures) {
            return createFormDataWithCallback(petId, contactInfo, latitude, longitude, specialFeatures, CallbackId.LOST_START);
        }

        private FormData createFormDataWithCallback(Long petId, String contactInfo, double latitude, double longitude, @Nullable String specialFeatures, CallbackId returnCallback) {
            List<FormStep> steps = FormType.CREATE_LOST_REQUEST.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Text(contactInfo));
            answers.put(steps.get(1), new FormInput.Location(latitude, longitude));
            if (specialFeatures != null) {
                answers.put(steps.get(2), new FormInput.Text(specialFeatures));
            }

            return new FormData(1L, 123L, returnCallback, answers, petId);
        }

        @SuppressWarnings("NullAway")
        private FormData createFormDataWithNullPetId(String contactInfo, double latitude, double longitude) {
            List<FormStep> steps = FormType.CREATE_LOST_REQUEST.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Text(contactInfo));
            answers.put(steps.get(1), new FormInput.Location(latitude, longitude));

            return new FormData(1L, 123L, CallbackId.LOST_START, answers, null);
        }

        private Point createPoint(double latitude, double longitude) {
            GeometryFactory geometryFactory = new GeometryFactory();
            return geometryFactory.createPoint(new Coordinate(longitude, latitude));
        }
    }
}
