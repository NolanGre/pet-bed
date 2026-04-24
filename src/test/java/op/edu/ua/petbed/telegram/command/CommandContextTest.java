package op.edu.ua.petbed.telegram.command;

import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;

import static op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil.withCommand;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CommandContextTest {

    @Test
    void from_validUpdate_returnsContext() {
        // given
        Update update = withCommand("/start", 123L, "testuser", 456L);
        Command command = Command.START;
        UserAuthContext authContext = new UserAuthContext(123L, 123L, UserType.REGULAR, "testuser");

        // when
        CommandContext result = CommandContext.from(update, command, authContext);

        // then
        assertThat(result.command()).isEqualTo(Command.START);
        assertThat(result.chatId()).isEqualTo(456L);
        assertThat(result.userAuthContext().userTelegramId()).isEqualTo(123L);
        assertThat(result.userAuthContext().username()).isEqualTo("testuser");
        assertThat(result.rawText()).isEqualTo("/start");
        assertThat(result.update()).isEqualTo(update);
    }
}