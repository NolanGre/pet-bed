package op.edu.ua.petbed.telegram.service;

import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackAction;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TelegramUpdateRouterImplTest {

    @Mock
    TelegramAuthService authService;

    @Mock
    CommandHandler startHandler;

    @Mock
    CommandHandler defaultHandler;

    @Mock
    CommandHandler profileHandler;

    @Mock
    CommandHandler publishHandler;

    @Mock
    CallbackHandler callbackHandler;

    private TelegramUpdateRouterImpl underTest;

    @BeforeEach
    void setUp() {
        List<CommandHandler> commandHandlers = List.of(startHandler, defaultHandler, profileHandler, publishHandler);
        List<CallbackHandler> callbackHandlers = List.of(callbackHandler);

        given(startHandler.getCommand()).willReturn(Command.START);
        given(defaultHandler.getCommand()).willReturn(Command.DEFAULT);
        given(profileHandler.getCommand()).willReturn(Command.PROFILE);
        given(publishHandler.getCommand()).willReturn(Command.PUBLISH);
        given(callbackHandler.getCallbackAction()).willReturn(CallbackAction.PAGINATION);

        underTest = new TelegramUpdateRouterImpl(authService, commandHandlers, callbackHandlers);
    }

    @Test
    void route_commandMessage_routesToHandler() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/start", 123L, "testuser");
        BotApiMethod<?> handlerResponse = mock(BotApiMethod.class);
        doReturn(handlerResponse).when(startHandler).handle(any());

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isSameAs(handlerResponse);
        verify(startHandler).handle(any());
    }

    @Test
    void route_callbackQuery_routesToHandler() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCallback("{\"a\":\"PAGINATION\",\"p\":\"test\",\"o\":0}", 123L, "testuser", 123L, 1);
        BotApiMethod<?> handlerResponse = mock(BotApiMethod.class);
        doReturn(handlerResponse).when(callbackHandler).handle(any());

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isSameAs(handlerResponse);
        verify(callbackHandler).handle(any());
    }

    @Test
    void route_textMessage_returnsDefault() {
        // given
        Update update = TelegramUpdateFixtureUtil.withTextMessage("Hello", 123L, 456L);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) result;
        assertThat(sendMessage.getText()).contains("Use /help for available commands");
    }

    @Test
    void route_requiresAuth_command_callsAuthFirst() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/profile", 123L, "testuser");
        BotApiMethod<?> handlerResponse = mock(BotApiMethod.class);
        UserAuthContext authContext = new UserAuthContext(1L, UserType.REGULAR);

        doReturn(handlerResponse).when(profileHandler).handle(any());
        given(authService.authenticate(123L, "testuser")).willReturn(authContext);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isSameAs(handlerResponse);
        verify(authService).authenticate(123L, "testuser");
    }

    @Test
    void route_requiresVolunteer_command_callsAuth() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/publish", 123L, "testuser");
        BotApiMethod<?> handlerResponse = mock(BotApiMethod.class);
        UserAuthContext authContext = new UserAuthContext(1L, UserType.VOLUNTEER);

        doReturn(handlerResponse).when(publishHandler).handle(any());
        given(authService.requireVolunteer(123L, "testuser")).willReturn(authContext);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isSameAs(handlerResponse);
        verify(authService).requireVolunteer(123L, "testuser");
    }

    @Test
    void route_unknownCommand_returnsDefaultHandler() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCommand("/unknown", 123L, "testuser");
        BotApiMethod<?> defaultResponse = mock(BotApiMethod.class);
        doReturn(defaultResponse).when(defaultHandler).handle(any());

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isSameAs(defaultResponse);
    }

    @Test
    void route_unknownCallback_returnsDefault() {
        // given
        Update update = TelegramUpdateFixtureUtil.withCallback("{\"a\":\"UNKNOWN\",\"p\":\"test\",\"o\":0}", 123L, "testuser", 123L, 1);

        // when
        BotApiMethod<?> result = underTest.route(update);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) result;
        assertThat(sendMessage.getText()).contains("Use /help for available commands");
    }
}