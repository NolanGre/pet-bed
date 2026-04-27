package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.feed.FeedService;
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
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedPostDetailCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FeedPostDetailCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_validPost_returnsPhotoMessage() {
            // given
            Long chatId = 123L;
            Long postId = 1L;

            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(
                    CallbackData.of(CallbackId.FEED_POST_DETAIL, postId, null)
            );

            FeedPostDTO post = new FeedPostDTO(
                    postId, 2L, "publisherUsername", "Test post text", "photo123",
                    50.45, 30.52, 1500.0, Instant.now()
            );
            given(feedService.findById(postId)).willReturn(post);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendPhoto.class);
            SendPhoto photo = (SendPhoto) result;
            assertThat(photo.getChatId()).isEqualTo(chatId.toString());
            assertThat(photo.getPhoto().getAttachName()).isEqualTo("photo123");
            assertThat(photo.getCaption()).contains("Test post text");
            assertThat(photo.getCaption()).contains("1.5 км від вас");
            assertThat(photo.getCaption()).contains("@publisherUsername");
        }

        @Test
        void handle_entityIdMissing_throwsException() {
            // given
            given(context.callbackData()).willReturn(
                    CallbackData.of(CallbackId.FEED_POST_DETAIL, null, null)
            );

            // when / then
            assertThatThrownBy(() -> underTest.handle(context))
                    .isInstanceOf(op.edu.ua.petbed.common.exceptions.PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode",
                            op.edu.ua.petbed.common.exceptions.PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }
}