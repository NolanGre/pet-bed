package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PetDeleteConfirmCallbackHandlerTest {

    @Mock
    PetService petService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    PetDeleteConfirmCallbackHandler underTest;

    @Nested
    class GetCallbackId {

        @Test
        void returns_PET_DELETE_CONFIRM() {
            assertThat(underTest.getCallbackId()).isEqualTo(CallbackId.PET_DELETE_CONFIRM);
        }
    }

    @Nested
    class Handle {

        @Test
        void entityIdIsNull_throws_PetBedException() {
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE_CONFIRM, null, null));

            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("Entity ID is required for PET_DELETE_CONFIRM");
        }

        @Test
        void entityIdNotNull_calls_petService_delete() {
            // given
            Long petId = 42L;
            given(context.chatId()).willReturn(123L);
            given(context.messageId()).willReturn(5);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE_CONFIRM, petId, null));

            // when
            underTest.handle(context);

            // then
            verify(petService).delete(petId);
        }

        @Test
        void returns_EditMessageText_with_success_message() {
            // given
            Long petId = 42L;
            Long chatId = 123L;
            Integer messageId = 5;
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE_CONFIRM, petId, null));

            // when
            var result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            assertThat(editMessage.getText())
                    .contains("✅")
                    .contains("видалено");
            assertThat(editMessage.getChatId()).isEqualTo(chatId.toString());
            assertThat(editMessage.getMessageId()).isEqualTo(messageId);
        }

        @Test
        void keyboard_contains_backButton_to_MY_PETS() {
            // given
            Long petId = 42L;
            given(context.chatId()).willReturn(123L);
            given(context.messageId()).willReturn(5);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE_CONFIRM, petId, null));

            // when
            var result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            InlineKeyboardMarkup replyMarkup = editMessage.getReplyMarkup();

            var hasBackButton = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getText().contains("⬅️ Повернутись"));
            assertThat(hasBackButton).isTrue();

            String expectedCallbackPrefix = String.valueOf(CallbackId.MY_PETS.id());
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith(expectedCallbackPrefix + ","));
            assertThat(hasCorrectCallback).isTrue();
        }
    }
}