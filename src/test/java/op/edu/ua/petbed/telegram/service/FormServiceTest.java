package op.edu.ua.petbed.telegram.service;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormEntity;
import op.edu.ua.petbed.telegram.form.FormRepository;
import op.edu.ua.petbed.telegram.form.handler.FormSubmissionHandler;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class FormServiceTest {

    @Mock
    FormRepository formRepository;

    @Mock
    FormSubmissionHandler formSubmissionHandler;

    @Mock
    TelegramClient telegramClient;

    @Mock
    TelegramMessageService telegramMessageService;

    @Nested
    class StartForm {

        @Test
        void new_form_returns_prompt_with_entity_saved() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.startCreateForm(FormType.ADD_PET, CallbackId.MY_PETS, userId, chatId);

            // then
            verify(formRepository).deleteById(userId);
            verify(formRepository).save(any(FormEntity.class));

            String text = extractText(result);
            assertThat(text).contains("Введіть ім'я тварини");
            // /cancel is now sent in a separate info message via telegramClient
        }

        @Test
        void existing_form_deletes_old_and_creates_new() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity existingEntity = mock(FormEntity.class);
            lenient().when(formRepository.findById(userId)).thenReturn(Optional.of(existingEntity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            underTest.startCreateForm(FormType.ADD_PET, CallbackId.MY_PETS, userId, chatId);

            // then
            verify(formRepository).deleteById(userId);
            verify(formRepository).save(any(FormEntity.class));
        }
    }

    @Nested
    class ProcessInput {

        @Test
        void valid_input_saves_and_returns_next_prompt() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            FormInput input = new FormInput.Text("Барсик");

            // when
            BotApiMethod<?> result = underTest.processInput(input, userId, chatId);

            // then
            verify(formRepository).save(entity);

            String text = extractText(result);
            // Step 1 is "Оберіть тип тварини"
            assertThat(text).contains("Оберіть тип тварини");
        }

        @Test
        void valid_input_last_step_returns_complete_message() {
            // given - fill 9 steps, last step is step 9 (index 9)
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));           // 0: name
            entity.applyStep(new FormInput.Text("cat"));               // 1: type
            entity.applyStep(new FormInput.Photo("file123"));          // 2: photo
            entity.applyStep(new FormInput.Text("Persian"));           // 3: breed
            entity.applyStep(new FormInput.Text("white"));             // 4: color
            entity.applyStep(new FormInput.Text("solid"));             // 5: pattern
            entity.applyStep(new FormInput.Text("3"));                 // 6: age
            entity.applyStep(new FormInput.Text("male"));              // 7: gender
            entity.applyStep(new FormInput.Text("small"));             // 8: size

            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            FormInput input = new FormInput.Text("friendly");          // 9: notes - last step

            // when
            BotApiMethod<?> result = underTest.processInput(input, userId, chatId);

            // then
            verify(formRepository).save(entity);

            String text = extractText(result);
            assertThat(text).contains("Ви завершили заповнення форми");
            assertThat(text).contains("/submit");
            assertThat(text).contains("/cancel");
        }

        @Test
        void invalid_input_returns_error_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            FormInput input = new FormInput.Text(""); // Empty text - invalid

            // when
            BotApiMethod<?> result = underTest.processInput(input, userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("ℹ️ Очікується саме текст.");
        }

        @Test
        void no_active_form_returns_no_active_form_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            given(formRepository.findById(userId)).willReturn(Optional.empty());
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            FormInput input = new FormInput.Text("test");

            // when
            BotApiMethod<?> result = underTest.processInput(input, userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("немає активних форм");
        }
    }

    @Nested
    class ConfirmForm {

        @Test
        void incomplete_form_returns_form_not_complete_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.confirmForm(userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("ще не заповнена");
        }

        @Test
        void no_active_form_returns_no_active_form_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            given(formRepository.findById(userId)).willReturn(Optional.empty());
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.confirmForm(userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("немає активних форм");
        }

        @Test
        void no_handler_for_type_throws_PetBedException() {
            // given - fill all 10 steps
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));           // 0: name
            entity.applyStep(new FormInput.Text("cat"));               // 1: type
            entity.applyStep(new FormInput.Photo("file123"));          // 2: photo
            entity.applyStep(new FormInput.Text("Persian"));           // 3: breed
            entity.applyStep(new FormInput.Text("white"));             // 4: color
            entity.applyStep(new FormInput.Text("solid"));             // 5: pattern
            entity.applyStep(new FormInput.Text("3"));                 // 6: age
            entity.applyStep(new FormInput.Text("male"));              // 7: gender
            entity.applyStep(new FormInput.Text("small"));             // 8: size
            entity.applyStep(new FormInput.Text("friendly"));          // 9: notes
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));

            FormService serviceWithHandler = new FormService(formRepository, List.of(), telegramClient, telegramMessageService); // No handlers

            // when & then
            assertThatThrownBy(() -> serviceWithHandler.confirmForm(userId, chatId))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
    }

    @Nested
    class CancelForm {

        @Test
        void active_form_deletes_and_returns_cancelled_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.cancelForm(userId, chatId);

            // then
            verify(formRepository).delete(entity);

            String text = extractText(result);
            assertThat(text).contains("скасовано");
        }

        @Test
        void no_active_form_returns_no_active_form_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            given(formRepository.findById(userId)).willReturn(Optional.empty());
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.cancelForm(userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("немає активних форм");
        }
    }

    @Nested
    class HasActiveForm {

        @Test
        void exists_true() {
            // given
            Long userId = 123L;
            given(formRepository.existsById(userId)).willReturn(true);
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            boolean result = underTest.hasActiveForm(userId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        void not_exists_false() {
            // given
            Long userId = 123L;
            given(formRepository.existsById(userId)).willReturn(false);
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            boolean result = underTest.hasActiveForm(userId);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class SkipStep {

        @Test
        void skip_mandatory_step_returns_warning_with_text() {
            // given - ADD_PET has mandatory steps (cannot skip)
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.skipStep(userId, chatId);

            // then
            verify(formRepository, never()).save(any());  // not saved - still on same step

            String text = extractText(result);
            assertThat(text).contains("неможливо пропустити");
        }

        @Test
        void skip_choice_step_returns_warning() {
            // given - step is choice (type selection)
            Long userId = 123L;
            Long chatId = 456L;
            FormEntity entity = FormEntity.initiateCreate(userId, chatId, FormType.ADD_PET, CallbackId.MY_PETS);
            entity.applyStep(new FormInput.Text("Барсик"));  // skip name
            given(formRepository.findById(userId)).willReturn(Optional.of(entity));
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.skipStep(userId, chatId);

            // then
            verify(formRepository, never()).save(any());  // not saved - still on same step

            String text = extractText(result);
            assertThat(text).contains("неможливо пропустити");
            assertThat(text).contains("Оберіть тип тварини");
        }

        @Test
        void no_active_form_returns_no_active_form_message() {
            // given
            Long userId = 123L;
            Long chatId = 456L;
            given(formRepository.findById(userId)).willReturn(Optional.empty());
            FormService underTest = new FormService(formRepository, List.of(), telegramClient, telegramMessageService);

            // when
            BotApiMethod<?> result = underTest.skipStep(userId, chatId);

            // then
            String text = extractText(result);
            assertThat(text).contains("немає активних форм");
        }
    }

    // Helper methods
    private static String extractText(BotApiMethod<?> method) {
        try {
            var field = method.getClass().getDeclaredField("text");
            field.setAccessible(true);
            return (String) field.get(method);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract text", e);
        }
    }

    private static Object extractReplyMarkup(BotApiMethod<?> method) {
        try {
            var field = method.getClass().getDeclaredField("replyMarkup");
            field.setAccessible(true);
            return field.get(method);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract replyMarkup", e);
        }
    }
}