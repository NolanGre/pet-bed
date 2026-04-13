package op.edu.ua.petbed.telegram.command;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.Assertions.assertThat;
import static op.edu.ua.petbed.telegram.testutil.TelegramUpdateFixtureUtil.withCommand;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CommandContextTest {

    @Test
    void from_validUpdate_returnsContext() {
        // given
        Update update = withCommand("/start", 123L, "testuser", 456L);
        Command command = Command.START;

        // when
        CommandContext result = CommandContext.from(update, command);

        // then
        assertThat(result.command()).isEqualTo(Command.START);
        assertThat(result.chatId()).isEqualTo(456L);
        assertThat(result.userId()).isEqualTo(123L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.rawText()).isEqualTo("/start");
        assertThat(result.update()).isEqualTo(update);
    }
}