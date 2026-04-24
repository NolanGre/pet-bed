package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfileHandlerTest {

    @Mock
    CommandContext context;

    @InjectMocks
    ProfileHandler underTest;

    @Test
    void getCommand_returnsPROFILE() {
        Command result = underTest.getCommand();
        assertThat(result).isEqualTo(Command.PROFILE);
    }

    @Test
    void handle_validContext_returnsUserProfile() {
        // given
        Long chatId = 123L;
        Long internalUserId = 456L;
        String username = "testuser";
        UserAuthContext authContext = new UserAuthContext(123L, internalUserId, UserType.REGULAR, username);

        given(context.chatId()).willReturn(chatId);
        given(context.userAuthContext()).willReturn(authContext);

        // when
        BotApiMethod<?> result = underTest.handle(context);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) result;
        assertThat(sendMessage.getText()).contains("Your Profile");
        // Profile handler shows internal user ID
        assertThat(sendMessage.getText()).contains(String.valueOf(internalUserId));
        assertThat(sendMessage.getText()).contains(username);
    }
}