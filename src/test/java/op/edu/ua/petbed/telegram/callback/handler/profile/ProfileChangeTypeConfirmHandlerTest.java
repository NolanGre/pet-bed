package op.edu.ua.petbed.telegram.callback.handler.profile;

import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.user.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfileChangeTypeConfirmHandlerTest {

    @Mock
    UserService userService;

    @Mock
    FeedService feedService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    ProfileChangeTypeConfirmHandler underTest;

    @Nested
    @DisplayName("getCallbackId()")
    class GetCallbackId {

        @Test
        void returns_PROFILE_CHANGE_TYPE_CONFIRM() {
            CallbackId result = underTest.getCallbackId();

            assertThat(result).isEqualTo(CallbackId.PROFILE_CHANGE_TYPE_CONFIRM);
        }
    }

    @Nested
    @DisplayName("handle(CallbackQueryContext)")
    class HandleMethod {

        @Test
        void calls_userService_toggleUserType() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.VOLUNTEER, null));

            // when
            underTest.handle(context);

            // then
            verify(userService).toggleUserType(userId);
        }
    }

    @Nested
    @DisplayName("handle() response")
    class HandleResponse {

        @Test
        void returns_editMessage_with_success_message_and_volunteer_type_with_checkmark_emoji() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.VOLUNTEER, null));

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            String text = editMessage.getText();
            assertThat(text)
                    .contains("✅")
                    .contains("змінено")
                    .contains("Волонтер");
        }
    }

    @Nested
    @DisplayName("handle() keyboard")
    class HandleKeyboard {

        @Test
        void keyboard_contains_back_button_to_PROFILE() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.VOLUNTEER, null));

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            InlineKeyboardMarkup replyMarkup = editMessage.getReplyMarkup();

            assertThat(replyMarkup.getKeyboard()).isNotEmpty();

            // Find back button with "⬅️ Повернутись"
            var hasBackButton = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getText().contains("⬅️ Повернутись"));

            assertThat(hasBackButton).isTrue();

            // Verify callback data contains PROFILE id (110)
            String expectedCallbackPrefix = String.valueOf(CallbackId.PROFILE.id());
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith(expectedCallbackPrefix + ","));

            assertThat(hasCorrectCallback).isTrue();
        }

        @Test
        void editMessage_uses_correct_message_id() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 42;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.VOLUNTEER, null));

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            assertThat(editMessage.getChatId()).isEqualTo(chatId.toString());
            assertThat(editMessage.getMessageId()).isEqualTo(messageId);
        }
    }

    @Test
    void throws_exception_when_entityId_is_null() {
        // given
        given(context.callbackData()).willReturn(new CallbackData(112, null, null));

        // when / then
        assertThatThrownBy(() -> underTest.handle(context))
                .isInstanceOf(PetBedException.class)
                .hasMessageContaining("EntityId is required");
    }

    @Nested
    @DisplayName("handle() feed integration")
    class FeedIntegration {

        @Test
        void handle_changesToRegular_deletesAllPosts() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.REGULAR, null));

            // when
            underTest.handle(context);

            // then
            verify(feedService).deleteAllByPublisherId(userId);
        }

        @Test
        void handle_changesToVolunteer_doesNotDeletePosts() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.VOLUNTEER, null));

            // when
            underTest.handle(context);

            // then
            verify(feedService, never()).deleteAllByPublisherId(userId);
        }

        @Test
        void handle_changesToRegular_includesDeleteMessage() {
            // given
            Long userId = 456L;
            Long chatId = 123L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.messageId()).willReturn(messageId);
            given(context.callbackData()).willReturn(new CallbackData(112, userId, null));

            given(userService.toggleUserType(userId))
                    .willReturn(new UserDTO(1L, userId, "username", UserType.REGULAR, null));

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            assertThat(editMessage.getText())
                    .contains("Ваші публікації видалено");
        }
    }
}