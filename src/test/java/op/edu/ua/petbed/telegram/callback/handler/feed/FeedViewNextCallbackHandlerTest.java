package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
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
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedViewNextCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    CallbackQueryContext context;

    @Mock
    TelegramClient telegramClient;

    @InjectMocks
    FeedViewNextCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_hasPosts_returnsPhotoMessage() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 10;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);

            FeedPostDTO post = new FeedPostDTO(
                    1L, 2L, "publisher", "Next post", "photo456",
                    50.45, 30.52, 2000.0, Instant.now()
            );
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getChatId()).isEqualTo(chatId.toString());
            assertThat(photo.getCaption()).contains("Next post");
            assertThat(photo.getCaption()).contains("2.0 км від вас");

            verify(feedService).findNextPostAndMarkAsViewed(userId);
        }

        @Test
        void handle_noMorePosts_returnsNoMoreMessage() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 10;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(null);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Більше публікацій немає");
        }

        @Test
        void handle_postWithLessThan1km_showsMeters() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 10;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);

            FeedPostDTO post = new FeedPostDTO(
                    1L, 2L, "publisher", "Close post", "photo123",
                    50.45, 30.52, 800.0, Instant.now()  // менше ніж 1 км
            );
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getCaption()).contains("800м від вас");
        }
    }
}
