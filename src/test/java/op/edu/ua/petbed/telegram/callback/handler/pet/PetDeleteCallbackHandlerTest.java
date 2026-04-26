package op.edu.ua.petbed.telegram.callback.handler.pet;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class PetDeleteCallbackHandlerTest {

    @Mock
    TelegramMessageService messageService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    PetDeleteCallbackHandler underTest;

    private void mockCallbackQuery() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        given(callbackQuery.getId()).willReturn("query-id");
        given(context.callbackQuery()).willReturn(callbackQuery);
    }

    @Nested
    class GetCallbackId {

        @Test
        void returns_PET_DELETE() {
            assertThat(underTest.getCallbackId()).isEqualTo(CallbackId.PET_DELETE);
        }
    }

    @Nested
    class Handle {

        @Test
        void entityIdIsNull_throws_PetBedException() {
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE, null, null));

            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasMessageContaining("Entity ID is required for PET_DELETE");
        }

        @Test
        void entityIdNotNull_calls_messageService_editOrReplace() {
            // given
            Long petId = 42L;
            Long chatId = 123L;
            Integer messageId = 5;
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE, petId, null));
            mockCallbackQuery();

            // when
            underTest.handle(context);

            // then
            verify(messageService).editOrReplace(
                    eq(messageId),
                    any(SendMessage.class)
            );
        }

        @Test
        void returns_AnswerCallbackQuery() {
            // given
            Long petId = 42L;
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE, petId, null));
            mockCallbackQuery();

            // when
            var result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(AnswerCallbackQuery.class);
        }

        @Test
        void sendMessage_contains_confirmation_text() {
            // given
            Long petId = 42L;
            Long chatId = 123L;
            Integer messageId = 5;
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE, petId, null));
            mockCallbackQuery();

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

            // when
            underTest.handle(context);

            // then
            verify(messageService).editOrReplace(eq(messageId), captor.capture());
            assertThat(captor.getValue().getText())
                    .contains("⚠️")
                    .contains("видалити")
                    .contains("неможливо скасувати");
        }

        @Test
        void sendMessage_contains_navButtons_with_petId() {
            // given
            Long petId = 42L;
            Long chatId = 123L;
            Integer messageId = 5;
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.PET_DELETE, petId, null));
            mockCallbackQuery();

            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);

            // when
            underTest.handle(context);

            // then
            verify(messageService).editOrReplace(eq(messageId), captor.capture());
            var replyMarkup = (InlineKeyboardMarkup) captor.getValue().getReplyMarkup();
            assertThat(replyMarkup.getKeyboard()).isNotEmpty();
            assertThat(replyMarkup.getKeyboard().toString()).contains(String.valueOf(CallbackId.PET_DELETE_CONFIRM.id()));
        }
    }
}