package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedMyPostsCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    TelegramMessageService messageService;

    @Mock
    CallbackQueryContext context;

    @Mock
    CallbackQuery callbackQuery;

    @InjectMocks
    FeedMyPostsCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_volunteerUser_hasPosts_returnsList() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 456;
            String callbackQueryId = "cq123";
            UserAuthContext auth = new UserAuthContext(chatId, userId, UserType.VOLUNTEER, "testuser");
            CallbackData callbackData = new CallbackData(CallbackId.FEED_MY_POSTS.id(), null, null);

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(callbackData);

            FeedPostDTO post = new FeedPostDTO(
                    1L, userId, "publisher", "My post content", "photo123",
                    50.45, 30.52, null, Instant.now()
            );
            Page<FeedPostDTO> postsPage = new PageImpl<>(List.of(post));

            given(feedService.findMyPosts(eq(userId), any(PageRequest.class)))
                    .willReturn(postsPage);
            given(messageService.editOrReplace(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId(callbackQueryId).build());

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(AnswerCallbackQuery.class);
            AnswerCallbackQuery answer = (AnswerCallbackQuery) result;
            assertThat(answer.getCallbackQueryId()).isEqualTo(callbackQueryId);

            verify(feedService).findMyPosts(eq(userId), any(PageRequest.class));
            verify(messageService).editOrReplace(eq(context), any(SendMessage.class));
        }

        @Test
        void handle_volunteerUser_noPosts_returnsEmptyMessage() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            Integer messageId = 456;
            UserAuthContext auth = new UserAuthContext(chatId, userId, UserType.VOLUNTEER, "testuser");
            CallbackData callbackData = new CallbackData(CallbackId.FEED_MY_POSTS.id(), null, 0);

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(callbackData);

            Page<FeedPostDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            given(feedService.findMyPosts(eq(userId), any(PageRequest.class)))
                    .willReturn(emptyPage);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText message = (EditMessageText) result;
            assertThat(message.getText()).contains("У вас поки що немає публікацій");
        }

        @Test
        void handle_nonVolunteerUser_returnsError() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(chatId, userId, UserType.REGULAR, "testuser");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            // when
            PartialBotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Тільки волонтери можуть переглядати публікації");

            verify(feedService, never()).findMyPosts(any(), any());
        }
    }
}
