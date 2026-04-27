package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.FeedPostDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedMenuCallbackHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    TelegramMessageService telegramMessageService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FeedMenuCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_volunteerWithMultiplePosts_returnsAllButtons() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.VOLUNTEER, "volunteer");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            Page<FeedPostDTO> postsPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 2);
            given(feedService.findMyPosts(userId, PageRequest.of(0, 1))).willReturn(postsPage);
            given(telegramMessageService.editOrSend(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId("query-id").build());

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(eq(context), captor.capture());
            
            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            assertThat(allButtons).hasSize(5);

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();
            assertThat(buttonTexts)
                    .contains("👁️ Переглянути оголошення")
                    .contains("🌍 Обрати геолокацію")
                    .contains("➕ Створити оголошення")
                    .contains("📋 Мої оголошення");
        }

        @Test
        void handle_volunteerWithOnePost_showsMyPostsButton() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.VOLUNTEER, "volunteer");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            Page<FeedPostDTO> postsPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 1);
            given(feedService.findMyPosts(userId, PageRequest.of(0, 1))).willReturn(postsPage);
            given(telegramMessageService.editOrSend(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId("query-id").build());

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(eq(context), captor.capture());
            
            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(allButtons).hasSize(5);
            assertThat(buttonTexts).contains("📋 Мої оголошення");
        }

        @Test
        void handle_volunteerWithNoPosts_hidesMyPostsButton() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.VOLUNTEER, "volunteer");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            Page<FeedPostDTO> postsPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 0);
            given(feedService.findMyPosts(userId, PageRequest.of(0, 1))).willReturn(postsPage);
            given(telegramMessageService.editOrSend(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId("query-id").build());

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(eq(context), captor.capture());
            
            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(allButtons).hasSize(4);
            assertThat(buttonTexts).doesNotContain("📋 Мої оголошення");
        }

        @Test
        void handle_regularUser_hidesCreateAndMyPosts() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "regular");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            Page<FeedPostDTO> postsPage = new PageImpl<>(List.of(), PageRequest.of(0, 1), 5);
            given(feedService.findMyPosts(userId, PageRequest.of(0, 1))).willReturn(postsPage);
            given(telegramMessageService.editOrSend(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId("query-id").build());

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(eq(context), captor.capture());
            
            SendMessage message = captor.getValue();
            InlineKeyboardMarkup replyMarkup = (InlineKeyboardMarkup) message.getReplyMarkup();
            List<InlineKeyboardButton> allButtons = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .toList();

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(allButtons).hasSize(3);
            assertThat(buttonTexts)
                    .contains("👁️ Переглянути оголошення")
                    .contains("🌍 Обрати геолокацію")
                    .doesNotContain("➕ Створити оголошення")
                    .doesNotContain("📋 Мої оголошення");
        }
    }
}
