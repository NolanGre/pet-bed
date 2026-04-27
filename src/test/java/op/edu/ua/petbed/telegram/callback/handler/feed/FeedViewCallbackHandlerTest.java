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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedViewCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FeedViewCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_hasPosts_returnsPhotoMessage() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            FeedPostDTO post = new FeedPostDTO(
                    1L, 2L, "publisher", "Test post", "photo123",
                    50.45, 30.52, 1500.0, Instant.now()
            );
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getChatId()).isEqualTo(chatId.toString());
            assertThat(photo.getCaption()).contains("Test post");
            assertThat(photo.getCaption()).contains("1.5 км від вас");
        }

        @Test
        void handle_noPosts_returnsTextMessage() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(null);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Немає публікацій");
        }

        @Test
        void handle_postWithoutDistance_hidesDistance() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            FeedPostDTO post = new FeedPostDTO(
                    1L, 2L, "publisher", "Test post", "photo123",
                    50.45, 30.52, null, Instant.now()
            );
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getCaption()).doesNotContain("від вас");
        }

        @Test
        void handle_postWithLessThan1km_showsMeters() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            FeedPostDTO post = new FeedPostDTO(
                    1L, 2L, "publisher", "Test post", "photo123",
                    50.45, 30.52, 500.0, Instant.now()
            );
            given(feedService.findNextPostAndMarkAsViewed(userId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getCaption()).contains("500м від вас");
        }
    }
}