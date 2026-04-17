package op.edu.ua.petbed.telegram.command;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CommandTest {

    @Test
    void fromLabel_validCommand_returnsCommand() {
        // when
        Command result = Command.fromLabel("/start");

        // then
        assertThat(result).isEqualTo(Command.START);
    }

    @Test
    void fromLabel_unknownCommand_returnsDefault() {
        // when
        Command result = Command.fromLabel("/unknown");

        // then
        assertThat(result).isEqualTo(Command.DEFAULT);
    }

    @Test
    void fromLabel_withArgs_returnsCommand() {
        // when
        Command result = Command.fromLabel("/start some arguments here");

        // then
        assertThat(result).isEqualTo(Command.START);
    }

    @Test
    void fromLabel_nullBlank_returnsDefault() {
        // when
        Command resultBlank = Command.fromLabel("");
        Command resultNull = Command.fromLabel(null);
        Command resultWhitespace = Command.fromLabel("   ");

        // then
        assertThat(resultBlank).isEqualTo(Command.DEFAULT);
        assertThat(resultNull).isEqualTo(Command.DEFAULT);
        assertThat(resultWhitespace).isEqualTo(Command.DEFAULT);
    }

    @Test
    void getLabel_returnsCommandLabel() {
        // then
        assertThat(Command.START.getLabel()).isEqualTo("/start");
        assertThat(Command.MENU.getLabel()).isEqualTo("/menu");
        assertThat(Command.PROFILE.getLabel()).isEqualTo("/profile");
        assertThat(Command.DEFAULT.getLabel()).isEmpty();
    }

    @Test
    void requiresAuth_returnsTrueForSTARTandPROFILE() {
        // then
        assertThat(Command.START.requiresAuth()).isTrue();
        assertThat(Command.PROFILE.requiresAuth()).isTrue();
        assertThat(Command.MENU.requiresAuth()).isFalse();
        assertThat(Command.DEFAULT.requiresAuth()).isFalse();
    }
}