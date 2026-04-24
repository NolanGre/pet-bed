package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.user.UserService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AddPetHandledTest {

    @Mock
    PetService petService;

    @Mock
    UserService userService;

    @InjectMocks
    AddPetHandled underTest;

    // .handle() -----------------------------------------------------------------

    @Nested
    class Handle {

        @Test
        void handle_validFormData_createsPetAndReturnsMessage() {
            // given
            List<FormStep> steps = FormType.ADD_PET.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Text("Barsik"));        // name
            answers.put(steps.get(1), new FormInput.Text("CAT"));       // type
            answers.put(steps.get(2), new FormInput.Photo("abc123"));    // photo
            answers.put(steps.get(3), new FormInput.Text("Persian"));    // breed
            answers.put(steps.get(4), new FormInput.Text("White"));     // color
            answers.put(steps.get(5), new FormInput.Text("Solid"));      // colorPattern
            answers.put(steps.get(6), new FormInput.Text("3"));           // age
            answers.put(steps.get(7), new FormInput.Text("MALE"));      // sex
            answers.put(steps.get(8), new FormInput.Text("SMALL"));     // size
            answers.put(steps.get(9), new FormInput.Text("None"));          // specialMarks

            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            UserDTO user = new UserDTO(10L, 123L, "testuser", UserType.REGULAR);
            given(userService.findById(1L)).willReturn(user);

            PetDTO createdPet = new PetDTO(
                    1L,
                    10L,
                    "Barsik",
                    PetType.CAT,
                    "abc123",
                    "Persian",
                    "White",
                    "Solid",
                    3,
                    PetSex.MALE,
                    PetSize.SMALL,
                    "None",
                    PetStatus.DEFAULT
            );
            given(petService.create(any(CreatePetDTO.class))).willReturn(createdPet);

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Barsik був доданий");
            assertThat(message.getChatId()).isEqualTo("123");
            assertThat(message.getReplyMarkup()).isNotNull();

            verify(userService).findById(1L);
            verify(petService).create(any(CreatePetDTO.class));
        }

        @Test
        void handle_userNotFound_throwsPetBedException() {
            // given
            List<FormStep> steps = FormType.ADD_PET.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Text("Barsik"));
            answers.put(steps.get(1), new FormInput.Text("CAT"));
            answers.put(steps.get(2), new FormInput.Photo("abc123"));
            answers.put(steps.get(3), new FormInput.Text("Persian"));
            answers.put(steps.get(4), new FormInput.Text("White"));
            answers.put(steps.get(5), new FormInput.Text("Solid"));
            answers.put(steps.get(6), new FormInput.Text("3"));
            answers.put(steps.get(7), new FormInput.Text("MALE"));
            answers.put(steps.get(8), new FormInput.Text("SMALL"));
            answers.put(steps.get(9), new FormInput.Text("None"));

            FormData data = new FormData(1L, 123L, CallbackId.MY_PETS, answers);

            given(userService.findById(1L))
                    .willThrow(new PetBedException("User not found", PetBedException.ErrorCode.USER_NOT_FOUND));

            // when/then
            assertThatThrownBy(() -> underTest.handle(data))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_NOT_FOUND);
        }
    }
}