package op.edu.ua.petbed.telegram.callback.handler.lost;

import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.lost.domain.service.FinderRecommendationCache;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LostFoundCallbackHandlerTest {

    @Mock
    FormService formService;

    @Mock
    FinderRecommendationCache finderRecommendationCache;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    LostFoundCallbackHandler underTest;

    private static final Long TELEGRAM_USER_ID = 123L;
    private static final Long INTERNAL_USER_ID = 1L;
    private static final Long CHAT_ID = 456L;
    private static final Integer MESSAGE_ID = 10;

    @Nested
    class GetCallbackId {

        @Test
        void getCallbackId_shouldReturnLostFound() {
            // when
            CallbackId result = underTest.getCallbackId();

            // then
            assertThat(result).isEqualTo(CallbackId.LOST_FOUND);
        }
    }

    @Nested
    class Handle {

        @Test
        void handle_shouldClearExistingCacheAndStartForm() {
            // given
            UserAuthContext auth = new UserAuthContext(TELEGRAM_USER_ID, INTERNAL_USER_ID, UserType.REGULAR, "testuser");
            org.mockito.BDDMockito.given(context.auth()).willReturn(auth);
            org.mockito.BDDMockito.given(context.chatId()).willReturn(CHAT_ID);

            SendMessage expectedMessage = SendMessage.builder()
                    .chatId(CHAT_ID.toString())
                    .text("Form started")
                    .build();
            doReturn(expectedMessage).when(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    INTERNAL_USER_ID,
                    CHAT_ID
            );

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            verify(finderRecommendationCache).remove(INTERNAL_USER_ID);
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        void handle_shouldReturnMenuCallback() {
            // given
            UserAuthContext auth = new UserAuthContext(TELEGRAM_USER_ID, INTERNAL_USER_ID, UserType.REGULAR, "testuser");
            org.mockito.BDDMockito.given(context.auth()).willReturn(auth);
            org.mockito.BDDMockito.given(context.chatId()).willReturn(CHAT_ID);

            SendMessage expectedMessage = SendMessage.builder()
                    .chatId(CHAT_ID.toString())
                    .text("Form started")
                    .build();
            doReturn(expectedMessage).when(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    INTERNAL_USER_ID,
                    CHAT_ID
            );

            // when
            underTest.handle(context);

            // then
            verify(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    INTERNAL_USER_ID,
                    CHAT_ID
            );
        }

        @Test
        void handle_withVolunteerUser_shouldAlsoClearCacheAndStartForm() {
            // given
            UserAuthContext auth = new UserAuthContext(TELEGRAM_USER_ID, INTERNAL_USER_ID, UserType.VOLUNTEER, "volunteer");
            org.mockito.BDDMockito.given(context.auth()).willReturn(auth);
            org.mockito.BDDMockito.given(context.chatId()).willReturn(CHAT_ID);

            SendMessage expectedMessage = SendMessage.builder()
                    .chatId(CHAT_ID.toString())
                    .text("Form started")
                    .build();
            doReturn(expectedMessage).when(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    INTERNAL_USER_ID,
                    CHAT_ID
            );

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            verify(finderRecommendationCache).remove(INTERNAL_USER_ID);
            assertThat(result).isEqualTo(expectedMessage);
        }

        @Test
        void handle_shouldUseInternalUserIdFromAuth() {
            // given
            Long differentInternalId = 999L;
            UserAuthContext auth = new UserAuthContext(TELEGRAM_USER_ID, differentInternalId, UserType.REGULAR, "testuser");
            org.mockito.BDDMockito.given(context.auth()).willReturn(auth);
            org.mockito.BDDMockito.given(context.chatId()).willReturn(CHAT_ID);

            SendMessage expectedMessage = SendMessage.builder()
                    .chatId(CHAT_ID.toString())
                    .text("Form started")
                    .build();
            doReturn(expectedMessage).when(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    differentInternalId,
                    CHAT_ID
            );

            // when
            underTest.handle(context);

            // then
            verify(finderRecommendationCache).remove(differentInternalId);
            verify(formService).startCreateForm(
                    FormType.CREATE_FOUND_REQUEST,
                    CallbackId.MENU,
                    differentInternalId,
                    CHAT_ID
            );
        }
    }
}
