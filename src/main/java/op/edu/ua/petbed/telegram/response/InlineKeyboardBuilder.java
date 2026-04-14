package op.edu.ua.petbed.telegram.response;

import org.jspecify.annotations.NullMarked;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Telegram inline keyboards.
 * Supports row-based button creation, callback buttons with data, and URL buttons.
 *
 * <p>Usage examples:</p>
 * <pre>
 * // Row-based builder
 * InlineKeyboardBuilder.builder()
 *     .row(row -> row.button("Take to Foster", "TAKE_" + petId))
 *     .row(row -> row.button("Next ▶️", "NEXT_10"))
 *     .build();
 *
 * // Simple flat builder
 * InlineKeyboardBuilder.simple()
 *     .button("Take", "TAKE_1")
 *     .button("Save", "SAVE_1")
 *     .build();
 *
 * // URL button
 * InlineKeyboardBuilder.simple()
 *     .urlButton("Open Website", "https://example.com")
 *     .build();
 * </pre>
 */
@NullMarked
public class InlineKeyboardBuilder {

    private final List<List<InlineKeyboardButton>> rows = new ArrayList<>();

    private InlineKeyboardBuilder() {
    }

    /**
     * Creates a new row-based builder.
     *
     * @return a new builder instance
     */
    public static InlineKeyboardBuilder builder() {
        return new InlineKeyboardBuilder();
    }

    /**
     * Creates a new simple flat builder.
     * All buttons are added to the same row.
     *
     * @return a new builder instance
     */
    public static InlineKeyboardBuilder simple() {
        return new InlineKeyboardBuilder();
    }

    /**
     * Adds a new row using the provided row configurer.
     *
     * @param rowConfigurer the row configuration
     * @return this builder for chaining
     */
    public InlineKeyboardBuilder row(RowConfigurer rowConfigurer) {
        RowBuilderImpl rowBuilderImpl = new RowBuilderImpl();
        rowConfigurer.configure(rowBuilderImpl);
        rows.add(rowBuilderImpl.build());
        return this;
    }

    /**
     * Adds a simple flat button.
     * All simple buttons share the same row.
     *
     * @param text button text
     * @param callbackData callback data
     * @return this builder for chaining
     */
    public InlineKeyboardBuilder button(String text, String callbackData) {
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
        }
        rows.getLast().add(
                InlineKeyboardButton.builder()
                        .text(text)
                        .callbackData(callbackData)
                        .build()
        );
        return this;
    }

    /**
     * Adds a URL button to the keyboard.
     *
     * @param text button text
     * @param url URL to open
     * @return this builder for chaining
     */
    public InlineKeyboardBuilder urlButton(String text, String url) {
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
        }
        rows.getLast().add(
                InlineKeyboardButton.builder()
                        .text(text)
                        .url(url)
                        .build()
        );
        return this;
    }

    /**
     * Builds the inline keyboard markup.
     *
     * @return the built InlineKeyboardMarkup
     */
    public InlineKeyboardMarkup build() {
        List<InlineKeyboardRow> keyboardRows = new ArrayList<>();
        for (List<InlineKeyboardButton> row : rows) {
            keyboardRows.add(new InlineKeyboardRow(row));
        }
        return InlineKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .build();
    }

    /**
     * Row builder interface for row-based configuration.
     */
    @NullMarked
    public interface RowBuilder {

        /**
         * Adds a callback button to the current row.
         *
         * @param text button text
         * @param callbackData callback data
         */
        void button(String text, String callbackData);

        /**
         * Adds a URL button to the current row.
         *
         * @param text button text
         * @param url URL to open
         */
        void urlButton(String text, String url);
    }

    /**
     * Internal implementation of RowBuilder.
     */
    private static final class RowBuilderImpl implements RowBuilder {

        private final List<InlineKeyboardButton> buttons = new ArrayList<>();

        @Override
        public void button(String text, String callbackData) {
            buttons.add(
                    InlineKeyboardButton.builder()
                            .text(text)
                            .callbackData(callbackData)
                            .build()
            );
        }

        @Override
        public void urlButton(String text, String url) {
            buttons.add(
                    InlineKeyboardButton.builder()
                            .text(text)
                            .url(url)
                            .build()
            );
        }

        List<InlineKeyboardButton> build() {
            return buttons;
        }
    }

    /**
     * Functional interface for configuring a row.
     */
    @FunctionalInterface
    @NullMarked
    public interface RowConfigurer {

        /**
         * Configures a row builder.
         *
         * @param row the row builder to configure
         */
        void configure(RowBuilder row);
    }
}