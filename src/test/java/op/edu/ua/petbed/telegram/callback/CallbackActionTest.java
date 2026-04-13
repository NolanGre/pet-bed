package op.edu.ua.petbed.telegram.callback;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackActionTest {

    @Test
    void fromString_validAction_returnsAction() {
        // when
        CallbackAction result = CallbackAction.fromString("CONFIRM");

        // then
        assertThat(result).isEqualTo(CallbackAction.CONFIRM);
    }

    @Test
    void fromString_lowercase_returnsAction() {
        // when
        CallbackAction result = CallbackAction.fromString("confirm");

        // then
        assertThat(result).isEqualTo(CallbackAction.CONFIRM);
    }

    @Test
    void fromString_unknown_returnsDefault() {
        // when
        CallbackAction result = CallbackAction.fromString("UNKNOWN_ACTION");

        // then
        assertThat(result).isEqualTo(CallbackAction.DEFAULT);
    }

    @Test
    void fromString_nullBlank_returnsDefault() {
        // when
        CallbackAction resultNull = CallbackAction.fromString(null);
        CallbackAction resultBlank = CallbackAction.fromString("");
        CallbackAction resultWhitespace = CallbackAction.fromString("   ");

        // then
        assertThat(resultNull).isEqualTo(CallbackAction.DEFAULT);
        assertThat(resultBlank).isEqualTo(CallbackAction.DEFAULT);
        assertThat(resultWhitespace).isEqualTo(CallbackAction.DEFAULT);
    }
}