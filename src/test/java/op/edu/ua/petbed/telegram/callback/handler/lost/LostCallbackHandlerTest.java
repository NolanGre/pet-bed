package op.edu.ua.petbed.telegram.callback.handler.lost;

import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostCallbackHandlerTest {

    @Mock
    LostRequestService lostRequestService;

    @Mock
    TelegramMessageService telegramMessageService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    LostCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_userWithActiveSearches_showsAllButtons() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "user");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            Point location = new GeometryFactory().createPoint(new Coordinate(30.5234, 50.4501));
            List<LostRequestDTO> activeSearches = List.of(
                    new LostRequestDTO(1L, 1L, "+380991234567", location, PetType.DOG, Instant.now())
            );
            given(lostRequestService.findActiveByOwnerId(userId)).willReturn(activeSearches);
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

            // 3 nav buttons + 1 back button = 4 buttons
            assertThat(allButtons).hasSize(4);

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();
            assertThat(buttonTexts)
                    .contains("🔎 Почати пошук")
                    .contains("🐕 Я знайшов тварину")
                    .contains("📋 Мої пошуки")
                    .contains("⬅️ Повернутись");
        }

        @Test
        void handle_userWithoutActiveSearches_hidesMySearchesButton() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "user");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);

            given(lostRequestService.findActiveByOwnerId(userId)).willReturn(List.of());
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

            // 2 nav buttons + 1 back button = 3 buttons
            assertThat(allButtons).hasSize(3);

            List<String> buttonTexts = allButtons.stream()
                    .map(InlineKeyboardButton::getText)
                    .toList();

            assertThat(buttonTexts)
                    .contains("🔎 Почати пошук")
                    .contains("🐕 Я знайшов тварину")
                    .doesNotContain("📋 Мої пошуки")
                    .contains("⬅️ Повернутись");
        }

        @Test
        void handle_displaysCorrectMessageText() {
            // given
            Long userId = 1L;
            Long chatId = 123L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "user");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(lostRequestService.findActiveByOwnerId(userId)).willReturn(List.of());
            given(telegramMessageService.editOrSend(eq(context), any(SendMessage.class)))
                    .willReturn(AnswerCallbackQuery.builder().callbackQueryId("query-id").build());

            // when
            underTest.handle(context);

            // then
            ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
            verify(telegramMessageService).editOrSend(eq(context), captor.capture());

            SendMessage message = captor.getValue();
            assertThat(message.getText())
                    .contains("🔍 Пошук тварин")
                    .contains("Запустити пошук загубленої тварини")
                    .contains("Повідомити про знайдену тварину")
                    .contains("Переглянути активні пошуки");
        }
    }
}
