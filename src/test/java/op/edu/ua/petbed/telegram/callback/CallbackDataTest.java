package op.edu.ua.petbed.telegram.callback;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackDataTest {

    @Test
    void from_validJson_returnsParsedData() {
        // given
        String json = "{\"a\":\"CONFIRM\",\"p\":\"item1\"}";

        // when
        CallbackData result = CallbackData.from(json);

        // then
        assertThat(result.action()).isEqualTo("CONFIRM");
        assertThat(result.payload()).isEqualTo("item1");
        assertThat(result.offset()).isNull();
    }

    @Test
    void from_jsonWithOffset_returnsParsedData() {
        // given
        String json = "{\"a\":\"PAGINATION\",\"o\":10}";

        // when
        CallbackData result = CallbackData.from(json);

        // then
        assertThat(result.action()).isEqualTo("PAGINATION");
        assertThat(result.payload()).isNull();
        assertThat(result.offset()).isEqualTo(10);
    }

    @Test
    void from_blankJson_returnsUnknown() {
        // when
        CallbackData result = CallbackData.from("");

        // then
        assertThat(result.action()).isEqualTo("unknown");
    }

    @Test
    void from_invalidJson_returnsUnknown() {
        // when
        CallbackData result = CallbackData.from("not valid json");

        // then
        assertThat(result.action()).isEqualTo("unknown");
    }

    @Test
    void toJson_validData_returnsJson() {
        // given
        CallbackData data = new CallbackData("CONFIRM", "item1", null);

        // when
        String result = data.toJson();

        // then
        assertThat(result).contains("\"a\":\"CONFIRM\"");
        assertThat(result).contains("\"p\":\"item1\"");
    }

    @Test
    void toJson_withOffset_returnsJson() {
        // given
        CallbackData data = new CallbackData("PAGINATION", null, 20);

        // when
        String result = data.toJson();

        // then
        assertThat(result).contains("\"a\":\"PAGINATION\"");
        assertThat(result).contains("\"o\":20");
    }
}