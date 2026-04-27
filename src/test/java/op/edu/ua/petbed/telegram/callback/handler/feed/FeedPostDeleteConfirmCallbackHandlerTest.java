package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
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
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedPostDeleteConfirmCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FeedPostDeleteConfirmCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_validDelete_deletesPostAndReturnsSuccess() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 456;
            Long postId = 10L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");
            CallbackData callbackData = CallbackData.of(CallbackId.FEED_POST_DELETE_CONFIRM, postId, null);

            given(context.auth()).willReturn(auth);
            given(context.callbackData()).willReturn(callbackData);
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            verify(feedService).delete(postId, userId);
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText editMessage = (EditMessageText) result;
            assertThat(editMessage.getChatId()).isEqualTo(chatId.toString());
            assertThat(editMessage.getMessageId()).isEqualTo(messageId);
            assertThat(editMessage.getText()).isEqualTo("✅ Оголошення видалено");
        }

        @Test
        void handle_entityIdMissing_throwsException() {
            // given
            Long userId = 1L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");
            CallbackData callbackData = CallbackData.of(CallbackId.FEED_POST_DELETE_CONFIRM, null, null);

            given(context.callbackData()).willReturn(callbackData);

            // when & then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }
}