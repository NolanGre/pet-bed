package op.edu.ua.petbed.telegram.response;

import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class InlineKeyboardBuilderTest {

    @Test
    void builder_single_row_single_button() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.builder()
                .row(row -> {
                    row.button("Click", "ACTION_1");
                })
                .build();

        assertThat(result.getKeyboard()).hasSize(1);
        assertThat(result.getKeyboard().getFirst().get(0).getText()).isEqualTo("Click");
        assertThat(result.getKeyboard().getFirst().get(0).getCallbackData()).isEqualTo("ACTION_1");
    }

    @Test
    void builder_multiple_rows_multiple_buttons() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.builder()
                .row(row -> {
                    row.button("Take", "TAKE_1");
                    row.button("Save", "SAVE_1");
                })
                .row(row -> {
                    row.button("Next", "NEXT_10");
                })
                .build();

        assertThat(result.getKeyboard()).hasSize(2);
        assertThat(result.getKeyboard().get(0)).hasSize(2);
        assertThat(result.getKeyboard().get(1)).hasSize(1);
    }

    @Test
    void builder_url_button() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.builder()
                .row(row -> {
                    row.urlButton("Open", "https://example.com");
                })
                .build();

        InlineKeyboardButton button = result.getKeyboard().getFirst().getFirst();
        assertThat(button.getText()).isEqualTo("Open");
        assertThat(button.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void simple_single_button() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.simple()
                .button("Click", "ACTION_1")
                .build();

        assertThat(result.getKeyboard()).hasSize(1);
        assertThat(result.getKeyboard().getFirst()).hasSize(1);
    }

    @Test
    void simple_multiple_buttons_same_row() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.simple()
                .button("Take", "TAKE_1")
                .button("Save", "SAVE_1")
                .button("Next", "NEXT_10")
                .build();

        assertThat(result.getKeyboard()).hasSize(1);
        assertThat(result.getKeyboard().getFirst()).hasSize(3);
    }

    @Test
    void simple_url_button() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.simple()
                .urlButton("Website", "https://example.com")
                .build();

        InlineKeyboardButton button = result.getKeyboard().getFirst().getFirst();
        assertThat(button.getText()).isEqualTo("Website");
        assertThat(button.getUrl()).isEqualTo("https://example.com");
    }

    @Test
    void simple_mixed_buttons_and_url() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.simple()
                .button("Take", "TAKE_1")
                .urlButton("Website", "https://example.com")
                .build();

        assertThat(result.getKeyboard()).hasSize(1);
        assertThat(result.getKeyboard().getFirst()).hasSize(2);
    }

    @Test
    void builder_empty_keyboard() {
        InlineKeyboardMarkup result = InlineKeyboardBuilder.builder().build();

        assertThat(result.getKeyboard()).isEmpty();
    }
}