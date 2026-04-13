package op.edu.ua.petbed.telegram.command.handler;

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
class DefaultHandlerTest {

    @Mock
    CommandContext context;

    @InjectMocks
    DefaultHandler underTest;

    @Test
    void getCommand_returnsDEFAULT() {
        Command result = underTest.getCommand();
        assertThat(result).isEqualTo(Command.DEFAULT);
    }

    @Test
    void handle_validContext_returnsUnknownMessage() {
        // given
        Long chatId = 123L;
        given(context.chatId()).willReturn(chatId);

        // when
        BotApiMethod<?> result = underTest.handle(context);

        // then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) result;
        assertThat(sendMessage.getText()).contains("Unknown command");
    }
}