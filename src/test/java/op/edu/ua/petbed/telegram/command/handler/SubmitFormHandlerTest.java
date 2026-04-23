package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
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
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil.withCommand;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class SubmitFormHandlerTest {

    @Mock
    FormService formService;

    @InjectMocks
    SubmitFormHandler underTest;

    @Nested
    class GetCommand {

        @Test
        void returns_SubmitForm() {
            // when
            Command result = underTest.getCommand();

            // then
            assertThat(result).isEqualTo(Command.SUBMIT_FORM);
        }
    }

    @Nested
    class Handle {

        @Test
        void confirmForm_called() {
            // given
            Update update = withCommand("/submit", 123L, "username", 456L);
            CommandContext context = CommandContext.from(update, Command.SUBMIT_FORM);
            SendMessage expectedResult = SendMessage.builder().chatId("456").text("Done").build();
            doAnswer(invocation -> expectedResult).when(formService).confirmForm(123L);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            verify(formService).confirmForm(123L);
            assertThat(result).isEqualTo(expectedResult);
        }
    }
}