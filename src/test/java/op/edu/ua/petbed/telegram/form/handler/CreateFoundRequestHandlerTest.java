package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.jspecify.annotations.NonNull;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateFoundRequestHandlerTest {

    @Mock
    FoundRequestService foundRequestService;

    @InjectMocks
    CreateFoundRequestHandler underTest;

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private static final Long USER_ID = 1L;
    private static final Long CHAT_ID = 123L;

    @Nested
    class Handle {

        @Test
        void handle_withValidData_createsFoundRequest() {
            // given
            FormData data = getFormDataWithAllFields();

            FoundRequestDTO createdDto = new FoundRequestDTO(
                    1L,
                    USER_ID,
                    "photo123",
                    PetType.DOG,
                    GEOMETRY_FACTORY.createPoint(new Coordinate(30.52, 50.45)),
                    "Labrador Golden Smooth male medium Friendly and playful",
                    Instant.now()
            );
            given(foundRequestService.create(
                    org.mockito.ArgumentMatchers.eq(USER_ID),
                    org.mockito.ArgumentMatchers.eq("photo123"),
                    org.mockito.ArgumentMatchers.eq(PetType.DOG),
                    org.mockito.ArgumentMatchers.any(Point.class),
                    org.mockito.ArgumentMatchers.anyString()
            )).willReturn(createdDto);

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Анкету збережено");
            assertThat(message.getChatId()).isEqualTo(CHAT_ID.toString());

            ArgumentCaptor<Point> locationCaptor = ArgumentCaptor.forClass(Point.class);
            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);

            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.eq(USER_ID),
                    org.mockito.ArgumentMatchers.eq("photo123"),
                    org.mockito.ArgumentMatchers.eq(PetType.DOG),
                    locationCaptor.capture(),
                    descriptionCaptor.capture()
            );

            Point capturedLocation = locationCaptor.getValue();
            assertThat(capturedLocation.getX()).isEqualTo(30.52);
            assertThat(capturedLocation.getY()).isEqualTo(50.45);
        }

        @Test
        void handle_buildsDescriptionFromTextFields() {
            // given
            FormData data = getFormDataWithTextFieldsOnly();

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    descriptionCaptor.capture()
            );

            String capturedDescription = descriptionCaptor.getValue();
            assertThat(capturedDescription).contains("Labrador");
            assertThat(capturedDescription).contains("Golden");
            assertThat(capturedDescription).contains("Smooth");
        }

        @Test
        void handle_skipsEmptyFieldsInDescription() {
            // given
            FormData data = getFormDataWithSomeEmptyFields();

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    descriptionCaptor.capture()
            );

            String capturedDescription = descriptionCaptor.getValue();
            assertThat(capturedDescription).contains("Labrador");
            assertThat(capturedDescription).doesNotContain("null");
            // Empty fields should not be in the description
            assertThat(capturedDescription).doesNotContain("  ");
        }

        @Test
        void handle_withAllOptionalFields_buildsCompleteDescription() {
            // given
            FormData data = getFormDataWithAllFields();

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    descriptionCaptor.capture()
            );

            String capturedDescription = descriptionCaptor.getValue();
            assertThat(capturedDescription).contains("Labrador");
            assertThat(capturedDescription).contains("Golden");
            assertThat(capturedDescription).contains("Smooth");
            assertThat(capturedDescription).contains("male");
            assertThat(capturedDescription).contains("medium");
            assertThat(capturedDescription).contains("Friendly and playful");
        }

        @Test
        void handle_withNoOptionalFields_buildsEmptyDescription() {
            // given
            FormData data = getFormDataWithNoOptionalFields();

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    descriptionCaptor.capture()
            );

            String capturedDescription = descriptionCaptor.getValue();
            assertThat(capturedDescription).isEmpty();
        }

        @Test
        void handle_returnsSuccessMessageWithRecommendationsButton() {
            // given
            FormData data = getFormDataWithAllFields();

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Анкету збережено");
            assertThat(message.getText()).contains("переглянути анкети буде неможливо");

            InlineKeyboardMarkup keyboard = (InlineKeyboardMarkup) message.getReplyMarkup();
            assertThat(keyboard).isNotNull();
            assertThat(keyboard.getKeyboard()).hasSize(1);
            assertThat(keyboard.getKeyboard().get(0)).hasSize(2);

            // First button should be "View recommendations"
            String firstButtonText = keyboard.getKeyboard().get(0).get(0).getText();
            assertThat(firstButtonText).contains("Переглянути рекомендації");

            // Second button should be "Back"
            String secondButtonText = keyboard.getKeyboard().get(0).get(1).getText();
            assertThat(secondButtonText).contains("Повернутись");
        }

        @Test
        void handle_parsesPetTypeCorrectly() {
            // given
            FormData data = getFormDataWithPetType("CAT");

            // when
            underTest.handle(data);

            // then
            ArgumentCaptor<PetType> petTypeCaptor = ArgumentCaptor.forClass(PetType.class);
            verify(foundRequestService).create(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),
                    petTypeCaptor.capture(),
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any()
            );

            PetType capturedPetType = petTypeCaptor.getValue();
            assertThat(capturedPetType).isEqualTo(PetType.CAT);
        }
    }

    // Helper methods to create FormData instances

    private static @NonNull FormData getFormDataWithAllFields() {
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();
        var answers = new LinkedHashMap<FormStep, FormInput>();
        answers.put(steps.get(0), new FormInput.Choice("DOG"));                      // pet type
        answers.put(steps.get(1), new FormInput.Photo("photo123"));                  // photo
        answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));             // location
        answers.put(steps.get(3), new FormInput.Text("Labrador"));                   // breed
        answers.put(steps.get(4), new FormInput.Text("Golden"));                     // color
        answers.put(steps.get(5), new FormInput.Text("Smooth"));                     // coat type
        answers.put(steps.get(6), new FormInput.Choice("male"));                     // sex
        answers.put(steps.get(7), new FormInput.Choice("medium"));                   // size
        answers.put(steps.get(8), new FormInput.Text("Friendly and playful"));       // special features

        return new FormData(USER_ID, CHAT_ID, CallbackId.MENU, answers);
    }

    private static @NonNull FormData getFormDataWithTextFieldsOnly() {
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();
        var answers = new LinkedHashMap<FormStep, FormInput>();
        answers.put(steps.get(0), new FormInput.Choice("DOG"));                      // pet type
        answers.put(steps.get(1), new FormInput.Photo("photo123"));                  // photo
        answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));             // location
        answers.put(steps.get(3), new FormInput.Text("Labrador"));                   // breed
        answers.put(steps.get(4), new FormInput.Text("Golden"));                     // color
        answers.put(steps.get(5), new FormInput.Text("Smooth"));                     // coat type
        // Skip sex (step 6) and size (step 7)
        answers.put(steps.get(8), new FormInput.Text("Friendly"));                   // special features

        return new FormData(USER_ID, CHAT_ID, CallbackId.MENU, answers);
    }

    private static @NonNull FormData getFormDataWithSomeEmptyFields() {
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();
        var answers = new LinkedHashMap<FormStep, FormInput>();
        answers.put(steps.get(0), new FormInput.Choice("DOG"));                      // pet type
        answers.put(steps.get(1), new FormInput.Photo("photo123"));                  // photo
        answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));             // location
        answers.put(steps.get(3), new FormInput.Text("Labrador"));                   // breed (provided)
        // Skip color (step 4) - null
        // Skip coat type (step 5) - null
        // Skip sex (step 6) - null
        // Skip size (step 7) - null
        // Skip special features (step 8) - null

        return new FormData(USER_ID, CHAT_ID, CallbackId.MENU, answers);
    }

    private static @NonNull FormData getFormDataWithNoOptionalFields() {
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();
        var answers = new LinkedHashMap<FormStep, FormInput>();
        answers.put(steps.get(0), new FormInput.Choice("DOG"));                      // pet type (required)
        answers.put(steps.get(1), new FormInput.Photo("photo123"));                  // photo (required)
        answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));             // location (required)
        // No optional fields

        return new FormData(USER_ID, CHAT_ID, CallbackId.MENU, answers);
    }

    private static @NonNull FormData getFormDataWithPetType(String petTypeValue) {
        List<FormStep> steps = FormType.CREATE_FOUND_REQUEST.steps();
        var answers = new LinkedHashMap<FormStep, FormInput>();
        answers.put(steps.get(0), new FormInput.Choice(petTypeValue));               // pet type
        answers.put(steps.get(1), new FormInput.Photo("photo123"));                  // photo
        answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));             // location

        return new FormData(USER_ID, CHAT_ID, CallbackId.MENU, answers);
    }
}
